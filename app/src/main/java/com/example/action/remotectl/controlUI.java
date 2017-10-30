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
import android.util.Log;
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
    private final static String TAG = Ble_Activity.class.getSimpleName();
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
    private TextView circle_angle;
    private int circle=0;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    //蓝牙特征值
    private static BluetoothGattCharacteristic target_chara = null;
    private Handler mhandler = new Handler();
    private AppData imdata ;
    private SeekBar angle_bar;
    private SeekBar speed_bar;
    private EditText angle_edit;
    private EditText edit_speed;

    /*操控走行界面*/
    private TextView textView_01, textView_02;
    private WheelView wheelView_01, wheelView_02;
    private int[] dataSend1=new int[4];
    private int[] dataSend2=new int[4];
    private EditText walk_edit_speed;

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
        final Handler sendHandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(dataSend1[0]!=dataSend1[2]||dataSend1[1]!=dataSend1[3]){
                    String s='a'+String.valueOf(dataSend1[0])+'b'+String.valueOf(dataSend1[1])+"\r\n";
                    sendString(s);
                }
                if(dataSend2[0]!=dataSend2[2]||dataSend2[1]!=dataSend2[3]){
                    String s='c'+String.valueOf(dataSend1[0])+'d'+String.valueOf(dataSend1[1])+"\r\n";
                    sendString(s);
                }
                dataSend1[2]=dataSend1[0];
                dataSend1[3]=dataSend1[1];
                dataSend2[2]=dataSend2[0];
                dataSend2[3]=dataSend2[1];
                sendHandler.postDelayed(this, 20);
            }
        };
        sendHandler.post(runnable);
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
        initView();
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
        angle_bar=(SeekBar) findViewById(R.id.progress_angle);
        angle_edit=findViewById(R.id.edit_angle);
        circle_angle=findViewById(R.id.angle_circle);
        speed_bar=findViewById(R.id.progress_speed);
        edit_speed=findViewById(R.id.edit_speed);
        walk_edit_speed=(EditText) findViewById(R.id.walk_edit_speed);
        findViewById(R.id.shoot_gun).setOnClickListener(this);
        findViewById(R.id.open).setOnClickListener(this);
        findViewById(R.id.shut).setOnClickListener(this);
        angle_bar.setOnSeekBarChangeListener(this);
        angle_edit.setOnClickListener(this);
        findViewById(R.id.circle_decrease).setOnClickListener(this);
        findViewById(R.id.circle_increase).setOnClickListener(this);
        findViewById(R.id.angle_decrease).setOnClickListener(this);
        findViewById(R.id.angle_increase).setOnClickListener(this);
        findViewById(R.id.speed_decrease).setOnClickListener(this);
        findViewById(R.id.speed_increase).setOnClickListener(this);
        findViewById(R.id.speed_change).setOnClickListener(this);
        findViewById(R.id.walk_speed_decrease).setOnClickListener(this);
        findViewById(R.id.walk_speed_increase).setOnClickListener(this);
        findViewById(R.id.walk_speed_change).setOnClickListener(this);
        speed_bar.setOnSeekBarChangeListener(this);
        angle_bar.setMax(3600);
        speed_bar.setMax(300000);
        connect_state.setText(status);
        imdata = new AppData(controlUI.this);
        edit_speed.setText(String.valueOf(imdata.getSettingSPEED()));
        walk_edit_speed.setText(String.valueOf(1000));
        speed_bar.setProgress((int)(imdata.getSettingSPEED()));
        circle_angle.setText(String.valueOf(imdata.getSettingCIRCLE()));
        angle_edit.setText(String.valueOf(imdata.getSettingANGLE()));
        angle_bar.setProgress((int)(imdata.getSettingANGLE()));
    }
    /**
     * 初始化视图组件
     */
    private void initView() {
        wheelView_01 = (WheelView) findViewById(R.id.wheelView_01);
        textView_01 = (TextView) findViewById(R.id.textView_01);
        wheelView_02 = (WheelView) findViewById(R.id.wheelView_02);
        textView_02 = (TextView) findViewById(R.id.textView_02);

        wheelView_01.setOnWheelViewMoveListener(
                new WheelView.OnWheelViewMoveListener() {
                    @Override
                    public void onValueChanged(int angle, int distance) {
                        textView_01.setText("角度：" + angle + "\t" + "距离：" + distance);
                        dataSend1[0]=angle;
                        dataSend1[1]=distance;
                    }
                }, 100L);
        wheelView_02.setOnWheelViewMoveListener(
                new WheelView.OnWheelViewMoveListener() {
                    @Override
                    public void onValueChanged(int angle, int distance) {
                        textView_02.setText("角度：" + angle + "\t" + "距离：" + distance);
                        dataSend2[0]=angle;
                        dataSend2[1]=distance;
                    }
                }, 100L);
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
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))//有效数据
            {
                //处理发送过来的数据
//                displayData(intent.getExtras().getString(
//                        BluetoothLeService.EXTRA_DATA));
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
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
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

    private void sendString(String s){
        if(!mFindService)
            return;
        target_chara.setValue(s);
        //调用蓝牙服务的写特征值方法实现发送数据
        mBluetoothLeService.writeCharacteristic(target_chara);
    }
    /*
     * 发送按键的响应事件，主要发送文本框的数据
     */
    @Override
    public void onClick(View v) {
        String s="null";
        switch (v.getId()){
            case R.id.shut:
                s="AT+shut\r\n";
                sendString(s);
                break;
            case R.id.open:
                s="AT+open\r\n";
                sendString(s);
                break;
            case R.id.circle_decrease:
                circle--;
                s="圈数 "+String.valueOf(circle);
                circle_angle.setText(s);
                break;
            case R.id.circle_increase:
                circle++;
                s="圈数 "+String.valueOf(circle);
                circle_angle.setText(s);
                break;
            case R.id.angle_decrease:
                angle_edit.setText(String.valueOf(Float.parseFloat(angle_edit.getText().toString())-0.5));
                break;
            case R.id.angle_increase:
                angle_edit.setText(String.valueOf(Float.parseFloat(angle_edit.getText().toString())+0.5));
                break;
            case R.id.speed_decrease:
                edit_speed.setText(String.valueOf(Float.parseFloat(edit_speed.getText().toString())-10000));
                break;
            case R.id.speed_increase:
                edit_speed.setText(String.valueOf(Float.parseFloat(edit_speed.getText().toString())+10000));
                break;
            case R.id.shoot_gun:
                AlertDialog.Builder dialog1 = new AlertDialog.Builder(controlUI.this);
                dialog1.setTitle("射吗？");
                dialog1.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        imdata.setSettingANGLE(Float.parseFloat(angle_edit.getText().toString()));
                        imdata.setSettingCIRCLE(circle);
                        String s;
                        s="AT+S"+String.valueOf(imdata.getSettingANGLE()+imdata.getSettingCIRCLE()*360)+"\r\n";
                        sendString(s);
                    }
                });
                dialog1.show();
                break;
            case R.id.speed_change:
                imdata.setSettingSPEED(Float.parseFloat(edit_speed.getText().toString()));
                s="AT+V"+String.valueOf(imdata.getSettingSPEED())+"\r\n";
                sendString(s);
                break;
            case R.id.walk_speed_decrease:
                walk_edit_speed.setText(String.valueOf(Float.parseFloat(walk_edit_speed.getText().toString())-1000));
                break;
            case R.id.walk_speed_increase:
                walk_edit_speed.setText(String.valueOf(Float.parseFloat(walk_edit_speed.getText().toString())+1000));
                break;
            case R.id.walk_speed_change:
                s="AT+W"+String.valueOf(Float.parseFloat(walk_edit_speed.getText().toString()))+"\r\n";
                sendString(s);
                break;
        }
    }
    //    通过进度条去调试文本框
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.progress_angle:
                angle_edit.setText(String.valueOf(progressToFloat(seekBar, progress)));
                break;
            case R.id.progress_speed:
                edit_speed.setText(String.valueOf(progressToFloat(seekBar, progress)));
        }
    }
    private float progressToFloat(SeekBar seekBar, int val) {
        switch (seekBar.getId()) {
            case R.id.progress_angle:
                return ((int)((val/10.f-180.f)*10))/10.f;
            case R.id.progress_speed:
                return val;
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


