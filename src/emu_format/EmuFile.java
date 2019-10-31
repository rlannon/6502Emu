package emu_format;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
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

    public void writeEmuFile(String filename)
    {
        DataOutputStream out = null;

        try
        {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
            // write the header
            out.write(new byte[]{ (byte)0xC0, 'E', 'M', 'U' });
            out.writeByte((byte)0x01);
            out.writeByte((byte)prgBanks.size());
            out.writeShort((short)0x00);

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

            out.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void addBank(short org, byte[] data)
    {
        this.prgBanks.add(new Bank(org, data));
    }

    public EmuFile() {
        this.prgBanks = new Vector<>();
    }

    public EmuFile(Vector<Bank> banks)
    {
        this.prgBanks = banks;
    }
}
