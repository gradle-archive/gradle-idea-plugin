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
 * Represents the compiler exclude settings for the project.  Used by the
 * CompilerConfiguration class.
 *
 * @author John Murph
 */
class CompilerExclude {
   /**
    * The url of the exclude.
    */
   String url

   /**
    * Whether the exclude recursively excludes subdirectories.
    */
   boolean recursive

   def CompilerExclude(url, recursive)
   {
      this.url = url;
      this.recursive = recursive;
   }

   boolean equals(o)
   {
      if (this.is(o)) return true;

      if (getClass() != o.class) return false;

      CompilerExclude that = (CompilerExclude) o;

      if (recursive != that.recursive) return false;
      if (url != that.url) return false;

      return true;
   }

   int hashCode()
   {
      int result;

      result = url.hashCode();
      result = 31 * result + (recursive ? 1 : 0);
      return result;
   }

   public String toString()
   {
      return "CompilerExclude{" +
            "url='" + url + '\'' +
            ", recursive=" + recursive +
            '}';
   }
}
