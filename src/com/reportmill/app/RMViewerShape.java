/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.RMAnimator;
import com.reportmill.graphics.RMColor;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.XMLElement;
import snap.view.ViewUtils;
import snap.web.WebURL;

/**
 * A shape to act as root of shape to be viewed.
 */
public class RMViewerShape extends RMParentShape {

    // The viewer
    RMViewer           _viewer;

    // The shape viewer uses to manage real root of shapes
    RMParentShape      _content;
    
/**
 * Creates a new ViewerShape.
 */
public RMViewerShape(RMViewer aViewer)  { _viewer = aViewer; }

/**
 * Returns the viewer.
 */
public RMViewer getViewer()  { return _viewer; }

/**
 * Returns the root shape that is being viewed in viewer.
 */
public RMParentShape getContent()  { return _content; }

/**
 * Sets the root shape that is being viewed in viewer.
 */
public void setContent(RMParentShape aShape)
{
    // Resolve page references on document and make sure it has a selected page
    if(aShape instanceof RMDocument) { RMDocument doc = (RMDocument)aShape; doc.resolvePageReferences(); }
    aShape.layout();
    
    // If old document, stop listening to shape changes and notify shapes hidden 
    if(_content!=null)
        _content.removePropChangeListener(_viewer); //_content.setShowing(false);
    
    // Set new document and fire property change
    if(_content!=null) removeChild(_content);
    addChild(aShape, 0);
    setSize(aShape.getWidth(), aShape.getHeight());
    firePropChange("Content", _content, _content = aShape);
    
    // Start listening to shape changes and notify shapes shown
    _content.addPropChangeListener(_viewer); //_content.setShowing(isShowing());
    
    // If not RMDocument, add some rendering to content
    if(!(_content instanceof RMDocument)) {
        _content.setColor(RMColor.get(ViewUtils.getBackFill()));
        _content.setEffect(new ShadowEffect());
    }
}

/**
 * Returns the root shape that is being viewed in viewer as a specific class.
 */
public <T extends RMParentShape> T getContent(Class<T> aClass)
{
    return aClass.isInstance(_content)? (T)_content : null;
}

/**
 * Returns the root shape as RMDocument, if available.
 */
public RMDocument getDocument()  { return getContent(RMDocument.class); }

/**
 * Returns the page count.
 */
public int getPageCount()  { RMDocument d = getDocument(); return d!=null? d.getPageCount() : 1; }

/**
 * Returns the page at index.
 */
public RMParentShape getPage(int anIndex)
{
    RMDocument d = getDocument(); return d!=null? d.getPage(anIndex) : _content;
}

/**
 * Returns the currently selected page shape.
 */
public RMParentShape getSelectedPage()
{
    RMDocument d = getDocument(); return d!=null? d.getSelectedPage() : _content;
}

/**
 * Returns the index of the current visible document page.
 */
public int getSelectedPageIndex()  { RMDocument d = getDocument(); return d!=null? d.getSelectedIndex() : 0; }

/**
 * Sets the page of viewer's document that is visible (by index).
 */
public void setSelectedPageIndex(int anIndex)
{
    RMDocument doc = getDocument(); if(doc!=null) doc.setSelectedIndex(anIndex);
}

/**
 * Return child animator.
 */
public RMAnimator getChildAnimator()
{
    return _content instanceof RMDocument? ((RMDocument)_content).getSelectedPage().getChildAnimator() : null;
}

/**
 * This is bogus, but we want to make sure that ViewerShape is always the same size as the content.
 */
protected void layoutChildren()
{
    setSize(getContent().getWidth(), getContent().getHeight());
}

/**
 * Override to return content preferred width.
 */
protected double computePrefWidth(double aHeight)  { RMShape c = getContent(); return c!=null? c.getPrefWidth() : 0; }

/**
 * Override to return content preferred height.
 */
protected double computePrefHeight(double aWidth)  { RMShape c = getContent(); return c!=null? c.getPrefHeight() : 0; }

/**
 * This is a notification call for impending visual shape attribute changes.
 */
protected void repaint(RMShape aShape)
{
    // If painting, complain that someone is calling a repaint during painting
    if(_ptg) // Should never happen, but good to check
        System.err.println("RMDocument.repaint(): called during painting");
    
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
    RMParentShape cs = getContent(); return cs!=null && cs.isSourceURLSet()? cs.getSourceURL() : null;
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