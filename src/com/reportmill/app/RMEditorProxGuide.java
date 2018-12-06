/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.shape.*;
import java.util.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;

/**
 * This class offers some methods to provide "proximity guides" for RMEditor. This feature draws lines indicating when
 * dragged shapes share alignments with some of their neighboring shapes (and also snaps to these alignments, too).
 */
public class RMEditorProxGuide {

    // Whether proximity guides are enabled.
    static boolean        _enabled = Prefs.get().getBoolean("ProximityGuide", false);
    
    // Whether bounds of parent shape are also check for proximity
    static boolean        _includeSuperSelectedShape = false;
    
    // The list of rects that need to be repainted for proximity guides
    static List <Rect>    _guidelineRects = new Vector();

/**
 * Returns whether proximity guides are enabled.
 */
public static boolean isEnabled()  { return _enabled; }

/**
 * Sets whether proximity guides are enabled.
 */
public static void setEnabled(boolean aFlag)
{
    _enabled=aFlag;
    Prefs.get().set("ProximityGuide", aFlag);
}

/**
 * Empties the guideline list and marks the old guideline region for repaint
 */
public static void clearGuidelines(RMEditor anEditor)
{
    markGuidelinesDirty(anEditor);
    _guidelineRects.clear();
}

/**
 * Runs through the guideline list and asks the editor to repaint the enclosing rect.
 */
public static void markGuidelinesDirty(RMEditor anEditor)
{
    // If no GuidelineRects, just return
    if(_guidelineRects.size()==0) return;

    // Get copy of first rect and union with successive rects
    Rect dirty = _guidelineRects.get(0).clone();
    for(int i=1; i<_guidelineRects.size(); i++)
        dirty.union(_guidelineRects.get(i));
    
    // Outset by 2 to cover stroke and repaint rect
    dirty.inset(-2, -2);
    anEditor.repaint(dirty);
}

/**
 * Called by the editor to paint all the guidelines in the guideline list
 */
public static void paintProximityGuides(RMEditor anEditor, Painter aPntr)
{
    // If no GuidelineRects, just return
    if(_guidelineRects.size()==0) return;
    
    // Set color to blue and stroke to 1.3pt dashed line
    aPntr.setColor(Color.BLUE);
    Stroke stroke = new Stroke(1.3, new float[] { 4,2 }, 0); aPntr.setStroke(stroke);

    // Draw proximity guide lines (with AntiAliasing on?)
    boolean aa = aPntr.setAntialiasing(true);
    for(Rect r : _guidelineRects) aPntr.drawLine(r.x, r.y, r.getMaxX(), r.getMaxY());
    aPntr.setAntialiasing(aa);
}

/**
 * If this flag is set, the bounds of the parent shape are also checked for proximity.
 */
public static void setIncludesSuperselectedShape(boolean aFlag)  { _includeSuperSelectedShape = aFlag; }

/**
 * Returns the list of shapes to be included in the proximity check.
 */
public static List <RMShape> getCandidateShapes(RMEditor anEditor)
{
    // Get super selected shape
    RMShape parent = anEditor.getSuperSelectedShape();
    if(parent.getChildCount()==0)
        return Collections.emptyList();
    
    // Get all peers of selected shapes
    List candidateShapes = new ArrayList(parent.getChildren());
    for(int i=0, iMax=anEditor.getSelectedShapeCount(); i<iMax; i++)
        ListUtils.removeId(candidateShapes, anEditor.getSelectedShape(i));
    
    // Optionally, also check against the bounds of the parent.
    // The "stepParent" is merely an empty shape whose bounds match the parent, but in the same coordinate
    // system as the other candidate shapes.
    if(_includeSuperSelectedShape) {
        RMShape stepParent = new RMShape(); stepParent.copyShape(parent);
        stepParent.setXY(0f, 0f);
        candidateShapes.add(stepParent);
    }
    
    // Return candidate shapes
    return candidateShapes;
}

/**
 * Calculate guidelines for the bounds of the selected shapes against all other superselected shapes.
 */
public static void createGuidelines(RMEditor anEditor)
{
    // If not in select tool drag move or resize, just return
    RMSelectTool.DragMode dragMode = anEditor.getSelectTool().getDragMode();
    if(dragMode!=RMSelectTool.DragMode.Move && dragMode!=RMSelectTool.DragMode.Resize)
        return;
    
    // If no selected shapes, just return
    if(anEditor.getSelectedShapeCount()==0)
        return;
    
    // Get parent of selected shapes (just return if structured table row)
    RMShape parent = anEditor.getSuperSelectedShape();
    if(parent instanceof RMTableRow && ((RMTableRow) parent).isStructured())
        return;
    
    // Get candidate shapes for editor
    List candidateShapes = getCandidateShapes(anEditor);
    
    // Get bounds
    Rect bounds = RMShapeUtils.getBoundsOfChildren(parent, anEditor.getSelectedShapes());
    
    // Create guidelines
    createGuidelines(anEditor, parent, bounds, candidateShapes);
}

/**
 * Recalculates all the proximity guides and marks dirty region in editor for old & new guide regions.
 * Guides are calculated between the bounds rectangle and each of the candidateShapes, within the parent RMShape.
 */
public static void createGuidelines(RMEditor anEditor, RMShape parent, Rect bounds, List candidateShapes)
{
    // If disabled, just return
    if(!_enabled) return;

    // Empty list and mark old guides dirty
    clearGuidelines(anEditor);

    // If no candidate shapes, just return
    if(candidateShapes==null || candidateShapes.isEmpty()) return;
    
    double minDX = 9999, maxDX = 9999, minDY = 9999, maxDY = 9999;
    RMShape minDXminYShape=null, minDXmaxYShape=null, maxDXminYShape=null, maxDXmaxYShape=null;
    RMShape minDYminXShape=null, minDYmaxXShape=null, maxDYminXShape=null, maxDYmaxXShape=null;
    double delta, x1, y1, x2, y2;
    Point p1, p2;

    // Iterate over children to see which is the closest to selectedShapes min/max X
    for(int i=0, iMax=candidateShapes.size(); i<iMax; i++) {

        // Get current child
        RMShape child = (RMShape) candidateShapes.get(i);

        delta=Math.abs(child.getFrameX() - bounds.x);
        if (delta < minDX) {
            minDX = delta;
            minDXminYShape = minDXmaxYShape=child;
        }
        else if(delta == minDX) {
            if(child.getFrameY() < minDXminYShape.getFrameY())
                minDXminYShape = child;
            if(child.getFrameMaxY() > minDXmaxYShape.getFrameMaxY())
                minDXmaxYShape = child;
        }

        delta = Math.abs(child.getFrameMaxX() - bounds.getMaxX());
        if (delta < maxDX) {
            maxDX = delta;
            maxDXminYShape = maxDXmaxYShape=child;
        }
        else if(delta == maxDX) {
            if(child.getFrameY() < maxDXminYShape.getFrameY())
                maxDXminYShape = child;
            if(child.getFrameMaxY() > maxDXmaxYShape.getFrameMaxY())
                maxDXmaxYShape = child;
        }

        delta=Math.abs(child.getFrameY() - bounds.y);
        if (delta < minDY) {
            minDY = delta;
            minDYminXShape=minDYmaxXShape=child;
        }
        else if(delta==minDY) {
            if(child.getFrameX() < minDYminXShape.getFrameX())
                minDYminXShape = child;
            if(child.getFrameMaxX() > minDYmaxXShape.getFrameMaxX())
                minDYmaxXShape = child;
        }

        delta = Math.abs(child.getFrameMaxY() - bounds.getMaxY());
        if(delta<maxDY) {
            maxDY = delta;
            maxDYminXShape = maxDYmaxXShape=child;
        }
        else if(delta==maxDY) {
            if(child.getFrameX() < maxDYminXShape.getFrameX())
                maxDYminXShape = child;
            if(child.getFrameMaxX() > maxDYmaxXShape.getFrameMaxX())
                maxDYmaxXShape = child;
        }
    }
    
    // Add any new guides to guidelines list
    if (minDX <= maxDX && minDX < 5) {
        x1 = minDXminYShape.getFrameX();
        y1 = Math.min(bounds.y, minDXminYShape.getFrameY());
        y2 = Math.max(bounds.getMaxY(), minDXmaxYShape.getFrameMaxY());
        p1 = anEditor.convertFromShape(x1, y1, parent);
        p2 = anEditor.convertFromShape(x1, y2, parent);
        addGuideline(p1, p2);
    }

    if (maxDX <= minDX && maxDX < 5) {
        x1 = maxDXminYShape.getFrameMaxX();
        y1 = Math.min(bounds.y, maxDXminYShape.getFrameY());
        y2 = Math.max(bounds.getMaxY(), maxDXmaxYShape.getFrameMaxY());
        p1 = anEditor.convertFromShape(x1, y1, parent);
        p2 = anEditor.convertFromShape(x1, y2, parent);
        addGuideline(p1, p2);
    }

    if (minDY <= maxDY && minDY < 5) {
        y1 = minDYminXShape.getFrameY();
        x1 = Math.min(bounds.x, minDYminXShape.getFrameX());
        x2 = Math.max(bounds.getMaxX(), minDYmaxXShape.getFrameMaxX());
        p1 = anEditor.convertFromShape(x1, y1, parent);
        p2 = anEditor.convertFromShape(x2, y1, parent);
        addGuideline(p1, p2);
    }
    
    if (maxDY <= minDY && maxDY < 5) {
        y1 = maxDYminXShape.getFrameMaxY();
        x1 = Math.min(bounds.x, maxDYminXShape.getFrameX());
        x2 = Math.max(bounds.getMaxX(), maxDYmaxXShape.getFrameMaxX());
        p1 = anEditor.convertFromShape(x1, y1, parent);
        p2 = anEditor.convertFromShape(x2, y1, parent);
        addGuideline(p1, p2);
    }

    markGuidelinesDirty(anEditor);
}

/**
 * Adds a guideline rect for the given points.
 */
private static void addGuideline(Point p1, Point p2)  { _guidelineRects.add(Rect.get(p1, p2)); }

/**
 * Returns the given point snapped to relevant proximity guides.
 */
public static Point pointSnappedToProximityGuides(RMEditor anEditor, Point aPoint)
{
    return pointSnappedToProximityGuides(anEditor,aPoint, anEditor.getSelectTool().getDragMode());
}

/**
 * Returns the given point snapped to relevant proxity guides for a given drag mode.
 */
public static Point pointSnappedToProximityGuides(RMEditor anEditor, Point aPoint, RMSelectTool.DragMode aDragMode)
{
    // If not enabled, just return point
    if(!_enabled)
        return aPoint;

    // If drag mode is not move or resize, just return point
    if(aDragMode!=RMSelectTool.DragMode.Move && aDragMode!=RMSelectTool.DragMode.Resize)
        return aPoint;

    // Get parent
    RMShape parent = anEditor.getSuperSelectedShape();
    
    // If parent is structured table row, just return point (wish this wasn't hard coded)
    if(parent instanceof RMTableRow && ((RMTableRow)parent).isStructured())
        return aPoint;
    
    // Get list of selected shapes
    List selectedShapes = anEditor.getSelectedShapes();
    
    // Get list of candidate shapes
    List candidateShapes = getCandidateShapes(anEditor);

    // Declare variable for bounds
    Rect bounds;

    // If mode is move, set bounds to snap the entire bounding box
    if(aDragMode==RMSelectTool.DragMode.Move)
        bounds = RMShapeUtils.getBoundsOfChildren(parent, selectedShapes);
    
    // If mode is resize, set bounds to just snap a handle
    else {
        bounds = new Rect(aPoint.x, aPoint.y, 0, 0);
        bounds = parent.parentToLocal(bounds, null).getBounds();
    }

    // Declare variables for minDX, maxDX, minDY and maxDY
    double minDX = 9999;
    double maxDX = 9999;
    double minDY = 9999;
    double maxDY = 9999;
    
    // Declare variables for minDX, maxDX, minDY, maxDY shapes
    RMShape minDXShape = null;
    RMShape maxDXShape = null;
    RMShape minDYShape = null;
    RMShape maxDYShape = null;

    // Iterate over children to see which is the closest to selectedShapes min/max X
    for (int i=0, iMax=candidateShapes.size(); i < iMax; i++) {

        // Get current child
        RMShape child = (RMShape)candidateShapes.get(i);

        double dx1 = Math.abs(child.getFrameX() - bounds.x);
        if(dx1 < minDX) {
            minDX = dx1;
            minDXShape = child;
        }

        double dx2 = Math.abs(child.getFrameMaxX() - bounds.getMaxX());
        if(dx2 < maxDX) {
            maxDX = dx2;
            maxDXShape = child;
        }

        double dy1 = Math.abs(child.getFrameY() - bounds.y);
        if(dy1 < minDY) {
            minDY = dy1;
            minDYShape = child;
        }

        double dy2 = Math.abs(child.getFrameMaxY() - bounds.getMaxY());
        if(dy2 < maxDY) {
            maxDY = dy2;
            maxDYShape = child;
        }
    }

    // If
    if(minDX <= maxDX && minDX < 5)
        aPoint.setX(aPoint.x - (bounds.getX() - minDXShape.getFrameX()));

    // If
    if(maxDX < minDX && maxDX < 5)
        aPoint.setX(aPoint.x - (bounds.getMaxX() - maxDXShape.getFrameMaxX()));

    // If
    if(minDY <= maxDY && minDY < 5)
        aPoint.setY(aPoint.y - (bounds.getY() - minDYShape.getFrameY()));

    // If
    if(maxDY < minDY && maxDY < 5)
        aPoint.setY(aPoint.y - (bounds.getMaxY() - maxDYShape.getFrameMaxY()));

    // Return point
    return aPoint;
}

}