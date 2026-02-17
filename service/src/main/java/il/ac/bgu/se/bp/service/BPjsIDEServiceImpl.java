package il.ac.bgu.se.bp.service;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.manage.DebuggerFactory;
import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import il.ac.bgu.se.bp.service.code.SourceCodeHelper;
import il.ac.bgu.se.bp.service.manage.PrototypeContextFactory;
import il.ac.bgu.se.bp.service.manage.SessionHandler;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.mozilla.javascript.ContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BPjsIDEServiceImpl implements BPjsIDEService {

    private static final Logger logger = new Logger(BPjsIDEServiceImpl.class);

    @Autowired
    private SessionHandler<BPJsDebugger<BooleanResponse>> sessionHandler;

    @Autowired
    private SourceCodeHelper sourceCodeHelper;

    @Autowired
    private DebuggerFactory<BooleanResponse> debuggerFactory;

    @Autowired
    private PrototypeContextFactory prototypeContextFactory;

    @PostConstruct
    public void setUp() {
        ContextFactory.initGlobal(prototypeContextFactory);
    }

    @Override
    public void subscribeUser(String sessionId, String userId) {
        System.out.println("Received message from {1} with sessionId {2}" + ",," + userId + "," + sessionId);
        sessionHandler.addUser(sessionId, userId);
    }

    @Override
    public BooleanResponse run(RunRequest runRequest, String userId) {
        if (!validateRequest(runRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        logger.info("received run request for user: {0}", userId);
        String filename = sourceCodeHelper.createCodeFile(runRequest.getSourceCode());
        if (StringUtils.isEmpty(filename)) {
            return createErrorResponse(ErrorCode.INVALID_SOURCE_CODE);
        }

        BPJsDebugger<BooleanResponse> bpProgramDebugger = debuggerFactory.getBPJsDebugger(userId, filename,
                DebuggerLevel.LIGHT);
        bpProgramDebugger.subscribe(sessionHandler);
        sessionHandler.addNewRunExecution(userId, bpProgramDebugger, filename);
        sessionHandler.updateLastOperationTime(userId);

        return bpProgramDebugger.startSync(new HashMap<>(), true, true, runRequest.isWaitForExternalEvents());
    }

    @Override
    public DebugResponse debug(DebugRequest debugRequest, String userId) {
        if (!validateRequest(debugRequest)) {
            return new DebugResponse(createErrorResponse(ErrorCode.INVALID_REQUEST));
        }

        if (!sessionHandler.validateUserId(userId)) {
            return new DebugResponse(createErrorResponse(ErrorCode.UNKNOWN_USER));
        }

        String filename = sourceCodeHelper.createCodeFile(debugRequest.getSourceCode());
        if (StringUtils.isEmpty(filename)) {
            return new DebugResponse(createErrorResponse(ErrorCode.INVALID_SOURCE_CODE));
        }

        logger.info("received debug request for user: {0}", userId);
        return handleNewDebugRequest(debugRequest, userId, filename);
    }

    private DebugResponse handleNewDebugRequest(DebugRequest debugRequest, String userId, String filename) {
        BPJsDebugger<BooleanResponse> bpProgramDebugger = debuggerFactory.getBPJsDebugger(userId, filename,
                DebuggerLevel.NORMAL);
        bpProgramDebugger.subscribe(sessionHandler);

        sessionHandler.addNewDebugExecution(userId, bpProgramDebugger, filename);
        sessionHandler.updateLastOperationTime(userId);

        Map<Integer, Boolean> breakpointsMap = debugRequest.getBreakpoints()
                .stream()
                .collect(Collectors.toMap(Function.identity(), b -> Boolean.TRUE));

        return bpProgramDebugger.startSync(breakpointsMap, debugRequest.isSkipSyncStateToggle(),
                debugRequest.isSkipBreakpointsToggle(), debugRequest.isWaitForExternalEvents());
    }

    @Override
    public BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest) {
        if (setBreakpointRequest == null) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.setBreakpoint(setBreakpointRequest.getLineNumber(),
                setBreakpointRequest.isStopOnBreakpoint());
    }

    @Override
    public BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakPointStatus) {
        if (toggleBreakPointStatus == null) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.toggleMuteBreakpoints(toggleBreakPointStatus.isSkipBreakpoints());
    }

    @Override
    public BooleanResponse toggleWaitForExternal(String userId,
            ToggleWaitForExternalRequest toggleWaitForExternalRequest) {
        if (toggleWaitForExternalRequest == null) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerOrRunnerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.toggleWaitForExternalEvents(toggleWaitForExternalRequest.isWaitForExternal());
    }

    @Override
    public BooleanResponse toggleMuteSyncPoints(String userId, ToggleSyncStatesRequest toggleMuteSyncPoints) {
        if (toggleMuteSyncPoints == null) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.toggleMuteSyncPoints(toggleMuteSyncPoints.isSkipSyncStates());
    }

    @Override
    public BooleanResponse stop(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerOrRunnerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.stop();
    }

    @Override
    public BooleanResponse stepOut(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.stepOut();
    }

    @Override
    public BooleanResponse stepInto(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.stepInto();
    }

    @Override
    public BooleanResponse stepOver(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.stepOver();
    }

    @Override
    public BooleanResponse continueRun(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.continueRun();
    }

    @Override
    public BooleanResponse nextSync(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.nextSync();
    }

    @Override
    public BooleanResponse selectEvent(String userId, SelectEventRequest selectEventRequest) {
        if (selectEventRequest == null || StringUtils.isEmpty(selectEventRequest.getEventName())) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        String eventName = selectEventRequest.getEventName();
        return bpJsDebugger.selectEvent(eventName);
    }

    @Override
    public BooleanResponse externalEvent(String userId, ExternalEventRequest externalEventRequest) {
        if (externalEventRequest == null || StringUtils.isEmpty(externalEventRequest.getExternalEvent())) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerOrRunnerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        String externalEvent = externalEventRequest.getExternalEvent();
        return externalEventRequest.isAddEvent() ? bpJsDebugger.addExternalEvent(externalEvent)
                : bpJsDebugger.removeExternalEvent(externalEvent);
    }

    @Override
    public EventsHistoryResponse getEventsHistory(String userId, int from, int to) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerOrRunnerByUser(userId);
        if (bpJsDebugger == null) {
            return new EventsHistoryResponse();
        }

        return new EventsHistoryResponse(bpJsDebugger.getEventsHistory(from, to));
    }

    @Override
    public BooleanResponse setSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        if (setSyncSnapshotRequest == null) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        long snapShotTime = setSyncSnapshotRequest.getSnapShotTime();
        return bpJsDebugger.setSyncSnapshot(snapShotTime);
    }

    @Override
    public SyncSnapshot exportSyncSnapshot(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerOrRunnerByUser(userId);
        if (bpJsDebugger == null) {
            return new SyncSnapshot();
        }

        return new SyncSnapshot(sessionHandler.getUsersSourceCode(userId), bpJsDebugger.getSyncSnapshot());
    }

    @Override
    public BooleanResponse importSyncSnapshot(String userId, ImportSyncSnapshotRequest importSyncSnapshotRequest) {
        if (!validateRequest(importSyncSnapshotRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        String filename = sourceCodeHelper.createCodeFile(importSyncSnapshotRequest.getSyncSnapshot().getSourceCode());
        if (StringUtils.isEmpty(filename)) {
            return new DebugResponse(createErrorResponse(ErrorCode.INVALID_SOURCE_CODE));
        }

        logger.info("received import sync snapshot request for user: {0}", userId);
        BPJsDebugger<BooleanResponse> bpProgramDebugger = debuggerFactory.getBPJsDebugger(userId, filename,
                DebuggerLevel.NORMAL);
        bpProgramDebugger.subscribe(sessionHandler);

        boolean isDebug = importSyncSnapshotRequest.isDebug();
        boolean isSkipSyncPoint = isDebug && importSyncSnapshotRequest.isSkipSyncStateToggle();
        boolean isSkipBreakpoints = isDebug && importSyncSnapshotRequest.isSkipBreakpointsToggle();
        boolean isWaitForExternalEvents = importSyncSnapshotRequest.isWaitForExternalEvents();
        Map<Integer, Boolean> breakpointsMap = isDebug ? importSyncSnapshotRequest.getBreakpoints()
                .stream().collect(Collectors.toMap(Function.identity(), b -> Boolean.TRUE)) : new HashMap<>();
        if (isDebug) {
            sessionHandler.addNewDebugExecution(userId, bpProgramDebugger, filename);
        } else {
            sessionHandler.addNewRunExecution(userId, bpProgramDebugger, filename);
        }
        sessionHandler.updateLastOperationTime(userId);
        BooleanResponse setupResponse = bpProgramDebugger.setup(breakpointsMap, isSkipBreakpoints, isSkipSyncPoint,
                isWaitForExternalEvents);
        if (!setupResponse.isSuccess()) {
            return setupResponse;
        }
        return bpProgramDebugger.setSyncSnapshot(importSyncSnapshotRequest.getSyncSnapshot());
    }

    @Override
    public BooleanResponse previewSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        if (setSyncSnapshotRequest == null) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        long snapShotTime = setSyncSnapshotRequest.getSnapShotTime();
        return bpJsDebugger.previewSyncSnapshot(snapShotTime);
    }

    @Override
    public BooleanResponse restoreLatestSnapshot(String userId) {
        BPJsDebugger<BooleanResponse> bpJsDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (bpJsDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        sessionHandler.updateLastOperationTime(userId);
        return bpJsDebugger.restoreLatestSnapshot();
    }

    private BooleanResponse createErrorResponse(ErrorCode errorCode) {
        return new BooleanResponse(false, errorCode);
    }

    private boolean validateRequest(RunRequest runRequest) {
        return runRequest != null && !StringUtils.isEmpty(runRequest.getSourceCode());
    }

    private boolean validateRequest(ImportSyncSnapshotRequest request) {
        return request != null && request.getSyncSnapshot() != null &&
                request.getSyncSnapshot().getSyncSnapshot() != null &&
                !StringUtils.isEmpty(request.getSyncSnapshot().getSourceCode());
    }

}
