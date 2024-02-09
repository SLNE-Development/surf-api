/* Generic definitions */


#define package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.objects
#define VALUE_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.doubles
#define WIDENED_package dev.slne.surf.surfapi.libs.fastutil.it.unimi.dsi.fastutil.objects
/* Assertions (useful to generate conditional code) */
#define KEY_CLASS_Reference 1
 #define KEYS_REFERENCE 1
#define VALUE_CLASS_Double 1
 #define VALUES_PRIMITIVE 1
 #define VALUES_INT_LONG_DOUBLE 1
/* Narrowing and widening */
#define KEY_NARROWING(x) x
#define KEY_LONG_NARROWING(x) it.unimi.dsi.fastutil.SafeMath.safeLongToReference(x)
#define VALUE_NARROWING(x) x
/* Current type and class (and size, if applicable) */
#define KEY_TYPE Object
#define KEY_TYPE_CAP Reference
#define VALUE_TYPE double
#define VALUE_TYPE_CAP Double
#define KEY_INDEX 9
#define KEY_TYPE_WIDENED Object
#define VALUE_TYPE_WIDENED double
#define KEY_CLASS Reference
#define VALUE_CLASS Double
#define VALUE_INDEX 7
#define KEY_CLASS_WIDENED Reference
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
#define KEY_VALUE ObjectValue
#define KEY_WIDENED_VALUE ObjectValue
#define VALUE_VALUE doubleValue
#define VALUE_WIDENED_VALUE doubleValue
/* Interfaces (keys) */
#define COLLECTION ReferenceCollection
#define STD_KEY_COLLECTION Collection
#define SET ReferenceSet
#define HASH ReferenceHash
#define SORTED_SET ReferenceSortedSet
#define STD_SORTED_SET SortedSet
#define FUNCTION Reference2DoubleFunction
#define MAP Reference2DoubleMap
#define SORTED_MAP Reference2DoubleSortedMap
#if KEY_CLASS_Object && VALUE_CLASS_Object
#define PAIR it.unimi.dsi.fastutil.Pair
#define SORTED_PAIR it.unimi.dsi.fastutil.SortedPair
#else
#define PAIR ReferenceDoublePair
#define SORTED_PAIR ReferenceDoubleSortedPair
#endif
#define MUTABLE_PAIR ReferenceDoubleMutablePair
#define IMMUTABLE_PAIR ReferenceDoubleImmutablePair
#define IMMUTABLE_SORTED_PAIR ReferenceReferenceImmutableSortedPair
#if KEYS_REFERENCE
#define STD_SORTED_MAP SortedMap
#define STRATEGY Strategy
#else
#define STD_SORTED_MAP Reference2DoubleSortedMap
#define STRATEGY PACKAGE.ReferenceHash.Strategy
#endif
#define LIST ReferenceList
#define BIG_LIST ReferenceBigList
#define STACK Stack
#define ATOMIC_ARRAY AtomicReferenceArray
#define PRIORITY_QUEUE PriorityQueue
#define INDIRECT_PRIORITY_QUEUE IndirectPriorityQueue
#define INDIRECT_DOUBLE_PRIORITY_QUEUE IndirectDoublePriorityQueue
#define KEY_CONSUMER Consumer
#define KEY_PREDICATE Predicate
#define KEY_UNARY_OPERATOR UnaryOperator
#define KEY_BINARY_OPERATOR BinaryOperator
#define KEY_ITERATOR ObjectIterator
#define KEY_WIDENED_ITERATOR ReferenceIterator
#define KEY_ITERABLE ObjectIterable
#define KEY_SPLITERATOR ObjectSpliterator
#define KEY_WIDENED_SPLITERATOR ReferenceSpliterator
#define KEY_BIDI_ITERATOR ObjectBidirectionalIterator
#define KEY_BIDI_ITERABLE ObjectBidirectionalIterable
#define KEY_LIST_ITERATOR ObjectListIterator
#define KEY_BIG_LIST_ITERATOR ObjectBigListIterator
#define STD_KEY_ITERATOR Iterator
#define STD_KEY_SPLITERATOR Spliterator
#define STD_KEY_ITERABLE Iterable
#define KEY_COMPARATOR Comparator
/* Interfaces (values) */
#define VALUE_COLLECTION DoubleCollection
#define VALUE_ARRAY_SET DoubleArraySet
#define VALUE_CONSUMER DoubleConsumer
#define VALUE_BINARY_OPERATOR DoubleBinaryOperator
#define VALUE_ITERATOR DoubleIterator
#define VALUE_SPLITERATOR DoubleSpliterator
#define VALUE_LIST_ITERATOR DoubleListIterator
/* Types and methods related to primitive-type support in the JDK */
#if KEYS_PRIMITIVE && ! KEY_CLASS_Boolean
#define JDK_PRIMITIVE_KEY_CONSUMER java.util.function.ReferenceConsumer
#define JDK_PRIMITIVE_PREDICATE java.util.function.ReferencePredicate
#define JDK_PRIMITIVE_BINARY_OPERATOR java.util.function.ReferenceBinaryOperator
#define JDK_PRIMITIVE_BINARY_OPERATOR_APPLY applyAsReference
#define JDK_PRIMITIVE_ITERATOR PrimitiveIterator.OfReference
#define JDK_PRIMITIVE_SPLITERATOR Spliterator.OfReference
#define JDK_PRIMITIVE_STREAM java.util.stream.ReferenceStream
#define JDK_PRIMITIVE_UNARY_OPERATOR java.util.function.ReferenceUnaryOperator
#define JDK_PRIMITIVE_KEY_APPLY applyAsReference
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.ReferenceFunction
#else
#define JDK_KEY_TO_GENERIC_FUNCTION java.util.function.Function
#endif
#if VALUES_PRIMITIVE && ! VALUE_CLASS_Boolean
#define JDK_PRIMITIVE_VALUE_CONSUMER java.util.function.DoubleConsumer
#define JDK_PRIMITIVE_VALUE_BINARY_OPERATOR java.util.function.DoubleBinaryOperator
#define JDK_PRIMITIVE_VALUE_OPERATOR_APPLY applyAsDouble
#endif
#define JDK_PRIMITIVE_FUNCTION java.util.function.ToDoubleFunction
 #define JDK_PRIMITIVE_FUNCTION_APPLY applyAsDouble
#if KEYS_INT_LONG_DOUBLE
#define METHOD_ARG_KEY_CONSUMER JDK_PRIMITIVE_KEY_CONSUMER
#define METHOD_ARG_PREDICATE JDK_PRIMITIVE_PREDICATE
#define METHOD_ARG_KEY_UNARY_OPERATOR JDK_PRIMITIVE_UNARY_OPERATOR
#define METHOD_ARG_KEY_BINARY_OPERATOR JDK_PRIMITIVE_BINARY_OPERATOR
#define KEY_OPERATOR_APPLY applyAsReference
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
#define VALUE_OPERATOR_APPLY applyAsDouble
#else
#define METHOD_ARG_VALUE_CONSUMER VALUE_CONSUMER VALUE_SUPER_GENERIC
#define METHOD_ARG_VALUE_BINARY_OPERATOR VALUE_PACKAGE.VALUE_BINARY_OPERATOR VALUE_GENERIC
#define VALUE_OPERATOR_APPLY apply
#endif
/* Abstract implementations (keys) */
#define ABSTRACT_COLLECTION AbstractReferenceCollection
#define ABSTRACT_SET AbstractReferenceSet
#define ABSTRACT_SORTED_SET AbstractReferenceSortedSet
#define ABSTRACT_FUNCTION AbstractReference2DoubleFunction
#define ABSTRACT_MAP AbstractReference2DoubleMap
#define ABSTRACT_FUNCTION AbstractReference2DoubleFunction
#define ABSTRACT_SORTED_MAP AbstractReference2DoubleSortedMap
#define ABSTRACT_LIST AbstractReferenceList
#define ABSTRACT_BIG_LIST AbstractReferenceBigList
#define SUBLIST ReferenceSubList
#define SUBLIST_RANDOM_ACCESS ReferenceRandomAccessSubList
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
#define KEY_ABSTRACT_COMPARATOR AbstractReferenceComparator
#endif
/* Abstract implementations (values) */
#define VALUE_ABSTRACT_COLLECTION AbstractDoubleCollection
#define VALUE_ABSTRACT_ITERATOR AbstractDoubleIterator
#define VALUE_ABSTRACT_BIDI_ITERATOR AbstractDoubleBidirectionalIterator
/* Static containers (keys) */
#define COLLECTIONS ReferenceCollections
#define SETS ReferenceSets
#define SORTED_SETS ReferenceSortedSets
#define LISTS ReferenceLists
#define BIG_LISTS ReferenceBigLists
#define MAPS Reference2DoubleMaps
#define FUNCTIONS Reference2DoubleFunctions
#define SORTED_MAPS Reference2DoubleSortedMaps
#define PRIORITY_QUEUES ObjectPriorityQueues
#define HEAPS ObjectHeaps
#define SEMI_INDIRECT_HEAPS ObjectSemiIndirectHeaps
#define INDIRECT_HEAPS ObjectIndirectHeaps
#define ARRAYS ObjectArrays
#define BIG_ARRAYS ObjectBigArrays
#define ITERABLES ObjectIterables
#define ITERATORS ObjectIterators
#define WIDENED_ITERATORS ReferenceIterators
#define SPLITERATORS ObjectSpliterators
#define WIDENED_SPLITERATORS ReferenceSpliterators
#define BIG_LIST_ITERATORS ObjectBigListIterators
#define BIG_SPLITERATORS ObjectBigSpliterators
#define COMPARATORS ObjectComparators
/* Static containers (values) */
#define VALUE_COLLECTIONS DoubleCollections
#define VALUE_SETS DoubleSets
#define VALUE_ARRAYS DoubleArrays
#define VALUE_ITERATORS DoubleIterators
#define VALUE_SPLITERATORS DoubleSpliterators
/* Implementations */
#define OPEN_HASH_SET ReferenceOpenHashSet
#define OPEN_HASH_BIG_SET ReferenceOpenHashBigSet
#define OPEN_DOUBLE_HASH_SET ReferenceOpenDoubleHashSet
#define OPEN_HASH_MAP Reference2DoubleOpenHashMap
#define OPEN_HASH_BIG_MAP Reference2DoubleOpenHashBigMap
#define STRIPED_OPEN_HASH_MAP StripedReference2DoubleOpenHashMap
#define OPEN_DOUBLE_HASH_MAP Reference2DoubleOpenDoubleHashMap
#define ARRAY_SET ReferenceArraySet
#define ARRAY_MAP Reference2DoubleArrayMap
#define LINKED_OPEN_HASH_SET ReferenceLinkedOpenHashSet
#define AVL_TREE_SET ReferenceAVLTreeSet
#define RB_TREE_SET ReferenceRBTreeSet
#define AVL_TREE_MAP Reference2DoubleAVLTreeMap
#define RB_TREE_MAP Reference2DoubleRBTreeMap
#define ARRAY_LIST ReferenceArrayList
#define IMMUTABLE_LIST ReferenceImmutableList
#define BIG_ARRAY_BIG_LIST ReferenceBigArrayBigList
#define MAPPED_BIG_LIST ReferenceMappedBigList
#define ARRAY_FRONT_CODED_LIST ReferenceArrayFrontCodedList
#define ARRAY_FRONT_CODED_BIG_LIST ReferenceArrayFrontCodedBigList
#define HEAP_PRIORITY_QUEUE ObjectHeapPriorityQueue
#define HEAP_SEMI_INDIRECT_PRIORITY_QUEUE ObjectHeapSemiIndirectPriorityQueue
#define HEAP_INDIRECT_PRIORITY_QUEUE ObjectHeapIndirectPriorityQueue
#define HEAP_SESQUI_INDIRECT_DOUBLE_PRIORITY_QUEUE ObjectHeapSesquiIndirectDoublePriorityQueue
#define HEAP_INDIRECT_DOUBLE_PRIORITY_QUEUE ObjectHeapIndirectDoublePriorityQueue
#define ARRAY_FIFO_QUEUE ObjectArrayFIFOQueue
#define ARRAY_PRIORITY_QUEUE ObjectArrayPriorityQueue
#define ARRAY_INDIRECT_PRIORITY_QUEUE ObjectArrayIndirectPriorityQueue
#define ARRAY_INDIRECT_DOUBLE_PRIORITY_QUEUE ObjectArrayIndirectDoublePriorityQueue
#define KEY_BUFFER ReferenceBuffer
/* Synchronized wrappers */
#define SYNCHRONIZED_COLLECTION SynchronizedReferenceCollection
#define SYNCHRONIZED_SET SynchronizedReferenceSet
#define SYNCHRONIZED_SORTED_SET SynchronizedReferenceSortedSet
#define SYNCHRONIZED_FUNCTION SynchronizedReference2DoubleFunction
#define SYNCHRONIZED_MAP SynchronizedReference2DoubleMap
#define SYNCHRONIZED_LIST SynchronizedReferenceList
/* Unmodifiable wrappers */
#define UNMODIFIABLE_COLLECTION UnmodifiableReferenceCollection
#define UNMODIFIABLE_SET UnmodifiableReferenceSet
#define UNMODIFIABLE_SORTED_SET UnmodifiableReferenceSortedSet
#define UNMODIFIABLE_FUNCTION UnmodifiableReference2DoubleFunction
#define UNMODIFIABLE_MAP UnmodifiableReference2DoubleMap
#define UNMODIFIABLE_LIST UnmodifiableReferenceList
#define UNMODIFIABLE_KEY_ITERATOR UnmodifiableReferenceIterator
#define UNMODIFIABLE_KEY_BIDI_ITERATOR UnmodifiableReferenceBidirectionalIterator
#define UNMODIFIABLE_KEY_LIST_ITERATOR UnmodifiableReferenceListIterator
/* Other wrappers */
#define KEY_READER_WRAPPER ReferenceReaderWrapper
#define KEY_DATA_INPUT_WRAPPER ReferenceDataInputWrapper
#define KEY_DATA_NIO_INPUT_WRAPPER ReferenceDataNioInputWrapper
/* Methods (keys) */
#define NEXT_KEY next
#define PREV_KEY previous
#define NEXT_KEY_WIDENED next
#define PREV_KEY_WIDENED previous
#define KEY_WIDENED_ITERATOR_METHOD referenceIterator
#define KEY_WIDENED_SPLITERATOR_METHOD referenceSpliterator
#define KEY_WIDENED_STREAM_METHOD referenceStream
#define KEY_WIDENED_PARALLEL_STREAM_METHOD referenceParallelStream
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
#define SINGLETON_METHOD referenceSingleton
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
#define MERGE_VALUE mergeDouble
#define NEXT_VALUE nextDouble
#define PREV_VALUE previousDouble
#define READ_VALUE readDouble
#define WRITE_VALUE writeDouble
#define ENTRY_GET_VALUE getDoubleValue
#define REMOVE_FIRST_VALUE removeFirstDouble
#define REMOVE_LAST_VALUE removeLastDouble
#define AS_VALUE_ITERATOR asDoubleIterator
#define AS_VALUE_SPLITERATOR asDoubleSpliterator
#define PAIR_RIGHT rightDouble
#define PAIR_SECOND secondDouble
#define PAIR_VALUE valueDouble
#if VALUES_REFERENCE
#define REQUIRE_VALUE_NON_NULL(v) java.util.Objects.requireNonNull(v);
#else
#define REQUIRE_VALUE_NON_NULL(v)
#endif
/* Methods (keys/values) */
#define ENTRYSET reference2DoubleEntrySet
/* Methods that have special names depending on keys (but the special names depend on values) */
#if KEYS_REFERENCE
#define GET_VALUE getDouble
#define REMOVE_VALUE removeDouble
#define COMPUTE_IF_ABSENT_JDK computeDoubleIfAbsent
#define COMPUTE_IF_ABSENT_NULLABLE computeDoubleIfAbsentNullable
#define COMPUTE_IF_ABSENT_PARTIAL computeDoubleIfAbsentPartial
#define COMPUTE computeDouble
#define COMPUTE_IF_PRESENT computeDoubleIfPresent
#define MERGE mergeDouble
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
#define VALUE2JAVAHASH(x) it.unimi.dsi.fastutil.HashCommon.double2int(x)
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
#include "drv/MutablePair.drv"

