package zaychik.extensions

import org.koin.java.KoinJavaComponent

inline fun <reified T> inject(): Lazy<T> {
    return KoinJavaComponent.inject(T::class.java)
}
