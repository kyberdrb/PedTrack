package sk.uniza.fri.pedtrack.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import sk.uniza.fri.pedtrack.Constants;
import sk.uniza.fri.pedtrack.MainActivity;
import sk.uniza.fri.pedtrack.MainService;
import sk.uniza.fri.pedtrack.R;
import sk.uniza.fri.pedtrack.algorithms.FileManager;
import sk.uniza.fri.pedtrack.algorithms.ServerBridge;

public class TrackingFragment extends Fragment {

    private final String CLASS_NAME = "TrackingFragment";
    private Switch aGPSSwitch;
    private Switch aWifiSwitch;
    private Switch aBluetoothSwitch;
    private TextView aGPSFixedLabel;
    private TextView aFilesRemainingLabel;
    private Button aSendFilesButton;

    private MainService mBoundService;
    private boolean mServiceBound = false;

    private boolean aRequestToSendCachedFiles = false;  // atribut inikujuci poziadavku na odosladnie cache suborov; uzitocny vtedy, ked servica nie je spustena, kedy treba odosielanie inicializovat v metode onServiceConnected

    private Context aContextFromActivity;

    /**
     * Obligatny bezparametricky konstruktor, ktory musi mat kazdy fragment
     */
    public TrackingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tracking_fragment, container, false);
        // okamzite pri vytvoreni fragmentu uloz index tohto fragmentu do atributu v aktivite - ten sa potom zapise do sharedpreferences v metode onPause spolu s dalsimi vecami. naposledy videny fragment sa obnovi v aktivite v metode onCreate
        Constants.broadcastMessage(getActivity(), CLASS_NAME, "mainactivity_broadcast",
                "change_last_seen_fragment_number", "0");
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initGraphicsElements();

        aGPSSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                gpsElementOperation(isChecked);
            }
        });
        aWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wifiElementOperation(isChecked);
            }
        });
        aBluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btElementOperation(isChecked);
            }
        });
        aSendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFilesButtonOperation();
            }
        });

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("trackingfragment_broadcast"));

        // metoda getNextCachedFile mav sebe callback, ktory meni textview a text v buttone
        FileManager fm = new FileManager();
        fm.getNextCachedFile(getActivity());
    }

    /**
     * Our handler for received Intents. This will be called whenever an Intent
     * with an action named "tracking_fragment_broadcast" is broadcasted.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // SPRAVY S OBSAHOM

            String message = intent.getStringExtra("num_of_cached_files");
            if (message != "" && message != null) {
                Log.i(CLASS_NAME, "Este " + message + " suborov");
                setTextOfFilesRemainingLabel(message);
            }

            message = intent.getStringExtra("gps_fixed");
            if (message != "" && message != null) {
                if (message == "y") {
                    aGPSFixedLabel.setText("GPS poloha zameraná!");
                } else {
                    aGPSFixedLabel.setText("Zameriavam GPS polohu ...");
                }
            }

            // SPRAVY S PRAZDNYM OBSAHOM

            message = intent.getStringExtra("offline");
            if (message != null) {      // SPRAVA "offline" JE PRAZDNA !!!
                aSendFilesButton.setText("Odošli súbory!");
                Log.i(CLASS_NAME, "Sme offline");
                // zastavit odosielanie suborov a ak nie su zapnute ziadne trackre, zastavit sluzbu
                if (noTrackersEnabled()) {
                    stopMainService();
                }
                Constants.makeToast(getActivity(), "Ste offline. Zapnite si Wi-Fi alebo mobilný internet.");
            }

            message = intent.getStringExtra("all_files_sent");
            if (message != null) {
                aSendFilesButton.setText("Odošli súbory!");
                // zastavit odosielanie suborov a ak nie su zapnute ziadne trackre, zastavit sluzbu
                if (noTrackersEnabled()) {
                    stopMainService();
                }
                Constants.makeToast(getActivity(), "Všetky súbory úspešne odoslané.");
            }

            message = intent.getStringExtra("abort");
            if (message != null) {
                aSendFilesButton.setText("Odošli súbory!");
                // zastavit odosielanie suborov a ak nie su zapnute ziadne trackre, zastavit sluzbu
                if (noTrackersEnabled()) {
                    stopMainService();
                }
                Constants.makeToast(getActivity(), "Odoslanie súborov prerušené!");
            }

        }
    };

    private void setTextOfFilesRemainingLabel(String paNumberOfCachedFiles) {
        int count = Integer.parseInt(paNumberOfCachedFiles);
        if (count == 0) {
            aFilesRemainingLabel.setText("Nemáte žiadne súbory na odoslanie");
            aSendFilesButton.setText("Odošli súbory!");
        } else if (count == 1) {
            aFilesRemainingLabel.setText("Máte " + paNumberOfCachedFiles + " súbor na odoslanie");
        } else if (count >= 2 && count <= 4) {
            aFilesRemainingLabel.setText("Máte " + paNumberOfCachedFiles + " súbory na odoslanie");
        } else {
            aFilesRemainingLabel.setText("Máte " + paNumberOfCachedFiles + " súborov na odoslanie");
        }
    }

    /**
     * inicializuje graficke prvky (elementy) tohto fragmentu
     */
    private void initGraphicsElements() {
        // Checkboxy
        aGPSSwitch = (Switch) getActivity().findViewById(R.id.switch_gps);
        aWifiSwitch = (Switch) getActivity().findViewById(R.id.switch_wifi);
        aBluetoothSwitch = (Switch) getActivity().findViewById(R.id.switch_bt);
        aGPSFixedLabel = (TextView) getActivity().findViewById(R.id.gps_fixed);
        aFilesRemainingLabel = (TextView) getActivity().findViewById(R.id.cached_files_remaining);

        // button
        aSendFilesButton = (Button) getActivity().findViewById(R.id.button_send_files);

    }

    //*********************************__Other__*********************************
    /**
     * metoda na testovanie jednotlivych sprav odosielanych a prijatych zo servera
     */
    private void serverCommunication() {
        mBoundService.getKnownDevices();
        mBoundService.getProjects();
        mBoundService.postFootprintEntry();
    }

    /**
     * vytahuje hodnoty podla klucov zo sharedpreferences suboru
     * treba to ifovat podla datoveho typu, lebo sa neda vratit resp. zapisat genericky objekt,
     * ale iba konkretny datovy typ
     * @param paKeyOfPreference kluc preferencie zo settingsfragmentu
     * @return genericky objekt - treba pretypovat pri kazom volani metody
     */
    @Nullable
    private String getValueFromSharedPrefs(String paKeyOfPreference) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Constants.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        Log.i(CLASS_NAME, "Nastavenie " + paKeyOfPreference + " bolo obnovene na " +
                    String.valueOf(sharedPreferences.getString(paKeyOfPreference, "")));
        return sharedPreferences.getString(paKeyOfPreference, "");
    }

    //*********************************__MainService__*********************************
    private void startMainService() {
        Intent intent = new Intent(getActivity(), MainService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mServiceConnection, 0);
    }

    private void stopMainService() {
        setClickableStateOfGuiElements(false);  // deaktivuj gui elementy
        Intent i = new Intent(getActivity(), MainService.class);
        getActivity().stopService(i);
        if (mServiceBound) {
            ServerBridge.stopSendingFiles = true;
            ServerBridge.allCachedFilesSent = false;
            ServerBridge.isSendingCacheFiles = false;

            if (mBoundService.getGPSTracker().isTrackingAllowed()) {
                disableGPS();
            } else if (mBoundService.getWifiTracker().isTrackingAllowed()) {
                disableWifi();
            } else if (mBoundService.getBluetoothTracker().isTrackingAllowed()) {
                disableBluetooth();
            }

            getActivity().unbindService(mServiceConnection);
        }
        mServiceBound = false;
        mBoundService = null;
        Log.i(CLASS_NAME, "Servica zlikvidovana");

        setClickableStateOfGuiElements(true);   // po uspesnom ukonceni sluzby aktivuj gui elementy
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //radsej sa nespoliehat na onServiceDisconnected
            /*
            onServiceDisconnected is not supposed to be raised when you unbind your service, so
            don't rely on it. It is supposed to inform you in case the connection between your
            Service and ServiceConnection is dropped.
            */
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainService.MyBinder myBinder = (MainService.MyBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;

            // nastav meno a priezvisko z nastaveni
            mBoundService.setName((String) getValueFromSharedPrefs("first_name"));
            mBoundService.setSurname((String) getValueFromSharedPrefs("surname"));

            // nastartuj sledovanie - ak su uz vopred nastavene nejake checkboxy trackerov a zaroven
            // este nemaju povolene sledovanie a aktivujeme gui elementy
            if (aGPSSwitch.isChecked() || aGPSSwitch.isActivated()) {
                if (!mBoundService.getGPSTracker().isTrackingAllowed()) {
                    enableGPS();
                }
            }
            if (aWifiSwitch.isChecked()) {
                if (!mBoundService.getWifiTracker().isTrackingAllowed()) {
                    enableWifi();
                }
            }
            if (aBluetoothSwitch.isChecked()) {
                if (!mBoundService.getBluetoothTracker().isTrackingAllowed()) {
                    enableBluetooth();
                }
            }
            if (aRequestToSendCachedFiles) {
                initializeSendingCachedFiles();
            }

            // obnovime funkcnost gui elementov, aby sme mali poriadok v sluzbach a instanciach z MainService
            setClickableStateOfGuiElements(true);
        }
    };

    private boolean noTrackersEnabled() {
        return !(aGPSSwitch.isChecked() || aWifiSwitch.isChecked() ||
                aBluetoothSwitch.isChecked());
    }

    private boolean atLeastOneTrackerEnabled() {
        return (aGPSSwitch.isChecked() || aWifiSwitch.isChecked() ||
                aBluetoothSwitch.isChecked());
    }

    private void setClickableStateOfGuiElements(boolean paClickable) {
        aGPSSwitch.setEnabled(paClickable);
        aWifiSwitch.setEnabled(paClickable);
        aBluetoothSwitch.setEnabled(paClickable);
        aSendFilesButton.setEnabled(paClickable);
    }

    //********************************__GPS__*********************************
    private void gpsElementOperation(boolean paIsChecked) {
        if (paIsChecked) {
            if (!isGPSEnabledInSettings()) {
                buildAlertMessageNoGps();
                aGPSSwitch.setChecked(false);
            }
            else if(gpsFineLocationPermissionDisabled()) { //teraz skontroluj, ci mas opravnenia
                Log.d("duck", "duck");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                aGPSSwitch.setChecked(false);
            }
            else {

                if (mBoundService == null) {
                    // deaktivuj vsetky ovladacie prvky
                    setClickableStateOfGuiElements(false);
                    startMainService();
                }

                if (mServiceBound) {
                    enableGPS();
                }
            }
        } else {
            if (mServiceBound) {
                disableGPS();
                if (noTrackersEnabled()) {
                    stopMainService();
                }
            }
        }
    }

    private void enableGPS() {
        mBoundService.getGPSTracker().setTrackingAllowed(true);
        mBoundService.getGPSTracker().enable(getActivity(), 3000);  // hodnota druheho parametra zatial nic neznamena, vid komentar metody "enable" v triede gpstracker
        aGPSFixedLabel.setAlpha(1);     // label s indikaciu zameriavania GPS je zobrazeny iba vtedy, ked je zapnute GPS sledovanie
        ServerBridge.stopSendingFiles = false;
    }

    private void disableGPS() {
        mBoundService.getGPSTracker().setTrackingAllowed(false);
        mBoundService.getGPSTracker().disable();
        aGPSFixedLabel.setAlpha(0);     // po vypnuti sledovania GPS "zmizne" aj label
    }

    private boolean gpsFineLocationPermissionDisabled() {
        return ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Kontroluje, ci je "Poloha" v Android nastaveniach zapnuta
     */
    private boolean isGPSEnabledInSettings() {
        LocationManager manager = (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );
        return manager.isProviderEnabled( LocationManager.GPS_PROVIDER );
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("GPS poloha je momentálne vypnutá, chcete ju zapnúť?")
                .setCancelable(false)
                .setPositiveButton("Áno", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //*********************************__Wi-Fi__*********************************
    private void wifiElementOperation(boolean paIsChecked) {
        if (paIsChecked) {
            if (mBoundService == null) {
                setClickableStateOfGuiElements(false);
                startMainService();
            }

            if (mServiceBound) {
                enableWifi();
            }
        } else {
            if (mServiceBound) {
                disableWifi();
                if (noTrackersEnabled()) {
                    stopMainService();
                }
            }
        }
    }

    private void enableWifi() {
        mBoundService.getWifiTracker().setTrackingAllowed(true);
        int refreshInterval;
        try {
            refreshInterval = Integer.parseInt(getValueFromSharedPrefs("wifi_refresh_interval"));
        } catch (NumberFormatException e) {
            refreshInterval = 1;
        }
        mBoundService.getWifiTracker().enable(getActivity(), refreshInterval);
        ServerBridge.stopSendingFiles = false;
    }

    private void disableWifi() {
        mBoundService.getWifiTracker().setTrackingAllowed(false);
        mBoundService.getWifiTracker().unregisterBroadcastListener(getActivity());
    }

    //*********************************__Bluetooth__*********************************
    private void btElementOperation(boolean paIsChecked) {
        if (paIsChecked) {
            if (mBoundService == null) {
                setClickableStateOfGuiElements(false);
                startMainService();
            }

            if (mServiceBound) {
                enableBluetooth();
            }
        } else {
            if (mServiceBound) {
                disableBluetooth();
                if (noTrackersEnabled()) {
                    stopMainService();
                }
            }
        }
    }

    private void enableBluetooth() {
        mBoundService.getBluetoothTracker().setTrackingAllowed(true);
        int refreshInterval;
        try {
            refreshInterval = Integer.parseInt(getValueFromSharedPrefs("bt_refresh_interval"));
        } catch (NumberFormatException e) {
            refreshInterval = 1;
        }

        // poistka proti tomu, ked zariadenie nema bluetooth modul - radsej som to osetril v aktivite
        try {
            mBoundService.getBluetoothTracker().enable(getActivity(), refreshInterval);
        } catch (NullPointerException e) {
            Constants.makeToast(getActivity(), "Bluetooth adaptér nenajdený");
            aBluetoothSwitch.setChecked(false);
        }
        ServerBridge.stopSendingFiles = false;
    }

    private void disableBluetooth() {
        mBoundService.getBluetoothTracker().setTrackingAllowed(false);
        mBoundService.getBluetoothTracker().unregisterBroadcastListener(getActivity());
    }

    /**
     * inicializacia odosielania suborov
     */
    private void sendFilesButtonOperation() {
        aSendFilesButton.setText("Odosielam súbory ...");
        if (mBoundService == null) {
            setClickableStateOfGuiElements(false);
            aRequestToSendCachedFiles = true;
            startMainService();
        } else {
            initializeSendingCachedFiles();
        }
    }

    private void initializeSendingCachedFiles() {
        ServerBridge.stopSendingFiles = false;
        mBoundService.newPositionNotify(null);
        aRequestToSendCachedFiles = false;
    }

    //*********************************__Lifecycle && GUI save/restore__*********************************
    /**
     * Ulozi stav GUI, akonahle ju uzivatel minimalizuje
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.i(CLASS_NAME, "onPause");
        if (mServiceBound) {
            getActivity().unbindService(mServiceConnection);
        }
        mServiceBound = false;
        saveGUI();
    }

    /**
     * Akonahle sa aktivita dostane do popredia, obnovi stav GUI
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLASS_NAME, "onResume");
        restoreGUI();

        if (!mServiceBound && atLeastOneTrackerEnabled()) {
            getActivity().bindService(new Intent(getActivity(), MainService.class), mServiceConnection, 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(CLASS_NAME, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(CLASS_NAME, "in onStop");
    }

    private void saveGUI() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Constants.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("checkGPS", aGPSSwitch.isChecked());
        editor.putBoolean("checkWifi", aWifiSwitch.isChecked());
        editor.putBoolean("checkBluetooth", aBluetoothSwitch.isChecked());
        editor.putFloat("gps_fixed_transparency",aGPSFixedLabel.getAlpha());
        editor.commit();

        Log.i(CLASS_NAME, "saveGUI - GUI fragmentu ulozene");
    }

    private void restoreGUI() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Constants.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);

        aGPSSwitch.setChecked(sharedPreferences.getBoolean("checkGPS", false));
        aWifiSwitch.setChecked(sharedPreferences.getBoolean("checkWifi", false));
        aBluetoothSwitch.setChecked(sharedPreferences.getBoolean("checkBluetooth", false));
        aGPSFixedLabel.setAlpha(sharedPreferences.getFloat("gps_fixed_transparency", 0));

        Log.i(CLASS_NAME, "restoreGUI - GUI fragmentu obnovene");
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        Log.i(CLASS_NAME, "LocalBroadcastManager odregistrovany");
        super.onDestroy();
        Log.i(CLASS_NAME, "onDestroy - fragment zniceny");
    }
}