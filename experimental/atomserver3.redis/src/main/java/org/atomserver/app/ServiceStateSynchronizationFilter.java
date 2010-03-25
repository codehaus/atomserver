package org.atomserver.app;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.atomserver.core.Substrate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceStateSynchronizationFilter implements ContainerRequestFilter {

    private final Substrate substrate;

    @Autowired
    public ServiceStateSynchronizationFilter(Substrate substrate) {
        this.substrate = substrate;
    }

    public ContainerRequest filter(ContainerRequest containerRequest) {
        return containerRequest;
    }
}
