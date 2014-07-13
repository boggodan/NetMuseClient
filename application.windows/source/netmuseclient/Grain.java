package netmuseclient;



import java.util.ArrayList;
import java.util.Comparator;
import java.util.Vector;

import com.lowagie.text.List;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.beadsproject.beads.ugens.WavePlayer;
import netmuseclient.SampleBank.Sample;
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
public class Grain extends AudioEntity
{


	//appears upon right clicking the entity

	
	//String identifier;

	Slider gainSlider;
	Slider grainSizeSlider;
	Slider grainPositionSlider;
	Slider intervalSlider;
	Slider pitchSlider;
	
	
	
	GranularSamplePlayer gsp;
	
	Glide gainGlide;
	
	Glide grainSizeGlide;
	Glide positionGlide;
	Glide intervalGlide;
	Glide pitchGlide;

	//is the envelope above 0.01f? otherwise, we need to turn this off
	Boolean isPlaying = false;
	
	Gain gain;
	
	controlP5.ListBox list;
	
	//list of references to the samples used in this entity
	Vector samples; 
	
    float pitch= 1.0f;
    
    int listSpacing;
    int gainSpacing;
    int grainSizeSpacing;
    int grainPositionSpacing;
    int intervalSpacing;
    int pitchSpacing;
  
    //the wavefront actuated gain of the grain, and its glide
	Gain grainvelope;
	Glide grainvelopeGlide;
	
	//prevent pause pop by muting before pause()
	Gain cullMute;
	
	float grainvvelopeAccum;
	
	Gain masterGain;
	Glide masterGainGlide;
	float masterGainMax;
	public Boolean activated;
	processing.core.PGraphics render;
	ArrayList <Float> waveDistances;
	Grain(NetMuseClient a, ControlP5 p, AudioContext ac, int x, int y)
	{
		
		super(a, p, ac, x, y);
		
		setUsesAttenuation();
		grainvvelopeAccum = 0.0f;
		masterGainMax = 0.25f;
		samples = new Vector();
		masterGainGlide = new Glide(ac, 1.0f, 20);
		masterGain = new Gain(ac,1,masterGainGlide);
		snapped = false;
		activated = false;
		grainvelope = new Gain(ac,1, 0.0f);
		grainvelopeGlide = new Glide(ac,0.0f,50);
		grainvelope.setGain(grainvelopeGlide);
		
		cullMute = new Gain(ac,1,1.0f);
		
		
	     type = "Grains";
	   //define the parameters
	     defParam("sampleParam", "group");
		    setParam("sampleParam", 0.0f);
		    defParam("gainParam", "param");
		    setParam("gainParam", 1.0f);
		    defParam("pitchParam", "param");
		    setParam("pitchParam", 1.0f);
		    defParam("grainSizeParam", "param");
		    setParam("grainSizeParam", 100.0f);
		    defParam("grainPositionParam", "param");
		    setParam("grainPositionParam", 0.1f);
		    defParam("grainIntervalParam", "param");
		    setParam("grainIntervalParam", 50.0f);
		   
		    waveDistances = new ArrayList<Float>();
	  try
	  {
	    samples.add(a.sampleBank.getByName("Microtonal"));
	    samples.add(a.sampleBank.getByName("Cthulhu"));
	    samples.add(a.sampleBank.getByName("Birdies"));
	    samples.add(a.sampleBank.getByName("Clangboop"));
	    samples.add(a.sampleBank.getByName("Fire Crackle"));
	    samples.add(a.sampleBank.getByName("Sea Shore"));
	    samples.add(a.sampleBank.getByName("Tuning Orchestra"));
	  }
	  catch(Exception ex)
	  {
		  ex.printStackTrace();
	  }
	
		a.println("setting up source file...");

		 a.println("setting up granulator...");
		 try{
			 
			 a.println("trying to create gsp object");
			 gsp = new GranularSamplePlayer(ac, ((SampleBank.Sample)samples.get(0)).sample);
			 //gsp.start();
			 a.println("setting up gsp parameters.");
			 
			 gainGlide = new Glide(ac, 1.0f, 1);
			 grainSizeGlide = new Glide(ac, 100, 50); 
			 intervalGlide = new Glide(ac, 50, 100); 
			 positionGlide = new Glide(ac, 0.1f * gsp.getSample().getLength(), 30);
		     pitchGlide = new Glide(ac, 1, 20);
			 
			 gsp.setPitch(pitchGlide);
			 gsp.setGrainSize(grainSizeGlide);
			 gsp.setGrainInterval(intervalGlide);
			 gsp.setPosition(positionGlide);
			 a.println("starting gsp...");
			 gsp.start();
			 a.println("started gsp!");
			 //samp = new SamplePlayer(ac, new net.beadsproject.beads.data.Sample(sourceFile));
			 //samp.setKillOnEnd(false);
			 //samp.setRate(pitchGlide);
			 
		 }
		 catch(Exception ex){
			 a.println("gsp exception at constructor!");
			ex.printStackTrace();
		 }
		
		 cullMute.addInput(gsp);
		 grainvelope.addInput(cullMute);
		 masterGain.addInput(grainvelope);
		 outGain.addInput(masterGain);
		 /*gainSpacing = autoSpace();
		gainSlider =  p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,1.0f,0.5f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Gain");
		pitchSpacing = autoSpace();
		pitchSlider = p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,2.0f,1.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Pitch");*/
		
		gainSpacing = autoSpace();
		gainSlider = p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,1.0f,1.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Gain").setColorLabel(a.color(0,0,0));
		grainSizeSpacing = autoSpace();
	    grainSizeSlider = p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,1000.0f,100.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Grain Size").setColorLabel(a.color(0,0,0));
	    grainPositionSpacing = autoSpace();
	    grainPositionSlider = p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,1.0f,0.1f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Position").setColorLabel(a.color(0,0,0));
	    intervalSpacing = autoSpace();
	    intervalSlider = p.addSlider("" + a.random(Integer.MAX_VALUE),40.0f,1000.0f,50.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Interval").setColorLabel(a.color(0,0,0));
	    pitchSpacing = autoSpace();
	    pitchSlider = p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,2.0f,1.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Pitch").setColorLabel(a.color(0,0,0));
		
		listSpacing = autoSpace(); listSpacing = autoSpace();
		list = p.addListBox("" + a.random(Integer.MAX_VALUE))
		         .setPosition(x + ch*2, yIncrement)
		         .setSize(100, 120)
		         .setItemHeight(13)
		         .setBarHeight(13)
		         .setColorBackground(a.color(40, 128))
		         .setColorActive(a.color(255, 128))
		         .setLabel("Samples")
		         ;
		
		for(int i =0; i<samples.size();i++)
		{
			list.addItem(((SampleBank.Sample)samples.get(i)).name, i);
		}
		
		
		gainSlider.setValue(1.0f);
	    pitchSlider.setValue(1.0f);
		
	    hideContextControls();
	   //a.sendNewAudioEntity("Sample", identifier, a.mouseX + a.cameraX, a.mouseY+a.cameraY);
	    genGFX();
	    a.println("got past gsp constructor.");
	}
	
	//pregenerate the gfx
		void genGFX()
		{
			render = a.createGraphics(ch*2, cw*2, a.P2D);  
			  render.smooth();
			  render.beginDraw();
			  
			  
			  
			  
			 
			  render.strokeWeight(2.0f);
			  render.stroke(255,255,255,255);
			  render.fill(0,0,0,255);
			  render.ellipse(ch,ch,ch,ch);
			  
			  
			  render.endDraw();
			  
			  render.filter(a.BLUR,2);
			 
			  render.endDraw();
		}
		
	//the wavefront that this entity is snapped to
	
		//check out if this entity needs to be snapped to a wavefront
		@Override
		public void handleSnapWavefronts()
		{
			for(int i = 0; i <a.audioEntities.size(); i++)
			{
				AudioEntity e = (AudioEntity)a.audioEntities.get(i);
				if(e.getClass().getName().equals("netmuseclient.Wavefront"))
				{
					
					Wavefront wf = (Wavefront)a.audioEntities.get(i);
					boolean test = wf.handleSnapEntity(this, this.x, this.y);
					if(test)
					{
						snapped = true;
						a.sendSnapWavefrontMessage(identifier, wf.identifier);
						break;
					}
					
				}		
			}
		}
		
		//unsnaps itself from whatever it's snapped to
		@Override
		public void unsnap()
		{
		  if(snapped)
		  {
			//search for itself
			for(int i =0; i<snapee.ownedEntities.size();i++)
			{
				AudioEntity e = (AudioEntity) ((OwnedEntity)snapee.ownedEntities.get(i)).e;
				if(e.identifier.equals(this.identifier))
				{
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
		
		@Override
		void unsnapRemote()
		{
		  if(snapped)
		  {
			//search for itself
			for(int i =0; i<snapee.ownedEntities.size();i++)
			{
				AudioEntity e = (AudioEntity) ((OwnedEntity)snapee.ownedEntities.get(i)).e;
				if(e.identifier.equals(this.identifier))
				{
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
	
	
	//draw the artefact
		public void draw(int camx, int camy)
	  {
		  /* You MUST call this, as it handles some common behaviours.*/
		  super.draw(camx, camy);
		  
		  prevx = x;
		  prevy = y;
		 
		  //the grains shake when they are influenced, these variables accomplish that
		  float shakeX,shakeY;
		  
		  shakeX = (a.random(100.0f)/100.0f)*grainvvelopeAccum* 20.0f;
		  shakeY = (a.random(100.0f)/100.0f)*grainvvelopeAccum* 20.0f;
		  
	      if(isPlaying)
	      {
	    	//SQUARE SEARCH METHOD
				//search for squares, within a circle, within square
				//AKA Squareception Algorithm
				  for(int nx = (int)x - (int)200; nx< x + 200; nx+=60)
				  for(int ny = (int)y - (int)200; ny< y + 200; ny+=60)
				  {
					  if(nx>0 && ny>0)
					  {
					  PVector squareLocation = a.landscapeMatrix.getSquareCoordsFromWorldCoords(nx, ny);
	                
					  float gfxDist = a.dist(nx,ny,x,y);
					  
						  
						  float colorScaler = (1.0f - gfxDist/200)*2.0f;
						  
						  if(gfxDist<200)
						  {
					
					       a.landscapeMatrix.modifyElement((int)squareLocation.x, (int)squareLocation.y, 0.0f,colorScaler,colorScaler);
				          }  
				  }
				  }
	      }
		  
		  a.image(render, drawx-ch + shakeX,drawy-ch+shakeY);
		  
		
		
		 
		  
		  gainSlider.setPosition(drawx+xSpacing, drawy - ch + gainSpacing);
		  grainSizeSlider.setPosition(drawx+xSpacing, drawy - ch + grainSizeSpacing);
		  grainPositionSlider.setPosition(drawx+xSpacing, drawy - ch + grainPositionSpacing);
		  intervalSlider.setPosition(drawx+xSpacing, drawy - ch + intervalSpacing);
		  pitchSlider.setPosition(drawx+xSpacing, drawy - ch + pitchSpacing);
		
		  
		  list.setPosition(drawx + xSpacing, drawy-ch+listSpacing);
	
		  
		 
	  }
	  
	  float grainCullDistance =0.0f;
	  
	  /* this vector stores the distances of each nearby wavefront. This vector is sorted,
	   * and then the smallest distance is used for culling
	   */
	  
	  @Override
	  public void cull(Boolean toggle)
	  {
		  gsp.pause(toggle);
		  grainvelope.pause(toggle);
		  outGain.pause(toggle);
		  masterGain.pause(toggle);
		  culled = toggle;
	  }
	  
	  @Override 
	  public void process()
	  {
		  super.process();
		  //do this so the  vector is definitely initialized
	      
		  //apply the envelope to the output from previously accumulated wavefront influences, but first cap it at 0.0f
		  if(grainvvelopeAccum>1.0f)
			  grainvvelopeAccum = 1.0f;
		  
		//Grain culling! Turn on/off grains preemptively depending on how far they are from the active radius
		  grainvelopeGlide.setValue(grainvvelopeAccum);
		  
		  
		  //sort the wave distance vector to find the closest wave. Discard everything else.
		 java.util.Collections.sort(waveDistances);
		 if(waveDistances.size()>0)
		 grainCullDistance = waveDistances.get(0);
		 waveDistances.clear();
	
		  if(grainCullDistance>= 1.4f && isPlaying)
		  {
			 // grainvelope.setGain(0.0f);
			  isPlaying=false;
			// grainvelopeGlide.setValueImmediately(0.0f);
		    //  outGain.pause(true);
			  cullMute.setGain(0.0f);
		      gsp.pause(true);
		      
		  }
		  if(grainCullDistance<= 1.4f && !isPlaying)
		  {
			  isPlaying = true;
			//   masterGain.pause(false);
			//     outGain.pause(false);
		      gsp.pause(false);
		      cullMute.setGain(1.0f);
		   
		  }
		  //set accumulator envelope to zero
		  grainvvelopeAccum = 0.0f;
		  
		  
		  if(sendPositionTimer.isFinished())
	      {
			  sendPositionTimer.start();  
	      }
	  }
	  
		void play()
		{
			activated = true;
			//g/sp.setToLoopStart();
			//gsp.start();
		  
		}
	  
		public  void showContextControls()
	  {
		  super.showContextControls();
		 list.show();
		 
		  gainSlider.show();
		
		  pitchSlider.show();
		  grainPositionSlider.show();
		  grainSizeSlider.show();
		  intervalSlider.show();
		  
	  }
		public  void hideContextControls()
	  {
		  super.hideContextControls();
		  list.hide();
			
			  gainSlider.hide();
			
			  pitchSlider.hide();
			  grainPositionSlider.hide();
			  grainSizeSlider.hide();
			  intervalSlider.hide();
	  }
	  
	  @Override
	  public boolean getIsOverControllers()
	  {
		  if(lockButton.isMouseOver() || unlockButton.isMouseOver() || removeButton.isMouseOver() || pitchSlider.isMouseOver() || list.isMouseOver() || moveButton.isMouseOver() || gainSlider.isMouseOver()
				  || grainPositionSlider.isMouseOver() || grainSizeSlider.isMouseOver() || intervalSlider.isMouseOver() || cloneButton.isMouseOver())
		  return true;
		  else
		  return false; 
	  }
	  
	  @Override
	  public void startMoving()
	  {
		  moving = true;
		  unsnap();
		  //unsnap from wavefront 
		  
	  }
	  @Override
	  public void stopMoving()
	  {
		  moving = false;
		  //snap this to something if need be
		  handleSnapWavefronts();
	  }
	  
	  @Override
	  public void startMultiMoving(int mx, int my)
	  {
		 super.startMultiMoving(mx, my);
		  unsnap();
		  //unsnap from wavefront 
		  
	  }
	  
	  @Override
	  public void stopMultiMoving(int mx, int my)
	  {
		  super.stopMultiMoving(mx, my);
		  //snap this to something if need be
		  handleSnapWavefronts();
	  }
	  
	  @Override
	  public void remoteParamChange(String paramName, float value, long timeTag)
	  {
		  setParam(paramName, value);
		  
		  if(paramName.equals("gainParam"))
		   {
			  gainSlider.setValueLabel("" + value);
			  masterGainMax = value;
			  masterGainGlide.setValue(value);
			  setParam("gainParam", value);
		   }
		  
		  if(paramName.equals("pitchParam"))
		   {
			  pitchSlider.setValueLabel("" + value);
			  pitch = value;
			  pitchGlide.setValue(value);
			  setParam("pitchParam", value);
		   }
		  if(paramName.equals("grainSizeParam"))
		  {
			  grainSizeSlider.setValueLabel("" + value);
			  grainSizeGlide.setValue(value);
			  setParam("grainSizeParam", value);
		  }
		  if(paramName.equals("grainPositionParam"))
		  {
			  grainPositionSlider.setValueLabel("" + value);
			  positionGlide.setValue(value * gsp.getSample().getLength());
			  setParam("grainPositionParam", value);
		  }
		  if(paramName.equals("grainIntervalParam"))
		  {
			  intervalSlider.setValueLabel("" + value);
			  intervalGlide.setValue(value);
			  setParam("grainIntervalParam", value);
		  }
		  
	  }
	  
	  @Override
	  public float paramHandler(String paramName)
	  {
		 a.println("handling event.");
		  if(paramName.equals(gainSlider.name()))
		   {
			  masterGainGlide.setValue(gainSlider.getValue());
			  setParam("gainParam", gainSlider.getValue());
			  return gainSlider.getValue();
		   }
		  if(paramName.equals(pitchSlider.name()))
		   {
			  pitchGlide.setValue(pitchSlider.getValue());
			  setParam("pitchParam", pitchSlider.getValue());
			  return pitchSlider.getValue();
		   }
		  if(paramName.equals(grainSizeSlider.name()))
				  {
			  grainSizeGlide.setValue(grainSizeSlider.getValue());
			  setParam("grainSizeParam", grainSizeSlider.getValue());
			  return grainSizeSlider.getValue();
				  }
		  if(paramName.equals(grainPositionSlider.name()))
		  {
			  positionGlide.setValue(grainPositionSlider.getValue() * gsp.getSample().getLength());
			  setParam("grainPositionParam", grainPositionSlider.getValue());
			  	return grainPositionSlider.getValue();
		  }
		  if(paramName.equals(intervalSlider.name()))
		  {
			  intervalGlide.setValue(intervalSlider.getValue());
			  setParam("grainIntervalParam", intervalSlider.getValue());
	  return intervalSlider.getValue();
	  
		  }
		  
		  else return 0.0f;
	  }
	  //translate controller numbers into param numbers
	  @Override
	  public String translateParamName(String ctlName)
	  {
	     if(ctlName.equals(gainSlider.getName()))
	    	 return "gainParam";
	     if(ctlName.equals(list.getName()))
	    	 return "listParam";
	     if(ctlName.equals(pitchSlider.getName()))
	    	 return "pitchParam";
	     if(ctlName.equals(grainSizeSlider.getName()))
	    	 return "grainSizeParam";
	     if(ctlName.equals(grainPositionSlider.getName()))
	    	 return "grainPositionParam";
	     if(ctlName.equals(intervalSlider.getName()))
	    	 return "grainIntervalParam";
	     if(ctlName.equals(removeButton.getName()))
	    	 return "removeParam";
	     else return "";
	  }
	  
	  @Override 
	  void groupHandler(String group, float value)
	  {
		  //user chose a sample. Switch the sample;
		  if(group.equals(list.name()))
		  {
			  setParam("sampleParam", value);
			  
			  //get the sample corresponding to this number
			  try
			  {
		
			  net.beadsproject.beads.data.Sample s = ((SampleBank.Sample)samples.get((int)value)).sample;
			  gsp.setSample(s);
			  }
			  catch(Exception ex)
			  {
			  }
			  setParam("sampleParam", value);  
		  }
	  }
	  
	  @Override 
	  void remoteGroupHandler(String group, float value)
	  {
		//user chose a sample. Switch the sample;
		  if(group.equals("sampleParam"))
		  {
			  //get the sample corresponding to this number
			  try
			  {
				 
				  net.beadsproject.beads.data.Sample s = ((SampleBank.Sample)samples.get((int)value)).sample;
				  gsp.setSample(s);
			  }
			  catch(Exception ex)
			  {
			  }
			  setParam("sampleParam", value);
		  }
	  }
	  
	  @Override
	  String translateGroupHandler(String group)
	  {
		  if(group.equals(list.name()))
		  {
			  return "sampleParam";
		  }
		  return "";
	  }
	  
	
}
