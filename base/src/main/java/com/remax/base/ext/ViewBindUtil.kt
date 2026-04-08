package com.example.base.extention

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@JvmName("inflateWithGeneric")
fun <VB : ViewBinding> AppCompatActivity.inflateBindingWithGeneric(layoutInflater: LayoutInflater): VB =
    withGenericBindingClass<VB, Any>(this) { clazz ->
        clazz.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB
    }.also { binding ->
        if (binding is ViewDataBinding) {
            binding.lifecycleOwner = this
        }
    }

@JvmName("inflateWithGeneric")
fun <VB : ViewBinding> Fragment.inflateBindingWithGeneric(
    layoutInflater: LayoutInflater,
    parent: ViewGroup?,
    attachToParent: Boolean
): VB {
    return inflateBindingWithGeneric<VB, Any>(null, layoutInflater, parent, attachToParent)
}

fun <VB : ViewBinding, T> Fragment.inflateBindingWithGeneric(
    targetClass: Class<T>? = null,
    layoutInflater: LayoutInflater,
    parent: ViewGroup?,
    attachToParent: Boolean
): VB =
    withGenericBindingClass<VB, T>(this, targetClass) { clazz ->
        clazz.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
            .invoke(null, layoutInflater, parent, attachToParent) as VB
    }.also { binding ->
        if (binding is ViewDataBinding) {
            binding.lifecycleOwner = viewLifecycleOwner
        }
    }

private fun <VB : ViewBinding, T> withGenericBindingClass(
    any: Any,
    targetClass: Class<T>? = null,
    block: (Class<VB>) -> VB
): VB {
    var genericSuperclass = any::class.java.genericSuperclass
    var superclass = any::class.java.superclass
    while (superclass != null) {
        if (genericSuperclass is ParameterizedType && (targetClass == null || targetClass == superclass)) {
            try {
                val type = if (genericSuperclass.actualTypeArguments.size > 1) {
                    genericSuperclass.actualTypeArguments[1]
                } else {
                    genericSuperclass.actualTypeArguments[0]
                }
                return block.invoke(type as Class<VB>)
            } catch (e: NoSuchMethodException) {
            } catch (e: ClassCastException) {
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
        genericSuperclass = superclass.genericSuperclass
        superclass = superclass.superclass
    }
    throw IllegalArgumentException("There is no generic of ViewBinding.")
}

inline fun <reified T : ViewBinding> ViewGroup.viewBinding() =
    ViewBindingDelegate(T::class.java, this)

class ViewBindingDelegate<T : ViewBinding>(
    private val bindingClass: Class<T>,
    val view: ViewGroup
) : ReadOnlyProperty<ViewGroup, T> {
    private var binding: T? = null

    override fun getValue(thisRef: ViewGroup, property: KProperty<*>): T {
        binding?.let { return it }

        @Suppress("UNCHECKED_CAST")
        binding = try {
            val inflateMethod =
                bindingClass.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java)
            inflateMethod.invoke(null, LayoutInflater.from(thisRef.context), thisRef) as T
        } catch (e: NoSuchMethodException) {
            val inflateMethod = bindingClass.getMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java
            )
            inflateMethod.invoke(null, LayoutInflater.from(thisRef.context), thisRef, true) as T
        }

        return binding ?: throw IllegalStateException("Binding should have been initialized")
    }
}