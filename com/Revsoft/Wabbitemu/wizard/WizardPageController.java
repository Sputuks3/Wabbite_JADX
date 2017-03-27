package com.Revsoft.Wabbitemu.wizard;

import android.support.annotation.NonNull;

public interface WizardPageController {
    void configureButtons(@NonNull WizardNavigationController wizardNavigationController);

    Object getControllerData();

    int getNextPage();

    int getPreviousPage();

    int getTitleId();

    boolean hasNextPage();

    boolean hasPreviousPage();

    boolean isFinalPage();

    void onHiding();

    void onShowing(Object obj);
}
