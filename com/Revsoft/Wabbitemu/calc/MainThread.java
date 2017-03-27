package com.Revsoft.Wabbitemu.calc;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class MainThread implements Callback, Runnable {
    private IntBuffer mCurrentScreenBuffer;
    private volatile boolean mHasCreatedLcd;
    private Rect mLcdRect;
    private final Paint mPaint = new Paint();
    private volatile Bitmap mScreenBitmap;
    private final Object mScreenLock = new Object();
    private Rect mScreenRect;
    private volatile SurfaceHolder mSurfaceHolder;

    public MainThread() {
        this.mPaint.setAntiAlias(false);
        this.mPaint.setARGB(255, 255, 255, 255);
    }

    public void recreateScreen(Rect lcdRect, Rect screenRect) {
        this.mLcdRect = lcdRect;
        this.mScreenRect = new Rect(screenRect);
        this.mScreenRect.offset(-this.mScreenRect.left, -this.mScreenRect.top);
        this.mScreenBitmap = Bitmap.createBitmap(this.mLcdRect.width(), this.mLcdRect.height(), Config.ARGB_8888);
        this.mCurrentScreenBuffer = ByteBuffer.allocateDirect((this.mLcdRect.width() * this.mLcdRect.height()) * 4).asIntBuffer();
        this.mHasCreatedLcd = true;
    }

    public IntBuffer getScreenBuffer() {
        return this.mCurrentScreenBuffer;
    }

    @Nullable
    public Bitmap getScreen() {
        Bitmap bitmap;
        synchronized (this.mScreenLock) {
            bitmap = this.mScreenBitmap;
        }
        return bitmap;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        if (this.mSurfaceHolder != null && this.mHasCreatedLcd) {
            synchronized (this.mScreenLock) {
                Canvas canvas = null;
                try {
                    canvas = this.mSurfaceHolder.lockCanvas();
                    if (canvas != null) {
                        this.mScreenBitmap.copyPixelsFromBuffer(this.mCurrentScreenBuffer);
                        if (getScreen() != null) {
                            canvas.drawBitmap(this.mScreenBitmap, this.mLcdRect, this.mScreenRect, this.mPaint);
                        }
                        if (canvas != null) {
                            this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    } else if (canvas != null) {
                        this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                } catch (Throwable th) {
                    if (canvas != null) {
                        this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        this.mSurfaceHolder = holder;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (this.mScreenLock) {
            this.mSurfaceHolder = null;
        }
    }
}
