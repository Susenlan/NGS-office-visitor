# NGS Visitor Android App

[中文](#中文说明) | [English](#english)

## 中文说明

专用于查看 NGS-Visitor 的 UniFi Hotspot Voucher 状态。App 直接读取 UDM Pro 的官方 Network API，不需要常开电脑。

## 功能

- 自动发现首个 UniFi Site，也可以手动填写 Site ID。
- 每 30 秒读取最多 1,000 个 Voucher。
- 按 `authorizedGuestCount / authorizedGuestLimit` 显示剩余人数。
- 显示首次激活后共用的到期时间。
- 可用、使用中、不可用筛选。
- 主屏和设置页可通过 `EN / 中文` 按钮即时切换中英文，无需重启 App。
- 支持手机横屏；横屏时使用左侧状态与筛选、右侧访客码列表的双栏布局，左侧面板可独立上下滚动。
- 顶部操作区自动避让状态栏、刘海和挖孔摄像头，并适配底部手势导航安全区。
- 界面采用 Nord Gas Solutions 官方 Logo 与 PowerPoint 模板中的品牌色。
- 一键复制 Voucher，或生成只包含 Voucher 数字的二维码。
- 点击发放后在本机预留 30 分钟，避免访客尚未连接时同一码被重复发给另一组；可手动取消预留。
- API Key 使用 Android Keystore 的 AES-GCM 加密，仅保存在手机本地。
- 支持系统信任证书、用户安装的企业 CA，以及 IT 核验后的 SHA-256 证书固定。
- 进入设置前必须验证管理员账号和密码；首次使用时由 IT 创建管理员。
- 隐私模式默认开启，可禁止截图和最近任务预览，并在 60 秒后自动清除剪贴板中的访客码。
- 支持普通 Android 应用固定，以及由 MDM 白名单控制的 Lock Task 自助终端模式。
- 网络错误使用脱敏提示，不显示内部 IP 地址、端口或手机源地址。

## UniFi 准备

1. 在 UniFi Network 网页端打开 `Control Plane / Integrations`（不同版本名称可能略有不同）。
2. 创建专用 API Key。建议绑定权限受限的专用账号，不使用 Owner 账号。
3. 确保手机连接员工内部 Wi-Fi，并能以 HTTPS 访问 UDM Pro。
4. 不要把手机连接到隔离的 NGS-Visitor VLAN。

## App 设置

本地访问：

```text
API 根地址: https://UDM的IP或内部域名
Site ID: 可留空自动发现
API Key: 由 IT 在手机上填写
旧数据缺省人数: 5
```

通过 UniFi Cloud Connector 访问时，API 根地址可填写完整地址：

```text
https://api.ui.com/v1/connector/consoles/{consoleId}/proxy/network/integration
```

如果 UDM 使用自签名证书，优先让 IT 把企业 CA 安装到手机。也可以在设置中填写经过可信渠道核对的叶证书 SHA-256 指纹。App 不提供“忽略所有证书错误”的危险选项。

## 构建

仓库保留了标准 Gradle/Android Studio 工程配置。本工作区另提供一个无需安装 Android Studio 的可复现构建脚本；依赖放在 `.tools`，不会修改系统环境：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-debug.ps1
```

调试 APK 输出：

```text
output/android/NGS-Visitor-debug.apk
```

## 安装与使用建议

- 通过 Android 的“固定应用/屏幕固定”功能把设备锁定在 NGS Visitor。
- 手机应放在工作人员侧，不要持续向访客展示全部有效码。
- 对外展示时一次只打开一个 Voucher 的二维码，展示完立即关闭。
- Release APK 应由公司自己的签名密钥签名；调试 APK 仅用于内部试装。
- MDM、应用固定及防火墙规则请参见 [安全部署清单](SECURITY_DEPLOYMENT.md)。

---

## English

NGS Visitor is a dedicated Android app for checking UniFi Hotspot Voucher status on the NGS-Visitor network. It reads the official UniFi Network API directly from the UDM Pro, so no always-on computer is required.

### Features

- Discovers the first UniFi site automatically, with an option to enter the Site ID manually.
- Reads up to 1,000 vouchers every 30 seconds.
- Shows remaining capacity from `authorizedGuestCount / authorizedGuestLimit`.
- Shows the shared expiry time after a voucher is activated for the first time.
- Provides Available, Active, and Unavailable filters.
- Switches instantly between Chinese and English from the main screen or Settings.
- Supports landscape mode with status and filters on the left and vouchers on the right. The left panel scrolls independently on shorter screens.
- Keeps controls clear of status bars, display cutouts, punch-hole cameras, and gesture-navigation areas.
- Uses the official Nord Gas Solutions logos and brand colours from the company PowerPoint template.
- Copies a voucher with one tap or displays a QR code containing only the voucher digits.
- Holds an issued voucher locally for 30 minutes, preventing it from being issued twice while a guest is still connecting. Staff can release the hold manually.
- Encrypts the API Key with AES-GCM through Android Keystore and stores it only on the phone.
- Supports system-trusted certificates, user-installed enterprise CAs, and an IT-verified SHA-256 certificate fingerprint.
- Requires an administrator account and password before Settings can be opened. IT creates the account during first use.
- Enables Privacy mode by default to block screenshots and recent-task previews and clear copied vouchers after 60 seconds.
- Supports ordinary Android screen pinning and MDM-allowlisted Lock Task kiosk mode.
- Uses sanitized network errors that do not expose internal IP addresses, ports, or the phone source address.

### UniFi preparation

1. Open `Control Plane / Integrations` in the UniFi Network web interface. The exact menu name may vary by version.
2. Create a dedicated API Key. Use a restricted account rather than an Owner account.
3. Connect the phone to the internal employee Wi-Fi and confirm that it can reach the UDM Pro over HTTPS.
4. Do not connect the phone to the isolated NGS-Visitor VLAN.

### App settings

For local access:

```text
API root URL: https://UDM-IP-or-internal-hostname
Site ID: leave blank for automatic discovery
API Key: entered by IT on the phone
Fallback guest limit: 5
```

For access through UniFi Cloud Connector, enter the complete API root URL:

```text
https://api.ui.com/v1/connector/consoles/{consoleId}/proxy/network/integration
```

If the UDM uses a self-signed certificate, ask IT to install the enterprise CA on the phone where possible. Alternatively, enter the leaf certificate's SHA-256 fingerprint after IT verifies it through a trusted channel. The app deliberately does not provide an unsafe option to ignore certificate errors.

### Build

The repository contains a standard Gradle/Android Studio project. This workspace also provides a reproducible build script that does not require Android Studio. Its dependencies are stored under `.tools` and do not modify the system environment.

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-debug.ps1
```

Debug APK output:

```text
output/android/NGS-Visitor-debug.apk
```

### Installation and operating guidance

- Use Android app pinning or screen pinning to keep the device in NGS Visitor.
- Keep the phone on the staff side and avoid displaying every valid voucher continuously.
- Show only one voucher QR code at a time and close it immediately after use.
- Sign the release APK with a company-controlled signing key. The debug APK is intended only for internal evaluation.
- See the bilingual [Security Deployment Checklist](SECURITY_DEPLOYMENT.md) for MDM, app pinning, and firewall rules.
