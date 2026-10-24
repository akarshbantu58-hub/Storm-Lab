package com.stormlab

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var devicesBox: LinearLayout
    private lateinit var details: TextView
    private val found = linkedMapOf<String, BluetoothDevice>()
    private val handler = Handler(Looper.getMainLooper())
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private val adapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
        }
        root.addView(TextView(this).apply { text = "StormLab"; textSize = 28f })
        status = TextView(this).apply { textSize = 15f; setPadding(0, 10, 0, 10) }
        root.addView(status)
        root.addView(Button(this).apply { text = "Scan for Bluetooth devices"; setOnClickListener { scan() } })
        root.addView(Button(this).apply { text = "Stop scan"; setOnClickListener { stopScan() } })
        root.addView(Button(this).apply { text = "Disconnect"; setOnClickListener {
            try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}
            gatt = null
            status.text = "Disconnected"
        } })
        val scrollContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollContent.addView(TextView(this).apply { text = "Found devices — tap to connect"; textSize = 18f })
        devicesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollContent.addView(devicesBox)
        details = TextView(this).apply { text = "GATT service details will appear here."; textSize = 12f; setTextIsSelectable(true); setPadding(0, 16, 0, 16) }
        scrollContent.addView(details)
        root.addView(ScrollView(this).apply { addView(scrollContent) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        status.text = "Grant Bluetooth permissions, then scan."
        requestPermissionsIfNeeded()
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

    private fun scan() {
        if (!permitted()) { requestPermissionsIfNeeded(); status.text = "Bluetooth permissions are needed."; return }
        val a = adapter
        if (a == null || !a.isEnabled) { status.text = "Turn Bluetooth on and try again."; return }
        scanner = a.bluetoothLeScanner
        if (scanner == null) { status.text = "BLE scanner unavailable."; return }
        found.clear()
        devicesBox.removeAllViews()
        details.text = "Scanning…"
        try {
            scanner?.startScan(callback)
            scanning = true
            status.text = "Scanning for 12 seconds. Keep the watch nearby and awake."
            handler.postDelayed({ stopScan() }, 12000)
        } catch (e: Exception) { status.text = "Could not scan: ${e.message}" }
    }

    private fun stopScan() {
        if (!scanning) return
        try { scanner?.stopScan(callback) } catch (_: Exception) {}
        scanning = false
        status.text = "Scan finished. Found ${found.size} device(s)."
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device
            val address = try { device.address } catch (_: Exception) { return }
            if (found.containsKey(address)) return
            found[address] = device
            runOnUiThread {
                val name = try { device.name ?: result.scanRecord?.deviceName ?: "Unnamed device" } catch (_: Exception) { "Bluetooth device" }
                devicesBox.addView(Button(this@MainActivity).apply {
                    text = "$name\n$address • RSSI ${result.rssi} dBm"
                    textSize = 12f
                    setOnClickListener { connect(device, name) }
                })
                details.text = "Tap a device to connect. No commands are sent."
            }
        }
        override fun onScanFailed(code: Int) { runOnUiThread { status.text = "Scan failed: $code" } }
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
