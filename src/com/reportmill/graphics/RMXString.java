/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.*;
import static com.reportmill.graphics.RMTextStyle.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.text.*;
import snap.util.*;

/**
 * An RMXString is like a String that lets you apply attributes, like fonts and colors, to character ranges. These
 * character ranges with common attributes are represented internally as the inner class Run.
 * <p>
 * You might use it like this:
 * <p><blockquote><pre>
 *    RMXString xstring = new RMXString("Hello World", RMColor.red);
 *    xstring.addAttribute(RMFont.getFont("Arial Bold", 12), 0, 5);
 *    xstring.addAttribute(RMFont.getFont("Arial BoldItalic", 12), 6, xstring.length());
 * </pre></blockquote><p>
 */
public class RMXString implements Cloneable, CharSequence, RMTypes, XMLArchiver.Archivable {

    // The RichText
    private RichText  _richText;

    /**
     * Creates an empty RMXString.
     */
    public RMXString()
    {
        _richText = new RichText();
    }

    /**
     * Creates an empty RMXString.
     */
    public RMXString(RichText aRT)
    {
        _richText = aRT;
    }

    /**
     * Creates an RMXString initialized with the given String and no attributes.
     */
    public RMXString(CharSequence theChars)
    {
        this();
        addChars(theChars);
    }

    /**
     * Creates an RMXString initialized with the given String with all characters set to the given attributes.
     */
    public RMXString(CharSequence theChars, Object... theAttrs)
    {
        this();
        addChars(theChars, theAttrs);
    }

    /**
     * Returns the RichText.
     */
    public RichText getRichText()  { return _richText; }

    /**
     * Returns the simple String represented by this RMXString.
     */
    public String getText()
    {
        return toString();
    }

    /**
     * The length.
     */
    public int length()  { return _richText.length(); }

    /**
     * Returns the char at given index.
     */
    public char charAt(int anIndex)  { return _richText.charAt(anIndex); }

    /**
     * Returns a subsequence.
     */
    public CharSequence subSequence(int aStart, int anEnd)  { return _richText.subSequence(aStart, anEnd); }

    /**
     * Sets the simple String represented by this RMXString.
     */
    public void setText(String aString)
    {
        replaceChars(aString, 0, length());
    }

    /**
     * Returns the index within this string of the first occurrence of the given substring.
     */
    public int indexOf(String aString)
    {
        return toString().indexOf(aString);
    }

    /**
     * Returns the index within this string of first occurrence of given substring, starting at given index.
     */
    public int indexOf(String aString, int aStart)
    {
        return toString().indexOf(aString, aStart);
    }

    /**
     * Appends the given String to the end of this XString.
     */
    public void addChars(CharSequence theChars)
    {
        addChars(theChars, length());
    }

    /**
     * Adds chars at index.
     */
    public void addChars(CharSequence theChars, int anIndex)
    {
        addChars(theChars, null, anIndex);
    }

    /**
     * Appends the given string to this XString, with the given attributes, at the given index.
     */
    public void addChars(CharSequence theChars, TextStyle aStyle, int anIndex)
    {
        if (theChars.length() == 0) return;
        _richText.addChars(theChars, aStyle, anIndex);
    }

    /**
     * Appends the given chars with the given attribute(s).
     */
    public void addChars(CharSequence theChars, Object... theAttrs)
    {
        TextStyle style = _richText.getStyleForCharIndex(length());
        Object attr0 = theAttrs != null && theAttrs.length > 0 ? theAttrs[0] : null;
        if (attr0 instanceof TextStyle) style = (TextStyle) attr0;
        else if (attr0 instanceof RMTextStyle) style = ((RMTextStyle) attr0)._style;
        else if (attr0 != null) style = style.copyFor(theAttrs);
        addChars(theChars, style, length());
    }

    /**
     * Appends the given string to the end of this XString, with the given attributes.
     */
    public void addChars(CharSequence theChars, Map theAttrs)
    {
        int index = length();
        TextStyle style = _richText.getStyleForCharIndex(index);
        if (theAttrs != null) style = style.copyFor(theAttrs);
        addChars(theChars, style, index);
    }

    /**
     * Removes characters in given range.
     */
    public void removeChars(int aStart, int anEnd)
    {
        _richText.removeChars(aStart, anEnd);
    }

    /**
     * Replaces chars in given range, with given String.
     */
    public void replaceChars(CharSequence theChars, int aStart, int anEnd)
    {
        replaceChars(theChars, null, aStart, anEnd);
    }

    /**
     * Replaces chars in given range, with given String, using the given attributes.
     */
    public void replaceChars(CharSequence theChars, RMTextStyle aStyle, int aStart, int anEnd)
    {
        TextStyle style = null;
        if (aStyle != null) style = aStyle._style;
        _richText.replaceChars(theChars, style, aStart, anEnd);
    }

    /**
     * Adds an XString to this string at given index.
     */
    public void addString(RMXString xStr, int anIndex)
    {
        _richText.addTextDoc(xStr._richText, anIndex);
    }

    /**
     * Replaces the chars in given range, with given XString.
     */
    public void replaceString(RMXString xStr, int aStart, int aEnd)
    {
        _richText.removeChars(aStart, aEnd);
        _richText.addTextDoc(xStr._richText, aStart);
    }

    /**
     * Returns the XString head run.
     */
    public RMXStringRun getRun()
    {
        return getRun(0);
    }

    /**
     * Returns the number of runs in this XString.
     */
    public int getRunCount()
    {
        List<TextLine> richTextLines = _richText.getLines();
        int runCount = 0;
        for (TextLine textLine : richTextLines)
            runCount += textLine.getRunCount();
        return runCount;
    }

    /**
     * Returns the specific Run at the given index in this XString.
     */
    public RMXStringRun getRun(int anIndex)
    {
        List<TextLine> richTextLines = _richText.getLines();
        int index = anIndex;
        RMXStringRun run = null;

        // Iterate over lines
        for (TextLine line : richTextLines) {
            int runCount = line.getRunCount();
            if (index < runCount) {
                TextRun textRun = line.getRun(index);
                run = new RMXStringRun(this, textRun);
                break;
            }
            else index -= runCount;
        }

        // Return
        return run;
    }

    /**
     * Returns the last run in this XString (convenience).
     */
    public RMXStringRun getRunLast()
    {
        int runCount = getRunCount();
        return getRun(runCount - 1);
    }

    /**
     * Returns the XString run that contains or ends at given index.
     */
    public RMXStringRun getRunForCharIndex(int anIndex)
    {
        TextRun run = _richText.getRunForCharIndex(anIndex);
        return new RMXStringRun(this, run);
    }

    /**
     * Returns the XString run that contains or ends at given index.
     */
    public RMXStringRun getRunForCharRange(int startIndex, int endIndex)
    {
        TextRun run = _richText.getRunForCharRange(startIndex, endIndex);
        return new RMXStringRun(this, run);
    }

    /**
     * Returns the text style for the run at the given character index.
     */
    public RMTextStyle getStyleForCharIndex(int anIndex)
    {
        RMXStringRun textStyle = getRunForCharIndex(anIndex);
        return textStyle.getStyle();
    }

    /**
     * Returns the text style for the run at the given character index.
     */
    public RMTextStyle getStyleForCharRange(int startIndex, int endIndex)
    {
        RMXStringRun run = getRunForCharRange(startIndex, endIndex);
        return run.getStyle();
    }

    /**
     * Sets the text style for given range.
     */
    public void setStyle(RMTextStyle aStyle, int aStart, int anEnd)
    {
        _richText.setStyle(aStyle._style, aStart, anEnd);
    }

    /**
     * Applies the given attribute to whole xstring, assuming it's a basic attr types (font, color, etc.).
     */
    public void setAttribute(Object anAttr)
    {
        setAttribute(anAttr, 0, length());
    }

    /**
     * Applies the given attribute to the given character range, assuming it's a basic attr type (font, color, etc.).
     */
    public void setAttribute(Object anAttr, int aStart, int anEnd)
    {
        String key = RMTextStyle.getStyleKey(anAttr);
        if (key != null) setAttribute(key, anAttr, aStart, anEnd);
    }

    /**
     * Adds a given attribute of given type to the whole string.
     */
    public void setAttribute(String aKey, Object anAttr)
    {
        setAttribute(aKey, anAttr, 0, length());
    }

    /**
     * Sets a given attribute to a given value for a given range.
     */
    public void setAttribute(String aKey, Object aVal, int aStart, int aEnd)
    {
        _richText.setStyleValue(aKey, aVal, aStart, aEnd);
    }

    /**
     * Returns the current font at the given character index.
     */
    public RMFont getFontAt(int anIndex)
    {
        return getRunForCharIndex(anIndex).getFont();
    }

    /**
     * Returns the current paragraph at the given character index.
     */
    public RMParagraph getParagraphAt(int anIndex)
    {
        TextLine line = _richText.getLineForCharIndex(anIndex);
        return new RMParagraph(line.getLineStyle());
    }

    /**
     * Sets the paragraph for the given character index range.
     */
    public void setParagraph(RMParagraph aPG, int aStart, int anEnd)
    {
        _richText.setLineStyle(aPG._lstyle, aStart, anEnd);
    }

    /**
     * Sets the xstring to be underlined.
     */
    public void setUnderlined(boolean aFlag)
    {
        setAttribute(UNDERLINE_KEY, aFlag ? 1 : null, 0, length());
    }

    /**
     * Returns the horizontal alignment of the first paragraph of the xstring.
     */
    public AlignX getAlignX()
    {
        return getParagraphAt(0).getAlignmentX();
    }

    /**
     * Sets the horizontal alignment of the xstring.
     */
    public void setAlignX(AlignX anAlignX)
    {
        if (anAlignX == AlignX.Full) _richText.setLineStyleValue(TextLineStyle.JUSTIFY_KEY, true, 0, length());
        else _richText.setLineStyleValue(TextLineStyle.ALIGN_KEY, anAlignX.hpos(), 0, length());
    }

    /**
     * Returns an XString for given char range.
     */
    public RMXString substring(int aStart, int aEnd)
    {
        return new RMXString(_richText.copyForRange(aStart, aEnd));
    }

    /**
     * Replaces any occurrence of consecutive newlines with a single newline.
     */
    public void coalesceNewlines()
    {
        // Iterate over occurrences of adjacent newlines (from back to font) and remove redundant newline chars
        String string = toString();
        for (int start = string.lastIndexOf("\n\n"); start >= 0; start = string.lastIndexOf("\n\n", start)) {
            int end = start + 1;
            while (start > 0 && string.charAt(start - 1) == '\n') start--;
            removeChars(start, end);
            string = toString();
        }

        // Also remove leading newline if present
        if (length() > 0 && charAt(0) == '\n')
            removeChars(0, 1);
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        return _richText.toXML(anArchiver);
    }

    /**
     * XML unarchival.
     */
    public RMXString fromXML(XMLArchiver anArch, XMLElement anElmt)
    {
        _richText.fromXML(anArch, anElmt);
        return this;
    }

    /**
     * Standard Object equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        RMXString other = anObj instanceof RMXString ? (RMXString) anObj : null;
        if (other == null) return false;
        return other._richText.equals(_richText);
    }

    /**
     * Returns a clone of this RMXString.
     */
    public RMXString clone()
    {
        RMXString clone;
        try { clone = (RMXString) super.clone(); }
        catch (Exception e) { throw new RuntimeException(e); }
        clone._richText = _richText.clone();
        return clone;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return _richText.getString();
    }

    /**
     * Performs @key@ substitution on an xstring.
     */
    public RMXString rpgClone(ReportOwner anRptOwner, Object userInfo, RMShape aShape, boolean doCopy)
    {
        // Declare local variable for resulting out-xstring and for whether something requested a recursive RPG run
        RMXString outString = this;
        boolean redo = false;

        // If userInfo provided, plug it into ReportMill
        if (userInfo != null && anRptOwner != null)
            anRptOwner.pushDataStack(userInfo);

        // Get range for first key found in string
        Range totalKeyRange = outString.nextKeyRangeAfterIndex(0, new Range());

        // While the inString still contains @key@ constructs, do substitution
        while (totalKeyRange.length() > 0) {

            // Get key start location (after @-sign) and length
            int keyStart = totalKeyRange.start + 1;
            int keyEnd = totalKeyRange.end - 1;
            Object valString = null;

            // Get the run at the given location
            RMXStringRun keyRun = outString.getRunForCharRange(keyStart, keyEnd);

            // If there is a key between the @-signs, evaluate it for substitution string
            if (keyEnd > keyStart) {

                // Get actual key string
                String keyString = outString.subSequence(keyStart, keyEnd).toString();

                // Get key string as key chain
                RMKeyChain keyChain = RMKeyChain.getKeyChain(keyString);

                // If keyChain hasPageReference, tell reportMill and skip this key
                if (aShape != null && keyChain.hasPageReference()) {
                    anRptOwner.addPageReferenceShape(aShape);
                    outString.nextKeyRangeAfterIndex(totalKeyRange.end, totalKeyRange);
                    continue;
                }

                // Get keyChain value
                Object val = RMKeyChain.getValue(anRptOwner, keyChain);

                // If val is list, replace with first value (or null)
                if (val instanceof List) {
                    List<?> list = (List<?>) val;
                    val = list.size() > 0 ? list.get(0) : null;
                }

                // If we found a String, then we'll just use it for key sub (although we to see if it's a KeyChain literal)
                if (val instanceof String) {

                    // Set string value to be substitution string
                    valString = val;

                    // If keyChain has a string literal, check to see if val is that string literal
                    if (keyChain.hasOp(RMKeyChain.Op.Literal) && !StringUtils.startsWithIC((String) val, "<html")) {
                        String string = val.toString();
                        int index = keyString.indexOf(string);

                        // If val is that string literal, get original xstring substring (with attributes)
                        if (index > 0 && keyString.charAt(index - 1) == '"' && keyString.charAt(index + string.length()) == '"') {
                            int start = index + keyStart;
                            valString = outString.substring(start, start + string.length());
                            redo = redo || string.contains("@");
                        }
                    }
                }

                // If we found an xstring, then we'll just use it for key substitution
                else if (val instanceof RMXString)
                    valString = val;

                    // If we found a keyChain, add @ signs and redo (this feature lets developers return an RMKeyChain)
                else if (val instanceof RMKeyChain) {
                    valString = "@" + val + "@";
                    redo = true;
                }

                // If val is Number, get format and change val to string (verify format type)
                else if (val instanceof Number) {
                    RMFormat format = keyRun.getFormat();
                    if (!(format instanceof RMNumberFormat)) format = RMNumberFormat.PLAIN;
                    valString = format.format(val);
                    TextStyle style = format.formatStyle(val);
                    if (style != null) valString = new RMXString((String) valString, style.getColor());
                }

                // If val is Date, get format and change val to string (verify format type)
                else if (val instanceof Date) {
                    RMFormat format = keyRun.getFormat();
                    if (!(format instanceof RMDateFormat)) format = RMDateFormat.defaultFormat;
                    valString = format.format(val);
                }

                // If value is null, either use current format's or Document's NullString
                else if (val == null) {
                    RMFormat fmt = keyRun.getFormat();
                    if (fmt != null)
                        valString = fmt.format(val);
                }

                // If object is none of standard types (Str, Num, Date, XStr or null), see if it will provide bytes
                else {

                    // Ask object for "bytes" method or attribute
                    Object bytes = RMKey.getValue(val, "bytes");

                    // If bytes is byte array, just set it
                    if (bytes instanceof byte[])
                        valString = new String((byte[]) bytes);

                        // If value is List, reset it so we don't get potential hang in toString
                    else if (val instanceof List)
                        valString = "<List>";

                        // If value is Map, reset to "Map" so we don't get potential hang in toString
                    else if (val instanceof Map)
                        valString = "<Map>";

                        // Set substitution value to string representation of provided object
                    else valString = val.toString();
                }

                // If substitution string is still null, replace it with document null-string
                if (valString == null)
                    valString = anRptOwner.getNullString() != null ? anRptOwner.getNullString() : "";
            }

            // If there wasn't a key between '@' signs, assume they wanted '@'
            else valString = "@";

            // If substitution string was found, perform substitution
            if (valString != null) {

                // If this is the first substitution, get a copy of outString
                if (outString == this && doCopy)
                    outString = clone();

                // If substitution string was raw string, perform replace (and possible rtf/html evaluation)
                if (valString instanceof String) {
                    String string = (String) valString;

                    // If string is HTML formatted text, parse into RMXString
                    if (StringUtils.startsWithIC(string, "<html"))
                        valString = RMEnv.getEnv().parseHTML(string, keyRun.getFont(), keyRun.getParagraph());

                        // If string is RTF formatted text, parse into RMXString
                    else if (string.startsWith("{\\rtf"))
                        valString = RMEnv.getEnv().parseRTF(string, keyRun.getFont());

                        // If string is normal string, just perform replace and update key range
                    else {
                        outString.replaceChars(string, totalKeyRange.start, totalKeyRange.end);
                        totalKeyRange.setLength(((String) valString).length());
                    }
                }

                // If substitution string is xstring, just do xstring replace
                if (valString instanceof RMXString) {
                    RMXString xstring = (RMXString) valString;
                    outString.replaceString(xstring, totalKeyRange.start, totalKeyRange.end);
                    totalKeyRange.setLength(xstring.length());
                }
            }

            // Get next totalKeyRange
            outString.nextKeyRangeAfterIndex(totalKeyRange.end, totalKeyRange);
        }

        // If userInfo was provided, remove it from ReportMill
        if (userInfo != null)
            anRptOwner.popDataStack();

        // If something requested a recursive RPG run, do it
        if (redo)
            outString = outString.rpgClone(anRptOwner, userInfo, aShape, false);

        // Return RPG string
        return outString;
    }

    /**
     * Returns the range of the next occurrence of @delimited@ text.
     */
    private Range nextKeyRangeAfterIndex(int anIndex, Range aRange)
    {
        // Get length of string (return bogus range if null)
        int length = length();
        if (length < 2)
            return aRange.set(-1, -1);

        // Get start of key (return if it is the last char)
        int startIndex = indexOf("@", anIndex);
        if (startIndex == length - 1) return aRange.set(startIndex, startIndex + 1);

        // If startRange of key was found, look for end
        if (startIndex >= 0) {
            int nextIndex = startIndex;
            while (++nextIndex < length) {
                char c = charAt(nextIndex);
                if (c == '"')
                    while ((++nextIndex < length) && (charAt(nextIndex) != '"')) ;
                else if (c == '@')
                    return aRange.set(startIndex, nextIndex + 1);
            }
        }

        // Set bogus range and return
        return aRange.set(-1, -1);
    }

    /**
     * A range class.
     */
    private static class Range {

        // Start/end
        int start, end;

        /** Constructor. */
        public Range()  { }

        /** Returns the range length */
        public int length()  { return end - start; }

        /** Sets the range length. */
        public void setLength(int aLength)  { end = start + aLength; }

        public Range set(int aStart, int anEnd)
        {
            start = aStart;
            end = anEnd;
            return this;
        }
    }
}