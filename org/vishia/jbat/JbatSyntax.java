package org.vishia.jbat;

public final class JbatSyntax {

  
  /**Version, history and license.
   * <ul>
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
  static final public int version = 20131003;

  
  public final static String syntax =
      "$comment=(?...?).\n"
    + "$endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + "$keywords= cmd | for | if | sub | main | call | break | java | new | String | Stringb | Obj | List | Pipe.\n"
    + "\n"
    + "\n"
    + "ZTextctrl::= \n"
    + "[<*|==jbat==?>==jbat== ]\n"
    + "{ \\<:scriptclass : <$\\.?scriptclass> \\> \n"
    + "| \\<:include : <*\\>\\ ?include> \\> \n"
    + "| \\<:subtext : <subtext> \n"
    + "| \\<:file\\><genFile> \n"
    + "| sub <subScript?subtext> \n"
    + "| main ( ) <mainScript?genFile> \n"
    + "| \\<= <variableDef>\n"
    + "} \\e.\n"
    + "\n"
    + "\n"
    + "subtext::= <$?name> [ : { <namedArgument?formalArgument> ? , }] \\><genContent?> \\<\\.subtext\\>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "##A genControl script should have a part <:file>....<.file> which describes how the whole file should build.\n"
    + "\n"
    + "genFile::= \n"
    +   "<genContent?>\n"
    + "\\<\\.file\\>.\n"
    +   "\n"
    + "  \n"
    + "\n"
    + "\n"
    + "##The textual content of any target, file, variable etc.\n"
    + "\n"
    + "genContent::=\n"
    + "{ \\<= <variableDef>                       ##Possibility to have local variables.\n"
    + "| \\<:for:<forContainer>\n"
    + "| \\<:if: <if>\n"
    + "| \\<:hasNext\\> <genContent?hasNext> \\<\\.hasNext\\>\n"
    //+ "| \\<+<variableDefment?addToList>\n"
    + "| \\<*subtext : <callSubtext>\n"
    + "| \\<*<dataText>\n"
    + "| \\<:\\><genContentNoWhitespace?>\\<\\.\\>\n"
    + "| <text>                        ##text after whitespace but inclusive trailing whitespaces till next control <: <* <.\n"
    + "}.\n"
    + "\n"
    + "\n"
    + "text::=<?><*|\\<:|\\<+|\\<=|\\<*|\\<\\.?text>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "dataText::=<objExpr?>[ : <\"\"?formatText>]     ##simple expression\n"
    + "     [ \\> \\<+\\> [{<?actualArgument> \\<:\\><genContentNoWhitespace?>\\<\\.\\> | \\<*<objExpr?>\\> | <text> ? \\<+\\> }] \\<\\.*\\> \n"
    + "     | \\> ].  ##expression with arguments\n"  
    + "\n"
    + "\n"
    + "objExpr::= <textExpr?> \n"         //either a text or a datapath, which may be any object, 
    + "  | <expression>.\n"              //or an expression
    + "\n"
    + "\n"
    + "textExpr::=  <\"\"?text> | \\<:\\><genContentNoWhitespace?genString>\\<\\.\\> \n"   
    + "  | new <newJavaClass> | [java |$!] <staticJavaMethod> \n" 
    + "  | info ( <?info> <datapath?>  ) \n"              
    + "  | $$<$?envVariable> | <datapath?> .\n"
    + "\n"
    + "\n"
    + "\n"
    + "expression::= \\<:\\><genContentNoWhitespace?genString>\\<\\.\\> \n"
    + "            | <multExpr?> [{ + <multExpr?addOperation> | - <multExpr?subOperation>}].\n"
    + "\n"
    + "\n"
    + "multExpr::= <value?startOperation> [{ * <value?multOperation> | / <value?divOperation> }].\n"
    + "\n"
    + "value::= [<?operator> + | - |] [<?unaryOperator> ! | ~ |]\n"
    + "   [<#?intValue> | 0x<#x?intValue> | <#f?floatValue> | '<!.?charValue>' | <\"\"?textValue> \n"
    + "   \n" 
    + "   | new <newJavaClass> | [java |$!] <staticJavaMethod> \n" 
    + "   | info ( <datapath?> <?info> ) | $$<$?envVariable> | <datapath?> <?datapath>].\n"
    + "\n"
    //+ "objvalue::=\n"
    + "\n"
    + "newJavaClass::= <$\\.?javapath> [ ({ <objExpr?argument> ? , } )].\n" 
    + "staticJavaMethod::= <$\\.?javapath> [ (+)<?extArgs>| ( [ { <objExpr?argument> ? , } ] )].\n"
    + "##a javapath is the full package path and class [.staticmetod] separated by dot. \n"
    + "\n"
    + "datapath::=<?> [$] <$?startVariable>[ [?\\. \\>] \\.{ <datapathElement> ? [?\\. \\>] \\.}].\n" // |{ <datapathElement> ? [?\\. \\>] \\.}.  \n"
    + "\n"
    + "datapathElement::=<$@-?ident> [( [{ <objExpr?argument> ? ,}])<?whatisit=r>].\n"  
    + "\n"
    
    + "XXXtextInStatement::=<?> <\"\"?XXXtextInStatement> | \\<:\\><genContentNoWhitespace?>\\<\\.\\> | <*\\ \\r\\n?XXXtextInStatement>.\n"
    
    + "genContentNoWhitespace::=<$NoWhiteSpaces>\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<*<dataText>\n"
    + "| \\<:n\\><?newline>\n"
    + "| <text>           ##text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    + "variableDef::=<?> <textVariable> | <objVariable>.\n"
    + "textVariable::= <$?name> \\> <genContent?>  \\<\\.=\\>.\n"
    + "objVariable::= <$?name> : <objExpr?> [ \\.\\>   ##simple expression\n"
    + "     | \\> [{ \\<:\\><genContentNoWhitespace?>\\<\\.\\> | \\<*<objExpr?>\\> | <text> ? \\<+\\> }] \\<\\.=\\>].  ##expression with arguments\n" 
    + "\n"
    + "namedArgument::= <$?name>[ = <objExpr?>].\n"
    + "\n"
    + "extArgument::=  \\<:\\><genContentNoWhitespace?>\\<\\.\\> | \\<*<objExpr?>\\> | <text>.\n"
    + "\n"
    + "forContainer::= [$]<$?@name> : <objExpr?> \\> <genContent?> \\<\\.for[ : <$?@name> ]\\>. ##name is the name of the container element data reference\n"
    + "\n"
    + "if::= <ifBlock> [{ \\<:elsif : <ifBlock>  }][ \\<:else\\> <genContent?elseBlock> ] \\<\\.if\\>.\n"
    + "ifBlock::= <condition> \\> <genContent?>.\n"
    + "\n"
    + "condition::=<?><expression> [<cmpOperation>].\n"  //NOTE: it is stored in the ifBlock.
    + "\n"
    + "cmpOperation::=[ \\?[<?name>gt|ge|lt|le|eq|ne] |  [<?name> != | == ]] <expression>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "mainScript::= <execScript?> . \n"
    + "\n"
    + "subScript::= <$?name> ( [{ <namedArgument?formalArgument> ? , }] ) <execScript?>. \n"

    + "\n"
    + "execScript::= \\{ [{ <execScriptStatement?> }] \\}.\n"

    + "execScriptStatement::=\n"
    + "  <execScript?statementBlock> \n"
    + "| Pipe <VarDef?Pipe> ;\n"
    + "| Stringb <VarDef?Stringb> ;\n"
    + "| Obj <VarDef?objVariable> ;\n"
    + "| String <VarString?textVariable> ; \n"
    + "| List <VarDef?List> ; \n"
    + "| Openfile <Openfile> \n"
    + "| for <forScript?forContainer> \n"
    + "| call <callScript?callSubtext> \n"
    + "| if <ifScript?if> \n"
    + "| \\<+ <textOut> \n"
    + "| [{ <datapath?-assign> = }] cmd <cmdLine?+?> \n"  ///
    + "| start <cmdLine?cmdStart> \n"
    //+ "| [{ <datapath?-assign> = }] java <staticJavaMethod?+?> \n"
    + "| <assignment> \n"
    + "| break <?breakBlock> ;\n"
    + "| exit <#?exitScript> ;\n"
    + "| onerror <onerror> \n"
    + ".\n"
    + "\n"
    + "assignment::= [{ <datapath?assign> = }] <objExpr?> ;.\n"
    + "\n"
    + "onerror::= [<#?errorLevel>] [<?errortype> notfound | file | internal|] <execScript?> .\n"
    + "\n"

    + "\n"
    + "VarDef::= <$?name> [ = <objExpr?>].\n"  //a text or object or expression
    + "\n"
    + "VarString::= <$?name> = \\<:\\><genContentNoWhitespace?>\\<\\.\\>.\n"
    + "\n"
    + "Openfile::= <$?name> = <textExpr?> ;.\n"
    + "\n"
    + "ifScript::= <ifScriptBlock?ifBlock> [{ elsif <ifScriptBlock?ifBlock>  }][ else <execScript?elseBlock> ].\n"
    + "\n"
    + "ifScriptBlock::= ( <condition> ) <execScript?> .\n"
    + "\n"
    + "forScript::= ( [$]<$?@name> : <objExpr?> )  <execScript?> .\n"
    + "\n"
    + "callScript::= <textExpr?callName> ( [{ <namedArgument?actualArgument> ? , }] ) ; .\n"
    + "callSubtext::=[<\"\"?callName>|<textExpr?callNameExpr>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + "\n"
    + "textOut::= <$?name> \\> <genContentNoWhitespace?>[ \\<\\.+\\> | \\<\\.n+\\><?newline>].\n"
    + "\n"
    + "cmdLine::= <textExpr?> [{[?;[\\ |\\n|\\r]] [\\<:arg\\>] <textExpr?actualArgument> }] \n"
    + "  [ \\<:stdout:[ pipe<?pipe>| [$]<$?stdoutVariable>] \\>] ;.\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
       + "\n";
 
  


  
}
