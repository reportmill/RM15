/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.util.ClassUtils;

/**
 * Provides a tool for editing RMFills.
 */
public class EffectTool extends RMEditorPane.SupportPane {

    // Map of tool instances by shape class
    Map            _tools = new Hashtable();
    
    // List of known effects
    static Effect  _effects[] = { new ShadowEffect(), new ReflectEffect(), new BlurEffect(),
        new EmbossEffect() };
    
/**
 * Creates a new EffectTool.
 */
public EffectTool()  { super(null); }

/**
 * Returns the number of known effects.
 */
public int getEffectCount()  { return _effects.length; }

/**
 * Returns an individual effect at given index.
 */
public Effect getEffect(int anIndex)  { return _effects[anIndex]; }

/**
 * Returns the currently selected shape's effect.
 */
public Effect getSelectedEffect()
{
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    return shape.getEffect();
}

/**
 * Iterate over editor selected shapes and set fill.
 */
public void setSelectedEffect(Effect anEffect)
{
    RMEditor editor = getEditor();
    for(int i=0, iMax=editor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = editor.getSelectedOrSuperSelectedShape(i);
        shape.setEffect(anEffect);
    }
}

/**
 * Returns the specific tool for a given shape.
 */
public EffectTool getTool(Object anObj)
{
    // Get tool from tools map - just return if present
    Class cls = anObj instanceof Class? (Class)anObj : anObj.getClass();
    EffectTool tool = (EffectTool)_tools.get(cls);
    if(tool==null) {
        _tools.put(cls, tool=getToolImpl(cls));
        tool.setEditorPane(getEditorPane());
    }
    return tool;
}

/**
 * Returns the specific tool for a given effect.
 */
static EffectTool getToolImpl(Class aClass)
{
    // Get class name
    String cname = aClass.getSimpleName();
    
    // Look for tool class in reportmill.apptools
    Class tclass = ClassUtils.getClass("com.reportmill.apptools." + cname + "Tool");
    
    // If tool class found, instantiate tool class
    if(tclass!=null)
        try { return (EffectTool)tclass.newInstance(); }
        catch(Exception ie) { ie.printStackTrace(); }
        
    // Otherwise, get tool for super class
    return getToolImpl(aClass.getSuperclass());
}

}