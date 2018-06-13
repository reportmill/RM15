/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.TextLineStyle;
import snap.util.*;

/**
 * This class represents attributes of a paragraph in an RMXString (all of the characters up to and including each
 * newline in an RMXString make up a paragraph). Paragraphs can have their own alignment, indentation, min/max line
 * height, etc. You might use this class like this:
 * <p><blockquote><pre>
 *   RMParagraph pgraph = RMParagraph.defaultParagraph.deriveAligned(RMParagraph.ALIGN_RIGHT);
 *   RMXString xstring = new RMXString("Hello World", pgraph);
 */
public class RMParagraph implements Cloneable, RMTypes, XMLArchiver.Archivable {
    
    // The line style
    TextLineStyle  _lstyle = TextLineStyle.DEFAULT;
    
    // Default paragraph
    public static final RMParagraph DEFAULT = new RMParagraph();
    public static final RMParagraph CENTERED = DEFAULT.deriveAligned(RMTypes.AlignX.Center);

    // Constants for tab types
    public static final char TAB_LEFT = TextLineStyle.TAB_LEFT;
    public static final char TAB_RIGHT = TextLineStyle.TAB_RIGHT;
    public static final char TAB_CENTER = TextLineStyle.TAB_CENTER;
    public static final char TAB_DECIMAL = TextLineStyle.TAB_DECIMAL;
    
/**
 * Creates a new paragraph object initialized to defaultParagraph.
 */
public RMParagraph() { }

/**
 * Creates a new paragraph with the given alignment and indentation.
 */
public RMParagraph(TextLineStyle aLineStyle)  { _lstyle = aLineStyle; }

/**
 * Returns the alignment associated with this paragraph.
 */
public AlignX getAlignmentX()
{
    if(_lstyle.isJustify()) return AlignX.Full;
    return AlignX.get(_lstyle.getAlign());
}

/**
 * Returns indentation of first line in paragraph (this can be set different than successive lines).
 */
public double getFirstIndent() { return _lstyle.getFirstIndent(); }

/**
 * Returns the left side indentation of this paragraph.
 */
public double getLeftIndent() { return _lstyle.getLeftIndent(); }

/**
 * Returns the right side indentation of this paragraph.
 */
public double getRightIndent() { return _lstyle.getRightIndent(); }

/**
 * Returns the spacing of lines expressed as a factor of a given line's height.
 */
public float getLineSpacing() { return (float)_lstyle.getSpacingFactor(); }

/**
 * Returns additional line spacing expressed as a constant amount in points.
 */
public float getLineGap() { return (float)_lstyle.getSpacing(); }

/**
 * Returns the minimum line height in printer points associated with this paragraph.
 */
public float getLineHeightMin() { return (float)_lstyle.getMinHeight(); }

/**
 * Returns the maximum line height in printer points associated with this paragraph.
 */
public float getLineHeightMax() { return (float)_lstyle.getMaxHeight(); }

/**
 * Returns the spacing between paragraphs in printer points associated with this paragraph.
 */
public float getParagraphSpacing() { return (float)_lstyle.getNewlineSpacing(); }

/**
 * Returns the number of tabs associated with this paragraph.
 */
public int getTabCount() { return _lstyle.getTabCount(); }

/**
 * Returns the specific tab value for the given index in printer points.
 */
public float getTab(int anIndex) { return (float)_lstyle.getTab(anIndex); }

/**
 * Returns the type of tab at the given index.
 */
public char getTabType(int anIndex) { return _lstyle.getTabType(anIndex); }

/**
 * Returns the raw tab array
 */
public float[] getTabs()
{
    double tabs[] = _lstyle.getTabs();
    float ftabs[] = new float[tabs.length]; for(int i=0;i<tabs.length;i++) ftabs[i] = (float)tabs[i];
    return ftabs;
}

/**
 * Returns the raw tab type array
 */
public char[] getTabTypes() { return _lstyle.getTabTypes(); }

/**
 * Returns the tab index for the given location.
 */
public int getTabIndex(double aLoc)  { return _lstyle.getTabIndex(aLoc); }

/**
 * Returns a paragraph identical to the receiver, but with the given alignment.
 */
public RMParagraph deriveAligned(AlignX anAlign)
{
    RMParagraph ps = clone();
    if(anAlign==AlignX.Full) ps._lstyle = _lstyle.copyFor(TextLineStyle.JUSTIFY_KEY, true);
    else ps._lstyle = _lstyle.copyFor(anAlign.hpos());
    return ps;
}

/**
 * Returns a paragraph identical to the receiver, but with the given indentation values.
 */
public RMParagraph deriveIndent(double firstIndent, double leftIndent, double rightIndent)
{
    RMParagraph ps = clone();
    ps._lstyle = ps._lstyle.copyFor(TextLineStyle.FIRST_INDENT_KEY, firstIndent);
    ps._lstyle = ps._lstyle.copyFor(TextLineStyle.LEFT_INDENT_KEY, leftIndent);
    ps._lstyle = ps._lstyle.copyFor(TextLineStyle.RIGHT_INDENT_KEY, rightIndent);
    return ps;
}

/**
 * Returns a paragraph identical to the receiver, but with the given line spacing.
 */
public RMParagraph deriveLineSpacing(float aHeight)
{
    RMParagraph ps = clone(); ps._lstyle = _lstyle.copyFor(TextLineStyle.SPACING_FACTOR_KEY, aHeight); return ps;
}

/**
 * Returns a paragraph identical to the receiver, but with the given line gap.
 */
public RMParagraph deriveLineGap(float aHeight)
{
    RMParagraph ps = clone(); ps._lstyle = _lstyle.copyFor(TextLineStyle.SPACING_KEY, aHeight); return ps;
}

/**
 * Returns a paragraph identical to the receiver, but with the given min line height.
 */
public RMParagraph deriveLineHeightMin(float aHeight)
{
    RMParagraph ps = clone(); ps._lstyle = _lstyle.copyFor(TextLineStyle.MIN_HEIGHT_KEY, aHeight); return ps;
}

/**
 * Returns a paragraph identical to the receiver, but with the given max line height.
 */
public RMParagraph deriveLineHeightMax(float aHeight)
{
    RMParagraph ps = clone(); ps._lstyle = _lstyle.copyFor(TextLineStyle.MAX_HEIGHT_KEY, aHeight); return ps;
}

/**
 * Standard clone of this object.
 */
public RMParagraph clone()
{
    try { return (RMParagraph)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    if(this==anObj) return true;
    if(!(anObj instanceof RMParagraph)) return false;
    RMParagraph other = (RMParagraph)anObj;
    if(!other._lstyle.equals(_lstyle)) return false;
    return true;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)  { return _lstyle.toXML(anArchiver); }

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    _lstyle = new TextLineStyle().fromXML(anArchiver, anElement);
    return this;
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "RMParagraph: " + _lstyle; }

}