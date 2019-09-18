/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.app.RMEditorProxGuide;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.util.ListUtils;
import snap.view.ViewEvent;

/**
 * This class handles mouse selection and manipulation of shapes, including:
 *   - Click on a shape selects a shape
 *   - Double click on a shape super-selects a shape
 *   - Drag a rect selects shapes
 *   - Shift click or shift drag XORs selection
 *   - Click and drag handle resizes shape
 */
public class RMSelectTool extends RMTool {
    
    // The mode of current even loop (Move, Resize, etc.)
    DragMode        _dragMode = DragMode.None;
    
    // The point of last mouse
    Point           _lastMousePoint;
    
    // A construct representing a shape whose handle was hit and the handle
    RMShapeHandle   _shapeHandle;
    
    // The shape handling mouse events
    RMShape         _eventShape;

    // The current selection rect (during DragModeSelect)
    Rect            _selRect = new Rect();
    
    // The list of shapes that will be selected (during DragModeSelect)
    List <RMShape>  _newSelShapes = new ArrayList();
    
    // Whether to re-enter mouse pressed
    boolean         _redoMousePressed;

    // Drag mode constants
    public enum DragMode { None, Move, Rotate, Resize, Select, EventDispatch };
    
/**
 * Handles mouse pressed for the select tool.
 */
public void mousePressed(ViewEvent anEvent)
{
    // Get current editor
    RMEditor editor = getEditor();

    // Call setNeedsRepaint on superSelectedShapes to wipe out handles
    for(RMShape shp : editor.getSuperSelectedShapes()) shp.repaint();

    // See if tool wants to handle this one
    RMTool toolShared = editor.getTool(editor.getSelectedOrSuperSelectedShapes());
    if(toolShared!=null && toolShared.mousePressedSelection(anEvent)) {
        _dragMode = DragMode.None; return; }
    
    // Reset re-enter flag
    _redoMousePressed = false;

    // Set downPoint to event location.
    _downPoint = getEditorEvents().getEventPointInDoc();
    
    // Get shape handle for event point
    _shapeHandle = getShapeHandleAtPoint(anEvent.getPoint());

    // If shape handle was found for event point, set mode to resize.
    if(_shapeHandle!=null) {
        
        // Set DragMode to Resize
        _dragMode = DragMode.Resize;
        
        // Register shape handle shape for repaint
        _shapeHandle.shape.repaint();

        // If _selectedShape is superSelected, select it instead
        if(isSuperSelected(_shapeHandle.shape))
            editor.setSelectedShape(_shapeHandle.shape);

        // Just return
        return;
    }
    
    // Get shape hit by event point
    RMShape hitShape = editor.getShapeAtPoint(anEvent.getX(), anEvent.getY());
    
    // If HitShape is super-selected, make sure it's main Editor.SuperSelShape and start DragMode.Select
    if(isSuperSelected(hitShape)) {
        if(hitShape!=editor.getSuperSelectedShape())
            editor.setSuperSelectedShape(hitShape);
        _dragMode = DragMode.Select;
    }

    // If HitShape should be super-selected automatically, super-select and re-enter
    else if(hitShape.getParent()!=null && hitShape.getParent().childrenSuperSelectImmediately()) {
        editor.setSuperSelectedShape(hitShape);
        mousePressed(anEvent);
        return;
    }
        
    // If Multi-click and HitShape is super-selectable, super-select and re-enter with reduced clicks
    else if(anEvent.getClickCount()>1 && getTool(hitShape).isSuperSelectable(hitShape)) {
        editor.setSuperSelectedShape(hitShape);
        ViewEvent event = anEvent.copyForClickCount(anEvent.getClickCount()-1);
        mousePressed(event);
        return;
    }

    // If Shift-click, either add or remove HitShape from Editor.SelShapes
    else if(anEvent.isShiftDown()) {
        if(isSelected(hitShape)) editor.removeSelectedShape(hitShape);
        else editor.addSelectedShape(hitShape);
        _dragMode = DragMode.None;
    }
        
    // Otherwise, make sure HitShape is selected and start move or rotate
    else {
        if(!isSelected(hitShape))
            editor.setSelectedShape(hitShape);
        _dragMode = !anEvent.isAltDown()? DragMode.Move : DragMode.Rotate;
    }
    
    // Set last point to event point in super selected shape coords
    _lastMousePoint = getEditorEvents().getEventPointInShape(false);
    
    // Get editor super selected shape and call mouse pressed for superSelectedShape's tool
    RMShape superSelShape = editor.getSuperSelectedShape();
    getTool(superSelShape).processEvent(superSelShape, anEvent);
    
    // If redo mouse pressed was requested, do redo
    if(getRedoMousePressed()) {
        mousePressed(anEvent); return; }
        
    // If event was consumed, set event shape and DragMode to event dispatch and return
    if(anEvent.isConsumed()) {
        _eventShape = superSelShape; _dragMode = DragMode.EventDispatch; return; }
    
    // If HitShape is selected, call mouse pressed on HitShape's tool
    if(isSelected(hitShape)) {
        
        // Call mouse pressed on mousePressedShape's tool
        getTool(hitShape).processEvent(hitShape, anEvent);
        
        // If redo mouse pressed was requested, do redo
        if(getRedoMousePressed()) {
            mousePressed(anEvent); return; }
            
        // If event was consumed, set event shape and drag mode to event dispatch and return
        if(anEvent.isConsumed()) {
            _eventShape = hitShape; _dragMode = DragMode.EventDispatch; return; }
    }
}

/**
 * Handles mouse dragged for the select tool.
 */
public void mouseDragged(ViewEvent anEvent)
{
    // Get current editor
    RMEditor editor = getEditor();
    
    // Holding ctrl down at any point during a drag prevents snapping 
    boolean shouldSnap = !anEvent.isControlDown();

    // Handle specific drag modes
    switch(_dragMode) {

        // Handle DragModeMove
        case Move:
            
            // If drag event is still a click candidate, just bail
            if(anEvent.isClickCandidate()) return;
            
            // Set undo title
            editor.undoerSetUndoTitle("Move");
            
            // Get SuperSelectedShape and disable ParentTracksBoundsOfChildren
            RMParentShape parent = editor.getSuperSelectedParentShape();
            
            // Get event point in super selected shape coords
            Point point = getEditorEvents().getEventPointInShape(false);

            // Move shapes once to event point without SnapToGrid
            moveShapes(_lastMousePoint, point);
                        
            // Get event point snapped to grid & edges, since SnapEdges will now be valid
            Point pointSnapped = getEditorEvents().getEventPointInShape(shouldSnap, shouldSnap);
            Point pointSnappedDoc = parent.localToParent(pointSnapped, null);
            
            // Move shapes again to snapped point
            moveShapes(point, pointSnapped);
            
            // Get PointSnapped in (potentially) new bounds and break
            _lastMousePoint = parent.parentToLocal(pointSnappedDoc, null);
            break;
            
        // Handle Rotate
        case Rotate:

            // Set Undo title
            editor.undoerSetUndoTitle("Rotate");
            Point point2 = getEditorEvents().getEventPointInShape(false);
            
            // Iterate over selected shapes and update roll
            for(RMShape shape : editor.getSelectedShapes()) { if(shape.isLocked()) continue;
                shape.setRoll(shape.getRoll() + point2.getY() - _lastMousePoint.getY()); }

            // Reset last point and break
            _lastMousePoint = point2;
            break;

        // Handle DragModeResize
        case Resize:
            
            // Register undo title "Resize"
            editor.undoerSetUndoTitle("Resize");
            
            // Get event point in super selected shape coords snapped to grid 
            Point resizePoint = getEditorEvents().getEventPointInShape(shouldSnap);
            
            // Move handle to current point and break
            _shapeHandle.tool.moveShapeHandle(_shapeHandle.shape, _shapeHandle.handle, resizePoint);
            break;

        // Handle DragModeSelect
        case Select:

            // Get current hit shapes
            List newShapes = getHitShapes();
            
            // Repaint selected shapes and SelectionRect
            for(RMShape s : _newSelShapes) repaintShape(s);
            editor.repaint(editor.convertFromShape(_selRect.getInsetRect(-2), null).getBounds());
            
            // Get new SelRect and clear NewSelShapes
            _selRect = Rect.get(_downPoint, editor.convertToShape(anEvent.getX(), anEvent.getY(), null));
            _newSelShapes.clear();

            // If shift key was down, exclusive OR (xor) newShapes with selectedShapes
            if(anEvent.isShiftDown()) {
                List xor = ListUtils.clone(editor.getSelectedShapes());
                ListUtils.xor(xor, newShapes);
                _newSelShapes.addAll(xor);
            }
            
            // If shit key not down, select all new shapes
            else _newSelShapes.addAll(newShapes);

            // Repaint selected shapes and SelectionRect
            for(RMShape s : _newSelShapes) repaintShape(s);
            editor.repaint(editor.convertFromShape(_selRect.getInsetRect(-2), null).getBounds());

            // break
            break;

        // Handle DragModeSuperSelect: Forward mouse drag on to super selected shape's mouse dragged and break
        case EventDispatch: getTool(_eventShape).processEvent(_eventShape, anEvent); break;

        // Handle DragModeNone
        case None: break;
    }
    
    // Create guidelines
    RMEditorProxGuide.createGuidelines(editor);
}

/**
 * Handles mouse released for the select tool.
 */
public void mouseReleased(ViewEvent anEvent)
{
    RMEditor editor = getEditor();
    
    // Handle DragModes
    switch(_dragMode) {

        // Handle Select
        case Select:
            
            // Get hit shapes
            List newShapes = getHitShapes();
            
            // If shift key was down, exclusive OR (xor) newShapes with selectedShapes. Else select new shapes
            if(newShapes.size()>0) {
                if(anEvent.isShiftDown()) {
                    List xor = ListUtils.clone(editor.getSelectedShapes());
                    ListUtils.xor(xor, newShapes);
                    editor.setSelectedShapes(xor);
                }
                else editor.setSelectedShapes(newShapes);
            }
            
            // If no shapes were selected, clear selectedShapes
            else editor.setSuperSelectedShape(editor.getSuperSelectedShape());

            // Reset NewSelShapes and SelRect since we don't need them anymore
            _newSelShapes.clear();
            _selRect.setRect(0,0,0,0);
            break;

        // Handle EventDispatch
        case EventDispatch:
            getTool(_eventShape).processEvent(_eventShape, anEvent);
            _eventShape = null;
            break;
    }
    
    // Clear proximity guidelines
    RMEditorProxGuide.clearGuidelines(editor);

    // Repaint editor
    editor.repaint();
    
    // Reset drag mode
    _dragMode = DragMode.None;
}

/**
 * Handles mouse moved - forward on to super selected shape tool.
 */
public void mouseMoved(ViewEvent anEvent)
{
    // Iterate over super selected shapes and forward mouseMoved for each shape
    RMEditor editor = getEditor();
    for(int i=1, iMax=editor.getSuperSelectedShapeCount(); i<iMax && !anEvent.isConsumed(); i++) {
        RMShape shape = editor.getSuperSelectedShape(i);
        getTool(shape).mouseMoved(shape, anEvent);
    }
}

/**
 * Moves the currently selected shapes from a point to a point.
 */
private void moveShapes(Point fromPoint, Point toPoint)
{
    // Iterate over selected shapes
    for(int i=0, iMax=getEditor().getSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = getEditor().getSelectedShape(i); if(shape.isLocked()) continue;
        double fx = fromPoint.getX(), fy = fromPoint.getY(), tx = toPoint.getX(), ty = toPoint.getY();
        shape.setFrameXY(shape.getFrameX() + tx - fx, shape.getFrameY() + ty - fy);
    }
}

/**
 * Returns the list of shapes hit by the selection rect formed by the down point and current point.
 */
private List <RMShape> getHitShapes()
{
    // Get selection path from rect around currentPoint and _downPoint
    RMEditor editor = getEditor();
    RMParentShape superShape = editor.getSuperSelectedParentShape(); if(superShape==null)return Collections.emptyList();
    Point curPoint = getEditorEvents().getEventPointInDoc();
    Rect selRect = Rect.get(curPoint, _downPoint);
    Shape path = superShape.parentToLocal(selRect, null);

    // If selection rect is outside super selected shape, move up shape hierarchy
    while(superShape!=editor.getDoc() &&
        !path.getBounds().intersectsRect(getTool(superShape).getBoundsSuperSelected(superShape))) {
        RMParentShape parent = superShape.getParent();
        editor.setSuperSelectedShape(parent);
        path = superShape.localToParent(path);
        superShape = parent;
    }

    // Make sure page is worst case
    if(superShape == editor.getDoc()) {
        superShape = editor.getSelPage();
        path = superShape.parentToLocal(selRect, null);
        editor.setSuperSelectedShape(superShape);
    }

    // Returns the children of the super-selected shape that intersect selection path
    return superShape.getChildrenIntersecting(path);
}

/**
 * Returns the last drag mode handled by the select tool.
 */
public DragMode getDragMode()  { return _dragMode; }

/**
 * Returns whether select tool should redo current mouse down.
 */
public boolean getRedoMousePressed()  { return _redoMousePressed; }

/**
 * Sets whether select tool should redo current mouse dwon.
 */
public void setRedoMousePressed(boolean aFlag)  { _redoMousePressed = aFlag; }

/**
 * Paints tool specific things, like handles.
 */
public void paintTool(Painter aPntr)
{
    // Paint handles for super selected shapes and selected shapes (if mouse up)
    super.paintTool(aPntr);
    
    // If not mouseDown (select/move/resize), just return
    RMEditor editor = getEditor();
    if(!editor.isMouseDown())
        return;
        
    // Make sure that text bounds are drawn?
    List <RMShape> selectedShapes = editor.getSelectedShapes();
    for(RMShape shape : selectedShapes) { if(!(shape instanceof RMTextShape)) continue;
        RMTextTool tool = (RMTextTool)getTool(shape);
        tool.paintBoundsRect((RMTextShape)shape, aPntr);
    }

    // Paint handles for selecting shapes
    paintHandlesForShapes(aPntr, _newSelShapes);

    // Paint SelRect: light transparent rect with darker transparent border
    Rect rect = editor.convertFromShape(_selRect, null).getBounds();
    aPntr.setColor(new Color(.9,.5)); aPntr.fill(rect);
    aPntr.setStroke(Stroke.Stroke1); aPntr.setColor(new Color(.6,.6)); aPntr.draw(rect);
}

/**
 * Calls Editor.repaint() for given shape, assuming it might be selected and have handles.
 */
void repaintShape(RMShape aShape)
{
    Rect bnds0 = aShape.getBoundsMarkedDeep(); bnds0.inset(-4);
    Rect bnds1 = aShape.localToParent(bnds0, null).getBounds();
    getEditor().repaint(bnds1);
}

/**
 * Tool callback selects parent of selected shapes (or just shape, if it's super-selected).
 */
public void reactivateTool()  { getEditor().popSelection(); }

}