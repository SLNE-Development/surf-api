/* Generic definitions */


#define package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.doubles
#define VALUE_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.shorts
#define WIDENED_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.doubles
/* Assertions (useful to generate conditional code) */
#define KEY_CLASS_Double 1
 #define KEYS_PRIMITIVE 1
 #define KEYS_INT_LONG_DOUBLE 1
#define VALUE_CLASS_Short 1
 #define VALUES_PRIMITIVE 1
 #define VALUES_BYTE_CHAR_SHORT_FLOAT 1
/* Narrowing and widening */
#define KEY_NARROWING(x) x
#define VALUE_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeIntToShort(x)
/* Current type and class (and size, if applicable) */
#define KEY_TYPE double
#define KEY_TYPE_CAP Double
#define VALUE_TYPE short
#define VALUE_TYPE_CAP Short
#define KEY_INDEX 7
#define KEY_TYPE_WIDENED double
#define VALUE_TYPE_WIDENED int
#define KEY_CLASS Double
#define VALUE_CLASS Short
#define VALUE_INDEX 2
#define KEY_CLASS_WIDENED Double
#define VALUE_CLASS_WIDENED Integer
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
#define KEY_VALUE doubleValue
#define KEY_WIDENED_VALUE doubleValue
#define VALUE_VALUE shortValue
#define VALUE_WIDENED_VALUE intValue
/* Interfaces (keys) */
#define COLLECTION DoubleCollection
#define STD_KEY_COLLECTION DoubleCollection
#define SET DoubleSet
#define HASH DoubleHash
#define SORTED_SET DoubleSortedSet
#define STD_SORTED_SET DoubleSortedSet
#define FUNCTION Double2ShortFunction
#define MAP Double2ShortMap
#define SORTED_MAP Double2ShortSortedMap
#if KEY_CLASS_Object && VALUE_CLASS_Object
#define PAIR it.unimi.dsi.fastutil.Pair
#define SORTED_PAIR it.unimi.dsi.fastutil.SortedPair
#else
#define PAIR DoubleShortPair
#define SORTED_PAIR DoubleShortSortedPair
#endif
#define MUTABLE_PAIR DoubleShortMutablePair
#define IMMUTABLE_PAIR DoubleShortImmutablePair
#define IMMUTABLE_SORTED_PAIR DoubleDoubleImmutableSortedPair
#if KEYS_REFERENCE
#define STD_SORTED_MAP SortedMap
#define STRATEGY Strategy
#else
#define STD_SORTED_MAP Double2ShortSortedMap
#define STRATEGY PACKAGE.DoubleHash.Strategy
#endif
#define LIST DoubleList
#define BIG_LIST DoubleBigList
#define STACK DoubleStack
#define ATOMIC_ARRAY AtomicDoubleArray
#define PRIORITY_QUEUE DoublePriorityQueue
#define INDIRECT_PRIORITY_QUEUE DoubleIndirectPriorityQueue
#define INDIRECT_DOUBLE_PRIORITY_QUEUE DoubleIndirectDoublePriorityQueue
#define KEY_CONSUMER DoubleConsumer
#define KEY_PREDICATE DoublePredicate
#define KEY_UNARY_OPERATOR DoubleUnaryOperator
#define KEY_BINARY_OPERATOR DoubleBinaryOperator
#define KEY_ITERATOR DoubleIterator
#define KEY_WIDENED_ITERATOR DoubleIterator
#define KEY_ITERABLE DoubleIterable
#define KEY_SPLITERATOR DoubleSpliterator
#define KEY_WIDENED_SPLITERATOR DoubleSpliterator
#define KEY_BIDI_ITERATOR DoubleBidirectionalIterator
#define KEY_BIDI_ITERABLE DoubleBidirectionalIterable
#define KEY_LIST_ITERATOR DoubleListIterator
#define KEY_BIG_LIST_ITERATOR DoubleBigListIterator
#define STD_KEY_ITERATOR DoubleIterator
#define STD_KEY_SPLITERATOR DoubleSpliterator
#define STD_KEY_ITERABLE DoubleIterable
#define KEY_COMPARATOR DoubleComparator
/* Interfaces (values) */
#define VALUE_COLLECTION ShortCollection
#define VALUE_ARRAY_SET ShortArraySet
#define VALUE_CONSUMER ShortConsumer
#define VALUE_BINARY_OPERATOR ShortBinaryOperator
#define VALUE_ITERATOR ShortIterator
#define VALUE_SPLITERATOR ShortSpliterator
#define VALUE_LIST_ITERATOR ShortListIterator
/* Types and methods related to primitive-type support in the JDK */
#if KEYS_PRIMITIVE && ! KEY_CLASS_Boolean
#define JDK_PRIMITIVE_KEY_CONSUMER java.util.function.DoubleConsumer
#define JDK_PRIMITIVE_PREDICATE java.util.function.DoublePredicate
#define JDK_PRIMITIVE_BINARY_OPERATOR java.util.function.DoubleBinaryOperator
#define JDK_PRIMITIVE_BINARY_OPERATOR_APPLY applyAsDouble
#define JDK_PRIMITIVE_ITERATOR PrimitiveIterator.OfDouble
#define JDK_PRIMITIVE_SPLITERATOR Spliterator.OfDouble
#define JDK_PRIMITIVE_STREAM java.util.stream.DoubleStream
#define JDK_PRIMITIVE_UNARY_OPERATOR java.util.function.DoubleUnaryOperator
#define JDK_PRIMITIVE_KEY_APPLY applyAsDouble
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.DoubleFunction
#else
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.Function
#endif
#if VALUES_PRIMITIVE && ! VALUE_CLASS_Boolean
#define JDK_PRIMITIVE_VALUE_CONSUMER java.util.function.IntConsumer
#define JDK_PRIMITIVE_VALUE_BINARY_OPERATOR java.util.function.IntBinaryOperator
#define JDK_PRIMITIVE_VALUE_OPERATOR_APPLY applyAsInt
#endif
#define JDK_PRIMITIVE_FUNCTION java.util.function.DoubleToIntFunction
 #define JDK_PRIMITIVE_FUNCTION_APPLY applyAsInt
#if KEYS_INT_LONG_DOUBLE
#define METHOD_ARG_KEY_CONSUMER JDK_PRIMITIVE_KEY_CONSUMER
#define METHOD_ARG_PREDICATE JDK_PRIMITIVE_PREDICATE
#define METHOD_ARG_KEY_UNARY_OPERATOR JDK_PRIMITIVE_UNARY_OPERATOR
#define METHOD_ARG_KEY_BINARY_OPERATOR JDK_PRIMITIVE_BINARY_OPERATOR
#define KEY_OPERATOR_APPLY applyAsDouble
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
#define VALUE_OPERATOR_APPLY applyAsShort
#else
#define METHOD_ARG_VALUE_CONSUMER VALUE_CONSUMER VALUE_SUPER_GENERIC
#define METHOD_ARG_VALUE_BINARY_OPERATOR VALUE_PACKAGE.VALUE_BINARY_OPERATOR VALUE_GENERIC
#define VALUE_OPERATOR_APPLY apply
#endif
/* Abstract implementations (keys) */
#define ABSTRACT_COLLECTION AbstractDoubleCollection
#define ABSTRACT_SET AbstractDoubleSet
#define ABSTRACT_SORTED_SET AbstractDoubleSortedSet
#define ABSTRACT_FUNCTION AbstractDouble2ShortFunction
#define ABSTRACT_MAP AbstractDouble2ShortMap
#define ABSTRACT_FUNCTION AbstractDouble2ShortFunction
#define ABSTRACT_SORTED_MAP AbstractDouble2ShortSortedMap
#define ABSTRACT_LIST AbstractDoubleList
#define ABSTRACT_BIG_LIST AbstractDoubleBigList
#define SUBLIST DoubleSubList
#define SUBLIST_RANDOM_ACCESS DoubleRandomAccessSubList
#define ABSTRACT_PRIORITY_QUEUE AbstractDoublePriorityQueue
#define ABSTRACT_STACK AbstractDoubleStack
#define KEY_ABSTRACT_ITERATOR AbstractDoubleIterator
#define KEY_ABSTRACT_SPLITERATOR AbstractDoubleSpliterator
#define KEY_ABSTRACT_BIDI_ITERATOR AbstractDoubleBidirectionalIterator
#define KEY_ABSTRACT_LIST_ITERATOR AbstractDoubleListIterator
#define KEY_ABSTRACT_BIG_LIST_ITERATOR AbstractDoubleBigListIterator
#if KEY_CLASS_Object
#define KEY_ABSTRACT_COMPARATOR Comparator
#else
#define KEY_ABSTRACT_COMPARATOR AbstractDoubleComparator
#endif
/* Abstract implementations (values) */
#define VALUE_ABSTRACT_COLLECTION AbstractShortCollection
#define VALUE_ABSTRACT_ITERATOR AbstractShortIterator
#define VALUE_ABSTRACT_BIDI_ITERATOR AbstractShortBidirectionalIterator
/* Static containers (keys) */
#define COLLECTIONS DoubleCollections
#define SETS DoubleSets
#define SORTED_SETS DoubleSortedSets
#define LISTS DoubleLists
#define BIG_LISTS DoubleBigLists
#define MAPS Double2ShortMaps
#define FUNCTIONS Double2ShortFunctions
#define SORTED_MAPS Double2ShortSortedMaps
#define PRIORITY_QUEUES DoublePriorityQueues
#define HEAPS DoubleHeaps
#define SEMI_INDIRECT_HEAPS DoubleSemiIndirectHeaps
#define INDIRECT_HEAPS DoubleIndirectHeaps
#define ARRAYS DoubleArrays
#define BIG_ARRAYS DoubleBigArrays
#define ITERABLES DoubleIterables
#define ITERATORS DoubleIterators
#define WIDENED_ITERATORS DoubleIterators
#define SPLITERATORS DoubleSpliterators
#define WIDENED_SPLITERATORS DoubleSpliterators
#define BIG_LIST_ITERATORS DoubleBigListIterators
#define BIG_SPLITERATORS DoubleBigSpliterators
#define COMPARATORS DoubleComparators
/* Static containers (values) */
#define VALUE_COLLECTIONS ShortCollections
#define VALUE_SETS ShortSets
#define VALUE_ARRAYS ShortArrays
#define VALUE_ITERATORS ShortIterators
#define VALUE_SPLITERATORS ShortSpliterators
/* Implementations */
#define OPEN_HASH_SET DoubleOpenHashSet
#define OPEN_HASH_BIG_SET DoubleOpenHashBigSet
#define OPEN_DOUBLE_HASH_SET DoubleOpenDoubleHashSet
#define OPEN_HASH_MAP Double2ShortOpenHashMap
#define OPEN_HASH_BIG_MAP Double2ShortOpenHashBigMap
#define STRIPED_OPEN_HASH_MAP StripedDouble2ShortOpenHashMap
#define OPEN_DOUBLE_HASH_MAP Double2ShortOpenDoubleHashMap
#define ARRAY_SET DoubleArraySet
#define ARRAY_MAP Double2ShortArrayMap
#define LINKED_OPEN_HASH_SET DoubleLinkedOpenHashSet
#define AVL_TREE_SET DoubleAVLTreeSet
#define RB_TREE_SET DoubleRBTreeSet
#define AVL_TREE_MAP Double2ShortAVLTreeMap
#define RB_TREE_MAP Double2ShortRBTreeMap
#define ARRAY_LIST DoubleArrayList
#define IMMUTABLE_LIST DoubleImmutableList
#define BIG_ARRAY_BIG_LIST DoubleBigArrayBigList
#define MAPPED_BIG_LIST DoubleMappedBigList
#define ARRAY_FRONT_CODED_LIST DoubleArrayFrontCodedList
#define ARRAY_FRONT_CODED_BIG_LIST DoubleArrayFrontCodedBigList
#define HEAP_PRIORITY_QUEUE DoubleHeapPriorityQueue
#define HEAP_SEMI_INDIRECT_PRIORITY_QUEUE DoubleHeapSemiIndirectPriorityQueue
#define HEAP_INDIRECT_PRIORITY_QUEUE DoubleHeapIndirectPriorityQueue
#define HEAP_SESQUI_INDIRECT_DOUBLE_PRIORITY_QUEUE DoubleHeapSesquiIndirectDoublePriorityQueue
#define HEAP_INDIRECT_DOUBLE_PRIORITY_QUEUE DoubleHeapIndirectDoublePriorityQueue
#define ARRAY_FIFO_QUEUE DoubleArrayFIFOQueue
#define ARRAY_PRIORITY_QUEUE DoubleArrayPriorityQueue
#define ARRAY_INDIRECT_PRIORITY_QUEUE DoubleArrayIndirectPriorityQueue
#define ARRAY_INDIRECT_DOUBLE_PRIORITY_QUEUE DoubleArrayIndirectDoublePriorityQueue
#define KEY_BUFFER DoubleBuffer
/* Synchronized wrappers */
#define SYNCHRONIZED_COLLECTION SynchronizedDoubleCollection
#define SYNCHRONIZED_SET SynchronizedDoubleSet
#define SYNCHRONIZED_SORTED_SET SynchronizedDoubleSortedSet
#define SYNCHRONIZED_FUNCTION SynchronizedDouble2ShortFunction
#define SYNCHRONIZED_MAP SynchronizedDouble2ShortMap
#define SYNCHRONIZED_LIST SynchronizedDoubleList
/* Unmodifiable wrappers */
#define UNMODIFIABLE_COLLECTION UnmodifiableDoubleCollection
#define UNMODIFIABLE_SET UnmodifiableDoubleSet
#define UNMODIFIABLE_SORTED_SET UnmodifiableDoubleSortedSet
#define UNMODIFIABLE_FUNCTION UnmodifiableDouble2ShortFunction
#define UNMODIFIABLE_MAP UnmodifiableDouble2ShortMap
#define UNMODIFIABLE_LIST UnmodifiableDoubleList
#define UNMODIFIABLE_KEY_ITERATOR UnmodifiableDoubleIterator
#define UNMODIFIABLE_KEY_BIDI_ITERATOR UnmodifiableDoubleBidirectionalIterator
#define UNMODIFIABLE_KEY_LIST_ITERATOR UnmodifiableDoubleListIterator
/* Other wrappers */
#define KEY_READER_WRAPPER DoubleReaderWrapper
#define KEY_DATA_INPUT_WRAPPER DoubleDataInputWrapper
#define KEY_DATA_NIO_INPUT_WRAPPER DoubleDataNioInputWrapper
/* Methods (keys) */
#define NEXT_KEY nextDouble
#define PREV_KEY previousDouble
#define NEXT_KEY_WIDENED nextDouble
#define PREV_KEY_WIDENED previousDouble
#define KEY_WIDENED_ITERATOR_METHOD doubleIterator
#define KEY_WIDENED_SPLITERATOR_METHOD doubleSpliterator
#define KEY_WIDENED_STREAM_METHOD doubleStream
#define KEY_WIDENED_PARALLEL_STREAM_METHOD doubleParallelStream
#define FIRST_KEY firstDoubleKey
#define LAST_KEY lastDoubleKey
#define GET_KEY getDouble
#define AS_KEY_BUFFER asDoubleBuffer
#define PAIR_LEFT leftDouble
#define PAIR_FIRST firstDouble
#define PAIR_KEY keyDouble
#define REMOVE_KEY removeDouble
#define READ_KEY readDouble
#define WRITE_KEY writeDouble
#define DEQUEUE dequeueDouble
#define DEQUEUE_LAST dequeueLastDouble
#define SINGLETON_METHOD doubleSingleton
#define FIRST firstDouble
#define LAST lastDouble
#define TOP topDouble
#define PEEK peekDouble
#define POP popDouble
#define KEY_EMPTY_ITERATOR_METHOD emptyDoubleIterator
#define KEY_EMPTY_SPLITERATOR_METHOD emptyDoubleSpliterator
#define AS_KEY_ITERATOR asDoubleIterator
#define AS_KEY_SPLITERATOR asDoubleSpliterator
#define AS_KEY_COMPARATOR asDoubleComparator
#define AS_KEY_ITERABLE asDoubleIterable
#define AS_KEY_WIDENED_ITERATOR asDoubleIterator
#define AS_KEY_WIDENED_SPLITERATOR asDoubleSpliterator
#define TO_KEY_ARRAY toDoubleArray
#define ENTRY_GET_KEY getDoubleKey
#define REMOVE_FIRST_KEY removeFirstDouble
#define REMOVE_LAST_KEY removeLastDouble
#define PARSE_KEY parseDouble
#define LOAD_KEYS loadDoubles
#define LOAD_KEYS_BIG loadDoublesBig
#define STORE_KEYS storeDoubles
#if KEYS_REFERENCE
#define MAP_TO_KEY map
#define MAP_TO_KEY_WIDENED map
#define RETURN_FALSE_IF_KEY_NULL(k) if (k == null) return false;
#define REQUIRE_KEY_NON_NULL(k) java.util.Objects.requireNonNull(k);
#else
#define MAP_TO_KEY mapToDouble
#define MAP_TO_KEY_WIDENED mapToDouble
#define REQUIRE_KEY_NON_NULL(k)
#define RETURN_FALSE_IF_KEY_NULL(k)
#endif
/* Methods (values) */
#define MERGE_VALUE mergeShort
#define NEXT_VALUE nextShort
#define PREV_VALUE previousShort
#define READ_VALUE readShort
#define WRITE_VALUE writeShort
#define ENTRY_GET_VALUE getShortValue
#define REMOVE_FIRST_VALUE removeFirstShort
#define REMOVE_LAST_VALUE removeLastShort
#define AS_VALUE_ITERATOR asShortIterator
#define AS_VALUE_SPLITERATOR asShortSpliterator
#define PAIR_RIGHT rightShort
#define PAIR_SECOND secondShort
#define PAIR_VALUE valueShort
#if VALUES_REFERENCE
#define REQUIRE_VALUE_NON_NULL(v) java.util.Objects.requireNonNull(v);
#else
#define REQUIRE_VALUE_NON_NULL(v)
#endif
/* Methods (keys/values) */
#define ENTRYSET double2ShortEntrySet
/* Methods that have special names depending on keys (but the special names depend on values) */
#if KEYS_REFERENCE
#define GET_VALUE getShort
#define REMOVE_VALUE removeShort
#define COMPUTE_IF_ABSENT_JDK computeShortIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeShortIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeShortIfAbsentPartial
#define COMPUTE computeShort
#define COMPUTE_IF_PRESENT computeShortIfPresent
#define MERGE mergeShort
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
#define VALUE2JAVAHASH(x) it.unimi.dsi.fastutil.HashCommon.short2int(x)
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
#include "drv/ImmutablePair.drv"

