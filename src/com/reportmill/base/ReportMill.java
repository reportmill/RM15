/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.shape.*;
import com.reportmill.graphics.*;
import java.util.*;
import java.util.prefs.Preferences;
import snap.gfx.GFXEnv;
import snap.util.SnapUtils;

/**
 * This class is used as a helper object by RMDocument to generate a report.
 */
public class ReportMill {
    
    // Provides a hook for converting unusual objects to standard objects (like Map, List, String, Number, etc.) 
    public static AppServer appServer = null;

    // The current license string
    private static String  _license;
    
    // Whether RM found a valid license
    private static boolean _licensed = SnapUtils.checkString(getLicense(), SnapUtils.isApp);
    
/** Static method prints initialization message (when RM invoked by Shell or by other app). */
static
{
    // If not app, print initialization message and turn on headless
    if(!SnapUtils.isApp) {
        
        // Print initialization message
        System.err.println("Initializing ReportMill (Build Date: " + SnapUtils.getBuildInfo() +
            ", Version " + SnapUtils.getVersion() + ", JVM " + System.getProperty("java.version") +
            ", User " + System.getProperty("user.name") + ")");

        // Set headless
        GFXEnv.getEnv().setHeadless();
    }
}

/**
 * An interface for classes than want to know about filled shapes.
 */
public interface Listener {
    public void didFillShape(RMShape aShape, RMShape aCopy);
}

/**
 * An inner class to provide a hook for converting objects (like NSArray to List, etc.).
 */
public interface AppServer {
    public Object convertFromAppServerType(Object anObject);
}

/**
 * Called by various objects to convert objects to generic types.
 */
public static Object convertFromAppServerType(Object anObj)
{
    if(appServer!=null) return appServer.convertFromAppServerType(anObj); // If AppServer, have it do conversion
    if(anObj instanceof Set) return new ArrayList((Set)anObj); // If Set, return List
    else if(anObj instanceof Object[]) return Arrays.asList((Object[])anObj); // If Array, return List
    return anObj; // Return object
}

/**
 * Returns the ReportMill license string for the current user.
 */
public static String getLicense()
{
    // If license hasn't been loaded yet, get it
    if(_license==null) try {
        
        // Get preferences for com.reportmill.Shell and prefs key (HostProperties1 for app, HostProperties2  for engine)
        Preferences prefs = Preferences.userNodeForPackage(com.reportmill.Shell.class);
        String prefsKey = SnapUtils.isApp? "HostProperties1" : "HostProperties2";
        
        // Get license for prefs key
        _license = prefs.get(prefsKey, null);
    }
    
    // Catch exceptions - in case security manager complains
    catch(Throwable t) { System.err.println("ReportMill.getLicense: Can't get license (" + t.getMessage() + ")"); }
    
    // Return license
    return _license;
}

/**
 * Sets the ReportMill license string for the current user.
 */
public static void setLicense(String aLicense)  { setLicense(aLicense, false, SnapUtils.isApp); }

/**
 * Sets the ReportMill license string for the current user (with option to persist).
 */
public static void setLicense(String aLicense, boolean persistent, boolean isApp)
{
    // If persistent, save license to preferences
    if(persistent) try {
        
        // Get preferences for com.reportmill.Shell and prefs key (HostProperties1 for app, HostProperties2  for engine)
        Preferences prefs = Preferences.userNodeForPackage(com.reportmill.Shell.class);
        String prefsKey = SnapUtils.isApp? "HostProperties1" : "HostProperties2";

        // Put license for prefs key (or remove if null) and flush preferences
        if(aLicense!=null) prefs.put(prefsKey, aLicense);
        else prefs.remove(prefsKey);
        prefs.flush();
    }
    
    // Catch exceptions - in case security manager complains
    catch(Throwable t) { System.err.println("ReportMill.setLicense: Can't set license (" + t.getMessage() + ")"); }
    
    // Set new license and determine if license is valid
    _license = aLicense;
    _licensed = SnapUtils.checkString(getLicense(), SnapUtils.isApp);
}

/**
 * Returns whether ReportMill has a valid license for the current user.
 */
public static boolean isLicensed()  { return _licensed; }

/**
 * Simple lc check.
 */
public static void lc(RMDocument aDoc)
{
    // If unlicensed, add watermark and complain
    if(!_licensed) {
        
        // Add watermark/unlicensed message to each report page
        for(int i=0; i<aDoc.getPages().size(); i++) addWatermark(aDoc.getPage(i));

        // If not app, print warning
        if(!SnapUtils.isApp) {
            System.err.println("Warning: Unlicensed copy of ReportMill for host " + SnapUtils.getHostname() +
                " and user " + System.getProperty("user.name") + " - call 214.513.1636 for license.");
            System.err.println("    Enter license with: \"java -cp /YourInstallDir/ReportMill.jar " +
                "com.reportmill.Shell -license <license-string>\"");
            System.err.println("    Or call \"ReportMill.setLicense(\"<license_string>\") in app " +
                "prior to report generation.");
            System.err.println("    This is only a warning (generated report will contain a watermark).");
        }
    }
}

/**
 * This method adds a watermark to the given shape.
 */
private static void addWatermark(RMParentShape aShape)
{
    // Get attributed string with REPORTMILL in 72pt grey (with R & M in 100pt)
    RMFont font72 = RMFont.getFont("Arial Bold", 72), font100 = font72.deriveFont(100);
    RMXString xstring = new RMXString("REPORTMILL", font72, new RMColor(.9));
    xstring.setAttribute(font100, 0, 1); xstring.setAttribute(font100, 6, 7); // Set R & M in 100pt

    // Create evalShape watermark across background
    RMTextShape evalShape = new RMTextShape(xstring);
    evalShape.setFrame((aShape.getWidth() - 570)/2, (aShape.getHeight() - 140)/2, 570, 140);
    evalShape.setRoll(45); evalShape.setOpacity(.667f);
    aShape.addChild(evalShape, 0);

    // Get attributed string with bottom eval message in 12pt
    String msg = "ReportMill Evaluation - request a free license at reportmill.com/free.";
    xstring = new RMXString(msg, RMFont.Helvetica12);

    // Create evalShape license string in lower left corner
    evalShape = new RMTextShape(xstring);
    evalShape.setFrame(5, aShape.getHeight() - 20, 500, 18);
    evalShape.setURL("http://www.reportmill.com/free");
    aShape.addChild(evalShape);
}

}