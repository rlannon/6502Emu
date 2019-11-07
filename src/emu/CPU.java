package emu;

import assembler.Status;
import emu_format.*;

public class CPU {
    /*
    The CPU for our emulator
     */

    // the size of our memory
    final private static int RAM_SIZE = 0x10000;

    // default program start address is 0x8000, but may be modified by modifying the RESET vector
    final private static int DEFAULT_ORG = 0x8000;

    // the NMI vector is located at 0xfffa (low) and 0xfffb (high)
    final private static short NMI_LOW = (byte)0xfa;
    final private static byte NMI_HIGH = (byte)0xfb;

    // the RESET vector is located at 0xfffc (low) and 0xfffd (high)
    // This will default to 0x8000, but can be adjusted in code with the ORG macro
    final private static int RESET_LOW = 0xfffc;
    final private static int RESET_HIGH = 0xfffd;

    // the IRQ vector is located at 0xfffe (low) and 0xffff (high)
    final private static short IRQ_LOW = (short)0xfffe;
    final private static short IRQ_HIGH = (short)0xffff;

    // the high byte of the stack pointer is hardwired to be 0x01
    final private static byte STACK_HIGH = (byte)0x01;

    // program origin (program start address)
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
    byte sp;

    byte status;
    int pc;

    /*

    Methods

     */

    // Flag access

    boolean isSet(byte flag) {
        return (this.status & flag) == flag;
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

    private void setFlag(byte flag) {
        this.status |= flag;
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

    /*

    Store Instructions
    These instructions handle all of our interaction with memory when we want to store a value

     */

    private void storeInMemory(byte value, int address) {
        // Stores a byte at a memory address
        // Offset is 0
        this.storeInMemory(value, address, 0);
    }

    private void storeInMemory(byte value, int address, int offset) {
        // Stores a byte at a memory address with an offset
        this.memory[address + (offset & 0xFF)] = value;
    }

    // todo: implement store instruction funcionality
    // todo: track which pages have been touched if we are in debug mode

    // Program execution

    void step() throws Exception {
        // Steps the CPU once; executes a single instruction

        // fetch the opcode
        int opcode = this.fetchInstruction() & 0xFF;
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

            ASL
            Arithmetic Shift Left

            Shifts all bits left, moving 0 into bit 0 and bit 7 into the carry
            Affects flags N, Z, C

             */

            // ASL: Zero
            case 0x06:
                // get the address and the last bit
                int address = this.fetchImmediateByte() & 0xFF;
                this.shiftLeft(address);
                break;
            // ASL: Zero, X
            case 0x16:
                address = (this.fetchImmediateByte() & 0xFF) + (this.x & 0xFF);
                this.shiftLeft(address);
                break;
            // ASL: Absolute 0e
            case 0x0e:
                address = this.fetchImmediateShort() & 0xFFFF;
                this.shiftLeft(address);
                break;
            // ASL: Absolute, X 1e
            case 0x1e:
                address = (this.fetchImmediateShort() & 0xFFFF) + (this.x & 0xFF);
                this.shiftLeft(address);
                break;
            // ASL: Single (A) 0a
            case 0x0a:
                boolean b7 = (((this.a & 0xFF) >> 7) & 1) > 0;

                // perform the bitshift
                this.a <<= 1;

                // update the flags
                if (b7) this.setFlag(Status.CARRY);
                this.updateNZFlags(this.a);
                break;

            // todo: implement instructions between ASL and LDA

            /*

            LDA
            Load Accumulator

            Loads the accumulator with some value.
            Affects flags N, Z

             */

            // LDA: Immediate
            case 0xA9:
                // get the operand
                this.a = this.fetchImmediateByte();

                // set flags
                this.updateNZFlags(this.a);
                break;
            // LDA: Zero
            case 0xA5:
                this.a = this.fetchByteFromMemory(this.fetchImmediateByte());
                this.updateNZFlags(this.a);
                break;
            // LDA: Zero, X
            case 0xB5:
                this.a = this.fetchByteFromMemory(this.fetchImmediateByte(), this.x);
                this.updateNZFlags(this.a);
                break;
            // LDA: Absolute
            case 0xAD:
                this.a = this.fetchByteFromMemory(this.fetchImmediateShort());
                this.updateNZFlags(this.a);
                break;
            // LDA: Absolute, X
            // LDA: Absolute, Y
            case 0xBD:
            case 0xB9:
                this.a = this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0xBD) ? this.x: this.y);
                this.updateNZFlags(this.a);
                break;
            case 0xA1:
                this.a = this.fetchIndirectX();
                this.updateNZFlags(this.a);
                break;
            case 0xB1:
                this.a = this.fetchIndirectY();
                this.updateNZFlags(this.a);
                break;

            /*

            LDX
            Load X Register

            Loads some value into register X
            Affects flags N, Z

             */

            // LDX: Immediate
            case 0xa2:
                this.x = fetchImmediateByte();
                this.updateNZFlags(this.x);
                break;
            // LDX: Zero Page
            // LDX: Zero Page, Y
            case 0xa6:
            case 0xb6:
                this.x = this.fetchByteFromMemory(this.fetchImmediateByte(), (opcode == 0xb6) ? this.y : 0);
                this.updateNZFlags(this.x);
                break;
            // LDX: Absolute
            // LDX: Absolute, Y
            case 0xae:
            case 0xbe:
                this.x = this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0xbe) ? this.y : 0);
                this.updateNZFlags(this.x);
                break;

            /*

            LDY
            Load Y Register

            Loads some value into register Y
            Affects flags N, Z

            a0 a4 b4 ac bc
             */

            // LDY: Immediate
            case 0xa0:
                this.y = this.fetchImmediateByte();
                this.updateNZFlags(this.y);
                break;
            // LDY: Zero Page
            // LDY: Zero Page, X
            case 0xa4:
            case 0xb4:
                this.y = this.fetchByteFromMemory(this.fetchImmediateByte(), (opcode == 0xb4) ? this.x : 0);
                this.updateNZFlags(this.y);
                break;
            // LDY: Absolute
            // LDY: Absolute, X
            case 0xac:
            case 0xbc:
                this.y = this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0xbc) ? this.x : 0);
                this.updateNZFlags(this.y);
                break;

            // todo: some more instructions

            /*

            STA
            Store Accumulator

            Stores the accumulator at a given place in memory. Possible addressing modes are:
                ZP:     0x85
                ZP, X:  0x95
                ABS:    0x8d
                ABS, X: 0x9d
                ABS, Y: 0x99
                IndX:   0x81
                IndY:   0x91

            Affects no flags

             */

            // STA: ZP
            case 0x85:
                address = (int)this.fetchImmediateByte() & 0xFF;
                this.storeInMemory(this.a, address);
                break;
            // STA: ZP, X
            case 0x95:
                address = (int)this.fetchImmediateByte() & 0xFF;
                this.storeInMemory(this.a, address, this.x);
                break;
            // STA: ABS
            case 0x8d:
                address = (int)this.fetchImmediateShort() & 0xFFFF;
                this.storeInMemory(this.a, address);
                break;
            // STA: ABS, X
            // STA: ABS, Y
            case 0x9d:
            case 0x99:
                address = (int)this.fetchImmediateShort() & 0xFFFF;
                this.storeInMemory(this.a, address, (opcode == 0x9d) ? this.x : this.y);
                break;

            /*

            Stack Instructions
            Handle all operations with the stack

            We can:
                - Transfer SP to and from X
                - Push and pull (pop) A
                - Push and pull (pop) the processor status

            Note that push instructions write the data to memory[stack pointer], *then* decrement
            Likewise, pull instructions increment, *then* retrieve data from the stack

            Affect no flags

             */

            // TXS
            case 0x9a:
                this.sp = this.x;
                break;
            // TSX
            case 0xba:
                this.x = this.sp;
                break;
            // PHA
            case 0x48:
                int stackAddress = ((STACK_HIGH << 8) | this.sp) & 0xFFFF;
                this.memory[stackAddress] = this.a;
                this.sp--;
                break;
            // PLA
            case 0x68:
                this.sp++;
                stackAddress = ((STACK_HIGH << 8) | this.sp) & 0xFFFF;
                this.a = this.memory[stackAddress];
                break;
            // PHP
            case 0x08:
                stackAddress = ((STACK_HIGH << 8) | this.sp) & 0xFFFF;
                this.memory[stackAddress] = this.status;
                this.sp--;
                break;
            // PLP
            case 0x28:
                this.sp++;
                stackAddress = ((STACK_HIGH << 8) | this.sp) & 0xFFFF;
                this.status = this.memory[stackAddress];
                break;

            // todo: finish implementing instructions

            default:
                // if the instruction isn't in the list, it is illegal
                throw new Exception("Illegal instruction");
        }
    }

    /*

    Instruction implementation functions

    These are the functions that execute individual instructions

     */

    private void add(int operand) {
        // adds operand + carry + a and sets flags accordingly

        // add A + M, and if C is set, add 1
        int result = (this.a & 0xFF) + (operand & 0xFF) + (isSet(Status.CARRY) ? 1 : 0);

        // set the V flag if our result changed signs (went from < to > 127 or <= 128 to <)
        if ( ((this.a < 127) && (result > 127)) || ((this.a < 0) && ((byte)operand < 0)) ) {
            this.setFlag(Status.OVERFLOW);
        }

        // set the carry flag if necessary
        if (result > 0xFF) {
            this.setFlag(Status.CARRY);
        } else {
            this.updateNZFlags((byte)(result & 0xFF));
        }

        // set a to the result
        this.a = (byte)(result & 0xFF);
    }

    private void and(int operand) {
        // performs logical and on a + memory and sets N and Z flags accordingly

        this.a &= operand;
        this.updateNZFlags(this.a);
    }

    private void shiftLeft(int address) {
        // Shift the data at memory[address] left by one bit
        boolean b7 = (((this.memory[address] & 0xFF) >> 7) & 1) == 1;

        // perform the bitshift
        this.memory[address] <<= 1;

        // update the flags
        if (b7) this.setFlag(Status.CARRY); // shift bit 7 into carry
        this.updateNZFlags(this.memory[address]);
    }

    private void shiftRight(int address) {
        // Shift the data at memory[address] right by one bit
        boolean b0 = ((this.memory[address] & 0xFF) & 1) == 1;

        // perform the bitshift
        this.memory[address] >>= 1;

        // update the flags
        if (b0) this.setFlag(Status.CARRY); // shift bit 0 into carry
        this.updateNZFlags(this.memory[address]);
    }

    // Constructors and setup methods

    public void reset() {
        this.status = (byte)0b00110000; // initialize status register
        this.sp = (byte)0xff;    // stack register should be initialized to 0xff (grows downwards)
        int startAddress = (this.memory[RESET_HIGH] << 8 | this.memory[RESET_LOW]) & 0xFFFF; // obtain the reset address from the reset vector
        this.pc = startAddress;    // set the start address
        this.halted = false;    // to allow execution to begin, make sure the halted flag is false
    }

    public void loadBinFile(String emuFilename) throws Exception {
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

                // set our debugger's pages used if we are in debug mode
                int page = address >> 8;
                int pageIndex = (data.length / 256);
                while (pageIndex >= 0) {
                    this.debugger.pagesUsed[page] = true;
                    page++;
                    pageIndex--;
                }

                // copy in the program data
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
        this.loadBinFile(emuFilename);
    }

    public CPU (String emuFilename) throws Exception {
        this(emuFilename, false);
    }
}
