package org.gradle.plugins.idea

import org.gradle.api.DefaultTask
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.maven.XmlProvider
import org.gradle.api.Action
import org.gradle.listener.ListenerBroadcast

public class IdeaProject extends DefaultTask {
    def subprojects;
    File outputFile;
    private ListenerBroadcast<Action> withXmlActions = new ListenerBroadcast<Action>(Action.class);

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
        Util.prettyPrintXML(outputFile, root, withXmlActions);
    }

    def String getDefaultXml() {
        getClass().getResource("defaultProject.xml").text
    }

    String toProjectPath(File file) {
        return Util.getRelativePath(project.projectDir, '$PROJECT_DIR$', file)
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
}
