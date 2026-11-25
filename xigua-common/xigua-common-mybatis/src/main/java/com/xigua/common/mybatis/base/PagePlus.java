package com.xigua.common.mybatis.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.Serializable;

/**
 * @ClassName Page
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/1 18:03
 */
public class PagePlus<T> extends Page<T> implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 从 MyBatis Plus Page 转换
     */
    public static <T> PagePlus<T> fromPage(Page<T> page) {
        PagePlus<T> pagePlus = new PagePlus<>();

        // 根据你的字段名设置
        if (pagePlus.getRecords() != null) {
            pagePlus.setRecords(page.getRecords());
        }

        pagePlus.setTotal(page.getTotal());
        pagePlus.setSize(page.getSize());
        pagePlus.setCurrent(page.getCurrent());

        return pagePlus;
    }
}
