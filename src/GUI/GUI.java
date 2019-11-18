package GUI;

// custom packages
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GUI extends Application {
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

    private void drawPixel(int address, byte value) {

    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("6502 SDK");

        // add the menubar to the page
        MenuBar menuBar = this.createMenu(primaryStage);

        VBox vbox = new VBox(menuBar);
        Scene primaryScene = new Scene(vbox, 1000, 500);
        primaryStage.setScene(primaryScene);

        // create a canvas

        vbox.getChildren().add(screen);

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
                // todo: execute instructions here
                // todo: signal a CPU NMI if it has been >= 1/30th of a second since last NMI; then, execute
                if (now - lastNMI > 33_333_333) {
                    lastNMI = System.nanoTime();
                    emu.nmi();
                }

                // todo: update graphics w/ canvas

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

                    if (genCoreDumpProperty.get()) {
                        try {
                            emu.coreDump();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        };
    }

    public void updateGraphics() {

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

    private void displayDebugger() {
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
            // todo: make sure it's a valid hex address
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
            // todo: check to see if a label exists in the debugger's symbol list
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
            // todo: get line number
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
        MenuItem exitOption = new MenuItem("Exit");

        // add them to the file menu
        fileMenu.getItems().add(openOption);
        fileMenu.getItems().add(new SeparatorMenuItem());
        fileMenu.getItems().add(exitOption);

        // set the actions for each item
        openOption.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    emu.addBinary(file.getAbsolutePath());
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
            System.out.println("Assembling...");
            FileChooser fileChooser = new FileChooser();
            File asmFile = fileChooser.showOpenDialog(stage);
            if (asmFile != null) {
                try {
                    emu.assemble(asmFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Could not assemble file:");
                    System.out.println(e.getMessage());
                }
            } else {
                System.out.println("Could not assemble file");
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
        MenuItem addBreakpointOption = new MenuItem("Add Breakpoint...");
        MenuItem removeBreakpointOption = new MenuItem("Remove Breakpoint...");

        runMenu.getItems().addAll(runOption, debugOption, new SeparatorMenuItem(), stopOption, new SeparatorMenuItem(), addBreakpointOption, removeBreakpointOption);

        runOption.setOnAction(actionEvent -> {
            // todo: run program
            System.out.println("Running program (well, it will)...");
            System.out.println("Gen core dump? " + genCoreDumpProperty.get());
            emu.reset();
            lastNMI = System.nanoTime();
            timer.start();
        });

        debugOption.setOnAction(actionEvent -> {
            // todo: debug program
            System.out.println("Running program in debug mode (well, it will)...");
            System.out.println("Gen core dump? " + genCoreDumpProperty.get());
            displayDebugger();
        });

        stopOption.setOnAction(actionEvent -> {
            // Terminate program execution
            emu.terminate();
            timer.stop();
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
    }
}
