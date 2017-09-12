package com.sirot.positivitydose;


import android.os.Bundle;
import android.support.v4.app.Fragment;

public class PositiveDoseActivity extends SingleFragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
    }

    @Override
    protected Fragment createFragment() {
        return new PositiveDoseFragment();
    }


}
