package emu;

import assembler.Disassembler;
import assembler.Assembler;
import emu_format.EmuFile;

import java.util.*;

public class Emulator {
    final int LATCH = 0x2000;   // the latch that tells us whether it's safe to copy memory
    final int NMI_REFRESH = 33;    // an NMI every 33 milliseconds (1/30 of a second)

    final private CPU cpu;    // the CPU we are running; automatically creates debugger
    public Debugger debugger;
    final private Assembler assembler; // the Assembler we are using
    final private HashMap<String, Input> inputs;   // user inputs; these are configurable

    private boolean debugMode;  // whether the emulator is running in debug mode

    public void assemble(String inputFilename, String outputFilename) throws Exception {
        // Assemble a file
        this.assembler.assemble(inputFilename, outputFilename);
    }

    public ArrayList<String> disassemble(int startAddress) throws Exception {
        /*
        disassembly
        Disassembles code in memory starting at the specified address

        Disassembles 1 page of data starting at startAddress, or until end of program memory.
        The output strings are individual lines formatted as follows:
            $<address>:   $<opcode> <operand>   <mnemonic> <operands>

        @param  startAddress    The address where we should start disassembly
        @return A string array containing the disassembly
         */

        ArrayList<String> disassembly;

        if (startAddress < 0 || startAddress > 0xFFFF) {
            throw new Exception("Start address out of range");
        } else {
            disassembly = Disassembler.disassemble(startAddress, this.getMemory());
        }

        return disassembly;
    }

    public void addBinary(String filename) throws Exception {
        EmuFile emu = EmuFile.loadEmuFile(filename);
        this.cpu.loadBinFile(emu);
        this.debugger.setDebugSymbols(emu.getDebugSymbols());
    }

    public void addInput(String character, int address, boolean triggersIRQ) throws Exception {
        /*

        addInput
        Adds a new input source to the emulator

        @param keyCode  The key code associated with this input
        @param address The address to which this input is mapped in memory
        @param triggersIRQ  Whether this input should trigger an IRQ or not (some inputs may be polled instead)

         */

        // todo: ensure multiple inputs aren't mapped to the same area of memory
        if (this.inputs.containsKey(character)) {
            throw new Exception("Input device already mapped");
        } else {
            this.inputs.put(character, new Input(character, address, triggersIRQ));
        }
    }

    public boolean hasInput(String character) {
        return this.inputs.containsKey(character);
    }

    public Input getInput(String character) {
        return this.inputs.get(character);
    }

    public HashMap<String, Input> getAllInputs() {
        return this.inputs;
    }

    public void removeInput(Input toRemove) {
        this.inputs.remove(toRemove.getMappedKeyCode());
    }

    // Run a program

    public void step() throws Exception {
        if (this.cpu.debugMode) {
            this.debugger.step();
        } else {
            this.cpu.step();
        }
    }

    public void reset() {
        // Resets the CPU
        this.cpu.signal(Signal.RESET);
    }

    public void terminate() {
        // Terminates program execution
        if (debugMode)
            this.debugger.terminate();
        else
            this.cpu.halted = true;
    }

    public void nmi() {
        // triggers a CPU NMI
        this.cpu.signal(Signal.NMI);
    }

    public void irq() {
        // triggers a CPU IRQ
        this.cpu.signal(Signal.IRQ);
    }

    public boolean isSet(byte flag) {
        return this.cpu.isSet(flag);
    }

    public void writeToMemory(int address, byte value) {
        /*
        Writes a byte to memory; to be used for input handling
        @param  address The address in memory we are writing to
        @param  value   The value we wish to write
         */

        this.cpu.memory[address] = value;
    }

    public void coreDump() throws Exception {
        if (this.cpu.debugMode) {
            this.cpu.debugger.generateCoreDump();
        } else {
            throw new Exception("Cannot generate core dump when debug mode is disabled");
        }
    }

    public void setDebugMode(boolean mode) {
        this.debugMode = mode;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public boolean isRunning() {
        return !this.cpu.halted;
    }

    public byte[] getMemory() {
        return this.cpu.memory;
    }

    public Emulator() {
        // create and reset our CPU
        this.cpu = new CPU();
        this.debugger = this.cpu.debugger;
        this.debugMode = false;
        this.reset();

        this.assembler = new Assembler();
        this.inputs = new HashMap<>();
    }
}
