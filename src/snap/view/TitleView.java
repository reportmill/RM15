/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import snap.geom.*;
import snap.gfx.*;
import snap.util.*;

/**
 * A view to attach a title to another view.
 */
public class TitleView extends ParentView implements ViewHost {

    // The view that paints title view decorations
    private TitleArea  _area;
    
    // The title label
    private Label  _label;

    // The content
    private View  _content;
    
    // The Style
    private TitleStyle  _tstyle;
    
    // Whether Title view is collapsible
    private boolean  _collapsible;
    
    // Whether Title view is expanded
    private boolean  _expanded = true;
    
    // Images for collapsed/expanded
    private View  _clpView;
    
    // Constants for TitleView styles
    public enum TitleStyle { Plain, EtchBorder, Button }
    
    // Constants for properties
    public static final String Collapsible_Prop = "Collapsible";
    public static final String Expanded_Prop = "Expanded";
    public static final String TitleStyle_Prop = "TitleStyle";

    // Constants for property defaults
    private static Insets DEFAULT_TITLE_VIEW_PADDING = new Insets(2);

    /**
     * Creates a new TitleView.
     */
    public TitleView()
    {
        // Create/add TitleArea
        setTitleStyle(TitleStyle.EtchBorder);

        // Configure TitleView
        setPadding(2,2,2,2);
    }

    /**
     * Creates a new TitleView for given title.
     */
    public TitleView(String aTitle)
    {
        this();
        setText(aTitle);
    }

    /**
     * Creates a new TitleView for given title.
     */
    public TitleView(String aTitle, View aView)
    {
        this();
        setText(aTitle);
        setContent(aView);
    }

    /**
     * Returns the label.
     */
    public Label getLabel()  { return _label; }

    /**
     * Override to get from label.
     */
    public String getText()  { return _label.getText(); }

    /**
     * Override to set to label.
     */
    public void setText(String aTitle)  { _label.setText(aTitle); }

    /**
     * Returns the content.
     */
    public View getContent()  { return _content; }

    /**
     * Sets the content.
     */
    public void setContent(View aView)
    {
        View old = _content; if(aView == old) return;
        if (_content != null)
            removeChild(_content);
        _content = aView;
        if (_content != null)
            addChild(_content);
    }

    /**
     * Returns the title style.
     */
    public TitleStyle getTitleStyle()  { return _tstyle; }

    /**
     * Sets the title style.
     */
    public void setTitleStyle(TitleStyle aTS)
    {
        // If already set, just return
        if (aTS == _tstyle) return;

        // Set value and fire prop change
        firePropChange(TitleStyle_Prop, _tstyle, _tstyle = aTS);

        // Relayout, repaint
        relayout();
        relayoutParent();
        repaint();

        // If Area needs to change, change it
        TitleArea area = createArea();
        setArea(area);
    }

    /**
     * Returns the TitleArea.
     */
    protected TitleArea getArea()  { return _area; }

    /**
     * Sets the TitleArea.
     */
    protected void setArea(TitleArea aTA)
    {
        // Remove old
        String text = null;
        if (_area!=null) {
            text = getText();
            removeChild(_area);
        }

        // Set/add new
        _area = aTA;
        _area.setTitleView(this);
        _area.setPadding(getPadding());
        addChild(_area, 0);
        _label = _area._label;
        if (text!=null) _label.setText(text);
    }

    /**
     * Creates the Area.
     */
    protected TitleArea createArea()
    {
        switch (getTitleStyle()) {
            case EtchBorder: return new TitleAreaEtched();
            case Button: return new TitleAreaButton();
            default: return new TitleAreaPlain();
        }
    }

    /**
     * Returns whether title view is collapsible.
     */
    public boolean isCollapsible()  { return _collapsible; }

    /**
     * Sets whether title view is collapsible.
     */
    public void setCollapsible(boolean aValue)
    {
        // Do normal setter
        if(aValue==_collapsible) return;
        firePropChange(Collapsible_Prop, _collapsible, _collapsible=aValue);
        setActionable(aValue);

        // If collapsible: Enable action event and listen for label click
        if (aValue) {
            View graphic = getCollapseGraphic();
            _label.setGraphic(graphic);
            graphic.setRotate(90);
        }

        // If not collapsible: Disable action event and stop listen for lable click
        else {
            _label.setGraphic(null);
            setExpanded(true);
        }
    }

    /**
     * Returns whether title view is expanded.
     */
    public boolean isExpanded()  { return _expanded; }

    /**
     * Sets whether title view is expanded.
     */
    public void setExpanded(boolean aValue)
    {
        // If value already set, just return
        if(aValue==_expanded) return;

        // Set value and fire prop change
        firePropChange(Expanded_Prop, _expanded, _expanded=aValue);
        relayout();

        // Update graphic
        View graphic = _label.getGraphic();
        if (graphic == null)
            return;
        graphic.setRotate(aValue? 90 : 0);
    }

    /**
     * Sets the expanded animated.
     */
    public void setExpandedAnimated(boolean aValue)
    {
        // If already set, just return
        if(aValue == _expanded) return;

        // Cache current size and set new Expanded value
        double viewW = getWidth();
        double viewH = getHeight();
        setExpanded(aValue);

        // Reset/get new PrefSize
        setPrefSize(-1,-1);
        double prefW = getPrefWidth();
        double prefH = getPrefHeight();

        // Set pref size to current size and expanded to true (for duration of anim)
        setPrefSize(viewW,viewH);
        setExpanded(true);

        // Clip content to bounds?
        View content = getContent();
        if(content != null)
            content.setClipToBounds(true);

        // Configure anim to new size
        ViewAnim anim = getAnim(0).clear();
        anim.getAnim(500).setPrefSize(prefW, prefH).setOnFinish(() -> setExpandedAnimDone(aValue)).needsFinish().play();

        // Get graphic and set initial anim rotate
        View graphic = _label.getGraphic();
        if (graphic == null)
            return;
        graphic.setRotate(aValue? 0 : 90);

        // Configure anim for graphic
        anim = graphic.getAnim(0).clear();
        if (aValue)
            anim.getAnim(500).setRotate(90).play();
        else anim.getAnim(500).setRotate(0).play();
    }

    /**
     * Called when setExpanded animation is done.
     */
    private void setExpandedAnimDone(boolean aValue)
    {
        setExpanded(aValue);
        setPrefSize(-1, -1);
    }

    /**
     * Called when Label receives a MousePress.
     */
    protected void toggleExpandedAnimated(ViewEvent anEvent)
    {
        fireActionEvent(anEvent);
        setExpandedAnimated(!isExpanded());
    }

    /**
     * ViewHost method: Override to return 1 if content is present.
     */
    public int getGuestCount()  { return getContent() != null ? 1 : 0; }

    /**
     * ViewHost method: Override to return content (and complain if index beyond 0).
     */
    public View getGuest(int anIndex)
    {
        if (anIndex > 0)
            throw new IndexOutOfBoundsException("Index " + anIndex + " beyond 0");
        return getContent();
    }

    /**
     * ViewHost method: Override to set content.
     */
    public void addGuest(View aChild, int anIndex)
    {
        setContent(aChild);
    }

    /**
     * ViewHost method: Override to clear content (and complain if index beyond 0).
     */
    public View removeGuest(int anIndex)
    {
        if (anIndex > 0)
            throw new IndexOutOfBoundsException("Index " + anIndex + " beyond 0");
        View cont = getContent();
        setContent(null);
        return cont;
    }

    /**
     * Returns whether content is showing.
     */
    public boolean isContentShowing()
    {
        return _content != null && (!isCollapsible() || isExpanded());
    }

    /**
     * Override to return preferred width of content.
     */
    protected double getPrefWidthImpl(double aH)  { return _area.getPrefWidth(aH); }

    /**
     * Override to return preferred height of content.
     */
    protected double getPrefHeightImpl(double aW)  { return _area.getPrefHeight(aW); }

    /**
     * Override to layout content.
     */
    protected void layoutImpl()
    {
        // Layout TitleArea to full bounds
        _area.setBounds(0, 0, getWidth(), getHeight());

        // Layout content
        Rect contentBounds = _area.getContentBounds();
        if (isContentShowing())
            _content.setBounds(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);
        else if (_content != null) {
            Insets ins = getInsetsAll();
            _content.setBounds(ins.left, ins.top, 0, 0);
        }
    }

    /**
     * Returns an image of a down arrow.
     */
    public View getCollapseGraphic()
    {
        // If already set, just return
        if (_clpView != null) return _clpView;

        // Create
        Polygon poly = new Polygon(2.5, .5, 2.5, 8.5, 8.5, 4.5);
        ShapeView shapeView = new ShapeView(poly);
        shapeView.setPrefSize(9,9);
        shapeView.setFill(Color.GRAY);
        shapeView.setBorder(Color.GRAY, 1);

        // Set, return
        return _clpView = shapeView;
    }

    /**
     * Override to forward to area.
     */
    public void setPadding(Insets theIns)
    {
        super.setPadding(theIns);
        _area.setPadding(theIns);
    }

    /**
     * Override to handle additional properties.
     */
    public Object getPropValue(String aPropName)
    {
        if (aPropName.equals("Value") || aPropName == Expanded_Prop)
            return isExpanded();
        return super.getPropValue(aPropName);
    }

    /**
     * Override to handle additional properties.
     */
    public void setPropValue(String aPropName, Object aValue)
    {
        if (aPropName.equals("Value") || aPropName == Expanded_Prop)
            setExpanded(Convert.boolValue(aValue));
        else super.setPropValue(aPropName, aValue);
    }

    /**
     * Returns a mapped property name name.
     */
    protected String getValuePropName()  { return Expanded_Prop; }

    /**
     * Override for custom defaults.
     */
    @Override
    public Object getPropDefault(String aPropName)
    {
        // Padding
        if (aPropName == Padding_Prop)
            return DEFAULT_TITLE_VIEW_PADDING;

        // Do normal version
        return super.getPropDefault(aPropName);
    }

    /**
     * XML archival.
     */
    protected XMLElement toXMLView(XMLArchiver anArchiver)
    {
        // Do normal archival
        XMLElement e = super.toXMLView(anArchiver);

        // Archive Text, TitleStyle
        String text = getText(); if (text!=null && text.length()>0) e.add(Text_Prop, text);
        if (getTitleStyle()!=TitleStyle.EtchBorder) e.add(TitleStyle_Prop, getTitleStyle());

        // Archive Expandable, Expanded
        if (isCollapsible()) e.add(Collapsible_Prop, true);
        if (isCollapsible() && !isExpanded()) e.add(Expanded_Prop, false);
        return e;
    }

    /**
     * XML unarchival.
     */
    protected void fromXMLView(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Do normal version
        super.fromXMLView(anArchiver,anElement);

        // Unarchive Title (legacy)
        if (anElement.hasAttribute(Text_Prop)) setText(anElement.getAttributeValue(Text_Prop));
        if (anElement.hasAttribute("Title")) setText(anElement.getAttributeValue("Title"));

        // Unrchive TitleStyle
        String tstr = anElement.getAttributeValue(TitleStyle_Prop);
        TitleStyle tstyl = tstr!=null? TitleStyle.valueOf(tstr) : null;
        if (tstyl!=null) setTitleStyle(tstyl);

        // Unrchive Expandable, Expanded
        if (anElement.hasAttribute(Collapsible_Prop)) setCollapsible(anElement.getAttributeBoolValue(Collapsible_Prop));
        if (anElement.hasAttribute(Expanded_Prop)) setExpanded(anElement.getAttributeBoolValue(Expanded_Prop));
    }

    /**
     * A View to draw content of TitleView.
     */
    public static class TitleArea extends ParentView {

        // The TitleView
        TitleView     _titleView;

        // The label
        Label         _label;

        /** Sets the TitleView. */
        public void setTitleView(TitleView aTV)  { _titleView = aTV; }

        /** Returns the content. */
        public View getContent()  { return _titleView._content; }

        /** Returns whether content is visible. */
        public boolean isContentShowing()  { return _titleView.isContentShowing(); }

        /** Override to return preferred width of TitleArea. */
        protected double getPrefWidthImpl(double aH)
        {
            // Get max of Content.PrefWidth and TitleArea.PrefWidth
            double prefWidth = _label.getPrefWidth();
            View content = getContent();
            if (isContentShowing())
                prefWidth = Math.max(prefWidth, content.getPrefWidth());

            // Get insets and return Content.PrefWidth + insets width
            Insets ins = getInsetsAll();
            return prefWidth + ins.getWidth();
        }

        /** Override to return preferred height of TitleArea. */
        protected double getPrefHeightImpl(double aW)
        {
            // Get combined of Content.PrefHeight and Label.PrefHeight
            double prefH = _label.getPrefHeight();
            View content = getContent();
            if (isContentShowing())
                prefH += content.getPrefHeight();

            // Get insets and return Content.PrefHeight + insets height
            Insets ins = getInsetsAll();
            return prefH + ins.getHeight();
        }

        /** Override to layout TitleArea. */
        protected void layoutImpl()
        {
            // Layout label
            double labelW = _label.getPrefWidth();
            double labelH = _label.getPrefHeight();
            _label.setBounds(0, 0, labelW, labelH);
        }

        /** Returns the content bounds. */
        public Rect getContentBounds()
        {
            // Get inset bounds and label height
            Insets ins = getInsetsAll();
            double areaX = ins.left;
            double areaY = ins.top;
            double areaW = getWidth() - ins.getWidth();
            double areaH = getHeight() - ins.getHeight();
            double labelH = _label.getPrefHeight();

            // Return rect
            return new Rect(areaX, areaY + labelH - 1, areaW, areaH - labelH + 1);
        }

        /** Triggers. */
        protected void fireActionEvent(ViewEvent anEvent)
        {
            //super.fireActionEvent(anEvent);
            if(_titleView.isCollapsible())
                _titleView.toggleExpandedAnimated(anEvent);
        }
    }

    /**
     * A TitleArea subclass to just create Label.
     */
    private static class TitleAreaPlain extends TitleArea {

        /** Create TitleAreaPlain. */
        public TitleAreaPlain()
        {
            // Create/configure label (for EtchBorder)
            _label = new Label();
            addChild(_label);

            // Listen for Label MousePress to trigger expand
            _label.addEventHandler(e -> fireActionEvent(e), MousePress);
        }
    }

    /**
     * A TitleArea subclass to display label inside Etched border.
     */
    private static class TitleAreaEtched extends TitleAreaPlain {

        /** Create TitleAreaEtched. */
        public TitleAreaEtched()
        {
            _label.setPadding(0,0,0,10);
        }

        /** Override to paint title and stroke. */
        protected void paintFront(Painter aPntr)
        {
            // Get TitleView bounds
            double w = getWidth();
            double h = getHeight();
            double sw = _label.getMaxX();
            double sh = _label.getHeight();

            // Create path for border
            double x1 = 5 + sw;
            double y1 = sh / 2;
            Path2D path = new Path2D();
            path.moveTo(x1, y1);
            path.lineTo(w - 2, y1);
            path.lineTo(w - 2, h - 2);
            path.lineTo(.5, h - 2);
            path.lineTo(.5, y1);
            path.lineTo(5, y1);

            // Paint path once in white and
            aPntr.translate(1,1);
            aPntr.setPaint(Color.WHITE);
            aPntr.setStroke(Stroke.Stroke1);
            aPntr.draw(path);

            // Paint again in gray
            aPntr.translate(-1,-1);
            aPntr.setPaint(Color.LIGHTGRAY);
            aPntr.draw(path);
        }
    }

    /**
     * A TitleArea subclass to display as button.
     */
    private static class TitleAreaButton extends TitleArea {

        // The Button
        Button      _button;

        /** Create TitleAreaButton. */
        public TitleAreaButton()
        {
            // Create/configure button
            _button = new Button();
            _button.setPadding(3,3,3,3);
            _button.setAlignX(HPos.LEFT);
            _button.setBorderRadius(0);
            addChild(_button);

            // Listen for Button click to trigger expand
            _button.addEventHandler(e -> fireActionEvent(e), Action);

            // Get label
            _label = _button.getLabel();
        }

        /** Override to return preferred width of TitleArea. */
        protected double getPrefWidthImpl(double aH)
        {
            // Get Button.PrefWidth and Content.PrefWidth
            double buttonW = _button.getPrefWidth();
            double contentW = 0;
            if(isContentShowing()) {
                View content = getContent();
                Insets ins = getInsetsAll();
                contentW = content.getPrefWidth() + ins.getWidth();
            }

            // Return max of Button.PrefWidth and Content.PrefWidth
            return Math.max(buttonW, contentW);
        }

        /** Override to return preferred height of TitleArea. */
        protected double getPrefHeightImpl(double aW)
        {
            // Get Button.PrefWidth and Content.PrefWidth
            double buttonH = _button.getPrefHeight();
            double contentH = 0;
            if(isContentShowing()) {
                View content = getContent();
                Insets ins = getInsetsAll();
                contentH = content.getPrefHeight() + ins.getHeight();
            }

            // Return combined Button.PrefWidth and Content.PrefWidth
            return buttonH + contentH;
        }

        /** Override to layout TitleArea. */
        protected void layoutImpl()
        {
            // Layout Button
            double bw = getWidth();
            double bh = _button.getPrefHeight();
            _button.setBounds(0, 0, bw, bh);
        }

        /** Returns the content bounds. */
        public Rect getContentBounds()
        {
            // Get inset bounds and label height
            Insets ins = getInsetsAll();
            double areaX = ins.left;
            double areaY = ins.top;
            double areaW = getWidth() - ins.getWidth();
            double areaH = getHeight() - ins.getHeight();
            double buttonH = _button.getPrefHeight();

            // Return rect
            return new Rect(areaX, areaY + buttonH, areaW, areaH - buttonH);
        }
    }
}