package assembler;

public class AddressingMode {
    // contains constants for addressing modes
    // these allow us to index into OPCODES for the appropriate instruction

    static final int Immediate = 0;
    static final int ZeroPage = 1;
    static final int ZeroPageX = 2;
    static final int ZeroPageY = 3;
    static final int Absolute = 4;
    static final int AbsoluteX = 5;
    static final int AbsoluteY = 6;
    static final int Indirect = 7;  // may only be used with a jump instruction
    static final int IndirectX = 8;
    static final int IndirectY = 9;
    static final int Single = 10;    // indicates this is a standalone instruction
    static final int Relative = 11;    // indicates the branch to which certain instructions jump
}
