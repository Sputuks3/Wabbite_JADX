package com.Revsoft.Wabbitemu.calc;

import android.os.SystemClock;
import android.util.Log;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

public class CalcThread extends Thread {
    private static final long FPS = 50;
    private static final long TPF = (TimeUnit.SECONDS.toMillis(1) / FPS);
    private long mDifference;
    private final AtomicBoolean mIsPaused = new AtomicBoolean(false);
    private final List<String> mPauseList = new ArrayList();
    private long mPreviousTimerMillis;
    private final List<Runnable> mRunnables = new CopyOnWriteArrayList();
    private CalcScreenUpdateCallback mScreenUpdateCallback;

    public void run() {
        while (!isInterrupted()) {
            for (Runnable runnable : this.mRunnables) {
                runnable.run();
                this.mRunnables.remove(runnable);
            }
            if (this.mIsPaused.get()) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            long newTimeMillis = SystemClock.elapsedRealtime();
            this.mDifference += ((newTimeMillis - this.mPreviousTimerMillis) & 63) - TPF;
            this.mPreviousTimerMillis = newTimeMillis;
            if (this.mDifference > (-TPF)) {
                CalcInterface.RunCalcs();
                if (this.mScreenUpdateCallback != null) {
                    IntBuffer screenBuffer = this.mScreenUpdateCallback.getScreenBuffer();
                    if (screenBuffer != null) {
                        screenBuffer.rewind();
                        CalcInterface.GetLCD(screenBuffer);
                        screenBuffer.rewind();
                        this.mScreenUpdateCallback.onUpdateScreen();
                    }
                }
                while (this.mDifference >= TPF) {
                    CalcInterface.RunCalcs();
                    this.mDifference -= TPF;
                }
            } else {
                this.mDifference += TPF;
                Log.d("Wabbitemu", "Frame skip");
            }
        }
    }

    public void setPaused(@Nonnull String key, boolean shouldBePaused) {
        if (shouldBePaused) {
            if (!this.mPauseList.contains(key)) {
                this.mPauseList.add(key);
            }
            this.mIsPaused.set(true);
            return;
        }
        this.mPauseList.remove(key);
        if (this.mPauseList.size() == 0) {
            this.mIsPaused.set(false);
        }
    }

    public void resetCalc() {
        this.mRunnables.add(new Runnable() {
            public void run() {
                CalcInterface.ResetCalc();
            }
        });
    }

    public void setScreenUpdateCallback(CalcScreenUpdateCallback callback) {
        this.mScreenUpdateCallback = callback;
    }

    public void queueRunnable(Runnable runnable) {
        this.mRunnables.add(runnable);
    }
}
