package emu;

import assembler.DebugSymbol;
import javafx.util.Pair;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.*;

public class Debugger {
    // The debugger for our CPU

    private CPU cpu;
    private boolean paused;   // whether we have stoped the CPU
    private boolean genCoreDump;    // whether we should generate a core dump on termination
    private boolean[] pagesUsed;    // tracks which pages have been touched by the CPU
    private ArrayList<Pair<Integer, Integer>> segments;    // tracks where our segments are
    Hashtable<Integer, Boolean> breakpoints;  // the breakpoints we have set
    private Hashtable<String, Integer> labels; // symbols and their addresses
    private Hashtable<Integer, Integer> lineNumbers;    // line numbers and their addresses

    /*

    Utility functions

     */

    public void setGenCoreDump(boolean toSet) {
        this.genCoreDump = toSet;
    }

    public void setBreakpoint(int address) {
        // Set a new breakpoint for the given address
        this.breakpoints.put(address, Boolean.TRUE);
    }

    public int getAddressFromLineNumber(int lineNumber) throws Exception {
        if (this.lineNumbers.containsKey(lineNumber)) {
            return this.lineNumbers.get(lineNumber);
        } else {
            throw new Exception("No such line number in file");
        }
    }

    public int getAddressFromLabel(String label) throws Exception {
        if (this.labels.containsKey(label)) {
            return this.labels.get(label);
        } else {
            throw new Exception("No such label found in debug symbols");
        }
    }

    public void removeBreakpoint(int address) {
        /*
        Remove the breakpoint at a given address
        @param  address The address of the breakpoint
         */
        this.breakpoints.remove(address);
    }

    public ArrayList<Integer> getBreakpoints() {
        return Collections.list(this.breakpoints.keys());
    }

    void addUsedPage(int page) {
        this.pagesUsed[page] = true;
    }

    void addUsedPageByAddress(int address) {
        // adds the page under 'address' as used
        int pageNumber = (address >> 8) & 0xFF;
        this.pagesUsed[pageNumber] = true;
    }

    void setDebugSymbols(Vector<DebugSymbol> toSet) {
        // debug symbols available to the debugger

        for (DebugSymbol sym: toSet) {
            // if the symbol has a label, add it to "labels"
            if (!sym.getLabel().equals("")) {
                this.labels.put(sym.getLabel(), sym.getAddress() & 0xFFFF);
            }

            // add the line number data to "lineNumbers"
            this.lineNumbers.put(sym.getLine(), sym.getAddress() & 0xFFFF);
        }
    }

    void addSegment(int begin, int length) {
        // Adds a segment to the debugger's list of code segments
        this.segments.add(new Pair<>(begin, length));
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

        // if we need to, generate a core dump
        if (this.genCoreDump) {
            this.generateCoreDump();
        }
    }

    public void resume() {
        // resumes CPU execution after a debug event has stopped it
        this.paused = false;
    }

    public boolean isPaused() {
        // tells us whether the debug program has stopped CPU execution
        return this.paused;
    }

    public void step() throws Exception {
        // steps the CPU one time; if a breakpoint is encountered, pauses

        if (this.isPaused()) {
            this.cpu.step();
        } else {
            if (this.breakpoints.containsKey(this.cpu.pc)) {
                this.pause();
            } else {
                this.cpu.step();
            }
        }
    }

    public void jump(int address) throws Exception {
        if (this.isPaused()) {
            this.cpu.pc = address;
        } else {
            throw new Exception("CPU must be paused to perform this operation");
        }
    }

    /*

    Fetch CPU Data
    Because the CPU data is package-private, we need public functions in the debugger to access that data
    The CPU internals should only be accessible through the debugger

     */

    void generateCoreDump() {
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

    // todo: determine whether these fetch functions are really necessary given the emulator is in the package
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
        this.paused = false;
        this.pagesUsed = new boolean[256];  // all initialized to false
        this.pagesUsed[1] = true;   // we will always include the stack in a core dump
        this.segments = new ArrayList<>();
        this.breakpoints = new Hashtable<>();
        this.lineNumbers = new Hashtable<>();
        this.labels = new Hashtable<>();
    }

    Debugger(CPU cpu) {
        // The debugger _must_ be initialized with a CPU

        this();
        this.cpu = cpu;
    }
}
