package dev.slne.surf.api.di

/**
 * Marks a class as a bean that provides behaviour, with an optional [scope].
 *
 * ```kotlin
 * @Service
 * class ShopService(
 *     private val transactions: TransactionApi,
 *     private val handlers: List<PriceHandler>,
 * )
 * ```
 *
 * The annotation carries no behaviour of its own beyond [BeanMeta] and the scope; it exists so the
 * intent reads at the declaration. Use [Repository] for a bean whose job is storage.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@BeanMeta
annotation class Service(val scope: Scope = Scope.SINGLETON)
