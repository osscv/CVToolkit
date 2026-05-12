package cv.toolkit.data

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class GifEncoder {
    private val frames = mutableListOf<Frame>()
    private var width = 0
    private var height = 0

    private data class Frame(val bitmap: Bitmap, val delayMs: Int)

    fun setSize(w: Int, h: Int) {
        width = w; height = h
    }

    fun addFrame(bitmap: Bitmap, delayMs: Int = 100) {
        val scaled = if (width > 0 && height > 0 && (bitmap.width != width || bitmap.height != height)) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else bitmap
        if (frames.isEmpty() && width == 0) {
            width = scaled.width; height = scaled.height
        }
        frames.add(Frame(scaled, delayMs))
    }

    fun encode(): ByteArray {
        require(frames.isNotEmpty()) { "No frames added" }
        val out = ByteArrayOutputStream()

        // Header
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))

        // Logical Screen Descriptor
        writeShort(out, width)
        writeShort(out, height)
        out.write(0xF7) // GCT flag=1, color res=7, sort=0, GCT size=7 (256 colors)
        out.write(0)     // Background color index
        out.write(0)     // Pixel aspect ratio

        // Global Color Table (6x7x6 = 252 uniform colors + 4 padding)
        val palette = buildPalette()
        out.write(palette)

        // Netscape Application Extension (loop forever)
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)
        out.write(1)
        writeShort(out, 0) // loop count 0 = infinite
        out.write(0)

        for (frame in frames) {
            // Graphic Control Extension
            out.write(0x21)
            out.write(0xF9)
            out.write(4)
            out.write(0x04) // disposal=1 (do not dispose), no transparency
            writeShort(out, frame.delayMs / 10) // delay in centiseconds
            out.write(0)   // transparent color index
            out.write(0)   // block terminator

            // Image Descriptor
            out.write(0x2C)
            writeShort(out, 0) // left
            writeShort(out, 0) // top
            writeShort(out, width)
            writeShort(out, height)
            out.write(0) // no local color table

            // LZW image data
            val indices = quantizeFrame(frame.bitmap)
            lzwCompress(out, indices)
        }

        out.write(0x3B) // Trailer
        return out.toByteArray()
    }

    private fun buildPalette(): ByteArray {
        val palette = ByteArray(256 * 3)
        var idx = 0
        for (r in 0..5) {
            for (g in 0..6) {
                for (b in 0..5) {
                    palette[idx * 3] = (r * 255 / 5).toByte()
                    palette[idx * 3 + 1] = (g * 255 / 6).toByte()
                    palette[idx * 3 + 2] = (b * 255 / 5).toByte()
                    idx++
                }
            }
        }
        return palette
    }

    private fun quantizeFrame(bitmap: Bitmap): ByteArray {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val indices = ByteArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = ((c shr 16) and 0xFF) * 5 / 255
            val g = ((c shr 8) and 0xFF) * 6 / 255
            val b = (c and 0xFF) * 5 / 255
            indices[i] = (r.coerceIn(0, 5) * 42 + g.coerceIn(0, 6) * 6 + b.coerceIn(0, 5)).toByte()
        }
        return indices
    }

    private fun lzwCompress(out: ByteArrayOutputStream, indices: ByteArray) {
        val minCodeSize = 8
        out.write(minCodeSize)

        val clearCode = 1 shl minCodeSize  // 256
        val eoiCode = clearCode + 1         // 257
        val maxTableSize = 4096

        var codeSize = minCodeSize + 1
        var nextCode = eoiCode + 1
        var maxCode = 1 shl codeSize
        val table = HashMap<Long, Int>(maxTableSize * 2)

        var bitBuffer = 0
        var bitsInBuffer = 0
        val subBlock = ByteArrayOutputStream()
        val blockOut = ByteArrayOutputStream()

        fun writeCode(code: Int) {
            bitBuffer = bitBuffer or (code shl bitsInBuffer)
            bitsInBuffer += codeSize
            while (bitsInBuffer >= 8) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer shr 8
                bitsInBuffer -= 8
                if (subBlock.size() >= 254) {
                    blockOut.write(subBlock.size())
                    blockOut.write(subBlock.toByteArray())
                    subBlock.reset()
                }
            }
        }

        fun flushBits() {
            if (bitsInBuffer > 0) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = 0
                bitsInBuffer = 0
            }
            if (subBlock.size() > 0) {
                blockOut.write(subBlock.size())
                blockOut.write(subBlock.toByteArray())
                subBlock.reset()
            }
        }

        fun clearTable() {
            table.clear()
            nextCode = eoiCode + 1
            codeSize = minCodeSize + 1
            maxCode = 1 shl codeSize
        }

        clearTable()
        writeCode(clearCode)

        if (indices.isEmpty()) {
            writeCode(eoiCode)
            flushBits()
            out.write(blockOut.toByteArray())
            out.write(0)
            return
        }

        var prefix = indices[0].toInt() and 0xFF
        for (i in 1 until indices.size) {
            val suffix = indices[i].toInt() and 0xFF
            val key = (prefix.toLong() shl 16) or suffix.toLong()
            val existing = table[key]
            if (existing != null) {
                prefix = existing
            } else {
                writeCode(prefix)
                if (nextCode < maxTableSize) {
                    table[key] = nextCode++
                    if (nextCode >= maxCode && codeSize < 12) {
                        codeSize++
                        maxCode = 1 shl codeSize
                    }
                } else {
                    writeCode(clearCode)
                    clearTable()
                }
                prefix = suffix
            }
        }
        writeCode(prefix)
        writeCode(eoiCode)
        flushBits()

        out.write(blockOut.toByteArray())
        out.write(0) // block terminator
    }

    private fun writeShort(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}
