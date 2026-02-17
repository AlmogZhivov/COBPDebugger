package il.ac.bgu.se.bp.rest.utils;

public class Endpoints {

    public static final String BASE_URI = "/bpjs";
    public static final String BASE_WEB_SOCKET_URI = "/ws";

    public static final String SUBSCRIBE = "/subscribe";
    public static final String STATE = "/state";
    public static final String CONSOLE = "/console";
    public static final String PROGRAM = "/program";
    public static final String UPDATE = "/update";

    public static final String CONSOLE_UPDATE = CONSOLE + UPDATE;
    public static final String STATE_UPDATE = STATE + UPDATE;
    public static final String PROGRAM_UPDATE = PROGRAM + UPDATE;

    public static final String RUN = "/run";
    public static final String DEBUG = "/debug";

    public static final String BREAKPOINT = "/breakpoint";
    public static final String STOP = "/stop";
    public static final String STEP_OUT = "/stepOut";
    public static final String STEP_INTO = "/stepInto";
    public static final String STEP_OVER = "/stepOver";
    public static final String CONTINUE = "/continue";

    public static final String NEXT_SYNC = "/nextSync";
    public static final String SELECT_EVENT = "/selectEvent";
    public static final String SYNC_STATES = "/syncStates";

    public static final String EVENTS = "/events";
    public static final String WAIT_EXTERNAL = "/waitExternal";

    public static final String EXTERNAL_EVENT = "/externalEvent";
    public static final String SYNC_SNAPSHOT = "/syncSnapshot";

}
