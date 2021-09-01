package org.jbones.schedule;


public interface ProblemObserver extends java.util.EventListener
{
   /**
      this should throw a runtime exception
      if it wants the current thread to stop 
      processing immediately.
   **/
   public abstract void onProblem(Object SourceOfProblem, Throwable e, Object OtherMsg);
}
