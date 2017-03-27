package com.Revsoft.Wabbitemu.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.SkinBitmapLoader;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.calc.CalculatorManager;
import com.Revsoft.Wabbitemu.calc.FileLoadedCallback;
import com.Revsoft.Wabbitemu.fragment.EmulatorFragment;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionActivity;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionEvent;
import com.Revsoft.Wabbitemu.utils.ErrorUtils;
import com.Revsoft.Wabbitemu.utils.IntentConstants;
import com.Revsoft.Wabbitemu.utils.PreferenceConstants;
import com.Revsoft.Wabbitemu.utils.StorageUtils;
import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import com.Revsoft.Wabbitemu.utils.ViewUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WabbitemuActivity extends Activity {
    private static final String DEFAULT_FILE_REGEX = "\\.(rom|sav|[7|8][2|3|x|c|5|6][b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$";
    private static final int LOAD_FILE_CODE = 1;
    private static final int SETUP_WIZARD = 2;
    public static String sBestCacheDir;
    private final CalculatorManager mCalcManager = CalculatorManager.getInstance();
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private EmulatorFragment mEmulatorFragment;
    private SharedPreferences mSharedPrefs;
    private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
    private final UserActivityTracker mUserActivityTracker = UserActivityTracker.getInstance();
    private final VisibilityChangeListener mVisibilityListener = new VisibilityChangeListener();
    private boolean mWasUserLaunched;

    private class FinishActivityClickListener implements OnClickListener {
        private FinishActivityClickListener() {
        }

        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            WabbitemuActivity.this.finish();
        }
    }

    private enum MainMenuItem {
        LOAD_FILE_MENU_ITEM(0),
        WIZARD_MENU_ITEM(1),
        RESET_MENU_ITEM(2),
        SCREENSHOT_MENU_ITEM(3),
        SETTINGS_MENU_ITEM(4),
        ABOUT_MENU_ITEM(5);
        
        private final int mPosition;

        private MainMenuItem(int position) {
            this.mPosition = position;
        }

        public static MainMenuItem fromPosition(int position) {
            for (MainMenuItem item : values()) {
                if (item.mPosition == position) {
                    return item;
                }
            }
            return null;
        }
    }

    private class VisibilityChangeListener implements OnSystemUiVisibilityChangeListener {
        private VisibilityChangeListener() {
        }

        public void onSystemUiVisibilityChange(int visibility) {
            WabbitemuActivity.this.setImmersiveMode(true);
        }
    }

    private void handleFile(File f, Runnable runnable) {
        this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.SEND_FILE, f.getAbsolutePath());
        this.mEmulatorFragment.handleFile(f, runnable);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HttpURLConnection.setFollowRedirects(true);
        this.mUserActivityTracker.initializeIfNecessary(this);
        this.mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        workaroundAsyncTaskIssue();
        if (testNativeLibraryLoad()) {
            AdUtils.initialize(getApplication());
            sBestCacheDir = findBestCacheDir();
            this.mCalcManager.initialize(this, sBestCacheDir);
            this.mSkinLoader.initialize(this);
            this.mUserActivityTracker.reportActivityStart(this);
            String fileName = getLastRomSetting();
            final Runnable runnable = getLaunchRunnable();
            if (fileName != null) {
                this.mCalcManager.loadRomFile(new File(fileName), new FileLoadedCallback() {
                    public void onFileLoaded(int errorCode) {
                        if (errorCode != 0) {
                            runnable.run();
                        }
                    }
                });
            }
            setContentView(R.layout.main);
            this.mEmulatorFragment = (EmulatorFragment) getFragmentManager().findFragmentById(R.id.content_frame);
            attachMenu();
            if (isFirstRun()) {
                this.mWasUserLaunched = false;
                startActivityForResult(new Intent(this, WizardActivity.class), 2);
                return;
            }
            CalcModel lastRomModel = CalcModel.fromModel(getLastRomModel());
            if (lastRomModel != CalcModel.NO_CALC) {
                this.mSkinLoader.loadSkinAndKeymap(lastRomModel);
                return;
            } else if (fileName == null || fileName.equals("")) {
                runnable.run();
                return;
            } else {
                return;
            }
        }
        ErrorUtils.showErrorDialog(this, R.string.error_failed_load_native_lib, new FinishActivityClickListener());
    }

    private String findBestCacheDir() {
        File cacheDir = getApplicationContext().getCacheDir();
        if (cacheDir != null) {
            return cacheDir.getAbsolutePath();
        }
        if (VERSION.SDK_INT >= 19) {
            for (File file : getApplicationContext().getExternalCacheDirs()) {
                if (file != null) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private void workaroundAsyncTaskIssue() {
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable th) {
        }
    }

    private boolean testNativeLibraryLoad() {
        try {
            this.mCalcManager.testLoadLib();
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public void onResume() {
        super.onResume();
        if (this.mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this.mVisibilityListener);
            setImmersiveMode(true);
        }
    }

    protected void onPause() {
        super.onPause();
        if (this.mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
            setImmersiveMode(false);
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
        }
    }

    public void onStop() {
        super.onStop();
        this.mUserActivityTracker.reportActivityStop(this);
    }

    private void attachMenu() {
        this.mDrawerLayout = (DrawerLayout) ViewUtils.findViewById((Activity) this, (int) R.id.drawer_layout, DrawerLayout.class);
        this.mDrawerList = (ListView) ViewUtils.findViewById((Activity) this, (int) R.id.left_drawer, ListView.class);
        this.mDrawerList.setAdapter(new ArrayAdapter(this, 17367043, getResources().getStringArray(R.array.menu_array)));
        this.mDrawerLayout.setScrimColor(Color.parseColor("#DD000000"));
        this.mDrawerList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                WabbitemuActivity.this.handleMenuItem(MainMenuItem.fromPosition(position));
            }
        });
        this.mDrawerLayout.setDrawerListener(new DrawerListener() {
            public void onDrawerStateChanged(int arg0) {
            }

            public void onDrawerSlide(View drawerView, float slideOffset) {
                WabbitemuActivity.this.mDrawerLayout.bringChildToFront(drawerView);
                WabbitemuActivity.this.mDrawerLayout.requestLayout();
            }

            public void onDrawerOpened(View arg0) {
                WabbitemuActivity.this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.OPEN_MENU);
            }

            public void onDrawerClosed(View arg0) {
            }
        });
    }

    private Runnable getLaunchRunnable() {
        return new Runnable() {
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        ErrorUtils.showErrorDialog(WabbitemuActivity.this, R.string.errorLink);
                    }
                });
                WabbitemuActivity.this.startActivityForResult(new Intent(WabbitemuActivity.this, WizardActivity.class), 2);
            }
        };
    }

    private String getLastRomSetting() {
        return this.mSharedPrefs.getString(PreferenceConstants.ROM_PATH.toString(), null);
    }

    private int getLastRomModel() {
        return this.mSharedPrefs.getInt(PreferenceConstants.ROM_MODEL.toString(), -1);
    }

    private boolean isFirstRun() {
        return this.mSharedPrefs.getBoolean(PreferenceConstants.FIRST_RUN.toString(), true);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == -1) {
                    final String fileName = data.getStringExtra(IntentConstants.FILENAME_EXTRA_STRING);
                    handleFile(new File(fileName), new Runnable() {
                        public void run() {
                            ErrorUtils.showErrorDialog(WabbitemuActivity.this, R.string.errorLink);
                            WabbitemuActivity.this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.SEND_FILE_ERROR, fileName);
                        }
                    });
                    return;
                }
                return;
            case 2:
                if (resultCode == -1) {
                    handleFile(new File(data.getStringExtra(IntentConstants.FILENAME_EXTRA_STRING)), getLaunchRunnable());
                    if (isFirstRun()) {
                        Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                        editor.putBoolean(PreferenceConstants.FIRST_RUN.toString(), false);
                        editor.apply();
                        this.mDrawerLayout.openDrawer(this.mDrawerList);
                        return;
                    }
                    return;
                } else if (!this.mWasUserLaunched) {
                    finish();
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mDrawerLayout.isDrawerOpen(this.mDrawerList)) {
            this.mDrawerLayout.closeDrawer(this.mDrawerList);
        } else {
            this.mDrawerLayout.openDrawer(this.mDrawerList);
        }
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        MainMenuItem position;
        switch (item.getItemId()) {
            case R.id.loadFileMenuItem:
                position = MainMenuItem.LOAD_FILE_MENU_ITEM;
                break;
            case R.id.rerunWizardMenuItem:
                position = MainMenuItem.WIZARD_MENU_ITEM;
                break;
            case R.id.resetMenuItem:
                position = MainMenuItem.RESET_MENU_ITEM;
                break;
            case R.id.settingsMenuItem:
                position = MainMenuItem.SETTINGS_MENU_ITEM;
                break;
            case R.id.aboutMenuItem:
                position = MainMenuItem.ABOUT_MENU_ITEM;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return handleMenuItem(position);
    }

    private boolean handleMenuItem(MainMenuItem position) {
        this.mDrawerLayout.closeDrawer(this.mDrawerList);
        this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.MENU_ITEM_SELECTED, position.toString());
        switch (position) {
            case SETTINGS_MENU_ITEM:
                launchSettings();
                break;
            case RESET_MENU_ITEM:
                resetCalc();
                break;
            case SCREENSHOT_MENU_ITEM:
                screenshotCalc();
                break;
            case WIZARD_MENU_ITEM:
                launchWizard();
                break;
            case LOAD_FILE_MENU_ITEM:
                launchBrowse();
                break;
            case ABOUT_MENU_ITEM:
                launchAbout();
                break;
            default:
                throw new IllegalStateException("Invalid menu item");
        }
        return true;
    }

    private void screenshotCalc() {
        this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.SCREENSHOT);
        Bitmap screenshot = this.mEmulatorFragment.getScreenshot();
        if (screenshot == null) {
            ErrorUtils.showErrorDialog(this, R.string.errorScreenshot);
            return;
        }
        Bitmap scaledScreenshot = Bitmap.createScaledBitmap(screenshot, screenshot.getWidth() * 2, screenshot.getHeight() * 2, true);
        if (StorageUtils.hasExternalStorage()) {
            File outputDir = new File(new File(StorageUtils.getPrimaryStoragePath(), "Wabbitemu"), "Screenshots");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            try {
                FileOutputStream out = new FileOutputStream(new File(outputDir, "screenshot" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png"));
                scaledScreenshot.compress(CompressFormat.PNG, 100, out);
                out.close();
                Toast.makeText(this, String.format(getResources().getString(R.string.screenshotSuccess), new Object[]{outputFile}), 1).show();
                return;
            } catch (Exception e) {
                ErrorUtils.showErrorDialog(this, R.string.errorScreenshot);
                return;
            }
        }
        ErrorUtils.showErrorDialog(this, R.string.errorMissingSdCard);
    }

    private void launchAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void launchBrowse() {
        String extensions;
        Intent setupIntent = new Intent(this, BrowseActivity.class);
        switch (this.mCalcManager.getModel()) {
            case TI_73:
                extensions = "\\.(rom|sav|73[b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            case TI_82:
                extensions = "\\.(rom|sav|82[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            case TI_83:
                extensions = "\\.(rom|sav|83[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            case TI_83P:
            case TI_83PSE:
            case TI_84P:
            case TI_84PSE:
                extensions = "\\.(rom|sav|8x[b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            case TI_84PCSE:
                extensions = "\\.(rom|sav|8[x|c][b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            case TI_85:
                extensions = "\\.(rom|sav|85[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            case TI_86:
                extensions = "\\.(rom|sav|86[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$";
                break;
            default:
                extensions = DEFAULT_FILE_REGEX;
                break;
        }
        String description = getResources().getString(R.string.browseFileDescription);
        setupIntent.putExtra(IntentConstants.EXTENSION_EXTRA_REGEX, extensions);
        setupIntent.putExtra(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, description);
        startActivityForResult(setupIntent, 1);
    }

    private void launchWizard() {
        this.mWasUserLaunched = true;
        startActivityForResult(new Intent(this, WizardActivity.class), 2);
    }

    private void resetCalc() {
        this.mEmulatorFragment.resetCalc();
    }

    private void launchSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @TargetApi(19)
    public void setImmersiveMode(boolean isImmersive) {
        if (VERSION.SDK_INT >= 19) {
            int newUiOptions;
            View decorView = getWindow().getDecorView();
            int uiOptions = decorView.getSystemUiVisibility();
            if (isImmersive) {
                newUiOptions = ((uiOptions | 2) | 4) | 4096;
            } else {
                newUiOptions = ((uiOptions & -3) & -5) & -4097;
            }
            decorView.setSystemUiVisibility(newUiOptions);
        }
    }
}
