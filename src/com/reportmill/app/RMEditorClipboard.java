/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.RMXString;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Point;
import snap.util.*;
import snap.view.*;

/**
 * Handles editor methods specific to clipboard operations (cut, copy paste).
 */
public class RMEditorClipboard {
    
    // The MIME type for reportmill xstring
    public static final String    RM_XML_TYPE = "reportmill/xml";
    
/**
 * Handles editor cut operation.
 */
public static void cut(RMEditor anEditor)
{
    // If text editing, have text editor do copy instead
    if(anEditor.getTextEditor()!=null)
        anEditor.getTextEditor().cut();
    
    // If not text editing, do copy and delete (and null anchor & smart paste shape)
    else {
        anEditor.copy();
        anEditor.delete();
        anEditor._lastCopyShape = anEditor._lastPasteShape = null;
    }
}

/**
 * Handles editor copy operation.
 */
public static void copy(RMEditor anEditor)
{
    // If text editing, have text editor do copy instead
    if(anEditor.getTextEditor()!=null)
        anEditor.getTextEditor().copy();

    // If not text editing, add selected shapes (serialized) to pasteboard for DrawPboardType
    else if(!(anEditor.getSelectedOrSuperSelectedShape() instanceof RMDocument) &&
            !(anEditor.getSelectedOrSuperSelectedShape() instanceof RMPage)) {
        
        // Get xml for selected shapes
        XMLElement xml = new RMArchiver().writeObject(anEditor.getSelectedOrSuperSelectedShapes());
        String xmlStr = xml.toString();
        
        // Get System clipboard and add data as RMData and String (text/plain)
        Clipboard cb = Clipboard.getCleared();
        cb.addData(RM_XML_TYPE, xmlStr);
        cb.addData(xmlStr);
        
        // Reset Editor.LastCopyShape/LastPasteShape
        anEditor._lastCopyShape = anEditor.getSelectedShape(0); anEditor._lastPasteShape = null;
    }
    
    // Otherwise beep
    else ViewUtils.beep();
}

/**
 * Handles editor paste operation.
 */
public static void paste(RMEditor anEditor)
{
    // If text editing, have text editor do paste instead
    if(anEditor.getTextEditor()!=null)
        anEditor.getTextEditor().paste();
    
    // If not text editing, do paste for system clipboard
    else {
        RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
        paste(anEditor, Clipboard.get(), parent, null);
    }
}

/**
 * Handles editor paste operation for given transferable, parent shape and location.
 */
public static void paste(RMEditor anEditor, Clipboard aCB, RMParentShape aParent, Point aPoint)
{
    // Declare variable for pasted shape
    RMShape pastedShape = null;

    // If PasteBoard has ReportMill Data, paste it
    if(aCB.hasData(RM_XML_TYPE)) {
        
        // Unarchive shapes from clipboard bytes
        Object object = getShapesFromClipboard(anEditor, aCB);
        
        // If data is list of previously copied shapes, add them
        if(object instanceof List) {
            List shapes = (List)object;
            anEditor.undoerSetUndoTitle("Paste Shape" + (shapes.size()>1? "s" : ""));
            anEditor.addShapesToShape(shapes, aParent, true);
            anEditor.setSelectedShapes(shapes);
        }
        
        // If data is text, create text object and add it
        else if(object instanceof RMXString) {
            RMTextShape text = new RMTextShape((RMXString)object);
            double width = Math.min(text.getPrefWidth(), aParent.getWidth());
            double height = Math.min(text.getPrefHeight(), aParent.getHeight());
            text.setSize(width, height);
            anEditor.undoerSetUndoTitle("Paste Text");
            anEditor.addShapesToShape(Arrays.asList(text), aParent, true);
            anEditor.setSelectedShape(text);
        }
        
        // Promote _smartPastedShape to anchor and set new _smartPastedShape
        if(anEditor._lastPasteShape!=null)
            anEditor._lastCopyShape = anEditor._lastPasteShape;
        anEditor._lastPasteShape = anEditor.getSelectedShape(0);
        
    }
    
    // Paste Image
    else if(aCB.hasImage()) {
        ClipboardData idata = aCB.getImageData();
        byte bytes[] = idata.getBytes();
        pastedShape = new RMImageShape(bytes);
    }
    
    // paste pdf
    else if((pastedShape=getTransferPDF(aCB)) != null) { }
    
    // last one - plain text
    else if((pastedShape=getTransferText(aCB)) != null) { }
        
    // Might as well log unsupported paste types
    else { //for(String type : aCB.getMIMETypes()) System.err.println("Unsupported type: " + type);
        ViewUtils.beep(); }

    // Add pastedShape
    if(pastedShape!=null) {
        
        // Set undo title
        anEditor.undoerSetUndoTitle("Paste");
        
        // Resize/relocate shape (if point was provided, move pasted shape to that point)
        pastedShape.setBestSize();
        if(aPoint!=null) {
            aPoint = anEditor.convertToShape(aPoint.x, aPoint.y, aParent);
            pastedShape.setXY(aPoint.getX() - pastedShape.getWidth()/2, aPoint.getY() - pastedShape.getHeight()/2);
        }
        
        // Add pasted shape to parent
        aParent.addChild(pastedShape);

        // Select imageShape, set selectTool and redisplay
        anEditor.setSelectedShape(pastedShape);
        anEditor.setCurrentToolToSelectTool();
        anEditor.repaint();
    }
}

/**
 * Returns the first Shape read from the system clipboard.
 */
public static RMShape getShapeFromClipboard(RMEditor anEditor)
{
    Object shapes = getShapesFromClipboard(anEditor, null);
    if(shapes instanceof List) shapes = ListUtils.get((List)shapes, 0);
    return shapes instanceof RMShape? (RMShape)shapes : null;
}

/**
 * Returns the shape or shapes read from the given transferable (uses system clipboard if null).
 */
public static Object getShapesFromClipboard(RMEditor anEditor, Clipboard aCB)
{
    // If no contents, use system clipboard
    Clipboard cboard = aCB!=null? aCB : Clipboard.get();
    
    // If no RMData, just return
    if(!cboard.hasData(RM_XML_TYPE))
        return null;

    // Get unarchived object from clipboard bytes
    byte bytes[] = cboard.getDataBytes(RM_XML_TYPE);
    Object obj = new RMArchiver().readObject(bytes);

    // A bit of a hack - remove any non-shapes (plugins for one)
    if(obj instanceof List) { List list = (List)obj;
        for(int i=list.size()-1; i>=0; --i)
            if(!(list.get(i) instanceof RMShape))
                list.remove(i);
    }
        
    // Return object
    return obj;
}

/**
 * Returns an RMText object with the contents if there's a plain text string on the clipboard.
 */
public static RMShape getTransferText(Clipboard aCB) 
{
    if(!aCB.hasString()) return null;
    String str = aCB.getString();
    return str!=null? new RMTextShape(str) : null;
}

/**
 * Returns an RMImage with the contents if there's a pdf image on the clipboard.
 */
public static RMShape getTransferPDF(Clipboard aCB) 
{
    if(!aCB.hasData("application/pdf")) return null;
    byte bytes[] = aCB.getDataBytes("application/pdf");
    return bytes!=null? new RMPDFShape(bytes) : null;
}

}