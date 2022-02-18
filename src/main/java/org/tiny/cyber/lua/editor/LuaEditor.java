package org.tiny.cyber.lua.editor;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.collection.ListModification;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaEditor {

    private static final String[] KEYWORDS = new String[]{
            "local", "and", "or", "not", "function", "table", "nil"
            , "for", "while", "do", "break", "in", "return", "until"
            , "goto", "repeat", "true", "false", "if", "then", "else", "elseif"
            , "end", "redis", "call", "log"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
//    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
//            + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)
    private static final String COMMENT_PATTERN = "--[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
            + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private static final String sampleCode = String.join("\n", new String[] {
                   " -- 库存key，存放库存数量\n" +
                    " local unsentNumKey  = KEYS[1]\n" +
                    " -- 幂等锁，用于防止重复追加\n" +
                    " local idempotentKey = ARGV[1]\n" +
                    " -- 幂等锁的直\n" +
                    " local idempotentVal = ARGV[2]\n" +
                    " -- 共享锁，用于防止多个请求操作\n" +
                    " -- local unsentLockKey = ARGV[3]\n" +
                    " -- 本次追加的数量\n" +
                    " local appendCount   = tonumber(ARGV[4])\n" +
                    " -- 操作结果\n" +
                    " local result = \"\"\n" +
                    "  -- 新增幂等锁，防止重复追加\n" +
                    " local exist =  redis.call('SETNX', idempotentKey, idempotentVal)\n" +
                    " -- 幂等key存在，证明是重复发，删掉锁\n" +
                    " if(exist ~= false) then\n" +
                    "     -- 删除共享锁\n" +
                    "     -- redis.call('DEL', unsentLockKey)\n" +
                    "     -- 幂等存在\n" +
                    "     result = \"500\"\n" +
                    " -- 幂等key不存在，证明是追加\n" +
                    " else\n" +
                    "     -- 获取当前库存\n" +
                    "     local curNum = tonumber(redis.call('GET', unsentNumKey))\n" +
                    "     -- 当前库存为正数\n" +
                    "     if curNum ~= nil and curNum >= 0 then\n" +
                    "         -- 递增库存\n" +
                    "         redis.call('INCRBY', unsentNumKey, appendCount)\n" +
                    "      -- 当前库存为负数\n" +
                    "     else\n" +
                    "         -- 重设库存\n" +
                    "         redis.call('SET', unsentNumKey, appendCount)\n" +
                    "     end\n" +
                    "     result = \"200\"\n" +
                    " end\n" +
                    "\n" +
                    " -- 返回\n" +
                    " return result"
    });


    public CodeArea drawEditor(StackPane stackPane) {
        CodeArea codeArea = new CodeArea();

        // add line numbers to the left of area
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setContextMenu(new DefaultContextMenu());

        // recompute syntax highlighting only for visible paragraph changes
        // Note that this shows how it can be done but is not recommended for production where multi-
        // line syntax requirements are needed, like comment blocks without a leading * on each line.
        codeArea.getVisibleParagraphs().addModificationObserver
                (
                        new VisibleParagraphStyler<>(codeArea, this::computeHighlighting)
                );

        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile("^\\s+");
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, KE ->
        {
            if (KE.getCode() == KeyCode.ENTER) {
                int caretPosition = codeArea.getCaretPosition();
                int currentParagraph = codeArea.getCurrentParagraph();
                Matcher m0 = whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1).getSegments().get(0));
                if (m0.find()) Platform.runLater(() -> codeArea.insertText(caretPosition, m0.group()));
            }
        });

        codeArea.replaceText(0, 0, sampleCode);

        stackPane.getChildren().addAll(new VirtualizedScrollPane<>(codeArea));

        return codeArea;

        //StackPane stackPane = new StackPane(new VirtualizedScrollPane<>(codeArea));
        //Scene scene = new Scene(stackPane, 800, 600);
        //scene.getStylesheets().add(LuaEditor.class.getResource("/css/java-keywords.css").toExternalForm());
        //primaryStage.setScene(scene);
        //primaryStage.setTitle("CyberLua v1.0 by 程序诗人");
        //primaryStage.show();
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>> {
        private final GenericStyledArea<PS, SEG, S> area;
        private final Function<String, StyleSpans<S>> computeStyles;
        private int prevParagraph, prevTextLength;

        public VisibleParagraphStyler(GenericStyledArea<PS, SEG, S> area, Function<String, StyleSpans<S>> computeStyles) {
            this.computeStyles = computeStyles;
            this.area = area;
        }

        @Override
        public void accept(ListModification<? extends Paragraph<PS, SEG, S>> lm) {
            if (lm.getAddedSize() > 0) {
                int paragraph = Math.min(area.firstVisibleParToAllParIndex() + lm.getFrom(), area.getParagraphs().size() - 1);
                String text = area.getText(paragraph, 0, paragraph, area.getParagraphLength(paragraph));

                if (paragraph != prevParagraph || text.length() != prevTextLength) {
                    int startPos = area.getAbsolutePosition(paragraph, 0);
                    Platform.runLater(() -> area.setStyleSpans(startPos, computeStyles.apply(text)));
                    prevTextLength = text.length();
                    prevParagraph = paragraph;
                }
            }
        }
    }

    private class DefaultContextMenu extends ContextMenu
    {
        private MenuItem fold, unfold, print;

        public DefaultContextMenu()
        {
            fold = new MenuItem( "折叠选中" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "展开选中" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

            //print = new MenuItem( "Print" );
            //print.setOnAction( AE -> { hide(); print(); } );

            //getItems().addAll( fold, unfold, print );
            getItems().addAll( fold, unfold );
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        private void fold() {
            ((CodeArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        private void unfold() {
            CodeArea area = (CodeArea) getOwnerNode();
            area.unfoldParagraphs( area.getCurrentParagraph() );
        }

        private void print() {
            System.out.println( ((CodeArea) getOwnerNode()).getText() );
        }
    }
}
