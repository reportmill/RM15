/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMFormat;
import com.reportmill.base.RMGroup;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.*;
import snap.util.MathUtils;

/**
 * This class generates (and represents) a RPG'd bar graph area.
 */
class RMGraphRPGBar extends RMGraphRPG {

    // The intervals for the graph
    RMGraphIntervals       _intervals;

    // The GraphShape cast to BarGraphShape
    BarGraphShape          _barShape;
    
    // Whether bar graph is meshed, stacked, layered
    boolean                _meshed, _stacked, _layered;
    
    // The shape that holds the basic attributes for bars (prototype)
    RMShape                _barProtype;
    
/**
 * Creates a bar graph maker.
 */
public RMGraphRPGBar(RMGraph aGraph, ReportOwner anRptOwner)
{
    // Do normal version
    super(aGraph, anRptOwner);
    
    // Calculate intervals
    _intervals = RMGraphIntervals.getGraphIntervals(this);
    
    // Set Meshed, Stacked and Layered
    _meshed = isMeshed(); _stacked = _graph.isStacked(); _layered = _graph.isLayered();
    
    // Set graph view from graph shape
    _barShape = (BarGraphShape)getGraphShape();
    
    // If graph parent has child named "BarPrototype" set as prototype
    if(_graph.getParent()!=null) _barProtype = _graph.getParent().getChildWithName("BarPrototype");
    
    // Add grid and bars
    addGrid();
    addBars();
    
    // Add value axis labels
    if(_graph.getValueAxis().getShowAxisLabels())
        addValueAxisLabels();
    
    // Add label axis labels
    if(_graph.getLabelAxis().getShowAxisLabels())
         addLabelAxisLabels();
     
     // Add series labels
     addSeriesLabels();
}

/**
 * Creates the graph shape.
 */
protected RMParentShape createGraphShape()
{
    return _graph.isDraw3D()? new RMGraphRPGBar3D(_graph) : new BarGraphShape2D(_graph);
}

/**
 * Returns the number of intervals for this filled graph.
 */
public int getIntervalCount()  { return _intervals.getIntervalCount(); }

/**
 * Returns the individual interval at a given index as a float value.
 */
public Float getInterval(int anIndex)  { return _intervals.getInterval(anIndex); }

/**
 * Returns the last interval as a float value.
 */
public Float getIntervalLast()  { return getInterval(getIntervalCount()-1); }

/**
 * Returns the number of suggested ticks between the intervals of the RPG'd graph.
 */ 
public int getMinorTickCount()
{
    // Calcuate height per tick - if height greater than 1 inch, return 4, greater than 3/4 inch return 3, otherwise 1
    double heightPerTick = _graph.getHeight()/(getIntervalCount() - 1);
    return heightPerTick>=72? 4 : heightPerTick>=50? 3 : 1;
}

/**
 * Returns whether graph is vertical.
 */
public boolean isVertical()  { return _graph.isVertical(); }

/**
 * Adds the grid to the graph area.
 */
private void addGrid()
{
    // Get graph min interval and max interval
    float minInterval = getInterval(0);
    float maxInterval = getIntervalLast();
    float totalInterval = maxInterval - minInterval;
    
    // Get graph bounds
    Rect bounds = _graph.getBoundsInside();
    
    // Get grid line width/height
    double lineW = isVertical()? bounds.width : 0;
    double lineH = isVertical()? 0 : bounds.height;
    
    // If drawMajorAxis, then add grid lines
    if(_graph.getValueAxis().getShowMajorGrid()) {
        
        // Get grid max
        double gridMax = isVertical()? bounds.height : bounds.width;
        double intervalSize = getInterval(1) - minInterval;
        double minorTickInterval = gridMax*intervalSize/totalInterval/(getMinorTickCount()+1);

        // Iterate over graph intervals
        for(int i=0, iMax=getIntervalCount(); i<iMax - 1; i++) {
            
            // Get interval ratio and line x & y
            double intervalRatio = i/(iMax - 1f);
            double lineX = isVertical()? bounds.x : bounds.x + bounds.width*intervalRatio;
            double lineY = isVertical()? bounds.y + bounds.height*intervalRatio : bounds.y;

            // DrawMajorAxis
            if(i>0) {
                RMLineShape line = new RMLineShape();
                line.setFrame(lineX, lineY, lineW, lineH);
                _barShape.addGridLineMajor(line);
            }

            // If not drawing minor axis, just continue
            if(!_graph.getValueAxis().getShowMinorGrid()) continue;
                
            // Draw minor axis
            for(int j=0; j<getMinorTickCount(); j++) {
                RMLineShape line = new RMLineShape();
                double minorLineX = isVertical()? bounds.x : lineX + (j+1)*minorTickInterval;
                double minorLineY = isVertical()? lineY + (j+1)*minorTickInterval : bounds.y;
                line.setStrokeColor(RMColor.lightGray);
                line.setFrame(minorLineX, minorLineY, lineW, lineH);
                _barShape.addGridLineMinor(line);
            }
        }
    }
    
    // Get whether zero axis line was added
    boolean zeroAxisLineAdded = false;
    if(_graph.getValueAxis().getShowMajorGrid())
        for(int i=0, iMax=getIntervalCount(); i<iMax; i++)
            zeroAxisLineAdded = zeroAxisLineAdded || MathUtils.equalsZero(getInterval(i));
    
    // If zero axis line not added, add it (happens when there are pos & neg values)
    if(!zeroAxisLineAdded) {
        RMLineShape line = new RMLineShape();
        double intervalRatio = minInterval/totalInterval;
        double lineX = isVertical()? 0 : -bounds.width*intervalRatio;
        double lineY = isVertical()? bounds.height + bounds.height*intervalRatio : 0;
        line.setFrame(lineX, lineY, lineW, lineH);
        _barShape.addGridLineMajor(line);
    }
    
    // If drawGroupSeparator, add separator line for each section, perpendicular to grid
    if(_graph.getLabelAxis().getShowGridLines()) {
        
        // Get SeriesCount, SeriesItemCount and SectionCount
        int seriesCount = getSeriesCount();
        int seriesItemCount = seriesCount>0? getSeries(0).getItemCount() : 0;
        int sectionCount = _meshed? seriesItemCount : seriesCount;
        
        // Iterate over series
        for(int i=1, iMax=sectionCount; i<iMax; i++) {
            RMLineShape line = new RMLineShape();
            double lineX = isVertical()? bounds.x + bounds.width*i/iMax : bounds.x;
            double lineY = isVertical()? bounds.y : bounds.y + bounds.height*i/iMax;
            line.setFrame(lineX, lineY, isVertical()? 0 : bounds.width, isVertical()? bounds.height : 0);
            _barShape.addGridLineSeparator(line);
        }
    }
}

/**
 * Adds the bars to the graph area.
 */
public void addBars()
{
    // Get graph bar prototype
    RMShape prototype = getBarPrototype();
    
    // Iterate over series and series items
    for(int i=0, iMax=getSeriesCount(); i<iMax; i++) { RMGraphSeries series = getSeries(i);
        for(int j=0, jMax=series.getItemCount(); j<jMax; j++) { RMGraphSeries.Item seriesItem = series.getItem(j);
            
            // Get bar by doing RPG on prototype for SeriesItem
            _rptOwner.pushDataStack(seriesItem.getGroup());
            RMShape bar = _rptOwner.rpg(prototype, (RMShape)_barShape);
            _rptOwner.popDataStack();
            
            // Set bar color and bounds
            int cindex = i; if(_stacked && (iMax==1 || !_meshed) || _graph.isColorItems()) cindex = j;
            RMColor color = getColor(cindex);
            bar.setColor(color);
            Rect barBounds = getBarBounds(i, j); bar.setBounds(barBounds);
            
            // Get layer index
            int layer = _graph.isLayered()? (_meshed? i : j) : 0;
            
            // Add Bar to graph view
            _barShape.addBar(bar, layer);
            
            // Set bar in series-value
            seriesItem.setBar(bar);
        }
    }
}

/**
 * Returns the bar bounds for bar at given series and item.
 */
public Rect getBarBounds(int aSeriesIndex, int anItemIndex)
{
    // Get the referenced series
    RMGraphSeries series = getSeries(aSeriesIndex);
    
    // Get series count and series item count
    int seriesCount = getSeriesCount();
    int seriesItemCount = series.getItemCount();
    
    // Get number of graph sections and number of items in each graph section
    int sectionCount = _meshed? seriesItemCount : seriesCount;
    int sectionItemCount = _meshed? seriesCount : seriesItemCount;
    
    // Get section index and section item index by converting from series index and series item index
    int sectionIndex = _meshed? anItemIndex : aSeriesIndex;
    int sectionItemIndex = _meshed? aSeriesIndex : anItemIndex;
    
    // Get graph width and height
    double width = isVertical()? _graph.getWidth() : _graph.getHeight();
    double height = isVertical()? _graph.getHeight() : _graph.getWidth();
    
    // Get section width
    double sectionWidth = width/sectionCount;

    // Get number of bars per section
    int sectionBarCount = _stacked || _layered? 1 : sectionItemCount;
    
    // Make sure section bar count is at least as many bars as minimum section bar count
    sectionBarCount = Math.max(sectionBarCount, _graph.getBars().getBarCount());
    
    // Get width of space between bars as a fraction of bar width
    double barGapFraction = _graph.getBars().getBarGap();
    
    // Get width of space between sections as a fraction of bar width
    float sectionGapFraction = _graph.getBars().getSetGap();
    
    // Calculate bar width
    double barWidth = sectionWidth/(sectionBarCount + (sectionBarCount-1)*barGapFraction + sectionGapFraction);
    
    // Calculate start of section
    double sectionX = barWidth*sectionGapFraction/2;
    
    // Calculate width of space between bars
    double barGap = barWidth*barGapFraction;
    
    // Calculate bar x
    double barX =  sectionIndex*sectionWidth + sectionX;
    
    // If not stacked or layered, shift bar by previous section bars
    if(!_stacked && !_layered)
        barX += sectionItemIndex*(barWidth + barGap);
    
    // Get graph min, max & total intervals
    float minInterval = getInterval(0);
    float maxInterval = getIntervalLast();
    float totalInterval = maxInterval - minInterval;

    // Calcuate bar y
    double barY = -minInterval/totalInterval*height;
    
    // If stacked, shift bar y by previous bars
    if(_stacked) {
        float previousValues = 0;
        for(int i=0; i<sectionItemIndex; i++)
            if(_meshed)
                previousValues += getSeries(i).getItem(anItemIndex).getValue().floatValue();
            else previousValues += series.getItem(i).getValue().floatValue();
        barY += previousValues/totalInterval*height;
    }
    
    // Get current loop series item
    RMGraphSeries.Item seriesItem = series.getItem(anItemIndex);
    
    // Get bar value
    double value = seriesItem.getFloatValue();
    
    // Calcuate bar height
    double barHeight = value/totalInterval*height;
    
    // If vertical, flip bars
    if(isVertical())
        barY = _graph.getHeight() - (barY + barHeight);
    
    // If horizontal, swap x & y
    else {
        double x = barX; barX = barY; barY = x;
        double w = barWidth; barWidth = barHeight; barHeight = w;
    }
    
    // Return bounds
    return new Rect(barX, barY, barWidth, barHeight);
}

/**
 * Returns the width available for a bar label.
 */
public double getBarLabelMaxWidth()
{
    // Get series count and series item count
    int seriesCount = getSeriesCount(); if(seriesCount==0) return _graph.getWidth();
    int seriesItemCount = getSeries(0).getItemCount();
    
    // Get number of graph sections and number of items in each graph section
    int sectionCount = _meshed? seriesItemCount : seriesCount;
    int sectionItemCount = _meshed? seriesCount : seriesItemCount;
    
    // Get graph width and height
    double width = isVertical()? _graph.getWidth() : _graph.getHeight();
    
    // Get section width
    double sectionWidth = width/sectionCount;

    // Get number of bars per section
    int sectionBarCount = _stacked || _layered? 1 : sectionItemCount;
    
    // Make sure section bar count is at least as many bars as minimum section bar count
    sectionBarCount = Math.max(sectionBarCount, _graph.getBars().getBarCount());
    
    // Calculate bar width
    return Math.round(sectionWidth/sectionBarCount*.95);
}

/**
 * Adds the value axis labels to the graph area.
 */
private void addValueAxisLabels()
{
    // Get the value axis
    RMGraphPartValueAxis valueAxis = _graph.getValueAxis();
    
    // Get the graph width & height
    double width = _graph.getWidth();
    double height = _graph.getHeight();
    
    // Get format from value axis (defaults to basic)
    RMFormat format = valueAxis.getFormat();

    // Create shape for value axis and configure
    RMPolygonShape axis = new RMPolygonShape(); axis.copyShape(_graph);
    double axisX = isVertical()? -5 : 0;
    double axisY = isVertical()? 0 : height;
    double axisW = isVertical()? 5 : width;
    double axisH = isVertical()? height : 5;
    axis.setFrame(axisX, axisY, axisW, axisH);
    axis.setStroke(new RMStroke());

    // Create path for axis labels line: Iterate over graph intervals to add ticks to axis path
    Path path = new Path();
    for(int i=0, iMax=getIntervalCount(); i<iMax; i++) {
        double intervalPosition = isVertical()? height - height*i/(iMax - 1) : width*i/(iMax - 1);
        path.moveTo(isVertical()? 0  : intervalPosition, isVertical()? intervalPosition : 0);
        path.lineTo(isVertical()? -5 : intervalPosition, isVertical()? intervalPosition : 5);
    }
    
    // Add path to axis and add axis shape to the graph
    axis.setPath(path);
    _barShape.addAxis(axis);

    // Iterate over graph intervals to create axis label texts
    for(int i=0, iMax=getIntervalCount(); i<iMax; i++) { Float interval = getInterval(i);

        // Get string for intervalNumber
        String str = format.format(interval);
        RMXString xstr = new RMXString(str, valueAxis.getFont(), valueAxis.getTextColor());
        xstr.setParagraph(RMParagraph.CENTERED, 0, xstr.length());
        
        // Create new text for label, copy value axis text shape attributes and size to fit
        RMTextShape label = new RMTextShape(xstr);
        label.copyShape(valueAxis);
        label.setBestSize();
        
        // Calculate interval position
        double intervalPosition = isVertical()? height - height*i/(iMax - 1) : width*i/(iMax - 1);
        
        // Get point by graph that we want label to be aligned with
        Point point2 = isVertical()? new Point(-5, intervalPosition) : new Point(intervalPosition, height + 5);
        
        // Get angle of label bounds perimeter point radial that we want to sync to
        double angle = -valueAxis.getRoll();
        
        // If vertical graph and angle less than 60, just zero it out
        if(isVertical() && Math.abs(angle)<=60) angle = 0;
        
        // If horizontal, we want to sync to 12 o'clock position instead of 3 o'clock position
        if(!isVertical()) angle -= 90;
        
        // Get point on label perimeter that we want to sync to (in label parent coords)
        Point point1 = label.getBoundsInside().getPerimeterPointForRadial(angle, true);
        point1 = label.localToParent(point1);

        // Offset label location from its current location to graph location
        label.offsetXY(point2.getX() - point1.getX(), point2.getY() - point1.getY());
        
        // Add axis label to graph view
        _barShape.addValueAxisLabel(label);
    }
}

/**
 * Adds the bar labels to the graph area.
 */
private void addLabelAxisLabels()
{
    // Get the label axis shape and label axis key (just return if empty)
    RMGraphPartLabelAxis labelAxis = _graph.getLabelAxis();
    String key = labelAxis.getItemKey(); if(key==null || key.length()==0) return;
    
    // Iterate over sections, get section item and add label axis label for section bounds and series item group
    for(int i=0, iMax=getSectionCount(); i<iMax; i++) { RMGraphSection section = getSection(i);
        RMGraphSection.Item sectionItem = section.getItem(0);
        addLabelAxisLabel(section.getBounds(), sectionItem.getGroup());
    }
}

/**
 * Adds a label axis label for given rect and group.
 */
private void addLabelAxisLabel(Rect aRect, RMGroup aGroup)
{
    // Create label for group: Get label axis, get label (a clone), set text, do RPG and set best size
    RMGraphPartLabelAxis labelAxis = _graph.getLabelAxis();
    RMTextShape label = new RMTextShape(labelAxis.getItemKey()); // Create new RMText with attributes of label axis
    label.copyShape(labelAxis); label.setFont(labelAxis.getFont());
    label.getXString().setParagraph(RMParagraph.CENTERED, 0, label.length());
    label.getXString().rpgClone(_rptOwner, aGroup, null, false); // Do rpg on label string
    label.setBestSize();  // Resize label to best size

    // If label width greater than available width for bar, grow height
    double labelMaxWidth = getBarLabelMaxWidth();
    if(label.getWidth()>labelMaxWidth && isVertical() && label.getRoll()==0) {
        label.setWidth(labelMaxWidth); label.setHeight(label.getBestHeight()); label.setAlignment(Pos.CENTER); }

    // Get point by graph that we want label to be aligned with
    Point point2 = isVertical()? new Point(aRect.getMidX(), aRect.getMaxY() + 5) :
        new Point(aRect.x - 5, aRect.getMidY());
    
    // Get angle of label bounds perimeter point radial that we want to sync to
    double angle = -labelAxis.getRoll();
    
    // If horizontal, we want to sync to 12 o'clock position instead of 3 o'clock position
    if(isVertical()) angle -= 90;
    
    // Get point on label perimeter that we want to sync to (in label parent coords)
    Point point1 = label.getBoundsInside().getPerimeterPointForRadial(angle, true);
    point1 = label.localToParent(point1);

    // Offset label location from its current location to graph location
    label.offsetXY(point2.getX() - point1.getX(), point2.getY() - point1.getY());
    
    // Add the bar label to graph view
    _barShape.addLabelAxisLabel(label);
}

/**
 * Adds the bar labels to the graph area.
 */
private void addSeriesLabels()
{
    // Iterate over series
    for(int i=_graph.getSeriesCount()-1; i>=0; i--) { RMGraphPartSeries seriesPart = _graph.getSeries(i);
        
        // If empty, just continue
        if(seriesPart.getFirstActivePosition()==null) continue;
        
        // Get active positions
        List <RMGraphPartSeries.LabelPos> activePositions = new ArrayList();
        for(RMGraphPartSeries.LabelPos position : RMGraphPartSeries.LabelPos.values())
            if(seriesPart.getLabelShape(position).length()>0)
                activePositions.add(position);
        
        // Iterate over series items and active positions and add labels
        RMGraphSeries series = getSeries(i);
        for(int j=series.getItemCount()-1; j>=0; j--) { RMGraphSeries.Item seriesItem = series.getItem(j);
            for(RMGraphPartSeries.LabelPos position : activePositions)
                addLabel(seriesPart.getLabelShape(position), position, seriesItem);
        }
    }
}

/**
 * Adds an individual label.
 */
public void addLabel(RMTextShape aLabel, RMGraphPartSeries.LabelPos aPosition, RMGraphSeries.Item seriesItem)
{
    // Get group
    RMGroup group = seriesItem.getGroup();

    // Create new RMText with attributes of label axis
    RMTextShape label = (RMTextShape)aLabel.cloneDeep();
    
    // Do rpg on new label string
    label.getXString().rpgClone(_rptOwner, group, null, false);
    
    // Resize label to best size
    label.setBestSize();
    
    // If label width greater than available width for bar, grow height
    double labelMaxWidth = getBarLabelMaxWidth();
    if(label.getWidth()>labelMaxWidth && isVertical() && label.getRoll()==0) {
        label.setWidth(labelMaxWidth); label.setHeight(label.getBestHeight()); label.setAlignment(Pos.CENTER); }

    // Get bar rect
    Rect barRect = seriesItem.getBar().getFrame();
    
    // Declare variables for label position
    double labelX = 0;
    double labelY = 0;

    // Top Position
    if(aPosition==RMGraphPartSeries.LabelPos.Top) {
        labelX = barRect.getMidX() - label.getWidth()/2;
        labelY = barRect.y + 2;
    }

    // Middle Position
    else if(aPosition==RMGraphPartSeries.LabelPos.Middle) {
        labelX = barRect.getMidX() - label.getWidth()/2;
        labelY = barRect.getMidY() - label.getHeight()/2;
    }

    // Bottom Position
    else if(aPosition==RMGraphPartSeries.LabelPos.Bottom) {
        labelX = barRect.getMidX() - label.getWidth()/2;
        labelY = barRect.getMaxY() - label.getHeight() - 2;
    }

    // Above position (or negative value)
    else if(aPosition==RMGraphPartSeries.LabelPos.Above || seriesItem.getFloatValue()<0) {
        labelX = isVertical()? barRect.getMidX() - label.getWidth()/2 : barRect.getMaxX() + 5;
        labelY = isVertical()? barRect.y - label.getHeight() - 5 : barRect.getMidY() - label.getHeight()/2;
    }
    
    // Outside Bottom
    else  {
        labelX = isVertical()? barRect.getMidX() - label.getWidth()/2 : barRect.x - label.getWidth() - 5;
        labelY = isVertical()? barRect.getMaxY() + 5 : barRect.getMidY() - label.getHeight()/2;
        
        // If barLabel is rotated, shift label to positions less likely to overlap
        if(label.getRoll()!=0) {
            label.setXY(labelX, labelY);
            
            // Vertical: Align left (+roll) or right (-roll) edge to bar midX, maxY+3
            if(isVertical()) {
                Point p = label.localToParent(label.getRoll()>0? 0 : label.getWidth(), label.getHeight()/2);
                labelX = labelX - (p.x - barRect.getMidX());
                labelY = labelY - (p.y - barRect.getMaxY()) + label.getHeight()/2*MathUtils.cos(label.getRoll()) + 3;
            }
            
            // Horizontal: Align right edge to bar minX-3, midY
            else {
                Point p = label.localToParent(label.getWidth(), label.getHeight()/2);
                labelX = labelX - (p.x - barRect.x) - 3;
                labelY = labelY - (p.y - barRect.getMidY());
            }
        }
    }

    // Set the XY
    label.setXY(labelX, labelY);
    
    // Add the bar label to graph view
    _barShape.addBarLabel(label, aPosition);
}

/**
 * Returns the shape used to represent the basic attributes of bars & wedges.
 */
public RMShape getBarPrototype()  { return _barProtype!=null? _barProtype : (_barProtype=createBarPrototype()); }

/**
 * Returns the shape used to represent the basic attributes of bars & wedges.
 */
private RMShape createBarPrototype()  { RMShape bp = new RMRectShape(); bp.setStroke(new RMStroke()); return bp; }

/**
 * An interface for a shape that renders a bar graph from bar graph pieces.
 */
public interface BarGraphShape extends RMGraphRPG.GraphShape {

    /** Add grid line. */
    public void addGridLineMajor(RMLineShape aLine);

    /** Add grid line. */
    public void addGridLineMinor(RMLineShape aLine);
    
    /** Add grid line. */
    public void addGridLineSeparator(RMLineShape aLine);
    
    /** Add Bar. */
    public void addBar(RMShape aBar, int aLayer);
    
    /** Add bar label. */
    public void addBarLabel(RMShape aBarLabel, RMGraphPartSeries.LabelPos aVersion);
    
    /** Add axis. */
    public void addAxis(RMShape aShape);
    
    /** Add value axis label. */
    public void addValueAxisLabel(RMShape anAxisLabel);
    
    /** Add label axis label. */
    public void addLabelAxisLabel(RMShape anAxisLabel);
}

/**
 * A BarGraphShape implementation.
 */
public static class BarGraphShape2D extends RMParentShape implements BarGraphShape {

    /** Creates a new BarGraphShape2D. */
    public BarGraphShape2D(RMGraph aGraph)  { copyShape(aGraph); }

    /** Returns the RMGraphRPG. */
    public RMGraphRPG getGraphRPG()  { return _grpg; } RMGraphRPG _grpg;
    
    /** Sets the RMGraphRPG. */
    public void setGraphRPG(RMGraphRPG aGRPG)  { _grpg = aGRPG; }
    
    /** Add grid line major. */
    public void addGridLineMajor(RMLineShape aLine)  { addChild(aLine); }
    
    /** Add grid line minor. */
    public void addGridLineMinor(RMLineShape aLine)  { addChild(aLine); }
    
    /** Add grid line separator */
    public void addGridLineSeparator(RMLineShape aLine)  { addChild(aLine); }
    
    /** Add bar shape. */
    public void addBar(RMShape aBar, int aLayer)  { addChild(aBar); }
    
    /** Add bar label. */
    public void addBarLabel(RMShape aBarLabel, RMGraphPartSeries.LabelPos aPosition)  { addChild(aBarLabel); }
    
    /** Add axis shape. */
    public void addAxis(RMShape aShape)  { addChild(aShape); }
    
    /** Add value axis label shape. */
    public void addValueAxisLabel(RMShape anAxisLabel)  { addChild(anAxisLabel); }
    
    /** Add label axis label shape. */
    public void addLabelAxisLabel(RMShape anAxisLabel)  { addChild(anAxisLabel); }
}

}