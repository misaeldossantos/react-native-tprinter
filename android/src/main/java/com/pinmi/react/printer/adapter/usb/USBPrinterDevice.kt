package com.pinmi.react.printer.adapter.usb

import android.hardware.usb.UsbDevice
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.pinmi.react.printer.adapter.abstracts.PrinterDevice
import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class USBPrinterDevice(val usbDevice: UsbDevice) : PrinterDevice {
    private val usbPrinterDeviceId: USBPrinterDeviceId

    init {
        usbPrinterDeviceId = USBPrinterDeviceId.valueOf(usbDevice.vendorId, usbDevice.productId)
    }

    override val printerDeviceId: PrinterDeviceId
        get() = usbPrinterDeviceId

    override fun toRNWritableMap(): WritableMap {
        val deviceMap = Arguments.createMap()
        deviceMap.putString("device_name", usbDevice.deviceName)
        deviceMap.putInt("device_id", usbDevice.deviceId)
        deviceMap.putInt("vendor_id", usbDevice.vendorId)
        deviceMap.putInt("product_id", usbDevice.productId)
        return deviceMap
    }
}
