package src.services;

import src.processors.TextProcessor;
import src.processors.BoyerMooreHorspool;
import src.models.Occurrence;
import java.io.*;
import java.util.*;

public class SearchService {
    private TextProcessor textProcessor;
    private String currentFilePath;
    
    public SearchService() {
        this.textProcessor = new TextProcessor();
    }

    public SearchResult loadFile(String filePath) throws IOException {
        File file = new File(filePath);
        
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }
        if (!file.canRead()) {
            throw new IOException("Permission denied: Cannot read " + filePath);
        }
        
        long startTime = System.nanoTime();
        textProcessor.loadFile(filePath);
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        
        this.currentFilePath = filePath;
        
        return new SearchResult(
            true,
            "File loaded successfully",
            textProcessor.getLines().size(),
            getTotalCharacters(),
            duration,
            null
        );
    }

    public SearchResult search(String keyword, SearchMode mode, boolean caseInsensitive) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SearchResult(false, "Keyword cannot be empty", 0, 0, 0, null);
        }
        
        long startTime = System.nanoTime();
        List<Occurrence> results = new ArrayList<>();
        
        try {
            switch (mode) {
                case PREFIX:
                    results = searchPrefix(keyword, caseInsensitive);
                    break;
                case SUBSTRING:
                    results = searchSubstring(keyword, caseInsensitive);
                    break;
                case WHOLE_WORD:
                    results = searchWholeWord(keyword, caseInsensitive);
                    break;
            }

            results.sort((occ1, occ2) -> {
                if (occ1.getLineNumber() != occ2.getLineNumber()) {
                    return Integer.compare(occ1.getLineNumber(), occ2.getLineNumber());
                }
                return Integer.compare(occ1.getStartIndex(), occ2.getStartIndex());
            });
            
            long duration = (System.nanoTime() - startTime) / 1000;
            return new SearchResult(
                true,
                "Search completed",
                results.size(),
                0,
                duration,
                results
            );
        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1000;
            return new SearchResult(
                false,
                "Search error: " + e.getMessage(),
                0,
                0,
                duration,
                null
            );
        }
    }

    public SearchResult replace(String keyword, String replacement, String outputPath, 
                                boolean caseInsensitive) {
        if (replacement == null) {
            return new SearchResult(false, "Replacement target cannot be null", 0, 0, 0, null);
        }
        
        long startTime = System.nanoTime();
        
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
            int replacementCount = 0;
            
            for (String line : textProcessor.getLines()) {
                String processedLine;
                
                if (caseInsensitive) {
                    processedLine = line.replaceAll("(?i)" + escapeRegex(keyword), replacement);
                } else {
                    processedLine = line.replace(keyword, replacement);
                }

                int count = 0;
                String temp = line;
                if (caseInsensitive) {
                    temp = line.toLowerCase();
                    keyword = keyword.toLowerCase();
                }
                int index = 0;
                while ((index = temp.indexOf(keyword, index)) != -1) {
                    count++;
                    index += keyword.length();
                }
                replacementCount += count;
                
                writer.write(processedLine);
                writer.newLine();
            }
            writer.close();
            
            long duration = (System.nanoTime() - startTime) / 1000;
            return new SearchResult(
                true,
                "Replacement completed. File saved to: " + outputPath,
                replacementCount,
                0,
                duration,
                null
            );
        } catch (IOException e) {
            long duration = (System.nanoTime() - startTime) / 1000;
            return new SearchResult(
                false,
                "Replace error: " + e.getMessage(),
                0,
                0,
                duration,
                null
            );
        }
    }

    public FileStats getFileStats() {
        return new FileStats(
            currentFilePath,
            textProcessor.getLines().size(),
            getTotalCharacters(),
            new File(currentFilePath).length()
        );
    }

    public boolean isFileLoaded() {
        return currentFilePath != null && !textProcessor.getLines().isEmpty();
    }

    public List<String> getFileLines() {
        return textProcessor.getLines();
    }

    private List<Occurrence> searchPrefix(String keyword, boolean caseInsensitive) {
        List<Occurrence> results = new ArrayList<>();
        
        for (int i = 0; i < textProcessor.getLines().size(); i++) {
            String line = textProcessor.getLines().get(i);
            String searchLine = caseInsensitive ? line.toLowerCase() : line;
            String searchKeyword = caseInsensitive ? keyword.toLowerCase() : keyword;
            int index = 0;

            while ((index = searchLine.indexOf(searchKeyword, index)) != -1) {
                
                if (index == 0 || !Character.isLetterOrDigit(searchLine.charAt(index - 1))) {
                    
                    int endIndex = index + keyword.length();
                    while (endIndex < line.length() && Character.isLetterOrDigit(line.charAt(endIndex))) {
                        endIndex++;
                    }
                    String fullWord = line.substring(index, endIndex);
                    results.add(new Occurrence(i + 1, index, fullWord));
                }
                index += keyword.length();
            }
        }

        return results;
    }
    
    private List<Occurrence> searchSubstring(String keyword, boolean caseInsensitive) {
        List<Occurrence> results = new ArrayList<>();
        
        for (int i = 0; i < textProcessor.getLines().size(); i++) {
            String line = textProcessor.getLines().get(i);
            String searchLine = caseInsensitive ? line.toLowerCase() : line;
            String searchKeyword = caseInsensitive ? keyword.toLowerCase() : keyword;
            int index = 0;
            
            while ((index = searchLine.indexOf(searchKeyword, index)) != -1) {
                String originalWord = line.substring(index, 
                    Math.min(index + keyword.length(), line.length()));
                results.add(new Occurrence(i + 1, index, originalWord));
                index += keyword.length();
            }
        }
        
        return results;
    }
    
    private List<Occurrence> searchWholeWord(String keyword, boolean caseInsensitive) {
        List<Occurrence> results = new ArrayList<>();
        
        for (int i = 0; i < textProcessor.getLines().size(); i++) {
            String line = textProcessor.getLines().get(i);
            String searchLine = caseInsensitive ? line.toLowerCase() : line;
            String searchKeyword = caseInsensitive ? keyword.toLowerCase() : keyword;
            int index = 0;
            
            while ((index = searchLine.indexOf(searchKeyword, index)) != -1) {
                if (isWholeWordMatch(searchLine, index, searchKeyword.length())) {
                    String originalWord = line.substring(index, 
                        Math.min(index + keyword.length(), line.length()));
                    results.add(new Occurrence(i + 1, index, originalWord));
                }
                index += keyword.length();
            }
        }
        
        return results;
    }
    
    private boolean isWholeWordMatch(String line, int start, int length) {
        boolean before = (start == 0) || !Character.isLetterOrDigit(line.charAt(start - 1));
        boolean after = (start + length == line.length()) || 
                       !Character.isLetterOrDigit(line.charAt(start + length));
        return before && after;
    }
    
    private int getTotalCharacters() {
        return textProcessor.getLines().stream().mapToInt(String::length).sum();
    }
    
    private String escapeRegex(String s) {
        return s.replaceAll("[^a-zA-Z0-9]", "\\\\$0");
    }

    public enum SearchMode {
        PREFIX, SUBSTRING, WHOLE_WORD
    }
    
    public static class SearchResult {
        public boolean success;
        public String message;
        public int count;
        public long fileSize;
        public long executionTimeUs;
        public List<Occurrence> results;
        
        public SearchResult(boolean success, String message, int count, long fileSize, 
                          long executionTimeUs, List<Occurrence> results) {
            this.success = success;
            this.message = message;
            this.count = count;
            this.fileSize = fileSize;
            this.executionTimeUs = executionTimeUs;
            this.results = results;
        }

        public String getMessage() {
            return message;
        }

        public List<Occurrence> getResults() {
            return results;
        }

        public int getCount() {
            return count;
        }

        public long getExecutionTimeUs() {
            return executionTimeUs;
        }

        public long getFileSize() {
            return fileSize;
        }

        public boolean isSuccess() {
            return success;
        }
    }
    
    public static class FileStats {
        public String filePath;
        public int lineCount;
        public int characterCount;
        public long fileSizeBytes;
        
        public FileStats(String filePath, int lineCount, int characterCount, long fileSizeBytes) {
            this.filePath = filePath;
            this.lineCount = lineCount;
            this.characterCount = characterCount;
            this.fileSizeBytes = fileSizeBytes;
        }
    }
}