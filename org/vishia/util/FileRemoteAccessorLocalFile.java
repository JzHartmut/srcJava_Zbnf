package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.util.FileRemote.FileRemoteAccessorSelector;

/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile implements FileRemoteAccessor
{
  
  /**Version, history and license.
   * <ul>
   * <li>2012-08-05 Hartmut new: If the oFile reference is null, the java.io.File instance for the local file will be created anyway.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-30 Hartmut new: execution of {@link #refreshFileProperties(FileRemote, Event)} and {@link #refreshFilePropertiesAndChildren(FileRemote, Event)}
   *   in an extra thread if a callback is given. It is substantial for a fluently working with files, if an access
   *   for example in network hangs.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: implementation of the {@link FileRemote#chgProps(String, int, int, long, org.vishia.util.FileRemote.FileRemoteEvent)} etc.
   * <li>2012-02-02 Hartmut chg: {@link #refreshFileProperties(FileRemote, File)}: There was an faulty recursive loop,
   *   more checks. 
   * <li>2012-01-09 Hartmut new: {@link #close()} terminates the thread.
   * <li>2012-01-06 Hartmut new: {@link #refreshFileProperties(FileRemote)} etc.
   * <li>2012-01-04 Hartmut new: copy file trees started from a given directory
   * <li>2011-12-31 Hartmut new {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. 
   * <li>2011-12-31 Hartmut new {@link #runCommissions} as extra thread.  
   * <li>2011-12-10 Hartmut creation: See {@link FileRemoteAccessor}.
   * </ul>
   * <br><br>
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
   * 
   */
  public static final int version = 20120721;

  private static FileRemoteAccessor instance;
  
  /**State of execution commissions.
   * '?': not started. 'w': waiting for commission, 'b': busy, 'x': should finish, 'z': finished
   */
  private char commissionState = '?';
  
  
  /**List of all commissions to do. */
  private final ConcurrentLinkedQueue<FileRemote.FileRemoteEvent> commissions = new ConcurrentLinkedQueue<FileRemote.FileRemoteEvent>();
  
  
  /**The thread to run all commissions. */
  /*
  protected Runnable runCommissions = new Runnable(){
    @Override public void run(){
      runCommissions();
    }
  };
  */
  
  //private Thread thread = new Thread(runCommissions, "vishia.FileLocal");
  //{ thread.start(); }
  
  
  EventThread singleThreadForCommission = new EventThread("FileAccessor-local");
  
  
  
  private Copy copy = new Copy();
  
  private FileRemote workingDir;
  
  
  public FileRemoteAccessorLocalFile() {
    singleThreadForCommission.startThread();
  }
  
  
  
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
  
  
  
  private File getLocalFile(FileRemote fileRemote){
    //NOTE: use the superclass File only as interface, use a second instance.
    //the access to super methods does not work. Therefore access to non-inherited File.methods.
    if(fileRemote.oFile == null){
      String path = fileRemote.getPath();
      fileRemote.oFile = new File(path);
    }
    return (File)fileRemote.oFile;
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
   * @see {@link org.vishia.util.FileRemoteAccessor#refreshFileProperties(org.vishia.util.FileRemote)}
   */
  @Override public void refreshFileProperties(final FileRemote fileRemote, final Event callback)
  { 
  
    
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new Runnable(){
      public void run(){
        File fileLocal = getLocalFile(fileRemote);
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
        if(callback !=null){
          callback.callback();
        }
      }
    };
  
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);
      threadObj.start(); //run in an exttra thread, the caller doesn't wait.
    }
  
  }  
    

  
  @Override public void refreshFilePropertiesAndChildren(final FileRemote fileRemote, final Event callback){
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new Runnable(){
      public void run(){
        refreshFileProperties(fileRemote, null);
        File fileLocal = getLocalFile(fileRemote);
        fileRemote.flags |= FileRemote.mChildrenGotten;
        if(fileLocal.exists()){
          File[] files = fileLocal.listFiles();
          if(files !=null){
            fileRemote.children = new FileRemote[files.length];
            int iFile = -1;
            for(File file1: files){
              fileRemote.children[++iFile] = newFile(file1);
            }
          }
        }
        if(callback !=null){
          callback.callback();
        }
        fileRemote.flags &= ~FileRemote.mThreadIsRunning;
      }
    };
      
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      if((fileRemote.flags & FileRemote.mThreadIsRunning) ==0) {
        fileRemote.flags |= FileRemote.mThreadIsRunning;
        Thread threadObj = new Thread(thread);
        threadObj.start(); //run in an exttra thread, the caller doesn't wait.
      } else {
        callback.consumed(); //ignore it.
      }
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

  
  //@Override 
  public FileRemote[] XXXlistFiles(FileRemote parent){
    FileRemote[] retFiles = null;
    if(parent.oFile == null){
      
    }
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
    FileRemote dir = FileRemote.fromFile(fileLocal.getParentFile());
    FileRemote fileRemote = new FileRemote(this, dir, sDir, name, 0, 0, 0, fileLocal);
    refreshFileProperties(fileRemote, null);  
    return fileRemote;
  }
  
  
  @Override public boolean delete(FileRemote file, FileRemote.FileRemoteEvent callback){
    File fileLocal = getLocalFile(file);
    if(callback == null){
      return fileLocal.delete();
    } else {
      boolean bOk = fileLocal.delete();
      callback.cmd = bOk ? 0: FileRemote.acknErrorDelete; 
      callback.callback();
      return bOk;
    }
  }

  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  
  @Override public void addCommission(FileRemote.FileRemoteEvent ev){ 
    ev.setDst(executerCommission, singleThreadForCommission);
    singleThreadForCommission.storeEvent(ev);
  }
  
  /*
  @Override public void addCommission(FileRemote.FileRemoteEvent com){ 
    switch(com.cmd){
    case FileRemote.cmdAbortAll: copy.bAbortAll = true; break;
    case FileRemote.cmdAbortDir: copy.bAbortDir = true; break;
    case FileRemote.cmdAbortFile: copy.bAbortFile = true; break;
    default: {
      //write commission in queue:
        commissions.add(com);
        synchronized(this){
          if(commissionState == 'w'){
            notify();
          } else {
            commissionState = 'c';
          }
        }
      } 
    }
  }
  */
  
 
  /*
  void runCommissions(){
    commissionState = 'r';
  
    while(commissionState != 'x'){ //exit?
      try{ //never let the thread crash
        FileRemote.FileRemoteEvent commission;
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
              commissionState = 'w';      //w = waiting, notify necessary
              try{ wait(1000); } catch(InterruptedException exc){}
              if(commissionState == 'w'){ //can be changed while waiting, set only to 'r' if 'w' is still present
                commissionState = 'r';
              }
            }
          }
        }
      } catch(Exception exc){
        System.err.println("Unexpected exception " + exc.getMessage());
        exc.printStackTrace(System.err);
      }
    }
  }
  */
  
  void execCommission(FileRemote.FileRemoteEvent commission){
    switch(commission.cmd){
    case FileRemote.cmdCheckFile: copy.checkCopy(commission); break;
    case FileRemote.cmdCopy: copy.execCopy(commission); break;
    case FileRemote.cmdMove: copy.execMove(commission); break;
    case FileRemote.cmdChgProps:  execChgProps(commission); break;
    case FileRemote.cmdChgPropsRec:  execChgPropsRecurs(commission); break;
    case FileRemote.cmdCountLength:  execCountLength(commission); break;
    case FileRemote.cmdDel:  execDel(commission); break;
      
    }
  }
  
  
  private void execChgProps(FileRemote.FileRemoteEvent co){
    File dst;
    FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = callBack !=null;
    if(co.newName !=null && ! co.newName.equals(co.filesrc.getName())){
      dst = new File(co.filesrc.getParent(), co.newName);
      ok &= co.filesrc.renameTo(dst);
    } else {
      dst = co.filesrc;
    }
    ok = chgFile(dst, co, ok);
    if(callBack !=null){
      if(ok){
        callBack.cmd = FileRemoteAccessor.kFinishOk; 
      } else {
        callBack.cmd = FileRemoteAccessor.kFinishNok; 
      }
      callBack.callback();
    }
  }
  
  
  private void execChgPropsRecurs(FileRemote.FileRemoteEvent co){
    File dst;
    FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = callBack !=null;
    if(co.newName !=null && ! co.newName.equals(co.filesrc.getName())){
      dst = new File(co.filesrc.getParent(), co.newName);
      ok &= co.filesrc.renameTo(dst);
    } else {
      dst = co.filesrc;
    }
    ok = chgPropsRecursive(dst, co, ok, 0);
    if(callBack !=null){
      if(ok){
        callBack.cmd = FileRemoteAccessor.kFinishOk; 
      } else {
        callBack.cmd = FileRemoteAccessor.kFinishNok; 
      }
      callBack.callback();
    }
  }
  
  
  
  private boolean chgPropsRecursive(File dst, FileRemote.FileRemoteEvent co, boolean ok, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(dst.isDirectory()){
      File[] filesSrc = dst.listFiles();
      for(File fileSrc: filesSrc){
        ok = chgPropsRecursive(fileSrc, co, ok, recursion +1);
      }
    } else {
      ok = chgFile(dst, co, ok);
    }
    return ok;
  }
  

  
  private boolean chgFile(File dst, FileRemote.FileRemoteEvent co, boolean ok){
    if(ok && (co.maskFlags & FileRemote.mCanWrite) !=0){ ok = dst.setWritable((co.newFlags & FileRemote.mCanWrite) !=0, true); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setWritable((co.newFlags & FileRemote.mCanWriteAny) !=0); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setReadable((co.newFlags & FileRemote.mCanWriteAny) !=0, true); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setReadable((co.newFlags & FileRemote.mCanWriteAny) !=0); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setExecutable((co.newFlags & FileRemote.mCanWriteAny) !=0, true); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setExecutable((co.newFlags & FileRemote.mCanWriteAny) !=0); }
    if(ok && co.newDate !=0 && co.newDate !=-1 ){ ok = dst.setLastModified(co.newDate); }
    return ok;
  }
  
  
  
  private void execCountLength(FileRemote.FileRemoteEvent co){
    long length = countLengthDir(co.filesrc, 0, 0);    
    FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    if(callBack !=null){
      if(length >=0){
        callBack.cmd = FileRemoteAccessor.kFinishOk;
        callBack.nrofBytesAll = length;
      } else {
        callBack.cmd = FileRemoteAccessor.kFinishNok; 
      }
      callBack.callback();
    }
  }
  
  
  private long countLengthDir(File file, long sum, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(file.isDirectory()){
      File[] filesSrc = file.listFiles();
      for(File fileSrc: filesSrc){
        sum = countLengthDir(fileSrc, sum, recursion+1);
      }
    } else {
      sum += file.length();
    }
    return sum;
  }
  
  
  
  void execDel(FileRemote.FileRemoteEvent co){
    
  }


  @Override public void close() throws IOException
  {
    synchronized(this){
      if(commissionState == 'w'){ notify(); }
      commissionState = 'x';
    }
  }
  
  
  EventConsumer executerCommission = new EventConsumer(){
    @Override public boolean processEvent(Event ev) {
      if(ev instanceof FileRemote.FileRemoteEvent){
        execCommission((FileRemote.FileRemoteEvent)ev);
        return true;
      } else {
        return false;
      }
    }
    
  };
  
  
  protected class Copy
  {
  
    long timestart;
    
    //boolean bAbortFile, bAbortDir, bAbortAll;
    
    int zFilesCheck, zFilesCopy;
    
    long zBytesCheck, zBytesCopy;
    
    /**Buffer for copy. It is allocated static. 
     * Only used in this thread {@link FileRemoteAccessorLocalFile#runCommissions}. 
     * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
     * at least the size of 1 record of the file system. */
    byte[] buffer = new byte[0x100000];  //1 MByte 16 kByte buffer
    
    
    /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.FileRemoteEvent)}
     * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.FileRemoteEvent)}. */
    private final List<File> listCopyFiles = new LinkedList<File>();
    
    private File currentFile;

    private int ctWorkingId = 0;

    private int checkId;
    
    void checkCopy(FileRemote.FileRemoteEvent co){
      this.currentFile= co.filesrc;
      listCopyFiles.clear();
      if(currentFile.isDirectory()){ 
        zBytesCheck = 0;
        zFilesCheck = 0;
        checkDir(currentFile, 1);
      } else {
        listCopyFiles.add(currentFile);
        zBytesCheck = co.filesrc.length();
        zFilesCheck = 1;
      }
      co.data1 = checkId = ++ctWorkingId;
      co.nrofBytesAll = (int)zBytesCheck;  //number between 0...1000
      co.nrofFiles = zFilesCheck;  //number between 0...1000
      co.cmd = FileRemoteAccessor.kNrofFilesAndBytes;
      co.callback();
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
            listCopyFiles.add(file);
            zFilesCheck +=1;
            zBytesCheck += file.length();
          }
        }
      //}catch(IOException exc){
        
      //}
    }

    
    
    private void execMove(FileRemote.FileRemoteEvent co){
      timestart = System.currentTimeMillis();
      if(co.filesrc.renameTo(co.filedst)){
        co.cmd = FileRemoteAccessor.kFinishOk; 
      } else {
        co.cmd = FileRemoteAccessor.kFinishNok; 
      }
      co.callback();
    }

    

    
    private void execCopy(FileRemote.FileRemoteEvent co){
      timestart = System.currentTimeMillis();
      zFilesCopy = 0;
      zBytesCopy = 0;
      if(co.filesrc.isDirectory()){
        copy.execCopyDir(co, co.filesrc, co.filedst);
      } else {
        copy.execCopyFile(co, co.filesrc, co.filedst);
      }
      if(co.cmd()== FileRemote.cmdAbortAll){
        co.setCmd(0);
      }
      co.cmd = FileRemoteAccessor.kFinishOk; //zBytesCopyFile == zBytesMax ? FileRemoteAccessor.kFinishOk : FileRemoteAccessor.kFinishNok;
      co.callback();
    }

    
    private void execCopyDir(FileRemote.FileRemoteEvent co, File src, File dst){
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
        int evCmd = co.cmd();
        if(evCmd == FileRemote.cmdAbortDir || evCmd == FileRemote.cmdAbortAll){
          if(evCmd == FileRemote.cmdAbortDir){
            FileSystem.rmdir(dst);
            co.setCmd(0);
          }
          break;
        }
      }
    }
    
    
    private void execCopyFile(FileRemote.FileRemoteEvent co, File src, File dst){
      FileInputStream in = null;
      FileOutputStream out = null;
      final long zBytesMax = src.length();
      long zBytesCopyFile = 0;
      int evCmd;
      try{
        in = new FileInputStream(src);
        out = new FileOutputStream(dst);
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
              co.data1 = (int)((float)zBytesCopyFile / zBytesMax * 1000);  //number between 0...1000
              co.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
              co.nrofFiles = zFilesCheck - zFilesCopy;
              co.nrofBytesInFile = (int)zBytesCopy;
              String name = src.getName();
              int zName = name.length();
              if(zName > co.fileName.length){ 
                zName = co.fileName.length;    //shorten the name, it is only an info 
              }
              System.arraycopy(name.toCharArray(), 0, co.fileName, 0, zName);
              Arrays.fill(co.fileName, zName, co.fileName.length, '\0');
              co.cmd = FileRemoteAccessor.kOperation;
              co.callback();
              timestart = time;
            }
          } else if(zBytes == -1){
            bContCopy = false;
            out.close();
          } else {
            //0 bytes ?
            bContCopy = true;
          }
          evCmd = co.cmd();
        }while(bContCopy && evCmd != FileRemote.cmdAbortFile && evCmd != FileRemote.cmdAbortDir && evCmd != FileRemote.cmdAbortAll);
      } catch(IOException exc){
        evCmd = 0;
        System.err.println("Copy exc "+ exc.getMessage());
        co.data1 = (int)((float)zBytesCopyFile / zBytesMax * 1000);  //number between 0...1000
        co.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
        co.nrofFiles = zFilesCheck - zFilesCopy;
        co.cmd = FileRemoteAccessor.kFinishError;
        co.callback();
      }
      try{
        if(in !=null) { in.close(); }
        if(out !=null) { out.close(); }
        if(evCmd != FileRemote.cmdAbortFile){
          boolean bOkdel = dst.delete();
          if(bOkdel){
            
          }
          co.setCmd(0);
        }
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

  
  public static FileRemoteAccessorSelector selectLocalFileAlways = new FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileRemoteAccessorLocalFile.getInstance();
    }
  };
  
}
