package com.pinmi.react.printer

import com.facebook.react.bridge.*
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.usb.USBPrinterAdapter
import com.pinmi.react.printer.adapter.usb.USBPrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class RNUSBPrinterModule(protected var reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), RNPrinterModule {
    override var adapter: PrinterAdapter? = null
    @ReactMethod
    override fun init(successCallback: Callback, errorCallback: Callback?) {
        adapter = USBPrinterAdapter.instance
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
        if (printerDevices.size > 0) {
            for (printerDevice in printerDevices) {
                pairedDeviceList.pushMap(printerDevice.toRNWritableMap())
            }
            successCallback.invoke(pairedDeviceList)
        } else {
            errorCallback.invoke("No Device Found")
        }
    }

    @ReactMethod
    override fun printRawData(base64Data: String, errorCallback: Callback) {
        adapter!!.printRawData(base64Data, errorCallback)
    }

    @ReactMethod
    fun connectPrinter(vendorId: Int, productId: Int, successCallback: Callback, errorCallback: Callback) {
        adapter!!.selectDevice(USBPrinterDeviceId.valueOf(vendorId, productId), successCallback, errorCallback)
    }

    override fun getName(): String {
        return "RNUSBPrinter"
    }
}
