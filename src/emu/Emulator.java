package emu;

import GUI.GUI;
import assembler.Assembler;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Vector;

public class Emulator {
    final int LATCH = 0x2000;   // the latch that tells us whether it's safe to copy memory
    final int NMI_REFRESH = 33;    // an NMI every 33 milliseconds (1/30 of a second)

    private GUI gui;
    private GraphicsContext gc; // the graphics context for the screen (Canvas)

    private CPU cpu;    // the CPU we are running; automatically creates debugger
    private Assembler assemble; // the Assembler we are using
    private Vector<Input> inputs;   // user inputs; these are configurable

    public void assemble(String filename) throws Exception {
        // Assemble a file
        this.assemble.assemble(filename);
    }

    public void addBinary(String filename) throws Exception {
        this.cpu.loadBinFile(filename);
    }

    public void assembleAndAdd(String filename) throws Exception {
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
        this.run(true, false);
    }

    public void run(boolean debug, boolean outputEnabled) throws Exception {
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
            if (debug) {
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

    public void setGraphicsContext(GraphicsContext gc) {
        this.gc = gc;
    }

    public Emulator() {
        // create and reset our CPU
        this.cpu = new CPU();
        this.reset();

        this.assemble = new Assembler();
        this.inputs = new Vector<>();

        this.gui = null;
        this.gc = null;
    }

    public Emulator(GUI gui) {
        this();
        this.gui = gui;
    }

    public Emulator(GraphicsContext gc) {
        // create a new emulator with a graphics context specified
        this();
        this.gc = gc;
    }
}
