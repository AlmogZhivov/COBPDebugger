package il.ac.bgu.se.bp.service;

import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;

public interface BPjsIDEService {

    void subscribeUser(String sessionId, String userId);

    BooleanResponse run(RunRequest runRequest, String userId);

    DebugResponse debug(DebugRequest debugRequest, String userId);

    BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest);

    BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakPointStatus);

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
