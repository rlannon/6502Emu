package emu;

import assembler.*;

public class Main {
    public static void main(String[] args)
    {
        try {
            Assembler asm = new Assembler();
            asm.assemble("test.s");

            CPU cpu = new CPU("assembled.emu", true);
            cpu.run();
            System.out.println();
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.toString());
        } finally {
            System.out.println("Done");
        }
    }
}
