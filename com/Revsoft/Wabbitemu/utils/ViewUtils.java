package com.Revsoft.Wabbitemu.utils;

import android.app.Activity;
import android.view.View;

public class ViewUtils {
    public static <T extends View> T findViewById(Activity activity, int resId, Class<T> clazz) {
        View foundView = activity.findViewById(resId);
        if (foundView == null) {
            throw new IllegalStateException("Unable to find view " + resId);
        } else if (clazz.isInstance(foundView)) {
            return (View) clazz.cast(foundView);
        } else {
            throw new IllegalStateException("Cannot cast view to " + clazz.getSimpleName());
        }
    }

    public static <T extends View> T findViewById(View view, int resId, Class<T> clazz) {
        View foundView = view.findViewById(resId);
        if (foundView == null) {
            throw new IllegalStateException("Unable to find view " + resId);
        } else if (clazz.isInstance(foundView)) {
            return (View) clazz.cast(foundView);
        } else {
            throw new IllegalStateException("Cannot cast view to " + clazz.getSimpleName());
        }
    }
}
