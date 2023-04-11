/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx3d.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.*;
import snap.props.PropChange;

/**
 * This class renders a bar graph in 3D.
 */
class RMGraphRPGBar3D extends RMScene3D implements RMGraphRPGBar.BarGraphShape {

    // The graph
    private RMGraph _graph;

    // Whether graph is vertical or not
    private boolean _vertical;

    // The graph fill
    private Paint _graphFill;

    // The graph border
    private Border _graphBorder;

    // The grid painter for back
    private Painter3D _backGridPainter;

    // The grid painter for side
    private Painter3D _sideGridPainter;

    // The bar width
    private double _barWidth = 0;

    // The list of bars
    private List<Bar> _bars = new ArrayList<>();

    // The list of bar labels
    private List<RMShape> _barLabels = new ArrayList<>();

    // The list of bar label types (synced with list above)
    private List<RMGraphPartSeries.LabelPos> _barLabelPositions = new ArrayList<>();

    // Axis label shapes
    private List<RMShape> _axisLabels = new ArrayList<>();

    // The number of layers
    private int _layerCount;

    // The GraphRPG
    private RMGraphRPG _graphRPG;

    // The axis box sides
    private Polygon3D _frontSide, _backSide, _leftSide, _rightSide;

    /**
     * Creates a RMGraphRPGBar3D.
     */
    public RMGraphRPGBar3D(RMGraph aGraph)
    {
        // Set attributes
        _graph = aGraph;
        _vertical = _graph.isVertical();
        RMFill graphFill = _graph.getFill();
        if (graphFill != null)
            _graphFill = graphFill.snap();
        RMStroke graphStroke = _graph.getStroke();
        if (graphStroke != null)
            _graphBorder = new Borders.LineBorder(graphStroke.getColor(), graphStroke.snap());
        setBounds(_graph.getBounds());
        setOpacity(_graph.getOpacity());
        setEffect(_graph.getEffect());
        copy3D(_graph.get3D());

        // Create Painter3Ds for back/side grid
        _backGridPainter = new Painter3D(getWidth(), getHeight());
        _sideGridPainter = new Painter3D(getDepth(), getHeight());
    }

    /**
     * Returns the RMGraphRPG.
     */
    public RMGraphRPG getGraphRPG()  { return _graphRPG; }

    /**
     * Sets the RMGraphRPG.
     */
    public void setGraphRPG(RMGraphRPG aGRPG)  { _graphRPG = aGRPG; }

    /**
     * Adds a major grid line to the graph view.
     */
    public void addGridLineMajor(RMLineShape aLine)
    {
        // Add line to BackGridPainter
        _backGridPainter.setColor(Color.BLACK);
        _backGridPainter.moveTo(aLine.getX(), aLine.getY());
        _backGridPainter.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());

        // Add line to SideGridPainter
        double scaleX = getDepth() / getWidth();
        _sideGridPainter.setColor(Color.BLACK);
        _sideGridPainter.moveTo(aLine.getX() * scaleX, aLine.getY());
        _sideGridPainter.lineTo(aLine.getFrameMaxX() * scaleX, aLine.getFrameMaxY());
    }

    /**
     * Adds a minor grid line to the graph view.
     */
    public void addGridLineMinor(RMLineShape aLine)
    {
        // Add line to BackGridPainter
        _backGridPainter.setColor(Color.LIGHTGRAY);
        _backGridPainter.moveTo(aLine.getX(), aLine.getY());
        _backGridPainter.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());

        // Add line to SideGridPainter
        double scaleX = getDepth() / getWidth();
        _sideGridPainter.setColor(Color.LIGHTGRAY);
        _sideGridPainter.moveTo(aLine.getX() * scaleX, aLine.getY());
        _sideGridPainter.lineTo(aLine.getFrameMaxX() * scaleX, aLine.getFrameMaxY());
    }

    /**
     * Adds a grid line separator to the graph view.
     */
    public void addGridLineSeparator(RMLineShape aLine)
    {
        // Add line to BackGridPainter
        _backGridPainter.setColor(Color.BLACK);
        _backGridPainter.moveTo(aLine.getX(), aLine.getY());
        _backGridPainter.lineTo(aLine.getFrameMaxX(), aLine.getFrameMaxY());
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

    /**
     * Rebuilds 3D representation of shapes from shapes list (called by layout manager).
     */
    protected void layoutImpl()
    {
        // Remove all existing children
        Scene3D scene = getScene();
        scene.removeChildren();

        // Builds the Axis box
        buildAxisBox();

        // Get Depth and LayerDepth
        double depth = getDepth();
        double layerDepth = depth / _layerCount;

        // Calculate bar depth (constrain to barWidth)
        double barDepth = layerDepth / (1 + _graph.getBars().getBarGap());
        barDepth = Math.min(barDepth, _barWidth);

        // Calculate bar min/max
        double barMin = (layerDepth - barDepth) / 2;
        double barMax = layerDepth - barMin;

        // Iterate over bars and add each bar shape at bar layer
        for (Bar bar : _bars) {
            addShapesForRMShape(bar.barShape, barMin + bar.layer * layerDepth, barMax + bar.layer * layerDepth, false);
        }

        // Create axis label shapes
        for (RMShape axisLabel : _axisLabels)
            addShapesForRMShape(axisLabel, depth + 1, depth + 1, false);

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
    }

    /**
     * Builds the Axis box.
     */
    protected void buildAxisBox()
    {
        // Remove all scene children
        Scene3D scene = getScene();

        // Get standard width, height, depth
        double width = getWidth();
        double height = getHeight();
        double depth = getDepth();

        // Get fill/stroke
        Color boxColor = _graphFill != null ? _graphFill.getColor() : null;
        Stroke boxStroke = _graphBorder != null ? _graphBorder.getStroke() : null;
        Color boxStrokeColor = _graphBorder != null ? _graphBorder.getColor() : null;

        // Create back plane shape
        _backSide = new Polygon3D(); _backSide.setName("GraphBoxBack");
        _backSide.setColor(boxColor);
        _backSide.setStroke(boxStroke);
        _backSide.setStrokeColor(boxStrokeColor);
        _backSide.setOpacity(.8f);
        _backSide.setPainter(_backGridPainter);
        _backSide.addPoint(0, 0, 0);
        _backSide.addPoint(width, 0, 0);
        _backSide.addPoint(width, height, 0);
        _backSide.addPoint(0, height, 0);
        scene.addChild(_backSide);

        // Create back plane shape
        _frontSide = new Polygon3D(); _frontSide.setName("GraphBoxFont");
        _frontSide.setColor(boxColor);
        _frontSide.setStroke(boxStroke);
        _frontSide.setStrokeColor(boxStrokeColor);
        _frontSide.setOpacity(.8f);
        _frontSide.setPainter(_backGridPainter);
        _frontSide.addPoint(0, 0, depth);
        _frontSide.addPoint(0, height, depth);
        _frontSide.addPoint(width, height, depth);
        _frontSide.addPoint(width, 0, depth);
        scene.addChild(_frontSide);

        // Create left side path shape
        _leftSide = new Polygon3D(); _leftSide.setName("GraphBoxLeft");
        _leftSide.setColor(Color.LIGHTGRAY);
        _leftSide.setStroke(Color.BLACK, 1);
        _leftSide.setOpacity(.8f);
        _leftSide.setPainter(_sideGridPainter);
        _leftSide.addPoint(0, 0, 0);
        _leftSide.addPoint(0, height, 0);
        _leftSide.addPoint(0, height, depth);
        _leftSide.addPoint(0, 0, depth);
        scene.addChild(_leftSide);

        // Create right side path shape
        _rightSide = new Polygon3D(); _rightSide.setName("GraphBoxRight");
        _rightSide.setColor(Color.LIGHTGRAY);
        _rightSide.setStroke(Color.BLACK, 1);
        _rightSide.setOpacity(.8f);
        _rightSide.setPainter(_sideGridPainter);
        _rightSide.addPoint(width, 0, 0);
        _rightSide.addPoint(width, 0, depth);
        _rightSide.addPoint(width, height, depth);
        _rightSide.addPoint(width, height, 0);
        scene.addChild(_rightSide);

        // Create floor path shape
        Path3D floor = new Path3D(); floor.setName("GraphBoxFloor");
        floor.setColor(Color.LIGHTGRAY);
        floor.setStroke(Color.BLACK, 1);
        floor.setOpacity(.8f);
        floor.setDoubleSided(true);
        floor.moveTo(0, -1, 0);
        floor.lineTo(width, -1, 0);
        floor.lineTo(width, -1, depth);
        floor.lineTo(0, -1, depth);
        floor.close();
        scene.addChild(floor);

        // Reset LeftSide visible
        resetSidesVisible();
    }

    /**
     * Override to set sides visible.
     */
    @Override
    protected void cameraDidPropChange(PropChange aPC)
    {
        super.cameraDidPropChange(aPC);
        resetSidesVisible();
    }

    /**
     * Override to set sides visible.
     */
    protected void resetSidesVisible()
    {
        if (_leftSide == null) return;

        // Calculate whether left side is visible
        Matrix3D sceneToCamera = _camera.getSceneToCamera();
        Vector3D leftSideNormal = _leftSide.getNormal();
        Vector3D leftSideNormalInCamera = sceneToCamera.transformVector(leftSideNormal);
        boolean leftSideVisible = _camera.isFacing(leftSideNormalInCamera);
        _leftSide.setVisible(leftSideVisible);
        _rightSide.setVisible(!leftSideVisible);

        // Calculate whether back side is visible
        Vector3D backSideNormal = _backSide.getNormal();
        Vector3D backSideNormalInCamera = sceneToCamera.transformVector(backSideNormal);
        boolean backSideVisible = _camera.isFacing(backSideNormalInCamera);
        _backSide.setVisible(backSideVisible);
        _frontSide.setVisible(!backSideVisible);
    }
}