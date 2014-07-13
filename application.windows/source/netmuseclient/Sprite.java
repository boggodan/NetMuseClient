package netmuseclient;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;


/**
 * draws an animated sprite 
 * @author bogdan
 *
 */
public class Sprite 
{
	NetMuseClient a;
	ArrayList spriteSheet;
	int w,h;
	int numSprites;
	int currentFrame =0; 
	
	 /**
	  * loads a sprite sheet from a path
	  * @param spriteSheetPath
	  * @param a
	  * @param numSprites
	  */
     public Sprite(String spriteSheetPath, NetMuseClient a, int numSprites)
     {
    	 spriteSheet = new ArrayList();
    	 this.a = a;

    	 //sheets are vertical;
    	 this.numSprites = numSprites;
    	 
    	 //fill with images
		  PImage img = a.loadImage("spriteSheetPath.png");
		  spriteSheet.add(img);
		  for(int i = 1; i<100; i++)
		  {
			 PImage img1 = a.loadImage("spriteSheetPath"+i +".png");
			
			 
			 spriteSheet.add(img1);
		  }
    	 
    	 //dimensions of one sprite
    	 w=img.width;
    	 h=img.height/numSprites;
    	 
    	 
     }
     
     /**
      * loads a sprite from a zip archive using Yonas Sandbæk's UnZipIt class
      * @param archiveName
      * @param spriteSheetPath
      * @param a
      * @param numSprites
      */
     
     public Sprite(String archiveName, String spriteSheetPath, NetMuseClient a, int numSprites)
     {
    	 spriteSheet = new ArrayList();
    	 this.a = a;
    	 UnZipIt assetDecompressor = new UnZipIt(a.sketchPath("") + "data/" + archiveName,a);
    	

    	
    	

    	 //sheets are vertical;
    	 this.numSprites = numSprites;
    	 
    	 //fill with images
		  PImage img = assetDecompressor.loadImage(spriteSheetPath + ".png");
		  spriteSheet.add(img);
		  for(int i = 1; i<numSprites; i++)
		  {
			 PImage img1 = assetDecompressor.loadImage(spriteSheetPath + i +".png");
			
			 
			 spriteSheet.add(img1);
		  }
    	 
    	 //dimensions of one sprite
    	 w=img.width;
    	 h=img.height/numSprites;
    	 restart();
    	 
     }
     
     /**
      * draws the sprite at some position
      * @param x
      * @param y
      */
     public void drawSprite(int x, int y)
     {
    	// a.println(currentFrame);
    	 
    	 PImage img = (PImage)spriteSheet.get(currentFrame);
    	 
    	 a.image(img, x, y);
    	 
    	 nextFrame();
    	 
    	 if(currentFrame>numSprites-1)
    	 restart();
     }
     
     void nextFrame()
     {
    	 currentFrame++;
     }
     
     void previousFrame()
     {
    	 currentFrame--;
     }
     
     void restart()
     {
    	 currentFrame = 0; 
     }
     
     
}
