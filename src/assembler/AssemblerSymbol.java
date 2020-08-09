package assembler;

class AssemblerSymbol {
    /*
    Implements a symbol for assembler symbol data
    These will be the objects stored in our symbol table
     */

    final private String name;    // the name of the symbol
    final private short data; // the address of the symbol (the data)
    final private byte length; // how long (in bytes) the symbol is (helps to optimize and check for errors)

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
