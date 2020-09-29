package svmtools;

import java.io.*;
import java.util.*;
import java.security.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.TrustedJavaClassGen;

public class AutoWobulate {

    /* Creates a batch file for all files on the classpath */
    public static Reader processFiles(String classpath) throws IllegalArgumentException {
        StringWriter sw = new StringWriter();

        /* The template used to batch wobulate the files */
        String templates =
            "template api_final { \n" +
            "  class_resource_access_key=\"cldc_cra\"\n" +
            "  primary_domain_key=\"cldc_domain\"\n" +
            "  default_field_accessibility=\"false\"\n" +
            "  default_method_accessibility=\"false\"\n" +
            "}\n\n" +

            "template api_non_final : api_final { \n" +
            "  subclass_key=\"cldc_subclass\"\n" +
            "}\n\n";

        /* write the templates to start of stream */
        sw.write(templates);

        ClassPath cp = new ClassPath(classpath);

        /* Get all available classes on classpath */
        Enumeration e = cp.getFiles("", "class");

        /* Discover each file and check whether it is final */
        while (e.hasMoreElements()) {
            JavaClass jc;

            //ClassFile cf = cp.getFile( ( (File) e.nextElement()).getName());
            ClassFile cf = (ClassFile)e.nextElement();

            try {
                ClassParser parser = new ClassParser(cf.getInputStream(), cf.getPath());
                jc = parser.parse();
            } catch (ClassFormatError ex) {
                throw new IllegalArgumentException(ex.getMessage());
            } catch (IOException ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }

            sw.write("class " + jc.getClassName());

            /* If the class is final, it cannot have subclass key */
            if (jc.isFinal()) {
                sw.write(" : api_final {} ");
            } else {
                sw.write(" : api_non_final {} ");
            }

            sw.write("\n");
        }

        return new StringReader(sw.toString());
    }

}
