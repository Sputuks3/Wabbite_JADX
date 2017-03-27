package com.Revsoft.Wabbitemu.activity;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import com.Revsoft.Wabbitemu.utils.ViewUtils;

public class AboutActivity extends Activity {
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.about);
        setTitle(R.string.about);
        ViewGroup textLinkContainer = (ViewGroup) ViewUtils.findViewById((Activity) this, (int) R.id.openSourceLinks, ViewGroup.class);
        for (int i = 0; i < textLinkContainer.getChildCount(); i++) {
            View view = textLinkContainer.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        try {
            ((TextView) ViewUtils.findViewById((Activity) this, (int) R.id.aboutVersion, TextView.class)).setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
            Log.e("About", "Version exception", e);
        }
        AdUtils.loadAd(findViewById(R.id.adView));
    }
}
