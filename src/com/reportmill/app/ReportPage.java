/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.view.View;
import snap.viewx.WebPage;

/**
 * A WebPage subclass for viewing a ReportFile.
 */
public class ReportPage extends WebPage {

/**
 * Creates a file pane for the given file in the requested mode.
 */
protected View createUI()
{
    RMViewerPane vpane = new RMViewerPane();
    vpane.getViewer().setDoc(getFile());
    return vpane.getUI();
}

/**
 * Override to return UI file.
 */
public Object getUISource()  { return getFile(); }

/**
 * Creates a file pane for the given file in the requested mode.
 */
/*protected JComponent createUI(RMParentShape aDoc, WebFile aFile)
{
    // Get document 
    ReportData rdata = (ReportData)aFile.getData();
    RMParentShape doc = aDoc!=null? aDoc : rdata.getRootShape();
    if(doc instanceof RMDocument && !rdata.isGenerated())
        doc = ((RMDocument)doc).generateReport();
    
    // Get viewer pane
    getViewerPane().getViewer().setContent(doc);
    return getViewerPane().getUI();
}*/

/**
 * Override to pass focus to viewer.
 */
/*public void notifyPageAdded(WebBrowser aBrowser)
{
    // If report hasn't been generated, generate report
    ReportData rdata = (ReportData)getFile().getData();
    if(rdata.getDocument()!=null && !rdata.isGenerated()) {
        RMDocument report = rdata.getDocument().generateReport();
        getViewerPane().getViewer().setContent(report);
    }
    
    // Request focus
    requestFocus(getUI());
}*/

}