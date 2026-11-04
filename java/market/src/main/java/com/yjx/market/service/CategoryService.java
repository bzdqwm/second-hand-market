package com.yjx.market.service;

import com.yjx.market.pojo.entity.Category;

import java.util.List;

/**
 * 类目业务接口。
 */
public interface CategoryService {

    /** 查询全部类目，按 sort 升序 */
    List<Category> listAll();
}