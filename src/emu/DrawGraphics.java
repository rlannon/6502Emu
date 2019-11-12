package emu;

import GUI.GUI;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DrawGraphics implements Runnable {
    /*

    The class which updates the graphics on the screen based on the "vram" in the CPU
    It accomplishes this by creating a new thread and performing the update in that thread

     */

    final int BUFFER_MIN = 0x2400;  // the beginning of the graphics buffer
    final int BUFFER_LEN = GUI.screenWidth * GUI.screenWidth;    // the length of that buffer

    private Thread t;
    private String threadName;
    private byte[] memory;
    private GraphicsContext gc;

    public void run() {
        // Executes the graphics drawing thread

        System.out.println("Drawing graphics");

        // todo: how to update canvas?

        // copy our data to the GUI
        for (int i = 0; i < BUFFER_LEN; i++) {
            // get the color of the pixel based on the value at the address
            byte colorByte = this.memory[BUFFER_MIN + i];
            Color color;
            switch (colorByte) {
                case 1:
                    color = Color.WHITE;
                    break;
                case 2:
                    color = Color.RED;
                    break;
                case 3:
                    color = Color.CYAN;
                    break;
                case 4:
                    color = Color.PURPLE;
                    break;
                case 5:
                    color = Color.GREEN;
                    break;
                case 6:
                    color = Color.BLUE;
                    break;
                case 7:
                    color = Color.YELLOW;
                    break;
                case 8:
                    color = Color.ORANGE;
                    break;
                case 9:
                    color = Color.BROWN;
                    break;
                default:
                    color = Color.BLACK;
                    break;
            }

            // now that we have the color, get the coordinate from the address
            int y = (i / GUI.screenWidth) * GUI.pxHeight;
            int x = i * GUI.pxWidth;

            // fill the rectangle accordingly
            gc.setFill(color);
            gc.fillRect(x, y, GUI.pxWidth, GUI.pxHeight);
        }
    }

    boolean isAlive() {
        // Determines whether the thread is running or not

        if (t == null) {
            return false;
        } else {
            return t.isAlive();
        }
    }

    void join() throws InterruptedException {
        if (t != null) {
            t.join();
        }
    }

    void start() {
        System.out.println("Starting thread...");
        t = new Thread(this, this.threadName);
        t.start();
    }

    DrawGraphics(String name, GraphicsContext gc, byte[] memory) {
        this.threadName = name;
        this.memory = memory;
        this.gc = gc;
    }
}
