package org.jbones.schedule;

public class SchedulerThread extends Thread
{
   protected TaskWrapper TaskWrapper;
   
   public SchedulerThread( TaskWrapper TaskWrapper, Runnable Runnable)
   {
      super(Runnable);
      this.TaskWrapper = TaskWrapper;
      TaskWrapper.ExecutingThread = this;
   }
   
   public TaskWrapper getTask() {
      return TaskWrapper;
   }
}
