package org.gradle.plugins.idea

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.Project

public class Util {
    static void prettyPrintXML(File outputFile, GPathResult root) {
        Writable xml = new StreamingMarkupBuilder().bind {
            mkp.xmlDeclaration()
            mkp.yield root
        }

        TransformerFactory factory = TransformerFactory.newInstance()
        def transformer = factory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, 'yes')

        File backupFile = new File(outputFile.getParentFile(), outputFile.getName() + '.backup')
        if (outputFile.exists()) {
            outputFile.renameTo(backupFile)
        } else {
            outputFile.parentFile.mkdir() // make sure the parent dir is created
        }

        try {
            outputFile.withWriter("UTF-8") {  Writer writer ->
                StreamResult result = new StreamResult(writer)
                transformer.transform(new StreamSource(new ByteArrayInputStream(xml.toString().bytes)), result)
            }

            // if successful, remove up the backup
            backupFile.delete()
        }
        catch (IOException exception) {
            backupFile.renameTo(outputFile)
            throw exception
        }
    }

    static GPathResult getSourceRoot(File outputFile, String defaultXml, def logger) {
        XmlSlurper slurper = new XmlSlurper();
        if (outputFile.exists()) {
            try {
                return slurper.parse(outputFile);
            }
            catch (Exception exception) {
                logger.warn("Error opening file $outputFile. Pretending file does not exist");
            }
        }
        return slurper.parseText(defaultXml);
    }


    static Collection<ProjectDependency> getProjectDeps(Project proj) {
        def config = proj.configurations.findByName('runtime')
        if (config != null) {
            return config.getAllDependencies(ProjectDependency)
        } else {
            return []
        }
    }

    static String getRelativeURI(File rootDir, String rootDirString, File file) {
        String relpath = getRelativePath(rootDir, rootDirString, file)
        return relativePathToURI(relpath)
    }

    static String getRelativePath(File rootDir, String rootDirString, File file) {
        String relpath = getRelativePath(rootDir, file)
        return rootDirString + '/' + relpath
    }

    static String relativePathToURI(String relpath) {
        if (relpath.endsWith('.jar'))
            return 'jar://' + relpath + '!/';
        else
            return 'file://' + relpath;
    }

    //This gets a relative path even if neither path is an ancestor of the other.
    // implemenation taken from http://www.devx.com/tips/Tip/13737 and slighly modified
    //@param relativeTo  the destinationFile
    //@param fromFile    where the relative path starts

    public static String getRelativePath(File relativeTo, File fromFile) {
        return matchPathLists(getPathList(relativeTo), getPathList(fromFile))
    }

    private static List getPathList(File f) {
        List list = []
        File r = f.getCanonicalFile()
        while (r != null) {
            list.add(r.getName())
            r = r.getParentFile()
        }

        return list
    }

    private static String matchPathLists(List r, List f) {
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
}