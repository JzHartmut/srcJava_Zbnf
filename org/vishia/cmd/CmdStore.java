package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.vishia.util.StringPart;


/**This class stores some prepared commands. The input of the store is a file, 
 * which contains the assembling of command lines maybe with placeholder for files.
 * <br><br>
 * Simple UML diagram:
 * <pre>
 * 
 * CmdStore ------*> PrepareCmd
 * </pre>
 * (See {@link org.vishia.util.Docu_UML_simpleNotation})
 * 
 * @author Hartmut Schorrig
 *
 */
public class CmdStore
{

  /**Version, history and license:
   * <ul>
   * <li>2012-02-19 Hartmut chg: {@link #readCmdCfg(File)} accepts $ENV, commentlines with // and #
   *   and start of command not with spaces on line start.
   * <li>2011-12-31 Hartmut chg {@link CmdBlock#title} is new, the syntax of configfile is changed.
   *   This class is used to capture all executables for a specified extension for The.file.Commander 
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
  
  /**Description of one command.
   */
  public static class CmdBlock
  {
    /**The identification for user in the selection list. */
    public String name;
  
    /**The title of the cmd, for selection. It is the part after ':' in the title line:
     * <pre>
     * ==name: title==
     * </pre>
     * */
    public String title;
    
    /**Some commands of this block. */
    public final List<PrepareCmd> listBlockCmds = new LinkedList<PrepareCmd>();

    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command */
    public PrepareCmd new_cmd(){ return new PrepareCmd(); }
    
    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command */
    public void add_cmd(PrepareCmd cmd)
    { cmd.prepareListCmdReplace();
      listBlockCmds.add(cmd); 
    }
    
    /**Returns all commands which are contained in this CmdBlock. */
    public final List<PrepareCmd> getCmds(){ return listBlockCmds; }
    
    @Override public String toString(){ return name + listBlockCmds; }
    
  }
  
  /**Contains all commands read from the configuration file in the read order. */
  private final List<CmdBlock> listCmds = new LinkedList<CmdBlock>();

  /**Contains all commands read from the configuration file in the read order. */
  private final Map<String, CmdBlock> idxCmd = new TreeMap<String, CmdBlock>();

  private String XXXsyntaxCmd = "Cmds::={ <cmd> }\\e. "
    + "cmd::= <* :?name> : { <*\\n?cmd> \\n } ."; 

  
  public CmdStore()
  {
  }
  
  
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command block */
  public CmdBlock new_CmdBlock(){ return new CmdBlock(); }
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command block. */
  public void add_CmdBlock(CmdBlock value){ listCmds.add(value); idxCmd.put(value.name, value); }
  
  
  public String readCmdCfg(File cfgFile)
  { String sError = null;
    BufferedReader reader = null;
    try{
      reader = new BufferedReader(new FileReader(cfgFile));
    } catch(FileNotFoundException exc){ sError = "CommandSelector - cfg file not found; " + cfgFile; }
    if(reader !=null){
      CmdBlock actBlock = null;
      listCmds.clear();
      String sLine;
      StringPart spLine = new StringPart();
      StringBuilder uLine = new StringBuilder(1000);
      try{ 
        while( (sLine = reader.readLine()) !=null){
          if(sLine.contains("$")){
            uLine.setLength(0);
            uLine.append(sLine.trim());
            spLine.assignReplaceEnv(uLine);
            sLine = uLine.toString();
          } else {
            sLine = sLine.trim();
            spLine.assign(sLine);
          }
          if(sLine.length() ==0){
            
          } else if( sLine.startsWith("==")){
            final int posColon = sLine.indexOf(':');
            final int posEnd = sLine.indexOf("==", 2);  
            //a new command block
            if(actBlock !=null){ add_CmdBlock(actBlock); }  //the last one. 
            actBlock = new_CmdBlock();
            if(posColon >=0 && posColon < posEnd){
              actBlock.name = sLine.substring(2, posColon).trim();
              actBlock.title = sLine.substring(posColon+1, posEnd).trim();
            } else {
              actBlock.name = sLine.substring(2, posEnd).trim();
              actBlock.title = "";
            }
          } else if(sLine.startsWith("@")){
          } else if(sLine.startsWith("//")){
          } else if(sLine.startsWith("#")){
                  
          } else { // if(sLine.startsWith(" ")){  //a command line
            PrepareCmd cmd = actBlock.new_cmd();
            cmd.set_cmd(sLine.trim());
            //cmd.prepareListCmdReplace();
            actBlock.add_cmd(cmd);
          }      
        }
        if(actBlock !=null){ add_CmdBlock(actBlock); } 
      } 
      catch(IOException exc){ sError = "CommandStore - cfg file read error; " + cfgFile; }
      catch(IllegalArgumentException exc){ sError = "CommandStore - cfg file error; " + cfgFile + exc.getMessage(); }
    }
    return sError;
  }
  
  
  /**Gets a named command
   * @param name The name given in configuration file
   * @return The prepared CmdBlock or null if not found.
   */
  public CmdBlock getCmd(String name){ return idxCmd.get(name); }
  
  /**Gets a contained commands for example to present in a selection list.
   * @return The list.
   */
  public final List<CmdBlock> getListCmds(){ return listCmds; }
  
}
