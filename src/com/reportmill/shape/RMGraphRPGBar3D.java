/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.gfx3d.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.geom.Path;
import snap.geom.Rect;
import snap.gfx.*;

/**
 * This class renders a bar graph in 3D.
 */
class RMGraphRPGBar3D extends RMScene3D implements RMGraphRPGBar.BarGraphShape {

    // The graph
    RMGraph _graph;

    // Whether graph is vertical or not
    boolean _vertical;

    // The background color for the grid
    RMFill _backFill;

    // The background stroke for the grid
    RMStroke _backStroke;

    // Shapes for grid
    Path _grid = new Path();

    // Shapes for the minor grid
    Path _gridMinor = new Path();

    // The grid path without separators
    Path _gridWithoutSep = new Path();

    // The bar width
    double _barWidth = 0;

    // The list of bars
    List<Bar> _bars = new ArrayList<>();

    // The list of bar labels
    List<RMShape> _barLabels = new ArrayList<>();

    // The list of bar label types (synced with list above)
    List<RMGraphPartSeries.LabelPos> _barLabelPositions = new ArrayList<>();

    // Axis label shapes
    List<RMShape> _axisLabels = new ArrayList<>();

    // The number of layers
    int _layerCount;

    /**
     * Creates a RMGraphRPGBar3D.
     */
    public RMGraphRPGBar3D(RMGraph aGraph)
    {
        // Set attributes
        _graph = aGraph;
        _vertical = _graph.isVertical();
        _backFill = _graph.getFill();
        _backStroke = _graph.getStroke();
        setBounds(_graph.getBounds());
        setOpacity(_graph.getOpacity());
        copy3D(_graph.get3D());
        getCamera().setAdjustZ(true);
    }

    /**
     * Returns the RMGraphRPG.
     */
    public RMGraphRPG getGraphRPG()
    {
        return _grpg;
    }

    RMGraphRPG _grpg;

    /**
     * Sets the RMGraphRPG.
     */
    public void setGraphRPG(RMGraphRPG aGRPG)
    {
        _grpg = aGRPG;
    }

    /**
     * Adds a major grid line to the graph view.
     */
    public void addGridLineMajor(RMLineShape aLine)
    {
        _grid.moveTo(aLine.getX(), aLine.getY());
        _grid.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());
        _gridWithoutSep.moveTo(aLine.getX(), aLine.getY());
        _gridWithoutSep.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());
    }

    /**
     * Adds a minor grid line to the graph view.
     */
    public void addGridLineMinor(RMLineShape aLine)
    {
        _gridMinor.moveTo(aLine.getX(), aLine.getY());
        _gridMinor.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());
    }

    /**
     * Adds a grid line separator to the graph view.
     */
    public void addGridLineSeparator(RMLineShape aLine)
    {
        _grid.moveTo(aLine.getX(), aLine.getY());
        _grid.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());
    }

    /**
     * Adds a bar to the graph view.
     */
    public void addBar(RMShape aBar, int aLayer)
    {
        // Set bar width
        if (_barWidth == 0)
            _barWidth = _vertical ? aBar.getWidth() : aBar.getHeight();

        // Add bar
        _bars.add(new Bar(aBar, aLayer));

        // Update layer count
        _layerCount = Math.max(_layerCount, aLayer + 1);
    }

    /**
     * A class to represent a bar shape.
     */
    private static class Bar {
        RMShape barShape;
        int layer;

        public Bar(RMShape aShape, int aLayer)
        {
            barShape = aShape;
            layer = aLayer;
        }
    }

    /**
     * Adds a bar label to the graph view.
     */
    public void addBarLabel(RMShape aBarLabel, RMGraphPartSeries.LabelPos aPosition)
    {
        _barLabels.add(aBarLabel);
        _barLabelPositions.add(aPosition);
    }

    /**
     * Adds the axis to the graph view. Handled in layoutImp
     */
    public void addAxis(RMShape aShape)  { }

    /**
     * Adds the value axis label to the graph view.
     */
    public void addValueAxisLabel(RMShape anAxisLabel)
    {
        _axisLabels.add(anAxisLabel);
    }

    /**
     * Adds the label axis label to the graph view.
     */
    public void addLabelAxisLabel(RMShape anAxisLabel)
    {
        _axisLabels.add(anAxisLabel);
    }

    /** Returns the width of the bars. */
    //public double getBarWidth()  { return _barWidth; }

    /**
     * Rebuilds 3D representation of shapes from shapes list (called by layout manager).
     */
    protected void layoutImpl()
    {
        // Remove all existing children
        removeChildren();
        removeShapes();

        // Get standard width, height, depth
        double width = getWidth(), height = getHeight(), depth = getDepth();

        // Get depth of layers
        double layerDepth = depth / _layerCount;

        // Calculate bar depth
        double barDepth = layerDepth / (1 + _graph.getBars().getBarGap());

        // Constrain bar depth to bar width
        barDepth = Math.min(barDepth, _barWidth);

        // Calcuate bar min/max
        double barMin = (layerDepth - barDepth) / 2;
        double barMax = layerDepth - barMin;

        // Iterate over bars and add each bar shape at bar layer
        for (Bar bar : _bars) {
            addShapesForRMShape(bar.barShape, barMin + bar.layer * layerDepth, barMax + bar.layer * layerDepth, false);
        }

        // Calculate whether back plane should be shifted to the front. Back normal = { 0, 0,-1 }.
        Camera camera = getCamera();
        Scene3D scene = getScene();
        Vector3D backNormal = scene.localToCameraForVector(0, 0, -1);
        boolean shiftBack = camera.isFacingAway(backNormal);
        double backZ = shiftBack ? 0 : depth;

        // Create back plane shape
        Path3D back = new Path3D();
        back.setOpacity(.8f);
        if (_backFill != null) back.setColor(_backFill.getColor());
        if (_backStroke != null) back.setStroke(_backStroke.getColor(), _backStroke.getWidth());
        back.moveTo(0, 0, backZ);
        back.lineTo(0, height, backZ);
        back.lineTo(width, height, backZ);
        back.lineTo(width, 0, backZ);
        back.close();
        if (!shiftBack)
            back.reverse();
        addShape(back);

        // Add Grid to back
        Path3D grid = new Path3D(_grid, backZ);
        grid.setStroke(Color.BLACK, 1);
        back.addLayer(grid);

        // Add GridMinor to back
        Path3D gridMinor = new Path3D(_gridMinor, backZ);
        gridMinor.setStrokeColor(Color.LIGHTGRAY);
        back.addLayer(gridMinor);

        // Calculate whether side plane should be shifted to the right. Side normal = { 1, 0, 0 }.
        Vector3D sideNormal = scene.localToCameraForVector(1, 0, 0);
        boolean shiftSide = _vertical && camera.isFacingAway(sideNormal);
        double sideX = shiftSide ? width : 0;

        // Create side path shape
        Path3D side = new Path3D();
        side.setColor(Color.LIGHTGRAY);
        side.setStroke(Color.BLACK, 1);
        side.setOpacity(.8f);
        side.moveTo(sideX, 0, 0);
        side.lineTo(sideX, height, 0);
        side.lineTo(sideX, height, depth);
        side.lineTo(sideX, 0, depth);
        side.close();

        // Make sure the side panel always points towards the camera
        Path3D sideInCameraCoords = scene.localToCamera(side);
        Vector3D sideNormalInCameraCoords = sideInCameraCoords.getNormal();
        boolean sideFacingAway = camera.isFacingAway(sideNormalInCameraCoords);
        if (sideFacingAway)
            side.reverse();
        addShape(side);

        // Create floor path shape
        Path3D floor = new Path3D();
        floor.setColor(Color.LIGHTGRAY);
        floor.setStroke(Color.BLACK, 1);
        floor.setOpacity(.8f);
        floor.moveTo(0, height + .5, 0);
        floor.lineTo(width, height + .5, 0);
        floor.lineTo(width, height + .5, depth);
        floor.lineTo(0, height + .5, depth);
        floor.close();

        // Make sure the floor always points towards the camera
        Path3D floorInCameraCoords = scene.localToCamera(floor);
        Vector3D floorNormalInCameraCoords = floorInCameraCoords.getNormal();
        boolean floorFacingAway = camera.isFacingAway(floorNormalInCameraCoords);
        if (floorFacingAway)
            floor.reverse();
        addShape(floor);

        // Determine whether side grid should be added to graph side or floor
        Path3D sideGridBuddy = _vertical ? side : floor;
        Rect gridWithoutSepBnds = _gridWithoutSep.getBounds(), gridMinorBnds = _gridMinor.getBounds();
        Rect gridRect = _vertical ? new Rect(0, gridWithoutSepBnds.y, depth, gridWithoutSepBnds.height) :
                new Rect(gridWithoutSepBnds.x, 0, gridWithoutSepBnds.width, depth);
        Rect gridMinorRect = _vertical ? new Rect(0, gridMinorBnds.y, depth, gridMinorBnds.height) :
                new Rect(gridMinorBnds.x, 0, gridMinorBnds.width, depth);
        Transform3D gridTrans = _vertical ? new Transform3D().rotateY(-90).translate(sideX, 0, 0) :
                new Transform3D().rotateX(90).translate(0, height, 0);

        // Configure grid
        Path sideGridPath = _gridWithoutSep.copyFor(gridRect);
        Path3D sideGrid = new Path3D(sideGridPath, 0);
        sideGrid.transform(gridTrans);
        sideGrid.setStroke(Color.BLACK, 1);
        sideGridBuddy.addLayer(sideGrid);

        // Add GridMinor to side3d
        Path sideGridPathMinor = _gridMinor.copyFor(gridMinorRect);
        Path3D sideGridMinor = new Path3D(sideGridPathMinor, 0);
        sideGridMinor.transform(gridTrans);
        sideGridMinor.setStroke(Color.LIGHTGRAY, 1);
        sideGridBuddy.addLayer(sideGridMinor);
        if (_backFill != null)
            sideGridBuddy.setColor(_backFill.getColor());
        if (_backStroke != null)
            sideGridBuddy.setStroke(_backStroke.getColor(), _backStroke.getWidth());

        // Create axis label shapes
        for (RMShape axisLabel : _axisLabels)
            addShapesForRMShape(axisLabel, -.1f, -.1f, false);

        // Create bar label shapes
        for (int i = 0, iMax = _barLabels.size(); i < iMax; i++) {

            // Get current loop bar label and bar label type
            RMShape barLabel = _barLabels.get(i);

            // Handle outside labels
            if (_barLabelPositions.get(i) == RMGraphPartSeries.LabelPos.Above ||
                    _barLabelPositions.get(i) == RMGraphPartSeries.LabelPos.Below)
                addShapesForRMShape(barLabel, depth / 2, depth / 2, false);

                // Handle inside
            else addShapesForRMShape(barLabel, (depth - _barWidth) / 2 - 5, (depth - _barWidth) / 2 - 5, false);
        }

        // Do normal version
        super.layoutImpl();
    }
}