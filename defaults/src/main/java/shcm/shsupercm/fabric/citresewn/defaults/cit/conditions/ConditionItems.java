package shcm.shsupercm.fabric.citresewn.defaults.cit.conditions;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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

public class ConditionItems extends ListCondition<ConditionItems.ItemCondition> {
    @Entrypoint(CITConditionContainer.ENTRYPOINT)
    public static final CITConditionContainer<ConditionItems> CONTAINER = new CITConditionContainer<>(ConditionItems.class, ConditionItems::new,
            "items", "matchItems");

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
        private static final Map<Identifier, Identifier> LEGACY_ITEM_ALIASES = Map.ofEntries(
                Map.entry(Identifier.of("minecraft", "gold_helmet"), Identifier.of("minecraft", "golden_helmet")),
                Map.entry(Identifier.of("minecraft", "gold_chestplate"), Identifier.of("minecraft", "golden_chestplate")),
                Map.entry(Identifier.of("minecraft", "gold_leggings"), Identifier.of("minecraft", "golden_leggings")),
                Map.entry(Identifier.of("minecraft", "gold_boots"), Identifier.of("minecraft", "golden_boots")),
                Map.entry(Identifier.of("minecraft", "gold_sword"), Identifier.of("minecraft", "golden_sword")),
                Map.entry(Identifier.of("minecraft", "gold_shovel"), Identifier.of("minecraft", "golden_shovel")),
                Map.entry(Identifier.of("minecraft", "gold_pickaxe"), Identifier.of("minecraft", "golden_pickaxe")),
                Map.entry(Identifier.of("minecraft", "gold_axe"), Identifier.of("minecraft", "golden_axe")),
                Map.entry(Identifier.of("minecraft", "gold_hoe"), Identifier.of("minecraft", "golden_hoe")),
                Map.entry(Identifier.of("minecraft", "gold_horse_armor"), Identifier.of("minecraft", "golden_horse_armor")),
                Map.entry(Identifier.of("minecraft", "wood_sword"), Identifier.of("minecraft", "wooden_sword")),
                Map.entry(Identifier.of("minecraft", "wood_shovel"), Identifier.of("minecraft", "wooden_shovel")),
                Map.entry(Identifier.of("minecraft", "wood_pickaxe"), Identifier.of("minecraft", "wooden_pickaxe")),
                Map.entry(Identifier.of("minecraft", "wood_axe"), Identifier.of("minecraft", "wooden_axe")),
                Map.entry(Identifier.of("minecraft", "wood_hoe"), Identifier.of("minecraft", "wooden_hoe"))
        );

        public Item item = null;

        @Override
        public void load(PropertyKey key, PropertyValue value, PropertyGroup properties) throws CITParsingException {
            super.load(key, value, properties);

            Identifier itemId = resolveItemAlias(this.value);
            if (Registries.ITEM.containsId(itemId)) {
                this.item = Registries.ITEM.get(itemId);
                return;
            }

            this.item = null;
            warn(this.value + " is not in the item registry", value, properties);
        }

        @Override
        protected Identifier getValue(CITContext context) {
            return this.value;
        }

        private static Identifier resolveItemAlias(Identifier itemId) {
            Identifier alias = LEGACY_ITEM_ALIASES.get(itemId);
            return alias != null ? alias : itemId;
        }
    }
}
