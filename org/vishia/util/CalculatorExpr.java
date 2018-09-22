package org.vishia.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;



/**This class provides a calculator for expressions. The expression are given 
 * in the reverse polish notation. It can be converted either from a simple string format
 * or from a {@link org.vishia.zbnf.ZbnfJavaOutput} output from a Zbnf parsing process 
 * and then converted to the internal format for a stack oriented running model. 
 * <br><br>
 * This class contains the expression itself and the capability to calculate in a single thread.
 * The calculation can be execute with one or some given values (usual float, integer type)
 * or it can access any Java internal variables using the capability of {@link DataAccess}.
 * <br><br>
 * <ul>
 * <li>Use {@link #setExpr(String)} to convert a String given expression to the internal format.
 * <li>Use {@link #calc(float)} for simple operations with one float input, especially for scaling values. It is fast.
 * <li>Use {@link #calc(Object...)} for universal expression calculation.
 * <li>Use {@link #calcDataAccess(Map, Object...)} to calculate with access to other Java data.
 * <li>Use a ZBNF parser with a proper script and {@link SetExpr} to parse via possibility of Java data access.
 *   That is done in the JZcmd script language. 
 * </ul>
 * If the expression works with simple variable or constant values, it is fast. For example it is able to use
 * in a higher frequently graphic application to scale values.
 * <br><br>
 * <br><br>
 * To test the capability and the correctness use {@link org.vishia.util.test.TestCalculatorExpr}. 
 * @author Hartmut Schorrig
 *
 */
public class CalculatorExpr
{
  
  /**Version, history and license.
   * <ul>
   * <li>2018-08-28 Hartmut new {@link Value#toNumValue()}, conversion especially from String, yet some TODO .
   * <li>2017-10-27 Hartmut exception on cmp operation with {@link #variableNotFoundExpr}
   * <li>2016-02-13 Hartmut bugfix: {@link StringPartScan#scanFractionalNumber(long, boolean)} has had a problem with negative numbers. 
   *   fixed there, adapted here.
   * <li>2015-07-05 Hartmut bugfix: On parenthesis expression there was an error if the {@link ExpressionType} in parenthesis was another 
   *   than the expression type of the stored accumulator. Then the expression type of the parenthesis has taken and the operation
   *   was faulty. Therefore general change: Store the ExpressionType in the accu in {@link Value#etype}. It may be instead {@link Value#type}
   *   but yet both is stored. Set the expression type always in that way that the expression type of the accumulator was taken
   *   and checked against the second operand. Remove the independent variable for the ExpressionType inside {@link #calcDataAccess(Map, Object...)}.   
   * <li>2015-07-04 Hartmut chg, need-feature: To check (float ?instanceof Float) a given boxed float value
   *   should be stored as {@link Value#oVal} too to execute the instanceof operation. The instanceof operator
   *   is enhanced so that a simple type is recognized with ?instanceof BoxedType.  
   * <li>2015-05-04 Hartmut new: {@link Value#type()} and {@link Value#longValue()}
   * <li>2014-12-06 Hartmut bugfix: If works with an "Num" Object in JZcmd resp if it gets an Object which is a {@link Value}
   *   the Object is used correct now. 
   * <li>2014-12-06 Hartmut chg: {@link #calc(float)} now uses the {@link #calcDataAccess(Map, Object...)} which is complete.
   *   Some enhancements and tunings. TODO same for {@link #calc(double)} etc.
   * <li>2014-12-06 Hartmut chg: correct check of object values whether it is a numeric type.
   * <li>2014-12-06 Hartmut chg: On DataAccess and FieldNotFoundException it is false if a boolean value is expected.
   * <li>2014-12-06 Hartmut chg: Handling of AND and OR operation: A special operation {@link Operators#boolCheckAndOperation}
   *   or {@link Operators#boolCheckOrOperation} is necessary to finish the evaluation on AND or OR operation if the result is false or true already.
   *   The older solution: It checks whether the next operation is an AND or OR. But that does not recognize  a boolean operation in parenthesis which may follow.
   *   because the AND or OR follows after the calculation of the parenthesis content only.
   * <li>2014-11-15 Hartmut new: instanceof as compare operator.   
   * <li>2014-08-10 Hartmut bugfix: on push to stack operation the type of the new level should start with {@value #startExpr} always
   *   because the new level is a new start type. The operation with the pushed value is done later, the adjusting of types should be done later too! 
   * <li>2014-02-22 Hartmut chg: now string >= string2 tests 'startswith' instead alphabetic comparison. 
   * <li>2014-02-22 Hartmut new: {@link #calcDataAccess(Map, Object...)} accepts {@link Value} as return of dataAccess.
   *   A Value is stored for Num variables in JZcmd especially.
   * <li>2014-01-26 Hartmut bugfix: and-expression with !val && val did not work.  
   * <li>2014-01-26 Hartmut chg: To add a datapath now {@link SetExpr#new_datapath()} is offered, all details of a datapath
   *   are handled in {@link DataAccess.DataAccessSet}. To support more complex {@link DataAccess.DatapathElement} especially
   *   with expressions or datapath as arguments, the method {@link SetExpr#newDataAccessSet()} supports overriding
   *   in a derived class of {@link SetExpr}, see {@link org.vishia.cmd.JZtxtcmdScript.JZcmdCalculatorExpr}. 
   * <li>2013-10-19 Hartmut new: {@link SetExpr} should know all possibilities of {@link DataAccess.DataAccessSet}
   *   too because an expression may be an DataAccess only. Yet only {@link SetExpr#new_newJavaClass()} realized.
   * <li>2013-10-19 Hartmut new: The CalculatorExpr gets the capability to generate String expressions
   *   using the {@link JZtxtcmdExecuter} class. This is because some arguments of methods may be a String.
   *   If the {@link #genString} is set, the CalculatorExpr is a String expression.
   *   Now this class, the {@link DataAccess} and the {@link JZtxtcmdExecuter} are one cluster of functionality.
   * <li>2013-09-02 Hartmut new: {@link CalculatorExpr.SetExpr} to set from a ZbnfParseResult using {@link org.vishia.zbnf.ZbnfJavaOutput}.
   *   This class can be invoked without ZbnfParser too, it is independent of it. But it isn't practicable. 
   * <li>2013-09-02 Hartmut new: CalculatorExpr: now supports unary ( expression in parenthesis ). 
   * <li>2013-08-18 Hartmut new: {@link Operation#unaryOperator}
   * <li>2013-08-19 Hartmut chg: The {@link DataAccess.DatapathElement} is a attribute of a {@link Operation}, not of a {@link Value}.
   *   A value is only a container for constant values or results.
   * <li>2012-12-22 Hartmut new: Now a value can contain a list of {@link DataAccess.DatapathElement} to access inside java data 
   *   to evaluate the value. The concept is synchronized with {@link org.vishia.zbatch.ZbatchGenScript}, 
   *   but not depending on it. The JbatGenScript uses this class, this class participates on the development
   *   and requirements of jbat.
   * <li>Bugfix because String value and thrown Exception. The class needs a test environment. TODO
   * <li>2012-12-22 some enhancements while using in {@link org.vishia.ZbatchExecuter.TextGenerator}.
   * <li>2012-04-17 new {@link #calc(float)} for float and int
   * <li>TODO unary operator, function 
   * <li>2011-10-15 Hartmut creation. The ideas were created in 1979..80 by me.  
   * </ul>
   *
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
  //@SuppressWarnings("hiding")
  public final static int version = 0x20141214;
  
   
   
  
   
  /**A value, maybe a constant, any given Object or an access description to a java program element.
   * 
   *
   */
  public static class Value { //extends Datapath{
    
    
    
    /**Type of the value. 
     * <ul>
     * <li>J I D F Z: long, int, double, float, boolean, the known Java characters for types see {@link java.lang.Class#getName()}
     * <li>C: Character value stored in the intValue.
     * <li>o: The oVal contains any object.
     * <li>t: A character sequence stored in stringVal,
     * <li>a: An Appendable,
     * <li>e: Contains an exception for not found datapath. The exception is ignored if the value is converted to a boolean value, then the boolean value is false.
     * <li>?: The value is empty, not defined.
     * </ul>
     */
    protected char type = '?';
    
    /**The expression type to check the second operand. The {@link #type()} is identically with {@link ExpressionType#typeChar()}.
     * It need not checked outside of the internally functionality of the outer class CalculatorExpr.
     */
    ExpressionType etype = startExpr;
    
    protected long longVal;
    protected int intVal;
    protected double doubleVal;
    protected float floatVal;
    protected boolean boolVal;
    protected CharSequence stringVal;
    protected Object oVal;
    
    public Value(long val){ type = 'J'; etype = longExpr; longVal = val; }
    
    public Value(int val){ type = 'I'; etype = intExpr; intVal = val; }
    
    public Value(double val){ type = 'D'; etype = doubleExpr; doubleVal = val; }
    
    public Value(float val){ type = 'F'; etype = floatExpr; floatVal = val; }
    
    public Value(boolean val){ type = 'Z'; etype = booleanExpr; boolVal = val; }
    
    public Value(char val){ type = 'C'; etype = intExpr; longVal = intVal = val; }
    
    public Value(String val){ type = 't'; etype = stringExpr; oVal = stringVal = val; }
    
    public Value(Appendable val){ type = 'a'; etype = objExpr; oVal = val; }
    
    public Value(Object val){ type = 'o'; etype = objExpr; oVal = val; }
    
    //public Value(List<DataPathItem> datpath){ type = 'd'; this.datapath = datapath; }
    
    public Value(){ type = '?'; etype = startExpr; }
    
    
    
    public char type(){ return type; }
    
    /**Copy all content from src to this.
     * @param src
     */
    public void copy(Value src){
      type = src.type;
      etype = src.etype;
      longVal = src.longVal;
      intVal = src.intVal;
      doubleVal = src.doubleVal;
      floatVal = src.floatVal;
      boolVal = src.boolVal;
      stringVal = src.stringVal;
      oVal = src.oVal;
    }
    
    /**Returns a boolean value. If the type of content is a numeric, false is returned if the value is ==0.
     * If the type is a text, false is returned if the string is empty.
     * If the type is any other Object, false is returned if the object referenz is ==null.
     * @return The boolean value.
     */
    public boolean booleanValue()
    { switch(type){
        case 'I': return intVal !=0;
        case 'J': return longVal !=0;
        case 'D': return doubleVal !=0;
        case 'F': return floatVal !=0;
        case 'C': return intVal !=0;
        case 'Z': return boolVal;
        case 't': return stringVal !=null && stringVal.length() >0;
        case 'o': 
          if(oVal instanceof Long)          { return ((Long)oVal).longValue() !=0;  }
          else if(oVal instanceof Integer)  { return ((Integer)oVal).intValue() !=0; }
          else if(oVal instanceof Short)    { return ((Short)oVal).shortValue() !=0; }
          else if(oVal instanceof Byte)     { return ((Byte)oVal).byteValue() !=0;  }
          else if(oVal instanceof Double)   { return ((Double)oVal).doubleValue() !=0; }
          else if(oVal instanceof Float)    { return ((Float)oVal).floatValue() !=0; }
          else if(oVal instanceof Boolean)  { return ((Boolean)oVal).booleanValue(); }
          else if(oVal instanceof Character){ return ((Character)oVal).charValue() !=0; }
          else { return oVal !=null; }
        case 'e': return false;
        case '?': return false; //throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public int intValue()
    { switch(type){
        case 'I': return intVal;
        case 'C': return intVal;
        case 'J': return (int)longVal;
        case 'D': return (int)doubleVal;
        case 'F': return (int)floatVal;
        case 'Z': return boolVal ? 1 : 0;
        case 't': return Integer.parseInt(stringVal.toString());
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': return 7777777; //TODO throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public long longValue()
    { switch(type){
        case 'I': return intVal;
        case 'C': return intVal;
        case 'J': return longVal;
        case 'D': return (long)doubleVal;
        case 'F': return (long)floatVal;
        case 'Z': return boolVal ? 1 : 0;
        case 't': return Long.parseLong(stringVal.toString());
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': return 777777777777L; //TODO throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public double doubleValue()
    { switch(type){
        case 'F': return floatVal;
        case 'I': return intVal;
        case 'C': return intVal;
        case 'J': return longVal;
        case 'D': return doubleVal;
        case 'Z': return boolVal ? 1.0 : 0;
        case 't': return Double.parseDouble(stringVal.toString());
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': return 7777777.0; //TODO throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public float floatValue()
    { switch(type){
        case 'I': return intVal;
        case 'C': return intVal;
        case 'J': return longVal;
        case 'D': return (float)doubleVal;
        case 'F': return floatVal;
        case 'Z': return boolVal ? 1.0f: 0;
        case 't': return Float.parseFloat(stringVal.toString());
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': return 77777.0f; //TODO throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    /**Returns the reference to the StringBuilder-buffer if the result is a concatenation of strings.
     * The StringBuilder-buffer can be changed after them in any public application, 
     * because the Value is only returned on end of calculation. 
     * Returns a reference to String in all other cases.
     * 
     * @return
     */
    public CharSequence stringValue(){ 
      switch(type){
        case 'I': return StringSeq.create(Integer.toString(intVal));
        case 'J': return StringSeq.create(Long.toString(longVal));
        case 'D': return StringSeq.create(Double.toString(doubleVal));
        case 'F': return StringSeq.create(Float.toString(floatVal));
        case 'C': return (new StringBuilder(1)).append((char)intVal);
        case 'Z': return StringSeq.create(Boolean.toString(boolVal));
        case 't': return stringVal;
        case 'o': return StringSeq.create(oVal ==null ? "null" : oVal.toString());
        case 'e': return StringSeq.create(oVal.toString());
        case '?': return StringSeq.create("??");
        default:  return StringSeq.create("?" + type);
      }//switch
    }

    /**Converts the Value to the adequate Object representation, especially for the wrapped
     * primitive types. If the value contains an int, char, long, double, float, boolean,
     * it is converted to the wrapped presentation of that. If the value contains an text, represented 
     * by a reference to {@link java.lang.CharSequence}, this instance is returned. It may be a 
     * {@link java.lang.String} or a {@link java.lang.StringBuilder}.
     * If the value contains a reference to an object because the expression consists of only one
     * argument, a {@link Operation#datapath} of an {@link DataAccess}, this Object is returned.
     * 
     * @return an Object which presents the value.
     */
    public Object objValue(){ 
      switch(type){
        case 'I': return new Integer(intVal);
        case 'C': return new Character((char)intVal);
        case 'J': return new Long(longVal);
        case 'D': return new Double(doubleVal);
        case 'F': return new Float(doubleVal);
        case 'Z': return new Boolean(boolVal);
        case 't': return stringVal;
        case 'e': case 'o': return oVal;
        default:  return "?" + type;
      }//switch
    }


    
    /**Returns true if the value stores an Object value.
     * Especially if the expression does contain only from a single dataAccess, the value
     * is an Object value.
     * @return false on stringValue or such. Tests {@link #type} == 'o'.
     */
    public boolean isObjValue(){ return type == 'o'; }
    
    
    public String stringxValue() {
      switch(type){
        case 'I': return Integer.toString(intVal);
        case 'J': return Long.toString(longVal);
        case 'D': return Double.toString(doubleVal);
        case 'F': return Float.toString(floatVal);
        case 'C': return "" + (char)intVal;
        case 'Z': return Boolean.toString(boolVal);
        case 't': return stringVal.toString();
        case 'o': return oVal ==null ? "null" : oVal.toString();
        case 'e': return ((Exception)oVal).getMessage();
        case '?': return "??";
        default:  return "?" + type;
      }//switch
      
    }
    
    
    /**Converts the content of the variable to a numeric one.
     * Detects hex with "0x", Integer, Long, Float, Double. The decimal separator should be a '.'.
     * Spaces are ignored, can be separators between digits. A separator ' or , is possible too.
     */
    public void toNumValue() {
      if( "JISBFD".indexOf(type) <0) {
        String sValue = stringVal !=null ? stringVal.toString() : oVal !=null ? oVal.toString() : "0";
        int zValue = sValue.length();
        int[] parsedCharsA = new int[1];
        long value = StringFunctions_C.parseIntRadix(sValue, 0, 999999, 10, parsedCharsA, " ,'");
        int parsedChars = parsedCharsA[0];
        if(parsedChars < zValue) {
          if(sValue.charAt(parsedChars) =='.') { //a float number
            long valueFrac = StringFunctions_C.parseIntRadix(sValue.substring(parsedChars+1), 0, 999999, 10, parsedCharsA, " ,'");
            if(parsedChars + parsedCharsA[0] > 7) {
              //TODO double
            } else {
              //TODO double
                
            }
          }
          //TODO regard kMGTmu�np
          //TODO let the rest of String in stringVal, it may be the unit.
        } 
        //if more character are present, ignore it.
        if("DF".indexOf(type) <0) { //not a float or double
          if(zValue <=9 ) { intVal = (int)value; type = 'I'; }
          else { longVal = value; type = 'L'; }
        }
      }
    
    }
    
    
    
    @Override public String toString(){ 
      switch(type){
        case 'I': return "I " + Integer.toString(intVal);
        case 'J': return "J " + Long.toString(longVal);
        case 'D': return "D " + Double.toString(doubleVal);
        case 'F': return "F " + Float.toString(floatVal);
        case 'C': return "C " + (char)intVal;
        case 'Z': return "Z " + Boolean.toString(boolVal);
        case 't': return "t " + stringVal.toString();
        case 'o': return "o " + oVal ==null ? "null" : oVal.toString();
        case 'e': return "e " + ((Exception)oVal).getMessage();
        case '?': return "??";
        default:  return "?" + type;
      }//switch
    }
  }
  
  
  
  /**Common interface to check and maybe change the type of expression.
   * All instances of Expression type implements this interface to check and maybe change the arguments adequate to the given or higher type.
   */
  protected interface ExpressionType {
    abstract char typeChar();
    
    /**Checks the second argument whether it is matching to the current expression type 
     * which matches to the accu.
     * If the val2 provides another type, either it is converted to the current expression type
     * or another (higher) expression type is taken and the accumulator value is converted.
     * @param accu The accumulator maybe changed..
     * @param val2 the second operand is tested, may be changed.
     * @return type of the expression. Often it is this, sometimes it is an higher type.
     */
    abstract ExpressionType checkArgument(Value accu, Value val2) throws Exception;
  }
  
  
  
  /**The type of val2 determines the expression type. The accu is not used because the first operation is a set operation always. 
   * The accu will be set with the following operation.
   */
  protected static final ExpressionType startExpr = new ExpressionType(){
    
    @Override public char typeChar() { return '!'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      accu.type = val2.type;
      switch(val2.type){
        case 'I': accu.etype = intExpr; break;
        case 'J': accu.etype = longExpr; break; 
        case 'F': accu.etype = floatExpr; break; 
        case 'D': accu.etype = doubleExpr; break; 
        case 'C': accu.etype = intExpr; break; 
        case 'Z': accu.etype = booleanExpr; break; 
        case 't': accu.etype = stringExpr; break; 
        case 'o': accu.etype = objExpr; break;   //first operand is any object type. 
        case 'e': accu.etype = variableNotFoundExpr; break;   //first operand is any object type. 
        default: throw new IllegalArgumentException("src type");
      } //switch  
      return accu.etype;
    }

    @Override public String toString(){ return "Type=!"; }
  };
  
  /**The current expression type is int. 
   * Changes the expression type to a higher type and convert the operands
   * if one of the operand have an abbreviating type.
   */
  protected static final ExpressionType intExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'I'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) throws Exception {
      switch(val2.type){
        case 'C': case 'I': return this; 
        case 'J': accu.longVal = accu.intVal; accu.type = 'J'; return accu.etype = longExpr; 
        case 'F': accu.floatVal = accu.intVal; accu.type = 'F'; return accu.etype = floatExpr; 
        case 'D': accu.doubleVal = accu.intVal; accu.type = 'D'; return accu.etype = doubleExpr; 
        case 'Z': accu.boolVal = accu.intVal !=0; accu.type = 'Z'; return accu.etype = booleanExpr; 
        case 'o': 
          if(val2.oVal instanceof Long)          { val2.longVal = ((Long)val2.oVal).longValue();       accu.longVal = accu.intVal; return longExpr; }
          else if(val2.oVal instanceof Integer)  { val2.intVal = ((Integer)val2.oVal).intValue();      return this; }
          else if(val2.oVal instanceof Short)    { val2.intVal = ((Short)val2.oVal).shortValue();      return this; }
          else if(val2.oVal instanceof Byte)     { val2.intVal = ((Byte)val2.oVal).byteValue();        return this; }
          else if(val2.oVal instanceof Double)   { val2.doubleVal = ((Double)val2.oVal).doubleValue(); accu.doubleVal = accu.intVal; return doubleExpr; }
          else if(val2.oVal instanceof Float)    { val2.floatVal = ((Float)val2.oVal).floatValue();    accu.floatVal = accu.intVal; return floatExpr; }
          else if(val2.oVal instanceof Boolean)  { val2.boolVal = ((Boolean)val2.oVal).booleanValue(); accu.boolVal = accu.intVal !=0; return booleanExpr; }
          else if(val2.oVal instanceof Character){ val2.intVal = ((Character)val2.oVal).charValue();   return this; }
          else throw new IllegalArgumentException("the value should be of a numeric type");
        case 'e': throw (Exception)val2.oVal;  //the oVal refers an exception instance.
        case 't': {
          try{ val2.longVal = Long.parseLong(val2.stringVal.toString());
            return this; 
          } catch(Exception exc){ throw new IllegalArgumentException("CalculatorExpr - String converion error"); }
        }
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }

    @Override public String toString(){ return "Type=I"; }


  };

  /**The current expression type is long. 
   * Changes the expression type to a higher type and convert the operands
   * if one of the operand have an abbreviating type.
   */
  protected static final ExpressionType longExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'J'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) throws Exception {
      switch(val2.type){
        case 'C': case 'I': val2.longVal = val2.intVal; return this; 
        case 'J': return this; 
        case 'F': accu.floatVal = accu.longVal; accu.type = 'F'; return accu.etype = floatExpr; 
        case 'D': accu.doubleVal = accu.longVal; accu.type = 'D'; return accu.etype = doubleExpr; 
        case 'o': 
          if(val2.oVal instanceof Long)          { val2.longVal = ((Long)val2.oVal).longValue();       return this; }
          else if(val2.oVal instanceof Integer)  { val2.longVal = ((Integer)val2.oVal).intValue();     return this; }
          else if(val2.oVal instanceof Short)    { val2.longVal = ((Short)val2.oVal).shortValue();     return this; }
          else if(val2.oVal instanceof Byte)     { val2.longVal = ((Byte)val2.oVal).byteValue();       return this; }
          else if(val2.oVal instanceof Double)   { val2.doubleVal = ((Double)val2.oVal).doubleValue(); accu.doubleVal = accu.longVal; return doubleExpr; }
          else if(val2.oVal instanceof Float)    { val2.floatVal = ((Float)val2.oVal).floatValue();    accu.floatVal = accu.longVal; return floatExpr; }
          else if(val2.oVal instanceof Boolean)  { val2.boolVal = ((Boolean)val2.oVal).booleanValue(); accu.boolVal = accu.longVal !=0; return booleanExpr; }
          else if(val2.oVal instanceof Character){ val2.intVal = ((Character)val2.oVal).charValue();   return this; }
          else throw new IllegalArgumentException("the value should be of a numeric type");
        case 'e': throw (Exception)val2.oVal;
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=J"; }


  };
  
  
  /**The current expression type is float. 
   * Changes the expression type to a higher type and convert the operands
   * if one of the operand have an abbreviating type.
   */
  protected static final ExpressionType floatExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'F'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) throws Exception {
      switch(val2.type){
        case 'C': case 'I': val2.floatVal = val2.intVal; return this; 
        case 'J': val2.doubleVal = val2.longVal; return doubleExpr; 
        case 'F': return this; 
        case 'D': accu.doubleVal = accu.floatVal; accu.type = 'D'; return doubleExpr; 
        case 'o': 
          if(val2.oVal instanceof Long)          { val2.floatVal = ((Long)val2.oVal).longValue();      return this; }
          else if(val2.oVal instanceof Integer)  { val2.floatVal = ((Integer)val2.oVal).intValue();    return this; }
          else if(val2.oVal instanceof Short)    { val2.floatVal = ((Short)val2.oVal).shortValue();    return this; }
          else if(val2.oVal instanceof Byte)     { val2.floatVal = ((Byte)val2.oVal).byteValue();      return this; }
          else if(val2.oVal instanceof Double)   { val2.doubleVal = ((Double)val2.oVal).doubleValue(); accu.doubleVal = accu.floatVal; return doubleExpr; }
          else if(val2.oVal instanceof Float)    { val2.floatVal = ((Float)val2.oVal).floatValue();    return this; }
          else if(val2.oVal instanceof Boolean)  { val2.boolVal = ((Boolean)val2.oVal).booleanValue(); accu.boolVal = accu.floatVal !=0; return booleanExpr; }
          else if(val2.oVal instanceof Character){ val2.floatVal = ((Character)val2.oVal).charValue(); return this; }
          else                                   { return objExpr; } 
          //else throw new IllegalArgumentException("the value should be of a numeric type");
        case 'e': throw (Exception)val2.oVal;
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=F"; }


  };
  
  
  /**The current expression type is double. 
   * Convert the operands
   * if one of the operand have an abbreviating type.
   */
  protected static final ExpressionType doubleExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'D'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) throws Exception {
      switch(val2.type){
        case 'C': case 'I': val2.doubleVal = val2.intVal; return this; 
        case 'J': val2.doubleVal = val2.longVal; return this; 
        case 'F': val2.doubleVal = val2.floatVal; return this; 
        case 'D': return this; 
        case 'o': 
          if(val2.oVal instanceof Long)          { val2.doubleVal = ((Long)val2.oVal).longValue();      return this; }
          else if(val2.oVal instanceof Integer)  { val2.doubleVal = ((Integer)val2.oVal).intValue();    return this; }
          else if(val2.oVal instanceof Short)    { val2.doubleVal = ((Short)val2.oVal).shortValue();    return this; }
          else if(val2.oVal instanceof Byte)     { val2.doubleVal = ((Byte)val2.oVal).byteValue();      return this; }
          else if(val2.oVal instanceof Double)   { val2.doubleVal = ((Double)val2.oVal).doubleValue();  return this; }
          else if(val2.oVal instanceof Float)    { val2.doubleVal = ((Float)val2.oVal).floatValue();    return this; }
          else if(val2.oVal instanceof Boolean)  { val2.boolVal = ((Boolean)val2.oVal).booleanValue();  accu.boolVal = accu.doubleVal !=0; return booleanExpr; }
          else if(val2.oVal instanceof Character){ val2.doubleVal = ((Character)val2.oVal).charValue(); return this; }
          else throw new IllegalArgumentException("the value should be of a numeric type");
        case 'e': throw (Exception)val2.oVal;
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=D"; }


  };
  
  
  
  /**The current expression type is boolean. 
   * Convert the operands to boolean
   * if one of the operand have an abbreviating type.
   */
  protected static final ExpressionType booleanExpr = new ExpressionType(){

    @Override public char typeChar() { return 'Z'; }
    
    /**Converts the value of val2 to boolean because a booleanExpr is required. 
     * @see org.vishia.util.CalculatorExpr.ExpressionType#checkArgument(org.vishia.util.CalculatorExpr.Value, org.vishia.util.CalculatorExpr.Value)
     */
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'C': 
        case 'I': val2.boolVal = val2.intVal !=0; break;
        case 'J': val2.boolVal = val2.longVal != 0; break;
        case 'F': val2.boolVal = val2.floatVal !=0; break;
        case 'D': val2.boolVal = val2.doubleVal !=0; break;
        case 't': val2.boolVal = val2.stringVal !=null && val2.stringVal.length() >0; break;
        case 'o':           
          if(val2.oVal instanceof Long)          { val2.boolVal = ((Long)val2.oVal).longValue() !=0;  }
          else if(val2.oVal instanceof Integer)  { val2.boolVal = ((Integer)val2.oVal).intValue() !=0; }
          else if(val2.oVal instanceof Short)    { val2.boolVal = ((Short)val2.oVal).shortValue() !=0; }
          else if(val2.oVal instanceof Byte)     { val2.boolVal = ((Byte)val2.oVal).byteValue() !=0;  }
          else if(val2.oVal instanceof Double)   { val2.boolVal = ((Double)val2.oVal).doubleValue() !=0; }
          else if(val2.oVal instanceof Float)    { val2.boolVal = ((Float)val2.oVal).floatValue() !=0; }
          else if(val2.oVal instanceof Boolean)  { val2.boolVal = ((Boolean)val2.oVal).booleanValue(); }
          else if(val2.oVal instanceof Character){ val2.boolVal = ((Character)val2.oVal).charValue() !=0; }
          else { val2.boolVal = val2.oVal !=null; }
          return this;
        case 'e': val2.boolVal = false; return this;
        case 'Z': return this; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
      return this;
    }
    @Override public String toString(){ return "Type=Z"; }
  };
  
  
  /**Converts the value of val2 to boolean because a booleanExpr is required. 
   */
  protected static final ExpressionType finishBooleanExpr = new ExpressionType(){

    @Override public char typeChar() { return 'z'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      //Note: the value is stored in the accu already.
      return booleanExpr;
    }
    @Override public String toString(){ return "Type=z"; }
  };
  
  
  /**The current expression type is String. 
   * Convert the operands {@link Object#toString()}
   * if one of the operand have an abbreviating type.
   */
  protected static final ExpressionType stringExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 't'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      if(accu.type !='t'){
        //especially any object.
        accu.stringVal = accu.stringValue();
        accu.type = 't';
      }
      if(val2.type !='t'){ 
        val2.stringVal = val2.stringValue();
      }
      return this;
    }

    @Override public String toString(){ return "Type=t"; }


  };
  

  /**Only used for the first and only one operand.
   * 
   */
  protected static final ExpressionType objExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'o'; }
    
    /**If the accu is clear, sets the accu from val2 and returns the type of val2, elsewhere converts both operands to strings and returns stringExpression.
     * First one is on set operation, the second for string concatenation.
     * @see org.vishia.util.CalculatorExpr.ExpressionType#checkArgument(org.vishia.util.CalculatorExpr.Value, org.vishia.util.CalculatorExpr.Value)
     */
    @Override public ExpressionType checkArgument(Value accu, Value val2) throws Exception {
      if(accu.type == '?'){
        //assert(false);   //should never true because if accu.type=='?' the type of expression should be set to startExpr
        accu.type = val2.type;
        switch(val2.type){
          case 'Z': return accu.etype = booleanExpr;
          case 'D': return accu.etype = doubleExpr;
          case 'F': return accu.etype = floatExpr;
          case 'I': return accu.etype = intExpr;
          case 'J': return accu.etype = longExpr;
          case 'o': return accu.etype = objExpr;
          case 'e': return accu.etype = objExpr;  //may test it.
          case 't': return accu.etype = stringExpr; 
          case '?': throw new IllegalArgumentException("the type is not determined while operation.");
          default: throw new IllegalArgumentException("unknown type char: " + val2.type);
        }//switch
      }
      else {
        if(accu.type !='t'){
          //especially any object.
          accu.stringVal = accu.stringValue();
          accu.type = 't';
        }
        if(val2.type !='t'){ 
          val2.stringVal = val2.stringValue();
        }
        return accu.etype = stringExpr;
      }
    }

    @Override public String toString(){ return "Type=o"; }


  };
  


  /**Only used for the first and only one operand.
   * 
   */
  protected static final ExpressionType variableNotFoundExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'e'; }
    
    /**If the accu is clear, sets the accu from val2 and returns the type of val2, elsewhere converts both operands to strings and returns stringExpression.
     * First one is on set operation, the second for string concatenation.
     * @see org.vishia.util.CalculatorExpr.ExpressionType#checkArgument(org.vishia.util.CalculatorExpr.Value, org.vishia.util.CalculatorExpr.Value)
     */
    @Override public ExpressionType checkArgument(Value accu, Value val2) throws Exception {
      if(accu.type == '?'){
        accu.type = 'e';
      }
      return accu.etype = this;
    }

    @Override public String toString(){ return "Type=e"; }


  };
  


  
  
  
  
  
  
  
  
  /**An operator for the current value (accu), the given second value maybe the operand from the {@link Operation},
   * or for some additional values in stack.
   * The operation are such as a set operation (set the arg to accu), add, subtract, negate, boolean operators etc or standard maths routines.
   * Instances of this class are defined in the {@link CalculatorExpr}.
   */
  public abstract static class Operator {
    protected final String name; 
    protected Operator(String name){ this.name = name; }
    
    /**Operates and returns the result type. This method is used only internally, it is private, only protected to show it in the documentation.
     * Before invoking the operation, both arguments are converted in a proper format for the operation with the method
     * {@link ExpressionType#checkArgument(Value, Value)} with respect to the current type of the expression
     * starting with the {@link CalculatorExpr#startExpr}.
     * @param Type The current type
     * @param accu First operand, used cumulatively or the only one operand for unary operations. 
     * @param arg The second argument for binary operations.
     * @return The result type after the operation.
     * @throws Exception If the operation is not admissible.
     */
    protected abstract void operate(Value accu, Value arg) throws Exception;
    
    /**Returns true on an unary operation, false on a operation with both operands. */
    protected abstract boolean isUnary();


    /**Returns true if this is a bool check operation. */
    protected abstract boolean isBoolCheck();




    @Override public String toString(){ return name; }
  }
  
                                                  
  
  /**This class contains all Operators as static references.
   * The operations are protected only to show it as documentation. They are used private only.
   * To use this operators: The {@link CalculatorExpr#setExpr(String)} converts a string given expression in the internal form
   * which uses this operations, respectively the SetExpr converts an expression from ZBNF
   * TODO Idea-typedOperator: this class may be an interface instead, which is implemented for the known 
   * {@link ExpressionType}. Then the switch-case can be saved in calculation time.
   */
  protected static class Operators
  {
  
  /**Converts the accu (val) to a boolean. Does not use val2. */
  protected static final Operator boolOperation = new Operator("bool "){
    @Override public void operate(Value val, Value val2) {
      val.boolVal = val.booleanValue();
      val.type = 'Z';
      val.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return true; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Converts the accu (val) to a boolean and negates it. Does not use val2. */
  protected static final Operator boolNotOperation = new Operator("b!"){
    @Override public void operate(Value val, Value value2) {
      val.boolVal = !val.booleanValue();
      val.type = 'Z';
      val.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return true; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Negates all bits from a given integer type value or negates a boolean in the accu. 
   * throws an {@link IllegalArgumentException} "unknown type" if it is a float type vale, String or Object value.
   * Unary operation, does not use value2. */
  protected static final Operator bitNotOperation = new Operator("~u") 
  {
    @Override public void operate(Value accu, Value value2) throws Exception {
      switch(accu.etype.typeChar()){
        case 'B': case 'S': 
        case 'C': case 'I': accu.intVal = ~accu.intVal; break;
        case 'J': accu.longVal = ~accu.longVal; break;
        //case 'D': accu.doubleVal = accu.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        case 'e': throw (Exception)accu.oVal; 
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return true; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Negates the accu, unary operation.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator negOperation = new Operator("-u"){
    @Override public void operate(Value accu, Value val2) throws Exception{
      switch(accu.etype.typeChar()){
        case 'B': case 'S': 
        case 'C': case 'I': accu.intVal = -accu.intVal; break;
        case 'J': accu.longVal = -accu.longVal; break;
        case 'D': accu.doubleVal = -accu.doubleVal; break;
        case 'F': accu.floatVal = -accu.floatVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        case 'e': throw (Exception)accu.oVal;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return true; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Adds the arg to the accu. Ors boolean values.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator addOperation = new Operator("+"){
    @Override public void operate(Value accu, Value arg) throws Exception{
      switch(accu.etype.typeChar()){
        case 'I': case 'S': case 'B': accu.intVal += arg.intVal; break;
        case 'J': accu.longVal += arg.longVal; break;
        case 'D': accu.doubleVal += arg.doubleVal; break;
        case 'F': accu.floatVal += arg.floatVal; break;
        case 'Z': accu.boolVal |= arg.boolVal; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Subtracts the arg from the accu.
   * throws an {@link IllegalArgumentException} "unknown Type" on Boolean, String or Object types.
   */
  protected static final Operator subOperation = new Operator("-"){
    @Override public void operate(Value accu, Value arg) throws Exception{
      switch(accu.etype.typeChar()){
        case 'I': accu.intVal -= arg.intVal; break;
        case 'J': accu.longVal -= arg.longVal; break;
        case 'D': accu.doubleVal -= arg.doubleVal; break;
        case 'F': accu.floatVal -= arg.floatVal; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Multiplies the arg with the accu, Ands a Boolean.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator mulOperation = new Operator("*"){
    @Override public void operate(Value accu, Value arg) throws Exception{
      switch(accu.etype.typeChar()){
        case 'I': accu.intVal *= arg.intVal; break;
        case 'J': accu.longVal *= arg.longVal; break;
        case 'D': accu.doubleVal *= arg.doubleVal; break;
        case 'F': accu.floatVal *= arg.floatVal; break;
        case 'Z': accu.boolVal &= arg.boolVal; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Divides the accu with the arg .
   * throws an {@link IllegalArgumentException} "unknown Type" on Boolean, String or Object types.
   */
  protected static final Operator divOperation = new Operator("/"){
    @Override public void operate(Value accu, Value arg) throws Exception{
      switch(accu.etype.typeChar()){
        case 'I': accu.intVal /= arg.intVal; break;
        case 'J': accu.longVal /= arg.longVal; break;
        case 'D': accu.doubleVal /= arg.doubleVal; break;
        case 'F': accu.floatVal /= arg.floatVal; break;
        case 'Z': accu.doubleVal /= arg.doubleVal; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Compares the arg with the accu. Set the accu to true if accu == arg2 before the operation. Returns a {@link CalculatorExpr#booleanExpr}.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator cmpEqOperation = new Operator(".cmp."){
    @Override public void operate(Value accu, Value arg) throws Exception {
      switch(accu.etype.typeChar()){
        case 'I': accu.boolVal = accu.intVal == arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal == arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) < (Math.abs(accu.doubleVal) / 100000); break;
        case 'F': accu.boolVal = Math.abs(accu.floatVal - arg.floatVal) < (Math.abs(accu.floatVal) / 100000.0f); break;
        case 'Z': accu.boolVal = accu.boolVal == arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.equals(accu.stringVal, arg.stringVal); break;
        case 'o': accu.boolVal = accu.oVal == null && arg.oVal == null || (accu.oVal !=null && arg.oVal !=null && accu.oVal.equals(arg.oVal)); break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Compares the arg with the accu. Set the accu to true if accu != arg2 before the operation. Returns a {@link CalculatorExpr#booleanExpr}.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator cmpNeOperation = new Operator("!="){
    @Override public void operate(Value accu, Value arg) throws Exception{
      switch(accu.etype.typeChar()){
        case 'I': accu.boolVal = accu.intVal != arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal != arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) >= (Math.abs(accu.doubleVal) / 100000); break;
        case 'F': accu.boolVal = Math.abs(accu.floatVal - arg.floatVal) >= (Math.abs(accu.floatVal) / 100000.0f); break;
        case 'Z': accu.boolVal = accu.boolVal != arg.boolVal; break;
        case 't': accu.boolVal = !StringFunctions.equals(accu.stringVal, arg.stringVal); break;
        case 'o': accu.boolVal = !(accu.oVal == null && arg.oVal == null) || (accu.oVal !=null && arg.oVal !=null && !accu.oVal.equals(arg.oVal)); break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Compares the arg with the accu. Set the accu to true if accu < arg2 before the operation. Returns a {@link CalculatorExpr#booleanExpr}.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator cmpLessThanOperation = new Operator("<"){
    @SuppressWarnings("unchecked")
    @Override public void operate(Value accu, Value arg) throws Exception {
      switch(accu.etype.typeChar()){
        case 'I': accu.boolVal = accu.intVal < arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal < arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal < arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal < arg.floatVal; break;
        case 'Z': accu.boolVal = !accu.boolVal && arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) < 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable<Object>)accu.oVal).compareTo(arg.oVal) < 0 : false; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Compares the arg with the accu. Set the accu to true if accu >= arg2 before the operation. Returns a {@link CalculatorExpr#booleanExpr}.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator cmpGreaterEqualOperation = new Operator(">="){
    @SuppressWarnings("unchecked")
    @Override public void operate(Value accu, Value arg) throws Exception {
      switch(accu.etype.typeChar()){
        case 'I': accu.boolVal = accu.intVal >= arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal >= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal >= arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal >= arg.floatVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = StringFunctions.startsWith(accu.stringVal, arg.stringVal); break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable<Object>)accu.oVal).compareTo(arg.oVal) >= 0 : false; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Compares the arg with the accu. Set the accu to true if accu > arg2 before the operation. Returns a {@link CalculatorExpr#booleanExpr}.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator cmpGreaterThanOperation = new Operator(">"){
    @SuppressWarnings("unchecked")
    @Override public void operate(Value accu, Value arg) throws Exception {
      switch(accu.etype.typeChar()){
        case 'I': accu.boolVal = accu.intVal > arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal > arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal > arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal > arg.floatVal; break;
        case 'Z': accu.boolVal = accu.boolVal && !arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) > 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable<Object>)accu.oVal).compareTo(arg.oVal) > 0 : false; break;
        case 'e': throw (Exception)accu.oVal;
      }
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Compares the arg with the accu. Set the accu to true if accu <= arg2 before the operation. Returns a {@link CalculatorExpr#booleanExpr}.
   * throws an {@link IllegalArgumentException} "unknown Type" on String or Object types.
   */
  protected static final Operator cmpLessEqualOperation = new Operator("<="){
    @SuppressWarnings("unchecked")
    @Override public void operate(Value accu, Value arg) throws Exception {
      switch(accu.etype.typeChar()){
        case 'I': accu.boolVal = accu.intVal <= arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal <= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal <= arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal <= arg.floatVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) <= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable<Object>)accu.oVal).compareTo(arg.oVal) <= 0 : false; break;
        case 'e': throw (Exception)accu.oVal;
        default: throw new IllegalArgumentException("unknown type" + accu.etype.toString());
      }
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Ands the arg with the accu. Converts the argument to boolean if they are not boolean using {@link Value#booleanValue()}. 
   * Returns a {@link CalculatorExpr#booleanExpr}.
   */
  protected static final Operator boolAndOperation = new Operator("&&"){
    @Override public void operate(Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() && arg.booleanValue();
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  

  
  /**Ors the arg with the accu. Converts the argument to boolean if they are not boolean using {@link Value#booleanValue()}. 
   * Returns a {@link CalculatorExpr#booleanExpr}.
   */
  protected static final Operator boolOrOperation = new Operator("||"){
    @Override public void operate(Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() || arg.booleanValue();
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
   
  /**Ands the arg with the accu. Converts the argument to boolean if they are not boolean using {@link Value#booleanValue()}. 
   * Returns a {@link CalculatorExpr#booleanExpr} if the accu is true
   * respectively a {@link CalculatorExpr#finishBooleanExpr} if the accu is false. In the last case all other And operations
   * cannot change the result, therefore they are not executed.
   */
  protected static final Operator boolCheckAndOperation = new Operator("!&&"){
    @Override public void operate(Value accu, Value arg) {
      accu.boolVal = accu.booleanValue();
      accu.type = 'Z';
      accu.etype = accu.boolVal ? booleanExpr : finishBooleanExpr;  //if false then finish the expression calculation.
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return true; }
  };
  
  
  /**Ors the arg with the accu. Converts the argument to boolean if they are not boolean using {@link Value#booleanValue()}. 
   * Returns a {@link CalculatorExpr#booleanExpr} if the accu is false
   * respectively a {@link CalculatorExpr#finishBooleanExpr} if the accu is true. In the last case all other Or operations
   * cannot change the result, therefore they are not executed.
   */
  protected static final Operator boolCheckOrOperation = new Operator("!||"){
    @Override public void operate(Value accu, Value arg) {
      accu.boolVal = accu.booleanValue();
      accu.type = 'Z';
      accu.etype = accu.boolVal ? finishBooleanExpr : booleanExpr;  //if true then finish the expression calculation.
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return true; }
  };
  
  
  
  
  /**Checks whether the accu is a instance of the given class.
   * The arg of the operate have to be of type java.lang.Class with the instance of the checked type.
   * If the arg is one of the Boxing types for the primitives the operate method returns true if the type in the accu
   * matches to that given boxing type though it is given as simple value, not as boxed, too.
   */
  protected static final Operator cmpInstanceofOperation = new Operator("?instanceof"){
    @Override public void operate(Value accu, Value arg) {
      Object oArg = arg.oVal;
      if(oArg == null || !(oArg instanceof Class<?>)) throw new IllegalArgumentException("\"instanceof\" should compare with a class type instance");
      
      accu.boolVal = accu.oVal !=null && DataAccess.isOrExtends(accu.oVal.getClass(), (Class<?>)oArg) 
          || oArg == java.lang.Float.class     && "FISB".indexOf(accu.type)>=0    //only a primitive value stored which can be converted to float
          || oArg == java.lang.Double.class    && "DFJISB".indexOf(accu.type)>=0  //only a primitive value stored which can be converted to double
          || oArg == java.lang.Long.class      && "JISB".indexOf(accu.type)>=0    //only a primitive value stored which can be converted to long
          || oArg == java.lang.Integer.class   && "ISB".indexOf(accu.type)>=0     //only a primitive value stored which can be converted to integer
          || oArg == java.lang.Short.class     && "SB".indexOf(accu.type)>=0      //only a primitive value stored which can be converted to short
          || oArg == java.lang.Byte.class      && "B".indexOf(accu.type)>=0       //only a primitive value stored which is a byte
          || oArg == java.lang.Character.class && "C".indexOf(accu.type)>=0       //only a primitive value stored which is a character
          || oArg == java.lang.Boolean.class   && "Z".indexOf(accu.type)>=0       //only a primitive value stored which is a boolean
         ;
      accu.type = 'Z';
      accu.etype = booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };
  
  
  
  
  
  
  /**Sets the given value to the accu.*/
  protected static final Operator setOperation = new Operator("!set"){
    @Override public void operate(Value accu, Value arg) {
      accu.type = arg.type;
      accu.etype = arg.etype;
      accu.oVal = arg.oVal;  //if given, copy it always
      switch(accu.type){
        case 'B': case 'S': 
        case 'C': case 'I': accu.intVal = arg.intVal; break;
        case 'J': accu.longVal = arg.longVal; break;
        case 'F': accu.floatVal = arg.floatVal; break;
        case 'D': accu.doubleVal = arg.doubleVal; break;
        case 'Z': accu.boolVal = arg.boolVal; break;
        case 't': accu.stringVal = arg.stringVal; break;
        case 'o': CalculatorExpr.convertObj(accu, arg.oVal); break; //maybe a numeric type Object.
        case 'e': break;  //oVal 
        default: throw new IllegalArgumentException("unknown type" + arg.etype.toString());
      }
    }
    @Override public boolean isUnary(){ return false; }
    @Override public boolean isBoolCheck(){ return false; }
  };


  } //class Operators


  /**An Operation in the list of operations. It contains the operator and maybe a operand.
   * <ul>
   * <li>The {@link Operation#operator} operates with the {@link CalculatorExpr#accu}, the {@link Operation#value}
   *   and maybe the {@link CalculatorExpr#stack}.
   * <li>An unary operator will be applied to the accu firstly. 
   * <li>The operand may be given as constant value, then it is stored with its type in {@link #value}.
   * <li>The operand may be given as index to the given input variables for expression calculation.
   *   Then {@link #ixVariable} is set >=0
   * <li>The operand may be referenced in top of stack, then {@link #ixVariable} = {@value #kStackOperand}
   * <li>The operand may be gotten from any java object, then {@link #datapath} is set.  
   * </ul>
   */
  public static class Operation
  {
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    protected static final int kArgumentUndefined = -4; 
     
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    public static final int kConstant = -1; 
     
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    public static final int kDatapath = -5; 
     
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    public static final int kStackOperand = -2; 
     
    /**Designation of {@link Operation#ixVariable} that the operand is the accumulator for unary operation. */
    public static final int kUnaryOperation = -3; 
    
    private static final int kCheckBoolExpr = -6;
    
     
    /**The operation Symbol if it is a primitive. 
     * <ul>
     * <li>! Take the value
     * <li>+ - * / :arithmetic operation
     * <li>& | : bitwise operation
     * <li>A O X: boolean operation and, or, xor 
     * */
    @Deprecated private char operatorChar;
    
    /**The operator for this operation. 
     * TODO Idea-typedOperator: The Operation should know the type if its given operand
     * and it should know the type of the operations before. Then this operator can be selected
     * for that given type. If the type is unknown, the operator should be a type-indifferent one (which checks the type).*/
    protected Operator operator;
    
    /**Either it is null or the operation has a single unary operator for the argument. */
    Operator unaryOperator;
    
    /**Either it is null or the operation has some unary operators (more as one). */
    List<Operator> unaryOperators;
    
    /**Number of input variable from array or special designation.
     * See {@link #kStackOperand} */
    protected int ixVariable;
    
    /**A constant value. @deprecated, use value*/
    @Deprecated
    double value_d;
    
    /**A constant value. @deprecated, use value*/
    @Deprecated
    Object oValue;
    
    /**It is used for constant values which's type is stored there too. */
    protected Value value;
    
    /**Set if the value of the operation should be gotten by data access on calculation time. */
    protected DataAccess.DataAccessSet datapath;
    
    public Operation(){
      this.ixVariable = kArgumentUndefined;
    }
    
    public Operation(String op, Value value){
      setOperator(op);
      //this.operator = op;
      //this.operatorChar = op.name.charAt(0);  //not only for all, therefore deprecated.
      this.ixVariable = this.operator.isUnary() ? kUnaryOperation : kArgumentUndefined; 
      this.value = value;
    }
    
    public Operation(Operator op, Value value){
      this.operator = op;
      this.operatorChar = op.name.charAt(0);  //not only for all, therefore deprecated.
      this.ixVariable = this.operator.isUnary() ? kUnaryOperation : kArgumentUndefined; 
      this.value = value;
    }
    
    public Operation(Operator op, int ixVariable){
      this.operator = op;
      this.operatorChar = op.name.charAt(0);  //not only for all, therefore deprecated.
      this.ixVariable = ixVariable; 
      if(op.isUnary()){
        assert(ixVariable == kUnaryOperation);
      }
    }
    
    public Operation(String op, int ixVariable){
      setOperator(op);
      this.ixVariable = ixVariable;
    }
    
    Operation(String operation, double value){ 
      this.value_d = value; this.value = new Value(value); 
      this.operator = getOperator(operation);
      this.operatorChar = this.operator.name.charAt(0);
      this.ixVariable = kConstant; 
      this.oValue = null; 
    }
    //Operation(char operation, int ixVariable){ this.value_d = 0; this.operation = operation; this.ixVariable = ixVariable; this.oValue = null; }
    //Operation(char operation, Object oValue){ this.value = 0; this.operation = operation; this.ixVariable = -1; this.oValue = oValue; }
    Operation(Operator operator, Object oValue){ 
      this.value_d = 0; this.operator = operator; this.operatorChar = operator.name.charAt(0); this.ixVariable = kConstant; this.oValue = oValue; 
    }
  
    
    
    public boolean hasOperator(){ return operator !=null; }
    
    public boolean hasUnaryOperator(){ return unaryOperator !=null || unaryOperators !=null; }
    
    public void add_datapathElement(DataAccess.DatapathElement item){ 
      if(datapath == null){ datapath = new DataAccess.DataAccessSet();}
      datapath.add_datapathElement(item); 
    }
    
    public void set_intValue(int val){
      if(value == null){ value = new Value(); }
      value.type = 'I';
      value.etype = intExpr;
      value.intVal = val;
    }
    
    
    public void set_doubleValue(double val){
      if(value == null){ value = new Value(); }
      value.type = 'D';
      value.etype = doubleExpr;
      value.doubleVal = val;
    }
    
    
    public void set_charValue(char val){
      if(value == null){ value = new Value(); }
      value.type = 'C';
      value.etype = intExpr;
      value.intVal = val;
    }
    
    
    public void set_textValue(String val){
      if(value == null){ value = new Value(); }
      value.type = 't';
      value.etype = stringExpr;
      value.stringVal = StringSeq.create(val);
    }

    /**Designates that the operand should be located in the top of stack. */
    public void setStackOperand(){ ixVariable = kStackOperand; }
    
    
    
    
    
    /**Sets one of some unary operators.
     * @param unary
     */
    public void addUnaryOperator(Operator unary){
      if(unaryOperators !=null){
        unaryOperators.add(unary);
      }
      else if(unaryOperator !=null){
        //it is the second one:
        unaryOperators = new ArrayList<Operator>();
        unaryOperators.add(unaryOperator);
        unaryOperators.add(unary);
        unaryOperator = null;  //it is added
      } else {
        unaryOperator = unary;
      }
      
    }
    
    public boolean addUnaryOperator(String op){
      Operator unary = operators.get(op);
      if(unary !=null) {
        addUnaryOperator(unary);
        return true;
      } else {
        return false;
      }
    }
    
    
    
    public void setOperator(Operator op){
      this.operator = op;
      this.operatorChar = op.name.charAt(0);
      if(op.isUnary()){
        ixVariable = kUnaryOperation;
      }
    }
    
    
    
    public boolean setOperator(String op){
      if(op.equals("?instanceof"))
        Debugutil.stop();
      Operator op1 = operators.get(op);
      this.operatorChar = op1.name.charAt(0);
      if(op1 !=null){ 
        setOperator(op1);
      }
      return this.operator !=null; 
    }
    
    /**Returns the datapath of this operation or null if the operator is not a datapath.
     */
    public DataAccess datapath(){ return datapath; }
    
    @Override public String toString(){ 
      StringBuilder u = new StringBuilder();
      u.append(operator);
      if(unaryOperator !=null){ u.append(" ").append(unaryOperator); }
      if(unaryOperators !=null){ u.append(" ").append(unaryOperators); }
      u.append(' ');
      if(ixVariable >=0) u.append(" arg[").append(ixVariable).append("]");
      else if(ixVariable == kConstant) u.append(" const ");
      else if(ixVariable == kStackOperand) u.append(" stack ");
      else if(ixVariable == kUnaryOperation) u.append(" unary:accu ");
      else if(ixVariable == kArgumentUndefined) u.append(" arg undef ");
      else if(ixVariable == kDatapath) u.append(" datapath: ");
      else if(ixVariable == 0);
      else u.append(" ?unknown ixVariable=").append(ixVariable);
      
      if(datapath !=null) u.append(datapath.toString());
      if (oValue !=null) u.append(" oValue:").append(oValue.toString());
      if (value !=null) u.append(" ").append(value.toString());
      //else u.append(" double:").append(value_d);
      return u.toString();
    }
  }
  
  
  /**This class and its inner class {@link SetExpr} is a helper class which contains a {@link CalculatorExpr} as composite. 
   * That {@link CalculatorExpr} will be filled with the operations which are assembled with this helper. 
   * Last the {@link SetExpr#expr()} should be invoked to get the expression. After them this instance can be garbaged.
   * 
   * It provides a set interface to the CalculatorExpr to store set parse results for the expression, 
   * especially able to use for the ZBNF parser using {@link org.vishia.zbnf.ZbnfJavaOutput}.
   * <ul>
   * <li>The methods of this class have to be called in the request order for operation. 
   *   The rule first multiplication, outside addition should be regard by the user of this class. 
   *   If an expression is parsed by the ZBNF parser or another parser, the parser syntax should know that rules.
   * <li>The first operation may be {@link SetExpr#new_dataAccess()} to access any data or {@link SetExpr#set_doubleValue(double)} to start with a constant value.
   *   Both methods stores a set operation with the given value and returns this or a proper instance for further operations.
   * <li>Especially {@link SetExpr#newDataAccessSet()} returns an instance for the {@link DataAccess} data. 
   *   With that filled instance {@link SetExpr#add_dataAccess(org.vishia.util.DataAccess.DataAccessSet)} should be called then.
   * <li>Further operations can be for example {@link SetExpr#new_multOperation()}, then SetExpr.set_xyzValue(val) and after them
   *   {@link SetExpr#add_multOperation(...)}. Any new_xyz operation stores the old one operation and sets a new empty operation as the current one.
   *   The following data are set to this operation. The following add_mult...() sets the operator and adds the operation.
   * <li>A {@link SetExpr#new_parenthesisExpr()} stores and clears the {@link SetExpr#actOperation}. Thus such as {@link SetExpr#set_intValue(int)}
   *   creates an new set operation which is stored ad start-Operation.If the execution sees a start operation and the accumulator contains any data,
   *   the accumulator will be stored in the stack and filled with the new value. 
   * <li>If a operation does not contain data, it is a operation with the stack content.     
   * </ul>
   * @author Hartmut Schorrig
   *
   */
  public static class SetExprBase //extends CalculatorExpr
  {
    private CalculatorExpr.Operation actOperation; // = new Operation("!");
    
    public final CalculatorExpr expr;
    
    protected final Object dbgParent;
    
    public SetExprBase(boolean d, Object dbgParent){
      this.expr = new CalculatorExpr();
      this.dbgParent = dbgParent;
    }
    
    public SetExprBase(boolean d){
      this.expr = new CalculatorExpr();
      this.dbgParent = null;
    }
    
    public SetExprBase(CalculatorExpr expr){
      this.expr = expr;
      this.dbgParent = null;
    }
    
    
    public SetExprBase(SetExprBase parent){
      this.expr = parent.expr;
      this.dbgParent = parent;
    }
    
    
    /**Instances of this inner class are created with parenthesis expressions and for the whole expression.
     * The reason is: store unary operators and the index for check boolean operations.
     *
     */
    public class SetExpr 
    {
    
      public final SetExpr parent;
      
      private final List<CalculatorExpr.Operator> unaryOperators = new ArrayList<CalculatorExpr.Operator>(); // = new Operation("!");

      private final int ixList;

      /**Creates a new instance of this class for sub expressions (parenthesis, arguments).
       * This method should be overridden and should create the derived instance if a derived
       * expression algorithm is used.
       * 
       * @param parent may be null.
       * @return a new instance which refers the parent.
       */
      //public SetExpr new_SetExpr(SetExpr parent){ return new SetExpr(parent); }
      
      
      public SetExpr() { parent = null; ixList = 0; }
      
      private SetExpr(SetExpr parent) { this.parent = parent; ixList = expr.listOperations.size(); }
      
      public CalculatorExpr expr() { return SetExprBase.this.expr; }
      
      /**Designates, that a expression in parenthesis is given, which should be calculated first.
       * A existing actOperation will be stored, and the {@link #actOperation} is set to null.
       * Thus a new set operation will be created at first of the parenthesis expression.
       * On runtime it forces a push to the stack.
       * @return this
       */
      public SetExpr new_parenthesisExpr(){
        //assert(actOperation == null);
        if(actOperation !=null){ //A start operation before
          addToOperations();
        }
        SetExpr pExpr = new SetExpr(this);
        return pExpr;  //new storage for unaryOperators
      }

      /**Closes an parenthesis expression. The actOperation will be stored and all bool check operations will be designated with the end index
       * The next operations have to be invoked with val.parent and the val instance itself should be garbaged.
       * @param val
       */
      public void add_parenthesisExpr(SetExpr val){
        val.closeExprPreparation();
      }

    
    
      public SetExpr new_boolOrOperation(){
        if(actOperation !=null){ addToOperations(); }
        return this;
      }
      
      
      /**Designates the end of a multiplication operation. Takes the operation into the expression list.
       * @param val this, unused
       */
      public void add_boolOrOperation(SetExpr val){
        if(actOperation ==null){
          actOperation = new CalculatorExpr.Operation();
          actOperation.setStackOperand();  
        }
        actOperation.setOperator("||");
        addToOperations(); 
      }
      
      
      
      public SetExpr set_boolCheckAndOperation(){
        if(actOperation !=null){ addToOperations(); }
        actOperation = new CalculatorExpr.Operation();
        actOperation.setOperator("!&&");
        actOperation.ixVariable = Operation.kCheckBoolExpr;
        return this;
      }
      
      
      
      public SetExpr set_boolCheckOrOperation(){
        if(actOperation !=null){ addToOperations(); }
        actOperation = new CalculatorExpr.Operation();
        actOperation.setOperator("!||");
        actOperation.ixVariable = Operation.kCheckBoolExpr;
        return this;
      }
      
      
      
      public SetExpr new_boolAndOperation(){
        if(actOperation !=null){ addToOperations(); }
        return this;
      }
      
      
      /**Designates the end of a AND operation. Takes the operation into the expression list.
       * @param val this, unused
       */
      public void add_boolAndOperation(SetExpr val){
        if(actOperation ==null){
          actOperation = new CalculatorExpr.Operation();
          actOperation.setStackOperand();  
        }
        actOperation.setOperator("&&");
        addToOperations(); 
      }
      
      
      public void set_boolNot(String val){
        unaryOperators.add(CalculatorExpr.getOperator("u!"));
      }
      
      
      
      
      public SetExpr new_cmpOperation(){
        if(actOperation !=null){
          addToOperations();  //if it is a start operation.
        }
        actOperation = new CalculatorExpr.Operation();
        return this;
      }
      
      public void add_cmpOperation(SetExpr val){
        addToOperations(); 
      }
  
      
      public void set_cmpOperator(String val){
        if(actOperation ==null){
          assert(false);
          actOperation = new CalculatorExpr.Operation();
        }
        actOperation.setOperator(val);
      }
      
      
      
      public void set_unaryOperator(String op){
        CalculatorExpr.Operator unaryOp = CalculatorExpr.getOperator("u" + op);
        assert(unaryOp !=null);  //should match to syntax prescript
        unaryOperators.add(unaryOp);
      }
      
      /**Designates the start of a new adding operation. The first start value should be taken into the
       * stackOperation statement list as start operation.
       * @return this
       */
      public SetExpr new_addOperation(){
        if(actOperation !=null){ addToOperations(); }
        assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
        return this;  
      }
      
      /**Designates the end of an add operation. Takes the operation into the expression list.
       * @param val this, unused
       */
      public void add_addOperation(SetExpr val){
        if(actOperation ==null){
          actOperation = new CalculatorExpr.Operation();
          actOperation.setStackOperand();  
        }
        actOperation.setOperator("+");
        addToOperations(); 
      }
  
      /**Designates the start of a new adding operation. The first start value should be taken into the
       * stackOperation statement list as start operation.
       * @return this
       */
      public SetExpr new_subOperation(){
        if(actOperation !=null){ addToOperations(); }
        assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
        return this;  
      }
      
      /**Designates the end of an add operation. Takes the operation into the expression list.
       * @param val this, unused
       */
      public void add_subOperation(SetExpr val){
        if(actOperation ==null){
          actOperation = new CalculatorExpr.Operation();
          actOperation.setStackOperand();  
        }
        actOperation.setOperator("-");
        addToOperations(); 
      }
  
      public SetExpr new_multOperation(){
        if(actOperation !=null){ addToOperations(); }
        assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
        return this;
      }
      
      /**Designates the end of a multiplication operation. Takes the operation into the expression list.
       * @param val this, unused
       */
      public void add_multOperation(SetExpr val){
        if(actOperation ==null){
          actOperation = new CalculatorExpr.Operation();
          actOperation.setStackOperand();  
        }
        actOperation.setOperator("*");
        addToOperations(); 
      }
  
  
      /**A character is stored as integer. 
       * @param val
       */
      public void set_charValue(String val){
        if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
        actOperation.set_charValue(val.charAt(0));
      }
      
      
      /**Sets a value to the current operation. 
       * @param val
       */
      public void set_intValue(int val){
        if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
        actOperation.set_intValue(val);
      }
      
      
      
      /**Sets a value to the current operation. 
       * @param val
       */
      public void set_doubleValue(double val){
        if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
        actOperation.set_doubleValue(val);
      }
      
      
      
      public void set_textValue(String val){
        if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
        actOperation.set_textValue(val);
      }
      
      
      
      
      
      /**It is override-able to create an derived instance.
       */
      protected DataAccess.DataAccessSet newDataAccessSet(){ return new DataAccess.DataAccessSet(); }
      
      /**Returns this because all methods of {@link DataAccess.DataAccessSet} are delegated in this class.
       * @return
       */
      public DataAccess.DataAccessSet new_dataAccess(){ 
        if(actOperation != null)
          Debugutil.stop();
        //assert(actOperation ==null);
        if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
        if(actOperation.datapath == null){ actOperation.datapath = newDataAccessSet();}
        return actOperation.datapath;
      } 
      
      public void add_dataAccess(DataAccess.DataAccessSet val){ }
      
  
      /**Returns the datapath if the expression contains only a datapath.
       * @return null if the expression is more complex.
       */
      public DataAccess onlyDataAccess(){
        if((expr.listOperations == null || expr.listOperations.size() ==0)
          && actOperation !=null
          && unaryOperators.size() ==0
          && actOperation.unaryOperator == null && actOperation.unaryOperators == null
          && actOperation.value == null
          && actOperation.datapath !=null
          //&& actOperation.ixVariable == Operation.kDatapath
        ){ 
          return actOperation.datapath;
        } else {
          return null;
        }
      }
      
  
      /**This routine must be called at least. It adds a simple value to the operation list.
       * If any second value was added already, the routine does nothing.
       */
      public void closeExprPreparation(){
        if(actOperation !=null){
          addToOperations();
        }
        int ixEnd = expr.listOperations.size();
        //Mark all checkBool-operations with the end index of this parenthesis block.
        //start on start of parenthesis block, end till current end.
        for(int ix = ixList; ix < ixEnd; ++ix) {
          Operation op = expr.listOperations.get(ix);
          if(op.ixVariable == Operation.kCheckBoolExpr) {
            op.ixVariable = ixEnd;
          }
        }
      }
      
      private void addToOperations(){
        if(!actOperation.hasOperator()){
          actOperation.setOperator("!set");  //it is initial value
        }
        if(unaryOperators.size()==1){
            actOperation.addUnaryOperator(unaryOperators.get(0));
        } else if(unaryOperators.size()>1) {
          ListIterator<CalculatorExpr.Operator> iter = unaryOperators.listIterator();
          while(iter.hasPrevious()){
            CalculatorExpr.Operator unary = iter.previous();
            actOperation.addUnaryOperator(unary);
          }
        }
        unaryOperators.clear();  //a new one is necessary.
        expr.addOperation(actOperation);
        actOperation = null;  //a new one is necessary.
      }
  
  
      
      /**Adds the {@link #actUnaryOperation} to the expression statements.
       * 
       */
      private void addUnaryToOperations(){
        if(unaryOperators !=null){
          ListIterator<CalculatorExpr.Operator> iter = unaryOperators.listIterator();
          while(iter.hasNext()){ iter.next(); }  //walk to end
          while(iter.hasPrevious()){             //operate backward
            CalculatorExpr.Operator unaryOp = iter.previous();
            CalculatorExpr.Operation unaryOperation = new CalculatorExpr.Operation(unaryOp, CalculatorExpr.Operation.kUnaryOperation);
            expr.addOperation(unaryOperation);    //add the unary as operation. Apply on accu.
          }
          unaryOperators.clear();  //a new one is necessary.
        }
      }
    }
  }

  
  
  
  
  /**All Operations which acts with the accumulator and the stack of values.
   * They will be executed one after another. All calculation rules of prior should be regarded
   * in this order of operations already. It will not be checked here.
   */
  protected final List<Operation> listOperations = new ArrayList<Operation>();
  
  
  /**The stack of values used temporary. It is only in used while any calculate routine runs. 
   * The top of all values is not stored in the stack but in the accu. It means that often the stack 
   * is not used. */
  protected final Stack<Value> stack = new Stack<Value>();
  
  /**The top of stack is the accumulator for the current level of adequate operations,
   * for example all multiplications without stack changing. It is the left operand of an operation.
   * The right operand is given in the operation itself. It an operation acts with the last 2 stack levels,
   * the value of the top of stack (the accu) is set locally in the operation and the second value of the 
   * stack operands is set to the accu. */
  protected Value accu = new Value();
  
  protected String[] variables;
  
  
  
  
  
  /**Map of all available operators associated with its String expression.
   * It will be initialized with:
   * <ul>
   * <li>"!set" Set the value to accu, set the accu to the stack.
   * <li>"+" "-" "*" "/" known arithmetic operators
   * <li>">=" "<=" ">" "<" "==" "!=" knwon compare operators
   * <li>"<>" other form of not equal operator
   * <li>"ge" "le" "gt" "lt" "eq" "ne" other form of comparators
   * <li>"||" "&&" known logical opeators
   * <li>"instanceof"
   * </ul>
   * Unary operators:
   * <ul> 
   * <li>"b" Convert to boolean.
   * <li>"b!" boolean not operator
   * <li>"u~" bit negation operator
   * <li>"u-" numeric negation
   * </ul>
   */
  protected static Map<String, Operator> operators;
  
  
  /**Map of all available unary operators associated with its String expression.
   * It will be initialized with:
   * <ul>
   * </ul>
   */
  //protected static Map<String, UnaryOperator>  unaryOperators;
  
  
  public CalculatorExpr(){
    if(operators == null){
      operators = new TreeMap<String, Operator>();
      operators.put("!",  Operators.setOperation);   //set accu to operand
      operators.put("!set",  Operators.setOperation);   //set accu to operand
      operators.put("+",  Operators.addOperation);
      operators.put("-",  Operators.subOperation);
      operators.put("*",  Operators.mulOperation);
      operators.put("/",  Operators.divOperation);
      operators.put(">=", Operators.cmpGreaterEqualOperation);
      operators.put(">",  Operators.cmpGreaterThanOperation);
      operators.put("<=", Operators.cmpLessEqualOperation);
      operators.put("<",  Operators.cmpLessThanOperation);
      operators.put("!=", Operators.cmpNeOperation);
      operators.put("<>", Operators.cmpNeOperation);
      operators.put("==", Operators.cmpEqOperation);
      operators.put("lt", Operators.cmpLessThanOperation);
      operators.put("le", Operators.cmpLessEqualOperation);
      operators.put("gt", Operators.cmpGreaterThanOperation);
      operators.put("ge", Operators.cmpGreaterEqualOperation);
      operators.put("eq", Operators.cmpEqOperation);
      operators.put("ne", Operators.cmpNeOperation);
      operators.put("instanceof", Operators.cmpInstanceofOperation);
      operators.put("!||", Operators.boolCheckOrOperation);
      operators.put("||", Operators.boolOrOperation);
      operators.put("!&&", Operators.boolCheckAndOperation);
      operators.put("&&", Operators.boolAndOperation);
      operators.put("ub", Operators.boolOperation);   //not for boolean
      operators.put("u!", Operators.boolNotOperation);   //not for boolean
      operators.put("u~", Operators.bitNotOperation);   //not for boolean
      operators.put("u-", Operators.negOperation);   //not for boolean

    }
  }
  
  
  /**Separates a name and the parameter list from a given String.
   * @param expr An expression in form " name (params)"
   * @return A String[2].
   *   The ret[0] is the expr without leading and trailing spaces if the expr does not contain a "("
   *   The ret[0] is "" if the expr contains only white spaces before "(" or it starts with "("
   *   The ret[0] contains the string part before "(" without leading and trailing spaces.
   *   The ret[1] contains the part between "(...)" without "()".
   *   The ret[1] is null if no "(" is found in expr..
   *   If the trailing ")" is missing, it is accepting.
   */
  public static String[] splitFnNameAndParams(String expr){
    String[] ret = new String[2];
    int posSep = expr.indexOf('(');
    if(posSep >=0){
      ret[0] = expr.substring(0, posSep).trim();
      int posEnd = expr.lastIndexOf(')');
      if(posEnd < 0){ posEnd = expr.length(); }
      ret[1] = expr.substring(posSep+1, posEnd);
    } else {
      ret[0] = expr.trim();
      ret[1] = null;
    }
    return ret;
  }
  

  
  /**Separates String parameters from a list.
   * Implementation yet: only split
   * Planned: detects name(param, param) inside a parameter.
   * @param expr Any expression with Strings separated with colon
   * @return The split expression, all arguments are trimmed (without leading and trailing spaces).
   */
  public static String[] splitFnParams(String expr){
    String[] split = expr.split(",");
    for(int ii=0; ii<split.length; ++ii){
      split[ii] = split[ii].trim();
    }
    return split;
  }
  
  /**Converts the given expression in a stack operable form.
   * The parsing of sExpr starts with an adding expression.
   * Its operators are read as multExpression.
   * @param sExpr String given expression such as "X*(Y-1)+Z"
   * @param sIdentifier List of identifiers for variables.
   * @return null if ok or an error description.
   */
  public String setExpr(String sExpr, String[] sIdentifier)
  { listOperations().clear();
    this.variables = sIdentifier;
    StringPartScan spExpr = new StringPartScan(sExpr);
    try{ parseAddExpr(spExpr, "!", 1);  
    } catch(ParseException exc){ return exc.getMessage(); }
    return null;
  }
  
  /**Converts the given expression in a stack operable form. One variable with name "X" will be created.
   * It means the expression can use "X" as variable.
   * @param sExpr For example "5.0*X" or "(X*X+1.5*X)"
   * @see #setExpr(String, String[])
   */
  public String setExpr(String sExpr)
  { listOperations().clear();
    this.variables = new String[]{"X"};
    StringPartScan spExpr = new StringPartScan(sExpr);
    try{ parseAddExpr(spExpr, "!", 1); 
    } catch(ParseException exc){ return exc.getMessage(); }
    return null;
  }
  

  /**The expression is an add or subtract expression.
   * call recursively for any number of operands.
   * call {@link #multExpr(StringPartScan, char)} to get the argument values.
   * @param spExpr
   * @param operation The first operation. On start operand it is "!" for set.
   * @return this
   */
  protected void parseAddExpr(StringPartScan spExpr, String startOperation, int recursion)
  throws ParseException
  { if(recursion > 1000) throw new RuntimeException("recursion");
    String op = startOperation;
    while(op != null) {
      parseMultExpr(spExpr, op, recursion +1);
      if(spExpr.scanSkipSpace().length()>0){
        char cc = spExpr.getCurrentChar();
        if("+-".indexOf(cc)>=0){
          spExpr.seek(1).scan().scanOk();
          op = "" + cc;
          //addExpr(spExpr, ""+cc, recursion+1);
        }
        else { op = null; } //end of addExpr, maybe on a ")"
      }
      else { op = null; }
    }//while
  }
  
  
  /**The more inner expression is a multiply or divide expression.
   * call recursively for any number of operands.
   * call {@link #parseArgument(StringPartScan, String, int)} to get the argument values. 
   * @param spExpr
   * @param operation The first operation. On start operand it is "!" for set.
   * @return
   */
  protected String parseMultExpr(StringPartScan spExpr, String startOperation, int recursion) 
  throws ParseException
  { if(recursion > 1000) throw new RuntimeException("recursion");
    String sError = null;
    String operation = startOperation;
    while(operation != null) {
      parseArgument(spExpr, operation, recursion);
      if(spExpr.scanSkipSpace().length()>0){
        char cc = spExpr.getCurrentChar();
        if("*/".indexOf(cc)>=0){
          spExpr.seek(1).scan().scanOk();
          operation = "" + cc;
          //addExpr(spExpr, ""+cc, recursion+1);
        }
        else { operation = null; } //end of addExpr, maybe on a ")"
      }
      else { operation = null; }
    }//while
    return sError;  //ok
  }
  

  
  
  /**Parses one argument for a {@link #parseMultExpr(StringPartScan, String, int)}
   * or maybe a {@link #parseAddExpr(StringPartScan, String, int)} on absens of multiply operation.
   * <ul>
   * <li>The argument can be especially an expression in parenthesis, for that an {@link #parseAddExpr(StringPartScan, String, int)}
   *   is invoked for the inner expression.
   * <li>An identifier which is found in the variables
   * <li>An constant value.
   * <li>TODO functions (Math) are not supported yet.
   * <li>TODO access to java data, therewith to Math.sin() etc. are not supported yet. Use {@link SetExpr} to use that. 
   *   Then an expression is parsed via ZBNF, used in the JZcmd script language.  
   * </ul>  
   * @param spExpr
   * @param operation
   * @param recursion
   * @throws ParseException
   */
  protected void parseArgument(StringPartScan spExpr, String operation, int recursion ) throws ParseException
  {
    if(spExpr.scanSkipSpace().scan("(").scanOk()){
      parseAddExpr(spExpr, "!", recursion+1);
      if(!spExpr.scanSkipSpace().scan(")").scanOk()) throw new ParseException(") expected", (int)spExpr.getCurrentPosition());
      listOperations.add(new Operation(operation, Operation.kStackOperand));
    } else if(spExpr.scanSkipSpace().scanIdentifier().scanOk()){
          CharSequence sIdent = spExpr.getLastScannedString();
      int ix;
      for(ix = 0; ix< variables.length; ++ix){
        if(StringFunctions.equals(variables[ix],sIdent)){
          listOperations.add(new Operation(operation, ix));
          ix = Integer.MAX_VALUE-1; //break;
        }
      }
      if(ix != Integer.MAX_VALUE){ //variable not found
        throw new ParseException("Variable not found: " + sIdent, (int)spExpr.getCurrentPosition());
      }
    } else if(spExpr.scanSkipSpace().scanInteger().scanOk()) {
      Value value = new Value();
      boolean bNegative = spExpr.getLastScannedIntegerSign();
      long longvalue = spExpr.getLastScannedIntegerNumber();
      if(spExpr.scanFractionalNumber(longvalue, bNegative).scanOk()) {
        double dval = spExpr.getLastScannedFloatNumber();
        if(spExpr.scan("F").scanOk()){
          value.floatVal = (float)dval;
          value.type = 'F';
          value.etype = floatExpr;
        } else {
          value.doubleVal = dval;
          value.type = 'D';
          value.etype = doubleExpr;
        }
      } else {
        //no float, check range of integer
        if(longvalue < 0x80000000L && longvalue >= -0x80000000L) {
          value.intVal = (int)longvalue; value.type = 'I'; value.etype = intExpr;
        } else {
          value.longVal = longvalue; value.type = 'J'; value.etype = longExpr;
        }
      }
      listOperations.add(new Operation(operation, value));
    }
  }
  
  
  
  
  
  
  /**Adds the given operation to the list of operations. The list of operations is executed one after another.
   * All calculation rules such as parenthesis, prior of multiplication, functions arguments etc.
   * should be regarded by the user. 
   * @param operation
   */
  public void addOperation(Operation operation){
    listOperations.add(operation);
  }
  
  
  
  /**Gets the list of operations in return polish notation.
   * Especially a datapath may completed with actual arguments of its called methods.
   */
  public List<Operation> listOperations(){ return listOperations; }
  
  
  /**Gets a operator to prepare operations.
   * @param op Any of "+", "-" etc.
   * @return null if op is faulty.
   */
  public static Operator getOperator(String op){ return operators.get(op); }
  
  
  
  /**Adds a operation to the execution stack.
   * Operation:
   * <ul>
   * <li>+, - * /
   * <li> < > = l, g 
   * </ul>
   * @param val
   * @param operation
   */
  public void XXXaddExprToStack(Object val, String operation){
    Operator operator = operators.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, val);
    listOperations.add(stackelement);
  }
  
  
  public void XXXaddExprToStack(int ixInputValue, String operation){
    Operator operator = operators.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, ixInputValue);
    listOperations.add(stackelement);
  }
  
  
  
  /**Calculates the expression with only one input. This is a simple variant for scaling values etc.
   * It invokes internally {@link #calcDataAccess(Map, Object...)} but without a access Map.
   * It means it must not contain access to other data.
   * @param input The only one input value.
   * @return The result.
   */
  public double calc(double input)
  { try{ calcDataAccess(null, input);
    } catch(Exception exc){ throw new RuntimeException(exc); }
    return this.accu.doubleValue();
  }
  

  
  /**Calculates the expression with only one float input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  @SuppressWarnings("synthetic-access") 
  public float Oldcalc(float input)
  { float val = 0;
    for(Operation oper: listOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operatorChar){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  

  /**Calculates the expression with only one input. This is a simple variant for scaling values etc.
   * It invokes internally {@link #calcDataAccess(Map, Object...)} but without a access Map.
   * It means it must not contain access to other data.
   * @param input The only one input value.
   * @return The result.
   */
  public float calc(float input)
  { try{ calcDataAccess(null, input);
    } catch(Exception exc){ throw new RuntimeException(exc); }
    return this.accu.floatValue();
  }
  
  
  /**Calculates the expression with only one input. This is a simple variant for scaling values etc.
   * It invokes internally {@link #calcDataAccess(Map, Object...)} but without a access Map.
   * It means it must not contain access to other data.
   * @param input The only one input value.
   * @return The result.
   */
  public long calcLong(long input)
  { try{ calcDataAccess(null, input);
    } catch(Exception exc){ throw new RuntimeException(exc); }
    return (long)this.accu.doubleValue();
  }
  
  
  /**Calculates the expression with only one input. This is a simple variant for scaling values etc.
   * It invokes internally {@link #calcDataAccess(Map, Object...)} but without a access Map.
   * It means it must not contain access to other data.
   * @param input The only one input value.
   * @return The result.
   */
  public long calcLong(int input)
  { try{ calcDataAccess(null, input);
    } catch(Exception exc){ throw new RuntimeException(exc); }
    return this.accu.intValue();
  }
  
  
  /**Calculates the expression with only one integer input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  @SuppressWarnings("synthetic-access") 
  public float Oldcalc(int input)
  { float val = 0;
    for(Operation oper: listOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operatorChar){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  
  
  
  
  /**Calculates the expression with some inputs, often only 1 input.
   * Before calculate, one should have defined the expression using {@link #setExpr(String)}
   * or {@link #setExpr(String, String[])} or via {@link SetExpr}.
   * @param args Array of some inputs
   * @return The result of the expression.
   */
  public Value calc(Object... args) { 
    try{ return calcDataAccess(null, args); 
    } catch(Exception exc){ throw new RuntimeException(exc); //unexpected if the expression does not contain a dataAccess.
    }
  }
  
  
  
  
  
  
  
  /**Calculates the expression with possible access to any stored object data with access via reflection.
   * @param javaVariables Any data which are access-able with its name. It is the first part of a datapath.
   * @param args Some args given immediately. Often numerical args. Often not used.
   * @return The result wrapped with a Value instance. This Value contains also the type info. 
   * @throws Exception Any exception is possible. Especially {@link java.lang.NoSuchFieldException} or such
   *   if the access via reflection is done.
   */
  public Value calcDataAccess(Map<String, DataAccess.Variable<Object>> javaVariables, Object... args) throws Exception{
    accu = new Value(); //empty, use a new instance because the last one may be used as reference anywhere.
    Value val2jar = new Value();  //Instance to hold values for the right side operand.
    Value val2; //Reference to the right side operand
    //ExpressionType type = startExpr;
    int ix = 0;
    int ixEnd = listOperations.size();
    while(ix < ixEnd){
      Operation oper = listOperations.get(ix);
      ix +=1;
      /*
       * Note: this block is not necessary, instead boolCheckAndOperation and boolCheckOrOperation is necessary
       * because only that works correct if an paranthesisexpression follows.
       * 
      if(accu.type == 'Z' && //special for boolean operation: don't evaluate an operand if it is not necessary.
          ( !accu.boolVal && (  oper.operator == Operators.boolAndOperation  //false remain false on and operation
                             //|| oper.operator == Operators.boolCheckAndOperation
                             )
          || accu.boolVal && oper.operator == Operators.boolOrOperation   //true remain true on or operation
        ) ){
        //don't get arguments, no side effect (like in Java, C etc.
      } else */ 
      {
        //Get the operand either from args or from Operation
        if(!oper.operator.isBoolCheck() && oper.ixVariable >=0){ 
          Object oval2 = args[oper.ixVariable];   //may throw ArrayOutOfBoundsException if less arguments
          //assert(oval2 !=null);
          convertObj(val2jar, oval2);
          val2 = val2jar;
        }  //an input value
        else if(oper.ixVariable == Operation.kStackOperand){
          val2 = accu;
          accu = stack.pop();              //may throw Exception if the stack is emtpy.
          //oval2 = null;
        }
        else if(oper.datapath !=null){
          try{
            Object oval2 = oper.datapath.access(javaVariables, true, false);
            convertObj(val2jar, oval2);
            val2 = val2jar;
          } catch(Exception exc){
            //get data does not found data or returns null:
            val2jar.type = 'e'; val2jar.oVal = exc;  //throw the exception if the oVal is need not for boolean or instanceof
            val2jar.etype = variableNotFoundExpr;
            val2 = val2jar;
          }
        }
        else {
          val2 = oper.value;
          //oval2 = null;
        }
        if(oper.operator == Operators.setOperation && accu.type != '?'){
          ExpressionType type = accu.etype;
          stack.push(accu);    //the accu is filled with a value, therefore push it and replace the accu with the new value. The accu is the top of stack substantially.
          accu = new Value();
          accu.etype = type;  //start with the expression type not lower then the pushed. Elsewhere it would set the expression type from the first operand.
          //type = startExpr;
        }
        //Convert the value adequate the given type of expression:
        if(!oper.operator.isUnary() && val2 !=null){  //if unary, don't change the type
          accu.etype.checkArgument(accu, val2);    //may change the type.
        }
        //
        //executes the operation:
        if(oper.unaryOperator !=null){
          if(val2 !=null && val2 != val2jar) {
            //a constant value is referred, don't change it:
            val2jar.copy(val2);
            val2 = val2jar;
          }
          oper.unaryOperator.operate(val2, null);   //change the right value
        }
        else if(oper.unaryOperators !=null){
          if(val2 !=null && val2 != val2jar) {
            //a constant value is referred, don't change it:
            val2jar.copy(val2);
            val2 = val2jar;
          }
          for(Operator unary: oper.unaryOperators){
            unary.operate(val2, null);   //change the right value
          }
        }
        //!!!!!!!!!!
        oper.operator.operate(accu, val2);  //operate, may change the type if the operator forces it.
        //
        if(accu.etype == finishBooleanExpr){
          ix = oper.ixVariable;
          //break;  //break the operation calculation, the result is given for AND or OR expression.
        }
      }
    }
    return accu;
  }
  
  
  
  static void convertObj(Value val2jar, Object oval2)
  {
    //Convert a Object-wrapped value into its real representation.
    val2jar.oVal = oval2;  //store the value in its wrapped instance in any case. Used for instanceof
    //Note: longVal etc. are not cleaned in any case
    if(oval2 instanceof Value)            { val2jar.copy((Value)oval2); }
    else if(oval2 instanceof Long)        { val2jar.longVal =   ((Long)oval2).longValue();     val2jar.type = 'J'; val2jar.etype = longExpr; }
    else if(oval2 instanceof Integer)     { val2jar.intVal = ((Integer)oval2).intValue();      val2jar.type = 'I'; val2jar.etype = intExpr; }
    else if(oval2 instanceof Short)       { val2jar.intVal =   ((Short)oval2).intValue();      val2jar.type = 'I'; val2jar.etype = intExpr; }
    else if(oval2 instanceof Byte)        { val2jar.intVal =    ((Byte)oval2).intValue();      val2jar.type = 'I'; val2jar.etype = intExpr; }
    else if(oval2 instanceof Boolean)     { val2jar.boolVal = ((Boolean)oval2).booleanValue(); val2jar.type = 'Z'; val2jar.etype = booleanExpr; }
    else if(oval2 instanceof Double)      { val2jar.doubleVal = ((Double)oval2).doubleValue(); val2jar.type = 'D'; val2jar.etype = doubleExpr; }
    else if(oval2 instanceof Float)       { val2jar.floatVal = ((Float)oval2).floatValue();    val2jar.type = 'F'; val2jar.etype = floatExpr; }
    else if(oval2 instanceof StringSeq)   { val2jar.stringVal = (StringSeq)oval2;              val2jar.type = 't'; val2jar.etype = stringExpr; }
    else                                  {                                                    val2jar.type = 'o'; val2jar.etype = objExpr; }
    
  }
  
  
  
  
  
}