import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.List;

public class TextSearchGUI extends JFrame {
    private TextProcessor tp = new TextProcessor();
    private JTextField searchField;
    private JComboBox<String> searchMode; 
    private JButton loadButton, searchButton;
    private JTextArea resultArea;
    private JLabel fileLabel;

    public TextSearchGUI() {
        super("Advanced Text Search Tool");
        setLayout(new BorderLayout());

        // --- Top Panel: File Operations ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadButton = new JButton("Load File");
        fileLabel = new JLabel("No file loaded");
        topPanel.add(loadButton);
        topPanel.add(fileLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Results Display ---
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Panel: Search Configuration ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        
        // Options: Prefix, Substring, Whole Word
        String[] modes = { "Prefix", "Substring", "Whole Word" };
        searchMode = new JComboBox<>(modes);
        
        searchButton = new JButton("Search");
        bottomPanel.add(new JLabel("Keyword: "));
        bottomPanel.add(searchField);
        bottomPanel.add(new JLabel("Mode: "));
        bottomPanel.add(searchMode);
        bottomPanel.add(searchButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Actions ---
        loadButton.addActionListener(e -> loadFile());
        searchButton.addActionListener(e -> performSearch());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                // Read file once and index it
                tp.loadFile(file.getAbsolutePath());
                fileLabel.setText(file.getName() + " (Ready)");
                resultArea.setText("File loaded and indexed successfully!\n");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a keyword.");
            return;
        }

        resultArea.setText("--- Search Results ---\n");
        String mode = (String) searchMode.getSelectedItem();
        
        // Measure execution time (excluding I/O)
        long startTime = System.nanoTime();
        
        if ("Prefix".equals(mode)) {
            displayPrefixResults(query);
        } else {
            // Fixed: Handles overlapping matches
            displayBoyerMooreResults(query, "Whole Word".equals(mode));
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000; // Microseconds
        
        resultArea.append("\n----------------------\n");
        resultArea.append("Execution Time: " + duration + " microseconds\n");
    }

    private void displayPrefixResults(String query) {
        List<Occurrence> results = tp.getPrefixEngine().searchPrefix(query);
        
        results.sort((a, b) -> {
            if (a.lineNumber != b.lineNumber) {
                return Integer.compare(a.lineNumber, b.lineNumber);
            }
            return Integer.compare(a.startIndex, b.startIndex);
        });

        resultArea.append("Total Occurrences Found: " + results.size() + "\n\n");
        
        for (Occurrence occ : results) {
            resultArea.append("[Line " + occ.lineNumber + ", Index " + occ.startIndex + "] ");
            resultArea.append("Word: " + occ.fullWord + "\n");
        }
    }

    private void displayBoyerMooreResults(String query, boolean isWholeWord) {
        List<String> lines = tp.getLines();
        int count = 0;
        String searchPattern = tp.normalize(query);

        for (int i = 0; i < lines.size(); i++) {
            String line = tp.normalize(lines.get(i));
            int index = 0;
            
    
            while ((index = BoyerMooreHorspool.search(line, searchPattern, index)) != -1) {
                boolean match = true;
                if (isWholeWord) {
                    match = isWholeWordMatch(line, index, searchPattern.length());
                }

                if (match) {
                    count++;
                    resultArea.append("[Line " + (i + 1) + ", Index " + index + "] " + lines.get(i).trim() + "\n");
                }

                // FIX: Increment by 1 to allow overlapping matches
                index += 1;
            }
        }
        resultArea.insert("Total Occurrences Found: " + count + "\n\n", 23);
    }

    private boolean isWholeWordMatch(String line, int start, int length) {
        boolean before = (start == 0) || !Character.isLetterOrDigit(line.charAt(start - 1));
        boolean after = (start + length == line.length()) || !Character.isLetterOrDigit(line.charAt(start + length));
        return before && after;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextSearchGUI::new);
    }
}
