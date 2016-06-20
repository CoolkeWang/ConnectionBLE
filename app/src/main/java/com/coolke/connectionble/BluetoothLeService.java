package com.coolke.connectionble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.socks.library.KLog;

import org.greenrobot.eventbus.EventBus;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2016/6/20.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static UUID UUID_NOTIFY =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    public BluetoothGattCharacteristic mNotifyCharacteristic;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * 关闭BLE连接服务
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    //向BLE发送数据
    public void writeValue(String strValue) {
        try {
            mNotifyCharacteristic.setValue(strValue.getBytes("gbk"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //GATT客户端从GATT服务器远程连接/断开
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {//当蓝牙设备已经连接
                    EventBus.getDefault().post(new DataBean(ACTION_GATT_CONNECTED));
                    KLog.d("连接到GATT服务器");
                    KLog.d("启动发现的服务:" +
                            mBluetoothGatt.discoverServices());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//当设备无法连接
                    KLog.d("Disconnected from GATT server.");
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                    EventBus.getDefault().post(new DataBean(ACTION_GATT_DISCONNECTED));
                }
            }
        }

        //特征读取操作的结果
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            KLog.d();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                EventBus.getDefault().post(new DataBean(ACTION_DATA_AVAILABLE,characteristic));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            KLog.d("OnCharacteristicWrite");
        }

        //远程设备的特性和描述符已被更新，即已发现的新服务。
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                KLog.d("onServicesDiscovered received: " + status);
                //
                findService(gatt.getServices());
            } else {
                if (mBluetoothGatt.getDevice().getUuids() == null) {
                    KLog.d("onServicesDiscovered received: " + status);
                }
            }
        }
        //远程特征发生改变
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            EventBus.getDefault().post(new DataBean(ACTION_DATA_AVAILABLE, characteristic));
            KLog.d("OnCharacteristicWrite");
        }

        //描述符读操作
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            KLog.d("onDescriptorRead");
        }

        //描述符写操作
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            KLog.d("onDescriptorWrite");
        }

        //读取远程设备连接的RSSI
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            KLog.d("onReadRemoteRssi");
        }

        //当一个写事务已完成时调用的回调
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            KLog.d("onReliableWriteCompleted");
        }
    };

    /**
     * 查找服务
     * @param gattServices
     */
    public void findService(List<BluetoothGattService> gattServices) {
        KLog.d("GATT服务总数:" + gattServices.size());
        for (BluetoothGattService gattService : gattServices) {
            KLog.d(gattService.getUuid().toString());
            KLog.d(UUID_SERVICE.toString());
            //判断是否是指定的服务
            if (gattService.getUuid().toString().equalsIgnoreCase(UUID_SERVICE.toString())) {
                //获取参数
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                KLog.d("参数总数:" + gattCharacteristics.size());
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    //判断是否是指定的通知
                    if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(UUID_NOTIFY.toString())) {
                        KLog.d(gattCharacteristic.getUuid().toString());
                        KLog.d(UUID_NOTIFY.toString());
                        mNotifyCharacteristic = gattCharacteristic;
                        setCharacteristicNotification(gattCharacteristic, true);
                        EventBus.getDefault().post(new DataBean(ACTION_GATT_SERVICES_DISCOVERED));
                        return;
                    }
                }
            }
        }
    }

    /**
     * 启用或禁用一个给定特性的通知。
     * @param characteristic
     * @param enabled
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            KLog.d("蓝牙适配器不能初始化");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * 初始化蓝牙适配器
     * @return
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                KLog.d("无法初始化蓝牙管理器.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            KLog.d("无法获得蓝牙适配器.");
            return false;
        }
        return true;
    }

    /**
     * 连接蓝牙
     * @param address
     * @return
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            KLog.d("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            KLog.d("Device not found.  Unable to connect.");
            return false;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        KLog.d("Trying to create a new connection.");
        return true;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            KLog.d("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 读取特征
     * @param characteristic
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            KLog.d("BluetoothAdapter not initialized");
            return;
        }
        //使用此方法会调用onCharacteristicRead
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    //获取支持的GattServices列表
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;
        return mBluetoothGatt.getServices();
    }
}
