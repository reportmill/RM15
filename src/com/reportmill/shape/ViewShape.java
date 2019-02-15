package com.reportmill.shape;
import com.reportmill.graphics.RMFont;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * A shape subclass that represents an app control (or Snap View).
 */
public class ViewShape extends RMShape {
    
    // The View
    View              _view;
    
    // The text associated with the view
    String            _text;
    
    // Whether view is multi line
    boolean           _multiline;
    
    // Constants for properties
    static final String Text_Prop = "Text";
    static final String View_Prop = "View";
    static final String Multiline_Prop = "Multiline";

    // Constants for View types
    public static final String TextField_Type = "TextField";
    public static final String Button_Type = "Button";
    public static final String RadioButton_Type = "RadioButton";
    public static final String CheckBox_Type = "CheckBox";
    public static final String ListView_Type = "ListView";
    public static final String ComboBox_Type = "ComboBox";
    public static final String Other_Type = "Other";

/**
 * Creates a new View Shape.
 */
public ViewShape()
{
    setViewType(TextField_Type);
    setStandardSize();
}

/**
 * Returns the view.
 */
public View getView()  { return _view; }

/**
 * Sets the view.
 */
public void setView(View aView)
{
    firePropChange(View_Prop, _view, _view = aView);
    _view.setSize(getWidth(), getHeight());
    if(getText()!=null) _view.setText(getText());
    _view.setFont(getFont());
    repaint();
}

/**
 * Returns the View type.
 */
public String getViewType()
{
    View view = getView();
    if(view instanceof TextArea) return TextField_Type;
    if(view instanceof RadioButton) return RadioButton_Type;
    if(view instanceof CheckBox) return CheckBox_Type;
    if(view instanceof Button) return Button_Type;
    if(view instanceof ListView) return ListView_Type;
    if(view instanceof ComboBox) return ComboBox_Type;
    return Other_Type;
}

/**
 * Sets the View type.
 */
public void setViewType(String aType)
{
    View view = null;
    switch(aType) {
        case TextField_Type: view = new TextArea(); break;
        case Button_Type: view = new Button("Button"); break;
        case RadioButton_Type: view = new RadioButton("Radio Button"); break;
        case CheckBox_Type: view = new CheckBox("CheckBox"); break;
        case ListView_Type: view = new ListView(); break;
        case ComboBox_Type: view = new ComboBox(); break;
    }
    if(view!=null)
        setView(view);
}

/**
 * Returns a standard size for control.
 */
Size getStandardSize(String aType)
{
    switch(aType) {
        case ListView_Type:  return new Size(120,140);
        default: return new Size(100,24);
    }
}

/**
 * Sets the standard size.
 */
public void setStandardSize()  { setSize(getStandardSize(getViewType())); }

/**
 * Returns the text associated with the view.
 */
public String getText()  { return _text; }

/**
 * Sets the text assoicated with the view.
 */
public void setText(String aString)
{
    if(SnapUtils.equals(aString, _text)) return;
    firePropChange(Text_Prop, _text, _text = aString);
    _view.setText(aString);
    repaint();
}

/**
 * Returns whether view is multiline (text).
 */
public boolean isMultiline()  { return _multiline; }

/**
 * Sets whether view is multiline (text).
 */
public void setMultiline(boolean aValue)  { _multiline = aValue; }

/**
 * Override to paint shape.
 */
protected void paintShape(Painter aPntr)
{
    super.paintShape(aPntr);
    ViewUtils.paintAll(_view, aPntr);
}

/**
 * Override to store font.
 */
public RMFont getFont()  { return _font!=null? _font : super.getFont(); } RMFont _font;

/**
 * Override to store font.
 */
public void setFont(RMFont aFont)  { _font = aFont; }

/**
 * Override to propogate to view.
 */
public void setWidth(double aValue)  { super.setWidth(aValue); _view.setWidth(aValue); }

/**
 * Override to propogate to view.
 */
public void setHeight(double aValue)  { super.setHeight(aValue); _view.setHeight(aValue); }

/**
 * Override to store font.
 */
public void setParent(RMParentShape aShape)
{
    super.setParent(aShape);
    if(_view!=null) _view.setFont(getFont());
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = super.toXML(anArchiver); e.setName("ViewShape");
    e.add("ViewType", getViewType());
    if(getText()!=null && getText().length()>0) e.add(Text_Prop, getText());
    if(isMultiline()) e.add(Multiline_Prop, true);
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXML(anArchiver, anElement);
    if(anElement.hasAttribute("ViewType")) setViewType(anElement.getAttributeValue("ViewType"));
    if(anElement.hasAttribute(Text_Prop)) setText(anElement.getAttributeValue(Text_Prop));
    if(anElement.hasAttribute(Multiline_Prop)) setMultiline(anElement.getAttributeBoolValue(Multiline_Prop));
    return this;
}

}