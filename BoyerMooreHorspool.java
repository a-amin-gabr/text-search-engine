import java.util.HashMap;
import java.util.Map;

public class BoyerMooreHorspool {

    //? preprocess for buldShiftTable
    private static Map<Character, Integer> buildShiftTable(String pattern) {
        Map<Character, Integer> table = new HashMap<>();
        int m = pattern.length();
        for (int i = 0; i < m - 1; i++) { //! m - 1 to exclude the last char
            table.put(pattern.charAt(i), m - 1 - i);
        }
        return table;
    }

    public static int search(String text, String pattern, int startIndex) {
        int n = text.length();
        int m = pattern.length();
        Map<Character, Integer> table = buildShiftTable(pattern);
        int i = startIndex;

        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(i + j)) {
                j--; //? matches from right to left
            }
            if (j < 0) return i; //? found

            char c = text.charAt(i + m - 1);
            int shift = table.getOrDefault(c, m);

            i += shift; 
        }
        return -1; // not found
    }
}
