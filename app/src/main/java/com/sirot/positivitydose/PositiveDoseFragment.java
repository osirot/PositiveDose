package com.sirot.positivitydose;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by sirot on 9/9/2017.
 */

public class PositiveDoseFragment extends Fragment {

    private Button mSelectFriend;
    private Button mSendButton;
    private TextView mLastPositivityDoseSent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_positive_dose, container,false);
        mSelectFriend = (Button) v.findViewById(R.id.select_friend);
        mSendButton = (Button) v.findViewById(R.id.send_positivity_dose);
        mLastPositivityDoseSent = (TextView) v.findViewById(R.id.last_positivity_dose_sent);
        return v;
    }
}
