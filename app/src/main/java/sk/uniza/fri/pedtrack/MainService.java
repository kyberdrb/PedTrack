package sk.uniza.fri.pedtrack;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.rest.data.footprint_entry.FootprintEntry;
import com.rest.data.footprint_entry.KdfpEntry;
import com.rest.data.request.MobileDeviceEntry;
import com.rest.data.request.MobileDeviceRegistration;
import com.rest.data.request.position_entry.BeaconEntry;
import com.rest.data.request.position_entry.GpsEntry;
import com.rest.data.request.position_entry.WifiEntry;

import java.util.HashMap;
import java.util.HashSet;

import sk.uniza.fri.pedtrack.algorithms.BluetoothTracker;
import sk.uniza.fri.pedtrack.algorithms.GPSTracker;
import sk.uniza.fri.pedtrack.algorithms.Position;
import sk.uniza.fri.pedtrack.algorithms.ServerBridge;
import sk.uniza.fri.pedtrack.algorithms.Tracker;
import sk.uniza.fri.pedtrack.algorithms.WifiTracker;

public class MainService extends Service implements IPosition {

    private final String CLASS_NAME = "MainService";
    public static final int NOTIFICATION_ID = 7746;

    private Tracker aTrackers[];
    private ServerBridge aServerBridge;
    private IBinder mBinder = new MyBinder();

    private String aName;
    private String aSurname;

    @Override
    public void onCreate() {
        aTrackers = new Tracker[3]; //pole 3 trackerov
        aServerBridge = new ServerBridge(getApplicationContext(), false);
        initTrackers();

        // zaregistruj lokalny broadcastreceiver
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(mMessageReceiver,
                new IntentFilter("mainservice_broadcast"));
    }

    /**
     * Our handler for received Intents. This will be called whenever an Intent
     * with an action named "custom-event-name" is broadcasted.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // SPRAVY S OBSAHOM

            String message = intent.getStringExtra("num_of_cached_files");
            if (message != "" && message != null) {
                Log.i(CLASS_NAME, "Este " + message + " suborov");
            }

            message = intent.getStringExtra("first_name");
            if (message != null) {
                aName = message;
                Log.i(CLASS_NAME, "Meno zmenene na " + aName);
            }

            message = intent.getStringExtra("surname");
            if (message != null) {
                aSurname = message;
                Log.i(CLASS_NAME, "Priezvisko zmenene na " + aSurname);
            }

            // SPRAVY S PRAZDNYM OBSAHOM

            // Get extra data included in the Intent
            message = intent.getStringExtra("offline");
            if (message != null) {      // SPRAVA "offline" JE PRAZDNA !!!
                // zastavit odosielanie suborov a ak nie su zapnute ziadne trackre, zastavit sluzbu
                if (noTrackersEnabled()) {
                    stopThisService();
                }
                setDefaultServerBridgeFlags();
            }

            message = intent.getStringExtra("all_files_sent");  // aj toto je sprava s prazdnym retazcom
            if (message != null) {
                // zastavit odosielanie suborov a ak nie su zapnute ziadne trackre, zastavit sluzbu
                if (noTrackersEnabled()) {
                    stopThisService();
                    return;
                }
                setDefaultServerBridgeFlags();
            }

            message = intent.getStringExtra("abort");
            if (message != null) {
                // zastavit odosielanie suborov a ak nie su zapnute ziadne trackre, zastavit sluzbu
                if (noTrackersEnabled()) {
                    stopThisService();
                }
                setDefaultServerBridgeFlags();
            }
        }
    };

    private void stopThisService() {
        stopForeground(true);
        stopSelf();
        Log.i(CLASS_NAME, "Servica zlikvidovana");
    }

    private boolean noTrackersEnabled() {
        return !(getGPSTracker().isTrackingAllowed() && getWifiTracker().isTrackingAllowed() &&
                getBluetoothTracker().isTrackingAllowed());
    }

    private void setDefaultServerBridgeFlags() {
        ServerBridge.stopSendingFiles = true;
        ServerBridge.allCachedFilesSent = false;
        ServerBridge.isSendingCacheFiles = false;
    }

    /**
     * inicializacia trackerov
     */
    public void initTrackers(){
        aTrackers[Tracker.ID_GPS] = new GPSTracker(this);
        aTrackers[Tracker.ID_WIFI] = new WifiTracker(this);
        aTrackers[Tracker.ID_BLUETOOTH] = new BluetoothTracker(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(CLASS_NAME, "onStartCommand called");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new android.support.v4.app.NotificationCompat.Builder(this)
                .setContentTitle("PedTrack")
                .setTicker("")
                .setContentText("PedTrack Live Tracker")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    public GPSTracker getGPSTracker() {
        return (GPSTracker) aTrackers[0];
    }
    public WifiTracker getWifiTracker() {
        return (WifiTracker) aTrackers[1];
    }
    public BluetoothTracker getBluetoothTracker() {
        return (BluetoothTracker) aTrackers[2];
    }

    public void setName(String paName) {
        aName = paName;
    }

    public void setSurname(String paSurname) {
        aSurname = paSurname;
    }

    public void getKnownDevices(){
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put(ServerBridge.URL, ServerBridge.URL_ROOT + ServerBridge.URL_GET_KNOWN_DEVICES);
        hm.put(ServerBridge.METHOD_TYPE, ServerBridge.GET);

        hm.put(ServerBridge.SENDER, "knownDevices");

        aServerBridge.getKnownDevices(hm);
    }

    public void getProjects(){
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put(ServerBridge.METHOD_TYPE, ServerBridge.GET);
        hm.put(ServerBridge.URL, ServerBridge.URL_ROOT+ ServerBridge.URL_GET_KNOWN_DEVICES_PROJECT_ID +"1");

        hm.put(ServerBridge.SENDER, "getProjects");

        aServerBridge.getProjects(hm);
    }

    /**
     * registracia mobilneho zariadenia na server podla jeho MAC adresy Wifi sietovky
     * @param paStartSendingCachedFiles indikator, ci tato metoda ma volat metodu sendCachedFile z triedy ServerBridge akonahle je zariadenie uspesne zaregistrovane
     */
    public void registerMobileDevice(boolean paStartSendingCachedFiles,
                                     String paFirstName, String paSurname){
        HashMap<String, String> hm = new HashMap<String, String>();

        MobileDeviceRegistration mdr = new MobileDeviceRegistration();
        mdr.setDeviceId(aServerBridge.getWifiMACAddress());
        mdr.setFirstName(paFirstName);
        mdr.setSurname(paSurname);

        Gson gson = new Gson();
        String jsonString = gson.toJson(mdr);

        hm.put(ServerBridge.REQUEST_BODY, jsonString);
        hm.put(ServerBridge.URL, ServerBridge.URL_ROOT+ ServerBridge.URL_POST_REGISTER_DEVICE);
        hm.put(ServerBridge.METHOD_TYPE, ServerBridge.POST);
        hm.put(ServerBridge.SENDER, "registerDevice");
        String request = paStartSendingCachedFiles ? "1" : "0";
        hm.put(ServerBridge.CONTINUE_SENDING_FILES, request);

        aServerBridge.setRegistering(true); //prave odosiela data
        aServerBridge.registerMobileDevice(hm);
    }

    public void pushRecord(Position paPosition) {

        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put(ServerBridge.REQUEST_BODY,
                makePushRecordJson(
                        getGPSTracker().makeGPSEntry(),
                        getWifiTracker().makeWifiEntry(),
                        getBluetoothTracker().makeBTEntry(),
                        paPosition));
        hm.put(ServerBridge.URL, ServerBridge.URL_ROOT+ ServerBridge.URL_POST_PUSH_RECORD);
        hm.put(ServerBridge.METHOD_TYPE, ServerBridge.POST);
        hm.put(ServerBridge.SENDER, "pushRecord");
        aServerBridge.pushRecord(hm);
    }

    private String makePushRecordJson(
            HashSet<GpsEntry> paGpsEntry,
            HashSet<WifiEntry> paWifiEntry,
            HashSet<BeaconEntry> paBeaconEntry,
            Position paPosition)
    {

        MobileDeviceEntry mde = new MobileDeviceEntry();
        mde.setPosX(paPosition.getX());
        mde.setPosY(paPosition.getY());
        mde.setElevation(paPosition.getZ());
        mde.setTimeOfMeasurement(paPosition.getTimeOfMeasurement());

        mde.setGpsDatas(paGpsEntry);
        mde.setWifiDatas(paWifiEntry);
        mde.setBeaconDatas(paBeaconEntry);

        mde.setMobileDeviceId(aServerBridge.getWifiMACAddress());
        mde.setProjectId(1);

        Gson gson = new Gson();
        String gsonString = gson.toJson(mde);
        return gsonString;
    }

    /**
     * Testovacia metoda na posielanie informacii o sile signalu medzi mobilnym zariadenim a
     * jednotlivymi pristupovymi bodmi a bluetooth beaconami
     * Jedno deviceID nemoze mat viacero merani v rovnakom case => kazde meranie musi mat rozne ID!
     */
    public void postFootprintEntry() {
        HashMap<String, String> hm = new HashMap<String, String>();

        FootprintEntry fpe = new FootprintEntry();
        fpe.setLatitude(1.0);
        fpe.setLongitude(2.0);
        fpe.setAltitude(3.0);

        KdfpEntry kdfp1 = new KdfpEntry();
        kdfp1.setKnownDeviceId("kd0");
        kdfp1.setWifiSignalStrength(33.3);
        kdfp1.setBtSignalStrength(44.4);
        fpe.getKdfpEntries().add(kdfp1);

        KdfpEntry kdfp2 = new KdfpEntry();
        kdfp2.setKnownDeviceId("kd2");
        kdfp2.setWifiSignalStrength(55.5);
        kdfp2.setBtSignalStrength(66.6);
        fpe.getKdfpEntries().add(kdfp2);

        KdfpEntry kdfp3 = new KdfpEntry();
        kdfp3.setKnownDeviceId("kd4");
        kdfp3.setWifiSignalStrength(77.7);
        kdfp3.setBtSignalStrength(88.8);
        fpe.getKdfpEntries().add(kdfp3);

        fpe.setProjectId(1);

        Gson gson = new Gson();
        String jsonString = gson.toJson(fpe);

        hm.put(ServerBridge.REQUEST_BODY, jsonString);
        hm.put(ServerBridge.URL, ServerBridge.URL_ROOT+ ServerBridge.URL_POST_FOOTPRINT_ENTRY);
        hm.put(ServerBridge.METHOD_TYPE, ServerBridge.POST);

        hm.put(ServerBridge.SENDER, "postFootprintEntry");

        aServerBridge.postFootprintEntry(hm);
    }

    /* Lifecycle metody */
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(CLASS_NAME, "in onBind");
        return mBinder;
    }

    public class MyBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(CLASS_NAME, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(CLASS_NAME, "in onUnbind");
        return true;
    }

    @Override
    public void newPositionNotify(Position paPosition) {
        if (paPosition == null) {
            // v pripade, ze je parameter paPosition null, inicializuje sa odosielanie cache suborov
            Log.i(CLASS_NAME, "Inicializacia odosielania suborov");
            if (!aServerBridge.isSendingCacheFiles) {
                // inicializuj odosielanie suborov
                registerMobileDevice(true, aName, aSurname); // najprv zariadenie zaregistrujeme a parameter potom povie triede ServerBridge, ze ma zavolat metodu sendCachedFile a potom sa to bude tocit
            }
        }
        else {
            // zaregistruj zariadenie do databazy
            if (Constants.isOnline(getApplicationContext())) {
                if (!aServerBridge.isRegistered()) {
                    if (!aServerBridge.isRegistering()) {
                        registerMobileDevice(false, aName, aSurname);    // parameter je false, lebo chcem najprv ponechat plne manualnu kontrolu nad odosielanim suborov
                    }
                }
            }

            // skontroluj, ktory tracker ohlasil poziciu
            if (paPosition.getTrackerID() == Tracker.ID_GPS) {
                Log.i(CLASS_NAME, "Prijate suradnice od triedy GPSTracker");
            } else if (paPosition.getTrackerID() == Tracker.ID_WIFI) {
                Log.i(CLASS_NAME, "Prijate suradnice od triedy WifiTracker");
            } else if (paPosition.getTrackerID() == Tracker.ID_BLUETOOTH) {
                Log.i(CLASS_NAME, "Prijate suradnice od triedy BluetoothTracker");
            }

            // ak sme online a zaregistrovany a neodosielame ziadne subory pricom nemame ZIADNE cache subory
            if (Constants.isOnline(getApplicationContext()) && aServerBridge.isRegistered()
                    && !aServerBridge.isSendingCacheFiles && aServerBridge.allCachedFilesSent)
            {
                // odosli data priamo na server
                pushRecord(paPosition);
            } else {
                // inak vytvor cache subor v cache priecinku aplikacie a vygeneruj obsah suboru
                // metodou makePushRecordJson()
                aServerBridge.getFileManager().cacheFile(
                        Long.toString(paPosition.getTimeOfMeasurement()),
                        makePushRecordJson(
                                getGPSTracker().makeGPSEntry(),
                                getWifiTracker().makeWifiEntry(),
                                getBluetoothTracker().makeBTEntry(),
                                paPosition),
                        getApplicationContext());

                aServerBridge.allCachedFilesSent = false;
            }
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(mMessageReceiver);
        Log.i(CLASS_NAME, "LocalBroadcastManager odregistrovany");
        super.onDestroy();
        Log.v(CLASS_NAME, "in onDestroy");
    }
}