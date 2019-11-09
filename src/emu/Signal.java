package emu;

public enum Signal {
    /*

    An enum containing our processor signals
    Use of an enum will allow us to generate compile-time errors to ensure only a signal from this enum is passed
        to the CPU.signal function

     */

    NMI,
    RESET,
    IRQ,
    BRK
}
