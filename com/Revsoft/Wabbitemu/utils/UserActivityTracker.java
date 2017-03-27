package com.Revsoft.Wabbitemu.utils;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionActivity;
import com.Revsoft.Wabbitemu.utils.AnalyticsConstants.UserActionEvent;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.Crashlytics.Builder;
import com.crashlytics.android.core.CrashlyticsCore;
import io.fabric.sdk.android.Fabric;

public class UserActivityTracker {
    private boolean mIsInitialized;

    private static class SingletonHolder {
        private static final UserActivityTracker SINGLETON = new UserActivityTracker();

        private SingletonHolder() {
        }
    }

    public static UserActivityTracker getInstance() {
        return SingletonHolder.SINGLETON;
    }

    private UserActivityTracker() {
    }

    public void initializeIfNecessary(Context context) {
        if (!this.mIsInitialized) {
            this.mIsInitialized = true;
            Fabric.with(context, new Builder().core(new CrashlyticsCore.Builder().disabled(false).build()).build());
            String androidId = Secure.getString(context.getApplicationContext().getContentResolver(), "android_id");
            if (androidId == null) {
                Crashlytics.setUserIdentifier("emptyAndroidId");
            } else {
                Crashlytics.setUserIdentifier(androidId);
            }
        }
    }

    public void reportActivityStart(Activity activity) {
        Crashlytics.log("Starting " + activity.getClass().getSimpleName());
    }

    public void reportActivityStop(Activity activity) {
        Crashlytics.log("Stop " + activity.getClass().getSimpleName());
    }

    public void reportBreadCrumb(String breadcrumb) {
        Crashlytics.log(4, "UserActivityTracker", breadcrumb);
    }

    public void reportBreadCrumb(String format, Object... args) {
        reportBreadCrumb(String.format(format, args));
    }

    public void setKey(String key, int value) {
        Crashlytics.setInt(key, value);
    }

    public void reportUserAction(UserActionActivity activity, UserActionEvent event) {
        reportUserAction(activity, event, null);
    }

    public void reportUserAction(UserActionActivity activity, UserActionEvent event, String extra) {
        String log = String.format("Activity: [%s] Event: [%s] Extra: [%s]", new Object[]{activity, event, extra});
        Crashlytics.log(log);
        Log.i("Wabbitemu", log);
    }
}
