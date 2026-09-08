package com.familyhub.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shared_list")
@Getter
@Setter
public class SharedList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListCategoryDisplayMode categoryDisplayMode;

    private Boolean showCompletedOverride;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    // createdAt is a non-unique @CreationTimestamp; items created in one bulk flush tie on it.
    // The id tiebreaker makes reads deterministic/stable (though not insertion-ordered for tied
    // rows — strict request order is guaranteed only on the bulk POST response, which slices the
    // in-memory insertion-ordered collection).
    @OrderBy("createdAt ASC, id ASC")
    private List<SharedListItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
