import * as EPToolkit from './utils/EPToolkit';

export interface PrinterOptions {
    beep?: boolean;
    cut?: boolean;
    tailingLine?: boolean;
    encoding?: string;
}

export enum PrinterWidth {
    "58mm" = 58,
    "80mm" = 80,
}

export interface PrinterImageOptions {
    beep?: boolean;
    cut?: boolean;
    tailingLine?: boolean;
    encoding?: string;
    imageWidth?: number;
    imageHeight?: number;
    printerWidthType?: PrinterWidth;
    // only ios
    paddingX?: number;
}

export interface IUSBPrinter {
    device_name: string;
    vendor_id: string;
    product_id: string;
}

export interface IBLEPrinter {
    device_name: string;
    inner_mac_address: string;
}

export interface INetPrinter {
    host: string;
    port: number;
}

export enum ColumnAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

export const textTo64Buffer = (text: string, opts: PrinterOptions) => {
    const defaultOptions = {
        beep: false,
        cut: false,
        tailingLine: false,
        encoding: "UTF8",
    };

    const options = {
        ...defaultOptions,
        ...opts,
    };

    const fixAndroid = "\n";
    const buffer = EPToolkit.exchange_text(text + fixAndroid, options);
    return buffer.toString("base64");
};

export const billTo64Buffer = (text: string, opts: PrinterOptions) => {
    const defaultOptions = {
        beep: true,
        cut: true,
        encoding: "UTF8",
        tailingLine: true,
    };
    const options = {
        ...defaultOptions,
        ...opts,
    };
    const buffer = EPToolkit.exchange_text(text, options);
    return buffer.toString("base64");
};

export const textPreprocessingIOS = (text: string, canCut = true, beep = true) => {
    let options = {
        beep: beep,
        cut: canCut,
    };
    return {
        text: text
            .replace(/<\/?CB>/g, "")
            .replace(/<\/?CM>/g, "")
            .replace(/<\/?CD>/g, "")
            .replace(/<\/?C>/g, "")
            .replace(/<\/?D>/g, "")
            .replace(/<\/?B>/g, "")
            .replace(/<\/?M>/g, ""),
        opts: options,
    };
};
