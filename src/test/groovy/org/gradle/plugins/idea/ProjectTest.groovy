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

import spock.lang.Specification
import org.gradle.listener.ListenerBroadcast
import org.gradle.api.Action


/**
 * @author Hans Dockter
 */
class ProjectTest extends Specification {
    Project project

    def initWithReaderAndNoJdkAndNoWildcards() {
        project = createProject(javaVersion: "1.4", reader: customProjectReader)

        expect:
        project.modulePaths == [new Path('file://$PROJECT_DIR$/gradle-intellij-plugin.iml', '$PROJECT_DIR$/gradle-intellij-plugin.iml')] as Set
        project.wildcards == ["?*.gradle", "?*.grails"] as Set
        project.jdk == new Jdk(true, false, "1.4")
    }

    def initWithReaderAndJdkAndWildcards_shouldBeMerged() {
        project = createProject(wildcards: ['?*.groovy'] as Set, reader: customProjectReader)

        expect:
        project.modulePaths == [new Path('file://$PROJECT_DIR$/gradle-intellij-plugin.iml', '$PROJECT_DIR$/gradle-intellij-plugin.iml')] as Set
        project.wildcards == ["?*.gradle", "?*.grails", "?*.groovy"] as Set
        project.jdk == new Jdk("1.6")
    }

    def initWithNullReader_shouldUseDefaults() {
        project = createProject(wildcards: ['!?*.groovy'] as Set)

        expect:
        project.modulePaths.size() == 0
        project.wildcards == ["!?*.groovy"] as Set
        project.jdk == new Jdk("1.6")
    }

    def toXml_shouldContainCustomValues() {
        when:
        project = createProject(wildcards: ['?*.groovy'] as Set, reader: customProjectReader)

        then:
        project == createProject(wildcards: ['?*.groovy'] as Set, reader: toXmlReader)
    }

    def toXml_shouldContainSkeleton() {
        when:
        project = createProject(reader: customProjectReader)

        then:
        new XmlParser().parse(toXmlReader).toString() == project.xml.toString()
    }

    def beforeConfigured() {
        ListenerBroadcast beforeConfiguredActions = new ListenerBroadcast(Action)
        beforeConfiguredActions.add("execute") { Project ideaProject ->
            ideaProject.modulePaths.clear()
        }
        def modulePaths = [new Path("a", "b")] as Set

        when:
        project = createProject(modulePaths: modulePaths, reader: customProjectReader, beforeConfiguredActions: beforeConfiguredActions)

        then:
        createProject(reader: toXmlReader).modulePaths == modulePaths
    }

    def whenConfigured() {
        def moduleFromInitialXml = null
        def moduleFromProjectConstructor = new Path("a", "b")
        def moduleAddedInWhenConfiguredAction = new Path("c", "d")
        ListenerBroadcast beforeConfiguredActions = new ListenerBroadcast(Action)
        beforeConfiguredActions.add("execute") { Project ideaProject ->
            moduleFromInitialXml = (ideaProject.modulePaths as List)[0]
        }
        ListenerBroadcast whenConfiguredActions = new ListenerBroadcast(Action)
        whenConfiguredActions.add("execute") { Project ideaProject ->
            assert ideaProject.modulePaths.contains(moduleFromInitialXml)
            assert ideaProject.modulePaths.contains(moduleFromProjectConstructor)
            ideaProject.modulePaths.add(moduleAddedInWhenConfiguredAction)
        }

        when:
        project = createProject(modulePaths: [moduleFromProjectConstructor] as Set, reader: customProjectReader,
                beforeConfiguredActions: beforeConfiguredActions,
                whenConfiguredActions: whenConfiguredActions)

        then:
        createProject(reader: toXmlReader).modulePaths == [moduleFromInitialXml, moduleFromProjectConstructor, moduleAddedInWhenConfiguredAction] as Set
    }

    private StringReader getToXmlReader() {
        StringWriter toXmlText = new StringWriter()
        project.toXml(toXmlText)
        return new StringReader(toXmlText.toString())
    }

    def withXml() {
        ListenerBroadcast withXmlActions = new ListenerBroadcast(Action)
        project = createProject(reader: customProjectReader, withXmlActions: withXmlActions)

        when:
        def modifiedVersion
        withXmlActions.add("execute") { xml ->
            xml.@version += 'x'
            modifiedVersion = xml.@version
        }

        then:
        new XmlParser().parse(toXmlReader).@version == modifiedVersion
    }

    private InputStreamReader getCustomProjectReader() {
        return new InputStreamReader(getClass().getResourceAsStream('customProject.xml'))
    }

    private Project createProject(Map customArgs) {
        ListenerBroadcast dummyBroadcast = new ListenerBroadcast(Action)
        Map args = [modulePaths: [] as Set, javaVersion: "1.6", wildcards: [] as Set, reader: null,
                beforeConfiguredActions: dummyBroadcast, whenConfiguredActions: dummyBroadcast, withXmlActions: dummyBroadcast] + customArgs
        return new Project(args.modulePaths, args.javaVersion, args.wildcards, args.reader,
                args.beforeConfiguredActions, args.whenConfiguredActions, args.withXmlActions)
    }
}
