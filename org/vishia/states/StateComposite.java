package org.vishia.states;

import java.lang.reflect.Constructor;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;



import org.vishia.util.DataAccess;
import org.vishia.util.InfoAppend;

public class StateComposite extends StateSimple implements InfoAppend
{
  /**Version, history and license.
   * <ul>
   * <li>2014-11-09 Hartmut chg: Capability of StateParallel contained here, class StateParallel removed: 
   *   It is possible to have a StateComposite with its own sub states, but a second or more parallel states
   *   in an own composite, which is yet the class StateAddParallel. It is {@link StateParallel}. More simple, more flexibility. 
   * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateCompositeBase}, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
   * <li>2013-05-11 Hartmut new: It is a {@link EventConsumer} yet. Especially a timer event needs a destination
   *   which is this class.
   * <li>2013-04-27 Hartmut adapt: The {@link #entry(EventMsg2)} and the {@link #entryAction(EventMsg2)} should get the event
   *   from the transition. See {@link StateSimpleBase}.
   * <li>2013-04-13 Hartmut re-engineering: 
   *   <ul>
   *   <li>New method {@link #setDefaultState(StateSimpleBase)}
   *   <li>{@link #entryDefaultState()} is package private now, it regards requirements of {@link StateParallelBase}.
   *   <li>The old override-able method entryDefault() was removed.
   *   <li>The overridden entry() was removed, replaced by #entryComposite, which is called in {@link StateSimpleBase#entry(int)}
   *     if the instance is this type.    
   *   </ul>
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2003 in C-language.
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
  public static final int version = 20130511;

  protected int maxStateSwitchesInLoop = 1000;
  
  /**Stores whether this composite state is active. Note that the #stateAct is set as history state
   * as well the state is not active. This bit is set to false too if the current state is exited
   * and a new state is not entered yet, while temporary transition processing. It helps to prevent double
   * execution of the {@link #exitTheState()} routine if exit of the enclosing state is processed.*/
  boolean isActive;
  
  /*package private*/ StateSimple stateAct;
  
  StateSimple stateDefault;
  
  
  
  /**List of all sub states of this composite state. This list is necessary to build the transition paths on startup.
   * It is nice to have for debugging. Therefore its name starts with 'a' to set it on top of variable list in debugging. */
  final StateSimple[] aSubstates;
  
  
  
  /**Only used for the {@link StateMachine#topState} 
   * and as special constructor to build a state machine from other data. See org.vishia.stateMGen.StateMGen. 
   */
  public StateComposite(StateMachine stateMachine, StateSimple[] aSubstates){
    this.aSubstates = aSubstates;
    this.stateMachine = stateMachine;
    this.stateId = "";
  };
  
  
  
  
  /**The constructor of any StateComposite checks the class for inner classes which are the states.
   * Each inner class which is instance of {@link StateSimple} is instantiated and stored both in the {@link StateMachine#stateMap} 
   * to find all states by its class.hashCode
   * and in {@link #aSubstates} for debugging only.
   * <br><br>
   * After them {@link #buildStatePathSubstates()} is invoked to store the state path in all states.
   * Then {@link #createTransitionListSubstate(int)} is invoked which checks the transition of all states recursively. 
   * Therewith all necessary data for the state machines's processing are created on construction. 
   * 
   * @see StateMachine#StateMachine()
   * @see StateComposite#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateComposite#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   */
  public StateComposite() {
    super();
    Class<?> clazz = this.getClass();
    //Class<?> clazzTop1 = clazz;
    Object env = this;
    do {
      Class<?> clazzTop2 = env.getClass();
      if(DataAccess.isOrExtends(clazzTop2, StateMachine.class)){
        this.stateMachine = (StateMachine)env;
        //firstly create the stateMap. Note: This is called in constructor of the StateTop-superclass.
      } else {
        env = DataAccess.getEnclosingInstance(env);
      }
    } while(stateMachine == null && env !=null);
      
    if(stateMachine == null){
      throw new IllegalArgumentException("");
    }
    Class<?>[] innerClasses = clazz.getDeclaredClasses();
    if(innerClasses.length >0) {  //it is a composite state.
      List<StateSimple> listSubstates = null;
      int ixSubstates = -1;
      try{
        for(Class<?> clazz1: innerClasses) {
          if(DataAccess.isOrExtends(clazz1, StateSimple.class)) {
            Constructor<?>[] ctor1 = clazz1.getDeclaredConstructors();
            //Constructor<?>[] ctor = clazz2.getDeclaredConstructors();
            ctor1[0].setAccessible(true);
            final Object oState = ctor1[0].newInstance(this);   //creates the instance, maybe a StateComposite or a StateAddParallel.
            //Note that the inner states are processed already in the yet called constructor.
            final StateSimple state;
            if(listSubstates ==null) { listSubstates = new LinkedList<StateSimple>(); }
            state = (StateSimple)oState;
            listSubstates.add(state);
            state.stateId = clazz1.getSimpleName();
            state.stateMachine = this.stateMachine;
            state.enclState = this;
            int idState = clazz1.hashCode();
            stateMachine.stateMap.put(idState, state);
            try { 
              clazz1.getDeclaredField("isDefault");
              if(this.stateDefault != null){ 
                throw new IllegalArgumentException("StateComposite - more as one default state in;" + stateId); 
              }
              this.stateDefault = state;  //The first state is the default one.
            } catch(NoSuchFieldException exc){} //empty!
          }
        }
      } catch(Exception exc){
        throw new RuntimeException(exc);
      }
      if(listSubstates !=null ){
        this.aSubstates = listSubstates.toArray(new StateSimple[listSubstates.size()]); 
      } else {
        this.aSubstates = null;
      }
      if(stateDefault == null && aSubstates !=null){ 
        throw new IllegalArgumentException("StateMachine - a default state is necessary. Define \"final boolean isDefault = true\" in one of an inner class State;" + stateId); 
      }
    } else { //no inner states
      this.aSubstates = null;
    }
  }
  
  
  
  /**It is called from outside if the state machine is build other than with Reflection.
   * @param key the key which is used to find the state in {@link StateMachine#stateMap} while preparing the transitions
   * @param state a new inner state.
   */
  public void addState(int key, StateSimple state){
    int ix = 0;
    while(ix < aSubstates.length && aSubstates[ix] !=null){ ix +=1; } //search next free
    if(ix >= aSubstates.length) throw new IllegalArgumentException("too many states to add");
    aSubstates[ix] = state;
    stateMachine.stateMap.put(state.hashCode(), state);
    if(stateDefault ==null){
      stateDefault = state;  //the first state is the default state.
    }
  }
  
  
  
  
  /**Sets the path to the state for this and all {@link #aSubstates}, recursively call.
   * This method is invoked in the constructor of the state machine only one time.
   * It is not for application.
   * @param enclState
   * @param recurs
   */
  @Override void buildStatePathSubstates(StateSimple enclState, int recurs) {
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty");
    this.buildStatePath(enclState);
    if(aSubstates !=null) {
      for(StateSimple subState: this.aSubstates){
        subState.buildStatePathSubstates(this, recurs +1);
      }
    }
  }



  
  
  /**Create all transition list for this state and all {@link #aSubstates}, recursively call.
   * This method is invoked in the constructor of the state machine only one time.
   * It is not for application.
   * @param recurs
   */
  void createTransitionListSubstate(int recurs){
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty, too many subStates; state=" + stateId);
    this.createTransitionList(this, null, 0);  
    if(aSubstates !=null) {
      for(StateSimple subState: this.aSubstates){
        subState.createTransitionListSubstate(recurs+1);
      } 
    }
  }



  
  
  


  /**Check whether this composite state has the given state as direct actual sub state
   * @param state Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  public final boolean isInState(StateSimple state){ 
    return isInState()             //this state is active too, or it is the top state.
        && (  stateAct == state   //the given state is the active.
           || aSubstates == null  //a poor parallel state container.
           );   
  }
  
  
  
  /**This method is used to entry the default state of this composite if the actual state is null (first invocation).  */
  /*package private*/ final int entryDefaultState(){ 
    int ret = 0;
    if(stateDefault !=null) {
      ret |= stateDefault.entryTheState(null,0);
    }
    return ret;
  }

  
  
  /**This method should be called from outside if the history state should be entered and all history states
   * should be entered in sub states.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryDeepHistory(EventObject ev){
    StateSimple stateActHistory = stateAct;  //save it
    int cont = entryTheState(ev,0);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    if(stateActHistory instanceof StateComposite){
      cont = ((StateComposite)stateActHistory).entryDeepHistory(ev);
    } else {
      cont = stateActHistory.entryTheState(ev,0);           //entry in the history sub state.
    }
    return cont;
  }
  
  
  /**This method should be called from outside if the history state should be entered but the default state of any
   * sub state should be entered.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryFlatHistory(EventObject ev){
    StateSimple stateActHistory = stateAct;  //save it
    int cont = entryTheState(ev,0);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    cont = stateActHistory.entryTheState(ev,0);             //entry in the history sub state.
    return cont;
  }
  
  

  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking either its {@link StateComposite#processEvent(EventObject)}
   * which calls this method recursively.
   * <br><br>
   * If this method returns with {@link StateSimpleBase#mRunToComplete} that invocation is repeated in a loop, to call
   * the transition of the new state too. But if the event was consumed by the last invocation, it is not supplied again
   * in the loop, the event parameter is set to null instead. It means only conditional transitions are possible.
   * This behavior is conform with the UML definition.
   * <br><br>
   * If the loop would not terminate because any state have a valid transition and the state machine switches forever,
   * the loop is terminated with an exception for a number of {@link #maxStateSwitchesInLoop}. This exception occurs
   * if the user stateMachine conditions are faulty only.
   * <br><br>
   * At least the {@link #checkTransitions(EventObject)} of this state is invoked but only if the event is not processed
   * or the state contains non-event triggered (conditional) transitions. Last one is signified by the {@link #modeTrans}.
   * <br><br>
   * This method overrides the {@link StateSimple#processEvent(EventObject)} which is overridden by {@link StateParallel#processEvent(EventObject)}
   * too to provide one method for event processing for all state kinds with the necessary different handling.
   * 
   * @param evP The event.
   * @return Some bits especially {@link StateSimpleBase#mEventConsumed} as result of the inside called {@link #checkTransitions(EventObject)}.
   */
  /*package private*/ @Override int processEvent(final EventObject evP){  //NOTE: should be protected.
    int cont = 0;
    EventObject evTrans = evP;
    if(aSubstates !=null) {
      int catastrophicalCount =  maxStateSwitchesInLoop;
      int contLoop = 0;
      do{
        contLoop &= ~mRunToComplete;  //only this is checked for any state transition. Left all other bits especially mEventConsumed.
        if(stateAct == null){
          contLoop |= entryDefaultState();  //regards also Parallel states.
          if(stateMachine.debugState && (contLoop & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(null, evTrans, contLoop); }
        } 
        StateSimple statePrev = stateAct;
        int trans = stateAct.processEvent(evTrans);
        if(stateMachine.debugState && statePrev instanceof StateSimple && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, trans); }
        contLoop |= trans;
        //
        if((contLoop & StateSimple.mEventConsumed) != 0){
          evTrans = null;
        }
        if(catastrophicalCount == 4) {
          catastrophicalCount = 3;  //set break point! to debug the loop
        }
      } while(isActive   //leave the loop if this composite state is exited.
          && (contLoop & mRunToComplete) !=0    //loop if runToComplete-bit is set, the new state should be checked.
          && --catastrophicalCount >=0
          );
      if(catastrophicalCount <0) {
        throw new RuntimeException("unterminated loop in state switches");
      }
      cont |= contLoop;
    }
    //Process to leave this state.
    //
    //evTrans: If the event was consumed in any inner transition, it is not present for the own transitions. UML-conform.
    //
    if(  evTrans != null   //evTrans is null if it was consumed in inner transitions. 
      || (modeTrans & StateSimple.mRunToComplete) !=0  //state has only conditional transitions
      ){
      //process the own transition. Do it after processing the inner state (omg.org)
      //and only if either an event is present or the state has only conditional transitions.
      StateSimple statePrev = stateAct;
      int trans = checkTransitions(evTrans); 
      if(stateMachine.debugState && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, trans); }
      cont |= trans;
    }
    return cont;  //runToComplete.bit may be set from an inner state transition too.
  }

  
  
  
  
  
  
  
  private void printStateSwitchInfo(StateSimple statePrev, EventObject evTrans, int cont) {
    //DateOrder date = new DateOrder();
    //Thread currThread = Thread.currentThread();
    //String sThread = currThread.getName();
    String sStatePrev = statePrev !=null ? statePrev.stateId : "INIT";
    //String sActiveState = getActiveState();
    StringBuilder uStateNext = new StringBuilder();
    if(stateAct == null){ uStateNext.append("--inactive--"); }
    else {
      StateSimple stateAct1 = stateAct;
      uStateNext.append(stateAct.stateId);
      while(stateAct1 instanceof StateComposite) {
        stateAct1 = ((StateComposite)stateAct1).stateAct;
        if(stateAct1 !=null) { 
          uStateNext.insert(0, '.').insert(0,  stateAct1.stateId);
        }
      }
    }
    if(!isActive){
      System.out.println("StateCompositeBase - leaved; " + sStatePrev + " ==> " + uStateNext + "; event=" + evTrans + ";");
    } else if((cont & StateSimple.mEventConsumed)!=0) {  //statePrev != stateAct){  //from the same in the same state!
      System.out.println("StateCompositeBase - switch;" + sStatePrev + " ==> "  + uStateNext + "; event=" + evTrans + ";");
    } else if(evTrans !=null){ 
      System.out.println("StateCompositeBase - switch;" + sStatePrev + " ==> "  + uStateNext + "; not used event=" + evTrans + ";");
    } else { 
      System.out.println("StateCompositeBase - switch;" + sStatePrev + " ==> "  + uStateNext + "; runToComplete;");
    }
    
  }

  
  


  /**Exits first the actual sub state (and that exits its actual sub state), after them this state is exited.
   * It calls {@link StateSimple#exitTheState()} which invokes the maybe application overridden {@link StateSimple#exit()} routine.
   */
  @Override void exitTheState(){ 
    if(isActive && stateAct !=null){
      stateAct.exitTheState();    //recursively call for all inner states which are yet active.
      isActive = false; //NOTE that StateSimpleBase.exit() sets isActive to false already. It is done twice.
    }
    super.exitTheState();
  }

  
  
  @Override public CharSequence getStatePath(){
    StringBuilder uPath = new StringBuilder(120);
    StateSimple state = this;
    while((state = state.enclState) !=null){
      uPath.append(':').append(state.stateId);
    }
    state = this;
    //*
    do{
      uPath.append('.').append(state.stateId);
      if(state instanceof StateComposite){
        state = ((StateComposite)state).stateAct;
      } else { state = null; }
    } while(state !=null);
    //*/
    return uPath;
  }
  
  
  
  @Override public CharSequence infoAppend(StringBuilder u) {
    if(u == null) { u = new StringBuilder(200); }
    String separator = "";
    if(aSubstates !=null) {
      u.append(stateId);
      u.append(separator);
      if(isActive) {
        stateAct.infoAppend(u);
        //u.append(stateAct.toString());
      }
      separator = "-"; 
    }
    return u;
    
  }
  
  
  @Override public String toString(){ 
    StringBuilder u = new StringBuilder();
    infoAppend(u);
    return u.toString();
  }


}
