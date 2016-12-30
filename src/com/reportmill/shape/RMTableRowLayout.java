/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.MathUtils;

/**
 * A shape layout implementation that handles layout of structured table rows.
 */
public class RMTableRowLayout extends RMSpringLayout {

/**
 * Returns the parent as table row.
 */
public RMTableRow getParent()  { return (RMTableRow)super.getParent(); }

/**
 * Override to do nothing if structured.
 */
public void reset()  { if(!getParent().isStructured()) super.reset(); }

/**
 * Performs layout.
 */
protected void layoutChildren()
{
    // Get the parent table row (just return normal version if not structured)
    RMTableRow parent = getParent(); if(!parent.isStructured()) { super.layoutChildren(); return; }
    if(parent.getChildCount()==0) return; // Just return if no children
    
    // Layout all children edge to edge, by iterating over children and setting successive x values
    List <RMShape> children = parent.getChildren(); double width = 0;
    for(RMShape child : children) {
        child.setBounds(width, 0, child.getWidth(), parent.getHeight());
        width += child.getWidth();
    }
    
    // If total width doesn't equal parent width, divy up and add to each child by ratio of their current sizes
    double pwidth = parent.getWidth();
    if(!MathUtils.equals(width,pwidth)) { double extra = pwidth - width, x = 0;
        for(RMShape child : children) {
            double ow = child.getWidth(), nw = ow + ow/width*extra;
            child.setX(x); child.setWidth(nw); x += nw;
        }
    }
    
    // Sync Structure With Alternates
    if(parent.getSyncStructureWithAlternates() && parent.getAlternates()!=null)
        for(RMTableRow alternate : (Collection<RMTableRow>)(Collection)parent.getAlternates().values())
            alternate.syncStructureWithShape(parent);

    // Sync Structure With Row Above
    if(parent.getSyncStructureWithRowAbove() && parent.getRowAbove()!=null)
        parent.getRowAbove().syncStructureWithShape(parent);
}

/**
 * Override to optimize structured case.
 */
protected double computePrefHeight(double aWidth)
{
    // If structured table row, return max of current row height and max child best size
    RMTableRow trow = getParent();
    if(trow.isStructured() && trow.getChildCount()>0) { double max = trow.getHeight();
        for(RMShape child : trow.getChildren()) max = Math.max(max, child.getPrefHeight()); return max; }
    return super.computePrefHeight(aWidth); // Otherwise, do normal version
}

}