package sk.uniza.fri.pedtrack.algorithms;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.HashMap;

import sk.uniza.fri.pedtrack.Constants;

/**
 * Odosiela subory na server HTTP protokolom
 * neskor treba server prerobit na HTTPS
 *
 */
public class ServerBridge {

    private static final String CLASS_NAME = "ServerBridge";
    public static final String URL = "url";
    public static final String URL_ROOT = "http://pedsim.fri.uniza.sk:8080/PedSimStore/PedSimData";
    public static final String URL_POST_REGISTER_DEVICE = "/RegisterDevice";
    public static final String URL_POST_PUSH_RECORD = "/PushRecord";
    public static final String URL_GET_KNOWN_DEVICES = "/KnownDevices";
    public static final String URL_GET_KNOWN_DEVICES_PROJECT_ID = "/KnownDevices?projectId=";
    public static final String URL_POST_FOOTPRINT_ENTRY = "/AddFootprint";
    public static final String URL_POST_ADD_KNOWN_DEVICE = "/AddKnownDevice";     //pridava jeden AP / Beacon
    public static final String URL_POST_ADD_KNOWN_DEVICES = "/AddKnownDevices";   //pridava viacero AP / Beacon
    public static final String URL_POST_PUSH_RECORDS = "/PushRecords";        //odoslanie polohy na server
    public static final String URL_POST_FOOTPRINT_ENTRIES = "/AddFootprints";    //posiela viacero footprintov na server
    public static final String URL_GET_FOOTPRINT_ENTRIES = "/Footprints";    //prijma viacero footprintov
    public static final String URL_GET_FOOTPRINT_ENTRIES_PARAM = "/pedsimData/getFootprints?projectId=";    //prijma viacero footprintov
    public static final String URL_GET_PROJECTS = "/Projects";

    public static final String SENDER = "odosielatel";
    public static final String REQUEST_BODY = "req_body";
    public static final String METHOD_TYPE = "method";
    public static final String POST = "post";
    public static final String GET = "get";
    public static final String FILE_NAME = "filename";
    public static final String CONTINUE_SENDING_FILES = "send_cached_files";

    //registracia pouzivatela
    private boolean isRegistering = false;
    private boolean isRegistered = false;

    //odosielanie cache suborov
    //tieto tri atributy potrebujem mat staticke, aby som mohol podla nich rozhodovat nielen v MainService ale aj v MainActivity
    // ale este uvidim, ci to bude vhodne riesenie
    public static boolean isSendingCacheFiles = false;    //indikuje odosielanie suborov vo vseobecnosti; pouziva sa v mainservice
    public static boolean allCachedFilesSent;     //indikuje, ci su vsetky subory odoslane
    public static boolean stopSendingFiles = true;       //ak je stopSendingFiles true, potom sa odosielanie suborov musi zastavit, lebo sme zastavili sluzbu MainService v metode stopMainService v triede MainActivity

    private FileManager aFileManager;
    private Context aContextFromMainService;
    private static Context aStaticContextFromMainService;

    private String aWifiMACAddress;

    // ********************* KONSTRUKTOR *********************

    /**
     *
     * @param paContext
     * @param paIsCalledFromSettingsFragment pokial instanciu vytvarame zo SettingsFragmentu, potom
     *                                       nechceme, aby sa inicializovali zbytocne atributy,
     *                                       lebo by to vyvolalo zbytocne callbacky
     */
    public ServerBridge(Context paContext, boolean paIsCalledFromSettingsFragment) {
        if (!paIsCalledFromSettingsFragment) {
            aContextFromMainService = paContext;

            initWifiMACaddr(paContext);
            aFileManager = new FileManager();
            if (aFileManager.getNextCachedFile(aContextFromMainService) == null) {
                allCachedFilesSent = true;
            } else {
                allCachedFilesSent = false;
            }
            stopSendingFiles = true;
            isSendingCacheFiles = false;
        }
    }

    //MAC adresa Wi-Fi adaptera telefonu
    public String initWifiMACaddr(Context paContext){
        WifiManager aManager = (WifiManager) paContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo aInfo;

        //ak je wifi zapnute, nechame ho zapnute
        if(aManager.isWifiEnabled()) {
            aInfo = aManager.getConnectionInfo();
            aWifiMACAddress = formatMACAddress(aInfo.getMacAddress());
        } else {
            //ak je vypnute, zapneme ho, zistime MAC adresu a hned ho vypneme
            aManager.setWifiEnabled(true);
            aInfo = aManager.getConnectionInfo();
            aWifiMACAddress = formatMACAddress(aInfo.getMacAddress());
            aManager.setWifiEnabled(false);
        }
        return aWifiMACAddress;
    }

    private String formatMACAddress(String paUnformattedMACAddress) {
        /* uprava MAC adresy do rovnakeho formatu: vsetky pismena budu male, a adresa nebude
         * obsahovat ziadne specialne znaky
         */
        String formattedMACAddress = paUnformattedMACAddress.toLowerCase();
        formattedMACAddress = formattedMACAddress.replaceAll("[^a-f0-9]","");
        return formattedMACAddress;
    }

    public String getWifiMACAddress() {
        return aWifiMACAddress;
    }

    // metody na registraciu pouzivatela
    public boolean isRegistering() {
        return isRegistering;
    }
    public boolean isRegistered() {
        return isRegistered;
    }
    public void setRegistering(boolean paRegistering) {
        isRegistering = paRegistering;
    }
    public void setRegistered(boolean paRegistered) {
        isRegistered = paRegistered;
    }

    public FileManager getFileManager(){
        return aFileManager;
    }

    /**
     * Odosiela informacie serveru v dalsom vlakne, aby sa nebrzdilo hlavne vlakno
     * hm = HashMap
     */
    private class SendMessageToServer extends AsyncTask<HashMap<String, String>, String, Boolean> {
        protected Boolean doInBackground(HashMap<String, String>... params) {
            boolean vysledok = false;

            //sprava sa odosle iba vtedy, ked to ma zmysel -
            if (Constants.isOnline(aContextFromMainService)) {

                HashMap<String, String> hm = params[0];

                //switch - podla medtody (POST/GET) -> porovnava podla REQUEST_METHOD z hm
                switch (hm.get(ServerBridge.METHOD_TYPE)) {

                    case ServerBridge.POST:
                        vysledok = postMessage(hm);
                        return vysledok;

                    case ServerBridge.GET:
                        vysledok = getMessage(hm);
                        return vysledok;
                }

            } else {
//                stopSendingFiles = true;

                // odosli spravu triede MainService
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "offline", "");

                // odosli spravu triede TrackingFragment
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "offline", "");
            }
            return vysledok;
        }

        private boolean postMessage(HashMap<String, String> paHM){
            try {
                HttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(paHM.get(ServerBridge.URL));
                httpPost.setHeader("Accept", "application/json");
                httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
                httpPost.setEntity(new StringEntity(paHM.get(ServerBridge.REQUEST_BODY)));   //string z hm
                HttpResponse resp = client.execute(httpPost);

                if (paHM.get(ServerBridge.SENDER) == "registerDevice"){
                    Log.i(CLASS_NAME, "Registracia Android zariadenia");
                }

                if (resp != null) {
                    if (resp.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                        Log.i("200 OK", paHM.get(SENDER) + " uspesne odoslane");

                        if (paHM.get(ServerBridge.SENDER) == "registerDevice"){
                            setRegistered(true);
                            setRegistering(false);
                            Log.i(CLASS_NAME, "Android zariadenie zaregistrovane");

                            if (paHM.get(ServerBridge.CONTINUE_SENDING_FILES) == "1") {
                                Log.i(CLASS_NAME, "Pokracujeme odosielanim cache suborov");
                                sendCachedFile(aContextFromMainService);
                            }
                        }

                        // handlovanie odoslania suboru
                        if (paHM.get(ServerBridge.FILE_NAME) != null) {
                            // po uspesnom odoslani obsahu suboru na server ho treba zmazat z cache priecinka
                            aFileManager.deleteCacheFile(paHM.get(ServerBridge.FILE_NAME), aContextFromMainService);
                            Log.i(CLASS_NAME, "Subor " +
                                    paHM.get(ServerBridge.FILE_NAME) +
                                    " bol uspesne odoslany");

                            if (aFileManager.getNextCachedFile(aContextFromMainService) == null) {   //vsetky subory su odoslane
                                allCachedFilesSent = true;
                                isSendingCacheFiles = false;
                                Log.i(CLASS_NAME, "Vsetky cache subory odoslane - zastavene z postmessage");

                                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                        "mainservice_broadcast", "all_files_sent", "");
                                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                        "trackingfragment_broadcast", "all_files_sent", "");

                            } else if (stopSendingFiles) {      // odosielanie suborov bolo zastavene, lebo sa zastavila sluzba
                                setDefaultFileSendingSettings();

                                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                        "mainservice_broadcast", "abort", "");
                                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                        "trackingfragment_broadcast", "abort", "");

                            } else {        // inak pokracuj a odosli dalsi subor
                                sendCachedFile(aContextFromMainService);
                            }
                        }

                        return true;
                    } else {
                        stopSendingFiles = true;
                        setDefaultFileSendingSettings();
                        Log.i(CLASS_NAME, "Subor " + paHM.get(ServerBridge.FILE_NAME) +
                                " sa nepodarilo odoslat.");

                        Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                "mainservice_broadcast", "abort", "");
                        Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                                "trackingfragment_broadcast", "abort", "");

                    }
                }
                Log.d("Odpoved servera", "" + resp.getStatusLine().getStatusCode());
            } catch (ClientProtocolException e) {
                e.printStackTrace();

                stopSendingFiles = true;
                setDefaultFileSendingSettings();
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "abort", "");
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "abort", "");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                stopSendingFiles = true;
                setDefaultFileSendingSettings();

                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "abort", "");
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "abort", "");

            } catch (IOException e) {   // tato vynimka nastane ked sa http klient snazi odoslat poziadavku na server, ale uzivatel stratil pripojenie na internet. v takomto pripade treba zastavit odosielanie suborov
                e.printStackTrace();
                stopSendingFiles = true;
                setDefaultFileSendingSettings();
                Log.i(CLASS_NAME, "Subor " + paHM.get(ServerBridge.FILE_NAME) +
                        " sa nepodarilo odoslat - strata pripojenia");      //subor s nazvom "null" je registracia zariadenia

                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "offline", "");
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "offline", "");

            } catch (Exception e) {
                e.printStackTrace();

                stopSendingFiles = true;
                setDefaultFileSendingSettings();
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "abort", "");
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "abort", "");
            }
            return false;
        }

        private boolean getMessage(HashMap<String, String> paHM) {
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(paHM.get(ServerBridge.URL));
                HttpResponse response = client.execute(request);
                if (response != null &&
                    response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                        Log.i("200 OK", paHM.get(SENDER) + " uspesne prijate");

                } else {
                    return false;   // nepodarilo sa prijat odpoved
                }

                // precitaj odpoved
                BufferedReader rd = new BufferedReader
                        (new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                StringBuffer sb = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                Log.d("Odpoved servera", "" + response.getStatusLine().getStatusCode());
                Log.i("Obsah spravy", sb.toString());
                return true;

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        protected void onProgressUpdate() {

        }

        protected void onPostExecute(Boolean result) {
            Log.i("Odoslane?", result ? "ano" : "nie");
        }
    }

    public void getKnownDevices(HashMap<String, String> paKnownDevicesHM){
        new SendMessageToServer().execute(paKnownDevicesHM);
    }

    public void getProjects(HashMap<String, String> paProjectsHM){
        new SendMessageToServer().execute(paProjectsHM);
    }

    public void registerMobileDevice(HashMap<String, String> paMobileDevicesHM){
        new SendMessageToServer().execute(paMobileDevicesHM);
    }

    public void pushRecord(HashMap<String, String> paPushHM){
        new SendMessageToServer().execute(paPushHM);
    }

    public void postFootprintEntry(HashMap<String, String> paPostFootprint){
        new SendMessageToServer().execute(paPostFootprint);
    }

    /**
     * Metoda sendCachedFile odosiela na server najstarsi subor v cache priecinku
     * Metoda sa vola z vnorenej triedy SendMessageToServer pri volani metody
     * registerDevice(true, ...), co znamena, ze sa ma pokracovat v odosielani suborov
     *
     * Kolko trva odoslanie 1000 suborov?
     * 22:34:53 - 22:38:04 => cca 3 min
     * vytazenie CPU: cca 20%
     * Velkost: cca 4MB na disku; skutocna velkost: <400kB
     */
    private void sendCachedFile(Context paContext) {
        // je cache priecinok neprazdny?
        // vyber najstarsi subor
            // uloz obsah suboru do string premennej
            // vytvor hashmap
            // vloz do nej telo spravy zo suboru, typ = post, nazov suboru a url
            // zavolaj pushRecord metodu v tejto triede a posun jej vytvoreny HashMap

        File nextFile = aFileManager.getNextCachedFile(paContext);

        if (Constants.isOnline(aContextFromMainService)) {
            if (nextFile != null) {
                isSendingCacheFiles = true;     //cache subory sa odosielaju
                allCachedFilesSent = false;   //este sme neodoslali vsetky

                String fileName = "";
                String jsonString = "";

                fileName = nextFile.getName();
                jsonString = aFileManager.readFile(nextFile);

                if (jsonString != "") {

                    HashMap<String, String> hm = new HashMap<String, String>();
                    hm.put(ServerBridge.REQUEST_BODY, jsonString);
                    hm.put(ServerBridge.URL, ServerBridge.URL_ROOT + ServerBridge.URL_POST_PUSH_RECORD);
                    hm.put(ServerBridge.METHOD_TYPE, ServerBridge.POST);
                    hm.put(ServerBridge.SENDER, "pushRecord");
                    hm.put(ServerBridge.FILE_NAME, fileName);

                    pushRecord(hm);
                } else {
                    //pokazeny subor treba vymazat
                    aFileManager.deleteCacheFile(fileName, aContextFromMainService);
                }
            } else {
                stopSendingFiles = true;            // uz neodosielam subory
                allCachedFilesSent = true;        // vsetky cache subory sa odoslali
                isSendingCacheFiles = false;        // cache subory sa prestali odosielat
                Log.i(CLASS_NAME, "Vsetky subory odoslane - zastavene zo sendcachedfile");

                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "mainservice_broadcast", "all_files_sent", "");
                Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                        "trackingfragment_broadcast", "all_files_sent", "");

            }
        } else {
            // nie sme online - nema zmysel odosielat subory
            stopSendingFiles = true;
            setDefaultFileSendingSettings();
            Log.i(CLASS_NAME, "Nie sme online");

            Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                    "mainservice_broadcast", "offline", "");
            Constants.broadcastMessage(aContextFromMainService, CLASS_NAME,
                    "trackingfragment_broadcast", "offline", "");
        }
    }

    public void setDefaultFileSendingSettings() {
        allCachedFilesSent = false;   // tvarime sa, ze je este co odoslat
        isSendingCacheFiles = false;    // ale nic sa zatial neodosiela
    }

    
}
