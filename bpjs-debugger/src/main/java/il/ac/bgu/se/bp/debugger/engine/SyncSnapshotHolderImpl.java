package il.ac.bgu.se.bp.debugger.engine;

import il.ac.bgu.cs.bp.bpjs.model.*;
import il.ac.bgu.se.bp.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class SyncSnapshotHolderImpl implements SyncSnapshotHolder<BProgramSyncSnapshot, BEvent> {

    private SortedMap<Long, Pair<BProgramSyncSnapshot, BEvent>> snapshotsByTimeChosen = new TreeMap<>();

    @Override
    public synchronized void addSyncSnapshot(BProgramSyncSnapshot snapshot, BEvent event) {
        if (snapshot == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        snapshotsByTimeChosen.put(currentTime, Pair.of(snapshot, event));
    }

    @Override
    public synchronized BProgramSyncSnapshot popKey(long snapshotTime) {
        if (!snapshotsByTimeChosen.containsKey(snapshotTime)) {
            return null;
        }

        BProgramSyncSnapshot oldBProgramSyncSnapshot = snapshotsByTimeChosen.get(snapshotTime).getLeft();
        snapshotsByTimeChosen = cloneTreeMap(snapshotsByTimeChosen.headMap(snapshotTime));

        return cloneBProgramSyncSnapshot(oldBProgramSyncSnapshot);
    }

    @Override
    public synchronized BEvent popValue(long snapshotTime) {
        if (!snapshotsByTimeChosen.containsKey(snapshotTime)) {
            return null;
        }

        return snapshotsByTimeChosen.get(snapshotTime).getRight();
    }

    @Override
    public synchronized BProgramSyncSnapshot getSnapshot(long snapshotTime) {
        if (!snapshotsByTimeChosen.containsKey(snapshotTime)) {
            return null;
        }
        return cloneBProgramSyncSnapshot(snapshotsByTimeChosen.get(snapshotTime).getLeft());
    }

    private BProgramSyncSnapshot cloneBProgramSyncSnapshot(BProgramSyncSnapshot oldBProgramSyncSnapshot) {
        BProgram aBProgram = oldBProgramSyncSnapshot.getBProgram();
        Set<BThreadSyncSnapshot> someThreadSnapshots = oldBProgramSyncSnapshot.getBThreadSnapshots();
        List<BEvent> someExternalEvents = oldBProgramSyncSnapshot.getExternalEvents();
        SafetyViolationTag violationTag = oldBProgramSyncSnapshot.getViolationTag();
        Map<String, Object> dataStore = oldBProgramSyncSnapshot.getDataStore();
        return new BProgramSyncSnapshot(aBProgram, someThreadSnapshots, dataStore, someExternalEvents, violationTag);
    }

    @Override
    public SortedMap<Long, Pair<BProgramSyncSnapshot, BEvent>> getAllSyncSnapshots() {
        return cloneTreeMap(snapshotsByTimeChosen);
    }

    @Override
    public SortedMap<Long, BEvent> getEventsHistoryStack(int from, int to) {
        SortedMap<Long, BEvent> events = new TreeMap<>(Collections.reverseOrder());
        if (snapshotsByTimeChosen.isEmpty() || from > snapshotsByTimeChosen.size() || from > to) {
            return events;
        }
        List<BEvent> eventsHistory = snapshotsByTimeChosen.values().stream().map(Pair::getRight)
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<Long> eventsTime = snapshotsByTimeChosen.keySet().stream().skip(1).collect(Collectors.toList());

        Collections.reverse(eventsHistory);
        Collections.reverse(eventsTime);
        int startIdx = Math.max(from, 0);
        int endIdx = to > eventsHistory.size() ? eventsHistory.size() : Math.max(to, 0);
        for (int i = startIdx; i < endIdx; i++) {
            events.put(eventsTime.get(i), eventsHistory.get(i));
        }
        return events;
    }

    private TreeMap<Long, Pair<BProgramSyncSnapshot, BEvent>> cloneTreeMap(
            SortedMap<Long, Pair<BProgramSyncSnapshot, BEvent>> treeMap) {
        return new TreeMap<>(treeMap);
    }
}
