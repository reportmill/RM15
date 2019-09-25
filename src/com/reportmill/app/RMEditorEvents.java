/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.shape.*;
import snap.gfx.Point;
import snap.gfx.Rect;
import snap.util.MathUtils;
import snap.view.*;

/**
 * Handles editor methods specific to event operations.
 */
public class RMEditorEvents extends RMViewerEvents {
    
    // The cached current event for any mouse loop handled by this editor events
    ViewEvent    _currentEvent;
    
    // The down point for any mouse loop handled by this editor events
    Point        _downPoint;
    
    // Whether to override editor preview mode
    boolean      _overridePreview;
    
    // Constants for guide orientation
    private static final byte GUIDE_HORIZONTAL = 0;
    private static final byte GUIDE_VERTICAL = 1;

/**
 * Creates a new editor events object.
 */
public RMEditorEvents(RMViewer aViewer)  { super(aViewer); }

/**
 * Returns the viewer as an editor.
 */
public RMEditor getEditor()  { return (RMEditor)getViewer(); }

/**
 * Handles key press events.
 */
public void processEvent(ViewEvent anEvent)
{
    // If editing, send event to tool: Get super selected shape and its tool and send event
    RMEditor editor = getEditor();
    boolean isKey = anEvent.isKeyEvent() && !anEvent.isConsumed();
    if(isKey && (!editor.isPreview() || getOverridePreview())) {
        RMShape superSelectedShape = editor.getSuperSelectedShape();
        RMTool tool = editor.getTool(superSelectedShape);
        tool.processKeyEvent(superSelectedShape, anEvent);
    }
    
    // If event not consumed, do normal version
    if(!isKey || !anEvent.isConsumed())
        super.processEvent(anEvent);
}

/**
 * Handle mouse pressed.
 */
public void mousePressed(ViewEvent anEvent)
{
    // Get the editor
    RMEditor editor = getEditor();
    
    // If in preview mode, call normal version
    if(editor.isPreview() && !getOverridePreview()) { super.mousePressed(anEvent); return; }
    
    // Cache current event
    _currentEvent = anEvent;
    
    // Set downpoint and last point to current event point in document coords
    _downPoint = editor.convertToShape(anEvent.getX(), anEvent.getY(), null);

    // If current tool isn't select tool, see if super selected shape needs to be updated
    if(!editor.isCurrentToolSelectTool()) {
        RMShape shape = editor.firstSuperSelectedShapeThatAcceptsChildrenAtPoint(_downPoint);
        if(shape!=editor.getSuperSelectedShape())
            editor.setSuperSelectedShape(shape);
    }

    // Forward mouse pressed to current tool
    editor.getCurrentTool().mousePressed(anEvent);
}

/**
 * Handle mouse dragged.
 */
public void mouseDragged(ViewEvent anEvent)
{
    // Get the editor
    RMEditor editor = getEditor();
    
    // If in preview mode, call normal version
    if(editor.isPreview() && !getOverridePreview()) { super.mouseDragged(anEvent); return; }
    
    // Cache current event
    _currentEvent = anEvent;
    
    // Forward mouse dragged to current tool
    editor.getCurrentTool().mouseDragged(anEvent);
    
    // Autoscroll
    editor.scrollToVisible(new Rect(anEvent.getX(), anEvent.getY(), 1, 1));
        
    // Update rulers
    RMEditorPane epane = editor.getEditorPane();
    if(epane.isShowRulers())
        epane.getRulerBox().setMousePoint(anEvent.getPoint());
}

/**
 * Handle mouse released.
 */
public void mouseReleased(ViewEvent anEvent)
{
    // Get the editor
    RMEditor editor = getEditor();
    
    // If in preview mode, call normal version
    if(editor.isPreview() && !getOverridePreview()) { super.mouseReleased(anEvent); return; }
    
    // Cache current event, forward mouse released to current tool, clear current event
    _currentEvent = anEvent;
    editor.getCurrentTool().mouseReleased(anEvent); _currentEvent = null;
}

/**
 * Handle mouse moved event.
 */
public void mouseMoved(ViewEvent anEvent)
{
    // If in preview mode, call normal version
    RMEditor editor = getEditor();
    if(editor.isPreview() && !getOverridePreview()) { super.mouseMoved(anEvent); return; }
    
    // Otherwise, call tool mouseMoved to do stuff like set cursors
    else editor.getCurrentTool().mouseMoved(anEvent);
        
    // Update rulers
    RMEditorPane epane = editor.getEditorPane();
    if(epane.isShowRulers())
        epane.getRulerBox().setMousePoint(anEvent.getPoint());
}

/**
 * Handle key released.
 */
public void keyReleased(ViewEvent anEvent)
{
    // If in preview mode, call normal version
    if(getEditor().isPreview() && !getOverridePreview()) { super.keyReleased(anEvent); return; }
}

/**
 * Handle key pressed.
 */
public void keyPressed(ViewEvent anEvent)
{
    // If in preview mode, call normal version
    RMEditor editor = getEditor();
    if(editor.isPreview() && !getOverridePreview()) {
        
        // Do normal version
        super.keyPressed(anEvent);
        
        // If event not consumed and user hit x, turn on preview override
        if(!anEvent.isConsumed() && anEvent.getKeyCode()==KeyCode.X)
            setOverridePreview(true);
        return; // Return
    }
    
    // If event is command key, just return
    if(anEvent.isShortcutDown()) return;
    
    // Get key code & key char
    int keyCode = anEvent.getKeyCode();
    char keyChar = anEvent.getKeyChar();
    
    // Handle escape (assuming mouse isn't down)
    if(keyCode==KeyCode.ESCAPE && !ViewUtils.isMouseDown()) {
        if(editor.getEditorPane().getAttributesPanel().getDrawer().isShowing())
            editor.getEditorPane().hideAttributesDrawer();
        else editor.popSelection();
    }
    
    // Handle backspace or delete key
    else if(keyCode==KeyCode.BACK_SPACE || keyCode==KeyCode.DELETE)
        editor.delete();
    
    // Handle left, right, up, down arrows
    else if(keyCode==KeyCode.LEFT) RMEditorUtils.moveLeftOnePoint(editor);
    else if(keyCode==KeyCode.RIGHT) RMEditorUtils.moveRightOnePoint(editor);
    else if(keyCode==KeyCode.UP) RMEditorUtils.moveUpOnePoint(editor);
    else if(keyCode==KeyCode.DOWN) RMEditorUtils.moveDownOnePoint(editor);

    // If 6 key, show Undo inspector (for undo debugging)
    else if(keyChar=='6')
        editor.getEditorPane().getInspectorPanel().setVisible(6);
    
    // If 8 key, show Undo inspector (for undo debugging)
    else if(keyChar=='8')
        editor.getEditorPane().getInspectorPanel().setVisible(8);
    
    // If T key, swap in linked text
    else if(keyChar=='t')
        ((RMTextTool)editor.getTool(RMTextShape.class)).convertToText(editor.getSelectedShape(), "test");
    
    // Otherwise, set consume to false
    else return;
    
    // Consume event
    anEvent.consume();
}

/**
 * Handle key pressed.
 */
public void keyTyped(ViewEvent anEvent)
{
    // If in preview mode, call normal version
    if(getEditor().isPreview() && !getOverridePreview()) { super.keyTyped(anEvent); return; }
}

/**
 * Returns the current event.
 */
public ViewEvent getCurrentEvent()  { return _currentEvent; }

/**
 * Returns the current event point in document coords.
 */
public Point getEventPointInDoc()  { return getEventPointInDoc(false); }

/**
 * Returns the current event point in document coords with an option to adjust to conform to grid.
 */
public Point getEventPointInDoc(boolean snapToGrid)
{
    // Get the editor
    RMEditor editor = getEditor();
    
    // Get current event point in doc coords, rounded to integers
    Point point = editor.convertToShape(_currentEvent.getX(), _currentEvent.getY(), null);
    point.snap();
    
    // If shift key is down, constrain values to increments of 45 degrees from _downPoint
    if(_currentEvent.isShiftDown() && !editor.isCurrentToolSelectToolAndSelecting()) {

        // Get absolute values of delta X and delta Y relative to mouseDown point
        double absX = Math.abs(point.getX() - _downPoint.getX()), absY = Math.abs(point.getY() - _downPoint.getY());

        // If X is greater than Y set Y to either X or zero
        if(absX > absY) {
            // If X is twice as big as Y or more, set Y to 0, If X is less than twice as big as Y, set Y to X
            if(absX > 2*absY) point.setY(_downPoint.getY());
            else point.setY(_downPoint.getY() + MathUtils.sign(point.getY() - _downPoint.getY())*absX);
        }

        // If Y is greater than X, set X to either Y or zero
        else {
            // If X is twice as big as Y or more, set Y to 0, If X is less than twice as big as Y, set Y to X
            if(absY > 2*absX) point.setX(_downPoint.getX());
            else point.setX(_downPoint.getX() + MathUtils.sign(point.getX() - _downPoint.getX())*absY);
        }
    }
    
    // If requested point snapped to grid, adjust point for grid
    if(snapToGrid)
        point = pointSnapped(point, false);
    
    // Return point
    return point;
}

/**
 * Returns the current event point in super-selected shape coords, optionally snapped to grid.
 */
public Point getEventPointInShape(boolean snapToGrid)  { return getEventPointInShape(snapToGrid, false); }

/**
 * Returns the current event point in super-selected shape coords with an option to adjust to conform to grid.
 */
public Point getEventPointInShape(boolean snapToGrid, boolean snapEdges)
{
    // Get event point in doc coords
    Point point = getEventPointInDoc();
    
    // If requested point snapped to grid, adjust point for grid
    if(snapToGrid)
        point = pointSnapped(point, snapEdges);
    
    // Return point converted to super selected point
    return getEditor().getSuperSelectedShape().parentToLocal(point, null);
}

/**
 * Returns the given point corrected for grids and guides.
 */
private Point pointSnapped(Point aPoint, boolean snapEdges)
{
    // Get the editor and editor shape
    RMEditor editor = getEditor();
    RMDocument doc = editor.getDoc(); if(doc==null) return aPoint;
    
    // Get local copy of point
    Point point = aPoint;
    double x = point.getX(), y = point.getY();

    // If doc snaps to grid, adjust for snap 
    if(doc.getSnapGrid())
        point = pointSnappedToGrid(point, snapEdges);
    
    // If doc has guides, adjust for guides
    else if(getGuideCount(doc)>0)
        point = pointSnappedToGuides(point, snapEdges);
    
    // If points haven't changed, adjust for proximity guides
    if(x==point.getX() && y==point.getY())
        point = RMEditorProxGuide.pointSnappedToProximityGuides(editor, point);
    
    // Return point
    return point;
}

/**
 * Returns a given point adjusted for grids & guides.
 */
private Point pointSnappedToGrid(Point aPoint, boolean snapEdges)
{
    // Get the editor and editor shape
    RMEditor editor = getEditor();
    RMDocument doc = editor.getDoc(); if(doc==null) return aPoint;
    
    // Get document frame
    RMShape spage = editor.getSelPage();
    Rect docFrame = editor.convertFromShape(spage.getBoundsInside(), spage).getBounds();
    double docFrameX = docFrame.getX(), docFrameY = docFrame.getY();
    
    // Get grid spacing
    double gridSpacing = doc.getGridSpacing()*editor.getZoomFactor();
    
    // Get dx/dy for maximum offsets
    double dx = gridSpacing/2 + .001f;
    double dy = dx;
    
    // If not snapping to all edges, round aPoint to nearest grid or guide
    if(!snapEdges) {
        
        // Get point in editor coords
        aPoint = editor.convertFromShape(aPoint.getX(),aPoint.getY(), null);
    
        // Get dx/dy to nearest grid
        double px = MathUtils.round(aPoint.getX() - docFrameX, gridSpacing) + docFrameX;
        double py = MathUtils.round(aPoint.getY() - docFrameY, gridSpacing) + docFrameY;
        dx = px - aPoint.getX();
        dy = py - aPoint.getY();
    }
    
    // If SnapEdges, find dx/dy for all edges of selected shapes to nearest grid or guide
    else {
    
        // Iterate over selected shapes
        for(int i=0, iMax=editor.getSelectedShapeCount(); i<iMax; i++) { RMShape shape = editor.getSelectedShape(i);
            
            // Get shape bounds in editor coords
            Rect rect = editor.convertFromShape(shape.getBoundsInside(), shape).getBounds();
            double rectx = rect.getX(), recty = rect.getY();
            
            // Find dx/dy to nearest grid
            double px = MathUtils.round(rectx - docFrameX, gridSpacing) + docFrameX;
            double py = MathUtils.round(recty - docFrameY, gridSpacing) + docFrameY;
            double pmx = MathUtils.round(rect.getMaxX() - docFrameX, gridSpacing) + docFrameX;
            double pmy = MathUtils.round(rect.getMaxY() - docFrameY, gridSpacing) + docFrameY;
            if(Math.abs(px - rectx)<Math.abs(dx))
                dx = px - rectx;
            if(Math.abs(py - recty)<Math.abs(dy))
                dy = py - recty;
            if(Math.abs(pmx - rect.getMaxX())<Math.abs(dx))
                dx = pmx - rect.getMaxX();
            if(Math.abs(pmy - rect.getMaxY())<Math.abs(dy))
                dy = pmy - rect.getMaxY();
        }
        
        // Adjust offsets and grid spacing for zoom factor
        dx /= editor.getZoomFactor();
        dy /= editor.getZoomFactor();
        gridSpacing /= editor.getZoomFactor();
    }

    // Go ahead and offset aPoint if necessary
    if(Math.abs(dx)<=gridSpacing/2) aPoint.offset(dx,0);
    if(Math.abs(dy)<=gridSpacing/2) aPoint.offset(0,dy);
        
    // Covert back to shape if we need to
    if(!snapEdges)
        aPoint = editor.convertToShape(aPoint.x, aPoint.y, null); // Get aPoint in world coords
    
    // Return point
    return aPoint;
}

/**
 * Returns a given point adjusted for grids & guides.
 */
private Point pointSnappedToGuides(Point aPoint, boolean snapEdges)
{
    // Get the editor, document and document frame
    RMEditor editor = getEditor();
    RMDocument doc = editor.getDoc(); if(doc==null) return aPoint;
    RMShape spage = editor.getSelPage();
    Rect docFrame = editor.convertFromShape(spage.getBoundsInside(), spage).getBounds();
    
    // Get grid spacing and dx/dy for maximum offsets
    double gridSpacing = doc.getGridSpacing()*editor.getZoomFactor();
    double dx = gridSpacing/2 + .001f;
    double dy = dx;
    
    // If not snapping to all edges, round aPoint to nearest grid or guide
    if(!snapEdges) {
        
        // Get point in editor coords
        aPoint = editor.convertFromShape(aPoint.getX(), aPoint.getY(), null);
    
        // Find min dx/dy to nearest guide
        for(int j=0, jMax=getGuideCount(doc); j<jMax; j++) {
            byte orientation = getGuideOrientation(j);
            double location = getGuideLocation(doc, j)*editor.getZoomFactor() +
                (orientation==GUIDE_VERTICAL? docFrame.getX() : docFrame.getY());

            if(orientation==GUIDE_VERTICAL) {
                if(Math.abs(location - aPoint.getX())<Math.abs(dx))
                    dx = location - aPoint.getX();
            }
            else if(Math.abs(location - aPoint.getY())<Math.abs(dy))
                    dy = location - aPoint.getY();
        }
    }
    
    // If _snapEdges, find dx/dy for all edges of selected shapes to nearest grid or guide
    else {
    
        // Iterate over selected shapes
        for(int i=0, iMax=editor.getSelectedShapeCount(); i<iMax; i++) { RMShape shape = editor.getSelectedShape(i);
            
            // Get shape bounds in editor coords
            Rect rect = editor.convertFromShape(shape.getBoundsInside(), shape).getBounds();
            
            // Iterate over guides to find dx/dy to nearest guide
            for(int j=0, jMax=getGuideCount(doc); j<jMax; j++) {
                
                // Get current loop guide orientation 
                int orientation = getGuideOrientation(j);
                
                // Get current loop guide location
                double location = getGuideLocation(doc, j)*editor.getZoomFactor() +
                    (orientation==GUIDE_VERTICAL? docFrame.getX() : docFrame.getY());
                
                // If vertical...
                if(orientation==GUIDE_VERTICAL) {
                    double minxDx = location - rect.getX(), maxxDx = location - rect.getMaxX();
                    if(Math.abs(minxDx)<Math.abs(dx))
                        dx = minxDx;
                    if(Math.abs(maxxDx)<Math.abs(dx))
                        dx = maxxDx;
                }
                
                // If horizontal...
                if(orientation==GUIDE_HORIZONTAL) {
                    double minyDy = location - rect.getY(), maxyDy = location - rect.getMaxY();
                    if(Math.abs(minyDy)<Math.abs(dy)) dy = minyDy;
                    if(Math.abs(maxyDy)<Math.abs(dy)) dy = maxyDy;
                }
            }
        }
        
        // Adjust offsets and grid spacing for zoom factor
        dx /= editor.getZoomFactor();
        dy /= editor.getZoomFactor();
        gridSpacing /= editor.getZoomFactor();
    }

    // Go ahead and offset aPoint if necessary
    if(Math.abs(dx)<=gridSpacing/2) aPoint.offset(dx,0);
    if(Math.abs(dy)<=gridSpacing/2) aPoint.offset(0,dy);
        
    // Covert back to shape if we need to
    if(!snapEdges)
        aPoint = editor.convertToShape(aPoint.x, aPoint.y, null); // Get aPoint in world coords
    
    // Return point
    return aPoint;
}

/**
 * Returns the number of guides (4 if snapping to margin, otherwise zero).
 */
public static int getGuideCount(RMDocument aDoc)  { return aDoc.getSnapMargin()? 4 : 0; }

/**
 * Returns the guide location for the given index.
 */
public static double getGuideLocation(RMDocument aDoc, int anIndex)
{
    switch(anIndex) {
        case 0: return aDoc.getMarginLeft();
        case 1: return aDoc.getSelPage().getWidth() - aDoc.getMarginRight();
        case 2: return aDoc.getMarginTop();
        case 3: return aDoc.getSelPage().getHeight() - aDoc.getMarginBottom();
    }
    return 0;
}

/**
 * Returns the guide orientation for the given index.
 */
private byte getGuideOrientation(int anIndex)  { return anIndex==0 || anIndex==1? GUIDE_VERTICAL : GUIDE_HORIZONTAL; }

/**
 * Returns whether to override preview mode.
 */
public boolean getOverridePreview()  { return _overridePreview; }

/**
 * Sets whether to override preview mode.
 */
public void setOverridePreview(boolean aValue)  { _overridePreview = aValue; }

}