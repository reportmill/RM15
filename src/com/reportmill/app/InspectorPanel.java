/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * This class is responsible for the UI associated with the inspector window.
 */
public class InspectorPanel extends RMEditorPane.SupportPane {
    
    // The selection path view
    ChildView             _selPathView;
    
    // The Title label
    Label                 _titleLabel;
    
    // The ShapeButton
    ToggleButton          _shapeBtn;
    
    // The ScrollView that holds UI for child inspectors
    ScrollView            _inspBox;
    
    // The child inspector current installed in inspector panel
    ViewOwner            _childInspector;
    
    // The inspector for paint/fill shape attributes
    ShapeFills           _shapeFills = new ShapeFills(getEditorPane());
    
    // The inspector for shape placement attributes (location, size, roll, scale, skew, autosizing)
    ShapePlacement       _shapePlacement = new ShapePlacement(getEditorPane());
    
    // The inspector for shape general attributes (name, url, text wrap around)
    ShapeGeneral         _shapeGeneral = new ShapeGeneral(getEditorPane());
    
    // The inspector for shape animation
    ShapeTree            _shapeTree = new ShapeTree(getEditorPane());
    
    // The inspector for Undo
    UndoInspector        _undoInspector;
    
    // The inspector for XML datasource
    DataSourcePanel      _dataSource;
    
    // Used for managing selection path
    RMShape              _deepestShape;
    
    // Used for managing selection path
    RMShape              _selectedShape;

/**
 * Creates a new InspectorPanel for EditorPane.
 */
public InspectorPanel(RMEditorPane anEP)  { super(anEP); }

/**
 * Initializes UI panel for the inspector.
 */
public void initUI()
{
    // Get/configure TitleLabel
    _titleLabel = getView("TitleLabel", Label.class);
    _titleLabel.setTextFill(Color.GRAY);
    
    // Get SelPathView and InspectorPanel
    _selPathView = getView("SelPathView", ChildView.class);
    enableEvents(_selPathView, MouseRelease);
    _shapeBtn = getView("ShapeSpecificButton", ToggleButton.class);
    
    // Get/configure ContentBox
    _inspBox = getView("ContentBox", ScrollView.class);
    _inspBox.setBorder(null);
    _inspBox.setBarSize(12);
    _inspBox.setFillWidth(true);
    
    // Create the Action that redispatches the event and add the action to the action map
    addKeyActionHandler("UndoAction", "meta Z");
}

/**
 * Refreshes the inspector for the current editor selection.
 */
public void resetUI()
{
    // Get editor (and just return if null) and tool for selected shapes
    RMEditor editor = getEditor(); if(editor==null) return;
    RMTool tool = editor.getTool(editor.getSelectedOrSuperSelectedShapes());
    
    // If ShapeSpecificButton is selected, instal inspector for current selection
    if(getViewBoolValue("ShapeSpecificButton"))
        setInspector(tool);
    
    // If ShapeFillsButton is selected, install fill inspector
    if(getViewBoolValue("ShapeFillsButton"))
        setInspector(_shapeFills);

    // Get the inspector (owner)
    ViewOwner owner = getInspector();
    
    // Get inspector title from owner and set
    String title = "Inspector";
    if(owner instanceof RMTool) title = ((RMTool)owner).getWindowTitle();
    else if(owner instanceof RMEditorPane.SupportPane) {
        title = ((RMEditorPane.SupportPane)owner).getWindowTitle();
        String shpName = tool.getShapeClass().getSimpleName().replace("RM", "").replace("Shape", "");
        title += " (" + shpName + ')';
    }
    _titleLabel.setText(title);

    // If owner non-null, tell it to reset
    if(owner!=null)
        owner.resetLater();
    
    // Reset the selection path view
    resetSelPathView();
    
    // Get image for current tool and set in ShapeSpecificButton
    Image timage = tool.getImage();
    getView("ShapeSpecificButton", ButtonBase.class).setImage(timage);
}

/**
 * Handles changes to the inspector UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle ShapePlacementButton
    if(anEvent.equals("ShapePlacementButton"))
        setInspector(_shapePlacement);
    
    // Handle ShapeGeneralButton
    if(anEvent.equals("ShapeGeneralButton"))
        setInspector(_shapeGeneral);
    
    // Handle UndoAction
    if(anEvent.equals("UndoAction"))
        getEditor().undo();
        
    // Handle SelPath
    if(anEvent.getName().startsWith("SelPath"))
        popSelection(SnapUtils.intValue(anEvent.getName()));
    
    // Handle SelPathView
    if(anEvent.equals("SelPathView") && anEvent.isMouseRelease())
        setVisible(9);
}

/**
 * Returns whether the inspector is visible.
 */
public boolean isVisible()  { return isUISet() && getUI().isShowing(); }

/**
 * Sets whether the inspector is visible.
 */
public void setVisible(boolean aValue)
{
    // If requested visible and inspector is not visible, make visible
    if(aValue && !isVisible())
        setVisible(-1);
}

/**
 * Sets the inspector to be visible, showing the specific sub-inspector at the given index.
 */
public void setVisible(int anIndex)
{
    // If index 0, 1 or 3, set appropriate toggle button true
    if(anIndex==0) setViewValue("ShapeSpecificButton", true);
    if(anIndex==1) setViewValue("ShapeFillsButton", true);
    if(anIndex==3) setViewValue("ShapeGeneralButton", true);
    
    // If index is 6, show _undoInspector
    if(anIndex==6) {
        setInspector(_undoInspector!=null? _undoInspector : (_undoInspector = new UndoInspector(getEditorPane())));
        _shapeBtn.getToggleGroup().setSelected(null); //setViewValue("OffscreenButton", true);
    }
    
    // If index is 7, show DataSource Inspector
    if(anIndex==7) {
        setInspector(_dataSource!=null? _dataSource : (_dataSource = new DataSourcePanel(getEditorPane())));
        _shapeBtn.getToggleGroup().setSelected(null); //setViewValue("OffscreenButton", true);
    }
    
    // If index is 9, show ShapeTree Inspector
    if(anIndex==9) {
        setInspector(_shapeTree);
        _shapeBtn.getToggleGroup().setSelected(null);
    }
}

/**
 * Returns whether inspector should update when editor does.
 */
public boolean isResetWithEditor()
{
    if(!isVisible()) return false;
    if(!ViewUtils.isMouseDrag()) return true;
    return getInspector()==_shapePlacement;
}

/**
 * Returns whether the inspector is showing the datasource inspector.
 */
public boolean isShowingDataSource()  { return isUISet() && _dataSource!=null && _dataSource.getUI().isShowing(); }

/**
 * Returns the inspector (owner) of the inspector pane.
 */
protected ViewOwner getInspector()  { return _childInspector; }

/**
 * Sets the inspector in the inspector pane.
 */
protected void setInspector(ViewOwner anOwner)
{
    // Set new inspector
    _childInspector = anOwner;
    
    // Get content and it grows height
    View content = anOwner.getUI();
    boolean contentGrowHeight = content.isGrowHeight();
    
    // Set content and whether Inspector ScrollView sizes or scrolls content vertically
    _inspBox.setContent(content);
    _inspBox.setFillHeight(contentGrowHeight);
}

/**
 * Updates the selection path view.
 */
public void resetSelPathView() 
{
    // Get main editor, Selected/SuperSelected shape and shape that should be selected in selection path
    RMEditor editor = getEditor();
    RMShape selectedShape = editor.getSelectedOrSuperSelectedShape();
    RMShape shape = _deepestShape!=null && _deepestShape.isAncestor(selectedShape)? _deepestShape : selectedShape;
    
    // If the selectedShape has changed because of external forces, reset selectionPath to point to it
    if(selectedShape != _selectedShape)
        shape = selectedShape;
    
    // Set new DeepestShape to be shape
    _deepestShape = shape; _selectedShape = selectedShape;

    // Remove current buttons
    for(int i=_selPathView.getChildCount()-1; i>=0; i--) {
        View button = _selPathView.removeChild(i);
        if(button instanceof ToggleButton) getToggleGroup("SelPathGroup").remove((ToggleButton)button);
    }
    
    // Add buttons for DeepestShape and its ancestors
    for(RMShape shp=_deepestShape; shp!=null && shp.getParent()!=null; shp=shp.getParent()) {
        
        // Create new button and configure action
        ToggleButton button = new ToggleButton(); button.setName("SelPath " + (shp.getAncestorCount()-1));
        button.setPrefSize(40,40); button.setMinSize(40,40); button.setShowArea(false);
        
        // Set button images
        Image img = editor.getTool(shp).getImage();
        button.setImage(img);
        button.setToolTip(shp.getClass().getSimpleName());
        if(shp==selectedShape) button.setSelected(true);  // Whether selected
        
        // Add button to selection path panel and button group
        _selPathView.addChild(button, 0); button.setOwner(this);
        getToggleGroup("SelPathGroup").add(button);
        if(shp!=_deepestShape) _selPathView.addChild(new Sep(), 1);
    }
}

/**
 * Changes the selection path selection to the level of the string index in the action event.
 */
public void popSelection(int selIndex) 
{
    // Get main editor (just return if editor or deepest shape is null)
    RMEditor editor = getEditor(); if(editor==null || _deepestShape==null) return;
    
    // If user selected descendant of current selected shape, select on down to it
    if(selIndex > editor.getSelectedOrSuperSelectedShape().getAncestorCount()-1) {
        
        // Get current deepest shape
        RMShape shape = _deepestShape;

        // Find shape that was clicked on
        while(selIndex != shape.getAncestorCount()-1)
            shape = shape.getParent();

        // If shape parent's childrenSuperSelectImmediately, superSelect shape
        if(shape.getParent().childrenSuperSelectImmediately())
            editor.setSuperSelectedShape(shape);

        // If shape shouldn't superSelect, just select it
        else editor.setSelectedShape(shape);
    }

    // If user selected ancestor of current shape, pop selection up to it
    else while(selIndex != editor.getSelectedOrSuperSelectedShape().getAncestorCount()-1)
        editor.popSelection();

    // Set selected shape to new editor selected shape
    _selectedShape = editor.getSelectedOrSuperSelectedShape();
    
    // Make sure shape specific inspector is selected
    if(!getViewBoolValue("ShapeSpecificButton"))
        getView("ShapeSpecificButton", ToggleButton.class).fire();
}

/**
 * Makes the inspector panel show the document inspector.
 */
public void showDocumentInspector()
{
    setVisible(0);
    resetSelPathView();
    popSelection(0);
}

/** View to render SelectionPath separator. */
private static class Sep extends View {
    protected double getPrefWidthImpl(double aH)  { return 5; }
    protected double getPrefHeightImpl(double aW)  { return 40; }
    protected void paintFront(Painter aPntr)  { aPntr.setColor(Color.DARKGRAY); aPntr.fill(_arrow); }
    static Polygon _arrow = new Polygon(0, 15, 5, 20, 0, 25);
}

}