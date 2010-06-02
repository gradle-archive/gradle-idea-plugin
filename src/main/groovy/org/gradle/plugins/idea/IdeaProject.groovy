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

import org.gradle.api.tasks.TaskAction

import org.gradle.api.Action
import org.gradle.listener.ListenerBroadcast
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.plugins.idea.model.Path
import org.gradle.plugins.idea.model.Project
import org.gradle.plugins.idea.model.ModulePath

/**
 * A task that generates and Idea ipr file.
 *
 * @author Hans Dockter
 */
public class IdeaProject extends DefaultTask {
    /**
     * The subprojects that should be mapped to modules in the ipr file. The subprojects will only be mapped, if the Idea plugin has been
     * applied to them.
     */
    Set subprojects

    /**
     * The ipr file
     */
    @OutputFile
    File outputFile

    /**
     * The java version used for defining the project sdk.
     */
    @Input
    String javaVersion

    /**
     * The wildcard resource patterns. Must not be null.
     */
    @Input
    Set wildcards

    private ListenerBroadcast<Action> beforeConfiguredActions = new ListenerBroadcast<Action>(Action.class);
    private ListenerBroadcast<Action> whenConfiguredActions = new ListenerBroadcast<Action>(Action.class);
    private ListenerBroadcast<Action> withXmlActions = new ListenerBroadcast<Action>(Action.class);

    def IdeaProject() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void updateXML() {
        Reader xmlreader = outputFile.exists() ? new FileReader(outputFile) : null;
        Set modules = subprojects.collect { subproject ->
            if (subproject.plugins.hasPlugin(IdeaPlugin)) {
                File imlFile = subproject.ideaModule.outputFile
                new ModulePath(project.projectDir, '$PROJECT_DIR$', imlFile)
            }
        }
        Project ideaProject = new Project(modules, javaVersion, wildcards, xmlreader, beforeConfiguredActions, whenConfiguredActions, withXmlActions)
        ideaProject.toXml(new FileWriter(outputFile))
    }

    /**
     * Adds a closure to be called when the ipr xml has been created. The xml is passed to the closure as a
     * parameter in form of a {@link groovy.util.Node}. The xml might be modified.
     *
     * @param closure The closure to execute when the ipr xml has been created.
     * @return this
     */
    IdeaProject withXml(Closure closure) {
        withXmlActions.add("execute", closure);
        return this;
    }

    /**
     * Adds a closure to be called after the existing ipr xml or the default xml has been parsed. The information
     * of this xml is used to populate the domain objects that model the customizable aspects of the ipr file.
     * The closure is called before the parameter of this task are added to the domain objects. This hook allows you
     * to do a partial clean for example. You can delete all modules from the existing xml while keeping all the other
     * parts. The closure gets an instance of {@link org.gradle.plugins.idea.model.Project} which can be modified.
     *
     * @param closure The closure to execute when the existing or default ipr xml has been parsed.
     * @return this
     */
    IdeaProject beforeConfigured(Closure closure) {
        beforeConfiguredActions.add("execute", closure);
        return this;
    }

    /**
     * Adds a closure after the domain objects that model the customizable aspects of the ipr file are fully populated.
     * Those objects are populated with the content of the existing or default ipr xml and the arguments of this task.
     * The closure gets an instance of {@link Project} which can be modified.
     *
     * @param closure The closure to execute after the {@link org.gradle.plugins.idea.model.Project} object has been fully populated.
     * @return this
     */
    IdeaProject whenConfigured(Closure closure) {
        whenConfiguredActions.add("execute", closure);
        return this;
    }
}
