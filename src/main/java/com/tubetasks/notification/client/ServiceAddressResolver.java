package com.tubetasks.notification.client;

import java.net.URI;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;

@Component
public class ServiceAddressResolver {

    private final LoadBalancerClient loadBalancerClient;

    public ServiceAddressResolver(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    public URI resolve(String url) {
        URI uri = URI.create(url);
        if (!"lb".equalsIgnoreCase(uri.getScheme())) {
            return uri;
        }
        String serviceId = uri.getHost();
        ServiceInstance instance = loadBalancerClient.choose(serviceId);
        if (instance == null) {
            throw new IllegalStateException("No instance registered for " + serviceId);
        }
        return loadBalancerClient.reconstructURI(instance, uri);
    }
}
