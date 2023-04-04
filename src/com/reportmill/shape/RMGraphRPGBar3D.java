/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx3d.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.*;

/**
 * This class renders a bar graph in 3D.
 */
class RMGraphRPGBar3D extends RMScene3D implements RMGraphRPGBar.BarGraphShape {

    // The graph
    private RMGraph _graph;

    // Whether graph is vertical or not
    private boolean _vertical;

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

    /**
     * Creates a RMGraphRPGBar3D.
     */
    public RMGraphRPGBar3D(RMGraph aGraph)
    {
        // Set attributes
        _graph = aGraph;
        _vertical = _graph.isVertical();
        setBounds(_graph.getBounds());
        setOpacity(_graph.getOpacity());
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

    /** Returns the width of the bars. */
    //public double getBarWidth()  { return _barWidth; }

    /**
     * Rebuilds 3D representation of shapes from shapes list (called by layout manager).
     */
    protected void layoutImpl()
    {
        // Remove all existing children
        removeChildren();

        // Remove all scene children
        Scene3D scene = getScene();
        scene.removeChildren();

        // Get standard width, height, depth
        double width = getWidth();
        double height = getHeight();
        double depth = getDepth();

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

        // Get fill/stroke
        RMFill graphFill = _graph.getFill();
        RMStroke graphStroke = _graph.getStroke();
        Color boxColor = graphFill != null ? graphFill.getColor() : null;
        Stroke boxStroke = graphStroke != null ? graphStroke.snap() : null;
        Color boxStrokeColor = graphStroke != null ? graphStroke.getColor() : null;


        // Create back plane shape
        Path3D back = new Path3D();
        back.setColor(boxColor);
        back.setStroke(boxStroke);
        back.setStrokeColor(boxStrokeColor);
        back.setOpacity(.8f);
        back.setPainter(_backGridPainter);
        back.moveTo(0, 0, 0);
        back.lineTo(width, 0, 0);
        back.lineTo(width, height, 0);
        back.lineTo(0, height, 0);
        back.close();
        scene.addChild(back);

        // Create back plane shape
        Path3D front = new Path3D();
        front.setColor(boxColor);
        front.setStroke(boxStroke);
        front.setStrokeColor(boxStrokeColor);
        front.setOpacity(.8f);
        front.setPainter(_backGridPainter);
        front.moveTo(0, 0, depth);
        front.lineTo(0, height, depth);
        front.lineTo(width, height, depth);
        front.lineTo(width, 0, depth);
        front.close();
        scene.addChild(front);

        // Create left side path shape
        Path3D leftSide = new Path3D();
        leftSide.setColor(Color.LIGHTGRAY);
        leftSide.setStroke(Color.BLACK, 1);
        leftSide.setOpacity(.8f);
        leftSide.setPainter(_sideGridPainter);
        leftSide.moveTo(0, 0, 0);
        leftSide.lineTo(0, height, 0);
        leftSide.lineTo(0, height, depth);
        leftSide.lineTo(0, 0, depth);
        leftSide.close();
        scene.addChild(leftSide);

        // Create right side path shape
        Path3D rightSide = new Path3D();
        rightSide.setColor(Color.LIGHTGRAY);
        rightSide.setStroke(Color.BLACK, 1);
        rightSide.setOpacity(.8f);
        rightSide.setPainter(_sideGridPainter);
        rightSide.moveTo(width, 0, 0);
        rightSide.lineTo(width, 0, depth);
        rightSide.lineTo(width, height, depth);
        rightSide.lineTo(width, height, 0);
        rightSide.close();
        scene.addChild(rightSide);

        // Create floor path shape
        Path3D floor = new Path3D();
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

        // Do normal version
        super.layoutImpl();
    }
}