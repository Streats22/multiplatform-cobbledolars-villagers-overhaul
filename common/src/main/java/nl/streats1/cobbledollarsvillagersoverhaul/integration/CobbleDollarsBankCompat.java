package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public final class CobbleDollarsBankCompat {

    private static final String OPEN_BANK_PACKET = "fr.harmex.cobbledollars.common.network.packets.c2s.OpenBankPacket";

    private static Constructor<?> openBankCtor;
    private static boolean resolved;

    private CobbleDollarsBankCompat() {
    }

        public static boolean tryOpenBank(UUID entityUuid) {
        if (!CobbleDollarsIntegration.isModLoaded()) {
            return false;
        }
        if (!resolve()) {
            return false;
        }
        try {
            Object packet = openBankCtor.newInstance(entityUuid != null ? entityUuid : UUID.fromString("00000000-0000-0000-0000-000000000000"));
            Method sendToServer = packet.getClass().getMethod("sendToServer");
            sendToServer.invoke(packet);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

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
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
