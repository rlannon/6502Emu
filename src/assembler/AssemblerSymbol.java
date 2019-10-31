package assembler;

public class AssemblerSymbol {
    /*
    Implements a symbol for assembler symbol data
    These will be the objects stored in our symbol table
     */

    private String name;    // the name of the symbol
    private short data;  // the address of the symbol (the data)

    public short getData() { return this.data; }

    AssemblerSymbol()
    {
        // default constructor
    }

    AssemblerSymbol(String name, short data)
    {
        this.name = name;
        this.data = data;
    }
}
