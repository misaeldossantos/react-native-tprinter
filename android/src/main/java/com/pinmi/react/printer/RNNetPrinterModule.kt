package com.pinmi.react.printer

import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.net.NetPrinterAdapter
import com.pinmi.react.printer.adapter.net.NetPrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class RNNetPrinterModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), RNPrinterModule {
    override var adapter: PrinterAdapter? = null
    @ReactMethod
    override fun init(successCallback: Callback, errorCallback: Callback?) {
        adapter = NetPrinterAdapter.instance
        adapter!!.init(reactContext, successCallback, errorCallback)
    }

    @ReactMethod
    override fun closeConn() {
        adapter = NetPrinterAdapter.instance
        adapter!!.closeConnectionIfExists()
    }

    @ReactMethod
    override fun getDeviceList(successCallback: Callback, errorCallback: Callback) {
        try {
            adapter!!.getDeviceList(errorCallback)
            successCallback.invoke()
        } catch (ex: Exception) {
            errorCallback.invoke(ex.message)
        }
        // this.adapter.getDeviceList(errorCallback);
    }

    @ReactMethod
    fun connectPrinterWithHostPort(host: String?, port: Int?, successCallback: Callback, errorCallback: Callback) {
        adapter!!.selectDevice(NetPrinterDeviceId.valueOf(host, port), successCallback, errorCallback)
    }

    @ReactMethod
    override fun printRawData(base64Data: String, errorCallback: Callback) {
        adapter!!.printRawData(base64Data, errorCallback)
    }

    override fun getName(): String {
        return "RNNetPrinter"
    }
}
