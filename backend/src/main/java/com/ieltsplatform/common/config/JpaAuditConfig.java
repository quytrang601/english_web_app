package com.ieltsplatform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing to automatically populate @CreatedDate and @LastModifiedDate
 * on entities extending {@link com.ieltsplatform.common.base.BaseEntity}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
