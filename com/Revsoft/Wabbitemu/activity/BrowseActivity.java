package com.Revsoft.Wabbitemu.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.fragment.BrowseFragment;
import com.Revsoft.Wabbitemu.utils.IntentConstants;
import com.Revsoft.Wabbitemu.utils.OnBrowseItemSelected;

public class BrowseActivity extends Activity implements OnBrowseItemSelected {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String regex = intent.getStringExtra(IntentConstants.EXTENSION_EXTRA_REGEX);
        String description = intent.getStringExtra(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING);
        Bundle bundle = new Bundle();
        bundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, regex);
        bundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, description);
        BrowseFragment fragment = new BrowseFragment();
        fragment.setCallback(this);
        fragment.setArguments(bundle);
        setTitle(R.string.selectFile);
        getFragmentManager().beginTransaction().replace(16908290, fragment).commit();
    }

    public void onBrowseItemSelected(String fileName) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(IntentConstants.FILENAME_EXTRA_STRING, fileName);
        setResult(-1, returnIntent);
        finish();
    }
}
