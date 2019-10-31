package emu;

import emu_format.Bank;

import java.util.Vector;

public class CPU {
    /*
    The CPU for our emulator
     */

    // the NMI vector is located at 0xfffa (low) and 0xfffb (high)
    final short NMI_LOW = (byte)0xfa;
    final byte NMI_HIGH = (byte)0xfb;

    // the RESET vector is located at 0xfffc (low) and 0xfffd (high)
    // This will default to 0x8000, but can be adjusted in code with the ORG macro
    final short RESET_LOW = (short)0xfffc;
    final short RESET_HIGH = (short)0xfffd;

    // the IRQ vector is located at 0xfffe (low) and 0xffff (high)
    final short IRQ_LOW = (short)0xfffe;
    final short IRQ_HIGH = (short)0xffff;

    // the high byte of the stack pointer is hardwired to be 0x01
    final byte STACK_HIGH = (byte)0x01;

    // our ram - an array of bytes
    byte[] memory;

    // prg origin (program start address)
    short org;

    /*

    REGISTERS

    The 6502 has six registers; 5 8-bit registers and one 16-bit program counter:
        - General-purpose:
            - A ->  Accumulator
        - Index:
            - X
            - Y
            - S ->  Stack pointer
        - System:
            - STATUS    ->  Processor Status
            - PC    ->  The program counter

        Note the STATUS register is organized as follows:
            N V - B     D I Z C
            | | | |     | | | |
            | | | |     | | | +- Carry
            | | | |     | | +--- Zero
            | | | |     | +----- Interrupt
            | | | |     +------- Decimal
            | | + +------------- No effect
            | +----------------- Overflow
            +------------------- Negative
        The register should be initialized to 00110000

     */

    byte a;

    byte x;
    byte y;
    byte s;

    byte status;
    short pc;

    private void reset()
    {
        this.pc = (RESET_HIGH << 8) | RESET_LOW;    // obtain the reset address from the reset vector
    }

    CPU()
    {
        // default constructor; initializes the cpu with no program memory
        this.org = (short)0x8000;  // this should default to 0x8000, but can be modified by the program
        this.status = (byte)0b00110000; // initialize status register
        this.s = (byte)0xff;    // stack register should be initialized to 0xff (grows downwards)

        // call the reset routine
        this.reset();
    }

    CPU(Vector<Bank> banks)
    {
        // initialize CPU memory using banks
        for (Bank bank: banks)
        {

        }
    }

    CPU(String emuFilename)
    {
        // initialize CPU memory using an emu file
    }
}
