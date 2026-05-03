package cqb13.NumbyHack.modules.general;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joml.Vector3d;

import cqb13.NumbyHack.NumbyHack;
import cqb13.NumbyHack.utils.TimerUtils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LogOutSpots extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Boolean> nameRender = sgGeneral.add(new BoolSetting.Builder()
            .name("name")
            .description("Shows the name of the player.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> healthRender = sgGeneral.add(new BoolSetting.Builder()
            .name("health")
            .description("Shows the health of the player.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> coordRender = sgGeneral.add(new BoolSetting.Builder()
            .name("coordinates")
            .description("Shows the coordinates of the player.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> armorCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("armor-check")
            .description("Checks if the player has armor on.")
            .defaultValue(true)
            .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the text.")
            .defaultValue(1)
            .min(0.2)
            .sliderRange(0.2, 2)
            .build());

    private final Setting<Boolean> notification = sgGeneral.add(new BoolSetting.Builder()
            .name("notification")
            .description("Notifies you when a player logs out.")
            .defaultValue(true)
            .build());

    // Render
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("The shape.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(146, 188, 98, 10)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(146, 188, 98, 255))
            .build());

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
            .name("name-color")
            .description("The name color.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build());

    private final Setting<SettingColor> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
            .name("name-background-color")
            .description("The name background color.")
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build());

    private final List<Entry> players = new ArrayList<>();

    private final List<PlayerInfo> lastPlayerList = new ArrayList<>();
    private final List<Player> lastPlayers = new ArrayList<>();

    private int timer;
    private Dimension lastDimension;

    public LogOutSpots() {
        super(NumbyHack.CATEGORY, "log-spots-+", "Displays a box where another player has logged out at.");
        lineColor.onChanged();
    }

    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getConnection().getOnlinePlayers());
        updateLastPlayers();

        timer = 10;
        lastDimension = PlayerUtils.getDimension();
    }

    @Override
    public void onDeactivate() {
        players.clear();
        lastPlayerList.clear();
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player)
                lastPlayers.add((Player) entity);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof Player) {
            int toRemove = -1;

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUUID())) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getConnection().getOnlinePlayers().size() != lastPlayerList.size()) {
            for (PlayerInfo entry : lastPlayerList) {
                if (mc.getConnection().getOnlinePlayers().stream()
                        .anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile())))
                    continue;

                for (Player player : lastPlayers) {
                    if (player.getUUID().equals(entry.getProfile().id())) {
                        boolean validArmor = true;
                        if (armorCheck.get()) {
                            for (int position = 1; position <= 4; position++) {
                                ItemStack itemStack = getItem(position, player);

                                if (itemStack.isEmpty()) {
                                    validArmor = false;
                                    break;
                                }
                            }
                        }

                        if (!validArmor)
                            break;

                        if (notification.get()) {
                            ChatUtils.sendMsg(Component.literal(player.getName().getString() + " Logged out!"));
                        }
                        add(new Entry(player));
                        break;
                    }
                }
            }

            lastPlayerList.clear();
            lastPlayerList.addAll(mc.getConnection().getOnlinePlayers());
            updateLastPlayers();
        }

        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }

        Dimension dimension = PlayerUtils.getDimension();
        if (dimension != lastDimension)
            players.clear();
        lastDimension = dimension;
    }

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    private ItemStack getItem(int index, Player entity) {
        return switch (index) {
            case 0 -> entity.getMainHandItem();
            case 1 -> entity.getItemBySlot(EquipmentSlot.HEAD);
            case 2 -> entity.getItemBySlot(EquipmentSlot.CHEST);
            case 3 -> entity.getItemBySlot(EquipmentSlot.LEGS);
            case 4 -> entity.getItemBySlot(EquipmentSlot.FEET);
            case 5 -> entity.getOffhandItem();
            default -> ItemStack.EMPTY;
        };
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (Entry player : players)
            player.render3D(event);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entry player : players)
            player.render2D();
    }

    private static final Vector3d pos = new Vector3d();

    private class Entry {
        public final double x, y, z;
        public final double halfWidth, height;

        public final TimerUtils passed = new TimerUtils();

        public final UUID uuid;
        public final String name;
        public final int health;
        public final String healthText;
        Player entity;

        public Entry(Player entity) {

            passed.reset();
            halfWidth = entity.getBbWidth() / 2;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;

            height = entity.getBoundingBox().getYsize();

            this.entity = entity;

            uuid = entity.getUUID();
            name = entity.getName().getString();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());

            healthText = " " + health;
        }

        public void render3D(Render3DEvent event) {
            WireframeEntityRenderer.render(event, entity, scale.get(), sideColor.get(), lineColor.get(),
                    shapeMode.get());
        }

        public void render2D() {
            if (PlayerUtils.distanceToCamera(x, y, z) > mc.options.renderDistance().get() * 16)
                return;

            TextRenderer text = TextRenderer.get();
            double s = scale.get();
            pos.set(x + halfWidth, y + height + 0.5, z + halfWidth);

            if (!NametagUtils.to2D(pos, s))
                return;

            NametagUtils.begin(pos);

            String content = "";
            if (nameRender.get())
                content = content + name;
            if (healthRender.get())
                content = content + " " + healthText + "HP";
            if (coordRender.get())
                content = content + " (" + Math.round(entity.getX()) + " " + Math.round(entity.getY()) + " "
                        + Math.round(entity.getZ()) + ")";

            // Render background
            double i = text.getWidth(content) / 2;
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-i, 0, i * 2, text.getHeight(), nameBackgroundColor.get());
            Renderer2D.COLOR.render();

            // Render name and health texts
            text.beginBig();
            if (nameRender.get())
                text.render(content, -i, 0, nameColor.get());
            text.end();

            NametagUtils.end();
        }
    }
}
