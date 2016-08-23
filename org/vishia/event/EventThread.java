package org.vishia.event;

import java.io.Closeable;
import java.io.IOException;
import java.util.EventObject;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.util.Assert;
import org.vishia.util.InfoAppend;

/**This class organizes the execution of events in a own thread. The class contains the event queue.
 * @author Hartmut Schorrig
 *
 */
public class EventThread implements Runnable, Closeable, InfoAppend
{
  /**Version, history and license.
   * <ul>
   * <li>2015-01-04 Hartmut chg: Now uses the Java-standard {@link EventObject} instead a specific one,
   *   but some methods are supported for {@link EventMsg}, especially {@link EventMsg#notifyDequeued()} and {@link EventMsg#stateOfEvent}.  
   * <li>2013-05-12 Hartmut chg: Now the thread starts and ends automatically if {@link #startThread()}
   *   is not invoked.
   * <li>2012...improved
   * <li>2011-12-27 Hartmut creation for event concept.
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
  public static final int version = 20130513;

  
  
  
  protected final ConcurrentLinkedQueue<EventObject> queueEvents = new ConcurrentLinkedQueue<EventObject>();

  /**The state of the thread
   * <ul>
   * <li>'.' non initialized.
   * <li>'s' started
   * <li>'r' Running, an event is processed
   * <li>'w' Waiting because the queue is empty
   * <li>'c' Should be closed, finished
   * <li>'x' finished.
   * 
   */
  protected char stateOfThread = '.';  
  
  protected final String threadName;

  protected Thread thread;
  
  private boolean startOnDemand;
  
  private int ctWaitEmptyQueue;
  
  protected int maxCtWaitEmptyQueue = 5;
  
  private boolean preserveRecursiveInfoAppend;
  
  public EventThread(String threadName)
  {
    this.threadName = threadName;
  }
  
  
  /**Creates and starts the thread. If this routine is called from the user, the thread runs
   * till the close() method was called. If this method is not invoked from the user,
   * the thread is created and started automatically if {@link #storeEvent(EventMsg)} was called.
   * In that case the thread stops its execution if the event queue is empty and about 5 seconds
   * are gone.  */
  public void startThread(){ 
    thread = new Thread(this, threadName);
    startOnDemand = false;
    thread.start(); 
  }
  
  
  public void storeEvent(EventObject ev){
    if(ev instanceof EventMsg) { ((EventMsg)ev).stateOfEvent = 'q'; }
    queueEvents.offer(ev);
    if(thread == null){
      startThread();
      startOnDemand = true;
    } else {
      synchronized(this){
        if(stateOfThread == 'w'){
          notify();
        } else {
          //stateOfThread = 'c';
        }
      }
    }
  }
  
  
  
  /**Removes this element from its queue if it is in the queue.
   * If the element is found in the queue, it is designated with 
   * @param ev
   * @return
   */
  public boolean removeFromQueue(EventMsg ev){
    boolean found = queueEvents.remove(ev);
    if(found){ 
      ev.stateOfEvent = 'a'; 
    }
    return found;
  }
  
  
  
  /**Applies an event from the queue to the destination in the event thread. 
   * This method should be overridden if other events then {@link EventMsg} are used because the destination of an event
   * is not defined for a java.util.EventObject. Therefore it should be defined in a user-specific way in the overridden method.
   * This method is proper for events of type {@link EventMsg} which knows their destination.
   * @param ev
   */
  protected void applyEvent(EventObject ev)
  {
    if(ev instanceof EventMsg){
      EventMsg<?> event = (EventMsg<?>) ev;
      event.stateOfEvent = 'e';
      event.notifyDequeued();
      try{
        event.donotRelinquish = false;   //may be overridden in processEvent if the event is stored in another queue
        event.evDst().processEvent(event);
      } catch(Exception exc) {
        CharSequence excMsg = Assert.exceptionInfo("EventThread.applyEvent exception", exc, 0, 50);
        System.err.append(excMsg);
        //exc.printStackTrace(System.err);
      }
      event.relinquish();  //the event can be reused, a waiting thread will be notified.
    }
  }
  
  
  
  
  
  /**Run method of the thread.
   * @see java.lang.Runnable#run()
   */
  @Override final public void run()
  { stateOfThread = 'r';
    do { 
      try{ //never let the thread crash
        EventObject event;
        if( (event = queueEvents.poll()) !=null){
          this.ctWaitEmptyQueue = 0;
          synchronized(this){
            if(stateOfThread != 'x'){
              stateOfThread = 'b'; //busy
            }
          }
          if(stateOfThread == 'b'){
            applyEvent(event);
          }
        } else {
          if(!startOnDemand   //wait anytime 
            || ++this.ctWaitEmptyQueue < this.maxCtWaitEmptyQueue //or count the empty wait cycles.
            ){
            synchronized(this){
              if(stateOfThread != 'x'){  //exit?
                stateOfThread = 'w';      //w = waiting, notify necessary
                try{ wait(1000); } catch(InterruptedException exc){}
                if(stateOfThread == 'w'){ //can be changed while waiting, set only to 'r' if 'w' is still present
                  stateOfThread = 'r';
                }
              }
            }
          }
        }
      } catch(Exception exc){
        System.out.println(Assert.exceptionInfo("EventThread - unexpected Exception; ", exc, 0, 7));
        exc.printStackTrace(System.err);
      }

    } while(stateOfThread != 'c' && this.ctWaitEmptyQueue < this.maxCtWaitEmptyQueue);
    stateOfThread = 'x';
    thread = null;
  }

  
  /**Returns true if the current thread is the thread which is aggregate to this EventThread.
   * It means the {@link #run()} method has called this method.
   * @return false if a statement in another thread checks whether this EventThread runs.
   */
  public boolean isCurrentThread() {
    return thread == Thread.currentThread();
  }
  
  public char getState(){ return stateOfThread; }

  @Override public void close() throws IOException
  { synchronized(this){
      if(stateOfThread == 'w'){
        notify();
      }
      stateOfThread = 'c';
    }  
  }
    
  
  @Override public CharSequence infoAppend(StringBuilder u) {
    if(u == null) { u = new StringBuilder(); }
    u.append("Thread ");
    u.append(threadName);
    if(!preserveRecursiveInfoAppend){
      preserveRecursiveInfoAppend = true;
      EventObject[] ev1 = queueEvents.toArray(new EventObject[20]); //presumed, no more as 20 elements in the queue, typically 1..0. 
      char sep = ':';
      for(EventObject ev: ev1){
        if(ev !=null) {
          u.append(sep).append(' ');
          u.append(ev.toString());
          sep = ',';
        }
      }
      preserveRecursiveInfoAppend = false;
    }
    u.append("; ");
    return u;
  }
  
  /*no: Returns only the thread name. Note: Prevent recursively call for gathering info.
   */
  @Override public String toString() { if(preserveRecursiveInfoAppend) return threadName; else return infoAppend(null).toString(); } 
  
}
