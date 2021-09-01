package org.jbones.schedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

import org.jbones.core.util.ClassName;
import org.jbones.core.*;
/**
	Main class of the jbones schedule package.
*/
public class Main {
   public static void main(String[] args) {
     System.out.println("running main in org.jbones.schedule...");
     try {
      Collection services = ServiceLocator.getServices();
      Iterator i = services.iterator();
      while (i.hasNext()) {
         System.out.println("core service:" + ClassName.name(i.next().getClass()));
      }
     } catch (Exception e) {
         System.err.println(e.getMessage());
     }
  }
}
