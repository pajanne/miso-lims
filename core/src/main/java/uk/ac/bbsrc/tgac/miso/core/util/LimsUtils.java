/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class to provde helpful functions to MISO
 *
 * @author Rob Davey
 * @since 0.0.2
 */
public class LimsUtils {
  public static final long SYSTEM_USER_ID = 0;

  protected static final Logger log = LoggerFactory.getLogger(LimsUtils.class);

  public static boolean isUrlValid(URL url) {
    try {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("HEAD");
      int responseCode = connection.getResponseCode();
      return (responseCode == 200);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static boolean isUrlValid(URI uri) {
    try {
      URL url = uri.toURL();
      return isUrlValid(url);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Join a collection, akin to Perl's join(), using a given delimiter to produce a single String 
   *
   * @param s of type Collection
   * @param delimiter of type String
   * @return String
   */
  public static String join(Collection s, String delimiter) {
    StringBuffer buffer = new StringBuffer();
    Iterator iter = s.iterator();
    while (iter.hasNext()) {
      buffer.append(iter.next());
      if (iter.hasNext()) {
        buffer.append(delimiter);
      }
    }
    return buffer.toString();
  }

  /**
   * Join an Array, akin to Perl's join(), using a given delimiter to produce a single String
   *
   * @param s of type Object[]
   * @param delimiter of type String
   * @return String
   */
  public static String join(Object[] s, String delimiter) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < s.length; i++) {
      buffer.append(s[i]);
      if (i<s.length-1) {
        buffer.append(delimiter);
      }
    }
    return buffer.toString();
  }

  public static <T> List<List<T>> partition(List<T> list, int size) {

   if (list == null)
      throw new NullPointerException(
          "'list' must not be null");
    if (!(size > 0))
      throw new IllegalArgumentException(
          "'size' must be greater than 0");

    return new Partition<T>(list, size);
  }

  private static class Partition<T> extends AbstractList<List<T>> {

    final List<T> list;
    final int size;

    Partition(List<T> list, int size) {
      this.list = list;
      this.size = size;
    }

    @Override
    public List<T> get(int index) {
      int listSize = size();
      if (listSize < 0)
        throw new IllegalArgumentException("negative size: " + listSize);
      if (index < 0)
        throw new IndexOutOfBoundsException(
            "index " + index + " must not be negative");
      if (index >= listSize)
        throw new IndexOutOfBoundsException(
            "index " + index + " must be less than size " + listSize);
      int start = index * size;
      int end = Math.min(start + size, list.size());
      return list.subList(start, end);
    }

    @Override
    public int size() {
      return (list.size() + size - 1) / size;
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }
  }

  /**
   * Computes the relative complement of two sets, i.e. those elements that are in A but not in B
   *
   * @param needles of type Set
   * @param haystack of type Set
   * @return Set
   */
  public static Set relativeComplement(Set needles, Set haystack) {
    Set diff = (Set)((HashSet)needles).clone();
    diff.removeAll(haystack);
    return diff;
  }

  /**
   * SLOWLY computes the relative complement of two sets, i.e. those elements that are in A but not in B, based on an object's given accessor to a property.
   * <p/>
   * This is distinctly less efficient than {@link uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.relativeComplement()} (this method uses reflection) but
   * can avoid comparing objects by hashcode. This is useful when trying to compare objects that have been persisted, and therefore have unique
   * IDs and Names, to objects that haven't, and hence have no ID or Name.
   * <p/>
   * If an exception occurs, null is returned.
   *
   * @param c of type Class
   * @param needles of type Set
   * @param haystack of type Set
   * @return Set
   */
  public static Set relativeComplementByProperty(Class c, String methodName, Set needles, Set haystack) {
    try {
      Method m = c.getMethod(methodName);
      Set diff = (Set)((HashSet)needles).clone();

      if (diff.size() > haystack.size()) {
        for (Iterator<?> i = haystack.iterator(); i.hasNext(); ) {
          Object h = i.next();
          String hProp = (String)m.invoke(h);
          for (Iterator<?> j = diff.iterator(); j.hasNext(); ) {
            Object n = j.next();
            String nProp = (String)m.invoke(n);
            if (nProp.equals(hProp)) {
              j.remove();
            }
          }
        }
      } else {
        for (Iterator<?> i = diff.iterator(); i.hasNext(); ) {
          Object n = i.next();
          String nProp = (String)m.invoke(n);
          for (Iterator<?> j = haystack.iterator(); j.hasNext(); ) {
            Object h = j.next();
            String hProp = (String)m.invoke(h);
            if (nProp.equals(hProp)) {
              i.remove();
            }
          }
        }
      }
      return diff;
    }
    catch (ConcurrentModificationException e) {
      e.printStackTrace();
      log.error("Backing set modification outside iterator.");
    }
    catch (NoSuchMethodException e) {
      e.printStackTrace();
      log.error("Class " + c.getName() + " doesn't declare a "+methodName+" method.");
    }
    catch (InvocationTargetException e) {
      e.printStackTrace();
      log.error("Cannot invoke "+methodName+" on class " + c.getName());
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
      log.error("Cannot invoke "+methodName+" on class " + c.getName());
    }
    return null;
  }

  public static String findHyperlinks(String text) {
    Pattern p = Pattern.compile("(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");
    Matcher m = p.matcher(text);

    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, "<a href='$0'>$0</a>");
    }
    m.appendTail(sb);
    return sb.toString();
  }

  public static String lookupLocation(String locationBarcode) {
    //TODO - proper lookup!
    /*
    if (locationBarcode is valid) {
      retrieve text representation of location and return
    }
    else {
      return null;
    }
     */
    return locationBarcode;
  }
  
  public static void unzipFile(File source) {
    unzipFile(source, null);
  }

  public static boolean unzipFile(File source, File destination) {
    final int BUFFER = 2048;
    
    try {
      BufferedOutputStream dest = null;
      FileInputStream fis = new FileInputStream(source);
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File outputFile = null;

        if (destination != null && destination.exists() && destination.isDirectory()) {
          outputFile = new File(destination, entry.getName());
        }
        else {
          outputFile = new File(entry.getName());
        }

        if (entry.isDirectory()) {
          System.out.println("Extracting directory: " + entry.getName());
          LimsUtils.checkDirectory(outputFile, true);
        }
        else {
          System.out.println("Extracting file: " + entry.getName());
          int count;
          byte data[] = new byte[BUFFER];
          FileOutputStream fos = new FileOutputStream(outputFile);
          dest = new BufferedOutputStream(fos, BUFFER);
          while ((count = zis.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, count);
          }
          dest.flush();
          dest.close();
        }
      }
      zis.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public static void writeFile(InputStream in, File path) throws IOException {
    OutputStream out = null;
    try {
      out = new FileOutputStream(path);
      try {
        byte[] buf = new byte[16884];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
      }
      catch (IOException e) {
        log.error("Could not write file: " + path.getAbsolutePath());
        e.printStackTrace();
      }
      finally {
        try {
          in.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Checks that a directory exists. This method will attempt to create the directory if it doesn't exist and if the attemptMkdir flag is true  
   *
   * @param path of type File
   * @param attemptMkdir of type boolean
   * @return boolean true if the directory exists/was created, false if not
   * @throws IOException when the directory exist check/creation could not be completed
   */
  public static boolean checkDirectory(File path, boolean attemptMkdir) throws IOException {
    boolean storageOk;

    if (attemptMkdir) {
      storageOk = path.exists() || path.mkdirs();
    }
    else {
      storageOk = path.exists();
    }

    if (!storageOk) {
      StringBuilder sb = new StringBuilder("The directory ["+path.toString()+"] doesn't exist");
      if (attemptMkdir) {
        sb.append(" or is not creatable");
      }
      sb.append(". Please create this directory and ensure that it is writable.");
      throw new IOException(sb.toString()); 
    }
    else {
      if (attemptMkdir) {
        log.info("Directory (" + path + ") exists.");
      }
      else {
        log.info("Directory (" + path + ") OK.");
      }
    }
    return storageOk;
  }

  /**
   * Similar to checkDirectory, but for single files.
   *
   * @param path of type File
   * @return boolean true if the file exists, false if not
   * @throws IOException when the file doesn't exist
   */
  public static boolean checkFile(File path) throws IOException {
    boolean storageOk = path.exists();
    if (!storageOk) {
      StringBuilder sb = new StringBuilder("The file ["+path.toString()+"] doesn't exist.");
      throw new IOException(sb.toString());
    }
    else {
      log.info("File (" + path + ") OK.");
    }
    return storageOk;
  }  

  /**
   * Helper method to parse and store output from a given process' stdout and stderr
   *
   * @param process of type Process
   * @return Map<String, String>
   * @throws IOException when
   */
  public static Map<String, String> checkPipes(Process process) throws IOException {
    HashMap<String, String> r = new HashMap<String, String>();
    String error = LimsUtils.processStdErr(process);
    if (error.equals("")) {
      String out = LimsUtils.processStdOut(process);
      log.debug(out);
      r.put("ok", out);
    }
    else {
      log.error(error);
      r.put("error", error);
    }
    return r;
  }

  public static byte[] objectToByteArray(Object o) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bout);
    oos.writeObject(o);
    return bout.toByteArray();
  }

  public static Object byteArrayToObject(byte[] bytes) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
    ObjectInputStream ois = new ObjectInputStream(bin);
    return ois.readObject();
  }

  /**
   * Reads the contents of an InputStream into a String
   *
   * @param in of type InputStream
   * @return String
   * @throws IOException when
   */
  public static String inputStreamToString(InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line;
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    return sb.toString();
  }

  /**
   * Reads the contents of an File into a String
   *
   * @param f of type File
   * @return String
   * @throws IOException when
   */
  public static String fileToString(File f) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line;
    BufferedReader br = new BufferedReader(new FileReader(f));
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    return sb.toString();
  }

  /**
   * Reads the contents of an InputStream into a byte[]
   *
   * @param in of type InputStream
   * @return byte[]
   * @throws IOException when
   */
  public static byte[] inputStreamToByteArray(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = in.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }

  /**
   * Process stdout from a given Process and concat it to a single String
   *
   * @param p of type Process
   * @return String
   * @throws IOException when
   */
  private static String processStdOut(Process p) throws IOException {
    return inputStreamToString(p.getInputStream());
  }

  /**
   * Process stderr from a given Process and concat it to a single String
   *
   * @param p of type Process
   * @return String
   * @throws IOException when
   */
  private static String processStdErr(Process p) throws IOException {
    return inputStreamToString(p.getErrorStream());
  }

  public static String getCurrentDateAsString(DateFormat df) {
    return df.format(new Date());
  }

  public static String getCurrentDateAsString() {
    return getCurrentDateAsString(new SimpleDateFormat("yyyyMMdd"));
  }

  public static final Pattern linePattern = Pattern.compile(".*\r?\n");

  public static Matcher grep(CharBuffer cb, Pattern pattern) {
    Matcher lm = linePattern.matcher(cb);  // Line matcher
    Matcher pm = null;                     // Pattern matcher
    while (lm.find()) {
      CharSequence cs = lm.group();      // The current line
      if (pm == null)
        pm = pattern.matcher(cs);
      else
        pm.reset(cs);
      if (pm.find()) {
        return pm;
      }
      if (lm.end() == cb.limit()) break;
    }
    return null;
  }

  public static Matcher grep(File f, Pattern p) throws IOException {
    // Charset and decoder for ISO-8859-15
    Charset charset = Charset.forName("ISO-8859-15");
    CharsetDecoder decoder = charset.newDecoder();

    // Open the file and then get a channel from the stream
    FileInputStream fis = new FileInputStream(f);
    FileChannel fc = fis.getChannel();

    // Get the file's size and then map it into memory
    int sz = (int) fc.size();
    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

    // Decode the file into a char buffer
    CharBuffer cb = decoder.decode(bb);

    // Perform the search
    Matcher m = grep(cb, p);

    // Close the channel and the stream
    fc.close();

    return m;
  }

  public static Matcher tailGrep(File f, Pattern p, int lines) throws IOException, FileNotFoundException {
    // Open the file and then get a channel from the stream
    FileInputStream fis = new FileInputStream(f);
    FileChannel fc = fis.getChannel();
    try {
      // Get the file's size and then map it into memory
      int sz = (int) fc.size();
      MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

      long cnt = 0;
      long i = 0;
      for (i = sz - 1; i >= 0; i--) {
        if (bb.get((int) i) == '\n') {
          cnt++;
          if (cnt == lines + 1)
            break;
        }
      }

      int offset = (int) i + 1;

      if (offset >= bb.limit()) throw new NoSuchElementException();
      ByteArrayOutputStream sb = new ByteArrayOutputStream();
      while (offset < bb.limit()) {
        for (; offset < bb.limit(); offset++) {
          sb.write(bb.get(offset));
        }
      }

      // Decode the file into a char buffer
      CharBuffer cb = CharBuffer.wrap(sb.toString());

      // Perform the search
      return grep(cb, p);
    }
    catch (IOException e) {
      throw e;
    }
    finally {
      // Close the channel and the stream
      safeClose(fc);
    }
  }

  public static String reflectString(Object o) {
    StringBuilder result = new StringBuilder();
    try {
      result.append(o.getClass().getName());
      result.append("\n————————————\n");
      Class c = o.getClass();
      Field fieldList[] = c.getDeclaredFields();
      for(Field entry: fieldList) {
        result.append(entry.getName());
        result.append(":");
        result.append(entry.get(o));
        result.append("\n");
      }
    } catch (Exception e) {
      result.append("\n\nERROR: " + e.getMessage() + "\n\n");
    }
    return result.toString();
  }

  // put this anywhere you like in your common code.
  public static void safeClose(Closeable c) {
    try {
      c.close();
    } catch (Throwable t) {
      // Resource close failed!  There's only one thing we can do:
      // Log the exception using your favorite logging framework
      t.printStackTrace();
    }
  }
}
