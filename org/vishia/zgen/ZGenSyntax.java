package org.vishia.zgen;

public final class ZGenSyntax {

  
  /**Version, history and license.
   * <ul>
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
      "$comment=(?...?).\n"
    + "$endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + "$keywords= new | java | cmd | start | debug | stdout | stdin | stderr | pipe | subtext | sub | main | call | cd "
    + "| StringBuffer | String | List | Openfile | Obj | Set | set | include | zbatch "
    + "| break | return | exit | onerror | for | while | if | elsif | else . \n"
    + "\n"
    + "ZGen::= \n"
    + "[<*|==ZGen==?>==ZGen== ]\n"
    //+ "{ \\<:scriptclass : <$\\.?scriptclass> \\> \n"
    + "[{ include [<\"\"?include> | <*\\ ?include>] ; }] \n"
    + "{ <DefVariables?> \n"
    + "| subtext  <subtext?subScript> \n"
    + "| sub <subScript> \n"
    + "| class <subClass> \n"
    + "| main ( ) <statementBlock?mainScript> \n"
    + "| //<*\\n?> ##line comment in C style\n"
    + "| /*<*|*/?>*/ ##block commment in C style\n"
    + "| ==endZGen==<*\\e?> \n"
    + "} \\e.\n"
    + "\n"
    + "DefVariables::=Pipe <DefObjVar?Pipe> ; \n"
    + "| StringBuffer <DefObjVar?StringBuffer> ; \n"
    + "| String <DefStringVar?textVariable> ; \n"
    + "| List <DefObjVar?List> ; \n"
    + "| Openfile <Openfile> ; \n"
    + "| Obj <DefObjVar?objVariable> ; \n"
    + "| Set <DefStringVar?setEnvVar> ; \n"
    + "| set <DefStringVar?setEnvVar> ; \n"
    + ".\n" ///
    + "\n"
    + "DefObjVar::= <variable?defVariable>  [ = <objExpr?>].\n"  //a text or object or expression
    + "\n"
    //+ "setEnvVar::= [<?name>[$]<$?>] = <textDatapath?> .\n"
    + "\n"
    + "DefStringVar::= <variable?defVariable> [ = <textDatapath?>].\n"  //[{ <variable?assign> = }] <textDatapath?> .\n"
    + "\n"
    + "Openfile::= <variable?defVariable> = <textDatapath?> .\n"
    + "\n"
    + "\n"
    + "variable::= <$@-?startVariable>[ [?\\. \\>] \\.{ <datapathElement> ? [?\\. \\>] \\.}].\n"
    + "\n"
    + "datapathElement::=[<$@-?ident>|<\"\"?ident>] [( [{ <objExpr?argument> ? ,}])<?whatisit=(>].\n"  
    + "\n"
    + "datapath::= \n"
    + "[ $$<$?envVariable> \n" 
    + "| [<?startVariable> $<![1-9]?>| $<$?>]    ## $1 .. $9 are the arguments of Jbatch, $name for environment \n"
    + "| <variable?> \n" 
    + "| new <newJavaClass> \n" 
    + "| java <staticJavaMethod> \n" 
    + "].\n"
    + "\n"
    + "\n"
    + "newJavaClass::= <$\\.?javapath> [ ( [{ <objExpr?argument> ? , }] )].\n" 
    + "staticJavaMethod::= <$\\.?javapath> [ (+)<?extArgs>| ( [ { <objExpr?argument> ? , } ] )].\n"
    + "##a javapath is the full package path and class [.staticmetod] separated by dot. \n"
    + "\n"
    
    
    
    + "subClass::= <$?name> \\{ \n"
    + "{ <DefVariables?> \n"
    + "| subtext  <subtext?subScript> \n"
    + "| sub <subScript> \n"
    + "| class <subClass> \n"
    + "} \\}. \n"
    + "\n"
    
    
    
    
    + "subtext::= <$?name> ( [ { <namedArgument?formalArgument> ? , }] ) <textExpr>.\n"
    + "\n"
    + "envVar::= <$?name> = <textDatapath?>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "objExpr::= <textDatapath?> \n"         //either a text or a datapath, which may be any object, 
    + "  | <expression>.\n"              //or an expression
    + "\n"
    + "\n"
    + "textDatapath::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | <datapath> .\n"
    + "\n"
    + "textValue::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | * <datapath> | <*;(\\ \\r\\n?text> .\n"
    + "\n"
    + "\n"
    + "condition::=<andExpr?> [{\\|\\| <andExpr?boolOrOperation>}].\n"  // || of <andExpr> 
    + "\n"
    + "andExpr::= <boolExpr?> [{ && <boolExpr?boolAndOperation>}].\n"    // && of <boolExpr>
    + "\n"  
    + "\n"    
    + "\n"
    + "boolExpr::= [<?boolNot> ! | not|]\n"
    + "[ ( <condition?parenthesisCondition> ) \n"                //boolean in paranthesis
    + "| <expression?> [<cmpOperation>]\n"  //simple boolean
    + "].\n"  
    + "\n"
    + "cmpOperation::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne] |  [<?cmpOperator> != | == ]] <expression?>.\n"
    + "\n"
    + "expression::= \\<:\\><textExpr>\\<\\.\\> \n"
    + "            | <multExpr?> [{ + <multExpr?addOperation> | - <multExpr?subOperation>}].\n"
    + "\n"
    + "\n"
    + "multExpr::= <value?> [{ * <value?multOperation> | / <value?divOperation> }].\n"
    + "\n"
    + "value::= <#?intValue> | <#f?floatValue> |   ##unary - associated to value.\n"
    + "[{[<?unaryOperator> ! | ~ | - | + ]}]        ##additional unary operators.\n"
    + "[<#?intValue> | 0x<#x?intValue> | <#f?floatValue> ##ones of kind of value:\n"
    + "| '<!.?charValue>' | <\"\"?textValue> \n"
    + "| ( <expression?parenthesisExpr> ) \n" 
    + "| <datapath> \n"
    + "].\n"
    + "\n"
    //+ "objvalue::=\n"
    + "\n"
    + "namedArgument::= [<?name>[$]<$?>|xxx][ = <objExpr?>].\n"
    + "\n"
    
    + "textExpr::=\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<:for:<forContainer>\n"
    + "| \\<:if: <if>\n"
    + "| \\<:hasNext\\> <textExpr?hasNext> \\<\\.hasNext\\>\n"
    + "| \\<*subtext : <callSubtext?call>\n"
    + "| \\<*<dataText>\n"
    + "| \\<:n\\><?newline>\n"
    + "| \\<:\\><textExprNoWhiteSpaces?>\\<\\.\\>"               //flat nesting
    + "| <*|\\<:|\\<+|\\<=|\\<*|\\<\\.?nonEmptyText>    ##non-empty text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    
    + "textExprNoWhiteSpaces::=<$NoWhiteSpaces>\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<:for:<forContainer>\n"
    + "| \\<:if: <if>\n"
    + "| \\<:hasNext\\> <textExpr?hasNext> \\<\\.hasNext\\>\n"
    + "| \\<*subtext : <callSubtext?call>\n"
    + "| \\<*<dataText>\n"
    + "| \\<:n\\><?newline>\n"
    + "| \\<:\\><textExpr?>\\<\\.\\>"               //flat nesting
    + "| <*|\\<:|\\<+|\\<=|\\<*|\\<\\.?textReplLf>    ##text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    + "dataText::=<datapath>[ : <\"\"?formatText>] \\>.     ##<*expr: format>\n"
    + "\n"
    + "forContainer::= [$]<$?@name> : <objExpr?> \\> <textExpr> \\<\\.for[ : <$?@name> ]\\>. ##name is the name of the container element data reference\n"
    + "\n"
    + "if::= <ifBlock> [{ \\<:elsif : <ifBlock>  }][ \\<:else\\> <textExpr?elseBlock> ] \\<\\.if\\>.\n"
    + "ifBlock::= <condition> \\> <textExpr>.\n"
    + "\n"
    + "\n"
    + "subScript::= <$?name> ( [{ <namedArgument?formalArgument> ? , }] ) <statementBlock?>. \n"

    + "\n"
    + "statementBlock::= \\{ [{ <statement?> }] \\}.\n"

    + "statement::=\n"
    + "  <statementBlock?statementBlock> \n"
    + "| REM <*\\n?> ##Remark like in batch files\n"
    + "| //<*\\n?> ##line commment in C style\n"
    + "| /*<*|*/?>*/ ##block commment in C style\n"
    + "| <DefVariables?> \n"
    + "| for <forScript?forContainer> \n"
    + "| call <callSubroutine?call> \n"
    + "| cd [<textValue?cd> | <*\\ ;?cd> ; ]  ##change current directory \n"
    + "| if <ifScript?if> \n"
    + "| while <whileScript?> \n"
    + "| <threadBlock> \n"
    + "| <textOut> \n"
    + "| <cmdLineWait?cmdLine> \n"  ///
    + "| start <cmdLine?cmdStart> \n"
    + "| move <srcdst?move> ; \n"
    + "| copy <srcdst?copy> ; \n"
    //+ "| [{ <datapath?-assign> = }] java <staticJavaMethod?+?> \n"
    + "| <assignExpr> \n"
    + "| break <?breakBlock> ;\n"
    + "| return <textValue?return> ;\n"
    + "| exit <#?exitScript> ;\n"
    + "| onerror <onerror> \n"
    + "| debug <textValue?debug> ; \n"
    + "| ; \n"
    + ".\n"
    + "\n"
    + "assignExpr::= [{ <variable?assign> [ = | += <?append>] }] <objExpr?> ;.\n"
    + "\n"
    + "threadBlock::= Thread <variable?defThreadVar> [;| = [thread] <statementBlock>] | [<variable?assignThreadVar> =] thread <statementBlock?>.\n"
    + "\n"
    + "srcdst::= <textValue?p1> <textValue?p2> .\n"
    + "\n"
    + "onerror::= [<#?errorLevel> | [<?errortype> notfound | file | internal | exit [<#?errorLevel>] ]|] <statementBlock?> .\n"
    + "\n"

    + "\n"
    + "ifScript::= <ifScriptBlock?ifBlock> [{ elsif <ifScriptBlock?ifBlock>  }][ else <statementBlock?elseBlock> ].\n"
    + "\n"
    + "whileScript::= <ifScriptBlock?whileBlock> .\n"
    + "\n"
    + "ifScriptBlock::= ( <condition> ) <statementBlock?> .\n"
    + "\n"
    + "forScript::= ( [$]<$\\.?@name> : <objExpr?> )  <statementBlock?> .\n"
    + "\n"
    + "callSubroutine::= <textValue?callName> ( [{ <namedArgument?actualArgument> ? , }] ) ; .\n"
    + "callSubtext::=[<\"\"?callName>|<textValue?callNameExpr>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + "\n"
    + "textOut::= \\<+ <variable?assign> \\> <textExpr>[ \\<\\.+\\> | \\<\\.n+\\><?newline>].\n"
    + "\n"
    + "cmdLineWait::=[{ <variable?assign> += }] cmd <cmdLine?>.\n"
    + "\n"
    + "cmdLine::= <textValue?> [{[?;[\\ |\\n|\\r]] [ \\<\\:arg\\><textExpr?argExpr>\\<\\.arg\\> |<textValue?actualArgument>] }] \n"
    + "  [ \\<:stdout:[ pipe<?pipe>| [$]<$?stdoutVariable>] \\>] ;.\n"
    + "\n"
       + "\n";
 
  


  
}
