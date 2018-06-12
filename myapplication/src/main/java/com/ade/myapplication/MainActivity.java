package com.ade.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private final String TAG = "MainActivity";

    private static final String[] perms = {
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    LocationManager locationManager;
    private LinearLayout rootLinearLayout;
    private TextView tvJingdu;
    private TextView tvWeidu;
    private TextView tvHaiba;
    private TextView tvSudu;
    private TextView tvTime;
    private TextView tvGpsStatus;
    private TextView log;
    private Button btn_clear_log;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();

        rootLinearLayout = findViewById(R.id.root_LinearLayout);
        tvJingdu = findViewById(R.id.tv_jingdu);
        tvWeidu = findViewById(R.id.tv_weidu);
        tvHaiba = findViewById(R.id.tv_haiba);
        tvSudu = findViewById(R.id.tv_sudu);
        tvTime = findViewById(R.id.tv_time);
        log = findViewById(R.id.log);
        btn_clear_log = findViewById(R.id.btn_clear_log);
        btn_clear_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Snackbar.make(rootLinearLayout, "没有GPS使用权限!", Snackbar.LENGTH_LONG).show();
            return;
        }


        if (checkGPSIsOpen()) {
            Snackbar.make(rootLinearLayout, "GPS已打开", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(rootLinearLayout, "GPS没有打开,请打开GPS!", Snackbar.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }


        // 为获取地理位置信息时设置查询条件
        String bestProvider = locationManager.getBestProvider(getCriteria(), true);

        Location location;
        location = locationManager.getLastKnownLocation(bestProvider);
        updateView(location);

        locationManager.addGpsStatusListener(listener);

        // 绑定监听，有4个参数
        // 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
        // 参数2，位置信息更新周期，单位毫秒
        // 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
        // 参数4，监听
        // 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新

        // 1秒更新一次，或最小位移变化超过1米更新一次；
        // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    private void printfLog(String str) {
        if(log.getLineCount() >= 25)
            log.setText("");
        log.append(str);
        log.append("\r\n");

    }

    // 状态监听
    GpsStatus.Listener listener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                // 第一次定位
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(TAG, "第一次定位");
                    printfLog("第一次定位");
                    break;
                // 卫星状态改变
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.i(TAG, "卫星状态改变");
                    printfLog("卫星状态改变");
                    // 获取当前状态
                    @SuppressLint("MissingPermission")
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    // 获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    // 创建一个迭代器保存所有卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites()
                            .iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    Log.i(TAG, "搜索到：" + count + "颗卫星");
                    printfLog("搜索到：" + count + "颗卫星");
                    break;
                // 定位启动
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i(TAG, "定位启动");
                    printfLog("定位启动");
                    break;
                // 定位结束
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i(TAG, "定位结束");
                    printfLog("定位结束");
                    break;
            }
        }


    };

    /**
     * 返回查询条件
     *
     * @return
     */
    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度
        criteria.setSpeedRequired(true);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        // 设置是否需要方位信息
        criteria.setBearingRequired(true);
        // 设置是否需要海拔信息
        criteria.setAltitudeRequired(true);
        // 设置电池消耗
        criteria.setPowerRequirement(Criteria.ACCURACY_HIGH);
        return criteria;
    }

    private void updateView(Location location) {
        if (location != null) {
            tvJingdu.setText(String.valueOf(location.getLatitude()));
            tvWeidu.setText(String.valueOf(location.getLongitude()));
            tvHaiba.setText(String.valueOf(location.getAltitude()));
            tvSudu.setText(String.valueOf(location.getSpeed()));
            tvTime.setText(String.valueOf(location.getTime()));
        }

    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateView(location);
            Log.i(TAG, "时间：" + location.getTime());
            Log.i(TAG, "经度：" + location.getLongitude());
            Log.i(TAG, "纬度：" + location.getLatitude());
            Log.i(TAG, "海拔：" + location.getAltitude());

            printfLog("时间：" + location.getTime());
            printfLog("经度：" + location.getLongitude());
            printfLog("纬度：" + location.getLatitude());
            printfLog("海拔：" + location.getAltitude());


        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, "当前GPS状态为可见状态");
                    printfLog("当前GPS状态为可见状态");
                    break;
                //GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, "当前GPS状态为服务区外状态");
                    printfLog("当前GPS状态为服务区外状态");
                    break;
                //GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                    printfLog("当前GPS状态为暂停服务状态");
                    break;
            }
        }

        /**
         * GPS开启时触发
         */
        @Override
        public void onProviderEnabled(String provider) {

            Snackbar.make(rootLinearLayout, "gps开启", Snackbar.LENGTH_LONG).show();
        }

        /**
         * GPS禁用时触发
         */
        @Override
        public void onProviderDisabled(String provider) {
            Snackbar.make(rootLinearLayout, "gps禁用", Snackbar.LENGTH_LONG).show();
        }
    };

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(MainActivity.this, "使用本程序前需要以下授权!", 6, perms);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle("请求权限")
                    .setRationale("缺失权限可能导致程序无法运行!请在弹出的窗口中授权，权限->设置单项权限->信任此应用(部分手机可能不同)")
                    .build().show();
        }
    }

    private boolean checkGPSIsOpen() {
        boolean isOpen;
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        isOpen = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        return isOpen;
    }
}
