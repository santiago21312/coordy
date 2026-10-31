package com.kodari.coordhud.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class CoordinateHudClient implements ClientModInitializer {
	private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.coordhud.toggle_coordinates",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_M,
			KeyMapping.Category.MISC
		)
	);
	private static final KeyMapping CYCLE_COLORS_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.coordhud.cycle_colors",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_N,
			KeyMapping.Category.MISC
		)
	);
	private static final KeyMapping CYCLE_POSITION_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.coordhud.cycle_position",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_J,
			KeyMapping.Category.MISC
		)
	);
	private static final KeyMapping CYCLE_SIZE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.coordhud.cycle_size",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_K,
			KeyMapping.Category.MISC
		)
	);
	private static final KeyMapping CYCLE_FORMAT_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.coordhud.cycle_format",
			InputConstants.Type.KEYBOARD,
				InputConstants.KEY_H,
			KeyMapping.Category.MISC
		)
	);
	private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("coordhud", "coordinates");
	private static final int[] LABEL_COLORS = {
		0xFFFFFF, 0x000000, 0x000000, 0xFFFFFF, 0x00FFFF,
		0xFF0000, 0xFF00FF, 0xFF69B4, 0xFFA500, 0x0000FF
	};
	private static final int[] VALUE_COLORS = {
		0xFFFFFF, 0x000000, 0xFFFFFF, 0x000000, 0xFFFFFF,
		0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF
	};
	private static boolean visible;
	private static int colorMode;
	private static int positionMode;
	private static int sizeMode;
	private static int formatMode;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (TOGGLE_KEY.consumeClick()) {
				visible = !visible;
			}
				while (CYCLE_COLORS_KEY.consumeClick()) {
					colorMode = (colorMode + 1) % LABEL_COLORS.length;
				}
				while (CYCLE_POSITION_KEY.consumeClick()) {
					positionMode = (positionMode + 1) % 6;
				}
				while (CYCLE_SIZE_KEY.consumeClick()) {
					sizeMode = (sizeMode + 1) % 4;
				}
				while (CYCLE_FORMAT_KEY.consumeClick()) {
					formatMode = (formatMode + 1) % 4;
				}
		});

		HudElementRegistry.addLast(HUD_ID, (graphics, deltaTracker) -> {
			if (!visible) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			if (client.player == null) {
				return;
			}

				int labelColor = LABEL_COLORS[colorMode];
				int valueColor = VALUE_COLORS[colorMode];
					String xValue = formatCoordinate(client.player.getX());
					String yValue = formatCoordinate(client.player.getY());
					String zValue = formatCoordinate(client.player.getZ());
					MutableComponent labeledX = labeledCoordinate("X", xValue, labelColor, valueColor);
					MutableComponent labeledY = labeledCoordinate("Y", yValue, labelColor, valueColor);
					MutableComponent labeledZ = labeledCoordinate("Z", zValue, labelColor, valueColor);
					Component[] coordinateLines = switch (formatMode) {
						case 1 -> new Component[] {
							Component.literal(xValue).withColor(valueColor)
								.append(Component.literal(" / ").withColor(valueColor))
								.append(Component.literal(yValue).withColor(valueColor))
								.append(Component.literal(" / ").withColor(valueColor))
								.append(Component.literal(zValue).withColor(valueColor))
						};
						case 2 -> new Component[] {labeledX, labeledY, labeledZ};
						case 3 -> new Component[] {
							Component.literal(xValue).withColor(valueColor),
							Component.literal(yValue).withColor(valueColor),
							Component.literal(zValue).withColor(valueColor)
						};
						default -> new Component[] {
							labeledX.append(Component.literal(" / ").withColor(valueColor))
								.append(labeledY)
								.append(Component.literal(" / ").withColor(valueColor))
								.append(labeledZ)
						};
					};
					int textWidth = 0;
					for (Component line : coordinateLines) {
						textWidth = Math.max(textWidth, client.font.width(line));
					}
				float scale = (textWidth + sizeMode * 5.0F) / textWidth;
				int scaledTextWidth = textWidth + sizeMode * 5;
					int lineSpacing = client.font.lineHeight + (coordinateLines.length > 1 ? 3 : 0);
					int scaledLineHeight = (int) Math.ceil((client.font.lineHeight * coordinateLines.length
						+ (coordinateLines.length - 1) * (coordinateLines.length > 1 ? 3 : 0)) * scale);
				int guiWidth = graphics.guiWidth();
				int guiHeight = graphics.guiHeight();
				int x;
				int y;
				switch (positionMode) {
					case 1 -> {
						x = 4;
						y = 4;
					}
					case 2 -> {
						x = guiWidth - 4 - scaledTextWidth / 2;
						y = 4;
					}
					case 3 -> {
						x = 4;
						y = guiHeight - scaledLineHeight - 4;
					}
					case 4 -> {
						x = guiWidth / 2;
						y = guiHeight - 39 - scaledLineHeight - 5;
					}
					case 5 -> {
						x = guiWidth - 4 - scaledTextWidth / 2;
						y = guiHeight - scaledLineHeight - 4;
					}
					default -> {
						x = guiWidth / 2;
						y = 4;
					}
				}
				graphics.pose().pushMatrix();
				graphics.pose().translate(x, y);
				graphics.pose().scale(scale, scale);
						for (int line = 0; line < coordinateLines.length; line++) {
							int lineWidth = client.font.width(coordinateLines[line]);
							int lineX = switch (positionMode) {
								case 1, 3 -> lineWidth / 2;
								case 2, 5 -> (textWidth - lineWidth) / 2;
								default -> 0;
							};
							graphics.centeredText(client.font, coordinateLines[line], lineX, line * lineSpacing, 0xFFFFFFFF);
					}
				graphics.pose().popMatrix();
		});
	}

	private static MutableComponent labeledCoordinate(String axis, String value, int labelColor, int valueColor) {
		return Component.literal(axis + ": ").withColor(labelColor)
			.append(Component.literal(value).withColor(valueColor));
	}

	private static String formatCoordinate(double value) {
		return String.format(Locale.ROOT, "%.1f", value).replace('.', ',');
	}
}