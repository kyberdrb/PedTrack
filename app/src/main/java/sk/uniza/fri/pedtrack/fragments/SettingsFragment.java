package sk.uniza.fri.pedtrack.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import sk.uniza.fri.pedtrack.Constants;
import sk.uniza.fri.pedtrack.R;
import sk.uniza.fri.pedtrack.algorithms.ServerBridge;

public class SettingsFragment extends PreferenceFragment {

    private final String CLASS_NAME = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Define the settings file to use by this settings fragment - urcuje nazov suboru
        // ktory bude pouzivat tento fragment defaultne na ukladanie nastaveni
        getPreferenceManager().setSharedPreferencesName(Constants.PREFERENCES_FILE_NAME);

        Constants.broadcastMessage(getActivity(), CLASS_NAME, "mainactivity_broadcast",
                "change_last_seen_fragment_number", "1");

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // vypni policko MAC adresy a nastav mu MAC adresu
        findPreference("device_id_wifi").setEnabled(false);
        findPreference("device_id_wifi").setTitle(
                new ServerBridge(getActivity(), true).initWifiMACaddr(getActivity()));

        // Listener pre wifi interval
        Preference.OnPreferenceChangeListener preferenceChangedListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // musim porovnavat kluc a kluc, lebo ked som sa pokusal porovnavat dva totozne stringy, tak mi to vyhodnotilo ako dve rozne veci, aj ked bol retazec totozny
                // porovnavanie cez equals vyriesilo problem
                Log.d(CLASS_NAME, String.valueOf("wifi_refresh_interval"));
                if (    preference.getKey().equals("wifi_refresh_interval") ||
                        preference.getKey().equals("bt_refresh_interval"))
                {
                    return numberCheck(newValue, preference.getKey());

                } else if (
                        preference.getKey() == getPreferenceManager().findPreference("first_name").getKey() ||
                        preference.getKey() == getPreferenceManager().findPreference("surname").getKey())
                {
                    // callback do mainservicy
                    Constants.broadcastMessage(getActivity(), CLASS_NAME, "mainservice_broadcast",
                            preference.getKey(), (String) newValue);

                    // callback do trackingragmentu na ulozenie nastaveni mena a priezviska

                }
                return false;
            }
        };
        EditTextPreference wifiPref = (EditTextPreference) findPreference("wifi_refresh_interval");
        wifiPref.setOnPreferenceChangeListener(preferenceChangedListener);

        EditTextPreference btPref = (EditTextPreference) findPreference("bt_refresh_interval");
        btPref.setOnPreferenceChangeListener(preferenceChangedListener);
    }

    /**
     * kontrola spravnosti na interval aktualizacie polohy pre wifi / bluetooth
     * @param paValue
     * @param paKeyOfPreference kto si nechal skontrolovat hodnotu -> wifi alebo bluetooth
     *                        podla tohto parametra ukladame prislusnu hodnotu do sharedprefs
     * @return uspech operacie: boli nastavenia zmenene?
     */
    private boolean numberCheck(Object paValue, String paKeyOfPreference) {
        try {
            if (    paKeyOfPreference.equals("wifi_refresh_interval") &&
                    Integer.parseInt((String)paValue) >= 1 &&
                    Integer.parseInt((String)paValue) <= 10)
            {
                // callback do wifitrackera, aby sa interval nastavil za behu
                Constants.broadcastMessage(getActivity(), CLASS_NAME, "wifitracker_broadcast",
                        "changeWifiRefreshInterval", (String) paValue);
                Log.d(CLASS_NAME, "Nastavenie wifi intervalu ulozene: nova hodnota = " + paValue);
                return true;

            } else if (
                    paKeyOfPreference.equals("bt_refresh_interval") &&
                    Integer.parseInt((String)paValue) >= 1 &&
                    Integer.parseInt((String)paValue) <= 10)
            {
                // callback do bluetoothtrackera, aby sa interval nastavil za behu
                Constants.broadcastMessage(getActivity(), CLASS_NAME,"bluetoothtracker_broadcast",
                        "changeBtRefreshInterval", (String) paValue);
                Log.d(CLASS_NAME, "Nastavenie bluetooth intervalu ulozene: nova hodnota = " + paValue);
                return true;

            } else {
                    Toast.makeText(getActivity(), paValue + " je mimo platného rozsahu.",
                            Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Znak \"" + paValue + "\" nie je číslo",
                    Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}