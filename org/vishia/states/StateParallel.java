package org.vishia.states;

import java.util.LinkedList;
import java.util.List;

import org.vishia.event.Event;
import org.vishia.stateMachine.StateSimpleBase;




/**Base class for parallel states.
 * @author Hartmut Schorrig
 *
 * @param <DerivedState> The class type of the state which is derived from this class. 
 *   This parameter is used to designate the {@link #stateAct} and all parameters which works with {@link #stateAct}
 *   for example {@link #isInState(StateSimpleBase)}. It helps to prevent confusions with other state instances else
 *   the own one. A mistake is detected to compile time already.
 * @param <EnclosingState> The class type which is the derived type of the enclosing state where this state is member of.
 */
public abstract class StateParallel extends StateComposite
{

  
  /**Version, history and license.
   * <ul>
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2001 in C-language and Java.
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
  public static final int version = 20130414;

  List<StateComposite> states = new LinkedList<StateComposite>();
  
  
  final public void addState(StateComposite state){
    states.add(state);
  }
  
  
  /*package private*/ final void entryDefaultParallelStates() {
    for(StateComposite state: states){
      state.entryDefaultState();
    }
  }
  

  /**This method sets this state in the enclosing composite state and sets the own {@link #stateAct} to null 
   * to force entry of the default state if {@link #process(Event)} is running firstly after them.
   * If this method will be called recursively in an {@link #entry(int)} of an inner state,
   * that entry sets the {@link #stateAct} in {@link #setState(StateSimpleBase)} after them so it is not null.
   * <br><br> 
   * This method should be overridden if a entry action is necessary in any state. 
   * The overridden form should call this method in form super.entry(isConsumed):
   * <pre>
  public int entry(isConsumed){
    super.entry(0);
    //statements of entry action.
    return isConsumed | runToComplete;  //if the trans action should be entered immediately after the entry.
    return isConsumed | complete;       //if the trans action should not be tested.
  }
   * </pre>  
   * 
   * @param isConsumed Information about the usage of an event in a transition, given as input and returned as output.
   * @return The parameter isConsumed may be completed with the bit {@link #mRunToComplete} if this state's {@link #trans(Event)}-
   *   method has non-event but conditional state transitions. Setting of this bit {@link #mRunToComplete} causes
   *   the invocation of the {@link #trans(Event)} method in the control flow of the {@link StateCompositeBase#process(Event)} method.
   *   This method sets {@link #mRunToComplete}.
   */
  /**package private*/ void XXXentryParallelBase(Event<?,?> ev){
    XXXentryComposite();
    for(StateComposite state: states){
      if( state.enclState != null && !state.enclState.isInState(state)) {  
        state.entryTheState(ev);          //executes the entry action of this enclosing state to notify the state by its enclosingState.
      }
      state.stateAct = null;
      state.isActive = true;
    }
  }


  
  
  @Override public int _processEvent(Event<?,?> ev){
    int cont = 0;
    for(StateComposite state: states){
      cont |= state._processEvent(ev);
    }
    if((cont & StateSimpleBase.mEventConsumed) != 0){
      ev = null;
    }
    checkTransitions(ev);  //the own trans
    return cont;
  }
  
 
 
  /**Exits first the actual sub state (and tha exits its actual sub state), after them this state is exited.
   * @see org.vishia.stateMachine.StateSimpleBase#exit()
   */
  @Override public StateComposite exitTheState(){ 
    for(StateComposite state: states){
      state.exitTheState();
    }
    return super.exitTheState();
  }


  
  

  
  
}
