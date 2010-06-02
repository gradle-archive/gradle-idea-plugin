package org.gradle.plugins.idea

import org.gradle.api.Project

class IdeaConvention {
    String gradleCacheVariable = 'GRADLE_CACHE'
    File gradleCacheHome

    def IdeaConvention(Project project) {
        gradleCacheHome = project.gradle.getGradleUserHomeDir() + "/cache"
    }
}
