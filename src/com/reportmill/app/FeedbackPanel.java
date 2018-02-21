/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebURL;

/**
 * This class provides a UI panel to send feedback back to ReportMill.
 */
public class FeedbackPanel extends ViewOwner {

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
    Prefs.get().set("ExceptionUserName", getViewStringValue("UserText"));
    Prefs.get().set("ExceptionEmail", getViewStringValue("EmailText"));
    sendFeedback();
}

/**
 * Initialize UI.
 */
public void initUI()
{
    setViewValue("UserText", Prefs.get().get("ExceptionUserName", ""));
    setViewValue("EmailText", Prefs.get().get("ExceptionEmail", ""));
    
    // Configure TypeComboBox
    String types[] = { "Bug Report", "Enhancement Request", "General Comment" };
    setViewItems("TypeComboBox", types);
    
    // Configure SeverityComboBox
    String severities[] = { "Low", "Medium", "High" };
    setViewItems("SeverityComboBox", severities);
    
    // Configure ModuleComboBox
    String mods[] = { "General", "Text", "Images", "Drawing", "Tables", "Graphs", "Labels", "API", "Other" };
    setViewItems("ModuleComboBox", mods);
}

/**
 * Send feedback via SendMail.py at reportmill.com.
 */
public void sendFeedback()
{        
    // Configure environment string
    StringBuffer env = new StringBuffer();
    String lic = ReportMill.getLicense(); if(lic==null) lic = "Unlicensed Copy";
    env.append("License: " + lic + "\n");
    env.append("Build Date: " + ReportMill.getBuildInfo() + "\n");
    env.append("Java VM: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n");
    env.append("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
    
    // Get to address
    String toAddr = "support@reportmill.com";
    
    // Get from address
    String name = getViewStringValue("UserText"); int nlen = name!=null? name.length() : 0;
    String email = getViewStringValue("EmailText"); int elen = email!=null? email.length() : 0;
    if(nlen>0 && elen>0) email = name + " <" + email + '>';
    else if(nlen>0) email = name; else if(elen==0) email = "Anonymous";
    String fromAddr = email;
    
    // Get subject
    String subject = "ReportMill Feedback";
    
    // Get body
    StringBuffer sb = new StringBuffer();
    sb.append(subject).append('\n').append('\n');
    sb.append("From: ").append(fromAddr).append('\n');
    sb.append("Type: ").append(getViewStringValue("TypeComboBox")).append('\n');
    sb.append("Severity: ").append(getViewStringValue("SeverityComboBox")).append('\n');
    sb.append("Module: ").append(getViewStringValue("ModuleComboBox")).append('\n').append('\n');
    sb.append("Title: ").append(getViewStringValue("TitleText")).append('\n').append('\n');
    sb.append(getViewStringValue("DescriptionText")).append('\n').append('\n').append(env);
    String body = sb.toString();
    
    // Get URL
    String url = "http://www.reportmill.com/cgi-bin/SendMail.py";

    // Send email in background thread
    new Thread() { public void run() {
        String str = sendMail(toAddr, fromAddr, subject, body, url);
        if(str!=null) System.out.println("ExceptionReporter Response: " + str);
    }}.start();
}

/**
 * Sends an email with given from, to, subject, body and SendMail url.
 */
public static String sendMail(String toAddr, String fromAddr, String aSubj, String aBody, String aURL)
{
    // Create full message text, create URL, post text bytes and return response string
    String text = String.format("To=%s\nFrom=%s\nSubject=%s\n%s", toAddr, fromAddr, aSubj, aBody);
    WebURL url = WebURL.getURL(aURL);
    byte bytes[] = url.postBytes(text.getBytes());
    return bytes!=null? new String(bytes) : null;
}

}