package src.processors;
import java.util.*;

import src.models.Occurrence;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    List<Occurrence> occurrences = new ArrayList<>(); 
    boolean isEndOfWord = false;
}

public class PrefixSearchEngine {
    private final TrieNode root = new TrieNode();

    public void insertWord(String word, int line, int index) {
        if (word == null || word.isEmpty()) return;
        
        TrieNode current = root;
        
        for (char l : word.toLowerCase().toCharArray()) {
            current = current.children.computeIfAbsent(l, k -> new TrieNode());
        }
        current.isEndOfWord = true;
        
        current.occurrences.add(new Occurrence(line, index, word));
    }

    public List<Occurrence> searchPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        
        TrieNode current = root;
        for (char l : prefix.toLowerCase().toCharArray()) {
            current = current.children.get(l);
            if (current == null) return Collections.emptyList();
        }

        List<Occurrence> results = new ArrayList<>();
        findAllOccurrences(current, results);
        return results;
    }

    private void findAllOccurrences(TrieNode node, List<Occurrence> results) {
        if (node.isEndOfWord) {
            results.addAll(node.occurrences);
        }
        for (TrieNode child : node.children.values()) {
            findAllOccurrences(child, results);
        }
    }
}