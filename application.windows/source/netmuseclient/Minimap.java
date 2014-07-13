package netmuseclient;
import java.util.*;

import com.lowagie.text.pdf.draw.DottedLineSeparator;

/**
 * implements the minimap
 * @author bogdan
 *
 */
public class Minimap 
{
	//a thing is a spot on the map
	Vector things;
	NetMuseClient a;
	Boolean isMouseOver;
	//width, height of minimap
	int w,h;
	//minimap position (it will generally be in the lower right corner)
	int x,y;
	float rw,rh;
	float blobSize;
	Boolean getIsMouseOver()
	{
		if(a.mouseX > x && a.mouseX < x+w && a.mouseY > y && a.mouseY < y+h)
		{
			isMouseOver = true;
		return true;
		}
		else
		{
			isMouseOver = false;
		return false;
		}
			
			
	}
	
   Minimap(NetMuseClient a, int x, int y, int w, int h)
   {
	   this.a = a;
	   this.w=w;
	   this.h=h;
	   this.x=x;
	   this.y=y;
	   rw = ((float)a.screenW / (float)a.worldWidth) * w;
	   rh = ((float)a.screenH / (float)a.worldHeight) * h;
	   things = new Vector();
   }
   
   /**
    * draw the minimap
    */
   public void draw()
   {
	   a.stroke(255,255,255,255);
	   a.strokeWeight(2.0f);
	   getIsMouseOver();
	   
	   if(!isMouseOver)
	   {
		   a.fill(30,30,30,100);
	   }
	   else a.fill(30,30,30,200);
	   a.rect(x,y,w,h);
	   
	   
	   //player view screen. Where are we?!
	   
	   float px,py;
	   px = ((float)a.cameraX / (float)a.worldWidth)*w; 
	   py = ((float)a.cameraY /(float)a.worldHeight)*h; 
	   
	
	   //draw the viewport on the map
	   
	   a.stroke(255,255,255,200);
	   a.noFill();
	   a.rect(px + x, py + y, rw,rh);
	   
	   //draw ALL the things
	   
	   for(int i =0; i <a.audioEntities.size();i++)
	   {
		   float rx,ry;
		   
		   AudioEntity e = (AudioEntity)a.audioEntities.get(i);
		   
		   rx = e.x * (w/(float)a.worldWidth);
		   ry = e.y * (h/(float)a.worldHeight);
		   
		   a.strokeWeight(2.0f);
		   a.stroke(255,255,255,255);
		   a.point(x+ rx, y+ ry);
		   
	   }
	   
	   //now draw all the user cursors
	   
	   for(int i =0; i <a.otherUsers.size();i++)
	   {
		   float rx,ry;
		   
		   UserDetails u = (UserDetails)a.otherUsers.get(i);
		   
		   rx = u.posx * (w/(float)a.worldWidth);
		   ry = u.posy * (h/(float)a.worldHeight);
		   
		   a.strokeWeight(2.0f);
		   a.stroke(u.userColor);
		   a.point(x+ rx, y+ ry);
		   
	   }
	   
   }
   

};

