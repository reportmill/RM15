/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.geom.Insets;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.*;
import snappdf.*;
import snappdf.write.*;

/**
 * A class to hold RMShapePdfr subclasses for RMPage, RMTextShape, RMImageShape.
 */
public class RMShapePdfrs {

    // Shared instances of RMShapePdfr subclasses
    protected static RMShapePainterPdfr<?> _shapePainterPdfr = new RMShapePainterPdfr<>();
    protected static RMTextShapePdfr<?> _textShapePdfr = new RMTextShapePdfr<>();
    protected static RMImageShapePdfr<?> _imgShapePdfr = new RMImageShapePdfr<>();
    protected static RMPagePdfr<?> _pageShapePdfr = new RMPagePdfr<>();

    /**
     * This class generates PDF for an RMShape using PDFPainter and shapes standard painting.
     */
    public static class RMShapePainterPdfr<T extends RMShape> extends RMShapePdfr<T> {

        /**
         * Writes given RMShape to PDFWriter using PDFPainter.
         */
        protected void writeShape(T aShape, RMPDFWriter aWriter)
        {
            PDPainter pdfPainter = new PDPainter(aWriter);
            pdfPainter.translate(-aShape.getX(), -aShape.getY());
            aShape.paint(pdfPainter);
        }
    }

    /**
     * This class generates PDF for an RMText.
     */
    public static class RMTextShapePdfr<T extends RMTextShape> extends RMShapePdfr<T> {

        /**
         * Writes a given RMShape hierarchy to a PDF file (recursively).
         */
        protected void writeShape(T aTextShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(aTextShape, aWriter);

            // If not editable, just write out text and return
            if (!aTextShape.isEditable()) {
                PDFWriterText.writeText(aWriter, aTextShape.getTextBox());
                return;
            }

            // Writing PDF Widget Annotation: Get pdf page object and bump PDF version to 1.4
            PDFPageWriter pwriter = aWriter.getPageWriter();

            // Get TextShape info
            String name = aTextShape.getName();
            String pdfName = name != null && name.length() > 0 ? name : "Text Box " + aWriter.getAcroFormFieldCount();
            String tooTip = name != null && name.length() > 0 ? name : null;
            String text = aTextShape.getText();
            String pdfText = text != null && text.length() > 0 ? text : null;

            // Get ViewShape frame in PDF page coords (minus text insets)
            RMShape page = aTextShape.getPageShape();
            Rect frame = aTextShape.localToParent(aTextShape.getBoundsInside(), page).getBounds();
            frame.y = page.getHeight() - frame.getMaxY();
            Insets ins = aTextShape.getMargin();
            frame.x += ins.left;
            frame.y += ins.bottom;
            frame.width -= ins.getWidth();
            frame.height -= ins.getHeight();

            // Create and add annotation to page
            PDFAnnotation widget = new PDFAnnotation.Widget(frame, "");
            pwriter.addAnnotation(widget);

            // Set Annotation Flags, Field-Type
            Map<String,Object> map = widget.getAnnotationMap();
            map.put("P", aWriter.getXRefTable().getRefString(pwriter));
            map.put("F", 4);
            map.put("FT", "/Tx"); // Makes widget printable textfield
            if (aTextShape.isMultiline())
                map.put("Ff", 1 << 12);

            // Get font name and set in Default Appearance
            Font font = aTextShape.getFont();
            PDFFontEntry fontEntry = aWriter.getFontEntry(font, 0);
            String fontName = '/' + fontEntry.getPDFName();
            int fontSize = (int) font.getSize();
            map.put("DA", "(0 0 0 rg " + fontName + ' ' + fontSize + " Tf)");

            // Set Widget Name, alt name, value, default value and fonts dict
            map.put("T", '(' + pdfName + ')'); // Name
            if (tooTip != null)
                map.put("TU", '(' + tooTip + ')'); // Alternate name (ToolTip)
            if (pdfText != null) {
                map.put("V", '(' + pdfText + ')');
                map.put("DV", '(' + pdfText + ')');
            }

            // Set Widget Default Resources dict
            Object fonts = aWriter.getFonts();
            Object fontsXRef = aWriter.getXRefTable().getRefString(fonts);
            Map<String,Object> drDict = Collections.singletonMap("Font", fontsXRef);
            map.put("DR", drDict);
        }
    }

    /**
     * PDF writer for RMImageShape.
     */
    public static class RMImageShapePdfr<T extends RMImageShape> extends RMShapePdfr<T> {

        /**
         * Override to write Image.
         */
        protected void writeShape(T anImageShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(anImageShape, aWriter);

            // Get image data (just return if missing) and image name and add image
            Image img = anImageShape.getImage();
            if (img == null) return;
            String iname = aWriter.getImageName(img);
            aWriter.addImage(img);

            // Get PDF page and gsave
            PDFPageWriter pdfPage = aWriter.getPageWriter();
            pdfPage.gsave();

            // Apply clip if needed
            if (anImageShape.getRadius() > .001) {
                Shape path = anImageShape.getPath();
                pdfPage.writePath(path);
                pdfPage.append("W n ");
            }

            // Get image bounds width and height
            Rect bounds = anImageShape.getImageBounds();
            double width = bounds.getWidth(), height = bounds.getHeight();

            // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
            pdfPage.writeTransform(width, 0, 0, -height, bounds.getX(), bounds.getMaxY());

            // Do image and grestore
            pdfPage.appendln("/" + iname + " Do");
            pdfPage.grestore();
        }
    }

    /**
     * PDF writer for RMPDFShape.
     */
    public static class RMPDFShapePdfr extends RMShapePdfr<RMPDFShape> {

        /**
         * Override to write PDF XObject form do.
         */
        protected void writeShape(RMPDFShape aPDFShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(aPDFShape, aWriter);

            // Get PDF data (just return if missing or invalid)
            RMPDFData pdata = aPDFShape.getPDFData();
            if (pdata == null) return;

            // Get pdf page (just return if no page contents - which is apparently legal)
            PDFPage page = pdata.getPDFFile().getPage(pdata.getPageIndex());
            if (page.getPageContentsStream() == null) return;
            String iname = String.valueOf(System.identityHashCode(page)); //aWriter.getImageName(pdata);

            // Add pdf data
            aWriter.addPDFData(pdata, iname);

            // Get PDF page, gsave and reset gstate defaults
            PDFPageWriter pdfPage = aWriter.getPageWriter();
            pdfPage.gsave();
            pdfPage.setLineCap(0);
            pdfPage.setLineJoin(0);

            // Apply clip if needed
            //if (aPDFShape.getRadius()>.1) { Shape p = aPDFShape.getPath(); pdfPage.writePath(p); pdfPage.append("W n "); }

            // Get image bounds width and height divided by PDF size (pdfImage writes out scale of imageBounds/imageSize)
            Rect bounds = aPDFShape.getImageBounds();
            double width = bounds.width / pdata.getWidth();
            double height = bounds.height / pdata.getHeight();

            // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
            pdfPage.writeTransform(width, 0, 0, -height, bounds.x, bounds.getMaxY());

            // Do image and grestore
            pdfPage.appendln("/" + iname + " Do");
            pdfPage.grestore();
        }
    }

    /**
     * This RMShapePdfr subclass writes PDF for RMPage.
     */
    public static class RMPagePdfr<T extends RMPage> extends RMShapePdfr<T> {

        /**
         * Writes a given RMShape hierarchy to a PDF file (recursively).
         */
        protected void writeShapeBefore(T aPageShape, RMPDFWriter aWriter)
        {
            // Get pdf page
            PDFPageWriter pdfPage = aWriter.getPageWriter();

            // Write page header comment
            pdfPage.appendln("\n% ------ page " + (aPageShape.page() - 1) + " -----");

            // legacy defaults different from pdf defaults
            pdfPage.setLineCap(1);
            pdfPage.setLineJoin(1);

            // Flip coords to match java2d model
            pdfPage.append("1 0 0 -1 0 ").append(aPageShape.getHeight()).appendln(" cm");
        }

        /**
         * Override to suppress grestore.
         */
        protected void writeShapeAfter(T aShape, RMPDFWriter aWriter)  { }
    }

    /**
     * This class generates PDF for an ViewShape.
     */
    public static class ViewShapePdfr extends RMShapePdfr<ViewShape> {

        /**
         * Writes a given RMShape hierarchy to a PDF file (recursively).
         */
        protected void writeShape(ViewShape aViewShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(aViewShape, aWriter);

            // Get ViewShape info
            String name = aViewShape.getName();
            String pdfName = name != null && name.length() > 0 ? '(' + name + ')' : null;
            String type = aViewShape.getViewType();
            String fieldType = getFieldType(type);
            boolean isText = type == ViewShape.TextField_Type;
            boolean isTextMultiline = isText && aViewShape.isMultiline();
            String text = aViewShape.getText();
            String pdfText = text != null && text.length() > 0 ? '(' + text + ')' : null;
            boolean isButton = fieldType == "/Btn";
            boolean isRadio = type == ViewShape.RadioButton_Type;
            boolean isCheckBox = type == ViewShape.CheckBox_Type;

            // Get flags field
            int flags = 0;
            if (isTextMultiline) flags |= 1 << 12;
            if (isRadio) flags |= 1 << 15;
            else if (isButton && !isCheckBox) flags |= 1 << 16;

            // Get ViewShape frame in PDF page coords (minus text insets)
            Rect frame = aViewShape.localToParent(aViewShape.getBoundsInside(), null).getBounds();
            frame.setY(aViewShape.getPageShape().getHeight() - frame.getMaxY());
            if (isText) {
                Insets ins = new Insets(2);
                frame.x += ins.left;
                frame.y += ins.bottom;
                frame.width -= ins.getWidth();
                frame.height -= ins.getHeight();
            }

            // Create and add annotation to page
            PDFAnnotation widget = new PDFAnnotation.Widget(frame, "");
            PDFPageWriter pwriter = aWriter.getPageWriter();
            pwriter.addAnnotation(widget);

            // Set Annotation Flags, Field-Type
            Map<String,Object> map = widget.getAnnotationMap(); //map.put("P", xrefs.getRefString(pwriter));
            map.put("F", 4);
            map.put("FT", fieldType); // Makes widget printable and field type
            if (flags != 0)
                map.put("Ff", flags);

            // Get font name and set in Default Appearance
            Font font = aViewShape.getFont();
            PDFFontEntry fontEntry = aWriter.getFontEntry(font, 0);
            String fontName = '/' + fontEntry.getPDFName();
            int fontSize = (int) font.getSize();
            map.put("DA", "(0 0 0 rg " + fontName + ' ' + fontSize + " Tf)");

            // Set Widget Name, alt name, value, default value and fonts dict
            if (pdfName != null) map.put("T", pdfName); // Name
            if (pdfName != null) map.put("TU", pdfName); // Alternate name (ToolTip)
            if (pdfText != null) {
                map.put("V", pdfText);
                map.put("DV", pdfText);
            }

            // Set Widget Default Resources dict
            Object fonts = aWriter.getFonts();
            Object fontsXRef = aWriter.getXRefTable().getRefString(fonts);
            Map<String,Object> drDict = Collections.singletonMap("Font", fontsXRef);
            map.put("DR", drDict);

            // Write Appearance Dictionary
            /*Map apnDict = new HashMap(8); apnDict.put("Type", "/XObject"); apnDict.put("Subtype", "/Form");
            apnDict.put("BBox", "[0 0 " + frame.width + " " + frame.height + "]");
            apnDict.put("Resources", xrefs.addObject(pwriter.getResourcesDict()));
            String str = fieldType + " BMC\nEMC"; byte strBytes[] = str.getBytes();
            PDFStream formStream = new PDFStream(strBytes, apnDict); Object formStreamXRef = xrefs.addObject(formStream);
            Map apDict = Collections.singletonMap("N", formStreamXRef); Object apDictXRef = xrefs.addObject(apDict);
            map.put("AP", apDict);*/
        }

        /**
         * Returns the Field Type of Widget annotation.
         */
        String getFieldType(String aViewType)
        {
            switch (aViewType) {
                case ViewShape.TextField_Type: return "/Tx";
                case ViewShape.Button_Type:
                case ViewShape.RadioButton_Type:
                case ViewShape.CheckBox_Type: return "/Btn";
                case ViewShape.ListView_Type:
                case ViewShape.ComboBox_Type: return "/Ch";
                default: System.err.println("RMShapePdfrs.ViewShapePdfr: Unknown view type"); return "/Tx";
            }
        }
    }
}