/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.jar.*;
import java.util.zip.GZIPInputStream;
import javax.swing.*;

/**
 * This app 
 */
public class AppLoader {

    // Constants
    static final String AppDirName = "ReportMill";
    static final String JarName = "RMStudio15.jar";
    static final String JarURL = "http://reportmill.com/rm15/RMStudio15.jar.pack.gz";
    static final String LoaderJarName = "AppLoader.jar"; // 
    static final String MainClass = "com.reportmill.app.App";

/**
 * Main method - reinvokes main1() on app event thread in exception handler.
 */
public static void main(final String args[])
{
    // Re-invoke on Swing thread
    if(!SwingUtilities.isEventDispatchThread()) {
        SwingUtilities.invokeLater(() -> main(args)); return; }
    
    // Invoke real main with exception handler
    try { main1(args); }
    catch(Throwable e) { JOptionPane.showMessageDialog(null, e.toString()); e.printStackTrace(); }
}

/**
 * Main method:
 *     - Gets main Jar file from default, if missing
 *     - Updates main Jar file from local update file, if previously loaded
 *     - Load main Jar into URLClassLoader, load main class and invoke main method
 *     - Check for update from remove site in background
 */
public static void main1(final String args[]) throws Exception
{
    // Make sure default jar is in place
    try { copyDefaultMainJar(); }
    catch(Exception e) { JOptionPane.showMessageDialog(null, e.toString()); e.printStackTrace(); }
    
    // If Update Jar exists, copy it into place
    File jar = getAppFile(JarName);
    File updateJar = getAppFile(JarName + ".update");
    if(updateJar.exists()) {
        copyFile(updateJar, jar);
        jar.setLastModified(updateJar.lastModified());
        updateJar.delete();
    }
    
    // If jar doesn't exist complain bitterly
    if(!jar.exists() || !jar.canRead())
        throw new RuntimeException("Main Jar not found!");
    
    // Check for updates in background thread
    new Thread(() -> checkForUpdatesSilent()).start();
    
    // Create URLClassLoader for main jar file, get App class and invoke main
    URLClassLoader ucl = new URLClassLoader(new URL[] { jar.toURI().toURL() });
    Class cls = ucl.loadClass(MainClass); //ucl.close();
    Method meth = cls.getMethod("main", new Class[] { String[].class });
    meth.invoke(null, new Object[] { args });
    if(cls==Object.class) ucl.close(); // Getting rid of warning message for ucl
}

/**
 * Copies the default main jar into place.
 */
private static void copyDefaultMainJar() throws IOException, ParseException
{
    // Get date from app package
    URL url = AppLoader.class.getProtectionDomain().getCodeSource().getLocation();
    String path0 = url.getPath(); path0 = URLDecoder.decode(path0, "UTF-8");
    String path2 = path0.replace(LoaderJarName, "BuildInfo.txt");
    BufferedReader br = new BufferedReader(new FileReader(path2));
    String text = br.readLine();
    br.close(); if(text==null || text.length()<0) return;
    SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.US);
    Date date = formatter.parse(text); long time = date.getTime();

    // Get main jar from app package
    String path1 = path0.replace(LoaderJarName, JarName);
    File jar0 = getAppFile(JarName);
    File jar1 = new File(path1);
    
    // If app package main jar is newer, copy it into place and set time
    if(jar0.exists() && jar0.lastModified()>=time) return;
    copyFile(jar1, jar0);
    jar0.setLastModified(time);
}

/**
 * Check for updates.
 */
private static void checkForUpdatesSilent()
{
    try { checkForUpdates(); }
    catch(Exception e) { e.printStackTrace(); }
}

/**
 * Check for updates.
 */
private static void checkForUpdates() throws IOException, MalformedURLException
{
    // Get URL connection and lastModified time
    File jarFile = getAppFile(JarName);
    URL url = new URL(JarURL);
    URLConnection connection = url.openConnection(); connection.setUseCaches(false);
    long mod0 = jarFile.lastModified(), mod1 = connection.getLastModified();
    if(mod0>=mod1) { System.out.println("No update available at " + JarURL + '(' + mod0 + '>' + mod1 + ')'); return; }
    
    // Get update file and write to JarName.update
    System.out.println("Loading update from " + JarURL);
    byte bytes[] = getBytes(connection); System.out.println("Update loaded");
    File updatePacked = getAppFile(JarName + ".pack.gz"), updateFile = getAppFile(JarName + ".update");
    writeBytes(updatePacked, bytes); System.out.println("Update saved: " + updatePacked);
    unpack(updatePacked, updateFile); System.out.println("Update unpacked: " + updateFile);
    updateFile.setLastModified(mod1);
    updatePacked.delete();
    
    // Let the user know
    String msg = "A new update is available. Restart application to apply";
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
}

/**
 * Returns the Main jar file.
 */
private static File getAppFile(String aName)  { return new File(getAppDir(), aName); }

/**
 * Returns the Main jar file.
 */
private static File getAppDir()  { return getAppDataDir(AppDirName, true); }


/**
 * 
 *  Utility Methods for AppLoader. 
 * 
 * 
 * 
 * 
 */


/**
 * Copies a file from one location to another.
 */
public static File copyFile(File aSource, File aDest) throws IOException
{
    // Get input stream, output file and output stream
    FileInputStream fis = new FileInputStream(aSource);
    File out = aDest.isDirectory()? new File(aDest, aSource.getName()) : aDest;
    FileOutputStream fos = new FileOutputStream(out);
    
    // Iterate over read/write until all bytes written
    byte[] buf = new byte[8192];
    for(int i=fis.read(buf); i!=-1; i=fis.read(buf))
        fos.write(buf, 0, i);
    
    // Close in/out streams and return out file
    fis.close();
    fos.close();
    return out;
}

/**
 * Writes the given bytes (within the specified range) to the given file.
 */
public static void writeBytes(File aFile, byte theBytes[]) throws IOException
{
    if(theBytes==null) { aFile.delete(); return; }
    FileOutputStream fileStream = new FileOutputStream(aFile);
    fileStream.write(theBytes);
    fileStream.close();
}

/**
 * Unpacks the given file into the destination file.
 */
public static File unpack(File aFile, File aDestFile) throws IOException
{
    // Get dest file
    File destFile = getUnpackDestination(aFile, aDestFile);
    
    // If already unpacked, return
    if(destFile.exists() && destFile.lastModified()>aFile.lastModified())
        return destFile;
    
    // Create new Gzip input stream
    FileInputStream fileInput = new FileInputStream(aFile);
    GZIPInputStream gzipInput = new GZIPInputStream(fileInput);
        
    // Create new FileOutput stream JarOutputStream
    FileOutputStream fileOut = new FileOutputStream(destFile);
    JarOutputStream jarOut = new JarOutputStream(fileOut);
    
    // Unpack file
    Pack200.newUnpacker().unpack(gzipInput, jarOut);
    
    // Close streams
    fileInput.close();
    gzipInput.close();
    jarOut.close();
    fileOut.close();
    
    // Return destination file
    return destFile;
}

/**
 * Returns the file that given packed file would be saved to using the unpack method.
 */
public static File getUnpackDestination(File aFile, File aDestFile)
{
    // Get dest file
    File destFile = aDestFile;

    // If dest file is null, create from packed file minus .pack.gz
    if(destFile==null)
        destFile = new File(aFile.getPath().replace(".pack.gz", ""));

    // If dest file is directory, change to file inside with packed file minus .pack.gz
    else if(destFile.isDirectory())
        destFile = new File(destFile, aFile.getName().replace(".pack.gz", ""));

    // Return destination file
    return destFile;
}

/**
 * Returns the AppData or Application Support directory file.
 */
public static File getAppDataDir(String aName, boolean doCreate)
{
    // Get user home + AppDataDir (platform specific) + name (if provided)
    String dir = System.getProperty("user.home");
    if(isWindows) dir += File.separator + "AppData" + File.separator + "Local";
    else if(isMac) dir += File.separator + "Library" + File.separator + "Application Support";
    if(aName!=null) dir += File.separator + aName;

    // Create file, actual directory (if requested) and return
    File dfile = new File(dir);
    if(doCreate && aName!=null)
        dfile.mkdirs();
    return dfile;
}

/**
 * Returns bytes for connection.
 */
public static byte[] getBytes(URLConnection aConnection) throws IOException
{
    InputStream stream = aConnection.getInputStream(); // Get stream for connection
    byte bytes[] = getBytes(stream); // Get bytes for stream
    stream.close();  // Close stream
    return bytes;  // Return bytes
}

/**
 * Returns bytes for an input stream.
 */
public static byte[] getBytes(InputStream aStream) throws IOException
{
    // Read contents of InputStream into ByteArrayOutputStream until EOF, return bytes
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    byte chunk[] = new byte[8192];
    for(int len=aStream.read(chunk, 0, chunk.length); len>0; len=aStream.read(chunk, 0, chunk.length)) {
        bs.write(chunk, 0, len); }
    return bs.toByteArray();
}

// Whether Windows/Mac
static boolean isWindows = (System.getProperty("os.name").indexOf("Windows") >= 0);
static boolean isMac = (System.getProperty("os.name").indexOf("Mac OS X") >= 0);

}