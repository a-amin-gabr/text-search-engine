package src.models;

public class Occurrence {
    public int lineNumber;
    public int startIndex;
    public String fullWord;

    public Occurrence(int lineNumber, int startIndex, String fullWord) {
        this.lineNumber = lineNumber;
        this.startIndex = startIndex;
        this.fullWord = fullWord;
    }

    @Override
    public String toString() {
        return "[Line " + lineNumber + ", Index " + startIndex + "] " + fullWord;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public String getFullWord() {
        return fullWord;
    }
}
