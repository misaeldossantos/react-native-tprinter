package com.pinmi.react.printer.adapter.net

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.pinmi.react.printer.adapter.ImageUtils
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.abstracts.PrinterDevice
import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class NetPrinterAdapter private constructor() : PrinterAdapter {
    private var mContext: ReactApplicationContext? = null
    private val LOG_TAG = "RNNetPrinter"
    private var mNetDevice: NetPrinterDevice? = null

    // {TODO- support other ports later}
    private val PRINTER_ON_PORTS = intArrayOf(9100)
    private var mSocket: Socket? = null
    private var isRunning = false
    override fun init(reactContext: ReactApplicationContext?, successCallback: Callback, errorCallback: Callback?) {
        mContext = reactContext
        successCallback.invoke()
    }

    override fun getDeviceList(errorCallback: Callback): List<PrinterDevice> {
        // errorCallback.invoke("do not need to invoke get device list for net
        // printer");
        // Use emitter instancee get devicelist to non block main thread
        this.scan()
        return ArrayList()
    }

    private fun scan() {
        if (isRunning) return
        Thread {
            try {
                isRunning = true
                emitEvent(EVENT_SCANNER_RUNNING, isRunning)
                val wifiManager = mContext!!.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddress = ipToString(wifiManager.connectionInfo.ipAddress)
                val array = Arguments.createArray()
                val prefix = ipAddress.substring(0, ipAddress.lastIndexOf('.') + 1)
                val suffix = ipAddress.substring(ipAddress.lastIndexOf('.') + 1, ipAddress.length).toInt()
                for (i in 0..255) {
                    if (i == suffix) continue
                    val ports = getAvailablePorts(prefix + i)
                    if (!ports.isEmpty()) {
                        val payload = Arguments.createMap()
                        payload.putString("host", prefix + i)
                        payload.putInt("port", 9100)
                        array.pushMap(payload)
                    }
                }
                emitEvent(EVENT_SCANNER_RESOLVED, array)
            } catch (ex: NullPointerException) {
                Log.i(LOG_TAG, "No connection")
            } finally {
                isRunning = false
                emitEvent(EVENT_SCANNER_RUNNING, isRunning)
            }
        }.start()
    }

    private fun emitEvent(eventName: String, data: Any) {
        if (mContext != null) {
            mContext!!.getJSModule(RCTDeviceEventEmitter::class.java).emit(eventName, data)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun getAvailablePorts(address: String): ArrayList<Int> {
        val ports = ArrayList<Int>()
        for (port in PRINTER_ON_PORTS) {
            if (crunchifyAddressReachable(address, port)) ports.add(port)
        }
        return ports
    }

    private fun ipToString(ip: Int): String {
        return (ip and 0xFF).toString() + "." + (ip shr 8 and 0xFF) + "." + (ip shr 16 and 0xFF) + "." + (ip shr 24 and 0xFF)
    }

    override fun selectDevice(printerDeviceId: PrinterDeviceId, sucessCallback: Callback, errorCallback: Callback) {
        val netPrinterDeviceId = printerDeviceId as NetPrinterDeviceId
        if (mSocket != null && !mSocket!!.isClosed && mNetDevice!!.printerDeviceId == netPrinterDeviceId) {
            Log.i(LOG_TAG, "already selected device, do not need repeat to connect")
            sucessCallback.invoke(mNetDevice!!.toRNWritableMap())
            return
        }
        try {
            val socket = Socket(netPrinterDeviceId.host, netPrinterDeviceId.port!!)
            if (socket.isConnected) {
                closeConnectionIfExists()
                mSocket = socket
                mNetDevice = NetPrinterDevice(netPrinterDeviceId.host, netPrinterDeviceId.port)
                sucessCallback.invoke(mNetDevice!!.toRNWritableMap())
            } else {
                errorCallback.invoke(
                    "unable to build connection with host: " + netPrinterDeviceId.host
                            + ", port: " + netPrinterDeviceId.port
                )
                return
            }
        } catch (e: IOException) {
            e.printStackTrace()
            errorCallback.invoke("failed to connect printer: " + e.message)
        }
    }

    override fun closeConnectionIfExists() {
        if (mSocket != null) {
            if (!mSocket!!.isClosed) {
                try {
                    mSocket!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            mSocket = null
        }
    }

    override fun printRawData(rawBase64Data: String, errorCallback: Callback) {
        if (mSocket == null) {
            errorCallback.invoke("Net connection is not built, may be you forgot to connectPrinter")
            return
        }
        val socket = mSocket
        Log.v(LOG_TAG, "start to print raw data $rawBase64Data")
        Thread {
            try {
                val bytes = Base64.decode(rawBase64Data, Base64.DEFAULT)
                val printerOutputStream = socket!!.getOutputStream()
                printerOutputStream.write(bytes, 0, bytes.size)
                printerOutputStream.flush()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "failed to print data$rawBase64Data")
                e.printStackTrace()
            }
        }.start()
    }

    override fun printImageData(imageUrl: String?, imageWidth: Int, imageHeight: Int) {
        if (imageUrl == null || imageUrl === "") {
            throw IllegalArgumentException("ImageUrl is empty!")
        }
        if (getStatus() !== "PAIRED") {
            throw IllegalArgumentException("Bluetooth not connected!")
        }
        ImageUtils.printImageFromStream(mSocket!!.outputStream, imageUrl, imageWidth, imageHeight)
    }

    override fun printImageBase64(bitmapImage: Bitmap?, imageWidth: Int, imageHeight: Int) {
        if (bitmapImage == null) {
            throw IllegalArgumentException("Image not found!")
        }
        if (mSocket == null) {
            throw IOException("Bluetooth connection is not built, may be you forgot to connectPrinter")
        }

        ImageUtils.printImageFromStream(mSocket!!.outputStream, bitmapImage, imageWidth, imageHeight)
    }

    override fun getStatus(): String {
        TODO("Not implemented")
    }

    companion object {
        private var mInstance: NetPrinterAdapter? = null
        private const val EVENT_SCANNER_RESOLVED = "scannerResolved"
        private const val EVENT_SCANNER_RUNNING = "scannerRunning"

        val instance: NetPrinterAdapter?
            get() {
                if (mInstance == null) {
                    mInstance = NetPrinterAdapter()
                }
                return mInstance
            }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private fun crunchifyAddressReachable(address: String, port: Int): Boolean {
            return try {
                Socket().use { crunchifySocket ->
                    // Connects this socket to the server with a specified timeout value.
                    crunchifySocket.connect(InetSocketAddress(address, port), 100)
                }
                // Return true if connection successful
                true
            } catch (exception: IOException) {
                exception.printStackTrace()
                false
            }
        }

    }
}
