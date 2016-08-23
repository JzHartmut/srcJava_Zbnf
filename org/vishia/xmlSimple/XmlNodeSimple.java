package org.vishia.xmlSimple;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.TreeNodeBase;


/**This is a simple variant of processing XML.*/

/**Representation of a XML node. It contains a tree of nodes or text content. */ 
public class XmlNodeSimple<UserData> extends TreeNodeBase<XmlNodeSimple<UserData>, UserData, XmlNode> implements XmlNode
{ 
  /**Version, history and license.
   * <ul>
   * <li>2012-11-24 Hartmut chg: The attributes are stored as children in the same tree as other content.
   *   It means that the attributes are visible too if the tree is evaluated as {@link TreeNodeBase} reference.
   * <li>2012-11-24 Hartmut new: {@link #addContent(XmlNode)} checks whether the child is an attribute node.
   *   Then it is added as attribute. Such nodes are created from {@link org.vishia.zbnf.ZbnfParser} now. 
   *   and the {@link XmlNodeSimple#attributes} may be removed.
   * <li>2012-11-03 Hartmut Now this class is derived from TreeNodeBase directly. It is a TreeNode by itself.
   * <li>2012-11-01 Hartmut The {@link TreeNodeBase} is used for the node structure. 
   *   Reference {@link #node}. The algorithm to manage the node structure is deployed in the
   *   TreeNodeBase yet. 
   * <li>2008-04-02: Hartmut some changes
   * <li>2008-01-15: Hartmut www.vishia.org creation
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
   */
  public static final int version = 20121104;


  
  /**The tag name of the node or the text if namespaceKey is "$". 
   * Note: It is not the key in the {@link TreeNodeBase}. The key there is namespaceKey:name. */
  final String name;
  
  /**The namespace-key. If it is "$", the node is a terminate text node. */
  final String namespaceKey;
  
  /**Text of an attribute or text of a text node. */
  String text;
  
  /**List of namespace declaration, typical null because only top elements has it. */
  TreeMap<String, String> namespaces;

  
  /**The parent node. */
  //XmlNode parent;
  
  public XmlNodeSimple(String name)
  { this(name, null, (UserData)null);
  }
  
  public XmlNodeSimple(String name, UserData data)
  { this(name, null, (UserData)null);
  }
  
  protected XmlNodeSimple(String key, String text, boolean isText)
  { super(key, null);
    this.name = key;
    this.text = text;
    this.namespaceKey = key;
  }
  
  public XmlNodeSimple(String name, String namespaceKey, UserData data)
  { super(calcKey(name, namespaceKey), data);
    this.name = name;
    //if(name.startsWith("@"))
      //Assert.stop();
    this.namespaceKey = namespaceKey;
  }
  
  public XmlNodeSimple(String name, String namespaceKey)
  { this(name, namespaceKey, (UserData)null);
  }
  
  public XmlNodeSimple(String name, String namespaceKey, String namespace)
  { this(name, namespaceKey, (UserData)null);
    namespaces.put(namespaceKey, namespace);    
  }
  
  
  private static String calcKey(String name, String namespaceKey){
    String key;  //build the key namespace:tagname or tagname
    if(namespaceKey != null) { key =  namespaceKey + ":" + name; }
    else { key = name; }
    return key;
  }
  
  @Override protected XmlNodeSimple<UserData> newNode(String key, UserData data){
    XmlNodeSimple<UserData> newNode = new XmlNodeSimple<UserData>(key, this.namespaceKey, data);
    return newNode;
  }

  
  public XmlNode createNode(String name, String namespaceKey)
  { return new XmlNodeSimple<UserData>(name,namespaceKey);
  }

  
  
  @SuppressWarnings("unchecked")
  public void setAttribute(String name, String value)
  { String aname = "@" + name;
    XmlNodeSimple<UserData> attribute = (XmlNodeSimple<UserData>)getChild(aname);
    if(attribute ==null){
      attribute = new XmlNodeSimple<UserData>(aname);
      addNode(attribute);
    }
    attribute.text = value;
  }
  
  public void addNamespaceDeclaration(String name, String value)
  { if(namespaces == null){ namespaces = new TreeMap<String, String>(); }
    namespaces.put(name, value);
  }
  
  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addContent(java.lang.String)
   */
  public XmlNode addContent(String text)
  { XmlNodeSimple<UserData> child = new XmlNodeSimple<UserData>("$", text, true);
    addNode(child);
    return this;
  }
  

  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addNewNode(java.lang.String, java.lang.String)
   */
  public XmlNode addNewNode(String name, String namespaceKey) throws XmlException
  { XmlNode node = new XmlNodeSimple<UserData>(name, namespaceKey);
    addContent(node);
    return node;
  }
  
  
  
  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addContent(org.vishia.xmlSimple.XmlNode)
   */
  @SuppressWarnings("unchecked")
  public XmlNode addContent(XmlNode child) 
  throws XmlException 
  { String nameChild = child.getName();
    if(child instanceof XmlNodeSimple<?>){
      XmlNodeSimple<UserData> child1 = (XmlNodeSimple<UserData>) child;
      addNode(child1);
    } else {
      //Because the child node from another implementation type of XmlNodeSimple,
      //a wrapper node should be created.
      //TODO regard all children in tree? - test with XmlNodeJdom.
      String name = child.getName();
      String namespaceKey = child.getNamespaceKey();
      String key = calcKey(name, namespaceKey);
      //TreeNodeBase<XmlNode,XmlNode> childnode = new TreeNodeBase<XmlNode,XmlNode>(key, child);
      XmlNodeSimple<UserData> childnode = new XmlNodeSimple<UserData>(name, key);
      addNode(childnode);
    }
    return this;
  }

  
  
  
  public boolean isTextNode(){ return namespaceKey != null && namespaceKey.equals("$"); }
  
  /**Returns the text of the node. If it isn't a text node, the tagName is returned. */
  public String getText()
  { if(text !=null) 
    { //it is a text node.
      return text;
    }
    else
    { List<XmlNode> textNodes = listChildren("$");
      String sText = "";
      for(XmlNode textNode: textNodes){
        sText += textNode.getText();
      }
      return sText;
    }
  }
  
  /**Returns the text if it is a text node. If it isn't a text node, the tagName is returned. */
  public String getName(){ return name; }
  
  public String getNamespaceKey(){ return namespaceKey; }
  
  public String getAttribute(String name)
  { XmlNodeSimple<UserData> attribute = getNode("@" + name, "/");
    if(attribute !=null){
      assert(attribute.text !=null);
      return attribute.text;
    }
    else return null;
  }

  public Map<String, String> getAttributes()
  {
    Map<String, String> mapAttributes = new TreeMap<String, String>();
    List<XmlNode> attributes = listChildren();
    if(attributes !=null){
      for(XmlNode attrib: attributes){
        String name = attrib.getName();
        if(name.startsWith("@")){
          mapAttributes.put(name.substring(1), attrib.getText());
        }
      }
    }
    return mapAttributes;
  }

  public Map<String, String> getNamespaces()
  {
    return namespaces;
  }

  public String removeAttribute(String name)
  {
    XmlNodeSimple<UserData> attribute = getNode("@" + name, "/");
    if(attribute !=null){
      attribute.detach();
      return attribute.text;
    }
    else return null;
  }



  @Override public String toString()
  { if(namespaceKey !=null && namespaceKey.equals("$")) return name;  //it is the text
    else if(name.startsWith("@")) return name + "=" + text; 
    else return "<" + name + (idxChildren !=null ? ">...</>" : ">"); //any container
  }

  
}


