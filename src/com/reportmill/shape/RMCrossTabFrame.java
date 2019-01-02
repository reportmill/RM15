/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;
import snap.util.*;

/**
 * This class wraps a shape around a crosstab to constrain the crosstab's bounds to a region on the page. It
 * also provides support for paginating to multiple pages.
 */
public class RMCrossTabFrame extends RMParentShape {

    // Whether a paginating table will reprint header rows
    boolean            _reprintHeaderRows = true;
    
/**
 * Creates a new crosstab.
 */
public RMCrossTabFrame()  { _width = 350; _height = 200; }

/**
 * Returns the crosstab associated with this crosstab.
 */
public RMCrossTab getTable()  { if(getChildCount()==0) createTable(); return (RMCrossTab)getChild(0); }

/**
 * Creates the RMCrossTab.
 */
protected RMCrossTab createTable()
{
    RMCrossTab ctab = new RMCrossTab(); ctab.addRow(); ctab.addCol();
    ctab.setHeaderRowCount(1); ctab.setHeaderColCount(1);
    ctab.getCell(0, 0).setVisible(false);
    addChild(ctab);
    return ctab;
}

/**
 * Returns whether a paginating table will reprint header rows.
 */
public boolean getReprintHeaderRows()  { return _reprintHeaderRows; }

/**
 * Sets whether a paginating table will reprint header rows.
 */
public void setReprintHeaderRows(boolean aFlag)  { _reprintHeaderRows = aFlag; }

/**
 * Override to constrain child crosstab to frame.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    // Get frame clone and CrossTab clone and add
    RMCrossTabFrame rclone = (RMCrossTabFrame)clone(); //new RMCrossTabFrame(); rclone.copyShape(this);
    RMCrossTab ctab = (RMCrossTab)anRptOwner.rpg(getTable(), rclone);
    rclone.addChild(ctab);
    
    // If CrossTab width is greater than frame width, shrink CrossTab width/height
    if(ctab.getWidth()>getWidth()) rclone.shrinkCrossTabToFitFrameWidth(ctab);
    
    // If paginating, return paginated frame clone, otherwise just frame clone
    if(anRptOwner.getPaginate() && getParent(RMTableRow.class)==null)
        return rclone.paginateFrame(ctab);
    return rclone;
}

/**
 * Shrinks CrossTab to fit frame width.
 */
private void shrinkCrossTabToFitFrameWidth(RMCrossTab ctab)
{
    // Get scale for table width, reset table width/height and layout immediately
    double sx = getWidth()/ctab.getWidth();
    for(int i=0, iMax=ctab.getColCount(); i<iMax; i++) { RMCrossTabCol col = ctab.getCol(i);
        col.setWidth(col.getWidth()*sx); }
    for(int i=0, iMax=ctab.getRowCount(); i<iMax; i++) { RMCrossTabRow row = ctab.getRow(i);
        row.setHeight(row.getHeight()*sx); }
    ctab.layout();
    
    // Iterate over cells and reset cell font scale factor (accounting for cells that span multiple rows/cols)
    for(int i=0, iMax=ctab.getRowCount(); i<iMax; i++)
        for(int j=0, jMax=ctab.getColCount(); j<jMax; j++) { RMCrossTabCell cell = ctab.getCell(i, j);
            if(cell.getRow()==i && cell.getCol()==j)
                cell.getRichText().scaleFonts(sx); }
}

/**
 * Paginates frame.
 */
private RMParentShape paginateFrame(RMCrossTab ctab)
{
    // Create ShapeList and while remainder is greater than page height, divide and add
    ReportOwner.ShapeList slist = new ReportOwner.ShapeList(); slist.addChild(this);
    while(ctab.getHeight()>getHeight()) {
        RMCrossTabFrame frame = new RMCrossTabFrame(); frame.copyShape(this);
        ctab = (RMCrossTab)ctab.divideShapeFromTop(getHeight()); // Divide clone from top by frame height
        ctab.setXY(0,0);
        frame.addChild(ctab); // Add to clone shape
        slist.addChild(frame);
    }
    
    // Return clone
    return slist;
}

/**
 * Paints crosstab.
 */
protected void paintShape(Painter aPntr)
{
    // Do normal version
    super.paintShape(aPntr);
    
    // If not editing, just return
    RMShapePaintProps props = RMShapePaintProps.get(aPntr);
    if(!props.isEditing()) return;
    
    // Get path for CrossTab: Iterate over columns to define horizontal corner piece
    RMCrossTab ctab = getTable();
    Path path = new Path(); double x = 0;
    for(int i=0, iMax=ctab.getColCount(); i<iMax; i++) {
        
        // Iterate up cells
        double y = ctab.getHeight();
        for(int j=ctab.getRowCount()-1; j>=0; j--) { if(ctab.getCell(j, i).isVisible()) break;
            y -= ctab.getRow(j).getHeight(); }
        
        // 
        if(i==0) path.moveTo(x, y);
        else path.lineTo(x, y);
        
        // Increment x to end of column and draw again
        x += ctab.getCol(i).getWidth();
        path.lineTo(x, y);
    }
    
    // Draw the rest of the path
    path.lineTo(x, 0); path.lineTo(getWidth(), 0); path.lineTo(getWidth(), getHeight());
    path.lineTo(0, getHeight()); path.close();
    
    // Draw path border and fill in background with light gray
    aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); aPntr.draw(path);
    aPntr.setColor(new Color(11/12f, 11/12f, 11/12f)); aPntr.fill(path);

    // Draw CrossTab button
    Rect button = new Rect(getWidth() - 100, getHeight() - 18, 100, 18);
    aPntr.setColor(Color.LIGHTGRAY); aPntr.fill(button);
    aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); aPntr.draw(button);
    aPntr.setColor(Color.DARKGRAY); aPntr.setFont(Font.Arial12.getBold());
    aPntr.drawString("CrossTab", button.getX() + 14, button.getMaxY() - 4);

    // Draw path border
    aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); aPntr.draw(path);    
}

/**
 * Override to return preferred height of table.
 */
protected double getPrefHeightImpl(double aWidth)
{
    return getChildCount()>0? Math.max(getChild(0).getHeight(), getHeight()) : getHeight();
}

/** Editor method. */
public boolean childrenSuperSelectImmediately()  { return true; }

/**
 * XML Archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    XMLElement e = super.toXMLShape(anArchiver); e.setName("cell-table-frame");
    if(!getReprintHeaderRows()) e.add("reprint-header-rows", false);
    return e;
}

/**
 * XML Unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXMLShape(anArchiver, anElement);
    if(anElement.hasAttribute("reprint-header-rows"))
        setReprintHeaderRows(anElement.getAttributeBoolValue("reprint-header-rows"));
}

}