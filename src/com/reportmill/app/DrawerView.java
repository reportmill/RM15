package com.reportmill.app;
import snap.gfx.*;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.Explode;

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
    
    // The round rect used for bounds shape
    RoundRect    _roundRect;
    
    // Dedicated anim so nothing interferes with drawer open/close
    ViewAnim     _slideAnim;
    
    // Whether drawer is currently hiding
    boolean      _hiding;
    
    // The size of the draw last sized
    Rect         _minBnds;
    
    // Constants
    public static final Effect SHADOW_EFFECT = new ShadowEffect(10, Color.GRAY, 0, 0);

/**
 * Creates DrawerView.
 */
public DrawerView(View aView)
{
    // Configure basic attributes
    setPadding(30,10,10,10);
    setFill(ViewUtils.getBackFill());
    setBorder(Color.GRAY, 1);
    setManaged(false);
    setLean(Pos.TOP_RIGHT);
    
    // Configure action
    enableEvents(MousePress, MouseDrag, MouseRelease, MouseEnter, MouseExit, MouseMove);
    
    // Add DrawerLabel
    addChild(getDrawerLabel());
    
    // Add Close box
    addChild(getCloseBox());
    
    // Set content
    setContent(aView);
    
    // Set Effect
    setEffect(SHADOW_EFFECT);
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
    BoxView box = new BoxView(aView, true, true);
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
    btn.addEventHandler(e -> toggleDrawer(), View.Action);
    
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
    size.width = Math.max(size.width, getWidth());
    size.height = Math.max(size.height, getHeight());
    setSize(size);
    setOpacity(1);

    // Get attach view and add this DrawerView
    ParentView parView = getAttachView();
    parView.setClipToBounds(true);
    ViewUtils.addChild(parView, this);
    
    // Adjust DrawerY if needed
    if(getMargin().top==0 || getMargin().top+getHeight()/2>parView.getHeight())
        setDrawerY(-1);
    
    // Start animate in
    setTransX(size.width);
    if(_slideAnim==null) _slideAnim = new ViewAnim(this);
    _slideAnim.clear().getAnim(800).setTransX(1).play();
}

/**
 * Hides the drawer.
 */
public void hide()
{
    // If hidden, just return
    if(!isShowing() || _hiding) return;
    
    // Animate out
    _hiding = true;
    _slideAnim.clear().getAnim(800).setTransX(getWidth()).setOnFinish(() -> hideDrawerDone()).play();
}

/**
 * Hides the drawer.
 */
public void explode()
{
    // If hidden, just return
    if(!isShowing() || _hiding) return;
    
    // Animate out
    _hiding = true;
    Explode exp = new Explode(this, 25, 25, () -> hideDrawerDone());
    exp.setHostView(getParent().getParent());
    exp.setRunTime(1400);
    exp.play();
}

/**
 * Cleanup when hideDrawer animation done.
 */
protected void hideDrawerDone()
{
    _hiding = false;
    ParentView parView = getAttachView();
    ViewUtils.removeChild(parView, this);
    setOpacity(1);
    setMaximized(false);
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
 * Returns whether drawer is maximized.
 */
public boolean isMaximized()  { return _minBnds!=null; }

/**
 * Resize the drawer to maximum size for view.
 */
public void setMaximized(boolean aValue)
{
    // If already set, just return
    if(aValue==isMaximized()) return;
    
    // If setting, grow window
    if(aValue) {
        double pw = getParent().getWidth();
        double ph = getParent().getHeight();
        double nw = pw - 12;
        double nh = ph - 24; _minBnds = getBounds();
        _slideAnim.clear().getAnim(400).setWidth(nw).setHeight(nh).setValue("DrawerY", 12).play();
    }
    
    // Otherwise, shrink size
    else {
        double ny = _minBnds.y;
        double nw = _minBnds.width;
        double nh = _minBnds.height; _minBnds = null;
        if(!isShowing()) { setSize(nw, nh); setDrawerY(ny); }
        else _slideAnim.clear().getAnim(400).setWidth(nw).setHeight(nh).setValue("DrawerY", ny).play();
    }
}

/**
 * ProcessEvent.
 */
protected void processEvent(ViewEvent anEvent)
{
    // Handle MousePress
    if(anEvent.isMousePress()) {
        
        // Clear MouseDownPoint and if not in margin, just bail
        _mouseDownPnt = null;
        if(!inMargin(anEvent)) return;
        
        // Set MouseDown vars
        _mouseDownPnt = anEvent.getPoint(getParent());
        _mouseDownY = getMargin().top; _mouseDownW = getWidth(); _mouseDownH = getHeight();
        _mouseDragged = false;
        _msDwnResize = inResize(anEvent);
        _msDwnResizeTop = inResizeTop(anEvent);
    }

    // Handle MouseDrag
    else if(anEvent.isMouseDrag()) {
        
        // If no MouseDownPoint, bail
        if(_mouseDownPnt==null) return;
        
        // Get new point and change
        Point pnt = anEvent.getPoint(getParent());
        double dx = pnt.x - _mouseDownPnt.x;
        double dy = pnt.y - _mouseDownPnt.y;
        
        // Either resize or reposition
        if(_msDwnResize) setDrawerSize(_mouseDownW - dx, _mouseDownH + dy);
        else if(_msDwnResizeTop) {
            setDrawerSize(_mouseDownW - dx, _mouseDownH - dy); setDrawerY(_mouseDownY + dy); }
        else setDrawerY(Math.max(_mouseDownY + dy, 0));
        
        // If significant change, set MouseDragged
        if(Math.abs(dx)>2 || Math.abs(dy)>2) _mouseDragged = true;
    }
    
    // Handle MouseRelease
    if(anEvent.isMouseRelease()) {
        
        // Clear MouseDownPoint
        _mouseDownPnt = null;
        
        // If click was inside content, just return
        if(_mouseDragged || !inMargin(anEvent)) return;
        
        // Toggle drawer
        if(anEvent.getX()>getWidth()-14) explode();
        else toggleDrawer();
    }
    
    // Handle MouseEnter, MouseExit, MouseMove
    else if(anEvent.isMouseEnter() || anEvent.isMouseMove())
        setCloseBoxHighlight(inMargin(anEvent));
    
    // Handle MouseExit
    else if(anEvent.isMouseExit())
        setCloseBoxHighlight(false);
}

// Some drag vars
private boolean _mouseDragged; Point _mouseDownPnt;
private double _mouseDownY, _mouseDownW, _mouseDownH;
private boolean _msDwnResize, _msDwnResizeTop;

/**
 * Sets the drawer Y relative to parent.
 */
private void setDrawerY(double aY)
{
    // Get Y value (if less than zero, adjust to place drawer in middle of parent)
    double y = aY;
    if(y<0) y = Math.round((getParent().getHeight() - getHeight())*2/3);
    
    // Get margin, adjust and update (just return if already at Y)
    Insets margin = getMargin().clone(); if(margin.top==y) return;
    margin.top = y;
    setMargin(margin);
}

/**
 * Sets the drawer Y relative to parent.
 */
private void setDrawerSize(double aW, double aH)
{
    Size size = getPrefSize();
    size.width = Math.max(size.width, aW);
    size.height = Math.max(size.height, aH);
    setSize(size);
    relayoutParent();
}

// Returns whether event point is in margin.
private boolean inMargin(ViewEvent anEvent)
{
    Rect contentRect = getBoundsLocal().getInsetRect(getInsetsAll());
    boolean inContent = contentRect.contains(anEvent.getPoint());
    return !inContent;
}

// Returns whether event point is bottom corner.
private boolean inResize(ViewEvent anEvent)
{
    Insets ins = getInsetsAll();
    Rect bnds = getBoundsLocal();
    bnds.setRect(bnds.x, bnds.height - ins.bottom, ins.left, ins.bottom);
    boolean inResize = bnds.contains(anEvent.getPoint());
    return inResize;
}

// Returns whether event point is bottom corner.
private boolean inResizeTop(ViewEvent anEvent)
{
    Insets ins = getInsetsAll();
    Rect bnds = getBoundsLocal();
    bnds.setRect(bnds.x, 0, ins.left, ins.top);
    boolean inResize = bnds.contains(anEvent.getPoint());
    return inResize;
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

/**
 * Override to handle DrawerY.
 */
public Object getValue(String aPropName)
{
    if(aPropName=="DrawerY") return getMargin().top;
    return super.getValue(aPropName);
}

/**
 * Override to handle DrawerY.
 */
public void setValue(String aPropName, Object aValue)
{
    if(aPropName=="DrawerY") setDrawerY(SnapUtils.doubleValue(aValue));
    else super.setValue(aPropName, aValue);
}

/**
 * Override to handle rounding radius.
 */
public Shape getBoundsShape()
{
    if(_roundRect!=null && (_roundRect.width!=getWidth() || _roundRect.height!=getHeight())) _roundRect = null;
    if(_roundRect!=null) return _roundRect;
    RoundRect rrect = new RoundRect(0, 0, getWidth(), getHeight(), 5);
    rrect = rrect.copyForCorners(true, false, false, true);
    return _roundRect = rrect;
}

}