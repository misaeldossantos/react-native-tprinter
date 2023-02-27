package com.pinmi.react.printer.adapter.abstracts

import com.facebook.react.bridge.WritableMap

/**
 * Created by misaeldossantos on 2023/02/25.
 */
interface PrinterDevice {
    val printerDeviceId: PrinterDeviceId
    fun toRNWritableMap(): WritableMap
}
