package com.javacravio.cravio.common.utils;

import com.uber.h3core.H3Core;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class H3Utils {

    private final H3Core h3Core;
    private final int resolution;

    public H3Utils(@Value("${cravio.h3.resolution}") int resolution) throws IOException {
        this.h3Core = H3Core.newInstance();
        this.resolution = resolution;
    }

    public String toCell(double latitude, double longitude) {
        return String.valueOf(h3Core.latLngToCell(latitude, longitude, resolution));
    }

    public Set<String> nearbyCells(String originCell, int ringSize) {
        long origin = Long.parseUnsignedLong(originCell);
        Set<String> cells = new HashSet<>(h3Core.gridDisk(origin, ringSize).stream()
                .map(String::valueOf)
                .toList());
        cells.add(originCell);
        return cells;
    }
}


