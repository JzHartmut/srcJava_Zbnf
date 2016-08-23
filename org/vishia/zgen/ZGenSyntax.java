package org.vishia.zgen;

public final class ZGenSyntax {

  
  /**Version, history and license.
   * <ul>
   * <li>2014-03-07 Hartmut new:  currdir without definition, Filepath as type of a named argument.
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   * <li>2014-02-22 Hartmut new: Bool and Num as variable types.
   * <li>2013-12-01 Hartmut new debug 
   * <li>2013-07-07 Hartmut improved: The older text generation view is now removed.
   *   <ul>
   *   <li>include "file" instead <:include:file>
   *   <li>subtext name(args) <:>...text...<.> instead <:subtext:name:args>...text...<.subtext>
   *   <li><:file>...<.file> removed, use main(){...}
   *   <li><=variabledef...> removed, use newer variabledef
   *   <li>genContent::= with whitespaces removed, instead <code>genContentNowWhiteSpaces::=</code> now exists
   *     as <code>textExpr::=</code> and contains <code><:for...> and <:if...></code>
   *   <li><code>textExpr::=</code> in older version now <code>textValue::=</code> with admissible 
   *     <code>"text", <:>...<.>, new ..., java ...</code>.
   *   <li><code>info</code> as syntactical unit removed, available as method <code>debug.info(...)</code>
   *   <li>  
   *   </ul>
   * <li>2013-07-07 Hartmut new: Now syntax as Java batch, invocation of command lines.
   * <li>2013-06-20 Hartmut new: Syntax with extArg for textual Arguments in extra block
   * <li>2013-06-29 Hartmut chg: Now <=var:expr.> should be terminated with .>
   * <li>2013-03-10 <code><:include:path> and <:scriptclass:JavaPath></code> is supported up to now.
   * <li>2013-01-05 Hartmut new: A expression is a concatenation of strings or + or - of numerics. It is used for all value expressions.
   *   In this kind an argument of <*path.method("text" + $$eNV_VAR + dataPath) is possible.
   *   Also <*path + path2> is possible whereby its the same like <*$path><*$path2> in that case.
   * <li>2012-12-26 Hartmut creation of this class: The syntax should be in a separate file, better for navigation.
   * <li>2012-12-10 Hartmut chg: The syntax is now stored in a static String variable. 
   * <li>2012-10-00 Hartmut new TextGenerator-syntax in a text file.
   * <li>2011-05-00 Hartmut creation of the syntax as Zmake syntax in a text file.
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
  static final public int version = 20131201;

  
  public final static String syntax =
      " $comment=(?...?).\n"
    + " $endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + " $keywords= new | cmd | cmd_check | start \n" 
    //+ "   | debug | java \n" 
    + "   | stdout | stdin | stderr \n" 
    + "   | subtext | sub | main | call | cd \n"
    + "   | Pipe | StringBuffer | Stringjar | String | List | Openfile | Fileset | Obj | Set | set | include | zbatch \n"
    + "   | break | XXXreturn | exit | onerror | for | while | do | if | elsif | else | throw . \n"
    + " \n"
    + " ZGen::= \n"
    + " [<*|==ZGen==?>==ZGen== ]\n"
    //+ " { \\<:scriptclass : <$\\.?scriptclass> \\> \n"
    + " [ ! checkZGen = <textValue?checkZGen> ; ]\n"
    + " [{ include [<\"\"?include> | <*;\\ ?include>] ; \n"
    + "  | currdir = <textDatapath?scriptCurrdir> ;\n"
    + " }] \n"
    + " { <DefVariable?> ; \n"
    + " | subtext  <subtext?subroutine> \n"
    + " | sub <subroutine> \n"
    + " | class <subClass> \n"
    + " | main ( ) \\{ <statementBlock?mainRoutine> \\} \n"
    + " | //<*\\n?> ##line comment in C style\n"
    + " | /*<*|*/?>*/ ##block commment in C style\n"
    + " | ==endZGen==<*\\e?> \n"
    + " } \\e.\n"
    + " \n"
    + " \n"
    + " subClass::= <$?name> \\{ \n"
    //+ " { <DefVariable?> ; \n"
    + " { subtext  <subtext?subroutine> \n"
    + " | sub <subroutine> \n"
    + " | class <subClass> \n"
    + " } \\}. \n"
    + " \n"
    + " \n"
    + " subroutine::= [<?type> String | Append | Openfile | Map | List | Obj |] <$?name> ( [{ <DefVariable?formalArgument> ? , }] ) \\{ [<statementBlock>] \\}. \n"
    + " \n"
    + " subtext::= <$?name> ( [ { <DefVariable?formalArgument> ? , }] ) <textExpr>.\n"
    + " \n"
    + " \n"
    + " statementBlock::= { <statement?> }.\n"
    + " \n"
    + " statement::=\n"
    + "   \\{ [<statementBlock>] \\} \n"
    + " | REM <*\\n?> ##Remark like in batch files\n"
    + " | //<*\\n?> ##line commment in C style\n"
    + " | /*<*|*/?>*/ ##block commment in C style\n"
    + " | <DefVariable?> ; \n"
    + " | for <forCtrl> \n"
    + " | <callSubroutine?call> \n"
    + " | cd [<textValue?cd> | <*\\ ;?cd> ; ]  ##change current directory \n"
    + " | if <ifCtrl> \n"
    + " | while <whileCtrl> \n"
    + " | do <dowhileCtrl> \n"
    + " | <threadBlock> \n"
    + " | \\<+ <textOut> \n"
    + " | <cmdLineWait?cmdWait> \n"  ///
    + " | start <cmdLine?cmdStart> \n"
    + " | zmake <zmake> \n"
    + " | move <srcdst?move> ; \n"
    + " | copy <srcdst?copy> ; \n"
    + " | break <?breakBlock> ; \n" 
    + " | return <?return> ; \n" 
    + " | exit <#?exitScript> ;\n"
    + " | throw on errorlevel <#?throwonerror> \n"
    + " | throw <textDataPath?throw> \n" 
    + " | onerror <onerror> \n"
    + " | if errorlevel <iferrorlevel> \n"
    + " | debug [<textValue?debug>| <?debug>] ; \n"
    + " | <assignExpr> \n"
    + " | ; \n"
    + " .\n"
    + " \n"
    + " debug::= xxx.\n"
    + " \n"
    + " srcdst::= [src=] <textValue?actualArgument> [dst=] <textValue?actualArgument> .\n"
    + " \n"
    + " \n"
    + " DefVariable::=String\\  <DefStringVar?textVariable> \n"  //note: without ; because used in sub argument list
    + " | StringBuffer\\  <DefObjVar?Stringjar> \n"
    + " | Stringjar\\  <DefObjVar?Stringjar> \n"
    + " | Num\\  <DefNumVar> \n"
    + " | Bool\\  <DefBoolVar> \n"
    + " | Pipe\\  <DefObjVar?Pipe> \n"
    + " | List\\  <DefObjVar?List> \n"
    + " | Map\\  <DefMapVar> \n"
    + " | Obj\\  <DefObjVar> \n"
    + " | Openfile\\  <Openfile> \n"
    + " | Fileset\\  <DefFileset> \n"
    + " | Filepath\\ <DefFilepath> \n"
    + " | Set\\  <DefStringVar?setEnvVar> \n"
    + " | set\\  <DefStringVar?setEnvVar> \n"
    + " | SET\\  <DefStringVar?setEnvVar> \n"
    + " .\n" ///
    + " \n"
    + " DefNumVar::= <dataPath?defVariable>  [ = <numExpr>].\n"  //a text or object or expression
    + " \n"
    + " DefBoolVar::= <dataPath?defVariable>  [ = <boolExpr>].\n"  //a text or object or expression
    + " \n"
    + " DefObjVar::= <dataPath?defVariable>  [ = <objExpr?>].\n"  //a text or object or expression
    + " \n"
    + " DefStringVar::= <dataPath?defVariable> [ = <textDatapath?>].\n"  //[{ <dataPath?assign> = }] <textDatapath?> .\n"
    + " \n"
    + " DefMapVar::= <dataPath?defVariable>.\n"  //[{ <dataPath?assign> = }] <textDatapath?> .\n"
    + " \n"
    + " Openfile::= <dataPath?defVariable> = <textDatapath?> .\n"
    + " \n"
    + " DefFileset::= <dataPath?defVariable> =  ( \n"
    + " [ commonpath = <Filepath?commonpath> , ] \n"
    + " { <Filepath> ? , } \n"
    + " ) .\n"
    + " \n"
    + " \n"
    + " DefFilepath::= <dataPath?defVariable> = <Filepath>. \n"
    + " \n"
    + " \n"
    + " Filepath::=<\"\"?!prepFilePath>|<*;\\ \\r\\n,)?!prepFilePath>. \n"
    + " \n"
    + " prepFilePath::=<$NoWhiteSpaces><! *?>\n"
    + " [ $$<$?@envVariable> [\\\\|/|]     ##path can start with a environment variable's content\n"
    + " | $<$?@scriptVariable> [\\\\|/|]   ##path can start with a scriptvariable's content\n"
    + " | [<!.?@drive>:]                   ## only 1 char with followed : is the drive letter\n"
    + "   [ [/|\\\\]<?@absPath>]           ## starting with / maybe after d: is absolute path\n"
    + " |]\n"
    + " [ <*:?@pathbase>[?:=]:]            ## all until : is pathbase, but not till a :=\n"
    + " [ <toLastChar:/\\\\?@path>[\\\\|/|]] ## all until last \\ or / is path\n"
    + " [ <toLastChar:.?@name>             ## all until exclusive dot is the name\n"
    + "   <*\\e?@ext>                      ## from dot to end is the extension\n"
    + " | <*\\e?@name>                     ## No dot is found, all is the name.\n"
    + " ] ."
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " textDatapath::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | <dataAccess> .\n"
    + " \n"
    + " textValue::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | * <dataAccess> | <*;(\\ \\r\\n?text> .\n"
    + " \n"
    + " \n"
    + " objExpr::= <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | <numExpr>.\n" //special handling if numExpr contains only a dataAccess.
    + " \n"
    + " \n"
    + " dataAccess::= \n"
    + " [ $$<$?envVariable> \n" 
    + " | [<?startVariable> $<![1-9]?>| $<$?>]    ## $1 .. $9 are the arguments of Jbatch, $name for environment \n"
    + " | new <newJavaClass> \n" 
    + " | java <staticJavaMethod> \n" 
    + " | : <staticJavaMethod> \n" 
    + " | <dataPath?> \n" 
    + " ].\n"
    + " \n"
    + " \n"
    + " newJavaClass::= <$\\.$?javapath> [ ( [{ <objExpr?argument> ? , }] )].\n" 
    + " staticJavaMethod::= <$\\.$?javapath> [( [ { <objExpr?argument> ? , } ] )].\n"
    + " ##a javapath is the full package path and class [.staticmetod] separated by dot. \n"
    + " \n"
    + " dataPath::= <$@-?startVariable>[ [?\\. \\>] \\.{ <datapathElement> ? [?\\. \\>] \\.}].\n"
    + " \n"
    + " datapathElement::= [<?ident>[@]<$-?>] [( [{ <objExpr?argument> ? ,}])<?whatisit=(>].\n"  
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " condition::=<andExpr?> [{\\|\\| <andExpr?boolOrOperation>}].\n"  // || of <andExpr> 
    + " \n"
    + " andExpr::= <boolExpr?> [{ && <boolExpr?boolAndOperation>}].\n"    // && of <boolExpr>
    + " \n"  
    + " boolExpr::= [<?boolNot> ! | not|]\n"
    + " [ ( <condition?parenthesisCondition> ) \n"                //boolean in paranthesis
    + " | <numExpr?> [<cmpOperation>]\n"  //simple boolean
    + " ].\n"  
    + " \n"
    + " cmpOperation::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne] |  [<?cmpOperator> != | == | \\>= | \\> | \\<= | \\< ]] <numExpr?>.\n"
    + " \n"
    + " conditionInText::=<andExprInText?> [{\\|\\| <andExprInText?boolOrOperation>}].\n"  // || of <andExpr> 
    + " \n"
    + " andExprInText::= <boolExprInText?> [{ && <boolExprInText?boolAndOperation>}].\n"    // && of <boolExpr>
    + " \n"  
    + " boolExprInText::= [<?boolNot> ! | not|]\n"
    + " [ ( <conditionInText?parenthesisCondition> ) \n"                //boolean in paranthesis
    + " | <numExpr?> [<cmpOperationInText?cmpOperation>]\n"  //simple boolean
    + " ].\n"  
    + " \n"
    + " cmpOperationInText::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne] |  [<?cmpOperator> != | == ]] <numExpr?>.\n"
    + " \n"
    + " \n"
    + " numExpr::=" //  \\<:\\><textExprNoWhiteSpaces?textExpr>\\<\\.\\> |\n"
    + "               bool ( <boolExpr?> ) \n"
    + "             | <multExpr?> [{ + <multExpr?addOperation> | - <multExpr?subOperation>}].\n"
    + " \n"
    + " \n"
    + " multExpr::= <value?> [{ * <value?multOperation> | / <value?divOperation> }].\n"
    + " \n"
    + " value::= <#?intValue> | <#f?floatValue> |   ##unary - associated to value.\n"
    + " [{[<?unaryOperator> ! | ~ | - | + ]}]        ##additional unary operators.\n"
    + " [<#?intValue> | 0x<#x?intValue> | <#f?floatValue> ##ones of kind of value:\n"
    + " | '<!.?charValue>' | <\"\"?textValue> \n"
    + " | ( <numExpr?parenthesisExpr> ) \n" 
    + " | <dataAccess> \n"
    + " ].\n"
    + " \n"
    //+ " objvalue::=\n"
    + " \n"
    
    + " textExpr::=<$NoWhiteSpaces>\n"
    + " { [?\\<\\.\\>]              ##abort on <.> \n"
    + " [ \\<:for:<forInText?forCtrl>\n"
    + " | \\<:if: <ifInText?ifCtrl>\n"
    + " | \\<:hasNext\\> <textExpr?hasNext> \\<\\.hasNext\\>\n"
    + " | \\<&subtext : <callSubtext?call>\n"
    + " | \\<:debug[:<textDatapath?debug>| <?debug>]\\>\n"
    + " | \\<&<dataText>\n"
    + " | \\<: [<?transcription> n | r | t | \\< | # ] \\>\n"
    + " | \\<:lf\\><?newline>\n"
    + " | \\<:\\ \\><!\\\\s*?> [ \\#\\#<*\\r\\n?> <!\\\\s*?> ]\n"      //skip all whitespaces and endlinecomment
    + " | \\#\\#<*\\r\\n?>{\\r|\\n}   ##skip comment in text\n"  //skip comment till inclusively newline
    + " | \\<:@<#?setColumn> [ + <#?minChars>] \\>  \n"               //set column
    + " | \\<:\\><textExpr?>\\<\\.\\>\n"               //flat nesting
    + " | <*|\\<:|\\<+|\\<=|\\<&|\\<\\.?plainText>\n"
    + " ]\n"
    + " }.\n"
    + " \n"
    + " \n"
    + " dataText::=<dataAccess>[ : <*\\>?formatText>] \\>.     ##<*expr: format>\n"
    + " \n"
    + " textOut::= [<dataPath?assign>] \\> <textExpr>[ \\<\\.+\\> | \\<\\.n+\\><?newline>].\n"
    + " \n"      //<?posIndent>
    + " \n"
    + " \n"
      //Note: the for-variable is adequate a DefVariable
    + " forCtrl::= ( <$?forVariable> : <dataAccess?forContainer> [ && <condition> ])  \\{ <statementBlock> \\} .\n"
    + " \n"
    + " forInText::= <$?forVariable> : <dataAccess?forContainer> [ && <condition> ] \\> <textExpr> \\<\\.for[ : <$?@name> ]\\>. ##name is the name of the container element data reference\n"
    + " \n"
    + " ifCtrl::= <ifBlock> [{ elsif <ifBlock>  }][ else \\{ [<statementBlock?elseBlock>] \\} ].\n"
    + " \n"
    + " ifBlock::= ( <condition> ) \\{ <statementBlock> \\} .\n"
    + " \n"
    + " ifInText::= <ifBlockInText?ifBlock> [{ \\<:elsif : <ifBlockInText?ifBlock>  }][ \\<:else\\> <textExpr?elseBlock> ] \\<\\.if\\>.\n"
    + " \n"
    + " ifBlockInText::= <conditionInText?condition> \\> <textExpr>.\n"
    + " \n"
    + " whileCtrl::= ( <condition> ) \\{ [<statementBlock>] \\} .\n"
    + " \n"
    + " dowhileCtrl::=  do \\{ [<statementBlock>] \\} while ( <condition> ) ; .\n"
    + " \n"
    + " \n"
    + " onerror::= [ <#?errorLevel> \n" 
    + "            | [<?errortype> notfound | cmd | file | internal | exit ]\n" 
    + "            |]\n" 
    + "            \\{ [<statementBlock>] \\}.\n"
    + " \n"
    + " \n"
    + " callSubroutine::= [{ <dataPath?assign> [ = | += <?append>] }] call <textValue?callName> ( [{ <namedArgument?actualArgument> ? , }] ) ; .\n"
    + " \n"
    + " callSubtext::=[<\"\"?callName>|<textValue?callNameExpr>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + " \n"
    + " namedArgument::= \n"
    + " Filepath\\ <$?name> = <Filepath>\n"
    + " | [<?name>[$]<$?>|xxx] = <objExpr?>\n"
    + " .\n"
    + " \n"
    + " \n"
     + " zmake::=<output> := <textValue?callName> ( { <namedArgument?actualArgument> | <zmakeInput> ? ,} ).\n"
    + " \n"
    + " output::=<*|\\ |\\r|\\n|:=?!prepFilePath>.\n"
    + " \n"
    + " zmakeInput::= [|<\"\"?!prepInputfile>| <*\\ \\r\\n,)&?!prepInputfile>] & <$?zmakeFilesetName> . \n"
    + " \n"
    + " prepInputfile::=<prepFilePath?zmakeInputDir>.\n"
    + " \n"
    + " \n"
    + " cmdLineWait::=[{ <dataPath?assign> += }] cmd\\  <cmdLine?>.\n"
    + " \n"
    + " cmdLine::= [\\!argsCheck!<?argsCheck>] <textValue?> [{[?;[\\ |\\n|\\r]] [ \\<\\:arg\\><textExpr?actualArgument>\\<\\.arg\\> |<textValue?actualArgument>] }] \n"
    + "   [ \\<:stdout:[ pipe<?pipe>| [$]<$?stdoutVariable>] \\>] ;.\n"
    + " \n"
    + " iferrorlevel::= <#?errorLevel> \\{ [<statementBlock>] \\}.\n"
    + " \n"
    + " \n"
    + " \n"
    + " assignExpr::= [{ <dataPath?assign> [ = | += <?append>] }] <objExpr?> ;.\n"
    + " \n"
    + " \n"
    + " threadBlock::= Thread <dataPath?defThreadVar> [;| = [thread] \\{ <statementBlock> \\}] | [<dataPath?assignThreadVar> =] thread \\{ <statementBlock> \\}.\n"
    + " \n"
    ;  
 
  


  
}
