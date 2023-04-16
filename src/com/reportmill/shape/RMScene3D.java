/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.geom.*;
import snap.gfx3d.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.*;
import snap.props.PropChange;
import snap.util.*;
import snap.view.ViewEvent;

/**
 * This encapsulates a Snap Scene3D to render simple 3d.
 */
public class RMScene3D extends RMParentShape {

    // A Scene3D to do real scene management
    Scene3D _scene;

    // A Camera to do camera work
    Camera _camera;

    // The default depth
    private double  _depth = 40;

    // List of real child shapes
    List<RMShape> _rmshapes = new ArrayList<>();

    // Constants for properties
    public static final String Depth_Prop = "Depth";

    /**
     * Constructor.
     */
    public RMScene3D()
    {
        super();
        _scene = new Scene3D();
        _camera = _scene.getCamera();
        _camera.setRenderer(new Renderer2D(_camera));
        _camera.addPropChangeListener(pc -> cameraDidPropChange(pc));
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
    public double getDepth()  { return _depth; }

    /**
     * Sets the depth of the scene.
     */
    public void setDepth(double aValue)
    {
        // If already set, just return
        if (aValue == _depth) return;

        // Set, fire prop change
        firePropChange(Depth_Prop, _depth, _depth = aValue);

        // Relayout/repaint
        relayout();
        repaint();
    }

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
     * Rebuilds display list of Path3Ds from Shapes.
     */
    protected void layoutImpl()
    {
        // If RMShapes, recreate Shape list from RMShapes
        if (getShapeRMCount() > 0) {
            _scene.removeChildren();
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
        _camera.paintScene(aPntr);

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
        _camera.setViewWidth(aValue);
    }

    /**
     * Override to forward to Scene3D.
     */
    public void setHeight(double aValue)
    {
        super.setHeight(aValue);
        _camera.setViewHeight(aValue);
    }

    /**
     * Override to account for Scene3D bounds.
     */
    public Rect getBoundsMarked()
    {
        Rect bounds = super.getBoundsMarked();
        Rect camBnds = _camera.getSceneBounds2D();
        if (camBnds.x < bounds.x) bounds.x = camBnds.x;
        if (camBnds.y < bounds.y) bounds.y = camBnds.y;
        if (camBnds.getMaxX() > bounds.getMaxX()) bounds.width = camBnds.getMaxX() - bounds.x;
        if (camBnds.getMaxY() > bounds.getMaxY()) bounds.height = camBnds.getMaxY() - bounds.y;
        return bounds;
    }

    /**
     * Called when camera changes to trigger repaint.
     */
    protected void cameraDidPropChange(PropChange aPC)
    {
        _pcs.fireDeepChange(this, aPC);
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
     * SmoothSides flag indicates whether to stroke polygons created during extrusion, to try to make them mesh better.
     */
    protected void addShapesForRMShape(RMShape aShape, double z1, double z2, boolean smoothSides)
    {
        // If aShape is text, add shape3d for background and add shape3d for char path shape
        if (aShape instanceof RMTextShape) {
            RMTextShape text = (RMTextShape) aShape;

            // If text draws fill or stroke, add child for background
            if (text.getFill() != null || text.getStroke() != null) {
                RMShape background = new RMPolygonShape(aShape.getPath()); // Create background shape from text
                background.copyShape(aShape);
                addShapesForRMShape(background, z1 + .1f, z2, smoothSides); // Add background shape
            }

            // Get shape for char paths and add shape3d for char path shape
            RMShape charsShape = RMTextShapeUtils.getTextPathShape(text);
            addShapesForRMShape(charsShape, z1, z1, smoothSides);
            return;
        }

        // Get shape path, flattened and in parent coords
        Shape shapePath = getShapePathFlatInScene(aShape);

        // Handle no depth: Create Path3D double-sided
        Shape3D shape3D;
        if (MathUtils.equals(z1, z2)) {
            shape3D = new Path3D(shapePath, z1);
            shape3D.setDoubleSided(true);
        }

        // Handle Extruded shapes: Create PathBox3D for z1/z3
        else {
            shape3D = new PathBox3D(shapePath, z1, z2);
            if (smoothSides)
                shape3D.setSmoothSides(true);
        }

        // Create 3D shape from path, set fill/stroke/opacity and add
        RMFill fill = aShape.getFill();
        if (fill != null)
            shape3D.setColor(fill.getColor());
        RMStroke stroke = aShape.getStroke();
        if (stroke != null)
            shape3D.setStroke(stroke.getColor(), stroke.getWidth());
        shape3D.setOpacity(aShape.getOpacity());
        _scene.addChild(shape3D);
    }

    /**
     * Returns the flattened shape path in Scene at depth = 0.
     */
    private Shape getShapePathFlatInScene(RMShape aShape)
    {
        // Get flat path
        Shape shapePath = aShape.getPath();
        Shape shapePathFlat = shapePath.getFlattenedShape();

        // Transform to parent (scene)
        Transform shapeToParent = aShape.getTransform();
        Shape shapePathFlatInScene = shapePathFlat.copyFor(shapeToParent);

        // Flip
        double height = getHeight();
        Path2D path = new Path2D(shapePathFlatInScene);
        for (int i = 0, iMax = path.getPointCount(); i < iMax; i++) {
            Point p = path.getPoint(i);
            double newY = height - p.y;
            path.setPoint(i, p.x, newY);
        }

        // Return
        return path;
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
        setDepth(aScene3D.getDepth());
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

        // Archive Depth, Yaw, Pitch, Roll, FocalLength
        if (getDepth() != 0) e.add("depth", getDepth());
        if (getYaw() != 0) e.add("yaw", getYaw());
        if (getPitch() != 0) e.add("pitch", getPitch());
        if (getRoll3D() != 0) e.add("zroll", getRoll3D());
        if (getFocalLength() != 60 * 72) e.add("focal-length", getFocalLength());

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
        _camera.setViewWidth(getWidth());
        _camera.setViewHeight(getHeight());

        // Unarchive Depth, Yaw, Pitch, Roll, FocalLength, OffsetZ
        setDepth(anElement.getAttributeFloatValue("depth"));
        setYaw(anElement.getAttributeFloatValue("yaw"));
        setPitch(anElement.getAttributeFloatValue("pitch"));
        setRoll3D(anElement.getAttributeFloatValue("zroll"));
        setFocalLength(anElement.getAttributeFloatValue("focal-length", 60 * 72));

        // Unarchive the 2d children
        XMLElement shapesXML = anElement.get("shapes");
        if (shapesXML != null)
            for (int i = 0, iMax = shapesXML.size(); i < iMax; i++)
                addShapeRM((RMShape) anArchiver.fromXML(shapesXML.get(i), this));
    }
}