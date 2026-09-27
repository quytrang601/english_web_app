package com.ieltsplatform.common.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEntityTest {

    // Concrete subclass to test abstract BaseEntity
    static class DummyEntity extends BaseEntity {
        private String name;

        public DummyEntity() {}

        public DummyEntity(UUID id, String name) {
            setId(id);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class AnotherEntity extends BaseEntity {
        public AnotherEntity(UUID id) {
            setId(id);
        }
    }

    @Test
    @DisplayName("Entities with the same non-null ID should be equal and have matching hashCodes")
    void testEqualsAndHashCode_SameId_ShouldBeEqual() {
        UUID id = UUID.randomUUID();
        DummyEntity entity1 = new DummyEntity(id, "Alpha");
        DummyEntity entity2 = new DummyEntity(id, "Beta");

        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    @DisplayName("Entities with different IDs should not be equal")
    void testEquals_DifferentId_ShouldNotBeEqual() {
        DummyEntity entity1 = new DummyEntity(UUID.randomUUID(), "Alpha");
        DummyEntity entity2 = new DummyEntity(UUID.randomUUID(), "Alpha");

        assertNotEquals(entity1, entity2);
    }

    @Test
    @DisplayName("Entity should equal itself")
    void testEquals_SameInstance_ShouldBeEqual() {
        DummyEntity entity = new DummyEntity(UUID.randomUUID(), "Alpha");

        assertEquals(entity, entity);
    }

    @Test
    @DisplayName("Entity should not equal null or different class")
    void testEquals_NullOrDifferentClass_ShouldNotBeEqual() {
        UUID id = UUID.randomUUID();
        DummyEntity entity1 = new DummyEntity(id, "Alpha");
        AnotherEntity entity2 = new AnotherEntity(id);

        assertNotEquals(entity1, null);
        assertNotEquals(entity1, entity2);
    }

    @Test
    @DisplayName("Entities with null IDs should not be equal unless they are the same instance")
    void testEquals_NullIds_ShouldOnlyEqualSameInstance() {
        DummyEntity entity1 = new DummyEntity();
        DummyEntity entity2 = new DummyEntity();

        assertEquals(entity1, entity1);
        assertNotEquals(entity1, entity2);
    }

    @Test
    @DisplayName("Audit fields and version getters and setters work correctly")
    void testFieldGettersAndSetters() {
        DummyEntity entity = new DummyEntity();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Long version = 1L;

        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setVersion(version);

        assertEquals(id, entity.getId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
        assertEquals(version, entity.getVersion());
    }
}
