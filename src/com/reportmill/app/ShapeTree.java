package com.reportmill.app;
import com.reportmill.shape.*;
import snap.gfx.Color;
import snap.view.*;

/**
 * An inspector to show ShapeTree.
 */
public class ShapeTree extends RMEditorPane.SupportPane {
    
    // The ShapeTree
    TreeView       _shapeTree;

/**
 * Creates a new ShapeFills pane.
 */
public ShapeTree(RMEditorPane anEP)  { super(anEP); }

/**
 * Returns the ViewTree.
 */
protected View createUI()
{
    if(_shapeTree!=null) return _shapeTree;
    TreeView tview = new TreeView();
    tview.setName("ShapeTree");
    tview.setGrowHeight(true);
    tview.setBorder(Color.GRAY, 1);
    tview.getScrollView().setBarSize(14);
    tview.setResolver(new ShapeTreeResolver());
    return _shapeTree = tview;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    enableEvents(_shapeTree, MouseRelease);
}

/**
 * Reset UI.
 */
protected void resetUI()
{
    _shapeTree.setItems(getEditor().getDoc());
    _shapeTree.expandAll();
    _shapeTree.setSelItem(getEditor().getSelectedOrSuperSelectedShape());
}

/**
 * Respond UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle ShapeTree
    if(anEvent.equals("ShapeTree") && anEvent.isActionEvent())
        getEditor().setSelectedShape((RMShape)anEvent.getSelItem());
        
    // Handle MouseClick
    if(anEvent.isMouseClick() && anEvent.getClickCount()==2)
        getEditor().setSuperSelectedShape((RMShape)anEvent.getSelItem());
}

/**
 * A TreeResolver for Document Shapes.
 */
public class ShapeTreeResolver extends TreeResolver <RMShape> {
    
    /** Returns the parent of given item. */
    public RMShape getParent(RMShape anItem)  { return anItem!=getEditor().getDoc()? anItem.getParent() : null; }

    /** Whether given object is a parent (has children). */
    public boolean isParent(RMShape anItem)
    {
        if(!(anItem instanceof RMParentShape)) return false;
        return ((RMParentShape)anItem).getChildCount()>0;
    }

    /** Returns the children. */
    public RMShape[] getChildren(RMShape aParent)
    {
        RMParentShape par = (RMParentShape)aParent;
        return par.getChildArray();
    }

    /** Returns the text to be used for given item. */
    public String getText(RMShape anItem)
    {
        String str = anItem.getClass().getSimpleName(); if(str.startsWith("RM")) str = str.substring(2);
        String name = anItem.getName(); if(name!=null) str += " - " + name;
        if(anItem instanceof RMTextShape) { RMTextShape ts = (RMTextShape)anItem;
            String text = ts.getText(); if(text!=null) str += " \"" + text + "\" "; }
        if(anItem instanceof RMTableRow) { RMTableRow tr = (RMTableRow)anItem;
            str += " - " + tr.getTitle(); }
        return str;
    }

    /** Return the image to be used for given item. */
    public View getGraphic(RMShape anItem)  { return null; }
}

}