package bau5.mods.craftingsuite.common.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import bau5.mods.craftingsuite.common.inventory.EnumInventoryModifier;

import com.google.common.collect.EnumBiMap;

public interface IModifiedTileEntityProvider {
	public EnumInventoryModifier getInventoryModifier();
	public int getModifiedInventorySize();
	public int getBaseInventorySize();
	public void initializeFromNBT(NBTTagCompound modifierTag);
	public void handleModifiers();
	
	public int getToolModifierInvIndex();
	public byte getDirectionFacing();
	public void setDirectionFacing(byte byt);
}