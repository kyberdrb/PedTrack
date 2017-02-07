package sk.uniza.fri.pedtrack.algorithms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.rest.data.request.position_entry.WifiEntry;

import java.util.Date;
import java.util.HashSet;

import sk.uniza.fri.pedtrack.Constants;
import sk.uniza.fri.pedtrack.IPosition;

public class WifiTracker extends Tracker {

    private final String CLASS_NAME = "WifiTracker";
    private WifiManager aWifiManager;
    private Handler aHandler = new Handler();
    private Runnable aRunnable;
    private int aDelay; // v milisekundach; meni sa podla nastaveni zavolanim metody onServiceConnected v triede MainService pri vytvarani novej instancie tohto trackera zavolanim metody "enable" alebo callbackom, aby sa interval zmenil za behu pri uz vytvorenom trackeri


    public WifiTracker(IPosition paIPosition){
        super(paIPosition);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("changeWifiRefreshInterval");
            if (message != "" && message != null) {
                aDelay = Integer.parseInt(message) * 1000;
                Log.i(CLASS_NAME, "Refresh interval zmeneny na " + message + " sekund");
            }

        }
    };

    /**
     * metoda zapne wifi, ked bola predtym vypnuta, ale ak bola wifi zapnuta, tak ju necha tak
     */
    @Override
    public void enable(Context paContext, int paDelay) {
        aDelay = paDelay * 1000;

        //zapni wifi
        aWifiManager = (WifiManager) paContext.getSystemService(paContext.WIFI_SERVICE);
        if (!aWifiManager.isWifiEnabled()) {
            //TODO - odkomentovat tento riadok, aby sa wifi naozaj zapla
            aWifiManager.setWifiEnabled(true);
        }

        // zaregistruj broadcast listener
        LocalBroadcastManager.getInstance(paContext).registerReceiver(mMessageReceiver,
                new IntentFilter("wifitracker_broadcast"));

        //nastartuj vlakno
        getWifiLocation();
        Log.i(CLASS_NAME, "WifiTracker spusteny");
    }

    private void getWifiLocation() {
        aRunnable = new Runnable(){
            public void run(){
                if (isTrackingAllowed()) {
                    //vypocitaj polohu a uloz ju do atributu v predkovi, aby sme potom mohli vytvorit entitu
                    aDate = Constants.updateDate();
                    aPosition = new Position(2, 4, 6, ID_WIFI, aDate);

                    //TODO - odkomentovat tento riadok na hlasenie polohy a ulozit suradnice do atributu
                    updatePosition(2, 4, 6, Tracker.ID_WIFI, aDate);
                    Log.i(CLASS_NAME, "purr the hurr durr wifi");
                    aHandler.postDelayed(this, aDelay);
                } else {
                    /*vlakno sa nechcelo ukoncit cez removeCallbacks pri zatvoreni a znovuotvoreni
                    aplikacie; musel som to urobit cez cez isTrackingAllowed, inak by sa vlakno
                    nikdy nezastavilo; zrejme to bude suvisiet s kontextom, lebo v mainservice to
                    tak funguje*/
                    Log.i(CLASS_NAME, "WifiTracker zastaveny");
                    return;
                }
            }
        };

        aHandler.postDelayed(aRunnable, aDelay);
    }

    public HashSet<WifiEntry> makeWifiEntry() {
        WifiEntry wifiEntry = new WifiEntry();
        HashSet<WifiEntry> hsWifi = new HashSet<>();
        if (isTrackingAllowed()) {
            //povyplnat znamymi wifi ap
            wifiEntry.setSignalStrength(0.0);
            wifiEntry.setKnownDeviceId("kd0");
            hsWifi.add(wifiEntry);
        } else {
            if (aPosition != null) {
                // ohlas poslednu znamu polohu
                wifiEntry.setSignalStrength(0.0);
                wifiEntry.setKnownDeviceId("kd0");
                hsWifi.add(wifiEntry);
            } else {
                // inak ohlas prazdnu polohu
                wifiEntry.setSignalStrength(0.0);
                wifiEntry.setKnownDeviceId("kd0");
                hsWifi.add(wifiEntry);
            }
        }
        return hsWifi;
    }

    public void unregisterBroadcastListener(Context paContext) {
        LocalBroadcastManager.getInstance(paContext).unregisterReceiver(mMessageReceiver);
        Log.i(CLASS_NAME, "LocalBroadcastManager odregistrovany");
    }
}
