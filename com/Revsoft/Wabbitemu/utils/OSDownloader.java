package com.Revsoft.Wabbitemu.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.wizard.controller.OsDownloadPageController;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;

public class OSDownloader extends AsyncTask<Integer, Integer, Boolean> {
    private final String mAuthCode;
    private final CalcModel mCalcType;
    private final String mOsDownloadUrl;
    private final String mOsFilePath;
    private final ProgressDialog mProgressDialog;
    private final UserActivityTracker mUserActivityTracker = UserActivityTracker.getInstance();

    protected OSDownloader(Context context, String osDownloadUrl, String osFilePath, CalcModel calcType, String authCode) {
        this.mOsDownloadUrl = osDownloadUrl;
        this.mOsFilePath = osFilePath;
        this.mProgressDialog = new ProgressDialog(context);
        this.mProgressDialog.setTitle(R.string.downloadingTitle);
        this.mProgressDialog.setMessage(context.getResources().getString(R.string.downloadingOsDescription));
        this.mProgressDialog.setIndeterminate(true);
        this.mProgressDialog.setProgressStyle(1);
        this.mCalcType = calcType;
        this.mAuthCode = authCode;
        this.mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                OSDownloader.this.cancel(true);
            }
        });
        this.mProgressDialog.show();
        this.mUserActivityTracker.reportBreadCrumb("Showing OS Download dialog");
    }

    protected void onPreExecute() {
        super.onPreExecute();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Boolean doInBackground(Integer... args) {
        IOException e;
        Throwable th;
        try {
            URL url = new URL(this.mOsDownloadUrl);
            OutputStream outputStream = null;
            InputStream inputStream = null;
            Boolean valueOf;
            try {
                this.mUserActivityTracker.reportBreadCrumb("Downloading OS: %s from: %s", this.mCalcType, url);
                OkHttpClient connection = new OkHttpClient();
                Builder request = new Builder().url(this.mOsDownloadUrl).addHeader("User-Agent", OsDownloadPageController.USER_AGENT);
                if (this.mAuthCode != null) {
                    request.addHeader("Cookie", "DownloadAuthorizationToken=" + this.mAuthCode);
                }
                Response response = connection.newCall(request.build()).execute();
                inputStream = response.body().byteStream();
                OutputStream outputStream2 = new FileOutputStream(this.mOsFilePath);
                try {
                    this.mUserActivityTracker.reportBreadCrumb("OS Response code: %s", Integer.valueOf(response.code()));
                    if (response.code() != 200) {
                        valueOf = Boolean.valueOf(false);
                        if (outputStream2 == null) {
                            return valueOf;
                        }
                        try {
                            outputStream2.close();
                            inputStream.close();
                            return valueOf;
                        } catch (IOException e2) {
                            return Boolean.valueOf(false);
                        }
                    }
                    long fileLength = response.body().contentLength();
                    byte[] data = new byte[4096];
                    long total = 0;
                    while (true) {
                        int count = inputStream.read(data);
                        if (count == -1) {
                            break;
                        } else if (isCancelled()) {
                            break;
                        } else {
                            total += (long) count;
                            if (fileLength > 0) {
                                publishProgress(new Integer[]{Integer.valueOf((int) ((100 * total) / fileLength))});
                            }
                            outputStream2.write(data, 0, count);
                        }
                    }
                    if (outputStream2 != null) {
                        try {
                            outputStream2.close();
                            inputStream.close();
                        } catch (IOException e3) {
                            return Boolean.valueOf(false);
                        }
                    }
                    return Boolean.valueOf(true);
                } catch (IOException e4) {
                    e = e4;
                    outputStream = outputStream2;
                } catch (Throwable th2) {
                    th = th2;
                    outputStream = outputStream2;
                }
            } catch (IOException e5) {
                e = e5;
                try {
                    this.mUserActivityTracker.reportBreadCrumb("OS download exception %s", e);
                    valueOf = Boolean.valueOf(false);
                    if (outputStream == null) {
                        return valueOf;
                    }
                    try {
                        outputStream.close();
                        inputStream.close();
                        return valueOf;
                    } catch (IOException e6) {
                        return Boolean.valueOf(false);
                    }
                } catch (Throwable th3) {
                    th = th3;
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                            inputStream.close();
                        } catch (IOException e7) {
                            return Boolean.valueOf(false);
                        }
                    }
                    throw th;
                }
            }
        } catch (MalformedURLException e8) {
            UserActivityTracker.getInstance().reportBreadCrumb("OS download url was bad " + this.mOsDownloadUrl);
            return Boolean.FALSE;
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        this.mProgressDialog.setIndeterminate(false);
        this.mProgressDialog.setMax(100);
        this.mProgressDialog.setProgress(progress[0].intValue());
    }

    protected void onCancelled() {
        super.onCancelled();
        this.mProgressDialog.dismiss();
        this.mUserActivityTracker.reportBreadCrumb("OS Download cancelled. Hiding dialog");
    }

    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        this.mProgressDialog.dismiss();
        this.mUserActivityTracker.reportBreadCrumb("Hiding OS Download dialog. Result [%s]", result);
    }
}
