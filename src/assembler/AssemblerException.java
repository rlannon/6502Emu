package assembler;

public class AssemblerException extends Exception {
    final private int lineNumber;
    final private String message;

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
