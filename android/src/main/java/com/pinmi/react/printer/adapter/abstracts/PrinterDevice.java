package com.pinmi.react.printer.adapter.abstracts;

import com.facebook.react.bridge.WritableMap;

/**
 * Created by xiesubin on 2017/9/21.
 */

public interface PrinterDevice {

    public PrinterDeviceId getPrinterDeviceId();
    public WritableMap toRNWritableMap();

}
