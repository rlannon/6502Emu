package emu_format;

import assembler.DebugSymbol;
import emu.Input;

import java.io.*;
import java.util.Vector;

public class EmuFile {
    /*

    The class for implementing and interacting with .emu files
    See "Emu Format Description.txt" for information on the format

     */

    // todo: if file is being used by another process, display in user console?

    private Vector<Bank> prgBanks;
    private Vector<DebugSymbol> debugSymbols;
    private Vector<Input> configuredInputs;

    private final static byte[] MAGIC_NUMBER = { (byte)0xC0, 'E', 'M', 'U' };
    private final static int VERSION = 1;

    /*

    Methods

    */

    public static EmuFile loadEmuFile(String filename) {
        // Loads the data in .emu file 'filename' and returns the object
        EmuFile em = null;
        DataInputStream in = null;

        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
            Vector<Bank> fileBanks = new Vector<>();
            Vector<DebugSymbol> fileDebugSymbols = new Vector<>();
            Vector<Input> configuredInputs = new Vector<>();

            // load the header
            byte[] magic_number = in.readNBytes(4);
            if (java.util.Arrays.equals(magic_number, MAGIC_NUMBER)) {
                // since the magic number is valid, attempt to read the file
                int version = in.readShort();
                if (version == VERSION) {
                    int numBanks = in.readByte();   // use int so that 0xFF will be interpreted as 255, not -1 (this will mess up the for loop)
                    int numDebugSymbols = in.readShort();   // again, use int so we can make accurate comparisons
                    boolean config = in.readBoolean();
                    in.readNBytes(6);   // 6 bytes reserved for future use

                    // read in our bank data
                    for (int i = 0; i < numBanks; i++) {
                        // fetch the data from the file
                        short origin = in.readShort();
                        int numBytes = in.readShort();
                        byte[] data = in.readNBytes(numBytes);

                        // construct the bank
                        fileBanks.add(new Bank(origin, data));
                    }

                    // read in our debug symbols
                    for (int i = 0; i < numDebugSymbols; i++) {
                        int labelLength = in.readInt();
                        char[] characters = new char[labelLength];
                        for (int charIndex = 0; charIndex < labelLength; charIndex++) {
                            characters[charIndex] = in.readChar();
                        }
                        String label = new String(characters);

                        // fetch the data from the file
                        int lineNumber = in.readInt();
                        short address = in.readShort();

                        // construct the symbol and add it to the vector
                        fileDebugSymbols.add(new DebugSymbol(lineNumber, address, label));
                    }

                    // read in our config data
                    if (config) {
                        int numConfigs = in.readShort();
                        for (int i = 0; i < numConfigs; i++) {
                            int address = in.readShort() & 0xFFFF;
                            boolean triggersIRQ = in.readBoolean();
                            int keyCodeLen = in.readByte() & 0xFF;
                            char[] chars = new char[keyCodeLen];
                            for (int j = 0; j < keyCodeLen; j++) {
                                chars[j] = in.readChar();
                            }
                            String keyCode = new String(chars);

                            Input toAdd = new Input(keyCode, address, triggersIRQ);
                            configuredInputs.add(toAdd);
                        }
                    }

                    // finally, construct the EmuFile object
                    em = new EmuFile(fileBanks, fileDebugSymbols, configuredInputs);

                    in.close();
                } else {
                    throw new Exception("Incompatible .emu file version");
                }
            } else {
                throw new Exception("Invalid magic number in .emu file");
            }
        }
        catch (Exception e)
        {
            System.out.println("Error loading emu file: " + e.getMessage());
        }

        return em;
    }

    public void writeEmuFile(String filename) {
        // Writes the current EmuFile object to file 'filename'

        DataOutputStream out = null;

        try
        {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + ".emu")));
            // write the header
            out.write(MAGIC_NUMBER); // magic number
            out.writeShort(VERSION);    // version
            out.writeByte((byte)prgBanks.size());   // number of banks
            out.writeShort(this.debugSymbols.size() & 0xFFFF);    // number of debug symbols
            out.writeBoolean(this.configuredInputs != null && this.configuredInputs.size() > 0);    // whether we have inputs
            out.write(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});  // write empty bytes

            // write bank data
            for (Bank bank: this.prgBanks)
            {
                out.writeShort(bank.getOrg());
                out.writeShort((short)bank.getData().length);
                byte[] data = bank.getData();
                for (byte b: data)
                {
                    out.writeByte(b);
                }
            }

            /*
            Write debug symbols:
                int: label -> length
                char: foreach (char: label)
                int: line number
                short: address
             */
            for (DebugSymbol debugSymbol: this.debugSymbols)
            {
                char[] characters = debugSymbol.getLabel().toCharArray();
                out.writeInt(characters.length);
                for (char c: characters) {
                    out.writeChar(c);
                }

                out.writeInt(debugSymbol.getLine());
                out.writeShort(debugSymbol.getAddress());
            }

            /*
            Write configured inputs:

             */
            if (this.configuredInputs != null && this.configuredInputs.size() > 0) {
                out.writeShort((short)this.configuredInputs.size());
                for (Input toWrite: this.configuredInputs) {
                    out.writeShort((short)toWrite.getAddress());
                    out.writeBoolean(toWrite.isTriggersIRQ());
                    byte codeLen = (byte)toWrite.getMappedKeyCode().length();
                    out.writeByte(codeLen);
                    char[] s = toWrite.getMappedKeyCode().toCharArray();
                    for (char c: s) {
                        out.writeChar(c);
                    }
                }
            }

            out.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public Vector<Bank> getPrgBanks() {
        return this.prgBanks;
    }

    public Vector<DebugSymbol> getDebugSymbols() {
        return this.debugSymbols;
    }

    public EmuFile(Vector<Bank> banks, Vector<DebugSymbol> debugSymbols, Vector<Input> configuredInputs) {
        this.prgBanks = banks;
        this.debugSymbols = debugSymbols;
        this.configuredInputs = configuredInputs;
    }

    public EmuFile(Vector<Bank> banks) {
        this(banks, null, null);
    }

    public EmuFile(Vector<Bank> banks, Vector<DebugSymbol> debugSymbols) {
        this(banks, debugSymbols, null);
    }

    public EmuFile() {
        this.prgBanks = new Vector<>();
    }
}
