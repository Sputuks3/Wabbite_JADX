package com.Revsoft.Wabbitemu.wizard.view;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import com.Revsoft.Wabbitemu.utils.ViewUtils;

public class OsPageView extends RelativeLayout {
    private final RadioGroup mRadioGroup = ((RadioGroup) ViewUtils.findViewById((View) this, (int) R.id.setupOsAcquisistion, RadioGroup.class));
    private final Spinner mSpinner = ((Spinner) ViewUtils.findViewById((View) this, (int) R.id.osVersionSpinner, Spinner.class));

    public OsPageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LayoutInflater.from(context).inflate(R.layout.os_page, this, true);
        ((TextView) ViewUtils.findViewById((View) this, (int) R.id.osTerms, TextView.class)).setMovementMethod(LinkMovementMethod.getInstance());
        AdUtils.loadAd(findViewById(R.id.adView));
    }

    public Spinner getSpinner() {
        return this.mSpinner;
    }

    public int getSelectedRadioId() {
        return this.mRadioGroup.getCheckedRadioButtonId();
    }
}
