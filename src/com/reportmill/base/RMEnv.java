package com.reportmill.base;
import com.reportmill.graphics.RMFont;
import com.reportmill.graphics.RMParagraph;
import com.reportmill.graphics.RMXString;
import com.reportmill.shape.RMDocument;
import snap.util.SnapUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * A class to allow certain functionality to be pluggable depending on platform (desktop/web).
 */
public class RMEnv {

    // The shared instance
    private static RMEnv _shared;

    /**
     * Returns a RichText for the given html string and a default font.
     */
    public RMXString parseHTML(String html, RMFont baseFont, RMParagraph aLineStyle)
    {
        System.err.println("RMEnv.parseHTML: Not implemented");
        return null;
    }

    /**
     * Returns an xstring from the given rtf string and default font.
     */
    public RMXString parseRTF(String rtf, RMFont baseFont)
    {
        System.err.println("RMEnv.parseRTF: Not implemented");
        return null;
    }

    /**
     * Returns the document as byte array of an Excel file.
     */
    public byte[] getBytesExcel(RMDocument aDoc)
    {
        System.err.println("RMEnv.getBytesExcel: Not implemented");
        return null;
    }

    /**
     * Returns a list of maps for a given ResultSet.
     */
    public List<Map<String,Object>> getResultSetAsMaps(Object aResultSet, int aLimit)
    {
        System.err.println("RMEnv.getResultSetAsMaps: Not implemented");
        return null;
    }

    /**
     * Returns the method for given class and name that best matches given parameter types.
     */
    public Method getMethodBest(Class aClass, String aName, Class... theClasses)
    {
        System.err.println("RMEnv.getMethodBest: Not implemented");
        return null;
    }

    /**
     * Returns the shared instance.
     */
    public static RMEnv getEnv()
    {
        if (_shared != null) return _shared;

        // Use generic for TeaVM, otherwise Swing version
        String className = SnapUtils.isTeaVM ? "com.reportmill.base.RMEnv" : "com.reportmill.base.RMEnvSwing";

        // Try to get/set class name instance
        try { return _shared = (RMEnv) Class.forName(className).newInstance(); }
        catch (Exception e) {
            System.err.println("RMEnv.getEnv: Can't set env: " + className + ", " + e);
            return _shared = new RMEnv();
        }
    }
}
