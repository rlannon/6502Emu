package assembler;

import javafx.util.Pair;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Disassembler {
    /*

    A class that allows disassembly of machine code

     */

    private static int getInstructionLength(int addressingMode) throws Exception {
        // Gets the length of an instruction given an addressing mode
        switch (addressingMode){
            case AddressingMode.Single:
                return 1;
            case AddressingMode.Immediate:
            case AddressingMode.ZeroPage:
            case AddressingMode.ZeroPageX:
            case AddressingMode.ZeroPageY:
            case AddressingMode.Indirect:
            case AddressingMode.IndirectX:
            case AddressingMode.IndirectY:
            case AddressingMode.Relative:
                return 2;
            case AddressingMode.Absolute:
            case AddressingMode.AbsoluteX:
            case AddressingMode.AbsoluteY:
                return 3;
            default:
                throw new Exception("Invalid addressing mode");
        }
    }

    private static int writeInstruction(int index, byte[] hex, ArrayList<String> disassembly) throws Exception {
        // Disassembles an instruction and returns a string

        StringBuilder disAsmString = new StringBuilder();

        if (hex != null) {
            Pair<String, Integer> instructionData;
            int instructionLength;

            try {
                instructionData = InstructionParser.getMnemonic(hex[index]);
                instructionLength = getInstructionLength(instructionData.getValue());
            } catch (Exception e) {
                instructionData = new Pair<>("???", 1);
                instructionLength = 1;
            }
            disAsmString.append(String.format("$%04x:\t\t", (index) & 0xFFFF));

            // get the bytes for the instruction
            for (int i = 0; i < instructionLength; i++) {
                byte b = hex[index + i];
                disAsmString.append(String.format("%02x ", b));
            }
            disAsmString.append((instructionLength < 3) ? "\t\t" : "\t");
            disAsmString.append(instructionData.getKey());

            if (instructionData.getValue() == AddressingMode.Immediate) {
                disAsmString.append(String.format(" #$%02x", hex[index + 1]));
            } else if (instructionData.getValue() == AddressingMode.Relative) {
                disAsmString.append(String.format("  $%02x", hex[index + 1]));
            } else if (instructionData.getValue() != AddressingMode.IndirectX && instructionData.getValue() != AddressingMode.IndirectY){
                if (instructionData.getValue() == AddressingMode.ZeroPage) {
                    disAsmString.append(String.format(" $%02x", hex[index + 1]));
                } else if (instructionData.getValue() == AddressingMode.ZeroPageX) {
                    disAsmString.append(String.format(" $%02x, X", hex[index + 1]));
                } else if (instructionData.getValue() == AddressingMode.ZeroPageY) {
                    disAsmString.append(String.format(" $%02x, Y", hex[index + 1]));
                } else if (instructionData.getValue() == AddressingMode.Absolute) {
                    disAsmString.append(String.format(" $%02x%02x", hex[index + 2], hex[index + 1]));
                } else if (instructionData.getValue() == AddressingMode.AbsoluteX) {
                    disAsmString.append(String.format(" $%02x%02x, X", hex[index + 2], hex[index + 1]));
                } else {
                    disAsmString.append(String.format(" $%02x%02x, Y", hex[index + 2], hex[index + 1]));
                }
            } else if (instructionData.getValue() != AddressingMode.Single) {
                if (instructionData.getValue() == AddressingMode.IndirectX) {
                    disAsmString.append(String.format(" ($%02x, X)", hex[index + 1]));
                } else {
                    disAsmString.append(String.format(" ($%02x), Y", hex[index + 1]));
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
        disassembly.add("Address\t\tHexdump\t\tAssembly\n");

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
