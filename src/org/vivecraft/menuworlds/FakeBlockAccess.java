package org.vivecraft.menuworlds;

import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.DimensionRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.CubeCoordinateIterator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.lighting.WorldLightManager;

public class FakeBlockAccess implements IWorldReader {
	private int version;
	private long seed;
	private DimensionType dimensionType;
	private boolean isFlat;
	private BlockState[] blocks;
	private byte[] skylightmap;
	private byte[] blocklightmap;
	private Biome[] biomemap;
	private int xSize;
	private int ySize;
	private int zSize;
	private int ground;

	private BiomeManager biomeManager;
	private DimensionRenderInfo dimensionInfo;
	
	public FakeBlockAccess(int version, long seed, BlockState[] blocks, byte[] skylightmap, byte[] blocklightmap, Biome[] biomemap, int xSize, int ySize, int zSize, int ground, DimensionType dimensionType, boolean isFlat) {
		this.version = version;
		this.seed = seed;
		this.blocks = blocks;
		this.skylightmap = skylightmap;
		this.blocklightmap = blocklightmap;
		this.biomemap = biomemap;
		this.xSize = xSize;
		this.ySize = ySize;
		this.zSize = zSize;
		this.ground = ground;
		this.dimensionType = dimensionType;
		this.isFlat = isFlat;

		this.biomeManager = new BiomeManager(this, BiomeManager.getHashedSeed(seed), dimensionType.getMagnifier());
		this.dimensionInfo = DimensionRenderInfo.func_243495_a(dimensionType);
	}
	
	private int encodeCoords(int x, int z) {
		return z * xSize + x;
	}
	
	private int encodeCoords(int x, int y, int z) {
		return (y * zSize + z) * xSize + x;
	}

	private int encodeCoords(BlockPos pos) {
		return encodeCoords(pos.getX(), pos.getY(), pos.getZ());
	}
	
	private boolean checkCoords(int x, int y, int z) {
		return x >= 0 && y >= 0 && z >= 0 && x < xSize && y < ySize && z < xSize;
	}

	private boolean checkCoords(BlockPos pos) {
		return checkCoords(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public int getGround() {
		return ground;
	}
	
	public int getXSize() {
		return xSize;
	}

	public int getYSize() {
		return ySize;
	}

	public int getZSize() {
		return zSize;
	}

	public long getSeed() {
		return seed;
	}

	@Override
	public DimensionType getDimensionType() {
		return dimensionType;
	}

	public DimensionRenderInfo getDimensionReaderInfo() {
		return dimensionInfo;
	}

	public double getVoidFogYFactor() {
		return isFlat ? 1.0D : 0.03125D;
	}

	public double getHorizon() {
		return isFlat ? 0.0D : 63.0D;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if (!checkCoords(pos))
			return Blocks.BEDROCK.getDefaultState();

		BlockState state = blocks[encodeCoords(pos)];
		return state != null ? state : Blocks.AIR.getDefaultState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public TileEntity getTileEntity(BlockPos pos) {
		return null; // You're a funny guy, I kill you last
	}

	@Override
	public int getBlockColor(BlockPos blockPosIn, ColorResolver colorResolverIn) {
		int i = Minecraft.getInstance().gameSettings.biomeBlendRadius;

		if (i == 0)
		{
			return colorResolverIn.getColor(this.getBiome(blockPosIn), (double)blockPosIn.getX(), (double)blockPosIn.getZ());
		}
		else
		{
			int j = (i * 2 + 1) * (i * 2 + 1);
			int k = 0;
			int l = 0;
			int i1 = 0;
			CubeCoordinateIterator cubecoordinateiterator = new CubeCoordinateIterator(blockPosIn.getX() - i, blockPosIn.getY(), blockPosIn.getZ() - i, blockPosIn.getX() + i, blockPosIn.getY(), blockPosIn.getZ() + i);
			int j1;

			for (BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(); cubecoordinateiterator.hasNext(); i1 += j1 & 255)
			{
				blockpos$mutable.setPos(cubecoordinateiterator.getX(), cubecoordinateiterator.getY(), cubecoordinateiterator.getZ());
				j1 = colorResolverIn.getColor(this.getBiome(blockpos$mutable), (double)blockpos$mutable.getX(), (double)blockpos$mutable.getZ());
				k += (j1 & 16711680) >> 16;
				l += (j1 & 65280) >> 8;
			}

			return (k / j & 255) << 16 | (l / j & 255) << 8 | i1 / j & 255;
		}
	}

	@Override
	public int getLightFor(LightType type, BlockPos pos) {
		if (!checkCoords(pos))
			return (type != LightType.SKY || !this.dimensionType.hasSkyLight()) && type != LightType.BLOCK ? 0 : type.defaultLightValue;

		if (type == LightType.SKY)
			return this.dimensionType.hasSkyLight() ? skylightmap[encodeCoords(pos)] : 0;
		else
			return type == LightType.BLOCK ? blocklightmap[encodeCoords(pos)] : type.defaultLightValue;
	}

	@Override
	public int getLightSubtracted(BlockPos pos, int amount) {
		if (!checkCoords(pos.getX(), 0, pos.getZ()))
			return 0;

		if (pos.getY() < 0) {
			return 0;
		} else if (pos.getY() >= 256) {
			int light = 15 - amount;
			if (light < 0)
				light = 0;
			return light;
		} else {
			int light = (this.dimensionType.hasSkyLight() ? skylightmap[encodeCoords(pos)] : 0) - amount;
			int blockLight = blocklightmap[encodeCoords(pos)];

			if (blockLight > light)
				light = blockLight;
			return light;
		}
	}

	@Override
	public float func_230487_a_(Direction face, boolean shade) {
		boolean flag = this.dimensionInfo.func_239217_c_(); // isNether?? yeah mate nice hard-coding

		if (!shade) {
			return flag ? 0.9F : 1.0F;
		}
		else {
			switch (face) {
				case DOWN:
					return flag ? 0.9F : 0.5F;

				case UP:
					return flag ? 0.9F : 1.0F;

				case NORTH:
				case SOUTH:
					return 0.8F;

				case WEST:
				case EAST:
					return 0.6F;

				default:
					return 1.0F;
			}
		}
	}

	@Override
	public boolean chunkExists(int x, int z) {
		return checkCoords(x * 16, 0, z * 16); // :thonk:
	}

	@Override
	public IChunk getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
		return null; // �\_(?)_/�
	}

	@Override
	public int getHeight(Heightmap.Type heightmapType, int x, int z) {
		return 0; // �\_(?)_/�
	}

	@Override
	public BlockPos getHeight(Heightmap.Type heightmapType, BlockPos pos) {
		return BlockPos.ZERO; // �\_(?)_/�
	}

	@Override
	public int getSkylightSubtracted() {
		return 0; // idk this is just what RenderChunkCache does
	}

	@Override
	public WorldBorder getWorldBorder() {
		return new WorldBorder();
	}

	@Override
	public boolean checkNoEntityCollision(Entity entityIn, VoxelShape shape) {
		return false; // ???
	}

	@Override
	public Stream<VoxelShape> func_230318_c_(@Nullable Entity p_230318_1_, AxisAlignedBB p_230318_2_, Predicate<Entity> p_230318_3_) {
		return null; // nani
	}

	@Override
	public boolean isAirBlock(BlockPos pos) {
		return this.getBlockState(pos).isAir();
	}

	@Override
	public Biome getNoiseBiome(int x, int y, int z) {
		if (!checkCoords(x * 4, y * 4, z * 4)) {
			x = MathHelper.clamp(x, 0, xSize / 4 - 1);
			y = MathHelper.clamp(y, 0, ySize / 4 - 1);
			z = MathHelper.clamp(z, 0, zSize / 4 - 1);
		}

		return biomemap[(y * (zSize / 4) + z) * (xSize / 4) + x];
	}

	@Override
	public int getStrongPower(BlockPos pos, Direction direction) {
		return 0;
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public int getSeaLevel() {
		return 63; // magic number
	}

	@Override
	public WorldLightManager getLightManager() {
		return null; // uh?
	}

	@Override
	public BiomeManager getBiomeManager() {
		return biomeManager;
	}

	@Override
	public Biome getNoiseBiomeRaw(int x, int y, int z) {
		return null; // don't need this
	}
}
