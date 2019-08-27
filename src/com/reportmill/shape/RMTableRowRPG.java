/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMGroup;
import com.reportmill.base.RMKeyChain;
import java.util.*;
import snap.gfx.Rect;
import snap.util.MathUtils;

/**
 * Report generation shape for RMTableRow.
 */
public class RMTableRowRPG extends RMSpringShape {

    // The TableRow used to do RPG (and potentially an alternate)
    RMTableRow            _row, _row2;
    
    // The group
    RMGroup               _group;
    
    // The parent RPG row
    RMTableRowRPG         _parentRPG;
    
    // Child rows
    List <RMTableRowRPG>  _childRPGs;
    
    // The split version of the row
    RMTableRowRPG         _split;

/**
 * Generate Report for ReportOwner, RMTableRow and group.
 */
public void rpgAll(ReportOwner anRptOwner, RMTableRow aRow, RMGroup aGroup, String aSuggestedVersion)
{
    // Add Group to RptOwner.DataStack
    anRptOwner.pushDataStack(aGroup);
    
    // Get version
    String version = getVersionName(anRptOwner, aRow, aGroup, aSuggestedVersion);
    RMTableRow row = (RMTableRow)aRow.getVersion(version);
    
    _row = aRow; _row2 = row; _group = aGroup; // Set ivars
    copyShape(row); // Copy attributes
    if(_row2.isStructured()) setSpringsDisabled(true); // If structured, disable springs
    row.rpgChildren(anRptOwner, this); // RPG children
    
    // Set best height
    setBestHeight(); // Set best height
    layoutDeep();
    
    // Handle invisible shapes
    if(!_row2.isStructured()) {
        if(_row2.getDeleteVerticalSpansOfHiddenShapes()) deleteVerticalSpansOfHiddenShapes();
        else if(_row2.getShiftShapesBelowHiddenShapesUp()) shiftShapesBelowHiddenShapesUp();
    }
    
    // Do bindings RPG
    row.rpgBindings(anRptOwner, this);
    
    // Remove Group from RptOwner.DataStack
    anRptOwner.popDataStack();
}

/**
 * Returns the template.
 */
public RMTableRow getTemplate()  { return _row; }

/**
 * Returns the group.
 */
public RMGroup getGroup()  { return _group; }

/**
 * Returns whether this row is header.
 */
public boolean isHeader()  { return _row.getTitle().endsWith("Header"); }

/**
 * Returns whether this row is details.
 */
public boolean isDetails()  { return _row.getTitle().endsWith("Details"); }

/**
 * Returns whether this row is summary.
 */
public boolean isSummary()  { return _row.getTitle().endsWith("Summary"); }

/**
 * Returns the appropriate version.
 */
String getVersionName(ReportOwner anRptOwner, RMTableRow aRow, RMGroup aGroup, String aSuggVersion)
{
    // If SuggestedVersion is present, return it
    if(aSuggVersion!=null && aRow.hasVersion(aSuggVersion))
        return aSuggVersion;
    
    // If group isTopNOthers, check for "TopN Others"/"TopN Others Reprint" and use if available
    if(aGroup.isTopNOthers()) {
        if(RMTableRow.VersionReprint.equals(aSuggVersion) && aRow.hasVersion("TopN Others Reprint")) {
            aGroup.setTopNOthers(false); return "TopN Others Reprint"; }
        if(aRow.hasVersion("TopN Others")) {  // Suppress TopNOthers aggregation since Version exists
            aGroup.setTopNOthers(false); return "TopN Others"; }
    }

    // If VersionKey is set and evaluates to a present version, return it
    if(aRow.getVersionKey()!=null) {
        String version = RMKeyChain.getStringValue(anRptOwner, aRow.getVersionKey());
        if(version!=null && aRow.hasVersion(version))
            return version;
    }

    // Try for FirstOnly and Alternate
    int index = aGroup.getParent()!=null? aGroup.index() : 0;
    if(index==0 && aRow.hasVersion(RMTableRow.VersionFirstOnly))
        return RMTableRow.VersionFirstOnly;
    if(index%2==1 && aRow.hasVersion(RMTableRow.VersionAlternate))
        return RMTableRow.VersionAlternate;
    
    // Return Standard version
    return RMTableRow.VersionStandard;
}

/**
 * Returns the number of child rpgs.
 */
public int getChildRPGCount()  { return _childRPGs!=null? _childRPGs.size() : 0; }

/**
 * Adds a child row.
 */
public void addChildRPG(RMTableRowRPG aRow)
{
    if(_childRPGs==null) _childRPGs = new ArrayList();
    _childRPGs.add(aRow);
    aRow._parentRPG = this;
}

/**
 * Deletes vertical spans of hidden shapes.
 */
public void deleteVerticalSpansOfHiddenShapes()
{
    // Create list of spans
    SpanList spans = new SpanList();
    
    // Collect hidden shape spans
    for(RMShape child : getChildren())
        if(!child.isVisible())
            spans.addSpan(new Span(child.getFrameY(), getShapeBelowFrameY(this, child)));
    
    // Remove visible shape spans
    if(spans.size()>0)
        for(RMShape child : getChildren())
            if(child.isVisible())
                spans.removeSpan(new Span(child.getFrameY(), getShapeBelowFrameY(this, child)));
    
    // Sort spans and reverse
    Collections.sort(spans);
    Collections.reverse(spans);
    
    // Delete spans
    for(Span span : spans) {
        
        // Iterate over children and shift them up
        for(RMShape child : getChildren())
            if(child.getFrameY()>=span.end)
                child.setFrameY(child.getFrameY() - span.getLength());
        
        // Remove bottom of shape
        setHeight(getHeight() - span.getLength());
    }
}

/**
 * Returns the next shape y for a given parent and child (so we can find the gap).
 */
public static double getShapeBelowFrameY(RMParentShape aParent, RMShape aChild)
{
    double y = aParent.getHeight();
    for(RMShape child : aParent.getChildren())
        if(child!=aChild && child.getFrameY()>aChild.getFrameMaxY() && child.getFrameY()<y)
            y = child.getFrameY();
    return y;    
}

/**
 * Shifts shapes below hidden shapes up.
 */
public void shiftShapesBelowHiddenShapesUp()
{
    // If no hidden shapes, just return
    boolean vsbl = true;
    for(int i=0, iMax=getChildCount(); i<iMax && vsbl; i++) vsbl = getChild(i).isVisible(); if(vsbl) return;
    
    // Get max FrameMaxY and shapes sorted by FrameY and FrameX
    double maxFrameY = RMShapeUtils.getMaxFrameMaxY(getChildren());
    List <RMShape> shapes = RMShapeUtils.getShapesSortedByFrameYFrameX(getChildren());
    
    // Shift shapes for each hidden shape (from bottom up)
    for(int i=shapes.size()-1; i>=0; i--) { RMShape shape = shapes.get(i);
        if(!shape.isVisible())
            shiftShapesBelowHiddenRect(shapes, shape.getFrame());
    }
    
    // Get new max frame y and remove bottom of shape
    double maxFrameY2 = RMShapeUtils.getMaxFrameMaxY(getChildren());
    if(!MathUtils.equals(maxFrameY, maxFrameY2))
        setHeight(getHeight() + maxFrameY2 - maxFrameY);
    
    // Reset layout
    resetLayout();
}

/**
 * Shifts shapes below hidden rect up.
 */
public void shiftShapesBelowHiddenRect(List <RMShape> theShapes, Rect aRect)
{
    // Get rect for region below given rect
    Rect belowRect = aRect.clone();
    belowRect.setY(aRect.getMaxY()); belowRect.setHeight(getHeight() - belowRect.getY());
    
    // Iterate over shapes and get shape rects, minX/maxX/maxY and sort rects into static and floating lists
    List <Rect> staticRects = new ArrayList();
    List <Rect> floatingRects = new ArrayList();
    List <RMShape> floatingRectShapes = new ArrayList();
    
    // Iterate over shapes and get shape rects, minX/maxX/maxY and sort rects into static and floating lists
    for(RMShape shape : theShapes) {
        
        // If shape not visible, just continue
        if(!shape.isVisible()) continue;
        
        // Get rect and add to list
        Rect shapeRect = shape.getFrame();
        
        // If shape rect is below, add it to floating rects and expand below rect
        if(belowRect.intersectsRect(shapeRect)) {
            floatingRects.add(shapeRect);
            floatingRectShapes.add(shape);
            belowRect.union(shapeRect);
        }
        
        // Otherwise add to static rects
        else staticRects.add(shapeRect);
    }
    
    // Get max y of floating rects and height to shift rects
    double maxY = getHeight(); for(Rect rect : floatingRects) maxY = Math.min(maxY, rect.getY());
    double height = maxY - aRect.getY();
    
    // Add height to floating rects
    for(Rect rect : floatingRects) { rect.y -= height; rect.height += height; }
    
    // Iterate over floating rects
    for(int i=0, iMax=floatingRects.size(); i<iMax; i++) { Rect floatingRect = floatingRects.get(i);
        for(Rect staticRect : staticRects) {
            if(floatingRect.intersectsRect(staticRect)) {
                floatingRects.remove(i);
                floatingRectShapes.remove(i);
                floatingRect.y += height; floatingRect.height -= height;
                staticRects.add(floatingRect);
                i = -1; iMax = floatingRects.size();
                break;
            }
        }
    }
    
    // Shift remaining floating rect shapes
    for(RMShape shape : floatingRectShapes)
        shape.setFrameY(shape.getFrameY() - height);
}

/** Override to make selectable. */
public boolean superSelectable()  { return true; }

/** Override to paint stroke on top. */
public boolean isStrokeOnTop()  { return true; }

/**
 * Override to handle structured row.
 */
protected void layoutImpl()
{
    // If not structured, just do normal version
    if(!_row2.isStructured()) { super.layoutImpl(); return; }
    
    // Layout children edge to edge by iterating over children and setting successive x values
    double dx = 0;
    for(RMShape child : getChildren()) {
        child.setBounds(dx, 0, child.getWidth(), getHeight());
        dx += child.getWidth();
    }
}

/**
 * Override to handle structured row.
 */
protected double getPrefHeightImpl(double aWidth)
{
    // If not structured, just return normal version
    if(!_row2.isStructured()) return super.getPrefHeightImpl(aWidth);
    
    // Return max of current row height and max child best size
    double max = getHeight();
    for(RMShape child : getChildren()) max = Math.max(max, child.getPrefHeight());
    return max;
}

/**
 * A class to represent an interval 
 */
public static class Span implements Comparable {

    /** Creates a new span. */
    public Span(double aStart, double anEnd)  { this.start = aStart; this.end = anEnd; }  double start, end;
    
    /** Returns the span length. */
    public double getLength()  { return end - start; }
    
    /** Returns whether given value is contained in the span (inclusive). */
    public boolean contains(double aValue)  { return MathUtils.lte(start, aValue) && MathUtils.lte(aValue, end); }
    
    /** Returns whether given span intersects this span. */
    public boolean intersects(Span aSpan)
    {
        return MathUtils.equals(start, aSpan.start) || MathUtils.equals(end, aSpan.end) ||
               MathUtils.lt(aSpan.start, end) && MathUtils.gt(aSpan.end, start);
    }
    
    /** Returns string representation of span. */
    public String toString()  { return "Span { start: " + start + ", end: " + end + " }"; }

    /** Comparable implementation. */
    public int compareTo(Object aSpan)  { return new Double(start).compareTo(((Span)aSpan).start); }
}

/**
 * A class to represent a list of spans.
 */
public static class SpanList extends ArrayList <Span> {

    /** Adds a span to a list of spans, either by extending an existing span or actually adding it to the list. */
    public void addSpan(Span aSpan)
    {
        // If empty span, just return
        if(MathUtils.lte(aSpan.end, aSpan.start)) return;
        
        // Iterate over spans and extends any overlapping span (and return)
        for(Span span : this) {
            
            // If given span starts inside loop span and ends after, extend current span, remove from list and re-add
            if(span.contains(aSpan.start) && !span.contains(aSpan.end)) {
                span.end = aSpan.end; this.remove(span); addSpan(span); return; }
            
            // If given span starts before loop span and ends inside, extend current span, remove from list and re-add
            if(!span.contains(aSpan.start) && span.contains(aSpan.end)) {
                span.start = aSpan.start; this.remove(span); addSpan(span); return; }
            
            // If loop span contains given span, just return
            if(span.contains(aSpan.start) && span.contains(aSpan.end))
                return;
        }
        
        // Since no overlapping span, add span
        add(aSpan);
    }
    
    /** Removes a span from a list of spans, either by reducing a span or by removing a span. */
    public void removeSpan(Span aSpan)
    {
        // Iterate over spans and reduce any that need to be reduced
        for(Span span : this) {
            
            // If given span starts in loop span and ends outside, reduce loop span to given span start
            if(span.contains(aSpan.start) && !span.contains(aSpan.end))
                span.end = aSpan.start;
            
            // If given span starts outside loop span and ends in span, reset loop span start to given span end
            if(!span.contains(aSpan.start) && span.contains(aSpan.end))
                span.start = aSpan.end;
            
            // If loop span contains given span, remove given span and add two spans
            if(span.contains(aSpan.start) && span.contains(aSpan.end)) {
                this.remove(span); addSpan(new Span(span.start, aSpan.start));
                addSpan(new Span(aSpan.end, span.end)); return; }
            
            // If given span contains loop span, remove it and re-run
            if(aSpan.contains(span.start) && aSpan.contains(span.end)) {
                this.remove(span); removeSpan(aSpan); return; }
        }
    }
}

}