package assembler;

import java.util.Vector;

class UnknownInstructionException extends Exception {
    UnknownInstructionException() {
        super();
    }
}

class Instruction {
    private String mnemonic;
    private Vector<OpcodeInformation> modes;

    String getMnemonic()
    {
        return this.mnemonic;
    }

    AddressingMode getAddressingMode(byte opcode) throws Exception {
        // Gets the addressing mode associated with the specified opcode
        int i = 0;
        boolean found = false;
        while (i < this.modes.size() && !found) {
            if (this.modes.get(i).getOpcode() == opcode) {
                found = true;
            }
            else {
                i++;
            }
        }

        if (found) {
            return this.modes.get(i).getAddresingMode();
        }
        else {
            throw new Exception("No such opcode for instruction '" + this.mnemonic + "'");
        }
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
            if (this.modes.get(i).getAddresingMode() == mode) {
                found = true;
            }
            else {
                i++;
            }
        }

        byte b = 0;
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
            if (this.modes.get(i).getAddresingMode() == mode) {
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

    Instruction(String mnemonic, byte[] addressingModes) {
        this.mnemonic = mnemonic;
        // this.addressingModes = addressingModes;
        // todo: refactor to populate by addressing mode
        this.modes = new Vector<>();
        AddressingMode modes[] = {
            AddressingMode.Immediate,
            AddressingMode.ZeroPage,
            AddressingMode.ZeroPageX,
            AddressingMode.ZeroPageY,
            AddressingMode.Absolute,
            AddressingMode.AbsoluteX,
            AddressingMode.AbsoluteY,
            AddressingMode.Indirect,
            AddressingMode.IndirectX,
            AddressingMode.IndirectY,
            AddressingMode.Implied,
            AddressingMode.Relative
        };
        for (int i = 0; i < addressingModes.length; i++) {
            if (addressingModes[i] == (byte)0xFF) {
                continue;
            }
            else {
                this.modes.add(new OpcodeInformation(modes[i], addressingModes[i], true));
            }
        }
    }

    Instruction(String mnemonic, OpcodeInformation[] opcodes) {
        // Create the table containing the
        this.modes = new Vector<>();
        for (OpcodeInformation oi: opcodes) {
            this.modes.add(oi);
        }
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
