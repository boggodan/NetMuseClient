package netmuseclient;
import java.util.Vector;

import oscP5.*;
import javazoom.jl.decoder.OutputChannels;
import processing.core.PApplet;
import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Slider;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import netP5.*;

/**
 * Base class for all audio entities. It doesn't really do anything on its own.
 * @author bogdan
 *
 */
public class AudioEntity 
{
	//username of the user that owns this entity
	String owner;
	Boolean moving;
	
	//hmm... if we want to move items together in a selection, we need to perform a multi move
	Boolean multiMoving;
	NetMuseClient a;
	int x,y;
	int drawx;
	int drawy;
	int cw,ch;
    
	//for multi-moving we need to remember this entity's relative position to the cursor
	int multiMoveX,multiMoveY;
	//used for autoplacing controllers;
	int ctlHeight = 13;
	int ctlSpacing = 2;
	int yIncrement = 0;
	int xSpacing = 40;
	Boolean snapped;
	ControlP5 p;
	Wavefront snapee;
	
	//context menu
	
	Button moveButton;
	int moveSpacing;
	Button removeButton;
	int removeSpacing = 0;
	Button cloneButton;
	int cloneSpacing = 0;
	Button lockButton;
	int lockSpacing = 0;
	Button unlockButton;
	int unlockSpacing = 0;
	String identifier;
	String type;
	float attenVal = 0.0f;
	boolean multiSelected = false;
	
	//remember what the previous x/y position was, so we only send reposition messages when the user has actually moved the wavefront
		int prevx,prevy;
		//we don't want to flood the server by sending move messages for every single child entity, every frame
		//send only once in a while
		Timer sendPositionTimer;
	
	boolean IS_SELECTED;
	
	//this one flows right into the audio outputs, is subject to distance attenuation and is panned
	Gain outGain;
	Glide outGainGlide;
	
	 //we should smooth the movement so it interpolates between positions
	 //that way it will be less jittery when moved by other users
	 
	 //we will only smooth the DRAWN position
	 //smoothed floating point x,y
	 float smx,smy;
	 //smooth floating point prev x,y
	 float smprevx,smprevy;
	 
	//main output gain
		//all objects have this as their output object
		
	 
	 MovLowpass xSmoother,ySmoother;
	 
	 //ownership and locking
	 String lockUser; // the user who locked this entity
	 Boolean isLocked = false;
	 //used to store the position relative to the mouse during a cloning state
	 int cloningX,cloningY;
	 
	/**
	 * automatically spaces context menu controls vertically, and returns a spacing value
	 * @return
	 */
	 public int autoSpace()
	{
		yIncrement = yIncrement + ctlHeight + ctlSpacing;
		return yIncrement;
	}
	
	/**
	 * sets the entity as selected
	 */
	 public void select()
	{
		IS_SELECTED = true;
	}
	
	/**
	 * sets the entity as not selected
	 */
	 public void unselect()
	{
		IS_SELECTED = false;
	}
	
	/**
	 * Activate the entity... override this for custom behaviour.
	 */
	 public void activate()
	{
		
	}
	
	/**
	 * unsnap the entity from it's parent wavefront.
	 */
	 public void unsnap()
	{
		
	}
	
	/**
	 * 
	 */
	  void unsnapRemote()
	{
		
	}
	
	/**
	 * Snaps the entity to a wavefront underneath it.
	 */
	 public void handleSnapWavefronts()
	{
		
	}
	
	
	
	/**
	 * 
	 * @param a the main applet
	 * @param p controlp5 instance
	 * @param ac audiocontext
	 * @param x 
	 * @param y
	 */
	 public AudioEntity(NetMuseClient a, ControlP5 p, AudioContext ac, int x, int y)
	{
		
		outGainGlide = new Glide(ac, 0.0f);
		outGain = new Gain(ac, 1, outGainGlide);
		type = "generic";
		entityParams = new Vector();
		attenVal = 0.0f;
		sendPositionTimer = new Timer(100, a);
		prevx=prevy =0;
		//give this entity a unique identifier
		  identifier = "" + (int)a.random(Integer.MAX_VALUE);
		  this.p = p;
		  this.a = a;
		 
		  
		  xSmoother = new MovLowpass();
		  ySmoother = new MovLowpass();
		  
		  //a.println(identifier);
		  cw = 15;
		  ch = 15;
		  this.x = x;
		  this.y = y; 
		  smx =x;
		  smy =y;
		  xSmoother.z=x;
		  ySmoother.z=y;
		  multiMoveX=multiMoveY=0;
		  moving = false;
		  multiMoving = false;
		  	moveSpacing = autoSpace();
		    moveButton =  p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Move");
			removeSpacing = autoSpace();
			removeButton =  p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Remove");
			cloneSpacing = autoSpace();
			cloneButton =  p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Clone");
			lockSpacing = autoSpace();
			lockButton =  p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Lock");
			unlockSpacing = autoSpace();
			unlockButton =  p.addButton("" + a.random(Integer.MAX_VALUE),0.0f,x + ch*2, y-ch+ yIncrement,100, 13).setCaptionLabel("Unlock");
	}
	
	/**
	 * sets the entity's owner user
	 * @param owner
	 */
	 public void setOwner(String owner)
	{
		this.owner = owner;
	}
	
	 public Boolean usesAttenuation = false;
	  void setUsesAttenuation()
	  {
		  usesAttenuation = true;
	  }
	  
	  
	  /* this function processes the internal logic of the entity. This function must be called in a timed loop on another thread,
	   * and is part of the MVC architecture.
	   */
	  public void process()
	{
		  //if not using attenuation, then outgain will always be 1.0
		 if(!usesAttenuation)
		 {
			 outGainGlide.setValue(1.0f);
		 }
		 else //otherwise, calculate attenuation
		 {
			 handleDistanceAttenuation(a.cameraX + a.screenW/2, a.cameraY + a.screenH/2);
		 }
		 
	}
	  
	  
	  /**
	   * draws the entity's graphics
	   * @param camx
	   * @param camy
	   */
	  public void draw(int camx, int camy)
	  {
		
		 if(moving)
		  {		 
		  x = a.mouseX + camx;
		  y = a.mouseY + camy;  
		    	 if(sendPositionTimer.isFinished())
			  a.sendMovedAudioEntity(identifier, x, y);	     
		  }
		 
		 
		 if(multiMoving)
		 {
			 x = a.mouseX + multiMoveX + camx;
			 y = a.mouseY + multiMoveY + camy;
			 //LOL?
			 if(sendPositionTimer.isFinished())
			 a.sendMovedAudioEntity(identifier, x, y);
		 }
	
		 smx = x;
		 smy = y;
		 
		 float smoothx,smoothy;
		 
		 smoothx = xSmoother.Process(smx);
		 smoothy = ySmoother.Process(smy);
		
		  drawx = (int)smoothx - camx;
		  drawy = (int)smoothy - camy;
		  
		  //if this object has been multi-selected
		  if(multiSelected)
		  {
			  a.noFill();
			  a.strokeWeight(1.0f);
			  a.stroke(0,0,0,100);
			  a.rect(drawx-cw,drawy-ch,cw*2, ch*2);
		  }
		  
		  moveButton.setPosition(drawx + xSpacing,drawy-ch+ moveSpacing);
		  removeButton.setPosition(drawx + xSpacing,drawy-ch+ removeSpacing);
		  cloneButton.setPosition(drawx + xSpacing,drawy-ch+ cloneSpacing);
		  lockButton.setPosition(drawx + xSpacing,drawy-ch+ lockSpacing);
		  unlockButton.setPosition(drawx + xSpacing,drawy-ch+ unlockSpacing);
		  
		  if(isLocked)
		  {
			  if(lockUser.equals(a.userDetails.username))
			  {
				 a.tint(0,255,0,100);
			  }
			  else
			  {
				  a.tint(255,0,0,100);
			  }
			  a.image(a.lockIcon,drawx + ch + 1, drawy + ch + 1 ); 
			  a.noTint();
		  }
	  }
	  
	  
	  Boolean culled = false;
	  
	 /**
	  * turns the entity's audio off
	  * @param toggle
	  */
	  public void cull(Boolean toggle)
	  {
		  
	  }
	  
	  /**
	   * sets the entity as locked
	   */
	  public void lock()
	  {
		  isLocked = true;
	  }
	  
	  /*
	   * sets the entity as not locked
	   */
	  public void unlock()
	  {
		  isLocked = false;
	  }
	  
	  /** 
	   * returns true if the entity belongs to this user, i.e. it is not locked or is locked with this username
	   * @return
	   */
	  public Boolean getIsMine()
	  {
		  if(isLocked && !lockUser.equals(a.userDetails.username))
			  return false;
		  else return true;
	  }
	  
	/**
	 * the sound is attenuated with reference to the centre of the user's screen.
	 * this function will also be used to turn off distant audio entities.
	 * If usesAttenation is set to true, then you must call this function in your draw/process
	 * function, otherwise the entity will not make a sound! If usesAttention is false, than outGain will
	 * always have maximum volume.
	 * @param nx
	 * @param ny
	 */
	  void handleDistanceAttenuation(float nx, float ny)
	  {
		  //distance from centre to object
		  float d = a.dist(nx,ny, this.x, this.y);
		  
		  //i capped the distance to 80, so it doesn't fade in very quickly towards 0
		  float denom =a.pow(d/300.0f,3);
		  if(denom<1.0)
			  denom = 1.0f;
		  
		  attenVal = (1.0f/denom);
		  //cap it at 1.0f
		  if(attenVal>1.0f)
			  attenVal=1.0f;
		  //anything below 0.01 should be turned to 0.0f, so we can treat it as inaudible and cull it
		  if(attenVal<0.005f)
			  attenVal = 0.005f;
		  
		  outGainGlide.setValue(attenVal);
		  
	  }
	  
	  /**
	   * shows the context menu
	   */
public void showContextControls()
	  {
		  moveButton.show();
		  removeButton.show();
		  cloneButton.show();
		  lockButton.show();
		  unlockButton.show();
		  
	  }
/**
 * hides the context menu
 */
public void hideContextControls()
	  {
		  moveButton.hide();
		  removeButton.hide();
		  cloneButton.hide();
		  lockButton.hide();
		  unlockButton.hide();
	  }
	  
	  /**
	   * show the controls for a group selection context menu
	   */
      public void showGroupContextControls()
	  {
		  moveButton.show();
		  removeButton.show();
		  cloneButton.show();
		  lockButton.show();
		  unlockButton.show();
		  
	  
	  
	  }
      /**
       * hide the controls for a group selection context menu
       */
      public void hideGroupContextControls()
	  {
		  moveButton.hide();
		  removeButton.hide();
		  cloneButton.hide();
		  lockButton.hide();
		  unlockButton.hide();
	  }
	  
      /**
       * is this mouse cursor over this entity' conext menu?
       * @return
       */
      public boolean getIsOverControllers(){
		  return true;
	  }
	  
	  /**
	   * set the entity as moving
	   */
      public void startMoving()
	  {
		  moving = true;
	  }
	  
	  /**
	   * set the entity as not moving
	   */
      public void stopMoving()
	  {
		  moving = false;
	  }
	  
	  /**
	   * sets the entity as moving within a selection group
	   * @param mx
	   * @param my
	   */
      public void startMultiMoving(int mx, int my)
	  {
		  multiMoveX = drawx - mx;
		  multiMoveY = drawy - my;
		  multiMoving = true;
	  }
	  
	  /**
	   * sets the entity as not moving within a selection group
	   * @param mx
	   * @param my
	   */
      public void stopMultiMoving(int mx, int my)
	  {
		  multiMoving = false;
	  }
	  
	  /**
	   * move to some position
	   * @param x
	   * @param y
	   */
      public void move(int x, int y){
		  
		  this. x= x;
		  this. y =y;
	  }
	  
	  /**
	   * get the entity's identifier
	   * @return
	   */
      public String getEntityID()
	  {
		 return identifier;  
	  }
	  
	  /**
	   * returns the output UGen
	   * @return
	   */
      public Gain getOutput()
	  {
		  
		  return outGain;
	  }
	  
	  /**
	   * parameter handles
	   * @param paramName
	   * @return
	   */
      public float paramHandler(String paramName)
	  {
		  return 0.0f;
	  }
	  
	  /**
	   * remote parameter handler
	   * @param paramName
	   * @param value
	   * @param timeTag
	   */
      public void remoteParamChange(String paramName, float value, long timeTag)
	  {
		  
	  }
	  
      public void handleResync(long timeTag)
	  {
	  }
	  
	  
	  /**
	   * send an OSC parameter change message
	   * @param p5
	   * @param serverIP
	   * @param user
	   * @param paramName
	   * @param identifier
	   * @param value
	   */
      public void sendOscParamChange(OscP5 p5, NetAddress serverIP, String user, String paramName, String identifier, float value)
	  {
		 OscMessage m = new OscMessage("/paramChange");
		 m.add(user);
		 m.add(identifier);
		 m.add(paramName);
		 m.add(value);
		 		 
		 
		 p5.send(m, serverIP);
	  }
	  
	  /**
	   * translate a parameter's name 
	   * @param ctlName
	   * @return
	   */
      public String translateParamName(String ctlName)
	  {
		  return "";
	  }
	  
	  void groupHandler(String group, float value)
	  {
		  
	  }
	  
	  void remoteGroupHandler(String group, float value)
	  {
		  
	  }
	  
	  String translateGroupHandler(String group)
	  {
		  return "";
	  }
	  
	  /** 
	   */
	  
	  
	  /**the EntityParam class is used to store named parameters for each type of entity,
	   * in order to facilitate cloning. This is independent from the parameter handling. 
	   * Define a parameter with defParam().
	   * @author bogdan
	   *
	   */
	 public class EntityParam
	  {
		  
		  String name;
		  float value;
		  //can be "group" or "param"
		  String type;
		  EntityParam(String name, String type)
		  {
			this.name = name;
			this.type = type;
			value = 0.0f;
		  }
		  
		  void setValue(float newVal)
		  {
			  value = newVal;
		  }
		  
	  };
	  
	  Vector entityParams;
	  
	  /**
	   * define a new parameter for this entity
	   * @param name
	   * @param type
	   */
	  public void defParam(String name, String type)
	  {
		  entityParams.add(new EntityParam(name, type));
	  }
	  
	  /**
	   * set a parameter for this entity
	   * @param name
	   * @param value
	   */
	  void setParam(String name, float value)
	  {
		  for(int i =0; i<entityParams.size(); i++)
		  {
			  EntityParam ep = (EntityParam)entityParams.get(i);
			  if(ep.name.equals(name))
			  {
				  ep.setValue(value);
				  break;
			  }
		  }
	  }
	  
	  /**
	   * returns a vector of the current parameters.
	   * @return
	   */
	  Vector getClonedParameters()
	  {
		  return entityParams;
	  }
}
