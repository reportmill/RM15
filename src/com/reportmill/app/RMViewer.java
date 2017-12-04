/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import java.awt.print.*;
import java.util.*;
import javax.print.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import snap.swing.J2DPainter;
import snap.web.WebURL;

/**
 * The RMViewer class is a JComponent subclass that can be used in Java client applications to display and/or print an
 * RMDocument.
 *
 * You might use it like this to simply print a document:
 * <p><blockquote><pre>
 *   new RMViewer(aDocument).print();
 * </pre></blockquote><p>
 * Or you might want to allocate one and add it to a Swing component hierarchy:
 * <p><blockquote><pre>
 *   RMViewer viewer = new RMViewer(); viewer.setContent(new RMDocument(aSource));
 *   myFrame.getContentPane().add(new JScrollPane(viewer));
 * </pre></blockquote>
 */
public class RMViewer extends View {

    // The shape viewer uses to manage real root of shapes
    RMViewerShape            _vshape = createViewerShape();
    
    // The Zoom mode
    ZoomMode                 _zoomMode = ZoomMode.ZoomAsNeeded;
    
    // Zoom factor
    double                   _zoomFactor = 1;
    
    // The previous zoom factor (for toggle zoom)
    double                   _lastZoomFactor = 1;

    // The helper class that handles events for viewer
    RMViewerEvents           _events = createEvents();

    // The current set of shapes that need to be redrawn after the current event
    List <RMShape>           _dirtyShapes = new Vector(32);
    
    // The area of the viewer marked for redraw after the current event
    Rect                     _dirtyRect;
    
    // Zoom modes
    public enum ZoomMode { ZoomToFit, ZoomAsNeeded, ZoomToFactor };
    
    // Constants for PropertyChanges
    public static final String Content_Prop = "Content";
        
/**
 * Creates a new RMViewer with an empty document in it.
 */
public RMViewer()
{
    enableEvents(MouseEvents); enableEvents(KeyEvents);
    setFocusable(true); setFocusWhenPressed(true);
    setFill(Color.LIGHTGRAY);
}

/**
 * Returns the viewer shape.
 */
public RMViewerShape getViewerShape()  { return _vshape; }

/**
 * Creates the viewer shape.
 */
protected RMViewerShape createViewerShape()  { return new RMViewerShape(this); }

/**
 * Returns the root shape that is the content of this viewer.
 */
public RMParentShape getContent()  { return _vshape.getContent(); }

/**
 * Sets the root shape that is the content of this viewer.
 */
public void setContent(RMParentShape aShape)
{
    // If already set, just return
    if(aShape==getContent()) return;
    
    // Set new document and fire property change
    RMShape shape = getContent(); _vshape.setContent(aShape);
    firePropChange(Content_Prop, shape, aShape);
    
    // Set ZoomToFitFactor and relayout/repaint (for possible size change)
    setZoomToFitFactor();
    relayout(); repaint();
}

/**
 * Sets the content from any source.
 */
public void setContent(Object aSource)  { setContent(new RMArchiver().getParentShape(aSource)); }

/**
 * Returns the RMDocument associated with this viewer.
 */
public RMDocument getDocument()  { return _vshape.getContent(RMDocument.class); }

/**
 * Returns the source URL.
 */
public WebURL getSourceURL()  { return getViewerShape().getSourceURL(); }

/**
 * Returns whether viewer is really doing editing.
 */
public boolean isEditing()  { return false; }

/**
 * Returns whether editor is preview (or viewer) mode.
 */
public boolean isPreview()  { return !isEditing(); }

/**
 * Returns the page count.
 */
public int getPageCount()  { return _vshape.getPageCount(); }

/**
 * Returns the currently selected page shape.
 */
public RMParentShape getSelectedPage()  { return _vshape.getSelectedPage(); }

/**
 * Returns the index of the current visible document page.
 */
public int getSelectedPageIndex()  { return _vshape.getSelectedPageIndex(); }

/**
 * Sets the page of viewer's document that is visible (by index).
 */
public void setSelectedPageIndex(int anIndex)  { _vshape.setSelectedPageIndex(anIndex); }

/**
 * Selects the next page.
 */
public void pageForward()  { setSelectedPageIndex(getSelectedPageIndex()+1); }

/**
 * Selects the previous page.
 */
public void pageBack()  { setSelectedPageIndex(getSelectedPageIndex()-1); }

/**
 * Returns the bounds of the viewer document.
 */
public Rect getDocBounds()  { return convertFromShape(getContent().getBoundsInside(), null).getBounds(); }

/**
 * Returns the bounds of the viewer document's selected page.
 */
public Rect getPageBounds()
{
    RMShape page = getSelectedPage();
    return convertFromShape(page.getBoundsInside(), page).getBounds();
}

/**
 * Returns the first shape hit by the given point.
 */
public RMShape getShapeAtPoint(double aX, double aY, boolean goDeep) { return getShapeAtPoint(new Point(aX,aY),goDeep);}

/**
 * Returns the first shape hit by the given point.
 */
public RMShape getShapeAtPoint(Point aPoint, boolean goDeep)
{
    // Convert point from viewer to selected page
    RMParentShape parent = getSelectedPage();
    Point point = convertToShape(aPoint.x, aPoint.y, parent);
    
    // Iterate over children to find shape hit by point
    RMShape shape = null; Point point2 = null;
    for(int i=parent.getChildCount(); i>0 && shape==null; i--) { RMShape child = parent.getChild(i-1);
        point2 = child.convertedPointFromShape(point, parent);
        if(child.contains(point2))
            shape = child;
    }
    
    // If we need to goDeep (and there was a top level hit shape), recurse until shape is found
    while(goDeep && shape instanceof RMParentShape) { parent = (RMParentShape)shape;
        RMShape shp = parent.getChildContaining(point2);
        if(shp!=null) { shape = shp; shape.convertPointFromShape(point2, parent); }
        else break;
    }
    
    // Return hit shape
    return shape;
}

/**
 * Returns the viewer's zoom factor (1 by default).
 */
public double getZoomFactor()  { return _zoomFactor; }

/**
 * Sets the viewer's zoom factor (1 for 100%).
 */
public void setZoomFactor(double aFactor)
{
    setZoomMode(ZoomMode.ZoomToFactor);
    setZoomFactorImpl(aFactor);
}

/**
 * Sets the viewer's zoom factor (1 for 100%) and mode.
 */
protected void setZoomFactorImpl(double aFactor)
{    
    // Constrain zoom factor to valid range (ZoomToFactor: 20%...10000%, ZoomAsNeed: Max of 1)
    ZoomMode zmode = getZoomMode();
    if(zmode==ZoomMode.ZoomToFactor) aFactor = Math.min(Math.max(.2f, aFactor), 100);
    else if(zmode==ZoomMode.ZoomAsNeeded) aFactor = Math.min(aFactor, 1);

    // If already at given factor, just return
    if(aFactor==_zoomFactor) return;

    // Set last zoom factor and new zoom factor and fire property change
    firePropChange("ZoomFactor", _lastZoomFactor = _zoomFactor, _zoomFactor = aFactor);
    
    // If ZoomToFactor and parent is viewport, resize and scroll to center of previous zoom
    if(isZoomToFactor()) {
        Rect vr = getZoomFocusRect(), vr2 = vr.clone();
        setSize(getPrefWidth(), getPrefHeight());
        vr2.scale(_zoomFactor/_lastZoomFactor);
        vr2.inset((vr2.getWidth() - vr.getWidth())/2, (vr2.getHeight() - vr.getHeight())/2);
        setVisRect(vr2);
    }
    
    // Relayout and repaint
    relayout(); relayoutParent(); repaint();
}

/**
 * Returns the ZoomMode (ZoomToFit, ZoomIfNeeded, ZoomToFactor).
 */
public ZoomMode getZoomMode()  { return _zoomMode; }

/**
 * Sets the ZoomMode.
 */
public void setZoomMode(ZoomMode aZoomMode)
{
    if(aZoomMode==getZoomMode()) return;
    firePropChange("ZoomMode", _zoomMode, _zoomMode = aZoomMode);
    setZoomToFitFactor(); // Reset ZoomFactor
}

/**
 * Returns whether viewer is set to ZoomToFactor.
 */
public boolean isZoomToFactor()  { return getZoomMode()==ZoomMode.ZoomToFactor; }

/**
 * Returns the zoom factor for the given mode at the current viewer size.
 */
public double getZoomFactor(ZoomMode aMode)
{
    // If ZoomToFactor, just return ZoomFactor
    if(aMode==ZoomMode.ZoomToFactor) return getZoomFactor();
    
    // Get ideal size and current size (if size is zero, return 1)
    double pw = getPrefWidthBase(), ph = getPrefHeightBase();
    double width = getWidth(), height = getHeight(); if(width==0 || height==0) return 1;
    
    // If ZoomAsNeeded and IdealSize is less than size, return
    if(aMode==ZoomMode.ZoomAsNeeded && pw<=width && ph<=height) return 1;
    if(aMode==ZoomMode.ZoomToFit && pw==width && ph==height) return 1;
    
    // Otherwise get ratio of parent size to ideal size (with some gutter added in) and return smaller axis
    double zw = width/(pw + 8f), zh = height/(ph + 8f);
    return Math.min(zw, zh);
}

/**
 * Sets the zoom to fit factor, based on the current zoom mode.
 */
public void setZoomToFitFactor()  { setZoomFactorImpl(getZoomFactor(getZoomMode())); }

/**
 * Returns zoom focus rect (just the visible rect by default, but overriden by editor to return selected shapes rect).
 */
public Rect getZoomFocusRect()  { return getVisRect(); }

/**
 * Returns the zoom factor to view the document at actual size taking into account the current screen resolution.
 */
public double getZoomToActualSizeFactor()  { return GFXEnv.getEnv().getScreenResolution()/72; }

/**
 * Sets the viewer's zoom to its previous value.
 */
public void zoomToggleLast()  { setZoomFactor(_lastZoomFactor); }

/**
 * Overrides to update ZoomFactor if dynamic.
 */
public void setWidth(double aValue)  { super.setWidth(aValue); setZoomToFitFactor(); }

/**
 * Overrides to update ZoomFactor if dynamic.
 */
public void setHeight(double aValue)  { super.setHeight(aValue); setZoomToFitFactor(); }

/**
 * Returns the content shape's X location in viewer.
 */
public int getContentX()
{
    float align = .5f; //RMShapeUtils.getAutosizeAlignmentX(getContent());
    return (int)Math.round(Math.max((getWidth()-getContent().getWidth()*getZoomFactor())*align, 0));
}

/**
 * Returns the content shape's Y location in viewer.
 */
public int getContentY()
{
    float align = .5f; //RMShapeUtils.getAutosizeAlignmentY(getContent());
    return (int)Math.round(Math.max((getHeight()-getContent().getHeight()*getZoomFactor())*align, 0));
}

/**
 * Returns a point converted from the coordinate space of the given shape to viewer coords.
 */
public Point convertFromShape(double aX, double aY, RMShape aShape)
{
    // If given shape, transform point from shape to doc
    Point point = new Point(aX,aY); if(aShape!=null) point = aShape.convertedPointToShape(point, null);
    
    // Multiply point for zoom, and transdate for doc centering
    double x = point.getX()*getZoomFactor() + getContentX();
    double y = point.getY()*getZoomFactor() + getContentY();
    return new Point(x,y);
}

/**
 * Returns a point converted from viewer coords to the coordinate space of the given shape.
 */
public Point convertToShape(double aX, double aY, RMShape aShape)
{
    // Correct for zoom
    double x = aX - getContentX(), y = aY - getContentY();
    
    // Get point unzoomed, convert to shape and return
    Point point = new Point(x/getZoomFactor(), y/getZoomFactor());
    if(aShape!=null) aShape.convertPointFromShape(point, null);
    return point;
}

/**
 * Returns a rect converted from the coordinate space of the given shape to viewer coords.
 */
public Shape convertFromShape(Shape aShp, RMShape aShape)
{
    Transform tfm = getTransformFromShape(aShape);
    return aShp.copyFor(tfm);
}

/**
 * Returns a shape converted from viewer coords to the coordinate space of the given RMShape.
 */
public Shape convertToShape(Shape aShp, RMShape aShape)
{
    Transform tfm = getTransformToShape(aShape);
    return aShp.copyFor(tfm);
}

/**
 * Returns the transform from given shape to viewer.
 */
public Transform getTransformFromShape(RMShape aShape)
{
    // Get transform from shape to doc, scale for zoom factor, translate for content x/y
    Transform trans = aShape!=null? aShape.getTransformToShape(null) : new Transform();
    if(getZoomFactor()!=1) trans.scale(getZoomFactor(), getZoomFactor());
    trans.translate(getContentX(), getContentY());
    return trans;
}

/**
 * Returns the transform from viewer to given shape.
 */
public Transform getTransformToShape(RMShape aShape)
{
    Transform tfm = getTransformFromShape(aShape); tfm.invert(); return tfm;
}

/**
 * Creates the object that is actually responsible for paining shapes in the viewer.
 */
protected RMShapePaintProps createShapePaintProps()  { return null; }

/**
 * Override to paint viewer shapes and page, margin, grid, etc.
 */
public void paintFront(Painter aPntr)
{
    Rect bnds = new Rect(0, 0, getWidth(), getHeight()); double scale = getZoomFactor();
    RMShapePaintProps props = createShapePaintProps(); if(props!=null) aPntr.setProps(props);
    RMShapeUtils.paintShape(aPntr, _vshape, bnds, scale);
    if(props!=null) aPntr.setProps(null); //RMShapePainter spntr = getShapePainter(aPntr); spntr.paintShape(_vshape);
    getEvents().paint(aPntr); // Have event helper paint above
}

/**
 * Returns the event helper for the viewer (handles mouse and keyboard input).
 */
public RMViewerEvents getEvents()  { return _events; }

/**
 * Creates a default event helper.
 */
protected RMViewerEvents createEvents()  { return new RMViewerEvents(this); }

/**
 * Handle mouse events.
 */
protected void processEvent(ViewEvent anEvent)
{
    super.processEvent(anEvent); // Do normal version
    getEvents().processEvent(anEvent); // Forward to event helper
}

/**
 * Returns the preferred size of the viewer (includes ZoomFactor).
 */
protected double getPrefWidthImpl(double aH)
{
    double w = getPrefWidthBase(); if(isZoomToFactor()) w *= getZoomFactor(); return w;
}

/**
 * Returns the preferred size of the viewer (ignores Zoom).
 */
protected double getPrefWidthBase()
{
    // Get doc width, add gutter if not PageLayout.Single and return
    double w = _vshape.getPrefWidth();
    if(getDocument()==null || getDocument().getPageLayout()!=RMDocument.PageLayout.Single) w += 8;
    return w;
}

/**
 * Returns the preferred size of the viewer (includes ZoomFactor).
 */
protected double getPrefHeightImpl(double aW)
{
    double h = getPrefHeightBase(); if(isZoomToFactor()) h *= getZoomFactor(); return h;
}

/**
 * Returns the preferred size of the viewer (ignores Zoom).
 */
protected double getPrefHeightBase()
{
    // Get doc height, add gutter if not PageLayout.Single and return
    double h = _vshape.getPrefHeight();
    if(getDocument()==null || getDocument().getPageLayout()!=RMDocument.PageLayout.Single) h += 8;
    return h;
}

/**
 * Returns the undoer associated with the viewer's document.
 */
public Undoer getUndoer()  { return _vshape.getUndoer(); }

/**
 * Sets the title of the next registered undo in the viewer's documents's undoer (convenience).
 */
public void undoerSetUndoTitle(String aTitle)
{
    if(getUndoer()!=null)
        getUndoer().setUndoTitle(aTitle);
}

/**
 * Returns whether undos exist in the viewer's documents's undoer (convenience).
 */
public boolean undoerHasUndos()  { return getUndoer()!=null && getUndoer().hasUndos(); }

/**
 * Returns whether changes to shapes cause repaints.
 */
public boolean getShapeRepaintEnabled()  { return _dirtyShapes!=null; }

/**
 * Sets whether changes to shapes cause repaints.
 */
public void setShapeRepaintEnabled(boolean aFlag)  { _dirtyShapes = aFlag? new Vector() : null; }

/**
 * Doc listener method - called before a shape makes a visual change.
 * Provides a mechanism to efficiently repaint the portion of the viewer that currently displays a shape. Registers
 * the area covered by the shape now and at event end, to efficiently repaint shapes in transition as well.
 */
public void docShapeRepaint(RMShape aShape)
{
    // If given shape hasn't been registered yet, post repaint and squirrel shape away for flushGraphics call
    if(isShowing() && _dirtyShapes!=null && !ListUtils.containsId(_dirtyShapes, aShape)) {
        
        // Add shape to dirty shapes set
        _dirtyShapes.add(aShape);
        
        // Get shape dirty rect
        Rect dirtyRect = getRepaintBoundsForShape(aShape);
        
        // If this is the first dirty shape, register for flushGraphics call
        if(_dirtyRect==null) {
            _dirtyRect = dirtyRect; // Init dirty rect
            getEnv().runLater(() -> flushShapeRepaints());
        }
        
        // Otherwise, add shape bounds to dirty rect
        else _dirtyRect.union(dirtyRect);
    }
    
    // Iterate over shape siblings to notify them of peer change
    RMParentShape parent = aShape.getParent();
    for(int i=0, iMax=parent!=null? parent.getChildCount() : 0; i<iMax; i++) { RMShape child = parent.getChild(i);
        if(child instanceof RMTextShape && child !=aShape)
            ((RMTextShape)child).peerDidChange(aShape);
    }
}

/**
 * This method repaints the total bounds of shapes that have previously been registered by shapeNeedsRepaint. This 
 * should only be used internally.
 */
protected void flushShapeRepaints()
{
    getContent().layout();
    // If no dirty shapes, just return
    if(_dirtyShapes==null || _dirtyShapes.size()==0) return;
    
    // Get local dirty shapes and clear ivar so nothing will re-register while we're building
    List <RMShape> dirtyShapes = _dirtyShapes; _dirtyShapes = null;
    
    // Declare variable for dirty rect
    Rect dirtyRect = _dirtyRect;
    
    // Iterate over dirty shapes and get total marked bounds in viewer coords
    for(RMShape shape : dirtyShapes) {
        Rect bounds = getRepaintBoundsForShape(shape); // Get shape marked bounds in viewer coords
        if(dirtyRect==null) dirtyRect = bounds;  // Either set or union dirty bounds
        else dirtyRect.union(bounds);
    }

    // Repaint dirty rect
    repaint(dirtyRect);
    
    // Reset dirty shapes and rect
    _dirtyShapes = dirtyShapes; _dirtyShapes.clear(); _dirtyRect = null;
}

/**
 * Returns the bounds for a given shape in the viewer.
 * Subclasses can override this to account for things like different bounds for selected shapes.
 */
public Rect getRepaintBoundsForShape(RMShape aShape)
{
    Rect bounds = aShape.getBoundsMarkedDeep();  // Get shape marked bounds
    bounds = convertFromShape(bounds, aShape).getBounds();  // Convert to viewer coords
    bounds.inset(-4, -4); // Outset for handles
    return bounds;
}

/**
 * Called when content shape has PropChange.
 */
protected void contentShapeDidPropChange(PropChange anEvent)
{
    // Handle SelectedPageIndex, PageSize, PageLayout
    String pname = anEvent.getPropertyName();
    if(pname.equals("SelectedPage") || pname.equals("PageSize") || pname.equals("PageLayout")) {
        relayout(); setZoomToFitFactor(); repaint();
        firePropChange("ContentChange" + pname, anEvent.getOldValue(), anEvent.getNewValue());
    }
}

/**
 * Returns the document shape for given name.
 */
public RMShape getShape(String aName)  { return _vshape.getChildWithName(aName); }

/**
 * Creates a shape mouse event.
 */
public ViewEvent createShapeEvent(RMShape aShape, ViewEvent anEvent, ViewEvent.Type aType)
{
    Point point = convertToShape(anEvent.getX(), anEvent.getY(), aShape);
    return new RMShapeEvent(aShape, anEvent, point, aType); // was ne
}

/**
 * This method tells the RMViewer to print by running the print dialog (configured to the default printer).
 */
public void print()  { print(null, true); }

/**
 * This method tells the RMViewer to print to the printer with the given printer name (use null for default printer). It
 * also offers an option to run the printer dialog.
 */
public void print(String aPrinterName, boolean runPanel)
{
    // Get first page, book and printer job for book
    RMShape page = _vshape.getPage(0);
    Book book = getBook();
    PrinterJob job = PrinterJob.getPrinterJob(); job.setPageable(book);
    
    // If a printerName was provided, try to find service with that name
    if(aPrinterName!=null) {
        PrintService services[] = PrinterJob.lookupPrintServices(), service = null;
        for(int i=0; i<services.length; i++)
            if(aPrinterName.equals(services[i].getName()))
                service = services[i];
        if(service!=null)
            try { job.setPrintService(service); }
            catch(Exception e) { e.printStackTrace(); service = null; }
        if(service==null) {
            System.err.println("RMViewer:Print: Couldn't find printer named " + aPrinterName);
            System.err.println("Available Services:");
            for(int i=0; i<services.length; i++)
                System.err.println("\t- " + services[i].getName());
            return;
        }
    }
    
    // Flip the orientation if printer has funny definition of portrait/landscape
    int orient = page.getHeight()>=page.getWidth()? PageFormat.PORTRAIT : PageFormat.LANDSCAPE;
    PageFormat pf = job.defaultPage();
    if(pf.getOrientation()==PageFormat.PORTRAIT && pf.getWidth()>pf.getHeight() ||
        pf.getOrientation()==PageFormat.LANDSCAPE && pf.getHeight()>pf.getWidth()) {
        orient = orient==PageFormat.PORTRAIT? PageFormat.LANDSCAPE : PageFormat.PORTRAIT;
        book.getPageFormat(0).setOrientation(orient);
    }
    
    // Run printDialog, and if successful, execute print
    boolean shouldPrint = !runPanel || job.printDialog();
    try { if(shouldPrint) job.print(); }
    catch(Exception e) { e.printStackTrace(); }
}

/**
 * Returns a java.awt.print.Book, suitable for AWT printing.
 */
public Book getBook()
{
    // Get document, generic viewer printable and book
    Printable printable = new RMVPrintable();
    Book book = new Book();
    
    // Iterate over pages and add to book
    for(int i=0, iMax=_vshape.getPageCount(); i<iMax; i++) { RMShape page = _vshape.getPage(i);
    
	    // Get doc width, height and orientation
	    double width = page.getWidth(), height = page.getHeight();
	    int orientation = PageFormat.PORTRAIT;
	    if(width>height) {
	        orientation = PageFormat.LANDSCAPE; width = height; height = page.getWidth(); }
	    
	    // Get paper and configure with appropriate paper size and imageable area
	    Paper paper = new Paper();
	    paper.setSize(width, height);
	    paper.setImageableArea(0, 0, width, height);
	
	    // Get pageFormat and configure with appropriate orientation and paper
	    PageFormat pageFormat = new PageFormat();
	    pageFormat.setOrientation(orientation);
	    pageFormat.setPaper(paper);
	    
	    // Appends page to book
	    book.append(printable, pageFormat);
    }

    // Return book
    return book;
}

/**
 * This inner class simply paints a reqested page to a given Graphics object.
 */
protected class RMVPrintable implements Printable {

    /** Print method. */
    public int print(java.awt.Graphics aGr, PageFormat pageFormat, int pageIndex)
    {
        // If bogus range, bail
        if(pageIndex>=_vshape.getPageCount()) return Printable.NO_SUCH_PAGE;
        
        // Get page at index, get/configure shape painter, paint shape, return success
        RMShape page = _vshape.getPage(pageIndex);
        Painter pntr = new J2DPainter(aGr); pntr.setPrinting(true);
        RMShapeUtils.paintShape(pntr, page, null, 1);
        return Printable.PAGE_EXISTS; // Return success
    }
}

}