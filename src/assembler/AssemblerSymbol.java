package assembler;

class AssemblerSymbol {
    /*
    Implements a symbol for assembler symbol data
    These will be the objects stored in our symbol table
     */

    private String name;    // the name of the symbol
    private short data; // the address of the symbol (the data)
    private byte length; // how long (in bytes) the symbol is (helps to optimize and check for errors)

    short getData() { return this.data; }

    byte getLength() { return this.length; }

    AssemblerSymbol(String name, short data, byte length) {
        this.name = name;
        this.data = data;
        this.length = length;
    }

    AssemblerSymbol(String name, short data) {
        this(name, data, (byte)4);
    }
}
