package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigObject;
import kotlin.ranges.IntRange;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@ConfigObject
public class RangeConfig {
    @ConfigEntry(id = "min", translation = T_PREF + "rangeMin")
    public int min;
    @ConfigEntry(id = "max", translation = T_PREF + "rangeMax")
    public int max;

    public RangeConfig(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public IntRange range() {
        if (min > max)
            min = max;
        return new IntRange(min, max);
    }
}
