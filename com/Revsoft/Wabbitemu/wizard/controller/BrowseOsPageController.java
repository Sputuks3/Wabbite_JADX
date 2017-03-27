package com.Revsoft.Wabbitemu.wizard.controller;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.fragment.BrowseFragment;
import com.Revsoft.Wabbitemu.utils.IntentConstants;
import com.Revsoft.Wabbitemu.utils.OnBrowseItemSelected;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.data.FinishWizardData;
import com.Revsoft.Wabbitemu.wizard.view.BrowseOsPageView;

public class BrowseOsPageController implements WizardPageController {
    private final OnBrowseItemSelected mBrowseCallback = new OnBrowseItemSelected() {
        public void onBrowseItemSelected(String fileName) {
            if (BrowseOsPageController.this.mNavController != null) {
                BrowseOsPageController.this.mOsPath = fileName;
                BrowseOsPageController.this.mNavController.finishWizard();
            }
        }
    };
    private CalcModel mCalcModel;
    private final Context mContext;
    private final FragmentManager mFragmentManager;
    private WizardNavigationController mNavController;
    private String mOsPath;

    public BrowseOsPageController(@NonNull BrowseOsPageView view, @NonNull FragmentManager fragmentManager) {
        this.mContext = view.getContext();
        this.mFragmentManager = fragmentManager;
    }

    public void configureButtons(@NonNull WizardNavigationController navController) {
        this.mNavController = navController;
        navController.hideNextButton();
    }

    public boolean hasPreviousPage() {
        return true;
    }

    public boolean hasNextPage() {
        return false;
    }

    public boolean isFinalPage() {
        return true;
    }

    public int getNextPage() {
        throw new IllegalStateException("No next page");
    }

    public int getPreviousPage() {
        return R.id.os_page;
    }

    public void onHiding() {
    }

    public void onShowing(Object previousData) {
        this.mCalcModel = (CalcModel) previousData;
        launchBrowseOs();
    }

    public int getTitleId() {
        return R.string.osSelectionTitle;
    }

    public Object getControllerData() {
        return new FinishWizardData(this.mCalcModel, this.mOsPath, false);
    }

    private void launchBrowseOs() {
        String extensions;
        switch (this.mCalcModel) {
            case TI_73:
                extensions = "\\.73u";
                break;
            case TI_84PCSE:
                extensions = "\\.8cu";
                break;
            case TI_83P:
            case TI_83PSE:
            case TI_84P:
            case TI_84PSE:
                extensions = "\\.8xu";
                break;
            default:
                throw new IllegalStateException("Invalid calc model");
        }
        Bundle setupBundle = new Bundle();
        setupBundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, extensions);
        setupBundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, this.mContext.getResources().getString(R.string.browseOSDescription));
        BrowseFragment fragInfo = new BrowseFragment();
        fragInfo.setCallback(this.mBrowseCallback);
        fragInfo.setArguments(setupBundle);
        FragmentTransaction transaction = this.mFragmentManager.beginTransaction();
        transaction.replace(R.id.browse_os_page, fragInfo);
        transaction.commit();
    }
}
