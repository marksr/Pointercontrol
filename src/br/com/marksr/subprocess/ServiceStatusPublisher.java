package br.com.marksr.subprocess;

public interface ServiceStatusPublisher {
    void setOnStatusChangeListener(ServiceStatusListener l);
    void removeOnStatusChangeListener(ServiceStatusListener l);
}
