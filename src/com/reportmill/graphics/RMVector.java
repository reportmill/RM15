/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;

/**
 * This class represents a 2D vector.
 */
public class RMVector implements Cloneable {

    // The x component of the vector
    public double     x;
    
    // The y component of the vector
    public double     y;

/**
 * Creates a new vector.
 */
public RMVector()  { }
    
/**
 * Creates a new vector.
 */
public RMVector(double X, double Y)  { x = X; y = Y; }

/**
 * Adds a vector.
 */
public RMVector add(RMVector aVector)  { return add(aVector.x, aVector.y); }

/**
 * Adds the vector components to this vector.
 */
public RMVector add(double X, double Y)  { x += X; y += Y; return this; }

/**
 * Normalizes the vector.
 */
public RMVector normalize()  { double t = getMagnitude(); x /= t; y /= t; return this; }

/**
 * Makes this receiver point in the opposite direction.
 */
public RMVector negate()  { x = -x; y = -y; return this; }

/**
 * Multiply vector by given magnitude.
 */
public RMVector multiply(double aValue)  { x *= aValue; y *= aValue; return this; }
    
/**
 * Returns the magnitude of the vector.
 */
public double getMagnitude()  { return Math.sqrt(x*x + y*y); }
    
/**
 * Returns the dot product of the receiver and the given vector.
 */
public double getDotProduct(RMVector v2)  { return x*v2.x + y*v2.y; }

/**
 * Returns the normalized vector.
 */
public RMVector getNormalized()  { return clone().normalize(); }

/**
 * Returns a basic clone of this object.
 */
public RMVector clone()
{
    try { return (RMVector)super.clone(); }
    catch(CloneNotSupportedException e) { return null; }
}

/**
 * Returns a string representation of the vector.
 */
public String toString()  { return "Vector [" + x + " " + y + "]"; }

}