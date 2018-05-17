/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;
import java.util.*;

/**
 * This class provides UI for showing the formatter from the currently selected shape and editing or changing it.
 */
public class FormatPanel extends RMEditorPane.SupportPane {
    
    // A list of standard number formats
    List <RMNumberFormat>  _numberFormats = new ArrayList();
    
    // A list of standard date formats
    List <RMDateFormat>    _dateFormats = new ArrayList();

    // Sample date object to be used to display date formats
    Date                   _sampleDate = new Date();
    
    // Sample positive number to be used to display number formats
    Float                  _sampleNumberPos = 1234.567f;
    
    // Sample negative number to be used to display number formats
    Float                  _sampleNumberNeg = -1234.567f;

    // The shared instance
    static FormatPanel     _shared; // This should go

/**
 * Creates a new FormatPanel.
 */
public FormatPanel(RMEditorPane anEP)
{
    // Do normal version and set shared instance
    super(anEP); _shared = this;
    
    // Load standard number formats from preferences
    String nums = Prefs.get().get("NumberFormats", getDefaultNumberFormatsString());
    setNumberFormatsString(nums);
    
    // Load standard date formats from preferences
    String dates = Prefs.get().get("DateFormats2", getDefaultDateFormatsString());
    setDateFormatsString(dates);
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Set table items and renderers
    TableView <RMNumberFormat> nfTable = getView("NumberFormatTable", TableView.class);
    nfTable.setCellConfigure(this :: configNumberFormatTable);
    nfTable.setItems(_numberFormats);
    TableView <RMDateFormat> dfTable = getView("DateFormatTable", TableView.class);
    dfTable.setCellConfigure(this :: configDateFormatTable);
    dfTable.setItems(_dateFormats);
}

/**
 * Reset UI panel.
 */
public void resetUI()
{
    // Get main editor and currently selected format (just return if null)
    RMEditor editor = getEditor(); if(editor==null) return;
    RMFormat format = RMEditorUtils.getFormat(editor);
            
    // Handle NumberFormat: Update NumberPanel
    if(format instanceof RMNumberFormat) { RMNumberFormat nformat = (RMNumberFormat)format;
        
        // Install number panel if absent
        setViewSelIndex("PickerPanel", 1);
        setViewValue("NumberFormatButton", true);
        
        // Update MoneyButton, PercentButton, CommaButton
        setViewValue("MoneyButton", nformat.isLocalCurrencySymbolUsed());
        setViewValue("PercentButton", nformat.isPercentSymbolUsed());
        setViewValue("CommaButton", nformat.isGroupingUsed());

        // Have the table select current format if present
        setViewSelIndex("NumberFormatTable", _numberFormats.indexOf(format));
        
        // Update NumberFormatText, NegativeInRedCheckBox, NumberNullStringText
        setViewValue("NumberFormatText", nformat.getPattern());
        setViewValue("NegativeInRedCheckBox", nformat.isNegativeInRed());
        setViewValue("NumberNullStringText", nformat.getNullString());
    }
    
    // Handle DateFormat: Update _datePanel text fields
    else if(format instanceof RMDateFormat) { RMDateFormat dformat = (RMDateFormat)format;
        
        // Install date panel if absent
        setViewSelIndex("PickerPanel", 2);
        setViewValue("DateFormatButton", true);

        // Have the table select current format if present
        setViewSelIndex("DateFormatTable", getDateFormatIndex(dformat.getPattern()));
        
        // Update DateFormatText, DateNullStringText
        setViewValue("DateFormatText", dformat.getPattern());
        setViewValue("DateNullStringText", dformat.getNullString());
    }
    
    // Handle NoFormatPanel
    else {
        setViewSelIndex("PickerPanel", 0);
        setViewValue("NoFormatButton", true);
    }
}

/**
 * Responds to changes from format panel UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get main editor and currently selected format, number format and date format (one or the other will be null)
    RMEditor editor = getEditor(); if(editor==null) return;
    RMFormat format = RMEditorUtils.getFormat(editor);
    RMNumberFormat numFormat = format instanceof RMNumberFormat? (RMNumberFormat)format : null;
    RMDateFormat dateFormat = format instanceof RMDateFormat? (RMDateFormat)format : null;
    
    // Handle NoFormatButton
    if(anEvent.equals("NoFormatButton"))  RMEditorUtils.setFormat(editor, null);
    
    // Handle NumberFormatButton
    if(anEvent.equals("NumberFormatButton") && !(format instanceof RMNumberFormat))
        RMEditorUtils.setFormat(editor, getNumberFormat(0).clone());
    
    // Handle DateFormatButton
    if(anEvent.equals("DateFormatButton") && !(format instanceof RMDateFormat))
        RMEditorUtils.setFormat(editor, new RMDateFormat(getDateFormat(0).getPattern()));
        
    // Handle NumberFormatTable
    if(anEvent.equals("NumberFormatTable")) {
        int row = anEvent.getSelIndex();
        if(row > -1) 
            RMEditorUtils.setFormat(editor, getNumberFormat(row).clone());
    }
    
    // Handle NumberFormatText
    if(anEvent.equals("NumberFormatText") && anEvent.getStringValue().length()>0) {
        try { format.setPattern(anEvent.getStringValue()); }
        catch(Exception e) {
            String msg = "Invalid number format (see DecimalFormat javadoc for info).";
            DialogBox dbox = new DialogBox("Invalid Number Format String"); dbox.setErrorMessage(msg);
            dbox.showMessageDialog(getUI());
        }
    }

    // Handle MoneyButton: If currently selected format is number format, add or remove dollars
    if(anEvent.equals("MoneyButton")) {
        if(numFormat==null) RMEditorUtils.setFormat(editor, RMNumberFormat.CURRENCY);
        else {
            numFormat = numFormat.clone(); // Clone it
            numFormat.setLocalCurrencySymbolUsed(!numFormat.isLocalCurrencySymbolUsed()); // Toggle whether $ is used
            RMEditorUtils.setFormat(editor, numFormat);
        }
    }
    
    // Handle PercentButton: If currently selected format is number format, add or remove percent symbol
    if(anEvent.equals("PercentButton")) {
        if(numFormat==null) RMEditorUtils.setFormat(editor, new RMNumberFormat("#,##0.00 %"));
        else {
            numFormat = numFormat.clone(); // Clone it
            numFormat.setPercentSymbolUsed(!numFormat.isPercentSymbolUsed()); // Toggle whether percent symbol is used
            RMEditorUtils.setFormat(editor, numFormat); // Set new format
        }
    }
    
    // Handle CommaButton: If currently selected format is number format, add or remove grouping
    if(anEvent.equals("CommaButton")) {
        if(numFormat==null) RMEditorUtils.setFormat(editor, new RMNumberFormat("#,##0.00"));
        else {
            numFormat = numFormat.clone(); // Clone it
            numFormat.setGroupingUsed(!numFormat.isGroupingUsed()); // Toggle whether grouping is used
            RMEditorUtils.setFormat(editor, numFormat); // Set new format
        }
    }
    
    // Handle DecimalAddButton: If currently selected format is number format, add decimal
    if(anEvent.equals("DecimalAddButton")) {
        if(numFormat!=null) {
            numFormat = numFormat.clone(); // Clone it
            numFormat.setMinimumFractionDigits(numFormat.getMinimumFractionDigits()+1); // Add decimal digits
            numFormat.setMaximumFractionDigits(numFormat.getMinimumFractionDigits());
            RMEditorUtils.setFormat(editor, numFormat); // Set new format
        }
    }
    
    // Handle DecimalRemoveButton: If currently selected format is number format, remove decimal digits
    if(anEvent.equals("DecimalRemoveButton")) {
        if(numFormat!=null) {
            numFormat = numFormat.clone(); // Clone it
            numFormat.setMinimumFractionDigits(numFormat.getMinimumFractionDigits()-1); // Remove decimal digits
            numFormat.setMaximumFractionDigits(numFormat.getMinimumFractionDigits());
            RMEditorUtils.setFormat(editor, numFormat); // Set new format
        }
    }
        // Handle NegativeInRedCheckBox
    if(anEvent.equals("NegativeInRedCheckBox"))
        numFormat.setNegativeInRed(anEvent.getBoolValue());
    
    // Handle NumberNullStringText
    if(anEvent.equals("NumberNullStringText"))
        numFormat.setNullString(anEvent.getStringValue());

    // Handle DateFormatTable
    if(anEvent.equals("DateFormatTable") && anEvent.getSelIndex()>=0) {
        int row = anEvent.getSelIndex();
        RMDateFormat df = getDateFormat(row).clone();
        RMEditorUtils.setFormat(editor, df);
    }
    
    // Handle DateFormatText
    if(anEvent.equals("DateFormatText") && anEvent.getStringValue().length()>0) {
        try { format.setPattern(anEvent.getStringValue()); }
        catch(Exception e) {
            String msg = "Invalid date format (see SimpleDateFormat javadoc for info).";
            DialogBox dbox = new DialogBox("Invalid Date Format String"); dbox.setErrorMessage(msg);
            dbox.showMessageDialog(getUI());
        }
    }
    
    // Handle DateNullStringText
    if(anEvent.equals("DateNullStringText"))
        dateFormat.setNullString(anEvent.getStringValue());
}

/**
 * Configure NumberFormatTable.
 */
private void configNumberFormatTable(ListCell <RMNumberFormat> aCell)
{
    RMNumberFormat fmt = aCell.getItem(); if(fmt==null) return;
    int col = aCell.getCol();
    String str = col==0? fmt.format(_sampleNumberPos) : col==1? fmt.format(_sampleNumberNeg) : fmt.getPattern();
    aCell.setText(str);
}

/**
 * Configure DateFormatTable.
 */
private void configDateFormatTable(ListCell <RMDateFormat> aCell)
{
    RMDateFormat fmt = aCell.getItem(); if(fmt==null) return;
    String str = aCell.getCol()==0? fmt.format(_sampleDate) : fmt.getPattern();
    aCell.setText(str);
}

/**
 * Returns the number of preset number formats available to the format panel.
 */
public int getNumberFormatCount()  { return _numberFormats.size(); }

/**
 * Returns the preset number format at the given index.
 */
public RMNumberFormat getNumberFormat(int anIndex)  { return _numberFormats.get(anIndex); }

/**
 * Returns the Format panel's current number format strings as a single newline separated string.
 */
public String getNumberFormatsString()
{
    StringBuffer sb = new StringBuffer();
    for(int i=0, iMax=getNumberFormatCount(); i<iMax; i++)
        sb.append(getNumberFormat(i).getPattern()).append('\n');
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
}

/**
 * Sets the FormatPanel's current number formats from a single newline separated string.
 */
public void setNumberFormatsString(String aString)
{
    _numberFormats.clear();
    List fstrings = StringUtils.separate(aString, "\n");
    for(int i=0, iMax=fstrings.size(); i<iMax; i++)
        _numberFormats.add(new RMNumberFormat(fstrings.get(i).toString()));
}

/**
 * Returns the number of preset date formats available to the format panel.
 */
public int getDateFormatCount()  { return _dateFormats.size(); }

/**
 * Returns the preset date format at the given index.
 */
public RMDateFormat getDateFormat(int anIndex)  { return _dateFormats.get(anIndex); }

/**
 * Returns the index of the preset date format for a given date format pattern.
 */
public int getDateFormatIndex(String aPattern)
{
    // Iterate over preset date formats to see if any matches the given pattern
    for(int i=0, iMax=getDateFormatCount(); i<iMax; i++)
        if(getDateFormat(i).getPattern().equals(aPattern))
            return i;
    return -1;  // Return -1 since pattern not found
}

/**
 * Returns the Format panel's current date format strings as a single newline separated string.
 */
public String getDateFormatsString()
{
    // Iterate over formats and add format pattern + newline to string buffer
    StringBuffer sb = new StringBuffer();
    for(int i=0, iMax=getDateFormatCount(); i<iMax; i++)
        sb.append(getDateFormat(i).getPattern()).append('\n');
    
    // Delete last newline and return string
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
}

/**
 * Sets the FormatPanel's current date formats from a single newline separated string.
 */
public void setDateFormatsString(String aString)
{
    _dateFormats.clear();
    List fstrings = StringUtils.separate(aString, "\n");
    for(int i=0, iMax=fstrings.size(); i<iMax; i++)
        _dateFormats.add(new RMDateFormat(fstrings.get(i).toString()));
}

/**
 * Returns ReportMill's default number format strings as a single newline delimited string.
 */
public String getDefaultNumberFormatsString()
{
    return
        "$ #,##0.00\n" +
        "$ #,##0\n" +
        "0.00\n" +
        "0\n" +
        "#,##0\n" +
        "000000\n" +
        "0%\n" +
        "0.00%";
}

/**
 * Returns ReportMill's default date format strings as a single newline delimited String.
 */
public String getDefaultDateFormatsString()
{
    return
        "EEEE, MMMM d, yyyy\n" +
        "MMMM d, yyyy\n" +
        "d MMMM yyyy\n" +
        "MM/dd/yy\n" +
        "MM/dd/yyyy\n" +
        "MMM dd, yyyy\n" +
        "dd MMM yyyy\n" +
        "dd-MMM-yyyy\n" +
        "HH:mm:ss a zzzz\n" +
        "hh:mm a";
}

/**
 * Returns the name for the attributes panel window.
 */
public String getWindowTitle()  { return "Format Panel"; }

/**
 * Returns the shared format panel.
 */
public static FormatPanel get()  { return _shared; }

}