package assembler;

import java.util.Hashtable;

class Instruction {
    private String mnemonic;
    private byte[] addressingModes;
    private Hashtable<String, Integer> addrM;

    String getMnemonic()
    {
        return this.mnemonic;
    }

    byte getAddressingMode(int index) {
        if (index < addressingModes.length)
        {
            return this.addressingModes[index];
        }
        else
        {
            throw new IndexOutOfBoundsException("Cannot access Instruction.addressingModes of the index specified");
        }
    }

    byte[] getAddressingModes() {
        return this.addressingModes;
    }

    Instruction(String mnemonic, byte[] addressingModes) {
        this.mnemonic = mnemonic;
        this.addressingModes = addressingModes;
    }

    Instruction(String mnemonic, String[] addressingModeName, int[] modeOpcode) {
        // will allow us to change over the internal functionality in the future

        this.addrM = new Hashtable<>(56);

        assert addressingModeName.length == modeOpcode.length: "Unequal modes and opcodes";
        for (int i = 0; i < addressingModeName.length; i++) {
            this.addrM.put(addressingModeName[i], modeOpcode[i]);
        }
    }
}
