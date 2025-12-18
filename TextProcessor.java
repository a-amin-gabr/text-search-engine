import java.io.*;
import java.util.*;

public class TextProcessor {

    private List<String> lines = new ArrayList<>();

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

    public String normalize(String text) {
        return text.toLowerCase();
    }
}
