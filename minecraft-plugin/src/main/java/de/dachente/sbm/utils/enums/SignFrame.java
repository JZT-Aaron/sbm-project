package de.dachente.sbm.utils.enums;

import java.util.Arrays;
import java.util.List;

import org.bukkit.DyeColor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public record SignFrame(DyeColor dyeColor, boolean glowing, List<Component> lines) {
    
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static SignFrame of(DyeColor dyeColor, boolean glowing, String... mmLines) {
        List<Component> lines = Arrays.stream(mmLines).map(MM::deserialize).toList();
        return new SignFrame(dyeColor, glowing, lines);
    }
}
