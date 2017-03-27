package com.Revsoft.Wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import javax.annotation.Nonnull;

public class ChooseOsPageView extends RelativeLayout {
    private final ProgressBar mLoadingSpinner = ((ProgressBar) findViewById(R.id.loadingSpinner));
    private final TextView mMessage = ((TextView) findViewById(R.id.message));

    public ChooseOsPageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LayoutInflater.from(context).inflate(R.layout.choose_os_page, this, true);
        AdUtils.loadAd(findViewById(R.id.adView));
    }

    @Nonnull
    public TextView getMessage() {
        return this.mMessage;
    }

    public ProgressBar getLoadingSpinner() {
        return this.mLoadingSpinner;
    }
}
