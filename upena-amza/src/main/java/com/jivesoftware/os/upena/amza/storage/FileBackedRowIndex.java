package com.jivesoftware.os.upena.amza.storage;

import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowScan;
import com.jivesoftware.os.upena.amza.shared.RowsIndex;
import com.jivesoftware.os.upena.amza.shared.TableName;
import com.jivesoftware.os.filer.io.AutoGrowingByteBufferBackedFiler;
import com.jivesoftware.os.filer.io.FileBackedMemMappedByteBufferFactory;
import com.jivesoftware.os.filer.io.FilerIO;
import com.jivesoftware.os.filer.io.api.KeyRange;
import com.jivesoftware.os.filer.io.api.StackBuffer;
import com.jivesoftware.os.filer.io.map.MapStore;
import com.jivesoftware.os.filer.io.map.SkipListMapContext;
import com.jivesoftware.os.filer.io.map.SkipListMapStore;
import com.jivesoftware.os.filer.map.store.LexSkipListComparator;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author jonathan.colt
 */
public class FileBackedRowIndex implements RowsIndex {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final TableName tableName;
    private final int keySize;
    private final boolean variableKeySizes;
    private final int payloadSize = 8 + 8 + 1; // fp, timetamp, tombstone
    private final int directoryOffset;
    private final File[] directories;
    private AutoGrowingByteBufferBackedFiler filer;
    private SkipListMapContext sls;

    public FileBackedRowIndex(TableName tableName,
        int keySize,
        boolean variableKeySizes,
        int directoryOffset,
        File... directories
    ) {

        this.tableName = tableName;
        this.keySize = keySize;
        this.variableKeySizes = variableKeySizes;
        this.directoryOffset = directoryOffset;
        this.directories = directories;
    }

    synchronized SkipListMapContext ensureCapacity(long roomForNMore) throws IOException {
        if (roomForNMore == 0 && filer == null) {
            return null;
        }

        if (filer == null) {
            String prefix = tableName.getTableName() + "-" + tableName.getRingName();
            byte[] headKey = new byte[keySize];
            Arrays.fill(headKey, Byte.MIN_VALUE);

            boolean compactedExists = false;
            for (File directory : directories) {
                File compactedDirectory = new File(directory, prefix + "/compacted");
                if (compactedDirectory.exists()) {
                    compactedExists = true;
                    break;
                }
            }

            if (compactedExists) {
                for (File directory : directories) {
                    File compactingDirectory = new File(directory, prefix + "/compacting/active");
                    if (compactingDirectory.exists()) {
                        FileUtils.moveDirectory(compactingDirectory, directory);
                    }
                    File compactedDirectory = new File(directory, prefix + "/compacted");
                    if (compactedDirectory.exists()) {
                        FileUtils.moveDirectory(compactedDirectory, directory);
                    }
                }
            }

            boolean coldStart = true;
            boolean fallbackToBackup = false;
            for (File directory : directories) {
                File activeDirectory = new File(directory, prefix + "/active");
                if (!activeDirectory.exists()) {
                    File newDirectory = new File(directory, prefix + "/growing");
                    if (newDirectory.exists()) {
                        FileUtils.moveDirectory(newDirectory, directory);
                        coldStart = false;
                    } else {
                        fallbackToBackup = true;
                        break;
                    }
                } else {
                    coldStart = false;
                }
            }

            if (fallbackToBackup) {
                int count = 0;
                for (File directory : directories) {
                    File backupDirectory = new File(directory, prefix + "/bkp");
                    if (backupDirectory.exists()) {
                        count++;
                    }
                }
                if (count == 0) {
                    coldStart = true;
                } else if (count == directories.length) {
                    for (File directory : directories) {
                        File backupDirectory = new File(directory, prefix + "/bkp");
                        File activeDirectory = new File(directory, prefix + "/active");
                        FileUtils.moveDirectory(backupDirectory, activeDirectory);
                    }
                    coldStart = false;
                } else {
                    LOG.error("Corrupt rowIndex for {}. Please rebuild.", tableName);
                    throw new RuntimeException("Your row index is corrupt. Please rebuild." + tableName);
                }
            }

            if (coldStart) {
                File[] activeDirectories = new File[directories.length];
                for (int i = 0; i < directories.length; i++) {
                    activeDirectories[i] = new File(directories[i], prefix + "/active");
                    activeDirectories[i].mkdirs();
                }

                long size = SkipListMapStore.INSTANCE.computeFilerSize((int) (2 + roomForNMore), keySize, variableKeySizes, payloadSize);
                FileBackedMemMappedByteBufferFactory factory = new FileBackedMemMappedByteBufferFactory(prefix, directoryOffset, activeDirectories);
                AutoGrowingByteBufferBackedFiler coldStartFiler = new AutoGrowingByteBufferBackedFiler(factory, 1024,
                    AutoGrowingByteBufferBackedFiler.MAX_BUFFER_SEGMENT_SIZE);

                SkipListMapContext slmc = SkipListMapStore.INSTANCE.create((int) (2 + roomForNMore), headKey, keySize, variableKeySizes, payloadSize,
                    LexSkipListComparator.cSingleton, coldStartFiler, new StackBuffer());
                coldStartFiler.seek(size);
                coldStartFiler.seek(0);

                filer = coldStartFiler;
                sls = slmc;

            } else {
                File[] activeDirectories = new File[directories.length];
                for (int i = 0; i < directories.length; i++) {
                    activeDirectories[i] = new File(directories[i], prefix + "/active");
                }

                FileBackedMemMappedByteBufferFactory factory = new FileBackedMemMappedByteBufferFactory(prefix, directoryOffset, activeDirectories);
                AutoGrowingByteBufferBackedFiler openFiler = new AutoGrowingByteBufferBackedFiler(factory, 1024,
                    AutoGrowingByteBufferBackedFiler.MAX_BUFFER_SEGMENT_SIZE);

                SkipListMapContext slmc = SkipListMapStore.INSTANCE.open(headKey, LexSkipListComparator.cSingleton, openFiler, new StackBuffer());
                long size = SkipListMapStore.INSTANCE.computeFilerSize(slmc.mapContext.maxCount, keySize, variableKeySizes, payloadSize);
                openFiler.seek(size);
                openFiler.seek(0);

                filer = openFiler;
                sls = slmc;
            }
        } else if (MapStore.INSTANCE.isFull(sls.mapContext)) {
            String prefix = tableName.getTableName() + "-" + tableName.getRingName();
            byte[] headKey = new byte[keySize];
            Arrays.fill(headKey, Byte.MIN_VALUE);

            int nextGrowSize = MapStore.INSTANCE.nextGrowSize(sls.mapContext, (int) roomForNMore);
            long newSize = SkipListMapStore.INSTANCE.computeFilerSize(nextGrowSize, keySize, variableKeySizes, payloadSize);

            File[] activeDirectories = new File[directories.length];
            File[] backupDirectories = new File[directories.length];
            File[] newDirectories = new File[directories.length];
            for (int i = 0; i < directories.length; i++) {
                activeDirectories[i] = new File(directories[i], prefix + "/active");
                backupDirectories[i] = new File(directories[i], prefix + "/bkp");
                newDirectories[i] = new File(directories[i], prefix + "/growing");
            }

            for (int i = 0; i < directories.length; i++) {
                FileUtils.deleteDirectory(newDirectories[i]);
                newDirectories[i].mkdirs();
            }

            FileBackedMemMappedByteBufferFactory newFactory = new FileBackedMemMappedByteBufferFactory(prefix, directoryOffset, newDirectories);
            AutoGrowingByteBufferBackedFiler newFiler = new AutoGrowingByteBufferBackedFiler(
                newFactory, newSize, AutoGrowingByteBufferBackedFiler.MAX_BUFFER_SEGMENT_SIZE);

            try {
                SkipListMapContext newSLS = SkipListMapStore.INSTANCE.create(nextGrowSize, headKey, keySize, variableKeySizes, payloadSize,
                    LexSkipListComparator.cSingleton, newFiler, new StackBuffer());
                SkipListMapStore.INSTANCE.copyTo(filer, sls, newFiler, newSLS, null, new StackBuffer());

            } catch (Exception x) {
                LOG.error("Failed to grow row index for " + tableName, x);
                try {
                    for (File dir : newDirectories) {
                        FileUtils.deleteDirectory(dir);
                    }
                } catch (Exception xx) {
                    LOG.error("Failed to cleanup after a failed grow " + tableName, xx);
                    throw xx;
                }
                throw x;
            }
            filer.close();
            filer = null;
            sls = null;
            newFiler.close();

            try {
                for (int i = 0; i < directories.length; i++) {
                    FileUtils.moveDirectory(activeDirectories[i], backupDirectories[i]);
                }

                for (int i = 0; i < directories.length; i++) {
                    FileUtils.moveDirectory(newDirectories[i], activeDirectories[i]);
                }

                for (int i = 0; i < directories.length; i++) {
                    FileUtils.deleteDirectory(backupDirectories[i]);
                }
            } catch (Exception x) {
                LOG.error("Failed to suffle the dirs after a grow.", x);
                throw x;
            }

            FileBackedMemMappedByteBufferFactory factory = new FileBackedMemMappedByteBufferFactory(prefix, directoryOffset, activeDirectories);
            AutoGrowingByteBufferBackedFiler reOpenFiler = new AutoGrowingByteBufferBackedFiler(factory, 1024,
                AutoGrowingByteBufferBackedFiler.MAX_BUFFER_SEGMENT_SIZE);
            SkipListMapContext slmc = SkipListMapStore.INSTANCE.open(headKey, LexSkipListComparator.cSingleton, reOpenFiler, new StackBuffer());
            long size = SkipListMapStore.INSTANCE.computeFilerSize(slmc.mapContext.maxCount, keySize, variableKeySizes, payloadSize);
            reOpenFiler.seek(size);
            reOpenFiler.seek(0);

            filer = reOpenFiler;
            sls = slmc;
        }
        return sls;

    }

    byte[] ser(RowIndexValue value) {
        ByteBuffer bb = ByteBuffer.allocate(payloadSize);
        if (value.getValue().length != 8) {
            throw new RuntimeException("This is expected to be a FP");
        }
        bb.put(value.getValue());
        bb.putLong(value.getTimestampId());
        bb.put(value.getTombstoned() ? (byte) 1 : 0);

        return bb.array();
    }

    RowIndexValue der(byte[] value) {
        ByteBuffer bb = ByteBuffer.wrap(value);
        return new RowIndexValue(FilerIO.longBytes(bb.getLong()), bb.getLong(), bb.get() == 1 ? true : false);
    }

    @Override
    synchronized public void put(Collection<? extends Map.Entry<RowIndexKey, RowIndexValue>> entries) {
        try {
            SkipListMapContext slmc = ensureCapacity(entries.size());
            for (Map.Entry<RowIndexKey, RowIndexValue> entry : entries) {
                SkipListMapStore.INSTANCE.add(filer, slmc, entry.getKey().getKey(), ser(entry.getValue()), new StackBuffer());
            }
        } catch (Exception x) {
            throw new RuntimeException("Failure while putting.", x);
        }
    }

    @Override
    synchronized public List<RowIndexValue> get(List<RowIndexKey> keys) {
        try {
            SkipListMapContext slmc = ensureCapacity(0);
            List<RowIndexValue> gots = new ArrayList<>(keys.size());
            if (slmc != null) {
                for (RowIndexKey key : keys) {
                    byte[] got = SkipListMapStore.INSTANCE.getExistingPayload(filer, sls, key.getKey(), new StackBuffer());
                    if (got != null) {
                        gots.add(der(got));
                    } else {
                        gots.add(null);
                    }
                }
            } else {
                gots.addAll(Collections.<RowIndexValue>nCopies(keys.size(), null));
            }
            return gots;

        } catch (Exception x) {
            throw new RuntimeException("Failure while putting.", x);
        }
    }

    @Override
    synchronized public List<Boolean> containsKey(List<RowIndexKey> keys) {
        try {
            SkipListMapContext slmc = ensureCapacity(0);
            List<Boolean> contains = new ArrayList<>(keys.size());
            if (slmc == null) {
                contains.addAll(Collections.<Boolean>nCopies(keys.size(), Boolean.FALSE));
            } else {
                for (RowIndexKey key : keys) {
                    contains.add(MapStore.INSTANCE.contains(filer, sls.mapContext, key.getKey(), new StackBuffer()));
                }
            }
            return contains;
        } catch (Exception x) {
            throw new RuntimeException("Failure while putting.", x);
        }
    }

    @Override
    synchronized public void remove(Collection<RowIndexKey> keys) {
        try {
            SkipListMapContext slmc = ensureCapacity(0);
            if (slmc != null) {
                for (RowIndexKey key : keys) {
                    SkipListMapStore.INSTANCE.remove(filer, sls, key.getKey(), new StackBuffer());
                }
            }
        } catch (Exception x) {
            throw new RuntimeException("Failure while putting.", x);
        }
    }

    @Override
    synchronized public boolean isEmpty() {
        try {
            SkipListMapContext slmc = ensureCapacity(0);
            if (slmc != null) {
                return MapStore.INSTANCE.getCount(filer, new StackBuffer()) == 0;
            } else {
                return true;
            }
        } catch (Exception x) {
            throw new RuntimeException("Failure while putting.", x);
        }
    }

    @Override
    synchronized public void clear() {
        // TODO remove file from FS
    }

    @Override
    synchronized public void commit() {

    }

    @Override
    synchronized public void compact() {

    }

    @Override
    synchronized public <E extends Exception> void rowScan(final RowScan<E> rowScan) throws E {
        try {
            SkipListMapContext slmc = ensureCapacity(0);
            if (slmc != null) {
                SkipListMapStore.INSTANCE.streamKeys(filer, slmc, slmc, null, new MapStore.KeyStream() {

                    @Override
                    public boolean stream(byte[] key) throws IOException {
                        byte[] got = SkipListMapStore.INSTANCE.getExistingPayload(filer, sls, key, new StackBuffer());
                        if (got != null) {
                            RowIndexValue value = der(got);
                            try {
                                return rowScan.row(-1, new RowIndexKey(key), value);
                            } catch (Exception e) {
                                throw new RuntimeException("Error in rowScan.", e);
                            }
                        }
                        return true;
                    }

                }, new StackBuffer());
            }
        } catch (Exception x) {
            throw new RuntimeException("streamKeys failure:", x);
        }
    }

    @Override
    synchronized public <E extends Exception> void rangeScan(RowIndexKey from, RowIndexKey to, final RowScan<E> rowScan) throws E {
        try {
            List<KeyRange> ranges = Collections.singletonList(new KeyRange(from.getKey(), to.getKey()));
            SkipListMapContext slmc = ensureCapacity(0);
            if (slmc != null) {
                SkipListMapStore.INSTANCE.streamKeys(filer, sls, sls, ranges, new MapStore.KeyStream() {

                    @Override
                    public boolean stream(byte[] key) throws IOException {
                        byte[] got = SkipListMapStore.INSTANCE.getExistingPayload(filer, sls, key, new StackBuffer());
                        if (got != null) {
                            RowIndexValue value = der(got);
                            try {
                                return rowScan.row(-1, new RowIndexKey(key), value);
                            } catch (Exception e) {
                                throw new RuntimeException("Error in rangeScan.", e);
                            }
                        }
                        return true;
                    }

                }, new StackBuffer());
            }
        } catch (Exception x) {
            throw new RuntimeException("streamKeys failure:", x);
        }
    }

    @Override
    public CompactionRowIndex startCompaction() throws Exception {
        final String prefix = tableName.getTableName() + "-" + tableName.getRingName();
        final File[] compactingDirectories = new File[directories.length];
        final File[] compactedDirectories = new File[directories.length];
        for (int i = 0; i < directories.length; i++) {
            compactedDirectories[i] = new File(directories[i], prefix + "/compacted");
            if (compactedDirectories[i].exists()) {
                FileUtils.deleteDirectory(compactedDirectories[i]);
            }
            compactingDirectories[i] = new File(directories[i], prefix + "/compacting");
            if (compactingDirectories[i].exists()) {
                FileUtils.deleteDirectory(compactingDirectories[i]);
            }
            compactingDirectories[i].mkdirs();
        }

        final FileBackedRowIndex fileBackedRowIndex = new FileBackedRowIndex(tableName,
            keySize,
            variableKeySizes,
            directoryOffset,
            compactingDirectories
        );

        synchronized (this) {
            if (filer != null) {
                fileBackedRowIndex.ensureCapacity(MapStore.INSTANCE.getCount(filer, new StackBuffer()));
            }
        }

        return new CompactionRowIndex() {

            @Override
            public void put(Collection<? extends Map.Entry<RowIndexKey, RowIndexValue>> entries) {
                fileBackedRowIndex.put(entries);
            }

            @Override
            public void abort() {
                for (File compactingDirectory : compactingDirectories) {
                    try {
                        FileUtils.deleteDirectory(compactingDirectory);
                    } catch (Exception x) {
                        LOG.error("Failed to remove compactingDirectories:" + compactingDirectory);
                    }
                }
            }

            @Override
            public void commit() throws Exception {
                synchronized (FileBackedRowIndex.this) {

                    if (filer != null) {
                        filer.close();
                        filer = null;
                        sls = null;
                    }

                    for (File directory : directories) {
                        File active = new File(directory, prefix + "/active");
                        FileUtils.deleteDirectory(active);
                    }

                    for (int i = 0; i < directories.length; i++) {
                        File active = new File(compactingDirectories[i], prefix + "/active");
                        FileUtils.moveDirectory(active, compactedDirectories[i]);
                    }

                    for (int i = 0; i < directories.length; i++) {
                        File active = new File(directories[i], prefix + "/active");
                        FileUtils.moveDirectory(compactedDirectories[i], active);
                    }

                    ensureCapacity(1);

                }
            }
        };
    }
}
