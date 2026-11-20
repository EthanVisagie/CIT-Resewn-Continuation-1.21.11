package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.MainHand;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import shcm.shsupercm.fabric.citresewn.CITResewn;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

final class HeldModelOverrides {
    private HeldModelOverrides() {}

    static ItemModel.Unbaked load(Identifier baseId, Identifier file, ResourceManager resources) {
        ItemModel.Unbaked base = model(baseId);
        try (var reader = new JsonReader(resources.openAsReader(file))) {
            reader.setStrictness(Strictness.LENIENT);
            // Geometry can dominate these files. Only materialize the legacy override list.
            if (reader.peek() != JsonToken.BEGIN_OBJECT)
                return base;
            reader.beginObject();
            while (reader.hasNext() && !reader.nextName().equals("overrides"))
                reader.skipValue();
            if (reader.peek() != JsonToken.BEGIN_ARRAY || !(JsonParser.parseReader(reader) instanceof JsonArray overrides))
                return base;

            ItemModel.Unbaked main = base;
            ItemModel.Unbaked off = base;
            for (var entry : overrides) {
                if (!(entry instanceof JsonObject override) || !(override.get("predicate") instanceof JsonObject predicate)
                        || predicate.size() != 1 || !(override.get("model") instanceof JsonPrimitive target) || !target.isString())
                    return base;
                var condition = predicate.entrySet().iterator().next();
                if (!(condition.getValue() instanceof JsonPrimitive threshold) || !threshold.isNumber()
                        || !(threshold.getAsDouble() > 0 && threshold.getAsDouble() <= 1))
                    return base;
                Identifier id = Identifier.tryParse(target.getAsString());
                if (id == null || resources.getResource(id.withPath(path -> "models/" + path + ".json")).isEmpty())
                    return base;
                switch (condition.getKey()) {
                    case "pommel:is_held" -> {
                        main = model(id);
                        off = main;
                    }
                    case "pommel:is_offhand" -> off = model(id);
                    default -> { return base; }
                }
            }
            if (main == base && off == base)
                return base;

            var hands = new SelectItemModel.UnbakedSwitch<>(new DisplayContext(), List.of(
                    new SelectItemModel.SwitchCase<>(List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
                            forArm(HumanoidArm.RIGHT, main, off)),
                    new SelectItemModel.SwitchCase<>(List.of(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
                            forArm(HumanoidArm.LEFT, main, off))));
            return new SelectItemModel.Unbaked(Optional.empty(), hands, Optional.of(base));
        } catch (IOException | JsonParseException e) {
            CITResewn.logWarnLoading("Could not read held model overrides from " + file + ": " + e.getMessage());
            return base;
        }
    }

    private static ItemModel.Unbaked forArm(HumanoidArm arm, ItemModel.Unbaked main, ItemModel.Unbaked off) {
        if (main == off)
            return main;
        var handedness = new SelectItemModel.UnbakedSwitch<>(new MainHand(), List.of(new SelectItemModel.SwitchCase<>(List.of(arm), main)));
        return new SelectItemModel.Unbaked(Optional.empty(), handedness, Optional.of(off));
    }

    private static ItemModel.Unbaked model(Identifier id) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of());
    }
}
