package com.claudacious.tilth.block;

import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;

/** Any block made of a specific soil: topsoil, subsoil, gley, grass-covered soil, farmland. */
public interface SoilLike {
    SoilTexture texture();

    SoilHorizon horizon();
}
