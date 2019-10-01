/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.base.*;
import com.reportmill.shape.*;
import java.util.*;
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
    
    // The Drag and Drop key
    String                   _dragKey;

    // Whether to show built in keys
    boolean                  _showBuiltIn = false;
    
    // The KeysTable
    TableView                _keysTable;
    
    // The KeysTable key
    String                   _keysTableKey = "";
    
    // Contants aggregate keys
    static final String      _aggregateKeys[] = { "total", "average", "count", "countDeep", "max", "min" };
    
    // Constants for heritage keys
    static final String      _heritageKeys[] = { "Running", "Remaining", "Up" };
    
    // Constants for built-in keys
    static final String      _builtInKeys[] = { "Date", "Page", "PageMax", "Page of PageMax",
        "PageBreak", "PageBreakMax", "PageBreakPage", "PageBreakPageMax", "Row" };
        
    // Shared built-in key nodes
    static List <KeyNode>    _builtInKeyNodes;

    // Current active KeysPanel
    static KeysPanel         _active;

/**
 * Creates a new KeysPanel.
 */
public KeysPanel(RMEditorPane anEP)
{
    super(anEP);
    _builtInKeyNodes = new ArrayList(); for(String key : _builtInKeys) _builtInKeyNodes.add(new KeyNode(key));
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
 * Returns the KeyPath entity.
 */
public Entity getKeyPathEntity(String aKey)
{
    Entity entity = getEntity(); if(entity==null) return null;
    Property kprop = entity.getKeyPathProperty(aKey); if(kprop==null) return null;
    Entity kentity = kprop.getRelationEntity();
    return kentity;
}

/**
 * Returns the items for key path entity.
 */
public List getKeyPathItems(String aKey)
{
    // Get full list key from selected shape and browser
    RMShape selShape = getSelectedShape();
    String kprfx = selShape.getDatasetKey(), ksfx = _keysBrowser.getPath();
    String key = kprfx!=null? (kprfx + '.' + ksfx) : ksfx;
    
    // Get Editor.Datasource dataset (just return if null)
    RMEditor editor = getEditor();
    RMDataSource dsrc = editor.getDataSource(); if(dsrc==null) return null;
    Map dset = dsrc.getDataset();
    
    // Get List
    List items = RMKeyChain.getListValue(dset, key);
    return items;
}
  
/**
 * Returns whether selected item is to-many.
 */
public boolean isSelectedLeaf()
{
    KeyNode node = (KeyNode)_keysBrowser.getSelItem();
    return node!=null && !node.isParent();
}

/**
 * Returns whether selected item is to-many.
 */
public boolean isSelectedToMany()
{
    KeyNode node = (KeyNode)_keysBrowser.getSelItem();
    return node!=null && node._isToMany;
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get/configure KeysBrowser
    _keysBrowser = getView("KeysBrowser", BrowserView.class);
    _keysBrowser.setResolver(new KeysBrowserResolver());
    _keysBrowser.setRowHeight(20);
    _keysBrowser.setCellConfigure(c -> configureKeysBrowserCell((ListCell)c));
    
    // Register KeysBrowser for click, drag
    enableEvents(_keysBrowser, MouseRelease, DragGesture, View.DragSourceEnd);
}

/**
 * Updates the UI from the current selection.
 */
public void resetUI()
{
    // Get selected shape and shape tool
    RMShape selShape = getSelectedShape();
    RMTool tool = getEditor().getTool(selShape);
    
    // Get entity from tool/shape and set in browser
    Entity entity = !_showBuiltIn? tool.getDatasetEntity(selShape) : null;
    if(entity!=_entity) { _entity = entity;
        _rootItems = entity!=null? new KeyNode("Root").getChildren() : _builtInKeyNodes;
        _keysBrowser.setItems(_rootItems);
    }
    
    // Update BuiltInKeysButton
    setViewValue("BuiltInKeysButton", _showBuiltIn);
    
    // Update KeysTableKey
    if(isShowKeysTable())
        setKeysTableKey(getKeyPath());
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
        _active = this;
        _dragKey = _keysBrowser.getPath();
        String dragKeyFull = getKeyPath();
    
        // Get event Clipboard and start drag
        Clipboard cboard = anEvent.getClipboard();
        cboard.addData(dragKeyFull);
        cboard.setDragImage(ImageUtils.getImage(dragKeyFull, getSelectedShape().getDocument().getFont()));
        cboard.startDrag();
        
        // Notify Attributes panel that dragging started
        getEditorPane().getAttributesPanel().childDragStart();
    }
    
    // Handle KeysBrowser DragSourceEnd
    if(anEvent.isDragSourceEnd()) {
        _dragKey = null;
        getEditorPane().getAttributesPanel().childDragStop();
    }
        
    // Handle BuiltInKeysButton
    if(anEvent.equals("BuiltInKeysButton"))
        _showBuiltIn = anEvent.getBoolValue();
        
    // Handle ShowKeysTableMenu
    if(anEvent.equals("ShowKeysTableMenu")) {
        boolean show = !isShowKeysTable();
        setShowKeysTable(show);
        runLaterDelayed(600, () -> getEditorPane().getAttributesPanel().getDrawer().setMaximized(show));
    }
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
 * Called to configure KeysBrowser cell: Make aggregate keys bold and .
 */
public void configureKeysBrowserCell(ListCell <KeyNode> aCell)
{
    KeyNode knode = aCell.getItem();
    if(knode!=null && knode._special) aCell.setFont(_keysBrowser.getFont().getBold());
    aCell.setToolTip(aCell.getText());
}

/**
 * Returns whether KeysTable is showing.
 */
public boolean isShowKeysTable()  { return _keysTable!=null && _keysTable.isShowing(); }

/**
 * Shows the KeysTable.
 */
public void setShowKeysTable(boolean aValue)
{
    // If already set, just return
    if(aValue==isShowKeysTable()) return;
    
    // Get SplitView
    SplitView split = getView("SplitView", SplitView.class);
    
    // If showing
    if(aValue) {
        
        // Create table if needed
        if(_keysTable==null) {
            _keysTable = new TableView();
            _keysTable.setGrowHeight(true);
            _keysTable.setShowHeader(true);
            _keysTable.setFont(Font.Arial12);
            _keysTable.setCellPadding(new Insets(2,4,2,4));
            _keysTable.setFocusable(false);
        }
        
        // Add KeysTable to split
        split.addItemWithAnim(_keysTable, 100);
    }
    
    // If hiding
    else split.removeItemWithAnim(_keysTable);
}

/**
 * Sets the KeysTableKey.
 */
public void setKeysTableKey(String aKey)
{
    // If already set, just return
    String key = aKey.replace("@", "");
    if(key.equals(_keysTableKey)) return;
    _keysTableKey = key;
    
    // Set columns
    while(_keysTable.getColCount()>0) _keysTable.removeCol(0);
    
    // Get entity for key
    Entity entity = getKeyPathEntity(key); if(entity==null) return;
    
    // Iterate over entity properties and add column for each
    List <Property> attrs = entity.getAttributes();
    for(Property prop : attrs) {
        if(prop.isRelation()) continue;
        TableCol col = new TableCol();
        col.setHeaderText(prop.getName());
        col.setItemKey(prop.getName());
        _keysTable.addCol(col);
    }
    
    // Get/set items
    List items = getKeyPathItems(key);
    _keysTable.setItems(items);
}

/**
 * Returns the icon of a double right arrow to indicate branch nodes of a "to-many" relationship in a browser.
 */
public static Image getDoubleArrowImage()
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
 * Returns the current drag key.
 */
public static String getDragKey()  { return _active!=null? _active._dragKey : null; }

/**
 * Drops a drag key.
 */
public static void dropDragKey(RMShape aShape, ViewEvent anEvent)
{
    // Get editor
    RMEditor editor = (RMEditor)anEvent.getView();
    
    // Handle KeysPanel to-many drop - run dataset key panel (after delay)
    if(_active.isSelectedToMany()) {
        String datasetKey = StringUtils.delete(KeysPanel.getDragKey(), "@");
        editor.getEnv().runLater(() -> RMEditorUtils.runDatasetKeyPanel(editor, datasetKey));
    }
    
    // Otherwise, just drop string as text shape
    else {
        aShape.repaint();
        editor.undoerSetUndoTitle("Drag and Drop Key");
        Clipboard cb = anEvent.getClipboard();
        RMEditorClipboard.paste(editor, cb, (RMParentShape)aShape, anEvent.getPoint());
    }
}

/**
 * An inner class to provide data for keys browser.
 */
static class KeysBrowserResolver extends TreeResolver <KeyNode> {
    
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
private class KeyNode {

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