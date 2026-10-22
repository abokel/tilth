package com.claudacious.tilth.block;

import com.claudacious.tilth.sim.soil.SoilState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** NBT (de)serialisation of {@link SoilState}. */
public final class SoilNbt {
    private SoilNbt() {
    }

    public static CompoundTag write(SoilState s) {
        CompoundTag t = new CompoundTag();
        t.putDouble("n", s.nitrogen);
        t.putDouble("p", s.phosphorus);
        t.putDouble("k", s.potassium);
        t.putDouble("s", s.sulfur);
        t.putDouble("ca", s.calcium);
        t.putDouble("mg", s.magnesium);
        t.putDouble("ph", s.ph);
        t.putDouble("om", s.organicMatter);
        t.putDouble("theta", s.moisture);
        t.putDouble("compaction", s.compaction);
        t.putDouble("tilth", s.tilth);
        t.putDouble("residue", s.residue);
        t.putDouble("residue_n", s.residueNitrogen);
        t.putDouble("incorporated", s.incorporatedFraction);
        t.putDouble("snow", s.snowpack);
        t.putDouble("flush", s.tillageFlushDays);
        t.putDouble("ec", s.salinity);
        t.putDouble("water_table", s.waterTableDepth);
        t.putInt("waterlogged_days", s.waterloggedDays);
        ListTag history = new ListTag();
        for (String h : s.cropHistory) {
            history.add(StringTag.valueOf(h));
        }
        t.put("history", history);
        return t;
    }

    public static SoilState read(CompoundTag t) {
        SoilState s = new SoilState();
        s.nitrogen = t.getDouble("n");
        s.phosphorus = t.getDouble("p");
        s.potassium = t.getDouble("k");
        s.sulfur = t.getDouble("s");
        s.calcium = t.getDouble("ca");
        s.magnesium = t.getDouble("mg");
        s.ph = t.getDouble("ph");
        s.organicMatter = t.getDouble("om");
        s.moisture = t.getDouble("theta");
        s.compaction = t.getDouble("compaction");
        s.tilth = t.getDouble("tilth");
        s.residue = t.getDouble("residue");
        s.residueNitrogen = t.getDouble("residue_n");
        s.incorporatedFraction = t.getDouble("incorporated");
        s.snowpack = t.getDouble("snow");
        s.tillageFlushDays = t.getDouble("flush");
        s.salinity = t.getDouble("ec");
        s.waterTableDepth = t.getDouble("water_table");
        s.waterloggedDays = t.getInt("waterlogged_days");
        ListTag history = t.getList("history", Tag.TAG_STRING);
        for (int i = 0; i < history.size(); i++) {
            s.cropHistory.add(history.getString(i));
        }
        return s;
    }
}
