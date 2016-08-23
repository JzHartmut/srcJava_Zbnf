package org.vishia.event;

import java.util.EventObject;

/**This is the super class for orders, which should be added to a {@link EventThread} instance.
 * It is designated as EventObject because it can be stored in the same queue where events are stored.
 * It is designated as EventConsumer because it can be triggered for run in a {@link EventThread#triggerRun()}
 * @author Hartmut Schorrig
 *
 */
public abstract class EventTimeOrderBase extends EventTimeout //Object //implements EventConsumer
{

  private static final long serialVersionUID = 1998821310413113722L;


  /**Version and history.
   * <ul>
   * <li>2015-01-10 Hartmut renamed from <code>OrderForList</code>
   * <li>2014-02-23 Hartmut removed from the component srcJava_vishiaGui to the srcJava_vishiaBase 
   *   because it is commonly able to use.
   * <li>2012-02-14 Hartmut corr: {@link #addToList(GralGraphicThread, int)}:
   *   For time saving: If an instance is added already and its new execution time
   *   is up to 5 ms later, nothing is done. It saves time for more as one call of this routine
   *   in a fast loop.
   * <li>2012-02-14 Hartmut corr: It was happen that an instance was designated with {@link #bAdded}==true
   *   but it wasn't added, Therefore on {@link #addToList(GralGraphicThread, int)} it is removed
   *   from list and added newly. It is more save. 
   * <li>2012-01-26 Hartmut chg: rename removeFromGraphicThread(GralGraphicThread) 
   *   to {@link #removeFromList(GralGraphicThread)}. It is a better naming because it is removed from the queue
   *   in the graphic thread. This class is only used in that queue. 
   * <li>2012-01-15 Hartmut new: {@link #name} to identify, proper for debugging
   * <li>2012-01-08 Hartmut new: {@link #addToList(GralGraphicThread, int)} and 
   *   {@link #removeFromList(GralGraphicThread)} as thread-safe functions which 
   *   marks the instance as added (for delayed execution, for re-using).
   * <li>2010-06-00 Hartmut created.
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
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String version = "2015-01-17";

  
  /**The name of the dispatch worker, used for debug at least. */
  public final String name;
  
  private int ctDone = 0;
  
  /**It is counted only. Used for debug. Possible to set. */
  public int dbgctDone = 0;
  
  /**True if a thread waits, see {@link #awaitExecution(int, int)}. */
  private boolean reqCtDone = false;

  //private boolean bAdded;
  
  
  
  public EventTimeOrderBase(String name)
  { super();
    this.name = name;
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventTimeOrderBase(String name, EventThreadIfc thread){
    super(null, null, thread);  //no EventSource necessary, no eventConsumer because this is an order.
    this.name = name;
  }
  

  public EventTimeOrderBase(String name, EventConsumer dst, EventThread thread){
    super(null, dst, thread);  //no EventSource necessary, no eventConsumer because this is an order.
    this.name = name;
  }
  

  
  
  
  //@Override public int processEvent(EventObject ev)
  //{ executeOrder(); return 1; }
  
  /**Returns the state of the consumer in a manual readable form. */
  //@Override 
  public String getStateInfo(){ 
    StringBuilder u = new StringBuilder(100);
    u.append(name);
    if(timeExecution >0){
      //TODO some information!
    }
    return u.toString();
  }
  

  
  /**Executes the order. In a graphic thread it handles any request before the system's dispatching routine starts.
   * This method should not be called. Only overridden. It is called from {@link #doExecute()} with freeing the order
   * if the order's time is expired. The user should activate the execution with 
   */
  protected abstract void executeOrder();
  
  /**Executes and sets the execute state to false.
   * Therewith it is added newly on new invocation of {@link #addToList(EventThread, int)} and {@link #addToList(EventThread, int, int)}.
   */
  protected final void doExecute(){ 
    timeExecutionLatest = 0; //set first before timeExecution = 0. Thread safety.
    timeExecution = 0;     //forces new adding if requested. Before execution itself!
    executeOrder();
    dbgctDone +=1;
    ctDone +=1;
    if(reqCtDone){
      synchronized(this){
        notify();
      }
    }
  }
  
  public void timeExecution(long time){ timeExecution = time; }
  
  public long timeExecution(){ return timeExecution; }
 
  
  
  public void addToList(EventThread dst, int delay){ addToList(dst, delay, 0); }
  
  
  /**Adds to the graphic thread or sets a new delay if is added already.
   * If it is added already and the new time to execution is in range max 5 ms later, this routine does nothing.
   * @param dst The graphic thread.
   * @param delay time in milliseconds for delayed execution or 0.
   */
  @Deprecated synchronized public void addToList(EventThread dst, int delay, int delayMax){
    assert(dst == evDstThread);
    long time = System.currentTimeMillis();
    activateAt(time + delay, delayMax == 0 ? 0 : time + delayMax);
    
  }
  
  
  
  
  
  /**Remove this from the queue of dispatch callbacks which are executed in any loop of the
   * graphic thread.
   * @param graphicThread it is the singleton instance refered with {@link GralMng#gralDevice}.
   */
  synchronized public void removeFromList(EventThread dst){
    timeExecution = 0;
    dst.removeTimeOrder(this);
  }
  
  
  /**Gets the information, how many times the routine is executed.
   * Especially it is for quest, whether it is executed 1 time if it is a single-execution-routine.
   * Note that the method should be thread-safe, use synchronized in the implementation.
   * @param setCtDone set the count for a new execution-counting. For example 0.
   * @return The number of times of execution after initializing or after last call of this method.
   */
  synchronized public int getCtDone(int setCtDone) {
    //reqCtDone = true;   //it is to notify, if this routine is called.
    int ctDone = this.ctDone;
    if(setCtDone >=0){ 
      this.ctDone = setCtDone; 
    }
    return ctDone;
  }
  
  
  /**
   * @deprecated it is unnecessary now because {@link #doExecute()} does the work.
   */
  protected synchronized void countExecution()
  { timeExecution = timeExecutionLatest = 0;
    timeExecution = 0;
  }
  
  

  
  /**waits for execution. This method can be called in any thread, especially in that thread, 
   * which initializes the request.
   * @param ctDoneRequested Number of executions requested.
   * @param timeout maximal waiting time in millisec, 0 means wait for ever for execution.
   * @return true if it is executed the requested number of.
   */
  public synchronized boolean awaitExecution(int ctDoneRequested, int timeout)
  { 
    long timeEnd = System.currentTimeMillis() + timeout; 
    boolean bWait;
    do {
      if(this.ctDone < ctDoneRequested ){
        reqCtDone = true;
        long waitingTime = timeEnd - System.currentTimeMillis();
        if(waitingTime > 0 || timeout == 0){
          try{ wait(timeout); } catch(InterruptedException exc){}
          bWait = true;
        } else bWait = false;
      } else bWait = false;
    } while(bWait);
    return(this.ctDone >= ctDoneRequested);
  }
  

  @Override public String toString(){ return name; }  

  
}
