/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.gfx3d.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.ViewEvent;

/**
 * This encapsulates a Snap Scene3D to render simple 3d.
 */
public class RMScene3D extends RMParentShape {
    
    // A Scene3D to do real scene management
    Scene3D        _scene = new Scene3D();
    
    // List of real child shapes
    List <RMShape> _rmshapes = new ArrayList();

/**
 * Creates an RMScene3D.
 */
public RMScene3D()
{
    _scene.addPropChangeListener(pce -> sceneChanged(pce));
}

/**
 * Returns the Scene3D.
 */
public Scene3D getScene()  { return _scene; }

/**
 * Returns the depth of the scene.
 */
public double getDepth()  { return _scene.getDepth(); }

/**
 * Sets the depth of the scene.
 */
public void setDepth(double aValue)  { _scene.setDepth(aValue); }

/**
 * Returns the rotation about the Y axis in degrees.
 */
public double getYaw()  { return _scene.getYaw(); }

/**
 * Sets the rotation about the Y axis in degrees.
 */
public void setYaw(double aValue)  { _scene.setYaw(aValue); }

/**
 * Returns the rotation about the X axis in degrees.
 */
public double getPitch()  { return _scene.getPitch(); }

/**
 * Sets the rotation about the X axis in degrees.
 */
public void setPitch(double aValue)  { _scene.setPitch(aValue); }

/**
 * Returns the rotation about the Z axis in degrees.
 */
public double getRoll3D()  { return _scene.getRoll(); }

/**
 * Sets the rotation about the Z axis in degrees.
 */
public void setRoll3D(double aValue)  { _scene.setRoll(aValue); }

/**
 * Returns the focal length of the camera (derived from the field of view and with view size).
 */
public double getFocalLength()  { return _scene.getFocalLength(); }

/**
 * Sets the focal length of the camera. Two feet is normal (1728 points).
 */
public void setFocalLength(double aValue)  { _scene.setFocalLength(aValue); }

/**
 * Returns the Z offset of the scene (for zooming).
 */
public double getOffsetZ()  { return _scene.getOffsetZ(); }

/**
 * Sets the Z offset of the scene (for zooming).
 */
public void setOffsetZ(double aValue)  { _scene.setOffsetZ(aValue); }

/**
 * Returns whether scene is rendered in pseudo 3d.
 */
public boolean isPseudo3D()  { return _scene.isPseudo3D(); }

/**
 * Sets whether scene is rendered in pseudo 3d.
 */
public void setPseudo3D(boolean aFlag)  { _scene.setPseudo3D(aFlag); }

/**
 * Returns the skew angle for X by Z.
 */
public double getPseudoSkewX()  { return _scene.getPseudoSkewX(); }

/**
 * Sets the skew angle for X by Z.
 */
public void setPseudoSkewX(double anAngle)  { _scene.setPseudoSkewX(anAngle); }

/**
 * Returns the skew angle for Y by Z.
 */
public double getPseudoSkewY()  { return _scene.getPseudoSkewY(); }

/**
 * Sets the skew angle for Y by Z.
 */
public void setPseudoSkewY(double anAngle)  { _scene.setPseudoSkewY(anAngle); }

/**
 * Returns the field of view of the camera (derived from focalLength).
 */
public double getFieldOfView()  { return _scene.getFieldOfView(); }

/**
 * Sets the field of view of the camera.
 */
public void setFieldOfView(double aValue)  { _scene.setFieldOfView(aValue); }

/**
 * Sets some reasonable default view settings.
 */
public void setDefaultViewSettings()  { _scene.setDefaultViewSettings(); }

/**
 * Returns the camera as a vector.
 */
public Vector3D getCamera()  { return _scene.getCamera(); }

/**
 * Returns the number of shapes in the shape list.
 */
public int getShapeCount()  { return _scene.getShapeCount(); }

/**
 * Returns the specific shape at the given index from the shape list.
 */
public Shape3D getShape(int anIndex)  { return _scene.getShape(anIndex); }

/**
 * Adds a shape to the end of the shape list.
 */
public void addShape(Shape3D aShape)  { _scene.addShape(aShape); }

/**
 * Removes the shape at the given index from the shape list.
 */
public void removeShapes()  { _scene.removeShapes(); }

/**
 * Returns the transform 3d for the scene's camera.
 */
public Transform3D getTransform3D()  { return _scene.getTransform3D(); }

/**
 * Returns a path in camera coords for given path in local coords.
 */
public Path3D localToCamera(Path3D aPath)  { return _scene.localToCamera(aPath); }

/**
 * Returns the given vector in camera coords.
 */
public Vector3D localToCameraForVector(double x, double y, double z)  { return _scene.localToCameraForVector(x,y,z); }

/**
 * Returns whether a vector is facing camera.
 */
public boolean isFacing(Vector3D aV3D)  { return _scene.isFacing(aV3D); }

/**
 * Returns whether a vector is facing away from camera.
 */
public boolean isFacingAway(Vector3D aV3D)  { return _scene.isFacingAway(aV3D); }

/**
 * Returns whether a Path3d is facing camera.
 */
public boolean isFacing(Path3D aPath)  { return _scene.isFacing(aPath); }

/**
 * Returns whether a Path3d is facing away from camera.
 */
public boolean isFacingAway(Path3D aPath)  { return _scene.isFacingAway(aPath); }

/**
 * Rebuilds display list of Path3Ds from Shapes.
 */
protected void layoutImpl()
{
    // If RMShapes, recreate Shape list from RMShapes
    if(getShapeRMCount()>0) {
        removeShapes();
        for(RMShape shp : _rmshapes)
            addShapesForRMShape(shp, 0, getDepth(), false);
    }
}

/**
 * Paints shape children.
 */
public void paintShapeChildren(Painter aPntr)
{
    // Paint Scene paths
    _scene.paintPaths(aPntr);
    
    // Do normal version
    super.paintShapeChildren(aPntr);
}

/**
 * Viewer method.
 */
public void processEvent(ViewEvent anEvent)  { _scene.processEvent(anEvent); }

/**
 * Override to forward to Scene3D.
 */
public void setWidth(double aValue)  { super.setWidth(aValue); _scene.setWidth(aValue); }

/**
 * Override to forward to Scene3D.
 */
public void setHeight(double aValue)  { super.setHeight(aValue); _scene.setHeight(aValue); }

/**
 * Called when scene changes.
 */
protected void sceneChanged(PropChange aPC)
{
    _pcs.fireDeepChange(this, aPC);
    relayout(); repaint();
}

/**
 * Returns the number of shapes in the shape list.
 */
public int getShapeRMCount()  { return _rmshapes.size(); }

/**
 * Returns the specific shape at the given index from the shape list.
 */
public RMShape getShapeRM(int anIndex)  { return _rmshapes.get(anIndex); }

/**
 * Adds a shape to the end of the shape list.
 */
public void addShapeRM(RMShape aShape)
{
    _rmshapes.add(aShape);
    relayout();
}

/**
 * Adds Shape3D objects for given RMShape.
 * FixEdges flag indicates wheter to stroke polygons created during extrusion, to try to make them mesh better.
 */
protected void addShapesForRMShape(RMShape aShape, double z1, double z2, boolean fixEdges)
{
    // If aShape is text, add shape3d for background and add shape3d for char path shape
    if(aShape instanceof RMTextShape) { RMTextShape text = (RMTextShape)aShape;
        
        // If text draws fill or stroke, add child for background
        if(text.getFill()!=null || text.getStroke()!=null) {
            RMShape background = new RMPolygonShape(aShape.getPath()); // Create background shape from text
            background.copyShape(aShape);
            addShapesForRMShape(background, z1+.1f, z2, fixEdges); // Add background shape
        }
        
        // Get shape for char paths and add shape3d for char path shape
        RMShape charsShape = RMTextShapeUtils.getTextPathShape(text);
        addShapesForRMShape(charsShape, z1, z1, fixEdges);
        return;
    }
    
    // Get shape path, flattened and in parent coords
    Path shapePath = new Path(aShape.getPath());
    shapePath = shapePath.getPathFlattened();
    shapePath.transformBy(aShape.getTransform());
    
    // Get path3d for shape path
    PathBox3D pathBox = new PathBox3D(shapePath, z1, z2, fixEdges);

    // Create 3D shape from path, set fill/stroke/opacity and add
    RMFill fill = aShape.getFill(); if(fill!=null) pathBox.setColor(fill.getColor());
    RMStroke stroke = aShape.getStroke(); if(stroke!=null) pathBox.setStroke(stroke.getColor(), stroke.getWidth());
    pathBox.setOpacity(aShape.getOpacity());
    addShape(pathBox);
}

/** Override to indicate that scene children are unhittable. */
public boolean isHittable(RMShape aChild)  { return false; }

/** Viewer method. */
public boolean acceptsMouse()  { return true; }

/**
 * Copy 3D attributes only.
 */
public void copy3D(RMScene3D aScene3D)  { getScene().copy3D(aScene3D.getScene()); }

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXMLShape(anArchiver); e.setName("scene3d");
    
    // Archive the RMShape children: create element for shape, iterate over shapes and add
    if(getShapeRMCount()>0) {
        XMLElement shapesXML = new XMLElement("shapes");
        for(int i=0, iMax=getShapeRMCount(); i<iMax; i++)
            shapesXML.add(anArchiver.toXML(getShapeRM(i)));
        e.add(shapesXML);
    }
    
    // Archive Depth, Yaw, Pitch, Roll, FocalLength, Offset3D
    if(getDepth()!=0) e.add("depth", getDepth());
    if(getYaw()!=0) e.add("yaw", getYaw());
    if(getPitch()!=0) e.add("pitch", getPitch());
    if(getRoll3D()!=0) e.add("zroll", getRoll3D());
    if(getFocalLength()!=60*72) e.add("focal-length", getFocalLength());
    if(getOffsetZ()!=0) e.add("offset-z", getOffsetZ());
    
    // Archive Pseudo3D
    if(isPseudo3D()) {
        e.add("pseudo", true);
        e.add("pseudo-skew-x", getPseudoSkewX());
        e.add("pseudo-skew-y", getPseudoSkewY());
    }
        
    // Return xml element
    return e;
}

/**
 * XML archival of children - overrides shape implementation to suppress archival of generated 3D shapes.
 */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement) { }

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Fix scene width/height
    _scene.setWidth(getWidth()); _scene.setHeight(getHeight());
    
    // Unarchive Depth, Yaw, Pitch, Roll, FocalLength, OffsetZ
    setDepth(anElement.getAttributeFloatValue("depth"));
    setYaw(anElement.getAttributeFloatValue("yaw"));
    setPitch(anElement.getAttributeFloatValue("pitch"));
    setRoll3D(anElement.getAttributeFloatValue("zroll"));
    setFocalLength(anElement.getAttributeFloatValue("focal-length", 60*72));
    setOffsetZ(anElement.getAttributeFloatValue("offset-z"));
    
    // Unarchive Pseudo3D
    setPseudo3D(anElement.getAttributeBoolValue("pseudo", false));
    setPseudoSkewX(anElement.getAttributeFloatValue("pseudo-skew-x"));
    setPseudoSkewY(anElement.getAttributeFloatValue("pseudo-skew-y"));
    
    // Unarchive the 2d children
    XMLElement shapesXML = anElement.get("shapes");
    if(shapesXML!=null)
        for(int i=0, iMax=shapesXML.size(); i<iMax; i++)
            addShapeRM((RMShape)anArchiver.fromXML(shapesXML.get(i), this));
}

}