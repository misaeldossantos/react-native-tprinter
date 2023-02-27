package com.pinmi.react.printer

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactMethod
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.ble.BLEPrinterDeviceId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by misaeldossantos on 2023/02/25.
 */
interface RNPrinterModule {
    var adapter: PrinterAdapter?

    fun init(successCallback: Callback, errorCallback: Callback?)
    fun closeConn()
    fun getDeviceList(successCallback: Callback, errorCallback: Callback)

    @ReactMethod
    fun printRawData(base64Data: String, errorCallback: Callback)

    @ReactMethod
    fun printImageData(imageUrl: String?, imageWidth: Int, imageHeight: Int, successCallback: Callback, errorCallback: Callback) {
        GlobalScope.launch {
            try {
                Log.v("imageUrl", imageUrl!!)
                adapter!!.printImageData(imageUrl, imageWidth, imageHeight)
                successCallback.invoke()
            } catch (e: Exception) {
                errorCallback.invoke(e.message)
            }
        }
    }

    @ReactMethod
    fun connectPrinter(innerAddress: String, successCallback: Callback, errorCallback: Callback) {
        GlobalScope.launch {
            adapter!!.selectDevice(BLEPrinterDeviceId.valueOf(innerAddress), successCallback, errorCallback)
        }
    }

    @ReactMethod
    fun getStatus(callback: Callback) {
        callback.invoke(adapter!!.getStatus())
    }

    @ReactMethod
    fun printImageBase64(base64: String?, imageWidth: Int, imageHeight: Int, successCallback: Callback, errorCallback: Callback) {
        GlobalScope.launch {
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                adapter!!.printImageBase64(decodedByte, imageWidth, imageHeight)
                successCallback.invoke()
            } catch (e: Exception) {
                errorCallback.invoke(e.message)
            }
        }
    }

}
