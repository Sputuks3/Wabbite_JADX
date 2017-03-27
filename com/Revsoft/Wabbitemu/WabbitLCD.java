package com.Revsoft.Wabbitemu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout.LayoutParams;
import com.Revsoft.Wabbitemu.calc.CalcScreenUpdateCallback;
import com.Revsoft.Wabbitemu.calc.MainThread;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WabbitLCD extends SurfaceView implements CalcScreenUpdateCallback {
    private final CalcKeyManager mCalcKeyManager = CalcKeyManager.getInstance();
    private final ExecutorService mExecutorService;
    private final MainThread mMainThread;

    public WabbitLCD(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        this.mMainThread = new MainThread();
        holder.addCallback(this.mMainThread);
        setFocusable(true);
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        return this.mCalcKeyManager.doKeyDownKeyCode(keyCode);
    }

    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        return this.mCalcKeyManager.doKeyUpKeyCode(keyCode);
    }

    public void onUpdateScreen() {
        this.mExecutorService.submit(this.mMainThread);
    }

    public IntBuffer getScreenBuffer() {
        return this.mMainThread.getScreenBuffer();
    }

    public void updateSkin(Rect lcdRect, Rect lcdSkinRect) {
        if (this.mMainThread != null && lcdRect != null && lcdSkinRect != null) {
            LayoutParams layoutParams = (LayoutParams) getLayoutParams();
            layoutParams.width = lcdSkinRect.width();
            layoutParams.height = lcdSkinRect.height();
            layoutParams.setMargins(lcdSkinRect.left, lcdSkinRect.top, 0, 0);
            setLayoutParams(layoutParams);
            getHolder().setFixedSize(lcdSkinRect.width(), lcdSkinRect.height());
            this.mMainThread.recreateScreen(lcdRect, lcdSkinRect);
        }
    }

    @Nullable
    public Bitmap getScreen() {
        if (this.mMainThread == null) {
            return null;
        }
        return this.mMainThread.getScreen();
    }
}
