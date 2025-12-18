import java.io.*;
import java.util.*;

public class TextProcessor {

    private List<String> lines = new ArrayList<>();

    // read file 
    public void loadFile(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
    }

    public List<String> getLines() {
        return lines;
    }

    // normalization
    public String normalize(String text) {
        return text.toLowerCase();
    }

    // Word Boundary check
    public boolean isWordBoundary(String line, int start, int length) {

        boolean left = (start == 0) ||
                !Character.isLetterOrDigit(line.charAt(start - 1));

        int end = start + length;
        boolean right = (end == line.length()) ||
                !Character.isLetterOrDigit(line.charAt(end));

        return left && right;
    }

    // search using boyer moore horspool algorithm
    public void searchWord(String word) {

        word = normalize(word);

        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {

            String line = normalize(lines.get(lineNum));
            int index = 0;

            while ((index = BoyerMooreHorspool.search(line, word, index)) != -1) {

                if (isWordBoundary(line, index, word.length())) {
                    System.out.println(
                        "Found at line " + (lineNum + 1) +
                        ", index " + index
                    );
                }

                index += word.length();
            }
        }
    }
}
