package com.example.module.controller;

import com.example.common.Response;
import com.example.module.dto.XxxCreateDTO;
import com.example.module.dto.XxxQueryDTO;
import com.example.module.dto.XxxUpdateDTO;
import com.example.module.service.XxxService;
import com.example.module.vo.XxxVO;
import com.example.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;

/**
 * Xxx 模块接口。
 *
 * <p>提供 Xxx 的查询、创建、更新、删除能力。</p>
 */
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
@Validated
public class XxxController {

    private final XxxService xxxService;

    /**
     * 查询单条。
     *
     * @param id Xxx ID
     * @return Xxx 详情
     */
    @GetMapping("/{id}")
    public Response<XxxVO> getById(@PathVariable @NotNull(message = "ID不能为空") Long id) {
        return Response.success(xxxService.getById(id));
    }

    /**
     * 分页查询。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    public Response<PageResult<XxxVO>> queryPage(@Validated XxxQueryDTO xxxQuery) {
        return Response.success(xxxService.queryPage(xxxQuery));
    }

    /**
     * 创建。
     *
     * @param createInfo 创建入参
     * @return 新记录 ID
     */
    @PostMapping
    public Response<Long> create(@Validated @RequestBody XxxCreateDTO createInfo) {
        return Response.success(xxxService.create(createInfo));
    }

    /**
     * 更新。
     *
     * @param id  Xxx ID
     * @param updateInfo 更新入参
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Response<Void> update(@PathVariable Long id, @Validated @RequestBody XxxUpdateDTO updateInfo) {
        xxxService.update(id, updateInfo);
        return Response.success();
    }

    /**
     * 删除。
     *
     * @param id Xxx ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id) {
        xxxService.delete(id);
        return Response.success();
    }
}
