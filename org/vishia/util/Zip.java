package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.vishia.mainCmd.MainCmd;

//import org.apache.tools.zip.ZipEntry;

/**This class supports working with zip files using the standard java zip methods.
 * This class supports a base path and wildcards like described 
 * in {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
 * <br><br>
 * Usage template:
 * <pre>
    Zip zip = new Zip();   //instance can be re-used in the same thread.
    zip.addSource(directory, path);
    zip.addSource(directory2, path2);
    String sError = zip.exec(dst, compressionLevel, comment);
    //the added sources are processed and removed.
    //next usage:
    zip.addSource(directory, path);
    String sError = zip.exec(dst, compressionLevel, comment);
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public class Zip {

  
  /**Version, history and license.
   * <ul>
   * <li>2013-02-09 Hartmut chg: {@link Cmdline#argList} now handled in {@link MainCmd#setArguments(org.vishia.mainCmd.MainCmd.Argument[])}.
   * <li>2013-01-20 Hartmut creation: New idea to use the zip facility of Java.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public final static int version = 20130120;

  
  
  
  static class Src { 
    String path; File dir;
    Src(String path, File dir) { this.path = path;  this.dir = dir; } 
  }
  
  private final List<Src> listSrc;
  
  
  /**Creates an empty instance for Zip.
   * One should call {@link #addSource(String)} or {@link #addSource(File, String)}
   * and then {@link #exec(File, int, String)} to create a Zipfile. 
   * <br><br>
   * The instance can be reused: After exec(...) the added sources are removed. call
   * {@link #addSource(String)} and {@link #exec(File, int, String)} with other files.
   * <br><br>
   * The instance should not be used in multithreading uncoordinated.
   */
  public Zip(){
    listSrc = new LinkedList<Src>();
  }
  
  /**Creates an instance for Zip with given sources. It is used especially in {@link #main(String[])}.
   * One can {@link #addSource(String)} with additional sources. The invoke {@link #exec(File, int, String)}
   * @param listSrc List of sources.
   */
  //public Zip(List<Src> listSrc){
  //  this.listSrc = listSrc;
  //}
  
  
  /**Adds a source file or some files designated with wildcards
   * @param src Path may contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. The localpath may contain wildcards. The basepath may be an absolute path or it is located
   *   in the systems current directory..
   *   If the sPath does not contain a basepath (especially it is a simple path to a file), this path is used in the zipfile.
   *   Especially the path can start from the current directory.
   *   For usage of basepath, localpath and wildcards see {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
   */
  public void addSource(String src){
    listSrc.add(new Src(src, null));
  }
  

  
  /**Adds a source file or some files designated with wildcards
   * @param dir the directory where the source path starts.
   * @param src Path may contain a basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. The localpath may contain wildcards.
   *   If the sPath does not contain a basepath (especially it is a simple path to a file), this path is used in the zipfile.
   *   For usage of basepath, localpath and wildcards see {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
   */
  public void addSource(File dir, String src){
    listSrc.add(new Src(src, dir));
  }
  

  
  /**Executes the zip with the given source files to a dst file.
   * @param dst The destination file.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @return an error hint or null if successful.
   */
  public String exec(File dst, int compressionLevel, String comment){
    String ret;
    final byte[] buffer = new byte[0x4000];
    try {
      ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(dst));
      
      //outZip.setComment(comment);
      outZip.setLevel(compressionLevel);
      
      
      for(Src src: listSrc){
        //get the files.
        List<FileSystem.FileAndBasePath> listFiles= new ArrayList<FileSystem.FileAndBasePath>();
        
        FileSystem.addFilesWithBasePath (src.dir, src.path, listFiles);
        
        for(FileSystem.FileAndBasePath filentry: listFiles){
          ZipEntry zipEntry = null;
          InputStream in = null;
          try{
            zipEntry = new ZipEntry(filentry.localPath);
            outZip.putNextEntry(zipEntry);
            in = new FileInputStream(filentry.file);
            int bytes;
            while( (bytes = in.read(buffer))>0){
              outZip.write(buffer, 0, bytes);
            }
          } catch(IOException exc){
            System.err.println("vishia.util.Zip.writeZip - entry; " + exc.getMessage());
          } finally {
            if(in !=null) { in.close(); }
            if(zipEntry !=null) { outZip.closeEntry(); }
          }
        }
      }  
      outZip.close();
      
      ret = null;
      
    } catch (FileNotFoundException e) {
      ret = "<?? vishia.zip.Zip - File not found: "+ e.getMessage() + "??>";
    } catch (IOException e) {
      ret = "<?? vishia.zip.Zip - Any file writing problem: "+ e.getMessage() + "??>";
    }
    listSrc.clear();
    return ret;
    
  }
  
  
  
  
  /**Executes Zip with given arguments.
   * Note: One can have add sources calling {@link #addSource(String)} before additional to the sources in add.
   * But usual the sources should be given only in args.
   * <br><br>
   * Invoke:
   * <pre>
   * Zip zip = new Zip();
   * Zip.Args args = .... get args from somewhere
   * zip.exec(args);
   * @param args
   * @return
   */
  public String exec(Args args){
    listSrc.addAll(args.listSrc);
    return exec(args.fOut, args.compress, args.comment);
  }
  
  
  
  /**Zips some files in a dst file.
   * @param dst The destination file.
   * @param sPath Path should contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. For usage of basepath, localpath see {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @return an error hint or null if successful.
   */
  public static String zipfiles(File dst, File srcdir, String sPath, int compressionLevel, String comment){
    Zip zip = new Zip();
    zip.addSource(sPath);
    return zip.exec(dst, compressionLevel, comment);
  }

  
  
  public static void main(String[] args){
    Args argData = new Args();
    Cmdline cmd = new Cmdline(argData);
    try{ cmd.parseArguments(args);
    } catch(ParseException exc){
      cmd.setExitErrorLevel(MainCmd.exitWithArgumentError);
      cmd.exit();
    }
    Zip zip = new Zip();
    zip.exec(argData);
  }
  
  
  
  
  /**This class holds arguments to zip.
   */
  public static class Args{
    public final List<Src> listSrc = new ArrayList<Src>();

    public int compress = 5;
    
    public File fOut;
    
    public String comment = "";

  }
  
  
  

  static class Cmdline extends MainCmd {

    
    final Args args;
    
    MainCmd.SetArgument setCompress = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      char cc;
      if(val.length()== 1 && (cc=val.charAt(0))>='0' && cc <='9'){
        args.compress = cc-'0';
        return true;
      } else return false;
    }};
    
    MainCmd.SetArgument setInput = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      args.listSrc.add(new Src(val, null));
      return true;
    }};
    
    MainCmd.SetArgument setOutput = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      args.fOut = new File(val);
      return true;
    }};
    
    MainCmd.Argument[] argList =
    { new MainCmd.Argument("-compress", ":0..9 set the compression rate 0=non .. 90max", setCompress)
    , new MainCmd.Argument("-o", ":ZIP.zip file for zip output", setOutput)
    , new MainCmd.Argument("", "INPUT file possible with wildcards also in path like \"path/** /dir* /name*.ext*\"", setInput)
    };
    

    
    @Override
    protected boolean checkArguments() {
      if(args.fOut !=null){ return true; }
      else return false;
    }

    
    Cmdline(Args args){
      this.args = args;
      super.addAboutInfo("Zip routine from Java");
      super.addAboutInfo("made by HSchorrig, 2013-02-09");
      super.addHelpInfo("args: -compress:# -o:ZIP.zip { INPUT}");  //[-w[+|-|0]]
      super.addArgument(argList);
      super.addStandardHelpInfo();
    }
    
  }
  
  
  
  public static void test2(){
    String err;
    if( (err = zipfiles(new File("testzip.zip"), null, "./.:*", 9, "zip comment")) !=null){
      System.err.println(err);
    }
  }
  
  
}
