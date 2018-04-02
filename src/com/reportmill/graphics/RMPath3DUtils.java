/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import snap.gfx.*;

/**
 * Helper methods for the RMPath3D class.
 */
public class RMPath3DUtils {

/**
 * Creates and returns a list of paths in 3D for a given 2D path and extrusion.
 */
public static List <RMPath3D> getPaths(RMPath aPath, float z1, float z2)  { return getPaths(aPath,z1,z2,0); }

/**
 * Creates and returns a list of paths in 3D for a given 2D path and extrusion. 
 * Also can take into account the width of a stroke applied to the side (extrusion) panels.
 */
public static List <RMPath3D> getPaths(RMPath aPath, double z1, double z2, double strokeWidth)
{
    // Create list to hold paths
    List <RMPath3D> paths = new ArrayList();

    // Declare local variable for back face
    RMPath3D back = null;
    
    // If path is closed, create path3d for front from aPath and z1
    if(aPath.isClosed()) {
        
        // Create path3d for front and back
        RMPath3D front = new RMPath3D(aPath, z1);
        back = new RMPath3D(aPath, z2);
        
        // Add front to paths list
        paths.add(front);
    
        // If front is pointing wrong way, reverse it
        if(front.getNormal().isAway(new RMVector3D(0, 0, -1), true))
            front.reverse();
        
        // Otherwise, reverse back
        else {
            back.reverse();
            aPath = back.getPath();
        }
    }
    
    // Make room for path stroke
    z1 += strokeWidth;
    z2 -= strokeWidth;
    
    // Iterate over path elements
    PathIter piter = aPath.getPathIter(null);
    double pts[] = new double[6], lastX = 0, lastY = 0, lastMoveX = 0, lastMoveY = 0;
    while(piter.hasNext()) switch(piter.getNext(pts)) {

        // MoveTo
        case MoveTo: lastX = lastMoveX = pts[0]; lastY = lastMoveY = pts[1]; break;
        
        // LineTo
        case LineTo: {
            //skip over points
            if(!Point.equals(lastX,lastY,pts[0],pts[1])) {
                RMPath3D path = new RMPath3D();
                path.moveTo(lastX, lastY, z1);
                path.lineTo(pts[0], pts[1], z1);
                path.lineTo(pts[0], pts[1], z2);
                path.lineTo(lastX, lastY, z2);
                path.close();
                double x = lastX + (pts[0] - lastX)/2;
                double y = lastY + (pts[1] - lastY)/2;
                path.setCenter(new RMPoint3D(x, y, z2/2));
                paths.add(path);
                lastX = pts[0]; lastY = pts[1];
            }
        } break;
            
        // QuadTo
        case QuadTo: {
            RMPath3D path = new RMPath3D();
            path.moveTo(lastX, lastY, z1);
            path.quadTo(pts[0], pts[1], z1, pts[2], pts[3], z1);
            path.lineTo(pts[4], pts[5], z2);
            path.quadTo(pts[0], pts[1], z2, lastX, lastY, z2);
            path.close();
            double x = lastX + (pts[2] - lastX)/2;
            double y = lastY + (pts[3] - lastY)/2;
            path.setCenter(new RMPoint3D(x, y, z2/2));
            paths.add(path);
            lastX = pts[2]; lastY = pts[3];
        } break;
            
        // CubicTo
        case CubicTo: {
            RMPath3D path = new RMPath3D();
            path.moveTo(lastX, lastY, z1);
            path.curveTo(pts[0], pts[1], z1, pts[2], pts[3], z1, pts[4], pts[5], z1);
            path.lineTo(pts[4], pts[5], z2);
            path.curveTo(pts[2], pts[3], z2, pts[0], pts[1], z2, lastX, lastY, z2);
            path.close();
            double x = lastX + (pts[4] - lastX)/2;
            double y = lastY + (pts[5] - lastY)/2;
            path.setCenter(new RMPoint3D(x, y, z2/2));
            paths.add(path);
            lastX = pts[4]; lastY = pts[5];
        } break;
        
        // Close
        case Close: {
            RMPath3D path = new RMPath3D();
            path.moveTo(lastX, lastY, z1);
            path.lineTo(lastMoveX, lastMoveY, z1);
            path.lineTo(lastMoveX, lastMoveY, z2);
            path.lineTo(lastX, lastY, z2);
            path.close();
            double x = lastX + (lastMoveX - lastX)/2;
            double y = lastY + (lastMoveY - lastY)/2;
            path.setCenter(new RMPoint3D(x, y, z2/2));
            paths.add(path);
        } break;
    }
    
    // Add back face to paths
    if(back != null)
        paths.add(back);
    
    // Return paths
    return paths;
}

}