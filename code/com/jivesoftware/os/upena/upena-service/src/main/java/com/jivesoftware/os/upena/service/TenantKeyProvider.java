package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantKey;

public class TenantKeyProvider implements UpenaKeyProvider<TenantKey, Tenant> {

    @Override
    public TenantKey getNodeKey(UpenaTable<TenantKey, Tenant> table, Tenant value) {
        return new TenantKey(value.tenantId);
    }
}
