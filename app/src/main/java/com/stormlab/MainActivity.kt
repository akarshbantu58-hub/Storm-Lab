package com.stormlab

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var devicesBox: LinearLayout
    private lateinit var details: TextView
    private lateinit var themeButton: Button
    private val found = linkedMapOf<String, BluetoothDevice>()
    private val handler = Handler(Looper.getMainLooper())
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var darkMode = false
    private val stopScanTask = Runnable { stopScan() }
    private val adapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
        }
        root.addView(TextView(this).apply { text = "StormLab"; textSize = 28f; tag = "title" })
        status = TextView(this).apply { textSize = 15f; setPadding(0, 10, 0, 10) }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Scan for Bluetooth devices"
            setOnClickListener { scan() }
        })
        root.addView(Button(this).apply { text = "Show paired / connected devices"; setOnClickListener { showPairedDevices() } })
        root.addView(Button(this).apply { text = "Custom OS readiness check"; setOnClickListener { showOsReadiness() } })
        root.addView(Button(this).apply {
            text = "Copy diagnostic report"
            setOnClickListener { copyDiagnosticReport() }
        })
        root.addView(Button(this).apply { text = "Stop scan"; setOnClickListener { stopScan() } })
        root.addView(Button(this).apply {
            text = "Disconnect"
            setOnClickListener {
                try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}
                gatt = null
                status.text = "Disconnected"
            }
        })
        themeButton = Button(this).apply {
            text = "Switch to dark mode"
            setOnClickListener {
                darkMode = !darkMode
                themeButton.text = if (darkMode) "Switch to light mode" else "Switch to dark mode"
                applyTheme(root)
            }
        }
        root.addView(themeButton)

        val scrollContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollContent.addView(TextView(this).apply { text = "Found devices — tap to connect"; textSize = 18f })
        devicesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollContent.addView(devicesBox)
        details = TextView(this).apply {
            text = "GATT service details will appear here."
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(0, 16, 0, 16)
        }
        scrollContent.addView(details)
        root.addView(ScrollView(this).apply { addView(scrollContent) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        applyTheme(root)
        status.text = "Grant Bluetooth permissions, then scan."
        requestPermissionsIfNeeded()
    }

    private fun applyTheme(view: View) {
        val bg = if (darkMode) Color.rgb(18, 18, 18) else Color.rgb(250, 250, 250)
        val fg = if (darkMode) Color.rgb(240, 240, 240) else Color.rgb(25, 25, 25)
        val buttonBg = if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(225, 232, 240)
        view.setBackgroundColor(bg)
        if (view is TextView) view.setTextColor(fg)
        if (view is Button) {
            view.backgroundTintList = ColorStateList.valueOf(buttonBg)
            view.setTextColor(fg)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyTheme(view.getChildAt(i))
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 10)
    }

    private fun permitted(): Boolean = if (Build.VERSION.SDK_INT >= 31) {
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun copyDiagnosticReport() {
        val report = """StormLab diagnostic report
Watch: boAt Storm Call 3
Bluetooth name: STORMCALL3_1A46
Firmware shown on watch: V3.00.58
Watch MAC shown in About: 4D:23:0B:C0:1A:46

App status: ${status.text}

--- Current service / diagnostic details ---
${details.text}

Safety: StormLab discovery is read-only. No firmware was written or flashed."""
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("StormLab diagnostic report", report))
            Toast.makeText(this, "Diagnostic report copied. Paste it into your chat.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not copy report: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showOsReadiness() {
        status.text = "Custom OS readiness: not yet verified"
        details.text = """CUSTOM OS READINESS — boAt Storm Call 3

Watch platform: Crest+ OS (manufacturer documentation)
Bootloader / recovery access: NOT VERIFIED
Chipset and flash memory layout: UNKNOWN
Compatible custom firmware image: NOT VERIFIED
Verified flashing protocol: NOT FOUND

RESULT: NOT READY TO FLASH.

Bluetooth pairing and GATT service discovery do not establish operating-system installation access. StormLab will not write firmware or send undocumented commands. A safe installer would require verified hardware identification, a matching firmware image, a documented flashing method, and a recovery path.

Next step: check the watch's ordinary Settings > System/About screen, if present, and note the exact model, software version, and hardware identifiers. Do not attempt to load a firmware file unless it is confirmed for this exact model."""
    }

    private fun showPairedDevices() {
        if (!permitted()) { requestPermissionsIfNeeded(); status.text = "Allow Nearby devices / Bluetooth permission, then tap this again."; return }
        val a = adapter
        if (a == null || !a.isEnabled) { status.text = "Turn Bluetooth on in tablet settings."; return }
        devicesBox.removeAllViews()
        found.clear()
        try {
            val paired = a.bondedDevices
            if (paired.isEmpty()) {
                status.text = "Android reports no paired Bluetooth devices."
                details.text = "Pair the watch in tablet Bluetooth settings first, then tap Show paired / connected devices."
                return
            }
            for (device in paired) {
                val name = try { device.name ?: "Unnamed paired device" } catch (_: SecurityException) { "Paired device" }
                found[device.address] = device
                devicesBox.addView(Button(this).apply {
                    text = "$name\n${device.address} • paired"
                    textSize = 12f
                    setOnClickListener { connect(device, name) }
                })
            }
            status.text = "Listed ${paired.size} paired device(s). Tap your watch to try connecting."
            details.text = "Paired in Android does not always mean the watch exposes a controllable BLE service. This app can only inspect services the watch makes available."
            applyTheme(root)
        } catch (e: SecurityException) {
            status.text = "Bluetooth permission denied. Allow Nearby devices in Android app settings."
        } catch (e: Exception) {
            status.text = "Could not list paired devices: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun scan() {
        if (!permitted()) { requestPermissionsIfNeeded(); status.text = "Bluetooth permissions are needed."; return }
        val a = adapter
        if (a == null || !a.isEnabled) { status.text = "Turn Bluetooth on and try again."; return }
        scanner = a.bluetoothLeScanner
        if (scanner == null) { status.text = "BLE scanner unavailable. Toggle Bluetooth off/on and retry."; return }
        stopScan()
        found.clear()
        devicesBox.removeAllViews()
        details.text = "Scanning for nearby BLE advertisements…"
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setReportDelay(0L)
                .build()
            scanner?.startScan(null, settings, callback)
            scanning = true
            status.text = "Scanning for 20 seconds. Keep the watch nearby and awake."
            handler.removeCallbacks(stopScanTask)
            handler.postDelayed(stopScanTask, 20000)
        } catch (e: SecurityException) {
            status.text = "Permission error: allow Nearby devices, then reopen StormLab."
        } catch (e: Exception) {
            status.text = "Could not scan: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun stopScan() {
        handler.removeCallbacks(stopScanTask)
        if (!scanning) return
        try { scanner?.stopScan(callback) } catch (_: Exception) {}
        scanning = false
        status.text = "Scan finished. Found ${found.size} BLE device(s)."
        if (found.isEmpty()) details.text = "No BLE advertisements received. The watch may be connected to another device or may not advertise BLE while idle."
    }

    private fun addDevice(result: ScanResult) {
        val device = result.device
        val address = try { device.address } catch (_: Exception) { return }
        val name = try {
            device.name ?: result.scanRecord?.deviceName ?: "Unnamed BLE device"
        } catch (_: SecurityException) {
            result.scanRecord?.deviceName ?: "BLE device"
        }
        if (found.containsKey(address)) {
            // Update a generic label if a later advertisement reveals the name.
            if (name != "Unnamed BLE device" && name != "BLE device") {
                found[address] = device
            }
            return
        }
        found[address] = device
        runOnUiThread {
            val label = if (name == "Unnamed BLE device") "Unnamed BLE device" else name
            devicesBox.addView(Button(this).apply {
                text = "$label\n$address • RSSI ${result.rssi} dBm"
                textSize = 12f
                setOnClickListener { connect(device, label) }
            })
            details.text = "Tap a device to connect. Discovery only; no commands are sent."
            applyTheme(root)
        }
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) = addDevice(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (result in results) addDevice(result)
        }
        override fun onScanFailed(code: Int) {
            runOnUiThread {
                scanning = false
                handler.removeCallbacks(stopScanTask)
                status.text = when (code) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan already running. Tap Stop scan, then scan again."
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Android could not register the scanner. Toggle Bluetooth off/on and retry."
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "This device does not support the requested BLE scan mode."
                    SCAN_FAILED_INTERNAL_ERROR -> "Android BLE scan error. Restart Bluetooth and retry."
                    else -> "Scan failed: $code"
                }
            }
        }
    }

    private fun connect(device: BluetoothDevice, name: String) {
        if (!permitted()) { requestPermissionsIfNeeded(); return }
        try {
            try { gatt?.close() } catch (_: Exception) {}
            status.text = "Connecting to $name…"
            details.text = "Connecting. This prototype performs discovery only."
            gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) { status.text = "Connection failed: ${e.message}" }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, code: Int, state: Int) {
            runOnUiThread {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    status.text = "Connected. Discovering services…"
                    try { g.discoverServices() } catch (e: Exception) { status.text = "Discovery failed: ${e.message}" }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) status.text = "Disconnected (status $code)."
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, code: Int) {
            runOnUiThread {
                if (code != BluetoothGatt.GATT_SUCCESS) { details.text = "Service discovery failed: $code"; return@runOnUiThread }
                val out = StringBuilder("GATT services discovered: ${g.services.size}\n\n")
                for (service in g.services) {
                    out.append("SERVICE ${service.uuid}\n")
                    for (c in service.characteristics) {
                        val p = c.properties
                        val props = mutableListOf<String>()
                        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
                        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
                        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESPONSE")
                        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
                        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) props.add("INDICATE")
                        out.append("  CHARACTERISTIC ${c.uuid}\n    properties: ${props.joinToString().ifEmpty { "unknown" }}\n")
                    }
                    out.append("\n")
                }
                out.append("Discovery only: this app does not write data or flash firmware.")
                details.text = out.toString()
                status.text = "Service discovery complete."
            }
        }
    }

    override fun onDestroy() {
        stopScan()
        try { gatt?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}
