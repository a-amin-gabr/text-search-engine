# Text Search Engine

A Java-based text search engine implementing efficient string matching algorithms as part of a **Data Structures & Algorithms** course project.

## 🌐 Live Demo

Try the CLI mode directly in your browser:  
**[https://dsa.university.projects.abdallahgabr.me/](https://dsa.university.projects.abdallahgabr.me/)**

---

## 🔍 Algorithms Implemented

### 1. Boyer-Moore-Horspool Algorithm

An efficient substring search algorithm that preprocesses the pattern to skip unnecessary comparisons.

- **Time Complexity**: O(n/m) average case, O(nm) worst case
- **Space Complexity**: O(k) where k is the alphabet size
- **Use Case**: Substring and whole-word matching

```java
// Bad character shift table for pattern skipping
private static Map<Character, Integer> buildShiftTable(String pattern) {
    Map<Character, Integer> table = new HashMap<>();
    int m = pattern.length();
    for (int i = 0; i < m - 1; i++) {
        table.put(pattern.charAt(i), m - 1 - i);
    }
    return table;
}
```

### 2. Trie (Prefix Tree)

A tree-based data structure for efficient prefix searching and word indexing.

- **Time Complexity**: O(m) for search where m is the key length
- **Space Complexity**: O(n × m) for n words of average length m
- **Use Case**: Prefix-based search, autocomplete functionality

```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    List<Occurrence> occurrences = new ArrayList<>();
    boolean isEndOfWord = false;
}
```

---

## ✨ Features

### Search Modes

| Mode | Description | Algorithm |
|------|-------------|-----------|
| **Prefix** | Find words starting with the keyword | Trie traversal |
| **Substring** | Find any text containing the keyword | Boyer-Moore-Horspool |
| **Whole Word** | Match exact whole words only | Boyer-Moore-Horspool + boundary check |

### CLI Modes

1. **Standard CLI** (`cli`) - Simple command-line interface
2. **Enhanced CLI** (`enhancedcli`) - Colorized output with ANSI codes, context lines, line numbers

### Core Functionality

- **Load** - Load text files for processing
- **Search** - Find occurrences with highlighting
- **Replace** - Find and replace text with preview
- **List** - Display all matches with line/column positions
- **Stats** - Show file statistics
- **Export** - Save search/replace results to file

---

## 📁 Project Structure

```
src/
├── TextSearchApplication.java      # Main entry point
├── factory/
│   └── SearchModeFactory.java      # Factory pattern for CLI modes
├── interfaces/
│   └── SearchInterface.java        # Common interface for modes
├── models/
│   └── Occurrence.java             # Search result model
├── modes/
│   ├── CLIMode.java                # Standard CLI implementation
│   ├── EnhancedCLIMode.java        # Enhanced CLI with colors
│   └── GUISearchMode.java          # Swing GUI implementation
├── processors/
│   ├── BoyerMooreHorspool.java     # BMH search algorithm
│   ├── PrefixSearchEngine.java     # Trie-based prefix search
│   └── TextProcessor.java          # File loading and indexing
└── services/
    └── SearchService.java          # Core search/replace logic
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+ (tested with Java 25)

### Build

```bash
# Compile
javac -d out src/**/*.java

# Create JAR
jar cfe text-search-cli.jar src.TextSearchApplication -C out .
```

### Run

```bash
# Standard CLI
java -jar text-search-cli.jar cli

# Enhanced CLI (with colors)
java -jar text-search-cli.jar enhancedcli

# GUI Mode
java -jar text-search-cli.jar gui
```

---

## 📖 Usage Examples

### Load a File

```
search-engine> load sample.txt
[OK] File loaded successfully
[STATS] Lines: 100 | Characters: 5420 | Time: 12ms
```

### Search with Different Modes

```
search-engine> search prefix he
search-engine> search substring ello
search-engine> search whole hello
```

### Find and Replace

```
search-engine> replace substring old new
```

### Switch CLI Modes (Live Demo)

```
!mode cli           # Switch to standard CLI
!mode enhancedcli   # Switch to enhanced CLI with colors
```

---

## 📊 Complexity Analysis

| Operation | Time Complexity | Space Complexity |
|-----------|-----------------|------------------|
| File Load | O(n × w) | O(n × w) |
| Prefix Search | O(m + k) | O(1) |
| Substring Search | O(n × m/p) | O(p) |
| Whole Word Search | O(n × m/p) | O(p) |
| Replace | O(n × m) | O(n) |

Where:

- n = number of characters in file
- w = average word length
- m = pattern length
- p = pattern length
- k = number of matches

---

## 🛠️ Design Patterns

- **Factory Pattern** - `SearchModeFactory` creates CLI/GUI modes
- **Strategy Pattern** - Different search algorithms (`PREFIX`, `SUBSTRING`, `WHOLE_WORD`)
- **MVC-like** - Separation of modes (view), services (controller), processors (model)
