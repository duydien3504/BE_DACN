package com.example.DACN.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@NamedEntityGraph(name = "Category.withSubCategories", attributeNodes = @NamedAttributeNode("subCategories"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    Long categoryId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(nullable = false, unique = true, length = 150)
    String slug;

    @Column(name = "icon_url")
    String iconUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parent; // Self-referencing for parent category

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false;

    @OneToMany(mappedBy = "parent")
    Set<Category> subCategories;

    @OneToMany(mappedBy = "category")
    Set<Product> products;
}
