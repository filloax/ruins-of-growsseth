package com.ruslan.growsseth.interfaces;

import com.ruslan.growsseth.maps.CustomMapDecoration;

import java.util.List;
import java.util.Map;

public interface WithCustomDecorations {
    Iterable<CustomMapDecoration> getCustomDecorations();
    Map<String, CustomMapDecoration> getCustomDecorationsMap();
    void gr$addClientSideCustomDecorations(List<CustomMapDecoration> customDecorations);
}
