package org.tiny.cyber.lua.service;

import com.alibaba.fastjson.JSON;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.tiny.cyber.lua.domain.ConfigConstant;
import org.tiny.cyber.lua.editor.LuaEditor;
import redis.clients.jedis.Jedis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainFormService {

    @FXML
    private StackPane stackPane;

    @FXML
    private Button btnExec;

    @FXML
    private TextArea txtKeys;

    @FXML
    private TextArea txtArgs;

    @FXML
    private TextArea txtLogs;

    private static CodeArea codeArea;

    private static Jedis jedis;

    @FXML
    public void initialize() {
        LuaEditor luaEditor = new LuaEditor();
        codeArea = luaEditor.drawEditor(stackPane);
        //codeArea.clear();
        //codeArea.replaceText(0, 0, "local");
    }

    @FXML
    public void run() {
        txtLogs.clear();
        try {
            String logKey = "cyberlualogcontainer";
            String sourceCode = codeArea.getText();
            String logSnapCode = "local logKey = \"" + logKey + "\"\n" +
                    "redis.pcall(\"DEL\", logKey)\n" +
                    "local function log(msg)\n" +
                    "  redis.pcall(\"RPUSH\", logKey, tostring(msg))\n" +
                    "end\n";
            String compactCode = logSnapCode + sourceCode;
            //System.out.println(compactCode);
            Object result = runWithResult(compactCode);
            List<String> logs = jedis.lrange(logKey, 0, -1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(JSON.toJSONString(result));
            stringBuilder.append("\n===============日志输出===============\n");
            for (String log : logs) {
                stringBuilder.append(log + "\n");
            }
            txtLogs.setText(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            txtLogs.setText(JSON.toJSONString(e));
        }
    }

    private Object runWithResult(String code) {
        if (jedis == null) {
            jedis = new Jedis(ConfigConstant.FORM_REDIS_HOST, ConfigConstant.FORM_REDIS_PORT);
        }
        List<String> keys = handleKeys();
        List<String> args = handleArgs();
        Object result = jedis.eval(code, keys, args);
        return result;
    }

    private List<String> handleKeys() {
        List<String> keys = new ArrayList<>();
        String txtkeys = txtKeys.getText();
        if (StringUtils.isBlank(txtkeys)) {
            return keys;
        }
        keys.addAll(Arrays.asList(txtkeys.split(",")));
        return keys;
    }

    private List<String> handleArgs() {
        List<String> args = new ArrayList<>();
        String txtargs = txtArgs.getText();
        if (StringUtils.isBlank(txtargs)) {
            return args;
        }
        args.addAll(Arrays.asList(txtargs.split(",")));
        return args;
    }

}
