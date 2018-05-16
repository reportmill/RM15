/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.shape.*;
import com.reportmill.graphics.*;
import java.io.*;
import java.util.*;
import snap.gfx.*;
import snap.util.ASCIICodec;

/**
 * Write RTF for a document.
 */
public class RMRTFWriter {

    // Font & Color table
    List <FontFile>    _fontTable;
    List <RMColor>       _colorTable;
    
    // settings that persist across shapes
    RMParagraph          _currentParagraph;
    RMFont               _currentFont;
    RMColor              _currentColor;
    
    // Current font style
    boolean              _isBold, _isItalic, _isUnderline;
    
    // current table nesting level
    int                  _tableLevel;

/**
 * Returns RTF bytes for given document.
 */
public byte[] getBytes(RMDocument aDoc)
{
    // Validate and resolve page references
    aDoc.layoutDeep();
    aDoc.resolvePageReferences();

    // Allocate font & color tables
    _fontTable = new ArrayList();
    _colorTable = new ArrayList(); _colorTable.add(RMColor.black); // Init with black
    
    // Set the current color & paragraph styles to the defaults
    _currentColor = RMColor.black; _currentParagraph = getRTFParagraphDefaults();
    _currentFont = null; _isBold = _isItalic = _isUnderline = false;
    
    // Create print stream and write the header
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(byteStream);
    ps.println("{\\rtf1\\mac\\ansi\\uc0 ");
    
    // Generate the body of the rtf
    _tableLevel = 0;
    byte body[] = getRTFForShapes(aDoc);
    
    // Output the font & color tables
    writeFontTable(ps);
    writeColorTable(ps);
    
    // Output the main body;
    ps.write(body, 0, body.length);
    
    // Close the top level and return bytes
    ps.println("}");
    return byteStream.toByteArray();
}

/**
 * Looks up a font in the font table and adds it if it's not there.
 * Returns the name that represents the font in the rtf ("f0", "f1", etc.)
 */
public int getFontIndex(RMFont f)
{
    FontFile fontfile = f.getFontFile();
    int i = _fontTable.indexOf(fontfile);
    if(i<0) { i = _fontTable.size(); _fontTable.add(fontfile); }
    return i;
}

/**
 * Output all the fonts as an RTF FontTable. Fonts are all referenced by name, never embedded.
 */
public void writeFontTable(PrintStream ps) 
{
    int nfonts = _fontTable.size();
    
    if (nfonts > 0) {
        ps.print("{\\fonttbl");
        for(int i=0; i<nfonts; ++i) { FontFile ff = _fontTable.get(i);
            ps.print("\\f"+i);
            writeFont(ps, ff);
        }
        ps.println("}");
    }
}

/**
 * Writes an entry in the font table for a particular font.
 */
public void writeFont(PrintStream aPS, FontFile aFontFile) 
{
    aPS.print(getRTFFontFamily(aFontFile)+" ");
    aPS.print("\\fcharset" + getRTFFontCharset(aFontFile) + " ");
    aPS.print(aFontFile.getName()+";");
}

public String getRTFFontFamily(FontFile f)
{
    /*  Figure out which of 
     *  \froman (proportionally spaced, serif)
     *  \fswiss (proportionally spaced, sans serif)
     *  \fmodern (fixed pitch, sans serif)
     *  \fscript (script)
     *  \fdecor  (decorative)
     *  \ftech (Technical, Symbol, & Mathematical)
     *  \fbidi (bi-directional - yeah, right)
     */
    return "\\fnil";   // \fnil means unknown
}

// Really just a shot in the dark
public int getRTFFontCharset(FontFile f)  { return 77; } // Mac

/**
 * Output all the fonts as an RTF fonttable. Fonts are all referenced by name, never embedded.
 */
public void writeColorTable(PrintStream ps) 
{
    int ncolors = _colorTable.size();
    
    if(ncolors > 0) {
        ps.print("{\\colortbl");
        for(int i=0; i<ncolors; ++i) { RMColor c = _colorTable.get(i);
            int r = c.getRedInt(), g = c.getGreenInt(), b = c.getBlueInt();
            
            // black is special
            if(r==0 && g==0 && b==0) 
                ps.print(";");
            else ps.print("\\red"+r+"\\green"+g+"\\blue"+b+";");
        }
        ps.println("}");
    }
}

public int getColorIndex(RMColor c)
{
    int i = _colorTable.indexOf(c);
    if(i<0) { i = _colorTable.size(); _colorTable.add(c); }
    return i;
}

/**
 * Convert an rm coord to rtf 'twips'.  A twip is a 20th of a point.
 */
public int twip(double x)  { return (int)(20*x); }

/**
 * Returns RTF for document Shapes.
 */
public byte[] getRTFForShapes(RMDocument aDoc)
{
    ByteArrayOutputStream outs = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(outs);
    
    // Output the paper size and margins
    ps.print("\\paperw" + twip(aDoc.getPage(0).getWidth()));
    ps.print("\\paperh" + twip(aDoc.getPage(0).getHeight()));
    ps.print("\\margl" + twip(aDoc.getMarginLeft()));
    ps.print("\\margr" + twip(aDoc.getMarginRight()));
    ps.print("\\margb" + twip(aDoc.getMarginBottom()));
    ps.print("\\margt" + twip(aDoc.getMarginTop()));
    ps.print("\\viewkind1"); // page layout view mode
    ps.println();
    
    // add a hard page break between pages
    for(int i=0, n=aDoc.getPageCount(); i<n; ++i) {
        if(i>0) ps.println("\\page");
        appendRTF(aDoc.getPage(i), ps);
    }
    
    ps.flush();
    return outs.toByteArray();
    
}

/**
 * Returns a new list with all the shapes to be output.
 */
List getRTFShapes(RMShape parent)  { List shapes = new ArrayList(); getRTFShapes(parent, shapes); return shapes; }

/**
 * Similar to Excel getSheetShapes(), which pulls out shapes that should be placed inside a table.  In the case of RTF,
 * however, everything goes in a table.  Therefore this routine just pulls out anything that we know how
 * to put in an rtf file. Anything weird we can use the jpg stuff to turn into an image.
 */
void getRTFShapes(RMShape aShape, List aList)
{
    if(!aShape.isVisible()) return; // If shape not visible, just return
    if(isRTFShape(aShape)) aList.add(aShape); // Add rtf for shape
    else for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) // recurse for every child
        getRTFShapes(aShape.getChild(i), aList);
}

/**
 * Returns true if this shape should can be represented in the rtf.
 */
boolean isRTFShape(RMShape aShape)
{
    return aShape instanceof RMRectShape || aShape instanceof RMImageShape || aShape instanceof RMOvalShape ||
        aShape instanceof RMPolygonShape || aShape instanceof RMScene3D;
}

public void appendRTF(RMShape aShape, PrintStream ps)
{
    // Table representing the page is placed relative to margins of the page, whereas nested tables are relative to ...
    if(aShape instanceof RMPage) {
        Rect bounds = null; //((RMPage)aShape).getDocument().getMarginRect();
        RMShapeTable cells = RMShapeTable.createTable(getRTFShapes(aShape), aShape, bounds);
        appendTable(cells, ps);
    }
        
    // Get the real shape this that goes in this cell
    else {
        
        // If shape is crosstab cell with cell shape, swap in ell shape
        if(aShape instanceof RMShapeTable.Cell &&
            ((RMShapeTable.Cell)aShape).getCellShape()!=null) 
            aShape = ((RMShapeTable.Cell)aShape).getCellShape();
        
        // Append text or image for shape
        if(aShape instanceof RMTextShape) appendText(((RMTextShape)aShape).getXString(), ps);
        else appendImageBytesForShape(aShape, ps);
    }
}

/**
 * Append table.
 * Tables are intermediary objects used for positioning content. Thus, tables themselves shouldn't do any drawing
 */
public void appendTable(RMShapeTable table, PrintStream ps)
{
    // empty docs can lead to null tables
    if(table==null) return;
    
    // Nested tables are differentiated by the itapN control word
    _tableLevel++;
    
    // Get ColumnCount
    int columnCount = table.getColCount();
    
    // TextEdit just sets the cellx values as if all cells were the same and the row always total to 6".  Bizarre.  
    int cellx = twip(72*6)/columnCount;
    int cellws[] = new int[columnCount]; for(int c=0;c<columnCount;c++) cellws[c] = twip(table.getColWidth(c));
    int pads = 0, border = 0; // cell padding and border weight
    
    // Iterate over table rows
    for(int r=0, nrows=table.getRowCount(); r<nrows; ++r) {
        
        // Basic setup
        ps.print("\\itap" + _tableLevel + "\\trowd "); // nest level - and table row defaults
        ps.println("\\trbrdrt\\brdrnil\\trbrdrl\\brdrnil\\trbrdrr\\brdrnil"); // no borders anywhere
        ps.println("\\trgaph0\\trleft0"); // table shouldn't add any spaces itself, so set all the coords to 0
        ps.println("\\trrh" + twip(table.getRowHeight(r))); // set the (minimum) height of the row

        // Iterate over columns and add the cells definitions for the row
        for(int c=0; c<columnCount; ++c) { RMShapeTable.Cell rmcell = table.getCell(r,c);
            int rowspan = rmcell.getRowSpan();
            int colspan = rmcell.getColumnSpan();
            
            // For merged cells, output a 'cl[v]mgf' for the first occurance, and a 'cl[v]mrg' thereafter
            if(colspan>1) {
                if(rmcell.getColumn()==c) ps.print("\\clmgf");
                else ps.print("\\clmrg");
            }
            if(rowspan>1) {
                if(rmcell.getRow()==r) ps.print("\\clvmgf");
                else ps.print("\\clvmrg");
            }
            
            // Cell shading
            RMFill fill = rmcell.getFill();
            if(fill != null) ps.print("\\clcbpat" + getColorIndex(fill.getColor()));
            else ps.print("\\clshdrawnil");

            // Vertical alignment
            ps.print("\\clvertal");
            switch(rmcell.getAlignmentY()) {
                case Top : ps.print("t"); break;
                case Middle : ps.print("c"); break;
                default : ps.print("b");
            }
            
            // Cell height - this doesn't seem to be a real rtf control word, but TextEdit needs it.  The ignore row
            // height (trrh) in favor of this made-up control
            ps.print("\\clheight"+twip(rmcell.getHeight()));
            
            // Cell borders
            RMShape cshape = rmcell.getCellShape();
            RMCrossTabCell ctcell = cshape instanceof RMCrossTabCell? (RMCrossTabCell)cshape : null;
            ps.print("\\clbrdrt"+RTFBorderStyle(ctcell!=null && ctcell.isShowTopBorder(),20));
            ps.print("\\clbrdrb"+RTFBorderStyle(ctcell!=null && ctcell.isShowBottomBorder(),20));
            ps.print("\\clbrdrl"+RTFBorderStyle(ctcell!=null && ctcell.isShowLeftBorder(),20));
            ps.print("\\clbrdrr"+RTFBorderStyle(ctcell!=null && ctcell.isShowRightBorder(),20));
            
            // CellX defines the right margin of the cell but clwWidth seems to be more important
            int cw = twip(rmcell.getWidth()) - pads - border;
            ps.print("\\clwWidth" + cw + "\\clftsWidth3");
            ps.print("\\clpadl" + pads + "\\clpadr" + pads);
            ps.println("\\cellx" + cellx*(c+1));
        }
        
        // Iterate over cells in row again and output the actual cell data, possibly recursing for nested tables
        for(int c=0; c<columnCount; ++c) { RMShapeTable.Cell rmcell = table.getCell(r, c);
            
            // Add the rtf for the text (or shapes) inside the cell, otherwise empty paragraph for merged cell
            if((rmcell.getRow()==r) && (rmcell.getColumn()==c))
                appendRTF(rmcell, ps);
            else ps.print("\\pard\\intbl\\itap" + _tableLevel);
            
            // End the cell
            ps.println(_tableLevel>1?"\\nestcell":"\\cell");

        }
        // End the row
        if(r==nrows-1) ps.print("\\lastrow");
        ps.println(_tableLevel>1?"\\nestrow":"\\row");
    }
    
    // restore previous table level
    --_tableLevel;
}

public void appendText(RMXString s, PrintStream ps)
{
    String text = s.toString();
    boolean newParagraph = true;
    
    // Iterate over runs
    for(int i=0, n=s.getRunCount(); i<n; ++i) { RMXStringRun run = s.getRun(i);
        
        // new paragraph at the start and then at each carriage return (when is \par used?)
        if (newParagraph || !run.getParagraph().equals(_currentParagraph)) {
            ps.print("\\pard");
            // we just reset the paragraph defaults above, so reset the currentParagraph
            _currentParagraph = getRTFParagraphDefaults();
            
            // we're always somewhere inside a table
            ps.print("\\intbl\\itap"+_tableLevel);
            RMParagraph par = run.getParagraph();
            //if any of the paragraph settings have changed, set them
            if (!par.equals(_currentParagraph)) {
                // paragraph alignment
                if (par.getAlignmentX() != _currentParagraph.getAlignmentX()) {
                    switch(par.getAlignmentX()) {
                        case Center : ps.print("\\qc"); break;
                        case Full : ps.print("\\qj"); break;
                        case Left : ps.print("\\ql"); break;
                        case Right : ps.print("\\qr"); break;
                    }
                }
                // tabs
                if (!Arrays.equals(par.getTabs(), _currentParagraph.getTabs()) ||
                    !Arrays.equals(par.getTabTypes(), _currentParagraph.getTabTypes())) {
                    for(int j=0, ntabs=par.getTabCount(); j<ntabs; ++j) {
                        switch(par.getTabType(j)) {
                        case RMParagraph.TAB_LEFT: break; // the default
                        case RMParagraph.TAB_RIGHT: ps.print("\\tqr"); break;
                        case RMParagraph.TAB_CENTER: ps.print("\\tqc"); break;
                        case RMParagraph.TAB_DECIMAL: ps.print("\\tqdec"); break;
                        }
                        ps.print("\\tx"+twip(par.getTab(j)));
                    }
                }
                
                // left indent, right indent, line spacing, etc. goes here
                _currentParagraph = par;
            }
            newParagraph = false;
        }
                    
        // Update current font
        RMFont font = run.getFont();
        if(_currentFont==null || !_currentFont.equals(font)) {
            
            // Font size units are half-points.  Who comes up with this shit?
            ps.print("\\f" + getFontIndex(font));
            ps.print("\\fs" + (int)(font.getSize()*2));
            _currentFont = font;
            
            // Update bold/italic (note that in RM this is a font property, whereas in RTF it's a character property)
            if(_isBold != font.isBold()) {
                _isBold = !_isBold; ps.print("\\b"+ (_isBold ? "" : "0")); } 
            if(_isItalic != font.isItalic()) {
                _isItalic = !_isItalic; ps.print("\\i" + (_isItalic ? "" : "0")); }
        }
        
        // Update text color
        RMColor color = run.getColor();
        if(!_currentColor.equals(color)) {
            ps.print("\\cf"+getColorIndex(color)); _currentColor = color; }
        
        // Update underlining: turn on or off
        if(_isUnderline != run.isUnderlined()) {
            _isUnderline = !_isUnderline; ps.print("\\ul" + ( _isUnderline ? "":"0"));  }
        
        // delimeter before raw text
        ps.println();
       
        // Iterate over run characters and write them
        for(int cp = run.start(), cend = run.end(); cp<cend; ++cp) { char c = text.charAt(cp);
        
            // A note about unicode escapes: The spec says that if you are emitting "\ u" unicode chars,
            // you should emit characters after it from the non-unicode codepage of the document which could be
            // substituted for the unicode character you really want for compatibility with rtf readers that don't do
            // unicode.  A "\ uc" control tells a valid unicode parser how many characters have been used for
            // compatibility, and so should be skipped over. "uc0", therefore, means no compatibility chars have been
            // generated. Some generators seems to constantly generate "uc0" controls.
            // We do it once in the header, and never generate compatibility characters.
            if(c>127)  // output anything beyond 7 bit ascii as 16-bit unicode
                ps.print("\\u"+(int)c+" ");
            
            // escape meaningful characters
            else if(c=='\n' || c=='\\' || c=='{' || c=='}')
                ps.print("\\"+c);
            
            // tabs and all other plain ascii chars get emitted as-is. 
            else if(c>=32 || c=='\t')
                ps.print(c);
        }
        
    }
}

/**
 * Returns the rtf control string for borders. Currently only does on or off.  RTF has a zillion other possibilities.
 */
public String RTFBorderStyle(boolean show, int width)  { return show? ("\\brdrs\\brdrw" + width) : "\\brdrnil"; }

/**
 * Convert the shape into an image stream and embedd that into the rtf.
 */
public void appendImageBytesForShape(RMShape s, PrintStream ps) 
{
    Image img = RMShapeUtils.createImage(s, Color.WHITE);
    byte[] pngBytes = img.getBytesPNG(); if(pngBytes==null) return;
    
    // The image code above uses getBoundsMarked(), so a rectangle of 100x100 might wind up returning an image of
    // 102x102 to account for the stroke width. The table building code, however doesn't use getBoundsMarked, so this
    // is probably going to cause trouble.
    Rect bounds = s.getBoundsMarked();
    
    ps.println();
    ps.print("\\*\\shppict {\\pict\\pngblip");
    ps.print("\\picwgoal" + twip(bounds.getWidth()) + "\\pichgoal" + twip(bounds.getHeight()));
    ps.print(" ");
    ps.print(ASCIICodec.encodeHex(pngBytes));
    ps.print("}");
}

/**
 * Creates and RMParagraph object whose values are set to the defaults assigned by the RTF spec.
 * The RMParagraph defaultParagraph is very similar to the rtf defaults:
 * tabs every 36 points (720 twips) align = left, leftIndent = rightIndent=0, lineSpacing=1
 */
RMParagraph getRTFParagraphDefaults()  { return new RMParagraph(); }

}