package sk.uniza.fri.pedtrack;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

/**
 * Created by andrej on 5.7.2016.
 */
public abstract class Constants {

    public static String PREFERENCES_FILE_NAME = "preferences";

    /**
     * metoda zistuje, ci mame pristup na internet
     * @param paContext
     * @return
     */
    public static boolean isOnline(Context paContext) {
        ConnectivityManager connManager = (ConnectivityManager) paContext.getSystemService(
                paContext.CONNECTIVITY_SERVICE);
        NetworkInfo mActiveConn = connManager.getActiveNetworkInfo();
        return (mActiveConn != null);
    }

    /**
     * Metoda broadcastMessage umoznuje poslat cokolvek akejkolvek triede, pokial ma zaregistrovany
     * LocalBroadcastManager (implementacia BroadcastReceivera)
     *
     * Zatial potrebujem odosielat iba jednu spravu s nazvom "message", ale pokial to bude nutne
     * da sa parametrom posunut aj cely zoznam sprav
     *
     * @param paContext kontext, v ramci, ktoreho spravu odosielame
     * @param paSenderClassName nazov odosielajucej triedy
     * @param paBroadcastType kazda trieda ma svoj intentfilter, ktorym filtruje prijimane
     *                        spravy podla jej typu
     *                        typ spravy = <nazov_triedy_malymi_pismenami>_broadcast
     *                        MainActivity -> mainactivity_broadcast
     *                        TrackingFragment -> trackingfragment_broadcast
     *                        MainService -> mainservice_broadcast
     * @param paMessageType typ spravy, ktoru chceme odoslat - specifikovane v metode onReceive
     *                      v triede TrackingFragment, MainActivity a MainService
     * @param paMessageContent obsah samotnej spravy
     *                         na zaklade obsahu spravy mozeme menit sluzbu/aktivitu zvonku napr.:
     *                              - rozhodovat o zivotnom cykle objektu, ktory prijal tuto spravu
     *                              - menit gui
     *                              - menit sharedpreferences, aby sa gui otvorilo tak
     */
    public static void broadcastMessage(Context paContext,
                                        String paSenderClassName, String paBroadcastType,
                                        String paMessageType, String paMessageContent)
    {
        Intent intent = new Intent(paBroadcastType);
        intent.putExtra(paMessageType, paMessageContent);
        LocalBroadcastManager.getInstance(paContext).sendBroadcast(intent);
        Log.i(paSenderClassName, "Odoslal som spravu " + paMessageType + " typu " + paBroadcastType
                + " so znenim: " + paMessageContent );
    }

    public static void makeToast(Context paContext, String paMessage) {
        CharSequence text = paMessage;
        int duration = Toast.LENGTH_SHORT;

        Toast.makeText(paContext, text, duration).show();
    }

    public static long updateDate() {
        return new Date().getTime();
    }
}
