package assembler;

class InstructionParser {
    // A class to parse instructions

    private static final String ZERO_PATTERN = "\\$[0-fF][0-fF],?";
    private static final String ABSOLUTE_PATTERN = "([a-zA-Z_]+[0-9a-zA-Z_]+,?)|(\\$[0-fF][0-fF][0-fF][0-fF],?)";    // either a label or an address
    private static final String INDIRECT_Y_PATTERN = "(\\([a-zA-Z_][0-9a-zA-Z_]+\\),)|(\\(\\$[0-fF][0-fF]\\),)";  // must be followed by "Y"
    private static final String INDIRECT_X_PATTERN = "(\\([a-zA-Z_][0-9a-zA-Z_]+,)|(\\(\\$[0-fF][0-fF],)"; // must be followed by "X)"

    static boolean supportsAddressingMode(String mnemonic, AddressingMode mode) throws Exception {
        /*
        Determines whether the instruction in question supports the addressing mode specified
         */

        mnemonic = mnemonic.toUpperCase();
        Instruction toTest = null;
        for (Instruction cur: AllInstructions.INSTRUCTIONS) {
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

        while (idx < AllInstructions.INSTRUCTIONS.length && !found)
        {
            if (candidate.equals(AllInstructions.INSTRUCTIONS[idx].getMnemonic()))
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

        while (i < AllInstructions.INSTRUCTIONS.length && !found)
        {
            if (mnemonic.equals(AllInstructions.INSTRUCTIONS[i].getMnemonic()))
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
            return AllInstructions.INSTRUCTIONS[i].getOpcode(mode);
        }
        else
        {
            throw new Exception("Could not find mnemonic specified");
        }
    }

    static Instruction getInstruction(byte opcode) throws Exception {
        // Gets the Instruction object associated with the given opcode
        boolean found = false;
        int i = 0;
        while (i < AllInstructions.INSTRUCTIONS.length && !found) {
            byte[] addressingModes = AllInstructions.INSTRUCTIONS[i].getOpcodes();
            int j = 0;
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
            return AllInstructions.INSTRUCTIONS[i];
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
            // binary
            return switch (prefix) {
                case "#" -> (short) Integer.parseInt(numString, 10);
                case "#$", "$" -> (short) Integer.parseInt(numString, 16);
                case "#%", "%" -> (short) Integer.parseInt(numString, 2);
                default -> throw new Exception("Invalid numeric prefix");
            };
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

    static AddressingMode getAddressingMode(byte opcode) throws Exception {
        // Gets the addressing mode of the opcode
        Instruction in = InstructionParser.getInstruction(opcode);
        boolean found = false;
        int i = 0;
        while (i < in.getModes().size()) {
            if (in.getModes().get(i).getOpcode() == opcode) {
                found = true;
            }
            else {
                i++;
            }
        }

        if (found) {
            return in.getModes().get(i).getAddresingMode();
        }
        else {
            throw new Exception("No addressing mode for instruction");
        }
    }

    static byte[] parseInstruction(String[] data) throws Exception {
        // Given a series of strings containing instruction mnemonics, returns the bytecode that those strings entail

        AddressingMode mode = getAddressingMode(data);
        short value;

        byte opcode = getOpcode(data[0], mode);

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
