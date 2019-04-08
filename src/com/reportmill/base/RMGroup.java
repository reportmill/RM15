/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.ListUtils;

/**
 * This class is a smart List subclass used to hold objects from an original list broken down by grouping keys. 
 */
public class RMGroup extends ArrayList implements RMKey.Get, RMKeyChain.Get {
    
    // Grouping key for children in this group
    String    _key;
    
    // The unique value for objects in this group
    Object    _value;
    
    // The parent group for this group
    RMGroup   _parent;
    
    // Whether group represents a single, non-group child object
    boolean   _isLeaf;
    
    // Whether group is a Top N Other aggregated group
    boolean   _isTopNOthers;
    
/**
 * Creates an empty group.
 */
public RMGroup()  { super(1); }

/**
 * Creates a group with the (assumed) list of groups.
 */
public RMGroup(List aList)  { super(aList==null? new ArrayList() : aList); }

/**
 * Returns the key associated with this group.
 */
public String getKey()  { return _key; }

/**
 * Returns the value associated with this group.
 */
public Object getValue()  { return _value; }

/**
 * Returns the parent for this group.
 */
public RMGroup getParent()  { return _parent; }

/**
 * Sets the parent for this group.
 */
public void setParent(RMGroup aGroup)  { getParentCount(); _parent = aGroup; }

/**
 * Returns the number of parents that this group has.
 */
public int getParentCount()  { return _pc>=0? _pc : (_pc=pcimpl()); }
private final int pcimpl()  { return _parent!=null? _parent.getParentCount() + 1 : 0; }
int _pc = -1; // Hack to cache ParentCount prior to resetting Parent

/**
 * Returns whether given group is an ancestor group.
 */
public boolean isAncestor(RMGroup aGroup)
{
    for(RMGroup g=getParent(); g!=null; g=g.getParent()) if(aGroup==g) return true;
    return false;
}

/**
 * Returns whether this group is a leaf group.
 */
public boolean isLeaf()  { return _isLeaf; }

/**
 * Returns whether this group is made up of the remainders from a TopN sort.
 */
public boolean isTopNOthers()  { return _isTopNOthers; }

/**
 * Sets whether this group is made up of the remainders from a TopN sort.
 */
public void setTopNOthers(boolean aFlag)  { _isTopNOthers = aFlag; }

/**
 * Returns the index of this group in its parent.
 */
public int index()  { return ListUtils.indexOfId(_parent, this); }

/**
 * Returns the sub-group at the given index.
 */
public RMGroup getGroup(int anIndex)
{
    // Wouldn't need this if we didn't allow multiple parents for groups (eg., happens with subgroup()).
    RMGroup group = (RMGroup)get(anIndex); if(group._parent!=this) group._parent = this; return group;
}

/**
 * Returns the last sub-group.
 */
public RMGroup getGroupLast()  { return getGroup(size()-1); }

/**
 * Groups this group by the groupings in the given grouper.
 */
public void groupBy(RMGrouper aGrouper, int start)
{
    // Get grouping
    RMGrouping grouping = aGrouper.getGrouping(start);
    
    // If not last grouping, do normal group by grouping and recurse
    if(start+1<aGrouper.getGroupingCount()) {
        groupBy(grouping, getExplicitValues(grouping));  // Do normal group by grouping
        for(int i=0, iMax=size(); i<iMax; i++)           // Recurse into subgroups for successive groupings
            getGroup(i).groupBy(aGrouper, start+1);
    }

    // If last grouping, group by leaf key then to top N and sorting
    else {
        groupByLeafKey(grouping.getKey()); // Group by leaf key (just turns leaf objects into single object groups)
        topNSortBy(grouping.getTopNSort()); // Do top n sort for grouping
        sortBy(grouping); // Do sorts for grouping
    }
}

/**
 * Groups this group by given grouping.
 */
public void groupBy(RMGrouping aGrouping)  { groupBy(aGrouping, getExplicitValues(aGrouping)); }

/**
 * Groups this group by given grouping.
 */
public void groupBy(RMGrouping aGrouping, List aValuesList)
{
    groupByKey(aGrouping.getKey(), aValuesList); // Group by grouping key
    topNSortBy(aGrouping.getTopNSort());  // Do top n sort for grouping
    sortBy(aGrouping);   // Do sorts for grouping
}

/**
 * Groups a new group by given keys list.
 */
public void groupByKey(String aKey, List aValuesList)
{
    // If key is null, just return
    if(aKey==null) return;
    
    // Set key to given key
    _key = aKey;
    
    // Allocate temporary map to perform grouping
    Map groupMap = new HashMap();
    int start = 0; // Declare variable for start (we'll start after added value groups if provided)
    
    // If values set was provided, add groups for them
    if(aValuesList!=null && aValuesList.size()>0) {
        
        // Get a value sample from group objects for key
        Object valueSample = null;
        for(int i=0, iMax=size(); valueSample==null && i<iMax; i++)
            valueSample = RMKeyChain.getValue(get(i), aKey);
        
        // Get value type from sample
        Property.Type valueType = DataUtils.getPropertyType(valueSample);
        
        // Iterate over set values
        for(int i=0, iMax=aValuesList.size(); i<iMax; i++) { Object value = aValuesList.get(i);
            
            // Convert to valueType
            if(valueType!=null) value = DataUtils.convertValue(value, valueType);
            
            // Get value key (if null, reset to Void.class to get a key)
            Object valueKey = value; if(valueKey==null) valueKey = Void.class;
            
            // Create group, add to group map and add to this group
            RMGroup group = new RMGroup(); group._parent = this; group._value = value;
            groupMap.put(valueKey, group);
            add(i, group);
        }
        
        // Set start after value groups
        start = aValuesList.size();
    }

    // Iterate over this group's children and place each child in a group
    for(int i=start, iMax=size(); i<iMax; i++) { Object object = get(i);
        
        // Get value for key for child object
        Object value = RMKeyChain.getValue(object, aKey);
        
        // Get value key (if null, reset to Void.class to get a key)
        Object valueKey = value; if(valueKey==null) valueKey = Void.class;
        
        // If value key is Map, substitute identityHashCode (since Map.hashCode is recursive and blows up for circ refs)
        if(valueKey instanceof Map)
            valueKey = System.identityHashCode(valueKey);
                
        // Get group for value
        RMGroup group = (RMGroup)groupMap.get(valueKey);
        
        // If there isn't yet a group for value, create one and add it to groupMap
        if(group==null) {
            group = new RMGroup(); group._parent = this; group._value = value; // Create new group
            groupMap.put(valueKey, group); // Add to group map
            set(groupMap.size()-1, group); // Install new group in this group's array
        }

        // Add object to group
        group.add(object);
    }

    // Remove original objects beyond range of newly added groups
    removeRange(groupMap.size(), size());
}

/**
 * Turns all list objects into leaf groups.
 */
public void groupByLeafKey(String aKey)
{
    // Set key to given key
    _key = aKey;
    
    // Iterate over objects - If object isn't group, replace it with group
    for(int i=0, iMax=size(); i<iMax; i++) { Object object = get(i);
        if(!(object instanceof RMGroup)) {
            RMGroup group = new RMGroup(); group._parent = this; group._isLeaf = true;
            group.add(object);
            set(i, group);
        }
    }
}

/**
 * Performs top N sort on group for an individual sort.
 */
public void topNSortBy(RMTopNSort aSort)
{
    // Just return if topN count is less than or equal zero
    if(aSort.getCount()<=0) return;
    
    // Get number of children that will stay with this group
    int stayCount = Math.min(size(), aSort.getCount());
    
    // Get range for others
    int othersStart = stayCount;
    int othersEnd = size();
    int othersLength = othersEnd - othersStart;

    // If aSort is non-null, sort objects
    if(aSort.getKey()!=null && aSort.getKey().length()>0)
        Collections.sort(this, aSort);

    // If IncludeOthers, but there is just one other, return (leave it)
    if(aSort.getIncludeOthers() && othersLength==1) return;

    // If includeOthers, coalesce others into one group
    if(aSort.getIncludeOthers() && othersLength>0) {
        
        // Create group for others and grab attributes from first group in others
        RMGroup group = new RMGroup();
        group._key = getGroup(othersStart).getKey(); group._parent = this; group._isTopNOthers = true;

        // Iterate over others list and grab objects from other groups
        for(int i=othersStart; i<othersEnd; i++) { RMGroup other = getGroup(i);
            if(other.isLeaf()) group.add(other); // If is leaf, add to group
            else group.addAll(other); // If not leaf, add all it's children
        }
        
        // Remove others from this group and add combined others group to this group
        removeRange(othersStart, othersEnd);
        add(group);
    }
    
    // Otherwise if padding and we're short, add objects
    else if(aSort.getPad() && size()<aSort.getCount()) {
        while(size()<aSort.getCount()) // Add objects for the missing
            add(getGroupLast().cloneEmpty());
    }

    // If not include others, remove others
    else removeRange(othersStart, othersEnd);
}

/**
 * Sorts by sorts in given grouping.
 */
public void sortBy(RMGrouping aGrouping)
{
    // Get grouping sorts list
    List sorts = aGrouping.getSorts();
    
    // If grouping sorts by values, insert value sort
    if(aGrouping.getSortOnValues()) {
        List values = aGrouping.getValues(); // Get values list
        if(values.size()>0) { // If values, add to sorts
            RMValueSort sort = new RMValueSort(aGrouping.getKey(), values); // Create new value sort
            sorts = sorts==null? new ArrayList() : new ArrayList(sorts); // Create or copy list
            sorts.add(0, sort); // Add new value sort
        }
    }
    
    // Do norm sortBy list
    sortBy(sorts);
}

/**
 * Sorts the group by the sorts in the given list.
 */
public void sortBy(List aSortList)
{
    // Just return if size is less than 2 or sort list is empty
    if(size()<2 || aSortList==null || aSortList.size()==0) return;
    
    // Get last group
    RMGroup lastGroup = getGroupLast();
    
    // If last group is topNOthers, remove from group to exclude from sort
    if(lastGroup.isTopNOthers())
        remove(size()-1);
    
    // Do sort
    RMSort.sort(this, aSortList);
    
    // If lastGroup is topNOthers, add back to group
    if(lastGroup.isTopNOthers())
        add(lastGroup);
}

/**
 * Returns all values for a given grouping in this group hierarchy.
 */
public List getAllValues(String aKey)  { return getAllValues(aKey, new HashSet(), new ArrayList()); }

/**
 * Returns all distinct values for given key in this group hierarchy, returned in both set and list.
 */
private List getAllValues(String aKey, Set aSet, List aList)
{
    // Iterate over children
    for(int i=0, iMax=size(); i<iMax; i++) { Object child = get(i);
        
        // If child is group, forward on
        if(child instanceof RMGroup && ((RMGroup)child).size()>0)
            ((RMGroup)child).getAllValues(aKey, aSet, aList);

        // If child isn't group, get value and check
        else {
            Object value = RMKeyChain.getValue(child, aKey); // Get value for key
            if(!aSet.contains(value)) {  // If value not in set, add it
                aSet.add(value); aList.add(value); }
        }
    }
    
    // Return list
    return aList;
}

/**
 * Returns all values explicit for a given grouping in this group hierarchy (from Values string and includeAllValues).
 */
private List getExplicitValues(RMGrouping aGrouping)
{
    // Get grouping values
    List valuesList = null;
    
    // If grouping includes explicitly defined values, get values list
    if(aGrouping.getIncludeValues())
        valuesList = aGrouping.getValues();
    
    // If include all values, get all values
    if(aGrouping.getIncludeAllValues()) {
        
        // Get top parent (by iterating up)
        RMGroup topParent = this;
        while(topParent.getParent()!=null)
            topParent = topParent.getParent();
        
        // Create set to store all values
        Set valuesSet = new HashSet();
        
        // If values provided, add them
        if(valuesList!=null) {
            valuesSet.addAll(valuesList);
            valuesList = new ArrayList(valuesList);
        }
        
        // If no values provides, create empty values list
        else valuesList = new ArrayList();
        
        // Get all values from top parent
        topParent.getAllValues(aGrouping.getKey(), valuesSet, valuesList);
    }
    
    // Return values list
    return valuesList;
}

/**
 * Returns a subset of this group from start index, inclusive, to end index, exclusive.
 */
public RMGroup subgroup(int start, int end)
{
    // Create group from sublist, set attributes from this group and return
    RMGroup group = new RMGroup(subList(start, end));
    group._key = _key; group._parent = _parent; group._isTopNOthers = _isTopNOthers;
    return group;
}

/**
 * Returns a subset of this group, running deep, which only includes the subset of the tree starting at startGroup
 * and ending at (but not including) endGroup.
 */
public RMGroup subgroup(RMGroup startGroup, RMGroup endGroup)
{
    // If this group is leaf, just return this group
    if(isLeaf()) return this;
    
    // Get start index by iterating up startGroup's parent chain
    int start = -1;
    for(RMGroup g=startGroup; g!=null && start<0; g=g.getParent())
        if(g.getParent()==this)
            start = g.index();
    
    // Get end index by iterating up endGroup's parent chain
    int end = -1;
    if(endGroup!=null && endGroup.getParent()==this)
        end = endGroup.index();
    else for(RMGroup g=endGroup; g!=null && end<0; g=g.getParent())
        if(g.getParent()==this)
            end = g.index() + 1;
    
    // If startGroup/endGroup weren't children, just return this
    if(start==-1 && end==-1) return this;

    // Fix start/end and get subgroup
    if(start<0) start = 0;
    if(end<0) end = size();
    
    // Get subgroup
    RMGroup sg = subgroup(start, end);
    
    // Replace subgroup's first group with recursive subgroup call
    if(sg.size()>0)
        sg.set(0, ((RMGroup)sg.get(0)).subgroup(startGroup, endGroup));
    
    // If subgroup's end group differs from first replace it with recursive subgroup call
    if(sg.size()>1)
        sg.set(sg.size()-1, ((RMGroup)sg.get(sg.size()-1)).subgroup(startGroup, endGroup));
        
    // Return new subgroup
    return sg;
}

/**
 * Returns the key for the given List if it is an RMGroup instance.
 */
public static String getKey(List aList)
{
    if(!(aList instanceof RMGroup)) return null; // If list isn't group, just return null
    return ((RMGroup)aList).getKey(); // Return group key
}

/**
 * Returns whether the given List is an RMGroup instance that also isLeaf.
 */
public static boolean isLeaf(Object aList)  { return aList instanceof RMGroup && ((RMGroup)aList).isLeaf(); }

/**
 * Returns whether the given List is an RMGroup instance that also top n others.
 */
public static boolean isTopNOthers(Object aList) { return aList instanceof RMGroup && ((RMGroup)aList).isTopNOthers(); }

/**
 * Custom implementation of valueForKey to handle group heritage keys.
 */
public Object getKeyValue(String aKey)
{
    // If aKey is "Up", return Parent
    if(aKey.equals("Up"))
        return _parent;

    // If aKey is "Running.", return subarray of Parent, up to and including 'this'
    if(aKey.equals("Running")) { int index = index() + 1;
        return _parent!=null && index<=_parent.size()? _parent.subgroup(0, index) : null; }

    // If aKey is "Remaining.", return subarray of Parent, from just beyond 'this' to end
    if(aKey.equals("Remaining"))
        return _parent!=null? _parent.subgroup(index() + 1, _parent.size()) : null;

    // If aKey is "Row", return Number of index() in Parent
    if(aKey.equals("Row"))
        return index() + 1;

    // If aKey is "Parent", see if any ancestor is a leaf from another grouping (like in a TableGroup) and return it
    if(aKey.equals("Parent")) {
        for(RMGroup parent=getParent(); parent!=null; parent=parent.getParent())
            if(parent.isLeaf())
                return parent;
        return getParent(); // Return parent since no master was found
    }
    
    // Page key should reference special group constrained to page start/end
    if(aKey.equals("Page")) return this;
    
    // Get first child sample (the first real, non-group child)
    Object sample = getFirstSample();
    
    // If no children, iterate up parents and see if key matches any parent key (return child value if match)
    if(sample==null) {
        for(RMGroup child=this, parent=getParent(); parent!=null; child=parent, parent=child.getParent())
            if(aKey.equals(parent.getKey()))
                return child.getValue();
        return null; // Return null since key didn't match any parent keys
    }
    
    // Return value for key on sample child
    return RMKey.getValue(sample, aKey);
}

/**
 * Custom implementation of valueForKeyChain to handle top N groups.
 */
public Object getKeyChainValue(Object aRoot, RMKeyChain aKeyChain)
{
    // If this is TopNOthers, we really want to turn keys for Numbers into aggregates (everything else to "Others")
    if(_isTopNOthers) { // && RMKeyChainAggr.getAggrMethod(aKC.getChildString(0))==null) {
        Object val = RMKeyChain.getValueImpl(aRoot, this, aKeyChain); String kstr = aKeyChain.toString();
        if(val instanceof Number && !kstr.equals("Row")) // If evaluates to Number, return total of keyChain
            return RMKeyChainAggr.total(this, aKeyChain);
        else if(val instanceof String) // If evaluates to String, return "Others"
            return "Others";
        return val; // Otherwise, return value
    }
    
    // If isLeaf and KeyChain.Op is FunctionCall, evaluate keyChain on first object
    if(isLeaf() && aKeyChain.getOp()==RMKeyChain.Op.FunctionCall)
        return RMKeyChain.getValue(aRoot, get(0), aKeyChain);

    // Otherwise, do normal version
    return RMKeyChain.getValueImpl(aRoot, this, aKeyChain);
}

/**
 * Returns the first child sample for this group. In other words, the first real, non-group child.
 */
private Object getFirstSample()
{
    // Iterate over children - If child is a group, check for its first leaf group
    for(int i=0, iMax=size(); i<iMax; i++) { Object child = get(i);
        if(child instanceof RMGroup) { RMGroup childGroup = (RMGroup)child;
            if(childGroup.size()==0) continue; // If group is empty, just continue
            Object sample = childGroup.getFirstSample(); // Get child's first sample and return if non-null
            if(sample!=null) return sample;
        }
        else return child; // If child isn't group, return it
    }
    return null; // Return null since there are no non-empty children
}

/**
 * Returns the next group which is a peer to this one.
 */
public RMGroup getNextPeer()
{
    if(_parent==null) return null; // If parent is null, just return null
    int index = index(); // Get index and return group at next index (or if at end, parent's next peer)
    return index+1<_parent.size()? _parent.getGroup(index+1) : _parent.getNextPeer();
}

/**
 * Standard equals. Just check identity because leaf objects might be interconnected (and can stack overflow).
 */
public boolean equals(Object anObj)  { return anObj==this; }

/**
 * Standard clone implementation.
 */
public RMGroup clone()
{
    //return (RMGroup)super.clone(); TeaVM doesn't like this
    RMGroup copy = new RMGroup((List)this);
    copy._key = _key; copy._value = _value; copy._parent = _parent;
    copy._isLeaf = _isLeaf; copy._isTopNOthers = _isTopNOthers;
    return copy;
}

/**
 * Clone deep implementation - clones this group and any child groups.
 */
public RMGroup cloneDeep()
{
    // Get normal clone (just return if leaf), and clone children
    RMGroup clone = clone(); if(isLeaf()) return clone;
    for(int i=0, iMax=size(); i<iMax; i++) {
        RMGroup grp = getGroup(i).cloneDeep(); grp._parent = clone;
        clone.set(i, grp); }
    return clone; // Return clone
}

/**
 * Clone deep implementation - clones this group and any child groups.
 */
public RMGroup cloneEmpty()
{
    // Get normal clone and clear
    RMGroup clone = clone(); clone._value = null; clone.clear();
    
    // Iterate over children and clone
    for(Object obj : this) if(obj instanceof RMGroup) clone.add(((RMGroup)obj).cloneEmpty());
    
    // Return clone
    return clone;
}

/**
 * Returns a string representation of this group.
 */
public String toString()
{
    StringBuffer sb = new StringBuffer("RMGroup key=").append(getKey()).append(", size=").append(size());
    RMGroup parent = getParent(); String pky = parent!=null? parent.getKey() : null;
    if(pky!=null) { sb.append(", Parent.Key=").append(pky).append(", Value=").append(RMKeyChain.getValue(this, pky)); }
    int index = index(); if(index>=0) sb.append(", Index=").append(index);
    if(isLeaf()) sb.append(", Leaf=true");
    return sb.toString();
}

/**
 * This RMGroup subclass/inner-class represents a subset of a group up to the given endGroup. The only difference
 * between this and a normal subgroup is (a) it ignores the startGroup and (b) the "Up" key points back to the original
 * group instead of the parent and (c) it supports a "Page" key which returns a subgroup that does take the startGroup
 * into account.
 */
public static class Running extends RMGroup {
    
    // Original group that we are a subset of
    RMGroup  _sourceGroup;
    
    // The page start/end groups
    RMGroup  _pageStartGroup, _pageEndGroup;
    
    /** Creates a Running group for parent group and start/end groups on page. */
    public Running(RMGroup aSourceGroup, RMGroup thePageStartGroup, RMGroup thePageEndGroup)
    {
        // Store original group and start/end groups
        _sourceGroup = aSourceGroup; _pageStartGroup = thePageStartGroup; _pageEndGroup = thePageEndGroup;
        
        // Add all children from group start to page end
        RMGroup subgroup = _sourceGroup.subgroup(null, thePageEndGroup);
        addAll(subgroup);
        
        // Get other group attributes from subgroup
        _key = subgroup.getKey();
        _parent = subgroup.getParent();
        _isTopNOthers = subgroup.isTopNOthers();
    }
    
    /** Overrides default valueForKey to handle Page and Running keys. */
    public Object getKeyValue(String aKey)
    {
        // Up key should reference source group
        if(aKey.equals("Up"))
            return _sourceGroup;
        
        // Page key should reference special group constrained to page start/end
        if(aKey.equals("Page"))
            return _sourceGroup.subgroup(_pageStartGroup, _pageEndGroup);
        
        // Running key should reference special group from beginning to page end
        if(aKey.equals("Running") && _sourceGroup.getParent()!=null)
            return _sourceGroup.getParent().subgroup(null, _pageEndGroup);
        
        // Every thing else should be handled normally
        return super.getKeyValue(aKey);
    }
}

}