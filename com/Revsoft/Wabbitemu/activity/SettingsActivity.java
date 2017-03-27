package com.Revsoft.Wabbitemu.activity;

import android.app.Activity;
import android.os.Bundle;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;

public class SettingsActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);
        setContentView(R.layout.settings);
        AdUtils.loadAd(findViewById(R.id.adView));
    }
}
