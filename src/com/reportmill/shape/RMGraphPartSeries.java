/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMFormat;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.Effect;
import snap.util.*;

/**
 * This shape is used by graph area to hold attributes of the value axis.
 */
public class RMGraphPartSeries extends RMShape {

    // The graph this series part works for
    RMGraph                     _graph;

    // The title of the series
    String                      _title;
    
    // The currently selected label position
    LabelPos                    _position = LabelPos.Middle;
    
    // The map of label shapes for label positions
    Map <LabelPos,RMTextShape>  _labelShapes = new HashMap();
    
    // Default cell paragraph (aligned center)
    static RMParagraph          _defaultParagraph = RMParagraph.DEFAULT.deriveAligned(RMTypes.AlignX.Center);

    // Constants for value label positions
    public enum LabelPos { Top, Middle, Bottom, Above, Below }

/**
 * Returns the title of the series.
 */
public String getTitle()  { return _title!=null? _title : (_title=getTitleDefault()); }

/**
 * Sets the title of the series.
 */
public void setTitle(String aTitle)
{
    _title = aTitle;
    relayoutParent();
}

/**
 * Returns the default title.
 */
protected String getTitleDefault()
{
    int i=0; while(i<_graph.getSeriesCount() && _graph.getSeries(i)!=this) i++;
    return "Series " + (i+1);
}

/**
 * Returns the value label position (top, middle, bottom, outside).
 */
public LabelPos getPosition()  { return _position; }

/**
 *  Sets the value label position (top, middle, bottom, outside).
 */
public void setPosition(LabelPos aPosition)
{
    if(aPosition==getPosition()) return;
    firePropChange("Position", _position, _position = aPosition);
    relayoutParent();
}

/**
 * Returns a label position for a given string.
 */
public LabelPos getPosition(String aString)
{
    if("outside".equals(aString)) aString = "Above"; // Fix string
    else aString = StringUtils.firstCharUpperCase(aString);
    return LabelPos.valueOf(aString);
}

/**
 * Returns a label shape for given position.
 */
public RMTextShape getLabelShape(LabelPos aPosition)
{
    RMTextShape labelShape = _labelShapes.get(aPosition);
    if(labelShape==null)
        _labelShapes.put(aPosition, labelShape = new RMTextShape());
    return labelShape;
}

/**
 * Sets the label shape text for position.
 */
public void setLabelText(LabelPos aPosition, String aString)
{
    RMTextShape ts = getLabelShape(aPosition);
    ts.setText(aString);
    relayoutParent();
}

/**
 * Returns the first active position.
 */
public LabelPos getFirstActivePosition()
{
    for(LabelPos position : LabelPos.values())
        if(getLabelShape(position).length()>0)
            return position;
    return null;
}

/**
 * Returns the proxy, determined by the current position.
 */
public RMTextShape getProxy()
{
    return getLabelShape(getPosition());
}

/** Override to handle proxy. */
public RMStroke getStroke()  { return getProxy().getStroke(); }

/** Override to handle proxy. */
public void setStroke(RMStroke aStroke)
{
    getProxy().setStroke(aStroke);
}

/** Override to handle proxy. */
public RMFill getFill()  { return getProxy().getFill(); }

/** Override to handle proxy. */
public void setFill(RMFill aFill)
{
    getProxy().setFill(aFill);
}

/** Override to handle proxy. */
public Effect getEffect()  { return getProxy().getEffect(); }

/** Override to handle proxy. */
public void setEffect(Effect anEffect)
{
    getProxy().setEffect(anEffect);
}

/** Override to handle proxy. */
public double getOpacity()  { return getProxy().getOpacity(); }

/** Override to handle proxy. */
public void setOpacity(double aValue)
{
    getProxy().setOpacity(aValue);
}

/** Override to handle proxy. */
public RMColor getTextColor()  { return getProxy().getTextColor(); }

/** Override to handle proxy. */
public void setTextColor(RMColor aColor)
{
    getProxy().setTextColor(aColor);
}

/** Override to handle proxy. */
public RMFont getFont()  { return getProxy().getFont(); }

/** Override to handle proxy. */
public void setFont(RMFont aFont)
{
    getProxy().setFont(aFont);
}

/** Override to handle proxy. */
public boolean isUnderlined()  { return getProxy().isUnderlined(); }

/** Override to handle proxy. */
public void setUnderlined(boolean aFlag)
{
    getProxy().setUnderlined(aFlag);
}

/** Override to handle proxy. */
public RMFormat getFormat()  { return getProxy().getFormat(); }

/** Override to handle proxy. */
public void setFormat(RMFormat aFormat)
{
    getProxy().setFormat(aFormat);
}

/** Override to handle proxy. */
public String getURL()  { return getProxy()!=null? getProxy().getURL() : super.getURL(); }

/** Override to handle proxy. */
public void setURL(String aURL)
{
    getProxy().setURL(aURL);
}

/** Override to handle proxy. */
public double getRoll()  { return getProxy().getRoll(); }

/** Override to handle proxy. */
public void setRoll(double aValue)
{
    getProxy().setRoll(aValue);
    relayoutParent();
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXML(anArchiver); e.setName("series");
    
    // Archive title
    if(!getTitle().equals(getTitleDefault()))
        e.add("title", getTitle());

    // Archive position
    //if(getPosition()!=LabelPosition.Middle) e.add("position", getPosition());
    
    // Archive labels
    for(LabelPos position : LabelPos.values())
        if(getLabelShape(position).length()>0) {
            XMLElement labelXML = getLabelShape(position).toXML(anArchiver);
            labelXML.setName("label");
            labelXML.add("position", position.toString());
            e.add(labelXML);
        }

    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXML(anArchiver, anElement);
    
    // Unarchive title
    if(anElement.hasAttribute("title"))
        setTitle(anElement.getAttributeValue("title"));
    
    // Unarchive label position shape if embedded string (legacy)
    if(anElement.get("string")!=null) {
        RMTextShape label = (RMTextShape)new RMTextShape().fromXML(anArchiver, anElement);
        LabelPos position = getPosition((anElement.getAttributeValue("position", LabelPos.Middle.toString())));
        _labelShapes.put(position, label);
    }
    
    // Unarchive labels
    for(XMLElement labelXML : anElement.getElements("label")) {
        RMTextShape label = (RMTextShape)new RMTextShape().fromXML(anArchiver, labelXML);
        LabelPos position = getPosition(labelXML.getAttributeValue("position"));
        _labelShapes.put(position, label);
    }
    
    // If active position, select it
    if(getFirstActivePosition()!=null)
        setPosition(getFirstActivePosition());

    // Return this graph
    return this;
}

}