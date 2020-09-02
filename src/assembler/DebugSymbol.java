package assembler;

public class DebugSymbol {
    private String label;
    final private int line;
    final private short address;

    void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public int getLine()
    {
        return this.line;
    }

    public short getAddress()
    {
        return this.address;
    }

    public DebugSymbol(int line, short address)
    {
        this(line, address, "");
    }

    public DebugSymbol(int line, short address, String label) {
        this.line = line;
        this.address = address;
        this.label = label;
    }
}
