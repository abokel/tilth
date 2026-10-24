package com.claudacious.tilth.item;

import com.claudacious.tilth.command.Reports;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.world.ClimateService;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A thermometer. Held, it shows the air temperature now in the action bar; used, it gives today's low and
 * high, frost risk and tomorrow's forecast. Temperatures follow the Tilth climate through the day: lowest at
 * dawn, highest in mid-afternoon.
 */
public class ThermometerItem extends Item {
    public ThermometerItem(Properties properties) {
        super(properties);
    }

    /** Air temperature now: a cosine rise from the low at dawn (06:00) to the high at 14:00, then a slower fall. */
    public static double airNow(DailyWeather w, long dayTime) {
        double hour = ((dayTime % 24000) / 1000.0 + 6.0) % 24.0;
        double range = w.tMax() - w.tMin();
        if (hour >= 6 && hour < 14) {
            return w.tMin() + range * (1 - Math.cos(Math.PI * (hour - 6) / 8.0)) / 2.0;
        }
        double since = (hour - 14 + 24) % 24;
        return w.tMax() - range * (1 - Math.cos(Math.PI * since / 16.0)) / 2.0;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server) || player.tickCount % 20 != 0
                || (!selected && player.getOffhandItem() != stack)) {
            return;
        }
        if (!server.dimensionType().hasSkyLight()) {
            player.displayClientMessage(Component.literal("The thermometer is off the scale here").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        BlockPos pos = player.blockPosition();
        DailyWeather w = ClimateService.today(server, pos);
        double now = airNow(w, server.getDayTime());
        player.displayClientMessage(Component.literal(String.format("%.1f °C", now)).withStyle(color(now))
                .append(Component.literal("  (today " + whole(w.tMin()) + " to " + whole(w.tMax()) + " °C)").withStyle(ChatFormatting.GRAY)), true);
    }

    /** Rounded to whole degrees, never "-0". */
    private static String whole(double t) {
        return String.valueOf(Math.round(t) + 0L);
    }

    private static ChatFormatting color(double t) {
        return t < 0 ? ChatFormatting.AQUA : t < 10 ? ChatFormatting.DARK_AQUA : t < 25 ? ChatFormatting.GREEN : t < 32 ? ChatFormatting.GOLD
                : ChatFormatting.RED;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && server.dimensionType().hasSkyLight()) {
            BlockPos pos = player.blockPosition();
            long day = TilthCalendar.today(server);
            DailyWeather w = ClimateService.today(server, pos);
            DailyWeather tomorrow = ClimateService.weather(server, pos, ClimateService.climateAt(server, pos), day + 1);
            double now = airNow(w, server.getDayTime());
            player.sendSystemMessage(Reports.header("Thermometer"));
            player.sendSystemMessage(Reports.line("Air now", String.format("%.1f °C", now)));
            player.sendSystemMessage(Reports.line("Today", String.format("low %.1f °C at dawn, high %.1f °C mid-afternoon", w.tMin(), w.tMax())));
            if (w.tMin() < 0) {
                player.sendSystemMessage(Reports.note(w.tMin() < -2 ? "Hard frost: tender crops will be killed." : "Light frost: tender crops are at risk."));
            }
            player.sendSystemMessage(Reports.line("Tomorrow", String.format("%s to %s °C%s", whole(tomorrow.tMin()), whole(tomorrow.tMax()),
                    tomorrow.precipitation() > 0.5 ? String.format(", %.0f mm %s", tomorrow.precipitation(), tomorrow.snow() > tomorrow.rain() ? "snow" : "rain")
                            : ", dry")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Hold to read the air temperature; use for today's range and tomorrow").withStyle(ChatFormatting.GRAY));
    }
}
