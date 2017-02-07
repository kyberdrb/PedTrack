package sk.uniza.fri.pedtrack.algorithms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.rest.data.request.position_entry.BeaconEntry;

import java.util.Date;
import java.util.HashSet;

import sk.uniza.fri.pedtrack.Constants;
import sk.uniza.fri.pedtrack.IPosition;

/**
 * Zistovanie polohy pomocou Bluetooth senzorov (Bluetooth Beacons)
 */
public class BluetoothTracker extends Tracker {

    private final String CLASS_NAME = "BluetoothTracker";
    private BluetoothManager aBTManager;
    private Handler aHandler = new Handler();
    private Runnable aRunnable;
    private int aDelay = 1000; // milliseconds - je to sice dane napevno, ale meni sa to podla nastaveni v metode onServiceConnected v triede MainService

    public BluetoothTracker(IPosition paIPosition) {
        super(paIPosition);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("changeBtRefreshInterval");
            if (message != "" && message != null) {
                aDelay = Integer.parseInt(message) * 1000;
                Log.i(CLASS_NAME, "Refresh interval zmeneny na " + message + " sekund");
            }

        }
    };

    @Override
    public void enable(Context paContext, int paDelay) {
        aDelay = paDelay * 1000;

        //zapni bluetooth
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            btAdapter.enable();
        }

        // zaregistruj broadcast listener
        LocalBroadcastManager.getInstance(paContext).registerReceiver(mMessageReceiver,
                new IntentFilter("bluetoothtracker_broadcast"));

        //nastartuj handler
        getBTLocation();
        Log.i(CLASS_NAME, "BluetoothTracker spusteny");
    }

    private void getBTLocation() {
        aRunnable = new Runnable(){
            public void run(){
                if (isTrackingAllowed()) {
                    //vypocitaj polohu a uloz ju do atributu v predkovi, aby sme potom mohli vytvorit entitu
                    aDate = Constants.updateDate();
                    aPosition = new Position(1, 3, 5, ID_BLUETOOTH, aDate);

                    updatePosition(1, 3, 5, Tracker.ID_BLUETOOTH, aDate);
                    Log.i(CLASS_NAME, "purr the hurr durr bluetooth");
                    aHandler.postDelayed(this, aDelay);
                } else {
                    //pozri metodu getWifiLocation, preco to vlakno zastavujem takto a nie cez removeCallbacks
                    Log.i(CLASS_NAME, "BluetoothTracker zastaveny");
                    return;
                }
            }
        };

        aHandler.postDelayed(aRunnable, aDelay);
    }

    public HashSet<BeaconEntry> makeBTEntry() {
        BeaconEntry beaconEntry = new BeaconEntry();
        HashSet<BeaconEntry> hsBeacon = new HashSet<>();
        if (isTrackingAllowed()) {
            //povyplnat znamymi bt beacnami
            beaconEntry.setSignalStrength(0.0);
            beaconEntry.setKnownDeviceId("kd1");
            hsBeacon.add(beaconEntry);
        } else {
            if (aPosition != null) {
                // ohlas poslednu znamu polohu
                beaconEntry.setSignalStrength(0.0);
                beaconEntry.setKnownDeviceId("kd1");
                hsBeacon.add(beaconEntry);
            } else {
                // inak ohlas prazdnu polohu
                beaconEntry.setSignalStrength(0.0);
                beaconEntry.setKnownDeviceId("kd1");
                hsBeacon.add(beaconEntry);
            }
        }
        return hsBeacon;
    }

    public void unregisterBroadcastListener(Context paContext) {
        LocalBroadcastManager.getInstance(paContext).unregisterReceiver(mMessageReceiver);
        Log.i(CLASS_NAME, "LocalBroadcastManager odregistrovany");
    }
}
