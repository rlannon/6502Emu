package assembler;

final class AllInstructions {
    static final Instruction[] INSTRUCTIONS = {
        /*
         * Implements some unofficial addressing modes and opcodes
         */

        new Instruction(
            "ADC",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0x69),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x65),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x75),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x6d),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x79),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0x61),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0x71)
            }
        ),
        new Instruction(
            "AND",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0x29),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x25),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x35),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x2d),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x3d),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0x39),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0x21),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0x31)
            }
        ),
        new Instruction(
            "ASL",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x06),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x16),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x0e),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x1e),
                new OpcodeInformation(AddressingMode.Implied, (byte)0x0a)
            }
        ),
        new Instruction(
            "BIT",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x24),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x2c),
            }
        ),
        new Instruction("BPL", new OpcodeInformation(AddressingMode.Relative, (byte)0x10)),
        new Instruction("BMI", new OpcodeInformation(AddressingMode.Relative, (byte)0x30)),
        new Instruction("BVC", new OpcodeInformation(AddressingMode.Relative, (byte)0x50)),
        new Instruction("BVS", new OpcodeInformation(AddressingMode.Relative, (byte)0x70)),
        new Instruction("BCC", new OpcodeInformation(AddressingMode.Relative, (byte)0x90)),
        new Instruction("BCS", new OpcodeInformation(AddressingMode.Relative, (byte)0xb0)),
        new Instruction("BNE", new OpcodeInformation(AddressingMode.Relative, (byte)0xd0)),
        new Instruction("BEQ", new OpcodeInformation(AddressingMode.Relative, (byte)0xf0)),
        new Instruction("BRK", new OpcodeInformation(AddressingMode.Implied, (byte)0x00)),
        new Instruction(
            "CMP",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xc9),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xc5),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0xd5),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xcd),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0xdd),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0xd9),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0xc1),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0xd1)
            }
        ),
        new Instruction(
            "CPX",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xe0),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xe4),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xec),
            }
        ),
        new Instruction(
            "CPY",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xc0),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xc4),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xcc),
            }
        ),
        new Instruction(
            "DEC",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xc6),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0xd6),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xce),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0xde),
            }
        ),
        new Instruction(
            "EOR",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0x49),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x45),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x55),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x4d),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x5d),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0x59),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0x41),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0x51),
            }
        ),
        new Instruction("CLC", new OpcodeInformation(AddressingMode.Implied, (byte)0x18)),
        new Instruction("SEC", new OpcodeInformation(AddressingMode.Implied, (byte)0x38)),
        new Instruction("CLI", new OpcodeInformation(AddressingMode.Implied, (byte)0x58)),
        new Instruction("SEI", new OpcodeInformation(AddressingMode.Implied, (byte)0x78)),
        new Instruction("CLV", new OpcodeInformation(AddressingMode.Implied, (byte)0xb8)),
        new Instruction("CLD", new OpcodeInformation(AddressingMode.Implied, (byte)0xd8)),
        new Instruction("SED", new OpcodeInformation(AddressingMode.Implied, (byte)0xf8)),
        new Instruction(
            "INC",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xe6),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0xf6),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xee),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0xfe),
            }
        ),
        new Instruction(
            "JMP",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x4c),
                new OpcodeInformation(AddressingMode.Indirect, (byte)0x6c)
            }
        ),
        new Instruction("JSR", new OpcodeInformation(AddressingMode.Absolute, (byte)0x20)),
        new Instruction(
            "LDA",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xa9),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xa5),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0xb5),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xad),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0xbd),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0xb9),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0xa1),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0xb1),
            }
        ),
        new Instruction(
            "LDX",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xa2),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xa6),
                new OpcodeInformation(AddressingMode.ZeroPageY, (byte)0xb6),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xae),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0xbe),
            }
        ),
        new Instruction(
            "LDX",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xa0),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xa4),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0xb4),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xac),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0xbc),
            }
        ),
        new Instruction(
            "LSR",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x46),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x56),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x4e),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x5e),
                new OpcodeInformation(AddressingMode.Implied, (byte)0x4a)
            }
        ),
        new Instruction(
            "NOP",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0x80, false),   // unofficial
                new OpcodeInformation(AddressingMode.Implied, (byte)0xea)
            }
        ),
        new Instruction(
            "ORA",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0x09),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x05),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x15),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x0d),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x1d),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0x19),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0x01),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0x11)
            }
        ),
        new Instruction("TAX", new OpcodeInformation(AddressingMode.Implied, (byte)0xaa)),
        new Instruction("TXA", new OpcodeInformation(AddressingMode.Implied, (byte)0x8a)),
        new Instruction("DEX", new OpcodeInformation(AddressingMode.Implied, (byte)0xca)),
        new Instruction("INX", new OpcodeInformation(AddressingMode.Implied, (byte)0xe8)),
        new Instruction("TAY", new OpcodeInformation(AddressingMode.Implied, (byte)0xa8)),
        new Instruction("TYA", new OpcodeInformation(AddressingMode.Implied, (byte)0x98)),
        new Instruction("DEY", new OpcodeInformation(AddressingMode.Implied, (byte)0x88)),
        new Instruction("INY", new OpcodeInformation(AddressingMode.Implied, (byte)0xc8)),
        new Instruction(
            "ROR",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x66),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x76),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x6e),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x7e),
                new OpcodeInformation(AddressingMode.Implied, (byte)0x6a)
            } 
        ),
        new Instruction(
            "ROL",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x26),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x36),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x2e),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x3e),
                new OpcodeInformation(AddressingMode.Implied, (byte)0x2a)
            } 
        ),
        new Instruction("RTI", new OpcodeInformation(AddressingMode.Implied, (byte)0x40)),
        new Instruction("RTS", new OpcodeInformation(AddressingMode.Implied, (byte)0x60)),
        new Instruction(
            "SBC",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.Immediate, (byte)0xe9),
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xe5),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0xf5),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xed),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0xfd),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0xf9),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0xe1),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0xf1),
            }
        ),
        new Instruction(
            "STA",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x85),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x95),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x8d),
                new OpcodeInformation(AddressingMode.AbsoluteX, (byte)0x9d),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0x99),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0x81),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0x99)
            }
        ),
        new Instruction("TXS", new OpcodeInformation(AddressingMode.Implied, (byte)0x9a)),
        new Instruction("TSX", new OpcodeInformation(AddressingMode.Implied, (byte)0xba)),
        new Instruction("PHA", new OpcodeInformation(AddressingMode.Implied, (byte)0x48)),
        new Instruction("PLA", new OpcodeInformation(AddressingMode.Implied, (byte)0x68)),
        new Instruction("PHP", new OpcodeInformation(AddressingMode.Implied, (byte)0x08)),
        new Instruction("PLP", new OpcodeInformation(AddressingMode.Implied, (byte)0x28)),
        new Instruction(
            "STX",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x86),
                new OpcodeInformation(AddressingMode.ZeroPageY, (byte)0x96),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x8e)
            }
        ),
        new Instruction(
            "STY",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x84),
                new OpcodeInformation(AddressingMode.ZeroPageX, (byte)0x94),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x8c)
            }
        ),
        
        /* Support some unofficial opcodes */

        new Instruction(
            "LAX",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0xa7, false),
                new OpcodeInformation(AddressingMode.ZeroPageY, (byte)0xb7, false),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0xaf, false),
                new OpcodeInformation(AddressingMode.AbsoluteY, (byte)0xbf, false),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0xa3, false),
                new OpcodeInformation(AddressingMode.IndirectY, (byte)0xb3, false)
            }
        ),
        new Instruction(
            "SAX",
            new OpcodeInformation[]{
                new OpcodeInformation(AddressingMode.ZeroPage, (byte)0x87, false),
                new OpcodeInformation(AddressingMode.ZeroPageY, (byte)0x97, false),
                new OpcodeInformation(AddressingMode.Absolute, (byte)0x8f, false),
                new OpcodeInformation(AddressingMode.IndirectX, (byte)0x83, false)
            }
        ),
        new Instruction("ALR", new OpcodeInformation(AddressingMode.Immediate, (byte)0x4b, false)),
        new Instruction("ANC", new OpcodeInformation(AddressingMode.Immediate, (byte)0x0b, false)),
        new Instruction("ARR", new OpcodeInformation(AddressingMode.Immediate, (byte)0x6b, false)),
    };
}
