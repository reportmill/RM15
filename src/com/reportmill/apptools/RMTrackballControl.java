/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMViewer;
import com.reportmill.app.RMViewerEvents;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.Random;
import snap.gfx.*;
import snap.view.*;
import snap.web.WebURL;

/**
 * This class implements the trackball widget.  It is an optional replacement for the Scene3DControl.
 * 
 * Trackball inherits the RMScene3D's behavior, which is that mouse motion in the x direction changes the pitch, and 
 * mouse motion in the y direction changes the yaw. The controll adds rotation about the z axis (roll) by clicking
 * on a ring outside the trackball.
 *   
 * Note that this particular behavior quickly goes wrong, since
 *  Rotate(y,x,z) * Rotate(dy,dx,dz) != Rotate (y+dy, x+dx, z+dz)
 * 
 * To make the behavior more reasonable, we could try 
 * any of :
 *   1.  Make Scene3D keep a matrix,instead of the euler angles and just keep rotating that by dx,dy,dz
 *   2.  Get the matrix, rotate by dx,dy,dz, decompose into new euler angles and set those
 *   3.  Use quaternions (gasp) 
 */
public class RMTrackballControl extends RMViewer {

    // The scene3d drawn by this viewer (contains the trackball's scuffmarks)
    RMScene3D        _scene;

    // The radius of the trackball sphere, which sits at the origin
    float            _radius = 36;
   
    // hit test result, for dragging
    int              _hitPart;

    // saved angle for calculating new roll during drags on the collar
    double           _lastRollAngle;

    // The trackball image, highlight image and knob image
    RMImageShape     _tball = new RMImageShape(WebURL.getURL(getClass(), "pkg.images/Trackball.png"));
    RMImageShape     _tball_lit = new RMImageShape(WebURL.getURL(getClass(), "pkg.images/Trackball_lit.png"));
    RMImageShape     _knob = new RMImageShape(WebURL.getURL(getClass(), "pkg.images/Trackball_knob.png"));
   
    // Location of the important parts of the control image
    static final float LEFT_EDGE = 2;
    static final float TOP_EDGE = 2;
    static final float COLLAR_THICKNESS = 16;
    static final float INNER_RADIUS = 39;
    static final float CENTER_X = LEFT_EDGE+COLLAR_THICKNESS+INNER_RADIUS;
    static final float CENTER_Y = TOP_EDGE+COLLAR_THICKNESS+INNER_RADIUS;
    static final float KNOB_WIDTH = 14;
    static final float KNOB_CENTER_X = 9;
    static final float KNOB_CENTER_Y = 11;
   
    // Possible hit test results
    static final int HIT_NONE = 0;
    static final int HIT_COLLAR = 1;
    static final int HIT_TRACKBALL = 2;

/**
 * Creates a new trackball control
 */
public RMTrackballControl()
{
    // Create a document whose size matches the image
    RMDocument doc = new RMDocument(_tball.getWidth(), _tball.getHeight()); doc.setShowMargin(false);
    RMPage page = doc.getPage(0); page.setPaintBackground(false);
    page.addChild(_tball);
    setContent(doc);
    
    // Create/configure scene and add to page
    _scene = new RMScene3D(); _scene.setBounds(2, 0, _tball.getWidth(), _tball.getHeight());
    page.addChild(_scene);
    enableEvents(Action);
    setFill(null);
}

/**
 * Override set bounds to fix zoom factor.
 */
public void setWidth(double aValue)
{
    if(aValue==getWidth()) return; super.setWidth(aValue); // Do normal version
    setZoomFactor(getWidth()/118); // Set zoom factor
    configureScene(); // Reconfigure scene
}

/**
 * Reconfigure scene.
 */
public void configureScene()
{
    // Add scuffmark polygons at random points on the trackball
    Random ran = new Random(); _scene.removeShapes();
    for(int i=0; i<50; i++) { float th = ran.nextFloat()*360, ph = ran.nextFloat()*360;
        addScuff(th,ph); }
}   

/**
 * Adds a polygon to the scene which attempts to represent a scuffmark on the sphere at polar location {theta,phi}
 */
private void addScuff(float theta, float phi)
{
    // Small triangle at the origin to represent a scuff mark
    RMPath3D path = new RMPath3D(); path.moveTo(-1,-1,0); path.lineTo(0,1,0); path.lineTo(1,-1,0); path.close();
    
    // translate out to surface of sphere and rotate to latitude, longitude
    RMTransform3D transform = new RMTransform3D();
    transform.translate(0, 0, _radius);
    transform.rotateY(theta).rotateZ(phi);
    
    // translate to scene origin
    RMPoint3D origin = _scene.getOrigin();
    transform.translate(origin.x, origin.y, origin.z);
    path.transform(transform);
    
    // create the shape
    RMScene3D.RMShape3D scuff = new RMScene3D.RMShape3D(path); scuff.setStroke(null);
    
    // If the trackball is shrunk down, draw the scuffmarks a darker color so they'll show up.
    if(getZoomFactor()<.75) scuff.setColor(new RMColor(0,0,0,.75f));
    else scuff.setColor(new RMColor(.2f,.2f,.2f,.5f));
    _scene.addShape(scuff);
}

/** Override to provide special event helper. */
public RMViewerEvents createEvents()  { return new TBInputAdapter(this); }

/**
 * A Viewer Event helper for TrackBall.
 */
public class TBInputAdapter extends RMViewerEvents { 
    
    /** Creates new TBInputAdapter. */
    public TBInputAdapter(RMViewer aVwr)  { super(aVwr); }

    /** Handle mouse pressed event.     */
    public void mousePressed(ViewEvent anEvent)
    {
        double scale = getZoomFactor();
        Point p = anEvent.getPoint(); p.x /= scale; p.y /= scale;
        double distance = p.getDistance(CENTER_X,CENTER_Y);
        
        // If inside trackball, replace image with lit version
        if(distance<=INNER_RADIUS) {
            _hitPart = HIT_TRACKBALL; // turn on hilight
            RMPage page = getDocument().getPage(0);
            page.removeChild(_tball); page.addChild(_tball_lit, 0);
        }
        
        // Else if in collar, add knob
        else if(distance<=INNER_RADIUS+COLLAR_THICKNESS && !_scene.isPseudo3D()) {
            _hitPart = HIT_COLLAR;
            getDocument().getPage(0).addChild(_knob);
            _lastRollAngle = getMouseAngle(p);
            positionKnob(p);
        }
        
        // Else
        else _hitPart = HIT_NONE;
        
        // Do normal mouse pressed
        super.mousePressed(anEvent);
    }
    
    /** Handle mouse dragged event. */
    public void mouseDragged(ViewEvent anEvent)
    {
        // If
        if(_hitPart==HIT_COLLAR) {
            double scale = getZoomFactor();
            Point p = anEvent.getPoint(); p.x /= scale; p.y /= scale;
            double theta = getMouseAngle(p);
            _scene.setRoll3D(_scene.getRoll3D()+(float)Math.toDegrees(theta - _lastRollAngle));
            _lastRollAngle = theta;
            positionKnob(p);
            repaint();
        }
        
        // Otherwise do normal mouse dragged
        else super.mouseDragged(anEvent);
        
        // Send ViewEvent to owner
        fireActionEvent();
    }
    
    /** Handle mouse released event. */
    public void mouseReleased(ViewEvent anEvent)
    {
        RMPage page = getDocument().getPage(0);
        if(_hitPart==HIT_TRACKBALL) {
            page.removeChild(_tball_lit); page.addChild(_tball,0); }
        else if(_hitPart==HIT_COLLAR)
            page.removeChild(_knob);
       
        // Do normal mouse released
        super.mouseReleased(anEvent);
       
        // Send ViewEvent to owner
        fireActionEvent();
    }
}

/**
 * Returns the angle from the mousePoint to the center of the control, in radians.
 */
public double getMouseAngle(Point p)  { double dx = p.x - CENTER_X, dy = p.y - CENTER_Y; return Math.atan2(dy, dx); }

/**
 * Move the collar knob to the correct location for the given mouse point.
 */
public void positionKnob(Point p)
{
    double theta = getMouseAngle(p), r = INNER_RADIUS + KNOB_WIDTH/2;
    double x = CENTER_X + r*Math.cos(theta) - KNOB_CENTER_X;
    double y = CENTER_Y + r*Math.sin(theta) - KNOB_CENTER_Y;
    _knob.setXY(x, y);
}

/**
 * Sync from given scene to this scene control.
 */
public void syncFrom(RMScene3D aScene)  { sync(aScene, _scene); }

/**
 * Sync to a given scene from this scene control.
 */
public void syncTo(RMScene3D aScene)  { sync(_scene, aScene); }

/** Sync scenes. */    
private void sync(RMScene3D s1, RMScene3D s2)
{
    if(s1.isPseudo3D()) { s2.setPseudoSkewX(s1.getPseudoSkewX()); s2.setPseudoSkewY(s1.getPseudoSkewY()); }
    else { s2.setPitch(s1.getPitch()); s2.setYaw(s1.getYaw()); s2.setRoll3D(s1.getRoll3D()); }
}

}