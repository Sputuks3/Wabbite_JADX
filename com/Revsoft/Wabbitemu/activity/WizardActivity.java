package com.Revsoft.Wabbitemu.activity;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.ViewAnimator;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.calc.CalculatorManager;
import com.Revsoft.Wabbitemu.calc.FileLoadedCallback;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionActivity;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionEvent;
import com.Revsoft.Wabbitemu.utils.ErrorUtils;
import com.Revsoft.Wabbitemu.utils.IntentConstants;
import com.Revsoft.Wabbitemu.utils.OSDownloader;
import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import com.Revsoft.Wabbitemu.utils.ViewUtils;
import com.Revsoft.Wabbitemu.wizard.OnWizardFinishedListener;
import com.Revsoft.Wabbitemu.wizard.WizardController;
import com.Revsoft.Wabbitemu.wizard.controller.BrowseOsPageController;
import com.Revsoft.Wabbitemu.wizard.controller.BrowseRomPageController;
import com.Revsoft.Wabbitemu.wizard.controller.CalcModelPageController;
import com.Revsoft.Wabbitemu.wizard.controller.ChooseOsPageController;
import com.Revsoft.Wabbitemu.wizard.controller.LandingPageController;
import com.Revsoft.Wabbitemu.wizard.controller.OsDownloadPageController;
import com.Revsoft.Wabbitemu.wizard.controller.OsPageController;
import com.Revsoft.Wabbitemu.wizard.data.FinishWizardData;
import com.Revsoft.Wabbitemu.wizard.view.BrowseOsPageView;
import com.Revsoft.Wabbitemu.wizard.view.BrowseRomPageView;
import com.Revsoft.Wabbitemu.wizard.view.ChooseOsPageView;
import com.Revsoft.Wabbitemu.wizard.view.LandingPageView;
import com.Revsoft.Wabbitemu.wizard.view.ModelPageView;
import com.Revsoft.Wabbitemu.wizard.view.OsDownloadPageView;
import com.Revsoft.Wabbitemu.wizard.view.OsPageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WizardActivity extends Activity {
    private final CalculatorManager mCalcManager = CalculatorManager.getInstance();
    private String mCreatedFilePath;
    private boolean mIsWizardFinishing;
    private OSDownloader mOsDownloader;
    private final UserActivityTracker mUserActivityTracker = UserActivityTracker.getInstance();
    private WizardController mWizardController;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUserActivityTracker.initializeIfNecessary(getApplicationContext());
        this.mUserActivityTracker.reportActivityStart(this);
        setContentView(R.layout.wizard);
        this.mWizardController = new WizardController(this, (ViewAnimator) ViewUtils.findViewById((Activity) this, (int) R.id.viewFlipper, ViewAnimator.class), (ViewGroup) ViewUtils.findViewById((Activity) this, (int) R.id.navContainer, ViewGroup.class), new OnWizardFinishedListener() {
            public void onWizardFinishedListener(Object finalData) {
                if (!WizardActivity.this.mIsWizardFinishing) {
                    WizardActivity.this.mIsWizardFinishing = true;
                    FinishWizardData finishInfo = (FinishWizardData) finalData;
                    if (finishInfo == null) {
                        ErrorUtils.showErrorDialog(WizardActivity.this, R.string.errorRomImage);
                        return;
                    }
                    CalcModel calcModel = finishInfo.getCalcModel();
                    WizardActivity.this.mUserActivityTracker.reportBreadCrumb("User finished wizard. Model: %s", calcModel);
                    if (finishInfo.shouldDownloadOs()) {
                        WizardActivity.this.mUserActivityTracker.reportUserAction(UserActionActivity.WIZARD_ACTIVITY, UserActionEvent.BOOTFREE_ROM);
                        WizardActivity.this.tryDownloadAndCreateRom(calcModel, finishInfo.getDownloadCode(), finishInfo.getOsDownloadUrl());
                    } else if (calcModel == CalcModel.NO_CALC) {
                        WizardActivity.this.mUserActivityTracker.reportUserAction(UserActionActivity.WIZARD_ACTIVITY, UserActionEvent.HAVE_OWN_ROM);
                        WizardActivity.this.finishSuccess(finishInfo.getFilePath());
                    } else {
                        WizardActivity.this.mUserActivityTracker.reportUserAction(UserActionActivity.WIZARD_ACTIVITY, UserActionEvent.BOOTFREE_ROM);
                        WizardActivity.this.createRomCopyOs(calcModel, finishInfo.getFilePath());
                    }
                }
            }
        });
        this.mWizardController.registerView(R.id.landing_page, new LandingPageController((LandingPageView) ViewUtils.findViewById((Activity) this, (int) R.id.landing_page, LandingPageView.class)));
        this.mWizardController.registerView(R.id.model_page, new CalcModelPageController((ModelPageView) ViewUtils.findViewById((Activity) this, (int) R.id.model_page, ModelPageView.class)));
        this.mWizardController.registerView(R.id.choose_os_page, new ChooseOsPageController((ChooseOsPageView) ViewUtils.findViewById((Activity) this, (int) R.id.choose_os_page, ChooseOsPageView.class)));
        this.mWizardController.registerView(R.id.os_page, new OsPageController((OsPageView) ViewUtils.findViewById((Activity) this, (int) R.id.os_page, OsPageView.class)));
        this.mWizardController.registerView(R.id.os_download_page, new OsDownloadPageController((OsDownloadPageView) ViewUtils.findViewById((Activity) this, (int) R.id.os_download_page, OsDownloadPageView.class)));
        this.mWizardController.registerView(R.id.browse_os_page, new BrowseOsPageController((BrowseOsPageView) ViewUtils.findViewById((Activity) this, (int) R.id.browse_os_page, BrowseOsPageView.class), getFragmentManager()));
        this.mWizardController.registerView(R.id.browse_rom_page, new BrowseRomPageController((BrowseRomPageView) ViewUtils.findViewById((Activity) this, (int) R.id.browse_rom_page, BrowseRomPageView.class), getFragmentManager()));
    }

    protected void onPause() {
        super.onPause();
        cancelDownloadTask();
    }

    public void onStop() {
        super.onStop();
        this.mUserActivityTracker.reportActivityStop(this);
    }

    protected void onDestroy() {
        super.onDestroy();
        cancelDownloadTask();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.wizard, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.helpMenuItem:
                this.mUserActivityTracker.reportUserAction(UserActionActivity.WIZARD_ACTIVITY, UserActionEvent.HELP);
                new Builder(this).setMessage(R.string.aboutRomDescription).setTitle(R.string.aboutRomTitle).setPositiveButton(17039370, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed() {
        if (!this.mWizardController.movePreviousPage()) {
            super.onBackPressed();
        }
    }

    private void tryDownloadAndCreateRom(CalcModel calcModel, String downloadCode, String osDownloadUrl) {
        if (this.mOsDownloader != null && this.mOsDownloader.getStatus() == Status.RUNNING) {
            throw new IllegalStateException("Invalid state, download running");
        } else if (isOnline()) {
            createRomDownloadOs(calcModel, downloadCode, osDownloadUrl);
        } else {
            new Builder(this).setMessage(getResources().getString(R.string.noNetwork)).setPositiveButton(17039370, new OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    WizardActivity.this.mIsWizardFinishing = false;
                    dialog.dismiss();
                }
            }).create().show();
        }
    }

    private boolean isOnline() {
        NetworkInfo netInfo = ((ConnectivityManager) getSystemService("connectivity")).getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void createRomCopyOs(final CalcModel calcModel, final String osFilePath) {
        String bootPagePath = extractBootpage(calcModel);
        if (bootPagePath == null) {
            finishRomError();
            return;
        }
        this.mCalcManager.createRom(osFilePath, bootPagePath, this.mCreatedFilePath, calcModel, new FileLoadedCallback() {
            public void onFileLoaded(int error) {
                WizardActivity.this.mUserActivityTracker.reportBreadCrumb("Creating ROM given OS: %s model: %s error: %s", osFilePath, calcModel, Integer.valueOf(error));
                if (error == 0) {
                    WizardActivity.this.finishSuccess(WizardActivity.this.mCreatedFilePath);
                } else {
                    WizardActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            WizardActivity.this.finishRomError();
                        }
                    });
                }
            }
        });
    }

    private String extractBootpage(CalcModel calcModel) {
        IOException e;
        Throwable th;
        Resources resources = getResources();
        File cache = getCacheDir();
        this.mCreatedFilePath = cache.getAbsolutePath() + "/";
        try {
            InputStream bootStream;
            File bootPagePath = File.createTempFile("boot", ".hex", cache);
            switch (calcModel) {
                case TI_73:
                    this.mCreatedFilePath += resources.getString(R.string.ti73);
                    bootStream = resources.openRawResource(R.raw.bf73);
                    break;
                case TI_83PSE:
                    this.mCreatedFilePath += resources.getString(R.string.ti83pse);
                    bootStream = resources.openRawResource(R.raw.bf83pse);
                    break;
                case TI_84P:
                    this.mCreatedFilePath += resources.getString(R.string.ti84p);
                    bootStream = resources.openRawResource(R.raw.bf84pbe);
                    break;
                case TI_84PSE:
                    this.mCreatedFilePath += resources.getString(R.string.ti84pse);
                    bootStream = resources.openRawResource(R.raw.bf84pse);
                    break;
                case TI_84PCSE:
                    this.mCreatedFilePath += resources.getString(R.string.ti84pcse);
                    bootStream = resources.openRawResource(R.raw.bf84pcse);
                    break;
                default:
                    this.mCreatedFilePath += resources.getString(R.string.ti83p);
                    bootStream = resources.openRawResource(R.raw.bf83pbe);
                    break;
            }
            this.mCreatedFilePath += ".rom";
            FileOutputStream fileOutputStream = null;
            try {
                byte[] buffer = new byte[4096];
                FileOutputStream outputStream = new FileOutputStream(bootPagePath);
                while (bootStream.read(buffer) != -1) {
                    try {
                        outputStream.write(buffer, 0, 4096);
                    } catch (IOException e2) {
                        e = e2;
                        fileOutputStream = outputStream;
                    } catch (Throwable th2) {
                        th = th2;
                        fileOutputStream = outputStream;
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e3) {
                        finishRomError();
                        fileOutputStream = outputStream;
                    }
                }
                fileOutputStream = outputStream;
            } catch (IOException e4) {
                e = e4;
                try {
                    this.mUserActivityTracker.reportBreadCrumb("ERROR writing bootpage %s", e);
                    finishRomError();
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e5) {
                            finishRomError();
                        }
                    }
                    return bootPagePath.getAbsolutePath();
                } catch (Throwable th3) {
                    th = th3;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e6) {
                            finishRomError();
                        }
                    }
                    throw th;
                }
            }
            return bootPagePath.getAbsolutePath();
        } catch (IOException e7) {
            this.mUserActivityTracker.reportBreadCrumb("ERROR extracting bootpage %s", e7);
            return null;
        }
    }

    private void createRomDownloadOs(CalcModel calcModel, String downloadCode, String osDownloadUrl) {
        final String bootPagePath = extractBootpage(calcModel);
        if (bootPagePath == null) {
            finishRomError();
            return;
        }
        int osVersion = ((Spinner) findViewById(R.id.osVersionSpinner)).getSelectedItemPosition();
        try {
            String osFilePath = File.createTempFile("tios", ".8xu", getCacheDir()).getAbsolutePath();
            final String str = osFilePath;
            final CalcModel calcModel2 = calcModel;
            this.mOsDownloader = new OSDownloader(this, osDownloadUrl, osFilePath, calcModel, downloadCode) {
                protected void onPostExecute(Boolean success) {
                    super.onPostExecute(success);
                    WizardActivity.this.createRom(success, str, bootPagePath, calcModel2);
                }

                protected void onCancelled() {
                    super.onCancelled();
                    WizardActivity.this.mIsWizardFinishing = false;
                }
            };
            this.mOsDownloader.execute(new Integer[0]);
        } catch (IOException e) {
            this.mUserActivityTracker.reportBreadCrumb("ERROR creating OS temp file: %s", e);
        }
    }

    private void createRom(Boolean success, String osFilePath, String bootPagePath, final CalcModel calcModel) {
        if (success.booleanValue()) {
            CalculatorManager.getInstance().createRom(osFilePath, bootPagePath, this.mCreatedFilePath, calcModel, new FileLoadedCallback() {
                public void onFileLoaded(int error) {
                    WizardActivity.this.mUserActivityTracker.reportBreadCrumb("Creating ROM type: %s error: %s", calcModel, Integer.valueOf(error));
                    if (error == 0) {
                        WizardActivity.this.finishSuccess(WizardActivity.this.mCreatedFilePath);
                    } else {
                        WizardActivity.this.finishRomError();
                    }
                }
            });
            return;
        }
        finishOsError();
    }

    private void finishOsError() {
        showOsError();
    }

    private void finishRomError() {
        showRomError();
    }

    private void finishSuccess(String fileName) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(IntentConstants.FILENAME_EXTRA_STRING, fileName);
        setResult(-1, resultIntent);
        finish();
    }

    private void showOsError() {
        this.mIsWizardFinishing = false;
        ErrorUtils.showErrorDialog(this, R.string.errorOsDownloadDescription);
    }

    private void showRomError() {
        this.mIsWizardFinishing = false;
        ErrorUtils.showErrorDialog(this, R.string.errorRomCreateDescription);
    }

    private void cancelDownloadTask() {
        if (this.mOsDownloader != null) {
            this.mOsDownloader.cancel(true);
            this.mOsDownloader = null;
        }
    }
}
