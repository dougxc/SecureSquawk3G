package svmtools.gui;

import java.util.Set;
import java.util.TreeSet;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Trusted;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import svmtools.*;

public class MethodsPanel extends MembersPanel {

    public static String methodToString(Method m) {
        StringBuffer buf = new StringBuffer(50);
        if (m.isStatic())
            buf.append("static ");
        buf.append(m.getName());
        buf.append("(");
        String params[] = Utility.methodSignatureArgumentTypes(m.getSignature());
        for (int j = 0; j != params.length; ++j) {
            buf.append(params[j]);
            if (j != (params.length - 1))
                buf.append(",");
        }
        buf.append(")");
        return buf.toString();
    }
    protected String memberToName(FieldOrMethod m) {
        return methodToString((Method)m);
    }

    public MethodsPanel(LoadedClass clazz) {
        super(clazz,"method");
    }

    protected FieldOrMethod[] getMembers() {
        return model.getOriginalClass().getMethods();
    }
    protected boolean getDefaultSetting(Trusted t) {
        return t.default_method_accessibility;
    }
    protected int[] getNonDefaultMembers(Trusted t) {
        return t.non_default_methods;
    }
    protected void setDefaultMemberAccessibility(boolean b) {
        model.setDefaultMethodAccessibility(b);
    }
    protected void clearNonDefaultMembers() {
        model.clearNonDefaultMembers(false);
    }
    protected void addNonDefaultMember(int m) {
        model.addNonDefaultMember(m,false);
    }
}
