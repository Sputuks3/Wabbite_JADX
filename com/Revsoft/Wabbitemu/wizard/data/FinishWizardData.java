package com.Revsoft.Wabbitemu.wizard.data;

import com.Revsoft.Wabbitemu.calc.CalcModel;
import javax.annotation.Nullable;

public class FinishWizardData {
    private final CalcModel mCalcModel;
    private final boolean mNeedsDownload;
    private String mOsDownloadUrl;
    private final String mStringValue;

    public FinishWizardData(CalcModel calcModel) {
        this.mCalcModel = calcModel;
        this.mStringValue = null;
        this.mNeedsDownload = false;
    }

    public FinishWizardData(String filePath) {
        this.mCalcModel = CalcModel.NO_CALC;
        this.mStringValue = filePath;
        this.mNeedsDownload = false;
    }

    public FinishWizardData(CalcModel calcModel, String downloadCode, boolean needsDownload) {
        this.mCalcModel = calcModel;
        this.mOsDownloadUrl = null;
        this.mStringValue = downloadCode;
        this.mNeedsDownload = needsDownload;
    }

    public FinishWizardData(CalcModel calcModel, String osDownloadUrl, String downloadCode) {
        this.mCalcModel = calcModel;
        this.mOsDownloadUrl = osDownloadUrl;
        this.mStringValue = downloadCode;
        this.mNeedsDownload = true;
    }

    public boolean shouldDownloadOs() {
        return this.mNeedsDownload;
    }

    @Nullable
    public String getDownloadCode() {
        if (this.mNeedsDownload) {
            return this.mStringValue;
        }
        throw new IllegalArgumentException("Cannot get download code for non download");
    }

    @Nullable
    public String getOsDownloadUrl() {
        if (this.mNeedsDownload) {
            return this.mOsDownloadUrl;
        }
        throw new IllegalArgumentException("Cannot get download url for non download");
    }

    @Nullable
    public String getFilePath() {
        return this.mStringValue;
    }

    public CalcModel getCalcModel() {
        return this.mCalcModel;
    }
}
