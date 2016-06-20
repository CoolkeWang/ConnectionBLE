package com.coolke.connectionble;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.socks.library.KLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.UnsupportedEncodingException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConnectionActivity extends AppCompatActivity {
    private final static String BLUETOOTH_NAME = "name";
    private final static String BLUETOOTH_ADDR = "addr";

    @BindView(R.id.ed_input)
    EditText edInput;
    @BindView(R.id.tv_content)
    TextView tvContent;
    @BindView(R.id.scrollView)
    ScrollView scrollView;

    private String bluetooth_name;
    private String bluetooth_addr;
    private boolean mConnected;

    public static void startActivity(Context context, String name, String addr) {
        Intent intent = new Intent(context.getApplicationContext(), ConnectionActivity.class);
        intent.putExtra(BLUETOOTH_NAME, name);
        intent.putExtra(BLUETOOTH_ADDR, addr);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        Intent intent = getIntent();
        bluetooth_name = intent.getStringExtra(BLUETOOTH_NAME);
        bluetooth_addr = intent.getStringExtra(BLUETOOTH_ADDR);

        getSupportActionBar().setTitle(bluetooth_name + "正在连接中....");

        //启动蓝牙服务
        Intent serviceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onclick(View v) {
        if(edInput.getText().toString().length() < 1){
            Toast.makeText(getApplicationContext(),"请输入内容",Toast.LENGTH_SHORT).show();
        }else {
            mBluetoothLeService.writeValue(edInput.getText().toString()+"\r\n");
            if (tvContent.length()>500){
                tvContent.setText("");
            }
            tvContent.append("from me : "+edInput.getText().toString()+"\n");
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    //蓝牙服务连接
    private BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                KLog.d("无法初始化蓝牙");
                finish();
            }
            KLog.d("蓝牙是好的");
            getSupportActionBar().setTitle(bluetooth_name + "已连接");
            mBluetoothLeService.connect(bluetooth_addr);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

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

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (mBluetoothLeService != null) {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
    }
}
