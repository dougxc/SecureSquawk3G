package com.sun.squawk.builder.tasks;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

/**
 * A Preprocessor instance is used to preprocess one or more input files in a manner similar to
 * the standard C preprocessor. The patterns recognized by the preprocessor and their corresponding
 * semantics are described below:
 *
 *  1. A section of code can be conditionally compiled by enclosing it in
 *
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */
public class Preprocessor extends Task {

    public Preprocessor(Properties properties) {
        this.properties = properties;
    }

    /**
     * The properties used to drive the preprocessing.
     */
    private final Properties properties;

    /**
     * Determines if source lines to be disabled during preprocessing should be replaced by
     * the empty string or prefixed by a C++ style comment. Default is true.
     */
    private boolean useComments = true;

    /**
     * Determines if source lines to be disabled during preprocessing should be replaced by
     * the empty string or prefixed by a C++ style comment.
     *
     * @return true if C++ style comments are to be used
     */
    public boolean getUseComments() {
        return useComments;
    }

    /**
     * Sets the attribute determining if source lines to be disabled during preprocessing should be replaced by
     * the empty string or prefixed by a C++ style comment.
     *
     * @return value the value to set the attribute to
     */
    public void setUseComments(boolean value) {
        useComments = value;
    }

    /**
     * Preprocess a set of files placing the resulting output in a given directory. Any existing files
     * are overwritten unless <code>overwrite</code> is <code>true</code>.
     *
     * @param inputFiles          the files to preprocess
     * @param destDir             the destination directory
     * @param disableWithComments if true, prefix disabled input lines with a C++ style comment otherwise
     *                            set them to be the empty string
     * @param editAssertCalls     if true, then lines containing a non-fully qualified call to a static
     *                            method in a class with the name "Assert" are {@link #editAssert edited}
     * @param overwrite           if true, then existing files are overwritten
     * @return the list of generated output files
     */
    public File[] execute(File[] inputFiles, File destDir, boolean disableWithComments, boolean editAssertCalls) throws IOException {

        Set fatalAssertionFiles = getFatalAssertionFiles();
        ArrayList outputFileList = new ArrayList(inputFiles.length);

        for (int i = 0 ; i < inputFiles.length ; i++) {
            File file = inputFiles[i];
            boolean assertsAreFatal = fatalAssertionFiles.contains(file);

            File outputFile = new File(destDir, file.getPath());
            preprocess(file, disableWithComments, outputFile, assertsAreFatal, editAssertCalls);
            if (outputFile.exists()) {
                outputFileList.add(outputFile);
            }
        }

        File[] outputFiles = new File[outputFileList.size()];
        outputFileList.toArray(outputFiles);
        return outputFiles;
    }

    /**
     * Decodes the property (if any) with the key "FATAL_ASSERTS" whose value is a space separated list of file names.
     * These files must have any calls to methods in the Assert class to be converted to the fatal version of those calls.
     *
     * @return  the set of files to be converted
     */
    private Set getFatalAssertionFiles() {
        Set set = new HashSet();
        String value = properties.getProperty("FATAL_ASSERTS");
        if (value != null) {
            StringTokenizer st = new StringTokenizer(value);
            while (st.hasMoreTokens()) {
                set.add(new File(st.nextToken()));
            }
        }
        return set;
    }


    public boolean preprocess(String inputFile, boolean disableWithComments, String outputFile, boolean assertsAreFatal, boolean editAssertCalls) throws IOException {
        boolean isSquawk64 = properties.getProperty("SQUAWK_64", "false").equals("true");

        String fileBase = new File(inputFile).getName();

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
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
        new File(new File(outputFile).getParent()).mkdirs();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
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
                        throw new CommandFailedException("Cannot find ] in "+inputFile+":"+lineNo);
                    }
                    String sect = line.substring(5, end);
                    String prop = props.getProperty(sect);
                    if (prop == null) {
                        if (verbose) {
                            stdout.println("No setting for \""+sect+"\" in "+buildPropsFile+" ("+inputFile+":"+lineNo+") setting true by default");
                        }
                        prop = "true"; // Default when not specified
                    }
                    if (prop.equals("false")) {
                        exclusions.put(sect, sect);
                        keep = false;
                        if (verbose) {
                            stdout.println("Rejecting "+sect+ " in "+inputFile);
                        }
                        break;
                    }
                } else
                /*
                 * "/*if[...]* /"
                 */
                if (line.startsWith("/*if[")) {
                    if (!output && !NESTEDSECTIONS) {
                        throw new CommandFailedException("Cannot nest conditional sections "+inputFile+":"+lineNo);
                    }
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+inputFile+":"+lineNo);
                    }

                    lastOutputState.push(output ? Boolean.TRUE : Boolean.FALSE);
                    lastElseClauseState.push(elseClause ? Boolean.TRUE : Boolean.FALSE);

                    String label = line.substring(5, end);
                    String prop = props.getProperty(label);
                    if (prop == null) {
                        if (verbose) {
                            stdout.println("No setting for \""+label+"\" in "+buildPropsFile+" ("+inputFile+":"+lineNo+") setting true by default");
                        }
                        prop = "true"; // Default when not specified
                    }
                    if (prop.equals("false")) {
                        exclusions.put(label, label);
                        if (verbose) {
                            String msg = "Excluding "+label+" in "+inputFile;
                            if (messagesOutput.get(msg) == null) {
                                messagesOutput.put(msg, msg);
                                stdout.println(msg);
                            }
                        }
                        output = false;
                    }
                    if (disableWithComments) {
                        line = "";
                    }
                    currentLabel.push(label);
                } else
                /*
                 * "/*else[...]* /"
                 */
                if (line.startsWith("/*else[")) {
                    if (currentLabel.empty() || currentLabel.peek() == "") {
                        throw new CommandFailedException("else[] with no if[] in "+inputFile+":"+lineNo);
                    }
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+inputFile+":"+lineNo);
                    }

                    String label = (String)currentLabel.peek();
                    output = !output;

                    if (!label.equals(line.substring(7, end))) {
                        throw new CommandFailedException("if/else tag missmatch in "+inputFile+":"+lineNo);
                    }

                    if (!output && verbose) {
                        String msg = "Excluding !"+label+" in "+inputFile;
                        if (messagesOutput.get(msg) == null) {
                            messagesOutput.put(msg, msg);
                            stdout.println(msg);
                        }
                    }
                    elseClause = true;

                    if (disableWithComments) {
                        line = "";
                    }
                } else
                /*
                 * "/*end[...]* /"
                 */
                if (line.startsWith("/*end[")) {
                    if (currentLabel.empty() || currentLabel.peek() == "") {
                        throw new CommandFailedException("end[] with no if[] in "+inputFile+":"+lineNo);
                    }
                    int end = line.indexOf(']');
                    if (end == -1) {
                        throw new CommandFailedException("Cannot find ] in "+inputFile+":"+lineNo);
                    }

                    String label = (String)currentLabel.pop();
                    output = (lastOutputState.pop() == Boolean.TRUE);
                    elseClause = (lastElseClauseState.pop() == Boolean.TRUE);

                    if (!label.equals(line.substring(6, end))) {
                        throw new CommandFailedException("if/end tag missmatch in "+inputFile+":"+lineNo);
                    }
                    if (disableWithComments) {
                        line = "";
                    }
                } else {
                    if (!output) {
                        if (disableWithComments) {
                            line = "";
                        } else {
                            if (!elseClause) {
                                line = "//" + line;
                            } else {
                                if (!line.startsWith("//")) {
                                    throw new CommandFailedException("lines in /*else[...]*/ clause must start with '//': "+inputFile+":"+lineNo);
                                }
                            }
                        }
                    } else {
                        if (elseClause) {
                            if (!line.startsWith("//")) {
                                throw new CommandFailedException("lines in /*else[...]*/ clause must start with '//': "+inputFile+":"+lineNo);
                            }
                            line = "  " + line.substring(2);
                        }

                        /*
                         * "/*VAL* /"
                         */
                        line = replaceValueWithProperty(inputFile, line, lineNo);

                        /*
                         * "/*WORD* /"
                         */
                        if (isSquawk64) {
                            line = replaceS64WithLong(inputFile, line, lineNo);
                        }

                        /*
                         * Process any calls to methods in the com.sun.sqauwk.util.Assert class to
                         * disable them if necessary. This is only down for CLDC compilations.
                         */
                        if (editAssertCalls) {
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
            throw new CommandFailedException("Uncaught exception while processing "+inputFile+":"+lineNo);
        }

        if (currentLabel.size() != 1) {
            String open = (String)currentLabel.pop();
            throw new CommandFailedException("no end[] for "+open+" in "+inputFile+":"+lineNo);
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
    public String editAssert(String line, String file, int lineNo, boolean enabled, boolean assertsAreFatal) {
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


}
