package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;
import org.jbones.core.util.*;

import java.util.*;
import java.util.regex.PatternSyntaxException;

public abstract class BaseTimerTask extends TimerTask {
   public static final String INITIALIZE = "initialize";
   private final String SCHEDULER_PROPERTIES_PATH = "/org/jbones/schedule/scheduler.properties";
   public BaseTimerTask() {
      super();
   }
   
   public abstract void run();

   protected void initialize(String taskName) {
      Properties p = new PropertiesUtil().loadPropertiesFileFromClassPath(SCHEDULER_PROPERTIES_PATH);
      String[] values = null;
      try {
         values = p.getProperty(taskName + "." + BaseTimerTask.INITIALIZE).split(",");
      } catch (PatternSyntaxException pse) {
            Log.getLog(Log.ERR).log(pse);
      } catch (Exception e) {
            Log.getLog(Log.ERR).log("problem loading " + SCHEDULER_PROPERTIES_PATH + ":" + taskName + "." + BaseTimerTask.INITIALIZE);
            Log.getLog(Log.ERR).log(e.getMessage());
            Log.getLog(Log.ERR).log(CoreException.getStackTrace(e));
      }
      setInitializers(taskName,values,p);
   }
   protected abstract void setInitializers (String taskName, String[] values, Properties p);
}
