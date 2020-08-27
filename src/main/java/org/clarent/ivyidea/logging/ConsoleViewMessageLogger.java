/*
 * Copyright 2010 Guy Mahieu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.clarent.ivyidea.logging;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import org.apache.ivy.util.AbstractMessageLogger;
import org.clarent.ivyidea.config.IvyIdeaConfigHelper;

import static com.intellij.execution.ui.ConsoleViewContentType.SYSTEM_OUTPUT;

public class ConsoleViewMessageLogger extends AbstractMessageLogger {

    private final ConsoleView consoleView;
    private final IvyLogLevel threshold;

    public ConsoleViewMessageLogger(final ConsoleView consoleView) {
        this.consoleView = consoleView;
        threshold = IvyIdeaConfigHelper.getIvyLoggingThreshold();
    }

    public void log(final String msg, final int level) {
        rawlog(msg, level);
    }

    public void rawlog(final String msg, final int level) {
        rawlog(msg, IvyLogLevel.fromLevelCode(level));
    }

    public void rawlog(final String message, final IvyLogLevel logLevelForMessage) {
        if (threshold.isMoreVerboseThan(logLevelForMessage)) {
            logToConsoleView(message + "\n", logLevelForMessage.getContentType());
        }
    }

    protected void doProgress() {
        logToConsoleView(".", SYSTEM_OUTPUT);
    }

    protected void doEndProgress(final String msg) {
        logToConsoleView(msg + '\n', SYSTEM_OUTPUT);
    }

    private void logToConsoleView(final String message, final ConsoleViewContentType contentType) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                consoleView.print(message, contentType);
            }
        });
    }
}
