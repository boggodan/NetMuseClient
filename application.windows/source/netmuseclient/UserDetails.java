package netmuseclient;

/**
 * stores user details
 * @author bogdan
 *
 */
public class UserDetails 
{
	int posx,posy;
	int userColor;
	
	float drawx,drawy;
	String username;
	MovLowpass xLP,yLP;
	
	//reference to parent applet
	NetMuseClient a;
	
	//timer that measures the time between a ping request and a ping response from the server
	Stopwatch pingTimer;
	
    public UserDetails(String username, int posx, int posy, int userColor, NetMuseClient a)
    {
    	this.a = a;
    	drawx = drawy = 0.0f;
    	xLP = new MovLowpass();
    	yLP = new MovLowpass();
    	
    	pingTimer = new Stopwatch(a);
    	
    	this.username = username;
    	this.posx = posx;
    	this.posy = posy;
    	this.userColor = userColor;
    }
    
    /**
     * starts a timer designed to measure ping
     */
    public void startPingMeasurement()
    {
    	pingTimer.start();
    }
    
    /**
     * get the time elapsed from the ping timer
     * @return
     */
    int getPingMeasurement()
    {
    	return pingTimer.getTime();
    }
    
}
