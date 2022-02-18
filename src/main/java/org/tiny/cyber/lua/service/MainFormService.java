package org.tiny.cyber.lua.service;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.tiny.cyber.lua.editor.LuaEditor;

public class MainFormService {

    @FXML
    private StackPane stackPane;

    @FXML
    public void initialize() {
        LuaEditor luaEditor = new LuaEditor();
        luaEditor.drawEditor(stackPane);
    }

}
