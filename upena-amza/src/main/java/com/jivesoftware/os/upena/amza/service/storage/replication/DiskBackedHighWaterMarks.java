package com.jivesoftware.os.upena.amza.service.storage.replication;

import com.jivesoftware.os.upena.amza.shared.HighwaterMarks;
import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;
import com.jivesoftware.os.upena.amza.shared.TableName;

public class DiskBackedHighWaterMarks implements HighwaterMarks {

    @Override
    public void clearRing(UpenaRingHost ringHost) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set(UpenaRingHost ringHost, TableName tableName, long highWatermark) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear(UpenaRingHost ringHost, TableName tableName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Long get(UpenaRingHost ringHost, TableName tableName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
