/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Font;
import snap.gfx.FontFile;

/**
 * This class represents a font for use in rich text.
 */
public class RMFont extends Font {
    
    // Some common fonts (using Arial since it seems more reliable on Windows & Mac)
    static public RMFont Helvetica10 = new RMFont("Arial", 10d);
    static public RMFont Helvetica12 = Helvetica10.deriveFont(12d);
    static public RMFont Helvetica14 = Helvetica10.deriveFont(14d);
    
/**
 * Creates a new font (Arial 10).
 */
public RMFont()  { }

/**
 * Returns the font for the given name and size.
 */
public RMFont(String aName, double aSize)  { super(aName, aSize); }

/**
 * Creates a font for the given font file and point size.
 */
protected RMFont(FontFile aFontFile, double aSize)  { super(aFontFile, aSize); }

/**
 * Returns the font for the given name and size (substitutes Arial if not found).
 */
public static RMFont getFont(String aName, double aSize)  { return new RMFont(aName, aSize); }

/**
 * Returns the user's default font.
 */
public static RMFont getDefaultFont()  { return new RMFont(); }

/**
 * Returns the bold version of this font.
 */
public RMFont getBold() { return get(super.getBold()); }

/**
 * Returns the italic version of this font.
 */
public RMFont getItalic() { return get(super.getItalic()); }

/**
 * Returns a font with the same family as the receiver but with the given size.
 */
public RMFont deriveFont(double aSize) { return get(super.deriveFont(aSize)); }

/**
 * Returns a font with the same family as the receiver but with size adjusted by given scale factor.
 */
public RMFont scaleFont(double aScale)  { return get(super.scaleFont(aScale)); }

/**
 * Returns an RMFont.
 */
public static RMFont get(Font aFont)
{
    if(aFont==null) return null;
    if(aFont instanceof RMFont) return (RMFont)aFont;
    return new RMFont(aFont.getFontFile(), aFont.getSize());
}

}