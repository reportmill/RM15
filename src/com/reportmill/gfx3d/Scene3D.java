/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.gfx3d;
import java.util.*;
import snap.util.*;

/**
 * This class manages a list of shapes, cameras and lights.
 */
public class Scene3D {
    
    // Rotation around y axis
    double         _yaw = 0;
    
    // Rotation around x axis
    double         _pitch = 0;
    
    // Rotation around z axis
    double         _roll = 0;
    
    // Camera
    Camera         _camera;
    
    // Lights
    Vector3D       _light = new Vector3D(0, 0, -1).normalize();
    
    // The currently cached transform 3d
    Transform3D    _xform3D;
    
    // List of Shape3ds - the model
    List <Shape3D> _shapes = new ArrayList();
    
    // The PropChangeSupport
    protected PropChangeSupport    _pcs = PropChangeSupport.EMPTY;

/**
 * Creates a Scene.
 */
public Scene3D()  { _camera = new Camera(); _camera.setScene(this); }

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
    firePropChange("Yaw", _yaw, _yaw = aValue); //rebuildPaths(); _xform3D = null;
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
    firePropChange("Pitch", _pitch, _pitch = aValue); //rebuildPaths(); _xform3D = null;
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
    firePropChange("Roll", _roll, _roll = aValue); //rebuildPaths(); _xform3D = null;
}

/**
 * Returns the camera as a vector.
 */
public Camera getCamera()  { return _camera; }

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
public void addShape(Shape3D aShape)  { _shapes.add(aShape); _camera.rebuildPaths(); }

/**
 * Removes the shape at the given index from the shape list.
 */
public void removeShapes()  { _shapes.clear(); _camera.rebuildPaths(); }

/**
 * Returns the transform 3d for the scene's camera.
 */
public Transform3D getTransform()
{
    Transform3D t = new Transform3D();
    t.rotate(_pitch, _yaw, _roll);
    return _xform3D = t;
}

/**
 * Returns the transform 3d for the scene's camera.
 */
public Transform3D getLocalToCamera()  { return _camera.getTransform(); }

/**
 * Returns a point in camera coords for given point in local coords.
 */
public Point3D localToCamera(Point3D aPoint)  { return localToCamera(aPoint.x, aPoint.y, aPoint.z); }

/**
 * Returns a point in camera coords for given point in local coords.
 */
public Point3D localToCamera(double aX, double aY, double aZ)  { return getLocalToCamera().transformPoint(aX, aY, aZ); }

/**
 * Returns a path in camera coords for given path in local coords.
 */
public Path3D localToCamera(Path3D aPath)  { return aPath.copyFor(getLocalToCamera()); }

/**
 * Returns the given vector in camera coords.
 */
public Vector3D localToCamera(Vector3D aV3D)  { return localToCameraForVector(aV3D.x, aV3D.y, aV3D.z); }

/**
 * Returns the given vector in camera coords.
 */
public Vector3D localToCameraForVector(double aX, double aY, double aZ)
{
    Vector3D v2 = new Vector3D(aX, aY, aZ); v2.transform(getLocalToCamera()); return v2;
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