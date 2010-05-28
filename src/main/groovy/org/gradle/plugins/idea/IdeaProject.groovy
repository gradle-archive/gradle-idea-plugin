/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.idea

import org.gradle.api.DefaultTask
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.maven.XmlProvider
import org.gradle.api.Action
import org.gradle.listener.ListenerBroadcast

/**
 * @author Hans Dockter
 */
public class IdeaProject extends DefaultTask {
    def subprojects;
    File outputFile;
    String javaVersion
    Set wildcards

    private ListenerBroadcast<Action> beforeConfiguredActions = new ListenerBroadcast<Action>(Action.class);
    private ListenerBroadcast<Action> whenConfiguredActions = new ListenerBroadcast<Action>(Action.class);
    private ListenerBroadcast<Action> withXmlActions = new ListenerBroadcast<Action>(Action.class);

    @TaskAction
    void updateXML() {
        Reader xmlreader = outputFile.exists() ? new FileReader(outputFile) : null;
        Set modules = subprojects.collect { subproject ->
            if (subproject.plugins.hasPlugin(IdeaPlugin)) {
                File imlFile = subproject.ideaModule.outputFile
                new Path(project.projectDir, '$PROJECT_DIR$', imlFile)
            }
        }
        Project ideaProject = new Project(modules, javaVersion, wildcards, xmlreader, beforeConfiguredActions, whenConfiguredActions, withXmlActions)
        ideaProject.toXml(new FileWriter(outputFile))
    }

    /**
     * <p>Adds a closure to be called when the ipr xml has been created. The xml is passed to the closure as a
     * parameter in form of a  {@link org.gradle.api.artifacts.maven.XmlProvider} . The xml might be modified.</p>
     *
     * @param closure The closure to execute when the ipr xml has been created.
     * @return this
     */
    IdeaProject withXml(Closure closure) {
        withXmlActions.add("execute", closure);
        return this;
    }

    IdeaProject beforeConfigured(Closure closure) {
        beforeConfiguredActions.add("execute", closure);
        return this;
    }

    IdeaProject whenConfigured(Closure closure) {
        whenConfiguredActions.add("execute", closure);
        return this;
    }
}
