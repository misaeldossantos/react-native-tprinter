package com.pinmi.react.printer.adapter.net

import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class NetPrinterDeviceId private constructor(val host: String?, val port: Int?) : PrinterDeviceId() {

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        if (!super.equals(o)) return false
        val that = o as NetPrinterDeviceId
        return if (host != that.host) false else port == that.port
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port.hashCode()
        return result
    }

    companion object {
        fun valueOf(host: String?, port: Int?): NetPrinterDeviceId {
            return NetPrinterDeviceId(host, port)
        }
    }
}
