package org.vivecraft.api;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.math.vector.Vector3d;

public class AimFixHandler extends ChannelInboundHandlerAdapter {
	private final NetworkManager netManager;

	public AimFixHandler(NetworkManager netManager) {
		this.netManager = netManager;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ServerPlayerEntity player = ((ServerPlayNetHandler)netManager.getNetHandler()).player;
		boolean isCapturedPacket = msg instanceof CPlayerTryUseItemPacket || msg instanceof CPlayerTryUseItemOnBlockPacket || msg instanceof CPlayerDiggingPacket;

		if (!NetworkHelper.isVive(player) || !isCapturedPacket || player.getServer() == null) {
			// we don't need to handle this packet, just defer to the next handler in the pipeline
			ctx.fireChannelRead(msg);
			return;
		}

		player.getServer().runAsync(() -> {
			// Save all the current orientation data
			Vector3d oldPos = player.getPositionVec();
			Vector3d oldPrevPos = new Vector3d(player.prevPosX, player.prevPosY, player.prevPosZ);
			float oldPitch = player.rotationPitch;
			float oldYaw = player.rotationYaw;
			float oldYawHead = player.rotationYawHead;
			float oldPrevPitch = player.prevRotationPitch;
			float oldPrevYaw = player.prevRotationYaw;
			float oldPrevYawHead = player.prevRotationYawHead;

			// Check again in case of race condition
			if (NetworkHelper.isVive(player)) {
				ServerVivePlayer data = NetworkHelper.vivePlayers.get(player.getGameProfile().getId());
				Vector3d pos = data.getControllerPos(0, player);
				Vector3d aim = data.getControllerDir(0);

				// Inject our custom orientation data
				player.setRawPosition(pos.x, pos.y, pos.z);
				player.prevPosX = pos.x;
				player.prevPosY = pos.y;
				player.prevPosZ = pos.z;
				player.rotationPitch = (float)Math.toDegrees(Math.asin(-aim.y));
				player.rotationYaw = (float)Math.toDegrees(Math.atan2(-aim.x, aim.z));
				player.prevRotationPitch = player.rotationPitch;
				player.prevRotationYaw = player.prevRotationYawHead = player.rotationYawHead = player.rotationYaw;
			}

			// Call the packet handler directly
			// This is several implementation details that we have to replicate
			try {
				if (netManager.isChannelOpen()) {
					try {
						((IPacket<INetHandler>)msg).processPacket(netManager.getNetHandler());
					} catch (ThreadQuickExitException e) { // Apparently might get thrown and can be ignored
					}
				}
			} finally {
				// Vanilla uses SimpleInboundChannelHandler, which automatically releases
				// by default, so we're expected to release the packet once we're done.
				ReferenceCountUtil.release(msg);
			}

			// Restore the original orientation data
			player.setRawPosition(oldPos.x, oldPos.y, oldPos.z);
			player.prevPosX = oldPrevPos.x;
			player.prevPosY = oldPrevPos.y;
			player.prevPosZ = oldPrevPos.z;
			player.rotationPitch = oldPitch;
			player.rotationYaw = oldYaw;
			player.rotationYawHead = oldYawHead;
			player.prevRotationPitch = oldPrevPitch;
			player.prevRotationYaw = oldPrevYaw;
			player.prevRotationYawHead = oldPrevYawHead;
		});
	}
}
