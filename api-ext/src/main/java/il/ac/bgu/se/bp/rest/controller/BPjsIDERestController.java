package il.ac.bgu.se.bp.rest.controller;

import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;

import java.security.Principal;

public interface BPjsIDERestController {

    void subscribeUser(String sessionId, Principal principal);

    BooleanResponse run(String userId, RunRequest code);

    DebugResponse debug(String userId, DebugRequest code);

    BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest);

    BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakpointsRequest);

    BooleanResponse toggleWaitForExternal(String userId, ToggleWaitForExternalRequest toggleWaitForExternalRequest);

    BooleanResponse toggleMuteSyncPoints(String userId, ToggleSyncStatesRequest toggleMuteSyncPoints);

    BooleanResponse stop(String userId);

    BooleanResponse stepOut(String userId);

    BooleanResponse stepInto(String userId);

    BooleanResponse stepOver(String userId);

    BooleanResponse continueRun(String userId);

    BooleanResponse nextSync(String userId);

    BooleanResponse selectEvent(String userId, SelectEventRequest selectEventRequest);

    BooleanResponse externalEvent(String userId, ExternalEventRequest externalEventRequest);

    EventsHistoryResponse getEventsHistory(String userId, int from, int to);

    BooleanResponse setSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest);

    SyncSnapshot exportSyncSnapshot(String userId);

    BooleanResponse importSyncSnapshot(String userId, ImportSyncSnapshotRequest importSyncSnapshotRequest);

    BooleanResponse previewSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest);

    BooleanResponse restoreLatestSnapshot(String userId);
}
