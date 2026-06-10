package com.fakespoof.wifidetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WifiDetector";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI 组件
    private TextView tvAppSsid, tvAppBssid, tvAppMac, tvAppIp;
    private TextView tvAppNetworkId, tvAppRssi, tvAppLinkSpeed, tvAppFrequency;
    private TextView tvJavaMac, tvJavaIpv4, tvJavaIpv6;
    private TextView tvSysMac, tvSysInterface;
    private TextView tvStatus;
    private Button btnRefresh;

    // 系统服务
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    // 伪造值（用于验证）
    private static final String PLACEHOLDER_MAC = "02:00:00:00:00:00";
    private static final String PLACEHOLDER_SSID = "<unknown ssid>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化系统服务
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // 初始化 UI 组件
        initViews();

        // 设置点击事件
        btnRefresh.setOnClickListener(v -> refreshData());

        // 检查权限
        checkPermissions();
    }

    private void initViews() {
        tvAppSsid = findViewById(R.id.tv_app_ssid);
        tvAppBssid = findViewById(R.id.tv_app_bssid);
        tvAppMac = findViewById(R.id.tv_app_mac);
        tvAppIp = findViewById(R.id.tv_app_ip);
        tvAppNetworkId = findViewById(R.id.tv_app_network_id);
        tvAppRssi = findViewById(R.id.tv_app_rssi);
        tvAppLinkSpeed = findViewById(R.id.tv_app_link_speed);
        tvAppFrequency = findViewById(R.id.tv_app_frequency);

        tvJavaMac = findViewById(R.id.tv_java_mac);
        tvJavaIpv4 = findViewById(R.id.tv_java_ipv4);
        tvJavaIpv6 = findViewById(R.id.tv_java_ipv6);

        tvSysMac = findViewById(R.id.tv_sys_mac);
        tvSysInterface = findViewById(R.id.tv_sys_interface);

        tvStatus = findViewById(R.id.tv_status);
        btnRefresh = findViewById(R.id.btn_refresh);
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            refreshData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                refreshData();
            } else {
                Toast.makeText(this, "需要位置权限才能检测WiFi信息", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshData() {
        // 获取 App 层 API 信息
        getAppLayerInfo();

        // 获取 Java 层信息
        getJavaLayerInfo();

        // 获取系统属性信息
        getSystemPropertyInfo();

        // 验证状态
        verifyStatus();
    }

    // ========== App层API ==========
    private void getAppLayerInfo() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                tvAppSsid.setText("SSID: ❌ 权限不足");
                tvAppBssid.setText("BSSID: ❌ 权限不足");
                tvAppMac.setText("MAC: ❌ 权限不足");
                return;
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                tvAppSsid.setText("SSID: ❌ 获取失败");
                tvAppBssid.setText("BSSID: ❌ 获取失败");
                tvAppMac.setText("MAC: ❌ 获取失败");
                return;
            }

            // SSID
            String ssid = wifiInfo.getSSID();
            if (ssid == null) ssid = "null";
            tvAppSsid.setText("SSID: " + ssid);

            // BSSID
            String bssid = wifiInfo.getBSSID();
            if (bssid == null) bssid = "null";
            tvAppBssid.setText("BSSID: " + bssid);

            // MAC
            String mac = wifiInfo.getMacAddress();
            if (mac == null) mac = "null";
            tvAppMac.setText("MAC: " + mac);

            // IP
            int ipInt = wifiInfo.getIpAddress();
            String ip = (ipInt & 0xFF) + "." + ((ipInt >> 8) & 0xFF) + "."
                    + ((ipInt >> 16) & 0xFF) + "." + ((ipInt >> 24) & 0xFF);
            tvAppIp.setText("IP: " + ip);

            // 网络ID
            tvAppNetworkId.setText("网络ID: " + wifiInfo.getNetworkId());

            // 信号强度
            tvAppRssi.setText("信号强度: " + wifiInfo.getRssi() + " dBm");

            // 连接速度
            tvAppLinkSpeed.setText("连接速度: " + wifiInfo.getLinkSpeed() + " Mbps");

            // 频率
            tvAppFrequency.setText("频率: " + wifiInfo.getFrequency() + " MHz");

            // 调试日志
            android.util.Log.d("WifiDetector",
                "App层结果: SSID=" + ssid + " BSSID=" + bssid + " MAC=" + mac + " IP=" + ip);

        } catch (Exception e) {
            tvAppSsid.setText("SSID: ❌ 错误: " + e.getMessage());
            tvAppBssid.setText("BSSID: ❌ 错误");
            tvAppMac.setText("MAC: ❌ 错误");
            android.util.Log.e("WifiDetector", "App层获取失败", e);
        }
    }

    // ========== Java层 ==========
    private void getJavaLayerInfo() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                tvJavaMac.setText("MAC: ❌ 获取失败 (权限不足)");
                tvJavaIpv4.setText("IPv4: ❌ 获取失败");
                tvJavaIpv6.setText("IPv6: ❌ 获取失败");
                return;
            }

            boolean found = false;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni == null) continue;

                String name = ni.getName();
                if (name != null && name.equals("wlan0")) {
                    found = true;

                    // MAC 地址
                    try {
                        byte[] macBytes = ni.getHardwareAddress();
                        if (macBytes != null) {
                            StringBuilder macBuilder = new StringBuilder();
                            for (int i = 0; i < macBytes.length; i++) {
                                macBuilder.append(String.format("%02X", macBytes[i]));
                                if (i < macBytes.length - 1) macBuilder.append(":");
                            }
                            tvJavaMac.setText("MAC: " + macBuilder.toString());
                        } else {
                            tvJavaMac.setText("MAC: null");
                        }
                    } catch (Exception e) {
                        tvJavaMac.setText("MAC: ❌ 错误: " + e.getMessage());
                    }

                    // IP 地址
                    try {
                        Enumeration<InetAddress> addresses = ni.getInetAddresses();
                        String ipv4 = null;
                        String ipv6 = null;

                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (addr != null && !addr.isLoopbackAddress()) {
                                String hostAddr = addr.getHostAddress();
                                if (hostAddr != null) {
                                    if (hostAddr.contains(":")) {
                                        ipv6 = hostAddr;
                                    } else {
                                        ipv4 = hostAddr;
                                    }
                                }
                            }
                        }

                        tvJavaIpv4.setText("IPv4: " + (ipv4 != null ? ipv4 : "无"));
                        tvJavaIpv6.setText("IPv6: " + (ipv6 != null ? ipv6 : "无"));

                    } catch (Exception e) {
                        tvJavaIpv4.setText("IPv4: ❌ 错误: " + e.getMessage());
                        tvJavaIpv6.setText("IPv6: ❌ 错误");
                    }

                    break;
                }
            }

            if (!found) {
                tvJavaMac.setText("MAC: ❌ 未找到 wlan0 接口");
                tvJavaIpv4.setText("IPv4: ❌ 未找到 wlan0");
                tvJavaIpv6.setText("IPv6: ❌ 未找到 wlan0");
            }

        } catch (Exception e) {
            tvJavaMac.setText("MAC: ❌ 错误: " + e.getMessage());
        }
    }

    // ========== 系统属性 ==========
    private void getSystemPropertyInfo() {
        // 读取 sysfs MAC
        String sysMac = readMacFromSysFs("wlan0");
        tvSysMac.setText("sysfs MAC: " + (sysMac != null ? sysMac : "无法读取"));

        // 读取 WiFi 接口
        String wlanInterface = getSystemProp("wifi.interface");
        tvSysInterface.setText("WiFi接口: " + (wlanInterface != null ? wlanInterface : "N/A"));
    }

    private String readMacFromSysFs(String interfaceName) {
        try {
            File file = new File("/sys/class/net/" + interfaceName + "/address");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String mac = reader.readLine();
                reader.close();
                return mac != null ? mac.trim().toUpperCase() : null;
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    private String getSystemProp(String key) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method method = clazz.getMethod("get", String.class);
            return (String) method.invoke(null, key);
        } catch (Exception e) {
            return null;
        }
    }

    // ========== 验证状态 ==========
    private void verifyStatus() {
        StringBuilder status = new StringBuilder();
        boolean allSpoofed = true;

        // 获取各层的值
        String ssid = tvAppSsid.getText().toString().replace("SSID: ", "");
        String bssid = tvAppBssid.getText().toString().replace("BSSID: ", "");
        String mac = tvAppMac.getText().toString().replace("MAC: ", "");
        String ip = tvAppIp.getText().toString().replace("IP: ", "");
        String sysMac = tvSysMac.getText().toString().replace("sysfs MAC: ", "");

        // 检查 SSID
        if (ssid.equals(PLACEHOLDER_SSID) || ssid.equals("null") || ssid.contains("加载中") || ssid.contains("❌")) {
            status.append("❌ SSID: 未hook (" + ssid + ")\n");
            allSpoofed = false;
        } else {
            status.append("✅ SSID: " + ssid + "\n");
        }

        // 检查 BSSID
        if (bssid.equals(PLACEHOLDER_MAC) || bssid.equals("null") || bssid.contains("加载中") || bssid.contains("❌")) {
            status.append("❌ BSSID: 未hook (" + bssid + ")\n");
            allSpoofed = false;
        } else {
            status.append("✅ BSSID: " + bssid + "\n");
        }

        // 检查 MAC
        if (mac.equals(PLACEHOLDER_MAC) || mac.equals("null") || mac.contains("加载中") || mac.contains("❌")) {
            status.append("❌ MAC: 未hook (" + mac + ")\n");
            allSpoofed = false;
        } else {
            status.append("✅ MAC: " + mac + "\n");
        }

        // 检查 IP
        if (ip.equals("0.0.0.0") || ip.equals("null") || ip.contains("加载中") || ip.contains("❌")) {
            status.append("❌ IP: 未hook (" + ip + ")\n");
            allSpoofed = false;
        } else {
            status.append("✅ IP: " + ip + "\n");
        }

        // 检查 sysfs MAC
        if (sysMac.equals("无法读取") || sysMac.contains("❌")) {
            status.append("⚠️ sysfs MAC: 无法读取\n");
        } else if (sysMac.equals("AA:BB:CC:DD:EE:FF")) {
            status.append("✅ sysfs MAC: " + sysMac + "\n");
        } else {
            String normalizedSysMac = sysMac.replace(":", "").replace("-", "").toUpperCase();
            String normalizedBssid = bssid.replace(":", "").replace("-", "").toUpperCase();
            String normalizedMac = mac.replace(":", "").replace("-", "").toUpperCase();

            if (normalizedSysMac.equals(normalizedBssid) || normalizedSysMac.equals(normalizedMac)) {
                status.append("✅ sysfs MAC: " + sysMac + "\n");
            } else {
                status.append("⚠️ sysfs MAC: 真实值 (" + sysMac + ")\n");
            }
        }

        // 总结
        status.append("\n");
        if (allSpoofed) {
            status.append("🎉 WifiSpoof 已生效！\n");
        } else {
            status.append("⚠️ 部分未hook\n");
            status.append("请检查 LSPosed 作用域\n");
            status.append("确保 WifiDetector 在作用域中");
        }

        tvStatus.setText(status.toString());
    }
}
