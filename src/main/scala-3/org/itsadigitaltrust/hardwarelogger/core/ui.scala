package org.itsadigitaltrust.hardwarelogger.core

import javafx.scene.control as jfxsc
import org.scalafx.extras


object ui:
  
  export scalafx.Includes.*
  export scalafx.stage.Window
  export scalafx.scene.{Node, Group, Cursor}
  export scalafx.scene.control.{Tab, TableColumn, TableView, TableRow, TabPane, Button, Label, TextField, Alert, ButtonType, MenuBar, Menu, MenuItem, ProgressBar, SeparatorMenuItem, Separator, CheckMenuItem}
  export Alert.AlertType
  export scalafx.scene.layout.{VBox, HBox, BorderPane, Priority, Region, GridPane, StackPane}
  export scalafx.scene.layout.Priority.*
  export scalafx.scene.text.TextAlignment
  export scalafx.scene.text.Font

  export scalafx.scene.input.{KeyCode, KeyEvent, MouseEvent, MouseButton, KeyCodeCombination, KeyCombination}
  export scalafx.event.EventType
  export scalafx.collections.ObservableBuffer

  export scalafx.beans.property.{StringProperty, BooleanProperty, IntegerProperty, DoubleProperty, ReadOnlyDoubleProperty, ReadOnlyBooleanProperty, ReadOnlyIntegerProperty, ReadOnlyObjectProperty}
  export scalafx.geometry.Insets
  export scalafx.geometry.Pos
  export Pos.{Center, CenterLeft, TopLeft, TopCenter, TopRight, CenterRight}

  export scalafx.event.subscriptions.Subscription

//  export org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
  
  export org.itsadigitaltrust.hardwarelogger.core.UIAssetLoader.{image, stylesheet, asset}
  
  export org.itsadigitaltrust.hardwarelogger.ui.ui.*

end ui

