package org.vishia.cmd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.cmd.CmdStore.CmdBlock;
//import org.vishia.mainCmd.MainCmd_ifc;
//import org.vishia.mainCmd.Report;

/**This class stores some prepared commands for execution and executes it one after another.
 * The commands can contain placeholder for files.
 * @author hartmut Schorrig
 *
 */
public class CmdQueue implements Closeable
{
  
  /**Version, history and license:
   * <ul>
   * <li>2011-12-03 Hartmut chg: The {@link #pendingCmds}-queue contains {@link PendingCmd} instead
   *   {@link CmdStore.CmdBlock} now. It is one level near on the really execution. A queued
   *   command may not stored in a CmdBlock. 
   * <li>2011-11-17 Hartmut new {@link #close()} to stop threads.
   * <li>2011-07-00 created
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
  public static int version = 20120609;
  
  private static class PendingCmd implements CmdGetFileArgs_ifc
  {
    //final CmdStore.CmdBlock cmdBlock;
    //public final List<PrepareCmd> listCmds;
    public final PrepareCmd cmd;
    final File[] files;
    File currentDir;
    
    /**Constructs a cmd which is added to a queue to execute in another thread.
     * @param cmd The prepared cmd
     * @param files Some files which may be used by the command.
     *   Note that a reference to another {@link CmdGetFileArgs_ifc} can't be used
     *   if that selection is valid only on calling time. Therefore the yet selected files
     *   should be referenced.CmdGetFileArgs_ifc
     * @param currentDir
     */
    public PendingCmd(PrepareCmd cmd, File[] files, File currentDir)
    { this.cmd = cmd;
      this.files = files;
      this.currentDir = currentDir;
    }

    @Override public void  prepareFileSelection()
    { }

    @Override public File getFileSelect()
    { return files[0];
    }
    
    @Override public File getFile1() { return files[0]; }
    
    @Override public File getFile2() { return files[1]; }
    
    @Override public File getFile3() { return files[2]; }

  }
  
  private final ConcurrentLinkedQueue<PendingCmd> pendingCmds = new ConcurrentLinkedQueue<PendingCmd>();
  
  private final CmdExecuter executer = new CmdExecuter();
  
  //private final MainCmd_ifc mainCmd;

  private PrintStream log;
  
  private boolean busy;
  
  //private final ProcessBuilder processBuilder = new ProcessBuilder();
  
  private Appendable cmdOutput = System.out;
  
  private Appendable cmdError = System.err;
  
  public CmdQueue(PrintStream log)
  {
    this.log = log;
  }
  

  /**Sets the output and error stream destination
   * @param userOut Destination for output of all executed commands. 
   *   If null then any cmd invocation is done without awaiting the end of execution.
   *   The output and error output isn't used then.
   * @param userErr may be null, then the error is redirected to output
   */
  public void setOutput(Appendable userOut, Appendable userErr)
  {
    cmdOutput = userOut;
    cmdError = userErr;
  }
  
  public void setWorkingDir(File file)
  { final File dir;
    if(!file.isDirectory()){
      dir = file.getParentFile();
    } else {
      dir = file;
    }
    //processBuilder.directory(dir);
  }
  

  
  /**Adds a command to execute later. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmdBlock The command block
   * @param files Some files
   * @return Number of members in queue pending for execution.
   */
  public int addCmd(CmdBlock cmdBlock, File[] files, File currentDir)
  {
    for(PrepareCmd cmd: cmdBlock.getCmds()){
      pendingCmds.add(new PendingCmd(cmd, files, currentDir));  //to execute.
    }
    return pendingCmds.size();
  }

  
  /**Adds a command to the queue which should be executed in another thread.
   * @param sCmd The command maybe with <*file> etc. like described in {@link PrepareCmd}
   * @param files null or some files for execution
   * @param currentDir directory of execution
   * @param kindOfExecution See {@link PrepareCmd#PrepareCmd(char)}
   * @return
   */
  public int addCmd(String sCmd, File[] files, File currentDir, char kindOfExecution)
  {
    PrepareCmd cmd = new PrepareCmd(kindOfExecution);
    cmd.set_cmd(sCmd);
    pendingCmds.add(new PendingCmd(cmd, files, currentDir));  //to execute.
    return pendingCmds.size();
    
  }
  
  
  
  /**Execute the pending commands.
   * This method should be called in a specified user thread.
   * 
   */
  public final void execCmds(Appendable outStatus, Appendable outErrors)
  {
    PendingCmd cmd1;
    boolean someExecute = false;
    while( (cmd1 = pendingCmds.poll())!=null){
      busy = true;
      someExecute = true;
      try{
        StringBuilder sCmdShow = new StringBuilder(120);
        sCmdShow.append("CmdQueue - exec; ");
        if(cmd1.currentDir !=null){
          executer.setCurrentDir(cmd1.currentDir);
          sCmdShow.append(cmd1.currentDir).append(">");
        }
        Class<?> javaClass = cmd1.cmd.getJavaClass();
        if(javaClass !=null){
          
        } else {
          //a operation system command:
          String[] sCmd = cmd1.cmd.prepareCmd(cmd1);
          for(String s1: sCmd){
            sCmdShow.append(s1).append(" ");
          }
          char kindOfExecution = cmd1.cmd.getKindOfExecution();
          if(">%".indexOf(kindOfExecution) >=0){
            if(outStatus !=null){ outStatus.append(">" + sCmd[0]); }
            if(cmdOutput !=null){
              if(cmd1.currentDir !=null){ cmdOutput.append(cmd1.currentDir.getCanonicalPath()).append(">"); }
              else { cmdOutput.append("??>");}
              for(String s:sCmd){
                if(s !=null){ cmdOutput.append(s).append(" "); }
              }
              cmdOutput.append("\n");
            }
            log.println(sCmdShow);
            //mainCmd.writeInfoln(sCmdShow.toString());
            
            int exitCode = executer.execute(sCmd, null, cmdOutput, cmdError, false);
            if(exitCode == 0){ cmdOutput.append("\nJavaCmd: cmd execution successfull\n"); }
            else {cmdOutput.append("\nJavaCmd: cmd execution errorlevel = " + exitCode + "\n"); }
          } else if(kindOfExecution == '&'){
            log.println(sCmdShow);
            //mainCmd.writeInfoln(sCmdShow.toString());
            if(outStatus !=null){ outStatus.append("&" + sCmd[0]); }
            executer.execute(sCmd, null, null, null, false);
            //cmdOutput.append("JavaCmd; started; " + sCmd + "\n");
          } else {
            //mainCmd.writeInfoln("CmdQueue - unexpected kind of execution; " + kindOfExecution);
            log.println("\nCmdQueue - unexpected kind of execution; " + kindOfExecution);
            
          }
        }
      } catch(Exception exc){ System.out.println("Exception " + exc.getMessage()); }
    }
    busy = false;
    if(someExecute && outStatus !=null){
      //clean status line after execution.
      try{ outStatus.append('\0');} catch(IOException exc){}
    }
  }


  /**Aborts a running cmd. If the cmd queue contains any further cmd, it is started yet.
   * If 
   * @return true if any cmd is aborted.
   */
  public boolean abortCmd()
  {
    return executer.abortCmd();
  }
  
  
  public boolean isBusy(){ return busy; }
  
  
  @Override public void close(){
    executer.close();
  }

}
