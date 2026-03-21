package cv.toolkit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.delay

// ── Enums ────────────────────────────────────────────────────────────────────

private enum class TypingMode(val label: String) {
    WORDS("Words"),
    SENTENCES("Sentences"),
    PARAGRAPHS("Paragraphs")
}

private enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

private enum class TestDuration(val label: String, val seconds: Int) {
    THIRTY("30s", 30),
    SIXTY("60s", 60),
    ONE_TWENTY("120s", 120),
    COMPLETE("Full Text", -1)
}

private enum class TestState { IDLE, RUNNING, COMPLETED }

// ── Word Banks ───────────────────────────────────────────────────────────────

private val easyWords = listOf(
    "the", "be", "to", "of", "and", "a", "in", "that", "have", "it",
    "for", "not", "on", "with", "he", "as", "you", "do", "at", "this",
    "but", "his", "by", "from", "they", "we", "say", "her", "she", "or",
    "an", "will", "my", "one", "all", "if", "no", "can", "had", "are",
    "was", "who", "what", "when", "so", "up", "out", "them", "then", "now",
    "look", "only", "come", "its", "over", "also", "back", "use", "two", "how",
    "our", "way", "even", "new", "want", "any", "give", "day", "most", "us",
    "big", "good", "me", "man", "old", "just", "know", "take", "get", "see",
    "go", "time", "very", "make", "like", "long", "own", "put", "ask", "work",
    "run", "try", "some", "call", "many", "name", "more", "part", "than", "been",
    "cat", "dog", "sun", "hot", "sit", "top", "red", "box", "cup", "let",
    "fun", "win", "set", "got", "yet", "far", "end", "act", "add", "age",
    "air", "arm", "art", "bad", "bag", "bed", "bit", "boy", "bus", "buy",
    "car", "cut", "did", "dry", "ear", "eat", "egg", "eye", "few", "fly",
    "fit", "fix", "gap", "gas", "got", "gun", "guy", "hat", "hit", "job",
    "joy", "key", "kid", "law", "lay", "led", "lie", "lip", "log", "lot",
    "low", "map", "mix", "net", "nor", "odd", "oil", "pay", "pen", "pet",
    "pie", "pin", "pop", "pot", "pull", "push", "race", "rain", "read", "rest",
    "rich", "ring", "rise", "road", "rock", "roll", "room", "rule", "safe", "said",
    "salt", "same", "sand", "save", "ship", "shop", "show", "side", "sign", "site"
)

private val mediumWords = listOf(
    "about", "after", "again", "below", "night", "never", "world", "still", "found", "every",
    "thing", "water", "where", "learn", "while", "right", "might", "great", "would", "could",
    "other", "which", "their", "there", "first", "light", "under", "began", "house", "place",
    "above", "start", "young", "story", "group", "study", "three", "until", "often", "watch",
    "along", "being", "leave", "small", "later", "stand", "point", "order", "seven", "eight",
    "build", "bring", "carry", "catch", "clear", "close", "cover", "cross", "dance", "death",
    "chair", "dream", "drink", "drive", "earth", "enjoy", "enter", "equal", "event", "exact",
    "field", "fight", "final", "floor", "force", "front", "glass", "green", "guess", "heart",
    "heavy", "horse", "human", "image", "issue", "judge", "large", "laugh", "level", "limit",
    "maybe", "metal", "model", "money", "month", "month", "music", "north", "offer", "ocean",
    "paper", "party", "peace", "phone", "photo", "piece", "plant", "power", "press", "price",
    "prove", "quick", "quiet", "raise", "range", "reach", "river", "round", "score", "serve",
    "shape", "share", "shoot", "short", "shout", "sleep", "smile", "solid", "sound", "south",
    "space", "speak", "spend", "staff", "stage", "state", "stone", "store", "sugar", "sweet",
    "table", "taste", "teach", "thank", "total", "touch", "tower", "track", "trade", "train",
    "treat", "trial", "trust", "truth", "union", "usual", "value", "video", "visit", "voice",
    "waste", "wheel", "white", "whole", "woman", "worth", "write", "wrong", "youth", "blank",
    "block", "board", "bonus", "brain", "brand", "brave", "bread", "break", "brief", "broad",
    "brown", "clock", "cloud", "coach", "coast", "count", "court", "craft", "crash", "cream",
    "crowd", "crown", "curve", "daily", "delay", "depth", "dirty", "doubt", "draft", "drawn"
)

private val hardWords = listOf(
    "abstract", "accident", "accurate", "achieved", "activity", "actually", "addition",
    "adequate", "adjusted", "advanced", "affected", "afforded", "agreeing", "alphabet",
    "although", "ambition", "analysis", "ancestor", "announce", "anything", "anywhere",
    "apparent", "approach", "approval", "argument", "artistic", "assembly", "assuming",
    "attached", "attacked", "attempts", "backbone", "backward", "bacteria", "balanced",
    "bankrupt", "baseline", "bathroom", "becoming", "behavior", "believed", "belonged",
    "benefits", "blankets", "bleeding", "blocking", "borrowed", "boundary", "branches",
    "breaking", "breeding", "briefing", "bringing", "brothers", "browsing", "building",
    "bulletin", "business", "calendar", "campaign", "capacity", "captured", "cardinal",
    "category", "cautious", "ceremony", "chairman", "champion", "chapters", "chemical",
    "children", "choosing", "circular", "civilian", "claiming", "climbing", "clinical",
    "clothing", "coaching", "collapse", "colonial", "colorful", "combined", "comeback",
    "commerce", "communal", "compared", "compiler", "composed", "compound", "computer",
    "concepts", "concerns", "concrete", "condense", "conflict", "congress", "connects",
    "consider", "constant", "consumer", "contains", "continue", "contract", "contrast",
    "controls", "converge", "convince", "cookbook", "corridor", "coverage", "creating",
    "creative", "criminal", "critical", "crossing", "crushing", "cultural", "currency",
    "customer", "Database", "daughter", "deadline", "dealings", "debugger", "december",
    "decision", "declared", "declined", "decrease", "defeated", "defender", "definite",
    "delegate", "deletion", "delicate", "delivery", "demanded", "departed", "deployed",
    "deposits", "describe", "designer", "despatch", "destruct", "detailed", "detected",
    "devotion", "diabetes", "diagonal", "dialogue", "diameter", "dictator", "dinosaur",
    "diploma", "diplomat", "directed", "director", "disabled", "disaster", "discount",
    "discover", "discrete", "disorder", "dispatch", "disposal", "disposed", "distance",
    "distinct", "district", "dividend", "division", "doctrine", "document", "domestic",
    "dominant", "donation", "doubtful", "download", "dramatic", "drawings", "dressing",
    "drinking", "dropping", "duration", "dynamics", "earnings", "economic", "educated",
    "efficacy", "election", "electric", "elegant", "elevated", "elimiate", "embedded",
    "emerging", "emission", "emotion", "emphasis", "employer", "empowers", "encoding",
    "endpoint", "engaging", "engineer", "enormous", "enrolled", "entering", "entirely",
    "entitled", "entrance", "envelope", "equality", "equation", "equipped", "escalate",
    "escaping", "estimate", "evaluate", "eventual", "everyone", "evidence", "evolving",
    "examined", "examples", "exchange", "exciting", "excluded", "executed", "exercise",
    "exhibits", "expected", "expedite", "expedite", "expenses", "explains", "explicit",
    "explored", "exponent", "exposure", "extended", "external", "extracts", "extremes",
    "eyebrows", "fabulous", "facility", "factored", "failsafe", "faithful", "familiar",
    "featured", "feedback", "festival", "fetching", "filename", "finalize", "findings",
    "finished", "firmware", "flexible", "floating", "flooding", "flourish", "focusing",
    "followed", "football", "forecast", "forensic", "formally", "formerly", "formulae",
    "fortress", "fostered", "founding", "fraction", "fragment", "framwork", "frankful",
    "freezing", "frequent", "friction", "friendly", "frontier", "fruition", "fuelling",
    "fulltime", "function", "generate", "genocide", "geometry", "gigabyte", "globally",
    "gorgeous", "gradient", "graduate", "graphing", "grateful", "gripping", "grouping",
    "guidance", "guardian", "handbook", "handling", "happened", "hardship", "hardware",
    "harmless", "headline", "helpless", "heritage", "highways", "historic", "holistic",
    "homeland", "honestly", "horrible", "hospital", "hostname", "humanity", "hundreds",
    "hydrogen", "ignoring", "illusion", "imagined", "imminent", "impacted", "imperial",
    "implicit", "imposing", "improved", "incident", "included", "increase", "indicate",
    "indirect", "industry", "inferior", "infinite", "informed", "inherent", "initiate",
    "innocent", "innovate", "inserted", "insisted", "inspired", "instance", "integral",
    "intended", "interact", "interest", "interior", "internal", "interval", "intimate",
    "intrinsic", "invasion", "invented", "invested", "investor", "involved", "isolated",
    "iterable", "iterator", "keyboard", "knockout", "labeling", "landlord", "language",
    "launched", "layering", "licensed", "lifetime", "likewise", "limiting", "literary",
    "literacy", "location", "lockdown", "logistic", "lonesome", "loophole", "magnetic",
    "maintain", "majority", "managing", "manifest", "manually", "mappings", "marathon",
    "marginal", "markdown", "mastered", "matching", "material", "maximize", "measured",
    "mechanic", "medieval", "membrane", "memorial", "merchant", "metadata", "midnight",
    "militant", "minimize", "ministry", "minority", "miracles", "mismatch", "mistakes",
    "mobility", "moderate", "modified", "molecule", "momentum", "monitors", "monopoly",
    "mortgage", "mounting", "multiply", "mutation", "myriadic", "mystical", "navigate",
    "negative", "neighbor", "networks", "nominace", "nonsense", "notebook", "numerous",
    "obstacle", "obtained", "occasion", "occupied", "offering", "officers", "offshore",
    "openness", "operated", "operator", "opponent", "opposite", "optimise", "optional",
    "ordinary", "organism", "organize", "oriented", "original", "orphaned", "outbreak",
    "outlined", "overcome", "overflow", "overhead", "overlook", "overlaps", "overload",
    "overseen", "overview", "pacemill", "packaged", "paradigm", "parallel", "parental",
    "partisan", "password", "patience", "pathname", "patterns", "peaceful", "peculiar",
    "pedagogy", "perceive", "periodic", "permeate", "persists", "personal", "persuade",
    "petition", "physical", "pinpoint", "pipeline", "platform", "pleasure", "plotting",
    "pointing", "polished", "politics", "populace", "populate", "portable", "portrait",
    "position", "positive", "possible", "postpone", "potatoes", "powerful", "practice",
    "precious", "preclude", "predator", "predicts", "prefixed", "pregnant", "premiere",
    "premises", "prepared", "presence", "preserve", "pressing", "pressure", "presumed",
    "pretends", "prevents", "previous", "princely", "princess", "printing", "priority",
    "prisoner", "probably", "problems", "proceeds", "produced", "producer", "products",
    "profiles", "profound", "progress", "projects", "prolific", "promised", "promoted",
    "prompted", "properly", "proposal", "proposed", "prospect", "protocol", "provider",
    "province", "prudence", "publicly", "punching", "purchase", "pursuing", "puzzling",
    "pyrrhous", "quadrant", "qualifed", "qualitor", "quantify", "quantize", "quarters"
)

// ── Sentence Bank ────────────────────────────────────────────────────────────

private val sentenceBank = listOf(
    // Programming / Technology
    "The quick brown fox jumps over the lazy dog.",
    "A well-designed algorithm can solve complex problems efficiently.",
    "Debugging code requires patience, attention to detail, and logical thinking.",
    "Version control systems help teams collaborate on software projects.",
    "The compiler translates high-level code into machine-readable instructions.",
    "Refactoring improves code quality without changing external behavior.",
    "Unit tests verify that individual components work as expected.",
    "APIs allow different software systems to communicate with each other.",
    "Cloud computing provides scalable resources on demand over the internet.",
    "Machine learning models improve their accuracy with more training data.",
    "A strong password should contain letters, numbers, and special characters.",
    "Containers package applications with their dependencies for consistent deployment.",
    "Open source software encourages collaboration and community-driven development.",
    "The database query returned thousands of records in milliseconds.",
    "Encryption protects sensitive data from unauthorized access and theft.",
    "Microservices architecture breaks applications into small independent services.",
    "Continuous integration automatically tests code changes before merging them.",
    "The network firewall blocks malicious traffic from reaching internal servers.",
    "Mobile applications must handle different screen sizes and orientations gracefully.",
    "Responsive web design adapts layouts to various device viewport widths.",

    // Nature / Science
    "The sun rises in the east and sets in the west every day.",
    "Photosynthesis converts sunlight into chemical energy stored in glucose molecules.",
    "The ocean covers more than seventy percent of the Earth's surface.",
    "Migration patterns of birds change with the shifting seasons each year.",
    "Gravity keeps planets in orbit around stars throughout the galaxy.",
    "The human body contains approximately thirty-seven trillion cells working together.",
    "Rainforests produce a significant portion of the world's oxygen supply.",
    "Volcanic eruptions can dramatically alter landscapes and weather patterns worldwide.",
    "DNA carries the genetic instructions for the development of living organisms.",
    "The speed of light is approximately three hundred thousand kilometers per second.",
    "Coral reefs support an incredible diversity of marine life beneath the waves.",
    "Earthquakes occur when tectonic plates shift along fault lines underground.",
    "The periodic table organizes chemical elements by their atomic properties.",
    "Renewable energy sources include solar, wind, hydroelectric, and geothermal power.",
    "Evolution by natural selection drives the adaptation of species over time.",
    "The atmosphere protects life on Earth by filtering harmful ultraviolet radiation.",
    "Glaciers store about sixty-nine percent of the world's fresh water supply.",
    "Thunderstorms form when warm moist air rises rapidly into the atmosphere.",
    "The moon's gravitational pull creates the tides in Earth's oceans daily.",
    "Ecosystems maintain a delicate balance between predators and their prey populations.",

    // Daily Life
    "A good morning routine sets the tone for a productive and fulfilling day.",
    "Cooking at home is often healthier and more affordable than eating out.",
    "Regular exercise improves both physical health and mental well-being significantly.",
    "Reading books expands your vocabulary and strengthens critical thinking skills.",
    "Getting enough sleep is essential for memory consolidation and overall health.",
    "Time management skills help you accomplish more with less stress each day.",
    "A balanced diet includes fruits, vegetables, proteins, and whole grains daily.",
    "Walking in nature reduces stress levels and improves your mood noticeably.",
    "Learning a new language opens doors to different cultures and opportunities.",
    "Maintaining strong friendships contributes to happiness and emotional resilience.",
    "Setting clear goals gives your daily actions direction and meaningful purpose.",
    "Keeping a journal helps organize thoughts and track personal growth over time.",
    "Drinking plenty of water throughout the day keeps your body properly hydrated.",
    "Meditation and mindfulness practices reduce anxiety and improve mental focus.",
    "Decluttering your living space creates a more calm and peaceful environment.",

    // Business / Professional
    "Effective communication is the cornerstone of successful business relationships.",
    "Strategic planning helps organizations navigate uncertainty and achieve long-term goals.",
    "Customer satisfaction drives brand loyalty and sustainable business revenue growth.",
    "Innovation requires a culture that embraces experimentation and tolerates failure.",
    "Data-driven decisions lead to more accurate and profitable business outcomes.",
    "Remote work has transformed how companies think about office space and culture.",
    "Supply chain management ensures products reach consumers efficiently and on time.",
    "Financial literacy empowers individuals to make informed investment decisions wisely.",
    "Leadership involves inspiring others to work toward a shared compelling vision.",
    "Market research provides valuable insights into consumer preferences and industry trends.",
    "Project management methodologies keep complex initiatives organized and on schedule.",
    "Networking creates professional opportunities that might otherwise remain undiscovered.",
    "Negotiation skills are essential for reaching mutually beneficial business agreements.",
    "Quality assurance processes prevent defects from reaching end users and customers.",
    "Sustainable business practices benefit both the environment and the bottom line.",

    // Technology / Future
    "Artificial intelligence is reshaping industries from healthcare to transportation rapidly.",
    "Blockchain technology enables secure decentralized transactions without intermediaries involved.",
    "Quantum computing promises to solve problems that classical computers cannot handle.",
    "The Internet of Things connects billions of everyday devices to the web.",
    "Virtual reality creates immersive experiences for entertainment, education, and training.",
    "Cybersecurity threats are growing more sophisticated and difficult to detect yearly.",
    "Autonomous vehicles could fundamentally change urban transportation and city planning.",
    "Biotechnology advances are enabling personalized medicine tailored to individual genetics.",
    "Space exploration continues to push the boundaries of human knowledge and capability.",
    "Three-dimensional printing is revolutionizing manufacturing and rapid prototyping processes.",
    "Edge computing processes data closer to where it is generated for speed.",
    "Digital twins create virtual replicas of physical systems for testing purposes.",
    "Natural language processing enables computers to understand and generate human text.",
    "Augmented reality overlays digital information onto the physical world around us.",
    "Wearable technology monitors health metrics and provides real-time biometric feedback.",

    // Miscellaneous
    "The best way to predict the future is to create it yourself today.",
    "Practice makes progress, and consistency is the key to mastering any skill.",
    "Every expert was once a beginner who refused to give up trying.",
    "Knowledge is power, but wisdom is knowing how to apply it properly.",
    "Creativity is intelligence having fun with the possibilities of the world.",
    "Success is not final and failure is not fatal; courage is what counts.",
    "The journey of a thousand miles begins with a single determined step.",
    "Actions speak louder than words, so let your work demonstrate your values.",
    "Simplicity is the ultimate sophistication in both design and daily living.",
    "Patience and persistence are the twin engines of meaningful achievement and growth."
)

// ── Paragraph Bank ───────────────────────────────────────────────────────────

private val paragraphBank = listOf(
    "Software development is a creative discipline that combines logic with imagination. " +
            "Programmers write code to solve real-world problems, automate repetitive tasks, and build " +
            "digital experiences used by millions of people every day. The best software is both " +
            "functional and elegant, balancing performance with readability.",

    "The natural world is full of remarkable phenomena that continue to inspire scientific " +
            "discovery. From the intricate structure of snowflakes to the vast complexity of ecosystems, " +
            "nature demonstrates patterns and principles that researchers are still working to fully " +
            "understand and appreciate.",

    "Effective communication is one of the most important skills in both personal and " +
            "professional life. Being able to clearly express ideas, listen actively to others, and " +
            "adapt your message to different audiences can open doors and build stronger relationships " +
            "in every area of your life.",

    "Technology has fundamentally transformed the way we live, work, and connect with one " +
            "another across the globe. Smartphones, cloud computing, and artificial intelligence have " +
            "created a world where information is instantly accessible and collaboration happens " +
            "seamlessly across vast distances and time zones.",

    "Healthy habits are the foundation of a long and fulfilling life. Regular physical " +
            "activity, balanced nutrition, adequate sleep, and stress management all contribute to " +
            "overall well-being. Small consistent choices made every day compound into significant " +
            "improvements in health over time.",

    "The history of human civilization is a story of innovation and adaptation. From the " +
            "invention of the wheel to the development of the internet, each breakthrough has built " +
            "upon previous discoveries, enabling societies to grow more complex, connected, and " +
            "capable of solving increasingly difficult challenges.",

    "Learning to type quickly and accurately is a valuable skill in the digital age. " +
            "Whether you are writing emails, coding software, or composing documents, fast typing " +
            "allows you to translate your thoughts into text more efficiently, saving time and " +
            "reducing the friction between thinking and creating.",

    "Space exploration represents one of humanity's greatest achievements and ambitions. " +
            "From the first moon landing to modern Mars rovers, our desire to explore beyond Earth " +
            "drives technological innovation and inspires generations to pursue careers in science, " +
            "engineering, and mathematics.",

    "Music is a universal language that transcends cultural boundaries and speaks directly " +
            "to human emotions. Whether classical, jazz, rock, or electronic, music has the power " +
            "to evoke memories, inspire creativity, and bring people together in shared moments of " +
            "joy, reflection, and celebration.",

    "Environmental conservation is one of the most pressing challenges facing our generation. " +
            "Protecting biodiversity, reducing carbon emissions, and developing sustainable practices " +
            "are essential for ensuring that future generations inherit a planet capable of supporting " +
            "abundant life and thriving communities."
)

// ── Text Generation ──────────────────────────────────────────────────────────

private fun generateText(mode: TypingMode, difficulty: Difficulty, wordCount: Int = 50): String {
    return when (mode) {
        TypingMode.WORDS -> {
            val bank = when (difficulty) {
                Difficulty.EASY -> easyWords
                Difficulty.MEDIUM -> easyWords + mediumWords
                Difficulty.HARD -> mediumWords + hardWords
            }
            bank.shuffled().take(wordCount).joinToString(" ")
        }

        TypingMode.SENTENCES -> {
            val count = when (difficulty) {
                Difficulty.EASY -> 3
                Difficulty.MEDIUM -> 5
                Difficulty.HARD -> 8
            }
            sentenceBank.shuffled().take(count).joinToString(" ")
        }

        TypingMode.PARAGRAPHS -> {
            val count = when (difficulty) {
                Difficulty.EASY -> 1
                Difficulty.MEDIUM -> 2
                Difficulty.HARD -> 3
            }
            paragraphBank.shuffled().take(count).joinToString("\n\n")
        }
    }
}

// ── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingTestScreen(navController: NavController) {

    // ── State ────────────────────────────────────────────────────────────────

    var typingMode by remember { mutableStateOf(TypingMode.WORDS) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var testDuration by remember { mutableStateOf(TestDuration.SIXTY) }
    var testState by remember { mutableStateOf(TestState.IDLE) }

    var targetText by remember { mutableStateOf(generateText(TypingMode.WORDS, Difficulty.MEDIUM)) }
    var typedText by remember { mutableStateOf("") }

    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var startTimeMs by remember { mutableLongStateOf(0L) }
    var errorCount by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Derived stats
    val correctChars = remember(typedText, targetText) {
        typedText.indices.count { i ->
            i < targetText.length && typedText[i] == targetText[i]
        }
    }
    val totalTyped = typedText.length
    val elapsedSeconds = elapsedTimeMs / 1000.0
    val wpm = if (elapsedSeconds > 0) ((correctChars / 5.0) / (elapsedSeconds / 60.0)) else 0.0
    val accuracy = if (totalTyped > 0) (correctChars.toDouble() / totalTyped * 100) else 100.0

    // ── Timer effect ─────────────────────────────────────────────────────────

    LaunchedEffect(testState) {
        if (testState == TestState.RUNNING) {
            while (testState == TestState.RUNNING) {
                elapsedTimeMs = System.currentTimeMillis() - startTimeMs
                // Check timed mode completion
                if (testDuration != TestDuration.COMPLETE && elapsedTimeMs / 1000 >= testDuration.seconds) {
                    testState = TestState.COMPLETED
                }
                delay(100)
            }
        }
    }

    // Auto-focus when test is idle and ready
    LaunchedEffect(testState) {
        if (testState == TestState.IDLE) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus requester not yet attached
            }
        }
    }

    // ── Helper functions ─────────────────────────────────────────────────────

    fun resetTest() {
        testState = TestState.IDLE
        typedText = ""
        elapsedTimeMs = 0L
        startTimeMs = 0L
        errorCount = 0
        targetText = generateText(typingMode, difficulty)
    }

    fun onTextInput(newText: String) {
        if (testState == TestState.COMPLETED) return

        // Start timer on first keystroke
        if (testState == TestState.IDLE && newText.isNotEmpty()) {
            testState = TestState.RUNNING
            startTimeMs = System.currentTimeMillis()
        }

        // Count new errors
        if (newText.length > typedText.length) {
            val newCharIndex = newText.length - 1
            if (newCharIndex < targetText.length && newText[newCharIndex] != targetText[newCharIndex]) {
                errorCount++
            }
        }

        typedText = newText

        // Check completion (Complete Text mode)
        if (testDuration == TestDuration.COMPLETE && typedText.length >= targetText.length) {
            testState = TestState.COMPLETED
        }
    }

    fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    // ── Build annotated display text ─────────────────────────────────────────

    val annotatedText = buildAnnotatedString {
        for (i in targetText.indices) {
            when {
                i < typedText.length && typedText[i] == targetText[i] -> {
                    // Correct character
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF4CAF50),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                    ) {
                        append(targetText[i])
                    }
                }

                i < typedText.length && typedText[i] != targetText[i] -> {
                    // Incorrect character
                    withStyle(
                        SpanStyle(
                            color = Color.White,
                            background = Color(0xFFE53935),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(targetText[i])
                    }
                }

                i == typedText.length -> {
                    // Current cursor position
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            background = MaterialTheme.colorScheme.primaryContainer,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(targetText[i])
                    }
                }

                else -> {
                    // Not yet typed
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                    ) {
                        append(targetText[i])
                    }
                }
            }
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.typing_test_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (testState != TestState.IDLE) {
                        IconButton(onClick = { resetTest() }) {
                            Icon(Icons.Filled.Refresh, "Reset")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Spacer(Modifier.height(4.dp))

            // ── Settings (visible when IDLE) ─────────────────────────────────

            if (testState == TestState.IDLE) {

                // Mode selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TypingMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = typingMode == mode,
                            onClick = {
                                typingMode = mode
                                targetText = generateText(mode, difficulty)
                                typedText = ""
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = TypingMode.entries.size
                            )
                        ) {
                            Text(mode.label, maxLines = 1)
                        }
                    }
                }

                // Difficulty selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Speed,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Difficulty:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Difficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = {
                                difficulty = diff
                                targetText = generateText(typingMode, diff)
                                typedText = ""
                            },
                            label = { Text(diff.label) }
                        )
                    }
                }

                // Duration selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Duration:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TestDuration.entries.forEach { dur ->
                        FilterChip(
                            selected = testDuration == dur,
                            onClick = { testDuration = dur },
                            label = { Text(dur.label) }
                        )
                    }
                }
            }

            // ── Live Stats Bar (visible when RUNNING) ────────────────────────

            if (testState == TestState.RUNNING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("WPM", "%.0f".format(wpm))
                        StatItem("Accuracy", "%.1f%%".format(accuracy))
                        StatItem("Time", formatElapsed(elapsedTimeMs))
                        StatItem("Errors", errorCount.toString())
                        StatItem(
                            "Progress",
                            "${typedText.length}/${targetText.length}"
                        )
                    }
                }
            }

            // ── Text Display Area ────────────────────────────────────────────

            if (testState != TestState.COMPLETED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = annotatedText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            lineHeight = 30.sp
                        )
                    }
                }

                // ── Hidden input field ───────────────────────────────────────

                BasicTextField(
                    value = typedText,
                    onValueChange = { newValue ->
                        // Only allow adding/removing from the end; prevent pasting large text
                        if (newValue.length <= targetText.length) {
                            onTextInput(newValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .height(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (typedText.isEmpty()) {
                                Text(
                                    text = if (testState == TestState.IDLE) "Start typing here..." else "Keep typing...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Start / Reset hint
                if (testState == TestState.IDLE) {
                    Text(
                        text = "The timer starts when you begin typing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ── Completion Screen ────────────────────────────────────────────

            if (testState == TestState.COMPLETED) {
                val finalWpm = if (elapsedSeconds > 0) ((correctChars / 5.0) / (elapsedSeconds / 60.0)) else 0.0
                val finalAccuracy = if (totalTyped > 0) (correctChars.toDouble() / totalTyped * 100) else 100.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Test Complete!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        // Results grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultItem("WPM", "%.0f".format(finalWpm))
                            ResultItem("Accuracy", "%.1f%%".format(finalAccuracy))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultItem("Time", formatElapsed(elapsedTimeMs))
                            ResultItem("Errors", errorCount.toString())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultItem("Characters", "${correctChars}/${targetText.length}")
                            ResultItem("Mode", typingMode.label)
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        // Performance label
                        val performanceLabel = when {
                            finalWpm >= 80 && finalAccuracy >= 95 -> "Excellent Typist!"
                            finalWpm >= 60 && finalAccuracy >= 90 -> "Great Job!"
                            finalWpm >= 40 && finalAccuracy >= 85 -> "Good Effort!"
                            finalWpm >= 25 -> "Keep Practicing!"
                            else -> "Practice Makes Perfect!"
                        }
                        Text(
                            performanceLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = { resetTest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Try Again")
                        }
                    }
                }
            }

            // ── Banner Ad ────────────────────────────────────────────────────

            BannerAd(modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ResultItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
