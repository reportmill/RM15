/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.PropChangeListener;
import snap.util.XMLElement;
import snap.web.WebURL;

/**
 * A shape to act as root of shape to be viewed.
 */
public class RMViewerShape extends RMParentShape {

    // The viewer
    RMViewer           _viewer;

    // The document being viewed
    RMDocument         _content;
    
    // A PropChangeListener to catch Content shape changes (Showing, PageSize, )
    PropChangeListener _viewerContentLsnr = pc -> _viewer.contentShapeDidPropChange(pc);
    
/**
 * Creates a new ViewerShape.
 */
public RMViewerShape(RMViewer aViewer)  { _viewer = aViewer; }

/**
 * Returns the viewer.
 */
public RMViewer getViewer()  { return _viewer; }

/**
 * Returns the root shape as RMDocument, if available.
 */
public RMDocument getDoc()  { return _content; }

/**
 * Returns the root shape as RMDocument, if available.
 */
public RMDocument getDocument()  { return _content; }

/**
 * Returns the root shape that is being viewed in viewer.
 */
public RMDocument getContent()  { return _content; }

/**
 * Sets the root shape that is being viewed in viewer.
 */
public void setContent(RMDocument aDoc)
{
    // Resolve page references on document and make sure it has a selected page
    aDoc.resolvePageReferences();
    aDoc.layout();
    
    // If old document, stop listening to shape changes and notify shapes hidden 
    if(_content!=null) _content.removePropChangeListener(_viewerContentLsnr);
    
    // Set new document and fire property change
    if(_content!=null) removeChild(_content);
    addChild(aDoc, 0);
    firePropChange("Content", _content, _content = aDoc);
    
    // Start listening to shape changes and notify shapes shown
    _content.addPropChangeListener(_viewerContentLsnr);
}

/**
 * Returns the page count.
 */
public int getPageCount()  { RMDocument d = getDoc(); return d!=null? d.getPageCount() : 1; }

/**
 * Returns the page at index.
 */
public RMPage getPage(int anIndex)  { RMDocument d = getDoc(); return d!=null? d.getPage(anIndex) : null; }

/**
 * Returns the currently selected page shape.
 */
public RMPage getSelectedPage()  { RMDocument d = getDoc(); return d!=null? d.getSelectedPage() : null; }

/**
 * Returns the index of the current visible document page.
 */
public int getSelectedPageIndex()  { RMDocument d = getDoc(); return d!=null? d.getSelectedIndex() : 0; }

/**
 * Sets the page of viewer's document that is visible (by index).
 */
public void setSelectedPageIndex(int anIndex)
{
    RMDocument doc = getDoc(); if(doc!=null) doc.setSelectedIndex(anIndex);
}

/**
 * Override to return content preferred width.
 */
protected double getPrefWidthImpl(double aHeight)  { RMDocument d = getDoc(); return d!=null? d.getPrefWidth() : 0; }

/**
 * Override to return content preferred height.
 */
protected double getPrefHeightImpl(double aWidth)  { RMDocument d = getDoc(); return d!=null? d.getPrefHeight() : 0; }

/**
 * Override to layout doc.
 */
protected void layoutImpl()
{
    // Get Doc, parent Width/Height and doc bounds in center of ViewerShape
    RMDocument doc = getDoc();
    double pw = getWidth(), ph = getHeight();
    double dw = doc.getPrefWidth(), dh = doc.getPrefHeight();
    double dx = pw>dw? Math.floor((pw-dw)/2) : 0, dy = ph>dh? Math.floor((ph-dh)/2) : 0;

    // Set doc location and scale for zoom factor
    doc.setBounds(dx,dy,dw,dh);
    if(doc.getScaleX()!=_viewer.getZoomFactor()) { double sc = _viewer.getZoomFactor();
        doc.setScaleXY(sc, sc); }
}

/**
 * This is a notification call for impending visual shape attribute changes.
 */
protected void repaint(RMShape aShape)
{
    // If painting, complain that someone is calling a repaint during painting
    if(_ptg) // Should never happen, but good to check
        System.err.println("RMViewerShape.repaint(): called during painting");
    
    // Send repaint event
    if(aShape==_content) _viewer.repaint();
    else _viewer.docShapeRepaint(aShape);
}

/**
 * Override to set Painting flag.
 */
public void paint(Painter aPntr)  { _ptg = true; super.paint(aPntr); _ptg = false; } boolean _ptg;

/**
 * Returns whether this shape is being viewed in a viewer.
 */
public boolean isViewing()  { return true; }

/**
 * Returns whether this shape is being edited in an editor.
 */
public boolean isEditing()  { return _viewer.isEditing(); }

/**
 * Returns the SourceURL.
 */
public WebURL getSourceURL()
{
    RMDocument cs = getContent(); return cs!=null && cs.isSourceURLSet()? cs.getSourceURL() : null;
}

/**
 * Sets the SourceURL.
 */
public void setSourceURL(WebURL aURL)  { if(getContent()!=null) getContent().setSourceURL(aURL); }

/**
 * Returns RXElement for content shape.
 */
public XMLElement getContentXML()
{
    getContent().layout();
    if(getDocument()!=null) getDocument().resolvePageReferences();
    return new RMArchiver().writeObject(getContent());
}

/**
 * Returns the content bytes.
 */
public byte[] getContentBytes()  { return getContentXML().getBytes(); }

}