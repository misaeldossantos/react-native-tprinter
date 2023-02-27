package com.pinmi.react.printer

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * Created by misaeldossantos on 2023/02/25.
 */
class RNPrinterPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(
            *arrayOf<NativeModule>(
                RNUSBPrinterModule(reactContext),
                RNBLEPrinterModule(reactContext),
                RNNetPrinterModule(reactContext)
            )
        )
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
