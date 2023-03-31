/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.gfx3d.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.geom.Path;
import snap.geom.Rect;
import snap.gfx.*;
import snap.props.PropChange;
import snap.util.*;
import snap.view.ViewEvent;

/**
 * This encapsulates a Snap Scene3D to render simple 3d.
 */
public class RMScene3D extends RMParentShape {

    // A Scene3D to do real scene management
    Scene3D _scene = new Scene3D();

    // A Camera to do camera work
    Camera _camera;

    // List of real child shapes
    List<RMShape> _rmshapes = new ArrayList<>();

    /**
     * Creates an RMScene3D.
     */
    public RMScene3D()
    {
        _camera = _scene.getCamera();
        _camera.addPropChangeListener(pce -> sceneChanged(pce));
    }

    /**
     * Returns the camera as a vector.
     */
    public Camera getCamera()  { return _camera; }

    /**
     * Returns the Scene3D.
     */
    public Scene3D getScene()  { return _scene; }

    /**
     * Returns the depth of the scene.
     */
    public double getDepth()  { return _camera.getDepth(); }

    /**
     * Sets the depth of the scene.
     */
    public void setDepth(double aValue)  { _camera.setDepth(aValue); }

    /**
     * Returns the rotation about the Y axis in degrees.
     */
    public double getYaw()  { return _camera.getYaw(); }

    /**
     * Sets the rotation about the Y axis in degrees.
     */
    public void setYaw(double aValue)  { _camera.setYaw(aValue); }

    /**
     * Returns the rotation about the X axis in degrees.
     */
    public double getPitch()  { return _camera.getPitch(); }

    /**
     * Sets the rotation about the X axis in degrees.
     */
    public void setPitch(double aValue)  { _camera.setPitch(aValue); }

    /**
     * Returns the rotation about the Z axis in degrees.
     */
    public double getRoll3D()  { return _camera.getRoll(); }

    /**
     * Sets the rotation about the Z axis in degrees.
     */
    public void setRoll3D(double aValue)  { _camera.setRoll(aValue); }

    /**
     * Returns the focal length of the camera (derived from the field of view and with view size).
     */
    public double getFocalLength()  { return _camera.getFocalLength(); }

    /**
     * Sets the focal length of the camera. Two feet is normal (1728 points).
     */
    public void setFocalLength(double aValue)  { _camera.setFocalLength(aValue); }

    /**
     * Returns the Z offset of the scene (for zooming).
     */
    public double getOffsetZ()  { return _camera.getOffsetZ(); }

    /**
     * Sets the Z offset of the scene (for zooming).
     */
    public void setOffsetZ(double aValue)  { _camera.setOffsetZ(aValue); }

    /**
     * Adds a shape to the end of the shape list.
     */
    public void addShape(Shape3D aShape)
    {
        _scene.addShape(aShape);
    }

    /**
     * Removes the shape at the given index from the shape list.
     */
    public void removeShapes()
    {
        _scene.removeShapes();
    }

    /**
     * Rebuilds display list of Path3Ds from Shapes.
     */
    protected void layoutImpl()
    {
        // If RMShapes, recreate Shape list from RMShapes
        if (getShapeRMCount() > 0) {
            removeShapes();
            for (RMShape shp : _rmshapes)
                addShapesForRMShape(shp, 0, getDepth(), false);
        }
    }

    /**
     * Paints shape children.
     */
    protected void paintShapeChildren(Painter aPntr)
    {
        // Paint Scene paths
        _camera.paintPaths(aPntr);

        // Do normal version
        super.paintShapeChildren(aPntr);
    }

    /**
     * Viewer method.
     */
    public void processEvent(ViewEvent anEvent)
    {
        _camera.processEvent(anEvent);
    }

    /**
     * Override to forward to Scene3D.
     */
    public void setWidth(double aValue)
    {
        super.setWidth(aValue);
        _camera.setWidth(aValue);
    }

    /**
     * Override to forward to Scene3D.
     */
    public void setHeight(double aValue)
    {
        super.setHeight(aValue);
        _camera.setHeight(aValue);
    }

    /**
     * Override to account for Scene3D bounds.
     */
    public Rect getBoundsMarked()
    {
        Rect bounds = super.getBoundsMarked();
        Rect camBnds = _camera.getSceneBounds();
        if (camBnds.x < bounds.x) bounds.x = camBnds.x;
        if (camBnds.y < bounds.y) bounds.y = camBnds.y;
        if (camBnds.getMaxX() > bounds.getMaxX()) bounds.width = camBnds.getMaxX() - bounds.x;
        if (camBnds.getMaxY() > bounds.getMaxY()) bounds.height = camBnds.getMaxY() - bounds.y;
        return bounds;
    }

    /**
     * Called when scene changes.
     */
    protected void sceneChanged(PropChange aPC)
    {
        _pcs.fireDeepChange(this, aPC);
        relayout();
        repaint();
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
        if (aShape instanceof RMTextShape) {
            RMTextShape text = (RMTextShape) aShape;

            // If text draws fill or stroke, add child for background
            if (text.getFill() != null || text.getStroke() != null) {
                RMShape background = new RMPolygonShape(aShape.getPath()); // Create background shape from text
                background.copyShape(aShape);
                addShapesForRMShape(background, z1 + .1f, z2, fixEdges); // Add background shape
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
        RMFill fill = aShape.getFill();
        if (fill != null) pathBox.setColor(fill.getColor());
        RMStroke stroke = aShape.getStroke();
        if (stroke != null) pathBox.setStroke(stroke.getColor(), stroke.getWidth());
        pathBox.setOpacity(aShape.getOpacity());
        addShape(pathBox);
    }

    /**
     * Override to indicate that scene children are unhittable.
     */
    public boolean isHittable(RMShape aChild)  { return false; }

    /**
     * Viewer method.
     */
    public boolean acceptsMouse()  { return true; }

    /**
     * Copy 3D attributes only.
     */
    public void copy3D(RMScene3D aScene3D)
    {
        getCamera().copy3D(aScene3D.getCamera());
    }

    /**
     * XML archival.
     */
    protected XMLElement toXMLShape(XMLArchiver anArchiver)
    {
        // Archive basic shape attributes and reset element name
        XMLElement e = super.toXMLShape(anArchiver);
        e.setName("scene3d");

        // Archive the RMShape children: create element for shape, iterate over shapes and add
        if (getShapeRMCount() > 0) {
            XMLElement shapesXML = new XMLElement("shapes");
            for (int i = 0, iMax = getShapeRMCount(); i < iMax; i++)
                shapesXML.add(anArchiver.toXML(getShapeRM(i)));
            e.add(shapesXML);
        }

        // Archive Depth, Yaw, Pitch, Roll, FocalLength, Offset3D
        if (getDepth() != 0) e.add("depth", getDepth());
        if (getYaw() != 0) e.add("yaw", getYaw());
        if (getPitch() != 0) e.add("pitch", getPitch());
        if (getRoll3D() != 0) e.add("zroll", getRoll3D());
        if (getFocalLength() != 60 * 72) e.add("focal-length", getFocalLength());
        if (getOffsetZ() != 0) e.add("offset-z", getOffsetZ());

        // Return xml element
        return e;
    }

    /**
     * Override to suppress.
     */
    protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)  { }

    /**
     * XML unarchival.
     */
    protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic shape attributes
        super.fromXMLShape(anArchiver, anElement);

        // Fix scene width/height
        _camera.setWidth(getWidth());
        _camera.setHeight(getHeight());

        // Unarchive Depth, Yaw, Pitch, Roll, FocalLength, OffsetZ
        setDepth(anElement.getAttributeFloatValue("depth"));
        setYaw(anElement.getAttributeFloatValue("yaw"));
        setPitch(anElement.getAttributeFloatValue("pitch"));
        setRoll3D(anElement.getAttributeFloatValue("zroll"));
        setFocalLength(anElement.getAttributeFloatValue("focal-length", 60 * 72));
        setOffsetZ(anElement.getAttributeFloatValue("offset-z"));

        // Unarchive the 2d children
        XMLElement shapesXML = anElement.get("shapes");
        if (shapesXML != null)
            for (int i = 0, iMax = shapesXML.size(); i < iMax; i++)
                addShapeRM((RMShape) anArchiver.fromXML(shapesXML.get(i), this));
    }
}