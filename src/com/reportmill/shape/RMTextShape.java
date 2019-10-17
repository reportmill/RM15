/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMFormat;
import com.reportmill.graphics.*;
import java.util.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;

/**
 * This class is an RMShape subclass for handling rich text. Text is probably the most common and useful element in a
 * ReportMill template. You might use this class to programmatically build or modify a template, like this:
 * <p><blockquote><pre>
 *   RMXString xstring = new RMXString("Hello world!", RMFont.getFont("Arial", 12), RMColor.red);
 *   RMText text = new RMText(xstring);
 *   template.getPage(0).addChild(text);
 *   text.setXY(36, 36);
 *   text.setSizeToFit();
 * </pre></blockquote>
 */
public class RMTextShape extends RMRectShape {
    
    // The real backing store for text is an xstring
    RMXString              _xstr;
    
    // The text margin (if different than default)
    Insets                 _margin = getMarginDefault();
    
    // Vertical alignment of text
    AlignY                 _alignY = AlignY.Top;
    
    // Specifies how text should handle overflow during RPG (ignore it, shrink it or paginate it)
    byte                   _wraps;
    
    // Whether to fit text on layout
    boolean                _fitText;
    
    // Whether text should wrap around other shapes that cause wrap
    boolean                _performsWrap = false;

    // Whether text should eliminate empty lines during RPG
    boolean                _coalesceNewlines;
    
    // Whether text should draw box around itself even if there's no stroke
    boolean                _drawsSelectionRect;
    
    // PDF option: Whether text box is editable in PDF
    boolean                _editable;
    
    // PDF option: Whether text is multiline when editable in PDF
    boolean                _multiline;

    // The linked text shape for rendering overflow, if there is one
    RMLinkedText           _linkedText;
    
    // A text box to manage RichText in shape bounds
    TextBox                _textBox;

    // The text editor, if one has been set
    RMTextEditor           _textEdtr;
    
    // The default text margin (top=1, left=2, bottom=0, right=2)
    static Insets          _marginDefault = new Insets(1, 2, 0, 2);
    
    // A listener to handle rich text changes
    PropChangeListener     _richTextLsnr = pc -> richTextDidPropChange(pc);
    
    // Constants for overflow behavior during RPG
    public static final byte WRAP_NONE = 0;
    public static final byte WRAP_BASIC = 1;
    public static final byte WRAP_SCALE = 2;
    
    // Constants for properties
    public static final String Editable_Prop = "Editable";
    public static final String Multiline_Prop = "Multiline";
    
/**
 * Creates an empty text instance.
 */
public RMTextShape() { }

/**
 * Creates a text instance initialized with the given RMXString.
 */
public RMTextShape(RMXString string)  { _xstr = string; }

/**
 * Creates a text instance initialized with the given plain text String.
 */
public RMTextShape(String plainText)  { getXString().addChars(plainText); }

/**
 * Returns the XString associated with this RMText.
 */
public RMXString getXString()
{
    if(_xstr!=null) return _xstr;
    _xstr = new RMXString();
    _xstr.getRichText().addPropChangeListener(_richTextLsnr);
    return _xstr;
}

/**
 * Sets the XString associated with this RMText.
 */
public void setXString(RMXString xString)
{
    // If value already set, just return
    if(xString==_xstr) return;
    
    // Stop listening to last XString and start listening to new XString
    if(_xstr!=null) _xstr.getRichText().removePropChangeListener(_richTextLsnr);
    if(xString!=null) xString.getRichText().addPropChangeListener(_richTextLsnr);
    
    // Set value and fire property change, and reset cached HeightToFit
    firePropChange("XString", _xstr, _xstr = xString);
    _textBox = null; _textEdtr = null;
    revalidate(); repaint();
}

/**
 * Returns the RichText.
 */
public RichText getRichText()  { return getXString().getRichText(); }

/**
 * Returns the length, in characters, of the XString associated with this RMText.
 */
public int length()  { return getRichText().length(); }

/**
 * Returns the text associated with this RMText as a plain String.
 */
public String getText()  { return getXString().getText(); }

/**
 * Replaces the current text associated with this RMText with the given String.
 */
public void setText(String aString)  { getXString().replaceChars(aString, 0, length()); }

/**
 * Returns the first character index visible in this text.
 */
public int getVisibleStart()  { return 0; }

/**
 * Returns the last character index visible in this text.
 */
public int getVisibleEnd()  { return getTextBox().getEnd(); }

/**
 * Returns whether all characters can be visibly rendered in text bounds.
 */
public boolean isAllTextVisible()  { return !getTextBox().isOutOfRoom(); }

/**
 * Returns the font for char 0.
 */
public RMFont getFont()
{
    if(isTextEditorSet())
        return getTextEditor().getFont();
    return getXString().getFontAt(0);
}

/**
 * Sets the font for all characters.
 */
public void setFont(RMFont aFont)
{
    if(isTextEditorSet())
        getTextEditor().setFont(aFont);
    else getXString().setAttribute(aFont);
}

/**
 * Returns the format for char 0.
 */
public RMFormat getFormat()
{
    if(isTextEditorSet())
        return getTextEditor().getFormat();
    return getXString().getRunAt(0).getFormat();
}

/**
 * Sets the format for all characters.
 */
public void setFormat(RMFormat aFormat)
{
    if(isTextEditorSet())
        getTextEditor().setFormat(aFormat);
    else getXString().setAttribute(aFormat, 0, length());
}

/**
 * Returns the color of the first character of the xstring associated with this RMText.
 */
public RMColor getTextColor()  { return getXString().getRunAt(0).getColor(); }

/**
 * Sets the color of the characters in the XString associated with this RMText.
 */
public void setTextColor(RMColor aColor)  { getXString().setAttribute(aColor); }

/**
 * Returns if char 0 is underlined.
 */
public boolean isUnderlined()
{
    if(isTextEditorSet())
        return getTextEditor().isUnderlined();
    return getRichText().getRunAt(0).isUnderlined();
}

/**
 * Sets all chars to be underlined.
 */
public void setUnderlined(boolean aFlag)
{
    if(isTextEditorSet())
        getTextEditor().setUnderlined(aFlag);
    else getRichText().setStyleValue(TextStyle.UNDERLINE_KEY, aFlag? 1 : 0, 0, length());
}

/**
 * Returns the border for char 0.
 */
public Border getTextBorder()
{
    if(isTextEditorSet())
        return getTextEditor().getTextBorder();
    return getRichText().getRunAt(0).getBorder();
}

/**
 * Sets the border for all characters.
 */
public void setTextBorder(Border aBorder)
{
    if(isTextEditorSet())
        getTextEditor().setTextBorder(aBorder);
    else getRichText().setStyleValue(TextStyle.BORDER_KEY,aBorder,0,length());
}

/**
 * Returns the alignment for char 0.
 */
public AlignX getAlignmentX()
{
    if(isTextEditorSet())
        return getTextEditor().getAlignX();
    return getXString().getAlignX();
}

/**
 * Sets the align for all chars.
 */
public void setAlignmentX(AlignX anAlignX)
{
    if(isTextEditorSet())
        getTextEditor().setAlignX(anAlignX);
    else getXString().setAlignX(anAlignX);
}

/**
 * Returns the alignment as a string, one of: "left", "center" or "right".
 */
public String getAlignString()  { return getAlignmentX().toString().toLowerCase(); }

/**
 * Returns the vertical alignment.
 */
public AlignY getAlignmentY()  { return _alignY; }

/**
 * Sets the vertical alignment.
 */
public void setAlignmentY(AlignY anAlignment)
{
    firePropChange("AlignmentY", _alignY, _alignY = anAlignment);
    revalidate(); repaint();
}

/**
 * Returns the wrapping behavior for over-filled rpgCloned text (NONE, WRAP, SHRINK).
 */
public byte getWraps()  { return _wraps; }

/**
 * Sets the wrapping behavior for over-filled rpgCloned text (NONE, WRAP, SHRINK).
 */
public void setWraps(byte aValue)  { _wraps = aValue; }

/**
 * Returns whether text should wrap around other shapes that cause wrap.
 */
public boolean getPerformsWrap()  { return _performsWrap; }

/**
 * Sets whether text should wrap around other shapes that cause wrap.
 */
public void setPerformsWrap(boolean aFlag)  { _performsWrap = aFlag; }

/**
 * Returns whether text should coalesce consecutive newlines in rpgClone.
 */
public boolean getCoalesceNewlines()  { return _coalesceNewlines; }

/**
 * Sets whether text should coalesce consecutive newlines in rpgClone.
 */
public void setCoalesceNewlines(boolean aFlag)  { _coalesceNewlines = aFlag; }

/**
 * Returns whether text should always draw at least a light gray border (useful when editing).
 */
public boolean getDrawsSelectionRect()  { return _drawsSelectionRect; }

/**
 * Sets whether text should always draw at least a light-gray border (useful when editing).
 */
public void setDrawsSelectionRect(boolean aValue)  { _drawsSelectionRect = aValue; }

/**
 * Returns whether text box is editable in PDF.
 */
public boolean isEditable()  { return _editable; }

/**
 * Sets whether text box is editable in PDF.
 */
public void setEditable(boolean aValue)
{
    firePropChange(Editable_Prop, _editable, _editable = aValue);
}

/**
 * Returns whether text is multiline when editable in PDF.
 */
public boolean isMultiline()  { return _multiline; }

/**
 * Sets whether text is multiline when editable in PDF.
 */
public void setMultiline(boolean aValue)
{
    firePropChange(Multiline_Prop, _multiline, _multiline = aValue);
}

/**
 * Returns the char spacing at char 0.
 */
public float getCharSpacing()
{
    if(isTextEditorSet())
        return getTextEditor().getCharSpacing();
    return getXString().getRun(0).getCharSpacing();
}

/**
 * Sets the char spacing for the text string.
 */
public void setCharSpacing(float aValue)
{
    if(isTextEditorSet())
        getTextEditor().setCharSpacing(aValue);
    else getXString().setAttribute(RMTextStyle.CHAR_SPACING_KEY, aValue==0? null : aValue);
}

/**
 * Returns the line spacing at char 0.
 */
public float getLineSpacing()
{
    if(isTextEditorSet())
        return getTextEditor().getLineSpacing();
    return getXString().getParagraphAt(0).getLineSpacing();
}

/**
 * Sets the line spacing for all chars.
 */
public void setLineSpacing(float aHeight)
{
    if(isTextEditorSet())
        getTextEditor().setLineSpacing(aHeight);
    else {
        RMParagraph ps = getXString().getParagraphAt(0).deriveLineSpacing(aHeight);
        getXString().setParagraph(ps, 0, length());
    }
}

/**
 * Returns the line gap at char 0.
 */
public float getLineGap()
{
    if(isTextEditorSet())
        return getTextEditor().getLineGap();
    return getXString().getParagraphAt(0).getLineGap();
}

/**
 * Sets the line gap for all chars.
 */
public void setLineGap(float aHeight)
{
    if(isTextEditorSet())
        getTextEditor().setLineGap(aHeight);
    else {
        RMParagraph ps = getXString().getParagraphAt(0).deriveLineGap(aHeight);
        getXString().setParagraph(ps, 0, length());
    }
}

/**
 * Returns the minimum line height at char 0.
 */
public float getLineHeightMin()
{
    if(isTextEditorSet())
        return getTextEditor().getLineHeightMin();
    return getXString().getParagraphAt(0).getLineHeightMin();
}

/**
 * Sets the minimum line height for all chars.
 */
public void setLineHeightMin(float aHeight)
{
    if(isTextEditorSet())
        getTextEditor().setLineHeightMin(aHeight);
    else {
        RMParagraph ps = getXString().getParagraphAt(0).deriveLineHeightMin(aHeight);
        getXString().setParagraph(ps, 0, length());
    }
}

/**
 * Returns the maximum line height at char 0.
 */
public float getLineHeightMax()
{
    if(isTextEditorSet())
        return getTextEditor().getLineHeightMax();
    return getXString().getParagraphAt(0).getLineHeightMax();
}

/**
 * Sets the maximum line height for all chars.
 */
public void setLineHeightMax(float aHeight)
{
    if(isTextEditorSet())
        getTextEditor().setLineHeightMax(aHeight);
    else {
        RMParagraph ps = getXString().getParagraphAt(0).deriveLineHeightMax(aHeight);
        getXString().setParagraph(ps, 0, length());
    }
}

/**
 * Returns margin.
 */
public Insets getMargin()  { return _margin; }

/**
 * Sets margin.
 */
public void setMargin(Insets aMargin)
{
    if(_margin.equals(aMargin)) return;
    firePropChange("Margin", _margin, _margin = aMargin);
    revalidate(); repaint();    
}

/**
 * Returns the default margin of the text (top=1, left=2, right=2, bottom=0).
 */
public Insets getMarginDefault()  { return _marginDefault; }

/**
 * Returns the margin as a string.
 */
public String getMarginString()
{
    return getMarginTop() + ", " + getMarginLeft() + ", " + getMarginBottom() + ", " + getMarginRight();
}

/**
 * Sets the margin as a string.
 */
public void setMarginString(String aString)
{
    // If given string is empty, set default margins
    if(aString==null || aString.trim().length()==0) { setMargin(getMarginDefault()); return; }
    
    // Split the string by commas or spaces and get the parts
    String parts[] = aString.indexOf(",")>0? aString.split(",") : aString.split(" ");
    String p1 = parts[0];
    String p2 = parts[Math.min(1, parts.length-1)];
    String p3 = parts[Math.min(2, parts.length-1)];
    String p4 = parts[Math.min(3, parts.length-1)];
    
    // Set margin from parts
    setMargin(new Insets(SnapUtils.intValue(p1),SnapUtils.intValue(p4),SnapUtils.intValue(p3),SnapUtils.intValue(p2)));
}

/**
 * Returns the left margin of the text (default to 2).
 */
public int getMarginLeft()  { return (int)Math.round(getMargin().getLeft()); }

/**
 * Returns the right margin of the text (defaults to 2).
 */
public int getMarginRight()  { return (int)Math.round(getMargin().getRight()); }

/**
 * Returns the top margin of the text (defaults to 1).
 */
public int getMarginTop()  { return (int)Math.round(getMargin().getTop()); }

/**
 * Returns the bottom margin of the text (defaults to 0).
 */
public int getMarginBottom()  { return (int)Math.round(getMargin().getBottom()); }

/**
 * Override to revalidate.
 */
public void setWidth(double aValue)  { super.setWidth(aValue); revalidate(); }

/**
 * Override to revalidate.
 */
public void setHeight(double aValue)  { super.setHeight(aValue); revalidate(); }

/**
 * Overrides shape implementation to get clip path.
 */
public Shape getPath()
{
    // If text doesn't perform wrap or parent is null, return normal path in bounds
    if(!getPerformsWrap() || getParent()==null)
        return getPathShape()!=null? getPathShape().getPath().copyFor(getBoundsInside()) : super.getPath();
    
    // Get peers who cause wrap (if none, just return super path in bounds)
    List peersWhoCauseWrap = getPeersWhoCauseWrap();
    if(peersWhoCauseWrap==null)
        return getPathShape()!=null? getPathShape().getPath().copyFor(getBoundsInside()) : super.getPath();
    
    // Add this text to list
    peersWhoCauseWrap.add(0, this);
    
    // Get the path minus the neighbors, convert back to this shape, reset bounds to this shape
    _performsWrap = false;
    Shape path = RMShapeUtils.getSubtractedPath(peersWhoCauseWrap, -3);  // INSET NAILED TO -3
    _performsWrap = true;
    path = parentToLocal(path);
    path = path.copyFor(getBoundsInside());
    return path;
}

/**
 * Returns the subset of children that cause wrap.
 */
private List <RMShape> getPeersWhoCauseWrap()
{
    // Iterate over children and add any that intersect frame
    List list = null;
    for(int i=0, iMax=getParent().getChildCount(); i<iMax; i++) { RMShape child = getParent().getChild(i);
        if(child!=this && child.getFrame().intersects(getFrame())) {
            if(list==null) list = new ArrayList(); list.add(child); } }
    return list;
}

/**
 * This notification method is called when any peer is changed.
 */
public void peerDidChange(RMShape aShape)
{
    // If this text respects neighbors and shape intersects it, register for redraw
    if(getPerformsWrap() && aShape.getFrame().intersectsRect(getFrame())) {
        revalidate(); repaint(); }
}

/**
 * Returns the shape that provides the path for this text to wrap text to.
 */
public RMShape getPathShape()  { return _pathShape; } RMShape _pathShape;

/**
 * Sets the shape that provides the path for this text to wrap text to.
 */
public void setPathShape(RMShape aShape)
{
    if(SnapUtils.equals(aShape, _pathShape)) return;
    firePropChange("PathShape", _pathShape, _pathShape = aShape);
    revalidate(); repaint();
}

/**
 * Overrides rectangle implementation to potentially clear path shape.
 */
public void setRadius(float aValue)  { super.setRadius(aValue); setPathShape(null); }

/**
 * Returns the linked text for this text (if any).
 */
public RMLinkedText getLinkedText()  { return _linkedText; }

/**
 * Sets the linked text for this text (if any).
 */
public void setLinkedText(RMLinkedText aLinkedText)
{
    // Set linked text, and if non-null, set its previous text to this text
    _linkedText = aLinkedText;
    if(_linkedText!=null)
        _linkedText.setPreviousText(this);
    revalidate(); repaint();
}

/**
 * Returns a text layout.
 */
public TextBox getTextBox()
{
    if(_textBox!=null) return _textBox;
    _textBox = new TextBox(); _textBox.setWrapLines(true); updateTextBox();
    return _textBox;
}

/**
 * Updates the text box.
 */
protected void updateTextBox()
{
    _textBox.setText(getXString().getRichText());
    Insets pad = getMargin(); double pl = pad.left, pr = pad.right, pt = pad.top, pb = pad.bottom;
    double w = getWidth() - pl - pr, h = getHeight() - pt - pb; if(w<0) w = 0; if(h<0) h = 0;
    _textBox.setBounds(pl, pt, w, h);
    _textBox.setStart(getVisibleStart());
    _textBox.setLinked(getLinkedText()!=null);
    _textBox.setAlignY(getAlignmentY().vpos());
    _textBox.setBoundsPath(!(getPath() instanceof Rect) || getPerformsWrap()? getPath() : null);
    _textBox.setHyphenate(RMTextEditor.isHyphenating());
    _textBox.setFontScale(1);
    if(_fitText) _textBox.scaleTextToFit();
}

/**
 * Returns whether there is a text editor.
 */
public boolean isTextEditorSet()  { return _textEdtr!=null; }

/**
 * Returns the text editor.
 */
public RMTextEditor getTextEditor()
{
    if(_textEdtr!=null) return _textEdtr;
    _textEdtr = new RMTextEditor();
    _textEdtr.setTextBox(getTextBox());
    _textEdtr.setXString(getXString());
    return _textEdtr;
}

/**
 * Clears the text editor.
 */
public void clearTextEditor()  { _textEdtr = null; }

/**
 * Override to compute from RMTextLayout.
 */
protected double getPrefWidthImpl(double aHeight)
{
    // If font scaling, return current size
    if(_wraps==WRAP_SCALE) return getWidth();
    if(length()==0) return 0; // Zero instead of getMarginLeft() + getMarginRight() so empty texts are hidden
    
    // Get text box width (from first visible char) and return that plus margin
    double pw = getRichText().getPrefWidth(getVisibleStart());
    return Math.ceil(getMarginLeft() + pw + getMarginRight());
}

/**
 * Override to compute from RMTextLayout.
 */
protected double getPrefHeightImpl(double aWidth)
{
    if(_wraps==WRAP_SCALE) return getHeight();
    if(length()==0) return 0; // Zero instead of getMarginTop()+getMarginBottom() so empty texts are hidden
    double ph = getTextBox().getPrefHeight(getTextBox().getWidth());
    return Math.ceil(getMarginTop() + ph + getMarginBottom());
}

/**
 * Generate report.
 */
protected RMShape rpgShape(ReportOwner anRptOwner, RMShape aParent)
{
    RMTextShape clone = clone();
    RMXString string = clone.getXString();

    // Do xstring RPG (if no change due to RPG, just use normal) with FirePropChangeEnabled turned off
    string.getRichText().setPropChangeEnabled(false);
    string.rpgClone(anRptOwner, null, clone, false);
        
    // If coalesce newlines is set, coalesce newlines
    if(getCoalesceNewlines())
        string.coalesceNewlines();

    // Trim line ends from end of string to prevent extra empty line height
    int len = string.length(), end = len; while(end>0 && StringUtils.isLineEndChar(string.charAt(end-1))) end--;
    if(end!=len)
        string.removeChars(end, len);

    // If WRAP_SCALE, set FitText ivar
    if(getWraps()==WRAP_SCALE) clone._fitText = true;
    
    // Enable string FirePropChangeEnabled and revalidate
    string.getRichText().setPropChangeEnabled(true);
    clone.revalidate();
    
    // If paginating, swap in paginated parts (disable in table row)
    if(getWraps()==WRAP_BASIC && !(getParent() instanceof RMTableRow)) {
        ReportOwner.ShapeList shapes = new ReportOwner.ShapeList();
        for(RMTextShape text : clone.paginate())
            shapes.addChild(text);
        return shapes;
    }
    
    // Return clone
    return clone;
}

/**
 * Paginates this text by creating linked texts to show all text and returns a list of this text and the linked texts.
 */
protected List <RMTextShape> paginate()
{
    // Create pages list with this text in it
    List <RMTextShape> pages = new ArrayList(); pages.add(this);
    
    // Cache vertical alignment and set to Top
    AlignY verticalAlignment = getAlignmentY();
    setAlignmentY(AlignY.Top);
    
    // Get linked texts until all text visible
    RMTextShape text = this;
    while(!text.isAllTextVisible()) {
        text = new RMLinkedText(text);
        pages.add(text);
    }
    
    // Restore alignment on last text and return list
    text.setAlignmentY(verticalAlignment);
    return pages;
}

/**
 * Re-does the RPG clone to resolve any @Page@ keys (assumed to be present in userInfo).
 */
protected void resolvePageReferences(ReportOwner aRptOwner, Object userInfo)
{
    // Do normal shape resolve page references
    super.resolvePageReferences(aRptOwner, userInfo);
    
    // RPG clone xstring again and set
    RMXString xstringCloneRPG = _xstr.rpgClone(aRptOwner, userInfo, null, true);
    setXString(xstringCloneRPG);
}

/**
 * Creates a shape suitable for the "remainder" portion of a divideShape call (just a clone by default).
 */
protected RMShape createDivideShapeRemainder(byte anEdge)  { return anEdge==0? new RMLinkedText(this) : clone(); }

/** Editor method - indicates that this shape can be super selected. */
public boolean superSelectable()  { return true; }

/** Editor method. */
public boolean isStructured()  { return _parent instanceof RMTableRow && ((RMTableRow)_parent).isStructured(); }

/**
 * Paints a text shape.
 */
protected void paintShape(Painter aPntr)
{
    // Paint normal background
    super.paintShape(aPntr);
    
    // Clip to shape bounds (cache clip)
    aPntr.save();
    aPntr.clip(getBoundsInside());

    // Paint TextEditor (if editing text)
    if(isTextEditorSet())
        paintTextEditor(aPntr, getTextEditor());
    
    // Paint TextBox
    getTextBox().paint(aPntr);
    
    // Restore
    aPntr.restore();
}

/**
 * Paints a given TextEditor.
 */
protected void paintTextEditor(Painter aPntr, RMTextEditor aTE)
{
    // Get selection path
    Shape path = aTE.getSelPath();

    // If empty selection, draw caret
    if(aTE.isSelEmpty() && path!=null) {
        aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); // Set color and stroke of cursor
        aPntr.setAntialiasing(false); aPntr.draw(path); aPntr.setAntialiasing(true); // Draw cursor
    }

    // If selection, get selection path and fill
    else { aPntr.setColor(new Color(128, 128, 128, 128)); aPntr.fill(path); }

    // If spell checking, get path for misspelled words and draw
    if(aTE.isSpellChecking() && aTE.length()>0) {
        Shape spath = aTE.getSpellingPath();
        if(spath!=null) { aPntr.setColor(Color.RED); aPntr.setStroke(Stroke.StrokeDash1); aPntr.draw(spath);
            aPntr.setColor(Color.BLACK); aPntr.setStroke(Stroke.Stroke1); }
    }
}

/**
 * Override to catch XString and TextEditor changes.
 */
protected void richTextDidPropChange(PropChange aPC)
{
    _pcs.fireDeepChange(this, aPC);
    repaint();
}

/**
 * Override to do home-brew layout.
 */
public void revalidate()
{
    if(_textBox!=null) updateTextBox();
    if(getLinkedText()!=null) { getLinkedText().revalidate(); getLinkedText().repaint(); }
}

/**
 * Standard clone implementation.
 */
public RMTextShape clone()
{
    // Get normal shape clone, clone XString, clear layout and return
    RMTextShape clone = (RMTextShape)super.clone();
    clone._xstr = null; clone._textBox = null; clone._textEdtr = null;
    clone._richTextLsnr = pc -> richTextDidPropChange(pc);
    if(_xstr!=null) clone.setXString(_xstr.clone());
    return clone;
}

/**
 * Override to support margin copy.
 */
public void copyShape(RMShape aShape)
{
    super.copyShape(aShape);
    RMTextShape other = aShape instanceof RMTextShape? (RMTextShape)aShape : null; if(other==null) return;
    setMargin(other.getMargin());
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name to text
    XMLElement e = super.toXML(anArchiver); e.setName("text");
    
    // Archive Margin, AlignmentY
    if(getMargin()!=getMarginDefault()) e.add("margin", getMarginString());
    if(_alignY!=AlignY.Top) e.add("valign", getAlignmentY().toString().toLowerCase());
    
    // Archive Wraps, PerformsWrap
    if(_wraps!=0) e.add("wrap", _wraps==WRAP_BASIC? "wrap" : "shrink");
    if(_performsWrap) e.add("WrapAround", true);
    
    // Archive CoalesceNewlines, DrawsSelectionRect
    if(_coalesceNewlines) e.add("coalesce-newlines", true);
    if(_drawsSelectionRect) e.add("draw-border", true);
    
    // Archive xstring
    if(!(this instanceof RMLinkedText)) {
        
        // Get the xml element for the xstring
        XMLElement xse = anArchiver.toXML(getXString());
        
        // Add individual child elements to this text's xml element
        for(int i=0, iMax=xse.size(); i<iMax; i++)
            e.add(xse.get(i));
    }
    
    // If linked text present, archive reference to it (it should be archived as normal part of shape hierarchy)
    if(getLinkedText()!=null)
        e.add("linked-text", anArchiver.getReference(getLinkedText()));
    
    // If there is a path shape, archive path shape
    if(getPathShape()!=null) {
        
        // Get path shape and an element (and add element to master element)
        RMShape pathShape = getPathShape();
        XMLElement pathShapeElement = new XMLElement("path-shape");
        e.add(pathShapeElement);
        
        // Archive path shape to path-shape element
        XMLElement pathShapeElementZero = anArchiver.toXML(pathShape);
        pathShapeElement.add(pathShapeElementZero);
    }
    
    // Archive PDF options
    if(isEditable()) e.add(Editable_Prop, true);
    if(isMultiline()) e.add(Multiline_Prop, true);
    
    // Return element for this shape
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXML(anArchiver, anElement);
    
    // Unarchive Margin, AlignmentY
    if(anElement.hasAttribute("margin")) setMarginString(anElement.getAttributeValue("margin"));
    if(anElement.hasAttribute("valign"))
        setAlignmentY(SnapUtils.valueOfIC(AlignY.class, anElement.getAttributeValue("valign")));
    
    // Unarchive Wraps, PerformsWrap
    String wrap = anElement.getAttributeValue("wrap", "none");
    if(wrap.equals("wrap")) setWraps(WRAP_BASIC); else if(wrap.equals("shrink")) setWraps(WRAP_SCALE);
    setPerformsWrap(anElement.getAttributeBoolValue("WrapAround"));
    
    // Unarchive CoalesceNewlines, DrawsSelectionRect
    setCoalesceNewlines(anElement.getAttributeBoolValue("coalesce-newlines"));
    if(anElement.getAttributeBoolValue("draw-border")) setDrawsSelectionRect(true);
    
    // Unarchive xString
    if(!(this instanceof RMLinkedText))
        getXString().fromXML(anArchiver, anElement);
    
    // Register for finish call
    anArchiver.getReference(anElement);
    
    // Unarchive path-shape if present
    if(anElement.get("path-shape")!=null) {
        
        // Get the dedicated path-shape element and its first child (the actual path-shape element)
        XMLElement pathShapeElement = anElement.get("path-shape");
        XMLElement pathShapeElementZero = pathShapeElement.get(0);
        
        // Unarchive the path shape and set
        RMShape pathShape = (RMShape)anArchiver.fromXML(pathShapeElementZero, null);
        setPathShape(pathShape);
    }
    
    // Unarchive PDF options
    if(anElement.hasAttribute(Editable_Prop)) setEditable(anElement.getAttributeBoolValue(Editable_Prop));
    if(anElement.hasAttribute(Multiline_Prop)) setMultiline(anElement.getAttributeBoolValue(Multiline_Prop));
    
    // Return this shape
    return this;
}

/**
 * XML reference unarchival - to unarchive linked text.
 */
public void fromXMLFinish(XMLArchiver anArchiver, XMLElement anElement)
{
    // If linked-text, get referenced linked text and set
    if(!anElement.hasAttribute("linked-text")) return;
    RMLinkedText linkedText = (RMLinkedText)anArchiver.getReference("linked-text", anElement);
    setLinkedText(linkedText);
}

/**
 * Standard toSring implementation.
 */
public String toString()
{
    String string = super.toString();
    string = string.substring(0, string.length() - 1);
    return string + ", \"" + getXString() + "\"]";
}

}