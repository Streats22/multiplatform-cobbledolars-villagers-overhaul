package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

public final class CobbleDollarsIntegration {

    private static final String COBBLEDOLLARS_MOD_ID = "cobbledollars";

    private static Boolean modLoaded = null;
    private static MethodHandle getBalanceHandle = null;
    private static MethodHandle setBalanceHandle = null;
    private static MethodHandle addBalanceHandle = null;
    private static MethodHandle removeBalanceHandle = null;
    private static boolean reflectionResolved = false;

    private CobbleDollarsIntegration() {}

    public static boolean isModLoaded() {
        if (modLoaded == null) {
            try {
                modLoaded = detectModLoaded();
            } catch (Throwable t) {
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
                return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, COBBLEDOLLARS_MOD_ID);
            if (loaded instanceof Boolean b) {
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
                return b;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static void resolveReflection() {
        if (reflectionResolved) return;
        reflectionResolved = true;
        if (!isModLoaded()) {
            return;
        }

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
                            }
                        }

                        if ((name.equals("setCobbleDollars") || name.equals("setBalance") || name.equals("set")) && params.length == 2) {
                            if (Player.class.isAssignableFrom(params[0]) &&
                                (params[1] == long.class || params[1] == int.class || params[1] == bigIntegerClass)) {
                                setBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                            }
                        }

                        if ((name.equals("addCobbleDollars") || name.equals("addBalance") || name.equals("add") ||
                             name.equals("giveCobbleDollars") || name.equals("give")) && params.length == 2) {
                            if (Player.class.isAssignableFrom(params[0]) &&
                                (params[1] == long.class || params[1] == int.class || params[1] == bigIntegerClass)) {
                                addBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                            }
                        }

                        if ((name.equals("removeCobbleDollars") || name.equals("removeBalance") || name.equals("remove") ||
                                name.equals("subtractCobbleDollars") || name.equals("subtract") ||
                             name.equals("takeCobbleDollars") || name.equals("take")) && params.length == 2) {
                            if (Player.class.isAssignableFrom(params[0]) &&
                                (params[1] == long.class || params[1] == int.class || params[1] == bigIntegerClass)) {
                                removeBalanceHandle = MethodHandles.publicLookup().unreflect(m);
                            }
                        }
                    }
                }
                
                if (getBalanceHandle != null && setBalanceHandle != null) {
                    break;
                }
            } catch (ClassNotFoundException e) {
            } catch (Throwable t) {
            }
        }

        if (getBalanceHandle == null || setBalanceHandle == null) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("CobbleDollars mod is loaded but balance API could not be resolved! Villager CobbleDollars payment will be disabled.");
        }
    }

    public static long getBalance(Player player) {
        if (!isModLoaded()) {
            return -1;
        }
        if (!(player instanceof ServerPlayer)) {
            return -1;
        }

        resolveReflection();

        if (getBalanceHandle == null) {
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
                return -1;
            }
            return balance;
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Add (or remove if negative) balance from player.
     * Tries to use dedicated add/remove methods first, falls back to get+set.
     */
    public static boolean addBalance(Player player, long amount) {
        if (!isModLoaded()) {
            return false;
        }
        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        resolveReflection();

        boolean success = false;

        if (amount > 0 && addBalanceHandle != null) {
            try {
                Class<?> secondParam = addBalanceHandle.type().parameterType(1);
                if (secondParam == BigInteger.class) {
                    addBalanceHandle.invoke(player, BigInteger.valueOf(amount));
                } else if (secondParam == long.class) {
                    addBalanceHandle.invoke(player, amount);
                } else {
                    addBalanceHandle.invoke(player, (int) amount);
                }
                success = true;
            } catch (Throwable t) {
            }
        } else if (amount < 0 && removeBalanceHandle != null) {
            long absAmount = Math.abs(amount);
            try {
                Class<?> secondParam = removeBalanceHandle.type().parameterType(1);
                if (secondParam == BigInteger.class) {
                    removeBalanceHandle.invoke(player, BigInteger.valueOf(absAmount));
                } else if (secondParam == long.class) {
                    removeBalanceHandle.invoke(player, absAmount);
                } else {
                    removeBalanceHandle.invoke(player, (int) absAmount);
                }
                success = true;
            } catch (Throwable t) {
            }
        }

        if (!success) {
            if (getBalanceHandle == null || setBalanceHandle == null) {
                return false;
            }

            try {
                Object current = getBalanceHandle.invoke(player);
                BigInteger currentBi = current instanceof BigInteger ? (BigInteger) current : BigInteger.valueOf(((Number) current).longValue());
                BigInteger newBalance = currentBi.add(BigInteger.valueOf(amount));

                if (newBalance.signum() < 0) {
                    return false;
                }

                Class<?> secondParam = setBalanceHandle.type().parameterType(1);

                if (secondParam == BigInteger.class) {
                    setBalanceHandle.invoke(player, newBalance);
                } else {
                    long clamped = newBalance.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : newBalance.longValue();
                    if (secondParam == long.class) {
                        setBalanceHandle.invoke(player, clamped);
                    } else {
                        setBalanceHandle.invoke(player, (int) clamped);
                    }
                }
                success = true;
            } catch (Throwable t) {
                return false;
            }
        }

        return success;
    }

    public static boolean isAvailable() {
        if (!isModLoaded()) return false;
        resolveReflection();
        return getBalanceHandle != null && setBalanceHandle != null;
    }
}
