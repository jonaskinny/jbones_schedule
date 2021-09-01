package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;

import java.util.*;

public class DummyTask extends TimerTask
{
   public DummyTask()
   {
      super();
   }
   
   public void run()
   {
      Log.getLog(Log.OUT).log("the dummy task " + this.toString() + " was run on: " + (new java.util.Date()).toString());
   }
}
