package xyz.iwolfking.woldsvaults.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import xyz.iwolfking.woldsvaults.blocks.tiles.ExtractorForgeTileEntity;
import xyz.iwolfking.woldsvaults.init.ModBlocks;

/** The 5×5×5 Extraction Forge: fabricates parts from identified blueprints and repairs extractors. */
public class ExtractorForgeBlock extends ExtractorControllerBlock {
    public static final int WIDTH = 5;
    public static final int HEIGHT = 5;
    public static final int DEPTH = 5;

    public ExtractorForgeBlock() {
        super(WIDTH, HEIGHT, DEPTH);
    }

    @Override
    public ExtractorStructureBlock getPartBlock() {
        return ModBlocks.EXTRACTION_FORGE_PART_BLOCK;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtractorForgeTileEntity(pos, state);
    }

    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> getServerTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTicker(type, ModBlocks.EXTRACTOR_FORGE_TILE_ENTITY_TYPE, ExtractorForgeTileEntity::tick);
    }
}
