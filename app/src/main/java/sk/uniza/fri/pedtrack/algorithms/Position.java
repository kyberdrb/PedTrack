package sk.uniza.fri.pedtrack.algorithms;

public class Position {

    private double aX;
    private double aY;
    private double aZ;
    private int aTrackerID;
    private long aTimeOfMeasurement;

    public Position(double paX, double paY, double paZ, int paTrackerID, long paTimeOfMeasurement) {
        aX = paX;
        aY = paY;
        aZ = paZ;
        aTrackerID = paTrackerID;
        aTimeOfMeasurement = paTimeOfMeasurement;
    }

    public double getX() {
        return aX;
    }

    public double getY() {
        return aY;
    }

    public double getZ() {
        return aZ;
    }

    public int getTrackerID() {
        return aTrackerID;
    }

    public long getTimeOfMeasurement() {
        return aTimeOfMeasurement;
    }
}
