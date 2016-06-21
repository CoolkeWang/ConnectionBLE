##描述
　　安卓4.3(API 18)为BLE的核心功能提供平台支持和API，App可以利用它来发现设备、查询服务和读写特性。相比传统的蓝牙，BLE更显著的特点是低功耗。这一优点使android App可以与具有低功耗要求的BLE设备通信，如近距离传感器、心脏速率监视器、健身设备等。
　　
##关键术语和概念

　　Attribute Protocol（ATT）—GATT在ATT协议基础上建立，也被称为GATT/ATT。ATT对在BLE设备上运行进行了优化，为此，它使用了尽可能少的字节。每个属性通过一个唯一的的统一标识符（UUID）来标识，每个String类型UUID使用128 bit标准格式。属性通过ATT被格式化为characteristics和services。
　　
　　Characteristic 一个characteristic包括一个单一变量和0-n个用来描述characteristic变量的descriptor，characteristic可以被认为是一个类型，类似于类。

　　Service service是characteristic的集合。例如，你可能有一个叫“Heart Rate Monitor(心率监测仪)”的service，它包括了很多characteristics，如“heart rate measurement(心率测量)”等。你可以在bluetooth.org 找到一个目前支持的基于GATT的配置文件和服务列表。

##角色和责任
　　中央 VS 外围设备。 适用于BLE连接本身。中央设备扫描，寻找广播；外围设备发出广播。
　　GATT 服务端 VS GATT 客户端。决定了两个设备在建立连接后如何互相交流。
　　
　　为了方便理解，想象你有一个Android手机和一个用于活动跟踪BLE设备，手机支持中央角色，活动跟踪器支持外围（为了建立BLE连接你需要注意两件事，只支持外围设备的两方或者只支持中央设备的两方不能互相通信）。

　　当手机和运动追踪器建立连接后，他们开始向另一方传输GATT数据。哪一方作为服务器取决于他们传输数据的种类。例如，如果运动追踪器想向手机报告传感器数据，运动追踪器是服务端。如果运动追踪器更新来自手机的数据，手机会作为服务端。
　　
##BLE权限
　　为了在app中使用蓝牙功能，必须声明蓝牙权限BLUETOOTH。利用这个权限去执行蓝牙通信，例如请求连接、接受连接、和传输数据。

　　如果想让你的app启动设备发现或操纵蓝牙设置，必须声明BLUETOOTH_ADMIN权限。当然如果你使用BLUETOOTH_ADMIN权限，你也必须声明BLUETOOTH权限。
```Java
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```
　　如果想声明你的app只为具有BLE的设备提供操作，在manifest文件中包括：
```Java
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
```
　　如果想让你的app提供给那些不支持BLE的设备，需要在manifest中包括上面代码并设置required="false"，然后在运行时可以通过使用PackageManager.hasSystemFeature()确定BLE的可用性。
```Java
// 使用此检查确定BLE是否支持在设备上，然后你可以有选择性禁用BLE相关的功能
if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
    finish();
}
```

##设置BLE
　　在你使用BLE通讯之前,你需要确认设备是否支持BLE，如果支持，确认已经启用。
　　如果不支持BLE，那么你应该适当地禁用部分BLE功能。如果支持BLE但被禁用，你可以无需离开应用程序而要求用户启动蓝牙.
###获取 BluetoothAdapter
```Java
//获取蓝牙管理器以及适配器
bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
bluetoothAdapter = bluetoothManager.getAdapter();
```
###开启蓝牙
```Java
// 确保蓝牙在设备上可以开启
if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
   Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
   startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
}
```

##发现BLE设备
　　为了发现BLE设备，使用startLeScan())方法。这个方法需要一个参数BluetoothAdapter.LeScanCallback。你必须实现它的回调函数，那就是返回的扫描结果。因为扫描非常消耗电量，你应当遵守以下准则：

　　只要找到所需的设备，停止扫描。
　　不要在循环里扫描，并且对扫描设置时间限制。以前可用的设备可能已经移出范围，继续扫描消耗电池电量。
　　注意：只能扫描BLE设备或者扫描传统蓝牙设备，不能同时扫描BLE和传统蓝牙设备。
```Java
private void scanLeDevice(final boolean enable) {
    if (enable) {
       // 经过预定扫描期后停止扫描
       mHandler.postDelayed(new Runnable() {
           @Override
           public void run() {
               mScanning = false;
               bluetoothAdapter.stopLeScan(mLeScanCallback);
           }
       }, SCAN_PERIOD);

       mScanning = true;
       bluetoothAdapter.startLeScan(mLeScanCallback);
    } else {
       mScanning = false;
       bluetoothAdapter.stopLeScan(mLeScanCallback);
    }
}
//扫描设备的回调
private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi,byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int i;
                        for (i = 0; i < deviceList.size(); i++) {
                            if (-1 != deviceList.get(i).get("").indexOf(device.getAddress())) {
                                break;
                            }
                        }
                        if (i >= deviceList.size()) {
                            Map<String, String> item = new HashMap<String, String>();
                            //获取设备名
                            item.put(BLUETOOTH_NAME, "设备名:" + device.getName());
                            //获取设备类型
                            switch (device.getType()) {
                                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                                    item.put(BLUETOOTH_TYPE, "设备类型:" + "BR/EDR devices");
                                    break;
                                case BluetoothDevice.DEVICE_TYPE_DUAL:
                                    item.put(BLUETOOTH_TYPE, "设备类型:" + "Dual Mode - BR/EDR/LE");
                                    break;
                                case BluetoothDevice.DEVICE_TYPE_LE:
                                    item.put(BLUETOOTH_TYPE, "设备类型:" + "Low Energy - LE-only");
                                    break;
                                case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                                    item.put(BLUETOOTH_TYPE, "设备类型:" + "Unknown");
                                    break;
                            }
                            //获取设备地址
                            item.put(BLUETOOTH_ADDR, "设备地址:" + device.getAddress());
                            //获取设备UUID
                            StringBuffer stringBuffer = new StringBuffer();
                            ParcelUuid[] uuids = device.getUuids();
                            if (uuids != null) {
                                for (ParcelUuid uuid : uuids) {
                                    stringBuffer.append(uuid.toString() + "\n");
                                }
                            }
                            item.put(BLUETOOTH_UUIDS, "UUID:" + stringBuffer.toString());
                            //更新设备列表
                            deviceList.add(item);
                            simpleAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        };
```
　　如果想要扫描指定的外围设备，可以改为调用startLeScan(UUID[], BluetoothAdapter.LeScanCallback)),需要提供你的app支持的GATT services的UUID对象数组。
##连接扫描到的BLE
```Java
BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
```
　　关于mGattCallback的写法
```Java
public final static String ACTION_GATT_CONNECTED ="com.example.bluetooth.le.ACTION_GATT_CONNECTED";
public final static String ACTION_GATT_DISCONNECTED ="com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
public final static String ACTION_GATT_SERVICES_DISCOVERED ="com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
public final static String ACTION_DATA_AVAILABLE ="com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

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
    public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
        KLog.d();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            EventBus.getDefault().post(new DataBean(ACTION_DATA_AVAILABLE,characteristic));
        }
    }
    //特征写入
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        KLog.d("OnCharacteristicWrite");
    }

    //远程设备的特性和描述符已被更新，即已发现的新服务。
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            KLog.d("onServicesDiscovered received: " + status);
            //查找服务
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
	    //读取到数据的时候将数据传递给处理的地方
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
```
　　查找服务
```Java
public final static UUID UUID_NOTIFY =UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
public final static UUID UUID_SERVICE =UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

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
```

##接受信息及数据
```Java
//接受数据
@Subscribe(threadMode = ThreadMode.MAIN)
public void handlerDataForService(DataBean dataBean) {
    //连接成功
    if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(dataBean.getAction())) {
        KLog.d("等待...");
    }//断开连接
    else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(dataBean.getAction())) {
        mConnected = false;
        getSupportActionBar().setTitle(bluetooth_name + "没有连接");
    }//可以开始干活了
    else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(dataBean.getAction())) {
        mConnected = true;
        getSupportActionBar().setTitle(bluetooth_name + "已连接");
    }//收到数据
    else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(dataBean.getAction())) {
        KLog.d("接受数据");
        String string = null;
        byte[] bytes = dataBean.getCharacteristic().getValue();
        if (bytes != null && bytes.length > 0) {
            try {
                string = new String(bytes,"gbk");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (string != null) {
            if (tvContent.length()>500){
                tvContent.setText("");
            }
            tvContent.append("from BLE : "+string+"\n");
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }
}
```

##向BLE写数据
```Java
public BluetoothGattCharacteristic mNotifyCharacteristic;
//向BLE发送数据
public void writeValue(String strValue) {
    try {
        mNotifyCharacteristic.setValue(strValue.getBytes("gbk"));
    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
    }
    mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
}
```

源码地址:https://github.com/CoolkeWang/ConnectionBLE<br/>
博客地址:http://blog.csdn.net/q531934288/article/details/51724238<br/>
本文参考:http://www.jianshu.com/p/bc408af3dd92<br/>
如有侵权问题请联系作者,及时删改<br/>
