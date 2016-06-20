package com.coolke.connectionble;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.lv)
    ListView lv;
    private SimpleAdapter simpleAdapter;
    private ArrayList<Map<String, String>> deviceList = new ArrayList<>();


    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 0x1;
    private final static String BLUETOOTH_NAME = "name";
    private final static String BLUETOOTH_TYPE = "type";
    private final static String BLUETOOTH_ADDR = "addr";
    private final static String BLUETOOTH_UUIDS = "uuids";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //判断手机是否支持BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        ButterKnife.bind(this);

        //获取蓝牙管理器以及适配器
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        //判断是否支持蓝牙
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 确保蓝牙在设备上可以开启
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //初始化listview
        simpleAdapter = new SimpleAdapter(getApplicationContext(),
                deviceList, R.layout.item,
                new String[]{BLUETOOTH_NAME, BLUETOOTH_TYPE, BLUETOOTH_ADDR, BLUETOOTH_UUIDS},
                new int[]{R.id.tv_name, R.id.tv_type, R.id.tv_addr, R.id.tv_uuid});

        lv.setAdapter(simpleAdapter);
        lv.setOnItemClickListener(lvOnItemClickListener);
    }

    //ListView元素点击
    private AdapterView.OnItemClickListener lvOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            KLog.d(position);

            String name = deviceList.get(position).get(BLUETOOTH_NAME);
            String addr = deviceList.get(position).get(BLUETOOTH_ADDR);

            name = name.substring(name.indexOf(":")+1);
            addr = addr.substring(addr.indexOf(":")+1);
            //启动连接Activity,并停止扫描
            ConnectionActivity.startActivity(MainActivity.this,name,addr);
            if (mScanning){
                scanLeDevice(!mScanning);
            }
        }
    };

    private BluetoothGatt mBluetoothGatt;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_search:
                scanLeDevice(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean mScanning;
    // 10秒后停止寻找.
    private static final long SCAN_PERIOD = 10000;
    private Handler mHandler = new Handler();

    /**
     * 扫描设备
     *
     * @param enable
     */
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
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
