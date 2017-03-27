package com.Revsoft.Wabbitemu.wizard;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.ViewUtils;

public class WizardNavigationController {
    private final Button mBackButton;
    private final Button mNextButton;
    private final WizardController mWizardController;

    public WizardNavigationController(@NonNull WizardController wizardController, @NonNull ViewGroup navContainer) {
        this.mWizardController = wizardController;
        this.mNextButton = (Button) ViewUtils.findViewById((View) navContainer, (int) R.id.nextButton, Button.class);
        this.mBackButton = (Button) ViewUtils.findViewById((View) navContainer, (int) R.id.backButton, Button.class);
        this.mNextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WizardNavigationController.this.mWizardController.moveNextPage();
            }
        });
        this.mBackButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WizardNavigationController.this.mWizardController.movePreviousPage();
            }
        });
    }

    public void onPageLaunched(@NonNull WizardPageController pageController) {
        setButtonVisibility(this.mBackButton, 0);
        setButtonVisibility(this.mNextButton, 0);
        setNextButton();
        pageController.configureButtons(this);
    }

    public void hideNextButton() {
        setButtonVisibility(this.mNextButton, 8);
    }

    public void hideBackButton() {
        setButtonVisibility(this.mBackButton, 8);
    }

    public void finishWizard() {
        this.mWizardController.moveNextPage();
    }

    public void movePreviousPage() {
        this.mWizardController.movePreviousPage();
    }

    public void setNextButton() {
        this.mNextButton.setText(R.string.next);
    }

    public void setFinishButton() {
        this.mNextButton.setText(R.string.finish);
    }

    private void setButtonVisibility(View button, int visibility) {
        button.setVisibility(visibility);
    }
}
