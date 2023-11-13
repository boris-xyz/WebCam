package com.kimdaq.livewebcam

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

class WebCam private constructor(
    val usbManager: UsbManager,
    val deviceId: Int,
    val portNum: Int,
) : SerialInputOutputManager.Listener {

    enum class UsbPermission {
        Unknown,
        Requested,
        Granted,
        Denied,
    }

    private val INTENT_ACTION_GRANT_USB: String = BuildConfig.APPLICATION_ID + ".GRANT_USB"
    private var usbPermission = UsbPermission.Unknown
    private val TAG = "WebCam Class"
    private var usbIoManager: SerialInputOutputManager? = null
    private var withIoManager: Boolean = false
    private var connected: Boolean = false
    private var port: UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var driver: UsbSerialDriver? = null

    class Builder {
        private lateinit var usbManager: UsbManager
        private var deviceId: Int = -1
        private var portNum: Int = -1

        fun setUsbManager(context: Context): Builder = apply {
            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        }

        fun setDriverId(driverId: Int): Builder = apply {
            this.deviceId = driverId
        }

        fun setPortNum(portNum: Int): Builder = apply {
            this.portNum = portNum
        }

        fun build() = WebCam(
            usbManager = usbManager,
            deviceId = deviceId,
            portNum = portNum,
        )
    }

    fun connectUsb(context: Context) {
        val availableDrivers: List<UsbSerialDriver> = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(context, context.resources.getString(R.string.emptyDrivers), Toast.LENGTH_SHORT).show()
            return
        }

        driver = availableDrivers.find { driver -> driver.device.deviceId == deviceId } ?: run {
            Toast.makeText(context, context.resources.getString(R.string.cannotfindDevice), Toast.LENGTH_SHORT).show()
            return
        }
        usbConnection = usbManager.openDevice(driver?.device) ?: run {
            Toast.makeText(context, context.resources.getString(R.string.cannotConnection), Toast.LENGTH_SHORT).show()
            return
        }
        val port: UsbSerialPort = driver?.ports?.getOrNull(portNum) ?: run {
            Toast.makeText(context, context.resources.getString(R.string.cannotfindport), Toast.LENGTH_SHORT).show()
            return
        }

        if (usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(
                driver?.device,
            )
        ) {
            usbPermission = UsbPermission.Requested
            val flags =
                PendingIntent.FLAG_MUTABLE
            val usbPermissionIntent =
                PendingIntent.getBroadcast(context, 0, Intent(INTENT_ACTION_GRANT_USB), flags)
            usbManager.requestPermission(driver?.device, usbPermissionIntent)
            return
        }

        if (!usbManager.hasPermission(driver?.device)) {
            Toast.makeText(context, context.resources.getString(R.string.permissionDenied), Toast.LENGTH_SHORT).show()
            return
        }

        runCatching {
            port.open(usbConnection)
        }.onSuccess {
            runCatching {
                port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            }.onFailure {
                Toast.makeText(context, context.resources.getString(R.string.unSupportedParams) + it.message, Toast.LENGTH_SHORT).show()
                return
            }
        }.onFailure {
            Toast.makeText(context, context.resources.getString(R.string.openFailed) + it.message, Toast.LENGTH_SHORT).show()
            disconnect()
            return
        }
        if (withIoManager) {
            usbIoManager = SerialInputOutputManager(port, this)
            usbIoManager?.start()
        }
        connected = true
    }

    fun disconnect() {
        connected = false
        usbIoManager?.let { manager ->
            manager.listener = null
            manager.stop()
        }
        usbIoManager = null
        runCatching {
            port?.close()
        }
        port = null
    }

    override fun onNewData(data: ByteArray?) {
        Log.d(TAG, data?.toString() ?: "")
    }

    override fun onRunError(e: Exception?) {
        Log.d(TAG, e?.message ?: "")
    }
}
