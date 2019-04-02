/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMKeyChain;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * This class is a shape representation of an image.
 */
public class RMImageShape extends RMRectShape {
    
    // The key used to get image during RPG
    String             _key;
    
    // An ImageRef reference to uniqued image
    ImageRef           _imgRef;
    
    // The padding
    int                _padding;
    
    // Whether to grow image to fit available area if shape larger than image.
    boolean            _growToFit = true;
    
    // Whether to preserve the natural width to height ratio of image
    boolean            _preserveRatio = true;
    
    // The image name, if image read from external file
    String             _iname;
    
/**
 * Creates an RMImageShape.
 */
public RMImageShape()  { }

/**
 * Creates an RMImageShape from the image source provided.
 */
public RMImageShape(Object aSource)  { setImageForSource(aSource); setBestSize(); }

/**
 * Returns the report key used to load an image if none is provided.
 */
public String getKey()  { return _key; }

/**
 * Sets the report key used to load an image if none is provided.
 */
public void setKey(String aString)
{
    firePropChange("Key", _key, _key = aString);
}

/**
 * Returns the ImageRef reference to uniqued image.
 */
public ImageRef getImageRef()  { return _imgRef; }

/**
 * Sets the ImageRef reference to uniqued image.
 */
protected void setImageRef(ImageRef anImageRef)
{
    if(anImageRef==getImageRef()) return;
    _imgRef = anImageRef;
    if(getParent()!=null) getParent().relayout(); repaint();
}

/**
 * Returns the image.
 */
public Image getImage()  { return _imgRef!=null? _imgRef.getImage() : null; }

/**
 * Sets the image from given source.
 */
public void setImageForSource(Object aSource)
{
    ImageRef iref = ImageRef.getImageRef(aSource);
    setImageRef(iref);
}

/**
 * Returns the padding.
 */
public int getPadding()  { return _padding; }

/**
 * Sets the padding.
 */
public void setPadding(int aPadding)
{
    firePropChange("Padding", _padding, _padding = aPadding);
    repaint();
}

/**
 * Returns the horizontal alignment.
 */
public AlignX getAlignmentX()  { return _alignX; } AlignX _alignX = AlignX.Center;

/**
 * Sets the horizontal alignment.
 */
public void setAlignmentX(AlignX anAlignX)  { _alignX = anAlignX; }

/**
 * Returns the vertical alignment.
 */
public AlignY getAlignmentY()  { return _alignY; } AlignY _alignY = AlignY.Middle;

/**
 * Sets the vertical alignment.
 */
public void setAlignmentY(AlignY anAlignY)  { _alignY = anAlignY; }

/**
 * Returns whether to grow image to fit available area if shape larger than image.
 */
public boolean isGrowToFit()  { return _growToFit; }

/**
 * Sets whether to grow image to fit available area if shape larger than image.
 */
public void setGrowToFit(boolean aValue)
{
    firePropChange("GrowToFit", _growToFit, _growToFit = aValue);
    repaint();
}

/**
 * Returns whether to preserve the natural width to height ratio of image.
 */
public boolean getPreserveRatio()  { return _preserveRatio; }

/**
 * Sets whether to preserve the natural width to height ratio of image.
 */
public void setPreserveRatio(boolean aValue)
{
    firePropChange("PreserveRatio", _preserveRatio, _preserveRatio = aValue);
    repaint();
}

/**
 * Returns the preferred width.
 */
protected double getPrefWidthImpl(double aHeight)
{
    Image img = getImage(); if(img==null) return 0;
    double pw = img.getWidth(), ph = img.getHeight();
    if(aHeight>0 && getPreserveRatio() && ph>aHeight) pw = aHeight*pw/ph;
    return pw;
}

/**
 * Returns the preferred height.
 */
protected double getPrefHeightImpl(double aWidth)
{
    Image img = getImage(); if(img==null) return 0;
    double pw = img.getWidth(), ph = img.getHeight();
    if(aWidth>0 && getPreserveRatio() && pw>aWidth) ph = aWidth*ph/pw;
    return ph;
}

/**
 * Report generation method.
 */
public RMShape rpgShape(ReportOwner aRptOwner, RMShape aParent)
{
    // Do normal version
    RMImageShape clone = (RMImageShape)super.rpgShape(aRptOwner, aParent);
    
    // If key: Evaluate key for image and set
    String key = getKey();
    if(key!=null && key.length()>0) {
        
        // Get key value
        Object value = RMKeyChain.getValue(aRptOwner, getKey());
        if(value instanceof RMKeyChain) value = ((RMKeyChain)value).getValue();
        WebURL url = WebURL.getURL(value); if(url!=null) value = url;
        
        // If PDF data, return PDFShape
        if(url!=null && url.getPath().toLowerCase().endsWith("pdf") ||
            value instanceof byte[] && RMPDFData.canRead((byte[])value))
                return RMPDFShape.rpgShape(aRptOwner, aParent, this, value);
                
        // Otherwise set new image
        clone.setImageForSource(value);
    }

    // This prevents RMImageShape from growing to actual image size in report
    // Probably need a GrowShapeToFit attribute to allow RPG image shape to grow
    clone.setPrefHeight(getHeight()*getScaleY());
    
    // Return clone
    return clone;
}

/**
 * Override to paint shape.
 */
protected void paintShape(Painter aPntr)
{
    // Do normal version
    super.paintShape(aPntr);
    
    // Get image (use empty placeholder image if null and editing)
    Image img = getImage();
    if(img==null) {
        if(!RMShapePaintProps.isEditing(aPntr)) return;
        img = RMImageFill.getEmptyImage(); if(img==null) return;
    }
    
    // Draw image transformed to bounds
    aPntr.clip(getPath());
    Rect ibnds = getImageBounds();
    double sx = ibnds.width/img.getPixWidth(), sy = ibnds.height/img.getPixHeight();
    Transform transform = new Transform(sx, 0, 0, sy, ibnds.x, ibnds.y);
    aPntr.drawImage(img, transform);
}

/**
 * Returns the image bounds.
 */
public Rect getImageBounds()
{
    // Get image and padding
    Image img = getImage(); if(img==null) img = RMImageFill.getEmptyImage();
    int pd = getPadding();
    
    // Get width/height for shape, image and padded area
    double sw = getWidth(), sh = getHeight();
    double iw = img.getWidth(), ih = img.getHeight();
    double pw = sw - pd*2, ph = sh - pd*2; if(pw<0) pw = 0; if(ph<0) ph = 0;
    
    // Get image bounds width/height, ShrinkToFit if greater than available space (with PreserveRatio, if set)
    double w = iw, h = ih; if(isGrowToFit()) { w = pw+1; h = ph+1; }
    if(w>pw) { w = pw; if(getPreserveRatio()) h = ih*w/iw; }
    if(h>ph) { h = ph; if(getPreserveRatio()) w = iw*h/ih; }
    
    // Get image bounds x/y for width/height and return rect
    AlignX ax = getAlignmentX(); AlignY ay = getAlignmentY();
    double x = ax==AlignX.Center? (sw - w)/2 : ax==AlignX.Left? pd : (sw - w);
    double y = ay==AlignY.Middle? (sh - h)/2 : ay==AlignY.Top? pd : (sh - h);
    return new Rect(x, y, w, h);
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name to image-shape
    XMLElement e = super.toXML(anArchiver); e.setName("image-shape");
    
    // Archive ImageName, if image read from external file
    if(_iname!=null) e.add("ImageName", _iname);
    
    // Archive Image
    else if(getImage()!=null) {
        String resName = anArchiver.addResource(getImage().getBytes(), getImageRef().getName());
        e.add("resource", resName);
    }
    
    // Archive Key, Padding, Alignment, GrowToFit, PreserveRatio
    if(_key!=null && _key.length()>0) e.add("key", _key);
    if(_padding>0) e.add("Padding", _padding);
    if(getAlignment()!=Pos.CENTER) e.add("Alignment", getAlignment());
    if(!isGrowToFit()) e.add("GrowToFit", isGrowToFit());
    if(!getPreserveRatio()) e.add("PreserveRatio", getPreserveRatio());
    
    // Return
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXML(anArchiver, anElement);
    
    // Unarchive Image resource: get resource bytes and set Image (if PDF - swap out RMPDFShape)
    String rname = anElement.getAttributeValue("resource");
    if(rname!=null) {
        byte bytes[] = anArchiver.getResource(rname);
        if(RMPDFData.canRead(bytes)) return new RMPDFShape().fromXML(anArchiver, anElement);
        _imgRef = ImageRef.getImageRef(bytes);
    }
    
    // Unarchive ImageName
    _iname = anElement.getAttributeValue("ImageName");
    if(_iname!=null) {
        Image img = Image.get(anArchiver.getSourceURL(), _iname);
        if(img!=null)
            _imgRef = ImageRef.getImageRef(img.getSource());
    }
    
    // Unarchive Key, Padding, GrowToFit, PreserveRatio
    if(anElement.hasAttribute("key")) setKey(anElement.getAttributeValue("key"));
    if(anElement.hasAttribute("Padding")) setPadding(anElement.getAttributeIntValue("Padding"));
    if(anElement.hasAttribute("GrowToFit")) setGrowToFit(anElement.getAttributeBooleanValue("GrowToFit"));
    if(anElement.hasAttribute("PreserveRatio")) setPreserveRatio(anElement.getAttributeBooleanValue("PreserveRatio"));
    
    // Legacy alignment (RM14)
    if(anElement.hasAttribute("Alignment")) {
        String as = anElement.getAttributeValue("Alignment");
        String s[] = { "TopLeft", "TopCenter", "TopRight", "CenterLeft", "Center", "CenterRight",
            "BottomLeft", "BottomCenter", "BottomRight" };
        int i = ArrayUtils.indexOf(s, as);
        if(i>=0) setAlignment(Pos.values()[i]);
    }
    
    // Legacy: If Fill is ImageFill and no ImageRef+Key or ImageFill.ImageRef, set ImageRef from IFill and clear fill
    if(getFill() instanceof RMImageFill) { RMImageFill ifill = (RMImageFill)getFill();
        XMLElement fill = anElement.get("fill"); ImageRef iref = ifill.getImageRef();
        if(getImageRef()==null && !ifill.isTiled()) { // && getKey()==null) {
            int fs = fill.getAttributeIntValue("fillstyle", 0); // Stretch=0, Tile=1, Fit=2, FitIfNeeded=3
            if(fs==0) { setImageRef(iref); setFill(null); setGrowToFit(true); setPreserveRatio(false); }
            else if(fs==2) { setImageRef(iref); setFill(null); setGrowToFit(true); setPreserveRatio(true); }
            else if(fs==3) { setImageRef(iref); setFill(null); setGrowToFit(false); setPreserveRatio(true); }
            double x = fill.getAttributeFloatValue("x"); if(x!=0) setAlignmentX(x<0? AlignX.Left : AlignX.Right);
            double y = fill.getAttributeFloatValue("y"); if(y!=0) setAlignmentY(y<0? AlignY.Top : AlignY.Bottom);
        }
        else if(iref==null) setFill(null);
        setPadding(fill.getAttributeIntValue("margin"));
    }
    
    // Return
    return this;
}

}