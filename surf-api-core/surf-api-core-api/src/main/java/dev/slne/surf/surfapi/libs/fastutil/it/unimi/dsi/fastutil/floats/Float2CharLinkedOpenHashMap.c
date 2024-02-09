#define SET_PREV( f64, p32 )       SET_UPPER( f64, p32 )
#define SET_NEXT( f64, n32 )       SET_LOWER( f64, n32 )
#define COPY_PREV( f64, p64 )      SET_UPPER64( f64, p64 )
#define COPY_NEXT( f64, n64 )      SET_LOWER64( f64, n64 )
#define GET_PREV( f64 )            GET_UPPER( f64 )
#define GET_NEXT( f64 )            GET_LOWER( f64 )
#define SET_UPPER_LOWER( f64, up32, low32 )    f64 = ( ( up32 & 0xFFFFFFFFL ) << 32 ) | ( low32 & 0xFFFFFFFFL )
#define SET_UPPER( f64, up32 )     f64 ^= ( ( f64 ^ ( ( up32 & 0xFFFFFFFFL ) << 32 ) ) & 0xFFFFFFFF00000000L )
#define SET_LOWER( f64, low32 )    f64 ^= ( ( f64 ^ ( low32 & 0xFFFFFFFFL ) ) & 0xFFFFFFFFL )
#define SET_UPPER64( f64, up64 )   f64 ^= ( ( f64 ^ ( up64 & 0xFFFFFFFF00000000L ) ) & 0xFFFFFFFF00000000L )
#define SET_LOWER64( f64, low64 )  f64 ^= ( ( f64 ^ ( low64 & 0xFFFFFFFFL ) ) & 0xFFFFFFFFL )
#define GET_UPPER( f64 )           (int) ( f64 >>> 32 )
#define GET_LOWER( f64 )           (int) f64

/* Generic definitions */
#define Linked

#define package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.floats
#define VALUE_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.chars
#define WIDENED_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.doubles
/* Assertions (useful to generate conditional code) */
#define KEY_CLASS_Float 1
 #define KEYS_PRIMITIVE 1
 #define KEYS_BYTE_CHAR_SHORT_FLOAT 1
#define VALUE_CLASS_Character 1
 #define VALUES_PRIMITIVE 1
 #define VALUES_BYTE_CHAR_SHORT_FLOAT 1
/* Narrowing and widening */
#define KEY_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeDoubleToFloat(x)
#define VALUE_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeIntToChar(x)
/* Current type and class (and size, if applicable) */
#define KEY_TYPE float
#define KEY_TYPE_CAP Float
#define VALUE_TYPE char
#define VALUE_TYPE_CAP Char
#define KEY_INDEX 6
#define KEY_TYPE_WIDENED double
#define VALUE_TYPE_WIDENED int
#define KEY_CLASS Float
#define VALUE_CLASS Character
#define VALUE_INDEX 5
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
#define KEY_VALUE floatValue
#define KEY_WIDENED_VALUE doubleValue
#define VALUE_VALUE charValue
#define VALUE_WIDENED_VALUE intValue
/* Interfaces (keys) */
#define COLLECTION FloatCollection
#define STD_KEY_COLLECTION FloatCollection
#define SET FloatSet
#define HASH FloatHash
#define SORTED_SET FloatSortedSet
#define STD_SORTED_SET FloatSortedSet
#define FUNCTION Float2CharFunction
#define MAP Float2CharMap
#define SORTED_MAP Float2CharSortedMap
#if KEY_CLASS_Object && VALUE_CLASS_Object
#define PAIR it.unimi.dsi.fastutil.Pair
#define SORTED_PAIR it.unimi.dsi.fastutil.SortedPair
#else
#define PAIR FloatCharPair
#define SORTED_PAIR FloatCharSortedPair
#endif
#define MUTABLE_PAIR FloatCharMutablePair
#define IMMUTABLE_PAIR FloatCharImmutablePair
#define IMMUTABLE_SORTED_PAIR FloatFloatImmutableSortedPair
#if KEYS_REFERENCE
#define STD_SORTED_MAP SortedMap
#define STRATEGY Strategy
#else
#define STD_SORTED_MAP Float2CharSortedMap
#define STRATEGY PACKAGE.FloatHash.Strategy
#endif
#define LIST FloatList
#define BIG_LIST FloatBigList
#define STACK FloatStack
#define ATOMIC_ARRAY AtomicFloatArray
#define PRIORITY_QUEUE FloatPriorityQueue
#define INDIRECT_PRIORITY_QUEUE FloatIndirectPriorityQueue
#define INDIRECT_DOUBLE_PRIORITY_QUEUE FloatIndirectDoublePriorityQueue
#define KEY_CONSUMER FloatConsumer
#define KEY_PREDICATE FloatPredicate
#define KEY_UNARY_OPERATOR FloatUnaryOperator
#define KEY_BINARY_OPERATOR FloatBinaryOperator
#define KEY_ITERATOR FloatIterator
#define KEY_WIDENED_ITERATOR DoubleIterator
#define KEY_ITERABLE FloatIterable
#define KEY_SPLITERATOR FloatSpliterator
#define KEY_WIDENED_SPLITERATOR DoubleSpliterator
#define KEY_BIDI_ITERATOR FloatBidirectionalIterator
#define KEY_BIDI_ITERABLE FloatBidirectionalIterable
#define KEY_LIST_ITERATOR FloatListIterator
#define KEY_BIG_LIST_ITERATOR FloatBigListIterator
#define STD_KEY_ITERATOR FloatIterator
#define STD_KEY_SPLITERATOR FloatSpliterator
#define STD_KEY_ITERABLE FloatIterable
#define KEY_COMPARATOR FloatComparator
/* Interfaces (values) */
#define VALUE_COLLECTION CharCollection
#define VALUE_ARRAY_SET CharArraySet
#define VALUE_CONSUMER CharConsumer
#define VALUE_BINARY_OPERATOR CharBinaryOperator
#define VALUE_ITERATOR CharIterator
#define VALUE_SPLITERATOR CharSpliterator
#define VALUE_LIST_ITERATOR CharListIterator
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
#define KEY_OPERATOR_APPLY applyAsFloat
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
#define VALUE_OPERATOR_APPLY applyAsChar
#else
#define METHOD_ARG_VALUE_CONSUMER VALUE_CONSUMER VALUE_SUPER_GENERIC
#define METHOD_ARG_VALUE_BINARY_OPERATOR VALUE_PACKAGE.VALUE_BINARY_OPERATOR VALUE_GENERIC
#define VALUE_OPERATOR_APPLY apply
#endif
/* Abstract implementations (keys) */
#define ABSTRACT_COLLECTION AbstractFloatCollection
#define ABSTRACT_SET AbstractFloatSet
#define ABSTRACT_SORTED_SET AbstractFloatSortedSet
#define ABSTRACT_FUNCTION AbstractFloat2CharFunction
#define ABSTRACT_MAP AbstractFloat2CharMap
#define ABSTRACT_FUNCTION AbstractFloat2CharFunction
#define ABSTRACT_SORTED_MAP AbstractFloat2CharSortedMap
#define ABSTRACT_LIST AbstractFloatList
#define ABSTRACT_BIG_LIST AbstractFloatBigList
#define SUBLIST FloatSubList
#define SUBLIST_RANDOM_ACCESS FloatRandomAccessSubList
#define ABSTRACT_PRIORITY_QUEUE AbstractFloatPriorityQueue
#define ABSTRACT_STACK AbstractFloatStack
#define KEY_ABSTRACT_ITERATOR AbstractFloatIterator
#define KEY_ABSTRACT_SPLITERATOR AbstractFloatSpliterator
#define KEY_ABSTRACT_BIDI_ITERATOR AbstractFloatBidirectionalIterator
#define KEY_ABSTRACT_LIST_ITERATOR AbstractFloatListIterator
#define KEY_ABSTRACT_BIG_LIST_ITERATOR AbstractFloatBigListIterator
#if KEY_CLASS_Object
#define KEY_ABSTRACT_COMPARATOR Comparator
#else
#define KEY_ABSTRACT_COMPARATOR AbstractFloatComparator
#endif
/* Abstract implementations (values) */
#define VALUE_ABSTRACT_COLLECTION AbstractCharCollection
#define VALUE_ABSTRACT_ITERATOR AbstractCharIterator
#define VALUE_ABSTRACT_BIDI_ITERATOR AbstractCharBidirectionalIterator
/* Static containers (keys) */
#define COLLECTIONS FloatCollections
#define SETS FloatSets
#define SORTED_SETS FloatSortedSets
#define LISTS FloatLists
#define BIG_LISTS FloatBigLists
#define MAPS Float2CharMaps
#define FUNCTIONS Float2CharFunctions
#define SORTED_MAPS Float2CharSortedMaps
#define PRIORITY_QUEUES FloatPriorityQueues
#define HEAPS FloatHeaps
#define SEMI_INDIRECT_HEAPS FloatSemiIndirectHeaps
#define INDIRECT_HEAPS FloatIndirectHeaps
#define ARRAYS FloatArrays
#define BIG_ARRAYS FloatBigArrays
#define ITERABLES FloatIterables
#define ITERATORS FloatIterators
#define WIDENED_ITERATORS DoubleIterators
#define SPLITERATORS FloatSpliterators
#define WIDENED_SPLITERATORS DoubleSpliterators
#define BIG_LIST_ITERATORS FloatBigListIterators
#define BIG_SPLITERATORS FloatBigSpliterators
#define COMPARATORS FloatComparators
/* Static containers (values) */
#define VALUE_COLLECTIONS CharCollections
#define VALUE_SETS CharSets
#define VALUE_ARRAYS CharArrays
#define VALUE_ITERATORS CharIterators
#define VALUE_SPLITERATORS CharSpliterators
/* Implementations */
#define OPEN_HASH_SET FloatLinkedOpenHashSet
#define OPEN_HASH_BIG_SET FloatLinkedOpenHashBigSet
#define OPEN_DOUBLE_HASH_SET FloatLinkedOpenDoubleHashSet
#define OPEN_HASH_MAP Float2CharLinkedOpenHashMap
#define OPEN_HASH_BIG_MAP Float2CharLinkedOpenHashBigMap
#define STRIPED_OPEN_HASH_MAP StripedFloat2CharOpenHashMap
#define OPEN_DOUBLE_HASH_MAP Float2CharLinkedOpenDoubleHashMap
#define ARRAY_SET FloatArraySet
#define ARRAY_MAP Float2CharArrayMap
#define LINKED_OPEN_HASH_SET FloatLinkedOpenHashSet
#define AVL_TREE_SET FloatAVLTreeSet
#define RB_TREE_SET FloatRBTreeSet
#define AVL_TREE_MAP Float2CharAVLTreeMap
#define RB_TREE_MAP Float2CharRBTreeMap
#define ARRAY_LIST FloatArrayList
#define IMMUTABLE_LIST FloatImmutableList
#define BIG_ARRAY_BIG_LIST FloatBigArrayBigList
#define MAPPED_BIG_LIST FloatMappedBigList
#define ARRAY_FRONT_CODED_LIST FloatArrayFrontCodedList
#define ARRAY_FRONT_CODED_BIG_LIST FloatArrayFrontCodedBigList
#define HEAP_PRIORITY_QUEUE FloatHeapPriorityQueue
#define HEAP_SEMI_INDIRECT_PRIORITY_QUEUE FloatHeapSemiIndirectPriorityQueue
#define HEAP_INDIRECT_PRIORITY_QUEUE FloatHeapIndirectPriorityQueue
#define HEAP_SESQUI_INDIRECT_DOUBLE_PRIORITY_QUEUE FloatHeapSesquiIndirectDoublePriorityQueue
#define HEAP_INDIRECT_DOUBLE_PRIORITY_QUEUE FloatHeapIndirectDoublePriorityQueue
#define ARRAY_FIFO_QUEUE FloatArrayFIFOQueue
#define ARRAY_PRIORITY_QUEUE FloatArrayPriorityQueue
#define ARRAY_INDIRECT_PRIORITY_QUEUE FloatArrayIndirectPriorityQueue
#define ARRAY_INDIRECT_DOUBLE_PRIORITY_QUEUE FloatArrayIndirectDoublePriorityQueue
#define KEY_BUFFER FloatBuffer
/* Synchronized wrappers */
#define SYNCHRONIZED_COLLECTION SynchronizedFloatCollection
#define SYNCHRONIZED_SET SynchronizedFloatSet
#define SYNCHRONIZED_SORTED_SET SynchronizedFloatSortedSet
#define SYNCHRONIZED_FUNCTION SynchronizedFloat2CharFunction
#define SYNCHRONIZED_MAP SynchronizedFloat2CharMap
#define SYNCHRONIZED_LIST SynchronizedFloatList
/* Unmodifiable wrappers */
#define UNMODIFIABLE_COLLECTION UnmodifiableFloatCollection
#define UNMODIFIABLE_SET UnmodifiableFloatSet
#define UNMODIFIABLE_SORTED_SET UnmodifiableFloatSortedSet
#define UNMODIFIABLE_FUNCTION UnmodifiableFloat2CharFunction
#define UNMODIFIABLE_MAP UnmodifiableFloat2CharMap
#define UNMODIFIABLE_LIST UnmodifiableFloatList
#define UNMODIFIABLE_KEY_ITERATOR UnmodifiableFloatIterator
#define UNMODIFIABLE_KEY_BIDI_ITERATOR UnmodifiableFloatBidirectionalIterator
#define UNMODIFIABLE_KEY_LIST_ITERATOR UnmodifiableFloatListIterator
/* Other wrappers */
#define KEY_READER_WRAPPER FloatReaderWrapper
#define KEY_DATA_INPUT_WRAPPER FloatDataInputWrapper
#define KEY_DATA_NIO_INPUT_WRAPPER FloatDataNioInputWrapper
/* Methods (keys) */
#define NEXT_KEY nextFloat
#define PREV_KEY previousFloat
#define NEXT_KEY_WIDENED nextDouble
#define PREV_KEY_WIDENED previousDouble
#define KEY_WIDENED_ITERATOR_METHOD doubleIterator
#define KEY_WIDENED_SPLITERATOR_METHOD doubleSpliterator
#define KEY_WIDENED_STREAM_METHOD doubleStream
#define KEY_WIDENED_PARALLEL_STREAM_METHOD doubleParallelStream
#define FIRST_KEY firstFloatKey
#define LAST_KEY lastFloatKey
#define GET_KEY getFloat
#define AS_KEY_BUFFER asFloatBuffer
#define PAIR_LEFT leftFloat
#define PAIR_FIRST firstFloat
#define PAIR_KEY keyFloat
#define REMOVE_KEY removeFloat
#define READ_KEY readFloat
#define WRITE_KEY writeFloat
#define DEQUEUE dequeueFloat
#define DEQUEUE_LAST dequeueLastFloat
#define SINGLETON_METHOD floatSingleton
#define FIRST firstFloat
#define LAST lastFloat
#define TOP topFloat
#define PEEK peekFloat
#define POP popFloat
#define KEY_EMPTY_ITERATOR_METHOD emptyFloatIterator
#define KEY_EMPTY_SPLITERATOR_METHOD emptyFloatSpliterator
#define AS_KEY_ITERATOR asFloatIterator
#define AS_KEY_SPLITERATOR asFloatSpliterator
#define AS_KEY_COMPARATOR asFloatComparator
#define AS_KEY_ITERABLE asFloatIterable
#define AS_KEY_WIDENED_ITERATOR asDoubleIterator
#define AS_KEY_WIDENED_SPLITERATOR asDoubleSpliterator
#define TO_KEY_ARRAY toFloatArray
#define ENTRY_GET_KEY getFloatKey
#define REMOVE_FIRST_KEY removeFirstFloat
#define REMOVE_LAST_KEY removeLastFloat
#define PARSE_KEY parseFloat
#define LOAD_KEYS loadFloats
#define LOAD_KEYS_BIG loadFloatsBig
#define STORE_KEYS storeFloats
#if KEYS_REFERENCE
#define MAP_TO_KEY map
#define MAP_TO_KEY_WIDENED map
#define RETURN_FALSE_IF_KEY_NULL(k) if (k == null) return false;
#define REQUIRE_KEY_NON_NULL(k) java.util.Objects.requireNonNull(k);
#else
#define MAP_TO_KEY mapToFloat
#define MAP_TO_KEY_WIDENED mapToDouble
#define REQUIRE_KEY_NON_NULL(k)
#define RETURN_FALSE_IF_KEY_NULL(k)
#endif
/* Methods (values) */
#define MERGE_VALUE mergeChar
#define NEXT_VALUE nextChar
#define PREV_VALUE previousChar
#define READ_VALUE readChar
#define WRITE_VALUE writeChar
#define ENTRY_GET_VALUE getCharValue
#define REMOVE_FIRST_VALUE removeFirstChar
#define REMOVE_LAST_VALUE removeLastChar
#define AS_VALUE_ITERATOR asCharIterator
#define AS_VALUE_SPLITERATOR asCharSpliterator
#define PAIR_RIGHT rightChar
#define PAIR_SECOND secondChar
#define PAIR_VALUE valueChar
#if VALUES_REFERENCE
#define REQUIRE_VALUE_NON_NULL(v) java.util.Objects.requireNonNull(v);
#else
#define REQUIRE_VALUE_NON_NULL(v)
#endif
/* Methods (keys/values) */
#define ENTRYSET float2CharEntrySet
/* Methods that have special names depending on keys (but the special names depend on values) */
#if KEYS_REFERENCE
#define GET_VALUE getChar
#define REMOVE_VALUE removeChar
#define COMPUTE_IF_ABSENT_JDK computeCharIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeCharIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeCharIfAbsentPartial
#define COMPUTE computeChar
#define COMPUTE_IF_PRESENT computeCharIfPresent
#define MERGE mergeChar
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
#define VALUE2JAVAHASH(x) it.unimi.dsi.fastutil.HashCommon.char2int(x)
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
#include "drv/LinkedOpenHashMap.drv"

