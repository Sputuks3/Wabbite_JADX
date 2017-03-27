package com.Revsoft.Wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import com.Revsoft.Wabbitemu.utils.ViewUtils;

public class LandingPageView extends RelativeLayout {
    private final RadioGroup mRadioGroup = ((RadioGroup) ViewUtils.findViewById((View) this, (int) R.id.setupOptionsRadioGroup, RadioGroup.class));

    public LandingPageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LayoutInflater.from(context).inflate(R.layout.landing_page, this, true);
        AdUtils.loadAd(findViewById(R.id.adView));
    }

    public int getSelectedRadioId() {
        return this.mRadioGroup.getCheckedRadioButtonId();
    }
}
