package il.ac.bgu.se.bp.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.JsEventSet;
import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.debugger.engine.SyncSnapshotHolder;
import il.ac.bgu.se.bp.socket.state.*;
import il.ac.bgu.se.bp.utils.logger.Logger;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.debugger.Dim;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSets.none;
import static il.ac.bgu.se.bp.utils.Common.NO_MORE_WAIT_EXTERNAL;


public class DebuggerStateHelper {
    private static final Logger logger = new Logger(DebuggerStateHelper.class);
    private Set<Pair<String, Object>> recentlyRegisteredBT = null;
    private HashMap<String, Object> newBTInterpreterFrames = new HashMap<>();
    private BPDebuggerState lastState = null;
    private String currentRunningBT = null;
    private SyncSnapshotHolder<BProgramSyncSnapshot, BEvent> syncSnapshotHolder;
    private String currentEvent = null;
    private static final int INITIAL_INDEX_FOR_EVENTS_HISTORY_ON_SYNC_STATE = 0;
    private static final int FINAL_INDEX_FOR_EVENTS_HISTORY_ON_SYNC_STATE = 10;
    private static final int MAX_VARIABLE_DEPTH = 8;
    private static final int MAX_VARIABLE_ENTRIES = 100;
    private static final int MAX_VARIABLE_STRING_LENGTH = 2000;
    private BPJsDebugger bpJsDebugger;
    private DebuggerLevel debuggerLevel;

    public DebuggerStateHelper(BPJsDebugger bpJsDebugger, SyncSnapshotHolder<BProgramSyncSnapshot, BEvent> syncSnapshotHolder, DebuggerLevel debuggerLevel) {
        this.bpJsDebugger = bpJsDebugger;
        this.syncSnapshotHolder = syncSnapshotHolder;
        this.debuggerLevel = debuggerLevel;
    }

    public BPDebuggerState generateDebuggerState(BProgramSyncSnapshot syncSnapshot, RunnerState state, Dim.ContextData lastContextData, Dim.SourceInfo sourceInfo) {
        lastState = generateDebuggerStateInner(syncSnapshot, state, lastContextData, sourceInfo);
        return lastState;
    }

    public void cleanFields() {
        recentlyRegisteredBT = null;
        newBTInterpreterFrames = null;
        currentRunningBT = null;
        currentEvent = null;
    }

    public boolean[] getBreakpoints(Dim.SourceInfo sourceInfo) {
        if (sourceInfo == null) {
            return new boolean[0];
        }
        try {
            boolean[] breakpoints = getValue(sourceInfo, "breakpoints");
            return breakpoints;
        } catch (Exception e) {
            logger.error("failed to get breakpoints, e: {0}", e, e.getMessage());
        }
        return new boolean[0];
    }

    private BPDebuggerState generateDebuggerStateInner(BProgramSyncSnapshot syncSnapshot, RunnerState state, Dim.ContextData lastContextData, Dim.SourceInfo sourceInfo) {
        SortedMap<Long, EventInfo> eventsHistory = generateEventsHistory(INITIAL_INDEX_FOR_EVENTS_HISTORY_ON_SYNC_STATE, FINAL_INDEX_FOR_EVENTS_HISTORY_ON_SYNC_STATE);
        DebuggerConfigs debuggerConfigs = generateDebuggerConfigs(bpJsDebugger);

        Map<String, String> globalEnv = getGlobalEnv(syncSnapshot);
        if (debuggerLevel.getLevel() > DebuggerLevel.LIGHT.getLevel()) {
            List<BThreadInfo> bThreadInfoList = generateBThreadInfos(syncSnapshot, state, lastContextData);
            EventsStatus eventsStatus = generateEventsStatus(syncSnapshot, state);
            Integer lineNumber = lastContextData == null ? null : lastContextData.frameCount() > 0 ? lastContextData.getFrame(0).getLineNumber() : null;
            boolean[] breakpoints = getBreakpoints(sourceInfo);
            return new BPDebuggerState(bThreadInfoList, eventsStatus, eventsHistory, currentRunningBT, lineNumber, debuggerConfigs, ArrayUtils.toObject(breakpoints), globalEnv);
        }
        return new BPDebuggerState(new LinkedList<>(), new EventsStatus(), eventsHistory, currentRunningBT, null, debuggerConfigs, new Boolean[0], globalEnv);
    }

    private Map<String, String> getGlobalEnv(BProgramSyncSnapshot syncSnapshot) {
        Map<String, String> globalEnv = new LinkedHashMap<>();

        Object[] ids = Arrays.stream(syncSnapshot.getBProgram().getGlobalScope().getIds()).filter((p) -> !p.toString().equals("bp")).toArray();
        for (Object id : ids) {
            Object jsValue = syncSnapshot.getBProgram().getFromGlobalScope(id.toString(), Object.class).get();
            String var_value = getVarGsonValue(jsValue);
            globalEnv.put(id.toString(), var_value);
        }
        return globalEnv;
    }

    private DebuggerConfigs generateDebuggerConfigs(BPJsDebugger bpJsDebugger) {
        return bpJsDebugger == null ? null :
                new DebuggerConfigs(bpJsDebugger.isMuteBreakPoints(), bpJsDebugger.isWaitForExternalEvents(), bpJsDebugger.isSkipSyncPoints());
    }

    private List<BThreadInfo> generateBThreadInfos(BProgramSyncSnapshot syncSnapshot, RunnerState state, Dim.ContextData lastContextData) {
        Set<BThreadSyncSnapshot> bThreadSyncSnapshots = syncSnapshot.getBThreadSnapshots();
        List<BThreadInfo> bThreadInfoList = bThreadSyncSnapshots
                .stream()
                .map(bThreadSyncSnapshot -> createBThreadInfo(bThreadSyncSnapshot, state, lastContextData, syncSnapshot))
                .collect(Collectors.toList());

        if (state.getDebuggerState() == RunnerState.State.JS_DEBUG && Context.getCurrentContext() != null) {
            bThreadInfoList.addAll(getRecentlyAddedBTInfo(lastContextData));
        } else {
            cleanFields();
        }
        return bThreadInfoList;
    }

    private EventsStatus generateEventsStatus(BProgramSyncSnapshot syncSnapshot, RunnerState state) {
        Set<SyncStatement> statements = syncSnapshot.getStatements();
        List<BEvent> requested = statements.stream().map(SyncStatement::getRequest).flatMap(Collection::stream).collect(Collectors.toList());

        if (!RunnerState.State.JS_DEBUG.equals(state.getDebuggerState())) {
            Context.enter();
        }
        List<EventSet> wait = getMatchingEventSet(requested, statements.stream().map(SyncStatement::getWaitFor));
        List<EventInfo> waitEvents = getMatchingEventInfo(wait);

        List<EventSet> blocked = getMatchingEventSet(requested, statements.stream().map(SyncStatement::getBlock));
        List<EventInfo> blockedEvents = getMatchingEventInfo(blocked);
        if (!RunnerState.State.JS_DEBUG.equals(state.getDebuggerState())) {
            Context.exit();
        }

        Set<EventInfo> requestedEvents = requested.stream().map((e) -> new EventInfo(getEventName(e))).collect(Collectors.toSet());
        List<EventInfo> externalEvents = syncSnapshot.getExternalEvents().stream()
                .filter(e -> !e.equals(none) && !e.equals(NO_MORE_WAIT_EXTERNAL))
                .map(e -> new EventInfo(e.getName()))
                .collect(Collectors.toList());
        if (currentEvent == null) {
            return new EventsStatus(waitEvents, blockedEvents, requestedEvents, externalEvents);
        } else {
            return new EventsStatus(waitEvents, blockedEvents, requestedEvents, externalEvents, new EventInfo(currentEvent));
        }
    }

    private List<EventInfo> getMatchingEventInfo(List<EventSet> eventSets) {
        return eventSets.stream().map((e) -> e.equals(none) ? null : new EventInfo(getEventName(e))).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<EventSet> getMatchingEventSet(List<BEvent> bEvents, Stream<EventSet> eventSetStream) {
        return eventSetStream.filter(e -> bEvents.stream().anyMatch(e::contains)).collect(Collectors.toList());
    }

    public SortedMap<Long, EventInfo> generateEventsHistory(int from, int to) {
        SortedMap<Long, BEvent> events = syncSnapshotHolder.getEventsHistoryStack(from, to);
        SortedMap<Long, EventInfo> eventsHistory = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<Long, BEvent> entry : events.entrySet()) {
            eventsHistory.put(entry.getKey(), new EventInfo(entry.getValue().name));
        }
        return eventsHistory;
    }

    private List<BThreadInfo> getRecentlyAddedBTInfo(Dim.ContextData lastContextData) {
        List<BThreadInfo> bThreadInfoList = new ArrayList<>();
        Context cx = Context.getCurrentContext();
        try {
            Object lastInterpreterFrame = getValue(cx, "lastInterpreterFrame");
            Object fnOrScript = lastInterpreterFrame == null ? null : getBaseFnOrScript(lastInterpreterFrame);

            for (Pair<String, Object> recentlyRegisteredPair : recentlyRegisteredBT) {
                Object o = recentlyRegisteredPair.getRight();
                String btName = recentlyRegisteredPair.getLeft();
                if (o == fnOrScript) { // current running bt
                    Map<Integer, BThreadScope> env = getEnvDebug(lastInterpreterFrame, lastContextData, btName);
                    newBTInterpreterFrames.put(btName, lastInterpreterFrame);
                    bThreadInfoList.add(new BThreadInfo(btName, env));
                } else {
                    Object savedInterpreterFrame = newBTInterpreterFrames.get(btName);
                    Map<Integer, BThreadScope> env = savedInterpreterFrame == null ? new HashMap<>() :
                            getEnvDebug(savedInterpreterFrame, lastContextData, btName);
                    bThreadInfoList.add(new BThreadInfo(btName, env));
                }
            }
        } catch (Exception e) {
            logger.error("failed to get recently added BThread info, e: {0}", e, e.getMessage());
        }
        return bThreadInfoList;
    }

    private Object getBaseFnOrScript(Object lastInterpreterFrame) {
        Object frame = lastInterpreterFrame;

        Object parentFrame = lastInterpreterFrame;
        try {
            while (parentFrame != null) {
                frame = parentFrame;
                parentFrame = getValue(frame, "parentFrame");
            }
            return getValue(frame, "fnOrScript");
        } catch (Exception e) {
            logger.error("getBaseFnOrScript: failed e: {0}", e, e.getMessage());
            return null;
        }
    }

    private String getEventName(EventSet eventSet) {
        if (eventSet instanceof BEvent)
            return ((BEvent) eventSet).getName();
        else return Objects.toString(eventSet);
    }

    private BThreadInfo createBThreadInfo(BThreadSyncSnapshot bThreadSS, RunnerState state, Dim.ContextData lastContextData, BProgramSyncSnapshot syncSnapshot) {
        try {
            Object implementation = getValue(bThreadSS.getScope(), "implementation");
            Map<Integer, BThreadScope> env = state == null ? null :
                    (state.getDebuggerState() == RunnerState.State.JS_DEBUG && Context.getCurrentContext() != null) ? getEnvDebug(implementation, lastContextData, bThreadSS.getName()) :
                            getEnv(implementation);

            Set<SyncStatement> statements = syncSnapshot.getStatements();
            List<BEvent> allRequestedBEvents = statements.stream().map(SyncStatement::getRequest).flatMap(Collection::stream).collect(Collectors.toList());

            EventSet waitFor = bThreadSS.getSyncStatement().getWaitFor();
            EventSet blocked = bThreadSS.getSyncStatement().getBlock();
            if (!RunnerState.State.JS_DEBUG.equals(state.getDebuggerState())) {
                Context.enter();
            }
            Set<BEvent> waitBEvents = allRequestedBEvents.stream().filter(req -> waitFor.contains(req)).collect(Collectors.toSet());
            Set<BEvent> blockedBEvents = allRequestedBEvents.stream().filter(req -> blocked.contains(req)).collect(Collectors.toSet());
            if (!RunnerState.State.JS_DEBUG.equals(state.getDebuggerState())) {
                Context.exit();
            }
            Set<EventInfo> waitEvents = waitBEvents.stream().map((e) -> e.equals(none) ? null : new EventInfo(getEventName(e))).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<EventInfo> blockedEvents = blockedBEvents.stream().map((e) -> e.equals(none) ? null : new EventInfo(getEventName(e))).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<EventInfo> requested = new ArrayList<>(bThreadSS.getSyncStatement().getRequest()).stream().map((r) -> new EventInfo(r.getName())).collect(Collectors.toSet());

            return new BThreadInfo(bThreadSS.getName(), env, waitEvents, blockedEvents, requested);
        } catch (Exception e) {
            logger.error("failed to create BThread info, e: {0}", e, e.getMessage());
        }
        return null;
    }

    private <T> T getValue(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field fld = instance.getClass().getDeclaredField(fieldName);
        fld.setAccessible(true);
        return (T) fld.get(instance);
    }

    /**
     * This function generating bthread env in JS debug state
     *
     * @param interpreterCallFrame
     * @param lastContextData
     * @param btName
     * @return
     */
    private Map<Integer, BThreadScope> getEnvDebug(Object interpreterCallFrame, Dim.ContextData lastContextData, String btName) {
        Map<Integer, BThreadScope> env = new HashMap<>();
        int key = 0;
        Context cx = Context.getCurrentContext();
        boolean currentRunningBT;
        try {
            Object cxInterpreterFrame = getValue(cx, "lastInterpreterFrame");
            if (cxInterpreterFrame == null) {
                return getEnv(interpreterCallFrame);
            }
            Object myScope = getValue(interpreterCallFrame, "scope");
            currentRunningBT = isScopesRelated(cxInterpreterFrame, myScope);
            Object parentFrame = interpreterCallFrame;
            if (currentRunningBT) { //current running BT
                logger.info("currentRunningBT + " + btName);
                for (int i = 0; i < lastContextData.frameCount(); i++) {
                    ScriptableObject scope = (ScriptableObject) lastContextData.getFrame(i).scope();
                    env.put(i, getScope(scope, lastContextData.getFrame(i).getLineNumber()));
                    key++;
                }
                key = lastContextData.frameCount();
                this.currentRunningBT = btName;
                parentFrame = getValue(cxInterpreterFrame, "parentFrame");
            }

            while (parentFrame != null) {
                if (currentRunningBT) {
                    Dim.StackFrame stackFrame = getValue(parentFrame, "debuggerFrame");
                    Dim.ContextData debuggerFrame = stackFrame.contextData();
                    if (debuggerFrame != lastContextData) {
                        for (int i = 0; i < debuggerFrame.frameCount(); i++) {
                            ScriptableObject scope = (ScriptableObject) debuggerFrame.getFrame(i).scope();
                            env.put(key, getScope(scope, debuggerFrame.getFrame(i).getLineNumber()));
                            key++;
                        }
                    }
                } else {
                    ScriptableObject scope = getValue(parentFrame, "scope");
                    Dim.StackFrame stackFrame = getValue(parentFrame, "debuggerFrame");
                    env.put(key, getScope(scope, stackFrame.getLineNumber()));
                    key++;
                }
                parentFrame = getValue(parentFrame, "parentFrame");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("getEnvDebug: failed to get env, e: {0}", e, e.getMessage());
        }
        return env;
    }

    private boolean isScopesRelated(Object cxInterpreterFrame, Object myScope) {
        Object scope;
        Object parentFrame = cxInterpreterFrame;
        try {
            while (parentFrame != null) {
                scope = getValue(parentFrame, "scope");
                if (scope == myScope)
                    return true;
                parentFrame = getValue(parentFrame, "parentFrame");
            }
            return false;
        } catch (Exception e) {
            logger.error("isScopesRelated: failed e: {0}", e, e.getMessage());
            return false;
        }
    }

    private Map<Integer, BThreadScope> getEnv(Object interpreterCallFrame) {
        Map<Integer, BThreadScope> env = new HashMap<>();
        int key = 0;
        try {
            ScriptableObject scope = getValue(interpreterCallFrame, "scope");
            Dim.StackFrame stackFrame = getValue(interpreterCallFrame, "debuggerFrame");
            env.put(key++, getScope(scope, stackFrame.getLineNumber()));
            Object parentFrame = getValue(interpreterCallFrame, "parentFrame");
            while (parentFrame != null) {
                scope = getValue(parentFrame, "scope");
                stackFrame = getValue(parentFrame, "debuggerFrame");
                env.put(key, getScope(scope, stackFrame.getLineNumber()));
                key++;
                parentFrame = getValue(parentFrame, "parentFrame");
            }
        } catch (Exception e) {
            logger.error("getEnv: failed to get env, e: {0}", e, e.getMessage());
        }
        return env;
    }

    private BThreadScope getScope(ScriptableObject scope, int lineNumber) {
        Map<String, String> variables = new LinkedHashMap<>();
        try {
            Object function = getValue(scope, "function");
            Object interpretedData = getValue(function, "idata");
            String itsName = getValue(interpretedData, "itsName");
            Object[] ids = Arrays.stream(scope.getIds()).filter((p) -> !p.toString().equals("arguments") && !p.toString().equals(itsName + "param")).toArray();
            for (Object id : ids) {
                Object jsValue = scope.get(id);
                String var_value = getVarGsonValue(jsValue);
                variables.put(id.toString(), var_value);
            }
            return new BThreadScope(itsName != null ? itsName : "BTMain", String.valueOf(lineNumber), variables);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("failed to get scope, e: {0}", e, e.getMessage());
        }
        return null;
    }

    private String getVarGsonValue(Object jsValue) {
        return serializeDebuggerValue(jsValue);
    }

    static String serializeDebuggerValue(Object jsValue) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeSpecialFloatingPointValues();
        Gson gson = gsonBuilder.create();
        try {
            Object safeValue = makeJsonSafe(jsValue, new IdentityHashMap<>(), 0);
            return gson.toJson(safeValue);
        } catch (StackOverflowError | RuntimeException e) {
            logger.error("getVarGsonValue Error: type: {0}, error: {1}", e,
                    jsValue == null ? "null" : jsValue.getClass().getName(), e.getMessage());
            return gson.toJson(objectLabel(jsValue));
        }
    }

    public BPDebuggerState getLastState() {
        return lastState;
    }

    private static Object makeJsonSafe(Object jsValue, IdentityHashMap<Object, Boolean> active, int depth) {
        if (jsValue == null || jsValue == Undefined.instance || jsValue == Scriptable.NOT_FOUND) {
            return null;
        }
        if (jsValue instanceof String || jsValue instanceof Character || jsValue instanceof Boolean) {
            return boundedString(String.valueOf(jsValue));
        }
        if (jsValue instanceof Number) {
            return jsValue;
        }
        if (jsValue instanceof ConsString) {
            return boundedString(jsValue.toString());
        }
        if (jsValue instanceof NativeFunction) {
            return ((NativeFunction) jsValue).getTypeOf();
        }
        if (jsValue instanceof ArrowFunction) {
            return ((ArrowFunction) jsValue).getTypeOf();
        }
        if (jsValue instanceof BEvent) {
            return ((BEvent) jsValue).getName();
        }
        if (jsValue instanceof EventSet || jsValue instanceof JsEventSet) {
            return safeToString(jsValue);
        }
        if (depth >= MAX_VARIABLE_DEPTH) {
            return "<max-depth:" + jsValue.getClass().getSimpleName() + ">";
        }
        if (active.containsKey(jsValue)) {
            return "<circular:" + jsValue.getClass().getSimpleName() + ">";
        }

        if (jsValue instanceof NativeJavaObject) {
            Object unwrapped = ((NativeJavaObject) jsValue).unwrap();
            if (unwrapped == jsValue) {
                return objectLabel(jsValue);
            }
            return makeJsonSafe(unwrapped, active, depth + 1);
        }

        if (jsValue instanceof NativeArray) {
            NativeArray jsArray = (NativeArray) jsValue;
            active.put(jsValue, Boolean.TRUE);
            try {
                int length = (int) Math.min(jsArray.getLength(), MAX_VARIABLE_ENTRIES);
                List<Object> values = new ArrayList<>(length + 1);
                for (int index = 0; index < length; index++) {
                    values.add(makeJsonSafe(jsArray.get(index), active, depth + 1));
                }
                if (jsArray.getLength() > MAX_VARIABLE_ENTRIES) {
                    values.add("<" + (jsArray.getLength() - MAX_VARIABLE_ENTRIES) + " more entries>");
                }
                return values;
            } finally {
                active.remove(jsValue);
            }
        }

        if (jsValue instanceof Scriptable) {
            Scriptable scriptable = (Scriptable) jsValue;
            active.put(jsValue, Boolean.TRUE);
            try {
                Map<String, Object> values = new LinkedHashMap<>();
                Object[] ids = scriptable.getIds();
                int count = Math.min(ids.length, MAX_VARIABLE_ENTRIES);
                for (int index = 0; index < count; index++) {
                    Object id = ids[index];
                    Object value = id instanceof Number
                            ? scriptable.get(((Number) id).intValue(), scriptable)
                            : scriptable.get(String.valueOf(id), scriptable);
                    values.put(boundedString(String.valueOf(id)), makeJsonSafe(value, active, depth + 1));
                }
                if (ids.length > MAX_VARIABLE_ENTRIES) {
                    values.put("<truncated>", (ids.length - MAX_VARIABLE_ENTRIES) + " more entries");
                }
                return values;
            } finally {
                active.remove(jsValue);
            }
        }

        if (jsValue instanceof Map) {
            active.put(jsValue, Boolean.TRUE);
            try {
                Map<String, Object> values = new LinkedHashMap<>();
                int count = 0;
                for (Object entryObject : ((Map<?, ?>) jsValue).entrySet()) {
                    if (count++ >= MAX_VARIABLE_ENTRIES) {
                        values.put("<truncated>", "more entries");
                        break;
                    }
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;
                    values.put(boundedString(String.valueOf(entry.getKey())),
                            makeJsonSafe(entry.getValue(), active, depth + 1));
                }
                return values;
            } finally {
                active.remove(jsValue);
            }
        }

        if (jsValue instanceof Iterable) {
            active.put(jsValue, Boolean.TRUE);
            try {
                List<Object> values = new ArrayList<>();
                for (Object value : (Iterable<?>) jsValue) {
                    if (values.size() >= MAX_VARIABLE_ENTRIES) {
                        values.add("<more entries>");
                        break;
                    }
                    values.add(makeJsonSafe(value, active, depth + 1));
                }
                return values;
            } finally {
                active.remove(jsValue);
            }
        }

        if (jsValue.getClass().isArray()) {
            active.put(jsValue, Boolean.TRUE);
            try {
                int sourceLength = Array.getLength(jsValue);
                int length = Math.min(sourceLength, MAX_VARIABLE_ENTRIES);
                List<Object> values = new ArrayList<>(length + 1);
                for (int index = 0; index < length; index++) {
                    values.add(makeJsonSafe(Array.get(jsValue, index), active, depth + 1));
                }
                if (sourceLength > MAX_VARIABLE_ENTRIES) {
                    values.add("<" + (sourceLength - MAX_VARIABLE_ENTRIES) + " more entries>");
                }
                return values;
            } finally {
                active.remove(jsValue);
            }
        }

        // Never reflectively serialize arbitrary Java objects. BPjs and Rhino objects
        // commonly contain parent/context links that form cycles.
        return objectLabel(jsValue);
    }

    private static String safeToString(Object value) {
        try {
            return boundedString(String.valueOf(value));
        } catch (StackOverflowError | RuntimeException ignored) {
            return objectLabel(value);
        }
    }

    private static String objectLabel(Object value) {
        return value == null ? "null" : "<" + value.getClass().getSimpleName() + ">";
    }

    private static String boundedString(String value) {
        if (value == null || value.length() <= MAX_VARIABLE_STRING_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VARIABLE_STRING_LENGTH) + "...<truncated>";
    }

    public void setRecentlyRegisteredBThreads(Set<Pair<String, Object>> recentlyRegistered) {
        this.recentlyRegisteredBT = recentlyRegistered;
    }

    public BPDebuggerState peekNextState(BProgramSyncSnapshot syncSnapshot, RunnerState state, Dim.ContextData lastContextData, Dim.SourceInfo sourceInfo) {
        return generateDebuggerStateInner(syncSnapshot, state, lastContextData, sourceInfo);
    }

    public void updateCurrentEvent(String name) {
        this.currentEvent = name;
    }

    public String getDebuggerId(){
        return this.bpJsDebugger.getDebuggerId();
    }
    public void notifyDebuggerSubscribers(BPEvent event){
        this.bpJsDebugger.notifySubscribers(event);
    }


}
