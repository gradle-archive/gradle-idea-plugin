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
 * Represents a the information for the project JavaSDK. This information are attributes of the ProjectRootManager
 * element in the ipr.
 * 
 * @author Hans Dockter
 */
class Jdk {
    boolean assertKeyword
    boolean jdk15 = false
    String projectJdkName

    def Jdk(String javaVersion) {
        if (javaVersion.startsWith("1.4")) {
            assertKeyword = true
            jdk15 = false
        }
        else if (javaVersion.compareTo("1.5") >= 0) {
            assertKeyword = true
            jdk15 = true
        }
        else {
            assertKeyword = false
        }
        projectJdkName = javaVersion
    }

    def Jdk(assertKeyword, jdk15, projectJdkName) {
        this.assertKeyword = assertKeyword;
        this.jdk15 = jdk15;
        this.projectJdkName = projectJdkName;
    }

    boolean equals(o) {
        if (this.is(o)) return true;

        if (getClass() != o.class) return false;

        Jdk jdk = (Jdk) o;

        if (assertKeyword != jdk.assertKeyword) return false;
        if (jdk15 != jdk.jdk15) return false;
        if (projectJdkName != jdk.projectJdkName) return false;

        return true;
    }

    int hashCode() {
        int result;

        result = 31 * result + (assertKeyword ? 1 : 0);
        result = 31 * result + (jdk15 ? 1 : 0);
        result = 31 * result + (projectJdkName != null ? projectJdkName.hashCode() : 0);
        return result;
    }


    public String toString() {
        return "Jdk{" +
                "assertKeyword=" + assertKeyword +
                ", jdk15=" + jdk15 +
                ", projectJdkName='" + projectJdkName + '\'' +
                '}';
    }
}
