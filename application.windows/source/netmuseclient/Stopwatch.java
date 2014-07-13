package netmuseclient;
import processing.core.PApplet;

/**
 * call getTime() to get the time elapsed since the stopwatch was started
 * @author bogdan
 *
 */
public class Stopwatch{
	 
	  int savedTime; // When Timer started
	  int totalTime; // How long Timer should last
	  PApplet ap;
	  Boolean state;
	  Stopwatch( PApplet ap) {
	    this.ap = ap;
	    state = false;
	  }
	  
	  // Starting the timer
	  void start() {
	    // When the timer starts it stores the current time in milliseconds.
	    savedTime = ap.millis(); 
	    state = true;
	  }
	  
	 
	  // The function isFinished() returns true if 5,000 ms have passed. 
	  // The work of the timer is farmed out to this method.
	  int getTime() { 
	    // Check how much time has passed
	    int passedTime = ap.millis()- savedTime;
	    return passedTime;
	  }
	}
