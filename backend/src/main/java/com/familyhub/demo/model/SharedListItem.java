package com.familyhub.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_list_item")
@Getter
@Setter
public class SharedListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "list_id", nullable = false)
    private SharedList list;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_kind", nullable = false, updatable = false, length = 20)
    private ListKind listKind;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ListCategory category;

    @Column(nullable = false, length = 100)
    private String text;

    @Column(nullable = false)
    private boolean completed;

    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void syncScopeFromList() {
        if (list != null) {
            familyId = list.getFamily().getId();
            listKind = list.getKind();
        }
    }
}
