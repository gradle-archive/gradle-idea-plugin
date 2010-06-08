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
package org.gradle.plugins.idea.model

import org.gradle.listener.ListenerBroadcast
import org.gradle.api.Action

/**
 * Represents the customizable elements of an ipr (via XML hooks everything of the ipr is customizable).
 *
 * @author Hans Dockter
 */
class Project {
    /**
     * A set of {@link ModulePath} instances pointing to the modules contained in the ipr.
     */
    Set modulePaths = []

    /**
     * Represents the compiler settings for the project.
     */
    CompilerConfiguration compilerConfiguration

    /**
     * Represent the jdk information of the project java sdk.
     */
    Jdk jdk

    private Node xml
    
    private ListenerBroadcast<Action> withXmlActions

    def Project(Set modulePaths, String javaVersion, Set wildcards, Reader inputXml, ListenerBroadcast<Action> beforeConfiguredActions,
                ListenerBroadcast<Action> whenConfiguredActions, ListenerBroadcast<Action> withXmlActions) {
        initFromXml(inputXml, javaVersion)

        beforeConfiguredActions.source.execute(this)

        this.modulePaths.addAll(modulePaths)
        compilerConfiguration.configure(wildcards)
        this.withXmlActions = withXmlActions

        whenConfiguredActions.source.execute(this)
    }

    private def initFromXml(Reader inputXml, String javaVersion) {
        Reader reader = inputXml ?: new InputStreamReader(getClass().getResourceAsStream('defaultProject.xml'))
        xml = new XmlParser().parse(reader)

        findModules().module.each { module ->
            this.modulePaths.add(new ModulePath(module.@fileurl, module.@filepath))
        }

        compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.initFromXml(xml)

        def jdkValues = findProjectRootManager().attributes()
        if (javaVersion) {
            jdk = new Jdk(javaVersion)
        } else {
            jdk = new Jdk(Boolean.parseBoolean(jdkValues.'assert-keyword'), Boolean.parseBoolean(jdkValues.'jdk-15'),
                          jdkValues.languageLevel, jdkValues.'project-jdk-name')
        }
    }

    def toXml(Writer writer) {
        findModules().replaceNode {
            modules {
                modulePaths.each { ModulePath modulePath ->
                    module(fileurl: modulePath.url, filepath: modulePath.path)
                }
            }
        }
        compilerConfiguration.toXml(xml)

        findProjectRootManager().@'assert-keyword' = jdk.assertKeyword
        findProjectRootManager().@'assert-jdk-15' = jdk.jdk15
        findProjectRootManager().@languageLevel = jdk.languageLevel
        findProjectRootManager().@'project-jdk-name' = jdk.projectJdkName

        withXmlActions.source.execute(xml)

        new XmlNodePrinter(new PrintWriter(writer)).print(xml)
    }

    private def findProjectRootManager() {
        return xml.component.find { it.@name == 'ProjectRootManager'}
    }

    private def findWildcardResourcePatterns() {
        xml.component.find { it.@name == 'CompilerConfiguration'}.wildcardResourcePatterns
    }

    private def findModules() {
        def moduleManager = xml.component.find { it.@name == 'ProjectModuleManager'}
        if (!moduleManager.modules) {
            moduleManager.appendNode('modules')
        }
        moduleManager.modules
    }


    boolean equals(o) {
        if (this.is(o)) return true;

        if (getClass() != o.class) return false;

        Project project = (Project) o;

        if (jdk != project.jdk) return false;
        if (modulePaths != project.modulePaths) return false;
        if (compilerConfiguration != project.compilerConfiguration) return false;

        return true;
    }

    int hashCode() {
        int result;

        result = (modulePaths != null ? modulePaths.hashCode() : 0);
        result = 31 * result + (compilerConfiguration != null ? compilerConfiguration.hashCode() : 0);
        result = 31 * result + (jdk != null ? jdk.hashCode() : 0);
        return result;
    }
}
