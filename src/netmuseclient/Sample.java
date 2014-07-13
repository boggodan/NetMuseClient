package netmuseclient;



import java.util.Vector;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.beadsproject.beads.ugens.WavePlayer;
import netmuseclient.Wavefront.OwnedEntity;
import processing.core.PApplet;
import processing.core.PVector;
import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Slider;
import controlP5.Slider2D;
import controlP5.Textlabel;

/* an individual Bloop that can be played by activation systems such
 * as particles, wavefronts or user interaction.
 */
public class Sample extends AudioEntity {


    //appears upon right clicking the entity


    //String identifier;

    Slider gainSlider;
    Slider pitchSlider;

    SamplePlayer samp;

    Glide gainGlide;
    Glide pitchGlide;
    //list of references to the samples used in this entity
    Vector samples;

    Gain gain;
    controlP5.ListBox list;


    float pitch = 1.0f;

    int gainSpacing = 0;
    int pitchSpacing;
    int listSpacing = 0;

    Gain masterGain;
    Glide masterGainGlide;
    float masterGainMax;
    public Boolean activated;
    processing.core.PGraphics render;
    Sample(NetMuseClient a, ControlP5 p, AudioContext ac, int x, int y) {

        super(a, p, ac, x, y);
        setUsesAttenuation();
        snapped = false;
        activated = false;
        gainGlide = new Glide(ac, 0.0f);
        pitchGlide = new Glide(ac, 1.0f);
        masterGainGlide = new Glide(ac, 0.25f);
        masterGain = new Gain(ac, 1, masterGainGlide);
        outGain.addInput(masterGain);
        samples = new Vector();
        masterGainMax = 0.25f;
        try {
            samples.add(a.sampleBank.getByName("Kick 1"));
            samples.add(a.sampleBank.getByName("Hat"));
            samples.add(a.sampleBank.getByName("Snare"));
            samples.add(a.sampleBank.getByName("Click 1"));
            samples.add(a.sampleBank.getByName("Click 2"));
            samples.add(a.sampleBank.getByName("Heavy Kick"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }




        String sourceFile = a.sketchPath("") + "data/kick1.wav"; // Whenever we load a file, we need to enclose   // the code in a Try/Catch block.  // Try/Catch blocks will inform us if the file   // can't be found  try {      // initialize our SamplePlayer, loading the file     // indicated by the sourceFile string    sp = new SamplePlayer(ac, new Sample(sourceFile));   }  catch(Exception e)  {    // If there is an error, show an error message     // at the bottom of the processing window.    println("Exception while attempting to load sample!");    e.printStackTrace(); // print description of the error    exit(); // and exit the program  }    // SamplePlayer can be set to be destroyed when  // it is done playing  // this is useful when you want to load a number of  // different samples, but only play each one once  // in this case, we would like to play the sample multiple   // times, so we set KillOnEnd to false  sp.setKillOnEnd(false);

        try {

            samp = new SamplePlayer(ac, ((SampleBank.Sample) samples.get(0)).sample);
            samp.setKillOnEnd(false);
            samp.setRate(pitchGlide);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        masterGain.addInput(samp);

        gainSpacing = autoSpace();
        gainSlider = p.addSlider("" + a.random(Integer.MAX_VALUE), 0.0f, 1.0f, 0.25f, x + ch * 2, y - ch + yIncrement, 100, 13).setCaptionLabel("Gain").setColorLabel(a.color(0, 0, 0));;
        pitchSpacing = autoSpace();
        pitchSlider = p.addSlider("" + a.random(Integer.MAX_VALUE), 0.0f, 2.0f, 1.0f, x + ch * 2, y - ch + yIncrement, 100, 13).setCaptionLabel("Pitch").setColorLabel(a.color(0, 0, 0));;

        //define the parameters
        defParam("gainParam", "param");
        setParam("gainParam", 0.25f);
        defParam("pitchParam", "param");
        setParam("pitchParam", 1.0f);
        defParam("sampleParam", "group");
        setParam("sampleParam", 0.0f);

        listSpacing = autoSpace();
        listSpacing = autoSpace();
        list = p.addListBox("" + a.random(Integer.MAX_VALUE))
            .setPosition(x + ch * 2, yIncrement)
            .setSize(100, 120)
            .setItemHeight(13)
            .setBarHeight(13)
            .setColorBackground(a.color(40, 128))
            .setColorActive(a.color(255, 128))
            .setLabel("Samples");

        for (int i = 0; i < samples.size(); i++) {
            list.addItem(((SampleBank.Sample) samples.get(i)).name, i);
        }

        type = "Sample";
        gainSlider.setValue(0.25f);
        pitchSlider.setValue(1.0f);

        hideContextControls();
        //a.sendNewAudioEntity("Sample", identifier, a.mouseX + a.cameraX, a.mouseY+a.cameraY);
        genGFX();
    }

    //pregenerate the gfx
    void genGFX() {
        render = a.createGraphics(ch * 2, cw * 2, a.P2D);
        render.smooth();
        render.beginDraw();




        render.strokeWeight(2.0f);
        render.stroke(255, 255, 255, 255);
        render.fill(0, 0, 255, 255);
        render.ellipse(ch, ch, ch, ch);


        render.endDraw();

        render.filter(a.BLUR, 2);

        render.endDraw();
    }

    //the wavefront that this entity is snapped to

    //check out if this entity needs to be snapped to a wavefront
    @
    Override
    public void handleSnapWavefronts() {
        for (int i = 0; i < a.audioEntities.size(); i++) {
            AudioEntity e = (AudioEntity) a.audioEntities.get(i);
            if (e.getClass().getName().equals("netmuseclient.Wavefront")) {

                Wavefront wf = (Wavefront) a.audioEntities.get(i);
                boolean test = wf.handleSnapEntity(this, this.x, this.y);
                if (test) {
                    snapped = true;
                    a.sendSnapWavefrontMessage(identifier, wf.identifier);
                    break;
                }

            }
        }
    }

    //unsnaps itself from whatever it's snapped to
    @
    Override
    public void unsnap() {
        if (snapped) {
            //search for itself
            for (int i = 0; i < snapee.ownedEntities.size(); i++) {
                AudioEntity e = (AudioEntity)((OwnedEntity) snapee.ownedEntities.get(i)).e;
                if (e.identifier.equals(this.identifier)) {
                    snapee.ownedEntities.remove(i);
                    //a.console.println("Unsnapped something!");
                    snapped = false;

                    a.sendUnsnapWavefrontMessage(identifier, snapee.identifier);
                    snapee = null;
                    break;
                }
            }
        }
    }

    @
    Override
    void unsnapRemote() {
        if (snapped) {
            //search for itself
            for (int i = 0; i < snapee.ownedEntities.size(); i++) {
                AudioEntity e = (AudioEntity)((OwnedEntity) snapee.ownedEntities.get(i)).e;
                if (e.identifier.equals(this.identifier)) {
                    snapee.ownedEntities.remove(i);
                    //a.console.println("Unsnapped something!");
                    snapped = false;
                    //snapee = null;
                    //a.sendUnsnapWavefrontMessage(identifier, snapee.identifier);
                    snapee = null;
                    break;
                }
            }
        }
    }

    @
    Override
    public void cull(Boolean toggle) {
        samp.pause(toggle);
        outGain.pause(toggle);
        masterGain.pause(toggle);
        culled = toggle;
    }

    //draw the artefact
    public void draw(int camx, int camy) {
        /* You MUST call this, as it handles some common behaviours.*/
        super.draw(camx, camy);

        prevx = x;
        prevy = y;

        if (sendPositionTimer.isFinished()) {
            sendPositionTimer.start();
        }

        //display gfx from separate pgraphics
        a.image(render, drawx - ch, drawy - ch);

        a.image(render, drawx - ch, drawy - ch);

        gainSlider.setPosition(drawx + xSpacing, drawy - ch + gainSpacing);
        pitchSlider.setPosition(drawx + xSpacing, drawy - ch + pitchSpacing);
        list.setPosition(drawx + xSpacing, drawy - ch + listSpacing);


    }


    void play() {
        activated = true;
        samp.setToLoopStart();
        samp.start();

        //SQUARE SEARCH METHOD
        //search for squares, within a circle, within square
        //AKA Squareception Algorithm
        for (int nx = (int) x - (int) 200; nx < x + 200; nx += 60)
            for (int ny = (int) y - (int) 200; ny < y + 200; ny += 60) {
                if (nx > 0 && ny > 0) {
                    PVector squareLocation = a.landscapeMatrix.getSquareCoordsFromWorldCoords(nx, ny);

                    float gfxDist = a.dist(nx, ny, x, y);


                    float colorScaler = (1.0f - gfxDist / 200) * 35.0f;

                    if (gfxDist < 200) {
                        a.landscapeMatrix.modifyElement((int) squareLocation.x, (int) squareLocation.y, colorScaler, colorScaler, 0.0f);
                    }
                }
            }

    }

    public void showContextControls() {
        super.showContextControls();
        list.show();

        gainSlider.show();

        pitchSlider.show();
    }
    public void hideContextControls() {
        super.hideContextControls();
        gainSlider.hide();

        list.hide();

        pitchSlider.hide();
    }

    @
    Override
    public boolean getIsOverControllers() {
        if (lockButton.isMouseOver() || unlockButton.isMouseOver() || removeButton.isMouseOver() || pitchSlider.isMouseOver() || list.isMouseOver() || moveButton.isMouseOver() || gainSlider.isMouseOver() || cloneButton.isMouseOver())
            return true;
        else
            return false;
    }

    @
    Override
    public void startMoving() {
        moving = true;
        unsnap();
        //unsnap from wavefront 

    }@
    Override
    public void stopMoving() {
        moving = false;
        //snap this to something if need be
        handleSnapWavefronts();
    }

    @
    Override
    public void startMultiMoving(int mx, int my) {
        super.startMultiMoving(mx, my);
        unsnap();
        //unsnap from wavefront 

    }

    @
    Override
    public void stopMultiMoving(int mx, int my) {
        super.stopMultiMoving(mx, my);
        //snap this to something if need be
        handleSnapWavefronts();
    }


    @
    Override
    public void remoteParamChange(String paramName, float value, long timeTag) {
        setParam(paramName, value);
        if (paramName.equals("gainParam")) {
            gainSlider.setValueLabel("" + value);
            masterGainMax = value;
            setParam("gainParam", value);
            masterGainGlide.setValue(value);

        }
        if (paramName.equals("listParam")) {
            //gainSlider.setValueLabel("" + value);
            //masterGainMax = value;
            //masterGainGlide.setValue(value);
        }
        if (paramName.equals("pitchParam")) {
            setParam("pitchParam", value);

            pitchSlider.setValueLabel("" + value);
            pitch = value;
            pitchGlide.setValue(value);
        }
    }

    @
    Override
    public float paramHandler(String paramName) {

            if (paramName.equals(gainSlider.name())) {
                masterGainGlide.setValue(gainSlider.getValue());
                setParam("gainParam", gainSlider.getValue());
                return gainSlider.getValue();
            }
            if (paramName.equals(pitchSlider.name())) {
                pitchGlide.setValue(pitchSlider.getValue());
                setParam("pitchParam", pitchSlider.getValue());
                return pitchSlider.getValue();
            }
            if (paramName.equals(list.name())) {
                return 1.0f;

            } else return 0.0f;
        }
        //translate controller numbers into param numbers
        @
    Override
    public String translateParamName(String ctlName) {
        if (ctlName.equals(gainSlider.getName()))
            return "gainParam";
        if (ctlName.equals(list.getName()))
            return "listParam";
        if (ctlName.equals(pitchSlider.getName()))
            return "pitchParam";
        if (ctlName.equals(removeButton.getName()))
            return "removeParam";
        else return "";
    }

    @
    Override
    void groupHandler(String group, float value) {
        //user chose a sample. Switch the sample;
        if (group.equals(list.name())) {
            //get the sample corresponding to this number
            try {
                net.beadsproject.beads.data.Sample s = ((SampleBank.Sample) samples.get((int) value)).sample;
                samp.setSample(s);
            } catch (Exception ex) {}
            setParam("sampleParam", value);
        }
    }

    @
    Override
    void remoteGroupHandler(String group, float value) {
        //user chose a sample. Switch the sample;
        if (group.equals("sampleParam")) {
            //get the sample corresponding to this number
            try {
                net.beadsproject.beads.data.Sample s = ((SampleBank.Sample) samples.get((int) value)).sample;
                samp.setSample(s);
            } catch (Exception ex) {}
            setParam("sampleParam", value);
        }
    }

    @
    Override
    String translateGroupHandler(String group) {
        if (group.equals(list.name())) {
            return "sampleParam";
        }
        return "";
    }


}