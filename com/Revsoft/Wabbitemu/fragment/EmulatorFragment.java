package com.Revsoft.Wabbitemu.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.Revsoft.Wabbitemu.CalcSkin;
import com.Revsoft.Wabbitemu.CalcSkin.CalcSkinChangedListener;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.SkinBitmapLoader;
import com.Revsoft.Wabbitemu.WabbitLCD;
import com.Revsoft.Wabbitemu.calc.CalculatorManager;
import com.Revsoft.Wabbitemu.calc.FileLoadedCallback;
import com.Revsoft.Wabbitemu.utils.PreferenceConstants;
import com.Revsoft.Wabbitemu.utils.ProgressTask;
import com.Revsoft.Wabbitemu.utils.ViewUtils;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class EmulatorFragment extends Fragment {
    private static final String ACTIVITY_PAUSE_KEY = "EmulatorFragment";
    public static final int REQUEST_CODE = 21;
    private CalcSkin mCalcSkin;
    private final CalculatorManager mCalculatorManager = CalculatorManager.getInstance();
    private Context mContext;
    private File mFileToHandle;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ImmersiveModeListener mImmersiveModeListener = new ImmersiveModeListener();
    private boolean mIsInitialized;
    private Runnable mRunnableToHandle;
    private ProgressTask mSendFileTask;
    private SharedPreferences mSharedPrefs;
    private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
    private final SkinUpdateListener mSkinUpdateListener = new SkinUpdateListener();
    private WabbitLCD mSurfaceView;

    private class ImmersiveModeListener implements OnSharedPreferenceChangeListener {
        private ImmersiveModeListener() {
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PreferenceConstants.IMMERSIVE_MODE.toString().equals(key)) {
                EmulatorFragment.this.mSkinLoader.destroySkin();
                EmulatorFragment.this.mSkinLoader.loadSkinAndKeymap(CalculatorManager.getInstance().getModel());
            }
        }
    }

    private class LoadFileAsyncTask extends ProgressTask {
        private final File mFile;
        private final boolean mIsRom;
        private final Runnable mRunnable;
        private volatile Boolean mSuccess;

        private LoadFileAsyncTask(Context context, String descriptionString, boolean isCancelable, Runnable runnable, File f, boolean isRom) {
            super(context, descriptionString, isCancelable);
            this.mRunnable = runnable;
            this.mFile = f;
            this.mIsRom = isRom;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            if (this.mIsRom) {
                EmulatorFragment.this.mCalcSkin.destroySkin();
                EmulatorFragment.this.mCalcSkin.invalidate();
            }
        }

        protected Boolean doInBackground(Void... params) {
            final CountDownLatch latch = new CountDownLatch(1);
            this.mSuccess = Boolean.FALSE;
            FileLoadedCallback callback = new FileLoadedCallback() {
                public void onFileLoaded(int errorCode) {
                    LoadFileAsyncTask.this.mSuccess = Boolean.valueOf(errorCode == 0);
                    latch.countDown();
                }
            };
            if (this.mIsRom) {
                EmulatorFragment.this.mCalculatorManager.loadRomFile(this.mFile, callback);
            } else {
                EmulatorFragment.this.mCalculatorManager.loadFile(this.mFile, callback);
            }
            try {
                latch.await();
                return this.mSuccess;
            } catch (InterruptedException e) {
                return Boolean.FALSE;
            }
        }

        protected void onPostExecute(Boolean wasSuccessful) {
            if (!(wasSuccessful.booleanValue() || this.mRunnable == null)) {
                this.mRunnable.run();
            }
            super.onPostExecute(wasSuccessful);
        }
    }

    private class SkinUpdateListener implements CalcSkinChangedListener {
        private SkinUpdateListener() {
        }

        public void onCalcSkinChanged(final Rect lcdRect, final Rect lcdSkinRect) {
            EmulatorFragment.this.mHandler.post(new Runnable() {
                public void run() {
                    EmulatorFragment.this.mSurfaceView.updateSkin(lcdRect, lcdSkinRect);
                    Log.d("View", "Request update");
                    EmulatorFragment.this.mCalcSkin.invalidate();
                }
            });
        }
    }

    public void handleFile(File f, Runnable runnable) {
        if (this.mIsInitialized) {
            boolean isRom;
            String name = f.getName();
            if (name.endsWith(".rom") || name.endsWith(".sav")) {
                isRom = true;
            } else {
                isRom = false;
            }
            this.mSendFileTask = new LoadFileAsyncTask(this.mContext, this.mContext.getResources().getString(isRom ? R.string.sendingRom : R.string.sendingFile), false, runnable, f, isRom);
            this.mSendFileTask.execute(new Void[0]);
            return;
        }
        this.mFileToHandle = f;
        this.mRunnableToHandle = runnable;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emulator, container);
        this.mSurfaceView = (WabbitLCD) ViewUtils.findViewById(view, (int) R.id.textureView, WabbitLCD.class);
        this.mCalcSkin = (CalcSkin) ViewUtils.findViewById(view, (int) R.id.skinView, CalcSkin.class);
        this.mCalculatorManager.setScreenCallback(this.mSurfaceView);
        this.mCalculatorManager.setCalcSkin(this.mCalcSkin);
        this.mSkinLoader.registerSkinChangedListener(this.mSkinUpdateListener);
        return view;
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSkinLoader.unregisterSkinChangedListener(this.mSkinUpdateListener);
        this.mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this.mImmersiveModeListener);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mContext = getActivity();
        this.mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mSharedPrefs.registerOnSharedPreferenceChangeListener(this.mImmersiveModeListener);
        if (VERSION.SDK_INT >= 23) {
            requestWritePermissions();
        }
    }

    @TargetApi(23)
    private void requestWritePermissions() {
        if (getActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 21);
        }
    }

    public void onResume() {
        this.mCalculatorManager.setCalcSkin(this.mCalcSkin);
        this.mCalculatorManager.unPauseCalc(ACTIVITY_PAUSE_KEY);
        this.mSurfaceView.updateSkin(this.mSkinLoader.getLcdRect(), this.mSkinLoader.getLcdSkinRect());
        this.mCalcSkin.invalidate();
        this.mIsInitialized = true;
        if (this.mFileToHandle != null) {
            handleFile(this.mFileToHandle, this.mRunnableToHandle);
            this.mFileToHandle = null;
            this.mRunnableToHandle = null;
        }
        super.onResume();
        updateSettings();
    }

    public void onPause() {
        this.mCalculatorManager.pauseCalc(ACTIVITY_PAUSE_KEY);
        super.onPause();
        this.mCalculatorManager.saveCurrentRom();
        this.mIsInitialized = false;
        if (this.mSendFileTask != null) {
            this.mSendFileTask.cancel(false);
        }
    }

    @Nullable
    public Bitmap getScreenshot() {
        return this.mSurfaceView.getScreen();
    }

    public void resetCalc() {
        this.mCalculatorManager.resetCalc();
    }

    private void updateSettings() {
        if (this.mSharedPrefs.getBoolean(PreferenceConstants.STAY_AWAKE.toString(), false)) {
            getActivity().getWindow().addFlags(128);
        }
    }
}
