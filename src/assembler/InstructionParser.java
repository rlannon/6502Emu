package assembler;

class InstructionParser {
    // A class to parse instructions

    private static final String ZERO_PATTERN = "\\$[0-fF][0-fF],?";
    private static final String ABSOLUTE_PATTERN = "([a-zA-Z_]+[0-9a-zA-Z_]+,?)|(\\$[0-fF][0-fF][0-fF][0-fF],?)";    // either a label or an address
    private static final String INDIRECT_Y_PATTERN = "(\\([a-zA-Z_][0-9a-zA-Z_]+\\),)|(\\(\\$[0-fF][0-fF]\\),)";  // must be followed by "Y"
    private static final String INDIRECT_X_PATTERN = "(\\([a-zA-Z_][0-9a-zA-Z_]+,)|(\\(\\$[0-fF][0-fF],)"; // must be followed by "X)"

    private static final Instruction[] OPCODES = {
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

            new Instruction("ALR", new byte[]{(byte)0x4b, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}),
            new Instruction("ANC", new byte[]{(byte)0x0b, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}),
            new Instruction("ARR", new byte[]{(byte)0x6b, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}),
    };

    static boolean supportsAddressingMode(String mnemonic, AddressingMode mode) throws Exception {
        /*
        Determines whether the instruction in question supports the addressing mode specified
         */

        mnemonic = mnemonic.toUpperCase();
        Instruction toTest = null;
        for (Instruction cur: OPCODES) {
            if (cur.getMnemonic().equals(mnemonic)) {
                toTest = cur;
                break;
            }
        }

        if (toTest == null)
            throw new Exception("No such mnemonic");
        else
            return toTest.supportsAddressingMode(mode);
    }

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

    static byte getOpcode(String mnemonic, AddressingMode mode) throws Exception {
        // Gets the opcode for the instruction
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
            return OPCODES[i].getOpcode(mode);
        }
        else
        {
            throw new Exception("Could not find mnemonic specified");
        }
    }

    static OpcodeInformation getMnemonic(byte opcode) throws Exception {
        // Gets the mnemonic and addressing mode of the instruction with a given opcode
        boolean found = false;
        int i = 0, j = 0;
        while (i < OPCODES.length && !found) {
            byte[] addressingModes = OPCODES[i].getOpcodes();
            j = 0;
            while (j < addressingModes.length && !found) {
                if (addressingModes[j] == opcode) {
                    found = true;
                } else {
                    j++;
                }
            }
            
            if (!found) {
                i++;
            }
        }

        if (found) {
            return new OpcodeInformation(OPCODES[i].getAddressingMode(opcode), opcode);
        } else {
            throw new Exception("Illegal instruction");
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

            // We will support parsing decimal integers, but BCD is not supported
            switch (prefix) {
                case "#":
                    return (short) Integer.parseInt(numString, 10);
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

    static AddressingMode getAddressingMode(String[] data) throws Exception {
        // get the addressing mode of the instruction

        if (data.length == 1)
        {
            return AddressingMode.Implied;
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
                    return AddressingMode.Implied;
                }
                // if the first character is a #, then it's immediate
                else if (data[1].charAt(0) == '#')
                {
                    return AddressingMode.Immediate;
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
                                throw new Exception("Invalid addressing mode");
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
                else if (data[0].toUpperCase().equals("JMP") && data[1].matches(Assembler.SYMBOL_NAME_REGEX)) {
                    // JMP may use absolute as well
                    return AddressingMode.Absolute;
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

        AddressingMode mode = getAddressingMode(data);
        short value;

        opcode = getOpcode(data[0], mode);

        // the opcode will be 0xFF (an invalid instruction) if we had a bad addressing mode
        if (opcode == (byte)0xFF)
        {
            throw new Exception("Invalid opcode or addressing mode");
        }
        else {
            if (mode == AddressingMode.Implied) {
                // instruction width 1
                return new byte[]{opcode};
            } else {
                // if we have a label, the value should be 0; otherwise, use parseNumber
                value = (data[1].matches(Assembler.SYMBOL_NAME_REGEX)) ? 0x00 : parseNumber(data[1]);

                // get instruction width based on addressing mode
                if (mode == AddressingMode.Immediate || mode == AddressingMode.IndirectX ||
                        mode == AddressingMode.IndirectY || mode == AddressingMode.Relative ||
                        mode == AddressingMode.ZeroPage || mode == AddressingMode.ZeroPageX ||
                        mode == AddressingMode.ZeroPageY) {
                    // instruction width 2
                    byte[] operand = new byte[]{(byte) (value & 0xFF)};
                    return new byte[]{opcode, operand[0]};
                } else if (mode == AddressingMode.Absolute || mode == AddressingMode.AbsoluteX ||
                        mode == AddressingMode.AbsoluteY || mode == AddressingMode.Indirect) {
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
