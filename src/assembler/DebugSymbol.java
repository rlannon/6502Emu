package assembler;

public class DebugSymbol {
    private int line;
    private short address;

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
    }
}
