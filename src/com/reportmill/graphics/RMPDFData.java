/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.lang.ref.WeakReference;
import java.util.*;
import snap.util.*;
import snap.web.WebURL;
import snappdf.*;
import snap.gfx.*;

/**
 * Provides info for an encapsulated PDF (a PDF used as an image).
 */
public class RMPDFData implements Cloneable {
    
    // The object that provided image bytes
    Object              _source;
    
    // The time the source was last modified (in milliseconds since 1970)
    long                _modTime;

    // The AWT version of this image
    Image               _image;

    // The original file bytes
    byte                _bytes[];
    
    // The image page index (if from multi-page image type like PDF)
    int                 _pageIndex;
    
    // The image page count (if from multi-page image type like PDF)
    int                 _pageCount = 1;
    
    // The image pixels wide/high
    int                 _width, _height;
    
    // The PDF file
    PDFFile             _pdfFile;

    // The cache used to hold application instances
    static List <WeakReference<RMPDFData>>  _cache = new ArrayList();
    
/**
 * Returns the original source for the image (byte[], File, InputStream or whatever).
 */
public Object getSource()  { return _source; }

/**
 * Sets the source.
 */
protected void setSource(Object aSource, int aPageIndex)
{
    // If no source, complain and return
    if(aSource==null) { System.err.println("RMPDFData.setSource: Source is null"); return; }
    
    // Get URL, source, modified time
    WebURL url = WebURL.getURL(aSource);
    _source = url!=null? url : aSource;
    _modTime = url!=null? url.getLastModTime() : System.currentTimeMillis();

    // Set Bytes, PageIndex
    _bytes = url!=null? url.getBytes() : SnapUtils.getBytes(aSource); // Get bytes
    _pageIndex = aPageIndex;  // Set PageIndex
    readBasicInfo(); _image = null; // Get reader and clear image
}

/**
 * Returns the source URL, if loaded from URL.
 */
public WebURL getSourceURL()  { return _source instanceof WebURL? (WebURL)_source : null; }

/**
 * Reads the basic info from PDF data.
 */
void readBasicInfo()
{
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
 * Returns the PDF file for the PDF image data (creating if necessary).
 */
public PDFFile getPDFFile()  { return _pdfFile; }

/**
 * Returns the name for the image (assigned from our hashCode).
 */
public String getName()  { return String.valueOf(System.identityHashCode(this)); }

/**
 * Returns the pdf width.
 */
public double getWidth()  { return _width; }

/**
 * Returns the pdf height.
 */
public double getHeight()  { return _height; }

/**
 * Returns the original bytes for the image (loaded from the source).
 */
public byte[] getBytes()  { return _bytes; }

/**
 * Returns the buffered image for image data.
 */
public Image getImage()
{
    if(_image!=null) return _image;
    return _image = getPDFFile().getPage(getPageIndex()).getImage();
}

/**
 * Returns the page index for the image.
 */
public int getPageIndex()  { return _pageIndex; }

/**
 * Returns the total number of pages for the image.
 */
public int getPageCount()  { return _pageCount; }

/**
 * Returns the image data for a successive page.
 */
public RMPDFData getPage(int aPage)
{
    int page = MathUtils.clamp(aPage, 0, _pageCount-1); if(page==_pageIndex) return this;
    RMPDFData clone = clone();
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
    Image image = getImage(); //if(image==null) image = EMPTY.getImage();
    double sx = w/image.getPixWidth(), sy = h/image.getPixHeight();
    Transform transform = new Transform(sx, 0, 0, sy, x, y);
    aPntr.drawImage(image, transform);
    
    // Otherwise draw page into Graphics2D
    getPDFFile().getPage(getPageIndex()).paint(aPntr, new Rect(x,y,w,h));
}

/**
 * Refreshes data from source.
 */
protected void refresh()
{
    WebURL url = getSourceURL();
    long modTime = url!=null? url.getLastModTime() : 0;
    if(modTime>_modTime) {
        setSource(url, _pageIndex); System.out.println("Refreshed PDFData"); }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other
    if(anObj==this) return true;
    RMPDFData other = anObj instanceof RMPDFData? (RMPDFData)anObj : null; if(other==null) return false;
    
    // Check bytes, PageIndex
    if(!ArrayUtils.equals(other.getBytes(), getBytes())) return false;
    if(other._pageIndex!=_pageIndex) return false;
    return true;
}

/**
 * Standard clone implementation.
 */
public RMPDFData clone()
{
    try { return (RMPDFData)super.clone(); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/**
 * Standard toString implementation.
 */
public String toString()
{
    String str = getClass().getSimpleName() + " { ";
    str += "Width=" + getWidth() + ", Height=" + getHeight();
    if(getPageCount()>0) str += ", Page=" + getPageIndex();
    if(getSourceURL()!=null) str += ", URL=" + getSourceURL();
    str += " }";
    return str;
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

/**
 * Returns a RMPDFData loaded from aSource.
 */
public static synchronized RMPDFData getPDFData(Object aSource)
{
    // If source is null, return EMPTY, if image data, return it dereferencing given page
    //if(aSource==null) return EMPTY;
    if(aSource instanceof RMPDFData) return (RMPDFData)aSource;
    
    // Get source url
    WebURL url = WebURL.getURL(aSource);
    
    // Iterate over image list and see if any match source
    for(int i=_cache.size()-1; i>=0; i--) { RMPDFData idata = _cache.get(i).get();
        
        // If null, remove weak reference and continue)
        if(idata==null) { _cache.remove(i); continue; }
        
        // If source matches cached source, return
        if(url!=null && url.equals(idata.getSourceURL()) || aSource==idata.getSource()) {
            idata.refresh();
            return idata;
        }
    }
    
    // Get bytes for source
    byte bytes[] = url!=null? url.getBytes() : SnapUtils.getBytes(aSource);
    if(bytes==null)
        return null;
    
    // Create new ImageData, add to cache (as WeakReference) and return
    RMPDFData idata = new RMPDFData();
    idata.setSource(url!=null? url : bytes, 0);
    _cache.add(new WeakReference(idata));
    return idata;
}

}