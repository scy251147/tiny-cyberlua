package org.tiny.cyber.lua;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.tiny.cyber.lua.domain.ConfigConstant;
import org.tiny.cyber.lua.editor.LuaEditor;
import redis.embedded.RedisServer;
import java.io.IOException;

public class MainForm extends Application {

    //redis服务器
    private static RedisServer redisServer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        startRedis();
        Parent root = FXMLLoader.load(getClass().getResource(ConfigConstant.FORM_FXML_PATH));
        primaryStage.setTitle(ConfigConstant.FORM_TITLE);
        primaryStage.getIcons().add(new Image(LuaEditor.class.getResourceAsStream(ConfigConstant.FORM_IMG_PATH)));
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(LuaEditor.class.getResource(ConfigConstant.FORM_CSS_PATH).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        //窗体关闭钩子
        primaryStage.setOnCloseRequest(event -> {
            if (redisServer != null) {
                redisServer.stop();
                System.out.println("redis server stop");
            }
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startRedis() throws IOException {
        redisServer = new RedisServer(ConfigConstant.FORM_REDIS_PORT);
        if (redisServer != null) {
            redisServer.stop();
        }
        redisServer.start();
        System.out.println("redis server start");
    }

}
