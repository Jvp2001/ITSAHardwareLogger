package org.itsadigitaltrust.hardwarelogger.mvvm

import javafx.beans.property.*
import org.itsadigitaltrust.common.Operators.in
import org.itsadigitaltrust.hardwarelogger.mvvm.properties.*
import org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions.*

import java.util.Objects
import java.util.function.Supplier
import scala.collection.mutable
import scala.compiletime.uninitialized

//TODO: Add model wrapper class based of mvvmfx

/**
 * @constructor Create a new instance of [[ModelWrapper]] that wraps the instance of the Model class wrapped by the property.
 *              Updates all data when the model instance changes.
 * @param modelProperty
 * the property of the model element that will be wrapped.
 */

class ModelWrapper[M](
                       private val modelProperty: ObjectProperty[M],
                     ):
  setup()
  /** In Scala, the class's body is the constructor's body. */

  private val dirtyFlag = new ReadOnlyBooleanWrapper()
  private val diffFlag = new ReadOnlyBooleanWrapper()
  private val fields = mutable.LinkedHashSet[PropertyField[?, M, ? <: Property[?]]]()
  private val identifiedFields = mutable.LinkedHashMap[String, PropertyField[?, M, ? <: Property[?]]]()
  private val immutableFields = mutable.LinkedHashSet[ImmutablePropertyField[?, M, ? <: Property[?]]]()


  /**
   * This flag is needed to support immutable fields. Without immutables when [[# commit ( )]] is invoked,
   * the fields of the model instance are changed. With immutables, however, on commit, the instance of the model itself is
   * replaced. By default, when the model instance is changed a [[# reload ( )]] is executed.
   * This is OK when the user changes the model instance. It is not OK when we replace the model instance because of immutable fields.
   * For this reason, we need to distinguish between a change of the model instance due to a commit with immutable fields#
   * and when the user changes the model instance. Therefore, during commit this flag will switch to <code>true</code>
   * to indicate that we are currently executing a commit.
   */
  private var inCommitPhase = false

  /**
   * Create a new instance of [[ModelWrapper]] that wraps the instance of the Model class wrapped by the property.
   * Updates all data when the model instance changes.
   *
   * @param model
   * the property of the model element that will be wrapped.
   */
  def this(model: M) =
    this(SimpleObjectProperty[M](model))

  /**
   * Create a new instance of [[ModelWrapper]] that is empty at the moment. You have to define the model element
   * that should be wrapped afterwards with the [[#set( Object )]] method.
   */
  def this() =
    this(SimpleObjectProperty())


  private def setup(): Unit =
    reload()
    modelProperty.addListener: (observable, oldValue, newValue) =>
      /*
			 * Only reload the values from the new model instance when it was changed by the user and not when it was changed
			 * during the commit phase.
			 */
      if !inCommitPhase then
        reload()


  infix def set(model: M): Unit =
    this.modelProperty.set(model)

  def model_=(model: M): Unit =
    this set model

  def model: Option[M] = Option(modelProperty.get())

  def updated(model: M): Unit =
    this set model

  /**
   * Resets all defined fields to their default values.
   * <p>
   * Default values can be defined as last argument of the overloaded "field" methods
   * (see [[# field(StringGetter, StringSetter, String)]])
   * or by using the [[# useCurrentValuesAsDefaults ( )]] method.
   *
   * <p>
   *
   * If no special default value was defined for a field the default value of the actual Property type will be used
   * (e.g. 0 for [[IntegerProperty]], <code>null</code> for [[StringProperty]] and [[ObjectProperty]] ...).
   *
   *
   * <p>
   * <b>Note:</b> This method has no effects on the wrapped model element but will only change the values of the
   * defined property fields.
   */
  def reset(): Unit =
    fields.foreach(_.resetToDefault())
    immutableFields.foreach(_.resetToDefault())

    calculateDifferenceFlag()

  /**
   * Use all values that are currently present in the wrapped model object as new default values for respective fields.
   * This overrides/updates the values that were set during the initialization of the field mappings.
   * <p>
   * Subsequent calls to [[# reset ( )]] will reset the values to this new default values.
   * <p>
   * Usage example:
   *
   * <pre>
   *
   * val wrapper = new ModelWrapper[Person]()
   * wrapper.field(Person::getName, Person::setName, "oldDefault")
   *
   * val p = new Person()
   * wrapper.set(p)
   *
   *
   * p.setName("Luise")
   *
   * wrapper.useCurrentValuesAsDefaults() // now "Luise" is the default value for the name field.
   *
   *
   * name.set("Hugo")
   * wrapper.commit()
   *
   * name.get() // Hugo
   * p.getName() // Hugo
   *
   *
   * wrapper.reset() // reset to the new defaults
   * name.get() // Luise
   * wrapper.commit() // put values from properties to the wrapped model object
   * p.getName() // Luise
   *
   * </pre>
   *
   *
   * If no model instance is set to be wrapped by the ModelWrapper, nothing will happen when this method is invoked.
   * Instead, the old default values will still be available.
   *
   */

  def useCurrentValuesAsDefaults(): Unit =
    model match
      case Some(wrappedModelInstance) =>
        iterateOverAllFields(wrappedModelInstance)(_.updateDefault)
      case _ => ()


  /**
   *
   * @param wrappedModelInstance the model instance to apply each field
   * @param method               The method to invoke on each field
   */
  private def iterateOverAllFields[ModelType](wrappedModelInstance: ModelType)(method: => PropertyField[?, M, ?] => ModelType => ?): Unit =
    fields.foreach(field => method(field)(wrappedModelInstance))
    immutableFields.foreach(field => method(field)(wrappedModelInstance))
  end iterateOverAllFields


  def commit(): Unit =
    model match
      case Some(wrappedModelInstance) =>
        inCommitPhase = true
        fields.foreach(_.commit(wrappedModelInstance))


        if immutableFields.nonEmpty then
          var tmp: M = null.asInstanceOf[M]
          model.foreach: t =>
            tmp = t
            immutableFields.foreach: immutableField =>
              tmp = immutableField.commitImmutable(tmp)
        end if
        inCommitPhase = false
        dirtyFlag.set(false)

        calculateDifferenceFlag()
      case None => ()

  end commit

  /**
   * Take the current values from the wrapped model element and put them in the corresponding property fields.
   * <p>
   * If no model element is defined, then nothing will happen.
   * <p>
   * <b>Note:</b> This method has no effects on the wrapped model element but will only change the values of the
   * defined property fields.
   */
  def reload(): Unit =
    model match
      case Some(wrappedModelInstance) =>
        iterateOverAllFields(wrappedModelInstance)(_.reload)
        dirtyFlag.set(false)
        calculateDifferenceFlag()
      case None => ()
  end reload

  /**
   * This method can be used to copy all values of this [[ModelWrapper]] instance
   * to the model instance provided as argument.
   * Existing values in the provided model instance will be overwritten.
   * <p>
   * This method doesn't change the state of this modelWrapper or the wrapped model instance.
   *
   * @param model a non-null instance of a model.
   */

  def copyValuesTo(model: M): Unit =
    Objects.requireNonNull(model)
    fields.foreach(_.commit(model))


  private def propertyWasChanged(): Unit =
    dirtyFlag.set(true)
    calculateDifferenceFlag()

  private def calculateDifferenceFlag(): Unit =
    val allFields: mutable.LinkedHashSet[PropertyField[?, M, ?]] = fields
    allFields ++= immutableFields
    model match
      case Some(wrappedModelInstance) =>
        for field <- allFields do
          if field.isDifferent(wrappedModelInstance) then
            diffFlag.set(true)
            return
      case None => ()
  end calculateDifferenceFlag


  private def add[T, R <: Property[T]](field: PropertyField[T, M, R]): R =
    fields.add(field)
    model.foreach: value =>
      field.reload(value)
    field.property

  private def addIdentified[T, R <: Property[T]](fieldName: String, propertyField: PropertyField[T, M, R]): R =
    if fieldName in identifiedFields then
      val property: Property[R] = identifiedFields(fieldName).property.asInstanceOf[Property[R]]
      property.getValue
    else
      identifiedFields(fieldName) = propertyField
      add(propertyField)

  private def addImmutable[T, R <: Property[T]](field: ImmutablePropertyField[T, M, R]): R =
    immutableFields += field

    model match
      case Some(value) =>
        field.reload(value)
      case None => ()

    field.property

  private def addIdentifedImmutable[T, R <: Property[T]](fieldName: String, propertyField: ImmutablePropertyField[T, M, R]): R =
    if fieldName in identifiedFields then
      val property: Property[R] = identifiedFields(fieldName).property.asInstanceOf[Property[R]]
      property.getValue
    else
      identifiedFields(fieldName) = propertyField
      addImmutable(propertyField)


  def differentProperty: ReadOnlyBooleanProperty =
    diffFlag.getReadOnlyProperty

  /**
   * @see [[differentProperty()]].
   */
  def isDifferent = diffFlag.get()


  /**
   * This boolean flag indicates whether there was a change to at least one wrapped property.
   * <p>
   * Note the difference to [[differentProperty]]: This property will turn to <code>true</code> when the value
   * of one of the wrapped properties is changed. It will only change back to <code>false</code> when either the
   * `#commit` or [[reload]] method is called. This property will stay <code>true</code> even if
   * afterwards another change is done so that the data is equal again.
   * In this case the [[differentProperty]]
   * will switch back to <code>false</code>.
   * <p/>
   *
   * Simply speaking: This property indicates whether there was a change done to the wrapped properties or not. The
   *
   * @see [[differentProperty]] indicates whether there is a difference in data at the moment.
   * @return a read-only boolean property indicating if there was a change done.
   */
  def dirtyProperty: ReadOnlyBooleanProperty =
    dirtyFlag.getReadOnlyProperty

  /**
   * @see [[dirtyProperty]]
   */
  def isDirty: Boolean =
    dirtyFlag.get()


//  def field[T, R <: Property[T], PBase <: Property[T]](getter: Getter[PBase, M], setter: MutableSetter[M, T], defaultValue: T)(ctor: => () => PBase): PBase =
//    add(new BeanPropertyField(propertyWasChanged, getter, setter, defaultValue, () => ctor()))
//
//  def immutableField[T, R <: Property[T], PBase <: Property[T]](getter: Getter[PBase, M], setter: ImmutableSetter[M, T], defaultValue: T)(ctor: => () => PBase): PBase =
//    add(new ImmutableBeanPropertyField(propertyWasChanged, getter, setter, defaultValue: T, () => ctor()))

  def field[T, R <: Property[T], PBase <: Property[T]](identifier: String, getter: Getter[PBase, M], setter: MutableSetter[M, T], defaultValue: T = null.asInstanceOf[T])(ctor: => () => PBase): PBase =
    addIdentified(identifier, new BeanPropertyField(propertyWasChanged, getter, setter, defaultValue, () => ctor()))

  def immutableField[T, R <: Property[T], PBase <: Property[T]](identifier: String, getter: Getter[PBase, M],
                                                                setter: ImmutableSetter[M, T], defaultValue: T = null.asInstanceOf[T])
                                                               (ctor: => () => PBase): PBase =
    addIdentifedImmutable(identifier, new ImmutableBeanPropertyField(propertyWasChanged, getter, setter, defaultValue, () => ctor()))

end ModelWrapper
