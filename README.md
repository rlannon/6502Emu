# 6502Emu

A simple 6502 emulator / development kit, written in Java.

## Getting Started

Simply compile and run in your JVM. The access specifiers have not been entirely worked out yet, as the app's user interface has not been designed yet. These access specifiers may change over time in order to reflect the final architecture of the application.

## Features

This project is intended to be a simple development kit for the 6502. While it may eventually lead to an NES emulator (or something similar), it currently has no other hardware emulation.

### Assembler

The kit comes with a very simple single-pass assembler. Projects must currently be located within one file, but as I continue work on the project, I intend to vastly improve the assembler and allow for more functionality and flexibility.

The assembly syntax and conventions I use are fairly standard:

* Directives must be prefixed with a dot. Currently supported:
  * ```.org $XXXX``` - Indicates the assembler should begin laying code at the specified address
  * ```.rs <length> <name>``` - Reserves some memory (variable creation)
  * ```.rsset <address>``` - Sets the address that should be used for the ```.rs``` directive
  * ```.macro <name> <value>``` - Defines a macro; all references to it in the code will be replaced with the macro's value
  * ```.db <bytes>``` or ```.byte <bytes>``` - Defines a series of bytes in program memory
  * ```.dw <words>``` or ```.word <words>``` - Defines a series of words in program memory
* Labels may contain:
  * letters
  * numbers, though they may not begin with a number
  * underscores
  * Labels may begin with dots, but this indicates they are sublabels, and get expanded to their full name. For example, ```.loop``` will be expanded to ```mySubroutine.loop``` if the last macro-level label in the segment is titled ```mySubroutine```. If there is no higher-level label within the current segment, it is a syntax error.

### EMU Files

Both the assembler and CPU utilize ```.emu``` files which contain program and debug information. These files indicate program and segment data as well as line numbers and their corresponding addresses. This will allow the debugger to set breakpoints.

### CPU

The CPU is the [MOS 6502](https://en.wikipedia.org/wiki/MOS_Technology_6502), in case you haven't guessed already. Note that currently, interrupts are not supported as there is no hardware emulation.

### Debugger

The emulator comes with a simple debugger for setting breakpoints and monitoring CPU status.
