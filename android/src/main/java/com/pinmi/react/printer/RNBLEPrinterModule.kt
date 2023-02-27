package com.pinmi.react.printer

import com.facebook.react.bridge.*
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.ble.BLEPrinterAdapter

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class RNBLEPrinterModule(protected var reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), RNPrinterModule {
    override var adapter: PrinterAdapter? = null
    @ReactMethod
    override fun init(successCallback: Callback, errorCallback: Callback?) {
        adapter = BLEPrinterAdapter.instance
        adapter!!.init(reactContext, successCallback, errorCallback)
    }

    @ReactMethod
    override fun closeConn() {
        adapter!!.closeConnectionIfExists()
    }

    @ReactMethod
    override fun getDeviceList(successCallback: Callback, errorCallback: Callback) {
        val printerDevices = adapter!!.getDeviceList(errorCallback)
        val pairedDeviceList = Arguments.createArray()
        for (printerDevice in printerDevices) {
            pairedDeviceList.pushMap(printerDevice.toRNWritableMap())
        }
        successCallback.invoke(pairedDeviceList)
    }

    @ReactMethod
    override fun printRawData(base64Data: String, errorCallback: Callback) {
        adapter!!.printRawData(base64Data, errorCallback)
    }

    override fun getName(): String {
        return "RNBLEPrinter"
    }
}
