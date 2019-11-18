package assembler;

public class DebugSymbol {
    private String label;
    private int line;
    private short address;

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
        this.line = line;
        this.address = address;
        this.label = "";
    }

    public DebugSymbol(String label, short address) {
        this.label = label;
        this.address = address;
    }
}
