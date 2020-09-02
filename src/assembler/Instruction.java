package assembler;

import java.util.Arrays;
import java.util.Vector;

class UnknownInstructionException extends Exception {
    UnknownInstructionException() {
        super();
    }
}

class Instruction {
    final private String mnemonic;
    final private Vector<OpcodeInformation> modes;

    String getMnemonic()
    {
        return this.mnemonic;
    }

    byte[] getOpcodes() {
        byte[] b = new byte[this.modes.size()];
        for (int i = 0; i < this.modes.size(); i++) {
            b[i] = this.modes.get(i).getOpcode();
        }
        return b;
    }

    byte getOpcode(AddressingMode mode) throws Exception {
        // Gets the opcode associated with the specified addressing mode for this instruction
        int i = 0;
        boolean found = false;
        while (!found && i < this.modes.size()) {
            if (this.modes.get(i).getAddressingMode() == mode) {
                found = true;
            }
            else {
                i++;
            }
        }

        byte b;
        if (found) {
            b = this.modes.get(i).getOpcode();
        }
        else {
            throw new Exception("This addressing mode is not supported for the instruction");
        }

        return b;
    }

    boolean supportsAddressingMode(AddressingMode mode) {
        // Checks whether the addressing mode specified exists for this instruction
        int i = 0;
        boolean found = false;
        while (!found && i < this.modes.size()) {
            if (this.modes.get(i).getAddressingMode() == mode) {
                found = true;
            }
            else {
                i++;
            }
        }

        return found;
    }

    Vector<OpcodeInformation> getModes() {
        return this.modes;
    }

    Instruction(String mnemonic, OpcodeInformation[] opcodes) {
        // Create the table containing the
        this.modes = new Vector<>();
        this.modes.addAll(Arrays.asList(opcodes));
        this.mnemonic = mnemonic;
    }

    Instruction(String mnemonic, OpcodeInformation opcode) {
        this.modes = new Vector<>();
        this.modes.add(opcode);
        this.mnemonic = mnemonic;
    }

    Instruction() {
        this.modes = new Vector<>();
        this.mnemonic = "";
    }
}
