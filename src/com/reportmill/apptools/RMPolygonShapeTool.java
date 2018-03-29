/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.List;
import snap.gfx.*;
import snap.gfx.PathIter.Seg;
import snap.view.*;

/**
 * This class manages creation and editing of polygon shapes.
 */
public class RMPolygonShapeTool <T extends RMPolygonShape> extends RMTool <T> {
    
    // The current path being added
    RMPath       _path;
    
    // Whether path should be smoothed on mouse up
    boolean      _smoothPathOnMouseUp;
    
    // Used to determine which path element to start smoothing from
    int          _pointCountOnMouseDown;
    
    // The point (in path coords) for new control point additions
    Point        _newPoint;

/**
 * Initialize UI.
 */
protected void initUI()
{
    getView("PathText", TextView.class).setFireActionOnFocusLost(true);
}

/**
 * Reset UI.
 */
protected void resetUI()
{
    // Get current PathView and path
    RMPolygonShape pshape = getSelectedShape();
    Path path = pshape.getPath();
    
    // Update PathText
    setViewText("PathText", path.getString());
}

/**
 * Handles the pop-up menu
 */
public void respondUI(ViewEvent anEvent)
{
    // Get current PathView and path
    RMPolygonShape pshape = getSelectedShape();
    
    // Handle PathText
    if(anEvent.equals("PathText")) {
        String str = anEvent.getStringValue();
        Path path = Path.getPathFromSVG(str); if(path==null) return;
        pshape.resetPath(new RMPath(path));
    }
    
    // Handle DeletePointMenuItem
    if(anEvent.equals("DeletePointMenuItem"))
        deleteSelectedPoint();
    
    // Handle AddPointMenuItem
    if(anEvent.equals("AddPointMenuItem"))
        addNewPoint();
}

/**
 * Returns the class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMPolygonShape.class; }

/**
 * Returns a new instance of the shape class that this tool is responsible for.
 */
protected T newInstance()  { T shape = super.newInstance(); shape.setStroke(new RMStroke()); return shape; }

/**
 * Returns whether tool should smooth path segments during creation.
 */
public boolean getSmoothPath()  { return false; }

/**
 * Handles mouse pressed for polygon creation.
 */
public void mousePressed(ViewEvent anEvent)
{
    boolean smoothPath = getSmoothPath(); if(anEvent.isAltDown()) smoothPath = !smoothPath;
    Point point = getEditorEvents().getEventPointInDoc(!smoothPath);

    // Register all selectedShapes dirty because their handles will probably need to be wiped out
    getEditor().getSelectedShapes().forEach(i -> i.repaint());

    // If this is the first mouseDown of a new path, create path and add moveTo. Otherwise add lineTo to current path
    if(_path==null) { _path = new RMPath(); _path.moveTo(point); }
    else _path.lineTo(point);    

    // Get the value of _shouldSmoothPathOnMouseUp for the mouseDrag and store current pointCount
    _smoothPathOnMouseUp = smoothPath;
    _pointCountOnMouseDown = _path.getPointCount();

    Rect rect = _path.getBounds().getInsetRect(-10);
    rect = getEditor().convertFromShape(rect, null).getBounds();
    getEditor().repaint(rect);
}

/**
 * Handles mouse dragged for polygon creation.
 */
public void mouseDragged(ViewEvent anEvent)
{
    Point point = getEditorEvents().getEventPointInDoc(!_smoothPathOnMouseUp);
    Rect rect = _path.getBounds();

    if(_smoothPathOnMouseUp || _path.getPointCount()==1) _path.lineTo(point);
    else _path.setPoint(_path.getPointCount()-1, point.getX(), point.getY());

    rect.union(_path.getBounds()); rect.inset(-10, -10);
    rect = getEditor().convertFromShape(rect, null).getBounds();
    getEditor().repaint(rect);
}

/**
 * Handles mouse released for polygon creation.
 */
public void mouseReleased(ViewEvent anEvent)
{
    if(_smoothPathOnMouseUp && _pointCountOnMouseDown<_path.getPointCount()) {
        getEditor().repaint();
        _path.fitToCurve(_pointCountOnMouseDown);
    }

    // Check to see if point landed in first point
    if(_path.getPointCount() > 2) {
        Seg lastElmnt = _path.getSegLast();
        int lastPointIndex = _path.getPointCount() - (lastElmnt==Seg.LineTo? 2 : 4);
        Point beginPoint = _path.getPoint(0);
        Point lastPoint = _path.getPoint(lastPointIndex);
        Point thisPoint = _path.getPointLast();
        Rect firstHandleRect = new Rect(beginPoint.x - 3, beginPoint.y - 3, 6f, 6f);
        Rect lastHandleRect = new Rect(lastPoint.x - 3, lastPoint.y - 3, 6f, 6f);
        Rect currentHandleRect = new Rect(thisPoint.x - 3, thisPoint.y - 3, 6f, 6f);
        boolean createPath = false;

        // If mouseUp is in startPoint, create poly and surrender to selectTool
        if(currentHandleRect.intersectsRect(firstHandleRect)) {
            if(lastElmnt==Seg.LineTo) _path.removeLastSeg();
            _path.close();
            createPath = true;
        }

        // If mouseUp is in startPoint, create poly and surrender to selectTool
        if(currentHandleRect.intersectsRect(lastHandleRect)) {
            if(_path.getSegLast()==Seg.LineTo) _path.removeLastSeg();
            createPath = true;
        }
        
        // Create poly, register for redisplay and surrender to selectTool
        if(createPath) {
            createPoly();
            getEditor().repaint();
            getEditor().setCurrentToolToSelectTool();
        }
    }
}

/**
 * Event handling - overridden to maintain default cursor.
 */
public void mouseMoved(T aPolygon, ViewEvent anEvent)
{
    // Get the mouse down point in shape coords
    Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aPolygon);
    
    // If control point is hit, change cursor to move
    RMPath path = aPolygon.getPath(); Size size = new Size(9,9);
    if(handleAtPointForBounds(path, point, aPolygon.getBoundsInside(), RMPolygonShape._selectedPointIndex, size)>=0) {
        getEditor().setCursor(Cursor.MOVE);
        anEvent.consume();
    }
    
    // Otherwise, do normal mouse moved
    else super.mouseMoved(aPolygon, anEvent);
}

/**
 * Event handling for shape editing.
 */
public void mousePressed(T aPolygon, ViewEvent anEvent)
{
    // If shape isn't super selected, just return
    if(!isSuperSelected(aPolygon)) return;
    
    // Get mouse down point in shape coords (but don't snap to the grid)
    Point point = getEditorEvents().getEventPointInShape(false);
    
    // Register shape for repaint
    aPolygon.repaint();
    
    // check for degenerate path
    if(aPolygon.getPath().getPointCount() < 2) 
        RMPolygonShape._selectedPointIndex = -1;
    
    // Otherwise, figure out the size of a handle in path coordinates and set index of path point hit by mouse down
    else {
        Size handles = new Size(9,9);
        int oldSelectedPt = RMPolygonShape._selectedPointIndex;
        int hp = handleAtPointForBounds(aPolygon.getPath(), point, aPolygon.getBoundsInside(), oldSelectedPt, handles);
        RMPolygonShape._selectedPointIndex = hp;
    
        if(anEvent.isPopupTrigger())
            runContextMenu(aPolygon, anEvent);
    }
    
    // Consume event
    anEvent.consume();
}

/**
 * Event handling for shape editing.
 */
public void mouseDragged(T aPolygon, ViewEvent anEvent)
{
    aPolygon.repaint();
    if(RMPolygonShape._selectedPointIndex>=0) {
        Point point = getEditorEvents().getEventPointInShape(true);
        RMPath path = aPolygon.getPath();
        point = pointInPathCoordsFromPoint(path, point, aPolygon.getBoundsInside());
        
        // Clone path, move control point & do all the other path funny business, reset path
        RMPath newPath = path.clone();
        setPointStructured(newPath, RMPolygonShape._selectedPointIndex, point);
        aPolygon.resetPath(newPath);
    } 
}

/**
 * Actually creates a new polygon shape from the polygon tool's current path.
 */
private void createPoly()
{
    if(_path!=null && _path.getPointCount()>2) {
        RMPolygonShape poly = new RMPolygonShape();
        Rect polyFrame = getEditor().getSuperSelectedShape().getConvertedRectFromShape(_path.getBounds(), null);
        poly.setFrame(polyFrame);
        poly.setStroke(new RMStroke());
        poly.setPath(_path);

        // Add shape to superSelectedShape (within an undo grouping).
        getEditor().undoerSetUndoTitle("Add Polygon");
        getEditor().getSuperSelectedParentShape().addChild(poly);

        // Select Shape
        getEditor().setSelectedShape(poly);
    }

    // Reset path
    _path = null;
}

/**
 * Overrides standard tool method to trigger polygon creation when the tool is deactivated.
 */
public void deactivateTool()  { createPoly(); }

/**
 * Overrides standard tool method to trigger polygon creation when the tool is reactivated.
 */
public void reactivateTool()  { createPoly(); }

/**
 * Editor method - called when an instance of this tool's shape in de-super-selected.
 */
public void willLoseSuperSelected(T aShape)
{
    super.willLoseSuperSelected(aShape);
    RMPolygonShape._selectedPointIndex = -1;
}

/**
 * Draws the polygon tool's path during path creation.
 */
public void paintTool(Painter aPntr)
{
    if(_path==null) return;
    aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); aPntr.draw(_path);
}

/**
 * Returns the bounds for this shape when it's super-selected.
 */
public Rect getBoundsSuperSelected(T aShape) 
{
    // Get shape bounds and shape path bounds
    Rect bounds = aShape.getBoundsInside();
    Rect pathBounds = aShape.getPath().getBounds();

    // Get 
    double mx1 = pathBounds.getMidX(), my1 = pathBounds.getMidY();
    double mx2 = bounds.getMidX(), my2 = bounds.getMidY();
    double sx = pathBounds.width==0? 1f : bounds.width/pathBounds.width;
    double sy = pathBounds.height==0? 1f : bounds.height/pathBounds.height;

    // Scale pathSSBounds.origin by sx and sy and translate it to the bounding rect's origin
    Rect pathSSBounds = getControlPointBounds(aShape.getPath());
    double x = (pathSSBounds.x-mx1)*sx + mx2;
    double y = (pathSSBounds.y-my1)*sy + my2;
    double w = bounds.width*pathSSBounds.width/pathBounds.width;
    double h = bounds.height*pathSSBounds.height/pathBounds.height;
    
    // Get super selected bounds, outset a bit and return
    Rect ssbounds = new Rect(x,y,w,h); ssbounds.inset(-3, -3); return ssbounds;
}

/**
 * Returns the bounds for all the control points.
 */
private Rect getControlPointBounds(RMPath path)
{
    // Get segment index for selected control point handle
    int mouseDownIndex = path.getSegIndexForPointIndex(RMPolygonShape._selectedPointIndex);
    if(mouseDownIndex>=0 && path.getSeg(mouseDownIndex)==Seg.CubicTo &&
        (path.getSegPointIndex(mouseDownIndex) == RMPolygonShape._selectedPointIndex))
        mouseDownIndex--;

    // Iterate over path elements
    Point p0 = path.getPointCount()>0? new Point(path.getPoint(0)) : new Point();
    double p1x = p0.x, p1y = p0.y, p2x = p1x, p2y = p1y;
    PathIter piter = path.getPathIter(null); double pts[] = new double[6];
    for(int i=0; piter.hasNext(); i++) switch(piter.getNext(pts)) {
        
        // Handle MoveTo
        case MoveTo:
            
        // Handle LineTo
        case LineTo: {
            p1x = Math.min(p1x, pts[0]); p1y = Math.min(p1y, pts[1]);
            p2x = Math.max(p2x, pts[0]); p2y = Math.max(p2y, pts[1]);
        } break;
        
        // Handle CubicTo
        case CubicTo: {
            if((i-1)==mouseDownIndex) {
                p1x = Math.min(p1x, pts[0]); p1y = Math.min(p1y, pts[1]);
                p2x = Math.max(p2x, pts[0]); p2y = Math.max(p2y, pts[1]);
            }
            if(i==mouseDownIndex) {
                p1x = Math.min(p1x, pts[2]); p1y = Math.min(p1y, pts[3]);
                p2x = Math.max(p2x, pts[2]); p2y = Math.max(p2y, pts[3]);
            }
            p1x = Math.min(p1x, pts[4]); p1y = Math.min(p1y, pts[5]);
            p2x = Math.max(p2x, pts[4]); p2y = Math.max(p2y, pts[5]);
        } break;
        
        // Handle default
        default: break;
    }
    
    // Create control point bounds rect, union with path bounds and return
    Rect cpbounds = new Rect(p1x, p1y, Math.max(1, p2x - p1x), Math.max(1, p2y - p1y));
    cpbounds.union(path.getBounds()); return cpbounds;
}

/**
 * Runs a context menu for the given event.
 */
public void runContextMenu(RMPolygonShape aPolyShape, ViewEvent anEvent)
{
    // Get the handle that was clicked on
    RMPath path = aPolyShape.getPath();
    int pindex = RMPolygonShape._selectedPointIndex;
    String mtitle = null, mname = null;
    
    // If clicked on a valid handle, add 'delete point' to menu, 
    if(pindex>=0) {
        if(pointOnPath(path, pindex)) { // Only on-path points can be deleted
            mtitle = "Delete Anchor Point"; mname ="DeletePointMenuItem"; }
    }
    
    // Otherwise if the path itself was hit, use 'add point'
    else {
        // Convert event point to shape coords
        _newPoint = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aPolyShape);
        
        // linewidth is probably in shape coords, and might need to get transformed to path coords here
        if(path.intersects(_newPoint.getX(), _newPoint.getY(), Math.max(aPolyShape.getStrokeWidth(),8))) {
            mtitle = "Add Anchor Point"; mname = "AddPointMenuItem"; }
    }
    
    // return if there's nothing to be done
    if(mname==null) return;
    
    // Create new PopupMenu
    Menu pmenu = new Menu();
    MenuItem mitem = new MenuItem(); mitem.setText(mtitle); mitem.setName(mname); pmenu.addItem(mitem);
    pmenu.setOwner(this);
    pmenu.show(anEvent.getView(), anEvent.getX(), anEvent.getY());
}

/**
 * Delete the selected control point and readjust shape bounds
 */
public void deleteSelectedPoint()
{
    // Make changes to a clone of the path so deletions can be undone
    RMPolygonShape p = getSelectedShape();
    RMPath path = p.getPath().clone();

    // get the index of the path segment corresponding to the selected control point
    int sindex = path.getSegIndexForPointIndex(RMPolygonShape._selectedPointIndex);

    // mark for repaint & undo
    p.repaint();

    // Delete the point from path in parent coords
    path.removeSeg(sindex);

    // if all points have been removed, delete the shape itself
    if (path.getSegCount()==0) {
        getEditor().undoerSetUndoTitle("Delete Shape");
        p.getParent().repaint();
        p.removeFromParent();
        getEditor().setSelectedShape(null);
    }
    
    // otherwise update path and bounds and deselect the deleted point
    else {
        getEditor().undoerSetUndoTitle("Delete Control Point");
        p.resetPath(path);
        RMPolygonShape._selectedPointIndex = -1;
    }
}

/**
 * Add a point to the curve by subdividing the path segment at the hit point.
 */
public void addNewPoint()
{
    // Get all the segments as a list of subpaths
    RMPolygonShape polygon = getSelectedShape();
    List <List<RMLine>> subpaths = (List)polygon.getPath().getSubpathsSegments();
    
    // Find hitInfo of segment by intersecting with either horizontal or vertial line segment
    RMLine hor = new RMLine(_newPoint.getX()-2, _newPoint.getY(), _newPoint.getX()+2, _newPoint.getY());
    RMLine vert = new RMLine(_newPoint.getX(), _newPoint.getY()-2, _newPoint.getX(), _newPoint.getY()+2);
    
    // Iterate over subpaths
    for(int i=0, iMax=subpaths.size(); i<iMax; i++) { List <RMLine> subpath = subpaths.get(i);
    
        // Iterate over subpath segments
        for(int j=0, jMax=subpath.size(); j<jMax; j++) { RMLine segment = subpath.get(j);
        
            // Get hit info for segment
            RMHitInfo hit = segment.getHitInfo(hor);
            if (hit==null)
                hit = segment.getHitInfo(vert);
            
            // If hit found, subdivide segment at hit point and create new path
            if(hit != null) {
                
                // get parametric hit point for segment
                double hitPoint = hit.getR();
                
                // readjust the hit segment's endpoint
                RMLine tailSeg = segment.clone();
                segment.setEnd(hitPoint);
                
                // Set the start of the new tail to the hit point & insert into the list
                tailSeg.setStart(hitPoint);
                subpath.add(j+1, tailSeg);

                // Create new path and add subpaths
                RMPath newPath = new RMPath();
                for(int k=0, kMax=subpaths.size(); k<kMax; k++)
                    newPath.addSegments(subpaths.get(k));
                
                polygon.repaint();
                polygon.resetPath(newPath); //p._mouseDownPointIndex = ??
                return;
            }
        }
    }
}

/**
 * Resets the point at the given index to the given point, while preserving something.
 */
private static void setPointStructured(Path aPath, int index, Point point)
{
    int elmtIndex = aPath.getSegIndexForPointIndex(index);
    Seg elmt = aPath.getSeg(elmtIndex);

    // If point at index is part of a curveto, perform structured set
    if(elmt==Seg.CubicTo) {
        int pointIndexForElementIndex = aPath.getSegPointIndex(elmtIndex);

        // If point index is control point 1, and previous element is a curveto, bring control point 2 of previous curveto in line
        if(index - pointIndexForElementIndex == 0) {
            if(elmtIndex-1>0 && aPath.getSeg(elmtIndex-1)==Seg.CubicTo) {
                Point endPoint = aPath.getPoint(index-1), cntrlPnt2 = aPath.getPoint(index-2);
                // endpoint==point winds up putting a NaN in the path 
                if (!endPoint.equals(point)) {
                    Size size = new Size(point.getX() - endPoint.getX(), point.getY() - endPoint.getY());
                    size.normalize(); size.negate();
                    Size size2 = new Size(cntrlPnt2.getX() - endPoint.getX(), cntrlPnt2.getY() - endPoint.getY());
                    double mag = size2.getMagnitude();
                    aPath.setPoint(index-2, endPoint.getX() + size.getWidth()*mag, endPoint.getY() + size.getHeight()*mag);
                }
                else {
                    // Illustrator pops the otherControlPoint here to what it was at the 
                    // start of the drag loop.  Not sure that's much better...
                }
            }
        }

        // If point index is control point 2, and next element is a curveto, bring control point 1 of next curveto in line
        else if(index - pointIndexForElementIndex == 1) {
            if(elmtIndex+1<aPath.getSegCount() && aPath.getSeg(elmtIndex+1)==Seg.CubicTo) {
                Point endPoint = aPath.getPoint(index+1), otherControlPoint = aPath.getPoint(index+2);
                // don't normalize a point
                if (!endPoint.equals(point)) {
                    Size size = new Size(point.getX() - endPoint.x, point.getY() - endPoint.y);
                    size.normalize(); size.negate();
                    Size size2 = new Size(otherControlPoint.x - endPoint.x, otherControlPoint.y - endPoint.y);
                    double mag = size2.getMagnitude();
                    aPath.setPoint(index+2, endPoint.x+size.width*mag, endPoint.y + size.height*mag);
                }
                else { }
            }
        }

        // If point index is curve end point, move the second control point by the same amount as main point move
        else if(index - pointIndexForElementIndex == 2) {
            Point p1 = new Point(point); p1.subtract(aPath.getPoint(index));
            Point p2 = new Point(aPath.getPoint(index-1)); p2.add(p1);
            aPath.setPoint(index-1, p2.getX(), p2.getY());
            if(elmtIndex+1<aPath.getSegCount() && aPath.getSeg(elmtIndex+1)==Seg.CubicTo) {
                p1 = new Point(point); p1.subtract(aPath.getPoint(index));
                p2 = new Point(aPath.getPoint(index+1)); p2.add(p1);
                aPath.setPoint(index+1, p2.getX(), p2.getY());
            }
        }
    }

    // If there is a next element and it is a curveto, move its first control point by the same amount as main point move
    else if(elmtIndex+1<aPath.getSegCount() && aPath.getSeg(elmtIndex+1)==Seg.CubicTo) {
        Point p1 = new Point(point); p1.subtract(aPath.getPoint(index));
        Point p2 = new Point(aPath.getPoint(index+1)); p2.add(p1);
        aPath.setPoint(index+1, p2.getX(), p2.getY());
    }

    // Set point at index to requested point
    aPath.setPoint(index, point.getX(), point.getY());
}

/**
 * Returns the handle index for a given point against this path scaled to the given rect.
 * Only returns points that are on the path, except for the control points of
 * selectedPoint (if not -1)
 */
private static int handleAtPointForBounds(Path aPath, Point aPoint, Rect aRect, int selectedPoint, Size handleSize)
{
    // convert point from shape coords to path coords
    Point point = pointInPathCoordsFromPoint(aPath, aPoint, aRect);

    // Check against off-path control points of selected path first, otherwise you might never be able to select one
    if(selectedPoint != -1) {
        int offPathPoints[]=new int[2];
        int noffPathPoints=0;
        int ecount = aPath.getSegCount();
        int eindex = getSegIndexForPointIndex(aPath, selectedPoint);
        Seg elmt = aPath.getSeg(eindex);
        
        // If the selected point is one of the on path points, figure out the indices of the others
        if(pointOnPath(aPath, selectedPoint)) {
            
            // If the selected element is a curveto or quadto, the second to the last control point will be active
            if(elmt==Seg.CubicTo || elmt==Seg.QuadTo)
                offPathPoints[noffPathPoints++] = selectedPoint-1;

            // If the element following the selected element is a curveto, it's first control point will be active
            if (eindex<ecount-1 && aPath.getSeg(eindex+1)==Seg.CubicTo)
                offPathPoints[noffPathPoints++] = selectedPoint+1;
        }
        
        // If selected point is off-path, add it to list to check and then figure out what other point might be active
        else {
            offPathPoints[noffPathPoints++] = selectedPoint;
            
            // if selected point is first control point, check previous segment, otherwise check next segment
            if (selectedPoint == aPath.getSegPointIndex(eindex)) {
                if(eindex>0 && aPath.getSeg(eindex-1)==Seg.CubicTo)
                    offPathPoints[noffPathPoints++] = selectedPoint-2;
            }
            else {
                if(eindex<ecount-1 && aPath.getSeg(eindex+1)==Seg.CubicTo) 
                    offPathPoints[noffPathPoints++] = selectedPoint+2;
            }
        }
        
        // hit test any selected off-path handles
        for(int i=0; i<noffPathPoints; ++i)
            if(hitHandle(aPath, point, offPathPoints[i], handleSize))
                return offPathPoints[i];
    }
    
    // Check the rest of the points, but only ones that are actually on the path
    for(int i=0, iMax=aPath.getPointCount(); i<iMax; i++)
        if(hitHandle(aPath, point, i, handleSize) && pointOnPath(aPath, i))
            return i;

    // nothing hit
    return -1;
}
        
/**
 * Hit test the point (in path coords) against a given path point.
 */
private static boolean hitHandle(Path aPath, Point aPoint, int ptIndex, Size handleSize)
{
    Point p = aPath.getPoint(ptIndex);
    Rect br = new Rect(p.x-handleSize.width/2, p.y-handleSize.height/2, handleSize.width, handleSize.height);
    return br.contains(aPoint.getX(), aPoint.getY());
}

/**
 * Returns the given point converted to path coords for given path bounds.
 */
private static Point pointInPathCoordsFromPoint(Path aPath, Point aPoint, Rect aRect)
{
    Rect bounds = aPath.getBounds();
    double sx = bounds.getWidth()/aRect.getWidth();
    double sy = bounds.getHeight()/aRect.getHeight();
    double x = (aPoint.getX()-aRect.getMidX())*sx + bounds.getMidX();
    double y = (aPoint.getY()-aRect.getMidY())*sy + bounds.getMidY();
    return new Point(x,y);
}

/**
 * Returns the element index for the given point index.
 */
private static int getSegIndexForPointIndex(Path aPath, int index)
{
    // Iterate over segments and increment element index
    int elementIndex = 0;
    for(int pointIndex=0; pointIndex<=index; elementIndex++)
        switch(aPath.getSeg(elementIndex)) {
            case MoveTo:
            case LineTo: pointIndex++; break;
            case QuadTo: pointIndex += 2; break;
            case CubicTo: pointIndex += 3; break;
            default: break;
        }
    
    // Return calculated element index
    return elementIndex - 1;
}

/**
 * Returns true of the point at pointIndex is on the path, and false if it is on the convex hull.
 */ 
private static boolean pointOnPath(Path aPath, int pointIndex)
{
    int sindex = getSegIndexForPointIndex(aPath, pointIndex);
    int indexInElement = pointIndex - aPath.getSegPointIndex(sindex);
    
    // Only the last point is actually on the path
    Seg seg = aPath.getSeg(sindex);
    int numPts = seg.getCount();
    return indexInElement==numPts-1;
}

/**
 * This inner class defines a polygon tool subclass for drawing freehand pencil sketches instead.
 */
public static class PencilTool extends RMPolygonShapeTool {

    /** Creates a new PencilTool. */
    public PencilTool(RMEditor anEd)  { setEditor(anEd); }
    
    /** Overrides polygon tool method to flip default smoothing. */
    public boolean getSmoothPath()  { return true; }
}

}