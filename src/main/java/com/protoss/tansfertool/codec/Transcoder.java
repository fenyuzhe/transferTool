package com.protoss.tansfertool.codec;

import com.protoss.tansfertool.dcm4cheri.MyImageWriterFactory;
import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.imageio.stream.SegmentedImageInputStream;
import org.dcm4che.data.*;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.util.UIDGenerator;
import org.dcm4cheri.image.ImageReaderFactory;
import org.dcm4cheri.image.ImageWriterFactory;
import org.dcm4cheri.image.ItemParser;
import org.dcm4cheri.imageio.plugins.DcmImageReader;
import org.dcm4cheri.imageio.plugins.DcmImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.nio.ByteOrder;
import java.util.Hashtable;

public class Transcoder {
    private static final String RGB = "RGB";
    private static final String YBR_FULL_422 = "YBR_FULL_422";
    private static final String YBR_RCT = "YBR_RCT";
    private static final String YBR_ICT = "YBR_ICT";

    private static Logger log = LoggerFactory.getLogger(Transcoder.class);

    private static DcmParserFactory parserFactory =
            DcmParserFactory.getInstance();

    private static DcmObjectFactory objectFactory =
            DcmObjectFactory.getInstance();

    protected static UIDGenerator uidGen = UIDGenerator.getInstance();

    private float compressionQuality = 0.75f;
    private double encodingRate = 1.;
    private ImageInputStream iis;
    private ImageOutputStream ios;
    private String encodeTS = UIDs.ExplicitVRLittleEndian;
    private DcmEncodeParam encodeParam = DcmDecodeParam.EVR_LE;
    private DcmParser parser;
    private DcmDecodeParam decodeParam;
    private PixelDataParam pixelDataParam;
    private ImageReader reader;
    private ImageWriter writer;
    private Dataset dsIn = objectFactory.newDataset();
    private Dataset dsOut = objectFactory.newDataset();
    private BufferedImage bi;
    private int frameIndex = 0;
    private boolean truncatePostPixelData = false;
    private ItemParser itemParser;
    private SegmentedImageInputStream siis;

    /**
     * if true, input stream is directly copied into output stream without pixel
     * decoding. Makes sense if output ts == input ts. cleared by readHeader if
     * no ts match.
     */
    private boolean directCopy = true;
    private boolean ignoreMissingPixelData;

    /**
     * if true, input stream is directly copied into output stream without pixel
     * decoding. Makes sense if output ts == input ts
     */
    public void setDirectCopy(boolean cp) {
        directCopy = cp;
    }

    /**
     * if true, input stream is directly copied into output stream without pixel
     * decoding. Makes sense if output ts == input ts
     */
    public boolean doDirectCopy() {
        return directCopy;
    }

    public void setInput(ImageInputStream iis) {
        this.iis = iis;
        this.bi = null;
    }

    public void setOutput(ImageOutputStream ios) {
        this.ios = ios;
    }

    public void setTransferSyntax(String transferSyntax) {
        this.encodeParam = DcmEncodeParam.valueOf(transferSyntax);
        this.encodeTS = transferSyntax;
    }

    public final float getCompressionQuality() {
        return compressionQuality;
    }

    public final void setCompressionQuality(float quality) {
        this.compressionQuality = quality;
    }

    public final double getEncodingRate() {
        return encodingRate;
    }

    public final void setEncodingRate(double rate) {
        this.encodingRate = rate;
    }

    public void setTruncatePostPixelData(boolean truncate) {
        this.truncatePostPixelData = truncate;

    }

    public boolean isTruncatePostPixelData() {
        return this.truncatePostPixelData;
    }

    public Dataset getDataset() {
        return dsIn;
    }

    public void transcode(ByteArrayInputStream infile, ByteArrayOutputStream outfile) throws Exception {
        ImageInputStream iis = null;
        ImageOutputStream ios = null;
        try {
            setInput(iis = ImageIO.createImageInputStream(infile));
            setOutput(ios = ImageIO.createImageOutputStream(outfile));

            transcode();
        } finally {
            if (iis != null) {
                try {
                    iis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ios != null) {
                try {
                    ios.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void transcode(File infile, File outfile) throws Exception {
        outfile.delete();
        ImageInputStream iis = null;
        ImageOutputStream ios = null;
        try {
            setInput(iis = ImageIO.createImageInputStream(infile));
            setOutput(ios = ImageIO.createImageOutputStream(outfile));

            transcode();
        } finally {
            if (iis != null) {
                try {
                    iis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ios != null) {
                try {
                    ios.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void transcode() throws Exception {
        if (iis == null) {
            throw new IllegalStateException("no input set");
        }
        if (ios == null) {
            throw new IllegalStateException("no output set");
        }
        // direct copy only if no truncation
        setDirectCopy(doDirectCopy() && !truncatePostPixelData);
        // parse & copy header. may clear doDirectCopy if input ts != output ts
        transcodeHeader();
        if (doDirectCopy()) // just copy without recoding
        {
            copyPixelData();
        } else {
            transcodePixelHeader();// copy intro of PixelData
            for (int i = 0, n = pixelDataParam.getNumberOfFrames(); i < n; i++) {
                transcodeNextFrame(); // recode frames
            }
            transcodePixelFooter();// finish up PixelData tag
        }
        transcodeFooter();// copy all the stuff after pixel data
    }

    private void transcodePixelFooter() throws Exception {
        if (itemParser != null) {
            itemParser.seekFooter();
        }
        if (encodeParam.encapsulated) {
            dsIn.writeHeader(ios, encodeParam, Tags.SeqDelimitationItem,
                    VRs.NONE, 0);
        }
    }

    private void transcodePixelHeader() throws Exception {
        readPixelHeader();
        writePixelHeader();
    }

    private void writePixelHeader() throws Exception {
        if (encodeParam.encapsulated) {
            dsIn.writeHeader(ios, encodeParam, Tags.PixelData, VRs.OB, -1);
            dsIn.writeHeader(ios, encodeParam, Tags.Item, VRs.NONE, 0);
//            ImageWriterFactory f = ImageWriterFactory.getInstance();
            MyImageWriterFactory f = MyImageWriterFactory.getInstance();
            writer = f.getWriterForTransferSyntax(encodeTS);
        } else {
            dsIn.writeHeader(ios, encodeParam, Tags.PixelData, VRs.OW,
                    pixelDataParam.getPixelDataLength());
        }
        log.debug("wrote header");
    }

    private void readPixelHeader() throws Exception {
        iis.setByteOrder(decodeParam.byteOrder);
        if (decodeParam.encapsulated) {
            itemParser = new ItemParser(parser);
            siis = new SegmentedImageInputStream(iis, itemParser);
            ImageReaderFactory f = ImageReaderFactory.getInstance();
            reader = f.getReaderForTransferSyntax(dsIn.getFileMetaInfo()
                    .getTransferSyntaxUID());
            bi = (BufferedImage) createBufferedImage();
        } else {
            bi = pixelDataParam.createBufferedImage(getMaxBits());
        }
        if (log.isDebugEnabled()) {
            log.debug("read header: " + pixelDataParam);
        }
    }

    /**
     * copy pixel tag without de/encoding
     */
    private void copyPixelData() throws Exception {
        if (parser.hasSeenEOF())
            return;
        int len = parser.getReadLength();
        dsIn.writeHeader(ios, encodeParam, parser.getReadTag(), parser
                .getReadVR(), len);
        if (len == -1) {
            parser.parseHeader();
            while (parser.getReadTag() == Tags.Item) {
                len = parser.getReadLength();
                dsIn.writeHeader(ios, encodeParam, Tags.Item, VRs.NONE, len);
                copy(iis, ios, len);
                parser.parseHeader();
            }
            dsIn.writeHeader(ios, encodeParam, Tags.SeqDelimitationItem,
                    VRs.NONE, 0);
        } else {
            copy(iis, ios, len);
        }
    }

    private void copy(ImageInputStream in, ImageOutputStream out, int totLen)
            throws Exception {
        int toRead = totLen == -1 ? Integer.MAX_VALUE : totLen;
        int bufferSize = 8192;
        byte[] buffer = new byte[bufferSize];
        for (int len; toRead > 0; toRead -= len) {
            len = in.read(buffer, 0, Math.min(toRead, buffer.length));
            if (len == -1) {
                if (totLen == -1) {
                    return;
                }
                throw new EOFException();
            }
            out.write(buffer, 0, len);
        }
    }

    public void transcodeHeader() throws Exception {
        readHeader();
        writeHeader();


    }

    public void readHeader() throws Exception {
        log.debug("reading header");
        parser = parserFactory.newDcmParser(iis);
        frameIndex = 0;
        dsIn.clear();
        parser.setDcmHandler(dsIn.getDcmHandler());
        parser.parseDcmFile(null, Tags.PixelData);
        if (parser.getReadTag() != Tags.PixelData) {
            if (ignoreMissingPixelData) {
                setDirectCopy(true);
            } else {
                throw new Exception("no pixel data in source object");
            }
        }
        decodeParam = parser.getDcmDecodeParam();

        dsOut.clear();
        dsOut.putAll(dsIn); // copy object, as some tags are modified by coerce

        onHeaderParsed(dsOut);
        if (doDirectCopy()) {
            FileMetaInfo fmi = dsIn.getFileMetaInfo();
            if (fmi != null) {
                setDirectCopy(encodeTS.equals(fmi.getTransferSyntaxUID()));
            } else {
                setDirectCopy(decodeParam.byteOrder == ByteOrder.LITTLE_ENDIAN
                        && encodeTS.equals(UIDs.ExplicitVRLittleEndian));
            }
        }
        pixelDataParam = new PixelDataParam(dsIn, isTypeShortSupported());

    }

    private int getMaxBits() {
        if (encodeTS.equals(UIDs.JPEGBaseline))
            return 8;
        if (encodeTS.equals(UIDs.JPEGExtended))
            return 12;
        return 16;
    }

    private boolean isTypeShortSupported() {
        return !(encodeTS.equals(UIDs.JPEGBaseline)
                || encodeTS.equals(UIDs.JPEGExtended)
                || encodeTS.equals(UIDs.JPEGLossless)
                || encodeTS.equals(UIDs.JPEGLossless14)
                || encodeTS.equals(UIDs.JPEGLSLossless) || encodeTS
                .equals(UIDs.JPEGLSLossy));
    }

    public void writeHeader() throws Exception {
        log.debug("writing header");
        coerceTS();
        ios.setByteOrder(encodeParam.byteOrder);
        if (!doDirectCopy())
            coerceDataset(dsOut);
        dsOut.writeFile(ios, encodeParam);
    }

    private void coerceTS() {
        if (encodeTS.equals(UIDs.JPEGBaseline)) {
            if (pixelDataParam.getBitsStored() > 8) {
                encodeTS = UIDs.JPEGExtended;
            }
        } else if (encodeTS.equals(UIDs.JPEGExtended)) {
            if (pixelDataParam.getBitsStored() <= 8) {
                encodeTS = UIDs.JPEGBaseline;
            }
        }
    }

    public void coerceDataset(Dataset dsOut) throws DcmValueException {
        final int samplesPerPixel = getSamplesPerPixel();
        dsOut.putUS(Tags.SamplesPerPixel, samplesPerPixel);
        dsOut.putCS(Tags.PhotometricInterpretation,
                getPhotometricInterpretation());
        if (samplesPerPixel > 1) {
            dsOut.putUS(Tags.PlanarConfiguration, getPlanarConfiguration());
        }
        if (!isCompressionLossless()) {
            dsOut.putCS(Tags.LossyImageCompression, "01");
            String[] imageTypes = dsOut.getStrings(Tags.ImageType);
            if (imageTypes == null || imageTypes.length == 0) {
                imageTypes = new String[1];
            }
            if (!"DERIVED".equals(imageTypes)) {
                imageTypes[0] = "DERIVED";
                dsOut.putCS(Tags.ImageType, imageTypes);
            }
            dsOut.putCS(Tags.LossyImageCompression, "01");
            if (encodeTS.equals(UIDs.JPEG2000Lossy)) {
                float[] oldVal = dsOut
                        .getFloats(Tags.LossyImageCompressionRatio);
                if (oldVal == null) {
                    oldVal = new float[0];
                }
                float[] newVal = new float[oldVal.length + 1];
                System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
                newVal[oldVal.length] = (float) (pixelDataParam
                        .getBitsAllocated() / getEncodingRate());
                dsOut.putDS(Tags.LossyImageCompressionRatio, newVal);
                dsOut.putST(Tags.DerivationDescription,
                        "JPKI lossy compressed "
                                + newVal[oldVal.length] + ":1");
            } else {
                dsOut.putST(Tags.DerivationDescription,
                        "JPEG lossy compressed");
            }
            DcmElement sq = dsOut.putSQ(Tags.SourceImageSeq);
            Dataset item = sq.addNewItem();
            item.putUI(Tags.RefSOPInstanceUID, dsOut
                    .getString(Tags.SOPInstanceUID));
            item.putUI(Tags.RefSOPClassUID, dsOut.getString(Tags.SOPClassUID));
            dsOut.putUI(Tags.SOPInstanceUID, uidGen.createUID());
        }
        dsOut.setFileMetaInfo(objectFactory.newFileMetaInfo(dsOut, encodeTS));
    }

    int getPlanarConfiguration() {
        if (encodeParam.encapsulated) {
            return 0;
        }
        return pixelDataParam.getPlanarConfiguration();
    }

    private String getPhotometricInterpretation() {
        if (getSamplesPerPixel() == 1) {
            return pixelDataParam.getPhotoMetricInterpretation();
        }
        if (encodeParam.encapsulated) {
            if (encodeTS.equals(UIDs.JPEGBaseline)
                    || encodeTS.equals(UIDs.JPEGExtended)) {
                return YBR_FULL_422;
            }
            if (encodeTS.equals(UIDs.JPEG2000Lossless)) {
                return YBR_RCT;
            }
            if (encodeTS.equals(UIDs.JPEG2000Lossy)) {
                return YBR_ICT;
            }
        }
        return RGB;
    }

    private int getSamplesPerPixel() {
        return pixelDataParam.getSamplesPerPixel();
    }

    public boolean hasMoreFrames() {
        if (pixelDataParam == null) {
            throw new IllegalStateException();
        }
        return frameIndex < pixelDataParam.getNumberOfFrames();
    }

    public void transcodeNextFrame() throws Exception {
        readNextFrame();
        BufferedImage outBi = onFrameDecoded(bi);
        writeNextFrame(outBi);
    }

    public void readNextFrame() throws Exception {
        log.debug("reading frame #" + (frameIndex + 1));
        if (decodeParam.encapsulated) {
            reader.setInput(siis);
            ImageReadParam param = reader.getDefaultReadParam();
            if (bi != null)
                param.setDestination(bi);
            bi = reader.read(0, param);
            itemParser.seekNextFrame(siis);
        } else {
            DataBuffer db = bi.getRaster().getDataBuffer();
            switch (db.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    read(((DataBufferByte) db).getBankData());
                    break;
                case DataBuffer.TYPE_SHORT:
                    read(((DataBufferShort) db).getBankData());
                    break;
                case DataBuffer.TYPE_USHORT:
                    read(((DataBufferUShort) db).getBankData());
                    break;
                default:
                    throw new RuntimeException("dataType:" + db.getDataType());
            }
            iis.flushBefore(iis.getStreamPosition());
        }
        if (log.isDebugEnabled()) {
            ColorModel cm = bi.getColorModel();
            ColorSpace cs = cm.getColorSpace();
            SampleModel sm = bi.getSampleModel();
            Raster raster = bi.getData();
            DataBuffer db = raster.getDataBuffer();
            log.debug("read frame #" + (frameIndex + 1) + "[bitype="
                    + bi.getType() + ", h=" + bi.getHeight() + ", w="
                    + bi.getWidth() + ", sm=" + classNameOf(sm) + ", bands="
                    + sm.getNumBands() + ", db=" + classNameOf(db) + ", banks="
                    + db.getNumBanks() + ", bits=" + cm.getPixelSize()
                    + ", cstype=" + cs.getType() + "]");
        }
        ++frameIndex;
    }

    private static String classNameOf(Object o) {
        String s = o.getClass().getName();
        return s.substring(s.lastIndexOf('.') + 1);
    }

    private void read(byte[][] data) throws Exception {
        for (int i = 0; i < data.length; i++) {
            iis.readFully(data[i], 0, data[i].length);
        }
    }

    private void read(short[][] data) throws Exception {
        for (int i = 0; i < data.length; i++) {
            iis.readFully(data[i], 0, data[i].length);
        }
    }

    public void writeNextFrame(BufferedImage bi) throws Exception {
        log.debug("writing frame #" + frameIndex);
        if (encodeParam.encapsulated) {
            long itemPos = ios.getStreamPosition();
            ios.flushBefore(itemPos);
            dsIn.writeHeader(ios, encodeParam, Tags.Item, VRs.NONE, -1);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(bi, null, null), getWriteParam());
            long endPos = ios.length();
            int itemLen = (int) (endPos - itemPos - 8);
            if ((itemLen & 1) != 0) {
                ios.write(0);
                ++itemLen;
                ++endPos;
            }
            ios.seek(itemPos);
            dsIn.writeHeader(ios, encodeParam, Tags.Item, VRs.NONE, itemLen);
            ios.seek(endPos);
            ios.flushBefore(endPos);
        } else {
            ios.flushBefore(ios.getStreamPosition());
            Raster raster = bi.getRaster();
            DataBuffer buffer = raster.getDataBuffer();
            final int stride = ((ComponentSampleModel) raster.getSampleModel())
                    .getScanlineStride();
            final int h = raster.getHeight();
            final int w = raster.getWidth();
            final int b = raster.getNumBands();
            final int wb = w * b;
            switch (buffer.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    for (int i = 0; i < h; ++i)
                        ios.write(((DataBufferByte) buffer).getData(), i * stride,
                                wb);
                    break;
                case DataBuffer.TYPE_USHORT:
                    for (int i = 0; i < h; ++i)
                        ios.writeShorts(((DataBufferUShort) buffer).getData(), i
                                * stride, wb);
                    break;
                case DataBuffer.TYPE_SHORT:
                    for (int i = 0; i < h; ++i)
                        ios.writeShorts(((DataBufferShort) buffer).getData(), i
                                * stride, wb);
                    break;
                default:
                    throw new RuntimeException("dataType:" + buffer.getDataType());
            }
            ios.flushBefore(ios.getStreamPosition());
        }
        log.debug("wrote frame #" + frameIndex);
    }

    public void transcodeFooter() throws Exception {
        if (!truncatePostPixelData) {
            readFooter();
            writeFooter();
        }
        if (reader != null) {
            reader.dispose();
        }
        if (writer != null) {
            writer.dispose();
        }
        bi = null;
    }

    private void readFooter() throws Exception {
        dsIn.clear();
        parser.parseDataset(decodeParam, -1);
    }

    private void writeFooter() throws Exception {
        dsIn.remove(Tags.DataSetTrailingPadding);
        dsIn.writeDataset(ios, encodeParam);
    }

    private ImageWriteParam getWriteParam() {
        ImageWriteParam wParam = writer.getDefaultWriteParam();
        if (encodeTS.equals(UIDs.JPEGBaseline)
                || encodeTS.equals(UIDs.JPEGExtended)) {
            wParam.setCompressionType("JPEG");
            wParam.setCompressionQuality(compressionQuality);
        } else if (encodeTS.equals(UIDs.JPEGLossless)
                || encodeTS.equals(UIDs.JPEGLossless14)) {
            wParam.setCompressionType("JPEG-LOSSLESS");
        } else if (encodeTS.equals(UIDs.JPEGLSLossless)) {
            wParam.setCompressionType("JPEG-LS");
        } else if (encodeTS.equals(UIDs.JPEG2000Lossless)) {
            J2KImageWriteParam j2KwParam = (J2KImageWriteParam) wParam;
            j2KwParam.setWriteCodeStreamOnly(true);
        } else if (encodeTS.equals(UIDs.JPEG2000Lossy)) {
            J2KImageWriteParam j2KwParam = (J2KImageWriteParam) wParam;
            j2KwParam.setWriteCodeStreamOnly(true);
            j2KwParam.setLossless(false);
            j2KwParam.setEncodingRate(encodingRate);
        }
        return wParam;
    }

    protected boolean isCompressionLossless() {
        return !encodeParam.encapsulated || encodeTS.equals(UIDs.JPEGLossless)
                || encodeTS.equals(UIDs.JPEGLossless14)
                || encodeTS.equals(UIDs.JPEGLSLossless)
                || encodeTS.equals(UIDs.JPEG2000Lossless);
    }

    /**
     * called after parsing object from input stream (until Tags.PixelData)
     *
     * @param ds
     *            parsed dataset containing header
     */
    public void onHeaderParsed(Dataset ds) {

    }

    /**
     * overwrite to perform processing on decoded frames.
     *
     * @param bi
     *            decoded frame
     * @return result image (default is image passed as bi)
     */
    public BufferedImage onFrameDecoded(BufferedImage bi) {
        return bi;
    }

    /**
     * @return Returns the ignoreMissingPixelData.
     */
    public boolean getIgnoreMissingPixelData() {
        return ignoreMissingPixelData;
    }

    /**
     * @param ignoreMissingPixelData
     *            The ignoreMissingPixelData to set.
     */
    public void setIgnoreMissingPixelData(boolean ignoreMissingPixelData) {
        this.ignoreMissingPixelData = ignoreMissingPixelData;
    }

    protected BufferedImage createBufferedImage() {
        int pixelStride;
        int[] bandOffset;
        int dataType;
        int colorSpace;
        if (pixelDataParam.getSamplesPerPixel() == 3) {
            pixelStride = 3;
            bandOffset = new int[] { 0, 1, 2 };
            dataType = DataBuffer.TYPE_BYTE;
            colorSpace = ColorSpace.CS_sRGB;
        } else {
            pixelStride = 1;
            bandOffset = new int[] { 0 };
            dataType = pixelDataParam.getBitsAllocated() == 8
                    ? DataBuffer.TYPE_BYTE
                    : DataBuffer.TYPE_USHORT;
            colorSpace = ColorSpace.CS_GRAY;
        }
        SampleModel sm = new PixelInterleavedSampleModel(dataType,
                pixelDataParam.getColumns(), pixelDataParam.getRows(),
                pixelStride, pixelDataParam.getColumns() * pixelStride,
                bandOffset);
        ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(colorSpace), sm.getSampleSize(), false,
                false, Transparency.OPAQUE, dataType);
        WritableRaster r = Raster.createWritableRaster(sm, new Point(0, 0));
        return new BufferedImage(cm, r, false, new Hashtable());
    }
}
