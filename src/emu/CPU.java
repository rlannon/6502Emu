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

    Status Register Methods

     */

    // Flag access

    boolean isSet(byte flag) {
        return (this.status & flag) == flag;
    }

    // Update the STATUS register

    private void updateNZFlags(byte value) {
        if (value < 0) {
            this.clearFlag(Status.ZERO);
            this.setFlag(Status.NEGATIVE);
        } else if (value == 0) {
            this.clearFlag(Status.NEGATIVE);
            this.setFlag(Status.ZERO);
        }
    }

    private void setFlag(byte flag) {
        this.status |= flag;
    }

    private void clearFlag(byte flag) {
        this.status &= ~(flag);
    }

    private void clearFlags() {
        // Clears all processor flags except D and I
        this.status &= ~(Status.NEGATIVE | Status.OVERFLOW | Status.ZERO | Status.CARRY);
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

        int pointer = this.fetchImmediateByte() & 0xFF;   // fetch the pointer value
        pointer += this.x & 0xFF;   // add the offset before fetching the actual address

        int addressLow = this.memory[pointer] & 0xFF;
        int addressHigh = this.memory[pointer + 1] & 0xFF;
        int address = ((addressHigh << 8) | addressLow) & 0xFFFF;
        return this.memory[address];
    }

    private byte fetchIndirectY() {
        // Handle an indirect indexed ($c0), y fetch
        // Looks at location $c0; goes to that location + y; gets that value

        int pointer = this.fetchImmediateByte() & 0xFF;   // fetch the pointer
        int addressLow = (this.memory[pointer]) & 0xFF;
        int addressHigh = (this.memory[pointer + 1]) & 0xFF;
        int address = ((addressHigh << 8) | addressLow) & 0xFFFF;
        address += this.y & 0xFF;
        return this.memory[address];    // get the value at that address
    }

    private short fetchImmediateShort() {
        byte lsb = this.memory[this.pc];
        this.pc++;
        byte msb = this.memory[this.pc];
        this.pc++;

        return (short)((msb << 8) | (lsb & 0xFF));
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
        this.debugger.addUsedPageByAddress(address);
    }

    // todo: implement store instruction functionality
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
        switch (opcode) {
            // BRK
            case 0x00:
                this.signal(Signal.BRK);
                break;

            // NOP
            case 0xEA:
                // do nothing
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

            /*

            BIT
            Test bits. This sets the Z flag as if the value at the address were ANDed with A
            The N and V flags are set to match bits 7 and 6, respectively, of the value stored at the tested address
            However, it does not modify the A register

            Affects flags N, V, Z

             */

            // BIT: Zero Page
            case 0x24:
            // BIT: Absolute
            case 0x2C:
                // get the value, update Z flag
                if (opcode == 0x24) {
                    address = (int) this.fetchImmediateByte() & 0xFF;
                } else {
                    address = (int) this.fetchImmediateShort() & 0xFFFF;
                }

                if ((this.a & this.memory[address]) == 0) {
                    this.setFlag(Status.ZERO);
                } else {
                    this.clearFlag(Status.ZERO);
                }

                // update the flags
                if ((address & 0b10000000) != 0) this.setFlag(Status.NEGATIVE);
                if ((address & 0b01000000) != 0) this.setFlag(Status.OVERFLOW);
                break;

            /*

            Branch Instructions
                BPL -   Branch on Plus (N flag clear)
                BMI -   Branch on Minus (N flag set)
                BVC -   Branch on Overflow clear
                BVS -   Branch on Overflow set
                BCC -   Branch on Carry clear
                BCS -   Branch on Carry set
                BNE -   Branch on not equal (Z flag clear)
                BEQ -   Branch on equal (Z flag set)

            All branch instructions use relative addressing mode

             */

            // BPL
            case 0x10:
                this.branchIfClear(Status.NEGATIVE);
                break;
            // BMI
            case 0x30:
                this.branchIfSet(Status.NEGATIVE);
                break;
            // BVC
            case 0x50:
                this.branchIfClear(Status.OVERFLOW);
                break;
            // BVS
            case 0x70:
                this.branchIfSet(Status.OVERFLOW);
                break;
            // BCC
            case 0x90:
                this.branchIfClear(Status.CARRY);
                break;
            // BCS
            case 0xB0:
                this.branchIfSet(Status.CARRY);
                break;
            // BNE
            case 0xD0:
                this.branchIfClear(Status.ZERO);
                break;
            // BEQ
            case 0xF0:
                this.branchIfSet(Status.ZERO);
                break;

            /*

            CMP
            Compare Accumulator

            Compares the accumulator to some value. It sets flags as if a subtraction had been carried out.
            If:
                A != Val    ->  Clear C, Z; set N
                A == Val    ->  Set Z flag
                A >= Val    ->  Set Carry
                A >= $80    ->  Set N flag
            Affects N, Z, C flags

            Note that these comparisons do not treat

             */

            // CMP: Immediate
            case 0xC9:
                this.compare(this.a, this.fetchImmediateByte());
                break;
            // CMP: Zero Page
            case 0xC5:
                this.compare(this.a, this.fetchByteFromMemory(this.fetchImmediateByte() & 0xFF));
                break;
            // CMP: ZP, X
            case 0xD5:
                this.compare(this.a, this.fetchByteFromMemory(this.fetchImmediateByte() & 0xFF, this.x));
                break;
            // CMP: Absolute
            case 0xCD:
                this.compare(this.a, this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF));
                break;
            // CMP: Absolute, X
            case 0xDD:
                this.compare(this.a, this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF, this.x));
                break;
            // CMP: Absolute, Y
            case 0xD9:
                this.compare(this.a, this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF, this.y));
                break;
            // Indirect, X
            case 0xC1:
                this.compare(this.a, this.fetchIndirectX());
                break;
            // Indirect, Y
            case 0xC2:
                this.compare(this.a, this.fetchIndirectY());
                break;

            /*

            CPX
            Compare X Register

            Compares the X register to some value.
            Sets flags identically to the CMP instruction.

             */

            // CPX: Immediate
            case 0xE0:
                this.compare(this.x, this.fetchImmediateByte());
                break;
            // CPX: Zero Page
            case 0xE4:
                this.compare(this.x, this.fetchByteFromMemory(this.fetchImmediateByte() & 0xFF));
                break;
            // CPX: Absolute
            case 0xEC:
                this.compare(this.x, this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF));
                break;

            /*

            CPY
            Compare Y Register

            Compares the Y register to some value.
            Sets flags identically to the CMP instruction.

             */

            // CPY: Immediate
            case 0xC0:
                this.compare(this.y, this.fetchImmediateByte());
                break;
            // CPY: Zero Page
            case 0xC4:
                this.compare(this.y, this.fetchByteFromMemory(this.fetchImmediateByte() & 0xFF));
                break;
            // CPY: Absolute
            case 0xCC:
                this.compare(this.y, this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF));
                break;

            /*

            DEC
            Decrement memory

            Affects N and Z flags, depending on whether the result is negative or 0

             */

            // DEC: Zero Page
            case 0xC6:
            // DEC: Zero Page, X
            case 0xD6:
                address = (this.fetchImmediateByte() & 0xFF) + ((opcode == 0xC6) ? 0 : this.x);
                this.memory[address]--;
                this.updateNZFlags(this.memory[address]);
                break;
            // DEC: Absolute
            case 0xCE:
            // DEC: Absolute, X
            case 0xDE:
                address = (this.fetchImmediateShort() & 0xFFFF) + ((opcode == 0xCE) ? 0 : this.x);
                this.memory[address]--;
                this.updateNZFlags(this.memory[address]);
                break;

            /*

            EOR
            Bitwise XOR

            Performs XOR on accumulator and another value, storing the result in the accumulator

            Affects N and Z flags, depending on the value of the result

             */

            // EOR: Immediate
            case 0x49:
                this.xor(this.fetchImmediateByte());
                break;
            // EOR: Zero Page
            // EOR: Zero Page, X
            case 0x45:
            case 0x55:
                this.xor(this.fetchByteFromMemory(this.fetchImmediateByte(), (opcode == 0x45) ? 0 : this.x));
                break;
            // EOR: Absolute
            case 0x4D:
                this.xor(this.fetchByteFromMemory(this.fetchImmediateShort()));
                break;
            // EOR: Absolute, X
            // EOR: Absolute, Y
            case 0x5D:
            case 0x59:
                this.xor(this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0x5D) ? this.x : this.y));
                break;
            // EOR: Indirect X
            case 0x41:
                this.xor(this.fetchIndirectX());
                break;
            // EOR: Indirect Y
            case 0x51:
                this.xor(this.fetchIndirectY());
                break;

            /*

            Flag / Status Instructions
            These affect flags where noted

            NB: The CLD and SED instructions are enabled, but the decimal flag is not implemented on this processor

             */

            // CLC
            case 0x18:
                this.clearFlag(Status.CARRY);
                break;
            // SEC
            case 0x38:
                this.setFlag(Status.CARRY);
                break;
            // CLI
            case 0x58:
                this.clearFlag(Status.INTERRUPT_DISABLE);
                break;
            // SEI
            case 0x78:
                this.setFlag(Status.INTERRUPT_DISABLE);
                break;
            // CLV
            case 0xB8:
                this.clearFlag(Status.OVERFLOW);
                break;
            // CLD
            case 0xD8:
                this.clearFlag(Status.DECIMAL);
                break;
            // SED
            case 0xF8:
                this.setFlag(Status.DECIMAL);
                break;

            /*

            INC
            Increment memory

            Affects N and Z flags, depending on the result

             */

            // INC: Zero Page
            case 0xE6:
            // INC: ZP, X
            case 0xF6:
                address = (this.fetchImmediateByte() & 0xFF) + ((opcode == 0xE6) ? 0 : this.x);
                this.memory[address]++;
                this.updateNZFlags(this.memory[address]);
                break;
            // INC: Absolute
            case 0xEE:
            // INC: Absolute, X
            case 0xFE:
                address = (this.fetchImmediateShort() & 0xFFFF) + ((opcode == 0xFE) ? 0 : this.x);
                this.memory[address]++;
                this.updateNZFlags(this.memory[address]);
                break;

            /*

            JMP
            Jump to memory address

            Transfers program execution to the following address OR the location contained within the following address
            Note: Indirect jump should never use a vector beginning on the last byte of a page
            For example:
                jmp ($30ff)
            will not fetch the address from $3100, $30ff, but rather from $3000, $30ff

            Affects no flags

             */

            // JMP: Absolute
            case 0x4C:
                address = this.fetchImmediateShort() & 0xFFFF;
                this.pc = address;
                break;
            // JMP: Indirect
            case 0x6C:
                int pointer = this.fetchImmediateShort() & 0xFFFF;
                int ptrLow = pointer & 0xFF;
                int ptrHigh = (pointer & 0xFF00) >> 8;

                int addressLow = this.memory[(ptrHigh << 8) | ptrLow];
                int addressHigh = this.memory[(ptrHigh << 8) | ((ptrLow + 1) & 0xFF)];

                this.pc = ((addressHigh & 0xFF) << 8) | (addressLow & 0xFF);
                break;

            /*

            JSR
            Jump to Subroutine

            Pushes (return address - 1) onto the stack (push high, then push low) and jumps to the address given

             */

            case 0x20:
                // Fetch the address to jump to and the return address
                address = this.fetchImmediateShort() & 0xFFFF;
                int returnAddress = this.pc - 1;
                byte returnLow = (byte)(returnAddress & 0xFF);
                byte returnHigh = (byte)((returnAddress >> 8) & 0xFF);

                // write the return address to the stack
                this.pushToStack(returnHigh);
                this.pushToStack(returnLow);

                // transfer control to 'address'
                this.pc = address;
                break;

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
            // LDA: Indirect X
            case 0xA1:
                this.a = this.fetchIndirectX();
                this.updateNZFlags(this.a);
                break;
            // LDA: Indirect Y
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

            /*

            LSR
            Logical shift right

            Shifts some memory (or A) to the right one position.
            0 is shifted into bit 7 and the original bit zero is shifted in to the carry flag
            Since 0 is shifted into bit 7, this will always clear the N flag

            Note this instruction can use the accumulator addressing mode, like 'lsr a'

            Affects flags N, Z, C

             */

            // LSR: Accumulator
            case 0x4A:
                boolean carry = (this.a & 0x01) == 1;
                this.a >>= 1;   // shift right by one bit
                this.a &= 0x7F; // ensure bit 7 is 0

                // Set/clear the C flag depending on bit
                if (carry)
                    this.setFlag(Status.CARRY);
                else
                    this.clearFlag(Status.CARRY);

                // clear the N flag
                this.clearFlag(Status.NEGATIVE);

                // set or clear the Z flag
                if (this.a == 0)
                    this.setFlag(Status.ZERO);
                else
                    this.clearFlag(Status.ZERO);
                break;
            // LSR: Zero Page
            // LSR: Zero Page, X
            case 0x46:
            case 0x56:
                address = ((this.fetchImmediateByte() & 0xFF) + ((opcode == 0x46) ? 0 : this.x)) & 0xFF;
                this.shiftRight(address);
                break;
            // LSR: Absolute
            // LSR: Absolute, X
            case 0x4E:
            case 0x5E:
                address = ((this.fetchImmediateShort() & 0xFFFF) + ((opcode == 0x4E) ? 0 : this.x)) & 0xFFFF;
                this.shiftRight(address);
                break;

            /*

            ORA
            Bitwise OR with Accumulator

            Operates like EOR or AND, performing bit logic with the accumulator and some value.

            Affects flags N, Z based on the result

             */

            // ORA: Immediate
            case 0x09:
                this.or(this.fetchImmediateByte() & 0xFF);
                break;
            // ORA: ZP
            // ORA: ZP, X
            case 0x05:
            case 0x15:
                this.or(this.fetchByteFromMemory((this.fetchImmediateByte() & 0xFF), ((opcode == 0x05) ? 0 : this.x)) & 0xFF);
                break;
            // ORA: Absolute
            case 0x0D:
                this.or(this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF));
                break;
            // ORA: Absolute, X
            // ORA: Absolute, Y
            case 0x1D:
            case 0x19:
                this.or(this.fetchByteFromMemory(this.fetchImmediateShort() & 0xFFFF, (opcode == 0x1D) ? this.x : this.y));
                break;
            // ORA: Indirect, X
            case 0x01:
                this.or(this.fetchIndirectX());
                break;
            // ORA: Indirect, Y
            case 0x11:
                this.or(this.fetchIndirectY());
                break;

            /*

            Register Instructions

            These instructions are all implied mode

             */

            // TAX
            case 0xAA:
                this.x = this.a;
                break;
            // TXA
            case 0x8A:
                this.a = this.x;
                break;
            // DEX
            case 0xCA:
                this.x--;
                break;
            // INX
            case 0xE8:
                this.x++;
                break;
            // TAY
            case 0xA8:
                this.y = this.a;
                break;
            // TYA
            case 0x98:
                this.a = this.y;
                break;
            // DEY
            case 0x88:
                this.y--;
                break;
            // INY
            case 0xC8:
                this.y++;
                break;

            /*

            ROR
            Rotate Right

            Shifts all bits right one position. The Carry is shifted into bit 7 and the original bit 0 is shifted into Carry

            Affects flags N, Z, C

             */

            // ROR: Accumulator
            case 0x6A:
                boolean b0 = (this.a &= 0x01) != 0;
                this.a >>= 1;

                if (this.isSet(Status.CARRY))
                    this.a |= 0x80;

                if (b0)
                    this.setFlag(Status.CARRY);
                else
                    this.clearFlag(Status.CARRY);
                break;
            // ROR: ZP
            // ROR: ZP, X
            case 0x66:
            case 0x76:
                address = (this.fetchImmediateByte() + ((opcode == 0x66) ? 0 : this.x & 0xFF)) & 0xFF;
                this.rotateRight(address);
                break;
            // ROR: Absolute
            // ROR: Absolute, X
            case 0x6E:
            case 0x7E:
                address = (this.fetchImmediateShort() + ((opcode == 0x6E) ? 0 : this.x & 0xFF)) &  0xFFFF;
                this.rotateRight(address);
                break;

            /*

            ROL
            Rotate Left

            Shifts all bits left

             */

            // ROL: Accumulator
            case 0x2A:
                b7 = (this.a &= 0x80) != 0;
                this.a <<= 1;

                if (this.isSet(Status.CARRY))
                    this.a |= 0x01;

                if (b7)
                    this.setFlag(Status.CARRY);
                else
                    this.clearFlag(Status.CARRY);
                break;
            // ROL: Zero Page
            // ROL: Zero Page, X
            case 0x26:
            case 0x36:
                address = (this.fetchImmediateByte() + ((opcode == 0x26) ? 0 : this.x & 0xFF)) & 0xFF;
                this.rotateLeft(address);
                break;
            // ROL: Absolute
            // ROL: Absolute, X
            case 0x2E:
            case 0x3E:
                address = (this.fetchImmediateShort() + ((opcode == 0x2E) ? 0 : this.x & 0xFF)) &  0xFFFF;
                this.rotateLeft(address);
                break;

            /*

            RTI
            Return from Interrupt

            RTI retrieves the processor status flags and the PC from the stack in that order
            Unlike RTS, the return address on the stack is the *actual address*, NOT (address - 1)

            Affects all flags

             */

            case 0x40:
                // get status
                this.status = this.pullFromStack();

                // get pc; remember we always push high, low
                returnLow = this.pullFromStack();
                returnHigh = this.pullFromStack();
                this.pc = ((returnHigh << 8) & 0xFF00) | (returnLow & 0xFF);
                break;

            /*

            RTS
            Return from subroutine

            Pulls the low byte, then the high byte, of the return address - 1.
            It then adds 1 to get the proper return address.

             */

            case 0x60:
                // fetch the low, then high bytes of the return address
                returnLow = this.pullFromStack();
                returnHigh = this.pullFromStack();

                // todo: determine whether jumping to a subroutine with a return address of $6000 would push $FF, $60 or $FF, $5F

                // pack them into the pc
                this.pc = ((returnHigh << 8) & 0xFF00) | (returnLow & 0xFF);

                // add one to ensure we return to the right place
                this.pc += 1;
                break;

            /*

            SBC
            Subtract with carry

            To subtract, set the carry before the operation. If the bit is cleared, it indicates a borrow occurred.

            Affects flags N, V, Z, C

             */

            // SBC: Immediate
            case 0xE9:
                this.subtract(this.fetchImmediateByte());
                break;
            // SBC: ZP
            // SBC: ZP, X
            case 0xE5:
            case 0xF5:
                this.subtract(this.fetchByteFromMemory(this.fetchImmediateByte(), (opcode == 0xE5) ? 0 : this.x));
                break;
            // SBC: Absolute
            case 0xED:
                this.subtract(this.fetchByteFromMemory(this.fetchImmediateShort()));
                break;
            // SBC: Absolute, X
            // SBC: Absolute, Y
            case 0xFD:
            case 0xF9:
                this.subtract(this.fetchByteFromMemory(this.fetchImmediateShort(), (opcode == 0xFD) ? this.x: this.y));
                break;
            // SBC: Indirect X
            case 0xE1:
                this.subtract(this.fetchIndirectX());
                break;
            // SBC: Indirect Y
            case 0xF1:
                this.subtract(this.fetchIndirectY());
                break;

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
            // STA: Indirect X
            case 0x81:
                // Handle an indexed indirect ($c0, x) fetch
                // Looks at location $c0, x; obtains data and uses that (and the following byte) as the address
                pointer = this.fetchImmediateByte() & 0xFF;   // fetch the address
                pointer += this.x & 0xFF;

                addressLow = this.memory[pointer] & 0xFF;
                addressHigh = this.memory[pointer + 1] & 0xFF;
                address = ((addressHigh << 8) | addressLow) & 0xFFFF;

                // pointer is at location base + index
                this.memory[address] = this.a;
                break;
            // STA: Indirect Y
            case 0x91:
                // Handle an indirect indexed ($c0), y fetch
                // Looks at location $c0; goes to that location + y; gets that value

                pointer = this.fetchImmediateByte() & 0xFF;   // fetch the pointer
                addressLow = this.memory[pointer] & 0xFF;
                addressHigh = this.memory[pointer + 1] & 0xFF;
                address = ((addressHigh << 8) | addressLow) & 0xFFFF;
                address += this.y & 0xFF;
                this.memory[address] = this.a;
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
                this.pushToStack(this.a);
                break;
            // PLA
            case 0x68:
                this.a = this.pullFromStack();
                break;
            // PHP
            case 0x08:
                this.pushToStack(this.status);
                break;
            // PLP
            case 0x28:
                this.status = this.pullFromStack();
                break;

            /*

            STX
            Store X register at a location in memory

            Affects no flags

             */

            // STX: ZP
            case 0x86:
            // STX: ZP, Y
            case 0x96:
                address = (int)this.fetchImmediateByte() & 0xFF;
                this.storeInMemory(this.x, address, (opcode == 0x86) ? 0 : this.y);
                break;
            // STX: Absolute
            case 0x8e:
                address = (int)this.fetchImmediateShort() & 0xFFFF;
                this.storeInMemory(this.x, address);
                break;

            /*

            STY
            Store Y register at a location in memory

            Affects no flags

             */

            // STY: ZP
            case 0x84:
            // STY: ZP, X
            case 0x94:
                address = (int)this.fetchImmediateByte() & 0xFF;
                this.storeInMemory(this.y, address, (opcode == 0x84) ? 0 : this.x);
                break;
            // STY: Absolute
            case 0x8c:
                address = (int)this.fetchImmediateShort() & 0xFFFF;
                this.storeInMemory(this.y, address);
                break;

            // Invalid opcodes will fall through to here
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

        // first, ensure our operand is set correctly
        operand &= 0xFF;

        if (((this.a ^ operand) & 0x80) != 0)
            this.clearFlag(Status.OVERFLOW);
        else
            this.setFlag(Status.OVERFLOW);

        // add A + M, and if C is set, add 1
        int result = (this.a & 0xFF) + (operand & 0xFF) + (isSet(Status.CARRY) ? 1 : 0);

        // set the carry flag if necessary
        if (result > 0xFF) {
            this.setFlag(Status.CARRY);
            if (this.isSet(Status.OVERFLOW) && result >= 0x180)
                this.clearFlag(Status.OVERFLOW);
        } else {
            this.clearFlag(Status.CARRY);
            if (this.isSet(Status.OVERFLOW) && result < 0x80)
                this.clearFlag(Status.OVERFLOW);
        }

        // set a to the result
        this.a = (byte)(result & 0xFF);
        this.updateNZFlags(this.a);
    }

    private void subtract(int operand) {
        // subtracts carry + a - operand and sets flags accordingly

        if ((((this.a) ^ operand) & 0x80) != 0) {
            this.setFlag(Status.OVERFLOW);
        } else {
            this.clearFlag(Status.OVERFLOW);
        }

        int result = 0xFF + (this.a & 0xFF) - operand + ((this.isSet(Status.CARRY)? 1 : 0));
        if (result < 0x100) {
            this.clearFlag(Status.CARRY);
            if (this.isSet(Status.OVERFLOW) && result < 0x80)
                this.clearFlag(Status.OVERFLOW);
        } else {
            this.setFlag(Status.CARRY);
            if (this.isSet(Status.OVERFLOW) && result >= 0x180)
                this.clearFlag(Status.OVERFLOW);
        }

        this.a = (byte)(result & 0xFF);
        this.updateNZFlags(this.a);
    }

    private void and(int operand) {
        // performs logical and on a + operand and sets N and Z flags accordingly

        operand &= 0xFF;
        this.a &= operand;
        this.updateNZFlags(this.a);
    }

    private void or(int operand) {
        // performs logical and on a + operand and sets N and Z flags accordingly

        operand &= 0xFF;
        this.a |= operand;
        this.updateNZFlags(this.a);
    }

    private void xor(int operand) {
        // performs a logical xor on a + operand and sets N and Z flags accordingly

        operand &= 0xFF;
        this.a ^= operand;
        this.updateNZFlags(this.a);
    }

    private void shiftLeft(int address) {
        // Shift the data at memory[address] left by one bit
        boolean b7 = (((this.memory[address] & 0xFF) >> 7) & 1) == 1;

        // perform the bitshift
        this.memory[address] <<= 1;

        // update the flags
        if (b7)
            this.setFlag(Status.CARRY); // shift bit 7 into carry
        else
            this.clearFlag(Status.CARRY);

        this.updateNZFlags(this.memory[address]);
    }

    private void shiftRight(int address) {
        // Performs a logical shift right on the data at the specified memory address

        boolean carry = (this.memory[address] & 0x01) == 1;
        this.memory[address] >>= 1;

        // Set/clear the C flag depending on bit
        if (carry)
            this.setFlag(Status.CARRY);
        else
            this.clearFlag(Status.CARRY);

        this.updateNZFlags(this.memory[address]);
    }

    private void rotateLeft(int address) {
        // Shifts all bits left one position, shifting carry into bit 0 and bit 7 into carry

        boolean carry = this.isSet(Status.CARRY);
        this.shiftLeft(address);
        if (carry) this.memory[address] |= 0x01;
    }

    private void rotateRight(int address) {
        // Identical to rotateLeft except that bits are shifted right

        boolean carry = this.isSet(Status.CARRY);
        this.shiftRight(address);
        if (carry) this.memory[address] |= 0x80;
    }

    private void compare(short register, short value) {
        /*

        Compares two values, setting flags accordingly

        This is how the flags are affected:
            R == Val    ->  Set Z flag
            R >= Val    ->  Set Carry
            R != Val    ->  Set N

         */

        register &= 0xFF;
        value &= 0xFF;

        this.clearFlags();

        if (register == value) {
            this.setFlag(Status.CARRY);
            this.setFlag(Status.ZERO);
        } else {
            // If R is greater than V, set C
            if (register > value) {
                this.setFlag(Status.CARRY);
            } else {
                // R must be < value if we are here
                this.setFlag(Status.NEGATIVE);
            }
        }
    }

    /*

    Branching functions
    These manipulate the program counter

     */

    private void branchIfSet(byte flag) {
        // Performs a branch if 'flag' is set
        byte offset = this.fetchImmediateByte();
        boolean branch = this.isSet(flag);
        if (branch) this.pc += offset;
    }

    private void branchIfClear(byte flag) {
        // Performs a branch if 'flag' is clear
        byte offset = this.fetchImmediateByte();
        boolean branch = !this.isSet(flag);
        if (branch) this.pc += offset;
    }

    /*

    Stack functions

     */

    private void pushToStack(byte data) {
        // pushes a byte onto the stack

        int address = ((STACK_HIGH << 8) | (this.sp & 0xFF)) & 0xFFFF;
        this.memory[address] = data;
        this.sp--;
    }

    private byte pullFromStack() {
        // pulls a byte from the stack and returns it

        this.sp++;
        int address = ((STACK_HIGH << 8) | (this.sp & 0xFF)) & 0xFFFF;
        return this.memory[address];
    }

    /*

    Signals and interrupts

     */

    private void reset() {
        this.status = (byte)0b00110000; // initialize status register
        this.sp = (byte)0xff;    // stack register should be initialized to 0xff (grows downwards)
        this.pc = (this.memory[RESET_HIGH] << 8 | this.memory[RESET_LOW]) & 0xFFFF; // obtain the reset address from the reset vector
        this.halted = false;    // to allow execution to begin, make sure the halted flag is false
        this.setFlag(Status.INTERRUPT_DISABLE); // a system reset should disable interrupts
    }

    private void interrupt(int vector) {
        /*

        Triggers an interrupt

        The process is as follows:
            - Push high byte of PC
            - Push low byte of PC
            - Push status
            - Obtains the address of the interrupt handler from 'vector'

         */

        // push high byte, low byte of the program counter
        this.pushToStack((byte)((this.pc >> 8) & 0xFF));
        this.pushToStack((byte)(this.pc & 0xFF));

        // push status
        this.pushToStack(this.status);

        // disable interrupts _after_ we push processor status
        this.setFlag(Status.INTERRUPT_DISABLE);

        // get the proper address from vector | (vector + 1) << 8; transfer control
        vector &= 0xFFFF;
        this.pc = ((this.memory[vector + 1] << 8) & 0xFF00) | (this.memory[vector] & 0xFF);
    }

    void signal(Signal signal) {
        // Sends a signal to the processor

        int vector;
        switch (signal) {
            case NMI:
                this.clearFlag(Status.B);
                vector = ((NMI_HIGH << 8) | NMI_LOW) & 0xFFFF;
                this.interrupt(vector);
                break;
            case RESET:
                this.reset();
                break;
            case IRQ:
                // If the IRQ disable flag is set, do nothing
                if (this.isSet(Status.INTERRUPT_DISABLE)) {
                    break;
                } else {
                    this.setFlag(Status.INTERRUPT_DISABLE);
                    this.clearFlag(Status.B);
                    vector = ((IRQ_HIGH << 8) | IRQ_LOW) & 0xFFFF;
                    this.interrupt(vector);
                }
                break;
            case BRK:
                // this emulator will use BRK as a "halt"
                this.setFlag(Status.B);
                this.halted = true;
                break;
        }
    }

    /*

    Constructors and setup methods

     */

    void loadBinFile(EmuFile emu) throws Exception {
        // initialize CPU memory using an emu file

        // loadEmuFile will return null if the file couldn't be loaded (though it will also throw an exception)
        if (emu != null) {
            // load the CPU memory as specified
            for (Bank segment: emu.getPrgBanks())
            {
                // copy the data from Bank.data into CPU memory starting at Bank.org
                int address = segment.getOrg() & 0xFFFF;
                byte[] data = segment.getData();

                // add the segment to the debugger
                this.debugger.addSegment(address, data.length);

                // set our debugger's pages used if we are in debug mode
                int pageIndex = (data.length / 256);
                int pageNumber = (address >> 8) & 0xFF;
                while (pageIndex >= 0) {
                    this.debugger.addUsedPage(pageNumber);
                    pageIndex--;
                    pageNumber++;
                }

                // copy in the program data
                for (int i = 0; i < data.length; i++, address++)
                {
                    this.memory[address] = data[i];
                }
            }
        } else {
            throw new Exception("Error reading .emu file; cannot initialize CPU");
        }
    }

    public CPU(boolean debug) {
        // default constructor; initializes the cpu with no program memory
        this.memory = new byte[RAM_SIZE];
        // program origin (program start address)
        short org = (short) DEFAULT_ORG;  // this should default to 0x8000, but can be modified by the program
        this.memory[RESET_LOW] = (byte)(org & 0xFF);
        this.memory[RESET_HIGH] = (byte)(org >> 8);
        this.debugMode = debug;
        this.debugger = new Debugger(this);
        this.halted = true;
    }

    public CPU() {
        this(false);
    }
}
