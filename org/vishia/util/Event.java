package org.vishia.util;

import java.util.concurrent.atomic.AtomicLong;

/**Base class for all events in a event driven software or for communications.
 * Events may contain data. Special data are contained in derived classes of this. The type of the event
 * can be checked using <code>event instanceof DerivedEvent</code>. The id may be only a hint.
 * <br><br>
 * <b>What is a event driven software</b>?<br>
 * There are 4 types of organization of the cooperation in a multi thread system or in a distributed system:
 * <ol>
 * <li><b>request, wait and notify</b>: The action which should be done by another thread or in another remote device
 *   is requested. Then the requesting thread waits for the execution (for response, for answer, for notify). 
 *   
 * <li><b>request and poll</b>: The action is requested. Then the requesting thread does any other action, 
 *   it can't be used the result from the request yet.
 *   But the thread polls cyclically (may be for some milliseconds or longer) the result of the requested order.
 * <li><b>callback</b>: The action which should be done by another thread or in another remote device
 *   is requested. With the request a callback reference is given. If the request is executed (received response etc),
 *   the other thread calls the callback method. In the callback method the results of the request can be processed.
 * <li><b>event driven</b>: The action is requested usual by sending an event to the executer instance. 
 *   The requested thread does nothing with any response in the immediate programming environment. 
 *   It requests only. But the executer instance may send an event back if it has finished the execution
 *   or if any other messages should be given.<br>
 *   Events are stored stored in a queue firstly. The queue will be checked by the 'event execution thread'.
 *   It is possible to have only one event execution thread in the system. In this case the execution will be done
 *   in the same thread, but to any later time (if the event is gotten from the queue).<br>
 *   There may be some more event execution threads, for example a thread which organizes communication.
 *   The execution of any request and the execution of the response is binded to the thread, 
 *   which polls the receivers event queue.<br>
 *   It is possible too to request any action non-event-driven, maybe by sending a request via Ethernet communication
 *   or adequate. But the response may be event-driven. The receiver thread of the communication
 *   puts the event in the queue. The execution of the response is done in the event-queue-thread.
 *   This is a proper mechanism to decouple activity. If the receiver thread would execute anything
 *   in another module, this execution may be hanging or may cause any exceptions which disturbs 
 *   the receiving thread so the whole receiving functionality may hang.  
 * </ol>
 * This 4 types may be present in a users program. The following remarks should be recognized:
 * <ol>
 * <li><b>request wait and notify</b>: This is the simplest and basic methodology for inter-thread-cooperation,
 *    supported by the basic operations Object.wait() and Object.notify() as basic feature provided in Java.
 *    <br>
 *    The waiting thread can't execute any other things while waiting. A timeout of waiting may be good 
 *    for exception situations. 
 *    <br>
 *    A user may use this methodology for simple functionalities in the application software. But it needs
 *    an extra thread if other things should be done also. Especially in a graphic environment (which is event driven
 *    for graphic operations), a wait for less milliseconds may be ok, but if the response is delayed
 *    or it isn't received, the whole graphic execution is hanging on. (Do you know this effect in some
 *    applications on PC operations systems?).   
 *    <br>Therefore it may be recommended that this methodology should not be used as the favorite solution.
 *    in application programming.
 *    <br>The wait and notify is proper for basic implementations for example to organize a event driven system.
 *    The thread waits for events in the queue. If any other thread put an event in the queue, it notifies
 *    the waiting thread. That event execution thread has only one mission: providing the events to its destinations.
 *    If there are no events, it should wait and sleep.
 * <li><b>request and poll</b>: This methodology may be used in cyclic execution systems for example for
 *   analog-like controlling of signals. The cyclic execution tests results from communication or inter-thread-communication
 *   in any cycle of execution. 
 * <li><b>callback</b>: This is a simple methodology to prevent waiting. But the callback method is executed 
 *   in the other thread, which is responsible to the execution of the request or which is the receiving thread
 *   of any communication with a remote device. Maybe some thread mutex mechanism should be necessary.
 * <li><b>event driven</b>: This methodology needs a system support in user space: The event execution thread. 
 *   That thread polls the event queue. It waits if the queue is empty. If an event is gotten, it is
 *   distributed to its destination. The destination is any instance of any class which offers a the 
 *   event processing method. 
 *   <br>
 *   The event driven methodology can be esteemed as the best for complex systems.     
 * </ol>  
 * <br><br>
 * <b>Usage of this Event class</b>:<br>
 * This class supplies a common use-able base for event driven programming. It hasn't any dependencies 
 * to other classes of this package or component except {@link EventConsumer} and {@link EventThread}. 
 * (It may be good if this class is a member of the java/lang package).
 * The event mechanisms are well known and used in some Java applications for example in graphic (AWT, SWT)
 * or in special frameworks. That event classes are similar, but they are embedded in their environment.
 * If they are used, unnecessary dependency are caused. <br>
 * This class supports event driven programming in the language C too. It is designed to use in simple C applications
 * which where translated with the vishia Java2C translator. Thats why the usage of inheritance 
 * for any kind of events isn't the first choice. This Event class contains enough data for simple and usual
 * usages.
 * <br><br>
 * <b>Usage for callback</b>:<br>
 * A request should be called with an reference to an instance to this Event class as parameter. 
 * This Event class contains the reference to the callback instance {@link #callback}. 
 * The callback instance implements the {@link EventConsumer}-interface. In this manner the request receiver
 * can invoke a callback using {@link #callback}.{@link EventConsumer#processEvent(Event)}. Note that this callback
 * is executed in the request receiver's thread.
 * <br><pre>
 *   Thread                     class             another Thread             
 *  Requester               Request preparer     Request executer                      
 *      |                          |                    |                       
 *      |->request(data, Event)--->|-->any queue------->|                                              
 *      |                          | (Event stored)     |                      
 *      |                          |                    |
 *      |                                        {executes request}
 *      |                                               |
 *      |                                       {fills Event with data}
 *      |<-------------Event.dst.processEvent(ev)-------|
 *      |                  it is a callback             |
 * </pre>
 * <br>
 * <br>
 * <b>Usage for event driven mechanism</b>:<br>
 * The difference between callback and event driven is marginal but substantial. Instead invocation of
 * a callback in the other thread, the event is put in a queue. The event execution thread polls the event
 * and invokes the callback method to the destination class to consume the event. The difference is only:
 * The event is stored in a queue, the callback is invoked by another thread. But this difference is
 * substantial.
 * <br><pre>
 *   Thread    Thread           class             another Thread             
 *  Requester EventThread   Request preparer     Request executer                      
 *      |         |                |                    |                       
 *      |->request(data, Event)--->|-->any queue------->|                                              
 *      |         |                | (Event stored)     |                      
 *      |         |                |                    |
 *      |         |                              {executes request}
 *      |         |                                     |
 *      |         |                             {fills Event with data}
 *      |         |<---Event.dst.processEvent(ev)-------|
 *      |         |        it is a callback             |
 *      |         |                                     |
 *      |         |---------------------+               |
 *      |<---Event.dst.processEvent(ev)-+      
 *      |     it is a callback              
 *      |         |                                     |
 *      |         |                                     |
 * </pre>
 * <br><br>
 * <b>Request and response in an event driven system</b>
 * In a common event driven system their isn't a thinking of 'request' and 'response'. Any event inducing
 * is independent of any other event. But usual mechanism uses 'request' and the associated 'response'.
 * For that methodology it is possible to give the event instance as parameter of the request already.
 * The event parameter contains the {@link #callback} for the response. This may be a instance in knowledge
 * of the requesting instance (maybe an inner class for implementation of {@link EventConsumer})
 * or the requesting instance itself. So the requested instance should not be known the requester permanently,
 * but only for the pending request.
 * <br><br>
 * <b>Remote device, communication</b>:<br>
 * If any action should be executed in any other device which is connected via any communication maybe
 * via Ethernet, or serial or any other special, the request for the execution has to be sent. This request
 * can contain this Event class as parameter for the response. <br>  
 * The execution occurs independently in the other device then. The communication may be work with
 * request and response. If the action is received and accepted, a response will be sent back which may
 * contain any answer data. The response of the device is received by the receiver thread of the communication.
 * <br>
 * The communication should store and manage a association between request to the other device and response
 * from the device. The association may be realized using a sequence, order or commission number. If the
 * associated response is received, the Event of the request is found out using the commission or sequence
 * number. Then the communication-receive-thread can use the Event instance to invoke the callback
 * or put the event in the queue of dst.
 * <br><pre>
 *   Thread                     class                Thread             
 *  Requester               Communication-tx     Communication-rx       Remote device
 *      |                          |                    |                     |
 *      |->request(data, Event)--->|---->telg: request to device(data)------->|(receives commission nr)
 *      |                          | (Event stored)     |                     |
 *      |                          |                    |               {executes request}
 *      |                                               |                     |
 *      |                                               |<--telg: response----|(uses same commission nr)
 *      |                                    {searches Event                  |
 *      |                                    {fills Event with data}          |
 *      |<--------------------response(Event)-----------|                     |
 *      |callback or Event queue                        |                     |
 *      |                                               |                     |
 *      |                                               |                     |
 * </pre>
 * <br><br>
 * Where is this Event class able to use:
 * <ul>
 * <li>As item in the event queue using the {@link EventThread}
 * <li>As callback item for communication for example in {@link FileRemote}.
 * </ul> 
 * 
 * @author Hartmut Schorrig
 *
 */
public class Event
{
  
  /**Version, history and license
   * <ul>
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. The event has more elements for forward and backward now.
   * <li>2012-07-28 renamed src, now {@link #refData}. It is not the source (creator) of the event
   *   but a value reference which may be used especially in the callback ({@link #callback}).
   *   Because it is private and the getter method {@link #getRefData()} is duplicated, the
   *   old routine {@link #getSrc()} is deprecated, it is downward compatible still. 
   * <li>2012-03-10 Hartmut new: {@link #owner}, {@link #forceRelease()}. 
   *   It is a problem if a request may be crased in a remote device, but the event is reserved 
   *   for answer in the proxy. It should be freed. Events may be re-used. 
   * <li>2012-01-22 Hartmut chg: {@link #use(long, int, Object, EventConsumer)} needs the dst as parameter.
   * <li>2012-01-05 Hartmut improved: {@link #callbackThread}, {@link #commisionId} instead order, more {@link #data2} 
   * <li>2011-12-27 Hartmut created, concept of event queue, callback need for remote copy and delete of files
   *   (in another thread too). A adequate universal class in java.lang etc wasn't found.
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
  public static final int version = 20120311;
  
  /**The current owner of the event. It is that instance, which has gotten the event instance
   * by any method invocation as parameter,
   * and stores this event till it is need for answer.
   * TODO not used yet. Maybe a List<EventOwner>
   */
  private EventOwner owner;
  
  /**The queue for events of the {@link EventThread} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst.{@link EventConsumer#processEvent(Event)} should be called immediately. */
  private EventThread dstThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  /*package private*/ EventConsumer dst;
  
  /**The queue for events of the {@link EventThread} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst.{@link EventConsumer#processEvent(Event)} should be called immediately. */
  private EventThread callbackThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  /*package private*/ EventConsumer callback;
  
  /**Any number to identify. It is dst-specific. */
  protected int cmd;
  
  protected int answer;
  
  /**The commission number for the request, which may be answered by this event. */
  protected long orderId;
  
  /**Timestamp of the request. It is atomic because the timestamp may be an identification
   * that the event instance is in use (for new-obviating usage like C-programming). */
  private AtomicLong dateCreation = new AtomicLong();
  
  /**Any value of this event. Mostly it is a return value. */
  public int data1, data2;
  
  /**Any value reference especially to return any information in the {@link #callback()}. */ 
  public Object oData;
  
  /**A referenced instance for the event. This reference is private because it should be set
   * calling {@link #Event(Object, EventConsumer)} or {@link #use(long, int, Object, EventConsumer)}
   * only. The calling environment determines the type. 
   * see {@link #getRefData()}. */
  private Object refData;
  
  public Event(Object refData, EventConsumer consumer){
    this.refData = refData; this.callback = consumer;
  }
  
  
  public int answer(){ return answer; }
  
  /**Check whether this event is in use and use it. An event instance can be re-used. If the order or the dateOrder
   * is set, the event is in use. In a target communication with embedded devices often the communication
   * resources are limited. It means that only one order can be requested at one time, and the execution
   * should be awaited. The usage of an re-used event for such orders can be help to organize the
   * requests step by step. If the answer-event instance is in use, a request is pending.
   *  
   * @return true if the event instance is able to use.
   */
  public boolean use(long order, int cmd, Object refData, EventConsumer dst){ 
    if(dateCreation.compareAndSet(0, System.currentTimeMillis())){
      this.orderId = order;
      this.cmd = cmd;
      this.refData = refData;
      this.callback = dst;
      return true;
    }
    else return false;
  }
  
  
  
  /**Returns true if the event is in use. Events may be re-used. That may be necessary if a non-dynamic
   * memory organization may be need, for example in C-like programming. It is possible anyway if actions
   * are done only one after another. In that kind the event instanc is created one time,
   * and re-used whenever it is needed. A re-using should wait for answer and set {@link #consumed()}.
   * If the answer hangs, {@link #forceRelease()} may be called.
   * @return true if it is ready to re-use.
   */
  public boolean isOccupied(){ return dateCreation.get() !=0; }
  
  
  /**Forces the release of the event instance to re-usage the event. 
   * If the event is in use, the owner will be notified
   * calling {@link EventOwner#remove(Event)} that the event should be released.
   * @return false only if the event is not released from the owner. 
   * Then the action should be repeated.
   */
  public boolean forceRelease() {
    boolean bOk = true;
    if(owner !=null){ bOk = owner.remove(this); }
    if(bOk){
      consumed();
    }
    return bOk;
  }
  
  
  /**Releases the event instance. It is the opposite to the {@link #consumed()} method.
   * The {@link #dateCreation} is set to 0 especially to designate the free-state of the Event instance.
   * All other data are reseted, so no unused references are hold.  
   */
  public void consumed(){
    this.cmd = 0;
    this.answer = 0;
    this.refData = null;
    this.dst = null;
    this.dstThread = null;
    this.callback = null;
    this.callbackThread = null;
    this.orderId = 0;
    data1 = data2 = 0;
    oData = null;
    owner = null;
    dateCreation.set(0);
  }

  
  
  /**Sends this event to the destination instance.
   * Either the element {@link #callbackThread} is not null, then the event is put in the queue
   * and the event thread is notified.
   * Or the dstQueue is null, then a callback is invoked using {@link #callback}.{@link EventConsumer#processEvent(Event this)}
   */
  public void callback(){
    if(callbackThread !=null){
      callbackThread.storeEvent(this);
    } else {
      callback.processEvent(this);
    }
  }
  
  
  /**Returns the stored src argument of construction.
   * Especially the creator of this Event instance knows the instance type
   * of src because one have written in it. The creator can re-cast to that instance type 
   * and read out any information. This approach is proper because the event was created in the past
   * with a designated data constellation. That constellation is stored with the src-reference
   * in the event till the callback execution. There isn't any other mechanism necessary to store
   * that data constellation.
   * <br><br>
   * A receiver of the event may deal with the src if the event id describes a defined type
   * and a 'instanceof' call is done.
   * 
   * @return the source instance which was stored either calling the ctor {@link #Event(Object, EventConsumer)}
   *   or calling re-usage of an event instance: {@link #use(long, int, Object, EventConsumer)}.  
   */
  public Object getRefData(){ return refData; }
  
  /**@deprecated use {@link #getRefData()}
   * @return
   */
  public Object getSrc(){ return refData; }
  
  
  
}
