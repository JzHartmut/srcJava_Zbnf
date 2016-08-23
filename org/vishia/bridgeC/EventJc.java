package org.vishia.bridgeC;

import org.vishia.event.EventMsg2;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;

/**This class extends the {@link EventMsg2} only with the capability of deletion from Memory for non-Garbage-Collecting C/C++ usage.
 * @author Hartmut Schorrig
 *
 */
public class EventJc<CmdEnum extends Enum<CmdEnum>, CmdBack extends Enum<CmdBack>> extends EventMsg2<CmdEnum, CmdBack>
{
  
  
  /**Version, history and license
   * <ul>
   * <li>2012-08-28 Hartmut created, the dependency from {@link EventMsg2} to the {@link MemC} should not present in the
   *   pure class {@link EventMsg2} but in this C-adaption package.
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
  public static final int version = 20120828;

  final boolean bPermanent;
  
  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventThread)}
   * before first usage. Use {@link #relinquish()} to release the usage. 
   * 
   */
  public EventJc(boolean permanentInstance){
    super();
    bPermanent = permanentInstance;
  }
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventJc(boolean permanentInstance, EventSource evSrc, EventConsumer consumer, EventThread thread){
    super(evSrc, consumer, thread);
    bPermanent = permanentInstance;
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   * @param callback Another event to interplay with the source of this event.
   */
  public EventJc(boolean permanentInstance, EventSource evSrc, EventConsumer consumer, EventThread thread, EventMsg2<CmdBack, CmdEnum> callback){
    super(evSrc, consumer, thread, callback);
    bPermanent = permanentInstance;
  }

  
  
  @Override public void relinquish(){
    super.relinquish();
    if(!bPermanent){
      MemC.free(this);
    }
    
  }

}
