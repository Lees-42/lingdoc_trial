package com.ruoyi.lingdoc.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.lingdoc.knowledge.domain.KbKnowledgeBase;
import com.ruoyi.lingdoc.knowledge.domain.dto.KbKnowledgeBaseDTO;
import com.ruoyi.lingdoc.knowledge.domain.vo.KbKnowledgeBaseVO;
import com.ruoyi.lingdoc.knowledge.mapper.KbKnowledgeBaseMapper;
import com.ruoyi.lingdoc.knowledge.service.IKbKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库Service实现
 */
@Slf4j
@Service
public class KbKnowledgeBaseServiceImpl extends ServiceImpl<KbKnowledgeBaseMapper, KbKnowledgeBase> implements IKbKnowledgeBaseService {

    @Override
    public TableDataInfo listKbKnowledgeBase(KbKnowledgeBaseDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KbKnowledgeBase> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(KbKnowledgeBase::getUserUuid, loginUser.getUserId().toString())
               .eq(KbKnowledgeBase::getStatus, "0")
               .orderByDesc(KbKnowledgeBase::getCreateTime);

        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<KbKnowledgeBase> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(dto.getPageNum(), dto.getPageSize());
        page(page, wrapper);

        // 转换为VO
        List<KbKnowledgeBaseVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        TableDataInfo dataInfo = new TableDataInfo();
        dataInfo.setRows(voList);
        dataInfo.setTotal(page.getTotal());
        return dataInfo;
    }

    @Override
    public KbKnowledgeBaseVO getKbKnowledgeBase(String kbId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        KbKnowledgeBase kb = baseMapper.selectByKbId(kbId);
        
        if (kb == null) {
            return null;
        }
        
        // 检查权限
        if (!kb.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权访问该知识库");
        }
        
        return convertToVO(kb);
    }

    @Override
    public int addKbKnowledgeBase(KbKnowledgeBaseDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 检查名称重复
        int count = baseMapper.countByName(loginUser.getUserId().toString(), dto.getKbName());
        if (count > 0) {
            throw new IllegalArgumentException("知识库名称已存在");
        }

        KbKnowledgeBase kb = new KbKnowledgeBase();
        BeanUtils.copyProperties(dto, kb);
        kb.setKbId(UUID.fastUUID().toString(true));
        kb.setUserUuid(loginUser.getUserId().toString());
        kb.setTotalFiles(0);
        kb.setTotalChunks(0);
        kb.setStatus("0");
        kb.setCreateTime(LocalDateTime.now());
        kb.setUpdateTime(LocalDateTime.now());

        return baseMapper.insert(kb);
    }

    @Override
    public int updateKbKnowledgeBase(KbKnowledgeBaseDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        KbKnowledgeBase existing = baseMapper.selectByKbId(dto.getKbId());
        if (existing == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 检查权限
        if (!existing.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权修改该知识库");
        }

        // 检查名称重复（排除自己）
        if (!existing.getKbName().equals(dto.getKbName())) {
            int count = baseMapper.countByName(loginUser.getUserId().toString(), dto.getKbName());
            if (count > 0) {
                throw new IllegalArgumentException("知识库名称已存在");
            }
        }

        KbKnowledgeBase kb = new KbKnowledgeBase();
        BeanUtils.copyProperties(dto, kb);
        kb.setUpdateTime(LocalDateTime.now());

        return baseMapper.updateById(kb);
    }

    @Override
    public int delKbKnowledgeBase(String kbId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        KbKnowledgeBase existing = baseMapper.selectByKbId(kbId);
        if (existing == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 检查权限
        if (!existing.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权删除该知识库");
        }

        // 软删除
        existing.setStatus("1");
        existing.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(existing);
    }

    @Override
    public List<KbKnowledgeBaseVO> listAccessibleKb(String userId) {
        List<KbKnowledgeBase> list = baseMapper.selectByUserId(userId);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private KbKnowledgeBaseVO convertToVO(KbKnowledgeBase kb) {
        KbKnowledgeBaseVO vo = new KbKnowledgeBaseVO();
        BeanUtils.copyProperties(kb, vo);
        return vo;
    }
}
