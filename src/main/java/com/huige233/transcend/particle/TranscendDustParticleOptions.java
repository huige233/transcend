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

public class TranscendDustParticleOptions implements ParticleOptions {

    public static final Codec<TranscendDustParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(TranscendDustParticleOptions::getColor),
                    Codec.FLOAT.fieldOf("scale").forGetter(TranscendDustParticleOptions::getScale),
                    Codec.INT.fieldOf("lifetime").forGetter(TranscendDustParticleOptions::getLifetime),
                    Codec.BOOL.fieldOf("glow").forGetter(TranscendDustParticleOptions::isGlow)
            ).apply(instance, TranscendDustParticleOptions::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<TranscendDustParticleOptions> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public TranscendDustParticleOptions fromCommand(ParticleType<TranscendDustParticleOptions> type, StringReader reader) throws CommandSyntaxException {
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
                    return new TranscendDustParticleOptions(new Vector3f(r, g, b), scale, lifetime, glow);
                }

                @Override
                public TranscendDustParticleOptions fromNetwork(ParticleType<TranscendDustParticleOptions> type, FriendlyByteBuf buf) {
                    return new TranscendDustParticleOptions(
                            new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                            buf.readFloat(),
                            buf.readInt(),
                            buf.readBoolean()
                    );
                }
            };

    public static final ParticleType<TranscendDustParticleOptions> TYPE = new ParticleType<>(false, DESERIALIZER) {
        @Override
        public Codec<TranscendDustParticleOptions> codec() {
            return CODEC;
        }
    };

    private final Vector3f color;
    private final float scale;
    private final int lifetime;
    private final boolean glow;

    public TranscendDustParticleOptions(Vector3f color, float scale, int lifetime, boolean glow) {
        this.color = color;
        this.scale = scale;
        this.lifetime = lifetime;
        this.glow = glow;
    }

    public TranscendDustParticleOptions(float r, float g, float b, float scale) {
        this(new Vector3f(r, g, b), scale, 20, true);
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
        return String.format("transcend:transcend_dust %.2f %.2f %.2f %.2f %d %b",
                color.x(), color.y(), color.z(), scale, lifetime, glow);
    }
}
