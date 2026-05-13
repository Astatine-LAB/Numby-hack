package cqb13.NumbyHack.modules.general;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.apache.commons.lang3.math.Fraction;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

/**
 * Heavily inspired by map copy from Ev Mod
 *
 * https://github.com/EvModder/EvMod
 */
public class BundleCopy extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
            .name("action-delay-ms")
            .description("Delay between clicks in milliseconds.")
            .defaultValue(50)
            .sliderMin(0)
            .sliderMax(100)
            .min(0)
            .max(500)
            .build());

    private final Setting<Keybind> copyKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("copy-key")
            .description("Starts copying or cancels copying.")
            .defaultValue(Keybind.fromKey(GLFW_KEY_C))
            .build());

    private long lastCopy;
    private final long copyCooldown = 250L;

    private static final int HOTBAR_END = 44;

    private enum ActionType {
        CLICK, SHIFT_CLICK, HOTBAR_SWAP, BUNDLE_SELECT
    }

    private static final class InvAction {
        final int slot;
        final int button;
        final ActionType type;

        InvAction(int slot, int button, ActionType type) {
            this.slot = slot;
            this.button = button;
            this.type = type;
        }
    }

    private final ArrayDeque<InvAction> clickQueue = new ArrayDeque<>();
    private boolean processing = false;
    private int ticksWaited = 0;

    private InvAction pendingAction = null;
    private int pendingTicks = 0;

    public BundleCopy() {
        super(NumbyHack.CATEGORY, "bundle-copy",
                "Copies maps in bundles, have 2 empty bundles and a bundle with maps in your inventory then press the copy key.");
    }

    private static final class ConstFields {
        final int RESULT, INPUT_START, INV_START, HOTBAR_START, HOTBAR_END;

        ConstFields(int RESULT, int INPUT_START, int INPUT_END, int INV_START, int INV_END, int HOTBAR_START,
                int HOTBAR_END) {
            this.RESULT = RESULT;
            this.INPUT_START = INPUT_START;
            this.INV_START = INV_START;
            this.HOTBAR_START = HOTBAR_START;
            this.HOTBAR_END = HOTBAR_END;
        }
    }

    private final ConstFields INV = new ConstFields(0, 1, 5, 9, 36, 36, 45);

    @Override
    public void onActivate() {
        clickQueue.clear();
        processing = false;
        ticksWaited = 0;
    }

    @Override
    public void onDeactivate() {
        clickQueue.clear();
        processing = false;
        ticksWaited = 0;
    }

    @EventHandler
    private void onKeyPress(KeyInputEvent event) {
        if (event.action != KeyAction.Press) {
            return;
        }

        if (mc.screen == null) {
            return;
        }

        if (event.key() != copyKey.get().getValue()) {
            return;
        }

        if (!clickQueue.isEmpty()) {
            clickQueue.clear();
            processing = false;
            ticksWaited = 0;
            info("Cancelled");
            return;
        }

        if (!(mc.screen instanceof InventoryScreen)) {
            return;
        }

        if (System.currentTimeMillis() - lastCopy < copyCooldown) {
            return;
        }

        lastCopy = System.currentTimeMillis();
        buildQueue();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        int delayTicks = Math.max(1, (int) Math.ceil(actionDelay.get() / 50.0));
        if (ticksWaited < delayTicks) {
            ticksWaited++;
            return;
        }
        ticksWaited = 0;

        if (pendingAction != null) {
            pendingTicks++;
            int pendingThreshold = Math.max(1, (int) Math.ceil(actionDelay.get() / 50.0));
            if (pendingTicks >= pendingThreshold) {
                pendingAction = null;
                pendingTicks = 0;
            }
            return;
        }

        if (clickQueue.isEmpty()) {
            if (processing) {
                processing = false;
                info("Done");
            }
            return;
        }

        InvAction action = clickQueue.poll();
        executeClick(action);
        pendingAction = action;
        pendingTicks = 0;
    }

    private void buildQueue() {
        final ItemStack[] slots = new ItemStack[HOTBAR_END + 1];
        for (int i = 0; i <= HOTBAR_END; ++i) {
            slots[i] = mc.player.getInventory().getItem(i);
        }

        // Count empty maps in grid
        int numEmptyMapsInGrid = 0;
        ItemStack in = slots[INV.INPUT_START + 1];
        if (!in.isEmpty() && in.getItem() == Items.MAP) {
            numEmptyMapsInGrid = in.getCount();
        }

        int lastEmptyMapSlot = -1;
        for (int i = HOTBAR_END; i >= INV.INV_START; --i) {
            if (slots[i].getItem() == Items.MAP) {
                lastEmptyMapSlot = i;
                break;
            }
        }

        if (lastEmptyMapSlot == -1) {
            error("No empty maps found");
            return;
        }

        final int totalEmptyMaps = IntStream.rangeClosed(INV.INV_START, lastEmptyMapSlot)
                .filter(i -> slots[i].getItem() == Items.MAP).map(i -> slots[i].getCount()).sum();

        ArrayDeque<InvAction> clicks = new ArrayDeque<>();
        copyMapArtInBundles(clicks, slots, INV, numEmptyMapsInGrid, totalEmptyMaps);
        if (clicks.isEmpty()) {
            error("Nothing to copy");
            return;
        }

        clickQueue.addAll(clicks);

        if (clickQueue.isEmpty()) {
            error("Nothing to copy");
            return;
        }

        processing = true;
        info("Copying maps...");
    }

    private void executeClick(InvAction a) {
        if (mc.player == null || mc.gameMode == null) {
            return;
        }

        try {
            int syncId = 0;

            switch (a.type) {
                case CLICK -> mc.gameMode.handleContainerInput(syncId, a.slot, a.button,
                        ContainerInput.PICKUP, mc.player);
                case SHIFT_CLICK -> mc.gameMode.handleContainerInput(syncId, a.slot, a.button,
                        ContainerInput.QUICK_MOVE, mc.player);
                case HOTBAR_SWAP -> mc.gameMode.handleContainerInput(syncId, a.slot, a.button,
                        ContainerInput.SWAP, mc.player);
                case BUNDLE_SELECT -> {
                    mc.gameMode.handleContainerInput(syncId, a.slot, a.button, ContainerInput.PICKUP, mc.player);
                }
            }
        } catch (Exception e) {
            error("Click error: " + e.getMessage());
        }
    }

    private final int getEmptyMapsIntoInput(final ArrayDeque<InvAction> clicks, final ItemStack[] slots,
            final ConstFields f, final int amtNeeded, int amtInGrid, final int dontLeaveEmptySlotsAfterThisSlot) {
        for (int j = f.INV_START; j < f.HOTBAR_END && amtInGrid < amtNeeded; ++j) {
            if (slots[j].getItem() != Items.MAP) {
                continue;
            }

            final boolean leaveOne = j > dontLeaveEmptySlotsAfterThisSlot;
            if (leaveOne && slots[j].getCount() == 1) {
                continue;
            }

            final int combinedCnt = slots[j].getCount() + amtInGrid;
            final int combinedHalfCnt = (slots[j].getCount() + 1) / 2 + amtInGrid;

            if (j >= f.HOTBAR_START && (!leaveOne || amtInGrid > 0) && slots[j].getCount() >= amtNeeded) {
                clicks.add(new InvAction(f.INPUT_START + 1, j - f.HOTBAR_START, ActionType.HOTBAR_SWAP));
                amtInGrid = slots[j].getCount();
                slots[j].setCount(combinedCnt - amtInGrid);
                break;
            } else if (combinedCnt <= 64) {
                clicks.add(new InvAction(j, 0, ActionType.CLICK));
                clicks.add(new InvAction(f.INPUT_START + 1, 0, ActionType.CLICK));
                slots[j] = ItemStack.EMPTY;
                amtInGrid = combinedCnt;
            } else if (slots[j].getCount() > 1 && combinedHalfCnt >= amtNeeded && combinedHalfCnt <= 64) {
                clicks.add(new InvAction(j, 1, ActionType.CLICK));
                clicks.add(new InvAction(f.INPUT_START + 1, 0, ActionType.CLICK));
                slots[j].setCount(slots[j].getCount() / 2);
                amtInGrid = combinedHalfCnt;
            } else {
                clicks.add(new InvAction(j, 0, ActionType.CLICK));
                clicks.add(new InvAction(f.INPUT_START + 1, 0, ActionType.CLICK));
                clicks.add(new InvAction(j, 0, ActionType.CLICK));
                slots[j].setCount(combinedCnt - 64);
                amtInGrid = 64;
            }
        }

        return amtInGrid;
    }

    private final int lastEmptySlot(ItemStack[] slots, final int END, final int START) {
        for (int i = END - 1; i >= START; --i) {
            if (slots[i].isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    private final int getNumStored(Fraction fraction) {
        assert 64 % fraction.getDenominator() == 0;
        return (64 / fraction.getDenominator()) * fraction.getNumerator();
    }

    private final String getCustomNameOrNull(ItemStack stack) {
        final var text = stack.get(DataComponents.ITEM_NAME);
        return text == null ? null : text.getString();
    }

    private record PrioAndSlot(int p, int slot) implements Comparable<PrioAndSlot> {
        @Override
        public int compareTo(PrioAndSlot o) {
            return p != o.p ? p - o.p : slot - o.slot;
        }
    }

    private void copyMapArtInBundles(final ArrayDeque<InvAction> clicks, final ItemStack[] slots, final ConstFields f,
            int numEmptyMapsInGrid, final int totalEmptyMaps) {
        final int[] slotsWithBundles = IntStream.range(f.INV_START, f.HOTBAR_END).filter(i -> {
            BundleContents contents = slots[i].get(DataComponents.BUNDLE_CONTENTS);
            if (contents == null)
                return false;
            for (var t : contents.items()) {
                if (t.item().value() != Items.FILLED_MAP)
                    return false;
            }
            return true;
        }).toArray();
        final BundleContents[] bundles = Arrays.stream(slotsWithBundles)
                .mapToObj(i -> slots[i].get(DataComponents.BUNDLE_CONTENTS))
                .toArray(BundleContents[]::new);
        final int SRC_BUNDLES = (int) Arrays.stream(bundles).filter(Predicate.not(BundleContents::isEmpty))
                .count();
        final int USABLE_EMPTY_BUNDLES = bundles.length - SRC_BUNDLES - 1;
        if (USABLE_EMPTY_BUNDLES <= 0) {
            error("Could not find usable empty bundle");
            return;
        }
        int LAST_EMPTY_SLOT = lastEmptySlot(slots, f.HOTBAR_END, f.INV_START);
        final int DESTS_PER_SRC = SRC_BUNDLES > USABLE_EMPTY_BUNDLES ? 999 : USABLE_EMPTY_BUNDLES / SRC_BUNDLES;

        TreeMap<Integer, List<Integer>> bundlesToCopy = new TreeMap<>();
        HashSet<Integer> usedDests = new HashSet<>();
        HashSet<Item> srcBundleTypes = new HashSet<>();
        HashSet<Item> dstBundleTypes = new HashSet<>();
        for (int i = 0; i < slotsWithBundles.length; ++i) {
            final int s1 = slotsWithBundles[i];
            if (bundles[i].isEmpty()) {
                continue;
            }

            srcBundleTypes.add(slots[s1].getItem());
            ArrayList<Integer> copyDests = new ArrayList<>();
            final String name1 = getCustomNameOrNull(slots[s1]);
            if (name1 != null) {
                for (int j = 0; j < slotsWithBundles.length && usedDests.size() < USABLE_EMPTY_BUNDLES
                        && copyDests.size() < DESTS_PER_SRC; ++j) {
                    if (bundles[j].isEmpty() && name1.equals(getCustomNameOrNull(slots[slotsWithBundles[j]]))
                            && usedDests.add(j)) {
                        copyDests.add(j);
                    }
                }
            }

            if (copyDests.isEmpty() && slots[s1].getItem() != Items.BUNDLE) {
                for (int j = 0; j < slotsWithBundles.length && usedDests.size() < USABLE_EMPTY_BUNDLES
                        && copyDests.size() < DESTS_PER_SRC; ++j) {
                    if (bundles[j].isEmpty() && slots[s1].getItem() == slots[slotsWithBundles[j]].getItem()
                            && (name1 == null || getCustomNameOrNull(slots[slotsWithBundles[j]]) == null)
                            && usedDests.add(j)) {
                        copyDests.add(j);
                    }
                }
            }

            if (copyDests.isEmpty()) {
                for (int j = 0; j < slotsWithBundles.length && usedDests.size() < USABLE_EMPTY_BUNDLES
                        && copyDests.size() < DESTS_PER_SRC; ++j) {
                    final int s2 = slotsWithBundles[j];
                    if (!bundles[j].isEmpty())
                        continue;
                    if (name1 != null && getCustomNameOrNull(slots[s2]) != null
                            && !name1.equals(getCustomNameOrNull(slots[s2])))
                        continue;
                    if (SRC_BUNDLES != USABLE_EMPTY_BUNDLES && slots[s1].getItem() != slots[s2].getItem())
                        continue;
                    if (!usedDests.add(j))
                        continue;
                    copyDests.add(j);
                }
            }

            if (copyDests.isEmpty()) {
                error("Could not determine destination bundles");
                return;
            }
            for (int j : copyDests) {
                dstBundleTypes.add(slots[slotsWithBundles[j]].getItem());
            }
            bundlesToCopy.put(i, copyDests);
        }
        final int emptyMapsNeeded = bundlesToCopy.entrySet().stream()
                .mapToInt(e -> getNumStored(bundles[e.getKey()].weight().getOrThrow()) * e.getValue().size()).sum();
        if (totalEmptyMaps < emptyMapsNeeded) {
            error("Insufficient empty maps");
            return;
        }

        final int tempBundleSlot;
        HashSet<Integer> unusedBundles = new HashSet<>();
        for (int i = 0; i < slotsWithBundles.length; ++i) {
            unusedBundles.add(i);
        }

        for (var e : bundlesToCopy.entrySet()) {
            unusedBundles.remove(e.getKey());
            unusedBundles.removeAll(e.getValue());
        }

        final int[] unusedBundleSlots = unusedBundles.stream().mapToInt(i -> slotsWithBundles[i]).toArray();
        final boolean anyUnnamedDst = bundlesToCopy.values().stream()
                .anyMatch(d -> d.stream()
                        .anyMatch(i -> slots[slotsWithBundles[i]].get(DataComponents.ITEM_NAME) == null));

        final PrioAndSlot pas = Arrays.stream(unusedBundleSlots)
                .mapToObj(i -> new PrioAndSlot(
                        ((!anyUnnamedDst && slots[i].get(DataComponents.ITEM_NAME) != null ? 4 : 0)
                                + (srcBundleTypes.contains(slots[i].getItem()) ? 2 : 0)
                                + (dstBundleTypes.contains(slots[i].getItem()) ? 1 : 0)),
                        i))
                .min(Comparator.naturalOrder()).get();

        tempBundleSlot = pas.slot;

        for (var entry : bundlesToCopy.entrySet()) {
            BundleContents content = bundles[entry.getKey()];

            ItemStack[] contentItems = new ItemStack[content.size()];
            int ci = 0;
            for (var t : content.items()) {
                contentItems[ci++] = t.create();
            }

            for (int _0 = 0; _0 < contentItems.length; ++_0) {
                clicks.add(new InvAction(slotsWithBundles[entry.getKey()], 1, ActionType.CLICK));
                clicks.add(new InvAction(tempBundleSlot, 0, ActionType.CLICK));
            }

            for (int i = 0; i < contentItems.length; ++i) {
                final int count = contentItems[i].getCount();

                clicks.add(new InvAction(tempBundleSlot, 1, ActionType.CLICK));

                clicks.add(new InvAction(f.INPUT_START, 0, ActionType.CLICK));
                boolean didShiftCraft = false;
                for (int d : entry.getValue()) {
                    if (numEmptyMapsInGrid < count) {
                        numEmptyMapsInGrid = getEmptyMapsIntoInput(clicks, slots, f, count, numEmptyMapsInGrid, 99);
                        LAST_EMPTY_SLOT = lastEmptySlot(slots, f.HOTBAR_END, f.INV_START);
                    }

                    numEmptyMapsInGrid -= count;
                    if (didShiftCraft) {
                        if (LAST_EMPTY_SLOT >= f.HOTBAR_START) {
                            clicks.add(new InvAction(f.INPUT_START, LAST_EMPTY_SLOT - f.HOTBAR_START,
                                    ActionType.HOTBAR_SWAP));

                        } else {
                            clicks.add(new InvAction(LAST_EMPTY_SLOT, 0, ActionType.CLICK));
                            clicks.add(new InvAction(f.INPUT_START, 0, ActionType.CLICK));
                        }
                    }

                    if (LAST_EMPTY_SLOT != -1
                            && (count > 1 || d == entry.getValue().get(entry.getValue().size() - 1))) {
                        didShiftCraft = true;
                        clicks.add(new InvAction(f.RESULT, 0, ActionType.SHIFT_CLICK));
                        clicks.add(new InvAction(LAST_EMPTY_SLOT, 1, ActionType.CLICK));
                        clicks.add(new InvAction(slotsWithBundles[d], 0, ActionType.CLICK));
                    } else {
                        didShiftCraft = false;
                        clicks.add(new InvAction(f.RESULT, 0, ActionType.CLICK));
                        clicks.add(new InvAction(f.INPUT_START, 0, ActionType.CLICK));
                        clicks.add(new InvAction(f.INPUT_START, 1, ActionType.CLICK));
                        clicks.add(new InvAction(slotsWithBundles[d], 0, ActionType.CLICK));
                    }
                }

                final int fromSlot = didShiftCraft ? LAST_EMPTY_SLOT : f.INPUT_START;
                clicks.add(new InvAction(fromSlot, 0, ActionType.CLICK));
                clicks.add(new InvAction(slotsWithBundles[entry.getKey()], 0, ActionType.CLICK));
            }
        }

        if (numEmptyMapsInGrid > 0) {
            clicks.add(new InvAction(f.INPUT_START + 1, 0, ActionType.SHIFT_CLICK));
        }
    }

    @Override
    public String getInfoString() {
        if (processing && !clickQueue.isEmpty()) {
            return clickQueue.size() + " clicks";
        }

        return null;
    }
}
