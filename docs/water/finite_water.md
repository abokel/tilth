---
title: "Ponds & Finite Water"
parent: Water
nav_order: 1
---

# Ponds & Finite Water

By default water behaves as in vanilla: two sources make a third. For a harder game, set **water.sourceCreation** to INFINITE_BIOMES_ONLY so that only oceans and rivers regenerate, and turn on **water.evaporation** so small ponds dry up in hot weather.

Rain refills the land. During rain, water runs to the lowest point of each hollow. A closed hollow on slow-draining ground (clay, compacted soil, rock) fills block by block into a **pond**. Flat, poorly drained ground collects **puddles**.

All of this is configurable: where sources may regenerate, how fast ponds form and evaporate, and how large a hollow may fill. Pumps and wells are planned for a later release.
