package com.yjx.market.pojo.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 通用分页返回结构。
 * <p>
 * 为什么不直接把 MyBatis-Plus 的 Page 对象返回给前端？
 * 因为它带一堆前端用不上的字段（optimizeCountSql、searchCount、orders…），
 * 而且字段名（records/size/current）不好看。
 */
@Data
public class PageVO<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总条数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页条数 */
    private long size;

    /** 总页数 */
    private long pages;

    /**
     * 从 MyBatis-Plus 的分页对象构造。
     *
     * @param source  查询出来的分页对象（里面是实体）
     * @param records 转换好的 VO 列表
     */
    public static <T> PageVO<T> of(IPage<?> source, List<T> records) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(source.getTotal());
        vo.setPage(source.getCurrent());
        vo.setSize(source.getSize());
        vo.setPages(source.getPages());
        return vo;
    }
}