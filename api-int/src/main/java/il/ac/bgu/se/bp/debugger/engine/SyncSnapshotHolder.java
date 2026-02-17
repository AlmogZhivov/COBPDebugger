package il.ac.bgu.se.bp.debugger.engine;

import il.ac.bgu.se.bp.utils.Pair;
import java.util.SortedMap;

public interface SyncSnapshotHolder<T, U> {
    void addSyncSnapshot(T snapshot, U event);

    T popKey(long snapshotTime);

    U popValue(long snapshotTime);

    T getSnapshot(long snapshotTime);

    SortedMap<Long, Pair<T, U>> getAllSyncSnapshots();

    SortedMap<Long, U> getEventsHistoryStack(int from, int to);
}
