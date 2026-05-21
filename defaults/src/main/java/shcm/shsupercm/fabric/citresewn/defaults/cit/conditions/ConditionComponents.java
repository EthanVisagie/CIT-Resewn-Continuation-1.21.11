package shcm.shsupercm.fabric.citresewn.defaults.cit.conditions;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import shcm.shsupercm.fabric.citresewn.CITResewn;
import shcm.shsupercm.fabric.citresewn.api.CITConditionContainer;
import shcm.shsupercm.fabric.citresewn.cit.CITCondition;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.cit.CITParsingException;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyKey;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConditionComponents extends CITCondition {
    /*? >=1.21*/ @Entrypoint(CITConditionContainer.ENTRYPOINT)
    public static final CITConditionContainer<ConditionComponents> CONTAINER = new CITConditionContainer<>(ConditionComponents.class, ConditionComponents::new,
            "components", "component", "nbt");
    private static final int LEGACY_NBT_WARNING_LIMIT_PER_PACK = 20;
    private static final Map<String, Integer> LEGACY_NBT_WARNING_COUNTS = new ConcurrentHashMap<>();

    /*? >=1.21*/ private DataComponentType<?> componentType;
    private String componentMetadata;
    private String matchValue;

    private ConditionNBT fallbackNBTCheck;

    @Override
    public void load(PropertyKey key, PropertyValue value, PropertyGroup properties) throws CITParsingException {
        String metadata = value.keyMetadata();
        if (key.path().equals("nbt")) {
            if (metadata.startsWith("display.Name")) {
                metadata = "minecraft:custom_name" + value.keyMetadata().substring("display.Name".length());
                warnLegacyNbt(properties, value, "display.Name");
            } else if (metadata.startsWith("display.Lore")) {
                metadata = "minecraft:lore" + value.keyMetadata().substring("display.Lore".length());
                warnLegacyNbt(properties, value, "display.Lore");
            } else
                throw new CITParsingException("NBT condition is not supported since 1.21", properties, value.position());
        }

        metadata = metadata.replace("~", "minecraft:");

        String componentId = metadata.split("\\.")[0];

        /*? >=1.21 {*/
        if ((this.componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.tryParse(componentId))) == null)
            throw new CITParsingException("Unknown component type \"" + componentId + "\"", properties, value.position());
        /*?}*/

        metadata = metadata.substring(componentId.length());
        if (metadata.startsWith("."))
            metadata = metadata.substring(1);
        this.componentMetadata = metadata;

        this.matchValue = value.value();

        this.fallbackNBTCheck = new ConditionNBT();
        String[] metadataNbtPath = metadata.split("\\.");
        if (metadataNbtPath.length == 1 && metadataNbtPath[0].isEmpty())
            metadataNbtPath = new String[0];
        this.fallbackNBTCheck.loadNbtCondition(value, properties, metadataNbtPath, this.matchValue);
    }

    private static void warnLegacyNbt(PropertyGroup properties, PropertyValue value, String legacyPath) {
        String warningKey = properties.packName + "|" + legacyPath;
        int count = LEGACY_NBT_WARNING_COUNTS.merge(warningKey, 1, Integer::sum);
        if (count <= LEGACY_NBT_WARNING_LIMIT_PER_PACK) {
            CITResewn.logWarnLoading(properties.messageWithDescriptorOf("Using legacy nbt." + legacyPath, value.position()));
        } else if (count == LEGACY_NBT_WARNING_LIMIT_PER_PACK + 1) {
            CITResewn.logWarnLoading("Suppressing further legacy nbt." + legacyPath + " warnings for " + properties.packName + " during this session");
        }
    }

    @Override
    public boolean test(CITContext context) {
        /*? >=1.21 {*/
        Object stackComponent = context.stack.getComponents().get(this.componentType);
        if (stackComponent != null) {
            if (stackComponent instanceof Component text) {
                if (this.fallbackNBTCheck.testString(null, text, context))
                    return true;
            } /*else if (stackComponent instanceof LoreComponent lore) {
                //todo avoid nbt based check if possible
            }*/

            Tag fallbackComponentNBT = ((DataComponentType<Object>) this.componentType).codec().encodeStart(context.world.registryAccess().createSerializationContext(NbtOps.INSTANCE), stackComponent).getOrThrow();
            return this.fallbackNBTCheck.testPath(fallbackComponentNBT, 0, context);
        }
        /*?}*/
        return false;
    }
}
