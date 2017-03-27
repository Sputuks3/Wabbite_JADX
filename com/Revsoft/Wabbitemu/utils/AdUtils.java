package com.Revsoft.Wabbitemu.utils;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdRequest.Builder;
import com.google.android.gms.ads.AdView;
import java.util.HashSet;
import java.util.Set;

public class AdUtils {
    private static Set<AdView> sLoadedAds = new HashSet();

    public static void initialize(Application application) {
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityStopped(Activity activity) {
            }

            public void onActivityStarted(Activity activity) {
            }

            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            public void onActivityResumed(Activity activity) {
                for (AdView adView : AdUtils.sLoadedAds) {
                    if (activity.findViewById(adView.getId()) != null) {
                        adView.resume();
                    }
                }
            }

            public void onActivityPaused(Activity activity) {
                for (AdView adView : AdUtils.sLoadedAds) {
                    if (activity.findViewById(adView.getId()) != null) {
                        adView.pause();
                    }
                }
            }

            public void onActivityDestroyed(Activity activity) {
                AdView[] array = new AdView[AdUtils.sLoadedAds.size()];
                AdUtils.sLoadedAds.toArray(array);
                for (AdView adView : array) {
                    if (activity.findViewById(adView.getId()) != null) {
                        AdUtils.destroyView(adView);
                    }
                }
                Log.d("AdUtils", "Activity destroyed, Ads visible: " + AdUtils.sLoadedAds.size());
            }

            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }
        });
    }

    public static void loadAd(View view) {
        AdView adView = (AdView) view;
        try {
            adView.loadAd(new Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice("EB10E0BD305DAC3CDCBD1850A7C259A9").build());
            sLoadedAds.add(adView);
        } catch (Exception e) {
            Log.d("AdUtils", "Ad threw exception, avoiding crash %s", e);
            Crashlytics.logException(e);
        }
        Log.d("AdUtils", "Showing ad, Ads visible: " + sLoadedAds.size());
    }

    public static void destroyView(View view) {
        AdView adView = (AdView) view;
        adView.destroy();
        sLoadedAds.remove(adView);
    }
}
