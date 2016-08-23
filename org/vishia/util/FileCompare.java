package org.vishia.util;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;



/**This class contains a functionality to compare file also in sub directories.
 * The result of comparison is presented in a tree.
 * @author Hartmut Schorrig
 *
 */
public class FileCompare
{
  
  
  
  final static int onlyTimestamp = 1;
  final static int content = 2;
  final static int withoutLineend = 4;
  final static int withoutEndlineComment = 8;
  final static int withoutComment = 16;
  
  
  final int mode;
  
  long minDiffTimestamp = 2000; 
  
  
  
  
  public FileCompare(int mode, long minDiffTimestamp)
  {
    this.mode = mode;
    this.minDiffTimestamp = minDiffTimestamp;
  }



  /**Class contains the comparison result for two files or sub directories in both trees.
   *
   */
  public static class Result
  {
    /**The left and the right file. If one of this is null, only the file at one side exists.
     */
    public final File file1, file2;
    
    public final String name;
    
    public final List<Result> subFiles;
    public boolean alone;
    public boolean equal;
    
    public boolean equalDaylightSaved;
    public boolean contentEqual;
    public boolean contentEqualWithoutEndline;
    
    public Result(File file1, File file2 )
    { this.file1 = file1;
      this.file2 = file2;
      this.name = file1 !=null ? file1.getName(): file2.getName();
      if(file1 !=null && file2 !=null && file1.isDirectory()){
        subFiles = new LinkedList<Result>();
      } else {
        subFiles = null;
      }
    }
  
    @Override public String toString(){ return name; }
    
  }
  
  
  
  /**Compares two directory trees. This method will be called recursively for all sub directories
   * which are found on both sides.
   * @param list1 List for result for dir1
   * @param list2 list for result for dir2
   * @param dir1 A directory
   * @param dir2 The second directory
   * @param sExclude Exclude filter for files (TODO)
   */
  void compare(List<Result> list, File dir1, File dir2, String[] sExclude)
  {
    File[] files1 = dir1.listFiles();
    File[] files2 = dir2.listFiles();
    //fill all files sorted by name in the idx:
    Map<String, File> idxFiles1 = new TreeMap<String, File>();
    Map<String, File> idxFiles2 = new TreeMap<String, File>();
    for(File file: files1){ 
      String name = file.isDirectory() ? ":" + file.getName() : file.getName();
      idxFiles1.put(name, file); 
    }
    for(File file: files2){ 
      String name = file.isDirectory() ? ":" + file.getName() : file.getName();
      idxFiles2.put(name, file); 
    }
    //
    //iterate over files
    Set<Map.Entry<String, File>> setFiles1 = idxFiles1.entrySet();
    Set<Map.Entry<String, File>> setFiles2 = idxFiles2.entrySet();
    Iterator<Map.Entry<String, File>> iter2 = setFiles2.iterator();
    Map.Entry<String, File> entry2 = null;
    String name1;
    String name2 = null;
    File file1, file2 = null;
    for(Map.Entry<String, File> entry1: setFiles1){
      if(entry2 == null) {  //get next entry  
        entry2 = iter2.hasNext() ? iter2.next() : null;
        if(entry2 !=null)
        { name2 = entry2.getKey();
          file2 = entry2.getValue();
        }
      }
      name1 = entry1.getKey();
      file1 = entry1.getValue();
      if(entry2 != null && name1.equals(name2)){
        final Result resEntry;
        resEntry = new Result(file1, file2);
        if(name1.startsWith(":")){
            //a directory
          compare(resEntry.subFiles, file1, file2, sExclude);
        } else {
          //the same file names, compare it:
          compareFile(resEntry);
        }
        list.add(resEntry);
        entry2 = null;    //use next
      } else if( entry2 != null && name1.compareTo(name2) >0){
        //file2 has no presentation at left
        Result resEntry = new Result(null, file2);
        resEntry.alone = true;
        list.add(resEntry);
        entry2 = null;  //use next
      } else {
        //file1 has no presentation at right
        Result resEntry = new Result(file1, null);
        resEntry.alone = true;
        list.add(resEntry);
      }
    }
    do{
      if(entry2 == null) {  //get next entry  
        entry2 = iter2.hasNext() ? iter2.next() : null;
      }
      if(entry2 !=null){
        name2 = entry2.getKey();
        file2 = entry2.getValue();
        //file2 has no presentation at left
        Result resEntry = new Result(null, file2);
        resEntry.alone = true;
        list.add(resEntry);
        entry2 = null;  //use next
      }
    } while(iter2.hasNext());
  }
  
  
  
  
  /**Compare two files.
   * @param file1
   * @param file2
   */
  void compareFile(Result file)
  {
    if(mode == onlyTimestamp){
      long date1 = file.file1.lastModified();
      long date2 = file.file2.lastModified();
      if(Math.abs(date1 - date2) < minDiffTimestamp){
        file.equal = true;
      } else if( Math.abs(date1 - date2 + 3600000) < minDiffTimestamp
              || Math.abs(date1 - date2 - 3600000) < minDiffTimestamp){ 
        file.equalDaylightSaved = true;
      }  
    }
  }
  
  
  
  void reportResult(PrintStream out, List<Result> list)
  {
    boolean bWriteDir = false;
    for(Result entry: list){
      if(entry.contentEqual){
        
      } else if(entry.alone && entry.file1 !=null){
        out.append("left         ; ").append(entry.name).append("\n");
      } else if(entry.alone && entry.file2 !=null){
        out.append("       right ; ").append(entry.name).append("\n");
      } else { 
        if(!entry.alone && entry.subFiles !=null){
          reportResult(out, entry.subFiles);
        } else {
          if(!bWriteDir){ bWriteDir = writeDir(out, entry); }
          out.append("     ??      ; ").append(entry.name).append("\n");
        }
      }
    }
    
  }
  
  
  
  boolean writeDir(PrintStream out, Result entry)
  {
    out.append("=========================================").append("\n");
    out.append(entry.file1.getAbsolutePath()).append("  ==  ").append(entry.file2.getAbsolutePath()).append("\n");
    return true;
  }
  
  
  public static void main(String[] args)
  {
    List<FileCompare.Result> list = new LinkedList<FileCompare.Result>();
    File dir1 = new File(args[0]);
    File dir2 = new File(args[1]);
    FileCompare main = new FileCompare(FileCompare.onlyTimestamp, 2000);
    main.compare(list, dir1, dir2, null);
    main.reportResult(System.out, list);
  }
  
}
