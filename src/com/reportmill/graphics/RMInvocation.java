/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.util.*;

/**
 * This class represent a method call  and is used to set values on another object, either by calling a "setFoo()"
 * method or by directly setting the value of a public instance variable.
 * The action string is always of the form "setFoo:".  If the target object has a setFoo() method, that method will
 * get called. If not, but it has a "foo" or "_foo" instance variable, that variable will get stuffed with the value.
 */
public class RMInvocation implements Cloneable {
    
    // The target of the method invocation
    Object         _target;
    
    // The string name of the method
    String         _action;
    
    // The method args
    Object         _args[] = new Object[1];
    
/**
 * Creates an uninitialized invocation.
 */
public RMInvocation() { }

/**
 * Creates a new invocation for the given object, method name and value.
 */
public RMInvocation(Object aTarget, String anAction, Object aValue)
{
    // Set target
    _target = aTarget;
    
    // Set action
    _action = anAction;
    
    // Set arg
    _args[0] = aValue;
}

/**
 * Returns the target for this invocation.
 */
public Object getTarget()  { return _target; }

/**
 * Returns the action for this invocation.
 */
public String getAction()  { return _action; }

/**
 * Returns the arg for this invocation.
 */
public Object getArg()  { return _args[0]; }

/**
 * Sets the arg for this invocation.
 */
public void setArg(Object anObj)  { _args[0] = anObj; }

/**
 * Returns the class of this invocation's arg.
 */
public Class getArgClass()  { return Key.getAccessor(getTarget(), getAction()).getGetMethod().getReturnType(); }

/**
 * Invoke method.
 */
public void invoke()  { KeyChain.setValue(getTarget(), getAction(), _args[0]); }

/**
 * Returns an invocation by blending this invocation with given invocation using the given fraction of this invocation.
 */
public RMInvocation getInterpolation(double aFraction, RMInvocation other)
{
    // Handle simple edge cases
    if(MathUtils.equals(aFraction, 0)) return this;
    if(MathUtils.equals(aFraction, 1)) return other;
    
    // Make a copy of self to hold interpolated invocation
    RMInvocation invocation = (RMInvocation)clone();
    
    // Get arg class
    Class argClass = getArgClass();
    
    // Handle float & double
    if(float.class.isAssignableFrom(argClass) || double.class.isAssignableFrom(argClass)) {
        double v1 = SnapUtils.doubleValue(getArg());
        double v2 = SnapUtils.doubleValue(other.getArg());
        double v3 = getInterpolation(aFraction, v1, v2);
        invocation.setArg(new Float(v3));
    }
    
    // Handle Boolean (just return this since initial fraction check indicated we hadn't hit other invocation
    else if(boolean.class.isAssignableFrom(argClass))
        return this;
    
    // Handle RMColor
    else if(RMColor.class.isAssignableFrom(argClass)) {
        RMColor c1 = (RMColor)getArg();
        RMColor c2 = (RMColor)other.getArg();
        invocation.setArg(c1.blend(c2, aFraction));
    }
    
    // Handle Blend interface implementers
    else if(getArg() instanceof Blend) {
        Object o1 = getArg();
        Object o2 = other.getArg();
        invocation.setArg(((Blend)o1).blend(o2, aFraction));
    }
      
    // Complain about anyone else
    else System.err.println("RMInvocation:blend: Unsupported arg type");

    // Return blended invocation
    return invocation;
}

/**
 * Returns an interpolation for a given fraction (from 0-1) and two values.
 */
private static double getInterpolation(double aFraction, double aStart, double anEnd)
{
    return aStart + (anEnd-aStart)*aFraction;
}

/**
 * Implemented by any class that can interpolate themselves for animation purposes.
 */
public interface Blend {
    public Object blend(Object object, double fraction);
}

/**
 * Standard clone implementation.
 */
public Object clone()
{
    // Declare variable for clone
    RMInvocation clone;
    
    // Do normal object clone
    try { clone = (RMInvocation)super.clone(); }
    catch(CloneNotSupportedException e) { return null; }
    
    // Get real copy of referenced args
    clone._args = new Object[] { _args[0] };
    
    // Return invocation clone
    return clone;
}

/**
 * Returns a string representation of this invocation.
 */
public String toString()  { return _args[0].toString(); }

}