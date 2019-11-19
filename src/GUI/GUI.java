package GUI;

// custom packages
import emu.DrawGraphics;
import emu.Emulator;

// JDK packages
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GUI extends Application {
    /*

    The GUI class allows a user interface with the emulator.
    Note the GUI is also responsible for running programs through the use of the animation timer.

     */

    private Emulator emu;

    final public static int pxWidth = 8;
    final public static int pxHeight = 8;
    final public static int screenWidth = 32;

    private final int INSTRUCTIONS_PER_FRAME;

    private BooleanProperty genCoreDumpProperty;

    private Canvas screen;
    private GraphicsContext screenContext;
    private AnimationTimer timer;
    private long lastNMI;

    private DrawGraphics gDrawer;

    private TextArea userConsole;

    /*

    Methods

     */

    public static void main(String[] args) {
        // launch the program

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("6502 SDK");

        // add the menubar to the page
        MenuBar menuBar = this.createMenu(primaryStage);

        VBox outer = new VBox(menuBar);
        HBox hbox = new HBox();
        hbox.setSpacing(10);
        VBox leftCol = new VBox();
        leftCol.setSpacing(10);
        VBox rightCol = new VBox();
        rightCol.setSpacing(10);
        outer.getChildren().add(hbox);
        hbox.getChildren().addAll(leftCol, rightCol);
        Scene primaryScene = new Scene(outer, 1000, 500);
        primaryStage.setScene(primaryScene);

        // add the register and status monitor
        TextArea registerMonitor = new TextArea();
        registerMonitor.setMaxWidth(300);
        registerMonitor.setMinHeight(200);
        registerMonitor.setEditable(false); // the user cannot modify the text content here, only the program can
        registerMonitor.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
        registerMonitor.appendText("A: $00\nX: $00\nY: $00\n\nPC: $0000\n\nSTATUS:\n\tN V B - D I Z C\n\t0 0 1 1 0 0 0 0");

        // create a label for the monitor
        Label regMonitorLabel = new Label("CPU Status");
        regMonitorLabel.setLabelFor(registerMonitor);

        // add the monitor and its label
        leftCol.getChildren().add(regMonitorLabel);
        leftCol.getChildren().add(registerMonitor);

        // create the information/error console
        userConsole = new TextArea();
        userConsole.setMaxWidth(300);
        userConsole.setMinHeight(200);
        userConsole.setEditable(false);
        userConsole.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));

        Label consoleLabel = new Label("Message Console");
        consoleLabel.setLabelFor(userConsole);

        leftCol.getChildren().add(consoleLabel);
        leftCol.getChildren().add(userConsole);

        // todo: create a canvas and allow graphics updates
        Label screenLabel = new Label("Screen");
        screenLabel.setLabelFor(screen);
        rightCol.getChildren().add(screenLabel);
        rightCol.getChildren().add(screen);
        screenContext.setFill(Color.BLACK);
        screenContext.fillRect(0, 0, screenWidth * pxWidth, screenWidth * pxHeight);

        primaryStage.show();

        // todo: run a loop _here_ to execute the program
        // todo: use an AnimationTimer to trigger NMIs in the emulator loop

        timer = new AnimationTimer() {
            /*

            The animation timer that actually runs the emulator program

             */

            @Override
            public void handle(long now) {
                // This function will be called _approximately_ 60 times per second
                // This means, assuming a clock speed of 2MHz and an average of 4 cycles per instruction, we can execute 8k instructions

                // NMI will be disabled when the CPU is paused for debugging
                if (emu.debugger.isPaused())
                    this.stop();

                if (now - lastNMI > 33_333_333) {
                    lastNMI = System.nanoTime();
                    emu.nmi();
                }

                // begin the thread
                new Thread(gDrawer).start();

                // execute our instructions
                // todo: do we really need separate loops for emu.debugger.step and emu.step, considering emu.step checks for debug mode?
                int i = 0;
                if (emu.isDebugMode()) {
                    while (!emu.debugger.isPaused() && emu.isRunning() && i < INSTRUCTIONS_PER_FRAME) {
                        try {
                            emu.debugger.step();
                            i++;
                        } catch (Exception e) {
                            emu.debugger.terminate();
                            System.out.println("Exception caught: " + e.getMessage());
                        }
                    }
                } else {
                    while (emu.isRunning() && i < INSTRUCTIONS_PER_FRAME) {
                        try {
                            emu.step();
                            i++;
                        } catch (Exception e) {
                            System.out.println("Exception caught: " + e.getMessage());
                        }
                    }
                }

                if (!emu.isRunning()) {
                    this.stop();
                    userConsole.appendText("Done.\n");

                    if (genCoreDumpProperty.get()) {
                        try {
                            emu.coreDump();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }

                // update our CPU monitor
                updateCPUMonitor(registerMonitor);
            }
        };
    }

    public void updateGraphics() {
        // todo: update screen graphics using concurrency
    }

    private void updateCPUMonitor(TextArea monitor) {
        monitor.clear();
        monitor.appendText(
                String.format("A: $%02x\nX: $%02x\nY: $%02x\n\nPC: $%04x\n\nSTATUS:\n\tN V B - D I Z C\n\t0 0 1 1 0 0 0 0",
                        emu.debugger.getA(),
                        emu.debugger.getX(),
                        emu.debugger.getY(),
                        emu.debugger.getPC())
        );
    }

    private void addInput() {
        Stage inputStage = new Stage();
        inputStage.setTitle("Add emulated hardware");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene inputEmuSetup = new Scene(grid, 300, 275);
        inputStage.setScene(inputEmuSetup);
        Text sceneTitle = new Text("Add Input");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label addressLabel = new Label("Memory-mapped Address:");
        grid.add(addressLabel, 0, 1);
        TextField addressField = new TextField();
        grid.add(addressField, 1, 1);
        Label keyMapLabel = new Label("Mapped Key:");
        grid.add(keyMapLabel, 0, 2);
        TextField mappedKey = new TextField();
        grid.add(mappedKey, 1, 2);
        CheckBox triggerIRQ = new CheckBox();
        triggerIRQ.setText("Trigger IRQ");
        triggerIRQ.setSelected(false);
        grid.add(triggerIRQ, 0, 3);

        Button addBtn = new Button("Add Input");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_CENTER);
        hbBtn.getChildren().add(addBtn);
        grid.add(hbBtn, 1, 6);

        addBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                System.out.println("address: " + addressField.getCharacters());
                System.out.println("key binding: " + mappedKey.getCharacters());    // todo: change to dropdown with 'keyboard' or 'mouse'
                System.out.println("trigger IRQ: " + triggerIRQ.isSelected());

                // todo: add input to emulator
            }
        });

        inputStage.show();
    }

    private void displayDebugPanel() {
        // Displays the program debugger
        Stage debugStage = new Stage();
        debugStage.setTitle("Debugger Panel");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene debugScene = new Scene(grid, 500, 250);
        debugStage.setScene(debugScene);

        ListView<Integer> breakpoints = new ListView<>();
        Text breakpointHeader = new Text("Breakpoints");
        breakpointHeader.setFont(Font.font("Tahoma", FontWeight.NORMAL, 12));
        grid.add(breakpointHeader, 0, 0, 2, 1);
        grid.add(breakpoints, 0, 1, 4, 4);

        for (Integer breakpoint: emu.debugger.getBreakpoints())
            breakpoints.getItems().add(breakpoint);

        debugStage.show();
    }

    private void addBreakpointDialog() {
        /*
        Shows the 'add breakpoint' dialog.
        This calls the function 'addBreakpoint' to actually add the data
         */

        Stage breakpointStage = new Stage();
        breakpointStage.setTitle("Add New Breakpoint");

        HBox hb = new HBox(8);
        Scene breakpointScene = new Scene(hb, 350, 30);
        breakpointStage.setScene(breakpointScene);

        ObservableList<String> options =
                FXCollections.observableArrayList(
                  "Address",
                        "Line Number",
                        "Label"
                );
        final ComboBox<String> bpOptions = new ComboBox<>(options);
        bpOptions.setValue("Address");
        hb.getChildren().add(bpOptions);

        TextField data = new TextField();
        hb.getChildren().add(data);

        Button addButton = new Button("Add");
        addButton.setMaxWidth(100);
        hb.getChildren().add(addButton);

        addButton.setOnAction(actionEvent -> {
            // add the breakpoint
            boolean successful = this.addBreakpoint(bpOptions.getValue(), data.getCharacters().toString());

            // if we were successful, close the stage; else, leave it open
            if (successful)
                breakpointStage.close();
        });

        breakpointStage.show();
    }

    private boolean addBreakpoint(String what, String where) {
        /*
         Adds a breakpoint to the emulator's debugger
         Returns whether the breakpoint was added successfully

         @param what    Whether the breakpoint is a label, address, or line number
         @param where   The data in the textfield (actual label name, address, or line number)
         @param stage   The stage onto which we should place the alert dialog
         @return    boolean
         */

        Alert failureAlert = new Alert(Alert.AlertType.ERROR);
        failureAlert.setTitle("Error");

        if (what.equals("Address")) {
            try {
                int address = Integer.parseInt(where, 16);
                if (address >= 0 && address < 65536) {
                    emu.debugger.setBreakpoint(address);
                    return true;
                } else {
                    throw new Exception("Address out of range");
                }
            } catch (NumberFormatException n) {
                failureAlert.setHeaderText("Invalid address");
                failureAlert.setContentText("You must enter a valid hexadecimal number");
                failureAlert.show();
                return false;
            } catch (Exception e) {
                failureAlert.setHeaderText("Invalid address");
                failureAlert.setContentText(e.getMessage());
                failureAlert.show();
                return false;
            }
        } else if (what.equals("Label")) {
            try {
                emu.debugger.setBreakpoint(where);
                return true;
            } catch (Exception e){
                failureAlert.setHeaderText("Label not found");
                failureAlert.setContentText("Label does not exist in the debugger's symbol table");
                failureAlert.show();
                return false;
            }
        } else {
            try {
                int lineNumber = Integer.parseInt(where);
                emu.debugger.setBreakpointByLineNumber(lineNumber);
                return true;
            } catch (Exception e) {
                failureAlert.setHeaderText("Invalid line number");
                failureAlert.setContentText(e.getMessage());
                failureAlert.show();
                return false;
            }
        }
    }

    /*

    Constructor and setup methods

     */

    private MenuBar createMenu(Stage stage) {
        /*

        Creates the menu bar for the application
        Also implements the functionality for each option in said menu

         */

        final MenuBar menu = new MenuBar();

        /*

        FILE MENU

         */

        final Menu fileMenu = new Menu("File");
        // create some menu options
        MenuItem openOption = new MenuItem("Open emu file...");
        // separator
        MenuItem clearConsoleOption = new MenuItem("Clear console");
        // separator
        MenuItem exitOption = new MenuItem("Exit");

        // add them to the file menu
        fileMenu.getItems().addAll(openOption, new SeparatorMenuItem(), clearConsoleOption, new SeparatorMenuItem(), exitOption);

        // set the actions for each item
        openOption.setOnAction(actionEvent -> {
            // create our file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EMU", "*.emu"));

            // get the file
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    emu.addBinary(file.getAbsolutePath());
                    userConsole.appendText("Successfully opened file.\n");
                } catch (Exception e) {
                    Alert fileAlert = new Alert(Alert.AlertType.ERROR);
                    fileAlert.setTitle("File Error");
                    fileAlert.setHeaderText("Could not load file");
                    fileAlert.setContentText(e.getMessage());
                    fileAlert.show();
                }
            } else {
                System.out.println("File was null...");
            }
        });

        clearConsoleOption.setOnAction(actionEvent -> {
            // clear the user console
            userConsole.clear();
        });

        exitOption.setOnAction(actionEvent -> System.exit(0));

        /*

        TOOLS MENU

         */
        final Menu toolsMenu = new Menu("Tools");
        // create some menu options
        // todo: allow text editor in this program?
        MenuItem asmOption = new MenuItem("Assemble...");
        MenuItem disassembleOption = new MenuItem("Disassemble...");
        MenuItem hexdumpOption = new MenuItem("Hexdump");
        CheckMenuItem coreDump = new CheckMenuItem("Generate core dump");   // todo: generate property getter and setter for this

        // add our menu items to the 'tools' menu
        toolsMenu.getItems().addAll(asmOption, disassembleOption, hexdumpOption, new SeparatorMenuItem(), coreDump);

        // set our actions for each option
        asmOption.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All files", "*.*"),
                    new FileChooser.ExtensionFilter("S File", "*.s"),
                    new FileChooser.ExtensionFilter("ASM File", "*.asm")
            );
            File asmFile = fileChooser.showOpenDialog(stage);

            if (asmFile != null) {
                userConsole.appendText("Assembling...\n");
                try {
                    emu.assemble(asmFile.getAbsolutePath(), "assembled1.emu");
                    userConsole.appendText("Done; no errors.\n");
                } catch (Exception e) {
                    userConsole.appendText("**** Assembly Error ****\n");
                    userConsole.appendText(e.getMessage() + "\n");
                }
            } else {
                System.out.println("No file to assemble.\n");
            }
        });

        disassembleOption.setOnAction(actionEvent -> {
            // todo: disassembly
            System.out.println("Disassembly not yet implemented");
        });

        hexdumpOption.setOnAction(actionEvent -> {
            // todo: hexdump
            System.out.println("Hexdump not yet implemented");
        });

        // set our 'genCoreDumpProperty' to be equal to our
        genCoreDumpProperty = coreDump.selectedProperty();

        /*

        RUN MENU

         */
        final Menu runMenu = new Menu("Run");
        MenuItem runOption = new MenuItem("Run...");
        MenuItem debugOption = new MenuItem("Debug...");
        MenuItem stopOption = new MenuItem("Terminate");
        MenuItem resetOption = new MenuItem("Reset");
        MenuItem debuggerPanelOption = new MenuItem("Open Debugger Panel");
        MenuItem addBreakpointOption = new MenuItem("Add Breakpoint...");
        MenuItem removeBreakpointOption = new MenuItem("Remove Breakpoint...");

        runMenu.getItems().addAll(runOption, debugOption, new SeparatorMenuItem(), stopOption, resetOption,
                new SeparatorMenuItem(), debuggerPanelOption, addBreakpointOption, removeBreakpointOption);

        runOption.setOnAction(actionEvent -> {
            // Run program
            userConsole.appendText("Running...\n");
            if (genCoreDumpProperty.get()) {
                Alert coreDumpAlert = new Alert(Alert.AlertType.WARNING);
                coreDumpAlert.setTitle("Runtime error");
                coreDumpAlert.setHeaderText("Cannot generate core dump");
                coreDumpAlert.setContentText("You must run a program in debug mode to generate a core dump");
                coreDumpAlert.show();
            }
            emu.setDebugMode(false);
            emu.reset();
            lastNMI = System.nanoTime();
            timer.start();
        });

        debugOption.setOnAction(actionEvent -> {
            // Run a program in debug mode
            userConsole.appendText("Debugging...\n");
            emu.debugger.setGenCoreDump(genCoreDumpProperty.get());
            displayDebugPanel();  // display debugger panel
            emu.setDebugMode(true); // run in debug mode
            emu.reset();
            lastNMI = System.nanoTime();
            timer.start();
        });

        stopOption.setOnAction(actionEvent -> {
            // Terminate program execution
            emu.terminate();
            timer.stop();
            userConsole.appendText("Terminated.\n");
        });

        resetOption.setOnAction(actionEvent -> {
            // Reset CPU

            // First, terminate
            emu.terminate();
            timer.stop();

            // Then, reset
            emu.reset();

            // Print a message in the log
            userConsole.appendText("Reset.\n");
        });

        debuggerPanelOption.setOnAction(actionEvent -> {
            // displays the debugger panel without debugging the program
            displayDebugPanel();
        });

        addBreakpointOption.setOnAction(actionEvent -> {
            // Add a breakpoint using the breakpoint dialog
            addBreakpointDialog();
        });

        removeBreakpointOption.setOnAction(actionEvent ->  {
            // Remove a breakpoint using the breakpoint dialog
        });

        // Create the MenuBar
        menu.getMenus().addAll(fileMenu, toolsMenu, runMenu);

        // return the MenuBar
        return menu;
    }

    public GUI() {
        this.emu = new Emulator(this);
        this.INSTRUCTIONS_PER_FRAME = 8_000;
        this.screen = new Canvas(screenWidth * pxWidth, screenWidth * pxHeight);
        this.screenContext = screen.getGraphicsContext2D();
        this.lastNMI = 0;
        this.gDrawer = new DrawGraphics("gDrawer", this.screenContext, this.emu.getMemory());
    }
}
