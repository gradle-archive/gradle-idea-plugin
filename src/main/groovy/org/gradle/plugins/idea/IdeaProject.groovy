package org.gradle.plugins.idea

import org.gradle.api.DefaultTask
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.tasks.TaskAction

public class IdeaProject extends DefaultTask {
    def subprojects;
    File outputFile;

    @TaskAction
    void updateXML() {
        GPathResult root = getSourceRoot(getOutputFile());
        def projectModuleManager = root.component.find { it.@name == 'ProjectModuleManager' }
        projectModuleManager.replaceNode {
            component(name: 'ProjectModuleManager') {
                modules {
                    subprojects.each { subproject ->
                        if (subproject.plugins.hasPlugin(IdeaPlugin)) {
                            File imlFile = subproject.ideaModule.outputFile
                            String projectPath = this.toProjectPath(imlFile)
                            module(fileurl: relativePathToURI(projectPath), filepath: projectPath)
                        }
                    }
                }
            }
        }
        Util.prettyPrintXML(outputFile, root);
    }

    def String getDefaultXML() {
        '''<project relativePaths="true" version="4">
            <component name="ProjectModuleManager"/>
            <component name="ProjectRootManager" version="2" assert-keyword="true" jdk-15="true" project-jdk-type="JavaSDK">
              <output url="file://$PROJECT_DIR$/out" />
            </component>
          </project>
          '''
    }


    private static String getRelativeURI(File rootDir, String rootDirString, File file) {
        String relpath = getRelativePath(rootDir, rootDirString, file)
        return relativePathToURI(relpath)
    }

    private static String getRelativePath(File rootDir, String rootDirString, File file) {
        String relpath = Util.getRelativePath(rootDir, file)
        return rootDirString + '/' + relpath
    }

    private static String relativePathToURI(String relpath) {
        if (relpath.endsWith('.jar'))
            return 'jar://' + relpath + '!/';
        else
            return 'file://' + relpath;
    }

    private String toProjectURL(File file) {
        return getRelativeURI(project.projectDir, '$PROJECT_DIR$', file)
    }

    def String toProjectPath(File file) {
        return getRelativePath(project.projectDir, '$PROJECT_DIR$', file)
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
