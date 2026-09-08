package com.familyhub.demo.security;

import com.familyhub.demo.model.Family;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;
import java.util.UUID;

public class WithMockFamilySecurityContextFactory implements WithSecurityContextFactory<WithMockFamily> {

    @Override
    public SecurityContext createSecurityContext(WithMockFamily annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Family family = new Family();
        family.setId(UUID.fromString(annotation.familyId()));
        family.setUsername(annotation.username());
        family.setName(annotation.familyName());
        family.setPasswordHash("$2a$10$dummyhashfortesting");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        family,
                        null,
                        Collections.emptyList()
                );

        context.setAuthentication(authentication);
        return context;
    }
}
