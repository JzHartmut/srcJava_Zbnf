package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile implements FileRemoteAccessor
{
  
  /**Version and history.
   * <ul>
   * <li>2012-02-02 Hartmut chg: {@link #setFileProperties(FileRemote, File)}: There was an faulty recursive loop,
   *   more checks. 
   * <li>2012-01-09 Hartmut new: {@link #close()} terminates the thread.
   * <li>2012-01-06 Hartmut new: {@link #setFileProperties(FileRemote)} etc.
   * <li>2012-01-04 Hartmut new: copy file trees started from a given directory
   * <li>2011-12-31 Hartmut new {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. 
   * <li>2011-12-31 Hartmut new {@link #runCommissions} as extra thread.  
   * <li>2011-12-10 Hartmut creation: See {@link FileRemoteAccessor}.
   * </ul>
   */
  public static final int version = 0x20111210;

  private static FileRemoteAccessor instance;
  
  /**State of execution commissions.
   * '?': not started. 'w': waiting for commission, 'b': busy, 'x': should finish, 'z': finished
   */
  private char commissionState = '?';
  
  
  /**List of all commissions to do. */
  private final ConcurrentLinkedQueue<Commission> commissions = new ConcurrentLinkedQueue<Commission>();
  
  
  
  /**The thread to run all commissions. */
  private Runnable runCommissions = new Runnable(){
    @Override public void run(){
      runCommissions();
    }
  };
  
  
  private Thread thread = new Thread(runCommissions, "vishia.FileLocal");
  
  { thread.start(); }
  
  private Copy copy = new Copy();
  
  private FileRemote workingDir;
  
  /**Returns the singleton instance of this class.
   * Note: The instance will be created and the thread will be started if this routine was called firstly.
   * @return The singleton instance.
   */
  public static FileRemoteAccessor getInstance(){
    if(instance == null){
      instance = new FileRemoteAccessorLocalFile();
    }
    return instance;
  }
  
  
  /*
  @Override public Object createFileObject(FileRemote file)
  { Object oFile = new File(file.path, file.name);
    return oFile;
  }
  */
  
  
  /**Sets the file properties from the local file.
   * checks whether the file exists and set the {@link FileRemote#mTested} flag any time.
   * If the file exists, the properties of the file were set, elsewhere they were set to 0.
   * @see {@link org.vishia.util.FileRemoteAccessor#setFileProperties(org.vishia.util.FileRemote)}
   */
  @Override public boolean setFileProperties(FileRemote fileRemote)
  { String path = fileRemote.getPath();
    File fileLocal = new File(path);
    setFileProperties(fileRemote, fileLocal);
    return true;
  }

  /**Sets the file properties from the local file.
   * Called from {@link #setFileProperties(FileRemote)}, description see there.
   * @param fileRemote
   * @param fileLocal
   */
  private void setFileProperties(FileRemote fileRemote, File fileLocal){
    String path = fileRemote.getPath();
    if(fileLocal.exists()){
      String canonicalPath = FileSystem.getCanonicalPath(fileLocal);
      long date = fileLocal.lastModified();
      long length = fileLocal.length();
      int flags = FileRemote.mExist | FileRemote.mTested;
      if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
      if(fileLocal.isHidden()){ flags |= FileRemote.mHidden; }
      if(fileLocal.canWrite()){ flags |= FileRemote.mCanWrite; }
      if(fileLocal.canRead()){ flags |= FileRemote.mCanRead; }
      if(fileLocal.canExecute()){ flags |= FileRemote.mExecute; }
      if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
      if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
      fileRemote._setProperties(length, date, flags, fileLocal);
      if(fileLocal.isAbsolute()){
        String pathCleaned = FileSystem.cleanAbsolutePath(path);
        if(!canonicalPath.startsWith(pathCleaned)){
          fileRemote.setSymbolicLinkedPath(canonicalPath);
        } else {
          fileRemote.setCanonicalAbsPath(canonicalPath);
        }
      } else { //relative path
        if(workingDir == null){
          workingDir = new FileRemote(FileSystem.getCanonicalPath(new File(".")));  //NOTE: should be absolute
        }
        fileRemote.setReferenceFile(workingDir);  
      }
    } else { //fileLocal not exists:
      //designate it as tested, mExists isn't set.
      fileRemote._setProperties(0, 0, FileRemote.mTested, fileLocal);
    }
  }

  
  @Override public ReadableByteChannel openRead(FileRemote file, long passPhase)
  { try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  @Override public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
    FileOutputStream stream = new FileOutputStream(file);
    return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  @Override public FileRemote[] listFiles(FileRemote parent){
    FileRemote[] retFiles = null;
    File dir = (File)parent.oFile;
    if(dir.exists()){
      File[] files = dir.listFiles();
      if(files !=null){
        retFiles = new FileRemote[files.length];
        int iFile = -1;
        for(File fileLocal: files){
          retFiles[++iFile] = newFile(fileLocal);
        }
      }
    }
    return retFiles;
  }

  
  public FileRemote newFile(File fileLocal){
    String name = fileLocal.getName();
    String sDir = fileLocal.getParent().replace('\\', '/');
    FileRemote fileRemote = new FileRemote(this, sDir, name, 0, 0, 0, null);
    setFileProperties(fileRemote, fileLocal);  
    return fileRemote;
  }
  
  
  
  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }
  
  
  @Override public void addCommission(Commission com){ 
    commissions.add(com);
    synchronized(this){
      if(commissionState == 'w'){
        notify();
      } else {
        commissionState = 'c';
      }
    }
  }
  
  
  
  void runCommissions(){
    commissionState = 'r';
  
    while(commissionState != 'x'){ //exit?
      try{ //never let the thread crash
        Commission commission;
        if( (commission = commissions.poll()) !=null){
          synchronized(this){
            if(commissionState != 'x'){
              commissionState = 'b'; //busy
            }
          }
          if(commissionState == 'b'){
            execCommission(commission);
          }
        } else {
          synchronized(this){
            if(commissionState != 'x'){  //exit?
              commissionState = 'w';
              try{ wait(1000); } catch(InterruptedException exc){}
              if(commissionState == 'w'){ //can be changed while waiting
                commissionState = 'r';
              }
            }
          }
        }
      } catch(Exception exc){
        System.err.println("Unexpected exception " + exc.getLocalizedMessage());
        exc.printStackTrace(System.err);
      }
    }
  }
  
  
  void execCommission(Commission commission){
    switch(commission.cmd){
    case Commission.kCheckFile: copy.checkCopy(commission); break;
    case Commission.kCopy: copy.execCopy(commission); break;
    case Commission.kDel:  execDel(commission); break;
    
    }
  }
  
  
  
  private class Copy
  {
  
    long timestart;
    
    long zFilesCheck, zFilesCopy;
    
    long zBytesCheck, zBytesCopy;
    
    
    /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.Commission)}
     * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. */
    private final List<File> listFiles = new LinkedList<File>();
    
    private File currentFile;

    

    
    
    void checkCopy(Commission co){
      this.currentFile= co.src;
      listFiles.clear();
      if(currentFile.isDirectory()){ 
        zBytesCheck = 0;
        zFilesCheck = 0;
        checkDir(currentFile, 1);
      } else {
        listFiles.add(currentFile);
        zBytesCheck = co.src.length();
        zFilesCheck = 1;
      }
      co.callBack.data3 = zBytesCheck;  //number between 0...1000
      co.callBack.data4 = zFilesCheck;  //number between 0...1000
      co.callBack.id = FileRemoteAccessor.kNrofFilesAndBytes;
      co.callBack.sendtoDst();
    }
    
    
    
    void checkDir(File dir, int recursion){
      //try{
        File[] files = dir.listFiles();
        for(File file: files){
          if(file.isDirectory()){
            if(recursion < 100){ //prevent loop with itself
              checkDir(file, recursion+1);  //recursively
            }
          } else {
            listFiles.add(file);
            zFilesCheck +=1;
            zBytesCheck += file.length();
          }
        }
      //}catch(IOException exc){
        
      //}
    }
    
    
    private void execCopy(Commission co){
      timestart = System.currentTimeMillis();
      zFilesCopy = 0;
      zBytesCopy = 0;
      if(co.src.isDirectory()){
        copy.execCopyDir(co, co.src, co.dst);
      } else {
        copy.execCopyFile(co, co.src, co.dst);
      }
      co.callBack.id = FileRemoteAccessor.kFinishOk; //zBytesCopyFile == zBytesMax ? FileRemoteAccessor.kFinishOk : FileRemoteAccessor.kFinishNok;
      co.callBack.sendtoDst();
    }

    
    private void execCopyDir(Commission co, File src, File dst){
      assert(src.isDirectory());
      dst.mkdirs();
      File[] filesSrc = src.listFiles();
      for(File fileSrc: filesSrc){
        if(fileSrc.isDirectory()){
          File dirDst = new File(dst, fileSrc.getName());
          execCopyDir(co, fileSrc, dirDst);
        } else {
          File fileDst = new File(dst, fileSrc.getName());
          execCopyFile(co, fileSrc, fileDst);
        }
      }
    }
    
    
    private void execCopyFile(Commission co, File src, File dst){
      FileInputStream in = null;
      FileOutputStream out = null;
      final long zBytesMax = src.length();
      long zBytesCopyFile = 0;
      try{
        in = new FileInputStream(src);
        out = new FileOutputStream(dst);
        byte[] buffer = new byte[0x4000];  //16 kByte buffer
        boolean bContCopy;
        do {
          int zBytes = in.read(buffer);
          if(zBytes > 0){
            bContCopy = true;
            zBytesCopyFile += zBytes;
            zBytesCopy += zBytes;
            out.write(buffer, 0, zBytes);
            long time = System.currentTimeMillis();
            //
            //feedback of progression after about 0.3 second. 
            if(time > timestart + 300){
              co.callBack.data1 = (int)((float)zBytesCopyFile / zBytesMax * 1000);  //number between 0...1000
              co.callBack.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
              co.callBack.data3 = zFilesCheck - zFilesCopy;
              co.callBack.id = FileRemoteAccessor.kOperation;
              co.callBack.sendtoDst();
              timestart = time;
            }
          } else if(zBytes == -1){
            bContCopy = false;
            out.close();
          } else {
            //0 bytes ?
            bContCopy = true;
          }
        }while(bContCopy);
      } catch(IOException exc){
        co.callBack.data1 = (int)((float)zBytesCopyFile / zBytesMax * 1000);  //number between 0...1000
        co.callBack.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
        co.callBack.data3 = zFilesCheck - zFilesCopy;
        co.callBack.id = FileRemoteAccessor.kFinishError;
        co.callBack.sendtoDst();
      }
      try{
        if(in !=null) { in.close(); }
        if(out !=null) { out.close(); }
      }catch(IOException exc){}
      try {
        long date = src.lastModified();
        dst.setLastModified(date);
      } catch(Exception exc){
        System.err.println("can't modified date: " + dst.getAbsolutePath());
      }
      zFilesCopy +=1;
    }
  }  
  
  
  
  
  void execDel(Commission commission){
    
  }


  @Override public void close() throws IOException
  {
    synchronized(this){
      if(commissionState == 'w'){ notify(); }
      commissionState = 'x';
    }
  }
  
  
  
}
