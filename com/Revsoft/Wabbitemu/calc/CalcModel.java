package com.Revsoft.Wabbitemu.calc;

import javax.annotation.Nonnull;

public enum CalcModel {
    NO_CALC(-1),
    TI_81(0),
    TI_82(1),
    TI_83(2),
    TI_85(3),
    TI_86(4),
    TI_73(5),
    TI_83P(6),
    TI_83PSE(7),
    TI_84P(8),
    TI_84PSE(9),
    TI_84PCSE(10);
    
    private final int mCalcInterfaceModel;

    private CalcModel(int calcInterfaceModel) {
        this.mCalcInterfaceModel = calcInterfaceModel;
    }

    @Nonnull
    public static CalcModel fromModel(int model) {
        for (CalcModel calcModel : values()) {
            if (calcModel.mCalcInterfaceModel == model) {
                return calcModel;
            }
        }
        return NO_CALC;
    }

    public int getModelInt() {
        return this.mCalcInterfaceModel;
    }
}
