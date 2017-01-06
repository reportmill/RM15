/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill;
import com.reportmill.base.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.*;

/**
 * This class offers a command line interface to some of ReportMill's functionality. You can invoke it like this:
 * 
 *    prompt> java -cp ReportMill.jar com.reportmill.Shell
 * 
 * This class supports a number of command line args:
 * 
 *    -license <your_license_key>   Installs a specified license for the current user.
 *    -fonts                        Prints all the font names on the system.
 *    -fonts2                       Prints all the font family names on the system.
 * 
 */
public class Shell {

/** This is the static main method called when launching Java with this class. */
public static void main(String args[])
{
    // Turn on headless property and trigger ReportMill init message
    new ReportMill();

    // If no args, print usage
    if(args.length==0 || args[0]=="?") {
        System.err.println("Usage:");
        System.err.println("  Licensing: -license <license_string>");
        System.err.println("  Testing: <template_path> <dataset_path> <output_path>");
    }

    // Declare variable for license, template file, input file and output file, ReportCount, ThreadCount
    String license = null;
    String rptfile = null, infile = null, outfile = null;
    int count = 1, threads = 0;
    
    // Declare variable for whether to paginate, generate table of contents, compress PDF, or hang (for testing)
    Boolean paginate = null;
    boolean toc = false;
    boolean compress = true;
    boolean hang = false;
    
    // Iterate over arguments
    for(int i=0; i<args.length; i++) { String arg = args[i];
        
        // Check for -license: If no license provided, print current license, otherwise get license
        if(arg.equals("-license")) {
            if(i+1==args.length) System.err.println("License: " + ReportMill.getLicense());
            else license = args[++i];
        }
            
        // Check for rptfile (.rpt)
        if(StringUtils.endsWithIC(arg, ".rpt")) rptfile = arg;
            
        // Check for infile (.xml)
        if(StringUtils.endsWithIC(arg, ".xml")) infile = arg;
            
        // Check for outfile (.pdf, .swf, .html, .csv)
        if(StringUtils.endsWithIC(arg, ".pdf") || StringUtils.endsWithIC(arg, ".swf") ||
            StringUtils.endsWithIC(arg, ".html") || StringUtils.endsWithIC(arg, ".csv"))
            outfile = arg;
            
        // Check for paginate
        if(arg.equals("-paginate"))
            try { paginate = new Boolean(!args[++i].equals("false")); }
            catch(Exception e) { paginate = new Boolean(true); }
            
        // Check for test
        if(arg.equals("-test")) {
            count = i+1==args.length? 100 : StringUtils.intValue(args[++i]);
            if(rptfile==null) rptfile = "Jar:/com/reportmill/examples/Movies.rpt";
            if(infile==null) infile = "Jar:/com/reportmill/examples/HollywoodDB.xml";
            if(outfile==null) outfile = "/tmp/Movies.pdf";
        }
        
        // Check for threads
        if(arg.equals("-threads") && i+1<args.length)
            threads = StringUtils.intValue(args[++i]);
            
        // Check for toc (table of contents)
        if(arg.equals("-toc")) toc = true;
            
        // Check for uncompress
        if(arg.equals("-nocompress")) compress = false;
        
        // Check for hang request
        if(arg.equals("-hang")) hang = true;
            
        // Check for fonts: Get list of fonts and print
        if(arg.startsWith("-fonts")) {
            String fonts[] = arg.equals("-fonts")? RMFont.getFontNames() : RMFont.getFamilyNames();
            for(int j=0, jMax=fonts.length; j<jMax; j++) System.err.println(fonts[j]);
        }
        
        // If arg is -resave, just resave existing template (potentially used to update templates)
        if(arg.equals("-resave")) {
            
            // Iterate over remaining args (assuming they are all template paths)
            for(int j=i+1; j<args.length; j++) {
                System.err.println("Resaving " + args[j]);
                RMDocument doc = new RMDocument(args[j]);
                doc.write(args[j]);
            }
            
            // Just exit after resaving
            System.exit(0);
        }
    }
    
    // If license was provided, install it
    if(license!=null) {
        if(ReportMill.checkString(license, false)) {
            ReportMill.setLicense(license, true, false);
            System.err.println("License is valid and has been installed on host " + SnapUtils.getHostname() +
                " for user " + System.getProperty("user.name") + ".");
        }
        else {
            ReportMill.setLicense(null, true, false);
            System.err.println("Invalid license - please check string and re-enter. " + 
                "Call 214.513.1636 or send email to <support@reportmill.com> for support.");
        }
    }
    
    // If paginate wasn't specified, get it from outfile
    if(paginate==null && outfile!=null)
        paginate = new Boolean(StringUtils.endsWithIC(outfile, "pdf"));
    
    // If we have rptfile, infile and outfile, generate report
    if(rptfile!=null && infile!=null && outfile!=null) {
        
        // Load fonts
        System.err.println("Loading Fonts... ");
        long time = System.currentTimeMillis();
        RMFont.getFontNames();
        float seconds = (System.currentTimeMillis() - time)/1000f;
        System.err.println("Font Loading... (" + seconds + " seconds)");
    
        // Load template
        System.err.print("Reading template: " + rptfile); System.err.flush();
        time = System.currentTimeMillis();
        RMDocument template = RMDocument.getDoc(rptfile);
        template.setCompress(compress);
        seconds = (System.currentTimeMillis() - time)/1000f;
        System.err.println(" (" + seconds + " seconds)");
        
        // Load XML data
        System.err.print("Reading infile: " + infile); System.err.flush();
        time = System.currentTimeMillis();
        Map map = new RMXMLReader().readObject(infile, template.getDataSourceSchema());
        seconds = (System.currentTimeMillis() - time)/1000f;
        System.err.println(" (" + seconds + " seconds)");
        
        // Generate reports, multi-threaded (Create report threads, start, then join)
        System.err.println("Generating Reports");
        time = System.currentTimeMillis();
        if(threads>0) {
            Thread threadArray[] = new Thread[4];
            for(int i=0; i<threads; i++) threadArray[i] = new RPGThread(template, map, outfile, paginate, i, count);
            for(int i=0; i<threads; i++) threadArray[i].start();
            for(int i=0; i<threads; i++) try { threadArray[i].join(); } catch(Exception e) { e.printStackTrace(); }
        }
        
        // Generate reports, single threaded
        else for(int i=1; i<=count; i++) {
            RMDocument report = template.generateReport(map, paginate.booleanValue());
            
            // If table of contents is requested, generate toc report and append
            if(toc) {
                RMTableOfContents toco = new RMTableOfContents(report);
                RMDocument toct = getTableOfContentsTemplate();
                RMDocument tocr = toct.generateReport(toco);
                tocr.addPages(report);
                report = tocr;
            }
            
            // Write output
            report.write(outfile);
            if(count>1) {
                if(i==count) System.err.println("" + i + " (Done)");
                else { System.err.print("" + i + " "); System.err.flush(); }
            }
        }
        
        // Null out template and data vars
        template = null; map = null;
        
        // Print done message
        seconds = (System.currentTimeMillis() - time)/1000f;
        System.err.println("Generated Reports (" + seconds + " seconds)");
        
        // Hang if requested (useful for optimizeit)
        if(hang) while(true) Thread.yield();
    }

    // Exit
    System.exit(0);
}

// An inner class to simply generate 'count' reports from a separate thread
static class RPGThread extends Thread {
    RMDocument template;
    Object     data;
    String     outfile;
    Boolean    paginate;
    int        id, count;
    public RPGThread(RMDocument t, Object d, String o, Boolean p, int i, int c) {
        template = t; data = d; outfile = o; paginate = p; id = i; count = c; }
    public void run() {
        for(int i=1; i<=count; i++) {
            RMDocument report = template.generateReport(data, paginate.booleanValue());
            String out = StringUtils.replace(outfile, ".", "_" + id + "_" + (i<10?"0":"") + i + ".");
            report.write(out);
            if(count>1) System.err.println("Thread " + id + ": Generated Report #" + i);
        }
    }
}

static RMDocument getTableOfContentsTemplate()
{
    // Create template
    RMDocument template = new RMDocument(612, 792);
    
    // Create table, size it and add it to first template page
    RMTable table = new RMTable("Objects");
    table.setFrame(36, 36, 540, 680);
    template.getPage(0).addChild(table);
    
    // Create Header row
    table.getGrouping("Objects").setHasHeader(true);
    RMTableRow headerRow = (RMTableRow)table.getChildWithTitle("Objects Header");
    headerRow.setNumberOfColumns(1); headerRow.setHeight(205);
    headerRow.getColumn(0).setText("Hollywood Report\n\nTable of Contents");
    headerRow.getColumn(0).setAlignmentX(RMTypes.AlignX.Center);
    headerRow.getColumn(0).getXString().setAttribute(RMFont.getFont("Times Bold", 72));
    headerRow.getColumn(0).getXString().setAttribute(RMFont.getFont("Times", 18), 16, 35);
    headerRow.setVersion("Reprint");
    headerRow.getColumn(0).setText("Table of Contents (Continued)");
    headerRow.getColumn(0).getXString().setAttribute(RMFont.getFont("Times", 18));
    headerRow.setHeight(30);
    headerRow.layout();
    
    // Create tableRow, turn off structuring and set height to 20
    RMTableRow tableRow = (RMTableRow)table.getChildWithTitle("Objects Details");
    tableRow.getColumn(1).setText("@row@. @getStudio.getName@");
    tableRow.getColumn(2).setText("........................ @page@");
    tableRow.getColumn(2).setAlignmentX(RMTypes.AlignX.Right);
    tableRow.getColumn(0).setWidth(.5f);
    tableRow.getColumn(1).setWidth(2);
    tableRow.getColumn(2).setWidth(2);
    tableRow.getColumn(3).setWidth(.5f);
    tableRow.layout();
    
    // Set URL and dataset key
    tableRow.setURL("Page:@page@");
    table.setDatasetKey("Movies.getStudio");

    // Return template
    return template;
}

}