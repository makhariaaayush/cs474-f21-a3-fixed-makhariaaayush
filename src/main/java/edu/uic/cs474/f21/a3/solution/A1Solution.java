package edu.uic.cs474.f21.a3.solution;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import edu.uic.cs474.f21.a3.DynamicDispatchExplainer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class A1Solution implements DynamicDispatchExplainer {

    private boolean sameArgs(MethodDeclaration a, String ... argumentTypes){
        if(a.getParameters().size() != argumentTypes.length)
            return false;

        for(int i = 0; i< argumentTypes.length; i ++){
            if(!a.getParameters().get(i).getTypeAsString().equals(argumentTypes[i])){
                return false;

            }
        }
        return true;
    }

    private static Set<MethodInfo> methodsBelongingToJavaLangObject = Set.of(
            new MethodInfo("toString", new String[]{}),
            new MethodInfo("equals", new String[]{"java.lang.Object"}),
            new MethodInfo("wait", new String[]{}),
            new MethodInfo("notify", new String[]{}),
            new MethodInfo("notifyAll", new String[]{}),
            new MethodInfo("hashCode", new String[]{})
    );

    private void findUp(Map<String, ClassOrInterfaceDeclaration> classes, String receiverType, String methodName,Set<String> ret, String... argumentTypes)
    {
        boolean flag = false;
        ClassOrInterfaceDeclaration d = classes.get(receiverType);
        methodFound: while (d != null) {
            for (MethodDeclaration a : d.getMethodsByName(methodName)) {
                if(!sameArgs(a, argumentTypes))
                    continue;
                if ((a.isStatic() || a.isPrivate()) && flag)
                    continue;

                if (a.isAbstract())
                    continue;

                ret.add(d.getName().asString());
                break methodFound;
            }
            if (d.getExtendedTypes().isEmpty()) {
                MethodInfo mi = new MethodInfo(methodName, argumentTypes);
                if (methodsBelongingToJavaLangObject.contains(mi)) {
                    ret.add("java.lang.Object");
                }
                break;
            }
            String superName = d.getExtendedTypes().get(0).getNameAsString();
            d = classes.get(superName);
            flag = true;
        }
    }

    private Set<String> findDown(Map<String, ClassOrInterfaceDeclaration> classes, String receiverType, String methodName, String... argumentTypes){
        Set<String> ret = new HashSet<>();

        ClassOrInterfaceDeclaration subClass = classes.get(receiverType);

            for (ClassOrInterfaceDeclaration d : classes.values()) {
                if (d.getExtendedTypes().isNonEmpty() && d.getExtendedTypes(0).getNameAsString().equals(subClass.getNameAsString())) {
                    for (MethodDeclaration a : d.getMethodsByName(methodName)) {
                        if (!sameArgs(a, argumentTypes))
                            continue;
                        if ((a.isStatic() || a.isPrivate()))
                            continue;

                        if (a.isAbstract())
                            continue;

                        ret.add(d.getName().asString());
                    }


                    ret.addAll(findDown(classes, d.getNameAsString(), methodName, argumentTypes));
                }
        }
        return ret;
    }

    @Override
    public Set<String> explain(Map<String, ClassOrInterfaceDeclaration> classes, String receiverType, String methodName, String... argumentTypes) {

        Set<String> ret = new HashSet<>();
        findUp(classes,receiverType,methodName,ret,argumentTypes);

        if("java.lang.Object".equals(receiverType)) {
            nextClass: for (ClassOrInterfaceDeclaration d : classes.values()) {
                for (MethodDeclaration a : d.getMethodsByName(methodName)) {
                    if (sameArgs(a, argumentTypes)) {
                        ret.add(d.getNameAsString());
                        continue nextClass;
                    }
                }
                ret.add("java.lang.Object");
            }
        }
        else{
            ret.addAll(findDown(classes, receiverType, methodName, argumentTypes));
        }
        return ret;
    }

    private static class MethodInfo {
        final String name;
        final String[] arguments;

        public MethodInfo(String name, String[] arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public int hashCode() {
            int ret = name.hashCode();
            for (String arg : arguments) {

                ret = ret ^ arg.hashCode();
            }
            return ret;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodInfo))
                return false;
            MethodInfo mi = (MethodInfo) obj;
            if (!mi.name.equals(this.name)) {
                return false;
            }
            if (mi.arguments.length != this.arguments.length)
                return false;
            for (int i = 0; i < this.arguments.length; i++) {
                if (!mi.arguments[i].equals(this.arguments[i]))
                    return false;
            }
            return true;
        }
    }
}
