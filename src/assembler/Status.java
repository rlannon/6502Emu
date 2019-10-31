package assembler;

public class Status {
    public static final byte NEGATIVE = (byte)0b10000000;
    public static final byte OVERFLOW = 0b01000000;
    // two unused bits between V and D flags
    public static final byte DECIMAL = 0b00001000;
    public static final byte INTERRUPT = 0b00000100;
    public static final byte ZERO = 0b00000010;
    public static final byte CARRY = 0b00000001;
}
