/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.graphics.*;
import java.text.*;
import java.util.Locale;
import snap.gfx.TextStyle;
import snap.util.*;

/**
 * This subclass extends DecimalFormat to implement ReportMill's RMFormat interface and to support the extended features
 * "Negative in Red" and "Null String". It also has a toPattern like method, getFormatString, for the purposes of 
 * archival.
 *
 * Those getFormatString/setFormatString support arbitrary decimal and grouping separators. This may be dangerous since
 * when people use the default English versions of those, I think they change depending on the default JVM Locale. Only
 * dangerous if someone wants to generate English Separators from a foreign local, I guess.
 *
 * Also, our modified pattern format can be ambiguous if either decimal or grouping separator not included. In this case 
 * we assume any separator preceded by a '0' is the decimal separator. This seems reasonable since a decimal separator 
 * preceded by # could result in an empty string or a fractional number with no leading zero (like ".123" instead of
 * "0.123") which seems somewhat rare. And also a grouping separator preceded by 0 seems somewhat rare because it could
 * result in a long zero string like "0,000".
 *
 * I suppose we should instead have a syntax explicitly declaring decimal and group separators: "$ #,##0.00;ds=,;gs=.".
 */
public class RMNumberFormat implements RMFormat, Cloneable {
    
    // The decimal format
    DecimalFormat               _fmt = new DecimalFormat();
    
    // The string to be substituted when requested to format null
    String                      _nullString = "<N/A>";
    
    // Whether to show negative values in red
    boolean                     _negativeInRed;
    
    // Format Symbols
    DecimalFormatSymbols        _fmtSyms;
    
    // Default Locale
    static Locale               _defaultLocale;
    
    // Some common formatters
    public static RMNumberFormat PLAIN = new RMNumberFormat("0.##");
    public static RMNumberFormat BASIC = new RMNumberFormat("#,##0");
    public static RMNumberFormat DOLLARS = new RMNumberFormat("$ #,##0.00");
    public static RMNumberFormat CURRENCY = new RMNumberFormat("#,##0.00");

/**
 * Static to modify PLAIN format.
 */
static
{
    PLAIN._fmt.setDecimalSeparatorAlwaysShown(false);
    CURRENCY.setLocalCurrencySymbolUsed(true);
}

/**
 * Creates an uninitialized number format.
 */
public RMNumberFormat()
{
    // If DefaultLocale, create and set DecimalFormatSymbols
    if(_defaultLocale!=null)
        setDecimalFormatSymbols(new DecimalFormatSymbols(_defaultLocale));
    else if(SnapUtils.isTeaVM)
        _fmt = (DecimalFormat)NumberFormat.getInstance(Locale.ENGLISH);
    
    // Get FormatSymbols
    _fmtSyms = _fmt.getDecimalFormatSymbols();
}

/**
 * Creates a number format for the given format string.
 */
public RMNumberFormat(String aFormatString)  { this(); setPattern(aFormatString); }

/**
 * Returns the String that is substituted when this format is asked to provide stringForObjectValue(null).
 */
public String getNullString()  { return _nullString; }

/**
 * Sets the String that is substituted when this format is asked to provide stringForObjectValue(null).
 */
public void setNullString(String aString)  { _nullString = aString; }

/**
 * Returns whether this format shows negative values in red.
 */
public boolean isNegativeInRed()  { return _negativeInRed; }

/**
 * Sets whether this format shows negative values in red.
 */
public void setNegativeInRed(boolean aFlag)  { _negativeInRed = aFlag; }

/**
 * Returns the Currency symbol used for this locale.
 */
public String getLocalCurrencySymbol()
{
    // Get the standard
    String symbol = _fmt.getCurrency().getSymbol();
    
    // If the currency symbol also uses the same character as the group or decimal separator, such as
    // "Kr." or "SFr.", putting it in the pattern will cause the DecimalFormat to throw an exception.
    String separators[] = {getDecimalSeparator(), getThousandsSeparator(), ".", ","};
    
    // just replace them with spaces
    for(int i=0; i<separators.length; ++i)
        if(separators[i]!=null && separators[i].length()>0)
            symbol = symbol.replace(separators[i], " ");
    
    // trim up the ends
    return symbol.trim();
}

/**
 * Returns whether local currency symbol is used.
 */
public boolean isLocalCurrencySymbolUsed()
{
    // Get pattern and return whether $ or local currency symbol or international currency symbol is used
    String pattern = _fmt.toPattern();
    return pattern.indexOf("$")>=0 || pattern.indexOf(getLocalCurrencySymbol())>=0;
}

/**
 * Sets whether local currency symbol is used.
 */
public void setLocalCurrencySymbolUsed(boolean aFlag)
{
    // Get pattern and currency symbol
    String pattern = _fmt.toPattern();
    String currencySymbol = getLocalCurrencySymbol();
    
    // If currency symbol isn't used, but requested, add it
    if(!isLocalCurrencySymbolUsed() && aFlag) {
        
        // Add currency symbol to pattern and negative pattern
        pattern = currencySymbol + " " + pattern;
        pattern = StringUtils.replace(pattern, ";", ";" + currencySymbol + " ");
        
        // Apply pattern
        _fmt.applyPattern(pattern);
    }
    
    // If currency symbol is used and not wanted, remove it
    else if(isLocalCurrencySymbolUsed() && !aFlag) {
        
        // Get pattern pieces (positive & negative)
        String patterns[] = pattern.split(";");
        
        // Strip out $ or local currency symbol or international symbol
        for(int i=0; i<patterns.length; i++) {
            patterns[i] = StringUtils.delete(patterns[i], "$");
            patterns[i] = StringUtils.delete(patterns[i], currencySymbol);
            patterns[i] = patterns[i].trim();
        }
        
        // Join back together
        pattern = patterns[0] + (patterns.length==2? patterns[1] : "");
        
        // Apply pattern
        _fmt.applyPattern(pattern);
    }
}

/**
 * Returns whether currency symbol is used (dollars, local or international).
 */
public boolean isCurrencySymbolUsed()
{
    // Get pattern and return whether $ or local currency symbol or international currency symbol is used
    String pattern = _fmt.toPattern();
    return pattern.indexOf("$")>=0 || pattern.indexOf(_fmt.getCurrency().getSymbol())>=0 || pattern.indexOf("\u00A4")>=0;
}

/**
 * Returns whether percent symbol is used.
 */
public boolean isPercentSymbolUsed()
{
    // Get pattern and return whether $ or local currency symbol or international currency symbol is used
    String pattern = _fmt.toPattern();
    return pattern.indexOf("%")>=0;
}

/**
 * Returns whether percent symbol is used.
 */
public void setPercentSymbolUsed(boolean aFlag)
{
    // If percent symbol is not used but requested, add it
    if(!isPercentSymbolUsed() && aFlag)
        _fmt.applyPattern(_fmt.toPattern()+"%");
    
    // If percent symbol is used but not wanted, remove it
    else if(isPercentSymbolUsed() && !aFlag)
        _fmt.applyPattern(StringUtils.delete(_fmt.toPattern(), "%").trim());
}

/**
 * Returns the thousands separator.
 */
public String getThousandsSeparator()  { return _fmt.isGroupingUsed()? "" + _fmtSyms.getGroupingSeparator() :null; }

/**
 * Sets the thousands separator.
 */
public void setThousandsSeparator(String aValue)
{
    if(aValue!=null && aValue.length()>0) {
        char c = aValue.charAt(0);
        _fmtSyms.setGroupingSeparator(c);
        setDecimalFormatSymbols(_fmtSyms);
        _fmt.setGroupingUsed(true);
        _fmt.setGroupingSize(3);
    }
    else _fmt.setGroupingUsed(false);
}

/**
 * Returns the decimal separator.
 */
public String getDecimalSeparator()  { return "" + _fmtSyms.getDecimalSeparator(); }

/**
 * Sets the decimal separator.
 */
public void setDecimalSeparator(String aValue)
{
    if(aValue!=null && aValue.length()>0) {
        char c = aValue.charAt(0);
        _fmtSyms.setDecimalSeparator(c);
        setDecimalFormatSymbols(_fmtSyms);
    }
}

/**
 * Returns minimum fraction digits.
 */
public int getMinimumFractionDigits()  { return _fmt.getMinimumFractionDigits(); }

/**
 * Sets minimum fraction digits.
 */
public void setMinimumFractionDigits(int aVal)  { _fmt.setMinimumFractionDigits(aVal); }

/**
 * Returns maximum fraction digits.
 */
public int getMaximumFractionDigits()  { return _fmt.getMaximumFractionDigits(); }

/**
 * Sets maximum fraction digits.
 */
public void setMaximumFractionDigits(int aVal)  { _fmt.setMaximumFractionDigits(aVal); }

/**
 * Returns whether grouping used.
 */
public boolean isGroupingUsed()  { return _fmt.isGroupingUsed(); }

/**
 * Sets minimum fraction digits.
 */
public void setGroupingUsed(boolean aVal)  { _fmt.setGroupingUsed(aVal); }

/**
 * Returns the format string for this format. This is basically the default toPattern() of DecimalFormat, except that
 * it allows for a arbitrary decimal and grouping separators.
 */
public String getPattern()
{
    // Get default pattern
    String p = _fmt.toPattern();
    
    // If non-standard decimal or grouping separators, substitute them.
    if(_fmtSyms.getDecimalSeparator()!='.' || (_fmt.isGroupingUsed() && _fmtSyms.getGroupingSeparator()!=',')) {
        if(_fmt.isGroupingUsed()) p = StringUtils.replace(p, ",", "xxx");
        p = StringUtils.replace(p, ".", "" + _fmtSyms.getDecimalSeparator());
        if(_fmt.isGroupingUsed()) p = StringUtils.replace(p, "xxx", "" + _fmtSyms.getGroupingSeparator());
    }

    // Return pattern
    return p;
}

/**
 * Sets the format string for this format. This is basically the default applyPattern() of DecimalFormat, except that
 * it allows for arbitrary decimal and grouping separators.
 */
public void setPattern(String aFormat)
{
    // If we find "[Red]" in pattern, set _negativeInRed and remove it
    String format = aFormat;
    _negativeInRed = format.indexOf("[Red]")>0;
    if(_negativeInRed)
        format = StringUtils.delete(format, "[Red]");
    
    // Try to discern decimal and grouping separator chars
    //   1. We start from end of positive portion
    //   2. We assume 0x0 or 0x# indicates decimal sep (x). If decimal sep wasn't preceded by 0, format could
    //        result in empty string or fraction with no leading zero, which seems somewhat unlikely.
    //   3. We assume #x0 or #x# indicates grouping sep (x). If grouping sep wasn't preceded by #, format could
    //        result in an unusually long zero string like "0,000.00", which seems undesirable and unlikely.
    //   4. If format includes both symbols everything is fine (we assume left one is grouping and right is decimal).
    char dsep = (char)0, gsep = (char)0;
    int length = format.indexOf(';'); if(length<0) length = format.length();
    for(int i=length-1; i>1; i--) { char c1 = format.charAt(i), c2 = format.charAt(i-1), c3 = format.charAt(i-2);
        if((c1=='0' || c1=='#') && (c2!='0' && c2!='#' && c2!=' ') && (c3=='0' || c3=='#')) {
            if(gsep!=0 && dsep==0) { dsep = gsep; gsep = c2; break; }
            else if(dsep==0 && c3=='0') dsep = c2;
            else if(gsep==0) gsep = c2;
            else break;
        }
    }
    
    // Substitute decimal and grouping separators if present and non-standard
    if(gsep>0 && gsep!=',') format = StringUtils.replace(format, "" + gsep, "xxx");
    if(dsep>0 && dsep!='.') format = StringUtils.replace(format, "" + dsep, ".");
    if(gsep>0 && gsep!=',') format = StringUtils.replace(format, "xxx", ",");
        
    // Try default DecimalFormat parsing with new format
    try {
        _fmt.applyPattern(format);
        _fmtSyms = _fmt.getDecimalFormatSymbols();
        if(dsep>0 && dsep!='.') _fmtSyms.setDecimalSeparator(dsep);
        if(gsep>0 && gsep!=',') _fmtSyms.setGroupingSeparator(gsep);
        setDecimalFormatSymbols(_fmtSyms);
    }
    
    // If that failed, re-throw exception
    catch(Exception e) { throw new IllegalArgumentException("Malformed format string \"" + aFormat + "\""); }
}

/**
 * Returns a formatted version of the given object.
 */
public String format(Object anObj)
{
    // If object is number, do normal formatting
    Object num = getNumber(anObj);
    if(num!=null)
        return _fmt.format(num);
    
    // If object is null, return null-string, otherwise complain "Not a number!"
    return anObj==null? _nullString : "<not a number>";
}

/**
 * Returns the format style.
 */
public TextStyle formatStyle(Object anObj)
{
    Number num = getNumber(anObj);
    if(num!=null && num.doubleValue()<0 && _negativeInRed)
        return TextStyle.DEFAULT.copyFor(RMColor.red);
    return null;
}

/**
 * Returns the number for given object.
 */
private Number getNumber(Object anObj)
{
    // Round number before it gets to Java, so we don't get "-0"
    Number num = anObj instanceof Number? (Number)anObj : null; if(num==null) return null;
    double value = num.doubleValue();
    if(value<0 && value>-1) {
        int dp = _fmt.getMaximumFractionDigits();
        double dpm = Math.pow(10, dp);
        num = Math.round(value*dpm)/dpm;
    }

    // For legacy reasons, percent formats are divided by 100. Java formats don't do this - maybe it can go one day.
    String str = _fmt.getPositiveSuffix(); if(str==null) str = ""; // For TeaVM
    if(str.indexOf('%') >= 0)
        num = num.doubleValue()/100;
        
    // Return number
    return num;
}

/**
 * Returns the default locale.
 */
public static Locale getDefaultLocale()  { return _defaultLocale; }

/**
 * Sets the default locale.
 */
public static void setDefaultLocale(Locale aLocale)  { _defaultLocale = aLocale; }

/**
 * Tries to parse a number from given string using this format.
 */
public Number parse(String aStr)
{
    try { return _fmt.parse(aStr); }
    catch(Exception e) { return null; }
}

/**
 * For TeaVM.
 */
void setDecimalFormatSymbols(DecimalFormatSymbols aDFS)
{
    if(SnapUtils.isTeaVM) return;
    _fmt.setDecimalFormatSymbols(aDFS);
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and super
    if(anObj==this) return true;
    if(!super.equals(anObj)) return false;
    
    // Get other number format
    RMNumberFormat other = (RMNumberFormat)anObj;
    
    // Check NullString and NegativeInRed
    if(!SnapUtils.equals(other._nullString, _nullString)) return false;
    if(other._negativeInRed!=_negativeInRed) return false;
    
    // Return true since all other checks passed
    return true;
}

/**
 * Standard clone implementation.
 */
public RMNumberFormat clone()
{
    RMNumberFormat clone = null; try { clone = (RMNumberFormat)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
    clone._fmt = (DecimalFormat)_fmt.clone(); clone._fmtSyms = _fmt.getDecimalFormatSymbols();
    return clone;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named format with type "number"
    XMLElement e = new XMLElement("format"); e.add("type", "number");
    
    // Archive Pattern, NullString, NegativeInRed
    e.add("pattern", getPattern());
    if(_nullString!=null && _nullString.length()>0) e.add("null-string", _nullString);
    if(_negativeInRed) e.add("negative-red", true);
    return e; // Return xml element
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Pattern, NullString, NegativeInRed
    setPattern(anElement.getAttributeValue("pattern"));
    _nullString = anElement.getAttributeValue("null-string");
    _negativeInRed = anElement.getAttributeBoolValue("negative-red");
    return this;  // Return this number format
}

}