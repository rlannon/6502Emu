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
import java.util.List;
import java.util.ListIterator;

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

    private TextArea registerMonitor;
    private TextArea userConsole;

    /*

    Methods

     */

    /*

    Functionality

     */

    private void deleteBreakpoints(ObservableList<Integer> toDelete) {
        // delete breakpoints from our list

        // only delete items if we have more than none selected
        if (toDelete.size() > 0) {
            for (Integer bp: toDelete) {
                emu.debugger.removeBreakpoint(bp);
            }
        } else {
            // display an error alert if we have none selected
            Alert selectedBreakpointsAlert = new Alert(Alert.AlertType.ERROR);
            selectedBreakpointsAlert.setTitle("Invalid selection");
            selectedBreakpointsAlert.setHeaderText("No breakpoints selected");
            selectedBreakpointsAlert.setContentText("You must select breakpoints to delete them");
            selectedBreakpointsAlert.show();
        }
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

    private void resume() {
        // Resume CPU execution after a pause
        emu.debugger.resume();
        lastNMI = System.nanoTime();
        timer.start();
    }

    /*

    Main methods

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
        leftCol.setPadding(new Insets(10, 10, 10, 10));
        VBox rightCol = new VBox();
        rightCol.setSpacing(10);
        rightCol.setPadding(new Insets(10, 10, 10, 10));
        outer.getChildren().add(hbox);
        hbox.getChildren().addAll(leftCol, rightCol);
        Scene primaryScene = new Scene(outer, 700, 600);
        primaryStage.setScene(primaryScene);

        // add the register and status monitor
        registerMonitor = new TextArea();
        registerMonitor.setMaxWidth(256);
        registerMonitor.setMinHeight(200);
        registerMonitor.setEditable(false); // the user cannot modify the text content here, only the program can
        registerMonitor.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
        registerMonitor.appendText("A: $00\nX: $00\nY: $00\nSP: $FF\n\nPC: $0000\n\nSTATUS:\n\tN V B - D I Z C\n\t0 0 1 1 0 0 0 0");

        // create a label for the monitor
        Label regMonitorLabel = new Label("CPU Status");
        regMonitorLabel.setLabelFor(registerMonitor);

        // create the information/error console
        userConsole = new TextArea();
        userConsole.setMaxWidth(256);
        userConsole.setMinHeight(500);
        userConsole.setEditable(false);
        userConsole.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));

        Label consoleLabel = new Label("Message Console");
        consoleLabel.setLabelFor(userConsole);

        leftCol.getChildren().add(consoleLabel);
        leftCol.getChildren().add(userConsole);

        /*

        Right Column

         */

        // Add the screen
        Label screenLabel = new Label("Screen");
        screenLabel.setLabelFor(screen);
        rightCol.getChildren().add(screenLabel);
        rightCol.getChildren().add(screen);
        clearScreen();

        // add the monitor and its label
        rightCol.getChildren().add(regMonitorLabel);
        rightCol.getChildren().add(registerMonitor);

        // finally, show the stage
        primaryStage.show();

        // we will use an animation timer to control CPU speed
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
                Thread drawerThread = new Thread(gDrawer);
                drawerThread.start();

                // execute our instructions
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

                // wait for the draw thread to finish
                try {
                    drawerThread.join();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted. " + e.getMessage());
                    this.stop();
                }

                // update our CPU monitor
                updateCPUMonitor();
            }
        };
    }

    private void updateCPUMonitor() {
        registerMonitor.clear();
        registerMonitor.appendText(
                String.format("A: $%02x\nX: $%02x\nY: $%02x\nSP: $%02x\n\nPC: $%04x\n\nSTATUS:\n\tN V B - D I Z C\n\t0 0 1 1 0 0 0 0",
                        emu.debugger.getA(),
                        emu.debugger.getX(),
                        emu.debugger.getY(),
                        emu.debugger.getStackPointer(),
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

        // Create the gridpane
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene debugScene = new Scene(grid, 500, 250);
        debugStage.setScene(debugScene);

        // List our breakpoints
        ListView<Integer> breakpoints = new ListView<>();
        breakpoints.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);   // allow multiple items to be selected

        // set the cell factory to display the number as hexadecimal
        breakpoints.setCellFactory(column -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("$%04x", item));
                }
            }
        });

        // Create a display for our breakpoints
        Text breakpointHeader = new Text("Breakpoints");
        breakpointHeader.setFont(Font.font("Tahoma", FontWeight.NORMAL, 12));
        grid.add(breakpointHeader, 0, 0, 2, 1);
        grid.add(breakpoints, 0, 1, 4, 5);

        // update the view
        updateBreakpointsListView(breakpoints);

        // Create some buttons for interactivity

        // CPU step button

        Button stepButton = new Button("Step");
        grid.add(stepButton, 5, 1, 2, 1);

        stepButton.setOnAction(actionEvent -> {
            if (emu.isDebugMode() && emu.debugger.isPaused()) {
                try {
                    emu.debugger.step();
                    updateCPUMonitor();
                } catch (Exception e) {
                    emu.terminate();
                    userConsole.appendText("Error encountered: " + e.getMessage() + "\n");
                }
            } else {
                // todo: only allow button to be pressed when it is paused?
                System.out.println("Cannot step when CPU is not paused");
            }
        });

        // Continue
        Button continueButton = new Button("Continue");
        grid.add(continueButton, 5, 2, 2, 1);

        continueButton.setOnAction(actionEvent -> {
            // we need to step once before we can resume
            try {
                emu.debugger.step();
                resume();
            } catch (Exception e) {
                userConsole.appendText("Could not continue: " + e.getMessage() + "\n");
            }
        });

        // Trigger NMI, graphics update buttons
        Button triggerNMIButton = new Button("Trigger NMI");
        grid.add(triggerNMIButton, 5, 3, 2, 1);

        triggerNMIButton.setOnAction(actionEvent -> emu.nmi());

        Button updateGraphicsButton = new Button("Update Graphics");
        grid.add(updateGraphicsButton, 5, 4, 2, 1);

        updateGraphicsButton.setOnAction(actionEvent -> {
            Thread drawThread = new Thread(gDrawer);
            drawThread.start();
        });

        // delete breakpoint
        Button deleteBreakpointsButton = new Button("Delete Breakpoints");
        grid.add(deleteBreakpointsButton, 5, 5, 2, 1);

        // Add functionality to the button
        deleteBreakpointsButton.setOnAction(actionEvent -> {
            // todo: refactor this so it isn't a lambda, as we use use it elsewhere
            // get the selected items
            ObservableList<Integer> selectedBreakpoints = breakpoints.getSelectionModel().getSelectedItems();
            // delete them
            deleteBreakpoints(selectedBreakpoints);
            // update the view
            updateBreakpointsListView(breakpoints);
        });

        // display our panel
        debugStage.show();
    }

    private void updateBreakpointsListView(ListView<Integer> breakpoints) {
        // Update the list view for 'breakpoints'
        breakpoints.getItems().clear(); // first, clear the list
        for (Integer breakpoint: emu.debugger.getBreakpoints()) // repopulate it
            breakpoints.getItems().add(breakpoint);
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

    private void deleteBreakpointDialog() {
        /*
        Removes a breakpoint from the debugger using a dialog
         */

        Stage breakpointStage = new Stage();
        breakpointStage.setTitle("Delete Breakpoint");

        // Use a VBox for our dialog
        VBox vb = new VBox(8);
        vb.setSpacing(10);
        vb.setPadding(new Insets(10, 10, 10, 10));
        Scene breakpointScene = new Scene(vb, 256, 256);
        breakpointStage.setScene(breakpointScene);

        // List our breakpoints
        ListView<Integer> breakpoints = new ListView<>();
        breakpoints.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);   // allow multiple breakpoints to be selected

        // set the cell factory to display the number as hexadecimal
        breakpoints.setCellFactory(column -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("$%04x", item));
                }
            }
        });

        // Create the delete button
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(actionEvent -> {
            // todo: update this when we refactor the function
            // get the selected items
            ObservableList<Integer> selectedBreakpoints = breakpoints.getSelectionModel().getSelectedItems();
            // delete them
            deleteBreakpoints(selectedBreakpoints);
            // update the view
            updateBreakpointsListView(breakpoints);
        });

        // add view and buttons to the vbox
        vb.getChildren().addAll(breakpoints, deleteButton);

        // update our view
        updateBreakpointsListView(breakpoints);

        // show the stage
        breakpointStage.show();
    }

    /*

    Constructor and setup methods

     */

    private void clearScreen() {
        // Clears the output screen by filling it with all black
        screenContext.setFill(Color.BLACK);
        screenContext.fillRect(0, 0, screenWidth * pxWidth, screenWidth * pxHeight);
    }

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
                    emu.debugger.pause();
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
                    new FileChooser.ExtensionFilter("Assembly Files", "*.s", "*.S", "*.asm")
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
        MenuItem stopOption = new MenuItem("Terminate");
        MenuItem resetOption = new MenuItem("Reset");

        runMenu.getItems().addAll(runOption, stopOption, resetOption);

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
            resume();
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

            // Then, reset the CPU, clear the screen and the monitor, and mark that the CPU is paused
            emu.reset();
            updateCPUMonitor();
            clearScreen();
            emu.debugger.pause();

            // Print a message in the log
            userConsole.appendText("Reset.\n");
        });

        /*

        Debug Menu

         */
        final Menu debugMenu = new Menu("Debug");
        MenuItem debugOption = new MenuItem("Debug...");
        MenuItem debuggerPanelOption = new MenuItem("Open Debugger Panel");
        MenuItem addBreakpointOption = new MenuItem("Add Breakpoint...");
        MenuItem removeBreakpointOption = new MenuItem("Delete Breakpoint...");
        CheckMenuItem enableDebugMode = new CheckMenuItem("Enable Debug Mode");
        debugMenu.getItems().addAll(debugOption, debuggerPanelOption, new SeparatorMenuItem(), addBreakpointOption,
                removeBreakpointOption, new SeparatorMenuItem(), enableDebugMode);

        debugOption.setOnAction(actionEvent -> {
            // Run a program in debug mode
            userConsole.appendText("Debugging...\n");
            emu.debugger.setGenCoreDump(genCoreDumpProperty.get());
            displayDebugPanel();  // display debugger panel
            emu.setDebugMode(true); // run in debug mode
            enableDebugMode.setSelected(true);
            emu.reset();
            resume();
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
            deleteBreakpointDialog();
        });

        enableDebugMode.setOnAction(actionEvent -> {
            emu.setDebugMode(enableDebugMode.selectedProperty().get());
        });

        // Create the MenuBar
        menu.getMenus().addAll(fileMenu, toolsMenu, runMenu, debugMenu);

        // return the MenuBar
        return menu;
    }

    public GUI() {
        this.emu = new Emulator(this);
        this.INSTRUCTIONS_PER_FRAME = 5_000;
        this.screen = new Canvas(screenWidth * pxWidth, screenWidth * pxHeight);
        this.screenContext = screen.getGraphicsContext2D();
        this.lastNMI = 0;
        this.gDrawer = new DrawGraphics("gDrawer", this.screenContext, this.emu.getMemory());
    }
}
