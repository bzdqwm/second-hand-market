package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类目表 category
 * <p>
 * 二级树结构：parentId = 0 表示一级类目。
 */
@Data
@TableName("category")
public class Category {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父类目 ID，0 为一级 */
    private Long parentId;

    /** 类目名 */
    private String name;

    /** 排序，越小越前 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;
}