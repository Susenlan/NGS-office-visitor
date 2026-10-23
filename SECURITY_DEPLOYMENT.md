# NGS Visitor 安全部署清单 / Security Deployment Checklist

## 中文

### MDM 与专用设备

建议把手机注册为 Android Enterprise 的“专用设备”或“完全受管设备”，并应用以下策略：

- App 包名：`com.nordgas.ngsvisitor`
- 将该包加入 Lock Task 白名单，并设置为设备开机后自动启动的前台 App。
- 在 App 设置中启用“应用固定 / MDM 自助终端模式”和“隐私模式”。
- 禁止未知来源安装、USB 调试、开发者选项和未经批准的 App。
- 禁止普通用户修改 Wi-Fi、VPN、证书、日期时间和设备管理员设置。
- 通过 MDM 安装和管理企业 CA，不允许工作人员自行安装用户 CA。
- 启用系统自动更新、远程锁定、远程擦除和设备合规告警。
- 保留一个由 IT 控制的自助终端退出方式，不向前台工作人员公开退出凭据。

App 的 Manifest 已声明 `lockTaskMode="if_whitelisted"`。MDM 需要通过 Android `DevicePolicyManager.setLockTaskPackages()` 或其管理控制台中的等效策略，将上述包名加入白名单。没有 MDM 白名单时，App 只能使用普通 Android 屏幕固定，用户仍可能通过系统组合键退出。

### 防火墙建议

为专用手机建立独立 VLAN，并使用 DHCP 保留地址。规则应按从上到下的顺序实施：

| 来源 | 目标 | 服务 | 动作 |
|---|---|---|---|
| NGS Visitor 设备或专用 VLAN | UDM Pro 管理地址 | TCP 443 | 允许 |
| NGS Visitor 设备或专用 VLAN | 受控 DNS、NTP、DHCP | 所需端口 | 允许 |
| NGS Visitor 设备或专用 VLAN | MDM 服务地址 | TCP 443 | 允许 |
| NGS Visitor 设备或专用 VLAN | NGS-Visitor 访客 VLAN | 全部 | 拒绝 |
| NGS Visitor 设备或专用 VLAN | 其他管理网段和内部服务器 | 全部 | 拒绝 |
| NGS Visitor 设备或专用 VLAN | 其他互联网地址 | 全部 | 按业务需要拒绝或严格限制 |

如果 App 通过 UniFi Cloud Connector 工作，可将 UDM 管理地址规则替换为仅允许 `api.ui.com:443`，并继续允许 MDM、DNS 和 NTP。实际域名白名单能力取决于公司防火墙。

不要仅依赖手机 MAC 地址做安全控制。应结合专用 VLAN、802.1X/企业 Wi-Fi、设备证书或 MDM 合规状态。

### UniFi API Key

- 使用专用账号生成 API Key，不使用 Owner 账号。
- 分配最低可行的只读权限。
- 在人员变更、设备丢失或怀疑泄露时立即撤销并重新生成。
- 建立定期轮换记录，并在轮换后验证 Voucher 读取功能。

### 上线检查

- 管理员账号已创建，密码不少于 10 个字符并由 IT 保管。
- 隐私模式已开启，截图和最近任务预览被阻止。
- 从剪贴板复制访客码后，60 秒内会自动清除。
- 错误页面不显示内部 IP、端口或手机源地址。
- MDM Lock Task 生效，普通用户无法退出 App 或进入系统设置。
- 防火墙日志确认手机只能访问必要目标。
- API Key 权限经过独立复核。
- 公司 Release APK 使用受控签名密钥，不使用 debug keystore。

---

## English

### MDM and dedicated-device policy

Enroll the phone as an Android Enterprise Dedicated Device or Fully Managed Device and apply these policies:

- App package: `com.nordgas.ngsvisitor`
- Allowlist the package for Lock Task and configure it as the foreground app after boot.
- Enable **App pinning / MDM kiosk mode** and **Privacy mode** in the app settings.
- Block unknown-source installation, USB debugging, developer options, and unapproved apps.
- Prevent normal users from changing Wi-Fi, VPN, certificates, date and time, or device-administrator settings.
- Install and manage the enterprise CA through MDM. Do not allow staff to install arbitrary user CAs.
- Enable automatic OS updates, remote lock, remote wipe, and compliance alerts.
- Keep an IT-controlled kiosk exit method and do not disclose the exit credentials to front-desk users.

The app manifest declares `lockTaskMode="if_whitelisted"`. The MDM must allowlist the package through Android `DevicePolicyManager.setLockTaskPackages()` or the equivalent setting in its management console. Without an MDM allowlist, the app can only use ordinary Android screen pinning, which users may exit with the system key combination.

### Firewall policy

Place the dedicated phone in its own VLAN and assign it a DHCP reservation. Apply rules in this order:

| Source | Destination | Service | Action |
|---|---|---|---|
| NGS Visitor device or dedicated VLAN | UDM Pro management address | TCP 443 | Allow |
| NGS Visitor device or dedicated VLAN | Controlled DNS, NTP, and DHCP | Required ports | Allow |
| NGS Visitor device or dedicated VLAN | MDM service endpoints | TCP 443 | Allow |
| NGS Visitor device or dedicated VLAN | NGS-Visitor guest VLAN | Any | Deny |
| NGS Visitor device or dedicated VLAN | Other management networks and internal servers | Any | Deny |
| NGS Visitor device or dedicated VLAN | Other internet destinations | Any | Deny or strictly limit as required |

When using UniFi Cloud Connector, replace the direct UDM rule with an allow rule for `api.ui.com:443`, while retaining access to MDM, DNS, and NTP. Domain allowlisting support depends on the company firewall.

Do not rely on the phone MAC address alone. Combine a dedicated VLAN with 802.1X or enterprise Wi-Fi, a device certificate, or MDM compliance state.

### UniFi API Key

- Generate the API Key from a dedicated account, not an Owner account.
- Assign the minimum practical read-only permissions.
- Revoke and replace it immediately after staff changes, device loss, or suspected exposure.
- Maintain a rotation record and verify voucher access after every rotation.

### Go-live checks

- IT has created and retained an administrator account with a password of at least 10 characters.
- Privacy mode is enabled and blocks screenshots and recent-task previews.
- Copied vouchers are removed from the clipboard within 60 seconds.
- Error screens do not show internal IP addresses, ports, or the phone source address.
- MDM Lock Task is active and normal users cannot leave the app or open system settings.
- Firewall logs confirm that the phone reaches only required destinations.
- A second person has reviewed the API Key permissions.
- The company Release APK uses a controlled signing key rather than the debug keystore.
