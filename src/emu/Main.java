package emu;

class Main {
    public static void main(String[] args) {
        try {
            Emulator emu = new Emulator();
            emu.assembleAndAdd("test.s");
            //emu.setBreakpoint(0x8000);
            emu.run(true);
            emu.coreDump();
        } catch (Exception e) {
            System.out.println("Caught exception: " + e.toString());
        }
    }
}
