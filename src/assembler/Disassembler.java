package assembler;

import java.util.ArrayList;

public class Disassembler {
    /*

    A class that allows disassembly of machine code

     */

    private static int getInstructionLength(byte opcode) {
        /*

        Gets the length of an instruction; this will also function where the disassembly is unknown

        If the instruction is known, we can use the addressing mode to determine the instruction length.
            Otherwise, we will need a slightly more complex algorithm

        Luckily, the 6502 opcodes have a pretty consistent map of opcode endings to instruction length
        Each instruction can be grouped into blocks separated by 0x20 -- 0x00, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0, 0xE0;
            this is the row, and doesn't really matter for this calculation.
            However, the base will allow us to determine the column number.
        Each column in the matrix will determine the length of the instruction:
            + 0x00 -> may have a length of 1 or 2
            + 0x04 -> 2
            + 0x08 -> 2
            + 0x0C -> 3
            + 0x10 -> 2
            + 0x14 -> 2
            + 0x18 -> 3
            + 0x1C -> 3
        Depending on the instruction, the column values may be offset by 1, 2, or 3
        Also note that a few instructions don't really follow this --
            - The KIL instruction located in column +0x12
            - The NOPs in +0x1a
        These instructions are only 1 byte, so we will have a special check for them

         */

        int len = 0;

        // use InstructionParser.getAddressingMode; if the instruction is unknown, it will throw an exception
        try {
            AddressingMode mode = InstructionParser.getAddressingMode(opcode);
            switch(mode) {
                case Implied:
                    len = 1;
                    break;
                case ZeroPage:
                case ZeroPageX:
                case ZeroPageY:
                case Immediate:
                case Relative:
                case IndirectX:
                case IndirectY:
                    len = 2;
                    break;
                case Indirect:
                case Absolute:
                case AbsoluteX:
                case AbsoluteY:
                    len = 3;
                    break;
            }
        }
        catch (UnknownInstructionException e) {
            // The instruction is unknown
            // First, we must figure out the remainder when we divide by 0x20 to figure out the column
            int remainder = ((int) opcode & 0xFF) % 0x20;
            if (remainder == 0x12 || remainder == 0x1a) {
                len = 1;
            } else {
                // using integer division, we can get the column number
                int col_num = remainder / 0x04;
                int[] columns = {2, 2, 2, 3, 2, 2, 3, 3};   // follows the matrix we gave in the comments
                len = columns[col_num];
            }
        }

        return len;
    }

    private static int writeInstruction(int index, byte[] hex, ArrayList<String> disassembly) throws Exception {
        // Disassembles an instruction and returns a string

        StringBuilder disAsmString = new StringBuilder();

        if (hex != null) {
            Instruction instructionData;    // the information for this instruction, obtained from the opcode
            int instructionLength;
            boolean valid_instruction = true;

            try {
                instructionData = InstructionParser.getInstruction(hex[index]);
                instructionLength = getInstructionLength(hex[index]);
            } catch (Exception e) {
                // if an exception was thrown, it's not an official opcode
                valid_instruction = false;

                // try to get the instruction length; this can be determined by the opcode itself
                instructionLength = getInstructionLength(hex[index]);
                instructionData = new Instruction();   // just use a .byte directive for unknowns
            }
            disAsmString.append(String.format("$%04x:\t\t", (index) & 0xFFFF)); // write the address of the instruction

            // get the bytes for the instruction
            for (int i = 0; i < instructionLength; i++) {
                byte b = hex[index + i];
                disAsmString.append(String.format("%02x ", b));
            }
            disAsmString.append((instructionLength < 3) ? "\t\t" : "\t");   // for display formatting
            disAsmString.append(instructionData.getMnemonic());

            // if we had a valid instruction, formulate the syntax
            if (valid_instruction) {
                AddressingMode instructionMode = InstructionParser.getAddressingMode(hex[index]);
                if (instructionMode == AddressingMode.Immediate) {
                    disAsmString.append(String.format(" #$%02x", hex[index + 1]));
                } else if (instructionMode == AddressingMode.Relative) {
                    disAsmString.append(String.format("  $%02x", hex[index + 1]));
                }
                else if (instructionMode == AddressingMode.ZeroPage) {
                    disAsmString.append(String.format(" $%02x", hex[index + 1]));
                } else if (instructionMode == AddressingMode.ZeroPageX) {
                    disAsmString.append(String.format(" $%02x, X", hex[index + 1]));
                } else if (instructionMode == AddressingMode.ZeroPageY) {
                    disAsmString.append(String.format(" $%02x, Y", hex[index + 1]));
                } else if (instructionMode == AddressingMode.Absolute) {
                    disAsmString.append(String.format(" $%02x%02x", hex[index + 2], hex[index + 1]));
                } else if (instructionMode == AddressingMode.AbsoluteX) {
                    disAsmString.append(String.format(" $%02x%02x, X", hex[index + 2], hex[index + 1]));
                } else if (instructionMode == AddressingMode.AbsoluteY) {
                    disAsmString.append(String.format(" $%02x%02x, Y", hex[index + 2], hex[index + 1]));
                } else if (instructionMode == AddressingMode.IndirectX) {
                    disAsmString.append(String.format(" ($%02x, X)", hex[index + 1]));
                } else if (instructionMode == AddressingMode.IndirectY) {
                    disAsmString.append(String.format(" ($%02x), Y", hex[index + 1]));
                }
            }
            else {
                // if we didn't have a valid instruction, just add the bytes to the directive
                for (int i = 0; i < instructionLength; i++) {
                    disAsmString.append(String.format(" $%02x", hex[index + i]));
                }
            }

            disassembly.add(disAsmString.toString());
            return instructionLength;
        } else {
            throw new NullPointerException("No bytes to disassemble");
        }
    }

    public static ArrayList<String> disassemble(int begin, byte[] data) throws Exception {
        // Disassembles the data supplied, saving to a file "disassembly.txt"

        // create (or open) the file where we want to save our data
        ArrayList<String> disassembly = new ArrayList<>();

        // write a short header
        disassembly.add("Address\t\tHexdump\t\tDisassembly\n");

        // todo: fix index out of bounds exception when page at $ff00 is disassembled

        // perform the disassembly
        int index = 0;
        while ((index < 256) && ((index + begin) <= 0xFFFF)) {
            index += writeInstruction(begin + index, data, disassembly);
        }

        // finally, close the file
        return disassembly;
    }
}
