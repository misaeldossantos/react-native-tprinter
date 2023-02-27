package com.pinmi.react.printer.adapter.usb

import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class USBPrinterDeviceId private constructor(val vendorId: Int, val productId: Int) : PrinterDeviceId() {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        if (!super.equals(o)) return false
        val that = o as USBPrinterDeviceId
        return if (vendorId != that.vendorId) false else productId == that.productId
    }

    override fun hashCode(): Int {
        var result = vendorId.hashCode()
        result = 31 * result + productId.hashCode()
        return result
    }

    companion object {
        fun valueOf(vendorId: Int, productId: Int): USBPrinterDeviceId {
            return USBPrinterDeviceId(vendorId, productId)
        }
    }
}
