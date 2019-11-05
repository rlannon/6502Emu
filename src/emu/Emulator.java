package emu;

import assembler.Assembler;

import java.security.Key;
import java.util.Vector;
import java.awt.event.*;

public class Emulator implements KeyListener {
    private CPU cpu;    // the CPU we are running; automatically creates debugger
    private Assembler assemble; // the Assembler we are using
    Vector<Input> inputs;   // user inputs; these are configurable

    public void keyTyped(KeyEvent e) {
        System.out.println(e.getID() + " typed!");
        if (e.isActionKey()) {
            this.cpu.halted = true;
        }
    }

    public void keyPressed(KeyEvent e) {
        System.out.println(e.getID() + " pressed!");
        if (e.isActionKey()) {
            this.cpu.halted = true;
        }
    }

    public void keyReleased(KeyEvent e) {
        System.out.println(e.getID() + " released!");
    }

    void assemble(String filename) {
        // Assemble a file

        try {
            this.assemble.assemble(filename);
        } catch (Exception e) {
            System.out.println("Error: " + e.toString());
        }
    }

    void addBinary(String filename) {
        try {
            this.cpu.loadBinFile(filename);
        } catch (Exception e) {
            System.out.println("The system encountered an error when loading the bin file: " + e.toString());
        }
    }

    void assembleAndAdd(String filename) {
        this.assemble(filename);    // assemble the file
        this.addBinary("assembled.emu");    // name for assembled file
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
                while (!this.cpu.debugger.isStopped() && !this.cpu.halted) {
                    this.cpu.debugger.step();
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

    void coreDump() throws Exception {
        if (this.cpu.debugMode) {
            this.cpu.debugger.generateCoreDump();
        } else {
            throw new Exception("Cannot generate core dump when debug mode is disabled");
        }
    }

    Emulator() {
        this.cpu = new CPU();
        this.assemble = new Assembler();
        this.inputs = new Vector<>();
    }
}
