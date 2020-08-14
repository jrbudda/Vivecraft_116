package org.vivecraft.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.vivecraft.asm.ObfNames;

import net.minecraft.client.audio.SoundEngine;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.state.StateHolder;
import net.minecraft.world.biome.BiomeManager;

public class MCReflection {

	public static final ReflectionField SoundHandler_sndManager = new ReflectionField(SoundHandler.class, "sndManager");
	public static final ReflectionMethod SoundEngine_reload = new ReflectionMethod(SoundEngine.class, "reload");

	//TODO: Verify srg of commented fields
	public static final String BlockState_OnBlockActivated = "onBlockActivated";
//	public static final ReflectionMethod Dimension_generateLightBrightnessTable = new ReflectionMethod(Dimension.class, "func_76556_a");
//	public static final ReflectionMethod Dimension_hasSkyLight = new ReflectionMethod(Dimension.class, "hasSkyLight");

	public static final ReflectionField PlayerController_blockHitDelay = new ReflectionField(PlayerController.class, "blockHitDelay");
	public static final ReflectionField PlayerController_isHittingBlock = new ReflectionField(PlayerController.class, "isHittingBlock");

	//	public static final ReflectionField GuiChat_inputField = new ReflectionField(GuiChat.class, "inputField");

	public static final ReflectionField KeyBinding_pressed = new ReflectionField(KeyBinding.class, "pressed");
	public static final ReflectionField KeyBinding_pressTime = new ReflectionField(KeyBinding.class, "pressTime");
	public static final ReflectionMethod KeyBinding_unpressKey = new ReflectionMethod(KeyBinding.class, "unpressKey");
	public static final ReflectionField KeyBinding_keyCode = new ReflectionField(KeyBinding.class, "keyCode");
	public static final ReflectionField KeyBinding_CATEGORY_ORDER = new ReflectionField(KeyBinding.class, "CATEGORY_ORDER");

	public static final ReflectionField Entity_eyeHeight = new ReflectionField(Entity.class, "eyeHeight");

	
	public static final ReflectionMethod RenderPlayer_setModelVisibilities = new ReflectionMethod(PlayerRenderer.class, "setModelVisibilities", AbstractClientPlayerEntity.class);
//	public static final ReflectionField TileEntityRendererDispatcher_fontRenderer = new ReflectionField(TileEntityRendererDispatcher.class, "fontRenderer");
//	public static final ReflectionField TextureMap_listAnimatedSprites = new ReflectionField(TextureMap.class, "listAnimatedSprites");
//	public static final ReflectionField PlayerEntity_spawnChunk = new ReflectionField(PlayerEntity.class, "spawnPos");
//	public static final ReflectionField PlayerEntity_spawnForced = new ReflectionField(PlayerEntity.class, "spawnForced");
//	public static final ReflectionMethod RenderGlobal_renderSky = new ReflectionMethod(RenderGlobal.class, "renderSky", BufferBuilder.class, float.class, boolean.class);
//	public static final ReflectionMethod RenderGlobal_renderStars = new ReflectionMethod(RenderGlobal.class, "renderStars", BufferBuilder.class);
//	public static final ReflectionField ModelManager_texmap = new ReflectionField(ModelManager.class, "field_174956_b");
//	public static final ReflectionField ModelManager_modelRegistry = new ReflectionField(ModelManager.class, "modelRegistry");
//	public static final ReflectionField ModelManager_defaultModel = new ReflectionField(ModelManager.class, "defaultModel");
	public static final ReflectionField CCustomPayloadPacket_channel = new ReflectionField(CCustomPayloadPacket.class, "channel");
	public static final ReflectionField CCustomPayloadPacket_data = new ReflectionField(CCustomPayloadPacket.class, "data");
	//public static final ReflectionField PlayerEntity_spawnPos = new ReflectionField(PlayerEntity.class, "spawnPos");
	//public static final ReflectionField PlayerEntity_spawnForced = new ReflectionField(PlayerEntity.class, "spawnForced");

	public static final ReflectionField StateHolder_mapCodec = new ReflectionField(StateHolder.class, "field_235893_d_");

	public static final ReflectionField ClientWorldInfo_isFlat = new ReflectionField(ClientWorld.ClientWorldInfo.class, "flatWorld");

	public static final ReflectionField BiomeManager_seed = new ReflectionField(BiomeManager.class, "seed");

	public static final ReflectionField NetworkManager_channel = new ReflectionField(NetworkManager.class, "channel");

	
	public static class ReflectionField {
		private final Class<?> clazz;
		private final String srgName;
		private Field field;

		public ReflectionField(Class<?> clazz, String srgName) {
			this.clazz = clazz;
			this.srgName = srgName;
			reflect();
		}
		
		public Object get(Object obj) {
			try {
				return field.get(obj);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		public void set(Object obj, Object value) {
			try {
				field.set(obj, value);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		private void reflect() {
			try
			{
				field = clazz.getDeclaredField(srgName);
			}
			catch (NoSuchFieldException e)
			{
				try
				{
					field = clazz.getDeclaredField(ObfNames.resolveField(srgName, true));
				}
				catch (NoSuchFieldException e1)
				{
					try
					{
						field = clazz.getDeclaredField(ObfNames.getDevMapping(srgName));
					}
					catch (NoSuchFieldException e2)
					{
						StringBuilder sb = new StringBuilder(srgName);
						if (!srgName.equals(ObfNames.resolveField(srgName, true)))
							sb.append(',').append(ObfNames.resolveField(srgName, true));
						if (!srgName.equals(ObfNames.getDevMapping(srgName)))
							sb.append(',').append(ObfNames.getDevMapping(srgName));
						throw new RuntimeException("reflecting field " + sb.toString() + " in " + clazz.toString(), e);
					}
				}
			}

			field.setAccessible(true); //lets be honest this is why we have this method.
		}
	}

	public static class ReflectionMethod {
		private final Class<?> clazz;
		private final String srgName;
		private final Class<?>[] params;
		private Method method;

		public ReflectionMethod(Class<?> clazz, String srgName, Class<?>... params) {
			this.clazz = clazz;
			this.srgName = srgName;
			this.params = params;
			reflect();
		}

		public Method getMethod() {
			return method;
		}
		
		public Object invoke(Object obj, Object... args) {
			try {
				return method.invoke(obj, args);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		private void reflect() {
			try
			{
				method = clazz.getDeclaredMethod(srgName, params);
			}
			catch (NoSuchMethodException e)
			{
				try
				{
					method = clazz.getDeclaredMethod(ObfNames.resolveMethod(srgName, true), params);
				}
				catch (NoSuchMethodException e1)
				{
					try
					{
						method = clazz.getDeclaredMethod(ObfNames.getDevMapping(srgName), params);
					}
					catch (NoSuchMethodException e2)
					{
						StringBuilder sb = new StringBuilder(srgName);
						if (!srgName.equals(ObfNames.resolveMethod(srgName, true)))
							sb.append(',').append(ObfNames.resolveMethod(srgName, true));
						if (!srgName.equals(ObfNames.getDevMapping(srgName)))
							sb.append(',').append(ObfNames.getDevMapping(srgName));
						if (params.length > 0) {
							sb.append(" with params ");
							sb.append(Arrays.stream(params).map(Class::getName).collect(Collectors.joining(",")));
						}
						throw new RuntimeException("reflecting method " + sb.toString() + " in " + clazz.toString(), e);
					}
				}
			}

			method.setAccessible(true);
		}
	}

	public static class ReflectionConstructor {
		private final Class<?> clazz;
		private final Class<?>[] params;
		private Constructor constructor;

		public ReflectionConstructor(Class<?> clazz, Class<?>... params) {
			this.clazz = clazz;
			this.params = params;
			reflect();
		}

		public Object newInstance(Object... args) {
			try {
				return constructor.newInstance(args);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		private void reflect() {
			try
			{
				constructor = clazz.getDeclaredConstructor(params);
			}
			catch (NoSuchMethodException e)
			{
				StringBuilder sb = new StringBuilder();
				if (params.length > 0) {
					sb.append(" with params ");
					sb.append(Arrays.stream(params).map(Class::getName).collect(Collectors.joining(",")));
				}
				throw new RuntimeException("reflecting constructor " + sb.toString() + " in " + clazz.toString(), e);
			}

			constructor.setAccessible(true);
		}
	}
}
