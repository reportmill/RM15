/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import java.util.*;
import snap.view.*;

/**
 * An inspector for general shape attributes, like property keys, name, text wrap around, etc.
 */
public class ShapeGeneral extends RMEditorPane.SupportPane {
    
    // The bindings table
    TableView <String>  _bindingsTable;

/**
 * Creates a new ShapeGeneral pane.
 */
public ShapeGeneral(RMEditorPane anEP)  { super(anEP); }

/**
 * Initialize UI panel for this inspector.
 */
protected void initUI()
{
    // Get bindings table
    _bindingsTable = getView("BindingsTable", TableView.class); _bindingsTable.setRowHeight(18);
    _bindingsTable.setCellConfigure(this :: configureBindingsTable);
    enableEvents(_bindingsTable, MouseRelease, DragDrop);
}

/**
 * Updates UI controsl from current selection.
 */
public void resetUI()
{
    // Get currently selected shape
    RMShape shape = getSelectedShape();
    
    // Reset NameText, UrlText
    setViewValue("NameText", shape.getName());
    setViewValue("UrlText", shape.getURL());

    // Reset table model shape
    _bindingsTable.setItems(shape.getPropNames());
    if(_bindingsTable.getSelIndex()<0) _bindingsTable.setSelIndex(0);
    _bindingsTable.updateItems();
    
    // Reset BindingsText
    String pname = _bindingsTable.getSelItem();
    Binding binding = shape.getBinding(pname);
    setViewValue("BindingsText", binding!=null? binding.getKey() : null);
}

/**
 * Updates current selection from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the current editor and selected shape (just return if null) and selected shapes
    RMShape shape = getSelectedShape(); if(shape==null) return;
    List <? extends RMShape> shapes = getEditor().getSelectedOrSuperSelectedShapes();
    
    // Handle NameText, UrlText
    if(anEvent.equals("NameText")) { String value = anEvent.getStringValue();
        for(RMShape shp : shapes) shp.setName(value); }
    if(anEvent.equals("UrlText")) { String value = anEvent.getStringValue();
        for(RMShape shp : shapes) shp.setURL(value); }
    
    // Handle BindingsTable
    if(anEvent.equals("BindingsTable")) {
        
        // Handle MouseRelease: Select text
        if(anEvent.isMouseRelease()) {
            requestFocus("BindingsText");
            getView("BindingsText", TextView.class).selectAll();
        }
            
        // Handle DragDrop
        if(anEvent.isDragDrop()) {
            Clipboard dboard = anEvent.getClipboard(); anEvent.acceptDrag();
            if(dboard.hasString()) {
                int row = _bindingsTable.getRowAt(anEvent.getX(), anEvent.getY()); if(row<0) return;
                String pname = shape.getPropNames()[row];
                String bkey = KeysPanel.getDragKey();
                shape.addBinding(pname, bkey);
            }
            anEvent.dropComplete();
        }
    }
    
    // Handle BindingsText
    if(anEvent.equals("BindingsText")) {
        
        // Get selected PropertyName and Key
        String pname = _bindingsTable.getSelItem(); if(pname==null) return;
        String key = getViewStringValue("BindingsText"); if(key!=null && key.length()==0) key = null;
        
        // Remove previous binding and add new one (if valid)
        for(RMShape shp : shapes)
            if(key!=null) shp.addBinding(pname, key);
            else shp.removeBinding(pname);
    }
}

/**
 * Returns the current selected shape for the current editor.
 */
public RMShape getSelectedShape()
{
    RMEditor e = getEditor(); if(e==null) return null;
    return e.getSelectedOrSuperSelectedShape();
}

/**
 * Returns the current selected shape for the current editor.
 */
public List <? extends RMShape> getSelectedShapes()
{
    RMEditor e = getEditor(); if(e==null) return Collections.EMPTY_LIST;
    return e.getSelectedOrSuperSelectedShapes();
}

/**
 * Called to configure BindingsTable.
 */
private void configureBindingsTable(ListCell <String> aCell)
{
    if(aCell.getCol()==0) return;
    String pname = aCell.getItem();
    RMShape shape = getSelectedShape(); if(shape==null) return;
    Binding binding = getSelectedShape().getBinding(pname);
    aCell.setText(binding!=null? binding.getKey() : null);
}

/**
 * Returns the name to be used in the inspector's window title.
 */
public String getWindowTitle()  { return "General Inspector"; }

}