package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.enchantment;

import net.minecraft.client.render.RenderLayer;
/*? >=1.21.11*/
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.class)
/*? >=1.21.11 {*/
public interface RenderLayerInvoker {
    @Invoker("of")
    static RenderLayer citresewn$of(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
/*?} else {*/
/*public interface RenderLayerInvoker {
}
*//*?}*/
