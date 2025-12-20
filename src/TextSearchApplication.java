package src;

import src.factory.SearchModeFactory;
import src.interfaces.SearchInterface;

public class TextSearchApplication {
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "cli";
        SearchInterface searchMode = SearchModeFactory.createSearchMode(mode);
        searchMode.run();
    }
}
