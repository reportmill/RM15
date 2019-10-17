/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.RMColor;
import snap.gfx.*;

/**
 * This class generates (and represents) a RPG'd line graph area.
 */
class RMGraphRPGLine extends RMGraphRPGBar {

    // The min/max x value of line graph
    double        _minX = 0, _maxX = 1;

/**
 * Creates a bar graph maker.
 */
public RMGraphRPGLine(RMGraph aGraph, ReportOwner anRptOwner)  { super(aGraph, anRptOwner); _layered = true; }

/**
 * Adds the bars to the graph area.
 */
public void addBars()
{
    // Get graph attributes: Graph.Type, Graph.Draw3D, Graph.ColorItems
    RMGraph.Type graphType = _graph.getType();
    boolean draw3D = _graph.isDraw3D();
    boolean colorItems = _graph.isColorItems();
    
    // Iterate over series
    for(int i=0, iMax=getSeriesCount(); i<iMax; i++) { RMGraphSeries series = getSeries(i);
        
        // Create path and iterate over series items
        Path path = new Path();
        for(int j=0, jMax=series.getItemCount(); j<jMax; j++) {
            
            // Get bounds of bar chart bar and line graph point
            Rect barBounds = getBarBounds(i, j);
            double lineX = barBounds.getMidX();
            double lineY = barBounds.getMinY();
            
            // If first series point, do MoveTo, otherwise LineTo
            if(j==0) path.moveTo(lineX, lineY);
            else path.lineTo(lineX, lineY);
            
            // Set min/max x
            if(j==0) _minX = lineX;
            else if(j+1==jMax) _maxX = lineX;
        }
        
        // If area, close path
        if(graphType==RMGraph.Type.Area) {
            path.lineTo(_maxX, _graph.getHeight());
            path.lineTo(_minX, _graph.getHeight());
            path.close();
        }
        
        // Create line shape
        RMPolygonShape lineShape = new RMPolygonShape(path);
        
        // If area or 3D line, set fill color and stroke to black
        RMColor color = getColor(i);
        if(graphType==RMGraph.Type.Area || draw3D) {
            lineShape.setColor(color);
            lineShape.setStrokeColor(color.darker().darker());
        }
        
        // If 2D line, set stroke color and line width
        else { lineShape.setColor(null); lineShape.setStroke(color, 2); }
        
        // Set line bounds
        lineShape.setBounds(path.getBounds());
        
        // Add to graph view
        if(graphType==RMGraph.Type.Area || graphType==RMGraph.Type.Line) {
            _barShape.addBar(lineShape, i);
        }
        
        // Iterate over series items
        for(int j=0, jMax=series.getItemCount(); j<jMax; j++) { RMGraphSeries.Item seriesItem = series.getItem(j);
            
            // Get bounds of bar chart bar
            Rect barBounds = getBarBounds(i, j);
            
            // Get line graph point
            double lineX = barBounds.getMidX();
            double lineY = barBounds.getMinY();
            
            // Create new linePointShape
            RMShape linePointShape = new RMRectShape();
            
            // Get inset
            int inset = graphType==RMGraph.Type.Scatter? 4 : 2;
            
            // Set bounds
            linePointShape.setBounds(lineX - inset, lineY - inset, inset*2, inset*2);
            
            // Set bar color
            RMColor col = colorItems? getColor(j) : color;
            linePointShape.setColor(col);
            linePointShape.setStrokeColor(col.darker().darker());
            
            // Add line point shape
            if(graphType==RMGraph.Type.Scatter || (graphType==RMGraph.Type.Line && !draw3D))
                _barShape.addBar(linePointShape, i);
            
            // Set bar in series-value
            seriesItem.setBar(linePointShape);
        }
    }
}

}