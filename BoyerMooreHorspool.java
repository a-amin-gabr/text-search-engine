import java.util.*;

public class BoyerMooreHorspool {

    private static Map<Character, Integer> buildShiftTable(String pattern) {
        Map<Character, Integer> table = new HashMap<>();
        int m = pattern.length();
        for (int i = 0; i < m - 1; i++) {
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
                j--;
            }
            if (j < 0) return i; // found
            char c = text.charAt(i + m - 1);
            i += table.getOrDefault(c, m);
        }
        return -1;
    }
}
