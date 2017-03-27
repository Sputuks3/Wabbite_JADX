package com.Revsoft.Wabbitemu.wizard.controller;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.fragment.BrowseFragment;
import com.Revsoft.Wabbitemu.utils.IntentConstants;
import com.Revsoft.Wabbitemu.utils.OnBrowseItemSelected;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.data.FinishWizardData;
import com.Revsoft.Wabbitemu.wizard.view.BrowseRomPageView;

public class BrowseRomPageController implements WizardPageController {
    private final OnBrowseItemSelected mBrowseCallback = new OnBrowseItemSelected() {
        public void onBrowseItemSelected(String fileName) {
            if (BrowseRomPageController.this.mNavController != null) {
                BrowseRomPageController.this.mSelectedFileName = fileName;
                BrowseRomPageController.this.mNavController.finishWizard();
            }
        }
    };
    private final Context mContext;
    private final FragmentManager mFragmentManager;
    private WizardNavigationController mNavController;
    private String mSelectedFileName;

    public BrowseRomPageController(@NonNull BrowseRomPageView view, @NonNull FragmentManager fragmentManager) {
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
        return R.id.landing_page;
    }

    public void onHiding() {
    }

    public void onShowing(Object previousData) {
        launchBrowseRom();
    }

    public int getTitleId() {
        return R.string.browseRomTitle;
    }

    public Object getControllerData() {
        if (this.mSelectedFileName == null) {
            return null;
        }
        return new FinishWizardData(this.mSelectedFileName);
    }

    private void launchBrowseRom() {
        Bundle setupBundle = new Bundle();
        setupBundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, "\\.(rom|sav)");
        setupBundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, this.mContext.getResources().getString(R.string.browseRomDescription));
        BrowseFragment fragInfo = new BrowseFragment();
        fragInfo.setCallback(this.mBrowseCallback);
        fragInfo.setArguments(setupBundle);
        FragmentTransaction transaction = this.mFragmentManager.beginTransaction();
        transaction.replace(R.id.browse_rom_page, fragInfo);
        transaction.commit();
    }
}
