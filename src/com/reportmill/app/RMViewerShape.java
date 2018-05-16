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
    RMDocument         _doc;
    
    // A PropChangeListener to catch doc changes (Showing, PageSize, )
    PropChangeListener _viewerDocLsnr = pc -> _viewer.docDidPropChange(pc);
    
/**
 * Creates a ViewerShape for given viewer.
 */
public RMViewerShape(RMViewer aViewer)  { _viewer = aViewer; }

/**
 * Returns the viewer.
 */
public RMViewer getViewer()  { return _viewer; }

/**
 * Returns the document.
 */
public RMDocument getDoc()  { return _doc; }

/**
 * Sets the document to be viewed in viewer.
 */
public void setDoc(RMDocument aDoc)
{
    // Resolve page references on document and make sure it has a selected page
    aDoc.resolvePageReferences();
    aDoc.layoutDeep();
    
    // If old document, stop listening to shape changes and notify shapes hidden 
    if(_doc!=null) _doc.removePropChangeListener(_viewerDocLsnr);
    
    // Set new document
    if(_doc!=null) removeChild(_doc);
    addChild(_doc = aDoc, 0);
    
    // Start listening to shape changes and notify shapes shown
    _doc.addPropChangeListener(_viewerDocLsnr);
}

/**
 * Returns the root shape as RMDocument, if available.
 */
public RMDocument getDocument()  { return _doc; }

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
public RMPage getSelPage()  { RMDocument d = getDoc(); return d!=null? d.getSelectedPage() : null; }

/**
 * Returns the index of the current visible document page.
 */
public int getSelPageIndex()  { RMDocument d = getDoc(); return d!=null? d.getSelectedIndex() : 0; }

/**
 * Sets the page of viewer's document that is visible (by index).
 */
public void setSelPageIndex(int anIndex)  { RMDocument d = getDoc(); if(d!=null) d.setSelectedIndex(anIndex); }

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
    
    // Forward to viewer
    _viewer.repaintShape(aShape);
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
public WebURL getSourceURL() { RMDocument d = getDoc(); return d!=null && d.isSourceURLSet()? d.getSourceURL() : null; }

/**
 * Sets the SourceURL.
 */
public void setSourceURL(WebURL aURL)  { if(getDoc()!=null) getDoc().setSourceURL(aURL); }

/**
 * Override to notify viewer.
 */
protected void setNeedsLayoutDeep(boolean aVal)
{
    super.setNeedsLayoutDeep(aVal); if(aVal) _viewer.relayout();
}

/**
 * Returns XMLElement for document.
 */
public XMLElement getDocXML()
{
    getDoc().layoutDeep();
    if(getDocument()!=null) getDocument().resolvePageReferences();
    return new RMArchiver().writeObject(getDoc());
}

/**
 * Returns the xml bytes for document.
 */
public byte[] getDocBytes()  { return getDocXML().getBytes(); }

}