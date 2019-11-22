package assembler;

public class AssemblerException extends Exception {
    private int lineNumber;
    private String message;

    public int getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public String getMessage() {
        return ("Line " + lineNumber + ": " + this.message);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    AssemblerException(String message, int lineNumber) {
        super(message);
        this.message = message;
        this.lineNumber = lineNumber;
    }
}
