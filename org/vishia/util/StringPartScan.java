package org.vishia.util;

import java.text.ParseException;

/**This class extends the capability of StringPartBase with the same properties.
 * @author Hartmut Schorrig
 *
 */
public class StringPartScan extends StringPartBase
{
  
  
  /**Position of scanStart() or after scanOk() as begin of next scan operations. */
  protected int beginScan;
  
  /**Last scanned integer number.*/
  protected final long[] nLastIntegerNumber = new long[5];
  
  /**current index of the last scanned integer number. -1=nothing scanned. 0..4=valid*/
  private int idxLastIntegerNumber = -1;
  
  /**Last scanned float number*/
  protected final double[] nLastFloatNumber = new double[5];
  
  /**current index of the last scanned float number. -1=nothing scanned. 0..4=valid*/
  private int idxLastFloatNumber = -1;
  
  /** Last scanned string. */
  protected CharSequence sLastString;
  

  
  public StringPartScan(CharSequence src, int begin, int end)
  { super(src, begin, end);
  }

  public StringPartScan(CharSequence src)
  { super(src);
  }

  
  /**Only yet for compatibility.
   * @param src
   */
  public StringPartScan(StringPartOld src)  
  { super(src.content);  //package private
  }

  
  
  /** Set the mode of ignoring comments.
   * If it is set, comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The string introduces and finishes a comment is setted by calling 
   * setIgnoreComment(String sStart, String sEnd). The default value is "/ *" and "* /" like in java-programming. 
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of this feature.
   */
  public boolean setIgnoreComment(boolean bSet)
  { boolean bRet = (bitMode & mSkipOverCommentInsideText_mode) != 0;
    if(bSet) bitMode |= mSkipOverCommentInsideText_mode;
    else     bitMode &= ~mSkipOverCommentInsideText_mode;
    return bRet;
  }
  
  
  /** Set the character string of inline commentmode of ignoring comments.
   * After this call, comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart Start character string of a inline comment
   * @param sEnd End character string of a inline comment
   * @return The last definition of the feature setIgnoreComment(boolean).
   */
  public boolean setIgnoreComment(String sStart, String sEnd)
  { boolean bRet = (bitMode & mSkipOverCommentInsideText_mode) != 0;
    bitMode |= mSkipOverCommentInsideText_mode;
    sCommentStart = sStart; 
    sCommentEnd   = sEnd;
    return bRet;
  }
  
  
  /** Set the mode of ignoring comments to end of line.
   * If it is set, end-line-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The string introduces a endofline-comment is setted by calling 
   * setEndlineCommentString(). The default value is "//" like in java-programming. 
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of the feature setIgnoreComment(boolean).
   */
  public boolean setIgnoreEndlineComment(boolean bSet) 
  { boolean bRet = (bitMode & mSkipOverCommentToEol_mode) != 0;
    if(bSet) bitMode |= mSkipOverCommentToEol_mode;
    else     bitMode &= ~mSkipOverCommentToEol_mode;
    return bRet;
  }
  

  
  /** Set the character string introducing the comments to end of line.
   * After this call, endline-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart String introducing end line comment
   * @return The last definition of this feature.
   */
  public boolean setIgnoreEndlineComment(String sStart) 
  { boolean bRet = (bitMode & mSkipOverCommentToEol_mode) != 0;
    bitMode |= mSkipOverCommentToEol_mode;
    sCommentToEol = sStart;
    return bRet;
  }
  
  /** Set the mode of ignoring whitespaces.
   * If it is set, whitespaces are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The chars accepted as whitespace are setted by calling 
   * setWhiteSpaceCharacters(). The default value is " \t\r\n\f" like in java-programming.
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of this feature.
   */
  public boolean setIgnoreWhitespaces(boolean bSet)
  { boolean bRet = (bitMode & mSkipOverWhitespace_mode) != 0;
    if(bSet) bitMode |= mSkipOverWhitespace_mode;
    else     bitMode &= ~mSkipOverWhitespace_mode;
    return bRet;
  }
  
  
  /**
   * @java2c=return-this.
   * @return
   */
  public StringPartScan scanStart()
  { bCurrentOk = true;
    scanOk();  //turn all indicees to ok
    return this;
  }


  
  
  
  private boolean scanEntry()
  { if(bCurrentOk)
    { seekNoWhitespaceOrComments();
      if(bStartScan)
      { idxLastIntegerNumber = -1;
        //idxLastFloatNumber = 0;
        //idxLastString = 0;
        bStartScan = false; 
      }
      if(begin == end)
      { bCurrentOk = false; //error, because nothing to scan.
      }
    }
    return bCurrentOk;
  }
  

  
  /**Test the result of scanning and set the scan Pos Ok, if current scanning was ok. If current scanning
   * was not ok, this method set the current scanning pos back to the position of the last call of scanOk()
   * or scanNext() or setCurrentOk().
   * @return true if the current scanning was ok, false if it was not ok.
   */

  public boolean scanOk()
  { if(bCurrentOk) 
    { beginScan =  beginLast = begin;    //the scanOk-position is the begin of maximal part.
      bStartScan = true;   //set all idxLast... to 0
    }
    else           
    { begin = beginLast= beginScan;   //return to the begin
    }
    //if(report != null){ report.report(6," scanOk:" + beginMin + ".." + begin + ":" + (bCurrentOk ? "ok" : "error")); }
    boolean bOk = bCurrentOk;
    bCurrentOk = true;        //prepare to next try scanning
    return(bOk);
  }


  
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /** scan next content, test the requested String.
   *  new since 2008-09: if sTest contains cEndOfText, test whether this is the end.
   *  skips over whitespaces and comments automatically, depends on the settings forced with
   *  calling of {@link #seekNoWhitespaceOrComments()} .<br/>
   *  See global description of scanning methods.
   * @java2c=return-this.
   *  @param sTest String to test
      @return this
  */
  public StringPartScan scan(final CharSequence sTestP)
  { if(bCurrentOk)   //NOTE: do not call scanEntry() because it returns false if end of text is reached,
    {                //      but the sTestP may contain only cEndOfText. end of text will be okay than.
      seekNoWhitespaceOrComments();
      CharSequence sTest;
      int len = StringFunctions.indexOf(sTestP,cEndOfText,0);
      boolean bTestToEndOfText = (len >=0);
      if(bTestToEndOfText){ sTest = sTestP.subSequence(0, len); }
      else { len = sTestP.length(); sTest = sTestP; }
      if(  (begin + len) <= endMax //content.length()
        && StringFunctions.equals(schars, begin, begin+len, sTest)
        && (  !bTestToEndOfText 
           || begin + len == end
           )
        )
      { begin += len;
      }
      else 
      { bCurrentOk = false; 
      }
    }
    return this;
  }


  
  /**
   * @java2c=return-this.
   * @param sQuotionmarkStart
   * @param sQuotionMarkEnd
   * @param sResult
   * @return
   */
  public StringPartScan scanQuotion(CharSequence sQuotionmarkStart, String sQuotionMarkEnd, String[] sResult)
  { return scanQuotion(sQuotionmarkStart, sQuotionMarkEnd, sResult, Integer.MAX_VALUE);
  }
  
  
  /**
   * @java2c=return-this.
   * @param sQuotionmarkStart
   * @param sQuotionMarkEnd
   * @param sResult
   * @param maxToTest
   * @return
   */
  public StringPartScan scanQuotion(CharSequence sQuotionmarkStart, String sQuotionMarkEnd, String[] sResult, int maxToTest)
  { if(scanEntry())
    { scan(sQuotionmarkStart).lentoNonEscapedString(sQuotionMarkEnd, maxToTest);
      if(bCurrentOk)
      { //TODO ...ToEndString, now use only 1 char in sQuotionMarkEnd
        if(sResult != null) sResult[0] = getCurrentPart().toString();
        fromEnd().seek(sQuotionMarkEnd.length());
      }
      else bCurrentOk = false; 
    }
    return this;
  }
  
  
  

  /**Scans if it is a integer number, contains exclusively of digits 0..9
      @param bHex true: scan hex Digits and realize base 16, otherwise realize base 10.
      @return long number represent the digits.
  */
  private long scanDigits(boolean bHex, int maxNrofChars)
  { if(bCurrentOk)
    { long nn = 0;
      boolean bCont = true;
      int pos = begin;
      int max = (end - pos) < maxNrofChars ? end : pos + maxNrofChars;
      do
      {
        if(pos < max)
        { char cc = schars.charAt(pos);
          if(cc >= '0' &&  cc <='9') nn = nn * (bHex? 16:10) + (cc - '0');
          else if(bHex && cc >= 'a' &&  cc <='f') nn = nn * 16 + (cc - 'a' + 10);
          else if(bHex && cc >= 'A' &&  cc <='F') nn = nn * 16 + (cc - 'A' + 10);
          else bCont = false;
          if(bCont){ pos +=1; }
        }
        else bCont = false;
      } while(bCont);
      if(pos > begin)
      { begin = pos;
        return nn;
        //nLastIntegerNumber = nn;
      }
      else bCurrentOk = false;  //scanning failed.
    }
    return -1; //on error
  }


  
  /**Scanns a integer number as positiv value without sign. 
   * All digit character '0' to '9' will be proceed. 
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   * @return
   */
  public StringPartScan scanPositivInteger() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value = scanDigits(false, Integer.MAX_VALUE);
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
        }
        else throw new ParseException("to much scanned integers",0);
      }  
    } 
    return this;
  }

  /**Scans an integer expression with possible sign char '-' at first.
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   * @return this
   */
  public StringPartScan scanInteger() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { boolean bNegativValue = false;
      if( schars.charAt(begin) == '-')
      { bNegativValue = true;
        seek(1);
      }
      long value = scanDigits(false, Integer.MAX_VALUE);
      if(bNegativValue)
      { value = - value; 
      }
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }  
    return this;
  }

  
  /**Scans a float number. The result is stored internally
   * and have to be got calling {@link #getLastScannedFloatNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown.
   * @param cleanBuffer true then clean the float number buffer because the values are not used. 
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public StringPartScan scanFloatNumber(boolean cleanBuffer)  throws ParseException
  {
    if(cleanBuffer){
      idxLastFloatNumber = -1; 
    }
    scanFloatNumber();
    return this;
  }
  
  
  
  /**Scans a float number. The result is stored internally
   * and have to be got calling {@link #getLastScannedFloatNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public StringPartScan scanFloatNumber() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long nInteger = 0, nFractional = 0;
      int nDivisorFract = 1, nExponent;
      //int nDigitsFrac;
      char cc;
      boolean bNegativValue = false, bNegativExponent = false;
      boolean bFractionalFollowed = false;
      
      if( (cc = schars.charAt(begin)) == '-')
      { bNegativValue = true;
        seek(1);
        cc = schars.charAt(begin);
      }
      if(cc == '.')
      { nInteger = 0;
        bFractionalFollowed = true;
      }
      else
      { nInteger = scanDigits(false, Integer.MAX_VALUE);
        if(bCurrentOk)
        { if(begin < endMax && schars.charAt(begin) == '.')
          { bFractionalFollowed = true;
          }
        }
      }
      
      if(bCurrentOk && bFractionalFollowed)
      { seek(1); //over .
        while(begin < endMax && getCurrentChar() == '0')
        { seek(1); nDivisorFract *=10;
        }
        //int posFrac = begin;
        nFractional = scanDigits(false, Integer.MAX_VALUE);
        if(bCurrentOk)
        { //nDigitsFrac = begin - posFrac;
        }
        else if(nDivisorFract >=10)
        { bCurrentOk = true; //it is okay, at ex."9.0" is found. There are no more digits after "0".
          nFractional = 0;
        }
      }   
      else {nFractional = 0; } //nDigitsFrac = 0;}
      
      if(bCurrentOk)
      { int nPosExponent = begin;
        if( nPosExponent < endMax && (cc = schars.charAt(begin)) == 'e' || cc == 'E')
        { seek(1);
          if( (cc = schars.charAt(begin)) == '-')
          { bNegativExponent = true;
            seek(1);
            cc = schars.charAt(begin);
          }
          if(cc >='0' && cc <= '9' )
          { nExponent = (int)scanDigits(false, Integer.MAX_VALUE);
            if(!bCurrentOk)
            { nExponent = 0;
            }
          }
          else
          { // it isn't an exponent, but a String beginning with 'E' or 'e'.
            //This string is not a part of the float number.
            begin = nPosExponent;
            nExponent = 0;
          }
        }
        else{ nExponent = 0; }
      } 
      else{ nExponent = 0; }
      
      if(bCurrentOk){ 
        double result = nInteger;
        if(nFractional > 0)
        { double fFrac = nFractional;
          while(fFrac >= 1.0)  //the read number is pure integer, it is 0.1234
          { fFrac /= 10.0; 
          }
          fFrac /= nDivisorFract;    //number of 0 after . until first digit.
          result += fFrac;
        }
        if(bNegativValue) { result = - result; }
        if(nExponent != 0)
        { if(bNegativExponent){ nExponent = -nExponent;}
          result *= Math.pow(10, nExponent);
        }
        if(idxLastFloatNumber < nLastFloatNumber.length -2){
          nLastFloatNumber[++idxLastFloatNumber] = result;
        } else throw new ParseException("to much scanned floats",0);
      }
    }  
    return this;
  }

  
  /**Scans a sequence of hex chars a hex number. No '0x' or such should be present. 
   * See scanHexOrInt().
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   */
  public StringPartScan scanHex(int maxNrofChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value = scanDigits(true, maxNrofChars);
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }
    return this;
  }

  /**Scans a integer number possible as hex, or decimal number.
   * If the number starts with 0x it is hexa. Otherwise it is a decimal number.
   * Octal numbers are not supported!  
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   * @param maxNrofChars The maximal number of chars to scan, if <=0 than no limit.
   * @return this to concatenate the call.
   */
  public StringPartScan scanHexOrDecimal(int maxNrofChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value;
      if( StringFunctions.equals(schars, begin, begin+2, "0x"))
      { seek(2); value = scanDigits(true, maxNrofChars);
      }
      else
      { value = scanDigits(false, maxNrofChars);
      }
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }
    return this;
  }

  
  /**
   * @java2c=return-this.
   * @return
   */
  public StringPartScan scanIdentifier()
  { return scanIdentifier(null, null);
  }
  
  
  /**
   * @java2c=return-this.
   * @param additionalStartChars
   * @param additionalChars
   * @return
   */
  public StringPartScan scanIdentifier(String additionalStartChars, String additionalChars)
  { if(scanEntry())
    { lentoIdentifier(additionalStartChars, additionalChars);
      if(bFound)
      { sLastString = getCurrentPart();
        begin = end;  //after identifier.
      }
      else
      { bCurrentOk = false;
      }
      end = endLast;  //revert the change of length, otherwise end = end of identifier.
    } 
    return this;
  }

  
  /**Returns the last scanned integer number. It is the result of the methods
   * <ul><li>{@link #scanHex(int)}
   * <li>{@link #scanHexOrDecimal(int)}
   * <li>{@link #scanInteger()}
   * </ul>
   * @return The number in long format. A cast to int, short etc. may be necessary
   *         depending on the expectable values.
   * @throws ParseException if called though no scan routine was called. 
   */
  public long getLastScannedIntegerNumber() throws ParseException
  { if(idxLastIntegerNumber >= 0)
    { return nLastIntegerNumber [idxLastIntegerNumber--];
    }
    else throw new ParseException("no integer number scanned.", 0);
  }
  
  
  /**Returns the last scanned float number.
   * @return The number in double format. A cast to float may be necessary
   *         depending on the expectable values and the storing format.
   * @throws ParseException if called though no scan routine was called. 
   */
  public double getLastScannedFloatNumber() throws ParseException
  { if(idxLastFloatNumber >= 0)
    { return nLastFloatNumber[idxLastFloatNumber--];
    }
    else throw new ParseException("no float number scanned.", 0);
  }
  
  
  
  /**Returns the part of the last scanning yet only from {@link #scanIdentifier()}
   * @return The CharSequence which refers in the parent sequence. Use toString() if you need
   *   an persistent String.
   */
  public CharSequence getLastScannedString()
  { return sLastString;
  }
  

  
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /** Gets a String with translitaration.
   *  The end of the string is determined by any of the given chars.
   *  But a char directly after the escape char \ is not detected as an end char.
   *  Example: getCircumScriptionToAnyChar("\"") ends not at a char " after an \,
   *  it detects the string "this is a \"quotion\"!".
   *  Every char after the \ is accepted. But the known subscription chars
   *  \n, \r, \t, \f, \b are converted to their control-char- equivalence.
   *  The \s and \e mean begin and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
   *  The actual part is tested for this, after this operation the actual part begins
   *  after the getting chars!
   *  @param sCharsEnd Assembling of chars determine the end of the part.  
   * */
  public CharSequence getCircumScriptionToAnyChar(String sCharsEnd)
  { return getCircumScriptionToAnyChar_p(sCharsEnd, false);
  }
  
  
  public CharSequence getCircumScriptionToAnyCharOutsideQuotion(String sCharsEnd)
  { return getCircumScriptionToAnyChar_p(sCharsEnd, true);
  }
  
  
  private CharSequence getCircumScriptionToAnyChar_p(String sCharsEnd, boolean bOutsideQuotion)
  { CharSequence sResult;
    if(begin == 4910)
      Assert.stop();
    final char cEscape = '\\';
    int posEnd    = (sCharsEnd == null) ? end 
                  : bOutsideQuotion ? indexOfAnyCharOutsideQuotion(sCharsEnd, 0, end-begin)
                                    : indexOfAnyChar(sCharsEnd);
    if(posEnd < 0) posEnd = end - begin;
    //int posEscape = indexOf(cEscape);
    //search the first escape char inside the string.
    int posEscape = StringFunctions.indexOf(schars, begin, begin + posEnd, cEscape) - begin;  
    if(posEscape < 0)
    { //there is no escape char in the current part to sCharsEnd,
      //no extra conversion is necessary.
      sResult = lento(posEnd).getCurrentPart();
    }
    else
    { //escape character is found before end
      if(schars.charAt(begin + posEnd-1)== cEscape)
      { //the escape char is the char immediately before the end char. 
        //It means, the end char isn't such one and posEnd is faulty. 
        //Search the really end char:
        do
        { //search the end char after part of string without escape char
          posEnd    = (sCharsEnd == null) ? end : indexOfAnyChar(sCharsEnd, posEscape +2, Integer.MAX_VALUE);
          if(posEnd < 0) posEnd = end;
          posEscape = indexOf(cEscape, posEscape +2);
        }while((posEscape +1) == posEnd);
      }
      lento(posEnd);
      
      sResult = SpecialCharStrings.resolveCircumScription(getCurrentPart());
    }
    fromEnd();
    return sResult;
  }


  
  /**Scans a String with transcription till one of end characters, maybe outside any quotation.
   *  The end of the string is determined by any of the given chars.
   *  But a char directly after the transcription char is not detected as an end char.
   *  Example: scanTranscriptionToAnyChar(dst, "<", '\\', '\"', '\"') 
   *  does not end at a char > after an \ and does not end inside the quotation,
   *  it detects the string "this \< is a \"quotation\"!" till a simple "<".
   *  Every char after the transcriptChar is accepted. But the known subscription chars
   *  \n, \r, \t, \f, \b are converted to their control-char- equivalence.
   *  The \s and \e mean begin and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
   *  The actual part is tested for this, after this operation the actual part begins
   *  after the getting chars!
   *
   * @param dst if it is null, then no result will be stored, elsewhere a CharSequence[1].
   * @param sCharsEnd End characters
   * @param transcriptChar typically '\\', 0 if not used
   * @param quotationStartChar typically '\"', may be "<" or such, 0 if not used
   * @param quotationEndChar The end char, typically '\"', may be ">" or such, 0 if not used
   * @return
   * @since 2013-09-07
   */
  public StringPartScan scanTranscriptionToAnyChar(CharSequence[] dst, String sCharsEnd
      , char transcriptChar, char quotationStartChar, char quotationEndChar)
  { if(scanEntry()){
      if(begin == 4910)
        Assert.stop();
      int posEnd = indexOfAnyChar(sCharsEnd, 0, end-begin, transcriptChar, quotationStartChar, quotationEndChar);
      if(posEnd >=0){
        lento(posEnd);
        if(dst !=null){
          dst[0] = StringFunctions.convertTranscription(getCurrentPart(), transcriptChar);
        }
        fromEnd();
      } else {
        bCurrentOk = false;
      }
    }
    return this;
  }


  

}
