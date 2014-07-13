package netmuseclient;
import processing.core.PApplet;
import controlP5.*;
import de.bezier.data.sql.*;
import java.math.*;
import java.security.*;
import java.util.*;
/**
 netMuseClient
 */
 
import oscP5.*;
import netP5.*;

/**
 * a group of controllers that can be repositioned together
 * @author bogdan
 *
 */
public class ControllerGroup
{
  ArrayList controllers;
  boolean state;
  ControlP5 cp5;
  int x; 
  int y;
  ControllerGroup(boolean state, int x, int y, ControlP5 cp5)
  {
    this.x = x;
    this.y = y;
    this.cp5 = cp5;
    this.state = state;
    controllers = new ArrayList();
  }
  
  Boolean getIsMouseOver()
  {
	  Boolean isMouseOver = false;
	  for(int i =0; i<controllers.size(); i++)
	  {
		  Controller c = (Controller)controllers.get(i);
		  if(c.isMouseOver())
		  {
			  return true;
		  }
	  }
	  return false;
  }
  
  public void addControllerTextBox(String name, String label, int x, int y, int w, int h)
  {
    
    //add the textfield to cp5
  controllers.add( cp5.addTextfield(name, this.x+x, this.y+y, w, h).setAutoClear(false).setCaptionLabel(label));
   
     
     
  }
  
  public void addControllerButton(String name, String label,int x, int y, int w, int h)
  {
    controllers.add(cp5.addButton(name,0.0f,this.x+x, this.y+y,w,h).setCaptionLabel(label));
    
  }
  
  public void addControllerSlider(String name, String label,int x, int y, int w, int h)
  {
    controllers.add(cp5.addSlider(name,0.0f,10.0f,5.0f,this.x+x, this.y+y,w,h).setCaptionLabel(label));
    
  }
  
  /**
   * hide the group
   */
  public void hide()
  {
    //hide all the controls
    for(int i =0; i<controllers.size();i++)
    {
     ((Controller)controllers.get(i)).hide();
    }
  }
  
  /**
   * show the group
   */
  public void show()
  {
    for(int i =0; i<controllers.size();i++)
    {
         ((Controller)controllers.get(i)).show();
    }
  }
  
};