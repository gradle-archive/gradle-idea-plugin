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

/**
 * @author Hans Dockter
 */
class ModuleDependency implements Dependency {
    String name
    String scope

    def ModuleDependency(name, scope) {
        this.name = name;
        this.scope = scope;
    }

    void addToNode(Node parentNode) {
        parentNode.appendNode('orderEntry', [type: 'module', 'module-name': name, exported: ''] + (scope ? [scope: scope] : [:]))
    }

    boolean equals(o) {
        if (this.is(o)) return true;

        if (getClass() != o.class) return false;

        ModuleDependency that = (ModuleDependency) o;

        if (name != that.name) return false;
        if (scope != that.scope) return false;

        return true;
    }

    int hashCode() {
        int result;

        result = name.hashCode();
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "ModuleDependency{" +
                "name='" + name + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }
}
