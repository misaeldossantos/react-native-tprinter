import {connectToHost} from './utils/net-connect';
import {NativeEventEmitter, NativeModules, Platform} from 'react-native';
import {processColumnText} from './utils/print-column';
import {ColumnAlignment, INetPrinter, PrinterImageOptions, PrinterOptions} from './printers';
import {billTo64Buffer, textPreprocessingIOS, textTo64Buffer} from './printers';

const RNNetPrinter = NativeModules.RNNetPrinter;

const NetPrinter = {
    init: (): Promise<void> =>
        new Promise((resolve, reject) =>
            RNNetPrinter.init(
                () => resolve(),
                (error: Error) => reject(error)
            )
        ),

    getDeviceList: (): Promise<INetPrinter[]> =>
        new Promise((resolve, reject) =>
            RNNetPrinter.getDeviceList(
                (printers: INetPrinter[]) => resolve(printers),
                (error: Error) => reject(error)
            )
        ),

    connectPrinter: (
        host: string,
        port: number,
        timeout?: number
    ): Promise<INetPrinter> =>
        new Promise(async (resolve, reject) => {
            try {
                await connectToHost(host, timeout);
                RNNetPrinter.connectPrinter(
                    host,
                    port,
                    (printer: INetPrinter) => resolve(printer),
                    (error: Error) => reject(error)
                );
            } catch (error) {
                reject(error?.message || `Connect to ${host} fail`);
            }
        }),

    closeConn: (): Promise<void> =>
        new Promise((resolve) => {
            RNNetPrinter.closeConn();
            resolve();
        }),

    printText: (text: string, opts = {}): void => {
        if (Platform.OS === "ios") {
            const processedText = textPreprocessingIOS(text, false, false);
            RNNetPrinter.printRawData(
                processedText.text,
                processedText.opts,
                (error: Error) => console.warn(error)
            );
        } else {
            RNNetPrinter.printRawData(textTo64Buffer(text, opts), (error: Error) =>
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
            RNNetPrinter.printRawData(
                processedText.text,
                processedText.opts,
                (error: Error) => console.warn(error)
            );
        } else {
            RNNetPrinter.printRawData(billTo64Buffer(text, opts), (error: Error) =>
                console.warn(error)
            );
        }
    },
    /**
     * image url
     * @param imgUrl
     * @param opts
     */
    printImage: function (imgUrl: string, opts: PrinterImageOptions = {}) {
        if (Platform.OS === "ios") {
            RNNetPrinter.printImageData(imgUrl, opts, (error: Error) =>
                console.warn(error)
            );
        } else {
            RNNetPrinter.printImageData(
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
            RNNetPrinter.printImageBase64(Base64, opts, (error: Error) =>
                console.warn(error)
            );
        } else {
            RNNetPrinter.printImageBase64(
                Base64,
                opts?.imageWidth ?? 0,
                opts?.imageHeight ?? 0,
                (error: Error) => console.warn(error)
            );
        }
    },

    /**
     * Android print with encoder
     * @param text
     */
    printRaw: (text: string): void => {
        if (Platform.OS === "ios") {
        } else {
            RNNetPrinter.printRawData(text, (error: Error) => console.warn(error));
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
        columnStyle: string[] = [],
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
            RNNetPrinter.printRawData(
                processedText.text,
                processedText.opts,
                (error: Error) => console.warn(error)
            );
        } else {
            RNNetPrinter.printRawData(textTo64Buffer(result, opts), (error: Error) =>
                console.warn(error)
            );
        }
    },
};

export const NetPrinterEventEmitter =
    Platform.OS === "ios"
        ? new NativeEventEmitter(RNNetPrinter)
        : new NativeEventEmitter();

export default NetPrinter
