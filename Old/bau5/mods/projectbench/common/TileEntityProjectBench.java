package bau5.mods.projectbench.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.common.network.PacketDispatcher;

/**
 * 
 * TileEntityProjectBench
 *
 * @author _bau5
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 * 
 */

public class TileEntityProjectBench extends TileEntity implements IInventory, ISidedInventory
{
	private ItemStack[] inv;
	private Packet nextPacket;
	private boolean shouldUpdate = false; 
	public boolean containerInit = false;
	public IInventory craftResult;
	public InventoryBasic craftSupplyMatrix;
	public LocalInventoryCrafting craftMatrix;
	private ItemStack result = null;
	private ItemStack lastResult;
	private int sync = 0;
	
	public void onInventoryChanged()
	{
		super.onInventoryChanged();
	}
	
	public TileEntityProjectBench()
	{
		craftSupplyMatrix = new InventoryBasic("pbCraftingSupply", true, 18);
		craftResult = new InventoryCraftResult();
		inv = new ItemStack[28];
		shouldUpdate = true;
		craftMatrix = new LocalInventoryCrafting(this);
	}
	
	public ItemStack findRecipe(boolean fromPacket) 
	{
		System.out.println("Finding Recipe.");
		if(worldObj == null)
			return null;
		lastResult = result;
		
		ItemStack stack = null;
		for(int i = 0; i < craftMatrix.getSizeInventory(); ++i) 
		{
			stack = getStackInSlot(i);
			craftMatrix.setInventorySlotContents(i, stack);
		}

		ItemStack recipe = CraftingManager.getInstance().findMatchingRecipe(craftMatrix, worldObj);
		if(recipe == null && validPlanInSlot() && recipeMatchesOutput(getPlanResult()))
			recipe = getPlanResult();
		setResult(recipe);
		
		if(!ItemStack.areItemStacksEqual(lastResult, result) && !fromPacket && !worldObj.isRemote)
			PacketDispatcher.sendPacketToAllInDimension(getDescriptionPacket(),
					worldObj.provider.dimensionId);			
		
		return recipe;
	}
	
	@Override
	public void updateEntity()
    {
		super.updateEntity();
		if(!containerInit && shouldUpdate){
			findRecipe(false);
			shouldUpdate = false;
		}
		if(sync > 6000){
			PacketDispatcher.sendPacketToAllInDimension(getDescriptionPacket(),
					worldObj.provider.dimensionId);
			sync = 0;
		}
    }

	private boolean recipeMatchesOutput(ItemStack planResult) {
		LocalInventoryCrafting crafting = (LocalInventoryCrafting) getPlanCraftingInventory();
		ItemStack foundResult = CraftingManager.getInstance().findMatchingRecipe(crafting, worldObj);
		if(foundResult == null)
			return false;
		else
			return ItemStack.areItemStacksEqual(foundResult, planResult);
	}
	
	public InventoryCrafting getLocalInventoryCrafting(){
		ItemStack stack = null;
		for(int i = 0; i < craftMatrix.getSizeInventory(); ++i) {
			stack = getStackInSlot(i);
			craftMatrix.setInventorySlotContents(i, stack);
		}
		return craftMatrix;
	}	
	
	public LocalInventoryCrafting getPlanCraftingInventory() {
		System.out.println("GetPlanCraftingInventory");
		LocalInventoryCrafting crafting = new LocalInventoryCrafting(this);
		ItemStack[] fromPlanTag = new ItemStack[9];
		NBTTagList tagList = getPlanStack().stackTagCompound.getTagList("Components");
		int i;
		for(i = 0; i < 9; i++){
			NBTTagCompound stackTag = (NBTTagCompound)tagList.tagAt(i);
			if(stackTag.hasKey("id")){
				fromPlanTag[i] = ItemStack.loadItemStackFromNBT(stackTag);
			}else{
				fromPlanTag[i] = null;
			}
			
		}
		for(i = 0; i < 9; i++){
			crafting.setUnlinkedInventory(i, fromPlanTag[i]);
		}
		return crafting;
	}

	public IInventory getSupplyInventory(){
		for(int i = 0; i < craftSupplyMatrix.getSizeInventory(); i++){
			craftSupplyMatrix.setInventorySlotContents(i, inv[i + 9]);
		}
		return craftSupplyMatrix;
	}
	
	public boolean validPlanInSlot(){
		return (getPlanStack() != null && getPlanStack().stackTagCompound != null && getPlanResult() != null);
	}

	public ItemStack getPlanResult() {
		ItemStack stack = (getPlanStack() != null ? ItemStack.loadItemStackFromNBT(getPlanStack().stackTagCompound.getCompoundTag("Result")) : null); 
		return stack;
	}
	
	public ItemStack getPlanStack() {
		return inv[27];
	}
	
	public void markShouldUpdate(){
		shouldUpdate = true;
	}
	public void updateResultSlot(){
		craftResult.setInventorySlotContents(0, result);
	}
	
	public ItemStack[] getCrafting()
	{
		ItemStack[] craftings = new ItemStack[9];
		for(int i = 0; i < 9; i++)
		{
			craftings[i] = inv[i];
		}
		return craftings;
	}
	
	public ItemStack getResult()
	{
		return (result == null) ? null : result.copy();
	}
	public void setResult(ItemStack stack)
	{		
		if(stack != null)
			result = stack.copy();
		else
			result = null;
		updateResultSlot();
	}
	
	@Override
	public int getSizeInventory() 
	{
		return inv.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) 
	{
		return inv[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) 
	{
		ItemStack stack = getStackInSlot(slot);
		if(stack != null)
		{
			if(stack.stackSize <= amount)
			{
				setInventorySlotContents(slot, null);
			} else
			{
				stack = stack.splitStack(amount);
				if(stack.stackSize == 0) 
				{
					setInventorySlotContents(slot, null);
				}else
					onInventoryChanged();
			}
		}
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) 
	{
		ItemStack stack = getStackInSlot(slot);
		if(stack != null)
		{
			setInventorySlotContents(slot, null);
		}
		onInventoryChanged();
		return stack;
	}

	public void shallowSet(int slot, ItemStack stack){
		inv[slot] = stack;
	}
	
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) 
	{
		inv[slot] = stack;
		if(stack != null && stack.stackSize > getInventoryStackLimit())
		{
			stack.stackSize = getInventoryStackLimit();
		}
		if(slot < 9)
			markShouldUpdate();
		onInventoryChanged();
	}
	
	public void shallowSetSlot(int slot, ItemStack stack){
		inv[slot] = stack;
	}

	@Override
	public String getInvName()
	{
		return "Project Bench";
	}

	@Override
	public int getInventoryStackLimit() 
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) 
	{
		return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
				player.getDistanceSq(xCoord +0.5, yCoord +0.5, zCoord +0.5) < 64;
	}
	
	public int[] getRecipeStacksForPacket()
	{
		ItemStack result;
		if(shouldUpdate){
			result = findRecipe(true);
		}
		else{
			result = this.result;
		}
		if(result != null)
		{
			int[] craftingStacks = new int[27];
			int index = 0;
			for(int i = 0; i < 9; i++)
			{
				if(inv[i] != null)
				{
					craftingStacks[index++] = inv[i].itemID;
					craftingStacks[index++] = inv[i].stackSize;
					craftingStacks[index++] = inv[i].getItemDamage();
				} else
				{
					craftingStacks[index++] = 0;
					craftingStacks[index++] = 0;
					craftingStacks[index++] = 0;
				}
			}
			return craftingStacks;
		} else
			return null;
	}

	public void buildResultFromPacket(int[] stacksData)
	{
		if(stacksData == null)
		{
			this.setResult(null);
			return;
		}
		if(stacksData.length != 0 && stacksData[0] > 0)
		{
			this.setResult(new ItemStack(stacksData[0], stacksData[1], stacksData[2]));
		} else
			this.setResult(null);
	}
	@Override
	public Packet getDescriptionPacket()
	{
		NBTTagCompound tag = new NBTTagCompound();
		this.writeToNBT(tag);
		return new Packet132TileEntityData(xCoord, yCoord, zCoord, 1, tag);/*PBPacketHandler.prepPacketMkI(this)*/
	}
	
	@Override
	public void onDataPacket(INetworkManager net, Packet132TileEntityData pkt) {
		super.onDataPacket(net, pkt);
		readFromNBT(pkt.data);
		if(this.worldObj != null)
			findRecipe(true);
	}

	@Override
	public void openChest() {}

	@Override
	public void closeChest() {}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound)
	{
		super.readFromNBT(tagCompound);
		
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
	@Override
	public void writeToNBT(NBTTagCompound tagCompound)
	{
		super.writeToNBT(tagCompound);
		
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
	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		int[] slots = null;
		switch(side){
		case 1: 
			{	slots = new int[10];
				for(int i = 0; i < slots.length; i++)
					slots[i] = i;
				return slots;
			}
		}
		return null;
	}
	@Override
	public boolean canInsertItem(int i, ItemStack itemstack, int j) {
		return true;
	}
	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j) {
		return true;
	}
	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}
	@Override
	public boolean isInvNameLocalized() {
		return false;
	}
}
