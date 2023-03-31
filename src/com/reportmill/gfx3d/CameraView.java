package com.reportmill.gfx3d;
import snap.geom.Rect;
import snap.gfx.*;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * A View subclass to render.
 */
public class CameraView extends ParentView {

    // The Camera
    Camera _camera;

    // The Scene
    Scene3D _scene;

    // Constants for properties
    public static String Yaw_Prop = "Yaw";
    public static String Pitch_Prop = "Pitch";
    public static String Roll_Prop = "Roll";
    public static String OffsetZ_Prop = "OffsetZ";

    /**
     * Creates a CameraView.
     */
    public CameraView()
    {
        _scene = new Scene3D();
        _camera = _scene.getCamera();
        _camera.addPropChangeListener(pc -> cameraChanged());
        enableEvents(MousePress, MouseDrag, MouseRelease);
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
    public double getRoll()  { return _camera.getRoll(); }

    /**
     * Sets the rotation about the Z axis in degrees.
     */
    public void setRoll(double aValue)  { _camera.setRoll(aValue); }

    /**
     * Returns the Z offset of the scene (for zooming).
     */
    public double getOffsetZ()  { return _camera.getOffsetZ(); }

    /**
     * Sets the Z offset of the scene (for zooming).
     */
    public void setOffsetZ(double aValue)  { _camera.setOffsetZ(aValue); }

    /**
     * Paints shape children.
     */
    protected void paintChildren(Painter aPntr)
    {
        // Paint Scene paths
        _camera.paintPaths(aPntr);

        // Do normal version
        super.paintChildren(aPntr);
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
    public void repaint()
    {
        Rect bnds = getBoundsMarked();
        repaintInParent(bnds);
    }

    /**
     * Override to account for Scene3D bounds.
     */
    public Rect getBoundsMarked()
    {
        Rect bounds = getBoundsLocal();
        Rect camBnds = _camera.getSceneBounds();
        if (camBnds.x < bounds.x) bounds.x = camBnds.x;
        if (camBnds.y < bounds.y) bounds.y = camBnds.y;
        if (camBnds.getMaxX() > bounds.getMaxX()) bounds.width = camBnds.getMaxX() - bounds.x;
        if (camBnds.getMaxY() > bounds.getMaxY()) bounds.height = camBnds.getMaxY() - bounds.y;
        bounds.inset(-2);
        return bounds;
    }

    /**
     * Called when scene changes.
     */
    private void cameraChanged()
    {
        relayout();
        repaint();
    }

    /**
     * Returns the value for given key.
     */
    public Object getPropValue(String aPropName)
    {
        if (aPropName.equals(Yaw_Prop)) return getYaw();
        if (aPropName.equals(Pitch_Prop)) return getPitch();
        if (aPropName.equals(Roll_Prop)) return getRoll();
        if (aPropName.equals(OffsetZ_Prop)) return getOffsetZ();
        return super.getPropValue(aPropName);
    }

    /**
     * Sets the value for given key.
     */
    public void setPropValue(String aPropName, Object aValue)
    {
        if (aPropName.equals(Yaw_Prop)) setYaw(SnapUtils.doubleValue(aValue));
        else if (aPropName.equals(Pitch_Prop)) setPitch(SnapUtils.doubleValue(aValue));
        else if (aPropName.equals(Roll_Prop)) setRoll(SnapUtils.doubleValue(aValue));
        else if (aPropName.equals(OffsetZ_Prop)) setOffsetZ(SnapUtils.doubleValue(aValue));
        else super.setPropValue(aPropName, aValue);
    }
}