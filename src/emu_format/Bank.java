package emu_format;

import java.util.Vector;

public class Bank {
    /*

    Contains bank information for an emu file

     */

    private short org;  // the origin address
    private byte[] data;    // the prg bytecode

    public short getOrg()
    {
        return this.org;
    }

    public byte[] getData()
    {
        return this.data;
    }

    public void setData(int offset, byte[] newData)
    {
        for (int i = 0; i < newData.length; i++, offset++)
        {
            this.data[offset] = newData[i];
        }
    }

    public Bank(short org, Vector<Byte> data)
    {
        this.org = org;
        this.data = new byte[data.size()];
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = data.elementAt(i);
        }
    }

    Bank(short org, byte[] data)
    {
        this.org = org;
        this.data = data;
    }
}
