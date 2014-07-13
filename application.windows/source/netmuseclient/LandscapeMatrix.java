package netmuseclient;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

/**
 * stores the background graphics
 * @author bogdan
 *
 */
public class LandscapeMatrix
{
	//2D matrix of landscape elements, stored as a 1d array
   Element[] elements;
   float elementSize = 60;
   int numElements;
   //the world is square, so this is the number of elements per row or column
   int width;
   NetMuseClient a;
   LandscapeMatrix(NetMuseClient a)
   {
	   this.a = a;
	   numElements = (a.worldWidth/(int)elementSize)*a.worldHeight;
	   
	   width = a.worldWidth/(int)elementSize;
	   a.println("num elements: " + numElements);
	   elements = new Element[numElements];
	   for(int i =0; i <elements.length; i++)
	   {
		   elements[i] = new Element();
	   }
	   
   }
   
   /**
    * gets an element at element position x,y
    * @param x
    * @param y
    * @return
    */
   public Element getElementXY(int x, int y)
   {
	   return elements[y*width +x];
	
   }
   
   /**
    * modifies the color of an element at position x,y
    * @param x
    * @param y
    * @param r
    * @param g
    * @param b
    */
   public void modifyElement(int x, int y, float r, float g, float b)
   {
	   getElementXY(x, y).r+=r;
	   getElementXY(x, y).g+=g;
	   getElementXY(x, y).b+=b;
   }
   
   /**
    * returns the element coordinates from world coordinates
    * @param x
    * @param y
    * @return
    */
   public PVector getSquareCoordsFromWorldCoords(int x, int y)
   {
	   PVector coords = new PVector(x/(int)elementSize,y/(int)elementSize);
	   
	   return coords;
   }
   
   /**
    * draws the background graphics
    */
   public void drawLandscape()
   {
	  int index=0;
	   for(int i =0; i <width; i++)
	   {
		   for(int j =0; j<width;j++)
		   {
		 int x,y;
		 x = j *(int)elementSize;
		 y = i*(int)elementSize;
		
		   elements[index].r*=0.98f;
		    elements[index].g*=0.98f;
		    elements[index].b*=0.98f;
		 
		 if(x+(int)elementSize > a.cameraX && x < a.cameraX + a. screenW && y+(int)elementSize > a.cameraY && y < a.cameraY + a. screenH)
		 {
			 a.noStroke();
			a.fill(255-elements[index].r,255-elements[index].g,255-elements[index].b);
			a.rect(x-a.cameraX,y-a.cameraY, (int)elementSize,(int)elementSize);
		
			
		 }
		 index++;
		   }
	   }
   }
   
   //landscape element 
   
   public class Element
   {
	   //these are arbitrary values that can be used in the graphics rendering in various ways
	   float r,g,b;
	   
	
	  Element()
	  {
	   
	    
		//alpha = a.random(50);
		//beta = a.random(50);
		//gamma = a.random(50);
		  r=g=b=0;
	  }
	  
	  
   };
}
