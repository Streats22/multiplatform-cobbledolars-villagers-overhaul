package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

public final class CobbleDollarsIntegration {

    private static final String COBBLEDOLLARS_MOD_ID = "cobbledollars";

    private static Boolean modLoaded = null;
    private static MethodHandle getBalanceHandle = null;
    private static MethodHandle setBalanceHandle = null;
    private static MethodHandle addBalanceHandle = null;
    private static MethodHandle removeBalanceHandle = null;
    private static boolean reflectionResolved = false;
    private static boolean apiLogged = false;

    private CobbleDollarsIntegration() {}

    public static boolean isModLoaded() {
        if (modLoaded == null) {
            try {
                modLoaded = detectModLoaded();
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("CobbleDollars mod loaded check: {}", modLoaded);
            } catch (Throwable t) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.error("Error checking if CobbleDollars is loaded", t);
                modLoaded = false;
            }
        }
        return modLoaded;
    }

    private static boolean detectModLoaded() {
        // NeoForge / Forge
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, COBBLEDOLLARS_MOD_ID);
            if (loaded instanceof Boolean b) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("NeoForge ModList check for {}: {}", COBBLEDOLLARS_MOD_ID, b);
                return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, COBBLEDOLLARS_MOD_ID);
            if (loaded instanceof Boolean b) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Forge ModList check for {}: {}", COBBLEDOLLARS_MOD_ID, b);
                return b;
            }
        } catch (Throwable ignored) {
        }

        // Fabric
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object loaded = fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(loader, COBBLEDOLLARS_MOD_ID);
            if (loaded instanceof Boolean b) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Fabric Loader check for {}: {}", COBBLEDOLLARS_MOD_ID, b);
                return b;
            }
        } catch (Throwable ignored) {
        }

        CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Could not detect if CobbleDollars is loaded via any platform");
        return false;
    }

    private static void resolveReflection() {
        if (reflectionResolved) return;
        reflectionResolved = true;
        if (!isModLoaded()) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("CobbleDollars mod not loaded, skipping reflection resolution");
            return;
        }

        CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== Resolving CobbleDollars API via reflection ===");

        String[] possibleClassNames = {
                "fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt",
                "fr.harmex.cobbledollars.api.CobbleDollarsAPI",
                "fr.harmex.cobbledollars.common.CobbleDollars",
                "fr.harmex.cobbledollars.common.utils.MoneyUtil",
                "com.harmex.cobbledollars.CobbleDollars",
                "com.harmex.cobbledollars.api.CobbleDollarsAPI"
        };

        for (String className : possibleClassNames) {
            try {
                Class<?> apiClass = Class.forName(className);
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Found CobbleDollars API class: {}", className);
                
                // Log ALL available methods once for debugging
                if (!apiLogged) {
                    apiLogged = true;
                    CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== All static methods in {} ===", className);
                    for (Method m : apiClass.getMethods()) {
                        if (Modifier.isStatic(m.getModifiers())) {
                            StringBuilder params = new StringBuilder();
                            for (Class<?> p : m.getParameterTypes()) {
                                if (params.length() > 0) params.append(", ");
                                params.append(p.getSimpleName());
                            }
                            CobbleDollarsVillagersOverhaulRca.LOGGER.info("  Method: {}({}) -> {}", 
                                m.getName(), params, m.getReturnType().getSimpleName());
                        }
                    }
                    CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== End of methods ===");
                }
                
                Class<?> bigIntegerClass = BigInteger.class;
                for (Method m : apiClass.getMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() >= 1) {
                        String name = m.getName();
                        Class<?>[] params = m.getParameterTypes();
                        
                        // Look for getBalance/getCobbleDollars
                        if ((name.equals("getCobbleDollars") || name.equals("getBalance") || name.equals("get")) && params.length == 1) {
                            if (Player.class.isAssignableFrom(params[0]) && 
                                (m.getReturnType() == long.class || m.getReturnType() == int.class || m.getReturnType() == bigIntegerClass)) {
                                getBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Resolved getBalance: {} -> {}", m, m.getReturnType().getSimpleName());
                            }
                        }
                        
                        // Look for setBalance/setCobbleDollars
                        if ((name.equals("setCobbleDollars") || name.equals("setBalance") || name.equals("set")) && params.length == 2) {
                            if (Player.class.isAssignableFrom(params[0]) && 
                                (params[1] == long.class || params[1] == int.class || params[1] == bigIntegerClass)) {
                                setBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Resolved setBalance: {}", m);
                            }
                        }
                        
                        // Look for addBalance/addCobbleDollars/add
                        if ((name.equals("addCobbleDollars") || name.equals("addBalance") || name.equals("add") || 
                             name.equals("giveCobbleDollars") || name.equals("give")) && params.length == 2) {
                            if (Player.class.isAssignableFrom(params[0]) && 
                                (params[1] == long.class || params[1] == int.class || params[1] == bigIntegerClass)) {
                                addBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Resolved addBalance: {}", m);
                            }
                        }
                        
                        // Look for removeBalance/removeCobbleDollars/subtract
                        if ((name.equals("removeCobbleDollars") || name.equals("removeBalance") || name.equals("remove") || 
                             name.equals("subtractCobbleDollars") || name.equals("subtract") || 
                             name.equals("takeCobbleDollars") || name.equals("take")) && params.length == 2) {
                            if (Player.class.isAssignableFrom(params[0]) && 
                                (params[1] == long.class || params[1] == int.class || params[1] == bigIntegerClass)) {
                                removeBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Resolved removeBalance: {}", m);
                            }
                        }
                    }
                }
                
                // If we found get and set, we're good
                if (getBalanceHandle != null && setBalanceHandle != null) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.info("Successfully resolved CobbleDollars API from: {}", className);
                    break;
                }
            } catch (ClassNotFoundException e) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.debug("Class not found: {}", className);
            } catch (Throwable t) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Error resolving class {}: {}", className, t.getMessage());
            }
        }

        CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== CobbleDollars API Resolution Summary ===");
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("  getBalance: {}", getBalanceHandle != null ? "FOUND" : "NOT FOUND");
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("  setBalance: {}", setBalanceHandle != null ? "FOUND" : "NOT FOUND");
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("  addBalance: {}", addBalanceHandle != null ? "FOUND" : "NOT FOUND");
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("  removeBalance: {}", removeBalanceHandle != null ? "FOUND" : "NOT FOUND");
        
        if (getBalanceHandle == null || setBalanceHandle == null) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("CobbleDollars mod is loaded but balance API could not be resolved! Villager CobbleDollars payment will be disabled.");
        }
    }

    public static long getBalance(Player player) {
        if (!isModLoaded()) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("getBalance called but CobbleDollars mod not loaded");
            return -1;
        }
        if (!(player instanceof ServerPlayer)) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("getBalance called with non-ServerPlayer: {}", player.getClass().getName());
            return -1;
        }
        
        resolveReflection();
        
        if (getBalanceHandle == null) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("getBalance handle is null - API not resolved");
            return -1;
        }
        
        try {
            Object result = getBalanceHandle.invoke(player);
            long balance;
            if (result instanceof BigInteger bi) {
                if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                    balance = Long.MAX_VALUE;
                } else {
                    balance = bi.longValue();
                }
            } else if (result instanceof Number n) {
                balance = n.longValue();
            } else {
                CobbleDollarsVillagersOverhaulRca.LOGGER.error("getBalance returned unexpected type: {}", result != null ? result.getClass().getName() : "null");
                return -1;
            }
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("getBalance for {}: {}", player.getName().getString(), balance);
            return balance;
        } catch (Throwable t) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("CobbleDollars getBalance failed", t);
            return -1;
        }
    }

    /**
     * Add (or remove if negative) balance from player.
     * Tries to use dedicated add/remove methods first, falls back to get+set.
     */
    public static boolean addBalance(Player player, long amount) {
        if (!isModLoaded()) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("addBalance called but CobbleDollars mod not loaded");
            return false;
        }
        if (!(player instanceof ServerPlayer)) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("addBalance called with non-ServerPlayer: {}", player.getClass().getName());
            return false;
        }
        
        resolveReflection();
        
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== addBalance START for {} amount={} ===", player.getName().getString(), amount);
        
        // Get current balance for logging
        long balanceBefore = getBalance(player);
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Balance BEFORE: {}", balanceBefore);
        
        boolean success = false;
        
        // Try dedicated add/remove methods first (they may handle persistence better)
        if (amount > 0 && addBalanceHandle != null) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Using addBalanceHandle to add {}", amount);
            try {
                Class<?> secondParam = addBalanceHandle.type().parameterType(1);
                Object result;
                if (secondParam == BigInteger.class) {
                    result = addBalanceHandle.invoke(player, BigInteger.valueOf(amount));
                } else if (secondParam == long.class) {
                    result = addBalanceHandle.invoke(player, amount);
                } else {
                    result = addBalanceHandle.invoke(player, (int) amount);
                }
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("addBalanceHandle result: {}", result);
                success = true;
            } catch (Throwable t) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.error("addBalanceHandle failed, falling back to set", t);
            }
        } else if (amount < 0 && removeBalanceHandle != null) {
            long absAmount = Math.abs(amount);
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Using removeBalanceHandle to remove {}", absAmount);
            try {
                Class<?> secondParam = removeBalanceHandle.type().parameterType(1);
                Object result;
                if (secondParam == BigInteger.class) {
                    result = removeBalanceHandle.invoke(player, BigInteger.valueOf(absAmount));
                } else if (secondParam == long.class) {
                    result = removeBalanceHandle.invoke(player, absAmount);
                } else {
                    result = removeBalanceHandle.invoke(player, (int) absAmount);
                }
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("removeBalanceHandle result: {}", result);
                success = true;
            } catch (Throwable t) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.error("removeBalanceHandle failed, falling back to set", t);
            }
        }
        
        // Fallback to get+set if dedicated methods aren't available or failed
        if (!success) {
            if (getBalanceHandle == null || setBalanceHandle == null) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.error("Cannot addBalance - handles are null (get={}, set={})", 
                    getBalanceHandle != null, setBalanceHandle != null);
                return false;
            }
            
            try {
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Using get+set fallback for amount={}", amount);
                
                Object current = getBalanceHandle.invoke(player);
                BigInteger currentBi = current instanceof BigInteger ? (BigInteger) current : BigInteger.valueOf(((Number) current).longValue());
                BigInteger newBalance = currentBi.add(BigInteger.valueOf(amount));
                
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("Calculated new balance: {} + {} = {}", currentBi, amount, newBalance);
                
                if (newBalance.signum() < 0) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.warn("New balance would be negative ({}), rejecting", newBalance);
                    return false;
                }
                
                Class<?> secondParam = setBalanceHandle.type().parameterType(1);
                CobbleDollarsVillagersOverhaulRca.LOGGER.info("setBalanceHandle expects param type: {}", secondParam.getSimpleName());
                
                if (secondParam == BigInteger.class) {
                    CobbleDollarsVillagersOverhaulRca.LOGGER.info("Calling setBalance with BigInteger: {}", newBalance);
                    setBalanceHandle.invoke(player, newBalance);
                } else {
                    long clamped = newBalance.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : newBalance.longValue();
                    if (secondParam == long.class) {
                        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Calling setBalance with long: {}", clamped);
                        setBalanceHandle.invoke(player, clamped);
                    } else {
                        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Calling setBalance with int: {}", (int) clamped);
                        setBalanceHandle.invoke(player, (int) clamped);
                    }
                }
                success = true;
            } catch (Throwable t) {
                CobbleDollarsVillagersOverhaulRca.LOGGER.error("CobbleDollars addBalance (get+set) failed", t);
                return false;
            }
        }
        
        // Verify the balance actually changed
        long balanceAfter = getBalance(player);
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Balance AFTER: {}", balanceAfter);
        
        long expectedBalance = balanceBefore + amount;
        if (balanceAfter != expectedBalance) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("!!! BALANCE MISMATCH !!! Expected: {}, Actual: {}, Difference: {}", 
                expectedBalance, balanceAfter, balanceAfter - expectedBalance);
        } else {
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Balance change verified successfully: {} -> {}", balanceBefore, balanceAfter);
        }
        
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== addBalance END success={} ===", success);
        return success;
    }

    public static boolean isAvailable() {
        if (!isModLoaded()) return false;
        resolveReflection();
        boolean available = getBalanceHandle != null && setBalanceHandle != null;
        CobbleDollarsVillagersOverhaulRca.LOGGER.debug("CobbleDollars isAvailable: {}", available);
        return available;
    }
}
