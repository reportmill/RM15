/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents a block of labels.
 */
public class RMLabels extends RMParentShape {
    
    // Dataset key used to get objects during RPG
    String           _datasetKey;
    
    // The grouping for labels
    RMGrouping       _grouping = new RMGrouping();
    
    // Number of rows/columns of labels
    int              _numberOfRows = 5, _numberOfColumns = 3;
    
    // Space between labels, horizontally/vertically
    double           _spacingWidth = 20, _spacingHeight = 20;
    
    // Default label width and height
    private final float DEFAULT_LABEL_WIDTH = 150;
    private final float DEFAULT_LABEL_HEIGHT = 90;

/**
 * Creates a new labels template.
 */
public RMLabels()
{
    _width = DEFAULT_LABEL_WIDTH*_numberOfColumns + _spacingWidth*(_numberOfColumns-1);
    _height = DEFAULT_LABEL_HEIGHT*_numberOfRows + _spacingHeight*(_numberOfRows-1);
}

/**
 * Returns the default label shape.
 */
public RMLabel getLabel()
{
    // If no label is present, create default and add
    if(getChildCount()==0) {
        RMLabel label = new RMLabel(); label.setAutosizing("-~~,-~~");
        label.setSize(DEFAULT_LABEL_WIDTH, DEFAULT_LABEL_HEIGHT);
        addChild(label);
    }
    
    // Return first child as label
    return (RMLabel)getChild(0);
}

/**
 * Returns the dataset key associated with labels.
 */
public String getDatasetKey()  { return _datasetKey; }

/**
 * Sets the dataset key associated with labels.
 */
public void setDatasetKey(String aKeyPath)  { _datasetKey = aKeyPath; }

/**
 * Returns the grouping for this labels shape.
 */
public RMGrouping getGrouping()  { return _grouping; }

/**
 * Returns the list of sorts associated with labels.
 */
public List getSorts()  { return _grouping.getSorts(); }

/**
 * Returns the number of sorts associated with labels.
 */
public int getSortCount()  { return _grouping.getSortCount(); }

/**
 * Returns the specific sort at the given index.
 */
public RMSort getSort(int anIndex)  { return _grouping.getSort(anIndex); }

/**
 * Returns the number of rows in the labels block.
 */
public int getNumberOfRows()  { return _numberOfRows; }

/**
 * Sets the number of rows in the labels block.
 */
public void setNumberOfRows(int aValue)  { _numberOfRows = aValue; }

/**
 * Returns the number of columns in the labels block.
 */
public int getNumberOfColumns()  { return _numberOfColumns; }

/**
 * Sets the number of columns in the labels block.
 */
public void setNumberOfColumns(int aValue)  { _numberOfColumns = aValue; }

/**
 * Returns the spacing between labels horizontally.
 */
public double getSpacingWidth()  { return _spacingWidth; }

/**
 * Sets the spacing between labels horizontally.
 */
public void setSpacingWidth(double aValue)  { _spacingWidth = aValue; }

/**
 * Returns the spacing between labels vertically.
 */
public double getSpacingHeight()  { return _spacingHeight; }

/**
 * Sets the spacing between labels vertically.
 */
public void setSpacingHeight(double aValue)  { _spacingHeight = aValue; }

/**
 * Returns the width of an individual label.
 */
public double getLabelWidth()  { return getLabel().getWidth(); }

/**
 * Returns the height of an individual label.
 */
public double getLabelHeight()  { return getLabel().getHeight(); }

/**
 * Overrides shape method to recalc spacing width.
 */
public void setWidth(double aWidth)
{
    double oldWidth = _width; // Cache old width
    super.setWidth(aWidth); // Do normal set width deep
    _spacingWidth = _spacingWidth*_width/oldWidth; // Adjust spacing according to ratio of new width to old width
}

/**
 * Overrides shape method to recalc spacing height.
 */
public void setHeight(double value)
{
    double h = _height; // Cache old height
    super.setHeight(value); // Do normal set height deep
    _spacingHeight = _spacingHeight*_height/h; // Adjust spacing according to ratio of new height to old height
}

/**
 * Resets labels block width and height if any of labels other attributes have changed.
 */
public void fixSize()
{
    // Fix width & height and reset layout so labels don't try to resize label
    setWidth(getLabelWidth()*_numberOfColumns + _spacingWidth*(_numberOfColumns-1));
    setHeight(getLabelHeight()*_numberOfRows + _spacingHeight*(_numberOfRows-1));
}

/**
 * Report Generation.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    // Get objects for labels, group and sort
    List objects = anRptOwner.getKeyChainListValue(_datasetKey);
    RMGroup group = new RMGroup(objects); // Get group for objects
    group.groupByLeafKey(null); // Group by nothing so each real object is child of RMGroup (and we can use group keys)
    RMSort.sort(group, _grouping.getSorts()); // Sort group

    // Create pages array to hold page shapes created to hold labels
    ReportOwner.ShapeList pages = new ReportOwner.ShapeList();
    
    // Get label shape to be used as a template for added labels
    RMLabel label = getLabel();

    // Top level group represents explicit page breaks, create shape for each page, set DataBearing object and fill
    RMParentShape page = null;
    for(int i=-1, j=-1, k=0, kMax=group.size(); k<kMax; k++) {

        // Adjust i & j
        j = (j+1)%_numberOfColumns;
        if(j==0)
            i = (i+1)%_numberOfRows;

        // If at beginning of new page, create and add new page shape
        if(i==0 && j==0) {
            page = new RMParentShape(); page.copyShape(this);
            pages.addChild(page);
        }

        // Get current loop object
        Object object = group.get(k);
        
        // Add object to data bearing objects, do rpg, and remove
        anRptOwner.pushDataStack(object);
        RMParentShape newLabel = (RMParentShape)anRptOwner.rpg(label, page);
        anRptOwner.popDataStack();
        
        // Add new label
        page.addChild(newLabel);
        
        // Calculate x & y for new label and set
        double x = j*label.getWidth() + j*_spacingWidth;
        double y = i*label.getHeight() + i*_spacingHeight;
        newLabel.setXY(x, y);
    }
    
    // Make sure there's at least one page
    if(pages.getChildCount()==0) { page = new RMParentShape(); page.copyShape(this); pages.addChild(page); }
    
    // Return pages
    return pages;
}
    
/**
 * Paint labels shapes.
 */
protected void paintShape(Painter aPntr)
{
    // Get labels bounds
    Rect bounds = getBoundsInside();

    // Draw frame rect
    aPntr.setColor(Color.DARKGRAY); aPntr.setStroke(Stroke.Stroke1);
    aPntr.draw(bounds);

    // Paint shape normally
    super.paintShape(aPntr);

    // Get labels template shape
    RMLabel label = getLabel();
    
    // Iterate over label rows/columns to draw individual label borders
    for(int i=0; i<getNumberOfRows(); i++)
        for(int j=0; j<getNumberOfColumns(); j++) {
            double labelX = j*label.getWidth() + j*getSpacingWidth(); // Get individual label x & y
            double labelY = i*label.getHeight() + i*getSpacingHeight();
            Rect lrect = new Rect(labelX, labelY, label.getWidth(), label.getHeight()); // Get label rect
            aPntr.setColor(i==0 && j==0? new Color(.45f, 0, 0) : Color.BLACK); // Set stroke red for first
            aPntr.draw(lrect); // Draw label rect
        }
}

/**
 * Override to clone grouping.
 */
public RMLabels clone()
{
    RMLabels clone = (RMLabels)super.clone();
    clone._grouping = _grouping.clone();
    return clone;
}

/**
 * Editor method - indicates that labels child should super-select immediately.
 */
public boolean childrenSuperSelectImmediately()  { return true; }

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXMLShape(anArchiver); e.setName("labels");
    
    // Archive DatasetKey, Sorts, NumberOfRows, NumberOfColumns, SpacingWidth, SpacingHeight
    if(_datasetKey!=null && _datasetKey.length()>0) e.add("list-key", _datasetKey);
    for(int i=0, iMax=getSortCount(); i<iMax; i++) e.add(getSort(i).toXML(anArchiver));
    if(_numberOfRows!=5) e.add("rows", _numberOfRows);
    if(_numberOfColumns!=3) e.add("cols", _numberOfColumns);
    if(_spacingWidth!=20) e.add("spacex", _spacingWidth);
    if(_spacingHeight!=20) e.add("spacey", _spacingHeight);
    
    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive DatasetKey, Sorts
    setDatasetKey(anElement.getAttributeValue("list-key"));
    List sorts = anArchiver.fromXMLList(anElement, "sort", null, this);
    _grouping.addSorts(sorts);
        
    // Unarchive NumberOfRows, NumberOfColumns, SpacingWidth, SpacingHeight
    setNumberOfRows(anElement.getAttributeIntValue("rows", 5));
    setNumberOfColumns(anElement.getAttributeIntValue("cols", 3));
    setSpacingWidth(anElement.getAttributeFloatValue("spacex", 20));
    setSpacingHeight(anElement.getAttributeFloatValue("spacey", 20));
}

}