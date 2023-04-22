package com.reportmill.base;
import com.reportmill.graphics.*;
import com.reportmill.out.RMExcelWriter;
import com.reportmill.shape.RMDocument;
import snap.util.GetBestMethod;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * An RMEnv subclass that uses JVM desktop features when appropriate.
 */
public class RMEnvSwing extends RMEnv {

    /**
     * Returns a RMXString for the given html string and a default font.
     */
    public RMXString parseHTML(String html, RMFont baseFont, RMParagraph aLineStyle)
    {
        return RMHTMLParser.parse(html, baseFont, aLineStyle);
    }

    /**
     * Returns an RMXString from the given rtf string and default font.
     */
    public RMXString parseRTF(String rtf, RMFont baseFont)
    {
        return RMRTFParser.parse(rtf, baseFont);
    }

    /**
     * Returns the document as byte array of an Excel file.
     */
    public byte[] getBytesExcel(RMDocument aDoc)
    {
        return new RMExcelWriter().getBytes(aDoc);
    }

    /**
     * Returns a list of maps for a given ResultSet.
     */
    public List<Map<String,Object>> getResultSetAsMaps(Object aResultSet, int aLimit)
    {
        ResultSet rs = (ResultSet) aResultSet;
        return RMSQLUtils.getMaps(rs, aLimit);
    }

    /**
     * Returns the method for given class and name that best matches given parameter types.
     */
    public Method getMethodBest(Class aClass, String aName, Class... theClasses)
    {
        return GetBestMethod.getBestMethod(aClass, aName, theClasses);
    }
}
