package svmtools;

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * This is the command line tool for manipulating a permit database in much
 * the same way that the standard java keytool is a command line tool for
 * manipulating a keystore.
 */

public class PermitTool {

    PermitTool() {
    }

    /**
     * These are the command line arguments.
     */
    String command;
    String cspArg = "svmtools.csp.MD5RSABasic";
    String dbArg = "permits.pdb";
    String ksArg;
    String cpArg;
    String storepassArg;
    boolean verboseArg;
    boolean preserveArg;
    Vector specfileArgs = new Vector(10);
    Vector sigspecArgs = new Vector(10);



    /**
     * Entry point for using the tool. See the usage method for further details.
     */
    static public void main(String args[]) {
        PermitTool instance = new PermitTool();
        instance.run(args);
    }

    void run(String[] args) {
        parseArgs(args);
        if (command.equals("sign"))
            doSign();
        else if (command.equals("list"))
            doList();
        else {
            Utility.error("Unknown command: "+command,true);
        }
    }

    void parseArgs(String[] args) {
        // Retrieve default value for keystore
        try {
            ksArg = System.getProperty("user.home") + File.separator + ".keystore";
        } catch (SecurityException se) {
        }
        // Parse args
        for (int i =0; i != args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) == '-') {
                // Command modes
                if (arg.equals("-list")) {
                    command = "list";
                } else if (arg.equals("-sign")) {
                    command = "sign";
                }

                // Command options
                else if (arg.equals("-csp")) {
                    cspArg = getArg(args,++i);
                } else if (arg.equals("-db")) {
                    dbArg = getArg(args,++i);
                } else if (arg.equals("-cp")) {
                    cpArg = getArg(args,++i);
                } else if (arg.equals("-keystore")) {
                    ksArg = getArg(args,++i);
                } else if (arg.equals("-storepass")) {
                    storepassArg = getArg(args,++i);
                } else if (arg.equals("-preserve")) {
                    preserveArg = true;
                } else if (arg.equals("-v")) {
                    verboseArg = true;
                } else {
                    Utility.error("Unknown option: "+arg);
                    usage();
                    System.exit(1);
                }
            }
            else {
                // It's a <sigspec> or a <specfile>
                if (arg.startsWith("@"))
                    specfileArgs.add(arg.substring(1));
                else
                    sigspecArgs.add(arg);
            }
        }
        if (command == null) {
            Utility.error("A command must be given.");
            usage();
            System.exit(1);
        }
    }

    void doList() {
        if (dbArg == null)
            Utility.error("-db arg required for list command.",true);

        PermitManager pm = new PermitManager();
        File dbFile = openDBFile(dbArg,pm);
        for (Enumeration e = pm.permits.keys(); e.hasMoreElements();) {
            String key = (String)e.nextElement();
            PermitManager.Permit p = (PermitManager.Permit)pm.permits.get(key);
            System.out.println(p.toString(true));
        }
    }

    void doSign() {

        // Ensure required args were supplied
        if (ksArg == null ||
           (sigspecArgs.size() == 0 && specfileArgs.size() == 0)) {
            usage();
            System.exit(1);
        }

        // Create ClassPath
        ClassPath cp;
        if (cpArg != null)
            cp = new ClassPath(cpArg);
        else
            cp = new ClassPath();

        // Retrieve the specified CSP
        CSP csp = CSP.getInstance(cspArg);
        if (csp == null)
            Utility.error("CSP class ("+cspArg+") not found.",true);

        // Open and load the specified keystore
        char storepass[];
        if (storepassArg == null) {
            System.err.print("Enter keystore password:  ");
            System.err.flush();
            storepass = null;
            try {
    	        storepass = Utility.readPasswd(System.in);
            } catch (IOException ioe) {
                System.err.println("Error while reading password.");
                ioe.printStackTrace(System.err);
            }
            if (storepass == null )
                Utility.error("A keystore password is required.",true);
        }
        else
            storepass = storepassArg.toCharArray();

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("JKS");
            InputStream ksStream = new FileInputStream(ksArg);
            ks.load(ksStream,storepass);
        } catch (KeyStoreException kse) {
            Utility.error("Error while loading keystore from "+ksArg+": "+
                kse.getMessage(),true);
        } catch (CertificateException kse) {
            Utility.error("Error while loading keystore from "+ksArg+": "+
                kse.getMessage(),true);
        } catch (NoSuchAlgorithmException kse) {
            Utility.error("Error while loading keystore from "+ksArg+": "+
                kse.getMessage(),true);
        } catch (IOException ioe) {
            Utility.error("Error while loading keystore from "+ksArg+": "+
                ioe.getMessage(),true);
        }

        PermitManager instance = new PermitManager();
        File dbFile = openDBFile(dbArg,instance);

        Vector allSpecs = (Vector)sigspecArgs.clone();
        // Firstly expand the spec files.
        for (Enumeration e = specfileArgs.elements(); e.hasMoreElements(); ) {
            String specfile = (String)e.nextElement();
            try {
                FileInputStream fis = new FileInputStream(specfile);
                BufferedReader lines = new BufferedReader(new InputStreamReader(fis));
                String line = lines.readLine();
                while(line != null) {
                    // ignore comment and blank lines
                    String trimmed = line.trim();
                    if (trimmed.length() > 0 && !(trimmed.startsWith("#")))
                        allSpecs.add(line);
                    line = lines.readLine();
                }
                fis.close();
            } catch (IOException ioe) {
                Utility.error("error processing sigspec file " + specfile);
                ioe.printStackTrace(System.err);
                System.exit(1);
            }
        }

        // This is a cache of alias -> key mappings that prevents the user
        // from having to re-enter the password for an alias used multiple times.
        Hashtable aliasToKeys = new Hashtable(23);

nextSpec:
        for (Enumeration e = allSpecs.elements(); e.hasMoreElements();) {
            String spec = (String)e.nextElement();
            StringTokenizer st = new StringTokenizer(spec,"=,",true);
            // The number of tokens (plus 1) must be a factor of 4
            // (i.e. name, "=", value, ",")
            if ((st.countTokens() + 1) % 4 != 0) {
                Utility.error("Ignoring malformed sig spec: "+spec);
                continue;
            }
            Hashtable nameValues = new Hashtable();
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                if (!(st.nextToken().equals("="))) {
                    Utility.error("Ignoring malformed sig spec: "+spec);
                    continue nextSpec;
                }
                String value = st.nextToken();
                // if this is the last name/value pair, there will be no ","
                if (st.hasMoreTokens() && !(st.nextToken().equals(","))) {
                    Utility.error("Ignoring malformed sig spec: "+spec);
                    continue nextSpec;
                }

                Object oldName = nameValues.put(name,value);
                if (oldName != null) {
                    Utility.error("Ignoring sig spec with duplicate '"+oldName+"' attribute: "+
                        spec);
                    continue nextSpec;
                }

            }

            String grantor = null;
            String grantee = null;
            String type = null;
            String alias = null;
            char keyPass[] = null;
            // Process name/value pairs
            for (Enumeration nv = nameValues.keys(); nv.hasMoreElements();) {
                String name = (String)nv.nextElement();
                String value = (String)nameValues.get(name);
                if (name.equals("grantor"))
                    grantor = value;
                else if (name.equals("grantee"))
                    grantee = value;
                else if (name.equals("type")) {
                    if (value.startsWith("s"))
                        type = PermitManager.Permit.SUBCLASS;
                    else if (value.startsWith("c"))
                        type = PermitManager.Permit.CLASS_RESOURCE_ACCESS;
                    else {
                        Utility.error("Ignoring sig spec with unknown privilege type: "+
                            spec);
                        continue nextSpec;
                    }
                }
                else if (name.equals("key"))
                    alias = value;
                else if (name.equals("passwd"))
                    keyPass = value.toCharArray();
                else {
                    Utility.error("Ignoring sig spec with unknown attribute '"+name+"': "+
                    spec);
                    continue nextSpec;
                }
            }
            if (grantor == null || grantee == null ||
                type == null || alias == null) {
                Utility.error("Ignoring sig spec with a missing required value: "+spec);
                continue nextSpec;
            }

            PrivateKey key = (PrivateKey)aliasToKeys.get(alias);
            if (key == null) {
                if (keyPass == null) {
                    System.err.print("Enter password for alias '"+alias+"' ");
                    System.err.print("or press return to use keystore password: ");
                    System.err.flush();
                    try {
                        keyPass = Utility.readPasswd(System.in);
                    } catch (IOException ioe) {
                        Utility.error("Error while reading password.");
                        ioe.printStackTrace(System.err);
                    }
                    if (keyPass == null)
                        keyPass = storepass;
                }

                try {
                    if (ks.containsAlias(alias) == false)
                        Utility.error("Alias '" + alias + "' does not exist",true);
                    if (ks.isKeyEntry(alias) == false)
                        Utility.error("Alias '" + alias + "' has no (private) key",true);
                    key = (PrivateKey)ks.getKey(alias, keyPass);
	        } catch (UnrecoverableKeyException uke) {
                    Utility.error("Invalid password.",true);
                } catch (KeyStoreException kse) {
                    // Will never happen
                } catch (NoSuchAlgorithmException nse) {
                    // Will never happen
                }

                // Ensure the key is compatible with the specified CSP
                if (!csp.isKeyTypeSupported(key,false)) {
                    Utility.error("Private key <" + alias + "> is not compatible with the "+
                        "given CSP");
                    continue nextSpec;
                }
                aliasToKeys.put(alias,key);
            }

            // Create the permit including its signature and add it to the
            // database.
            PermitManager.Permit p = new PermitManager.Permit(grantor,grantee,type,null);
            if (instance.permits.containsKey(p.asKey())) {
                if (preserveArg != true)
                    Utility.error("Overwriting existing permit: "+p.toString());
                else
                    continue;
            }


            try {
                if (verboseArg)
                    System.err.println("Signing "+grantee);
                instance.servicePermitRequest(p,csp,key,true,cp);
            } catch (CSPException cspe) {
                Utility.error("Error while creating permit ("+p+"): "+cspe.getMessage());
            }
            catch (IOException ioe) {
                Utility.error("Error while creating permit ("+p+"): "+ioe.getMessage());
            }
        }

        // Now write out the database to the given file
        try {
            // Serialize to byte array first - only write to file if
            // there was no problem with serialization
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            instance.serialize(dos);
            FileOutputStream fos = new FileOutputStream(dbFile);
            fos.write(baos.toByteArray());
            fos.close();
        } catch (IOException ioe) {
            Utility.error("Error while saving database: "+ ioe.getMessage());
            ioe.printStackTrace(System.err);
            System.exit(1);
        }
        System.out.println(instance.permits.size()+" permits written to database ("+
            dbFile.getAbsolutePath()+")");

    }

    /**
     * Print the usage message on stderr.
     */
    static private void usage() {
    System.err.println(
    "Usage:\n"+
    "PermitTool <command> [ -csp <csp> ] [ -db <dbfile> ] [ -keystore <ksfile> ]\n"+
    "              [ -storepass <passwd> ] [ -cp <classpath> ] [ -v ] [ -preserve ]\n"+
    "              [ @<reqfile> | <req> ]+\n"+
    "where:\n"+
    "   <command> - '-sign' or '-list'.\n"+
    "   <csp>     - the CSP class to use (default: svmtools.csp.MD5RSABasic)\n"+
    "   <dbfile>  - the file to which the database of generated permits\n"+
    "               should be written. If this file already exists, then\n"+
    "               the PermitTool initialises the database with the\n"+
    "               entries already in the file. However, generated permits\n"+
    "               always overwrite existing ones. (default: permits.pdb)\n"+
    "   <ksfile>  - the JKS keystore providing the keys for signing.\n"+
    "               (default: {user.home}/.keystore)\n"+
    "   <passwd>  - the password for the keystore. The user will be prompted\n"+
    "               for this password if it is not given here.\n"+
    "   <classpath> - search path for classes (default: CLASSPATH)\n"+
    "   -v        - verbose.\n"+
    "   -preserve - don't overwrite permits already existing in the given database.\n"+
    "   <reqfile> - a file containing one or more lines, each line containing\n"+
    "               a <req>.\n"+
    "   <req>     - a set of comma separated name/value pairs specifying a\n"+
    "               permit request:\n"+
    "               \n"+
    "   Name    |Required| Description\n"+
    "   --------+--------+------------------------------------------------\n"+
    "   grantor | yes    | The class name of the privilege granting class.\n"+
    "   grantee | yes    | The class to which the privilege is being granted.\n"+
    "                    | This class must be locatable on the CLASSPATH.\n"+
    "   type    | yes    | Specifys the type of privilege being granted. Must\n"+
    "                    | be \"s\" (for subclass) or \"c\" (for class resource access).\n"+
    "   key     | yes    | The alias of a key in the given keystore that is\n"+
    "                    | to be used to generate the signature.\n"+
    "   passwd  | no     | The password of this key. If not provided, the\n"+
    "                    | user will be prompted.\n"+
    "\n"+
    "(Note that at least one @<reqfile> or <req> must be given)\n"+
    "Classes are searched for from the current directory and then on the CLASSPATH.");
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

     File openDBFile(String dbArg, PermitManager pm) {
        File dbFile = new File(dbArg);
        if (dbFile.isFile() && dbFile.canRead() && dbFile.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(dbArg);
                DataInputStream in = new DataInputStream(fis);
                pm.load(in,true);
            }
            catch (IOException ioe) {
                Utility.error("Error parsing database file "+dbArg);
                ioe.printStackTrace(System.err);
                System.exit(1);
            }
        }
        return dbFile;
     }
}