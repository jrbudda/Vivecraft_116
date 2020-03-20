package org.vivecraft.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.google.common.io.Files;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.IntIdentityHashBiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.dimension.DimensionType;

public class MenuWorldExporter {
	public static final int VERSION = 2;
	public static final int MIN_VERSION = 2;

	public static byte[] saveArea(World world, int xMin, int zMin, int xSize, int zSize, int ground) throws IOException {
		BlockStateMapper mapper = new BlockStateMapper();

		int ySize = world.getHeight();
		int[] blocks = new int[xSize * ySize * zSize];
		byte[] skylightmap = new byte[xSize * ySize * zSize];
		byte[] blocklightmap = new byte[xSize * ySize * zSize];
		int[] biomemap = new int[xSize * zSize];
		for (int x = xMin; x < xMin + xSize; x++) {
			int xl = x - xMin;
			for (int z = zMin; z < zMin + zSize; z++) {
				int zl = z - zMin;
				int index2 = zl * xSize + xl;
				BlockPos pos2 = new BlockPos(x, 0, z);
				biomemap[index2] = (byte)Registry.BIOME.getId(world.getBiome(pos2));
				for (int y = 0; y < ySize; y++) {
					int index3 = (y * zSize + zl) * xSize + xl;
					BlockPos pos3 = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos3);
					blocks[index3] = mapper.getId(state);
					skylightmap[index3] = (byte)world.getLightFor(LightType.SKY, pos3);
					blocklightmap[index3] = (byte)world.getLightFor(LightType.BLOCK, pos3);
				}
			}
		}
		
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(data);
		dos.writeInt(xSize);
		dos.writeInt(ySize);
		dos.writeInt(zSize);
		dos.writeInt(ground);
		dos.writeInt(world.dimension.getType().getId());
		dos.writeUTF(world.getWorldInfo().getGenerator().getName());
		dos.writeBoolean(world.dimension.hasSkyLight()); // because we can't init it later
		mapper.writePalette(dos);
		for (int i = 0; i < blocks.length; i++) {
			dos.writeInt(blocks[i]);
		}
		for (int i = 0; i < skylightmap.length; i++) {
			dos.writeByte(skylightmap[i] | (blocklightmap[i] << 4));
		}
		for (int i = 0; i < biomemap.length; i++) {
			dos.writeInt(biomemap[i]);
		}

		Header header = new Header();
		header.version = VERSION;
		header.uncompressedSize = data.size();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DataOutputStream headerStream = new DataOutputStream(output);
		header.write(headerStream);

		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		deflater.setInput(data.toByteArray());
		deflater.finish();
		byte[] buffer = new byte[1048576];
		while (!deflater.finished()) {
			int len = deflater.deflate(buffer);
			output.write(buffer, 0, len);
		}
		
		return output.toByteArray();
	}
	
	public static void saveAreaToFile(World world, int xMin, int zMin, int xSize, int zSize, int ground, File file) throws IOException {
		byte[] bytes = saveArea(world, xMin, zMin, xSize, zSize, ground);
		Files.write(bytes, file);
	}
	
	public static FakeBlockAccess loadWorld(byte[] data) throws IOException, DataFormatException {
		Header header = new Header();
		try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
			header.read(dis);
		}
		if (header.version > VERSION || header.version < MIN_VERSION)
			throw new DataFormatException("Unsupported menu world version: " + header.version);

		Inflater inflater = new Inflater();
		inflater.setInput(data, Header.SIZE, data.length - Header.SIZE);
		ByteArrayOutputStream output = new ByteArrayOutputStream(header.uncompressedSize);
		byte[] buffer = new byte[1048576];
		while (!inflater.finished()) {
			int len = inflater.inflate(buffer);
			output.write(buffer, 0, len);
		}

		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
		int xSize = dis.readInt();
		int ySize = dis.readInt();
		int zSize = dis.readInt();
		int ground = dis.readInt();
		DimensionType dimensionType = DimensionType.getById(dis.readInt());
		if (dimensionType == null)
			dimensionType = DimensionType.OVERWORLD;
		WorldType worldType = WorldType.byName(dis.readUTF());
		if (worldType == null)
			worldType = WorldType.DEFAULT;
		boolean hasSkyLight = dis.readBoolean();
		BlockStateMapper mapper = new BlockStateMapper();
		mapper.readPalette(dis);
		BlockState[] blocks = new BlockState[xSize * ySize * zSize];
		for (int i = 0; i < blocks.length; i++) {
			blocks[i] = mapper.getState(dis.readInt());
		}
		byte[] skylightmap = new byte[xSize * ySize * zSize];
		byte[] blocklightmap = new byte[xSize * ySize * zSize];
		for (int i = 0; i < skylightmap.length; i++) {
			int b = dis.readByte() & 0xFF;
			skylightmap[i] = (byte)(b & 15);
			blocklightmap[i] = (byte)(b >> 4);
		}
		Biome[] biomemap = new Biome[xSize * zSize];
		for (int i = 0; i < biomemap.length; i++) {
			biomemap[i] = getBiome(dis.readInt());
		}
		
		return new FakeBlockAccess(blocks, skylightmap, blocklightmap, biomemap, xSize, ySize, zSize, ground, dimensionType, worldType, hasSkyLight);
	}
    
	public static FakeBlockAccess loadWorld(InputStream is) throws IOException, DataFormatException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte[] buffer = new byte[1048576];
		int count;
		while ((count = is.read(buffer)) != -1) {
			data.write(buffer, 0, count);
		}
		return loadWorld(data.toByteArray());
	}

	public static int readVersion(File file) throws IOException {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
			Header header = new Header();
			header.read(dis);
			return header.version;
		}
	}

	private static Biome getBiome(int biomeId)
	{
		Biome biome = Registry.BIOME.getByValue(biomeId);
		return biome == null ? Biomes.PLAINS : biome;
	}

	// Just version for now, but could have future use
	public static class Header {
		public static final int SIZE = 8;

		public int version;
		public int uncompressedSize;

		public void read(DataInputStream dis) throws IOException {
			version = dis.readInt();
			uncompressedSize = dis.readInt();
		}

		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(version);
			dos.writeInt(uncompressedSize);
		}
	}

	private static class BlockStateMapper {
		IntIdentityHashBiMap<BlockState> paletteMap = new IntIdentityHashBiMap<>(256);

		int getId(BlockState state) {
			int id = paletteMap.getId(state);
			if (id == -1) {
				return paletteMap.add(state);
			} else {
				return id;
			}
		}

		BlockState getState(int id) {
			return paletteMap.getByValue(id);
		}

		void readPalette(DataInputStream dis) throws IOException {
			paletteMap.clear();
			int size = dis.readInt();
			for (int i = 0; i < size; i++) {
				CompoundNBT tag = new CompoundNBT();
				tag.read(dis, 0, NBTSizeTracker.INFINITE);
				paletteMap.add(NBTUtil.readBlockState(tag));
			}
		}

		void writePalette(DataOutputStream dos) throws IOException {
			dos.writeInt(paletteMap.size());
			for (int i = 0; i < paletteMap.size(); i++) {
				CompoundNBT tag = NBTUtil.writeBlockState(paletteMap.getByValue(i));
				tag.write(dos);
			}
		}
	}
}
