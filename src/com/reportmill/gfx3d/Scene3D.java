/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.gfx3d;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.ViewEvent;

/**
 * This class manages a list of shapes and transforms them to 3D representations for display based on
 * X and Y axis rotations.
 * 
 * 3D conventions:
 * 
 *   Coordinate system: Right handed (not left handed)
 *   Polygon front: Right hand rule (counter-clockwise defined polygons face forward)
 *   Transforms: Row major notation (as opposed to column major, points are assumed row vectors) 
 */
public class Scene3D {
    
    // Width, height, depth
    double         _width, _height, _depth = 40;
    
    // Rotation around y axis
    double         _yaw = 0;
    
    // Rotation around x axis
    double         _pitch = 0;
    
    // Rotation around z axis
    double         _roll = 0;
    
    // Offset from z axis
    double         _offsetZ = 0, _offsetZ2;
    
    // Whether to adjust Z to keep scene positive
    boolean        _adjustZ;
    
    // Perspective
    double         _focalLength = 60*72;
    
    // Whether to do simple 3d rendering effect by skewing geometry a little bit
    boolean        _pseudo3D;
    
    // The skew in radians along x/y axis when doing pseudo 3d
    double         _pseudoSkewX, _pseudoSkewY;
    
    // Coefficient of ambient reflection for shading
    double         _ka = .6f;
    
    // Coefficient of diffuse reflection for shading
    double         _kd = .5f;
    
    // Camera
    Vector3D       _camera = new Vector3D(0, 0, 1);
    
    // Lights
    Vector3D       _light = new Vector3D(0, 0, -1).normalize();
    
    // The currently cached transform 3d
    Transform3D    _xform3D;
    
    // List of Shape3ds - the model
    List <Shape3D> _shapes = new ArrayList();
    
    // List of Path3Ds - for rendering
    List <Path3D>  _paths = new ArrayList();
    
    // Whether paths list needs to be rebuilt
    boolean        _rebuildPaths;
    
    // Mouse drag variable - mouse drag last point
    Point          _pointLast;
    
    // used for shift-drag to indicate which axis to constrain rotation to
    int            _dragConstraint;
    
    // The PropChangeSupport
    protected PropChangeSupport    _pcs = PropChangeSupport.EMPTY;

    // Constants for mouse drag constraints
    public final int CONSTRAIN_NONE = 0;
    public final int CONSTRAIN_PITCH = 1;
    public final int CONSTRAIN_YAW = 2;
    
/**
 * Returns the width of the scene.
 */
public double getWidth()  { return _width; }

/**
 * Sets the width of the scene.
 */
public void setWidth(double aValue)
{
    if(aValue==_width) return;
    firePropChange("Width", _width, _width = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the height of the scene.
 */
public double getHeight()  { return _height; }

/**
 * Sets the height of the scene.
 */
public void setHeight(double aValue)
{
    if(aValue==_height) return;
    firePropChange("Height", _height, _height = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the depth of the scene.
 */
public double getDepth()  { return _depth; }

/**
 * Sets the depth of the scene.
 */
public void setDepth(double aValue)
{
    if(aValue==_depth) return;
    firePropChange("Depth", _depth, _depth = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the rotation about the Y axis in degrees.
 */
public double getYaw()  { return _yaw; }

/**
 * Sets the rotation about the Y axis in degrees.
 */
public void setYaw(double aValue)
{
    if(aValue==_yaw) return;
    firePropChange("Yaw", _yaw, _yaw = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the rotation about the X axis in degrees.
 */
public double getPitch()  { return _pitch; }

/**
 * Sets the rotation about the X axis in degrees.
 */
public void setPitch(double aValue)
{
    if(aValue==_pitch) return;
    firePropChange("Pitch", _pitch, _pitch = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the rotation about the Z axis in degrees.
 */
public double getRoll()  { return _roll; }

/**
 * Sets the rotation about the Z axis in degrees.
 */
public void setRoll(double aValue)
{
    if(aValue==_roll) return;
    firePropChange("Roll", _roll, _roll = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the focal length of the camera (derived from the field of view and with view size).
 */
public double getFocalLength()  { return _focalLength; }

/**
 * Sets the focal length of the camera. Two feet is normal (1728 points).
 */
public void setFocalLength(double aValue)
{
    if(aValue==_focalLength) return;
    firePropChange("FocalLength", _focalLength, _focalLength = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the Z offset of the scene (for zooming).
 */
public double getOffsetZ()  { return _offsetZ; }

/**
 * Sets the Z offset of the scene (for zooming).
 */
public void setOffsetZ(double aValue)
{
    if(aValue==_offsetZ) return;
    firePropChange("OffsetZ", _offsetZ, _offsetZ = aValue);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns whether to adjust Z to keep scene positive.
 */
public boolean isAdjustZ()  { return _adjustZ; }

/**
 * Sets whether to adjust Z to keep scene positive.
 */
public void setAdjustZ(boolean aValue)  { _adjustZ = aValue; rebuildPaths(); }

/**
 * Returns whether scene is rendered in pseudo 3d.
 */
public boolean isPseudo3D()  { return _pseudo3D; }

/**
 * Sets whether scene is rendered in pseudo 3d.
 */
public void setPseudo3D(boolean aFlag)
{
    if(_pseudo3D==aFlag) return;
    firePropChange("Pseudo3D", _pseudo3D, _pseudo3D = aFlag);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the skew angle for X by Z.
 */
public double getPseudoSkewX()  { return _pseudoSkewX; }

/**
 * Sets the skew angle for X by Z.
 */
public void setPseudoSkewX(double anAngle)
{
    if(anAngle==_pseudoSkewX) return;
    firePropChange("PseudoSkewX", _pseudoSkewX, _pseudoSkewX = anAngle);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the skew angle for Y by Z.
 */
public double getPseudoSkewY()  { return _pseudoSkewY; }

/**
 * Sets the skew angle for Y by Z.
 */
public void setPseudoSkewY(double anAngle)
{
    if(anAngle==_pseudoSkewY) return;
    firePropChange("PseudoSkewY", _pseudoSkewY, _pseudoSkewY = anAngle);
    rebuildPaths(); _xform3D = null;
}

/**
 * Returns the field of view of the camera (derived from focalLength).
 */
public double getFieldOfView()
{
    double height = Math.max(getWidth(), getHeight());
    double fieldOfView = Math.toDegrees(Math.atan(height/(2*_focalLength)));
    return fieldOfView*2;
}

/**
 * Sets the field of view of the camera.
 */
public void setFieldOfView(double aValue)
{
    double height = Math.max(getWidth(), getHeight());
    double tanTheta = Math.tan(Math.toRadians(aValue/2));
    double focalLength = height/(2*tanTheta);
    setFocalLength(focalLength);
}

/**
 * Sets some reasonable default view settings.
 */
public void setDefaultViewSettings()
{
    // Set defaults for pseudo 3d
    if(isPseudo3D()) { setPseudoSkewX(.3f); setPseudoSkewY(-.25f); setDepth(20); setFocalLength(60*72); }
    
    // Set defaults for true 3d
    else { setYaw(23); setPitch(12); setDepth(100); setFocalLength(8*72); }    
}

/**
 * Returns the camera as a vector.
 */
public Vector3D getCamera()  { return _camera; }

/**
 * Returns the scene light as a vector.
 */
public Vector3D getLight()  { return _light; }

/**
 * Returns the number of shapes in the shape list.
 */
public int getShapeCount()  { return _shapes.size(); }

/**
 * Returns the specific shape at the given index from the shape list.
 */
public Shape3D getShape(int anIndex)  { return _shapes.get(anIndex); }

/**
 * Adds a shape to the end of the shape list.
 */
public void addShape(Shape3D aShape)  { _shapes.add(aShape); rebuildPaths(); }

/**
 * Removes the shape at the given index from the shape list.
 */
public void removeShapes()  { _shapes.clear(); }

/**
 * Returns the transform 3d for the scene's camera.
 */
public Transform3D getTransform3D()
{
    // If already set, just return
    if(_xform3D!=null) return _xform3D;
    
    // If pseudo 3d, just return skewed transform
    if(isPseudo3D()) {
        Transform3D t = new Transform3D(); t.skew(_pseudoSkewX, _pseudoSkewY);
        t.perspective(getFocalLength());
        return t;
    }
    
    // Bar chart used to do this rotation to "make pitch always relative to camera" instead of rotate below
    //t.rotateY(_yaw); t.rotate(new Vector3D(1,0,0), _pitch); t.rotate(new Vector3D(0,0,1), _roll);
    
    // Normal transform: translate about center, rotate X & Y, translate by Z, perspective, translate back
    double midx = getWidth()/2, midy = getHeight()/2, midz = getDepth()/2;
    Transform3D t = new Transform3D(); t.translate(-midx, -midy, -midz);
    t.rotate(_pitch, _yaw, _roll);
    t.translate(0, 0, getOffsetZ() - _offsetZ2);
    t.perspective(getFocalLength());
    t.translate(midx, midy, midz);
    return _xform3D = t;
}

/**
 * Resets secondary Z offset to keep scene in positive axis space.
 */
protected void adjustZ()
{
    // Cache and clear scene3D Z offset and bar view Z offset
    double offsetZ = getOffsetZ(); setOffsetZ(0); _offsetZ2 = 0; _xform3D = null;
    
    // Get bounding box in camera coords with no Z offset
    double width = getWidth(), height = getHeight(), depth = getDepth();
    Path3D bbox = new Path3D(); bbox.moveTo(0, 0, 0); bbox.lineTo(0, 0, depth); bbox.lineTo(width, 0, depth);
    bbox.lineTo(width, 0, 0); bbox.lineTo(0, 0, 0); bbox.lineTo(0, height, 0);
    bbox.lineTo(0, height, depth); bbox.lineTo(width, height, depth); bbox.lineTo(width, height, 0); bbox.close();
    bbox.transform(getTransform3D());
    
    // Get offset Z of graph view from bounding box and restore original graph Z offset
    _offsetZ2 = bbox.getZMin(); setOffsetZ(offsetZ); _xform3D = null;
}

/**
 * Returns a point in camera coords for given point in local coords.
 */
public Point3D localToCamera(Point3D aPoint)  { return localToCamera(aPoint.x, aPoint.y, aPoint.z); }

/**
 * Returns a point in camera coords for given point in local coords.
 */
public Point3D localToCamera(double aX, double aY, double aZ)
{
    Point3D p = new Point3D(aX, aY, aZ); p.transform(getTransform3D()); return p;
}

/**
 * Returns a path in camera coords for given path in local coords.
 */
public Path3D localToCamera(Path3D aPath)  { return aPath.copyFor(getTransform3D()); }

/**
 * Returns the given vector in camera coords.
 */
public Vector3D localToCamera(Vector3D aV3D)  { return localToCameraForVector(aV3D.x, aV3D.y, aV3D.z); }

/**
 * Returns the given vector in camera coords.
 */
public Vector3D localToCameraForVector(double aX, double aY, double aZ)
{
    Vector3D v2 = new Vector3D(aX, aY, aZ); v2.transform(getTransform3D()); return v2;
}

/**
 * Returns whether a vector is facing camera.
 */
public boolean isFacing(Vector3D aV3D)  { return aV3D.isAway(getCamera(), true); }

/**
 * Returns whether a vector is facing away from camera.
 */
public boolean isFacingAway(Vector3D aV3D)  { return aV3D.isAligned(getCamera(), true); }

/**
 * Returns whether a Path3d is facing camera.
 */
public boolean isFacing(Path3D aPath)  { return isFacing(aPath.getNormal()); }

/**
 * Returns whether a Path3d is facing away from camera.
 */
public boolean isFacingAway(Path3D aPath)  { return isFacingAway(aPath.getNormal()); }

/**
 * Returns the specific Path3D at the given index from the display list.
 */
public List <Path3D> getPaths()  { if(_rebuildPaths) rebuildPathsNow(); return _paths; }

/**
 * Returns the number of Path3Ds in the display list.
 */
public int getPathCount()  { return getPaths().size(); }

/**
 * Returns the specific Path3D at the given index from the display list.
 */
public Path3D getPath(int anIndex)  { return getPaths().get(anIndex); }

/**
 * Adds a path to the end of the display list.
 */
protected void addPath(Path3D aShape)  { _paths.add(aShape); }

/**
 * Removes the shape at the given index from the shape list.
 */
protected void removePaths()  { _paths.clear(); }

/**
 * Called to indicate that paths list needs to be rebuilt.
 */
protected void rebuildPaths()  { _rebuildPaths = true; }

/**
 * Rebuilds display list of Path3Ds from Shapes.
 */
protected void rebuildPathsNow()
{
    // Adjust Z
    if(isAdjustZ()) adjustZ();
    
    // Remove all existing Path3Ds
    removePaths();
    
    // Iterate over shapes and add paths
    for(Shape3D shp : _shapes)
        addPathsForShape(shp);
    
    // Resort paths
    Path3D.sort(_paths);
    _rebuildPaths = false;
}

/**
 * Adds the paths for shape.
 */
protected void addPathsForShape(Shape3D aShape)
{
    // Get the camera transform & optionally align it to the screen
    Transform3D xform = getTransform3D();
    Color color = aShape.getColor();

    // Iterate over paths
    for(Path3D path3d : aShape.getPath3Ds()) {
        
        // Get path copy transformed by scene transform
        path3d = path3d.copyFor(xform);
        
        // Backface culling : Only add paths that face the camera
        if(!path3d.getNormal().isAligned(getCamera(), true)) {
            if(color!=null) setRenderColor(path3d, color);
            addPath(path3d);
        }
    }
}

/**
 * Sets the color for a 3d shape from a base color.
 */
protected void setRenderColor(Path3D aShape3D, Color aColor)
{
    // Get shape3d path3d and normal - If facing away from camera, negate normal
    Vector3D normal = aShape3D.getNormal();
    if(normal.isAligned(getCamera(), true))
        normal.negate();
        
    // Get dot product of shape3d surface normal and light source vector
    double normalDotLight = normal.getDotProduct(getLight());
    
    // Calculate color components based on original color, surface normal, reflection constants and light source
    double r = aColor.getRed()*_ka + aColor.getRed()*_kd*normalDotLight; r = Math.min(r,1);
    double g = aColor.getGreen()*_ka + aColor.getGreen()*_kd*normalDotLight; g = Math.min(g,1);
    double b = aColor.getBlue()*_ka + aColor.getBlue()*_kd*normalDotLight; b = Math.min(b,1);
    
    // Set new color
    aShape3D.setColor(new Color(r, g, b, aColor.getAlpha()));    
}

/**
 * Paints shape children.
 */
public void paintPaths(Painter aPntr)
{
    // Iterate over Path3Ds and paint
    List <Path3D> paths = getPaths();
    for(int i=0, iMax=paths.size(); i<iMax; i++) { Path3D child = paths.get(i);
        
        // Paint path and path layers
        paintPath3D(aPntr, child);
        if(child.getLayers().size()>0) for(Path3D layer : child.getLayers())
            paintPath3D(aPntr, layer);
    }
}

/**
 * Paints a Path3D.
 */
protected void paintPath3D(Painter aPntr, Path3D aPath3D)
{
    // Get path, fill and stroke
    Shape path = aPath3D.getPath();
    Paint fill = aPath3D.getColor(), stroke = aPath3D.getStrokeColor();
    
    // Get opacity and set if needed
    double op = aPath3D.getOpacity(), oldOP = 0;
    if(op<1) { oldOP = aPntr.getOpacity(); aPntr.setOpacity(op*oldOP); }
    
    // Do fill and stroke
    if(fill!=null) {
        aPntr.setPaint(fill); aPntr.fill(path); }
    if(stroke!=null) {
        aPntr.setPaint(stroke); aPntr.setStroke(aPath3D.getStroke()); aPntr.draw(path); }
        
    // Reset opacity if needed
    if(op<1) aPntr.setOpacity(oldOP);
}

/** Paints a Path3D with labels on sides. */
/*private void paintPath3DDebug(Painter aPntr, Path3D aPath3D, String aStr) {
    aPntr.setOpacity(.8); paintPath3D(aPntr, aPath3D); aPntr.setOpacity(1);
    Font font = Font.Arial14.getBold(); double asc = font.getAscent(); aPntr.setFont(font);
    Rect r = font.getStringBounds(aStr), r2 = aPath3D.getPath().getBounds();
    aPntr.drawString(aStr, r2.x + (r2.width - r.width)/2, r2.y + (r2.height - r.height)/2 + asc);
}*/

/**
 * Viewer method.
 */
public void processEvent(ViewEvent anEvent)
{
    // Do normal version
    if(anEvent.isConsumed()) return;
    
    // Handle MousePressed: Set last point to event location in scene coords and _dragConstraint
    if(anEvent.isMousePress()) {
        _pointLast = anEvent.getPoint(); //_valueAdjusting = true;
        _dragConstraint = CONSTRAIN_NONE;
    }
    
    // Handle MouseDragged
    else if(anEvent.isMouseDrag())
        mouseDragged(anEvent);
        
    // Handle MouseReleased
    //else if(anEvent.isMouseRelease()) { _valueAdjusting = false; repaint(); relayout(); }
}

/**
 * Viewer method.
 */
public void mouseDragged(ViewEvent anEvent)
{
    // Get event location in this scene shape coords
    Point point = anEvent.getPoint();

    // If pseudo3d, set skew using event offset
    if(isPseudo3D()) {
        setPseudoSkewX(getPseudoSkewX() + (point.x - _pointLast.x)/100);
        setPseudoSkewY(getPseudoSkewY() + (point.y - _pointLast.y)/100);
    }
    
    // If right-mouse, muck with perspective
    else if(anEvent.isShortcutDown())
        setOffsetZ(getOffsetZ() + _pointLast.y - point.y);
    
    // Otherwise, just do pitch and roll
    else {
        
        // Shift-drag constrains to just one axis at a time
        if(anEvent.isShiftDown()) {
            
            // If no constraint
            if(_dragConstraint==CONSTRAIN_NONE) {
                if(Math.abs(point.y-_pointLast.y)>Math.abs(point.x-_pointLast.x)) 
                    _dragConstraint = CONSTRAIN_PITCH;
                else _dragConstraint = CONSTRAIN_YAW;
            }
            
            // If Pitch constrained
            if(_dragConstraint==CONSTRAIN_PITCH)
                point.x = _pointLast.x;
            
            // If Yaw constrained
            else point.y = _pointLast.y;
        }
        
        // Set pitch & yaw
        setPitch(getPitch() + (point.y - _pointLast.y)/1.5f);
        setYaw(getYaw() - (point.x - _pointLast.x)/1.5f);
    }
    
    // Set last point
    _pointLast = point;
}

/**
 * Copy attributes of another scene.
 */
public void copy3D(Scene3D aScene3D)
{
    setDepth(aScene3D.getDepth());
    setYaw(aScene3D.getYaw()); setPitch(aScene3D.getPitch()); setRoll(aScene3D.getRoll());
    setFocalLength(aScene3D.getFocalLength()); setOffsetZ(aScene3D.getOffsetZ());
    setPseudo3D(aScene3D.isPseudo3D());
    setPseudoSkewX(aScene3D.getPseudoSkewX()); setPseudoSkewY(aScene3D.getPseudoSkewY());
}

/**
 * Add listener.
 */
public void addPropChangeListener(PropChangeListener aLsnr)
{
    if(_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
    _pcs.addPropChangeListener(aLsnr);
}

/**
 * Remove listener.
 */
public void removePropChangeListener(PropChangeListener aLsnr)  { _pcs.removePropChangeListener(aLsnr); }

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal)
{
    if(!_pcs.hasListener(aProp)) return;
    firePropChange(new PropChange(this, aProp, oldVal, newVal));
}

/**
 * Fires a given property change.
 */
protected void firePropChange(PropChange aPCE)  { _pcs.firePropChange(aPCE); }

}