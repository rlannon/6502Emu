package emu;

public class Input {
    /*
    The class for handling our input
    Has a memory-mapped address, whether it triggers an IRQ, and the appropriate key bindings
     */

    private boolean triggersIRQ;    // whether this input triggers an IRQ
    private int address;  // the address to which that input is mapped
    private String mappedKeyCode;  // the key code associated with the input

    public String getMappedKeyCode() {
        return this.mappedKeyCode;
    }

    public int getAddress() {
        return this.address & 0xFFFF;
    }

    public boolean isTriggersIRQ() {
        return this.triggersIRQ;
    }

    Input(String key, short address) {
        this(key, address, false);
    }

    Input(String mappedKeyCode, int address, boolean triggersIRQ) {
        this.mappedKeyCode = mappedKeyCode;
        this.address = address;
        this.triggersIRQ = triggersIRQ;
    }
}
