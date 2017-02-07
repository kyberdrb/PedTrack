package sk.uniza.fri.pedtrack.algorithms;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import sk.uniza.fri.pedtrack.Constants;

/**
 * Projektove subory, zoznam wifi ap a beacnov
 * "/data/data/sk.uniza.fri.pedtrack/files"
 *
 * Polohy
 * "/data/data/sk.uniza.fri.pedtrack/cache/positions
 */
public class FileManager {

    private final String CLASS_NAME = "FileManager";

    public void cacheFile(String paFileName, String paFileContent, Context paContext) {
        try {
            if (paFileContent != "") {
                File cacheDir = new File(paContext.getCacheDir(), "positions");
                if(!cacheDir.exists()) {
                    cacheDir.mkdir();
                    Log.d(CLASS_NAME, "priecinok na ukladanie pozicii nebol najdeny " +
                            "a bol uspesne vytvoreny");
                }
                File cacheFile = new File(paContext.getCacheDir(), "positions" +
                        File.separator + paFileName);
                FileOutputStream fos = new FileOutputStream(cacheFile);
                fos.write(paFileContent.getBytes());
                fos.close();

                // aktualizuj pocet cache suborov v gui (trackingfragment)
                File files[] = cacheDir.listFiles();
                Constants.broadcastMessage(paContext, CLASS_NAME,
                        "trackingfragment_broadcast", "num_of_cached_files",
                        Integer.toString(files.length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteCacheFile(String paFileName, Context paContext) {
        File cacheDir = new File(paContext.getCacheDir(), "positions");
        File file = new File(cacheDir, paFileName);
        file.delete();

        // aktualizuj label v trackingfragmente
        File files[] = cacheDir.listFiles();
        Constants.broadcastMessage(paContext, CLASS_NAME,
                "trackingfragment_broadcast", "num_of_cached_files",
                Integer.toString(files.length));
    }

    /**
     *
     * @param paContext
     * @return najstarsi subor v priecinku
     */
    public File getNextCachedFile(Context paContext) {
        File cacheDir = new File(paContext.getCacheDir(), "positions");

        if(!cacheDir.exists()) {
            cacheDir.mkdir();
            Log.d(CLASS_NAME, "priecinok na ukladanie pozicii nebol najdeny " +
                    "a bol uspesne vytvoreny");
        }

        File files[] = cacheDir.listFiles();
        // callback do treckingfragmentu (settext textview) a mainservicy (sharedprefs editor), aby
        // sme mohli oznamovat pocet suborov a menit labely v gui
        Constants.broadcastMessage(paContext, CLASS_NAME,
                "trackingfragment_broadcast", "num_of_cached_files",
                Integer.toString(files.length));

        int oldestFileIndex = 0;
        long actualFileNameNumber = 0;
        long oldestFileNameNumber = Long.MAX_VALUE;

        try {
            for (int i = 0; i < files.length; i++) {
                if (Long.parseLong(files[i].getName()) < oldestFileNameNumber) {
                    oldestFileNameNumber = actualFileNameNumber;
                    oldestFileIndex = i;
                }
            }
            return files[oldestFileIndex];

        } catch (ArrayIndexOutOfBoundsException e){
            Log.i(CLASS_NAME, "Priecinok cache je prazdny");
        } catch (NumberFormatException e) {
            Log.i(CLASS_NAME, "Neplatny format cisla - toto nie je subor s polohou: nema timestamp " +
                    "ako nazov suboru. Zrejme su v cache priecinku aj ine subory.");
        }
        return null;
    }

    public String readFile (File paFile) {
        File fl = null;
        FileInputStream fin = null;
        String ret = "";
        try {
            fl = paFile;
            fin = new FileInputStream(fl);
            ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
