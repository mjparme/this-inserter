package com.mike.thisinsert;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

/**
 * User: mjparmel
 * Date: Feb 27, 2009
 * Time: 2:13:28 PM
 */
public class InsertThisAction extends AnAction {
    private static final Logger logger = Logger.getInstance(InsertThisAction.class.getName());

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        ThisInserter inserter = new ThisInserter(project);
        WriteActionRunner runner = new WriteActionRunner(inserter);
        CommandProcessor.getInstance().executeCommand(project, runner, "Insert This", null);
    }
}
