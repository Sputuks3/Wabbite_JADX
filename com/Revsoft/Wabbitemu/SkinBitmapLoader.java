package com.Revsoft.Wabbitemu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.internal.view.SupportMenu;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;
import com.Revsoft.Wabbitemu.CalcSkin.CalcSkinChangedListener;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.calc.CalculatorManager;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionActivity;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionEvent;
import com.Revsoft.Wabbitemu.utils.PreferenceConstants;
import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import io.fabric.sdk.android.services.settings.SettingsJsonConstants;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkinBitmapLoader {
    private static final Point[] RECT_POINTS = new Point[]{new Point(150, 1350), new Point(190, 1366), new Point(524, 1364), new Point(558, 1350), new Point(632, 1070), new Point(632, 546), new Point(74, 546), new Point(74, 1136), new Point(626, 1136), new Point(626, 546), new Point(68, 546), new Point(68, 1070), new Point(142, 1350), new Point(176, 1364), new Point(510, 1366), new Point(550, 1350)};
    private static final int SKIN_HEIGHT = 1450;
    private static final int SKIN_WIDTH = 700;
    private final SparseArray<Rect> mButtonRects = new SparseArray();
    private Context mContext;
    private boolean mCorrectRatio;
    private int mFaceplateColor;
    private Path mFaceplatePath;
    private final AtomicBoolean mHasLoadedSkin = new AtomicBoolean(false);
    private double mHeightMaxScale;
    private int mKeymapHeight;
    private double mKeymapHeightScale;
    private int[] mKeymapPixels;
    private int mKeymapWidth;
    private double mKeymapWidthScale;
    private boolean mLargeScreen;
    private Rect mLcdRect;
    private boolean mMaximizeSkin;
    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PreferenceConstants.FACEPLATE_COLOR.toString())) {
                SkinBitmapLoader.this.mFaceplateColor = sharedPreferences.getInt(key, -7829368);
                loadSkinThread();
            } else if (key.equals(PreferenceConstants.LARGE_SCREEN.toString())) {
                SkinBitmapLoader.this.mLargeScreen = sharedPreferences.getBoolean(key, false);
                loadSkinThread();
            } else if (key.equals(PreferenceConstants.CORRECT_SCREEN_RATIO.toString())) {
                SkinBitmapLoader.this.mCorrectRatio = sharedPreferences.getBoolean(key, false);
                loadSkinThread();
            } else if (key.equals(PreferenceConstants.FULL_SKIN_SIZE.toString())) {
                SkinBitmapLoader.this.mMaximizeSkin = SkinBitmapLoader.this.mSharedPrefs.getBoolean(PreferenceConstants.FULL_SKIN_SIZE.toString(), false);
                loadSkinThread();
            }
        }

        private void loadSkinThread() {
            new Thread(new Runnable() {
                public void run() {
                    SkinBitmapLoader.this.mHasLoadedSkin.set(false);
                    SkinBitmapLoader.this.loadSkinAndKeymap(CalculatorManager.getInstance().getModel());
                }
            }).start();
        }
    };
    private double mRatio;
    private volatile Bitmap mRenderedSkinImage;
    private Resources mResources;
    private Rect mScreenRect;
    private SharedPreferences mSharedPrefs;
    private final Set<CalcSkinChangedListener> mSkinListeners = new HashSet();
    private Rect mSkinRect;
    private int mSkinX;
    private int mSkinY;
    private final UserActivityTracker mUserActivityTracker = UserActivityTracker.getInstance();
    private double mWidthMaxScale;

    private static class SingletonHolder {
        private static final SkinBitmapLoader SINGLETON = new SkinBitmapLoader();

        private SingletonHolder() {
        }
    }

    public static SkinBitmapLoader getInstance() {
        return SingletonHolder.SINGLETON;
    }

    public void initialize(Context context) {
        this.mContext = context;
        this.mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mCorrectRatio = this.mSharedPrefs.getBoolean(PreferenceConstants.CORRECT_SCREEN_RATIO.toString(), false);
        this.mLargeScreen = this.mSharedPrefs.getBoolean(PreferenceConstants.LARGE_SCREEN.toString(), false);
        this.mFaceplateColor = this.mSharedPrefs.getInt(PreferenceConstants.FACEPLATE_COLOR.toString(), -7829368);
        this.mMaximizeSkin = this.mSharedPrefs.getBoolean(PreferenceConstants.FULL_SKIN_SIZE.toString(), false);
        this.mResources = context.getResources();
    }

    public void registerSkinChangedListener(CalcSkinChangedListener listener) {
        this.mSkinListeners.add(listener);
    }

    public void unregisterSkinChangedListener(CalcSkinChangedListener listener) {
        this.mSkinListeners.remove(listener);
    }

    public void destroySkin() {
        this.mRenderedSkinImage = null;
        this.mScreenRect = null;
        this.mSkinRect = null;
        this.mLcdRect = null;
        this.mKeymapPixels = null;
        this.mHasLoadedSkin.set(false);
    }

    public void loadSkinAndKeymap(CalcModel model) {
        if (!this.mHasLoadedSkin.getAndSet(true)) {
            String skinImageFile;
            String keymapImageFile;
            switch (model) {
                case TI_73:
                    skinImageFile = "ti73";
                    keymapImageFile = "ti83pkeymap";
                    break;
                case TI_81:
                    skinImageFile = "ti81";
                    keymapImageFile = "ti81keymap";
                    break;
                case TI_82:
                    skinImageFile = "ti82";
                    keymapImageFile = "ti82keymap";
                    break;
                case TI_83:
                    skinImageFile = "ti83";
                    keymapImageFile = "ti83keymap";
                    break;
                case TI_83P:
                    skinImageFile = "ti83p";
                    keymapImageFile = "ti83pkeymap";
                    break;
                case TI_83PSE:
                    skinImageFile = "ti83pse";
                    keymapImageFile = "ti83pkeymap";
                    break;
                case TI_84P:
                    skinImageFile = "ti84p";
                    keymapImageFile = "ti84psekeymap";
                    break;
                case TI_84PSE:
                    skinImageFile = "ti84pse";
                    keymapImageFile = "ti84psekeymap";
                    break;
                case TI_84PCSE:
                    skinImageFile = "ti84pcse";
                    keymapImageFile = "ti84pcsekeymap";
                    break;
                case TI_85:
                    skinImageFile = "ti85";
                    keymapImageFile = "ti85keymap";
                    break;
                case TI_86:
                    skinImageFile = "ti86";
                    keymapImageFile = "ti86keymap";
                    break;
                default:
                    return;
            }
            if (this.mLargeScreen) {
                keymapImageFile = keymapImageFile + "large";
            }
            if (this.mResources.getConfiguration().smallestScreenWidthDp >= SettingsJsonConstants.ANALYTICS_FLUSH_INTERVAL_SECS_DEFAULT) {
                keymapImageFile = "tablet/" + keymapImageFile;
                skinImageFile = "tablet/" + skinImageFile;
            } else {
                keymapImageFile = "phone/" + keymapImageFile;
                skinImageFile = "phone/" + skinImageFile;
            }
            setSurfaceSize(skinImageFile + ".png", keymapImageFile + ".png", model);
            notifySkinChanged();
        }
    }

    private void notifySkinChanged() {
        for (CalcSkinChangedListener listener : this.mSkinListeners) {
            listener.onCalcSkinChanged(this.mLcdRect, this.mScreenRect);
        }
    }

    private synchronized void setSurfaceSize(String skinImageId, String keymapImageId, CalcModel model) {
        int skinWidth;
        int skinHeight;
        Point displaySize = getDisplaySize();
        this.mSkinX = 0;
        this.mSkinY = 0;
        this.mRatio = 1.0d;
        int smallestScreenWidthDp = this.mResources.getConfiguration().smallestScreenWidthDp;
        boolean isTablet = smallestScreenWidthDp >= 600;
        if (isTablet) {
            this.mRatio = Math.min(((double) displaySize.x) / 700.0d, ((double) displaySize.y) / 1450.0d);
            skinWidth = (int) (700.0d * this.mRatio);
            skinHeight = (int) (1450.0d * this.mRatio);
            this.mSkinX = (displaySize.x - skinWidth) / 2;
            this.mSkinY = (displaySize.y - skinHeight) / 2;
        } else {
            skinWidth = displaySize.x;
            skinHeight = displaySize.y;
        }
        Bitmap skinImage = getScaledSkinImage(skinImageId, this.mResources, skinWidth, skinHeight);
        Bitmap keymapImage = getKeymapImage(keymapImageId, this.mResources);
        this.mKeymapWidth = keymapImage.getWidth();
        this.mKeymapHeight = keymapImage.getHeight();
        this.mKeymapWidthScale = ((double) displaySize.x) / ((double) this.mKeymapWidth);
        this.mKeymapHeightScale = ((double) displaySize.y) / ((double) this.mKeymapHeight);
        if (smallestScreenWidthDp >= 600) {
            if (this.mKeymapHeightScale > this.mKeymapWidthScale) {
                this.mKeymapHeightScale = this.mKeymapWidthScale;
            } else {
                this.mKeymapWidthScale = this.mKeymapHeightScale;
            }
        }
        this.mKeymapPixels = new int[(this.mKeymapWidth * this.mKeymapHeight)];
        keymapImage.getPixels(this.mKeymapPixels, 0, this.mKeymapWidth, 0, 0, this.mKeymapWidth, this.mKeymapHeight);
        keymapImage.recycle();
        Rect lcdRect = parseKeymap();
        if (lcdRect == null) {
            this.mLcdRect = new Rect(0, 0, 1, 1);
        } else {
            int lcdWidth;
            int lcdHeight;
            double extraPadding = (double) this.mContext.getResources().getDimension(R.dimen.maxSkinPadding);
            if (!this.mMaximizeSkin || isTablet) {
                if (isTablet) {
                    this.mSkinRect = new Rect(0, 0, displaySize.x, displaySize.y);
                } else {
                    this.mSkinRect = new Rect(0, 0, skinImage.getWidth(), skinImage.getHeight());
                }
                this.mWidthMaxScale = 1.0d;
                this.mHeightMaxScale = 1.0d;
            } else {
                this.mSkinRect = new Rect((int) ((((double) this.mSkinRect.left) * this.mKeymapWidthScale) - extraPadding), (int) ((((double) this.mSkinRect.top) * this.mKeymapHeightScale) - extraPadding), (int) ((((double) this.mSkinRect.right) * this.mKeymapWidthScale) + extraPadding), (int) ((((double) this.mSkinRect.bottom) * this.mKeymapHeightScale) + extraPadding));
                this.mWidthMaxScale = ((double) skinWidth) / ((double) (this.mSkinRect.right - this.mSkinRect.left));
                this.mHeightMaxScale = ((double) skinHeight) / ((double) (this.mSkinRect.bottom - this.mSkinRect.top));
            }
            switch (model) {
                case TI_84PCSE:
                    lcdWidth = 320;
                    lcdHeight = 240;
                    break;
                case TI_85:
                case TI_86:
                    lcdWidth = 128;
                    lcdHeight = 64;
                    break;
                default:
                    lcdWidth = 96;
                    lcdHeight = 64;
                    break;
            }
            this.mLcdRect = new Rect(0, 0, lcdWidth, lcdHeight);
            this.mScreenRect = new Rect((((int) ((((double) lcdRect.left) * this.mKeymapWidthScale) * this.mWidthMaxScale)) + this.mSkinX) - this.mSkinRect.left, ((int) ((((double) lcdRect.top) * this.mKeymapHeightScale) * this.mHeightMaxScale)) - this.mSkinRect.top, (((int) ((((double) lcdRect.right) * this.mKeymapWidthScale) * this.mWidthMaxScale)) + this.mSkinX) - this.mSkinRect.left, ((int) ((((double) lcdRect.bottom) * this.mKeymapHeightScale) * this.mHeightMaxScale)) - this.mSkinRect.top);
            if (this.mCorrectRatio) {
                double screenRatio = ((double) this.mLcdRect.width()) / ((double) this.mLcdRect.height());
                int shift;
                Rect rect;
                if (((double) this.mScreenRect.width()) / ((double) this.mScreenRect.height()) > screenRatio) {
                    int oldWidth = this.mScreenRect.width();
                    int screenWidth = (int) (((double) this.mScreenRect.height()) * screenRatio);
                    this.mScreenRect.right = this.mScreenRect.left + screenWidth;
                    shift = (oldWidth - screenWidth) / 2;
                    rect = this.mScreenRect;
                    rect.left += shift;
                    rect = this.mScreenRect;
                    rect.right += shift;
                } else {
                    int oldHeight = this.mScreenRect.height();
                    int screenHeight = (int) (((double) this.mScreenRect.width()) * screenRatio);
                    this.mScreenRect.bottom = this.mScreenRect.top + screenHeight;
                    shift = (oldHeight - screenHeight) / 2;
                    rect = this.mScreenRect;
                    rect.top += shift;
                    rect = this.mScreenRect;
                    rect.bottom += shift;
                }
            }
            this.mFaceplateColor = this.mSharedPrefs.getInt(PreferenceConstants.FACEPLATE_COLOR.toString(), -7829368);
            this.mSharedPrefs.registerOnSharedPreferenceChangeListener(this.mPrefListener);
            createRenderSkin(displaySize.x, displaySize.y, skinImage, model);
        }
    }

    private Bitmap getScaledSkinImage(String skinImageId, Resources resources, int skinWidth, int skinHeight) {
        Bitmap skinImage = getSkinImage(skinImageId, resources, null);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(skinImage, skinWidth, skinHeight, true);
        skinImage.recycle();
        return scaledBitmap;
    }

    private Bitmap getSkinImage(String skinImageId, Resources resources, Options options) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        try {
            inputStream = resources.getAssets().open(skinImageId);
            bitmap = BitmapFactory.decodeStream(inputStream, null, null);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    Log.w("CalcSkin", "Exception closing input stream" + ex.toString());
                }
            }
        } catch (IOException ex2) {
            Log.w("CalcSkin", "Exception reading input stream" + ex2.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex22) {
                    Log.w("CalcSkin", "Exception closing input stream" + ex22.toString());
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex222) {
                    Log.w("CalcSkin", "Exception closing input stream" + ex222.toString());
                }
            }
        }
        return bitmap;
    }

    private Bitmap getKeymapImage(String keymapImageId, Resources resources) {
        return getSkinImage(keymapImageId, resources, null);
    }

    private Point getDisplaySize() {
        Display display = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        Point displaySize = new Point();
        if (VERSION.SDK_INT < 17 || !this.mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
            display.getSize(displaySize);
        } else {
            display.getRealSize(displaySize);
        }
        return displaySize;
    }

    private Rect parseKeymap() {
        this.mButtonRects.clear();
        Rect lcdRect = null;
        this.mSkinRect = null;
        for (int pixelOffset = 0; pixelOffset < this.mKeymapWidth * this.mKeymapHeight; pixelOffset++) {
            int pixelData = this.mKeymapPixels[pixelOffset];
            if (pixelData == SupportMenu.CATEGORY_MASK) {
                if (lcdRect == null) {
                    int x = pixelOffset % this.mKeymapWidth;
                    int y = pixelOffset / this.mKeymapWidth;
                    lcdRect = new Rect(x, y, x, y);
                    this.mSkinRect = new Rect(lcdRect);
                } else {
                    updateRect(lcdRect, pixelOffset);
                    updateRect(this.mSkinRect, pixelOffset);
                }
            }
            if (pixelData != -1) {
                Rect rect = (Rect) this.mButtonRects.get(pixelData);
                if (rect == null) {
                    x = pixelOffset % this.mKeymapWidth;
                    y = pixelOffset / this.mKeymapWidth;
                    this.mButtonRects.put(pixelData, new Rect(x, y, x, y));
                } else {
                    updateRect(rect, pixelOffset);
                    updateRect(this.mSkinRect, pixelOffset);
                }
            }
        }
        if (lcdRect != null) {
            return lcdRect;
        }
        Log.d("Keymap", "Keymap fail");
        return null;
    }

    private void updateRect(Rect rect, int offset) {
        int x = offset % this.mKeymapWidth;
        int y = offset / this.mKeymapWidth;
        if (rect.left > x) {
            rect.left = x;
        } else if (rect.right < x) {
            rect.right = x;
        }
        if (rect.top > y) {
            rect.top = y;
        } else if (rect.bottom < y) {
            rect.bottom = y;
        }
    }

    private void createRenderSkin(int width, int height, Bitmap skinImage, CalcModel model) {
        this.mRenderedSkinImage = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas skinCanvas = new Canvas(this.mRenderedSkinImage);
        skinCanvas.setDensity(0);
        this.mFaceplatePath = getSkinPath();
        drawFaceplate(skinCanvas, model);
        skinCanvas.drawBitmap(skinImage, (float) this.mSkinX, (float) this.mSkinY, null);
    }

    private Path getSkinPath() {
        Path path = new Path();
        path.moveTo((float) RECT_POINTS[0].x, (float) RECT_POINTS[0].y);
        for (int i = 1; i < RECT_POINTS.length; i++) {
            path.lineTo((float) RECT_POINTS[i].x, (float) RECT_POINTS[i].y);
        }
        Matrix scaleMatrix = new Matrix();
        path.computeBounds(new RectF(), true);
        scaleMatrix.setScale((float) this.mRatio, (float) this.mRatio, (float) this.mSkinX, (float) this.mSkinY);
        path.offset((float) this.mSkinX, (float) this.mSkinY);
        path.transform(scaleMatrix);
        return path;
    }

    public int getKeymapPixel(int x, int y) {
        int index = (this.mKeymapWidth * ((int) (((double) (this.mSkinRect.top + y)) / (this.mKeymapHeightScale * this.mHeightMaxScale)))) + ((int) (((double) (this.mSkinRect.left + x)) / (this.mKeymapWidthScale * this.mWidthMaxScale)));
        if (index > this.mKeymapPixels.length) {
            this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.INVALID_KEYMAP_PIXEL);
            return this.mKeymapPixels[this.mKeymapPixels.length - (index % this.mKeymapWidth)];
        } else if (index >= 0) {
            return this.mKeymapPixels[index];
        } else {
            this.mUserActivityTracker.reportUserAction(UserActionActivity.MAIN_ACTIVITY, UserActionEvent.INVALID_KEYMAP_PIXEL);
            return this.mKeymapPixels[0 - (index % this.mKeymapWidth)];
        }
    }

    private void drawFaceplate(Canvas canvas, CalcModel model) {
        if (model == CalcModel.TI_84PSE || model == CalcModel.TI_84PCSE) {
            this.mFaceplateColor = this.mSharedPrefs.getInt(PreferenceConstants.FACEPLATE_COLOR.toString(), -7829368);
            if (this.mSkinX > 0) {
                Paint faceplatePaint = new Paint();
                faceplatePaint.setStyle(Style.FILL);
                faceplatePaint.setColor(this.mFaceplateColor);
                canvas.drawPath(this.mFaceplatePath, faceplatePaint);
                return;
            }
            canvas.drawColor(this.mFaceplateColor);
        }
    }

    public Bitmap getRenderedSkin() {
        return this.mRenderedSkinImage;
    }

    public Rect getLcdRect() {
        return this.mLcdRect;
    }

    public Rect getLcdSkinRect() {
        return this.mScreenRect;
    }

    public Rect getSkinRect() {
        return this.mSkinRect;
    }

    public int getSkinX() {
        return this.mSkinX;
    }

    public int getSkinY() {
        return this.mSkinY;
    }

    public boolean isOutsideKeymap(int x, int y) {
        return ((double) x) >= ((double) this.mKeymapWidth) * this.mKeymapWidthScale || ((double) y) >= ((double) this.mKeymapHeight) * this.mKeymapHeightScale || x < 0 || y < 0 || this.mKeymapPixels == null;
    }

    @Nullable
    public Rect getKeymapRect(int x, int y) {
        Rect buttonRect = (Rect) this.mButtonRects.get(getKeymapPixel(x, y));
        if (buttonRect == null) {
            return null;
        }
        return new Rect((((int) ((((double) buttonRect.left) * this.mKeymapWidthScale) * this.mWidthMaxScale)) + this.mSkinX) - this.mSkinRect.left, (((int) ((((double) buttonRect.top) * this.mKeymapHeightScale) * this.mHeightMaxScale)) + this.mSkinY) - this.mSkinRect.top, (((int) ((((double) buttonRect.right) * this.mKeymapWidthScale) * this.mWidthMaxScale)) + this.mSkinX) - this.mSkinRect.left, (((int) ((((double) buttonRect.bottom) * this.mKeymapHeightScale) * this.mHeightMaxScale)) + this.mSkinY) - this.mSkinRect.top);
    }
}
