package com.caburum.autotorcher;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTorcherMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("autotorcher");

	private static KeyBinding enableKey;
	private static boolean isEnabled = false;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		enableKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.autotorcher.enable",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_BRACKET,
			"category.autotorcher"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				while (enableKey.wasPressed()) {
					isEnabled = !isEnabled;
					client.player.sendMessage(Text.translatable(isEnabled ? "autotorcher.enabled" : "autotorcher.disabled"), true);
				}
				if (isEnabled)
					placeTorchesWhileMoving(client);
			}
		});
	}

//	private static BlockPos lastLitPos = null;

	private static void placeTorchesWhileMoving(MinecraftClient client) {
		World world = client.world;
		if (world != null && client.player != null) {
			// wait until we get a lighting update
//			if (lastLitPos != null) {
//				if (world.getLightLevel(LightType.BLOCK, lastLitPos) == 0) return;
//				lastLitPos = null;
//			}

			BlockPos playerBlockPos = client.player.getBlockPos();

			final int REACH = 3;
			for (int x = -REACH; x <= REACH; x++) {
				for (int z = -REACH; z <= REACH; z++) {
					BlockPos blockPos = playerBlockPos.add(x, -1, z);
					if (world.getBlockState(blockPos).isFullCube(world, blockPos) && world.getBlockState(blockPos.up()).isAir()) {
						int lightLevel = world.getLightLevel(LightType.BLOCK, blockPos.up());
//						LOGGER.debug(blockPos + " ll " + lightLevel);
						if (lightLevel < 1) {
//							LOGGER.debug("placing torch at " + blockPos);

							if (!client.player.getMainHandStack().getItem().equals(Items.TORCH)) {
								// todo: move slot?
								return;
							}

							// todo: place on wall?
							BlockHitResult hit = new BlockHitResult(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Direction.UP, blockPos, false);
							if (client.interactionManager != null) {
								client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
								client.player.swingHand(Hand.MAIN_HAND, true);
								client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
								world.updateNeighborsAlways(blockPos, Blocks.TORCH);
//								lastLitPos = blockPos.up();
								return;
							}

							client.player.sendMessage(Text.translatable("autotorcher.failed").styled(style -> style.withColor(TextColor.fromFormatting(Formatting.RED))), true);
						}
					}
				}
			}
		}
	}
}