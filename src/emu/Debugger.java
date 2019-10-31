package emu;

public class Debugger {
    // The debugger for our CPU

    CPU cpu;
    boolean stopped;   // whether we have stoped the CPU

    public void stop()
    {
        // halts the CPU
        this.stopped = true;
    }

    public void resume()
    {
        // resumes CPU execution
        this.stopped = false;
    }

    public boolean isStopped()
    {
        return this.stopped;
    }

    public void step() throws Exception
    {
        // steps the CPU one time
        this.cpu.step();
    }

    Debugger()
    {
        // default constructor
        this.stopped = false;
    }

    Debugger(CPU cpu)
    {
        this();
        this.cpu = cpu;
    }
}
