package org.vishia.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
 * </ul>
 * If the expression works with simple variable or constant values, it is fast. For example it is able to use
 * in a higher frequently graphic application to scale values.
 * <br><br>
 * TODO see DataAccess: A value or subroutine can use any java elements which are accessed via reflection mechanism.
 * Write "X * $classpath.Myclass.method(X)" to build an expression which's result depends on the called
 * static method.
 * <br><br>
 * To test the capability and the correctness use {@link org.vishia.util.test.TestCalculatorExpr}. 
 * @author Hartmut Schorrig
 *
 */
public class CalculatorExpr
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-08-18 Hartmut new: CalculatorExpr: now supports unary ( expression in parenthesis ). 
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
   @SuppressWarnings("hiding")
  public final static int version = 20121222;
  
   
   
   
  
  /**A path to any Java Object or method given with identifier names.
   * The access is organized using reflection.
   * <ul>
   * <li>This class can describe a left value. It may be a Container to which a value is added
   * or a {@link java.lang.Appendable}, to which a String is added.  
   * <li>This class can describe a value, which is the result of access to the last element of the path.
   * </ul>
   */
  public static class Datapath
  {
    /**The description of the path to any data if the script-element refers data. It is null if the script element
     * does not refer data. If it is filled, the instances are of type {@link ZbnfDataPathElement}.
     * If it is used in {@link DataAccess}, its base class {@link DataAccess.DatapathElement} are used. The difference
     * are the handling of actual values for method calls. See {@link ZbnfDataPathElement#actualArguments}.
     */
    protected List<DataAccess.DatapathElement> datapath;
    
    public List<DataAccess.DatapathElement> datapath(){ return datapath; }
    
    public void add_datapathElement(DataAccess.DatapathElement item){ 
      if(datapath == null){
        datapath = new ArrayList<DataAccess.DatapathElement>();
      }
      datapath.add(item); 
    }

    
    
  }
   
   
  /**A value, maybe a constant, any given Object or an access description to a java program element.
   * 
   *
   */
  public static class Value { //extends Datapath{
    
    
    
    /**Type of the value. 
     * <ul>
     * <li>J I D F Z: long, double, boolean, the known Java characters for types see {@link java.lang.Class#getName()}
     * <li>o: The oVal contains any object.
     * <li>t: A character sequence stored in stringVal,
     * <li>d: Access via the data path using reflection
     * </ul>
     */
    protected char type = '?';
    protected long longVal;
    protected int intVal;
    protected double doubleVal;
    protected float floatVal;
    protected boolean boolVal;
    protected StringSeq stringVal;
    protected Object oVal;
    
    public Value(long val){ type = 'J'; longVal = val; }
    
    public Value(int val){ type = 'I'; intVal = val; }
    
    public Value(double val){ type = 'D'; doubleVal = val; }
    
    public Value(float val){ type = 'F'; floatVal = val; }
    
    public Value(boolean val){ type = 'Z'; boolVal = val; }
    
    public Value(char val){ type = 'C'; longVal = val; }
    
    public Value(String val){ type = 't'; stringVal = StringSeq.create(val); }
    
    public Value(Appendable val){ type = 'a'; oVal = val; }
    
    public Value(Object val){ type = 'o'; oVal = val; }
    
    //public Value(List<DataPathItem> datpath){ type = 'd'; this.datapath = datapath; }
    
    public Value(){ type = '?'; }
    
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
        case 'Z': return boolVal;
        case 't': return stringVal.length() >0;
        case 'o': return oVal !=null;
        case '?': throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public double doubleValue()
    { switch(type){
        case 'I': return intVal;
        case 'J': return longVal;
        case 'D': return doubleVal;
        case 'Z': return boolVal ? 1.0 : 0;
        case 't': return Double.parseDouble(stringVal.toString());
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': throw new IllegalArgumentException("the type is not determined while operation.");
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
    public StringSeq stringValue(){ 
      switch(type){
        case 'I': return StringSeq.create(Integer.toString(intVal));
        case 'J': return StringSeq.create(Long.toString(longVal));
        case 'D': return StringSeq.create(Double.toString(doubleVal));
        case 'Z': return StringSeq.create(Boolean.toString(boolVal));
        case 't': return stringVal;
        case 'o': return StringSeq.create(oVal ==null ? "null" : oVal.toString());
        case '?': return StringSeq.create("??");
        default:  return StringSeq.create("?" + type);
      }//switch
    }

    public Object objValue(){ 
      switch(type){
        case 'I': return new Integer(intVal);
        case 'J': return new Long(longVal);
        case 'D': return new Double(doubleVal);
        case 'Z': return new Boolean(boolVal);
        case 't': return stringVal;
        case 'o': return oVal;
        default:  return "?" + type;
      }//switch
    }


    
    @Override public String toString(){ 
      switch(type){
        case 'I': return Integer.toString(intVal);
        case 'J': return Long.toString(longVal);
        case 'D': return Double.toString(doubleVal);
        case 'Z': return Boolean.toString(boolVal);
        case 't': return stringVal.toString();
        case 'o': return oVal ==null ? "null" : oVal.toString();
        case '?': return "??";
        default:  return "?" + type;
      }//switch
    }
  }
  
  
  
  /**Common interface for the type of expression.
   */
  private interface ExpressionType{
    abstract char typeChar();
    
    /**Checks the second argument whether it is matching to the current expression type 
     * which matches to the accu.
     * If the val2 provides another type, either it is converted to the current expression type
     * or another (higher) expression type is taken and the accumulator value is converted.
     * @param accu The accumulator maybe changed..
     * @param val2 the second operand is tested, may be changed.
     * @return type of the expression.
     */
    abstract ExpressionType checkArgument(Value accu, Value val2);
  }
  
  
  public abstract static class Operator{
    private final String name; 
    protected Operator(String name){ this.name = name; }
    
    protected abstract ExpressionType operate(ExpressionType Type, Value accu, Value arg);
    
    protected abstract boolean isUnary();
    @Override public String toString(){ return name; }
  }
  
                                                  
  private abstract static class XXXUnaryOperator{
    private final String name; 
    XXXUnaryOperator(String name){ this.name = name; }
    abstract ExpressionType operate(ExpressionType Type, Value val);
    @Override public String toString(){ return name; }
  }
  
  
  
  /**The type of val2 determines the expression type. The accu is not used because it is a set operation. 
   * The accu will be set with the following operation.
   */
  protected static final ExpressionType startExpr = new ExpressionType(){
    
    @Override public char typeChar() { return '!'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'I': return intExpr; 
        case 'J': return longExpr; 
        case 'F': return floatExpr; 
        case 'D': return doubleExpr; 
        case 'Z': return booleanExpr; 
        case 't': return stringExpr; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }

    @Override public String toString(){ return "Type=!"; }

  };
  
  private static final ExpressionType intExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'I'; }
    
    /**The current expression type is int. The accumulator is of type int. 
     * Change the expression type and convert the operands
     * if one of the operand have an abbreviating type.
     * @see org.vishia.util.CalculatorExpr.ExpressionType#checkArgument(org.vishia.util.CalculatorExpr.Value, org.vishia.util.CalculatorExpr.Value, java.lang.Object)
     */
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'I': return this; 
        case 'J': accu.longVal = accu.intVal; return longExpr; 
        case 'F': accu.floatVal = accu.intVal; return floatExpr; 
        case 'D': accu.doubleVal = accu.intVal; return doubleExpr; 
        case 'Z': return booleanExpr; 
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

  private static final ExpressionType longExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'J'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'I': val2.longVal = val2.intVal; return this; 
        case 'J': return this; 
        case 'F': accu.floatVal = accu.longVal; return floatExpr; 
        case 'D': accu.doubleVal = accu.longVal; return doubleExpr; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=J"; }


  };
  
  
  private static final ExpressionType floatExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'J'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'I': val2.floatVal = val2.intVal; return this; 
        case 'J': val2.doubleVal = val2.longVal; return doubleExpr; 
        case 'F': return this; 
        case 'D': accu.doubleVal = accu.floatVal; return doubleExpr; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=J"; }


  };
  
  
  private static final ExpressionType doubleExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'J'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'I': val2.doubleVal = val2.intVal; return this; 
        case 'J': val2.doubleVal = val2.longVal; return this; 
        case 'F': val2.doubleVal = val2.floatVal; return this; 
        case 'D': return this; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=J"; }


  };
  
  
  
  protected static final ExpressionType booleanExpr = new ExpressionType(){

    @Override public char typeChar() { return 'Z'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'I': val2.boolVal = val2.intVal !=0; break;
        case 'J': val2.boolVal = val2.longVal != 0; break;
        case 'F': val2.boolVal = val2.floatVal !=0; break;
        case 'D': val2.boolVal = val2.floatVal !=0; break;
        case 't': val2.boolVal = val2.stringVal !=null && val2.stringVal.length() >0; break;
        case '0': val2.boolVal = val2.oVal !=null; break;
        case 'Z': break; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
      return this;
    }
    @Override public String toString(){ return "Type=Z"; }
  };
  
  
  private static final ExpressionType stringExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 't'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      if(val2.type !='t'){ 
        val2.stringVal = val2.stringValue();
        val2.type = 't';
      }
      return this;
    }

    @Override public String toString(){ return "Type=t"; }


  };
  

  
  
  
  private static final Operator boolOperation = new Operator("bool "){
    @Override public ExpressionType operate(ExpressionType type, Value val, Value val2) {
      val.boolVal = val.booleanValue();
      val.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  private static final Operator boolNotOperation = new Operator("b!"){
    @Override public ExpressionType operate(ExpressionType type, Value val, Value value2) {
      val.boolVal = !val.booleanValue();
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  private static final Operator bitNotOperation = new Operator("~u"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value value2) {
      switch(type.typeChar()){
        case 'B': case 'S': 
        case 'I': accu.intVal = ~accu.intVal; break;
        case 'J': accu.longVal = ~accu.longVal; break;
        //case 'D': accu.doubleVal = accu.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  private static final Operator negOperation = new Operator("-u"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value val2) {
      switch(type.typeChar()){
        case 'B': case 'S': 
        case 'I': accu.intVal = -accu.intVal; break;
        case 'J': accu.longVal = -accu.longVal; break;
        case 'D': accu.doubleVal = -accu.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  private static final Operator setOperation = new Operator("!"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.type = type.typeChar();
      switch(accu.type){
        case 'B': case 'S': 
        case 'I': accu.intVal = arg.intVal; break;
        case 'J': accu.longVal = arg.longVal; break;
        case 'F': accu.floatVal = arg.floatVal; break;
        case 'D': accu.doubleVal = arg.doubleVal; break;
        case 'Z': accu.boolVal = arg.boolVal; break;
        case 't': accu.stringVal = arg.stringVal; break;
        case 'o': accu.oVal = arg.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator addOperation = new Operator("+"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal += arg.intVal; break;
        case 'J': accu.longVal += arg.longVal; break;
        case 'D': accu.doubleVal += arg.doubleVal; break;
        case 'Z': accu.doubleVal += arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator subOperation = new Operator("-"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal -= arg.intVal; break;
        case 'J': accu.longVal -= arg.longVal; break;
        case 'F': case 'D': accu.doubleVal -= arg.doubleVal; break;
        case 'Z': accu.doubleVal -= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator mulOperation = new Operator("*"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal *= arg.intVal; break;
        case 'J': accu.longVal *= arg.longVal; break;
        case 'D': accu.doubleVal *= arg.doubleVal; break;
        case 'Z': accu.doubleVal *= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator divOperation = new Operator("/"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal /= arg.intVal; break;
        case 'J': accu.longVal /= arg.longVal; break;
        case 'D': accu.doubleVal /= arg.doubleVal; break;
        case 'Z': accu.doubleVal /= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator cmpEqOperation = new Operator(".cmp."){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal == arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal == arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) < (Math.abs(accu.doubleVal) / 100000); break;
        case 'Z': accu.boolVal = accu.boolVal == arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.equals(arg.stringVal); break;
        case 'o': accu.boolVal = accu.oVal == null && arg.oVal == null || (accu.oVal !=null && arg.oVal !=null && accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator cmpNeOperation = new Operator("!="){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal != arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal != arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) >= (Math.abs(accu.doubleVal) / 100000); break;
        case 'Z': accu.boolVal = accu.boolVal != arg.boolVal; break;
        case 't': accu.boolVal = !accu.stringVal.equals(arg.stringVal); break;
        case 'o': accu.boolVal = !(accu.oVal == null && arg.oVal == null) || (accu.oVal !=null && arg.oVal !=null && !accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator boolOrOperation = new Operator("||"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() || arg.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator boolAndOperation = new Operator("&&"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() && arg.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator cmpLessThanOperation = new Operator("<"){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal < arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal < arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal < arg.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal && arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) < 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) < 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator cmpGreaterEqualOperation = new Operator(">="){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal >= arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal >= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal >= arg.doubleVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) >= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) >= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator cmpGreaterThanOperation = new Operator(">"){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal > arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal > arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal > arg.doubleVal; break;
        case 'Z': accu.boolVal = accu.boolVal && !arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) > 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) > 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  private static final Operator cmpLessEqualOperation = new Operator("<="){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal <= arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal <= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal <= arg.doubleVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) <= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) <= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   

  
  /**An Operation in the list of operations. It contains the operator and maybe a operand.
   * <ul>
   * <li>The operator operates with the current stack context.
   * <li>An unary operator will be applied to the value firstly. 
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
     
    /**The operation Symbol if it is a primitive. 
     * <ul>
     * <li>! Take the value
     * <li>+ - * / :arithmetic operation
     * <li>& | : bitwise operation
     * <li>A O X: boolean operation and, or, xor 
     * */
    private char operation;
    
    /**The operator for this operation. */
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
    protected DataAccess datapath;
    
    public Operation(){
      this.ixVariable = kArgumentUndefined;
    }
    
    public Operation(Operator op, int ixVariable){
      this.operator = op;
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
      this.value_d = value; this.operator = getOperator(operation); this.ixVariable = kConstant; this.oValue = null; 
    }
    //Operation(char operation, int ixVariable){ this.value_d = 0; this.operation = operation; this.ixVariable = ixVariable; this.oValue = null; }
    //Operation(char operation, Object oValue){ this.value = 0; this.operation = operation; this.ixVariable = -1; this.oValue = oValue; }
    Operation(Operator operator, Object oValue){ 
      this.value_d = 0; this.operator = operator; this.operation = '.'; this.ixVariable = kConstant; this.oValue = oValue; 
    }
  
    
    
    public boolean hasOperator(){ return operator !=null; }
    
    public boolean hasUnaryOperator(){ return unaryOperator !=null || unaryOperators !=null; }
    
    public void add_datapathElement(DataAccess.DatapathElement item){ 
      if(datapath == null){ datapath = new DataAccess();}
      datapath.add_datapathElement(item); 
    }
    
    public void set_intValue(int val){
      if(value == null){ value = new Value(); }
      value.type = 'I';
      value.intVal = val;
    }
    
    
    public void set_textValue(String val){
      if(value == null){ value = new Value(); }
      value.type = 't';
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
      if(op.isUnary()){
        ixVariable = kUnaryOperation;
      }
    }
    
    
    
    public boolean setOperator(String op){
      this.operation = op.charAt(0);
      Operator op1 = operators.get(op);
      if(op1 !=null){ 
        setOperator(op1);
      }
      return this.operator !=null; 
    }
    
    
    
    @Override public String toString(){ 
      StringBuilder u = new StringBuilder();
      u.append(operator);
      if(unaryOperator !=null){ u.append(" ").append(unaryOperator); }
      if(unaryOperators !=null){ u.append(" ").append(unaryOperators); }
      if(ixVariable >=0) u.append(" arg[").append(ixVariable).append("]");
      else if(ixVariable == kStackOperand) u.append(" stack");
      else if(datapath !=null) u.append(datapath.toString());
      else if (oValue !=null) u.append(" oValue:").append(oValue.toString());
      else if (value !=null) u.append(" ").append(value.toString());
      else u.append(" double:").append(value_d);
      return u.toString();
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
   * <li>"!" Set the value to accu, set the accu to the stack.
   * <li>"+" "-" "*" "/" known arithmetic operators
   * <li>">=" "<=" ">" "<" "==" "!=" knwon compare operators
   * <li>"<>" other form of not equal operator
   * <li>"ge" "le" "gt" "lt" "eq" "ne" other form of comparators
   * <li>"||" "&&" known logical opeators
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
      operators.put("!",  setOperation);   //set accu to operand
      operators.put("+",  addOperation);
      operators.put("-",  subOperation);
      operators.put("*",  mulOperation);
      operators.put("/",  divOperation);
      operators.put(">=", cmpGreaterEqualOperation);
      operators.put(">",  cmpGreaterThanOperation);
      operators.put("<=", cmpLessEqualOperation);
      operators.put("<",  cmpLessThanOperation);
      operators.put("!=", cmpNeOperation);
      operators.put("<>", cmpNeOperation);
      operators.put("==", cmpEqOperation);
      operators.put("lt", cmpLessThanOperation);
      operators.put("le", cmpLessEqualOperation);
      operators.put("gt", cmpGreaterThanOperation);
      operators.put("ge", cmpGreaterEqualOperation);
      operators.put("eq", cmpEqOperation);
      operators.put("ne", cmpNeOperation);
      operators.put("||", boolOrOperation);
      operators.put("&&", boolAndOperation);
      operators.put("ub",  boolOperation);   //not for boolean
      operators.put("u!",  boolNotOperation);   //not for boolean
      operators.put("u~",  bitNotOperation);   //not for boolean
      operators.put("u-",  negOperation);   //not for boolean

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
   * @param sExpr String given expression such as "X*(Y-1)+Z"
   * @param sIdentifier List of identifiers for variables.
   * @return null if ok or an error description.
   */
  public String setExpr(String sExpr, String[] sIdentifier)
  {
    this.variables = sIdentifier;
    StringPart sp = new StringPart(sExpr);
    return multExpr(sp, "!", 1);  //TODO addExpr
  }
  
  /**Converts the given expression in a stack operable form. One variable with name "X" will be created.
   * It means the expression can use "X" as variable.
   * @param sExpr For example "5.0*X" or "(X*X+1.5*X)"
   * @see #setExpr(String, String[])
   */
  public String setExpr(String sExpr)
  {
    this.variables = new String[]{"X"};
    StringPart sp = new StringPart(sExpr);
    return addExpr(sp, "!", 1);
  }
  

  /**The outer expression is a add or subtract expression.
   * call recursively for any number of operands.
   * call {@link #multExpr(StringPart, char)} to get the argument values.
   * @param sp
   * @param operation The first operation.
   * @return this
   */
  private String addExpr(StringPart sp, String operation, int recursion)
  { String sError = null;
    if(recursion > 1000) throw new RuntimeException("recursion");
    sError = multExpr(sp, operation, recursion +1);
    if(sError == null && sp.length()>0){
      char cc = sp.getCurrentChar();
      if("+-".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return addExpr(sp, ""+cc, recursion+1);
      }
    }
    return null;
  }
  
  
  /**The more inner expression is a mult or divide expression.
   * call recursively for any number of operands.
   * call functions to get the argument values. 
   * @param sp
   * @param operation
   * @return
   */
  private String multExpr(StringPart sp, String operation, int recursion)
  { if(recursion > 1000) throw new RuntimeException("recursion");
    try{
      if(sp.scanIdentifier().scanOk()){
        String sIdent = sp.getLastScannedString();
        int ix;
        for(ix = 0; ix< variables.length; ++ix){
          if(variables[ix].equals(sIdent)){
            listOperations.add(new Operation(operation, ix));
            ix = Integer.MAX_VALUE-1; //break;
          }
        }
        if(ix != Integer.MAX_VALUE){ //variable not found
          return("unknown variable" + sIdent);
        }
      } else if(sp.scanFloatNumber().scanOk()){
        listOperations.add(new Operation(operation, sp.getLastScannedFloatNumber()));
      }
    }catch(ParseException exc){
      return("ParseException float number"); 
    }
    if(sp.length()>0){
      char cc = sp.getCurrentChar();
      if("*/".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return multExpr(sp, ""+cc, recursion+1);
      }
    }
    return null;  //ok
  }
  

  
  /**Adds the given operation to the list of operations. The list of operations is executed one after another.
   * All calculation rules such as parenthesis, prior of multiplication, functions arguments etc.
   * should be regarded by the user. 
   * @param operation
   */
  public void addOperation(Operation operation){
    listOperations.add(operation);
  }
  
  
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
  
  
  
  /**Calculate with more as one input value.
   * @param input
   * @return
   */
  public double calc(double[] input)
  { double val = 0;
    return val;
  }
  

  
  /**Calculates the expression with only one input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public double calc(double input)
  { double val = 0;
    for(Operation oper: listOperations){
      final double val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = oper.value_d; }
      switch(oper.operation){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  

  
  /**Calculates the expression with only one float input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public float calc(float input)
  { float val = 0;
    for(Operation oper: listOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operation){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  

  
  /**Calculates the expression with only one integer input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public float calc(int input)
  { float val = 0;
    for(Operation oper: listOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operation){
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
   * or {@link #setExpr(String, String[])}.
   * @param args Array of some inputs
   * @return The result of the expression.
   */
  public Value calc(Object... args){
    Value accu = new Value();
    Value val2 = new Value();
    ExpressionType check = startExpr;
    for(Operation oper: listOperations){
      //Get the operand either from args or from Operation
      Object oVal2;
      if(oper.ixVariable >=0){ oVal2 = args[oper.ixVariable]; }  //an input value
      else { oVal2 = oper.oValue; }                              //a constant value inside the Operation
      //
      //Convert the value adequate the given type of expression:
      check = check.checkArgument(accu, val2);    //may change the type.
      //
      //executes the operation:
      check = oper.operator.operate(check, accu, val2);  //operate, may change the type if the operator forces it.
    }
    accu.type = check.typeChar();  //store the result type
    return accu;
  }
  
  

  
  
  
  
  
  
  
  
  
  /**Calculates the expression with possible access to any stored object data and access support via reflection.
   * An value can contain a {@link Datapath#datapath} which describe any class's field or method
   * which were found via a reflection access. The datapath is build calling {@link #setExpr(String)}
   * or {@link #setExpr(String, String[])} using the form "$var.reflectionpath"
   * 
   * @param javaVariables Any data which are access-able with its name. It is the first part of a datapath.
   * @param args Some args given immediately. Often numerical args.
   * @return The result wrapped with a Value instance. This Value contains the type info too. 
   * @throws Exception Any exception is possible. Especially {@link java.lang.NoSuchFieldException} or such
   *   if the access via reflection is done.
   */
  public Value calcDataAccess(Map<String, Object> javaVariables, Object... args) throws Exception{
    accu = new Value(); //empty
    Value val3 = new Value();  //Instance to hold values for the right side operand.
    Value val2; //Reference to the right side operand
    ExpressionType type = startExpr;
    for(Operation oper: listOperations){
      if(accu.type == 'Z' && 
          ( !accu.boolVal && oper.operator == boolAndOperation  //false remain false on and operation
          || accu.boolVal && oper.operator == boolOrOperation   //true remain true on or operation
        ) ){
        //don't get arguments, no side effect (like in Java, C etc.
      } else {
        //Get the operand either from args or from Operation
        Object oval2;
        if(oper.ixVariable >=0){ 
          val2 = val3;
          oval2 = args[oper.ixVariable];
        }  //an input value
        else if(oper.ixVariable == Operation.kStackOperand){
          val2 = accu;
          accu = stack.pop();
          oval2 = null;
        }
        else if(oper.datapath !=null){
          val2 = val3;
          oval2 = oper.datapath.getDataObj(javaVariables, true, false);
        }
        else {
          val2 = oper.value;
          oval2 = null;
        }
        //
        //Convert a Object-wrapped value into its real representation.
        if(oval2 !=null){
          if(oval2 instanceof Long)             { val2.longVal =   ((Long)oval2).longValue(); val2.type = 'J'; }
          else if(oval2 instanceof Integer)     { val2.intVal = ((Integer)oval2).intValue(); val2.type = 'I'; }
          else if(oval2 instanceof Short)       { val2.intVal =   ((Short)oval2).intValue(); val2.type = 'I'; }
          else if(oval2 instanceof Byte)        { val2.intVal =    ((Byte)oval2).intValue(); val2.type = 'I'; }
          else if(oval2 instanceof Boolean)     { val2.boolVal = ((Boolean)oval2).booleanValue(); val2.type = 'Z'; }
          else if(oval2 instanceof Double)      { val2.doubleVal = ((Double)oval2).doubleValue(); val2.type = 'D'; }
          else if(oval2 instanceof Float)       { val2.doubleVal = ((Float)oval2).floatValue(); val2.type = 'F'; }
          else if(oval2 instanceof StringSeq){ val2.stringVal = (StringSeq)oval2; val2.type = 't'; }
          else                                  { val2.oVal = oval2; val2.type = 'L'; }
          val2.oVal = oval2;;
        }
        if(oper.operator == setOperation && accu.type != '?'){
          stack.push(accu);
          accu = new Value();
        }
        //Convert the value adequate the given type of expression:
        if(!oper.operator.isUnary()){  //if unary, don't change the type
          type = type.checkArgument(accu, val2);    //may change the type.
        }
        //
        //executes the operation:
        if(oper.unaryOperator !=null){
          oper.unaryOperator.operate(type, val2, null);   //change the right value
        }
        else if(oper.unaryOperators !=null){
          for(Operator unary: oper.unaryOperators){
            unary.operate(type, val2, null);   //change the right value
          }
        }
        type = oper.operator.operate(type, accu, val2);  //operate, may change the type if the operator forces it.
      }
    }
    return accu;
  }
  
  
  
  
  
}
