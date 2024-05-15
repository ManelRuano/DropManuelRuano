package com.mygdx.game;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

public class GameScreen implements Screen {
    final drop game;

    Texture background;
    Texture dropImage;
    Texture bucketImage;
    Sound dropSound;
    Sound perderSound;
    Music rainMusic;
    OrthographicCamera camera;
    Rectangle bucket;
    Array<Rectangle> raindrops;
    long lastDropTime;
    static int dropsGathered=0;

    // Variables para controlar el estado de slow motion
    private boolean isSlowMotion = false;

    // Define la velocidad normal de movimiento de las gotas y el cubo
    private static final float NORMAL_DROP_SPEED = 200; // Velocidad normal de las gotas en píxeles por segundo
    private static final float NORMAL_BUCKET_SPEED = 200; // Velocidad normal del cubo en píxeles por segundo

    // Define los factores de ralentización para el slow motion
    private static final float SLOW_MOTION_FACTOR = 0.5f; // Factor de ralentización para slow motion (0.5 significa la mitad de la velocidad normal)

    public GameScreen(final drop game) {
        this.game = game;

        // load the images for the droplet and the bucket, 64x64 pixels each
        dropImage = new Texture(Gdx.files.internal("img_2.png"));
        bucketImage = new Texture(Gdx.files.internal("img_1.png"));
        background = new Texture(Gdx.files.internal("img.png"));

        // load the drop sound effect and the rain background "music"
        dropSound = Gdx.audio.newSound(Gdx.files.internal("gota.mp3"));
        perderSound = Gdx.audio.newSound(Gdx.files.internal("derrota.mp3"));
        rainMusic = Gdx.audio.newMusic(Gdx.files.internal("lluvia.mp3"));
        rainMusic.setLooping(true);

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        // create a Rectangle to logically represent the bucket
        bucket = new Rectangle();
        bucket.x = 800 / 2 - 64 / 2; // center the bucket horizontally
        bucket.y = 20; // bottom left corner of the bucket is 20 pixels above
        // the bottom screen edge
        bucket.width = 64;
        bucket.height = 64;

        // create the raindrops array and spawn the first raindrop
        raindrops = new Array<Rectangle>();
        spawnRaindrop();

    }

    private void spawnRaindrop() {
        Rectangle raindrop = new Rectangle();
        raindrop.x = MathUtils.random(0, 800 - 64);
        raindrop.y = 480;
        raindrop.width = 64;
        raindrop.height = 64;
        raindrops.add(raindrop);
        lastDropTime = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {
        // Limpia la pantalla con un color azul oscuro
        ScreenUtils.clear(0, 0, 0.2f, 1);

        // Actualiza la cámara
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();

        game.batch.draw(background, 0, 0, 800, 480);

        game.font.draw(game.batch, "Gotas recogidas: " + dropsGathered, 660, 480);
        game.batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height);
        for (Rectangle raindrop : raindrops) {
            game.batch.draw(dropImage, raindrop.x, raindrop.y);
        }
        game.batch.end();

        // Procesa la entrada del usuario
        handleInput();

        // Comprueba si necesitamos crear una nueva gota de lluvia
        if (TimeUtils.nanoTime() - lastDropTime > 1000000000)
            spawnRaindrop();

        // Mueve las gotas de lluvia y verifica las colisiones
        float dropSpeed = isSlowMotion ? 100 : 200;
        Iterator<Rectangle> iter = raindrops.iterator();
        while (iter.hasNext()) {
            Rectangle raindrop = iter.next();
            raindrop.y -= dropSpeed * Gdx.graphics.getDeltaTime(); // Mueve la gota hacia abajo

            // Verificar si la gota está fuera de la pantalla
            if (raindrop.y + 64 < 0) {
                iter.remove();
                perderSound.play();
                game.setScreen(new GameOverScreen(game));
            }

            // Verificar si la parte inferior de la gota toca la parte superior del cubo y está horizontalmente alineada
            if (raindrop.y <= bucket.y + bucket.height && raindrop.y + raindrop.height >= bucket.y + bucket.height) {
                if (raindrop.x + raindrop.width > bucket.x && raindrop.x < bucket.x + bucket.width) {
                    dropsGathered++;
                    dropSound.play();
                    iter.remove();
                }
            }
        }
    }

    // Método para manejar la entrada del usuario, incluyendo el activar y desactivar el slow motion
    private void handleInput() {
        if (Gdx.input.isKeyPressed(Keys.SPACE)) {
            isSlowMotion = true; // Activa el slow motion cuando se pulsa la barra espaciadora
        } else {
            isSlowMotion = false; // Desactiva el slow motion cuando se suelta la barra espaciadora
        }

        // Mueve el cubo según la entrada del teclado
        if (Gdx.input.isKeyPressed(Keys.LEFT))
            bucket.x -= isSlowMotion ? 100 * Gdx.graphics.getDeltaTime() : 200 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Keys.RIGHT))
            bucket.x += isSlowMotion ? 100 * Gdx.graphics.getDeltaTime() : 200 * Gdx.graphics.getDeltaTime();

        // Asegura que el cubo se mantenga dentro de los límites de la pantalla
        if (bucket.x < 0)
            bucket.x = 0;
        if (bucket.x > 800 - 64)
            bucket.x = 800 - 64;
    }


    // Método para mover el cubo con una velocidad dada
    private void moveBucket(float speed) {
        if (Gdx.input.isKeyPressed(Keys.LEFT))
            bucket.x -= speed;
        if (Gdx.input.isKeyPressed(Keys.RIGHT))
            bucket.x += speed;

        // Asegura que el cubo se mantenga dentro de los límites de la pantalla
        if (bucket.x < 0)
            bucket.x = 0;
        if (bucket.x > 800 - 64)
            bucket.x = 800 - 64;
    }

    // Método para mover las gotas de lluvia con una velocidad dada
    private void moveRaindrops(float speed) {
        Iterator<Rectangle> iter = raindrops.iterator();
        while (iter.hasNext()) {
            Rectangle raindrop = iter.next();
            raindrop.y -= speed; // Mueve la gota hacia abajo

            // Verifica si la gota está fuera de la pantalla
            if (raindrop.y + 64 < 0) {
                iter.remove();
                perderSound.play();
                game.setScreen(new GameOverScreen(game));
            }

            // Verifica si la parte inferior de la gota toca la parte superior del cubo y está horizontalmente alineada
            if (raindrop.y <= bucket.y + bucket.height && raindrop.y + raindrop.height >= bucket.y + bucket.height) {
                if (raindrop.x + raindrop.width > bucket.x && raindrop.x < bucket.x + bucket.width) {
                    dropsGathered++;
                    dropSound.play();
                    iter.remove();
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
        // start the playback of the background music
        // when the screen is shown
        rainMusic.play();
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        dropImage.dispose();
        bucketImage.dispose();
        dropSound.dispose();
        rainMusic.dispose();
        background.dispose();
    }

}
