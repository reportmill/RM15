/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * This class provides a Preferences panel UI window.
 */
public class PreferencesPanel extends ViewOwner {
    
    // The FormatPanel
    FormatPanel        _formatPanel = FormatPanel.get();

/**
 * Runs the panel.
 */
public void showPanel(View aView)
{
    DialogBox dbox = new DialogBox("Preferences Panel"); dbox.setContent(getUI());
    while(dbox.showConfirmDialog(aView) && !apply());
}

/**
 * Initialize UI panel.
 */
public void initUI()
{
    // Set LicenseText & EnableExceptionsCheckBox
    setViewValue("LicenseText", Prefs.get().get("HostProperties1", null));
    setViewValue("EnableExceptionsCheckBox", Prefs.get().getBoolean("ExceptionReportingEnabled", true));
    
    // Set NumberFormatsText & DateFormatsText
    setViewValue("NumberFormatsText", _formatPanel.getNumberFormatsString());
    setViewValue("DateFormatsText", _formatPanel.getDateFormatsString());
}

/**
 * Updates user preferences settings from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle ResetNumbersButton
    if(anEvent.equals("ResetNumbersButton"))
        setViewValue("NumberFormatsText", _formatPanel.getDefaultNumberFormatsString());
    
    // Handle ResetDatesButton
    if(anEvent.equals("ResetDatesButton"))
        setViewValue("DateFormatsText", _formatPanel.getDefaultDateFormatsString());
}

/**
 * Handles the preferences panel apply button.
 */
public boolean apply()
{
    // Get License key
    String licenseKey = StringUtils.min(getViewStringValue("LicenseText"));
    
    // If license is provided but invalid, complain and return
    if(licenseKey!=null && !ReportMill.checkString(licenseKey, true)) {
        String msg = "The license key entered is invalid - please recheck and try again.";
        DialogBox dbox = new DialogBox("Invalid License"); dbox.setErrorMessage(msg);
        dbox.showMessageDialog(getUI());
        return false;
    }
    
    // Set license
    ReportMill.setLicense(licenseKey, true, true);

    // Save the exception reporting pref
    Prefs.get().set("ExceptionReportingEnabled", getViewBoolValue("EnableExceptionsCheckBox"));
    
    // Get pref panel number formats and the original number formats
    String nums = getViewStringValue("NumberFormatsText");
    String oldNums = _formatPanel.getNumberFormatsString();
    
    // If number formats have changed, try to commit them
    if(!nums.equals(oldNums)) {
    
        // If setting the format throws exception, reset old format string, show error dialog and return false
        try { _formatPanel.setNumberFormatsString(nums); }
        catch(Exception e) {
            _formatPanel.setNumberFormatsString(oldNums);
            String msg = "Invalid number format (see format panel for examples).";
            DialogBox dbox = new DialogBox("Invalid Number Format String"); dbox.setErrorMessage(msg);
            dbox.showMessageDialog(getUI());
            return false;
        }
        
        // Add new format string to default (clear it if it's the default)
        if(nums.equals(_formatPanel.getDefaultNumberFormatsString()))
            nums = null;
        Prefs.get().set("NumberFormats", nums);
    }
    
    // Get pref panel date formats and original date formats
    String dates = getViewStringValue("DateFormatsText");
    String oldDates = _formatPanel.getDateFormatsString();
    
    // If date formats have changed, commit them
    if(!dates.equals(oldDates)) {

        // If setting the format throws exception, reset old format string, show error dialog and return false
        try { _formatPanel.setDateFormatsString(dates); }
        catch(Exception e) {
            _formatPanel.setDateFormatsString(oldDates);
            String msg = "Invalid date format (see format panel for examples).";
            DialogBox dbox = new DialogBox("Invalid Date Format String"); dbox.setErrorMessage(msg);
            dbox.showMessageDialog(getUI());
            return false;
        }

        // Add new format string to default (clear it if it's the default)
        if(dates.equals(_formatPanel.getDefaultDateFormatsString()))
            dates = null;
        Prefs.get().set("DateFormats2", dates);
    }
    
    // Flush properties to registry and return true
    Prefs.get().flush();
    return true;
}

}