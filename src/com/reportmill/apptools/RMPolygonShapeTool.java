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
import static com.reportmill.apptools.RMPolygonShapeUtils.*;

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
 * Override to return empty panel.
 */
protected View createUI()  { return new Label(); }

/**
 * Handles the pop-up menu
 */
public void respondUI(ViewEvent anEvent)
{
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
 * Draws the polygon tool's path durring path creation.
 */
public void paintTool(Painter aPntr)
{
    if(_path!=null) {
        Rect pbounds = getEditor().getPageBounds();
        aPntr.translate(pbounds.getX(), pbounds.getY());
        aPntr.scale(getEditor().getZoomFactor(), getEditor().getZoomFactor());
        aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); aPntr.draw(_path);
        aPntr.scale(1/getEditor().getZoomFactor(), 1/getEditor().getZoomFactor());
        aPntr.translate(-pbounds.getX(), -pbounds.getY());
    }
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
 * This inner class defines a polygon tool subclass for drawing freehand pencil sketches instead.
 */
public static class PencilTool extends RMPolygonShapeTool {

    /** Creates a new PencilTool. */
    public PencilTool(RMEditor anEd)  { setEditor(anEd); }
    
    /** Overrides polygon tool method to flip default smoothing. */
    public boolean getSmoothPath()  { return true; }
}

}