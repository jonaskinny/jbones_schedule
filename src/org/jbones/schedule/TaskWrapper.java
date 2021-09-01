package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;

import java.io.*;
import java.util.*;


/**
   a wrapper around a java task that
   contains functionality to track
   the state and other statistics of the 
   task.  the state can be one of INITIALIZED,
   IDLE, RUNNING, CANCELING, CANCELED, 
   FAILED.
**/
public class TaskWrapper extends TimerTask
   implements Serializable
{
   static final long SerialVersionUID = -298208825764901098L;
   public static final int CANCELED = 1;
   public static final int CANCELING = 2;
   public static final int RUNNING = 3;
   public static final int IDLE = 4;
   public static final int FAILED = 5;
   public static final int INITIALIZED = 6;
   public static final String[] STATE_LABELS = new String[] {
      "CANCELED", "CANCELING", "RUNNING", "IDLE", "FAILED", "INITIALIZED" };
   
   protected long TWENTY_FOUR_HOURS = 1000*60*60*24;//24 x hour ( 60 minutes ( 60 seconds (1000 ms)))
   
   protected transient TimerTask Task;
   protected String Label;
   protected java.util.Date mdtCreateDate;
   protected java.util.Date mdtLastStartDate;
   protected java.util.Date mdtLastEndDate;
   protected String RunType;
   protected int State;
   protected long TaskRepeatDelay;
   protected transient ProblemObserver ProblemObserver;
   public transient Thread ExecutingThread;
   protected String Description;


   public TaskWrapper(
      String Label,
      TimerTask TimerTask,
      java.util.Date dtAnchorDate,
      long lngTaskRepeatMillis,
      String RunType,
      ProblemObserver ProblemObserver)
   {
      this (
         Label,
         TimerTask,
         dtAnchorDate,
         lngTaskRepeatMillis,
         RunType,
         ProblemObserver,
         "no description.");
   }
   
   public TaskWrapper(
      String Label,
      TimerTask TimerTask,
      java.util.Date dtAnchorDate,
      long lngTaskRepeatMillis,
      String RunType,
      ProblemObserver ProblemObserver,
      String Description)
   {
      super();
      this.Task = TimerTask;
      this.Label = Label;
      this.mdtCreateDate = dtAnchorDate;
      this.mdtLastStartDate = mdtLastEndDate = null;
      this.State = INITIALIZED;
      this.TaskRepeatDelay = lngTaskRepeatMillis;
      this.ExecutingThread = null;
      this.RunType = RunType;
      this.ProblemObserver = ProblemObserver;
      this.Description = Description;
   }
   
   protected TaskWrapper()
   {
      super();
      Task = null;
      Label = "none";
      mdtCreateDate = null;
      mdtLastStartDate = mdtLastEndDate = null;
      State = INITIALIZED;
      TaskRepeatDelay = 0L;
      ExecutingThread = null;
      RunType = "";
      ProblemObserver = null;
      Description = "";
   }
   
   public synchronized void reset()
   {
      State = IDLE;
   }
   /**
      this will interrupt the executing thread
      if there is one referenced.
   **/
   public synchronized boolean cancel()
   {
      if (State==CANCELED) return false;
      State = CANCELING;
      //doesnt matter if another thread
      //modifies this value.
      
      if (ExecutingThread!=null)
         ExecutingThread.interrupt();
      
      //the interrupted exception will not
      //set the flag so we are safe from that
      //thread modifying State's value.
      State = CANCELED;
      return true;
   }
   
   public synchronized java.util.Date getLastStartDate(){
      return mdtLastStartDate;
   }
   
   public synchronized java.util.Date getLastEndDate(){
      return mdtLastEndDate;
   }
   
   public synchronized java.util.Date getCreateDate(){
      return mdtCreateDate;
   }
   
   public synchronized String getRunType() {
      return RunType;
   }
   
   public String toString()
   {
      return "[" + Task +
         ":label "+Label+
         ":created " + mdtCreateDate+
         ":laststart " + mdtLastStartDate+
         ":lastend " + mdtLastEndDate +
         ":state "+ STATE_LABELS [ State-1]+
         ":repeats "+TaskRepeatDelay+
         ":runtype "+RunType+
         ": "+ProblemObserver + "]";
   }
   
   public String getLabel() {
      return Label;
   }
   
   public synchronized boolean isRunning() {
      return State==RUNNING;
   }
   public synchronized boolean failed() {
      return State==FAILED;
   }
   public synchronized boolean hasBeenCanceled(){
      return State==CANCELED;
   }
   public synchronized int getState() {
      return State;
   }
   public void copyStatistics(TaskWrapper TaskWrapper)
   {
      this.State = TaskWrapper.State;
      this.mdtLastStartDate = TaskWrapper.mdtLastStartDate;
      this.mdtLastEndDate = TaskWrapper.mdtLastEndDate;
      this.mdtCreateDate = TaskWrapper.mdtCreateDate;
   }
   public String getDescription() {
      return Description;
   }
   public void setDescription(String Description) {
      Description = Description;
   }
   public long getTaskRepeatDelay() {
      return TaskRepeatDelay;
   }
   
   public TimerTask getWrappedTask() {
      return Task;
   }
   
   public ProblemObserver getProblemObserver() {
      return ProblemObserver;
   }
   public void setProblemObserver(ProblemObserver ProblemObserver) {
      ProblemObserver = ProblemObserver;
   }
   protected void notifyOfProblem(Throwable throwable)
   {
      if (ProblemObserver==null) return;
      ProblemObserver.onProblem(
         this,throwable,throwable);
   }

   public void run()
   {
      try {
         //long startDifference = System.currentTimeMillis() - mdtCreateDate.getTime();
         //long idealExecution = mdtCreateDate.getTime() + (startDifference/TaskRepeatDelay)*TaskRepeatDelay;
         mdtLastStartDate = new java.util.Date();//idealExecution);
         State = RUNNING;
         Task.run();
         State = IDLE;
         mdtLastEndDate = new java.util.Date();
      } catch (RuntimeException e) {
         State = FAILED;
         notifyOfProblem(e);
         throw e;
      } catch (Exception e) {
         State = FAILED;
         notifyOfProblem(e);
      }
   }
   
   public boolean executionCriteriaMet()
   {
      if (State == CANCELED)
         return false;
      if (State == CANCELING) 
      {
         State = CANCELED;
         return false;
      }
      
      java.util.Date dtCurrentDate = new java.util.Date();
      long lngCurrent = dtCurrentDate.getTime();
      long lngCreate = mdtCreateDate.getTime();
      long lngLast = mdtLastStartDate==null? 0 : mdtLastStartDate.getTime();
      long lngTimeSinceLast = lngCurrent - lngLast;
      
      //if initialized but not time to execute yet.
      if (State == INITIALIZED)
         return lngCurrent >= lngCreate;//if first time, run it if over due.
      
      if (RunType.equals("scheduleAtBeginingOfMonth"))
      {
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(dtCurrentDate);
         if (calendar.get(Calendar.DAY_OF_MONTH)==1 &&
            (lngTimeSinceLast >= TWENTY_FOUR_HOURS))
               return true;
         return false;
      }
      else if ( RunType.equals("scheduleAtEndOfMonth"))
      {
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(dtCurrentDate);
         if (calendar.get(Calendar.DAY_OF_MONTH)==calendar.getActualMaximum(Calendar.DAY_OF_MONTH) &&
            (lngTimeSinceLast >= TWENTY_FOUR_HOURS))
               return true;
         return false;
      }
      
      //scheduleAtFixedRate
      
      //recompute the next execution time 
      long lngNext = lngCreate + (((lngCurrent - lngCreate)/TaskRepeatDelay) + 1)*TaskRepeatDelay;
      if (lngNext - lngLast <= TaskRepeatDelay )
         return false;
      return true;
   }
   
   
}
