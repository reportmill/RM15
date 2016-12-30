/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.RMXString;
import com.reportmill.shape.*;
import java.io.*;
import java.util.*;
import snap.gfx.Point;
import snap.util.*;
import snap.view.*;

/**
 * Handles editor methods specific to clipboard operations (cut, copy paste).
 */
public class RMEditorClipboard {
    
    // A defined data flavor for RM shapes and DataFlavors supported by RMEditor
    //public static DataFlavor RMDataFlavor = new DataFlavor("application/reportmill", "ReportMill Shape Data");
    //public static DataFlavor SupportedFlavors[] = { RMDataFlavor, DataFlavor.stringFlavor };
    
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
        
        // Get xml for selected shapes, create and set in EditorClipboard and install in SystemClipboard
        XMLElement xml = new RMArchiver().writeObject(anEditor.getSelectedOrSuperSelectedShapes());
        //RMEditorClipboard ec = new RMEditorClipboard(xml.getBytes());tkit.getSystemClipboard().setContents(ec,null);
        Clipboard cb = Clipboard.get();
        cb.setContent("RMData", xml.getBytes(), Clipboard.STRING, xml.toString());
        
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
        Clipboard cb = Clipboard.get(); //tkit.getSystemClipboard().getContents(null);
        RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
        paste(anEditor, cb, parent, null);
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
    if(aCB.hasContent("RMData")) try { //isDataFlavorSupported(RMDataFlavor)) try {
        
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
    
    // Catch paste RMData exceptions
    catch(Exception e) { e.printStackTrace(); }
    
    // Paste Image
    //else if(aCB.isDataFlavorSupported(DataFlavor.imageFlavor)) try {
    //    Image image = (Image)contents.getTransferData(DataFlavor.imageFlavor);
    //    byte bytes[] = RMAWTUtils.getBytesJPEG(image);
    //    pastedShape = new RMImageShape(bytes); }
    //catch(Exception e) { e.printStackTrace(); }
    
    // paste pdf
    else if((pastedShape=getTransferPDF(aCB)) != null) { }
    
    // last one - plain text
    else if((pastedShape=getTransferText(aCB)) != null) { }
        
    // Might as well log unsupported paste types
    else {
        //DataFlavor flvrs[] = contents.getTransferDataFlavors();
        //for(DataFlavor f : flvrs) System.err.println("Unsupported flavor: " + f.getMimeType() + " " + f.getSubType());
        ViewUtils.beep();
    }

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
    if(aCB==null)
        aCB = Clipboard.get(); // tkit.getSystemClipboard().getContents(null);        
    
    // If PasteBoard has ReportMill Data, paste it
    if(aCB.hasContent("RMData")) try { //isDataFlavorSupported(RMDataFlavor)) try {
    
        // Get bytes from clipboard
        InputStream bis = (InputStream)aCB.getContent("RMData"); //getTransferData(RMDataFlavor);
        byte bytes[] = new byte[bis.available()];
        bis.read(bytes);
        
        // Get unarchived object from clipboard bytes
        Object object = new RMArchiver().readObject(bytes);

        // A bit of a hack - remove any non-shapes (plugins for one)
        if(object instanceof List) { List list = (List)object;
            for(int i=list.size()-1; i>=0; --i)
                if(!(list.get(i) instanceof RMShape))
                    list.remove(i);
        }
        
        // Return object
        return object;
    }
    
    // Handle exceptions and return
    catch(Exception e) { e.printStackTrace(); }
    return null;
}

/**
 * Returns an RMText object with the contents if there's a plain text string on the clipboard.
 */
public static RMShape getTransferText(Clipboard aCB) 
{
    String string = aCB.getString();
    return string==null? null : new RMTextShape(string);
}

/**
 * Returns an RMImage with the contents if there's a pdf image on the clipboard.
 */
public static RMShape getTransferPDF(Clipboard aCB) 
{
    try {
        //DataFlavor pdflav = new DataFlavor("application/pdf");
        if(aCB.hasContent("application/pdf")) { //.isDataFlavorSupported(pdflav)) {
            InputStream ps = (InputStream)aCB.getContent("application/pdf"); //contents.getTransferData(pdflav);
            if(ps!=null) return new RMImageShape(ps);
        }
    }
    catch(Exception e) { e.printStackTrace(); } return null;
}

/** Transferable methods. */
//public DataFlavor[] getTransferDataFlavors()  { return SupportedFlavors; }
//public boolean isDataFlavorSupported(DataFlavor f){ return f.equals(RMDataFlavor)||f.equals(DataFlavor.stringFlavor);}
/*public Object getTransferData(DataFlavor aFlavor) throws UnsupportedFlavorException, IOException {
    if(aFlavor.equals(RMDataFlavor)) return new ByteArrayInputStream(_bytes);
    if(aFlavor.equals(DataFlavor.stringFlavor)) return new String(_bytes);
    throw new UnsupportedFlavorException(aFlavor); }*/

}