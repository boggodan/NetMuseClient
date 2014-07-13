package netmuseclient;

/**
 * Asynchronous timer, which can run in a separate thread.
 * @author bogdan
 *
 */
class SyncTimer extends Thread {
    int currentMillis;
    int prevMillis;
    int startMillis;
    int maxTime;
    NetMuseClient a;

    /**
     *
     * @param a
     * @param time
     */
    SyncTimer(NetMuseClient a, int time) {
        this.a = a;
        maxTime = time;
        startMillis = a.millis();
        prevMillis = 0;
        currentMillis = 0;

    }

    /**
     * Starts the timer.
     */
    @
    Override
    public void start() {
        super.start();
    }

    @
    Override
    public void run() {
        while (true) {

            currentMillis = a.millis() - startMillis;

            if (currentMillis > maxTime) {
                startMillis = a.millis();
                currentMillis = 0;
                //throw the event
                a.console.println("DEBUG: Async Timer Tick!");

            }
        }
    }

};