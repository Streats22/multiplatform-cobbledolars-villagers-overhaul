package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Compatibility layer to open CobbleDollars' bank screen.
 * CobbleDollars uses Cobblemon's network API; OpenBankPacket(merchantUUID) is sent to server.
 * When opening from our villager shop, we pass the entity's UUID (CobbleDollars may accept it for "bank at this entity" or ignore it for player bank).
 */
public final class CobbleDollarsBankCompat {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String OPEN_BANK_PACKET = "fr.harmex.cobbledollars.common.network.packets.c2s.OpenBankPacket";

    private static Constructor<?> openBankCtor;
    private static boolean resolved;

    private CobbleDollarsBankCompat() {
    }

    /**
     * Attempt to open the CobbleDollars bank. Uses reflection to send OpenBankPacket.
     *
     * @param entityUuid UUID of the entity we're trading with (villager/trader). CobbleDollars may use this or a nil UUID for player bank.
     * @return true if the packet was sent, false if CobbleDollars is not loaded or reflection failed.
     */
    public static boolean tryOpenBank(UUID entityUuid) {
        if (!CobbleDollarsIntegration.isModLoaded()) {
            LOGGER.debug("CobbleDollars not loaded, cannot open bank");
            return false;
        }
        if (!resolve()) {
            return false;
        }
        try {
            Object packet = openBankCtor.newInstance(entityUuid != null ? entityUuid : UUID.fromString("00000000-0000-0000-0000-000000000000"));
            Method sendToServer = packet.getClass().getMethod("sendToServer");
            sendToServer.invoke(packet);
            LOGGER.info("Sent CobbleDollars OpenBankPacket for entity {}", entityUuid);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("Failed to send CobbleDollars OpenBankPacket: {}", t.getMessage());
            return false;
        }
    }

    /**
     * Open bank using the entity ID from our shop screen. Resolves entity from client level.
     */
    public static boolean tryOpenBankFromVillagerId(int villagerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return false;
        Entity entity = mc.level.getEntity(villagerId);
        UUID uuid = entity != null ? entity.getUUID() : null;
        return tryOpenBank(uuid);
    }

    public static boolean isBankAvailable() {
        return CobbleDollarsIntegration.isModLoaded() && resolve();
    }

    private static boolean resolve() {
        if (resolved) return openBankCtor != null;
        resolved = true;
        try {
            Class<?> packetClass = Class.forName(OPEN_BANK_PACKET);
            openBankCtor = packetClass.getConstructor(UUID.class);
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Resolved CobbleDollars OpenBankPacket for bank integration");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("CobbleDollars OpenBankPacket not found (CobbleDollars may not be installed)");
            return false;
        } catch (Throwable t) {
            LOGGER.warn("Failed to resolve CobbleDollars OpenBankPacket: {}", t.getMessage());
            return false;
        }
    }
}
