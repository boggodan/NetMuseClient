package netmuseclient;
/**
 * 1 pole lowpass shared by zioguido@gmail.com on the MusicDSP forum.
 * @author zioguido@gmail.com
 *
 */
public class MovLowpass
	 {
	
		 MovLowpass() { a = 0.90f; b = 1.f - a; z = 0; };
	     
	     float Process(float in) { z = (in * b) + (z * a); return z; }
	
	     float a, b, z;
	 };