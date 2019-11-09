package assembler;

public final class Status {
    public static final byte NEGATIVE = (byte)0b10000000;
    public static final byte OVERFLOW = 0b01000000;

    // unused 5th bit
    public static final byte B = 0b00010000;    // distinguishes how the flags were pushed

    public static final byte DECIMAL = 0b00001000;
    public static final byte INTERRUPT_DISABLE = 0b00000100;
    public static final byte ZERO = 0b00000010;
    public static final byte CARRY = 0b00000001;
}
