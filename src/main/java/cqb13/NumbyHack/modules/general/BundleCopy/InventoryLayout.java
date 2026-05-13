package cqb13.NumbyHack.modules.general.BundleCopy;

public class InventoryLayout {
    final int resultSlot;
    final int inputStart;
    final int inventoryStart;
    final int hotbarStart;
    final int hotbarEnd;

    InventoryLayout(int resultSlot, int inputStart, int inventoryStart, int hotbarStart, int hotbarEnd) {
        this.resultSlot = resultSlot;
        this.inputStart = inputStart;
        this.inventoryStart = inventoryStart;
        this.hotbarStart = hotbarStart;
        this.hotbarEnd = hotbarEnd;
    }
}
