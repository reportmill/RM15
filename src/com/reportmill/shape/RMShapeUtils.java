/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import java.util.*;
import snap.gfx.*;

/**
 * Some shape utility methods.
 */
public class RMShapeUtils {

/**
 * Convenience to mark a whole list of shapes for repaint.
 */
public static void repaint(List <? extends RMShape> theShapes)
{
    // Iterate over shapes in list and call set needs repaint
    for(int i=0, iMax=theShapes.size(); i<iMax; i++)
        theShapes.get(i).repaint();
}

/**
 * Returns the bounds of a given subset of this shape's children.
 */
public static Rect getBoundsOfChildren(RMShape aShape, List <? extends RMShape> aList)
{
    // If list is null or empty, return this shape's bounds inside
    if(aList==null || aList.size()==0)
        return aShape.getBoundsInside();
    
    // Declare and initialize a rect to frame of first shape in list
    Rect rect = aList.get(0).getFrame();
    
    // Iterate over successive shapes in list and union their frames
    for(int i=1, iMax=aList.size(); i<iMax; i++) {
        RMShape child = aList.get(i);
        rect.unionEvenIfEmpty(child.getFrame());
    }
    
    // Return frame
    return rect;
}

/**
 * Adds the subset of children in given list into a given group shape, which is then added back to receiver.
 */
public static RMParentShape groupShapes(List <? extends RMShape> theShapes, RMParentShape groupShape)
{
    // Get copy of shapes, sorted by their original index in parent
    List <? extends RMShape> shapes = RMSort.sortedList(theShapes, "indexOf");
    
    // Get parent
    RMParentShape parent = shapes.get(0).getParent();
    
    // If no group shape, create one
    if(groupShape==null) {
        groupShape = new RMSpringShape();
        groupShape.setBounds(getBoundsOfChildren(parent, shapes));
    }

    // Add groupShape to the current parent (with no transform)
    parent.addChild(groupShape);

    // Remove children from current parent and add to groupShape
    for(RMShape child : shapes) {
        child.convertToShape(null);
        parent.removeChild(child);
        groupShape.addChild(child);
        child.convertFromShape(null);
    }

    // Return group shape
    return groupShape;
}

/**
 * Returns a polygon shape by combining paths of given shapes.
 */
public static RMPolygonShape getCombinedPathsShape(List <RMShape> theShapes)
{
    // Get first shape, parent and combined bounds
    RMShape shape0 = theShapes.size()>0? theShapes.get(0) : null; if(shape0==null) return null;
    RMShape parent = shape0.getParent();
    Rect combinedBounds = getBoundsOfChildren(parent, theShapes);
    
    // Get the path of the combined shapes
    Shape combinedPath = getCombinedPath(theShapes);

    // Create combined shape, configure and return
    RMPolygonShape shape = new RMPolygonShape(combinedPath);
    shape.copyShape(shape0); shape._rss = null;
    shape.setFrame(combinedBounds);
    return shape;
}

/**
 * Returns the combined path from given shapes.
 */
public static Shape getCombinedPath(List <RMShape> theShapes)
{
    List <Shape> paths = getPathsFromShapes(theShapes, 0); Shape s1 = paths.get(0);
    for(int i=1, iMax=paths.size(); i<iMax; i++) { Shape s2 = paths.get(i);
        s1 = Shape.add(s1, s2); }
    return s1;
}

/**
 * Returns a polygon shape by combining paths of given shapes.
 */
public static RMPolygonShape getSubtractedPathsShape(List <RMShape> theShapes, int anInset)
{
    // Get SubtractedPath by subtracting paths and its bounds
    Shape subtractedPath = getSubtractedPath(theShapes, 0);
    Rect subtractedBounds = subtractedPath.getBounds();

    // Create shape, configure and return
    RMPolygonShape shape = new RMPolygonShape(subtractedPath);
    shape.copyShape(theShapes.get(0)); shape._rss = null;
    shape.setBounds(subtractedBounds);
    return shape;
}

/**
 * Returns the combined path from given shapes.
 */
public static Shape getSubtractedPath(List <RMShape> theShapes, int anInset)
{
    // Eliminate shapes that don't intersect first shape frame
    RMShape shape0 = theShapes.get(0);
    Rect shape0Frame = shape0.getFrame();
    List <RMShape> shapes = theShapes;
    for(int i=shapes.size()-1; i>=0; i--) { RMShape shape = shapes.get(i);
        if(!shape.getFrame().intersects(shape0Frame)) {
            if(shapes==theShapes) shapes = new ArrayList(theShapes); shapes.remove(i); }}
    
    // Get shape paths, iterate over them, successively subtract them and return final
    List <Shape> paths = getPathsFromShapes(shapes, anInset); Shape s1 = paths.get(0);
    for(int i=1, iMax=paths.size(); i<iMax; i++) { Shape s2 = paths.get(i);
        s1 = Shape.subtract(s1, s2); }
    return s1;
}

/**
 * Returns the list of paths from the given shapes list.
 */
private static List <Shape> getPathsFromShapes(List <RMShape> theShapes, int anInset)
{
    // Get first shape and parent
    RMShape shape0 = theShapes.get(0);
    RMShape parent = shape0.getParent(); // Should probably get common ancestor

    // Iterate over shapes, get bounds of each (inset), path of each (in parent coords) and add to list
    List paths = new ArrayList(theShapes.size());
    for(int i=0, iMax=theShapes.size(); i<iMax; i++) { RMShape shape = theShapes.get(i);
        Rect bounds = shape.getBoundsInside(); if(anInset!=0 && i>0) bounds.inset(anInset);
        Shape path = shape.getPath().copyFor(bounds);
        path = shape.getConvertedToShape(path, parent);
        paths.add(path);
    }
    
    // Return paths list
    return paths;
}

/**
 * Returns an image for the given shape, with given background color (null for clear) and scale.
 */
public static Image createImage(RMShape aShape, Color aColor)
{
    // Get marked bounds for shape
    Rect bounds = aShape instanceof RMPage? aShape.getBounds() : aShape.getBoundsMarkedDeep();
    
    // Calculate image size from shape bounds and scale (rounded up to integral size)
    int w = (int)Math.ceil(bounds.getWidth());
    int h = (int)Math.ceil(bounds.getHeight());
    
    // If shape has no area, return empty image
    if(w==0 || h==0)
        return Image.get(1,1,false);
    
    // Create new image
    Image img = Image.get(w, h, aColor==null || aColor.getAlphaInt()!=255);
    
    // Create painter and configure
    Painter pntr = img.getPainter(); pntr.setImageQuality(1);
    
    // Fill background
    if(aColor!=null) {
        pntr.setColor(aColor); pntr.fillRect(0,0,w,h); }

    // Create shape painter and configure
    //RMShapePainter sp = new RMShapePainter(pntr); sp.setBounds(0,0,w,h); sp.setPrinting(true); sp.paintShape(aShape);
    
    // Paint shape and return image
    paintShape(pntr, aShape, new Rect(0,0,w,h), 1);
    pntr.flush();
    return img;
}

/**
 * Paints a simple shape.
 */
public static void paintShape(Painter aPntr, RMShape aShape, Rect aBounds, double aScale)
{
    // Validate shape
    if(aShape instanceof RMParentShape) ((RMParentShape)aShape).layout();
    
    // Get shape marked bounds
    Rect shapeBounds = aShape.getBoundsMarked();
    double shapeWidth = shapeBounds.getWidth();
    double shapeHeight = shapeBounds.getHeight();
    
    // Cache gstate
    aPntr.save();
    
    // If bounds are present, set transform to position content
    if(aBounds!=null) {
        
        // Get the discrepancy of bounds size and shape scaled size
        double dw = aBounds.getWidth() - shapeWidth*aScale;
        double dh = aBounds.getHeight() - shapeHeight*aScale;
        
        // Constrain alignment to bounds (maybe this should be an option)
        if(dw<0) dw = 0; if(dh<0) dh = 0;
        
        // Get the translations to bounds with specified alignments (don't allow alignment outside)
        double tx = aBounds.getX() + dw*.5, ty = aBounds.getY() + dh*.5;
        tx = Math.round(tx - .01); ty = Math.round(ty - .01); // Round down?
        aPntr.translate(tx, ty);
    }
    
    // Do scale
    if(aScale!=1)
        aPntr.scale(aScale, aScale);
    
    // Apply inverse shape transform to negate effects of shape paint applying transform
    aPntr.transform(aShape.getTransformInverse());

    // Paint shape and restore gstate
    aShape.paint(aPntr);
    aPntr.restore();
}

}