/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import snap.geom.*;
import snap.gfx.*;
import snap.view.*;

/**
 * This class manages creation and editing of polygon shapes.
 */
public class RMPolygonShapeTool<T extends RMPolygonShape> extends RMTool<T> {

    // The current path being added
    private Path2D _path;

    // Whether path should be smoothed on mouse up
    private boolean _smoothPathOnMouseUp;

    // Used to determine which path element to start smoothing from
    private int _pointCountOnMouseDown;

    // The point (in path coords) for new control point additions
    private Point _newPoint;

    // The path point handle hit by current mouse down
    public static int _selectedPointIndex = 0;

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
        RMPolygonShape polygonShape = getSelectedShape();
        Path2D path = polygonShape.getPath();

        // Update PathText
        setViewText("PathText", path.getSvgString());
    }

    /**
     * Handles the pop-up menu
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Get current PathView and path
        RMPolygonShape polygonShape = getSelectedShape();

        // Handle PathText
        if (anEvent.equals("PathText")) {
            String str = anEvent.getStringValue();
            Path2D path = new Path2D();
            path.appendSvgString(str);
            polygonShape.setPathAndBounds(path);
        }

        // Handle MakeSimpleButton
        if (anEvent.equals("MakeSimpleButton")) {
            Shape selShapePath = polygonShape.getPath();
            Shape simpleShape = Shape.getNotSelfIntersectingShape(selShapePath);
            polygonShape.setPathAndBounds(simpleShape);
        }

        // Handle DeletePointMenuItem
        if (anEvent.equals("DeletePointMenuItem"))
            deleteSelectedPoint();

        // Handle AddPointMenuItem
        if (anEvent.equals("AddPointMenuItem"))
            addPathPointAtPoint(_newPoint);
    }

    /**
     * Returns the class that this tool is responsible for.
     */
    public Class<T> getShapeClass()  { return (Class<T>) RMPolygonShape.class; }

    /**
     * Returns a new instance of the shape class that this tool is responsible for.
     */
    protected T newInstance()
    {
        T shape = super.newInstance();
        shape.setStroke(new RMStroke());
        return shape;
    }

    /**
     * Returns whether a given shape is super-selectable.
     */
    public boolean isSuperSelectable(RMShape aShape)  { return true; }

    /**
     * Returns whether tool should smooth path segments during creation.
     */
    public boolean getSmoothPath()  { return false; }

    /**
     * Handles mouse pressed for polygon creation.
     */
    public void mousePressed(ViewEvent anEvent)
    {
        boolean smoothPath = getSmoothPath();
        if (anEvent.isAltDown())
            smoothPath = !smoothPath;
        Point point = getEditorEvents().getEventPointInDoc(!smoothPath);

        // If this is the first mouseDown of a new path, create path and add moveTo
        if (_path == null) {
            _path = new Path2D();
            _path.moveTo(point.x, point.y);
        }

        // Otherwise add lineTo to current path
        else _path.lineTo(point.x, point.y);

        // Get the value of _shouldSmoothPathOnMouseUp for the mouseDrag and store current pointCount
        _smoothPathOnMouseUp = smoothPath;
        _pointCountOnMouseDown = _path.getPointCount();

        // Repaint editor
        getEditor().repaint();
    }

    /**
     * Handles mouse dragged for polygon creation.
     */
    public void mouseDragged(ViewEvent anEvent)
    {
        Point point = getEditorEvents().getEventPointInDoc(!_smoothPathOnMouseUp);
        Rect rect = _path.getBounds();

        // Update path
        if (_smoothPathOnMouseUp || _path.getPointCount() == 1)
            _path.lineTo(point.x, point.y);
        else _path.setPoint(_path.getPointCount() - 1, point.x, point.y);

        // Repaint editor
        rect.union(_path.getBounds());
        rect.inset(-10, -10);
        rect = getEditor().convertFromShape(rect, null).getBounds();
        getEditor().repaint(rect);
    }

    /**
     * Handles mouse released for polygon creation.
     */
    public void mouseReleased(ViewEvent anEvent)
    {
        if (_smoothPathOnMouseUp && _pointCountOnMouseDown < _path.getPointCount()) {
            getEditor().repaint();
            _path.fitToCurveFromPointIndex(_pointCountOnMouseDown);
        }

        // Check to see if point landed in first point
        if (_path.getPointCount() > 2) {
            Seg lastSeg = _path.getLastSeg();
            int lastPointIndex = _path.getPointCount() - (lastSeg == Seg.LineTo ? 2 : 4);
            Point beginPoint = _path.getPoint(0);
            Point lastPoint = _path.getPoint(lastPointIndex);
            Point thisPoint = _path.getLastPoint();
            Rect firstHandleRect = new Rect(beginPoint.x - 3, beginPoint.y - 3, 6f, 6f);
            Rect lastHandleRect = new Rect(lastPoint.x - 3, lastPoint.y - 3, 6f, 6f);
            Rect currentHandleRect = new Rect(thisPoint.x - 3, thisPoint.y - 3, 6f, 6f);
            boolean createPath = false;

            // If mouseUp is in startPoint, create poly and surrender to selectTool
            if (currentHandleRect.intersectsRect(firstHandleRect)) {
                if (lastSeg == Seg.LineTo)
                    _path.removeLastSeg();
                _path.close();
                createPath = true;
            }

            // If mouseUp is in startPoint, create poly and surrender to selectTool
            if (currentHandleRect.intersectsRect(lastHandleRect)) {
                if (_path.getLastSeg() == Seg.LineTo)
                    _path.removeLastSeg();
                createPath = true;
            }

            // Create poly, register for redisplay and surrender to selectTool
            if (createPath) {
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
        RMEditor editor = getEditor();
        Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), aPolygon);

        // If control point is hit, change cursor to move
        if (Path2DUtils.handleAtPoint(aPolygon.getPath(), point, _selectedPointIndex) >= 0) {
            editor.setCursor(Cursor.MOVE);
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
        if (!isSuperSelected(aPolygon)) return;

        // Get mouse down point in shape coords (but don't snap to the grid)
        Point point = getEditorEvents().getEventPointInShape(false);

        // Register shape for repaint
        aPolygon.repaint();

        // check for degenerate path
        if (aPolygon.getPath().getPointCount() < 2)
            _selectedPointIndex = -1;

        // Otherwise, figure out the size of a handle in path coordinates and set index of path point hit by mouse down
        else {
            int oldSelectedPt = _selectedPointIndex;
            int hp = Path2DUtils.handleAtPoint(aPolygon.getPath(), point, oldSelectedPt);
            _selectedPointIndex = hp;

            if (anEvent.isPopupTrigger()) {
                runContextMenu(aPolygon, anEvent);
                anEvent.consume();
            }
        }

        // Consume event
        anEvent.consume();
    }

    /**
     * Event handling for shape editing.
     */
    public void mouseDragged(T aPolygon, ViewEvent anEvent)
    {
        // If not dragging a point, just return
        if (_selectedPointIndex < 0) return;

        // Repaint, create path with moved point and set new path
        aPolygon.repaint();
        Point point = getEditorEvents().getEventPointInShape(true);
        Shape path = aPolygon.getPath();
        Path2D newPath = Path2DUtils.setPointSmoothly(path, _selectedPointIndex, point);
        aPolygon.setPathAndBounds(newPath);
    }

    /**
     * Actually creates a new polygon shape from the polygon tool's current path.
     */
    private void createPoly()
    {
        if (_path != null && _path.getPointCount() > 2) {
            RMEditor editor = getEditor();
            RMPolygonShape polygonShape = new RMPolygonShape();
            Rect polyFrame = editor.getSuperSelectedShape().parentToLocal(_path.getBounds(), null).getBounds();
            polygonShape.setFrame(polyFrame);
            polygonShape.setStroke(new RMStroke());
            polygonShape.setPath(_path);

            // Add shape to superSelectedShape (within an undo grouping).
            editor.undoerSetUndoTitle("Add Polygon");
            editor.getSuperSelectedParentShape().addChild(polygonShape);

            // Select Shape
            editor.setSelectedShape(polygonShape);
        }

        // Reset path
        _path = null;
    }

    /**
     * Overrides standard tool method to trigger polygon creation when the tool is deactivated.
     */
    public void deactivateTool()
    {
        createPoly();
    }

    /**
     * Overrides standard tool method to trigger polygon creation when the tool is reactivated.
     */
    public void reactivateTool()
    {
        createPoly();
    }

    /**
     * Editor method - called when an instance of this tool's shape in de-super-selected.
     */
    public void willLoseSuperSelected(T aShape)
    {
        super.willLoseSuperSelected(aShape);
        _selectedPointIndex = -1;
    }

    /**
     * Draws the polygon tool's path during path creation.
     */
    public void paintTool(Painter aPntr)
    {
        if (_path == null) return;
        aPntr.setColor(Color.BLACK);
        aPntr.setStroke(Stroke.Stroke1);
        aPntr.draw(_path);
    }

    /**
     * Handles painting a polygon shape.
     */
    public void paintHandles(T aPoly, Painter aPntr, boolean isSuperSelected)
    {
        // Do normal version (and just return if not super-selected)
        super.paintHandles(aPoly, aPntr, isSuperSelected);
        if (!isSuperSelected)
            return;

        // Get plygon path
        Path2D pathInLocal = aPoly.getPath();
        Shape shapeInEditor = aPoly.localToParent(pathInLocal, null);
        Path2D path = shapeInEditor instanceof Path2D ? (Path2D) shapeInEditor : new Path2D(shapeInEditor);
        Path2DUtils.paintHandles(path, aPntr, _selectedPointIndex);
    }

    /**
     * Runs a context menu for the given event.
     */
    private void runContextMenu(RMPolygonShape aPolyShape, ViewEvent anEvent)
    {
        // Get the handle that was clicked on
        Path2D path = aPolyShape.getPath();
        int pointIndex = _selectedPointIndex;
        String menuTitle = null;
        String menuName = null;

        // If clicked on a valid handle, add 'delete point' to menu,
        if (pointIndex >= 0) {
            if (Path2DUtils.isPointOnPath(path, pointIndex)) { // Only on-path points can be deleted
                menuTitle = "Delete Anchor Point";
                menuName = "DeletePointMenuItem";
            }
        }

        // Otherwise if the path itself was hit, use 'add point'
        else {
            // Convert event point to shape coords
            _newPoint = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aPolyShape);

            // linewidth is probably in shape coords, and might need to get transformed to path coords here
            if (path.intersects(_newPoint.x, _newPoint.y, Math.max(aPolyShape.getStrokeWidth(), 8))) {
                menuTitle = "Add Anchor Point";
                menuName = "AddPointMenuItem";
            }
        }

        // return if there's nothing to be done
        if (menuName == null) return;

        // Create new PopupMenu
        Menu contextMenu = new Menu();
        MenuItem menuItem = new MenuItem();
        menuItem.setText(menuTitle);
        menuItem.setName(menuName);
        contextMenu.addItem(menuItem);
        contextMenu.setOwner(this);
        contextMenu.show(anEvent.getView(), anEvent.getX(), anEvent.getY());
    }

    /**
     * Add a point to the curve by subdividing the path segment at the hit point.
     */
    private void addPathPointAtPoint(Point aPoint)
    {
        // Get old path and new path
        RMPolygonShape polygonShape = getSelectedShape();
        Path2D path = polygonShape.getPath();
        Path2D newPath = Path2DUtils.addPathPointAtPoint(path, aPoint);

        // If new path differs, set new path
        if (!newPath.equals(path))
            polygonShape.setPathAndBounds(newPath);
    }

    /**
     * Delete the selected control point and readjust shape bounds
     */
    private void deleteSelectedPoint()
    {
        // Get selected polygon shape and register for repaint
        RMPolygonShape polygonShape = getSelectedShape();
        polygonShape.repaint();

        // Get path and remove point
        Shape path = polygonShape.getPath();
        Path2D newPath = Path2DUtils.removePointAtIndexSmoothly(path, _selectedPointIndex);

        // If new path is valid, set in polygon shape
        if (newPath.getSegCount() > 0)  {
            getEditor().undoerSetUndoTitle("Delete Path Point");
            polygonShape.setPathAndBounds(newPath);
            _selectedPointIndex = -1;
        }

        // Otherwise complain
        else beep();
    }

    /**
     * Override to return extended rect when path is showing control points.
     */
    @Override
    public Rect getBoundsSuperSelected(T aShape)
    {
        Path2D path = aShape.getPath();
        Rect pathBounds = Path2DUtils.getControlPointBoundsWithSelPointIndex(path, _selectedPointIndex);
        pathBounds.inset(-3, -3);
        return pathBounds;
    }

    /**
     * This inner class defines a polygon tool subclass for drawing freehand pencil sketches instead.
     */
    public static class PencilTool<T extends RMPolygonShape> extends RMPolygonShapeTool<T> {

        /**
         * Constructor.
         */
        public PencilTool(RMEditor anEd)
        {
            setEditor(anEd);
        }

        /**
         * Overrides polygon tool method to flip default smoothing.
         */
        public boolean getSmoothPath()  { return true; }
    }
}