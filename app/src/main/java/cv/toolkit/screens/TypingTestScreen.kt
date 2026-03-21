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
    "fit", "fix", "gap", "gas", "gun", "guy", "hat", "hit", "job",
    "joy", "key", "kid", "law", "lay", "led", "lie", "lip", "log", "lot",
    "low", "map", "mix", "net", "nor", "odd", "oil", "pay", "pen", "pet",
    "pie", "pin", "pop", "pot", "pull", "push", "race", "rain", "read", "rest",
    "rich", "ring", "rise", "road", "rock", "roll", "room", "rule", "safe", "said",
    "salt", "same", "sand", "save", "ship", "shop", "show", "side", "sign", "site",
    "able", "also", "area", "army", "away", "baby", "ball", "band", "bank", "base",
    "bath", "bear", "beat", "been", "bell", "best", "bill", "bird", "blow", "blue",
    "boat", "body", "bomb", "bond", "bone", "book", "born", "boss", "both", "burn",
    "cake", "calm", "came", "camp", "card", "care", "case", "cash", "cast", "cell",
    "chin", "city", "club", "coal", "coat", "code", "cold", "cook", "cool", "cope",
    "copy", "core", "cost", "crew", "crop", "dare", "dark", "data", "date", "dawn",
    "dead", "deal", "dear", "debt", "deep", "deny", "desk", "diet", "dirt", "disc",
    "dish", "dock", "does", "done", "door", "dose", "down", "drag", "draw", "drew",
    "drop", "drug", "drum", "dual", "dull", "dump", "dust", "duty", "each", "earn",
    "ease", "east", "edge", "else", "euro", "even", "ever", "evil", "exam", "exit",
    "face", "fact", "fail", "fair", "fall", "fame", "farm", "fast", "fate", "fear",
    "feed", "feel", "feet", "fell", "felt", "file", "fill", "film", "find", "fine",
    "fire", "firm", "fish", "flat", "flee", "flew", "flip", "flow", "fold", "folk",
    "fond", "food", "fool", "foot", "ford", "form", "fort", "foul", "four", "free",
    "from", "fuel", "full", "fund", "fury", "fuse", "gain", "game", "gang", "gate",
    "gave", "gear", "gene", "gift", "girl", "glad", "glow", "glue", "goal", "goes",
    "gold", "golf", "gone", "grab", "gray", "grew", "grey", "grid", "grip", "grow",
    "gulf", "gust", "hair", "half", "hall", "halt", "hand", "hang", "hard", "harm",
    "hate", "haul", "head", "heal", "heap", "hear", "heat", "heel", "held", "hell",
    "help", "here", "hero", "hide", "high", "hill", "hint", "hire", "hold", "hole",
    "holy", "home", "hook", "hope", "horn", "host", "hour", "huge", "hung", "hunt",
    "hurt", "icon", "idea", "inch", "into", "iron", "item", "jack", "jail", "jean",
    "join", "joke", "jump", "jury", "keen", "keep", "kept", "kick", "kill", "kind",
    "king", "knee", "knew", "knit", "knot", "lack", "lady", "laid", "lake", "lamp",
    "land", "lane", "last", "late", "lead", "leaf", "lean", "left", "lend", "lens",
    "less", "lied", "life", "lift", "limb", "lime", "line", "link", "lion", "list",
    "live", "load", "loan", "lock", "logo", "lone", "look", "loop", "lord", "lose",
    "loss", "lost", "loud", "love", "luck", "lump", "lung", "made", "mail", "main",
    "make", "male", "mall", "mark", "mask", "mass", "mate", "meal", "mean", "meat",
    "meet", "melt", "memo", "menu", "mere", "mess", "mile", "milk", "mill", "mind",
    "mine", "miss", "mode", "mood", "moon", "more", "most", "move", "much", "must",
    "myth", "nail", "navy", "near", "neat", "neck", "need", "news", "next", "nice",
    "nine", "node", "none", "nose", "note", "odds", "okay", "once", "only", "onto",
    "open", "oral", "oven", "pace", "pack", "page", "paid", "pain", "pair", "pale",
    "palm", "pant", "park", "pass", "past", "path", "peak", "peer", "pick", "pile",
    "pine", "pink", "pipe", "plan", "play", "plea", "plot", "plug", "plus", "poem",
    "poet", "pole", "poll", "pond", "pool", "poor", "pope", "port", "pose", "post",
    "pour", "pray", "prey", "prop", "pull", "pump", "pure", "puts", "quit", "rack",
    "rage", "raid", "rail", "rank", "rare", "rate", "raw", "real", "rear", "reef",
    "rely", "rent", "rest", "rice", "ride", "riot", "rise", "risk", "rode", "role",
    "roof", "root", "rope", "rose", "ruin", "rush", "sake", "sang", "seal", "seat",
    "seed", "seek", "seem", "seen", "self", "sell", "send", "sent", "shed", "shin",
    "shoe", "shot", "shut", "sick", "silk", "sing", "sink", "size", "skin", "slam",
    "slap", "slip", "slot", "slow", "snap", "snow", "soap", "sock", "soft", "soil",
    "sold", "sole", "some", "song", "soon", "sort", "soul", "sour", "span", "spin",
    "spot", "star", "stay", "stem", "step", "stir", "stop", "such", "suit", "sure",
    "swim", "tail", "take", "tale", "talk", "tall", "tank", "tape", "task", "taxi",
    "team", "tear", "teen", "tell", "tend", "tent", "term", "test", "text", "than",
    "that", "thin", "thus", "tick", "tide", "tidy", "tied", "tier", "till", "tiny",
    "tire", "toe", "told", "toll", "tomb", "tone", "took", "tool", "tops", "tore",
    "torn", "toss", "tour", "town", "trap", "tree", "trim", "trio", "trip", "true",
    "tube", "tuck", "tune", "turn", "twin", "type", "tyre", "ugly", "unit", "upon",
    "urge", "used", "user", "vale", "vary", "vast", "verb", "very", "view", "visa",
    "void", "volt", "vote", "wade", "wage", "wait", "wake", "walk", "wall", "want",
    "ward", "warm", "warn", "wash", "wave", "weak", "wear", "weed", "week", "well",
    "went", "were", "west", "what", "whom", "wide", "wife", "wild", "wine", "wing",
    "wire", "wise", "wish", "with", "woke", "wood", "wool", "word", "wore", "worn",
    "wrap", "yard", "yeah", "year", "yell", "zero", "zone", "zoom"
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
    "maybe", "metal", "model", "money", "month", "music", "north", "offer", "ocean",
    "paper", "party", "peace", "phone", "photo", "piece", "plant", "power", "press", "price",
    "prove", "quick", "quiet", "raise", "range", "reach", "river", "round", "score", "serve",
    "shape", "share", "shoot", "short", "shout", "sleep", "smile", "solid", "sound", "south",
    "space", "speak", "spend", "staff", "stage", "state", "stone", "store", "sugar", "sweet",
    "table", "taste", "teach", "thank", "total", "touch", "tower", "track", "trade", "train",
    "treat", "trial", "trust", "truth", "union", "usual", "value", "video", "visit", "voice",
    "waste", "wheel", "white", "whole", "woman", "worth", "write", "wrong", "youth", "blank",
    "block", "board", "bonus", "brain", "brand", "brave", "bread", "break", "brief", "broad",
    "brown", "clock", "cloud", "coach", "coast", "count", "court", "craft", "crash", "cream",
    "crowd", "crown", "curve", "daily", "delay", "depth", "dirty", "doubt", "draft", "drawn",
    "admit", "adopt", "agent", "agree", "ahead", "alarm", "album", "alien", "align", "alive",
    "allow", "alone", "alter", "among", "anger", "angle", "angry", "anime", "apart", "apple",
    "apply", "arena", "argue", "arise", "aside", "asset", "atlas", "avoid", "awake", "award",
    "aware", "badge", "basic", "basin", "basis", "beach", "began", "being", "bench", "berry",
    "birth", "blade", "blame", "blast", "blaze", "bleed", "blend", "bless", "blind", "bloom",
    "blown", "blunt", "boost", "bound", "brick", "bride", "brisk", "broke", "brook", "brush",
    "buddy", "bunch", "burst", "buyer", "cabin", "cable", "camel", "cargo", "cause", "cedar",
    "chain", "chalk", "chaos", "charm", "chase", "cheap", "check", "cheek", "cheer", "chess",
    "chest", "chief", "child", "chill", "choir", "chose", "chunk", "civil", "claim", "clash",
    "class", "clean", "clerk", "click", "cliff", "climb", "cling", "clone", "cloth", "coach",
    "color", "combo", "comes", "comic", "coral", "could", "couch", "crack", "crane", "crawl",
    "crazy", "creek", "crest", "crime", "crisp", "cruel", "crush", "cycle", "dance", "dealt",
    "decay", "decor", "demon", "dense", "depot", "derby", "devil", "diary", "digit", "ditch",
    "dizzy", "dodge", "donor", "donut", "dozen", "drain", "drama", "drank", "drape", "dress",
    "dried", "drift", "drill", "drove", "drums", "drunk", "dryer", "dunce", "dusty", "dwarf",
    "dwell", "eager", "early", "easel", "eater", "edict", "eight", "elbow", "elder", "elect",
    "elite", "email", "empty", "enemy", "enjoy", "entry", "envoy", "epoch", "equip", "erode",
    "error", "essay", "ethic", "evade", "exact", "exert", "exile", "extra", "fable", "facet",
    "faint", "fairy", "faith", "false", "fancy", "fatal", "fault", "feast", "fence", "ferry",
    "fetch", "fever", "fiber", "fifty", "filth", "flame", "flank", "flare", "flash", "flesh",
    "flock", "flood", "floss", "flour", "fluid", "fluke", "flute", "focal", "focus", "foggy",
    "force", "forge", "forth", "forty", "forum", "found", "frame", "frank", "fraud", "fresh",
    "frost", "froze", "fruit", "fully", "fungi", "fuzzy", "gauge", "ghost", "giant", "given",
    "globe", "gloom", "glory", "gloss", "glove", "glyph", "going", "goods", "goose", "gorge",
    "grace", "grade", "grain", "grand", "grant", "grape", "graph", "grasp", "grass", "grave",
    "graze", "greed", "grief", "grind", "groan", "groom", "gross", "group", "grove", "grown",
    "guard", "guide", "guild", "guilt", "guise", "gully", "gusty", "habit", "handy", "happy",
    "hardy", "harsh", "haste", "hasty", "haven", "heart", "heave", "hedge", "herbs", "heron",
    "honey", "honor", "hover", "humor", "hurry", "hyena", "ideal", "idiot", "imply", "inbox",
    "index", "indie", "infer", "inner", "input", "inter", "intro", "ionic", "ivory", "jewel",
    "joint", "joker", "jolly", "juice", "juicy", "knack", "knead", "kneel", "knife", "knock",
    "known", "label", "labor", "lance", "laser", "latch", "later", "layer", "lemon", "level",
    "lever", "light", "liken", "lilac", "liner", "links", "liter", "liver", "lobby", "local",
    "lodge", "logic", "loose", "lotus", "lover", "lower", "loyal", "lucid", "lunar", "lunch",
    "lunge", "lymph", "lyric", "macro", "magic", "major", "manga", "manor", "maple", "march",
    "mayor", "media", "medic", "melon", "mercy", "merge", "merit", "merry", "micro", "midst",
    "might", "mimic", "miner", "minor", "minus", "mirth", "misty", "mixer", "modem", "moist",
    "month", "moral", "motor", "mount", "mourn", "mouse", "mouth", "movie", "muddy", "multi",
    "mural", "naive", "nerve", "never", "newly", "nexus", "niche", "night", "ninja", "noble",
    "noise", "north", "notch", "noted", "novel", "nudge", "nurse", "occur", "olive", "onset",
    "opera", "orbit", "organ", "other", "ought", "outer", "outdo", "overt", "oxide", "ozone",
    "panic", "paste", "patch", "pause", "pearl", "pedal", "penny", "perch", "peril", "phase",
    "piano", "pilot", "pinch", "pixel", "pizza", "plaid", "plain", "plane", "plate", "plaza",
    "plead", "pluck", "plumb", "plume", "plump", "plunge", "poach", "polar", "polka", "poppy",
    "porch", "poser", "pouch", "pound", "power", "prank", "prawn", "pride", "prime", "print",
    "prior", "prism", "prize", "probe", "prone", "proof", "proud", "proxy", "prune", "psalm",
    "pulse", "pupil", "puppy", "purge", "purse", "quake", "qualm", "queen", "query", "quest",
    "quota", "quote", "radar", "radio", "rally", "ramen", "rapid", "ratio", "realm", "rebel",
    "recap", "refer", "reign", "relax", "relay", "renal", "renew", "repay", "reply", "rider",
    "ridge", "rifle", "rigid", "rinse", "risen", "risky", "rival", "river", "robin", "robot",
    "rocky", "rogue", "roots", "rouge", "rough", "route", "rover", "royal", "rugby", "ruins",
    "ruler", "rumor", "rural", "sadly", "saint", "salad", "salon", "satin", "sauce", "scale",
    "scare", "scene", "scent", "scope", "scout", "screw", "seize", "sense", "setup", "seven",
    "shade", "shaft", "shall", "shame", "shark", "sharp", "shear", "sheet", "shelf", "shell",
    "shift", "shire", "shock", "shore", "sight", "since", "sixth", "sixty", "sized", "skate",
    "skill", "skull", "slash", "slave", "sleek", "slept", "slice", "slide", "slope", "smart",
    "smell", "smoke", "snack", "snake", "solar", "solve", "sorry", "spark", "spawn", "spear",
    "spell", "spent", "spice", "spill", "spine", "spoke", "spoon", "sport", "spray", "squad",
    "stack", "stain", "stake", "stale", "stall", "stamp", "stark", "steal", "steam", "steel",
    "steep", "steer", "stick", "stiff", "still", "sting", "stock", "stole", "stood", "stool",
    "storm", "story", "stove", "strap", "straw", "stray", "strip", "stuck", "stuff", "stump",
    "style", "surge", "swamp", "swear", "sweep", "swell", "swept", "swift", "swing", "sword",
    "swore", "sworn", "swung", "syrup", "taboo", "tally", "talon", "tango", "tease", "tempo",
    "theft", "theme", "thick", "thing", "think", "third", "thorn", "those", "threw", "throw",
    "thump", "tiger", "tight", "title", "today", "token", "topic", "torch", "tough", "toxic",
    "trace", "trait", "trash", "trend", "tribe", "trick", "troop", "truck", "truly", "trunk",
    "tumor", "tuner", "twice", "twist", "ultra", "uncle", "under", "unify", "unity", "upper",
    "upset", "urban", "usher", "utter", "valid", "valve", "vapor", "vault", "verse", "vigor",
    "vinyl", "viola", "viper", "viral", "virus", "vital", "vivid", "vocal", "vodka", "vogue",
    "voter", "vouch", "vowel", "wagon", "waist", "watch", "weary", "weave", "wedge", "wheat",
    "where", "which", "whirl", "widow", "width", "witch", "woman", "women", "woody", "world",
    "worst", "wound", "wrath", "wreck", "wrist", "wrote", "yacht", "yield", "young", "zebra"
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
    "Patience and persistence are the twin engines of meaningful achievement and growth.",

    // History & Geography
    "Ancient civilizations built remarkable structures that still stand thousands of years later.",
    "The printing press revolutionized the spread of knowledge across entire continents.",
    "Trade routes connected distant cultures and enabled the exchange of goods and ideas.",
    "Maps have evolved from hand-drawn scrolls to interactive digital navigation systems.",
    "The industrial revolution transformed economies from agriculture to manufacturing and beyond.",
    "Democracy originated in ancient Greece where citizens voted directly on important laws.",
    "The Silk Road facilitated trade between the East and West for centuries.",
    "Continental drift slowly moves landmasses across the surface of the Earth over millions of years.",
    "The Renaissance sparked a revival of art, science, and cultural achievement across Europe.",
    "Ancient libraries preserved knowledge that might otherwise have been lost to history forever.",

    // Psychology & Philosophy
    "Critical thinking involves analyzing information objectively before forming a reasoned judgment.",
    "Emotional intelligence helps people navigate social situations with empathy and self-awareness.",
    "Habits form through repeated actions until the behavior becomes automatic and effortless.",
    "Motivation can come from external rewards or internal desires for personal achievement.",
    "Cognitive biases are mental shortcuts that sometimes lead to systematic errors in thinking.",
    "Mindfulness involves paying attention to the present moment without judgment or distraction.",
    "Resilience is the ability to recover quickly from difficulties and adapt to change.",
    "Empathy allows us to understand and share the feelings of another person deeply.",
    "Self-discipline is often more important than raw talent in achieving long-term success.",
    "Philosophy encourages us to question assumptions and explore the nature of reality and truth.",

    // Sports & Fitness
    "Team sports teach valuable lessons about cooperation, communication, and shared responsibility.",
    "Stretching before exercise helps prevent injuries and improves overall physical flexibility.",
    "Marathon runners train for months to build the endurance needed for the full distance.",
    "Swimming provides a full-body workout while being gentle on joints and muscles.",
    "Good sportsmanship means treating opponents with respect regardless of the final outcome.",
    "Yoga combines physical postures with breathing techniques to improve body and mind harmony.",
    "Athletes track their performance metrics to identify areas for improvement and growth.",
    "Proper nutrition fuels athletic performance and supports faster recovery after intense workouts.",
    "Rock climbing requires both physical strength and mental problem-solving abilities to succeed.",
    "Tennis demands quick reflexes, strategic thinking, and excellent hand-eye coordination skills.",

    // Food & Cooking
    "Fresh ingredients are the foundation of flavorful and nutritious home-cooked meals.",
    "Baking requires precise measurements while cooking allows more room for creative improvisation.",
    "Spices from around the world add depth and complexity to simple everyday dishes.",
    "Fermentation is an ancient food preservation technique that also creates unique flavors.",
    "Meal planning saves time, reduces food waste, and helps maintain a healthy diet.",
    "Different cultures have developed unique cooking methods suited to their local ingredients.",
    "A sharp knife is the most important and versatile tool in any kitchen setup.",
    "Sourdough bread relies on wild yeast and bacteria for its distinctive tangy flavor.",
    "Seasonal cooking ensures you eat produce at its peak freshness and nutritional value.",
    "The art of plating transforms a simple dish into a visually appealing experience.",

    // Arts & Culture
    "Music has the power to evoke deep emotions and create lasting personal memories.",
    "Photography captures fleeting moments and preserves them as permanent visual records of life.",
    "Theater brings stories to life through the combined art of acting and stagecraft.",
    "Architecture shapes the physical spaces where we live, work, and gather together daily.",
    "Painting allows artists to express emotions and ideas through color, texture, and form.",
    "Literature explores the human condition through narratives that span cultures and centuries.",
    "Dance is one of the oldest forms of human expression found in every culture.",
    "Sculpture transforms raw materials into three-dimensional art that occupies physical space beautifully.",
    "Cinema combines visual storytelling with sound to create immersive narrative experiences for audiences.",
    "Calligraphy elevates handwriting into an art form that requires patience and steady precision.",

    // Education & Learning
    "Active recall is more effective for learning than simply rereading notes or textbooks.",
    "Teaching others is one of the best ways to deepen your own understanding of a topic.",
    "Curiosity drives the desire to learn and explore new subjects throughout your entire life.",
    "Spaced repetition helps transfer information from short-term memory into long-term retention effectively.",
    "Making mistakes is an essential part of the learning process and should be embraced.",
    "Collaborative learning allows students to benefit from diverse perspectives and shared knowledge.",
    "Visual aids like diagrams and charts can make complex information easier to understand quickly.",
    "Reading widely exposes you to new vocabulary, ideas, and ways of thinking about the world.",
    "Practice problems help reinforce theoretical knowledge with practical application and deeper understanding.",
    "Good study habits established early in life provide a strong foundation for lifelong learning.",

    // Environment & Sustainability
    "Recycling reduces the amount of waste that ends up in landfills and oceans worldwide.",
    "Solar panels convert sunlight directly into electricity without producing harmful greenhouse emissions.",
    "Planting trees helps absorb carbon dioxide and provides habitat for countless wildlife species.",
    "Water conservation is essential in regions facing drought and increasing demand for fresh water.",
    "Sustainable agriculture practices protect soil health while maintaining productive crop yields over time.",
    "Electric vehicles produce zero tailpipe emissions and help reduce urban air pollution significantly.",
    "Biodiversity loss threatens the stability of ecosystems that humans depend on for survival.",
    "Reducing single-use plastics is a simple but effective step toward environmental conservation efforts.",
    "Wind farms generate clean renewable energy by converting the kinetic energy of moving air.",
    "Ocean cleanup efforts aim to remove millions of tons of plastic debris from our seas.",

    // Travel & Adventure
    "Traveling to new places broadens your perspective and deepens your understanding of the world.",
    "Learning basic phrases in the local language shows respect and enriches your travel experience.",
    "Hiking through mountain trails offers breathtaking views and a rewarding sense of accomplishment.",
    "Cultural immersion involves participating in local customs, traditions, and daily activities firsthand.",
    "Packing light makes traveling more convenient and gives you greater flexibility on your journey.",
    "Street food offers an authentic taste of local cuisine at affordable prices for travelers.",
    "National parks preserve natural wonders and provide outdoor recreation opportunities for all visitors.",
    "Journaling during trips helps you remember details and reflect on your experiences meaningfully later.",
    "Solo travel builds confidence, independence, and the ability to navigate unfamiliar situations alone.",
    "Responsible tourism minimizes negative impacts on local communities and natural environments everywhere.",

    // Science & Discovery
    "Astronomers use powerful telescopes to observe galaxies billions of light-years away from Earth.",
    "The periodic table arranges elements by atomic number revealing patterns in their chemical properties.",
    "Vaccines have saved millions of lives by training the immune system to fight specific diseases.",
    "Genetic engineering allows scientists to modify DNA sequences with unprecedented precision and speed.",
    "The theory of relativity fundamentally changed our understanding of space, time, and gravity.",
    "Chemistry explains how atoms bond together to form the molecules that make up everything around us.",
    "Satellites orbiting Earth provide critical data for weather forecasting and global communication networks.",
    "The discovery of penicillin revolutionized medicine and opened the era of modern antibiotics.",
    "Nanotechnology works at the atomic scale to create materials with extraordinary new properties.",
    "Fossil records provide evidence of life forms that existed millions of years before humans appeared.",
    "Quantum mechanics describes the bizarre behavior of particles at the subatomic scale of reality.",
    "Medical imaging technologies allow doctors to see inside the human body without invasive surgery.",
    "The scientific method provides a systematic approach to testing hypotheses and discovering truth.",
    "Plate tectonics explains how the continents move and why earthquakes occur along fault lines.",
    "Stem cell research holds promise for treating diseases that currently have no effective cure.",

    // Mathematics & Logic
    "Mathematics provides the language and tools needed to describe patterns in the natural world.",
    "Probability theory helps us make rational decisions when faced with uncertainty and incomplete information.",
    "The Pythagorean theorem relates the sides of a right triangle in a beautifully simple equation.",
    "Statistics allow researchers to draw meaningful conclusions from large and complex datasets efficiently.",
    "Algebra teaches us to solve problems by representing unknown quantities with variables and equations.",
    "Geometry helps architects and engineers design structures that are both beautiful and structurally sound.",
    "Calculus provides the mathematical framework for understanding rates of change and accumulated quantities.",
    "Logic is the foundation of clear reasoning and the basis of all computer programming languages.",
    "Fibonacci numbers appear surprisingly often in natural patterns like flower petals and spiral shells.",
    "Cryptography uses mathematical algorithms to secure digital communications and protect sensitive information.",

    // Technology & Innovation
    "Cloud storage allows users to access their files from any device connected to the internet.",
    "Robotics combines mechanical engineering with computer science to create machines that perform tasks autonomously.",
    "Voice assistants use natural language processing to understand and respond to spoken commands accurately.",
    "Drones are being used for delivery, photography, agriculture, and emergency search and rescue operations.",
    "Biometric authentication uses unique physical characteristics like fingerprints to verify personal identity securely.",
    "The development of the transistor paved the way for modern computing and digital electronics.",
    "Smart home devices automate everyday tasks like adjusting lighting, temperature, and security systems.",
    "Open source communities collaborate globally to build software that anyone can use and improve freely.",
    "Digital currencies operate on decentralized networks that enable peer-to-peer transactions without intermediaries.",
    "Fiber optic cables transmit data at the speed of light across vast distances with minimal loss.",
    "Machine vision enables computers to interpret and understand visual information from cameras and sensors.",
    "Additive manufacturing builds objects layer by layer from digital designs using various materials precisely.",
    "Edge computing reduces latency by processing data closer to the source rather than in distant centers.",
    "Quantum encryption promises virtually unbreakable security based on the fundamental laws of physics.",
    "Haptic feedback technology simulates the sense of touch in digital interfaces and virtual environments.",

    // Personal Development
    "Setting specific and measurable goals increases the likelihood of achieving meaningful personal progress.",
    "Journaling regularly helps clarify thoughts, process emotions, and track personal growth over time.",
    "Public speaking skills can be improved through consistent practice and constructive feedback from others.",
    "Time blocking is a productivity technique that assigns specific tasks to dedicated periods of the day.",
    "Building good financial habits early in life creates a foundation for long-term wealth and security.",
    "Networking with people from different industries expands your perspective and creates unexpected opportunities.",
    "Failure is not the opposite of success but rather an important stepping stone on the path to it.",
    "Reading biographies of successful people provides valuable insights and inspiration for personal achievement.",
    "Maintaining a growth mindset means believing that abilities can be developed through dedication and effort.",
    "Gratitude practice has been shown to improve mental health and overall life satisfaction significantly.",

    // Architecture & Design
    "Sustainable architecture incorporates energy-efficient materials and designs that minimize environmental impact effectively.",
    "Interior design transforms empty rooms into functional and aesthetically pleasing spaces for living and working.",
    "Urban gardens bring greenery into cities and provide residents with fresh produce and community connection.",
    "Minimalist design removes unnecessary elements to create clean and focused visual compositions that communicate clearly.",
    "Color theory helps designers choose palettes that evoke specific emotions and guide user attention effectively.",
    "Accessibility in design ensures that products and spaces can be used by people of all abilities.",
    "Typography plays a crucial role in readability and the overall visual hierarchy of any design layout.",
    "User experience research involves studying how real people interact with products to improve their design.",
    "Ergonomic furniture design promotes comfort and reduces physical strain during long periods of sitting or standing.",
    "Responsive design ensures websites and applications look great on screens of every size and resolution.",

    // Music & Sound
    "Learning to play a musical instrument develops coordination, discipline, and creative expression simultaneously.",
    "Sound waves travel through air at approximately three hundred and forty-three meters per second at sea level.",
    "Composers use melody, harmony, and rhythm to create emotional experiences that transcend spoken language barriers.",
    "Digital audio workstations have democratized music production, making it accessible to bedroom producers worldwide.",
    "The human ear can detect frequencies ranging from twenty hertz to approximately twenty thousand hertz.",
    "Acoustic design in concert halls ensures that sound reaches every seat with optimal clarity and balance.",
    "Music therapy is used to treat conditions ranging from anxiety and depression to chronic pain management.",
    "Vinyl records have made a surprising comeback as audiophiles appreciate their warm analog sound quality.",
    "Podcasting has created a new medium for storytelling, education, and conversation that anyone can participate in.",
    "Noise-canceling headphones use active electronics to reduce unwanted ambient sounds for a quieter listening experience.",

    // Economics & Finance
    "Compound interest allows savings to grow exponentially over time, rewarding patience and early investment habits.",
    "Supply and demand are the fundamental forces that determine prices in a free market economy.",
    "Diversifying investments across different asset classes reduces overall portfolio risk and volatility significantly.",
    "Inflation gradually reduces the purchasing power of money, making financial planning essential for everyone.",
    "Entrepreneurship drives economic growth by creating new businesses, products, jobs, and innovative solutions.",
    "Understanding tax regulations helps individuals and businesses minimize their legal tax burden effectively.",
    "Global trade agreements facilitate commerce between nations by reducing tariffs and other trade barriers.",
    "Budgeting is the foundation of personal financial management and helps track income versus expenses clearly.",
    "Central banks influence economic activity by adjusting interest rates and controlling the money supply carefully.",
    "Financial literacy education empowers people to make informed decisions about saving, investing, and spending wisely.",

    // Communication & Media
    "Social media has fundamentally changed how people share information and connect with each other globally.",
    "Effective writing requires clear thinking, logical organization, and careful attention to word choice and tone.",
    "Body language communicates powerful messages that sometimes contradict the words being spoken out loud.",
    "Journalism plays a vital role in democracy by informing citizens about important events and issues.",
    "Active listening involves fully concentrating on what someone is saying rather than planning your response.",
    "Visual storytelling combines images and narrative to communicate complex ideas quickly and memorably to audiences.",
    "Email etiquette includes using clear subject lines, concise messages, and appropriate greetings and closings.",
    "Presentation skills combine content knowledge with delivery techniques to engage and persuade any audience effectively.",
    "Cross-cultural communication requires awareness of different customs, values, and communication styles around the world.",
    "Constructive feedback focuses on specific behaviors and offers actionable suggestions for improvement rather than criticism.",

    // Health & Wellness
    "Regular cardiovascular exercise strengthens the heart and improves blood circulation throughout the entire body.",
    "Mental health is just as important as physical health and deserves equal attention and care always.",
    "Adequate sleep allows the brain to consolidate memories, repair tissues, and regulate hormones properly overnight.",
    "Hydration affects energy levels, cognitive function, and physical performance more than most people realize daily.",
    "Preventive healthcare catches potential problems early when they are most treatable and least costly to address.",
    "Stress management techniques like deep breathing and meditation can lower blood pressure and reduce anxiety.",
    "A balanced microbiome in the gut influences digestion, immunity, and even mood and mental clarity significantly.",
    "Ergonomic workstations reduce the risk of repetitive strain injuries and chronic back pain for office workers.",
    "Vitamins and minerals from whole foods support essential bodily functions that supplements alone cannot fully replace.",
    "Social connections and meaningful relationships are among the strongest predictors of longevity and overall happiness.",

    // Space & Astronomy
    "The observable universe contains an estimated two trillion galaxies, each containing billions of individual stars.",
    "Light from the nearest star beyond our sun takes over four years to reach planet Earth.",
    "Black holes have gravitational fields so strong that not even light can escape their powerful pull.",
    "The International Space Station orbits Earth approximately every ninety minutes at over seventeen thousand miles per hour.",
    "Mars rovers have discovered evidence suggesting that liquid water once flowed on the surface of the red planet.",
    "The James Webb Space Telescope captures infrared images of the earliest galaxies formed after the Big Bang.",
    "Asteroids contain valuable minerals that future space mining operations may one day extract and utilize commercially.",
    "Solar flares can disrupt satellite communications, power grids, and GPS systems across entire continents on Earth.",
    "The search for extraterrestrial life focuses on planets within habitable zones where liquid water could exist.",
    "Rocket reusability has dramatically reduced the cost of launching payloads into orbit around the Earth."
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
            "abundant life and thriving communities.",

    "The art of writing clean code goes beyond mere functionality. It requires thoughtful naming " +
            "conventions, consistent formatting, and a deep understanding of the problem domain. Clean " +
            "code reads like well-written prose, telling a story that future developers can easily follow " +
            "and extend without confusion or unnecessary complexity.",

    "Artificial intelligence is transforming every industry from healthcare diagnostics to autonomous " +
            "transportation. Machine learning algorithms can now identify patterns in massive datasets that " +
            "would take humans years to analyze. As these technologies mature, they promise to augment human " +
            "capabilities rather than simply replace them in the workforce.",

    "The ocean remains one of the least explored frontiers on our planet. Deep sea expeditions " +
            "continue to discover new species and geological formations that challenge our understanding " +
            "of life on Earth. Marine biologists estimate that millions of ocean species remain unknown " +
            "to science, hidden in the vast depths of the underwater world.",

    "Photography has evolved dramatically since its invention in the early nineteenth century. " +
            "From daguerreotypes to digital sensors, the technology has changed but the core principle " +
            "remains the same: capturing light to preserve a moment in time. Today anyone with a " +
            "smartphone can create images that rival professional equipment from just decades ago.",

    "Urban planning shapes the cities where billions of people live, work, and play every day. " +
            "Good city design balances transportation networks, green spaces, commercial zones, and " +
            "residential areas to create livable communities. As populations grow, sustainable urban " +
            "planning becomes increasingly critical for quality of life and environmental health.",

    "The human brain is the most complex organ in the known universe, containing approximately " +
            "eighty-six billion neurons connected by trillions of synapses. Neuroscientists are only " +
            "beginning to understand how these vast networks of cells give rise to consciousness, " +
            "memory, emotion, and the full spectrum of human experience.",

    "Cooking is both a science and an art that brings people together across cultures and " +
            "generations. Understanding the chemistry behind techniques like caramelization, emulsion, " +
            "and fermentation empowers home cooks to experiment confidently. The kitchen is a laboratory " +
            "where creativity meets precision to produce something nourishing and delicious.",

    "Renewable energy technology has advanced rapidly in recent years, making solar and wind " +
            "power increasingly cost-competitive with fossil fuels. Battery storage solutions are " +
            "improving to address intermittency challenges, and smart grid technology enables more " +
            "efficient distribution of clean energy across vast electrical networks.",

    "Language is the most powerful tool humans have ever developed for sharing knowledge and " +
            "building civilization. From ancient cave paintings to modern programming languages, our " +
            "ability to encode and transmit information has driven every major advancement in human " +
            "history. Learning new languages opens windows into different ways of thinking and being.",

    "The global economy is an intricate web of interconnected markets, supply chains, and " +
            "financial systems that span every continent. Economic decisions made in one country can " +
            "ripple across the world within hours. Understanding these connections helps individuals " +
            "and businesses make more informed decisions in an increasingly globalized world.",

    "Physical fitness is not just about appearance but about building a body and mind that " +
            "can handle the demands of daily life with energy and resilience. Regular exercise " +
            "strengthens the cardiovascular system, builds lean muscle, improves mental health, and " +
            "increases overall longevity. Even small daily movement habits compound into significant results.",

    "The history of mathematics stretches back thousands of years to ancient Mesopotamia and " +
            "Egypt where early civilizations developed counting systems and geometric principles. Today " +
            "mathematics underpins everything from computer algorithms to financial markets, quantum " +
            "physics to medical imaging. It remains the universal language of logic and precision.",

    "Good design is invisible. When products, interfaces, and spaces are well-designed, users " +
            "interact with them effortlessly without thinking about the underlying structure. This " +
            "seamlessness requires enormous effort from designers who must anticipate needs, eliminate " +
            "friction, and create experiences that feel intuitive and natural to every user.",

    "Climate change represents the defining challenge of our era, requiring unprecedented " +
            "global cooperation to address effectively. Scientists agree that reducing greenhouse gas " +
            "emissions is essential to limiting temperature rise and its cascading effects on weather " +
            "patterns, sea levels, agriculture, and biodiversity across the planet.",

    "Cybersecurity is no longer just a concern for large corporations and governments. Every " +
            "individual who uses the internet faces potential threats from phishing attacks, malware, " +
            "and data breaches. Strong passwords, two-factor authentication, and regular software updates " +
            "are simple but effective measures that everyone should adopt to protect their digital lives.",

    "The world of competitive gaming has grown into a billion-dollar industry that rivals " +
            "traditional sports in viewership and prize money. Professional esports players train for " +
            "hours daily to refine their reflexes, strategy, and teamwork. Major tournaments fill arenas " +
            "with enthusiastic fans and attract millions of online viewers from around the globe.",

    "Libraries have evolved far beyond their traditional role as repositories of printed books. " +
            "Modern libraries offer digital resources, maker spaces, community programs, and technology " +
            "access that serve diverse community needs. They remain one of the few public spaces where " +
            "anyone can learn, explore, and connect without any cost or membership requirements.",

    "The psychology of color influences human behavior in ways that most people never consciously " +
            "notice. Restaurants use warm reds and oranges to stimulate appetite, while hospitals choose " +
            "calming blues and greens to reduce patient anxiety. Understanding color psychology gives " +
            "designers and marketers a powerful tool for shaping perception and guiding decisions.",

    "Public transportation systems are the backbone of sustainable urban mobility in cities around " +
            "the world. Efficient metro networks, bus rapid transit, and light rail systems reduce traffic " +
            "congestion, lower carbon emissions, and provide affordable mobility for millions of commuters " +
            "who depend on reliable transit to reach work, school, and essential services every day.",

    "The craft of storytelling has been central to human culture since our earliest ancestors " +
            "gathered around campfires to share tales of adventure and wisdom. Today, storytelling " +
            "manifests in novels, films, video games, and podcasts, but the fundamental purpose remains " +
            "the same: to connect people through shared experiences and universal human emotions.",

    "Sleep science has revealed that rest is not a passive state but an active process " +
            "during which the brain consolidates memories, clears metabolic waste, and reorganizes " +
            "neural connections. Chronic sleep deprivation has been linked to impaired cognitive function, " +
            "weakened immunity, and increased risk of serious health conditions including heart disease.",

    "Volunteering provides benefits that extend far beyond the immediate help given to those in " +
            "need. Research shows that regular volunteers experience lower rates of depression, greater " +
            "life satisfaction, and even improved physical health. Community service builds social bonds " +
            "and creates a sense of purpose that enriches the lives of both givers and receivers.",

    "The evolution of mobile phones from simple communication devices to powerful pocket computers " +
            "has transformed virtually every aspect of modern life. We now carry devices that serve as " +
            "cameras, navigation systems, payment terminals, and entertainment centers. This convergence " +
            "of technology has made smartphones indispensable tools for billions of people worldwide.",

    "Critical infrastructure like power grids, water treatment plants, and communication networks " +
            "forms the invisible foundation that modern society depends upon every single day. Protecting " +
            "these systems from natural disasters, cyberattacks, and aging equipment requires ongoing " +
            "investment and vigilance from governments, utilities, and security professionals alike.",

    "The relationship between humans and dogs stretches back at least fifteen thousand years, " +
            "making it one of the oldest and most enduring interspecies partnerships in history. Dogs " +
            "have served as hunters, herders, guards, and companions. Modern research confirms that pet " +
            "ownership reduces stress, encourages physical activity, and combats loneliness in measurable ways.",

    "Fermentation has been used by humans for thousands of years to preserve food, create " +
            "beverages, and develop complex flavors that cannot be achieved through any other cooking " +
            "method. From Korean kimchi to French cheese, Japanese miso to German sauerkraut, fermented " +
            "foods reflect the ingenuity of cultures that learned to harness beneficial microorganisms.",

    "The transition to remote work has challenged traditional assumptions about productivity and " +
            "collaboration in professional settings. While some workers thrive with the flexibility and " +
            "autonomy of working from home, others miss the spontaneous interactions and social connections " +
            "of a shared office. Finding the right balance remains an evolving challenge for organizations.",

    "Typography is far more than choosing a font for a document or website design. " +
            "The spacing between letters, the height of lines, the weight of strokes, and the " +
            "contrast between thick and thin elements all contribute to readability, mood, and the " +
            "overall personality of written communication. Great typography is invisible; poor typography is painful.",

    "Coral reefs are among the most biodiverse ecosystems on Earth, supporting roughly a " +
            "quarter of all marine species despite covering less than one percent of the ocean floor. " +
            "Rising water temperatures, ocean acidification, and pollution threaten these delicate structures. " +
            "Conservation efforts including marine protected areas are critical to preserving reef ecosystems.",

    "The art of negotiation is essential in both business and personal relationships. Successful " +
            "negotiators prepare thoroughly, listen actively, and seek solutions that benefit all parties " +
            "involved. Understanding the other side's needs and constraints is often more important than " +
            "aggressively pushing for your own demands, leading to more durable and satisfying agreements.",

    "Data visualization transforms raw numbers into meaningful visual stories that humans can " +
            "quickly understand and act upon. Charts, graphs, maps, and infographics reveal patterns, " +
            "trends, and outliers that might be invisible in spreadsheets of raw data. The best " +
            "visualizations are accurate, clear, and designed to communicate a specific insight effectively.",

    "The human digestive system is a remarkably complex network of organs that breaks down food " +
            "into nutrients the body can absorb and use for energy, growth, and cellular repair. " +
            "Trillions of bacteria in the gut microbiome play essential roles in digestion, immunity, " +
            "and even mental health, making gut health a frontier of modern medical research.",

    "Open water swimming requires a different set of skills compared to pool swimming. " +
            "Navigating waves, currents, and varying temperatures demands physical preparation and mental " +
            "toughness. Sighting techniques help swimmers maintain course without lane lines, while " +
            "acclimatization to cold water builds resilience. The sense of freedom in natural water is unmatched.",

    "Ancient Rome's engineering achievements continue to influence modern infrastructure design. " +
            "Roman roads, aqueducts, and concrete structures demonstrated principles of durability and " +
            "efficiency that engineers still admire today. The Pantheon's unreinforced concrete dome, " +
            "built nearly two thousand years ago, remains the largest of its kind in the world.",

    "Mindful eating involves paying full attention to the experience of eating and drinking, " +
            "both inside and outside the body. It means noticing colors, smells, textures, flavors, " +
            "and even sounds of food. This practice helps people develop a healthier relationship with " +
            "food, recognize hunger and fullness cues, and enjoy meals more thoroughly and gratefully.",

    "The concept of universal basic income has gained attention as automation threatens to " +
            "displace workers across many industries. Proponents argue it could reduce poverty and " +
            "provide economic security, while critics worry about funding costs and potential impacts " +
            "on work motivation. Pilot programs around the world are testing various models.",

    "Birds are the last living descendants of dinosaurs, carrying a lineage that stretches " +
            "back over one hundred and fifty million years. Their ability to fly has allowed them " +
            "to colonize every continent, from tropical rainforests to frozen Antarctic shores. " +
            "Migration patterns demonstrate extraordinary feats of navigation spanning thousands of miles.",

    "The practice of meditation has roots in traditions spanning thousands of years across " +
            "many cultures. Modern neuroscience has confirmed that regular meditation physically changes " +
            "brain structure, thickening regions associated with attention and emotional regulation. " +
            "Even brief daily sessions can reduce stress, improve focus, and enhance overall well-being.",

    "Graphic novels have emerged as a respected literary form that combines visual art with " +
            "narrative storytelling to explore complex themes including identity, history, trauma, and " +
            "social justice. Works by artists around the world have demonstrated that sequential art " +
            "can achieve the same emotional depth and intellectual rigor as traditional prose literature.",

    "The internet of things is connecting billions of devices from household appliances to " +
            "industrial sensors, creating vast networks of data that can be analyzed to optimize " +
            "everything from energy consumption to traffic flow. As more objects become connected, " +
            "questions about data privacy, security, and digital infrastructure become increasingly urgent.",

    "Permaculture is a design philosophy that seeks to create sustainable human habitats by " +
            "following patterns and relationships found in natural ecosystems. By observing how forests, " +
            "wetlands, and prairies function, permaculture practitioners design gardens, farms, and " +
            "communities that produce food while regenerating soil, water, and biodiversity over time.",

    "The development of writing systems roughly five thousand years ago marked one of the most " +
            "transformative innovations in human history. Writing allowed knowledge to be recorded, " +
            "transmitted across distances, and preserved for future generations. From cuneiform to " +
            "digital text, the ability to encode language has been the foundation of civilization.",

    "Documentary filmmaking has the unique power to illuminate hidden truths, amplify marginalized " +
            "voices, and spark important public conversations about the world we share. The best " +
            "documentaries combine rigorous journalism with compelling storytelling, using the visual " +
            "medium to create emotional connections that motivate audiences to think differently and act."
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                }

                // Duration selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TestDuration.entries.forEach { dur ->
                            FilterChip(
                                selected = testDuration == dur,
                                onClick = { testDuration = dur },
                                label = { Text(dur.label, maxLines = 1) }
                            )
                        }
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Repeat same text
                                    testState = TestState.IDLE
                                    typedText = ""
                                    elapsedTimeMs = 0L
                                    startTimeMs = 0L
                                    errorCount = 0
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Replay, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Repeat")
                            }
                            Button(
                                onClick = { resetTest() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Shuffle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Test")
                            }
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
