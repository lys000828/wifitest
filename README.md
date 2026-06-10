# WiFi 检测器 (WifiDetector)

一个用于验证 WifiSpoof 模块是否生效的检测 App。

## 功能

该 App 会从三个层面获取 WiFi 信息，并对比显示：

1. **App层API (WifiManager)** - 通过 Android 标准 API 获取
   - SSID
   - BSSID
   - MAC 地址
   - IP 地址
   - 网络ID
   - 信号强度
   - 连接速度
   - 频率

2. **Java层 (NetworkInterface)** - 通过 Java 网络接口获取
   - MAC 地址
   - IPv4 地址
   - IPv6 地址

3. **系统属性** - 通过系统属性和 sysfs 获取
   - sysfs MAC 地址
   - WiFi 接口名称

4. **验证状态** - 自动检测各项信息是否被成功伪造

## 使用方法

### 1. 编译安装

```bash
cd test
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 配置 WifiSpoof 模块

1. 确保 WifiSpoof 模块已启用
2. 在 LSPosed 中将作用域设置为「所有应用」
3. **必须将 WifiDetector 也添加到作用域中**
4. 重启设备

### 3. 测试验证

1. 打开 WifiDetector App
2. 点击「刷新检测」按钮
3. 查看各层获取的 WiFi 信息
4. 检查「验证状态」区域的提示

## 验证结果解读

### 成功标志 ✅

如果 WifiSpoof 模块生效，你应该看到：

- SSID 显示你设置的伪造值（不是 `<unknown ssid>`）
- BSSID 显示你设置的伪造值（不是 `02:00:00:00:00:00`）
- MAC 显示你设置的伪造值（不是 `02:00:00:00:00:00`）
- 频率显示正常值（不是 `-1`）

### 失败标志 ❌

如果看到以下情况，说明模块未生效：

- SSID 显示 `<unknown ssid>`
- BSSID 或 MAC 显示 `02:00:00:00:00:00`
- 频率显示 `-1`
- 系统属性显示真实 MAC 地址

## 常见问题

### Q: 为什么 App 层 API 返回的都是占位值？

A: 这是 Android 10+ 的系统限制。WifiSpoof 需要 hook 这些 API 才能返回伪造值。请检查：

1. WifiSpoof 模块是否已启用
2. LSPosed 作用域是否包含此 App
3. 是否已重启设备

### Q: 系统属性显示了真实 MAC 怎么办？

A: 这说明 sysfs 的 hook 没有生效。可能原因：

1. 目标 App 没有被 hook
2. 某些 Android 版本的 sysfs 读取方式不同

### Q: 如何查看 hook 日志？

A: 在 WifiSpoof App 中点击「查看日志」按钮，日志文件位于 `/sdcard/wifispoof_hook.log`

## 技术说明

该 App 会检测三个层面的 WiFi 信息：

1. **App层API**：最容易被 hook，因为大多数 App 都使用这些 API
2. **Java层**：通过 NetworkInterface 获取，hook 难度中等
3. **系统属性**：通过 sysfs 获取，hook 难度较高

如果只有 App 层 API 被伪造，而其他层没有，说明 hook 不完整。
