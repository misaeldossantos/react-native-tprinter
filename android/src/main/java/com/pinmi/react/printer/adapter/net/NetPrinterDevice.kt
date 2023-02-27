package com.pinmi.react.printer.adapter.net

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.pinmi.react.printer.adapter.abstracts.PrinterDevice
import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class NetPrinterDevice(host: String?, port: Int?) : PrinterDevice {
    private val mNetPrinterDeviceId: NetPrinterDeviceId

    init {
        mNetPrinterDeviceId = NetPrinterDeviceId.valueOf(host, port)
    }

    override val printerDeviceId: PrinterDeviceId
        get() = mNetPrinterDeviceId

    override fun toRNWritableMap(): WritableMap {
        val deviceMap = Arguments.createMap()
        deviceMap.putString("device_name", mNetPrinterDeviceId.host + ":" + mNetPrinterDeviceId.port)
        deviceMap.putString("host", mNetPrinterDeviceId.host)
        deviceMap.putInt("port", mNetPrinterDeviceId.port!!)
        return deviceMap
    }
}
