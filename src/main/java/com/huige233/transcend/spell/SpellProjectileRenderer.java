package com.huige233.transcend.spell;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile> {

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpellProjectile entity) {
        return new ResourceLocation("transcend", "textures/item/spell_scroll.png");
    }

    // render() inherited from EntityRenderer does nothing visible.
    // The projectile's visual is handled entirely by DustParticleOptions
    // spawned in SpellProjectile.tick() on the client side.
}
