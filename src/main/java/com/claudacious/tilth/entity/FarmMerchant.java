package com.claudacious.tilth.entity;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.genetics.SeedGenetics;
import com.claudacious.tilth.market.Market;
import com.claudacious.tilth.registry.TilthEntities;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.genetics.CropGenome;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Optional;

/**
 * The travelling farm merchant: a wandering trader in a straw hat who deals in farm goods. What he pays for produce
 * follows the market on the day he arrives, so he is worth catching after a drought or a blight. He also sells seed
 * of named cultivars bred for yield and hardiness, veterinary draughts, compost and tack. He turns up now and then
 * near a player and moves on after two days.
 */
public class FarmMerchant extends WanderingTrader {
    private record Cultivar(String species, String name) {
    }

    private static final List<Cultivar> CULTIVARS = List.of(new Cultivar("common_wheat", "Maris Widgeon"), new Cultivar("barley", "Golden Promise"),
            new Cultivar("oats", "Black Tartarian"), new Cultivar("maize", "Reid's Yellow Dent"), new Cultivar("rye", "Petkus"),
            new Cultivar("sunflower", "Mammoth Grey"), new Cultivar("tomato", "Brandywine"), new Cultivar("common_bean", "Cherokee Trail"));

    public FarmMerchant(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Market market = Market.get(server.getServer());
        // He buys: a shilling's worth of produce for a silver shilling, at today's prices.
        for (Item item : List.of(Items.WHEAT, Items.BEEF, Items.EGG, TilthItems.CHEESE.get(), TilthItems.BUTTER.get(),
                TilthItems.FLEECE.get(), TilthItems.WOOLLEN_CLOTH.get(), Items.HAY_BLOCK, Items.POTATO)) {
            double each = market.unitPrice(new ItemStack(item));
            if (each > 0) {
                int count = Math.max(1, Math.min(64, (int) Math.ceil(1.0 / each)));
                offers.add(new MerchantOffer(new ItemCost(item, count), new ItemStack(TilthItems.SILVER_SHILLING.get()), 12, 2, 0.05f));
            }
        }
        // He sells named cultivars: better growth and yield, some hardiness.
        for (int i = 0; i < 3; i++) {
            Cultivar c = CULTIVARS.get(random.nextInt(CULTIVARS.size()));
            com.claudacious.tilth.sim.crop.CropSpecies species = com.claudacious.tilth.sim.crop.Crops.byId(c.species);
            Item seed = species == null ? Items.AIR : BuiltInRegistries.ITEM.get(ResourceLocation.parse(species.seedItem));
            if (seed == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(seed, 8);
            java.util.Random r = new java.util.Random(random.nextLong());
            boolean dwarf = com.claudacious.tilth.sim.genetics.MatingSystem.dwarfingHelps(c.species);
            CropGenome bred;
            if (com.claudacious.tilth.sim.genetics.MatingSystem.of(c.species) == com.claudacious.tilth.sim.genetics.MatingSystem.OUTCROSS) {
                // Cross-pollinated crops are sold as F1 hybrids of two inbred lines: vigorous, but saved seed segregates.
                CropGenome a = CropGenome.of(c.species, c.name + " A", com.claudacious.tilth.sim.genetics.CropGenotype.pureLine(r, 0.7, true, false), 8);
                CropGenome b = CropGenome.of(c.species, c.name + " B", com.claudacious.tilth.sim.genetics.CropGenotype.pureLine(r, 0.7, false, false), 8);
                CropGenome f1 = com.claudacious.tilth.sim.genetics.GeneticsModel.cross(a, b, r);
                bred = CropGenome.of(c.species, c.name + " F1", f1.genotype(), 1);
            } else {
                // Self-pollinated crops are sold as true-breeding pure lines.
                bred = CropGenome.of(c.species, c.name, com.claudacious.tilth.sim.genetics.CropGenotype.pureLine(r, 0.75, r.nextBoolean(), dwarf), 8);
            }
            SeedGenetics.setGenome(stack, bred);
            offers.add(new MerchantOffer(new ItemCost(TilthItems.SILVER_SHILLING.get(), 4 + random.nextInt(4)), stack, 4, 5, 0.05f));
        }
        Item shilling = TilthItems.SILVER_SHILLING.get();
        offers.add(new MerchantOffer(new ItemCost(shilling, 2), new ItemStack(TilthItems.VETERINARY_DRAUGHT.get(), 2), 6, 3, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(shilling, 1), new ItemStack(TilthItems.COMPOST.get(), 8), 8, 1, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(shilling, 1), new ItemStack(Items.LEAD, 2), 6, 1, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(shilling, 6), Optional.of(new ItemCost(Items.LEATHER, 3)), new ItemStack(Items.SADDLE), 2, 5, 0.05f));
        // Change: he will break a crown into shillings, or make up a crown from nine.
        offers.add(new MerchantOffer(new ItemCost(TilthItems.GOLD_CROWN.get(), 1), new ItemStack(shilling, 9), 16, 0, 0.0f));
    }

    /** Now and then (by config), a merchant arrives near a player who has none nearby, and stays two days. */
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 24000 != 12000 || !TilthConfig.MARKET.getAsBoolean()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() || level.random.nextDouble() > TilthConfig.MERCHANT_CHANCE.getAsDouble()) {
            return;
        }
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        if (!level.getEntitiesOfClass(FarmMerchant.class, player.getBoundingBox().inflate(128)).isEmpty()) {
            return;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int r = 16 + level.random.nextInt(16);
            BlockPos column = player.blockPosition().offset((int) (Math.cos(angle) * r), 0, (int) (Math.sin(angle) * r));
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP) || !level.getFluidState(pos.below()).isEmpty()) {
                continue;
            }
            FarmMerchant merchant = TilthEntities.FARM_MERCHANT.get().spawn(level, pos, MobSpawnType.EVENT);
            if (merchant != null) {
                merchant.setDespawnDelay(48000);
                merchant.setWanderTarget(player.blockPosition());
                player.sendSystemMessage(Component.literal("A travelling farm merchant has come by, buying produce at market prices.")
                        .withStyle(ChatFormatting.GOLD));
            }
            return;
        }
    }
}
