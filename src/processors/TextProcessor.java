package src.processors;
import java.io.*;
import java.util.*;

public class TextProcessor {
    private List<String> lines = new ArrayList<>();
    
    private PrefixSearchEngine prefixEngine = new PrefixSearchEngine();

    public void loadFile(String path) throws IOException {
        lines.clear(); 
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        int lineNum = 1;
        
        while ((line = br.readLine()) != null) {
            lines.add(line);
            
            indexLine(line, lineNum); 
            lineNum++;
        }
        br.close();
    }

    private void indexLine(String line, int lineNum) {
        
        String[] words = line.split("\\s+");
        int lastFoundIndex = 0;
        
        for (String word : words) {
            
            String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (!cleanWord.isEmpty()) {
                
                int startIndex = line.indexOf(word, lastFoundIndex);
                prefixEngine.insertWord(cleanWord, lineNum, startIndex);
                
                lastFoundIndex = startIndex + word.length();
            }
        }
    }

    public List<String> getLines() {
        return lines;
    }

    public PrefixSearchEngine getPrefixEngine() {
        return prefixEngine;
    }

    public String normalize(String text) {
        return text.toLowerCase();
    }
}