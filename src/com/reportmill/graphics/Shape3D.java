package com.reportmill.graphics;
import com.reportmill.shape.RMPolygonShape;
import snap.gfx.*;

/**
 * RMPolygon subclass that encapsulates a Path3D.
 */
public class Shape3D extends RMPolygonShape {

    // The path 3d used to describe this shape 3d
    Path3D _path3d;
    
    /** Creates a new shape 3d from the given path3d. */
    public Shape3D(Path3D aPath3D) { setPath3D(aPath3D); }
    
    /** Returns the path3D for this shape. */
    public Path3D getPath3D()  { return _path3d; }
    
    /** Sets the path3d for this shape. */
    public void setPath3D(Path3D aPath3D)
    {
        _path3d = aPath3D;
        Path path = aPath3D.getPath(); Rect pbounds = path.getBounds();
        setBounds(pbounds);
        Path shp = (Path)path.copyFor(new Rect(0, 0, pbounds.getWidth(), pbounds.getHeight()));
        setPath(shp);
    }
}