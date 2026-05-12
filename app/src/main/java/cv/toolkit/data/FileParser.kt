package cv.toolkit.data

import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses XLSX (Office Open XML) bytes into a list of column names and records.
 * XLSX is a ZIP file containing XML worksheets and shared strings.
 * No external dependencies needed — uses Android's built-in ZIP + XML parsers.
 */
object FileParser {

    data class ParsedData(
        val columns: List<String>,
        val records: List<Map<String, Any?>>
    )

    /**
     * Try to parse the given bytes as XLSX, XLS (via datastore), or CSV text.
     * Returns null if the format is not supported.
     */
    fun parse(bytes: ByteArray, maxRows: Int = 200): ParsedData? {
        // Check for ZIP signature (XLSX, DOCX, etc.)
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            return parseXlsx(bytes, maxRows)
        }

        // Try as text
        val text = try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { return null }
        if (text.isBlank()) return null

        // Check for HTML table
        val trimmed = text.trimStart()
        if (trimmed.startsWith("<!") || trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.startsWith("<table", ignoreCase = true) || trimmed.contains("<table", ignoreCase = true)) {
            val htmlResult = parseHtmlTable(text, maxRows)
            if (htmlResult != null) return htmlResult
        }

        // Detect CSV/TSV delimiter
        val firstLine = text.lines().firstOrNull() ?: return null
        val delimiter = when {
            firstLine.contains('\t') -> '\t'
            firstLine.contains(',') -> ','
            firstLine.contains(';') -> ';'
            else -> return null
        }

        return parseCsv(text, delimiter, maxRows)
    }

    fun parseCsv(text: String, delimiter: Char = ',', maxRows: Int = 200): ParsedData? {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return null

        val headers = splitCsvLine(lines[0], delimiter)
        if (headers.size < 2) return null

        val records = lines.drop(1).take(maxRows).map { line ->
            val values = splitCsvLine(line, delimiter)
            headers.mapIndexed { i, h ->
                val raw = values.getOrElse(i) { "" }.trim()
                val num = raw.toDoubleOrNull()
                h.trim() to ((num ?: raw) as Any?)
            }.toMap()
        }

        return ParsedData(headers.map { it.trim() }, records)
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> { result.add(current.toString()); current = StringBuilder() }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseHtmlTable(html: String, maxRows: Int): ParsedData? {
        return try {
            // Extract all <table> content — find the largest table
            val tables = mutableListOf<String>()
            var searchFrom = 0
            while (true) {
                val start = html.indexOf("<table", searchFrom, ignoreCase = true)
                if (start == -1) break
                val end = html.indexOf("</table>", start, ignoreCase = true)
                if (end == -1) break
                tables.add(html.substring(start, end + 8))
                searchFrom = end + 8
            }
            if (tables.isEmpty()) return null

            // Use the largest table (most likely the data table)
            val table = tables.maxByOrNull { it.length } ?: return null

            // Extract rows
            val allRows = mutableListOf<List<String>>()
            var rowSearch = 0
            while (true) {
                val trStart = table.indexOf("<tr", rowSearch, ignoreCase = true)
                if (trStart == -1) break
                val trEnd = table.indexOf("</tr>", trStart, ignoreCase = true)
                if (trEnd == -1) break
                val rowHtml = table.substring(trStart, trEnd)

                // Extract cells (th or td)
                val cells = mutableListOf<String>()
                var cellSearch = 0
                while (true) {
                    // Find <td or <th
                    val tdStart = rowHtml.indexOf("<td", cellSearch, ignoreCase = true)
                    val thStart = rowHtml.indexOf("<th", cellSearch, ignoreCase = true)
                    val cellStart = when {
                        tdStart == -1 && thStart == -1 -> -1
                        tdStart == -1 -> thStart
                        thStart == -1 -> tdStart
                        else -> minOf(tdStart, thStart)
                    }
                    if (cellStart == -1) break

                    // Find the > after the tag attributes
                    val contentStart = rowHtml.indexOf(">", cellStart)
                    if (contentStart == -1) break

                    // Find closing tag
                    val tdEnd = rowHtml.indexOf("</td>", contentStart, ignoreCase = true)
                    val thEnd = rowHtml.indexOf("</th>", contentStart, ignoreCase = true)
                    val cellEnd = when {
                        tdEnd == -1 && thEnd == -1 -> -1
                        tdEnd == -1 -> thEnd
                        thEnd == -1 -> tdEnd
                        else -> minOf(tdEnd, thEnd)
                    }
                    if (cellEnd == -1) break

                    val cellContent = rowHtml.substring(contentStart + 1, cellEnd)
                        .replace(Regex("<[^>]*>"), "") // Strip nested HTML tags
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&nbsp;", " ")
                        .replace("&#39;", "'")
                        .replace("&quot;", "\"")
                        .trim()
                    cells.add(cellContent)
                    cellSearch = cellEnd + 5
                }
                if (cells.isNotEmpty()) allRows.add(cells)
                rowSearch = trEnd + 5
            }

            if (allRows.size < 2) return null

            val headers = allRows[0]
            val records = allRows.drop(1).take(maxRows).map { row ->
                headers.mapIndexed { i, h ->
                    val raw = row.getOrElse(i) { "" }
                    val num = raw.toDoubleOrNull()
                    h to ((num ?: raw) as Any?)
                }.toMap()
            }

            ParsedData(headers, records)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseXlsx(bytes: ByteArray, maxRows: Int): ParsedData? {
        return try {
            val zipEntries = mutableMapOf<String, ByteArray>()
            val zis = java.util.zip.ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase()
                if (name.contains("sharedstrings") || name.contains("sheet1") || name.contains("sheet.xml")) {
                    zipEntries[name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()

            // Parse shared strings
            val sharedStrings = mutableListOf<String>()
            val ssEntry = zipEntries.keys.firstOrNull { it.contains("sharedstrings") }
            if (ssEntry != null) {
                val ssXml = String(zipEntries[ssEntry]!!, Charsets.UTF_8)
                val dbf = DocumentBuilderFactory.newInstance()
                dbf.isNamespaceAware = false
                val doc = dbf.newDocumentBuilder().parse(InputSource(StringReader(ssXml)))
                val tNodes = doc.getElementsByTagName("t")
                for (i in 0 until tNodes.length) {
                    sharedStrings.add(tNodes.item(i).textContent ?: "")
                }
            }

            // Parse sheet1
            val sheetEntry = zipEntries.keys.firstOrNull { it.contains("sheet1") || it.contains("sheet.xml") }
                ?: return null
            val sheetXml = String(zipEntries[sheetEntry]!!, Charsets.UTF_8)
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = false
            val doc = dbf.newDocumentBuilder().parse(InputSource(StringReader(sheetXml)))
            val rows = doc.getElementsByTagName("row")

            if (rows.length < 2) return null

            // First row = headers
            val headerRow = rows.item(0)
            val headers = mutableListOf<String>()
            val headerCells = (headerRow as? org.w3c.dom.Element)?.getElementsByTagName("c")
            if (headerCells != null) {
                for (i in 0 until headerCells.length) {
                    val cell = headerCells.item(i) as? org.w3c.dom.Element ?: continue
                    headers.add(getCellValue(cell, sharedStrings))
                }
            }
            if (headers.isEmpty()) return null

            // Data rows
            val records = mutableListOf<Map<String, Any?>>()
            val limit = minOf(rows.length, maxRows + 1)
            for (r in 1 until limit) {
                val row = rows.item(r) as? org.w3c.dom.Element ?: continue
                val cells = row.getElementsByTagName("c")
                val record = mutableMapOf<String, Any?>()
                for (i in 0 until cells.length) {
                    val cell = cells.item(i) as? org.w3c.dom.Element ?: continue
                    val colIndex = cellRefToIndex(cell.getAttribute("r") ?: "")
                    if (colIndex in headers.indices) {
                        val raw = getCellValue(cell, sharedStrings)
                        val num = raw.toDoubleOrNull()
                        record[headers[colIndex]] = num ?: raw
                    }
                }
                if (record.isNotEmpty()) records.add(record)
            }

            if (records.isEmpty()) return null
            ParsedData(headers, records)
        } catch (_: Exception) {
            null
        }
    }

    private fun getCellValue(cell: org.w3c.dom.Element, sharedStrings: List<String>): String {
        val type = cell.getAttribute("t") ?: ""
        val vNode = cell.getElementsByTagName("v").item(0) ?: return ""
        val value = vNode.textContent ?: ""
        return when (type) {
            "s" -> { // Shared string
                val idx = value.toIntOrNull() ?: return value
                sharedStrings.getOrElse(idx) { value }
            }
            "inlineStr" -> {
                val tNode = cell.getElementsByTagName("t").item(0)
                tNode?.textContent ?: value
            }
            else -> value
        }
    }

    private fun cellRefToIndex(ref: String): Int {
        // Convert cell reference like "A1", "B1", "AA1" to 0-based column index
        var index = 0
        for (c in ref) {
            if (c.isLetter()) {
                index = index * 26 + (c.uppercaseChar() - 'A' + 1)
            } else break
        }
        return index - 1
    }
}
