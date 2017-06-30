/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;

/**
 * This class represents a shape for a document inside a page of another document.
 */
public class RMNestedDoc extends RMParentShape {
    
    // Actual document
    RMDocument      _doc;
    
    // Selected page index
    int             _pageIndex = 0;
    
/**
 * Returns the nested document.
 */
public RMDocument getNestedDoc()  { return _doc; }

/**
 * Sets the nested document.
 */
public void setNestedDoc(RMDocument aDoc)
{
    _doc = aDoc;
    copyShape(aDoc.getPage(0));
    setSelectedPageIndex(0);
}

/**
 * Returns the selected page index.
 */
public int getSelectedPageIndex()  { return _pageIndex; }

/**
 * Sets the selected page index for the nested doc.
 */
public void setSelectedPageIndex(int anIndex)
{
    // Set page index
    _pageIndex = Math.min(anIndex, getNestedDoc().getPageCount() - 1);
    
    // Get selected page
    RMPage page = getSelectedPage();
    
    // Remove children and add children from nested document
    removeChildren();
    for(int i=page.getChildCount()-1; i>=0; i--)
        addChild(page.getChild(i), 0);
}

/**
 * Returns the selected page.
 */
public RMPage getSelectedPage()  { return getNestedDoc().getPage(getSelectedPageIndex()); }

/**
 * Editor method - indicates that nested doc is super-selectable.
 */
public boolean superSelectable() { return true; }

}