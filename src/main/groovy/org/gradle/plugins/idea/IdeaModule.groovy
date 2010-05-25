package org.gradle.plugins.idea

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.SourceSet
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction
import org.gradle.api.plugins.JavaPlugin

public class IdeaModule extends DefaultTask {
    File imlDir
    File moduleDir
    File outputFile
    SourceSet mainSource
    SourceSet testSource
    Map scopes = [:]

    @TaskAction
    void updateXML() {
        GPathResult xmlRoot = getSourceRoot(getOutputFile());

        def moduleRootManager = xmlRoot.component.find { it.@name == 'NewModuleRootManager' }
        moduleRootManager.replaceNode {
            component(name: 'NewModuleRootManager', 'inherit-compiler-output': 'false') {
                content(url: toModuleURL(moduleDir)) {
                    if (mainSource) {
                        output(url: toModuleURL(mainSource.classesDir))
                        mainSource?.allSource?.sourceTrees*.srcDirs*.each { File file ->
                            sourceFolder(url: toModuleURL(file), isTestSource: 'false')
                        }
                    }
                    if (testSource) {
                        'output-test'(url: toModuleURL(testSource.classesDir))
                        testSource?.allSource?.sourceTrees*.srcDirs*.each { File file ->
                            sourceFolder(url: toModuleURL(file), isTestSource: 'true')
                        }
                    }
                }
            }

            orderEntry(type: 'inheritedJdk')
            orderEntry(type: 'sourceFolder', forTests: 'false')

            scopes.each {scope, configuration ->
                def libs = getExternalDependencies(scope)
                libs.each { lib ->
                    orderEntry(type: 'module-library', scope: "${scope.toUpperCase()}", exported: '') {
                        library {
                            CLASSES() { root(url: toModuleURL(lib)) }
                            JAVADOC()
                            SOURCES()
                        }
                    }
                }
                def projectDependencies = getProjectDependencies(scope)
                projectDependencies.each { projectDependency ->
                    orderEntry(type: 'module', 'module-name': projectDependency.dependencyProject.name, exported: '')
                }
            }

            orderEntryProperties()
        }
        Util.prettyPrintXML(getOutputFile(), xmlRoot);
    }


    def getProjectDependencies(String scope) {
        if (scopes[scope]) {
            return scopes[scope].inject([]) { result, configuration ->
                result += configuration.getAllDependencies(ProjectDependency)
            }
        }
        return []
    }

    def getExternalDependencies(String scope) {
        if (scopes[scope]) {
            return scopes[scope].inject([]) { result, configuration ->
                result += configuration.files {
                    !(it instanceof ProjectDependency)
                }
            }
        }
        return []
    }

    String toModuleURL(File file) {
        Util.getRelativeURI(imlDir, '$MODULE_DIR$', file)
    }

    def String getDefaultXML() {
        '''<module relativePaths="true" type="JAVA_MODULE" version="4">
        <component name="NewModuleRootManager"/>
        <component name="FacetManager"/>
      </module>
      '''
    }

    private GPathResult getSourceRoot(File outputFile) {
        XmlSlurper slurper = new XmlSlurper();
        if (outputFile.exists()) {
            try {
                return slurper.parse(outputFile);
            }
            catch (Exception exception) {
                System.out.println("Error opening existing file, pretending file does not exist");
            }
        }
        return slurper.parseText(defaultXML);
    }
}