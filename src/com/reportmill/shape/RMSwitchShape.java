/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMKeyChain;
import java.util.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents a sophisticated shape that can maintain multiple versions of itself. For example, a shape can
 * have a "Standard" version and an "Alternate" version (for alternating TableRows).
 */
public class RMSwitchShape extends RMParentShape {
    
    // Current version name
    String          _version = getDefaultVersionName();
    
    // Map of alternate versions
    Map             _alternates;

    // An optional key to specify what version should be used in RPG clone
    String          _versionKey;

/**
 * Returns the currently selected version string for the switch shape.
 */
public String getVersion()  { return _version; }

/**
 * Sets the currently selected version of the switch shape to given version name. If the specified version name
 * doesn't yet exist, this method creates a new version for it.
 */
public void setVersion(String aName)
{
    // If value already set, just return
    if(aName.equals(getVersion())) return;
    
    // Disable Undo registration
    undoerDisable();

    // Get place-holder "outgoingShape" (create it if it doesn't exist)
    RMSwitchShape outShape = getAlternates(true).get(_version);
    if(outShape==null) {
        outShape = clone(); outShape._version = _version;
        _alternates.put(_version, outShape);
    }

    // Transfer attributes from this shape to outgoing shape
    transferAttributes(outShape);
    
    // Get place-holder "incomingShape" (if needed, make one by copying defaultVersion)
    RMSwitchShape inShape = getAlternates().get(aName);
    if(inShape == null) {
        RMSwitchShape defaultVersion = getAlternates().get(getDefaultVersionName());
        inShape = defaultVersion!=null? defaultVersion.cloneDeep() : clone();
        inShape._alternates = null; inShape._version = aName;
    }
    
    // Transfer attributes from incoming shape to this shape
    String url = getURL();
    inShape.transferAttributes(this);
    setURL(url);
    
    // Enable Undo registration
    undoerEnable();

    // Set value and fire property change
    firePropChange("Version", _version, _version = aName);
}

/**
 * This method is used by setVersion to transfers attributes to/from alternate versions.
 */
protected void transferAttributes(RMSwitchShape toShape)
{
    // Get the origin of toShape
    Point origin = toShape.getXYP();
    
    // Have toShape copy normal shape attributes
    toShape.copyShape(this);
    
    // Install children from this shape into toShape
    for(RMShape child : getChildArray())
        toShape.addChild(child);
    
    // Reset origin
    toShape.setXYP(origin.getX(), origin.getY());
}

/**
 * Returns the switch shape associated with the given version name.
 */
public RMSwitchShape getVersion(String aName)
{
    // If receiver is _version, just return this
    if(aName.equals(_version))
        return this;
    
    // If alternates are null, return null
    if(getAlternates()==null)
        return null;
        
    // Get shapeForVersion from alternates and return
    RMSwitchShape shape = getAlternates().get(aName);
    if(shape==null)
        return null;
    
    // Reset name? because this was screwed up (prior to 11/20/01?)
    shape._version = aName;
    
    // Return switch shape
    return shape;
}

/**
 * Removes the version shape associated with the given name from the alternates map.
 */
public void removeVersion(String aString)
{
    // Set version to default version if version to be removed is active
    if(_version.equals(aString))
        setVersion(getDefaultVersionName());
    
    // Remove version from alternates map
    _alternates.remove(aString);
}

/**
 * Returns the map used to hold alternate versions.
 */
public Map <String, RMSwitchShape> getAlternates()  { return _alternates; }

/**
 * Returns the map used to hold alternate versions.
 */
public Map <String, RMSwitchShape> getAlternates(boolean doCreate)
{
    if(_alternates==null && doCreate)
        _alternates = new Hashtable();
    return _alternates;
}

/**
 * Returns whether switch shape has a version with the given name.
 */
public boolean hasVersion(String aString)  { return getVersion(aString)!=null; }

/**
 * Returns a sorted list of all version names contained in the switch shape.
 */
public List <String> getVersionNames()
{
    List versions = _alternates==null? new ArrayList() : new ArrayList(_alternates.keySet()); // Get version names
    if(!versions.contains(_version)) versions.add(_version); // If current version name missing, add it
    Collections.sort(versions); // Sort
    return versions;
}

/**
 * Returns the default version name.
 */
public String getDefaultVersionName()  { return "Standard"; }

/**
 * Returns the version key used to get the version used in a report.
 */
public String getVersionKey()  { return _versionKey; }

/**
 * Sets the version key used to get the version used in a report.
 */
public void setVersionKey(String aVersionKey)  { _versionKey = aVersionKey; }

/**
 * Overrides standard shape method to resize alternates.
 */
public void setWidth(double aWidth)
{
    // Do normal shape set width deep
    super.setWidth(aWidth);
    
    // If alternates, set width deep on alternate shapes
    if(getAlternates()!=null)
        for(RMShape alt : getAlternates().values())
            if(alt!=this)
                alt.setWidth(aWidth);
}

/**
 * Paints switch shape.
 */
protected void paintShape(Painter aPntr)
{
    // If switch shape doesn't draw a stroke, draw a light one to indicate its bounds
    if(getClass()==RMSwitchShape.class && getStroke()==null && RMShapePaintProps.isEditing(aPntr)) {
        aPntr.setColor(Color.LIGHTGRAY); aPntr.setStroke(Stroke.Stroke1);
        aPntr.draw(getBoundsInside());
    }
    
    // Paint shape normally
    super.paintShape(aPntr);
}

/**
 * Report generation.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    // Get version
    String version = getDefaultVersionName();
    
    // If version key is provided, try to evaluate for a version string
    if(getAlternates()!=null && getVersionKey()!=null) {
        String vkey = RMKeyChain.getStringValue(anRptOwner, getVersionKey());
        if(vkey!=null && hasVersion(vkey))
            version = vkey;
    }
    
    // Forward to alternate, if appropriate
    RMSwitchShape alternate = getVersion(version);
    return alternate.rpgAllSuper(anRptOwner, aParent);
}

/**
 * Report generation.
 */
protected RMShape rpgAllSuper(ReportOwner anRptOwner, RMShape aParent)  { return super.rpgAll(anRptOwner, aParent); }

/**
 * Standard clone implementation.
 */
public RMSwitchShape clone()
{
    RMSwitchShape clone = (RMSwitchShape)super.clone(); // Get normal shape clone
    clone._alternates = null; // Clone alternates
    return clone; // Return clone
}

/**
 * Standard clone deep implementation.
 */
public RMSwitchShape cloneDeep()
{
    // Get normal shape clone (just return if no alternates)
    RMSwitchShape clone = (RMSwitchShape)super.cloneDeep(); if(getAlternates()==null) return clone;
        
    // Iterate over alternates and place in clone
    for(String key : getAlternates().keySet()) {
        RMSwitchShape alternate = getAlternates().get(key);
        clone.getAlternates(true).put(key, alternate.cloneDeep());
    }
    
    // Set clone version to default and return
    clone.setVersion(getDefaultVersionName());
    return clone;
}

/** Editor method - specifies that switch shape is super selectable. */
public boolean superSelectable()  { return true; }

/** Editor method - specifies that switch shape accepts children. */
public boolean acceptsChildren()  { return true; }

/**
 * XML Archival generic - break toXML into toXMLShape and toXMLShapeChildren.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Cache current version and make sure that switch shape is set to default version
    String version = getAlternates()!=null && !getVersion().equals(getDefaultVersionName())? getVersion() : null;
    if(version!=null)
        setVersion(getDefaultVersionName());
    
    // Do normal version
    XMLElement xml = super.toXML(anArchiver);
    
    // Reset version to pre-archival state
    if(version!=null)
        setVersion(version);
    
    // Return xml
    return xml;
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXMLShape(anArchiver); e.setName("switchshape");

    // Archive VersionKey
    if(_versionKey!=null && _versionKey.length()>0) e.add("version-key", _versionKey);
    return e; // Return xml element
}

/**
 * XML child archival - override to archive alternates.
 */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
{
    // Do normal child archival (just return if no alternates)
    super.toXMLChildren(anArchiver, anElement); if(getAlternates()==null) return;
    
    // Create alternates xml element and add to parent element
    XMLElement alternatesXML = new XMLElement("alternates");
    anElement.add(alternatesXML);
    
    // Iterate over alternate versions and add xml
    for(String key : getAlternates().keySet()) {
        
        // If version key is default, just continue
        if(key.equals(getDefaultVersionName())) continue;
        
        // Get alternate shape, generate xml and add to alts xml
        RMShape shape = getAlternates().get(key);
        XMLElement alternateXML = shape.toXML(anArchiver);
        alternateXML.add("alt-key", key);
        alternatesXML.add(alternateXML);
    }
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);

    // Unarchive VersionKey
    setVersionKey(anElement.getAttributeValue("version-key"));
    
    // Unarchive alternates
    XMLElement alternatesXML = anElement.get("alternates");
    if(alternatesXML!=null) {
        for(int i=0, iMax=alternatesXML.size(); i<iMax; i++) {
            XMLElement altElm = alternatesXML.get(i);
            String altKey = altElm.getAttributeValue("alt-key");
            RMSwitchShape shape = (RMSwitchShape)anArchiver.fromXML(altElm, this);
            shape._parent = null; // Bogus, this can go when shape unarchival stops setting this
            getAlternates(true).put(altKey, shape);
        }
    }
}

}