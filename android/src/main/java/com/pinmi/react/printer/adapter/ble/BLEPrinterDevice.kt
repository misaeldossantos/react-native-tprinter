package com.pinmi.react.printer.adapter.ble

import android.bluetooth.BluetoothDevice
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.pinmi.react.printer.adapter.abstracts.PrinterDevice
import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class BLEPrinterDevice(private val mBluetoothDevice: BluetoothDevice?) : PrinterDevice {
    private val mPrinterDeviceId: BLEPrinterDeviceId

    init {
        mPrinterDeviceId = BLEPrinterDeviceId.valueOf(mBluetoothDevice!!.address)
    }

    override val printerDeviceId: PrinterDeviceId
        get() = mPrinterDeviceId

    override fun toRNWritableMap(): WritableMap {
        val deviceMap = Arguments.createMap()
        deviceMap.putString("inner_mac_address", mPrinterDeviceId.innerMacAddress)
        deviceMap.putString("device_name", mBluetoothDevice!!.name)
        return deviceMap
    }
}
