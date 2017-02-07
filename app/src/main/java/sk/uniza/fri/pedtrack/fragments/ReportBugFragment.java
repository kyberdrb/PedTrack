package sk.uniza.fri.pedtrack.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sk.uniza.fri.pedtrack.Constants;
import sk.uniza.fri.pedtrack.R;
import sk.uniza.fri.pedtrack.algorithms.ServerBridge;

public class ReportBugFragment extends Fragment {

    private final String CLASS_NAME = "AboutFragment";

    public ReportBugFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.report_bug_fragment, container, false);
        // okamzite pri vytvoreni fragmentu uloz index tohto fragmentu
        Constants.broadcastMessage(getActivity(), CLASS_NAME, "mainactivity_broadcast",
                "change_last_seen_fragment_number", "3");
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(CLASS_NAME, "onDestroy - fragment zniceny");
    }
}
