/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.gfx.*;

/**
 * Some shape utility methods.
 */
public class RMShapeUtils {

/**
 * Returns the average width of shapes.
 */
public static double getAverageWidth(List <RMShape> theShapes)
{
    double w = 0; for(RMShape s : theShapes) w += s.getWidth();
    return w/theShapes.size();
}

/**
 * Returns the average width of shapes.
 */
public static double getMaxFrameMaxY(List <RMShape> theShapes)
{
    double my = 0; for(RMShape s : theShapes) if(s.isVisible()) my = Math.max(my, s.getFrameMaxY());
    return my;
}

/**
 * Sorts given list of shapes by X.
 */
public static void sortByX(List <RMShape> theShapes)
{
    Collections.sort(theShapes, (s0,s1) -> compareShapeX(s0,s1));
}

/**
 * Returns a copy of given list of shapes sorted by X.
 */
public static List <RMShape> getShapesSortedByX(List <RMShape> theShapes)
{
    return getShapesSorted(theShapes, (s0,s1) -> compareShapeX(s0,s1));
}

/**
 * Returns a copy of given list of shapes sorted by FrameX.
 */
public static List <RMShape> getShapesSortedByFrameX(List <RMShape> theShapes)
{
    return getShapesSorted(theShapes, (s0,s1) -> compareShapeFrameX(s0,s1));
}

/**
 * Returns a copy of given list of shapes sorted by FrameY.
 */
public static List <RMShape> getShapesSortedByFrameY(List <RMShape> theShapes)
{
    return getShapesSorted(theShapes, (s0,s1) -> compareShapeFrameY(s0,s1));
}

/**
 * Returns a copy of given list of shapes sorted by FrameY.
 */
public static List <RMShape> getShapesSortedByFrameYFrameX(List <RMShape> theShapes)
{
    return getShapesSorted(theShapes, (s0,s1) -> compareShapeFrameYFrameX(s0,s1));
}

/**
 * Returns a copy of given list of shapes sorted by shape Index.
 */
public static List <RMShape> getShapesSortedByIndex(List <RMShape> theShapes)
{
    return getShapesSorted(theShapes, (s0,s1) -> compareShapeIndex(s0,s1));
}

/**
 * Returns a copy of given list of shapes sorted by Comparator.
 */
public static List <RMShape> getShapesSorted(List <RMShape> theShapes, Comparator <RMShape> aComp)
{
    List <RMShape> shapes = new ArrayList(theShapes);
    Collections.sort(shapes, aComp);
    return shapes;
}

/** Compare methods for Shape X, FrameX, FrameY, FrameYFrameX, Index */
private static int compareShapeX(RMShape s0, RMShape s1)
{ double v0 = s0.getX(), v1 = s1.getX(); return v0<v1? -1 : v0>v1? 1 : 0; }
private static int compareShapeFrameX(RMShape s0, RMShape s1)
{ double v0 = s0.getFrameX(), v1 = s1.getFrameX(); return v0<v1? -1 : v0>v1? 1 : 0; }
private static int compareShapeFrameY(RMShape s0, RMShape s1)
{ double v0 = s0.getFrameY(), v1 = s1.getFrameY(); return v0<v1? -1 : v0>v1? 1 : 0; }
private static int compareShapeFrameYFrameX(RMShape s0, RMShape s1)
{ int c = compareShapeFrameY(s0,s1); if(c==0) c = compareShapeFrameX(s0,s1); return c; }
private static int compareShapeIndex(RMShape s0, RMShape s1)
{ int v0 = s0.indexOf(), v1 = s1.indexOf(); return v0<v1? -1 : v0>v1? 1 : 0; }

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
    // Iterate over shapes, get bounds of each (inset), path of each (in parent coords) and add to list
    List paths = new ArrayList(theShapes.size());
    for(int i=0, iMax=theShapes.size(); i<iMax; i++) { RMShape shape = theShapes.get(i);
        Rect bounds = shape.getBoundsInside(); if(anInset!=0 && i>0) bounds.inset(anInset);
        Shape path = shape.getPath().copyFor(bounds);
        path = shape.localToParent(path);
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
    Image img = Image.getImageForSizeAndScale(w, h, aColor==null || aColor.getAlphaInt()!=255, 2);
    
    // Create painter and configure
    Painter pntr = img.getPainter();
    pntr.setPrinting(true);
    pntr.setImageQuality(1);
    
    // Fill background
    if(aColor!=null) {
        pntr.setColor(aColor); pntr.fillRect(0,0,w,h); }

    // Paint shape and return image
    layoutDeep(aShape);
    paintShape(pntr, aShape, new Rect(0,0,w,h), 1);
    return img;
}

/**
 * Makes sure shape layout is up to date.
 */
public static void layoutDeep(RMShape aShape)
{
    if(aShape instanceof RMParentShape) ((RMParentShape)aShape).layoutDeep();
}

/**
 * Paints a simple shape.
 */
public static void paintShape(Painter aPntr, RMShape aShape, Rect aBounds, double aScale)
{
    // Cache gstate
    aPntr.save();
    
    // If bounds are present, set transform to position content
    if(aBounds!=null) {
        
        // Get shape marked bounds
        Rect sbnds = aShape.getBoundsMarked();
        double sw = sbnds.getWidth();
        double sh = sbnds.getHeight();
    
        // Get the discrepancy of bounds size and shape scaled size
        double dw = aBounds.getWidth() - sw*aScale;
        double dh = aBounds.getHeight() - sh*aScale;
        
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
    aPntr.transform(aShape.getParentToLocal());

    // Paint shape and restore gstate
    aShape.paint(aPntr);
    aPntr.restore();
}

}