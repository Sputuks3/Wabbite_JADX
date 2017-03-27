package com.Revsoft.Wabbitemu.wizard.controller;

import android.support.annotation.NonNull;
import android.widget.Spinner;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.calc.CalcModel;
import com.Revsoft.Wabbitemu.utils.SpinnerDropDownAdapter;
import com.Revsoft.Wabbitemu.wizard.WizardNavigationController;
import com.Revsoft.Wabbitemu.wizard.WizardPageController;
import com.Revsoft.Wabbitemu.wizard.data.FinishWizardData;
import com.Revsoft.Wabbitemu.wizard.data.OSDownloadData;
import com.Revsoft.Wabbitemu.wizard.view.OsPageView;
import java.util.ArrayList;
import java.util.List;

public class OsPageController implements WizardPageController {
    private CalcModel mCalcModel;
    private final OsPageView mView;

    public OsPageController(@NonNull OsPageView osPageView) {
        this.mView = osPageView;
    }

    public void configureButtons(@NonNull WizardNavigationController navController) {
        if (isFinalPage()) {
            navController.setFinishButton();
        } else {
            navController.setNextButton();
        }
    }

    public boolean hasPreviousPage() {
        return true;
    }

    public boolean hasNextPage() {
        return !isFinalPage();
    }

    public boolean isFinalPage() {
        return this.mView.getSelectedRadioId() == R.id.downloadOsRadio && this.mCalcModel != CalcModel.TI_84PCSE;
    }

    public int getNextPage() {
        if (!isFinalPage()) {
            return this.mCalcModel == CalcModel.TI_84PCSE ? R.id.os_download_page : R.id.browse_os_page;
        } else {
            throw new IllegalStateException("No next page");
        }
    }

    public int getPreviousPage() {
        return R.id.model_page;
    }

    public void onHiding() {
    }

    public void onShowing(Object previousData) {
        List<String> items = new ArrayList();
        this.mCalcModel = ((OSDownloadData) previousData).mCalcModel;
        switch (this.mCalcModel) {
            case TI_73:
                items.add("1.91");
                break;
            case TI_83P:
            case TI_83PSE:
                items.add("1.19");
                break;
            case TI_84P:
            case TI_84PSE:
                items.add("2.55 MP");
                items.add("2.43");
                break;
            case TI_84PCSE:
                items.add("4.0");
                break;
            default:
                throw new IllegalStateException("Invalid calc model");
        }
        Spinner spinner = this.mView.getSpinner();
        spinner.setAdapter(new SpinnerDropDownAdapter(spinner.getContext(), items));
        spinner.setSelection(0);
    }

    public int getTitleId() {
        return R.string.osSelectionTitle;
    }

    public Object getControllerData() {
        return isFinalPage() ? new FinishWizardData(this.mCalcModel, null, true) : this.mCalcModel;
    }
}
