package com.Revsoft.Wabbitemu.utils;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import com.Revsoft.Wabbitemu.R;
import com.crashlytics.android.Crashlytics;

public class ErrorUtils {
    public static void showErrorDialog(Context context, int errorMessage) {
        showErrorDialog(context, errorMessage, null);
    }

    public static void showErrorDialog(Context context, int errorMessage, final OnClickListener onClickListener) {
        String error = context.getResources().getString(errorMessage);
        Crashlytics.log(6, context.getClass().getName(), error);
        Crashlytics.logException(new Exception());
        new Builder(context).setTitle(R.string.errorTitle).setMessage(error).setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (onClickListener != null) {
                    onClickListener.onClick(dialog, which);
                }
            }
        }).create().show();
    }
}
