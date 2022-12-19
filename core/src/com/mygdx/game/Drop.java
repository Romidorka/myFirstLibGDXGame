package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class Drop {
    Texture img;
    Rectangle rect;
    boolean is_enemy;

    private final double acceleration = 6;
    private double speed_y = 0;

    Drop(boolean is_enemy){
        this.is_enemy = is_enemy;

        String[] friend_skins = {"arch.png", "debian.png", "fedora.png", "mint.png"};
        String[] enemy_skins = {"microsoft.png", "win7.png"};
        Pixmap pixmap;

        if (is_enemy) {
            pixmap = new Pixmap(Gdx.files.internal("win/" + enemy_skins[MathUtils.random(0, enemy_skins.length - 1)]));
        }else {
            pixmap = new Pixmap(Gdx.files.internal("linux/" + friend_skins[MathUtils.random(0, friend_skins.length - 1)]));
        }
        Pixmap pixmap_scaled = new Pixmap(64, 64, pixmap.getFormat());
        pixmap_scaled.drawPixmap(pixmap,
                0, 0, pixmap.getWidth(), pixmap.getHeight(),
                0, 0, pixmap_scaled.getWidth(), pixmap_scaled.getHeight()
        );
        img = new Texture(pixmap_scaled);
        rect = new Rectangle();
        rect.width = 64;
        rect.height = 64;
        rect.x = MathUtils.random(0, 800-rect.width);
        rect.y = 480;
    }

    public void calc_physics(){
        speed_y += acceleration;
        rect.y -= speed_y * Gdx.graphics.getDeltaTime();
    }

}
