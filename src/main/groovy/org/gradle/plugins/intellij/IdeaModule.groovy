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
package org.gradle.plugins.intellij

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.listener.ListenerBroadcast
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.Configuration

import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.Input
import org.gradle.api.artifacts.ResolvedConfiguration

import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.specs.Specs
import org.gradle.plugins.intellij.model.Module
import org.gradle.plugins.intellij.model.Path
import org.gradle.plugins.intellij.model.ModuleLibrary

/**
 * @author Hans Dockter
 */
public class IdeaModule extends DefaultTask {
    /**
     * The content root directory of the module. Must not be null.
     */
    @InputFiles
    File moduleDir

    /**
     * The iml file. Used to look for existing files as well as the target for generation. Must not be null. 
     */
    @OutputFile
    File outputFile

    /**
     * The dirs containing the production sources. Must not be null.
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
     * Whether to download and add sources associated with the dependency jars. Defaults to true. 
     */
    @Input
    boolean downloadSources = true

    @Input
    boolean downloadJavadoc = true

    /**
     * The keys of this map are the Intellij scopes. Each key points to another map that has two keys, plus and minus.
     * The values of those keys are sets of {@link org.gradle.api.artifacts.Configuration} objects. The files of the
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
            result + (getModuleLibraries(scope) + getModules(scope))
        }
    }

    protected Set getModules(String scope) {
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

    protected Set getModuleLibraries(String scope) {
        if (scopes[scope]) {
            Set firstLevelDependencies = getScopeDependencies(scopes[scope])
            ResolvedConfiguration resolvedConfiguration = project.configurations.detachedConfiguration((firstLevelDependencies as Dependency[])).resolvedConfiguration
            def allResolvedDependencies = getAllDeps(resolvedConfiguration.firstLevelModuleDependencies)

            Set sourceDependencies = getResolvableDependenciesForAllResolvedDependencies(allResolvedDependencies) { dependency ->
                addSourceArtifact(dependency)
            }
            Map sourceFiles = downloadSources ?  getFiles(sourceDependencies, "sources") : [:]

            Set javadocDependencies = getResolvableDependenciesForAllResolvedDependencies(allResolvedDependencies) { dependency ->
                addJavadocArtifact(dependency)    
            }
            Map javadocFiles = downloadJavadoc ? getFiles(javadocDependencies, "javadoc") : [:]

            return resolvedConfiguration.getFiles(Specs.SATISFIES_ALL).collect { File binaryFile ->
                File sourceFile = sourceFiles[binaryFile.name]
                File javadocFile = javadocFiles[binaryFile.name]
                new ModuleLibrary([getPath(binaryFile)] as Set, javadocFile ? [getPath(javadocFile)] as Set : [] as Set, sourceFile ? [getPath(sourceFile)] as Set : [] as Set, [] as Set, scope)
            }
        }
        return []
    }

    private Set getScopeDependencies(configurations) {
        Set firstLevelDependencies = []
        configurations.plus.each { configuration ->
            firstLevelDependencies += configuration.getAllDependencies(ExternalDependency)
        }
        configurations.minus.each { configuration ->
            firstLevelDependencies -= configuration.getAllDependencies(ExternalDependency)
        }
        return firstLevelDependencies
    }

    private def getFiles(Set dependencies, String classifier) {
        return project.configurations.detachedConfiguration((dependencies as Dependency[])).files.inject([:]) { result, sourceFile ->
            String key = sourceFile.name.replace("-${classifier}.jar", '.jar')
            result[key] = sourceFile
            result
        }
    }

    private List getResolvableDependenciesForAllResolvedDependencies(Set allResolvedDependencies, Closure configureClosure) {
        return allResolvedDependencies.collect { ResolvedDependency resolvedDependency ->
            def dependency = new DefaultExternalModuleDependency(resolvedDependency.moduleGroup, resolvedDependency.moduleName, resolvedDependency.moduleVersion,
                    resolvedDependency.configuration)
            dependency.transitive = false
            configureClosure.call(dependency)
            dependency
        }
    }

    protected Set getAllDeps(Set deps) {
        Set result = []
        deps.each { ResolvedDependency resolvedDependency ->
            if(resolvedDependency.children) {
                result.addAll(getAllDeps(resolvedDependency.children))
            }
            result.add(resolvedDependency)
        }
        result
    }

    protected def addSourceArtifact(DefaultExternalModuleDependency dependency) {
        dependency.artifact { artifact ->
            artifact.name = dependency.name
            artifact.type = 'source'
            artifact.extension = 'jar'
            artifact.classifier = 'sources'
        }
    }

    protected def addJavadocArtifact(DefaultExternalModuleDependency dependency) {
        dependency.artifact { artifact ->
            artifact.name = dependency.name
            artifact.type = 'javadoc'
            artifact.extension = 'jar'
            artifact.classifier = 'javadoc'
        }
    }


    protected def getSources(Map configurations) {
        def sourceDependencies = getExternalDependencies(configurations).collect { dependency ->
            DefaultExternalModuleDependency sourceDependency = new DefaultExternalModuleDependency(dependency.group, dependency.name, dependency.version)
            sourceDependency.transitive = false
            sourceDependency.artifact { artifact ->
                artifact.name = dependency.name
                artifact.type = 'source'
                artifact.extension = 'jar'
                artifact.classifier = 'sources'
            }
            sourceDependency
        }
        project.configurations.detachedConfiguration((sourceDependencies as Dependency[])).files
    }

    protected def getJavadocs(Map configurations) {
        def javadocDependencies = getExternalDependencies(configurations).collect { dependency ->
            DefaultExternalModuleDependency javadocDependency = new DefaultExternalModuleDependency(dependency.group, dependency.name, dependency.version)
            javadocDependency.transitive = false
            javadocDependency.artifact { artifact ->
                artifact.name = dependency.name
                artifact.type = 'javadoc'
                artifact.extension = 'jar'
                artifact.classifier = 'javadoc'
            }
            javadocDependency
        }
        project.configurations.detachedConfiguration((javadocDependencies as Dependency[])).files
    }

    protected Set getExternalDependencies(Map configurations) {
        Set externalDependencies = []
        configurations.plus.each { configuration ->
            externalDependencies += getExternalDependencies(configuration)
        }
        configurations.minus.each { configuration ->
            externalDependencies -= getExternalDependencies(configuration)
        }
        return externalDependencies
    }

    protected Collection getExternalDependencies(Configuration configuration) {
        return configuration.getAllDependencies(ExternalDependency)
    }

    protected Path getPath(File file) {
        new Path(outputFile.parentFile, '$MODULE_DIR$', file)
    }

    void withXml(Closure closure) {
        withXmlActions.add("execute", closure);
    }

    void beforeConfigured(Closure closure) {
        beforeConfiguredActions.add("execute", closure);
    }

    void whenConfigured(Closure closure) {
        whenConfiguredActions.add("execute", closure);
    }
}