package GUI;

// custom packages
import emu.Emulator;

// JDK packages
import javafx.animation.AnimationTimer;
import javafx.application.Application;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;

public class GUI extends Application {
    private Emulator emu;

    final public static int pxWidth = 8;
    final public static int pxHeight = 8;
    final public static int screenWidth = 32;

    private Canvas screen;
    private GraphicsContext screenContext;

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

        AnimationTimer timer = new AnimationTimer() {
            /*

            The animation timer that actually runs the emulator program

             */

            @Override
            public void handle(long now) {
                // This function will be called _approximately_ 60 times per second
                // This means, assuming a clock speed of 2MHz and an average of 4 cycles per instruction, we can execute 8k instructions
                // todo: execute instructions here
                // todo: signal a CPU NMI if it has been >= 1/30th of a second since last NMI; then, execute
            }
        };

        // todo: start timer when the user hits 'run'
        timer.start();
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
                System.out.println("key binding: " + mappedKey.getCharacters());
                System.out.println("trigger IRQ: " + triggerIRQ.isSelected());
            }
        });

        inputStage.show();
    }

    /*

    Constructor and setup methods

     */

    private MenuBar createMenu(Stage stage) {
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

        openOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(stage);
                if (file != null) {
                    System.out.println("file: " + file.getAbsolutePath());
                } else {
                    System.out.println("File was null...");
                }
            }
        });

        exitOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        /*

        TOOLS MENU

         */
        final Menu toolsMenu = new Menu("Tools");
        // create some menu options
        // MenuItem editorOption = new MenuItem("Open Editor"); // todo: enable editor?
        MenuItem asmOption = new MenuItem("Assemble...");
        MenuItem disassembleOption = new MenuItem("Disassemble...");
        MenuItem hexdumpOption = new MenuItem("Hexdump");
        CheckMenuItem coreDump = new CheckMenuItem("Generate core dump");
        // add the items to the tools menu
        // toolsMenu.getItems().add(editorOption);
        toolsMenu.getItems().addAll(asmOption, disassembleOption, hexdumpOption, coreDump);

        asmOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
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
            }
        });

        disassembleOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // todo: disassembly
                System.out.println("Disassembly not yet implemented");
            }
        });

        hexdumpOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // todo: hexdump
                System.out.println("Hexdumb not yet implemented");
            }
        });

        /*

        RUN MENU

         */
        final Menu runMenu = new Menu("Run");
        MenuItem runOption = new MenuItem("Run...");
        MenuItem debugOption = new MenuItem("Debug...");

        runMenu.getItems().addAll(runOption, debugOption);

        runOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // todo: run program
                System.out.println("Running program (well, it will)...");
            }
        });

        debugOption.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // todo: debug program
                System.out.println("Running program in debug mode (well, it will)...");
            }
        });

        // Create the MenuBar
        menu.getMenus().addAll(fileMenu, toolsMenu, runMenu);

        // return the MenuBar
        return menu;
    }

    public GUI() {
        this.emu = new Emulator(this);
        this.screen = new Canvas(screenWidth * pxWidth, screenWidth * pxHeight);
        this.screenContext = screen.getGraphicsContext2D();
    }
}
