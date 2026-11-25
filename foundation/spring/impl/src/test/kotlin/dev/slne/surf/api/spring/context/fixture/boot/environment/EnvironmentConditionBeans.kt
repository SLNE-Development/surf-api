package dev.slne.surf.api.spring.context.fixture.boot.environment

import dev.slne.surf.api.spring.context.fixture.boot.FEATURE_PROPERTY
import dev.slne.surf.api.spring.context.fixture.boot.MARKER_RESOURCE
import org.springframework.boot.autoconfigure.condition.*
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.boot.system.JavaVersion
import org.springframework.boot.thread.Threading
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(FEATURE_PROPERTY, havingValue = "on")
class FeatureBean

@Service
@ConditionalOnExpression("'\${$FEATURE_PROPERTY:off}' == 'on'")
class ExpressionBean

@Service
@ConditionalOnResource(resources = ["classpath:$MARKER_RESOURCE"])
class ResourceBean

@Service
@ConditionalOnJava(JavaVersion.SEVENTEEN)
class JavaBean

@Service
@ConditionalOnThreading(Threading.VIRTUAL)
class VirtualThreadsBean

@Service
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class KubernetesBean

/**
 * Matches when the feature property is set and the marker resource exists.
 */
class FeatureWithResource : AllNestedConditions(ConfigurationPhase.REGISTER_BEAN) {

    @ConditionalOnProperty(FEATURE_PROPERTY, havingValue = "on")
    class Feature

    @ConditionalOnResource(resources = ["classpath:$MARKER_RESOURCE"])
    class Resource
}

@Service
@Conditional(FeatureWithResource::class)
class FeatureWithResourceBean

/**
 * Matches when the feature property is not set.
 */
class FeatureOff : NoneNestedConditions(ConfigurationPhase.REGISTER_BEAN) {

    @ConditionalOnProperty(FEATURE_PROPERTY, havingValue = "on")
    class Feature
}

@Service
@Conditional(FeatureOff::class)
class FeatureOffBean
