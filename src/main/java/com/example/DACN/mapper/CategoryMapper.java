package com.example.DACN.mapper;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.dto.response.CategoryTreeResponse;
import com.example.DACN.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(source = "categoryId", target = "categoryId")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "iconUrl", target = "iconUrl")
    @Mapping(source = "parent.categoryId", target = "parentId")
    @Mapping(source = "hasDeleted", target = "hasDeleted")
    CategoryResponse toCategoryResponse(Category category);

    @Mapping(source = "categoryId", target = "categoryId")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "iconUrl", target = "iconUrl")
    @Mapping(source = "hasDeleted", target = "hasDeleted")
    @Mapping(target = "children", ignore = true)
    CategoryTreeResponse toCategoryTreeResponse(Category category);
}
