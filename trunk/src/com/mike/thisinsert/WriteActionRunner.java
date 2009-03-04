package com.mike.thisinsert;

import com.intellij.openapi.application.ApplicationManager;

/**
 * User: mjparmel
 * Date: Feb 27, 2009
 * Time: 3:03:07 PM
 */
public class WriteActionRunner implements Runnable {
   private Runnable runnable;

   public WriteActionRunner(Runnable runnable) {
      this.runnable = runnable;
   }

   public void run() {
      ApplicationManager.getApplication().runWriteAction(this.runnable);
   }
}
