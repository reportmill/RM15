/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.*;
import snap.text.TextLine;
import snap.text.TextRun;
import snap.text.TextStyle;

/**
 * The Run class represents a range of characters in an xstring that share common attributes.
 * <p>
 * This class makes a point to treat its attributes map as read-only so they can be shared among multiple runs.
 */
public class RMXStringRun implements CharSequence {

    // The xstring that this run is a part of
    private RMXString _xstr;

    // The text line
    private TextLine _textLine;

    // The start/end char index of this run in string
    private int _start, _end;

    // The attributes of the Run (Font, Color, etc.)
    private TextStyle _style;

    /**
     * Creates a new run.
     */
    protected RMXStringRun(RMXString anXStr, TextRun aRun)
    {
        _xstr = anXStr;
        _textLine = aRun.getLine();
        _start = _textLine.getStartCharIndex() + aRun.getStartCharIndex();
        _end = _textLine.getStartCharIndex() + aRun.getEndCharIndex();
        _style = aRun.getStyle();
    }

    /**
     * Returns the start character index for this run.
     */
    public int start()  { return _start; }

    /**
     * Returns the end character index for this run.
     */
    public int end()  { return _end; }

    /**
     * Returns the length in characters for this run.
     */
    public int length()  { return _end - _start; }

    /**
     * CharSequence method returning character at given index.
     */
    public char charAt(int anIndex)
    {
        return _xstr.charAt(_start + anIndex);
    }

    /**
     * CharSequence method return character sequence for range.
     */
    public CharSequence subSequence(int aStart, int anEnd)
    {
        return _xstr.subSequence(_start + aStart, _start + anEnd);
    }

    /**
     * Returns the text style.
     */
    public RMTextStyle getStyle()
    {
        return new RMTextStyle(_style);
    }

    /**
     * Returns the font for this run.
     */
    public RMFont getFont()
    {
        return getStyle().getFont();
    }

    /**
     * Returns the color for this run.
     */
    public RMColor getColor()
    {
        return getStyle().getColor();
    }

    /**
     * Returns the format for this run.
     */
    public RMFormat getFormat()
    {
        return getStyle().getFormat();
    }

    /**
     * Returns the paragraph for this run.
     */
    public RMParagraph getParagraph()
    {
        return new RMParagraph(_textLine.getLineStyle());
    }

    /**
     * Returns whether this run is underlined.
     */
    public boolean isUnderlined()
    {
        return getStyle().isUnderlined();
    }

    /**
     * Returns the char spacing.
     */
    public float getCharSpacing()
    {
        return (float) getStyle().getCharSpacing();
    }

    /**
     * Return next run.
     */
    public RMXStringRun getNext()
    {
        if (_end == _xstr.length()) return null;
        return _xstr.getRunForCharIndex(_end);
    }

    /**
     * Returns a string representation of this run.
     */
    public String toString()
    {
        return _xstr.subSequence(_start, _end).toString();
    }
}