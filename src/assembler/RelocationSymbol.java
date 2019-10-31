package assembler;

public class RelocationSymbol {
    // Used for resolving symbol references in files
    String name;    // the symbol's name
    short bank; // the bank where the symbol was found
    short offset;   // the offset within that bank
    int addressingMode; // the instruction addressing mode

    public String getName() { return this.name; }
    public short getBank() { return this.bank; }
    public short getOffset() { return this.offset; }
    public int getAddressingMode() { return this.addressingMode; }

    RelocationSymbol(String name, short bank, short offset, int addressingMode)
    {
        this.name = name;
        this.bank = bank;
        this.offset = offset;
        this.addressingMode = addressingMode;
    }
}
