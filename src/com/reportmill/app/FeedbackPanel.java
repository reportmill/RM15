/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import java.util.*;
import snap.util.*;
import snap.view.*;

/**
 * This class provides a UI panel to send feedback back to ReportMill.
 */
public class FeedbackPanel extends ViewOwner {

    // The cgimail template URL
    public static String _url = "http://www.reportmill.com/cgi-bin/cgiemail/email/rm-feedback.txt";

/**
 * Show panel.
 */
public void showPanel(View aView)
{
    // Show panel (just return if cancelled)
    DialogBox dbox = new DialogBox("ReportMill Feedback");
    dbox.setContent(getUI()); dbox.setOptions("Submit", "Cancel");
    if(!dbox.showConfirmDialog(null)) return;
    
    // Update preferences and send feedback
    PrefsUtils.prefsPut("ExceptionUserName", getViewStringValue("UserText"));
    PrefsUtils.prefsPut("ExceptionEmail", getViewStringValue("EmailText"));
    sendFeedback();
}

/**
 * Initialize UI.
 */
public void initUI()
{
    setViewValue("UserText", PrefsUtils.prefs().get("ExceptionUserName", ""));
    setViewValue("EmailText", PrefsUtils.prefs().get("ExceptionEmail", ""));
}

/**
 * Send feedback via cgiemail at reportmill.com.
 */
public void sendFeedback()
{        
    // Configure environment string
    StringBuffer environment = new StringBuffer();
    String license = ReportMill.getLicense();
    if(license==null) license = "Unlicensed Copy";
    environment.append("License: " + license + "\n");
    environment.append("Build Date: " + SnapUtils.getBuildInfo() + "\n");
    environment.append("Java VM: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n");
    environment.append("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
    
    // Configure keys
    final Map map = new HashMap();
    map.put("user-name", getViewStringValue("UserText"));
    map.put("user-email", getViewStringValue("EmailText"));
    map.put("environment", environment.toString());
    map.put("type", getViewStringValue("TypeComboBox"));
    map.put("severity", getViewStringValue("SeverityComboBox"));
    map.put("module", getViewStringValue("ModuleComboBox"));
    map.put("title", getViewStringValue("TitleText"));
    map.put("description", getViewStringValue("DescriptionText"));
    
    // Send email in background thread
    new Thread() { public void run() {
        Exception e = URLUtils.sendCGIEmail(_url, map);
        if(e!=null) e.printStackTrace();
    }}.start();
}

}