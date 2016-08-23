package org.vishia.util.sampleStateM;

import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventSource;
import org.vishia.util.EventThread;

public class EvX extends Event{
  
  
  enum Cmd{EvX, EvY};
  
  EvX(EventSource src, EventConsumer dst, EventThread thread){
    super(src,null, dst, thread);
  }
  
  boolean sendEvent(Cmd cmd){ return sendEvent_(cmd); }
   
}
