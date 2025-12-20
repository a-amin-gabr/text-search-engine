package src.factory;

public class SearchModeFactory {
    public static src.interfaces.SearchInterface createSearchMode(String mode) {
        switch (mode.toLowerCase()) {
            case "gui":
                return new src.modes.GUISearchMode();
            case "cli":
                return new src.modes.CLIMode();
            case "enhancedcli":
                return new src.modes.EnhancedCLIMode();
            default:
                throw new IllegalArgumentException("Unknown search mode: " + mode);
        }
    }
}
