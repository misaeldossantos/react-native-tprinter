import {NativeModules, Platform} from 'react-native';
import {processColumnText} from './utils/print-column';
import {ColumnAlignment, IBLEPrinter, PrinterImageOptions, PrinterOptions} from './printers';
import {billTo64Buffer, textPreprocessingIOS, textTo64Buffer} from './printers';

const RNBLEPrinter = NativeModules.RNBLEPrinter;

const BLEPrinter = {
    init: (): Promise<void> =>
        new Promise((resolve, reject) =>
            RNBLEPrinter.init(
                () => resolve(),
                (error: Error) => reject(error)
            )
        ),

    getDeviceList: (): Promise<IBLEPrinter[]> =>
        new Promise((resolve, reject) =>
            RNBLEPrinter.getDeviceList(
                (printers: IBLEPrinter[]) => resolve(printers),
                (error: Error) => reject(error)
            )
        ),

    connectPrinter: (inner_mac_address: string): Promise<IBLEPrinter> =>
        new Promise((resolve, reject) =>
            RNBLEPrinter.connectPrinter(
                inner_mac_address,
                (printer: IBLEPrinter) => resolve(printer),
                (error: Error) => reject(error)
            )
        ),

    closeConn: (): Promise<void> =>
        new Promise((resolve) => {
            RNBLEPrinter.closeConn();
            resolve();
        }),

    printText: (text: string, opts: PrinterOptions = {}): void => {
        if (Platform.OS === "ios") {
            const processedText = textPreprocessingIOS(text, false, false);
            RNBLEPrinter.printRawData(
                processedText.text,
                processedText.opts,
                (error: Error) => console.warn(error)
            );
        } else {
            RNBLEPrinter.printRawData(textTo64Buffer(text, opts), (error: Error) =>
                console.warn(error)
            );
        }
    },

    printBill: (text: string, opts: PrinterOptions = {}): void => {
        if (Platform.OS === "ios") {
            const processedText = textPreprocessingIOS(
                text,
                opts?.cut ?? true,
                opts.beep ?? true
            );
            RNBLEPrinter.printRawData(
                processedText.text,
                processedText.opts,
                (error: Error) => console.warn(error)
            );
        } else {
            RNBLEPrinter.printRawData(billTo64Buffer(text, opts), (error: Error) =>
                console.warn(error)
            );
        }
    },
    printImage: (imgUrl: string, opts: PrinterImageOptions = {}): Promise<void> => {
        return new Promise((resolve, reject) => {
            if (Platform.OS === "ios") {
                /**
                 * just development
                 */
                RNBLEPrinter.printImageData(imgUrl, opts, resolve, reject);
            } else {
                RNBLEPrinter.printImageData(
                    imgUrl,
                    opts?.imageWidth ?? 0,
                    opts?.imageHeight ?? 0,
                    resolve,
                    reject
                );
            }
        })

    },
    /**
     * base 64 string
     * @param Base64
     * @param opts
     */
    printImageBase64: function (Base64: string, opts: PrinterImageOptions = {}): Promise<void> {
        return new Promise((resolve, reject) => {

            if (Platform.OS === "ios") {
                /**
                 * just development
                 */
                RNBLEPrinter.printImageBase64(Base64, opts, resolve, reject);
            } else {
                /**
                 * just development
                 */
                RNBLEPrinter.printImageBase64(
                    Base64,
                    opts?.imageWidth ?? 0,
                    opts?.imageHeight ?? 0,
                    resolve, reject
                );
            }
        })
    },
    /**
     * android print with encoder
     * @param text
     */
    printRaw: (text: string): void => {
        if (Platform.OS === "ios") {
        } else {
            RNBLEPrinter.printRawData(text, (error: Error) => console.warn(error));
        }
    },


    getStatus: (): Promise<"BLUETOOTH_DISABLED" | "NOT_CONNECTED" | "PAIRING" | "PAIRED"> =>
        new Promise((resolve) =>
            RNBLEPrinter.getStatus(resolve)
        ),

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
        if (Platform.OS === "ios") {
            const processedText = textPreprocessingIOS(result, false, false);
            RNBLEPrinter.printRawData(
                processedText.text,
                processedText.opts,
                (error: Error) => console.warn(error)
            );
        } else {
            RNBLEPrinter.printRawData(textTo64Buffer(result, opts), (error: Error) =>
                console.warn(error)
            );
        }
    },
};

export default BLEPrinter
