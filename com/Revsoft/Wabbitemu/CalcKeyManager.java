package com.Revsoft.Wabbitemu;

import com.Revsoft.Wabbitemu.calc.CalculatorManager;
import com.Revsoft.Wabbitemu.utils.KeyMapping;
import java.util.ArrayList;
import javax.annotation.Nullable;

public class CalcKeyManager {
    private static final CalcKeyManager INSTANCE = new CalcKeyManager();
    private static final KeyMapping[] KEY_MAPPINGS = new KeyMapping[]{new KeyMapping(20, 0, 0), new KeyMapping(21, 0, 1), new KeyMapping(22, 0, 2), new KeyMapping(19, 0, 3), new KeyMapping(66, 1, 0), new KeyMapping(77, 5, 0), new KeyMapping(29, 5, 6), new KeyMapping(30, 4, 6), new KeyMapping(31, 3, 6), new KeyMapping(32, 5, 5), new KeyMapping(33, 4, 5), new KeyMapping(34, 3, 5), new KeyMapping(35, 2, 5), new KeyMapping(36, 1, 5), new KeyMapping(37, 5, 4), new KeyMapping(38, 4, 4), new KeyMapping(39, 3, 4), new KeyMapping(40, 2, 4), new KeyMapping(41, 1, 4), new KeyMapping(42, 5, 3), new KeyMapping(43, 4, 3), new KeyMapping(44, 3, 3), new KeyMapping(45, 2, 3), new KeyMapping(46, 1, 3), new KeyMapping(47, 5, 2), new KeyMapping(48, 4, 2), new KeyMapping(49, 3, 2), new KeyMapping(50, 2, 2), new KeyMapping(51, 1, 2), new KeyMapping(52, 5, 1), new KeyMapping(53, 4, 1), new KeyMapping(54, 3, 1), new KeyMapping(62, 4, 0), new KeyMapping(7, 4, 0), new KeyMapping(8, 4, 1), new KeyMapping(9, 3, 1), new KeyMapping(10, 2, 1), new KeyMapping(11, 4, 2), new KeyMapping(12, 3, 2), new KeyMapping(13, 2, 2), new KeyMapping(14, 4, 3), new KeyMapping(15, 3, 3), new KeyMapping(16, 2, 3), new KeyMapping(56, 3, 0), new KeyMapping(55, 4, 4), new KeyMapping(81, 1, 1), new KeyMapping(69, 1, 2), new KeyMapping(17, 2, 3), new KeyMapping(76, 1, 4), new KeyMapping(71, 3, 4), new KeyMapping(72, 2, 4), new KeyMapping(59, 6, 5), new KeyMapping(60, 1, 6), new KeyMapping(57, 5, 7), new KeyMapping(70, 4, 7)};
    private final CalculatorManager mCalculatorManager = CalculatorManager.getInstance();
    private final ArrayList<KeyMapping> mKeysDown = new ArrayList();

    public static CalcKeyManager getInstance() {
        return INSTANCE;
    }

    public void doKeyDown(int id, int group, int bit) {
        this.mCalculatorManager.pressKey(group, bit);
        this.mKeysDown.add(new KeyMapping(id, group, bit));
    }

    public boolean doKeyDownKeyCode(int keyCode) {
        KeyMapping mapping = getKeyMapping(keyCode);
        if (mapping == null) {
            return false;
        }
        doKeyDown(keyCode, mapping.getGroup(), mapping.getBit());
        return true;
    }

    public void doKeyUp(int id) {
        KeyMapping mapping = null;
        for (int i = 0; i < this.mKeysDown.size(); i++) {
            KeyMapping possibleMapping = (KeyMapping) this.mKeysDown.get(i);
            if (possibleMapping != null && possibleMapping.getKey() == id) {
                mapping = (KeyMapping) this.mKeysDown.get(i);
            }
        }
        if (mapping != null) {
            this.mCalculatorManager.releaseKey(mapping.getGroup(), mapping.getBit());
            this.mKeysDown.remove(mapping);
        }
    }

    public boolean doKeyUpKeyCode(int keyCode) {
        if (getKeyMapping(keyCode) == null) {
            return false;
        }
        doKeyUp(keyCode);
        return true;
    }

    @Nullable
    private static KeyMapping getKeyMapping(int keyCode) {
        for (KeyMapping mapping : KEY_MAPPINGS) {
            if (mapping.getKey() == keyCode) {
                return mapping;
            }
        }
        return null;
    }
}
