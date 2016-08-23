package org.vishia.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;


/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile extends FileRemoteAccessor
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-03-31 Hartmut bugfix: number of percent in backevent while copy
   * <li>2012-11-17 Hartmut chg: review of {@link #execChgProps(org.vishia.util.FileRemote.CmdEvent)} etc. It should not work before.
   *   yet not all is tested. 
   * <li>2012-10-01 Hartmut chg: Some adaption because {@link FileRemote#listFiles()} returns File[] and not FileRemote[].
   * <li>2012-10-01 Hartmut experience {@link #useFileChildren}
   * <li>2012-10-01 Hartmut new: {@link #refreshFilePropertiesAndChildren(FileRemote, org.vishia.util.FileRemote.CallbackEvent)} time measurement
   * <li>2012-09-26 Hartmut new: {@link #refreshFileProperties(FileRemote, org.vishia.util.FileRemote.CallbackEvent)} 
   *   thread with exception msg.
   * <li>2012-08-05 Hartmut new: If the oFile reference is null, the java.io.File instance for the local file will be created anyway.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-30 Hartmut new: execution of {@link #refreshFileProperties(FileRemote, Event)} and {@link #refreshFilePropertiesAndChildren(FileRemote, Event)}
   *   in an extra thread if a callback is given. It is substantial for a fluently working with files, if an access
   *   for example in network hangs.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: implementation of the {@link FileRemote#chgProps(String, int, int, long, org.vishia.util.FileRemote.CallbackEvent)} etc.
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
  public static final int version = 20130331;

  /**Some experience possible: if true, then store File objects in {@link FileRemote#children} instead
   * {@link FileRemote} objects. The File objects may be replaces by FileRemote later if necessary. This may be done
   * in applications. The problem is: Wrapping a File with FileRemote does not change the reference in {@link FileRemote#children}
   * automatically. It should be done by any algorithm. Therefore this compiler switch is set to false yet.
   */
  private final static boolean useFileChildren = false;
  
  
  private static FileRemoteAccessor instance;
  
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };

  

  
  EventThread singleThreadForCommission = new EventThread("FileAccessor-local");
  
  /**Destination for all events which forces actions in the execution thread.
   * 
   */
  EventConsumer executerCommission = new EventConsumer("FileRemoteAccessorLocal - executerCommision"){
    @Override protected boolean processEvent_(Event ev) {
      if(ev instanceof Copy.EventCpy){ //internal Event
        copy.stateCopy.process(ev);
        return true;
      } else if(ev instanceof FileRemote.CmdEvent){  //event from extern
            execCommission((FileRemote.CmdEvent)ev);
        return true;
      } else {
        return false;
      }
    }
    
  };
  

  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileRemoteAccessorLocalFile)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  private final Copy copy = new Copy(this);  
  
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
  @Override public void refreshFileProperties(final FileRemote fileRemote, final FileRemote.CallbackEvent callback)
  { 
  
    
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new RunRefresh(fileRemote, callback);
  
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);
      threadObj.start(); //run in an extra thread, the caller doesn't wait.
    }
  
  }  
    

  
  @Override public void refreshFilePropertiesAndChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){

    RunRefreshWithChildren thread = new RunRefreshWithChildren(fileRemote, callback);
    
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      if((fileRemote.flags & FileRemote.mThreadIsRunning) ==0) {
        fileRemote.flags |= FileRemote.mThreadIsRunning;
        Thread threadObj = new Thread(thread);
        thread.time = System.currentTimeMillis();
        threadObj.start(); //run in an exttra thread, the caller doesn't wait.
      } else {
        System.err.println("FileRemoteAccessLocalFile.refreshFilePropertiesAndChildren - double call, ignored;");
        callback.relinquish(); //ignore it.
      }
    }
  }

  
  @Override
  public List<File> getChildren(FileRemote file, FileFilter filter){
    File data = (File)file.oFile;
    File[] children = data.listFiles(filter);
    List<File> list = new LinkedList<File>();
    for(File file1: children){
      list.add(file1);
    }
    return list;
  }

  
  
  @Override public ReadableByteChannel openRead(FileRemote file, long passPhase)
  { try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  
  @Override public InputStream openInputStream(FileRemote file, long passPhase){
    try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream;
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
          retFiles[++iFile] = newFile(fileLocal, parent);
        }
      }
    }
    return retFiles;
  }

  
  public FileRemote newFile(File fileLocal, FileRemote dir){
    String name;
    //if(fileLocal.isDirectory()){
      //name = fileLocal.getName() + "/";
    //} else {
      name = fileLocal.getName();
    //}
    //File parent = fileLocal.getParentFile();
    String sDir = dir.getAbsolutePath().replace('\\', '/');
    //String sDir = fileLocal.getParent().replace('\\', '/');
    //FileRemote dir = null; //FileRemote.fromFile(parent);
    int flags = fileLocal.isDirectory() ? FileRemote.mDirectory:0;
    FileRemote fileRemote = new FileRemote(this, dir, sDir, name, 0, 0, flags, fileLocal);
    //refreshFileProperties(fileRemote, null);  
    return fileRemote;
  }
  
  
  @Override public boolean delete(FileRemote file, FileRemote.CallbackEvent callback){
    File fileLocal = getLocalFile(file);
    if(callback == null){
      return fileLocal.delete();
    } else {
      boolean bOk = fileLocal.delete();
      callback.occupy(evSrc, true);
      callback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.errorDelete );
      return bOk;
    }
  }

  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  
  
  
  /**Creates an CmdEvent if necessary, elsewhere uses the opponent of the given evBack and occupies it.
   * While occupying the Cmdevent is completed with the destination, it is {@link #executerCommission}.
   * @see org.vishia.util.FileRemoteAccessor#prepareCmdEvent(org.vishia.util.FileRemote.CallbackEvent)
   */
  @Override public FileRemote.CmdEvent prepareCmdEvent(FileRemote.CallbackEvent evBack){
    Event cmdEvent1 = evBack.getOpponent();
    if(cmdEvent1 !=null){
      if(!cmdEvent1.occupy(evSrc, null, executerCommission, singleThreadForCommission, false)){
        return null;
      }
    } else {
      cmdEvent1 = new FileRemote.CmdEvent(evSrc, null, executerCommission, singleThreadForCommission, evBack);
    }
    return (FileRemote.CmdEvent) cmdEvent1; 
  }
  
  
  void execCommission(FileRemote.CmdEvent commission){
    FileRemote.Cmd cmd = commission.getCmd();
    switch(cmd){
      case check: copy.checkCopy(commission); break;
      case overwr:
      case abortAll:
      case abortCopyDir:
      case abortCopyFile:
      case copy: copy.stateCopy.process(commission); break;
      case move: copy.execMove(commission); break;
      case chgProps:  execChgProps(commission); break;
      case chgPropsRecurs:  execChgPropsRecurs(commission); break;
      case countLength:  execCountLength(commission); break;
      case delete:  execDel(commission); break;
      
    }
  }
  
  
  private void execChgProps(FileRemote.CmdEvent co){
    FileRemote dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName !=null && ! co.newName.equals(co.filesrc.getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName);
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(fileRenamed);
    } else {
      dst = co.filesrc;
    }
    ok = chgFile(dst, co.maskFlags, co.newFlags, ok);
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done; 
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();
    
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd );
  }
  
  
  private void execChgPropsRecurs(FileRemote.CmdEvent co){
    FileRemote dst;
    boolean ok = co !=null;
    if(co.newName !=null && ! co.newName.equals(co.filesrc.getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName);
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(fileRenamed);
    } else {
      dst = co.filesrc;
    }
    ok &= chgPropsRecursive(dst, co.maskFlags, co.newFlags, ok, 0);
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done ; 
    } else {
      cmd = FileRemote.CallbackCmd.error ; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd);
  }
  
  
  
  private boolean chgPropsRecursive(File dst, int maskFlags, int newFlags, boolean ok, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(dst.isDirectory()){
      File[] filesSrc = dst.listFiles();
      for(File fileSrc: filesSrc){
        ok = chgPropsRecursive(fileSrc, maskFlags, newFlags, ok, recursion +1);
      }
    } else {
      ok = chgFile(dst, maskFlags, newFlags, ok);
    }
    return ok;
  }
  

  
  private boolean chgFile(File dst, int maskFlags, int newFlags, boolean ok){
    //if(dst instanceof FileRemote)
    //int flagsNow = dst.getFlags();
    //int chg = (flagsNow ^ newFlags) & maskFlags;  //changed and masked
    int chg = maskFlags;
    int mask = 1;
    while(mask !=0){
      if((chg & mask)!=0){ 
        if(!chgFile1(dst, mask, newFlags)){
          ok = false;
        }
      }
      mask <<=1;
    }
    return ok;
  }
  
  
  private boolean chgFile1(File dst, int maskFlags, int newFlags){
    boolean bOk;
    boolean set = (newFlags & maskFlags ) !=0;
    switch(maskFlags){
      case FileRemote.mCanWrite:{ bOk = dst.setWritable(set); } break;
      case FileRemote.mCanWriteAny:{ bOk = dst.setWritable(set, true); } break;
      default: { bOk = false; }
    }//switch
    if(bOk && dst instanceof FileRemote){
      FileRemote dst1 = (FileRemote)dst;
      dst1.flags = DataAccess.setBit(dst1.flags, maskFlags, set);
    }
    return bOk;
  }
  
  
  
  private void execCountLength(FileRemote.CmdEvent co){
    long length = countLengthDir(co.filesrc, 0, 0);    
    FileRemote.CallbackEvent evback = co.getOpponent();
    evback.occupy(evSrc, true);
    FileRemote.CallbackCmd cmd;
    if(length >=0){
      cmd = FileRemote.CallbackCmd.done; 
      evback.nrofBytesAll = length;
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    evback.sendEvent(cmd );
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
  
  
  
  void execDel(FileRemote.CmdEvent co){
    
  }


  @Override public void close() throws IOException
  { singleThreadForCommission.close();
  }
  
  
  
  protected static class Copy
  {
  
    /**This data set holds the information about the currently processed directory or file
     * while copying.
     *
     */
    private class DataSetCopy1Recurs{
      
      /**The source and destination file or directory. */
      File src, dst;

      /**null if this card describes a file and not a directory. The content of src*/
      File[] listSrc;
      /**current index in listSrc while in State {@link EStateCopy#Process}.*/
      int ixSrc;
      
     
    }
    
    
    
    /**The event type for intern events. One permanent instance of this class will be created. */
    private final class EventCpy extends Event{
      
      /**The simple constructor calls {@link Event#Event(Object, EventConsumer, EventThread)}
       * with the {@link FileRemoteAccessorLocalFile#executerCommission}
       * and the {@link FileRemoteAccessorLocalFile#singleThreadForCommission}
       * because the event should be send to that.
       */
      EventCpy(FileRemoteAccessorLocalFile accessor){
        super(null, null, accessor.executerCommission, accessor.singleThreadForCommission, new EventCpy(accessor,true));
      }
      
      /**Creates a simple event as opponent. */
      EventCpy(FileRemoteAccessorLocalFile accessor, boolean second){
        super(null, null, accessor.executerCommission, accessor.singleThreadForCommission, null);
      }
      
      /**Qualified sendEvent with the correct enum type of command code.
       * @param cmd
       * @return true if success.
       */
      boolean sendEvent(CmdCpyIntern cmd){ return super.sendEvent(cmd); }
      
      /**Qualified getCmd with the correct enum type of command code.
       * @return The command inside the received event.
       */
      @Override public CmdCpyIntern getCmd(){ return (CmdCpyIntern)super.getCmd(); }
      
      
      @Override public EventCpy getOpponent(){ return (EventCpy)super.getOpponent(); }
    };
    
    /**The only one instance for this event. It is allocated permanently. That is possible because the event is fired 
     * only once at the same time in this class locally and private.
     * The event and its oposite are used one after another. That is because the event is relinguished after it is need twice.
     */
    final EventCpy evCpy;
    
    /**Stored callback event when the state machine is in state {@link EStateCopy#Process}. 
     * 
     */
    private FileRemote.CallbackEvent evBackInfo;
    
    
    /**commands to send inside this state machine. */
    //private final static int cmdCopyDirFile = 0xc0b70001, cmdCopyFilePart = 0xc0b70002, cmdCopyFile = 0xc0b0003
    //  , cmdCopyDir = 0xc0b7d13, cmdCopyAsk = 0xc0b70a58; 
    
    
    private enum CmdCpyIntern { //Event.Cmd {
      free, reserve,
      
      /**Start a copy process. Event cmd for transition from {@link StateCopy#Start} to {@link StateCopyProcess#DirOrFile} */
      start, 
      
      /**A new {@link DataSetCopy1Recurs} instance was created in {@link Copy#actData}. It should be checked
       * whether it is a directory or file. Event cmd for transition to {@link StateCopyProcess#DirOrFile}. */
      //dirFile,
      //openSubDir,
      subDir, 
      file,
      dir, 
      ask,
      /**Sent if a subdir is found but it is empty. */
      emptyDir
      
    }

    /**Local helper class (only instantiated local) to check which type of event.
     */
    private static class PrepareEventCmd{
      final FileRemote.Cmd cmde;
      final CmdCpyIntern cmdi;
      final FileRemote.CmdEvent eve;
      final EventCpy evi;
      
      PrepareEventCmd(Event evP){
        if(evP instanceof FileRemote.CmdEvent){
          eve = (FileRemote.CmdEvent) evP;
          cmde = eve.getCmd();
          evi = null;
          cmdi = CmdCpyIntern.free;
        } else if(evP instanceof EventCpy){
          eve = null;
          cmde = FileRemote.Cmd.free;
          evi = (EventCpy) evP;
          cmdi = evi.getCmd();
        } else {
          eve = null;
          cmde = FileRemote.Cmd.free;
          evi = null;
          cmdi = CmdCpyIntern.free;
        }
      }
    }

    
    
    final FileRemoteAccessorLocalFile outer;
    
    /**Main state. */
    //EStateCopy stateCopy = EStateCopy.Null;
    
    /**Inner state inside Process. */
    //EStateCopyProcess stateCopyProcess = EStateCopyProcess.Null;
    
    /**Mode of copy, see {@link FileRemote#modeCopyCreateAsk}, {@link FileRemote#modeCopyReadOnlyAks}, {@link FileRemote#modeCopyExistAsk}. */
    int modeCopyOper; 
    
    /**Set one time after receive event to skip in {@link Copy.StateCopyAsk}. 
     * Reset if used in the next state transition of the following {@link Copy.StateCopyFileContent}. */
    boolean bAbortDirectory;
    
    /**Set one time after receive event to overwrite in {@link Copy.StateCopyAsk}. 
     * Reset if used in the next state transition of the following {@link Copy.StateCopyFileContent}. */
    boolean bOverwrfile;
    
     long timestart;
    
    //boolean bAbortFile, bAbortDir, bAbortAll;
    
    /**Opened file for read to copy or null. */
    FileInputStream in = null;
    
    /**Opened file for write to copy or null. */
    FileOutputStream out = null;
    
    /**The number of bytes which are copied yet in a copy process to calculate how many percent is copied. */
    int zBytesCopyFile;
    
    /**The number of bytes of a copying file to calculate how many percent is copied. */
    long zBytesFile;
    
    
    int zFilesCheck, zFilesCopy;
    
    long zBytesCheck, zBytesCopy;
    
    /**Buffer for copy. It is allocated static. 
     * Only used in this thread {@link FileRemoteAccessorLocalFile#runCommissions}. 
     * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
     * at least the size of 1 record of the file system. */
    byte[] buffer = new byte[0x100000];  //1 MByte 16 kByte buffer
    
    
    boolean bCopyFinished;
    
   /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}
     * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}. */
    private final List<File> listCopyFiles = new LinkedList<File>();
    
    private final ConcurrentLinkedQueue<Event> storedCopyEvents = new ConcurrentLinkedQueue<Event>();
    
    private final Stack<DataSetCopy1Recurs> recursDirs = new Stack<DataSetCopy1Recurs>();
    
    private DataSetCopy1Recurs actData;
    
    
    private File currentFile;

    private int ctWorkingId = 0;

    private int checkId;
    
    
    Copy(FileRemoteAccessorLocalFile accessor){
      this.outer = accessor;
      evCpy = new EventCpy(accessor);
    }
    
    /**Creates a new entry for the file deepnes stack.
     * @param src
     * @param dst
     */
    void newDataSetCopy(File src, File dst){
      if(actData !=null){ recursDirs.push(actData); }
      actData = new DataSetCopy1Recurs();
      actData.src = src;
      actData.dst = dst;
      ///
    }
    

    
    /**Cmd check
     * @param ev
     */
    void checkCopy(FileRemote.CmdEvent ev){
      this.currentFile= ev.filesrc;
      this.modeCopyOper = ev.modeCopyOper;
      listCopyFiles.clear();
      FileRemote.CallbackEvent evback = ev.getOpponent();
      if(currentFile.isDirectory()){ 
        zBytesCheck = 0;
        zFilesCheck = 0;
        checkDir(currentFile, 1);
      } else {
        listCopyFiles.add(currentFile);
        zBytesCheck = ev.filesrc.length();
        zFilesCheck = 1;
      }
      
      evback.occupy(outer.evSrc, true);
      evback.data1 = checkId = ++ctWorkingId;
      evback.nrofBytesAll = (int)zBytesCheck;  //number between 0...1000
      evback.nrofFiles = zFilesCheck;  //number between 0...1000
      evback.sendEvent(FileRemote.CallbackCmd.doneCheck);
    }
    
    
    
    /**subroutine for {@link #checkCopy(CmdEvent)}
     * @param dir
     * @param recursion
     */
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

    
    
    private void sendEv(CmdCpyIntern cmd){
      final EventCpy ev;
      if(evCpy.occupy(outer.evSrc, false)){
        ev = evCpy;
      } else {
        ev = evCpy.getOpponent();
        ev.occupy(outer.evSrc, true);
      }
      ev.sendEvent(cmd);
    }
    
    
    /**Prepares the callback event for ask anything.
     * @param cmd
     */
    void sendEventAsk(File pathShow, FileRemote.CallbackCmd cmd){
      Assert.check(evBackInfo !=null);
      if(evBackInfo.occupyRecall(1000, outer.evSrc, true)){
        String absPath = pathShow.getAbsolutePath();
        if(absPath.length() > evBackInfo.fileName.length-1){
          absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
        }
        StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
          evBackInfo.sendEvent(cmd);
      } else {
        Assert.checkMsg (false, null);
      }
          
    }
    
    
    
    
    private void execMove(FileRemote.CmdEvent co){
      timestart = System.currentTimeMillis();
      FileRemote.CallbackCmd cmd;
      FileRemote.CallbackEvent evback = co.getOpponent();
      if(co.filesrc.renameTo(co.filedst)){
        cmd = FileRemote.CallbackCmd.done ; 
      } else {
        cmd = FileRemote.CallbackCmd.error ; 
      }
      System.out.println("FileRemoteAccessorLocalFile - move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
      evback.occupy(outer.evSrc, true);
      evback.sendEvent(cmd);
    }

    
    /**Executes copy for 1 file or 1 segment of file. State-controlled. 
     * <pre>
     *                                         ?src.isFile-----~cmdFilePart----------->(CopyFilePart)
     * (Init)---cmdCopy--->(Copy)---cmdDir-----?src.isDirectory]---+
     *                        |<------~cmdDir----------------------+
     *                        |
     *                        |-----cmdFilePart-----
     * </pre>
     * <ul>
     * <li>State {@link #stateCopyReady}: Ready for opeation any copy action.
     *   <ul>
     *   <li>ev ev {@link FileRemote#cmdCopy}: ~ev {@link #cmdCopyDirFile} --> {@link #stateCopyDirFile}
     *     with that: start copying any directory or file.
     *   </ul>
     * <li>State {@link #stateCopyDirFile}: The state where a directory is analyzed to copy.
     *   <ul>
     *   <li>ev {@link FileRemote#cmdCopy}: store in {@link #storedCopyEvents}, --> {@link #stateCopyDirFile}
     *     Therewith new incomming cmdCopy will be stored, not ignored, but not executed yet.
     *   <li>ev {@value #cmdCopyDirFile}:  
     *     check whether it is a dir or file:
     *     <ul>
     *     <li>A dir: create entry in {@link #recursDirs}; get {@link File#listFiles()}; ~{@link #cmdCopyDirFile}
     *         --> {@link #stateCopyDirFile}
     *         <br>It is a
     *     <li>A file: open it, create dst, save in {@link DataSetCopy1Recurs#in} and out.
     *       <ul> 
     *       <li> If exeception on open: callback({@link FileRemote#acknErrorOpen}, --> {@link #stateCopyDirFile}.
     *         It waits for any response. If response did not come, the rest of copy of other files are waiting forever.
     *         The user is informed about this state.
     *       <li> If the open and creation is successfull, ~ {@link #cmdCopyFilePart}, --> {@link #stateCopyFileContent}  
     *       </ul>
     *     </ul>
     *   </ul>
     * <li>State {@link #stateCopyFileContent}
     *   <ul>
     *   <li>ev {@link #cmdCopyFilePart}: copy parts for 300 ms or max for that file.
     *     <ul>
     *     <li>If the file is finished:
     *     <li>If the file is not finished:
     *     </ul>
     *   </ul>  
     * <li>State {@link #stateCopyFileFinished}: Check continue:
     *   <pre>
     *    boolean bCont = false;
          do{
          if(entry){
            if(++entry.isSrc > listSrc.length(){
              remove entry;
              bCont = true;
            } else {
              ~cmdCopyDirFile(listSrc[ixSrc]); 
          } else {
            ~cmdFinish
          } while(bCont);
     *   </pre>
     *      
     * </ul>                       
     * */
    void XXXtrans_CopyState(Event ev){
    }
    
    
    
    
    
    
    
    StateCopyTop stateCopy = new StateCopyTop();
    
    private class StateCopyTop extends StateTopBase<StateCopyTop>{

      StateCopyReady stateReady = new StateCopyReady(this);
      //StateCopyStart start = new StateCopyStart(this);
      StateCopyProcess stateProcess = new StateCopyProcess(this);

      protected StateCopyTop() {
        super("StateCopyTop");
      }

      @Override public int entryDefault(){
        return stateReady.entry(eventNotConsumed);
      }
    
      @Override public int entry(int consumed){
        stateCopy.stateReady.entry(0);
        return consumed;
      }

    }

    
    private class StateCopyReady extends StateSimpleBase<StateCopyTop>{
      
      
      StateCopyReady(StateCopyTop superState) { super(superState, "Ready"); }

      //A(MainState enclosingState){ super(enclosingState); }
    
      @Override public int trans(Event evP) {
        if(evP instanceof FileRemote.CmdEvent){
          FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
          FileRemote.Cmd cmd = ev.getCmd();
          if(cmd == FileRemote.Cmd.copy){
            //gets and store data from the event:
            Copy.this.modeCopyOper = ev.modeCopyOper;
            newDataSetCopy(ev.filesrc, ev.filedst);
            evBackInfo = ev.getOpponent();
            StateCopyTop exitState = exit();
            int cont = exitState.stateProcess.entry(mEventConsumed);
            timestart = System.currentTimeMillis();
            return exitState.stateProcess.stateCopyDirOrFile.entry(cont);
          }
          else {
            return eventNotConsumed;
          }
        }
        else {
          return eventNotConsumed;
        }
      }
    }

        
    private class StateCopyProcess extends StateCompositeBase<StateCopyProcess, StateCopyTop>{

      StateCopyDirOrFile stateCopyDirOrFile = new StateCopyDirOrFile(this);
      StateCopySubDir stateCopySubdir = new StateCopySubDir(this);
      StateCopyFileContent stateCopyFileContent = new StateCopyFileContent(this);
      StateCopyFileFinished stateCopyFileFinished = new StateCopyFileFinished(this);
      StateCopyAsk stateCopyAsk = new StateCopyAsk(this);

      protected StateCopyProcess(StateCopyTop superState) { super(superState, "Process"); }

      @Override public int entryDefault(){
        return stateCopyDirOrFile.entry(eventNotConsumed);
      }
    

      @Override public int trans(Event evP){
        PrepareEventCmd ev = new PrepareEventCmd(evP);
        if(ev.cmde == FileRemote.Cmd.copy){
          ev.eve.donotRelinquish();
          storedCopyEvents.add(ev.eve);  //save it, execute later if that cmdCopy is finished.
          return 0;
        }
        else if(ev.cmde == FileRemote.Cmd.abortAll){
          
          storedCopyEvents.clear();
          return exit().stateReady.entry(StateSimpleBase.mEventConsumed);
        } else {
          return 0;
        }
      }

    }

    
    
    /**Prepare copying the given file or directory.
     * {@link #actData} is set with {@link DataSetCopy1Recurs#src} and {@link DataSetCopy1Recurs#dst}.
     * The {@link DataSetCopy1Recurs#listSrc} is not prepared yet.
     * This state checks whether it is a file or directory. 
     * <ul>
     * <li>If it is a file, the file src and dst files will be opened.
     *   <ul>
     *   <li>If it is succeed, an self-event {@link #cmdCopyFile} was sent and the state {@link EStateCopyProcess#FileContent} 
     *     is entered immediately. In this state
     *   <li>If the open fails, a callback is sent and the the state {@link EStateCopyProcess#Ask} entered immediately.
     *   </ul>
     * <li>If it is a directory, first a backevent is sent which informs about the directory path.
     *   Then the list of files are gotten. This may spend some time.
     *   If it is ready, the first file of the list is processed. To do that, a new instance of {@link DataSetCopy1Recurs}
     *   is created and stored as {@link #actData}.
     *   An event {@link #cmdCopyDirFile} is sent to this itself and the state remains. It is the recursion in the directory.
     *   <br>
     *   It means that the program flow returns to the event queue (inversion of control). Therefore a abort or skip event
     *   have a chance to processing.
     * <li>      
     * @param ev
     * @return
     */
    private class StateCopyDirOrFile extends StateSimpleBase<StateCopyProcess>{
      
      
      StateCopyDirOrFile(StateCopyProcess superState) { super(superState, "DirOrFile"); }

      //A(MainState enclosingState){ super(enclosingState); }
    
      @Override public int entry(int isConsumed){
        return super.entry(isConsumed | StateSimpleBase.mRunToComplete);
      }

      
      /**This state processes a new {@link DataSetCopy1Recurs} stored in {@link #actData}.
       * It branches to the necessary next state:
       * <ul>
       * <li>(())-----[src.isDirectory()]---->{@link Copy.StateCopySubDir}
       * <li>(())-----/open, create Files --->{@link Copy.StateCopyFileContent}
       *   <ul>
       *   <li>? exception ----->{@link Copy.StateCopyAsk}
       *   </ul>                   
       * </ul>
       * @param ev
       * @return
       */
      @Override public int trans(Event ev) {
        //EventCpy ev = evP;
        if(actData.src.isDirectory()){
          return exit().stateCopySubdir.entry(mEventConsumed);  //exit and entry in the same state.
        } else {
          //It is a file. try to open/create
          StateCopyProcess exitState= exit();  //exit this state to tran to another.
          //
          if(actData.dst.exists()){
            if(bOverwrfile){  //The last event was "overwrite", therefore overwrite only this one file.
              bOverwrfile = false;
              boolean bOk = true;
              if(!actData.dst.canWrite()){
                bOk = actData.dst.setWritable(true);
              }
              if(!bOk){
                sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
                return exitState.stateCopyAsk.entry(eventNotConsumed); 
              }
            }
            else if((Copy.this.modeCopyOper & FileRemote.modeCopyExistMask) == FileRemote.modeCopyExistSkip){
              //generally don't overwrite existing files:
              return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
            }
            else if(actData.dst.canWrite()){
              //can overwrite, but check timestamp
              long timeSrc = actData.src.lastModified();
              long timeDst = actData.dst.lastModified();
              //The destination file exists and it is writeable. Check:
              switch(Copy.this.modeCopyOper & FileRemote.modeCopyExistMask){
                case FileRemote.modeCopyExistAsk: 
                  sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstOverwr );
                  return exitState.stateCopyAsk.entry(eventNotConsumed); 
                case FileRemote.modeCopyExistNewer: 
                  if( (timeSrc - timeDst) < 0){
                    return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
                  }  //else: do copy
                case FileRemote.modeCopyExistOlder: 
                  if( (timeSrc - timeDst) > 0){
                    return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
                  }  //else: do copy
              }
            } else {  //can not write, readonly
              //The destination file exists and it is readonly. Check:
              switch(Copy.this.modeCopyOper & FileRemote.modeCopyReadOnlyMask){
                case FileRemote.modeCopyReadOnlyAks: 
                  sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstReadonly );
                  return exitState.stateCopyAsk.entry(eventNotConsumed); 
                case FileRemote.modeCopyReadOnlyNever: 
                  return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
                case FileRemote.modeCopyReadOnlyOverwrite: {
                  boolean bOk = actData.dst.setWritable(true);
                  if(!bOk){
                    sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
                    return exitState.stateCopyAsk.entry(eventNotConsumed); 
                  }  //else now try to open to overwrite.
                } break;  
              }
              
            }
          }
          Copy.this.zBytesFile = actData.src.length();
          Copy.this.zBytesCopyFile = 0;
          try{ 
            FileSystem.mkDirPath(actData.dst);
            Copy.this.out = new FileOutputStream(actData.dst);
          } catch(IOException exc){
            sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorDstCreate );
            return exitState.stateCopyAsk.entry(eventNotConsumed);
          }

          try{ Copy.this.in = new FileInputStream(actData.src);
          } catch(IOException exc){
            sendEventAsk(actData.src, FileRemote.CallbackCmd.askErrorSrcOpen );
            return exitState.stateCopyAsk.entry(eventNotConsumed);
          }
          return exitState.stateCopyFileContent.entry(eventNotConsumed);
        }
      }
    }


    
    private class StateCopySubDir extends StateSimpleBase<StateCopyProcess>{
      
      
      StateCopySubDir(StateCopyProcess superState) { super(superState, "SubDir"); }

      @Override public int entry(int consumed){
        super.entry(consumed);
        //onentry action
        //first send a callback
        if(evBackInfo.occupyRecall(outer.evSrc, false)){
          String absPath = actData.src.getAbsolutePath();
          if(absPath.length() > evBackInfo.fileName.length-1){
            absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
          }
          StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
          evBackInfo.sendEvent(FileRemote.CallbackCmd.copyDir);
        }
        //This action may need some time.
        actData.dst.mkdirs();
        actData.listSrc = actData.src.listFiles();
        actData.ixSrc = 0;
        if(actData.listSrc.length ==0){
          //an empty directory:
          sendEv(CmdCpyIntern.emptyDir);
        } else {
          File srcInDir = actData.listSrc[0];
          FileRemote dstDir = FileRemote.fromFile(actData.dst);
          //
          //use the first entry of the dir:
          //
          recursDirs.push(actData);
          actData = new DataSetCopy1Recurs();
          actData.src = srcInDir;
          actData.dst = new FileRemote(dstDir, srcInDir.getName());
          sendEv(CmdCpyIntern.subDir);
        }
        return consumed;  //return to the queue

      }
      
      /**
       * <ul>
       * <li>(())---{@link CmdCpyIntern#subDir}----->{@link #entry_CopyDirFile(EventCpy)}
       * </ul>
       * @param ev
       * @return
       */
      @Override public int trans(Event ev) {
        if(ev.getCmd() == CmdCpyIntern.subDir){
          return exit().stateCopyDirOrFile.entry(mEventConsumed);  //exit and entry in the same state.
        } else if(ev.getCmd() == CmdCpyIntern.emptyDir){ 
          return exit().stateCopyFileFinished.entry(mEventConsumed);  //exit and entry in the same state.
        } else {
          return eventNotConsumed;
        }
      }
        
    }


    private class StateCopyFileContent extends StateSimpleBase<StateCopyProcess>{
      
      
      StateCopyFileContent(StateCopyProcess superState) { super(superState, "FileContent"); }

      @Override public int entry(int isConsumed){
        sendEv(CmdCpyIntern.file); 
        bCopyFinished = false;
        return super.entry(isConsumed);
      }
      
    
      /**Executes copying the file or a part of the file for 300 ms or abort copying the file.
       * <uL>
       * <li>If the event cmd {@link #cmdCopyFile} is received, the file is copied or a part of the file is copied
       *   till approximately 300 ms are exhausted. 
       *   <ul>
       *   <li>If the whole file is not processed  an event {@link #cmdCopyfile} is sent to itself
       *     to continue the copying in a new cycle of the state machine.   
       *   <li>If the file is processed, it will be closed and the state {@link #entry_CopyFileFinished(CmdEvent)}
       *     will be entered without condition.
       *   </ul>
       * <li>If the event cmd {@link FileRemote#cmdAbortFile} is gotten, the dst file will be closed and deleted.
       *   Then the {@link #entry_CopyFileFinished(CmdEvent)} will be entered unconditionally.
       * </ul>
       * @param ev
       * @return
       */
      @Override public int trans(Event evP) {
        PrepareEventCmd ev = new PrepareEventCmd(evP);
        int newState;   
        if(ev.cmdi == CmdCpyIntern.file){
          //boolean bContCopy;
          newState = -1;   //it set, then the loop should terminated
          do {
            try{
              int zBytes = Copy.this.in.read(buffer);
              if(zBytes > 0){
                Copy.this.zBytesCopyFile += zBytes;
                zBytesCopy += zBytes;
                Copy.this.out.write(buffer, 0, zBytes);
                //synchronized(this){ try{ wait(20);} catch(InterruptedException exc){} } //only test.
                long time = System.currentTimeMillis();
                //
                //feedback of progression after about 0.3 second. 
                if(time > timestart + 300){
                  timestart = time;
                  if(evBackInfo.occupyRecall(outer.evSrc, false)){
                      
                    evBackInfo.data1 = (int)((float)(Copy.this.zBytesCopyFile / Copy.this.zBytesFile) * 1000);  //number between 0...1000
                    if(zBytesCheck >0){
                      evBackInfo.data2 = (int)((float)(zBytesCopy / zBytesCheck) * 1000);  //number between 0...1000
                    } else {
                      evBackInfo.data2 = 0;
                    }
                    evBackInfo.nrofFiles = zFilesCheck - zFilesCopy;
                    evBackInfo.nrofBytesInFile = (int)zBytesCopy;
                    String absPath = actData.src.getAbsolutePath();
                    if(absPath.length() > evBackInfo.fileName.length-1){
                      absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
                    }
                    StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
                    evBackInfo.sendEvent(FileRemote.CallbackCmd.nrofFilesAndBytes );
                  }
                  sendEv(CmdCpyIntern.file); //keep alive the copy process.
                  newState = stateCompleted + mEventConsumed;
                } 
              } else if(zBytes == -1){
                //bContCopy = false;
                bCopyFinished = true;
                newState = exit().stateCopyFileFinished.entry(mEventConsumed);
                
              } else {
                //0 bytes ?
                //bContCopy = true;
                //newState = false;
              }
            } catch(IOException exc){
              //bContCopy = false;
              System.err.printf("FileRemoteAccessorLocalFile - Copy error; %s\n", exc.getMessage());
              sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorCopy );
              newState = exit().stateCopyAsk.entry(mEventConsumed);
            }
          }while(newState == -1);
          
        } else if(ev.cmde == FileRemote.Cmd.abortCopyFile ){
          
          newState = exit().stateCopyFileFinished.entry(mEventConsumed);
        } else {
          newState = eventNotConsumed;
        }
        return newState;
      }
      
      @Override public StateCopyProcess exit(){
        StateCopyProcess encl = super.exit();
        try{
          Copy.this.in.close();
          Copy.this.in = null;
          Copy.this.out.close();
          Copy.this.out = null;
          if(!bCopyFinished){
            if(!actData.dst.delete()) {
              System.err.println("FileRemoteAccessorLocalFile - Problem delete after abort; " + actData.dst.getAbsolutePath());
            }
          } else {
            long date = actData.src.lastModified();
            actData.dst.setLastModified(date);
          }
        } catch(IOException exc){
          System.err.printf("FileRemoteAccessorLocalFile - Problem close; %s\n", actData.dst.getAbsolutePath());
        }
        
        return encl;
      }
    }

    

    /**This state is activated after the copy process of one file was finished from {@link StateCopyFileContent}
     * or if the file was skipped (from {@link StateCopyDirOrFile}. 
     * <ul>
     * <li>entry: Checks whether there is a further file to copy in the same directory 
     *   or it returns to the parent directory and checks the further files there.
     *   If all files are copied the {@link Copy#actData} are set to null. 
     * <li>trans [actData==null] ==> {@link StateCopyReady}
     * <li>trans [else] ==> {@link StateCopyDirOrFile}
     * <br><br>
     * 
     * @author Hartmut
     *
     */
    private class StateCopyFileFinished extends StateSimpleBase<StateCopyProcess>{
      
      StateCopyFileFinished(StateCopyProcess superState) { super(superState, "FileFinished"); }

      @Override public int entry(int isConsumed){
        super.entry(isConsumed);
        //stateCopyProcess = EStateCopyProcess.FileFinished;
        boolean bCont;
        //close currently file if this state is entered from stateAsk. The regular close() is executed on exit of stateCopyFile.
        if(Copy.this.in !=null){
          try{ Copy.this.in.close();
          } catch(IOException exc){
            System.err.printf("FileRemoteAccessorLocalFile -Copy close src while abort, unexpected error; %s\n", exc.getMessage());
          }
          Copy.this.in = null;
        }
        if(Copy.this.out !=null){
          try{ Copy.this.out.close();
          } catch(IOException exc){
            System.err.printf("FileRemoteAccessorLocalFile -Copy close dst while abort, unexpected error; %s\n", exc.getMessage());
          }
          Copy.this.out = null;
        }
        do{
          if(recursDirs.empty()) {
            actData = null;
            bCont = false;
          } else {
            actData = recursDirs.pop();
            if(actData.listSrc !=null //copy a directory tree?
              && ++actData.ixSrc < actData.listSrc.length
              ){
              File src = actData.listSrc[actData.ixSrc];
              FileRemote dst = new FileRemote(FileRemote.fromFile(actData.dst), src.getName());
              newDataSetCopy(src, dst);
              //sendEv(CmdCpyIntern.dirFile);
              bCont = false;
            } else {
              bCont = true;
            }
          }
        }while(bCont);
        return isConsumed | StateSimpleBase.mRunToComplete;
      }
      
      
    
      @Override public int trans(Event evP) {
        //EventCpy ev = (EventCpy)evP;
        /*
        if(ev.getCmd() == CmdCpyIntern.dirFile){
          return exit().dirOrFile.entry(consumed);
        }
        else*/ 
        if(actData == null){
          //send done Back
          if(evBackInfo.occupyRecall(1000, outer.evSrc, false)){
            evBackInfo.sendEvent(FileRemote.CallbackCmd.done);
            ///
            Event ev1;
            while( (ev1 = storedCopyEvents.poll() ) !=null) {
              ev1.sendEventAgain();
            }
            
          }
          return exit().exit().stateReady.entry(eventNotConsumed);
        }
        else {
          //another file or directory
          return exit().stateCopyDirOrFile.entry(eventNotConsumed);
        }
      }
    }


    private class StateCopyAsk extends StateSimpleBase<StateCopyProcess>{
      
      
      StateCopyAsk(StateCopyProcess superState) { super(superState, "Ask"); }

      @Override public int entry(int isConsumed){
        //onyl to set a breakpoint.
        return super.entry(isConsumed);
      }
      
      
      @Override public int trans(Event evP) {
        if(evP ==null){ 
          return 0;
        } else {
          FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
          FileRemote.Cmd cmd = ev.getCmd();
          //
          if(cmd == FileRemote.Cmd.overwr){
            Copy.this.modeCopyOper = ev.modeCopyOper;
            bOverwrfile = true;
            ///actData.dst.setWritable(true);
            return exit().stateCopyDirOrFile.entry(mEventConsumed);
          } 
          else if(cmd == FileRemote.Cmd.abortCopyFile){   //it is the skip file.
            Copy.this.modeCopyOper = ev.modeCopyOper;
            return exit().stateCopyFileFinished.entry(mEventConsumed);
          } 
          else if(cmd == FileRemote.Cmd.abortCopyDir){
            Copy.this.modeCopyOper = ev.modeCopyOper;
            bAbortDirectory = true;
            return exit().stateCopyFileContent.entry(mEventConsumed);
          } 
          else if(cmd == FileRemote.Cmd.abortAll){
            Copy.this.storedCopyEvents.clear();   //abort all other files too.
            return exit().exit().stateReady.entry(mEventConsumed);
            
          }else{
            return eventNotConsumed;
          }
        }
      }
    }
    


  }  
  

  
  /**A thread which gets all file properties independent of a caller of the #re
   */
  private class RunRefresh implements Runnable{
    final FileRemote fileRemote;
    
    final FileRemote.CallbackEvent callback;
    
    RunRefresh(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
      this.fileRemote= fileRemote;
      this.callback = callback;
    }
    
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
        callback.occupy(evSrc, true);
        callback.sendEvent(FileRemote.CallbackCmd.done);
      }
    }
    
    
  }
  
  
  /**A thread which gets all file properties inclusive children independent of a caller of the #re
   */
  private class RunRefreshWithChildren implements Runnable{
    long time;
    
    final FileRemote fileRemote;
    
    final FileRemote.CallbackEvent callback;
    
    RunRefreshWithChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
      this.fileRemote= fileRemote;
      this.callback = callback;
    }
    
    public void run(){
      try{
        refreshFileProperties(fileRemote, null);
        File fileLocal = getLocalFile(fileRemote);
        //fileRemote.flags |= FileRemote.mChildrenGotten;
        if(fileLocal.exists()){
          long time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - start listFiles; dt=" + (time1 - time));
          
          File[] files = fileLocal.listFiles();
          time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - ok listFiles; dt=" + (time1 - time));
          if(files !=null){
            if(useFileChildren){
              fileRemote.children = files;
            } else {
              fileRemote.children = new FileRemote[files.length];
              int iFile = -1;
              for(File file1: files){
                fileRemote.children[++iFile] = newFile(file1, fileRemote);
              }
            }
          }
        }
        if(callback !=null){
          callback.occupy(evSrc, true);
          long time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - callback listFiles; dt=" + (time1 - time));
          callback.sendEvent(FileRemote.CallbackCmd.done);
          time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - finish listFiles; dt=" + (time1 - time));
        }
        fileRemote.flags &= ~FileRemote.mThreadIsRunning;
      }
      catch(Exception exc){
        System.err.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - Thread Excpetion;" + exc.getMessage());
      }
    }
  }
    

  
  
  
  public static FileRemote.FileRemoteAccessorSelector selectLocalFileAlways = new FileRemote.FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileRemoteAccessorLocalFile.getInstance();
    }
  };
  
}
