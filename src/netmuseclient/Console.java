package netmuseclient;
import processing.core.PApplet;
import processing.core.PFont;

import java.awt.Color;
import java.util.*;

import org.apache.tools.ant.taskdefs.LoadFile;


/**
 * reusable text console
 * @author bogdan
 *
 */
public class Console {

    Timer time;
    int x, y;
    int w, h;
    PFont font;
    int fsize;
    int color;
    NetMuseClient a;
    ArrayList messages;

    float visibility;

    public Console(NetMuseClient a, int x, int y, int w, int h) {
            time = new Timer(4000, a);
            visibility = 0;
            font = a.loadFont("SegoeUI-48.vlw");
            this.a = a;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            fsize = a.screenHeight / 20;
            messages = new ArrayList();
            color = a.color(255, 255, 255);
        }
        /**
         * print a black line of text
         * @param s
         */
    public void println(String s) {
        messages.add(new ConsoleMessage(s, a.color(0, 0, 0, 255)));
        visibility = 1.0f;
        time.start();
    }

    /**
     * print a colored line of text
     * @param s
     * @param color
     */
    public void println(String s, int color) {
        messages.add(new ConsoleMessage(s, color));
        visibility = 1.0f;
        time.start();
    }

    /**
     * draw the console
     */
    public void draw() {

        if (time.isFinished()) {
            if (visibility >= 0)
                visibility *= 0.9999999;
        }


        a.textFont(font, 20);

        for (int i = messages.size() - 1; i > 0; i--) {
            ConsoleMessage m = (ConsoleMessage) messages.get(i);
            a.fill(a.red(m.color), a.green(m.color), a.blue(m.color), (float)(255 - (messages.size() - i) * 20) * visibility);

            a.text(m.message, 20, y + h - (messages.size() - i) * 20);



        }


    }

    /**
     * parses and processes console commands.
     * In some cases it might add messages to the console.
     * @param text
     */
    public void processCommand(String text) {

        if (text.startsWith("/")) {
            String[] command;
            command = text.split(" ");

            if (command[0].equals("/ping")) {
                if (command.length < 2)
                    noSuchCommandError();
                else {
                    if (command[1].equals("-server"))
                        a.sendPingMessage();
                    else
                    if (command[1].equals("-all"))
                        a.sendPingAll();
                }

            } else noSuchCommandError();

        }
    }

    /**
     * informs the user that they entered a bad command
     */
    public void noSuchCommandError() {
        println("No such command.", a.color(255, 0, 0));
    }

}