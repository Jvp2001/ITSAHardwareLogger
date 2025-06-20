/**
 * Copyright (C) 2015 uphy.jp
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.uphy.javafx.console;

import com.sun.javafx.PlatformUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * @author Yuhi Ishikura
 */
public class ConsoleView extends BorderPane
{

    private final PrintStream out;
    private final TextArea textArea;
    private final InputStream in;
    private final ContextMenu menu = new ContextMenu();
    public ConsoleView()
    {
        this(Charset.defaultCharset());
    }

    public ConsoleView(Charset charset)
    {
        getStyleClass().add("console");
        this.textArea = new TextArea();
        this.textArea.setWrapText(true);
        this.textArea.setEditable(false);
        this.textArea.setCursor(Cursor.DEFAULT);
        setCenter(this.textArea);

        final TextInputControlStream stream = new TextInputControlStream(this.textArea, Charset.defaultCharset());
        this.out = new PrintStream(stream.getOut(), true, charset);
        this.in = stream.getIn();

        menu.getItems().add(createItem("Clear console", e ->
        {
            try
            {
                stream.clear();
                this.textArea.clear();
            } catch (IOException e1)
            {
                throw new RuntimeException(e1);
            }
        }));
        this.textArea.setContextMenu(menu);

        setPrefWidth(600);
        setPrefHeight(800);
        menu.getItems().add(createItem("Copy", e ->
        {
            var content = new ClipboardContent();
            content.putString(textArea.getText());
            Clipboard.getSystemClipboard().setContent(content);

        }));


        menu.getItems().add(createItem("Save", e ->
        {
            var fileChooser = new FileChooser();
            fileChooser.setTitle("Save Console Output");
            fileChooser.setInitialFileName("Console Output.txt");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            var file = fileChooser.showSaveDialog(getParent().getScene().getWindow());
            if (file != null)
            {
                // Open file's folder in file explorer or finder
                try
                {
                    Files.writeString(Path.of(file.getAbsolutePath()), textArea.getText(), Charset.defaultCharset());
                    if (PlatformUtil.isWindows())
                    {

                        Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select", file.getParentFile().getAbsolutePath()});
                    } else
                    {
                        Runtime.getRuntime().exec(new String[]{"open", "-R", file.getAbsolutePath()});
                    }
                } catch (IOException ex)
                {
                    throw new RuntimeException(ex);
                }
            }

        }));
    }

    
    public final String getOutput()
    {
        return textArea.getText();
    }    

    protected MenuItem createAndAddItem(String name, EventHandler<ActionEvent> a)
    {
        MenuItem item = createItem(name, a);
        menu.getItems().add(item);
        return item;
    }

    private MenuItem createItem(String name, EventHandler<ActionEvent> a)
    {
        final MenuItem menuItem = new MenuItem(name);
        menuItem.setOnAction(a);
        return menuItem;
    }

    public PrintStream getOut()
    {
        return out;
    }

    public InputStream getIn()
    {
        return in;
    }

}
