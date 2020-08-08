package assembler;

class RelocationSymbol {
    // Used for resolving symbol references in files
    private String name;    // the symbol's name
    private short bank; // the bank where the symbol was found
    private short offset;   // the offset within that bank
    private AddressingMode addressingMode; // the instruction addressing mode
    private boolean isDefinition;   // if we are using a .db directive, note that
    private int lineNumber; // the line number of the symbol

    String getName() { return this.name; }
    short getBank() { return this.bank; }
    short getOffset() { return this.offset; }
    AddressingMode getAddressingMode() { return this.addressingMode; }
    boolean isDefinition() { return this.isDefinition; }
    int getLineNumber() { return this.lineNumber; }

    RelocationSymbol(String name, short bank, short offset, AddressingMode mode, int lineNumber, boolean isDefinition)
    {
        this.name = name;
        this.bank = bank;
        this.offset = offset;
        this.addressingMode = mode;
        this.lineNumber = lineNumber;
        this.isDefinition = isDefinition;
    }

    RelocationSymbol(String name, short bank, short offset, AddressingMode mode, int lineNumber) {
        this(name, bank, offset, mode, lineNumber, false);
    }
}
