package emu;

class Main {
    public static void main(String[] args) {
        try {
            Emulator emu = new Emulator();
            emu.assembleAndAdd("test.s");
            emu.run(true);
            emu.coreDump();
        } catch (Exception e) {
            System.out.println("Caught exception: " + e.toString());
        }
    }
}
