/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.base.RMFormat;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.util.ListUtils;
import snap.view.ViewUtils;

/**
 * Handles useful methods to help editor.
 */
public class RMEditorUtils {

    // The last color set by or returned to the color panel
    static Color    _lastColor = Color.BLACK;

/**
 * Groups the given shape list to the given group shape.
 * If given shapes list is null, use editor selected shapes.
 * If given group shape is null, create new generic group shape.
 */
public static void groupShapes(RMEditor anEditor, List <RMShape> theShapes, RMParentShape aGroupShape)
{
    // If shapes not provided, use editor selected shapes
    if(theShapes==null)
        theShapes = anEditor.getSelectedShapes();
    
    // If there are less than 2 selected shapes play a beep (the user really should know better)
    if(theShapes.size()==0) { anEditor.beep(); return; }
    
    // Set undo title
    anEditor.undoerSetUndoTitle("Group");

    // Get copy of shapes, sorted by their original index in parent
    List <RMShape> shapes = RMShapeUtils.getShapesSortedByIndex(theShapes);
    
    // Get parent
    RMParentShape parent = shapes.get(0).getParent();
    
    // If no group shape, create one
    if(aGroupShape==null) {
        aGroupShape = new RMSpringShape();
        aGroupShape.setBounds(RMShapeUtils.getBoundsOfChildren(parent, shapes));
    }

    // Add groupShape to the current parent (with no transform)
    parent.addChild(aGroupShape);

    // Iterate over children and group to GroupShape
    for(RMShape child : shapes)
        groupShape(child, aGroupShape);
    
    // Select group shape
    anEditor.setSelectedShape(aGroupShape);
}

/**
 * Adds child shape to group shape.
 */
private static void groupShape(RMShape child, RMParentShape gshape)
{
    // Get center point in parent coords and store as child x/y
    RMParentShape parent = child.getParent();
    Point cp = child.localToParent(child.getWidth()/2, child.getHeight()/2);
    child.setXY(cp.x, cp.y);
    
    // Move child to GroupShape
    parent.removeChild(child);
    gshape.addChild(child);
        
    // Undo transforms of group shape
    child.setRoll(child.getRoll() - gshape.getRoll());
    child.setScaleX(child.getScaleX()/gshape.getScaleX()); child.setScaleY(child.getScaleY()/gshape.getScaleY());
    child.setSkewX(child.getSkewX() - gshape.getSkewX()); child.setSkewY(child.getSkewY() - gshape.getSkewY());
    
    // Reset center point: Get old center point in GroupShape coords and offset child by new center in GroupShape coords
    cp = gshape.parentToLocal(cp.x, cp.y);
    Point cp2 = child.localToParent(child.getWidth()/2, child.getHeight()/2);
    child.offsetXY(cp.x - cp2.x, cp.y - cp2.y);
}

/**
 * Ungroups any currently selected group shapes.
 */
public static void ungroupShapes(RMEditor anEditor)
{
    // Get currently super selected shape and create list to hold ungrouped shapes
    List <RMShape> ungroupedShapes = new Vector();
    
    // Register undo title for ungrouping
    anEditor.undoerSetUndoTitle("Ungroup");

    // See if any of the selected shapes can be ungrouped
    for(RMShape shape : anEditor.getSelectedShapes()) {
        
        // If shape cann't be ungrouped, skip
        if(!anEditor.getTool(shape).isUngroupable(shape)) continue;
        RMParentShape groupShape = (RMParentShape)shape;
        RMParentShape parent = groupShape.getParent();
            
        // Iterate over children and ungroup from GroupShape
        for(RMShape child : groupShape.getChildArray()) {
            ungroupShape(child);
            ungroupedShapes.add(child);
        }

        // Remove groupShape from parent
        parent.removeChild(groupShape);
    }

    // If were some ungroupedShapes, select them (set selected objects for undo/redo)
    if(ungroupedShapes.size()>0)
        anEditor.setSelectedShapes(ungroupedShapes);

    // If no ungroupedShapes, beep at silly user
    else anEditor.beep();
}

/**
 * Transforms given shape to world coords.
 */
private static void ungroupShape(RMShape child)
{
    // Get center point in parent coords and store as child x/y
    RMParentShape gshape = child.getParent(), parent = gshape.getParent();
    Point cp = child.localToParent(child.getWidth()/2, child.getHeight()/2, parent);
    child.setXY(cp.x, cp.y);
    
    // Coalesce transforms up the parent chain
    child.setRoll(child.getRoll() + gshape.getRoll());
    child.setScaleX(child.getScaleX() * gshape.getScaleX()); child.setScaleY(child.getScaleY() * gshape.getScaleY());
    child.setSkewX(child.getSkewX() + gshape.getSkewX()); child.setSkewY(child.getSkewY() + gshape.getSkewY());

    // Remove from group shape & add to group shape parent
    gshape.removeChild(child);
    parent.addChild(child);
    
    // Reset center point: Get new center in parent coords and offset child by change
    Point cp2 = child.localToParent(child.getWidth()/2, child.getHeight()/2);
    child.offsetXY(cp.x - cp2.x, cp.y - cp2.y);
}

/**
 * Orders all currently selected shapes to the front.
 */
public static void bringToFront(RMEditor anEditor)
{
    RMParentShape parent = anEditor.getSuperSelectedParentShape();
    if(parent==null || anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Bring to Front");
    parent.bringShapesToFront(anEditor.getSelectedShapes());
}

/**
 * Orders all currently selected shapes to the back.
 */
public static void sendToBack(RMEditor anEditor)
{
    RMParentShape parent = anEditor.getSuperSelectedParentShape();
    if(parent==null || anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Send to Back");
    parent.sendShapesToBack(anEditor.getSelectedShapes());
}

/**
 * Arranges currently selected shapes in a row relative to their top.
 */
public static void makeRowTop(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Row Top");
    double minY = anEditor.getSelectedShape().getFrameY();
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameY(minY);
}

/**
 * Arranges currently selected shapes in a row relative to their center.
 */
public static void makeRowCenter(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Row Center");
    double midY = anEditor.getSelectedShape().getFrame().getMidY();
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameY(midY - shape.getHeight()/2);
}

/**
 * Arranges currently selected shapes in a row relative to their bottom.
 */
public static void makeRowBottom(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Row Bottom");
    double maxY = anEditor.getSelectedShape().getFrameMaxY();
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameY(maxY - shape.getHeight());
}

/**
 * Arranges currently selected shapes in a column relative to their left border.
 */
public static void makeColumnLeft(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Column Left");
    double minX = anEditor.getSelectedShape().getFrameX();
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameX(minX);
}

/**
 * Arranges currently selected shapes in a column relative to their center.
 */
public static void makeColumnCenter(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Column Center");
    double midX = anEditor.getSelectedShape().getFrame().getMidX();
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameX(midX - shape.getWidth()/2);
}

/**
 * Arranges currently selected shapes in a column relative to their right border.
 */
public static void makeColumnRight(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Column Right");
    double maxX = anEditor.getSelectedShape().getFrameMaxX();    
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameX(maxX - shape.getWidth());
}

/**
 * Makes currently selected shapes all have the same width and height as the first selected shape.
 */
public static void makeSameSize(RMEditor anEditor)
{
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    anEditor.undoerSetUndoTitle("Make Same Size");
    Size size = anEditor.getSelectedShape().getSize();
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setSize(size.getWidth(), size.getHeight());
}

/**
 * Makes currently selected shapes all have the same width as the first selected shape.
 */
public static void makeSameWidth(RMEditor anEditor)
{
    // If no shapes, beep and return
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    
    // Register undo title
    anEditor.undoerSetUndoTitle("Make Same Width");
    
    // Get first selected shape width
    double width = anEditor.getSelectedShape().getWidth();
    
    // Iterate over selected shapes and set width
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setWidth(width);
}

/**
 * Makes currently selected shapes all have the same height as the first selected shape.
 */
public static void makeSameHeight(RMEditor anEditor)
{
    // If no shapes, beep and return
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    
    // Register undo title
    anEditor.undoerSetUndoTitle("Make Same Height");
    
    // Get first selected shape height
    double height = anEditor.getSelectedShape().getHeight();
    
    // Iterate over selected shapes and set height
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setHeight(height);
}

/**
 * Makes currently selected shapes size to fit content.
 */
public static void setSizeToFit(RMEditor anEditor)
{
    // If no shapes, beep and return
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    
    // Register undo title
    anEditor.undoerSetUndoTitle("Size to Fit");
    
    // Iterate over shapes and size to fit
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setBestSize();
}

/**
 * Arranges currently selected shapes such that they have the same horizontal distance between them.
 */
public static void equallySpaceRow(RMEditor anEditor)
{
    // If no selected shapes, beep and return
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    
    // Get selectedShapes sorted by minXInParentBounds
    List <RMShape> shapes = RMShapeUtils.getShapesSortedByFrameX(anEditor.getSelectedShapes());
    float spaceBetweenShapes = 0;

    // Calculate average space between shapes
    for(int i=1, iMax=shapes.size(); i<iMax; i++)
        spaceBetweenShapes += shapes.get(i).getFrameX() - shapes.get(i-1).getFrameMaxX();
    if(shapes.size()>1)
        spaceBetweenShapes = spaceBetweenShapes/(shapes.size()-1);
    
    // Reset average space between shapes
    anEditor.undoerSetUndoTitle("Equally Space Row");
    for(int i=1, iMax=shapes.size(); i<iMax; i++) {
        RMShape shape = shapes.get(i);
        RMShape lastShape = shapes.get(i-1);
        double tx = lastShape.getFrameMaxX() + spaceBetweenShapes;
        shape.setFrameX(tx);
    }
}

/**
 * Arranges currently selected shapes such that they have the same vertical distance between them.
 */
public static void equallySpaceColumn(RMEditor anEditor)
{
    // If no selected shapes, beep and return
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    
    // Get selectedShapes sorted by minXInParentBounds
    List <RMShape> shapes = RMShapeUtils.getShapesSortedByFrameY(anEditor.getSelectedShapes());
    float spaceBetweenShapes = 0;

    // Calculate average space between shapes
    for(int i=1, iMax=shapes.size(); i<iMax; i++)
        spaceBetweenShapes += shapes.get(i).getFrameY() - shapes.get(i-1).getFrameMaxY();
    if(shapes.size()>1)
        spaceBetweenShapes = spaceBetweenShapes/(shapes.size()-1);

    // Reset average space between shapes
    anEditor.undoerSetUndoTitle("Equally Space Column");
    for(int i=1, iMax=shapes.size(); i<iMax; i++) {
        RMShape shape = shapes.get(i);
        RMShape lastShape = shapes.get(i-1);
        double ty = lastShape.getFrameMaxY() + spaceBetweenShapes;
        shape.setFrameY(ty);
    }
}

/**
 * Adds the selected shapes to a Switch Shape.
 */
public static void groupInSwitchShape(RMEditor anEditor)
{
    // Get selected shapes and parent (just return if no shapes)
    List <RMShape> shapes = anEditor.getSelectedShapes(); if(shapes.size()==0) { anEditor.beep(); return; }
    RMShape parent = anEditor.getSelectedShape(0).getParent();
    
    // Create switch shape to hold selected shapes with fram of combined bounds of children (ouset by just a little)
    RMSwitchShape groupShape = new RMSwitchShape();
    groupShape.setFrame(RMShapeUtils.getBoundsOfChildren(parent, shapes).getInsetRect(-2));

    // Add shapes to group shape (with undo title)
    anEditor.undoerSetUndoTitle("Group in Switch Shape");
    groupShapes(anEditor, shapes, groupShape);
}

/**
 * Adds the selected shapes to a Scene3D Shape.
 */
public static void groupInScene3D(RMEditor anEditor)
{
    // If no shapes, beep and return
    if(anEditor.getSelectedShapeCount()==0) { anEditor.beep(); return; }
    
    // Get selected shapes
    List <RMShape> selectedShapes = ListUtils.clone(anEditor.getSelectedShapes());
    
    // Get parent
    RMParentShape parent = anEditor.getSelectedShape(0).getParent();
    
    // Get new Scene3D to group selected shapes in
    RMScene3D groupShape = new RMScene3D();
    
    // Set scene3D to combined bounds of children
    groupShape.setFrame(RMShapeUtils.getBoundsOfChildren(parent, selectedShapes));

    // Set undo title
    anEditor.undoerSetUndoTitle("Group in Scene3D");
    
    // Iterate over children and add to group shape
    for(int i=0, iMax=selectedShapes.size(); i<iMax; i++) {
        RMShape shape = selectedShapes.get(i);
        groupShape.addShapeRM(shape);
        shape.removeFromParent();
        shape.setXY(shape.x() - groupShape.x(), shape.y() - groupShape.y());
    }
    
    // Add group shape to original parent
    parent.addChild(groupShape);
    
    // Select new shape
    anEditor.setSelectedShape(groupShape);
}

/**
 * Create new shape by coalescing the outer perimeters of the currently selected shapes.
 */
public static void combinePaths(RMEditor anEditor)
{
    // If shapes less than 2, just beep and return
    if(anEditor.getSelectedShapeCount()<2) { anEditor.beep(); return; }
    
    // Get selected shapes and create CombinedShape
    List <RMShape> selectedShapes = ListUtils.clone(anEditor.getSelectedShapes());
    RMPolygonShape combinedShape = RMShapeUtils.getCombinedPathsShape(selectedShapes);
    
    // Remove original children and replace with CombinedShape
    anEditor.undoerSetUndoTitle("Add Paths");
    RMParentShape parent = anEditor.getSuperSelectedParentShape();
    for(RMShape shape : selectedShapes) parent.removeChild(shape);
    parent.addChild(combinedShape);
    
    // Select CombinedShape
    anEditor.setSelectedShape(combinedShape);
}

/**
 * Create new shape by coalescing the outer perimeters of the currently selected shapes.
 */
public static void subtractPaths(RMEditor anEditor)
{
    // If shapes less than 2, just beep and return
    if(anEditor.getSelectedShapeCount()<2) { anEditor.beep(); return; }
    
    // Get selected shapes and create SubtractedShape
    List <RMShape> selectedShapes = ListUtils.clone(anEditor.getSelectedShapes());
    RMPolygonShape subtractedShape = RMShapeUtils.getSubtractedPathsShape(selectedShapes, 0);
    
    // Remove original children and replace with SubtractedShape
    anEditor.undoerSetUndoTitle("Subtract Paths");
    RMParentShape parent = anEditor.getSuperSelectedParentShape();
    for(RMShape shape : selectedShapes) parent.removeChild(shape);
    parent.addChild(subtractedShape);
    
    // Select SubtractedShape
    anEditor.setSelectedShape(subtractedShape);
}

/**
 * Converts currently selected shape to image.
 */
public static void convertToImage(RMEditor anEditor)
{
    // Get currently selected shape (if shape is null, just return)
    RMShape shape = anEditor.getSelectedShape(); if(shape==null) return;
    
    // Get image for shape, get PNG bytes for image and create new RMImageShape for bytes
    Image image = RMShapeUtils.createImage(shape, null);
    byte imageBytes[] = image.getBytesPNG();
    RMImageShape imageShape = new RMImageShape(imageBytes);
    
    // Set ImageShape XY and add to parent
    imageShape.setXY(shape.getX() + shape.getBoundsMarked().getX(), shape.getY() + shape.getBoundsMarked().getY());
    shape.getParent().addChild(imageShape, shape.indexOf());
    
    // Replace old selectedShape with image and remove original shape
    anEditor.setSelectedShape(imageShape);
    shape.removeFromParent();
}

/**
 * Moves all the currently selected shapes one point to the right.
 */
public static void moveRightOnePoint(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Move Right One Point");
    RMDocument doc = anEditor.getDoc();
    double offset = doc.getSnapGrid()? doc.getGridSpacing() : 1;
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameX(shape.getFrameX() + offset);
}

/**
 * Moves all the currently selected shapes one point to the left.
 */
public static void moveLeftOnePoint(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Move Left One Point");
    RMDocument doc = anEditor.getDoc();
    double offset = doc.getSnapGrid()? doc.getGridSpacing() : 1;
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameX(shape.getFrameX() - offset);
}

/**
 * Moves all the currently selected shapes one point up.
 */
public static void moveUpOnePoint(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Move Up One Point");
    RMDocument doc = anEditor.getDoc();
    double offset = doc.getSnapGrid()? doc.getGridSpacing() : 1;
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameY(shape.getFrameY() - offset);
}

/**
 * Moves all the currently selected shapes one point down.
 */
public static void moveDownOnePoint(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Move Down One Point");
    RMDocument doc = anEditor.getDoc();
    double offset = doc.getSnapGrid()? doc.getGridSpacing() : 1;
    for(RMShape shape : anEditor.getSelectedShapes())
        shape.setFrameY(shape.getFrameY() + offset);
}

/**
 * Moves all the currently selected shapes to a new page layer.
 */
public static void moveToNewLayer(RMEditor anEditor)
{
    RMDocument doc = anEditor.getDoc();
    if(anEditor.getSelectedShapeCount()==0 || doc==null) { anEditor.beep(); return; }
    doc.getSelPage().moveToNewLayer(anEditor.getSelectedShapes());
}

/**
 * Returns the specified type of color (text, stroke or fill) of editor's selected shape.
 */
public static Color getSelectedColor(RMEditor anEditor)
{
    // Get selected or super selected shape
    RMShape shape = anEditor.getSelectedOrSuperSelectedShape();
    
    // If selected or super selected shape is page that doesn't draw color, return "last color" (otherwise, reset it)
    if((shape instanceof RMPage || shape instanceof RMDocument) && shape.getFill()==null)
        return _lastColor;
    else _lastColor = Color.BLACK;
        
    // If text color and text editing, return color of text editor
    if(anEditor.getTextEditor()!=null)
        return anEditor.getTextEditor().getColor();
        
    // Return selected shape's color
    return anEditor.getSelectedOrSuperSelectedShape().getColor();
}

/**
 * Sets the specified type of color (text, stroke or fill) of editor's selected shape.
 */
public static void setSelectedColor(RMEditor anEditor, Color aColor)
{
    // Get selected or super selected shape
    RMColor color = RMColor.get(aColor);
    RMShape shape = anEditor.getSelectedOrSuperSelectedShape();
        
    // If editor selected or super selected shape is document or page, set "last color" and return
    if(shape instanceof RMPage || shape instanceof RMDocument) {
        _lastColor = aColor; return; }

    // If text color and text editing, return color of text editor
    if(anEditor.getTextEditor()!=null) {
        
        // Get text editor
        RMTextEditor ted = anEditor.getTextEditor();
        
        // If command down, and text is outlined, set color of outline instead
        if(ViewUtils.isMetaDown() && ted.getTextBorder()!=null) {
            Border lbrdr = ted.getTextBorder();
            ted.setTextBorder(Border.createLineBorder(aColor, lbrdr.getWidth()));
        }
        
        // If no command down, set color of text editor
        else ted.setColor(color);
    }
    
    // If fill color, set selected shapes' fill color
    else {
    
        // If command-click, set gradient fill
        if(ViewUtils.isMetaDown()) {
            RMColor c1 = shape.getFill()!=null? shape.getColor() : RMColor.clearWhite;
            shape.setFill(new RMGradientFill(c1, color, 0));
        }
        
        // If not command click, just set the color of all the selected shapes
        else setColor(anEditor, color);
    }
}

/**
 * Sets the fill color of the editor's selected shapes.
 */
public static void setColor(RMEditor anEditor, RMColor aColor)
{
    // Iterate over editor selected shapes or super selected shape
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        shape.setColor(aColor);
}

/**
 * Sets the stroke color of the editor's selected shapes.
 */
public static void setStrokeColor(RMEditor anEditor, RMColor aColor)
{
    // Iterate over editor selected shapes or super selected shape
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        shape.setStrokeColor(aColor);
}

/**
 * Sets the text color of the editor's selected shapes.
 */
public static void setTextColor(RMEditor anEditor, RMColor aColor)
{
    // If text editing, forward on to text editor
    if(anEditor.getTextEditor()!=null)
        anEditor.getTextEditor().setColor(aColor);
        
    // Otherwise, iterate over editor selected shapes or super selected shape
    else for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        shape.setTextColor(aColor);
}

/**
 * Returns the font of editor's selected shape.
 */
public static RMFont getFont(RMEditor anEditor)
{
    RMFont font = null;
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax && font==null; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        font = tool.getFont(anEditor, shape);
    }
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax && font==null; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        font = tool.getFontDeep(anEditor, shape);
    }
    return font!=null? font : RMFont.getDefaultFont();
}

/**
 * Sets the font family of editor's selected shape(s).
 */
public static void setFontFamily(RMEditor anEditor, Font aFont)
{
    RMFont font = RMFont.get(aFont);
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        tool.setFontKeyDeep(anEditor, shape, RMTool.FontFamily_Key, font);
    }
}

/**
 * Sets the font name of editor's selected shape(s).
 */
public static void setFontName(RMEditor anEditor, Font aFont)
{
    RMFont font = RMFont.get(aFont);
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        tool.setFontKeyDeep(anEditor, shape, RMTool.FontName_Key, font);
    }
}

/**
 * Sets the font size of editor's selected shape(s).
 */
public static void setFontSize(RMEditor anEditor, float aSize, boolean isRelative)
{
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        String key = isRelative? RMTool.FontSizeDelta_Key : RMTool.FontSize_Key;
        tool.setFontKeyDeep(anEditor, shape, key, aSize);
    }
}

/**
 * Sets the "boldness" of text in the currently selected shapes.
 */
public static void setFontBold(RMEditor anEditor, boolean aFlag)
{
    anEditor.undoerSetUndoTitle("Make Bold");
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        tool.setFontKeyDeep(anEditor, shape, RMTool.FontBold_Key, aFlag);
    }
}

/**
 * Sets the italic state of text in the currently selected shapes.
 */
public static void setFontItalic(RMEditor anEditor, boolean aFlag)
{
    anEditor.undoerSetUndoTitle("Make Italic");
    for(int i=0, iMax=anEditor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = anEditor.getSelectedOrSuperSelectedShape(i);
        RMTool tool = anEditor.getTool(shape);
        tool.setFontKeyDeep(anEditor, shape, RMTool.FontItalic_Key, aFlag);
    }
}

/**
 * Returns whether the currently selected shape is underlined.
 */
public static boolean isUnderlined(RMEditor anEdtr)  { return anEdtr.getSelectedOrSuperSelectedShape().isUnderlined(); }

/**
 * Sets the currently selected shapes to be underlined.
 */
public static void setUnderlined(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Make Underlined");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        shape.setUnderlined(!shape.isUnderlined());
}

/**
 * Returns the outline state of the currently selected shape (null if none).
 */
public static Border getTextBorder(RMEditor anEditor)
{
    RMShape shp = anEditor.getSelectedOrSuperSelectedShape();
    RMTextShape tshp = shp instanceof RMTextShape? (RMTextShape)shp : null; if(tshp==null) return null;
    return tshp.getTextBorder();
}

/**
 * Sets the currently selected shapes to be outlined.
 */
public static void setTextBorder(RMEditor anEditor)
{
    if(getTextBorder(anEditor)==null) {
        setTextBorder(anEditor, Border.createLineBorder(Color.BLACK,1));
        setTextColor(anEditor, RMColor.white);
    }
    else {
        setTextBorder(anEditor, null);
        setTextColor(anEditor, RMColor.black);
    }
}

/**
 * Sets the outline state of the currently selected shapes.
 */
public static void setTextBorder(RMEditor anEditor, Border aBorder)
{
    anEditor.undoerSetUndoTitle("Make Outlined");
    for(RMShape shp : anEditor.getSelectedOrSuperSelectedShapes()) {
        if(shp instanceof RMTextShape)
            ((RMTextShape)shp).setTextBorder(aBorder);
    }
}

/**
 * Returns the horizontal alignment of the text of the currently selected shapes.
 */
public static RMTypes.AlignX getAlignmentX(RMEditor anEditor)
{
    return anEditor.getSelectedOrSuperSelectedShape().getAlignmentX();
}

/**
 * Sets the horizontal alignment of the text of the currently selected shapes.
 */
public static void setAlignmentX(RMEditor anEditor, RMTypes.AlignX anAlign)
{
    anEditor.undoerSetUndoTitle("Alignment Change");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        shape.setAlignmentX(anAlign);
}

/**
 * Sets the currently selected shapes to show text as superscript.
 */
public static void setSuperscript(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Make Superscript");
    RMTextEditor ted = anEditor.getTextEditor();
    if(ted!=null)
        ted.setSuperscript();
}

/**
 * Sets the currently selected shapes to show text as subscript.
 */
public static void setSubscript(RMEditor anEditor)
{
    anEditor.undoerSetUndoTitle("Make Subscript");
    RMTextEditor ted = anEditor.getTextEditor();
    if(ted!=null)
        ted.setSubscript();
}

/**
 * Returns the format of the editor's selected shape.
 */
public static RMFormat getFormat(RMEditor anEditor)  { return anEditor.getSelectedOrSuperSelectedShape().getFormat(); }

/**
 * Sets the format of editor's selected shape(s).
 */
public static void setFormat(RMEditor anEditor, RMFormat aFormat)
{
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        shape.setFormat(aFormat);
}

/**
 * Splits the selected shape in half on the horizontal axis.
 */
public static void splitHorizontal(RMEditor editor)
{
    editor.undoerSetUndoTitle("Split Column");
    RMShape shape = editor.getSuperSelectedShape();
    RMParentShape parent = shape.getParent();
    shape.repaint();
    shape = shape.divideShapeFromLeft(shape.getWidth()/2);
    parent.addChild(shape);
    editor.setSuperSelectedShape(shape);
}

/**
 * Adds an image placeholder to the given editor.
 */
public static void addImagePlaceholder(RMEditor anEditor)
{
    // Create image shape
    RMImageShape imageShape = new RMImageShape(null);
    
    // Get parent and move image shape to center
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    imageShape.setFrame((int)(parent.getWidth()/2 - 24), (int)(parent.getHeight()/2 - 24), 48, 48);

    // Set image in image shape and add imageShape to mainShape
    anEditor.undoerSetUndoTitle("Add Image");
    parent.addChild(imageShape);

    // Select imageShape, set selectTool and redisplay
    anEditor.setSelectedShape(imageShape);
    anEditor.setCurrentToolToSelectTool();
    anEditor.repaint();
}

/**
 * Adds a subreport to the given editor.
 */
public static void addSubreport(RMEditor anEditor)
{
    // Create image shape
    RMSubreport subreport = new RMSubreport();
    
    // Get parent and move shape to center
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    subreport.setFrame((int)(parent.getWidth()/2 - 200), (int)(parent.getHeight()/2 - 60), 400, 120);

    // Add shape to parent
    anEditor.undoerSetUndoTitle("Add Subreport");
    parent.addChild(subreport);

    // Select shape, set selectTool and repaint
    anEditor.setSelectedShape(subreport);
    anEditor.setCurrentToolToSelectTool();
    anEditor.repaint();
}

/**
 * Adds a widget to the given editor.
 */
public static void addWidget(RMEditor anEditor)
{
    // Create image shape
    ViewShape viewShape = new ViewShape();
    
    // Get parent and move shape to center
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    viewShape.setXY((int)(parent.getWidth()/2 - viewShape.getWidth()/2),
        (int)(parent.getHeight()/2 - viewShape.getHeight()/2));

    // Add shape to parent
    anEditor.undoerSetUndoTitle("Add Widget");
    parent.addChild(viewShape);

    // Select shape, set selectTool and repaint
    anEditor.setSelectedShape(viewShape);
    anEditor.setCurrentToolToSelectTool();
    anEditor.repaint();
}

/**
 * Runs the dataset key panel to add a table, graph, crosstab or labels to given editor.
 */
public static void runDatasetKeyPanel(RMEditor anEditor, String aKeyPath)
{
    // Hide AttributesPanel Drawer
    RMEditorPane editorPane = anEditor.getEditorPane();
    editorPane.hideAttributesDrawer();
    
    // Run dataset key panel to get dataset element type
    int type = new DatasetKeyPanel().showDatasetKeyPanel(anEditor);
    
    // Add appropriate dataset key element for returned type
    switch(type) {
        case DatasetKeyPanel.TABLE: RMTableTool.addTable(anEditor, aKeyPath); break;
        case DatasetKeyPanel.GRAPH: RMGraphTool.addGraph(anEditor, aKeyPath); break;
        case DatasetKeyPanel.LABELS: RMLabelsTool.addLabels(anEditor, aKeyPath); break;
        case DatasetKeyPanel.CROSSTAB: RMCrossTabTool.addCrossTab(anEditor, aKeyPath); break;
    }
    
    // If EditorPane.Inspector showing DataSource inspector, reset os ShapeSpecific
    if(editorPane.getInspectorPanel().isShowingDataSource())
        editorPane.getInspectorPanel().setVisible(0);
}

}