package edu.uic.cs474.f21.a3;

import java.util.Map;

public interface ObjectInspector {
    Map<String, String> describeObject(Object o);

    void updateObject(Object o, Map<String, Object> fields);

}
