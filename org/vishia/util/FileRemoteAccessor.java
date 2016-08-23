package org.vishia.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.vishia.fileLocalAccessor.FileRemoteAccessorLocalFile;
import org.vishia.util.FileAccessZip.FileZipData;

/**Interface for instances, which organizes a remote access to files.
 * One instance per transfer protocol are need.
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class FileRemoteAccessor implements Closeable
{
  /**Version, history and license.
   * <ul>
   * <li>2012-09-12 Hartmut bugfix: {@link #getChildren(FileRemote, FileFilter)} here only abstract.
   * <li>2012-08-12 Hartmut new: {@link #openInputStream(FileRemote, long)}
   * <li>2012-08-12 Hartmut new: {@link #getChildren(FileRemote, FileFilter)} implemented here.
   * <li>2012-08-12 Hartmut chg: up to now this is not an interface but an abstract class. It contains common method implementation.
   *   An derivation (or implementation of the interface before that change) may not need other base classes.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: {@link Commission#newDate} etc. 
   *   for {@link FileRemote#chgProps(String, int, int, long, org.vishia.util.FileRemote.CallbackEvent)}.
   * <li>2012-01-09 Hartmut new: This class extends from Closeable, because an implementation 
   *  may have an running thread which is need to close. A device should be closeable any time.
   * <li>2012-01-06 Hartmut new {@link #refreshFileProperties(FileRemote)}. 
   * <li>2011-12-31 Hartmut new {@link Commission} and {@link #addCommission(Commission)}. It is used
   *   to add commissions to the implementation class to do in another thread/via communication.
   * <li>2011-12-10 Hartmut creation: Firstly only the {@link FileRemoteAccessorLocalFile} is written.
   *   
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
  public static final int version = 20120310;
  
  //public final static int kOperation = 0xd00000, kFinishOk = 0xf10000, kFinishNok = 0xf10001
  //, kFinishError = 0xf1e3303, kNrofFilesAndBytes = 0xd00001, kCopyDir = 0xd0cd13;

  
  /**Gets the properties of the file from the physical file.
   * @param file the destination file object.
   * @param callback If null then the method waits for response from the maybe remote file system
   *   with a suitable timeout. 
   *   If not null then the method may return immediately without any waiting
   *   and the callback method in the {@link Event#callback()} is invoked maybe in another thread
   *   if the answer is gotten. 
   */
  public abstract void refreshFileProperties(FileRemote file, FileRemote.CallbackEvent callback);

  /**Gets the properties and the children of the file from the physical file.
   * @param file the destination file object.
   * @param callback If null then the method waits for response from the maybe remote file system
   *   with a suitable timeout. 
   *   If not null then the method may return immediately without any waiting
   *   and the callback method in the {@link Event#callback()} is invoked maybe in another thread
   *   if the answer is gotten. 
   */
  public abstract void refreshFilePropertiesAndChildren(FileRemote file, FileRemote.CallbackEvent callback);

  
  public abstract List<File> getChildren(FileRemote file, FileFilter filter);
  
  

  /**Try to delete the file.
   * @param callback
   * @return If the callback is null, the method returns if the file is deleted or it can't be deleted.
   *   The it returns true if the file is deleted successfully. If the callback is not null, it returns true.
   */
  public abstract boolean delete(FileRemote file, FileRemote.CallbackEvent callback);
  
  public abstract ReadableByteChannel openRead(FileRemote file, long passPhase);
  
  public abstract InputStream openInputStream(FileRemote file, long passPhase);
  
  public abstract WritableByteChannel openWrite(FileRemote file, long passPhase);
 
  //FileRemote[] listFiles(FileRemote parent);
  
  /**Creates or prepares a CmdEvent to send to the correct destination. The event is ready to use but not . */
  public abstract FileRemote.CmdEvent prepareCmdEvent(FileRemote.CallbackEvent evBack);

  
  
  public abstract boolean isLocalFileSystem();

  
  /**The file object is a java.io.File for the local file system. If it is a remote file system,
   * the file object may be a instance for communication with the remote file system.
   * @param file The description of the file.
   * @return Any object.
   */
  //Object createFileObject(FileRemote file);
  
  
  
  public class XXXCommission
  {
    public final static int kCheckFile = 0xcecf1e, kCheck = 0xcec, kCopy = 0xc0b7, kDel = 0xde1ede
    , kMove = 0x307e, kChgProps = 0xc5a9e, kChgPropsRec = 0xc595ec
    , kCountLength = 0xc0311e39
    , kAbortFile = 0xab03df1e, kAbortDir = 0xab03dd13, kAbortAll = 0xab03da11;
    
    
    int cmd;
    
    FileRemote src, dst;
    
    FileRemote.CallbackEvent callBack;
    
    /**For {@link #kChgProps}: a new name. */
    String newName;
    
    /**For {@link #kChgProps}: new properties with bit designation see {@link FileRemote#flags}. 
     * maskFlags contains bits which properties should change, newFlags contains the value of that bit. */
    int maskFlags, newFlags;
    
    long newDate;
    
  }
  
  
  
  
}
