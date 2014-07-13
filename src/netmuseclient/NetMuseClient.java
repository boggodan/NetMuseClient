package netmuseclient;

/* netMUSE - Networked Multi-User Sound Environment
 * Programmed by Bogdan Vera
 * University of York - Music Research Centre
 * Prototype Version
 */

/* This file contains the main Processing applet and the main function. */

import processing.core.PApplet;

import processing.core.PFont;
import processing.core.PImage;
import processing.core.PVector;
import controlP5.*;
import de.bezier.data.sql.*;
import processing.opengl.*;
//import seltar.unzipit.*;
import sun.io.Converters;
import java.awt.Image;



import java.awt.Color;
import java.math.*;
import java.security.*;
import java.util.*;
/**
 netMuseClient
 */

import oscP5.*;
import netP5.*;
import netmuseclient.AudioEntity.EntityParam;
import netmuseclient.Minimap;
import netmuseclient.OscMessageQueue.AddRemoteEntity;
import netmuseclient.OscMessageQueue.UpdateExistingEntity;

import net.beadsproject.beads.*;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Reverb;

import java.net.Socket;
import java.nio.*;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.swing.ButtonModel;

import javazoom.jl.converter.Converter;

import org.apache.tools.ant.taskdefs.Sleep;

import com.sun.org.apache.bcel.internal.generic.IUSHR;
public class NetMuseClient extends PApplet {



    processing.core.PGraphics pg;

    Timer sendUserPositionChangedTimer;
    Boolean IS_AWAITING_AUTHENTICATION = false;
    Boolean IS_AWAITING_REGISTRATION = false;
    //stopwatch used to calculate ping/latency
    Stopwatch pingWatch;
    Timer authenticationTimeOutTimer;
    Timer registrationTimeOutTimer;
    UnZipIt assetDecompressor;


    //END BACKGROUND MENU BLOBS



    String clientVersion = "0.2";
    public Vector audioEntities;
    OscMessageQueue messageQueue;
    //size of the actual world that everything resides in (in arbitrary units, but they're the same as pixels for simplicity)
    int worldWidth = 10000;
    int worldHeight = 10000;

    public float cameraX = 0;
    public float cameraY = 0;

    int screenW;
    int screenH;

    //state vars
    boolean sessionStarted = false;
    boolean sessionUpdating = false;

    Console console;

    PFont usernameFont;

    int numEntitiesSelected = 0;

    boolean drawFunctionSemaphore = false;
    boolean eventHandlerSemaphore = false;
    ControllerGroup authenticationGroup;
    ControllerGroup registerGroup;
    ControllerGroup entitiesMenu;

    OscP5 oscP5;
    NetAddress myRemoteLocation;
    ControlP5 cp5;
    MySQL msql;
    int userRegisterColor = 0;
    public UserDetails userDetails;
    Timer timer;
    PImage userPointer;
    Vector otherUsers;
    AudioContext ac;

    net.beadsproject.beads.ugens.Reverb outReverb;
    Gain outGain;


    SampleBank sampleBank;

    //is the user currently in the process of placing an entity?
    boolean IS_PLACING = false;
    //is the user currently in the process of cloning one or several entities?
    boolean IS_CLONING = false;
    //is the user currently dragging a selection box over some objects
    boolean IS_SELECTING = false;
    //selection box origin
    PVector selectOrigin;
    //a vector of evil clones! 
    Vector cloningVector;
    //what entity is currently being placed
    String ghostEntity;
    //used when clicking controls, to prevent the program from registering a click twice
    boolean ignoreMouseClick;

    //things relevant to the context menu
    //the id of the entity currently showing its context menu
    String contextEntityID = "";
    //pointer to the entity currently showing its context menu, for easy access
    AudioEntity currentContextEntity;

    //entities in a multiselection
    Vector currentSelectedEntities;

    boolean IS_CONTEXT_VISIBLE = false;
    //is the group selection context menu visible?
    boolean IS_GROUP_CONTEXT_VISIBLE = false;

    //things relevant to moving objects around
    boolean IS_MOVING_ENTITY = false;
    Vector currentMovingEntities;

    Textfield chatBox;
    PImage glow;

    PImage lockIcon;


    //minimap
    Minimap miniMap;
    //minimap size relative to the screen
    int minimapW, minimapH;

    //landscape matrix
    LandscapeMatrix landscapeMatrix;

    Sprite animConnecting;
    Sprite animRegistering;


    public static void main(String args[]) {
        PApplet.main(new String[] {
            netmuseclient.NetMuseClient.class.getName()
        });
    }

    //not used
    public void timingThread() {

    }

    /**
     * processes the internal logic of entities, and is run on a dedicated thread.
     */
    public void entityProcessThread() {
        while (true) {
            Vector wavefrontRemainder; //stores wavefronts to be processed last, so we don't have to search the vector twice
            wavefrontRemainder = new Vector();
            synchronized(audioEntities) {
                //process all non-wavefronts, put all wavefronts into another list to process afterwards
                for (int i = 0; i < audioEntities.size(); i++) {

                    AudioEntity entity = (AudioEntity) audioEntities.get(i);

                    if (entity.getClass().getName().equals("netmuseclient.Wavefront")) {
                        wavefrontRemainder.add(entity);
                    } else {
                        entity.process();
                    }

                }

                try {
                    for (int i = 0; i < wavefrontRemainder.size(); i++) {
                        Wavefront entity = (Wavefront) wavefrontRemainder.get(i);
                        entity.process();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }


            }
            delay(5);
        }
    }

    PImage titleGfx;
    PImage titleBackground;
    PImage reticulatingGfx;


    //camera movement vectors, for smooth camera movement
    PVector camVec;
    //camera properties

    float maxCameraSpeed = 20.0f;
    float cameraAccel = 1.0f;
    float cameraDrag = 0.9f;

    /**
     * PApplet setup method.
     */
    public void setup() {

        screenW = 1280;
        screenH = 800;
        size(1280, 800, OPENGL);
        frameRate(100);

        animConnecting = new Sprite("Sprites/animConnecting.zip", "animConnecting", this, 31);
        authenticationTimeOutTimer = new Timer(20000, this);
        registrationTimeOutTimer = new Timer(20000, this);
        sampleBank = new SampleBank(this);
        sampleBank.addSample("kick1.wav", "Kick 1");
        sampleBank.addSample("hat.wav", "Hat");
        sampleBank.addSample("snare.wav", "Snare");
        sampleBank.addSample("click1.wav", "Click 1");
        sampleBank.addSample("click2.wav", "Click 2");
        sampleBank.addSample("heavykick.wav", "Heavy Kick");
        sampleBank.addSample("microtonal1.wav", "Microtonal");
        sampleBank.addSample("birdies.wav", "Birdies");
        sampleBank.addSample("cthulhu.wav", "Cthulhu");
        sampleBank.addSample("clangboop.wav", "Clangboop");

        sampleBank.addSample("fire.wav", "Fire Crackle");
        sampleBank.addSample("seashore.wav", "Sea Shore");
        sampleBank.addSample("orchtune.wav", "Tuning Orchestra");
        camVec = new PVector(0.0f, 0.0f);

        pg = createGraphics(160, 90, P2D);

        currentMovingEntities = new Vector();
        currentSelectedEntities = new Vector();
        titleGfx = loadImage("netMuseTitle.png");
        titleGfx.resize(screenW / 3, screenH / 4);
        reticulatingGfx = loadImage("reticulatingSplines.png");
        titleBackground = loadImage("menubackground.png");


        ignoreMouseClick = false;
        IS_PLACING = false;

        usernameFont = loadFont("Consolas-Bold-30.vlw");

        glow = loadImage("white-glow.png");
        glow.resize(60, 60);

        lockIcon = loadImage("lockIcon.png");

        userPointer = loadImage("TrollFace.png");
        userPointer.resize(50, 50);

        //list of controller ids are readable names
        messageQueue = new OscMessageQueue(this);

        cp5 = new ControlP5(this);
        authenticationGroup = new ControllerGroup(true, screenW / 2 - 100, screenH / 3, cp5);
        authenticationGroup.addControllerTextBox("loginUsername", "Username", 10, 10, 200, 50);
        authenticationGroup.addControllerTextBox("loginPassword", "Password", 10, 80, 200, 50);
        authenticationGroup.addControllerTextBox("serverIP", "Server IP", 10, 160, 200, 50);
        authenticationGroup.addControllerButton("loginOk", "Login", 10, 240, 80, 50);
        authenticationGroup.addControllerButton("loginNewUser", "New User", 120, 240, 80, 50);

        registerGroup = new ControllerGroup(true, screenW / 2 - 100, screenH / 3, cp5);
        registerGroup.addControllerTextBox("registerUsername", "Username", 10, 10, 200, 50);
        registerGroup.addControllerTextBox("registerPassword", "Password", 10, 80, 200, 50);
        registerGroup.addControllerSlider("registerColor", "Your Color", 10, 150, 200, 50);
        registerGroup.addControllerButton("registerOk", "Register", 10, 220, 80, 50);
        registerGroup.addControllerButton("registerCancel", "Cancel", 120, 220, 80, 50);

        entitiesMenu = new ControllerGroup(true, width - 200, 0, cp5);
        entitiesMenu.addControllerButton("menu_Grain", "Grain", 0, 0, 200, 50);
        entitiesMenu.addControllerButton("menu_Bloop", "Bloop", 0, 50, 200, 50);
        entitiesMenu.addControllerButton("menu_Sample", "Sample", 0, 100, 200, 50);
        entitiesMenu.addControllerButton("menu_Wavefront", "Wavefront", 0, 150, 200, 50);
        entitiesMenu.addControllerButton("menu_Cancel", "Cancel", 0, 200, 200, 50);
        entitiesMenu.hide();
        cp5.addButton("showEntitiesMenu").setPosition(width - 200, 0)
            .setSize(200, 50).setLabel("Show Palette")
            .setVisible(false);


        registerGroup.hide();
        myRemoteLocation = new NetAddress("78.105.196.223", 8000);
        otherUsers = new Vector();

        float val = cp5.get(Slider.class, "registerColor").getValue();
        colorMode(HSB, 100);
        userRegisterColor = color(val * 10.0f, 100, 100);

        (cp5.get(Slider.class, "registerColor")).setColorBackground(color(val * 10.0f, 100, 100));
        colorMode(RGB, 255);



        chatBox = cp5.addTextfield("chatBox", 50, height - 50, width - 100, 30).setAutoClear(false).setCaptionLabel("Chat");
        chatBox.setFont(ControlP5.grixel);
        chatBox.setColorBackground(color(0, 100, 100, 255));
        chatBox.hide();




        timer = new Timer(3000, this);
        timer.start();

        console = new Console(this, 0, height - 200, screenW, 200);



        // AudioFormat af = new AudioFormat(44100,16,1,true,true);
        ac = new AudioContext();
        pingWatch = new Stopwatch(this);


        outGain = new Gain(ac, 1, 0.6f);


        ac.out.addInput(outGain);


        //init entities vector
        audioEntities = new Vector();

        //init cloning vector
        cloningVector = new Vector();

        minimapW = screenW / 5;
        minimapH = screenW / 5; // has to be square so just use the width
        miniMap = new Minimap(this, screenW - minimapW, screenH - minimapH, minimapW, minimapH);

        oscP5 = new OscP5(this, (int) random(1000) + 10000);
        oscP5.properties().setSRSP(true);

        sendUserPositionChangedTimer = new Timer(100, this);
        landscapeMatrix = new LandscapeMatrix(this);

        //start entity logic thread
        thread("entityProcessThread");
        console.println("", color(0, 0, 0));
        console.println("netMUSE Client has started successfully!", color(0, 0, 0));

    }

    /**
     * Starts the AudioContext and displays a starting message in the console.
     * Also makes the entity menu visible.
     */
    void doneUpdating() {
        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(true);
        console.println("DEBUG: success! synced with server.");
        console.println("This client version is: " + clientVersion);
        ac.start();
    }

    boolean[] keys = new boolean[526];
    int menuTint = 0;

    /**
     * Draw function.
     */
    public void draw() {
        console.draw();

        //println(sessionUpdating);
        try {
            drawFunctionSemaphore = true;
            ellipseMode(CENTER);

            messageQueue.process();
            messageQueue.queue.clear();

            if (!sessionStarted) {

                image(titleBackground, 0, 0);

                if (IS_AWAITING_AUTHENTICATION) {
                    animConnecting.drawSprite(authenticationGroup.x + 10, authenticationGroup.y + 100);
                    if (authenticationTimeOutTimer.isFinished()) {
                        IS_AWAITING_AUTHENTICATION = false;
                        authenticationTimeOutTimer = new Timer(20000, this);
                        console.println("Authentication failed.");
                        authenticationGroup.show();
                    }
                }

                if (IS_AWAITING_REGISTRATION) {
                    animConnecting.drawSprite(authenticationGroup.x + 10, authenticationGroup.y + 100);
                    if (registrationTimeOutTimer.isFinished()) {
                        IS_AWAITING_REGISTRATION = false;
                        registrationTimeOutTimer = new Timer(20000, this);
                        console.println("Registration failed.");
                        registerGroup.show();
                    }
                }


            } else
            if (sessionStarted && sessionUpdating) {
                //send keep alive message
                if (timer.isFinished()) {
                    println("sent keep alive");
                    sendKeepAlive();
                    //console.println("Sent keep alive message");
                    timer.start();
                }
                background(0);
                image(reticulatingGfx, 0, 0);

            } else if (sessionStarted && (!sessionUpdating)) {


                //send keep alive message
                if (timer.isFinished()) {
                    sendKeepAlive();
                    //console.println("Sent keep alive message");
                    timer.start();
                }

                background(240, 255, 255);
                landscapeMatrix.drawLandscape();


                userDetails.posx = mouseX + (int) cameraX;
                userDetails.posy = mouseY + (int) cameraY;


                //draw objects
                numEntitiesSelected = 0;
                //REMEMBER THAT WAVEFRONTS MUST BE PROCESSED LAST TO ACCUMULATE INFLUENCES!
                Vector wavefrontRemainder; //stores wavefronts to be processed last, so we don't have to search the vector twice
                wavefrontRemainder = new Vector();
                //THIS LOOP PERFORMS CULLING TOO
                synchronized(audioEntities) {
                    //process all non-wavefronts, put all wavefronts into another list to process afterwards
                    currentSelectedEntities.clear();
                    for (int i = 0; i < audioEntities.size(); i++) {

                        AudioEntity entity = (AudioEntity) audioEntities.get(i);
                        if (entity.multiSelected) {
                            if (entity.getIsMine()) {
                                currentSelectedEntities.add(entity);
                                numEntitiesSelected++;
                            }
                        }

                        if (entity.getClass().getName().equals("netmuseclient.Wavefront")) {
                            wavefrontRemainder.add(entity);
                        } else {

                            //culling
                            if (entity.attenVal <= 0.0051) {
                                if (!entity.culled)
                                    entity.cull(true);
                            } else {
                                if (entity.culled)
                                    entity.cull(false);
                            }

                            if (!entity.culled)
                                entity.draw((int) cameraX, (int) cameraY);

                        }

                    }

                    try {

                        //now process the wavefronts
                        for (int i = 0; i < wavefrontRemainder.size(); i++) {
                            Wavefront entity = (Wavefront) wavefrontRemainder.get(i);
                            //culling
                            if (entity.attenVal <= 0.0051) {
                                if (!entity.culled)
                                    entity.cull(true);
                            } else {
                                if (entity.culled)
                                    entity.cull(false);
                            }

                            if (!entity.culled)
                                entity.draw((int) cameraX, (int) cameraY);

                            // entity.handleTimer();

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }


                }

                for (int i = 0; i < otherUsers.size(); i++) {

                    UserDetails u = (UserDetails) otherUsers.get(i);

                    u.drawx = (int)(u.xLP.Process((float) u.posx));
                    u.drawy = (int)(u.yLP.Process((float) u.posy));

                    tint(red(u.userColor), green(u.userColor), blue(u.userColor), 100);
                    image(userPointer, u.drawx - cameraX, u.drawy - cameraY);
                    noTint();
                    //println("The other user's absolute position is: " + u.posx);
                    fill(0, 0, 0, 200);
                    textFont(usernameFont, 15);
                    text(u.username, u.drawx - cameraX, u.drawy - cameraY);

                }

                if (sendUserPositionChangedTimer.isFinished())
                    sendPositionChanged(userDetails.posx, userDetails.posy);

                if (sendUserPositionChangedTimer.isFinished())
                    sendUserPositionChangedTimer.start();



                //draw ghost entity
                if (IS_PLACING) {

                    textFont(usernameFont, 15);

                    fill(255, 20, 20, 200);
                    rect(mouseX - 10, mouseY - 10, 20, 20);
                    fill(0, 0, 0, 200);
                    text(ghostEntity, mouseX + 10, mouseY + 10);
                }

                if (IS_CLONING) {

                    for (int i = 0; i < cloningVector.size(); i++) {
                        AudioEntity clone = (AudioEntity) cloningVector.get(i);
                        textFont(usernameFont, 15);
                        fill(255, 20, 20, 200);
                        rect(mouseX + clone.cloningX - 10, mouseY + clone.cloningY - 10, 20, 20);
                        fill(0, 0, 0, 200);
                        text("Clone", mouseX + clone.cloningX - 10, mouseY + clone.cloningY - 10);
                    }
                }

                //if the user is dragging a selection box
                //selection box draw
                if (IS_SELECTING) {
                    strokeWeight(1.0f);
                    stroke(0, 0, 0, 200);
                    fill(0, 0, 0, 40);

                    //determine the top left and bottom right PVectors so we can handle selection
                    PVector topLeft, bottomRight;
                    topLeft = new PVector(0, 0);
                    bottomRight = new PVector(0, 0);
                    if (mouseX < selectOrigin.x && mouseY < selectOrigin.y) {
                        topLeft = new PVector(mouseX, mouseY);
                        bottomRight = new PVector(selectOrigin.x, selectOrigin.y);
                        rect(mouseX, mouseY, selectOrigin.x - mouseX, selectOrigin.y - mouseY);
                    }

                    if (mouseX < selectOrigin.x && mouseY > selectOrigin.y) {
                        topLeft = new PVector(mouseX, selectOrigin.y, selectOrigin.x - mouseX);
                        bottomRight = new PVector(mouseX + selectOrigin.x - mouseX, selectOrigin.y + mouseY - selectOrigin.y);

                        rect(mouseX, selectOrigin.y, selectOrigin.x - mouseX, mouseY - selectOrigin.y);
                    }

                    if (mouseX > selectOrigin.x && mouseY < selectOrigin.y) {
                        topLeft = new PVector(selectOrigin.x, mouseY);
                        bottomRight = new PVector(selectOrigin.x + mouseX - selectOrigin.x, mouseY + selectOrigin.y - mouseY);

                        rect(selectOrigin.x, mouseY, mouseX - selectOrigin.x, selectOrigin.y - mouseY);
                    }

                    if (mouseX > selectOrigin.x && mouseY > selectOrigin.y) {
                        topLeft = new PVector(selectOrigin.x, selectOrigin.y);
                        bottomRight = new PVector(selectOrigin.x + mouseX - selectOrigin.x, selectOrigin.y + mouseY - selectOrigin.y);

                        rect(selectOrigin.x, selectOrigin.y, mouseX - selectOrigin.x, mouseY - selectOrigin.y);
                    }

                    for (int i = 0; i < audioEntities.size(); i++) {
                        AudioEntity e = (AudioEntity) audioEntities.get(i);

                        if (e.drawx > topLeft.x && e.drawx < bottomRight.x && e.drawy > topLeft.y && e.drawy < bottomRight.y) {
                            if (e.getIsMine())
                                e.multiSelected = true;
                        } else
                            e.multiSelected = false;


                    }

                }

                if (IS_CHOOSING_WF_TARGET) {
                    float wx, wy;
                    wx = currentOriginWavefront.x - cameraX;
                    wy = currentOriginWavefront.y - cameraY;

                    stroke(0, 0, 0);
                    strokeWeight(1.0f);

                    line(wx, wy, mouseX, mouseY);

                }

                miniMap.draw();



                if (mousePressed && miniMap.getIsMouseOver()) {

                    cameraX = (((float) mouseX - (float) miniMap.x - miniMap.rw / 2) / (float) miniMap.w) * (float) worldWidth;
                    cameraY = (((float) mouseY - (float) miniMap.y - miniMap.rh / 2) / (float) miniMap.h) * (float) worldHeight;
                }


            }




            console.draw();


            //camera scrolling via arrow keys if the camera is in the worldspace

            if (keys[RIGHT]) {

                camVec.x += cameraAccel;
                if (camVec.x > maxCameraSpeed)
                    camVec.x = maxCameraSpeed;
            } else
            if (keys[LEFT]) {
                camVec.x -= cameraAccel;
                if (camVec.x < -maxCameraSpeed)
                    camVec.x = -maxCameraSpeed;
            }

            if (keys[UP]) {
                camVec.y -= cameraAccel;
                if (camVec.y < -maxCameraSpeed)
                    camVec.y = -maxCameraSpeed;
            } else
            if (keys[DOWN]) {
                camVec.y += cameraAccel;
                if (camVec.y > maxCameraSpeed)
                    camVec.y = maxCameraSpeed;
            }

            cameraX += camVec.x;
            cameraY += camVec.y;

            if (cameraX < 0) {
                camVec.x = 0.0f;
                cameraX = 0.0f;
            }

            if (cameraY < 0) {
                camVec.y = 0.0f;
                cameraY = 0.0f;
            }

            if (cameraX + screenW > worldWidth) {
                camVec.x = 0.0f;
                cameraX = worldWidth - screenW;
            }

            if (cameraY + screenH > worldHeight) {
                camVec.y = 0.0f;
                cameraY = worldHeight - screenH;
            }


            camVec.x *= cameraDrag;
            camVec.y *= cameraDrag;

            drawFunctionSemaphore = false;
        } catch (Exception ex) {
            console.println("ERROR: Exception in render process. Details:");
            console.println(ex.getMessage());
        }




    }

    /**
     * The user pressed a key down. This only handles the enter key for now,
     * which is used in the chatting box.
     */
    @
    Override
    public void keyPressed() {

        //println(keyCode);

        if (keyCode == 10) {
            println("pressed enter");
            if (sessionStarted) {
                if (!chatBox.isVisible()) {
                    chatBox.show();
                    chatBox.setFocus(true);
                } else {
                    //first of all, figure out if this is a command message
                    String message = chatBox.getText();
                    if (message.startsWith("/")) {
                        console.processCommand(message);
                    } else
                        sendChatMessage(message);
                    chatBox.hide();
                    chatBox.clear();
                }
            }



        }

        keys[keyCode] = true;

    }

    @
    Override
    public void keyReleased() {
        keys[keyCode] = false;
    }

    @
    Override
    public void mouseMoved() {
        try {
            if (sessionStarted) {



                super.mouseMoved();
            }
        } catch (Exception ex) {
            console.println("ERROR: Exception in mouse event process. Details:");
            console.println(ex.getMessage());
        }
    }

    @
    Override
    public void mouseReleased() {
        if (sessionStarted) {
            if (mouseButton == LEFT) {
                if (IS_SELECTING)
                    IS_SELECTING = false;
            }
        }
    }

    /**
     * Main mouse handler.
     */
    @
    Override
    public void mousePressed() {
        if (!miniMap.getIsMouseOver()) {
            try {
                if (sessionStarted) {
                    if (mouseButton == LEFT) {

                        if (IS_CHOOSING_WF_TARGET) {
                            //did user click on an entity?
                            for (int i = 0; i < audioEntities.size(); i++) {
                                AudioEntity e = (AudioEntity) audioEntities.get(i);
                                float mx = mouseX;
                                float my = mouseY;
                                //distance to centre of object
                                float d = dist(mx, my, (float) e.drawx, (float) e.drawy);

                                if (d < e.ch / 2) {
                                    if (e.type.equals("Wavefront")) {
                                        setWavefrontTarget((Wavefront) e);
                                        console.println("Wavefronts linked!");
                                        break;
                                    }
                                }
                            }
                            stopWavefrontTargetChoice();
                        }



                        if (!entitiesMenu.getIsMouseOver()) {
                            if (IS_MOVING_ENTITY) {
                                IS_MOVING_ENTITY = false;
                                for (int i = 0; i < currentMovingEntities.size(); i++) {
                                    AudioEntity a = (AudioEntity) currentMovingEntities.get(i);
                                    a.stopMoving();
                                    a.stopMultiMoving(mouseX, mouseY);
                                }
                                currentMovingEntities.clear();
                            }

                            if (!IS_PLACING && !IS_CLONING && !IS_MOVING_ENTITY) {

                                //don't start selecting if we're over a context menu
                                //remember that even the group context menu is technically one entity's menu

                                if (IS_CONTEXT_VISIBLE || IS_GROUP_CONTEXT_VISIBLE) {
                                    if (!currentContextEntity.getIsOverControllers()) {
                                        IS_SELECTING = true;
                                        selectOrigin = new PVector(mouseX, mouseY);
                                    }
                                } else {
                                    IS_SELECTING = true;
                                    selectOrigin = new PVector(mouseX, mouseY);
                                }
                            }


                            if (IS_PLACING) {
                                IS_PLACING = false;

                                addNewEntity(ghostEntity, mouseX, mouseY);
                            }

                            if (IS_CLONING) {
                                try {
                                    endCloning();
                                    //clone the entity... we only support cloning one at the time at the moment, so it will be the first in the vector
                                    for (int i = 0; i < cloningVector.size(); i++) {
                                        AudioEntity originalEntity = (AudioEntity) cloningVector.get(i);

                                        addClone(originalEntity, mouseX + originalEntity.cloningX, mouseY + originalEntity.cloningY);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                cloningVector.clear();
                            }
                        }

                        //hide all context menus if user clicked outside the active one
                        if (IS_CONTEXT_VISIBLE)
                            if (!currentContextEntity.getIsOverControllers()) {

                                for (int i = 0; i < audioEntities.size(); i++) {
                                    AudioEntity a = (AudioEntity) audioEntities.get(i);
                                    a.hideContextControls();

                                }

                                IS_CONTEXT_VISIBLE = false;
                            }

                        if (IS_GROUP_CONTEXT_VISIBLE)
                            if (!currentContextEntity.getIsOverControllers()) {

                                for (int i = 0; i < audioEntities.size(); i++) {
                                    AudioEntity a = (AudioEntity) audioEntities.get(i);
                                    a.hideContextControls();

                                }

                                IS_GROUP_CONTEXT_VISIBLE = false;
                            }

                    } else if (mouseButton == RIGHT) {

                        stopWavefrontTargetChoice();

                        //only handle context menu if the user is not currently cloning, placing or moving an entity
                        if (!IS_CLONING && !IS_PLACING && !IS_MOVING_ENTITY) {
                            //context menu handling
                            for (int i = 0; i < audioEntities.size(); i++) {
                                AudioEntity a = (AudioEntity) audioEntities.get(i);
                                //hide all other context controls
                                a.hideContextControls();
                                ///IS_CONTEXT_VISIBLE = false;
                                //IS_GROUP_CONTEXT_VISIBLE = false;
                                //show the context control for the control we right clicked on
                                float mx = mouseX;
                                float my = mouseY;
                                //distance to centre of object
                                float d = dist(mx, my, (float) a.drawx, (float) a.drawy);

                                if (d < a.ch / 2) {
                                    //if theres' only one entity selected, show the one entity context menu

                                    if (a.multiSelected && numEntitiesSelected > 1) {


                                        println("show context menu!");
                                        //only show the common options (move,remove,etc)
                                        a.showGroupContextControls();

                                        //we won't use the context entity, but we have to know which one is showing the menu
                                        currentContextEntity = a;
                                        IS_GROUP_CONTEXT_VISIBLE = true;
                                        IS_CONTEXT_VISIBLE = false;
                                    } else {
                                        //only show context menu if the entity is this user's
                                        if (a.getIsMine()) {
                                            a.showContextControls();
                                            IS_CONTEXT_VISIBLE = true;
                                            IS_GROUP_CONTEXT_VISIBLE = false;
                                            currentContextEntity = a;
                                        }
                                    }


                                }



                            }
                        }
                    }
                }
            } catch (Exception ex) {
                console.println("ERROR: Exception in mouse click process. Details:");
                console.println(ex.getMessage());
            }
        }
    }

    /**
     * Turns IS_CLONING on so the program knows the user is currently cloning one or more entities.
     */
    public void startCloning() {
        IS_CLONING = true;
    }

    /**
     * Turns IS_CLONING off.
     */
    public void endCloning() {
        IS_CLONING = false;
    }

    /**
     * Adds an entity to the audioEntities list based on a clone, by copying its parameters.
     * Also informs the server of the entity, it's position and its parameters.
     * @param originalEntity
     * @param x
     * @param y
     */
    public void addClone(AudioEntity originalEntity, int x, int y) {
        AudioEntity clonedEntity = null;
        //create the entity
        if (originalEntity instanceof Grain) {
            clonedEntity = addNewEntity("Grain", x, y);
        }
        if (originalEntity instanceof Bloop) {
            clonedEntity = addNewEntity("Bloop", x, y);
        }
        if (originalEntity instanceof Sample) {
            clonedEntity = addNewEntity("Sample", x, y);
        }
        if (originalEntity instanceof Wavefront) {
            clonedEntity = addNewEntity("Wavefront", x, y);
        }

        //copy the parameters of the old entity into the new one
        Vector clonedParams = originalEntity.getClonedParameters();
        for (int i = 0; i < clonedParams.size(); i++) {
            EntityParam param = (EntityParam) clonedParams.get(i);
            println("cloned parameter: " + param.name + " with value: " + param.value);
            //just use remote param change, it will do just fine
            if (param.type.equals("param")) {
                clonedEntity.remoteParamChange(param.name, param.value, 0);
                clonedEntity.sendOscParamChange(oscP5, myRemoteLocation, userDetails.username, param.name, clonedEntity.identifier, param.value);
            }
            if (param.type.equals("group")) {
                clonedEntity.remoteGroupHandler(param.name, param.value);
                sendGroupControlMessage(clonedEntity.identifier, param.name, param.value);
            }

        }
    }

    /**
     * Adds a new entity and informs the server.
     * @param type
     * @param x
     * @param y
     * @return
     */
    public AudioEntity addNewEntity(String type, int x, int y) {
        if (type.equals("Grain")) {
            Grain h = new Grain(this, cp5, ac, x + (int) cameraX, y + (int) cameraY);
            h.setOwner(userDetails.username);
            audioEntities.add(h);
            outGain.addInput(h.getOutput());
            //console.println(userDetails.username + " added a Wavefront entity.");

            sendNewAudioEntity("Grain", h.identifier, x + (int) cameraX, y + (int) cameraY);
            //handle wavefront snapping
            //we do this here instead of the constructor, so handleSnapWavefronts isn't called when an object is added remotely unless there is
            //an explicit snap message
            h.handleSnapWavefronts();

            return h;
        }

        if (type.equals("Bloop")) {
            Bloop h = new Bloop(this, cp5, ac, x + (int) cameraX, y + (int) cameraY);
            h.setOwner(userDetails.username);
            audioEntities.add(h);
            //console.println(userDetails.username + " added a Bloop entity.");
            outGain.addInput(h.getOutput());

            sendNewAudioEntity("Bloop", h.identifier, x + (int) cameraX, y + (int) cameraY);
            //handle wavefront snapping
            //we do this here instead of the constructor, so handleSnapWavefronts isn't called when an object is added remotely unless there is
            //an explicit snap message
            h.handleSnapWavefronts();

            return h;
        }

        if (type.equals("Wavefront")) {
            Wavefront h = new Wavefront(this, cp5, ac, x + (int) cameraX, y + (int) cameraY);
            h.setOwner(userDetails.username);
            audioEntities.add(h);

            sendNewAudioEntity("Wavefront", h.identifier, x + (int) cameraX, y + (int) cameraY);

            return h;
        }

        if (type.equals("Sample")) {
            Sample h = new Sample(this, cp5, ac, x + (int) cameraX, y + (int) cameraY);
            h.setOwner(userDetails.username);
            audioEntities.add(h);
            outGain.addInput(h.getOutput());

            sendNewAudioEntity("Sample", h.identifier, x + (int) cameraX, y + (int) cameraY);
            //handle wavefront snapping
            //we do this here instead of the constructor, so handleSnapWavefronts isn't called when an object is added remotely unless there is
            //an explicit snap message
            h.handleSnapWavefronts();

            return h;
        }




        return null;

    }


    /**
     * ControlP5 GUI event handler.
     * @param theEvent
     */
    public void controlEvent(ControlEvent theEvent) {

        try {
            eventHandlerSemaphore = true;
            if (!theEvent.isGroup()) {
                String ctlName = theEvent.getController().getName();

                if (ctlName.equals("loginOk")) {

                    String usr = cp5.get(Textfield.class, "loginUsername").getText();
                    String pwd = cp5.get(Textfield.class, "loginPassword").getText();
                    myRemoteLocation = new NetAddress(cp5.get(Textfield.class, "serverIP").getText(), 8000);
                    if ((!usr.equals("")) && (!pwd.equals("")) && !myRemoteLocation.address().equals("")) {
                        sendAuthenticationRequest(usr, pwd);

                        IS_AWAITING_AUTHENTICATION = true;
                        authenticationGroup.hide();
                        authenticationTimeOutTimer.start();
                    }


                    //access the database, check out the server address, then check authentication

                }

                if (ctlName.equals("registerOk")) {
                    String usr = cp5.get(Textfield.class, "registerUsername").getText();
                    String pwd = cp5.get(Textfield.class, "registerPassword").getText();

                    if ((!usr.equals("")) && (!pwd.equals(""))) {
                        sendRegistrationRequest(usr, pwd);
                    }
                    IS_AWAITING_REGISTRATION = true;
                    registerGroup.hide();
                    registrationTimeOutTimer.start();
                }

                if (ctlName.equals("loginNewUser")) {
                    authenticationGroup.hide();
                    registerGroup.show();
                }

                if (ctlName.equals("registerColor")) {
                    float val = cp5.get(Slider.class, "registerColor").getValue();
                    colorMode(HSB, 100);
                    userRegisterColor = color(val * 10.0f, 100, 100);

                    (cp5.get(Slider.class, "registerColor")).setColorBackground(color(val * 10.0f, 100, 100));
                }

                if (ctlName.equals("registerCancel")) {
                    authenticationGroup.show();
                    registerGroup.hide();
                }
                //only do the menu stuff if the user is not placing, cloning or moving
                if (!IS_CLONING && !IS_PLACING && !IS_MOVING_ENTITY) {
                    if (ctlName.equals("showEntitiesMenu")) {
                        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(false);
                        entitiesMenu.show();
                    }
                    if (ctlName.equals("menu_Cancel")) {
                        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(true);
                        entitiesMenu.hide();
                    }

                    if (ctlName.equals("menu_Grain")) {

                        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(true);
                        entitiesMenu.hide();


                        IS_PLACING = true;
                        ignoreMouseClick = true;
                        ghostEntity = "Grain";

                    }

                    if (ctlName.equals("menu_Bloop")) {

                        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(true);
                        entitiesMenu.hide();



                        IS_PLACING = true;
                        ignoreMouseClick = true;
                        ghostEntity = "Bloop";
                    }

                    if (ctlName.equals("menu_Wavefront")) {

                        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(true);
                        entitiesMenu.hide();



                        IS_PLACING = true;
                        ignoreMouseClick = true;
                        ghostEntity = "Wavefront";
                    }

                    if (ctlName.equals("menu_Sample")) {

                        (cp5.get(Button.class, "showEntitiesMenu")).setVisible(true);
                        entitiesMenu.hide();



                        IS_PLACING = true;
                        ignoreMouseClick = true;
                        ghostEntity = "Sample";
                    }
                }
                /*CONTEXT MENU HANDLERS
                 * these handlers are dynamic, and based on the
                 * entity currently showing its context menu.
                 */
                if (IS_CONTEXT_VISIBLE && !IS_GROUP_CONTEXT_VISIBLE) {
                    if (ctlName.equals(currentContextEntity.moveButton.name())) {
                        //console.println("MOVING!");
                        currentContextEntity.startMoving();
                        IS_MOVING_ENTITY = true;
                        currentMovingEntities.clear();
                        currentMovingEntities.add(currentContextEntity);
                    } else
                    if (ctlName.equals(currentContextEntity.cloneButton.name())) {


                        //console.println("MOVING!");
                        startCloning();
                        cloningVector.clear();

                        currentContextEntity.cloningX = currentContextEntity.drawx - mouseX;
                        currentContextEntity.cloningY = currentContextEntity.drawy - mouseY;

                        cloningVector.add(currentContextEntity);
                        currentContextEntity.hideContextControls();
                        currentContextEntity = null;
                        IS_CONTEXT_VISIBLE = false;

                    } else
                    if (ctlName.equals(currentContextEntity.removeButton.name())) {

                        currentContextEntity.hideContextControls();
                        sendRemoveAudioEntity(currentContextEntity.identifier);
                        currentContextEntity.outGain.kill();
                        audioEntities.remove(currentContextEntity);

                        currentContextEntity = null;
                        IS_CONTEXT_VISIBLE = false;
                        System.gc();

                    } else
                    if (ctlName.equals(currentContextEntity.lockButton.name())) {

                        currentContextEntity.hideContextControls();


                        currentContextEntity.lockUser = userDetails.username;
                        currentContextEntity.lock();
                        sendLockMessage(currentContextEntity.identifier);
                        currentContextEntity = null;
                        IS_CONTEXT_VISIBLE = false;


                    } else
                    if (ctlName.equals(currentContextEntity.unlockButton.name())) {

                        currentContextEntity.hideContextControls();

                        currentContextEntity.unlock();
                        sendUnlockMessage(currentContextEntity.identifier);
                        currentContextEntity = null;
                        IS_CONTEXT_VISIBLE = false;


                    } else {

                        //handle current context entity parameter changes automatically

                        float value = currentContextEntity.paramHandler(ctlName);
                        //we have to translate this controller name, because the controllers have random names that do not
                        //correspond between users. We have to see what paramName this is.
                        String paramName = currentContextEntity.translateParamName(ctlName);


                        currentContextEntity.sendOscParamChange(oscP5, myRemoteLocation, userDetails.username, paramName, currentContextEntity.identifier, value);


                    }
                } else if (IS_GROUP_CONTEXT_VISIBLE && !IS_CONTEXT_VISIBLE) {
                    if (ctlName.equals(currentContextEntity.removeButton.name())) {
                        println("Remove a bunch of things!");
                        println("Number of entities elected: " + currentSelectedEntities.size());
                        for (int i = 0; i < currentSelectedEntities.size(); i++) {
                            AudioEntity e = (AudioEntity) currentSelectedEntities.get(i);
                            e.hideContextControls();
                            sendRemoveAudioEntity(e.identifier);
                            e.outGain.kill();
                            audioEntities.remove(e);
                            IS_GROUP_CONTEXT_VISIBLE = false;
                        }
                    }

                    if (ctlName.equals(currentContextEntity.moveButton.name())) {
                        currentMovingEntities.clear();
                        //println("Number of entities elected: " + currentSelectedEntities.size());
                        for (int i = 0; i < currentSelectedEntities.size(); i++) {
                            AudioEntity e = (AudioEntity) currentSelectedEntities.get(i);
                            e.hideContextControls();

                            e.startMultiMoving(mouseX, mouseY);
                            IS_MOVING_ENTITY = true;

                            currentMovingEntities.add(e);

                            IS_GROUP_CONTEXT_VISIBLE = false;
                        }


                    }

                    if (ctlName.equals(currentContextEntity.cloneButton.name())) {


                        //console.println("MOVING!");
                        startCloning();
                        cloningVector.clear();

                        for (int i = 0; i < currentSelectedEntities.size(); i++) {
                            AudioEntity e = (AudioEntity) currentSelectedEntities.get(i);
                            e.cloningX = e.drawx - mouseX;
                            e.cloningY = e.drawy - mouseY;

                            cloningVector.add(e);
                            e.hideContextControls();
                            e = null;
                            IS_CONTEXT_VISIBLE = false;
                        }


                    }

                    if (ctlName.equals(currentContextEntity.lockButton.name())) {


                        //console.println("MOVING!");
                        startCloning();
                        cloningVector.clear();

                        for (int i = 0; i < currentSelectedEntities.size(); i++) {
                            AudioEntity e = (AudioEntity) currentSelectedEntities.get(i);
                            e.lock();
                            e.lockUser = userDetails.username;
                            sendLockMessage(e.identifier);
                            e.hideContextControls();
                            e = null;
                            IS_CONTEXT_VISIBLE = false;
                        }


                    }
                    if (ctlName.equals(currentContextEntity.unlockButton.name())) {


                        //console.println("MOVING!");
                        startCloning();
                        cloningVector.clear();

                        for (int i = 0; i < currentSelectedEntities.size(); i++) {
                            AudioEntity e = (AudioEntity) currentSelectedEntities.get(i);
                            e.unlock();
                            sendUnlockMessage(e.identifier);

                            e.hideContextControls();
                            e = null;
                            IS_CONTEXT_VISIBLE = false;
                        }


                    }


                }
            } else if (theEvent.isGroup()) {
                if (IS_CONTEXT_VISIBLE) {
                    String groupName = theEvent.group().getName();
                    float value = theEvent.group().getValue();
                    currentContextEntity.groupHandler(groupName, value);

                    sendGroupControlMessage(currentContextEntity.identifier, currentContextEntity.translateGroupHandler(groupName), value);
                }
            }
            eventHandlerSemaphore = false;
        } catch (Exception ex) {
            console.println("ERROR: Exception in OSCP5 ctl handler. Details:");
            console.println(ex.getMessage());
        }
    }


    /**
     * OSC message listener. Puts incoming messages into the message queue.
     * @param theOscMessage
     */
    public void oscEvent(OscMessage theOscMessage) {

        //println(theOscMessage.addrPattern());

        try {

            //wait for draw function if the semaphore is on
            while (drawFunctionSemaphore) {

            }

            /* print the address pattern and the typetag of the received OscMessage */
            if (theOscMessage.addrPattern().equals("/userJoinOK")) {

                /*only handle this if the client is already awaiting authentication
		otherwise this might arrive after the user has been notified of a time out, which might confusing. */

                if (IS_AWAITING_AUTHENTICATION) {
                    messageQueue.add(messageQueue.new UserJoinOK(theOscMessage.get(0).intValue(), theOscMessage.get(1).stringValue()));
                    IS_AWAITING_AUTHENTICATION = false;

                }




            }

            if (theOscMessage.addrPattern().equals("/regSuccess")) {
                /*only handle this if the client is already awaiting authentication
		otherwise this might arrive after the user has been notified of a time out, which might confusing. */

                if (IS_AWAITING_REGISTRATION) {

                    IS_AWAITING_REGISTRATION = false;
                    console.println("Registration successful!", color(0, 0, 0));
                    registerGroup.hide();
                    authenticationGroup.show();
                }

                console.println("Registration successful!", color(0, 0, 0));


            }


            if (theOscMessage.addrPattern().equals("/regFail")) {
                console.println("The server responded with a registration error.", color(0, 0, 0));
            }

            if (theOscMessage.addrPattern().equals("/error_userAlreadyExists")) {

                console.println("Couldn't log in because this username is already logged in");
                console.println("Odds are you've recently crashed from the server and it hasn't purged your username it.");
                console.println("This should occur shortly.");

            }

            if (theOscMessage.addrPattern().equals("/updateComplete")) {
                println("got update finished message.");
                String serverName;
                Vector serverGreeting = new Vector();

                serverName = theOscMessage.get(1).stringValue();
                int numLines = theOscMessage.get(0).intValue();

                for (int i = 0; i < numLines; i++) {
                    serverGreeting.add(theOscMessage.get(2 + i).stringValue());
                }


                messageQueue.add(messageQueue.new UpdateFinishedMessage(serverName, serverGreeting));
                //println("got past update");
            }

            if (theOscMessage.addrPattern().equals("/update_existingUser")) {

                String usr;
                int newx, newy;
                int newcolor;

                usr = theOscMessage.get(0).stringValue();
                newx = theOscMessage.get(1).intValue();
                newy = theOscMessage.get(2).intValue();
                newcolor = theOscMessage.get(3).intValue();

                try {
                    if (!checkUserExists(usr))
                        otherUsers.add(new UserDetails(usr, newx, newy, newcolor, this));
                } catch (ConcurrentModificationException ex) {
                    console.println("ERROR: Concurrent modification on user update update: " + usr, color(255, 0, 0));
                }
                //console.println("A new user already existed. Their username was: " + usr);

            }

            /*all of these other incoming messages must be ignored unless the login has connected, otherwise confusion will ensue*/
            if (sessionStarted) {

                if (theOscMessage.addrPattern().equals("/stopUpdating")) {
                    sessionUpdating = false;
                }

                //update to know about a user that is already on the server when we log in
                //not used



                if (theOscMessage.addrPattern().equals("/new_userJoined")) {

                    String usr;
                    int newx, newy;
                    int newcolor;

                    usr = theOscMessage.get(0).stringValue();
                    newx = theOscMessage.get(1).intValue();
                    newy = theOscMessage.get(2).intValue();
                    newcolor = theOscMessage.get(3).intValue();


                    if (!checkUserExists(usr))
                        otherUsers.add(new UserDetails(usr, newx, newy, newcolor, this));

                    try {
                        console.println(usr + " is connecting!");
                    } catch (ConcurrentModificationException ex) {
                        console.println("ERROR: Concurrent modification on user update update: " + usr, color(255, 0, 0));
                    }
                }

                //a user left the server or has dropped out
                if (theOscMessage.addrPattern().equals("/userDropped")) {
                    String usr = theOscMessage.get(0).stringValue();
                    //look up this user and remove them from the active session
                    for (int i = 0; i < otherUsers.size(); i++) {
                        UserDetails u = (UserDetails) otherUsers.get(i);
                        if (u.username.equals(usr)) {
                            console.println("DEBUG: User: " + usr + " has left!");
                            otherUsers.remove(i);
                        }

                    }
                }


                if (theOscMessage.addrPattern().equals("/action_userMoved")) {
                    String usr;
                    int newx, newy;

                    usr = theOscMessage.get(0).stringValue();
                    newx = theOscMessage.get(1).intValue();
                    newy = theOscMessage.get(2).intValue();

                    //look up this user and update their location details
                    for (int i = 0; i < otherUsers.size(); i++) {
                        UserDetails u = (UserDetails) otherUsers.get(i);
                        if (u.username.equals(usr)) {
                            u.posx = newx;
                            u.posy = newy;
                        }
                    }
                    //console.println("User: " + usr + " moved! X: " + newx + " Y: " + newy);

                }

                if (theOscMessage.addrPattern().equals("/newAudioEntity")) {

                    String type, identifier, user;
                    int nx, ny;
                    user = theOscMessage.get(0).stringValue();
                    type = theOscMessage.get(1).stringValue();
                    identifier = theOscMessage.get(2).stringValue();
                    nx = theOscMessage.get(3).intValue();
                    ny = theOscMessage.get(4).intValue();
                    try {
                        messageQueue.add(messageQueue.new AddRemoteEntity(type, identifier, user, nx, ny));
                    } catch (ConcurrentModificationException ex) {
                        console.println("ERROR: Concurrent modification error on remotely loaded entity: " + identifier, color(255, 0, 0));
                    }

                }

                if (theOscMessage.addrPattern().equals("/movedAudioEntity")) {



                    String identifier, user;
                    int nx, ny;
                    user = theOscMessage.get(0).stringValue();
                    identifier = theOscMessage.get(1).stringValue();
                    nx = theOscMessage.get(2).intValue();
                    ny = theOscMessage.get(3).intValue();
                    println("Moving audio entity: " + identifier);
                    try {

                        messageQueue.add(messageQueue.new MovedAudioEntity(identifier, user, nx, ny));
                    } catch (ConcurrentModificationException ex) {
                        console.println("ERROR: Concurrent modification error on moved entity: " + identifier, color(255, 0, 0));
                    }


                }
                if (theOscMessage.addrPattern().equals("/paramChange")) {
                    //this type of message is a bundle. It contains the osc messages and a timetag;



                    String identifier, user, paramName;
                    long timeTag = 0;
                    float value;

                    user = theOscMessage.get(0).stringValue();
                    identifier = theOscMessage.get(1).stringValue();
                    paramName = theOscMessage.get(2).stringValue();
                    value = theOscMessage.get(3).floatValue();
                    println("Got a paramchange message for " + paramName);
                    //is this a time tag entity?

                    try {
                        timeTag = Long.valueOf(theOscMessage.get(4).stringValue());
                    } catch (Exception ex) {
                        println("timetag exception");
                        ex.printStackTrace();
                    }
                    //console.println("GOT PARAM CHANGE WITH TIME: " + timeTag, color(255,255,0));



                    try {

                        //console.println("Param change. Identifier: " + identifier + " paramName: " + paramName);
                        messageQueue.add(messageQueue.new ParamChange(identifier, user, paramName, value, timeTag));
                    } catch (Exception ex) {
                        println("ERROR: Concurrent modification error on paramChange: " + paramName);
                    }
                }

                if (theOscMessage.addrPattern().equals("/wavefrontTimeSync")) {
                    String identifier, time;


                    identifier = theOscMessage.get(0).stringValue();
                    time = theOscMessage.get(1).stringValue();
                    console.println("Resyncing wavefront with identifier: " + identifier);
                    try {
                        messageQueue.add(messageQueue.new WavefrontTimeSync(identifier, time));
                    } catch (Exception ex) {
                        console.println("ERROR: Concurrent modification error on wavefront time sync: " + identifier, color(255, 0, 0));
                    }
                }



                if (theOscMessage.addrPattern().equals("/triggerWavefront")) {
                    //console.println("Activated...");	
                    String identifier;

                    identifier = theOscMessage.get(0).stringValue();

                    try {
                        synchronized(audioEntities) {
                            for (int i = 0; i < audioEntities.size(); i++) {
                                AudioEntity a = (AudioEntity) audioEntities.get(i);
                                if (a.identifier.equals(identifier)) {
                                    Wavefront aa = (Wavefront) audioEntities.get(i);
                                    aa.activate();

                                }

                            }
                        }


                    } catch (Exception ex) {
                        console.println("ERROR: Concurrent modification wavefront trigger: " + identifier, color(255, 0, 0));
                    }
                }

                if (theOscMessage.addrPattern().equals("/groupControlMessage")) {

                    String identifier;
                    String group;
                    float value;



                    identifier = theOscMessage.get(1).stringValue();
                    group = theOscMessage.get(2).stringValue();
                    value = theOscMessage.get(3).floatValue();

                    messageQueue.add(messageQueue.new GroupParamChange(identifier, group, value));

                    //console.println("passed group control message.");
                }


                if (theOscMessage.addrPattern().equals("/chatMessage")) {
                    String user;
                    String message;

                    user = theOscMessage.get(0).stringValue();
                    message = theOscMessage.get(1).stringValue();

                    for (int i = 0; i < otherUsers.size(); i++) {
                        UserDetails u = (UserDetails) otherUsers.get(i);
                        if (u.username.equals(user)) {
                            console.println(user + " says: " + message, u.userColor);
                            break;
                        }
                    }

                    if (user.equals(userDetails.username)) {
                        console.println(user + " says: " + message, userDetails.userColor);
                    }




                }

                //another user has snapped something to a wavefront
                if (theOscMessage.addrPattern().equals("/snapWavefrontMessage")) {
                    //println("Got snap message.");
                    String entityID;
                    String snapeeID;

                    int numEntities, entIndex;
                    numEntities = 0;
                    entIndex = 0;
                    entityID = theOscMessage.get(0).stringValue();
                    snapeeID = theOscMessage.get(1).stringValue();

                    if (theOscMessage.arguments().length == 4) {
                        numEntities = theOscMessage.get(2).intValue();
                        entIndex = theOscMessage.get(3).intValue();
                    }
                    println("Snap message arrived for snapee " + entIndex + " of " + numEntities);
                    //println("Snapping entityID: " + entityID + " snapeeID: " + snapeeID);
                    messageQueue.add(messageQueue.new SnapToWavefront(entityID, snapeeID, numEntities, entIndex));
                }

                //another user has unsnapped something from a wavefront
                if (theOscMessage.addrPattern().equals("/unsnapWavefrontMessage")) {
                    String entityID;
                    String snapeeID;
                    entityID = theOscMessage.get(0).stringValue();
                    snapeeID = theOscMessage.get(1).stringValue();
                    println("Unsnapping entityID: " + entityID + " snapeeID: " + snapeeID);
                    messageQueue.add(messageQueue.new UnsnapFromWavefront(entityID, snapeeID));
                }

                //another user has snapped something to a wavefront
                if (theOscMessage.addrPattern().equals("/removeEntity")) {
                    println("Got snap message.");
                    String identifier;

                    identifier = theOscMessage.get(0).stringValue();


                    messageQueue.add(messageQueue.new RemoveRemoteEntity(identifier));
                }

                //ping message
                if (theOscMessage.addrPattern().equals("/ping")) {

                    console.println("SERVER PING = " + (pingWatch.getTime()) + "milliseconds.", color(20, 0, 255));
                    pingWatch = new Stopwatch(this);
                }

                //the server's request for a specific client ping response
                if (theOscMessage.addrPattern().equals("/clientPingFromServer")) {

                    console.println("Responded to ping request from user: " + theOscMessage.get(0).stringValue(), color(20, 0, 255));
                    OscMessage reply = new OscMessage("/clientPingFromClient");
                    reply.add(userDetails.username);
                    reply.add(theOscMessage.get(0).stringValue());

                    oscP5.send(reply, myRemoteLocation);
                }

                //the server's relays a client's ping response
                if (theOscMessage.addrPattern().equals("/clientPingResponse")) {

                    String user = theOscMessage.get(0).stringValue();
                    println("Ping response from: " + user);
                    for (int i = 0; i < otherUsers.size(); i++) {
                        UserDetails ud = (UserDetails) otherUsers.get(i);
                        if (ud.username.equals(user)) {
                            console.println("PING for user " + user + " =  " + ud.pingTimer.getTime() + " milliseconds.", color(0, 20, 255));


                            ud.pingTimer = new Stopwatch(this);
                        }
                    }



                }

                //print an error from the server to the console
                if (theOscMessage.addrPattern().equals("/error")) {
                    console.println(theOscMessage.get(0).stringValue(), color(0, 0, 0));
                }




                //another user has unsnapped something from a wavefront
                if (theOscMessage.addrPattern().equals("/lockEntity")) {

                    String entityID;
                    String userID;
                    userID = theOscMessage.get(0).stringValue();
                    entityID = theOscMessage.get(1).stringValue();

                    for (int i = 0; i < audioEntities.size(); i++) {
                        AudioEntity e = (AudioEntity) audioEntities.get(i);
                        if (e.identifier.equals(entityID)) {
                            e.lock();
                            e.lockUser = userID;
                            break;
                        }
                    }
                }

                //another user has unsnapped something from a wavefront
                if (theOscMessage.addrPattern().equals("/unlockEntity")) {
                    String entityID;
                    String userID;
                    userID = theOscMessage.get(0).stringValue();
                    entityID = theOscMessage.get(1).stringValue();

                    for (int i = 0; i < audioEntities.size(); i++) {
                        AudioEntity e = (AudioEntity) audioEntities.get(i);
                        if (e.identifier.equals(entityID)) {
                            e.unlock();
                            //e.lockUser=userID;
                            break;
                        }
                    }
                }

            }
        } catch (Exception ex) {
            console.println("ERROR: Exception in event handling function. Details:. ", color(255, 0, 0));
            console.println(ex.getMessage());
        }

    }

    /**
     * Adds an entity based on an OSC message. Does not inform the server.
     * @param user
     * @param type
     * @param identifier
     * @param x
     * @param y
     */
    public void addEntityRemote(String user, String type, String identifier, int x, int y) {
        if (type.equals("Bloop")) {
            Bloop h = new Bloop(this, cp5, ac, x, y);
            h.identifier = identifier;
            h.setOwner(userDetails.username);
            audioEntities.add(h);

            outGain.addInput(h.outGain);
        }

        if (type.equals("Wavefront")) {
            Wavefront h = new Wavefront(this, cp5, ac, x, y);
            h.identifier = identifier;
            h.setOwner(userDetails.username);
            audioEntities.add(h);

        }

        if (type.equals("Sample")) {
            Sample h = new Sample(this, cp5, ac, x, y);
            h.identifier = identifier;
            h.setOwner(userDetails.username);
            audioEntities.add(h);
            outGain.addInput(h.outGain);

        }

        if (type.equals("Grain")) {
            Grain h = new Grain(this, cp5, ac, x, y);
            h.identifier = identifier;
            h.setOwner(userDetails.username);
            audioEntities.add(h);
            outGain.addInput(h.outGain);

        }



    }

    /**
     * Checks to make sure that this user doesn't already somehow exist in the local session.
     * @param user
     * @return
     */
    public boolean checkUserExists(String user) {
        boolean found = false;
        //make sure this user doesn't already exist for some weird reason
        for (int i = 0; i < otherUsers.size(); i++) {
            UserDetails u = (UserDetails) otherUsers.get(i);
            if (u.username == user) {
                found = true;
                break;
            }

        }

        return found;
    }

    /**
     * Gets a controlP5 conotroller by its id.
     * @param id
     * @return
     */
    public Controller getControllerById(int id) {


        for (Controller c: cp5.getAll(Controller.class)) {
            if (c.getId() == id)
                return c;
        }

        return null;
    }

    /**
     * Notes that the session has now begun by turning sessionStarted to true.
     */
    public void startSession() {
        sessionStarted = true;
    }

    /**
     * Returns the SHA256 digest of the input string.
     * @param input
     * @return
     */
    public String getsha256(String input) {
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(input.getBytes(), 0, input.length());

            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }


    /**
     * Sends an entity position change OSC message to the server.
     * @param x
     * @param y
     */
    public void sendPositionChanged(int x, int y) {
        OscMessage m = new OscMessage("/userPositionChanged");
        m.add(userDetails.username);
        m.add(x);
        m.add(y);
        oscP5.send(m, myRemoteLocation);
    }


    /**
     * Informs the server that the user has added a new entity.
     * @param type
     * @param identifier
     * @param x
     * @param y
     */
    public void sendNewAudioEntity(String type, String identifier, int x, int y) {

        OscMessage m = new OscMessage("/newAudioEntity");
        m.add(userDetails.username);
        m.add(type);
        m.add(identifier);
        m.add(x);
        m.add(y);
        oscP5.send(m, myRemoteLocation);

    }

    /** 
     * Informs the server that the user has removed an entity.
     * @param identifier
     */
    public void sendRemoveAudioEntity(String identifier) {
        OscMessage m = new OscMessage("/removeEntity");
        m.add(userDetails.username);
        m.add(identifier);
        oscP5.send(m, myRemoteLocation);
    }


    /**
     * Informs the server that the user has moved an entity.
     * @param identifier
     * @param x
     * @param y
     */
    public void sendMovedAudioEntity(String identifier, int x, int y) {
        OscMessage m = new OscMessage("/movedAudioEntity");
        m.add(userDetails.username);
        m.add(identifier);
        m.add(x);
        m.add(y);
        oscP5.send(m, myRemoteLocation);
    }


    /**
     * Sends a keep alive message to the server.
     */
    public void sendKeepAlive() {
        OscMessage m = new OscMessage("/keepAlive");
        m.add(userDetails.username);
        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Informs the server that an entity has been locked with this username.
     * @param identifier
     */
    public void sendLockMessage(String identifier) {
        OscMessage m = new OscMessage("/lockEntity");
        m.add(userDetails.username);
        m.add(identifier);
        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Informs the server that an entity has been unlocked.
     * @param identifier
     */
    public void sendUnlockMessage(String identifier) {
        OscMessage m = new OscMessage("/unlockEntity");
        m.add(userDetails.username);
        m.add(identifier);
        oscP5.send(m, myRemoteLocation);
    }


    /**
     * Sends an OSC chat message.
     * @param message
     */
    public void sendChatMessage(String message) {
        if (!message.equals("")) {
            OscMessage m = new OscMessage("/chatMessage");
            m.add(userDetails.username);
            m.add(message);
            oscP5.send(m, myRemoteLocation);
        }
    }

    /**
     * Sends a group control message.
     * @param identifier
     * @param group
     * @param value
     */
    public void sendGroupControlMessage(String identifier, String group, float value) {
        OscMessage m = new OscMessage("/groupControlMessage");
        m.add(userDetails.username);
        m.add(identifier);
        m.add(group);
        m.add(value);
        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Informs the server that an entity has been snapped to a wavefront.
     * @param entityID
     * @param snapeeID
     */
    public void sendSnapWavefrontMessage(String entityID, String snapeeID) {
        OscMessage m = new OscMessage("/snapWavefrontMessage");
        m.add(entityID);
        m.add(snapeeID);
        m.add(userDetails.username);
        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Informs the server that an entity has been unsnapped.
     * @param entityID
     * @param snapeeID
     */
    public void sendUnsnapWavefrontMessage(String entityID, String snapeeID) {
        OscMessage m = new OscMessage("/unsnapWavefrontMessage");
        m.add(entityID);
        m.add(snapeeID);
        m.add(userDetails.username);

        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Sends a ping message and wait for the server to respond.
     */
    public void sendPingMessage() {
        OscMessage m = new OscMessage("/ping");
        m.add(userDetails.username);
        pingWatch.start();
        oscP5.send(m, myRemoteLocation);
    }

    /**
     * send a request for ping measurement from all the users connected. The client sends a message to the server,
     * which sends messages to all the users. The users then send replies back to the server and the server routes
     * them back to this client, where a set of timers have measured the ping.
     */
    public void sendPingAll() {
        OscMessage m = new OscMessage("/pingAll");
        m.add(userDetails.username);

        //turn on ping timers for all the users in the userDetails list
        for (int i = 0; i < otherUsers.size(); i++) {
            UserDetails u = (UserDetails) otherUsers.get(i);
            u.startPingMeasurement();
        }

        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Sends a request to log-in.
     * @param username
     * @param password
     */
    public void sendAuthenticationRequest(String username, String password) {
        OscMessage m = new OscMessage("/userJoin");
        m.add(username);
        m.add(getsha256(password));
        oscP5.send(m, myRemoteLocation);
    }

    /**
     * Sends a request to register a new user.
     * @param username
     * @param password
     */
    public void sendRegistrationRequest(String username, String password) {
        OscMessage m = new OscMessage("/userRegister");
        m.add(username);
        m.add(getsha256(password));
        m.add(userRegisterColor);
        oscP5.send(m, myRemoteLocation);
    }


    Wavefront currentOriginWavefront = null;
    Boolean IS_CHOOSING_WF_TARGET = false;

    /**
     * the user clicked on a Wavefront's 'Target Wavefront' button. Turn on the
     * IS_CHOOSING_WF_TARGET variable and set the current origin wavefront
     */
    public void startWavefrontTargetChoice() {
        IS_CHOOSING_WF_TARGET = true;
        currentOriginWavefront = (Wavefront) currentContextEntity;
        currentContextEntity.hideContextControls();
        //currentContextEntity = null;
        IS_CONTEXT_VISIBLE = false;
    }

    /**
     * sets a wavefront's target and informs the server
     */

    public void setWavefrontTarget(Wavefront target) {
        IS_CHOOSING_WF_TARGET = false;
        currentOriginWavefront.target = target;
        OscMessage m = new OscMessage("/setWavefrontTarget");
        m.add(currentOriginWavefront.identifier);
        m.add(target.identifier);
        oscP5.send(m, myRemoteLocation);

        currentOriginWavefront = null;
    }

    /**
     * stops targeting... maybe the user clicked on something else or opened another context menu
     */
    public void stopWavefrontTargetChoice() {
        IS_CHOOSING_WF_TARGET = false;

    }

    /**
     * Converts from a byte array to a long.
     * @param in
     * @return
     */
    public long byteArrayToLong(byte[] in ) {

        ByteBuffer buf = ByteBuffer.wrap( in );
        return buf.getLong();
    }

    /** 
     * Converts from a long to a byte array.
     * @param in
     * @return
     */
    public byte[] longToByteArray(long in ) {
        byte b[] = new byte[8];

        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putLong( in );
        return b;

    }


}