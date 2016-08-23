package org.vishia.fileRemote;

import org.vishia.util.Assert;
import org.vishia.util.FileRemote;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;

/**This class combines some {@link FileRemote} instances for common usage.
 * It ensures that the same FileRemote object is used for the same string given path.
 * @author Hartmut Schorrig
 *
 */
public class FileCluster
{
  /**Version, history and license.
   * <ul>
   * <li>2013-04-22 Hartmut created. The base idea was: Select files and use this selection in another part of application.
   *   It should be ensured that the same instance is used for the selection and other usage.
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
  public static final int version = 20130501;
  
  /**This index contains the association between paths and its FileRemote instances.
   */
  IndexMultiTable<String, FileRemote> idxPaths = new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);
  

  /**Number of selected bytes in all selected files. */
  long[] selectBytes = new long[2];
  
  /**Number of selected files. */
  int[] selectFiles = new int[2];
  
  
  /**The directory where the selection should be done.
   * 
   */
  FileRemote dirBaseOfSelection;

  
  public FileCluster(){
  }

  
  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote get( final String sPath){
   return(get(sPath, null));
  }

  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote get( final String sDirP, final String sName){
    CharSequence sDir1 = FileSystem.normalizePath(sDirP); //sPath.replace('\\', '/');
    StringBuilder uPath = sDir1 instanceof StringBuilder ? (StringBuilder)sDir1: new StringBuilder(sDir1);
    if(uPath.charAt(uPath.length()-1) !='/'){ uPath.append('/'); }
    if(sName !=null) { uPath.append(sName); }
    String sPath = uPath.toString();
    FileRemote ret = idxPaths.search(sPath);
    if(ret == null){
      ret = new FileRemote(this, null, null, sDirP, sName, 0, 0, 0, null, true);
      idxPaths.put(sPath, ret);
    } else {
      String sPathRet = ret.getAbsolutePath();
      int zPathRet = sPathRet.length();
      if(sPathRet.equals(sPath)){
        return ret;
      } else if(sPath.startsWith(sPathRet)
          && sPath.charAt(zPathRet) == '/'){
        int posSep1 = zPathRet;
        //a directory of the file was found.
        do{
          int posSep2 = sPath.indexOf('/', posSep1+1);
          if(posSep2 <0){
            String sNameRet = sPath.substring(posSep1+1);
            if(sNameRet.length() ==0) return ret;
            else {
              return ret.child(sNameRet);
            }
          } else {
            String sDirRet = sPath.substring(posSep1+1, posSep2);
            ret = ret.child(sDirRet);
            posSep1 = posSep2;
          }
        } while(true);  //returns inside.
      } else {
        //another directory
        ret = new FileRemote(this, null, null, sDirP, sName, 0, 0, 0, null, true);
        idxPaths.put(sPath, ret);
      }
    }
    return ret;
  }

  
  
  
}
