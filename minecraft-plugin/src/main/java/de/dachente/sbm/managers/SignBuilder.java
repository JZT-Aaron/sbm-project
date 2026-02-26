package de.dachente.sbm.managers;

import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

import de.dachente.sbm.utils.enums.SignFrame;

public class SignBuilder {


    public static void loadFrameToSign(Sign sign, Side sideD, SignFrame signFrame) {
        SignSide side = sign.getSide(sideD);
        if(side.lines().isEmpty()) return;
        for(int i = 0; i < 4; i++) {
            side.line(i, signFrame.lines().get(i));
        }
        side.setColor(signFrame.dyeColor());
        side.setGlowingText(signFrame.glowing());
        
        sign.update(true, false);
    }
}