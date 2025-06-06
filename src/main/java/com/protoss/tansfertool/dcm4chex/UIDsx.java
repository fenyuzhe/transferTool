package com.protoss.tansfertool.dcm4chex;

public class UIDsx {
    /** Private constructor */
    private UIDsx() {

    }

    public static String forName(String name) {
        try {
            return (String)UIDsx.class.getField(name).get(null);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Unkown UID Name: " + name);
        }
    }

    public static boolean isValid(String uid) {
        char[] a = uid.toCharArray();
        if (a.length == 0 || a.length > 64)
            return false;

        int state = 0;
        for (int i = 0; state != -1 && i < a.length; ++i) {
            switch (state) {
                case 0: // expect digit after point
                    state = a[i] == '0' ? 2 : isDigit(a[i]) ? 1 : -1;
                    break;
                case 1: // expect digit or point
                    state = a[i] == '.' ? 0 : isDigit(a[i]) ? 1 : -1;
                    break;
                case 2: // expect point
                    state = a[i] == '.' ? 0 : -1;
                    break;
            }
        }
        return state == 1 || state == 2;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** TransferSyntax: Implicit VR Little Endian */
    public static final String ImplicitVRLittleEndian = "1.2.840.10008.1.2";

    /** TransferSyntax: Explicit VR Little Endian */
    public static final String ExplicitVRLittleEndian = "1.2.840.10008.1.2.1";

    /** TransferSyntax: Deflated Explicit VR Little Endian */
    public static final String DeflatedExplicitVRLittleEndian = "1.2.840.10008.1.2.1.99";

    /** TransferSyntax: Explicit VR Big Endian */
    public static final String ExplicitVRBigEndian = "1.2.840.10008.1.2.2";

    /** TransferSyntax: JPEG Baseline (Process 1) */
    public static final String JPEGBaseline = "1.2.840.10008.1.2.4.50";

    /** TransferSyntax: JPEG Extended (Process 2 & 4) */
    public static final String JPEGExtended = "1.2.840.10008.1.2.4.51";

    /** TransferSyntax: JPEG Extended (Process 3 & 5) (Retired) */
    public static final String JPEGExtended35Retired = "1.2.840.10008.1.2.4.52";

    /** TransferSyntax: JPEG Spectral Selection, Non- Hierarchical (Process 6 & 8) (Retired) */
    public static final String JPEG68Retired = "1.2.840.10008.1.2.4.53";

    /** TransferSyntax: JPEG Spectral Selection, Non- Hierarchical (Process 7 & 9) (Retired) */
    public static final String JPEG79Retired = "1.2.840.10008.1.2.4.54";

    /** TransferSyntax: JPEG Full Progression, Non- Hierarchical (Process 10 & 12) (Retired) */
    public static final String JPEG1012Retired = "1.2.840.10008.1.2.4.55";

    /** TransferSyntax: JPEG Full Progression, Non- Hierarchical (Process 11 & 13) (Retired) */
    public static final String JPEG1113Retired = "1.2.840.10008.1.2.4.56";

    /** TransferSyntax: JPEG Lossless, Non-Hierarchical (Process 14) */
    public static final String JPEGLossless14 = "1.2.840.10008.1.2.4.57";

    /** TransferSyntax: JPEG Lossless, Non-Hierarchical (Process 15) (Retired) */
    public static final String JPEGLossless15Retired = "1.2.840.10008.1.2.4.58";

    /** TransferSyntax: JPEG Extended, Hierarchical (Process 16 & 18) (Retired) */
    public static final String JPEG1618Retired = "1.2.840.10008.1.2.4.59";

    /** TransferSyntax: JPEG Extended, Hierarchical (Process 17 & 19) (Retired) */
    public static final String JPEG1719Retired = "1.2.840.10008.1.2.4.60";

    /** TransferSyntax: JPEG Spectral Selection, Hierarchical (Process 20 & 22) (Retired) */
    public static final String JPEG2022Retired = "1.2.840.10008.1.2.4.61";

    /** TransferSyntax: JPEG Spectral Selection, Hierarchical (Process 21 & 23) (Retired) */
    public static final String JPEG2123Retired = "1.2.840.10008.1.2.4.62";

    /** TransferSyntax: JPEG Full Progression, Hierarchical (Process 24 & 26) (Retired) */
    public static final String JPEG2426Retired = "1.2.840.10008.1.2.4.63";

    /** TransferSyntax: JPEG Full Progression, Hierarchical (Process 25 & 27) (Retired) */
    public static final String JPEG2527Retired = "1.2.840.10008.1.2.4.64";

    /** TransferSyntax: JPEG Lossless, Hierarchical (Process 28) (Retired) */
    public static final String JPEGLoRetired = "1.2.840.10008.1.2.4.65";

    /** TransferSyntax: JPEG Lossless, Hierarchical (Process 29) (Retired) */
    public static final String JPEG29Retired = "1.2.840.10008.1.2.4.66";

    /** TransferSyntax: JPEG Lossless, Non- Hierarchical, First-Order Prediction (Process 14 [Selection Value 1]) */
    public static final String JPEGLossless = "1.2.840.10008.1.2.4.70";

    /** TransferSyntax: JPEG-LS Lossless Image Compression */
    public static final String JPEGLSLossless = "1.2.840.10008.1.2.4.80";

    /** TransferSyntax: JPEG-LS Lossy (Near-Lossless) Image Compression */
    public static final String JPEGLSLossy = "1.2.840.10008.1.2.4.81";

    /** TransferSyntax: JPEG 2000 Lossless Image Compression */
    public static final String JPEG2000Lossless = "1.2.840.10008.1.2.4.90";

    /** TransferSyntax: JPEG 2000 Lossy Image Compression */
    public static final String JPEG2000Lossy = "1.2.840.10008.1.2.4.91";

    /** TransferSyntax: MPEG2 Main Profile @ Main Level */
    public static final String MPEG2 = "1.2.840.10008.1.2.4.100";

    /** TransferSyntax: RLE Lossless */
    public static final String RLELossless = "1.2.840.10008.1.2.5";

    /** SOPClass: Verification SOP Class */
    public static final String Verification = "1.2.840.10008.1.1";

    /** SOPClass: Media Storage Directory Storage */
    public static final String MediaStorageDirectoryStorage = "1.2.840.10008.1.3.10";

    /** SOPClass: Basic Study Content Notification SOP Class */
    public static final String BasicStudyContentNotification = "1.2.840.10008.1.9";

    /** SOPClass: Storage Commitment Push Model SOP Class */
    public static final String StorageCommitmentPushModel = "1.2.840.10008.1.20.1";

    /** SOPClass: Storage Commitment Pull Model SOP Class */
    public static final String StorageCommitmentPullModel = "1.2.840.10008.1.20.2";

    /** SOPClass: Procedural Event Logging SOP Class */
    public static final String ProceduralEventLoggingSOPClass = "1.2.840.10008.1.40";

    /** SOPClass: Detached Patient Management SOP Class */
    public static final String DetachedPatientManagement = "1.2.840.10008.3.1.2.1.1";

    /** SOPClass: Detached Visit Management SOP Class */
    public static final String DetachedVisitManagement = "1.2.840.10008.3.1.2.2.1";

    /** SOPClass: Detached Study Management SOP Class */
    public static final String DetachedStudyManagement = "1.2.840.10008.3.1.2.3.1";

    /** SOPClass: Study Component Management SOP Class */
    public static final String StudyComponentManagement = "1.2.840.10008.3.1.2.3.2";

    /** SOPClass: Modality Performed Procedure Step SOP Class */
    public static final String ModalityPerformedProcedureStep = "1.2.840.10008.3.1.2.3.3";

    /** SOPClass: Modality Performed Procedure Step Retrieve SOP Class */
    public static final String ModalityPerformedProcedureStepRetrieve = "1.2.840.10008.3.1.2.3.4";

    /** SOPClass: Modality Performed Procedure Step Notification SOP Class */
    public static final String ModalityPerformedProcedureStepNotification = "1.2.840.10008.3.1.2.3.5";

    /** SOPClass: Detached Results Management SOP Class */
    public static final String DetachedResultsManagement = "1.2.840.10008.3.1.2.5.1";

    /** SOPClass: Detached Interpretation Management SOP Class */
    public static final String DetachedInterpretationManagement = "1.2.840.10008.3.1.2.6.1";

    /** SOPClass: Storage Service Class */
    public static final String StorageServiceClass = "1.2.840.10008.4.2";

    /** SOPClass: Basic Film Session SOP Class */
    public static final String BasicFilmSession = "1.2.840.10008.5.1.1.1";

    /** SOPClass: Basic Film Box SOP Class */
    public static final String BasicFilmBoxSOP = "1.2.840.10008.5.1.1.2";

    /** SOPClass: Basic Grayscale Image Box SOP Class */
    public static final String BasicGrayscaleImageBox = "1.2.840.10008.5.1.1.4";

    /** SOPClass: Basic Color Image Box SOP Class */
    public static final String BasicColorImageBox = "1.2.840.10008.5.1.1.4.1";

    /** SOPClass: Referenced Image Box SOP Class (Retired) */
    public static final String ReferencedImageBoxRetired = "1.2.840.10008.5.1.1.4.2";

    /** SOPClass: Print Job SOP Class */
    public static final String PrintJob = "1.2.840.10008.5.1.1.14";

    /** SOPClass: Basic Annotation Box SOP Class */
    public static final String BasicAnnotationBox = "1.2.840.10008.5.1.1.15";

    /** SOPClass: Printer SOP Class */
    public static final String Printer = "1.2.840.10008.5.1.1.16";

    /** SOPClass: Printer Configuration Retrieval SOP Class */
    public static final String PrinterConfigurationRetrieval = "1.2.840.10008.5.1.1.16.376";

    /** SOPClass: VOI LUT Box SOP Class */
    public static final String VOILUTBox = "1.2.840.10008.5.1.1.22";

    /** SOPClass: Presentation LUT SOP Class */
    public static final String PresentationLUT = "1.2.840.10008.5.1.1.23";

    /** SOPClass: Image Overlay Box SOP Class (Retired) */
    public static final String ImageOverlayBox = "1.2.840.10008.5.1.1.24";

    /** SOPClass: Basic Print Image Overlay Box SOP Class */
    public static final String BasicPrintImageOverlayBox = "1.2.840.10008.5.1.1.24.1";

    /** SOPClass: Print Queue Management SOP Class */
    public static final String PrintQueueManagement = "1.2.840.10008.5.1.1.26";

    /** SOPClass: Stored Print Storage SOP Class */
    public static final String StoredPrintStorage = "1.2.840.10008.5.1.1.27";

    /** SOPClass: Hardcopy Grayscale Image Storage SOP Class */
    public static final String HardcopyGrayscaleImageStorage = "1.2.840.10008.5.1.1.29";

    /** SOPClass: Hardcopy Color Image Storage SOP Class */
    public static final String HardcopyColorImageStorage  = "1.2.840.10008.5.1.1.30";

    /** SOPClass: Pull Print Request SOP Class */
    public static final String PullPrintRequest = "1.2.840.10008.5.1.1.31";

    /** SOPClass: Media Creation Management SOP Class */
    public static final String MediaCreationManagementSOPClass = "1.2.840.10008.5.1.1.33";

    /** SOPClass: Computed Radiography Image Storage */
    public static final String ComputedRadiographyImageStorage = "1.2.840.10008.5.1.4.1.1.1";

    /** SOPClass: Digital X-Ray Image Storage - For Presentation */
    public static final String DigitalXRayImageStorageForPresentation = "1.2.840.10008.5.1.4.1.1.1.1";

    /** SOPClass: Digital X-Ray Image Storage - For Processing */
    public static final String DigitalXRayImageStorageForProcessing = "1.2.840.10008.5.1.4.1.1.1.1.1";

    /** SOPClass: Digital Mammography X-Ray Image Storage - For Presentation */
    public static final String DigitalMammographyXRayImageStorageForPresentation = "1.2.840.10008.5.1.4.1.1.1.2";

    /** SOPClass: Digital Mammography X-Ray Image Storage - For Processing */
    public static final String DigitalMammographyXRayImageStorageForProcessing = "1.2.840.10008.5.1.4.1.1.1.2.1";

    /** SOPClass: Digital Intra-oral X-Ray Image Storage - For Presentation */
    public static final String DigitalIntraoralXRayImageStorageForPresentation = "1.2.840.10008.5.1.4.1.1.1.3";

    /** SOPClass: Digital Intra-oral X-Ray Image Storage - For Processing */
    public static final String DigitalIntraoralXRayImageStorageForProcessing = "1.2.840.10008.5.1.4.1.1.1.3.1";

    /** SOPClass: CT Image Storage */
    public static final String CTImageStorage = "1.2.840.10008.5.1.4.1.1.2";

    /** SOPClass: Enhanced CT Image Storage */
    public static final String EnhancedCTImageStorage = "1.2.840.10008.5.1.4.1.1.2.1";

    /** SOPClass: Ultrasound Multi-frame Image Storage (Retired) */
    public static final String UltrasoundMultiframeImageStorageRetired = "1.2.840.10008.5.1.4.1.1.3";

    /** SOPClass: Ultrasound Multi-frame Image Storage */
    public static final String UltrasoundMultiframeImageStorage = "1.2.840.10008.5.1.4.1.1.3.1";

    /** SOPClass: MR Image Storage */
    public static final String MRImageStorage = "1.2.840.10008.5.1.4.1.1.4";

    /** SOPClass: Enhanced MR Image Storage */
    public static final String EnhancedMRImageStorage = "1.2.840.10008.5.1.4.1.1.4.1";

    /** SOPClass: MR Spectroscopy Storage */
    public static final String MRSpectroscopyStorage = "1.2.840.10008.5.1.4.1.1.4.2";

    /** SOPClass: Nuclear Medicine Image Storage (Retired) */
    public static final String NuclearMedicineImageStorageRetired = "1.2.840.10008.5.1.4.1.1.5";

    /** SOPClass: Ultrasound Image Storage (Retired) */
    public static final String UltrasoundImageStorageRetired = "1.2.840.10008.5.1.4.1.1.6";

    /** SOPClass: Ultrasound Image Storage */
    public static final String UltrasoundImageStorage = "1.2.840.10008.5.1.4.1.1.6.1";

    /** SOPClass: Secondary Capture Image Storage */
    public static final String SecondaryCaptureImageStorage = "1.2.840.10008.5.1.4.1.1.7";

    /** SOPClass: Multi-frame Single Bit Secondary Capture Image Storage */
    public static final String MultiframeSingleBitSecondaryCaptureImageStorage = "1.2.840.10008.5.1.4.1.1.7.1";

    /** SOPClass: Multi-frame Grayscale Byte Secondary Capture Image Storage */
    public static final String MultiframeGrayscaleByteSecondaryCaptureImageStorage = "1.2.840.10008.5.1.4.1.1.7.2";

    /** SOPClass: Multi-frame Grayscale Word Secondary Capture Image Storage */
    public static final String MultiframeGrayscaleWordSecondaryCaptureImageStorage = "1.2.840.10008.5.1.4.1.1.7.3";

    /** SOPClass: Multi-frame Color Secondary Capture Image Storage */
    public static final String MultiframeColorSecondaryCaptureImageStorage = "1.2.840.10008.5.1.4.1.1.7.4";

    /** SOPClass: Standalone Overlay Storage */
    public static final String StandaloneOverlayStorage = "1.2.840.10008.5.1.4.1.1.8";

    /** SOPClass: Standalone Curve Storage */
    public static final String StandaloneCurveStorage = "1.2.840.10008.5.1.4.1.1.9";

    /** SOPClass: 12-lead ECG Waveform Storage */
    public static final String TwelveLeadECGWaveformStorage = "1.2.840.10008.5.1.4.1.1.9.1.1";

    /** SOPClass: General ECG Waveform Storage */
    public static final String GeneralECGWaveformStorage = "1.2.840.10008.5.1.4.1.1.9.1.2";

    /** SOPClass: Ambulatory ECG Waveform Storage */
    public static final String AmbulatoryECGWaveformStorage = "1.2.840.10008.5.1.4.1.1.9.1.3";

    /** SOPClass: Hemodynamic Waveform Storage */
    public static final String HemodynamicWaveformStorage = "1.2.840.10008.5.1.4.1.1.9.2.1";

    /** SOPClass: Cardiac Electrophysiology Waveform Storage */
    public static final String CardiacElectrophysiologyWaveformStorage = "1.2.840.10008.5.1.4.1.1.9.3.1";

    /** SOPClass: Basic Voice Audio Waveform Storage */
    public static final String BasicVoiceAudioWaveformStorage = "1.2.840.10008.5.1.4.1.1.9.4.1";

    /** SOPClass: Standalone Modality LUT Storage */
    public static final String StandaloneModalityLUTStorage = "1.2.840.10008.5.1.4.1.1.10";

    /** SOPClass: Standalone VOI LUT Storage */
    public static final String StandaloneVOILUTStorage = "1.2.840.10008.5.1.4.1.1.11";

    /** SOPClass: Grayscale Softcopy Presentation State Storage SOP Class */
    public static final String GrayscaleSoftcopyPresentationStateStorage = "1.2.840.10008.5.1.4.1.1.11.1";

    /** SOPClass: X-Ray Angiographic Image Storage */
    public static final String XRayAngiographicImageStorage = "1.2.840.10008.5.1.4.1.1.12.1";

    /** SOPClass: X-Ray Radiofluoroscopic Image Storage */
    public static final String XRayRadiofluoroscopicImageStorage = "1.2.840.10008.5.1.4.1.1.12.2";

    /** SOPClass: X-Ray Angiographic Bi-Plane Image Storage (Retired) */
    public static final String XRayAngiographicBiPlaneImageStorageRetired = "1.2.840.10008.5.1.4.1.1.12.3";

    /** SOPClass: Nuclear Medicine Image Storage */
    public static final String NuclearMedicineImageStorage = "1.2.840.10008.5.1.4.1.1.20";

    /** SOPClass: Raw Data Storage */
    public static final String RawDataStorage = "1.2.840.10008.5.1.4.1.1.66";

    /** SOPClass: Spatial Registration Storage */
    public static final String SpatialRegistrationStorage = "1.2.840.10008.5.1.4.1.1.66.1";

    /** SOPClass: Spatial Fiducials Storage */
    public static final String SpatialFiducialsStorage = "1.2.840.10008.5.1.4.1.1.66.2";

    /** SOPClass: VL Image Storage (Retired) */
    public static final String VLImageStorageRetired = "1.2.840.10008.5.1.4.1.1.77.1";

    /** SOPClass: VL Multi-frame Image Storage (Retired) */
    public static final String VLMultiframeImageStorageRetired = "1.2.840.10008.5.1.4.1.1.77.2";

    /** SOPClass: VL Endoscopic Image Storage */
    public static final String VLEndoscopicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.1";

    /** SOPClass: VL Microscopic Image Storage */
    public static final String VLMicroscopicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.2";

    /** SOPClass: VL Slide-Coordinates Microscopic Image Storage */
    public static final String VLSlideCoordinatesMicroscopicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.3";

    /** SOPClass: VL Photographic Image Storage */
    public static final String VLPhotographicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.4";

    /** SOPClass: Video Endoscopic Image Storage */
    public static final String VideoEndoscopicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.1.1";

    /** SOPClass: Video Microscopic Image Storage */
    public static final String VideoMicroscopicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.2.1";

    /** SOPClass: Video Photographic Image Storage */
    public static final String VideoPhotographicImageStorage = "1.2.840.10008.5.1.4.1.1.77.1.4.1";

    /** SOPClass: Basic Text SR */
    public static final String BasicTextSR = "1.2.840.10008.5.1.4.1.1.88.11";

    /** SOPClass: Enhanced SR */
    public static final String EnhancedSR = "1.2.840.10008.5.1.4.1.1.88.22";

    /** SOPClass: Comprehensive SR */
    public static final String ComprehensiveSR = "1.2.840.10008.5.1.4.1.1.88.33";

    /** SOPClass: Procedure Log Storage */
    public static final String ProcedureLogStorage = "1.2.840.10008.5.1.4.1.1.88.40";

    /** SOPClass: Mammography CAD SR */
    public static final String MammographyCADSR = "1.2.840.10008.5.1.4.1.1.88.50";

    /** SOPClass: Key Object Selection Document */
    public static final String KeyObjectSelectionDocument = "1.2.840.10008.5.1.4.1.1.88.59";

    /** SOPClass: Chest CAD SR */
    public static final String ChestCADSR = "1.2.840.10008.5.1.4.1.1.88.65";

    /** SOPClass: Positron Emission Tomography Image Storage */
    public static final String PositronEmissionTomographyImageStorage = "1.2.840.10008.5.1.4.1.1.128";

    /** SOPClass: Standalone PET Curve Storage */
    public static final String StandalonePETCurveStorage = "1.2.840.10008.5.1.4.1.1.129";

    /** SOPClass: RT Image Storage */
    public static final String RTImageStorage = "1.2.840.10008.5.1.4.1.1.481.1";

    /** SOPClass: RT Dose Storage */
    public static final String RTDoseStorage = "1.2.840.10008.5.1.4.1.1.481.2";

    /** SOPClass: RT Structure Set Storage */
    public static final String RTStructureSetStorage = "1.2.840.10008.5.1.4.1.1.481.3";

    /** SOPClass: RT Beams Treatment Record Storage */
    public static final String RTBeamsTreatmentRecordStorage = "1.2.840.10008.5.1.4.1.1.481.4";

    /** SOPClass: RT Plan Storage */
    public static final String RTPlanStorage = "1.2.840.10008.5.1.4.1.1.481.5";

    /** SOPClass: RT Brachy Treatment Record Storage */
    public static final String RTBrachyTreatmentRecordStorage = "1.2.840.10008.5.1.4.1.1.481.6";

    /** SOPClass: RT Treatment Summary Record Storage */
    public static final String RTTreatmentSummaryRecordStorage = "1.2.840.10008.5.1.4.1.1.481.7";

    /** SOPClass: Patient Root Query/Retrieve Information Model - FIND */
    public static final String PatientRootQueryRetrieveInformationModelFIND = "1.2.840.10008.5.1.4.1.2.1.1";

    /** SOPClass: Patient Root Query/Retrieve Information Model - MOVE */
    public static final String PatientRootQueryRetrieveInformationModelMOVE = "1.2.840.10008.5.1.4.1.2.1.2";

    /** SOPClass: Patient Root Query/Retrieve Information Model - GET */
    public static final String PatientRootQueryRetrieveInformationModelGET = "1.2.840.10008.5.1.4.1.2.1.3";

    /** SOPClass: Study Root Query/Retrieve Information Model - FIND */
    public static final String StudyRootQueryRetrieveInformationModelFIND = "1.2.840.10008.5.1.4.1.2.2.1";

    /** SOPClass: Study Root Query/Retrieve Information Model - MOVE */
    public static final String StudyRootQueryRetrieveInformationModelMOVE = "1.2.840.10008.5.1.4.1.2.2.2";

    /** SOPClass: Study Root Query/Retrieve Information Model - GET */
    public static final String StudyRootQueryRetrieveInformationModelGET = "1.2.840.10008.5.1.4.1.2.2.3";

    /** SOPClass: Patient/Study Only Query/Retrieve Information Model - FIND */
    public static final String PatientStudyOnlyQueryRetrieveInformationModelFIND = "1.2.840.10008.5.1.4.1.2.3.1";

    /** SOPClass: Patient/Study Only Query/Retrieve Information Model - MOVE */
    public static final String PatientStudyOnlyQueryRetrieveInformationModelMOVE = "1.2.840.10008.5.1.4.1.2.3.2";

    /** SOPClass: Patient/Study Only Query/Retrieve Information Model - GET */
    public static final String PatientStudyOnlyQueryRetrieveInformationModelGET = "1.2.840.10008.5.1.4.1.2.3.3";

    /** SOPClass: Modality Worklist Information Model - FIND */
    public static final String ModalityWorklistInformationModelFIND = "1.2.840.10008.5.1.4.31";

    /** SOPClass: General Purpose Worklist Information Model - FIND */
    public static final String GeneralPurposeWorklistInformationModelFIND = "1.2.840.10008.5.1.4.32.1";

    /** SOPClass: General Purpose Scheduled Procedure Step SOP Class */
    public static final String GeneralPurposeScheduledProcedureStepSOPClass = "1.2.840.10008.5.1.4.32.2";

    /** SOPClass: General Purpose Performed Procedure Step SOP Class */
    public static final String GeneralPurposePerformedProcedureStepSOPClass = "1.2.840.10008.5.1.4.32.3";

    /** SOPClass: Instance Availability Notification SOP Class */
    public static final String InstanceAvailabilityNotificationSOPClass = "1.2.840.10008.5.1.4.33";

    /** SOPClass: General Relevant Patient Information Query General Relevant */
    public static final String PatientInformationQuery = "1.2.840.10008.5.1.4.37.1";

    /** SOPClass: Breast Imaging Relevant Patient Information Query */
    public static final String BreastImagingRelevantPatientInformationQuery = "1.2.840.10008.5.1.4.37.2";

    /** SOPClass: Cardiac Relevant Patient Information Query */
    public static final String CardiacRelevantPatientInformationQuery = "1.2.840.10008.5.1.4.37.3";

    /** SOPClass: Hanging Protocol Storage */
    public static final String HangingProtocolStorage = "1.2.840.10008.5.1.4.38.1";

    /** SOPClass: Hanging Protocol Information Model - FIND */
    public static final String HangingProtocolInformationModelFIND = "1.2.840.10008.5.1.4.38.2";

    /** SOPClass: Hanging Protocol Information Model - MOVE */
    public static final String HangingProtocolInformationModelMOVE = "1.2.840.10008.5.1.4.38.3";

    /** SOPClass: Tiani Patient Root Query/Retrieve Information Model - FIND */
    public static final String TianiPatientRootQueryRetrieveInformationModelFIND = "1.2.40.0.13.1.5.1.4.1.2.1.1";

    /** SOPClass: Tiani Study Root Query/Retrieve Information Model - FIND */
    public static final String TianiStudyRootQueryRetrieveInformationModelFIND = "1.2.40.0.13.1.5.1.4.1.2.2.1";

    /** SOPClass: Tiani Patient/Study Only Query/Retrieve Information Model - FIND */
    public static final String TianiPatientStudyOnlyQueryRetrieveInformationModelFIND = "1.2.40.0.13.1.5.1.4.1.2.3.1";

    /** MetaSOPClass: Detached Patient Management Meta SOP Class */
    public static final String DetachedPatientManagementMetaSOPClass = "1.2.840.10008.3.1.2.1.4";

    /** MetaSOPClass: Detached Results Management Meta SOP Class */
    public static final String DetachedResultsManagementMetaSOPClass = "1.2.840.10008.3.1.2.5.4";

    /** MetaSOPClass: Detached Study Management Meta SOP Class */
    public static final String DetachedStudyManagementMetaSOPClass = "1.2.840.10008.3.1.2.5.5";

    /** MetaSOPClass: Basic Grayscale Print Management Meta SOP Class */
    public static final String BasicGrayscalePrintManagement = "1.2.840.10008.5.1.1.9";

    /** MetaSOPClass: Referenced Grayscale Print Management Meta SOP Class (Retired) */
    public static final String ReferencedGrayscalePrintManagementRetired = "1.2.840.10008.5.1.1.9.1";

    /** MetaSOPClass: Basic Color Print Management Meta SOP Class */
    public static final String BasicColorPrintManagement = "1.2.840.10008.5.1.1.18";

    /** MetaSOPClass: Referenced Color Print Management Meta SOP Class (Retired) */
    public static final String ReferencedColorPrintManagementRetired = "1.2.840.10008.5.1.1.18.1";

    /** MetaSOPClass: Pull Stored Print Management Meta SOP Class */
    public static final String PullStoredPrintManagement = "1.2.840.10008.5.1.1.32";

    /** MetaSOPClass: General Purpose Worklist Management Meta SOP Class */
    public static final String GeneralPurposeWorklistManagementMetaSOPClass = "1.2.840.10008.5.1.4.32";

    /** SOPInstance: Storage Commitment Push Model SOP Instance */
    public static final String StorageCommitmentPushModelSOPInstance = "1.2.840.10008.1.20.1.1";

    /** SOPInstance: Storage Commitment Pull Model SOP Instance */
    public static final String StorageCommitmentPullModelSOPInstance = "1.2.840.10008.1.20.2.1";

    /** SOPInstance: Procedural Event Logging SOP Instance */
    public static final String ProceduralEventLoggingSOPInstance = "1.2.840.10008.1.40.1";

    /** SOPInstance: Talairach Brain Atlas Frame of Reference */
    public static final String TalairachBrainAtlasFrameOfReference = "1.2.840.10008.1.4.1.1";

    /** SOPInstance: SPM2 T1 Frame of Reference */
    public static final String SPM2T1FrameOfReference = "1.2.840.10008.1.4.1.2";

    /** SOPInstance: SPM2 T2 Frame of Reference */
    public static final String SPM2T2FrameOfReference = "1.2.840.10008.1.4.1.3";

    /** SOPInstance: SPM2 PD Frame of Reference */
    public static final String SPM2PDFrameOfReference = "1.2.840.10008.1.4.1.4";

    /** SOPInstance: SPM2 EPI Frame of Reference */
    public static final String SPM2EPIFrameOfReference = "1.2.840.10008.1.4.1.5";

    /** SOPInstance: SPM2 FIL T1 Frame of Reference */
    public static final String SPM2FILT1FrameOfReference = "1.2.840.10008.1.4.1.6";

    /** SOPInstance: SPM2 PET Frame of Reference */
    public static final String SPM2PETFrameOfReference = "1.2.840.10008.1.4.1.7";

    /** SOPInstance: SPM2 TRANSM Frame of Reference */
    public static final String SPM2TRANSMFrameOfReference = "1.2.840.10008.1.4.1.8";

    /** SOPInstance: SPM2 SPECT Frame of Reference */
    public static final String SPM2SPECTFrameOfReference = "1.2.840.10008.1.4.1.9";

    /** SOPInstance: SPM2 GRAY Frame of Reference */
    public static final String SPM2GRAYFrameOfReference = "1.2.840.10008.1.4.1.10";

    /** SOPInstance: SPM2 WHITE Frame of Reference */
    public static final String SPM2WHITEFrameOfReference = "1.2.840.10008.1.4.1.11";

    /** SOPInstance: SPM2 CSF Frame of Reference */
    public static final String SPM2CSFFrameOfReference = "1.2.840.10008.1.4.1.12";

    /** SOPInstance: SPM2 BRAINMASK Frame of Reference */
    public static final String SPM2BRAINMASKFrameOfReference = "1.2.840.10008.1.4.1.13";

    /** SOPInstance: SPM2 AVG305T1 Frame of Reference */
    public static final String SPM2AVG305T1FrameOfReference = "1.2.840.10008.1.4.1.14";

    /** SOPInstance: SPM2 AVG152T1 Frame of Reference */
    public static final String SPM2AVG152T1FrameOfReference = "1.2.840.10008.1.4.1.15";

    /** SOPInstance: SPM2 AVG152T2 Frame of Reference */
    public static final String SPM2AVG152T2FrameOfReference = "1.2.840.10008.1.4.1.16";

    /** SOPInstance: SPM2 AVG152PD Frame of Reference */
    public static final String SPM2AVG152PDFrameOfReference = "1.2.840.10008.1.4.1.17";

    /** SOPInstance: SPM2 SINGLESUBJT1 Frame of Reference */
    public static final String SPM2SINGLESUBJT1FrameOfReference = "1.2.840.10008.1.4.1.18";

    /** SOPInstance: ICBM 452 T1 Frame of Reference */
    public static final String ICBM452T1FrameOfReference = "1.2.840.10008.1.4.2.1";

    /** SOPInstance: ICBM Single Subject MRI Frame of Reference */
    public static final String ICBMSingleSubjectMRIFrameOfReference = "1.2.840.10008.1.4.2.2";

    /** SOPInstance: Printer SOP Instance */
    public static final String PrinterSOPInstance = "1.2.840.10008.5.1.1.17";

    /** SOPInstance: Printer Configuration Retrieval SOP Instance */
    public static final String PrinterConfigurationRetrievalSOPInstance = "1.2.840.10008.5.1.1.17.376";

    /** SOPInstance: Print Queue SOP Instance */
    public static final String PrintQueueSOPInstance = "1.2.840.10008.5.1.1.25";

    /** ApplicationContextName: DICOM Application Context Name */
    public static final String DICOMApplicationContextName = "1.2.840.10008.3.1.1.1";

    /** CodingScheme: DICOM Controlled Terminology Coding Scheme */
    public static final String DICOMControlledTerminologyCodingScheme = "1.2.840.10008.2.16.4";

    /** SynchronizationFrameOfReference: Universal Coordinated Time */
    public static final String UniversalCoordinatedTime = "1.2.840.10008.15.1.1";

    public static final String ModalityWorklistSOPClass = "1.2.840.10008.5.1.4.31";
}
