import {NativeModules, Platform} from 'react-native';
import {processColumnText} from './utils/print-column';
import {ColumnAlignment, IUSBPrinter, PrinterImageOptions, PrinterOptions} from './printers';
import {billTo64Buffer, textTo64Buffer} from './printers';

const RNUSBPrinter = NativeModules.RNUSBPrinter;

const USBPrinter = {
    init: (): Promise<void> =>
        new Promise((resolve, reject) =>
            RNUSBPrinter.init(
                () => resolve(),
                (error: Error) => reject(error)
            )
        ),

    getDeviceList: (): Promise<IUSBPrinter[]> =>
        new Promise((resolve, reject) =>
            RNUSBPrinter.getDeviceList(
                (printers: IUSBPrinter[]) => resolve(printers),
                (error: Error) => reject(error)
            )
        ),

    connectPrinter: (vendorId: string, productId: string): Promise<IUSBPrinter> =>
        new Promise((resolve, reject) =>
            RNUSBPrinter.connectPrinterWithVendor(
                vendorId,
                productId,
                (printer: IUSBPrinter) => resolve(printer),
                (error: Error) => reject(error)
            )
        ),

    closeConn: (): Promise<void> =>
        new Promise((resolve) => {
            RNUSBPrinter.closeConn();
            resolve();
        }),

    printText: (text: string, opts: PrinterOptions = {}): void =>
        RNUSBPrinter.printRawData(textTo64Buffer(text, opts), (error: Error) =>
            console.warn(error)
        ),

    printBill: (text: string, opts: PrinterOptions = {}): void =>
        RNUSBPrinter.printRawData(billTo64Buffer(text, opts), (error: Error) =>
            console.warn(error)
        ),
    /**
     * image url
     * @param imgUrl
     * @param opts
     */
    printImage: function (imgUrl: string, opts: PrinterImageOptions = {}) {
        if (Platform.OS === "ios") {
            RNUSBPrinter.printImageData(imgUrl, opts, (error: Error) =>
                console.warn(error)
            );
        } else {
            RNUSBPrinter.printImageData(
                imgUrl,
                opts?.imageWidth ?? 0,
                opts?.imageHeight ?? 0,
                (error: Error) => console.warn(error)
            );
        }
    },
    /**
     * base 64 string
     * @param Base64
     * @param opts
     */
    printImageBase64: function (Base64: string, opts: PrinterImageOptions = {}) {
        if (Platform.OS === "ios") {
            RNUSBPrinter.printImageBase64(Base64, opts, (error: Error) =>
                console.warn(error)
            );
        } else {
            RNUSBPrinter.printImageBase64(
                Base64,
                opts?.imageWidth ?? 0,
                opts?.imageHeight ?? 0,
                (error: Error) => console.warn(error)
            );
        }
    },
    /**
     * android print with encoder
     * @param text
     */
    printRaw: (text: string): void => {
        if (Platform.OS === "ios") {
        } else {
            RNUSBPrinter.printRawData(text, (error: Error) => console.warn(error));
        }
    },
    /**
     * `columnWidth`
     * 80mm => 46 character
     * 58mm => 30 character
     */
    printColumnsText: (
        texts: string[],
        columnWidth: number[],
        columnAlignment: ColumnAlignment[],
        columnStyle: string[],
        opts: PrinterOptions = {}
    ): void => {
        const result = processColumnText(
            texts,
            columnWidth,
            columnAlignment,
            columnStyle
        );
        RNUSBPrinter.printRawData(textTo64Buffer(result, opts), (error: Error) =>
            console.warn(error)
        );
    },
};

export default USBPrinter
