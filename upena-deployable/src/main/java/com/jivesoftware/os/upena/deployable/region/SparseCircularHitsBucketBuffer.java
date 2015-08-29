package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.util.Arrays;

/**
 * Supports storing a hits waveform. You can add hit @ time in any order.
 * The Buffer will slide to accommodate hits.
 *
 * NOT thread safe any more, synchronize externally.
 *
 * @author jonathan
 */
public class SparseCircularHitsBucketBuffer {

    private static final MetricLogger logger = MetricLoggerFactory.getLogger();

    private long mostRecentTimeStamp = Long.MIN_VALUE;
    private long oldestBucketNumber = Long.MIN_VALUE;
    private long youngestBucketNumber;
    private final long utcOffset; // shifts alignment of buckets as offset from UTC, e.g. aligning with start of work day in PST
    private final long bucketWidthMillis;
    private int cursor; // always points oldest bucket. cursor -1 is the newestBucket
    private final int numberOfBuckets;
    private final double[] hits;

    public SparseCircularHitsBucketBuffer(int numberOfBuckets, long utcOffset, long bucketWidthMillis) {
        this.numberOfBuckets = numberOfBuckets;
        this.utcOffset = utcOffset;
        this.bucketWidthMillis = bucketWidthMillis;
        hits = new double[numberOfBuckets];
        Arrays.fill(hits, Double.NaN);
    }

    public long mostRecentTimestamp() {
        return mostRecentTimeStamp;
    }

    public long duration() {
        return bucketWidthMillis * numberOfBuckets;
    }


    public void set(long time, double value) {
        if (time > mostRecentTimeStamp) {
            mostRecentTimeStamp = time;
        }
        long absBucketNumber = absBucketNumber(time);
        if (oldestBucketNumber == Long.MIN_VALUE) {
            oldestBucketNumber = absBucketNumber - (numberOfBuckets - 1);
            youngestBucketNumber = absBucketNumber;
        } else {
            if (absBucketNumber < oldestBucketNumber) {
                logger.debug("Moving backwards is unsupported so we will simply drop the hit on the floor!");
                return;
            }
            if (absBucketNumber > youngestBucketNumber) {
                // we need to slide the buffer to accommodate younger values
                long delta = absBucketNumber - youngestBucketNumber;
                for (int i = 0; i < delta; i++) {
                    hits[cursor] = 0; // zero out oldest
                    cursor = nextCursor(cursor, 1); // move cursor
                }
                oldestBucketNumber += delta;
                youngestBucketNumber = absBucketNumber;
            }
        }
        int delta = (int) (absBucketNumber - oldestBucketNumber);
        hits[nextCursor(cursor, delta)] = value;

    }

    private long absBucketNumber(long time) {
        long absBucketNumber = time / bucketWidthMillis;
        long absNearestEdge = bucketWidthMillis * absBucketNumber;
        long remainder = time - (absNearestEdge);
        if (remainder < utcOffset) {
            return absBucketNumber - 1;
        } else {
            return absBucketNumber;
        }
    }

    private int nextCursor(int cursor, int move) {
        cursor += move;
        if (cursor >= numberOfBuckets) {
            cursor -= numberOfBuckets;
        }
        return cursor;
    }

    public double[] rawSignal() {
        double[] copy = new double[numberOfBuckets];
        int c = cursor;
        double lastH = 0d;
        for (int i = 0; i < numberOfBuckets; i++) {
            double h = hits[c];
            copy[i] = Double.isNaN(h) ? lastH : h ;
            lastH = h;
            c = nextCursor(c, 1);
        }
        return copy;
    }

    @Override
    public String toString() {
        return "SparseCircularHitsBucketBuffer{" +
            "mostRecentTimeStamp=" + mostRecentTimeStamp +
            ", oldestBucketNumber=" + oldestBucketNumber +
            ", youngestBucketNumber=" + youngestBucketNumber +
            ", utcOffset=" + utcOffset +
            ", bucketWidthMillis=" + bucketWidthMillis +
            ", cursor=" + cursor +
            ", numberOfBuckets=" + numberOfBuckets +
            ", hits=" + Arrays.toString(hits) +
            '}';
    }
}
