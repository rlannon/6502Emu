package assembler;

class RelocationSymbol {
    // Used for resolving symbol references in files
    private String name;    // the symbol's name
    private short bank; // the bank where the symbol was found
    private short offset;   // the offset within that bank
    private int addressingMode; // the instruction addressing mode

    String getName() { return this.name; }
    short getBank() { return this.bank; }
    short getOffset() { return this.offset; }
    int getAddressingMode() { return this.addressingMode; }

    RelocationSymbol(String name, short bank, short offset, int addressingMode)
    {
        this.name = name;
        this.bank = bank;
        this.offset = offset;
        this.addressingMode = addressingMode;
    }
}
