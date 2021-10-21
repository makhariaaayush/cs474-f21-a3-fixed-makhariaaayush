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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class A2Solution implements ObjectInspector {
    String key;
    String value;


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
        fs.stream()
                .forEach(f -> f.setAccessible(true) );

        for (Field f : fs) {
            try {
                String key = describeFieldName(f);
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

    private final Set<Class<?>> primitive = Set.of(int.class,
            long.class, float.class, double.class, short.class, byte.class,
            char.class, boolean.class);

    private boolean isPrimitive(Class<?> c) {

        return primitive.contains(c);
    }

    private String describePrimitive(Object val) {
        switch (val.getClass().getName()) {
            case "java.lang.Integer":
                Stream<String> StreamI1 = Stream.of(Integer.toString((Integer) val));
                return StreamI1.reduce("", String::concat);

            case "java.lang.Long":
                Stream<String> StreamL1 = Stream.of(Long.toString((Long) val));
                Stream<String> StreamL2 = Stream.of("#L");
                Stream<String> StreamL3 = Stream.concat(StreamL1, StreamL2);
                return StreamL3.reduce("", String::concat);

            case "java.lang.Float":
                Stream<String> StreamF1 = Stream.of(Float.toString((Float) val));
                Stream<String> StreamF2 = Stream.of("#F");
                Stream<String> StreamF3 = Stream.concat(StreamF1, StreamF2);
                return StreamF3.reduce("", String::concat);
            case "java.lang.Double":
                Stream<String> StreamD1 = Stream.of(Double.toString(((Double) val)));
                Stream<String> StreamD2 = Stream.of("#D");
                Stream<String> StreamD3 = Stream.concat(StreamD1, StreamD2);
                return StreamD3.reduce("", String::concat);
            case "java.lang.Short":
                Stream<String> StreamS1 = Stream.of("0");
                Stream<String> StreamS2 = Stream.of(Integer.toOctalString((Short) val));
                Stream<String> StreamS3 = Stream.concat(StreamS1, StreamS2);
                return StreamS3.reduce("", String::concat);
            case "java.lang.Byte":
                Stream<String> StreamB1 = Stream.of("0x");
                Stream<String> StreamB2 = Stream.of(Integer.toHexString((Byte) val));
                Stream<String> StreamB3 = Stream.concat(StreamB1, StreamB2);
                return StreamB3.collect(Collectors.joining());
            case "java.lang.Character":
                Stream<String> StreamC1 = Stream.of(Character.toString((Character) val));
                return StreamC1.collect(Collectors.joining());
            case "java.lang.Boolean":
                Stream<String> StreamBo1 = Stream.of(Boolean.toString((Boolean) val));
                return StreamBo1.collect(Collectors.joining());
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
                assert f != null;
                f.setAccessible(true);
                f.set(o, e.getValue());
            } catch (ReflectiveOperationException ex) {
                throw new Error(ex);
            }
        }
    }
}
