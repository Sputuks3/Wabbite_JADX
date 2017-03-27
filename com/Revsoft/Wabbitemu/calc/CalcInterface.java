package com.Revsoft.Wabbitemu.calc;

import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import java.nio.IntBuffer;

class CalcInterface {
    public static final int NO_CALC = -1;
    public static final int ON_KEY_BIT = 0;
    public static final int ON_KEY_GROUP = 5;
    public static final int TI_73 = 5;
    public static final int TI_81 = 0;
    public static final int TI_82 = 1;
    public static final int TI_83 = 2;
    public static final int TI_83P = 6;
    public static final int TI_83PSE = 7;
    public static final int TI_84P = 8;
    public static final int TI_84PCSE = 10;
    public static final int TI_84PSE = 9;
    public static final int TI_85 = 3;
    public static final int TI_86 = 4;

    public static native int CreateRom(String str, String str2, String str3, int i);

    public static native int GetLCD(IntBuffer intBuffer);

    public static native int GetModel();

    public static native void Initialize(String str);

    public static native int LoadFile(String str);

    public static native void PressKey(int i, int i2);

    public static native void ReleaseKey(int i, int i2);

    public static native void ResetCalc();

    public static native void RunCalcs();

    public static native boolean SaveCalcState(String str);

    public static native void SetAutoTurnOn(boolean z);

    public static native long Tstates();

    CalcInterface() {
    }

    static {
        UserActivityTracker userActivityTracker = UserActivityTracker.getInstance();
        userActivityTracker.reportBreadCrumb("Starting loading libarary");
        System.loadLibrary("Wabbitemu");
        userActivityTracker.reportBreadCrumb("Loaded library");
    }
}
