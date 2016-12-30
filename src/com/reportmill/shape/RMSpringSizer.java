/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.shape.RMSpringLayout.SpringInfo;
import java.util.ArrayList;
import java.util.List;
import snap.util.ListUtils;

/**
 * A class that calculates pref sizes for a given shape and its children.
 */
class RMSpringSizer {

    // The parent shape to be sized
    RMParentShape    _parent;
    
    // The spring layout
    RMSpringLayout   _layout;

    // The parent best height
    double           _bh;
    
    // The children Boxes
    Box              _cboxes[];
    
    // Constants for positions above and below
    enum Position { Above, Below };

/**
 * Creates a new sizes for a given SpringLayout.
 */
public RMSpringSizer(RMParentShape aParent)  { _parent = aParent; _layout = (RMSpringLayout)aParent.getLayout(); }

/**
 * Resizes the given shape to accommodate any children inside who need to grow.
 */
public double getPrefHeight()  { if(_cboxes==null) getChildBoxes(); return _bh; }

/**
 * Resizes the given shape to accommodate any children inside who need to grow.
 */
public Box[] getChildBoxes()
{
    // If already calculated, just return
    if(_cboxes!=null) return _cboxes;
    
    // Get parent height
    double pheight = _bh = _parent.getHeight();
    
    // Get boxes
    _cboxes = new Box[_parent.getChildCount()];
    for(int i=0, iMax=_parent.getChildCount(); i<iMax; i++) { RMShape child = _parent.getChild(i);
        _cboxes[i] = new Box(child); }
    
    // Iterate over children to get list of those that need to grow
    List <Box> childrenToGrow = null;
    for(Box box : _cboxes)
        if(box.needsToGrow()) {
            if(childrenToGrow==null) childrenToGrow = new ArrayList();
            childrenToGrow.add(box);
        }
    
    // If no children need to grow, just return
    if(childrenToGrow==null) { _bh = pheight; return _cboxes; }

    // Iterate until we have grown all children in childrenToGrow list
    while(!childrenToGrow.isEmpty()) {
        
        // Get new height and declare index variable for shape with min best height for this interaction
        double newHeight = pheight;
        int index = 0;

        // Find the child from childrenToGrow with the min BestHeight requirement,
        //  the required height of it's parent (self), and set all shapes to heightStretches
        for(int i=0, iMax=childrenToGrow.size(); i<iMax; i++) { Box child = childrenToGrow.get(i);
            
            // Get child's height, best height and difference
            double childHeight = child.getHeight();
            double childBestHeight = Math.max(childHeight, child._bh);
            double heightGrowth = childBestHeight - childHeight;
            
            // Get child's autosize string and reset child to only heightStretches
            String asize = child._asize;
            if(asize.charAt(5)=='-') asize = child._asize = "---,-~-";

            // Get child StretchingHeight and add top/bottom margin to stretching height if it stretches
            double stretchingHeight = childHeight;
            if(asize.charAt(4)=='~') stretchingHeight += child.getY();
            if(asize.charAt(6)=='~') stretchingHeight += pheight - child.getMaxY();

            // Add growth times a ratio of stretching length::view length to Super
            double newHeightToAccomodateChildBestHeight = pheight + heightGrowth*stretchingHeight/childHeight;

            // Find smallest newHeight of self to accommodate at least one of the childrenToGrow's BestHeight
            if(i==0 || newHeightToAccomodateChildBestHeight<newHeight) {
                newHeight = newHeightToAccomodateChildBestHeight; index = i; }
        }

        // Move the child with the minimum BestHeight requirement to the front of the array
        ListUtils.moveToFront(childrenToGrow, index);

        // For each child in childrenToGrow adjust springs for those children and those above and below them
        for(int i=0, iMax=childrenToGrow.size(); i<iMax; i++) { Box child = childrenToGrow.get(i);
            
            // If child heightStrches but not topMarginStrches, have shapes above setOnlyBottomAndRightMarginStrchs
            String asize = child._asize;
            if(asize.charAt(5)=='~' && asize.charAt(4)=='-') {
                List <Box> childrenAbove = childrenWithPositionRelativeToChild(Position.Above, child);
                for(int k=0, kMax=ListUtils.size(childrenAbove); k<kMax; k++)
                    childrenAbove.get(k)._asize = "--~,--~";
            }

            // If child heightStrchs but not bottomMarginStrtchs, have shapes below setOnlyTopAndRightMarginStrchs
            if(asize.charAt(5)=='~' && asize.charAt(6)=='-') {
                List <Box> childrenBelow = childrenWithPositionRelativeToChild(Position.Below, child);
                for(int k=0, kMax=ListUtils.size(childrenBelow); k<kMax; k++)
                    childrenBelow.get(k)._asize = "--~,~--"; // Only top and right margins stretch
            }
        }

        // Set height of boxes to smallest height to accommodate child with minimum BestHeight requirement
        setHeight(_cboxes, pheight, pheight = _bh = newHeight);

        // Trim all childrenToGrow that have met BestHeight
        for(int i=childrenToGrow.size()-1;  i>=0; i--) { Box child = childrenToGrow.get(i);
            if(!child.needsToGrow())
                childrenToGrow.remove(i); }

        // Reset everyone's springs to their defaults
        for(Box child : _cboxes) child._asize = child._asize0;
    }
    
    // Return child boxes
    return _cboxes;
}

/**
 * A class to represent the children bounds.
 */
class Box extends snap.gfx.Rect {

    // The autosize settings
    String       _asize, _asize0;
    
    // The best size
    double       _bh;
    
    /** Creates a new box for a Node. */
    public Box(RMShape aShape)
    {
        SpringInfo sinfo = _layout.getSpringInfo(aShape);
        _asize = _asize0 = aShape.getAutosizing();
        _bh = _layout.getBestHeight(aShape);
        setRect(sinfo.x, sinfo.y, sinfo.width, sinfo.height);
    }
    
    /** Returns whether box needs to grow. */
    boolean needsToGrow()  { return getHeight() + .005 < _bh && getHeight()>0; }
    
    /** Returns whether the receiver intersects with the given rect (horizontally only). */
    public boolean widthsIntersect(Box r2)
    {
        if(this.width <= 0f || r2.width <= 0f) return false;
        if(this.x < r2.x) { if(this.x + this.width <= r2.x) return false; }
        else { if(r2.x + r2.width <= this.x) return false; }
        return true;
    }
}

/**
 * Returns a list of children of given shape with given relative position to given child shape.
 */
List <Box> childrenWithPositionRelativeToChild(Position aPosition, Box aChild)
{
    // Iterate over child boxes and get those that hasPositionRelativeToPeer
    List <Box> hits = null;
    for(Box child : _cboxes) {
        if(child==aChild) continue; // If given child, skip
        if(hasPositionRelativeToPeer(child, aPosition, aChild)) { // If child has relative position, add to list
            if(hits==null) hits = new ArrayList(); hits.add(child); }
    }
    
    // If no children are found with relative position, return null
    if(hits==null) return null;

    // For each child that has position relative to aChild, find shapes that have same position relative to them
    for(int i=0, iMax=hits.size(); i<iMax; i++) { Box hitChild = hits.get(i);

        // Get children who have position relative to aChild
        for(int j=0, jMax=_cboxes.length; j<jMax; j++) { Box child = _cboxes[j];
            
            // If child is given child or hit child, skip
            if(child==aChild || child==hitChild) continue;
            
            // If child isn't in hit list but has position relative to child in hit list, add child
            if(ListUtils.indexOfId(hits, child)==-1 && hasPositionRelativeToPeer(child, aPosition, hitChild))
                hits.add(child);
        }
    }

    // Return hit children
    return hits;
}

/**
 * Returns whether given shape has given position relative to other given shape.
 */
static boolean hasPositionRelativeToPeer(Box aShape, Position aPosition, Box aPeer)
{
    // If bounds widths intersect
    if(aShape.widthsIntersect(aPeer)) {
        if(aPosition==Position.Above) { // Check position above
            if(aShape.getMaxY() <= Math.min(aPeer.getMidY(),aPeer.getY()+10))
                return true; }
        else if(aShape.getY() >= Math.max(aPeer.getMidY(),aPeer.getMaxY()-10)) // Check position below
            return true;
    }
    
    // Return false since conditions of position weren't met
    return false;
}

/**
 * Returns the child rects for given parent height.
 */
public void setHeight(Box theRects[], double oH, double nH) //double oW, double nW, 
{
    // Iterate over children and calculate new bounds rects
    for(int i=0; i<theRects.length; i++) { Box rect = theRects[i];
        String asize = rect._asize;
        //boolean lms = asize.charAt(0)=='~', ws = asize.charAt(1)=='~', rms = asize.charAt(2)=='~';
        //double sw = (lms? x1 : 0) + (ws? w1 : 0) + (rms? oW - (x1 + w1) : 0), dw = nW - oW;
        boolean tms = asize.charAt(4)=='~', hs = asize.charAt(5)=='~', bms = asize.charAt(6)=='~';
        double x = rect.getX(), y = rect.getY(), w = rect.getWidth(), h = rect.getHeight();
        double sh = (tms? y : 0) + (hs? h : 0) + (bms? oH - (y + h) : 0), dh = nH - oH;
        if(tms && sh!=0) y += dh*y/sh; // if(lms && sw!=0) x += dw*x/sw; if(ws && sw!=0) w += dw*w/sw;
        if(hs && sh!=0) h += dh*h/sh; 
        rect.setRect(x, y, w, h);
    }
}

}