package emu_format;

import assembler.DebugSymbol;

import java.io.*;
import java.util.Vector;

public class EmuFile {
    /*

    The class for implementing and interacting with .emu files
    The .emu file is meant for use in the emulator for loading programs. Two variations exist:
        - RAW: contains only a brief header and the program data. This is loaded at 0x8000
        - Bank: contains banks, which are specified by ORG, and loaded into program memory accordingly
    The format is structured as follows:
        prg_header:
            - (5) magic_number: 0x19 0x66 'E' 'M' 'U'
            - (1) file_type:    0x00 = RAW, 0x01 = bank
            - (2) num_banks
            - (2) prg_len: only used if the file type is RAW. Otherwise, should be 0x00 0x00
        body:
        If type is raw:
            - (prg_len) prg ->  The program data
        If the type is bank:
            For each according to (prg_header.num_banks):
                .bank_header:
                    (2) org ->  The address where the code should go
                    (2) len ->  The length of the data in the bank
                .body:
                    (.bank_header.len) data ->  The prg data
     */

    Vector<Bank> prgBanks;
    Vector<DebugSymbol> debugSymbols;

    private final static byte[] MAGIC_NUMBER = { (byte)0xC0, 'E', 'M', 'U' };

    public static EmuFile loadEmuFile(String filename)
    {
        // Loads the data in .emu file 'filename' and returns the object
        EmuFile em = null;
        DataInputStream in = null;

        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
            Vector<Bank> fileBanks = new Vector<>();
            Vector<DebugSymbol> fileDebugSymbols = new Vector<>();

            // load the header
            byte[] magic_number = in.readNBytes(4);
            if (java.util.Arrays.equals(magic_number, MAGIC_NUMBER)) {
                // since the magic number is valid, attempt to read the file
                int numBanks = in.readByte();   // use int so that 0xFF will be interpreted as 255, not -1 (this will mess up the for loop)
                int numDebugSymbols = in.readShort();   // again, use int so we can make accurate comparisons

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
                for (int i = 0; i < numDebugSymbols; i++)
                {
                    // fetch the data from the file
                    int lineNumber = in.readInt();
                    short address = in.readShort();

                    // construct the symbol and add it to the vector
                    fileDebugSymbols.add(new DebugSymbol(lineNumber, address));
                }

                // finally, construct the EmuFile object
                em = new EmuFile(fileBanks, fileDebugSymbols);

                in.close();
            } else {
                throw new Exception("Invalid magic number in .emu file");
            }
        }
        catch (Exception e)
        {
            System.out.println("Error loading emu file: " + e.toString());
        }

        return em;
    }

    public void writeEmuFile(String filename)
    {
        // Writes the current EmuFile object to file 'filename'

        DataOutputStream out = null;

        try
        {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
            // write the header
            out.write(MAGIC_NUMBER); // magic number
            out.writeByte((byte)prgBanks.size());   // number of banks
            out.writeShort((short)this.debugSymbols.size());    // number of debug symbols

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

            // write debug symbols -- first line number, then address
            for (DebugSymbol debugSymbol: this.debugSymbols)
            {
                out.writeInt(debugSymbol.getLine());
                out.writeShort(debugSymbol.getAddress());
            }

            out.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public Vector<Bank> getPrgBanks()
    {
        return this.prgBanks;
    }

    public Vector<DebugSymbol> getDebugSymbols()
    {
        return this.debugSymbols;
    }

    public EmuFile() {
        this.prgBanks = new Vector<>();
    }

    public EmuFile(Vector<Bank> banks)
    {
        this.prgBanks = banks;
    }

    public EmuFile(Vector<Bank> banks, Vector<DebugSymbol> debugSymbols)
    {
        this(banks);
        this.debugSymbols = debugSymbols;
    }
}
