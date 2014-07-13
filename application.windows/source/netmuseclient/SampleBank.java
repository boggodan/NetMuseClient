package netmuseclient;
import net.beadsproject.beads.*;
import net.beadsproject.beads.data.Sample;
import java.util.*;
/* a vector of audio samples, storing paths, memory allocated samples and sample names.
 * The bank stores samples in no particular order, and names them, so that entities can choose
 * what samples they can use. Rather than loading samples into memory inside each entity (which would take
 * up too much RAM), they will be loaded (and unloaded) into memory by this sample bank.
 */

/**
 * a vector of audio samples, storing paths, memory allocated samples and sample names.
 * The bank stores samples in no particular order, and names them, so that entities can choose
 * what samples they can use. Rather than loading samples into memory inside each entity (which would take
 * up too much RAM), they will be loaded (and unloaded) into memory by this sample bank.
 * @author bogdan
 *
 */
public class SampleBank 
{
	Vector samplesVect;
	NetMuseClient a;
     SampleBank(NetMuseClient a)
     {
    	 this.a = a;
    	 samplesVect = new Vector();
     }
     
     /**
      * add a sample to the sample bank, based on its path, and set its name in the list
      * @param path
      * @param name
      */
    public void addSample(String path, String name)
     {
    	 Sample s = new Sample(path, name, a);
    	 samplesVect.add(s);
     }
     
    /**
     * get a sample by name
     * @param name
     * @return
     */
     SampleBank.Sample getByName(String name)
     {
    	 for(int i =0; i <samplesVect.size();i++)
    	 {
    		 SampleBank.Sample s = (SampleBank.Sample)samplesVect.get(i);
    		 if(s.name.equals(name))
    		 {
    			 return s;
    		 }
    	 }
    	 return null;
     }
     
     /**
      * stores information about an audio sample
      * @author bogdan
      *
      */
     public class Sample
     {
    	 String path; 
    	 String name;
    	 net.beadsproject.beads.data.Sample sample; 
    	 Sample(String path, String name, NetMuseClient a)
    	 {
    		 this.path = path;
    		 this.name = name;
    		 try{
    		 sample = new net.beadsproject.beads.data.Sample(a.sketchPath("") + "data/" + path);
    		 }
    		 catch(Exception ex)
    		 {
    			 ex.printStackTrace();
    		 }
    	 }
     };
};
