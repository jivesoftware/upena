package com.jivesoftware.os.upena.amza.service.storage.replication;

import com.jivesoftware.os.upena.amza.shared.HighwaterMarks;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.amza.shared.TableName;

public class DiskBackedHighWaterMarks implements HighwaterMarks {

    @Override
    public void clearRing(RingHost ringHost) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set(RingHost ringHost, TableName tableName, long highWatermark) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear(RingHost ringHost, TableName tableName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Long get(RingHost ringHost, TableName tableName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
