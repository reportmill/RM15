package com.reportmill.app;
import snap.view.*;
import snap.gfx.*;

/**
 * A View that slides in/out of another view.
 */
public class DrawerView extends ParentView {
    
    // The Content view
    View         _content;
    
    // The button tab used to trigger drawer
    Button       _tabButton;

    // The label on the tab button
    Label        _tabLabel;
    
    // The label at the top of drawer
    Label        _drawerLabel;
    
    // The CloseBox
    View         _closeBox;

/**
 * Creates DrawerView.
 */
public DrawerView(View aView)
{
    // Configure basic attributes
    setPadding(30,10,10,10);
    setFill(ViewUtils.getBackFill());
    setBorder(Color.DARKGRAY, 1);
    setManaged(false);
    setLean(Pos.CENTER_RIGHT);
    
    // Configure action
    enableEvents(MouseRelease, MouseEnter, MouseExit, MouseMove);
    
    // Add DrawerLabel
    addChild(getDrawerLabel());
    
    // Add Close box
    addChild(getCloseBox());
    
    // Set content
    setContent(aView);
}

/**
 * Returns the box content.
 */
public View getContent()  { return _content; }

/**
 * Sets the box content.
 */
public void setContent(View aView)
{
    // If already set, just return
    if(aView==getContent()) return;
    
    // Remove old content
    if(_content!=null) removeChild(_content);
    
    // Add new Content in box
    BoxView box = new BoxView(aView);
    box.setPadding(1,1,1,1);
    box.setBorder(Border.createLoweredBevelBorder());
    _content = box;

    // Add new COntent
    if(_content!=null) addChild(_content);
}

/**
 * Returns the tab button.
 */
public Button getTabButton()
{
    // If already set, return
    if(_tabButton!=null) return _tabButton;
    
    // Create/configure TabButton
    Button btn = new Button();
    btn.setSize(22,88);
    btn.setManaged(false);
    btn.setLean(Pos.CENTER_RIGHT);
    btn.setPosition(Pos.CENTER_LEFT);
    
    // Add tab label
    Label tlabel = getTabLabel();
    ViewUtils.addChild(btn, tlabel);
    
    // Add EventHandler to call show when clicked
    btn.addEventHandler(e -> show(), View.Action);
    
    // Return button
    return _tabButton = btn;
}

/**
 * Returns the tab label.
 */
public Label getTabLabel()
{
    // If already set, just return
    if(_tabLabel!=null) return _tabLabel;
    
    // Create/configure
    Label label = new Label();
    label.setFont(Font.Arial12.getBold());
    label.setTextFill(Color.DARKGRAY);
    label.setManaged(false);
    label.setLean(Pos.CENTER);
    label.setRotate(-90);
    label.addPropChangeListener(pc -> label.setSize(label.getPrefSize()));
    return _tabLabel = label;
}

/**
 * Returns the drawer label.
 */
public Label getDrawerLabel()
{
    // If already set, just return
    if(_drawerLabel!=null) return _drawerLabel;
    
    // Create/configure
    Label label = new Label();
    label.setPadding(6,0,0,0);
    label.setFont(Font.Arial12.getBold());
    label.setTextFill(Color.GRAY);
    label.setManaged(false);
    label.setLean(Pos.TOP_CENTER);
    label.addPropChangeListener(pc -> label.setSize(label.getPrefSize()));
    return _drawerLabel = label;
}

/**
 * Returns the close box view.
 */
protected View getCloseBox()
{
    // If already set, just return
    if(_closeBox!=null) return _closeBox;
    
    Polygon poly = new Polygon(0,2,2,0,5,3,8,0,10,2,7,5,10,8,8,10,5,7,2,10,0,8,3,5);
    ShapeView cbox = new ShapeView(poly);
    cbox.setManaged(false);
    cbox.setLean(Pos.TOP_RIGHT);
    cbox.setBorder(Color.BLACK, .5);
    cbox.setMargin(5,8,0,0);
    cbox.setPrefSize(11,11);
    return _closeBox = cbox;
}

/**
 * Sets the CloseBox highlight.
 */
protected void setCloseBoxHighlight(boolean aValue)
{
    View cbox = getCloseBox();
    cbox.setFill(aValue? Color.PINK : null);
    cbox.setBorder(Color.BLACK, aValue? 1 : .5);
}

/**
 * Shows the tab button in given view.
 */
public void showTabButton(ParentView aView)
{
    View btn = getTabButton();
    ViewUtils.addChild(aView, btn);
}

/**
 * Returns the view to attach to.
 */
public ParentView getAttachView()
{
    return getTabButton().getParent();
}

/**
 * Shows the drawer.
 */
public void show()
{
    // If already showing, just return
    if(isShowing()) return;
    
    // Resize to view
    Size size = getPrefSize();
    setSize(size);
    setOpacity(1);

    // Get attach view and add this DrawerView
    ParentView parView = getAttachView();
    parView.setClipToBounds(true);
    ViewUtils.addChild(parView, this);
    
    // Start animate in
    setTransX(size.width);
    getAnim(800).clear().setTransX(1).play();
}

/**
 * Hides the drawer.
 */
public void hide()
{
    // If hidden, just return
    if(!isShowing()) return;
    
    // Animate out
    getAnim(800).clear().setTransX(getWidth()).setOnFinish(a -> hideDrawerDone()).play();
}

/**
 * Cleanup when hideDrawer animation done.
 */
protected void hideDrawerDone()
{
    ParentView parView = getAttachView();
    ViewUtils.removeChild(parView, this);
}

/**
 * Shows/Hides DrawerView.
 */
public void toggleDrawer()
{
    if(isShowing()) hide();
    else show();
}

/**
 * ProcessEvent.
 */
protected void processEvent(ViewEvent anEvent)
{
    // Handle MouseRelease
    if(anEvent.isMouseRelease()) {
        
        // If click was inside content, just return
        if(!inMargin(anEvent)) return;
        
        // Toggle drawer
        toggleDrawer();
    }
    
    // Handle MouseEnter, MouseExit, MouseMove
    else if(anEvent.isMouseEnter() || anEvent.isMouseMove())
        setCloseBoxHighlight(inMargin(anEvent));
    
    // Handle MouseExit
    else if(anEvent.isMouseExit())
        setCloseBoxHighlight(false);
}

// Returns whether event point is in margin.
private boolean inMargin(ViewEvent anEvent)
{
    Rect contentRect = getBoundsLocal().getInsetRect(getInsetsAll());
    boolean inContent = contentRect.contains(anEvent.getPoint());
    return !inContent;
}

/**
 * Override to return preferred width of content.
 */
protected double getPrefWidthImpl(double aH)  { return BoxView.getPrefWidth(this, getContent(), aH); }

/**
 * Override to return preferred height of content.
 */
protected double getPrefHeightImpl(double aW)  { return BoxView.getPrefHeight(this, getContent(), aW); }

/**
 * Override to layout content.
 */
protected void layoutImpl()  { BoxView.layout(this, getContent(), null, true, true); }

}