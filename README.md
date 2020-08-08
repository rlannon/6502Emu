# 6502Emu

A simple 6502 emulator and development kit, written in Java.

## Getting Started

Simply compile and run in your JVM. Note that because JavaFX is used for the user interface, it cannot be compiled to a `.jar` file (or at least not easily).

## Features

This project is intended to be a simple development kit for the 6502. While it may eventually lead to an NES emulator (or something similar), it currently has very limited hardware emulation capabilities which are not nearly as complex or powerful as an NES emulator might.

### Assembler

The kit comes with a very simple single-pass assembler. Projects must currently be located within one file, but as I continue work on the project, I intend to vastly improve the assembler and allow for more functionality and flexibility.

The assembly syntax and conventions I use are fairly standard:

* Directives must be prefixed with a dot. Currently supported:
  * `.org $XXXX` - Indicates the assembler should begin laying code at the specified address
  * `.rs <length> <name>` - Reserves some memory (variable creation)
  * `.rsset <address>` - Sets the address that should be used for the `.rs` directive
  * `.macro <name> <value>` - Defines a macro; all references to it in the code will be replaced with the macro's value
  * `.db <bytes>` or `.byte <bytes>` - Defines a series of bytes in program memory
  * `.dw <words>` or `.word <words>` - Defines a series of words. Note you should not define them in little endian format, the assembler will do it automatically. If you wish to specify data this way, then use `.db`.
* Labels may contain:
  * letters
  * numbers, though they may not begin with a number
  * underscores
  * Labels may begin with dots, but this indicates they are _sublabels_ which get expanded to a longer name beginning with the last higher-level label. For example, `.loop` will be expanded to `mySubroutine.loop` if the last macro-level label found is titled `mySubroutine`. If there is no higher-level label, using a sublabel is a syntax error.

### EMU Files

Both the assembler and CPU utilize `.emu` files which contain program and debug information. These files indicate program and segment data as well as line numbers and their corresponding addresses. The format also notes the addresses of program labels. These features allow the debugger to set breakpoints at addresses, line numbers, or labels.

### CPU

The CPU is the [MOS 6502](https://en.wikipedia.org/wiki/MOS_Technology_6502), in case you haven't guessed already. Currently, the emulator does not accurately track clock cycle information, and a rough estimate of the number of instructions that can be executed per second is given. However, this number is probably a little too low, and a future update will include more accurate CPU timings.

### Emulated Hardware

This project allows for limited hardware emulation capabilities:

#### Screen

There is a 32x32 emulated screen between memory locations `$2400` and `$27FF`. Information about said output can be found in `src/GUI/Emulated Inputs and Outputs`. The screen generates an NMI 1 millisecond before each update, which occurs at a framerate of *approximately* 30Hz. A Java animation timer and concurrency are both used to accomplish this.

#### Inputs

The user may also configure emulated inputs. The user may choose from individual keys or from the keyboard as a whole; if the latter is chosen, it overrides all other inputs that are configured. Inputs may be mapped to a memory address and the user may choose whether they should trigger an IRQ.

### Debugger

The emulator comes with a simple debugger for debugging programs that allows the user to set breakpoints, step through program execution, trigger NMIs and graphics updates, set the program counter, and monitor memory. The project also comes with a simple disassembler that allows users to examine program memory and instructions during program execution.
