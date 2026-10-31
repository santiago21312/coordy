package com.kodari.coordhud.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class CoordinateHudClient implements ClientModInitializer {
	private static final String CATEGORY = "key.categories.misc";
	private static final KeyMapping TOGGLE_KEY = register("key.coordhud.toggle_coordinates", InputConstants.KEY_M);
	private static final KeyMapping CYCLE_COLORS_KEY = register("key.coordhud.cycle_colors", InputConstants.KEY_N);
	private static final KeyMapping CYCLE_POSITION_KEY = register("key.coordhud.cycle_position", InputConstants.KEY_J);
	private static final KeyMapping CYCLE_SIZE_KEY = register("key.coordhud.cycle_size", InputConstants.KEY_K);
	private static final KeyMapping CYCLE_FORMAT_KEY = register("key.coordhud.cycle_format", InputConstants.KEY_H);
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

	private static KeyMapping register(String name, int keyCode) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, CATEGORY)
		);
	}

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

		HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
	}

	private static void render(GuiGraphics graphics) {
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

		Component[] coordinateLines;
		if (formatMode == 1) {
			MutableComponent line = colored(xValue, valueColor);
			line.append(colored(" / ", valueColor));
			line.append(colored(yValue, valueColor));
			line.append(colored(" / ", valueColor));
			line.append(colored(zValue, valueColor));
			coordinateLines = new Component[] {line};
		} else if (formatMode == 2) {
			coordinateLines = new Component[] {
				labeledCoordinate("X", xValue, labelColor, valueColor),
				labeledCoordinate("Y", yValue, labelColor, valueColor),
				labeledCoordinate("Z", zValue, labelColor, valueColor)
			};
		} else if (formatMode == 3) {
			coordinateLines = new Component[] {
				colored(xValue, valueColor),
				colored(yValue, valueColor),
				colored(zValue, valueColor)
			};
		} else {
			MutableComponent line = labeledCoordinate("X", xValue, labelColor, valueColor);
			line.append(colored(" / ", valueColor));
			line.append(labeledCoordinate("Y", yValue, labelColor, valueColor));
			line.append(colored(" / ", valueColor));
			line.append(labeledCoordinate("Z", zValue, labelColor, valueColor));
			coordinateLines = new Component[] {line};
		}

		int textWidth = 0;
		for (Component line : coordinateLines) {
			textWidth = Math.max(textWidth, client.font.width(line));
		}
		float scale = (textWidth + sizeMode * 5.0F) / textWidth;
		int scaledTextWidth = textWidth + sizeMode * 5;
		int extraSpacing = coordinateLines.length > 1 ? 3 : 0;
		int lineSpacing = client.font.lineHeight + extraSpacing;
		int scaledLineHeight = (int) Math.ceil((client.font.lineHeight * coordinateLines.length
			+ (coordinateLines.length - 1) * extraSpacing) * scale);
		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		int x;
		int y;
		switch (positionMode) {
			case 1:
				x = 4;
				y = 4;
				break;
			case 2:
				x = guiWidth - 4 - scaledTextWidth / 2;
				y = 4;
				break;
			case 3:
				x = 4;
				y = guiHeight - scaledLineHeight - 4;
				break;
			case 4:
				x = guiWidth / 2;
				y = guiHeight - 39 - scaledLineHeight - 5;
				break;
			case 5:
				x = guiWidth - 4 - scaledTextWidth / 2;
				y = guiHeight - scaledLineHeight - 4;
				break;
			default:
				x = guiWidth / 2;
				y = 4;
				break;
		}

		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate((double) x, (double) y, 0.0D);
		pose.scale(scale, scale, 1.0F);
		for (int line = 0; line < coordinateLines.length; line++) {
			int lineWidth = client.font.width(coordinateLines[line]);
			int lineX;
			switch (positionMode) {
				case 1:
				case 3:
					lineX = lineWidth / 2;
					break;
				case 2:
				case 5:
					lineX = (textWidth - lineWidth) / 2;
					break;
				default:
					lineX = 0;
					break;
			}
			graphics.drawCenteredString(client.font, coordinateLines[line], lineX, line * lineSpacing, 0xFFFFFFFF);
		}
		pose.popPose();
	}

	private static MutableComponent labeledCoordinate(String axis, String value, int labelColor, int valueColor) {
		MutableComponent label = colored(axis + ": ", labelColor);
		label.append(colored(value, valueColor));
		return label;
	}

	private static MutableComponent colored(String text, int rgb) {
		MutableComponent component = Component.literal(text);
		component.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
		return component;
	}

	private static String formatCoordinate(double value) {
		return String.format(Locale.ROOT, "%.1f", value).replace('.', ',');
	}
}
