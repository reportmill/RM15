/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.util.MathUtils;
import snap.pdf.*;
import snap.gfx.*;

/**
 * Provides info for an encapsulated PDF (a PDF used as an image).
 */
public class RMImageDataPDF extends RMImageData {
    
    // The PDF file
    PDFFile          _pdfFile;

/**
 * Returns the PDF file for the PDF image data (creating if necessary).
 */
public PDFFile getPDFFile()  { return _pdfFile; }

/**
 * Reads the basic info from PDF data.
 */
public void readBasicInfo()
{
    // Set type
    _type = "pdf";
    
    // Create PDF file from image data bytes and set
    if(_pdfFile==null)
        _pdfFile = new PDFFile(getBytes());
    
    // Get pdf page
    PDFPage page = _pdfFile.getPage(getPageIndex());
    
    // Set image data width and height
    _width = (int)Math.ceil(page.getCropBox().getWidth());
    _height = (int)Math.ceil(page.getCropBox().getHeight());
    
    // Set image data page count
    _pageCount = _pdfFile.getPageCount();
}

/**
 * Override to get image from PDFFile.
 */
public Image createImage()  { return getPDFFile().getPage(getPageIndex()).getImage(); }

/**
 * Returns the image data for a successive page.
 */
public RMImageData getPage(int aPage)
{
    int page = MathUtils.clamp(aPage, 0, _pageCount-1); if(page==_pageIndex) return this;
    RMImageData clone = clone();
    clone._pageIndex = page; clone._image = null;
    clone.readBasicInfo();
    return clone;
}

/**
 * Draw at maximum resolution.  Page is scaled & translated to fit exactly in r.
 */
public void paint(Painter aPntr, double x, double y, double w, double h)
{
    // If not printing, do normal version and return
    if(!aPntr.isPrinting()) { super.paint(aPntr, x, y, w, h); return; }
    
    // Otherwise draw page into Graphics2D
    getPDFFile().getPage(getPageIndex()).paint(aPntr, new Rect(x,y,w,h));
}

/**
 * Returns whether PDF image reader can read files with given extension.
 */    
public static boolean canRead(String anExt)  { return anExt!=null && anExt.toLowerCase().equals("pdf"); }

/**
 * Returns whether PDF image reader can read the file provided in the byte array.
 */
public static boolean canRead(byte data[])
{
    // Return true if first 5 bytes are "%PDF-"
    if(data==null || data.length<10) return false;
    return data[0]==(byte)'%' && data[1]==(byte)'P' && data[2]==(byte)'D' && data[3]==(byte)'F' && data[4]==(byte)'-';
}

}