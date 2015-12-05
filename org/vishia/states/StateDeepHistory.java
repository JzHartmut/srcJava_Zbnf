package org.vishia.states;

/**This is the Pseudo-state for deep history transitions. It is only a marker without extra data.
 * 
 * @author Hartmut Schorrig
 *
 */
public class StateDeepHistory extends StateSimple
{
  public StateDeepHistory(String name){
    super();
    stateId = name;
    
  }

  public StateDeepHistory(){
    this("deepHistory");
    
  }
}
