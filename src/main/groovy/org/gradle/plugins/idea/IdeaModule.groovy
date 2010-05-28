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

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.listener.ListenerBroadcast

/**
 * @author Hans Dockter
 */
public class IdeaModule extends DefaultTask {
    /**
     * The root directory of the module. Must not be null.
     */
    @InputFiles
    File moduleDir

    /**
     * The iml file.
     */
    @OutputFile
    File outputFile

    /**
     * The dirs containing the productions sources. Must not be null.
     */
    @InputFiles
    Set sourceDirs

    /**
     * The dirs containing the test sources. Must not be null.
     */
    @InputFiles
    Set testSourceDirs

    /**
     * The dirs to be excluded by intellij. Must not be null.
     */
    @InputFiles
    def excludeDirs

    /**
     * The intellij output dir for the production sources. If null no entry for output dirs is created.
     */
    @InputFiles @Optional
    File outputDir

    /**
     * The intellij output dir for the test sources. If null no entry for test output dirs is created.
     */
    @InputFiles @Optional
    File testOutputDir

    /**
     * The keys of this map are the Intellij scopes. Each key points to another map that has two keys, plus and minus.
     * The values of those keys are sets of {@link org.gradle.api.artifacts.Configuration}       objects. The files of the
     * plus configurations are added minus the files from the minus configurations.
     */
    Map scopes = [:]

    private ListenerBroadcast<Action> beforeConfiguredActions = new ListenerBroadcast<Action>(Action.class);
    private ListenerBroadcast<Action> whenConfiguredActions = new ListenerBroadcast<Action>(Action.class);
    private ListenerBroadcast<Action> withXmlActions = new ListenerBroadcast<Action>(Action.class);

    def IdeaModule() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void updateXML() {
        Reader xmlreader = outputFile.exists() ? new FileReader(outputFile) : null;
        println getDependencies()
        Module module = new Module(getSourcePaths(), getTestSourcePaths(), getExcludePaths(), getOutputPath(), getTestOutputPath(),
                getDependencies(), xmlreader, beforeConfiguredActions, whenConfiguredActions, withXmlActions)
        module.toXml(new FileWriter(outputFile))
    }

    protected Path getOutputPath() {
        outputDir ? getPath(outputDir) : null
    }

    protected Path getTestOutputPath() {
        testOutputDir ? getPath(testOutputDir) : null
    }

    protected Set getSourcePaths() {
        sourceDirs.collect { getPath(it) }
    }

    protected Set getTestSourcePaths() {
        testSourceDirs.collect { getPath(it) }
    }

    protected Set getExcludePaths() {
        excludeDirs.collect { getPath(it) }
    }

    protected Set getDependencies() {
        scopes.keySet().inject([] as LinkedHashSet) {result, scope ->
            result + (getExternalDependencies(scope) + getProjectDependencies(scope))
        }
    }

    protected Set getProjectDependencies(String scope) {
        if (scopes[scope]) {
            def configurations = scopes[scope]
            def included = configurations.plus.inject([] as Set) { includes, configuration ->
                includes + configuration.getAllDependencies(ProjectDependency).collect { projectDependency -> projectDependency.dependencyProject }
            }
            configurations.minus.each { configuration ->
                included = included - configuration.getAllDependencies(ProjectDependency).collect { projectDependency -> projectDependency.dependencyProject }
            }
            return included.collect { new ModuleDependency(it.name, scope) }
        }
        return []
    }

    protected Set getExternalDependencies(String scope) {
        if (scopes[scope]) {
            def configurations = scopes[scope]
            def included = configurations.plus.inject([] as Set) { includes, configuration ->
                includes + configuration.files {
                    !(it instanceof ProjectDependency)
                }
            }
            def excluded = configurations.minus.inject([] as Set) { excludes, configuration ->
                excludes + configuration.files {
                    !(it instanceof ProjectDependency)
                }
            }
            return (included - excluded).collect { new ModuleLibrary([getPath(it)], [] as Set, [] as Set, [] as Set, scope)}
        }
        return []
    }

    protected Path getPath(File file) {
        new Path(outputFile.parentFile, '$MODULE_DIR$', file)
    }

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