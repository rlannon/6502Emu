package emu;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Hashtable;

public class Debugger {
    // The debugger for our CPU

    private CPU cpu;
    private boolean paused;   // whether we have stoped the CPU
    boolean[] pagesUsed;    // tracks which pages have been touched by the CPU
    Hashtable<Integer, Boolean> breakpoints;  // the breakpoints we have set

    /*

    Utility functions

     */

    void setBreakpoint(int address) {
        // Set a new breakpoint for the given address
        this.breakpoints.put(address, Boolean.FALSE);
    }

    void removeBreakpoint(int address) {
        // Remove the breakpoint at an address. If none exists, do nothing
        this.breakpoints.remove(address);
    }

    /*

    Execution functions
    These functions are used in the process of executing and debugging the program

    */

    public void pause() {
        // temporarily halts the CPU
        this.paused = true;
    }

    public void terminate() {
        // kills the process, setting this.cpu.halted
        this.cpu.halted = true;
    }

    public void resume() {
        // resumes CPU execution after a debug event has stopped it
        this.paused = false;
    }

    boolean isPaused() {
        // tells us whether the debug program has stopped CPU execution
        return this.paused;
    }

    boolean hasHalted() {
        // tells us whether the CPU program has terminated
        return this.cpu.halted;
    }

    public void step() throws Exception {
        // steps the CPU one time
        this.cpu.step();
    }

    /*

    Fetch CPU Data
    Because the CPU data is package-private, we need public functions in the debugger to access that data
    The CPU internals should only be accessible through the debugger

     */

    public void generateCoreDump() {
        // Generates a core dump for the cpu, saving it to 'core.bin'

        // Each page will start with { 0xFF, 0xFF, ADDR_HIGH, ADDR_LOW }
        final byte[] pageHeader = new byte[]{(byte)0xFF, (byte)0xFF};

        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("core.bin")));

            for (int page = 0; page < this.pagesUsed.length; page++) {
                // if that page was accessed, write it to the core dump
                if (this.pagesUsed[page]) {
                    // write the header and page
                    out.write(pageHeader);
                    out.write((byte)(page & 0xFF));
                    out.write(0x00);

                    // write every address in the page
                    for (int addr = 0; addr < 256; addr++) {
                        out.write(this.cpu.memory[((page << 8) | addr)]);
                    }
                }
            }

            // close our output stream
            out.close();
        } catch (Exception e) {
            System.out.println("Could not write core dump file; " + e.toString());
        }
    }

    public byte[] getMemory() {
        return this.cpu.memory;
    }

    public byte getA() {
        return this.cpu.a;
    }

    public byte getX() {
        return this.cpu.x;
    }

    public byte getY() {
        return this.cpu.y;
    }

    public byte getStackPointer() {
        return this.cpu.sp;
    }

    public byte getStatus() {
        return this.cpu.status;
    }

    public int getPC() {
        return this.cpu.pc;
    }

    /*

    Constructors

    */

    private Debugger() {
        // default constructor
        this.paused = false;
        this.pagesUsed = new boolean[256];  // all initialized to false
        this.breakpoints = new Hashtable<>();
    }

    Debugger(CPU cpu) {
        this();
        this.cpu = cpu;
    }
}
