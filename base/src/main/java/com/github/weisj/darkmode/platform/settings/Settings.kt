package com.github.weisj.darkmode.platform.settings

import kotlin.reflect.KMutableProperty0

interface SettingsContainerProvider {
    val enabled : Boolean
    fun create() : SettingsContainer
}

open class SingletonSettingsContainerProvider(
    provider : () -> SettingsContainer,
    override val enabled : Boolean = true
) : SettingsContainerProvider {
    private val container by lazy(provider)
    override fun create(): SettingsContainer = container
}

interface SettingsContainer : SettingsGroup {
    val namedGroups: MutableList<NamedSettingsGroup>
    val unnamedGroup: SettingsGroup
}

fun SettingsContainer.allProperties() : List<ValueProperty<Any>> = namedGroups.flatten() + unnamedGroup

fun SettingsContainer.withName(name : String) = lazy{ allProperties().first { it.name == name } }

fun <T> SettingsContainer.withProperty(prop : KMutableProperty0<T>) = withName(prop.name)

/**
 * Container for {@link ValueProperty}s. Properties can be group into
 * logical units using a {@SettingsGroup}.
 *
 * All properties not contained inside a {@SettingsGroup} will automatically belong
 * to the unnamed group of the container.
 */
abstract class DefaultSettingsContainer private constructor(
    override val unnamedGroup: SettingsGroup
) : SettingsContainer, SettingsGroup by unnamedGroup {

    constructor() : this(DefaultSettingsGroup())

    override val namedGroups: MutableList<NamedSettingsGroup> = mutableListOf()
}

fun <T> SettingsGroup.add(property: ValueProperty<T>) {
    this.add(property.castSafelyTo()!!)
}

/**
 * Provides grouping for {@link ValueProperty}s
 */
typealias SettingsGroup = MutableList<ValueProperty<Any>>

interface NamedSettingsGroup : SettingsGroup {
    val name: String
}

open class DefaultSettingsGroup internal constructor(
    private val properties: MutableList<ValueProperty<Any>> = mutableListOf()
) : SettingsGroup by properties

class DefaultNamedSettingsGroup internal constructor(
    override val name: String
) : DefaultSettingsGroup(), NamedSettingsGroup

/**
 * Wrapper for properties that provides a description and parser/writer used
 * for persistent storage.
 */
interface ValueProperty<T> : Observable<ValueProperty<*>> {
    val description: String
    val name: String
    var value: T
    var active: Boolean
}

/**
 * Property with a backing value that has a different type than the exposed value.
 */
interface TransformingValueProperty<R, T> : ValueProperty<T> {
    var backingValue: R
}

/**
 * Property that can be stored in String format.
 */
interface PersistentValueProperty<T> : TransformingValueProperty<T, String>

fun ValueProperty<*>.toTransformer(): TransformingValueProperty<Any, Any>? =
    castSafelyTo<TransformingValueProperty<Any, Any>>()

inline fun <reified T : Any> ValueProperty<T>.asPersistent(): PersistentValueProperty<T>? =
    castSafelyTo<PersistentValueProperty<T>>()

/**
 * The effective value of the property. If the property is a transforming property the
 * backing field is chosen. Because of this for a reference to a simple ValueProperty<T>
 * the most general value that can be returned is Any.
 */
val <T : Any> ValueProperty<T>.effectiveProperty : KMutableProperty0<Any>
    get() = toTransformer()?.let { it::backingValue } ?: this::value.withOutType()!!

// Offers type specific overload of effective property for transforming properties.
val <R, T> TransformingValueProperty<R, T>.effective: KMutableProperty0<R>
    get() = ::backingValue


class SimpleValueProperty<T : Any> internal constructor(
    description: String?,
    property: KMutableProperty0<T>
) : ValueProperty<T>, Observable<ValueProperty<*>> by DefaultObservable() {
    override val description: String = description ?: property.name
    override val name by property::name
    override var value: T by property
    override var active by observable(true)
}

open class SimpleTransformingValueProperty<R, T : Any> internal constructor(
    delegate: ValueProperty<R>,
    transformer: Transformer<R, T>
) : TransformingValueProperty<R, T>, Observable<ValueProperty<*>> by delegate {
    override val description by delegate::description
    override val name by delegate::name
    override var active by delegate::active

    override var backingValue: R by delegate::value
    override var value: T by transformer.delegate(backingProp = ::backingValue)
}

class SimplePersistentValueProperty<R>(
    delegate: ValueProperty<R>,
    transformer: Transformer<R, String>
) : SimpleTransformingValueProperty<R, String>(delegate, transformer),
    PersistentValueProperty<R>

/**
 * Property that has a limited set of values the property can take on.
 */
abstract class ChoiceProperty<R, T> internal constructor(
    private val delegateProperty: TransformingValueProperty<R, T>
) : TransformingValueProperty<R, T> by delegateProperty {
    var choiceValue: R by ::backingValue
    var choices: List<R> = ArrayList()
    var renderer: (R) -> String = { it.toString() }
}

class TransformingChoiceProperty<R, T : Any> internal constructor(
    property: TransformingValueProperty<R, T>
) : ChoiceProperty<R, T>(property) {
    constructor(property: ValueProperty<R>, transformer: Transformer<R, T>)
            : this(SimpleTransformingValueProperty(property, transformer))
}

class PersistentChoiceProperty<R>(
    property: PersistentValueProperty<R>
) : ChoiceProperty<R, String>(property),
    PersistentValueProperty<R> {
    constructor(property: ValueProperty<R>, transformer: Transformer<R, String>)
            : this(SimplePersistentValueProperty(property, transformer))
}

interface PropertyController<T> {
    var predicate: (T?) -> Boolean
    val controlled: MutableSet<Lazy<ValueProperty<*>>>

    @JvmDefault
    fun control(vararg properties : Lazy<ValueProperty<*>>) {
        controlled.addAll(properties)
    }

    @JvmDefault
    fun control(vararg properties : ValueProperty<*>) {
        controlled.addAll(properties.map { lazyOf(it) })
    }
}

class SimplePropertyController<T>(override var predicate: (T?) -> Boolean) : PropertyController<T> {
    override val controlled : MutableSet<Lazy<ValueProperty<*>>> = mutableSetOf()
}

fun <T> PropertyController<T>.inverted() = predicate.let { predicate = { t -> !it(t) } }

class SimpleBooleanProperty(
    delegate: SimpleValueProperty<Boolean>,
    predicate: (Boolean?) -> Boolean = { it?:false }
) : ValueProperty<Boolean> by delegate, PropertyController<Boolean> by SimplePropertyController(predicate)

class SimplePersistentBooleanProperty(
    delegate: PersistentValueProperty<Boolean>,
    predicate: (Boolean?) -> Boolean = { it?:false }
) : PersistentValueProperty<Boolean> by delegate, PropertyController<Boolean> by SimplePropertyController(predicate)

fun SettingsContainer.group(name: String = "", init: SettingsGroup.() -> Unit) : SettingsGroup {
    val group = DefaultNamedSettingsGroup(name)
    namedGroups.add(group)
    group.init()
    return group
}

fun SettingsContainer.unnamedGroup(init: SettingsGroup.() -> Unit) : SettingsGroup {
    init()
    return this
}

fun <T : ValueProperty<T>> SettingsGroup.property(property: T, init: T.() -> Unit = {}): T =
    property.also { it.init(); add(it) }

fun <T : Any> SettingsGroup.property(
    description: String? = null,
    value: KMutableProperty0<T>,
    init: SimpleValueProperty<T>.() -> Unit = {}
): ValueProperty<T> =
    SimpleValueProperty(description, value).also { it.init(); add(it) }

fun <R : Any, T : Any> SettingsGroup.property(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>,
    init: SimpleValueProperty<R>.() -> Unit = {}
): TransformingValueProperty<R, T> =
    SimpleTransformingValueProperty(SimpleValueProperty(description, value).also(init), transformer).also { add(it) }

fun SettingsGroup.stringProperty(
    description: String? = null,
    value: KMutableProperty0<String>
): ValueProperty<String> = property(description, value)

fun SettingsGroup.booleanProperty(
    description: String? = null,
    value: KMutableProperty0<Boolean>,
    init: SimpleBooleanProperty.() -> Unit = {}
): ValueProperty<Boolean> = SimpleBooleanProperty(SimpleValueProperty(description, value)).also { it.init(); add(it) }

fun <R : Any, T : Any> SettingsGroup.choiceProperty(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>,
    init: ChoiceProperty<R, T>.() -> Unit = {}
): ChoiceProperty<R, T> =
    TransformingChoiceProperty(SimpleValueProperty(description, value), transformer).also { it.init(); add(it) }

fun <T : Any> SettingsGroup.choiceProperty(
    description: String? = null,
    value: KMutableProperty0<T>,
    init: ChoiceProperty<T, T>.() -> Unit = {}
): ChoiceProperty<T, T> =
    TransformingChoiceProperty<T, T>(
        SimpleValueProperty(description, value),
        identityTransformer()
    ).also { it.init(); add(it) }

fun <R : Any> SettingsGroup.persistentProperty(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, String>,
    init: SimpleValueProperty<R>.() -> Unit = {}
): PersistentValueProperty<R> =
    SimplePersistentValueProperty(SimpleValueProperty(description, value).also(init), transformer).also { add(it) }

fun SettingsGroup.persistentStringProperty(
    description: String? = null,
    value: KMutableProperty0<String>
): ValueProperty<String> = persistentProperty(description, value, identityTransformer())

fun SettingsGroup.persistentBooleanProperty(
    description: String? = null,
    value: KMutableProperty0<Boolean>,
    init: SimplePersistentBooleanProperty.() -> Unit = {}
): PersistentValueProperty<Boolean> =
    SimplePersistentBooleanProperty(
        SimplePersistentValueProperty(
            SimpleValueProperty(description, value),
            transformerOf(String::toBoolean, Boolean::toString)
        )
    ).also { it.init(); add(it) }

fun <R : Any> SettingsGroup.persistentChoiceProperty(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, String>,
    init: ChoiceProperty<R, String>.() -> Unit = {}
): ChoiceProperty<R, String> =
    PersistentChoiceProperty(SimpleValueProperty(description, value), transformer).also { it.init(); add(it) }

fun SettingsGroup.persistentChoiceProperty(
    description: String? = null,
    value: KMutableProperty0<String>,
    init: ChoiceProperty<String, String>.() -> Unit = {}
): ChoiceProperty<String, String> = choiceProperty(description, value, init)

