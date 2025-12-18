import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        // line by line and normalizaion 
        TextProcessor tp = new TextProcessor();
        tp.loadFile("input.txt");

        // display lines 
        List<String> lines = tp.getLines();
        for (String line : lines) {
            System.out.println(line);
        }

        // search using BoyerMooreHorspool
        Scanner sc = new Scanner(System.in);
        System.out.print("search: ");   
        String search = sc.nextLine();
        tp.searchWord(search);
    }
}
