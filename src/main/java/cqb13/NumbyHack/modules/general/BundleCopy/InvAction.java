package cqb13.NumbyHack.modules.general.BundleCopy;

public class InvAction {
    public enum ActionType {
        CLICK, SHIFT_CLICK, HOTBAR_SWAP, BUNDLE_SELECT
    }

    final int slot;
    final int button;
    final ActionType type;

    InvAction(int slot, int button, ActionType type) {
        this.slot = slot;
        this.button = button;
        this.type = type;
    }
}
