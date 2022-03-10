package rmtests;
import com.reportmill.base.RMExtras;
import com.reportmill.base.RMXMLReader;
import com.reportmill.shape.RMDocument;
import com.reportmill.shape.RMPage;
import com.reportmill.shape.RMShape;
import snap.geom.Point;
import java.util.List;

/**
 * This test class generates a report with page children scaled down.
 */
public class ScaleDocContent {

    /**
     * Loads a template with page children scaled down
     */
    public static RMDocument getDocWithContentScaled(Object aSource, double aScale)
    {
        // Do normal getDoc
        RMDocument doc = RMDocument.getDoc(aSource);

        // Iterate over doc pages
        List<RMPage> pages = doc.getPages();
        for (RMPage page : pages) {

            // Get page children and scale page
            page.setScaleXY(aScale, aScale);

            // Iterate over page children and scale while preserving child global XY
            for (RMShape child : page.getChildren()) {
                Point childXY = child.localToParent(0, 0, null);
                child.setScaleXY(aScale, aScale);
                Point childXYScaled = child.localToParent(0, 0);
                child.offsetXY(childXY.x - childXYScaled.x, childXY.y - childXYScaled.y);
            }

            // Restore page scale
            page.setScaleXY(1, 1);
        }

        // Return doc
        return doc;
    }

    /**
     *
     */
    public static void generateMoviesReport()
    {
        Object templateSource = RMExtras.getMoviesURL();
        RMDocument template = getDocWithContentScaled(templateSource, .5);

        Object dataSetSource = RMExtras.getHollywoodURL();
        Object dataSet = new RMXMLReader().readObject(dataSetSource);
        RMDocument report = template.generateReport(dataSet);
        report.writePDF("/tmp/Movies.pdf");
    }

    public static void main(String[] args)
    {
        generateMoviesReport();
    }
}

