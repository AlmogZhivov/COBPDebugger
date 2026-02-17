package il.ac.bgu.se.bp.rest;

import il.ac.bgu.se.bp.rest.controller.BPjsIDERestController;
import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import il.ac.bgu.se.bp.service.BPjsIDEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static il.ac.bgu.se.bp.rest.utils.Constants.SIMP_SESSION_ID;
import static il.ac.bgu.se.bp.rest.utils.Endpoints.*;

@Controller
@RequestMapping(BASE_URI)
public class BPjsIDERestControllerImpl implements BPjsIDERestController {

    @Autowired
    private BPjsIDEService bPjsIDEService;

    @MessageMapping(SUBSCRIBE)
    public @ResponseBody void subscribeUser(@Header(SIMP_SESSION_ID) String sessionId, Principal principal) {
        System.out.println("userId: " + principal.getName());
        bPjsIDEService.subscribeUser(sessionId, principal.getName());
    }

    @Override
    @RequestMapping(value = RUN, method = RequestMethod.POST)
    public @ResponseBody BooleanResponse run(@RequestHeader("userId") String userId, @RequestBody RunRequest code) {
        return bPjsIDEService.run(code, userId);
    }

    @Override
    @RequestMapping(value = DEBUG, method = RequestMethod.POST)
    public @ResponseBody DebugResponse debug(@RequestHeader("userId") String userId, @RequestBody DebugRequest code) {
        return bPjsIDEService.debug(code, userId);
    }

    @Override
    @RequestMapping(value = BREAKPOINT, method = RequestMethod.POST)
    public @ResponseBody BooleanResponse setBreakpoint(@RequestHeader("userId") String userId,
            @RequestBody SetBreakpointRequest setBreakpointRequest) {
        return bPjsIDEService.setBreakpoint(userId, setBreakpointRequest);
    }

    @Override
    @RequestMapping(value = BREAKPOINT, method = RequestMethod.PUT)
    public @ResponseBody BooleanResponse toggleMuteBreakpoints(@RequestHeader("userId") String userId,
            @RequestBody ToggleBreakpointsRequest toggleBreakpointsRequest) {
        return bPjsIDEService.toggleMuteBreakpoints(userId, toggleBreakpointsRequest);
    }

    @Override
    @RequestMapping(value = WAIT_EXTERNAL, method = RequestMethod.PUT)
    public @ResponseBody BooleanResponse toggleWaitForExternal(@RequestHeader("userId") String userId,
            @RequestBody ToggleWaitForExternalRequest toggleWaitForExternalRequest) {
        return bPjsIDEService.toggleWaitForExternal(userId, toggleWaitForExternalRequest);
    }

    @Override
    @RequestMapping(value = SYNC_STATES, method = RequestMethod.PUT)
    public @ResponseBody BooleanResponse toggleMuteSyncPoints(@RequestHeader("userId") String userId,
            @RequestBody ToggleSyncStatesRequest toggleMuteSyncPoints) {
        return bPjsIDEService.toggleMuteSyncPoints(userId, toggleMuteSyncPoints);
    }

    @Override
    @RequestMapping(value = STOP, method = RequestMethod.GET)
    public @ResponseBody BooleanResponse stop(@RequestHeader("userId") String userId) {
        return bPjsIDEService.stop(userId);
    }

    @Override
    @RequestMapping(value = STEP_OUT, method = RequestMethod.GET)
    public @ResponseBody BooleanResponse stepOut(@RequestHeader("userId") String userId) {
        return bPjsIDEService.stepOut(userId);
    }

    @Override
    @RequestMapping(value = STEP_INTO, method = RequestMethod.GET)
    public @ResponseBody BooleanResponse stepInto(@RequestHeader("userId") String userId) {
        return bPjsIDEService.stepInto(userId);
    }

    @Override
    @RequestMapping(value = STEP_OVER, method = RequestMethod.GET)
    public @ResponseBody BooleanResponse stepOver(@RequestHeader("userId") String userId) {
        return bPjsIDEService.stepOver(userId);
    }

    @Override
    @RequestMapping(value = CONTINUE, method = RequestMethod.GET)
    public @ResponseBody BooleanResponse continueRun(@RequestHeader("userId") String userId) {
        return bPjsIDEService.continueRun(userId);
    }

    @Override
    @RequestMapping(value = NEXT_SYNC, method = RequestMethod.GET)
    public @ResponseBody BooleanResponse nextSync(@RequestHeader("userId") String userId) {
        return bPjsIDEService.nextSync(userId);
    }

    @Override
    @RequestMapping(value = SELECT_EVENT, method = RequestMethod.POST)
    public @ResponseBody BooleanResponse selectEvent(@RequestHeader("userId") String userId,
            @RequestBody SelectEventRequest selectEventRequest) {
        return bPjsIDEService.selectEvent(userId, selectEventRequest);
    }

    @Override
    @RequestMapping(value = EXTERNAL_EVENT, method = RequestMethod.POST)
    public @ResponseBody BooleanResponse externalEvent(@RequestHeader("userId") String userId,
            @RequestBody ExternalEventRequest externalEventRequest) {
        return bPjsIDEService.externalEvent(userId, externalEventRequest);
    }

    @Override
    @RequestMapping(value = EVENTS, method = RequestMethod.GET)
    public @ResponseBody EventsHistoryResponse getEventsHistory(@RequestHeader("userId") String userId,
            @RequestParam(name = "from") int from,
            @RequestParam(name = "to") int to) {
        return bPjsIDEService.getEventsHistory(userId, from, to);
    }

    @Override
    @RequestMapping(value = SYNC_SNAPSHOT, method = RequestMethod.PUT)
    public @ResponseBody BooleanResponse setSyncSnapshot(@RequestHeader("userId") String userId,
            @RequestBody SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return bPjsIDEService.setSyncSnapshot(userId, setSyncSnapshotRequest);
    }

    @Override
    @RequestMapping(value = SYNC_SNAPSHOT, method = RequestMethod.GET)
    public @ResponseBody SyncSnapshot exportSyncSnapshot(@RequestHeader("userId") String userId) {
        return bPjsIDEService.exportSyncSnapshot(userId);
    }

    @Override
    @RequestMapping(value = SYNC_SNAPSHOT, method = RequestMethod.POST)
    public @ResponseBody BooleanResponse importSyncSnapshot(@RequestHeader("userId") String userId,
            @RequestBody ImportSyncSnapshotRequest importSyncSnapshotRequest) {
        return bPjsIDEService.importSyncSnapshot(userId, importSyncSnapshotRequest);
    }

    @Override
    @RequestMapping(value = "/previewSnapshot", method = RequestMethod.PUT)
    public @ResponseBody BooleanResponse previewSyncSnapshot(@RequestHeader("userId") String userId,
            @RequestBody SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return bPjsIDEService.previewSyncSnapshot(userId, setSyncSnapshotRequest);
    }

    @Override
    @RequestMapping(value = "/restoreSnapshot", method = RequestMethod.GET)
    public @ResponseBody BooleanResponse restoreLatestSnapshot(@RequestHeader("userId") String userId) {
        return bPjsIDEService.restoreLatestSnapshot(userId);
    }

}
