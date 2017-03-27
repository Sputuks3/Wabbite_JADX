package com.Revsoft.Wabbitemu.utils;

public class KeyMapping {
    int bit;
    int group;
    int key;

    public KeyMapping(int key, int group, int bit) {
        this.key = key;
        this.group = group;
        this.bit = bit;
    }

    public int getKey() {
        return this.key;
    }

    public int getGroup() {
        return this.group;
    }

    public int getBit() {
        return this.bit;
    }
}
