/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.shape.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.data.Entity;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * This is the base class for tools in RM - the objects that provide GUI editing for RM shapes.
 */
public class RMTool <T extends RMShape> extends ViewOwner {
    
    // The Editor that owns this tool
    RMEditor                _editor;
    
    // The Editor pane
    RMEditorPane            _editorPane;

    // The newly created shape instance
    RMShape                 _shape;
    
    // The mouse down point that initiated last tool mouse loop
    Point                   _downPoint;
    
    // The image for a shape handle
    static Image            _handle = Image.get(RMEditor.class, "Handle8x8.png");
    
    // Handle constants
    public static final byte HandleWidth = 8;
    public static final byte HandleNW = 0;
    public static final byte HandleNE = 1;
    public static final byte HandleSW = 2;
    public static final byte HandleSE = 3;
    public static final byte HandleW = 4;
    public static final byte HandleE = 5;
    public static final byte HandleN = 6;
    public static final byte HandleS = 7;

/**
 * Returns the shape class that this tool handles.
 */
public Class <T> getShapeClass()  { return (Class<T>)RMShape.class; }

/**
 * Returns a new instance of the shape class that this tool is responsible for.
 */
protected T newInstance()  { return ClassUtils.newInstance(getShapeClass()); }

/**
 * Returns the string to be used for the inspector window title.
 */
public String getWindowTitle()  { return "Shape Inspector"; }

/**
 * Create Node.
 */
protected View createUI()  { return getClass()==RMTool.class? new Label() : super.createUI(); }

/**
 * Returns the given shape's dataset entity.
 * Provides a level of indirection show shapes can show a different entity in the designer.
 */
public Entity getDatasetEntity(RMShape aShape)  { return aShape.getDatasetEntity(); }

/**
 * Returns the currently active editor.
 */
public RMEditor getEditor()  { return _editor; }

/**
 * Sets the currently active editor.
 */
public void setEditor(RMEditor anEditor)  { _editor = anEditor; }

/**
 * Returns the currently active editor pane.
 */
public RMEditorPane getEditorPane()
{
    if(_editorPane!=null) return _editorPane;
    return _editorPane = _editor.getEditorPane();
}

/**
 * Returns the editor event handler.
 */
public RMEditorEvents getEditorEvents()  { return getEditor().getEvents(); }
    
/**
 * Returns the current selected shape for the current editor.
 */
public T getSelectedShape()
{
    RMEditor e = getEditor(); if(e==null) return null;
    RMShape s = e.getSelectedOrSuperSelectedShape();
    return ClassUtils.getInstance(s, getShapeClass());
}

/**
 * Returns the current selected shapes for the current editor.
 */
public List <? extends RMShape> getSelectedShapes()  { return getEditor().getSelectedOrSuperSelectedShapes(); }

/**
 * Called when a tool is selected.
 */
public void activateTool()  { }

/**
 * Called when a tool is deselected (when another tool is selected).
 */
public void deactivateTool()  { }

/**
 * Called when a tool is selected even when it's already the current tool.
 */
public void reactivateTool()  { }

/**
 * Called when a tool is deselected to give an opportunity to finalize changes in progress.
 */
public void flushChanges(RMEditor anEditor, RMShape aShape)  { }

/**
 * Returns whether a given shape is selected in the editor.
 */
public boolean isSelected(RMShape aShape)  { return getEditor().isSelected(aShape); }

/**
 * Returns whether a given shape is superselected in the editor.
 */
public boolean isSuperSelected(RMShape aShape)  { return getEditor().isSuperSelected(aShape); }

/**
 * Returns whether a given shape is super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return aShape.superSelectable(); }

/**
 * Returns whether a given shape accepts children.
 */
public boolean getAcceptsChildren(RMShape aShape)  { return aShape.acceptsChildren(); }

/**
 * Returns whether a given shape can be ungrouped.
 */
public boolean isUngroupable(RMShape aShape)  { return aShape.getChildCount()>0; }

/**
 * Editor method - called when an instance of this tool's shape is super selected.
 */
public void didBecomeSuperSelected(T aShape)  { }

/**
 * Editor method - called when an instance of this tool's shape in de-super-selected.
 */
public void willLoseSuperSelected(T aShape)  { }

/**
 * Returns the bounds of the shape in parent coords when super selected (same as getBoundsMarkedDeep by default).
 */
public Rect getBoundsSuperSelected(T aShape)  { return aShape.getBoundsMarkedDeep(); }

/**
 * Converts from shape units to tool units.
 */
public double getUnitsFromPoints(double aValue)
{
    RMEditor editor = getEditor(); RMDocument doc = editor.getDocument();
    return doc!=null? doc.getUnitsFromPoints(aValue) : aValue;
}

/**
 * Converts from tool units to shape units.
 */
public double getPointsFromUnits(double aValue)
{
    RMEditor editor = getEditor(); RMDocument doc = editor.getDocument();
    return doc!=null? doc.getPointsFromUnits(aValue) : aValue;
}

/**
 * Returns the font for the given shape.
 */
public RMFont getFont(RMEditor anEditor, RMShape aShape)  { return aShape.getFont(); }

/**
 * Sets the font for the given shape.
 */
public void setFont(RMEditor anEditor, RMShape aShape, RMFont aFont)  { aShape.setFont(aFont); }

/**
 * Returns the font for the given shape.
 */
public RMFont getFontDeep(RMEditor anEditor, RMShape aShape)
{
    RMFont font = getFont(anEditor, aShape);
    for(int i=0, iMax=aShape.getChildCount(); i<iMax && font==null; i++) font = aShape.getChild(i).getFont();
    for(int i=0, iMax=aShape.getChildCount(); i<iMax && font==null; i++) {
        RMShape child = aShape.getChild(i); RMTool tool = anEditor.getTool(child);
        font = tool.getFontDeep(anEditor, child);
    }
    return font;
}

/**
 * Sets the font family for given shape.
 */
public void setFontFamily(RMEditor anEditor, RMShape aShape, RMFont aFont)
{
    // Get new font for given font family font and current shape font size/style and set
    RMFont font = getFont(anEditor, aShape), font2 = aFont;
    if(font!=null) {
        if(font.isBold()!=font2.isBold() && font2.getBold()!=null) font2 = font2.getBold();
        if(font.isItalic()!=font2.isItalic() && font2.getItalic()!=null) font2 = font2.getItalic();
        font2 = font2.deriveFont(font.getSize());
    }
    setFont(anEditor, aShape, font2);
}

/**
 * Sets the font family for given shape.
 */
public void setFontFamilyDeep(RMEditor anEditor, RMShape aShape, RMFont aFont)
{
    // Set FontFamily for shape and recurse for children
    setFontFamily(anEditor, aShape, aFont);
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
        RMTool tool = anEditor.getTool(child); tool.setFontFamilyDeep(anEditor, child, aFont); }
}

/**
 * Sets the font name for given shape.
 */
public void setFontName(RMEditor anEditor, RMShape aShape, RMFont aFont)
{
    // Get new font for name and current shape size and set
    RMFont font = getFont(anEditor, aShape);
    RMFont font2 = font!=null? aFont.deriveFont(font.getSize()) : aFont;
    setFont(anEditor, aShape, font2);
}

/**
 * Sets the font name for given shape.
 */
public void setFontNameDeep(RMEditor anEditor, RMShape aShape, RMFont aFont)
{
    // Set Font name for shape and recurse for children
    setFontName(anEditor, aShape, aFont);
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
        RMTool tool = anEditor.getTool(child); tool.setFontNameDeep(anEditor, child, aFont); }
}

/**
 * Sets the font size for given shape.
 */
public void setFontSize(RMEditor anEditor, RMShape aShape, double aSize, boolean isRelative)
{
    // Get new font for current shape font at new size and set
    RMFont font = getFont(anEditor, aShape); if(font==null) return;
    RMFont font2 = isRelative? font.deriveFont(font.getSize() + aSize) : font.deriveFont(aSize);
    setFont(anEditor, aShape, font2);
}

/**
 * Sets the font size for given shape.
 */
public void setFontSizeDeep(RMEditor anEditor, RMShape aShape, double aSize, boolean isRelative)
{
    setFontSize(anEditor, aShape, aSize, isRelative);
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
        RMTool tool = anEditor.getTool(child); tool.setFontSizeDeep(anEditor, child, aSize, isRelative); }    
}

/**
 * Sets the font to bold or not bold for given shape.
 */
public void setFontBold(RMEditor anEditor, RMShape aShape, boolean aFlag)
{
    RMFont font = getFont(anEditor, aShape); if(font==null || font.isBold()==aFlag) return;
    RMFont font2 = font.getBold(); if(font2==null) return;
    setFont(anEditor, aShape, font2);
}

/**
 * Sets the font to bold or not bold for given shape and its children.
 */
public void setFontBoldDeep(RMEditor anEditor, RMShape aShape, boolean aFlag)
{
    setFontBold(anEditor, aShape, aFlag);
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
        RMTool tool = anEditor.getTool(child); tool.setFontBoldDeep(anEditor, child, aFlag); }    
}

/**
 * Sets the font to italic or not italic for given shape.
 */
public void setFontItalic(RMEditor anEditor, RMShape aShape, boolean aFlag)
{
    RMFont font = getFont(anEditor, aShape); if(font==null || font.isItalic()==aFlag) return;
    RMFont font2 = font.getItalic(); if(font2==null) return;
    setFont(anEditor, aShape, font2);
}

/**
 * Sets the font to italic or not italic for given shape and its children.
 */
public void setFontItalicDeep(RMEditor anEditor, RMShape aShape, boolean aFlag)
{
    setFontItalic(anEditor, aShape, aFlag);
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
        RMTool tool = anEditor.getTool(child); tool.setFontItalicDeep(anEditor, child, aFlag); }    
}

/**
 * Event handling - called on mouse move when this tool is active.
 */
public void mouseMoved(ViewEvent anEvent)  { }

/**
 * Event handling for shape creation.
 */
public void mousePressed(ViewEvent anEvent)
{
    // Set undo title
    getEditor().undoerSetUndoTitle("Add Shape");

    // Save the mouse down point
    _downPoint = getEditorEvents().getEventPointInShape(true);

    // Create shape and move to downPoint
    _shape = newInstance();
    _shape.setXY(_downPoint);
    
    // Add shape to superSelectedShape and select shape
    getEditor().getSuperSelectedParentShape().addChild(_shape);
    getEditor().setSelectedShape(_shape);
}

/**
 * Event handling for shape creation.
 */
public void mouseDragged(ViewEvent anEvent)
{
    _shape.repaint();
    Point currentPoint = getEditorEvents().getEventPointInShape(true);
    double x = Math.min(_downPoint.getX(), currentPoint.getX());
    double y = Math.min(_downPoint.getY(), currentPoint.getY());
    double w = Math.abs(currentPoint.getX() - _downPoint.getX());
    double h = Math.abs(currentPoint.getY() - _downPoint.getY());
    _shape.setFrame(x, y, w, h);
}

/**
 * Event handling for shape creation.
 */
public void mouseReleased(ViewEvent anEvent)  { getEditor().setCurrentToolToSelectTool(); _shape = null; }

/**
 * Event handling from SelectTool for super selected shapes.
 */
public void processEvent(T aShape, ViewEvent anEvent)
{
    switch(anEvent.getType()) {
        case MousePress: mousePressed(aShape, anEvent); break;
        case MouseDrag: mouseDragged(aShape, anEvent); break;
        case MouseRelease: mouseReleased(aShape, anEvent); break;
        case MouseMove: mouseMoved(aShape, anEvent); break;
        default: if(anEvent.isKeyEvent()) processKeyEvent(aShape, anEvent);
    }
}

/**
 * Event handling from select tool for super selected shapes.
 */
public void mousePressed(T aShape, ViewEvent anEvent)  { }

/**
 * Event handling from select tool for super selected shapes.
 */
public void mouseDragged(T aShape, ViewEvent anEvent)  { }

/**
 * Event handling from select tool for super selected shapes.
 */
public void mouseReleased(T aShape, ViewEvent anEvent)  { }

/**
 * Event handling from select tool - called on mouse move when tool shape is super selected.
 * MouseMoved is useful for setting a custom cursor.
 */
public void mouseMoved(T aShape, ViewEvent anEvent)
{
    // Just return if shape isn't the super-selected shape
    //if(aShape!=getEditor().getSuperSelectedShape()) return;
    
    // Get handle shape
    RMShapeHandle shapeHandle = getShapeHandleAtPoint(anEvent.getPoint());
    
    // Declare variable for cursor
    Cursor cursor = null;
    
    // If shape handle is non-null, set cursor and return
    if(shapeHandle!=null)
        cursor = shapeHandle.tool.getHandleCursor(shapeHandle.shape, shapeHandle.handle);
    
    // If mouse not on handle, check for mouse over a shape
    else {
        
        // Get mouse over shape
        RMShape shape = getEditor().getShapeAtPoint(anEvent.getX(),anEvent.getY());
        
        // If shape isn't super selected and it's parent doesn't superselect children immediately, choose move cursor
        if(!isSuperSelected(shape) && !shape.getParent().childrenSuperSelectImmediately())
            cursor = Cursor.MOVE;
        
        // If shape is text and either super-selected or child of a super-select-immediately, choose text cursor
        if(shape instanceof RMTextShape && (isSuperSelected(shape) || shape.getParent().childrenSuperSelectImmediately()))
            cursor = Cursor.TEXT;
    }
    
    // Set cursor if it differs
    getEditor().setCursor(cursor);
}

/**
 * Event hook during selection.
 */
public boolean mousePressedSelection(ViewEvent anEvent)  { return false; }

/**
 * Returns a tool tip string for given shape and event.
 */
public String getToolTip(T aShape, ViewEvent anEvent)  { return null; }

/**
 * Editor method.
 */
public void processKeyEvent(T aShape, ViewEvent anEvent)  { }

/**
 * Paints when tool is active for things like SelectTool's handles & selection rect or polygon's in-progress path.
 */
public void paintTool(Painter aPntr)  { }

/**
 * Handles painting shape handles (or any indication that a shape is selected/super-selected).
 */
public void paintHandles(T aShape, Painter aPntr, boolean isSuperSelected)
{
    // If no handles, just return
    if(getHandleCount(aShape)==0) return;
    
    // Turn off antialiasing and cache current composite
    aPntr.setAntialiasing(false);
    double opacity = aPntr.getOpacity();
    
    // If super-selected, set composite to make drawing semi-transparent
    if(isSuperSelected)
        aPntr.setOpacity(.64);
    
    // Determine if rect should be reduced if the shape is especially small
    boolean mini = aShape.getWidth()<16 || aShape.getHeight()<16;
        
    // Iterate over shape handles, get rect (reduce if needed) and draw
    for(int i=0, iMax=getHandleCount(aShape); i<iMax; i++) {
        Rect hr = getHandleRect(aShape, i, isSuperSelected); if(mini) hr.inset(1, 1);
        aPntr.drawImage(_handle, hr.getX(), hr.getY(), hr.getWidth(), hr.getHeight());
    }
        
    // Restore composite and turn on antialiasing
    aPntr.setOpacity(opacity);
    aPntr.setAntialiasing(true);
}

/**
 * Returns the number of handles for this shape.
 */
public int getHandleCount(T aShape)  { return 8; }

/**
 * Returns the point for the handle of the given shape at the given handle index in the given shape's coords.
 */
public Point getHandlePoint(T aShape, int aHandle, boolean isSuperSelected)
{
    // Get bounds of given shape
    Rect bounds = isSuperSelected? getBoundsSuperSelected(aShape).getInsetRect(-HandleWidth/2):aShape.getBoundsInside();
    
    // Get minx and miny of given shape
    double minX = aShape.width()>=0? bounds.getX() : bounds.getMaxX();
    double minY = aShape.height()>=0? bounds.getY() : bounds.getMaxY();
    
    // Get maxx and maxy of givn shape
    double maxX = aShape.width()>=0? bounds.getMaxX() : bounds.getX();
    double maxY = aShape.height()>=0? bounds.getMaxY() : bounds.getY();
    
    // Get midx and midy of given shape
    double midX = minX + (maxX-minX)/2;
    double midY = minY + (maxY-minY)/2;
    
    // Get point for given handle
    switch(aHandle) {
        case HandleNW: return new Point(minX, minY);
        case HandleNE: return new Point(maxX, minY);
        case HandleSW: return new Point(minX, maxY);
        case HandleSE: return new Point(maxX, maxY);
        case HandleW: return new Point(minX, midY);
        case HandleE: return new Point(maxX, midY);
        case HandleN: return new Point(midX, minY);
        case HandleS: return new Point(midX, maxY);
    }
    
    // Return null if invalid handle
    return null;
}

/**
 * Returns the rect for the handle at the given index in editor coords.
 */
public Rect getHandleRect(T aShape, int aHandle, boolean isSuperSelected)
{
    // Get handle point for given handle index in shape coords and editor coords
    Point hp = getHandlePoint(aShape, aHandle, isSuperSelected);
    Point hpEd = getEditor().convertFromShape(hp.getX(), hp.getY(), aShape);
    
    // Get handle rect at handle point, outset rect by handle width and return
    Rect hr = new Rect(Math.round(hpEd.getX()), Math.round(hpEd.getY()), 0, 0);
    hr.inset(-HandleWidth/2);
    return hr;
}

/**
 * Returns the handle hit by the given editor coord point.
 */
public int getHandleAtPoint(T aShape, Point aPoint, boolean isSuperSelected)
{
    // Iterate over shape handles, get handle rect for current loop handle and return index if rect contains point
    for(int i=0, iMax=getHandleCount(aShape); i<iMax; i++) {
        Rect hr = getHandleRect(aShape, i, isSuperSelected);
        if(hr.contains(aPoint.getX(), aPoint.getY()))
            return i; }
    return -1; // Return -1 since no handle at given point
}

/**
 * Returns the cursor for given handle.
 */
public Cursor getHandleCursor(T aShape, int aHandle)
{
    // Get cursor for handle type
    switch(aHandle) {
        case HandleN: return Cursor.N_RESIZE;
        case HandleS: return Cursor.S_RESIZE;
        case HandleE: return Cursor.E_RESIZE;
        case HandleW: return Cursor.W_RESIZE;
        case HandleNW: return Cursor.NW_RESIZE;
        case HandleNE: return Cursor.NE_RESIZE;
        case HandleSW: return Cursor.SW_RESIZE;
        case HandleSE: return Cursor.SE_RESIZE;
    }

    // Return null
    return null;
}

/**
 * Moves the handle at the given index to the given point.
 */
public void moveShapeHandle(T aShape, int aHandle, Point toPoint)
{
    // Get handle point in shape coords and shape parent coords
    Point p1 = getHandlePoint(aShape, aHandle, false);
    Point p2 = aShape.convertedPointFromShape(toPoint, aShape.getParent());
    
    // If middle handle is used, set delta and p2 of that component to 0
    boolean minX = false, maxX = false, minY = false, maxY = false;
    switch(aHandle) {
        case HandleNW: minX = minY = true; break;
        case HandleNE: maxX = minY = true; break;
        case HandleSW: minX = maxY = true; break;
        case HandleSE: maxX = maxY = true; break;
        case HandleW: minX = true; break;
        case HandleE: maxX = true; break;
        case HandleS: maxY = true; break;
        case HandleN: minY = true; break;
    }

    // Calculate new width and height for handle move
    double dx = p2.getX() - p1.getX(), dy = p2.getY() - p1.getY();
    double nw = minX? aShape.width() - dx : maxX? aShape.width() + dx : aShape.width();
    double nh = minY? aShape.height() - dy : maxY? aShape.height() + dy : aShape.height();

    // Set new width and height, but calc new X & Y such that opposing handle is at same location w.r.t. parent
    Point op = getHandlePoint(aShape, getHandleOpposing(aHandle), false);
    aShape.convertPointToShape(op, aShape.getParent());
    
    // Make sure new width and height are not too small
    if(Math.abs(nw)<.1) nw = MathUtils.sign(nw)*.1f;
    if(Math.abs(nh)<.1) nh = MathUtils.sign(nh)*.1f;

    // Set size
    aShape.setSize(nw, nh);
    
    // Get point
    Point p = getHandlePoint(aShape, getHandleOpposing(aHandle), false);
    aShape.convertPointToShape(p, aShape.getParent());
    
    // Set frame
    aShape.setFrameXY(aShape.getFrameX() + op.getX() - p.getX(), aShape.getFrameY() + op.getY() - p.getY());
}

/**
 * Returns the handle index that is across from given handle index.
 */
public int getHandleOpposing(int handle)
{
    // Return opposing handle from given panel
    switch(handle) {
        case HandleNW: return HandleSE;
        case HandleNE: return HandleSW;
        case HandleSW: return HandleNE;
        case HandleSE: return HandleNW;
        case HandleW: return HandleE;
        case HandleE: return HandleW;
        case HandleS: return HandleN;
        case HandleN: return HandleS;
    }
    
    // Return -1 if given handle is unknown
    return -1;
}

/**
 * An inner class describing a shape and a handle.
 */
public static class RMShapeHandle {

    // The shape, handle index and shape tool
    public RMShape  shape;
    public int      handle;
    public RMTool   tool;
    
    /** Creates a new shape-handle. */
    public RMShapeHandle(RMShape aShape, int aHndl, RMTool aTool) { shape = aShape; handle = aHndl; tool = aTool; }
}

/**
 * Returns the shape handle for the given editor point.
 */
public RMShapeHandle getShapeHandleAtPoint(Point aPoint)
{
    // Declare variable for shape and handle and shape tool
    RMShape shape = null; int handle = -1; RMTool tool = null;
    RMEditor editor = getEditor();

    // Check selected shapes for a selected handle index
    for(int i=0, iMax=editor.getSelectedShapeCount(); handle==-1 && i<iMax; i++) {
        shape = editor.getSelectedShape(i);
        tool = editor.getTool(shape);
        handle = tool.getHandleAtPoint(shape, aPoint, false);
    }

    // Check super selected shapes for a selected handle index
    for(int i=0, iMax=editor.getSuperSelectedShapeCount(); handle==-1 && i<iMax; i++) {
        shape = editor.getSuperSelectedShape(i);
        tool = editor.getTool(shape);
        handle = tool.getHandleAtPoint(shape, aPoint, true);
    }

    // Return shape handle
    return handle>=0? new RMShapeHandle(shape, handle, tool) : null;
}

/**
 * Implemented by shapes that can handle drag & drop.
 */
public boolean acceptsDrag(T aShape, ViewEvent anEvent)
{
    // Bogus, but currently the page accepts everything
    if(aShape.isRoot()) return true;
    
    // Return true for Color drag or File drag
    if(anEvent.getClipboard().hasColor()) return true;
    
    // Handle file drag - really just want to check for images here, but can't ask for transferable contents yet
    if(anEvent.getClipboard().hasFiles())
        return true;
    
    // Return true in any case if accepts children
    return getEditor().getTool(aShape).getAcceptsChildren(aShape);
}

/**
 * Notifies tool that a something was dragged into of one of it's shapes with drag and drop.
 */
public void dragEnter(RMShape aShape, ViewEvent anEvent)  { }

/**
 * Notifies tool that a something was dragged out of one of it's shapes with drag and drop.
 */
public void dragExit(RMShape aShape, ViewEvent anEvent)  { }

/**
 * Notifies tool that something was dragged over one of it's shapes with drag and drop.
 */
public void dragOver(RMShape aShape, ViewEvent anEvent)  { }

/**
 * Notifies tool that something was dropped on one of it's shapes with drag and drop.
 */
public void drop(T aShape, ViewEvent anEvent)
{
    // If a binding key drop, apply binding
    if(KeysPanel.getDragKey()!=null)
        KeysPanel.dropDragKey(aShape, anEvent);

    // Handle String drop
    else if(anEvent.getClipboard().hasString())
        dropString(aShape, anEvent);

    // Handle color panel drop
    else if(anEvent.getClipboard().hasColor())
        dropColor(aShape, anEvent);

    // Handle File drop - get list of dropped files and add individually
    else if(anEvent.getClipboard().hasFiles())
        dropFiles(aShape, anEvent);
}

/**
 * Called to handle dropping a string.
 */
public void dropString(T aShape, ViewEvent anEvent)  { }

/**
 * Called to handle dropping a color.
 */
public void dropColor(RMShape aShape, ViewEvent anEvent)
{
    Color color = anEvent.getClipboard().getColor();
    getEditor().undoerSetUndoTitle("Set Fill Color");
    aShape.setFill(new RMFill(RMColor.get(color)));
}

/**
 * Called to handle dropping a file.
 */
public void dropFiles(RMShape aShape, ViewEvent anEvent)
{
    List <ClipboardData> filesList = anEvent.getClipboard().getFiles(); Point point = anEvent.getPoint();
    for(ClipboardData file : filesList)
        point = dropFile(aShape, file, anEvent.getPoint());
}

/**
 * Called to handle a file drop on the editor.
 */
private Point dropFile(RMShape aShape, ClipboardData aFile, Point aPoint)
{
    // Get path and extension (set to empty string if null)
    String ext = aFile.getExtension(); if(ext==null) return aPoint; ext = ext.toLowerCase();

    // If xml file, pass it to setDataSource()
    if(ext.equals("xml") || ext.equals("json"))
        getEditorPane().setDataSource(aFile.getSourceURL(), aPoint.getX(), aPoint.getY());

    // If image file, add image shape
    else if(RMImageData.canRead(ext))
        runLater(() -> dropImageFile(aShape, aFile, aPoint));

    // If reportmill file, addReportFile
    else if(ext.equals("rpt")) dropReportFile(aShape, aFile, aPoint);
    
    // Return point offset by 10
    aPoint.offset(10, 10); return aPoint;
}

/**
 * Called to handle an image drop on the editor.
 */
private void dropImageFile(RMShape aShape, ClipboardData aFile, Point aPoint)
{
    // Get image source
    Object imgSrc = aFile.getSourceURL()!=null? aFile.getSourceURL() : aFile.getBytes();
    
    // If image hit a real shape, see if user wants it to be a texture
    RMEditor editor = getEditor();
    if(aShape!=editor.getSelectedPage()) {
        
        // Create drop image file options array
        String options[] = { "Image Shape", "Texture", "Cancel" };
        
        // Run drop image file options panel
        String msg = "Image can be either image shape or texture", title = "Image import";
        DialogBox dbox = new DialogBox(title); dbox.setQuestionMessage(msg); dbox.setOptions(options);
        switch(dbox.showOptionDialog(null, options[0])) {
        
            // Handle Create Image Shape
            case 0: while(!editor.getTool(aShape).getAcceptsChildren(aShape)) aShape = aShape.getParent(); break;
            
            // Handle Create Texture
            case 1: aShape.setFill(new RMImageFill(imgSrc, true));
            
            // Handle Cancel
            case 2: return;
        }
    }
    
    // Get parent to add image shape to and drop point in parent coords
    RMParentShape parent = aShape instanceof RMParentShape? (RMParentShape)aShape : aShape.getParent();
    Point point = editor.convertToShape(aPoint.x, aPoint.y, parent);
    
    // Create new image shape
    RMImageShape imageShape = new RMImageShape(imgSrc);
    
    // If image not PDF and is bigger than hit shape, shrink down
    if(!imageShape.getImageData().getType().equals("pdf"))
        if(imageShape.getWidth()>parent.getWidth() || imageShape.getHeight()>parent.getHeight()) {
            double w = imageShape.getWidth();
            double h = imageShape.getHeight();
            double w2 = w>h? 320 : 320/h*w;
            double h2 = h>w? 320 : 320/w*h;
            imageShape.setSize(w2, h2);
        }

    // Set bounds centered around point (or centered on page if image covers 75% of page or more)
    if(imageShape.getWidth()/editor.getWidth()>.75f || imageShape.getHeight()/editor.getHeight()>.75)
        imageShape.setXY(0, 0);
    else imageShape.setXY(point.x - imageShape.getWidth()/2, point.y - imageShape.getHeight()/2);

    // Add imageShape with undo
    editor.undoerSetUndoTitle("Add Image");
    parent.addChild(imageShape);
    
    // Select imageShape and SelectTool
    editor.setSelectedShape(imageShape);
    editor.setCurrentToolToSelectTool();
    
    // If image not loaded, resize when loaded
    Image img = imageShape.getImageData().getImage();
    if(!img.isLoaded()) {
        img.addPropChangeListener(pce -> {
            double mx = imageShape.getX() + imageShape.getWidth()/2, my = imageShape.getY() + imageShape.getHeight()/2;
            double w = img.getWidth(), h = img.getHeight();
            Rect rect = new Rect(mx-w/2,my-h/2,w,h); rect.snap();
            imageShape.setBounds(rect);
        });
    }
}

/**
 * Called to handle a report file drop on the editor.
 */
private void dropReportFile(RMShape aShape, ClipboardData aFile, Point aPoint)
{
    // Find a parent shape that accepts children
    RMEditor editor = getEditor();
    RMParentShape parent = aShape instanceof RMParentShape? (RMParentShape)aShape : aShape.getParent();
    while(!editor.getTool(parent).getAcceptsChildren(parent))
        parent = parent.getParent();
    
    // Get document for dropped file and embedded document shape for document
    RMDocument doc = RMDocument.getDoc(aFile.getBytes());
    RMNestedDoc ndoc = new RMNestedDoc(); ndoc.setNestedDoc(doc);
    
    // Center embedded document around drop point
    Point point = editor.convertToShape(aPoint.x, aPoint.y, parent);
    ndoc.setXY(point);

    // Add edoc to document
    editor.undoerSetUndoTitle("Add Embedded Document");
    parent.addChild(ndoc);

    // Select edoc and selectTool
    editor.setSelectedShape(ndoc);
    editor.setCurrentToolToSelectTool();
}

/**
 * Returns a clone of a gallery shape (hook to allow extra configuration for subclasses).
 */
public RMShape getGalleryClone(T aShape)  { return aShape.cloneDeep(); }

/**
 * Creates a shape mouse event.
 */
protected ViewEvent createShapeEvent(RMShape s, ViewEvent e)  { return getEditor().createShapeEvent(s, e, null); }

/**
 * Returns the image used to represent shapes that this tool represents.
 */
public Image getImage()
{
    for(Class c=getClass(); c!=RMTool.class; c=c.getSuperclass()) {
        String name = c.getSimpleName().replace("Tool", "") + ".png";
        Image image = Image.get(c, name);
        if(image!=null) return image;
    }
    return Image.get(RMTool.class, "RMShape.png");
}

/**
 * Returns the specific tool for a given shape.
 */
public static RMTool createTool(Class aClass)
{
    // Handle root
    if(aClass==RMShape.class) return new RMTool();
    
    // Declare variable for tool class
    Class tclass = null;
    
    // If class name starts with RM, check tool package for built-in RMShape tools
    String cname = aClass.getSimpleName();
    if(cname.startsWith("RM")) {
        tclass = ClassUtils.getClass("com.reportmill.apptools." + cname + "Tool");
        if(tclass==null && cname.endsWith("Shape"))
            tclass = ClassUtils.getClass("com.reportmill.apptools." + cname.replace("Shape", "Tool"));
    }

    // If not found, try looking in same package for shape class plus "Tool"
    if(tclass==null)
        tclass = ClassUtils.getClass(aClass.getName() + "Tool", aClass);
    
    // If not found and class ends in "Shape", try looking in same package for class that ends with "Tool" instead
    if(tclass==null && cname.endsWith("Shape"))
        tclass = ClassUtils.getClass(StringUtils.replace(aClass.getName(), "Shape", "Tool"), aClass);
    
    // If not found and class is some external shapes package, look in external tools package
    if(tclass==null && aClass.getName().indexOf(".shape.")>0) {
        String classPath = StringUtils.replace(aClass.getName(), ".shape.", ".tool.");
        String classPath2 = StringUtils.delete(classPath, "Shape") + "Tool";
        tclass = ClassUtils.getClass(classPath2, aClass);
    }
    
    // If not found, try looking for inner class named "Tool"
    if(tclass==null)
        tclass = ClassUtils.getClass(aClass.getName() + "$" + "Tool", aClass);
    
    // If tool class found, instantiate tool class
    if(tclass!=null)
        try { return (RMTool)tclass.newInstance(); }
        catch(Exception e) { throw new RuntimeException(e); }
        
    // Otherwise, get tool for super class
    return createTool(aClass.getSuperclass());
}

}