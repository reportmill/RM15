/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.pdf.PDFPage;
import snap.pdf.write.*;

/**
 * A custom class.
 */
public class RMShapePdfrs {
    
    // RMTextShapePdfr
    static RMTextShapePdfr     _textShapePdfr = new RMTextShapePdfr();

    // RMImageShapePdfr
    static RMImageShapePdfr    _imgShapePdfr = new RMImageShapePdfr();

    // RMPagePdfr
    static RMPagePdfr          _pageShapePdfr = new RMPagePdfr();

/**
 * This class generates PDF for an RMText.
 */
public static class RMTextShapePdfr <T extends RMTextShape> extends RMShapePdfr<T> {

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
            pdfPage.writePath(path); pdfPage.append(" re W n ");
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

}