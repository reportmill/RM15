/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.Rect;

/**
 * This class renders a bar graph in 3D.
 */
class RMGraphShapeBar3D extends RMScene3D implements RMGraphRPGBar.BarGraphShape {
    
    // The graph
    RMGraph           _graph;
    
    // Whether graph is vertical or not
    boolean           _vertical;

    // The background color for the grid
    RMFill            _backgroundFill;
    
    // The background stroke for the grid
    RMStroke          _backgroundStroke;
    
    // Shapes for grid
    RMPath            _grid = new RMPath();
    
    // Shapes for the minor grid
    RMPath            _gridMinor = new RMPath();
    
    // The grid path without separators
    RMPath            _gridWithoutSep = new RMPath();
    
    // The bar width
    double            _barWidth = 0;
    
    // The list of bars
    List <Bar>        _bars = new ArrayList();
    
    // The list of bar labels
    List <RMShape>    _barLabels = new ArrayList();
    
    // The list of bar label types (synced with list above)
    List <RMGraphPartSeries.LabelPos>  _barLabelPositions = new ArrayList();
    
    // Axis label shapes
    List <RMShape>    _axisLabels = new ArrayList();
    
    // The number of layers
    int               _layerCount;
    
    // The z offset
    double            _offsetZ2 = 0;

/**
 * Creates a new graph bar view.
 */
public RMGraphShapeBar3D(RMGraph aGraph)
{
    // Set attributes
    _graph = aGraph; _vertical = _graph.isVertical();
    _backgroundFill = _graph.getFill(); _backgroundStroke = _graph.getStroke();
    setBounds(_graph.getBounds());
    setOpacity(_graph.getOpacity());
    copy3D(_graph.get3D());
}

/** Returns the RMGraphRPG. */
public RMGraphRPG getGraphRPG()  { return _grpg; } RMGraphRPG _grpg;

/** Sets the RMGraphRPG. */
public void setGraphRPG(RMGraphRPG aGRPG)  { _grpg = aGRPG; }

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
    if(_barWidth==0)
        _barWidth = _vertical? aBar.getWidth() : aBar.getHeight();
    
    // Add bar
    _bars.add(new Bar(aBar, aLayer));
    
    // Update layer count
    _layerCount = Math.max(_layerCount, aLayer+1);
}

/**
 * A class to represent a bar shape.
 */
private static class Bar {
    RMShape barShape;
    int layer;
    public Bar(RMShape aShape, int aLayer) { barShape = aShape; layer = aLayer; }
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
 * Adds the axis to the graph view.
 */
public void addAxis(RMShape aShape)  { addChild(aShape); }

/**
 * Adds the value axis label to the graph view.
 */
public void addValueAxisLabel(RMShape anAxisLabel)  { _axisLabels.add(anAxisLabel); }

/**
 * Adds the label axis label to the graph view.
 */
public void addLabelAxisLabel(RMShape anAxisLabel)  { _axisLabels.add(anAxisLabel); }

/**
 * Returns the width of the bars.
 */
public double getBarWidth()  { return _barWidth; }

/**
 * Returns bar graph's camera transform (overrides Scene3D to make pitch always relative to camera).
 */
public Transform3D getTransform3D()
{
    // If pseudo 3d, just use original implementation
    if(isPseudo3D())
        return super.getTransform3D();
    
    // Normal transform: 
    Transform3D t = new Transform3D();
    t.translate(-getWidth()/2, -getHeight()/2, -getDepth()/2);
    t.rotateY(getYaw());
    t.rotate(new Vector3D(1, 0, 0), getPitch());
    t.rotate(new Vector3D(0, 0, 1), getRoll3D());
    t.translate(0, 0, getOffsetZ() - _offsetZ2);
    t.perspective(getFocalLength());
    t.translate(getWidth()/2, getHeight()/2, getDepth()/2);
    
    // Return transform
    return t;
}

/**
 * Rebuilds 3D representation of shapes from shapes list (called by layout manager).
 */
protected void layoutImpl()
{
    // Remove all existing children
    removeChildren();

    // Cache and clear scene3D Z offset
    double offsetZ = getOffsetZ();
    setOffsetZ(0);
    
    // Clear bar view Z offset
    _offsetZ2 = 0;
    
    // Get bounding box in camera coords with no Z offset
    Path3D bbox = new Path3D();
    bbox.moveTo(0, 0, 0);
    bbox.lineTo(0, 0, getDepth());
    bbox.lineTo(getWidth(), 0, getDepth());
    bbox.lineTo(getWidth(), 0, 0);
    bbox.lineTo(0, 0, 0);
    bbox.lineTo(0, getHeight(), 0);
    bbox.lineTo(0, getHeight(), getDepth());
    bbox.lineTo(getWidth(), getHeight(), getDepth());
    bbox.lineTo(getWidth(), getHeight(), 0);
    bbox.close();
    bbox.transform(getTransform3D());
    
    // Get offset Z of graph view from bounding box
    _offsetZ2 = bbox.getZMin();

    // Restore original graph Z offset
    setOffsetZ(offsetZ);
    
    // Get depth of layers
    double layerDepth = getDepth()/_layerCount;
    
    // Calculate bar depth
    double barDepth = layerDepth/(1 + _graph.getBars().getBarGap());
    
    // Constrain bar depth to bar width
    barDepth = Math.min(barDepth, _barWidth);
    
    // If pseudo3d, depth should be layer depth
    if(isPseudo3D())
        barDepth = layerDepth;
    
    // Calcuate bar min/max
    double barMin = (layerDepth-barDepth)/2;
    double barMax = layerDepth - barMin;
    
    // Iterate over bars and add each bar shape at bar layer
    for(int i=0, iMax=_bars.size(); i<iMax; i++) { Bar bar = _bars.get(i);
    addChild3D(bar.barShape, barMin + bar.layer*layerDepth, barMax + bar.layer*layerDepth, false); }
    
    // Calculate whether back plane should be shifted to the front
    Vector3D backVector = new Vector3D(0, 0, -1).transform(getTransform3D());
    boolean shiftBack = backVector.isAligned(getCamera(), true);
    double backZ = shiftBack? 0 : getDepth();
    
    // Create back plane path
    Path3D back = new Path3D();
    back.moveTo(0, 0, backZ);
    back.lineTo(0, getHeight(), backZ);
    back.lineTo(getWidth(), getHeight(), backZ);
    back.lineTo(getWidth(), 0, backZ);
    back.close();
    back.transform(getTransform3D());
    if(!shiftBack)
        back.reverse();
    
    // Create back plane shape
    Shape3D back3d = new Shape3D(back);
    setFillAndStroke(back3d, _backgroundFill, _backgroundStroke, null);
    back3d.setOpacity(.8f);
    addChild(back3d);
    
    // Add _grid to back3d
    Path3D gpath3d = new Path3D(_grid, backZ);
    gpath3d.transform(getTransform3D());
    Shape3D grid3d = new Shape3D(gpath3d);
    grid3d.setXY(grid3d.x() - back3d.x(), grid3d.y() - back3d.y());
    grid3d.setStroke(new RMStroke());
    back3d.addChild(grid3d);
    
    // Add _gridMinor to back3d
    gpath3d = new Path3D(_gridMinor, backZ);
    gpath3d.transform(getTransform3D());
    Shape3D gridMinor3d = new Shape3D(gpath3d);
    gridMinor3d.setXY(gridMinor3d.x() - back3d.x(), gridMinor3d.y() - back3d.y());
    gridMinor3d.setStrokeColor(RMColor.lightGray);
    back3d.addChild(gridMinor3d);

    // Calculate whether side plane should be shifted to the right
    Vector3D sideVector = new Vector3D(1, 0, 0).transform(getTransform3D());
    boolean shiftSide = _vertical && !isPseudo3D() && sideVector.isAligned(getCamera(), true);
    double sideX = shiftSide? getWidth() : 0;
        
    // Create side path
    Path3D side = new Path3D();
    side.moveTo(sideX, 0, 0);
    side.lineTo(sideX, getHeight(), 0);
    side.lineTo(sideX, getHeight(), getDepth());
    side.lineTo(sideX, 0, getDepth());
    side.close();
    side.transform(getTransform3D());
    
    // For horizonatal bar charts, make sure the side panel always points towards the camera
    if(!_vertical || isPseudo3D())
        shiftSide = side.getNormal().isAway(getCamera(),true);

    // If side wasn't shifted, reverse it
    if(!shiftSide)
        side.reverse();   
    
    // Create side shape
    Shape3D side3d = new Shape3D(side);
    setColor(side3d, RMColor.lightGray);
    side3d.setStroke(new RMStroke());
    side3d.setOpacity(.8f);        
    addChild(side3d);
    
    // Create floor path
    Path3D floor = new Path3D();
    floor.moveTo(0, getHeight() + .5f, 0);
    floor.lineTo(getWidth(), getHeight() + .5f, 0);
    floor.lineTo(getWidth(), getHeight() + .5f, getDepth());
    floor.lineTo(0, getHeight() + .5f, getDepth());
    floor.close();
    floor.transform(getTransform3D());
    if(floor.getNormal().isAligned(getCamera(), true))
        floor.reverse();
    
    // Create floor shape
    Shape3D floor3d = new Shape3D(floor);
    setColor(floor3d, RMColor.lightGray);
    floor3d.setStroke(new RMStroke());
    floor3d.setOpacity(.8f);
    addChild(floor3d);
    
    // Determine whether side grid should be added to graph side or floor
    Shape3D sideGridBuddy = _vertical? side3d : floor3d;
    Rect gridWithoutSepBnds = _gridWithoutSep.getBounds(), gridMinorBnds = _gridMinor.getBounds();
    Rect gridRect = _vertical? new Rect(0, gridWithoutSepBnds.getY(), getDepth(), gridWithoutSepBnds.getHeight()) :
        new Rect(gridWithoutSepBnds.getX(), 0, gridWithoutSepBnds.getWidth(), getDepth());
    Rect gridMinorRect = _vertical? new Rect(0, gridMinorBnds.getY(), getDepth(), gridMinorBnds.getHeight()) :
        new Rect(gridMinorBnds.getX(), 0, gridMinorBnds.getWidth(), getDepth());
    Transform3D gridTrans = _vertical? new Transform3D().rotateY(-90).translate(sideX, 0, 0) :
        new Transform3D().rotateX(90).translate(0, getHeight(), 0);
    
    // Configure grid
    RMPath sideGridPath = _gridWithoutSep.getPathInRect(gridRect);
    Path3D sideGridPath3D = new Path3D(sideGridPath, 0);
    sideGridPath3D.transform(gridTrans);
    sideGridPath3D.transform(getTransform3D());
    Shape3D sideGrid3D = new Shape3D(sideGridPath3D);
    sideGrid3D.setXY(sideGrid3D.x() - sideGridBuddy.x(), sideGrid3D.y() - sideGridBuddy.y());
    sideGrid3D.setStroke(new RMStroke());
    sideGridBuddy.addChild(sideGrid3D);

    // Add _gridMinor to side3d
    sideGridPath = _gridMinor.getPathInRect(gridMinorRect);
    sideGridPath3D = new Path3D(sideGridPath, 0);
    sideGridPath3D.transform(gridTrans);
    sideGridPath3D.transform(getTransform3D());
    sideGrid3D = new Shape3D(sideGridPath3D);
    sideGrid3D.setXY(sideGrid3D.x() - sideGridBuddy.x(), sideGrid3D.y() - sideGridBuddy.y());
    sideGrid3D.setStrokeColor(RMColor.lightGray);
    sideGridBuddy.addChild(sideGrid3D);
    setFillAndStroke(sideGridBuddy, _backgroundFill, _backgroundStroke, null);
    
    // If no pseudo 3d, add axis and bar labels as 3d shapes
    if(!isPseudo3D()) {
         
        // Create axis label shapes
        for(int i=0, iMax=_axisLabels.size(); i<iMax && !getValueIsAdjusting(); i++)
            addChild3D(_axisLabels.get(i), -.1f, -.1f, false);
    
        // Create bar label shapes
        for(int i=0, iMax=_barLabels.size(); i<iMax && !getValueIsAdjusting(); i++) {
            
            // Get current loop bar label and bar label type
            RMShape barLabel = _barLabels.get(i);
            
            // Handle outside labels
            if(_barLabelPositions.get(i)==RMGraphPartSeries.LabelPos.Above ||
                _barLabelPositions.get(i)==RMGraphPartSeries.LabelPos.Below)
                addChild3D(barLabel, getDepth()/2, getDepth()/2, false);
    
            // Handle inside
            else addChild3D(barLabel, (getDepth() - _barWidth)/2 - 5, (getDepth() - _barWidth)/2 - 5, false);
        }
    }
          
    // Do 3d sort
    resort();
    
    // If Pseudo3d, add bar labels
    if(isPseudo3D()) {

        // Create axis label shapes
        for(int i=0, iMax=_axisLabels.size(); i<iMax && !getValueIsAdjusting(); i++)
            addChild(_axisLabels.get(i));
    
        // Create bar label shapes
        for(int i=0, iMax=_barLabels.size(); i<iMax && !getValueIsAdjusting(); i++)
            addChild(_barLabels.get(i));
    }
}

}