import seltar.unzipit.*;

UnZipIt z;
PImage img;
void setup()
{
  // find zip file
  String loadPath = selectInput();
  // load it
  z = new UnZipIt(loadPath, this);
  
  // read an image from the zip file
  img = z.loadImage("png/64x64/add.png");
}

void draw()
{
  background(255);
  // draw the image
  if(img != null){
    image(img, 0, 0);
  }
}
