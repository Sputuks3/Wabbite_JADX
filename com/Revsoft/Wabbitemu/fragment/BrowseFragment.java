package com.Revsoft.Wabbitemu.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.AdUtils;
import com.Revsoft.Wabbitemu.utils.FileUtils;
import com.Revsoft.Wabbitemu.utils.IntentConstants;
import com.Revsoft.Wabbitemu.utils.OnBrowseItemSelected;
import com.Revsoft.Wabbitemu.utils.ViewUtils;

public class BrowseFragment extends Fragment {
    public static final int REQUEST_CODE = 20;
    private View mAdView;
    private OnBrowseItemSelected mBrowseCallback;
    private String mExtensionsRegex;
    private final FileUtils mFileUtils = FileUtils.getInstance();
    private ListView mListView;
    private AsyncTask<Void, Void, ArrayAdapter<String>> mSearchTask;

    public void setCallback(@Nullable OnBrowseItemSelected browseCallback) {
        this.mBrowseCallback = browseCallback;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browse, container, false);
        if (VERSION.SDK_INT >= 23) {
            requestReadPermissions();
        }
        if (getArguments() != null) {
            this.mExtensionsRegex = getArguments().getString(IntentConstants.EXTENSION_EXTRA_REGEX);
            this.mListView = (ListView) ViewUtils.findViewById(view, (int) R.id.browseView, ListView.class);
            this.mListView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    String filePath = (String) BrowseFragment.this.mListView.getItemAtPosition(position);
                    if (BrowseFragment.this.mBrowseCallback != null) {
                        BrowseFragment.this.mBrowseCallback.onBrowseItemSelected(filePath);
                    }
                }
            });
            startSearch(view, this.mExtensionsRegex);
            this.mAdView = view.findViewById(R.id.adView);
            AdUtils.loadAd(this.mAdView);
        }
        return view;
    }

    @TargetApi(23)
    private void requestReadPermissions() {
        if (getActivity().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 20);
            View view = getView();
            if (view != null) {
                startSearch(view, this.mExtensionsRegex);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 20) {
            this.mFileUtils.invalidateFiles();
            if (getView() != null) {
                startSearch(getView(), this.mExtensionsRegex);
            }
        }
    }

    private void startSearch(final View view, final String extensionsRegex) {
        this.mSearchTask = new AsyncTask<Void, Void, ArrayAdapter<String>>() {
            private Context mContext;
            private View mLoadingSpinner;

            protected void onPreExecute() {
                this.mContext = BrowseFragment.this.getActivity();
                this.mLoadingSpinner = ViewUtils.findViewById(view, (int) R.id.browseLoadingSpinner, View.class);
                this.mLoadingSpinner.setVisibility(0);
            }

            protected ArrayAdapter<String> doInBackground(Void... params) {
                return new ArrayAdapter(this.mContext, 17367043, BrowseFragment.this.mFileUtils.getValidFiles(extensionsRegex));
            }

            protected void onPostExecute(ArrayAdapter<String> adapter) {
                this.mLoadingSpinner.setVisibility(8);
                BrowseFragment.this.mListView.setAdapter(adapter);
                BrowseFragment.this.mSearchTask = null;
            }
        };
        if (extensionsRegex != null) {
            this.mSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mSearchTask != null) {
            this.mSearchTask.cancel(true);
        }
        if (this.mAdView != null) {
            AdUtils.destroyView(this.mAdView);
        }
    }
}
