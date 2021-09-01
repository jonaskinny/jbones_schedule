package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;

public class GenericTaskProblemObserver implements ProblemObserver {
   /**
      this should throw a runtime exception
      if it wants the current thread to stop 
      processing immediately.
   **/
   public void onProblem(Object SourceOfProblem, Throwable e, Object OtherMsg) {
      Log.getLog(Log.ERR).log("GenericTaskProblemObserver is reporting a problem from ");
      Log.getLog(Log.ERR).log(SourceOfProblem.toString());
      Log.getLog(Log.ERR).log(OtherMsg.toString());
      Log.getLog(Log.ERR).log(e.getMessage());
      throw new RuntimeException("GenericTaskProblemObserver.onProblem()" + e.getMessage());
   }
}
