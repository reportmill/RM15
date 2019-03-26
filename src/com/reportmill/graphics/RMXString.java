/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.*;
import static com.reportmill.graphics.RMTextStyle.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.util.*;

/**
 * An RMXString is like a String that lets you apply attributes, like fonts and colors, to character ranges. These
 * character ranges with common attributes are represented internally as the inner class Run.
 *
 * You might use it like this:
 * <p><blockquote><pre>
 *    RMXString xstring = new RMXString("Hello World", RMColor.red);
 *    xstring.addAttribute(RMFont.getFont("Arial Bold", 12), 0, 5);
 *    xstring.addAttribute(RMFont.getFont("Arial BoldItalic", 12), 6, xstring.length());
 * </pre></blockquote><p>
 */
public class RMXString implements Cloneable, CharSequence, RMTypes, XMLArchiver.Archivable {

    // The RichText
    RichText             _rtext;

/**
 * Creates an empty RMXString.
 */
public RMXString()  { _rtext = new RichText(); }

/**
 * Creates an empty RMXString.
 */
public RMXString(RichText aRT)  { _rtext = aRT; }

/**
 * Creates an RMXString initialized with the given String and no attributes.
 */
public RMXString(CharSequence theChars)  { this(); addChars(theChars); }

/**
 * Creates an RMXString initialized with the given String with all characters set to the given attributes.
 */
public RMXString(CharSequence theChars, Object ... theAttrs)  { this(); addChars(theChars, theAttrs); }

/**
 * Returns the RichText.
 */
public RichText getRichText()  { return _rtext; }

/**
 * Returns the simple String represented by this RMXString.
 */
public String getText()  { return toString(); }

/**
 * The length.
 */
public int length()  { return _rtext.length(); }

/**
 * Returns the char at given index.
 */
public char charAt(int anIndex)  { return _rtext.charAt(anIndex); }

/**
 * Returns a subsequence.
 */
public CharSequence subSequence(int aStart, int anEnd)  { return _rtext.subSequence(aStart, anEnd); }

/**
 * Sets the simple String represented by this RMXString.
 */
public void setText(String aString)  { replaceChars(aString, 0, length()); }

/**
 * Returns the index within this string of the first occurrence of the given substring.
 */
public int indexOf(String aString)  { return toString().indexOf(aString); }

/**
 * Returns the index within this string of first occurrence of given substring, starting at given index.
 */
public int indexOf(String aString, int aStart)  { return toString().indexOf(aString, aStart); }

/**
 * Appends the given String to the end of this XString.
 */
public void addChars(CharSequence theChars)  { addChars(theChars, length()); }

/**
 * Adds chars at index.
 */
public void addChars(CharSequence theChars, int anIndex)  { addChars(theChars, null, anIndex); }

/**
 * Appends the given string to this XString, with the given attributes, at the given index.
 */
public void addChars(CharSequence theChars, TextStyle aStyle, int anIndex)
{
    if(theChars.length()==0) return;
    _rtext.addChars(theChars, aStyle, anIndex);
}

/**
 * Appends the given chars with the given attribute(s).
 */
public void addChars(CharSequence theChars, Object ... theAttrs)
{
    TextStyle style = _rtext.getStyleAt(length());
    Object attr0 = theAttrs!=null && theAttrs.length>0? theAttrs[0] : null;
    if(attr0 instanceof TextStyle) style = (TextStyle)attr0;
    else if(attr0 instanceof RMTextStyle) style = ((RMTextStyle)attr0)._style;
    else if(attr0!=null) style = style.copyFor(theAttrs);
    addChars(theChars, style, length());
}

/**
 * Appends the given string to the end of this XString, with the given attributes.
 */
public void addChars(CharSequence theChars, Map theAttrs)
{
    int index = length();
    TextStyle style = _rtext.getStyleAt(index); if(theAttrs!=null) style = style.copyFor(theAttrs);
    addChars(theChars, style, index);
}

/**
 * Removes characters in given range.
 */
public void removeChars(int aStart, int anEnd)  { _rtext.removeChars(aStart, anEnd); }

/**
 * Replaces chars in given range, with given String.
 */
public void replaceChars(CharSequence theChars, int aStart, int anEnd)  { replaceChars(theChars, null, aStart, anEnd); }

/**
 * Replaces chars in given range, with given String, using the given attributes.
 */
public void replaceChars(CharSequence theChars, RMTextStyle aStyle, int aStart, int anEnd)
{
    TextStyle style = null; if(aStyle!=null) style = aStyle._style;
    _rtext.replaceChars(theChars, style, aStart, anEnd);
}

/**
 * Adds an XString to this string at given index.
 */
public void addString(RMXString xStr, int anIndex)  { _rtext.addText(xStr._rtext, anIndex); }

/**
 * Replaces the chars in given range, with given XString.
 */
public void replaceString(RMXString xStr, int aStart, int aEnd)  { _rtext.replaceText(xStr._rtext, aStart, aEnd); }

/**
 * Returns the XString head run.
 */
public RMXStringRun getRun()  { return getRun(0); }

/**
 * Returns the number of runs in this XString.
 */
public int getRunCount()  { int rc = 0; for(RichTextLine ln : _rtext.getLines()) rc += ln.getRunCount(); return rc; }

/**
 * Returns the specific Run at the given index in this XString.
 */
public RMXStringRun getRun(int anIndex)
{
    int index = anIndex; RMXStringRun run = null;
    for(RichTextLine line : _rtext.getLines()) { int rc = line.getRunCount();
        if(index<rc) { run = new RMXStringRun(this, line, line.getRun(index)); break; } else index -= rc; }
    return run;
}

/**
 * Returns the last run in this XString (convenience).
 */
public RMXStringRun getRunLast()  { int rc = getRunCount(); return getRun(rc-1); }

/**
 * Returns the XString run that contains or ends at given index.
 */
public RMXStringRun getRunAt(int anIndex)
{
    RichTextLine line = _rtext.getLineAt(anIndex);
    RichTextRun run = line.getRunAt(anIndex - line.getStart());
    return new RMXStringRun(this, line, run);
}

/**
 * Returns the text style for the run at the given character index.
 */
public RMTextStyle getStyleAt(int anIndex)  { return getRunAt(anIndex).getStyle(); }
    
/**
 * Sets the text style for given range.
 */
public void setStyle(RMTextStyle aStyle, int aStart, int anEnd)  { _rtext.setStyle(aStyle._style, aStart, anEnd); }

/**
 * Applies the given attribute to whole xstring, assuming it's a basic attr types (font, color, etc.).
 */
public void setAttribute(Object anAttr)  { setAttribute(anAttr, 0, length()); }

/**
 * Applies the given attribute to the given character range, assuming it's a basic attr type (font, color, etc.).
 */
public void setAttribute(Object anAttr, int aStart, int anEnd)
{
    String key = RMTextStyle.getStyleKey(anAttr);
    if(key!=null) setAttribute(key, anAttr, aStart, anEnd);
}

/**
 * Adds a given attribute of given type to the whole string.
 */
public void setAttribute(String aKey, Object anAttr)  { setAttribute(aKey, anAttr, 0, length()); }

/**
 * Sets a given attribute to a given value for a given range.
 */
public void setAttribute(String aKey, Object aVal, int aStart, int aEnd)
{
    _rtext.setStyleValue(aKey, aVal, aStart, aEnd);
}

/**
 * Returns the current font at the given character index.
 */
public RMFont getFontAt(int anIndex)  { return getRunAt(anIndex).getFont(); }

/**
 * Returns the current paragraph at the given character index.
 */
public RMParagraph getParagraphAt(int anIndex)
{
    RichTextLine line = _rtext.getLineAt(anIndex);
    return new RMParagraph(line.getLineStyle());
}

/**
 * Sets the paragraph for the given character index range.
 */
public void setParagraph(RMParagraph aPG, int aStart, int anEnd)
{
    _rtext.setLineStyle(aPG._lstyle, aStart, anEnd);
}

/**
 * Sets the xstring to be underlined.
 */
public void setUnderlined(boolean aFlag)  { setAttribute(UNDERLINE_KEY, aFlag? 1 : null, 0, length()); }

/**
 * Returns the horizontal alignment of the first paragraph of the xstring.
 */
public AlignX getAlignX()  { return getParagraphAt(0).getAlignmentX(); }

/**
 * Sets the horizontal alignment of the xstring.
 */
public void setAlignX(AlignX anAlignX)
{
    if(anAlignX==AlignX.Full) _rtext.setLineStyleValue(TextLineStyle.JUSTIFY_KEY, true, 0, length());
    else _rtext.setLineStyleValue(TextLineStyle.ALIGN_KEY, anAlignX.hpos(), 0, length());
}

/**
 * Returns an XString for given char range.
 */
public RMXString substring(int aStart, int aEnd)  { return new RMXString(_rtext.subtext(aStart,aEnd)); }

/**
 * Replaces any occurrence of consecutive newlines with a single newline.
 */
public void coalesceNewlines()
{
    // Iterate over occurrences of adjacent newlines (from back to font) and remove redundant newline chars
    String string = toString();
    for(int start=string.lastIndexOf("\n\n"); start>=0; start=string.lastIndexOf("\n\n", start)) {
        int end = start + 1;
        while(start>0 && string.charAt(start-1)=='\n') start--;
        removeChars(start, end);
        string = toString();
    }
    
    // Also remove leading newline if present
    if(length()>0 && charAt(0)=='\n')
        removeChars(0, 1);
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)  { return _rtext.toXML(anArchiver); }

/**
 * XML unarchival.
 */
public RMXString fromXML(XMLArchiver anArch, XMLElement anElmt)  { _rtext.fromXML(anArch, anElmt); return this; }

/**
 * Standard Object equals implementation.
 */
public boolean equals(Object anObj)
{
    if(anObj==this) return true;
    RMXString other = anObj instanceof RMXString? (RMXString)anObj : null; if(other==null) return false;
    return other._rtext.equals(_rtext);
}
  
/**
 * Returns a clone of this RMXString.
 */
public RMXString clone()
{
    RMXString clone; try { clone = (RMXString)super.clone(); }
    catch(Exception e) { throw new RuntimeException(e); }
    clone._rtext = _rtext.clone();
    return clone;
}

/**
 * Standard toString implementation.
 */
public String toString()  { return _rtext.getString(); }

/**
 * Performs @key@ substitution on an xstring.
 */
public RMXString rpgClone(ReportOwner anRptOwner, Object userInfo, RMShape aShape, boolean doCopy)
{
    // Declare local variable for resulting out-xstring and for whether something requested a recursive RPG run
    RMXString outString = this;
    boolean redo = false;

    // If userInfo provided, plug it into ReportMill
    if(userInfo!=null && anRptOwner!=null)
        anRptOwner.pushDataStack(userInfo);

    // Get range for first key found in string
    Range totalKeyRange = outString.nextKeyRangeAfterIndex(0, new Range());
    
    // While the inString still contains @key@ constructs, do substitution
    while(totalKeyRange.length() > 0) {
        
        // Get key start location (after @-sign) and length
        int keyLocation = totalKeyRange.start + 1;
        int keyLength = totalKeyRange.length() - 2;
        Object valString = null;
        
        // Get the run at the given location
        RMXStringRun keyRun = outString.getRunAt(keyLocation);

        // If there is a key between the @-signs, evaluate it for substitution string
        if(keyLength > 0) {
            
            // Get actual key string
            String keyString = outString.subSequence(keyLocation, keyLocation + keyLength).toString();
            
            // Get key string as key chain
            RMKeyChain keyChain = RMKeyChain.getKeyChain(keyString);

            // If keyChain hasPageReference, tell reportMill and skip this key
            if(aShape!=null && keyChain.hasPageReference()) {
                anRptOwner.addPageReferenceShape(aShape);
                outString.nextKeyRangeAfterIndex(totalKeyRange.end, totalKeyRange);
                continue;
            }
            
            // Get keyChain value
            Object val = RMKeyChain.getValue(anRptOwner, keyChain);
            
            // If val is list, replace with first value (or null)
            if(val instanceof List) { List list = (List)val;
                val = list.size()>0? list.get(0) : null; }
                
            // If we found a String, then we'll just use it for key sub (although we to see if it's a KeyChain literal)
            if(val instanceof String) {
                
                // Set string value to be substitution string
                valString = val;
    
                // If keyChain has a string literal, check to see if val is that string literal
                if(keyChain.hasOp(RMKeyChain.Op.Literal) && !StringUtils.startsWithIC((String)val, "<html")) {
                    String string = val.toString();
                    int index = keyString.indexOf(string);
                    
                    // If val is that string literal, get original xstring substring (with attributes)
                    if(index>0 && keyString.charAt(index-1)=='"' && keyString.charAt(index+string.length())=='"') {
                        int start = index + keyLocation;
                        valString = outString.substring(start, start + string.length());
                        redo = redo || string.indexOf("@")>=0;
                    }
                }
            }

            // If we found an xstring, then we'll just use it for key substitution
            else if(val instanceof RMXString)
                valString = val;
                
            // If we found a keyChain, add @ signs and redo (this feature lets developers return an RMKeyChain)
            else if(val instanceof RMKeyChain) {
                valString = "@" + val.toString() + "@"; redo = true; }

            // If val is Number, get format and change val to string (verify format type)
            else if(val instanceof Number) {
                RMFormat format = keyRun.getFormat();
                if(!(format instanceof RMNumberFormat)) format = RMNumberFormat.PLAIN;
                valString = format.format(val);
                snap.gfx.TextStyle style = format.formatStyle(val);
                if(style!=null) valString = new RMXString((String)valString, style.getColor());
            }

            // If val is Date, get format and change val to string (verify format type)
            else if(val instanceof Date) {
                RMFormat format = keyRun.getFormat();
                if(!(format instanceof RMDateFormat)) format = RMDateFormat.defaultFormat;
                valString = format.format(val);
            }

            // If value is null, either use current format's or Document's NullString
            else if(val==null) {
                RMFormat fmt = keyRun.getFormat();
                if(fmt != null)
                    valString = fmt.format(val);
            }
            
            // If object is none of standard types (Str, Num, Date, XStr or null), see if it will provide bytes
            else {
                
                // Ask object for "bytes" method or attribute
                Object bytes = RMKey.getValue(val, "bytes");
                
                // If bytes is byte array, just set it
                if(bytes instanceof byte[])
                    valString = new String((byte[])bytes);
                
                // If value is List, reset it so we don't get potential hang in toString
                else if(val instanceof List)
                    valString = "<List>";
                
                // If value is Map, reset to "Map" so we don't get potential hang in toString
                else if(val instanceof Map)
                    valString = "<Map>";
                
                // Set substitution value to string representation of provided object
                else valString = val.toString();
            }

            // If substitution string is still null, replace it with document null-string
            if(valString == null) 
                valString = anRptOwner.getNullString()!=null? anRptOwner.getNullString() : "";
        }

        // If there wasn't a key between '@' signs, assume they wanted '@'
        else valString = "@";

        // If substitution string was found, perform substitution
        if(valString != null) {

            // If this is the first substitution, get a copy of outString
            if(outString==this && doCopy)
                outString = clone();

            // If substitution string was raw string, perform replace (and possible rtf/html evaluation)
            if(valString instanceof String) { String string = (String)valString;
                
                // If string is HTML formatted text, parse into RMXString
                if(StringUtils.startsWithIC(string, "<html"))
                    valString = RMHTMLParser.parse(string, keyRun.getFont(), keyRun.getParagraph());
                
                // If string is RTF formatted text, parse into RMXString
                else if(string.startsWith("{\\rtf"))
                    valString = RMRTFParser.parse(string, keyRun.getFont());
                
                // If string is normal string, just perform replace and update key range
                else {
                    outString.replaceChars(string, totalKeyRange.start, totalKeyRange.end);
                    totalKeyRange.setLength(((String)valString).length());
                }
            }
            
            // If substitution string is xstring, just do xstring replace
            if(valString instanceof RMXString) { RMXString xstring = (RMXString)valString;
                outString.replaceString(xstring, totalKeyRange.start, totalKeyRange.end);
                totalKeyRange.setLength(xstring.length());
            }
        }

        // Get next totalKeyRange
        outString.nextKeyRangeAfterIndex(totalKeyRange.end, totalKeyRange);
    }
    
    // If userInfo was provided, remove it from ReportMill
    if(userInfo!=null)
        anRptOwner.popDataStack();

    // If something requested a recursive RPG run, do it
    if(redo)
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
    int length = length(); if(length<2) return aRange.set(-1, -1);

    // Get start of key (return if it is the last char)
    int startIndex = indexOf("@", anIndex);
    if(startIndex==length-1) return aRange.set(startIndex, startIndex+1);

    // If startRange of key was found, look for end
    if(startIndex>=0) {
        int nextIndex = startIndex;
        while(++nextIndex < length) { char c = charAt(nextIndex);
            if(c=='"')
                while((++nextIndex<length) && (charAt(nextIndex)!='"'));
            else if(c=='@')
                return aRange.set(startIndex, nextIndex+1);
        }
    }
    
    // Set bogus range and return
    return aRange.set(-1, -1);
}

/**
 * A range class.
 */
private static class Range {
    int start, end;
    public int length()  { return end - start; }
    public void setLength(int aLength)  { end = start + aLength; } 
    public Range set(int aStart, int anEnd)  { start = aStart; end = anEnd; return this; }
}

}