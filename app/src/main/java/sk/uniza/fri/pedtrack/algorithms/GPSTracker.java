package sk.uniza.fri.pedtrack.algorithms;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.rest.data.request.position_entry.GpsEntry;

import java.util.Date;
import java.util.HashSet;

import sk.uniza.fri.pedtrack.Constants;
import sk.uniza.fri.pedtrack.IPosition;

/**
 * GPSTracker zistuje GPS suradnice
 */
public class GPSTracker extends Tracker implements LocationListener, GpsStatus.Listener {

    private final String CLASS_NAME = "GPSTracker";
    private boolean aIsLocationEnabled = false;
    protected LocationManager aLocationManager;
    private Location aLocation;
    private static final int MIN_DISTANCE_CHANGE_FOR_UPDATE = 1;
    private static final int MIN_TIME_BW_UPDATES = 3000;
    private boolean isGPSFix = false;
    private long mLastLocationMillis;
    private Context aContextFromMainService;

    public GPSTracker(IPosition paIPosition) {
        super(paIPosition);
    }

    public boolean isGPSFix() {
        return isGPSFix;
    }

    /**
     *
     * @param paContext
     * @param paMinTimeForUpdate pre buducnost, keby sme nahodou chceli menit rychlost updatov
     */
    @Override
    public void enable(Context paContext, int paMinTimeForUpdate) {
        try {
            aContextFromMainService = paContext;
            aLocationManager = (LocationManager) paContext.getSystemService(paContext.LOCATION_SERVICE);
            //PermissionCheck: opravnenia su osetrene v triede MainActivity
            aLocationManager.addGpsStatusListener(this);    //zaregistruj GPSListener, aby sme mohli sledovať stav GPS
            aIsLocationEnabled = aLocationManager.isProviderEnabled(aLocationManager.GPS_PROVIDER);

            if (aIsLocationEnabled) {
                if (aLocation == null) {
                    //PermissionCheck: opravnenia su osetrene v triede MainActivity
                    aLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATE, this);

                    if (aLocationManager != null) {
                        //PermissionCheck: opravnenia su osetrene v triede MainActivity
                        aLocation = aLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        if (aLocation != null) {
                            Log.i(CLASS_NAME, "GPS tracker spusteny");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void enable(Context paContext) {
//        try {
//            aLocationManager = (LocationManager) paContext.getSystemService(paContext.LOCATION_SERVICE);
//            aIsLocationEnabled = aLocationManager.isProviderEnabled(aLocationManager.NETWORK_PROVIDER);
//
//            if (aIsLocationEnabled) {
//                if (aLocation == null) {
//                    aLocationManager.requestLocationUpdates(
//                            LocationManager.NETWORK_PROVIDER,
//                            MIN_TIME_BW_UPDATES,
//                            MIN_DISTANCE_CHANGE_FOR_UPDATE, this);
//
//                    if (aLocationManager != null) {
//                        aLocation = aLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//
//                        if (aLocation != null) {
//                            Log.i("GPSTracker", "GPS tracker spusteny");
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void disable() {
        if (aLocationManager != null) {
            //PermissionCheck: opravnenia su osetrene v triede MainActivity
            aLocationManager.removeUpdates(this);
            aLocationManager = null;
            aLocation = null;
            Log.i(CLASS_NAME, "GPS tracker zastaveny");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("GPSTracker", "onLocationChanged()");
        if (location != null) {
            //uloz polohu do predka, aby sme potom mohli vytvorit entitu
            aDate = Constants.updateDate();
            aPosition = new Position(location.getLatitude(),
                    location.getLongitude(),
                    location.getAltitude(),
                    super.ID_GPS,
                    aDate);

            super.updatePosition(location.getLatitude(),
                    location.getLongitude(),
                    location.getAltitude(),
                    super.ID_GPS,
                    aDate);

            Log.i(CLASS_NAME, "GPS suradnice sa zmenili: " +
                    "x=" + location.getLatitude() + " y=" + location.getLongitude() + " z=" + location.getAltitude());
            mLastLocationMillis = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:  //sme zamerani pocas sledovania
                //if (aLocation != null)
                    if((SystemClock.elapsedRealtime() - mLastLocationMillis) < MIN_TIME_BW_UPDATES)
                    {
                        isGPSFix = true;
                        Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                "mainservice_broadcast", "gps_fixed", "y");
                        Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                "trackingfragment_broadcast", "gps_fixed", "y");
                    } else {
                        isGPSFix = false;
                        Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                "mainservice_broadcast", "gps_fixed", "n");
                        Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                "trackingfragment_broadcast", "gps_fixed", "n");
                    }

                if (isGPSFix) {     //sme zamerany, ale nepotrebujeme robit nic, lebo vsetko robi metoda onLocationChanged

                } else {            //stratili sme GPS signal
                    isGPSFix = false;
                }
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:     //sme zamerani po prvy krat
                isGPSFix = true;

                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "gps_fixed", "y");
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "gps_fixed", "y");

                break;
        }
    }

    public HashSet<GpsEntry> makeGPSEntry() {
        HashSet<GpsEntry> hsGPS = new HashSet<>();
        if (isTrackingAllowed()) {
            hsGPS.add(fillGPSEntry(aPosition.getX(), aPosition.getY(), aPosition.getZ()));
        } else {
            if (aPosition != null) {
                // ohlas poslednu znamu polohu
                hsGPS.add(fillGPSEntry(aPosition.getX(), aPosition.getY(), aPosition.getZ()));
            } else {
                // inak ohlas prazdnu polohu
                hsGPS.add(fillGPSEntry(0.0, 0.0, 0.0));
            }
        }
        return hsGPS;
    }

    private GpsEntry fillGPSEntry(double paX, double paY, double paZ) {
        GpsEntry gpsEntry = new GpsEntry();
        gpsEntry.setLatitude(paX);
        gpsEntry.setLongitude(paY);
        gpsEntry.setAltitude(paZ);
        return gpsEntry;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

