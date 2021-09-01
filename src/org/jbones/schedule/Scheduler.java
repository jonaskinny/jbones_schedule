package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;
import org.jbones.core.util.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
/*
   </pre>
   .class - class that extends java.util.TimerTask with a zero 
      argument constructor.
   
   .type - can be the following values:
               scheduleAtBeginingOfMonth
               scheduleAtEndOfMonth
               scheduleAtFixedRate
   
   .frequency - this is interpreted differently for each type.
               for scheduleAtBeginningOfMonth this specifies month 1-12
               for scheduleAtEndOfMonth this specifies month 1-12
               for scheduleAtFixedRate this specifies the repetition period as a long integer
   
   .startdate - the date in MM/DD/YYYY HH/mm/ss when the task should
      be first executed.

   .problemobserver - specifies a class that implements org.jbones.schedule.ProblemObserver
      with a zero constructor. no problem constructor is attached
      if this is blank.
   </pre>
**/
public class Scheduler implements Runnable,Service {
   protected Vector Tasks;
   protected long scanDelay;
   protected boolean working;
   protected Properties Props;
   protected String RecollectionFile;
   
   // INTERFACE
   private boolean safeToUse = false;
   public void initialize() throws CoreException {
      try {
         Props = new PropertiesUtil().loadPropertiesFileFromClassPath("/org/jbones/schedule/scheduler.properties");
         Tasks = new Vector();
         RecollectionFile = Props.getProperty("recollectionfile");
         if (RecollectionFile==null)
            RecollectionFile = "recollection.file";
         working = true;
         String ScanDelay = Props.getProperty("scandelay");
         if (ScanDelay == null)
            ScanDelay  = "5000";//5 second delay per check.
         scanDelay = Long.parseLong(ScanDelay);
         if(!shouldRun()){
           setUnsafeToUse();
           return;
         }
         Log.getLog(Log.OUT).log("trying to load tasks");
         loadTasks();
         Log.getLog(Log.OUT).log("done loading tasks");
      } catch (Exception e) {
         setUnsafeToUse();
         Log.getLog(Log.ERR).log("problem initializing scheduler");
         Log.getLog(Log.ERR).log(e.getMessage());
         Log.getLog(Log.ERR).log(CoreException.getStackTrace(e));
         throw new CoreException(CoreException.getStackTrace(e));
      }
      setSafeToUse();
   }
   public String getName() throws CoreException {
      // should return the name of the subclass
      try {
          return ClassName.name(this.getClass());
      } catch(Exception e){
            Log.getLog(Log.ERR).log("problem getting class name");
            throw new RuntimeException(CoreException.getStackTrace(e));
      }
   }
   public boolean safeToUse() throws CoreException {
      return safeToUse;
   }
   public void setUnsafeToUse() {
      safeToUse = false;
   }
   public void setSafeToUse() {
      safeToUse = true;
   }
  

   public Scheduler() {

   }
    private boolean shouldRun() throws CoreException {

      String Process = Props.getProperty("processmode");
      String Ip = Props.getProperty("ip");
      if (!isThisInternetAddress(Ip)) //if ip does not match.
      {
        Log.getLog(Log.OUT).log("[skips: same ip]");
         return false;
      }

      String LockFileName = Props.getProperty ("filesystemlocklocation") +
            File.separatorChar + getLocalIP()+".lock";

      //if another process of this label already started.
      if ( Process.equals("singleprocess") &&
         !isFirstOnThisMachine(LockFileName))
      {
         Log.getLog(Log.OUT).log("[Scheduler:process already started on machine.]");
         return false;
      }
      return true;
   }

      /**
   /**
      determines if the passed in string
      identifies the machine that this process
      lives on.
   **/
   public static boolean isThisInternetAddress( String IpOrHostName) {
      IpOrHostName = IpOrHostName.trim().toLowerCase();
      if (IpOrHostName.equals("localhost") ||
         IpOrHostName.equals("127.0.0.1")) return true;
      try {
         InetAddress Address = InetAddress.getLocalHost();
         String UrlByAddress = Address.getHostAddress().trim().toLowerCase();
         if (UrlByAddress.equals(IpOrHostName)) return true;
         String UrlByName = Address.getHostName().trim().toLowerCase();
         return UrlByName.equals(IpOrHostName);
      } catch (Exception e) {
         Log.getLog(Log.ERR).log(e.getMessage());
         return false;
      }
   }
   public String getLocalIP() {
   	try {
   	   return java.net.InetAddress.getLocalHost(
            ).getHostAddress();
      } catch (Exception e) {
         Log.getLog(Log.ERR).log(e);
         Log.getLog(Log.ERR).log(CoreException.getStackTrace(e));
         return "127.0.0.1";
      }
   }

   public synchronized void schedule(TaskWrapper task) {
      Tasks.addElement(task);
      (new SchedulerThread(task, this)).start();
      Log.getLog(Log.OUT).log("[Scheduler: Queuing Task - "+task.toString()+"]");
   }

   /**
      you can use this to load extra tasks into this scheduler
      after core started up.  examples include when you
      want to have a separate tasks file for each application.
      upon the load of the application you can ask the scheduler
      to load extra tasks from that separate properties file. NOTE:
      you should not load tasks that are already loaded without
      stopping the prior copy. requires the "tasks" property
      as a comma delimited string in the properties file.
   **/
   public synchronized void loadTasks() {
      try {
         Hashtable Recollection = loadTaskRecollection();
         StringTokenizer Token = new StringTokenizer(Props.getProperty("tasks"), ",", false);

         while(Token.hasMoreTokens())
         { 
            String TaskName = Token.nextToken();
            
            try {
               java.util.Date StartDate = toDate(
                  Props.getProperty(TaskName+".startdate"));

               long frequency = Long.parseLong(
                  Props.getProperty(TaskName+".frequency"));
               
               TimerTask Task = (TimerTask ) Class.forName(
                  Props.getProperty( TaskName+".class")).newInstance();
               if (null != Task && Task instanceof org.jbones.schedule.BaseTimerTask) {
                  ((org.jbones.schedule.BaseTimerTask)Task).initialize(TaskName);
               }
               String RunType = Props.getProperty(TaskName+".type");
               
               String ProblemObserverClass = Props.getProperty(TaskName+".problemobserver");
               ProblemObserver PO =
                  ProblemObserverClass==null ||
                     ProblemObserverClass.equals("") ?
                     (ProblemObserver) Class.forName("org.jbones.schedule.GenericTaskProblemObserver").newInstance() :
                     (ProblemObserver) Class.forName(ProblemObserverClass).newInstance();
               Log.getLog(Log.OUT).log("ProblemObserver created for Task " + TaskName);
               String Description = Props.getProperty(TaskName+".description");
                
               TaskWrapper TaskWrapper1 = new TaskWrapper(TaskName,Task,StartDate,frequency,RunType,PO,Description);
               Log.getLog(Log.OUT).log(TaskName + ".label= " + TaskWrapper1.getLabel());
               Log.getLog(Log.OUT).log(TaskName + ".toString= " + TaskWrapper1);
               TaskWrapper TaskWrapper2 = null;
               if (null != Recollection) {
                  Log.getLog(Log.OUT).log("Getting prior statistics for Task " + TaskName);
                  TaskWrapper2 = (TaskWrapper) Recollection.get(TaskWrapper1.getLabel());
                  TaskWrapper1.copyStatistics(TaskWrapper2);
               }

               schedule(TaskWrapper1);

            } catch (Exception e) {
               setUnsafeToUse();
               Log.getLog(Log.ERR).log("problem loading task:"+TaskName);
               Log.getLog(Log.ERR).log(e.getMessage());
               Log.getLog(Log.ERR).log(CoreException.getStackTrace(e));
            }
         }

      
      } catch (Exception pe) {
         Log.getLog(Log.ERR).log(
            "There was an exception in loading "+
            "the scheduled tasks list."+pe.toString());
         throw new RuntimeException(pe.toString());
      }
   }
   
   protected synchronized void saveTaskRecollection() {
      Hashtable Ht = new Hashtable();
      int intSize = Tasks.size();
      for(int h=0;h<intSize;h++)
      {
         TaskWrapper TaskWrapper = (TaskWrapper)Tasks.get(h);
         Ht.put(TaskWrapper.getLabel(),TaskWrapper);
      }
      
      FileOutputStream FOS = null;
      ObjectOutputStream OOS = null;
      try {
         Log.getLog(Log.OUT).log("[saving "+RecollectionFile+"...]");
         FOS = new FileOutputStream (RecollectionFile);
         OOS = new ObjectOutputStream(FOS);
         OOS.writeObject(Ht);
         OOS.flush();
         Log.getLog(Log.OUT).log("["+RecollectionFile+" saved.]");
      } catch (Exception e) {
         Log.getLog(Log.ERR).log( "[problem saving " + RecollectionFile);
      } finally {
         StreamUtils.closeStream(FOS);
         StreamUtils.closeStream(OOS);
      }
      
   }
   
   protected synchronized Hashtable loadTaskRecollection() {
      Log.getLog(Log.OUT).log("trying to load recollection of last schedule");
      FileInputStream FIS = null;
      ObjectInputStream OIS = null;

      try {
         FIS = new FileInputStream (RecollectionFile);
         OIS = new ObjectInputStream(FIS);
         Hashtable Ht = (Hashtable)OIS.readObject();
         Log.getLog(Log.OUT).log("["+RecollectionFile+" found.]");
         return Ht;
      } catch (Exception e) {
         Log.getLog(Log.ERR).log("Scheduler can not locate " + RecollectionFile + " so no history statistics of prior Task runs will be used.");
         return null;
      } finally {
         StreamUtils.closeStream(FIS);
         StreamUtils.closeStream(OIS);
      }
   }
         
   
   
   protected SimpleDateFormat theDateFormat = 
      new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
   
   protected java.util.Date toDate(String dateString) throws java.text.ParseException {
      return theDateFormat.parse(dateString);

   }
   
   /**
      this method gets run by instances of SchedulerThread
      for each scheduled task.
   **/
   public void run() {
      
      Thread currentThread = Thread.currentThread();
      if (! (currentThread instanceof SchedulerThread)) return;
      TaskWrapper Tw = ((SchedulerThread)currentThread).getTask();
      Log.getLog(Log.OUT).log("scheduler thread started to handle: "+Tw.getLabel());
      saveTaskRecollection();
		while (working&&!Tw.hasBeenCanceled())
      {
         try {
	   	   Thread.sleep(scanDelay);
		   } catch (InterruptedException e) {
			   Log.getLog(Log.ERR).log(e.toString());
			}
         if (!Tw.executionCriteriaMet()) continue;
         try {
            Log.getLog(Log.OUT).log("[Scheduler Service: "+Tw.getLabel()+" starting.]");
            Tw.run();
            Log.getLog(Log.OUT).log("[Scheduler Service: "+Tw.getLabel()+" stopped.]");
            saveTaskRecollection();
            
         } catch (Exception e) {
            Log.getLog(Log.ERR).log("task: "+Tw+" failed with exception:");
            System.err.println(e);
         }
         
      }
      Tw.ExecutingThread = null;
      Log.getLog(Log.OUT).log("[scheduler thread stopped: "+currentThread.toString()+" for : "+Tw.getLabel()+"]");
      saveTaskRecollection();
   }




   /**
      get the task with the label
      @param label label of the task.
      @return will return a valid TaskWrapper
         if valid, null otherwise.
   **/
   public synchronized TimerTask getTask(String TaskName) {
      int intSize = Tasks.size();
      for(int h=0;h<intSize;h++)
      {
         TaskWrapper TaskWrapper = (TaskWrapper)Tasks.get(h);
         if (TaskWrapper.getLabel().equals(TaskName))
            return TaskWrapper;
      }
      return null;
   }
   
   /**
      Terminates this timer, discarding any currently scheduled tasks. 
   **/
   public synchronized void cancel() {
      Log.getLog(Log.OUT).log("[scheduler stopping...]");
      int intSize = Tasks.size();
      for( int h=0;h<intSize;h++)
         ((TaskWrapper)Tasks.get(h)).cancel();
      working = false;
   }
   
   
   
   public String getInfo() {
      StringBuffer SB = new StringBuffer();
      SB.append("Tasks:\r\n");
      int intSize = Tasks.size();
      for(int h=0;h<intSize;h++)
        SB.append(Tasks.get(h).toString()+"\r\n");
        SB.append("Total Tasks:"+intSize);
      return SB.toString();
   }
   
   /**
      returns an Enumeration of TaskWrapper(s)
   **/
   public Enumeration getAllTasks() {
      return Tasks.elements();
   }
   
   public synchronized boolean removeTask(String TaskName) {
      int intSize = Tasks.size();
      for(int h=0;h<intSize;h++)
      {
         TaskWrapper TaskWrapper = (TaskWrapper)Tasks.get(h);
         if (TaskWrapper.getLabel().equals(TaskName))
         {
            Tasks.remove(h);
            return true;
         }
      }
      return false;
   }
   protected static boolean isFirstOnThisMachine(String FileName) {
      boolean ableToCreateLockingFile = (new ProcessBasedFileLock(FileName)).access();
      if (! ableToCreateLockingFile) {
         Log.getLog(Log.OUT).log("unable to create locking file ... likely caused by locking file already existing. This can occur if a prior process using this file lock was terminated without proper shutdown.");   
      }
      return ableToCreateLockingFile;
   }

}
