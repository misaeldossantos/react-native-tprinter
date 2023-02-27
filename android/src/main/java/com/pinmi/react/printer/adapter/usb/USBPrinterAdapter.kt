package com.pinmi.react.printer.adapter.usb

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.*
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.pinmi.react.printer.adapter.ImageUtils
import com.pinmi.react.printer.adapter.abstracts.PrinterAdapter
import com.pinmi.react.printer.adapter.abstracts.PrinterDevice
import com.pinmi.react.printer.adapter.abstracts.PrinterDeviceId
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by misaeldossantos on 2017/9/20.
 */
class USBPrinterAdapter private constructor() : PrinterAdapter {
    private val LOG_TAG = "RNUSBPrinter"
    private var mContext: Context? = null
    private var mUSBManager: UsbManager? = null
    private var mPermissionIndent: PendingIntent? = null
    private var mUsbDevice: UsbDevice? = null
    private var mUsbDeviceConnection: UsbDeviceConnection? = null
    private var mUsbInterface: UsbInterface? = null
    private var mEndPoint: UsbEndpoint? = null
    private val mUsbDeviceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        assert(usbDevice != null)
                        Log.i(LOG_TAG, "success to grant permission for device " + usbDevice!!.deviceId + ", vendor_id: " + usbDevice.vendorId + " product_id: " + usbDevice.productId)
                        mUsbDevice = usbDevice
                    } else {
                        assert(usbDevice != null)
                        Toast.makeText(context, "User refuses to obtain USB device permissions" + usbDevice!!.deviceName, Toast.LENGTH_LONG).show()
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                if (mUsbDevice != null) {
                    Toast.makeText(context, "USB device has been turned off", Toast.LENGTH_LONG).show()
                    closeConnectionIfExists()
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == action || UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                synchronized(this) {
                    if (mContext != null) {
                        (mContext as ReactApplicationContext).getJSModule(RCTDeviceEventEmitter::class.java)
                            .emit(EVENT_USB_DEVICE_ATTACHED, null)
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun init(reactContext: ReactApplicationContext?, successCallback: Callback, errorCallback: Callback?) {
        mContext = reactContext
        mUSBManager = mContext!!.getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIndent = PendingIntent.getBroadcast(mContext, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        mContext!!.registerReceiver(mUsbDeviceReceiver, filter)
        Log.v(LOG_TAG, "RNUSBPrinter initialized")
        successCallback.invoke()
    }

    override fun closeConnectionIfExists() {
        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection!!.releaseInterface(mUsbInterface)
            mUsbDeviceConnection!!.close()
            mUsbInterface = null
            mEndPoint = null
            mUsbDeviceConnection = null
        }
    }

    override fun getDeviceList(errorCallback: Callback): List<PrinterDevice> {
        val lists: MutableList<PrinterDevice> = ArrayList()
        if (mUSBManager == null) {
            errorCallback.invoke("USBManager is not initialized while get device list")
            return lists
        }
        for (usbDevice in mUSBManager!!.deviceList.values) {
            lists.add(USBPrinterDevice(usbDevice))
        }
        return lists
    }

    override fun selectDevice(printerDeviceId: PrinterDeviceId, successCallback: Callback, errorCallback: Callback) {
        if (mUSBManager == null) {
            errorCallback.invoke("USBManager is not initialized before select device")
            return
        }
        val usbPrinterDeviceId = printerDeviceId as USBPrinterDeviceId
        if (mUsbDevice != null && mUsbDevice!!.vendorId == usbPrinterDeviceId.vendorId && mUsbDevice!!.productId == usbPrinterDeviceId.productId) {
            Log.i(LOG_TAG, "already selected device, do not need repeat to connect")
            if (!mUSBManager!!.hasPermission(mUsbDevice)) {
                closeConnectionIfExists()
                mUSBManager!!.requestPermission(mUsbDevice, mPermissionIndent)
            }
            successCallback.invoke(USBPrinterDevice(mUsbDevice!!).toRNWritableMap())
            return
        }
        closeConnectionIfExists()
        if (mUSBManager!!.deviceList.size == 0) {
            errorCallback.invoke("Device list is empty, can not choose device")
            return
        }
        for (usbDevice in mUSBManager!!.deviceList.values) {
            if (usbDevice.vendorId == usbPrinterDeviceId.vendorId && usbDevice.productId == usbPrinterDeviceId.productId) {
                Log.v(LOG_TAG, "request for device: vendor_id: " + usbPrinterDeviceId.vendorId + ", product_id: " + usbPrinterDeviceId.productId)
                closeConnectionIfExists()
                mUSBManager!!.requestPermission(usbDevice, mPermissionIndent)
                successCallback.invoke(USBPrinterDevice(usbDevice).toRNWritableMap())
                return
            }
        }
        errorCallback.invoke("can not find specified device")
        return
    }

    private fun openConnection(): Boolean {
        if (mUsbDevice == null) {
            Log.e(LOG_TAG, "USB Deivce is not initialized")
            return false
        }
        if (mUSBManager == null) {
            Log.e(LOG_TAG, "USB Manager is not initialized")
            return false
        }
        if (mUsbDeviceConnection != null) {
            Log.i(LOG_TAG, "USB Connection already connected")
            return true
        }
        val usbInterface = mUsbDevice!!.getInterface(0)
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    val usbDeviceConnection = mUSBManager!!.openDevice(mUsbDevice)
                    if (usbDeviceConnection == null) {
                        Log.e(LOG_TAG, "failed to open USB Connection")
                        return false
                    }
                    return if (usbDeviceConnection.claimInterface(usbInterface, true)) {
                        mEndPoint = ep
                        mUsbInterface = usbInterface
                        mUsbDeviceConnection = usbDeviceConnection
                        Log.i(LOG_TAG, "Device connected")
                        true
                    } else {
                        usbDeviceConnection.close()
                        Log.e(LOG_TAG, "failed to claim usb connection")
                        false
                    }
                }
            }
        }
        return true
    }

    override fun printRawData(data: String, errorCallback: Callback) {
        Log.v(LOG_TAG, "start to print raw data $data")
        val isConnected = openConnection()
        if (isConnected) {
            Log.v(LOG_TAG, "Connected to device")
            Thread {
                val bytes = Base64.decode(data, Base64.DEFAULT)
                val b = mUsbDeviceConnection!!.bulkTransfer(mEndPoint, bytes, bytes.size, 100000)
                Log.i(LOG_TAG, "Return Status: b-->$b")
            }.start()
        } else {
            val msg = "failed to connected to device"
            Log.v(LOG_TAG, msg)
            errorCallback.invoke(msg)
        }
    }

    override fun printImageData(imageUrl: String?, imageWidth: Int, imageHeight: Int) {
        val bitmapImage = getBitmapFromURL(imageUrl)
        if (bitmapImage == null) {
            throw IllegalArgumentException("Image not found")
        }

        Log.v(LOG_TAG, "start to print image data $bitmapImage")
        val isConnected = openConnection()
        if (isConnected) {
            Log.v(LOG_TAG, "Connected to device")
            val pixels = ImageUtils.getPixelsSlow(bitmapImage, imageWidth, imageHeight)
            var b = mUsbDeviceConnection!!.bulkTransfer(mEndPoint, SET_LINE_SPACE_24, SET_LINE_SPACE_24.size, 100000)
            b = mUsbDeviceConnection!!.bulkTransfer(mEndPoint, CENTER_ALIGN, CENTER_ALIGN.size, 100000)
            var y = 0
            while (y < pixels.size) {

                // Like I said before, when done sending data,
                // the printer will resume to normal text printing
                mUsbDeviceConnection!!.bulkTransfer(mEndPoint, SELECT_BIT_IMAGE_MODE, SELECT_BIT_IMAGE_MODE.size, 100000)

                // Set nL and nH based on the width of the image
                val row = byteArrayOf((0x00ff and pixels[y].size).toByte(), (0xff00 and pixels[y].size shr 8).toByte())
                mUsbDeviceConnection!!.bulkTransfer(mEndPoint, row, row.size, 100000)
                for (x in pixels[y].indices) {
                    // for each stripe, recollect 3 bytes (3 bytes = 24 bits)
                    val slice = ImageUtils.recollectSlice(y, x, pixels)
                    mUsbDeviceConnection!!.bulkTransfer(mEndPoint, slice, slice.size, 100000)
                }

                // Do a line feed, if not the printing will resume on the same line
                mUsbDeviceConnection!!.bulkTransfer(mEndPoint, LINE_FEED, LINE_FEED.size, 100000)
                y += 24
            }
            mUsbDeviceConnection!!.bulkTransfer(mEndPoint, SET_LINE_SPACE_32, SET_LINE_SPACE_32.size, 100000)
            mUsbDeviceConnection!!.bulkTransfer(mEndPoint, LINE_FEED, LINE_FEED.size, 100000)
        } else {
            val msg = "Failed to connected to device"
            Log.v(LOG_TAG, msg)
            throw IOException(msg)
        }
    }

    override fun printImageBase64(bitmapImage: Bitmap?, imageWidth: Int, imageHeight: Int) {
        if (bitmapImage == null) {
            throw IllegalArgumentException("Image not found")
        }
        Log.v(LOG_TAG, "start to print image data $bitmapImage")
        val isConnected = openConnection()
        if (isConnected) {
            Log.v(LOG_TAG, "Connected to device")
            val pixels = ImageUtils.getPixelsSlow(bitmapImage, imageWidth, imageHeight)
            var b = mUsbDeviceConnection!!.bulkTransfer(mEndPoint, SET_LINE_SPACE_24, SET_LINE_SPACE_24.size, 100000)
            b = mUsbDeviceConnection!!.bulkTransfer(mEndPoint, CENTER_ALIGN, CENTER_ALIGN.size, 100000)
            var y = 0
            while (y < pixels.size) {

                // Like I said before, when done sending data,
                // the printer will resume to normal text printing
                mUsbDeviceConnection!!.bulkTransfer(mEndPoint, SELECT_BIT_IMAGE_MODE, SELECT_BIT_IMAGE_MODE.size, 100000)

                // Set nL and nH based on the width of the image
                val row = byteArrayOf((0x00ff and pixels[y].size).toByte(), (0xff00 and pixels[y].size shr 8).toByte())
                mUsbDeviceConnection!!.bulkTransfer(mEndPoint, row, row.size, 100000)
                for (x in pixels[y].indices) {
                    // for each stripe, recollect 3 bytes (3 bytes = 24 bits)
                    val slice = ImageUtils.recollectSlice(y, x, pixels)
                    mUsbDeviceConnection!!.bulkTransfer(mEndPoint, slice, slice.size, 100000)
                }

                // Do a line feed, if not the printing will resume on the same line
                mUsbDeviceConnection!!.bulkTransfer(mEndPoint, LINE_FEED, LINE_FEED.size, 100000)
                y += 24
            }
            mUsbDeviceConnection!!.bulkTransfer(mEndPoint, SET_LINE_SPACE_32, SET_LINE_SPACE_32.size, 100000)
            mUsbDeviceConnection!!.bulkTransfer(mEndPoint, LINE_FEED, LINE_FEED.size, 100000)
        } else {
            val msg = "Failed to connected to device"
            Log.v(LOG_TAG, msg)
            throw IOException(msg)
        }
    }

    override fun getStatus(): String {
        TODO("Not implemented")
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mInstance: USBPrinterAdapter? = null
        private const val ACTION_USB_PERMISSION = "com.pinmi.react.USBPrinter.USB_PERMISSION"
        private const val EVENT_USB_DEVICE_ATTACHED = "usbAttached"
        private const val ESC_CHAR = 0x1B.toChar()
        private val SELECT_BIT_IMAGE_MODE = byteArrayOf(0x1B, 0x2A, 33)
        private val SET_LINE_SPACE_24 = byteArrayOf(ESC_CHAR.code.toByte(), 0x33, 24)
        private val SET_LINE_SPACE_32 = byteArrayOf(ESC_CHAR.code.toByte(), 0x33, 32)
        private val LINE_FEED = byteArrayOf(0x0A)
        private val CENTER_ALIGN = byteArrayOf(0x1B, 0X61, 0X31)
        val instance: USBPrinterAdapter?
            get() {
                if (mInstance == null) {
                    mInstance = USBPrinterAdapter()
                }
                return mInstance
            }

        fun getBitmapFromURL(src: String?): Bitmap? {
            return try {
                val url = URL(src)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val myBitmap = BitmapFactory.decodeStream(input)
                val baos = ByteArrayOutputStream()
                myBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                myBitmap
            } catch (e: IOException) {
                // Log exception
                null
            }
        }
    }
}
