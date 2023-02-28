package com.reportmill.graphics;
import java.lang.ref.WeakReference;
import java.util.*;
import snap.gfx.Image;
import snap.util.*;
import snap.web.WebURL;

/**
 * A class provides a unique reference to an image for a given source (URL, bytes).
 */
public class ImageRef {

    // The object that provided image bytes
    private Object  _source;

    // The image
    private Image  _image;

    // The original file bytes
    private byte[]  _bytes;

    // The time the source was last modified (in milliseconds since 1970)
    private long  _modTime;

    // The cache used to hold application instances
    private static List<WeakReference<ImageRef>>  _cache = new ArrayList<>();

    /**
     * Creates an ImageRef for aSource.
     */
    private ImageRef(Object aSource)
    {
        setSource(aSource);
        _cache.add(new WeakReference<>(this));
    }

    /**
     * Returns the image name.
     */
    public String getName()
    {
        Image image = getImage();
        String imageName = image.getName();
        return imageName != null ? imageName : String.valueOf(System.identityHashCode(image));
    }

    /**
     * Returns the original source for the image (byte[], File, InputStream or whatever).
     */
    public Object getSource()  { return _source; }

    /**
     * Sets the source (either a WebURL or bytes).
     */
    protected void setSource(Object aSource)
    {
        // Get URL, source, modified time
        WebURL url = WebURL.getURL(aSource);
        _source = url != null ? url : aSource;
        _modTime = url != null ? url.getLastModTime() : System.currentTimeMillis();

        // Otherwise, assume source can provide bytes
        _bytes = url != null ? url.getBytes() : SnapUtils.getBytes(aSource);
        _image = null;
    }

    /**
     * Returns the source URL, if loaded from URL.
     */
    public WebURL getSourceURL()
    {
        return _source instanceof WebURL ? (WebURL) _source : null;
    }

    /**
     * Returns the image.
     */
    public Image getImage()
    {
        if (_image != null) return _image;
        Object source = getSource();
        return _image = Image.get(source);
    }

    /**
     * Returns the original bytes for the image (loaded from the source).
     */
    public byte[] getBytes()
    {
        // If already set, just return
        if (_bytes != null) return _bytes;

        // If image set, get bytes from image
        if (_image != null)
            return _image.getBytes();

        // If Source URL available get from it
        WebURL url = getSourceURL();
        if (url != null)
            return _bytes = url.getBytes();

        // Otherwise try from source
        return _bytes = SnapUtils.getBytes(_source);
    }

    /**
     * Refreshes data from source.
     */
    protected void refresh()
    {
        WebURL url = getSourceURL();
        if (url == null)
            return;
        long modTime = url.getLastModTime();
        if (modTime > _modTime) {
            setSource(url);
            System.out.println("Refreshed ImageRef Image");
        }
    }

    /**
     * Returns a unique ImageRef for aSource.
     */
    public static synchronized ImageRef getImageRef(Object aSource)
    {
        // If source is null, return null, if ImageRef, return it
        if (aSource == null)
            return null;
        if (aSource instanceof ImageRef)
            return (ImageRef) aSource;

        // Handle Image
        if (aSource instanceof Image) {
            Image image = (Image) aSource;
            Object imageSource = image.getSource();
            ImageRef imageRef = imageSource != null ? getImageRef(imageSource) : new ImageRef(null);
            if (imageRef._image == null)
                imageRef._image = image;
            return imageRef;
        }

        // Get url for source - if found, return ImageRef for URL
        WebURL url = WebURL.getURL(aSource);
        if (url != null)
            return getImageRef(url);

        // Get bytes for source - if found, return Image for bytes
        byte[] bytes = SnapUtils.getBytes(aSource);
        if (bytes != null)
            return getImageRef(bytes);

        // Complain and return null
        System.err.println("ImageRef: Can't load image for source: " + aSource);
        return null;
    }

    /**
     * Returns a unique ImageRef for URL.
     */
    private static ImageRef getImageRef(WebURL aURL)
    {
        // Iterate over ImageRefs and see if any match URL
        for (int i = _cache.size() - 1; i >= 0; i--) {
            ImageRef imageRef = _cache.get(i).get();

            // If null, remove weak reference and continue)
            if (imageRef == null) {
                _cache.remove(i);
                continue;
            }

            // If URL matches ImageRef URL, return
            if (aURL.equals(imageRef.getSourceURL())) {
                imageRef.refresh();
                return imageRef;
            }
        }

        // Create new ImageRef and return
        return new ImageRef(aURL);
    }

    /**
     * Returns a unique ImageRef for bytes.
     */
    private static ImageRef getImageRef(byte[] theBytes)
    {
        // Iterate over ImageRefs and see if any match bytes
        for (int i = _cache.size() - 1; i >= 0; i--) {
            ImageRef imageRef = _cache.get(i).get();

            // If null, remove weak reference and continue)
            if (imageRef == null) {
                _cache.remove(i);
                continue;
            }

            // If bytes match ImageRef bytes, return
            if (ArrayUtils.equals(theBytes, imageRef.getBytes()))
                return imageRef;
        }

        // Create new ImageRef and return
        return new ImageRef(theBytes);
    }
}