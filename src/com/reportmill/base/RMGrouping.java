/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This object represents an individual grouping with attributes like key, sorts, topN sort, etc.
 */
public class RMGrouping implements Cloneable, XMLArchiver.Archivable {
    
    // The grouping key
    String           _key;
    
    // The list of sorts
    List <RMSort>    _sorts = new Vector();
    
    // The top N sort
    RMTopNSort       _topNSort = new RMTopNSort(null, RMSort.ORDER_ASCEND, 0, false);
    
    // Values (in the form of comma separated keychain keys) explicitly defined to sort or to include
    String           _values;
    
    // Whether to sort on explicitly defined values
    boolean          _sortOnValues;
    
    // Whether to explicitly include explicitly defined values
    boolean          _includeValues;
    
    // Whether grouping includes all values for grouping key found in entire dataset in every subgroup
    boolean          _includeAllValues = false;
    
    // Whether grouping has header
    boolean          _hasHeader = false;
    
    // Whether grouping has details
    boolean          _hasDetails = false;
    
    // Whether grouping has summary
    boolean          _hasSummary = false;
    
    // Selected sort index (used in editer only)
    int              _selectedSortIndex = -1;

    // The PropChangeSupport
    PropChangeSupport  _pcs = PropChangeSupport.EMPTY;

/**
 * Creates an empty grouping.
 */
public RMGrouping() { }

/**
 * Creates a grouping with the given key.
 */
public RMGrouping(String aKey)  { _key = aKey; }

/**
 * Returns the grouping key.
 */
public String getKey()  { return _key; }

/**
 * Sets the grouping key.
 */
public void setKey(String aValue)
{
    if(SnapUtils.equals(aValue, _key)) return;
    firePropChange("Key", _key, _key = aValue);
}

/**
 * Returns the groupings list of sorts.
 */
public List <RMSort> getSorts()  { return _sorts; }

/**
 * Returns the number of sorts in the grouping.
 */
public int getSortCount()  { return _sorts.size(); }

/**
 * Returns the individual sort at the given index.
 */
public RMSort getSort(int anIndex)  { return _sorts.get(anIndex); }

/**
 * Adds the given sort to the grouping.
 */
public RMGrouping addSort(RMSort aSort)  { addSort(aSort, getSortCount()); return this; }

/**
 * Adds the given sort to the grouping.
 */
public void addSort(RMSort aSort, int anIndex)
{
    // Add sort, set selected sort index, firePropertyChange
    _sorts.add(anIndex, aSort);
    _selectedSortIndex = anIndex;
    firePropChange("Sort", null, aSort, anIndex);
}

/**
 * Removes the sort at the given index.
 */
public RMSort removeSort(int anIndex)
{
    // Remove sort, set selected sort index, firePropertyChange, return sort
    RMSort sort = _sorts.remove(anIndex);
    _selectedSortIndex = Math.min(anIndex, getSortCount() - 1);
    firePropChange("Sort", sort, null, anIndex);
    return sort;
}

/**
 * Adds the list of sorts to the grouping.
 */
public void addSorts(List<RMSort> aList)  { for(RMSort s : aList) addSort(s); }

/**
 * Adds a sort to the grouping for the given sort key.
 */
public void addSort(String aSortKey)  { addSort(new RMSort(aSortKey)); }

/**
 * Removes the given sort from the grouping.
 */
public int removeSort(RMSort aSort)
{
    int index = ListUtils.indexOfId(getSorts(), aSort);
    if(index>=0) removeSort(index);
    return index;
}

/**
 * Moves a sort from the source index to the destination index.
 */
public void moveSort(int fromIndex, int toIndex)
{
    RMSort sort = removeSort(fromIndex);
    addSort(sort, toIndex);
}

/**
 * Returns the top N sort for the grouping.
 */
public RMTopNSort getTopNSort()  { return _topNSort; }

/**
 * Sets the top N sort for the grouping.
 */
public void setTopNSort(RMTopNSort aSort)
{
    if(SnapUtils.equals(aSort, _topNSort)) return;
    firePropChange("TopNSort", _topNSort, _topNSort = aSort);
}

/**
 * Returns whether the grouping includes all values.
 */
public boolean getIncludeAllValues()  { return _includeAllValues; }

/**
 * Sets whether the grouping includes all values.
 */
public void setIncludeAllValues(boolean aValue)
{
    if(aValue==_includeAllValues) return;
    firePropChange("IncludeAllValues", _includeAllValues, _includeAllValues = aValue);
}

/**
 * Returns the values (in the form of comma separated keychain keys) explicitly defined to sort or to include. 
 */
public String getValuesString()  { return _values; }

/**
 * Sets the values (in the form of comma separated keychain keys) explicitly defined to sort or to include. 
 */
public void setValuesString(String aString)
{
    if(SnapUtils.equals(aString, _values)) return;
    firePropChange("ValuesString", _values, _values = aString);
}

/**
 * Returns a list of explicit values for this grouping.
 */
public List getValues()
{
    // Create list to return
    List valuesList = new ArrayList();
    
    // If no values string, just return
    if(_values==null)
        return valuesList;
    
    // Get values with commas replaced by newlines
    String valuesString = _values.replace(',', '\n');
    
    // Convert to string array
    String valueStrings[] = valuesString.split("\n");
    
    // Iterate over values strings
    for(int i=0; i<valueStrings.length; i++) {
        
        // Get current loop value trimmed
        String valueString = valueStrings[i].trim();
        
        // If length is non-zero, evaluate and add
        if(valueString.length()>0) {
            
            // Evaluate value string as key (maybe it would be useful to support keys on aReportMill one day?)
            Object value = RMKeyChain.getValue(new Object(), valueString);
            
            // If value is non-null, add it
            if(value!=null)
                valuesList.add(value);
        }
    }
    
    // Return valuesList
    return valuesList;
}

/**
 * Returns whether to sort on values explicitly provided. 
 */
public boolean getSortOnValues()  { return _sortOnValues; }

/**
 * Sets whether to sort on values explicitly provided. 
 */
public void setSortOnValues(boolean aFlag)
{
    if(aFlag==_sortOnValues) return;
    firePropChange("SortOnValues", _sortOnValues, _sortOnValues = aFlag);
}

/**
 * Returns whether to include values explicitly provided. 
 */
public boolean getIncludeValues()  { return _includeValues; }

/**
 * Sets whether to include values explicitly provided. 
 */
public void setIncludeValues(boolean aFlag)
{
    if(aFlag==_includeValues) return;
    firePropChange("IncludeValues", _includeValues, _includeValues = aFlag);
}

/**
 * Returns whether the grouping has a header.
 */
public boolean getHasHeader()  { return _hasHeader; }

/**
 * Sets whether the grouping has a header.
 */
public void setHasHeader(boolean aValue)
{
    if(aValue==_hasHeader) return;
    firePropChange("HasHeader", _hasHeader, _hasHeader = aValue);
}

/**
 * Returns whether the grouping has a details.
 */
public boolean getHasDetails()  { return _hasDetails; }

/**
 * Sets whether the grouping has a details.
 */
public void setHasDetails(boolean aValue)
{
    if(aValue==_hasDetails) return;
    firePropChange("HasDetails", _hasDetails, _hasDetails = aValue);
}

/**
 * Returns whether the grouping has a summary.
 */
public boolean getHasSummary()  { return _hasSummary; }

/**
 * Sets whether the grouping has a summary.
 */
public void setHasSummary(boolean aValue)
{
    if(aValue==_hasSummary) return;
    firePropChange("HasSummary", _hasSummary, _hasSummary = aValue);
}

/**
 * Returns the currently selected grouping's currently selected sort (for editing, mostly).
 */
public int getSelectedSortIndex()  { return _selectedSortIndex; }

/**
 * Sets the currently selected grouping's currently selected sort (for editing, mostly).
 */
public void setSelectedSortIndex(int anIndex)  { _selectedSortIndex = anIndex; }

/**
 * Returns the currently selected grouping's sort (while editing only).
 */
public RMSort getSelectedSort()
{
    // If selected sort index is out of bounds, just return null
    if(_selectedSortIndex<0 || _selectedSortIndex>=getSortCount())
        return null;
    
    // Return selected sort
    return getSort(_selectedSortIndex);
}

/**
 * Add listener.
 */
public void addPropChangeListener(PropChangeListener aLsnr)
{
    if(_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
    _pcs.addPropChangeListener(aLsnr);
}

/**
 * Remove listener.
 */
public void removePropChangeListener(PropChangeListener aLsnr)  { _pcs.removePropChangeListener(aLsnr); }

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal)
{
    if(!_pcs.hasListener(aProp)) return;
    PropChange pc = new PropChange(this, aProp, oldVal, newVal);
    firePropChange(pc);
}

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
{
    if(!_pcs.hasListener(aProp)) return;
    PropChange pc = new PropChange(this, aProp, oldVal, newVal, anIndex);
    firePropChange(pc);
}

/**
 * Fires a given property change.
 */
protected void firePropChange(PropChange aPC)
{
    _pcs.firePropChange(aPC);
}

/**
 * Standard clone implementation.
 */
public RMGrouping clone()
{
    // Do normal clone
    RMGrouping clone = null; try { clone = (RMGrouping)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
    
    // Clear PropChangeSupport
    clone._pcs = PropChangeSupport.EMPTY;
    
    // Clone Sorts
    clone._sorts = new ArrayList(_sorts.size());
    for(RMSort s : _sorts) {
        RMSort s2 = s.clone();
        clone.addSort(s2);
    }
    
    // Clone TopNSort and return
    clone._topNSort = (RMTopNSort)_topNSort.clone();
    return clone;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other grouping
    if(anObj==this) return true;
    RMGrouping other = anObj instanceof RMGrouping? (RMGrouping)anObj : null; if(other==null) return false;
    
    // Check key, sorts, TopNSort
    if(!SnapUtils.equals(other._key, _key)) return false;
    if(!SnapUtils.equals(other._sorts, _sorts)) return false;
    if(!SnapUtils.equals(other._topNSort, _topNSort)) return false;
    
    // Check IncludeAllValues, Values, SortOnValues, IncludeValues
    if(other._includeAllValues!=_includeAllValues) return false;
    if(!SnapUtils.equals(other._values, _values)) return false;
    if(other._sortOnValues!=_sortOnValues) return false;
    if(other._includeValues!=_includeValues) return false;
    
    // Check has header, details, summary
    if(other._hasHeader!=_hasHeader) return false;
    if(other._hasDetails!=_hasDetails) return false;
    if(other._hasSummary!=_hasSummary) return false;
    return true; // Return true if all checks pass
}

/**
 * Returns string representation of grouping. 
 */
public String toString()  { return getClass().getSimpleName() + ": " + getKey(); }

/**
 * XML Archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named grouper
    XMLElement e = new XMLElement("grouping");
    
    // Archive key, sorts
    if(_key!=null && _key.length()>0) e.add("key", _key);
    for(int i=0, iMax=_sorts.size(); i<iMax; i++)
        e.add(getSort(i).toXML(anArchiver));
        
    // Archive top n sort key, order, count, include others, pad
    if(_topNSort.getKey()!=null && _topNSort.getKey().length()>0) e.add("topn", _topNSort.getKey());
    if(_topNSort.getOrder()!=RMSort.ORDER_ASCEND) e.add("topn-order", _topNSort.getOrderString());
    if(_topNSort.getCount()>0) e.add("topn-count", _topNSort.getCount());
    if(_topNSort.getIncludeOthers()) e.add("topn-include", true);
    if(_topNSort.getPad()) e.add("topn-pad", true);
    
    // Archive includeAllValues, values string, sortOnValues, includeValues
    if(_includeAllValues) e.add("allvalues", true);
    if(_values!=null && _values.length()>0) e.add("values", _values);
    if(_sortOnValues) e.add("sort-on-values", _sortOnValues);
    if(_includeValues) e.add("include-values", _includeValues);
        
    // Archive hasHeader, hasDetails, hasSummary
    if(_hasHeader) e.add("header", true);
    if(_hasDetails) e.add("details", true);
    if(_hasSummary) e.add("summary", true);
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive key
    if(anElement.hasAttribute("key"))
        setKey(anElement.getAttributeValue("key"));
    
    // Unarchive sorts
    _sorts = anArchiver.fromXMLList(anElement, "sort", null, this);
        
    // Unarchive top n sort key
    if(anElement.hasAttribute("topn"))
        _topNSort.setKey(anElement.getAttributeValue("topn"));
    
    // Unarchive top n sort order
    if(anElement.hasAttribute("topn-order"))
        _topNSort.setOrderString(anElement.getAttributeValue("topn-order"));
    
    // Unarchive top n sort count
    if(anElement.hasAttribute("topn-count"))
        _topNSort.setCount(anElement.getAttributeIntValue("topn-count"));

    // Unarchive top n sort includeOthers
    if(anElement.hasAttribute("topn-include"))
        _topNSort.setIncludeOthers(anElement.getAttributeBoolValue("topn-include"));
        
    // Unarchive top n sort pad
    if(anElement.hasAttribute("topn-pad"))
        _topNSort.setPad(anElement.getAttributeBoolValue("topn-pad"));
        
    // Unarchive includeAllValues
    setIncludeAllValues(anElement.getAttributeBoolValue("allvalues"));
    
    // Unarchive values string
    _values = anElement.getAttributeValue("values");
    
    // Unarchive sort on values
    if(anElement.hasAttribute("sort-on-values"))
        _sortOnValues = anElement.getAttributeBoolValue("sort-on-values");
    
    // Unarchive include values
    if(anElement.hasAttribute("include-values"))
        _includeValues = anElement.getAttributeBoolValue("include-values");
    
    // Unarchive hasHeader, hasDetails, hasSummary
    setHasHeader(anElement.getAttributeBoolValue("header"));
    setHasDetails(anElement.getAttributeBoolValue("details"));
    setHasSummary(anElement.getAttributeBoolValue("summary"));
    return this; // Return this grouping
}

}