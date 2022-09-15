/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.shape.*;
import org.apache.poi.hssf.usermodel.*;
import snap.geom.Point;
import snap.geom.Rect;

/**
 * This class is used to associate a HSSFSheet with other HSSF objects that are specific to the sheet, but that
 * cannot be gotten directly from the sheet through the HSSF api. For example, the only way to get the
 * drawingPatriarch is with the createDrawingPatriarch() method, which blows away any existing shapes.
 * Therefore, you can only call this once per sheet.  How stupid.
 */
class RMExcelSheet {

    // The sheet
    HSSFSheet _sheet;

    // The top-level ancestor of free-form shapes
    HSSFPatriarch _patriarch;

    // The point in shape space which corresponds to 0,0 in the patriarch
    Point _sheetOrigin;

    /**
     * Create new excel sheet.
     */
    public RMExcelSheet(HSSFSheet aSheet)
    {
        _sheet = aSheet;
    }

    public HSSFSheet getSheet()
    {
        return _sheet;
    }

    public void setOrigin(Point pt)
    {
        _sheetOrigin = pt;
    }

    public HSSFPatriarch getPatriarch()
    {
        return _patriarch != null ? _patriarch : (_patriarch = _sheet.createDrawingPatriarch());
    }

    /**
     * The coords in excel are specified as offsets from a particular row & column. Four numbers are required to specify
     * an arbitrary point on the sheet - row,column,dx,dy. dx & dy represent percentages of a columnwidth or rowheight.
     * The values of dy are in units of 1/256 of the height of the row.
     * The values of dx are in units of 1/1024 of a column width.
     * For example, an Excel Y coordinate of :
     * ExcelY={row=5, dx=128}
     * would convert to absolute sheet Y coordinate :
     * SheetY=Sum(rowHeights[1...4])+rowHeights[5]*128/256
     * How screwy. Since each row can be a different size, converting from absolute coordinates to excel coordinates
     * involves figuring out the size of everything.
     */
    private static class ExcelPoint {

        // row,col  range={0, 65280}
        public short _row, _column;

        // x offset (units of 1/1024 of column width) range={0, 1023}
        public int _dx;

        // y offset (in units of 1/256 of row height) range={0,255}
        public int _dy;

        public ExcelPoint(short aRow, short aCol, int xoff, int yoff)
        {
            _row = aRow;
            _column = aCol;
            _dx = xoff;
            _dy = yoff;
        }

        public String toString()
        {
            return "{ row=" + _row + "+" + _dy / 256f + ", col=" + _column + "+" + _dx / 1024f + " }";
        }
    }

    public ExcelPoint convertPoint(Point pt)
    {
        int r, c, dx, dy;
        float height = 0, rowheight = _sheet.getDefaultRowHeightInPoints();
        HSSFRow row = null;

        // negative origins clamped.  Could cause distortion
        float y = (float) Math.max(pt.getY() - _sheetOrigin.getY(), 0);
        for (r = 0; r < 65280; ++r) {
            row = _sheet.getRow(r);
            if (row == null) {
                // add extra rows if needed.  Height is the same as the last real row
                row = _sheet.createRow(r);
                row.setHeightInPoints(rowheight);
            } else
                rowheight = row.getHeightInPoints();
            if (height + rowheight > y)
                break;
            height += rowheight;
        }
        dy = (int) (256 * (y - height) / rowheight);

        // The x is similar to the y, with 2 main differences. First, there is no getColumnWidthInPoints() method.  Column
        // widths are specified in terms of character widths. See description of RMExcelWriter.getCharWidthFromPoints()
        // Second, the dx is in units of 1/1024 of the column width
        int maxColumnInRow = row.getLastCellNum();
        float width = 0, colwidth = 256 * 13;

        // Use floating point version of width, since dx units have greater precision that column width units
        double x = RMExcelWriter.getCharWidthFromPoints(Math.max(pt.getX() - _sheetOrigin.getX(), 0)) * 256;
        for (c = 0; c < 255; ++c) {
            // append new cells to the row if needed
            if (c > maxColumnInRow) {
                row.createCell(c);
                // might have valid cells in rows above, in which case you don't want to change the column width.
                // An empty column is supposed to be 0, but poi returns 8
                if (_sheet.getColumnWidth(c) <= 8)
                    _sheet.setColumnWidth(c, (short) colwidth);
            }

            // column widths are unsigned shorts, so make sure not to sign extend
            colwidth = _sheet.getColumnWidth(c) & 0xffff;

            if (width + colwidth > x)
                break;
            width += colwidth;
        }
        dx = (int) (1024 * (x - width) / colwidth);

        return new ExcelPoint((short) r, (short) c, dx, dy);
    }

    public HSSFShape addNewShape(RMShape aShape, HSSFShapeContainer aParent)
    {
        HSSFShape hssfShape = null;

        // get the bounds with bounds() instead of getBounds() to preserve possible negative values
        Rect bounds = aShape.bounds();

        if (aParent == null)
            aParent = getPatriarch();

        // The patriarch is the top level shape. The only kind of parent shape below the patriarch is a group shape.
        // The patriarch and the group use different kind of anchors, however.
        if (aParent instanceof HSSFPatriarch) {
            ExcelPoint origin = convertPoint(bounds.getXY());
            ExcelPoint max = convertPoint(new Point(bounds.getMaxX(), bounds.getMaxY()));

            HSSFClientAnchor anchor = new HSSFClientAnchor(origin._dx, origin._dy, max._dx, max._dy, origin._column, origin._row, max._column, max._row);

            // Handle image
            if (aShape instanceof RMImageShape)
                hssfShape = ((HSSFPatriarch) aParent).createPicture(anchor, 0); // fill in index later

                // Handle text
            else if (aShape instanceof RMTextShape && ((RMTextShape) aShape).length() > 0)
                hssfShape = ((HSSFPatriarch) aParent).createTextbox(anchor);

                // Handle everything else
            else hssfShape = ((HSSFPatriarch) aParent).createSimpleShape(anchor);
        }

        // Handle HSSFShapeGroup
        else if (aParent instanceof HSSFShapeGroup) {
            // Get points in coordinate system of group. For the HSSFChildAnchor, I think it's in % of the parent
            HSSFChildAnchor anchor = new HSSFChildAnchor(/*TODO:  This is nonsense!*/0, 0, 0, 0);
            hssfShape = ((HSSFShapeGroup) aParent).createShape(anchor);
        }

        // Return HSSFShape
        return hssfShape;
    }

    public HSSFShape addRect(RMShape aRect, HSSFShapeContainer aParent)
    {
        HSSFSimpleShape s = (HSSFSimpleShape) addNewShape(aRect, aParent);
        s.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
        return s;
    }

    public HSSFShape addLine(RMShape aLine, HSSFShapeContainer aParent)
    {
        HSSFSimpleShape s = (HSSFSimpleShape) addNewShape(aLine, aParent);
        s.setShapeType(HSSFSimpleShape.OBJECT_TYPE_LINE);
        return s;
    }

}