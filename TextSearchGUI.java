import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

public class TextSearchGUI extends JFrame {

    private TextProcessor tp = new TextProcessor();
    private JTextField searchField;
    private JButton loadButton, searchButton;
    private JTextArea resultArea;
    private JLabel fileLabel;

    public TextSearchGUI() {
        super("Text Search Tool");
        setLayout(new BorderLayout());

        // Top panel: load file and show path
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadButton = new JButton("Load File");
        fileLabel = new JLabel("No file loaded");
        topPanel.add(loadButton);
        topPanel.add(fileLabel);
        add(topPanel, BorderLayout.NORTH);

        // Center panel: results area
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel: search field and button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(30);
        searchButton = new JButton("Search");
        bottomPanel.add(new JLabel("Search: "));
        bottomPanel.add(searchField);
        bottomPanel.add(searchButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button actions
        loadButton.addActionListener(e -> loadFile());
        searchButton.addActionListener(e -> searchText());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                tp.loadFile(file.getAbsolutePath());
                fileLabel.setText(file.getName() + " loaded");
                resultArea.setText("File loaded successfully!\n");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load file: " + ex.getMessage());
            }
        }
    }

    private void searchText() {
        String pattern = searchField.getText().trim();
        if (pattern.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.");
            return;
        }

        resultArea.setText(""); // clear previous results
        List<String> lines = tp.getLines();
        if (lines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No file loaded.");
            return;
        }

        pattern = tp.normalize(pattern);

        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
            String line = tp.normalize(lines.get(lineNum));
            int index = 0;
            while ((index = BoyerMooreHorspool.search(line, pattern, index)) != -1) {
                resultArea.append("Found at line " + (lineNum + 1) + ", index " + index + ": " + lines.get(lineNum) + "\n");
                index += pattern.length();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextSearchGUI::new);
    }
}
