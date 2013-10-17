package bau5.mods.craftingsuite.common.tileentity.parthandlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import bau5.mods.craftingsuite.common.tileentity.TileEntityModdedTable;
import cpw.mods.fml.common.network.PacketDispatcher;

public class InventoryHandler implements IInventory{
	public ItemStack[] inv = null;
	private TileEntityModdedTable tileEntity;
	private IInventory craftResult = new InventoryCraftResult();
	private LocalInventoryCrafting craftingMatrix = new LocalInventoryCrafting();
	private LocalInventoryCrafting lastCraftMatrix= new LocalInventoryCrafting();
	private int[] craftingInventoryRange = new int[2];
	
	public ItemStack result = null;
	public ItemStack lastResult = null;
	
	public boolean shouldUpdate = false;
	
	public InventoryHandler(TileEntityModdedTable tile){
		tileEntity = tile;
	}
	
	public void initInventory() {
		inv = new ItemStack[tileEntity.modifications().getSizeInventory()];
		craftingInventoryRange = tileEntity.modifications().getCrafingRange();
	}

	public ItemStack findRecipe(boolean fromPacket){
		if(tileEntity.worldObj == null)
			return null;
		lastResult = result;
		ItemStack stack = null;
		for(int i = 0; i < craftingMatrix.getSizeInventory(); ++i) 
		{
			stack = getStackInSlot(i + craftingInventoryRange[0]);
			craftingMatrix.setInventorySlotContents(i, stack);
		}
	
		ItemStack recipe = CraftingManager.getInstance().findMatchingRecipe(craftingMatrix, tileEntity.worldObj);
		setResult(recipe);
		
		if(!ItemStack.areItemStacksEqual(lastResult, result) && !fromPacket && !tileEntity.worldObj.isRemote){
			PacketDispatcher.sendPacketToAllAround(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 30D, tileEntity.worldObj.provider.dimensionId, tileEntity.getDescriptionPacket());
		}
		return recipe;
	}

	private void setResult(ItemStack recipe) {
		result = recipe;
		craftResult.setInventorySlotContents(0, recipe);
	}
	
	public void onTileInventoryChanged() {
		if(!tileEntity.containerHandler().isContainerInit() && !tileEntity.containerHandler().isContainerWorking()){
			if(checkDifferences()){
				markForUpdate();
				makeNewMatrix();
			}
		}
	}
	
	private void makeNewMatrix() {
		for(int i = 0; i < 9; i++){
			lastCraftMatrix.setInventorySlotContents(i, inv[i +craftingInventoryRange[0]]);
		}
	}

	public void markForUpdate(){
		shouldUpdate = true;
	}
	
	private boolean checkDifferences() {
		for(int i = 0; i < 9; i++){
			if(!ItemStack.areItemStacksEqual(lastCraftMatrix.getStackInSlot(i), inv[i]))
				return true;
		}
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}
	
	@Override
	public int getSizeInventory() {
		return inv.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return inv[i];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		ItemStack stack = getStackInSlot(slot);
		if(stack != null){
			if(stack.stackSize <= amount){
				setInventorySlotContents(slot, null);
			}else{
				stack = stack.splitStack(amount);
				if(stack.stackSize == 0){
					setInventorySlotContents(slot, null);
				}else
					onInventoryChanged();
			}
		}
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		ItemStack stack = getStackInSlot(slot);
		if(stack != null){
			setInventorySlotContents(slot, null);
		}
		onInventoryChanged();
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		inv[slot] = stack;
		if(stack != null && stack.stackSize > getInventoryStackLimit()){
			stack.stackSize = getInventoryStackLimit();
		}
	}

	@Override
	public String getInvName() {
		return "Modded Crafting Table";
	}

	@Override
	public boolean isInvNameLocalized() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return tileEntity.worldObj.getBlockTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord) == tileEntity &&
				entityplayer.getDistanceSq(tileEntity.xCoord +0.5, tileEntity.yCoord +0.5, tileEntity.zCoord +0.5) < 64;
	}

	@Override
	public void openChest() {}

	@Override
	public void closeChest() {}


	public void readInventoryFromNBT(NBTTagCompound tagCompound) {
		NBTTagList tagList = tagCompound.getTagList("Inventory");
		for(int i = 0; i < tagList.tagCount(); i++)
		{
			NBTTagCompound tag = (NBTTagCompound) tagList.tagAt(i);
			byte slot = tag.getByte("Slot");
			if(slot >= 0 && slot < inv.length)
			{
				inv[slot] = ItemStack.loadItemStackFromNBT(tag);
			}
		}
	}


	public void writeInventoryToNBT(NBTTagCompound tagCompound) {	
		NBTTagList itemList = new NBTTagList();	
		for(int i = 0; i < inv.length; i++)
		{
			ItemStack stack = inv[i];
			if(stack != null)
			{
				NBTTagCompound tag = new NBTTagCompound();	
				tag.setByte("Slot", (byte)i);
				stack.writeToNBT(tag);
				itemList.appendTag(tag);
			}
		}
		tagCompound.setTag("Inventory", itemList);
	}

	public IInventory resultMatrix() {
		return craftResult;
	}
	
	public class LocalInventoryCrafting extends InventoryCrafting{
		private TileEntity theTile;
		public LocalInventoryCrafting() {
			super(new Container(){
				@Override
				public boolean canInteractWith(EntityPlayer var1) {
					return false;
				}
			}, 3, 3);
		}
		public LocalInventoryCrafting(TileEntity tileEntity){
			this();
			theTile = tileEntity;
		}
	}

	@Override
	public void onInventoryChanged() {
		onTileInventoryChanged();
	}
	
}