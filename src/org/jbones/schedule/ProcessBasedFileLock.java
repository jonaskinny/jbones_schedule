package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;

import java.io.*;
import java.util.*;

/**
   a locked file that can 
   serve as a cross process resource
   lock.  you can use this to 
   give access to a contending process 
   on a particular locked file to do 
   something.  to determine if the current
   thread/process has acquired the lock
   and may do its thing, you should
   call boolean access().  it is guaranteed
   that only one process and its threads
   will have true returned and other threads
   in other processes on the same machine
   will have false returned.
**/
public class ProcessBasedFileLock
{
   protected File theLockFile;
   protected boolean canAccess;
   private FileOutputStream theFileDeletionStopper;
   
   public ProcessBasedFileLock(String fileName)
   {
      theLockFile = new File(fileName);
      try {
         canAccess = theLockFile.createNewFile();
         if (canAccess)
            theFileDeletionStopper = new FileOutputStream(theLockFile);
      } catch (IOException e) {
         Log.getLog(Log.ERR).log(e.toString());
         canAccess = false;
      }
      Runtime.getRuntime().addShutdownHook(new Thread() {
         public void run() { ProcessBasedFileLock.this.release(); }
      });

   }
  
   /**
      this will activate the file lock 
      for this process.
   **/
   public boolean access()
   {
      return canAccess;
   }
   
   public boolean release()
   {
      if (!canAccess) return false;
      try {
         if (theFileDeletionStopper!=null)
            theFileDeletionStopper.close();
      } catch (Exception e) {
         Log.getLog(Log.ERR).log(e.toString());
      }
      return theLockFile.delete();
   }
}
