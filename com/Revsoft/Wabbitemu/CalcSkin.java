package com.Revsoft.Wabbitemu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.Revsoft.Wabbitemu.utils.PreferenceConstants;
import java.util.ArrayList;
import java.util.List;

public class CalcSkin extends View {
    private final CalcKeyManager mCalcKeyManager = CalcKeyManager.getInstance();
    private final Rect mDrawRect = new Rect();
    private boolean mHasVibrationEnabled;
    private final List<Rect> mKeymapDrawRect = new ArrayList();
    private final Paint mKeymapPaint = new Paint();
    private final Paint mPaint;
    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PreferenceConstants.USE_VIBRATION.toString())) {
                CalcSkin.this.mHasVibrationEnabled = sharedPreferences.getBoolean(key, true);
            }
        }
    };
    private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
    private final Vibrator mVibrator;

    public interface CalcSkinChangedListener {
        void onCalcSkinChanged(Rect rect, Rect rect2);
    }

    public CalcSkin(Context context, AttributeSet attrs) {
        super(context, attrs);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this.mPrefListener);
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mHasVibrationEnabled = sharedPrefs.getBoolean(PreferenceConstants.USE_VIBRATION.toString(), true);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(false);
        this.mPaint.setARGB(255, 255, 255, 255);
        this.mKeymapPaint.setAntiAlias(false);
        this.mKeymapPaint.setARGB(128, 0, 0, 0);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        for (int i = 0; i < event.getPointerCount(); i++) {
            handled |= handleTouchEvent(event, i);
        }
        return handled;
    }

    public void onDraw(Canvas canvas) {
        Bitmap renderedSkin = this.mSkinLoader.getRenderedSkin();
        canvas.drawColor(-12303292);
        if (renderedSkin != null) {
            Rect src = this.mSkinLoader.getSkinRect();
            this.mDrawRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(renderedSkin, src, this.mDrawRect, this.mPaint);
        }
        for (Rect rect : this.mKeymapDrawRect) {
            canvas.drawRect(rect, this.mKeymapPaint);
        }
    }

    public void destroySkin() {
        this.mSkinLoader.destroySkin();
    }

    public Rect getLCDRect() {
        return this.mSkinLoader.getLcdRect();
    }

    public Rect getLCDSkinRect() {
        return this.mSkinLoader.getLcdSkinRect();
    }

    private boolean handleTouchEvent(MotionEvent event, int index) {
        int x = (int) (event.getX(index) - ((float) this.mSkinLoader.getSkinX()));
        int y = (int) (event.getY(index) - ((float) this.mSkinLoader.getSkinY()));
        int id = event.getPointerId(index);
        if (this.mSkinLoader.isOutsideKeymap(x, y)) {
            return false;
        }
        int actionMasked = event.getActionMasked();
        if (actionMasked == 1 || actionMasked == 6 || actionMasked == 3) {
            for (int i = 0; i < this.mKeymapDrawRect.size(); i++) {
                invalidate((Rect) this.mKeymapDrawRect.get(i));
            }
            this.mKeymapDrawRect.clear();
            this.mCalcKeyManager.doKeyUp(id);
            return true;
        }
        int color = this.mSkinLoader.getKeymapPixel(x, y);
        if (Color.red(color) == 255) {
            return false;
        }
        int group = Color.green(color) >> 4;
        int bit = Color.blue(color) >> 4;
        if (group > 7 || bit > 7) {
            return false;
        }
        if (actionMasked == 0 || actionMasked == 5) {
            if (this.mHasVibrationEnabled) {
                this.mVibrator.vibrate(50);
            }
            Rect rect = this.mSkinLoader.getKeymapRect(x, y);
            if (rect == null) {
                return false;
            }
            this.mKeymapDrawRect.add(rect);
            invalidate(rect);
            this.mCalcKeyManager.doKeyDown(id, group, bit);
        }
        return true;
    }
}
