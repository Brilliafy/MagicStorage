package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileStorageUnit;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockStorageUnit extends Block implements ITileEntityProvider {

    public enum StorageUnitType implements IStringSerializable {
        BASIC(0, "basic"), CRIMTANE(1,"crimtane"), DEMONITE(2,"demonite"), HELLSTONE(3,"hellstone"),
        HALLOWED(4,"hallowed"), BLUE_CHLORO(5,"blue_chlorophyte"), LUMINITE(6,"luminite"), TERRA(7,"terra");

        final int id; final String name;
        StorageUnitType(int id, String name) { this.id = id; this.name = name; }
        @Override public String getName() { return name; }
        public static StorageUnitType byId(int id) { return values()[Math.min(id, values().length-1)]; }
    }

    public static final PropertyEnum<StorageUnitType> VARIANT = PropertyEnum.create("variant", StorageUnitType.class);

    public BlockStorageUnit() {
        super(Material.IRON);
        setRegistryName(ModInfo.MOD_ID, "storage_unit");
        setTranslationKey(ModInfo.MOD_ID + ".storage_unit");
        setHardness(5.0F); setResistance(10.0F);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, StorageUnitType.BASIC));
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (StorageUnitType type : StorageUnitType.values())
            items.add(new ItemStack(this, 1, type.ordinal()));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, VARIANT); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(VARIANT).ordinal(); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(VARIANT, StorageUnitType.byId(meta)); }
    @Override public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(VARIANT, StorageUnitType.byId(meta));
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        TileStorageUnit unit = new TileStorageUnit();
        unit.setTier(Math.min(meta, 6));
        return unit;
    }

    @Override
    public int damageDropped(IBlockState state) { return state.getValue(VARIANT).ordinal(); }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(fromPos);
        if (te instanceof com.brilliafy.magicstorage.tile.TileStorageHeart) {
            ((com.brilliafy.magicstorage.tile.TileStorageHeart) te).markNeedsRefresh();
        }
        // Also notify when a neighbor tile changes in a way that affects our unit
        TileEntity myTe = worldIn.getTileEntity(pos);
        if (myTe instanceof TileStorageUnit) {
            TileStorageUnit unit = (TileStorageUnit) myTe;
            if (unit.getHeartPos() != null) {
                TileEntity heartTe = worldIn.getTileEntity(unit.getHeartPos());
                if (heartTe instanceof com.brilliafy.magicstorage.tile.TileStorageHeart) {
                    ((com.brilliafy.magicstorage.tile.TileStorageHeart) heartTe).markNeedsRefresh();
                }
            }
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                     float hitX, float hitY, float hitZ) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileStorageUnit)) return true;
        TileStorageUnit unit = (TileStorageUnit) te;
        
        if (playerIn.isSneaking()) {
            // Shift-rightclick: check for upgrade item first, else show info
            ItemStack held = playerIn.getHeldItem(hand);
            if (!held.isEmpty() && held.getItem() instanceof com.brilliafy.magicstorage.item.ItemUpgrade) {
                if (!worldIn.isRemote) {
                    com.brilliafy.magicstorage.item.ItemUpgrade upgrade = (com.brilliafy.magicstorage.item.ItemUpgrade) held.getItem();
                    int newTier = upgrade.getTargetTier();
                    if (unit.canUpgradeTo(newTier)) {
                        unit.setTier(newTier);
                        if (!playerIn.isCreative()) {
                            // Consume upgrade from whichever hand it's in
                            if (hand == net.minecraft.util.EnumHand.MAIN_HAND) {
                                playerIn.inventory.mainInventory.get(playerIn.inventory.currentItem).shrink(1);
                                if (playerIn.inventory.mainInventory.get(playerIn.inventory.currentItem).getCount() <= 0) {
                                    playerIn.inventory.mainInventory.set(playerIn.inventory.currentItem, net.minecraft.item.ItemStack.EMPTY);
                                }
                            } else {
                                playerIn.inventory.offHandInventory.get(0).shrink(1);
                                if (playerIn.inventory.offHandInventory.get(0).getCount() <= 0) {
                                    playerIn.inventory.offHandInventory.set(0, net.minecraft.item.ItemStack.EMPTY);
                                }
                            }
                            playerIn.inventory.markDirty();
                            if (playerIn instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                ((net.minecraft.entity.player.EntityPlayerMP) playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                            }
                        }
                        worldIn.playSound(null, pos, net.minecraft.init.SoundEvents.ITEM_ARMOR_EQUIP_IRON, net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
                        playerIn.sendMessage(new net.minecraft.util.text.TextComponentString("Upgraded to " + unit.getTierName()));
                    } else {
                        playerIn.sendMessage(new net.minecraft.util.text.TextComponentString("Cannot upgrade further"));
                    }
                }
                return true;
            }
            if (!worldIn.isRemote) {
                int slots = unit.getSlotCount();
                int filled = 0;
                for (int i = 0; i < slots; i++) {
                    if (!unit.getInventory().getStackInSlot(i).isEmpty()) filled++;
                }
                float pct = slots > 0 ? (filled * 100.0f / slots) : 0;
                playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GREEN + unit.getTierName() + " Storage Unit"
                ));
                playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                    filled + "/" + slots + " slots used (" + String.format("%.1f", pct) + "% full)"
                ));
            }
            return true;
        }
        
        
        // Regular right-click: open GUI
        if (!worldIn.isRemote) {
            playerIn.openGui(com.brilliafy.magicstorage.MagicStorage.instance,
                2, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileStorageUnit) {
            TileStorageUnit unit = (TileStorageUnit) te;
            
            // Try to redistribute items to other units in the network before dropping
            BlockPos heartPos = unit.getHeartPos();
            if (heartPos != null && worldIn.getTileEntity(heartPos) instanceof com.brilliafy.magicstorage.tile.TileStorageHeart) {
                com.brilliafy.magicstorage.tile.TileStorageHeart heart = (com.brilliafy.magicstorage.tile.TileStorageHeart) worldIn.getTileEntity(heartPos);
                
                // Collect items from the broken unit (slot by slot, not merged)
                java.util.List<ItemStack> itemsToMove = new java.util.ArrayList<>();
                for (int i = 0; i < unit.getSlotCount(); i++) {
                    ItemStack s = unit.getInventory().getStackInSlot(i);
                    if (!s.isEmpty()) {
                        itemsToMove.add(s.copy());
                        unit.getInventory().setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                
                if (!itemsToMove.isEmpty()) {
                    // Get other connected units, sorted by distance from this position
                    java.util.List<BlockPos> otherUnits = new java.util.ArrayList<>(heart.getConnectedUnitPositions());
                    otherUnits.remove(pos);
                    otherUnits.sort((a, b) -> Double.compare(a.distanceSq(pos), b.distanceSq(pos)));
                    
                    // Try to insert each item into the nearest units with space
                    java.util.List<ItemStack> leftovers = new java.util.ArrayList<>();
                    for (ItemStack item : itemsToMove) {
                        ItemStack remainder = item;
                        for (BlockPos otherPos : otherUnits) {
                            if (remainder.isEmpty()) break;
                            TileEntity otherTe = worldIn.getTileEntity(otherPos);
                            if (otherTe instanceof TileStorageUnit) {
                                TileStorageUnit otherUnit = (TileStorageUnit) otherTe;
                                remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(otherUnit.getInventory(), remainder, false);
                            }
                        }
                        if (!remainder.isEmpty()) {
                            leftovers.add(remainder);
                        }
                    }
                    
                    // Drop leftovers
                    for (ItemStack leftover : leftovers) {
                        net.minecraft.inventory.InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), leftover);
                    }
                    
                    // Trigger heart refresh to remove this unit from the network
                    heart.markNeedsRefresh();
                }
            } else {
                // Not connected to a heart — drop all contents
                unit.dropContents(worldIn, pos);
            }
        }
        super.breakBlock(worldIn, pos, state);
    }
}
