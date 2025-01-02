val relocationPrefix: String by project

plugins {
    `core-convention`
}

dependencies {
    api(project(":surf-api-core:surf-api-core-server"))
    api(project(":surf-api-velocity:surf-api-velocity-api"))
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    api(libs.fastutil)
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.aide.reflection)
    runtimeOnly(libs.flogger.slf4j.backend)
    annotationProcessor(libs.velocity.api)
}

tasks {
    shadowJar {
        relocate("it.unimi.dsi.fastutil", "$relocationPrefix.fastutil")
    }

//    shadowJar {
        // Thank you velocity for using fastutil but excluding the types we need - https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/build.gradle.kts#L30
//        exclude("it/unimi/dsi/fastutil/ints/*Int2Object*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntCo*")
//        exclude("it/unimi/dsi/fastutil/ints/AbstractIntList*")
//        exclude("it/unimi/dsi/fastutil/ints/*Int*Set*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntSpliterator*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntStack*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntBoolean*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntByte*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntChar*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntDouble*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntFloat*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntInt*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntLong*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntObject*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntReference*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntShort*Pair*")
//        exclude("it/unimi/dsi/fastutil/ints/IntHash*")
//        exclude("it/unimi/dsi/fastutil/ints/IntImmutableList*")
//        exclude("it/unimi/dsi/fastutil/ints/Int*IndirectHeaps*")
//        exclude("it/unimi/dsi/fastutil/ints/*IntItera*")
//        exclude("it/unimi/dsi/fastutil/ints/IntPredicate*")
//        exclude("it/unimi/dsi/fastutil/ints/IntSpliterator*")
//        exclude("it/unimi/dsi/fastutil/ints/IntUnaryOperator*")
//        exclude("it/unimi/dsi/fastutil/ints/package-info*")
//
//        exclude("it/unimi/dsi/fastutil/objects/*Object2Int*")
//        exclude("it/unimi/dsi/fastutil/objects/*Object*Itera*")
//        exclude("it/unimi/dsi/fastutil/objects/*Object*itera*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectCollection*")
//        exclude("it/unimi/dsi/fastutil/objects/*Object*List*")
//////        exclude("it/unimi/dsi/fastutil/objects/*Object*Set*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectArraySet*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectBoolean*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectByte*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectChar*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectDouble*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectFloat*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectInt*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectLong*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectObject*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectShort*Pair*")
//        exclude("it/unimi/dsi/fastutil/objects/*ObjectComparators*")
//        exclude("it/unimi/dsi/fastutil/objects/*Object*Heap*")
//////        exclude("it/unimi/dsi/fastutil/objects/*Object*Set*")
//        exclude("it/unimi/dsi/fastutil/objects/package-info*")
//
//        exclude("it/unimi/dsi/fastutil/*Abstract*")
//        exclude("it/unimi/dsi/fastutil/*Array*")
//        exclude("it/unimi/dsi/fastutil/*Bi*")
//        exclude("it/unimi/dsi/fastutil/*Function*")
//        exclude("it/unimi/dsi/fastutil/*Hash*")
//        exclude("it/unimi/dsi/fastutil/*Queue*")
//        exclude("it/unimi/dsi/fastutil/*Pair*")
//        exclude("it/unimi/dsi/fastutil/*Math*")
//        exclude("it/unimi/dsi/fastutil/*Size*")
//        exclude("it/unimi/dsi/fastutil/*Stack*")
//        exclude("it/unimi/dsi/fastutil/*Swapper*")
//        exclude("it/unimi/dsi/fastutil/package-info*")
//
////        relocate("it.unimi.dsi.fastutil", "dev.slne.fastutil")
////          it.unimi.dsi.fastutil.objects.ObjectSets
//        exclude("it/unimi/dsi/fastutil/object/ObjectSets")
//        exclude("it/unimi/dsi/fastutil/object/Object2BooleanMaps")
//        exclude("it/unimi/dsi/fastutil/objects/ObjectArraySet")
//    }
}

//configurations {
//    runtimeClasspath {
//        resolutionStrategy {
//            eachDependency {
//                if (requested.group == "it.unimi.dsi" && requested.name == "fastutil") {
//                    exclude("it/unimi/dsi/fastutil/ints/*Int2Object*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntCo*")
//                    exclude("it/unimi/dsi/fastutil/ints/AbstractIntList*")
//                    exclude("it/unimi/dsi/fastutil/ints/*Int*Set*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntSpliterator*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntStack*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntBoolean*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntByte*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntChar*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntDouble*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntFloat*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntInt*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntLong*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntObject*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntReference*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntShort*Pair*")
//                    exclude("it/unimi/dsi/fastutil/ints/IntHash*")
//                    exclude("it/unimi/dsi/fastutil/ints/IntImmutableList*")
//                    exclude("it/unimi/dsi/fastutil/ints/Int*IndirectHeaps*")
//                    exclude("it/unimi/dsi/fastutil/ints/*IntItera*")
//                    exclude("it/unimi/dsi/fastutil/ints/IntPredicate*")
//                    exclude("it/unimi/dsi/fastutil/ints/IntSpliterator*")
//                    exclude("it/unimi/dsi/fastutil/ints/IntUnaryOperator*")
//                    exclude("it/unimi/dsi/fastutil/ints/package-info*")
//
//                    exclude("it/unimi/dsi/fastutil/objects/*Object2Int*")
//                    exclude("it/unimi/dsi/fastutil/objects/*Object*Itera*")
//                    exclude("it/unimi/dsi/fastutil/objects/*Object*itera*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectCollection*")
//                    exclude("it/unimi/dsi/fastutil/objects/*Object*List*")
//////        exclude("it/unimi/dsi/fastutil/objects/*Object*Set*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectArraySet*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectBoolean*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectByte*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectChar*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectDouble*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectFloat*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectInt*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectLong*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectObject*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectShort*Pair*")
//                    exclude("it/unimi/dsi/fastutil/objects/*ObjectComparators*")
//                    exclude("it/unimi/dsi/fastutil/objects/*Object*Heap*")
//////        exclude("it/unimi/dsi/fastutil/objects/*Object*Set*")
//                    exclude("it/unimi/dsi/fastutil/objects/package-info*")
//
//                    exclude("it/unimi/dsi/fastutil/*Abstract*")
//                    exclude("it/unimi/dsi/fastutil/*Array*")
//                    exclude("it/unimi/dsi/fastutil/*Bi*")
//                    exclude("it/unimi/dsi/fastutil/*Function*")
//                    exclude("it/unimi/dsi/fastutil/*Hash*")
//                    exclude("it/unimi/dsi/fastutil/*Queue*")
//                    exclude("it/unimi/dsi/fastutil/*Pair*")
//                    exclude("it/unimi/dsi/fastutil/*Math*")
//                    exclude("it/unimi/dsi/fastutil/*Size*")
//                    exclude("it/unimi/dsi/fastutil/*Stack*")
//                    exclude("it/unimi/dsi/fastutil/*Swapper*")
//                    exclude("it/unimi/dsi/fastutil/package-info*")
//
////                    relocate("it.unimi.dsi.fastutil", "dev.slne.fastutil")
////                    it.unimi.dsi.fastutil.objects.ObjectSets
//                    exclude("it/unimi/dsi/fastutil/object/ObjectSets")
//                    exclude("it/unimi/dsi/fastutil/object/Object2BooleanMaps")
//                }
//            }
//        }
//    }
//}

description = "surf-api-velocity-server"
