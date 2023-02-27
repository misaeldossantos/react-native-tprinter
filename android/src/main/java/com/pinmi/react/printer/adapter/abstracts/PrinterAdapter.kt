package com.pinmi.react.printer.adapter.abstracts

import android.graphics.Bitmap
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext

/**
 * Created by misaeldossantos on 2023/02/25.
 */
interface PrinterAdapter {
    fun init(reactContext: ReactApplicationContext?, successCallback: Callback, errorCallback: Callback?)
    fun getDeviceList(errorCallback: Callback): List<PrinterDevice>
    fun selectDevice(printerDeviceId: PrinterDeviceId, successCallback: Callback, errorCallback: Callback)
    fun closeConnectionIfExists()
    fun printRawData(rawBase64Data: String, errorCallback: Callback)
    fun printImageData(imageUrl: String?, imageWidth: Int, imageHeight: Int)
    fun printImageBase64(imageUrl: Bitmap?, imageWidth: Int, imageHeight: Int)
    fun getStatus(): String
}
