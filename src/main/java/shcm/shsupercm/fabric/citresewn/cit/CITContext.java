package shcm.shsupercm.fabric.citresewn.cit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
/*? >=1.21*/ import net.minecraft.component.ComponentType;
/*? >=1.21*/ import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Holds momentary information to be used for CITs' condition matching and type effects.
 */
public class CITContext {
    /**
     * The main item stack to check for the CIT.
     */
    public final ItemStack stack;

    /**
     * The item's containing world(defaults to {@link MinecraftClient#world} if null)
     */
    public final World world;

    /**
     * The item's associated living entity if present. (null if not relevant)
     */
    public final LivingEntity entity;

    /**
     * Cached enchantment map from {@link #stack}.
     * @see #enchantments()
     */
    private Map<Identifier, Integer> enchantments = null;

    /**
     * Lazily built cache of this context's components already encoded to NBT, keyed by component type.
     * <p>
     * A single context is tested against every CIT rule registered for the stack's item, and each rule
     * used to re-encode the very same component through its codec. On large packs that is hundreds of
     * identical encodes per stack per frame, so the result is memoized here for the context's lifetime.
     * It must never outlive the context (the stack it was built from is fixed for that context).
     */
    /*? >=1.21*/ private IdentityHashMap<ComponentType<?>, NbtElement> componentNbt = null;

    /**
     * Lazily fetched component map of {@link #stack}.
     * <p>
     * {@link ItemStack#getComponents()} is not free: other mods hook it (gugle-carpet-addition deep-copies the
     * whole {@code custom_data} NBT on every call to look for its own marker key), so a single context that is
     * tested against many CIT rules must only ask for it once.
     */
    /*? >=1.21*/ private net.minecraft.component.ComponentMap components = null;

    public CITContext(ItemStack stack, World world, LivingEntity entity) {
        this.stack = stack;
        this.world = world == null ? MinecraftClient.getInstance().world : world;
        this.entity = entity;
    }

    /**
     * @see #enchantments
     * @return a map of this context item's enchantments
     */
    public Map<Identifier, Integer> enchantments() {
        if (this.enchantments == null) {
            this.enchantments = new LinkedHashMap<>();
            /*? <1.21 {*/
            /*for (NbtElement nbtElement : stack.isOf(Items.ENCHANTED_BOOK) ? EnchantedBookItem.getEnchantmentNbt(stack) : stack.getEnchantments())
                this.enchantments.put(EnchantmentHelper.getIdFromNbt((NbtCompound) nbtElement), EnchantmentHelper.getLevelFromNbt((NbtCompound) nbtElement));
            *//*?} else {*/
            for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : EnchantmentHelper.getEnchantments(stack).getEnchantmentEntries())
                this.enchantments.put(entry.getKey().getKey().map(RegistryKey::getValue).orElseGet(() -> Identifier.of("unregistered")), entry.getValue());
            /*?}*/

        }
        return this.enchantments;
    }

    /**
     * @return {@link #stack}'s component map, fetched at most once per context
     * @see #components
     */
    /*? >=1.21 {*/
    public net.minecraft.component.ComponentMap components() {
        if (this.components == null)
            this.components = this.stack.getComponents();
        return this.components;
    }
    /*?}*/

    /**
     * Returns the given component of {@link #stack} encoded to NBT, encoding it at most once per context.
     *
     * @param type    component type to look up
     * @param encoder builds the NBT representation, only called on a cache miss
     * @return the encoded component, or {@code null} if the stack has no such component
     * @see #componentNbt
     */
    /*? >=1.21 {*/
    public NbtElement getComponentNbt(ComponentType<?> type, Supplier<NbtElement> encoder) {
        if (this.componentNbt == null)
            this.componentNbt = new IdentityHashMap<>(4);

        NbtElement cached = this.componentNbt.get(type);
        if (cached == null && !this.componentNbt.containsKey(type)) {
            cached = encoder.get();
            if (cached != null)
                this.componentNbt.put(type, cached);
        }
        return cached;
    }
    /*?}*/

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CITContext that &&
                Objects.equals(this.stack, that.stack) &&
                Objects.equals(this.world, that.world) &&
                Objects.equals(this.entity, that.entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, world, entity);
    }
}
