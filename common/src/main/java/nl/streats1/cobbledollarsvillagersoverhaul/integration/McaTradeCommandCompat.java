package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Soft helpers for MCA's {@code VillagerCommandHandler} (Trade button) without compile deps.
 */
public final class McaTradeCommandCompat {

    private McaTradeCommandCompat() {
    }

    public static boolean isTradeCommand(String command) {
        return "trade".equals(command);
    }

    /**
     * Redirect MCA Trade to the CobbleDollars shop. On success, stops MCA interaction UI.
     *
     * @param commandHandler MCA {@code VillagerCommandHandler} instance ({@code this} from mixin)
     * @return true if the trade command was handled (caller should cancel MCA handle)
     */
    public static boolean tryRedirectTrade(Object commandHandler, ServerPlayer player, String command) {
        if (!isTradeCommand(command) || commandHandler == null || player == null) {
            return false;
        }
        AbstractVillager villager = findVillagerEntity(commandHandler);
        if (villager == null) {
            return false;
        }
        if (!McaTradeRedirect.tryOpenCobbleDollarsShop(villager, player)) {
            return false;
        }
        stopInteracting(commandHandler);
        return true;
    }

    static AbstractVillager findVillagerEntity(Object commandHandler) {
        Object raw = findEntityFieldValue(commandHandler);
        return raw instanceof AbstractVillager villager ? villager : null;
    }

    static Object findEntityFieldValue(Object commandHandler) {
        if (commandHandler == null) {
            return null;
        }
        for (Class<?> c = commandHandler.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!"entity".equals(field.getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    return field.get(commandHandler);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static void stopInteracting(Object commandHandler) {
        if (commandHandler == null) {
            return;
        }
        for (Class<?> c = commandHandler.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod("stopInteracting");
                m.setAccessible(true);
                m.invoke(commandHandler);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    /**
     * When MCA's {@code canTradeWithProfession} cannot be reflected, avoid opening shop for
     * unemployed / nitwit villagers; otherwise allow (offers may be generated lazily).
     */
    static boolean fallbackCanTrade(Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return true;
        }
        try {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            return profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
