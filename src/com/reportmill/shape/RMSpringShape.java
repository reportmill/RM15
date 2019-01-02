/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.gfx.*;
import snap.util.*;

/**
 * A parent shape that does child layout according to autosizing (springs and struts) settings.
 */
public class RMSpringShape extends RMParentShape {
    
    // Whether springs resizing is disabled
    boolean             _springsDisabled;
    
    // The parent best height
    double              _bh;
    
    // The children Boxes
    Box                 _cboxes[];
    
    // The PropChangeListener to notify of changes in child
    PropChangeListener  _childLsnr;
    
    // Constants for positions above and below
    enum Position { Above, Below };

/**
 * Return whether springs have been disabled.
 */
public boolean isSpringsDisabled()  { return _springsDisabled; }

/**
 * Return whether springs have been disabled.
 */
public void setSpringsDisabled(boolean aValue)  { _springsDisabled = aValue; }

/**
 * Override to layout children based on autosizing setting.
 */
protected void layoutImpl()
{
    Rect rects[] = getChildBounds();
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i); Rect rect = rects[i];
        child.setFrame(rect.x, rect.y, rect.width, rect.height); }
}

/**
 * Override to get from layout, if set.
 */
protected double getPrefHeightImpl(double aWidth) { if(_cboxes==null) getChildBoxes(); return _bh; }

/**
 * Returns the bounds of children based on simple spring resizing.
 */
private Rect[] getChildBounds()
{
    // If ChildBoxes is set, return them
    if(_cboxes!=null) return _cboxes;
    
    // Get original child rects
    int ccount = getChildCount();
    double newPW = getWidth(), newPH = getHeight();
    
    // Iterate over children and calculate new bounds rect for original child bounds and new parent width/height
    Rect rects[] = new Rect[ccount];
    for(int i=0; i<ccount; i++) { RMShape child = getChild(i); SpringInfo sinfo = getSpringInfo(child);
    
        // Create rect and update for new width/height
        Rect rect = rects[i] = new Rect(sinfo.x, sinfo.y, sinfo.width, sinfo.height);
        double oldPW = sinfo.pwidth, oldPH = sinfo.pheight; if(newPW==oldPW && newPH==oldPH) continue;
        String asize = child.getAutosizing();
        
        // Set new width/height
        setWidth(rect, asize, oldPW, newPW);
        setHeight(rect, asize, oldPH, newPH);
    }
    
    // Return rects
    return rects;
}

/**
 * Sets the rect width for given parent new/old width and autosizing
 */
private static void setWidth(Rect rect, String asize, double oldWidth, double newWidth)
{
    // Get setting for whether left-stretches, width-stretches and right-stretches
    boolean lms = asize.charAt(0)=='~', ws = asize.charAt(1)=='~', rms = asize.charAt(2)=='~';
    
    // Get rect x/width, stretch width and width change
    double rx = rect.x, rw = rect.width;
    double sw = (lms? rx : 0) + (ws? rw : 0) + (rms? oldWidth - (rx + rw) : 0);
    double dw = newWidth - oldWidth;
    
    // Update rect x/width
    if(lms) rect.x += sw>0? dw*rx/sw : dw;
    if(ws && sw!=0) rect.width += dw*rw/sw; 
}

/**
 * Sets the rect height for given parent new/old heights and autosizing
 */
private static void setHeight(Rect rect, String asize, double oldHeight, double newHeight)
{
    // Get setting for whether top-stretches, height-stretches and bottom-stretches
    boolean tms = asize.charAt(4)=='~', hs = asize.charAt(5)=='~', bms = asize.charAt(6)=='~';
    
    // Get rect y/height, stretch height and height change
    double ry = rect.y, rh = rect.height;
    double sh = (tms? ry : 0) + (hs? rh : 0) + (bms? oldHeight - (ry + rh) : 0);
    double dh = newHeight - oldHeight;
    
    // Update rect y/height
    if(tms) rect.y += sh>0? dh*ry/sh : dh;
    if(hs && sh!=0) rect.height += dh*rh/sh; 
}

/**
 * Returns the bounds of children accounting for springs settings and children that want to grow.
 */
public Box[] getChildBoxes()
{
    // If already calculated, just return
    if(_cboxes!=null) return _cboxes;
    
    // Get parent height
    double pheight = _bh = getHeight();
    
    // Get boxes
    _cboxes = new Box[getChildCount()];
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        _cboxes[i] = new Box(child, getSpringInfo(child)); }
    
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
        for(Box rect : _cboxes) setHeight(rect, rect._asize, pheight, newHeight);
        pheight = _bh = newHeight;

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
 * Returns a list of children of given shape with given relative position to given child shape.
 */
private List <Box> childrenWithPositionRelativeToChild(Position aPos, Box aChild)
{
    // Iterate over child boxes and get those that hasPositionRelativeToPeer
    List <Box> hits = null;
    for(Box child : _cboxes) {
        if(child==aChild) continue; // If given child, skip
        if(hasPositionRelativeToPeer(child, aPos, aChild)) { // If child has relative position, add to list
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
            if(ListUtils.indexOfId(hits, child)==-1 && hasPositionRelativeToPeer(child, aPos, hitChild))
                hits.add(child);
        }
    }

    // Return hit children
    return hits;
}

/**
 * Returns spring info for child.
 */
protected SpringInfo getSpringInfo(RMShape aChild)  { return (SpringInfo)aChild._springInfo; }

/**
 * Adds spring info for child.
 */
protected void addSpringInfo(RMShape aChild)
{
    double x = aChild.getFrameX(), y = aChild.getFrameY(), w = aChild.getFrameWidth(), h = aChild.getFrameHeight();
    SpringInfo sinfo = new SpringInfo(x,y,w,h,getWidth(),getHeight());
    aChild._springInfo = sinfo;
}

/**
 * Removes spring info for child.
 */
protected void removeSpringInfo(RMShape aChild)  { aChild._springInfo = null; }

/**
 * Override to initialize child springs.
 */
public void addChild(RMShape aChild, int anIndex)
{
    super.addChild(aChild, anIndex); if(_springsDisabled) return;
    aChild.addPropChangeListener(_childLsnr!=null? _childLsnr : (_childLsnr = pc -> childPropChanged(pc)));
    addSpringInfo(aChild); _cboxes = null;
}

/**
 * Override to remove child springs.
 */
public RMShape removeChild(int anIndex)
{
    RMShape child = super.removeChild(anIndex); if(_springsDisabled) return child;
    child.removePropChangeListener(_childLsnr);
    removeSpringInfo(child); _cboxes = null;
    return child;
}

/**
 * Override to paint dashed box around bounds.
 */
protected void paintShape(Painter aPntr)
{
    // Do normal version
    super.paintShape(aPntr); if(getClass()!=RMSpringShape.class) return;
    
    // Paint dashed box around bounds
    RMShapePaintProps props = RMShapePaintProps.get(aPntr);
    if(props.isEditing() && getStroke()==null && getFill()==null && getEffect()==null &&
        (props.isSelected(this) || props.isSuperSelected(this))) {
        aPntr.setColor(Color.LIGHTGRAY); aPntr.setStroke(Stroke.Stroke1.copyForDashes(3,2));
        aPntr.setAntialiasing(false); aPntr.draw(getBoundsInside()); aPntr.setAntialiasing(true);
    }
}

/**
 * Resets layout.
 */
protected void resetLayout()
{
    for(RMShape child : getChildren()) addSpringInfo(child);
    setNeedsLayout(false);
}

/**
 * Override to reset layout.
 */
public RMShape divideShapeFromTop(double anAmount)
{
    RMShape btmShape = super.divideShapeFromTop(anAmount);
    resetLayout();
    return btmShape;
}

/**
 * Called to update child SpringInfo on bounds change.
 */
protected void childPropChanged(PropChange anEvent)
{
    // If InLayout (we caused property change), just return
    if(_inLayout) return;

    // Get property name - if frame changer, do something
    String pname = anEvent.getPropName();
    if(pname=="X" || pname=="Y" || pname=="Width" || pname=="Height" || pname=="Roll" || pname=="Autosizing") {
        RMShape child = (RMShape)anEvent.getSource();
        addSpringInfo(child);
    }
}

/**
 * Returns whether given shape has given position relative to other given shape.
 */
private static boolean hasPositionRelativeToPeer(Box aShape, Position aPos, Box aPeer)
{
    // If bounds widths intersect
    if(aShape.widthsIntersect(aPeer)) {
        if(aPos==Position.Above) { // Check position above
            if(aShape.getMaxY() <= Math.min(aPeer.getMidY(),aPeer.getY()+10))
                return true; }
        else if(aShape.getY() >= Math.max(aPeer.getMidY(),aPeer.getMaxY()-10)) // Check position below
            return true;
    }
    
    // Return false since conditions of position weren't met
    return false;
}

/**
 * A class to hold info for a spring child.
 */
private static class SpringInfo {
    
    // The bounds and original parent width/height
    double x, y, width, height, pwidth, pheight;
    
    /** Creates a SpringInfo. */
    public SpringInfo(double aX, double aY, double aW, double aH, double aPW, double aPH) {
        x = aX; y = aY; width = aW; height = aH; pwidth = aPW; pheight = aPH; }
}

/**
 * A class to represent the children bounds.
 */
private static class Box extends snap.gfx.Rect {

    // The autosize settings
    String       _asize, _asize0;
    
    // The best size
    double       _bh;
    
    /** Creates a new box for a Node. */
    public Box(RMShape aShape, SpringInfo sinfo)
    {
        _asize = _asize0 = aShape.getAutosizing();
        _bh = aShape.getBestHeight();
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

}