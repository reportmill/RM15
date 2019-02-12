/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.gfx3d.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.List;
import java.util.Map;
import snap.gfx.*;
import snappdf.PDFPage;
import snappdf.PDFWriter;
import snappdf.write.*;

/**
 * A class to hold RMShapePdfr subclasses for RMPage, RMTextShape, RMImageShape, RMScene3D.
 */
public class RMShapePdfrs {
    
    // Shared instances of RMShapePdfr subclasses
    static RMTextShapePdfr     _textShapePdfr = new RMTextShapePdfr();
    static RMImageShapePdfr    _imgShapePdfr = new RMImageShapePdfr();
    static RMPagePdfr          _pageShapePdfr = new RMPagePdfr();
    static RMScene3DPdfr       _scene3DPdfr = new RMScene3DPdfr();

/**
 * This class generates PDF for an RMText.
 */
public static class RMTextShapePdfr <T extends RMTextShape> extends RMShapePdfr <T> {

    /** Writes a given RMShape hierarchy to a PDF file (recursively). */
    protected void writeShape(T aTextShape, RMPDFWriter aWriter)
    {
        // Do normal version
        super.writeShape(aTextShape, aWriter);
        PDFWriterText.writeText(aWriter, aTextShape.getTextBox());
    }
}

/**
 * PDF writer for RMImageShape.
 */
public static class RMImageShapePdfr <T extends RMImageShape> extends RMShapePdfr <T> {

    /** Override to write ImageData. */
    protected void writeShape(T anImageShape, RMPDFWriter aWriter)
    {
        // Do normal version
        super.writeShape(anImageShape, aWriter);
        
        // Get image fill and image data (just return if missing or invalid)
        RMImageData idata = anImageShape.getImageData(); if(idata==null || !idata.isValid()) return;
        String iname = aWriter.getImageName(idata);
        
        // Get whether image fill is for pdf image (and just return if no page contents - which is apparently legal)
        boolean pdfImage = idata instanceof RMImageDataPDF;
        if(pdfImage) { RMImageDataPDF pdata = (RMImageDataPDF)idata;
            PDFPage page = pdata.getPDFFile().getPage(idata.getPageIndex());
            if(page.getPageContentsStream()==null)
                return;
        }
    
        // Add image data
        aWriter.addImageData(idata);
    
        // Get PDF page
        PDFPageWriter pdfPage = aWriter.getPageWriter();
        
        // Gsave
        pdfPage.gsave();
        
        // If pdf image, reset gstate defaults
        if(pdfImage) {
            pdfPage.setLineCap(0);
            pdfPage.setLineJoin(0);
        }
        
        // Apply clip if needed
        if(anImageShape.getRadius()>.001) {
            Shape path = anImageShape.getPath();
            pdfPage.writePath(path); pdfPage.append("W n ");
        }
        
        // Get image bounds width and height
        Rect bounds = anImageShape.getImageBounds();
        double width = bounds.getWidth(), height = bounds.getHeight();
    
        // pdfImage writes out scale of imageBounds/imageSize
        if(pdfImage) {
            width /= idata.getImageWidth();
            height /= idata.getImageHeight();
        }
    
        // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
        pdfPage.writeTransform(width, 0, 0, -height, bounds.getX(), bounds.getMaxY());
        
        // Do image
        pdfPage.appendln("/" + iname + " Do");
            
        // Grestore
        pdfPage.grestore();
        
        // If image has alpha, declare output to be PDF-1.4
        if(idata.hasAlpha() && idata.getSamplesPerPixel()==4)
            aWriter.getPDFFile().setVersion(1.4f);
    }
}

/**
 * This RMShapePdfr subclass writes PDF for RMPage.
 */
public static class RMPagePdfr <T extends RMPage> extends RMShapePdfr <T> {

    /** Writes a given RMShape hierarchy to a PDF file (recursively). */
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
    
    /** Override to suppress grestore. */
    protected void writeShapeAfter(T aShape, RMPDFWriter aWriter)  { }
}

/**
 * This class generates PDF for an RMScene3D.
 */
public static class RMScene3DPdfr <T extends RMScene3D> extends RMShapePdfr <T> {

    /** Writes a given RMShape hierarchy to a PDF file (recursively). */
    protected void writeShape(T aScene3D, RMPDFWriter aWriter)
    {
        // Do normal version
        super.writeShape(aScene3D, aWriter);
        
        // Write Path3Ds
        Camera camera = aScene3D.getCamera();
        List <Path3D> paths = camera.getPaths();
        for(Path3D path : paths) {
            writePath(aScene3D, path, aWriter);
            if(path.getLayers().size()>0) for(Path3D layer : path.getLayers())
                writePath(aScene3D, layer, aWriter);
        }
    }
    
    /** Writes a path. */
    protected void writePath(RMScene3D aScene3D, Path3D aPath, PDFWriter aWriter)
    {
        // Get path, fill and stroke
        Shape path = aPath.getPath();
        Color fillColor = aPath.getColor(), strokeColor = aPath.getStrokeColor();
        PDFPageWriter pdfPage = aWriter.getPageWriter();
        
        // Get opacity and set if needed
        double op = aPath.getOpacity();
        if(op<1) { pdfPage.gsave(); pdfPage.setOpacity(op*aScene3D.getOpacityDeep()); }
        
        // Do fill and stroke
        if(fillColor!=null)
            SnapPaintPdfr.writeShapeFill(path, fillColor, aWriter);
        if(strokeColor!=null)
            SnapPaintPdfr.writeShapeStroke(path, aPath.getStroke(), strokeColor, aWriter);
            
        // Reset opacity if needed
        if(op<1) pdfPage.grestore();
    }
}

/**
 * Returns a shared ViewShapePdfr.
 */
public static ViewShapePdfr getViewShapePdfr()  { return _vspdfr!=null? _vspdfr : (_vspdfr=new ViewShapePdfr()); }
private static ViewShapePdfr _vspdfr;

/**
 * This class generates PDF for an ViewShape.
 */
public static class ViewShapePdfr <T extends ViewShape> extends RMShapePdfr <T> {

    /** Writes a given RMShape hierarchy to a PDF file (recursively). */
    protected void writeShape(T aViewShape, RMPDFWriter aWriter)
    {
        // Do normal version
        super.writeShape(aViewShape, aWriter);
        
        // Get ViewShape info
        String name = aViewShape.getName();
        String type = aViewShape.getViewType(), fieldType = getFieldType(type);
        boolean isText = type==ViewShape.TextField_Type, isTextMultiline = isText;// && aViewShape.isMultiline();
        String text = aViewShape.getText(), pdfText = text!=null? '(' + text + ')' : null;
        boolean isButton = fieldType=="/Btn", isRadio = type==ViewShape.RadioButton_Type;
        boolean isCheckBox = type==ViewShape.CheckBox_Type;
        
        // Get flags field
        int flags = 0;
        if(isTextMultiline) flags |= 1<<12;
        if(isRadio) flags |= 1<<15;
        else if(isButton && !isCheckBox) flags |= 1<<16;
        
        // Get frame
        Rect frame = aViewShape.localToParent(aViewShape.getBoundsInside(), null).getBounds();
        frame.setY(aViewShape.getPageShape().getHeight() - frame.getMaxY());
        
        // Write annotation
        PDFAnnotation widget = new PDFAnnotation.Widget(frame, "");
        PDFPageWriter pwriter = aWriter.getPageWriter();
        pwriter.addAnnotation(widget);
        
        // Set Annotation Flags, Field-Type, name, Caption
        Map map = widget.getAnnotationMap();
        map.put("F", 4);
        map.put("FT", fieldType);
        map.put("Ff", flags);
        if(name!=null && name.length()>0) map.put("H", aViewShape.getName());
        
        // Set widget text
        if(text!=null && text.length()>0) {
            if(isText) map.put("V", pdfText);
            else map.put("T", pdfText);
        }
    }
    
    /** Returns the Field Type of Widget annotation. */
    String getFieldType(String aViewType)
    {
        switch(aViewType) {
            case ViewShape.TextField_Type: return "/Tx";
            case ViewShape.Button_Type: case ViewShape.RadioButton_Type: case ViewShape.CheckBox_Type: return "/Btn";
            case ViewShape.ListView_Type: case ViewShape.ComboBox_Type: return "/Ch";
            default: System.err.println("RMShapePdfrs.ViewShapePdfr: Unknown view type"); return "/Tx";
        }
    }
}

}