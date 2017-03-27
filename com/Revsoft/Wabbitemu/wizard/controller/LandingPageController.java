package com.Revsoft.Wabbitemu.wizard.controller;

import android.support.annotation.NonNull;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.view.LandingPageView;

public class LandingPageController implements WizardPageController {
    private final LandingPageView mView;

    public LandingPageController(@NonNull LandingPageView view) {
        this.mView = view;
    }

    public void configureButtons(@NonNull WizardNavigationController navController) {
        navController.hideBackButton();
    }

    public boolean hasPreviousPage() {
        return false;
    }

    public boolean hasNextPage() {
        return true;
    }

    public boolean isFinalPage() {
        return false;
    }

    public int getNextPage() {
        switch (this.mView.getSelectedRadioId()) {
            case R.id.browseRomRadio:
                return R.id.browse_rom_page;
            case R.id.createWizardRadio:
                return R.id.model_page;
            default:
                throw new IllegalStateException("Invalid radio id");
        }
    }

    public int getPreviousPage() {
        throw new IllegalStateException("No previous page");
    }

    public void onHiding() {
    }

    public void onShowing(Object previousData) {
    }

    public int getTitleId() {
        return R.string.gettingStartedTitle;
    }

    public Object getControllerData() {
        return null;
    }
}
