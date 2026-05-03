package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * modified by cqb13
 * Ported from:
 * Tanuki, orignaly by walaryne
 */
public class SafeFire extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> fire = sgGeneral.add(new BoolSetting.Builder()
            .name("fire")
            .description("Prevents you from walking into fire.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> soulFire = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-fire")
            .description("Prevents you from walking into soul fire.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> campfire = sgGeneral.add(new BoolSetting.Builder()
            .name("campfire")
            .description("Prevents you from walking into normal campfires.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> soulCampfire = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-campfire")
            .description("Prevents you from walking into soul campfires.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> lava = sgGeneral.add(new BoolSetting.Builder()
            .name("lava")
            .description("Prevents you from walking into lava.")
            .defaultValue(false)
            .build());

    public SafeFire() {
        super(NumbyHack.CATEGORY, "safe-fire", "Prevents you from walking into fire.");
    }

    @EventHandler
    public void onCollisionShape(CollisionShapeEvent event) {
        if (event.state.getBlock() instanceof FireBlock && fire.get()) {
            event.shape = Shapes.block();
        }

        if (event.state.getBlock() instanceof SoulFireBlock && soulFire.get()) {
            event.shape = Shapes.block();
        }

        if (event.state.getBlock() == Blocks.CAMPFIRE && campfire.get()) {
            event.shape = Shapes.block();
        }

        if (event.state.getBlock() == Blocks.SOUL_CAMPFIRE && soulCampfire.get()) {
            event.shape = Shapes.block();
        }

        if (event.state.getBlock() == Blocks.LAVA && lava.get()) {
            event.shape = Shapes.block();
        }
    }
}
