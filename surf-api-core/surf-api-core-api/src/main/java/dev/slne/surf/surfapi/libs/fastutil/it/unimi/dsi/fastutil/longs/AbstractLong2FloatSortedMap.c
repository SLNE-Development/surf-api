/* Generic definitions */


#define package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.longs
#define VALUE_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.floats
#define WIDENED_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.longs
/* Assertions (useful to generate conditional code) */
#define KEY_CLASS_Long 1
 #define KEYS_PRIMITIVE 1
 #define KEYS_INT_LONG_DOUBLE 1
#define VALUE_CLASS_Float 1
 #define VALUES_PRIMITIVE 1
 #define VALUES_BYTE_CHAR_SHORT_FLOAT 1
/* Narrowing and widening */
#define KEY_NARROWING(x) x
#define KEY_LONG_NARROWING(x) x
#define VALUE_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeDoubleToFloat(x)
/* Current type and class (and size, if applicable) */
#define KEY_TYPE long
#define KEY_TYPE_CAP Long
#define VALUE_TYPE float
#define VALUE_TYPE_CAP Float
#define KEY_INDEX 4
#define KEY_TYPE_WIDENED long
#define VALUE_TYPE_WIDENED double
#define KEY_CLASS Long
#define VALUE_CLASS Float
#define VALUE_INDEX 6
#define KEY_CLASS_WIDENED Long
#define VALUE_CLASS_WIDENED Double
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
#define KEY_VALUE longValue
#define KEY_WIDENED_VALUE longValue
#define VALUE_VALUE floatValue
#define VALUE_WIDENED_VALUE doubleValue
/* Interfaces (keys) */
#define COLLECTION LongCollection
#define STD_KEY_COLLECTION LongCollection
#define SET LongSet
#define HASH LongHash
#define SORTED_SET LongSortedSet
#define STD_SORTED_SET LongSortedSet
#define FUNCTION Long2FloatFunction
#define MAP Long2FloatMap
#define SORTED_MAP Long2FloatSortedMap
#if KEY_CLASS_Object && VALUE_CLASS_Object
#define PAIR it.unimi.dsi.fastutil.Pair
#define SORTED_PAIR it.unimi.dsi.fastutil.SortedPair
#else
#define PAIR LongFloatPair
#define SORTED_PAIR LongFloatSortedPair
#endif
#define MUTABLE_PAIR LongFloatMutablePair
#define IMMUTABLE_PAIR LongFloatImmutablePair
#define IMMUTABLE_SORTED_PAIR LongLongImmutableSortedPair
#if KEYS_REFERENCE
#define STD_SORTED_MAP SortedMap
#define STRATEGY Strategy
#else
#define STD_SORTED_MAP Long2FloatSortedMap
#define STRATEGY PACKAGE.LongHash.Strategy
#endif
#define LIST LongList
#define BIG_LIST LongBigList
#define STACK LongStack
#define ATOMIC_ARRAY AtomicLongArray
#define PRIORITY_QUEUE LongPriorityQueue
#define INDIRECT_PRIORITY_QUEUE LongIndirectPriorityQueue
#define INDIRECT_DOUBLE_PRIORITY_QUEUE LongIndirectDoublePriorityQueue
#define KEY_CONSUMER LongConsumer
#define KEY_PREDICATE LongPredicate
#define KEY_UNARY_OPERATOR LongUnaryOperator
#define KEY_BINARY_OPERATOR LongBinaryOperator
#define KEY_ITERATOR LongIterator
#define KEY_WIDENED_ITERATOR LongIterator
#define KEY_ITERABLE LongIterable
#define KEY_SPLITERATOR LongSpliterator
#define KEY_WIDENED_SPLITERATOR LongSpliterator
#define KEY_BIDI_ITERATOR LongBidirectionalIterator
#define KEY_BIDI_ITERABLE LongBidirectionalIterable
#define KEY_LIST_ITERATOR LongListIterator
#define KEY_BIG_LIST_ITERATOR LongBigListIterator
#define STD_KEY_ITERATOR LongIterator
#define STD_KEY_SPLITERATOR LongSpliterator
#define STD_KEY_ITERABLE LongIterable
#define KEY_COMPARATOR LongComparator
/* Interfaces (values) */
#define VALUE_COLLECTION FloatCollection
#define VALUE_ARRAY_SET FloatArraySet
#define VALUE_CONSUMER FloatConsumer
#define VALUE_BINARY_OPERATOR FloatBinaryOperator
#define VALUE_ITERATOR FloatIterator
#define VALUE_SPLITERATOR FloatSpliterator
#define VALUE_LIST_ITERATOR FloatListIterator
/* Types and methods related to primitive-type support in the JDK */
#if KEYS_PRIMITIVE && ! KEY_CLASS_Boolean
#define JDK_PRIMITIVE_KEY_CONSUMER java.util.function.LongConsumer
#define JDK_PRIMITIVE_PREDICATE java.util.function.LongPredicate
#define JDK_PRIMITIVE_BINARY_OPERATOR java.util.function.LongBinaryOperator
#define JDK_PRIMITIVE_BINARY_OPERATOR_APPLY applyAsLong
#define JDK_PRIMITIVE_ITERATOR PrimitiveIterator.OfLong
#define JDK_PRIMITIVE_SPLITERATOR Spliterator.OfLong
#define JDK_PRIMITIVE_STREAM java.util.stream.LongStream
#define JDK_PRIMITIVE_UNARY_OPERATOR java.util.function.LongUnaryOperator
#define JDK_PRIMITIVE_KEY_APPLY applyAsLong
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.LongFunction
#else
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.Function
#endif
#if VALUES_PRIMITIVE && ! VALUE_CLASS_Boolean
#define JDK_PRIMITIVE_VALUE_CONSUMER java.util.function.DoubleConsumer
#define JDK_PRIMITIVE_VALUE_BINARY_OPERATOR java.util.function.DoubleBinaryOperator
#define JDK_PRIMITIVE_VALUE_OPERATOR_APPLY applyAsDouble
#endif
#define JDK_PRIMITIVE_FUNCTION java.util.function.LongToDoubleFunction
 #define JDK_PRIMITIVE_FUNCTION_APPLY applyAsDouble
#if KEYS_INT_LONG_DOUBLE
#define METHOD_ARG_KEY_CONSUMER JDK_PRIMITIVE_KEY_CONSUMER
#define METHOD_ARG_PREDICATE JDK_PRIMITIVE_PREDICATE
#define METHOD_ARG_KEY_UNARY_OPERATOR JDK_PRIMITIVE_UNARY_OPERATOR
#define METHOD_ARG_KEY_BINARY_OPERATOR JDK_PRIMITIVE_BINARY_OPERATOR
#define KEY_OPERATOR_APPLY applyAsLong
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
#define VALUE_OPERATOR_APPLY applyAsFloat
#else
#define METHOD_ARG_VALUE_CONSUMER VALUE_CONSUMER VALUE_SUPER_GENERIC
#define METHOD_ARG_VALUE_BINARY_OPERATOR VALUE_PACKAGE.VALUE_BINARY_OPERATOR VALUE_GENERIC
#define VALUE_OPERATOR_APPLY apply
#endif
/* Abstract implementations (keys) */
#define ABSTRACT_COLLECTION AbstractLongCollection
#define ABSTRACT_SET AbstractLongSet
#define ABSTRACT_SORTED_SET AbstractLongSortedSet
#define ABSTRACT_FUNCTION AbstractLong2FloatFunction
#define ABSTRACT_MAP AbstractLong2FloatMap
#define ABSTRACT_FUNCTION AbstractLong2FloatFunction
#define ABSTRACT_SORTED_MAP AbstractLong2FloatSortedMap
#define ABSTRACT_LIST AbstractLongList
#define ABSTRACT_BIG_LIST AbstractLongBigList
#define SUBLIST LongSubList
#define SUBLIST_RANDOM_ACCESS LongRandomAccessSubList
#define ABSTRACT_PRIORITY_QUEUE AbstractLongPriorityQueue
#define ABSTRACT_STACK AbstractLongStack
#define KEY_ABSTRACT_ITERATOR AbstractLongIterator
#define KEY_ABSTRACT_SPLITERATOR AbstractLongSpliterator
#define KEY_ABSTRACT_BIDI_ITERATOR AbstractLongBidirectionalIterator
#define KEY_ABSTRACT_LIST_ITERATOR AbstractLongListIterator
#define KEY_ABSTRACT_BIG_LIST_ITERATOR AbstractLongBigListIterator
#if KEY_CLASS_Object
#define KEY_ABSTRACT_COMPARATOR Comparator
#else
#define KEY_ABSTRACT_COMPARATOR AbstractLongComparator
#endif
/* Abstract implementations (values) */
#define VALUE_ABSTRACT_COLLECTION AbstractFloatCollection
#define VALUE_ABSTRACT_ITERATOR AbstractFloatIterator
#define VALUE_ABSTRACT_BIDI_ITERATOR AbstractFloatBidirectionalIterator
/* Static containers (keys) */
#define COLLECTIONS LongCollections
#define SETS LongSets
#define SORTED_SETS LongSortedSets
#define LISTS LongLists
#define BIG_LISTS LongBigLists
#define MAPS Long2FloatMaps
#define FUNCTIONS Long2FloatFunctions
#define SORTED_MAPS Long2FloatSortedMaps
#define PRIORITY_QUEUES LongPriorityQueues
#define HEAPS LongHeaps
#define SEMI_INDIRECT_HEAPS LongSemiIndirectHeaps
#define INDIRECT_HEAPS LongIndirectHeaps
#define ARRAYS LongArrays
#define BIG_ARRAYS LongBigArrays
#define ITERABLES LongIterables
#define ITERATORS LongIterators
#define WIDENED_ITERATORS LongIterators
#define SPLITERATORS LongSpliterators
#define WIDENED_SPLITERATORS LongSpliterators
#define BIG_LIST_ITERATORS LongBigListIterators
#define BIG_SPLITERATORS LongBigSpliterators
#define COMPARATORS LongComparators
/* Static containers (values) */
#define VALUE_COLLECTIONS FloatCollections
#define VALUE_SETS FloatSets
#define VALUE_ARRAYS FloatArrays
#define VALUE_ITERATORS FloatIterators
#define VALUE_SPLITERATORS FloatSpliterators
/* Implementations */
#define OPEN_HASH_SET LongOpenHashSet
#define OPEN_HASH_BIG_SET LongOpenHashBigSet
#define OPEN_DOUBLE_HASH_SET LongOpenDoubleHashSet
#define OPEN_HASH_MAP Long2FloatOpenHashMap
#define OPEN_HASH_BIG_MAP Long2FloatOpenHashBigMap
#define STRIPED_OPEN_HASH_MAP StripedLong2FloatOpenHashMap
#define OPEN_DOUBLE_HASH_MAP Long2FloatOpenDoubleHashMap
#define ARRAY_SET LongArraySet
#define ARRAY_MAP Long2FloatArrayMap
#define LINKED_OPEN_HASH_SET LongLinkedOpenHashSet
#define AVL_TREE_SET LongAVLTreeSet
#define RB_TREE_SET LongRBTreeSet
#define AVL_TREE_MAP Long2FloatAVLTreeMap
#define RB_TREE_MAP Long2FloatRBTreeMap
#define ARRAY_LIST LongArrayList
#define IMMUTABLE_LIST LongImmutableList
#define BIG_ARRAY_BIG_LIST LongBigArrayBigList
#define MAPPED_BIG_LIST LongMappedBigList
#define ARRAY_FRONT_CODED_LIST LongArrayFrontCodedList
#define ARRAY_FRONT_CODED_BIG_LIST LongArrayFrontCodedBigList
#define HEAP_PRIORITY_QUEUE LongHeapPriorityQueue
#define HEAP_SEMI_INDIRECT_PRIORITY_QUEUE LongHeapSemiIndirectPriorityQueue
#define HEAP_INDIRECT_PRIORITY_QUEUE LongHeapIndirectPriorityQueue
#define HEAP_SESQUI_INDIRECT_DOUBLE_PRIORITY_QUEUE LongHeapSesquiIndirectDoublePriorityQueue
#define HEAP_INDIRECT_DOUBLE_PRIORITY_QUEUE LongHeapIndirectDoublePriorityQueue
#define ARRAY_FIFO_QUEUE LongArrayFIFOQueue
#define ARRAY_PRIORITY_QUEUE LongArrayPriorityQueue
#define ARRAY_INDIRECT_PRIORITY_QUEUE LongArrayIndirectPriorityQueue
#define ARRAY_INDIRECT_DOUBLE_PRIORITY_QUEUE LongArrayIndirectDoublePriorityQueue
#define KEY_BUFFER LongBuffer
/* Synchronized wrappers */
#define SYNCHRONIZED_COLLECTION SynchronizedLongCollection
#define SYNCHRONIZED_SET SynchronizedLongSet
#define SYNCHRONIZED_SORTED_SET SynchronizedLongSortedSet
#define SYNCHRONIZED_FUNCTION SynchronizedLong2FloatFunction
#define SYNCHRONIZED_MAP SynchronizedLong2FloatMap
#define SYNCHRONIZED_LIST SynchronizedLongList
/* Unmodifiable wrappers */
#define UNMODIFIABLE_COLLECTION UnmodifiableLongCollection
#define UNMODIFIABLE_SET UnmodifiableLongSet
#define UNMODIFIABLE_SORTED_SET UnmodifiableLongSortedSet
#define UNMODIFIABLE_FUNCTION UnmodifiableLong2FloatFunction
#define UNMODIFIABLE_MAP UnmodifiableLong2FloatMap
#define UNMODIFIABLE_LIST UnmodifiableLongList
#define UNMODIFIABLE_KEY_ITERATOR UnmodifiableLongIterator
#define UNMODIFIABLE_KEY_BIDI_ITERATOR UnmodifiableLongBidirectionalIterator
#define UNMODIFIABLE_KEY_LIST_ITERATOR UnmodifiableLongListIterator
/* Other wrappers */
#define KEY_READER_WRAPPER LongReaderWrapper
#define KEY_DATA_INPUT_WRAPPER LongDataInputWrapper
#define KEY_DATA_NIO_INPUT_WRAPPER LongDataNioInputWrapper
/* Methods (keys) */
#define NEXT_KEY nextLong
#define PREV_KEY previousLong
#define NEXT_KEY_WIDENED nextLong
#define PREV_KEY_WIDENED previousLong
#define KEY_WIDENED_ITERATOR_METHOD longIterator
#define KEY_WIDENED_SPLITERATOR_METHOD longSpliterator
#define KEY_WIDENED_STREAM_METHOD longStream
#define KEY_WIDENED_PARALLEL_STREAM_METHOD longParallelStream
#define FIRST_KEY firstLongKey
#define LAST_KEY lastLongKey
#define GET_KEY getLong
#define AS_KEY_BUFFER asLongBuffer
#define PAIR_LEFT leftLong
#define PAIR_FIRST firstLong
#define PAIR_KEY keyLong
#define REMOVE_KEY removeLong
#define READ_KEY readLong
#define WRITE_KEY writeLong
#define DEQUEUE dequeueLong
#define DEQUEUE_LAST dequeueLastLong
#define SINGLETON_METHOD longSingleton
#define FIRST firstLong
#define LAST lastLong
#define TOP topLong
#define PEEK peekLong
#define POP popLong
#define KEY_EMPTY_ITERATOR_METHOD emptyLongIterator
#define KEY_EMPTY_SPLITERATOR_METHOD emptyLongSpliterator
#define AS_KEY_ITERATOR asLongIterator
#define AS_KEY_SPLITERATOR asLongSpliterator
#define AS_KEY_COMPARATOR asLongComparator
#define AS_KEY_ITERABLE asLongIterable
#define AS_KEY_WIDENED_ITERATOR asLongIterator
#define AS_KEY_WIDENED_SPLITERATOR asLongSpliterator
#define TO_KEY_ARRAY toLongArray
#define ENTRY_GET_KEY getLongKey
#define REMOVE_FIRST_KEY removeFirstLong
#define REMOVE_LAST_KEY removeLastLong
#define PARSE_KEY parseLong
#define LOAD_KEYS loadLongs
#define LOAD_KEYS_BIG loadLongsBig
#define STORE_KEYS storeLongs
#if KEYS_REFERENCE
#define MAP_TO_KEY map
#define MAP_TO_KEY_WIDENED map
#define RETURN_FALSE_IF_KEY_NULL(k) if (k == null) return false;
#define REQUIRE_KEY_NON_NULL(k) java.util.Objects.requireNonNull(k);
#else
#define MAP_TO_KEY mapToLong
#define MAP_TO_KEY_WIDENED mapToLong
#define REQUIRE_KEY_NON_NULL(k)
#define RETURN_FALSE_IF_KEY_NULL(k)
#endif
/* Methods (values) */
#define MERGE_VALUE mergeFloat
#define NEXT_VALUE nextFloat
#define PREV_VALUE previousFloat
#define READ_VALUE readFloat
#define WRITE_VALUE writeFloat
#define ENTRY_GET_VALUE getFloatValue
#define REMOVE_FIRST_VALUE removeFirstFloat
#define REMOVE_LAST_VALUE removeLastFloat
#define AS_VALUE_ITERATOR asFloatIterator
#define AS_VALUE_SPLITERATOR asFloatSpliterator
#define PAIR_RIGHT rightFloat
#define PAIR_SECOND secondFloat
#define PAIR_VALUE valueFloat
#if VALUES_REFERENCE
#define REQUIRE_VALUE_NON_NULL(v) java.util.Objects.requireNonNull(v);
#else
#define REQUIRE_VALUE_NON_NULL(v)
#endif
/* Methods (keys/values) */
#define ENTRYSET long2FloatEntrySet
/* Methods that have special names depending on keys (but the special names depend on values) */
#if KEYS_REFERENCE
#define GET_VALUE getFloat
#define REMOVE_VALUE removeFloat
#define COMPUTE_IF_ABSENT_JDK computeFloatIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeFloatIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeFloatIfAbsentPartial
#define COMPUTE computeFloat
#define COMPUTE_IF_PRESENT computeFloatIfPresent
#define MERGE mergeFloat
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
#define VALUE2JAVAHASH(x) it.unimi.dsi.fastutil.HashCommon.float2int(x)
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
#include "drv/AbstractSortedMap.drv"

