package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

public class MyGdxGame extends ApplicationAdapter {
	private OrthographicCamera camera;
	SpriteBatch batch;
	Texture img;
	Sound lost_sound;
	Sound save_sound;
	Sound levelup_sound;

	private Rectangle tux;
	private int lives=5;
	private double tux_speed_x;
	private double tux_speed_y;
	private long lastEnemyDropTime;
	private long lastFriendDropTime;
	private Vector3 touchPos;
	private Array<Drop> drops;
	private int difficulty=1;
	private final String[] tux_skins = {"tux_noob.png", "tux_normal.png", "tux_master.png", "tux_pro.png"};
	private Array<Texture> tux_textures;
	private BitmapFont font;


	@Override
	public void create () {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);
		batch = new SpriteBatch();
		lost_sound = Gdx.audio.newSound(Gdx.files.internal("lose.wav"));
		save_sound = Gdx.audio.newSound(Gdx.files.internal("save.wav"));
		levelup_sound = Gdx.audio.newSound(Gdx.files.internal("levelup.wav"));

		tux_textures = new Array<Texture>();
		for (int i=0;i<tux_skins.length; i++) {
			Pixmap skin_pixmap = new Pixmap(Gdx.files.internal("tux/" + tux_skins[i]));
			Pixmap skin_pixmap_scaled = new Pixmap(100, 100, skin_pixmap.getFormat());
			skin_pixmap_scaled.drawPixmap(skin_pixmap,
					0, 0, skin_pixmap.getWidth(), skin_pixmap.getHeight(),
					0, 0, skin_pixmap_scaled.getWidth(), skin_pixmap_scaled.getHeight()
			);
			tux_textures.add(new Texture(skin_pixmap_scaled));
		}
		img = tux_textures.get(0);

		tux = new Rectangle();
		tux.x = 800/2 - 100/2;
		tux.y = 20;
		tux.width = 64;
		tux.height = 64;

		touchPos = new Vector3();
		drops = new Array<Drop>();
//		font = new BitmapFont();
		font = new BitmapFont(Gdx.files.internal("font.fnt"),false);

	}

	@Override
	public void render () {
		ScreenUtils.clear(0, 0.5f, 0.9f, 1);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(img, tux.x, tux.y);
		for(Drop drop: drops) {
			batch.draw(drop.img, drop.rect.x, drop.rect.y);
		}
		font.draw(batch, "Lives: " + lives, 10, 460);
		font.draw(batch, "Saved: " + difficulty, 10, 420);
		if (lives<=0) font.draw(batch, "YOU LOSE", 320, 280);
		batch.end();

		if (tux.x<0 || tux.x>800-100){
			tux.x = Math.min(Math.max(1, tux.x), 799-100);
			tux_speed_x=-tux_speed_x * .75f;
		}else {
			for(int i=0;i<20;i++) {
				if (Gdx.input.isTouched(i)) {
					touchPos.set(Gdx.input.getX(i), Gdx.input.getY(i), 0);
					camera.unproject(touchPos);
					if (touchPos.y <= 180) {
						if (touchPos.x < 400)
							tux_speed_x -= Math.abs((touchPos.x - 400)) / 2 / 400 * 24 * Math.sqrt(Gdx.graphics.getDeltaTime());
						else
							tux_speed_x += Math.abs((touchPos.x - 400)) / 2 / 400 * 24 * Math.sqrt(Gdx.graphics.getDeltaTime());
					}
				}
			}

			if (Gdx.input.isKeyPressed(Input.Keys.A))
				tux_speed_x -= 10 * Math.sqrt(Gdx.graphics.getDeltaTime());
			if (Gdx.input.isKeyPressed(Input.Keys.D))
				tux_speed_x += 10 * Math.sqrt(Gdx.graphics.getDeltaTime());
		}
		tux.x += tux_speed_x;
		tux_speed_x = tux_speed_x - tux_speed_x / 20;

		if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && tux_speed_y==0)
			tux_speed_y += 140 * Math.sqrt(Gdx.graphics.getDeltaTime());

		for(int i=0;i<20;i++) {
			if (Gdx.input.isTouched(i)) {
				touchPos.set(Gdx.input.getX(i), Gdx.input.getY(i), 0);
				camera.unproject(touchPos);
				if (touchPos.y > 180 && tux_speed_y == 0) {
					tux_speed_y += 140 * Math.sqrt(Gdx.graphics.getDeltaTime());
				}
			}
		}

		tux.y += tux_speed_y;
		tux_speed_y -= 1;
		if(tux.y<=20)tux_speed_y=0;

		if(TimeUtils.nanoTime()/1000 - lastEnemyDropTime > Math.max(10_000_000 - (1_000 * difficulty), 1_000_000) && lives>0){
			spawnDrop(true);
		}
		if(TimeUtils.nanoTime()/1000 - lastFriendDropTime > Math.max(1_000_000 - (10_000 * difficulty), 500_000) && lives>0){
			spawnDrop(false);
		}

		for (Iterator<Drop> iter = drops.iterator(); iter.hasNext(); ) {
			Drop drop = iter.next();
			drop.calc_physics();
			if(drop.rect.overlaps(tux)){
				if(drop.is_enemy){
					lost_sound.play(0.5f);
					lives-=1;
				} else{
					difficulty+=1;
					save_sound.play(0.5f);
				}
				drop.img.dispose();
				iter.remove();
				continue;
			}

			if(drop.rect.y < 0 - drop.rect.height*2){
				if(!drop.is_enemy){
					lost_sound.play(0.5f);
					lives-=1;
				}
				drop.img.dispose();
				iter.remove();
			}

		}
		changeSkin();
		checkDefeat();
	}

	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();

		for(Drop drop: drops){
			drop.img.dispose();
		}
	}

	public void spawnDrop(boolean is_enemy){
		if (is_enemy) {
			for (int i = 0; i <= difficulty / 20; i++) {
				Drop drop = new Drop(true);
				drops.add(drop);
			}
			lastEnemyDropTime = TimeUtils.nanoTime()/1000;
		}else{
			Drop drop = new Drop(false);
			drops.add(drop);
			lastFriendDropTime = TimeUtils.nanoTime()/1000;
		}
	}

	public void changeSkin(){
		if(difficulty>10 && difficulty<=20 && !img.equals(tux_textures.get(1))){
			img = tux_textures.get(1);
			levelup_sound.play();
		}else if(difficulty>20 && difficulty<=30 && !img.equals(tux_textures.get(2))){
			img = tux_textures.get(2);
			levelup_sound.play();
		}else if(difficulty>30 && !img.equals(tux_textures.get(3))){
			img = tux_textures.get(3);
			levelup_sound.play();
		}
	}

	public void checkDefeat(){
		if (lives<=0){
			for (int i=0;i<drops.size;i++) {
				Drop drop = drops.get(0);
				drop.img.dispose();
				drops.removeIndex(0);
			}
		}
	}
}
