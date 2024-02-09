plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-server"))
    api(project(":surf-api-velocity:surf-api-velocity-api"))
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    implementation(libs.fastutil)
    annotationProcessor(libs.velocity.api)
}

tasks {
    shadowJar {
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

        relocate("it.unimi.dsi.fastutil", "dev.slne.fastutil")
    }
}

description = "surf-api-velocity-server"
