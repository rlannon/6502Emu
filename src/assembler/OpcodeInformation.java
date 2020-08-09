package assembler;

class OpcodeInformation {
    final private boolean official;   // unofficial opcodes are supported, but the assembler should warn the user if the are used
    final private AddressingMode mode;    // the addressing mode of the opcode
    final private byte opcode;    // the actual opcode

    AddressingMode getAddressingMode() {
        return this.mode;
    }

    byte getOpcode() {
        return this.opcode;
    }

    boolean isOfficial() {
        return this.official;
    }

    OpcodeInformation(AddressingMode mode, byte opcode, boolean official) {
        this.mode = mode;
        this.opcode = opcode;
        this.official = official;
    }

    OpcodeInformation(AddressingMode mode, byte opcode) {
        this(mode, opcode, true);
    }
}