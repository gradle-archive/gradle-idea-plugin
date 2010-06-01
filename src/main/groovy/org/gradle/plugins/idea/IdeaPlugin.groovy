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

import org.gradle.api.JavaVersion

/**
 * @author Hans Dockter
 *
 * When applied to a project, this plugin add one IdeaModule task. If the project is the root project, the plugin
 * adds also an IdeaProject task.
 *
 * If the java plugin is or has been added to a project where this plugin is applied to, the IdeaModule task
 */
class IdeaPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configure(project) {
            apply plugin: 'base' // We apply the base plugin to have the clean<taskname> rule
            task('cleanIdea', description: 'Cleans IDEA project files (IML, IPR)')
            task('idea', description: 'Generates IDEA project files (IML, IPR)')
            if (isRoot(project)) {
                task('ideaProject', description: 'Generates IDEA project file (IPR)', type: IdeaProject) {
                    outputFile = new File(project.projectDir, project.name + ".ipr")
                    subprojects = rootProject.allprojects
                    javaVersion = JavaVersion.VERSION_1_6.toString()
                    wildcards = ['!?*.java', '!?*.groovy']
                }
                idea.dependsOn 'ideaProject'

                project.cleanIdea.dependsOn "cleanIdeaProject"
            }
            task('ideaModule', description: 'Generates IDEA module files (IML)', type: IdeaModule) {
                outputFile = new File(project.projectDir, project.name + ".iml")
                imlDir = outputFile.getParentFile();
                moduleDir = project.projectDir
                sourceDirs = []
                testSourceDirs = []
                excludeDirs = []
            }
            idea.dependsOn 'ideaModule'

            cleanIdea.dependsOn "cleanIdeaModule"
            
            plugins.withType(JavaPlugin).allPlugins {
                if (isRoot(project)) {
                    ideaProject {
                        javaVersion = project.sourceCompatibility
                    }
                }
                ideaModule {
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

