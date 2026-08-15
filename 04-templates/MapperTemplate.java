package com.example.module.mapper;

import com.example.module.entity.Xxx;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Xxx 数据访问接口。
 *
 * <p>继承 BaseMapper 获得通用 CRUD，自定义查询方法写在此处。</p>
 */
@Mapper
public interface XxxMapper extends BaseMapper<Xxx> {

    // 复杂查询方法在此声明，SQL 在 resources/mapper/XxxMapper.xml 中实现
    // 示例：
    // Page<Xxx> selectXxxPage(Page<Xxx> page, @Param("query") XxxQueryDTO xxxQuery);
}
