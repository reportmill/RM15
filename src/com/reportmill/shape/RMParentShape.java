/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.ArrayList;
import java.util.List;
import snap.gfx.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * A shape implementation that can have children.
 */
public class RMParentShape extends RMShape {

    // The children of this shape
    List <RMShape> _children = new ArrayList();
    
    // The layout manager for children
    RMShapeLayout  _layout = null;
    
    // Whether children need layout
    boolean        _needsLayout;
    
    // Whether layout is in the process of being done
    boolean        _inLayout;

    // The SourceURL
    WebURL         _sourceURL;
    
    // A listener to catch child PropChange (for editor undo)
    PropChangeListener  _childPCL;
    
    // A listener to catch child DeepChange (for editor undo)
    DeepChangeListener  _childDCL;
    
/**
 * Returns the number of children associated with this shape.
 */
public int getChildCount()  { return _children.size(); }

/**
 * Returns the child at the given index.
 */
public RMShape getChild(int anIndex)  { return _children.get(anIndex); }

/**
 * Returns the list of children associated with this shape.
 */
public List <RMShape> getChildren()  { return _children; }

/**
 * Adds the given child to the end of this shape's children list.
 */
public final void addChild(RMShape aChild)  { addChild(aChild, getChildCount()); }

/**
 * Adds the given child to this shape's children list at the given index.
 */
public void addChild(RMShape aChild, int anIndex)
{
    // If child already has parent, remove from parent
    if(aChild._parent!=null && aChild._parent!=this)
        aChild._parent.removeChild(aChild);
    
    // Add child to children list and set child's parent to this shape
    _children.add(anIndex, aChild);
    aChild.setParent(this);
    
    // Notify layout of add child
    if(_layout!=null) _layout.addChild(aChild);
    
    // If this shape has PropChangeListeners, start listening to children as well
    if(hasDeepChangeListener()) {
        aChild.addPropChangeListener(getChildPCL()); aChild.addDeepChangeListener(getChildDCL()); }
    
    // Fire property change
    firePropChange("Child", null, aChild, anIndex);
    
    // Register for repaint and validation
    relayout(); repaint();
}

/**
 * Remove's the child at the given index from this shape's children list.
 */
public RMShape removeChild(int anIndex)
{
    // Remove child from children list and clear parent
    RMShape child = _children.remove(anIndex);
    child.setParent(null);
    
    // Fire property change
    firePropChange("Child", child, null, anIndex);
    
    // Notify layout of remove child
    if(_layout!=null) _layout.removeChild(child);
    
    // Stop listening to PropertyChanges, repaint, revalidate and return
    child.removePropChangeListener(getChildPCL()); child.removeDeepChangeListener(getChildDCL());
    relayout(); repaint();
    return child;
}

/**
 * Removes the given child from this shape's children list.
 */
public int removeChild(RMShape aChild)
{
    int index = indexOfChild(aChild);
    if(index>=0) removeChild(index);
    return index;
}

/**
 * Returns the index of the given child in this shape's children list.
 */
public int indexOfChild(RMShape aChild)  { return ListUtils.indexOfId(_children, aChild); }

/**
 * Returns the last child of this shape.
 */
public RMShape getChildLast()  { return getChildCount()>0? getChild(getChildCount()-1) : null; }

/**
 * Returns a copy of the children as an array.
 */
public RMShape[] getChildArray()  { return _children.toArray(new RMShape[getChildCount()]); }

/**
 * Removes all children from this shape (in reverse order).
 */
public void removeChildren()  { for(int i=getChildCount()-1; i>=0; i--) removeChild(i); }

/**
 * Returns bounds of all children of this shape, which can sometimes differ from this shapes bounds.
 */
public Rect getBoundsOfChildren()
{
    // Iterate over (visible) children and union child frames
    Rect rect = null;
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        if(!child.isVisible()) continue;
        if(rect==null) rect = child.getFrame();
        else rect.unionEvenIfEmpty(child.getFrame());
    }
    
    // Return frame (or bounds inside if null)
    return rect!=null? rect : getBoundsInside();
}

/**
 * Returns first child found with the given name (called recursively on children if not found at current level).
 */
public RMShape getChildWithName(String aName)
{
    // Iterate over children to see if any match given name
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        if(aName.equals(child.getName()))
            return child; }

    // Iterate over children and forward call to them
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        if(child instanceof RMParentShape && ((child = ((RMParentShape)child).getChildWithName(aName)) != null))
            return child; }

    // Return null since no child of given name was found
    return null;
}

/**
 * Returns first child found with the given class (called recursively on children if not found at current level).
 */
public <T> T getChildWithClass(Class<T> aClass)
{
    // Iterate over children to see if any match given class
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        if(aClass.isInstance(child))
            return (T)child; }

    // Iterate over children and forward call to them
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { Object child = getChild(i);
        if(child instanceof RMParentShape && ((child=((RMParentShape)child).getChildWithClass(aClass)) != null))
            return (T)child; }

    // Return null since no child of given class was found
    return null;
}

/**
 * Returns all the shapes in the shape hierarchy of a particular class.
 */
public <T extends RMShape> List<T> getChildrenWithClass(Class<T> aClass)
{
    return getChildrenWithClass(aClass, new ArrayList());
}

/**
 * Adds all the shapes in the shape hierarchy of a particular class to the list.
 * Returns the list as a convenience.
 */
public <T extends RMShape> List<T> getChildrenWithClass(Class<T> aClass, List aList)
{
    // Iterate over children and add children with class 
    for(int i=0, iMax=getChildCount(); i<iMax; i++) {  RMShape child = getChild(i);
        if(aClass.isInstance(child))
            aList.add(child);
        else if(child instanceof RMParentShape)
            ((RMParentShape)child).getChildrenWithClass(aClass, aList);
    }
            
    // Return list
    return aList;
}

/**
 * Adds a deep change listener to shape to listen for shape changes and property changes received by shape.
 */
public void addDeepChangeListener(DeepChangeListener aLsnr)
{
    boolean first = !hasDeepChangeListener();
    super.addDeepChangeListener(aLsnr);
    if(first)   // If first listener, add for children
        for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
            child.addPropChangeListener(getChildPCL()); child.addDeepChangeListener(getChildDCL()); }
}

// Return Child PropChangeListener and DeepChangeListener
PropChangeListener getChildPCL()  { return _childPCL!=null? _childPCL : (_childPCL=pc -> childDidPropChange(pc)); }
DeepChangeListener getChildDCL()  { return _childDCL!=null? _childDCL : (_childDCL=(l,p) -> childDidDeepChange(l,p)); }

/** Called when child/descendant changes forward changes on to deep listeners. */
void childDidPropChange(PropChange aPC)  { _pcs.fireDeepChange(this, aPC); }
void childDidDeepChange(Object aLsnr, PropChange aPC)  { _pcs.fireDeepChange(aLsnr, aPC); }

/**
 * Returns whether Source URL is set.
 */
public boolean isSourceURLSet()  { return _sourceURL!=null; }

/**
 * Returns the Source URL.
 */
public WebURL getSourceURL()  { return _sourceURL!=null? _sourceURL : _parent!=null? _parent.getSourceURL() : null; }

/**
 * Sets the Source URL.
 */
public void setSourceURL(WebURL aURL)  { _sourceURL = aURL; }

/**
 * Returns whether children need to be laid out.
 */
public boolean getNeedsLayout()  { return _needsLayout; }

/**
 * Sets whether children need to be laid out.
 */
public void setNeedsLayout(boolean aValue)  { _needsLayout = aValue; }

/**
 * Sets shape layout to invalid and requests deferred layout.
 */
public void relayout()  { setNeedsLayout(true); }

/**
 * Does immediate layout of this shape and children (if invalid).
 */
public void layout()
{
    // Validate deep then do layout
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        if(child instanceof RMParentShape) ((RMParentShape)child).layout(); }
    
    // If layout needed, do layout
    if(getNeedsLayout() && !_inLayout) {
        undoerDisable(); _inLayout = true;
        layoutImpl(); setNeedsLayout(false);
        undoerEnable(); _inLayout = false;
    }
}

/**
 * Called to reposition/resize children.
 */
protected void layoutImpl()  { if(_layout!=null) _layout.layout(); }

/**
 * Override to get from layout, if set.
 */
protected double getPrefHeightImpl(double aWidth)
{
    return _layout!=null? _layout.getPrefHeight(aWidth) : super.getPrefHeightImpl(-1);
}

/**
 * Returns the layout for this shape.
 */
public RMShapeLayout getLayout()  { return _layout; }

/**
 * Sets the layout for this shape.
 */
protected void setLayout(RMShapeLayout aLayout)
{
    _layout = aLayout; _layout._parent = this;
    for(RMShape c : getChildren()) _layout.addChild(c);
}

/**
 * Returns whether given child shape is hittable.
 */
protected boolean isHittable(RMShape aChild)  { return aChild.isVisible(); }

/**
 * Override to trigger layout.
 */
public void setWidth(double aValue)  { super.setWidth(aValue); relayout(); }

/**
 * Override to trigger layout.
 */
public void setHeight(double aValue)  { super.setHeight(aValue); relayout(); }

/**
 * Returns the first (top) shape hit by the point given in this shape's coords.
 */
public RMShape getChildContaining(Point aPoint)
{
    // Iterate over children
    for(int i=getChildCount()-1; i>=0; i--) { RMShape child = getChild(i);
        if(!child.isHittable()) continue; // Get current loop child
        Point point = child.parentToLocal(aPoint); // Get point converted to child
        if(child.contains(point)) // If child contains point, return child
            return child;
    }
    
    // Return null if no child contains point
    return null;
}

/**
 * Returns the child shapes hit by the path given in this shape's coords.
 */
public List <RMShape> getChildrenIntersecting(Shape aPath)
{
    // Create list for intersecting children
    List hit = new ArrayList();

    // Iterate over children
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        
        // If not hittable, continue
        if(!child.isHittable()) continue;
        
        // If child frame doesn't intersect path, just continue
        if(!child.getFrame().intersectsEvenIfEmpty(aPath.getBounds()))
            continue;
        
        // Get path converted to child and if child intersects path, add child to hit list
        Shape path = child.parentToLocal(aPath);
        if(child.intersects(path))
            hit.add(child);
    }
    
    // Return hit list
    return hit;
}

/**
 * Divides the shape by a given amount from the top. Returns a clone of the given shape with bounds 
 * set to the remainder. Divides children among the two shapes (recursively calling divide shape for those straddling).
 */
public RMShape divideShapeFromTop(double anAmount)
{
    // Validate
    layout();
    
    // Call normal divide from top edge
    RMParentShape bottomShape = (RMParentShape)super.divideShapeFromTop(anAmount);
    
    // Iterate over children to see if they belong to self or newShape (or need to be recursively split)
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        
        // Get child min y
        double childMinY = child.getFrameY();
    
        // If child is below border move it to new y in BottomShape
        if(childMinY>=getHeight()) {
            child._y = childMinY - getHeight();
            bottomShape.addChild(child);
            i--; iMax--; // Reset counters for removed child 
        }
    
        // If child stradles border, divide it and add divided part to newShape
        else if(child.getFrameMaxY()>getHeight()) {
            
            // Get child top/bottom height and divide from top by amount
            double childTopHeight = getHeight() - childMinY; // , cbh = child.getHeight() - childTopHeight;
            RMShape childBottom = child.divideShapeFromTop(childTopHeight);
            
            // Move new child bottom shape to new y in BottomShape
            childBottom._y = 0;
            bottomShape.addChild(childBottom);
            
            // If child bottom grew, grow bottom shape and reset layout? (I don't think this can happen)
            //if(cb.getHeight()!=cbh) { bms.setHeight(bms.getHeight() + cb.getHeight() - cbh); bms.layoutReset(); }
            
            // Reset autosizing so that child bottom is nailed to bottomShape top
            if(_layout!=null) {
                StringBuffer as = new StringBuffer(childBottom.getAutosizing()); as.setCharAt(4, '-');
                childBottom.setAutosizing(as.toString());
            }
        }
    }

    // Reset shape layout to suppress layout and return bottomShape
    if(_layout!=null) _layout.reset();
    return bottomShape;
}

/**
 * Moves the subset of children in the given list to the front of the children list.
 */
public void bringShapesToFront(List <RMShape> shapes)
{
    for(RMShape shape : shapes) {
        removeChild(shape);
        addChild(shape);
    }
}

/**
 * Moves the subset of children in the given list to the back of the children list.
 */
public void sendShapesToBack(List <RMShape> shapes)
{
    for(int i=0, iMax=shapes.size(); i<iMax; i++) { RMShape shape = shapes.get(i);
        removeChild(shape);
        addChild(shape, i);
    }
}

/**
 * Generate report with report owner.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    RMParentShape clone = (RMParentShape)rpgShape(anRptOwner, aParent);
    rpgBindings(anRptOwner, clone);
    clone = (RMParentShape)rpgChildren(anRptOwner, clone);
    return clone;
}

/**
 * Generate report with report owner.
 */
protected RMShape rpgChildren(ReportOwner anRptOwner, RMParentShape aParent)
{
    RMParentShape parent = aParent;
    ReportOwner.ShapeList slists[] = null;
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        RMShape crpg = anRptOwner.rpg(child, aParent);
        if(crpg instanceof ReportOwner.ShapeList) {
            if(slists==null) slists = new ReportOwner.ShapeList[iMax];
            slists[i] = (ReportOwner.ShapeList)crpg;
            aParent.addChild(crpg.getChild(0));
        }
        else aParent.addChild(crpg);
    }
    
    // If ShapesList child was encountered, create a ShapesList for this shape
    if(slists!=null) {
        int iMax = 0;
        for(ReportOwner.ShapeList slist : slists) if(slist!=null) iMax = Math.max(iMax, slist.getChildCount());
        parent = new ReportOwner.ShapeList();
        parent.addChild(aParent);
        for(int i=1; i<iMax; i++) { RMParentShape page = clone(); parent.addChild(page);
            for(int j=0; j<slists.length; j++) { ReportOwner.ShapeList slist = slists[j];
                if(slist==null) {
                    RMShape ch = aParent.getChild(j), clone = ch.cloneDeep();
                    page.addChild(clone);
                    if(ListUtils.containsId(anRptOwner.getPageReferenceShapes(),ch))
                        anRptOwner.addPageReferenceShape(clone);
                }
                else if(i<slist.getChildCount())
                    page.addChild(slist.getChild(i));
            }
        }
    }
    
    // Return parent
    return parent;
}

/**
 * Standard implementation of Object clone. Null's out shape's parent and children.
 */
public RMParentShape clone()
{
    RMParentShape clone = (RMParentShape)super.clone();
    clone._children = new ArrayList();
    if(_layout!=null) clone.setLayout(_layout.clone());
    return clone;
}

/**
 * Clones all attributes of this shape with complete clones of its children as well.
 */
public RMParentShape cloneDeep()
{
    RMParentShape clone = clone();
    for(int i=0, iMax=getChildCount(); i<iMax; i++) clone.addChild(getChild(i).cloneDeep());
    return clone;
}

/**
 * XML Archival generic - break toXML into toXMLShape and toXMLShapeChildren.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = toXMLShape(anArchiver); // Archive shape
    toXMLChildren(anArchiver, e); // Archive children
    return e; // Return xml element
}

/**
 * XML Archival of basic shape.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)  { return super.toXML(anArchiver); }

/**
 * XML archival of children.
 */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
{
    // Archive children
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        anElement.add(anArchiver.toXML(child, this)); }    
}

/**
 * XML unarchival generic - break fromXML into fromXMLShape and fromXMLShapeChildren.
 */
public RMShape fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Legacy
    if(getClass()==RMParentShape.class) {
        //if(anElement.getElement("layout")!=null) return new RMFlowShape().fromXML(anArchiver, anElement);
        if(anElement.getName().equals("shape")) return new RMSpringShape().fromXML(anArchiver, anElement);
    }
    
    // Unarchive shape and children and return
    fromXMLShape(anArchiver, anElement); // Unarchive shape
    fromXMLChildren(anArchiver, anElement); // Unarchive children
    return this;
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)  { super.fromXML(anArchiver,anElement); }

/**
 * XML unarchival for shape children.
 */
protected void fromXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
{
    // Iterate over child elements and unarchive shapes
    for(int i=0, iMax=anElement.size(); i<iMax; i++) { XMLElement childXML = anElement.get(i);
        
        // Get child class - if RMShape, unarchive and add
        Class childClass = anArchiver.getClass(childXML.getName());
        if(childClass!=null && RMShape.class.isAssignableFrom(childClass)) {
            RMShape shape = (RMShape)anArchiver.fromXML(childXML, this);
            addChild(shape);
        }
    }
}

}