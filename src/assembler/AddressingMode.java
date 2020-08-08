package assembler;

public enum AddressingMode {
    // contains constants for addressing modes
    // these allow us to index into OPCODES for the appropriate instruction

    Immediate,
    ZeroPage,
    ZeroPageX,
    ZeroPageY,
    Absolute,
    AbsoluteX,
    AbsoluteY,
    Indirect,   // may only be used with a jump instruction
    IndirectY,
    IndirectX,
    Implied,    // indicates this is a standalone instruction
    Relative    // indicates the branch to which certain instructions jump
}
