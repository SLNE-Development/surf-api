/* Generic definitions */


#define package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.objects
#define VALUE_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.longs
#define WIDENED_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.objects
/* Assertions (useful to generate conditional code) */
#define KEY_CLASS_Object 1
 #define KEYS_REFERENCE 1
#define VALUE_CLASS_Long 1
 #define VALUES_PRIMITIVE 1
 #define VALUES_INT_LONG_DOUBLE 1
/* Narrowing and widening */
#define KEY_NARROWING(x) x
#define KEY_LONG_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeLongToObject(x)
#define VALUE_NARROWING(x) x
/* Current type and class (and size, if applicable) */
#define KEY_TYPE Object
#define KEY_TYPE_CAP Object
#define VALUE_TYPE long
#define VALUE_TYPE_CAP Long
#define KEY_INDEX 8
#define KEY_TYPE_WIDENED Object
#define VALUE_TYPE_WIDENED long
#define KEY_CLASS Object
#define VALUE_CLASS Long
#define VALUE_INDEX 4
#define KEY_CLASS_WIDENED Object
#define VALUE_CLASS_WIDENED Long
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
#define KEY_VALUE ObjectValue
#define KEY_WIDENED_VALUE ObjectValue
#define VALUE_VALUE longValue
#define VALUE_WIDENED_VALUE longValue
/* Interfaces (keys) */
#define COLLECTION ObjectCollection
#define STD_KEY_COLLECTION Collection
#define SET ObjectSet
#define HASH ObjectHash
#define SORTED_SET ObjectSortedSet
#define STD_SORTED_SET SortedSet
#define FUNCTION Object2LongFunction
#define MAP Object2LongMap
#define SORTED_MAP Object2LongSortedMap
#if KEY_CLASS_Object && VALUE_CLASS_Object
#define PAIR it.unimi.dsi.fastutil.Pair
#define SORTED_PAIR it.unimi.dsi.fastutil.SortedPair
#else
#define PAIR ObjectLongPair
#define SORTED_PAIR ObjectLongSortedPair
#endif
#define MUTABLE_PAIR ObjectLongMutablePair
#define IMMUTABLE_PAIR ObjectLongImmutablePair
#define IMMUTABLE_SORTED_PAIR ObjectObjectImmutableSortedPair
#if KEYS_REFERENCE
#define STD_SORTED_MAP SortedMap
#define STRATEGY Strategy
#else
#define STD_SORTED_MAP Object2LongSortedMap
#define STRATEGY PACKAGE.ObjectHash.Strategy
#endif
#define LIST ObjectList
#define BIG_LIST ObjectBigList
#define STACK Stack
#define ATOMIC_ARRAY AtomicObjectArray
#define PRIORITY_QUEUE PriorityQueue
#define INDIRECT_PRIORITY_QUEUE IndirectPriorityQueue
#define INDIRECT_DOUBLE_PRIORITY_QUEUE IndirectDoublePriorityQueue
#define KEY_CONSUMER Consumer
#define KEY_PREDICATE Predicate
#define KEY_UNARY_OPERATOR UnaryOperator
#define KEY_BINARY_OPERATOR BinaryOperator
#define KEY_ITERATOR ObjectIterator
#define KEY_WIDENED_ITERATOR ObjectIterator
#define KEY_ITERABLE ObjectIterable
#define KEY_SPLITERATOR ObjectSpliterator
#define KEY_WIDENED_SPLITERATOR ObjectSpliterator
#define KEY_BIDI_ITERATOR ObjectBidirectionalIterator
#define KEY_BIDI_ITERABLE ObjectBidirectionalIterable
#define KEY_LIST_ITERATOR ObjectListIterator
#define KEY_BIG_LIST_ITERATOR ObjectBigListIterator
#define STD_KEY_ITERATOR Iterator
#define STD_KEY_SPLITERATOR Spliterator
#define STD_KEY_ITERABLE Iterable
#define KEY_COMPARATOR Comparator
/* Interfaces (values) */
#define VALUE_COLLECTION LongCollection
#define VALUE_ARRAY_SET LongArraySet
#define VALUE_CONSUMER LongConsumer
#define VALUE_BINARY_OPERATOR LongBinaryOperator
#define VALUE_ITERATOR LongIterator
#define VALUE_SPLITERATOR LongSpliterator
#define VALUE_LIST_ITERATOR LongListIterator
/* Types and methods related to primitive-type support in the JDK */
#if KEYS_PRIMITIVE && ! KEY_CLASS_Boolean
#define JDK_PRIMITIVE_KEY_CONSUMER java.util.function.ObjectConsumer
#define JDK_PRIMITIVE_PREDICATE java.util.function.ObjectPredicate
#define JDK_PRIMITIVE_BINARY_OPERATOR java.util.function.ObjectBinaryOperator
#define JDK_PRIMITIVE_BINARY_OPERATOR_APPLY applyAsObject
#define JDK_PRIMITIVE_ITERATOR PrimitiveIterator.OfObject
#define JDK_PRIMITIVE_SPLITERATOR Spliterator.OfObject
#define JDK_PRIMITIVE_STREAM java.util.stream.ObjectStream
#define JDK_PRIMITIVE_UNARY_OPERATOR java.util.function.ObjectUnaryOperator
#define JDK_PRIMITIVE_KEY_APPLY applyAsObject
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.ObjectFunction
#else
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.Function
#endif
#if VALUES_PRIMITIVE && ! VALUE_CLASS_Boolean
#define JDK_PRIMITIVE_VALUE_CONSUMER java.util.function.LongConsumer
#define JDK_PRIMITIVE_VALUE_BINARY_OPERATOR java.util.function.LongBinaryOperator
#define JDK_PRIMITIVE_VALUE_OPERATOR_APPLY applyAsLong
#endif
#define JDK_PRIMITIVE_FUNCTION java.util.function.ToLongFunction
 #define JDK_PRIMITIVE_FUNCTION_APPLY applyAsLong
#if KEYS_INT_LONG_DOUBLE
#define METHOD_ARG_KEY_CONSUMER JDK_PRIMITIVE_KEY_CONSUMER
#define METHOD_ARG_PREDICATE JDK_PRIMITIVE_PREDICATE
#define METHOD_ARG_KEY_UNARY_OPERATOR JDK_PRIMITIVE_UNARY_OPERATOR
#define METHOD_ARG_KEY_BINARY_OPERATOR JDK_PRIMITIVE_BINARY_OPERATOR
#define KEY_OPERATOR_APPLY applyAsObject
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
#define VALUE_OPERATOR_APPLY applyAsLong
#else
#define METHOD_ARG_VALUE_CONSUMER VALUE_CONSUMER VALUE_SUPER_GENERIC
#define METHOD_ARG_VALUE_BINARY_OPERATOR VALUE_PACKAGE.VALUE_BINARY_OPERATOR VALUE_GENERIC
#define VALUE_OPERATOR_APPLY apply
#endif
/* Abstract implementations (keys) */
#define ABSTRACT_COLLECTION AbstractObjectCollection
#define ABSTRACT_SET AbstractObjectSet
#define ABSTRACT_SORTED_SET AbstractObjectSortedSet
#define ABSTRACT_FUNCTION AbstractObject2LongFunction
#define ABSTRACT_MAP AbstractObject2LongMap
#define ABSTRACT_FUNCTION AbstractObject2LongFunction
#define ABSTRACT_SORTED_MAP AbstractObject2LongSortedMap
#define ABSTRACT_LIST AbstractObjectList
#define ABSTRACT_BIG_LIST AbstractObjectBigList
#define SUBLIST ObjectSubList
#define SUBLIST_RANDOM_ACCESS ObjectRandomAccessSubList
#define ABSTRACT_PRIORITY_QUEUE AbstractPriorityQueue
#define ABSTRACT_STACK AbstractStack
#define KEY_ABSTRACT_ITERATOR AbstractObjectIterator
#define KEY_ABSTRACT_SPLITERATOR AbstractObjectSpliterator
#define KEY_ABSTRACT_BIDI_ITERATOR AbstractObjectBidirectionalIterator
#define KEY_ABSTRACT_LIST_ITERATOR AbstractObjectListIterator
#define KEY_ABSTRACT_BIG_LIST_ITERATOR AbstractObjectBigListIterator
#if KEY_CLASS_Object
#define KEY_ABSTRACT_COMPARATOR Comparator
#else
#define KEY_ABSTRACT_COMPARATOR AbstractObjectComparator
#endif
/* Abstract implementations (values) */
#define VALUE_ABSTRACT_COLLECTION AbstractLongCollection
#define VALUE_ABSTRACT_ITERATOR AbstractLongIterator
#define VALUE_ABSTRACT_BIDI_ITERATOR AbstractLongBidirectionalIterator
/* Static containers (keys) */
#define COLLECTIONS ObjectCollections
#define SETS ObjectSets
#define SORTED_SETS ObjectSortedSets
#define LISTS ObjectLists
#define BIG_LISTS ObjectBigLists
#define MAPS Object2LongMaps
#define FUNCTIONS Object2LongFunctions
#define SORTED_MAPS Object2LongSortedMaps
#define PRIORITY_QUEUES ObjectPriorityQueues
#define HEAPS ObjectHeaps
#define SEMI_INDIRECT_HEAPS ObjectSemiIndirectHeaps
#define INDIRECT_HEAPS ObjectIndirectHeaps
#define ARRAYS ObjectArrays
#define BIG_ARRAYS ObjectBigArrays
#define ITERABLES ObjectIterables
#define ITERATORS ObjectIterators
#define WIDENED_ITERATORS ObjectIterators
#define SPLITERATORS ObjectSpliterators
#define WIDENED_SPLITERATORS ObjectSpliterators
#define BIG_LIST_ITERATORS ObjectBigListIterators
#define BIG_SPLITERATORS ObjectBigSpliterators
#define COMPARATORS ObjectComparators
/* Static containers (values) */
#define VALUE_COLLECTIONS LongCollections
#define VALUE_SETS LongSets
#define VALUE_ARRAYS LongArrays
#define VALUE_ITERATORS LongIterators
#define VALUE_SPLITERATORS LongSpliterators
/* Implementations */
#define OPEN_HASH_SET ObjectOpenHashSet
#define OPEN_HASH_BIG_SET ObjectOpenHashBigSet
#define OPEN_DOUBLE_HASH_SET ObjectOpenDoubleHashSet
#define OPEN_HASH_MAP Object2LongOpenHashMap
#define OPEN_HASH_BIG_MAP Object2LongOpenHashBigMap
#define STRIPED_OPEN_HASH_MAP StripedObject2LongOpenHashMap
#define OPEN_DOUBLE_HASH_MAP Object2LongOpenDoubleHashMap
#define ARRAY_SET ObjectArraySet
#define ARRAY_MAP Object2LongArrayMap
#define LINKED_OPEN_HASH_SET ObjectLinkedOpenHashSet
#define AVL_TREE_SET ObjectAVLTreeSet
#define RB_TREE_SET ObjectRBTreeSet
#define AVL_TREE_MAP Object2LongAVLTreeMap
#define RB_TREE_MAP Object2LongRBTreeMap
#define ARRAY_LIST ObjectArrayList
#define IMMUTABLE_LIST ObjectImmutableList
#define BIG_ARRAY_BIG_LIST ObjectBigArrayBigList
#define MAPPED_BIG_LIST ObjectMappedBigList
#define ARRAY_FRONT_CODED_LIST ObjectArrayFrontCodedList
#define ARRAY_FRONT_CODED_BIG_LIST ObjectArrayFrontCodedBigList
#define HEAP_PRIORITY_QUEUE ObjectHeapPriorityQueue
#define HEAP_SEMI_INDIRECT_PRIORITY_QUEUE ObjectHeapSemiIndirectPriorityQueue
#define HEAP_INDIRECT_PRIORITY_QUEUE ObjectHeapIndirectPriorityQueue
#define HEAP_SESQUI_INDIRECT_DOUBLE_PRIORITY_QUEUE ObjectHeapSesquiIndirectDoublePriorityQueue
#define HEAP_INDIRECT_DOUBLE_PRIORITY_QUEUE ObjectHeapIndirectDoublePriorityQueue
#define ARRAY_FIFO_QUEUE ObjectArrayFIFOQueue
#define ARRAY_PRIORITY_QUEUE ObjectArrayPriorityQueue
#define ARRAY_INDIRECT_PRIORITY_QUEUE ObjectArrayIndirectPriorityQueue
#define ARRAY_INDIRECT_DOUBLE_PRIORITY_QUEUE ObjectArrayIndirectDoublePriorityQueue
#define KEY_BUFFER ObjectBuffer
/* Synchronized wrappers */
#define SYNCHRONIZED_COLLECTION SynchronizedObjectCollection
#define SYNCHRONIZED_SET SynchronizedObjectSet
#define SYNCHRONIZED_SORTED_SET SynchronizedObjectSortedSet
#define SYNCHRONIZED_FUNCTION SynchronizedObject2LongFunction
#define SYNCHRONIZED_MAP SynchronizedObject2LongMap
#define SYNCHRONIZED_LIST SynchronizedObjectList
/* Unmodifiable wrappers */
#define UNMODIFIABLE_COLLECTION UnmodifiableObjectCollection
#define UNMODIFIABLE_SET UnmodifiableObjectSet
#define UNMODIFIABLE_SORTED_SET UnmodifiableObjectSortedSet
#define UNMODIFIABLE_FUNCTION UnmodifiableObject2LongFunction
#define UNMODIFIABLE_MAP UnmodifiableObject2LongMap
#define UNMODIFIABLE_LIST UnmodifiableObjectList
#define UNMODIFIABLE_KEY_ITERATOR UnmodifiableObjectIterator
#define UNMODIFIABLE_KEY_BIDI_ITERATOR UnmodifiableObjectBidirectionalIterator
#define UNMODIFIABLE_KEY_LIST_ITERATOR UnmodifiableObjectListIterator
/* Other wrappers */
#define KEY_READER_WRAPPER ObjectReaderWrapper
#define KEY_DATA_INPUT_WRAPPER ObjectDataInputWrapper
#define KEY_DATA_NIO_INPUT_WRAPPER ObjectDataNioInputWrapper
/* Methods (keys) */
#define NEXT_KEY next
#define PREV_KEY previous
#define NEXT_KEY_WIDENED next
#define PREV_KEY_WIDENED previous
#define KEY_WIDENED_ITERATOR_METHOD objectIterator
#define KEY_WIDENED_SPLITERATOR_METHOD objectSpliterator
#define KEY_WIDENED_STREAM_METHOD objectStream
#define KEY_WIDENED_PARALLEL_STREAM_METHOD objectParallelStream
#define FIRST_KEY firstKey
#define LAST_KEY lastKey
#define GET_KEY get
#define AS_KEY_BUFFER asBuffer
#define PAIR_LEFT left
#define PAIR_FIRST first
#define PAIR_KEY key
#define REMOVE_KEY remove
#define READ_KEY readObject
#define WRITE_KEY writeObject
#define DEQUEUE dequeue
#define DEQUEUE_LAST dequeueLast
#define SINGLETON_METHOD objectSingleton
#define FIRST first
#define LAST last
#define TOP top
#define PEEK peek
#define POP pop
#define KEY_EMPTY_ITERATOR_METHOD emptyObjectIterator
#define KEY_EMPTY_SPLITERATOR_METHOD emptyObjectSpliterator
#define AS_KEY_ITERATOR asObjectIterator
#define AS_KEY_SPLITERATOR asObjectSpliterator
#define AS_KEY_COMPARATOR asObjectComparator
#define AS_KEY_ITERABLE asObjectIterable
#define AS_KEY_WIDENED_ITERATOR asObjectIterator
#define AS_KEY_WIDENED_SPLITERATOR asObjectSpliterator
#define TO_KEY_ARRAY toArray
#define ENTRY_GET_KEY getKey
#define REMOVE_FIRST_KEY removeFirst
#define REMOVE_LAST_KEY removeLast
#define PARSE_KEY parse
#define LOAD_KEYS loads
#define LOAD_KEYS_BIG loadsBig
#define STORE_KEYS stores
#if KEYS_REFERENCE
#define MAP_TO_KEY map
#define MAP_TO_KEY_WIDENED map
#define RETURN_FALSE_IF_KEY_NULL(k) if (k == null) return false;
#define REQUIRE_KEY_NON_NULL(k) java.util.Objects.requireNonNull(k);
#else
#define MAP_TO_KEY mapToObject
#define MAP_TO_KEY_WIDENED mapToObject
#define REQUIRE_KEY_NON_NULL(k)
#define RETURN_FALSE_IF_KEY_NULL(k)
#endif
/* Methods (values) */
#define MERGE_VALUE mergeLong
#define NEXT_VALUE nextLong
#define PREV_VALUE previousLong
#define READ_VALUE readLong
#define WRITE_VALUE writeLong
#define ENTRY_GET_VALUE getLongValue
#define REMOVE_FIRST_VALUE removeFirstLong
#define REMOVE_LAST_VALUE removeLastLong
#define AS_VALUE_ITERATOR asLongIterator
#define AS_VALUE_SPLITERATOR asLongSpliterator
#define PAIR_RIGHT rightLong
#define PAIR_SECOND secondLong
#define PAIR_VALUE valueLong
#if VALUES_REFERENCE
#define REQUIRE_VALUE_NON_NULL(v) java.util.Objects.requireNonNull(v);
#else
#define REQUIRE_VALUE_NON_NULL(v)
#endif
/* Methods (keys/values) */
#define ENTRYSET object2LongEntrySet
/* Methods that have special names depending on keys (but the special names depend on values) */
#if KEYS_REFERENCE
#define GET_VALUE getLong
#define REMOVE_VALUE removeLong
#define COMPUTE_IF_ABSENT_JDK computeLongIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeLongIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeLongIfAbsentPartial
#define COMPUTE computeLong
#define COMPUTE_IF_PRESENT computeLongIfPresent
#define MERGE mergeLong
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
#define VALUE2JAVAHASH(x) it.unimi.dsi.fastutil.HashCommon.long2int(x)
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
#include "drv/Functions.drv"

