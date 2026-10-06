package org.itsadigitaltrust.hardwarelogger.contextbasedemnubar

import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.MenuContributor
import scalafx.beans.property.ObjectProperty

/**
 * A central, shared state service that acts as the single source of truth 
 * for the active menu context in the application.
 *
 * This service contains zero UI controls or scene imports, ensuring 
 * full compliance with the MVVM architectural pattern.
 */
class MenuManagerService:

  /**
   * The central reactive state engine tracking which tab or view context 
   * is active right now.
   *
   * The custom MenuBar binds to this property to automatically re-render 
   * its actions whenever it switches.
   */
  val activeContributor = new ObjectProperty[Option[MenuContributor]](this, "activeContributor", None)

  /**
   * Updates the global menu context. ViewModels call this when their tab 
   * gains focus or is highlighted by the user selection models.
   *
   * @param contributor An optional reference to the current active MenuContributor
   */
  def setCurrentContext(contributor: Option[MenuContributor]): Unit =
    activeContributor.value = contributor
    
  def currentContext_=(contributor: Option[MenuContributor]): Unit =
    activeContributor.value = contributor
    
  def currentContext: Option[MenuContributor] = activeContributor.value

end MenuManagerService
