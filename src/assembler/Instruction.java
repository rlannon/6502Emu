package assembler;

import java.util.Hashtable;

public class Instruction {
    private String mnemonic;
    private boolean standalone;
    private byte[] addressingModes;
    private Hashtable<String, Integer> addrM;

    public String getMnemonic()
    {
        return this.mnemonic;
    }

    public byte getAddressingMode(int index)
    {
        if (index < addressingModes.length)
        {
            return this.addressingModes[index];
        }
        else
        {
            throw new IndexOutOfBoundsException("Cannot access Instruction.addressingModes of the index specified");
        }
    }

    public Instruction(String mnemonic, byte[] addressingModes)
    {
        this.mnemonic = mnemonic;
        this.addressingModes = addressingModes;
    }

    public Instruction(String mnemonic, String[] addressingModeName, int[] modeOpcode)
    {
        // will allow us to change over the internal functionality in the future

        this.addrM = new Hashtable<>(56);

        assert addressingModeName.length == modeOpcode.length: "Unequal modes and opcodes";
        for (int i = 0; i < addressingModeName.length; i++) {
            this.addrM.put(addressingModeName[i], modeOpcode[i]);
        }
    }
}
