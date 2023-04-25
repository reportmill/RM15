/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import snap.geom.*;
import snap.gfx.*;
import snap.util.*;

import java.util.Objects;

/**
 * A class to display simple text with associcated image(s).
 */
public class Label extends ParentView {
    
    // The view to show text string
    protected StringView  _stringView;
    
    // The graphics view
    protected View  _graphic;
    
    // The graphics view after text
    private View  _graphicAfter;
    
    // The image name, if loaded from local resource
    private String  _imageName;
    
    // Whether label text is editable
    private boolean  _editable;
    
    // Whether label text is currently being edited
    private boolean  _editing;

    // A textfield for editing
    private TextField  _editor;
    
    // Constants for properties
    public static final String Editable_Prop = "Editable";
    public static final String Editing_Prop = "Editing";
    public static final String Graphic_Prop = "Graphic";
    public static final String GraphicAfter_Prop = "GraphicAfter";

    // Constants for property defaults
    private static final double DEFAULT_LABEL_SPACING = 4;

    /**
     * Creates a label node.
     */
    public Label()
    {
        super();
    }

    /**
     * Creates a label node with given text.
     */
    public Label(String aStr)
    {
        setText(aStr);
    }

    /**
     * Creates a label node with given graphic, text, and after graphic.
     */
    public Label(View aGrph, String aStr, View aGrphAfter)
    {
        setGraphic(aGrph);
        setText(aStr);
        setGraphicAfter(aGrphAfter);
    }

    /**
     * Returns the text.
     */
    public String getText()
    {
        return _stringView != null ? _stringView.getText() : null;
    }

    /**
     * Sets the text.
     */
    public void setText(String aValue)
    {
        // If value already set or setting null in label with no StringView, just return
        String oldVal = getText(); if (Objects.equals(aValue, oldVal)) return;
        if (aValue == null && !isStringViewSet())
            return;

        // Set value and fire prop change
        StringView sview = getStringView();
        sview.setText(aValue);
        sview.setVisible(aValue != null && aValue.length() > 0);
        firePropChange(Text_Prop, oldVal, aValue);
    }

    /**
     * Returns the image.
     */
    public Image getImage()
    {
        return _graphic instanceof ImageView ? ((ImageView)_graphic).getImage() : null;
    }

    /**
     * Sets the image.
     */
    public void setImage(Image anImage)
    {
        Image image = getImage(); if (anImage == image) return;
        if (_graphic instanceof ImageView)
            ((ImageView)_graphic).setImage(anImage);
        else setGraphic(new ImageView(anImage)); //firePropChange("Image", image, anImage); delete soon
    }

    /**
     * Returns the image after text.
     */
    public Image getImageAfter()
    {
        return _graphicAfter instanceof ImageView ? ((ImageView)_graphicAfter).getImage() : null;
    }

    /**
     * Sets the image after text.
     */
    public void setImageAfter(Image anImage)
    {
        Image image = getImage(); if (anImage == image) return;
        if (_graphicAfter instanceof ImageView)
            ((ImageView)_graphicAfter).setImage(anImage);
        else setGraphicAfter(new ImageView(anImage)); //firePropChange("Image", image, anImage); delete soon
    }

    /**
     * Returns the text fill.
     */
    public Paint getTextFill()
    {
        return _stringView != null ? _stringView.getTextFill() : null;
    }

    /**
     * Sets the text fill.
     */
    public void setTextFill(Paint aPaint)
    {
        getStringView().setTextFill(aPaint);
    }

    /**
     * Returns the StringView.
     */
    public boolean isStringViewSet()  { return _stringView !=null; }

    /**
     * Returns the StringView.
     */
    public StringView getStringView()
    {
        // If StringView already set, just return
        if (_stringView != null) return _stringView;

        // Create, configure, add StringView and return
        _stringView = new StringView();
        _stringView.setGrowWidth(isEditable());
        _stringView.setAlignX(getAlignX());
        addChild(_stringView, getGraphic() != null ? 1 : 0);
        return _stringView;
    }

    /**
     * Returns the graphic node.
     */
    public View getGraphic()  { return _graphic; }

    /**
     * Sets the graphic node.
     */
    public void setGraphic(View aGraphic)
    {
        // If already set, just return
        View old = getGraphic(); if (aGraphic == old) return;

        // Remove old
        if (_graphic != null && _graphic.getParent() != null)
            removeChild(_graphic);

        // Set
        _graphic = aGraphic;

        // Add new
        if (_graphic != null)
            addChild(_graphic, 0);

        // Fire prop change
        firePropChange(Graphic_Prop, old, _graphic);
    }

    /**
     * Returns the graphic node after the text.
     */
    public View getGraphicAfter()  { return _graphicAfter; }

    /**
     * Sets the graphic node after the text.
     */
    public void setGraphicAfter(View aGraphic)
    {
        // If already set, just return
        View old = getGraphicAfter(); if (aGraphic == old) return;

        // Remove old
        if (_graphicAfter != null && _graphicAfter.getParent() != null)
            removeChild(_graphicAfter);

        // Set new
        _graphicAfter = aGraphic;

        // Add new
        if (_graphicAfter != null)
            addChild(_graphicAfter);

        // Fire prop change
        firePropChange(GraphicAfter_Prop, old, _graphicAfter);
    }

    /**
     * Returns the image name, if loaded from local resource.
     */
    public String getImageName()  { return _imageName; }

    /**
     * Sets the image name, if loaded from local resource.
     */
    public void setImageName(String aName)
    {
        _imageName = aName;
    }

    /**
     * Returns whether label text is editable.
     */
    public boolean isEditable()  { return _editable; }

    /**
     * Sets whether label text is editable.
     */
    public void setEditable(boolean aValue)
    {
        if (aValue == isEditable()) return;
        firePropChange(Editable_Prop, _editable, _editable = aValue);

        // Enable/Disable MosueRelease
        if (aValue)
            enableEvents(MouseRelease);
        else disableEvents(MouseRelease);

        // If Editable, StringView should fill width
        if (isStringViewSet())
            getStringView().setGrowWidth(isEditable());
    }

    /**
     * Returns whether editable.
     */
    public boolean isEditing()  { return _editing; }

    /**
     * Sets editing.
     */
    public void setEditing(boolean aValue)
    {
        // If value already set, just return
        if (aValue == isEditing()) return;
        _editing = aValue;

        // Handle set true
        if (aValue) {
            TextField editor = getEditor();
            editor.setText(getText());
            Rect bnds = getStringView().getBounds();
            bnds.inset(-2);
            editor.setBounds(bnds);
            addChild(editor);
            editor.selectAll();
            editor.requestFocus();
            getStringView().setPaintable(false);
        }

        // Handle set false
        else {
            removeChild(_editor);
            setText(_editor.getText());
            getStringView().setPaintable(true);
            _editor = null;
        }

        // Fire prop change
        firePropChange(Editing_Prop, !aValue, aValue);
    }

    /**
     * Returns the editor.
     */
    public TextField getEditor()
    {
        // If editor set, return
        if (_editor != null) return _editor;

        // Create and return editor
        TextField editor = new TextField();
        editor.setManaged(false);
        editor.setBorderRadius(2);
        editor.setFill(new Color(1,.95));
        editor.setBorder(new Color(1,.3,.3,.5), 1);
        editor.setBorder(editor.getBorder().copyForInsets(Insets.EMPTY));
        editor.setPadding(2,2,2,2);
        editor.setAlignX(getAlignX());
        editor.setFont(getFont());
        editor.addEventHandler(e -> editorFiredAction(), Action);
        editor.addPropChangeListener(pc -> editorFocusChanged(editor), Focused_Prop);
        return _editor = editor;
    }

    /**
     * Called when editor fires action.
     */
    protected void editorFiredAction()
    {
        setEditing(false);
        fireActionEvent(null);
    }

    /**
     * Called when editor focus changes.
     */
    protected void editorFocusChanged(TextField editor)
    {
        if (!editor.isFocused())
            setEditing(false);
    }

    /**
     * Handle events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        if (isEditable() && anEvent.isMouseRelease() && anEvent.getClickCount()==2)
            setEditing(true);
    }

    /**
     * Returns the preferred width.
     */
    protected double getPrefWidthImpl(double aH)
    {
        if (isHorizontal())
            return RowView.getPrefWidth(this, aH);
        return ColView.getPrefWidth(this, -1);
    }

    /**
     * Returns the preferred height.
     */
    protected double getPrefHeightImpl(double aW)
    {
        if (isHorizontal())
            return RowView.getPrefHeight(this, aW);
        return ColView.getPrefHeight(this, -1);
    }

    /**
     * Layout children.
     */
    protected void layoutImpl()
    {
        if (isHorizontal())
            RowView.layout(this, false);
        else ColView.layout(this, false);
    }

    /**
     * Returns a mapped property name.
     */
    public String getValuePropName()  { return "Text"; }

    /**
     * Override to make default align center-left.
     */
    public Pos getDefaultAlign()  { return Pos.CENTER_LEFT; }

    /**
     * Override to forward to StringView.
     */
    public void setAlign(Pos aPos)
    {
        super.setAlign(aPos);
        if (isStringViewSet())
            getStringView().setAlignX(getAlignX());
    }

    /**
     * Override for custom defaults.
     */
    @Override
    public Object getPropDefault(String aPropName)
    {
        // Spacing
        if (aPropName == Spacing_Prop)
            return DEFAULT_LABEL_SPACING;

        // Do normal version
        return super.getPropDefault(aPropName);
    }

    /**
     * XML archival.
     */
    public XMLElement toXMLView(XMLArchiver anArchiver)
    {
        // Archive basic view attributes
        XMLElement e = super.toXMLView(anArchiver);

        // Archive Text and ImageName
        String text = getText();
        if (text != null && text.length() > 0)
            e.add("text", text);
        String iname = getImageName();
        if (iname != null)
            e.add("image", iname);

        // Return element
        return e;
    }

    /**
     * XML unarchival.
     */
    protected void fromXMLView(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic view attributes
        super.fromXMLView(anArchiver, anElement);

        // Unarchive Text and ImageName
        setText(anElement.getAttributeValue("text", anElement.getAttributeValue("value")));
        String iname = anElement.getAttributeValue("image");
        if (iname != null) {
            setImageName(iname);
            Image image = ViewArchiver.getImage(anArchiver, iname);
            if (image != null)
                setImage(image);
        }
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + " { text=" + getText() + "}";
    }
}