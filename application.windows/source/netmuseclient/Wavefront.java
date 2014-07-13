package netmuseclient;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.buffers.CosineWindow;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.WavePlayer;
import netP5.NetAddress;
import processing.core.PApplet;
import processing.core.PVector;
import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Slider;
import controlP5.Slider2D;
import controlP5.Textlabel;
import java.util.*;

import javax.sound.midi.SysexMessage;

import oscP5.OscBundle;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;

/* this object puts out periodic waves that trigger audio entities. 
 * The timing system for this object is serverside, so the server will send
 * a message to the client every time a wavefront object activates.
 * this will ensure that all the users are more or less synchronized regardless
 * of when they joined. 
 */
public class Wavefront extends AudioEntity
{


	//timer that waits for resync
	Timer resyncTimer;
	//timer that makes the wavefronts go woooshh awesomely
	Timer wavefrontTimer;
	//appears upon right clicking the entity
	
	//String identifier;
	Slider rateSlider;
	Slider distSlider;
	Button activateButton;
	Button targetButton;
	
	int rateSpacing;
	int distSpacing;
	int activateSpacing;
	int targetSpacing;
	
	//the target wavefront, so we can draw some cool gfx showing the connection
	Wavefront target;
	
	float rate;
	float distance;
	float currentRadius;
	//is it currently in the process of expanding a wavefront
	Boolean active;
	//is it currently in a state of wavefrontage
	Boolean state;
	ArrayList triggeredEntities;
	
	
	
	int timer1;
	int timerTemp;
	
	boolean waitingForSync = false;
	
	//a list of references to the objects that this wavefront owns, and where they are, so we can keep them in sync
	//when the wavefront is resized or moved.
	Vector ownedEntities;
	
	
	/*we have to detect when the distance slider has been released to perform a supersync.
	 * This boolean holds whether the slider is being held down or not between frames, so 
	 * we can detect a change in its state to detect when it has been released.
	 */
	boolean resizeSliderUsage = false;
	
	//we can have less OSC traffic by only sending resize messages once in a while
		Timer sendResizeTimer;
		
	Wavefront(NetMuseClient a, ControlP5 p, AudioContext ac, int x, int y)
	{
		
		super(a, p, ac, x, y);
		setUsesAttenuation(); //just for culling 
		ownedEntities = new Vector();
		resyncTimer = new Timer(10,a);
		wavefrontTimer = new Timer(1000,a);
		triggeredEntities = new ArrayList();
		timer1 = 0;
		timerTemp = 0;
		 currentRadius = 0.0f;
		 rate = 1000.0f;
		 distance = 500.0f;
	active = false;
		rateSpacing = 0;
		 state = true;
	
		rateSpacing = autoSpace();
		rateSlider =  p.addSlider("" + a.random(Integer.MAX_VALUE),0.0f,10000.0f,1000.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Rate").setColorLabel(a.color(0,0,0));;
		distSpacing = autoSpace();
		distSlider =  p.addSlider("" + a.random(Integer.MAX_VALUE),1.0f,1000.0f,500.0f,x+ch*2, y -  ch + yIncrement, 100,13).setCaptionLabel("Distance").setColorLabel(a.color(0,0,0));;
		activateSpacing = autoSpace();
		activateButton = p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Activate");
		targetSpacing = autoSpace();
		targetButton = p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Target Wavefront");
		
	 sendResizeTimer = new Timer(100,a);
		defParam("rateParam", "param");
		setParam("rateParam", 1000.0f);
		defParam("distParam", "param");
		setParam("distParam", 500.0f);
		type = "Wavefront";
		
		rateSlider.setValue(1000.0f);
		distSlider.setValue(300.0f);
		
	    hideContextControls();
	    //a.sendNewAudioEntity("Wavefront", identifier, a.mouseX + a.cameraX, a.mouseY+a.cameraY);
	}
	
	//sends out a wave
	@Override
	public void activate()
	{
		currentRadius = 0.0f;
		active = true;
		triggeredEntities.clear();
		
		timer1 = a.millis();
		timerTemp = a.millis();
	}
	
	//we need to override the stopMoving method so we can update the final position of child entities
	//this is because we're sending the move messages for each entity on a timer so they might not necessariyl be in the right spot when the
	//user stops moving the wavefront before the timer elapsed. This ensures consistency.
	
	@Override
	public void stopMoving()
	{
		super.stopMoving();
		a.console.println("DEBUG: Sent wavefront supersync.", a.color(0,0,255));
		for (int i = 0; i<ownedEntities.size();i++)
  		{
			OwnedEntity oe = (OwnedEntity)ownedEntities.get(i);
			a.sendMovedAudioEntity(oe.e.identifier, oe.e.x, oe.e.y);
  		}
	}
	
	
	  float spd;
	  float timeElapsed;
	  float increment;
	  float binSize;
	  //this is the visibible radius that actually started from the first bin rather than the centre
	  float activeRadius;
	
	  @Override
	  public  void cull(Boolean toggle)
	  {
		  
		  culled = toggle;
	  }
	  
	//draw the artefact
	  public  void draw(int camx, int camy)
	  {
		  
		  super.draw(camx, camy);
		  
		  
		  
		  //user stopped resizing, perform supersync
		  if(resizeSliderUsage == true && !distSlider.isMousePressed())
		  {
			  
			  for (int i = 0; i<ownedEntities.size();i++)
		  		{
		  			
		  		   OwnedEntity oe = (OwnedEntity)ownedEntities.get(i);
		  		   
		  		   
		  		   oe.repositionOwnedEntity(x,y);
		  		
		  		      
		  		//        a.println("sent move message");
		  		        a.sendMovedAudioEntity(oe.e.identifier, oe.e.x, oe.e.y);
		  		        
		  		      
		  		   }
		  }
		  
		  resizeSliderUsage = distSlider.isMousePressed();
		  
		  
		  if(distSlider.isMousePressed())
		  {
			  for (int i = 0; i<ownedEntities.size();i++)
		  		{
		  			
		  		   OwnedEntity oe = (OwnedEntity)ownedEntities.get(i);
		  		   
		  		   
		  		   oe.repositionOwnedEntity(x,y);
		  		
		  		      if(sendResizeTimer.isFinished())
		  		      {
		  		//        a.println("sent move message");
		  		        a.sendMovedAudioEntity(oe.e.identifier, oe.e.x, oe.e.y);
		  		        
		  		      }
		  		   }
		  		if(sendResizeTimer.isFinished())
	  		      {
		  			sendResizeTimer.start();  
	  		      }
		  }
		  
		 
		  
		  if(moving)
		  {		
		  //move all the owned entities as well
		  if(x!=prevx || y!=prevy)
		  {
			  
		  		for (int i = 0; i<ownedEntities.size();i++)
		  		{
		  			
		  		   OwnedEntity oe = (OwnedEntity)ownedEntities.get(i);
		  		   oe.repositionOwnedEntity(x,y);
		  		      if(sendPositionTimer.isFinished())
		  		      {
		  		        a.sendMovedAudioEntity(oe.e.identifier, oe.e.x, oe.e.y);
		  		        
		  		      }
		  		   }
		  	
		  }
		  }
		  
		  prevx = x;
		  prevy = y;
		
		  if(sendPositionTimer.isFinished())
	      {
			  sendPositionTimer.start();  
	      }
		
		  	
		  a.strokeWeight(1.0f);
		  a.stroke(200,200,200,200);
		  a.fill(255,20,20,200);
		  a.ellipse(drawx,drawy,ch,cw);
		  
		
		  rateSlider.setPosition(drawx+xSpacing, drawy-ch + rateSpacing);
		  distSlider.setPosition(drawx + xSpacing, drawy-ch + distSpacing);

		  activateButton.setPosition(drawx + xSpacing,drawy-ch+ activateSpacing);
		  targetButton.setPosition(drawx + xSpacing,drawy-ch+ targetSpacing);
		 
		  
		  
		  if(active)
		  {
			  
			  float gridScaler = distance/8.0f;
			 // a.println(currentRadius);
			  
			//NORMAL DRAWING
			  
			  a.fill(0,0,0,0);
			  a.strokeWeight(3.0f);
			  a.stroke(255,0,0,200);
		
			  
			  a.ellipse(drawx,drawy,activeRadius,activeRadius);
			  
			  a.stroke(0,255,0,200);
			 // a.ellipse(drawx,drawy, activeRadius - gridScaler*1.4f, activeRadius-gridScaler*1.4f);
			 // a.ellipse(drawx,drawy, activeRadius + gridScaler*1.4f, activeRadius+gridScaler*1.4f);
			  
			  synchronized(a.audioEntities)
			  {
			  //we have to remember which entities we triggered in each cycle, so we don't overtrigger
			  
			  
			  //if the user is in the area of this wavefront, draw its grid
			  //float thisRadius = 0.0f;
			  float mouseDist = a.dist(a.mouseX, a.mouseY, drawx, drawy);
			  
			  
			  if(mouseDist<distance/2)
			  {
			  float radialInterval = distance / 16.0f;
			  
			  a.noFill();
			  
			  for(int i =1; i <17; i++)
			  {
				  if(mouseDist*2 > radialInterval*(i-1) + radialInterval/2 && mouseDist*2 < radialInterval*(i) + radialInterval/2)
				  a.stroke(0,0,255,80);
				  else
				  a.stroke(0,0,255,40);
			  a.ellipse((float)drawx,(float)drawy, radialInterval*i,radialInterval*i);
			  
			  }
			  }
		  }
			
  
			//SQUARE SEARCH METHOD
			//search for squares, within a circle, within square
			//AKA Squareception Algorithm
			  for(int nx = (int)x - (int)distance; nx< x + distance; nx+=60)
			  for(int ny = (int)y - (int)distance; ny< y + distance; ny+=60)
			  {
				  if(nx>0 && ny>0)
				  {
				  PVector squareLocation = a.landscapeMatrix.getSquareCoordsFromWorldCoords(nx, ny);
                  
				  float gfxDist = a.dist(nx,ny,x,y);
				  
					  
					  float colorScaler = (1.0f - gfxDist/distance)*3.0f;
					  
					  if(gfxDist<distance)
					  {
				       a.landscapeMatrix.modifyElement((int)squareLocation.x, (int)squareLocation.y, colorScaler,0.0f,colorScaler);
			          }  
			  }
			  }
			 
			  
			  

			  
		  }
		  
	  }
	  
	  @Override 
	  public  void process()
	  {
		  super.process();
		  if(active)
		  {
			//calculate the speed for this based on rate and distance
			  //a.println("Calculating speed.");
			  //this has to be framerate independent, so we need to time it
			  //spd is the speed in pixels per millisecond
			   spd = distance/(rate);
			  
			  timer1 = a.millis();
			   timeElapsed = timer1-timerTemp;
			  timerTemp = timer1;
			   increment = timeElapsed * spd;
			  currentRadius+= increment;
			  binSize = distance/16;
			  //this is the visibible radius that actually started from the first bin rather than the centre
			   activeRadius = currentRadius+binSize;
			   
			   if(currentRadius > distance)
				  {
					  active = false;
				  }
			   
			   for(int i = 0; i <a.audioEntities.size();i++)
				  {
					 //get an item. We don't know what it is yet or what to do to it.
					 //a.println(a.audioEntities.get(i).getClass().getName());
					  if(a.audioEntities.get(i).getClass().getName().equals("netmuseclient.Bloop"))
					  {
						  Bloop h = (Bloop)a.audioEntities.get(i);
						  float distance = a.dist((float)h.x, (float)h.y, (float)x, (float)y) *2.0f;
						  if(distance < activeRadius)
						  {
							  
							  //have we triggered this before
							  Boolean isTriggered = false;
							  for(int j =0; j < triggeredEntities.size(); j++)
							  {
								  AudioEntity p = (AudioEntity)triggeredEntities.get(j);
								  if(h.identifier.equals(p.identifier))
								  {
								  isTriggered = true;
								  break;
								  }
							  }
							  if(!isTriggered)
							  {
							  h.envelopeTrigger();
							  triggeredEntities.add(h);
							  //a.console.println("Triggered...");
							  }
							  
							  
						  }
						  
				      }
					  
					  if(a.audioEntities.get(i).getClass().getName().equals("netmuseclient.Sample"))
					  {
						  Sample h = (Sample)a.audioEntities.get(i);
						  float distance = a.dist((float)h.x, (float)h.y, (float)x, (float)y) *2.0f;
						  if(distance < activeRadius)
						  {
							  
							  //have we triggered this before
							  Boolean isTriggered = false;
							  for(int j =0; j < triggeredEntities.size(); j++)
							  {
								  AudioEntity p = (AudioEntity)triggeredEntities.get(j);
								  if(h.identifier.equals(p.identifier))
								  {
								  isTriggered = true;
								  break;
								  }
							  }
							  if(!isTriggered)
							  {
								 // a.println("OMg triggered!");
							  h.play();
							  triggeredEntities.add(h);
							  //a.console.println("Triggered...");
							  }
							  
							  
						  }
						  
				      }
					  
					  if(a.audioEntities.get(i).getClass().getName().equals("netmuseclient.Grain"))
					  {
						  Grain g = (Grain)a.audioEntities.get(i);
						  float d = a.dist((float)g.x, (float)g.y, (float)x, (float)y) *2.0f;
						  float gridScaler = distance/13.0f;
						  if(d>activeRadius - gridScaler && d<activeRadius + gridScaler)
						  {
							  float velValue = 1.0f - Math.abs(d-activeRadius)/gridScaler;
							  g.grainvvelopeAccum+=velValue;
						  }
						  
						  //set the cull distance so we can pre-emptively turn off the grain on there
						  float cullDistance = Math.abs(d - activeRadius)/gridScaler;
						  try
						  {
						  g.waveDistances.add((Float)cullDistance);
						  }
						  catch(Exception ex)
						  {
							  ex.printStackTrace();
						  }
						 // g.grainCullDistance = cullDistance;
				      }
					  
				  }
			   
		  }
	  }
	  
	  public  void showContextControls()
	  {
		  super.showContextControls();
		  activateButton.show();
		  rateSlider.show();
		  distSlider.show();
		  targetButton.show();
		  
	  }
	  public  void hideContextControls()
	  {
		  super.hideContextControls();
		  activateButton.hide();
		  distSlider.hide();
		  rateSlider.hide();
		  targetButton.hide();
	  }
	  
	  @Override
	  public  boolean getIsOverControllers()
	  {
		  if(targetButton.isMouseOver() || lockButton.isMouseOver() || unlockButton.isMouseOver() || removeButton.isMouseOver() || cloneButton.isMouseOver() || rateSlider.isMouseOver() || moveButton.isMouseOver() || distSlider.isMouseOver() || activateButton.isMouseOver())
		  return true;
		  else
		  return false; 
	  }
	  
	  @Override
	  public void remoteParamChange(String paramName, float value, long timeTag)
	  {
		  setParam(paramName, value);
		  if(paramName.equals("rateParam"))
		   {
			rateSlider.setValueLabel("" + value);
			rate = value;
			
				
             if(!a.sessionUpdating)
             {
             
		     wavefrontTimer = new Timer((int)rate,a);
			  wavefrontTimer.start();
			  activate();
			  setParam("rateParam", value);
			  state = true;
             }
			
		   }
		  if(paramName.equals("distParam"))
		   {
			distSlider.setValueLabel("" + value);
			float oldDist = distance;
			distance = value;
			setParam("distParam", value);
			for (int i = 0; i<ownedEntities.size();i++)
	  		{
	  		   OwnedEntity oe = (OwnedEntity)ownedEntities.get(i);
	  		   oe.refreshEntityPosition(x, y, distance/oldDist);
	  		}
			
			if(!a.sessionUpdating)
            {
		     wavefrontTimer = new Timer((int)rate,a);
			  wavefrontTimer.start();
			  activate();
			  state = true;
            }
		   }
		  if(paramName.equals("activateParam"))
		   {
			
			//state= !state;
			  if(!a.sessionUpdating)
	             {
		     wavefrontTimer = new Timer((int)rate,a);
			  wavefrontTimer.start();
			  activate();
			  state = true;
	             }
		   }
	  }
	  
	  //NOT USED ANYMORE
	  /*this function is left here for future work. It was used in the synchronized wavefronts 
	   * implementation. (non-Javadoc)
	   * @see netmuseclient.AudioEntity#handleResync(long)
	   */
	  @Override
	  public void handleResync(long timeTag)
	  {
	/* this is a finnicky one. We have to use the rate, distance and time to tag to figure out
	 * when the next activation should occur, and then activate wavefront (turn on the local timer).
	 * The wavefront should then theoretically become in sync with all the other users, assuming
	 * that everyone's system clock is correct
	 */
		  
		  long currentTime = System.currentTimeMillis();
		  long addedTime = timeTag;
		  //a.console.println("Debug: RESYNC. Time tag: " + timeTag );
		  
		  //time it takes the wavefront to complete a cycle
		  int timeToCompletion = (int)rate;
		  a.println("Time to Completion: " + timeToCompletion);
		  //time already elapsed due to propagation latency
		  long timeElapsed = currentTime  - addedTime;
		  
		  //if we're dealing with a time tag long ago
		  if(timeElapsed>timeToCompletion)
		  {
			  timeElapsed = timeToCompletion - (timeElapsed%timeToCompletion);
		  }
		  a.println("Time elapsed: " + timeElapsed);
		  //time we need to wait until turning on this wavefront
		  
		  int ETA = timeToCompletion - Integer.valueOf("" + timeElapsed);
		  a.println("ETA: " + ETA);
		  //a.console.println("DEBUG: Local time is " + currentTime);
		  //a.console.println("DEBUG: ETA = " + ETA + "WAITING FOR RESYNC.");
		  
		  //turn on the wait timer in another thread
		  waitingForSync = true;
		  resyncTimer = new Timer(ETA, a);
		  resyncTimer.start();
	  }
	  
	  //not used in the current sync implementation
	  void handleTimer()
	  {
		  
			 
	
		  
		
		 
		  if(wavefrontTimer.isFinished())
		  {
			  activate();
			  wavefrontTimer.start();
		  }
		  
		  
	  
	  }
	  
	  
	  @Override
	  public  float paramHandler(String paramName)
	  {
		 
		  if(paramName.equals(rateSlider.name()))
		   {
			  rate = rateSlider.getValue();
			  //wavefrontTimer = new Timer((int)rate,a);
			  if(state)
				 {
			     //wavefrontTimer = new Timer((int)rate,a);
			     //wavefrontTimer.start();
				 }
			  activate();
			  
			  setParam("rateParam", rateSlider.getValue());
			  
			  return rate;
			  
		   }
		  
		  if(paramName.equals(distSlider.name()))
		   {
			  
			  for (int i = 0; i<ownedEntities.size();i++)
		  		{
		  		   OwnedEntity oe = (OwnedEntity)ownedEntities.get(i);
		  		   oe.resizeOwnedEntity((float)distSlider.getValue()/(float)distance,x,y);
		  		}
			  
			  distance = distSlider.getValue();
			  
			  
			  
			  if(state)
				 {
			     //wavefrontTimer = new Timer((int)rate,a);
			     //wavefrontTimer.start();
				 }
			 // a.println("Distance is: " + distance);
			  activate();
			  
			  setParam("distParam", distSlider.getValue());
			  return distance;
			 
		   }
		  
		  if(paramName.equals(activateButton.name()))
		   {
			 a.println("Activated wavefront.");
			 //state = !state;
			 
			 if(state)
			 {
		    // wavefrontTimer = new Timer((int)rate,a);
		     //wavefrontTimer.start();
			 }
			 activate();
		   }
		  
		  if(paramName.equals(targetButton.name()))
		   {
			 a.console.println("Pick another wavefront to target!", a.color(0,255,10));
			 a.startWavefrontTargetChoice();
		   }
		 
		   return 0.0f;
	  }
	  //translate controller numbers into param numbers
	  @Override
	  public  String translateParamName(String ctlName)
	  {
		    if(ctlName.equals(rateSlider.getName()))
		    	 return "rateParam";
		     if(ctlName.equals(distSlider.getName()))
		    	 return "distParam";
		     if(ctlName.equals(activateButton.getName()))
		    	 return "activateParam";
		     if(ctlName.equals(removeButton.getName()))
		    	 return "removeParam";
		     else return "";
		     
	    
	  }
	  
	  /*this is where it gets crazy. The wavefront needs to be time synced, so this function is ovverriden
	 to tell the server when this param change occured. This is used to time sync the wavefronts between computers,
	  by calculating when the next activation will occur and telling everyone about it. 
	 
	 */
	  @Override
	//if a param change occurs, we use this function to inform the server
	  public void sendOscParamChange(OscP5 p5, NetAddress serverIP, String user, String paramName, String identifier, float value)
	  {
		 OscMessage m = new OscMessage("/paramChange");
		
		 
		 
		 
		 m.add(user);
		 m.add(identifier);
		 m.add(paramName);
		 m.add(value);
		  
		 //a.console.println("Sent param time: " + System.currentTimeMillis());

		//time tag as a string
		 m.add("" + System.currentTimeMillis());

		 p5.send(m, serverIP);
	  }
	  
	//an entity and it's position on the wavefront's grid
		class OwnedEntity
		{
			//reference to original entity;
			AudioEntity e;
		  float rad,ang;
		  OwnedEntity(AudioEntity e, float x, float y, float ox, float oy, float dist)
		  {
			  this.e = e;
			  
			  PVector v1 = new PVector(x-ox, y-oy);
			  PVector v2 = new PVector(100,0); 
			  
			  if(y-oy > 0)
			  ang = PVector.angleBetween(v1, v2);
			  else
			  ang = -PVector.angleBetween(v1, v2);
			  rad = dist;
			  
		  }
		  
		  /*called when the wavefront is moved
		   * to ensure that any owned entities are moved along
		   */
		  void repositionOwnedEntity(int x, int y)
		  {
			  e.x = (int)(Math.cos(ang)*rad) + x;	
			  e.y = (int)(Math.sin(ang)*rad) + y;
		  }
		  
		  /*called when the wavefront is resized, to make sure
		   * that any owned entities are also moved to their relative positions.
		   * Must be called before the wavefront is actually resized, so that
		   * the current distance is the old one for this function
		   */
		  void resizeOwnedEntity(float newDist, int x, int y)
		  {
			  rad = rad*newDist;
			  e.x = (int)(Math.cos(ang)*rad) + x;	
			  e.y = (int)(Math.sin(ang)*rad) + y;
		  }
		  
		  void snapToGrid(int bin, int x, int y, float wfDistance)
			{
				
				//snap to closest grid circle
				  float binSize = wfDistance/16.0f;
				  
				  //what grid circle is this in?
				  for(int i =1; i<17; i++)
				  {
					  if(rad > binSize*(i-1) + binSize/2 && rad < binSize*(i) + binSize/2)
					  {
						rad = i*binSize;  
					  break;
					  }
				  }
				  
				 
				  e.x = (int)(Math.cos(ang)*rad) + x;	
				  e.y = (int)(Math.sin(ang)*rad) + y;
				  
			}
		  
		  //refresh this entity's position after remote resize
		  void refreshEntityPosition(float x, float y, float newDist)
		  {
			rad = rad*newDist;
		  }
		  
		}
		
		
	
	  //snap an entity into this wavefront's grid. Snappable entities are things like harmonics, samples, grains.
	  boolean handleSnapEntity(AudioEntity e, int x, int y)
	  {
		  float dist = a.dist(x, y, this.x, this.y);
		  if(dist<distance/2)
		  {
			  //snap this to a grid bin
			  //this feature is active by default for now
			  
			  float numBin = (distance)/dist * 16;
			  numBin = 200;
			  
			  PVector v1 = new PVector(x-this.x, y - this.y);
			  PVector v2 = new PVector(100,0); 
			  
			  float ang;
			  
			  if(y-this.y > 0)
			  ang = PVector.angleBetween(v1, v2);
			  else
			  ang = -PVector.angleBetween(v1, v2);
			  
			  
			  //DAFUQ?!?!?!
			  //float nx = (int)(Math.cos(ang)) * numBin + x;	
			  //float ny = (int)(Math.sin(ang)) * numBin + y;
			  
			  
			  //a.console.println("Entity snapped!");
			  //i guess we'll have to store it as relative coordinates. Polar should do fine.
			  OwnedEntity oe = new OwnedEntity(e, x, y, this.x,this.y, dist);
			  oe.snapToGrid(0, this.x, this.y, distance/2);
			  
			  
			  e.snapee = this;
			  ownedEntities.add(oe);	
			  return true;
		  }
		  return false;
	  }
	  
	  
	  
}
