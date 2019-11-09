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
    private final static String[] ASM_DIRECTIVES = {
            ".org", ".db", ".byte", ".dw", ".word", ".rsset", ".rs"
    };

    // some patterns
    private final static Pattern TO_IGNORE = Pattern.compile("[\\s]|(;.*)");
    private final static String SYMBOL_NAME_REGEX = "(?!\\$)(#?\\.?[a-zA-Z_]+[0-9a-zA-Z_]*)";

    /*

    Data Members

     */

    private FileReader asmIn;   // the input asm file

    // place tracking
    private short currentOrigin;    // the current origin
    private short currentOffset;    // the current byte offset from the origin

    // data address tracking
    private short rsAddress; // the internal counter for the .rs directive

    // bank and data buffer
    private Vector<Bank> banks;
    private byte[] buffer;  // a buffer to hold our bytecode until we hit a new segment or the end of the file
    // todo: make 'buffer' into a vector since it is being updated so much?

    // Symbols
    private String parentSymbolName;    // the name of the current parent symbol (allows for .sym)
    private Vector<DebugSymbol> debugSymbols;   // debug symbols for our debugger
    private Hashtable<String, AssemblerSymbol> symbolTable; // the table containing our assembler symbols
    private Vector<RelocationSymbol> relocationTable;   // the table for holding all unresolved symbol references

    /*

    Methods

     */

    private void copyToBuffer(byte[] toCopy) {
        // adds the contents of array 'toCopy' to our buffer

        // first, create a new array of the appropriate size
        byte[] bankData = new byte[this.buffer.length + toCopy.length];

        // copy the contents
        // copy the data from the buffer into bankData
        System.arraycopy(this.buffer, 0, bankData, 0, this.buffer.length);
        // now, copy the definedBytes into bankData where we left off
        System.arraycopy(toCopy, 0, bankData, this.buffer.length, toCopy.length);

        // finally, set 'buffer' equal to 'bankData' (to update the buffer)
        this.buffer = bankData;
    }

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
        while (idx < ASM_DIRECTIVES.length && !found)
        {
            if (toCheck.equals(ASM_DIRECTIVES[idx]))
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
                    if (rSym.getAddressingMode() == AddressingMode.Relative) {
                        // The Relative addressing mode requires an offset (signed) to the address
                        // difference is label - reference to get the offset to the label - 2, to account for the instruction's width
                        short labelAddr = asmSym.getData();
                        short refAddr = (short)(rSym.getBank() + rSym.getOffset());
                        byte offset = (byte)(labelAddr - refAddr - 2);
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

    public void assemble(String filename) throws Exception {
        // an overloaded version of parseFile that allows us to call the function with a filename
        // this is for the case where we didn't pass a filename to the constructor
        try {
            this.asmIn = new FileReader(filename);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        this.assemble();
    }

    private void assemble() throws Exception {
        // parses the ASM file this.asmIn

        int lineNumber = 1;
        if (this.asmIn != null)
        {
            System.out.println("Assembling file...");
            Scanner asmScan = new Scanner(this.asmIn);

            while (asmScan.hasNextLine())
            {
                try {
                    // add the current line and its address to our debugSymbols vector
                    this.debugSymbols.add(new DebugSymbol(lineNumber, (short) (this.currentOrigin + this.currentOffset)));

                    // get the line
                    String line = asmScan.nextLine();

                    // split the line into its components, ignoring comments and whitespace
                    String[] lineData = splitString(line);

                    // skip all empty lines and comments
                    if (line.length() > 0 && lineData.length > 0) {
                        // check to see if we have an instruction
                        if (InstructionParser.isMnemonic(lineData[0])) {
                            // check to see if lineData[1] is a symbol name; if so, add it to the relocation table
                            if (lineData.length > 1) {
                                if (lineData[1].matches(SYMBOL_NAME_REGEX)) {
                                    // if we have the address-of-symbol operator (#), skip it
                                    if (lineData[1].charAt(0) == '#') {
                                        lineData[1] = lineData[1].substring(1);
                                    }

                                    // if we have a ., get the parent label
                                    if (lineData[1].charAt(0) == '.') {
                                        lineData[1] = getFullSymbolName(lineData[1]);
                                    }

                                    this.relocationTable.add(new RelocationSymbol(lineData[1], this.currentOrigin, this.currentOffset, InstructionParser.getAddressingMode(lineData)));
                                }
                            }

                            // get our instruction data, update the offset, and add the bytes to our bytecode buffer
                            byte[] instructionData = InstructionParser.parseInstruction(lineData);
                            this.copyToBuffer(instructionData);

                            // update the offset
                            this.currentOffset += instructionData.length;   // increase our offset
                        }
                        // check to see if we have an assembler directive
                        else if (isDirective(lineData[0])) {
                            // get the directive as a string to make the code easier to read
                            String directive = lineData[0].toLowerCase();

                            // .org directive
                            switch (directive) {
                                case ".org":
                                    this.handleOrg(lineData);
                                    break;
                                case ".db":
                                case ".byte":
                                    this.defineByte(lineData);
                                    break;
                                case ".dw":
                                case ".word":
                                    this.defineWords(lineData);
                                    break;
                                case ".rsset":
                                    this.handleRSSet(lineData);
                                    break;
                                case ".rs":
                                    this.reserveBytes(lineData);
                                    break;
                                default:
                                    throw new Exception("Invalid assembler directive");
                            }
                        }
                        // otherwise, it is a symbol name
                        else {
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
                                    if (lineData.length > 3) {
                                        throw new Exception("Invalid syntax");
                                    }
                                }
                            }

                            // create a symbol with the current offset
                            String fullSymName = this.getFullSymbolName(lineData[0]);

                            // check to see if the symbol is already in our table
                            if (this.symbolTable.contains(fullSymName)) {
                                throw new Exception("Symbol already in table");
                            } else {
                                // add it to the symbol table
                                AssemblerSymbol sym = new AssemblerSymbol(fullSymName, (short) (this.currentOrigin + this.currentOffset));
                                this.symbolTable.put(fullSymName, sym);
                            }
                        }
                    }
                    // skip empty lines, commented lines

                    // increment our line number
                    lineNumber++;
                } catch (Exception e) {
                    // if an exception occurred during assembly, catch it, add the line number, and throw a new one
                    throw new Exception("Error on line " + lineNumber + ": " + e.toString());
                }
            }

            // once we are done, create a new bank if we had data
            if (this.buffer.length > 0)
            {
                this.banks.add(new Bank(this.currentOrigin, this.buffer));
            }

            asmScan.close();

            // next, resolve all symbols
            this.resolveSymbols();

            // now, create an EmuFile
            EmuFile emu = new EmuFile(this.banks, this.debugSymbols);
            emu.writeEmuFile("assembled.emu");

            System.out.println("Done.");
        }
        else
        {
            throw new IOException("No file to parse");
        }
    }

    /*

    Assembler directives

    All of the following functions handle assembler directives and their functionality.

     */

    private void handleOrg(String[] lineData) throws Exception {
        // Handles a .org directive

        // check to see if we have data in our current buffer
        if (this.buffer.length > 0) {
            // create a new bank and add it to our banks
            this.banks.add(new Bank(this.currentOrigin, this.buffer));

            // clear the buffer
            this.buffer = new byte[]{};
        }

        // get the new origin and update currentOrigin and currentOffset
        this.currentOrigin = InstructionParser.parseNumber(lineData[1]);
        this.currentOffset = 0;

        // we should also update the parent symbol because we are in a new segment
        this.parentSymbolName = null;
    }

    private void handleRSSet(String[] lineData) throws Exception {
        // Handles the .rsset directive

        if (lineData[1].charAt(0) == '#') {
            throw new Exception("Invalid directive syntax");
        } else {
            this.rsAddress = InstructionParser.parseNumber(lineData[1]);
        }
    }

    private void reserveBytes(String[] lineData) throws Exception {
        /*

        Reserves a specified number of bytes of memory with a label
        The syntax is:
            .rs <num_byes> <label>

         */

        if (lineData.length > 2) {
            int numBytes = Integer.parseInt(lineData[1]);
            if (numBytes > 0) {
                String name = lineData[2];

                // ensure our name follows the appropriate guidelines (and doesn't start with a #)
                if (name.matches(SYMBOL_NAME_REGEX) && (name.charAt(0) != '#')) {
                    // add the symbol
                    name = this.getFullSymbolName(name);
                    this.symbolTable.put(name, new AssemblerSymbol(name, this.rsAddress));
                    this.rsAddress += numBytes;  // advance rsAddress by the number of bytes in the symbol
                } else {
                    throw new Exception("Invalid symbol name");
                }
            }  else {
                throw new Exception("Bytes to reserve must be a positive integer");
            }
        } else {
            throw new Exception("Invalid directive syntax");
        }
    }

    private void defineByte(String[] lineData) throws Exception {
        /*

        Defines a byte, or series of bytes

        Proper syntax is:
            .db <byte>, <byte>, ... <byte>
        Each byte should be given with a hexadecimal or binary prefix. The '#' prefix is not allowed

         */

        // the number of bytes will be equal to the length of the data - the directive
        int numBytes = lineData.length - 1;
        if (numBytes == 0) {
            throw new Exception("Must define bytes");
        } else {
            // create an array to store the defined bytes
            byte[] definedBytes = new byte[numBytes];

            // iterate through our string array and lay down bytes
            for (int i = 1; i < lineData.length; i++) {
                // parse the number there
                if (lineData[i].charAt(0) == '#') {
                    throw new Exception("Invalid syntax");
                } else {
                    // fetch our number
                    byte data = (byte)(InstructionParser.parseNumber(lineData[i]) & 0xFF);
                    definedBytes[i - 1] = data;

                    // increment the offset to account for the data
                    this.currentOffset += 1;
                }
            }

            // now, copy 'definedBytes' into our bank
            this.copyToBuffer(definedBytes);
        }
    }

    private void defineWords(String[] lineData) throws Exception {
        // Defines a word, or a series of words

        int numWords = lineData.length - 1;
        if (numWords == 0) {
            throw new Exception("Must define words");
        } else {
            // create an array to store the defined words *in little-endian format*
            byte[] definedWords = new byte[2 * numWords];

            // iterate through our string and lay down our bytes
            for (int i = 1, arrIndex = 0; i < lineData.length; i++, arrIndex += 2) {
                // parse the number there
                if (lineData[i].charAt(0) == '#') {
                    throw new Exception("Invalid syntax");
                } else {
                    // fetch the number
                    short num = InstructionParser.parseNumber(lineData[i]);
                    byte numLow = (byte)(num & 0xFF);
                    byte numHigh = (byte)((num >> 8) & 0xFF);

                    // add the bytes (low, high) to the array
                    definedWords[arrIndex] = numLow;
                    definedWords[arrIndex + 1] = numHigh;

                    // update our offset
                    this.currentOffset += 2;
                }
            }

            // finally, copy our words to the buffer
            this.copyToBuffer(definedWords);
        }
    }

    /*

    Constructors

     */

    public Assembler() {
        // default constructor
        this.asmIn = null;
        this.currentOffset = 0;
        this.debugSymbols = new Vector<>();
        this.symbolTable = new Hashtable<>();
        this.relocationTable = new Vector<>();
        this.buffer = new byte[]{};
        this.currentOrigin = (short)0x8000; // default program origin is 0x8000
        this.rsAddress = (short)0x0200;  // default rs origin is 0x0200
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
