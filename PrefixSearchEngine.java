import java.util.*;

class Occurrence {
    int lineNumber;
    int startIndex;
    String fullWord; 

    public Occurrence(int lineNumber, int startIndex, String fullWord) {
        this.lineNumber = lineNumber;
        this.startIndex = startIndex;
        this.fullWord = fullWord;
    }
}

/* 
    * Represents a single character in the Trie.
    * Stores a list of occurrences if a word ends at this node.
*/
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    List<Occurrence> occurrences = new ArrayList<>(); 
    boolean isEndOfWord = false;
}

public class PrefixSearchEngine {
    private final TrieNode root = new TrieNode();

    /* 
        * Inserts a word into the Trie character by character.
        * Supports case-insensitivity by converting word to lowercase.
    */
    public void insertWord(String word, int line, int index) {
        if (word == null || word.isEmpty()) return;
        
        TrieNode current = root;
        // Build the path in the Trie for the word
        for (char l : word.toLowerCase().toCharArray()) {
            current = current.children.computeIfAbsent(l, k -> new TrieNode());
        }
        current.isEndOfWord = true;
        // Add the metadata (line, index) to the final node of the word
        current.occurrences.add(new Occurrence(line, index, word));
    }

    /* 
        * Navigates the Trie to find the node representing the given prefix.
        * Returns an empty list if the prefix is not found.
    */
    public List<Occurrence> searchPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        
        TrieNode current = root;
        for (char l : prefix.toLowerCase().toCharArray()) {
            current = current.children.get(l);
            if (current == null) return Collections.emptyList();
        }
        
        // After finding the prefix node, collect all words under it
        List<Occurrence> results = new ArrayList<>();
        findAllOccurrences(current, results);
        return results;
    }

    /* 
        * Helper recursive function to gather all occurrences from a starting node downwards.
        * This finds all words that start with the same prefix.
    */
    private void findAllOccurrences(TrieNode node, List<Occurrence> results) {
        if (node.isEndOfWord) {
            results.addAll(node.occurrences);
        }
        for (TrieNode child : node.children.values()) {
            findAllOccurrences(child, results);
        }
    }
}