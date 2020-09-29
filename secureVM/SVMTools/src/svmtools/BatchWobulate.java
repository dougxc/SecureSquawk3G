package svmtools;

/**
 * This class provides a utility for batch wobulating a set of classes based
 * on a ASCII specification. This spec can be hand written or can be generated
 * by this class for a given set of classes that already have a Trusted
 * attribute.
 *
 * This utility is primarily for re-wobulating a class when it has been modified
 * where the wobulation is based upon a previous wobulator done via the GUI
 * Wobulator tool.
 */

import java.io.*;
import java.util.*;
import java.security.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.TrustedJavaClassGen;

public class BatchWobulate {

    /**
     * This is a simple Exception subclass that provides helpful context for
     * exceptions raised while parsing from a StreamTokenizer.
     */
    static public class ParseException extends Exception {
        ParseException(StreamTokenizer st,String message) {
            super("line: "+st.lineno()+": "+message);
        }
    }

    /**
     * This class encapsulates a Wobulation specification.
     */
    static class Spec {
        // The fields of this class are all encoded in object variables so that
        // a null test can be used to ascertain if a value was
        // provided for the field.

        /**
         * The access flags are represented as an array of the flag
         * names (as defined by the Trusted class). For example,
         * "TACC_SUBCLASS", "TACC_EXCEPTION".
         */
        String[] access_flags;

        /**
         * All the keys are represented as aliases into a keystore.
         * The alias for the primary_domain_key must correspond to a key pair
         * as the private key is required for signing.
         */
        String subclass_key;
        String class_resource_access_key;
        String primary_domain_key;

        /**
         * The default accessibility for fields and methods.
         */
        Boolean default_field;
        Boolean default_method;

        /**
         * The list of fields and methods that don't comply with the default
         * accessibility setting. The format of field and method specifiers is:
         *
         *     <name>:<type>
         *
         * where <type> uses the format of Field and Method Descriptors as
         * defined in the JVM specification.
         */
        String[] non_default_fields;
        String[] non_default_methods;

        /**
         * The permits are represented as arrays of the permit grantor's
         * names.
         */
        String[] subclass_permits;
        String[] class_resource_access_permits;

        /**
         * Verify that all non-optional fields have been given values.
         * @return the list of non-optional fields that have not been
         * assigned values.
         */
        public String[] missingFieldValues() {
            Vector v = new Vector(3);
            if (primary_domain_key == null)
                v.add("primary_domain_key");
            if (default_field == null)
                v.add("default_field_accessibility");
            if (default_method == null)
                v.add("default_method_accessibility");
            int size = v.size();
            if (size == 0)
                return null;
            String[] result = new String[size];
            return (String[])v.toArray(result);
        }

        /**
         * Convert the spec to a string.
         * @param delim The delimiter to insert between the serialized
         * fields.
         */
        public String toString(String delim) {
            String[] namesAndValues = new String[] {
                "access_flags",Utility.toString(access_flags,"\",\""),
                "subclass_key",subclass_key,
                "class_resource_access_key",class_resource_access_key,
                "primary_domain_key",primary_domain_key,
                "default_field_accessibility",default_field.toString(),
                "default_method_accessibility",default_method.toString(),
                "non_default_fields",Utility.toString(non_default_fields,"\",\""),
                "non_default_methods",Utility.toString(non_default_methods,"\",\""),
                "subclass_permits",Utility.toString(subclass_permits,"\",\""),
                "class_resource_access_permits",
                    Utility.toString(class_resource_access_permits,"\",\"")
            };
            int size = 0;
            int delimSize = (delim == null ? 0 : delim.length());
            for (int i = 0; i != namesAndValues.length; i += 2) {
                if (namesAndValues[i + 1] != null)
                    size += namesAndValues[i].length() +
                            namesAndValues[i + 1].length() + 2 +
                            delimSize;
            }
            if (size != 0) {
                StringBuffer buf = new StringBuffer(size);
                for (int i = 0; i != namesAndValues.length; i += 2) {
                    if (namesAndValues[i + 1] != null) {
                        buf.append(namesAndValues[i]);
                        buf.append("=\"");
                        buf.append(namesAndValues[i + 1]);
                        buf.append("\"");
                        if (delim != null)
                            buf.append(delim);
                    }
                }
                String result = buf.toString();
                if (delim != null && result.endsWith(delim))
                    result = result.substring(0,result.length()-delim.length());
                return result;
            } else
                return "";
        }

        public String toString() {
            return toString(",");
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Spec))
                return false;
            return toString(null).equals(((Spec)o).toString(null));
        }

        public int hashCode() {
            return toString(null).hashCode();
        }

        /**
         * Parse a value string.
         */
        private String[] parseValues(StreamTokenizer st) throws ParseException {
            try {
                Vector values = new Vector(10);
                boolean expectValue = true;
                do {
                    int ttype = st.nextToken();
                    if (ttype == '"') {
                        if (!expectValue)
                            throw new ParseException(st,"Expected ',': "+st);
                        values.add(st.sval);
                        expectValue = false;
                    }
                    else
                    if (ttype == (int)',') {
                        if (expectValue)
                            throw new ParseException(st,"Expected a value: "+st);
                        expectValue = true;
                    }
                    else {
                        if (expectValue)
                            throw new ParseException(st,"Expected a value: "+st);
                        st.pushBack();
                        break;
                    }
                } while (true);
                if (values.size() == 0)
                    throw new ParseException(st,"Expected at least one value");

                String result[] = new String[values.size()];
                values.toArray(result);
                return result;
            } catch (IOException ioe) {
                throw new ParseException(st,ioe.getMessage());
            }
        }

        /**
         * Consume a token, throwing an exception if it was not found.
         */
        private void eatToken(StreamTokenizer st, char c) throws ParseException {
            try {
                if (st.nextToken() != (int)c)
                    throw new ParseException(st,"Expected '"+c+"'");
            } catch (IOException ioe) {
                throw new ParseException(st,ioe.getMessage());
            }
        }

        /**
         * Construct from a StreamTokenizer.
         * @param opening The opening delimitor.
         * @param delim The inter-filed delimitor.
         * @param closing The closing delimitor.
         */
        public Spec(StreamTokenizer st, char opening, char closing, Spec template)
            throws ParseException
        {
            // Initialise from template first if supplied
            if (template != null) {
                access_flags = template.access_flags;
                subclass_key = template.subclass_key;
                class_resource_access_key = template.class_resource_access_key;
                primary_domain_key = template.primary_domain_key;
                default_field = template.default_field;
                default_method = template.default_method;
                non_default_fields = template.non_default_fields;
                non_default_methods = template.non_default_methods;
                subclass_permits = template.subclass_permits;
                class_resource_access_permits =
                    template.class_resource_access_permits;
            }

            try {
                eatToken(st,opening);
                Set seenNames = new HashSet();
                int token;
                while ((token = st.nextToken()) != (int)closing) {

                    // Parse the field name
                    if (token != st.TT_WORD)
                        throw new ParseException(st,"Invalid field name: "+st);
                    String name = st.sval;
                    if (seenNames.contains(name)) {
                        throw new ParseException(st,
                            "Duplicate entry for '"+name);
                    } else {
                        seenNames.add(name);
                    }


                    // Parse the field value(s)
                    eatToken(st,'=');
                    String[] values = parseValues(st);

                    // Process the field entry
                    if (name.equals("access_flags")) {
                        access_flags = values;
                    }
                    else if (name.equals("subclass_key")) {
                        subclass_key = values[0];
                    }
                    else if (name.equals("class_resource_access_key")) {
                        class_resource_access_key = values[0];
                    }
                    else if (name.equals("primary_domain_key")) {
                        primary_domain_key = values[0];
                    }
                    else if (name.equals("default_field_accessibility")) {
                        default_field = Boolean.valueOf(values[0]);
                    }
                    else if (name.equals("default_method_accessibility")) {
                        default_method = Boolean.valueOf(values[0]);
                    }
                    else if (name.equals("non_default_fields")) {
                        non_default_fields = values;
                    }
                    else if (name.equals("non_default_methods")) {
                        non_default_methods = values;
                    }
                    else if (name.equals("subclass_permits")) {
                        subclass_permits = values;
                    }
                    else if (name.equals("class_resource_access_permits")) {
                        class_resource_access_permits = values;
                    }
                    else {
                        throw new ParseException(st,"Unknown field: "+name);
                    }
                }
            }
            catch (IOException ioe) {
                throw new ParseException(st,ioe.getMessage());
            }
            String[] missing = missingFieldValues();
            if (missing != null) {
                throw new ParseException(st,
                    "Missing values for these required field(s): "+
                    Utility.toString(missing,", "));
            }
        }

        /**
         * Retrieve the alias for a given key.
         * @param keyData The key data.
         * @param km The keystore manager.
         * @throwss NoSuchElementException if keyData != null and does
         * not have an entry in km.
         */
        private String keyToAlias(byte[] keyData, KeyManager km) {
            if (keyData == null)
                return null;
            String alias = km.getAlias(keyData);
            if (alias != null)
                return alias;
            throw new NoSuchElementException();
        }

        /**
         * Convert member indexes to their encodings.
         * @param indexes The member indexes.
         * @param members The table of members.
         */
        private String[] memberIndexesToEncodings(int[] indexes,
            FieldOrMethod[] members)
        {
            if (indexes == null || indexes.length == 0)
                return null;
            String[] result = new String[indexes.length];
            for (int i = 0; i != indexes.length; i++) {
                FieldOrMethod m = members[indexes[i]];
                result[i] = m.getName()+" "+m.getSignature();
            }
            return result;
        }

        /**
         * Convert member encodings to their indexes.
         * @param encodings The member encodings.
         * @param members The table of members.
         */
        private int[] memberEncodingsToIndexes(String[] encodings,
            FieldOrMethod[] members)
        {
            if (encodings == null || encodings.length == 0)
                return null;
            String type;
            if (members instanceof Field[])
                type = "field";
            else
                type = "method";

            int[] result = new int[encodings.length];

nextEncoding:
            for (int i = 0; i != encodings.length; i++) {
                StringTokenizer encoding = new StringTokenizer(encodings[i]," ");
                String name = null, sig = null;
                try {
                    name = encoding.nextToken();
                    sig = encoding.nextToken();
                } catch (NoSuchElementException nsee) {
                    throw new IllegalArgumentException("Bad "+type+
                        " encoding: "+encodings[i]);
                }
                // Search sequentially through the members table. This is slow
                // but what can you do...
                for (int j = 0; j != members.length; j++) {
                    FieldOrMethod m = members[j];
                    if (m.getName().equals(name) &&
                        m.getSignature().equals(sig)) {
                        result[i] = j;
                        continue nextEncoding;
                    }
                }
                // Member not found - throw exception
                throw new IllegalArgumentException("No " + type +
                    " with encoding '"+encodings[i]+"'");
            }
            return result;
        }

        /**
         * Convert an array of permits to an array of the names of the
         * permits grantors.
         */
        private String[] permitsToStringArray(Trusted.Permit[] permits,
            ConstantPool cp, byte type) {
            if (permits == null || permits.length == 0)
                return null;
            String[] result = new String[permits.length];
            for (int i = 0; i != permits.length; i++) {
                int index = permits[i].getClassIndex();
                result[i] = cp.constantToString(index,type);
            }
            return result;
        }

        /**
         * Construct from a Trusted class.
         */
        public Spec(JavaClass jc, KeyManager km) {
            Trusted tc = jc.getTrustedAttribute();
            if (tc == null)
                throw new IllegalArgumentException(jc.getClassName() +
                    " must have a Trusted attribute");

            // Retrieve the access flags
            access_flags = Trusted.flagsToStringArray(tc.getAccessFlags());

            // Retrieve the key aliases
            String keyName = "Subclass";
            try {
                subclass_key = keyToAlias(tc.getSubclassKeyBytes(),km);
                keyName = "Class resource access";
                class_resource_access_key =
                    keyToAlias(tc.getClassResourceAccessKeyBytes(),km);
                keyName = "Primary domain";
                primary_domain_key = keyToAlias(tc.getDomains()[0].getKey(),km);
            } catch (NoSuchElementException nsee) {
                throw new IllegalArgumentException(keyName +
                    " key has no entry in the given keystore");
            }

            // Retrieve the default member accessibility
            default_field = new Boolean(tc.getDefaultFieldAccessibility());
            default_method = new Boolean(tc.getDefaultMethodAccessibility());

            // Retrieve the non-default members
            non_default_fields = memberIndexesToEncodings(
                tc.getFieldsAccessibility(),jc.getFields());
            non_default_methods = memberIndexesToEncodings(
                tc.getMethodsAccessibility(),jc.getMethods());

            // Retrieve subclass permits
            Trusted.Permit permits[] = tc.getSubclassPermits();
            if (permits != null && permits.length != 0) {
                String interfaces[] = jc.getInterfaceNames();
                subclass_permits = new String[permits.length];
                for (int i = 0; i != permits.length; i++) {
                    int index = permits[i].getClassIndex();
                    if (index == interfaces.length)
                        subclass_permits[i] = jc.getSuperclassName();
                    else
                        subclass_permits[i] = interfaces[index];
                }
            }

            // Retrieve class resource access permits
            String[] noReflectionCRA = permitsToStringArray(
                tc.getClassResourceAccessPermits(),
                jc.getConstantPool(),
                Constants.CONSTANT_Class);
            String[] reflectionCRA = permitsToStringArray(
                tc.getRefClassResourceAccessPermits(),
                tc.getTrustedConstantPool(),
                Constants.CONSTANT_Utf8);

            if (noReflectionCRA != null || reflectionCRA != null) {
                int length = (noReflectionCRA != null?noReflectionCRA.length:0)+
                             (reflectionCRA != null ? reflectionCRA.length:0);
                class_resource_access_permits = new String[length];
                int copied = 0;
                if (noReflectionCRA != null) {
                    copied = noReflectionCRA.length;
                    System.arraycopy(noReflectionCRA,0,
                        class_resource_access_permits,
                        0,length);
                }
                if (reflectionCRA != null) {
                    System.arraycopy(reflectionCRA,0,
                        class_resource_access_permits,
                        copied,reflectionCRA.length);
                }
            }
        }

        /**
         * Find the key with a given alias in a given keystore and return
         * its CSP encoding.
         */
        private byte[] getAndEncodeKey(String alias, KeyManager km,
            CSP csp) {
            KeyPair kp = km.getKeyPair(alias);
            if (kp == null || kp.getPublic() == null)
                throw new IllegalArgumentException("Alias '"+alias+
                    "' not found in keystore");
            try {
                return csp.encodePublicKey(kp.getPublic());
            } catch (CSPException cspe) {
                throw new IllegalArgumentException(
                    "Error while encoding public key '"+alias+"': "+
                        cspe.getMessage());
            }
        }

        private void retrieveAndApplyPermits(
            String[] grantors, Hashtable permits, boolean isSubclass,
            TrustedJavaClassGen gen, String className)
        {
            if (grantors == null)
                return;
            String type = (isSubclass ?
                PermitManager.Permit.SUBCLASS :
                PermitManager.Permit.CLASS_RESOURCE_ACCESS);
            String fieldName = (isSubclass ?
                "subclass_permits" : "class_resource_access_permits");
            for (int i = 0; i != grantors.length; i++) {
                String grantor = grantors[i];
                PermitManager.Permit permit = null;
                if (permits == null ||
                    (permit = (PermitManager.Permit)
                     permits.get(grantor+className+type)) == null)
                {
                    throw new IllegalArgumentException(
                        "."+fieldName+": permit granted by '"+
                        grantor+"' not found in permit database");
                }
                if (isSubclass)
                    gen.setSubclassPermit(grantor,permit.getSignature());
                else
                    gen.setClassResourceAccessPermit(grantor,
                        permit.getSignature(),false);
            }
        }

        /**
         * Wobulate a class based on this spec and return the resulting
         * JavaClass.
         * @param jc The un-wobulated JavaClass
         * @param csp The CSP to use.
         * @param km The KeyManager to use.
         * @param pm The PermitManager to use.
         */
        public JavaClass applyToClass(JavaClass jc, CSP csp,
            KeyManager km, PermitManager pm) {
            TrustedJavaClassGen gen = new TrustedJavaClassGen(jc,
                csp.getIdentifier());
            String className = jc.getClassName();

            if (access_flags != null) {
                int flags = 0;
                for (int i = 0; i != access_flags.length; i++)
                try {
                    flags |= Trusted.stringToFlag(access_flags[i]);
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException(
                        ".access_flags: unknown flag '"+
                        access_flags[i]+"'");
                }
                gen.setClassAccessFlags(flags);
            }

            if (subclass_key != null) {
                try {
                    gen.setSubclassKey(getAndEncodeKey(subclass_key,km,csp));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        ".subclass_key: "+e.getMessage());
                }
            }
            if (class_resource_access_key != null) {
                try {
                    gen.setClassResourceAccessKey(
                        getAndEncodeKey(class_resource_access_key,km,csp));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        ".class_resource_access_key: "+e.getMessage());
                }
            }

            gen.setDefaultFieldAccessibility(default_field.booleanValue());
            gen.setDefaultMethodAccessibility(default_method.booleanValue());

            if (non_default_fields != null) {
                try {
                    int[] indexes = memberEncodingsToIndexes(
                        non_default_fields,jc.getFields());
                    for (int i = 0; i != indexes.length; i++)
                        gen.addNonDefaultMember(indexes[i],true);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        ".non_default_fields: "+e.getMessage());
                }
            }
            if (non_default_methods != null) {
                try {
                    int[] indexes = memberEncodingsToIndexes(
                        non_default_methods,jc.getMethods());
                    for (int i = 0; i != indexes.length; i++)
                        gen.addNonDefaultMember(indexes[i],false);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        ".non_default_methods: "+e.getMessage());
                }
            }
            Hashtable permits = pm.getPermitsByGrantee(className);
            retrieveAndApplyPermits(subclass_permits,permits,true,
                gen,className);
            retrieveAndApplyPermits(class_resource_access_permits,
                permits,false,gen,className);

            try {
                KeyPair kp = km.getKeyPair(primary_domain_key);
                if (kp == null || kp.getPrivate() == null ||
                    kp.getPublic() == null)
                    throw new IllegalArgumentException(
                        ".primary_domain_key: Alias '"+
                        primary_domain_key+"' not found or does not correspond to a complete key pair");
                byte[] part1 = gen.getSubclassPermitInput();
                byte[] part2 = gen.getDomainsSignatureInput();
                byte[] whole = new byte[part1.length+part2.length];
                System.arraycopy(part1,0,whole,0,part1.length);
                System.arraycopy(part2,0,whole,part1.length,part2.length);
                byte[] sig = csp.sign(whole,kp.getPrivate());
                return gen.getTrustedClass(
                    new byte[][] { csp.encodePublicKey(kp.getPublic()) },
                    new byte[][] { sig },
                    true);
            } catch (VerifyError ve) {
                throw new IllegalArgumentException(ve.getMessage());
            } catch (CSPException cspe) {
                throw new IllegalArgumentException(cspe.getMessage());
            }
        }
    }

    /**
     * Process one or more classes that already have Trusted attributes and
     * generate a batch spec. Any of the given classes that don't have a
     * Trusted attribute are ignored.
     */
    public static void createBatchFile(PrintStream out, JavaClass classes[],
            KeyManager km) {

        // Map of Spec -> Vector<JavaClass>
        Hashtable specs = new Hashtable(classes.length);

        for (int i = 0; i != classes.length; i++) {
            JavaClass jc = classes[i];

            if (jc.getTrustedAttribute() == null)
                continue;

            Spec spec = new Spec(jc,km);
            Vector classNames = (Vector)specs.get(spec);
            if (classNames == null) {
                classNames = new Vector(10);
                specs.put(spec,classNames);
            }
            classNames.add(jc.getClassName());
        }
        int templateNo = 0;

        out.println("# THIS IS A GENERATED FILE.");
        out.println("# Generated on " + new Date());
        out.println();

        for (Iterator i = specs.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry)i.next();
            Spec spec = (Spec)e.getKey();
            Vector classNames = (Vector)e.getValue();

            if (classNames.size() == 1) {
                String className = (String)classNames.elementAt(0);
                out.println("# Stand alone class '" + className + "'");
                out.println("class " + className + " {");
                out.println("  " + spec.toString("\n  "));
                out.println("}\n");
            } else {
                String templateName = "spec_" + templateNo;
                templateNo++;
                out.println("# Template '" + templateName + "' and its classes");
                out.println("template " + templateName + " {");
                out.println("  " + spec.toString("\n  "));
                out.println("}");
                for (Iterator j = classNames.iterator(); j.hasNext();) {
                    String className = (String)j.next();
                    out.println("class " + className + " : " +
                        templateName + " {}");
                }
                out.println();
            }
        }
    }

    /**
     * Process a set of classes according to a batch file.
     * @throws IllegalArgumentException if there is any error in the spec
     * or during the wobulation.
     */
    public static JavaClass[] processBatchFile(Reader file, String classpath,
        CSP csp, PermitManager pm, KeyManager km)
            throws IllegalArgumentException
    {
        StreamTokenizer st = new StreamTokenizer(file);
        st.resetSyntax();
        st.whitespaceChars(0, ' ');
        st.eolIsSignificant(false);
        st.lowerCaseMode(false);
        st.wordChars('A','Z');
        st.wordChars('a','z');
        st.wordChars('0','9');
        st.commentChar((int)'#');
        st.quoteChar('"');
        char wordChars[] = {'<','>','_','.','[','(',')','/','$'};
        for(int i = 0; i != wordChars.length; i++) {
            st.wordChars(wordChars[i],wordChars[i]);
        }

        Hashtable templates = new Hashtable();
        Hashtable classes = new Hashtable();

        int token;
        try {
        while ((token = st.nextToken()) != st.TT_EOF) {

            boolean isTemplateDefn;
            Spec spec = null;

            // Parse "class" or "template"
            if (token != st.TT_WORD ||
               !(st.sval.equals("class") || st.sval.equals("template")))
                throw new ParseException(st,"Expected 'class' or 'template'");
            isTemplateDefn = st.sval.equals("template");
            if (isTemplateDefn) {
                if (templates.contains(st.sval))
                    throw new ParseException(st,"Re-declaring template '"+
                        st.sval+"'");
            } else {
                if (classes.contains(st.sval))
                    throw new ParseException(st,"Re-declaring class '"+
                        st.sval+"'");
            }

            // Parse class or template name
            if (st.nextToken() != st.TT_WORD)
                throw new ParseException(st,"Expected class or template name");
            String name = st.sval;

            // Parse optional ':' <template> clause
            token = st.nextToken();
            if (token == (int)':') {
                token = st.nextToken();
                if (token != st.TT_WORD)
                    throw new ParseException(st,"Expected template name");
                String templateName = st.sval;
                spec = (Spec)templates.get(templateName);
                if (spec == null)
                    throw new ParseException(st,"Using undefined template '"+
                        templateName+"'");
            } else
                st.pushBack();

            // Parse spec
            spec = new Spec(st,'{','}',spec);

            if (isTemplateDefn) {
                templates.put(name,spec);
            } else {
                classes.put(name,spec);
            }
        }
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe.toString());
        } catch (ParseException pe) {
            throw new IllegalArgumentException(pe.getMessage());
        }


        // Wobulate classes according to specs...
        ClassPath cp = new ClassPath(classpath);
        JavaClass[] result = new JavaClass[classes.size()];
        int nextClassIndex = 0;
        for (Iterator i = classes.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry)i.next();
            String className = (String)e.getKey();
            Spec spec = (Spec)e.getValue();

            String fileName = className.replace('.',File.separatorChar) +
                ".class";
            ClassFile cf = cp.getFile(fileName);
            if (cf == null) {
                System.err.println("Warning: cannot find class " + className + " - skipping");
                continue;
            }
            JavaClass jc = null;
            try {
                ClassParser parser = new ClassParser(cf.getInputStream(),cf.getPath());
                jc = parser.parse();
            } catch (IOException ioe) {
                throw new IllegalArgumentException("Problem parsing class " +
                    className+": "+ioe.getMessage());
            }

            if (jc.getTrustedAttribute() != null)
                throw new IllegalArgumentException(className +
                    " has an existing Trusted attribute");

            try {
                jc = spec.applyToClass(jc,csp,km,pm);
                result[nextClassIndex++] = jc;
            } catch (IllegalArgumentException ex) {
                String prefix = "While processing " + className;
                String msg = ex.getMessage();
                String spacer = (msg != null && msg.startsWith(".") ?
                    "" : ": ");
                throw new IllegalArgumentException(prefix+spacer+msg);
            }
        }
        return result;

    }
}


