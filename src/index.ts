import {COMMANDS} from "./utils/printer-commands";
import NetPrinter, { NetPrinterEventEmitter } from './NetPrinter'
import USBPrinter from './USBPrinter'
import BLEPrinter from './BLEPrinter'

export enum RN_THERMAL_RECEIPT_PRINTER_EVENTS {
    EVENT_NET_PRINTER_SCANNED_SUCCESS = "scannerResolved",
    EVENT_NET_PRINTER_SCANNING = "scannerRunning",
    EVENT_NET_PRINTER_SCANNED_ERROR = "registerError",
}


export {COMMANDS, NetPrinter, BLEPrinter, USBPrinter, NetPrinterEventEmitter};

