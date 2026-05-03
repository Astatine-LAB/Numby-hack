package cqb13.NumbyHack.modules.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

public class CoordinateConverter extends Command {
    public CoordinateConverter() {
        super("coordinate-converter", "Converts coordinates to opposite dimensions (ow/end <-> nether)",
                "coord-convert",
                "cc");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("x", IntegerArgumentType.integer(-2147483648, 2147483647))
                .then(argument("z", IntegerArgumentType.integer(-2147483648, 2147483647))
                        .executes(context -> {
                            int xLoc = context.getArgument("x", Integer.class);
                            int zLoc = context.getArgument("z", Integer.class);

                            Dimension targetDimension = getOppositeDimension();

                            Vec3 coords = convertCoords(xLoc, zLoc, targetDimension);
                            MutableComponent text = Component.literal("Location in ");
                            text.append(targetDimension.toString() + ": ");
                            text.append(ChatUtils.formatCoords(coords));
                            text.append(".");
                            info(text);
                            return SINGLE_SUCCESS;
                        })));
    }

    private static Dimension getOppositeDimension() {
        Dimension dimension = PlayerUtils.getDimension();

        if (dimension.equals(Dimension.End) || dimension.equals(Dimension.Overworld)) {
            return Dimension.Nether;
        } else {
            return Dimension.Overworld;
        }
    }

    private static Vec3 convertCoords(int xLoc, int zLoc, Dimension targetDimension) {
        if (targetDimension.equals(Dimension.Nether)) {
            return new Vec3(xLoc / 8, mc.player.getY(), zLoc / 8);
        } else {
            return new Vec3(xLoc * 8, mc.player.getY(), zLoc * 8);
        }
    }
}
