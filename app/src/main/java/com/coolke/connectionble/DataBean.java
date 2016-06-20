package com.coolke.connectionble;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Administrator on 2016/6/20.
 */
public class DataBean {
    private String action;
    private BluetoothGattCharacteristic characteristic;

    public DataBean(String action) {
        this.action = action;
    }

    public DataBean(String action, BluetoothGattCharacteristic characteristic) {
        this.action = action;
        this.characteristic = characteristic;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }
}
