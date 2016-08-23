package org.vishia.cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.StringFunctions;


/**This class contains the internal representation of a ZGen script. 
 * The translator is contained in {@link org.vishia.zgen.ZGen} in the srcJava_Zbnf source package. 
 * This class is independent of ZBNF. It is used for working in srcJava_vishiaBase environment.
 * It means, without the srcJava_Zbnf source package all sources of that are able to compile, but
 * this class have not data, respectively it is not instantiated. It may be possible to instantiate
 * and fill by direct invocation of the new_semantic(...) and add_semantic(...) operations, for example
 * for simple scripts.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ZGenScript {
  /**Version, history and license.
   * <ul>
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   * <li>2014-02-22 Hartmut new: Bool and Num as variable types.
   * <li>2014-02-16 Hartmut: new {@link #fileScript} stored here. 
   * <li>2014-01-01 Hartmut re-engineering: {@link ZGenitem} has one of 4 active associations for its content.
   * <li>2013-12-26 Hartmut re-engineering: Now the Statement class is obsolete. Instead all statements have the base class
   *   {@link ZGenitem}. That class contains only elements which are necessary for all statements. Some special statements
   *   have its own class with some more elements, especially for the ZBNF parse result. Compare it with the syntax
   *   in {@link org.vishia.zgen.ZGenSyntax}.    
   * <li>2013-07-30 Hartmut chg {@link #translateAndSetGenCtrl(File)} returns void.
   * <li>2013-07-20 Hartmut chg Some syntactical changes.
   * <li>2013-07-14 Hartmut tree traverse enable because {@link Argument#parentList} and {@link StatementList#parentStatement}
   * <li>2013-06-20 Hartmut new: Syntax with extArg for textual Arguments in extra block
   * <li>2013-03-10 Hartmut new: <code><:include:path></code> of a sub script is supported up to now.
   * <li>2013-10-09 Hartmut new: <code><:scriptclass:JavaPath></code> is supported up to now.
   * <li>2013-01-13 Hartmut chg: The {@link ZbatchExpressionSet#ascertainValue(Object, Map, boolean, boolean, boolean)} is moved
   *   and adapted from TextGenerator.getContent. It is a feauture from the Expression to ascertain its value.
   *   That method and {@link ZbatchExpressionSet#text()} can be invoked from a user script immediately.
   *   The {@link ZbatchExpressionSet} is used in {@link org.vishia.zmake.ZmakeUserScript}.
   * <li>2013-01-02 Hartmut chg: localVariableScripts removed. The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   * <li>2012-12-24 Hartmut chg: Now the 'ReferencedData' are 'namedArgument' and it uses 'dataAccess' inside. 
   *   The 'dataAccess' is represented by a new {@link XXXXXXStatement}('e',...) which can have {@link ZbatchExpressionSet#constValue} 
   *   instead a {@link ZbatchExpressionSet#datapath}. 
   * <li>2012-12-24 Hartmut chg: {@link ZbnfDataPathElement} is a derived class of {@link DataAccess.DatapathElement}
   *   which contains destinations for argument parsing of a called Java-subroutine in a dataPath.  
   * <li>2012-12-23 Hartmut chg: A {@link XXXXXXStatement} and a {@link Argument} have the same usage aspects for arguments
   *   which represents values either as constants or dataPath. Use Argument as super class for ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link ZbatchExpressionSet#textArg} if a data path is given, use for formatting a numerical value.   
   * <li>2012-12-22 Hartmut new: Syntax as constant string inside. Some enhancements to set control: {@link #translateAndSetGenCtrl(StringPartBase)} etc.
   * <li>2012-12-22 Hartmut chg: <:if:...> uses {@link CalculatorExpr} for expressions.
   * <li>2012-11-24 Hartmut chg: @{@link XXXXXXStatement#datapath} with {@link DataAccess.DatapathElement} 
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-19 Hartmut chg: Renaming: {@link XXXXXXStatement} instead Zbnf_ScriptElement (shorter). The Scriptelement
   *   is the component for the genContent-Elements now instead Zbnf_genContent. This class contains attributes of the
   *   content elements. Only if a sub content is need, an instance of Zbnf_genContent is created as {@link XXXXXXStatement#statementlist}.
   *   Furthermore the {@link XXXXXXStatement#statementlist} should be final because it is only created if need for the special 
   *   {@link XXXXXXStatement#elementType}-types (TODO). This version works for {@link org.vishia.stateMGen.StateMGen}.
   * <li>2012-10-11 Hartmut chg Syntax changed of ZmakeGenCtrl.zbnf: dataAccess::={ <$?path>? \.}. 
   *   instead dataAccess::=<$?name>\.<$?elementPart>., it is more universal. adapted. 
   * <li>2012-10-10 new: Some enhancements, it is used for {@link org.vishia.zbatch.ZbatchExecuter} now too.
   * <li>2011-03-00 created.
   *   It is the concept of specialized {@link GralWidget}.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
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
  static final public int version = 20130310;

  final MainCmdLogging_ifc console;

  final File fileScript;
  
  
  final Map<String, Subroutine> subroutinesAll = new TreeMap<String, Subroutine>();
  
  
  
  /**List of the script variables in order of creation in the jbat script file and all includes.
   * The script variables can contain inputs of other variables which are defined before.
   * Therefore the order is important.
   * This list is stored firstly in the {@link StatementList#statements} in an instance of 
   * {@link ZbnfZGenScript} and then transferred from all includes and from the main script 
   * to this container because the {@link ZbnfZGenScript} is only temporary and a ensemble of all
   * Statements should be present from all included files. The statements do not contain
   * any other type of statement than script variables because only ScriptVariables are admissible
   * in the syntax. Outside of subroutines and main there should only exist variable definitions. 
   */
  private final List<DefVariable> XXXlistScriptVariables = new ArrayList<DefVariable>();

  /**The script element for the whole file. It shall contain calling of <code><*subtext:name:...></code> 
   */
  Subroutine mainRoutine;
  
  protected ZGenitem checkZGenFile;
  
  /**The class which presents the script level. */
  ZGenClass scriptClass;
  
  //public String scriptclassMain;

  public ZGenScript(MainCmdLogging_ifc console, File fileScript)
  { this.console = console;
    this.fileScript = fileScript;

  }

  
  public void setMainRoutine(Subroutine mainRoutine){
    this.mainRoutine = mainRoutine;
  }
  
  public ZGenClass scriptClass(){ return scriptClass; }
  
  
  public void XXXsetFromIncludedScript(ZbnfZGenScript includedScript){
    if(includedScript.scriptfile.getMainRoutine() !=null){
      //use the last found main, also from a included script but firstly from main.
      mainRoutine = includedScript.scriptfile.getMainRoutine();   
    }
    if(includedScript.outer.scriptClass.statements !=null){
      for(ZGenitem item: includedScript.outer.scriptClass.statements){
        scriptClass.statements.add(item);
        if(item instanceof DefVariable){
          //listScriptVariables.add((DefVariable)item);
        }
      }
    }

  }
  
  public final Subroutine getMain(){ return mainRoutine; }
  
  
  public Subroutine getSubtextScript(CharSequence name){ return subroutinesAll.get(name.toString()); }
  
  
  public void writeStruct(Appendable out) throws IOException{
    //mainRoutine.writeStruct(0, out);
    scriptClass.writeStruct(0, out);
  }
  
  
  public List<DefVariable> XXXgetListScriptVariables(){ return XXXlistScriptVariables; }




  
  /**Common Superclass for a ZGen script item.
   * A script item is either a statement maybe with sub statements, or an expression, or an access to data
   * or a constant text. Therefore only one of the associations {@link #statementlist}, {@link #dataAccess},
   * {@link #expression} or {@link #textArg} is set.
   *
   */
  public static class ZGenitem
  {
    /**Designation what presents the element.
     * 
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>n</td><td>simple newline text</td></tr>
     * <tr><td>T</td><td>textual output to any variable or file</td></tr>
     * <tr><td>l</td><td>add to list</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.Filepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.Filepath, String)}</td></tr>
     * <tr><td>e</td><td>A datatext, from <*expression> or such.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #textArg}==null, {@link #statementlist} == null.</td></tr>
     * <tr><td>j</td><td>call of a static java method. {@link #identArgJbat}==its name, {@link #statementlist} == null.</td></tr>
     * <tr><td>c</td><td>cmd line invocation.</td></tr>
     * <tr><td>d</td><td>cd change current directory.</td></tr>
     * <tr><td>J</td><td>Object variable {@link #identArgJbat}==its name, {@link #statementlist} == null.</td></tr>
     * <tr><td>P</td><td>Pipe variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>U</td><td>Buffer variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>S</td><td>String variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>L</td><td>Container variable, a list</td></tr>
     * <tr><td>W</td><td>Opened file, a Writer in Java</td></tr>
     * <tr><td>=</td><td>assignment of an expression to a variable.</td></tr>
     * <tr><td>B</td><td>statement block</td></tr>
     * <tr><td>C</td><td><:for:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>E</td><td><:else> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>F</td><td><:if:condition:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>G</td><td><:elsif:condition:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>w</td><td>while(cond) {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>b</td><td>break</td></tr>
     * <tr><td>?</td><td><:if:...?gt> compare-operation in if</td></tr>
     * 
     * <tr><td>Z</td><td>a target,</td></tr>
     * <tr><td>Y</td><td>the file</td></tr>
     * <tr><td>xxxX</td><td>a subtext definition</td></tr>
     * </table> 
     */
    protected char elementType;    
    
    /**Hint to the source of this parsed argument or statement. */
    int srcLine, srcColumn;
    
    /**Necessary for throwing exceptions with the {@link StatementList#srcFile} in its text. */
    final StatementList parentList;
    
    /**If need, sub statements, maybe null. An argument may need a StatementList
     * to calculate the value of the argument if is is more complex. Alternatively
     * an Argument can be calculated with the {@link #expression} or with {@link #dataAccess}
     * or it is a simple {@link #textArg}.*/
    public StatementList statementlist;
    
    /**Any access to an Object, maybe content of a variable, maybe access to any Java data,
     * maybe invocation of a Java routine. */
    public DataAccess dataAccess;
  
    /**Any calculation of data. */
    public CalculatorExpr expression;

    /**From Zbnf <""?textInStatement>, constant text, null if not used. */
    public String textArg; 
    
    
    ZGenitem(StatementList parentList, char whatisit){
      if(parentList == null)
        Assert.stop();
      this.parentList = parentList;
      this.elementType = whatisit;
    }
    
    /*package private*/ char elementType(){ return elementType; }
    
    public StatementList statementlist(){ return statementlist; }

    
    /**From ZBNF. If this method was found, the line will be stored.
     * See {@link org.vishia.zbnf.ZbnfJavaOutput}
     * @param line
     */
    public void set_inputLine_(int line){ srcLine = line; }
    
    
    
    public ZGenDataAccess new_dataAccess() { 
      assert(statementlist == null && dataAccess == null && expression == null && textArg == null);
      return new ZGenDataAccess(); 
    }
    
    public void add_dataAccess(ZGenDataAccess val){ 
      dataAccess = val;
    }
    



    public void set_text(String text) { 
      assert(statementlist == null && dataAccess == null && expression == null && textArg == null);
      CharSequence cText;
      if(text.contains("\n=")){   //= on start of line, remove it
        StringBuilder u = new StringBuilder(text);
        cText = u;
        int pos = 0;
        while( (pos = u.indexOf("\n=",pos))>=0){
          u.replace(pos+1, pos+2, "");
        }
      } else {
        cText = text;
      }
      textArg = cText.toString(); //StringSeq.create(cText, true);  //let the text inside the StringBuilder.
      //if(statementlist == null){ statementlist = new StatementList(this); }
      //statementlist.set_text(text);
    }
    
    public StatementList new_statementBlock(){
      assert(statementlist == null && dataAccess == null && expression == null && textArg == null);
      statementlist = new StatementList(this);
      return statementlist;
    }
    
    public void add_statementBlock(StatementList val){ }
    
    

    /**From Zbnf, a part <:>...<.> */
    public StatementList new_textExpr() { 
      assert(statementlist == null && dataAccess == null && expression == null && textArg == null);
      return this.statementlist = new StatementList(); 
    }
    
    public void add_textExpr(StatementList val){}

    public ZGenCalculatorExpr new_numExpr() { 
      assert(statementlist == null && dataAccess == null && expression == null && textArg == null);
      return new ZGenCalculatorExpr(this); 
    }

    public void add_numExpr(ZGenCalculatorExpr val){ 
      DataAccess dataAccess = val.onlyDataAccess();
      if(dataAccess !=null){
        this.dataAccess = dataAccess;
      } else {
        val.closeExprPreparation();
        this.expression = val.expr; 
      }
    }
    
    
    public ZGenCalculatorExpr new_boolExpr() { 
      assert(statementlist == null && dataAccess == null && expression == null && textArg == null);
      return new ZGenCalculatorExpr(this); 
    }

    public void add_boolExpr(ZGenCalculatorExpr val){ 
      DataAccess dataAccess = val.onlyDataAccess();
      if(dataAccess !=null){
        this.dataAccess = dataAccess;
      } else {
        val.closeExprPreparation();
        this.expression = val.expr; 
      }
    }
    
    
    static String sindentA = "                                                                               "; 
    
    /**Writes a complete readable information about this item with all nested information.
     * @param indent
     * @param out
     * @throws IOException
     */
    final void writeStruct(int indent, Appendable out) throws IOException{
      String sIndent= (2*indent < sindentA.length()-2) ? sindentA.substring(0, 2*indent) : sindentA;
      out.append(sIndent);
      writeStructLine(out);
      writeStructAdd(indent, out);
      if(textArg !=null){
        out.append("\"").append(textArg).append("\"");
      }
      if(dataAccess !=null){
        dataAccess.writeStruct(out);
      }
      if(expression !=null){
        String sExpr = expression.toString();
        out.append(sExpr);
      }
      out.append("\n");
      if(statementlist !=null){
        for(ZGenitem item: statementlist.statements){
          item.writeStruct(indent+1, out);
        }
      }
    }
    
    /**Prepares information in following lines if necessary. It is possible to append in the only one line too.
     * This routine have to be append a newline at last.
     * @param indent
     * @param out
     * @throws IOException
     */
    void writeStructAdd(int indent, Appendable out) throws IOException{  }
    
    
    /**Prepares the information about the ZGenitem in one line. A newline is not appended here! 
     * This routine is called in {@link #toString()} and in {@link #writeStruct(int, Appendable)}.
     * It should be called in all overridden routines with super.writeStructLine
     * for the derived statement types. 
     * @param u 
     * @throws IOException
     */
    void writeStructLine(Appendable u) throws IOException {
      u.append(" @").append(Integer.toString(srcLine)).append(",").append(Integer.toString(srcColumn)).append(':').append(elementType);
      switch(elementType){
        case 't': u.append(" text \"").append(textArg).append("\""); break;
        /*
        case 'S': u.append("String " + identArgJbat;
        case 'J': u.append("Obj " + identArgJbat;
        case 'P': u.append("Pipe " + identArgJbat;
        case 'U': u.append("Buffer " + identArgJbat;
        case 'o': u.append("(?outp." + textArg + "?)";
        case 'i': u.append("(?inp." + textArg + "?)";
        */
        case 'e': u.append(" <*)"); break;  //expressions.get(0).dataAccess
        //case 'g': u.append("<$" + path + ">";
        //case 's': u.append("call " + identArgJbat;
        case 'B': u.append(" { statementblock }"); break;
        case 'I': u.append(" (?forInput?)...(/?)"); break;
        case 'L': u.append(" (?forList "  + "?)"); break;
        case 'C': u.append(" <:for:Container "  + "?)"); break;
        case 'f': u.append(" if "); break;
        case 'F': u.append(" Filepath "); break;
        case 'G': u.append(" Fileset "); break;
        case 'g': u.append(" elsif "); break;
        case 'N': u.append(" <:hasNext> content <.hasNext>"); break;
        case 'E': u.append(" else "); break;
        case 'Y': u.append(" <:file> "); break;
        case 'b': u.append(" break; "); break;
        case 'c': u.append(" cmd "); break;
        case 'm': u.append(" move "); break;
        case 'x': u.append(" thread "); break;
        case 'y': u.append(" copy "); break;
        case 'z': u.append(" exit "); break;
        //case 'X': u.append(" call " + identArgJbat ;
        default: //do nothing. Fo in overridden method.
      }

    }
    
    
    @Override public String toString(){
      StringBuilder u = new StringBuilder();
      try{ writeStructLine(u); } catch(IOException exc){} //append on StringBuilder has not a IOException!
      return u.toString();
    }

  }
  
  
  
  
  
  
  /**Argument for a subroutine. It has a name. 
   *
   */
  public static class Argument extends ZGenitem  { //CalculatorExpr.Datapath{
    
    /**Name of the argument. It is the key to assign calling argument values. */
    public String identArgJbat;
    
    /**Set whether the argument is a filepath. The the references of ZGenitem are null.*/
    protected Filepath filepath;
   
    public Argument(StatementList parentList){
      super(parentList, '.');
    }
    
    public void set_name(String name){ this.identArgJbat = name; }
    
    public String getIdent(){ return identArgJbat; }
    
    @Override public String toString(){
      return identArgJbat + " = " + super.toString();
    }
 
    
    /**From ZBNF: The argument is given with <code>Filepath name = "a path"</code>. */
    public ZbnfFilepath new_Filepath(){ return new ZbnfFilepath(); }
    
    public void add_Filepath(ZbnfFilepath valz){ filepath =valz.filepath; }
    

    
  }
  
 

  
  public static class ZGenDataAccess extends DataAccess.DataAccessSet {

    
    public ZGenDataAccess(){ super(); }
    
    @Override public final ZGenDatapathElement new_datapathElement(){ return new ZGenDatapathElement(); }

    public final void add_datapathElement(ZGenDatapathElement val){ 
      super.add_datapathElement(val); 
    }
    
    
  }
  
  
  
  
  public static class ZGenDatapathElement extends DataAccess.SetDatapathElement {

    
    /**Expressions to calculate the {@link #fnArgs}.
     * The arguments of a subroutine can be given directly, then the expression is not necessary
     * and this reference is null.
     */
    protected List<ZGenitem> fnArgsExpr;

    public ZGenDatapathElement(){ super(); }
    
    /**Creates a new Expression Set instance to add any properties of an expression.
     * This method is used especially by {@link org.vishia.zbnf.ZbnfJavaOutput} to set
     * results from the parser. This method may be overridden for enhanced capabilities.
     * @return
     */
    public ZGenitem new_argument(){
      //ObjExpr actualArgument = new ObjExpr(new_CaluclatorExpr());
      ZGenitem actualArgument = new ZGenitem(null, '+');
      //ScriptElement actualArgument = new ScriptElement('e', null);
      //ZbnfDataPathElement actualArgument = new ZbnfDataPathElement();
      return actualArgument;
    }
    
    /**From Zbnf.
     * The Arguments of type {@link Statement} have to be resolved by evaluating its value in the data context. 
     * The value is stored in {@link DataAccess.DatapathElement#addActualArgument(Object)}.
     * See {@link #add_datapathElement(org.vishia.util.DataAccess.DatapathElement)}.
     * @param val The Scriptelement which describes how to get the value.
     */
    public void add_argument(ZGenitem val){ 
      if(fnArgsExpr == null){ fnArgsExpr = new ArrayList<ZGenitem>(); }
      fnArgsExpr.add(val);
    } 

    
    public void writeStruct(int indent, Appendable out) throws IOException {
      out.append(ident).append(':').append(whatisit);
      if(fnArgsExpr!=null){
        String sep = "(";
        for(ZGenitem arg: fnArgsExpr){
          arg.writeStruct(indent+1, out);
        }
      }
    }
  }
  
  
  public static class ZGenCalculatorExpr extends CalculatorExpr.SetExpr {
    
    ZGenCalculatorExpr(Object dbgParent){ super(true, dbgParent); }
    
    /**It creates an {@link ZGenDataAccess} because overridden {@link #newDataAccessSet()}
     * of {@link CalculatorExpr.SetExpr#newDataAccessSet()} 
     */
    @Override public ZGenDataAccess new_dataAccess(){ 
      return (ZGenDataAccess)super.new_dataAccess();  
    }

    public void add_dataAccess(ZGenDataAccess val){ }

    @Override protected ZGenDataAccess newDataAccessSet(){ return new ZGenDataAccess(); }
    
    
  }
  
  
  
  
  
  /**A < fileset> in the ZmakeStd.zbnf. It is assigned to a script variable if it was created by parsing the ZmakeUserScript.
   * If the fileset is used in a target, it is associated to the target to get the absolute paths of the files
   * temporary while processing that target.
   * <br><br>
   * The Zbnf syntax for parsing is defined as
   * <pre>
   * fileset::= { basepath = <file?basepath> | <file> ? , }.
   * </pre>
   * The <code>basepath</code> is a general path for all files which is the basepath (in opposite to localpath of each file)
   * or which is a pre-basepath if any file is given with basepath.
   * <br><br>
   * Uml-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *               UserFileset
   *                    |------------commonBasepath-------->{@link Filepath}
   *                    |
   *                    |------------filesOfFileset-------*>{@link Filepath}
   *                                                        -drive:
   *                                                        -absPath: boolean
   *                                                        -basepath
   *                                                        -localdir
   *                                                        -name
   *                                                        -someFiles: boolean
   *                                                        -ext
   * </pre>
   * 
   */
  public static class UserFileset extends DefVariable
  {
    
    final ZGenScript script;
    
    /**From ZBNF basepath = <""?!prepSrcpath>. It is a part of the base path anyway. It may be absolute, but usually relative. 
     * If null then unused. */
    Filepath commonBasepath;
    
    /**From ZBNF srcext = <""?srcext>. If null then unused. */
    //public String srcext;
    
    
    /**All entries of the file set how it is given in the users script. */
    List<Filepath> filesOfFileset = new LinkedList<Filepath>();
    
    UserFileset(StatementList parentList, ZGenScript script){
      super(parentList, 'G');
      this.script = script;
    }
    
    
    public UserFileset(StatementList parentList){
      super(parentList, 'G');
      this.script = null;
    }
    
    
    public void set_name(String name){
      DataAccess.DataAccessSet dataAccess1 = new DataAccess.DataAccessSet();
      this.dataAccess = dataAccess1;
      dataAccess1.set_startVariable(name);
    }
    
    
    /**From Zbnf: srcpath = <""?!prepSrcpath>.
     * It sets the base path for all files of this fileset. This basepath is usually relative.
     * @return ZBNF component.
     */
    public ZbnfFilepath new_commonpath(){ return new ZbnfFilepath(); }  //NOTE: it has not a parent. this is not its parent!
    public void set_commonpath(ZbnfFilepath val){ commonBasepath = val.filepath; }
    
    /**From ZBNF: < Filepath>. */
    public ZbnfFilepath new_Filepath(){ return new ZbnfFilepath(); }
    
    /**From ZBNF: < file>. */
    public void add_Filepath(ZbnfFilepath valz){ 
      Filepath val =valz.filepath;
      if(val.basepath !=null || val.localdir.length() >0 || val.name.length() >0 || val.drive !=null){
        //only if any field is set. not on empty val
        filesOfFileset.add(val); 
      }
    }
    
    
      
    @Override
    public String toString(){ 
      StringBuilder u = new StringBuilder();
      if(commonBasepath !=null) u.append("basepath="+commonBasepath+", ");
      u.append(filesOfFileset);
      return u.toString();
    }
  }
  
  

  
  public static class DefFilepath extends DefVariable
  {
    Filepath filepath;

    DefFilepath(StatementList parentList){
      super(parentList, 'F');
    }
  
    public ZbnfFilepath new_Filepath(){ return new ZbnfFilepath(); } 
    
    public void add_Filepath(ZbnfFilepath val){ filepath = val.filepath; } 
    
  }
 
  
  
  /**The parsed content of a Filepath in a ZGen script.
   */
  public final static class Filepath {
  
  
    /**From ZBNF: $<$?scriptVariable>. If given, then the basePath() starts with it. 
     */
    protected String scriptVariable, envVariable;
    
    /**The drive letter if a drive is given. */
    String drive;
    
    /**From Zbnf: [ [/|\\]<?@absPath>]. Set if the path starts with '/' or '\' maybe after drive letter. */
    protected boolean absPath;
    
    /**Path-part before a ':'. It is null if the basepath is not given. */
    protected String basepath;
    
    /**Localpath after ':' or the whole path. It is an empty "" if a directory is not given. 
     * It does not contain a slash on end. */
    protected String localdir = "";
    
    /**From Zbnf: The filename without extension. */
    protected String name = "";
    
    
    /**From Zbnf: The extension inclusive the leading dot. */
    protected String ext = "";
    
    /**Set to true if a "*" was found in any directory part.*/
    protected boolean allTree;
    
    /**Set to true if a "*" was found in the name or extension.*/
    protected boolean someFiles;
    

  
  
  }
  
  
  
  /**This class is used only temporary while processing the parse result into a instance of {@link Filepath}
   * while running {@link ZbnfJavaOutput}. 
   */
  public static class ZbnfFilepath{
    
    /**The instance which are filled with the components content. It is used for the user's data tree. */
    final Filepath filepath;
    
    
    ZbnfFilepath(){
      filepath = new Filepath();
    }
    
    /**FromZbnf. */
    public void set_drive(String val){ filepath.drive = val; }
    
    
    /**FromZbnf. */
    public void set_absPath(){ filepath.absPath = true; }
    
    /**FromZbnf. */
    public void set_scriptVariable(String val){ filepath.scriptVariable = val; }
    
    
    /**FromZbnf. */
    public void set_envVariable(String val){ filepath.envVariable = val; }
    
    

    
    //public void set_someFiles(){ someFiles = true; }
    //public void set_wildcardExt(){ wildcardExt = true; }
    //public void set_allTree(){ allTree = true; }
    
    /**FromZbnf. */
    public void set_pathbase(String val){
      filepath.basepath = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_path(String val){
      filepath.localdir = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_name(String val){
      filepath.name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.someFiles |= val.indexOf('*')>=0;
    }
    
    /**FromZbnf. If the name is empty, it is not the extension but the name.*/
    public void set_ext(String val){
      if(val.equals(".") && filepath.name.equals(".")){
        filepath.name = "..";
        //filepath.localdir += "../";
      }
      else if((val.length() >0 && val.charAt(0) == '.') || filepath.name.length() >0  ){ 
        filepath.ext = val;  // it is really the extension 
      } else { 
        //a file name is not given, only an extension is parsed. Use it as file name because it is not an extension!
        filepath.name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      }
      filepath.someFiles |= val.indexOf('*')>=0;
    }
    

  }
  
  

  public static class Zmake extends CallStatement {

    
    Filepath output;
    
    List<ZmakeInput> input = new ArrayList<ZmakeInput>();
    
    Zmake(StatementList parentList)
    { super(parentList, 'Z');
    }
    
    
    public ZbnfFilepath new_output(){
      return new ZbnfFilepath();
    }
    
    public void add_output(ZbnfFilepath val){ output = val.filepath; }
    
    
    public ZmakeInput new_zmakeInput(){ return new ZmakeInput(); }
    
    public void add_zmakeInput(ZmakeInput val){ input.add(val); };
    
    
  }
  
  
  
  public static class ZmakeInput {
    
    public String zmakeFilesetName;
    
    Filepath zmakeInputDir;
    
    
    public ZbnfFilepath new_zmakeInputDir(){ return new ZbnfFilepath(); }
    
    public void add_zmakeInputDir(ZbnfFilepath val){ zmakeInputDir = val.filepath; }
  }
  
  
  
  /**An element of the script, maybe a simple text, an condition etc.
   * It may have sub statements , see aggregation {@link #statementlist}. 
   * <br>
   * UML-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *   Statement                 Statements          Statement
   *        |                         |              !The sub statements
   *        |-----statementlist------>|                  |
   *        |                         |                  |
   *                                  |----statements--*>|
   * 
   * </pre> 
   */
  public static class XXXXXXStatement extends ZGenitem //Argument
  {
    
    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex dataAccess
     * null if not used. */
    public List<DataAccess> assignObjs;
    
    
    /**The variable which should be created or to which a value is assigned to. */
    public DataAccess variable;
    
    //public String value;
    
    //public List<String> path;
    
    
    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    public List<Argument> arguments;
    
    /**The statements in this sub-ScriptElement were executed if an exception throws
     * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
     * If it is null, no exception handling is done.
     * <br><br>
     * This block can contain any statements as error replacement. If they fails too,
     * the iferror-Block can contain an iferror too.
     * 
     */
    public List<Onerror> onerror;
    
    

    
    public XXXXXXStatement(StatementList parentList, char whatisit) //, StringSeq text)
    { super(parentList, whatisit);
      //this.textArg = text;
      if("BNXYZvl".indexOf(whatisit)>=0){
        statementlist = new StatementList(this);
      }
      else if("IVL".indexOf(whatisit)>=0){
        statementlist = new StatementList(this);
      }
    }
    
    public List<Argument> getReferenceDataSettings(){ return arguments; }
    
    public StatementList getSubContent(){ return statementlist; }
    
    //public void set_name(String name){ this.identArgJbat = name; }
    
    
    
    /**Sets the nonEmptyText From ZBNF. invokes {@link #set_textReplLf(String)} if the text contains
     * other characters as white spaces. 
     */
    public void XXXXset_plainText(String text){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.set_plainText(text);      
    }

    
    
    /**Gathers a text which is assigned to any variable or output. <+ name>text<.+>
     */
    public XXXXXXStatement new_textOut(){ return new XXXXXXStatement(parentList, 'T'); }

    public void add_textOut(XXXXXXStatement val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val); 
    } 
    
    
    public void XXXXXset_newline(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new XXXXXXStatement(parentList, 'n'));   /// 
    }
    
    
    
    public DefVariable new_setEnvVar(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_setEnvVar(); 
    }

    public void add_setEnvVar(DefVariable val){ statementlist.add_setEnvVar(val); } 
    
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public DefVariable new_textVariable(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new DefVariable(parentList, 'S'); 
    } 

    public void add_textVariable(DefVariable val){ statementlist.statements.add(val); statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);} 
    
    
    /**Defines a variable which is able to use as pipe.
     */
    public DefVariable new_Pipe(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new DefVariable(parentList, 'P'); 
    } 

    public void add_Pipe(DefVariable val){ statementlist.statements.add(val); statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val); }
    
    /**Defines a variable which is able to use as String buffer.
     */
    public DefVariable new_Stringjar(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new DefVariable(parentList, 'U'); 
    } 

    public void add_Stringjar(DefVariable val){ statementlist.statements.add(val);  statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);}
    
        
    /**Defines a variable which is able to use as container.
     */
    public DefVariable new_List(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new DefVariable(parentList, 'L'); 
    } 

    public void add_List(DefVariable val){ statementlist.statements.add(val);  statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as Appendable, it is a Writer.
     */
    public DefVariable new_Openfile(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new DefVariable(parentList, 'W'); 
    } 

    public void add_Openfile(DefVariable val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);  
      statementlist.onerrorAccu = null; 
      statementlist.withoutOnerror.add(val);
    }
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public DefVariable new_objVariable(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new DefVariable(parentList, 'J'); 
    } 

    public void add_objVariable(DefVariable val){ statementlist.statements.add(val); statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);}
    
    
        
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_p1(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_p1(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      if(arguments.size() >=1){
        arguments.set(0,val);
      } else {  //size is 0
        arguments.add(val);
      }
    }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_p2(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_p2(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      if(arguments.size() >=2){
        arguments.set(1,val);
      } else {
        while(arguments.size() < 1){ arguments.add(null); }  //empty
        arguments.add(val);
      }
    }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_actualArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      arguments.add(val); }
    
    
    /**From Zbnf: [{ <dataAccess?-assign> = }] 
     */
    public ZGenDataAccess new_assign(){ return new ZGenDataAccess(); }
    
    public void add_assign(ZGenDataAccess val){ 
      if(variable == null){ variable = val; }
      else {
        if(assignObjs == null){ assignObjs = new LinkedList<DataAccess>(); }
        assignObjs.add(val); 
      }
    }

    
    /**From Zbnf: < variable?defVariable> 
     */
    public ZGenDataAccess new_defVariable(){ return new ZGenDataAccess(); }
    
    public void add_defVariable(ZGenDataAccess val){   
      int whichStatement = "SPULJW".indexOf(elementType);
      char whichVariableType = "SPULOA".charAt(whichStatement);
      val.setTypeToLastElement(whichVariableType);
      //don't use the dataPath, it may be the path to the initial data.
      variable = val;
      //if(assignObjs == null){ assignObjs = new LinkedList<DataAccess>(); }
      //assignObjs.add(val); 
    }

     
    public XXXXXXStatement new_assignExpr(){ 
      return new XXXXXXStatement(parentList, '='); 
    } 

    public void add_assignExpr(XXXXXXStatement val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);  
      statementlist.onerrorAccu = null; 
      statementlist.withoutOnerror.add(val);
    }
    
    
    
    
    public void set_append(){
      if(elementType == '='){ elementType = '+'; }
      else throw new IllegalArgumentException("ZGenScript - unexpected set_append");
    }
    
    
    public ZGenitem new_debug()
    { if(statementlist == null) { statementlist = new StatementList(this); }
      return statementlist.new_debug();
    }
    
    public void add_debug(ZGenitem val){statementlist.add_debug(val); }

    
    
    public Onerror new_onerror(){
      return new Onerror(parentList);
    }
    

    public void add_onerror(Onerror val){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);
      /*
      if(statementlist.onerrorAccu == null){ statementlist.onerrorAccu = new LinkedList<Onerror>(); }
      for( ZGenitem previousStatement: statementlist.withoutOnerror){
        previousStatement.onerror = onerror;  
        //use the same onerror list for all previous statements without error designation.
      }
      */
      statementlist.withoutOnerror.clear();  //remove all entries, they are processed.
    }

    
    public void set_breakBlock(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      XXXXXXStatement statement = new XXXXXXStatement(parentList, 'b');
      statementlist.statements.add(statement);
    }
    
    
    public void add_hasNext(XXXXXXStatement val){}

    public XXXXXXStatement new_elseBlock()
    { StatementList subGenContent = new StatementList(this);
      XXXXXXStatement statement = new XXXXXXStatement(parentList, 'E');
      statement.statementlist = subGenContent;  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_elseBlock(XXXXXXStatement val){}

    
    
    
    public ThreadBlock new_threadBlock()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_threadBlock();
    }
    
    public void add_threadBlock(ThreadBlock val){statementlist.add_threadBlock(val);}

    
    

    

    public ZGenitem new_cd()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_cd();
    }
    
    
    public void add_cd(ZGenitem val)
    { statementlist.add_cd(val);
    }
    
    

    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?)
    public void set_fnEmpty(String val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'f', StringSeq.create(val));
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
    }
    
    public void set_outputValue(String text){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'o', StringSeq.create(text));
      statementlist.statements.add(statement); 
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
    }
    
    public void set_inputValue(String text){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'i', StringSeq.create(text));
      statementlist.statements.add(statement); 
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
    }
    
    //public void set_variableValue(String text){ subContent.content.add(new ScriptElement('v', text)); }
    
    public Statement new_forInputContent()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'I');
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forInputContent(Statement val){}

    
    public Statement xxxnew_forVariable()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'V');
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void xxxadd_forVariable(Statement val){} //empty, it is added in new_forList()
*/

    
    
    public XXXXXXStatement new_forList()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      XXXXXXStatement statement = new XXXXXXStatement(parentList, 'L');
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forList(XXXXXXStatement val){} //empty, it is added in new_forList()

    
    public XXXXXXStatement new_addToList(){ 
      XXXXXXStatement subGenContent = new XXXXXXStatement(parentList, 'l');
      statementlist.addToList.add(subGenContent.statementlist);
      return subGenContent;
    }
   
    
    public void add_addToList(XXXXXXStatement val)
    {
    }

    
    public void set_exitScript(int val){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.set_exitScript(val);
    }
    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void axxxdd_fnEmpty(XXXXXXStatement val){  }
    

    
    @Override public String toString()
    {
      switch(elementType){
      case 't': return "text"; //textArg.toString();
      /*
      case 'S': return "String " + identArgJbat;
      case 'J': return "Obj " + identArgJbat;
      case 'P': return "Pipe " + identArgJbat;
      case 'U': return "Buffer " + identArgJbat;
      case 'o': return "(?outp." + textArg + "?)";
      case 'i': return "(?inp." + textArg + "?)";
      */
      case 'e': return "<*" +   ">";  //expressions.get(0).dataAccess
      //case 'g': return "<$" + path + ">";
      //case 's': return "call " + identArgJbat;
      case 'B': return "{ statementblock }";
      case '?': return "onerror";
      case 'I': return "(?forInput?)...(/?)";
      case 'L': return "(?forList "  + "?)";
      case 'C': return "<:for:Container "  + "?)";
      case 'f': return "if";
      case 'g': return "elsif";
      case 'N': return "<:hasNext> content <.hasNext>";
      case 'E': return "else";
      case 'Y': return "<:file>";
      case 'b': return "break;";
      case 'c': return "cmd;";
      case 'm': return "move;";
      case 'x': return "thread";
      case 'y': return "copy";
      case 'z': return "exit";
      case '=': return "assignExpr";
      case '+': return "appendExpr";
      //case 'X': return "call " + identArgJbat ;
      default: return "(??" + elementType + " " + "?)";
      }
    }
    
    
  }

  
  /**In ZBNF: <*dataAccess:formatString>
   */
  public static class DataText extends ZGenitem
  {
    public DataText(StatementList parentList)
    { super(parentList, 'e');
    }

    public String format;
    
    public void set_formatText(String text){ this.format = text; }
    

  }
  
  

  
  
  
  public static class DefVariable extends ZGenitem
  {
    
    /**The variable which should be created. 
     * The variable maybe build with name.subname.subname. 
     * It is possible to add an element to an internal container in Java data. 
     */
    public DataAccess defVariable;
    
    boolean bConst;
    
    DefVariable(StatementList parentList, char type){
      super(parentList, type);
    }
    
    
    /**From Zbnf: [ const <?const>] */
    public void set_const(){ bConst = true; } 
    
    /**From Zbnf: < variable?defVariable> inside a DefVariable::=...
     */
    public ZGenDataAccess new_defVariable(){ return new ZGenDataAccess(); }
    
    public void add_defVariable(ZGenDataAccess val){   
      int whichStatement = "SPULJKQWMCFG".indexOf(elementType);
      char whichVariableType = "SPULOKQAMOFG".charAt(whichStatement);  //from elementType to variable type.
      if(bConst){
        whichVariableType = Character.toLowerCase(whichVariableType);  //see DataAccess.access
      }
      val.setTypeToLastElement(whichVariableType);
      defVariable = val;
    }

    //public void new_statementBlock(){}
    
    //public void set_name(String val){}
    
    /**Returns the simple variable name if the variable is on one level only.
     * Returns name.name for more levels.
     * @return
     */
    String getVariableIdent(){
      final String name; 
      List<DataAccess.DatapathElement> path = defVariable.datapath();
      int zpath = path.size();
      if(path == null || zpath ==0){
        name = null;
      }
      else if(path.size() == 1){
        name = defVariable.datapath().get(0).ident();
      } else {
        name = null;  //TODO name.name
      }
      return name;
    }
    
    
    @Override void writeStructLine(Appendable out) throws IOException {
      super.writeStructLine(out);
      out.append(" Defvariable ").append(defVariable.toString());
    }

    @Override public String toString(){ return elementType + "=" + "DefVariable " + ":"+ defVariable; }
    
  };
  
  
  
  
  public static class AssignExpr extends ZGenitem
  {

    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex datapath
     * null if not used. */
    public List<ZGenDataAccess> assignObjs;
    
    
    /**The variable which should be created or to which a value is assigned to. */
    public ZGenDataAccess variable;
    
    AssignExpr(StatementList parentList, char elementType)
    { super(parentList, elementType);
    }
    
    /**From Zbnf: [{ <dataAccess?-assign> = }] 
     */
    public ZGenDataAccess new_assign(){ return new ZGenDataAccess(); }
    
    public void add_assign(ZGenDataAccess val){ 
      if(variable == null){ variable = val; }
      else {
        if(assignObjs == null){ assignObjs = new LinkedList<ZGenDataAccess>(); }
        assignObjs.add(val); 
      }
    }

    
    public void set_append(){
      if(elementType == '='){ elementType = '+'; }
      else throw new IllegalArgumentException("ZGenScript - unexpected set_append");
    }
    
    
    @Override void writeStructLine(Appendable out) throws IOException {
      super.writeStructLine(out);
      if(variable !=null){
        out.append(" assign ");
        variable.writeStruct(out);
        out.append(" = ");        
      } else {
        out.append(" invoke ");
      }
    }

    
    
    @Override public String toString(){ return  variable + " = " + super.toString(); }
  }
  
  
  public static class TextOut extends ZGenitem
  {

    /**The variable which should be created or to which a value is assigned to. */
    public ZGenDataAccess variable;
    
    int indent;
    
    TextOut(StatementList parentList, char elementType)
    { super(parentList, elementType);
    }
    
    
    
    public void set_inputColumn_(int col){ this.indent = col; } 
    
    /**From Zbnf: [{ <dataAccess?-assign> = }] 
     */
    public ZGenDataAccess new_assign(){ return new ZGenDataAccess(); }
    
    public void add_assign(ZGenDataAccess val){ 
      variable = val; 
    }

    public void set_newline(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new ZGenitem(parentList, 'n'));  
    }

    
    public void XXXXset_transscription(String val){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new ZGenitem(parentList, 'n'));  
    }
  }
  
  
  
  public static class TextColumn extends ZGenitem
  {
    /**The column where the current position is to be set. */
    final int column;
    
    /**If >=0, then at least this number of spaces are added on setColumn. 
     * The column is not be exact than, but existing text won't be not overridden.
     * If -1 then the setPosition may override existing text, but the column is exact.
     */
    int minChars = -1;
    
    TextColumn(StatementList parentList, int column)
    { super(parentList, '@');
      this.column = column;
    }

    @Override
    void writeStructAdd(int indent, Appendable out) throws IOException{ out.append(" setColumn ").append(Integer.toString(column)); }

  }
  
  
  
  
  public static class IfStatement extends ZGenitem
  {

    IfStatement(StatementList parentList, char whatisit)
    { super(parentList, whatisit);
    }
    
    public IfCondition new_ifBlock()
    { //StatementList subGenContent = new StatementList(this);
      IfCondition statement = new IfCondition(parentList, 'g');
      //statement.statementlist = subGenContent;  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_ifBlock(IfCondition val){}



    public StatementList new_elseBlock()
    { ZGenitem statement = new ZGenitem(parentList, 'E');
      statement.statementlist = new StatementList(this);  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement.statementlist;  //The else sub statementlist.
    }
    
    public void add_elseBlock(StatementList val){}

    
  }
  
  
  
  
  
  
  public static class CondStatement extends ZGenitem
  {
    
    public ZGenitem condition;

    //DataAccess conditionValue;
    
    CondStatement(StatementList parentList, char type){
      super(parentList, type);
    }

    /**From Zbnf: < condition>. A condition is an expression. It is the same like {@link #new_numExpr()}
     */
    public ZGenCalculatorExpr new_condition(){  
      condition = new ZGenitem(statementlist, '.');
      return condition.new_numExpr();
    }
    
    public void add_condition(ZGenCalculatorExpr val){ 
      condition.add_numExpr(val);
    }
    
  };
  
  
  
  public static class IfCondition extends CondStatement
  {
    
    public boolean bElse;
    
    IfCondition(StatementList parentList, char whatis){
      super(parentList, whatis);
    }
    
  }






  public static class ForStatement extends CondStatement
  {
    
    String forVariable;
    
    
    DataAccess forContainer;
    
    ForStatement(StatementList parentList, char type){
      super(parentList, type);
    }
    
    
    public void set_forVariable(String name){ this.forVariable = name; }

    
    public ZGenDataAccess new_forContainer() { 
      return new ZGenDataAccess(); 
    }
    
    public void add_forContainer(ZGenDataAccess val){ 
      forContainer = val;
    }
    


    
  };
  
  
  
  
  public static class Subroutine extends ZGenitem
  {
    public String name;
    
    
    public List<DefVariable> formalArgs;
    
    char type;
    
    Subroutine(StatementList parentList){
      super(parentList, 'X');
    }
    
    public void set_name(String name){ this.name = name; }

    
    public void XXXset_type(String val){
      type = val.charAt(0);
      if(type == 'S' && val.length() > 6){ type = 'U'; } //StringBuffer
      else if(type == 'O' && val.length()!=3){ type = 'A'; }  //Openfile
    }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Subroutine new_formalArgument(){ return this; } //new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_formalArgument(Subroutine val){}
    
    public DefVariable new_DefObjVar(){
      return new DefVariable(parentList, 'J'); 
    }
    
    public void add_DefObjVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    public DefVariable new_DefNumVar(){
      return new DefVariable(parentList, 'K'); 
    }
    
    public void add_DefNumVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_DefBoolVar(){
      return new DefVariable(parentList, 'Q'); 
    }
    
    public void add_DefBoolVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_textVariable(){
      return new DefVariable(parentList, 'S'); 
    }
    
    public void add_textVariable(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_DefMapVar(){
      return new DefVariable(parentList, 'M'); 
    }
    
    public void add_DefMapVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefFilepath new_DefFilepath(){ return new DefFilepath(this.parentList); } 

    public void add_DefFilepath(DefFilepath val){ 
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }
    
    

    
    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public DefVariable new_setEnvVar(){ 
      return new DefVariable(parentList, 'S'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_setEnvVar(DefVariable val){ 
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      //change the first identifier to $name
      val.defVariable.datapath().get(0).setIdent("$" + val.defVariable.datapath().get(0).ident());
      //val.identArgJbat = "$" + val.identArgJbat;
      formalArgs.add(val); 
    } 

    
    
    
    @Override void writeStructAdd(int indent, Appendable out) throws IOException{
      if(formalArgs !=null){
        for(DefVariable item: formalArgs){
          item.writeStruct(indent+1, out);
        }
      }
      out.append(")\n");
    }
    
    
    @Override void writeStructLine(Appendable out) throws IOException {
      super.writeStructLine(out);
      if(name==null){
        out.append(" main(");
      } else {
        out.append(" sub ").append(name).append("(");
      }
    }

    
  };
  
  
  
  public static class CallStatement extends AssignExpr
  {
    
    public ZGenitem callName;
    
    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    public List<Argument> actualArgs;
    

    
    CallStatement(StatementList parentList, char elementType){
      super(parentList, elementType);
    }
    
    public ZGenitem new_callName(){ return callName = new Argument(parentList); }
    
    public void set_callName(ZGenitem val){}
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_actualArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(Argument val){ 
      if(actualArgs == null){ actualArgs = new ArrayList<Argument>(); }
      actualArgs.add(val); }
    
    
    @Override void writeStructAdd(int indent, Appendable out) throws IOException{
      callName.writeStruct(0, out);
      if(actualArgs !=null){
        for(Argument item: actualArgs){
          item.writeStruct(indent+1, out);
        }
      }
      out.append("\n");
    }
    

  };
  
  
  
  
  /**class for a cmd execution (operation system cmd process invocation).
   * The assignExpr is used for stdout capturing.
   * @author hartmut
   *
   */
  public static class CmdInvoke extends AssignExpr
  {

    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    public List<ZGenitem> cmdArgs;
    
    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex datapath
     * null if not used. */
    public List<DataAccess> errorPipes;
    
    
    /**The variable which should be created or to which a value is assigned to. */
    public DataAccess errorPipe;
    

    /**The variable which should be created or to which a value is assigned to. */
    public DataAccess inputPipe;
    
    boolean bCmdCheck;

    public boolean bShouldNotWait;

    CmdInvoke(StatementList parentList, char elementType)
    { super(parentList, elementType);
    }
    
     /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public ZGenitem new_actualArgument(){ return new ZGenitem(parentList, '.'); }
     
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(ZGenitem val){ 
      if(cmdArgs == null){ cmdArgs = new ArrayList<ZGenitem>(); }
      cmdArgs.add(val); 
    }
    
    
    public void set_argsCheck(){ bCmdCheck = true; }

  }
  
  
  
  public static class ExitStatement extends ZGenitem
  {
    
    int exitValue;
    
    ExitStatement(StatementList parentList, int exitValue){
      super(parentList, 'z');
      this.exitValue = exitValue;
    }
  };
  
  
  
  public static class ThreadBlock extends ZGenitem
  {
    
    String XXXthreadName;    
    
    DataAccess threadVariable;
    
    ThreadBlock(StatementList parentList){
      super(parentList, 'x');
    }

    
    public void XXXset_name(String name){ this.XXXthreadName = name; }

    /**From Zbnf: [{ Thread <dataAccess?defThreadVar> = }] 
     */
    public ZGenDataAccess new_defThreadVar(){ 
      return new ZGenDataAccess(); 
    }
    
    public void add_defThreadVar(ZGenDataAccess val){ 
      val.setTypeToLastElement('T');
      threadVariable = val;
      //identArgJbat = "N";  //Marker for a new Variable.
    }

    
    /**From Zbnf: [{ Thread <dataAccess?assignThreadVar> = }] 
     */
    public ZGenDataAccess new_assignThreadVar(){ 
      return new ZGenDataAccess(); 
    }
    
    public void add_assignThreadVar(ZGenDataAccess val){ 
      threadVariable = val;
    }

    


  
  
  }
  
  
  
  /**This class contains expressions for error handling.
   * The statements in this sub-ScriptElement were executed if an exception throws
   * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
   * If it is null, no exception handling is done.
   * <br><br>
   * This block can contain any statements as error replacement. If they fails too,
   * the iferror-Block can contain an iferror too.
   * 
 */
  public final static class Onerror extends ZGenitem
  {
    /**From ZBNF. If not changed then it is not a cmd error type.*/
    public int errorLevel = Integer.MIN_VALUE;
    
    
    /**
     * <ul>
     * <li>'n' for notfound
     * <li>'f' file error
     * <li>'i' any internal exception.
     * <li>'?' any exception.
     * </ul>
     * 
     */
    public char errorType = '?';
    
    public void set_errortype(String type){
      errorType = type.charAt(0); //n, i, f
    }
 
    Onerror(StatementList parentList){
      super(parentList, '?');
    }
    
    /**Sets the statement to a cmd error execution.
     * See {@link ZGenExecuter.ExecuteLevel#execCmdError(Onerror)}.
     * This method is called in {@link StatementList#add_onerror(Onerror)}.
     */
    void setCmdError(){ elementType = '#'; }
    
    @Override public String toString(){
      if(elementType == '#') return "onerror " + errorLevel;
      else if(elementType == 'v') return "throwonerror " + errorLevel;
      else return "onerror " + errorType;
    }
    
 }
  
  
  
  
  
  /**Organization class for a list of script elements inside another Scriptelement.
   *
   */
  public static class StatementList
  {
    ZGenitem currStatement;
    
    /**Hint to the source of this parsed argument or statement. */
    String srcFile = "srcFile-yet-unknown";
    
    final ZGenitem parentStatement;
    
    /**True if < genContent> is called for any input, (?:forInput?) */
    //public final boolean XXXisContentForInput;
    
    public String cmpnName;
    
    public final List<ZGenitem> statements = new ArrayList<ZGenitem>();
    

    /**List of currently onerror statements.
     * This list is referenced in the appropriate {@link XXXXXXStatement#onerror} too. 
     * If an onerror statement will be gotten next, it is added to this list using this reference.
     * If another statement will be gotten next, this reference is cleared. So a new list will be created
     * for a later getting onerror statement. 
     */
    List<Onerror> onerrorAccu;

    
    List<ZGenitem> withoutOnerror = new LinkedList<ZGenitem>();
    
    
    /**True if the block {@link Argument#statementlist} contains at least one variable definition.
     * In this case the execution of the ScriptElement as a block should be done with an separated set
     * of variables because new variables should not merge between existing of the outer block.
     */
    public boolean bContainsVariableDef;

    
    int indentText;
    
    /**Scripts for some local variable. This scripts where executed with current data on start of processing this genContent.
     * The generator stores the results in a Map<String, String> localVariable. 
     * 
     */
    //private final List<ScriptElement> localVariableScripts = new ArrayList<ScriptElement>();
    
    public final List<StatementList> addToList = new ArrayList<StatementList>();
    
    //public List<String> datapath = new ArrayList<String>();
    
    public StatementList()
    { this.parentStatement = null;
      //this.isContentForInput = false;
    }
        
    public StatementList(ZGenitem parentStatement)
    { this.parentStatement = parentStatement;
      //this.isContentForInput = false;
    }
        
    /*
    public StatementList(Argument parentStatement, boolean isContentForInput)
    { this.parentStatement = parentStatement;
      //this.isContentForInput = isContentForInput;
    }
    */
        
    
    public void set_inputColumn_(int col){ this.indentText = col; } 
    

    
    public StatementList new_statementBlock(){
      ZGenitem statement = new ZGenitem(this, 'B');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement.statementlist = new StatementList(statement);
    }
    
    public void add_statementBlock(StatementList val){}

    

    
    
    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public ZGenitem new_debug(){
      return new ZGenitem(this, 'D'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_debug(ZGenitem val){ 
      statements.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    
    
    public void set_debug(){
      statements.add(new ZGenitem(this, 'D'));
    }
    
    /**Gathers a text which is assigned to any variable or output. <+ name>text<.+>
     */
    public TextOut new_textOut(){ return new TextOut(this, 'T'); }

    public void add_textOut(TextOut val){ 
      statements.add(val); 
    } 
    
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public DefVariable new_textVariable(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'S'); 
    } 

    public void add_textVariable(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);} 
    
    
    /**Defines a variable which is able to use as pipe.
     */
    public DefVariable new_Pipe(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'P'); 
    } 

    public void add_Pipe(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val); }
    
    /**Defines a variable which is able to use as String buffer.
     */
    public DefVariable new_Stringjar(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'U'); 
    } 

    public void add_Stringjar(DefVariable val){ statements.add(val);  onerrorAccu = null; withoutOnerror.add(val);}
    
        
    /**Defines a variable which is able to use as container.
     */
    public DefVariable new_List(){ ////
      bContainsVariableDef = true; 
      return new DefVariable(this, 'L'); 
    } 

    public void add_List(DefVariable val){ statements.add(val);  onerrorAccu = null; withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as container.
     */
    public DefVariable new_DefMapVar(){ ////
      bContainsVariableDef = true; 
      DefVariable statement = new DefVariable(this, 'M'); 
      statements.add(statement);  onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    } 

    public void add_DefMapVar(DefVariable val){ }
    
    /**Defines a variable which is able to use as Appendable, it is a Writer.
     */
    public DefVariable new_Openfile(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'W'); 
    } 

    public void add_Openfile(DefVariable val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    public DefFilepath new_DefFilepath(){
      bContainsVariableDef = true; 
      return new DefFilepath(this); 
    } 

    public void add_DefFilepath(DefFilepath val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    public UserFileset new_DefFileset(){
      bContainsVariableDef = true; 
      return new UserFileset(this); 
    } 

    public void add_DefFileset(UserFileset val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    public Zmake new_zmake(){
      bContainsVariableDef = true; 
      return new Zmake(this); 
    }
    
    public void add_zmake(Zmake val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public DefVariable new_DefObjVar(){ 
      bContainsVariableDef = true; 
      return new DefVariable(this, 'J'); 
    } 

    public void add_DefObjVar(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    
    public DefVariable new_DefNumVar(){ 
      bContainsVariableDef = true; 
      return new DefVariable(this, 'K'); 
    } 

    public void add_DefNumVar(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    
    public DefVariable new_DefBoolVar(){ 
      bContainsVariableDef = true; 
      return new DefVariable(this, 'Q'); 
    } 

    public void add_DefBoolVar(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public DefVariable new_setEnvVar(){ 
      return new DefVariable(this, 'S'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_setEnvVar(DefVariable val){ 
      //change the last identifier to $name
      List<DataAccess.DatapathElement> datapath = val.defVariable.datapath();
      int ix = datapath.size()-1;  //usual 0 if 1 element for simple set
      DataAccess.DatapathElement lastElement = datapath.get(ix);
      lastElement.setIdent("$" + lastElement.ident());
      statements.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    

    //public List<ScriptElement> getLocalVariables(){ return localVariableScripts; }
    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    public DataText new_dataText(){ return new DataText(this); }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    public void add_dataText(DataText val){ 
      statements.add(val);
      onerrorAccu = null; withoutOnerror.add(val);
    }
    
    /**Sets the textReplLf From ZBNF. 
     * Inside a <code>textExpr::=...<*|\<:|\<+|\<=|\<*|\<\.?textReplLf></code>.
     * The text is written in the source file, but the line feed character sequence may be another
     * int the generated text. Additional a left indent can be removed.
     * @param text
     */
    public void set_textReplLf(String text){
      CharSequence cText;
      if(text.contains("\nXXXX=")){
        StringBuilder u = new StringBuilder(text);
        cText = u;
        int pos = 0;
        while( (pos = u.indexOf("\nXXXX=",pos))>=0){
          u.replace(pos+1, pos+2, "");
        }
      } else {
        cText = text;
      }
      ZGenitem statement = new ZGenitem(this, 't');
      statement.textArg = text;
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    
    
    public void set_setColumn(int val){
      currStatement = new TextColumn(this, val);
      statements.add(currStatement);
      onerrorAccu = null; withoutOnerror.add(currStatement);
    }

    
    public void set_minChars(int val){
      ((TextColumn)currStatement).minChars = val;
    }

    
    /**Sets the plainText From ZBNF. invokes {@link #set_textReplLf(String)} if the text contains
     * other characters as white spaces. 
     */
    public void set_plainText(String text){
      //if(!StringFunctions.isEmptyOrOnlyWhitespaces(text)){
        set_textReplLf(text);
      //}
    }
    
    
    public void set_transcription(String val){
      ZGenitem statement = new ZGenitem(this, '\\');
      char cc = val.charAt(0);
      switch(cc){
        case 'n': statement.textArg = "\n"; break;
        case 'r': statement.textArg = "\r"; break;
        case 't': statement.textArg = "\t"; break;
        case '<': statement.textArg = "<"; break;
      }
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    public void set_newline(){
      ZGenitem statement = new ZGenitem(this, 'n');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    
    public AssignExpr new_assignExpr(){ 
      return new AssignExpr(this, '='); 
    } 

    public void add_assignExpr(AssignExpr val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    
    public ZGenitem new_throw(){ 
      return new ZGenitem(this, 'r'); 
    } 

    public void add_throw(ZGenitem val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    public void set_throwonerror(int val){ 
      Onerror statement = new Onerror(this);
      statement.elementType = 'v';
      statement.errorLevel = val;
      statements.add(statement);
    } 

    
    
    public Onerror new_onerror(){
      return new Onerror(this);
    }
    

    public void add_onerror(Onerror val){
      if(val.errorLevel != Integer.MIN_VALUE){
        val.setCmdError();
      }
      statements.add(val);
      /*
      if(statementlist.onerrorAccu == null){ statementlist.onerrorAccu = new LinkedList<Onerror>(); }
      for( ZGenitem previousStatement: statementlist.withoutOnerror){
        previousStatement.onerror = onerror;  
        //use the same onerror list for all previous statements without error designation.
      }
      */
      withoutOnerror.clear();  //remove all entries, they are processed.
    }

    

    public Onerror new_iferrorlevel(){
      return new Onerror(this);
    }
    
    public void add_iferrorlevel(Onerror val){
      val.setCmdError();
      statements.add(val);
    }


    public void set_breakBlock(){ 
      ZGenitem statement = new ZGenitem(this, 'b');
      statements.add(statement);
    }
    
 

    
    
    public IfStatement new_ifCtrl(){
      StatementList subGenContent = new StatementList(parentStatement);
      IfStatement statement = new IfStatement(this, 'f');
      statement.statementlist = subGenContent;  //The statement contains a genContent. 
      return statement;

    }

    
    public void add_ifCtrl(IfStatement val){
      statements.add(val);
      onerrorAccu = null; withoutOnerror.add(val);
      
    }

    
    
    public ZGenitem new_hasNext()
    { ZGenitem statement = new ZGenitem(this, 'N');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_hasNext(ZGenitem val){}
    
    

    
    

    /**for(name: iterable).
     * It builds a DefVariable, because it is similar. Variable is the for-variable.
     * @return 
     */
    public ForStatement new_forCtrl()
    { ForStatement statement = new ForStatement(this, 'C');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forCtrl(ForStatement val){}


    public CondStatement new_whileCtrl()
    { CondStatement statement = new CondStatement(this, 'w');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_whileCtrl(CondStatement val){}


    public CondStatement new_dowhileCtrl()
    { CondStatement statement = new CondStatement(this, 'u');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_dowhileCtrl(CondStatement val){}


    public ThreadBlock new_threadBlock()
    { ThreadBlock statement = new ThreadBlock(this);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_threadBlock(ThreadBlock val){}

    
    
    
    public CallStatement new_call()
    { CallStatement statement = new CallStatement(this, 's');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_call(CallStatement val){}

    

    public CmdInvoke new_cmdWait()
    { CmdInvoke statement = new CmdInvoke(this, 'c');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_cmdWait(CmdInvoke val){}

    
    public CmdInvoke new_cmdStart()
    { CmdInvoke statement = new CmdInvoke(this, 'c');
      statement.bShouldNotWait = true;
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_cmdStart(CmdInvoke val){}

    


    public CallStatement new_move()
    { CallStatement statement = new CallStatement(this, 'm');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_move(CallStatement val){}


    public CallStatement new_copy()
    { CallStatement statement = new CallStatement(this, 'y');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_copy(CallStatement val){}


    /*
    public void set_cd(String val)
    { ZGenitem statement = new ZGenitem(this, 'd');
      statement.textArg = StringSeq.create(val);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    */
    
    public ZGenitem new_cd(){
      ZGenitem statement = new ZGenitem(this, 'd');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    
    public void add_cd(ZGenitem val){}
    
    
    
    public void set_name(String name){
      cmpnName = name;
    }
    
    
    public void set_exitScript(int val){
      ExitStatement statement = new ExitStatement(this, val);
      statements.add(statement);
    }  
    
    
    public void XXXadd_dataAccess(String val)
    {
      //dataAccess.add(val);
    }

    
    @Override public String toString()
    { return "genContent name=" + cmpnName + ":" + statements;
    }
  }
  

  
  
  /**A class in the ZGen syntax.
   * The class can contain statements, which are variable definitions of the class variable. 
   * Therefore this class extends the StatementList.
   */
  public class ZGenClass extends StatementList
  {
    
    /**Sub classes of this class. */
    List<ZGenClass> classes;
    
    /**All subroutines of this class. */
    final Map<String, ZGenScript.Subroutine> subroutines = new TreeMap<String, ZGenScript.Subroutine>();
    
    protected ZGenClass(){}
    
    
    public final List<ZGenClass> classes(){ return classes; }
    
    public final Map<String, ZGenScript.Subroutine> subroutines(){ return subroutines; }
    
    public ZGenClass new_subClass(){ return new ZGenClass(); }
    
    public void add_subClass(ZGenClass val){ 
      if(classes == null){ classes = new ArrayList<ZGenClass>(); }
      classes.add(val); 
    }
    
    public Subroutine new_subroutine(){ return new Subroutine(this); }
    
    public void add_subroutine(Subroutine val){ 
      if(val.name == null){
        val.name = "main";
      }
      String sName = val.name.toString();
      subroutines.put(sName, val); 
      String nameGlobal = cmpnName == null ? sName : cmpnName + "." + sName;
      subroutinesAll.put(nameGlobal, val); 
    }
    
    
    
    public void writeStruct(int indent, Appendable out) throws IOException{
      
      if(statements !=null){
        for(ZGenitem item: statements){
          item.writeStruct(indent+1, out);
        }
      }
      for(Map.Entry<String, Subroutine> entry: subroutines.entrySet()){
        Subroutine sub = entry.getValue();
        sub.writeStruct(0, out);
      }
      //for(Map.Entry<String, ZGenClass> entry: classes.entrySet()){
      if(classes !=null){
        for(ZGenClass class1: classes){
          class1.writeStruct(indent+1, out);
        }
      }
    }

    
    
  }
  
  
  
  
  
  
  /**Main class for ZBNF parse result.
   * This class has the enclosing class to store {@link ZbatchGenScript#subroutinesAll}, {@link ZbatchGenScript#listScriptVariables}
   * etc. while parsing the script. The <code><:file>...<.file></code>-script is stored here locally
   * and used as the main file script only if it is the first one of main or included script. The same behaviour is used  
   * <pre>
   * ZmakeGenctrl::= { <target> } \e.
   * </pre>
   */
  public final static class ZbnfZGenScript extends ZGenClass
  {

    protected final ZGenScript outer;
    
    public Scriptfile scriptfile;    
    
    public ZbnfZGenScript(ZGenScript outer){
      outer.super();   //ZGenClass is non-static, enclosing is outer.
      this.outer = outer;
      outer.scriptClass = this; //outer.new ZGenClass();
    }
    
    public void set_include(String val){ 
      if(scriptfile.includes ==null){ scriptfile.includes = new ArrayList<String>(); }
      scriptfile.includes.add(val); 
    }
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public DefVariable new_scriptCurrdir(){
      bContainsVariableDef = true; 
      DefVariable variable = new DefVariable(this, 'S'); 
      variable.defVariable = new DataAccess("@currdir", 'S');
      return variable;
    } 

    public void add_scriptCurrdir(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);} 
    
    
    
    /**Any script file gets its own mainRoutine because the {@link #scriptfile} is one instance per parsed script file.
     * The lastly valid {@link ZGenScript#scriptFile} is set from the last processes file.
     * @return
     */
    public StatementList new_mainRoutine(){ 
      scriptfile.mainRoutine = new Subroutine(outer.scriptClass); 
      scriptfile.mainRoutine.statementlist = new StatementList(null);
      return scriptfile.mainRoutine.statementlist;
    }
    
    public void add_mainRoutine(StatementList val){  }
    
    
    
    public ZGenitem new_checkZGen(){ return new ZGenitem(this, '\0'); } 

    public void add_checkZGen(ZGenitem val){ outer.checkZGenFile = val; }

  }
  

  
  /**For one scriptfile, on include use extra instance per include.
   */
  public static class Scriptfile {
    public List<String> includes;
    
    /**The script element for the whole file of this script. 
     * It is possible that it is from a included script.
     * It shall contain calling of <code><*subtext:name:...></code> 
     */
    Subroutine mainRoutine;
    
    /**Returns the main routine which may be parsed in this maybe included script. */
    public Subroutine getMainRoutine(){ return mainRoutine; }
    
    
    
  }
  
  

}
