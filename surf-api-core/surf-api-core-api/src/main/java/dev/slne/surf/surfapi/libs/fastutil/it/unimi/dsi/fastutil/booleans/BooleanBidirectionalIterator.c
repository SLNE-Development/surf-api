/* Generic definitions */


#define package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.booleans
#define VALUE_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.objects
#define WIDENED_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.booleans
/* Assertions (useful to generate conditional code) */
#define KEY_CLASS_Boolean 1
 #define KEYS_PRIMITIVE 1
#define VALUE_CLASS_Object 1
 #define VALUES_REFERENCE 1
/* Narrowing and widening */
#define KEY_NARROWING(x) x
#define KEY_LONG_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeLongToBoolean(x)
#define VALUE_NARROWING(x) x
/* Current type and class (and size, if applicable) */
#define KEY_TYPE boolean
#define KEY_TYPE_CAP Boolean
#define VALUE_TYPE Object
#define VALUE_TYPE_CAP Object
#define KEY_INDEX 0
#define KEY_TYPE_WIDENED boolean
#define VALUE_TYPE_WIDENED Object
#define KEY_CLASS Boolean
#define VALUE_CLASS Object
#define VALUE_INDEX 8
#define KEY_CLASS_WIDENED Boolean
#define VALUE_CLASS_WIDENED Object
#define KEYS_USE_REFERENCE_EQUALITY KEY_CLASS_Reference
#define VALUES_USE_REFERENCE_EQUALITY VALUE_CLASS_Reference
#if KEYS_REFERENCE
#define KEY_GENERIC_CLASS K
#define KEY_GENERIC_TYPE K
#define KEY_GENERIC_CLASS_WIDENED K
#define KEY_GENERIC_TYPE_WIDENED K
#define KEY_GENERIC <K>
#define KEY_GENERIC_DIAMOND <>
#define KEY_GENERIC_WILDCARD <?>
#define KEY_EXTENDS_GENERIC <? extends K>
#define KEY_SUPER_GENERIC <? super K>
#define KEY_CLASS_CAST (K)
#define KEY_GENERIC_CAST (K)
#define KEY_GENERIC_ARRAY_CAST (K[])
#define KEY_GENERIC_BIG_ARRAY_CAST (K[][])
#define DEPRECATED_IF_KEYS_REFERENCE @Deprecated
#define DEPRECATED_IF_KEYS_PRIMITIVE
#define SUPPRESS_WARNINGS_KEY_UNCHECKED @SuppressWarnings("unchecked")
#define SUPPRESS_WARNINGS_KEY_RAWTYPES @SuppressWarnings("rawtypes")
#define SUPPRESS_WARNINGS_KEY_UNCHECKED_RAWTYPES @SuppressWarnings({"unchecked","rawtypes"})
#define SAFE_VARARGS @SafeVarargs
#if defined(Custom)
#define SUPPRESS_WARNINGS_CUSTOM_KEY_UNCHECKED @SuppressWarnings("unchecked")
#else
#define SUPPRESS_WARNINGS_CUSTOM_KEY_UNCHECKED
#endif
#else
#define KEY_GENERIC_CLASS KEY_CLASS
#define KEY_GENERIC_TYPE KEY_TYPE
#define KEY_GENERIC_CLASS_WIDENED KEY_CLASS_WIDENED
#define KEY_GENERIC_TYPE_WIDENED KEY_TYPE_WIDENED
#define KEY_GENERIC
#define KEY_GENERIC_DIAMOND
#define KEY_GENERIC_WILDCARD
#define KEY_EXTENDS_GENERIC
#define KEY_SUPER_GENERIC
#define KEY_CLASS_CAST (KEY_CLASS)
#define KEY_GENERIC_CAST
#define KEY_GENERIC_ARRAY_CAST
#define KEY_GENERIC_BIG_ARRAY_CAST
#define DEPRECATED_IF_KEYS_REFERENCE
#define DEPRECATED_IF_KEYS_PRIMITIVE @Deprecated
#define SUPPRESS_WARNINGS_KEY_UNCHECKED
#define SUPPRESS_WARNINGS_KEY_RAWTYPES
#define SUPPRESS_WARNINGS_KEY_UNCHECKED_RAWTYPES
#define SUPPRESS_WARNINGS_CUSTOM_KEY_UNCHECKED
#define SAFE_VARARGS
#endif
#if VALUES_REFERENCE
#define VALUE_GENERIC_CLASS V
#define VALUE_GENERIC_TYPE V
#define VALUE_GENERIC_CLASS_WIDENED V
#define VALUE_GENERIC_TYPE_WIDENED V
#define VALUE_GENERIC <V>
#define VALUE_GENERIC_DIAMOND <>
#define VALUE_EXTENDS_GENERIC <? extends V>
#define VALUE_SUPER_GENERIC <? super V>
#define VALUE_GENERIC_CAST (V)
#define VALUE_GENERIC_ARRAY_CAST (V[])
#define DEPRECATED_IF_VALUES_REFERENCE @Deprecated
#define DEPRECATED_IF_VALUES_PRIMITIVE
#define SUPPRESS_WARNINGS_VALUE_UNCHECKED @SuppressWarnings("unchecked")
#define SUPPRESS_WARNINGS_VALUE_RAWTYPES @SuppressWarnings("rawtypes")
#else
#define VALUE_GENERIC_CLASS VALUE_CLASS
#define VALUE_GENERIC_TYPE VALUE_TYPE
#define VALUE_GENERIC_CLASS_WIDENED VALUE_CLASS_WIDENED
#define VALUE_GENERIC_TYPE_WIDENED VALUE_TYPE_WIDENED
#define VALUE_GENERIC
#define VALUE_GENERIC_DIAMOND
#define VALUE_EXTENDS_GENERIC
#define VALUE_SUPER_GENERIC
#define VALUE_GENERIC_CAST
#define VALUE_GENERIC_ARRAY_CAST
#define DEPRECATED_IF_VALUES_REFERENCE
#define DEPRECATED_IF_VALUES_PRIMITIVE @Deprecated
#define SUPPRESS_WARNINGS_VALUE_UNCHECKED
#define SUPPRESS_WARNINGS_VALUE_RAWTYPES
#endif
#if KEYS_REFERENCE
#if VALUES_REFERENCE
#define KEY_VALUE_GENERIC <K,V>
#define KEY_VALUE_GENERIC_DIAMOND <>
#define KEY_VALUE_EXTENDS_GENERIC <? extends K, ? extends V>
#define KEY_GENERIC_VALUE_EXTENDS_GENERIC <K, ? extends V>
#define KEY_SUPER_GENERIC_VALUE_EXTENDS_GENERIC <? super K, ? extends V>
#else
#define KEY_VALUE_GENERIC <K>
#define KEY_VALUE_GENERIC_DIAMOND <>
#define KEY_VALUE_EXTENDS_GENERIC <? extends K>
#define KEY_GENERIC_VALUE_EXTENDS_GENERIC <K>
#define KEY_SUPER_GENERIC_VALUE_EXTENDS_GENERIC <? super K>
#endif
#else
#if VALUES_REFERENCE
#define KEY_VALUE_GENERIC <V>
#define KEY_VALUE_GENERIC_DIAMOND <>
#define KEY_VALUE_EXTENDS_GENERIC <? extends V>
#define KEY_GENERIC_VALUE_EXTENDS_GENERIC <? extends V>
#define KEY_SUPER_GENERIC_VALUE_EXTENDS_GENERIC <? extends V>
#else
#define KEY_VALUE_GENERIC
#define KEY_VALUE_GENERIC_DIAMOND
#define KEY_VALUE_EXTENDS_GENERIC
#define KEY_GENERIC_VALUE_EXTENDS_GENERIC
#define KEY_SUPER_GENERIC_VALUE_EXTENDS_GENERIC
#endif
#endif
#if KEYS_REFERENCE || VALUES_REFERENCE
#define SUPPRESS_WARNINGS_KEY_VALUE_UNCHECKED @SuppressWarnings("unchecked")
#define SUPPRESS_WARNINGS_KEY_VALUE_RAWTYPES @SuppressWarnings("rawtypes")
#define SUPPRESS_WARNINGS_KEY_VALUE_UNCHECKED_RAWTYPES @SuppressWarnings({"rawtypes", "unchecked"})
#else
#define SUPPRESS_WARNINGS_KEY_VALUE_UNCHECKED
#define SUPPRESS_WARNINGS_KEY_VALUE_RAWTYPES
#define SUPPRESS_WARNINGS_KEY_VALUE_UNCHECKED_RAWTYPES
#endif
/* Value methods */
#define KEY_VALUE booleanValue
#define KEY_WIDENED_VALUE booleanValue
#define VALUE_VALUE ObjectValue
#define VALUE_WIDENED_VALUE ObjectValue
/* Interfaces (keys) */
#define COLLECTION BooleanCollection
#define STD_KEY_COLLECTION BooleanCollection
#define SET BooleanSet
#define HASH BooleanHash
#define SORTED_SET BooleanSortedSet
#define STD_SORTED_SET BooleanSortedSet
#define FUNCTION Boolean2ObjectFunction
#define MAP Boolean2ObjectMap
#define SORTED_MAP Boolean2ObjectSortedMap
#if KEY_CLASS_Object && VALUE_CLASS_Object
#define PAIR it.unimi.dsi.fastutil.Pair
#define SORTED_PAIR it.unimi.dsi.fastutil.SortedPair
#else
#define PAIR BooleanObjectPair
#define SORTED_PAIR BooleanObjectSortedPair
#endif
#define MUTABLE_PAIR BooleanObjectMutablePair
#define IMMUTABLE_PAIR BooleanObjectImmutablePair
#define IMMUTABLE_SORTED_PAIR BooleanBooleanImmutableSortedPair
#if KEYS_REFERENCE
#define STD_SORTED_MAP SortedMap
#define STRATEGY Strategy
#else
#define STD_SORTED_MAP Boolean2ObjectSortedMap
#define STRATEGY PACKAGE.BooleanHash.Strategy
#endif
#define LIST BooleanList
#define BIG_LIST BooleanBigList
#define STACK BooleanStack
#define ATOMIC_ARRAY AtomicBooleanArray
#define PRIORITY_QUEUE BooleanPriorityQueue
#define INDIRECT_PRIORITY_QUEUE BooleanIndirectPriorityQueue
#define INDIRECT_DOUBLE_PRIORITY_QUEUE BooleanIndirectDoublePriorityQueue
#define KEY_CONSUMER BooleanConsumer
#define KEY_PREDICATE BooleanPredicate
#define KEY_UNARY_OPERATOR BooleanUnaryOperator
#define KEY_BINARY_OPERATOR BooleanBinaryOperator
#define KEY_ITERATOR BooleanIterator
#define KEY_WIDENED_ITERATOR BooleanIterator
#define KEY_ITERABLE BooleanIterable
#define KEY_SPLITERATOR BooleanSpliterator
#define KEY_WIDENED_SPLITERATOR BooleanSpliterator
#define KEY_BIDI_ITERATOR BooleanBidirectionalIterator
#define KEY_BIDI_ITERABLE BooleanBidirectionalIterable
#define KEY_LIST_ITERATOR BooleanListIterator
#define KEY_BIG_LIST_ITERATOR BooleanBigListIterator
#define STD_KEY_ITERATOR BooleanIterator
#define STD_KEY_SPLITERATOR BooleanSpliterator
#define STD_KEY_ITERABLE BooleanIterable
#define KEY_COMPARATOR BooleanComparator
/* Interfaces (values) */
#define VALUE_COLLECTION ObjectCollection
#define VALUE_ARRAY_SET ObjectArraySet
#define VALUE_CONSUMER Consumer
#define VALUE_BINARY_OPERATOR BinaryOperator
#define VALUE_ITERATOR ObjectIterator
#define VALUE_SPLITERATOR ObjectSpliterator
#define VALUE_LIST_ITERATOR ObjectListIterator
/* Types and methods related to primitive-type support in the JDK */
#if KEYS_PRIMITIVE && ! KEY_CLASS_Boolean
#define JDK_PRIMITIVE_KEY_CONSUMER java.util.function.BooleanConsumer
#define JDK_PRIMITIVE_PREDICATE java.util.function.BooleanPredicate
#define JDK_PRIMITIVE_BINARY_OPERATOR java.util.function.BooleanBinaryOperator
#define JDK_PRIMITIVE_BINARY_OPERATOR_APPLY applyAsBoolean
#define JDK_PRIMITIVE_ITERATOR PrimitiveIterator.OfBoolean
#define JDK_PRIMITIVE_SPLITERATOR Spliterator.OfBoolean
#define JDK_PRIMITIVE_STREAM java.util.stream.BooleanStream
#define JDK_PRIMITIVE_UNARY_OPERATOR java.util.function.BooleanUnaryOperator
#define JDK_PRIMITIVE_KEY_APPLY applyAsBoolean
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.BooleanFunction
#else
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.Function
#endif
#if VALUES_PRIMITIVE && ! VALUE_CLASS_Boolean
#define JDK_PRIMITIVE_VALUE_CONSUMER java.util.function.ObjectConsumer
#define JDK_PRIMITIVE_VALUE_BINARY_OPERATOR java.util.function.ObjectBinaryOperator
#define JDK_PRIMITIVE_VALUE_OPERATOR_APPLY applyAsObject
#endif
#if KEYS_INT_LONG_DOUBLE
#define METHOD_ARG_KEY_CONSUMER JDK_PRIMITIVE_KEY_CONSUMER
#define METHOD_ARG_PREDICATE JDK_PRIMITIVE_PREDICATE
#define METHOD_ARG_KEY_UNARY_OPERATOR JDK_PRIMITIVE_UNARY_OPERATOR
#define METHOD_ARG_KEY_BINARY_OPERATOR JDK_PRIMITIVE_BINARY_OPERATOR
#define KEY_OPERATOR_APPLY applyAsBoolean
#else
#define METHOD_ARG_KEY_CONSUMER KEY_CONSUMER KEY_SUPER_GENERIC
#define METHOD_ARG_PREDICATE KEY_PREDICATE KEY_SUPER_GENERIC
#define METHOD_ARG_KEY_UNARY_OPERATOR KEY_UNARY_OPERATOR KEY_GENERIC
#define METHOD_ARG_KEY_BINARY_OPERATOR KEY_BINARY_OPERATOR KEY_GENERIC
#define KEY_OPERATOR_APPLY apply
#endif
#if VALUES_INT_LONG_DOUBLE
#define METHOD_ARG_VALUE_CONSUMER JDK_PRIMITIVE_VALUE_CONSUMER
#define METHOD_ARG_VALUE_BINARY_OPERATOR JDK_PRIMITIVE_VALUE_BINARY_OPERATOR
#define VALUE_OPERATOR_APPLY applyAsObject
#else
#define METHOD_ARG_VALUE_CONSUMER VALUE_CONSUMER VALUE_SUPER_GENERIC
#define METHOD_ARG_VALUE_BINARY_OPERATOR VALUE_PACKAGE.VALUE_BINARY_OPERATOR VALUE_GENERIC
#define VALUE_OPERATOR_APPLY apply
#endif
/* Abstract implementations (keys) */
#define ABSTRACT_COLLECTION AbstractBooleanCollection
#define ABSTRACT_SET AbstractBooleanSet
#define ABSTRACT_SORTED_SET AbstractBooleanSortedSet
#define ABSTRACT_FUNCTION AbstractBoolean2ObjectFunction
#define ABSTRACT_MAP AbstractBoolean2ObjectMap
#define ABSTRACT_FUNCTION AbstractBoolean2ObjectFunction
#define ABSTRACT_SORTED_MAP AbstractBoolean2ObjectSortedMap
#define ABSTRACT_LIST AbstractBooleanList
#define ABSTRACT_BIG_LIST AbstractBooleanBigList
#define SUBLIST BooleanSubList
#define SUBLIST_RANDOM_ACCESS BooleanRandomAccessSubList
#define ABSTRACT_PRIORITY_QUEUE AbstractBooleanPriorityQueue
#define ABSTRACT_STACK AbstractBooleanStack
#define KEY_ABSTRACT_ITERATOR AbstractBooleanIterator
#define KEY_ABSTRACT_SPLITERATOR AbstractBooleanSpliterator
#define KEY_ABSTRACT_BIDI_ITERATOR AbstractBooleanBidirectionalIterator
#define KEY_ABSTRACT_LIST_ITERATOR AbstractBooleanListIterator
#define KEY_ABSTRACT_BIG_LIST_ITERATOR AbstractBooleanBigListIterator
#if KEY_CLASS_Object
#define KEY_ABSTRACT_COMPARATOR Comparator
#else
#define KEY_ABSTRACT_COMPARATOR AbstractBooleanComparator
#endif
/* Abstract implementations (values) */
#define VALUE_ABSTRACT_COLLECTION AbstractObjectCollection
#define VALUE_ABSTRACT_ITERATOR AbstractObjectIterator
#define VALUE_ABSTRACT_BIDI_ITERATOR AbstractObjectBidirectionalIterator
/* Static containers (keys) */
#define COLLECTIONS BooleanCollections
#define SETS BooleanSets
#define SORTED_SETS BooleanSortedSets
#define LISTS BooleanLists
#define BIG_LISTS BooleanBigLists
#define MAPS Boolean2ObjectMaps
#define FUNCTIONS Boolean2ObjectFunctions
#define SORTED_MAPS Boolean2ObjectSortedMaps
#define PRIORITY_QUEUES BooleanPriorityQueues
#define HEAPS BooleanHeaps
#define SEMI_INDIRECT_HEAPS BooleanSemiIndirectHeaps
#define INDIRECT_HEAPS BooleanIndirectHeaps
#define ARRAYS BooleanArrays
#define BIG_ARRAYS BooleanBigArrays
#define ITERABLES BooleanIterables
#define ITERATORS BooleanIterators
#define WIDENED_ITERATORS BooleanIterators
#define SPLITERATORS BooleanSpliterators
#define WIDENED_SPLITERATORS BooleanSpliterators
#define BIG_LIST_ITERATORS BooleanBigListIterators
#define BIG_SPLITERATORS BooleanBigSpliterators
#define COMPARATORS BooleanComparators
/* Static containers (values) */
#define VALUE_COLLECTIONS ObjectCollections
#define VALUE_SETS ObjectSets
#define VALUE_ARRAYS ObjectArrays
#define VALUE_ITERATORS ObjectIterators
#define VALUE_SPLITERATORS ObjectSpliterators
/* Implementations */
#define OPEN_HASH_SET BooleanOpenHashSet
#define OPEN_HASH_BIG_SET BooleanOpenHashBigSet
#define OPEN_DOUBLE_HASH_SET BooleanOpenDoubleHashSet
#define OPEN_HASH_MAP Boolean2ObjectOpenHashMap
#define OPEN_HASH_BIG_MAP Boolean2ObjectOpenHashBigMap
#define STRIPED_OPEN_HASH_MAP StripedBoolean2ObjectOpenHashMap
#define OPEN_DOUBLE_HASH_MAP Boolean2ObjectOpenDoubleHashMap
#define ARRAY_SET BooleanArraySet
#define ARRAY_MAP Boolean2ObjectArrayMap
#define LINKED_OPEN_HASH_SET BooleanLinkedOpenHashSet
#define AVL_TREE_SET BooleanAVLTreeSet
#define RB_TREE_SET BooleanRBTreeSet
#define AVL_TREE_MAP Boolean2ObjectAVLTreeMap
#define RB_TREE_MAP Boolean2ObjectRBTreeMap
#define ARRAY_LIST BooleanArrayList
#define IMMUTABLE_LIST BooleanImmutableList
#define BIG_ARRAY_BIG_LIST BooleanBigArrayBigList
#define MAPPED_BIG_LIST BooleanMappedBigList
#define ARRAY_FRONT_CODED_LIST BooleanArrayFrontCodedList
#define ARRAY_FRONT_CODED_BIG_LIST BooleanArrayFrontCodedBigList
#define HEAP_PRIORITY_QUEUE BooleanHeapPriorityQueue
#define HEAP_SEMI_INDIRECT_PRIORITY_QUEUE BooleanHeapSemiIndirectPriorityQueue
#define HEAP_INDIRECT_PRIORITY_QUEUE BooleanHeapIndirectPriorityQueue
#define HEAP_SESQUI_INDIRECT_DOUBLE_PRIORITY_QUEUE BooleanHeapSesquiIndirectDoublePriorityQueue
#define HEAP_INDIRECT_DOUBLE_PRIORITY_QUEUE BooleanHeapIndirectDoublePriorityQueue
#define ARRAY_FIFO_QUEUE BooleanArrayFIFOQueue
#define ARRAY_PRIORITY_QUEUE BooleanArrayPriorityQueue
#define ARRAY_INDIRECT_PRIORITY_QUEUE BooleanArrayIndirectPriorityQueue
#define ARRAY_INDIRECT_DOUBLE_PRIORITY_QUEUE BooleanArrayIndirectDoublePriorityQueue
#define KEY_BUFFER BooleanBuffer
/* Synchronized wrappers */
#define SYNCHRONIZED_COLLECTION SynchronizedBooleanCollection
#define SYNCHRONIZED_SET SynchronizedBooleanSet
#define SYNCHRONIZED_SORTED_SET SynchronizedBooleanSortedSet
#define SYNCHRONIZED_FUNCTION SynchronizedBoolean2ObjectFunction
#define SYNCHRONIZED_MAP SynchronizedBoolean2ObjectMap
#define SYNCHRONIZED_LIST SynchronizedBooleanList
/* Unmodifiable wrappers */
#define UNMODIFIABLE_COLLECTION UnmodifiableBooleanCollection
#define UNMODIFIABLE_SET UnmodifiableBooleanSet
#define UNMODIFIABLE_SORTED_SET UnmodifiableBooleanSortedSet
#define UNMODIFIABLE_FUNCTION UnmodifiableBoolean2ObjectFunction
#define UNMODIFIABLE_MAP UnmodifiableBoolean2ObjectMap
#define UNMODIFIABLE_LIST UnmodifiableBooleanList
#define UNMODIFIABLE_KEY_ITERATOR UnmodifiableBooleanIterator
#define UNMODIFIABLE_KEY_BIDI_ITERATOR UnmodifiableBooleanBidirectionalIterator
#define UNMODIFIABLE_KEY_LIST_ITERATOR UnmodifiableBooleanListIterator
/* Other wrappers */
#define KEY_READER_WRAPPER BooleanReaderWrapper
#define KEY_DATA_INPUT_WRAPPER BooleanDataInputWrapper
#define KEY_DATA_NIO_INPUT_WRAPPER BooleanDataNioInputWrapper
/* Methods (keys) */
#define NEXT_KEY nextBoolean
#define PREV_KEY previousBoolean
#define NEXT_KEY_WIDENED nextBoolean
#define PREV_KEY_WIDENED previousBoolean
#define KEY_WIDENED_ITERATOR_METHOD booleanIterator
#define KEY_WIDENED_SPLITERATOR_METHOD booleanSpliterator
#define KEY_WIDENED_STREAM_METHOD booleanStream
#define KEY_WIDENED_PARALLEL_STREAM_METHOD booleanParallelStream
#define FIRST_KEY firstBooleanKey
#define LAST_KEY lastBooleanKey
#define GET_KEY getBoolean
#define AS_KEY_BUFFER asBooleanBuffer
#define PAIR_LEFT leftBoolean
#define PAIR_FIRST firstBoolean
#define PAIR_KEY keyBoolean
#define REMOVE_KEY removeBoolean
#define READ_KEY readBoolean
#define WRITE_KEY writeBoolean
#define DEQUEUE dequeueBoolean
#define DEQUEUE_LAST dequeueLastBoolean
#define SINGLETON_METHOD booleanSingleton
#define FIRST firstBoolean
#define LAST lastBoolean
#define TOP topBoolean
#define PEEK peekBoolean
#define POP popBoolean
#define KEY_EMPTY_ITERATOR_METHOD emptyBooleanIterator
#define KEY_EMPTY_SPLITERATOR_METHOD emptyBooleanSpliterator
#define AS_KEY_ITERATOR asBooleanIterator
#define AS_KEY_SPLITERATOR asBooleanSpliterator
#define AS_KEY_COMPARATOR asBooleanComparator
#define AS_KEY_ITERABLE asBooleanIterable
#define AS_KEY_WIDENED_ITERATOR asBooleanIterator
#define AS_KEY_WIDENED_SPLITERATOR asBooleanSpliterator
#define TO_KEY_ARRAY toBooleanArray
#define ENTRY_GET_KEY getBooleanKey
#define REMOVE_FIRST_KEY removeFirstBoolean
#define REMOVE_LAST_KEY removeLastBoolean
#define PARSE_KEY parseBoolean
#define LOAD_KEYS loadBooleans
#define LOAD_KEYS_BIG loadBooleansBig
#define STORE_KEYS storeBooleans
#if KEYS_REFERENCE
#define MAP_TO_KEY map
#define MAP_TO_KEY_WIDENED map
#define RETURN_FALSE_IF_KEY_NULL(k) if (k == null) return false;
#define REQUIRE_KEY_NON_NULL(k) java.util.Objects.requireNonNull(k);
#else
#define MAP_TO_KEY mapToBoolean
#define MAP_TO_KEY_WIDENED mapToBoolean
#define REQUIRE_KEY_NON_NULL(k)
#define RETURN_FALSE_IF_KEY_NULL(k)
#endif
/* Methods (values) */
#define MERGE_VALUE merge
#define NEXT_VALUE next
#define PREV_VALUE previous
#define READ_VALUE readObject
#define WRITE_VALUE writeObject
#define ENTRY_GET_VALUE getValue
#define REMOVE_FIRST_VALUE removeFirst
#define REMOVE_LAST_VALUE removeLast
#define AS_VALUE_ITERATOR asObjectIterator
#define AS_VALUE_SPLITERATOR asObjectSpliterator
#define PAIR_RIGHT right
#define PAIR_SECOND second
#define PAIR_VALUE value
#if VALUES_REFERENCE
#define REQUIRE_VALUE_NON_NULL(v) java.util.Objects.requireNonNull(v);
#else
#define REQUIRE_VALUE_NON_NULL(v)
#endif
/* Methods (keys/values) */
#define ENTRYSET boolean2ObjectEntrySet
/* Methods that have special names depending on keys (but the special names depend on values) */
#if KEYS_REFERENCE
#define GET_VALUE get
#define REMOVE_VALUE remove
#define COMPUTE_IF_ABSENT_JDK computeObjectIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeObjectIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeObjectIfAbsentPartial
#define COMPUTE compute
#define COMPUTE_IF_PRESENT computeIfPresent
#define MERGE merge
#else
#define GET_VALUE get
#define REMOVE_VALUE remove
#define COMPUTE_IF_ABSENT_JDK computeIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeIfAbsentPartial
#define COMPUTE compute
#define COMPUTE_IF_PRESENT computeIfPresent
#endif
/* Equality */
#define KEY_EQUALS_NOT_NULL_CAST(x,y) KEY_EQUALS_NOT_NULL(x,y)
#define KEY2INTHASH_CAST(x) KEY2INTHASH(x)
#if KEY_CLASS_Object
#define KEY_EQUALS(x,y) java.util.Objects.equals(x, y)
#define KEY_EQUALS_NOT_NULL(x,y) ( (x).equals(y) )
#define KEY_IS_NULL(x) ( (x) == null )
#elif KEY_CLASS_Float
#define KEY_EQUALS(x,y) ( Float.floatToIntBits(x) == Float.floatToIntBits(y) )
#define KEY_EQUALS_NOT_NULL(x,y) ( Float.floatToIntBits(x) == Float.floatToIntBits(y) )
#define KEY_IS_NULL(x) ( Float.floatToIntBits(x) == 0 )
#elif KEY_CLASS_Double
#define KEY_EQUALS(x,y) ( Double.doubleToLongBits(x) == Double.doubleToLongBits(y) )
#define KEY_EQUALS_NOT_NULL(x,y) ( Double.doubleToLongBits(x) == Double.doubleToLongBits(y) )
#define KEY_IS_NULL(x) ( Double.doubleToLongBits(x) == 0 )
#else
#define KEY_EQUALS(x,y) ( (x) == (y) )
#define KEY_EQUALS_NOT_NULL(x,y) ( (x) == (y) )
#define KEY_IS_NULL(x) ( (x) == KEY_NULL )
#endif
#ifdef Custom
#undef KEY_EQUALS
#define KEY_EQUALS(x,y) ( strategy.equals( (x), (y) ) )
#undef KEY_EQUALS_NOT_NULL
#define KEY_EQUALS_NOT_NULL(x,y) ( strategy.equals( (x), (y) ) )
#undef KEY_EQUALS_NOT_NULL_CAST
#define KEY_EQUALS_NOT_NULL_CAST(x,y) ( strategy.equals(  KEY_GENERIC_CAST (x), (y) ) )
#define KEY_EQUALS_NULL(x) ( strategy.equals( (x), KEY_NULL ) )
#else
#define KEY_EQUALS_NULL(x) KEY_IS_NULL(x)
#endif
#define VALUE_EQUALS_NOT_NULL_CAST(x,y) VALUE_EQUALS_NOT_NULL(x,y)
#define VALUE2INTHASH_CAST(x) VALUE2INTHASH(x)
#if VALUE_CLASS_Object
#define VALUE_EQUALS(x,y) java.util.Objects.equals(x, y)
#define VALUE_EQUALS_NOT_NULL(x,y) ( (x).equals(y) )
#define VALUE_IS_NULL(x) ( (x) == null )
#elif VALUE_CLASS_Float
#define VALUE_EQUALS(x,y) ( Float.floatToIntBits(x) == Float.floatToIntBits(y) )
#define VALUE_EQUALS_NOT_NULL(x,y) ( Float.floatToIntBits(x) == Float.floatToIntBits(y) )
#define VALUE_IS_NULL(x) ( Float.floatToIntBits(x) == 0 )
#elif VALUE_CLASS_Double
#define VALUE_EQUALS(x,y) ( Double.doubleToLongBits(x) == Double.doubleToLongBits(y) )
#define VALUE_EQUALS_NOT_NULL(x,y) ( Double.doubleToLongBits(x) == Double.doubleToLongBits(y) )
#define VALUE_IS_NULL(x) ( Double.doubleToLongBits(x) == 0 )
#else
#define VALUE_EQUALS(x,y) ( (x) == (y) )
#define VALUE_EQUALS_NOT_NULL(x,y) ( (x) == (y) )
#define VALUE_IS_NULL(x) ( (x) == VALUE_NULL )
#endif
/* Object/Reference-only definitions (keys) */
#if KEYS_REFERENCE
#define REMOVE remove
#define KEY_OBJ2TYPE(x) (x)
#define KEY_CLASS2TYPE(x) (x)
#define KEY2OBJ(x) (x)
#ifdef Custom
#define KEY2JAVAHASH_NOT_NULL(x) ( strategy.hashCode(x) )
#define KEY2INTHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( strategy.hashCode(x) ) )
#undef KEY2INTHASH_CAST
#define KEY2INTHASH_CAST(x) ( it.unimi.dsi.fastutil.HashCommon.mix( strategy.hashCode(  KEY_GENERIC_CAST  x) ) )
#define KEY2LONGHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (long)( strategy.hashCode(x)) ) ) )
#elif KEY_CLASS_Object
#define KEY2JAVAHASH_NOT_NULL(x) ( (x).hashCode() )
#define KEY2JAVAHASH(x) ( (x) == null ? 0 : (x).hashCode() )
#define KEY2INTHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (x).hashCode() ) )
#define KEY2LONGHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (long)( (x).hashCode() ) ) )
#else
#define KEY2JAVAHASH_NOT_NULL(x) ( System.identityHashCode(x) )
#define KEY2INTHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( System.identityHashCode(x) ) )
#define KEY2LONGHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (long)( System.identityHashCode(x) ) ) )
#endif
#define KEY_CMP(x,y) ( ((Comparable<KEY_GENERIC_CLASS>)(x)).compareTo(y) )
#define KEY_CMP_EQ(x,y) ( ((Comparable<KEY_GENERIC_CLASS>)(x)).compareTo(y) == 0 )
#define KEY_LESS(x,y) ( ((Comparable<KEY_GENERIC_CLASS>)(x)).compareTo(y) < 0 )
#define KEY_LESSEQ(x,y) ( ((Comparable<KEY_GENERIC_CLASS>)(x)).compareTo(y) <= 0 )
#define KEY_NULL (null)
#else
/* Primitive-type-only definitions (keys) */
#define REMOVE rem
#define KEY_CLASS2TYPE(x) (x).KEY_VALUE()
#define KEY_OBJ2TYPE(x) KEY_CLASS2TYPE((KEY_CLASS)(x))
#define KEY2OBJ(x) KEY_CLASS.valueOf(x)
#if KEY_CLASS_Boolean
#define KEY_CMP_EQ(x,y) ( (x) == (y) )
#define KEY_NULL (false)
#define KEY_CMP(x,y) ( KEY_CLASS.compare((x),(y)) )
#define KEY_LESS(x,y) ( !(x) && (y) )
#define KEY_LESSEQ(x,y) ( !(x) || (y) )
#else
#if KEY_CLASS_Byte || KEY_CLASS_Short || KEY_CLASS_Character
#define KEY_NULL ((KEY_TYPE)0)
#else
#define KEY_NULL (0)
#endif
#if KEY_CLASS_Float || KEY_CLASS_Double
#define KEY_CMP_EQ(x,y) ( KEY_CLASS.compare((x),(y)) == 0 )
#define KEY_CMP(x,y) ( KEY_CLASS.compare((x),(y)) )
#define KEY_LESS(x,y) ( KEY_CLASS.compare((x),(y)) < 0 )
#define KEY_LESSEQ(x,y) ( KEY_CLASS.compare((x),(y)) <= 0 )
#else
#define KEY_CMP_EQ(x,y) ( (x) == (y) )
#define KEY_CMP(x,y) ( KEY_CLASS.compare((x),(y)) )
#define KEY_LESS(x,y) ( (x) < (y) )
#define KEY_LESSEQ(x,y) ( (x) <= (y) )
#endif
#if KEY_CLASS_Float
#define KEY2LEXINT(x) fixFloat(x)
#elif KEY_CLASS_Double
#define KEY2LEXINT(x) fixDouble(x)
#else
#define KEY2LEXINT(x) (x)
#endif
#endif
#ifdef Custom
#define KEY2JAVAHASH_NOT_NULL(x) ( strategy.hashCode(x) )
#define KEY2INTHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( strategy.hashCode(x) ) )
#define KEY2LONGHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (long)( strategy.hashCode(x) ) ) )
#else
#if KEY_CLASS_Float
#define KEY2JAVAHASH_NOT_NULL(x) it.unimi.dsi.fastutil.HashCommon.float2int(x)
#define KEY2INTHASH(x) it.unimi.dsi.fastutil.HashCommon.mix( it.unimi.dsi.fastutil.HashCommon.float2int(x) )
#define KEY2LONGHASH(x) it.unimi.dsi.fastutil.HashCommon.mix( (long)( it.unimi.dsi.fastutil.HashCommon.float2int(x) ) )
#define INT(x) (x)
#elif KEY_CLASS_Double
#define KEY2JAVAHASH_NOT_NULL(x) it.unimi.dsi.fastutil.HashCommon.double2int(x)
#define KEY2INTHASH(x) (int)it.unimi.dsi.fastutil.HashCommon.mix( Double.doubleToRawLongBits(x) )
#define KEY2LONGHASH(x) it.unimi.dsi.fastutil.HashCommon.mix( Double.doubleToRawLongBits(x) )
#define INT(x) (int)(x)
#elif KEY_CLASS_Long
#define KEY2JAVAHASH_NOT_NULL(x) it.unimi.dsi.fastutil.HashCommon.long2int(x)
#define KEY2INTHASH(x) (int)it.unimi.dsi.fastutil.HashCommon.mix( (x) )
#define KEY2LONGHASH(x) it.unimi.dsi.fastutil.HashCommon.mix( (x) )
#define INT(x) (int)(x)
#elif KEY_CLASS_Boolean
#define KEY2JAVAHASH_NOT_NULL(x) ((x) ? 1231 : 1237)
#define KEY2INTHASH(x) ((x) ? 0xfab5368 : 0xcba05e7b)
#define KEY2LONGHASH(x) ((x) ? 0x74a19fc8b6428188L : 0xbaeca2031a4fd9ecL)
#else
#define KEY2JAVAHASH_NOT_NULL(x) (x)
#define KEY2INTHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (x) ) )
#define KEY2LONGHASH(x) ( it.unimi.dsi.fastutil.HashCommon.mix( (long)( (x) ) ) )
#define INT(x) (x)
#endif
#endif
#endif
#ifndef KEY2JAVAHASH
#define KEY2JAVAHASH(x) KEY2JAVAHASH_NOT_NULL(x)
#endif
/* Object/Reference-only definitions (values) */
#if VALUES_REFERENCE
#define VALUE_OBJ2TYPE(x) (x)
#define VALUE_CLASS2TYPE(x) (x)
#define VALUE2OBJ(x) (x)
#if VALUE_CLASS_Object
#define VALUE2JAVAHASH(x) ( (x) == null ? 0 : (x).hashCode() )
#else
#define VALUE2JAVAHASH(x) ( (x) == null ? 0 : System.identityHashCode(x) )
#endif
#define VALUE_NULL (null)
#else
/* Primitive-type-only definitions (values) */
#define VALUE_CLASS2TYPE(x) (x).VALUE_VALUE()
#define VALUE_OBJ2TYPE(x) VALUE_CLASS2TYPE((VALUE_CLASS)(x))
#define VALUE2OBJ(x) VALUE_CLASS.valueOf(x)
#if VALUE_CLASS_Float || VALUE_CLASS_Double || VALUE_CLASS_Long
#define VALUE_NULL (0)
#define VALUE2JAVAHASH(x) it.unimi.dsi.fastutil.HashCommon.Object2int(x)
#elif VALUE_CLASS_Boolean
#define VALUE_NULL (false)
#define VALUE2JAVAHASH(x) (x ? 1231 : 1237)
#else
#if VALUE_CLASS_Integer
#define VALUE_NULL (0)
#else
#define VALUE_NULL ((VALUE_TYPE)0)
#endif
#define VALUE2JAVAHASH(x) (x)
#endif
#endif
/* START_OF_JAVA_SOURCE */
#include "drv/BidirectionalIterator.drv"

