package org.vishia.xmlReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;



/*Test with jztxtcmd: call jztxtcmd with this java file with its full path:
D:/vishia/ZBNF/srcJava_vishiaBase/org/vishia/xmlReader/XmlReader.java
==JZtxtcmd==
##yet empty, TODO test
==endJZcmd==
 */


/**This is the main class to read an XML file with a given configuration file, store data in a Java instance via reflection paths given in the config.xml.
 * A configuration file, written in XML too, contains the data path for the XML elements which should be stored in Java data.
 * A main method does not exists because it is only proper to invoke the read routine inside a Java application. 
 * <br>
 * Application example:
 * <pre>
 * XmlReader xmlReader = new XmlReader;   //instance to work, more as one file one after another
 * xmlReader.setCfg(cfgFile);             //configuration for next xmlRead()
 * AnyClass data = new AnyClass();        //a proper output instance matching to the cfg
 * xmlReader.readXml(xmlInputFile, data); //reads the xml file and stores read data.
 * </pre>
 * 
 * The configuration file contains the template of the xml file with paths to store the content in a proper user instance
 * The paths is processed with {@link DataAccess.DatapathElement}. The configuration is hold in an instance of {@link XmlCfg}.
 * Detail description of the config file see {@link XmlCfg}.
 * @author Hartmut Schorrig.
 *
 */
public class XmlReader
{
  /**Version, License and History:
   * <ul>
   * <li>2017-12-25 first version which can be used.
   * <li>2017-01 created.
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
  public static final String version = "2017-12-25";
  
  
  /**To store the read configuration. */
  XmlCfg cfg = new XmlCfg();
  
  
  /**Configuration to read a config file. */
  final XmlCfg cfgCfg;
  
  
  
  /**Size of the buffer to hold a part of the xml input file. It should be enough big to hold 1 element (without content).
   * 
   */
  int sizeBuffer = 20000;
  
  int debugStopLine = -1;
  
  /**Assignment between nameSpace-alias and nameSpace-value gotten from the xmlns:ns="value" declaration in the read XML file. */
   Map<String, String> namespaces = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
   
   
  public XmlReader() {
    cfgCfg = XmlCfg.newCfgCfg();
  }   
   
  
  /**Only for internal debug. See implementation. There is a possibility to set a break point if the parser reaches the line.
   * @param line
   */
  public void setDebugStop(int line) {
    debugStopLine = line;
  }
   
  public void readXmlCfg(File input) {
    cfg = new XmlCfg();
    //read
  }
  

  public void readXml(File input, Object output, CharSequence xmlCfg) {
  }


  private String readXml(File input, Object output, XmlCfg xmlCfg) {
    String error = null;
    InputStream sInput = null; 
    try{ 
      sInput = new FileInputStream(input);
      String sPathInput = FileSystem.normalizePath(input.getAbsoluteFile()).toString();
      error = readXml(sInput, sPathInput, output, xmlCfg);
      sInput.close();
    } catch(IOException exc) {
      error = "XmlReader.readXml(...) file not found: " + input.getAbsolutePath();
    }
    return error;
  }




  public String readZipXml(File zipInput, String pathInZip, Object output) {
    String error = null;
    try {
      ZipFile zipFile = new ZipFile(zipInput);
      ZipEntry zipEntry = zipFile.getEntry(pathInZip);
      InputStream sInput = zipFile.getInputStream(zipEntry);
      String sInputPath = zipInput.getAbsolutePath() + ":" + pathInZip;
      error = readXml(sInput, sInputPath, output, cfg);
      sInput.close();
      zipFile.close();
    } catch(Exception exc) {
      error = exc.getMessage();
    } 
    return error;
  }



  
  /**Reads the xml content from an opened stream.
   * The stream is firstly tested whether the first line contains a encoding hint. This is obligate in XML.
   * Then the input is read into a character buffer using the {@link StringPartFromFileLines} class. 
   * The {@link StringPartScan} scans the XML syntax. 
   * <br>
   * The xmlCfg determines which elements, attributes and textual content is transferred to the output data.
   * See Description of {@link XmlReader}.
   * @param input any opened InputStream. Typically it is an FileInputStream or InputStream from a {@link ZipEntry}.
   * @param sInputPath The path to the input stream, used for error hints while parsing.
   * @param output Any output data. The structure should match to the xmlCfg.
   * @param xmlCfg A configuration. It can be gotten via {@link #readCfg(File)}.
   * @return null if no error. Elsewhere an error message, instead of throwing.
   */
  public String readXml(InputStream input, String sInputPath, Object output, XmlCfg xmlCfg) {
    String error = null;
    StringPartFromFileLines inp = null;
    try {
      inp = new StringPartFromFileLines(input, sInputPath, sizeBuffer, "encoding", null);
      readXml(inp, output, xmlCfg);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(Exception exc) {
      exc.printStackTrace();
      Debugutil.stop();
    } finally {
      if(inp !=null) { inp.close(); }
    }
    return error;
  }
  
  
  
  /**Core routine to read in whole XML stream.
   * @param inp
   * @param output
   * @param cfg1
   * @throws Exception
   */
  private void readXml(StringPartFromFileLines inp, Object output, XmlCfg cfg1) 
  throws Exception
  { inp.setIgnoreWhitespaces(true);
    inp.seekEnd("<"); //search the first start "<". Skip over all other characters. Only spaces are expected.
    if(inp.scan("?").scanOk()) { //skip over the first "<?xml declaration line ?>
      inp.seekEnd("?>");    //skip over the <? head info ?>. Note: The encoding is regarded from StringPartFromFileLines
      inp.scanOk();    //sets the scan start to this position.
    }
    while(inp.seekEnd("<").found()) {
      inp.scanOk();
      inp.readnextContentFromFile(sizeBuffer/2);
      if(inp.scan().scan("---").scanOk()) { //comment line
       inp.seekEnd("--->");
      }
      else {
        parseElement(inp, output, cfg1.rootNode);  //the only one root element.
      }
    }
    Debugutil.stop();
  }



  /**Parse a whole element with all inner content
   * @param inp scanOk-Position after the "<" before the identifier.
   * @param output
   * @param cfg1
   * @throws Exception 
   */
  private void parseElement(StringPartFromFileLines inp, Object output, XmlCfg.XmlCfgNode cfgNode) 
  throws Exception
  { 
    if(debugStopLine >=0){
      int line = inp.getLineAndColumn(null);
      if(line == debugStopLine)
        Debugutil.stop();
    }
    //scan the <tag
    if(!inp.scanIdentifier(null, "-:").scanOk()) throw new IllegalArgumentException("tag name expected");
    //
    //The tag name of the element:
    CharSequence sTag = inp.getLastScannedString();
    //TODO replace alias.
    //
    //search the tag name in the cfg:
    //
    Object subOutput;
    XmlCfg.XmlCfgNode subCfgNode;
    if(cfgNode == null) {   //check whether this element should be regarded:
      subOutput = null;     //this element should not be evaluated.
      subCfgNode = null;
    } else {
      Assert.check(output !=null);
      subCfgNode = cfgNode.subnodes == null ? null : cfgNode.subnodes.get(sTag);  //search the proper cfgNode for this <tag
      if(subCfgNode == null) { subCfgNode = cfgNode.subNodeUnspec; }
      //
      //get the subOutput before parsing attributes because attribute values should be stored in the sub output.:
      if(subCfgNode !=null && subCfgNode.bStoreAttribsInNewContent) { //the tag was found, the xml element is expected.
        subOutput = getDataForTheElement(output, subCfgNode, sTag, null);
        if(subOutput == null) {
          Debugutil.stop();
        }
        //
      } else {
        subOutput = null; //don't store output. 
      }
    }
    //
    @SuppressWarnings("unchecked")
    Map<String, String>[] attribs = new Map[1];
    //
    //
    CharSequence keyResearch = parseAttributes(inp, sTag, subOutput, subCfgNode, attribs);
    //
    //
    //get the subOutput after parsing attributes because attribute values may be used to create the sub output:
    if(subCfgNode !=null && !subCfgNode.bStoreAttribsInNewContent) { //the tag was found, the xml element is expected.
      subOutput = getDataForTheElement(output, subCfgNode, sTag, attribs);
    }
    //
    if(keyResearch==null) {
      subOutput = null;
    } else if(keyResearch.length() >0) {
      subCfgNode = cfgNode.subnodes == null ? null : cfgNode.subnodes.get(keyResearch);  //search the proper cfgNode for this <tag
      subOutput = subCfgNode == null ? null : getDataForTheElement(output, subCfgNode, keyResearch, attribs);
    }
    //
    //check content.
    //
    if(inp.scan("/").scan(">").scanOk()) {
      //end of element
    }
    else if(inp.scan(">").scanOk()) {
      //textual content
      StringBuilder content = null;
      //
      //loop to parse <tag ...> THE CONTENT </tag>
      while( ! inp.scan().scan("<").scan("/").scanOk()) { //check </ as end of node
        inp.readnextContentFromFile(sizeBuffer/2);
        if(inp.scan("<").scanOk()) {
          if(inp.scan("!--").scanOk()) {
            inp.seekEnd("-->");
          } else {
            parseElement(inp, subOutput, subCfgNode);  //nested element.
          }
        } else {
          if(content == null && subOutput !=null) { content = new StringBuilder(500); }
          parseContent(inp, content);  //add the content between some tags to the content Buffer.
        }
      }
      //
      inp.readnextContentFromFile(sizeBuffer/2);
      //the </ is parsed on end of while already above.
      if(!inp.scanIdentifier(null, "-:").scanOk())  throw new IllegalArgumentException("</tag expected");
      inp.setLengthMax();  //for next parsing
      if(!inp.scan(">").scanOk())  throw new IllegalArgumentException("</tag > expected");
      if(content !=null && subOutput !=null) {
        storeContent(content, subCfgNode, subOutput, attribs);
      }
    } else {
      throw new IllegalArgumentException("either \">\" or \"/>\" expected");
    }
    inp.setLengthMax();  //for next parsing
  }










  /**
   * @param inp
   * @param tag
   * @param output
   * @param cfgNode
   * @param attribMap
   * @return null then do not use this element because faulty attribute values. "" then no special key, length>0: repeat search config.
   * @throws Exception
   */
  private CharSequence parseAttributes(StringPartFromFileLines inp, CharSequence tag, Object output, XmlCfg.XmlCfgNode cfgNode, Map<String, String>[] attribMap) 
  throws Exception
  { CharSequence keyret = ""; //no special key. use element.
    StringBuilder keyretBuffer = null;
    //read all attributes. NOTE: read formally from text even if bUseElement = false.
    while(inp.scanIdentifier(null, "-:").scan("=").scanOk()) {  //an attribute found:
      final CharSequence sAttrNsNameRaw = inp.getLastScannedString();
      if(!inp.scanQuotion("\"", "\"", null).scanOk()) throw new IllegalArgumentException("attr value expected");
      if(cfgNode !=null) {
        String sAttrValue = inp.getLastScannedString().toString();  //"value" in quotation
        int posNs = StringFunctions.indexOf(sAttrNsNameRaw, ':');  //namespace check
        final CharSequence sAttrNsName;
        if(posNs >=0) {
          //Namespace
          CharSequence ns = sAttrNsNameRaw.subSequence(0, posNs);
          final CharSequence sAttrName = sAttrNsNameRaw.subSequence(posNs+1, sAttrNsNameRaw.length());
          //String nsName = sAttrName.toString();
          if(StringFunctions.equals(ns, "xmlns")){
            String nsValue = sAttrValue.toString();
            namespaces.put(sAttrName.toString(), nsValue);
            sAttrNsName = null;
          } else {
            String nsValue = namespaces.get(ns);  //defined in this read xml file.
            if(nsValue == null) {
              sAttrNsName = null;  //Namespace not registered in the input file, especially "xml".
            } else if(cfgNode.cfg.xmlnsAssign !=null) {
              String nsCfg = cfgNode.cfg.xmlnsAssign.get(nsValue);
              //Todo search defined name in cfg for the nameSpace.
              if(nsCfg == null) { 
                sAttrNsName = null;  //Namespace not in cfg registered. 
              } else {
                sAttrNsName = nsCfg + ":" + sAttrName; //nameSpace alias from cfg file. Unified.
              }
            } else {
              //read the config file, here the xmlnsAssign is null, use the attribute name as given.
              sAttrNsName = sAttrNsNameRaw;
            }
          }
        }
        else {
          sAttrNsName = sAttrNsNameRaw;
        }
        if(sAttrNsName !=null) {
           XmlCfg.AttribDstCheck cfgAttrib= cfgNode.attribs == null ? null : cfgNode.attribs.get(sAttrNsName);
           if(cfgAttrib != null) {
             if(cfgAttrib.bUseForCheck) {
               if(keyretBuffer == null) { keyretBuffer = new StringBuilder(64); keyretBuffer.append(tag); keyret = keyretBuffer; }
               keyretBuffer.append("@").append(sAttrNsName).append("=\"").append(sAttrValue).append("\"");
             }
             else if(cfgAttrib.daccess !=null) {
               storeAttrData(output, cfgAttrib.daccess, sAttrNsName, sAttrValue);
             } else if(cfgAttrib.storeInMap !=null) {
               if(attribMap[0] == null){ attribMap[0] = new TreeMap<String, String>(); }
               attribMap[0].put(cfgAttrib.storeInMap, sAttrValue);
             }
           } else {
             if(cfgNode.attribsUnspec !=null) { //it is especially to read the config file itself.
               storeAttrData(output, cfgNode.attribsUnspec, sAttrNsName, sAttrValue);
             }
           }
        }
      }
      inp.readnextContentFromFile(sizeBuffer/2);
    } //while
    return keyret;
  }





  /**Invokes the associated method to get/create the appropriate data instance for the element.
   * @param output
   * @param subCfgNode
   * @param sTag
   * @return
   * @throws Exception
   */
  @SuppressWarnings("static-method")
  Object getDataForTheElement( Object output, XmlCfg.XmlCfgNode subCfgNode, CharSequence sTag, Map<String, String>[] attribs) 
  {
    Object subOutput;
    if(subCfgNode.elementStorePath == null) { //no attribute xmlinput.data="pathNewElement" is given:
      subOutput = output; //use the same output. No new element in data.
    }
    else {
      try{ 
        int nrArgs = subCfgNode.elementStorePath.nrArgNames();
        Object[] args;
        if(nrArgs >0) {
          args = new Object[nrArgs]; 
          for(int ix = 0; ix < nrArgs; ++ix) {
            String argName = subCfgNode.elementStorePath.argName(ix);
            if(attribs !=null && attribs[0]!=null && (args[ix] = attribs[0].get(argName))!=null){} //content of attribute filled in args[ix]
            else if(argName.equals("tag")) { args[ix] = sTag; }
            else throw new IllegalArgumentException("argname");
          }
          subOutput = DataAccess.invokeMethod(subCfgNode.elementStorePath, null, output, true, false, args);
        } else {
          //it may be a method too but without textual parameter.
          subOutput = DataAccess.access(subCfgNode.elementStorePath, output, true, false, false, null);
        }
        if(subOutput == null) {
          System.err.println("getDataForTheElement returns null");
        }
      } catch(Exception exc) {
        subOutput = null;
        System.err.println("error getDataForTheElement: " + exc.getMessage());
      }
    }
    return subOutput;
  }



  /**Invokes the associated method to store the attribute value.
   * <ul>
   * <li> This method is invoked while reading the cfg.xml to store the attribute value with XmlNode#addAttributeStorePath().
   * <li> This method is invoked while reading the user.xml to store the users attribute value in the users data.
   * </ul>
   * @param output It is an instance of {@link XmlCfgNode} while reading the cfg.xml, it is an user instance while reading the user.xml
   * @param dstPath The dataAccess which should be executed.
   * @param sAttrNsName
   * @param sAttrValue
   */
  @SuppressWarnings("static-method")
  void storeAttrData( Object output, DataAccess.DatapathElement dstPath, CharSequence searchKey, CharSequence sAttrValue) 
  {
    try{ 
      int nrArgs = dstPath.nrArgNames();
      Object[] args;
      if(nrArgs >0) {
        args = new Object[nrArgs]; 
        for(int ix = 0; ix < nrArgs; ++ix) {
          String argName = dstPath.argName(ix);
          if(argName.equals("name")) { args[ix] = searchKey; }
          else if(argName.equals("value")) { args[ix] = sAttrValue; }
          else throw new IllegalArgumentException("argname");
        }
        DataAccess.invokeMethod(dstPath, null, output, true, false, args);
      } else {
        DataAccess.storeValue(dstPath, output, sAttrValue, true);
      }
    } catch(Exception exc) {
      System.err.println("error storeAttrData: " + exc.getMessage());
    }
  }




  @SuppressWarnings("static-method")
  private void setOutputAttr(Object output, DataAccess.DatapathElement dstPath, CharSequence name, CharSequence value)
  {
    if(dstPath.equals("@") && output instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> oMap = (Map<String, Object>)output;
      oMap.put("@"+ name, value.toString());
    }
  }



  /**
   * @param inp
   * @param buffer maybe null then ignore content.
   */
  private void parseContent(StringPartFromFileLines inp, StringBuilder buffer)
  throws IOException
  { boolean bContReadContent;
    int posAmp = buffer == null ? 0 : buffer.length()-1; //NOTE: possible text between elements, append, start from current length.
    inp.seekNoWhitespace();
    do { //maybe read a long content in more as one portions.
      inp.lento('<');  //The next "<" is either the end with </tag) or a nested element.
      bContReadContent = !inp.found();
      if(bContReadContent) {
        inp.setLengthMax();
      } else {
        inp.lenBacktoNoWhiteSpaces();
      }
      CharSequence content1 = inp.getCurrentPart();
      inp.fromEnd();
      if(buffer !=null && buffer.length() > 0) { 
        //any content already stored, insert a space between the content parts.
        buffer.append(' ');
      }
      if(buffer !=null) { buffer.append(content1); }
      inp.readnextContentFromFile(sizeBuffer/2);
    } while(bContReadContent);
    if(buffer !=null) {
      while( (posAmp  = buffer.indexOf("&", posAmp+1)) >=0) {  //replace the subscription of &lt; etc.
        if(StringFunctions.startsWith(buffer, posAmp+1, posAmp+4, "lt;")) { buffer.replace(posAmp, posAmp+4, "<");  }
        else if(StringFunctions.startsWith(buffer, posAmp+1, posAmp+4, "gt;")) { buffer.replace(posAmp, posAmp+4, ">");  }
        else if(StringFunctions.startsWith(buffer, posAmp+1, posAmp+4, "amp;")) { buffer.replace(posAmp, posAmp+4, "&");  }
        else if(StringFunctions.startsWith(buffer, posAmp+1, posAmp+4, "auml;")) { buffer.replace(posAmp, posAmp+4, "�");  }
      }
    }
  }
  
  
  
  private void storeContent(StringBuilder buffer, XmlCfg.XmlCfgNode cfgNode, Object output, Map<String, String>[] attribs) {
    DataAccess.DatapathElement dstPath = cfgNode.contentStorePath;
    if(dstPath !=null) {
      try{ 
        int nrArgs = dstPath.nrArgNames();
        Object[] args;
        if(nrArgs >0) {
          args = new Object[nrArgs]; 
          for(int ix = 0; ix < nrArgs; ++ix) {
            String argName = dstPath.argName(ix);
            if(attribs[0]!=null && (args[ix] = attribs[0].get(argName))!=null){} //content of attribute filled in args[ix]
            else if(argName.equals("text")) { args[ix] = buffer; }
            else throw new IllegalArgumentException("argname");
          }
          DataAccess.invokeMethod(dstPath, null, output, true, false, args);
        } else {
          DataAccess.storeValue(dstPath, output, buffer, true);
        }
      } catch(Exception exc) {
        System.err.println("error storeContent: " + exc.getMessage());
      }
    }
  }

  public void readCfg(File file) {
    readXml(file, this.cfg.rootNode, this.cfgCfg);
    cfg.transferNamespaceAssignment(this.namespaces);
  }



  public void readXml(File file, Object dst) {
    this.readXml(file, dst, this.cfg);
  }  
    
  



}
