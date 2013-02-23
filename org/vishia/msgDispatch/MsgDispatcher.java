/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.msgDispatch;

/**@changes:
 */



import java.io.FileDescriptor;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
//import java.util.Date;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.MemC;
import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.VaArgBuffer;
import org.vishia.bridgeC.Va_list;
import org.vishia.util.FileWriter;
import org.vishia.util.StringPart;



/**The message dispatcher supports to dispatch messages by an ident number to several destinations.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgDispatcher extends MsgDispatcherCore implements LogMessage
{

/**Version and history:
 * <ul>
 * <li>2012-01-13 Hartmut make some methods public, especially {@link #searchDispatchBits(int)}.
 *   It make it possible to use the dispatch data without calling {@link #sendMsg(int, String, Object...)}.
 * <li>2009-02-05 Hartmut  *corr nrofMixedOutputs
 *                   *bugfix: setOutputRoutine() should count maxDst
 *                   *new: reportOutput
 * <li>2009-02-04 Hartmut  *corr: Syntax errors in setOutputFromString() tested and improved. 
 *                   The param errorBuffer is now optional. It may be null.
 * <li>2009-02-03 Hartmut  *new: method isOnline
 * <li>2009-02-01 Hartmut  *new method getSharedFreeEntries().
 *                   * dispatchMsg(...) returns the mask of non-handled outputs, to handle later or in queue.
 *                   * sendMsgVaList(...) returns true, 
 *                   functionality: All non dispatched messages in calling thread are to dispatched in dispatcher thread, 
 *                   using return value of dispatchMsg(...). The functionality are not change really.
 *                   * dispatchQueuedMsg(): Try to evaluated non output messages, but this algorithm isn't good. No change.
 *                   * Implementation of close and flush: It means here: unconditionally dispatching.
 * </ul>
 */
public static final int version = 0x20120113; 
  
  //private LogMessage[] outputs = new LogMessage[28];
  
  /**A console output is standard everytime..
   * @java2c=noGC.
   */
  public final LogMessage outputConsole = LogMessageStream.create(FileDescriptor.out);
  
  //private boolean[] dstInDispatcherThread = new boolean[28];
  
  private int maxDst = 0;

      
  
  /**Initializes the instance.
   * @param maxDispatchEntries The max number of message-ident-number ranges which are dispatched 
   *        to different outputs. It is a static limited data amount. 
   *        If you have about 1000 different message-ident-numbers, 1000 is okay. 
   *        If you have some more different message-ident-numbers, but with determined ranges,
   *        which are always dispatched to the same destinations, that number of ranges is okay.
   *        In other words, it is the maximal number of lines in a message dispatched file, 
   *        if all message ranges are set new.
   *          
   * @param maxQueue The static limited maximal size of the queue to store messages from user threads
   *        to dispatch in the dispatcher thread. If you call the dispatching in dispatcher thread
   *        cyclicly in 100-ms-steps, and you have worst case no more as 200 messages in this time,
   *        200 is okay.
   * @param maxOutputs The static limited maximal number of outputs.
   * @param nrofMixedOutputs
   */
  public MsgDispatcher(int maxDispatchEntries, int maxQueue, int maxOutputs, int nrofMixedOutputs)
  { super(maxQueue, nrofMixedOutputs);
    
    /**@java2c = embeddedArrayElements. */
    Entry[] entries = new Entry[maxQueue];
    for(int idxEntry = 0; idxEntry < entries.length; idxEntry++)
    { entries[idxEntry] = new Entry();
    }
    int idxEntry;
    
    /**All entries, 1 time allocated, are stored in the freeOrders. From there they are taken
     * if an entry is necessary. */
    for(idxEntry = 0; idxEntry < maxQueue; idxEntry++)
    { this.freeOrders.add(entries[idxEntry]);
    }
    
    { this.listIdents = new int[maxDispatchEntries];
      this.listBitDst = new int[maxDispatchEntries];
      this.listIdents[0] = 0;
      this.listIdents[1] = Integer.MAX_VALUE;
      this.listBitDst[0] = 0;
      this.listBitDst[1] = 0xFFFFFFFF;  //this value will be unused because MAX_VALUE don't be used.
      this.actNrofListIdents = 2;
    }
    /*allocate the output array: */
    outputs = new Output[maxOutputs];
    for(int idxDst = 0; idxDst < maxOutputs; idxDst++){ outputs[idxDst] = new Output();}
    
    /**Set default output to console. */
    setOutputRoutine(0, "CON", false, outputConsole);  //mConsole
    setOutputRoutine(1, "qCON", true, outputConsole);   //mConsoleQueued
    setOutputRange(0, Integer.MAX_VALUE, mConsole, MsgDispatcher.mSet, 3);
  }
  
  /**Gets the internal free entries for sharing with an other log output, 
   * at example LogMessageFile.
   */
  public final ConcurrentLinkedQueue<Entry> getSharedFreeEntries(){ return freeOrders; }
  
  
  public final void setDefaults(String fileOut)
  { setOutputRoutine(0, "CON", false, outputConsole);  //mConsole
    setOutputRoutine(1, "qCON", true, outputConsole);   //mConsoleQueued
    //LogMessage outputFile = new LogMessageFile(fileOut);
    //setOutputRoutine(1, false, outputFile);     //mFile
    setOutputRange(0, Integer.MAX_VALUE, mConsole, MsgDispatcher.mSet, 3);
  }
  
  
  
  
  

  /**inserts an ident range after given position:
   * <pre> 1 means any stored ident number, 7 means a number behind,
   * i,j means the inserted idents. The idx selects the 1.-ident.
   * before: ....1.7......
   * after:  ....1.i.j.7..</pre>
   * If fromIdent == toIdent, only one position is inserted:
   * <pre>
   * before: ....1.7....
   * after:  ....1.j.7..
   * @param idx Index, after them the ident is inserted.
   * @param fromIdent first value of the ident.
   * @param toIdent second value of the ident.
   * @param mask bit to or
   */
  private final int insertIdent(int idx, int fromIdent, int toIdent)
  { final int ident0 = listIdents[idx];
    final int ident3 = listIdents[idx+1];
    final int idxFrom;  //idx where fromIdent is set
    if(ident0 < fromIdent && toIdent+1 < ident3)
    { /**insert two new positions, {..., ident0, fromIdent, toIdent, ident3, ...} :*/
      idxFrom = idx+1;
      actNrofListIdents +=2;
      //copy after idx, src from idx+1, because the content at idx is okay. 
      System.arraycopy(listIdents, idx+1, listIdents, idx+3, actNrofListIdents - (idx+3));
      System.arraycopy(listBitDst, idx+1, listBitDst, idx+3, actNrofListIdents - (idx+3));
      listIdents[idxFrom] = fromIdent;
      listIdents[idxFrom+1] = toIdent+1;
      /**Copy the mask from range ident0.. to the new positions. */
      listBitDst[idxFrom] = listBitDst[idxFrom+1] = listBitDst[idx];
      //listBitDst[idxFrom] |= mask;
    }
    else if(ident0 < fromIdent)
    { /**insert one new positions, {..., ident0, fromIdent, ident3, ...} :*/
      assert(toIdent+1 == ident3);
      idxFrom = idx+1;
      actNrofListIdents +=1;
      System.arraycopy(listIdents, idx+1, listIdents, idx+2, actNrofListIdents - (idx+2));
      System.arraycopy(listBitDst, idx+1, listBitDst, idx+2, actNrofListIdents - (idx+2));
      listIdents[idxFrom] = fromIdent;
      /**Copy the mask from range ident0.. to the new positions. */
      listBitDst[idxFrom] =  listBitDst[idx];
      //listBitDst[idxFrom] |= mask; 
    }
    else if(ident0 == fromIdent && toIdent+1 < ident3)
    { /**insert one new positions, {..., ident0, toIdent, ident3, ...} :*/
      actNrofListIdents +=1;
      idxFrom = idx;
      System.arraycopy(listIdents, idx+1, listIdents, idx+2, actNrofListIdents - (idx+2));
      System.arraycopy(listBitDst, idx+1, listBitDst, idx+2, actNrofListIdents - (idx+2));
      listIdents[idx+1] = toIdent+1;
      /**Copy the mask from range ident0.. to the new positions. */
      listBitDst[idx+1] =  listBitDst[idx];
    }
    else
    { assert(ident0 == fromIdent && toIdent+1 == ident3);
      idxFrom = idx;
      //replace only
      //listBitDst[idx] |= mask; 
    }
    return idxFrom;
  }
  

  
  
  
  /**Sets a destination interface to a index for dispatching.
   * @param dstIdx The index 0..28 or greater, which is associated to the destination.
   * @param bQueued true than the message is queued if this destination is used.
   * @param outputs The destination for message or log output.
   */
  public final void setOutputRoutine(int dstIdx, String name, boolean bQueued, LogMessage dst)
  { int mask;  
    if(dstIdx < 0 || dstIdx > outputs.length) 
      throw new IllegalArgumentException("dstIdx fault. Hint: an index, not a mask!");
    if(maxDst <= dstIdx){ maxDst= dstIdx+1; }
    /*
    if(dstIdx > nrofMixedOutputs)
    { mask = dstIdx << nrofMixedOutputs;
    }
    else
    { mask = 1 << dstIdx;
    }
    if(bQueued)
    { mask |= mDispatchInDispatcherThread;
    }
    else
    { mask |= mDispatchInCallingThread;
    }
    */
    this.outputs[dstIdx].outputIfc = dst;
    this.outputs[dstIdx].dstInDispatcherThread = bQueued;
    this.outputs[dstIdx].name = name;
  }
  
  
  /**Sets the output dispatch bits for the given message number range.
   * @param fromIdent The first ident number for which the dispatch bit is valid.
   * @param toIdent The last ident number for which the dispatch bit is valid.
   * @param dst Bit or index for the destination for the given output range.
   *            An or-value {@link #mDispatchWithIndex} can be added. If this Bit is set,
   *            the rest of bits are the index of the output, greater as {@link #nrofMixedOutputs}.
   *            In this case mode should be {@link #mSet}.
   *            <br>
   *            If the bit {@link #mDispatchWithIndex} isn't set, the bits 0..23 describe one or more outputs.
   *            <br>
   *            If the bit combination of dst isn't valid, an invalidArgumentException is thrown.
   * @param mode Ones of {@link #mAdd}, {@link #mSet}, {@link #mRemove} to presribe,
   *             what to do with dst.
   * @param level
   * @return
   */
  public final int setOutputRange(final int fromIdent, int toIdent, final int dst, final int mode, final int level)
  { /**Found index where the idents are arranged in . */
    int idx1, idx2;
    final int maskSet = completeDispatchInThreadBits(dst); 
    if(toIdent == Integer.MAX_VALUE)
    { //the range is from ...inclusive to, let the MAX_VALUE outside, because there should be and end entry,
      toIdent = Integer.MAX_VALUE -1; 
    }
    idx1 = Arrays.binarySearch(listIdents, 0, actNrofListIdents, fromIdent);
    if(idx1 < 0) idx1 = -idx1 -2;  //example: nr between idx=2 and 3 returns -4, converted to 2
    if(idx1 < 0) idx1 = 0;        //if nr before idx = 0, use properties of msg nr=0
    //at idx1 : ident0  <= fromIdent
    
    int ident2 = listIdents[idx1+1];  //the ident after the fromIdent-area
    /**The first index at which the mask is changed. */ 
    final int idx1Mask;
    /**The next index after last index which mask is changed. */
    final int idx2Mask;
    if(toIdent < ident2)
    { //the whole range fromIdent...toIdent is at or between idx1...idx1+1 
      idx1Mask = insertIdent(idx1, fromIdent, toIdent);
      idx2Mask = idx1Mask +1;
    }
    else
    { int ident1 = listIdents[idx1+1];  
      idx1Mask = insertIdent(idx1, fromIdent, ident1-1);  //inserts after idx1, if necessary
      
      idx2 = Arrays.binarySearch(listIdents, 0, actNrofListIdents, toIdent+1);
      if(idx2 < 0) idx2 = -idx2 -2;  //example: nr between idx=2 and 3 returns -4, converted to 2
      
      //at idx2 : ident3 <= toIdent //<= ident3
      int ident3 = listIdents[idx2+1];
      @SuppressWarnings("unused")
      int mask3 = listBitDst[idx2]; 
      idx2Mask = insertIdent(idx2, toIdent+1, ident3-1);    //inserts before idx2+1, if necessary
    }
    for(int imask = idx1Mask; imask < idx2Mask; imask++)
    { switch(mode)
      { case mSet: listBitDst[imask] = maskSet; break; 
        case mAdd: listBitDst[imask] |= maskSet; break;
        case mRemove: listBitDst[imask] &= ~ (maskSet & mDispatchBits); break;
        default: throw new IllegalArgumentException("failed mode");
      }  
    }
    return 0;
  }

  
  
  
  
  
  /**Sets the output from a String content.
   * Syntax-Example:
   * <pre>
   * 123..512:0x27  //It is a bit mask, all messages from 123 to inclusive 521 are set to channels 0,1,2 and 5  
   * 1001:+CON.      //Message 1001 is sent additional to CON
   * </pre>
   * @param ctrl The control String.
   * @param errorBuffer A Buffer to assign an error String: 
   *        This method doesn't allocate any memory, therefore no String concatenation is done. 
   *        Instead an error String is assembled in the buffer using append.
   *        This argument may be null, than only a short hint of error is returned as constant string
   *        or as substring from ctrl. 
   * @return null if all ok, otherwise an error hint. The errorBuffer contains an particularly text. 
   */
  public final String setOutputFromString(String ctrl, StringBuffer errorBuffer)
  {
    String sError = null;
    if(errorBuffer != null)
    { errorBuffer.setLength(0);
    }
    /** @java2c=stackInstance.*/
    StringPart spCtrl = new StringPart(ctrl);
    spCtrl.setIgnoreWhitespaces(true);
    spCtrl.setIgnoreComment("/*", "*/");
    spCtrl.setIgnoreEndlineComment("//");
    boolean continueAll = true;
    try
    { do
      { int fromIdent, toIdent;
        if(spCtrl.seekNoWhitespaceOrComments().length() == 0)
        {
          continueAll = false;  //finish.
        }
        else if(spCtrl.scanInteger().scanOk())
        { fromIdent = (int)spCtrl.getLastScannedIntegerNumber();
          toIdent = -1;
          if(spCtrl.scan("..").scanInteger().scanOk())
          { toIdent = (int)spCtrl.getLastScannedIntegerNumber();
          }
          if(!spCtrl.scan(":").scanOk())
          { 
            sError = toIdent == -1 ? "\":\"or \"..\" expected" : "\":\" expected";
          }
          else
          { if(toIdent == -1){ toIdent = fromIdent; }
            if(spCtrl.scanHexOrDecimal(8).scanOk())
            { int dst = (int)spCtrl.getLastScannedIntegerNumber();
              setOutputRange(fromIdent, toIdent, dst, mSet, 3);
              if(!spCtrl.scan(";").scanOk())
              { sError = "\";\" expected after number-output.";
              }
            }
            else
            { boolean continueEntry = true;
              int mode = mSet;
              do
              { String sOutput = null;
                if(spCtrl.scan("+").scanOk()){ mode = mAdd; }
                else if(spCtrl.scan("-").scanOk()){ mode = mRemove; }
                else if(spCtrl.scan(";").scanOk())
                { continueEntry = false; 
                }
                else
                { if(mode != mSet)
                  { sError = "\"+\" or \"-\" or \";\" expected";
                  }
                }
                if(continueEntry && sError == null)
                { if(spCtrl.scanIdentifier().scanOk())
                  { sOutput = spCtrl.getLastScannedString();
                  }
                  else
                  { sError = "\"DST\" expected, DST should be an identifier.";
                  }
                  if(sError == null)
                  { assert(sOutput != null);
                    int bitOutput = -1;
                    //search the ident
                    int idxDst = 0; 
                    while(idxDst < outputs.length && bitOutput == -1)
                    { if(outputs[idxDst].name != null && outputs[idxDst].name.equals(sOutput))
                      { bitOutput = 1<<idxDst;
                      }
                      else
                      { idxDst++; 
                      }
                    }
                    if(bitOutput == -1)
                    { if(errorBuffer != null)
                      { errorBuffer.append("Output not found:").append(sOutput);
                      }
                      sError = sOutput;  //short variant if buffer isn't given.
                      continueAll = false;
                    }
                    else
                    {
                      setOutputRange(fromIdent, toIdent, bitOutput, mode, 3);
                      mode = -1;  //other mode as mSet expected.
                    }
                  }
                }  
              }while(sError == null && continueEntry && continueAll);
            }//if hexInt else ident;
          }//: scanned
        }
        else
        { sError = "Number for first message to dispatch expected";
          continueAll = false;  
        }
             
      } while(continueAll && sError == null);
    }
    catch(ParseException exc)
    { sError = exc.getMessage(); 
    }
    if(  sError != null      //any error 
      && errorBuffer!= null  //the error buffer is given
      && errorBuffer.length() == 0 //and it is nothing noted there.
      )
    { /**Write the error in the buffer, with fault input string position. */
      int nrofCharsRest = errorBuffer.capacity() - sError.length() - 5; 
      /**Prevent Buffer expansion, use rest size. */
      errorBuffer.append(sError).append(" at:").append(spCtrl.getCurrent(nrofCharsRest));
    }
    return sError; 
  }
  
  
  
  /**Writes the msg dispatching outputs in file. 
   * The form is the same which are used for {@link #setOutputFromString(String, StringBuffer)}.
   * @param file The file should be opened already.
   *        Hint: A simple writer isn't used because Java2C don't able to translate it yet. (Version 0.84)
   * @return true if no error.       
   */
  public final boolean reportOutput(FileWriter file)
  { boolean bOk = true;
    try
    { /**A temporary buffer, in C in Stack. Do not use String concatenating, because no dynamically mem!
       * @java2c=fixStringBuffer, stackInstance.
       */
      
      file.write("//Syntax-Example\n");
      file.write("//  1200..1257: +File +qCON -CON;\n");
      file.write("//  4567:File;\n");
      file.write("//Ident number from..to or only one ident number.\n");
      file.write("//\":\" and \";\" are obligatory. Whitespaces in a line and line end comment are allowed.\n");
      file.write("//if first DST without \"+\" or \"-\": Redirect first only to this destination, all existing dst are deleted.\n");
      file.write("//All following dst have to be separated with \"+\" or \"-\":\n");
      file.write("//\"+\" means: use Dst, \"-\" means, don't use, delete if exists before.\n");
      file.write("//at exampe switch off file output for a single ident, than write:\n");
      file.write("//1234:-File;  \n");
      file.write("\n//All existing dst (destinations):\n");
    
      /** @java2c=stackInstance.*/
      StringBuffer line = new StringBuffer(200);
      
      for(int ii = 0; ii < maxDst; ii++)
      { line.setLength(0);
        Output dst = outputs[ii]; 
        if(dst.outputIfc != null)
        { line.append("//").append(ii).append(": ").append(dst.name);
          if(dst.dstInDispatcherThread){ line.append(" - queued");  }
          line.append(";\n");
          file.write(line.toString());
        }
      }
      file.write("\n//All existing dispatching entries:\n");
      for(int ii = 0; ii < actNrofListIdents-1; ii++)
      { int ident1 = listIdents[ii];
        int ident2 = listIdents[ii+1]-1; //end ident
        
        line.setLength(0);
        line.append(ident1);
        if(ident2 != ident1)
        { /**not single message ident */
          line.append("..").append(ident2);
        }
        line.append(":");
        int bitDst = listBitDst[ii];
        boolean bFirst = true;
        int maskBitDst = 1;
        for(int iDst = 0; iDst < maxDst; iDst++)
        { if( (bitDst & maskBitDst)!=0)
          { Output dst = outputs[iDst];
            if(dst.outputIfc != null)
            { if(!bFirst){ line.append("+"); }
              else { bFirst = false; } 
              line.append(dst.name);
            }
          }
          if(iDst < nrofMixedOutputs)
          { /**Test next bit. */
            maskBitDst <<=1;
          }
          else
          { /**A non mixed output: */
            int idxNonMixedDst = ((bitDst & mDstOneOutput) >> nrofMixedOutputs) -1 + nrofMixedOutputs;
            if(idxNonMixedDst >= nrofMixedOutputs)
            { Output dst = outputs[iDst];
              if(dst.outputIfc != null)
              { if(!bFirst){ line.append("+"); }
                else { bFirst = false; } 
                line.append(dst.name);
              } 
            }
          }
        }//for,all dst of one line
        line.append(";\n");
        file.write(line.toString());
      }
    }
    catch(IOException exc)
    { bOk = false;  //end it. write no more lines. It may be disk is full.
    }
    return bOk;  
  }
  
  
  
  
  /**Completes a destination bit mask with the information, whether any destinations are used
   * in the calling thread or in the dispatcher thread. 
   * @param dstBits The input mask
   * @return returns the same bits as in input mask, 
   *         but with Bits {@link mDispatchInDispatcherThread} and/or {@link mDispatchInCallingThread} 
   */
  private final int completeDispatchInThreadBits(int dstBits)
  { int dstBitRet = dstBits;
    /**Assert: The bits should not be negative, because elsewhere >> shifts a 1-bit into, 
     * and the while-loop won't be ended.
     */
    assert(dstBits >= 0);
    int idst = 0;
    while(dstBits != 0) //abort if no bits are set anymore.
    { if(  (dstBits & 1)!=0 ){
        if( outputs[idst].dstInDispatcherThread ) { 
        	dstBitRet |= mDispatchInDispatcherThread; 
        } else { 
        	dstBitRet |= mDispatchInCallingThread; 
        }
      }
      dstBits >>=1;
      idst += 1;
    }
    return dstBitRet;
  }
  








  
  /**Sends a message. See interface.  
   * @param identNumber
   * @param text The text representation of the message, format string, see java.lang.String.format(..). 
   *             @pjava2c=zeroTermString.
   * @param args see interface
   * @java2c=stacktrace:no-param.
   */
   @Override public final boolean  sendMsg(int identNumber, String text, Object... args)
   { /**store the variable arguments in a Va_list to handle for next call.
      * The Va_list is used also to store the arguments between threads in the MessageDispatcher.
      * @java2c=stackInstance.*/
   	 final Va_list vaArgs =  new Va_list(args);  
     return sendMsgVaList(identNumber, OS_TimeStamp.os_getDateTime(), text, vaArgs);
   }

   
   /**Sends a message. See interface.  
    * @param identNumber
    * @param text The text representation of the message, format string, see java.lang.String.format(..). 
    *             @pjava2c=zeroTermString.
    * @param args see interface
    * @java2c=stacktrace:no-param.
    */
    @Override public final boolean  sendMsgTime(int identNumber, final OS_TimeStamp creationTime, String text, Object... args)
    { /**store the variable arguments in a Va_list to handle for next call.
       * The Va_list is used also to store the arguments between threads in the MessageDispatcher.
       * @java2c=stackInstance.*/
    	final Va_list vaArgs =  new Va_list(args);  
      return sendMsgVaList(identNumber, creationTime, text, vaArgs);
    }

    
  
  
  /**Dispatches all messages, which are stored in the queue. 
   * This routine should be called in any user thread respectively it is called in TODOx1.
   * @return number of messages which are found to dispatch, for statistic use.
   */
  public final int dispatchQueuedMsg()
  { int nrofFoundMsg = 0;
    /**Limit the number of while-loops to prevent thread hanging. */
    int cntDispatchedMsg = 100;
    boolean bCont;
    Entry firstNotSentMsg = null;
    do
    { Entry entry = listOrders.poll();
      bCont = (entry != null && entry != firstNotSentMsg);
      if(bCont)
      { nrofFoundMsg +=1;
        int notDispatchedDst = dispatchMsg(entry.dst, true, entry.ident, entry.timestamp, entry.text, entry.values.get_va_list());
        if(true || notDispatchedDst == 0)
        { entry.values.clean();
          entry.ident = 0;  
          freeOrders.offer(entry);
        }
        else
        { /**Not all destinations are processed. 
           * It is possible that a file is in at-least-closing time.
           */ 
          if(firstNotSentMsg == null)
          { firstNotSentMsg = entry;
          }
          /**re-assign the entry to the orders to process later. 
          { SimpleDateFormat testDate = new SimpleDateFormat("mm:ss,SSS");
            String sTime = testDate.format(entry.timestamp);
            System.out.println("re-enter " + sTime); //String.format("%1$2ts.%1$2tQ", new Date()));
          }
          */
          //parkedOrders.offer(entry);
        }
      }
      
    }while(bCont && (--cntDispatchedMsg) >=0);
    if(cntDispatchedMsg == 0)
    { /**max nrof msg are processed:
       * The while-queue is left because a thread hanging isn't able to accept.
       * The rest of messages, if there are any, will stay in the queue.
       * This routine is repeated, if any other message is added to the queue
       * or after a well-long delay. The situation of thread hanging because any error
       * is the fatal version of software error. Prevent it. 
       */
      //printf("MsgDisp: WARNING to much messages in queue\n");
      /**Count this situation to enable to inspect it. */  
      testCnt.tomuchMsgPerThread +=1;
    }
    return nrofFoundMsg;
  }
  
  
  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  final void stop(){}


  /**close and flush forces the dispatching of the messages in the queue. 
   * @see org.vishia.util.LogMessage#close()
   */
  @Override
  public void close()
  { //flush(); //do the same
    dispatchQueuedMsg();
  }


  /**flush forces the dispatching of the messages in the queue. 
   * @see org.vishia.util.LogMessage#close()
   */
  @Override
  public void flush()
  { dispatchQueuedMsg();
  }

  @Override
  public boolean isOnline()
  { return true;
    
  }

  /**Outputs the queued messages calling {@link LogMessage#flush()} for all queued outputs.
   * This method can be called in the dispatcher thread cyclically, as opposite to the 
   * {@link DispatcherThread#start()}, where this routine is called too. 
   */
  public final void tickAndFlushOrClose()
  { dispatchQueuedMsg();
    for(int ix = 0; ix < outputs.length; ix++){
    	Output output = outputs[ix];
    	if(output.dstInDispatcherThread){
    		output.outputIfc.flush();
    	}
    }
  }
  
  
  /**Class to organize a dispachter thread.
   * 
   *
   */
  public class DispatcherThread extends Thread
  {
  	private final int cycleMillisec;
  	
  	/**Initializes the Thread for dispaching messages.
  	 * @param cycleMillisec The cycle time for output.
  	 */
  	public DispatcherThread(int cycleMillisec)
  	{
  	  super("MsgDisptch");
  		this.cycleMillisec = cycleMillisec;
  	  start();
  	}
  	
  	@Override public void run()
  	{
  		while(true)
  		{ try{ Thread.sleep(cycleMillisec);} catch(InterruptedException exc){}
        tickAndFlushOrClose();
  		}
  	}
  }
  
  
}
