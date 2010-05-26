package org.gradle.plugins.idea

import org.gradle.api.DefaultTask
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.tasks.TaskAction

public class IdeaProject extends DefaultTask {
    def subprojects;
    
    File outputFile;

    @TaskAction
    void updateXML() {
        GPathResult root = Util.getSourceRoot(outputFile, defaultXml, logger);
        def projectModuleManager = root.component.find { it.@name == 'ProjectModuleManager' }
        projectModuleManager.replaceNode {
            component(name: 'ProjectModuleManager') {
                modules {
                    subprojects.each { subproject ->
                        if (subproject.plugins.hasPlugin(IdeaPlugin)) {
                            File imlFile = subproject.ideaModule.outputFile
                            String projectPath = this.toProjectPath(imlFile)
                            module(fileurl: Util.relativePathToURI(projectPath), filepath: projectPath)
                        }
                    }
                }
            }
        }
        Util.prettyPrintXML(outputFile, root);
    }

    def String getDefaultXml() {
        '''<project relativePaths="true" version="4">
            <component name="ProjectModuleManager"/>
            <component name="ProjectRootManager" version="2" assert-keyword="true" jdk-15="true" project-jdk-type="JavaSDK">
              <output url="file://$PROJECT_DIR$/out" />
            </component>
          </project>
          '''
    }

    private String toProjectPath(File file) {
        return Util.getRelativePath(project.projectDir, '$PROJECT_DIR$', file)
    }
}
