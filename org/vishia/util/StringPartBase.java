package org.vishia.util;


/**This is an alternative to the {@link java.lang.String} which uses a shared reference to the char sequence.
 * This class is able to use if String processing is done in a closed thread. This class must not be used 
 * instead java.lang.String if the String would referenced persistently and used from more as one thread.
 * String with this class are not immutable.
 * @author Hartmut Schorrig
 *
 */
/**The StringPart class represents a flexible valid part of a character string which's spread is changeable. 
 * It may be seen as an alternative to the standard {@link java.lang.String} for the capability to build a {@link String#substring(int)}.
 * <ul>
 * <li>1. The substring or Part of the String can be build with some operations, {@link #seek(String, int)}, {@link #lento(String)} etc.
 * <li>2. This class represents a Part of the String which is able to change.
 * <li>3. The operation to build a Part does not build an independent String, but refers inside the given String.
 * <li>4. The Part is able to build from any CharSequence, especially from a StringBuilder or from any char[]-Array.
 * </ul>
 * <b>Calculation time and memory effect</b>:<br>
 * The 3. minute affects the calculation time for extensive using of parts of a String. The {@link String#substring(int)} method
 * of standard Java till Version 6 builds a substring using and references the stored parent String. It was a cheap operation 
 * in calculation time. 
 * <br><br>
 * In Java version 7 this behavior was changed. Up to version 7 a substring builds an new buffer for the substring
 * in the heap. The advantage is: If a long String exists firstly, then some substrings are build, and the firstly long String
 * is not used anymore, the memory of the long String can garbaged now. The application does not need yet memory for the originally long String,
 * only the typical short substrings are stored in the heap. For applications, which builds some short substrings from a
 * long parent String, it saves memory.
 * <br><br>
 * But if substrings are need extensively from one long String, to search somewhat etc, The creation of new memory for any substring
 * may be an expensive operation. This class works with the given String, builds parts of the string with indices, 
 * and does not need memory for any sub part.
 * <br><br>
 * 
 * 
 * <b>Multithreading, persistence of Strings</b>:<br>
 * A StringPart depends of its parent CharSequence. That CharSequence maybe a String, which is persistent. But that CharSequence
 * maybe a StringBuilder or any other volatile storage. Changing the CharSequence my disturb operations of the StringPart.
 * Therefore the parent CharSequence should be notice. Is it changed? 
 * <br><br>
 * If a Part should be stored persistently, one can use a {@link #toString()} method of any returned CharSequence
 * for example {@link #getCurrentPart()}.toString(). This builds a persistent String which can be stored and used independent of all others.
 * <br><br>
 * But if the Part of String is used in the same thread, not stored, and another thread does not disturb the content of the 
 * StringPart's parent CharSequence (which may be usual), the waiver to build a persistent String may save a little bit of calculation time.
 * A method which accepts a {@link java.lang.CharSequence} as parameter should not store that in suggestion of persistence. 
 * For example {@link StringBuilder#append(CharSequence)} uses a non-persistent character sequence and adds it to its own buffer.
 * <br><br>
 * 
 *  
 * <b>Principles of operation</b>:<br>
 * The StringPart class is associated to any CharSequence. Additionally 4 Parameters determine the actual part of the String
 * and the limits of changing of the actual part. The followed image is used to explain the spread of a part:
 * <pre>
 * abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
 * =====================     The === indicates the maximal part
 *   -----------             The --- indicates the valid part before some operation
 *         +++++             The +++ indicates the valid part after some operation
 * </pre> 
 * The actual part of the string is changeable, without building a new substring. 
 * So some operations of seeking and scanning are offered. 
 * <br><br>
 * <b>Types of Methods</b>:<br>
 * <ul>
 * <li>assign: assigns a new parent string: {@link #assign(CharSequence)}, like constructor
 * <li>seek: changes the start position of the actual (current) string part, do not change the end of the actual part,
 *   from there, seek changes the length. Seek returns this, so concatenation of method calls is possible.
 *   <ul>
 *   <li>{@link #seek(int)}: Seek with given number of chars, for example seek(1) to skip over one character
 *   <li>{@link #seek(char, int)}, {@link #seek(String, int)}: Searches any char or String
 *   <li>{@link #seekAnyString(String[], int[])}: Searches any of some given String.
 *   <li>{@link #seekNoWhitespace()}, {@link #seekNoWhitespaceOrComments()}: skip over all white spaces, maybe over comments
 *   <li>{@link #seekBegin()} Expands the spread starting from the most left position
 *   </ul>  
 * <li>lento: changes the end of the actual string part.
 *   <ul>
 *   <li>{@link #lento(int)}: set a length of the valid part
 *   <li>{@link #lento(char)}, {@link #lento(CharSequence, int)}: length till a end character or end string
 *   <li>{@link #lentoAnyChar(String, int)}, {@link #lentoAnyString(String[], int)}: length till one of some given end characters or Strings
 *   <li>{@link #lentoAnyCharOutsideQuotion(String, int): regards String in quotation as non-applying.
 *   <li>#lentoAnyNonEscapedChar(String, int): regards characters after a special char as non-applying.
 *   <li>#lentoAnyStringWithIndent(String[], String, int, StringBuilder): regards indentation typically for source files.
 *   <li>#lentoIdentifier(), #lentoIdentifier(String, String): accepts identifier
 *   </ul>
 * <li>get: Gets an content without changing.
 *   <ul>
 *   <li>#getCurrentPart(): The valid part as CharSequence, use toString() to transform to a persistent String.
 *   <li>#getCurrent(int): Requested number of chars from start of the current part, for tests and debugging.
 *   <li>#getLastPart(): Last valid part before the last seek or scan.
 *   </ul>
 * <li>indexOf: search any one in the valid part.
 *   <ul>
 *   <li>#indexEndOfQuotion(char, int, int) etc.
 *   </ul>
 * </ul>            
 */

public class StringPartBase implements CharSequence, Comparable<CharSequence>
{
  /**Version, history and license.
   * <ul>
   * <li>2013-10-26 Hartmut chg: Does not use substring yet, some gardening, renaming. 
   * <li>2013-09-07 Hartmut new: {@link #scanTranscriptionToAnyChar(CharSequence[], String, char, char, char)}
   *   the {@link #getCircumScriptionToAnyChar(String)} does not work correctly (it has a bug). Use the new one.
   * <li>2013-01-20 Hartmut TODO: The {@link #content} should be a CharSequence. Then the instance of content may be a StringBuilder.
   *   All content.substring should be replaced by content.subsequence(). The content.indexof-Method should be implemented here.
   *   Advantage: A derived class can use the {@link #content} as StringBuilder and it can shift the string by operating with
   *   large contents. Note that a origin position should be used then. This class can contain and regard a origin position,
   *   which is =0 in this class. See {@link StringPartFromFileLines}. That class doesn't regard a less buffer yet, but it should do so. 
   * <li>2013-01-19 Hartmut new: {@link #getPart(int, int)}
   * <li>2012-02-19 Hartmut new: {@link #assignReplaceEnv(StringBuilder)}
   * <li>2011-10-10 Hartmut new: {@link #scanFloatNumber(boolean)}. It should be possible to scan a float with clearing the buffer. Using in ZbnfParser.
   * <li>1011-07-18 Hartmut bugfix: some checks of length in {@link #scanFloatNumber()}. If the String contains only the number digits,
   *                an IndexOutOfBounds-exception was thrown because the end of the String was reached. 
   * <li>2009-03-16 Hartmut new: scanStart() returns this, not void. Useable in concatenation.
   * <li>2007-05-08 JcHartmut  change: seekAnyChar(String,int[]) renamed to {@link seekAnyString(String,int[])} because it was an erroneous identifier. 
   * <li>2007-05-08 JcHartmut  new: {@link lastIndexOfAnyChar(String,int,int)}
   * <li>2007-05-08 JcHartmut  new: {@link lentoAnyChar(String, int, int)}
   *                           it should programmed consequently for all indexOf and lento methods.
   * <li>2007-04-00 JcHartmut  some changes, not noted.
   * <li>2004-01-00 JcHartmut  initial revision The idea of such functionality was created in th 1990th in C++ language.
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
   */
  public final static int version = 20130504; 

  
  /** Flag to force setting the start position after the seeking string. See description on seek(String, int).
   */
   public static final int seekEnd = 1;

   /** Flag bit to force seeking backward. This value is contens impilicit in the mSeekBackFromStart or ~End,
       using to detect internal the backward mode.
   */
   private static final int mSeekBackward_ = 0x10;

   /** Flag bit to force seeking left from start (Backward). This value is contens impilicit in the seekBackFromStart
       using to detect internal the seekBackFromStart-mode.
   */
   private static final int mSeekToLeft_ = 0x40;

   /** Flag to force seeking backward from the start position. See description on seek(String).
   */
   public static final int seekToLeft = mSeekToLeft_ + mSeekBackward_;


   /** Flag to force seeking backward from the end position. See description on seek(String).
   */
   public static final int seekBack = 0x20 + mSeekBackward_;

   /** Flag to force seeking forward. See description on seek(String).
   */
   public static final int seekNormal = 0;


   /** Bit in mode. If this bit ist set, all whitespace are overreaded
    * before calling any scan method.
    */
   protected static final int mSkipOverWhitespace_mode = 0x1;

   /** Bit in mode. If this bit ist set, all comments are overreaded
    * before calling any scan method.
    */
   protected static final int mSkipOverCommentInsideText_mode = 0x2;

   /** Bit in mode. If this bit ist set, all comments are overreaded
    * before calling any scan method.
    */
   protected static final int mSkipOverCommentToEol_mode = 0x4;

   /** The string defined the start of comment inside a text.*/
   String sCommentStart = "/*";
   
   /** The string defined the end of comment inside a text.*/
   String sCommentEnd = "*/";
   
   /** The string defined the start of comment to end of line*/
   String sCommentToEol = "//";
   


  
  /**The characters in the view. This array can be large. For example to pares a file with 10 mBytes length,
   * it may be 10 MBytes. 
   * 
   */
  //protected char[] chars;
  
  /**Helper to access {@link #chars} with CharSequence reference. 
   * Especially used in {@link StringFunctions}. */
  protected CharSequence schars = new CharSequence(){
    @Override public char charAt(int index){ return schars.charAt(index); }
    @Override public int length(){ return schars.length(); }
    @Override public CharSequence subSequence(int start, int end)
    { return new Part(start + apos, end + apos);
    }
  };
  
  /**The absolute position which is located to the {@link #chars}[0].
   * It is possible that {@link #chars} presents only a view of the whole characters.
   */
  protected int apos;
  
  /** The actual start position of the valid part.*/
  protected int begin;
  /** The actual exclusive end position of the valid part.*/
  protected int end;

  
  /** True if the last operation of lento__(), seek etc. has found anything. See {@link #found()}. */
  boolean bFound = true;
  
  
  /**false if current scanning is not match*/
  protected boolean bCurrentOk = true;
  
  /**If true, than all idxLastScanned... are set to 0, 
   * it is after {@link #scanOk()} or after {@link #scanStart}
   */ 
  protected boolean bStartScan = true;

  
  
  /**The most left possible start position. We speak about the 'maximal Part':
    * The actual valid part can not exceed the borders startMin and endMax of the maximal part after any operation.
    * The content of the associated string outside the maximal part is unconsidered. The atrributes startMin and endMax
    * are not set by any operations except for the constructors and the set()-methods.
      <br/>Set to 0 if constructed from a string,
      determined by the actual start if constructed from a StringPart.
      <hr/><u>In the explanation of the methods the following notation is used as samples:</u><pre>
abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
  =====================     The === indicates the maximal part
    -----------             The --- indicates the valid part before operation
               ++++++++     The +++ indicates the valid part after operation
      </pre>
  */
  protected int begiMin;

  /** Borders of the last part before calling of scan__(), seek__(), lento__(). If there are different to the current part,
   * the call of restoreLastPart use this values. scanOk() sets the startLast-variable to the actual start or rewinds
   * the actual start ot startLast.
   */
  protected int beginLast, endLast;



  /** The rightest possible exclusive end position. See explanation on startMin.
      <br/>Set to content.length() if constructed from a string,
      determined by the actual end if constructed from a StringPart*/
  protected int endMax;

  
  /** Some mode bits */
  protected int bitMode = 0;
  

  
  /**Implementor of the shifter capability in a derived class. May be null if shifting is not supported. */
  protected StringPartShift shifter;

  
  /** The char used to code start of text. */  
  public static final char cStartOfText = (char)(0x2);
  
  /** The char used to code end of text. */  
  public static final char cEndOfText = (char)(0x3);


  
  
  /**Constructs with a given CharSequence, especially with a given String.
   * @param src Any CharSequence or String
   */
  public StringPartBase(CharSequence src){
    this(src, 0, src.length());
  }
  
  /**Builds a StringPart which uses the designated part of the given src.
   * @param src It will be referenced.
   * @param start The beginMin and begin value for the StringPart.
   * @param end The end and endMax value for the StringPart.
   */
  public StringPartBase(CharSequence src, int start, int end){
    this.apos = 0;
    this.begiMin = this.begin = start;
    this.endMax = this.end = end;
    schars = src;
  }
  
  
  
  /** Sets the content to the given string, forgets the old content. Initialy the whole string is valid.
  @java2c=return-this.
  @param content The content.
  @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase assign(CharSequence content)
  { 
    begiMin = beginLast = begin = 0;
    endMax = end = endLast = content.length();
    bStartScan = bCurrentOk = true;
    schars = content;
    return this;
  }


  /** Sets the start of the maximal part to the actual start of the valid part.
    See also seekBegin(), that is the opposite operation.
    <hr/><u>example:</u><pre>
  abcdefghijklmnopqrstuvwxyz  The associated String
  ================        The maximal part before operation
       ------             The actual part
       ===========        The maximal part after operation
    </pre>
    @java2c=return-this.
    @param src The given StringPart.
    @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase setBeginMaxPart()
  { begiMin = begin;
    return this;
  }






  /** Sets the start of the part to the exclusively end, set the end to the end of the content.
    <hr/><u>example:</u><pre>
  abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
       -----              The valid part before
            +++++         The valid part after.
    </pre>
    @java2c=return-this.
    @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase fromEnd()
  {
    beginLast = begin;
    endLast = end;
    begin = end;
    end = endMax;
    return this;
  }

  
  
  /** get the Line ct. Note: it returns null in this class, may be overridden.
  @return Number of last read line.
*/
public int getLineCt(){ return 0; }


  
  /** Compares the Part of string with the given string
   */
   public boolean equals(CharSequence sCmp)
   { return StringFunctions.equals(schars, begin, end, sCmp); //content.substring(start, end).equals(sCmp);
   }




   /**compares the Part of string with the given string.
    * new since 2008-09: if sCmp contains a cEndOfText char (coded with \e), the end of text is tested.
    * @param sCmp The text to compare.
   */
   public boolean startsWith(CharSequence sCmp)
   { int pos_cEndOfText = StringFunctions.indexOf(sCmp, cEndOfText, 0); //sCmp.indexOf(cEndOfText);
     
     if(pos_cEndOfText >=0)
     { if(pos_cEndOfText ==0)
       { return begin == end;
       }
       else
       { return StringFunctions.equals(schars, begin, end, sCmp); //content.substring(start, end).equals(sCmp);
       }
       
     }
     else
     { return StringFunctions.startsWith(schars, begin, end, sCmp); //content.substring(start, end).startsWith(sCmp);
     }
   }



  
  /* (non-Javadoc)
   * @see java.lang.CharSequence#length()
   */
  @Override public int length(){ return end - begin; }

  /**Returns the lenght of the maximal part from current position. Returns also 0 if no string is valid.
     @return number of chars from current position to end of maximal part.
   */
  public int lengthMaxPart()
  { if(endMax > begin) return endMax - begin;
    else return 0;
  }

  
  
  
  /** Sets the endposition of the part of string to the given chars after start.
    @java2c=return-this.
    @param len The new length. It must be positive.
    @return <code>this</code> to concat some operations.
    @throws IndexOutOfBoundsException if the len is negativ or greater than the position endMax.
   */
  public StringPartBase lento(int len)
  throws IndexOutOfBoundsException
  { endLast = end;
    int endNew = begin + len;
    if(endNew < begin)  /**@java2c=StringBuilderInThreadCxt.*/ throwIndexOutOfBoundsException("lento(int) negative:" + (endNew - begin));
    if(endNew > endMax) /**@java2c=StringBuilderInThreadCxt.*/ throwIndexOutOfBoundsException("lento(int) after endMax:" + (endNew - endMax));
    end = endNew;
    return this;
  }





  
  /** Sets the end position of the part of string to exclusively the char cc.
  If the char cc is not found, the end position is set to start position, so the part of string is empty.
  It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
  That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
  of the last part.
  <hr/><u>example:</u><pre>
  abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part of src
       -----              The valid part of src before calling the method
       +                  after calling lento('w') the end is set to start
                          position, the length() is 0, because the 'w' is outside.
       ++++++++++         calling set0end() is possible and produce this result.
    </pre>
    @java2c=return-this.
    @param cc char to determine the exclusively end char.
    @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
      Sets {@link #bFound} to false if the end char is not found.
  */
  public StringPartBase lento(char cc)
  { endLast = end;
    end = begin-1;
    while(++end < endLast){
      if(schars.charAt(end) == cc) { bFound = true; return this; }
    }
    end = begin;  //not found
    bFound = false;
    return this;
  }

  
  
  /** Sets the endposition of the part of string to exclusively the given string.
      If the string is not found, the end position is set to start position, so the part of string is emtpy.
      It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
      That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
      of the last part, example see lento(char cc)
      @java2c=return-this.
      @param ss string to determine the exclusively end char.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
    */
  public StringPartBase lento(String ss)
  { return lento(ss, seekNormal);
  }



  /** Sets the endposition of the part of string to exclusively the given string.
    If the string is not found, the end position is set to start position, so the part of string is emtpy.
    It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
    That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
    of the last part, example see lento(char cc)
    @java2c=return-this.
    @param ss string to determine the exclusively end char.
    @param mode Mode of seeking the end, seekEnd or 0 is possible.
    @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase lento(CharSequence ss, int mode)
  { endLast = end;
    int pos = StringFunctions.indexOf(schars, begin, end, ss);
    bFound = (pos >=0);
    if(pos >= 0) { end = pos; 
                   if((mode & seekEnd) != 0){ end += ss.length();}
                 }
    else         { end = begin; }
    return this;
  }




  /**Sets the endposition of the part of string to the end of the identifier which is beginning on start.
   * If the part starts not with a identifier char, the end is set to the start position.
   * <hr/><u>example:</u><pre>
    abcd  this is a part uvwxyz The associated String
    =====================     The border of valid parts of src
       -------              The valid part of the src before calling the method
       +++                  after calling lentoIdentifier(). The start position
                            is not effected. That's why the identifier-part is only "his".
      </pre>
      @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
    */
  public StringPartBase lentoIdentifier()
  {
    return lentoIdentifier(null, null);
  }


  /** Sets the endposition of the part of string to the end of the identifier which is beginning on start.
   *  If the part starts not with a identifier char, the end is set to the start position.
   *  @see lentoIdentifier().
   *  @java2c=return-this.
   *  @param additionalChars String of additinal chars there are also accept
   *         as identifier chars. 
   */
  public StringPartBase lentoIdentifier(String additionalStartChars, String additionalChars)
  { endLast = end;
    end = begin;
    if(end >= endMax){ bFound = false; }
    else
      
    { //TODO use StringFunctions.lenIdentifier
      char cc = schars.charAt(end);
      if(   cc == '_' 
        || (cc >= 'A' && cc <='Z') 
        || (cc >= 'a' && cc <='z') 
        || (additionalStartChars != null && additionalStartChars.indexOf(cc)>=0)
        )
      { end +=1;
        while(  end < endMax 
             && (  (cc = schars.charAt(end)) == '_' 
                || (cc >= '0' && cc <='9') 
                || (cc >= 'A' && cc <='Z') 
                || (cc >= 'a' && cc <='z') 
                || (additionalChars != null && additionalChars.indexOf(cc)>=0)
             )  )
        { end +=1; }
      }  
      bFound = (end > begin);
    }
    return this;
  }


  /** Sets the len to the first position of any given char, but not if the char is escaped.
   *  'Escaped' means, a \ is disposed before the char.
   *  Example: lentoAnyNonEscapedChar("\"") ends not at a \", but at ".
   *  it detects the string "this is a \"quotion\"!".
   *  <br>
   *  This method doesn't any things, if the last scanning call isn't match. Invoking of 
   *  {@link scanOk()} before guarantees that the method works.
   *  @java2c=return-this.
   *  @param sCharsEnd Assembling of chars determine the end of the part.  
   * */
  public StringPartBase lentoAnyNonEscapedChar(String sCharsEnd, int maxToTest)
  { if(bCurrentOk)
    { final char cEscape = '\\';
      endLast = end;
      int pos = indexOfAnyChar(sCharsEnd,0,maxToTest);
      while(pos > begin+1 && schars.charAt(pos-1)==cEscape)
      { //the escape char is before immediately. It means, the end char is not matched.
        pos = indexOfAnyChar(sCharsEnd, pos+1-begin, maxToTest);
      }
      if(pos < 0){ end = begin; bFound = false; }
      else       { end = begin + pos; bFound = true; }
    }  
    return this;
  }

  
  
  
  /**Sets the length of the valid part to the first position of the given String, 
   * but not if the String is escaped.
   * 'Escaped' means, a \ is disposed before the char.
   * Example: lentoNonEscapedString("<?") does not accept "\\<?".
   * <br><br>
   * This method doesn't any things, if the last scanning call isn't match. Invoking of 
   * {@link scanOk()} before guarantees that the method works.
   * @java2c=return-this.
   * @param sCharsEnd Assembling of chars determine the end of the part.  
   */
  public StringPartBase lentoNonEscapedString(String sEnd, int maxToTest)
  { if(bCurrentOk)
    { final char cEscape = '\\';
      endLast = end;
      int pos = indexOf(sEnd,0,maxToTest);
      while(pos > begin+1 && schars.charAt(pos-1)==cEscape)
      { //the escape char is before immediately. It means, the end char is not matched.
        pos = indexOf(sEnd, pos+1-begin, maxToTest);
      }
      if(pos < 0){ end = begin; bFound = false; }
      else       { end = begin + pos; bFound = true; }
    }  
    return this;
  }

  
  
  
  /**Sets the length to the end of the maximal part if the length is 0. This method could be called at example
  if a end char is not detected and for that reason the part is valid to the end.
   * @java2c=return-this.
   @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   */
  public StringPartBase len0end()
  { if(end <= begin) end = endMax;
    return this;
  }
  
  
  

  
  /** Displaces the start of the part for some chars to left or to right.
  If the seek operation would exceeds the maximal part borders, a StringIndexOutOfBoundsException is thrown.

  <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
=================         The maximal part
     -----              The valid part before
   +++++++              The valid part after calling seek(-2).
       +++              The valid part after calling seek(2).
         +              The valid part after calling seek(5).
                        The start is set to end, the lenght() is 0.
++++++++++++              The valid part after calling seek(-5).
  </pre>
   *  @java2c=return-this.
  @param nr of positions to displace. Negative: Displace to left.
  @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   */
  public StringPartBase seek(int nr)
  { beginLast = begin;
    begin += nr;
    if(begin > end)
      /**@java2c=StringBuilderInThreadCxt.*/ 
      throwIndexOutOfBoundsException("seek=" + nr + " begin=" + (begin-nr) + " end=" + end);
    else if(begin < begiMin) 
      /**@java2c=StringBuilderInThreadCxt.*/
      throwIndexOutOfBoundsException("seek=" + nr + " begin=" + (begin-nr) + " begin-min=" + begiMin);
    bFound = true;
    return this;
  }






/** Displaces the start of the part to the first char it is no whitespace.
  If the current char at seek position is not a whitespace, the method has no effect.
  If only whitespaces are founded to the end of actual part, the position is set to this end.

  <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
=================         The maximal part
----------              The valid part before
   +++++++              The valid part after, if 'defg' are whitespaces
++++++++++              The valid part after is the same as before, if no whitespace at current position
         .              The valid part after is emtpy, if only whitespaces re found.
  </pre>
*  @java2c=return-this.
  @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
*/
public StringPartBase seekNoWhitespace()
{ beginLast = begin;
while( begin < end && " \t\r\n\f".indexOf(schars.charAt(begin)) >=0 )
{ begin +=1;
}
bFound = (begin > beginLast);
return this;
}


/*=================================================================================================================*/
/*=================================================================================================================*/
/*=================================================================================================================*/
/** skip over comment and whitespaces
*/

/**@deprecated see {@link seekNoWhitespaceOrComments()}
*  @java2c=return-this.
* 
*/ 
@Deprecated
protected StringPartBase skipWhitespaceAndComment()
{ return seekNoWhitespaceOrComments();
}


/** Displaces the begin of the part to the first char it is no whitespace or comment.
  If the current char at seek position is not a whitespace or not the beginning of a comment, the method has no effect.
  If only whitespaces and comments are found to the end of actual part, the position is set to its end.

  <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
=================         The maximal part
----------              The valid part before
   +++++++              The valid part after, if 'defg' are whitespaces
++++++++++              The valid part after is the same as before, if no whitespace at current position
         .              The valid part after is emtpy, if only whitespaces re found.
  </pre>
  @java2c=return-this.
  @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
*/
public StringPartBase seekNoWhitespaceOrComments()
{ int start00 = begin;
int start0;
do
{ start0 = begin;
  if( (bitMode & mSkipOverWhitespace_mode) != 0)
  { seekNoWhitespace();
  }
  if( (bitMode & mSkipOverCommentInsideText_mode) != 0)   
  { if(StringFunctions.compare(schars, begin, sCommentStart, 0, sCommentStart.length())==0) 
    { seek(sCommentEnd, seekEnd);  
    }
  }
  if( (bitMode & mSkipOverCommentToEol_mode) != 0)   
  { if(StringFunctions.compare(schars, begin, sCommentToEol, 0, sCommentToEol.length())==0)
    { seek('\n', seekEnd);  
    }
  }
}while(begin != start0);  //:TRICKY: if something is done, repeat all conditions.
bFound = (begin > start00);
return this;
}

/** Returns true, if the last called seek__(), lento__() or skipWhitespaceAndComment()
* operation founds the requested condition. This methods posits the current Part in a appropriate manner
* if the seek or lento-conditions were not prosperous. In this kinds this method returns false.
* @return true if the last seek__(), lento__() or skipWhitespaceAndComment()
* operation matches the condition.
*/
public boolean found()
{ return bFound;
}



/** Displaces the begin of the part to the leftest possible begin.
  <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
=================         The maximal part
     -----              The valid part before
++++++++++++              The valid part after calling seekBegin().
  </pre>
*  @java2c=return-this.
  @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
*/
protected StringPartBase seekBegin()
{ begin = beginLast = begiMin;
return this;
}










  /** Searchs the given String inside the valid part, posits the begin of the part to the begin of the searched string.
    The end of the part is not affected.
    If the string is not found, the begin is posit on the actual end. The length()-method supplies 0.
    Methods such fromEnd() are not interacted from the result of the searching.
    The rule is: seek()-methods only shifts the begin position.
  
    <hr/><u>example:</u><pre>
that is a liststring and his part The associated String
=============================   The maximal part
  ----------------------      The valid part before
       +++++++++++++++++      The valid part after seek("is",StringPartBase.seekNormal).
         +++++++++++++++      The valid part after seek("is",StringPartBase.seekEnd).
                      ++      The valid part after seek("is",StringPartBase.back).
                       .      The valid part after seek("is",StringPartBase.back + StringPartBase.seekEnd).
 +++++++++++++++++++++++      The valid part after seek("is",StringPartBase.seekToLeft).
   +++++++++++++++++++++      The valid part after seek("is",StringPartBase.seekToLeft + StringPartBase.seekEnd).
++++++++++++++++++++++++++      The valid part after seek("xx",StringPartBase.seekToLeft).
                       .      The valid part after seek("xx",StringPartBase.seekNormal)
                              or seek("xx",StringPartBase.back).

  </pre>
  *  @java2c=return-this.
    @param sSeek The string to search for.
    @param mode Mode of seeking, use ones of back, seekToLeft, seekNormal, added with seekEnd.
    @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase seek(String sSeek, int mode){ 
    beginLast = begin;
    int seekArea1, seekArea9;
    //String sSeekArea;
    int posNotFound;  //position if not found in dependence of area of seek and direction
    if( (mode & mSeekToLeft_) == mSeekToLeft_) { 
      int posAreaEnd = begin + sSeek.length() -1;  //the sSeek-string may be begin at (begin-1)
      if(posAreaEnd > endMax) posAreaEnd = endMax;  //but not over the end.
      seekArea1 = begiMin;
      seekArea9 = posAreaEnd;
      //sSeekArea = content.substring(startMin, posAreaEnd );
      posNotFound = begin; //if not found, the rightest position of area
    }
    else { 
      seekArea1 = begin;
      seekArea9 = end;
      //sSeekArea = content.substring(begin, end );
      posNotFound = end; //if not found, the rightest position of area
    }
    
    int pos;
    if( (mode & mSeekBackward_) == mSeekBackward_) { 
      pos = StringFunctions.lastIndexOf(schars, seekArea1, seekArea9, sSeek); //sSeekArea.lastIndexOf(sSeek);
    }
    else { 
      pos = StringFunctions.indexOf(schars, seekArea1, seekArea9, sSeek);
    }
    
    if(pos < 0) { 
      begin = posNotFound;
      bFound = false;   
    } else { 
      bFound = true;
      begin = pos;
      if( (mode & seekEnd) == seekEnd ) { begin += sSeek.length();
      }
    }
    
    return this;
  }



/** Searchs the given String inside the valid part, posits the begin of the part to the begin of the searched string.
*  The end of the part is not affected.<br>
*  If the string is not found, the begin is posit to the actual end. The length()-method supplies 0.
  Methods such fromEnd() are not interacted from the result of the searching.
  The rule is: seek()-methods only shifts the begin position.<br>
  see {@link seek(String sSeek, int mode)}
* @java2c=return-this.
 @param strings List of String contains the strings to search.
* @param nrofFoundString If given, [0] is set with the number of the found String in listStrings, 
*                        count from 0. This array reference may be null, then unused.
* @return this.       
*/  
public StringPartBase seekAnyString(String[] strings, int[] nrofFoundString)
//public StringPartBase seekAnyString(List<String> strings, int[] nrofFoundString)
{ beginLast = begin;
int pos;
pos = indexOfAnyString(strings, 0, Integer.MAX_VALUE, nrofFoundString, null);
if(pos < 0)
{ bFound = false;   
  begin = end;
}
else
{ bFound = true;
  begin = begin + pos;
}
return this;
}








  /** Searchs the given character inside the valid part, posits the begin of the part to the begin of the searched char.
    The end of the part is not affected.
    If the string is not found, the begin is posit on the actual end
    or, if mode contents seekBack, the begin of the maximal part. 
    In this cases isFound() returns false and a call of restoreLastPart() restores the old parts.
    The length()-method supplies 0.
    Methods such fromEnd() are not interacted from the result of the searching.
    The rule is: seek()-methods only shifts the begin position.<br/>
    The examples are adequate to seek(String, int mode);
  
  *  @java2c=return-this.
    @param cSeek The character to search for.
    @param mode Mode of seeking, use ones of back, seekToLeft, seekNormal, added with seekEnd.
    @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase seek(char cSeek, int mode)
  { beginLast = begin;
    int seekArea1, seekArea9;
    //String sSeekArea;
    int posNotFound;  //position if not found in dependence of area of seek and direction
    if( (mode & mSeekToLeft_) == mSeekToLeft_)
    { int posAreaEnd = begin;  //the sSeek-string may be begin at (begin-1)
      if(posAreaEnd > endMax) posAreaEnd = endMax;  //but not over the end.
      seekArea1 = begiMin;
      seekArea9 = posAreaEnd;
      //sSeekArea = content.substring(startMin, posAreaEnd );
      posNotFound = begin; //if not found, the rightest position of area
    }
    else
    { seekArea1 = begin;
      seekArea9 = end;
      //sSeekArea = content.substring(begin, end );
      posNotFound = end; //if not found, the rightest position of area
    }
    int pos;
    if( (mode & mSeekBackward_) == mSeekBackward_){
      pos = StringFunctions.lastIndexOf(schars, seekArea1, seekArea9, cSeek); 
    }
    else {                                         
      pos = StringFunctions.indexOf(schars, seekArea1, seekArea9, cSeek);
    }
    
    if(pos < 0)
    { begin = posNotFound;
      bFound = false;   
    }
    else
    { bFound = true;
      begin = pos;
      if( (mode & seekEnd) == seekEnd )
      { begin += 1;
      }
    }
    
    return this;
  }
  


  /** Returns the position of the char within the part,
   * returns -1 if the char is not found in the part.
     The methode is likely String.indexOf().
    @param ch character to find.
    @return position of the char within the part or -1 if not found within the part.
    @exception The method throws no IndexOutOfBoundaryException.
    It is the same behavior like String.indexOf(char, int fromEnd).
  */
  public int indexOf(char ch)
  { int pos = StringFunctions.indexOf(schars, begin, end, ch);;
    if(pos < 0) return -1;
    else return pos - begin;
  }

  /** Returns the position of the char within the part, started inside the part with fromIndex,
   * returns -1 if the char is not found in the part started from 'fromIndex'.
     The method is likely String.indexOf().
    @param ch character to find.
    @param fromIndex start of search within the part.
    @return position of the char within the part or -1 if not found within the part.
    @exception The method throws no IndexOutOfBoundaryException. If the value of fromIndex
    is negative or greater than the end position, -1 is returned (means: not found).
    It is the same behavior like String.indexOf(char, int fromEnd).
  */
  public int indexOf(char ch, int fromIndex)
  { if(fromIndex >= (end - begin) || fromIndex < 0) return -1;
    else
    { int pos = StringFunctions.indexOf(schars, begin + fromIndex, end, ch);;
      if(pos < 0) return -1;
      else return pos - begin + fromIndex;
    }
  }


  /** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
      Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
         abcdefgabcdefghijk
  part:   =============  </pre>
    @param sCmp string to find
    @return position of the string within the part or -1 if not found within the part.
  */
  public int indexOf(CharSequence sCmp)
  { int pos = StringFunctions.indexOf(schars, begin, end, sCmp);  //content.substring(begin, end).indexOf(sCmp);
    if(pos < 0) return -1;
    else return pos - begin;
  }




  /** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
      Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
         abcdefgabcdefghijk
  part:   =============  </pre>
    @param sCmp string to find
    @return position of the string within the part or -1 if not found within the part.
  */
  public int indexOf(CharSequence sCmp, int fromIndex, int maxToTest)
  { int max = (end - begin) < maxToTest ? end : begin + maxToTest;
    if(fromIndex >= (max - begin) || fromIndex < 0) return -1;
    else
    { int pos = StringFunctions.indexOf(schars, begin + fromIndex, max, sCmp); //content.substring(begin + fromIndex, max).indexOf(sCmp);
      if(pos < 0) return -1;
      else return pos - begin + fromIndex;
    }
  }



 
  
  public int indexOfAnyChar(String sChars, final int fromWhere, final int maxToTest)
  {
    int pos = begin + fromWhere;
    int max = (end - pos) < maxToTest ? end : pos + maxToTest;
    char cc;
    while(pos < max && sChars.indexOf(cc = schars.charAt(pos)) < 0){  //end char not found:
      pos += 1;
    }
    int nChars = pos - begin;
    if(pos < max 
      || (pos == max && sChars.indexOf(cEndOfText) >= 0)
      )
    { nChars = pos - begin;
    }
    else { nChars = -1; }
    return nChars;
  }
  
  /**Returns the position of one of the chars in sChars within the part, started inside the part with fromIndex,
  returns -1 if the char is not found in the part started from 'fromIndex'.
 @param sChars contents some chars to find. If it contains the char with code {@link #cEndOfText}
   then the number of chars till the end of this text are returned if no char was found.
   If a char with code of {@link #cEndOfText} is found in this string, it is the end of this search process too.
 @param fromIndex begin of search within the part.
 @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE.
 @param transcriptChar any char which is the transcription designation char, especially '\\'.
   Set to 0 if no transcription should be regarded.
 @param quotationStartChar any char which is the begin char of a quotation. Set to 0 if no quotation should be regarded.
 @param quotationEndChar the adequate end char   
 @return position of first founded char inside the actual part, but not greater than maxToTest, if no chars is found unitl maxToTest,
         but -1 if the end is reached.
*/
public int indexOfAnyChar(String sChars, final int fromWhere, final int maxToTest
   , char transcriptChar, char quotationStartChar, char quotationEndChar)
{ int pos = begin + fromWhere;
 int max = (end - pos) < maxToTest ? end : begin + maxToTest;
 boolean bNotFound = true;
 while(pos < max && bNotFound){ 
   char cc = schars.charAt(pos);
   if(cc == quotationStartChar && cc !=0)
   { int endQuotion = indexEndOfQuotation(quotationEndChar, transcriptChar, pos - begin, max - begin);
     if(endQuotion < 0){ pos = max; }
     else{ pos = endQuotion + begin; }
   }
   else if(cc == transcriptChar && cc != 0 && pos < (max-1)){
     pos +=2;
   }
   else
   { if(sChars.indexOf(cc) >= 0){ 
     bNotFound = false; 
     } else{ 
       pos +=1; 
     }
   }
 }
 if(bNotFound){
   if(sChars.indexOf(cEndOfText) >= 0) return pos - begin;  // it is found because cEndOfText is searched too.
   else return -1;
 }
 else return (pos - begin);
}



/**Returns the last position of one of the chars in sChars 
* within the part of actual part from (fromIndex) to (fromIndex+maxToTest) 
* or returs -1 if the char is not found in this part.
 @param sChars contents some chars to find. The char with code 
 @param fromIndex begin of search within the part.
 @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
 @return position of first founded char inside the actual part, but not greater than maxToTest. 
        if no chars is found unitl maxToTest,
         but -1 if the end is reached.
*/
public int lastIndexOfAnyChar(String sChars, final int fromWhere, final int maxToTest)
{ int pos = (end - begin) < maxToTest ? end-1 : begin + maxToTest-1;
 int min = begin + fromWhere;
 
 while(pos >= min && sChars.indexOf(schars.charAt(pos)) < 0)
 { pos -=1;
 }
 int index = pos >= min 
           ? pos - begin  //relative found position
           :  -1;         //not found
 return index;
}



  /**Returns the position of one of the chars in sChars within the part, started inside the part with fromIndex,
   *  returns -1 if the char is not found in the part started from 'fromIndex'.
   * @param listStrings contains some Strings to find.
   * @param fromWhere begin of search within the part.
   * @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
    * @param nrofFoundString If given, [0] is set with the number of the found String in listStrings, 
   *                        count from 0. This array reference may be null, then unused.
   * @param foundString If given, [0] is set with the found String. This array reference may be null.
   * @return position of first founded char inside the actual part, but not greater than maxToTest, 
   *                 if no chars is found until maxToTest, but -1 if the end is reached.
   */
  public int indexOfAnyString
  ( String[] listStrings
  , final int fromWhere
  , final int maxToTest
  , int[] nrofFoundString
  , String[] foundString
  )
  { int pos = begin + fromWhere;
    int max = (end - pos) < maxToTest ? end : pos + maxToTest;
    //int endLast = end;
    //StringBuffer sFirstCharBuffer = new StringBuffer(listStrings.size());
    assert(listStrings.length < 100);  //static size is need
    /** @java2c=stackInstance.*/
    StringBuffer sFirstCharBuffer = new StringBuffer(100);
    //Iterator<String> iter = listStrings.iterator();
    boolean acceptToEndOfText = false;
    //while(iter.hasNext())
    /**Compose a String with all first chars, to test whether a current char of src is equal. */
    { int ii = -1;
    while(++ii < listStrings.length)
    { //String sString = (String)(iter.next());
      String sString = listStrings[ii];
      if(sString.charAt(0) == cEndOfText)
      { acceptToEndOfText = true;}
      else 
      { sFirstCharBuffer.append(sString.charAt(0)); }
    } }
    /**@java2c=toStringNonPersist.*/
    String sFirstChars = sFirstCharBuffer.toString();
    boolean found = false;
    while(!found && pos < max)
    { 
      int nrofFoundString1 = -1;
      /**increment over not matching chars, test all first chars: */
      while(pos < max && (nrofFoundString1 = sFirstChars.indexOf(schars.charAt(pos))) < 0) pos +=1;
      
      if(pos < max)
      { /**a fist matching char is found! test wether or not the whole string is matched.
       * Test all Strings, the first test is the test of begin char. */
        int ii = -1;
        while(!found && ++ii < listStrings.length)  //NOTE: don't use for(...) because found is a criterium of break.
        { //String sString = (String)(iter.next());
          String sString = listStrings[ii];
          int testLen = sString.length();
          if((max - pos) >= testLen 
              && StringFunctions.equals(schars, pos, pos+testLen, sString)
          ) 
          { found = true;
          if(foundString != null)
          { foundString[0] = sString;
          }
          if(nrofFoundString != null)
          { nrofFoundString[0] = ii;
          }
          }
          //else { nrofFoundString1 +=1; }
        }
        if(!found){ pos +=1; }  //check from the next char because no string matches.
        
      }
    }
    int nChars;
    if(pos < max 
        || (pos == max && acceptToEndOfText)
    )
    { nChars = pos - begin;
    }
    else { 
      nChars = -1; 
      if(foundString != null)
      { foundString[0] = null;
      }
      if(nrofFoundString != null)
      { nrofFoundString[0] = -1;
      }
    }
    return nChars;
  }



/** Searches any char contented in sChars,
* but skip over quotions while testing. Example: The given string is<pre>
* abc "yxz" end:zxy</pre>
* The calling is<pre>
* lentoAnyCharOutsideQuotion("xyz", 20);</pre>
* The result current part is<pre>
* abc "yxz" end:</pre>
* because the char 'z' is found first as the end char, but outside the quoted string "xyz".
* @param sChars One of this chars is a endchar. It may be null: means, every chars is a endchar.
* @param fromWhere Offset after begin to begin search. It may be 0 in many cases.
* @param maxToTest
* @return
*/
public int indexOfAnyCharOutsideQuotion(String sChars, final int fromWhere, final int maxToTest)
{ int pos = begin + fromWhere;
 int max = (end - pos) < maxToTest ? end : begin + maxToTest;
 boolean bNotFound = true;
 while(pos < max && bNotFound)
 { char cc = schars.charAt(pos);
   if(cc == '\"')
   { int endQuotion = indexEndOfQuotion('\"', pos - begin, max - begin);
     if(endQuotion < 0){ pos = max; }
     else{ pos = endQuotion + begin; }
   }
   else
   { if(sChars.indexOf(cc) >= 0){ bNotFound = false; }
     else{ pos +=1; }
   }
 }
 return (bNotFound) ? -1 : (pos - begin);
}





/**Searches the end of a quoted string. In Generally, a backslash skips over the next char
* and does not test it as end of the quotion.  
* @param fromWhere Offset after begin to begin search. 
*                  It may be 0 if the quotion starts at begin, it is the position of the left
*                  quotion mark.
* @param maxToTest Limit for searching, offset from begin. It may be Integer.MAX_INT
* @return -1 if no end of quotion is found, else the position of the char after the quotion, 
*          at least 2 because a quotion has up to 2 chars, the quotion marks itself.
*/
public int indexEndOfQuotion(char cEndQuotion, final int fromWhere, final int maxToTest)
{ int pos = begin + fromWhere +1;
 int max = (end - pos) < maxToTest ? end : pos + maxToTest;
 boolean bNotFound = true;
 while(pos < max && bNotFound)
 { char cc = schars.charAt(pos++);
   if(cc == '\\' && (pos+1) < max)
   { pos += 1; //on \ overread the next char, test char after them!
   }
   else if(cc == cEndQuotion)
   { bNotFound = false;
   }
 }
 return (bNotFound ? -1 : (pos - begin));
}





/**Searches the end of a quoted string. In Generally, a backslash skips over the next char
* and does not test it as end of the quotion.  
* @param fromWhere Offset after begin to begin search. 
*                  It may be 0 if the quotion starts at begin, it is the position of the left
*                  quotion mark.
* @param maxToTest Limit for searching, offset from begin. It may be Integer.MAX_INT
* @return -1 if no end of quotion is found, else the position of the char after the quotion, 
*          at least 2 because a quotion has up to 2 chars, the quotion marks itself.
*/
public int indexEndOfQuotation(char cEndQuotion, char transcriptChar, final int fromWhere, final int maxToTest)
{ int pos = begin + fromWhere +1;
 int max = (end - pos) < maxToTest ? end : pos + maxToTest;
 boolean bNotFound = true;
 while(pos < max && bNotFound)
 { char cc = schars.charAt(pos++);
   if(cc == transcriptChar && cc !=0 && (pos+1) < max)
   { pos += 1; //on \ overread the next char, test char after them!
   }
   else if(cc == cEndQuotion)
   { bNotFound = false;
   }
 }
 return (bNotFound ? -1 : (pos - begin));
}





/**Returns the position of one of the chars in sChars within the part,
  returns -1 if the char is not found in the actual part.
 @param sChars contents some chars to find.
 @return position of first founded char inside the actual part or -1 if not found.
*/
public int indexOfAnyChar(String sChars)
{ return indexOfAnyChar(sChars, 0, Integer.MAX_VALUE);
}



/**Returns the position of the first char other than the chars in sChars within the part, started inside the part with fromIndex,
  returns -1 if all chars inside the parts  started from 'fromIndex' are chars given by sChars.
 @param sChars contents the chars to overread.
 @param fromIndex begin of search within the part.
 @return position of first foreign char inside the actual part or -1 if not found.
*/
public int indexOfNoChar(String sChars, final int fromWhere)
{ int pos = begin + fromWhere;
 while(pos < end && sChars.indexOf(schars.charAt(pos)) >= 0) pos +=1;
 return (pos >= end) ? -1 : (pos - begin);
}


/**Returns the position of the first char other than the chars in sChars within the part,
  returns -1 if all chars inside the parts are chars given by sChars.
 @param sChars contents the chars to overread.
 @return position of first foreign char inside the actual part or -1 if not found.
*/
public int indexOfNoChar(String sChars)
{ return indexOfNoChar(sChars, 0);
}



/** Sets the length of the current part to any char content in sChars (terminate chars). 
* If a terminate char is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate char is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0. 
* @java2c=return-this.
* @param sChars Some chars searched as terminate char for the actual part.
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @param mode Possible values are StringPartBase.seekBack or StringPartBase.seekNormal = 0.       
* @return This itself.
*/
public StringPartBase lentoAnyChar(String sChars, int maxToTest)
{ return lentoAnyChar(sChars, maxToTest, seekNormal);
}



/** Sets the length of the current part to any char content in sChars (terminate chars). 
* If a terminate char is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate char is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0. 
* @java2c=return-this.
* @param sChars Some chars searched as terminate char for the actual part.
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @param mode Possible values are StringPartBase.seekBack or StringPartBase.seekNormal = 0.       
* @return This itself.
*/
public StringPartBase lentoAnyChar(String sChars, int maxToTest, int mode)
{ endLast = end;
 int pos;
 if((mode & mSeekBackward_) != 0)
 { pos = lastIndexOfAnyChar(sChars, 0, maxToTest);
 }
 else
 { pos = indexOfAnyChar(sChars, 0, maxToTest);
 }
 if(pos < 0){ end = begin; bFound = false; }
 else       { end = begin + pos; bFound = true; } 
 return this;
}



/** Sets the length of the current part to any terminate string given in sString. 
* If a terminate string is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate string is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0. 
* @java2c=return-this.
* @param sString The first char is the separator. 
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @return This itself.
*/
public StringPartBase lentoAnyString(String[] strings, int maxToTest)
//public StringPartBase lentoAnyString(List<String> strings, int maxToTest)
{ return lentoAnyString(strings, maxToTest, seekNormal);
}



/** Sets the length of the current part to any terminate string given in sString. 
* If a terminate string is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate string is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0. 
* @java2c=return-this.
* @param sString The first char is the separator. 
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @param mode possible values are StrinPart.seekNormal or StringPartBase.seekEnd.
*        <ul><li>StringPartBase.seekEnd: the found string is inclusive.
*        </ul>       
* @return This itself.
*/
public StringPartBase lentoAnyString(String[] strings, int maxToTest, int mode)
//public StringPartBase lentoAnyString(List<String> strings, int maxToTest, int mode)
{ endLast = end;
 /**@java2c=stackInstance. It is only used internally. */
 String[] foundString = new String[1];
 int pos = indexOfAnyString(strings, 0, maxToTest, null, foundString);
 if(pos < 0){ end = begin; bFound = false; }
 else       
 { if( (mode & seekEnd) != 0)
   { pos += foundString[0].length();
   }
   end = begin + pos; bFound = true; 
 } 
 return this;
}



/** Sets the length of the current part to any terminate string given in sString. 
* If a terminate string is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate string is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0.
* <br>
* This method consideres the indent of the first line. In followed lines all chars are skipped 
* if there are inclose in sIndentChars until the column position of the first line. 
* If another char not inclosed in sIndentChars is found, than it is the beginning of this line.
* If the last char in sIndentChars is a space " ", additional all spaces and tabs '\t' will be
* skipped. This method is helpfull to convert indented text into a string without the indents, at example:
* <pre>
* . /** This is a comment
* .   * continued in a next line with indent.
* .  but it is able that the user doesn't respect the indentation
* .        also with to large indentation,
* .   * *The second asterix should not be skipped.
* </pre>
* From this text passage the result is:
* <pre>
* .This is a comment
* .continued in a next line with indent.
* .but it is able that the user doesn't respect the indentation
* .also with to large indentation,
* .*The second asterix should not be skipped.
* </pre>
* Using the result it is possible to detect paragraph formatting in wikipedia style 
* (see vishia.xml.ConvertWikistyleTextToXml.java) 
*   
* @param strings List of type String, containing the possible end strings.
* @param iIndentChars possible chars inside a skipped indentation. If the last char is space (" "),
*        also spaces after the indentation of the first line are skipped. 
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @param buffer The buffer where the found String is stored. The stored String has no indentations.       
* @since 2007, 2010-0508 changed param buffer, because better useable in C (java2c)
*/
public void lentoAnyStringWithIndent(String[] strings, String sIndentChars, int maxToTest, StringBuilder buffer)
//public String lentoAnyStringWithIndent(List<String> strings, String sIndentChars, int maxToTest)
{ endLast = end;
 //String sRet; sRet = "";
 buffer.setLength(0);
 int indentColumn = getCurrentColumn();
 int startLine = begin;
 boolean bAlsoWhiteSpaces = (sIndentChars.charAt(sIndentChars.length()-1) == ' ');
 int pos = indexOfAnyString(strings, 0, maxToTest, null, null);
 if(pos < 0){ end = begin; bFound = false; }
 else       
 { this.bFound = true;
   this.end = this.begin + pos; 
   boolean bFinish = false;
   while(!bFinish)  
   { pos = StringFunctions.indexOf(schars, 0, startLine, '\n');
     if(pos < 0) pos = this.end;
     if(pos > this.end)
     { //next newline after terminated string, that is the last line.
       pos = this.end;
       bFinish = true;
     }
     else { pos +=1; } // '\n' including
     //append the line to output string:
     buffer.append(schars.subSequence(startLine, pos));
     if(!bFinish)
     { //skip over indent.
       startLine = pos;
       int posIndent = startLine + indentColumn;
       if(posIndent > end) posIndent = end;
       while(startLine < posIndent && sIndentChars.indexOf(schars.charAt(startLine)) >=0)
       { startLine +=1;
       }
       if(bAlsoWhiteSpaces)
       { while(" \t".indexOf(schars.charAt(startLine)) >=0)
         { startLine +=1;
         }
       }
     }
   }  
 } 
 return ; //buffer.toString();
}



/** Sets the length of the current part to any char content in sChars (terminate chars),
* but skip over quotions while testing. Example: The given string is<pre>
* abc "yxz" ende:zxy</pre>
* The calling is<pre>
* lentoAnyCharOutsideQuotion("xyz", 20);</pre>
* The result current part is<pre>
* abc "yxz" ende:</pre>
* because the char 'z' is found first as the end char, but outside the quoted string "xyz".<br/>
* If a terminate char is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate char is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0. 
* @java2c=return-this.
* @param sChars Some chars searched as terminate char for the actual part.
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @return This itself.
*/
public StringPartBase lentoAnyCharOutsideQuotion(String sChars, int maxToTest)
{ endLast = end;
 int pos = indexOfAnyCharOutsideQuotion(sChars, 0, maxToTest);
 if(pos < 0){ end = begin; bFound = false; }
 else       { end = begin + pos; bFound = true; } 
 return this;
}


/** Sets the length of the current part to the end of the quotion. It is not tested here,
* whether or not the actual part starts with a left quotion mark.
* In Generally, a backslash skips over the next char and does not test it as end of the quotion.  
* @java2c=return-this.
* @param sEndQuotion The char determine the end of quotion, it may be at example " or ' or >.
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @return This itself.
*/
public StringPartBase lentoQuotionEnd(char sEndQuotion, int maxToTest)
{ endLast = end;
 int pos = indexEndOfQuotion(sEndQuotion, 0, maxToTest);
 if(pos < 0){ end = begin; bFound = false; }
 else       { end = begin + pos; bFound = true; } 
 return this;
}


/** Sets the length of the current part to any char content in sChars (terminate chars). 
* If a terminate char is not found, the length of the current part is set to 0.
* The same result occurs, if a terminate char is found at begin of the current part.
* If the difference of this behavior is important, use instead indexOfAnyChar() and test the
* return value, if it is &lt; 0. 
* @java2c=return-this.
* @param sChars Some chars searched as terminate char for the actual part.
* @return This itself.
*/
public StringPartBase lentoAnyChar(String sChars)
{ lentoAnyChar(sChars, Integer.MAX_VALUE);
 return this;
}


  /**This routine provides the this-pointer as StringPartScan in a concatenation of StringPartBase-invocations. 
   * @return this
   * @throws ClassCastException if the instance is not a StringPartScan. That is an internal software error.
   */
  public StringPartScan scan()
  { return (StringPartScan)this;
  }





  /** Central mehtod to invoke excpetion, usefull to set a breakpoint in debug
   * or to add some standard informations.
   * @param sMsg
   * @throws IndexOutOfBoundsException
   */
  private void throwIndexOutOfBoundsException(String sMsg)
  throws IndexOutOfBoundsException
  { throw new IndexOutOfBoundsException(sMsg);
  }
  
  

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override public int compareTo(CharSequence str2)
  { return StringFunctions.compare(this, 0, str2, 0, Integer.MAX_VALUE);
  }
  

  
  
  
  /* (non-Javadoc)
   * @see java.lang.CharSequence#charAt(int)
   */
  @Override
  public char charAt(int index){ 
    return absCharAt(begin + index);
  }

  
  protected char absCharAt(int index){
    int pos = index - apos;
    if(pos < 0){
      if(shifter !=null){ 
        shifter.shiftLeft(-pos);
        pos = begin + index - apos;  //it should be >=0
      } else throw new IllegalArgumentException("StringPartBase.charAt - faulty; " + index);
    } else if(pos > schars.length()){
      if(shifter !=null){ 
        shifter.shiftNext(pos - schars.length() +1);
        pos = begin + index - apos;  //it should be < length
      } else throw new IllegalArgumentException("StringPartBase.charAt - faulty; " + index);
    }
    if(pos >=0 && pos < schars.length()) return schars.charAt(pos);
    else throw new IllegalArgumentException("StringPartBase.charAt - faulty; " + index);
  }

  /**Returns a volatile CharSequence from the range inside the current part.
   * If it is not possible an IllegalArgumentException is thrown.
   * The difference to {@link #subString(int, int)} is: It is not persistant.
   * This method should only used if the CharSequence is processed in the thread immediately
   * for example by adding to another StringBuilder etc. The returned instance should not be saved
   * for later usage.
   *  
   * @see java.lang.CharSequence#subSequence(int, int)
   */
  @Override public CharSequence subSequence(int from, int to)
  { 
    if(from < 0 || to > (end - begin)) throw new IllegalArgumentException("StringPartBase.subString - faulty;" + from);
    return new Part(begin+from, begin+to); 
  } 
  
  
  
  /**Returns a String from the range inside the current part.
   * If it is not possible an IllegalArgumentException is thrown.
   * @see java.lang.CharSequence#subSequence(int, int)
   */
  public String subString(int from, int to){ 
    if(from < 0 || to > (end - begin)) throw new IllegalArgumentException("StringPartBase.subString - faulty;" + from);
    return absSubString(begin + from, begin + to); 
  }
  
  
  
  
  /**Returns a String from absolute range.
   * @param from The absolute position.
   * @param to The absolute end.
   * @return A valid String or an IllegalArgumentException is occurred
   */
  protected String absSubString(int from, int to)
  { 
    int pos = from - apos;
    int len = to - from;
    int end1 = pos + len;
    if(pos < 0){
      if(shifter !=null){ 
        shifter.shiftLeft(-pos);
        pos = begin + from - apos;  //it should be >=0
      } else throw new IllegalArgumentException("StringPartBase.subString - faulty; " + from);
    } else if(end1 > schars.length()){
      if(shifter !=null){ 
        shifter.shiftNext(end1 - schars.length() +1);
        pos = begin + from - apos;  
        end1 = pos + len; //it should be < schars.length()
      } else throw new IllegalArgumentException("StringPartBase.subSequence - faulty; " + from);
    }
    if(pos >=0 && end1 <= schars.length()){
      return schars.subSequence(pos, pos + len).toString(); 
    }
    else throw new IllegalArgumentException("StringPartBase.subSequence - faulty; " + from);
  }

  
  
  
  /** Gets the next chars from current Position.
   *  This method don't consider the spread of the actutal and maximal part.
      @param nChars number of chars to return. If the number of chars available in string
      is less than the required number, only the available string is returned.
  */

  public CharSequence getCurrent(int nChars)
  { final int nChars1 =  (schars.length() - begin) < nChars ? schars.length() - begin : nChars;
    return( new Part(begin + apos, begin + nChars1 + apos));
  }

  /** Gets the next char at current Position.
  */
  public char getCurrentChar()
  { if(begin < schars.length()){ return schars.charAt(begin); }
    else return '\0'; ///**@java2c=StringBuilderInThreadCxt.*/ throw new IndexOutOfBoundsException("end of StringPartBase:" + begin); // return cEndOfText;
  }
 
  
  /** Gets the current position in line (column of the text).
   * It is the number of chars from the last '\n' or from beginning to the actual char.
   * @return Position of the actual char from begin of line, leftest position is 0.
   */
  public int getCurrentColumn()
  { int pos = StringFunctions.lastIndexOf(schars, 0, begin, '\n');
    if(pos < 0) return begin;  //first line, no \n before
    else return begin - pos -1;
  }
  
  /** Returns the actual part of the string.
   * 
   */
  public CharSequence getCurrentPart()
  { if(end > begin) return new Part(begin + apos, end + apos);
    else            return "";
  }
  

  /** Returns the last part of the string before any seek or scan operation.
   * 
   */
  public CharSequence getLastPart()
  { if(begin > beginLast) return new Part(beginLast + apos, begin + apos);
    else            return "";
  }
  

  /** Returns the actual part of the string.
   * 
   */
  public CharSequence getCurrentPart(int maxLength)
  { int max = (end - begin) <  maxLength ? end : begin + maxLength;
    if(end > begin) return new Part(begin + apos, max + apos);
    else            return ""; 
  }
  

  



  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override public String toString(){ 
    return schars.subSequence(this.begin, this.end).toString();
  }


  
  
  /**This class presents a part of the string.
   */
  public class Part implements CharSequence{ 
    
    /**Absolute positions of part of chars*/
    int b1, e1;
    
    
    /**A subsequence
     * @param from absolute positions
     * @param to
     */
    protected Part(int from, int to){
      b1 = from; e1 = to;
    }
    
    @Override
    public char charAt(int index)
    { return absCharAt(b1 + index);
    }
    
    @Override
    public int length()
    { return e1 - b1;
    }
    
    @Override
    public CharSequence subSequence(int from, int end)
    { return new Part(b1 + from, b1 + end);
    }
  
    @Override public String toString(){
      return absSubString(b1, e1);
    }
    
  }

  
  /**This interface should be implemented by derived classes of {@link StringPartBase}
   * which supports longer char sequences by using a shorter buffer for chars.
   */
  public interface StringPartShift
  {
    void shiftLeft(int nrofChars);
    
    /**Reads the next bytes for example from a file
     * and shifts the chars in the buffer.
     * @param nrofChars
     * @return
     */
    void shiftNext(int nrofChars);
    
  }

  
}
