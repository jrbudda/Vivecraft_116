package org.vivecraft.render;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderState.TextureState;
import net.minecraft.client.renderer.RenderType.State;
import net.minecraft.client.renderer.tileentity.EndPortalTileEntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.optifine.Config;
import net.optifine.RandomEntities;
import net.optifine.SmartAnimations;
import net.optifine.util.CompareUtils;
import net.optifine.util.CompoundKey;

public abstract class VRRenderType extends RenderState{
	
	public VRRenderType(String p_i107_1_, Runnable p_i107_2_, Runnable p_i107_3_) {
		super(p_i107_1_, p_i107_2_, p_i107_3_);
	}

	private static Map<CompoundKey, RenderType> RENDER_TYPES;
	
    private static RenderType getRenderType(String p_getRenderType_0_, ResourceLocation p_getRenderType_1_, Supplier<RenderType> p_getRenderType_2_)
    {
        CompoundKey compoundkey = new CompoundKey(p_getRenderType_0_, p_getRenderType_1_);
        return getRenderType(compoundkey, p_getRenderType_2_);
    }

  	private static RenderType getRenderType(CompoundKey p_getRenderType_0_, Supplier<RenderType> p_getRenderType_1_)
	{
		if (RENDER_TYPES == null)
		{
			RENDER_TYPES = new HashMap<>();
		}

		RenderType rendertype = RENDER_TYPES.get(p_getRenderType_0_);

		if (rendertype != null)
		{
			return rendertype;
		}
		else
		{
			rendertype = p_getRenderType_1_.get();
			RENDER_TYPES.put(p_getRenderType_0_, rendertype);
			return rendertype;
		}
	}

    public static RenderType getTextNoCull(ResourceLocation locationIn)
    {
        return getRenderType("text_nocull", locationIn, (Supplier<RenderType>)() ->
        {
            return RenderType.makeType("text_nocull", DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP, 7, 256, false, true, RenderType.State.getBuilder().texture(new RenderState.TextureState(locationIn, false, false)).alpha(DEFAULT_ALPHA).transparency(NO_TRANSPARENCY).lightmap(LIGHTMAP_ENABLED).cull(CULL_DISABLED).build(false));
        });
    }
    


}

