/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import snap.util.Undoer;

/**
 * The root shape for RMEditor.
 */
public class RMEditorShape extends RMViewerShape {

    // The editor
    RMEditor         _editor;

    // An optional undoer object to track document changes
    Undoer           _undoer;
    
/**
 * Creates a new editor shape.
 */
public RMEditorShape(RMEditor anEditor)
{
    super(anEditor); _editor = anEditor;
    addDeepChangeListener(_editor);
}

/**
 * Returns the editor.
 */
public RMEditor getEditor()  { return _editor; }

/**
 * Override to do stuff for editor shape.
 */
public void setDoc(RMDocument aDoc)
{
    // Do normal version
    super.setDoc(aDoc);
    
    // Make sure current document page is super-selected
    if(_editor._selectedShapes!=null) _editor.setSuperSelectedShape(getSelectedPage());
    
    // Create and install undoer
    _undoer = new Undoer();
}

/**
 * Returns whether content snaps to grid.
 */
public boolean getSnapGrid()  { RMDocument d = getDocument(); return d!=null && d.getSnapGrid(); }

/**
 * Returns the content grid spacing.
 */
public double getGridSpacing()  { RMDocument d = getDocument(); return d!=null? d.getGridSpacing() : 1; }

/**
 * Returns whether content snaps to margin.
 */
public boolean getSnapMargin()  { RMDocument d = getDocument(); return d!=null && d.getSnapMargin(); }

/**
 * Returns the undoer.
 */
public Undoer getUndoer()  { return _undoer; }

}