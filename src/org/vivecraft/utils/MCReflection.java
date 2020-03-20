package org.vivecraft.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.vivecraft.asm.ObfNames;

import net.minecraft.client.audio.SoundEngine;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.dimension.Dimension;

public class MCReflection {
	//public static final ReflectionField SoundManager_sndSystem = new ReflectionField(SoundEngine.class, "field_148620_e");
	//public static final ReflectionField SoundManager_loaded = new ReflectionField(SoundEngine.class, "field_148617_f");
	public static final String BlockState_OnBlockActivated = "func_220051_a";
	public static final ReflectionMethod Dimension_generateLightBrightnessTable = new ReflectionMethod(Dimension.class, "func_76556_a");
	public static final ReflectionMethod Dimension_hasSkyLight = new ReflectionMethod(Dimension.class, "func_191066_m");
	public static final ReflectionField PlayerController_blockHitDelay = new ReflectionField(PlayerController.class, "field_78781_i");
	public static final ReflectionField PlayerController_isHittingBlock = new ReflectionField(PlayerController.class, "field_78778_j");
//	public static final ReflectionField GuiChat_inputField = new ReflectionField(GuiChat.class, "field_146415_a");
	public static final ReflectionField KeyBinding_pressed = new ReflectionField(KeyBinding.class, "field_74513_e");
	public static final ReflectionField KeyBinding_pressTime = new ReflectionField(KeyBinding.class, "field_151474_i");
	public static final ReflectionMethod KeyBinding_unpressKey = new ReflectionMethod(KeyBinding.class, "func_74505_d");
	public static final ReflectionField KeyBinding_keyCode = new ReflectionField(KeyBinding.class, "field_74512_d");
//	public static final ReflectionField TileEntityRendererDispatcher_fontRenderer = new ReflectionField(TileEntityRendererDispatcher.class, "field_147557_n");
	public static final ReflectionField KeyBinding_CATEGORY_ORDER = new ReflectionField(KeyBinding.class, "field_193627_d");
	public static final ReflectionMethod RenderPlayer_setModelVisibilities = new ReflectionMethod(PlayerRenderer.class, "func_177137_d", AbstractClientPlayerEntity.class);
//	public static final ReflectionField TextureMap_listAnimatedSprites = new ReflectionField(TextureMap.class, "field_94258_i");
//	public static final ReflectionField PlayerEntity_spawnChunk = new ReflectionField(PlayerEntity.class, "field_71077_c");
//	public static final ReflectionField PlayerEntity_spawnForced = new ReflectionField(PlayerEntity.class, "field_82248_d");
//	public static final ReflectionMethod RenderGlobal_renderSky = new ReflectionMethod(RenderGlobal.class, "func_174968_a", BufferBuilder.class, float.class, boolean.class);
//	public static final ReflectionMethod RenderGlobal_renderStars = new ReflectionMethod(RenderGlobal.class, "func_180444_a", BufferBuilder.class);
	public static final ReflectionField ModelManager_texmap = new ReflectionField(ModelManager.class, "field_174956_b");
	public static final ReflectionField ModelManager_modelRegistry = new ReflectionField(ModelManager.class, "field_174958_a");
	public static final ReflectionField ModelManager_defaultModel = new ReflectionField(ModelManager.class, "field_174955_d");

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
