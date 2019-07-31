/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.base.ReportMill;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.text.SimpleDateFormat;
import java.util.*;
import snappdf.*;
import snappdf.write.*;

/**
 * PDF Writer.
 */
public class RMPDFWriter extends PDFWriter {

    // Map of unique PDF datas
    List <RMPDFData>            _pdfDatas = new ArrayList();
    
/**
 * Returns a PDF byte array for a given RMDocument.
 */
public byte[] getBytes(RMDocument aDoc)
{
    // Validate and resolve doc page references
    aDoc.layoutDeep();
    aDoc.resolvePageReferences();
    
    // Add watermark
    ReportMill.lc(aDoc);

    // Create PDFFile
    _pfile = new PDFFile();
    _xtable = _pfile._xtable = new PDFXTable(null);
    
    // Init and add info dict to xref
    _pfile.addInfoDictValue("CreationDate", "D:" + new SimpleDateFormat("yyyMMddHHmmss").format(new Date()));
    if(aDoc.getMetadata().size()>0)
        for(Map.Entry<String,String> entry : aDoc.getMetadata().entrySet())
            _pfile.addInfoDictValue(entry.getKey(), entry.getValue());
    _xtable.addObject(_pfile._infoDict);

    // Init and add catalog to xref
    _pfile._catalogDict.put("Type", "/Catalog");
    _pfile._catalogDict.put("PageMode", "/UseNone");
    _xtable.addObject(_pfile._catalogDict);
    
    // Init and add to xref and catalog
    _pfile._pageTree = new PDFPageTree(_pfile);
    _pfile._catalogDict.put("Pages", _xtable.addObject(_pfile._pageTree));

    // Add fonts and images to xref
    _xtable.addObject(getFonts());
    _xtable.addObject(getImageRefs());
    
    // Tell acrobat reader not to scale when printing by default (only works in PDF 1.6, but is harmless in < 1.6)
    _pfile._catalogDict.put("ViewerPreferences", PDFWriter.getViewerPreferencesDefault());
    
    // Get doc pdf attributes
    _compress = aDoc.getCompress();
    
    // Set PDF file author
    if(_pfile.getAuthor()==null) _pfile.setAuthor("ReportMill User");
    
    // Set PDF file creator
    String version = "ReportMill " + ReportMill.getVersion();
    String build = ", Build: " + ReportMill.getBuildInfo();
    String jvm = ", JVM: " + System.getProperty("java.version");
    _pfile.setCreator(version + build + jvm);
    
    // Iterate over doc pages
    for(int i=0, iMax=aDoc.getPageCount(); i<iMax; i++) { RMShape page = aDoc.getPage(i);
        
        // Get pdf page, set media box and add to pages tree and xref
        _pageWriter = new PDFPageWriter(_pfile, this);
        _pageWriter.setMediaBox(page.getBoundsInside());
        
        // Have page pdfr write pdf
        RMShapePdfr.getPdfr(page).writePDF(page, this);
    }
      
    // run a pass though all the _pages to resolve any forward references
    _pfile._pageTree.resolvePageReferences();
    
    // Write out header but save away version in case it gets updated
    String versionStr = _pfile.getVersionString();
    appendln("%" + versionStr);
    
    // Write 4 binary bytes in comment to indicate we may use 8 bit binary
    append(new byte[] { (byte)'%', (byte)'\242', (byte)'\243', (byte)'\245', (byte)'\250' });
    appendln();
    
    // The _objects & the xref table 
    int off = writeXRefTable();
    
    // The trailer
    appendln("trailer"); appendln("<<");
    append("/Size ").append(_xtable.getEntryCount() + 1).appendln();
    append("/Root ").appendln(_xtable.getRefString(_pfile._catalogDict));
    append("/Info ").appendln(_xtable.getRefString(_pfile._infoDict));
    
    // Add a uniqueID to the trailer
    String idString = _pfile.getFileIDString();
    append("/ID [").append(idString).append(idString).append(']').appendln();

    // Write cross reference table and end of file marker
    appendln(">>");
    appendln("startxref"); append(off).appendln();
    appendln("%%EOF");
    
    // Now get actual pdf bytes
    byte pdfBytes[] = toByteArray();
    
    // If version string was bumped during generation, go back and update header
    if(!_pfile.getVersionString().equals(versionStr)) { String newStr = _pfile.getVersionString();
        
        // PDF files are extremely sensitive to position, so make sure headers are same size
        int newLen = newStr.length(), oldLen = versionStr.length();
        if(newLen > oldLen) 
            throw new RuntimeException("error trying to update pdf version number to " + newStr);
        
        // Copy new version in (pad with spaces if new version is smaller)
        for(int i=0; i<oldLen; i++)
            pdfBytes[i+1] = (byte)(i<newLen? newStr.charAt(i) : ' ');
    }
    
    // Return pdf bytes
    return pdfBytes;
}

/**
 * Adds an image data (uniqued) to file reference table, if not already present. 
 */
public void addPDFData(RMPDFData aPD, String aName)
{
    // If not present, unique, add to xref table and add to image refs
    Map <String,String> imageRefs = getImageRefs();
    if(!imageRefs.containsKey(aName))
        imageRefs.put(aName, _xtable.addObject(getUniquePDFData(aPD)));
}

/**
 * Returns a unique image data for given image data.
 */
public RMPDFData getUniquePDFData(RMPDFData aPD)
{
    int index = _pdfDatas.indexOf(aPD);
    if(index<0) _pdfDatas.add(index = _pdfDatas.size(), aPD);
    return _pdfDatas.get(index);
}

/**
 * Writes any kind of object to the PDF buffer.
 */
public void writeXRefEntry(Object anObj)
{
    // Handle RMPDFData
    if(anObj instanceof RMPDFData) { RMPDFData pdata = (RMPDFData)anObj;
        PDFWriterPDF.writePDF(this, pdata.getPDFFile(), pdata.getPageIndex()); }
        
    // Otherwise, do normal version
    else super.writeXRefEntry(anObj);
}

}