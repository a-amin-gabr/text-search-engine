package src.modes;

import src.interfaces.SearchInterface;
import src.services.SearchService;
import src.services.SearchService.SearchMode;
import src.services.SearchService.SearchResult;
import src.models.Occurrence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EnhancedCLIMode implements SearchInterface {

    private static final String RESET = "\u001B[0m";

    private static final String BOLD = "\u001B[1m";

    private static final String DIM = "\u001B[2m";

    private static final String RED = "\u001B[31m";

    private static final String GREEN = "\u001B[32m";

    private static final String YELLOW = "\u001B[33m";

    private static final String BLUE = "\u001B[34m";

    private static final String MAGENTA = "\u001B[35m";

    private static final String CYAN = "\u001B[36m";

    private static final String WHITE = "\u001B[37m";

    private static final int SEPARATOR_WIDTH = 80;

    private static final String CLI_PROMPT = "enhanced> ";

    private static final int DEFAULT_CONTEXT_LINES = 0;

    private static final String CMD_EXIT = "exit";
    private static final String CMD_QUIT = "quit";
    private static final String CMD_Q = "q";
    private static final String CMD_HELP = "help";
    private static final String CMD_HELP_ALT = "?";
    private static final String CMD_CLEAR = "clear";
    private static final String CMD_CLS = "cls";
    private static final String CMD_LOAD = "load ";
    private static final String CMD_LIST = "list ";
    private static final String CMD_SEARCH = "search ";
    private static final String CMD_REPLACE = "replace ";
    private static final String CMD_CONTEXT = "context ";
    private static final String CMD_LINES_ON = "lines on";
    private static final String CMD_LINES_OFF = "lines off";
    private static final String CMD_STATS = "stats";

    @FunctionalInterface
    private interface SaveAction {
        void save(String filePath);
    }

    private final SearchService searchService;

    private final Scanner scanner;

    private boolean scannerActive;

    private int contextLines;

    private boolean showLineNumbers;

    public EnhancedCLIMode() {
        this.searchService = new SearchService();
        this.scanner = new Scanner(System.in);
        this.scannerActive = true;
        this.contextLines = DEFAULT_CONTEXT_LINES;
        this.showLineNumbers = true;
    }

    @Override
    public void run() {
        printWelcome();

        while (scannerActive) {
            System.out.print(colorize(CLI_PROMPT, CYAN + BOLD));

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

        if (lowerInput.equals(CMD_EXIT) || lowerInput.equals(CMD_QUIT) || lowerInput.equals(CMD_Q)) {
            printGoodbye();
            scannerActive = false;
        } else if (lowerInput.equals(CMD_HELP) || lowerInput.equals(CMD_HELP_ALT)) {
            printHelp();
        } else if (lowerInput.equals(CMD_CLEAR) || lowerInput.equals(CMD_CLS)) {
            clearScreen();
        } else if (lowerInput.startsWith(CMD_LOAD)) {
            handleLoad(input.substring(CMD_LOAD.length()).trim());
        } else if (lowerInput.startsWith(CMD_LIST)) {
            handleList(input.substring(CMD_LIST.length()).trim());
        } else if (lowerInput.startsWith(CMD_SEARCH)) {
            handleSearch(input.substring(CMD_SEARCH.length()).trim());
        } else if (lowerInput.startsWith(CMD_REPLACE)) {
            handleReplace(input.substring(CMD_REPLACE.length()).trim());
        } else if (lowerInput.startsWith(CMD_CONTEXT)) {
            handleContext(input.substring(CMD_CONTEXT.length()).trim());
        } else if (lowerInput.equals(CMD_LINES_ON)) {
            showLineNumbers = true;
            printSuccess("Line numbers enabled.");
        } else if (lowerInput.equals(CMD_LINES_OFF)) {
            showLineNumbers = false;
            printSuccess("Line numbers disabled.");
        } else if (lowerInput.equals(CMD_STATS)) {
            handleStats();
        } else {
            printError("Unknown command: '" + input + "'. Type 'help' for available commands.");
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
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
            System.out.printf("%s[STATS]%s Lines: %s%d%s | Characters: %s%d%s | Time: %s%dms%s%n",
                    DIM, RESET,
                    CYAN, result.getCount(), RESET,
                    CYAN, result.getFileSize(), RESET,
                    YELLOW, result.getExecutionTimeUs(), RESET);
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    private void handleContext(String input) {
        try {
            int lines = Integer.parseInt(input.trim());
            if (lines < 0) {
                printError("Context lines must be non-negative.");
                return;
            }
            contextLines = lines;
            printSuccess("Context set to " + lines + " lines.");
        } catch (NumberFormatException e) {
            printError("Invalid number. Usage: context <number>");
        }
    }

    private void handleStats() {
        if (!validateFileLoaded()) {
            return;
        }

        var stats = searchService.getFileStats();
        printSeparator();
        System.out.println(colorize("  FILE STATISTICS", BOLD + CYAN));
        printDivider();
        System.out.printf("  %sFile:%s      %s%n", DIM, RESET, stats.filePath);
        System.out.printf("  %sLines:%s     %s%d%s%n", DIM, RESET, CYAN, stats.lineCount, RESET);
        System.out.printf("  %sChars:%s     %s%d%s%n", DIM, RESET, CYAN, stats.characterCount, RESET);
        System.out.printf("  %sSize:%s      %s%d bytes%s%n", DIM, RESET, CYAN, stats.fileSizeBytes, RESET);
        printSeparator();
    }

    private void handleSearch(String input) {
        if (!validateFileLoaded()) {
            return;
        }

        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            printError("Usage: search <mode> <pattern>");
            printInfo("Modes: prefix|p, substring|s, whole|w, regex|r");
            return;
        }

        String modeStr = parts[0].toLowerCase();
        String pattern = parts[1].trim();

        if (pattern.isEmpty()) {
            printError("Pattern cannot be empty.");
            return;
        }

        if (modeStr.equals("regex") || modeStr.equals("r")) {
            handleRegexSearch(pattern);
            return;
        }

        SearchMode mode = parseSearchModeSafe(modeStr);
        if (mode == null) {
            return;
        }

        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        SearchResult result = searchService.search(pattern, mode, caseInsensitive);
        displayHighlightedResult(result, mode.toString(), pattern.length());
    }

    private void handleRegexSearch(String patternStr) {
        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        try {
            int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
            Pattern pattern = Pattern.compile(patternStr, flags);

            List<String> fileLines = searchService.getFileLines();
            List<RegexMatch> matches = new ArrayList<>();

            for (int lineNum = 0; lineNum < fileLines.size(); lineNum++) {
                String line = fileLines.get(lineNum);
                Matcher matcher = pattern.matcher(line);

                while (matcher.find()) {
                    matches.add(new RegexMatch(lineNum + 1, matcher.start(),
                            matcher.end() - matcher.start(), matcher.group()));
                }
            }

            displayRegexResult(matches, patternStr, caseInsensitive);

        } catch (PatternSyntaxException e) {
            printError("Invalid regex pattern: " + e.getDescription());
        }
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

        String modeStr = parts[0].toLowerCase();
        String keyword = parts[1].trim();

        if (modeStr.equals("regex") || modeStr.equals("r")) {
            handleRegexList(keyword);
            return;
        }

        SearchMode mode = parseSearchModeSafe(modeStr);
        if (mode == null || !validateKeyword(keyword)) {
            return;
        }

        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        SearchResult result = searchService.search(keyword, mode, caseInsensitive);
        displayListResult(result, mode.toString(), caseInsensitive);
    }

    private void handleRegexList(String patternStr) {
        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        try {
            int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
            Pattern pattern = Pattern.compile(patternStr, flags);

            List<String> fileLines = searchService.getFileLines();
            List<RegexMatch> matches = new ArrayList<>();

            for (int lineNum = 0; lineNum < fileLines.size(); lineNum++) {
                String line = fileLines.get(lineNum);
                Matcher matcher = pattern.matcher(line);

                while (matcher.find()) {
                    matches.add(new RegexMatch(lineNum + 1, matcher.start(),
                            matcher.end() - matcher.start(), matcher.group()));
                }
            }

            displayRegexListResult(matches, patternStr, caseInsensitive);

        } catch (PatternSyntaxException e) {
            printError("Invalid regex pattern: " + e.getDescription());
        }
    }

    private void handleReplace(String input) {
        if (!validateFileLoaded()) {
            return;
        }

        String[] parts = input.split("\\s+", 3);
        if (parts.length < 3) {
            printError("Usage: replace <mode> <pattern> <replacement>");
            return;
        }

        String modeStr = parts[0].toLowerCase();
        String pattern = parts[1].trim();
        String replacement = parts[2].trim();

        if (pattern.isEmpty()) {
            printError("Pattern cannot be empty.");
            return;
        }

        if (modeStr.equals("regex") || modeStr.equals("r")) {
            handleRegexReplace(pattern, replacement);
            return;
        }

        SearchMode mode = parseSearchModeSafe(modeStr);
        if (mode == null) {
            return;
        }

        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        SearchResult result = searchService.search(pattern, mode, caseInsensitive);
        displayReplacedResult(result, mode.toString(), replacement);
    }

    private void handleRegexReplace(String patternStr, String replacement) {
        boolean caseInsensitive = askCaseInsensitive();
        if (!scannerActive)
            return;

        try {
            int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
            Pattern pattern = Pattern.compile(patternStr, flags);

            List<String> fileLines = searchService.getFileLines();
            List<String> replacedLines = new ArrayList<>();
            int matchCount = 0;

            for (String line : fileLines) {
                Matcher matcher = pattern.matcher(line);
                int count = 0;
                while (matcher.find())
                    count++;
                matchCount += count;

                replacedLines.add(pattern.matcher(line).replaceAll(replacement));
            }

            displayRegexReplaceResult(replacedLines, matchCount, patternStr, replacement, caseInsensitive);

        } catch (PatternSyntaxException e) {
            printError("Invalid regex pattern: " + e.getDescription());
        }
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
        System.out.print(colorize("[?] ", YELLOW) + "Case insensitive? (Y/n): ");
        String response = safeReadLine();

        if (response == null) {
            return true;
        }

        return !response.trim().equalsIgnoreCase("n");
    }

    private SearchMode parseSearchModeSafe(String modeStr) {
        try {
            return parseSearchMode(modeStr);
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
                    "Invalid mode: '" + mode + "'. Use: prefix|p, substring|s, whole|w, regex|r");
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

    private void displayListResult(SearchResult result, String mode, boolean caseInsensitive) {
        printSeparator();

        if (!result.isSuccess()) {
            printError(result.getMessage());
            printSeparator();
            return;
        }

        System.out.printf("%s[LIST]%s Mode: %s%s%s | Case: %s | Found: %s%d%s occurrences%n",
                BOLD, RESET,
                CYAN, mode, RESET,
                caseInsensitive ? "insensitive" : "sensitive",
                GREEN + BOLD, result.getCount(), RESET);
        System.out.printf("%s[TIME]%s %s%d%s microseconds%n", DIM, RESET, YELLOW, result.getExecutionTimeUs(), RESET);
        printDivider();

        List<Occurrence> occurrences = result.getResults();
        if (occurrences != null && !occurrences.isEmpty()) {
            for (int i = 0; i < occurrences.size(); i++) {
                Occurrence occ = occurrences.get(i);
                System.out.printf("  %s%3d.%s %s[Line %d, Col %d]%s \"%s%s%s\"%n",
                        DIM, i + 1, RESET,
                        CYAN, occ.getLineNumber(), occ.getStartIndex(), RESET,
                        RED + BOLD, occ.getFullWord(), RESET);
            }
        } else {
            printInfo("No occurrences found.");
        }

        printSeparator();
    }

    private void displayRegexListResult(List<RegexMatch> matches, String pattern, boolean caseInsensitive) {
        printSeparator();

        System.out.printf("%s[REGEX LIST]%s Pattern: %s%s%s | Case: %s | Found: %s%d%s matches%n",
                BOLD + MAGENTA, RESET,
                YELLOW, pattern, RESET,
                caseInsensitive ? "insensitive" : "sensitive",
                GREEN + BOLD, matches.size(), RESET);
        printDivider();

        if (!matches.isEmpty()) {
            for (int i = 0; i < matches.size(); i++) {
                RegexMatch m = matches.get(i);
                System.out.printf("  %s%3d.%s %s[Line %d, Col %d]%s \"%s%s%s\"%n",
                        DIM, i + 1, RESET,
                        CYAN, m.lineNum(), m.startIndex(), RESET,
                        RED + BOLD, m.matchedText(), RESET);
            }
        } else {
            printInfo("No matches found.");
        }

        printSeparator();
    }

    private void displayHighlightedResult(SearchResult result, String mode, int keywordLength) {
        printSeparator();

        if (!result.isSuccess()) {
            printError(result.getMessage());
            printSeparator();
            return;
        }

        System.out.printf("%s[SEARCH]%s Mode: %s%s%s | Found: %s%d%s occurrences%n",
                BOLD, RESET,
                CYAN, mode, RESET,
                GREEN + BOLD, result.getCount(), RESET);
        System.out.printf("%s[TIME]%s %s%d%s microseconds%n", DIM, RESET, YELLOW, result.getExecutionTimeUs(), RESET);
        printDivider();

        List<Occurrence> occurrences = result.getResults();
        List<String> fileLines = searchService.getFileLines();

        displayHighlightedFile(fileLines, occurrences);

        printSeparator();
        printMatchSummary(result.getCount());

        if (occurrences != null && !occurrences.isEmpty()) {
            promptSaveHighlighted(occurrences);
        }
    }

    private void displayRegexResult(List<RegexMatch> matches, String pattern, boolean caseInsensitive) {
        printSeparator();

        System.out.printf("%s[REGEX SEARCH]%s Pattern: %s%s%s | Case: %s | Found: %s%d%s matches%n",
                BOLD + MAGENTA, RESET,
                YELLOW, pattern, RESET,
                caseInsensitive ? "insensitive" : "sensitive",
                GREEN + BOLD, matches.size(), RESET);
        printDivider();

        List<String> fileLines = searchService.getFileLines();
        displayRegexHighlightedFile(fileLines, matches);

        printSeparator();
        printMatchSummary(matches.size());

        if (!matches.isEmpty()) {
            promptSaveRegex(pattern, caseInsensitive);
        }
    }

    private void displayHighlightedFile(List<String> fileLines, List<Occurrence> occurrences) {
        
        Set<Integer> linesToShow = calculateLinesToShow(occurrences, fileLines.size());

        int occIndex = 0;
        int lastPrintedLine = 0;

        for (int lineNum = 1; lineNum <= fileLines.size(); lineNum++) {
            
            if (contextLines > 0 && !linesToShow.contains(lineNum)) {
                continue;
            }

            if (contextLines > 0 && lastPrintedLine > 0 && lineNum > lastPrintedLine + 1) {
                System.out.println(colorize("    ...", DIM));
            }
            lastPrintedLine = lineNum;

            String line = fileLines.get(lineNum - 1);
            String linePrefix = showLineNumbers ? formatLineNumber(lineNum) : "";

            if (occurrences == null || occIndex >= occurrences.size() ||
                    occurrences.get(occIndex).getLineNumber() != lineNum) {
                System.out.println(linePrefix + line);
                continue;
            }

            StringBuilder result = new StringBuilder();
            int charIndex = 0;
            int currentOccIndex = occIndex;

            while (charIndex < line.length()) {
                if (currentOccIndex < occurrences.size() &&
                        occurrences.get(currentOccIndex).getLineNumber() == lineNum &&
                        occurrences.get(currentOccIndex).getStartIndex() == charIndex) {

                    Occurrence occ = occurrences.get(currentOccIndex);
                    int matchLen = occ.getFullWord().length();

                    result.append(RED + BOLD);
                    result.append(line, charIndex, charIndex + matchLen);
                    result.append(RESET);

                    charIndex += matchLen;
                    currentOccIndex++;
                } else {
                    result.append(line.charAt(charIndex));
                    charIndex++;
                }
            }

            System.out.println(linePrefix + result.toString());

            while (occIndex < occurrences.size() &&
                    occurrences.get(occIndex).getLineNumber() == lineNum) {
                occIndex++;
            }
        }
    }

    private Set<Integer> calculateLinesToShow(List<Occurrence> occurrences, int totalLines) {
        Set<Integer> lines = new TreeSet<>();

        if (contextLines == 0 || occurrences == null || occurrences.isEmpty()) {
            for (int i = 1; i <= totalLines; i++) {
                lines.add(i);
            }
            return lines;
        }

        for (Occurrence occ : occurrences) {
            int line = occ.getLineNumber();
            int start = Math.max(1, line - contextLines);
            int end = Math.min(totalLines, line + contextLines);

            for (int i = start; i <= end; i++) {
                lines.add(i);
            }
        }

        return lines;
    }

    private void displayRegexHighlightedFile(List<String> fileLines, List<RegexMatch> matches) {
        int matchIndex = 0;

        for (int lineNum = 1; lineNum <= fileLines.size(); lineNum++) {
            String line = fileLines.get(lineNum - 1);
            String linePrefix = showLineNumbers ? formatLineNumber(lineNum) : "";

            if (matchIndex >= matches.size() || matches.get(matchIndex).lineNum() != lineNum) {
                System.out.println(linePrefix + line);
                continue;
            }

            StringBuilder result = new StringBuilder();
            int charIndex = 0;

            while (charIndex < line.length()) {
                if (matchIndex < matches.size() &&
                        matches.get(matchIndex).lineNum() == lineNum &&
                        matches.get(matchIndex).startIndex() == charIndex) {

                    RegexMatch m = matches.get(matchIndex);

                    result.append(RED + BOLD);
                    result.append(line, charIndex, charIndex + m.length());
                    result.append(RESET);

                    charIndex += m.length();
                    matchIndex++;
                } else {
                    result.append(line.charAt(charIndex));
                    charIndex++;
                }
            }

            System.out.println(linePrefix + result.toString());
        }
    }

    private void displayReplacedResult(SearchResult result, String mode, String replacement) {
        printSeparator();

        if (!result.isSuccess()) {
            printError(result.getMessage());
            printSeparator();
            return;
        }

        System.out.printf("%s[REPLACE]%s Mode: %s%s%s | Replacing: %s%d%s occurrences%n",
                BOLD, RESET,
                CYAN, mode, RESET,
                GREEN + BOLD, result.getCount(), RESET);
        System.out.printf("  %sReplacement:%s %s%s%s%n", DIM, RESET, GREEN + BOLD, replacement, RESET);
        printDivider();

        List<Occurrence> occurrences = result.getResults();
        List<String> fileLines = searchService.getFileLines();

        if (occurrences == null || occurrences.isEmpty()) {
            printInfo("No occurrences found to replace.");
            for (int i = 0; i < fileLines.size(); i++) {
                String linePrefix = showLineNumbers ? formatLineNumber(i + 1) : "";
                System.out.println(linePrefix + fileLines.get(i));
            }
        } else {
            displayReplacedFile(fileLines, occurrences, replacement);
        }

        printSeparator();
        printMatchSummary(result.getCount());

        if (occurrences != null && !occurrences.isEmpty()) {
            promptSaveReplaced(occurrences, replacement);
        }
    }

    private void displayRegexReplaceResult(List<String> replacedLines, int matchCount,
            String pattern, String replacement, boolean caseInsensitive) {
        printSeparator();

        System.out.printf("%s[REGEX REPLACE]%s Pattern: %s%s%s | Replacing: %s%d%s matches%n",
                BOLD + MAGENTA, RESET,
                YELLOW, pattern, RESET,
                GREEN + BOLD, matchCount, RESET);
        System.out.printf("  %sReplacement:%s %s%s%s%n", DIM, RESET, GREEN + BOLD, replacement, RESET);
        printDivider();

        for (int i = 0; i < replacedLines.size(); i++) {
            String linePrefix = showLineNumbers ? formatLineNumber(i + 1) : "";
            System.out.println(linePrefix + replacedLines.get(i));
        }

        printSeparator();
        printMatchSummary(matchCount);

        if (matchCount > 0) {
            promptSaveRegexReplace(replacedLines);
        }
    }

    private void displayReplacedFile(List<String> fileLines, List<Occurrence> occurrences, String replacement) {
        int occIndex = 0;

        for (int lineNum = 1; lineNum <= fileLines.size(); lineNum++) {
            String line = fileLines.get(lineNum - 1);
            String linePrefix = showLineNumbers ? formatLineNumber(lineNum) : "";

            if (occIndex >= occurrences.size() ||
                    occurrences.get(occIndex).getLineNumber() != lineNum) {
                System.out.println(linePrefix + line);
                continue;
            }

            StringBuilder result = new StringBuilder();
            int charIndex = 0;
            int currentOccIndex = occIndex;

            while (charIndex < line.length()) {
                if (currentOccIndex < occurrences.size() &&
                        occurrences.get(currentOccIndex).getLineNumber() == lineNum &&
                        occurrences.get(currentOccIndex).getStartIndex() == charIndex) {

                    Occurrence occ = occurrences.get(currentOccIndex);

                    result.append(GREEN + BOLD);
                    result.append(replacement);
                    result.append(RESET);

                    charIndex += occ.getFullWord().length();
                    currentOccIndex++;
                } else {
                    result.append(line.charAt(charIndex));
                    charIndex++;
                }
            }

            System.out.println(linePrefix + result.toString());

            while (occIndex < occurrences.size() &&
                    occurrences.get(occIndex).getLineNumber() == lineNum) {
                occIndex++;
            }
        }
    }

    private void promptSaveResults(String promptMessage, SaveAction saveAction) {
        System.out.print("\n" + colorize("[?] ", YELLOW) + promptMessage + " (y/N): ");
        String response = safeReadLine();

        if (response != null && response.trim().equalsIgnoreCase("y")) {
            System.out.print(colorize("[>] ", CYAN) + "Enter output file path: ");
            String filePath = safeReadLine();

            if (filePath != null && !filePath.trim().isEmpty()) {
                saveAction.save(filePath.trim());
            } else {
                printError("Invalid file path.");
            }
        }
    }

    private void promptSaveHighlighted(List<Occurrence> occurrences) {
        promptSaveResults("Save highlighted results to file?",
                filePath -> saveHighlightedFile(occurrences, filePath));
    }

    private void promptSaveReplaced(List<Occurrence> occurrences, String replacement) {
        promptSaveResults("Save replaced results to file?",
                filePath -> saveReplacedFile(occurrences, replacement, filePath));
    }

    private void promptSaveRegex(String pattern, boolean caseInsensitive) {
        promptSaveResults("Save regex results to file?",
                filePath -> saveRegexHighlightedFile(pattern, caseInsensitive, filePath));
    }

    private void promptSaveRegexReplace(List<String> replacedLines) {
        promptSaveResults("Save replaced results to file?",
                filePath -> saveLinesToFile(replacedLines, filePath));
    }

    private void saveHighlightedFile(List<Occurrence> occurrences, String filePath) {
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
                    StringBuilder result = new StringBuilder();
                    int charIndex = 0;

                    while (charIndex < line.length()) {
                        if (occIndex < occurrences.size() &&
                                occurrences.get(occIndex).getLineNumber() == lineNum &&
                                occurrences.get(occIndex).getStartIndex() == charIndex) {

                            Occurrence occ = occurrences.get(occIndex);
                            result.append("<<");
                            result.append(line, charIndex, charIndex + occ.getFullWord().length());
                            result.append(">>");
                            charIndex += occ.getFullWord().length();
                            occIndex++;
                        } else {
                            result.append(line.charAt(charIndex));
                            charIndex++;
                        }
                    }
                    outputLine = result.toString();

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

    private void saveReplacedFile(List<Occurrence> occurrences, String replacement, String filePath) {
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
                    StringBuilder result = new StringBuilder();
                    int charIndex = 0;

                    while (charIndex < line.length()) {
                        if (occIndex < occurrences.size() &&
                                occurrences.get(occIndex).getLineNumber() == lineNum &&
                                occurrences.get(occIndex).getStartIndex() == charIndex) {

                            Occurrence occ = occurrences.get(occIndex);
                            result.append(replacement);
                            charIndex += occ.getFullWord().length();
                            occIndex++;
                        } else {
                            result.append(line.charAt(charIndex));
                            charIndex++;
                        }
                    }
                    outputLine = result.toString();

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

    private void saveRegexHighlightedFile(String patternStr, boolean caseInsensitive, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
            Pattern pattern = Pattern.compile(patternStr, flags);
            List<String> fileLines = searchService.getFileLines();

            for (String line : fileLines) {
                Matcher matcher = pattern.matcher(line);
                String highlighted = matcher.replaceAll("<<$0>>");
                writer.write(highlighted);
                writer.newLine();
            }

            printSuccess("Results saved to: " + filePath);
        } catch (Exception e) {
            printError("Failed to save file: " + e.getMessage());
        }
    }

    private void saveLinesToFile(List<String> lines, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            printSuccess("Results saved to: " + filePath);
        } catch (IOException e) {
            printError("Failed to save file: " + e.getMessage());
        }
    }

    private String colorize(String text, String color) {
        return color + text + RESET;
    }

    private String formatLineNumber(int lineNum) {
        return String.format("%s%4d │%s ", CYAN + DIM, lineNum, RESET);
    }

    private void printSuccess(String message) {
        System.out.println(colorize("[Done] ", GREEN + BOLD) + message);
    }

    private void printError(String message) {
        System.err.println(colorize("[Error] ", RED + BOLD) + message);
    }

    private void printInfo(String message) {
        System.out.println(colorize("[Info] ", BLUE) + message);
    }

    private void printMatchSummary(int count) {
        if (count > 0) {
            System.out.printf("  %s%d match%s found%s%n",
                    GREEN + BOLD, count, count == 1 ? "" : "es", RESET);
        } else {
            System.out.println(colorize("  No matches found", DIM));
        }
    }

    private void printSeparator() {
        System.out.println(colorize("═".repeat(SEPARATOR_WIDTH), DIM));
    }

    private void printDivider() {
        System.out.println(colorize("─".repeat(SEPARATOR_WIDTH), DIM));
    }

    private void printHelp() {
        printSeparator();
        System.out.println(colorize("  ENHANCED CLI - HELP", BOLD + CYAN));
        printSeparator();
        System.out.println();

        System.out.println(colorize("  FILE OPERATIONS", BOLD + YELLOW));
        System.out.printf("    %sload%s <path>           Load a text file%n", GREEN, RESET);
        System.out.printf("    %sstats%s                 Show file statistics%n", GREEN, RESET);
        System.out.println();

        System.out.println(colorize("  SEARCH OPERATIONS", BOLD + YELLOW));
        System.out.printf("    %ssearch%s <mode> <pattern>               Search and highlight%n", GREEN, RESET);
        System.out.printf("    %slist%s   <mode> <pattern>               List all matches%n", GREEN, RESET);
        System.out.printf("    %sreplace%s <mode> <pattern> <replacement>  Find and replace%n", GREEN, RESET);
        System.out.println();

        System.out.println(colorize("  SEARCH MODES", BOLD + YELLOW));
        System.out.printf("    %sprefix%s | %sp%s       Words starting with pattern%n", CYAN, RESET, CYAN, RESET);
        System.out.printf("    %ssubstring%s | %ss%s    Any text containing pattern%n", CYAN, RESET, CYAN, RESET);
        System.out.printf("    %swhole%s | %sw%s        Exact whole words only%n", CYAN, RESET, CYAN, RESET);
        System.out.printf("    %sregex%s | %sr%s        Regular expression pattern%n", MAGENTA, RESET, MAGENTA, RESET);
        System.out.println();

        System.out.println(colorize("  DISPLAY OPTIONS", BOLD + YELLOW));
        System.out.printf("    %scontext%s <n>          Set context lines around matches%n", GREEN, RESET);
        System.out.printf("    %slines on%s/%soff%s        Toggle line numbers%n", GREEN, RESET, GREEN, RESET);
        System.out.printf("    %sclear%s                Clear screen%n", GREEN, RESET);
        System.out.println();

        System.out.println(colorize("  OTHER", BOLD + YELLOW));
        System.out.printf("    %shelp%s | %s?%s            Show this help%n", GREEN, RESET, GREEN, RESET);
        System.out.printf("    %sexit%s | %sq%s            Exit program%n", GREEN, RESET, GREEN, RESET);
        System.out.println();

        printSeparator();
    }

    private void printWelcome() {
        System.out.println();
        printSeparator();
        System.out.println(colorize("       ╔═══════════════════════════════════════════╗", CYAN));
        System.out.println(colorize("       ║", CYAN)
                + colorize("    TEXT SEARCH ENGINE - ENHANCED MODE     ", BOLD + WHITE) + colorize("║", CYAN));
        System.out.println(colorize("       ║", CYAN) + colorize("         v2.0 with Regex & Colors          ", DIM)
                + colorize("║", CYAN));
        System.out.println(colorize("       ╚═══════════════════════════════════════════╝", CYAN));
        printSeparator();
        System.out.println("  Type " + colorize("help", GREEN) + " for a list of commands.");
        System.out.println();
    }

    private void printGoodbye() {
        System.out.println();
        printSeparator();
        System.out.println(colorize("  Thank you for using Enhanced Text Search Engine!", BOLD));
        printSeparator();
        System.out.println();
    }

    private record RegexMatch(int lineNum, int startIndex, int length, String matchedText) {
    }
}
