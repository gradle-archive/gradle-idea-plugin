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
package org.gradle.plugins.intellij.model

/**
 * Represents a path in a format as used often in ipr and iml files.
 *
 * @author Hans Dockter
 */
class Path {
    /**
     * The url of the path. Must not be null
     */
    String url

    /**
     * The path string of this path. Might be null.
     */
    String path

    def Path(rootDir, rootDirString, file) {
        path = getRelativePath(rootDir, rootDirString, file)
        url = relativePathToURI(path)
    }

    def Path(url) {
        this(url, null)
    }

    def Path(url, path) {
        assert url != null
        this.url = url;
        this.path = path;
    }

    private String getRelativePath(File rootDir, String rootDirString, File file) {
        String relpath = getRelativePath(rootDir, file)
        return rootDirString + '/' + relpath
    }

    private String getRelativeURI(String relativePath) {
        String relpath = getRelativePath(rootDir, rootDirString, file)
        return relativePathToURI(relpath)
    }

    private String relativePathToURI(String relpath) {
        if (relpath.endsWith('.jar'))
            return 'jar://' + relpath + '!/';
        else
            return 'file://' + relpath;
    }

    // This gets a relative path even if neither path is an ancestor of the other.
    // implemenation taken from http://www.devx.com/tips/Tip/13737 and slighly modified
    //@param relativeTo  the destinationFile
    //@param fromFile    where the relative path starts
    protected String getRelativePath(File relativeTo, File fromFile) {
        return matchPathLists(getPathList(relativeTo), getPathList(fromFile))
    }

    private List getPathList(File f) {
        List list = []
        File r = f.getCanonicalFile()
        while (r != null) {
            list.add(r.getName())
            r = r.getParentFile()
        }

        return list
    }

    private String matchPathLists(List r, List f) {
        StringBuilder s = new StringBuilder();

        // eliminate the common root
        int i = r.size() - 1
        int j = f.size() - 1
        while ((i >= 0) && (j >= 0) && (r[i] == f[j])) {
            i--
            j--
        }

        // for each remaining level in the relativeTo path, add a ..
        for (; i >= 0; i--)
            s.append('..').append('/')

        // for each level in the file path, add the path
        for (; j >= 1; j--)
            s.append(f[j]).append('/')

        // add the file name and return the result
        return s.append(f[j]).toString()
    }


    boolean equals(o) {
        if (this.is(o)) return true;

        if (getClass() != o.class) return false;

        Path path1 = (Path) o;

        if (path != path1.path) return false;
        if (url != path1.url) return false;

        return true;
    }

    int hashCode() {
        int result;

        result = url.hashCode();
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "Path{" +
                "url='" + url + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
