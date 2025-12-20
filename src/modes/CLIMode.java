package src.modes;

import src.interfaces.SearchInterface;
import src.services.SearchService;
import src.services.SearchService.SearchMode;
import src.services.SearchService.SearchResult;
import src.models.Occurrence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CLIMode implements SearchInterface {
    private static final String HIGHLIGHT_MARKER = "$";
    private static final int SEPARATOR_WIDTH = 70;
    private static final String CLI_PROMPT = "search-engine> ";
    private final SearchService searchService;
    private final Scanner scanner;
    private boolean scannerActive;
    
    public CLIMode() {
        this.searchService = new SearchService();
        this.scanner = new Scanner(System.in);
        this.scannerActive = true;
    }

    @Override
    public void run() {
        printWelcome();

        while (scannerActive) {
            System.out.print(CLI_PROMPT);

            String input = safeReadLine();
            if (input == null) {
                
                break;
            }

            input = input.trim();
            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        cleanup();
    }

    private void processCommand(String input) {
        String lowerInput = input.toLowerCase();

        if (lowerInput.equals("exit")) {
            printGoodbye();
            scannerActive = false;
        } else if (lowerInput.equals("help")) {
            printHelp();
        } else if (lowerInput.startsWith("load ")) {
            handleLoad(input.substring(5).trim());
        } else if (lowerInput.startsWith("list ")) {
            handleList(input.substring(5).trim());
        } else if (lowerInput.startsWith("search ")) {
            handleSearch(input.substring(7).trim());
        } else if (lowerInput.startsWith("replace ")) {
            handleReplace(input.substring(8).trim());
        } else {
            printError("Unknown command. Type 'help' for available commands.");
        }
    }

    private void cleanup() {
        if (scanner != null) {
            scanner.close();
        }
    }

    private void handleLoad(String filePath) {
        if (filePath.isEmpty()) {
            printError("File path cannot be empty. Usage: load <file_path>");
            return;
        }

        try {
            SearchResult result = searchService.loadFile(filePath);
            printSuccess(result.getMessage());
            System.out.printf("[STATS] Lines: %d | Characters: %d | Time: %dms%n",
                    result.getCount(), result.getFileSize(), result.getExecutionTimeUs());
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    private void handleSearch(String input) {
        if (!validateFileLoaded()) {
            return;
        }

        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            printError("Usage: search <mode> <keyword>");
            return;
        }

        String modeStr = parts[0];
        String keyword = parts[1].trim();

        SearchMode mode = parseSearchModeSafe(modeStr);
        if (mode == null || !validateKeyword(keyword)) {
            return;
        }

        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        SearchResult result = searchService.search(keyword, mode, caseInsensitive);
        displayHighlightedResult(result, mode, caseInsensitive, keyword.length());
    }

    private void handleList(String input) {
        if (!validateFileLoaded()) {
            return;
        }

        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            printError("Usage: list <mode> <keyword>");
            return;
        }

        String modeStr = parts[0];
        String keyword = parts[1].trim();

        SearchMode mode = parseSearchModeSafe(modeStr);
        if (mode == null || !validateKeyword(keyword)) {
            return;
        }

        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        SearchResult result = searchService.search(keyword, mode, caseInsensitive);
        displayListResult(result, mode, caseInsensitive);
    }

    private void handleReplace(String input) {
        if (!validateFileLoaded()) {
            return;
        }

        String[] parts = input.split("\\s+", 3);
        if (parts.length < 3) {
            printError("Usage: replace <mode> <keyword> <replacement>");
            return;
        }

        String modeStr = parts[0];
        String keyword = parts[1].trim();
        String replacement = parts[2].trim();

        SearchMode mode = parseSearchModeSafe(modeStr);
        if (mode == null || !validateKeyword(keyword)) {
            return;
        }

        if (replacement.isEmpty()) {
            printError("Replacement cannot be empty.");
            return;
        }

        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        SearchResult result = searchService.search(keyword, mode, caseInsensitive);
        displayReplacedResult(result, mode, caseInsensitive, replacement);
    }

    private String safeReadLine() {
        try {
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            } else {
                scannerActive = false;
                return null;
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            scannerActive = false;
            return null;
        }
    }

    private boolean askCaseInsensitive() {
        System.out.print("[OPTION] Case insensitive? (y/n, default: y): ");
        String response = safeReadLine();

        if (response == null) {
            return true; 
        }

        return !response.trim().equalsIgnoreCase("n");
    }

    private SearchMode parseSearchModeSafe(String modeStr) {
        try {
            return parseSearchMode(modeStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
            return null;
        }
    }

    private SearchMode parseSearchMode(String mode) {
        return switch (mode) {
            case "prefix", "p" -> SearchMode.PREFIX;
            case "substring", "sub", "s" -> SearchMode.SUBSTRING;
            case "whole", "whole-word", "w" -> SearchMode.WHOLE_WORD;
            default -> throw new IllegalArgumentException(
                    "Invalid mode: '" + mode + "'. Valid modes: prefix|p, substring|sub|s, whole|w");
        };
    }

    private boolean validateFileLoaded() {
        if (!searchService.isFileLoaded()) {
            printError("No file loaded. Use 'load <file_path>' first.");
            return false;
        }
        return true;
    }

    private boolean validateKeyword(String keyword) {
        if (keyword.isEmpty()) {
            printError("Keyword cannot be empty.");
            return false;
        }
        return true;
    }

    private void displayListResult(SearchResult result, SearchMode mode, boolean caseInsensitive) {
        printSeparator();

        if (!result.isSuccess()) {
            printError(result.getMessage());
            printSeparator();
            return;
        }

        System.out.printf("[LIST] Mode: %s | Case: %s | Found: %d occurrences%n",
                mode, caseInsensitive ? "insensitive" : "sensitive", result.getCount());
        System.out.printf("[TIME] Execution time: %d microseconds%n", result.getExecutionTimeUs());
        printDivider();

        List<Occurrence> occurrences = result.getResults();
        if (occurrences != null && !occurrences.isEmpty()) {
            for (int i = 0; i < occurrences.size(); i++) {
                Occurrence occ = occurrences.get(i);
                System.out.printf("%3d. [Line %d, Col %d] \"%s\"%n",
                        i + 1, occ.getLineNumber(), occ.getStartIndex(), occ.getFullWord());
            }
        } else {
            System.out.println("[INFO] No occurrences found.");
        }

        printSeparator();
    }

    private void displayHighlightedResult(SearchResult result, SearchMode mode,
            boolean caseInsensitive, int keywordLength) {
        printSeparator();

        if (!result.isSuccess()) {
            printError(result.getMessage());
            printSeparator();
            return;
        }

        System.out.printf("[SEARCH] Mode: %s | Case: %s | Found: %d occurrences%n",
                mode, caseInsensitive ? "insensitive" : "sensitive", result.getCount());
        System.out.printf("[TIME] Execution time: %d microseconds%n", result.getExecutionTimeUs());
        printDivider();

        List<Occurrence> occurrences = result.getResults();
        List<String> fileLines = searchService.getFileLines();

        displayProcessedFile(fileLines, occurrences, keywordLength, ProcessMode.HIGHLIGHT, null);

        printSeparator();

        if (occurrences != null && !occurrences.isEmpty()) {
            promptSaveHighlighted(occurrences, keywordLength);
        }
    }

    private void displayReplacedResult(SearchResult result, SearchMode mode,
            boolean caseInsensitive, String replacement) {
        printSeparator();

        if (!result.isSuccess()) {
            printError(result.getMessage());
            printSeparator();
            return;
        }

        System.out.printf("[REPLACE] Mode: %s | Case: %s | Replacing: %d occurrences%n",
                mode, caseInsensitive ? "insensitive" : "sensitive", result.getCount());
        printDivider();

        List<Occurrence> occurrences = result.getResults();
        List<String> fileLines = searchService.getFileLines();

        if (occurrences == null || occurrences.isEmpty()) {
            System.out.println("[INFO] No occurrences found to replace.");
            
            for (String line : fileLines) {
                System.out.println(line);
            }
        } else {
            displayProcessedFile(fileLines, occurrences, 0, ProcessMode.REPLACE, replacement);
        }

        printSeparator();

        if (occurrences != null && !occurrences.isEmpty()) {
            promptSaveReplaced(occurrences, replacement);
        }
    }

    private enum ProcessMode {
        HIGHLIGHT, 
        REPLACE 
    }

    private void displayProcessedFile(List<String> fileLines, List<Occurrence> occurrences,
            int keywordLen, ProcessMode mode, String replacement) {

        int occIndex = 0;

        for (int lineNum = 1; lineNum <= fileLines.size(); lineNum++) {
            String line = fileLines.get(lineNum - 1);

            if (occurrences == null || occIndex >= occurrences.size() ||
                    occurrences.get(occIndex).getLineNumber() != lineNum) {
                System.out.println(line);
                continue;
            }

            String processedLine = buildProcessedLine(line, occurrences, occIndex,
                    lineNum, keywordLen, mode, replacement);
            System.out.println(processedLine);

            while (occIndex < occurrences.size() &&
                    occurrences.get(occIndex).getLineNumber() == lineNum) {
                occIndex++;
            }
        }
    }

    private String buildProcessedLine(String line, List<Occurrence> occurrences,
            int startIndex, int lineNum, int keywordLen, ProcessMode mode, String replacement) {

        StringBuilder result = new StringBuilder();
        int charIndex = 0;
        int occIndex = startIndex;

        while (charIndex < line.length()) {
            
            if (occIndex < occurrences.size() &&
                    occurrences.get(occIndex).getLineNumber() == lineNum &&
                    occurrences.get(occIndex).getStartIndex() == charIndex) {

                Occurrence occ = occurrences.get(occIndex);
                int matchLen = occ.getFullWord().length();

                if (mode == ProcessMode.HIGHLIGHT) {
                    
                    result.append(HIGHLIGHT_MARKER);
                    result.append(line, charIndex, charIndex + matchLen);
                    result.append(HIGHLIGHT_MARKER);
                    charIndex += matchLen;
                } else {
                    
                    result.append(replacement);
                    charIndex += matchLen;
                }
                occIndex++;
            } else {
                
                result.append(line.charAt(charIndex));
                charIndex++;
            }
        }

        return result.toString();
    }

    private void promptSaveHighlighted(List<Occurrence> occurrences, int keywordLen) {
        System.out.print("\n[OPTION] Save highlighted results to file? (y/n): ");
        String response = safeReadLine();

        if (response != null && response.trim().equalsIgnoreCase("y")) {
            System.out.print("[INPUT] Enter output file path: ");
            String filePath = safeReadLine();

            if (filePath != null && !filePath.trim().isEmpty()) {
                saveProcessedFile(occurrences, filePath.trim(), ProcessMode.HIGHLIGHT, null, keywordLen);
            } else {
                printError("Invalid file path.");
            }
        }
    }

    private void promptSaveReplaced(List<Occurrence> occurrences, String replacement) {
        System.out.print("\n[OPTION] Save replaced results to file? (y/n): ");
        String response = safeReadLine();

        if (response != null && response.trim().equalsIgnoreCase("y")) {
            System.out.print("[INPUT] Enter output file path: ");
            String filePath = safeReadLine();

            if (filePath != null && !filePath.trim().isEmpty()) {
                saveProcessedFile(occurrences, filePath.trim(), ProcessMode.REPLACE, replacement, 0);
            } else {
                printError("Invalid file path.");
            }
        }
    }

    private void saveProcessedFile(List<Occurrence> occurrences, String filePath,
            ProcessMode mode, String replacement, int keywordLen) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            List<String> fileLines = searchService.getFileLines();
            int occIndex = 0;

            for (int lineNum = 1; lineNum <= fileLines.size(); lineNum++) {
                String line = fileLines.get(lineNum - 1);
                String outputLine;

                if (occIndex >= occurrences.size() ||
                        occurrences.get(occIndex).getLineNumber() != lineNum) {
                    outputLine = line;
                } else {
                    outputLine = buildProcessedLine(line, occurrences, occIndex,
                            lineNum, keywordLen, mode, replacement);

                    while (occIndex < occurrences.size() &&
                            occurrences.get(occIndex).getLineNumber() == lineNum) {
                        occIndex++;
                    }
                }

                writer.write(outputLine);
                writer.newLine();
            }

            printSuccess("Results saved to: " + filePath);
        } catch (IOException e) {
            printError("Failed to save file: " + e.getMessage());
        }
    }

    private void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    private void printError(String message) {
        System.err.println("[ERROR] " + message);
    }

    private void printSeparator() {
        System.out.println("=".repeat(SEPARATOR_WIDTH));
    }

    private void printDivider() {
        System.out.println("-".repeat(SEPARATOR_WIDTH));
    }

    private void printHelp() {
        printSeparator();
        System.out.println("[HELP] AVAILABLE COMMANDS");
        printSeparator();
        System.out.println();
        System.out.println("  File Operations:");
        System.out.println("    load <file_path>                         Load a text file");
        System.out.println();
        System.out.println("  Search Operations:");
        System.out.println("    search  <mode> <keyword>                 Search and highlight");
        System.out.println("    list    <mode> <keyword>                 List all matches");
        System.out.println("    replace <mode> <keyword> <replacement>   Find and replace");
        System.out.println();
        System.out.println("  Search Modes:");
        System.out.println("    prefix | p       Match words starting with keyword");
        System.out.println("    substring | s    Match any text containing keyword");
        System.out.println("    whole | w        Match exact whole words only");
        System.out.println();
        System.out.println("  Other Commands:");
        System.out.println("    help             Show this help message");
        System.out.println("    exit             Exit the program");
        System.out.println();
        printSeparator();
    }

    private void printWelcome() {
        System.out.println();
        printSeparator();
        System.out.println("           TEXT SEARCH ENGINE - CLI MODE");
        printSeparator();
        System.out.println("Type 'help' for a list of commands.");
        System.out.println();
    }

    private void printGoodbye() {
        System.out.println();
        printSeparator();
        System.out.println("Thank you for using Text Search Engine!");
        printSeparator();
        System.out.println();
    }
}
