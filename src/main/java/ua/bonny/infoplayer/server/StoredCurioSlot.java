package ua.bonny.infoplayer.server;

final class StoredCurioSlot {
    String identifier;
    int index;
    boolean cosmetic;
    String item;

    StoredCurioSlot() {
    }

    StoredCurioSlot(String identifier, int index, boolean cosmetic, String item) {
        this.identifier = identifier;
        this.index = index;
        this.cosmetic = cosmetic;
        this.item = item;
    }
}
