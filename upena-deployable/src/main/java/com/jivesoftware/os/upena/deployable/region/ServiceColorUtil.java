package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author jonathan.colt
 */
public class ServiceColorUtil {

    public static Map<ServiceKey, Integer> serviceKeysOrder(UpenaStore upenaStore) throws Exception {
        ConcurrentNavigableMap<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> listServices = upenaStore.services.find(
            new ServiceFilter(null, null, 0, 100_000));
        int i = 0;

        Map<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> sort = new ConcurrentSkipListMap<>((ServiceKey o1, ServiceKey o2) -> {
            com.jivesoftware.os.upena.shared.Service so1 = listServices.get(o1).getValue();
            com.jivesoftware.os.upena.shared.Service so2 = listServices.get(o2).getValue();
            int c = so1.name.compareTo(so2.name);
            if (c != 0) {
                return c;
            }
            return o1.compareTo(o2);
        });
        sort.putAll(listServices);

        Map<ServiceKey, Integer> serviceNumber = new HashMap<>();
        for (Map.Entry<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> entrySet : sort.entrySet()) {
            if (!entrySet.getValue().getTombstoned()) {
                serviceNumber.put(entrySet.getKey(), i);
                i++;
            }
        }
        return serviceNumber;

    }

    public static Map<ServiceKey, String> serviceKeysColor(UpenaStore upenaStore) throws Exception {
        ConcurrentNavigableMap<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> listServices = upenaStore.services.find(
            new ServiceFilter(null, null, 0, 100_000));
        int i = 0;

        Map<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> sort = new ConcurrentSkipListMap<>((ServiceKey o1, ServiceKey o2) -> {
            com.jivesoftware.os.upena.shared.Service so1 = listServices.get(o1).getValue();
            com.jivesoftware.os.upena.shared.Service so2 = listServices.get(o2).getValue();
            int c = so1.name.compareTo(so2.name);
            if (c != 0) {
                return c;
            }
            return o1.compareTo(o2);
        });
        sort.putAll(listServices);

        Map<String, Integer> serviceNumber = new HashMap<>();
        for (Map.Entry<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> entrySet : sort.entrySet()) {
            if (!entrySet.getValue().getTombstoned()) {
                serviceNumber.put(entrySet.getValue().getValue().name, i);
                i++;
            }
        }

        Map<ServiceKey, String> serviceColor = new HashMap<>();
        for (Map.Entry<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> entrySet : sort.entrySet()) {
            if (!entrySet.getValue().getTombstoned()) {
                i = serviceNumber.get(entrySet.getValue().getValue().name);
                String idColor = idColorRGB((double) i / (double) serviceNumber.size(), 1f);
                serviceColor.put(entrySet.getKey(), idColor);
            }
        }

        return serviceColor;
    }

    public static String idColorRGB(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        float hue = (float) value / 3f;
        hue = (1f / 3f) + (hue * 2);
        Color color = new Color(Color.HSBtoRGB(hue, sat, 1f));
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }
}
