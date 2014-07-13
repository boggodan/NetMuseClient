package netmuseclient;

import com.sun.xml.internal.messaging.saaj.soap.Envelope;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.WavePlayer;
import netmuseclient.Wavefront.OwnedEntity;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import sun.font.CreatedFontTracker;
import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Slider;
import controlP5.Slider2D;
import controlP5.Textlabel;

/* an individual Bloop that can be played by activation systems such
 * as particles, wavefronts or user interaction.
 */
public class Bloop extends AudioEntity {


    //appears upon right clicking the entity


    //String identifier;
    Slider freqSlider;
    Slider gainSlider;

    Slider attackSlider, decaySlider;

    WavePlayer wp;
    Glide freqGlide;
    Glide gainGlide;
    Gain gain;

    int freqSpacing;
    int gainSpacing;
    int attackSpacing, decaySpacing;

    processing.core.PGraphics render;


    net.beadsproject.beads.ugens.Envelope gainEnvelope;
    Gain masterGain;
    Glide masterGainGlide;
    float masterGainMax;
    public Boolean activated;

    float attackTime = 2.0f;
    float decayTime = 1000.0f;


    Bloop(NetMuseClient a, ControlP5 p, AudioContext ac, int x, int y) {

        super(a, p, ac, x, y);
        setUsesAttenuation();
        snapped = false;
        activated = false;
        gainGlide = new Glide(ac, 0.0f);
        masterGainMax = 0.25f;
        gainEnvelope = new net.beadsproject.beads.ugens.Envelope(ac, 0.0f);
        gain = new Gain(ac, 1, gainEnvelope);
        masterGainGlide = new Glide(ac, 0.25f);
        masterGain = new Gain(ac, 1, masterGainGlide);
        freqGlide = new Glide(ac, 200.0f);
        wp = new WavePlayer(ac, freqGlide, Buffer.SINE);
        gain.addInput(wp);
        masterGain.addInput(gain);
        outGain.addInput(masterGain);
        freqSpacing = 0;
        type = "Bloop";
        freqSpacing = autoSpace();
        freqSlider = p.addSlider("" + a.random(Integer.MAX_VALUE), 0.0f, 1000.0f, 200.0f, x + ch * 2, y - ch + yIncrement, 100, 13).setCaptionLabel("Frequency").setColorLabel(a.color(0, 0, 0));;
        gainSpacing = autoSpace();
        gainSlider = p.addSlider("" + a.random(Integer.MAX_VALUE), 0.0f, 1.0f, 0.25f, x + ch * 2, y - ch + yIncrement, 100, 13).setCaptionLabel("Gain").setColorLabel(a.color(0, 0, 0));;
        attackSpacing = autoSpace();
        attackSlider = p.addSlider("" + a.random(Integer.MAX_VALUE), 0.0f, 2000.0f, 0.0f, x + ch * 2, y - ch + yIncrement, 100, 13).setCaptionLabel("Attack Time").setColorLabel(a.color(0, 0, 0));;
        decaySpacing = autoSpace();
        decaySlider = p.addSlider("" + a.random(Integer.MAX_VALUE), 0.0f, 2000.0f, 1000.0f, x + ch * 2, y - ch + yIncrement, 100, 13).setCaptionLabel("Decay Time").setColorLabel(a.color(0, 0, 0));;

        gainSlider.setValue(0.25f);
        freqSlider.setValue(200.0f);
        attackSlider.setValue(2.0f);
        decaySlider.setValue(1000.0f);
        //a.sendNewAudioEntity("Bloop", identifier, a.mouseX + a.cameraX, a.mouseY+a.cameraY);
        //handleSnapWavefronts();
        hideContextControls();

        //define the parameters
        defParam("freqParam", "param");
        setParam("freqParam", 200.0f);
        defParam("gainParam", "param");
        setParam("gainParam", 0.25f);
        defParam("attackParam", "param");
        setParam("attackParam", 2.0f);
        defParam("decayParam", "param");
        setParam("decayParam", 1000.0f);


        //draw the gfx in a PGraphics
        genGFX();
    }

    //pregenerate the gfx
    void genGFX() {
        render = a.createGraphics(ch * 2, cw * 2, a.P2D);
        render.smooth();
        render.beginDraw();




        render.strokeWeight(2.0f);
        render.stroke(255, 255, 255, 255);
        render.fill(255, 255, 255, 255);
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
                //this call will snap the entity to the grid if that option is enabled in the wavefront
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
    public void unsnapRemote() {
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
        wp.pause(toggle);
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


        freqSlider.setPosition(drawx + xSpacing, drawy - ch + freqSpacing);
        gainSlider.setPosition(drawx + xSpacing, drawy - ch + gainSpacing);
        attackSlider.setPosition(drawx + xSpacing, drawy - ch + attackSpacing);
        decaySlider.setPosition(drawx + xSpacing, drawy - ch + decaySpacing);

    }


    void envelopeTrigger() {
        activated = true;
        gainEnvelope.clear();

        gainEnvelope.addSegment(0.0f, 5.0f);
        gainEnvelope.addSegment(1.0f, attackTime);
        gainEnvelope.addSegment(0.0f, decayTime);


        //SQUARE SEARCH METHOD
        //search for squares, within a circle, within square
        //AKA Squareception Algorithm
        for (int nx = (int) x - (int) 200; nx < x + 200; nx += 60)
            for (int ny = (int) y - (int) 200; ny < y + 200; ny += 60) {
                if (nx > 0 && ny > 0) {
                    PVector squareLocation = a.landscapeMatrix.getSquareCoordsFromWorldCoords(nx, ny);

                    float gfxDist = a.dist(nx, ny, x, y);


                    float colorScaler = (1.0f - gfxDist / 200) * 70.0f;

                    if (gfxDist < 200) {
                        a.landscapeMatrix.modifyElement((int) squareLocation.x, (int) squareLocation.y, -colorScaler, -colorScaler, -colorScaler);
                    }
                }
            }


    }

    public void showContextControls() {
        super.showContextControls();
        freqSlider.show();
        gainSlider.show();
        attackSlider.show();
        decaySlider.show();

    }
    public void hideContextControls() {
        super.hideContextControls();
        gainSlider.hide();
        freqSlider.hide();
        attackSlider.hide();
        decaySlider.hide();
    }

    @
    Override
    public boolean getIsOverControllers() {

        if (lockButton.isMouseOver() || unlockButton.isMouseOver() || removeButton.isMouseOver() || freqSlider.isMouseOver() || moveButton.isMouseOver() || gainSlider.isMouseOver() || cloneButton.isMouseOver() ||
            attackSlider.isMouseOver() || decaySlider.isMouseOver())
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

        if (paramName.equals("freqParam")) {
            freqSlider.setValueLabel("" + value);
            freqGlide.setValue(value);
            setParam("freqParam", value);

        }

        if (paramName.equals("gainParam")) {
            gainSlider.setValueLabel("" + value);
            masterGainMax = value;
            masterGainGlide.setValue(value);
            setParam("gainParam", value);

        }

        if (paramName.equals("attackParam")) {
            attackSlider.setValueLabel("" + value);
            attackTime = value;
            setParam("attackParam", value);

        }

        if (paramName.equals("decayParam")) {
            decaySlider.setValueLabel("" + value);
            decayTime = value;
            setParam("decayParam", value);

        }

    }

    @
    Override
    public float paramHandler(String paramName) {

            if (paramName.equals(freqSlider.name())) {
                freqGlide.setValue(freqSlider.getValue());
                setParam("freqParam", freqSlider.getValue());
                return freqSlider.getValue();

            }
            if (paramName.equals(gainSlider.name())) {
                masterGainGlide.setValue(gainSlider.getValue());
                setParam("gainParam", gainSlider.getValue());
                return gainSlider.getValue();
            }
            if (paramName.equals(attackSlider.name())) {
                attackTime = attackSlider.value();
                setParam("attackParam", attackSlider.getValue());
                return attackSlider.getValue();
            }
            if (paramName.equals(decaySlider.name())) {
                decayTime = decaySlider.value();
                setParam("decayParam", decaySlider.getValue());
                return decaySlider.getValue();
            } else return 0.0f;
        }
        //translate controller numbers into param numbers
        @
    Override
    public String translateParamName(String ctlName) {
        if (ctlName.equals(freqSlider.getName()))
            return "freqParam";
        if (ctlName.equals(gainSlider.getName()))
            return "gainParam";
        if (ctlName.equals(attackSlider.getName()))
            return "attackParam";
        if (ctlName.equals(decaySlider.getName()))
            return "decayParam";
        if (ctlName.equals(removeButton.getName()))
            return "removeParam";

        else return "";
    }


}