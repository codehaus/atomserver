package org.atomserver.directory;

import org.apache.abdera.model.Service;

public interface ServiceDirectory {
    AtomServerService get(String name);

    Service remove(String name);

    void put(String name, Service service);

    Iterable<Service> list();
}
