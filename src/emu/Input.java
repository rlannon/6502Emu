package emu;

public class Input {
    /*
    The class for handling our input
    Has a memory-mapped address, whether it triggers an IRQ, and the appropriate key bindings
     */

    private boolean triggersIRQ;    // whether this input triggers an IRQ
    private short address;  // the address to which that input is mapped
    private int mappedKeyCode;  // the key code associated with the input

    short getAddress() {
        return this.address;
    }

    boolean triggersIRQ() {
        return this.triggersIRQ;
    }

    Input(int mappedKeyCode, short address) {
        this(mappedKeyCode, address, false);
    }

    Input(int mappedKeyCode, short address, boolean triggersIRQ) {
        this.mappedKeyCode = mappedKeyCode;
        this.address = address;
        this.triggersIRQ = triggersIRQ;
    }
}
