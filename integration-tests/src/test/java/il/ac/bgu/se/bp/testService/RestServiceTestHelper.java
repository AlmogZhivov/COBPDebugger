package il.ac.bgu.se.bp.testService;

import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import il.ac.bgu.se.bp.rest.utils.Endpoints;
import il.ac.bgu.se.bp.session.ITSessionManagerImpl;
import il.ac.bgu.se.bp.session.ITStompSessionHandler;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;

import static il.ac.bgu.se.bp.common.Utils.waitUntilPredicateSatisfied;
import static il.ac.bgu.se.bp.rest.utils.Endpoints.*;

public class RestServiceTestHelper implements TestService {

    private static final String BASE_URI = "localhost:8080";
    private static final String BASE_REST_URI = "http://" + BASE_URI + Endpoints.BASE_URI;
    private static final String SOCKET_URI = "ws://" + BASE_URI + "/ws";
    private static final String USER_ID = "userId";

    private final ITSessionManagerImpl usersSessionHandler;

    private static final ConcurrentHashMap<String, String> userTestIdsToServerIds = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ITStompSessionHandler> userSessionsByTestIds = new ConcurrentHashMap<>();

    public RestServiceTestHelper(ITSessionManagerImpl sessionHandler) {
        this.usersSessionHandler = sessionHandler;
    }

    private String getSocketUserId(String userTestId) {
        return userTestIdsToServerIds.get(userTestId);
    }

    @Override
    public void subscribeUser(String sessionId, Principal principal) {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        ITStompSessionHandler sessionHandler = new ITStompSessionHandler(usersSessionHandler);
        sessionHandler.setTestUserId(principal.getName());
        stompClient.connect(SOCKET_URI, sessionHandler);

        waitUntilPredicateSatisfied(sessionHandler::isConnected, 500, 3);
        userTestIdsToServerIds.put(principal.getName(), sessionHandler.getServerUserId());
        userSessionsByTestIds.put(principal.getName(), sessionHandler);
    }

    @Override
    public BooleanResponse run(String userId, RunRequest runRequest) {
        return performPostRequest(userId, RUN, runRequest, BooleanResponse.class);
    }

    @Override
    public DebugResponse debug(String userId, DebugRequest debugRequest) {
        return performPostRequest(userId, DEBUG, debugRequest, DebugResponse.class);
    }

    @Override
    public BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest) {
        return performPostRequest(userId, BREAKPOINT, setBreakpointRequest, BooleanResponse.class);
    }

    @Override
    public BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakpointsRequest) {
        return performPutRequest(userId, BREAKPOINT, toggleBreakpointsRequest, BooleanResponse.class);
    }

    @Override
    public BooleanResponse toggleMuteSyncPoints(String userId, ToggleSyncStatesRequest toggleMuteSyncPoints) {
        return performPutRequest(userId, SYNC_STATES, toggleMuteSyncPoints, BooleanResponse.class);
    }

    @Override
    public BooleanResponse toggleWaitForExternal(String userId,
            ToggleWaitForExternalRequest toggleWaitForExternalRequest) {
        return performPutRequest(userId, WAIT_EXTERNAL, toggleWaitForExternalRequest, BooleanResponse.class);
    }

    @Override
    public BooleanResponse stop(String userId) {
        return performGetRequest(userId, STOP, BooleanResponse.class);
    }

    @Override
    public BooleanResponse stepOut(String userId) {
        return performGetRequest(userId, STEP_OUT, BooleanResponse.class);
    }

    @Override
    public BooleanResponse stepInto(String userId) {
        return performGetRequest(userId, STEP_INTO, BooleanResponse.class);
    }

    @Override
    public BooleanResponse stepOver(String userId) {
        return performGetRequest(userId, STEP_OVER, BooleanResponse.class);
    }

    @Override
    public BooleanResponse continueRun(String userId) {
        return performGetRequest(userId, CONTINUE, BooleanResponse.class);
    }

    @Override
    public BooleanResponse nextSync(String userId) {
        return performGetRequest(userId, NEXT_SYNC, BooleanResponse.class);
    }

    @Override
    public BooleanResponse selectEvent(String userId, SelectEventRequest selectEventRequest) {
        return performPostRequest(userId, SELECT_EVENT, selectEventRequest, BooleanResponse.class);
    }

    @Override
    public BooleanResponse externalEvent(String userId, ExternalEventRequest externalEventRequest) {
        return performPostRequest(userId, EXTERNAL_EVENT, externalEventRequest, BooleanResponse.class);
    }

    @Override
    public BooleanResponse setSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return performPutRequest(userId, SYNC_SNAPSHOT, setSyncSnapshotRequest, BooleanResponse.class);
    }

    @Override
    public SyncSnapshot exportSyncSnapshot(String userId) {
        return performGetRequest(userId, SYNC_SNAPSHOT, SyncSnapshot.class);
    }

    @Override
    public BooleanResponse importSyncSnapshot(String userId, ImportSyncSnapshotRequest importSyncSnapshotRequest) {
        return null;
    }

    @Override
    public EventsHistoryResponse getEventsHistory(String userId, int from, int to) {
        return performGetRequest(userId, EVENTS, EventsHistoryResponse.class);
    }

    @Override
    public BooleanResponse previewSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return performPutRequest(userId, "/previewSnapshot", setSyncSnapshotRequest, BooleanResponse.class);
    }

    @Override
    public BooleanResponse restoreLatestSnapshot(String userId) {
        return performGetRequest(userId, "/restoreSnapshot", BooleanResponse.class);
    }

    private <T> T performPostRequest(String userId, String URL, Object body, Class<T> clazz) {
        Response response = RestAssured.with().header(new Header(USER_ID, getSocketUserId(userId))).body(body)
                .contentType(ContentType.JSON).when().post(BASE_REST_URI + URL);
        response.then().statusCode(200);
        return response.getBody().as(clazz);
    }

    private <T> T performPutRequest(String userId, String URL, Object body, Class<T> clazz) {
        Response response = RestAssured.with().header(new Header(USER_ID, getSocketUserId(userId))).body(body)
                .contentType(ContentType.JSON).when().put(BASE_REST_URI + URL);
        response.then().statusCode(200);
        return response.getBody().as(clazz);
    }

    private <T> T performGetRequest(String userId, String URL, Class<T> clazz) {
        Response response = RestAssured.with().header(new Header(USER_ID, getSocketUserId(userId)))
                .contentType(ContentType.JSON).when().get(BASE_REST_URI + URL);
        response.then().statusCode(200);
        return response.getBody().as(clazz);
    }
}
