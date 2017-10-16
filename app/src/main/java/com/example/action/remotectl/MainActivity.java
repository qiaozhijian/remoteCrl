package com.example.action.remotectl;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
/**
 * @Description: TODO<MainActivity类实现打开蓝牙、扫描蓝牙>
 * @author 广州汇承信息科技有限公司
 * @data: 2014-10-12 上午10:28:18
 * @version: V1.0
 */
public class MainActivity extends Activity implements View.OnClickListener {
    // 扫描蓝牙按钮
    private Button scan_btn;
    // 蓝牙适配器
    BluetoothAdapter mBluetoothAdapter;
    //蓝牙扫描类
    private BluetoothLeScanner scanner;
    // 蓝牙信号强度
    private ArrayList<Integer> rssis;
    // 自定义Adapter
    LeDeviceListAdapter mleDeviceListAdapter;
    // listview显示扫描到的蓝牙信息
    ListView lv;
    // 描述扫描蓝牙的状态
    private boolean mScanning;
    private boolean scan_flag;
    private Handler mHandler;
    int REQUEST_ENABLE_BT = 1;
    // 蓝牙扫描时间
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化控件
        init();
        // 初始化蓝牙
        init_ble();
        scan_flag = true;
        // 自定义适配器
        mleDeviceListAdapter = new LeDeviceListAdapter();
        // 为listview指定适配器
        lv.setAdapter(mleDeviceListAdapter);

		/* listview点击函数 */
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                                    long id) {
                // TODO Auto-generated method stub
                final BluetoothDevice device = mleDeviceListAdapter
                        .getDevice(position);
                if (device == null)
                    return;
                final Intent intent = new Intent(MainActivity.this,
                        Ble_Activity.class);
//                传递信息
                intent.putExtra(Ble_Activity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(Ble_Activity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                intent.putExtra(Ble_Activity.EXTRAS_DEVICE_RSSI, rssis.get(position).toString());

                if (mScanning) {
                    /* 停止扫描设备 */
                    scanner.stopScan(mScanCallback);
                    mScanning = false;
                }

                try {
                    // 启动Ble_Activity
                    startActivity(intent);
                    Log.d("bletrack","start success");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("bletrack","start fail");
                    // TODO: handle exception
                }
            }
        });

    }

    /**
     * @Title: init
     * @Description: TODO(初始化UI控件)
     * @param
     * @return void
     * @throws
     */
    private void init() {
        scan_btn = (Button) this.findViewById(R.id.scan_dev_btn);
        scan_btn.setOnClickListener(this);
        lv = (ListView) this.findViewById(R.id.lv);
        mHandler = new Handler();
    }

    /**
     * @Title: init_ble
     * @Description: TODO(初始化蓝牙)
     * @param
     * @return void
     * @throws
     */
    private void init_ble() {
        // 手机硬件支持蓝牙
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes Bluetooth adapter.
        // 获取手机本地的蓝牙适配器
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 打开蓝牙权限
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    /*
     * 按钮响应事件
     */
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        if (scan_flag) {
            mleDeviceListAdapter = new LeDeviceListAdapter();
            lv.setAdapter(mleDeviceListAdapter);
            scan_btn.setText("停止扫描");
            scanLeDevice(true);
        } else {
            scanLeDevice(false);
            scan_btn.setText("扫描设备");
        }
    }

    /**
     * @Title: scanLeDevice
     * @Description: TODO(扫描蓝牙设备)
     * @param enable
     *            (扫描使能，true:扫描开始,false:扫描停止)
     * @return void
     * @throws
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    scan_flag = true;
                    scan_btn.setText("扫描设备");
                    Log.i("bletrack", "stoping scanning");
                    scanner.stopScan(mScanCallback);   // 停止扫描
                }
            }, SCAN_PERIOD);
			/* 开始扫描蓝牙设备，带mLeScanCallback 回调函数 */
            Log.i("SCAN", "begin.....................");
            mScanning = true;
            scan_flag = false;
            scan_btn.setText("停止扫描");
            Log.i("bletrack", "begin scanning");
            scanner.startScan(mScanCallback);  // 开始扫描Callback);5.0及之前的版本
        } else {
            mScanning = false;
            Log.i("bletrack", "stoping scanning");
            scanner.stopScan(mScanCallback);   // 停止扫描
            scan_flag = true;
        }

    }
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothDevice deviceScan = result.getDevice();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
					/* 讲扫描到设备的信息输出到listview的适配器 */
                    mleDeviceListAdapter.addDevice(deviceScan, result.getRssi());
                    mleDeviceListAdapter.notifyDataSetChanged();
                }
            });

            System.out.println("Address:" + deviceScan.getAddress());
            System.out.println("Name:" + deviceScan.getName());
            System.out.println("rssi:" + result.getRssi());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            // 扫描失败，并且失败原因
            Log.d("bletrack", "扫描失败" + String.valueOf(errorCode));
        }
    };

    /**
     * @Description: TODO<自定义适配器Adapter,作为listview的适配器>
     * @author 广州汇承信息科技有限公司
     * @data: 2014-10-12 上午10:46:30
     * @version: V1.0
     */
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;

        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            rssis = new ArrayList<Integer>();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device, int rssi) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                rssis.add(rssi);
            }
        }

        public void clear() {
            mLeDevices.clear();
            rssis.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public long getItemId(int i) {

            return i;
        }

        /**
         * 重写getview
         *
         * **/
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            // General ListView optimization code.
            // 加载listview每一项的视图
            view = mInflator.inflate(R.layout.listitem, null);
            // 初始化三个textview显示蓝牙信息
            TextView deviceAddress = (TextView) view
                    .findViewById(R.id.tv_deviceAddr);
            TextView deviceName = (TextView) view
                    .findViewById(R.id.tv_deviceName);
            TextView rssi = (TextView) view.findViewById(R.id.tv_rssi);

            BluetoothDevice device = mLeDevices.get(i);
            deviceAddress.setText(device.getAddress());
            deviceName.setText(device.getName());
            rssi.setText("" + rssis.get(i));

            return view;
        }
    }

}
