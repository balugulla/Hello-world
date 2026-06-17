package org.gulla.service.gulla.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final Long resourceId;

    public ResourceNotFoundException(String resourceType, Long resourceId) {
        super(String.format("%s not found with id: %d", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }
}
