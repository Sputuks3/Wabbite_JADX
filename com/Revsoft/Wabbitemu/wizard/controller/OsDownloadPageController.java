package com.Revsoft.Wabbitemu.wizard.controller;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.data.FinishWizardData;
import com.Revsoft.Wabbitemu.wizard.data.OSDownloadData;
import com.Revsoft.Wabbitemu.wizard.view.OsDownloadPageView;

public class OsDownloadPageController implements WizardPageController {
    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.97+Safari/537.36";
    private CalcModel mCalcModel;
    private String mDownloadCode;
    private WizardNavigationController mNavController;
    private String mOsUrl;
    private final OsDownloadPageView mView;
    private final WebView mWebView = this.mView.getWebView();

    private class JavaScriptInterface {
        private JavaScriptInterface() {
        }

        @JavascriptInterface
        public void onFoundCode(final String message) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    if (OsDownloadPageController.this.mDownloadCode == null) {
                        OsDownloadPageController.this.mDownloadCode = message.replace("\"", "");
                        OsDownloadPageController.this.mNavController.finishWizard();
                    }
                }
            });
        }
    }

    private class OsDownloadWebViewClient extends WebViewClient {
        private OsDownloadWebViewClient() {
        }

        public void onPageFinished(WebView view, String url) {
            String newStyles = "#layout-default { \twidth: 100%; }  #header-site { \tdisplay: none; }  .sublayout-etdownloadbundle { \tmin-height: auto; }   .sublayout-etdownloadbundledetails { \twidth: auto; }  .sublayout-etdownloadsactivitiesheader { \tdisplay: none; }  .etdownloadbundleheader { \tpadding-top: 10px; \twidth: auto; }  .etheroimage { \tdisplay: none; }  .column-pdf, .column-size, .column-version, .column-appsspaces { \tdisplay: none; }  .etguidebooks, .etrelatedsoftware { \tdisplay: none; }  ui-dialog-buttonset > button { \tmargin: 20px; \tdisplay: block; }  eula-captcha > div { \twidth: 100% !important; }  .etmaincontent {  display: none; }  .back-to-results {  display: none; }  .feature-summary {  display: none; }  #footer-site {  display: none; }  .ui-dialog-buttonset {  display: block !important;  margin-left: auto;  margin-right: auto;  width: 300px; }  .dialog-key-eula.ui-dialog .ui-dialog-buttonpane .ui-dialog-buttonset .ui-button {  margin: 20px; }  .dialog-eula {  max-height: 200px !important; } .column-downloaditem.protected-download { opacity: 1 } .column-downloaditem { opacity: 0 }";
            view.loadUrl("javascript:$(document).ajaxComplete(function(e, xhr, settings) { if (xhr.status == 200)Android.onFoundCode(xhr.responseText);else alert('Error getting download code'); });Dialogs.Init('Eula', function(dialogData) { dialogData.params.width = 400 } ); Dialogs['Eula'].Dialog.dialog(Dialogs['Eula'].params); $(\"<style type='text/css'>#layout-default { \twidth: 100%; }  #header-site { \tdisplay: none; }  .sublayout-etdownloadbundle { \tmin-height: auto; }   .sublayout-etdownloadbundledetails { \twidth: auto; }  .sublayout-etdownloadsactivitiesheader { \tdisplay: none; }  .etdownloadbundleheader { \tpadding-top: 10px; \twidth: auto; }  .etheroimage { \tdisplay: none; }  .column-pdf, .column-size, .column-version, .column-appsspaces { \tdisplay: none; }  .etguidebooks, .etrelatedsoftware { \tdisplay: none; }  ui-dialog-buttonset > button { \tmargin: 20px; \tdisplay: block; }  eula-captcha > div { \twidth: 100% !important; }  .etmaincontent {  display: none; }  .back-to-results {  display: none; }  .feature-summary {  display: none; }  #footer-site {  display: none; }  .ui-dialog-buttonset {  display: block !important;  margin-left: auto;  margin-right: auto;  width: 300px; }  .dialog-key-eula.ui-dialog .ui-dialog-buttonpane .ui-dialog-buttonset .ui-button {  margin: 20px; }  .dialog-eula {  max-height: 200px !important; } .column-downloaditem.protected-download { opacity: 1 } .column-downloaditem { opacity: 0 }</style>\").appendTo('head')");
            view.setVisibility(0);
            OsDownloadPageController.this.mView.showProgressBar(false);
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            OsDownloadPageController.this.mView.showProgressBar(false);
            AlertDialog dialog = new Builder(view.getContext()).setMessage(R.string.errorWebPageDownloadError).setTitle(R.string.errorTitle).create();
            dialog.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    OsDownloadPageController.this.mNavController.movePreviousPage();
                }
            });
            dialog.show();
        }
    }

    public OsDownloadPageController(@NonNull OsDownloadPageView osDownloadPageView) {
        this.mView = osDownloadPageView;
        this.mWebView.getSettings().setJavaScriptEnabled(true);
        this.mWebView.getSettings().setDomStorageEnabled(true);
        this.mWebView.getSettings().setUserAgentString(USER_AGENT);
        this.mWebView.setWebViewClient(new OsDownloadWebViewClient());
        this.mWebView.addJavascriptInterface(new JavaScriptInterface(), "Android");
    }

    public void configureButtons(@NonNull WizardNavigationController navController) {
        this.mNavController = navController;
        navController.hideNextButton();
    }

    public boolean hasPreviousPage() {
        return true;
    }

    public boolean hasNextPage() {
        return false;
    }

    public boolean isFinalPage() {
        return true;
    }

    public int getNextPage() {
        throw new UnsupportedOperationException("No next page");
    }

    public int getPreviousPage() {
        return R.id.model_page;
    }

    public void onHiding() {
        this.mWebView.setVisibility(8);
        this.mDownloadCode = null;
    }

    public void onShowing(Object previousData) {
        OSDownloadData osDownloadData = (OSDownloadData) previousData;
        this.mCalcModel = osDownloadData.mCalcModel;
        this.mOsUrl = osDownloadData.mOsUrl;
        this.mWebView.setVisibility(8);
        this.mDownloadCode = null;
        this.mView.showProgressBar(true);
        switch (this.mCalcModel) {
            case TI_73:
                this.mWebView.loadUrl("https://education.ti.com/en/us/software/details/en/956CE30854A74767893104FCDF195B76/73ti73exploreroperatingsystem");
                return;
            case TI_83P:
            case TI_83PSE:
                this.mWebView.loadUrl("https://education.ti.com/en/us/software/details/en/C95956E744FB4C0A899F5A63EBEA60DD/83ti83plusoperatingsystemsoftware");
                return;
            case TI_84P:
            case TI_84PSE:
                this.mWebView.loadUrl("https://education.ti.com/en/us/software/details/en/B7DADA7FD4AA40CE9D7911B004B8C460/ti84plusoperatingsystem");
                return;
            case TI_84PCSE:
                this.mWebView.loadUrl("https://education.ti.com/en/asia/software/details/en/812E5FCF48C6456CB156A03DE5D07016/singaporeapprovedosapps");
                return;
            default:
                throw new IllegalStateException("Invalid calculator type");
        }
    }

    public int getTitleId() {
        return R.string.osDownloadTitle;
    }

    public Object getControllerData() {
        return new FinishWizardData(this.mCalcModel, this.mOsUrl, this.mDownloadCode);
    }
}
