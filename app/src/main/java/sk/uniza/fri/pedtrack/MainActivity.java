package sk.uniza.fri.pedtrack;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import sk.uniza.fri.pedtrack.adapter.SlidingMenuAdapter;
import sk.uniza.fri.pedtrack.algorithms.ServerBridge;
import sk.uniza.fri.pedtrack.fragments.AboutFragment;
import sk.uniza.fri.pedtrack.fragments.ReportBugFragment;
import sk.uniza.fri.pedtrack.fragments.SettingsFragment;
import sk.uniza.fri.pedtrack.fragments.TrackingFragment;
import sk.uniza.fri.pedtrack.model.ItemSlideMenu;

public class MainActivity extends AppCompatActivity {

    private final String CLASS_NAME = "MainActivity";
    private List<ItemSlideMenu> aListSliding;
    private SlidingMenuAdapter aSlidingAdapter;
    private ListView aSlidingListView;
    private DrawerLayout aDrawerLayout;
    private ActionBarDrawerToggle aActionBarDrawerToggle;

    private int aLastFragmentNum = 0;   // uklada index posledneho videneho fragmentu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //Init component
        aSlidingListView = (ListView) findViewById(R.id.lv_sliding_menu);
        aDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        aListSliding = new ArrayList<>();
        //Add item for sliding list
        aListSliding.add(new ItemSlideMenu(R.mipmap.ic_launcher, "Sledovanie"));
        aListSliding.add(new ItemSlideMenu(R.drawable.ic_action_settings, "Nastavenia"));
        aListSliding.add(new ItemSlideMenu(R.drawable.ic_action_about, "O aplikácií"));
        aListSliding.add(new ItemSlideMenu(R.mipmap.ic_launcher, "Nahlásiť problém"));
        aSlidingAdapter = new SlidingMenuAdapter(this, aListSliding);
        aSlidingListView.setAdapter(aSlidingAdapter);

        //Display icon to open/ close sliding list
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // zobrazime ten fragment, ktory bol naposledy ulozeny - index fragmentu sa uklada v metode onPause
        restoreLastSeenFragmentNum(true);

        //Hanlde on item click
        aSlidingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchToFragment(position, false);
            }
        });

        aActionBarDrawerToggle = new ActionBarDrawerToggle(this, aDrawerLayout, R.string.drawer_opened, R.string.drawer_closed){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };

        aDrawerLayout.setDrawerListener(aActionBarDrawerToggle);

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("mainactivity_broadcast"));
    }

    /**
     * Our handler for received Intents. This will be called whenever an Intent
     * with an action named "tracking_fragment_broadcast" is broadcasted.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("change_last_seen_fragment_number");
            if (message != "" && message != null) {
                Log.i(CLASS_NAME, "Poslednu zobrazeny fragment ma cislo: " + message);
                aLastFragmentNum = Integer.parseInt(message);
            }

            message = intent.getStringExtra("saveToShare");
            if (message != "" && message != null) {
                Log.i(CLASS_NAME, "Poslednu zobrazeny fragment ma cislo: " + message);
                aLastFragmentNum = Integer.parseInt(message);
            }
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        aActionBarDrawerToggle.syncState();
    }

    /**
     * Metoda prepina fragmenty a upravuje niektore graficke prvky aktivity
     * @param paPosition index fragmentu
     * @param paRestoreFromOnCreate vynutenie prepnutia na dany fragment pri obnoveni aktivity
     *                              z metody onCreate
     */
    private void switchToFragment(int paPosition, boolean paRestoreFromOnCreate) {
        Log.i(CLASS_NAME, "Prepiname fragment z metody onCreate? \t " + paRestoreFromOnCreate);
        if (paPosition != aLastFragmentNum || paRestoreFromOnCreate) {
            //Set title
            setTitle(aListSliding.get(paPosition).getTitle());
            //item selected
            aSlidingListView.setItemChecked(paPosition, true);
            //Display with last seen fragment when start
            replaceFragment(paPosition);
        }
        //Close menu
        aDrawerLayout.closeDrawer(aSlidingListView);
    }

    /**
     * metoda na vymenu fragmentov
     * kazdy fragment ma svoj index v zozname fragmentov (v draweri)
     * @param pos index fragmentu
     *            0: TrackingFragment
     *            1: SettingsFragment
     *            2: AboutFragment
     *            3: ReportBugFragment
     */
    private void replaceFragment(int pos) {
        Fragment fragment = null;
        switch (pos) {
            case 0:
                fragment = new TrackingFragment();
                break;
            case 1:
                fragment = new SettingsFragment();
                break;
            case 2:
                fragment = new AboutFragment();
                break;
            case 3:
                fragment = new ReportBugFragment();
                break;
            default:
                fragment = new TrackingFragment();
                break;
        }

        if(null!=fragment) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.main_content, fragment);
            //transaction.addToBackStack(null);     // tento riadok sposoboval duplicitu fragmentov tym, ze ich instancie ukladal do zasobnika, co nebolo ziaduce; odstranenim tohto riadku sa docielilo to, ze v pamati existuje iba instancia prave zobrazeneho fragmentu
            transaction.commit();
        }
    }

    /**
     * umoznuje otvorit a zatvorit drawer cez hamburger button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (this.aDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    this.aDrawerLayout.closeDrawer(GravityCompat.START);
                    Log.i(CLASS_NAME, "Drawer zatvoreny cez sipku (sipka sa transformovala " +
                            "na hamburger button)");
                } else {
                    aDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER
                    Log.i(CLASS_NAME, "Drawer otvoreny cez hamburger button");
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ked uzivatel stlaci "back", najprv sa zatvori drawer, a az potom cela aktivita
     */
    @Override
    public void onBackPressed() {
        if (this.aDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.aDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * ulozi posledny videny fragment
     */
    private void saveLastSeenFragmentNum() {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("fragment_number", aLastFragmentNum);
        editor.commit();
        Log.i(CLASS_NAME, "saveLastSeenFragmentNum - posledny videny fragment ulozeny");
    }

    /**
     * obnovi index posledneho videneho fragmentu a zobrazi ho
     */
    private void restoreLastSeenFragmentNum(boolean paRestoreFromOnCreate) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        aLastFragmentNum = sharedPreferences.getInt("fragment_number", 0);
        switchToFragment(aLastFragmentNum, paRestoreFromOnCreate);
        Log.i(CLASS_NAME, "restoreLastSeenFragmentNum - posledny videny fragment obnoveny");
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveLastSeenFragmentNum();
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.i(CLASS_NAME, "LocalBroadcastManager odregistrovany");
        super.onDestroy();
        Log.i(CLASS_NAME, "Aktivita znicena");
    }
}
