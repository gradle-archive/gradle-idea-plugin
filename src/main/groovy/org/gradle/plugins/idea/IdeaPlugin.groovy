package org.gradle.plugins.idea;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Delete

class IdeaPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configure(project) {
            boolean isRootProject = project.parent == null
            task('ideaClean', description: 'Cleans IDEA project files (IML, IPR and IWS)', type: Delete)
            task('idea', description: 'Generates IDEA project files (IML, IPR and IWS)')
            if (isRootProject) {
                task('ideaProject', description: 'Generates IDEA project file (IPR)', type: IdeaProject) {
                    outputFile = new File(project.projectDir, project.name + ".ipr")
                    subprojects = rootProject.allprojects
                }
                project.idea.dependsOn 'ideaProject'
                project.ideaClean.delete project.ideaProject.outputFile
            }
            task('ideaModule', description: 'Generates IDEA module files (IML)', type: IdeaModule) {
                outputFile = new File(project.projectDir, project.name + ".iml")
                imlDir = outputFile.getParentFile();
                moduleDir = project.projectDir
            }
            project.idea.dependsOn 'ideaModule'
            project.ideaClean.delete project.ideaModule.outputFile
            plugins.withType(JavaPlugin).allPlugins {
                project.ideaModule {
                    mainSource = project.sourceSets.main
                    testSource = project.sourceSets.test
                    def configurations = project.configurations
                    scopes = [
                            compile: [configurations.compile],
                            runtime: [configurations.runtime.copy()],
                            test: [configurations.testRuntime.copy(), configurations.testCompile.copy()]
                    ]
                }
            }
        }
    }
}

