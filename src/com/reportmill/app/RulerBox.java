package com.reportmill.app;
import snap.gfx.*;
import snap.util.MathUtils;
import snap.view.*;
import com.reportmill.shape.*;

/**
 * A View subclass to add rulers to a given content view.
 * Currently assumes that view is RMEditor is ScrollView.
 */
public class RulerBox extends ParentView {
    
    // The content view
    View         _content;

    // The horizontal/vertical ruler views
    RulerView    _hruler, _vruler;
    
/**
 * Returns the box content.
 */
public View getContent()  { return _content; }

/**
 * Sets the box content.
 */
public void setContent(View aView)
{
    if(aView==_content) return;
    removeChildren();
    if(_content!=null) removeChild(_content);
    _content = aView;
    if(_content!=null) addChild(_content);
}

/**
 * Returns whether editor pane shows rulers.
 */
public boolean isShowRulers()  { return _hruler!=null; }

/**
 * Sets whether editor pane shows rulers.
 */
public void setShowRulers(boolean aValue)
{
    // If already set, just return
    if(aValue==isShowRulers()) return;
    
    // Determine if we should resize window after toggle (depending on whether window is at preferred size)
    WindowView win = getWindow();
    boolean doPack = win.getSize().equals(win.getPrefSize());
    
    // If no rulers, create and add them
    if(_hruler==null) {
        
        // Get Content for rulers
        View content = _content;
        if(content instanceof ScrollView)
            content = ((ScrollView)content).getContent();
        
        // Create rulers and add
        _hruler = new RulerView(content);
        _vruler = new RulerView(content);
        _vruler.setVertical(true);
        addChild(_hruler);
        addChild(_vruler);
    }
    
    // Otherwise, remove and clear them
    else {
        removeChild(_hruler);
        removeChild(_vruler);
        _hruler = _vruler = null;
    }
    
    // Resize window if window was previously at preferred size
    if(doPack)
        getWindow().pack();
}

/**
 * Sets the mouse point.
 */
public void setMousePoint(Point aPoint)
{
    if(_hruler!=null) _hruler.setMousePoint(aPoint);
    if(_vruler!=null) _vruler.setMousePoint(aPoint);
}
    
/**
 * Returns the preferred width.
 */
protected double getPrefWidthImpl(double aH)
{
    return BorderView.getPrefWidth(this, _content, _hruler, null, null, _vruler, aH);
}

/**
 * Returns the preferred height.
 */
protected double getPrefHeightImpl(double aW)
{
    return BorderView.getPrefHeight(this, _content, _hruler, null, null, _vruler, aW);
}

/**
 * Layout children.
 */
protected void layoutImpl()
{
    BorderView.layout(this, _content, _hruler, null, null, _vruler);
}

/**
 * A view to draw a ruler.
 */
private static class RulerView extends View {

    // The ruler editor and document
    RMEditor                  _editor;
    RMDocument                _doc;

    // Zoom Factor
    double                    _zoomFactor;
    
    // The mouse point
    Point                     _mouse = new Point();
    
    // Ruler constants
    private static final int          RULER_WIDTH = 20;
    private static final Font         RULER_FONT = Font.Arial10.deriveFont(8);
    private static final Stroke       MOUSE_STROKE = Stroke.Stroke1.copyForDashes(2, 2);
    
    /**
     * Creates a new editor ruler.
     */
    public RulerView(View aView)
    {
        // Set views
        _editor = (RMEditor)aView;
        _doc = _editor.getDoc();
        _zoomFactor = _editor.getZoomFactor();
        
        // Standard view attributes
        setFill(Color.LIGHTGRAY);
        setBorder(Color.BLACK, 1);
        setClipToBounds(true);
    }
    
    /**
     * Returns the size in points of a standard measure.
     */
    public int getUnitWidth()  { return (int)Math.round(72*_zoomFactor); }
            
    /** Override to control size. */
    protected double getPrefWidthImpl(double aH)
    {
        return isHorizontal()? _doc.getWidth() : RULER_WIDTH;
    }
    
    /** Override to control size. */
    protected double getPrefHeightImpl(double aW)
    {
        return isHorizontal()? RULER_WIDTH : _doc.getHeight();
    }
    
    /**
     * Sets the mouse point.
     */
    public void setMousePoint(Point aPoint)  { _mouse = aPoint; repaint(); }
    
    /**
     * Paint.
     */
    protected void paintFront(Painter aPntr)
    {
        if(isHorizontal()) paintHor(aPntr);
        else paintVer(aPntr);
    }
    
    /**
     * Paint Horizontal.
     */
    protected void paintHor(Painter aPntr)
    {
        // Get ruler bounds in doc coords
        Rect vrect = _editor.getVisRect();
        Rect bnds = new Rect(vrect.x - RULER_WIDTH, 0, getWidth(), RULER_WIDTH);
        bnds = _editor.convertToShape(bnds, _doc).getBounds();
        
        // Scale and translate ruler to doc coords
        aPntr.save();
        double scale = getWidth()/bnds.width;
        aPntr.scale(scale,1);
        aPntr.translate(-bnds.x, 0);
        
        // Get basic coords
        double usize = getUnitWidth();
        double x = MathUtils.floor(bnds.x, usize), maxx = bnds.getMaxX();
        double h = getHeight(), hh = h/2;
        double dx = usize, dxm = 9;
        
        // Get/set font and get font ascent
        Font font = RULER_FONT; aPntr.setFont(font);
        double fontAscent = font.getAscent();
        
        // Paint ticks/labels
        while(x<maxx) {
            String str = String.valueOf((int)Math.round(x));
            aPntr.drawString(str, x+2, fontAscent+2);
            aPntr.drawLine(x,0,x,h);
            for(double x2=x+dxm, mx2=x+dx/2-1; x2<mx2; x2+=dxm) aPntr.drawLine(x2,h,x2,h-5);
            double midx = x+dx/2; aPntr.drawLine(midx,h,midx,h-hh);
            for(double x2=midx+dxm, mx2=x+dx-1; x2<mx2; x2+=dxm) aPntr.drawLine(x2,h,x2,h-5);
            x += dx;
        }
        
        // Paint shape position
        Rect selBnds = getShapeBounds();
        if(selBnds!=null) {
            aPntr.setColor(new Color(1,.5)); aPntr.fillRect(selBnds.x, 0, selBnds.width, getHeight()); }
            
        // Paint mouse position
        Point mp = _editor.convertToShape(_mouse.x, _mouse.y, _doc);
        aPntr.setColor(Color.BLACK); aPntr.setStroke(MOUSE_STROKE);
        aPntr.drawLine(mp.getX(), 0, mp.getX(), getHeight());
        aPntr.restore();
    }
    
    /**
     * Paint Vertical.
     */
    protected void paintVer(Painter aPntr)
    {
        // Get ruler bounds in doc coords
        Rect vrect = _editor.getVisRect();
        Rect bnds = new Rect(0, vrect.y, RULER_WIDTH, getHeight());
        bnds = _editor.convertToShape(bnds, _doc).getBounds();
        
        // Scale ruler to doc coords
        aPntr.save();
        double scale = getHeight()/bnds.height;
        aPntr.scale(1,scale);
        aPntr.translate(0, -bnds.y);
        
        // Get basic coords
        double usize = getUnitWidth();
        double y = MathUtils.floor(bnds.y,usize), maxy = bnds.getMaxY();
        double w = getWidth(), hw = w/2;
        double dy = usize, dym = 9;
        
        // Get/set font and get font ascent
        Font font = RULER_FONT; aPntr.setFont(font);
        double fontAscent = font.getAscent();
        
        // Paint ticks/labels
        while(y<maxy) {
            String str = String.valueOf((int)Math.round(y));
            aPntr.drawString(str,2, y + fontAscent+2);
            aPntr.drawLine(0,y,w,y);
            for(double y2=y+dym, my2=y+dy/2-1; y2<my2; y2+=dym) aPntr.drawLine(w-5,y2,w,y2);
            double my = y+dy/2; aPntr.drawLine(w,my,w-hw,my);
            for(double y2=my+dym, my2=y+dy-1; y2<my2; y2+=dym) aPntr.drawLine(w,y2,w-5,y2);
            y += dy;
        }
        
        // Paint shape position
        Rect selBnds = getShapeBounds();
        if(selBnds!=null) {
            aPntr.setColor(new Color(1,.5)); aPntr.fillRect(0, selBnds.y, getWidth(), selBnds.height); }
            
        // Paint mouse position
        Point mp = _editor.convertToShape(_mouse.x, _mouse.y, _doc);
        aPntr.setColor(Color.BLACK); aPntr.setStroke(MOUSE_STROKE);
        aPntr.drawLine(0, mp.getY(), getWidth(), mp.getY());
        aPntr.restore();
    }
    
    /** Returns the current shape bounds. */
    private Rect getShapeBounds()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        if(shape==null || shape instanceof RMDocument || shape instanceof RMPage) return null;
        return shape.localToParent(shape.getBoundsInside(), null).getBounds();
    }
}

}