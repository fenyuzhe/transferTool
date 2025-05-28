package com.protoss.tansfertool.codec;

import com.protoss.tansfertool.dcm4chex.UIDsx;
import org.dcm4cheri.imageio.plugins.DcmImageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.UIDs;
import org.dcm4che.imageio.plugins.DcmMetadata;

import static com.protoss.tansfertool.util.TransferFileUtil.copyFile;

public class TranscoderMain {
    private static Logger log = LoggerFactory.getLogger(TranscoderMain.class);
    private static ResourceBundle rb =
            ResourceBundle.getBundle("TranscoderMain");

//    public static void main(String[] args) {
//        System.out.println(System.currentTimeMillis());
//        //transcode(new File("d:/2003.dcm"), new File("d:/2003.dcm"), "ImplicitVRLittleEndian");//"ImplicitVRLittleEndian");//
//        File file  = new File("D:\\pacsimage\\CT\\20230523\\0000052030\\6");
//        for (File f : file.listFiles()) {
//            transcode(f, new File("D:\\pacsimage\\imageioCompression\\CT\\0000052030\\6\\"+f.getName()), "JPEG2000Lossless");
//        }
////        transcode(new File("e:/123.dcm"), new File("e:/1_3_1.dcm"), "JPEG2000Lossless");//"JPEG2000Lossy;1.4");//
////        transcode(new File("g:/1.dcm"), new File("g:/1_1.dcm"), "JPEG2000Lossy;2.9");
//        //transcode(new File("e:/1_3_1.dcm"), new File("e:/1_3.dcm"), "ImplicitVRLittleEndian");
//    	/*
//        try {
//        File file = new File("e:/1_3.dcm");
//        byte buffer[] = new byte[(int) file.length()];
//        BufferedInputStream input;
//        input = new BufferedInputStream(
//        new FileInputStream(file));
//        input.read(buffer, 0, buffer.length);
//        input.close();
//        buffer = TranscoderMain.transcode(buffer, "JPEG2000Lossy;2.9");
//        BufferedOutputStream output = new BufferedOutputStream(
//        new FileOutputStream(new File("e:/1_3_1.dcm")));
//        output.write(buffer);
//        output.close();
//        } catch (FileNotFoundException e) {
//        e.printStackTrace();
//        } catch (IOException e) {
//        e.printStackTrace();
//        }
//         */
//        System.out.println(System.currentTimeMillis());
//        if (1 == 1) {
//            return;
//        }
//
//        //args = new String[]{"--j2kr", "d:/401_10.dcm", "d:/c2.dcm"};
//        int c;
//        //String arg;
//        LongOpt[] longopts = {
//                new LongOpt("trunc-post-pixeldata", LongOpt.NO_ARGUMENT, null, 't'),
//                new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'),
//                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
//                new LongOpt("ivle", LongOpt.NO_ARGUMENT, null, 'd'),
//                new LongOpt("evle", LongOpt.NO_ARGUMENT, null, 'e'),
//                new LongOpt("evbe", LongOpt.NO_ARGUMENT, null, 'b'),
//                new LongOpt("jpll", LongOpt.NO_ARGUMENT, null, 'l'),
//                new LongOpt("jlsl", LongOpt.NO_ARGUMENT, null, 's'),
//                new LongOpt("j2kr", LongOpt.NO_ARGUMENT, null, 'r'),
//                new LongOpt("jply", LongOpt.OPTIONAL_ARGUMENT, null, 'y'),
//                new LongOpt("j2ki", LongOpt.OPTIONAL_ARGUMENT, null, 'i'),};
//        //
//        Getopt g = new Getopt("dcm4chex-codec", args, "jhv", longopts, true);
//        Transcoder t = new Transcoder();
//        while ((c = g.getopt()) != -1) {
//            switch (c) {
//                case 'd':
//                    t.setTransferSyntax(UIDs.ImplicitVRLittleEndian);
//                    break;
//                case 'e':
//                    t.setTransferSyntax(UIDs.ExplicitVRLittleEndian);
//                    break;
//                case 'b':
//                    t.setTransferSyntax(UIDs.ExplicitVRBigEndian);
//                    break;
//                case 'l':
//                    t.setTransferSyntax(UIDs.JPEGLossless);
//                    break;
//                case 's':
//                    t.setTransferSyntax(UIDs.JPEGLSLossless);
//                    break;
//                case 'r':
//                    t.setTransferSyntax(UIDs.JPEG2000Lossless);
//                    break;
//                case 'y':
//                    t.setTransferSyntax(UIDs.JPEGBaseline);
//                    t.setCompressionQuality(
//                            toCompressionQuality(g.getOptarg()));
//                    break;
//                case 'i':
//                    t.setTransferSyntax(UIDs.JPEG2000Lossy);
//                    t.setEncodingRate(toEncodingRate(g.getOptarg()));
//                    break;
//                case 't':
//                    t.setTruncatePostPixelData(true);
//                    break;
//                case 'v':
//                    System.out.println(
//                            MessageFormat.format(
//                                    rb.getString("version"),
//                                    new Object[]{
//                                            Package.getPackage("org.dcm4chex.codec").getImplementationVersion()}));
//                    return;
//                case '?':
//                case 'h':
//                    System.out.println(rb.getString("usage"));
//                    return;
//            }
//        }
//        if (!checkArgs(g.getOptind(), args)) {
//            System.out.println(rb.getString("usage"));
//            return;
//        }
//        File dest = new File(args[args.length - 1]);
//        for (int i = g.getOptind(); i + 1 < args.length; ++i) {
//            transcode(t, new File(args[i]), dest);
//        }
//    }

    public Dataset fileToDataset(File f) {
        BufferedInputStream in = null;
        FileFormat ff = null;
        DcmParser parser = null;
        Dataset ds = null;

        try {
            in = new BufferedInputStream(new FileInputStream(f));
        } catch (IOException ioe) {
            System.out.println("Can't read file: " + f.getPath());
            return null;
        }

        try {
            parser = DcmParserFactory.getInstance().newDcmParser(in);
            try {
                ff = parser.detectFileFormat();
            } catch (Exception ioe) {
                System.out.println("Can't detect DICOM file format for file: " + f.getPath());
                // Try next file
                return null;
            }

            ImageInputStream iis = null;
            try {
                iis = ImageIO.createImageInputStream(f);
                Iterator iter = ImageIO.getImageReadersByFormatName("DICOM");
                ImageReader reader = null;
                while (iter.hasNext()) {
                    ImageReader it = (ImageReader) iter.next();
                    if (it instanceof DcmImageReader) {
                        reader = it;
                    }
                }
                reader.setInput(iis, false);
                ds = ((DcmMetadata) reader.getStreamMetadata()).getDataset();
            } catch (IOException e) {
                log.error(e.getMessage());
            } finally {
                if (iis != null) {
                    try {
                        iis.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }

            /*
            ds = DcmObjectFactory.getInstance().newDataset();

            try {
            ds.readFile(in, ff, -1);
            } catch (IOException ioe) {
            System.out.println("Can't create Dataset for file: " + f.getPath());
            // Try next file
            return null;
            }*/
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }

        return ds;
    }

    public Dataset byteToDataset(byte[] src_byte) {
        ByteArrayInputStream in = null;
        FileFormat ff = null;
        DcmParser parser = null;
        Dataset ds = null;

        in = new ByteArrayInputStream(src_byte);

        try {
            parser = DcmParserFactory.getInstance().newDcmParser(in);

            try {
                ff = parser.detectFileFormat();
            } catch (Exception ioe) {
                System.out.println("Can't detect DICOM file format for file: " + src_byte.length);
                // Try next file
                return null;
            }

            ImageInputStream iis = null;
            try {
                iis = ImageIO.createImageInputStream(in);
                Iterator iter = ImageIO.getImageReadersByFormatName("DICOM");
                ImageReader reader = null;
                while (iter.hasNext()) {
                    ImageReader it = (ImageReader) iter.next();
                    if (it instanceof DcmImageReader) {
                        reader = it;
                    }
                }
                reader.setInput(iis, false);

                ds = ((DcmMetadata) reader.getStreamMetadata()).getDataset();

            } catch (IOException e) {
                log.error(e.getMessage());
            } finally {
                if (iis != null) {
                    try {
                        iis.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }

            /*
            ds = DcmObjectFactory.getInstance().newDataset();

            try {
            ds.readFile(in, ff, -1);
            } catch (IOException ioe) {
            System.out.println("Can't create Dataset for file: " + f.getPath());
            // Try next file
            return null;
            }*/
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }

        return ds;
    }

    // 将一个在内存中的dicom文件src_byte，转换成另一个文件，返回
    public byte[] transcode(byte[] src_byte, String transparam) {
        String[] params = transparam.split(";");
        String transUID = params[0].equals("") ? null : UIDsx.forName(params[0]);

        Dataset ds_in = byteToDataset(src_byte);
        if (ds_in == null) {
            return null;
        }

        if ((transUID == null) || transUID.equals(ds_in.getFileMetaInfo().getTransferSyntaxUID())) {
            log.info("copy file without transcode");
            return src_byte;
        }
        log.info("copy file with transcode" + transparam);
        Transcoder t = new Transcoder();
        t.setTransferSyntax(transUID);

        if (transUID.equals(UIDsx.JPEGBaseline)) {
            t.setCompressionQuality(
                    toCompressionQuality(params.length >= 2 ? params[1] : null));
        } else if (transUID.equals(UIDsx.JPEG2000Lossy)) {
            t.setEncodingRate(toEncodingRate(params.length >= 2 ? params[1] : null));
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        transcode(t, src_byte, bo);
        return bo.toByteArray();
    }

    public void transcode(Transcoder t, byte[] src_byte, ByteArrayOutputStream dest) {
        ByteArrayInputStream in = new ByteArrayInputStream(src_byte);
        try {
            long srcLength = src_byte.length;
            System.out.print(
                    ""
                            + " ["
                            + (srcLength >>> 10)
                            + " KB] -> ");
            long begin = System.currentTimeMillis();
            t.transcode(in, dest);
            long end = System.currentTimeMillis();
            long destLength = dest.toByteArray().length;
            System.out.println(" [" + (destLength >>> 10) + " KB] ");
            System.out.println(
                    "  takes "
                            + (end - begin)
                            + " ms, compression rate= "
                            + ((destLength < srcLength)
                            ? (srcLength / (float) destLength) + " : 1"
                            : "1 : " + (destLength / (float) srcLength)));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * @param src 源文件
     * @param dest 转换后文件
     * @param transparam 转换参数,包括转换方法和参数
     */
    public synchronized void transcode(File src, File dest, String transparam) {
        String[] params = transparam.split(";");
        String transUID = params[0].equals("") ? null : UIDsx.forName(params[0]);

        Dataset ds_in = fileToDataset(src);
        if (ds_in == null) {
            return;
        }

        if ((transUID == null) || transUID.equals(ds_in.getFileMetaInfo().getTransferSyntaxUID())) {
            try {
                log.info("copy file without transcode" + src + "to" + dest);
                copyFile(src, dest);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return;
        }
        log.info("copy file with transcode " + transparam);
        Transcoder t = new Transcoder();
        t.setTransferSyntax(transUID);

        if (transUID.equals(UIDsx.JPEGBaseline)) {
            t.setCompressionQuality(
                    toCompressionQuality(params.length >= 2 ? params[1] : null));
        } else if (transUID.equals(UIDsx.JPEG2000Lossy)) {
            t.setEncodingRate(toEncodingRate(params.length >= 2 ? params[1] : null));
        }
        transcode(t, src, dest);
    }

    public void transcode(Transcoder t, File src, File dest) {
        if (src.isDirectory()) {
            File[] file = src.listFiles();
            for (int i = 0; i < file.length; i++) {
                transcode(t, file[i], dest);
            }
        } else {
            //Dataset ds_in = fileToDataset(src);
            //System.out.println( ds_in.getFileMetaInfo().getTransferSyntaxUID());

            try {
                File outFile =
                        dest.isDirectory() ? new File(dest, src.getName()) : dest;
                long srcLength = src.length();
                log.info(
                        ""
                                + src
                                + " ["
                                + (srcLength >>> 10)
                                + " KB] -> "
                                + outFile);
                long begin = System.currentTimeMillis();
                t.transcode(src, outFile);
                long end = System.currentTimeMillis();
                long destLength = outFile.length();
                log.info(" [" + (destLength >>> 10) + " KB] ");
                log.info(
                        "  takes "
                                + (end - begin)
                                + " ms, compression rate= "
                                + ((destLength < srcLength)
                                ? (srcLength / (float) destLength) + " : 1"
                                : "1 : " + (destLength / (float) srcLength)));
            } catch (Exception e) {
                e.printStackTrace(System.out);
                log.error("trancode file failed :"+e);
                try {
                    log.info("trancode file failed copy file without not transcode" + src + "to" + dest);
                    copyFile(src, dest);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        }
    }

    public boolean checkArgs(int off, String[] args) {
        switch (args.length - off) {
            case 0:
                System.out.println(rb.getString("missingArgs"));
                return false;
            case 1:
                System.out.println(rb.getString("missingDest"));
                return false;
            case 2:
                if (!(new File(args[off])).isDirectory()) {
                    break;
                }
            default:
                if (!(new File(args[args.length - 1])).isDirectory()) {
                    System.out.println(
                            MessageFormat.format(
                                    rb.getString("needDir"),
                                    new Object[]{args[args.length - 1]}));
                    return false;
                }
        }
        return true;
    }

    public float toCompressionQuality(String s) {
        if (s != null) {
            try {
                int quality = Integer.parseInt(s);
                if (quality >= 0 && quality <= 100) {
                    return quality / 100.f;
                }
            } catch (IllegalArgumentException e) {
            }
            System.out.println(
                    MessageFormat.format(
                            rb.getString("ignoreQuality"),
                            new Object[]{s}));
        }
        return .75f;
    }

    public double toEncodingRate(String s) {
        if (s != null) {
            try {
                double rate = Double.parseDouble(s);
                if (rate > 0) {
                    return rate;
                }
            } catch (IllegalArgumentException e) {
            }
            System.out.println(
                    MessageFormat.format(
                            rb.getString("ignoreRate"),
                            new Object[]{s}));
        }
        return 1.;
    }
}
