package com.yjx.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yjx.market.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper                                    // ← 每个接口都要加一遍
public interface CategoryMapper extends BaseMapper<Category> {
}
