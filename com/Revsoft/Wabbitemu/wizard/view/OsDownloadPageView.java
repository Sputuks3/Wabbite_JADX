package com.Revsoft.Wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import com.Revsoft.Wabbitemu.utils.ViewUtils;

public class OsDownloadPageView extends RelativeLayout {
    private final ProgressBar mLoadingSpinner = ((ProgressBar) ViewUtils.findViewById((View) this, (int) R.id.loadingSpinner, ProgressBar.class));
    private final WebView mWebView = ((WebView) ViewUtils.findViewById((View) this, (int) R.id.webDownloadView, WebView.class));

    public OsDownloadPageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LayoutInflater.from(context).inflate(R.layout.os_download_page, this, true);
        AdUtils.loadAd(findViewById(R.id.adView));
    }

    public WebView getWebView() {
        return this.mWebView;
    }

    public void showProgressBar(boolean shouldShow) {
        this.mLoadingSpinner.setVisibility(shouldShow ? 0 : 8);
    }
}
