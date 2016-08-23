package org.vishia.util;


public interface SortedTreeWalkerCallback<Type>
{
  /**It is similar {@link java.nio.file.FileVisitResult}.
   * This class is defined here because it runs with Java-6 too.
   */
  public enum Result
  {
    cont
    , terminate
    , skipSiblings
    , skipSubtree
  }

  
  /**This class contains the number of files etc. for callback.
   * @author hartmut
   *
   */
  public static class Counters
  { public long nrofBytes;
    /* @param nrofBytes total number of bytes of all files in this directory without sub directories
    /* @param nrofDirs number of found sub directories in this directory. 
    /* @param nrofFiles number of regular files  in this directory.*/
    public int nrofDirs, nrofFiles;
  
    public int nrofDirSelected, nrofFilesSelected;
  }
  
  /**Invoked before start of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
   * or an adequate communication.
   */
  void start(Type startDir);
  
  /**Invoked on start on walking through a directory. If this method is invoked for a sub directory, the  {@link #offerFile(Type)} is not invoked.
   * @param dir the directory
   * @return information to abort, maybe boolean.
   */
  Result offerDir(Type dir);
  
  /**Invoked on end of walking through a directory.
   * It is invoked in the thread which executes a Type action.
   * It is possible to create an event and store it in a queue but there are necessary some more events
   * it may not be good.
   * @param file
   * @param cnt The number of byted, dirs, files selected and total of this directory.
   */
  Result finishedDir(Type dir, Counters cnt);
  
  /**Invoked for any file, invoked for a sub directory only if the depth is reached and {@link #offerDir(Type)} is not called.
   * @param file
   * @return information to abort or continue.
   */
  Result offerFile(Type file);
  
  /**Invoked after finishing a {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
   */
  void finished(long nrofBytes, int nrofFiles);
  
  
  /**Returns true if the file tree walking should be terminated respectively aborted.
   * This routine should be called in any walk tree routine.
   * The aborted state should be set from outside in the implementation classes.
   * @return true if should terminated.
   */
  boolean shouldAborted();

  
  
  /**Use this as template for anonymous implementation. Frequently without 'static'.*/
  static SortedTreeWalkerCallback<Object> callbackTemplate = new SortedTreeWalkerCallback<Object>()
  {

    @Override public void start(Object startDir) {  }
    @Override public void finished(long nrofBytes, int nrofFiles) {  }

    @Override public Result offerDir(Object file) {
      return Result.cont;      
    }
    
    @Override public Result finishedDir(Object file, SortedTreeWalkerCallback.Counters cnt) {
      return Result.cont;      
    }
    
    

    @Override public Result offerFile(Object file) {
      return Result.cont;
    }

    
    @Override public boolean shouldAborted(){
      return false;
    }
    
  };
  
  

  
}
