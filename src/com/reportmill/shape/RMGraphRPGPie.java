/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.Rect;
import snap.util.MathUtils;

/**
 * This class generates (and represents) a RPG'd pie graph area.
 */
class RMGraphRPGPie extends RMGraphRPG {
    
    // The shape that hold pie graph
    PieGraphShape       _pieShape;

/**
 * Creates a pie graph area for the given graph area and graphRPG.
 */
public RMGraphRPGPie(RMGraph aGraph, ReportOwner anRptOwner)
{
    // Do normal version
    super(aGraph, anRptOwner);
    
    // Set pie shape and configure
    _pieShape = (PieGraphShape)getGraphShape();
    configure();
}

/**
 * Creates the graph shape.
 */
protected RMParentShape createGraphShape()
{
    return _graph.isDraw3D()? new PieGraphShape3D(_graph) : new PieGraphShape2D(_graph);
}

/**
 * Override to force pie charts to be meshed if only one key.
 */
public boolean isMeshed()  { if(_graph.getKeyCount()==1) return false; return super.isMeshed(); }

/**
 * This is the workhorse method that really configures the graph area.
 */
protected void configure()
{
    // Get Graph, graph bounds
    RMGraph graph = _graph;
    Rect bounds = graph.getBoundsInside();
    
    // Get Pie shape and info
    RMGraphPartPie pieShape = graph.getPie();
    boolean drawWedgeLabelLines = pieShape.getDrawWedgeLabelLines();
    String extrusionKey = pieShape.getExtrusionKey();
    double holeRatio = pieShape.getHoleRatio();

    // Get LabelAxis
    RMGraphPartLabelAxis labelAxis = graph.getLabelAxis();
    
    // Get wedge label string
    RMXString barLabelString = null;
    if(labelAxis.getShowAxisLabels()) {
        String itemKey = labelAxis.getItemKey();
        barLabelString = new RMXString(itemKey, labelAxis.getFont());
        barLabelString.setParagraph(RMParagraph.CENTERED, 0, barLabelString.length());
    }
    
    // Get wedge prototype
    RMOvalShape prototype = new RMOvalShape();
    prototype.setHoleRatio(holeRatio);
    prototype.setStrokeColor(RMColor.black);
    
    // Iterate over each graph section and add individual pies
    for(int i=0, iMax=getSectionCount(); i<iMax; i++) { RMGraphSection section = getSection(i);
        
        // Get pie bounds
        Rect totalBounds = getPieBounds(i, iMax, bounds);
        
        // Declare variable for pie start angle and current sweep
        float startAngle = -90;
        float sweep = 0;

        // Declare variable for pie bounds
        Rect pieBounds = totalBounds.getInsetRect(5);

        // If there are wedge labels, reset pieBounds to totalBounds inset by 1/5 width
        if(barLabelString!=null)
            pieBounds = totalBounds.getInsetRect(totalBounds.getWidth()/10, totalBounds.getHeight()/10);
        
        // if there are extruded pie wedges, pieBounds is totalBounds inset another 10%
        if(!extrusionKey.equals(RMGraphPartPie.EXTRUDE_NONE))
            pieBounds.inset(pieBounds.getWidth()/20);
            
        // Round pieBounds down to largest square in rect
        pieBounds = squareRectInRect(pieBounds);
        
        // Get total value of section
        float total = section.getSectionTotal();

        // If total is zero, then we can't draw wedges. Just add oval and continue
        if(total==0) {
            RMOvalShape oval = new RMOvalShape();
            oval.setHoleRatio(holeRatio);
            oval.setFrame(pieBounds);
            _pieShape.addWedge(oval);
            continue;
        }

        // Iterate over section items and add wedges
        for(int j=0, jMax=section.getItemCount(); j<jMax; j++) { RMGraphSection.Item sectionItem = section.getItem(j);
            
            // Get wedge value and wedge group
            float value = sectionItem.getFloatValue();
            RMGroup group = sectionItem.getGroup();
            
            // Update start angle by last sweep and calculate sweep
            startAngle += sweep;
            sweep = value/total*360;
            
            // Create new oval shape for wedge
            _rptOwner.pushDataStack(group);
            RMOvalShape wedge = (RMOvalShape)_rptOwner.rpg(prototype, _graphShape);
            _rptOwner.popDataStack();
            
            // Set wedge bounds to pie bounds and set start angle and sweep
            wedge.setBounds(pieBounds);
            wedge.setStartAngle(startAngle);
            wedge.setSweepAngle(sweep);
            
            // if wedge should be extruded, calculate proper bounds
            boolean extrude = !extrusionKey.equals(RMGraphPartPie.EXTRUDE_NONE) &&
                (extrusionKey.equals(RMGraphPartPie.EXTRUDE_ALL) ||
                (extrusionKey.equals(RMGraphPartPie.EXTRUDE_FIRST) && j==0) ||
                (extrusionKey.equals(RMGraphPartPie.EXTRUDE_LAST) && j==jMax-1));
                
            // If there's an extrusion key and it's false, it must be custom - try evaluating it for a boolean value
            if(!extrude && extrusionKey.length() > 0) 
                extrude = RMKeyChain.getBoolValue(group, extrusionKey);

            // If extruding, reset wedge bounds to extruded bounds
            if(extrude) {
                double wedgeAngle = MathUtils.mod(startAngle + sweep/2, 360);
                double extrusionGap = pieBounds.getWidth()/20;
                double offsetX = MathUtils.cos(wedgeAngle)*extrusionGap;
                double offsetY = MathUtils.sin(wedgeAngle)*extrusionGap;
                Rect wedgeBounds = pieBounds.clone(); wedgeBounds.offset(offsetX, offsetY);
                wedge.setFrame(wedgeBounds);
            }
            
            // Set wedge color
            wedge.setColor(getColor(j));
            
            // Add wedge to section item
            sectionItem.setBar(wedge);
            
            // Add wedge shape to PieShape
            _pieShape.addWedge(wedge);
        }

        // Add Wedge Labels
        if(barLabelString!=null) {
            
            // Declare rect for last wedge label bounds
            Rect lastLabelBounds = null;
            
            // Declare local variable for angle
            double lastAngle = 0;

            // Iterate over wedges and create and add wedge label for each
            for(int j=0, jMax=section.getItemCount(); j<jMax; j++) {
                
                // Get current loop section item and item wedge
                RMGraphSection.Item sectionItem = section.getItem(j);
                RMOvalShape wedge = (RMOvalShape)sectionItem.getBar();
                
                // Calcuate percent of 
                float percent = wedge.getSweepAngle()/360*100;
                
                // Calcuate angle of radian that bisects
                double angle = MathUtils.mod(wedge.getStartAngle() + wedge.getSweepAngle()/2, 360);
                
                // Get string
                Map map = Collections.singletonMap("Percent", percent);
                
                // Add group
                _rptOwner.pushDataStack(sectionItem.getGroup());
                RMXString string = barLabelString.rpgClone(_rptOwner, map, null, true);
                _rptOwner.popDataStack();

                // Get new wedge label text
                RMTextShape label = new RMTextShape(string);
                
                // Set stroke and fill
                if(labelAxis.getStroke()!=null) label.setStrokeColor(labelAxis.getStrokeColor());
                if(labelAxis.getFill()!=null) label.setColor(labelAxis.getColor());
                
                // Have wedge label set size to fit
                label.setBestSize();

                // Calcuate mid point of wedge label bounds from pie bounds extended by 20 pts in width/height
                double labelMidX = pieBounds.getMidX() + MathUtils.cos(angle)*(pieBounds.getWidth()/2 + 20);
                double labelMidY = pieBounds.getMidY() + MathUtils.sin(angle)*(pieBounds.getHeight()/2 + 20);
                
                // Calculate wedge label location
                double labelX = angle>=90 && angle <= 270? labelMidX - label.width()/2 : labelMidX;
                double labelY = angle>=90 && angle <= 270? labelMidY - label.height()/2 : labelMidY - label.height()/2;
                
                // Set label position
                label.setXY(labelX, labelY);
                
                // If wedgeLabelBounds intersects lastWedgeLabelBounds, scoot it up or down
                if(j>0 && label.getFrame().getInsetRect(2).intersects(lastLabelBounds))
                    label.setY(angle>=180? lastLabelBounds.y - label.getHeight()+4 : lastLabelBounds.getMaxY()-4);

                // Add wedge label text to PieShape
                _pieShape.addWedgeLabel(label);

                // draw a line from label to wedge, if specified in template
                if(drawWedgeLabelLines) {
                    
                    // Get label frame
                    Rect labelFrame = label.getFrame();
                    
                    // Declare wedge label line start point to middle of label
                    double startX = labelFrame.getMidX();
                    double startY = labelFrame.getMidY();
                    
                    // Adjust line to edge of label based on angle
                    if(angle>=45 && angle<135) startY = labelFrame.y;
                    else if(angle>=135 && angle<225) startX = labelFrame.getMaxX();
                    else if(angle>=225 && angle<315) startY = labelFrame.getMaxY();
                    else startX = labelFrame.x;
                    
                    // Calculate wedge label line end point
                    double endX = _graph.getWidth()/2 + MathUtils.cos(angle)*((wedge.width()/2)*.8f);
                    double endY = _graph.getHeight()/2 + MathUtils.sin(angle)*((wedge.width()/2)*.8f);
                    
                    // Create wedge label line shape, set StrokeColor to LightGray and add label line
                    RMLineShape line = new RMLineShape(startX, startY, endX, endY);
                    line.setStrokeColor(RMColor.lightGray);
                    _pieShape.addWedgeLabelLine(line);
                }
                
                // Calculate lastWedgeLabelBounds. If in same quadrant do union, if in new quadrant, copy
                if(j==0 || MathUtils.trunc(lastAngle, 90)!=MathUtils.trunc(angle, 90))
                    lastLabelBounds = label.getFrame();
                else lastLabelBounds.union(label.getFrame());
                
                // Update last angle
                lastAngle = angle;
            }
        }
    }
}

/**
 * Returns the bounds for an individual pie in the graph area (when there are multiple keys).
 */
private Rect getPieBounds(int anIndex, int aCount, Rect aRect)
{
    // Get width & height
    double width = aRect.width;
    double height = aRect.height;
    
    // Get x & y & total
    int x = 1, y = 1, total = x*y;

    // Find number of x & y chunks to break width and height into, such that
    //  ratio of x/y is as close as possible to ratio of width/height
    while(aCount > total) {
        if(Math.abs((x+1.)/y - width/height) <= Math.abs(x/(y+1.) - width/height)) x++;
        else y++;
        total = x*y;
    }

    // Calculate rect for index, assuming index traverses grid from left to right, top to bottom
    return new Rect(aRect.x + (anIndex%x)*width/x, aRect.y + (y - 1 - anIndex/x)*height/y, width/x, height/y);
}

/** Creates a rect representing the largest square inside rect. */
private static Rect squareRectInRect(Rect aRect)
{
    double x = aRect.getX(), y = aRect.getY(), w = aRect.getWidth(), h = aRect.getHeight();
    if(w>h) { x += (w-h)/2; w = h; } else { y += (h-w)/2; h = w; }
    return new Rect(x,y,w,h);
}

/**
 * An interface for a shape that renders a bar graph from bar graph pieces.
 */
public interface PieGraphShape extends RMGraphRPG.GraphShape {

    /** Add a wedge shape. */
    public void addWedge(RMShape aShape);
    
    /** Add a wedge label shape. */
    public void addWedgeLabel(RMTextShape aLabel);
    
    /** Add a wedge label line. */
    public void addWedgeLabelLine(RMLineShape aLine);
}

/**
 * A BarGraphShape implementation.
 */
public static class PieGraphShape2D extends RMParentShape implements PieGraphShape {

    /** Creates a new BarGraphShape2D. */
    public PieGraphShape2D(RMGraph aGraph)  { copyShape(aGraph); }

    /** Returns the RMGraphRPG. */
    public RMGraphRPG getGraphRPG()  { return _grpg; } RMGraphRPG _grpg;
    
    /** Sets the RMGraphRPG. */
    public void setGraphRPG(RMGraphRPG aGRPG)  { _grpg = aGRPG; }

    /** Implements PieView method to just add wedge shape. */
    public void addWedge(RMShape aBar)  { addChild(aBar); }
    
    /** Implements PieView method to just add wedge label shape. */
    public void addWedgeLabel(RMTextShape aLabel)  { addChild(aLabel); }
    
    /** Implements PieView method to just add wedge label line shape. */
    public void addWedgeLabelLine(RMLineShape aLine)  { addChild(aLine); }
}

/**
 * This graph renders a pie graph in 3D.
 */
static class PieGraphShape3D extends RMScene3D implements PieGraphShape {

    // List of pie wedges, labels and lines
    List <RMShape>    _wedges = new ArrayList();
    List <RMShape>    _labels = new ArrayList();
    List <RMShape>    _lines = new ArrayList();

    /** Creates a new pie view 3d. */
    public PieGraphShape3D(RMGraph aGraph)
    {
        copyShape(aGraph); // Copy graph area attributes
        copy3D(aGraph.get3D()); // Copy 3D attributes from graph area 3D
    }
    
    /** Returns the RMGraphRPG. */
    public RMGraphRPG getGraphRPG()  { return _grpg; } RMGraphRPG _grpg;
    
    /** Sets the RMGraphRPG. */
    public void setGraphRPG(RMGraphRPG aGRPG)  { _grpg = aGRPG; }
    
    /** Adds a wedge shape to graph view. */
    public void addWedge(RMShape aWedge)  { _wedges.add(aWedge); }
    
    /** Adds a wedge label to graph view. */
    public void addWedgeLabel(RMTextShape aLabel)  { _labels.add(aLabel); }
    
    /** Adds a wedge label line to graph view. */
    public void addWedgeLabelLine(RMLineShape aLine)  { _lines.add(aLine); }
    
    /** Rebuilds 3D representation of shapes from shapes list (called by layout manager). */
    protected void layoutImpl()
    {
        // Remove Shape3Ds
        removeShapes();
        
        // Iterate over wedges and add them as 3D
        for(int i=0, iMax=_wedges.size(); i<iMax; i++) { RMShape wedge = _wedges.get(i);
            addShapesForRMShape(wedge, 0, getDepth(), true); }
        
        // Iterate over lines and add them as 3D
        //for(int i=0, iMax=_lines.size(); i<iMax; i++) addChild3D(_lines.get(i), getDepth()/3-5, getDepth()/3-5);
        
        // Create label shapes
        boolean fullRender = true; // !isValueAdjusting()
        for(int i=0, iMax=_labels.size(); i<iMax && fullRender; i++) { RMShape label = _labels.get(i);
            addShapesForRMShape(label, -5, -5, false); }
    
        // Do normal version
        super.layoutImpl();
    }
}

}