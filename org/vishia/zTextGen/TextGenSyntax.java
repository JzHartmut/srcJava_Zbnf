package org.vishia.zTextGen;

public final class TextGenSyntax {

  
  /**Version, history and license.
   * <ul>
   * <li>2013-01-05 Hartmut new: A sumExpression is a concatenation of strings or + or - of numerics. It is used for all value expressions.
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
  static final public int version = 20121226;

  
  public final static String syntax =
    "$comment=(?...?).\n"
    + "$endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + "\n"
    + "ZTextctrl::= \n"
    + "{ <subtext> \n"
    + "| <genFile> \n"
    + "| \\<= <variableDef>\n"
    + "} \\e.\n"
    + "\n"
    + "\n"
    + "subtext::= \\<:subtext : <$?name> [ : { <namedArgument?formalArgument> ? , }] \\><genContent?> \\<\\.subtext\\>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "##A genControl script should have a part <:file>....<.file> which describes how the whole file should build.\n"
    + "\n"
    + "genFile::= \\<:file\\>\n"
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
    + "| \\<*<dataText>\\>\n"
    + "| \\<:\\><genContentNoWhitespace?>\\<\\.\\>\n"
    + "| <*|\\<:|\\<*|\\<\\.?text>                        ##text after whitespace but inclusive trailing whitespaces till next control <: <* <.\n"
    + "}.\n"
    + "\n"
    + "\n"
    + "\n"
    + "callSubtext::=[<\"\"?name>|<sumExpression>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + "\n"
    + "dataText::=<sumExpression>[ : <\"\"?formatText>].\n"  //<*dataText>
    + "\n"
    + "sumExpression::={ <sumValue> ? [! + | -] }.\n"
    + "\n"
    + "sumValue::= [<?operator> + | -|] [<#?intValue> | 0x<#x?intValue> | <#f?floatValue> | '<!.?charValue>' | <\"\"?textValue> \n"
    + "              | $new\\  <newJavaClass> | $!<staticJavaMethod> | $$<$?envVariable> | <datapath>].\n"
    + "\n"
    + "newJavaClass::= <$\\.?javapath> [ ({ <sumExpression?argument> ? , } )].\n" ///
    + "staticJavaMethod::= <$\\.?javapath> ( [ { <sumExpression?argument> ? , } ] ).\n"
    + "##a javapath is the full package path and class [.staticmetod] separated by dot. \n"
    + "\n"
    + "datapath::=<?> $<$?startVariable>[\\.{ <datapathElement> ? \\.}] |{ <datapathElement> ? \\.}.  \n"
    + "\n"
    + "datapathElement::=<$@-?ident> [( [{ <sumExpression?argument> ? ,}])<?whatisit=r>].\n"  
    + "\n"
    + "genContentNoWhitespace::=<$NoWhiteSpaces>\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<*<dataText>\\>\n"
    + "| <*|\\<:|\\<*|\\<\\.?text>           ##text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    + "variableDef::=<?> <textVariable> | <objVariable>.\n"
    + "textVariable::= <$?name> \\> <genContent?>  \\<\\.=\\>.\n"
    + "objVariable::= <$?name> : <sumExpression> \\>.\n"
    + "\n"
    + "namedArgument::= <$?name>[ = <sumExpression>].\n"
    + "\n"
    + "forContainer::= [$]<$?@name> : <sumExpression> \\> <genContent?> \\<\\.for[ : <$?@name> ]\\>. ##name is the name of the container element data reference\n"
    + "\n"
    + "if::= <ifBlock> [{ \\<:elsif : <ifBlock>  }][ \\<:else\\> <genContent?elseBlock> ] \\<\\.if\\>.\n"
    + "ifBlock::= <condition> \\> <genContent?>.\n"
    + "\n"
    + "condition::=<?><sumExpression> [<cmpOperation>].\n"  //NOTE: it is stored in the ifBlock.
    + "\n"
    + "cmpOperation::=[ \\?[<?name>gt|ge|lt|le|eq|ne] |  [<?name> != | == ]] <sumExpression>\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n";
 
  


  
}
