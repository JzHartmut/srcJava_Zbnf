package org.vishia.cmd;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;



import org.vishia.cmd.CmdExecuter;
import org.vishia.cmd.ZGenScript.ZGenDataAccess;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringPartAppend;
import org.vishia.util.StringSeq;


/**This class is the executer of ZGen. With it both statements can be executed and texts from any Java-stored data 
 * can be generated controlled with the content of {@link ZGenScript}. 
 * An instance of this class is used while {@link #execute(ZGenScript, boolean, boolean, Appendable))} is running.
 * You should not use the instance concurrently in more as one thread. But you can use this instance
 * for one after another call of {@link #execute(ZGenScript, boolean, boolean, Appendable)}.
 * <br><br>
 * @author Hartmut Schorrig
 *
 */
public class ZGenExecuter {
  
  
  /**Version, history and license.
   * <ul>
   * <li>2014-02-01 Hartmut new: onerror errorlevel for cmd now works as statement. {@link ExecuteLevel#cmdErrorlevel}. 
   * <li>2014-02-01 Hartmut chg: now {@link ExecuteLevel#execute(org.vishia.cmd.ZGenScript.StatementList, Appendable, int, boolean)}
   *   returns the exit designation (break, return) 
   * <li>2014-01-12 Hartmut chg: now uses a Stringjar {@link StringPartAppend} instead StringBuffer in Syntax and execution.
   * <li>2014-01-09 Hartmut new: If the "text" variable or any other Appendable variable has a null-value,
   *   a StringBuilder is instantiated therefore and stored in this variable. It is possible therewith
   *   to create an text only if necessary. We don't need a StringBuilder instance if it is never used. 
   * <li>2014-01-06 Hartmut new: {@link ZGenThreadResult} has the error Variable, test thread 
   * <li>2013-12-26 Hartmut new: subroutine returns a value and assigns to any variable.
   * <li>2013-12-26 Hartmut re-engineering: Now the Statement class is obsolete. Instead all statements have the base class
   *   {@link ZGenitem}. That class contains only elements which are necessary for all statements. Some special statements
   *   have its own class with some more elements, especially for the ZBNF parse result. Compare it with the syntax
   *   in {@link org.vishia.zgen.ZGenSyntax}.    
   * <li>2013-10-27 Hartmut chg: Definition of a String name [= value] is handled like assign. Stored with 
   *   {@link DataAccess#storeValue(List, Map, Object, boolean)} with special designation in {@link DataAccess.DatapathElement#whatisit}
   * <li>2013-10-20 Hartmut chg: The {@link #scriptVariables} and the {@link ExecuteLevel#localVariables} are of type
   *   {@link DataAccess.Variable} and not only Object. Advantage: The variable references can be changed in the
   *   instance of {@link DataAccess.Variable#val}, this changing is valid for all references to that variable.
   *   In this kind a script-variable can be changed in a subroutine. This may be necessary and should be supported.
   *   Nevertheless a concept of non-changeable script variables may be proper. It should realized in runtime.
   * <li>2013-10-13 Hartmut chg: onerror: only the coherent statements in one block are checked for onerror. See description.
   *   onerror exit treaded.
   * <li>2013-01-13 Hartmut chg: The method getContent is moved and adapted to {@link ZbatchGenScript.ZbatchExpressionSet#ascertainValue(Object, Map, boolean, boolean, boolean)}.
   * <li>2013-01-12 Hartmut chg: improvements while documentation. Some syntax details. Especially handling of visibility of variables.
   * <li>2013-01-02 Hartmut chg: The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   *   But a script-global variable referred with {@link #listScriptVariables} is defined only one time on start of text generation
   *   with the routine {@link ZbatchExecuter#genScriptVariables(ZbatchGenScript, Object, boolean)}.  
   * <li>2012-12-23 Hartmut chg: {@link #getContent(org.vishia.zbatch.ZbatchGenScript.ZbatchExpressionSet, Map, boolean)} now uses
   *   an {@link ZbatchGenScript.ZbatchExpressionSet} instead a List<{@link DataAccess.DatapathElement}>. Therewith const values are able to use
   *   without extra dataPath, only with a ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link ZbatchGenScript.ZbatchExpressionSet#textArg} if a data path is given, use for formatting a numerical value.
   * <li>2012-12-08 Hartmut new: <:subtext:name:formalargs> has formal arguments now. On call it will be checked and
   *   maybe default values will be gotten.
   * <li>2012-12-08 Hartmut chg: {@link #parseGenScript(File, Appendable)}, {@link #genScriptVariables()}, 
   *   {@link #genContent(ZbatchGenScript, Object, boolean, Appendable)} able to call extra especially for Zmake, currDir.
   *   It is possible to define any script variables in the generating script and use it then to control getting data 
   *   from the input data.
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-11-04 Hartmut chg: adaption to DataAccess respectively distinguish between getting a container or an simple element.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-10 Usage of {@link ZbatchGenScript}.
   * <li>2012-10-03 created. Backgorund was the {@link org.vishia.zmake.Zmake} generator, but that is special for make problems.
   *   A generator which converts ZBNF-parsed data from an Java data context to output texts in several form, documenation, C-sources
   *   was need.
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
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-01-12";

  /**Variable for any exception while accessing any java resources. It is the $error variable of the script. */
  protected String accessError = null;
  
  public final boolean bWriteErrorInOutput;
  
  public static final int kBreak = -1;
  
  public static final int kReturn = -2;
  
  /**Only internal designation.*/
  private static final int kFalse = -3;
  
  public static final int kSuccess = 0;
  
  
  
  protected boolean bAccessPrivate;
  
  protected final MainCmdLogging_ifc log;
  
  /**The java prepared generation script. */
  ZGenScript genScript;
  
  /**Instance for the main script part. */
  //Gen_Content genFile;

  /**Generated content of all script variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  //private final IndexMultiTable<String, DataAccess.Variable> scriptVariables = new_Variables();
  
  
  final ExecuteLevel scriptLevel;
  
  final ZGenThreadResult scriptThread;
  
  /**Generated content of all script environment variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  //final Map<String, String> scriptEnvVariables = new TreeMap<String, String>();
  
  
  protected final Queue<ZGenThread> threads = new ConcurrentLinkedQueue<ZGenThread>();
  
  private boolean bScriptVariableGenerated;
  
  /**The time stamp from {@link System#currentTimeMillis()} on start of script. */
  public long startmilli;
  
  /**The time stamp from {@link System#nanoTime()} on start of script. */
  public long startnano;
  
  /**The newline char sequence. */
  String newline = "\r\n";
  

  /**Creates a ZGenExecuter with possible writing exceptions in the output text.
   * 
   * @param log maybe null
   */
  public ZGenExecuter(MainCmdLogging_ifc log){
    this.log = log;
    bWriteErrorInOutput = false;
    scriptThread = new ZGenThreadResult();
    scriptLevel = new ExecuteLevel(scriptThread);
  }
  
  
  /**Creates a ZGenExecuter with possible writing exceptions in the output text.
   * The advantage of that is: The script runs to its end. It does not break on first exception.
   * The cause is able to read.
   * @param log maybe null
   * @param bWriteErrorInOutput If true then does not throw but writes the exception in the current output.
   */
  public ZGenExecuter(MainCmdLogging_ifc log, boolean bWriteErrorInOutput){
    this.log = log;
    this.bWriteErrorInOutput = bWriteErrorInOutput;
    scriptThread = new ZGenThreadResult();
    scriptLevel = new ExecuteLevel(scriptThread);
  }
  
  
  /**Returns the association to all script variables. The script variables can be changed
   * via this association. Note that change of script variables is a global action, which should not
   * be done for special requests in any subroutine.
   */
  public Map<String, DataAccess.Variable<Object>> scriptVariables(){ return scriptLevel.localVariables; }
  
  public ExecuteLevel scriptLevel(){ return scriptLevel; }
  
  public void setScriptVariable(String name, char type, Object content, boolean bConst) 
  throws IllegalAccessException{
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, name, type, content, bConst);
  }
  
  
  
  /**Generates script-global variables.
   * 
   * @param genScript It should be the same how used on {@link #genContent(ZGenScript, Object, boolean, Appendable)}
   *   but it may be another one for special cases.
   * @param userData Used userdata for content of scriptvariables. It should be the same how used on 
   *   {@link #genContent(ZGenScript, Object, boolean, Appendable)} but it may be another one for special cases.
   * @param accessPrivate true than access to private data of userData
   * @return The built script variables. 
   *   One can evaluate some script variables before running {@link #genContent(ZGenScript, Object, boolean, Appendable)}.
   *   Especially it is used for {@link org.vishia.zmake.Zmake to set the currDir.} 
   * @throws IOException
   * @throws IllegalAccessException 
   */
  public Map<String, DataAccess.Variable<Object>> genScriptVariables(ZGenScript genScriptPar
      , boolean accessPrivate, Map<String, DataAccess.Variable<Object>> srcVariables) 
  throws IOException, IllegalAccessException
  {
    this.genScript = genScriptPar;
    //this.data = userData;
    this.bAccessPrivate = accessPrivate;
    if(srcVariables !=null){
      for(Map.Entry<String, DataAccess.Variable<Object>> entry: srcVariables.entrySet()){
        DataAccess.Variable<Object> var = entry.getValue();
        DataAccess.createOrReplaceVariable(scriptLevel.localVariables, var.name(), var.type(), var.value(), var.isConst());
      }
    }
    if(scriptLevel.localVariables.get("currdir") == null){
      //CurrDir currDirWrapper = new CurrDir();
      //currDirWrapper.currDir 
      File currdir = new File(".").getAbsoluteFile().getParentFile();
      //StringBuilder cd = new StringBuilder();
      //cd.append(FileSystem.normalizePath(currDirWrapper.currDir.getAbsolutePath()));
      //DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "$CD", 'E', currdir.toString(), false);
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "currdir", 'O', currdir, false);
    }
    //if(scriptLevel.localVariables.get("error") == null){ DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "error", 'A', accessError, true); }
    if(scriptLevel.localVariables.get("mainCmdLogging") == null){ DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "mainCmdLogging", 'O', log, true); }
    if(scriptLevel.localVariables.get("nextNr") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nextNr", 'O', nextNr, true); }
    //DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nrElementInContainer", 'O', null);
    if(scriptLevel.localVariables.get("out") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "out", 'A', System.out, true); }
    if(scriptLevel.localVariables.get("err") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "err", 'A', System.err, true); }
    if(scriptLevel.localVariables.get("null") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "null", 'O', null, true); }
    if(scriptLevel.localVariables.get("jbat") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "jbat", 'O', this, true); }
    if(scriptLevel.localVariables.get("zgen") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "zgen", 'O', this, true); }
    if(scriptLevel.localVariables.get("file") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "file", 'O', new FileSystem(), true); }
    if(scriptLevel.localVariables.get("test") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "test", 'O', new ZGenTester(), true); }
    //
    //generate all variables in this script:
    try{
      scriptLevel.execute(genScript.scriptClass, null, 0, false);
      /*
      for(ZGenScript.DefVariable scriptVariableScript: genScript.getListScriptVariables()){
        Object value;
        switch(scriptVariableScript.elementType()){
          case 'S':  value = scriptLevel.evalString(scriptVariableScript); break;
          case 'J':  value = scriptLevel.evalObject(scriptVariableScript, true); break;
          default: value = "???";
        }
        /*
        List<DataAccess.DatapathElement> assignPath = scriptVariableScript.defVariable.datapath();
        if(assignPath.size() == 1 && assignPath.get(0).ident().equals("$CD")){
          //special handling of current directory:
          scriptLevel.changeCurrDir((CharSequence)value);  //normalize, set "currDir"
        } else {
          scriptVariableScript.defVariable.storeValue(scriptLevel.localVariables, value, true);
        }
        */
        /*
        StringBuilder uVariable = new StringBuilder();
        ExecuteLevel genVariable = new ExecuteLevel(null, scriptLevel.localVariables); //NOTE: use recent scriptLevel.localVariables.
        genVariable.execute(scriptVariableScript.getSubContent(), uVariable, false);
        scriptLevel.localVariables.put(scriptVariableScript.identArgJbat, uVariable); //Buffer.toString());
        * /
      }
      */
    } catch(Exception exc){
      System.out.println("JbatchExecuter - Scriptvariable faulty; " );
    }
    bScriptVariableGenerated = true;
    return scriptLevel.localVariables;
  }
  


  
  /**Initializes without any script variables, clears the instance.
   * 
   */
  public void reset(){
    bScriptVariableGenerated = false;
    scriptLevel.localVariables.clear();
    this.genScript = null;
  }
  
  
  /**Initializes, especially generate all script variables. All content before is removed.
   * Especially script variables from a previous usage of the instance are removed.
   * If you want to use a ZGenExecuter more as one time with different scripts
   * but with the same script variables, one should call this routine one time on start,
   * and then {@link #execute(ZGenScript, boolean, boolean, Appendable)} with maybe several scripts,
   * which should not contain script variables, or one should call 
   * {@link #execSub(org.vishia.cmd.ZGenScript.Subroutine, Map, boolean, Appendable)}
   * with one of the subroutines in the given script.
   * 
   * @param genScript Generation script in java-prepared form. It contains the building prescript
   *   for the script variables.
   * @param accessPrivate decision whether private and protected members from Java instances can be accessed.   
   * @throws IOException only if out.append throws it.
   * @throws IllegalAccessException if a const scriptVariable are attempt to modify.
   */
  public void initialize(@SuppressWarnings("hiding") ZGenScript genScript, boolean accessPrivate) 
  throws IOException, IllegalAccessException
  {
    this.scriptLevel.localVariables.clear();
    this.bAccessPrivate = accessPrivate;
    this.genScript = genScript;
    genScriptVariables(genScript, accessPrivate, null);
  }

  
  /**Generates an output with the given script.
   * @param genScript Generation script in java-prepared form. 
   * @param out Any output. It is used for direct text output and it is stored as variable "text"
   *   to write "<+text>...<.+>" to output to it.
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  /**
   * @param genScript The script
   * @param accessPrivate
   * @param bWaitForThreads should set to true if it is a command line invocation of Java,
   *   the exit should wait for all threads. May set to false if calling inside a long running application.
   * @param out Text output of <+text>....<.+>
   * @return exit level? 0
   * @throws IOException
   * @throws IllegalAccessException if a const scriptVariable are attempt to modify.
   */
  public CharSequence execute(ZGenScript genScript, boolean accessPrivate, boolean bWaitForThreads, Appendable out) 
  throws Exception, IllegalAccessException
  { this.bAccessPrivate = accessPrivate;
    //this.data = userData;
    this.genScript = genScript;

    if(!bScriptVariableGenerated){
      genScriptVariables(genScript, accessPrivate, null);
    }
    setScriptVariable("text", 'A', out, true);  //NOTE: out maybe null
    ZGenScript.Subroutine contentScript = genScript.getMain();
    ExecuteLevel execFile = new ExecuteLevel(scriptThread, null, null);
    startmilli = System.currentTimeMillis();
    startnano = System.nanoTime();
    execFile.execute(contentScript.statementlist, out, 0, false);
    if(bWaitForThreads){
      boolean bWait = true;
      while(bWait){
        synchronized(threads){
          bWait = threads.size() !=0;
          if(bWait){
            try{ threads.wait(1000); }
            catch(InterruptedException exc){}
          }
        }
      }

    }
    @SuppressWarnings("cast")
    DataAccess.Variable<Object> varText = (DataAccess.Variable<Object>)scriptLevel.localVariables.get("text");
    Object oText = varText.value();
    if(oText !=null && oText instanceof CharSequence){
      return (CharSequence)oText;
    }
    else return null;
  }

  
  /**Executes the given sub routine invoked from any user application. 
   * The script variables are used from a {@link #initialize(ZGenScript, boolean)}
   * or one of the last {@link #execute(ZGenScript, boolean, boolean, Appendable)}.
   * The {@link ExecuteLevel}, the subroutine's context, is created below the script level. 
   * All of the script variables are known in the subroutine. Additional the args are given.
   * The time measurements {@link #startmilli} and {@link #startnano} starts newly.
   * @param statement The subroutine in the script.
   * @param accessPrivate
   * @param out The text output.
   * @return
   * @throws IOException
   */
  public void execSub(ZGenScript.Subroutine statement, Map<String, DataAccess.Variable<Object>> args
      , boolean accessPrivate, Appendable out) 
  throws Exception
  {
    ExecuteLevel level = new ExecuteLevel(scriptThread, scriptLevel, null);
    //The args should be added to the localVariables of the subroutines level:
    level.localVariables.putAll(args);
    setScriptVariable("text", 'A', out, true);
    //Executes the statements of the sub routine:
    startmilli = System.currentTimeMillis();
    startnano = System.nanoTime();
    level.execute(statement.statementlist, out, 0, false);
    //return sError1;
  }
  
  
  
  
  public Object getScriptVariable(String name) throws NoSuchFieldException
  { return DataAccess.getVariable(scriptLevel.localVariables, name, true); }

  
  public DataAccess.Variable removeScriptVariable(String name)
  { return scriptLevel.localVariables.remove(name);
    
  }

  
  
  

  protected Map<String, DataAccess.Variable> xnew_Variables(){
    return new TreeMap<String, DataAccess.Variable>();
    
  }
  

  protected IndexMultiTable<String, DataAccess.Variable<Object>> new_Variables(){
    return new IndexMultiTable<String, DataAccess.Variable<Object>>(IndexMultiTable.providerString);
  }
  
  
  public void runThread(ExecuteLevel executeLevel, ZGenScript.ThreadBlock statement){
    try{ 
      executeLevel.execute(statement.statementlist, null, 0, false);
    } 
    catch(Exception exc){
      //TODO anything with the environment onerror statement?
      exc.printStackTrace(System.out);
    }
  }
  


  /**
   * @param sError
   * @param out
   * @return false to assign to an ok variable.
   * @throws IOException
   */
  boolean writeError(String sError, Appendable out) throws IOException{
    if(bWriteErrorInOutput){
      out.append(sError);
    } else {
      throw new IllegalArgumentException(sError); 
    }
    return false;

  }
  
  
  
  CharSequence textError(Exception exc, ZGenScript.ZGenitem zgenitem){
    StringBuilder text = new StringBuilder(100); 
    text.append(exc).append( " @FILE:").append(zgenitem.srcLine).append(",").append(zgenitem.srcColumn);
    if(bWriteErrorInOutput){
      Throwable excCause = exc, excText = exc;
      int catastrophicalcount = 10;
      while( --catastrophicalcount >=0 && (excCause = excCause.getCause()) !=null){
        excText = excCause;  //if !=null
      }
      text.append(excText.getMessage());
    } else {
      throw new IllegalArgumentException(text.toString());  //forwarding
    }
    return text;
  }
  
  
  
  /**Wrapper to generate a script with specified localVariables.
   * A new Wrapper is created on any subroutine level. It is used in a {@link CalculatorExpr#calcDataAccess(Map, Object...)} 
   * to generate an expression independent of an environment.
   *
   */
  public final class ExecuteLevel
  {
    /**Not used yet. Only for debug! */
    public final ExecuteLevel parent;
    
    
    final ZGenThreadResult threadData;
    
    /**Generated content of local variables in this nested level including the {@link ZbatchExecuter#scriptLevel.localVariables}.
     * The variables are type invariant on language level. The type is checked and therefore 
     * errors are detected on runtime only. */
    public final IndexMultiTable<String, DataAccess.Variable<Object>> localVariables;
    
    /**Set on break statement. Used only in a for-container execution to break the loop over the elements. */
    private boolean isBreak;
    
    /**The error level which is returned from an operation system cmd invocation.
     * It is used for the {@link #execCmdError(org.vishia.cmd.ZGenScript.Onerror)}.
     */
    private int cmdErrorlevel = 0;
    
    /**Constructs data for a local execution level.
     * @param parentVariables if given this variable are copied to the local ones.
     *   They contains the script variables too. If not given (null), only the script variables
     *   are copied into the {@link #localVariables}. Note that subroutines do not know the
     *   local variables of its calling routine! This argument is only set if nested statement blocks
     *   are to execute. 
     */
    protected ExecuteLevel(ZGenThreadResult threadData, ExecuteLevel parent, Map<String, DataAccess.Variable<Object>> parentVariables)
    { this.parent = parent;
      this.threadData = threadData;
      localVariables = new_Variables();
      if(parentVariables == null){
        for(Map.Entry<String, DataAccess.Variable<Object>> e: scriptLevel.localVariables.entrySet()){
          DataAccess.Variable<Object> var = e.getValue();
          String key = e.getKey();
          if(key.equals("zgensub")){
            
          }
          else if(var.isConst()){
            localVariables.put(key, var);
          } else {
            //build a new independent variable, which can be changed.
            DataAccess.Variable<Object> var2 = new DataAccess.Variable<Object>(var);
            localVariables.put(key, var2);
          }
        }
      } else {
        localVariables.putAll(parentVariables);  //use the same if it is not a subText, only a 
      }
      try{ 
        DataAccess.createOrReplaceVariable(localVariables,  "zgensub", 'O', this, true);
        localVariables.add("error", threadData.error);
      } catch(IllegalAccessException exc){ throw new IllegalArgumentException(exc); }
    }

    
    
    /**Constructs data for the script execution level.
     */
    protected ExecuteLevel(ZGenThreadResult threadData)
    { this.parent = null;
      this.threadData = threadData;
      localVariables = new_Variables();
    }

    
    
    /**Returns the log interface from the environment class. */
    public MainCmdLogging_ifc log(){ return log; }
    
    
    public void setLocalVariable(String name, char type, Object content, boolean isConst) 
    throws IllegalAccessException {
      DataAccess.createOrReplaceVariable(localVariables, name, type, content, isConst);
    }
    
    
    
    
    
    /**Executes an inner script part maybe with a new level of nested local variables.
     * If the contentScript does not contain any variable definition 
     * (see @link {@link ZGenScript.StatementList#bContainsVariableDef}) then this level is used,
     * it prevents a non necessary instantiation of an {@link ExecuteLevel} and copying of local variables. 
     * If new variables are to build in the statements, a new instance of {@link ExecuteLevel} is created
     * and all variables of this.{@link #localVariables} are copied to it. It means the nested level
     * knows the same variables but it can add new variables without effect to this level. Changing
     * of existing variables effects all outer levels. That is necessary, an outer-defined variable can be changed
     * in an inner level. That is guaranteed because only the reference is changed.
     * @param contentScript of the inner level.
     * @param out The main output
     * @param bContainerHasNext only used for for-statement
     * @return an error hint.
     * @throws IOException
     */
    public int executeNewlevel(ZGenScript.StatementList contentScript, final Appendable out, int indentOut, boolean bContainerHasNext) 
    throws Exception 
    { final ExecuteLevel level;
      if(contentScript.bContainsVariableDef){
        level = new ExecuteLevel(threadData, this, localVariables);
      } else {
        level = this;
      }
      return level.execute(contentScript, out, indentOut, bContainerHasNext);
    }

  
    /**
     * @param contentScript
     * @param bContainerHasNext Especially for <:for:element:container>SCRIPT<.for> to implement <:hasNext>
     * @return a return Map if created.
     * @throws IOException 
     */
    private int execute(ZGenScript.StatementList contentScript, final Appendable out, int indentOut, boolean bContainerHasNext) 
    throws Exception 
    {
      Appendable uBuffer = out;
      //Generate direct requested output. It is especially on inner content-scripts.
      int ixStatement = -1;
      int ret = 0;
      //Note: don't use an Iterator, use ixStatement because it will be incremented onError.
      while(ret == 0 && ++ixStatement < contentScript.statements.size()) { //iter.hasNext() && sError == null){
        ZGenScript.ZGenitem statement = contentScript.statements.get(ixStatement); //iter.next();
        //for(TextGenScript.ScriptElement statement: contentScript.content){
        try{    
          switch(statement.elementType()){
          case 't': executeText(statement, out, indentOut);break; //<:>...textexpression <.>
          case 'n': uBuffer.append(newline);  break;   //<.n+>
          case '\\': uBuffer.append(statement.textArg);  break;   //<:n> transcription
          case 'T': textAppendToVarOrOut((ZGenScript.TextOut)statement, out); break; //<+text>...<.+> 
          case 'U': defineExpr((ZGenScript.DefVariable)statement); break; //setStringVariable(statement); break; 
          case 'S': defineExpr((ZGenScript.DefVariable)statement); break; //setStringVariable(statement); break; 
          case 'P': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            executeDefVariable((ZGenScript.DefVariable)statement, 'P', uBufferVariable, true);
          } break;
          case 'L': {
            Object value = evalObject(statement, true); 
              //getContent(statement, localVariables, false);  //not a container
            if(!(value instanceof Iterable<?>)) 
              throw new NoSuchFieldException("JbatExecuter - exec variable must be of type Iterable ;" + ((ZGenScript.DefVariable)statement).defVariable);
            executeDefVariable((ZGenScript.DefVariable)statement, 'L', value, true);
          } break;
          case 'M': executeDefVariable((ZGenScript.DefVariable)statement, 'M', new TreeMap<String, Object>(), true); break; 
          case 'W': executeOpenfile((ZGenScript.DefVariable)statement); break;
          case 'J': {
            Object value = evalObject(statement, false);
            executeDefVariable((ZGenScript.DefVariable)statement, 'O', value, false);
          } break;
          case 'e': executeDatatext((ZGenScript.DataText)statement, out); break; 
          case 's': execSubroutine((ZGenScript.CallStatement)statement, out, indentOut); break;  //sub
          case 'x': executeThread((ZGenScript.ThreadBlock)statement); break;             //thread
          case 'm': executeMove((ZGenScript.CallStatement)statement); break;             //move
          case 'y': executeCopy((ZGenScript.CallStatement)statement); break;             //copy
          case 'c': executeCmdline((ZGenScript.CmdInvoke)statement); break;              //cmd
          case 'd': executeChangeCurrDir(statement); break;                              //cd
          case 'C': ret = executeForContainer((ZGenScript.ForStatement)statement, out, indentOut); break;
          case 'B': ret = execNestedLevel(statement, out, indentOut); break;              //statementBlock
          case 'F': executeIfStatement((ZGenScript.IfStatement)statement, out, indentOut); break;
          case 'w': ret = whileStatement((ZGenScript.CondStatement)statement, out, indentOut); break;
          case 'u': ret = dowhileStatement((ZGenScript.CondStatement)statement, out, indentOut); break;
          case 'N': executeIfContainerHasNext(statement, out, indentOut, bContainerHasNext); break;
          case '=': { 
            Object val = evalObject(statement, false);
            executeAssign((ZGenScript.AssignExpr)statement, val); 
          } break;
          case '+': appendExpr((ZGenScript.AssignExpr)statement); break;        //+=
          case '?': break;  //don't execute a onerror, skip it.  //onerror
          case 'z': throw new ZGenExecuter.ExitException(((ZGenScript.ExitStatement)statement).exitValue);  
          case 'D': debug(statement); break;  //debug
          case 'r': execThrow(statement); break;
          case 'b': isBreak = true; ret = ZGenExecuter.kBreak; break;
          case '#': ret = execCmdError((ZGenScript.Onerror)statement, out, indentOut); break;
          default: 
            uBuffer.append("Jbat - execute-unknown type; '" + statement.elementType() + "' :ERROR=== ");
          }//switch
          
        } catch(Exception exc){
          //any statement has thrown an exception.
          //check onerror with proper error type anywhere after this statement, it is stored in the statement.
          //continue there.
          boolean found = false;
          char excType;   //NOTE: the errortype in an onerror statement is the first letter of error keyword in syntax; notfound, file, internal, exit
          int errLevel = 0;
          if(exc instanceof ExitException){ excType = 'e'; errLevel = ((ExitException)exc).exitLevel; }
          else if(exc instanceof IOException){ excType = 'f'; }
          else if(exc instanceof NoSuchFieldException || exc instanceof NoSuchMethodException){ excType = 'n'; }
          else { excType = 'i'; }
          //Search the block of onerror after this statement.
          //Maybe use an index in any statement, to prevent search time.
          while(++ixStatement < contentScript.statements.size() && (statement = contentScript.statements.get(ixStatement)).elementType() != '?');
          if(ixStatement < contentScript.statements.size()){
            //onerror-block found.
            do { //search the appropriate error type:
              char onerrorType;
              ZGenScript.Onerror errorStatement = (ZGenScript.Onerror)statement;
              if( ((onerrorType = errorStatement.errorType) == excType
                || (onerrorType == '?' && excType != 'e')   //common onerror is valid for all excluding exit 
                )  ){
                found = excType != 'e' || errLevel >= errorStatement.errorLevel;  //if exit exception, then check errlevel
              }
            } while(!found && ++ixStatement < contentScript.statements.size() && (statement = contentScript.statements.get(ixStatement)).elementType() == '?');
          }
          if(found){
            String sError1 = exc.getMessage();
            try{ setLocalVariable("errorMsg", 'S', sError1, false);
            } catch(IllegalAccessException exc1){ throw new IllegalArgumentException(exc1); }
            ret = execute(statement.statementlist, out, indentOut, false);  //executes the onerror block
            //a kBreak, kReturn etc. is used in the calling level.
            threadData.error.setValue(null);  //clear for next usage.
          } else {
            CharSequence sExc = Assert.exceptionInfo("ZGen - execute-exception;", exc, 0, 20);
            if(threadData.error.value()==null){
              threadData.error.setValue(sExc);
            }
            throw exc; //new RuntimeException(sExc.toString());
            //sError = exc.getMessage();
            //System.err.println("ZGen - execute-exception; " + exc.getMessage());
            //exc.printStackTrace();
            //throw exc;
            //throw new IllegalArgumentException (exc.getMessage());
          }
        }
      }//while
      /*
      DataAccess.Variable<Object> retVar = localVariables.get("return");
      if(retVar !=null){
        @SuppressWarnings("unchecked")
        Map<String, DataAccess.Variable<Object>> ret =  (Map<String, DataAccess.Variable<Object>>)retVar.value(); 
        return ret;
      }
      else { return null; }
      */
      return ret;
    }
    
    
    
    void executeText(ZGenScript.ZGenitem statement, Appendable out, int indentOut) throws IOException{
      int posLine = 0;
      int posEnd1, posEnd2;
      if(statement.textArg.startsWith("'''trans ==> dst"))
        stop();
      int zText = statement.textArg.length();
      do{
        char cEnd = '\n';  
        posEnd1 = statement.textArg.indexOf(cEnd, posLine);
        posEnd2 = statement.textArg.indexOf('\r', posLine);
        if(posEnd2 >= 0 && (posEnd2 < posEnd1 || posEnd1 <0)){
          posEnd1 = posEnd2;  // \r found before \n
          cEnd = '\r';
        }
        if(posEnd1 >= 0){ 
          out.append(statement.textArg.substring(posLine, posEnd1));   
          out.append(newline);  //The newline of ZGen invocation.
          //skip over posEnd1, skip over the other end line character if found. 
          if(++posEnd1 < zText){
            if(cEnd == '\r'){ if(statement.textArg.charAt(posEnd1)=='\n'){ posEnd1 +=1; }}
            else            { if(statement.textArg.charAt(posEnd1)=='\r'){ posEnd1 +=1; }}
            int indentCt = indentOut;
            while(--indentCt >= 0 && posEnd1 < zText && " \t".indexOf(statement.textArg.charAt(posEnd1))>=0){
              posEnd1 +=1; //skip over all indentation chars  
            }
          }
          posLine = posEnd1;
        } else { //the rest till end.
          out.append(statement.textArg.substring(posLine));   
        }
        
      } while(posEnd1 >=0);  //output all lines.
    }
    
    

    void executeDefVariable(ZGenScript.DefVariable statement, char type, Object value, boolean isConst) 
    throws Exception {
      storeValue(statement.defVariable, localVariables, value, bAccessPrivate);
      //setLocalVariable(statement.name, type, value, isConst);
       
    }

      
    int executeForContainer(ZGenScript.ForStatement statement, Appendable out, int indentOut) throws Exception
    {
      ZGenScript.StatementList subContent = statement.statementlist();  //The same sub content is used for all container elements.
      ExecuteLevel forExecuter = new ExecuteLevel(threadData, this, localVariables);
      //creates the for-variable in the executer level.
      DataAccess.Variable<Object> forVariable = DataAccess.createOrReplaceVariable(forExecuter.localVariables, statement.forVariable, 'O', null, false);
      //a new level for the for... statements. It contains the foreachData and maybe some more variables.
      Object container = dataAccess(statement.forContainer, localVariables, bAccessPrivate, true, false, null);
      //Object container = statement.forContainer.getDataObj(localVariables, bAccessPrivate, true);
      //DataAccess.Dst dst = new DataAccess.Dst();
      //DataAccess.access(statement.defVariable.datapath(), null, localVariables, bAccessPrivate,false, true, dst);
      boolean cond = true;
      int cont = kSuccess;
      if(container instanceof String && ((String)container).startsWith("<?")){
        writeError((String)container, out);
      }
      else if(container !=null && container instanceof Iterable<?>){
        Iterator<?> iter = ((Iterable<?>)container).iterator();
        while(cond && !forExecuter.isBreak() && iter.hasNext()){
          cond = (cont == kSuccess);
          if(cond && statement.condition !=null){
            cond = evalCondition(statement.condition);
          }
          if(cond){
            Object foreachData = iter.next();
            forVariable.setValue(foreachData);
            cont = forExecuter.execute(subContent, out, indentOut, iter.hasNext());
          }
        }//while of for-loop
      }
      else if(container !=null && container instanceof Map<?,?>){
        Map<?,?> map = (Map<?,?>)container;
        Set<?> entries = map.entrySet();
        Iterator<?> iter = entries.iterator();
        while(cond && !forExecuter.isBreak() && iter.hasNext()){
          cond = (cont == kSuccess);
          if(cond && statement.condition !=null){
            cond = evalCondition(statement.condition);
          }
          if(cond){
            Map.Entry<?, ?> foreachDataEntry = (Map.Entry<?, ?>)iter.next();
            Object foreachData = foreachDataEntry.getValue();
            forVariable.setValue(foreachData);
            cont = forExecuter.execute(subContent, out, indentOut, iter.hasNext());
          }
        }
      }
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    
    int executeIfContainerHasNext(ZGenScript.ZGenitem hasNextScript, Appendable out, int indentOut, boolean bContainerHasNext) 
    throws Exception{
      int cont = kSuccess;
      if(bContainerHasNext){
        //(new Gen_Content(this, false)).
        cont = execute(hasNextScript.statementlist(), out, indentOut, false);
      }
      return cont;
    }
    
    
    
    /**it contains maybe more as one if block and else. 
     * @throws Exception */
    int executeIfStatement(ZGenScript.IfStatement ifStatement, Appendable out, int indentOut) throws Exception{
      int cont = kFalse;
      Iterator<ZGenScript.ZGenitem> iter = ifStatement.statementlist.statements.iterator();
      //boolean found = false;  //if block found
      while(iter.hasNext() && cont == kFalse ){
        ZGenScript.ZGenitem statement = iter.next();
        switch(statement.elementType()){
          case 'G': { //if-block
            boolean hasNext = iter.hasNext();
            cont = executeIfBlock((ZGenScript.IfCondition)statement, out, indentOut, hasNext);
          } break;
          case 'E': { //elsef
            cont = execute(statement.statementlist, out, indentOut, false);
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + statement.elementType() + "' :ERROR=== ");
          }
        }//switch
      }//for
      if(cont == kBreak){ cont = kSuccess; } //break in an if block does not break at calling level.
      return cont;
    }
    
    
    
    /**Executes a while statement. 
     * @throws Exception */
    int whileStatement(ZGenScript.CondStatement whileStatement, Appendable out, int indentOut) 
    throws Exception 
    {
      int cont = kSuccess;
      boolean cond;
      do{
        cond =  (cont ==kSuccess)
             && evalCondition(whileStatement.condition); //.calcDataAccess(localVariables);
        if(cond){
          cont = execute(whileStatement.statementlist, out, indentOut, false);
        }
      } while(cond);  //if executed, check cond again.  
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    /**Executes a dowhile statement. 
     * @throws Exception */
    int dowhileStatement(ZGenScript.CondStatement whileStatement, Appendable out, int indentOut) 
    throws Exception 
    { int cont;
      boolean cond;
      do{
        cont = execute(whileStatement.statementlist, out, indentOut, false);
        cond =  (cont ==kSuccess)
             && evalCondition(whileStatement.condition); //.calcDataAccess(localVariables);
      } while(cond);  //if executed, check cond again.  
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    /**Checks the condition and executes the if-block if the condition is true.
     * If the condition contains elements which are not found in the datapath (throwing {@link NoSuchElementException}),
     * the condition is false, That exception is not thrown forward.
     * @param ifBlock
     * @param out
     * @param bIfHasNext
     * @return true if the condition is true. If it returns false, an elsif or else-Block should be executed.
     * @throws Exception
     */
    int executeIfBlock(ZGenScript.IfCondition ifBlock, Appendable out, int indentOut, boolean bIfHasNext) 
    throws Exception
    { int cont;
      //Object check = getContent(ifBlock, localVariables, false);
      
      //CalculatorExpr.Value check;
      boolean bCheck;
      bCheck = evalCondition(ifBlock.condition); //.calcDataAccess(localVariables);
      if(bCheck){
        cont = execute(ifBlock.statementlist, out, indentOut, bIfHasNext);
      } else cont = kFalse;
      return cont;
    }
    
    
    
    
    
    
    /**Invocation for <+name>text<.+>.
     * It gets the Appendable from the assign variable
     * and executes {@link #execute(org.vishia.cmd.ZGenScript.StatementList, Appendable, boolean)}
     * with it.
     * @param statement the statement
     * @throws Exception 
     */
    void textAppendToVarOrOut(ZGenScript.TextOut statement, Appendable out) throws Exception
    { Appendable out1;
      if(statement.variable !=null){
        Object chn;
        //Object oVar = DataAccess.access(statement.variable.datapath(), null, localVariables, bAccessPrivate, false, true, null);
        Object oVar = dataAccess(statement.variable,localVariables, bAccessPrivate, false, true, null);
        if(oVar instanceof DataAccess.Variable<?>){
          @SuppressWarnings("unchecked")
          DataAccess.Variable<Object> var = (DataAccess.Variable<Object>) oVar;
          chn = var.value();
          if(chn == null && var.type() == 'A'){
            chn = new StringPartAppend();
            var.setValue(chn);            //Creates a new StringPartAppend for an uninitialized variable. Stores there.
          }
        } else {
          //it is not a variable, can be direct stored Appendable:
          chn = oVar;
        }
        if(!(chn instanceof Appendable)) {
          throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
        }
        out1 = (Appendable)chn;  //append, it may be a StringPartAppend.
      } else {
        out1 = out;
      }
      if(statement.statementlist !=null){
        //executes the statement, use the Appendable to output immediately
        execute(statement.statementlist, out1, statement.indent, false);
      } else {
        //Any other text expression
        CharSequence text = evalString(statement);
        if(text !=null){
          out1.append(text);
        }
      }
    }
    
    

    
    
    
    
    
    private int execSubroutine(ZGenScript.CallStatement callStatement, Appendable out, int indentOut) 
    throws IllegalArgumentException, Exception
    { int success = kSuccess;
      boolean ok = true;
      final CharSequence nameSubtext;
      /*
      if(statement.name == null){
        //subtext name gotten from any data location, variable name
        Object oName = ascertainText(statement.expression, localVariables); //getContent(statement, localVariables, false);
        nameSubtext = DataAccess.getStringFromObject(oName, null);
      } else {
        nameSubtext = statement.name;
      }*/
      nameSubtext = evalString(callStatement.callName); 
      ZGenScript.Subroutine subtextScript = genScript.getSubtextScript(nameSubtext);  //the subtext script to call
      if(subtextScript == null){
        throw new NoSuchElementException("JbatExecuter - subroutine not found; " + nameSubtext);
      } else {
        ExecuteLevel subtextGenerator = new ExecuteLevel(threadData, this, null);
        if(subtextScript.formalArgs !=null){
          //
          //build a Map temporary to check which arguments are used:
          //
          TreeMap<String, CheckArgument> check = new TreeMap<String, CheckArgument>();
          for(ZGenScript.DefVariable formalArg: subtextScript.formalArgs) {
            check.put(formalArg.getVariableIdent(), new CheckArgument(formalArg));
          }
          //
          //process all actual arguments:
          //
          List<ZGenScript.Argument> actualArgs = callStatement.actualArgs;
          if(actualArgs !=null){
            for( ZGenScript.Argument actualArg: actualArgs){  //process all actual arguments
              Object ref;
              ref = evalObject(actualArg, false);
              CheckArgument checkArg = check.get(actualArg.getIdent());      //is it a requested argument (per name)?
              if(checkArg == null){
                ok = writeError("??: *subtext;" + nameSubtext + ": " + actualArg.identArgJbat + " faulty argument.?? ", out);
              } else {
                checkArg.used = true;    //requested and resolved.
                char cType = checkArg.formalArg.elementType();
                //creates the argument variable with given actual value and the requested type in the sub level.
                DataAccess.createOrReplaceVariable(subtextGenerator.localVariables, actualArg.identArgJbat, cType, ref, false);
              }
            }
          }
          //check whether all formal arguments are given with actual args or get its default values.
          //if not all variables are correct, write error.
          for(Map.Entry<String, CheckArgument> checkArg : check.entrySet()){
            CheckArgument arg = checkArg.getValue();
            if(!arg.used){
              //Generate on scriptLevel (classLevel) because the formal parameter list should not know things of the calling environment.
              Object ref = scriptLevel.evalObject(arg.formalArg, false);
              String name = arg.formalArg.getVariableIdent();
              char cType = arg.formalArg.elementType();
              //creates the argument variable with given default value and the requested type.
              DataAccess.createOrReplaceVariable(localVariables, name, cType, ref, false);
            }
          }
        } else if(callStatement.actualArgs !=null){
          ok = writeError("??: call" + nameSubtext + " called with arguments, it has not one.??", out);
        }
        if(ok){
          success = subtextGenerator.execute(subtextScript.statementlist, out, indentOut, false);
          if(success == kBreak || success == kReturn){
            success = kSuccess;  //break or return in subroutine ignored on calling level!
          }
          if(callStatement.variable !=null || callStatement.assignObjs !=null){
            DataAccess.Variable<Object> retVar = subtextGenerator.localVariables.get("return");
            Object value = retVar !=null ? retVar.value() : null;
            executeAssign(callStatement, value);
          }
        }
      }
      return success;
    }
    
    
    
    
    /**executes statements in another thread.
     * @throws Exception 
     */
    private void executeThread(ZGenScript.ThreadBlock statement) 
    throws Exception
    { final ZGenThreadResult result;
      if(statement.threadVariable !=null){
        try{
          //if(statement.threadName != null){  //marker for a new ThreadVariable
            result = new ZGenThreadResult();
            storeValue(statement.threadVariable, localVariables, result, bAccessPrivate);
          //} else { 
            //use existing thread variable, Exception if not found.
          //  result = (ZGenThreadResult)statement.threadVariable.getDataObj(localVariables, bAccessPrivate, false);
          //}
        } catch(Exception exc){
          throw new IllegalArgumentException("JbatchExecuter - thread assign failure; path=" + statement.dataAccess.toString());
        }
      } else {
        result = new ZGenThreadResult();  //without assignment to a variable.
      }
      ExecuteLevel threadLevel = new ExecuteLevel(result, this, localVariables);
      ZGenThread thread = new ZGenThread(threadLevel, statement, result);
      synchronized(threads){
        threads.add(thread);
      }
      Thread threadmng = new Thread(thread, "ZGen");
      threadmng.start();  
      //it does not wait on finishing this thread.
    }

    
    
    
    /**Executes an internal statement block. If the Statementlist has local variables, a new
     * instance of this is created. Elsewhere the same instance is used.
     * 
     * @param script
     * @param out
     * @return continue information for the calling level, 
     *   see return of {@link #execute(org.vishia.cmd.ZGenScript.StatementList, Appendable, int, boolean)}
     *   A {@link ZGenExecuter#kBreak} inside the statementlist is not returned. The break is only valid
     *   inside the block. All other return values of execute are returned.
     * @throws IOException
     */
    private int execNestedLevel(ZGenScript.ZGenitem script, Appendable out, int indentOut) 
    throws Exception
    {
      ExecuteLevel genContent;
      if(script.statementlist.bContainsVariableDef){
        genContent = new ExecuteLevel(threadData, this, localVariables);
      } else {
        genContent = this;  //don't use an own instance, save memory and calculation time.
      }
      int ret = genContent.execute(script.statementlist, out, indentOut, false);
      if(ret == kBreak){ 
        ret = kSuccess; 
      }
      return ret;
    }

    
    
    void executeCmdline(ZGenScript.CmdInvoke statement) 
    throws IllegalArgumentException, Exception
    {
      boolean ok = true;
      final CharSequence sCmd;
      if(statement.textArg == null){
        //cmd gotten from any data location, variable name
        sCmd = evalString(statement); 
      } else {
        sCmd = statement.textArg;
      }
      String[] args;
      if(statement.cmdArgs !=null){
        args = new String[statement.cmdArgs.size() +1];
        int iArg = 1;
        for(ZGenScript.ZGenitem arg: statement.cmdArgs){
          String sArg = evalString(arg).toString(); //XXXascertainText(arg.expression, localVariables);
          args[iArg++] = sArg;
        }
      } else { 
        args = new String[1]; 
      }
      args[0] = sCmd.toString();
      List<Appendable> outCmd;
      if(statement.variable !=null){
        outCmd = new LinkedList<Appendable>();
        Object oOutCmd = dataAccess(statement.variable, localVariables, bAccessPrivate, false, false, null);
        //Object oOutCmd = statement.variable.getDataObj(localVariables, bAccessPrivate, false);
        if(oOutCmd instanceof Appendable){
          outCmd.add((Appendable)oOutCmd);
        } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
        }
        if(statement.assignObjs !=null){
          for(DataAccess assignObj1 : statement.assignObjs){
            oOutCmd = dataAccess(assignObj1, localVariables, bAccessPrivate, false, false, null);
            //oOutCmd = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
            if(oOutCmd instanceof Appendable){
              outCmd.add((Appendable)oOutCmd);
            } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
            }
        } }
      } else {
        outCmd = null;
      }
      CmdExecuter cmdExecuter = new CmdExecuter();
      //CurrDir currDir = (CurrDir)DataAccess.getVariable(localVariables,"currDir", true);
      
      Map<String,String> env = cmdExecuter.environment();
      Iterator<DataAccess.Variable<Object>> iter = localVariables.iterator("$");
      boolean cont = true;
      while(cont && iter.hasNext()){
        DataAccess.Variable variable = iter.next();
        String name = variable.name();
        String value = variable.value().toString();
        if(name.startsWith("$")){
          env.put(name.substring(1), value);
        } else {
          cont = false;
        }
      }
      File currdir = (File)localVariables.get("currdir").value();
      cmdExecuter.setCurrentDir(currdir);
      this.cmdErrorlevel = cmdExecuter.execute(args, statement.bShouldNotWait, null, outCmd, null);
      cmdExecuter.close();
    }
    

    int execCmdError(ZGenScript.Onerror statement, Appendable out, int indentOut) throws Exception {
      int ret = 0;
      if(this.cmdErrorlevel >= statement.errorLevel){
        ret = execute(statement.statementlist, out, indentOut, false);
      }
      return ret;
    }



    void executeChangeCurrDir(ZGenScript.ZGenitem statement)
    throws Exception
    {
      CharSequence arg = evalString(statement);
      changeCurrDir(arg, localVariables); 
    }

    
    /**Executes the cd command: changes the directory in this execution level.
     * @param arg maybe a relative path.
     * @throws NoSuchFieldException if "currdir" is not found, unexpected.
     * @throws IllegalAccessException
     */
    protected void changeCurrDir(CharSequence arg, Map<String, DataAccess.Variable<Object>> variables) 
    throws NoSuchFieldException, IllegalAccessException{
      //String sCurrDir;
      final CharSequence arg1;
      boolean absPath = FileSystem.isAbsolutePathOrDrive(arg);
      File cdnew;
      if(absPath){
        //Change the content of the currdir to the absolute directory.
        cdnew = new File(arg.toString());
        if(!cdnew.exists() || !cdnew.isDirectory()){
          throw new IllegalArgumentException("ZGenExecuter - cd, dir not exists; " + arg);
        }
      } else {
        File cdcurr = (File)DataAccess.getVariable(variables,"currdir", true).value();
        cdnew = new File(cdcurr, arg.toString());
        if(!cdnew.exists() || !cdnew.isDirectory()){
          throw new IllegalArgumentException("ZGenExecuter - cd, dir not exists; " + arg + "; abs=" + cdnew.getAbsolutePath());
        }
        String sPath = FileSystem.getCanonicalPath(cdnew);
        cdnew = new File(sPath);
      }
      DataAccess.createOrReplaceVariable(variables,"currdir", 'L', cdnew, true);
      //FileSystem.normalizePath(u);   //resolve "xxx/../xxx"
      //sCurrDir = u.toString();
      
    }
    
     
    
    /**Inserts <*a_datapath> in the out.
     * @param statement
     * @param out
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private void executeDatatext(ZGenScript.DataText statement, Appendable out)  //<*datatext>
    throws IllegalArgumentException, Exception
    {
      CharSequence text = "??";
      try{
        Object obj = dataAccess(statement.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //Object obj = statement.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(obj==null){ text = "null"; 
        } else {
          //else if(obj instanceof CalculatorExpr.Value){
          //  obj = ((CalculatorExpr.Value)obj).objValue();
          //}
          if(statement.format !=null){ //it is a format string:
             text = String.format(statement.format, obj);
          } else {
            text = obj.toString();
          }
        }
      } catch(Exception exc){
        text = textError(exc, statement);  //throws
      }
      out.append(text); 
    }
    
    void executeMove(ZGenScript.CallStatement statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence s1 = evalString(statement.actualArgs.get(0));
      CharSequence s2 = evalString(statement.actualArgs.get(1));
      File fileSrc = new File(s1.toString());
      File fileDst = new File(s2.toString());
      boolean bOk = fileSrc.renameTo(fileDst);
      if(!bOk) throw new IOException("JbatchExecuter - move not successfully; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
    void executeCopy(ZGenScript.CallStatement statement) 
    throws Exception
    {
      CharSequence s1 = evalString(statement.actualArgs.get(0));
      CharSequence s2 = evalString(statement.actualArgs.get(1));
      File fileSrc = new File(s1.toString());
      File fileDst = new File(s2.toString());
      int nrofBytes = FileSystem.copyFile(fileSrc, fileDst);
      if(nrofBytes <0) throw new FileNotFoundException("JbatchExecuter - copy src not found; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
    /**Creates a new FileWriter with the given name {@link #evalString(org.vishia.cmd.ZGenScript.Argument)}
     * of the statement. If the file name is local, the actual value of $CD is set as pre-path.
     * The current directory of the file system is not proper to use because the current directory of this 
     * execution level should be taken therefore. If the path is faulty or another exception is thrown,
     * the exception is forwarded to the execution level (onerror-statement).
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void executeOpenfile(ZGenScript.DefVariable statement) 
    throws IllegalArgumentException, Exception
    {
      String sFilename = evalString(statement).toString();
      Writer writer;
      if(!FileSystem.isAbsolutePath(sFilename)){
        //build an absolute filename with $CD, the current directory of the file system is not proper to use.
        
        @SuppressWarnings("unused")
        File cd = (File)localVariables.get("currdir").value();
        //sFilename = cd + "/" + sFilename;
        File fWriter = new File(cd, sFilename);
        writer = new FileWriter(fWriter);
      } else {
        writer = new FileWriter(sFilename);  //given absolute path
      }
      storeValue(statement.defVariable, localVariables, writer, bAccessPrivate);
      //setLocalVariable(statement.identArgJbat, 'A', writer);
    }
    
    
    
    
    /**Executes a <code>assignment::= [{ < datapath?assign > = }] < expression > ;.</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void executeAssign(ZGenScript.AssignExpr statement, Object val) 
    throws IllegalArgumentException, Exception
    {
      ZGenScript.ZGenDataAccess assignObj1 = statement.variable;
      Iterator<ZGenScript.ZGenDataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        //Object dst = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
        DataAccess.Dst dstField = new DataAccess.Dst();
        //List<DataAccess.DatapathElement> datapath = assignObj1.datapath(); 
        //Object dst = DataAccess.access(datapath, null, localVariables, bAccessPrivate, false, true, dstField);
        Object dst = dataAccess(assignObj1,localVariables, bAccessPrivate, false, true, dstField);
        if(dst instanceof DataAccess.Variable){
          DataAccess.Variable var = (DataAccess.Variable) dst; //assignObj1.accessVariable(localVariables, bAccessPrivate);
          dst = var.value();
          switch(var.type()){
            case 'A': {
              throwIllegalDstArgument("assign to appendable faulty", assignObj1, statement); 
            } break;
            case 'U': {
              assert(dst instanceof StringPartAppend);            
              StringPartAppend u = (StringPartAppend) dst;
              u.clear();
              if(!(val instanceof CharSequence)){
                val = val.toString();
              }
              u.append((CharSequence)val);
            } break;
            case 'S':{
              if(val == null || val instanceof String || val instanceof StringSeq && ((StringSeq)val).isUnmated()){
                var.setValue(val);
              } else {
                var.setValue(val.toString());
              }
            } break;
            default:{
              var.setValue(val);   //sets the value to the variable.
            }
          }//switch
        } else {
          //check whether the field is compatible with val
          dstField.set(val);
          //DataAccess.storeValue(assignObj1.datapath(), localVariables, val, bAccessPrivate);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
    }
    
    
    
    
    
    
    /**Executes a <code>DefObjVar::= < variable?defVariable>  [ = < objExpr?>]</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void defineExpr(ZGenScript.DefVariable statement) 
    throws IllegalArgumentException, Exception
    {
      Object init = evalObject(statement, false);  //maybe null
      Object val;
      switch(statement.elementType()){
        case 'U': {
          if(init == null){
            val = new StringPartAppend();
          } else {
            CharSequence init1 = init instanceof CharSequence ? (CharSequence)init : init.toString();
            StringPartAppend u = new StringPartAppend();
            u.append(init1);
            val = u;
          }
        } break;
        case 'S':{
          if(init == null || init instanceof String || init instanceof StringSeq && ((StringSeq)init).isUnmated()){
            val = init;
          } else {
            val = init.toString();
          }
        } break;
        default: val = init;
      }
      //DataAccess.Variable var = (DataAccess.Variable)DataAccess.access(statement.defVariable.datapath(), null, localVariables, bAccessPrivate, false, true, null);
      //var.setValue(val);
      List<DataAccess.DatapathElement> datapath = statement.defVariable.datapath();
      if(datapath.get(0).ident().equals("return") && !localVariables.containsKey("return")) {
        //
        //creates the local variable return on demand:
        DataAccess.Variable<Object> ret = new DataAccess.Variable<Object>('M', "return", new_Variables());
        localVariables.add("return", ret);
      }
      storeValue(statement.defVariable, localVariables, val, bAccessPrivate);
      
    }
    
    
    
    /**Executes a <code>appendExpr::= [{ < datapath?assign > += }] < expression > ;.</code>.
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void appendExpr(ZGenScript.AssignExpr statement) 
    throws IllegalArgumentException, Exception
    {
      Object val = evalObject(statement, false);
      DataAccess assignObj1 = statement.variable;
      Iterator<ZGenScript.ZGenDataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        Object dst = dataAccess(assignObj1, localVariables, bAccessPrivate, false, false, null);
        //Object dst = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
        if(dst instanceof Appendable){
          if(!(val instanceof CharSequence)){
            val = val.toString();
          }
          ((Appendable) dst).append((CharSequence)val);
        } else {
          throwIllegalDstArgument("dst should be Appendable", assignObj1, statement);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
    }
    
    
    
    void execThrow(ZGenScript.ZGenitem statement) throws Exception {
      CharSequence msg = evalString(statement);
      throw new IllegalArgumentException(msg.toString());
    }
    
    
    /**Checks either the {@link ZGenScript.Argument#dataAccess} or, if it is null,
     * the {@link ZGenScript.Argument#expression}. Returns either the Object which is gotten
     * by the {@link DataAccess#getDataObj(Map, boolean, boolean)} or which is calculated
     * by the expression. Returns an instance of {@link CalculatorExpr.Value} if it is 
     * a result by an expression.
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalDatapathOrExpr(ZGenScript.Argument arg) throws Exception{
      if(arg.dataAccess !=null){
        Object o = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o; }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        if(value.isObjValue()){ return value.objValue(); }
        else return value;
      } else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
    }
    
    
    

    
    public CharSequence evalString(ZGenScript.ZGenitem arg) 
    throws Exception 
    {
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        Object o = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o.toString(); }
      } else if(arg.statementlist !=null){
        StringPartAppend u = new StringPartAppend();
        executeNewlevel(arg.statementlist, u, 0, false);
        return StringSeq.create(u, true);
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        return value.stringValue();
      } else return null;  //it is admissible.
      //} else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
    }
    
    
    
    /**Accesses the data. Before execution all arguments of methods inside the {@link #datapath()}
     * are calculated with the capability of ZGen.
     * @param dataPool
     * @param accessPrivate
     * @param bContainer
     * @param bVariable
     * @param dst
     * @return
     * @throws Exception
     */
    Object dataAccess(DataAccess dataAccess
    , Map<String, DataAccess.Variable<Object>> dataPool
    , boolean accessPrivate
    , boolean bContainer
    , boolean bVariable
    , DataAccess.Dst dst
    ) throws Exception {
      calculateArguments(dataAccess);
      return DataAccess.access(dataAccess.datapath(), null, dataPool, accessPrivate, bContainer, bVariable, dst);
    }
      
    
    
    void storeValue(DataAccess dataAccess
    , Map<String, DataAccess.Variable<Object>> dataPool
    , Object value
    , boolean accessPrivate
    )throws Exception {
      calculateArguments(dataAccess);
      dataAccess.storeValue(dataPool, value, accessPrivate);
    }

    
    
    
    private void calculateArguments(DataAccess dataAccess) throws Exception {
      for(DataAccess.DatapathElement dataElement : dataAccess.datapath()){  //loop over all elements of the path with or without arguments.
        //check all datapath elements whether they have method calls with arguments:
        if(dataElement instanceof ZGenScript.ZGenDatapathElement){
          ZGenScript.ZGenDatapathElement zgenDataElement = (ZGenScript.ZGenDatapathElement)dataElement;
          if(zgenDataElement.fnArgsExpr !=null){
            int nrofArgs = zgenDataElement.fnArgsExpr.size();
            Object[] args = new Object[nrofArgs];
            int iArgs = -1;
            for(ZGenScript.ZGenitem expr: zgenDataElement.fnArgsExpr){
              Object arg = evalObject(expr, false);
              args[++iArgs] = arg;
            }
            zgenDataElement.setActualArgumentArray(args);
          }
        }
      }

    }
    
    
    
    /**Gets the value of the given Argument. Either it is a 
     * <ul>
     * <li>String from {@link ZGenScript.ZGenitem#textArg}
     * <li>Object from {@link ZGenScript.ZGenitem#dataAccess}
     * <li>Object from {@link ZGenScript.ZGenitem#statementlist}
     * <li>Object from {@link ZGenScript.ZGenitem#expression}
     * <li>
     * </ul>
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalObject(ZGenScript.ZGenitem arg, boolean bContainer) throws Exception{
      Object obj;
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        //calculate arguments firstly:
        obj = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //obj = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
      } else if(arg.statementlist !=null){
        StringPartAppend u = new StringPartAppend();
        executeNewlevel(arg.statementlist, u, arg.statementlist.indent, false);
        obj = u.toString();
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        obj = value.objValue();
      } else obj = null;  //throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
      return obj;
    }
    
    
    
    
    public boolean evalCondition(ZGenScript.ZGenitem arg) throws Exception{
      boolean ret;
      if(arg.textArg !=null) return true;
      else if(arg.dataAccess !=null){
        try{
          Object obj = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
          if(obj instanceof Number){
            ret = ((Number)obj).intValue() !=0;
          } else if(obj instanceof Boolean){
            ret = ((Boolean)obj).booleanValue();
          } else {
            ret = obj !=null;
          }
        } catch(NoSuchElementException exc){
          ret = false;
        } catch(NoSuchFieldException exc){
          ret = false;
        } catch(NoSuchMethodException exc){
          ret = false;
        }
      } else if(arg.statementlist !=null){
        throw new IllegalArgumentException("ZGenExecuter - unexpected, faulty syntax");
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        ret = value.booleanValue();
      } else throw new IllegalArgumentException("ZGenExecuter - unexpected, faulty syntax");
      return ret;
      
    }
    
    
    
    boolean isBreak(){ return isBreak; }
    
    void debug(ZGenScript.ZGenitem statement) throws Exception{
      CharSequence text = evalString(statement);
      Assert.stop();
    }
    
    
    void throwIllegalDstArgument(CharSequence text, DataAccess dst, ZGenScript.ZGenitem statement)
    throws IllegalArgumentException
    {
      StringBuilder u = new StringBuilder(100);
      u.append("ZGen - ").append(text).append(";").append(dst);
      u.append("; in file ").append(statement.parentList.srcFile);
      u.append(", line ").append(statement.srcLine).append(" col ").append(statement.srcColumn);
      throw new IllegalArgumentException(u.toString());
    }
    
  }    
  /**Small class instance to build a next number. 
   * Note: It is anonymous to encapsulate the current number value. 
   * The only one access method is Object.toString(). It returns a countered number.
   */
  private final Object nextNr = new Object(){
    int nr = 0;
    @Override
    public String toString(){
      return "" + ++nr;
    }
  };
  
  
  
  
  

  
  /**Class only to check argument lists and use default values for arguments. */
  private class CheckArgument
  {
    /**Reference to the formal argument. */
    final ZGenScript.DefVariable formalArg;
    
    /**Set to true if this argument is used. */
    boolean used;
    
    CheckArgument(ZGenScript.DefVariable formalArg){ this.formalArg = formalArg; }
  }
  
  
  void stop(){
    
    
  }

  
  
  /**State variable of a running thread or finished thread.
   * This instance will be notified if any waits (join operation)
   */
  protected static class ZGenThreadResult implements CharSequence
  {
    StringBuilder uText = new StringBuilder();
    
    /**Exception text. If not null then an exception is thrown and maybe thrown for the next level.
     * This text can be gotten by the "error" variable.
     */
    DataAccess.Variable<Object> error = new DataAccess.Variable<Object>('S', "error", null);
    
    /**State of thread execution. 
     * <ul>
     * <li>i: init
     * <li>r: runs
     * <li>y: finished
     * </ul>
     */
    char state = 'i';

    
    ZGenThreadResult(){}
    
    protected void clear(){
      state = 'i';
    }
    
    @Override public char charAt(int index){ return uText.charAt(index); }

    @Override public int length(){ return uText.length(); }

    @Override public CharSequence subSequence(int start, int end){ return uText.subSequence(start, end); }
    
  }
  
  
  
  /**A thread instance of ZGen.
   */
  protected class ZGenThread implements Runnable
  {
    final ExecuteLevel executeLevel;

    final ZGenScript.ThreadBlock statement;

    final ZGenThreadResult result;
    
    public ZGenThread(ExecuteLevel executeLevel, ZGenScript.ThreadBlock statement, ZGenThreadResult result)
    {
      this.executeLevel = executeLevel;
      this.statement = statement;
      this.result = result;
    }

    @Override public void run(){ 
      runThread(executeLevel, statement); 
      synchronized(threads){
        boolean bOk = threads.remove(this);
        assert(bOk);
        if(threads.size() == 0){
          threads.notify();
        }
      }
    }
    
  }
  
  
  public static class ExitException extends Exception
  {
    public int exitLevel;
    
    public ExitException(int exitLevel){
      this.exitLevel = exitLevel;
    }
  }
  
  
  
  
}
