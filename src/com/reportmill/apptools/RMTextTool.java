/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.shape.*;
import com.reportmill.graphics.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * This class provides UI editing for text shapes.
 */
public class RMTextTool <T extends RMTextShape> extends RMTool <T> {
    
    // The TextArea
    TextArea           _textArea;
    
    // The shape hit by text tool on mouse down
    RMShape            _downShape;
    
    // Whether editor should resize RMText whenever text changes
    boolean            _updatingSize = false;
    
    // The minimum height of the RMText when editor text editor is updating size
    double             _updatingMinHeight = 0;

    // Whether current mouse drag should be moving table column
    boolean            _moveTableColumn;

    // A Listener for RichText PropChange
    PropChangeListener  _richTextLsnr = pc -> richTextDidPropChange(pc);

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get the TextView and register to update selection
    TextView textView = getView("TextView", TextView.class);
    textView.getScrollView().setBarSize(12);
    _textArea = textView.getTextArea();
    _textArea.addPropChangeListener(pc -> textAreaChangedSel(), TextArea.Selection_Prop);
}

/**
 * Refresh UI from currently selected text shape.
 */
public void resetUI()
{
    // Get editor and currently selected text
    RMEditor editor = getEditor();
    RMTextShape text = getSelectedShape(); if(text==null) return;
    
    // Get paragraph from text
    int selStart = 0; if(_textArea.isFocused()) selStart = _textArea.getSelStart();
    RMParagraph pgraph = text.getXString().getParagraphAt(selStart);
    
    // If editor is text editing, get paragraph from text editor instead
    RMTextEditor ted = editor.getTextEditor();
    if(ted!=null)
        pgraph = ted.getInputParagraph();
    
    // Update AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton
    setViewValue("AlignLeftButton", pgraph.getAlignmentX()==RMTypes.AlignX.Left);
    setViewValue("AlignCenterButton", pgraph.getAlignmentX()==RMTypes.AlignX.Center);
    setViewValue("AlignRightButton", pgraph.getAlignmentX()==RMTypes.AlignX.Right);
    setViewValue("AlignFullButton", pgraph.getAlignmentX()==RMTypes.AlignX.Full);
    
    // Update AlignTopButton, AlignMiddleButton, AlignBottomButton
    setViewValue("AlignTopButton", text.getAlignmentY()==RMTypes.AlignY.Top);
    setViewValue("AlignMiddleButton", text.getAlignmentY()==RMTypes.AlignY.Middle);
    setViewValue("AlignBottomButton", text.getAlignmentY()==RMTypes.AlignY.Bottom);
    
    // Set TextView RichText and selection
    _textArea.setRichText(text.getRichText());
    if(ted!=null)
        _textArea.setSel(ted.getSelStart(), ted.getSelEnd());

    // Get text's background color and set in TextArea if found
    Color color = null; for(RMShape shape=text; color==null && shape!=null;) {
        if(shape.getFill()==null) shape = shape.getParent(); else color = shape.getFill().getColor(); }
    _textArea.setFill(color==null? Color.WHITE : color);
    
    // Get xstring font size and scale up to 12pt if any string run is smaller
    double fsize = 12;
    for(RichTextLine line : text.getRichText().getLines())
        for(RichTextRun run : line.getRuns())
            fsize = Math.min(fsize, run.getFont().getSize());
    _textArea.setFontScale(fsize<12? 12/fsize : 1);
    
    // Update MarginText, RoundingThumb, RoundingText
    setViewValue("MarginText", text.getMarginString());
    setViewValue("RoundingThumb", text.getRadius());
    setViewValue("RoundingText", text.getRadius());

    // Update ShowBorderCheckBox, CoalesceNewlinesCheckBox, PerformWrapCheckBox
    setViewValue("ShowBorderCheckBox", text.getDrawsSelectionRect());
    setViewValue("CoalesceNewlinesCheckBox", text.getCoalesceNewlines());
    setViewValue("PerformWrapCheckBox", text.getPerformsWrap());

    // Update PaginateRadio, ShrinkRadio, GrowRadio
    setViewValue("PaginateRadio", text.getWraps()==RMTextShape.WRAP_BASIC);
    setViewValue("ShrinkRadio", text.getWraps()==RMTextShape.WRAP_SCALE);
    setViewValue("GrowRadio", text.getWraps()==RMTextShape.WRAP_NONE);
    
    // Update CharSpacingSpinner, LineSpacingSpinner, LineGapSpinner
    setViewValue("CharSpacingSpinner", text.getCharSpacing());
    setViewValue("LineSpacingSpinner", text.getLineSpacing());
    setViewValue("LineGapSpinner", text.getLineGap());
    
    // If line height min not set (0), update LineHeightMinSpinner with current font size
    // If valid line height min, update LineHeightMinSpinner with line height
    //double lineHtMin = text.getLineHeightMin();
    //boolean lineHtMinSet = lineHtMin!=0; if(!lineHtMinSet) lineHtMin = RMEditorUtils.getFont(editor).getSize();
    //setViewValue("LineHeightMinSpinner", lineHtMin);
    // If line height max not set, update LineHeightMaxSpinner with current font size
    // If line height max is set, update LineHeightMaxSpinner with line height max
    //double lineHtMax = text.getLineHeightMax();
    //boolean lineHtMaxSet = lineHtMax>999; if(!lineHtMaxSet) lineHtMax = RMEditorUtils.getFont(editor).getSize();
    //setViewValue("LineHeightMaxSpinner", lineHtMax);
    
    // Update PDF options: EditableCheckBox, MultilineCheckBox
    setViewValue("EditableCheckBox", text.isEditable());
    setViewValue("MultilineCheckBox", text.isEditable() && text.isMultiline());
    setViewEnabled("MultilineCheckBox", text.isEditable());
}

/**
 * Handles changes from UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get editor, currently selected text shape and text shapes (just return if null)
    RMEditor editor = getEditor();
    RMTextShape text = getSelectedShape(); if(text==null) return;
    List <RMTextShape> texts = (List)getSelectedShapes();
    
    // Register repaint for texts
    for(RMShape txt : texts) txt.repaint(); //texts.forEach(i -> i.repaint());
    
    // Handle AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton, AlignTopButton, AlignMiddleButton
    if(anEvent.equals("AlignLeftButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Left);
    if(anEvent.equals("AlignCenterButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Center);
    if(anEvent.equals("AlignRightButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Right);
    if(anEvent.equals("AlignFullButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Full);
    if(anEvent.equals("AlignTopButton")) for(RMTextShape txt : texts) txt.setAlignmentY(RMTypes.AlignY.Top);
    if(anEvent.equals("AlignMiddleButton")) for(RMTextShape txt : texts) txt.setAlignmentY(RMTypes.AlignY.Middle);
    if(anEvent.equals("AlignBottomButton")) for(RMTextShape txt : texts) txt.setAlignmentY(RMTypes.AlignY.Bottom);
    
    // If RoundingThumb or RoundingText, make sure shapes have stroke
    if(anEvent.equals("RoundingThumb") || anEvent.equals("RoundingText"))
        for(RMTextShape t : texts) t.setStroke(new RMStroke());

    // Handle MarginText, RoundingThumb, RoundingText
    if(anEvent.equals("MarginText")) for(RMTextShape txt : texts) txt.setMarginString(anEvent.getStringValue());
    if(anEvent.equals("RoundingThumb")) for(RMTextShape txt : texts) txt.setRadius(anEvent.getFloatValue());
    if(anEvent.equals("RoundingText")) for(RMTextShape txt : texts) txt.setRadius(anEvent.getFloatValue());

    // Handle ShowBorderCheckBox, CoalesceNewlinesCheckBox, PerformWrapCheckBox
    if(anEvent.equals("ShowBorderCheckBox"))
        for(RMTextShape txt : texts) txt.setDrawsSelectionRect(anEvent.getBoolValue());
    if(anEvent.equals("CoalesceNewlinesCheckBox"))
        for(RMTextShape txt : texts) txt.setCoalesceNewlines(anEvent.getBoolValue());
    if(anEvent.equals("PerformWrapCheckBox"))
        for(RMTextShape txt : texts) txt.setPerformsWrap(anEvent.getBoolValue());

    // Handle PaginateRadio, ShrinkRadio, GrowRadio
    if(anEvent.equals("PaginateRadio")) for(RMTextShape txt : texts) txt.setWraps(RMTextShape.WRAP_BASIC);
    if(anEvent.equals("ShrinkRadio")) for(RMTextShape txt : texts) txt.setWraps(RMTextShape.WRAP_SCALE);
    if(anEvent.equals("GrowRadio")) for(RMTextShape txt : texts) txt.setWraps(RMTextShape.WRAP_NONE);
    
    // Handle CharSpacingSpinner, LineSpacingSpinner, LineSpacingSingleButton, LineSpacingDoubleButton, LineGapSpinner
    if(anEvent.equals("CharSpacingSpinner")) RMTextTool.setCharSpacing(editor, anEvent.getFloatValue());
    if(anEvent.equals("LineSpacingSpinner")) RMTextTool.setLineSpacing(editor, anEvent.getFloatValue());
    if(anEvent.equals("LineSpacingSingleButton")) RMTextTool.setLineSpacing(editor, 1);
    if(anEvent.equals("LineSpacingDoubleButton")) RMTextTool.setLineSpacing(editor, 2);
    if(anEvent.equals("LineGapSpinner")) RMTextTool.setLineGap(editor, anEvent.getFloatValue());

    // Handle LineHeightMinSpinner, LineHeightMaxSpinner
    //if(anEvent.equals("LineHeightMinSpinner")) setLineHeightMin(editor, Math.max(anEvent.getFloatValue(), 0));
    //if(anEvent.equals("LineHeightMaxSpinner")) {
    //    float val = anEvent.getFloatValue(); if(val>=999) val = Float.MAX_VALUE; setLineHeightMax(editor, val); }
    
    // Handle MakeMinWidthMenuItem, MakeMinHeightMenuItem
    if(anEvent.equals("MakeMinWidthMenuItem")) for(RMTextShape txt : texts) txt.setWidth(txt.getBestWidth());
    if(anEvent.equals("MakeMinHeightMenuItem")) for(RMTextShape txt : texts) txt.setHeight(txt.getBestHeight());
    
    // Handle TurnToPathMenuItem
    if(anEvent.equals("TurnToPathMenuItem"))
        for(int i=0; i<texts.size(); i++) {
            RMTextShape text1 = texts.get(i);
            RMShape textPathShape = RMTextShapeUtils.getTextPathShape(text1);
            RMParentShape parent = text1.getParent();
            parent.addChild(textPathShape, text1.indexOf());
            parent.removeChild(text1);
            editor.setSelectedShape(textPathShape);
        }
    
    // Handle TurnToCharsShapeMenuItem
    if(anEvent.equals("TurnToCharsShapeMenuItem"))
        for(int i=0; i<texts.size(); i++) {
            RMTextShape text1 = texts.get(i);
            RMShape textCharsShape = RMTextShapeUtils.getTextCharsShape(text1);
            RMParentShape parent = text1.getParent();
            parent.addChild(textCharsShape, text1.indexOf());
            parent.removeChild(text1);
            editor.setSelectedShape(textCharsShape);
        }
    
    // Handle LinkedTextMenuItem
    if(anEvent.equals("LinkedTextMenuItem")) {
        
        // Get linked text identical to original text and add to text's parent
        RMLinkedText linkedText = new RMLinkedText(text);
        text.getParent().addChild(linkedText);
        
        // Shift linked text down if there's room, otherwise right, otherwise just offset by quarter inch
        if(text.getFrameMaxY() + 18 + text.getFrame().height*.75 < text.getParent().getHeight())
            linkedText.offsetXY(0, text.getHeight() + 18);
        else if(text.getFrameMaxX() + 18 + text.getFrame().width*.75 < text.getParent().getWidth())
            linkedText.offsetXY(text.getWidth() + 18, 0);
        else linkedText.offsetXY(18, 18);
        
        // Select and repaint new linked text
        editor.setSelectedShape(linkedText);
        linkedText.repaint();
    }
    
    // Update PDF options: EditableCheckBox, MultilineCheckBox
    if(anEvent.equals("EditableCheckBox")) text.setEditable(anEvent.getBoolValue());
    if(anEvent.equals("MultilineCheckBox")) text.setMultiline(anEvent.getBoolValue());
}

/**
 * Called when TextArea changes selection.
 */
private void textAreaChangedSel()
{
    // If in resetUI, just return
    if(isSendEventDisabled()) return;
    
    // Get text, repaint and make sure it's super-selected
    RMEditor editor = getEditor();
    RMTextShape textShape = getSelectedShape(); if(textShape==null) return;
    textShape.repaint();
    if(textShape!=editor.getSuperSelectedShape())
        editor.setSuperSelectedShape(textShape);
    
    // Get TextEditor and update sel from TextArea
    RMTextEditor textEd = editor.getTextEditor();
    if(textEd!=null)
        textEd.setSel(_textArea.getSelStart(), _textArea.getSelEnd());
        
    // ResetUI on MouseUp
    ViewUtils.runOnMouseUp(() -> getEditorPane().resetLater());
}

/**
 * Overrides standard tool method to deselect any currently editing text.
 */
public void activateTool()
{
    if(getEditor().getSuperSelectedShape() instanceof RMTextShape)
        getEditor().setSuperSelectedShape(getEditor().getSuperSelectedShape().getParent());
}

/**
 * Event handling - overridden to install text cursor.
 */
public void mouseMoved(ViewEvent anEvent)  { getEditor().setCursor(Cursor.TEXT); }

/**
 * Event handling - overridden to install text cursor.
 */
public void mouseMoved(T aShape, ViewEvent anEvent)
{
    if(getEditor().getShapeAtPoint(anEvent.getPoint()) instanceof RMTextShape) {
        getEditor().setCursor(Cursor.TEXT); anEvent.consume(); }
}

/**
 * Handles mouse pressed for text tool. Special support to super select any text hit by tool mouse pressed.
 */
public void mousePressed(ViewEvent anEvent)
{
    // Register all selectedShapes dirty because their handles will probably need to be wiped out
    for(RMShape shp : getEditor().getSelectedShapes()) shp.repaint();

    // Get shape hit by down point
    _downShape = getEditor().getShapeAtPoint(anEvent.getX(),anEvent.getY());
    
    // Get _downPoint from editor
    _downPoint = getEditorEvents().getEventPointInShape(true);
    
    // Create default text instance and set initial bounds to reasonable value
    RMTextShape tshape = new RMTextShape(); _shape = tshape;
    Rect defaultBounds = getDefaultBounds(tshape, _downPoint);
    _shape.setFrame(defaultBounds);
    
    // Add shape to superSelectedShape (within an undo grouping) and superSelect
    getEditor().undoerSetUndoTitle("Add Text");
    getEditor().getSuperSelectedParentShape().addChild(_shape);
    getEditor().setSuperSelectedShape(_shape);
    _updatingSize = true;
}

/**
 * Handles mouse dragged for tool. If user doesn't really drag, then default text box should align the base line
 * of the text about the pressed point. If they do really drag, then text box should be the rect they drag out.
 */
public void mouseDragged(ViewEvent anEvent)
{
    // If shape wasn't created in mouse down, just return
    if(_shape==null) return;
    
    // Set shape to repaint
    _shape.repaint();
    
    // Get event point in shape parent coords
    Point point = getEditorEvents().getEventPointInShape(true);
    point = _shape.localToParent(point);
    
    // Get text default bounds and effective down point
    RMTextShape tshape = (RMTextShape)_shape;
    Rect defaultBounds = getDefaultBounds(tshape, _downPoint);
    Point downPoint = defaultBounds.getPoint(Pos.TOP_LEFT);

    // Get new bounds rect from default bounds and drag point (make sure min height is default height)
    Rect rect = Rect.get(downPoint, point);
    rect.width = Math.max(rect.width, defaultBounds.width);
    rect.height = Math.max(rect.height, defaultBounds.height);
    
    // Set UpdatingMinHeight to drag rect height, but if text rect unreasonably thin, reset to 0
    _updatingMinHeight = rect.height;
    if(rect.width<=30)
        _updatingMinHeight = 0;
    
    // Set new shape bounds
    _shape.setFrame(rect);
}

/**
 * Event handling for text tool mouse loop.
 */
public void mouseReleased(ViewEvent e)
{
    // Get event point in shape parent coords
    Point upPoint = getEditorEvents().getEventPointInShape(true);
    upPoint = _shape.localToParent(upPoint);
    
    // If upRect is really small, see if the user meant to convert a shape to text instead
    if(Math.abs(_downPoint.x - upPoint.x)<=3 && Math.abs(_downPoint.y - upPoint.y)<=3) {
        
        // If hit shape is text, just super-select that text and return
        if(_downShape instanceof RMTextShape) {
            _shape.removeFromParent();
            getEditor().setSuperSelectedShape(_downShape);
        }
        
        // If hit shape is Rectangle, Oval or Polygon, swap for RMText and return
        else if(shouldConvertToText(_downShape)) {
            _shape.removeFromParent();
            convertToText(_downShape, null);
        }
    }
    
    // Set editor current tool to select tool
    getEditor().setCurrentToolToSelectTool();
    
    // Reset tool shape
    _shape = null;
}

/**
 * Event handling for shape editing (just forwards to text editor).
 */
public void processEvent(T aTextShape, ViewEvent anEvent)
{
    // Handle KeyEvent
    if(anEvent.isKeyEvent()) {
        processKeyEvent(aTextShape, anEvent); return; }
        
    // If MoveTableColumn, forward to moveTableColumn()
    if(_moveTableColumn)
        moveTableColumn(anEvent);
        
    // If text is a structured table row column and point is outside column, start MoveTableRow
    else if(anEvent.isMouseDrag()) { RMTextShape tshp = aTextShape;
        Point pnt = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aTextShape); double px = pnt.getX();
        if(tshp.isStructured() && (px<-20 || px>tshp.getWidth()+10) && tshp.getParent().getChildCount()>1) {
            tshp.undoerSetUndoTitle("Reorder columns");
            getEditor().setSelectedShape(tshp); _moveTableColumn = true; return; }
    }
        
    // If shape isn't super selected, just return
    if(!isSuperSelected(aTextShape)) return;
    
    // If mouse event, convert event to text shape coords and consume
    if(anEvent.isMouseEvent()) { anEvent.consume();
        Point pnt = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aTextShape);
        anEvent = anEvent.copyForPoint(pnt.getX(), pnt.getY());
    }
        
    // Forward on to editor
    aTextShape.getTextEditor().processEvent(anEvent);
    aTextShape.repaint();
}

/**
 * Key event handling for super selected text.
 */
public void processKeyEvent(T aTextShape, ViewEvent anEvent)
{
    // If tab was pressed and text is structured table row column, forward selection onto next column
    if(aTextShape.isStructured() && anEvent.isKeyPress() &&
        anEvent.getKeyCode()==KeyCode.TAB && !anEvent.isAltDown()) {
        
        // Get structured text table row, child table rows and index of child
        RMParentShape tableRow = aTextShape.getParent();
        List children = RMShapeUtils.getShapesSortedByX(tableRow.getChildren());
        int index = children.indexOf(aTextShape);
        
        // If shift is down, get index to the left, wrapped, otherwise get index to the right, wrapped
        if(anEvent.isShiftDown()) index = (index - 1 + children.size())%children.size();
        else index = (index + 1)%children.size();
        
        // Get next text and super-select
        RMShape nextText = (RMShape)children.get(index);
        getEditor().setSuperSelectedShape(nextText);
        
        // Consume event and return
        anEvent.consume(); return;
    }

    // Have text editor process key event
    aTextShape.getTextEditor().processEvent(anEvent);
    aTextShape.repaint();
}

/**
 * Move Table Column stuff (table row column re-ordering).
 */
private void moveTableColumn(ViewEvent anEvent)
{
    // Get editor, editor SelectedShape and TableRow
    RMEditor editor = getEditor();
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    RMTableRow tableRow = (RMTableRow)shape.getParent(); tableRow.repaint();
    
    // Get event x in TableRow coords and whether point is in TableRow
    Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), tableRow); point.y = 2;
    boolean inRow = tableRow.contains(point);
    
    // Handle MouseDragged: layout children by X (if outside row, skip drag shape)
    if(anEvent.isMouseDrag()) {
        List <RMShape> children = RMShapeUtils.getShapesSortedByFrameX(tableRow.getChildren()); float x = 0;
        for(RMShape child : children) {
            if(child==shape) { if(inRow) child.setX(point.x-child.getWidth()/2); else { child.setX(9999); continue; }}
            else child.setX(x); x += child.getWidth(); }
    }
    
    // Handle MouseReleased: reset children
    if(anEvent.isMouseRelease()) {

        // If shape in row, set new index
        if(inRow) {
            int iold = shape.indexOf();
            int inew = 0; while(inew<tableRow.getChildCount() && tableRow.getChild(inew).getX()<=shape.getX()) inew++;
            if(iold!=inew) {
                tableRow.removeChild(iold); if(inew>iold) inew--;
                tableRow.addChild(shape, inew);
            }
        }
        
        // If shape is outside bounds of tableRow, remove it
        else {
            tableRow.removeChild(shape);
            editor.setSuperSelectedShape(tableRow);
        }

        // Do layout again to snap shape back into place
        tableRow.layoutDeep(); _moveTableColumn = false;
    }
}

/**
 * Editor method - installs this text in RMEditor's text editor.
 */
public void didBecomeSuperSelected(T aTextShape)
{
    // Start listening to changes to TextShape RichText
    aTextShape.getRichText().addPropChangeListener(_richTextLsnr);
}

/**
 * Editor method - uninstalls this text from RMEditor's text editor and removes new text if empty.
 */
public void willLoseSuperSelected(T aTextShape)
{
    // If text editor was really just an insertion point and ending text length is zero, remove text
    if(_updatingSize && aTextShape.length()==0 &&
        getEditor().getSelectTool().getDragMode()==RMSelectTool.DragMode.None)
        aTextShape.removeFromParent();
    
    // Stop listening to changes to TextShape RichText
    aTextShape.getRichText().removePropChangeListener(_richTextLsnr);
    _updatingSize = false; _updatingMinHeight = 0;
    
    // Set text editor's text shape to null
    aTextShape.clearTextEditor();
}

/**
 * Handle changes to Selected TextShape.RichText
 */
protected void richTextDidPropChange(PropChange aPC)
{
    // If updating size, reset text width & height to accommodate text
    if(_updatingSize) {
        
        // Get TextShape
        RMTextShape textShape = getSelectedShape(); if(textShape==null) return;
    
        // Get preferred text shape width
        double maxWidth = _updatingMinHeight==0? textShape.getParent().getWidth() - textShape.getX() :
            textShape.getWidth();
        double prefWidth = textShape.getPrefWidth(); if(prefWidth>maxWidth) prefWidth = maxWidth;

        // If width gets updated, get & set desired width (make sure it doesn't go beyond page border)
        if(_updatingMinHeight==0)
            textShape.setWidth(prefWidth);

        // If PrefHeight or current height is greater than UpdatingMinHeight (which won't be zero if user drew a
        //  text box to enter text), set Height to PrefHeight
        double prefHeight = textShape.getPrefHeight();
        if(prefHeight>_updatingMinHeight || textShape.getHeight()>_updatingMinHeight)
            textShape.setHeight(Math.max(prefHeight, _updatingMinHeight));
    }
}

/**
 * Event hook during selection.
 */
public boolean mousePressedSelection(ViewEvent anEvent)
{
    // Iterator over selected shapes and see if any has an overflow indicator box that was hit
    List shapes = getEditor().getSelectedOrSuperSelectedShapes();
    for(int i=0, iMax=shapes.size(); i<iMax; i++) { RMTextShape text = (RMTextShape)shapes.get(i);
        
        // If no linked text and not painting text indicator, just continue
        if(text.getLinkedText()==null && !isPaintingTextLinkIndicator(text)) continue;

        // Get point in text coords
        Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), text);
        
        // If pressed was in overflow indicator box, add linked text (or select existing one)
        if(point.x>=text.getWidth()-20 && point.x<=text.getWidth()-10 && point.y>=text.getHeight()-5) {
            if(text.getLinkedText()==null) sendEvent("LinkedTextMenuItem");   // If not linked text, add it
            else getEditor().setSelectedShape(text.getLinkedText());          // Otherwise, select it
            return true;    // Return true so SelectTool goes to DragModeNone
        }
    }
    
    // Return false is mouse point wasn't in overflow indicator box
    return false;
}

/**
 * Moves the handle at the given index to the given point.
 */
public void moveShapeHandle(T aShape, int aHandle, Point toPoint)
{
    // If not structured, do normal version
    if(!aShape.isStructured()) { super.moveShapeHandle(aShape, aHandle, toPoint); return; }
    
    // Get handle point in shape coords and shape parent coords
    Point p1 = getHandlePoint(aShape, aHandle, false);
    Point p2 = aShape.parentToLocal(toPoint);
    
    // Get whether left handle and width change
    boolean left = aHandle==HandleW || aHandle==HandleNW || aHandle==HandleSW;
    double dw = p2.getX() - p1.getX(); if(left) dw = -dw;
    double nw = aShape.getWidth() + dw;
    if(nw<8) { nw = 8; dw = nw - aShape.getWidth(); }
    
    // Get shape to adjust and new width (make sure it's no less than 8)
    int index = aShape.indexOf();
    int index2 = left? index-1 : index+1;
    RMShape other = aShape.getParent().getChild(index2);
    double nw2 = other.getWidth() - dw;
    if(nw2<8) { nw2 = 8; dw = other.getWidth() - nw2; nw = aShape.getWidth() + dw; } 
    
    // Adjust shape and layout parent
    aShape.setWidth(nw);
    other.setWidth(nw2);
    aShape.getParent().layout();
}

/**
 * Overrides tool tooltip method to return text string if some chars aren't visible.
 */
public String getToolTip(T aTextShape, ViewEvent anEvent)
{
    // If all text is visible and greater than 8 pt, return null
    if(aTextShape.isAllTextVisible() && aTextShape.getFont().getSize()>=8) return null;
    
    // Get text string (just return if empty), trim to 64 chars or less and return
    String string = aTextShape.getText(); if(string==null || string.length()==0) return null;
    if(string.length()>64) string = string.substring(0,64) + "...";
    return string;
}

/**
 * Paints selected shape indicator, like handles (and maybe a text linking indicator).
 */
public void paintHandles(T aText, Painter aPntr, boolean isSuperSelected)
{
    // Paint bounds rect (maybe)
    paintBoundsRect(aText, aPntr);

    // If text is structured, draw rectangle buttons
    if(aText.isStructured()) {
        
        // Iterate over shape handles, get rect and draw
        aPntr.setAntialiasing(false);
        for(int i=0, iMax=getHandleCount(aText); i<iMax; i++) {
            Rect hr = getHandleRect(aText, i, isSuperSelected);
            aPntr.drawButton(hr, false); }
        aPntr.setAntialiasing(true);
    }

    // If not structured or text linking, draw normal
    else if(!isSuperSelected)
        super.paintHandles(aText, aPntr, isSuperSelected);
    
    // Call paintTextLinkIndicator
    if(isPaintingTextLinkIndicator(aText))
        paintTextLinkIndicator(aText, aPntr);
}

/**
 * Paint bounds rect (maybe): Set color (red if selected, light gray otherwise), get bounds path and draw.
 */
public void paintBoundsRect(RMTextShape aText, Painter aPntr)
{
    if(!isShowBoundsRect(aText)) return;
    aPntr.save();
    aPntr.setColor(getEditor().isSuperSelected(aText)? new Color(.9f, .4f, .4f) : Color.LIGHTGRAY);
    aPntr.setStroke(Stroke.Stroke1.copyForDashes(3, 2));
    Shape path = aText.getPath().copyFor(aText.getBoundsInside());
    path = getEditor().convertFromShape(path, aText);
    aPntr.setAntialiasing(false); aPntr.draw(path); aPntr.setAntialiasing(true);
    aPntr.restore();
}

/**
 * Returns whether to show bounds rect.
 */
private boolean isShowBoundsRect(RMTextShape aText)
{
    RMEditor editor = getEditor();
    if(aText.getStroke()!=null) return false; // If text draws it's own stroke, return false
    if(!editor.isEditing()) return false; // If editor is previewing, return false
    if(aText.isStructured()) return false; // If structured text, return false
    if(editor.isSelected(aText) || editor.isSuperSelected(aText)) return true; // If selected, return true
    if(aText.length()==0) return true; // If text is zero length, return true
    if(aText.getDrawsSelectionRect()) return true; // If text explicitly draws selection rect, return true
    return false; // Otherwise, return false
}

/**
 * Returns whether to paint text link indicator.
 */
public boolean isPaintingTextLinkIndicator(RMTextShape aText)
{
    // If text is child of table row, return false
    if(aText.getParent() instanceof RMTableRow) return false;
    
    // If there is a linked text, return true
    if(aText.getLinkedText()!=null) return true;
    
    // If height is less than half-inch, return false
    if(aText.getHeight()<36) return false;
    
    // If all text visible, return false
    if(aText.isAllTextVisible()) return false;
    
    // Return true
    return true;
}

/**
 * Paints the text link indicator.
 */
public void paintTextLinkIndicator(RMTextShape aText, Painter aPntr)
{
    // Turn off anti-aliasing
    aPntr.setAntialiasing(false);

    // Get overflow indicator box center point in editor coords
    Point point = getEditor().convertFromShape(aText.getWidth()-15, aText.getHeight(), aText);
    
    // Get overflow indicator box rect in editor coords
    Rect rect = new Rect(point.x - 5, point.y - 5, 10, 10);
        
    // Draw white background, black frame, and plus sign and turn off aliasing
    aPntr.setColor(aText.getLinkedText()==null? Color.WHITE : new Color(90, 200, 255)); aPntr.fill(rect);
    aPntr.setColor(aText.getLinkedText()==null? Color.BLACK : Color.GRAY);
    aPntr.setStroke(Stroke.Stroke1); aPntr.draw(rect);
    aPntr.setColor(aText.getLinkedText()==null? Color.BLACK : Color.WHITE);
    aPntr.setStroke(new Stroke(1)); //, BasicStroke.CAP_BUTT, 0));
    aPntr.drawLine(rect.getMidX(), rect.y + 2, rect.getMidX(), rect.getMaxY() - 2);
    aPntr.drawLine(rect.x + 2, rect.getMidY(), rect.getMaxX() - 2, rect.getMidY());

    // Turn on antialiasing
    aPntr.setAntialiasing(true);
}
    
/**
 * Editor method - returns handle count.
 */
public int getHandleCount(T aText)  { return aText.isStructured()? 2 : super.getHandleCount(aText); }

/**
 * Editor method - returns handle rect in editor coords.
 */
public Rect getHandleRect(T aTextShape, int handle, boolean isSuperSelected)
{
    // If structured, return special handles (tall & thin)
    if(aTextShape.isStructured()) {
        
        // Get handle point in text bounds, convert to table row bounds
        Point cp = getHandlePoint(aTextShape, handle, true);
        cp = aTextShape.localToParent(cp);
        
        // If point outside of parent, return bogus rect
        if(cp.getX()<0 || cp.getX()>aTextShape.getParent().getWidth())
           return new Rect(-9999,-9999,0,0);

        // Get handle point in text coords
        cp = getHandlePoint(aTextShape, handle, false);
        
        // Get handle point in editor coords
        cp = getEditor().convertFromShape(cp.getX(), cp.getY(), aTextShape);
        
        // Get handle rect
        Rect hr = new Rect(cp.getX()-3, cp.getY(), 6, aTextShape.height() * getEditor().getZoomFactor());
        
        // If super selected, offset
        if(isSuperSelected)
            hr.offset(handle==0? -2 : 2, 0);
        
        // Return handle rect
        return hr;
    }
    
    // Return normal shape handle rect
    return super.getHandleRect(aTextShape, handle, isSuperSelected);
}

/**
 * Overrides Tool implementation to accept KeysPanel drags.
 */
public boolean acceptsDrag(T aShape, ViewEvent anEvent)
{
    // If KeysPanel is dragging, return true
    if(KeysPanel.getDragKey()!=null)
        return true;
    
    // Otherwise, return normal
    return super.acceptsDrag(aShape, anEvent);
}

/**
 * Override normal implementation to handle KeysPanel drop.
 */
public void drop(T aShape, ViewEvent anEvent)
{
    // If a keys panel drop, add key to text
    if(KeysPanel.getDragKey()!=null) {
        String string = anEvent.getClipboard().getString();
        RMTextShape text = aShape;
        if(text.length()==0)
            text.setText(string);
        else text.getXString().addChars(" " + string);
    }
    
    // Otherwise, do normal drop
    else super.drop(aShape, anEvent);
}

/**
 * Returns the shape class that this tool edits.
 */
public Class getShapeClass()  { return RMTextShape.class; }

/**
 * Returns the name of this tool to be displayed by inspector.
 */
public String getWindowTitle()  { return "Text Inspector"; }

/**
 * Returns whether text tool should convert to text.
 */
public boolean shouldConvertToText(RMShape aShape)
{
    if(aShape instanceof RMImageShape) return false;
    if(aShape instanceof RMPDFShape) return false;
    if(aShape.isLocked()) return false;
    return aShape instanceof RMRectShape || aShape instanceof RMOvalShape ||
        aShape instanceof RMPolygonShape;
}

/**
 * Converts a shape to a text shape.
 */
public void convertToText(RMShape aShape, String aString)
{
    // If shape is null, just return
    if(aShape==null) return;
    
    // Get text shape for given shape (if given shape is text, just use it)
    RMTextShape text = aShape instanceof RMTextShape? (RMTextShape)aShape : new RMTextShape();
    
    // Copy attributes of given shape
    if(text!=aShape)
        text.copyShape(aShape);
    
    // Copy path of given shape
    if(text!=aShape)
        text.setPathShape(aShape);
    
    // Swap this shape in for original
    if(text!=aShape) {
        aShape.getParent().addChild(text, aShape.indexOf());
        aShape.getParent().removeChild(aShape);
    }
    
    // Install a bogus string for testing
    if(aString!=null && aString.equals("test"))
        aString = getTestString();
    
    // If aString is non-null, install in text
    if(aString!=null)
        text.setText(aString);
    
    // Select new shape
    getEditor().setSuperSelectedShape(text);
}

/**
 * Returns a rect suitable for the default bounds of a given text at a given point. This takes into account the font
 * and margins of the given text.
 */
private static Rect getDefaultBounds(RMTextShape aText, Point aPoint)
{
    // Get text font (or default font, if not available) and margin
    RMFont font = aText.getFont(); if(font==null) font = RMFont.getDefaultFont();
    Insets margin = aText.getMargin();
    
    // Default width is a standard char plus margin, default height is font line height plus margin
    double w = Math.round(font.charAdvance('x') + margin.getWidth());
    double h = Math.round(font.getLineHeight() + margin.getHeight());
    
    // Get bounds x/y from given (cursor) point and size
    double x = Math.round(aPoint.x - w/2) + 1;
    double y = Math.round(aPoint.y - h/2) - 1;
    
    // Return integral bounds rect
    Rect rect = new Rect(x, y, w, h); rect.snap();
    return rect;
}

/**
 * Returns a test string.
 */
private static String getTestString()
{
    return "Leo vitae diam est luctus, ornare massa mauris urna, vitae sodales et ut facilisis dignissim, " +
    "imperdiet in diam, quis que ad ipiscing nec posuere feugiat ante velit. Viva mus leo quisque. Neque mi vitae, " +
    "nulla cras diam fusce lacus, nibh pellentesque libero. " +
    "Dolor at venenatis in, ac in quam purus diam mauris massa, dolor leo vehicula at commodo. Turpis condimentum " +
    "varius aliquet accumsan, sit nullam eget in turpis augue, vel tristique, fusce metus id consequat orci " +
    "penatibus. Ipsum vehicula euismod aliquet, pharetra. " +
    "Fusce lectus proin, neque cr as eget, integer quam facilisi a adipiscing posuere. Imper diet sem sapien. " +
    "Pretium natoque nibh, tristique odio eligendi odio molestie mas sa. Volutpat justo fringilla rut rum augue. " +
    "Lao reet ulla mcorper molestie.";
}

/** Sets the character spacing for the currently selected shapes. */
private static void setCharSpacing(RMEditor anEditor, float aValue)
{
    anEditor.undoerSetUndoTitle("Char Spacing Change");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        if(shape instanceof RMTextShape)
            ((RMTextShape)shape).setCharSpacing(aValue);
}

/** Sets the line spacing for all chars (or all selected chars, if editing). */
private static void setLineSpacing(RMEditor anEditor, float aHeight)
{
    anEditor.undoerSetUndoTitle("Line Spacing Change");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        if(shape instanceof RMTextShape)
            ((RMTextShape)shape).setLineSpacing(aHeight);
}

/** Sets the line gap for all chars (or all selected chars, if editing). */
private static void setLineGap(RMEditor anEditor, float aHeight)
{
    anEditor.undoerSetUndoTitle("Line Gap Change");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        if(shape instanceof RMTextShape)
            ((RMTextShape)shape).setLineGap(aHeight);
}

/** Sets the minimum line height for all chars (or all selected chars, if editing). */
private static void setLineHeightMin(RMEditor anEditor, float aHeight)
{
    anEditor.undoerSetUndoTitle("Min Line Height Change");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        if(shape instanceof RMTextShape)
            ((RMTextShape)shape).setLineHeightMin(aHeight);
}

/** Sets the maximum line height for all chars (or all selected chars, if eiditing). */
private static void setLineHeightMax(RMEditor anEditor, float aHeight)
{
    anEditor.undoerSetUndoTitle("Max Line Height Change");
    for(RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
        if(shape instanceof RMTextShape)
            ((RMTextShape)shape).setLineHeightMax(aHeight);
}

}