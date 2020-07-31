package assembler;

import javafx.util.Pair;

class AddressingMode {
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
    static final int Implied = 10;    // indicates this is a standalone instruction
    static final int Relative = 11;    // indicates the branch to which certain instructions jump

    /*
    private Pair<Boolean, Integer> immediate;
    private Pair<Boolean, Integer> zero_page;
    private Pair<Boolean, Integer> zero_page_x;
    private Pair<Boolean, Integer> zero_page_y;
    private Pair<Boolean, Integer> absolute;
    private Pair<Boolean, Integer> absolute_x;
    private Pair<Boolean, Integer> absolute_y;
    private Pair<Boolean, Integer> indirect;
    private Pair<Boolean, Integer> indirect_x;
    private Pair<Boolean, Integer> indirect_y;
    private Pair<Boolean, Integer> implied;
    private Pair<Boolean, Integer> relative;

    AddressingMode(
            Pair<Boolean, Integer> immediate,
            Pair<Boolean, Integer> zero_page,
            Pair<Boolean, Integer> zero_page_x,
            Pair<Boolean, Integer> zero_page_y,
            Pair<Boolean, Integer> absolute,
            Pair<Boolean, Integer> absolute_x,
            Pair<Boolean, Integer> absolute_y,
            Pair<Boolean, Integer> indirect,
            Pair<Boolean, Integer> indirect_x,
            Pair<Boolean, Integer> indirect_y,
            Pair<Boolean, Integer> implied,
            Pair<Boolean, Integer> relative
    ) {
        this.immediate = immediate;
        this.zero_page = zero_page;
        this.zero_page_x = zero_page_x;
        this.zero_page_y = zero_page_y;
        this.absolute = absolute;
        this.absolute_x = absolute_x;
        this.absolute_y = absolute_y;
        this.indirect = indirect;
        this.indirect_x = indirect_x;
        this.indirect_y = indirect_y;
        this.implied = implied;
        this.relative = relative;
    }
    */
}
