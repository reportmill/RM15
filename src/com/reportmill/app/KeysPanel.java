/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.data.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * This class shows the current set of keys relative to the current editor selection in a browser and lets users
 * drag and drop them to the editor.
 */
public class KeysPanel extends RMEditorPane.SupportPane {
    
    // The KeysPanel browser
    BrowserView              _keysBrowser;
    
    // The current entity for the keys browser
    Entity                   _entity = new Entity("Bogus");
    
    // The current root items for the keys browser
    List <KeyNode>           _rootItems;
    
    // The icon for a "to-many" branch
    static Image             _doubleArrowImage;
    
    // The Drag and Drop key, since Java 1.4 doesn't support getting transferable in dragEnter/dragOver.
    String                   _dragKey;

    // Whether to show built in keys
    boolean                  _showBuiltIn = false;
    
    // Contants aggregate keys
    static final String      _aggregateKeys[] = { "total", "average", "count", "countDeep", "max", "min" };
    
    // Constants for heritage keys
    static final String      _heritageKeys[] = { "Running", "Remaining", "Up" };
    
    // Constants for built-in keys
    static final String      _builtInKeys[] = { "Date", "Page", "PageMax", "Page of PageMax",
        "PageBreak", "PageBreakMax", "PageBreakPage", "PageBreakPageMax", "Row" };
        
    // Shared built-in key nodes
    static List <KeyNode>    _builtInKeyNodes;

    // Shared
    static KeysPanel         _shared;

/**
 * Creates a new KeysPanel.
 */
public KeysPanel(RMEditorPane anEP)
{
    super(anEP);
    _builtInKeyNodes = new ArrayList(); for(String key : _builtInKeys) _builtInKeyNodes.add(new KeyNode(key));
    if(_shared==null) _shared = this;
}

/**
 * Returns the entity.
 */
public Entity getEntity()  { return _entity; }

/**
 * Returns the current key path selected by the browser.
 */
public String getKeyPath()
{
    String key = _keysBrowser.getPath(); // Get normal path
    if(key.equals("Page of PageMax")) return "@Page@ of @PageMax@";  // Special case for Page of PageMax
    return "@" + key + "@"; // Return path with @ signs
}
  
/**
 * Returns whether selected item is to-many.
 */
public boolean isSelectedLeaf()
{
    KeyNode node = (KeyNode)_keysBrowser.getSelectedItem();
    return node!=null && !node.isParent();
}

/**
 * Returns whether selected item is to-many.
 */
public static boolean isSelectedToMany()
{
    KeyNode node = (KeyNode)_shared._keysBrowser.getSelectedItem();
    return node!=null && node._isToMany;
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    _keysBrowser = getView("KeysBrowser", BrowserView.class);
    _keysBrowser.setResolver(new KeysBrowserResolver()); _keysBrowser.setRowHeight(16);
    _keysBrowser.setCellConfigure(c -> configureKeysBrowserCell((ListCell)c));
    enableEvents(_keysBrowser, MouseRelease, DragGesture, View.DragSourceEnd);
}

/**
 * Updates the UI from the current selection.
 */
public void resetUI()
{
    // Get selected shape and shape tool
    RMShape selectedShape = getSelectedShape();
    RMTool tool = getEditor().getTool(selectedShape);
    
    // Get entity from tool/shape and set in browser
    Entity entity = !_showBuiltIn? tool.getDatasetEntity(selectedShape) : null;
    if(entity!=_entity) { _entity = entity;
        _rootItems = entity!=null? new KeyNode("Root").getChildren() : _builtInKeyNodes;
        _keysBrowser.setItems(_rootItems);
    }
    
    // Update BuiltInKeysButton
    setViewValue("BuiltInKeysButton", _showBuiltIn);
}

/**
 * Updates the current selection from the UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle KeysBrowser (double-click - used to check anEvent.getClickCount()==2)
    if(anEvent.equals("KeysBrowser") && anEvent.isMouseClick() && anEvent.getClickCount()==2) {

        // If double-click on RMTable, add grouping
        RMEditor editor = getEditor();
        if(getSelectedShape() instanceof RMTable) {
            RMTableTool tool = (RMTableTool)editor.getTool(getSelectedShape());
            tool.addGroupingKey(_keysBrowser.getPath());
        }

        // If leaf click for RMText, add key
        else if(isSelectedLeaf() && editor.getTextEditor()!=null)
            editor.getTextEditor().replace(getKeyPath());
    }
    
    // Handle DragGesture
    if(anEvent.isDragGesture()) {
        
        // If drag was in scrollbar, just return
        if(ViewUtils.getDeepestChildAt(anEvent.getView(), anEvent.getX(), anEvent.getY(), ScrollBar.class)!=null)
            return;
        
        // Set the drag key and get drag key with @-signs
        _dragKey = _keysBrowser.getPath();
        String dragKeyFull = getKeyPath();
    
        // Get event Dragboard and start drag
        Clipboard dboard = anEvent.getDragboard();
        dboard.setContent(dragKeyFull);
        dboard.setDragImage(ImageUtils.getImage(dragKeyFull, getSelectedShape().getDocument().getFont()));
        dboard.startDrag();
    }
    
    // Handle KeysBrowser DragSourceEnd
    if(anEvent.isDragSourceEnd())
        _dragKey = null;
        
    // Handle BuiltInKeysButton
    if(anEvent.equals("BuiltInKeysButton"))
        _showBuiltIn = anEvent.getBoolValue();
}

/**
 * Returns the current editor's selected shape.
 */
public RMShape getSelectedShape()  { return getEditor().getSelectedOrSuperSelectedShape(); }

/**
 * Returns the window title for this panel.
 */
public String getWindowTitle()  { return "Keys Panel"; }

/**
 * Returns the current drag key.
 */
public static String getDragKey()
{
    return _shared!=null && _shared._keysBrowser!=null? _shared._dragKey : null;
}

/**
 * Sets the current drag key.
 */
public static void setDragKey(String aKey)  { _shared._dragKey = aKey; }

/**
 * Returns the icon of a double right arrow to indicate branch nodes of a "to-many" relationship in a browser.
 */
public Image getDoubleArrowImage()
{
    // If double arrow icon hasn't been created, create it
    if(_doubleArrowImage!=null) return _doubleArrowImage;
    Image img = Image.get(16, 11, true);
    Polygon poly = new Polygon(1.5,1.5,7.5,5.5,1.5,9.5);
    Painter pntr = img.getPainter(); pntr.setColor(Color.BLACK); pntr.fill(poly);
    pntr.translate(7,0); pntr.fill(poly); pntr.flush();
    return _doubleArrowImage = img;
}

/**
 * Returns whether selected key path is to-many.
 */
//public static boolean isSelectedToMany()  { return _shared!=null && _shared._keysBrowser.isSelectedToMany(); }

/**
 * Drops a drag key.
 */
public static void dropDragKey(RMShape aShape, ViewEvent anEvent)
{
    // Get editor
    RMEditor editor = (RMEditor)anEvent.getView(); //getDropTargetContext().getComponent();
    
    // Handle KeysPanel to-many drop - run dataset key panel (after delay)
    if(isSelectedToMany()) {
        String datasetKey = StringUtils.delete(KeysPanel.getDragKey(), "@");
        editor.getEnv().runLater(() -> RMEditorShapes.runDatasetKeyPanel(editor, datasetKey));
    }
    
    // Otherwise, just drop string as text shape
    else {
        aShape.repaint();
        editor.undoerSetUndoTitle("Drag and Drop Key");
        Clipboard cb = anEvent.getDragboard();
        RMEditorClipboard.paste(editor, cb, (RMParentShape)aShape, anEvent.getPoint());
    }
}

/**
 * Called to configure KeysBrowser cell: Make aggregate keys bold and .
 */
public void configureKeysBrowserCell(ListCell <KeyNode> aCell)
{
    KeyNode knode = aCell.getItem();
    if(knode!=null && knode._special) aCell.setFont(_keysBrowser.getFont().getBold());
    aCell.setToolTip(aCell.getText());
}

/**
 * An inner class to provide data for keys browser.
 */
class KeysBrowserResolver extends TreeResolver <KeyNode> {
    
    /** Returns the parent of given item. */
    public KeyNode getParent(KeyNode anItem)  { return anItem._parent; }
    
    /** Returns whether given browser node is a leaf. */
    public boolean isParent(KeyNode anItem)  { return anItem.isParent(); }

    /** Returns the specific child of the given browser node at the given index.  */
    public KeyNode[] getChildren(KeyNode anItem)  { return anItem.getChildren().toArray(new KeyNode[0]); }
    
    // The text of given item
    public String getText(KeyNode anItem)  { return anItem.getKey(); }
    
    // The branch icon of given item
    public Image getBranchImage(KeyNode anItem)  { return anItem._isToMany? getDoubleArrowImage() : null; }
}

/**
 * An inner class for Node of KeysBrowser.
 */
private class KeyNode extends Object {

    // Node parent
    KeyNode           _parent;

    // Node key
    String            _key;
    
    // The Property (if based on one)
    Property          _prop;
    
    // Node children
    List              _children;
    
    // Whether node is "to-many"
    boolean           _isToMany;
    
    // Whether node is special key (aggregate or heritage)
    boolean           _special;
    
    /** Creates a new KeyNode for key. */
    public KeyNode(String aKey)  { _key = aKey; }
    
    /** Creates a new KeyNode for given parent, key and optional property. */
    public KeyNode(KeyNode aParent, String aKey, Property aProp)  { this(aKey); _parent = aParent; _prop = aProp; }
    
    /** Returns the node key. */
    public String getKey()  { return _key; }
    
    /** Returns whether node is a parent. */
    public boolean isParent()  { return _prop!=null && _prop.isRelation() || _special; }
    
    /** Returns the list of children for this node. */
    public List <KeyNode> getChildren()  { return _children!=null? _children : (_children=createChildren()); }
    
    /** Creates the list of children for this node. */
    protected List <KeyNode> createChildren()
    {
        // Create children list and get entity for node
        List children = new ArrayList();
        Entity entity = getEntity(); if(entity==null) return children;
        
        // Add attributes
        for(int i=0, iMax=entity.getAttributeCount(); i<iMax; i++) {
            Property attr = entity.getAttributeSorted(i); if(attr.isPrivate()) continue;
            KeyNode child = new KeyNode(this, attr.getName(), attr);
            children.add(child);
        }
        
        // Add relations
        for(int i=0, iMax=entity.getRelationCount(); i<iMax; i++) {
            Property rel = entity.getRelationSorted(i); if(rel.isPrivate()) continue;
            KeyNode child = new KeyNode(this, rel.getName(), rel);
            child._isToMany = rel.isToMany();
            children.add(child);
        }
        
        // Add aggregate keys
        if(getShowAggregates())
            for(int i=0; i<_aggregateKeys.length; i++) {
                KeyNode child = new KeyNode(this, _aggregateKeys[i], null);
                children.add(child); child._special = true;
            }
        
        // If Root, add heritage keys
        if(_parent==null)
            for(int i=0; i<_heritageKeys.length; i++) {
                KeyNode child = new KeyNode(this, _heritageKeys[i], null);
                children.add(child); child._special = true;
            }
        
        // Return list of child nodes
        return children;
    }
    
    /** Returns whether node should have aggregates. */
    protected boolean getShowAggregates()
    {
        if(_parent==null) return true;
        if(ArrayUtils.contains(_heritageKeys, getKey())) return true;
        if(_prop!=null && _prop.isRelation() && _prop.isToMany()) return true;
        return false;
    }
    
    /** Returns the entity. */
    protected Entity getEntity()
    {
        if(_parent==null) return KeysPanel.this.getEntity();
        if(_prop!=null) return _prop.getRelationEntity();
        return _parent.getEntity();
    }
    
    // Returns node as string
    public String toString()  { return _key; }
}

}