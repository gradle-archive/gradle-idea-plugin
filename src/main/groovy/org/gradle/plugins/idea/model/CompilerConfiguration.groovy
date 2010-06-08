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
 * Represents the compiler settings for the project.
 *
 * @author John Murph
 */
class CompilerConfiguration {
   /**
    * The default compiler to use inside of IDEA.
    */
   String defaultCompiler = 'Javac'

   /**
    * A set of regular expressions (as Strings) of what files to consider resources.
    * e.g. '.+\.(properties|xml|html|dtd|tld)'
    */
   Set resourceExtensions = []

   /**
    * A set of wildcard string to be included/excluded from the resources.
    */
   Set wildcards = []

   /**
    * A set of CompilerExclude objects that indicate urls to exclude and
    * whether the exclude includes subdirectories 
    */
   Set excludes = []


   def CompilerConfiguration()
   {
   }

   def CompilerConfiguration(defaultCompiler, resourceExtensions, wildcards, excludes)
   {
      this.defaultCompiler = defaultCompiler;
      this.resourceExtensions = resourceExtensions;
      this.wildcards = wildcards;
      this.excludes = excludes;
   }


   boolean equals(o)
   {
      if (this.is(o)) return true;

      if (getClass() != o.class) return false;

      CompilerConfiguration that = (CompilerConfiguration) o;

      if (defaultCompiler != that.defaultCompiler) return false;
      if (resourceExtensions != that.resourceExtensions) return false;
      if (wildcards != that.wildcards) return false;
      if (excludes != that.excludes) return false;

      return true;
   }

   int hashCode()
   {
      int result;

      result = defaultCompiler.hashCode();
      result = 31 * result + resourceExtensions.hashCode();
      result = 31 * result + wildcards.hashCode();
      result = 31 * result + excludes.hashCode();
      return result;
   }

   public String toString()
   {
      return "CompilerConfiguration{" +
            "defaultCompiler='" + defaultCompiler + '\'' +
            ", resourceExtensions=" + resourceExtensions +
            ", wildcards=" + wildcards +
            ", excludes=" + excludes +
            '}';
   }

   def configure(Set wildcards) {
      this.wildcards.addAll(wildcards)
   }

   def initFromXml(Node xml) {
      Node config = findCompilerConfiguration(xml)

      def compilerAttrs = findDefaultCompiler(config).attributes()
      if (compilerAttrs.containsKey('value'))
         defaultCompiler = compilerAttrs.value

      findResourceExtensions(config).entry.each { entry ->
          this.resourceExtensions.add(entry.@name)
      }

      findWildcardResourcePatterns(config).entry.each { entry ->
          this.wildcards.add(entry.@name)
      }

      findCompilerExcludes(config).directory.each { directory ->
          this.excludes.add(new CompilerExclude(directory.@url, directory.@includeSubdirectories))
      }
   }

   def toXml(Node xml) {
      Node config = findCompilerConfiguration(xml)

      findDefaultCompiler(config).@value = defaultCompiler

      findResourceExtensions(config).replaceNode {
         resourceExtensions {
            this.resourceExtensions.each { extension ->
               entry(name: extension)
            }
         }
      }

      findWildcardResourcePatterns(config).replaceNode {
         wildcardResourcePatterns {
            this.wildcards.each { wildcard ->
               entry(name: wildcard)
            }
         }
      }

      addOrReplace(config, findCompilerExcludes(config)) {
         excludeFromCompile {
            this.excludes.each { exclude ->
               directory(url: exclude.url, includeSubdirectories: exclude.recursive)
            }
         }
      }
   }

   public void addExclude(String url, boolean recursive) {
      excludes.add new CompilerExclude(url, recursive)
   }

   private def addOrReplace(def parent, def node, Closure closure) {
      if (node) {
         node.replaceNode(closure)
      } else {
         getNewNodes(closure).each { parent.append(it) }
      }
   }

   private def getNewNodes(Closure closure) {
      NodeBuilder builder = new NodeBuilder();
      Node newNode = (Node) builder.invokeMethod("dummyNode", closure);
      newNode.children();
   }

   private def findCompilerConfiguration(Node xml) {
       xml.component.find { it.@name == 'CompilerConfiguration' }
   }

   private def findDefaultCompiler(Node config) {
       config.option.find { it.@name == 'DEFAULT_COMPILER' }
   }

   private def findResourceExtensions(Node config) {
       config.resourceExtensions
   }

   private def findWildcardResourcePatterns(Node config) {
       config.wildcardResourcePatterns
   }

   private def findCompilerExcludes(Node config) {
       config.excludeFromCompile
   }
}
