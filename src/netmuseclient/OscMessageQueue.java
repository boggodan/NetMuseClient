package netmuseclient;
import java.awt.Color;
import java.util.ConcurrentModificationException;
import java.util.Vector;

import netmuseclient.Wavefront.OwnedEntity;
import processing.core.PApplet;


/**
 * this queue is designed to gather OSC messages, and then process them
 * in the main thread, as opposed to directly doing them in the OSC thread. This is
 * to avoid concurrent access errors. The downside is that the synchronization of added objects becomes framerate
 * dependant, but this shouldn't really matter if timetags are used.
 * @author bogdan
 *
 */
public class OscMessageQueue {

    /**
     * inherit this message class to make your own message type.
     * Just add your message to the list and it will be automatically processed.
     * @author bogdan
     *
     */
    public class Message {
        String messageType;
        Message() {

        }

        void process(NetMuseClient a) {

        }
    }

    class AddRemoteEntity extends Message {
        AddRemoteEntity(String type, String identifier, String user, int nx, int ny) {
            this.type = type;
            this.identifier = identifier;
            this.user = user;
            this.nx = nx;
            this.ny = ny;
            messageType = "addRemoteEntity";
        }

        @
        Override
        void process(NetMuseClient a) {
            a.addEntityRemote(user, type, identifier, nx, ny);
        }

        String type, identifier, user;
        int nx, ny;
    };

    class UpdateExistingEntity extends Message {
        UpdateExistingEntity(String type, String identifier, String user, int nx, int ny) {
            this.type = type;
            this.identifier = identifier;
            this.user = user;
            this.nx = nx;
            this.ny = ny;
            messageType = "updateExistingEntity";
        }

        @
        Override
        void process(NetMuseClient a) {
            a.addEntityRemote(user, type, identifier, nx, ny);
        }

        String type, identifier, user;
        int nx, ny;
    };

    //another user removed an entity
    class RemoveRemoteEntity extends Message {
        RemoveRemoteEntity(String identifier) {
            this.identifier = identifier;
            messageType = "removeRemoteEntity";
        }

        @
        Override
        void process(NetMuseClient a) {
            //look up the entity and delete it
            for (int i = 0; i < a.audioEntities.size(); i++) {
                AudioEntity e = (AudioEntity) a.audioEntities.get(i);
                if (e.identifier.equals(identifier)) {
                    //destroy the context menu if it was open this side
                    if (e == a.currentContextEntity) {
                        a.currentContextEntity.hideContextControls();


                        a.currentContextEntity = null;
                        a.IS_CONTEXT_VISIBLE = false;
                    }

                    a.audioEntities.remove(i);
                    a.console.println("DEBUG: Remote entity removal went better than expected.");
                    break;
                }
            }
        }

        String identifier;

    };

    class MovedAudioEntity extends Message {
        MovedAudioEntity(String identifier, String user, int nx, int ny) {

            this.identifier = identifier;
            this.user = user;
            this.nx = nx;
            this.ny = ny;
            messageType = "movedAudioEntity";
        }

        @
        Override
        void process(NetMuseClient a) {
            for (int i = 0; i < a.audioEntities.size(); i++) {
                AudioEntity e = (AudioEntity) a.audioEntities.get(i);
                //console.println("Tried to move identifier: " + identifier + " real id: " + a );
                if (e.getEntityID().equals(identifier)) {
                    e.x = nx;
                    e.y = ny;
                }
            }
        }

        String identifier, user;
        int nx, ny;
    };

    class ParamChange extends Message {
        ParamChange(String identifier, String user, String paramName, float value, long timeTag) {
            this.timeTag = timeTag;
            this.identifier = identifier;
            this.user = user;
            this.paramName = paramName;
            this.value = value;
            messageType = "paramChange";
        }

        @
        Override
        void process(NetMuseClient a) {
            for (int i = 0; i < a.audioEntities.size(); i++) {
                AudioEntity e = (AudioEntity) a.audioEntities.get(i);
                if (e.identifier.equals(identifier))
                    e.remoteParamChange(paramName, value, (long) timeTag);
            }
        }

        String identifier, user, paramName;
        long timeTag = 0;
        float value;
    };

    class GroupParamChange extends Message {
        GroupParamChange(String identifier, String group, float value) {

            this.identifier = identifier;
            this.group = group;
            this.value = value;
            messageType = "groupParamChange";
        }

        @
        Override
        void process(NetMuseClient a) {
            for (int i = 0; i < a.audioEntities.size(); i++) {

                AudioEntity ae = (AudioEntity) a.audioEntities.get(i);
                if (ae.identifier.equals(identifier)) {
                    ae.remoteGroupHandler(group, value);
                }

            }
        }

        String identifier;
        String group;
        float value;
    };

    //not used in this version
    class WavefrontTimeSync extends Message {
        WavefrontTimeSync(String identifier, String time) {
            this.identifier = identifier;
            this.time = time;
            messageType = "wavefrontTimeSync";
        }

        @
        Override
        void process(NetMuseClient a) {
            for (int i = 0; i < a.audioEntities.size(); i++) {
                AudioEntity e = (AudioEntity) a.audioEntities.get(i);
                if (e.identifier.equals(identifier)) {

                }

            }
        }

        String identifier, time;
    };

    class UpdateFinishedMessage extends Message {
        String serverName;
        Vector serverGreeting;
        UpdateFinishedMessage(String serverName, Vector serverGreeting) {
            this.serverName = serverName;
            this.serverGreeting = serverGreeting;

            messageType = "updateFinished";
        }

        @
        Override
        void process(NetMuseClient a) {

            a.sessionUpdating = false;
            a.doneUpdating();

            //print the server name
            a.console.println("Server name: " + serverName, a.color(255, 20, 0, 255));

            //print the server greeting
            for (int i = 0; i < serverGreeting.size(); i++) {
                a.console.println((String) serverGreeting.get(i));
            }

        }


    };

    class UserJoinOK extends Message {
        int clr;
        String username;
        UserJoinOK(int clr, String username) {
            this.clr = clr;
            this.username = username;

            messageType = "userJoinOK";
        }

        @
        Override
        void process(NetMuseClient a) {

            try {
                a.startSession();
                a.console.println("DEBUG: started session...");
                a.sessionUpdating = true;



                a.authenticationGroup.hide();
                a.registerGroup.hide();
                a.userDetails = new UserDetails(username, 0, 0, clr, a);

            } catch (Exception ex) {
                a.println("Exception in userjoin process function.");
                ex.printStackTrace();
            }

        }


    };

    //IMPLEMENT THIS NEXT

    class SnapToWavefront extends Message {
        int numEntities, entIndex;
        SnapToWavefront(String entityID, String snapeeID, int numEntities, int entIndex) {
            this.entIndex = entIndex;
            this.numEntities = numEntities;
            this.entityID = entityID;
            this.snapeeID = snapeeID;
            messageType = "snapWavefrontMessage";
        }

        @
        Override
        void process(NetMuseClient a) {
            try {
                a.println("Processing: snap " + entIndex + " of " + numEntities);
                //references to snapped, snapee entities
                AudioEntity snapped = null;
                Wavefront snapee = null;

                for (int i = 0; i < a.audioEntities.size(); i++) {
                    AudioEntity e = (AudioEntity) a.audioEntities.get(i);
                    //snapped entity
                    if (e.identifier.equals(entityID)) {
                        //this doesn't get allocated
                        snapped = (AudioEntity) a.audioEntities.get(i);

                    }

                    //snapee entity
                    if (e.identifier.equals(snapeeID)) {

                        snapee = (Wavefront) a.audioEntities.get(i);

                    }
                }
                //a.println("Got past finding the entities.");
                //snapped seems to be null. Why?
                /*CRASHES BECAUSE MESSAGE ARRIVES BEFORE QUEUE IS PROCESSED
                 * ADD MESSAGE TYPE TO QUEUE CLASS AND PROCESS IN DRAW FUNCTION
                 */

                a.println("Snapped info: " + snapped);
                a.println("Snapee info: " + snapee);
                //snap dem entiteez
                snapee.handleSnapEntity(snapped, snapped.x, snapped.y);
                a.println("Got past snapping.");
            } catch (Exception ex) {
                a.println("TRAIN WRECK!");
                a.println("Train wreck!");
                a.println("WARNING: An entity failed to snap. The server has been informed.");
                ex.printStackTrace();
            }
        }
        String entityID, snapeeID;
    }

    class UnsnapFromWavefront extends Message {
        UnsnapFromWavefront(String entityID, String snapeeID) {
            this.entityID = entityID;
            this.snapeeID = snapeeID;
            messageType = "unsnapWavefrontMessage";
        }

        @
        Override
        void process(NetMuseClient a) {
            a.println("processing unsnap message.");
            a.println("****************888888888*");
            //references to snapped, snapee entities
            AudioEntity snapped = null;
            Wavefront snapee = null;

            for (int i = 0; i < a.audioEntities.size(); i++) {
                AudioEntity e = (AudioEntity) a.audioEntities.get(i);
                //snapped entity
                if (e.identifier.equals(entityID)) {
                    //this doesn't get allocated
                    snapped = (AudioEntity) a.audioEntities.get(i);

                }

                //snapee entity
                if (e.identifier.equals(snapeeID)) {
                    snapee = (Wavefront) a.audioEntities.get(i);

                }
            }
            a.println("Got past finding the entities.");

            //unsnap
            for (int i = 0; i < snapee.ownedEntities.size(); i++) {
                OwnedEntity oe = (OwnedEntity) snapee.ownedEntities.get(i);
                if (oe.e.identifier.equals(entityID)) {
                    snapee.ownedEntities.remove(i);
                }
            }

            snapped.snapped = false;
            snapped.snapee = null;
            //a.console.println("Unsnapped " + entityID + " from " + snapeeID); 

        }
        String entityID, snapeeID;
    }

    Vector queue;

    NetMuseClient a;

    OscMessageQueue(NetMuseClient a) {
        this.a = a;
        queue = new Vector();
    }

    /**
     * add a message to the queue
     * @param mess
     */
    public void add(Message mess) {
        queue.add(mess);
    }

    /**
     * process the queu
     */
    void process() {
        for (int i = 0; i < queue.size(); i++) {
            Message m = (Message) queue.get(i);
            a.println("Processing message: " + m.messageType);
            m.process(a);
        }
    }


}