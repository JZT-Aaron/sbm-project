package de.dachente.sbm.utils.enums;

public enum Status {
    DEAD("☠"),
    WATCHING("🙌"),
    PLAYING("🗡"),
    WAITING("⏳"),
    PARKOURING("👟"),
    INLOBBY("🚩");

    private final String symbol; // String sstatt Character

    Status(String symbol) {
        this.symbol = symbol;
    }   

    public String getSymbol() {
        return symbol;
    }
}
