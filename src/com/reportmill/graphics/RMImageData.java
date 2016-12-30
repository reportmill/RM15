/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.lang.ref.WeakReference;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * This class manages image data. Each instance holds the raw image data and provides methods to return
 * attributes of the decoded image.
 */
public class RMImageData implements Cloneable {

    // The object that provided image bytes
    Object              _source;
    
    // The time the source was last modified (in milliseconds since 1970)
    long                _modTime;

    // The AWT version of this image
    Image               _image;

    // The original file bytes
    byte                _bytes[];
    
    // The image bytes uncompressed
    byte                _bytesDecoded[];
    
    // The image page index (if from multi-page image type like PDF)
    int                 _pageIndex;
    
    // The image page count (if from multi-page image type like PDF)
    int                 _pageCount = 1;
    
    // The image type ("gif", "jpg", "png", etc.)
    String              _type = "";
    
    // The image pixels wide/high
    int                 _width, _height;
    
    // The image DPI vertical/horizontal
    double               _dpiX, _dpiY;
    
    // The image samples per pixel
    int                 _spp;
    
    // The image bits per sample
    int                 _bps;
    
    // Whether image has alpha
    boolean             _hasAlpha;
    
    // The image color map (if indexed color)
    byte                _colorMap[];
    
    // The image transparent color index (if indexed color with alpha index)
    int                 _transparentColorIndex = -1;
    
    // Whether this image is valid
    boolean             _valid = true;
    
    // The cache used to hold application instances
    static List <WeakReference<RMImageData>>  _cache = new ArrayList();
    
    // A shared empty ImageData
    public static RMImageData EMPTY = getImageData(RMImageData.class.getResourceAsStream("DefaultImage.png"), 0);
    
/**
 * Returns an image data loaded from aSource. If image type supports multiple pages, page index can be specified.
 */
public static synchronized RMImageData getImageData(Object aSource, int aPageIndex)
{
    // If source is null, return EMPTY, if image data, return it dereferencing given page
    if(aSource==null) return EMPTY;
    if(aSource instanceof RMImageData) return ((RMImageData)aSource).getPage(aPageIndex);
    
    // Get source url
    WebURL url = null; try { url = WebURL.getURL(aSource); } catch(Exception e) { }
    
    // Iterate over image list and see if any match source
    for(int i=_cache.size()-1; i>0; i--) { RMImageData idata = _cache.get(i).get();
        
        // If null, remove weak reference and continue)
        if(idata==null) { _cache.remove(i); continue; }
        
        // If source matches cached source, return
        if(url!=null && url.equals(idata.getSourceURL()) || aSource==idata.getSource()) {
            idata.refresh();
            return idata.getPage(aPageIndex);
        }
    }
    
    // Get bytes for source
    byte bytes[] = url!=null && url.getFile()!=null? url.getFile().getBytes() : SnapUtils.getBytes(aSource);
    
    // Create new ImageData, add to cache (as WeakReference) and return
    RMImageData idata = RMImageDataPDF.canRead(bytes)? new RMImageDataPDF() : new RMImageData();
    idata.setSource(url!=null? url : bytes!=null? bytes : aSource, aPageIndex);
    _cache.add(new WeakReference(idata));
    return idata;
}

/**
 * Returns the original source for the image (byte[], File, InputStream or whatever).
 */
public Object getSource()  { return _source; }

/**
 * Sets the source.
 */
protected void setSource(Object aSource, int aPageIndex)
{
    // Get URL, source, modified time
    WebURL url = null; try { url = WebURL.getURL(aSource); } catch(Exception e) { }
    _source = url!=null? url : aSource;
    _modTime = url!=null && url.getFile()!=null? url.getFile().getLastModifiedTime() : System.currentTimeMillis();

    // If source is image, set basic info
    if(aSource instanceof Image) {
        _image = (Image)aSource;
        _type = _image.hasAlpha()? "png" : "jpg";
        _width = (int)_image.getWidth(); _height = (int)_image.getHeight();
        _hasAlpha = _image.hasAlpha();
        _spp = _hasAlpha? 4 : 3;
        _bps = 8;
        _bytes = null;
    }
    
    // Otherwise, assume source can provide bytes
    else if(aSource!=null) {
        _bytes = url!=null && url.getFile()!=null? url.getFile().getBytes() : SnapUtils.getBytes(aSource); // Get bytes
        _pageIndex = aPageIndex;  // Set PageIndex
        readBasicInfo(); _image = null; // Get reader and clear image
    }
}

/**
 * Returns the source URL, if loaded from URL.
 */
public WebURL getSourceURL()  { return _source instanceof WebURL? (WebURL)_source : null; }

/**
 * Reads basic image info.
 */
public void readBasicInfo()
{
    // Set image type
    _type = ImageUtils.getImageType(getBytes());
    
    // Special case jpg, since PDF & Flash can make do with raw file data and _pw, _ph & _bps
    if(_type.equals("jpg")) {
        ImageUtils.ImageInfo info = ImageUtils.getInfoJPG(getBytes());
        _width = info.width; _height = info.height;
        _spp = info.spp; _bps = info.bps; _dpiX = info.dpiX; _dpiY = info.dpiY;
        return;
    }
    
    // Read image
    Image img = getImage();
    
    // Get basic info from image (width, height, samples per pixel, bits per sample)
    _width = (int)img.getWidth();
    _height = (int)img.getHeight();
    
    // Get samples per pixel and bits per sample from color model
    _spp = img.getSamplesPerPixel();
    _bps = 8; // Only support 8 bit samples (expand everything)
    if(_spp==2) // Grayscale/alpha unsupported, expand to RGB
        _spp = 4;
    _hasAlpha = img.hasAlpha();

    // Specail handling of IndexColorModel
    if(img.isIndexedColor()) {
        _spp = 1; _colorMap = img.getColorMap(); _transparentColorIndex = img.getAlphaColorIndex(); }
}

/**
 * Returns the name for the image (assigned from our hashCode).
 */
public String getName()  { return "" + System.identityHashCode(this); }

/**
 * Returns the type for the image (one of gif, jpg, png, pdf, etc.).
 */
public String getType()  { return _type; }

/**
 * Returns the number of pixels horizontally.
 */
public int getWidth()  { return _width; }

/**
 * Returns the number of pixels vertically.
 */
public int getHeight()  { return _height; }

/**
 * Returns the actual display width of the image in printer's points using the image DPI if available.
 */
public double getImageWidth()  { return _dpiX>0? _width*(72f/_dpiX) : _width; }

/**
 * Returns the actual display height of the image in printer's points using the image DPI if available.
 */
public double getImageHeight()  { return _dpiY>0? _height*(72f/_dpiY) : _height; }

/**
 * Returns whether the image is non-grayscale.
 */
public boolean isColor()  { return isIndexedColor() || _spp>2; }

/**
 * Returns whether image has transparency.
 */
public boolean hasAlpha()  { return _hasAlpha; }

/**
 * Returns the number of samples per pixel (RGB=3, RGBA=4, GrayScale=1, etc.).
 */
public int getSamplesPerPixel()  { return _spp; }

/**
 * Returns the number of bits per sample (eg, 24 bit RGB image is 8 bits per sample).
 */
public int getBitsPerSample()  { return _bps; }

/**
 * Returns the number of bits per pixel (derived from bits per sample and samples per pixel).
 */
public int getBitsPerPixel()  { return getBitsPerSample()*getSamplesPerPixel(); }

/**
 * Returns the number of bytes per row (derived from width and bits per pixel).
 */
public int getBytesPerRow()  { return (getWidth()*getBitsPerPixel()+7)/8; }

/**
 * Returns the buffered image for image data.
 */
public Image getImage()  { return _image!=null? _image : (_image=createImage()); }

/**
 * Creates the image for this ImageData.
 */
public Image createImage()  { return Image.get(getBytes()); }

/**
 * Returns the original bytes for the image (loaded from the source).
 */
public byte[] getBytes()  { return _bytes!=null? _bytes : (_bytes=createBytes()); }

/**
 * Creates bytes from the image (loaded from the source).
 */
protected byte[] createBytes()  { return getSource() instanceof Image? getImage().getBytes() : null; }

/**
 * Returns the decoded image bytes for the image.
 */
public byte[] getBytesDecoded()  { return _bytesDecoded!=null? _bytesDecoded : (_bytesDecoded=createBytesDecoded()); }

/**
 * Creates the decoded bytes for the image data.
 */
protected byte[] createBytesDecoded()
{
    try { return getImage().getBytesRGBA(); }
    catch(Exception e) { _valid = false; return new byte[_height*getBytesPerRow()]; }
}

/**
 * Returns whether image uses a color map.
 */
public boolean isIndexedColor()  { return _colorMap!=null; }

/**
 * Color map support: returns the bytes of color map from a color map image.
 */
public byte[] getColorMap()  { return _colorMap; }

/**
 * Color map support: returns the index of the transparent color in a color map image.
 */
public int getAlphaColorIndex()  { return _transparentColorIndex; }

/**
 * Returns the page index for the image.
 */
public int getPageIndex()  { return _pageIndex; }

/**
 * Returns the total number of pages for the image.
 */
public int getPageCount()  { return _pageCount; }

/**
 * Returns the image data for an alternate page.
 */
public RMImageData getPage(int aPage)  { return this; }

/**
 * Returns whether the image was loaded successfully.
 */
public boolean isValid()  { return _valid; }

/**
 * Refreshes data from source.
 */
protected void refresh()
{
    WebURL url = getSourceURL(); if(url==null || url.getFile()==null) return;
    url.getFile().reload();
    if(url.getFile().getLastModifiedTime()>_modTime)
        setSource(url, _pageIndex);
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other
    if(anObj==this) return true;
    RMImageData other = anObj instanceof RMImageData? (RMImageData)anObj : null; if(other==null) return false;
    
    // Check bytes (use method in case images source was java Image and bytes need to be generated), PageIndex
    if(!ArrayUtils.equals(other.getBytes(), getBytes())) return false;
    if(other._pageIndex!=_pageIndex) return false;
    return true; // Return true since all checks passed
}

/**
 * Standard clone implementation.
 */
public RMImageData clone()
{
    try { return (RMImageData)super.clone(); }
    catch(Exception e) { e.printStackTrace(); return null; }
}

/**
 * Draws image data in given rect.
 */
public void paint(Painter aPntr, double x, double y, double w, double h)
{
    // Get image
    Image image = getImage();
    if(image==null) image = EMPTY.getImage();
    
    // If image is non-null, draw it
    if(image!=null) {
        double sx = w/image.getPixWidth(), sy = h/image.getPixHeight();
        Transform transform = Transform.get(sx, 0, 0, sy, x, y);
        aPntr.drawImage(image, transform);
    }
}

/**
 * Returns whether given extension is supported.
 */
public static boolean canRead(String anExt)  { return Image.canRead(anExt) || RMImageDataPDF.canRead(anExt); }

/**
 * Returns whether image reader can read the file provided in the byte array.
 */
public static boolean canRead(byte bytes[])  { return Image.canRead(bytes) || RMImageDataPDF.canRead(bytes); }

}