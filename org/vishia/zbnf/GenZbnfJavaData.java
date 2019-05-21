package org.vishia.zbnf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPreparer;

/**This class is used to generate two Java source files as container for parsed data derived from the zbnf syntax script.
 * @author hartmut
 *
 */
public class GenZbnfJavaData
{

  /**Version, history and license.
   * <ul>
   * <li>2019-05-14 Hartmut creation: 
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
   */
  public static final String sVersion = "2019-05-16";

  
  
  /**Command line args */
  protected static class Args{
    
    /**Cmdline-argument, set on -s option. */
    public File fileSyntax = null;
    
    public File dirJava;
    
    public String sJavaPkg;
    
    public String sJavaClass;
  
  }


  /**Command line args */
  private final Args args;

  private final MainCmdLogging_ifc log;
  
  
  /**Writer for the base data class and the Zbnf JavaOut class.
   * 
   */
  private Writer wr, wrz;

  /**StandardTypes. */
  protected final TreeMap<String, String> idxStdTypes = new TreeMap<String, String>();
  
  /**All parsed components from {@link ZbnfParser#listSubPrescript}. */
  protected TreeMap<String,ZbnfSyntaxPrescript> idxSubSyntax;
  
  
  /**The syntax components which are to process yet (are used for parse result storing).  */
  protected List<String> listCmpn = new ArrayList<String>();
  
  /**Index of already registered components to add in {@link #listCmpn} only one time. */
  protected Map<String, String> idxRegisteredCmpn = new TreeMap<String, String>();
  
  
  
  /**Text for Java header. */
  private final StringPreparer sJavaHead = new StringPreparer( 
      "package <&pkgpath>;\n"
    + "\n"
    + "import java.util.ArrayList;\n"
    + "import java.util.List;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. */\n"
    + "public class <&javaclass> {\n"
    + "\n");
  
  /**Text for Java header for Zbnf writer class. */
  private final StringPreparer sJavaHeadZbnf = new StringPreparer( 
      "package <&pkgpath>;\n"
    + "\n"
    + "import java.util.ArrayList;\n"
    + "import java.util.List;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. \n"
    + " * It is the derived class to write Zbnf result. */\n"
    + "public class <&javaclass>_Zbnf extends <&javaclass>{\n"
    + "\n");
  
  /**Text for class header for syntax component data storing. */
  private final StringPreparer sJavaCmpnClass = new StringPreparer( 
      "\n"
    + "\n"
    + "\n"
    + "  /**Cmpn Class. */\n"
    + "  public static class <&cmpnclass> {\n"
    + "  \n");
  
  /**Text for class header for syntax component to write from zbnf. */
  private final StringPreparer sJavaCmpnClassZbnf = new StringPreparer( 
      "\n"
    + "\n"
    + "\n"
    + "  /**Cmpn Class. */\n"
    + "  public static class <&cmpnclass>_Zbnf extends <&dataclass>.<&cmpnclass> {\n"
    + "  \n");
  
  private static final String sJavaCmpnEnd = 
      "  \n"
    + "  }\n"
    + "\n";
  
  private static final String sJavaEnd = 
      "\n"
    + "}\n"
    + "\n";
  
  private static final StringPreparer sJavaSimpleVar = new StringPreparer( 
      "    \n"
    + "    protected <&type> <&name>;\n"
    + "    \n"
    + "    \n");
  
  private static final StringPreparer sJavaListVar = new StringPreparer( 
      "    \n"
    + "    protected List<<&type>> <&name>;\n"
    + "    \n"
    + "    \n");
  
  private static final StringPreparer sJavaSimpleVarOper = new StringPreparer( 
      "    \n    \n"
    + "    /**Access to parse result.*/\n"
    + "    public <&type> <&name>() { return <&name>; }\n"
    + "    \n"
    + "    \n");
  
  private static final StringPreparer sJavaListVarOper = new StringPreparer( 
      "    \n    \n"
    + "    /**Access to parse result.*/\n"
    + "    public Iterable<<&type>> <&name>() { return <&name>; }\n"
    + "    \n"
    + "    \n");
  
  
  private static final StringPreparer sJavaSimpleVarZbnf = new StringPreparer( 
      "    /**Set routine for the singular component <<&type>?<&name>>. */\n"
    + "    public void set_<&name>(<&type> val) { super.<&name> = val; }\n"
    + "    \n"
    + "    \n");
  
  private static final StringPreparer sJavaListVarZbnf = new StringPreparer( 
      "    /**Set routine for the singular component <<&type>?<&name>>. */\n"
    + "    public void set_<&name>(<&type> val) { \n"
    + "      if(super.<&name>==null) { super.<&name> = new ArrayList<<&typeGeneric>>(); }\n"
    + "      super.<&name>.add(val); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  
  private static final StringPreparer sJavaCmpnZbnf = new StringPreparer( 
      "    /**Creates an instance for the result. &lt;<&type>?<&name>&gt;*/\n"
    + "    public <&type>_Zbnf new_<&name>() { return new <&type>_Zbnf(); }\n"
    + "    \n"
    + "    /**Set the result. &lt;<&type>?<&name>&gt;*/\n"
    + "    public void set_<&name>(<&type>_Zbnf val) { super.<&name> = val; }\n"
    + "    \n"
    + "    \n");
  
  private static final StringPreparer sJavaListCmpnZbnf = new StringPreparer( 
      "    /**create and add routine for the list component <<&type>?<&name>>. */\n"
    + "    public <&type>_Zbnf new_<&name>() { return new <&type>_Zbnf(); }\n"
    + "    \n"
    + "    /**Add the result to the list. &lt;<&type>?<&name>&gt;*/\n"
    + "    public void add_<&name>(<&type>_Zbnf val) {\n"
    + "      if(super.<&name>==null) { super.<&name> = new ArrayList<<&type>>(); }\n"
    + "      super.<&name>.add(val); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  
  
  public GenZbnfJavaData(Args args, MainCmdLogging_ifc log)
  { this.args = args;
    this.log = log;
    idxStdTypes.put("float","Float");
    idxStdTypes.put("int","Integer");
    idxStdTypes.put("String","String");
    idxStdTypes.put("double","Double");
    idxStdTypes.put("long","Long");
  }


  
  
  
  
  public void parseAndGenerate(File fSyntax) {
    ZbnfParser parser = new ZbnfParser(log);
    try {
      parser.setSyntax(args.fileSyntax);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    idxSubSyntax = parser.listSubPrescript;
    ZbnfSyntaxPrescript mainScript = parser.mainScript();
    
    evaluateSyntax(mainScript);
  }
  
  
  
  
  
  
  
  
  private void evaluateSyntax(ZbnfSyntaxPrescript mainScript) {
    String sJavaOutputDir = args.dirJava.getAbsolutePath() + "/" + args.sJavaPkg.replace(".","/") + "/";
    File sJavaOutputFile = new File(sJavaOutputDir + args.sJavaClass + ".java");
    File sJavaOutputFileZbnf = new File(sJavaOutputDir + args.sJavaClass + "_Zbnf.java");
    try {
      FileSystem.mkDirPath(sJavaOutputDir);
      wr = new FileWriter(sJavaOutputFile);
      wrz = new FileWriter(sJavaOutputFileZbnf);
    } catch (IOException e) {
      System.err.println("cannot create: " + sJavaOutputFile.getAbsolutePath());
    }
    try {
      sJavaHead.exec(wr, args.sJavaPkg, args.sJavaClass);
      sJavaHeadZbnf.exec(wrz, args.sJavaPkg, args.sJavaClass);
      //
      //
      //
      WrClass wrClass = new WrClass();  //the main class to write
      ZbnfSyntaxPrescript startScript = this.idxSubSyntax.get(mainScript.sDefinitionIdent);
      wrClass.evaluateChildSyntax(startScript.childSyntaxPrescripts, false, 1);
      wrClass.writeOperations();
      //
      //
      //
      int ixCmpn = 0;
      while(listCmpn.size() > ixCmpn) { //possible to add on end in loop
        String sCmpn = this.listCmpn.get(ixCmpn++);
        ZbnfSyntaxPrescript cmpn = idxSubSyntax.get(sCmpn);
        if(cmpn == null) {
          throw new IllegalArgumentException("syntax component not found: " + sCmpn);
        }
        wrClass = new WrClass();
        wrClass.wrClassCmpn(cmpn);
        
      }
      wr.append(sJavaEnd);
      wrz.append(sJavaEnd);
      //
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      System.err.println(e1.getMessage());
    }
    try {
      if(wr!=null) { wr.close(); }
      if(wrz!=null) { wrz.close(); }
    } catch (IOException e) {
      System.err.println("internal error cannot close: " + sJavaOutputFile.getAbsolutePath());
    }
  }

  
  
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] sArgs)
  { 
    smain(sArgs, true);
  }


  /**Invocation from another java program without exit the JVM
   * @param sArgs same like {@link #main(String[])}
   * @return "" or an error String
   */
  public static String smain(String[] sArgs){ return smain(sArgs, false); }


  private static String smain(String[] sArgs, boolean shouldExitVM){
    String sRet;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    try{
      mainCmdLine.addCmdLineProperties();
      boolean bOk;
      try{ bOk = mainCmdLine.parseArguments(); }
      catch(Exception exception)
      { mainCmdLine.report("Argument error:", exception);
        mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
        bOk = false;
      }
      if(bOk)
      { GenZbnfJavaData main = new GenZbnfJavaData(args, mainCmdLine);     //the main instance
        /* The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
           to hold the contact to the command line execution.
        */
        try{ main.parseAndGenerate(args.fileSyntax);; }
        catch(Exception exc)
        { //catch the last level of error. No error is reported direct on command line!
          System.err.println(exc.getMessage());
        }
      }
      sRet = "";
    } catch(Exception exc){
      sRet = exc.getMessage();
    }
    if(shouldExitVM) { mainCmdLine.exit(); }
    return sRet;
  }

  
  
  
  private class WrClass {
    Map<String, String> variables = new TreeMap<String, String>();

    StringBuilder wrOp = new StringBuilder(1000);
    
    
    
    /**Writes a Class for a syntax Component.
     * @param cmpn
     * @throws IOException
     */
    private void wrClassCmpn(ZbnfSyntaxPrescript cmpn) throws IOException {
      sJavaCmpnClass.exec(wr, firstUppercase(cmpn.sDefinitionIdent));
      sJavaCmpnClassZbnf.exec(wrz, firstUppercase(cmpn.sDefinitionIdent), args.sJavaClass);
      //
      //
      //
      evaluateChildSyntax(cmpn.childSyntaxPrescripts, false, 0);
      //
      writeOperations();
      //
      wr.append(sJavaCmpnEnd);
      wrz.append(sJavaCmpnEnd);
    }

    /**An syntax item can have an inner syntax tree. It is not a component. 
     * The result of the inner tree is stored in the same level.
     * If a called component is found, it is checked whether it has an own semantic
     * and the component's syntax is not contained in #idxCmpnWithoutSemantic ( name::=&lt;?> )
     * Then it is added to build a new class.
     * If it has not a semantic, the called component is expand here. 
     * @param childScript
     * @param bList
     * @param level
     * @throws IOException
     */
    void evaluateChildSyntax(List<ZbnfSyntaxPrescript> childScript, boolean bList, int level) throws IOException {
      for(ZbnfSyntaxPrescript item: childScript) {
        String semantic = item.sSemantic == null ? "" : item.sSemantic;
        if(semantic.startsWith("@")) { semantic = semantic.substring(1); }
        if(semantic.length() >0) {
        }
        boolean bRepetition = bList;
        if(item.eType !=null) {
          switch(item.eType) {
            
            case kAlternative: 
            case kAlternativeOptionCheckEmptyFirst:
            case kSimpleOption:
            case kAlternativeOption:
              if(item.sSemantic !=null) {
                //It is [<?semantic>...]: The parsed content in [...] should be stored as String
                wrVariable("String", semantic, bList, false); 
              }
              break;
            
            case kExpectedVariant:
              break;
            
            case kFloatWithFactor:
            case kFloatNumber: wrVariable("float", semantic, bList, false); break;
            
            case kPositivNumber:
            case kIntegerNumber:
            case kHexNumber: wrVariable("int", semantic, bList, false); break;
            
            case kStringUntilEndString:
            case kStringUntilEndStringInclusive:
            case kStringUntilEndStringTrim:
            case kStringUntilEndStringWithIndent:
            case kStringUntilEndchar:
            case kStringUntilEndcharInclusive:
            case kStringUntilEndcharOutsideQuotion:
            case kStringUntilEndcharWithIndent:
            case kStringUntilRightEndchar:
            case kStringUntilRightEndcharInclusive:
            case kQuotedString:
            case kRegularExpression:
            case kIdentifier:  wrVariable("String", semantic, bList, false); break;
            
            case kNegativVariant:
            case kNotDefined:
              break;
              
            case kOnlySemantic:
              break;
            case kRepetition: bRepetition = true; //store immediately result in list
              break;
            case kRepetitionRepeat:  bRepetition = true;
              break;
            case kSkipSpaces:
              break;
            case kSyntaxComponent: evaluateSubCmpn(item, bList, level);
              break;
            case kSyntaxDefinition:
              break;
            case kTerminalSymbol:
              break;
            case kTerminalSymbolInComment:
              break;
            case kUnconditionalVariant:
              break;
            default:
          }
        }
        //any item can contain an inner tree. Especially { ...inner syntax <cmpn>...}
        //in a repetition bList = true;
        if(item.childSyntaxPrescripts !=null) {
          evaluateChildSyntax(item.childSyntaxPrescripts, bRepetition, level+1);
        }
      }
    }

    private void wrVariable(String type, String semantic, boolean bList
      , boolean bCmpn
      ) throws IOException {
      if(semantic !=null) { //else: do not write, parsed without data
        String sTypeExist = variables.get(semantic);
        if(sTypeExist !=null) {
          if(! sTypeExist.equals(type)) {
            throw new IllegalArgumentException("Semantic " + semantic + " with different types");
          }
        } else {
          variables.put(semantic, type);
          String name = firstLowercase(semantic);
          String sTypeZbnf = type;
          String sTypeGeneric = idxStdTypes.get(type);
          if(sTypeGeneric == null) { 
//            sTypeZbnf = args.sJavaClass + "." + type;
//            sTypeGeneric = args.sJavaClass + "." + type; 
            sTypeZbnf = type;
            sTypeGeneric = type; 
          }
          if(bList) {
            GenZbnfJavaData.sJavaListVar.exec(wr, sTypeGeneric, name);
            GenZbnfJavaData.sJavaListVarOper.exec(wrOp, sTypeGeneric, name);
            if(bCmpn) {
              GenZbnfJavaData.sJavaListCmpnZbnf.exec(wrz, sTypeZbnf, name);
            } else {
              GenZbnfJavaData.sJavaListVarZbnf.exec(wrz, type, name, sTypeGeneric);
            }
          } else {
            GenZbnfJavaData.sJavaSimpleVar.exec(wr, type, name);
            GenZbnfJavaData.sJavaSimpleVarOper.exec(wrOp, type, name);
            if(bCmpn) {
              GenZbnfJavaData.sJavaCmpnZbnf.exec(wrz, sTypeZbnf, name);
            } else {
              GenZbnfJavaData.sJavaSimpleVarZbnf.exec(wrz, type, name);
            }
            
          }
        }
      }
    }
  
    
    
    /**This routine is called for <code>&lt;cmpnSyntax...></code>.
     * <ul>
     * <li>The component is searched in {@link #idxSubSyntax}. It should be found, elsewhere it is an IllegalArgumentException
     * <li>The semantic is taken either from the component's definition if it is not given on the item.
     *   In this case it is written in source like <code>&lt;component></code> and item[@link #sSemantic} contains "@"-
     * <li>In that case the semantic is usual the syntax identifier.
     * <li>In that case it can be abbreviating because <code>component::=&lt;?semantic></code> is given.
     * <li>In that case the semantic can be null if <code>component::=&lt;?></code> is given. See next List.
     * <li>The semantic is taken from the item if <code>&lt;component?semantic></code> is given.
     * <li>The semantic is null if <code>&lt;component?></code> is given.
     * </ul>
     * Depending on semantic == null:
     * <ul>
     * <li>If the semantic is null, then the component's syntax definition is used
     * and the component's data are created in this class.  
     * <li>If the semantic is given then a container for the component's data is created in this class 
     *   via {@link #wrVariable(String, String, boolean, boolean)}
     *   and the component's name is {@link #registerCmpn(String)} to create a class for it later if not created already. 
     * </ul>  
     * @param item The calling item of the component
     * @param bList true if the syntax is part of a repetition
     * @param level
     * @throws IOException
     */
    private void evaluateSubCmpn(ZbnfSyntaxPrescript item, boolean bList, int level) throws IOException {
      
      if(item.sDefinitionIdent.equals("input_variable_list"))
        Debugutil.stop();
      ZbnfSyntaxPrescript prescript = idxSubSyntax.get(item.sDefinitionIdent); 
      if(prescript == null) throw new IllegalArgumentException("error in syntax, component not found: " + item.sDefinitionIdent);
      //on semantic "@" in the item the semantic of the prescript should be used.
      //That is usually the same like item.sDefinitionIdent, but can be defined other via cpmpn::=<?semantic> 
      String semantic = item.sSemantic == null ? null : item.sSemantic.equals("@") ? prescript.sSemantic : item.sSemantic; 
      if(semantic == null) { //either the item is written with <...?> or the prescript with ::=<?> 
      //if(item.sSemantic == null || prescript.sSemantic == null) {
        //expand here
        evaluateChildSyntax(prescript.childSyntaxPrescripts, bList, level);
      }
      else {
        //create an own class for the component, write a container here.
        wrVariable(firstUppercase(item.sDefinitionIdent), semantic, bList, true);
        registerCmpn(item.sDefinitionIdent);
      }
      
    }
    
    
    private void registerCmpn(String name) {
      if(GenZbnfJavaData.this.idxRegisteredCmpn.get(name) == null) {
        GenZbnfJavaData.this.idxRegisteredCmpn.put(name, name);
        GenZbnfJavaData.this.listCmpn.add(name);
      }
    }
  
    
    
    private String firstUppercase(String src) {
      char cc = src.charAt(0);
      if(Character.isUpperCase(cc)) return src;
      else return Character.toUpperCase(cc) + src.substring(1);
    }
    
    private String firstLowercase(String src) {
      char cc = src.charAt(0);
      if(Character.isLowerCase(cc)) return src;
      else return Character.toLowerCase(cc) + src.substring(1);
    }
    
    
    void writeOperations() throws IOException {
      wr.append(wrOp);
      wrOp.setLength(0);
    }
    
  }
 
  
  
  
  
  

  /**The inner class CmdLine helps to evaluate the command line arguments
     * and show help messages on command line.
     */
    private static class CmdLine extends MainCmd
    { 
    
      
      
      public final MainCmd.Argument[] defArguments =
      { new MainCmd.Argument("-s", "<SYNTAX>    syntax prescript in ZBNF format for parsing", new MainCmd.SetArgument(){ 
            @Override public boolean setArgument(String val){ 
              argData.fileSyntax = new File(val);  return true;
            }})
          , new MainCmd.Argument("-dirJava", ":<dirJava>    directory for Java output", new MainCmd.SetArgument(){ 
            @Override public boolean setArgument(String val){ 
              argData.dirJava = new File(val);  return true;
            }})    
          , new MainCmd.Argument("-pkg", ":<pkg.path>    directory for Java output", new MainCmd.SetArgument(){ 
            @Override public boolean setArgument(String val){ 
              argData.sJavaPkg = val;  return true;
            }})    
          , new MainCmd.Argument("-class", ":<class>.java    directory for Java output", new MainCmd.SetArgument(){ 
            @Override public boolean setArgument(String val){ 
              argData.sJavaClass = val;  return true;
          }})    
      };
  
      public final Args argData;
      
      /*---------------------------------------------------------------------------------------------*/
      /**Constructor of the cmdline handling class.
      The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
  */
      protected CmdLine(Args argData, String[] sCmdlineArgs)
      { super(sCmdlineArgs);
        this.argData = argData;
      }
      
  
      void addCmdLineProperties(){
        super.addAboutInfo("Conversion text to XML via ZBNF");
        super.addAboutInfo("made by HSchorrig, 2006-03-20..2014-05-29");
        super.addHelpInfo("args: -i:<INPUT> -s:<SYNTAX> -[x|y|z]:<OUTPUT> [{-a:<NAME>=<VALUE>}]");  //[-w[+|-|0]]
        super.addArgument(defArguments);
        super.addHelpInfo("==Standard arguments of MainCmd==");
        super.addStandardHelpInfo();
      }
      
    
    
    
      /*---------------------------------------------------------------------------------------------*/
      /**Checks the cmdline arguments relation together.
         If there is an inconsistents, a message should be written. It may be also a warning.
         @return true if successfull, false if failed.
      */
      @Override
      protected boolean checkArguments()
      { boolean bOk = true;
    
        if(argData.fileSyntax == null)            { bOk = false; writeError("ERROR argument Syntaxfile is obligat."); }
        else if(argData.fileSyntax.length()==0)   { bOk = false; writeError("ERROR argument Syntaxfile without content.");}
    
        return bOk;
    
     }
    }//class CmdLine

  
  
}