/*
 * This file is part of RS: Requestify.
 *
 * Copyright 2018, Buuz135
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.buuz135.refinedstoragerequestify.proxy.block.network;

import com.buuz135.refinedstoragerequestify.proxy.block.tile.TileRequester;
import com.buuz135.refinedstoragerequestify.proxy.config.RequestifyConfig;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.inventory.fluid.FluidInventory;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerBase;
import com.raoulvdberge.refinedstorage.inventory.listener.ListenerNetworkNode;
import com.raoulvdberge.refinedstorage.tile.config.FilterConfig;
import com.raoulvdberge.refinedstorage.tile.config.FilterType;
import com.raoulvdberge.refinedstorage.tile.config.IRSFilterConfigProvider;
import com.raoulvdberge.refinedstorage.util.StackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.List;

public class NetworkNodeRequester extends NetworkNode implements IRSFilterConfigProvider {

    public static final String ID = "requester";

    private static final String NBT_AMOUNT = "Amount";
    private static final String NBT_MISSING = "MissingItems";

    private final FilterConfig config = new FilterConfig.Builder(this)
            .allowedFilterTypeItemsAndFluids()
            .filterTypeItems()
            .filterSizeOne()
            .compareDamageAndNbt()
            .customFilterTypeSupplier((ft) -> world.isRemote ? FilterType.values()[TileRequester.TYPE.getValue()] : ft).build();
    private int amount = 0;
    private boolean isMissingItems = false;
    private ICraftingTask craftingTask = null;

    public NetworkNodeRequester(World world, BlockPos pos) {
        super(world, pos);
    }

    @Override
    public void updateNetworkNode() {
        super.updateNetworkNode();
        if (network == null) return;
        if (canUpdate() && ticks % 10 == 0 && (craftingTask == null || !network.getCraftingManager().getTasks().contains(craftingTask))) {
            if (this.config.isFilterTypeItem()) {
                List<ItemStack> filterList = this.config.getItemFilters();
                if (!filterList.isEmpty()) {
                    ItemStack filter = filterList.get(0);
                    ItemStack current = network.extractItem(filter, amount, Action.SIMULATE);
                    if (current == null || current.isEmpty() || current.getCount() < amount) {
                        int count = current == null || current.isEmpty() ? amount : amount - current.getCount();
                        if (count > 0) {
                            craftingTask = network.getCraftingManager().request(this, filter, Math.min(RequestifyConfig.MAX_CRAFT_AMOUNT, count));
                            isMissingItems = true;
                        }
                    } else {
                        isMissingItems = false;
                    }
                }
            }
            if (this.config.isFilterTypeFluid()) {
                List<FluidStack> filterList = this.config.getFluidFilters();
                if (!filterList.isEmpty()) {
                    FluidStack filter = filterList.get(0);
                    FluidStack current = network.extractFluid(filter, amount, Action.SIMULATE);
                    if (current == null || current.amount < amount) {
                        int count = current == null ? amount : amount - current.amount;
                        if (count > 0) {
                            craftingTask = network.getCraftingManager().request(this, filter, count);
                            isMissingItems = true;
                        }
                    } else {
                        isMissingItems = false;
                    }
                }
            }
        }
    }

    @Override
    public int getEnergyUsage() {
        return 10;
    }

    @Override
    public String getId() {
        return ID;
    }


    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        markNetworkNodeDirty();
    }

    public boolean isMissingItems() {
        return isMissingItems && craftingTask == null;
    }

    @Override
    public void read(NBTTagCompound tag) {
        super.read(tag);
        if (tag.hasKey(NBT_AMOUNT)) {
            amount = tag.getInteger(NBT_AMOUNT);
        }
        if (tag.hasKey(NBT_MISSING)) {
            isMissingItems = tag.getBoolean(NBT_MISSING);
        }
    }

    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        super.write(tag);
        tag.setInteger(NBT_AMOUNT, amount);
        tag.setBoolean(NBT_MISSING, isMissingItems);
        return tag;
    }

    @Override
    public NBTTagCompound writeConfiguration(NBTTagCompound tag) {
        super.writeConfiguration(tag);
        tag.setInteger(NBT_AMOUNT, amount);
        tag.setBoolean(NBT_MISSING, isMissingItems);
        return tag;
    }

    @Override
    public void readConfiguration(NBTTagCompound tag) {
        super.readConfiguration(tag);
        if (tag.hasKey(NBT_AMOUNT)) {
            amount = tag.getInteger(NBT_AMOUNT);
        }
        if (tag.hasKey(NBT_MISSING)) {
            isMissingItems = tag.getBoolean(NBT_MISSING);
        }
    }

    @Nonnull
    @Override
    public FilterConfig getConfig() {
        return config;
    }

    @Override
    public boolean hasConnectivityState() {
        return true;
    }

}
