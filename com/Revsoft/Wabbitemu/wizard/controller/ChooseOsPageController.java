package com.Revsoft.Wabbitemu.wizard.controller;

import android.os.AsyncTask;
import android.util.Log;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.activity.WabbitemuActivity;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.extract.MsiDatabase;
import com.Revsoft.Wabbitemu.extract.MsiDatabase.CItem;
import com.Revsoft.Wabbitemu.extract.MsiHandler;
import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.data.FinishWizardData;
import com.Revsoft.Wabbitemu.wizard.data.OSDownloadData;
import com.Revsoft.Wabbitemu.wizard.view.ChooseOsPageView;
import dorkbox.cabParser.CabException;
import dorkbox.cabParser.CabParser;
import dorkbox.cabParser.CabStreamSaver;
import dorkbox.cabParser.structure.CabFileEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.CookieManager;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ChooseOsPageController implements WizardPageController {
    private String mAuthUrl;
    private CalcModel mCalcModel;
    private String mDownloadedOsPath;
    private boolean mHasNextPage;
    private boolean mIsFinalPage;
    private boolean mIsFinished;
    private FindOSDownloadsAsyncTask mLoadOsPageTask;
    private WizardNavigationController mNavController;
    private int mNextPage;
    private final ChooseOsPageView mView;

    private class FindOSDownloadsAsyncTask extends AsyncTask<Void, Void, NextOsAction> {
        private ByteArrayOutputStream outputStream;

        private class MyCabStreamSaver implements CabStreamSaver {
            private final Pattern mExtension;

            public MyCabStreamSaver(Pattern extension) {
                this.mExtension = extension;
            }

            public OutputStream openOutputStream(CabFileEntry cabFileEntry) {
                if (!this.mExtension.matcher(cabFileEntry.getName()).matches() || FindOSDownloadsAsyncTask.this.outputStream != null) {
                    return null;
                }
                FindOSDownloadsAsyncTask.this.outputStream = new ByteArrayOutputStream((int) cabFileEntry.getSize());
                return FindOSDownloadsAsyncTask.this.outputStream;
            }

            public void closeOutputStream(OutputStream outputStream, CabFileEntry cabFileEntry) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                    }
                }
            }

            public boolean saveReservedAreaData(byte[] bytes, int i) {
                return false;
            }
        }

        private FindOSDownloadsAsyncTask() {
        }

        protected NextOsAction doInBackground(Void... params) {
            NextOsAction osAction = tryLoadOsPage();
            if (osAction != null) {
                return osAction;
            }
            NextOsAction msiAction = tryLoadMsi();
            if (msiAction != null) {
                return msiAction;
            }
            return new NextOsAction(NextAction.ERROR, null);
        }

        private NextOsAction tryLoadMsi() {
            Pattern extensionPattern;
            String extension;
            Exception e;
            Exception e2;
            Throwable th;
            switch (ChooseOsPageController.this.mCalcModel) {
                case TI_73:
                    extensionPattern = Pattern.compile(".*\\.73u", 2);
                    extension = ".73u";
                    break;
                case TI_83P:
                case TI_83PSE:
                    extensionPattern = Pattern.compile(".*83Plus.*\\.8xu", 2);
                    extension = ".8xu";
                    break;
                case TI_84P:
                case TI_84PSE:
                    extensionPattern = Pattern.compile(".*84Plus.*\\.8xu", 2);
                    extension = ".8xu";
                    break;
                case TI_84PCSE:
                    extensionPattern = Pattern.compile(".*\\.8cu", 2);
                    extension = ".8cu";
                    break;
                default:
                    return null;
            }
            OkHttpClient connection = new Builder().cookieJar(new JavaNetCookieJar(new CookieManager())).build();
            String msiLink = tryLoadMsiPage(connection);
            if (msiLink == null) {
                return null;
            }
            RandomAccessFile randomAccessFile = null;
            try {
                Response response = connection.newCall(new Request.Builder().url(msiLink).addHeader("User-Agent", OsDownloadPageController.USER_AGENT).build()).execute();
                File msiFile = new File(WabbitemuActivity.sBestCacheDir, "msiFile.msi");
                FileOutputStream fileOutputStream = new FileOutputStream(msiFile);
                fileOutputStream.write(response.body().bytes());
                fileOutputStream.close();
                RandomAccessFile randomAccessFile2 = new RandomAccessFile(msiFile, "r");
                try {
                    MsiDatabase msiDatabase = new MsiDatabase();
                    msiDatabase.open(randomAccessFile2);
                    MsiHandler msiHandler = new MsiHandler(msiDatabase);
                    int i = 0;
                    this.outputStream = null;
                    for (CItem item : msiDatabase.Items) {
                        if (item.getRealName().endsWith(".cab")) {
                            new CabParser(new ByteArrayInputStream(msiHandler.GetStream(randomAccessFile2, item, i)), (CabStreamSaver) new MyCabStreamSaver(extensionPattern)).extractStream();
                            File osFile = new File(WabbitemuActivity.sBestCacheDir, "osFile" + extension);
                            if (this.outputStream != null) {
                                FileOutputStream fileOutputStream2 = new FileOutputStream(osFile);
                                fileOutputStream2.write(this.outputStream.toByteArray());
                                fileOutputStream2.close();
                                this.outputStream.close();
                                NextOsAction nextOsAction = new NextOsAction(NextAction.LOAD_MSI, osFile.getAbsolutePath());
                                if (randomAccessFile2 == null) {
                                    return nextOsAction;
                                }
                                try {
                                    randomAccessFile2.close();
                                    return nextOsAction;
                                } catch (IOException e3) {
                                    Log.w("Wabbitemu", "Failed to close file " + e3);
                                    return nextOsAction;
                                }
                            } else if (randomAccessFile2 == null) {
                                return null;
                            } else {
                                try {
                                    randomAccessFile2.close();
                                    return null;
                                } catch (IOException e32) {
                                    Log.w("Wabbitemu", "Failed to close file " + e32);
                                    return null;
                                }
                            }
                        }
                        i++;
                    }
                    if (randomAccessFile2 != null) {
                        try {
                            randomAccessFile2.close();
                            randomAccessFile = randomAccessFile2;
                        } catch (IOException e322) {
                            Log.w("Wabbitemu", "Failed to close file " + e322);
                            randomAccessFile = randomAccessFile2;
                        }
                    } else {
                        randomAccessFile = randomAccessFile2;
                    }
                } catch (IOException e4) {
                    e = e4;
                    randomAccessFile = randomAccessFile2;
                    e2 = e;
                    try {
                        UserActivityTracker.getInstance().reportBreadCrumb("Exception loading msi " + e2);
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (IOException e3222) {
                                Log.w("Wabbitemu", "Failed to close file " + e3222);
                            }
                        }
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (IOException e32222) {
                                Log.w("Wabbitemu", "Failed to close file " + e32222);
                            }
                        }
                        throw th;
                    }
                } catch (CabException e5) {
                    e = e5;
                    randomAccessFile = randomAccessFile2;
                    e2 = e;
                    UserActivityTracker.getInstance().reportBreadCrumb("Exception loading msi " + e2);
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                    return null;
                } catch (Throwable th3) {
                    th = th3;
                    randomAccessFile = randomAccessFile2;
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                e2 = e;
                UserActivityTracker.getInstance().reportBreadCrumb("Exception loading msi " + e2);
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                return null;
            } catch (CabException e7) {
                e = e7;
                e2 = e;
                UserActivityTracker.getInstance().reportBreadCrumb("Exception loading msi " + e2);
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                return null;
            }
            return null;
        }

        @Nullable
        private String tryLoadMsiPage(OkHttpClient connection) {
            try {
                Elements elements = Jsoup.parse(connection.newCall(new Request.Builder().url("https://epsstore.ti.com/OA_HTML/csksxvm.jsp;jsessionid=b401c39d98b4886b458efc7dd5d8327db3bc7777671e65db5db506a2e6bafa8c.e34TbNuKax4RaO0Mah0LaxaTchyRe0?jfn=ZGC7FD5432DD1749EE35764C594E5B43B3511EE256D94188614786F910B87AE331265643E242F68AFA6CE2579F26775AF7EC&lepopus=bE7LmZ2FxS3jD0l7eyTp1L9xkc&lepopus_pses=ZG6B09CB0A3807E876346D50579677E97CB0AB13CC596FF029053030558FF7479CA28505CDAD2053EE19E6BE618AC72AA757DCFBE5884A6B21&oas=eFTq1K0o_gpUMQl6PJojPw..&nSetId=130494&nBrowseCategoryId=10464&cskViewSolSourcePage=cskmbasicsrch.jsp%3FcategoryId%3D10464%26fRange%3Dnull%26fStartRow%3D0%26fSortBy%3D2%26fSortByOrder%3D1").addHeader("User-Agent", OsDownloadPageController.USER_AGENT).build()).execute().body().string()).select("#rightcol a");
                if (elements.isEmpty()) {
                    return null;
                }
                return "https://epsstore.ti.com//OA_HTML/" + ((Element) elements.iterator().next()).attr("href");
            } catch (IOException e) {
                UserActivityTracker.getInstance().reportBreadCrumb("Exception loading msi page " + e);
                return null;
            }
        }

        @Nullable
        private NextOsAction tryLoadOsPage() {
            String urlString = getOsPageUrl();
            if (urlString == null) {
                return null;
            }
            try {
                Iterator it = Jsoup.parse(new OkHttpClient().newCall(new Request.Builder().url(urlString).addHeader("User-Agent", OsDownloadPageController.USER_AGENT).build()).execute().body().string()).select(".column-downloaditem").iterator();
                while (it.hasNext()) {
                    Element element = (Element) it.next();
                    Iterator it2 = element.select("a").iterator();
                    while (it2.hasNext()) {
                        String href = ((Element) it2.next()).attr("href");
                        if (href != null && (href.toLowerCase().endsWith("8xu") || href.toLowerCase().endsWith("8cu"))) {
                            return new NextOsAction(element.classNames().contains("protected-download") ? NextAction.LOAD_AUTHENTICATED_OS_PAGE : NextAction.LOAD_UNAUTHENTICATED_OS_PAGE, href);
                        }
                    }
                }
            } catch (IOException e) {
                UserActivityTracker.getInstance().reportBreadCrumb("Failed to download os page " + e);
            }
            return null;
        }

        @Nullable
        private String getOsPageUrl() {
            switch (ChooseOsPageController.this.mCalcModel) {
                case TI_73:
                    return "https://education.ti.com/en/us/software/details/en/956CE30854A74767893104FCDF195B76/73ti73exploreroperatingsystem";
                case TI_84P:
                case TI_84PSE:
                    return "https://education.ti.com/en/us/software/details/en/B7DADA7FD4AA40CE9D7911B004B8C460/ti84plusoperatingsystem";
                default:
                    return null;
            }
        }

        protected void onPostExecute(NextOsAction action) {
            NextAction nextAction = action.mNextAction;
            switch (nextAction) {
                case LOAD_MSI:
                    ChooseOsPageController.this.mIsFinalPage = true;
                    ChooseOsPageController.this.mDownloadedOsPath = action.mData;
                    break;
                case LOAD_AUTHENTICATED_OS_PAGE:
                case LOAD_UNAUTHENTICATED_OS_PAGE:
                    ChooseOsPageController.this.mIsFinalPage = false;
                    String authUrl = action.mData;
                    ChooseOsPageController chooseOsPageController = ChooseOsPageController.this;
                    if (authUrl != null && authUrl.startsWith("/")) {
                        authUrl = "https://education.ti.com" + authUrl;
                    }
                    chooseOsPageController.mAuthUrl = authUrl;
                    ChooseOsPageController.this.mNextPage = nextAction == NextAction.LOAD_AUTHENTICATED_OS_PAGE ? R.id.os_download_page : R.id.os_page;
                    ChooseOsPageController.this.mHasNextPage = true;
                    break;
                case ERROR:
                    ChooseOsPageController.this.mNavController.hideNextButton();
                    ChooseOsPageController.this.mView.getLoadingSpinner().setVisibility(8);
                    ChooseOsPageController.this.mView.getMessage().setText(R.string.errorWebPageDownloadError);
                    break;
            }
            if (ChooseOsPageController.this.mNavController == null) {
                ChooseOsPageController.this.mIsFinished = true;
            } else {
                ChooseOsPageController.this.mNavController.finishWizard();
            }
        }
    }

    private enum NextAction {
        LOAD_MSI,
        LOAD_AUTHENTICATED_OS_PAGE,
        LOAD_UNAUTHENTICATED_OS_PAGE,
        ERROR
    }

    private static class NextOsAction {
        public final String mData;
        public final NextAction mNextAction;

        private NextOsAction(@Nonnull NextAction nextAction, @Nullable String data) {
            this.mNextAction = nextAction;
            this.mData = data;
        }
    }

    public ChooseOsPageController(@Nonnull ChooseOsPageView osPageView) {
        this.mView = osPageView;
    }

    public void configureButtons(@Nonnull WizardNavigationController navController) {
        this.mNavController = navController;
        navController.hideNextButton();
        if (this.mIsFinished) {
            this.mNavController.finishWizard();
        }
    }

    public boolean hasPreviousPage() {
        return true;
    }

    public boolean hasNextPage() {
        return this.mHasNextPage;
    }

    public boolean isFinalPage() {
        return this.mIsFinalPage;
    }

    public int getNextPage() {
        return this.mNextPage;
    }

    public int getPreviousPage() {
        return R.id.model_page;
    }

    public void onHiding() {
        if (this.mLoadOsPageTask != null) {
            this.mLoadOsPageTask.cancel(true);
            this.mLoadOsPageTask = null;
        }
    }

    public void onShowing(Object previousData) {
        this.mCalcModel = (CalcModel) previousData;
        this.mHasNextPage = false;
        this.mIsFinalPage = false;
        this.mIsFinished = false;
        this.mView.getMessage().setText(R.string.long_loading);
        this.mView.getLoadingSpinner().setVisibility(0);
        this.mLoadOsPageTask = new FindOSDownloadsAsyncTask();
        this.mLoadOsPageTask.execute(new Void[0]);
    }

    public int getTitleId() {
        return R.string.osLoadingTitle;
    }

    public Object getControllerData() {
        return this.mIsFinalPage ? new FinishWizardData(this.mCalcModel, this.mDownloadedOsPath, false) : new OSDownloadData(this.mCalcModel, this.mAuthUrl);
    }
}
