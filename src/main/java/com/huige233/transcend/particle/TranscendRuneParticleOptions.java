package com.huige233.transcend.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

public class TranscendRuneParticleOptions implements ParticleOptions {

    public static final Codec<TranscendRuneParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(TranscendRuneParticleOptions::getColor),
                    Codec.FLOAT.fieldOf("scale").forGetter(TranscendRuneParticleOptions::getScale),
                    Codec.INT.fieldOf("lifetime").forGetter(TranscendRuneParticleOptions::getLifetime),
                    Codec.BOOL.fieldOf("glow").forGetter(TranscendRuneParticleOptions::isGlow)
            ).apply(instance, TranscendRuneParticleOptions::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<TranscendRuneParticleOptions> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public TranscendRuneParticleOptions fromCommand(ParticleType<TranscendRuneParticleOptions> type, StringReader reader) throws CommandSyntaxException {
                    reader.expect(' ');
                    float r = reader.readFloat();
                    reader.expect(' ');
                    float g = reader.readFloat();
                    reader.expect(' ');
                    float b = reader.readFloat();
                    reader.expect(' ');
                    float scale = reader.readFloat();
                    reader.expect(' ');
                    int lifetime = reader.readInt();
                    reader.expect(' ');
                    boolean glow = reader.readBoolean();
                    return new TranscendRuneParticleOptions(new Vector3f(r, g, b), scale, lifetime, glow);
                }

                @Override
                public TranscendRuneParticleOptions fromNetwork(ParticleType<TranscendRuneParticleOptions> type, FriendlyByteBuf buf) {
                    return new TranscendRuneParticleOptions(
                            new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                            buf.readFloat(),
                            buf.readInt(),
                            buf.readBoolean()
                    );
                }
            };

    public static final ParticleType<TranscendRuneParticleOptions> TYPE = new ParticleType<>(false, DESERIALIZER) {
        @Override
        public Codec<TranscendRuneParticleOptions> codec() {
            return CODEC;
        }
    };

    private final Vector3f color;
    private final float scale;
    private final int lifetime;
    private final boolean glow;

    public TranscendRuneParticleOptions(Vector3f color, float scale, int lifetime, boolean glow) {
        this.color = color;
        this.scale = scale;
        this.lifetime = lifetime;
        this.glow = glow;
    }

    public Vector3f getColor() {
        return color;
    }

    public float getScale() {
        return scale;
    }

    public int getLifetime() {
        return lifetime;
    }

    public boolean isGlow() {
        return glow;
    }

    @Override
    public ParticleType<?> getType() {
        return TYPE;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(color.x());
        buf.writeFloat(color.y());
        buf.writeFloat(color.z());
        buf.writeFloat(scale);
        buf.writeInt(lifetime);
        buf.writeBoolean(glow);
    }

    @Override
    public String writeToString() {
        return String.format("transcend:transcend_rune %.2f %.2f %.2f %.2f %d %b",
                color.x(), color.y(), color.z(), scale, lifetime, glow);
    }
}
