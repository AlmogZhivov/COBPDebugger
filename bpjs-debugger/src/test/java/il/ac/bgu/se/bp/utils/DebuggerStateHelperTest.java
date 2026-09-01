package il.ac.bgu.se.bp.utils;

import org.junit.Test;
import org.mozilla.javascript.NativeObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class DebuggerStateHelperTest {

    @Test
    public void serializesCircularJavaCollectionWithoutOverflow() {
        List<Object> values = new ArrayList<>();
        values.add("visible");
        values.add(values);

        String json = DebuggerStateHelper.serializeDebuggerValue(values);

        assertTrue(json.contains("visible"));
        assertTrue(json.contains("circular:ArrayList"));
    }

    @Test
    public void serializesCircularRhinoObjectWithoutOverflow() {
        NativeObject value = new NativeObject();
        value.put("name", value, "piece");
        value.put("self", value, value);

        String json = DebuggerStateHelper.serializeDebuggerValue(value);

        assertTrue(json.contains("piece"));
        assertTrue(json.contains("circular:NativeObject"));
    }

    @Test
    public void doesNotReflectivelySerializeUnknownJavaObjects() {
        SelfReferencingValue value = new SelfReferencingValue();

        String json = DebuggerStateHelper.serializeDebuggerValue(value);

        assertTrue(json.contains("SelfReferencingValue"));
    }

    private static class SelfReferencingValue {
        @SuppressWarnings("unused")
        private final SelfReferencingValue self = this;
    }
}
