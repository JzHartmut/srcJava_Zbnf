package org.vishia.xmlReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.cmd.JZtxtcmdTester;
import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions_B;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;

import javafx.scene.Node;

/**This class reads any XML file and writes its structure to a XmlCfg format. 
 * With it the xmlCfg to read that file to data can be prepared.
 * @author Hartmut Schorrig
 *
 */
public class XmlJzCfgAnalyzer
{
  /**Version, License and History:
   * <ul>
   * <li>2018-09-10 writes cfg attributes.
   * <li>2018-08-15 created.
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
  public static final String version = "2018-08-15";

  
  int debugStopLineXmlInp = -1;
  
  /**The common structure data of the read XML file. */
  final XmlStructureData xmlStructData = new XmlStructureData();
  
  /**The tree of the structure of the read XML file. */
  XmlStructureNode xmlStructTree = new XmlStructureNode(null, "root", xmlStructData);  //the root node for reading config

  
  
  public XmlJzCfgAnalyzer(){
    
  }
  
  /**Only for internal debug. See implementation. There is a possibility to set a break point if the parser reaches the line.
   * @param line
   */
  public void setDebugStop(int line) {
    debugStopLineXmlInp = line;
  }
 
  
  
  
  public void writeCfgTemplate(File wrCfg) {
    FileWriter writer = null;
    try {
      //Output it as cfg.xml
      XmlNodeSimple<?> root = new XmlNodeSimple<>("xmlinput:root");
      root.addNamespaceDeclaration("xmlinput", "www.vishia.org/XmlReader-xmlinput");
      
      //for(Map.Entry<String, XmlStructureData.CfgSubtreeType> e: this.xmlStructData.allElementTypes.entrySet()) {
        //XmlStructureData.CfgSubtreeType e1 = e.getValue();
        //for(XmlStructureNode srcnode: e1.occurrence) {
        for(XmlStructureNode srcnode: this.xmlStructData.cfgSubtreeList) {
            //add one subtree node for each tag type in its context:
          assert(srcnode.sSubtreenode !=null);  //it should be designated.
          XmlNode wrCfgsubtreenode = root.addNewNode("subtree", "xmlinput"); //second node "cfg"
          wrCfgsubtreenode.setAttribute("name", null, srcnode.sSubtreenode);
          wrCfgsubtreenode.setAttribute("class", "xmlinput", srcnode.sSubtreenode);
          if(srcnode.nodes !=null) for(Map.Entry<String, XmlStructureNode> e_srcSubnode: srcnode.nodes.entrySet()) {
            XmlStructureNode srcSubnode = e_srcSubnode.getValue();
            XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(srcSubnode.tag);
            wrCfgsubtreenode.addContent(xmlNodeSub);
            addWrNode(xmlNodeSub, srcSubnode, 999);
          }
        }
      //}
      //add the root cfg node with all its subnodes:
      XmlNode cfgnode = root.addNewNode("cfg", "xmlinput"); //second node "cfg"
      //
      //add all nodes from data, recursively called.
      addWrNode(cfgnode, xmlStructTree, 999);  
      //
      SimpleXmlOutputter oXml = new SimpleXmlOutputter();
      writer = new FileWriter(wrCfg);
      oXml.write(writer, root);
      writer.close();
    } catch(XmlException | IOException exc) {
      CharSequence error = Assert.exceptionInfo("unexpected", exc, 0, 100);
      System.err.append(error);
    } finally {
      if(writer !=null) {
        try { writer.close(); } catch(IOException exc) { System.err.append("cannot close"+wrCfg.getAbsolutePath());}
      }
    }
    
    Debugutil.stop();
  }

  
  
  public void XXXXXXXXXXXXXreadXmlStruct_writeCfgTemplate(File fXmlIn, File wrCfg) {
    readXmlStruct(fXmlIn);
    writeCfgTemplate(wrCfg);
  }
  
  
  /**Adds the node and recursively all sub nodes from {@link XmlStructureNode}
   * It is invoked after a XML file war read with {@link XmlCfg#newCfgReadStruct()} 
   * @param wrCfgXmlNode The xml node for output to add.
   * @param structNode The node from the structure of the read XML file, 
   * @param recursion decremented, exception on <=0
   * @throws XmlException on XML error, IllegalArgumentException on recursion error.
   */
  private void addWrNode(XmlNode wrCfgXmlNode, XmlStructureNode structNode, int recursion) throws XmlException {
    if(recursion <0) throw new IllegalArgumentException();
    CharSequence sArg;
    
    if(structNode.attribs !=null) {
      StringBuilder uArg= new StringBuilder(100);
      sArg = uArg;
      char sep = '(';
      for(String name: structNode.attribs.keySet()) {
        wrCfgXmlNode.setAttribute(name, "!@"+name);
        uArg.append(sep).append(name);
        sep = ',';
      }
      uArg.append(')');
    } else {
      sArg = "()";
    }
    if(structNode.nodes !=null || structNode.attribs !=null) {
      if(structNode.onlySingle) {
        wrCfgXmlNode.setAttribute("data", "xmlinput", "!set_" + structNode.tagIdent + sArg);
      } else {
        wrCfgXmlNode.setAttribute("data", "xmlinput", "!add_" + structNode.tagIdent + sArg);
      }
      if(structNode.nodes !=null) { //has subnodes
        if(structNode.sSubtreenode !=null) {
          //The tag type occurs more as one time in different situations, but with the same meaning. Use subtree in cfg.
          String sSubtreeName = structNode.sSubtreenode;
          wrCfgXmlNode.setAttribute("subtree", "xmlinput", sSubtreeName);
        } else {
          //Only one time tag type: tree inside.
          wrCfgXmlNode.setAttribute("class", "xmlinput", structNode.tagIdent  );
          for(Map.Entry<String, XmlStructureNode> e: structNode.nodes.entrySet()) {
            XmlStructureNode subnode = e.getValue();
            String tag = e.getKey();
            XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(tag);
            wrCfgXmlNode.addContent(xmlNodeSub);
            addWrNode(xmlNodeSub, subnode, recursion-1);
          }
        }
      }
      if(structNode.bText) {
        wrCfgXmlNode.addContent("!set_text()");
      }
    } else { //no attribs, no sub tree
      if(structNode.onlySingle) {
        wrCfgXmlNode.addContent("!set_" + structNode.tag + "()");
      } else {
        wrCfgXmlNode.addContent("!add_" + structNode.tag + "()");
      }
    }
  }


  /**Reads any XML file and stores the structure in the {@link #xmlStructTree}.
   *  
   * @param fXmlIn
   */
  public void readXmlStruct(File fXmlIn) {
    XmlJzReader xmlReader = new XmlJzReader();
    if(debugStopLineXmlInp >0) {
      xmlReader.setDebugStop(debugStopLineXmlInp);
    }
    xmlReader.readXml(fXmlIn, xmlStructTree, newCfgReadStruct());
    xmlStructData.checkCfgSubtree(); //removeSingleEntries();
    Debugutil.stop();
  }


  
  /**Creates the configuration to read any xml file to store its structure especially in {@link XmlJzCfgAnalyzer.XmlStructureNode}.
   * @return instance of XmlCfg to do so.
   */
  public static XmlCfg newCfgReadStruct()
  { XmlCfg cfgCommon = new XmlCfg();
    cfgCommon.rootNode = new XmlCfg.XmlCfgNode(null, cfgCommon, null);  //The rootnode of the cfg is only formalistic.
    
    XmlCfg.XmlCfgNode rootNode = new XmlCfg.XmlCfgNode(null, cfgCommon, "?");  //<xmlinput:cfg as node 2. level
    cfgCommon.rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    
    //On any element the 'addElement(tag)' is invoked via Reflection. 
    rootNode.setNewElementPath("!addElement(tag)");  //executed in the data destination instance.
    rootNode.addAttribStorePath("?", "!setAttribute(name)"); 
    rootNode.setContentStorePath("!setTextOccurrence()");
    return cfgCommon;
  }

  

  
  
  /**Reads a XML file and writes its structure as cfg.xml to help creating a config for the XmlJzReader
   * @param args pathfileIn.xml pathfileOutcfg.xml 
   */
  public static void main(String[] args) {
    XmlJzCfgAnalyzer main = new XmlJzCfgAnalyzer();
    main.readXmlStruct(new File(args[0]));
    //JZtxtcmdTester.dataHtmlNoExc(main.data, new File("T:/datashow.html"), true);
    main.writeCfgTemplate(new File(args[1]));
  }
  
  
  
  static class XmlStructureData {
    
    static class CfgSubtreeType {
      
      List<XmlStructureNode> occurrence = new ArrayList<XmlStructureNode>();
      
      public CfgSubtreeType(){}
      
      @Override public String toString() { return "" + occurrence.size() + " * " + occurrence.get(0).toString(); }
    }
    
    static class CfgSubtreeType2 {
      
      final String tag;
      //XmlStructureNodeBuilder node;
      List<XmlStructureNode> occurrence = new ArrayList<XmlStructureNode>();
      
      XmlStructureNode representative;
      
      List<CfgSubtreeType2> dependings = new ArrayList<CfgSubtreeType2>();
      
      boolean bSort;
      
      Map<String, String> attributeNames = new TreeMap<String, String>();
      Map<String, String> nodeNames = new TreeMap<String, String>();
      
      public CfgSubtreeType2(XmlStructureNode structNode){
        this.tag = structNode.tag;
        this.representative = structNode; //the first
        this.occurrence.add(structNode);
      }
      
      
      @Override public String toString() { return "" + occurrence.size() + " * " + occurrence.get(0).toString(); }
    }

    
    List<XmlStructureNode> XXXXXXXallElements = new ArrayList<XmlStructureNode>();
    
    
    /**Stores all node types per tagName, more as one possible with the same tag name with its occurence in the structure file. */
    IndexMultiTable<String, CfgSubtreeType> allElementTypes = new IndexMultiTable<String, CfgSubtreeType>(IndexMultiTable.providerString);
   
    /**Stores all node types with occurrence more as one time, with tag name but with extra entry for any different content (really different type with same tag). */
    IndexMultiTable<String, CfgSubtreeType2> allElementTypes2 = new IndexMultiTable<String, CfgSubtreeType2>(IndexMultiTable.providerString);
   
    
    
    /**Stores all node types for cfg subtree with the subtree name as key. */
    Map<String, XmlStructureNode> cfgSubtreeByName = new IndexMultiTable<String, XmlStructureNode>(IndexMultiTable.providerString);
    
    /**Stores the cfg subtree in the usage order. */
    List<XmlStructureNode> cfgSubtreeList = new ArrayList<XmlStructureNode>();
    
    
    
    private void createCfgSubtree(XmlStructureNode node, char nameModif) {
      CfgSubtreeType2 cfgSubtreeType = new CfgSubtreeType2(node);
      if(nameModif < 'A') {
        node.sSubtreenode = node.tag;
      } else {
        node.sSubtreenode = node.tag + '_' + nameModif;
      }
      if(node.attribs !=null) for(Map.Entry<String, String> e: node.attribs.entrySet()) {
        cfgSubtreeType.attributeNames.put(e.getKey(), e.getKey());
      }
      if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
        cfgSubtreeType.nodeNames.put(e.getKey(), e.getKey());
      }
      allElementTypes2.add(node.tag, cfgSubtreeType);  //more as one with same tag name possible, store only for comparison (check)
      cfgSubtreeByName.put(node.sSubtreenode, node);   //unique cfg-subtree key, for usage.

    }
    
    
    void addStructureNodeOccurence(XmlStructureNode node) {
      CfgSubtreeType cfgSubtreeWithAllOccurences = allElementTypes.get(node.tag); 
      if(cfgSubtreeWithAllOccurences == null) {
        cfgSubtreeWithAllOccurences = new CfgSubtreeType();
        allElementTypes.put(node.tag, cfgSubtreeWithAllOccurences);
      }
      cfgSubtreeWithAllOccurences.occurrence.add(node);
    }
    
    void XXXaddStructureNodeOccurence(XmlStructureNode node) {
      XXXXXXXallElements.add(node);
    }
    

    
    /**Invoked for all element types found in the source XML tree.
     * @param node
     */
    private void checkStructureNodeOccurence(XmlStructureNode node) {
      if(node.nodes == null && node.attribs == null) {
        return; //without sub nodes: does not need to store as subtree.
      }
      Iterator<CfgSubtreeType2> iterCfgSubtrees = allElementTypes2.iterator(node.tag);
      CfgSubtreeType2 cfgSubtreeType;
      char nameModif = 'A'-1;
      boolean found = false;
      if(!iterCfgSubtrees.hasNext() || !(cfgSubtreeType = iterCfgSubtrees.next()).tag.equals(node.tag)) {
        createCfgSubtree(node, '\0');
      }
      else {
        if(node.tag.equals("AttributeList"))
          Debugutil.stop();
        do {
          nameModif +=1;
          //check whether the found cfgSubtree seems to be the same type, because it has the same children:
          int nrfound=0, nrcount=0;
          if(node.attribs !=null) for(Map.Entry<String, String> e: node.attribs.entrySet()) {
            String key = e.getKey();
            if(cfgSubtreeType.attributeNames.get(key) !=null) {
              nrfound +=1;
            }
            nrcount +=1;
          }
          Map<String, String> foundNodeNames = new TreeMap<String, String>();
          if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
            String key = e.getKey();
            if(foundNodeNames.get(key)==null) { //only any occurrence of a subnode type only as one time type occurrence.
              foundNodeNames.put(key, key);
              if(cfgSubtreeType.nodeNames.get(e.getKey()) !=null) {
                nrfound +=1;
              }
              nrcount +=1;
            } else {
              assert(false);
            }
          }
          if(nrfound < (nrcount+3) /4) { //most (3/4) nodes or attributes are non-identical: It is another cfgSubtree type.
          } else {
            found = true;
            cfgSubtreeType.occurrence.add(node);
            if(node.attribs !=null) for(Map.Entry<String, String> e: node.attribs.entrySet()) {
              String key = e.getKey();
              if(cfgSubtreeType.attributeNames.get(key) ==null) {
                cfgSubtreeType.attributeNames.put(key, key);     //an attribute non detected as yet, add it in representative.;
                cfgSubtreeType.representative.attribs.put(key, e.getValue());
              }
            }
            if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
              String key = e.getKey();
              foundNodeNames.put(key, key);
              if(cfgSubtreeType.nodeNames.get(e.getKey()) ==null) {
                cfgSubtreeType.nodeNames.put(key, key);    //a node non detected as yet, add it in representative.
                cfgSubtreeType.representative.nodes.put(key, e.getValue());
              }
            }
            break;
          }
        } while(!found && iterCfgSubtrees.hasNext() && (cfgSubtreeType = iterCfgSubtrees.next()).tag.equals(node.tag));
        if(!found) {
          createCfgSubtree(node, nameModif);
        }
      }
    }
    
    
    
    
    private void checkCfgSubtree() {
      for(Map.Entry<String, CfgSubtreeType> e: allElementTypes.entrySet()) {
        CfgSubtreeType cfgSubtreeOccurrences = e.getValue();
        if(cfgSubtreeOccurrences.occurrence.size() >1) { 
          //only if the element with this tag name occurs more as one time in the structure tree
          for(XmlStructureNode structNode: cfgSubtreeOccurrences.occurrence ) {
            checkStructureNodeOccurence(structNode);
          }            
        }
        //XmlStructureNode node= e.getValue().representative;
      }
      //detect all dependencies in cfg-subtree
      for(Map.Entry<String, CfgSubtreeType2> e: allElementTypes2.entrySet()) {
        CfgSubtreeType2 cfgSubtree = e.getValue();
        XmlStructureNode node= e.getValue().representative;
        if(!node.bDependencyChecked) {
          node.bDependencyChecked = true;
          checkUsageSubtreenode(cfgSubtree, node, 99);
        }
      }
      for(Map.Entry<String, CfgSubtreeType2> e: allElementTypes2.entrySet()) {
        CfgSubtreeType2 cfgSubtree = e.getValue();
        processDependingCfgSubtree(cfgSubtree);
      }
    }
    
    
    
    
    private void processDependingCfgSubtree(CfgSubtreeType2 cfgSubtree) {
      if(!cfgSubtree.bSort ) {
        if(cfgSubtree.dependings.size()==0) {
          cfgSubtreeList.add(cfgSubtree.representative);
          cfgSubtree.bSort = true;
        } else {
          for(CfgSubtreeType2 dep: cfgSubtree.dependings) {
            processDependingCfgSubtree(dep);
          }
          //all dependencies processed.
          cfgSubtreeList.add(cfgSubtree.representative);
          cfgSubtree.bSort = true;
        }
      }
    }
    
    
    
    
    private void checkUsageSubtreenode(CfgSubtreeType2 cfgSubtreeNeeds, XmlStructureNode node, int recursiveCt) {
      assert(recursiveCt >= 0);
      if(node.nodes !=null) for(Map.Entry<String,XmlStructureNode> e_subnode: node.nodes.entrySet()) {
        XmlStructureNode subnode = e_subnode.getValue();
        if(subnode.sSubtreenode !=null) { //need as subtree
          if(!subnode.bDependencyChecked) {
            subnode.bDependencyChecked = true;
            //cfgSubtreeList.add(0, subnode);  //add before the evaluated node.
            CfgSubtreeType2 cfgSubtreeSub;
            Iterator<CfgSubtreeType2> iterSearch = allElementTypes2.iterator(subnode.tag);
            while(iterSearch.hasNext() && (cfgSubtreeSub = iterSearch.next()).tag.equals(subnode.tag)) {
              if(cfgSubtreeSub.representative.sSubtreenode.equals(subnode.sSubtreenode)) {
                cfgSubtreeNeeds.dependings.add(cfgSubtreeSub);
                checkUsageSubtreenode(cfgSubtreeSub, cfgSubtreeSub.representative, recursiveCt -1);  //firstly check this usage.
                break;
              }
            }
        } }
        else {
          checkUsageSubtreenode(cfgSubtreeNeeds, subnode, recursiveCt -1);  //not in cfg-subtree, process it.
        }
      }
    }
    
    
    
    
    
    
    
    
  }
  
  
  

  /**This class is one node for one element to store the structure of a XML file via {@link XmlJzReader}.
   * <ul>
   * <li>An instance of this class should be created as root node. The root node from the XML file is stored there as the first level.
   * It means, this root instance is not presented in the XML file, and the root instance has one member: The root node of the XML file.
   * <li>Instances are created for any found element with different tags in the XML tree. This instances are stored in the internal {@link #nodes} tree.
   * <li>If elements with the same tag name are found on one level of the XML file, only one instance of this is stored in {@link #nodes}.
   *   It means an instance presents not the content of the XML file but the structure. An instance of this contains the sum of all found attributes.
   * <li>If an element with a known tag is found in a deeper level of the XML file,    
   * </ul> 
   * The information of the built tree of this class can be used to 
   * <ul>
   * <li>Write a config file template, using {@link XmlJzCfgAnalyzer#writeCfgTemplate(File)}
   * </ul>
   * This class builds the root node and any child node too.
   * <pre>
   * XmlCfgNodeBuilder<*>--nodes-->XmlCfgNodeBuilder
   *                                  <*> 
   *                                   |
   *                                   +--attribs--->String
   * </pre>
   * @author Hartmut Schorrig
   *
   */
  static class XmlStructureNode
  {
    /**Tag name of the element. */
    final String tag;
    
    final String tagIdent;
    
    String sSubtreenode;
    
    /**Set if at least one of the occurrences has a text content.*/
    boolean bText = false;
    
    boolean XXXbDetermineWithParent = false;
    
    boolean bDependencyChecked;
    
    
    //boolean bUseSubtree;
    
    /**Set if the node is only one time in all found trees. 
     * It means that a variable is sufficient to store the content of all attributes and the text if no subNodes exists.
     */
    boolean onlySingle = true;
    
    
    final XmlStructureNode parent;
    
    /**Found sub nodes. The list is supplemented if new sub nodes are found on further occurrences of elements. */
    Map<String, XmlStructureNode> nodes;
    
    /**Found attributes. The list is supplemented if new attribute names are found on further occurrences of elements. */
    Map<String, String> attribs;
    
    
    int nrofAttributes = 0;
    
    /**Set if new attributes are found. Then the 
     * 
     */
    boolean bNewAttributes = false; 
    
    
    final XmlStructureData xmlStructData;
    
    XmlStructureNode(XmlStructureNode parent, String tag, XmlStructureData xmlStructData){ 
      this.parent = parent; 
      this.tag = tag;
      this.tagIdent = StringFunctions_B.replaceNonIdentifierChars(tag, 'A').toString();
      
      this.xmlStructData = xmlStructData; 
    }
    
    
    /**Returns an instance to store the occurrence of a XML-element.
     * If more XML-elements with the same tag name are found, only one occurrence of this element type is stored.
     * If an element with the same tag name is given, which is already registered, then the same instance is returned from the found element before.
     * With them maybe new occurring attributes or new occurring elements are stored. 
     * It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public XmlStructureNode addElement(String tag) { 
      if(tag.equals("Document")) {
        Debugutil.stop();
      }
      if(nodes == null) {
        nodes = new IndexMultiTable<String, XmlStructureNode>(IndexMultiTable.providerString);
      }
      XmlStructureNode subNode = nodes.get(tag); //use existent one with same tag to strore further content.
      if(subNode == null) {
        subNode = new XmlStructureNode(this, tag, xmlStructData); 
        nodes.put( tag, subNode);
        xmlStructData.addStructureNodeOccurence(subNode);
      } else {
        subNode.onlySingle = false; //at least twice in this tree.
      }
      return subNode; 
    }
    
    
    /**It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public void setAttribute(String name) { 
      if(attribs == null) { 
        attribs = new TreeMap<String, String>(); 
        bNewAttributes = true;
        attribs.put(name, name);  //the first attrin
      }
      else if(attribs.get(name) ==null) {
        bNewAttributes = true;
        attribs.put(name, name);  //a new attrib
      }
    }

    
    public void setTextOccurrence() { bText = true; }
   
    
    @Override public String toString(){ return tag + (attribs != null ? " attr:" + attribs.toString():"") + (nodes !=null ? " nodes:" + nodes.toString() : ""); }
    
  }

  
  
}
