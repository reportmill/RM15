/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import snap.gfx.*;
import snappdf.write.*;

/**
 * This RMObjectPdfr subclass writes PDF for RMShape.
 */
public class RMShapePdfr <T extends RMShape> {
    
    // Shared RMShapePdfr
    static RMShapePdfr    _shapePdfr = new RMShapePdfr();

/**
 * Writes a given RMShape hierarchy to a PDF file (recursively).
 */
public void writePDF(T aShape, RMPDFWriter aWriter)
{
    // Write shape
    writeShapeBefore(aShape, aWriter);
    
    // If shape has effect, forward to it
    if(aShape.getEffect()!=null) RMEffectPdfr.writeShapeEffect(aShape, aWriter);
    
    // Otherwise, do basic write shape all
    else writeShapeAll(aShape, aWriter);
    
    // Write shape after children
    writeShapeAfter(aShape, aWriter);    
}

/**
 * Writes a given RMShape hierarchy to a PDF file (recursively).
 */
protected void writeShapeBefore(T aShape, RMPDFWriter aWriter)
{
    // Get page
    PDFPageWriter pdfPage = aWriter.getPageWriter();
    
    // Save the graphics transform
    pdfPage.gsave();
        
    // If not rotated/scaled, write simple translation matrix
    if(!aShape.isRSS())
        pdfPage.append("1 0 0 1 ").append(aShape.getX()).append(' ').append(aShape.getY()).appendln(" cm");
    
    // If rotated/scaled, write full transform
    else pdfPage.writeTransform(aShape.getTransform());
}
    
/**
 * Writes the shape and then the shape's children.
 */
protected void writeShapeAll(T aShape, RMPDFWriter aWriter)
{
    // Write shape fills
    writeShape(aShape, aWriter);
    
    // Write shape children
    writeShapeChildren(aShape, aWriter);
}

/**
 * Writes a given RMShape hierarchy to a PDF file (recursively).
 */
protected void writeShape(T aShape, RMPDFWriter aWriter)
{
    // Get pdf page
    PDFPageWriter pwriter = aWriter.getPageWriter();
    
    // Set shape opacity
    pwriter.setOpacity(aShape.getOpacityDeep());
    
    // Clip to bounds???
    //pwriter.print(aShape.getBoundsInside()); pageBuffer.println(" re W n"));
        
    // Get fill and write pdf if not null
    RMFill fill = aShape.getFill();
    if(fill!=null)
        RMFillPdfr.writeShapeFill(aShape, fill, aWriter);
    
    // Get stroke and write pdf if not null
    if(aShape.getStroke()!=null && !aShape.isStrokeOnTop())
        writeShapeStroke(aShape, aWriter);
}

/**
 * Writes a given RMShape hierarchy to a PDF file (recursively).
 */
protected void writeShapeChildren(RMShape aShape, RMPDFWriter aWriter)
{
    // Write children
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
        if(child.isVisible())
            getPdfr(child).writePDF(child, aWriter);
    }
}
    
/**
 * Writes a given RMShape hierarchy to a PDF file (recursively).
 */
protected void writeShapeAfter(T aShape, RMPDFWriter aWriter)
{
    // Get stroke and write pdf if not null
    if(aShape.getStroke()!=null && aShape.isStrokeOnTop())
        writeShapeStroke(aShape, aWriter);
        
    // Get pdf page
    PDFPageWriter pwriter = aWriter.getPageWriter();
    
    // Restore graphics state
    pwriter.grestore();

    // Add link, if it's there (What happens with rotated or skewed shapes?)
    String url = aShape.getURL();
    if(url!=null) {
        RMShape page = aShape.getPageShape();
        Rect frame = aShape.localToParent(aShape.getBoundsInside(), page).getBounds();
        frame.setY(page.getHeight() - frame.getMaxY());
        PDFAnnotation link = new PDFAnnotation.Link(frame, url);
        if(url.startsWith("textfield")) link = new PDFAnnotation.Widget(frame,"");
        pwriter.addAnnotation(link);
    }
}

/**
 * Writes PDF for given RMShape and it's stroke.
 */
protected void writeShapeStroke(RMShape aShape, RMPDFWriter aWriter)
{
    RMStroke stroke = aShape.getStroke();
    Shape path = aShape.getPath(), spath = stroke.getStrokePath(path);
    SnapPaintPdfr.writeShapeStroke(spath, stroke.snap(), stroke.getColor(), aWriter);
}

/**
 * Returns the shape pdfr for a shape.
 */
public static RMShapePdfr getPdfr(RMShape aShape)
{
    if(aShape instanceof RMTextShape) return RMShapePdfrs._textShapePdfr;
    if(aShape instanceof RMImageShape) return RMShapePdfrs._imgShapePdfr;
    if(aShape instanceof RMPDFShape) return RMShapePdfrs.getPDFShapePdfr();
    if(aShape instanceof RMPage) return RMShapePdfrs._pageShapePdfr;
    if(aShape instanceof RMScene3D) return RMShapePdfrs._scene3DPdfr;
    if(aShape instanceof ViewShape) return RMShapePdfrs.getViewShapePdfr();
    return _shapePdfr;
}

}