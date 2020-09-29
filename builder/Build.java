/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * This is the launcher for building parts (or all) of the Squawk VM. It is also
 * used to start the translator and the VM once it has been built.
 */
public class Build {

    Hashtable exclusions = new Hashtable();

    /*---------------------------------------------------------------------------*\
     *                           Java compiler interface                         *
    \*---------------------------------------------------------------------------*/

    public int javaCompile(Build env, String args, boolean verbose) throws Exception {
        args = fix(args);
        String tempName = "javacinput.tmp";
        delete(tempName);
        PrintStream out = new PrintStream(new FileOutputStream(tempName));
        out.println(args);
        out.close();
        int res = env.exec(os.getJDKTool("javac")+" @"+tempName);
        if (!verbose) {
            delete(tempName);
        }
        return res;
    }

    /*---------------------------------------------------------------------------*\
     *                         C compiler interface                              *
    \*---------------------------------------------------------------------------*/

    boolean buildDLL;
    CCompiler ccompiler;
    CCompiler.Options cOptions;

    /**
     * This class is the abstract interface to the C compiler used to build the slow VM.
     */
    public static abstract class CCompiler {

        /**
         * The options that parameterise the compilation.
         */
        static class Options {
            boolean production  = false;
            boolean tracing     = false;
            boolean profiling   = false;
            boolean o1          = false;
            boolean o2          = false;
            boolean assume      = false;
            boolean typemap     = false;
            boolean maxinline   = false;
            boolean is64        = false;
            boolean disabled    = false;
            boolean macroize    = false;
            String  cflags      = "";
            String  lflags      = "";
        }

        /**
         * Identifies the C compiler.
         */
        private final String  name;

        /**
         * Creates CCompiler instance.
         *
         * @param name  the compiler's identifier
         */
        protected CCompiler(String name) {
            this.name = name;
        }

        /**
         * Gets the identifier for this compiler.
         *
         * @return the identifier for this compiler
         */
        final String getName() {
            return name;
        }

        /**
         * Compiles a given source file, placing the created executable file in the current working directory.
         *
         * @param env          the builder used to execute the C compiler
         * @param includeDirs  the directories to search for header files (a space separated list of directories)
         * @param source       path to the source file name
         * @param out          prefix of output file(s)
         * @return the name of the created executable or dll file
         */
        public abstract String compile(Build env, String includeDirs, String source, String outPrefix) throws Exception ;

        /**
         * Creates a string of compiler switches based on a given set of abstract options.
         *
         * @param options   the options specified on the builder's command line
         * @return the compiler switches corresponding to <code>options</code>
         */
        public abstract String cflags(Options options);

        /**
         * Gets the compiler switch used to prefix include directories.
         *
         * @return the compiler switch used to prefix include directories
         */
        public abstract String getIncludeSwitch();

        /**
         * Formats a string of space separated include directories into a string
         * compiler include comand line switches.
         *
         * @param includeDirs  a string of space separated include directories
         * @return the given include dirs formatted as a compiler switch
         */
        public final String formatIncludes(String includeDirs) {
            StringTokenizer st = new StringTokenizer(includeDirs, " ");
            StringBuffer buf = new StringBuffer(includeDirs.length());
            while (st.hasMoreTokens()) {
                buf.append(getIncludeSwitch()).
                    append(st.nextToken()).
                    append(' ');
            }
            return buf.toString();
        }

        /**
         * Gets the name of the Squawk dymanic compiler.
         *
         * @param options the options specified on the builder's command line
         * @return        the name of the compiler
         */
        abstract String getSquawkCompilerName(Options options);
    }

    /**
     * The interface for the "cl" MS Visual C++ compiler.
     */
    public static class MscCompiler extends CCompiler {
        MscCompiler() {
            super("msc");
        }

        /**
         * {@inheritDoc}
         */
        public String cflags(Options options) {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                 { buf.append("/O1 ");              }
            if (options.o2)                 { buf.append("/O2 /Ogityb2 /Gs "); }
            if (!options.o1 && !options.o2) { buf.append("/ZI ");              }
            if (options.tracing)            { buf.append("/DTRACE ");          }
            if (options.profiling)          { buf.append("/DPROFILING /MT ");  }
            if (options.macroize)           { buf.append("/DMACROIZE ");       }
            if (options.assume)             { buf.append("/DASSUME ");         }
            if (options.typemap)            { buf.append("/DTYPEMAP ");        }
            if (options.maxinline)          { buf.append("/DMAXINLINE ");      }

            if (options.is64) {
                System.err.println("Warning: -64 option ignored by MscCompiler");
            }

            buf.append("/DIOPORT ");
            return buf.append(options.cflags).append(' ').toString();
        }

        /**
         * {@inheritDoc}
         */
        public String compile(Build env, String includeDirs, String source, String outPrefix) throws Exception {
            StreamGobbler.Filter filter = new StreamGobbler.Filter() {
                public boolean match(String line) {
                    return line.indexOf("wd4996") == -1 && line.indexOf("deprecated") == -1;
                }
            };
            String output;
            try {
                env.execOutputFilter = filter;
                if (env.buildDLL) {
                    output = System.mapLibraryName(outPrefix);
                    env.exec("cl",
                             "/nologo /wd4996 " + cflags(env.cOptions) + " " +
                             formatIncludes(includeDirs) +
                             " /Fe" + output + " /LD " + source +
                             " /link wsock32.lib /IGNORE:4089");
                } else {
                    output = outPrefix + env.os.getExecutableExtension();
                    env.exec("cl",
                             "/nologo /wd4996 " + cflags(env.cOptions) + " " +
                             formatIncludes(includeDirs) +
                             " /Fe" + output + " " + source +
                             " /link wsock32.lib /IGNORE:4089");
                }
            } finally {
                env.execOutputFilter = null;
            }
            return output;
        }

        /**
         * {@inheritDoc}
         */
        public String getIncludeSwitch(){
            return "/I";
        }

        /**
         * {@inheritDoc}
         */
        String getSquawkCompilerName(Options options) {
            return "X86";
        }
    }

    /**
     * Abstracts over the "gcc" and "cc" compilers.
     */
    public abstract static class UnixCompiler extends CCompiler {

        UnixCompiler(String name) {
            super(name);
        }

        /**
         * {@inheritDoc}
         */
        public String getIncludeSwitch(){
            return "-I";
        }
    }


    /**
     * The interface for the "gcc" compiler.
     */
    public static class GccCompiler extends UnixCompiler {
        GccCompiler() {
            super("gcc");
        }

        GccCompiler(String name) {
            super(name);
        }

        /**
         * {@inheritDoc}
         */
        public String cflags(Options options) {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                 { buf.append("-O1 ");               }
            if (options.o2)                 { buf.append("-O2 ");               }
            if (!options.o1 && !options.o2) { buf.append("-g ");                }
            if (options.tracing)            { buf.append("-DTRACE ");           }
            if (options.profiling)          { buf.append("-DPROFILING ");       }
            if (options.macroize)           { buf.append("-DMACROIZE ");        }
            if (options.assume)             { buf.append("-DASSUME ");          }
            if (options.typemap)            { buf.append("-DTYPEMAP ");         }
            if (options.maxinline)          { buf.append("-DMAXINLINE -O3 ");   }

            if (options.is64) {
                buf.append("-DSQUAWK_64=true ").append("-m64 ");
            } else {
                buf.append("-DSQUAWK_64=false ").append("-m32 ");
            }
            buf.append("-DIOPORT ");
            return buf.append(options.cflags).append(' ').toString();
        }

        /**
         * Gets the compilation options that must come after the source file.
         *
         * @return the compilation options that must come after the source file
         */
        public String getCompileSuffix(Build env) {
            return " -ldl -lm";
        }

        /**
         * Gets the platform-dependant gcc switch used to produce a shared library.
         *
         * @return the platform-dependant gcc switch used to produce a shared library
         */
        public String getSharedLibrarySwitch() {
            return "-shared";
        }

        /**
         * {@inheritDoc}
         */
        public String compile(Build env, String includeDirs, String source, String outPrefix) throws Exception {
            String output;
            if (env.buildDLL) {
                output = System.mapLibraryName(outPrefix);
                env.exec("gcc",
                         cflags(env.cOptions) + " " + formatIncludes(includeDirs) +
                         " " + getSharedLibrarySwitch() + " -o " + output + " " +
                         source + getCompileSuffix(env));
            } else {
                output = outPrefix + env.os.getExecutableExtension();
                env.exec("gcc",
                         cflags(env.cOptions) + " " + formatIncludes(includeDirs) +
                         " -o " + output + " " + source + getCompileSuffix(env));
            }
            return output;
        }

        /**
         * {@inheritDoc}
         */
        String getSquawkCompilerName(Options options) {
            return "X86";
        }
    }


    /**
     * The interface for the "gcc-macosx" compiler.
     */
    public static class GccMacOSXCompiler extends GccCompiler {
        GccMacOSXCompiler() {
            super("gcc-macosx");
        }

        /**
         * {@inheritDoc}
         */
        public String cflags(Options options) {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                 { buf.append("-Os ");               }
            if (options.o2)                 { buf.append("-O2 ");               }
            if (!options.o1 && !options.o2) { buf.append("-g ");                }
            if (options.tracing)            { buf.append("-DTRACE ");           }
            if (options.profiling)          { buf.append("-DPROFILING ");       }
            if (options.macroize)           { buf.append("-DMACROIZE ");        }
            if (options.assume)             { buf.append("-DASSUME ");          }
            if (options.typemap)            { buf.append("-DTYPEMAP ");         }
            if (options.maxinline)          { buf.append("-DMAXINLINE -O3 ");   }
            buf.append("-DIOPORT ");
            return buf.append(options.cflags).append(' ').toString();
        }

        /**
         * {@inheritDoc}
         */
        public String getCompileSuffix(Build env) {
            File dir = new File(env.os.getJDKHome());
            while (dir != null && !dir.getName().equals("JavaVM.framework")) {
                dir = dir.getParentFile();
            }

            if (dir == null) {
                throw new RuntimeException("can't find JavaVM.framework directory starting from " + env.os.getJDKHome());
            }

            String framework;
            File usualLocation = new File(dir, "JavaVM");
            if (usualLocation.exists()) {
                framework = usualLocation.getPath();
            } else {
                framework = env.findOne(dir.getPath(), "JavaVM", (String)null);
            }
            if (framework == null) {
                throw new RuntimeException("can't find 'JavaVM' under " + dir.getPath());
            }
            return " " + framework;
        }

        /**
         * {@inheritDoc}
         */
        public String getSharedLibrarySwitch() {
            return "-dynamiclib -single_module";
        }

        /**
         * {@inheritDoc}
         */
        String getSquawkCompilerName(Options options) {
            return "MAC";
        }
    }


    /**
     * The interface for the "cc" compiler.
     */
    public static class CcCompiler extends UnixCompiler {
        CcCompiler() {
            super("cc");
        }

        /**
         * {@inheritDoc}
         */
        public String cflags(Options options) {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                 { buf.append("-xO2 -xspace ");      }
            if (options.o2)                 { buf.append("-xO2 ");              }
            if (!options.o1 && !options.o2) { buf.append("-xsb -g ");           }
            if (options.tracing)            { buf.append("-DTRACE ");           }
            if (options.profiling)          { buf.append("-DPROFILING ");       }
            if (options.macroize)           { buf.append("-DMACROIZE ");        }
            if (options.assume)             { buf.append("-DASSUME ");          }
            if (options.typemap)            { buf.append("-DTYPEMAP ");         }
            if (options.maxinline)          { buf.append("-DMAXINLINE -xO5 ");  }
            if (options.is64)               { buf.append("-DSQUAWK_64=true ").append("-xarch=v9 "); }
            buf.append("-DIOPORT ");
            return buf.append(options.cflags).append(' ').toString();
        }

        /**
         * {@inheritDoc}
         */
        public String compile(Build env, String includeDirs, String source, String outPrefix) throws Exception {
            String output;
            if (env.buildDLL) {
                output = System.mapLibraryName(outPrefix);
                env.exec("cc",
                         cflags(env.cOptions) + " " + formatIncludes(includeDirs) +
                         " -G -lthread -o " + output + " " + source +
                         " -ldl -lm -lsocket -lnsl ");
            } else {
                output = outPrefix + env.os.getExecutableExtension();
                env.exec("cc",
                         cflags(env.cOptions) + " " + formatIncludes(includeDirs) +
                         " -lthread -o " + output + " " + source +
                         " -ldl -lm -lsocket -lnsl ");
            }
            // create shared library
            return output;
        }

        /**
         * {@inheritDoc}
         */
        String getSquawkCompilerName(Options options) {
            return (options.is64) ? "SV9_64" : "SV8";
        }
    }


    /*---------------------------------------------------------------------------*\
     *                               OS classes                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Factory method to create an OS instance based in the name of an OS.
     *
     * @param osName  the name of the OS
     * @return OS     the OS instance
     */
    public static OS createOS(String osName) {
        if (osName.startsWith("windows")) {
            return new Build.WindowsOS();
        } else if (osName.startsWith("sun")) {
            return new Build.SunOS();
        } else if (osName.startsWith("lin")) {
            return new Build.LinuxOS();
        } else if (osName.startsWith("mac os x")) {
            return new Build.MacOSX();
        } else {
            return null;
        }
    }

    /**
     * This class abstracts the properties of the underlying operating system that are
     * required for C compilation.
     */
    public static abstract class OS {

        /**
         * The prefix of the relative path to the preverifier executable.
         */
        static final String  PREVERIFIER_PREFIX = "tools/preverify";

        /**
         * Gets the directories in which the JVM dynamic libraries are located that are
         * required for starting an embedded JVM via the Invocation API.
         *
         * @return  one or more library paths separated by ':'
         */
        public abstract String getJVMLibraryPath();

        /**
         * Gets the directory where the machine dependent "jni_md.h" file can be found.
         *
         * @return String
         */
        public final String getJNI_MDIncludePath() {
            String path = findOne(getJDKHome(), "jni_md.h", (String)null);
            if (path == null) {
                throw new RuntimeException("could not find 'jni_md.h' under " + getJDKHome());
            }
            return new File(path).getParent();
        }

        /**
         * Gets the extension used for executable files in this OS.
         *
         * @return the extension used for executable files in this OS
         */
        public abstract String getExecutableExtension();

        /**
         * Gets the relative path to the preverifier executable.
         *
         * @return the relative path to the preverifier executable
         */
        public abstract String getPreverifierPath();

        /**
         * Gets a string that describes the concrete OS.
         *
         * @return a string that describes the concrete OS
         */
        public final String toString() {
            return System.getProperty("os.name");
        }

        /**
         * Displays a message describing what environment variables need to be set so that the
         * embedded JVM can be started properly by the squawk executable.
         *
         * @param out PrintStream  the stream on which to print the message
         */
        public abstract void showJNIEnvironmentMessage(PrintStream out);

        /**
         * Gets the path to the JDK installation directory.
         *
         * @return the path to the JDK installation directory
         */
        public final String getJDKHome() {
            String jdkHome = System.getProperty("java.home");
            if (jdkHome.endsWith("jre")) {
                jdkHome = jdkHome.substring(0, jdkHome.length()-4);
            }
            return jdkHome;
        }

        /**
         * Gets the relative path to a JDK tool executable.
         *
         * @param program   the name of the JDK tool executable without the platform specific executable extension
         * @return the relative path to the JDK tool executable denoted by <code>program</code>
         */
        public final String getJDKTool(String program)      {
            String sep = File.separator;
            String path = getJDKHome()+sep+"bin"+sep+program+getExecutableExtension();
            if (!new File(path).isFile()) {
                PrintStream out = System.out;
                out.println();
                out.println("Could not find the '"+program+"' executable.");
                out.println();
                out.println("This is most likely due to having run the builder with a");
                out.println("JRE java executable instead of a JDK java executable.");
                out.println("To fix this, make sure that the 'bin' directory of the");
                out.println("JDK (e.g. \"C:\\j2sdk\\bin\") preceeds the directory");
                out.println("containing the JRE java executable (e.g. \"C:\\WINNT\\SYSTEM32\")");
                out.println("or specify the absolute path to the java executable when");
                out.println("running the builder (e.g. \"C:\\j2sdk\\bin\\java.exe -jar build.jar ...\")");
                out.println();
                System.exit(1);
            }
            return path;
        }

    }

    /**
     * This class represents the Windows OS.
     */
    public static final class WindowsOS extends OS {

        /**
         * {@inheritDoc}
         */
        public String getExecutableExtension() {
            return ".exe";
        }

        /**
         * {@inheritDoc}
         */
        public String getPreverifierPath() {
            return fix(PREVERIFIER_PREFIX+".exe");
        }

        /**
         * {@inheritDoc}
         */
        public String getJVMLibraryPath() {
            return findOne(getJDKHome(), "jvm.dll", new String[] {"hotspot", "client", ""});
        }

        /**
         * {@inheritDoc}
         */
        public void showJNIEnvironmentMessage(PrintStream out) {
            String env = getJVMLibraryPath();
            if (env != null) {
                out.println();
                out.println("To configure the environment for Squawk, try the following command:");
                out.println();
                out.println("    set JVMDLL="+env);
                out.println();
            } else {
                out.println();
                out.println("The JVMDLL environment variable must be set to the full path of 'jvm.dll'.");
                out.println();
            }
        }
    }

    public static abstract class UnixOS extends OS {

        /**
         * {@inheritDoc}
         */
        public String   getExecutableExtension() {
            return "";
        }

        /**
         * {@inheritDoc}
         */
        public String getJVMLibraryPath() {
            String javaHome = getJDKHome();
            String jvm = findOne(javaHome, "libjvm.so", new String[] {"hotspot", "client", ""});
            String verifier = findOne(javaHome, "libverify.so", "");
            if (jvm != null && verifier != null) {
                jvm = (new File(jvm)).getParent();
                verifier = (new File(verifier)).getParent();
                return jvm + ":" + verifier;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public void showJNIEnvironmentMessage(PrintStream out) {
            String env = getJVMLibraryPath();
            if (env != null) {
                out.println();
                out.println("To configure the environment for Squawk, try the following command under bash:");
                out.println();
                out.println("    export LD_LIBRARY_PATH=" + env + ":$LD_LIBRARY_PATH");
                out.println();
                out.println("or in csh/tcsh");
                out.println();
                out.println("    setenv LD_LIBRARY_PATH " + env + ":$LD_LIBRARY_PATH");
                out.println();
            } else {
                out.println();
                out.println("The LD_LIBRARY_PATH environment variable must be set to include the directories");
                out.println("containing 'libjvm.so' and 'libverify.so'.");
                out.println();
            }
        }
    }

    public static class LinuxOS extends UnixOS   {
        public String   getPreverifierPath() {
            return fix(PREVERIFIER_PREFIX+".linux");
        }
    }

    public static class MacOSX extends UnixOS {
        public String   getPreverifierPath() {
            return fix(PREVERIFIER_PREFIX+".macosx");
        }

        /**
         * {@inheritDoc}
         */
        public void showJNIEnvironmentMessage(PrintStream out) {
            out.println();
            out.println("There is no need to configure the environment for Squawk on Mac OS X as the location of the");
            out.println("JavaVM framework is built into the executable.");
            out.println();
        }
    }

    public static class SunOS extends UnixOS {
        public String   getPreverifierPath() {
            return fix(PREVERIFIER_PREFIX+".solaris");
        }
    }

    private OS os;

    /*---------------------------------------------------------------------------*\
     *                              Command classes                              *
    \*---------------------------------------------------------------------------*/

    /**
     * A Command instance describes a builder command.
     */
    abstract class Command {

        private final String name;
        private final String name2;
        public final String name()  { return name; }
        public final String name2() { return name2; }

        protected Command(String name) {
            this.name = name;
            this.name2 = "";
        }

        protected Command(String name, String name2) {
            this.name  = name;
            this.name2 = name2;
        }

        /**
         * @param cmd
         * @param args
         * @return
         */
        public abstract int run(String cmd, String[] args) throws Exception;
    }

    abstract class CompilationCommand extends Command {
        CompilationCommand(String name) {
            super(name);
        }
        CompilationCommand(String name, String name2) {
            super(name, name2);
        }
    }

    abstract class CompilationCommandIfPresent extends CompilationCommand {
        CompilationCommandIfPresent(String name) {
            super(name);
        }
    }

    private boolean compilationSupported = true;

    /**
     * The set of commands supported by the builder.
     */
    private Vector commands = new Vector(Arrays.asList(new Command[] {
        new CompilationCommand("clean") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Cleaning...");
                clean(new File("."), ".class");
                clean(new File("vm/bld"), ".c");
                clean(new File("vm/bld"), ".h");
                clean(new File("vm/bld"), ".exe");
                clean(new File("vm/bld"), ".obj");
                clean(new File("temp/src"), ".java");
                clean(new File("tck/gen"), ".java");
                return 0;
            }
        },
        new Command("traceviewer") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running traceviewer...");
                String options = join(args, 0, args.length, " ");
                return java("-Xms256M -Xmx256M -cp j2se/classes;j2me/classes", "com.sun.squawk.traces.TraceViewer", options);
            }
        },
        new Command("profileviewer") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running profileviewer...");
                String options = join(args, 0, args.length, " ");
                return java("-cp j2se/classes;j2me/classes", "com.sun.squawk.traces.ProfileViewer", options);
            }
        },
        new Command("gctf") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running GCTraceFilter...");
                String options = join(args, 0, args.length, " ");
                return java("-cp j2se/classes;j2me/classes", "com.sun.squawk.traces.GCTraceFilter", options);
            }
        },
        new Command("ht2html") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running heap trace 2 HTML converter...");
                String options = join(args, 0, args.length, " ");
                return java("-Xbootclasspath#a:j2se/classes;j2me/classes", "com.sun.squawk.ht2html.Main", options);
            }
        },
        new Command("translate") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running translator...");
                String options = join(args, 0, args.length, " ");
                return java("-Xbootclasspath#a:j2se/classes;j2me/classes;translator/classes", "com.sun.squawk.translator.main.Main", options);
            }
        },
        new Command("romizetck") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Translating and romizing TCK suites...");
                TCK tck = new CLDC_TCK1_0a();
                tck.romizetck(args, Build.this);
                return 0;
            }
        },
        new Command("runtck") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running TCK...");
                String options = join(args, 0, args.length, " ");
                return java("-Djava.library.path=. -cp j2se/classes;j2me/classes", "com.sun.squawk.vm.TCK", options);
            }
        },
        new Command("verify") {
            void usage(String errMsg, PrintStream out) {
                if (errMsg != null) {
                    out.println(errMsg);
                }
                out.println("usage: verify [options] [class | @file]*");
                out.println("where options include:");
                out.println("    -cp <path>          the classpath where the classes can be found");
                out.println("    -v                  verbose execution");
                out.println("    -h                  show this help message and exit");
                out.println("");
                out.println("The @file arg specifies a file containing a list of");
                out.println("class names, one per line");
            }
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running translator as verifier...");
                String options = "-v ";
                if (args.length == 0) {
                    usage(null, stdout);
                    return 0;
                }
                for (int i = 0; i != args.length; ++i) {
                    String arg = args[i];
                    if (arg.equals("-cp")) {
                        options += "-cp " + args[++i];
                    } else if (arg.startsWith("-v")) {
                        options += " -traceloading";
                    } else if (arg.startsWith("-h")) {
                        usage(null, stdout);
                        return 0;
                    } else {
                        options += " " + arg;
                    }
                }
                return java("-Xbootclasspath#a:j2se/classes;j2me/classes;translator/classes", "com.sun.squawk.translator.main.Main", options);
            }
        },
        new Command("listtck") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Listing TCK ...");
                String options = join(args, 0, args.length, " ");
                return java("-cp tck/classes", "CLDC_TCK", options);
            }
        },
        new Command("runbco") {
            public int run(String cmd, String[] args) throws Exception {
                String options = join(args, 0, args.length, " ");
                return java("-cp bco/classes", "com.sun.javawand.tools.opt.Optimizer", options);
            }
        },
        new Command("jvmenv") {
            public int run(String cmd, String[] args) throws Exception {
                os.showJNIEnvironmentMessage(stdout);
                return 0;
            }
        },
        new Command("romize") { // Simple romize option
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running romizer...");
                String options = join(args, 0, args.length, " ");
                return java("-Xmx300M -Xbootclasspath#a:j2se/classes;romizer/classes;j2me/classes;translator/classes", "java.lang.Romizer", options);
            }
        },
        new Command("rom") { // Clever romize option
            /**
             * Construct the arguments for the "rom" command.
             *
             * @param   args   the command arguments
             * @return  the folded string
             */
            private String extractRomizerOptions(String[] args) {
                StringBuffer buf = new StringBuffer(1000);
                String extraCP = "";

                int i = 0;
                while (i != args.length) {
                    String arg = args[i];
                    if (arg.charAt(0) == '-') {
                        if (arg.startsWith("-cp:")) {
                            extraCP = arg.substring("-cp:".length());
                        } else {
                            buf.append(arg).append(' ');
                        }
                    } else {
                        break;
                    }
                    i++;
                }

                String dirs = "";
                String cp = "";
                while (i != args.length) {
                    String arg = args[i];
                    if (new File(arg).isDirectory()) {
                        arg += "/j2meclasses";
                    }
                    if (new File(arg).exists()) {
                        cp += arg + ' ';
                    }
                    dirs += arg + ' ';
                    i++;
                }

                cp += extraCP;
                cp = "-cp:" + cp.trim().replace(' ', File.pathSeparatorChar);

                return buf.toString() + cp + ' ' + dirs;
            }

            void createBuildFlagsHeaderFile(String buildFlags) throws IOException {
                FileOutputStream fos = new FileOutputStream("slowvm/src/vm/buildflags.h");
                PrintStream out = new PrintStream(fos);
                out.println("#define BUILD_FLAGS \"" + buildFlags + '"');
                fos.close();
            }

            /**
             * Preprocesses a Squawk preprocessor input file (i.e. a file with a ".spp" suffix). The file is processed with
             * the {@link #preprocessSource() Java source preprocessor} and then with the {@link Macro} preprocessor.
             * The file generated after the preprocessing is the input file with the ".spp" suffix removed.
             *
             * @param inputFile   the input source file
             */
            void preprocess(String inputFile) throws Exception {
                if (!inputFile.endsWith(".spp")) {
                    throw new IllegalArgumentException("input file must end with \".spp\"");
                }
                String outputFile = inputFile.substring(0, inputFile.length() - ".spp".length());
                String tempFile = inputFile + ".temp";

                // Remove old version of generated file
                new File(outputFile).delete();

                preprocessSource(inputFile, true, tempFile, true, false);
                Macro.convert(tempFile, outputFile, Build.this.cOptions.macroize);

                // Make the generated file read-only
                new File(outputFile).setReadOnly();
            }

            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running romizer...");

                String outPrefix = "squawk";

                // Only run the romizer if there is one or more directory of classes passed to the 'rom' command
                if (args.length > 0) {
                    String options = "-arch:" + ccompiler.getSquawkCompilerName(Build.this.cOptions) + " " + extractRomizerOptions(args);
                    int res = java("-Xmx300M -Xbootclasspath#a:j2se/classes;romizer/classes;j2me/classes;translator/classes", "java.lang.Romizer", options);
                    if (res != 0) {
                        return res;
                    }

                    // Modify the prefix of the compiler output files if necessary
                    int index = options.indexOf("-o:");
                    if (index != -1) {
                        int end = options.indexOf(' ', index);
                        if (end == -1) {
                            end = options.length();
                        }
                        outPrefix = options.substring(index + "-o:".length(), end);
                    }
                }

                // Run the C compiler to compile the slow VM
                if (!Build.this.cOptions.disabled) {
                    createBuildFlagsHeaderFile(ccompiler.cflags(cOptions).trim());

                    // Preprocess any files with the ".spp" suffix
                    String sppFiles = find(fix("slowvm/src/vm"), ".spp");
                    if (sppFiles != null) {
                        StringTokenizer st = new StringTokenizer(sppFiles);
                        while (st.hasMoreTokens()) {
                            String file = st.nextToken();
                            preprocess(file);
                        }
                    }

                    String source = "slowvm/src/vm/squawk.c";
                    stdout.println("Compiling " + fix(source) + " ...");
                    ccompiler.compile(Build.this, os.getJDKHome()+ "/include " + os.getJNI_MDIncludePath() + " slowvm/src/rts/" + ccompiler.getName(), source, outPrefix);
                }
                return 0;
            }
        },
        new Command("squawk") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running squawk...");
                String options = join(args, 0, args.length, " ");
                return java("-Djava.library.path=. -cp j2se/classes;j2me/classes", "com.sun.squawk.vm.Main", options);
            }
        },
        new Command("map", "cmap") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running mapper...");
                String options = join(args, 0, args.length, " ");
                String mainClassName = (cmd.equals("map") ? "Mapper" : "ClasspathMapper");
                return java("-Xms256M -Xmx256M  -Xbootclasspath#a:j2se/classes;mapper/classes;j2me/classes;translator/classes", "java.lang."+mainClassName, options);
            }
        },
        new Command("romizebadtck") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Romizing bad tck test...");
                String test = args[0];
                String filter = test;
                if (args.length > 1) {
                    filter += args[1];
                }
                String options = "-cp j2me/classes:tck/tck.jar -tracefilter "+filter+" -traceverifier -traceloading tck "+test;
                return java("-Xbootclasspath#a:j2se/classes;romizer/classes;j2me/classes;translator/classes", "java.lang.Romizer", options);
            }
        },
        new Command("systemproperties") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Displaying system properties...");
                Properties properties = System.getProperties();
                SortedMap map = new TreeMap();
                map.putAll(properties);
                for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry)iterator.next();
                    System.out.println(entry.getKey()+"="+entry.getValue());
                }
                return 0;
            }
        },
        new Command("bco") { // new CompilationCommandIfPresent("bco") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building bco...");
                return javac_j2se("bco/classes", "bco", find("bco/src", ".java"));
            }
        },
        new CompilationCommand("j2me") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building j2me...");
                return javac_j2me(null, "j2me", defines + find("j2me/src", ".java"));
            }
        },
        new CompilationCommand("j2meTests") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building j2me tests...");
                return javac_j2me("j2me/classes", "j2meTests", defines + find("j2me/src/com/sun/squawk/compiler/tests", ".java"));
            }
        }
        ,
        new CompilationCommand("translator") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building translator...");
                return javac_j2me("j2me/classes;", "translator", find("translator/src", ".java"));
            }
        },
        new CompilationCommand("j2se") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building j2se...");
                StreamGobbler.Filter filter = new StreamGobbler.Filter() {
                    public boolean match(String line) {
                        return line.indexOf("deprecat") == -1;
                    }
                };
                int res = 0;
                try {
                    execOutputFilter = filter;
                    res = javac_j2se("j2me/classes;translator/classes;j2se/bcel-5.1.jar", "j2se", find("j2se/src", ".java"));
                } finally {
                    execOutputFilter = null;
                }
                if (res != 0) {
                    return res;
                }
                return exec("jar cf squawk.jar @j2se/jarlist/jarlist");
            }
        },
        new CompilationCommand("romizer") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building romizer...");
                String classes = find("romizer/src", ".java");
                return javac_j2se("j2me/classes;translator/classes;j2se/classes", "romizer", classes);
            }
        },
        new CompilationCommandIfPresent("jasmin") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building jasmin...");
                return jasmin_j2me("j2me/classes", "jasmin", find("jasmin/src", ".j"));
            }
        },
        new CompilationCommandIfPresent("graphics") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building graphics...");
                return javac_j2me("j2me/classes;", "graphics", find("graphics/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("samples") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building samples...");
                return javac_j2me("j2me/classes;graphics/classes;", "samples", find("samples/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("prototypecompiler") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building prototypecompiler...");
                return javac_j2me("j2me/classes", "prototypecompiler", find("prototypecompiler/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("compiler") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building compiler...");
                return javac_j2me("j2me/classes", "compiler", find("compiler/src", ".java"));
            }
        },
        new CompilationCommand("vmgen") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building vmgen...");
                return javac_j2me("j2me/classes;prototypecompiler/classes;", "vmgen", find("vmgen/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("mapper") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building mapper...");
                String classes = find("mapper/src", ".java");
                int res = javac_j2se("j2me/classes;translator/j2meclasses", "mapper", classes);
                return res;

            }
        },
        new Command("listclass") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running listclass ...");
                String options = join(args, 0, args.length, " ");
                return java("-cp j2se/classes;j2me/classes;j2se/bcel-5.1.jar", "com.sun.squawk.listclass.Main", options);
            }
        },
        new Command("hexdump") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running hexdump ...");
                String options = join(args, 0, args.length, " ");
                return java("-cp j2se/classes", "com.sun.squawk.util.HexDump", options);
            }
        },
        new CompilationCommandIfPresent("tck") {
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Building tck...");
                return javac_j2me("j2me/j2meclasses", "tck", find("tck/src", ".java"));
            }
        },
        new Command("protocomp_test") { // compiler test: run only 1 test
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running prototype compiler test ");
                String testName = "com.sun.squawk.compiler.tests." + args[0];
                stdout.println (testName);
                return java("-cp prototypecompiler/classes;j2se/classes;j2me/classes", testName, "");
            }
        },
        new Command("comp_test") {  // compiler test: run only 1 test
            public int run(String cmd, String[] args) throws Exception {
                stdout.println("Running compiler test ");
                String testName = "com.sun.squawk.compiler.tests." + args[0];
                stdout.println (testName);
                return java("-cp compiler/classes;j2se/classes;j2me/classes", testName, "");
            }
        }

    }));


    /*---------------------------------------------------------------------------*\
     *                                  Constants                                *
    \*---------------------------------------------------------------------------*/


    private final static boolean NESTEDSECTIONS = true;


    final String defines;

    boolean ignoreErrors = false;
    boolean verbose  = false;
    boolean diagnostics  = false;
    boolean runDocCheck = false;
    boolean runJavadoc = false;
    boolean runJavadocAPI = false;
    boolean useTimer = false;
    boolean clearJppLines = false;
    boolean wobulateClasses = false;
    String  buildPropsFile = "build.properties";


    /*---------------------------------------------------------------------------*\
     *                          Standard output streams                          *
    \*---------------------------------------------------------------------------*/

    private PrintStream stdout;
    private PrintStream stderr;

    public PrintStream setOut(PrintStream ps) {
        stdout = ps;
        ps = System.out;
        System.setOut(stdout);
        return ps;
    }

    public PrintStream setErr(PrintStream ps) {
        stderr = ps;
        ps = System.err;
        System.setErr(stderr);
        return ps;
    }

    public PrintStream stdout() { return stdout; }
    public PrintStream stderr() { return stderr; }


    /*---------------------------------------------------------------------------*\
     *                          General file utilities                           *
    \*---------------------------------------------------------------------------*/

    /**
     * Ensures a specified directory exists, creating it if necessary.
     *
     * @param  path  the path of the directory to test
     */
    public static void ensureDirExists(String path) {
        File directory = new File(fix(path));
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new CommandFailedException("Could not create directory: "+path, 1);
            }
        } else {
            if (!directory.isDirectory()) {
                throw new CommandFailedException("Path is not a directory: " + path, 1);
            }
        }
    }

    /**
     * Find all files under a given directory.
     *
     * @param   dir     the directory to search from.
     * @param   suffix  the suffix used to filter the results or null for no filtering
     * @return  the results in a string separated by spaces or null if nothing was found
     */
    public static String find(String dir, String suffix) {
        StringBuffer sb = new StringBuffer();
        Vector vec = new Vector();
        try {
            com.sun.squawk.util.Find.find(new File(dir), suffix, vec, false);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        for (Enumeration e = vec.elements(); e.hasMoreElements();){
            String f = (String)e.nextElement();
            sb.append(" ");
            sb.append(f);

        }
        return sb.toString();
    }

    /**
     * Find all directories under a given directory.
     *
     * @param   dir     the directory to search from.
     * @param   suffix  the suffix used to filter the results or null for no filtering
     * @return  the results in a string separated by spaces or null if nothing was found
     */
    public static String findDirs(String dir, String suffix) {
        StringBuffer sb = new StringBuffer();
        Vector vec = new Vector();
        try {
            com.sun.squawk.util.Find.find(new File(dir), suffix, vec, true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (vec.isEmpty()) {
            return null;
        }
        for (Enumeration e = vec.elements(); e.hasMoreElements();){
            String f = (String)e.nextElement();
            sb.append(" ");
            sb.append(f);

        }
        return sb.toString();
    }

    /**
     * Find a single file matching a specified suffix and an optional extra filter.
     *
     * @param dir The directory
     * @param suffix
     * @param match
     * @return
     * @throws Exception
     */
    public static String findOne(String dir, String suffix, String match) {
        String results = find(dir, suffix).trim();
        if (results == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(results);
        String result = st.nextToken();
        while (st.hasMoreTokens() && match != null && result.indexOf(match) == - 1) {
            result = st.nextToken();
        }
        return result;
    }

    public static String findOne(String dir, String suffix, String[] matches) {
        String result = null;
        for (int i = 0; i != matches.length && result == null; i++) {
            result = findOne(dir, suffix, matches[i]);
        }
        return result;
    }

    /**
     * Determines if a given file exists.
     *
     * @param name
     * @return
     */
    public static boolean exists(String name) {
        return (new File(fix(name))).exists();
    }

    /**
     * Delete a file.
     *
     * @param name
     * @return
     */
    public static boolean delete(String name) {
        return delete(new File(name));
    }

    /**
     * Delete a file.
     *
     * @param name
     * @return
     */
    public static boolean delete(File file) {
        return file.delete();
    }

    public static String basename(String path, String suffix) {
        if (path.endsWith(suffix)) {
            path = path.substring(0, path.length() - suffix.length());
        }
        return new File(path).getName();
    }

    /**
     * Delete all files under a specified directory that match a specified suffix.
     *
     * @param dir
     * @param suffix
     * @return
     */
    public void clean(File dir, String suffix) throws Exception {
        File[] files = dir.listFiles();
        if (files != null) {
            for(int i = 0 ; i < files.length ; i++) {
                File f = files[i];
                if (f.isDirectory()) {
                    clean(f, suffix);
                } else {
                    if (f.getName().endsWith(suffix)) {
                        delete(f);
                    }
                }
            }
        }
    }


    /**
     * Copy a file.
     *
     * @param from
     * @param to
     * @throws IOException
     */
    public void cp(String from, String to) throws IOException {
        String toFileDir = new File(to).getParent();
        ensureDirExists(toFileDir);
        InputStream  is = new BufferedInputStream(new FileInputStream(from));
        OutputStream os = new BufferedOutputStream(new FileOutputStream(to));

        int ch = is.read();
        while (ch != -1) {
            os.write(ch);
            ch = is.read();
        }
        is.close();
        os.close();
    }

    /**
     * Executes the UNIX chmod command on a file.
     *
     * @param path   the file to chmod
     * @param mode   the mode of the chmod
     * @return true if the command succeeded, false otherwise
     */
    public boolean chmod(String path, String mode) {
        try {
            exec("chmod " + mode + " " + fix(path));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /*---------------------------------------------------------------------------*\
     *                           Command line interface                          *
    \*---------------------------------------------------------------------------*/

    public static Vector parseLines(String file, Vector v) throws IOException {
        FileReader fr = null;
        if (v == null) {
            v = new Vector();
        }
        try {
            BufferedReader br = new BufferedReader(fr = new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                v.addElement(line);
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
        return v;
    }

    /**
     * Parse a file containing command lines and add the corresponding
     * invocations to a given vector.
     * @param cmdFile
     * @param invocations
     */
    public static void parseCommandFile(String cmdFile, Vector invocations) {
        try {
            if (!cmdFile.endsWith(".bld")) {
                cmdFile = "bld/"+cmdFile+".bld";
            }
            Vector lines = parseLines(cmdFile, null);
            for (Enumeration e = lines.elements(); e.hasMoreElements();) {
                String invocation = (String)e.nextElement();
                invocation = invocation.trim();
                if (invocation.length() == 0 || invocation.charAt(0) == '#') {
                    continue;
                }

                /*
                 * If this is another command file, include its commands 'in situ'
                 */
                if (invocation.charAt(0) == '@') {
                    parseCommandFile(invocation.substring(1), invocations);
                } else {
                    StringTokenizer st = new StringTokenizer(invocation);
                    String[] args = new String[st.countTokens()];
                    for (int i = 0; i != args.length; i++) {
                        args[i] = st.nextToken();
                    }
                    invocations.addElement(args);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Vector invocations = new Vector();
        Build instance     = new Build(System.out, System.err);

        args = instance.parseArgs(args);
        instance.stdout.println("os="+instance.os);
        instance.stdout.println("java.home="+instance.os.getJDKHome());
        instance.stdout.println("java.vm.version="+System.getProperty("java.vm.version"));
        instance.stdout.println("C compiler="+instance.ccompiler.name);
        if (instance.verbose) {
            instance.stdout.println("Builder properties:");
            Enumeration keys = instance.props.keys();
            Enumeration values = instance.props.elements();
            while (keys.hasMoreElements()) {
                instance.stdout.println("    "+keys.nextElement()+'='+values.nextElement());
            }
        }

        if (args != null) {
            String cmd = args[0];
            if (cmd.charAt(0) == '@') {
                if (args.length != 1) {
                    System.err.println("Warning: args after '"+cmd+"' ignored.");
                }
                parseCommandFile(cmd.substring(1), invocations);
            } else {
                invocations.addElement(args);
            }
        }

        int exitCode = 0;
        for (Enumeration e = invocations.elements(); e.hasMoreElements();) {
            args = (String[])e.nextElement();

            /*
             * If the invocation is from a command file, it may specify its
             * own builder options which override the current ones.
             */
            if (args[0].charAt(0) == '-') {
                args = instance.parseArgs(args);
            }
            try {
                int result = instance.run(args);
                if (result != 0 && !instance.ignoreErrors) {
                    System.exit(result);
                } else {
                    exitCode += result;
                }
            } catch (CommandFailedException ex) {
                System.err.println(ex);
                if (!instance.ignoreErrors) {
                    System.exit(ex.exitVal);
                } else {
                    exitCode += ex.exitVal;
                }
            }
        }
        if (!instance.exclusions.isEmpty()) {
            System.err.print("Excluded:");
            for (Enumeration e = instance.exclusions.keys() ; e.hasMoreElements() ; ) {
                String s = (String)e.nextElement();
                System.err.print(" "+s);
            }
            System.err.println();
        }
        System.exit(exitCode);
    }

    /**
     * A wrapper to print a usage message line conditional upon whether or not
     * it relates to compilation.
     * @param isCompilationMsg
     * @param out
     * @param line
     */
    private void usageln(boolean isCompilationMsg, PrintStream out, String line) {
        if (!isCompilationMsg || compilationSupported) {
            out.println(line);
        }
    }

    /**
     * Print the usage message.
     * @param errMsg An optional error message.
     */
    public void usage (String errMsg, boolean exit) {

        PrintStream out = stderr;

        if (errMsg != null) {
            out.println(errMsg);
        }
        usageln(false, out, "Usage: build [ build-options] [ command [ command_options ] | @batch ]");
        usageln(false, out, "where build-options include:");
        usageln(false, out, "    -stdout <file>      redirect stdout to <file>");
        usageln(false, out, "    -stderr <file>      redirect stderr to <file>");
        usageln(false, out, "    -os <name>          the operating system for native executables");
        usageln(false, out, "                        Supported: 'windows', 'linux' or 'sunos' or 'macosx'");
        usageln(true,  out, "    -comp <name>        the C compiler used to build native executables");
        usageln(true,  out, "    -dll                builds squawk DLL instead of executable");
        usageln(true,  out, "    -nocomp             disables C compilation");
        usageln(true,  out, "                        Supported: 'msc', 'gcc' or 'cc' or 'gcc-macosx'");
        usageln(false, out, "    -jpda:<port>        start JVM listening for JPDA connection on 'port'");
        usageln(false, out, "    -jmem:<mem>         java memory option shortcut for '-java:-Xmx<mem>'");
        usageln(false, out, "    -java:<opts>        extra java options (e.g. '-java:-Xms128M')");
        usageln(true,  out, "    -javac:<opts>       extra javac options (e.g. '-javac:-g:none')");
        usageln(true,  out, "    -doccheck           use the DocCheck during java compilation");
        usageln(true,  out, "    -javadoc            generate complete javadoc during java compilation");
        usageln(true,  out, "    -javadoc:api        generate API javadoc during java compilation");
        usageln(true,  out, "    -bco                optimize classes with BCO");
        usageln(true,  out, "    -cflags:<opts>      extra C compiler options (e.g. '-cflags:-g')");
        usageln(true,  out, "    -lflags:<opts>      extra C linker options (e.g. '-lflags:/DLL')");
        usageln(true,  out, "    -o1                 optimize C compilation/linking for minimal size");
        usageln(true,  out, "    -o2                 optimize C compilation/linking for speed");
        usageln(true,  out, "    -o3                 optimize C compilation/linking for max speed");
        usageln(true,  out, "    -tracing            enable tracing in the slow VM");
        usageln(true,  out, "    -profiling          enable profiling in the slow VM");
        usageln(true,  out, "    -assume             enable assertions in the slow VM");
        usageln(true,  out, "    -typemap            enable type checking in the slow VM");
        usageln(false, out, "    -debug              enable debugging and assertion code in the Java code");
        usageln(true,  out, "    -prod               build the production version of the slow VM");
        usageln(false, out, "    -64                 build for a 64 bit system");
        usageln(false, out, "    -t                  time the execution of the command(s) executed");
        usageln(false, out, "    -k                  keep executing subsequent commands in a batch file");
        usageln(false, out, "                        even if a single command fails (c.f. '-k' in gnumake)");
        usageln(false, out, "    -verbose            verbose execution");
        usageln(false, out, "    -diag               print message when running subcommands (e.g. javac,");
        usageln(false, out, "                        preverifier, BCO etc)");
        usageln(true,  out, "    -props <file>       add to/override default properties from <file>");
        usageln(true,  out, "    -Dname=[value]      sets build property 'name' to 'value' (default='true')");
        usageln(true,  out, "    -wobulate           wobulate j2me and translator classes (default='false')");
        usageln(false, out, "    -help               show this usage message and exit");
        usageln(false, out, "");
        usageln(true,  out, "The '-tracing' and '-assume' options are");
        usageln(true,  out, "implicitly enabled if the '-prod' option is not specified.");
        usageln(true,  out, "");
        usageln(false, out, "The supported commands are (compilation commands are marked with '*'):");
        usageln(true,  out, "If no command is given then all compilation commands are run sequentially.");
        for (Enumeration e = commands.elements(); e.hasMoreElements();) {
            Command c = (Command)e.nextElement();
            usageln(false, out, "    "+c.name()+(c instanceof CompilationCommand ? " *" : ""));
        }

        if (exit) {
            System.exit(1);
        }
    }

    /**
     * Get the argument to a command line option. If the argument is not provided,
     * then a usage message is printed and the system exits.
     * @param args The command line arguments.
     * @param index The index at which the option's argument is located
     * @param opt The name of the option.
     * @return the options argument.
     */
    public String getOptArg(String[] args, int index, String opt) {
        if (index >= args.length) {
            usage("The " + opt + " option requires an argument.", true);
        }
        return args[index];
    }

    private void loadBuildProperties(String file) throws IOException {
        props.load(new FileInputStream(file));
        buildPropsFile = file;
    }

    /**
     * Create an instance of the builder to run a single command.
     * @param args
     * @param standardOut
     * @param standardErr
     * @throws Exception
     */
    public Build(PrintStream standardOut, PrintStream standardErr) throws Exception {

        this.defines    = find("define/src", ".java");
        this.cwd        = new File(System.getProperty("user.dir"));

        setOut(standardOut);
        setErr(standardErr);

        try {
            loadBuildProperties(buildPropsFile);
            try {
                loadBuildProperties("build.override");
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> build.override file found <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                BufferedReader reader = new BufferedReader(new FileReader("build.override"));
                String line = reader.readLine();
                while (line != null) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "+line);
                    }
                    line = reader.readLine();
                }
                reader.close();
            } catch(IOException ex) {
            }
        } catch (IOException ex) {
            /*
             * If there's no default build.properties file, then no compilation can be done
             */
            Vector newCommands = new Vector(commands.size());
            for (Enumeration e = commands.elements(); e.hasMoreElements(); ) {
                Object c = e.nextElement();
                if (!(c instanceof CompilationCommand)) {
                    newCommands.addElement(c);
                }
                commands = newCommands;
            }
            compilationSupported = false;
        }
    }

    /**
     * Parse the command line args passed to the builder.
     * @param args
     * @return the tail of the given 'args' that gives the command and
     * its arguments.
     * @throws Exception
     */
    public String[] parseArgs(String[] args) throws Exception {
        int argp        = 0;
        String osName   = System.getProperty("os.name" ).toLowerCase();
        CCompiler.Options cOptions = new CCompiler.Options();

        props.put("MACROIZE", "false"); // The default

        while (args.length > argp) {
            String arg = args[argp];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-stdout")) {
                this.stdout = new PrintStream(new FileOutputStream(getOptArg(args, ++argp, "-stdout")));
                System.setOut(stdout);
            } else if (arg.equals("-stderr")) {
                this.stderr = new PrintStream(new FileOutputStream(getOptArg(args, ++argp, "-stderr")));
                System.setErr(stderr);
            } else if (arg.equals("-os")) {
                osName = getOptArg(args, ++argp, "-os").toLowerCase();
            } else if (arg.equals("-dll")) {
                buildDLL = true;
            } else if (arg.equals("-comp")) {
                String compName = getOptArg(args, ++argp, "-comp").toLowerCase();
                if (compName.equals("msc")) {
                    ccompiler = new MscCompiler();
                } else if (compName.equals("gcc")) {
                    ccompiler = new GccCompiler();
                } else if (compName.equals("gcc-macox")) {
                    ccompiler = new GccMacOSXCompiler();
                } else if (compName.equals("cc")) {
                    ccompiler = new CcCompiler();
                } else {
                    usage("Non-supported C compiler: "+compName, true);
                }
            } else if (arg.equals("-nocomp")) {
                cOptions.disabled = true;
            } else if (arg.equals("-mac")) {
                cOptions.macroize = true;
                props.put("MACROIZE", "true");
            } else if (arg.startsWith("-cflags:")) {
                cOptions.cflags += " " + arg.substring("-cflags:".length());
            } else if (arg.startsWith("-lflags:")) {
                cOptions.lflags += " " + arg.substring("-lflags:".length());
            } else if (arg.startsWith("-jpda:")) {
                String port = arg.substring("-jpda:".length());
                javaOptions += " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + port;
            } else if (arg.startsWith("-jmem:")) {
                String mem = arg.substring("-jmem:".length());
                javaOptions += " -Xms" + mem + " -Xmx" + mem;
            } else if (arg.startsWith("-java:")) {
                javaOptions += " " + arg.substring("-java:".length());
            } else if (arg.startsWith("-javac:")) {
                javacOptions += " " + arg.substring("-javac:".length());
            } else if (arg.startsWith("-bco")) {
                bco = true;
            } else if (arg.equals("-javadoc")) {
                runJavadoc = true;
            } else if (arg.equals("-javadoc:api")) {
                runJavadoc = true;
                runJavadocAPI = true;
            } else if (arg.equals("-doccheck")) {
                runDocCheck = true;
            } else if (arg.equals("-o1")) {
                cOptions.o1 = true;
            } else if (arg.equals("-o2")) {
                cOptions.o2 = true;
            } else if (arg.equals("-o3")) {
                cOptions.o2 = true;
                cOptions.maxinline = true;
            } else if (arg.startsWith("-prod")) {
                cOptions.production = true;
            } else if (arg.equals("-64")) {
                cOptions.is64 = true;
                props.put("SQUAWK_64", "true");
            } else if (arg.equals("-tracing")) {
                cOptions.tracing = true;
            } else if (arg.equals("-profiling")) {
                cOptions.profiling = true;
            } else if (arg.equals("-assume")) {
                cOptions.assume = true;
            } else if (arg.equals("-typemap")) {
                cOptions.typemap = true;
            } else if (arg.equals("-t")) {
                useTimer = true;
            } else if (arg.equals("-k")) {
                ignoreErrors = true;
            } else if (arg.equals("-verbose")) {
                verbose = true;
            } else if (arg.equals("-diag")) {
                diagnostics = true;
            } else if (arg.equals("-props")) {
                String name = getOptArg(args, ++argp, "-props");
                loadBuildProperties(name);
            } else if (arg.startsWith("-D")) {
                String def = arg.substring("-D".length());
                int index = def.indexOf('=');
                if (index == -1) {
                    props.put(def, "true");
                } else {
                    props.put(def.substring(0, index), def.substring(index+1));
                }
            } else if (arg.equals("-debug")) {
                props.put("J2ME.DEBUG", "true");
            } else if (arg.equals("-wobulate")) {
                wobulateClasses = true;
            } else if (arg.equals("-help")) {
                usage(null, true);
            } else {
                usage("Unknown option "+arg, true);
            }
            argp++;
        }

        if (props.getProperty("SQUAWK_64", "false").equals("true")) {
            cOptions.is64 = true;
        }

        /* If we are building the secure squawk vm, wobulate files be default */
        if (props.getProperty("TRUSTED", "true").equals("true")) {
            wobulateClasses = true;
        }


        if (props.getProperty("J2ME.COLLECTOR", "").equals("java.lang.Lisp2Collector")) {
            props.setProperty("WRITE_BARRIER", "true");
            cOptions.cflags += " -DWRITE_BARRIER";
        } else {
            props.setProperty("WRITE_BARRIER", "false");
        }

        /*
         * The -tracing, and -assume options are turned on by default
         * if -production was not specified
         */
        if (!cOptions.production) {
            cOptions.tracing     = true;
            cOptions.assume      = true;
        }

        /*
         * Configure OS
         */
        os = createOS(osName);
        if (os == null) {
            usage("Non-supported OS: "+osName, true);
        }

        /*
         * Choose a default compiler
         */
        if (ccompiler == null) {
            if (os instanceof WindowsOS) {
                ccompiler = new MscCompiler();
            } else if (os instanceof LinuxOS) {
                ccompiler = new GccCompiler();
            } else if (os instanceof MacOSX) {
                ccompiler = new GccMacOSXCompiler();
            } else if (os instanceof SunOS) {
                ccompiler = new CcCompiler();
            }
        }
        this.cOptions = cOptions;

        if (argp == args.length) {
            return new String[] { "all" };
        } else {
            String[] cmdAndArgs = new String[args.length - argp];
            System.arraycopy(args, argp, cmdAndArgs, 0, cmdAndArgs.length);
            return cmdAndArgs;
        }
    }

    public int run(String[] cmdAndArgs) throws Exception {
        String cmd = cmdAndArgs[0];
        String[] cmdArgs;
        if (cmdAndArgs.length > 1) {
            cmdArgs = new String[cmdAndArgs.length - 1];
            System.arraycopy(cmdAndArgs, 1, cmdArgs, 0, cmdArgs.length);
        } else {
            cmdArgs = new String[0];
        }

        long start = System.currentTimeMillis();
        try {
            boolean all = cmd.equals("all");
            boolean found = false;
            for (Enumeration e = commands.elements(); e.hasMoreElements(); ) {
                Command c = (Command)e.nextElement();
                if (c.name().equals(cmd) || c.name2().equals(cmd)) {
                    c.run(cmd, cmdArgs);
                    found = true;

                    if (c instanceof CompilationCommand) {
                        if (cmdArgs.length != 0) {
                            cmd = cmdArgs[0];
                            Object old = cmdArgs;
                            cmdArgs = new String[cmdArgs.length - 1];
                            if (cmdArgs.length != 0) {
                                System.arraycopy(old, 1, cmdArgs, 0, cmdArgs.length);
                            }
                            // restart at top of command list
                            e = commands.elements();
                            continue;
                        }
                    }
                    break;
                } else if (all && c instanceof CompilationCommand) {
                    if (c instanceof CompilationCommandIfPresent) {
                        if (!exists(c.name())) {
                            continue;
                        }
                    }
                    try {
                        c.run(cmd, cmdArgs);
                    } catch (CommandFailedException ex) {
                        if (!ignoreErrors) {
                            throw ex;
                        }
                        System.err.println(ex);
                    }
                }
            }

            if (!all && !found) {
                stderr.println("Unknown command: " + cmd);
                return 1;
            }

        } finally {
            if (useTimer) {
                stdout.println("Time: "+(System.currentTimeMillis()-start)+"ms");
            }
        }


        return 0;
    }


    /*---------------------------------------------------------------------------*\
     *                          Java source preprocessor                         *
    \*---------------------------------------------------------------------------*/

    private Properties props = new Properties();
    private Hashtable messagesOutput = new Hashtable();

    /**
     * Gets the list of file prefixes that must have any calls to methods in lava.lang.Assert
     * to be converted to the fatal version of those calls.
     *
     * @return  the list of file prefixes to be converted
     */
    private Vector getFatalAssertionFilePrefixes() {
        Vector prefixes = new Vector();
        String files = props.getProperty("FATAL_ASSERTS");
        if (files != null) {
            StringTokenizer st = new StringTokenizer(files);
            while (st.hasMoreTokens()) {
                String file = fix(st.nextToken());
                prefixes.addElement(file);
            }
        }
        return prefixes;
    }

    /**
     * Preprocess a Java source file converting it into another Java
     * source file. This process comments out any lines between
     * preprocessing directives of the form:
     *
     *    /*if[<label>]* /   <-- close C-style comment - space added for
     *    /*end[<label>]* /  <-- syntactic correctness of this source file
     *
     * where the directive is at the beginning of a new line and the value
     * of the property named by <label> is 'false'. The properties are
     * specified in the 'build.properties' file.
     * @param filelist
     * @param clearLines If true, then excluded and preprocessing directive
     * lines are made empty as opposed to being commented.
     * This enables preprocessing directives to be placed in methods that may
     * be converted to macros by j2c. It also enables creating sources that
     * free from redundant stuff - good for extracting a Java Card API for
     * example
     * @return
     * @throws Exception
     */
    public String preprocessSource(String filelist, boolean clearLines, String outputDir, boolean isCLDC) throws Exception {
        StringBuffer outfilelist = new StringBuffer();
        String[] files = cut(fix(filelist));
        if (outputDir == null) {
            outputDir = "temp";
        }

        Vector fatalAssertionFilePrefixes = getFatalAssertionFilePrefixes();

        for (int i = 0 ; i < files.length ; i++) {
            String file = files[i];

            boolean assertsAreFatal = false;
            for (Enumeration e = fatalAssertionFilePrefixes.elements(); e.hasMoreElements();) {
                String prefix = (String)e.nextElement();
                if (file.startsWith(prefix)) {
                    assertsAreFatal = true;
                    break;
                }
            }

            /*
             * Replace the top-level element of the file's path with 'temp'
             */
            int sep = file.indexOf(File.separatorChar);
            if (sep == -1) {
                throw new CommandFailedException("Cannot find "+File.separator+" in: "+file);
            }
            String outfile = outputDir+file.substring(sep);

            boolean keep = preprocessSource(file, clearLines, outfile, assertsAreFatal, isCLDC);

            if (keep) {
                outfilelist.append(" "+outfile);
            } else {
                delete(outfile);
            }
        }

        return outfilelist.toString();
    }






    public boolean  preprocessSource(String         file,
                                     boolean        clearLines,
                                     String         outfile,
                                     boolean        assertsAreFatal,
                                     boolean        isCLDC) throws Exception
    {
        boolean isSquawk64 = props.getProperty("SQUAWK_64", "false").equals("true");
        String fileBase = new File(file).getName();

        boolean keep = true;
        boolean output = true;
        boolean assertEnabled = props.getProperty("J2ME.DEBUG", "false").equals("true");
        boolean elseClause = false;

        Stack lastOutputState = new Stack();
        Stack lastElseClauseState = new Stack();
        Stack currentLabel = new Stack();
        currentLabel.push("");

        /*
         * Open the streams for the input file and the generated file
         */
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        new File(new File(outfile).getParent()).mkdirs();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile)));
        PrintWriter out = new PrintWriter(bw);

        String line = br.readLine();
        int lineNo = 0;

        try {
            while(line != null) {
                lineNo++;
                /*
                 * "//if[..."
                 */
                if (line.startsWith("//if[")) {
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+file+":"+lineNo);
                    }
                    String sect = line.substring(5, end);
                    String prop = props.getProperty(sect);
                    if (prop == null) {
                        if (verbose) {
                            stdout.println("No setting for \""+sect+"\" in "+buildPropsFile+" ("+file+":"+lineNo+") setting true by default");
                        }
                        prop = "true"; // Default when not specified
                    }
                    if (prop.equals("false")) {
                        exclusions.put(sect, sect);
                        keep = false;
                        if (verbose) {
                            stdout.println("Rejecting "+sect+ " in "+file);
                        }
                        break;
                    }
                } else
                /*
                 * "/*if[...]* /"
                 */
                if (line.startsWith("/*if[")) {
                    if (!output && !NESTEDSECTIONS) {
                        throw new CommandFailedException("Cannot nest conditional sections "+file+":"+lineNo);
                    }
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+file+":"+lineNo);
                    }

                    lastOutputState.push(output ? Boolean.TRUE : Boolean.FALSE);
                    lastElseClauseState.push(elseClause ? Boolean.TRUE : Boolean.FALSE);

                    String label = line.substring(5, end);
                    String prop = props.getProperty(label);
                    if (prop == null) {
                        if (verbose) {
                            stdout.println("No setting for \""+label+"\" in "+buildPropsFile+" ("+file+":"+lineNo+") setting true by default");
                        }
                        prop = "true"; // Default when not specified
                    }
                    if (prop.equals("false")) {
                        exclusions.put(label, label);
                        if (verbose) {
                            String msg = "Excluding "+label+" in "+file;
                            if (messagesOutput.get(msg) == null) {
                                messagesOutput.put(msg, msg);
                                stdout.println(msg);
                            }
                        }
                        output = false;
                    }
                    if (clearLines) {
                        line = "";
                    }
                    currentLabel.push(label);
                } else
                /*
                 * "/*else[...]* /"
                 */
                if (line.startsWith("/*else[")) {
                    if (currentLabel.empty() || currentLabel.peek() == "") {
                        throw new CommandFailedException("else[] with no if[] in "+file+":"+lineNo);
                    }
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+file+":"+lineNo);
                    }

                    String label = (String)currentLabel.peek();
                    output = !output;

                    if (!label.equals(line.substring(7, end))) {
                        throw new CommandFailedException("if/else tag missmatch in "+file+":"+lineNo);
                    }

                    if (!output && verbose) {
                        String msg = "Excluding !"+label+" in "+file;
                        if (messagesOutput.get(msg) == null) {
                            messagesOutput.put(msg, msg);
                            stdout.println(msg);
                        }
                    }
                    elseClause = true;

                    if (clearLines) {
                        line = "";
                    }
                } else
                /*
                 * "/*end[...]* /"
                 */
                if (line.startsWith("/*end[")) {
                    if (currentLabel.empty() || currentLabel.peek() == "") {
                        throw new CommandFailedException("end[] with no if[] in "+file+":"+lineNo);
                    }
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+file+":"+lineNo);
                    }

                    String label = (String)currentLabel.pop();
                    output = (lastOutputState.pop() == Boolean.TRUE);
                    elseClause = (lastElseClauseState.pop() == Boolean.TRUE);

                    if (!label.equals(line.substring(6, end))) {
                        throw new CommandFailedException("if/end tag missmatch in "+file+":"+lineNo);
                    }
                    if (clearLines) {
                        line = "";
                    }
                } else {
                    if (!output) {
                        if (clearLines) {
                            line = "";
                        } else {
                            if (!elseClause) {
                                line = "//" + line;
                            } else {
                                if (!line.startsWith("//")) {
                                    throw new CommandFailedException("lines in /*else[...]*/ clause must start with '//': "+file+":"+lineNo);
                                }
                            }
                        }
                    } else {
                        if (elseClause) {
                            if (!line.startsWith("//")) {
                                throw new CommandFailedException("lines in /*else[...]*/ clause must start with '//': "+file+":"+lineNo);
                            }
                            line = "  " + line.substring(2);
                        }

                        /*
                         * "/*VAL* /"
                         */
                        line = replaceValueWithProperty(file, line, lineNo);

                        /*
                         * "/*WORD* /"
                         */
                        if (isSquawk64) {
                            line = replaceS64WithLong(file, line, lineNo);
                        }

                        /*
                         * Process any calls to methods in the com.sun.sqauwk.util.Assert class to
                         * disable them if necessary. This is only down for CLDC compilations.
                         */
                        if (isCLDC) {
                            line = editAssert(line, fileBase, lineNo, assertEnabled, assertsAreFatal);
                        }
                    }
                }

                out.println(line);
                line = br.readLine();
            }
        } catch (Exception ex) {
            if (ex instanceof CommandFailedException) {
                throw ex;
            }
            ex.printStackTrace();
            throw new CommandFailedException("Uncaught exception while processing "+file+":"+lineNo);
        }

        if (currentLabel.size() != 1) {
            String open = (String)currentLabel.pop();
            throw new CommandFailedException("no end[] for "+open+" in "+file+":"+lineNo);
        }
        out.close();
        bw.close();
        br.close();
        return keep;
    }

    /**
     * Converts a call to a method in the Assert class to use the fatal version of that call.
     * No conversion is done if the call is already the fatal version.
     *
     * @param line      the line containing the call
     * @param bracket   the index of the opening '(' of the call
     * @return the converted line
     */
    private String makeAssertFatal(String line, int bracket) {
        // Only convert to a fatal assert if it isn't already one
        if (line.lastIndexOf("Fatal", bracket) == -1 && line.lastIndexOf("always", bracket) == -1) {

            // convert call to fatal version
            line = line.substring(0, bracket) + "Fatal" + line.substring(bracket);
        }
        return line;
    }

    /**
     * Converts a call to a method in the Assert class so that the file and line number is prepended
     * to the String constant passed as the first parameter.
     *
     * @param line      the line containing the call
     * @param invoke    the index of the "Assert." in the line
     * @param file      the file containing the line
     * @param lineNo    the number of the line
     * @return the converted line
     */
    private String prependFileAndLineNo(String line, int invoke, String file, int lineNo) {
        int quote = line.indexOf('"', invoke);
        String fileAndLineNo = "\"[" + file + ":" + lineNo + "] ";
        if (quote != -1) {
            line = line.substring(0, quote) + fileAndLineNo + line.substring(quote + 1);
        } else {
            int bracket = line.lastIndexOf(");");
            if (bracket != -1) {
                String comma = ", ";
                if (line.charAt(bracket - 1) == '(') {
                    comma = "";
                }
                line = line.substring(0, bracket) + comma + fileAndLineNo + '"' + line.substring(bracket);
            }
        }
        return line;
    }

    /**
     * Edit any calls to methods in the Assert class. If assertions are enabled and the line
     * contains a call to a method in the Assert class that passes a String constant as a parameter,
     * then the current file name and line number are prepended to the string. Otherwise, if
     * assertions are not enabled and the line contains a call to a method in the Assert class, the
     * line is commented out.
     *
     * @param line    the line to edit
     * @param file    the file containing the line
     * @param lineNo  the line number of the line
     * @param enabled specifies if assertions are enabled
     * @param assertsAreFatal specifies that for the current class, all assertions must be converted to be fatal
     * @return the edited line
     */
    private String editAssert(String line, String file, int lineNo, boolean enabled, boolean assertsAreFatal) {
        int invoke = line.indexOf("Assert.");
        if (invoke != -1 && line.indexOf('(', invoke) != -1) {
            if (!enabled) {
                int bracket = line.indexOf('(', invoke);
                if (assertsAreFatal) {
                    line = makeAssertFatal(line, bracket);
                }

                String method = line.substring(invoke + "Assert.".length(), line.indexOf('(', invoke));
                if (method.startsWith("that") || method.startsWith("should")) {
                    String newLine;
                    if (line.lastIndexOf("throw", invoke) != -1) {
                        // Change "throw Assert..." into "throw (RuntimeException)null; // Assert..."
                        newLine = line.substring(0, invoke) + "(RuntimeException)null; //" + line.substring(invoke);
                    } else if (method.startsWith("that")) {
                        // Change "Assert..." into "if (false) Assert..."
                        newLine = line.substring(0, invoke) + "if (false) " + line.substring(invoke);
                    } else {
                        line = prependFileAndLineNo(line, invoke, file, lineNo);
                        newLine = line.substring(0, invoke) + "if (Assert.SHOULD_NOT_REACH_HERE_ALWAYS_ENABLED) " + line.substring(invoke);
                    }
                    line = newLine;
                }
            } else {
                if (assertsAreFatal) {
                    line = makeAssertFatal(line, line.indexOf('(', invoke));
                }

                // prepend file name and line number to message
                line = prependFileAndLineNo(line, invoke, file, lineNo);
            }
        }
        return line;
    }

    /**
     * Searches for the pattern "/*VAL* /<value>/*<name>* / and if found,
     * replaces 'value' with the value of the property named by 'name'
     * in the file build.properties.
     *
     * @param infile   the file being processed
     * @param line     the line being processed
     * @param lno      the number of the line being processed
     * @return the content of the line after the replacement (if any) has been performed
     */
    private String replaceValueWithProperty(String infile, String line, int lno) throws CommandFailedException {
        int index = line.indexOf("/*VAL*/");
        if (index != -1) {
            int defaultIndex = index + "/*VAL*/".length();

            int propIndex = line.indexOf("/*", defaultIndex);
            int propEndIndex = (propIndex == -1) ? -1 : line.indexOf("*/", propIndex);
            if (propIndex == -1 || propEndIndex == -1) {
                throw new CommandFailedException("Cannot find /*<property name>*/ after /*VAL*/ in "+infile+":"+lno);
            }

            String propName = line.substring(propIndex+2, propEndIndex);

            String prop = props.getProperty(propName);
            if (prop != null) {
                StringBuffer buf = new StringBuffer(line.length());
                buf.append(line.substring(0, defaultIndex)).
                    append(prop).
                    append(line.substring(propIndex));
                line = buf.toString();
            }
        }
        return line;
    }

    /**
     * Searches for the pattern "/*S64* /" prefixed or suffixed by "int", "Int" or "INT"
     * and if found, replaces the prefix or suffix with "long", "Long" or "LONG" respectively.
     *
     * @param infile   the file being processed
     * @param line     the line being processed
     * @param lno      the number of the line being processed
     * @return the content of the line after the replacement (if any) has been performed
     */
    private String replaceS64WithLong(String infile, String line, int lno) throws CommandFailedException {
        int index = line.indexOf("/*S64*/");
        int suffixIndex = index + "/*S64*/".length();
        boolean prefixed = false;
        boolean suffixed = false;
        if (index != -1) {
            if (index > 3) {
                String replace = null;
                String prefix = line.substring(index - 3, index);
                if (prefix.equals("int")) {
                    replace = "long";
                } else if (prefix.equals("Int")) {
                    replace = "Long";
                } else if (prefix.equals("INT")) {
                    replace = "LONG";
                }
                if (replace != null) {
                    prefixed = true;
                    line = line.substring(0, index - 3) + replace + line.substring(suffixIndex);
                    suffixIndex -= "/*S64*/".length();
                }
            }

            if (prefixed) {
                if (line.length() - suffixIndex >= 3 &&
                    line.substring(suffixIndex, suffixIndex + 3).equalsIgnoreCase("int"))
                {
                    throw new CommandFailedException("/*S64*/ macro must have exactly one \"int\", \"Int\", or \"INT\" prefix or suffix in "+infile+":"+lno);
                }
            } else {
                if (line.length() - suffixIndex < 3) {
                    throw new CommandFailedException("/*S64*/ macro must have exactly one \"int\", \"Int\", or \"INT\" prefix or suffix in "+infile+":"+lno);
                }

                String replace = null;
                String suffix = line.substring(suffixIndex, suffixIndex + 3);
                if (suffix.equals("int")) {
                    replace = "long";
                } else if (suffix.equals("Int")) {
                    replace = "Long";
                } else if (suffix.equals("INT")) {
                    replace = "LONG";
                }
                if (replace != null) {
                    suffixed = true;
                    line = line.substring(0, index) + replace + line.substring(suffixIndex + 3);
                }
            }
        }
        if (prefixed || suffixed) {
            return replaceS64WithLong(infile, line, lno);
        }
        return line;
    }

    /*---------------------------------------------------------------------------*\
     *                 java tools (javac, javadoc, BCO) execution                *
    \*---------------------------------------------------------------------------*/

    String javacOptions = "-g";
    boolean bco;

    /**
     * Gets a line of command line options from a file. The file to read is
     * found by starting with <code>dir</code> and looking for a file named
     * <code>name</code>. If no such file exists, the search recursively
     * continues to the parents of <code>dir</code>.
     *
     * @param   dir   the directory in which to start searching
     * @param   name  the name of the file containing the line of options
     * @param   command  the command to which the options pertain
     * @return  the first line of the first file named <code>name</code> found
     *                in a directory traversal starting at <code>dir</code> and
     *                recursing up parent directories
     * @throws CommandFailedException if the options file does not exist or if
     *                there was a problem reading the file
     */
    private String getOptionsFromFile(File dir, String name, String command) {
        dir = dir.getAbsoluteFile();
        while (dir != null) {
            File path = new File(dir, name);
            if (path.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(path));
                    String options = br.readLine();
                    if (verbose || diagnostics) {
                        stdout.println("[read "+command+" options from "+path+"]");
                    }
                    return options;
                } catch (IOException ex) {
                    throw new CommandFailedException("IO error while reading "+path+": "+ex);
                }
            }
            dir = dir.getParentFile();
        }
        //throw new CommandFailedException("Could not find file "+name);
        return null;
    }

    /**
     * Runs BCO over all the classes in a specified directory.
     *
     * @param   inputDir      directory containing classes to be processed
     * @param   outputDir     directory into which output is written
     * @throws  Exception     if something goes wrong
     */
    public boolean bco(String inputDir, String outputDir, File optionsFileDir) throws Exception {

        /*
         * Clean the output directory
         */
        clean(new File(outputDir), ".class");

        String options = "-d "+outputDir;
        if (optionsFileDir == null) {
            optionsFileDir = new File(inputDir);
        }
        //options += " -speculative";
        options += " -g -log " + fix(outputDir + "/bco.log ");
        String fileOptions = getOptionsFromFile(optionsFileDir, "bco.options", "bco");
        if (fileOptions == null) {
            return false;
        }
        options += " " + fileOptions;
        if (diagnostics) {
            stdout.println("[running BCO ...]");
        }
        int res;
        ensureDirExists(outputDir);
        if ((res = java("-Xmx300M -cp bco/classes", "com.sun.javawand.tools.opt.Optimizer", options+" "+ inputDir)) != 0) {
            throw new CommandFailedException("BCO returned "+res, res);
        }
        return true;
    }

    /**
     * Runs the Doc Check javadoc doclet over a set of Java source files.
     *
     * @param   baseDir      the parent directory under which the "javadoc"
     *                       directory exists (or will be created) for the output
     * @param   classPathArg the class path option used to compile the source
     *                       files. This is used to find the javadoc
     *                       generated for the classes in the class path.
     * @param   sourcePath   the path to find the sources
     * @param   packages     the names of the packages whose sources are to be processed
     * @throws  Exception    if something goes wrong
     */
    public void javadoc(String baseDir, String classPathArg, String sourcePath, String packages) throws Exception {
        String outputDir = fix(baseDir + "/javadoc");
        if (diagnostics) {
            stdout.println("[running javadoc (output dir: "+outputDir+") ...]");
        }

        classPathArg = fix(classPathArg.trim());
        String classPath = classPathArg.substring(classPathArg.lastIndexOf(' ')+1);
        StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
        String linkOptions = "";
        String classesSuffix = fix("/classes");
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            /*
             * only use the path if it ends with "classes"
             */
            if (path.endsWith(classesSuffix)) {
                String pathBase = path.substring(0, path.length() - ("classes".length()+1));
                linkOptions += " -link ../../" + pathBase + "/javadoc";
                sourcePath += fix(";" + pathBase + "/src");
            }
        }

        ensureDirExists(outputDir);
        clean(new File(outputDir), "");

        int res = exec(
                        os.getJDKTool("javadoc"), "-d "+outputDir+
                        " -classpath " + classPath +
                        linkOptions +
                        (runJavadocAPI ? "" : " -private") +
                        " -quiet" +
                        " -breakiterator" +
                        " -sourcepath " + sourcePath + " "+
                        packages
                      );

        if (res != 0) {
            throw new CommandFailedException("javadoc returned "+res, res);
        }
    }

    /**
     * Runs the Doc Check javadoc doclet over a set of Java source files.
     *
     * @param   baseDir      the parent directory under which the "doccheck"
     *                       directory exists (or will be created) for the output
     * @param   classPathArg the class path option used to compile the source
     *                       files. This is used to find the javadoc
     *                       generated for the classes in the class path.
     * @param   sourcePath   the path to find the sources
     * @param   packages     the names of the packages whose sources are to be processed
     * @throws  Exception    if something goes wrong
     */
    public void doccheck(String baseDir, String classPathArg, String sourcePath, String packages) throws Exception {
        String outputDir = fix(baseDir + "/doccheck");
        if (diagnostics) {
            stdout.println("[running doccheck (output dir: "+outputDir+") ...]");
        }

        ensureDirExists(outputDir);
        clean(new File(outputDir), "");

        classPathArg = fix(classPathArg.trim());
        String classPath = classPathArg.substring(classPathArg.lastIndexOf(' ')+1);

        int res = exec(
                        "javadoc", "-d "+outputDir+
                        " -classpath " + classPath +
                        " -execDepth 2" +
                        " -evident 5" +
                        " -private" +
                        " -sourcepath " + sourcePath +
                        " -docletpath " + fix("tools/doccheck.jar")+
                        " -doclet com.sun.tools.doclets.doccheck.DocCheck " +
                        packages
                      );

        if (res != 0) {
            throw new CommandFailedException("doccheck returned "+res, res);
        }
    }

    /**
     * Extract a source path and list of packages from a given set of Java
     * source files. Each source file is presumed to contain the substring
     * "src" which is used to partition source file path into a source path
     * element and a package element.
     *
     * @param   sourceFiles  the set of source files
     * @param   packages     the vector onto which the extracted set of unique
     *                       packages are appended
     * @return  a vector of the extracted source path elements
     */
    public Vector extractSourcePathAndPackages(String sourceFiles, Vector packages) {
        Vector sp = new Vector();
        StringTokenizer st = new StringTokenizer(sourceFiles);
        while (st.hasMoreElements()) {
            String sourceFile = st.nextToken();
            if (sourceFile.endsWith(".java")) {
                int srcIndex = sourceFile.indexOf("src");
                if (srcIndex != -1) {
                    srcIndex += 3; // bump to '/' or '\' after "src"
                    String spElement = sourceFile.substring(0, srcIndex);
                    if (!sp.contains(spElement)) {
                        sp.addElement(spElement);
                    }

                    int index = sourceFile.lastIndexOf(File.separatorChar);
                    if (index > srcIndex) {
                        String pkg = sourceFile.substring(srcIndex+1, index);
                        pkg = pkg.replace(File.separatorChar, '.');
                        if (!packages.contains(pkg)) {
                            packages.addElement(pkg);
                        }
                    }
                }
            }
        }
        return sp;
    }

    /**
     * Copy the "doc-files" subdirectory correspondong to a given Java source.
     *
     * @param javadocDir
     * @param files
     */
    private void copyDocFiles(String javadocDir, String files) throws IOException {
        Vector packages = new Vector();
        Vector sourcePath = extractSourcePathAndPackages(files, packages);
        Vector docFileDirs = new Vector();
        Vector docFiles = new Vector();

        for (Enumeration e = sourcePath.elements(); e.hasMoreElements();) {
            File path = new File((String)e.nextElement());

            docFileDirs.clear();
            com.sun.squawk.util.Find.find(path, "doc-files", docFileDirs, true);

            for (Enumeration ee = docFileDirs.elements(); ee.hasMoreElements();) {
                File docFileDir = new File((String)ee.nextElement());
                docFiles.clear();
                com.sun.squawk.util.Find.find(docFileDir, null, docFiles, false);

                String cvsInfix = fix("/CVS/");
                String srcInfix = fix("src/");
                for (Enumeration eee = docFiles.elements(); eee.hasMoreElements();) {
                    String file = (String)eee.nextElement();
                    if (file.indexOf(cvsInfix) == -1) {
                        int index = file.indexOf(srcInfix);
                        String dstFile = javadocDir + file.substring(index + 3);
                        cp(file, dstFile);
                    }
                }
            }
        }

    }

    /**
     * Compile a set of classes.
     *
     * @param   classPath  the compilation class path
     * @param   outputDir  the directory into the classes are to be written
     * @param   files      the list of source files to compile
     * @throws  Exception  if something goes wrong
     */
    public void javac(String classPath, String outputDir, String files, boolean isCLDC) throws Exception {
        outputDir = fix(outputDir);
        String baseDir = (new File(outputDir)).getParent();
        String processedFiles = preprocessSource(files, clearJppLines, null, isCLDC);
        if (diagnostics) {
            stdout.println("[running javac ...]");
        }

        ensureDirExists(outputDir);
        String args = classPath+" "+javacOptions+" -d "+outputDir+" "+processedFiles;
        if (isCLDC) {
            args = " -target 1.2 -source 1.3 " + args;
        } else {
            // required to make Hotspot run via JNI on Mac OS X
            args = " -target 1.2 -source 1.2 " + args;
        }
        int res = javaCompile(this, args, verbose);
        if (res != 0) {
            throw new CommandFailedException("javac returned "+res, res);
        }


        if (runDocCheck || runJavadoc) {
            Vector packageList = new Vector();
            Vector sourcePath = extractSourcePathAndPackages(processedFiles, packageList);

            String sp = join(sourcePath, 0, sourcePath.size(), File.pathSeparator);
            String packages = join(packageList, 0, packageList.size(), " ");

            if (runDocCheck) {
                doccheck(baseDir, classPath, sp, packages);
            }
            if (runJavadoc) {
                javadoc(baseDir, classPath, sp, packages);

                /*
                 * Copy the "doc-files" directories
                 */
                copyDocFiles(fix(baseDir+"/javadoc"), files);
            }
        }

        if (!verbose) {
            clean(new File("temp/src"), ".java"); // delete temp files if -verbose is not used
        }
    }

    /**
     * Compile a set of classes that will be deployed on a J2SE system.
     *
     * @param   classPath  the compilation class path
     * @param   baseDir    the parent of the "classes" directory which will
     *                     contain the output of the compilation
     * @param   files      the list of source files to compile
     * @return  0
     * @throws  Exception  if something goes wrong
     */
    public int javac_j2se(String classPath, String baseDir, String files) throws Exception {
        if (classPath != null) {
            classPath = "-classpath "+classPath;
        } else {
            classPath = "";
        }
        String source = "";

        /*
         * Clean out the class files from the last build of this target in
         * case it was build with different preprocessor settings
         */
        clean(new File(baseDir), ".class");

        String outputDir = baseDir+"/classes";
        javac(source + classPath, outputDir, files, false);
        return 0;
    }



    /**
     * Run the CLDC preverifier over a set of classes and write the resulting
     * classes to a specified directory.
     *
     * @param   classPath   directories in which to look for classes
     * @param   outputDir   directory into which output is written
     * @param   input       classnames and/or directory names containing input
     * @throws  Exception   if something goes wrong
     */
    public void preverify(String classPath, String outputDir, String input) throws Exception {
        /*
         * Clean the output directory
         */
        clean (new File(outputDir), ".class");

        if (diagnostics) {
            stdout.println("[running preverifier ...]");
        }
        ensureDirExists(outputDir);

        // Ensure that the preverifier is executable which may not be the case if
        // Squawk was checked out with a Java CVS client (Java doesn't know anything
        // about 'execute' file permissions and so cannot preserve them).
        chmod(os.getPreverifierPath(), "+x");

        int res = exec(os.getPreverifierPath()+" "+classPath+" -d "+outputDir+" " + input);
        if (res != 0) {
            throw new CommandFailedException("Preverifier failed", res);
        }
    }


    /**
    * Run the Wobulator over the set of preverified classes.
    *
    * @param   classPath   directories in which to look for classes
    * @param   outputDir   directory into which output is written
    * @throws  Exception   if something goes wrong
    */
   public void wobulate(String classPath, String outputDir) throws Exception {
       /*
        * Clean the output directory
        */
       clean (new File(outputDir), ".class");

       if (diagnostics) {
           stdout.println("[running wobulator ...]");
       }

       ensureDirExists(outputDir);

       /* Run the automatic wobulator */
       int res = exec("java -jar " + fix("tools/wobulator.jar") +
                      " -auto" +
                      " -cp " + classPath +
                      " -storepass 123456" +
                      " -keystore " +fix("tools/cldc.keystore") +
                      " -d " + outputDir);

       if (res != 0) {
           throw new CommandFailedException("Wobulator failed", res);
       }
   }



    /**
     * Compile a set of classes that will be deployed on a CLDC system. This
     * process includes the necessary preverification step.
     *
     * @param   classPath  the compilation class path
     * @param   baseDir    the parent of the "classes" directory which will
     *                     contain the output of the compilation
     * @param   files      the list of source files to compile
     * @throws  Exception  if something goes wrong
     */
    public int javac_j2me(String classPath, String baseDir, String files) throws Exception {
        String javacClassPath;
        String prevClassPath;

if (baseDir.equals("j2meTests")) {
    baseDir = "j2me";
} else {
        /*
         * Clean out the class files from the last build of this target in
         * case it was build with different preprocessor settings
         */

        clean(new File(baseDir), ".class");
}
        if (classPath != null) {
            javacClassPath = "-bootclasspath "+classPath;
            prevClassPath  = " -classpath "+classPath;
        } else {
            javacClassPath = "-bootclasspath .";
            prevClassPath  = "";
        }

        String javacOutputDir = baseDir + "/classes";
        String j2meOutputDir  = baseDir + "/j2meclasses";

        /*
         * Compile the sources
         */
        javac(javacClassPath, javacOutputDir, files, true);

        if (bco) {
            /*
             * Run BCO
             */
            if (bco(javacOutputDir, j2meOutputDir, null)) {
                return 0;
            }
            stdout.println("Warning could not find bco.options file for "+baseDir+" using KVM preverifier");
        }

        /*
         * Run preverifier on output from javac only
         */

        if(wobulateClasses) {
            String preverifyOutputDir = baseDir + "/preverify";
            preverify(prevClassPath, preverifyOutputDir, javacOutputDir);
            wobulate(preverifyOutputDir, j2meOutputDir);
        } else {
            preverify(prevClassPath, j2meOutputDir, javacOutputDir);
        }


        return 0;
    }

    /**
     * Assemble a set of classes that will be deployed on a CLDC system. This
     * process includes the necessary preverification step.
     *
     * @param   classPath  the preverifier class path
     * @param   baseDir    the parent of the "classes" directory which will
     *                     contain the output of the compilation
     * @param   files      the list of ".j" files to compile
     * @throws  Exception  if something goes wrong
     */
    public int jasmin_j2me(String classPath, String baseDir, String files) throws Exception {
        String prevClassPath = " -classpath "+classPath;

        /*
         * Clean out the class files from the last build of this target in
         * case it was build with different preprocessor settings
         */
        clean(new File(baseDir), ".class");

        String jasminOutputDir = baseDir+"/classes";
        String j2meOutputDir  = baseDir+"/j2meclasses";

        /*
         * Assemble the sources
         */
        int res = exec(os.getJDKTool("java")+" -jar jasmin/jasmin.jar -d "+jasminOutputDir + " " + files);
        if (res != 0) {
            throw new CommandFailedException("jasmin returned "+res, res);
        }

        if (bco) {
            /*
             * Run BCO
             */
            if (bco(jasminOutputDir, j2meOutputDir, null)) {
                return 0;
            }
            stdout.println("Warning could not find bco.options file for "+baseDir+" using KVM preverifier");
        }

        /*
         * Run preverifier on output from javac only
         */
        preverify(prevClassPath, j2meOutputDir, jasminOutputDir);
        return 0;
    }

    /*---------------------------------------------------------------------------*\
     *                             Java execution                                *
    \*---------------------------------------------------------------------------*/

    String javaOptions = "";

    /**
     * Execute a Java program.
     *
     * @param vmArgs
     * @param mainClass
     * @param appArgs
     * @return int
     */
    public int java(String vmArgs, String mainClass, String appArgs) throws Exception {
        return exec(os.getJDKTool("java")+" "+vmArgs+" "+javaOptions+" "+mainClass, appArgs);
    }

    /*
     * fix
     */
    public static String fix(String str) {
        str = str.replace(';', File.pathSeparatorChar);
        str = str.replace('/', File.separatorChar);
        str = str.replace('#', '/');
        return str;
    }


    /**
     * Cuts a string of white space separated tokens into an array of strings, one
     * element for each token.
     *
     * @param str           the string to cut
     * @param preambleSize  the number of extra null elements to prepend to the result
     * @return 'str' as an array of strings
     */
    public static String[] cut(String str, int preambleSize) {
    StringTokenizer st = new StringTokenizer(str, " ");
        String res[] = new String[st.countTokens()+preambleSize];
        while (st.hasMoreTokens()) {
            res[preambleSize++] = st.nextToken();
        }
        return res;
    }

    /**
     * Fold an array of strings into a single string.
     *
     * @param   parts   the array to fold
     * @param   offset  the offset at which folding starts
     * @param   length  the numbers of elements to fold
     * @param   delim   the delimiter to place between the folded elements
     * @return  the folded string
     */
    public static String join(String[] parts, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(parts[i]);
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /**
     * Fold a vector of objects into a single string. The toString method is
     * used to convert each object into a string.
     *
     * @param   parts   the vector to fold
     * @param   offset  the offset at which folding starts
     * @param   length  the numbers of elements to fold
     * @param   delim   the delimiter to place between the folded elements
     * @return  the folded string
     */
    public static String join(Vector parts, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(parts.elementAt(i));
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /*
     * cut
     */
    public static String[] cut(String str) {
        return cut(str, 0);
    }


    /*---------------------------------------------------------------------------*\
     *                          System command execution                         *
    \*---------------------------------------------------------------------------*/

    private File cwd;

    /**
     * Change the current working directory. This only effects the working directory
     * of commands run through 'exec()'.
     * @param dir
     */
    public void cd(String dir) {
        dir = fix(dir);
        if (verbose) {
            stdout.println("cd "+dir);
        }
        File fDir = new File(dir);
        if (fDir.isAbsolute()) {
            cwd = fDir;
        } else {
            try {
                cwd = (new File(cwd, dir)).getCanonicalFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /*
     * exec
     */
    public int exec(String cmd) throws Exception {
        return exec(cmd, "");
    }

    /*
     * exec
     */
    public int exec(String cmd, String options) throws Exception {
        return exec(cmd, options, null);
    }

    StreamGobbler.Filter execOutputFilter;
    public int exec(String cmd, String options, String[] envp) throws Exception {

        PrintStream out = stdout;
        PrintStream err = stderr;

        cmd = fix(cmd);
        if (options != null && options.length() > 0) {
            cmd += " " + options;
        }
        if (verbose) {
            stdout.println("EXEC: "+cmd);
        }

        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(cmd, envp, cwd);
            StreamGobbler errorGobbler  = new StreamGobbler(proc.getErrorStream(), err, null, execOutputFilter);
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), out, null, execOutputFilter);
            errorGobbler.start();
            outputGobbler.start();

            int exitVal = proc.waitFor();
            errorGobbler.join();
            outputGobbler.join();

            if (verbose || exitVal != 0) {
                stdout.println("EXEC result =====> " + exitVal);
            }
            if (exitVal != 0) {
                throw new CommandFailedException("Process.exec("+cmd+") returned "+exitVal, exitVal);
            }
            return exitVal;
        } catch (InterruptedException ie) {
            return -1;
        } finally {
            /*
             * Ensure that the native process (if any is killed).
             */
            if (proc != null) {
                proc.destroy();
                proc = null;
            }
        }
    }
}

class CommandFailedException extends RuntimeException {
    public final int exitVal;
    CommandFailedException(String msg, int exitVal) {
        super(msg);
        this.exitVal = exitVal;
    }
    CommandFailedException(String msg) {
        super(msg);
        this.exitVal = 1;
    }
}
