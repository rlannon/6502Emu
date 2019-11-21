package emu;

import GUI.GUI;
import assembler.Assembler;
import emu_format.EmuFile;
import java.time.Duration;
import java.time.Instant;
import java.util.Vector;

public class Emulator {
    final int LATCH = 0x2000;   // the latch that tells us whether it's safe to copy memory
    final int NMI_REFRESH = 33;    // an NMI every 33 milliseconds (1/30 of a second)

    private GUI gui;

    private CPU cpu;    // the CPU we are running; automatically creates debugger
    public Debugger debugger;
    private Assembler assemble; // the Assembler we are using
    private Vector<Input> inputs;   // user inputs; these are configurable

    private boolean debugMode;  // whether the emulator is running in debug mode

    public void assemble(String inputFilename, String outputFilename) throws Exception {
        // Assemble a file
        this.assemble.assemble(inputFilename, outputFilename);
    }

    public void addBinary(String filename) throws Exception {
        EmuFile emu = EmuFile.loadEmuFile(filename);
        this.cpu.loadBinFile(emu);
        this.debugger.setDebugSymbols(emu.getDebugSymbols());
    }

    void setBreakpoint(int address) {
        this.cpu.debugger.setBreakpoint(address);
    }

    public void addInput(int keyCode, short address, boolean triggersIRQ) {
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

    public void step() throws Exception {
        if (this.cpu.debugMode) {
            this.debugger.step();
        } else {
            this.cpu.step();
        }
    }

    void run() throws Exception {
        this.run(false);
    }

    public void run(boolean outputEnabled) throws Exception {
        /*

        Runs a program
        @param  debug   Tells us whether we are running in debug mode

         */

        System.out.println("Running...");
        this.cpu.debugMode = true;  // set debug mode

        long timeSinceLastNMI;
        Instant lastNMI = Instant.now();    // some instants in time to ensure we have NMIs at the proper time

        // our DrawGraphics class will allow us to update the output graphics based on CPU memory
        // it creates a new thread so that our cpu can continue executing instructions while the screen is being updated
        //DrawGraphics gDrawer = new DrawGraphics("drawG  1", this.gc, this.cpu.memory);

        while (!this.cpu.halted) {
            // todo: handle user input

            // if we have enabled the output, we need to see if an update is necessary
            if (outputEnabled) {
                // have an NMI once every NMI_REFRESH seconds
                timeSinceLastNMI = Duration.between(lastNMI, Instant.now()).toMillis();

                // only trigger an NMI if the graphicsDrawer thread is not still alive
                if (timeSinceLastNMI >= NMI_REFRESH /*&& !gDrawer.isAlive()*/) {
                    this.cpu.signal(Signal.NMI);
                    lastNMI = Instant.now();
                } //else if (gDrawer.isAlive()) {
                    //gDrawer.join();
                //}

                // if the value at the latch is not 0, trigger a data copy from the buffer to the screen
                if (this.cpu.memory[LATCH] != 0) {
                    // reset the latch
                    this.cpu.memory[this.LATCH] = 0;

                    // update the graphics
                    //gDrawer.start();
                }
            }

            // step the CPU
            if (this.debugMode) {
                if (!this.cpu.debugger.isPaused()) {
                    this.cpu.debugger.step();
                } else {
                    if (this.cpu.debugger.breakpoints.containsKey(this.cpu.pc)) {
                        this.cpu.debugger.pause();
                    }
                }
            } else {
                this.cpu.step();
            }
        }

        // print some basic info if we are in debug mode
        if (this.debugMode) {
            System.out.println("Processor Info:");
            System.out.println("\tA: " + String.format("$%02x", this.cpu.a));
            System.out.println("\tX: " + String.format("$%02x", this.cpu.x));
            System.out.println("\tY: " + String.format("$%02x", this.cpu.y));
            System.out.println("\tSP: " + String.format("$%02x", this.cpu.sp));
            System.out.println("\tPC: " + String.format("$%04x", this.cpu.pc));
            System.out.println("\tOrigin: " + String.format("$%02x%02x", this.cpu.memory[0xfffd], this.cpu.memory[0xfffc]));
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

    public boolean canDraw() {
        return (this.cpu.memory[LATCH] != 0);
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

        this.assemble = new Assembler();
        this.inputs = new Vector<>();

        this.gui = null;
    }

    public Emulator(boolean debug) {
        this();
        this.debugMode = true;
    }

    public Emulator(GUI gui) {
        this();
        this.gui = gui;
    }
}
