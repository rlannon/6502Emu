package emu;

import assembler.Assembler;
import java.util.Vector;

public class Emulator {
    private CPU cpu;    // the CPU we are running; automatically creates debugger
    private Assembler assemble; // the Assembler we are using
    private Vector<Input> inputs;   // user inputs; these are configurable

    void assemble(String filename) throws Exception {
        // Assemble a file
        this.assemble.assemble(filename);
    }

    void addBinary(String filename) throws Exception {
        this.cpu.loadBinFile(filename);
    }

    void assembleAndAdd(String filename) throws Exception {
        this.assemble(filename);    // assemble the file
        this.addBinary("assembled.emu");    // name for assembled file
    }

    void setBreakpoint(int address) {
        this.cpu.debugger.setBreakpoint(address);
    }

    void addInput(int keyCode, short address, boolean triggersIRQ) {
        /*

        addInput
        Adds a new input source to the emulator

        @param keyCode  The key code associated with this input
        @param address The address to which this input is mapped in memory
        @param triggersIRQ  Whether this input should trigger an IRQ or not (some inputs may be polled instead)

         */

        // todo: ensure multiple inputs aren't mapped to the same area of memory
        this.inputs.add(new Input(keyCode, address, triggersIRQ));
    }

    void run(boolean debug) throws Exception {
        /*

        Runs a program
        @param  debug   Tells us whether we are running in debug mode

         */
        System.out.println("Running...");
        this.cpu.debugMode = true;  // set debug mode
        while (!this.cpu.halted) {
            // todo: handle user input

            // if we are debugging, use cpu.debugger.step(); otherwise, use cpu.step()
            if (debug) {
                while (!this.cpu.debugger.isPaused() && !this.cpu.halted) {
                    // check to see if we have hit a breakpoint; if so, pause; else, step
                    if (this.cpu.debugger.breakpoints.containsKey(this.cpu.pc)) {
                        this.cpu.debugger.pause();
                    } else {
                        this.cpu.debugger.step();
                    }
                }
            } else {
                this.cpu.step();
            }
        }

        // print some basic info if we are in debug mode
        if (debug) {
            System.out.println("Processor Info:");
            System.out.println("\tA: " + String.format("$%02x", this.cpu.a));
            System.out.println("\tX: " + String.format("$%02x", this.cpu.x));
            System.out.println("\tY: " + String.format("$%02x", this.cpu.y));
            System.out.println("\tSP: " + String.format("$%02x", this.cpu.sp));
            System.out.println("\tPC: " + String.format("$%04x", this.cpu.pc));
            System.out.println("\tOrigin: " + String.format("$%02x%02x", this.cpu.memory[0xfffd], this.cpu.memory[0xfffc]));
        }
    }

    void reset() {
        // Resets the CPU
        this.cpu.signal(Signal.RESET);
    }

    void coreDump() throws Exception {
        if (this.cpu.debugMode) {
            this.cpu.debugger.generateCoreDump();
        } else {
            throw new Exception("Cannot generate core dump when debug mode is disabled");
        }
    }

    public Emulator() {
        // create and reset our CPU
        this.cpu = new CPU();
        this.reset();

        this.assemble = new Assembler();
        this.inputs = new Vector<>();
    }
}
