//package org.itsadigitaltrust.hardwarelogger.ui;
//
//
//import javafx.geometry.Pos;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//import org.controlsfx.control.action.Action;
//
//public final class ToastHelper
//{
//    private static boolean canShow = true;
//    public static void makeText(Stage ownerStage, String toastMsg, int toastDelay) {
//        if (!canShow) return;
//        canShow = false;
//        final var notification = org.controlsfx.control.Notifications.create()
//                .text(toastMsg)
//                .position(Pos.CENTER)
//                .action(new Action(e -> canShow = true))
//                .hideAfter(new Duration(toastDelay))
//                .owner(ownerStage)
//                .title("Save Successful");
//
//        notification.showInformation();
//    }
//
//}