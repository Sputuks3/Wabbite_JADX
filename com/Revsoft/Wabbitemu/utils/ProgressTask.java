package com.Revsoft.Wabbitemu.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

public abstract class ProgressTask extends AsyncTask<Void, Void, Boolean> {
    private final Context mContext;
    private final String mDescriptionString;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final boolean mIsCancelable;
    private ProgressDialog mProgress;
    private Runnable mProgressRunnable;

    public ProgressTask(Context context, String descriptionString, boolean isCancelable) {
        this.mContext = context;
        this.mDescriptionString = descriptionString;
        this.mIsCancelable = isCancelable;
    }

    protected void onPreExecute() {
        super.onPreExecute();
        this.mProgressRunnable = new Runnable() {
            public void run() {
                ProgressTask.this.mProgress = new ProgressDialog(ProgressTask.this.mContext);
                ProgressTask.this.mProgress.setTitle("Loading");
                ProgressTask.this.mProgress.setMessage(ProgressTask.this.mDescriptionString);
                ProgressTask.this.mProgress.setCancelable(ProgressTask.this.mIsCancelable);
                ProgressTask.this.mProgress.show();
            }
        };
        this.mHandler.postDelayed(this.mProgressRunnable, 500);
    }

    protected void onPostExecute(Boolean arg) {
        super.onPostExecute(arg);
        this.mHandler.removeCallbacks(this.mProgressRunnable);
        if (this.mProgress != null && this.mProgress.isShowing()) {
            this.mProgress.dismiss();
        }
    }

    protected Context getContext() {
        return this.mContext;
    }
}
