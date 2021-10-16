package edu.uic.cs474.f21.a3.solution;

import edu.uic.cs474.f21.a3.ObjectInspector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class A2Solution implements ObjectInspector {
    String key;
    String value;
    Object Obj;
    Object Obj1;

    @Override
    public Map<String, String> describeObject(Object o) {
        Class<?> c;
        Set<Field> fs;
        if (o instanceof Class) {
            c = (Class<?>) o;
            fs = getAllFields(c, true);
        } else {
            c = o.getClass();
            fs = getAllFields(c, false);
        }
        Map<String, String> ret = new HashMap<>();

        for (Field f : fs) {
            try {
                String key = describeFieldName(f);
                f.setAccessible(true);
                Object valueOfTheField = f.get(o);
                String value = describeFieldValue(f.getType(), valueOfTheField);
                ret.put(key, value);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
        return ret;
    }

    private String describeFieldName(Field f) {
        int mods = f.getModifiers();
        if (Modifier.isStatic(mods)) {
            return f.getDeclaringClass().getSimpleName() + "." + f.getName();
        } else {
            return f.getName();
        }
    }

    private Set<Field> getAllFields(Class<?> c, boolean onlyStatic) {
        Set<Field> ret = new HashSet<>();

        while (c != null) {
            Field[] declaredFields = c.getDeclaredFields();
            for (Field f : declaredFields) {
                if (onlyStatic && !Modifier.isStatic(f.getModifiers()))
                    continue;
                ret.add(f);
            }
            for(Class<?> iface : c.getInterfaces()){
                ret.addAll(getAllInterfaceField(iface));
            }
            c = c.getSuperclass();

        }
        return ret;
    }
    private Set<Field> getAllInterfaceField(Class<?> iface){
        Set<Field> ret = new HashSet<>();

        for(Field f : iface.getDeclaredFields()){
            ret.add(f);
        }
        for(Class<?> i : iface.getInterfaces()){
            ret.addAll(getAllInterfaceField(i));
        }
        return ret;
    }

    private String describeFieldValue(Class<?> fieldType, Object val) {
        if (val == null) {
            return "null";
        }
        String ret = describePrimitive(val);
        if (ret != null) {
            if (isPrimitive(fieldType))
                return ret;
            else
                return "Boxed " + ret;
        }
        try {
            try{
                for(Method m : val.getClass().getDeclaredMethods()){
                    if(!"debug".equals(m.getName()))
                        continue;
                    if(!Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0){
                        return (String) m.invoke(val);
                    }
                    if(Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(val.getClass())){
                        return (String) m.invoke(null,val);
                    }
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
            return val.toString();
        } catch (Error e) {
            return "Raised error: " + e.getClass().getName();
        } catch (RuntimeException e) {
            return "Thrown exception: " + e.getClass().getName();
        }catch(Throwable e){
            return "Thrown checked exception: " +e.getClass().getName();
        }

    }

    private Set<Class<?>> primitive = Set.of(int.class, long.class, float.class, double.class, short.class, byte.class,
            char.class, boolean.class);

    private boolean isPrimitive(Class<?> c) {
        return primitive.contains(c);
    }

    private String describePrimitive(Object val) {
        switch (val.getClass().getName()) {
            case "java.lang.Integer":
                return Integer.toString((Integer) val);
            case "java.lang.Long":
                return Long.toString((Long) val) + "#L";
            case "java.lang.Float":
                return Float.toString((Float) val) + "#F";
            case "java.lang.Double":
                return Double.toString((Double) val) + "#D";
            case "java.lang.Short":
                return "0" + Integer.toOctalString((Short) val);
            case "java.lang.Byte":
                return "0x" + Integer.toHexString((Byte) val);
            case "java.lang.Character":
                return Character.toString((Character) val);
            case "java.lang.Boolean":
                return Boolean.toString((Boolean) val);
            default:
                return null;

        }
    }

    @Override
    public void updateObject(Object o, Map<String, Object> fields) {
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            try {
                Class<?> c = o.getClass();
                Field f = null;
                for (Field ff : getAllFields(c, false)) {
                    if (ff.getName().equals(e.getKey())) {
                        f = ff;
                        break;
                    }
                }
                f.setAccessible(true);
                f.set(o, e.getValue());
            } catch (ReflectiveOperationException ex) {
                throw new Error(ex);
            }
        }
    }
}
