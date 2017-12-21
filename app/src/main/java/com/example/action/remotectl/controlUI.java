package com.example.action.remotectl;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntegerRes;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Summer on 2017/10/16.
 */

public class controlUI extends Activity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener {
    private final static String TAG = controlUI.class.getSimpleName();
    //蓝牙4.0的UUID,其中0000ffe1-0000-1000-8000-00805f9b34fb是广州汇承信息科技有限公司08蓝牙模块的UUID
    public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String EXTRAS_DEVICE_RSSI = "RSSI";
    //蓝牙连接状态
    private boolean mConnected = false;
    private boolean mFindService=false;
    private String status = "disconnected";
    //蓝牙地址
    private String mDeviceAddress;
    private Bundle b;
    //蓝牙service,负责后台的蓝牙服务
    private static BluetoothLeService mBluetoothLeService;
    //文本框，显示接受的内容
    private TextView connect_state;
    private TextView gas_value;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    //蓝牙特征值
    private static BluetoothGattCharacteristic target_chara = null;
    private Handler mhandler = new Handler();
    private AppData imdata ;

    private SeekBar steering_bar;
    private SeekBar pitch_bar;
    private SeekBar gas_bar;
    private EditText angle_edit;
    private EditText pitch_edit;;
    private EditText gas_edit;


    //	只是为了更新左上角的状态textview
    private Handler myHandler = new Handler() {
        // 2.重写消息处理函数
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // 判断发送的消息
                case 1: {
                    // 更新View·
                    String state = msg.getData().getString("connect_state");
                    connect_state.setText(state);
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        init();
        blueInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解除广播接收器
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
    }

    // Activity出来时候，绑定广播接收器，监听蓝牙连接服务传过来的事件
    @Override
    protected void onResume() {
        super.onResume();
        //绑定广播接收器
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            //根据蓝牙地址，建立连接
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }
    void blueInit()
    {
        //		获得上一个activity的消息
        b = getIntent().getExtras();
        //从意图获取显示的蓝牙信息
        mDeviceAddress = b.getString(EXTRAS_DEVICE_ADDRESS);
		/* 启动蓝牙service */
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
    /**
     * @param
     * @return void
     * @throws
     * @Title: init
     * @Description: TODO(初始化UI控件)
     */
    private void init() {
        setContentView(R.layout.ctrui);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制为横屏
        connect_state = (TextView) this.findViewById(R.id.connect_state);
        gas_value=(TextView)findViewById(R.id.gas_value); 
        angle_edit=findViewById(R.id.angle_edit);
        pitch_edit=findViewById(R.id.edit_pitch);
        gas_edit=findViewById(R.id.gas_edit);

        steering_bar=(SeekBar) findViewById(R.id.steering_bar);
        steering_bar.setOnSeekBarChangeListener(this);
        steering_bar.setMax(2200);

        pitch_bar=(SeekBar)findViewById(R.id.pitch_angle);
        pitch_bar.setOnSeekBarChangeListener(this);
        pitch_bar.setMax(600);

        gas_bar=(SeekBar)findViewById(R.id.gas_bar);
        gas_bar.setOnSeekBarChangeListener(this);
        gas_bar.setMax(10);

        findViewById(R.id.open).setOnClickListener(this);
        findViewById(R.id.shut).setOnClickListener(this);
        findViewById(R.id.shoot_gun).setOnClickListener(this);
        findViewById(R.id.shoot_reset).setOnClickListener(this);
        findViewById(R.id.steer_decrease).setOnClickListener(this);
        findViewById(R.id.steer_increase).setOnClickListener(this);
        findViewById(R.id.pitch_decrease).setOnClickListener(this);
        findViewById(R.id.pitch_increase).setOnClickListener(this);
        findViewById(R.id.steer_change).setOnClickListener(this);
        findViewById(R.id.pitch_change).setOnClickListener(this);
        findViewById(R.id.gas_decrease).setOnClickListener(this);
        findViewById(R.id.gas_increase).setOnClickListener(this);
        findViewById(R.id.gas_change).setOnClickListener(this);

        connect_state.setText(status);

        imdata = new AppData(controlUI.this);
        steering_bar.setProgress((int)(imdata.getSettingSTEER()));
        angle_edit.setText(String.valueOf(imdata.getSettingSTEER()));
        pitch_bar.setProgress((int)(imdata.getSettingPITCH()));
        pitch_edit.setText(String.valueOf(imdata.getSettingPITCH()));
        gas_bar.setProgress((int)(imdata.getSettingGAS()));
        gas_edit.setText(String.valueOf(imdata.getSettingGAS()));
    }
    /* BluetoothLeService绑定的回调函数 */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            // 根据蓝牙地址，连接设备
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }

    };

    /**
     * 广播接收器，负责接收BluetoothLeService类发送的数据
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))//Gatt连接成功
            {
                mConnected = true;
                status = "connected";
                //更新连接状态
                updateConnectionState(status);
                System.out.println("BroadcastReceiver :" + "device connected");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED//Gatt连接失败
                    .equals(action)) {
                mConnected = false;
                mFindService=false;
                status = "disconnected";
                //更新连接状态
                updateConnectionState(status);
                System.out.println("BroadcastReceiver :"
                        + "device disconnected");

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED//发现GATT服务器
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //获取设备的所有蓝牙服务
                displayGattServices(mBluetoothLeService
                        .getSupportedGattServices());
                System.out.println("BroadcastReceiver :"
                        + "device SERVICES_DISCOVERED");
                mFindService=true;
            } else if (BluetoothLeService.ACTION_DATA_READ_AVAILABLE.equals(action))//有效数据
            {
                String aim="AT+5";
                String msg=new String();
                msg=intent.getExtras().getString(BluetoothLeService.EXTRA_DATA);
                if(msg.length()>=4) {
                    Log.d("bletrack", msg.substring(0, 4));
                    if (msg.substring(0, 4).equals(aim)) {
                        gas_value.setText(msg.substring(4));
                    }
                }
            }
        }
    };

    /* 更新连接状态，不是更新连接参数 */
    private void updateConnectionState(String status) {
        Message msg = new Message();
        msg.what = 1;
        Bundle b = new Bundle();
        b.putString("connect_state", status);
        msg.setData(b);
        //将连接状态更新的UI的textview上
        myHandler.sendMessage(msg);
        System.out.println("connect_state:" + status);

    }

    /* 意图过滤器 */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ_AVAILABLE);
        return intentFilter;
    }

    /**
     * @param
     * @return void
     * @throws
     * @Title: displayGattServices
     * @Description: TODO(处理蓝牙服务)
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";

        // 服务数据,可扩展下拉列表的第一级数据
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        // 特征数据（隶属于某一级服务下面的特征值集合）
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

        // 部分层次，所有特征值集合
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            // 获取服务列表
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            // 查表，根据该uuid获取对应的服务名称。SampleGattAttributes这个表需要自定义。

            gattServiceData.add(currentServiceData);

            System.out.println("Service uuid:" + uuid);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

            // 从当前循环所指向的服务中读取特征值列表
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();

            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            // 对于当前循环所指向的服务中的每一个特征值
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                if (gattCharacteristic.getUuid().toString()
                        .equals(HEART_RATE_MEASUREMENT)) {
                    // 测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                    mhandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            mBluetoothLeService
                                    .readCharacteristic(gattCharacteristic);
                        }
                    }, 200);

                    // 接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    mBluetoothLeService.setCharacteristicNotification(
                            gattCharacteristic, true);
                    target_chara = gattCharacteristic;
                    // 设置数据内容
                    // 往蓝牙模块写入数据
                    // mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                }
                List<BluetoothGattDescriptor> descriptors = gattCharacteristic
                        .getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    System.out.println("---descriptor UUID:"
                            + descriptor.getUuid());
                    // 获取特征值的描述
                    mBluetoothLeService.getCharacteristicDescriptor(descriptor);
                    // mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    // true);
                }

                gattCharacteristicGroupData.add(currentCharaData);
            }
            // 按先后顺序，分层次放入特征值集合中，只有特征值
            mGattCharacteristics.add(charas);
            // 构件第二级扩展列表（服务下面的特征值）
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }

    }

    private class ToastRunnable implements Runnable {
        String mText;
        boolean mway;

        public ToastRunnable(String text, boolean way) {
            mText = text;
            mway = way;
        }

        @Override
        public void run() {
            if (mway)
                Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
            else {
                Toast toast = Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }
    private void sendString(String s){
        if(!mFindService)
            return;
        target_chara.setValue(s);
        //调用蓝牙服务的写特征值方法实现发送数据
        mBluetoothLeService.writeCharacteristic(target_chara);
    }
    int getIntegerCst(@IntegerRes int id)
    {
        return getResources().getInteger(id);
    }
    Handler handler=new Handler();
    /*
     * 发送按键的响应事件，主要发送文本框的数据
     */
    @Override
    public void onClick(View v) {
        String s="null";
        switch (v.getId()){
            case R.id.shut:
                s="AT+"+String.valueOf(getIntegerCst(R.integer.claw_cst))+String.valueOf(1)+"\r\n";
                sendString(s);
                handler.post(new ToastRunnable("爪子闭合",true));
                break;
            case R.id.open:
                s="AT+"+String.valueOf(getIntegerCst(R.integer.claw_cst))+String.valueOf(0)+"\r\n";
                sendString(s);
                handler.post(new ToastRunnable("爪子打开",true));
                break;
            case R.id.steer_decrease:
                angle_edit.setText(String.valueOf(Float.parseFloat(angle_edit.getText().toString())-0.5));
                handler.post(new ToastRunnable("舵机顺时针偏",true));
                break;
            case R.id.steer_increase:
                angle_edit.setText(String.valueOf(Float.parseFloat(angle_edit.getText().toString())+0.5));
                handler.post(new ToastRunnable("舵机逆时针偏",true));
                break;
            case R.id.pitch_decrease:
                pitch_edit.setText(String.valueOf(Float.parseFloat(pitch_edit.getText().toString())-0.5));
                handler.post(new ToastRunnable("向下转",true));
                break;
            case R.id.pitch_increase:
                pitch_edit.setText(String.valueOf(Float.parseFloat(pitch_edit.getText().toString())+0.5));
                handler.post(new ToastRunnable("向上转",true));
                break;
            case R.id.pitch_change:
                imdata.setSettingPITCH(Float.parseFloat(pitch_edit.getText().toString()));
                s="AT+"+String.valueOf(getIntegerCst(R.integer.pitch_cst))+String.valueOf(imdata.getSettingPITCH())+"\r\n";
                sendString(s);
                handler.post(new ToastRunnable("改变吧！赐予我力量！",true));
                break;
            case R.id.gas_decrease:
                gas_edit.setText(String.valueOf(Float.parseFloat(gas_edit.getText().toString())-0.01));
                handler.post(new ToastRunnable("大家注意，我要放气了",true));
                break;
            case R.id.gas_increase:
                gas_edit.setText(String.valueOf(Float.parseFloat(gas_edit.getText().toString())+0.01));
                handler.post(new ToastRunnable("先储存一会，等会再放",true));
                break;
            case R.id.gas_change:
                imdata.setSettingGAS(Float.parseFloat(gas_edit.getText().toString()));
                s="AT+"+String.valueOf(getIntegerCst(R.integer.gas_cst))+String.valueOf(imdata.getSettingGAS())+"\r\n";
                sendString(s);
                handler.post(new ToastRunnable("改变吧！赐予我力量！",true));
                break;
            case R.id.steer_change:
                imdata.setSettingSTEER(Float.parseFloat(angle_edit.getText().toString()));
                s="AT+"+String.valueOf(getIntegerCst(R.integer.steer_cst))+String.valueOf(imdata.getSettingSTEER())+"\r\n";
                sendString(s);
                handler.post(new ToastRunnable("改变吧！赐予我力量！",true));
                break;
            case R.id.shoot_gun:
                AlertDialog.Builder dialog1 = new AlertDialog.Builder(controlUI.this);
                dialog1.setTitle("射吗？");
                dialog1.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String s;
                        s="AT+"+String.valueOf(getIntegerCst(R.integer.shoot_cst))+String.valueOf(1)+"\r\n";
                        sendString(s);
                        handler.post(new ToastRunnable("射吧！就在此发！",true));
                    }
                });
                dialog1.show();
                break;
            case R.id.shoot_reset:
                s="AT+"+String.valueOf(getIntegerCst(R.integer.shoot_cst))+String.valueOf(0)+"\r\n";
                sendString(s);
                handler.post(new ToastRunnable("先休息一会......",true));
                break;
        }
    }
    //    通过进度条去调试文本框
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.steering_bar:
                angle_edit.setText(String.valueOf(progressToFloat(seekBar, progress)));
                break;
            case R.id.pitch_angle:
                pitch_edit.setText(String.valueOf(progressToFloat(seekBar, progress)));
                break;
            case R.id.gas_bar:
                gas_edit.setText(String.valueOf(progressToFloat(seekBar, progress)));
                break;
        }
    }
    private float progressToFloat(SeekBar seekBar, int val) {
        switch (seekBar.getId()) {
            case R.id.steering_bar:
                return ((int)((val/10.f-110.f)*10))/10.f;
            case R.id.pitch_angle:
                return ((int)((val/100.f)*100))/10.f;
            case R.id.gas_bar:
                return ((int)((val/100.f)*100))/10.f;
            default:
                Log.e("paramChange", "err progressToFloat");
                return 0.0f;
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}


