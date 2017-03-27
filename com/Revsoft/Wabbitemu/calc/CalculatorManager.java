package com.Revsoft.Wabbitemu.calc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.Revsoft.Wabbitemu.CalcSkin;
import com.Revsoft.Wabbitemu.SkinBitmapLoader;
import com.Revsoft.Wabbitemu.utils.PreferenceConstants;
import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import java.io.File;
import java.lang.reflect.Array;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

public class CalculatorManager {
    private static final long MAX_TSTATE_KEY = 600000000;
    private static final long MAX_TSTATE_ON_KEY = 600000000000000L;
    private static final long MIN_TSTATE_KEY = 600;
    private static final long MIN_TSTATE_ON_KEY = 25000;
    private static final String PAUSE_KEY = "pauseKey";
    private CalcSkin mCalcSkin;
    private final CalcThread mCalcThread;
    private Context mContext;
    @Nonnull
    private CalcModel mCurrentModel;
    private String mCurrentRomFile;
    private final AtomicBoolean mHasLoadedRom;
    private boolean mIsInitialized;
    private final long[][] mKeyTimePressed;
    private final ScheduledExecutorService mRepressExecutor;
    private SharedPreferences mSharedPrefs;
    private final SkinBitmapLoader mSkinLoader;
    private final UserActivityTracker mUserTracker;

    private static class InitializeRunnable implements Runnable {
        private final String mBestCacheDir;

        public InitializeRunnable(String bestCacheDir) {
            this.mBestCacheDir = bestCacheDir;
        }

        public void run() {
            CalcInterface.Initialize(this.mBestCacheDir);
        }
    }

    private static class LoadFileRunnable implements Runnable {
        private final FileLoadedCallback callback;
        private final File file;

        private LoadFileRunnable(File file, FileLoadedCallback callback) {
            this.file = file;
            this.callback = callback;
        }

        public void run() {
            UserActivityTracker.getInstance().reportBreadCrumb("Loading file error %s", Integer.valueOf(CalcInterface.LoadFile(this.file.getPath())));
            this.callback.onFileLoaded(linkResult);
        }
    }

    private class LoadRomRunnable implements Runnable {
        private final FileLoadedCallback mCallback;

        public LoadRomRunnable(FileLoadedCallback callback) {
            this.mCallback = callback;
        }

        public void run() {
            String reportingString;
            boolean wasSuccess = true;
            CalcInterface.SetAutoTurnOn(CalculatorManager.this.mSharedPrefs.getBoolean(PreferenceConstants.AUTO_TURN_ON.toString(), true));
            int errorCode = CalcInterface.LoadFile(CalculatorManager.this.mCurrentRomFile);
            if (errorCode != 0) {
                wasSuccess = false;
            }
            CalculatorManager.this.mCurrentModel = CalcModel.fromModel(CalcInterface.GetModel());
            if (wasSuccess) {
                reportingString = "Loaded rom " + CalculatorManager.this.mCurrentModel;
            } else {
                reportingString = "Failed to load ROM at " + CalculatorManager.this.mCurrentRomFile;
            }
            CalculatorManager.this.mUserTracker.reportBreadCrumb(reportingString);
            if (wasSuccess) {
                CalculatorManager.this.unPauseCalc(CalculatorManager.PAUSE_KEY);
            }
            CalculatorManager.this.handleRomLoaded(this.mCallback, errorCode);
        }
    }

    private class SaveCurrentRomRunnable implements Runnable {
        private SaveCurrentRomRunnable() {
        }

        public void run() {
            CalculatorManager.this.mCurrentRomFile = CalculatorManager.this.mContext.getFilesDir().getAbsoluteFile() + "/Wabbitemu.sav";
            if (CalcInterface.SaveCalcState(CalculatorManager.this.mCurrentRomFile)) {
                CalculatorManager.this.updateCurrentRomSetting();
            }
            CalculatorManager.this.mHasLoadedRom.set(false);
            Log.e("CalculatorManager", "Finished writing ROM");
        }
    }

    private static class SingletonHolder {
        private static final CalculatorManager SINGLETON = new CalculatorManager();

        private SingletonHolder() {
        }
    }

    public static CalculatorManager getInstance() {
        return SingletonHolder.SINGLETON;
    }

    private CalculatorManager() {
        this.mUserTracker = UserActivityTracker.getInstance();
        this.mSkinLoader = SkinBitmapLoader.getInstance();
        this.mHasLoadedRom = new AtomicBoolean();
        this.mCalcThread = new CalcThread();
        this.mKeyTimePressed = (long[][]) Array.newInstance(Long.TYPE, new int[]{8, 8});
        this.mRepressExecutor = new ScheduledThreadPoolExecutor(1);
        this.mCurrentModel = CalcModel.NO_CALC;
        pauseCalc(PAUSE_KEY);
        this.mCalcThread.start();
    }

    public void initialize(Context context, String cacheDir) {
        if (!this.mIsInitialized) {
            this.mIsInitialized = true;
            this.mContext = context;
            this.mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            this.mCalcThread.queueRunnable(new InitializeRunnable(cacheDir));
        }
    }

    public void loadRomFile(File file, FileLoadedCallback callback) {
        if (this.mHasLoadedRom.get() && this.mCurrentRomFile.equals(file.getPath())) {
            handleRomLoaded(callback, 0);
            return;
        }
        this.mHasLoadedRom.set(false);
        this.mCurrentRomFile = file.getPath();
        this.mUserTracker.reportBreadCrumb("Loading rom " + file.getAbsolutePath());
        this.mCalcThread.queueRunnable(new LoadRomRunnable(callback));
    }

    public void loadFile(File file, FileLoadedCallback callback) {
        this.mCalcThread.queueRunnable(new LoadFileRunnable(file, callback));
    }

    public void setScreenCallback(CalcScreenUpdateCallback callback) {
        this.mCalcThread.setScreenUpdateCallback(callback);
    }

    public void setCalcSkin(CalcSkin calcSkin) {
        this.mCalcSkin = calcSkin;
    }

    public void pauseCalc(String pauseKey) {
        this.mCalcThread.setPaused(pauseKey, true);
    }

    public void unPauseCalc(String pauseKey) {
        this.mCalcThread.setPaused(pauseKey, false);
    }

    public void pressKey(final int group, final int bit) {
        this.mCalcThread.queueRunnable(new Runnable() {
            public void run() {
                CalcInterface.PressKey(group, bit);
                CalculatorManager.this.mKeyTimePressed[group][bit] = CalcInterface.Tstates();
            }
        });
    }

    public void releaseKey(final int group, final int bit) {
        this.mCalcThread.queueRunnable(new Runnable() {
            public void run() {
                if (CalculatorManager.this.hasCalcProcessedKey(group, bit)) {
                    CalcInterface.ReleaseKey(group, bit);
                    return;
                }
                CalculatorManager.this.mRepressExecutor.schedule(new TimerTask() {
                    public void run() {
                        CalculatorManager.this.releaseKey(group, bit);
                    }
                }, 40, TimeUnit.MILLISECONDS);
            }
        });
    }

    private boolean hasCalcProcessedKey(int group, int bit) {
        long tstates = CalcInterface.Tstates();
        long timePressed = this.mKeyTimePressed[group][bit];
        if (group == 5 && bit == 0) {
            if (MIN_TSTATE_ON_KEY + timePressed <= tstates) {
                return true;
            }
            return false;
        } else if (MIN_TSTATE_KEY + timePressed > tstates) {
            return false;
        } else {
            return true;
        }
    }

    public void resetCalc() {
        this.mCalcThread.resetCalc();
    }

    public void saveCurrentRom() {
        if (this.mCurrentRomFile == null) {
            return;
        }
        if (!this.mCurrentRomFile.equals("") || !this.mHasLoadedRom.get()) {
            this.mCalcThread.queueRunnable(new SaveCurrentRomRunnable());
        }
    }

    public void createRom(String osFilePath, String bootPagePath, String createdFilePath, CalcModel calcModel, FileLoadedCallback callback) {
        final String str = osFilePath;
        final String str2 = bootPagePath;
        final String str3 = createdFilePath;
        final CalcModel calcModel2 = calcModel;
        final FileLoadedCallback fileLoadedCallback = callback;
        this.mCalcThread.queueRunnable(new Runnable() {
            public void run() {
                fileLoadedCallback.onFileLoaded(CalcInterface.CreateRom(str, str2, str3, calcModel2.getModelInt()));
            }
        });
    }

    public CalcModel getModel() {
        return this.mCurrentModel;
    }

    public void testLoadLib() {
    }

    private void updateCurrentRomSetting() {
        this.mSharedPrefs.edit().putString(PreferenceConstants.ROM_PATH.toString(), this.mCurrentRomFile).putInt(PreferenceConstants.ROM_MODEL.toString(), CalcInterface.GetModel()).commit();
    }

    private void handleRomLoaded(FileLoadedCallback callback, int errorCode) {
        CalcModel model = getModel();
        this.mUserTracker.setKey("RomType", model.getModelInt());
        boolean wasSuccessful = errorCode == 0;
        if (this.mCalcSkin != null && wasSuccessful) {
            this.mSkinLoader.loadSkinAndKeymap(model);
        }
        this.mHasLoadedRom.set(wasSuccessful);
        callback.onFileLoaded(errorCode);
    }
}
