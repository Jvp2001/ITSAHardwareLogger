package scalafx.application

import javafx.application.Application as JFXApplication
object JFXApp3AccessHook: 
  // I need define this so I can access this property from JFXApp3, as it is only accessible inside the scalafx.application I have to do this.
  def getActiveJFXApp: JFXApplication = JFXApp3.ActiveJFXApp3

