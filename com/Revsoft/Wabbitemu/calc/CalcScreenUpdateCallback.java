package com.Revsoft.Wabbitemu.calc;

import java.nio.IntBuffer;

public interface CalcScreenUpdateCallback {
    IntBuffer getScreenBuffer();

    void onUpdateScreen();
}
