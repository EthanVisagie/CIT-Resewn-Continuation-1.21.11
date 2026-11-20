package shcm.shsupercm.fabric.citresewn.defaults.cit.conditions;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import shcm.shsupercm.fabric.citresewn.api.CITConditionContainer;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.cit.builtin.conditions.IdentifierCondition;
import shcm.shsupercm.fabric.citresewn.cit.builtin.conditions.ListCondition;
import shcm.shsupercm.fabric.citresewn.cit.CITParsingException;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyKey;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ConditionItems extends ListCondition<ConditionItems.ItemCondition> {
    @Entrypoint(CITConditionContainer.ENTRYPOINT)
    public static final CITConditionContainer<ConditionItems> CONTAINER = new CITConditionContainer<>(ConditionItems.class, ConditionItems::new,
            "items", "matchItems");

    private static final Map<Identifier, Identifier> LEGACY_ITEM_RENAMES = Map.of(
            Identifier.withDefaultNamespace("grass"), Identifier.withDefaultNamespace("short_grass"),
            Identifier.withDefaultNamespace("scute"), Identifier.withDefaultNamespace("turtle_scute"),
            Identifier.withDefaultNamespace("chain"), Identifier.withDefaultNamespace("iron_chain"),
            Identifier.withDefaultNamespace("sign"), Identifier.withDefaultNamespace("oak_sign"),
            Identifier.withDefaultNamespace("cactus_green"), Identifier.withDefaultNamespace("green_dye"),
            Identifier.withDefaultNamespace("rose_red"), Identifier.withDefaultNamespace("red_dye"),
            Identifier.withDefaultNamespace("dandelion_yellow"), Identifier.withDefaultNamespace("yellow_dye"),
            Identifier.withDefaultNamespace("zombie_pigman_spawn_egg"), Identifier.withDefaultNamespace("zombified_piglin_spawn_egg")
    );

    public static Identifier resolveLegacyItemId(Identifier id) {
        if (id == null || BuiltInRegistries.ITEM.containsKey(id))
            return id;
        Identifier renamed = LEGACY_ITEM_RENAMES.get(id);
        return renamed != null && BuiltInRegistries.ITEM.containsKey(renamed) ? renamed : id;
    }

    public Item[] items = new Item[0];

    public ConditionItems() {
        super(ItemCondition.class, ItemCondition::new);
    }

    public ConditionItems(Item... items) {
        this();
        this.items = items;
    }

    @Override
    public void load(PropertyKey key, PropertyValue value, PropertyGroup properties) throws CITParsingException {
        super.load(key, value, properties);

        Set<Item> items = new LinkedHashSet<>();

        for (ItemCondition itemCondition : this.conditions)
            items.add(itemCondition.item);

        this.items = items.toArray(new Item[0]);
    }

    @Override
    public boolean test(CITContext context) {
        for (Item item : this.items)
            if (context.stack.getItem() == item)
                return true;

        return false;
    }

    protected static class ItemCondition extends IdentifierCondition {
        public Item item = null;

        @Override
        public void load(PropertyKey key, PropertyValue value, PropertyGroup properties) throws CITParsingException {
            super.load(key, value, properties);

            Identifier resolved = resolveLegacyItemId(this.value);
            if (BuiltInRegistries.ITEM.containsKey(resolved)) {
                this.value = resolved;
                this.item = BuiltInRegistries.ITEM.getValue(resolved);
            } else {
                this.item = null;
                warn(this.value + " is not in the item registry", value, properties);
            }
        }

        @Override
        protected Identifier getValue(CITContext context) {
            return this.value;
        }
    }
}
