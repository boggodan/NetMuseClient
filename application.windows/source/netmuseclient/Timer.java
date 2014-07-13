package netmuseclient;
import processing.core.PApplet;
public class Timer{
	 
	  int savedTime; // When Timer started
	  int totalTime; // How long Timer should last
	  PApplet ap;
	  Timer(int tempTotalTime, PApplet ap) {
	    this.ap = ap;
		  totalTime = tempTotalTime;
	  }
	  
	  // Starting the timer
	  void start() {
	    // When the timer starts it stores the current time in milliseconds.
	    savedTime = ap.millis(); 
	  }
	  
	  // The function isFinished() returns true if 5,000 ms have passed. 
	  // The work of the timer is farmed out to this method.
	  boolean isFinished() { 
	    // Check how much time has passed
	    int passedTime = ap.millis()- savedTime;
	    if (passedTime > totalTime) {
	      return true;
	    } else {
	      return false;
	    }
	  }
	}
