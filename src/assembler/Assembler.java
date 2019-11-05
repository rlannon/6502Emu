package assembler;

import emu_format.*;

import java.io.*;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.Hashtable;

public class Assembler {
    /*

    Implements a single-pass assembler for 6502 asm
    This may be upgraded to a multi-pass later, but for now we will keep it simple

     */

    // our assembler directives
    private final static String[] asmDirectives = {
            ".org", ".db", ".dw", ".segment"
    };

    // some patterns
    private final static Pattern TO_IGNORE = Pattern.compile("[\\s]|(;.*)");

    /*

    Data Members

     */

    private FileReader asmIn;   // the input asm file

    // place tracking
    private short currentOrigin;    // the current origin
    private short currentOffset;    // the current byte offset from the origin

    // bank and data buffer
    private Vector<Bank> banks;
    private byte[] buffer;  // a buffer to hold our bytecode until we hit a new segment or the end of the file

    // Symbols
    private String parentSymbolName;    // the name of the current parent symbol (allows for .sym)
    private Vector<DebugSymbol> debugSymbols;   // debug symbols for our debugger
    private Hashtable<String, AssemblerSymbol> symbolTable; // the table containing our assembler symbols
    private Vector<RelocationSymbol> relocationTable;   // the table for holding all unresolved symbol references

    /*

    Methods

     */

    private String getFullSymbolName(String symName) throws Exception {
        /*

        Since labels may have sublabels, we must get the full symbol name every time
        For example:
            mySubroutine:
                ; some code
            .loop:
                ; some code
        The ".loop" label will be expanded to "mySubroutine.loop"
        Every time a new top-level label is hit, this.parentSymbolName gets updated

         */
        if (symName.length() > 0) {
            // Gets the full name, if the symbol is a child symbol
            if (symName.charAt(0) == '.') {
                // ensure the label has a parent and that
                if (this.parentSymbolName == null || this.parentSymbolName.equals("")) {
                    throw new Exception("Cannot create child symbol, as no parent symbol could be found");
                } else {
                    return this.parentSymbolName + symName;
                }
            } else {
                this.parentSymbolName = symName;    // update the parent symbol
                return symName;
            }
        }
        else
        {
            throw new Exception("String is empty");
        }
    }

    private boolean isDirective(String toCheck) {
        boolean found = false;
        int idx = 0;
        while (idx < this.asmDirectives.length && !found)
        {
            if (toCheck.equals(this.asmDirectives[idx]))
            {
                found = true;
            }
            else
            {
                idx++;
            }
        }

        return found;
    }

    private String[] splitString(String toSplit) {
        // split the string according to TO_IGNORE, removing empty strings and nullptrs
        return java.util.Arrays.stream(this.TO_IGNORE.split(toSplit))
                .filter(value -> value != null && value.length() > 0)
                .toArray(String[]::new);
    }

    private void resolveSymbols() throws Exception {
        // performs symbol resolution for all symbols in the relocation table

        for (RelocationSymbol rSym: this.relocationTable)
        {
            Bank relocBank = null;
            for (int i = 0; i < this.banks.size(); i++)
            {
                if (this.banks.elementAt(i).getOrg() == rSym.getBank())
                {
                    relocBank = this.banks.elementAt(i);
                    break;
                }
            }
            if (relocBank != null) {
                // get the symbol from our symbol table
                AssemblerSymbol asmSym = this.symbolTable.get(rSym.getName());
                if (asmSym != null)
                {
                    byte[] littleEndianAddressData = null;

                    // different addressing modes will require different things from the symbol
                    // a bne instruction, for example, requires the offset to the label
                    if (rSym.getAddressingMode() == AddressingMode.Relative) {
                        // difference is label - reference to get the offset to the label
                        short labelAddr = asmSym.getData();
                        short refAddr = (short)(rSym.getBank() + rSym.getOffset());
                        byte offset = (byte)(labelAddr - refAddr);
                        littleEndianAddressData = new byte[]{ offset };
                    } else if (rSym.getAddressingMode() == AddressingMode.Absolute) {
                        // absolute requires the whole address
                        littleEndianAddressData = new byte[]{(byte) (asmSym.getData() & 0xFF), (byte) ((asmSym.getData() >> 8) & 0xFF)};
                    } else {
                        throw new Exception("Invalid addressing mode for label reference");
                    }

                    // replace at offset + 1 to leave room for the opcode
                    relocBank.setData(rSym.getOffset() + 1, littleEndianAddressData);
                } else {
                    throw new NullPointerException("Could not find referenced symbol");
                }
            } else {
                throw new NullPointerException("Could not find bank information in symbol resolution");
            }
        }
    }

    public boolean assemble(String filename) throws IOException {
        // an overloaded version of parseFile that allows us to call the function with a filename
        // this is for the case where we didn't pass a filename to the constructor
        try {
            this.asmIn = new FileReader(filename);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return this.assemble();
    }

    boolean assemble() throws IOException {
        // parses the ASM file this.asmIn

        int lineNumber = 1;
        if (this.asmIn != null)
        {
            System.out.println("Assembling file...");
            Scanner asmScan = new Scanner(this.asmIn);

            while (asmScan.hasNextLine())
            {
                // add the current line and its address to our debugSymbols vector
                this.debugSymbols.add(new DebugSymbol(lineNumber, (short)(this.currentOrigin + this.currentOffset)));

                // get the line
                String line = asmScan.nextLine();

                // split the line into its components, ignoring comments and whitespace
                String[] lineData = splitString(line);

                // skip all empty lines and comments
                if (line.length() > 0 && lineData.length > 0)
                {
                    // check to see if we have an instruction
                    if (InstructionParser.isMnemonic(lineData[0]))
                    {
                        // check to see if lineData[1] is a symbol name; if so, add it to the relocation table
                        if (lineData.length > 1)
                        {
                            if (lineData[1].matches("(?!\\$)(#?\\.?[a-zA-Z_]+[0-9a-zA-Z_]*)"))
                            {
                                // if we have the address-of-symbol operator (#), skip it
                                if (lineData[1].charAt(0) == '#')
                                {
                                    lineData[1] = lineData[1].substring(1);
                                }

                                // if we have a ., get the parent label
                                if (lineData[1].charAt(0) == '.')
                                {
                                    try {
                                        lineData[1] = getFullSymbolName(lineData[1]);
                                    } catch (Exception e) {
                                        System.out.println(e.toString());
                                    }
                                }

                                try {
                                    this.relocationTable.add(new RelocationSymbol(lineData[1], this.currentOrigin, this.currentOffset, InstructionParser.getAddressingMode(lineData)));
                                } catch (Exception e) {
                                    System.out.println(e.toString());
                                }
                            }
                        }

                        // get our instruction data, update the offset, and add the bytes to our bytecode buffer
                        byte[] instructionData = InstructionParser.parseInstruction(lineData);
                        byte[] bytecode = new byte[this.buffer.length + instructionData.length];
                        System.arraycopy(this.buffer, 0, bytecode, 0, this.buffer.length);
                        System.arraycopy(instructionData, 0, bytecode, this.buffer.length, instructionData.length);
                        this.buffer = bytecode;

                        // update the offset
                        this.currentOffset += instructionData.length;   // increase our offset
                    }
                    // finally, check to see if we have an assembler directive
                    else if (isDirective(lineData[0]))
                    {
                        // .org directive
                        if (lineData[0].toLowerCase().equals(".org"))
                        {
                            // check to see if we have data in our current buffer
                            if (this.buffer.length > 0)
                            {
                                // create a new bank and add it to our banks
                                this.banks.add(new Bank(this.currentOrigin, this.buffer));

                                // clear the buffer
                                this.buffer = new byte[]{};
                            }

                            // get the new origin and update currentOrigin and currentOffset
                            try
                            {
                                this.currentOrigin = InstructionParser.parseNumber(lineData[1]);
                                this.currentOffset = 0;

                                // we should also update the parent symbol because we are in a new segment
                                this.parentSymbolName = null;
                            } catch (Exception e) {
                                System.out.println("Parse error: " + e.toString());
                            }
                        }
                    }
                    // otherwise, it is a symbol name
                    else
                    {
                        try {
                            // the symbol must be followed immediately by a colon or an equals sign
                            if (lineData.length == 1) {
                                if (lineData[0].charAt(lineData[0].length() - 1) == ':') {
                                    lineData[0] = lineData[0].substring(0, lineData[0].length() - 1);
                                } else {
                                    throw new Exception("Invalid syntax");
                                }
                            } else {
                                if (!lineData[1].equals("=")) {
                                    throw new Exception("Invalid syntax");
                                } else {
                                    if (lineData.length > 3)
                                    {
                                        throw new Exception("Invalid syntax");
                                    }
                                }
                            }

                            // create a symbol with the current offset
                            String fullSymName = this.getFullSymbolName(lineData[0]);

                            // check to see if the symbol is already in our table
                            if (this.symbolTable.contains(fullSymName))
                            {
                                throw new Exception("Symbol already in table");
                            } else {
                                // add it to the symbol table
                                AssemblerSymbol sym = new AssemblerSymbol(fullSymName, (short)(this.currentOrigin + this.currentOffset));
                                this.symbolTable.put(fullSymName, sym);
                            }
                        }
                        catch (Exception e)
                        {
                            System.out.println("Error on line " + lineNumber + ": " + e.toString());
                            return false;
                        }
                    }
                }
                // skip empty lines, commented lines

                // increment our line number
                lineNumber++;
            }

            // once we are done, create a new bank if we had data
            if (this.buffer.length > 0)
            {
                this.banks.add(new Bank(this.currentOrigin, this.buffer));
            }

            asmScan.close();

            // next, resolve all symbols
            try {
                this.resolveSymbols();
            } catch (Exception e) {
                System.out.println("Could not resolve symbols; " + e.toString());
            }

            // now, create an EmuFile
            EmuFile emu = new EmuFile(this.banks, this.debugSymbols);
            emu.writeEmuFile("assembled.emu");

            System.out.println("Done.");
        }
        else
        {
            throw new IOException("No file to parse");
        }

        return true;
    }

    public Assembler() {
        // default constructor
        this.asmIn = null;
        this.currentOffset = 0;
        this.debugSymbols = new Vector<>();
        this.symbolTable = new Hashtable<>();
        this.relocationTable = new Vector<>();
        this.buffer = new byte[]{};
        this.currentOrigin = (short)0x8000;
        this.banks = new Vector<>();
    }

    public Assembler(String filename) {
        this();

        try {
            asmIn = new FileReader(filename);
        }
        catch (java.io.FileNotFoundException e)
        {
            System.out.println(e.toString());
        }
    }
}
