package emu;

import GUI.GUI;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class DrawGraphics implements Runnable {
    /*

    The class which updates the graphics on the screen based on the "vram" in the CPU
    It accomplishes this by creating a new thread and performing the update in that thread

     */

    final private String threadName;
    final private byte[] memory;
    final private GraphicsContext gc;

    private final static int BUFFER_MIN = 0x2400;
    private final static int BUFFER_LEN = GUI.screenWidth * GUI.screenWidth;
    private final static Color[] colors = {
            Color.BLACK, Color.WHITE, Color.RED, Color.CYAN, Color.PURPLE, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.ORANGE, Color.BROWN, Color.PINK, Color.DARKGRAY, Color.GRAY, Color.LIGHTGREEN,
            Color.LIGHTBLUE, Color.LIGHTGRAY
    };

    public void run() {
        // Executes the graphics drawing thread

        try {
            Thread.sleep(1);    // wait 1ms before drawing graphics to allow NMI time to execute

            // copy our data to the GUI
            // the length of that buffer
            for (int i = 0; i < BUFFER_LEN; i++) {
                // get the color of the pixel based on the value at the address
                // the beginning of the graphics buffer
                byte colorByte = this.memory[BUFFER_MIN + i];
                colorByte &= 0x0F;  // we only care about the low nibble

                Color color;
                if (colorByte < colors.length) {
                    color = colors[colorByte];
                }
                else {
                    color = Color.BLACK;
                }

                // now that we have the color, get the coordinate from the address
                int y = (i / GUI.screenWidth) * GUI.pxHeight;
                int x = (i % GUI.screenWidth) * GUI.pxWidth;

                // fill the rectangle accordingly
                gc.setFill(color);
                gc.fillRect(x, y, GUI.pxWidth, GUI.pxHeight);
            }
        } catch (InterruptedException e) {
            System.out.println("Failed to draw graphics!");
        }
    }

    public void start() {
        System.out.println("Starting thread...");
        Thread t = new Thread(this, this.threadName);
        t.start();
    }

    public DrawGraphics(String name, GraphicsContext gc, byte[] memory) {
        this.threadName = name;
        this.memory = memory;
        this.gc = gc;
    }
}
