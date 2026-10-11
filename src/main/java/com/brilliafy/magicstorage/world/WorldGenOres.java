package com.brilliafy.magicstorage.world;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class WorldGenOres implements IWorldGenerator {

    private WorldGenerator hellstoneGen;
    private WorldGenerator luminiteGen;

    public WorldGenOres() {
        this.hellstoneGen = new WorldGenMinable(ModBlocksRef.HELLSTONE_ORE.getDefaultState(), 5, BlockMatcher.forBlock(Blocks.NETHERRACK));
        this.luminiteGen = new WorldGenMinable(ModBlocksRef.LUMINITE_ORE.getDefaultState(), 4, BlockMatcher.forBlock(Blocks.END_STONE));
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        int dim = world.provider.getDimension();
        if (dim == -1) {
            // Nether: Hellstone Ore
            // Generates in Netherrack beneath the Nether lava oceans (strictly below Y: 31)
            for (int i = 0; i < 8; i++) {
                int x = chunkX * 16 + random.nextInt(16);
                int y = 5 + random.nextInt(26); // Y: 5 to 30 (under lava ocean level Y 31)
                int z = chunkZ * 16 + random.nextInt(16);
                hellstoneGen.generate(world, random, new BlockPos(x, y, z));
            }
        } else if (dim == 1) {
            // End: Luminite Ore on outer islands only!
            // Main island radius is ~800-1000 blocks. Require distance >= 1024 blocks (64 chunks)
            long distSq = (long) chunkX * chunkX + (long) chunkZ * chunkZ;
            if (distSq >= 64L * 64L) {
                for (int i = 0; i < 5; i++) {
                    int x = chunkX * 16 + random.nextInt(16);
                    int y = 10 + random.nextInt(65);
                    int z = chunkZ * 16 + random.nextInt(16);
                    luminiteGen.generate(world, random, new BlockPos(x, y, z));
                }
            }
        }
    }
}
