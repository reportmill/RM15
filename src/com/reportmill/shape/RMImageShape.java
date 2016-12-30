/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMKeyChain;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.util.*;

/**
 * This class is a shape representation of an image.
 */
public class RMImageShape extends RMRectShape {
    
    // The key used to get image data during RPG
    String             _key;
    
    // The image data
    RMImageData        _imageData;
    
    // The page index
    int                _pageIndex;
    
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
public RMImageShape(Object aSource)  { setImageData(aSource); setBestSize(); }

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
 * Returns the image data.
 */
public RMImageData getImageData()  { return _imageData; }

/**
 * Sets the image data.
 */
public void setImageData(RMImageData anImageData)
{
    RMImageData idata = anImageData!=RMImageData.EMPTY? anImageData : null; if(idata==getImageData()) return;
    _imageData = idata;
    setPageIndex(idata.getPageIndex());
    if(getParent()!=null) getParent().relayout(); repaint();
}

/**
 * Sets the image data from given source.
 */
public void setImageData(Object aSource)  { setImageData(RMImageData.getImageData(aSource, 0)); }

/**
 * Returns the page index.
 */
public int getPageIndex()  { return _pageIndex; }

/**
 * Sets the page index.
 */
public void setPageIndex(int anIndex)
{
    int index = anIndex; if(getImageData()!=null) index = MathUtils.clamp(index, 0, getImageData().getPageCount()-1);
    if(index==getPageIndex()) return;
    firePropChange("PageIndex", _pageIndex, _pageIndex = index);
    if(getImageData()!=null) setImageData(getImageData().getPage(_pageIndex));
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
protected double computePrefWidth(double aHeight)
{
    RMImageData id = getImageData(); if(id==null) return 0;
    double pw = id.getImageWidth(), ph = id.getImageHeight();
    if(aHeight>0 && getPreserveRatio() && ph>aHeight) pw = aHeight*pw/ph;
    return pw;
}

/**
 * Returns the preferred height.
 */
protected double computePrefHeight(double aWidth)
{
    RMImageData id = getImageData(); if(id==null) return 0;
    double pw = id.getImageWidth(), ph = id.getImageHeight();
    if(aWidth>0 && getPreserveRatio() && pw>aWidth) ph = aWidth*ph/pw;
    return ph;
}

/**
 * Report generation method.
 */
public RMShape rpgShape(ReportOwner aRptOwner, RMShape aParent)
{
    // If no key, do normal version
    if(getKey()==null || getKey().length()==0) return super.rpgShape(aRptOwner, aParent);
    
    // Get value for key (if string literal, get real string)
    Object value = RMKeyChain.getValue(aRptOwner, getKey());
    if(value instanceof RMKeyChain) value = ((RMKeyChain)value).getValue();
    
    // Get ImageData for value
    RMImageData idata = RMImageData.getImageData(value, 0);
    
    // Create clone, set new ImageData and return
    RMImageShape clone = (RMImageShape)clone();
    clone.setImageData(idata);
    clone.setPrefHeight(getHeight()); // Probably need a GrowShapeToFit attribute to allow RPG image shape to grow
    return clone;
}

/**
 * Override to paint shape.
 */
public void paintShape(Painter aPntr)
{
    super.paintShape(aPntr);
    RMImageData id = getImageData();
    if(id==null) { if(getFill()!=null || !RMShapePaintProps.isEditing(aPntr)) return; else id = RMImageData.EMPTY; }
    Rect ibounds = getImageBounds();
    aPntr.clip(getPath());
    id.paint(aPntr, ibounds.getX(), ibounds.getY(), ibounds.getWidth(), ibounds.getHeight());
}

/**
 * Returns the image bounds.
 */
public Rect getImageBounds()
{
    // Get image data and padding
    RMImageData id = getImageData(); if(id==null) id = RMImageData.EMPTY;
    int pd = getPadding();
    
    // Get width/height for shape, image and padded area
    double sw = getWidth(), sh = getHeight();
    double iw = id.getImageWidth(), ih = id.getImageHeight();
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
    
    // Archive ImageData
    else if(_imageData!=null) {
        String resName = anArchiver.addResource(_imageData.getBytes(), _imageData.getName());
        e.add("resource", resName);
    }
    
    // Archive PageIndex, Key, Padding, Alignment, GrowToFit, PreserveRatio
    if(_pageIndex>0) e.add("PageIndex", _pageIndex);
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
    
    // Unarchive PageIndex
    if(anElement.hasAttribute("PageIndex")) setPageIndex(anElement.getAttributeIntValue("PageIndex"));
    
    // Unarchive Image resource: get resource bytes, page and set ImageData
    String rname = anElement.getAttributeValue("resource");
    if(rname!=null) {
        byte bytes[] = anArchiver.getResource(rname); // Get resource bytes
        int page = anElement.getAttributeIntValue("PageIndex"); // Unarchive page number
        _imageData = RMImageData.getImageData(bytes, page); // Create new image data
    }
    
    // Unarchive ImageName
    _iname = anElement.getAttributeValue("ImageName");
    if(_iname!=null) {
        Image img = Image.get(anArchiver.getSourceURL(), _iname);
        if(img!=null)
            _imageData = RMImageData.getImageData(img.getBytes(), 0);
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
    
    // Legacy: If Fill is ImageFill and no ImageData+Key or ImageFill.ImageData, set ImageData from IFill and clear fill
    if(getFill() instanceof RMImageFill) { RMImageFill ifill = (RMImageFill)getFill();
        XMLElement fill = anElement.get("fill"); RMImageData idata = ifill.getImageData();
        if(getImageData()==null && !ifill.isTiled()) { // && getKey()==null) {
            int fs = fill.getAttributeIntValue("fillstyle", 0); // Stretch=0, Tile=1, Fit=2, FitIfNeeded=3
            if(fs==0) { setImageData(idata); setFill(null); setGrowToFit(true); setPreserveRatio(false); }
            else if(fs==2) { setImageData(idata); setFill(null); setGrowToFit(true); setPreserveRatio(true); }
            else if(fs==3) { setImageData(idata); setFill(null); setGrowToFit(false); setPreserveRatio(true); }
            double x = fill.getAttributeFloatValue("x"); if(x!=0) setAlignmentX(x<0? AlignX.Left : AlignX.Right);
            double y = fill.getAttributeFloatValue("y"); if(y!=0) setAlignmentY(y<0? AlignY.Top : AlignY.Bottom);
        }
        else if(idata==RMImageData.EMPTY) setFill(null);
        setPadding(fill.getAttributeIntValue("margin"));
    }
    
    // Return
    return this;
}

}