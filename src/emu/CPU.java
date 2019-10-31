package emu;

import assembler.Status;
import emu_format.*;

public class CPU {
    /*
    The CPU for our emulator
     */

    // the size of our memory
    final private int RAM_SIZE = 0x10000;

    // default program start address is 0x8000, but may be modified by modifying the RESET vector
    final private int DEFAULT_ORG = 0x8000;

    // the NMI vector is located at 0xfffa (low) and 0xfffb (high)
    final short NMI_LOW = (byte)0xfa;
    final byte NMI_HIGH = (byte)0xfb;

    // the RESET vector is located at 0xfffc (low) and 0xfffd (high)
    // This will default to 0x8000, but can be adjusted in code with the ORG macro
    final private int RESET_LOW = 0xfffc;
    final private int RESET_HIGH = 0xfffd;

    // the IRQ vector is located at 0xfffe (low) and 0xffff (high)
    final short IRQ_LOW = (short)0xfffe;
    final short IRQ_HIGH = (short)0xffff;

    // the high byte of the stack pointer is hardwired to be 0x01
    final byte STACK_HIGH = (byte)0x01;

    // prg origin (program start address)
    private short org;

    // our ram - an array of bytes
    byte[] memory;

    // Debugger and runtime system variables
    boolean halted; // to tell us whether the CPU has halted
    boolean debugMode;  // whether we should run the CPU in debug mode
    Debugger debugger;  // the CPU debugger

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
    int pc;

    /*

    Methods

     */

    // Flag access functions

    byte getNegative()
    {
        return (byte)(this.status & Status.NEGATIVE);
    }

    byte getOverflow()
    {
         return (byte)(this.status & Status.OVERFLOW);
    }

    byte getDecimal()
    {
        return (byte)(this.status & Status.DECIMAL);
    }

    byte getInterrupt()
    {
        return (byte)(this.status & Status.INTERRUPT);
    }

    byte getZero()
    {
        return (byte)(this.status & Status.ZERO);
    }

    byte getCarry()
    {
        return (byte)(this.status & Status.CARRY);
    }

    /*

    Fetch Instructions

    These functions fetch instructions and operands, modifying the PC automatically

     */

    private byte fetchInstruction() {
        byte opcode = this.memory[this.pc];
        this.pc++;
        return opcode;
    }

    private byte fetchImmediateByte() {
        byte operand = this.memory[this.pc];
        this.pc++;
        return operand;
    }

    private byte fetchByteFromMemory(int address) throws Exception {
        return this.fetchByteFromMemory(address, 0);
    }

    private byte fetchByteFromMemory(int address, int index) throws Exception {
        // Get the value at this.memory[address + index]
        address &= 0xFFFF;
        if (address + index < RAM_SIZE) {
            return this.memory[address + index];
        } else {
            throw new Exception("Indexed beyond bounds of memory is illegal");
        }
    }

    private byte fetchIndirectX() {
        // Handle an indexed indirect ($c0, x) fetch
        // Looks at location $c0, x; obtains data and uses that (and the following byte) as the address

        int base = this.fetchImmediateByte() & 0xFF;   // fetch the address

        // pointer is at location address + index
        int address = (this.memory[base + (this.x & 0xFF)]) | (this.memory[base + (this.x & 0xFF) + 1] << 8);
        return this.memory[address];
    }

    private byte fetchIndirectY() {
        // Handle an indirect indexed ($c0), y fetch
        // Looks at location $c0; goes to that location + y; gets that value

        int pointer = this.fetchImmediateByte() & 0xFF;   // fetch the pointer
        int address = (this.memory[pointer] + (this.y & 0xFF)) & 0xFFFF;   // go to the address indicated by the pointer, offset by index
        return this.memory[address];    // get the value at that address
    }

    private short fetchImmediateShort() {
        byte lsb = this.memory[this.pc];
        this.pc++;
        byte msb = this.memory[this.pc];
        this.pc++;

        return (short)((msb << 8) | lsb);
    }

    // Code execution

    public void run() throws Exception {
        // runs a program in debug mode

        if (this.debugMode) {
            // when in debug mode, the CPU checks with the debugger before executing instructions
            while (!this.halted) {
                while (!this.debugger.isStopped() && !this.halted) {
                    this.step();

                    if (this.pc > 0xFFFF) {
                        this.pc = 0x00;
                        this.halted = true;
                    }
                }
            }
        } else {
            while (!this.halted) {
                this.step();

                if (this.pc > 0xFFFF) {
                    this.pc = 0x00;
                    this.halted = true;
                }
            }
        }
    }

    void step() throws Exception {
        // Steps the CPU once; executes a single instruction

        // fetch the opcode
        int opcode = this.fetchInstruction();
        byte operand;

        /*

        Instruction execution uses a switch statement to dispatch functions

         */
        switch (opcode)
        {
            // BRK
            case 0x00:
                this.halted = true;
                break;

            /*

            ADC
            Add with Carry

            Affects flags N, Z, C, V
            A + M + C   ->  A, C

            */

            // ADC: Immediate
            case 0x69:
                // len 2
                operand = this.fetchImmediateByte();
                this.add(operand);
                break;
            // ADC: Zero
            // ADC: Zero, X
            case 0x65:
            case 0x75:
                // len 2
                operand = this.fetchByteFromMemory(this.fetchImmediateByte(), (opcode == 0x65) ? 0 : this.x);
                this.add(operand);
                break;
            // ADC: Absolute
            case 0x6D:
                operand = this.fetchByteFromMemory(this.fetchImmediateShort());
                this.add(operand);
                break;
            // ADC: Absolute, X
            // ADC: Absolute, Y
            case 0x7D:
            case 0x79:
                operand = this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0x7D)? this.x : this.y);
                this.add(operand);
                break;
            // ADC: Indexed Indirect, X
            case 0x61:
                operand = this.fetchIndirectX();
                this.add(operand);
                break;
            // ADC: Indirect Indexed, Y
            case 0x71:
                operand = this.fetchIndirectY();
                this.add(operand);
                break;

            /*

            AND
            Bitwise AND with Accumulator

            Affects flags N, Z
            A & M   ->  A

             */

            // AND: Immediate
            case 0x29:
                operand = this.fetchImmediateByte();
                this.and(operand);
                break;
            // AND: Zero
            case 0x25:
                operand = this.fetchByteFromMemory(this.fetchImmediateByte());
                this.and(operand);
                break;
            // AND: Zero, X
            case 0x35:
                operand = this.fetchByteFromMemory(this.fetchImmediateByte(), this.x);
                this.and(operand);
                break;
            // AND: Absolute
            case 0x2D:
                operand = this.fetchByteFromMemory(this.fetchImmediateShort());
                this.and(operand);
                break;
            // AND: Absolute, X
            // AND: Absolute, Y
            case 0x3D:
            case 0x39:
                operand = this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0x3D) ? this.x : this.y);
                this.and(operand);
                break;
            // AND: Indexed Indirect, X
            case 0x21:
                this.and(this.fetchIndirectX());
                break;
            // AND: Indirect Indexed, Y
            case 0x31:
                this.and(this.fetchIndirectY());
                break;

            /*

            LDA
            Load Accumulator

            Loads the accumulator with some value.
            Affects flags N, Z

             */

            // LDA: Immediate
            case (byte)0xA9:
                // get the operand
                this.a = this.fetchImmediateByte();

                // set flags
                this.updateNZFlags(this.a);
                break;
            // LDA: Zero
            case (byte)0xA5:
                this.a = this.fetchByteFromMemory(this.fetchImmediateByte());
                this.updateNZFlags(this.a);
                break;
            // LDA: Zero, X
            case (byte)0xB5:
                this.a = this.fetchByteFromMemory(this.fetchImmediateByte(), this.x);
                this.updateNZFlags(this.a);
                break;
            // LDA: Absolute
            case (byte)0xAD:
                this.a = this.fetchByteFromMemory(this.fetchImmediateShort());
                this.updateNZFlags(this.a);
                break;
            // LDA: Absolute, X
            // LDA: Absolute, Y
            case (byte)0xBD:
            case(byte)0xB9:
                this.a = this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == (byte)0xBD) ? this.x: this.y);
                this.updateNZFlags(this.a);
                break;
            case (byte)0xA1:
                this.a = this.fetchIndirectX();
                this.updateNZFlags(this.a);
                break;
            case (byte)0xB1:
                this.a = this.fetchIndirectY();
                this.updateNZFlags(this.a);
                break;

            default:
                // if the instruction isn't in the list, it is illegal
                throw new Exception("Illegal instruction");
        }
    }

    // Update the STATUS register

    private void updateNZFlags(byte value) {
        if (value < 0) {
            this.status &= Status.ZERO;
            this.status |= Status.NEGATIVE;
        } else if (value == 0) {
            this.status &= ~Status.NEGATIVE;
            this.status |= Status.ZERO;
        }
    }

    /*

    Instruction implementation functions

    These are the functions that execute individual instructions

     */

    private void add(int operand) {
        // adds operand + carry + a and sets flags accordingly
        // todo: implement add function
    }

    private void and(int operand) {
        // performs logical and on a + memory and sets N and Z flags accordingly

        this.a &= operand;
        this.updateNZFlags(this.a);
    }

    // Constructors and setup methods

    public void reset() {
        this.status = (byte)0b00110000; // initialize status register
        this.s = (byte)0xff;    // stack register should be initialized to 0xff (grows downwards)
        int startAddress = (this.memory[RESET_HIGH] << 8 | this.memory[RESET_LOW]) & 0xFFFF; // obtain the reset address from the reset vector
        this.pc = startAddress;    // set the start address
        this.halted = false;    // to allow execution to begin, make sure the halted flag is false
    }

    public CPU(boolean debug) {
        // default constructor; initializes the cpu with no program memory
        this.memory = new byte[RAM_SIZE];
        this.org = (short)DEFAULT_ORG;  // this should default to 0x8000, but can be modified by the program
        this.memory[RESET_LOW] = (byte)(this.org & 0xFF);
        this.memory[RESET_HIGH] = (byte)(this.org >> 8);
        this.debugMode = debug;
        this.debugger = new Debugger(this);
    }

    public CPU() {
        this(false);
    }

    public CPU(String emuFilename, boolean debug) throws Exception {
        this(debug);

        // initialize CPU memory using an emu file
        EmuFile emu = EmuFile.loadEmuFile(emuFilename);

        // loadEmuFile will return null if the file couldn't be loaded (though it will also throw an exception)
        if (emu != null) {
            // load the CPU memory as specified
            for (Bank segment: emu.getPrgBanks())
            {
                // copy the data from Bank.data into CPU memory starting at Bank.org
                int address = segment.getOrg() & 0xFFFF;
                byte[] data = segment.getData();
                for (int i = 0; i < data.length; i++, address++)
                {
                    this.memory[address] = data[i];
                }
            }

            // now that the memory has been loaded, use reset() to begin execution
            this.reset();
        } else {
            throw new Exception("Error reading .emu file; cannot initialize CPU");
        }
    }

    public CPU (String emuFilename) throws Exception {
        this(emuFilename, false);
    }
}
