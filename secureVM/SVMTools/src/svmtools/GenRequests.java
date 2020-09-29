package svmtools;

import java.io.*;
import java.util.*;
import org.apache.bcel.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;

/**
 * This is a simple utility that process a set of classes and generates a
 * template permit request file in a format almost compatible with
 * the PermitManager utility.
 * For each class, it generates a subclass privilege request for
 * its superclass and each interface it implements (if any) and a
 * class resource access privilege request for each class for which it
 * accesses a class resource (i.e. a static field/method or a constructor).
 * The output needs to have the "key=<alias>" value added to each entry
 * before it can be feed in to the PermitTool.
 */
public class GenRequests {

    private String alias;
    private Hashtable passwds = new Hashtable();
    private Set requests = new HashSet();
    private String currentClass;
    Set domains;

    private GenRequests(Set domains)
    {
        this.domains = domains;
    }

    private String getAliasResponse() {
        System.err.print("Enter alias ('':use last response,'-':skip permit): ");
        String response = null;
        try {
            response = (new BufferedReader(
                new InputStreamReader(System.in))).readLine().trim();
        } catch (IOException ioe) {
        }
        if (response == null)
            return null;
        if (response.length() == 0)
            return alias;
        if (response.length() == 1 && response.charAt(0) == '-')
            return null;
        alias = response;
        return alias;
    }

    private void processFiles(String files[]) throws Exception {
        for (int i = 0; i < files.length; i++) {
            String classFile = files[i];

        JavaClass clazz = new ClassParser(classFile).parse();
            ConstantPool cp = clazz.getConstantPool();
            ConstantPoolGen cpGen = new ConstantPoolGen(cp);

            currentClass = clazz.getClassName();
            Set subPermits = new HashSet();
            Set craPermits = new HashSet();

//ADD SUPPORT FOR -I and -AIPT OPTIONS

            // Find the superclass and interfaces (if any) of the class
            String superClass = clazz.getSuperclassName();
            if (!subPermits.contains(superClass))
                makeSubclassRequest(superClass);
            String[] interfaces = clazz.getInterfaceNames();
            for (int j = 0; j != interfaces.length; ++j) {
                String interfaceName = interfaces[j];
                if (!subPermits.contains(interfaceName))
                    makeSubclassRequest(interfaceName);

            }

            // To find the class resources accessed by this class, search for
            // a call to a constructor (INVOKESPECIAL) or any static access
            // to a class (GETSTATIC,PUTSTATIC,INVOKESTATIC).
            Method[] methods = clazz.getMethods();
            for (int j = 0; j != methods.length; ++j) {
                Method m = methods[j];
                Code code = m.getCode();
                if (code != null) {
                    InstructionList list = new InstructionList(code.getCode());
                    InstructionHandle[] insts = list.getInstructionHandles();
                    for (int k = 0; k != insts.length; ++k ) {
                        Instruction inst = insts[k].getInstruction();
                        switch (inst.getTag()) {
                        case Constants.INVOKESPECIAL:
                            // Only interested in call to a constructor (except
                            // constructor of direct superclass)
                            InvokeInstruction ii = (InvokeInstruction)inst;
                            if (!ii.getMethodName(cpGen).equals("<init>") ||
                                ii.getClassName(cpGen).equals(clazz.getSuperclassName()))
                                break;
                        case Constants.GETSTATIC:
                        case Constants.PUTSTATIC:
                        case Constants.INVOKESTATIC:
                            org.apache.bcel.generic.FieldOrMethod fmInst =
                                (org.apache.bcel.generic.FieldOrMethod)inst;
                            String grantor = fmInst.getClassName(cpGen);
                            if (!grantor.equals(currentClass) &&
                                !craPermits.contains(grantor)) {
                                makeClassResourceAccessRequest(grantor);
                                craPermits.add(grantor);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void makeRequest(String grantor, String type) {
        if (domains != null && DomainSpec.shareDomain(domains,grantor,currentClass))
            return;
        String fullType = (type.equals("s") ? "Subclass" : "Class resource access");
        System.out.println(fullType + " permit granted by "+grantor+" to "+currentClass);
        String thisAlias = getAliasResponse();
        if (thisAlias == null)
            return;
        String request = "grantee="+currentClass+",grantor="+grantor+
            ",type="+type+",key="+thisAlias;
        requests.add(request);
    }

    private void makeSubclassRequest(String grantor) {
        makeRequest(grantor,"s");
    }
    private void makeClassResourceAccessRequest(String grantor) {
        makeRequest(grantor,"c");
    }

    private void serialize(PrintStream out) throws Exception {
        for (Iterator i = requests.iterator(); i.hasNext(); ) {
            out.println(i.next());
        }
    }

    /**
     * Helper method to extract and add permits from an existing Trusted
     * attribute.
     */
    private void addPermitGrantors(Trusted.Permit permits[],
        ConstantPool cp, Set grantors) {
        if (permits.length == 0)
            return;
        for (int i = 0; i != permits.length; ++i) {
            Trusted.Permit p = permits[i];
            String grantor = cp.constantToString(p.getClassIndex(),
                Constants.CONSTANT_Class);
            grantors.add(grantor);
        }
    }

    /**
     * Print the usage message on stderr.
     */
    static private void usage() {
    System.err.println(
    "Usage:\n"+
    "GenRequests [ -out <reqfile> ] \n"+
    "            [ -domain <dom_spec> ]* - files...\n"+
    "where:\n"+
    "   <reqfile>    - the file to which the requests will be written\n"+
    "                  (default: stdout).\n"+
    "   <dom_spec>   - a domain spec. Comma separated list of domain members\n"+
    "                  specified as explicit classes (e.g. 'java.lang.Class'),\n"+
    "                  packages (e.g. 'java.lang.*') and package roots\n"+
    "                  (e.g. 'java.lang.**'). Requests will not be generated for\n"+
    "                  inter-classes relationships where 2 classes share a domain.\n"+
    "   files...     - one or more .class files");
    }

    public static void main(String args[]) throws Exception {
        // Single value args
        String outArg = null;
        Set domains = null;

        // Parse args
        int i;
        for (i = 0; i != args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) == '-') {
                if (arg.equals("-out")) {
                    outArg = getArg(args,++i);
                }
                else if (arg.equals("-domain")) {
                    if (domains == null)
                        domains = new HashSet();
                    domains.add(new DomainSpec(getArg(args,++i)));
                } else {
                    Utility.error("Unknown option: "+arg);
                    usage();
                    System.exit(1);
                }
            }
            else
                break;
        }

        // Ensure all required args were supplied
        if (args.length == i) {
            usage();
            System.exit(1);
        }

        // The rest are .class files.
        String files[] = new String[args.length - i];
        System.arraycopy(args,i,files,0,files.length);

        GenRequests instance = new GenRequests(domains);
        instance.processFiles(files);

        PrintStream out;
        if (outArg == null)
            out = System.out;
        else
            out = new PrintStream(new FileOutputStream(outArg));

        out.println("# THIS IS A GENERATED FILE.");
        out.println("# Generated on " + new Date());
        out.println("# Command line arguments: " + Utility.toString(args," "));
        out.println();

        instance.serialize(out);
    }

    /**
     * Helper method to extract the next command line arg. Includes a bounds
     * check which results to a call to System.exit if it fails.
     * @param args The command line args.
     * @param index The index of the next arg.
     * @return the next arg.
     */
     private static String getArg(String args[], int index) {
        if (index >= args.length) {
            usage();
            System.exit(1);
        }
        return args[index];
     }
}

class DomainSpec {
    Set packages;
    Set packagesRoots;
    Set classes;

    DomainSpec(String spec) {
        packages = new HashSet();
        packagesRoots = new HashSet();
        classes = new HashSet();

        StringTokenizer st = new StringTokenizer(spec,",");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.endsWith(".**")) {
                packagesRoots.add(tok.substring(0,tok.length() - 3));
            } else
            if (tok.endsWith(".*")) {
                packages.add(tok.substring(0,tok.length() - 2));
            } else {
                classes.add(tok);
            }
        }
    }

    static boolean shareDomain(Set domains, String c1, String c2) {
        for (Iterator i = domains.iterator(); i.hasNext();) {
            DomainSpec ds = (DomainSpec)i.next();
            if (ds.contains(c1) && ds.contains(c2))
                return true;
        }
        return false;
    }

    static String getPackage(String clazz) {
        int index = clazz.lastIndexOf('.');
        if (index == -1)
            return "";
        else
            return clazz.substring(0,index);
    }

    boolean contains(String clazz) {
        boolean exists = classes.contains(clazz);
        if (exists)
            return true;
        String pkg = getPackage(clazz);
        if (packages.contains(pkg)) {
            classes.add(clazz);
            return true;
        }
        for (Iterator i = packagesRoots.iterator(); i.hasNext();) {
            String pkgRoot = (String)i.next();
            if (pkg.startsWith(pkgRoot)) {
                classes.add(clazz);
                packages.add(pkg);
                return true;
            }
        }
        return false;
    }
}
