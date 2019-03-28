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
public class RMImageData {

    // The object that provided image bytes
    Object              _source;
    
    // The time the source was last modified (in milliseconds since 1970)
    long                _modTime;

    // The platform native version of this image
    Image               _image;

    // The original file bytes
    byte                _bytes[];
    
    // The image points wide/high
    double              _width, _height;
    
    // The cache used to hold application instances
    static List <WeakReference<RMImageData>>  _cache = new ArrayList();
    
    // A shared empty ImageData
    public static RMImageData EMPTY = getImageData(WebURL.getURL(RMImageData.class, "DefaultImage.png"));
    
/**
 * Returns the original source for the image (byte[], File, InputStream or whatever).
 */
public Object getSource()  { return _source; }

/**
 * Sets the source.
 */
protected void setSource(Object aSource)
{
    // Get URL, source, modified time
    WebURL url = WebURL.getURL(aSource);
    _source = url!=null? url : aSource; if(_source==null) return;
    _modTime = url!=null? url.getLastModTime() : System.currentTimeMillis();

    // Otherwise, assume source can provide bytes
    _bytes = url!=null? url.getBytes() : SnapUtils.getBytes(aSource); _image = null;
    readBasicInfo();
}

/**
 * Returns the source URL, if loaded from URL.
 */
public WebURL getSourceURL()  { return _source instanceof WebURL? (WebURL)_source : null; }

/**
 * Reads basic image info.
 */
void readBasicInfo()
{
    // Special case jpg, since PDF can embed raw file data and _pw, _ph & _bps
    String type = ImageUtils.getImageType(getBytes());
    if(type.equals("jpg")) {
        ImageUtils.ImageInfo info = ImageUtils.getInfoJPG(getBytes());
        _width = info.width*(72d/info.dpiX);
        _height = info.height*(72d/info.dpiY);
        return;
    }
    
    // Get basic info from image
    Image img = getImage();
    _width = (int)img.getWidth();
    _height = (int)img.getHeight();
}

/**
 * Returns the name for the image (assigned from our hashCode).
 */
public String getName()  { return String.valueOf(System.identityHashCode(this)); }

/**
 * Returns the actual display width of the image in printer points.
 */
public double getWidth()  { return _width; }

/**
 * Returns the actual display height of the image in printer points.
 */
public double getHeight()  { return _height; }

/**
 * Returns the buffered image for image data.
 */
public Image getImage()
{
    if(_image!=null) return _image;
    return _image = Image.get(getBytes());
}

/**
 * Returns the original bytes for the image (loaded from the source).
 */
public byte[] getBytes()
{
    if(_bytes!=null) return _bytes;
    byte bytes[] = _image!=null? _image.getBytes() : null;
    return _bytes = bytes;
}

/**
 * Refreshes data from source.
 */
protected void refresh()
{
    WebURL url = getSourceURL();
    long modTime = url!=null? url.getLastModTime() : 0;
    if(modTime>_modTime) {
        setSource(url); System.out.println("Refreshed ImageData"); }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    if(anObj==this) return true;
    RMImageData other = anObj instanceof RMImageData? (RMImageData)anObj : null; if(other==null) return false;
    return ArrayUtils.equals(other.getBytes(), getBytes());
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "RMImageData: URL=" + getSourceURL(); }

/**
 * Returns an image data loaded from aSource. If image type supports multiple pages, page index can be specified.
 */
public static synchronized RMImageData getImageData(Object aSource)
{
    // If source is null, return EMPTY, if image data, return it dereferencing given page
    if(aSource==null) return EMPTY;
    if(aSource instanceof RMImageData) return (RMImageData)aSource;
    
    // Handle Image - I don't think anything does this anymore
    if(aSource instanceof Image) return getImageData((Image)aSource);
    
    // Get source url
    WebURL url = WebURL.getURL(aSource);
    
    // Iterate over image list and see if any match source
    for(int i=_cache.size()-1; i>0; i--) { RMImageData idata = _cache.get(i).get();
        
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
    RMImageData idata = new RMImageData(); idata.setSource(url!=null? url : bytes);
    _cache.add(new WeakReference(idata));
    return idata;
}

/** Returns an image data loaded from Image. */
private static RMImageData getImageData(Image img) {System.out.println("RMImageData.init(img): Never gets called");
    RMImageData idata = new RMImageData(); idata._image = img;
    idata._width = img.getPixWidth(); idata._height = img.getPixHeight();
    return idata;
}

}