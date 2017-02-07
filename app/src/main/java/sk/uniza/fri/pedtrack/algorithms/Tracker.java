package sk.uniza.fri.pedtrack.algorithms;

import android.content.Context;

import java.util.Date;

import sk.uniza.fri.pedtrack.IPosition;

public abstract class Tracker {

    private boolean aTrackingAllowed = false;
    public static final int ID_GPS = 0;
    public static final int ID_WIFI = 1;
    public static final int ID_BLUETOOTH = 2;

    protected Position aPosition;
    protected final IPosition aIPosition;
    protected long aDate;

    public Tracker(IPosition paIPosition) {
        aIPosition = paIPosition;
    }

    public void enable(Context paContext, int paDelay){

    }

    /**
     * metoda nie je potrebna; sledovacie triedy resp ich Handlery sa zastavuju cez setTrackingAllowed
     */
    public void disable(){

    }

    public boolean isTrackingAllowed() {
        return aTrackingAllowed;
    }

    public void setTrackingAllowed(boolean paTrackingAllowed) {
        aTrackingAllowed = paTrackingAllowed;
    }

    protected void updatePosition(double paX, double paY, double paZ, int paTrackerID,
                                  long paTimeOfMeasurement) {
        aIPosition.newPositionNotify(new Position(paX, paY, paZ, paTrackerID, paTimeOfMeasurement));
    }
}
