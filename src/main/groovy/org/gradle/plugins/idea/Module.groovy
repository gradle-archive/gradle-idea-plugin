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
import org.gradle.listener.ListenerBroadcast

/**
 * Represents the customizable elements of an iml (via XML hooks everything of the iml is customizable).
 *
 * @author Hans Dockter
 */
class Module {
    /**
     * The foldes for the production code. Must not be null.
     */
    Set sourceFolders = [] as LinkedHashSet

    /**
     * The folders for the test code. Must not be null.
     */
    Set testSourceFolders = [] as LinkedHashSet

    /**
     * Folders to be excluded. Must not be null.
     */
    Set excludeFolders = [] as LinkedHashSet

    /**
     * The dir for the production source classes. If null this output dir element is not added.
     */
    Path outputDir

    /**
     * The dir for the compiled test source classes. If null this output element is not added.
     */
    Path testOutputDir

    /**
     * The dependencies of this module. Must not be null. Has instances of type {@link Dependency}.
     */
    Set dependencies = [] as LinkedHashSet
    
    private Node xml

    private ListenerBroadcast<Action> withXmlActions

    def Module(Set sourceFolders, Set testSourceFolders, Set excludeFolders, Path outputDir, Path testOutputDir, Set dependencies,
               Reader inputXml, ListenerBroadcast<Action> beforeConfiguredActions,
               ListenerBroadcast<Action> whenConfiguredActions, ListenerBroadcast<Action> withXmlActions) {
        initFromXml(inputXml)

        beforeConfiguredActions.source.execute(this)

        this.sourceFolders.addAll(sourceFolders);
        this.testSourceFolders.addAll(testSourceFolders);
        this.excludeFolders.addAll(excludeFolders);
        if (outputDir) this.outputDir = outputDir;
        if (testOutputDir) this.testOutputDir = testOutputDir;
        this.dependencies.addAll(dependencies);
        this.withXmlActions = withXmlActions;

        whenConfiguredActions.source.execute(this)
    }

    private def initFromXml(Reader inputXml) {
        Reader reader = inputXml ?: new InputStreamReader(getClass().getResourceAsStream('defaultModule.xml'))
        xml = new XmlParser().parse(reader)

        readSourceAndExcludeFolderFromXml()
        readOutputDirsFromXml()
        readDependenciesFromXml()
    }

    private def readOutputDirsFromXml() {
        def outputDirUrl = findOutputDir()?.@url
        def testOutputDirUrl = findTestOutputDir()?.@url
        this.outputDir = outputDirUrl ? new Path(outputDirUrl) : null
        this.testOutputDir = testOutputDirUrl ? new Path(testOutputDirUrl) : null
    }

    private def readDependenciesFromXml() {
        return findOrderEntries().each { orderEntry ->
            switch (orderEntry.@type) {
                case "module-library":
                    Set classes = orderEntry.library.CLASSES.root.collect { new Path(it.@url)}
                    Set jarDirectories = orderEntry.library.jarDirectory.collect { new JarDirectory(new Path(it.@url), Boolean.parseBoolean(it.@recursive)) }
                    dependencies.add(new ModuleLibrary(classes, [] as Set, [] as Set, jarDirectories, orderEntry.@scope))
                    break
                case "module":
                    dependencies.add(new ModuleDependency(orderEntry.@'module-name', orderEntry.@scope))
            }
        }
    }

    private def readSourceAndExcludeFolderFromXml() {
        findSourceFolder().each { sourceFolder ->
            if (sourceFolder.@isTestSource == 'false') {
                this.sourceFolders.add(new Path(sourceFolder.@url))
            } else {
                this.testSourceFolders.add(new Path(sourceFolder.@url))
            }
        }
        findExcludeFolder().each { excludeFolder ->
            this.excludeFolders.add(new Path(excludeFolder.@url))
        }
    }

    /**
     * Generates the XML for the iml.
     *
     * @param writer The writer where the iml xml is generated into.
     */
    def toXml(Writer writer) {
        removeSourceAndExcludeFolderFromXml()
        addSourceAndExcludeFolderToXml()
        addOutputDirsToXml()

        removeDependenciesFromXml()
        addDependenciesToXml()

        withXmlActions.source.execute(xml)

        new XmlNodePrinter(new PrintWriter(writer)).print(xml)
    }

    private def addOutputDirsToXml() {
        if (outputDir) {
            findOrCreateOutputDir().@url = outputDir.url
        }
        if (testOutputDir) {
            findOrCreateTestOutputDir().@url = testOutputDir.url
        }
    }

    private Node findOrCreateOutputDir() {
        return findOutputDir() ?: findNewModuleRootManager().appendNode("output")
    }

    private Node findOrCreateTestOutputDir() {
        return findTestOutputDir() ?: findNewModuleRootManager().appendNode("output-test")
    }

    private Set addDependenciesToXml() {
        return dependencies.each { Dependency dependency ->
            dependency.addToNode(findNewModuleRootManager())
        }
    }

    private def addSourceAndExcludeFolderToXml() {
        sourceFolders.each { Path path ->
            findContent().appendNode('sourceFolder', [url: path.url, isTestSource: 'false'])
        }
        testSourceFolders.each { Path path ->
            findContent().appendNode('sourceFolder', [url: path.url, isTestSource: 'true'])
        }
        excludeFolders.each { Path path ->
            findContent().appendNode('excludeFolder', [url: path.url])
        }
    }

    private def removeSourceAndExcludeFolderFromXml() {
        findSourceFolder().each { sourceFolder ->
            findContent().remove(sourceFolder)
        }
        findExcludeFolder().each { excludeFolder ->
            findContent().remove(excludeFolder)
        }
    }

    private def removeDependenciesFromXml() {
        return findOrderEntries().each { orderEntry ->
            if (isDependencyOrderEntry(orderEntry)) {
                findNewModuleRootManager().remove(orderEntry)
            }
        }
    }

    protected boolean isDependencyOrderEntry(def orderEntry) {
        ['module-library', 'module'].contains(orderEntry.@type)
    }

    private Node findContent() {
        findNewModuleRootManager().content[0]
    }

    private def findSourceFolder() {
        return findContent().sourceFolder
    }

    private def findExcludeFolder() {
        return findContent().excludeFolder
    }

    private Node findOutputDir() {
        return findNewModuleRootManager().output[0]
    }

    private Node findNewModuleRootManager() {
        return xml.component.find { it.@name == 'NewModuleRootManager'}
    }

    private Node findTestOutputDir() {
        return findNewModuleRootManager().'output-test'[0]
    }

    private def findOrderEntries() {
        return findNewModuleRootManager().orderEntry
    }


    boolean equals(o) {
        if (this.is(o)) return true;

        if (getClass() != o.class) return false;

        Module module = (Module) o;

        if (dependencies != module.dependencies) return false;
        if (excludeFolders != module.excludeFolders) return false;
        if (outputDir != module.outputDir) return false;
        if (sourceFolders != module.sourceFolders) return false;
        if (testOutputDir != module.testOutputDir) return false;
        if (testSourceFolders != module.testSourceFolders) return false;

        return true;
    }

    int hashCode() {
        int result;

        result = (sourceFolders != null ? sourceFolders.hashCode() : 0);
        result = 31 * result + (testSourceFolders != null ? testSourceFolders.hashCode() : 0);
        result = 31 * result + (excludeFolders != null ? excludeFolders.hashCode() : 0);
        result = 31 * result + outputDir.hashCode();
        result = 31 * result + testOutputDir.hashCode();
        result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
        return result;
    }


    public String toString() {
        return "Module{" +
                "dependencies=" + dependencies +
                ", sourceFolders=" + sourceFolders +
                ", testSourceFolders=" + testSourceFolders +
                ", excludeFolders=" + excludeFolders +
                ", outputDir=" + outputDir +
                ", testOutputDir=" + testOutputDir +
                '}';
    }
}
