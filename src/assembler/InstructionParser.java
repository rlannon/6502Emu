package assembler;

class InstructionParser {
    // A class to parse instructions

    private static final String ZERO_PATTERN = "\\$[0-fF][0-fF],?";
    private static final String ABSOLUTE_PATTERN = "([a-zA-Z_]+[0-9a-zA-Z_]+)|(\\$[0-fF][0-fF][0-fF][0-fF],?)";    // either a label or an address
    private static final String INDIRECT_Y_PATTERN = "(\\([a-zA-Z_][0-9a-zA-Z_]+\\),)|(\\(\\$[0-fF][0-fF]\\),)";  // must be followed by "Y"
    private static final String INDIRECT_X_PATTERN = "(\\([a-zA-Z_][0-9a-zA-Z_]+,)|(\\(\\$[0-fF][0-fF],)"; // must be followed by "X)"

    private static final Instruction[] OPCODES = {
            /* Immediate, Zero, ZeroX, ZeroY, Absolute, AbsoluteX, AbsoluteY, Indirect, IndirectX, IndirectY, Single, Relative */
            new Instruction("ADC", new byte[]{0x069, 0x65, 0x75, (byte)0xFF, 0x6d, 0x7d, 0x79, (byte)0xFF, 0x61, 0x71, (byte)0xFF, (byte)0xFF}),
            new Instruction("AND", new byte[]{0x29, 0x25, 0x35, (byte)0xFF, 0x2d, 0x3d, 0x39, (byte)0xFF, 0x21, 0x31, (byte)0xFF, (byte)0xFF}),
            new Instruction("ASL", new byte[]{ (byte)0xFF, 0x06, 0x16, (byte)0xFF, 0x0e, 0x1e, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x0a, (byte)0xFF }),
            new Instruction("BIT", new byte[]{(byte)0xFF, 0x24, (byte)0xFF, (byte)0xFF, 0x2c, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("BPL", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x10}),
            new Instruction("BMI", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x30}),
            new Instruction("BVC", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x50}),
            new Instruction("BVS", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x70}),
            new Instruction("BCC", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x90}),
            new Instruction("BCS", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xb0}),
            new Instruction("BNE", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xd0}),
            new Instruction("BEQ", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xf0}),
            new Instruction("BRK", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00, (byte)0xFF}),
            new Instruction("CMP", new byte[]{(byte)0xc9, (byte)0xc5, (byte)0xd5, (byte)0xFF, (byte)0xcd, (byte)0xdd, (byte)0xd9, (byte)0xFF, (byte)0xc1, (byte)0xd1, (byte)0xFF, (byte)0xFF}),
            new Instruction("CPX", new byte[]{(byte)0xe0, (byte)0xe4, (byte)0xFF, (byte)0xFF, (byte)0xec, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("CPY", new byte[]{(byte)0xc0, (byte)0xc4, (byte)0xFF, (byte)0xFF, (byte)0xcc, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("DEC", new byte[]{(byte)0xFF, (byte)0xc6, (byte)0xd6, (byte)0xFF, (byte)0xce, (byte)0xde, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("EOR", new byte[]{0x49, 0x45, 0x55, (byte)0xFF, 0x4d, 0x5d, 0x59, (byte)0xFF, 0x41, 0x51, (byte)0xFF, (byte)0xFF}),
            new Instruction("CLC", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x18, (byte)0xFF}),
            new Instruction("SEC", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x38, (byte)0xFF}),
            new Instruction("CLI", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x58, (byte)0xFF}),
            new Instruction("SEI", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x78, (byte)0xFF}),
            new Instruction("CLV", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xb8, (byte)0xFF}),
            new Instruction("CLD", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xd8, (byte)0xFF}),
            new Instruction("SED", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xf8, (byte)0xFF}),
            new Instruction("INC", new byte[]{(byte)0xFF, (byte)0xe6, (byte)0xf6, (byte)0xFF, (byte)0xee, (byte)0xfe, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("JMP", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x4c, (byte)0xFF, (byte)0xFF, 0x6c, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("JSR", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x20, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("LDA", new byte[]{(byte)0xa9, (byte)0xa5, (byte)0xb5, (byte)0xFF, (byte)0xad, (byte)0xbd, (byte)0xb9, (byte)0xFF, (byte)0xa1, (byte)0xb1, (byte)0xFF, (byte)0xFF}),
            new Instruction("LDX", new byte[]{(byte)0xa2, (byte)0xa6, (byte)0xFF, (byte)0xb6, (byte)0xae, (byte)0xFF, (byte)0xbe, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("LDY", new byte[]{(byte)0xa0, (byte)0xa4, (byte)0xb4, (byte)0xFF, (byte)0xac, (byte)0xbc, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("LSR", new byte[]{(byte)0xFF, 0x46, 0x56, (byte)0xFF, 0x4e, 0x5e, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x4a, (byte)0xFF}),
            new Instruction("NOP", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xea, (byte)0xFF}),
            new Instruction("ORA", new byte[]{0x09, 0x05, 0x15, (byte)0xFF, 0x0d, 0x1d, 0x19, (byte)0xFF, 0x01, 0x11, (byte)0xFF, (byte)0xFF}),
            new Instruction("TAX", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xaa, (byte)0xFF}),
            new Instruction("TXA", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x8a, (byte)0xFF}),
            new Instruction("DEX", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xca, (byte)0xFF}),
            new Instruction("INX", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xe8, (byte)0xFF}),
            new Instruction("TAY", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xa8, (byte)0xFF}),
            new Instruction("TYA", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x98, (byte)0xFF}),
            new Instruction("DEY", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x88, (byte)0xFF}),
            new Instruction("INY", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xc8, (byte)0xFF}),
            new Instruction("ROR", new byte[]{(byte)0xFF, 0x66, 0x76, (byte)0xFF, 0x6e, 0x7e, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x6a, (byte)0xFF}),
            new Instruction("ROL", new byte[]{(byte)0xFF, 0x26, 0x36, (byte)0xFF, 0x2e, 0x3e, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x2a, (byte)0xFF}),
            new Instruction("RTI", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x40, (byte)0xFF}),
            new Instruction("RTS", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x60, (byte)0xFF}),
            new Instruction("SBC", new byte[]{(byte)0xe9, (byte)0xe5, (byte)0xf5, (byte)0xFF, (byte)0xed, (byte)0xfd, (byte)0xf9, (byte)0xFF, (byte)0xe1, (byte)0xf1, (byte)0xFF, (byte)0xFF}),
            new Instruction("STA", new byte[]{(byte)0xFF, (byte)0x85, (byte)0x95, (byte)0xFF, (byte)0x8d, (byte)0x9d, (byte)0x99, (byte)0xFF, (byte)0x81, (byte)0x91, (byte)0xFF, (byte)0xFF}),
            new Instruction("TXS", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x9a, (byte)0xFF}),
            new Instruction("TSX", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xba, (byte)0xFF}),
            new Instruction("PHA", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x48, (byte)0xFF}),
            new Instruction("PLA", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x68, (byte)0xFF}),
            new Instruction("PHP", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x08, (byte)0xFF}),
            new Instruction("PLP", new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x28, (byte)0xFF}),
            new Instruction("STX", new byte[]{(byte)0xFF, (byte)0x86, (byte)0xFF, (byte)0x96, (byte)0x8e, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
            new Instruction("STY", new byte[]{(byte)0xFF, (byte)0x84, (byte)0x94, (byte)0xFF, (byte)0x8c, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}),
    };

    static boolean isMnemonic(String candidate) {
        // Determines whether the supplied string is a mnemonic or not
        boolean found = false;
        int idx = 0;
        candidate = candidate.toUpperCase();    // ensure we are comparing uppercase

        while (idx < OPCODES.length && !found)
        {
            if (candidate.equals(OPCODES[idx].getMnemonic()))
            {
                found = true;
            }
            else {
                idx++;
            }
        }

        return found;
    }

    static byte getOpcode(String mnemonic, int addressingMode) throws Exception {
        boolean found = false;
        int i = 0;
        mnemonic = mnemonic.toUpperCase();

        while (i < OPCODES.length && !found)
        {
            if (mnemonic.equals(OPCODES[i].getMnemonic()))
            {
                found = true;
            }
            else
            {
                i++;
            }
        }

        if (found)
        {
            return OPCODES[i].getAddressingMode(addressingMode);
        }
        else
        {
            throw new Exception("Could not find mnemonic specified");
        }
    }

    static short parseNumber(String candidate) throws Exception {
        /*

        Given a string containing some number to parse (e.g., "#$30" or "$7"), returns the number
        This means we must parse our prefixes:
            #XX     ->  Decimal. Not allowed in this assembler (at least not yet)
            #$XX    ->  Hexadecimal value
            $XX     ->  Hex address
            #%XX    ->  Binary value
            %XX     ->  Binary address

         */

        int numIndex = 0;
        while (numIndex < candidate.length())
        {
            // if we have a letter or a number
            if (java.lang.Character.isAlphabetic(candidate.charAt(numIndex)) || java.lang.Character.isDigit(candidate.charAt(numIndex)))
            {
                break;
            }
            else
            {
                numIndex++;
            }
        }

        // we won't be passing symbols here, so we don't have to worry about that
        if (numIndex == 0)
        {
            throw new Exception("Illegal argument for instruction");
        }
        else
        {
            // get the prefix
            String prefix = candidate.substring(0, numIndex);
            if (prefix.charAt(0) == '(')
            {
                prefix = prefix.substring(1);
            }

            // get the number, removing any trailing parens/commas if there are any
            String numString = candidate.substring(numIndex);
            char lastChar = numString.charAt(numString.length() - 1);
            if (lastChar == ',' || lastChar == ')') {
                // could end with a comma
                if (lastChar == ',') {
                    numString = numString.substring(0, numString.length() - 1);
                }

                lastChar = numString.charAt(numString.length() - 1);
                // we may have an indirect mode (e.g., ($00), Y )
                if (lastChar == ')') {
                    numString = numString.substring(0, numString.length() - 1);
                } else if (!Character.toString(lastChar).matches("[0-9a-fA-F]")) {
                    // if the last character is not a valid hex number, then we have a syntax error
                    throw new Exception("Invalid syntax");
                }
            }

            // do not support decimal mode (at least not yet)
            switch (prefix) {
                case "#":
                    throw new Exception("Decimal mode currently unsupported");
                case "#$":
                case "$":
                    // binary
                    return (short) Integer.parseInt(numString, 16);
                case "#%":
                case "%":
                    return (short) Integer.parseInt(numString, 2);
                default:
                    throw new Exception("Invalid numeric prefix");
            }
        }
    }

    static int getAddressingMode(String[] data) throws Exception {
        // get the addressing mode of the instruction

        if (data.length == 1)
        {
            return AddressingMode.Single;
        }
        else
        {
            String mnemonic = data[0].toUpperCase();
            if (mnemonic.equals("BPL") || mnemonic.equals("BMI") || mnemonic.equals("BVC") || mnemonic.equals("BVS") ||
                mnemonic.equals("BCC") || mnemonic.equals("BCS") || mnemonic.equals("BNE") || mnemonic.equals("BEQ") || mnemonic.equals("BRK"))
            {
                return AddressingMode.Relative;
            }
            else
            {
                if (data[1].toUpperCase().equals("A"))
                {
                    // things like 'asl a' use this
                    return AddressingMode.Single;
                }
                else if (data[1].matches(ZERO_PATTERN))
                {
                    if (data[1].charAt(data[1].length() - 1) == ',')
                    {
                        if (data.length == 3) {
                            if (data[2].toUpperCase().equals("X")) {
                                return AddressingMode.ZeroPageX;
                            } else if (data[2].toUpperCase().equals("Y")) {
                                return AddressingMode.ZeroPageY;
                            } else {
                                throw new Exception("Invalid addressing mode!");
                            }
                        } else {
                            throw new Exception("Expected index");
                        }
                    }
                    else
                    {
                        return AddressingMode.ZeroPage;
                    }
                }
                else if (data[1].matches(ABSOLUTE_PATTERN))
                {
                    // if it ends in a comma, then we have absolute indexed
                    if (data[1].charAt(data[1].length() - 1) == ',')
                    {
                        if (data[2].toUpperCase().equals("X"))
                        {
                            return AddressingMode.AbsoluteX;
                        }
                        else if (data[2].toUpperCase().equals("Y"))
                        {
                            return AddressingMode.AbsoluteY;
                        } else {
                            throw new Exception("Invalid addressing mode");
                        }
                    } else {
                        return AddressingMode.Absolute; // otherwise, it's just plain old absolute
                    }
                }
                else if (data[0].toUpperCase().equals("JMP") && data[1].matches("\\(.+\\)"))
                {
                    // Indirect may only be used with JMP, and syntax is JMP (MEMORY)
                    return AddressingMode.Indirect;
                }
                else if (data[1].matches(INDIRECT_Y_PATTERN))
                {
                    if (data[2].toUpperCase().equals("Y")) {
                        return AddressingMode.IndirectY;
                    } else {
                        throw new Exception("Invalid addressing mode syntax");
                    }
                }
                else if (data[1].matches(INDIRECT_X_PATTERN))
                {
                    if (data[2].toUpperCase().equals("X)"))
                        return AddressingMode.IndirectX;
                    else
                        throw new Exception("Invalid addressing mode syntax");
                }
                // if the first character is a #, then it's immediate
                else if (data[1].charAt(0) == '#')
                {
                    return AddressingMode.Immediate;
                }
                else
                {
                    throw new Exception("Invalid addressing mode");
                }
            }
        }
    }

    static byte[] parseInstruction(String[] data) throws Exception {
        // Given a series of strings containing instruction mnemonics, returns the bytecode that those strings entail

        byte opcode = 0;

        int addressingMode = getAddressingMode(data);
        short value;

        opcode = getOpcode(data[0], addressingMode);

        // the opcode will be 0xFF (an invalid instruction) if we had a bad addressing mode
        if (opcode == (byte)0xFF)
        {
            throw new Exception("Invalid opcode or addressing mode");
        }
        else {
            if (addressingMode == AddressingMode.Single) {
                // instruction width 1
                return new byte[]{opcode};
            } else {
                // if we have a label, the value should be 0; otherwise, use parseNumber
                value = (data[1].matches("\\.?[a-zA-Z_]+.+")) ? 0x00 : parseNumber(data[1]);

                // get instruction width based on addressing mode
                if (addressingMode == AddressingMode.Immediate || addressingMode == AddressingMode.IndirectX ||
                        addressingMode == AddressingMode.IndirectY || addressingMode == AddressingMode.Relative ||
                        addressingMode == AddressingMode.ZeroPage || addressingMode == AddressingMode.ZeroPageX ||
                        addressingMode == AddressingMode.ZeroPageY) {
                    // instruction width 2
                    byte[] operand = new byte[]{(byte) (value & 0xFF)};
                    return new byte[]{opcode, operand[0]};
                } else if (addressingMode == AddressingMode.Absolute || addressingMode == AddressingMode.AbsoluteX ||
                        addressingMode == AddressingMode.AbsoluteY || addressingMode == AddressingMode.Indirect) {
                    // instruction width 3
                    byte[] operand = new byte[]{(byte) (value & 0xFF), (byte) ((value >> 8) & 0xFF)};
                    return new byte[]{opcode, operand[0], operand[1]};
                } else {
                    throw new Exception("Invalid syntax");
                }
            }
        }
    }
}
