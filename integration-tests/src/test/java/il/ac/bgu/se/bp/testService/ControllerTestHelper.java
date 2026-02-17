package il.ac.bgu.se.bp.testService;

import il.ac.bgu.se.bp.rest.controller.BPjsIDERestController;
import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;

public class ControllerTestHelper implements TestService {

    @Autowired
    private BPjsIDERestController bPjsIDERestController;

    @Override
    public void subscribeUser(String sessionId, Principal principal) {
        bPjsIDERestController.subscribeUser(sessionId, principal);
    }

    @Override
    public BooleanResponse run(String userId, RunRequest code) {
        return bPjsIDERestController.run(userId, code);
    }

    @Override
    public DebugResponse debug(String userId, DebugRequest code) {
        return bPjsIDERestController.debug(userId, code);
    }

    @Override
    public BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest) {
        return bPjsIDERestController.setBreakpoint(userId, setBreakpointRequest);
    }

    @Override
    public BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakpointsRequest) {
        return bPjsIDERestController.toggleMuteBreakpoints(userId, toggleBreakpointsRequest);
    }

    @Override
    public BooleanResponse toggleMuteSyncPoints(String userId, ToggleSyncStatesRequest toggleMuteSyncPoints) {
        return bPjsIDERestController.toggleMuteSyncPoints(userId, toggleMuteSyncPoints);
    }

    @Override
    public BooleanResponse toggleWaitForExternal(String userId,
            ToggleWaitForExternalRequest toggleWaitForExternalRequest) {
        return bPjsIDERestController.toggleWaitForExternal(userId, toggleWaitForExternalRequest);
    }

    @Override
    public BooleanResponse stop(String userId) {
        return bPjsIDERestController.stop(userId);
    }

    @Override
    public BooleanResponse stepOut(String userId) {
        return bPjsIDERestController.stepOut(userId);
    }

    @Override
    public BooleanResponse stepInto(String userId) {
        return bPjsIDERestController.stepInto(userId);
    }

    @Override
    public BooleanResponse stepOver(String userId) {
        return bPjsIDERestController.stepOver(userId);
    }

    @Override
    public BooleanResponse continueRun(String userId) {
        return bPjsIDERestController.continueRun(userId);
    }

    @Override
    public BooleanResponse nextSync(String userId) {
        return bPjsIDERestController.nextSync(userId);
    }

    @Override
    public BooleanResponse selectEvent(String userId, SelectEventRequest selectEventRequest) {
        return bPjsIDERestController.selectEvent(userId, selectEventRequest);
    }

    @Override
    public BooleanResponse externalEvent(String userId, ExternalEventRequest externalEventRequest) {
        return bPjsIDERestController.externalEvent(userId, externalEventRequest);
    }

    @Override
    public BooleanResponse setSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return bPjsIDERestController.setSyncSnapshot(userId, setSyncSnapshotRequest);
    }

    @Override
    public SyncSnapshot exportSyncSnapshot(String userId) {
        return bPjsIDERestController.exportSyncSnapshot(userId);
    }

    @Override
    public BooleanResponse importSyncSnapshot(String userId, ImportSyncSnapshotRequest importSyncSnapshotRequest) {
        return bPjsIDERestController.importSyncSnapshot(userId, importSyncSnapshotRequest);
    }

    @Override
    public EventsHistoryResponse getEventsHistory(String userId, int from, int to) {
        return bPjsIDERestController.getEventsHistory(userId, from, to);
    }

    @Override
    public BooleanResponse previewSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return bPjsIDERestController.previewSyncSnapshot(userId, setSyncSnapshotRequest);
    }

    @Override
    public BooleanResponse restoreLatestSnapshot(String userId) {
        return bPjsIDERestController.restoreLatestSnapshot(userId);
    }
}
