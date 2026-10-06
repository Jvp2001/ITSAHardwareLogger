package org.itsadigitaltrust.hardwarelogger.ui.derivation

import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.scene.control.{Spinner, TextField, Label}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.*

trait UIControlFactory[A]:
  def createField(initialValue: A, annotations: List[UISchemaAnnotation]): DeferredField

  final def apply(initialValue: A, annotations: List[UISchemaAnnotation]): DeferredField =
    createField(initialValue, annotations)

object UIControlFactory:

  given UIControlFactory[StringProperty] with
    override def createField(initialValue: StringProperty, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue())
          else
            new TextField:
              text <==> initialValue
        ,
        flushValue = () => initialValue
      )

  given UIControlFactory[ReadOnlyStringProperty] with
    override def createField(initialValue: ReadOnlyStringProperty, annotations: List[UISchemaAnnotation]): DeferredField =
      DeferredField(
        controlBuilder = () => Label(initialValue()),
        flushValue = () => initialValue
      )

  given UIControlFactory[DoubleProperty] with
    override def createField(initialValue: DoubleProperty, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue().toString)
          else
            val spinner = Spinner[Double](Double.MinValue, Double.MaxValue, initialValue())
            spinner.valueProperty().onChange { (_, _, newVal) => initialValue() = newVal }
            spinner
        ,
        flushValue = () => initialValue
      )

  given UIControlFactory[FloatProperty] with
    override def createField(initialValue: FloatProperty, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue().toString)
          else
            val spinner = Spinner[Double](Float.MinValue.toDouble, Float.MaxValue.toDouble, initialValue().toDouble)
            spinner.valueProperty().onChange { (_, _, newVal) => initialValue() = newVal.toFloat }
            spinner
        ,
        flushValue = () => initialValue
      )

  given UIControlFactory[IntegerProperty] with
    override def createField(initialValue: IntegerProperty, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue().toString)
          else
            val spinner = Spinner[Integer](Int.MinValue, Int.MaxValue, initialValue())
            spinner.valueProperty().onChange { (_, _, newVal) => initialValue() = newVal.toInt }
            spinner
        ,
        flushValue = () => initialValue
      )

  given UIControlFactory[LongProperty] with
    override def createField(initialValue: LongProperty, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue().toString)
          else
            val spinner = Spinner[Long](Long.MinValue, Long.MaxValue, initialValue())
            spinner.valueProperty().onChange { (_, _, newVal) => initialValue() = newVal.toLong }
            spinner
        ,
        flushValue = () => initialValue
      )

  private given numericReadonlyControlFactory[N <: Number]: UIControlFactory[ReadOnlyProperty[N, Number]] with
    override def createField(initialValue: ReadOnlyProperty[N, Number], annotations: List[UISchemaAnnotation]): DeferredField =
      DeferredField(
        controlBuilder = () => Label(initialValue.value.toString),
        flushValue = () => initialValue
      )

  // --- 1. Standard String Primitives ---
  given UIControlFactory[String] with
    override def createField(initialValue: String, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      val localProp = StringProperty(initialValue)
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
          Label(initialValue)
          else
            new TextField:
              text <==> localProp
        ,
        flushValue = () => localProp.value
      )

  // --- 2. Floating-Point Numeric Primitives ---
  given UIControlFactory[Double] with
    override def createField(initialValue: Double, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      val localProp = DoubleProperty(initialValue)
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue.toString)
          else
            val spinner = Spinner[Double](Double.MinValue, Double.MaxValue, initialValue)
            spinner.valueProperty().onChange { (_, _, newVal) => localProp() = newVal }
            spinner
        ,
        flushValue = () => localProp.value
      )

  given UIControlFactory[Float] with
    override def createField(initialValue: Float, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      val localProp = FloatProperty(initialValue)
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue.toString)
          else
            val spinner = Spinner[Double](Float.MinValue.toDouble, Float.MaxValue.toDouble, initialValue.toDouble)
            spinner.valueProperty().onChange { (_, _, newVal) => localProp() = newVal.toFloat }
            spinner
        ,
        flushValue = () => localProp.value
      )

  // --- 3. Integral Numeric Primitives ---
  given UIControlFactory[Int] with
    override def createField(initialValue: Int, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      val localProp = IntegerProperty(initialValue)
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue.toString)
          else
            val spinner = Spinner[Integer](Int.MinValue, Int.MaxValue, initialValue)
            spinner.valueProperty().onChange { (_, _, newVal) => localProp() = newVal.toInt }
            spinner
        ,
        flushValue = () => localProp.value
      )

  given UIControlFactory[Long] with
    override def createField(initialValue: Long, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      val localProp = LongProperty(initialValue)
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            Label(initialValue.toString)
          else
            val spinner = Spinner[Long](Long.MinValue, Long.MaxValue, initialValue)
            spinner.valueProperty().onChange { (_, _, newVal) => localProp() = newVal.toLong }
            spinner
        ,
        flushValue = () => localProp.value
      )

  // --- 4. Boolean Flag Primitives ---
  given UIControlFactory[Boolean] with
    override def createField(initialValue: Boolean, annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
      val localProp = BooleanProperty(initialValue)
      import scalafx.scene.control.CheckBox
      DeferredField(
        controlBuilder = () =>
          if isReadOnly then
            val cb = new CheckBox:
              selected = initialValue
              disable = true
            cb
          else
            new CheckBox:
              selected <==> localProp
        ,
        flushValue = () => localProp.value
      )
      
  end given

  // --- 5. Generic Option Combinator Factory ---
  given optionalControlFactory[A](using inner: UIControlFactory[A]): UIControlFactory[Option[A]] with
    override def createField(initialValue: Option[A], annotations: List[UISchemaAnnotation]): DeferredField =
      val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])

      // 1. Handle the Read-Only visual path cleanly
      if isReadOnly then
        DeferredField(
          controlBuilder = () =>
            initialValue match
              case Some(value) =>
                // Unroll the inner factory logic forcing it as read-only
                inner.createField(value, annotations).controlBuilder()
              case None =>
                new Label:
                  text = "None"
                  style = "-fx-font-style: italic; -fx-text-fill: #a0a0a0; -fx-padding: 4 0 4 0;"
          ,
          flushValue = () => initialValue
        )

      // 2. Handle the Editable visual path with dynamic sub-node injection
      else
        import scalafx.scene.layout.HBox
        import scalafx.scene.control.CheckBox
        import scalafx.geometry.Pos

        // Track whether the optional value is currently active
        val presenceCheck = new CheckBox:
          selected = initialValue.isDefined

        // Extract an initial inner value or fall back to a type-safe zero-initialized default
        val underlyingValue = initialValue.getOrElse(fallbackDefault[A])
        val innerField = inner.createField(underlyingValue, annotations)

        // Pre-build the interactive primitive node control wrapper
        val innerControlNode = innerField.controlBuilder()
        innerControlNode.visible <== presenceCheck.selected
        innerControlNode.managed <== presenceCheck.selected

        DeferredField(
          controlBuilder = () =>
            new HBox:
              spacing = 10
              alignment = Pos.CenterLeft
              children = Seq(presenceCheck, innerControlNode)
          ,
          flushValue = () =>
            if presenceCheck.selected() then
              Some(innerField.flushValue().asInstanceOf[A])
            else
              None
        )

  /** Helper method to supply clean default boundary fallbacks for missing Option contents */
  private inline def fallbackDefault[A]: A =
    val stringType = summon[scala.reflect.ClassTag[String]]
    val intType = summon[scala.reflect.ClassTag[Int]]
    val doubleType = summon[scala.reflect.ClassTag[Double]]
    val booleanType = summon[scala.reflect.ClassTag[Boolean]]

    inline scala.compiletime.erasedValue[A] match
      case _: String => "".asInstanceOf[A]
      case _: Int => 0.asInstanceOf[A]
      case _: Double => 0.0.asInstanceOf[A]
      case _: Boolean => false.asInstanceOf[A]
      case _ => null.asInstanceOf[A]


end UIControlFactory
