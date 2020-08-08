package assembler;

class OpcodeInformation {
    private boolean official;   // unofficial opcodes are supported, but the assembler should warn the user if the are used
    private AddressingMode mode;    // the addressing mode of the opcode
    private byte opcode;    // the actual opcode

    AddressingMode getAddresingMode() {
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