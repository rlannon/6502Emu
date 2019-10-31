package emu_format;

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

    int dataSize() { return this.data.length; }

    public Bank(short org, byte[] data)
    {
        this.org = org;
        this.data = data;
    }
}
