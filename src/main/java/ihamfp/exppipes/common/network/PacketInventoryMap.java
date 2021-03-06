package ihamfp.exppipes.common.network;

import java.util.ArrayList;

import ihamfp.exppipes.ExppipesMod;
import ihamfp.exppipes.containers.GuiContainerPipeRequest;
import ihamfp.exppipes.tileentities.InvCacheEntry;
import ihamfp.exppipes.tileentities.TileEntityRequestPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

// Actually doesn't send the whole map at once, but only one entry
public class PacketInventoryMap implements IMessage {
	ItemStack stack;
	int count;
	BlockPos pos;
	
	public PacketInventoryMap() {}
	
	public PacketInventoryMap(ItemStack stack, int count, BlockPos pos) {
		this.stack = stack;
		this.count = count;
		this.pos = pos;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.stack = ByteBufUtils.readItemStack(buf);
		this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		this.count = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeItemStack(buf, this.stack);
		buf.writeInt(this.pos.getX());
		buf.writeInt(this.pos.getY());
		buf.writeInt(this.pos.getZ());
		buf.writeInt(count);
	}
	
	public static class Handler implements IMessageHandler<PacketInventoryMap,IMessage> {
		@Override
		public IMessage onMessage(PacketInventoryMap message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				World clientWorld = ExppipesMod.proxy.getClientWorld();
				if (clientWorld == null) return;
				TileEntity tile = clientWorld.getTileEntity(message.pos);
				if (!(tile instanceof TileEntityRequestPipe)) return;
				TileEntityRequestPipe terp = (TileEntityRequestPipe)tile;
				
				if (terp.invCache == null) terp.invCache = new ArrayList<InvCacheEntry>();
				terp.invCache.add(new InvCacheEntry(message.stack, message.count));
				
				if (GuiContainerPipeRequest.sortID) { // sort by id
					terp.invCache.sort((a,b) -> Item.getIdFromItem(a.stack.getItem()) - Item.getIdFromItem(b.stack.getItem()));
				} else {
					terp.invCache.sort((a,b) -> b.count - a.count); // reverse count
				}
			});
			return null;
		}
	}
}