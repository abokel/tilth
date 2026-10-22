package com.claudacious.tilth.plot;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.sim.plot.PolygonMath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A surveyed field: a named polygon owned by a player. */
public final class Plot {
    private final UUID id;
    private String name;
    private final UUID owner;
    private final String ownerName;
    private final List<PolygonMath.Vertex> vertices;
    private final Set<UUID> members;
    private final long createdDay;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    public Plot(UUID id, String name, UUID owner, String ownerName, List<PolygonMath.Vertex> vertices, Set<UUID> members, long createdDay) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.ownerName = ownerName;
        this.vertices = List.copyOf(vertices);
        this.members = new HashSet<>(members);
        this.createdDay = createdDay;
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
        for (PolygonMath.Vertex v : vertices) {
            x0 = Math.min(x0, v.x());
            x1 = Math.max(x1, v.x());
            z0 = Math.min(z0, v.z());
            z1 = Math.max(z1, v.z());
        }
        this.minX = x0;
        this.maxX = x1;
        this.minZ = z0;
        this.maxZ = z1;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public List<PolygonMath.Vertex> vertices() {
        return vertices;
    }

    public Set<UUID> members() {
        return members;
    }

    public long createdDay() {
        return createdDay;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && PolygonMath.contains(vertices, x, z);
    }

    public boolean mayModify(UUID player) {
        return owner.equals(player) || members.contains(player);
    }

    public long blockCount() {
        return PolygonMath.blockCount(vertices);
    }

    public double areaSquareMetres() {
        return PolygonMath.areaSquareMetres(vertices, TilthConfig.METRES_PER_BLOCK.getAsDouble());
    }

    public double perimeterMetres() {
        return PolygonMath.perimeter(vertices, TilthConfig.METRES_PER_BLOCK.getAsDouble());
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putUUID("owner", owner);
        t.putString("owner_name", ownerName);
        int[] coords = new int[vertices.size() * 2];
        for (int i = 0; i < vertices.size(); i++) {
            coords[2 * i] = vertices.get(i).x();
            coords[2 * i + 1] = vertices.get(i).z();
        }
        t.putIntArray("vertices", coords);
        ListTag m = new ListTag();
        for (UUID u : members) {
            m.add(NbtUtils.createUUID(u));
        }
        t.put("members", m);
        t.putLong("created", createdDay);
        return t;
    }

    public static Plot load(CompoundTag t) {
        int[] coords = t.getIntArray("vertices");
        List<PolygonMath.Vertex> v = new ArrayList<>();
        for (int i = 0; i + 1 < coords.length; i += 2) {
            v.add(new PolygonMath.Vertex(coords[i], coords[i + 1]));
        }
        Set<UUID> members = new HashSet<>();
        ListTag m = t.getList("members", Tag.TAG_INT_ARRAY);
        for (Tag tag : m) {
            if (tag instanceof IntArrayTag) {
                members.add(NbtUtils.loadUUID(tag));
            }
        }
        return new Plot(t.getUUID("id"), t.getString("name"), t.getUUID("owner"), t.getString("owner_name"), v, members, t.getLong("created"));
    }
}
