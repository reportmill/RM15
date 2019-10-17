/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.base.RMDataSource;
import com.reportmill.graphics.RMTextEditor;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * This class subclasses RMViewer to support RMDocument editing.
 */
public class RMEditor extends RMViewer implements DeepChangeListener {

    // Whether we're really editing
    boolean             _editing = true;
    
    // List of currently selected shapes
    List <RMShape>      _selShapes = new ArrayList();
    
    // List of super selected shapes (all ancestors of selected shapes)
    List <RMShape>      _superSelShapes = new ArrayList();
    
    // The last shape that was copied to the clipboard (used for smart paste)
    RMShape             _lastCopyShape;
    
    // The last shape that was pasted from the clipboard (used for smart paste)
    RMShape             _lastPasteShape;
    
    // A helper class to handle drag and drop
    RMEditorDnD         _dragHelper = createDragHelper();
    
    // A shape to be drawn if set to drag-over shape during drag and drop
    Shape               _dragShape;
    
    // The select tool
    RMSelectTool        _selectTool;
    
    // Map of tool instances by shape class
    Map <Class, RMTool> _tools = new HashMap();
    
    // The current editor tool
    RMTool              _currentTool = getSelectTool();

    // The time of last drop of XML file
    long                _dropTime;

    // Icon for XML image
    static Image        _xmlImage = Image.get(RMEditor.class, "DS_XML.png");
    
    // XML Image offset for animation
    static double       _xmlDX, _xmlDY;
    
    // Constants for PropertyChanges
    public static final String CurrentTool_Prop = "CurrentTool";
    public static final String SelShapes_Prop = "SelShapes";
    public static final String SuperSelShape_Prop = "SuperSelShape";
    
/**
 * Creates a new editor.
 */
public RMEditor()
{
    // SuperSelect ViewerShape
    setSuperSelectedShape(getViewerShape());
    
    // Enable Drag events
    enableEvents(DragEvents);
    
    // Enable ToolTips so getToolTip gets called and disable FocusKeys so tab doesn't leave editor
    setToolTipEnabled(true);
    setFocusKeysEnabled(false);
}

/**
 * Returns the editor pane for this editor, if there is one.
 */
public RMEditorPane getEditorPane()  { return _ep!=null? _ep : (_ep=getOwner(RMEditorPane.class)); }
private RMEditorPane _ep;

/**
 * Returns whether viewer is really doing editing.
 */
public boolean isEditing()  { return _editing; }

/**
 * Sets whether viewer is really doing editing.
 */
public void setEditing(boolean aFlag)  { _editing = aFlag; }

/**
 * Creates the Painter.Props object to provide shape selection information.
 */
protected RMShapePaintProps createShapePaintProps()  { return new EditorShapePainterProps(); }

/**
 * Returns the text editor (or null if not editing).
 */
public RMTextEditor getTextEditor()
{
    RMShape shp = getSuperSelectedShape();
    return shp instanceof RMTextShape? ((RMTextShape)shp).getTextEditor() : null;
}

/**
 * Creates the shapes helper.
 */
protected RMEditorDnD createDragHelper()  { return new RMEditorDnD(this); }

/**
 * Returns the first selected shape.
 */
public RMShape getSelectedShape()  { return getSelectedShapeCount()==0? null : getSelectedShape(0); }

/**
 * Selects the given shape.
 */
public void setSelectedShape(RMShape aShape)  { setSelectedShapes(aShape==null? null : Arrays.asList(aShape)); }

/**
 * Returns the number of selected shapes.
 */
public int getSelectedShapeCount()  { return _selShapes.size(); }

/**
 * Returns the selected shape at the given index.
 */
public RMShape getSelectedShape(int anIndex)  { return ListUtils.get(_selShapes, anIndex); }

/**
 * Returns the selected shapes list.
 */
public List <RMShape> getSelectedShapes()  { return _selShapes; }

/**
 * Selects the shapes in the given list.
 */
public void setSelectedShapes(List <RMShape> theShapes)
{
    // If shapes already set, just return
    if(ListUtils.equalsId(theShapes, _selShapes)) return;
    
    // Request focus in case current focus view has changes
    requestFocus();
    
    // If shapes is null or empty super-select the selected page and return
    if(theShapes==null || theShapes.size()==0) {
        setSuperSelectedShape(getSelPage()); return; }
    
    // Get the first shape in given shapes list
    RMShape shape = theShapes.get(0);
    
    // If shapes contains superSelectedShapes, superSelect last and return (hidden trick for undoSelectedObjects)
    if(theShapes.size()>1 && shape==getDoc()) {
        setSuperSelectedShape(theShapes.get(theShapes.size()-1)); return; }
    
    // Get the shape's parent
    RMShape shapesParent = shape.getParent();
    
    // If shapes parent is the document, super select shape instead
    if(shapesParent==getDoc()) {
        setSuperSelectedShape(shape); return; }
    
    // Super select shapes parent
    setSuperSelectedShape(shapesParent);
    
    // Add shapes to selected list
    _selShapes.addAll(theShapes);
    
    // Fire PropertyChange
    firePropChange(SelShapes_Prop, null, theShapes);
}

/**
 * Add a shape to the selected shapes list.
 */
public void addSelectedShape(RMShape aShape)
{
    List list = new ArrayList(getSelectedShapes()); list.add(aShape);
    setSelectedShapes(list);
}

/**
 * Remove a shape from the selected shapes list.
 */
public void removeSelectedShape(RMShape aShape)
{
    List list = new ArrayList(getSelectedShapes()); list.remove(aShape);
    setSelectedShapes(list);
}

/**
 * Returns the first super-selected shape.
 */
public RMShape getSuperSelectedShape()
{
    return getSuperSelectedShapeCount()==0? null : getSuperSelectedShape(getSuperSelectedShapeCount()-1);
}

/**
 * Super select a shape.
 */
public void setSuperSelectedShape(RMShape aShape)
{
    // Request focus in case current focus view has changes
    requestFocus();
    
    // If given shape is null, reset to selected page
    RMShape shape = aShape!=null? aShape : getSelPage();
    
    // Unselect selected shapes
    _selShapes.clear();

    // Remove current super-selected shapes that aren't an ancestor of given shape    
    while(shape!=getSuperSelectedShape() && !shape.isAncestor(getSuperSelectedShape())) {
        RMShape ssShape = getSuperSelectedShape();
        getTool(ssShape).willLoseSuperSelected(ssShape);
        ListUtils.removeLast(_superSelShapes);
    }

    // Add super selected shape (recursively adds parents if missing)
    if(shape!=getSuperSelectedShape())
        addSuperSelectedShape(shape);
    
    // Fire PropertyChange and repaint
    firePropChange(SuperSelShape_Prop, null, aShape);
    repaint();
}

/**
 * Adds a super selected shape.
 */
private void addSuperSelectedShape(RMShape aShape)
{
    // If parent isn't super selected, add parent first
    if(aShape.getParent()!=null && !isSuperSelected(aShape.getParent()))
        addSuperSelectedShape(aShape.getParent());

    // Add ancestor to super selected list
    _superSelShapes.add(aShape);
    
    // Notify tool
    getTool(aShape).didBecomeSuperSelected(aShape);

    // If ancestor is page but not document's selected page, make it the selected page
    if(aShape instanceof RMPage && aShape!=getDoc().getSelPage())
        getDoc().setSelPage((RMPage)aShape);
}

/**
 * Returns the first super selected shape, if parent shape.
 */
public RMParentShape getSuperSelectedParentShape()
{
    RMShape ss = getSuperSelectedShape(); return ss instanceof RMParentShape? (RMParentShape)ss : null;
}

/**
 * Returns whether a given shape is selected in the editor.
 */
public boolean isSelected(RMShape aShape)  { return ListUtils.containsId(_selShapes, aShape); }

/**
 * Returns whether a given shape is super-selected in the editor.
 */
public boolean isSuperSelected(RMShape aShape)  { return ListUtils.containsId(_superSelShapes, aShape); }

/**
 * Returns the number of super-selected shapes.
 */
public int getSuperSelectedShapeCount()  { return _superSelShapes.size(); }

/**
 * Returns the super-selected shape at the given index.
 */
public RMShape getSuperSelectedShape(int anIndex)  { return _superSelShapes.get(anIndex); }

/**
 * Returns the super selected shape list.
 */
public List <RMShape> getSuperSelectedShapes()  { return _superSelShapes; }

/**
 * Returns the number of currently selected shapes or simply 1, if a shape is super-selected.
 */
public int getSelectedOrSuperSelectedShapeCount()  { int sc = getSelectedShapeCount(); return sc>0? sc : 1; }

/**
 * Returns the currently selected shape at the given index, or the super-selected shape.
 */
public RMShape getSelectedOrSuperSelectedShape(int anIndex)
{
    return getSelectedShapeCount()>0? getSelectedShape(anIndex) : getSuperSelectedShape();
}

/**
 * Returns the currently selected shape or, if none, the super-selected shape.
 */
public RMShape getSelectedOrSuperSelectedShape()
{
    return getSelectedShapeCount()>0? getSelectedShape() : getSuperSelectedShape();
}
    
/**
 * Returns the currently selected shapes or, if none, the super-selected shape in a list.
 */
public List <RMShape> getSelectedOrSuperSelectedShapes()
{
    return getSelectedShapeCount()>0? _selShapes : Arrays.asList(getSuperSelectedShape());
}
    
/**
 * Un-SuperSelect currently super selected shape.
 */
public void popSelection()
{
    // If there is a selected shape, just super-select parent (clear selected shapes)
    RMShape selShape = getSelectedShape();
    if(selShape!=null && selShape.getParent()!=null) {
        setSuperSelectedShape(selShape.getParent());
        return;
    }

    // Otherwise select super-selected shape (or its parent if it has childrenSuperSelectImmediately)
    if(getSuperSelectedShapeCount()>2) {
        RMShape superSelShape = getSuperSelectedShape();
        if(superSelShape instanceof RMTextShape)
            setSelectedShape(superSelShape);
        else if(superSelShape.getParent().childrenSuperSelectImmediately())
            setSuperSelectedShape(superSelShape.getParent());
        else setSelectedShape(superSelShape);
    }
    
    // Otherwise, beep
    else beep();
}

/**
 * Override to account for selected shapes potentially having different bounds.
 */
protected Rect getRepaintBoundsForShape(RMShape aShape)
{
    // Do normal version
    Rect bnds = super.getRepaintBoundsForShape(aShape);
    
    // If shape is selected, correct for handles
    if(isSelected(aShape)) bnds.inset(-4, -4);
    
    // If shape is super-selected, correct for handles
    else if(isSuperSelected(aShape)) {
        bnds = getTool(aShape).getBoundsSuperSelected(aShape); bnds.inset(-16, -16); }
    
    // Return bounds
    return bnds;
}

/**
 * This method finalizes any (potentially cached) changes in progress in the editor (like from text editing).
 */
public void flushEditingChanges()
{
    // Get super-selected shape and its tool and tell tool to flushChanges
    RMShape shape = getSuperSelectedShape();
    RMTool tool = getTool(shape);
    tool.flushChanges(this, shape);
}

/**
 * Returns first shape hit by point given in View coords.
 */
public RMShape getShapeAtPoint(double aX, double aY)  { return getShapeAtPoint(new Point(aX,aY)); }

/**
 * Returns first shape hit by point given in View coords.
 */
public RMShape getShapeAtPoint(Point aPoint)
{
    // Get superSelShape
    RMShape superSelShape = getSuperSelectedShape();
    
    // If superSelectedShape is document, start with page instead (maybe should go)
    if(superSelShape==getDoc())
        superSelShape = getSelPage();

    // Get the point in superSelectedShape's coords
    Point point = convertToShape(aPoint.x, aPoint.y, superSelShape);

    // Get child of superSelectedShape hit by point
    RMShape shapeAtPoint = getChildShapeAtPoint(superSelShape, point);
    
    // If no superSelectedShape child hit by point, find first superSelectedShape that is hit & set to shapeAtPoint
    while(superSelShape!=getDoc() && shapeAtPoint==null) {
        point = superSelShape.localToParent(point);
        superSelShape = superSelShape.getParent();
        shapeAtPoint = getChildShapeAtPoint(superSelShape, point);
    }

    // See if point really hits an upper level shape that overlaps shapeAtPoint
    if(shapeAtPoint!=null && shapeAtPoint!=getSelPage()) {
        
        // Declare shape/point variables used to iterate up shape hierarchy
        RMShape ssShape = shapeAtPoint;
        Point pnt = point;

        // Iterate up shape hierarchy
        while(ssShape!=getSelPage() && ssShape.getParent()!=null) {
            
            // Get child of parent hit point point
            RMShape hitChild = getChildShapeAtPoint(ssShape.getParent(), pnt);
            
            // If child not equal to original shape, change shapeAtPoint
            if(hitChild != ssShape) {
                superSelShape = ssShape.getParent();
                shapeAtPoint = hitChild; point = pnt;
            }
            
            // Update loop shape/point variables
            ssShape = ssShape.getParent();
            pnt = ssShape.localToParent(pnt);
        }
    }

    // Make sure page is worst case
    if(shapeAtPoint==null || shapeAtPoint==getDoc())
        shapeAtPoint = getSelPage();
        
    // Return shape at point
    return shapeAtPoint;
}

/**
 * Returns the child of the given shape hit by the given point.
 */
public RMShape getChildShapeAtPoint(RMShape aShape, Point aPoint)
{
    // If given shape is null, return null
    if(aShape==null) return null;
    
    // Iterate over shape children
    for(int i=aShape.getChildCount(); i>0; i--) { RMShape child = aShape.getChild(i-1);
        
        // If not hittable, continue
        if(!child.isHittable()) continue;
        
        // Get given point in child shape coords
        Point point = child.parentToLocal(aPoint);

        // If child is super selected and point is in child super selected bounds, return child
        if(isSuperSelected(child) && getTool(child).getBoundsSuperSelected(child).contains(point))
            return child;
        
        // If child isn't super selected and contains point, return child
        else if(child.contains(point))
            return child;
    }
    
    // Return null if no children hit by point
    return null;
}

/**
 * Returns the first SuperSelectedShape that accepts children.
 */
public RMParentShape firstSuperSelectedShapeThatAcceptsChildren()
{
    // Get super selected shape
    RMShape shape = getSuperSelectedShape();
    RMParentShape parent = shape instanceof RMParentShape? (RMParentShape)shape : shape.getParent();

    // Iterate up hierarchy until we find a shape that acceptsChildren
    while(!getTool(parent).getAcceptsChildren(parent))
        parent = parent.getParent();

    // Make sure page is worst case
    if(parent==getDoc())
        parent = getSelPage();

    // Return parent
    return parent;
}

/**
 * Returns the first SuperSelected shape that accepts children at a given point.
 */
public RMShape firstSuperSelectedShapeThatAcceptsChildrenAtPoint(Point aPoint)
{
    // Go up chain of superSelectedShapes until one acceptsChildren and is hit by aPoint
    RMShape shape = getSuperSelectedShape();
    RMParentShape parent = shape instanceof RMParentShape? (RMParentShape)shape : shape.getParent();

    // Iterate up shape hierarchy until we find a shape that is hit and accepts children
    while(!getTool(parent).getAcceptsChildren(parent) ||
        !parent.contains(parent.parentToLocal(aPoint, null))) {

        // If shape childrenSuperSelImmd and shape hitByPt, see if any shape children qualify (otherwise use parent)
        if(parent.childrenSuperSelectImmediately() && parent.contains(parent.parentToLocal(aPoint, null))) {
            RMShape childShape = parent.getChildContaining(parent.parentToLocal(aPoint, null));
            if(childShape!=null && getTool(childShape).getAcceptsChildren(childShape))
                parent = (RMParentShape)childShape;
            else parent = parent.getParent();
        }

        // If shape's children don't superSelectImmediately or it is not hit by aPoint, just go up parent chain
        else parent = parent.getParent();

        if(parent==null)
            return getSelPage();
    }

    // Make sure page is worst case
    if(parent==getDoc())
        parent = getSelPage();

    // Return shape
    return parent;
}

/**
 * Standard clipboard cut functionality.
 */
public void cut()  { RMEditorClipboard.cut(this); }

/**
 * Standard clipboard copy functionality.
 */
public void copy()  { RMEditorClipboard.copy(this); }

/**
 * Standard clipbard paste functionality.
 */
public void paste()  { RMEditorClipboard.paste(this); }

/**
 * Causes all the children of the current super selected shape to become selected.
 */
public void selectAll()
{
    // If document selected, select page
    RMShape superSelShape = getSuperSelectedShape();
    if(superSelShape instanceof RMDocument) {
        setSuperSelectedShape(((RMDocument)superSelShape).getSelPage());
        superSelShape = getSuperSelectedShape();
    }
    
    // If text editing, forward to text editor
    if(getTextEditor()!=null)
        getTextEditor().selectAll();
    
    // Otherwise, select all children
    else if(superSelShape.getChildCount()>0) {
        
        // Get list of all hittable children of super-selected shape
        List shapes = new ArrayList();
        for(RMShape shape : superSelShape.getChildren())
            if(shape.isHittable())
                shapes.add(shape);
        
        // Select shapes
        setSelectedShapes(shapes);
    }
}

/**
 * Deletes all the currently selected shapes.
 */
public void delete()
{
    // Get copy of selected shapes (just beep and return if no selected shapes)
    RMShape shapes[] = _selShapes.toArray(new RMShape[0]);
    if(shapes.length==0) { if(getTextEditor()==null) beep(); return; }

    // Get/superSelect parent of selected shapes
    RMParentShape parent = getSelectedShape().getParent(); if(parent==null) return;
    setSuperSelectedShape(parent);

    // Set undo title
    undoerSetUndoTitle(getSelectedShapeCount()>1? "Delete Shapes" : "Delete Shape");
    
    // Remove all shapes from their parent
    for(RMShape shape : shapes) {
        parent.removeChild(shape);
        if(_lastPasteShape==shape) _lastPasteShape = null;
        if(_lastCopyShape==shape) _lastCopyShape = null;
    }
}

/**
 * Adds shapes as children to given shape.
 */
public void addShapesToShape(List <? extends RMShape> theShapes, RMParentShape aShape, boolean withCorrection)
{
    // If no shapes, just return
    if(theShapes.size()==0) return;
    
    // Declare variables for dx, dy, dr
    double dx = 0, dy = 0, dr = 0;

    // Smart paste
    if(withCorrection) {

        // If there is an last-copy-shape and new shapes will be it's peer, set offset
        if(_lastCopyShape!=null && _lastCopyShape.getParent()==aShape) {

            if(_lastPasteShape!=null) {
                RMShape firstShape = theShapes.get(0);
                dx = 2*_lastPasteShape.x() - _lastCopyShape.x() - firstShape.x();
                dy = 2*_lastPasteShape.y() - _lastCopyShape.y() - firstShape.y();
                dr = 2*_lastPasteShape.getRoll() - _lastCopyShape.getRoll() - firstShape.getRoll();
            }

            else dx = dy = getDoc().getGridSpacing();
        }
    }

    // Get each individual shape and add it to the superSelectedShape
    for(int i=0, iMax=theShapes.size(); i<iMax; i++) { RMShape shape = theShapes.get(i);
        
        // Add current loop shape to given parent shape
        aShape.addChild(shape);

        // Smart paste
        if(withCorrection) {
            Rect parentShapeRect = aShape.getBoundsInside();
            shape.setXY(shape.x() + dx, shape.y() + dy);
            shape.setRoll(shape.getRoll() + dr);
            Rect rect = shape.getFrame();
            rect.width = Math.max(1, rect.width);
            rect.height = Math.max(1, rect.height);
            if(!parentShapeRect.intersectsRect(rect))
                shape.setXY(0, 0);
        }
    }
}

/**
 * Adds a page to the document after current page.
 */
public void addPage()  { addPage(null, getSelPageIndex()+1); }

/**
 * Adds a page to the document before current page.
 */
public void addPagePrevious()  { addPage(null, getSelPageIndex()); }

/**
 * Adds a given page to the current document at the given index.
 */
public void addPage(RMPage aPage, int anIndex)
{
    RMDocument doc = getDoc(); if(doc==null) { beep(); return; }
    RMPage page = aPage!=null? aPage : doc.createPage();
    doc.addPage(page, anIndex);
    setSelPageIndex(anIndex);
}

/**
 * Removes current page from document.
 */
public void removePage()
{
    RMDocument doc = getDoc();
    if(doc==null || doc.getPageCount()<=1) { beep(); return; }
    removePage(getSelPageIndex());
}

/**
 * Removes the document page at the given index.
 */
public void removePage(int anIndex)
{
    // Register for Undo, remove page and set page to previous one
    RMDocument doc = getDoc(); if(doc==null) { beep(); return; }
    undoerSetUndoTitle("Remove Page");
    doc.removePage(anIndex);
    setSelPageIndex(Math.min(anIndex, doc.getPageCount()-1));
}

/**
 * Returns the SelectTool.
 */
public RMSelectTool getSelectTool()
{
    if(_selectTool!=null) return _selectTool;
    _selectTool = new RMSelectTool(); _selectTool.setEditor(this);
    return _selectTool;
}

/**
 * Returns the specific tool for a list of shapes (if they have the same tool).
 */
public RMTool getTool(List aList)
{
    Class commonClass = ClassUtils.getCommonClass(aList); // Get class for first object
    return getTool(commonClass); // Return tool for common class
}

/**
 * Returns the specific tool for a given shape.
 */
public RMTool getTool(Object anObj)
{
    // Get the shape class and tool from tools map - if not there, find and set
    Class sclass = ClassUtils.getClass(anObj);
    RMTool tool = _tools.get(sclass);
    if(tool==null) {
        _tools.put(sclass, tool = RMTool.createTool(sclass));
        tool.setEditor(this);
    }
    return tool;
}

/**
 * Tool method - returns the currently selected tool.
 */
public RMTool getCurrentTool()  { return _currentTool; }

/**
 * Tool method - sets the currently select tool to the given tool.
 */
public void setCurrentTool(RMTool aTool)
{
    // If tool is already current tool, just reactivate and return
    if(aTool==_currentTool) {
        aTool.reactivateTool(); return; }

    // Deactivate current tool and reset to new tool
    _currentTool.deactivateTool();
    
    // Set new current tool
    firePropChange(CurrentTool_Prop, _currentTool, _currentTool = aTool);
        
    // Activate new tool and have editor repaint
    _currentTool.activateTool();
        
    // Repaint editor
    repaint();
}

/**
 * Returns whether the select tool is currently selected.
 */
public boolean isCurrentToolSelectTool()  { return _currentTool==getSelectTool(); }

/**
 * Sets the current tool to the select tool.
 */
public void setCurrentToolToSelectTool()
{
    if(getCurrentTool()!=getSelectTool())
        setCurrentTool(getSelectTool());
}

/**
 * Tool method - Returns whether the select tool is currently selected and if it's currently being used to select.
 */
public boolean isCurrentToolSelectToolAndSelecting()
{
    return isCurrentToolSelectTool() && getSelectTool().getDragMode()==RMSelectTool.DragMode.Select;
}

/**
 * Resets the currently selected tool.
 */
public void resetCurrentTool()
{
    _currentTool.deactivateTool();
    _currentTool.activateTool();
}

/**
 * Override viewer method to reset selected shapes on page change.
 */
public void setSelPageIndex(int anIndex)
{
    super.setSelPageIndex(anIndex); // Do normal version
    setSuperSelectedShape(getSelPage()); // Super-select new page
}

/**
 * Scrolls selected shapes to visible.
 */
public Rect getSelectedShapesBounds()
{
    // Get selected/super-selected shape(s) and parent (just return if parent is null or document)
    List <? extends RMShape> shapes = getSelectedOrSuperSelectedShapes();
    RMShape parent = shapes.get(0).getParent();
    if(parent==null || parent instanceof RMDocument)
        return getDocBounds();
    
    // Get select shapes rect in viewer coords and return
    Rect sbounds = RMShapeUtils.getBoundsOfChildren(parent, shapes);
    sbounds = convertFromShape(sbounds, parent).getBounds();
    return sbounds;
}

/**
 * Override to have zoom focus on selected shapes rect.
 */
public Rect getZoomFocusRect()
{
    Rect sbounds = getSelectedShapesBounds();
    Rect vrect = getVisRect();
    sbounds.inset((sbounds.getWidth() - vrect.getWidth())/2, (sbounds.getHeight() - vrect.getHeight())/2);
    return sbounds;
}

/**
 * Overrides Viewer implementation to paint tool extras, guides, datasource image.
 */
public void paintFront(Painter aPntr)
{
    // Do normal paint
    super.paintFront(aPntr);
    
    // Have current tool paintTool (paints selected shape handles by default)
    RMTool tool = getCurrentTool();
    tool.paintTool(aPntr);
   
    // Paint proximity guides
    RMEditorProxGuide.paintProximityGuides(this, aPntr);
    
    // If datasource is present and editing, draw XMLImage in lower right corner of doc
    if(getDataSource()!=null && isEditing()) {

        // Get visible rect and image X & Y
        Rect vrect = getVisRect();
        int x = (int)vrect.getMaxX() - 53;
        int y = (int)vrect.getMaxY() - 53;
        
        // Draw semi-transparent image: Cache previous composite, set semi-transparent composite, paint image, restore
        aPntr.setOpacity(.9);
        aPntr.drawImage(_xmlImage, x + _xmlDX, y + _xmlDY, 53, 53);
        aPntr.setOpacity(1);
    }
    
    // Paint DragShape, if set
    if(_dragShape!=null) {
        aPntr.setColor(new Color(0,.6,1,.5)); aPntr.setStrokeWidth(3); aPntr.draw(_dragShape); }
}

/**
 * Override to return as RMEditorEvents.
 */
public RMEditorEvents getEvents()  { return (RMEditorEvents)super.getEvents(); }

/**
 * Override to return RMEditorEvents.
 */
public RMViewerEvents createEvents()  { return new RMEditorEvents(this); }

/**
 * Override to revalidate when ideal size changes.
 */
protected void processEvent(ViewEvent anEvent)
{
    // Do normal version
    super.processEvent(anEvent);
    
    // Handle DragEvent
    if(anEvent.isDragEvent())
        _dragHelper.processEvent(anEvent);
        
    // See if zoom needs to be reset for any input events
    else if(anEvent.isMouseDrag() || anEvent.isMouseRelease() || anEvent.isKeyRelease()) {
        
        // If zoom to factor, revalidate when preferred size changes
        if(isZoomToFactor()) {
            if(!getSize().equals(getPrefSize()))
                relayout();
            if(!getVisRect().contains(getSelectedShapesBounds()) &&
                getSelectTool().getDragMode()==RMSelectTool.DragMode.Move)
                scrollToVisible(getSelectedShapesBounds());
        }
        
        // If zoom to fit, update zoom to fit factor (just returns if unchanged)
        else setZoomToFitFactor();
    }        
}

/** 
 * Returns a tool tip string by asking deepest shape's tool.
 */
public String getToolTip(ViewEvent anEvent)
{
    // If not editing, do normal get tool tip text
    if(!isEditing()) return super.getToolTip(anEvent);
    
    // Get deepest shape under point (just return if null), get tool and return tool's ToolTip for shape
    RMShape shape = getShapeAtPoint(anEvent.getX(), anEvent.getY(), true); if(shape==null) return null;
    RMTool tool = getTool(shape);
    return tool.getToolTip(shape, anEvent);
}

/**
 * Returns the datasource associated with the editor's document.
 */
public RMDataSource getDataSource()  { RMDocument d = getDoc(); return d!=null? d.getDataSource() : null; }

/**
 * Sets the datasource associated with the editor's document.
 */
public void setDataSource(RMDataSource aDataSource, double aX, double aY)
{
    // Set Doc.DataSource and repaint
    getDoc().setDataSource(aDataSource);
    repaint();

    // If valid drop point, animate into place
    if(aX>0) {
        Rect vrect = getVisRect();
        double dx = aX - (vrect.getMaxX() - 53);
        double dy = aY - (vrect.getMaxY() - 53);
        getAnimCleared(1800).setOnFrame(() -> setDataSourceAnimFrame(dx, dy)).play();
    }
}

/**
 * Called when setDataSource gets frame update.
 */
private void setDataSourceAnimFrame(double dx, double dy)
{
    ViewAnim anim = getAnim(0);
    double time = anim.getTime(), maxTime = anim.getMaxTime();
    double ratio = time/maxTime;
    _xmlDX = SnapUtils.doubleValue(anim.interpolate(dx, 0, ratio));
    _xmlDY = SnapUtils.doubleValue(anim.interpolate(dy, 0, ratio));
    repaint();
}

/**
 * Returns the sample dataset from the document's datasource.
 */
public Object getDataSourceDataset()  { RMDataSource ds = getDataSource(); return ds!=null? ds.getDataset() : null; }

/**
 * Resets the editor pane later.
 */
public void resetEditorPaneLater()
{
    RMEditorPane ep = getEditorPane();
    ep.resetLater();
}

/**
 * Resets the editor pane later.
 */
public void resetEditorPaneOnMouseUp()
{
    RMEditorPane ep = getEditorPane();
    ViewUtils.runOnMouseUp(() -> ep.resetLater());
}

/**
 * Called to undo the last edit operation in the editor.
 */
public void undo()
{
    // If undoer exists, do undo, select shapes and repaint
    if(getUndoer()!=null && getUndoer().getUndoSetLast()!=null) {
        UndoSet undoSet = getUndoer().undo();
        setUndoSelection(undoSet.getUndoSelection());
        repaint();
    }

    // Otherwise beep
    else beep();
}

/**
 * Called to redo the last undo operation in the editor.
 */
public void redo()
{
    // If undoer exists, do undo, select shapes and repaint
    if(getUndoer()!=null && getUndoer().getRedoSetLast()!=null) {
        UndoSet redoSet = getUndoer().redo();
        setUndoSelection(redoSet.getRedoSelection());
        repaint();
    }

    // Otherwise beep
    else beep();
}

/**
 * Sets the undo selection.
 */
protected void setUndoSelection(Object aSelection)
{
    // Handle List <RMShape>
    if(aSelection instanceof List)
        setSelectedShapes((List)aSelection);
}

/**
 * Property change.
 */
public void deepChange(Object aShape, PropChange aPC)
{
    // If deep change for EditorTextEditor, just return since it registers Undo itself (with better coalesce)
    //if(getTextEditor()!=null && getTextEditor().getTextShape()==aShape &&
    //    (anEvent.getSource() instanceof RMXString || anEvent.getSource() instanceof RMXStringRun)) return;
    
    // Add undo change
    addUndoChange(aPC);
    
    // Reset EditorPane UI
    resetEditorPaneLater();
}

/**
 * Property change.
 */
protected void addUndoChange(PropChange aPC)
{
    // Get undoer (just return if null)
    Undoer undoer = getUndoer(); if(undoer==null) return;
    
    // Handle some changes special
    String pname = aPC.getPropName();
    if(pname==RMGraph.ProxyShape_Prop) {
        resetEditorPaneOnMouseUp(); return; }
    
    // If no undos and change is RMDocument.SelectedPage or RMTableGroup.MainTable, just return
    if(!undoer.hasUndos()) {
        if(pname==RMDocument.SelPageIndex_Prop) return;
        if(pname=="MainTable") return;
        if(pname=="Version") return;
    }
        
    // If no changes yet, set selected objects
    if(undoer.getActiveUndoSet().getChangeCount()==0)
        undoer.setUndoSelection(new ArrayList(getSelectedOrSuperSelectedShapes()));
    
    // Add property change
    undoer.addPropChange(aPC);
    
    // Save UndoerChanges after delay
    saveUndoerChangesLater();
}

/**
 * Saves Undo Changes.
 */
protected void saveUndoerChanges()
{
    // Get undoer
    Undoer undoer = getUndoer(); if(undoer==null || !undoer.isEnabled()) return;
    
    // Set undo selected-shapes
    List shapes = getSelectedShapeCount()>0? getSelectedShapes() : getSuperSelectedShapes();
    if(undoer.getRedoSelection()==null)
        undoer.setRedoSelection(new ArrayList(shapes));
    
    // Save undo changes
    undoer.saveChanges();
    
    // Reset EditorPane
    resetEditorPaneLater();
}

/**
 * Saves undo changes after a delay.
 */
protected void saveUndoerChangesLater()
{
    // If runnable already set, just return
    if(_saveChangesRun!=null) return; _saveChangesRun = _scrShared;
    
    // If MouseDown, run on mouse up, otherwise run later
    if(ViewUtils.isMouseDown()) ViewUtils.runOnMouseUp(_saveChangesRun);
    else getEnv().runLater(_saveChangesRun);
}

// A Runnable for runLater(saveUndoerChanges())
private Runnable _saveChangesRun, _scrShared = () -> { saveUndoerChanges(); _saveChangesRun = null; };

/**
 * A RMShapePaintProps subclass for editor.
 */
private class EditorShapePainterProps extends RMShapePaintProps {

    /** Returns whether painting is for editor. */
    public boolean isEditing()  { return RMEditor.this.isEditing(); }
    
    /** Returns whether given shape is selected. */
    public boolean isSelected(RMShape aShape)  { return RMEditor.this.isSelected(aShape); }
    
    /** Returns whether given shape is super selected. */
    public boolean isSuperSelected(RMShape aShape)  { return RMEditor.this.isSuperSelected(aShape); }
    
    /** Returns whether given shape is THE super selected shape. */
    public boolean isSuperSelectedShape(RMShape aShape)  { return RMEditor.this.getSuperSelectedShape()==aShape; }
}

/** Play beep. */
public void beep()  { ViewUtils.beep(); }

}