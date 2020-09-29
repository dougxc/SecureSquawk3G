/*
 * @(#)ClassPath.java	1.40 00/02/02
 *
 * Copyright 1994-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package svmtools;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.File;
import java.io.IOException;
import java.util.zip.*;
import java.util.Vector;
/**
 * This class is used to represent a class path, which can contain both
 * directories and zip files.
 */
public
class ClassPath {
    static final char dirSeparator = File.pathSeparatorChar;

    /**
     * The original class path string
     */
    String pathstr;

    /**
     * List of class path entries
     */
    private ClassPathEntry[] path;

    /**
     * Build a class path from the specified path string
     */
    public ClassPath(String pathstr) {
    init(pathstr);
    }

    /**
     * Build a default class path from the path strings specified by
     * the properties sun.boot.class.path and env.class.path, in that
     * order. Be aware that sun.boot.class.path may not be defined if
     * we are running in the old-style launcher, in which case we just
     * use java.class.path.
     */
    public ClassPath() {
    String syscp = System.getProperty("sun.boot.class.path");
    String cp = null;

    if (syscp == null) {
        /* Must be oldjava. */
        cp = System.getProperty("java.class.path");
        if (cp == null)
        cp = ".";
    } else {
        String envcp = System.getProperty("env.class.path");
        if (envcp == null)
        envcp = ".";
        cp = syscp + File.pathSeparator + envcp;
    }
    init(cp);
    }

    private void init(String pathstr) {
    int i, j, n;
    // Save original class path string
    this.pathstr = pathstr;

        if (pathstr.length() == 0) {
            this.path = new ClassPathEntry[0];
        }

    // Count the number of path separators
    i = n = 0;
    while ((i = pathstr.indexOf(dirSeparator, i)) != -1) {
        n++; i++;
    }
    // Build the class path
    ClassPathEntry[] path = new ClassPathEntry[n+1];
    int len = pathstr.length();
    for (i = n = 0; i < len; i = j + 1) {
        if ((j = pathstr.indexOf(dirSeparator, i)) == -1) {
        j = len;
        }
        if (i == j) {
        path[n] = new ClassPathEntry();
        path[n++].dir = new File(".");
        } else {
        File file = new File(pathstr.substring(i, j));
        if (file.isFile()) {
            try {
            ZipFile zip = new ZipFile(file);
            path[n] = new ClassPathEntry();
            path[n++].zip = zip;
            } catch (ZipException e) {
            } catch (IOException e) {
            // Ignore exceptions, at least for now...
            }
        } else {
            path[n] = new ClassPathEntry();
            path[n++].dir = file;
        }
        }
    }
    // Trim class path to exact size
    this.path = new ClassPathEntry[n];
    System.arraycopy((Object)path, 0, (Object)this.path, 0, n);
    }

    /**
     * Find the specified directory in the class path
     */
    public ClassFile getDirectory(String name) {
    return getFile(name, true);
    }

    /**
     * Load the specified file from the class path
     */
    public ClassFile getFile(String name) {
    return getFile(name, false);
    }

    private final String fileSeparatorChar = "" + File.separatorChar;

    private ClassFile getFile(String name, boolean isDirectory) {
    String subdir = name;
    String basename = "";
    if (!isDirectory) {
        int i = name.lastIndexOf(File.separatorChar);
        subdir = name.substring(0, i + 1);
        basename = name.substring(i + 1);
    } else if (!subdir.equals("")
           && !subdir.endsWith(fileSeparatorChar)) {
        // zip files are picky about "foo" vs. "foo/".
        // also, the getFiles caches are keyed with a trailing /
        subdir = subdir + File.separatorChar;
        name = subdir;	// Note: isDirectory==true & basename==""
    }
    for (int i = 0; i < path.length; i++) {
        if (path[i].zip != null) {
        String newname = name.replace(File.separatorChar, '/');
        ZipEntry entry = path[i].zip.getEntry(newname);
        if (entry != null) {
            return new ClassFile(path[i].zip, entry);
        }
        } else {
        File file = new File(path[i].dir.getPath(), name);
        String list[] = path[i].getFiles(subdir);
        if (isDirectory) {
            if (list.length > 0) {
            return new ClassFile(file);
            }
        } else {
            for (int j = 0; j < list.length; j++) {
            if (basename.equals(list[j])) {
                // Don't bother checking !file.isDir,
                // since we only look for names which
                // cannot already be packages (foo.java, etc).
                return new ClassFile(file);
            }
            }
        }
        }
    }
    return null;
    }


    /**
     * Returns list of files given a package name and extension.
     */
    public Enumeration getFiles(String pkg, String ext) {
        Hashtable files = new Hashtable();

        /*
         * Classpath can specify multiple directories, or JAR's, this will search
         * each of them in turn.
         */
        for (int i = path.length; --i >= 0; ) {
            if (path[i].zip != null) {
                Enumeration e = path[i].zip.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String name = entry.getName();
                    name = name.replace('/', File.separatorChar);
                    if (name.startsWith(pkg) && name.endsWith(ext)) {
                        files.put(name, new ClassFile(path[i].zip, entry));
                    }
                }
            } else {
                //String[] list = path[i].getFiles(pkg);
                File[] list = path[i].getAllFiles();
                for (int j = 0; j < list.length; j++) {

                    if(list[j] == null) {
                        continue;
                    }
                    String name = list[j].getPath();
                    //System.out.println(name);
                    if (name.endsWith(ext)) {
                        //name = pkg + File.separatorChar + name;
                        //File file = new File(path[i].dir.getPath(), name);
                        files.put(name, new ClassFile(list[j]));
                    }
                }
            }
        }
        return files.elements();
    }

    /**
     * Release resources.
     */
    public void close() throws IOException {
    for (int i = path.length; --i >= 0; ) {
        if (path[i].zip != null) {
        path[i].zip.close();
        }
    }
    }

    /**
     * Returns original class path string
     */
    public String toString() {
    return pathstr;
    }
}




/**
 * A class path entry, which can either be a directory or an open zip file.
 */
class ClassPathEntry {
    File dir;
    ZipFile zip;

    Hashtable subdirs = new Hashtable(29); // cache of sub-directory listings

    File[] getAllFiles() {

        /* The classpath entry cannot be a file */
        if(dir == null || !dir.isDirectory())  {
            System.out.println("null dir?");
            return new File[0];
        }

        /* Start the search from the base directory of the classpath */
        Vector v = getAllFiles(dir);

        File[] f = new File[v.size()];

        for(int i = 0; i < v.size(); i++) {
            f[i] = (File)v.get(i);
        }
        return f;
    }

    Vector getAllFiles(File baseDir) {
        Vector v = new Vector();

        if(!baseDir.isDirectory()) {
            System.out.println("Trying to load basedir that is file?!");
            return v;
        }

        String[] subordinates = baseDir.list();

        for(int i = 0; i < subordinates.length; i++) {
            File f = new File(baseDir, subordinates[i]);
            if(f.isFile()) {
                v.add(f);
            } else {
                v.addAll(getAllFiles(f));
            }
        }

        return v;
    }

    String[] getFiles(String subdir) {
        String files[] = (String[]) subdirs.get(subdir);
        if (files == null) {
            // search the directory, exactly once
            File sd = new File(dir.getPath(), subdir);
            if (sd.isDirectory()) {
                files = sd.list();
                if (files == null) {
                    // should not happen, but just in case, fail silently
                    files = new String[0];
                }
                if (files.length == 0) {
                    String nonEmpty[] = {""};
                    files = nonEmpty;
                }
            } else {
                files = new String[0];
            }
            subdirs.put(subdir, files);
        }
        return files;
    }

}
