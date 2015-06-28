package com.netflix.eureka2.server.registry;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Set;

import com.netflix.eureka2.metric.EurekaRegistryMetricFactory;
import com.netflix.eureka2.registry.EurekaRegistrationProcessor;
import com.netflix.eureka2.registry.SourcedEurekaRegistry;
import com.netflix.eureka2.registry.instance.InstanceInfo;
import com.netflix.eureka2.server.service.overrides.OverridesService;

/**
 * @author Tomasz Bak
 */
@Singleton
public class RegistrationChannelProcessorProvider implements Provider<EurekaRegistrationProcessor> {

    private final PreservableRegistryProcessor preservableRegistrationProcessor;

    @Inject
    public RegistrationChannelProcessorProvider(SourcedEurekaRegistry sourcedEurekaRegistry,
                                                Set<OverridesService> overrideServices,
                                                EvictionQuotaKeeper evictionQuotaKeeper,
                                                EurekaRegistryMetricFactory metricFactory) {
        this.preservableRegistrationProcessor = new PreservableRegistryProcessor(
                combine(sourcedEurekaRegistry, overrideServices),
                evictionQuotaKeeper,
                metricFactory
        );
    }

    @PreDestroy
    public void shutdown() {
        preservableRegistrationProcessor.shutdown();
    }

    @Override
    public EurekaRegistrationProcessor<InstanceInfo> get() {
        return preservableRegistrationProcessor;
    }

    private static OverridesService combine(SourcedEurekaRegistry sourcedEurekaRegistry, Set<OverridesService> overrideServices) {
        if (overrideServices.isEmpty()) {
            throw new IllegalArgumentException("No override service provided");
        }
        Iterator<OverridesService> it = overrideServices.iterator();
        OverridesService head = it.next();
        OverridesService tail = head;
        while (it.hasNext()) {
            OverridesService next = it.next();
            tail.addOutboundHandler(next);
            tail = next;
        }
        tail.addOutboundHandler(sourcedEurekaRegistry);
        return head;
    }
}
