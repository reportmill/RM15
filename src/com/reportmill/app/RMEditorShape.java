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

/**
 * Creates a new editor shape.
 */
public RMEditorShape(RMEditor anEditor)
{
    super(anEditor); _editor = anEditor;
    addDeepChangeListener(_editor);
}

/**
 * Override to do stuff for editor shape.
 */
public void setDoc(RMDocument aDoc)
{
    // Do normal version
    super.setDoc(aDoc);
    
    // Make sure current document page is super-selected
    if(_editor._selShapes!=null) {
        RMPage page = getSelPage();
        _editor.setSuperSelectedShape(page);
    }
    
    // Create and install undoer
    if(_editor.isEditing()) _undoer = new Undoer();
}

}