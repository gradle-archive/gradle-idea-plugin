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

/**
 * Represents an orderEntry of type module-library in the iml xml.
 *
 * @author Hans Dockter
 */
class ModuleLibrary implements Dependency {
    /**
     * A set of {@link org.gradle.plugins.idea.model.Path} instances for class libraries.
     */
    Set classes

    /**
     * A set of {@link org.gradle.plugins.idea.model.JarDirectory} instances for directories containing jars.
     */
    Set jarDirectories

    /**
     * A set of {@link org.gradle.plugins.idea.model.Path} instances for javadoc associated with the library elements.
     */
    Set javadoc

    /**
     * A set of {@link org.gradle.plugins.idea.model.Path} instances for source code associated with the library elements.
     */
    Set sources

    /**
     * The scope of this dependency. If null the scope attribute is not added.
     */
    String scope

    boolean exported = false

    def ModuleLibrary(classes, javadoc, sources, jarDirectories, scope) {
        this.classes = classes;
        this.jarDirectories = jarDirectories;
        this.javadoc = javadoc;
        this.sources = sources;
        this.scope = scope
    }

    void addToNode(Node parentNode) {
        Node libraryNode = parentNode.appendNode('orderEntry', [type: 'module-library'] + (exported ? [exported: ""] : [:]) + (scope ? [scope: scope] : [:])).appendNode('library')
        Node classesNode = libraryNode.appendNode('CLASSES')
        Node javadocNode = libraryNode.appendNode('JAVADOC')
        Node sourcesNode = libraryNode.appendNode('SOURCES')
        classes.each { Path path ->
            classesNode.appendNode('root', [url: path.url])
        }
        javadoc.each { Path path ->
            javadocNode.appendNode('root', [url: path.url])
        }
        sources.each { Path path ->
            sourcesNode.appendNode('root', [url: path.url])
        }
        jarDirectories.each { JarDirectory jarDirectory ->
            libraryNode.appendNode('jarDirectory', [url: jarDirectory.path.url, recursive: jarDirectory.recursive])
        }
    }


    boolean equals(o) {
        if (this.is(o)) return true;

        if (getClass() != o.class) return false;

        ModuleLibrary that = (ModuleLibrary) o;

        if (classes != that.classes) return false;
        if (jarDirectories != that.jarDirectories) return false;
        if (javadoc != that.javadoc) return false;
        if (scope != that.scope) return false;
        if (sources != that.sources) return false;

        return true;
    }

    int hashCode() {
        int result;

        result = classes.hashCode();
        result = 31 * result + jarDirectories.hashCode();
        result = 31 * result + javadoc.hashCode();
        result = 31 * result + sources.hashCode();
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "ModuleLibrary{" +
                "classes=" + classes +
                ", jarDirectories=" + jarDirectories +
                ", javadoc=" + javadoc +
                ", sources=" + sources +
                ", scope='" + scope + '\'' +
                '}';
    }
}
