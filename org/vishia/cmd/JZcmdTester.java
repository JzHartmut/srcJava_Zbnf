package org.vishia.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.DataAccess;
import org.vishia.util.DataShow;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions;
import org.vishia.xmlSimple.SimpleXmlOutputter;

public class JZcmdTester
{


  /**Version, history and license.
   * <ul>
   * <li>2014-06-29 Hartmut chg now all methods are static and part of {@link JZcmdTester}.
   * <li>2013-07-28 Hartmut chg/new: dataTree now writes XML
   * <li>2013-03-10 Hartmut chg/new: dataTree supports superclass content.
   * <li>2012-12-00 Hartmut improved: dataTree circular references with @ 1234 (address) to mark it.
   * <li>2012-10-08 created. dataTree A presentation of the content of a Java data tree was necessary.
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
  static final public String sVersion = "2014-06-29";

  
  
  
  /**Returns an information about the given obj:
   * It returns the "Type=< clazz.getCanonicalType()>; toString=< toStringValue>;"
   * @param obj Any object
   * @return the information string.
   */
  public CharSequence info(Object obj){
    if(obj == null) return "null-reference";
    else {
      //Build an information string about the object:
      StringBuilder u = new StringBuilder();
      Class<?> clazz = obj.getClass();
      u.append("Type=");
      u.append(clazz.getCanonicalName());
      u.append("; toString=").append(obj.toString());
      u.append("; ");
      return u;
    }
  }

  
  /**Same as {@link #info(Object)} as one line
   * @param start text before "Type=..."
   * @param obj Any object
   * @return the information string.
   */
  public CharSequence infoln(CharSequence start, Object obj){
    if(obj == null) return start + ": null-reference";
    else {
      //Build an information string about the object:
      StringBuilder u = new StringBuilder();
      u.append(start);
      Class<?> clazz = obj.getClass();
      u.append("Type=");
      u.append(clazz.getCanonicalName());
      u.append("; toString=").append(obj.toString());
      u.append(";\n");
      return u;
    }    
  }

  
  
  public void mapToFile(Map<String, Object> map, String sFile) throws IOException{
    Writer wr = new FileWriter(sFile); 
      mapToFile(map, wr, 0);
    wr.close();
  }
  
  
  /**Writes the content of a Map into a Appendable for debugging. If the map refers another Map,
   * this routine is invoked recursively with incremented indent.
   * @param map Any map with String key, especially Variables from JZcmd.
   * @param wr Destination
   * @param indent Number of indents. 
   * @throws IOException
   */
  public void mapToFile(Map<String, Object> map, Appendable wr, int indent) throws IOException{
    if(indent < 50) {
      for(Map.Entry<String, Object> entry: map.entrySet()){
        String key = entry.getKey();
        Object value = entry.getValue();
        wr.append(StringFunctions.indent2(indent)).append(key).append("; ").append(value.toString()).append("\n");  
        if(value instanceof Map<?, ?>){
          @SuppressWarnings("unchecked")
          Map<String, Object> submap = (Map<String, Object>) value;  
          mapToFile(submap, wr, indent+1);
        }
      }
    } else {
      wr.append(StringFunctions.indent2(indent)).append("...too many recursions");
    }
  }
  
  
  
  public void debugField(String ident){ DataAccess.debugIdent(ident); }
  
  public void debugMethod(String ident){ DataAccess.debugMethod(ident); }
  
  
  
  
  public static void dataTree(Object data, Appendable out, int maxRecurs) throws IOException 
  { DataShow.dataTree(data, out, maxRecurs); }
  
  public static void dataTreeXml(Object data, Appendable out, int maxRecurs) throws IOException 
  { DataShow.dataTreeXml(data, out, maxRecurs); }
  
  
  /**Generates a html file which contains the description of all data contained in referred in the given data instance.
   * See {@link DataShow#outHtml(Object, Appendable)} - that capability is used. This is only a wrapper method
   * to adapt a given File path in a JZcmd environment. The file is created or overwritten and closed after them.
   * @param data Any instance
   * @param file The description of the file
   * @throws IOException on file system error
   * @throws NoSuchFieldException if the JZcmdFilepath access an internal variable which is not given. It is not expected usual.
   */
  public static void dataHtml(Object data, JZcmdFilepath file) throws IOException, NoSuchFieldException 
  { Writer out = new FileWriter(file.absfile().toString());
    DataShow.outHtml(data, out);
    out.close();
  }
  
  

  
  /**Generates a html file which contains the description of all data contained in referred in the given data instance.
   * See {@link DataShow#outHtml(Object, Appendable)} - that capability is used. This is only a wrapper method
   * to offer this method without effort in usage. The file is created or overwritten and closed after them.
   * Note: a variant with a String given path is not supported because they may dubieties with the current directory.
   * @param data Any instance
   * @param path An usual absolute file path in a File object
   * @throws IOException
   */
  public static void dataHtml(Object data, File path) throws IOException 
  { Writer out = new FileWriter(path);
    DataShow.outHtml(data, out);
    out.close();
  }
  
  

  
  
  
  
  
  /**This method is only intend to set a breakpoint into it.
   * @return
   */
  public int stop(){
    return 0;
  }
  

}
