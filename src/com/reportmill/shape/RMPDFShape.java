/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMKeyChain;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.util.*;

/**
 * This class is a shape representation of a PDF page.
 */
public class RMPDFShape extends RMRectShape {
    
    // The key used to get pdf data during RPG
    String             _key;
    
    // The pdf data
    RMPDFData          _pdfData;
    
    // The page index
    int                _pageIndex;
    
    // The padding
    int                _padding;
    
    // Whether to grow page to fit available area if shape larger than page.
    boolean            _growToFit = true;
    
    // Whether to preserve the natural width to height ratio of page
    boolean            _preserveRatio = true;
    
/**
 * Creates a RMPDFShape.
 */
public RMPDFShape()  { }

/**
 * Creates a RMPDFShape from the PDF file source provided.
 */
public RMPDFShape(Object aSource)  { setPDFData(aSource); setBestSize(); }

/**
 * Returns the report key used to load PDF data if not provided.
 */
public String getKey()  { return _key; }

/**
 * Sets the report key used to load PDF data if not provided.
 */
public void setKey(String aString)  { firePropChange("Key", _key, _key = aString); }

/**
 * Returns the PDF data.
 */
public RMPDFData getPDFData()  { return _pdfData; }

/**
 * Sets the PDF data.
 */
public void setPDFData(RMPDFData aPD)
{
    if(aPD==getPDFData()) return;
    _pdfData = aPD;
    setPageIndex(aPD.getPageIndex());
    if(getParent()!=null) getParent().relayout(); repaint();
}

/**
 * Sets the PDF data from given source.
 */
public void setPDFData(Object aSource)  { setPDFData(RMPDFData.getPDFData(aSource)); }

/**
 * Returns the page index.
 */
public int getPageIndex()  { return _pageIndex; }

/**
 * Sets the page index.
 */
public void setPageIndex(int anIndex)
{
    int index = anIndex; if(getPDFData()!=null) index = MathUtils.clamp(index, 0, getPDFData().getPageCount()-1);
    if(index==getPageIndex()) return;
    firePropChange("PageIndex", _pageIndex, _pageIndex = index);
    if(getPDFData()!=null) setPDFData(getPDFData().getPage(_pageIndex));
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
 * Returns whether to grow page to fit available area if shape larger than page.
 */
public boolean isGrowToFit()  { return _growToFit; }

/**
 * Sets whether to grow page to fit available area if shape larger than page.
 */
public void setGrowToFit(boolean aValue)
{
    firePropChange("GrowToFit", _growToFit, _growToFit = aValue);
    repaint();
}

/**
 * Returns whether to preserve the natural width to height ratio of page.
 */
public boolean getPreserveRatio()  { return _preserveRatio; }

/**
 * Sets whether to preserve the natural width to height ratio of page.
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
    RMPDFData id = getPDFData(); if(id==null) return 0;
    double pw = id.getWidth(), ph = id.getHeight();
    if(aHeight>0 && getPreserveRatio() && ph>aHeight) pw = aHeight*pw/ph;
    return pw;
}

/**
 * Returns the preferred height.
 */
protected double getPrefHeightImpl(double aWidth)
{
    RMPDFData id = getPDFData(); if(id==null) return 0;
    double pw = id.getWidth(), ph = id.getHeight();
    if(aWidth>0 && getPreserveRatio() && pw>aWidth) ph = aWidth*ph/pw;
    return ph;
}

/**
 * Report generation method.
 */
public RMShape rpgShape(ReportOwner aRptOwner, RMShape aParent)
{
    // Do normal version
    RMPDFShape clone = (RMPDFShape)super.rpgShape(aRptOwner, aParent);
    
    // If key: Evaluate key for PDF data and set
    String key = getKey();
    if(key!=null && key.length()>0) {
        Object value = RMKeyChain.getValue(aRptOwner, getKey());
        if(value instanceof RMKeyChain) value = ((RMKeyChain)value).getValue();
        clone.setPDFData(value);
    }

    // This prevents RMPDFShape from growing to actual PDF page size in report
    // Probably need a GrowShapeToFit attribute to allow RPG shape to grow
    clone.setPrefHeight(getHeight()*getScaleY());
    
    // Return clone
    return clone;
}

/**
 * Report generation method from RMImageShape that got PDF data.
 */
static RMShape rpgShape(ReportOwner aRptOwner, RMShape aParent, RMImageShape aShape, Object aSource)
{
    RMPDFShape pshape = new RMPDFShape(); pshape.copyShape(aShape); pshape.setGrowToFit(aShape.isGrowToFit());
    pshape.setPreserveRatio(aShape.getPreserveRatio()); pshape.setPadding(aShape.getPadding());
    pshape.setPDFData(aSource); pshape.setPrefHeight(pshape.getHeight()*pshape.getScaleY());
    return pshape;
}

/**
 * Override to paint shape.
 */
protected void paintShape(Painter aPntr)
{
    super.paintShape(aPntr);
    RMPDFData pd = getPDFData(); if(pd==null) { System.out.println("RMPDFShape.paint: Need empty impl"); return; }
    Rect ibounds = getImageBounds();
    aPntr.clip(getPath());
    pd.paint(aPntr, ibounds.x, ibounds.y, ibounds.width, ibounds.height);
}

/**
 * Returns the image bounds.
 */
public Rect getImageBounds()
{
    // Get PDF data and padding
    RMPDFData id = getPDFData();
    int pd = getPadding();
    
    // Get width/height for shape, page and padded area
    double sw = getWidth(), sh = getHeight();
    double iw = id.getWidth(), ih = id.getHeight();
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
    
    // Archive PDFData
    if(_pdfData!=null) {
        String resName = anArchiver.addResource(_pdfData.getBytes(), _pdfData.getName());
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
    
    // Unarchive PDF file resource: get resource bytes, page and set PDF data
    String rname = anElement.getAttributeValue("resource");
    if(rname!=null) {
        byte bytes[] = anArchiver.getResource(rname);
        _pdfData = RMPDFData.getPDFData(bytes);
        int page = anElement.getAttributeIntValue("PageIndex");
        if(page>0 && _pdfData!=null) _pdfData = _pdfData.getPage(page);
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
    
    // Return
    return this;
}

/**
 * Creates a new document from a PDF source.
 */
public static RMDocument getDocPDF(Object aSource, RMDocument aBaseDoc)
{
    // Get/create new document (with no pages)
    RMDocument doc = aBaseDoc!=null? aBaseDoc : new RMDocument();
    while(doc.getPageCount()>0) doc.removePage(0);
    
    // Get PDF data for source and iterate over each PDF page and create/add document page
    RMPDFData pdata = RMPDFData.getPDFData(aSource);
    for(int i=0, iMax=pdata.getPageCount(); i<iMax; i++) { RMPDFData pd = pdata.getPage(i);
        RMPage page = doc.addPage(); page.setSize(pd.getWidth(), pd.getHeight());
        page.addChild(new RMPDFShape(pd));
        if(i==0) doc.setSize(page.getWidth(), page.getHeight());
    }
    
    // Return doc
    return doc;
}

}