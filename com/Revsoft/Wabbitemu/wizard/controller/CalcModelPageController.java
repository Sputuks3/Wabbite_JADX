package com.Revsoft.Wabbitemu.wizard.controller;

import android.support.annotation.NonNull;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.view.ModelPageView;

public class CalcModelPageController implements WizardPageController {
    private final ModelPageView mView;

    public CalcModelPageController(ModelPageView view) {
        this.mView = view;
    }

    public void configureButtons(@NonNull WizardNavigationController navController) {
    }

    public boolean hasPreviousPage() {
        return true;
    }

    public boolean hasNextPage() {
        return true;
    }

    public boolean isFinalPage() {
        return false;
    }

    public int getNextPage() {
        return R.id.choose_os_page;
    }

    public int getPreviousPage() {
        return R.id.landing_page;
    }

    public void onHiding() {
    }

    public void onShowing(Object previousData) {
    }

    public int getTitleId() {
        return R.string.calculatorTypeTitle;
    }

    public Object getControllerData() {
        switch (this.mView.getSelectedRadioId()) {
            case R.id.ti83pRadio:
                return CalcModel.TI_83P;
            case R.id.ti73Radio:
                return CalcModel.TI_73;
            case R.id.ti83pseRadio:
                return CalcModel.TI_83PSE;
            case R.id.ti84pRadio:
                return CalcModel.TI_84P;
            case R.id.ti84pseRadio:
                return CalcModel.TI_84PSE;
            case R.id.ti84pcseRadio:
                return CalcModel.TI_84PCSE;
            default:
                throw new IllegalStateException("Invalid radio id");
        }
    }
}
