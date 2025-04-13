package org.itsadigitaltrust.hardwarelogger.mvvm

import scalafx.beans.property.*
import scalafx.Includes.*
import org.itsadigitaltrust.common.Operators.in
import org.itsadigitaltrust.hardwarelogger.mvvm.properties.*
import org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions.*

import java.util.Objects
import java.util.function.Supplier
import scala.collection.mutable
import scala.compiletime.uninitialized


/**
 * @constructor Create a new instance of [[ModelWrapper]] that wraps the instance of the Model class wrapped by the property.
 *              Updates all data when the model instance changes.
 * @param modelProperty
 * the property of the model element that will be wrapped.
 */

class ModelWrapper[M](
                       private val modelProperty: ObjectProperty[M],
                     ):

  /** In Scala, the class's body is the constructor's body. */

  private val dirtyFlag = new ReadOnlyBooleanWrapper()
  private val diffFlag = new ReadOnlyBooleanWrapper()
  private var fields = new mutable.LinkedHashSet[PropertyField[?, ?, M, ? <: Property[?, ?]]]()
  private var identifiedFields = new mutable.LinkedHashMap[String, PropertyField[?, ?,  M, ? <: Property[?, ?]]]()
  private var immutableFields = new mutable.LinkedHashSet[ImmutablePropertyField[?, ?, M, ? <: Property[?, ?]]]()

  setup()

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
    this(ObjectProperty[M](model))


  private def setup(): Unit =
    fields = new mutable.LinkedHashSet[PropertyField[?, ?, M, ? <: Property[?, ?]]]()
    identifiedFields = new mutable.LinkedHashMap[String, PropertyField[?, ?, M, ? <: Property[?, ?]]]()
    immutableFields = new mutable.LinkedHashSet[ImmutablePropertyField[?, ?, M, ? <: Property[?, ?]]]()
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
  private def iterateOverAllFields[ModelType](wrappedModelInstance: ModelType)(method: => PropertyField[?, ?, M, ?] => ModelType => ?): Unit =
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


  def propertyWasChanged(): Unit =
    dirtyFlag.set(true)
    calculateDifferenceFlag()

  private def calculateDifferenceFlag(): Unit =
    val allFields: mutable.LinkedHashSet[PropertyField[?, ?, M, ?]] = fields
    allFields ++= immutableFields
    model match
      case Some(wrappedModelInstance) =>
        for field <- allFields do
          if field.isDifferent(wrappedModelInstance) then
            diffFlag.set(true)
      case None => ()
  end calculateDifferenceFlag


  def add[T, J, R <: Property[T, J]](field: PropertyField[T, J, M, R]): R =
    fields.add(field)
    model.foreach: value =>
      field.reload(value)
    field.targetProperty
  end add


  def addIdentified[T, J, R <: Property[T, J]](fieldName: String, propertyField: PropertyField[T, J, M, R]): R =
    if fieldName in identifiedFields then
      val property: Property[T, J] = identifiedFields(fieldName).targetProperty.asInstanceOf[Property[T,J]]
      property.asInstanceOf[R]
    else
      identifiedFields(fieldName) = propertyField
      add(propertyField)
  end addIdentified


  def addImmutable[T, J, R <: Property[T, J]](field: ImmutablePropertyField[T, J, M, R]): R =
    immutableFields += field

    model match
      case Some(value) =>
        field.reload(value)
      case None => ()

    field.targetProperty
  end addImmutable


  def addIdentifiedImmutable[T, J, R <: Property[T, J]](fieldName: String, propertyField: ImmutablePropertyField[T, J, M, R]): R =
    if fieldName in identifiedFields then
      val property: Property[R, R] = identifiedFields(fieldName).targetProperty.asInstanceOf[Property[R, R]]
      return property.getValue

    identifiedFields(fieldName) = propertyField
    addImmutable(propertyField)
  end addIdentifiedImmutable



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

//  def field[T, J, R <: Property[T, J], PS <: R](identifier: String, getter: Getter[T, M], setter: MutableSetter[M, J], defaultValue: J = null.asInstanceOf[J])( propertySupplier: T => PS): PS =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, propertySupplier))
  def field[T, J, R <: Property[T, J], PS <: R](identifier: String, getter: Getter[T, M], defaultValue: J)( propertySupplier: T => PS): PS =
    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, (m,j: J) => (), defaultValue, propertySupplier))
  def field[T, R <: Property[T, Number], PS <: R](identifier: String, getter: Getter[T, M], defaultValue: Number)( propertySupplier: T => PS): PS =
    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, (m,j: Number) => (), defaultValue, propertySupplier))

  def immutableField[T, J, R <: Property[T, J], PS <: R](identifier: String, getter: Getter[T, M], setter: ImmutableSetter[M, J], defaultValue: J = null.asInstanceOf[J])( propertySupplier: T => PS): PS =
    addIdentified(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, propertySupplier))
//
//  def field(identifier: String, accessor: StringPropertyAccessor[M]): StringProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, "", () => new StringProperty(null, identifier)))
//
//  def field(identifier: String, accessor: StringPropertyAccessor[M], defaultValue: String): StringProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new StringProperty(null, identifier)))

//  def field(getter: BooleanGetter[M], setter: BooleanSetter[M]): BooleanProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, false, () => new BooleanProperty()))
//
//  def field(getter: BooleanGetter[M], setter: BooleanSetter[M], defaultValue: Boolean): BooleanProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new BooleanProperty()))
//
//  def immutableField(getter: BooleanGetter[M], immutableSetter: BooleanImmutableSetter[M], defaultValue: Boolean): BooleanProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new BooleanProperty()))
//
//  def field(accessor: BooleanPropertyAccessor[M]): BooleanProperty =
//    add(new FxPropertyField(this.propertyWasChanged(), accessor, false, () => new BooleanProperty()))
//
//  def field(accessor: BooleanPropertyAccessor[M], defaultValue: Boolean): BooleanProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new BooleanProperty()))
//
//  def field(identifier: String, getter: BooleanGetter[M], setter: BooleanSetter[M]): BooleanProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new BooleanProperty(null, identifier)))
//
//  def field(identifier: String, getter: BooleanGetter[M], setter: BooleanSetter[M], defaultValue: Boolean): BooleanProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new BooleanProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: BooleanGetter[M], immutableSetter: BooleanImmutableSetter[M]): BooleanProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new BooleanProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: BooleanGetter[M], immutableSetter: BooleanImmutableSetter[M], defaultValue: Boolean): BooleanProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new BooleanProperty(null, identifier)))
//
//  def field(identifier: String, accessor: BooleanPropertyAccessor[M]): BooleanProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged(), accessor, () => new BooleanProperty(null, identifier)))
//
//  def field(identifier: String, accessor: BooleanPropertyAccessor[M], defaultValue: Boolean): BooleanProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new BooleanProperty(null, identifier)))
//
//  def field(getter: DoubleGetter[M], setter: DoubleSetter[M]): DoubleProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, 0, () => new DoubleProperty()))
//
//  def immutableField(getter: DoubleGetter[M], immutableSetter: DoubleImmutableSetter[M]): DoubleProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new DoubleProperty()))
//
//  def field(getter: DoubleGetter[M], setter: DoubleSetter[M], defaultValue: Double): DoubleProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new DoubleProperty()))
//
//  def immutableField(getter: DoubleGetter[M], immutableSetter: DoubleImmutableSetter[M], defaultValue: Double): DoubleProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new DoubleProperty()))
//
//  def field(accessor: DoublePropertyAccessor[M]): DoubleProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, () => new DoubleProperty()))
//
//  def field(accessor: DoublePropertyAccessor[M], defaultValue: Double): DoubleProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new DoubleProperty()))
//
//  def field(identifier: String, getter: DoubleGetter[M], setter: DoubleSetter[M]): DoubleProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new DoubleProperty(null, identifier)))
//
//  def field(identifier: String, getter: DoubleGetter[M], setter: DoubleSetter[M], defaultValue: Double): DoubleProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new DoubleProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: DoubleGetter[M], immutableSetter: DoubleImmutableSetter[M]): DoubleProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new DoubleProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: DoubleGetter[M], immutableSetter: DoubleImmutableSetter[M], defaultValue: Double): DoubleProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new DoubleProperty(null, identifier)))
//
//  def field(identifier: String, accessor: DoublePropertyAccessor[M]): DoubleProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, () => new DoubleProperty(null, identifier)))
//
//  def field(identifier: String, accessor: DoublePropertyAccessor[M], defaultValue: Double): DoubleProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new DoubleProperty(null, identifier)))
//
//  def field(getter: FloatGetter[M], setter: FloatSetter[M]): FloatProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new FloatProperty()))
//
//  def immutableField(getter: FloatGetter[M], immutableSetter: FloatImmutableSetter[M]): FloatProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new FloatProperty()))
//
//  def field(getter: FloatGetter[M], setter: FloatSetter[M], defaultValue: Float): FloatProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new FloatProperty()))
//
//  def immutableField(getter: FloatGetter[M], immutableSetter: FloatImmutableSetter[M], defaultValue: Float): FloatProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new FloatProperty()))
//
//  def field(accessor: FloatPropertyAccessor[M]): FloatProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, () => new FloatProperty()))
//
//  def field(accessor: FloatPropertyAccessor[M], defaultValue: Float): FloatProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new FloatProperty()))
//
//  def field(identifier: String, getter: FloatGetter[M], setter: FloatSetter[M]): FloatProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new FloatProperty(null, identifier)))
//
//  def field(identifier: String, getter: FloatGetter[M], setter: FloatSetter[M], defaultValue: Float): FloatProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new FloatProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: FloatGetter[M], immutableSetter: FloatImmutableSetter[M]): FloatProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new FloatProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: FloatGetter[M], immutableSetter: FloatImmutableSetter[M], defaultValue: Float): FloatProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new FloatProperty(null, identifier)))
//
//  def field(identifier: String, accessor: FloatPropertyAccessor[M]): FloatProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, () => new FloatProperty(null, identifier)))
//
//  def field(identifier: String, accessor: FloatPropertyAccessor[M], defaultValue: Float): FloatProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new FloatProperty(null, identifier)))
//
//  def field(getter: IntGetter[M], setter: IntSetter[M]): IntegerProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new IntegerProperty()))
//
//  def immutableField(getter: IntGetter[M], immutableSetter: IntImmutableSetter[M]): IntegerProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new IntegerProperty()))
//
//  def field(getter: IntGetter[M], setter: IntSetter[M], defaultValue: Int): IntegerProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new IntegerProperty()))
//
//  def immutableField(getter: IntGetter[M], immutableSetter: IntImmutableSetter[M], defaultValue: Int): IntegerProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new IntegerProperty()))
//
//  def field(accessor: IntPropertyAccessor[M]): IntegerProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, () => new IntegerProperty()))
//
//  def field(accessor: IntPropertyAccessor[M], defaultValue: Int): IntegerProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new IntegerProperty()))
//
//  def field(identifier: String, getter: IntGetter[M], setter: IntSetter[M]): IntegerProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new IntegerProperty(null, identifier)))
//
//  def field(identifier: String, getter: IntGetter[M], setter: IntSetter[M], defaultValue: Int): IntegerProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new IntegerProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: IntGetter[M], immutableSetter: IntImmutableSetter[M]): IntegerProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new IntegerProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: IntGetter[M], immutableSetter: IntImmutableSetter[M], defaultValue: Int): IntegerProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new IntegerProperty(null, identifier)))
//
//  def field(identifier: String, accessor: IntPropertyAccessor[M]): IntegerProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, () => new IntegerProperty(null, identifier)))
//
//  def field(identifier: String, accessor: IntPropertyAccessor[M], defaultValue: Int): IntegerProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new IntegerProperty(null, identifier)))
//
//  def field(getter: LongGetter[M], setter: LongSetter[M]): LongProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new LongProperty()))
//
//  def immutableField(getter: LongGetter[M], immutableSetter: LongImmutableSetter[M]): LongProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new LongProperty()))
//
//  def field(getter: LongGetter[M], setter: LongSetter[M], defaultValue: Long): LongProperty =
//    add(new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new LongProperty()))
//
//  def immutableField(getter: LongGetter[M], immutableSetter: LongImmutableSetter[M], defaultValue: Long): LongProperty =
//    addImmutable(new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new LongProperty()))
//
//  def field(accessor: LongPropertyAccessor[M]): LongProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, () => new LongProperty()))
//
//  def field(accessor: LongPropertyAccessor[M], defaultValue: Long): LongProperty =
//    add(new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new LongProperty()))
//
//  def field(identifier: String, getter: LongGetter[M], setter: LongSetter[M]): LongProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, () => new LongProperty(null, identifier)))
//
//  def field(identifier: String, getter: LongGetter[M], setter: LongSetter[M], defaultValue: Long): LongProperty =
//    addIdentified(identifier, new BeanPropertyField(this.propertyWasChanged, getter, setter, defaultValue, () => new LongProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: LongGetter[M], immutableSetter: LongImmutableSetter[M]): LongProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, () => new LongProperty(null, identifier)))
//
//  def immutableField(identifier: String, getter: LongGetter[M], immutableSetter: LongImmutableSetter[M], defaultValue: Long): LongProperty =
//    addIdentifiedImmutable(identifier, new ImmutableBeanPropertyField(this.propertyWasChanged, getter, immutableSetter, defaultValue, () => new LongProperty(null, identifier)))
//
//  def field(identifier: String, accessor: LongPropertyAccessor[M]): LongProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, () => new LongProperty(null, identifier)))
//
//  def field(identifier: String, accessor: LongPropertyAccessor[M], defaultValue: Long): LongProperty =
//    addIdentified(identifier, new FxPropertyField(this.propertyWasChanged, accessor, defaultValue, () => new LongProperty(null, identifier)))

end ModelWrapper
