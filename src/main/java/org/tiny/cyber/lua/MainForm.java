package org.tiny.cyber.lua;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.tiny.cyber.lua.editor.LuaEditor;

public class MainForm extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/mainform.fxml"));
        primaryStage.setTitle("CyberLua v1.0 程序诗人");
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(LuaEditor.class.getResource("/css/java-keywords.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
