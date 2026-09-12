package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies only the CobbleDollars Bank mixin that matches the installed API layout:
 * Beta-5.x {@code world.item.trading.shop.Bank} vs Beta-6.x {@code api.bank.Bank}.
 */
public class CobbleDollarsBankMixinPlugin implements IMixinConfigPlugin {

    private static final String LEGACY_BANK = "fr.harmex.cobbledollars.common.world.item.trading.shop.Bank";
    private static final String API_BANK = "fr.harmex.cobbledollars.common.api.bank.Bank";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".BankMixin")) {
            return classPresent(LEGACY_BANK);
        }
        if (mixinClassName.endsWith(".BankMixinApi")) {
            return classPresent(API_BANK);
        }
        return true;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, CobbleDollarsBankMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
