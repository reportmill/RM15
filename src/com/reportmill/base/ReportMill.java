/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.GFXUtils;
import snap.util.*;

/**
 * This class is used as a helper object by RMDocument to generate a report.
 */
public class ReportMill {

    // Whether ReportMill is running as desktop app
    public static boolean isApp;

    // Provides a hook for converting unusual objects to standard objects (like Map, List, String, Number, etc.) 
    public static AppServer appServer = null;

    // Whether RM has been initialized
    private static boolean _initalized;

    // The build info string from "BuildInfo.txt" (eg, "Aug-31-04")
    private static String _buildInfo;

    // Maximum number of pages allowed in a report
    private static int  _maxPageCount = 500;

    /**
     * An interface for classes than want to know about filled shapes.
     */
    public interface Listener {
        void didFillShape(RMShape aShape, RMShape aCopy);
    }

    /**
     * An inner class to provide a hook for converting objects (like NSArray to List, etc.).
     */
    public interface AppServer {
        Object convertFromAppServerType(Object anObject);
    }

    /**
     * Called by various objects to convert objects to generic types.
     */
    public static Object convertFromAppServerType(Object anObj)
    {
        // If AppServer, have it do conversion
        if (appServer != null)
            return appServer.convertFromAppServerType(anObj);

        // If Set, return List
        if (anObj instanceof Set)
            return new ArrayList<>((Set<?>) anObj);

        // If Array, return List
        else if (anObj instanceof Object[])
            return Arrays.asList((Object[]) anObj);

        // Return object
        return anObj;
    }

    /**
     * Returns the maximum number of pages allowed in a report.
     */
    public static int getMaxPageCount()  { return _maxPageCount; }

    /**
     * Sets the maximum number of pages allowed in a report.
     */
    public static void setMaxPageCount(int aCount)  { _maxPageCount = aCount; }

    /**
     * Sets the ReportMill license string for the current user.
     */
    public static boolean isLicensed()  { return Voucher.isLicensed(); }

    /**
     * Returns the ReportMill license string for the current user.
     */
    public static String getLicense()  { return Voucher.getLicense(); }

    /**
     * Sets the ReportMill license string for the current user.
     */
    public static void setLicense(String aLicense)
    {
        Voucher.setLicense(aLicense, false, isApp);
    }

    /**
     * Returns the ReportMill version.
     */
    public static float getVersion()
    {
        return 15;
    }

    /**
     * Returns a build date string (eg, "Jan-26-03") as generated into BuildInfo.txt at build time.
     */
    public static String getBuildInfo()
    {
        // If already set, just return
        if (_buildInfo != null) return _buildInfo;

        // If build info file hasn't been loaded, load it
        try {
            _buildInfo = SnapUtils.getText(SnapUtils.class, "/com/reportmill/BuildInfo.txt").trim();
        } catch (Exception e) {
            System.err.println("ReportMill.getBuildInfo: " + e);
            _buildInfo = "BuildInfo not found";
        }
        return _buildInfo;
    }

    /**
     * Prints initialization message (when RM invoked by Shell or by other app).
     */
    public static void init()
    {
        // If already initalized, just return
        if (_initalized) return;
        _initalized = true;

        // If app, just return
        if (ReportMill.isApp) return;

        // Print initialization message
        String buildInfo = getBuildInfo();
        double rmVers = getVersion();
        String javaVers = System.getProperty("java.version");
        String user = System.getProperty("user.name");
        String info = String.format("Build Date: %s, Version %.1f, JVM %s, User %s", buildInfo, rmVers, javaVers, user);
        System.err.println("Initializing ReportMill (" + info + ")");

        // Set headless
        GFXUtils.setHeadless();
    }
}