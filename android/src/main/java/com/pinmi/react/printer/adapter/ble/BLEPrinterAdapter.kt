package com.pinmi.react.printer.adapter.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.*
import com.pinmi.react.printer.adapter.ImageUtils
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.abstracts.PrinterDevice
import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId
import java.io.IOException
import java.util.*

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class BLEPrinterAdapter private constructor() : PrinterAdapter {
    private val LOG_TAG = "RNBLEPrinter"
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothSocket: BluetoothSocket? = null
    private var mContext: ReactApplicationContext? = null
    override fun init(reactContext: ReactApplicationContext?, successCallback: Callback, errorCallback: Callback?) {
        mContext = reactContext
        successCallback.invoke()
    }

    override fun getDeviceList(errorCallback: Callback): List<PrinterDevice> {
        val bluetoothAdapter = bTAdapter
        val printerDevices: MutableList<PrinterDevice> = ArrayList()
        if (bluetoothAdapter == null) {
            errorCallback.invoke("No bluetooth adapter available")
            return printerDevices
        }
        if (!bluetoothAdapter.isEnabled) {
            errorCallback.invoke("bluetooth is not enabled")
            return printerDevices
        }
        val pairedDevices = bTAdapter.bondedDevices
        for (device in pairedDevices) {
            printerDevices.add(BLEPrinterDevice(device))
        }
        return printerDevices
    }

    override fun selectDevice(printerDeviceId: PrinterDeviceId, successCallback: Callback, errorCallback: Callback) {
        val bluetoothAdapter = bTAdapter
        if (bluetoothAdapter === null) {
            errorCallback.invoke("No bluetooth adapter available")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            errorCallback.invoke("bluetooth is not enabled")
            return
        }
        val blePrinterDeviceId = printerDeviceId as BLEPrinterDeviceId
        if (mBluetoothDevice != null) {
            if (mBluetoothDevice!!.address == blePrinterDeviceId.innerMacAddress && mBluetoothSocket != null) {
                Log.v(LOG_TAG, "do not need to reconnect")
                successCallback.invoke(BLEPrinterDevice(mBluetoothDevice).toRNWritableMap())
                return
            } else {
                closeConnectionIfExists()
            }
        }
        val pairedDevices = bTAdapter.bondedDevices
        for (device in pairedDevices) {
            if (device.address == blePrinterDeviceId.innerMacAddress) {
                try {
                    connectBluetoothDevice(device)
                    successCallback.invoke(BLEPrinterDevice(mBluetoothDevice).toRNWritableMap())
                    return
                } catch (e: IOException) {
                    e.printStackTrace()
                    errorCallback.invoke(e.message)
                    return
                }
            }
        }
        val errorText = "Can not find the specified printing device, please perform Bluetooth pairing in the system settings first."
        //        Toast.makeText(this.mContext, errorText, Toast.LENGTH_LONG).show();
        errorCallback.invoke(errorText)
    }

    @Throws(IOException::class)
    private fun connectBluetoothDevice(device: BluetoothDevice) {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        mBluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
        val socket = mBluetoothSocket as BluetoothSocket
        socket.connect()
        mBluetoothDevice = device //最后一步执行
    }

    override fun closeConnectionIfExists() {
        try {
            if (mBluetoothSocket != null) {
                mBluetoothSocket!!.close()
                mBluetoothSocket = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (mBluetoothDevice != null) {
            mBluetoothDevice = null
        }
    }

    override fun printRawData(rawBase64Data: String, errorCallback: Callback) {
        if (mBluetoothSocket == null) {
            errorCallback.invoke("bluetooth connection is not built, may be you forgot to connectPrinter")
            return
        }
        val socket = mBluetoothSocket
        Log.v(LOG_TAG, "start to print raw data $rawBase64Data")
        Thread {
            val bytes = Base64.decode(rawBase64Data, Base64.DEFAULT)
            try {
                val printerOutputStream = socket!!.outputStream
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
        ImageUtils.printImageFromStream(mBluetoothSocket!!.outputStream, imageUrl, imageWidth, imageHeight)
    }

    override fun printImageBase64(bitmapImage: Bitmap?, imageWidth: Int, imageHeight: Int) {
        if (bitmapImage == null) {
            throw IllegalArgumentException("Image not found!")
        }
        if (mBluetoothSocket == null) {
            throw IOException("Bluetooth connection is not built, may be you forgot to connectPrinter")
        }

        ImageUtils.printImageFromStream(mBluetoothSocket!!.outputStream, bitmapImage, imageWidth, imageHeight)
    }

    override fun getStatus(): String {
        if (!bTAdapter.isEnabled) {
            return "BLUETOOTH_DISABLED"
        }
        var status = "NOT_CONNECTED"
        if (mBluetoothSocket != null && mBluetoothSocket!!.isConnected) {
            when (mBluetoothSocket!!.remoteDevice.bondState) {
                BluetoothDevice.BOND_NONE -> status = "NOT_CONNECTED"
                BluetoothDevice.BOND_BONDING -> status = "PAIRING"
                BluetoothDevice.BOND_BONDED -> status = "PAIRED"
            }
            if (status === "PAIRED" && mBluetoothSocket!!.inputStream == null) {
                status = "NOT_CONNECTED"
            }
        }
        return status
    }

    companion object {
        private var mInstance: BLEPrinterAdapter? = null
        val instance: BLEPrinterAdapter?
            get() {
                if (mInstance == null) {
                    mInstance = BLEPrinterAdapter()
                }
                return mInstance
            }
        private val bTAdapter: BluetoothAdapter
            private get() = BluetoothAdapter.getDefaultAdapter()

    }
}
