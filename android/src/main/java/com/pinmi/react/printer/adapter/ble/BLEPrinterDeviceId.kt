package com.pinmi.react.printer.adapter.ble

import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class BLEPrinterDeviceId private constructor(val innerMacAddress: String) : PrinterDeviceId() {

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        if (!super.equals(o)) return false
        val that = o as BLEPrinterDeviceId
        return innerMacAddress == that.innerMacAddress
    }

    override fun hashCode(): Int {
        return innerMacAddress.hashCode()
    }

    companion object {
        fun valueOf(innerMacAddress: String): BLEPrinterDeviceId {
            return BLEPrinterDeviceId(innerMacAddress)
        }
    }
}
