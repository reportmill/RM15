/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * This class is used to fill a shape's path with an image.
 */
public class RMImageFill extends RMFill {

    // An ImageRef reference to uniqued image
    ImageRef           _iref;
    
    // The snap ImagePaint
    ImagePaint         _snap;
    
    // An image placeholder for missing images
    static Image       _emptyImage;
    
/**
 * Creates a plain image fill.
 */
public RMImageFill()  { }

/**
 * Creates an image fill from an image source.
 */
public RMImageFill(Object aSource)  { this(aSource, false); }

/**
 * Creates an image fill from an image source and is tiled parameter.
 */
public RMImageFill(Object aSource, boolean isTiled)
{
    setImageForSource(aSource);
    if(isTiled) _snap = new ImagePaint(getImage());
    else _snap = new ImagePaint(getImage(), new Rect(0,0,1,1), false);
}

/**
 * Returns the image associated with this image fill.
 */
public Image getImage()  { return _iref!=null? _iref.getImage() : null; }

/**
 * Returns the ImageRef reference to uniqued image.
 */
public ImageRef getImageRef()  { return _iref; }

/**
 * Sets the image from given source.
 */
protected void setImageForSource(Object aSource)
{
    _iref = ImageRef.getImageRef(aSource);
}

/**
 * Returns the X location (offset) of the image fill image.
 */
public double getX()  { return isTiled()? _snap.getX() : 0; }

/**
 * Returns the Y location (offset) of the image fill image.
 */
public double getY()  { return isTiled()? _snap.getY() : 0; }

/**
 * Returns the width of the image fill image.
 */
public double getWidth()  { return _snap.getWidth(); }

/**
 * Returns the height of the image fill image.
 */
public double getHeight()  { return _snap.getHeight(); }

/**
 * Returns the scale x of the image fill image.
 */
public double getScaleX()  { return isAbsolute()? getWidth()/getImage().getWidth() : getWidth(); }

/**
 * Returns the scale y of the image fill image.
 */
public double getScaleY()  { return isAbsolute()? getHeight()/getImage().getHeight() : getHeight(); }

/**
 * Returns whether to tile fill image.
 */
public boolean isTiled()  { return isAbsolute(); }

/**
 * Returns whether paint is defined in terms independent of primitive to be filled.
 */
public boolean isAbsolute()  { return _snap.isAbsolute(); }

/**
 * Creates a new image fill for rect and absolute flag.
 */
public RMImageFill copyFor(Rect aRect, boolean isAbs)
{
    RMImageFill copy = (RMImageFill)clone(); copy._snap = _snap.copyFor(aRect, isAbs); return copy;
}

/**
 * Creates a new image fill for rect and absolute flag.
 */
public RMImageFill copyFor(double aX, double aY, double aW, double aH, boolean isAbs)
{
    return copyFor(new Rect(aX,aY,aW,aH), isAbs);
}

/**
 * Creates a new image fill identical to this image fill, but with new value for given attribute.
 */
public RMImageFill copyForScale(double aScaleX, double aScaleY)
{
    double w = isAbsolute()? getImage().getWidth()*aScaleX : aScaleX;
    double h = isAbsolute()? getImage().getHeight()*aScaleY : aScaleY;
    return copyFor(new Rect(getX(),getY(),w,h), isAbsolute());
}

/**
 * Creates a new image fill identical to this image fill, but with new value for given attribute.
 */
public RMImageFill copyTiled(boolean isTiled)
{
    RMImageFill copy = (RMImageFill)clone();
    if(isTiled) copy._snap = new ImagePaint(getImage());
    else copy._snap = new ImagePaint(getImage(), new Rect(0,0,1,1), false);
    return copy;
}

/**
 * Returns the snap version of this fill.
 */
public Paint snap()  { return _snap; }

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and super and get other
    if(anObj==this) return true;
    RMImageFill other = anObj instanceof RMImageFill? (RMImageFill)anObj : null; if(other==null) return false;
    if(other._iref!=_iref) return false;
    return _snap.equals(other._snap);
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic fill attributes and set type
    XMLElement e = super.toXML(anArchiver); e.add("type", "image");

    // Archive Image bytes
    Image img = getImage();
    if(img!=null) {
        String resName = anArchiver.addResource(img.getBytes(), getImageRef().getName());
        e.add("resource", resName);
    }
    
    // Archive Tile
    if(isTiled()) { e.add("Tile", true); e.add("fillstyle", 1); }
    
    // Archive X, Y, ScaleX, ScaleY
    if(getX()!=0) e.add("x", getX());
    if(getY()!=0) e.add("y", getY());
    if(getScaleX()!=1) e.add("scale-x", getScaleX());
    if(getScaleY()!=1) e.add("scale-y", getScaleY());
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic fill attributes
    super.fromXML(anArchiver, anElement);
    
    // Unarchive ImageName: get resource bytes, page and set ImageRef
    String iname = anElement.getAttributeValue("resource");
    if(iname!=null) {
        byte bytes[] = anArchiver.getResource(iname);
        setImageForSource(bytes);
    }
    
    // Unarchive Tile, legacy FillStyle (Stretch=0, Tile=1, Fit=2, FitIfNeeded=3)
    boolean tiled = anElement.getAttributeBooleanValue("Tile", false);
    if(anElement.hasAttribute("fillstyle")) tiled = anElement.getAttributeIntValue("fillstyle")==1;
    
    // Unarchive X, Y
    double x = 0, y = 0;
    if(anElement.hasAttribute("x")) x = anElement.getAttributeFloatValue("x");
    if(anElement.hasAttribute("y")) y = anElement.getAttributeFloatValue("y");
    
    // Unarchive ScaleX, ScaleY
    double sx = anElement.getAttributeFloatValue("scale-x", 1);
    double sy = anElement.getAttributeFloatValue("scale-y", 1);
        
    // Create Snap ImagePaint
    Image img = getImage(); if(img==null) img = getEmptyImage();
    if(tiled) _snap = new ImagePaint(img, new Rect(x,y,img.getWidth()*sx,img.getHeight()*sx), true);
    else _snap = new ImagePaint(img, new Rect(0,0,sx,sy), false);
    
    // Return this fill
    return this;
}

/**
 * Returns an image place holder for missing images.
 */
public static Image getEmptyImage()
{
    if(_emptyImage!=null) return _emptyImage;
    WebURL url = WebURL.getURL(RMImageFill.class, "DefaultImage.png");
    return _emptyImage = Image.get(url);
}

}