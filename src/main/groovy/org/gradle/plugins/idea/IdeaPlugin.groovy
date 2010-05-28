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
package org.gradle.plugins.idea;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.JavaVersion

/**
 * @author Hans Dockter
 */
class IdeaPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configure(project) {
            task('ideaClean', description: 'Cleans IDEA project files (IML, IPR and IWS)', type: Delete)
            task('idea', description: 'Generates IDEA project files (IML, IPR and IWS)')
            if (isRoot(project)) {
                task('ideaProject', description: 'Generates IDEA project file (IPR)', type: IdeaProject) {
                    outputFile = new File(project.projectDir, project.name + ".ipr")
                    subprojects = rootProject.allprojects
                    javaVersion = JavaVersion.VERSION_1_5.toString()
                    wildcards = ['!?*.java', '!?*.groovy']
                }
                project.idea.dependsOn 'ideaProject'
                project.ideaClean.delete project.ideaProject.outputFile
            }
            task('ideaModule', description: 'Generates IDEA module files (IML)', type: IdeaModule) {
                outputFile = new File(project.projectDir, project.name + ".iml")
                imlDir = outputFile.getParentFile();
                moduleDir = project.projectDir
                sourceDirs = []
                testSourceDirs = []
            }
            project.idea.dependsOn 'ideaModule'
            project.ideaClean.delete project.ideaModule.outputFile
            plugins.withType(JavaPlugin).allPlugins {
                if (isRoot(project)) {
                    project.ideaProject {
                        javaVersion = project.sourceCompatibility
                    }
                }
                project.ideaModule {
                    sourceDirs = project.sourceSets.main.allSource.sourceTrees.srcDirs.flatten()
                    testSourceDirs = project.sourceSets.test.allSource.sourceTrees.srcDirs.flatten()
                    outputDir = project.sourceSets.main.classesDir
                    testOutputDir = project.sourceSets.test.classesDir
                    def configurations = project.configurations
                    scopes = [
                            COMPILE: [plus: [configurations.compile], minus:[]],
                            RUNTIME: [plus: [configurations.runtime], minus: [configurations.compile]],
                            TEST: [plus: [configurations.testRuntime], minus: [configurations.runtime]]
                    ]
                }
            }
        }
    }

    private boolean isRoot(Project project) {
        return project.parent == null
    }
}

