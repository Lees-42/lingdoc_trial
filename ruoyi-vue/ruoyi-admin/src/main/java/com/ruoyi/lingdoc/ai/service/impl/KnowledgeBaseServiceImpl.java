package com.ruoyi.lingdoc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.lingdoc.ai.domain.dto.KbCreateDTO;
import com.ruoyi.lingdoc.ai.domain.dto.KbUpdateDTO;
import com.ruoyi.lingdoc.ai.domain.entity.KbKnowledgeBase;
import com.ruoyi.lingdoc.ai.domain.vo.KbKnowledgeBaseVO;
import com.ruoyi.lingdoc.ai.mapper.KbDocumentChunkMapper;
import com.ruoyi.lingdoc.ai.mapper.KbEmbeddingMapper;
import com.ruoyi.lingdoc.ai.mapper.KbKnowledgeBaseMapper;
import com.ruoyi.lingdoc.ai.service.IKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库Service实现
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KbKnowledgeBaseMapper, KbKnowledgeBase> 
        implements IKnowledgeBaseService {

    @Autowired
    private KbDocumentChunkMapper chunkMapper;
    
    @Autowired
    private KbEmbeddingMapper embeddingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbKnowledgeBaseVO createKnowledgeBase(KbCreateDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }
        String userUuid = loginUser.getUserId().toString();
        
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setKbId("kb_" + UUID.fastUUID().toString(true));
        kb.setUserUuid(userUuid);
        kb.setKbName(dto.getKbName());
        kb.setKbDesc(dto.getKbDesc());
        kb.setEmbeddingModel("text-embedding-3-small");
        kb.setChunkSize(dto.getChunkSize() != null ? dto.getChunkSize() : 512);
        kb.setChunkOverlap(dto.getChunkOverlap() != null ? dto.getChunkOverlap() : 50);
        kb.setStatus(1);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setCreateBy(loginUser.getUsername());
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());
        
        this.save(kb);
        log.info("创建知识库成功: kbId={}, userUuid={}", kb.getKbId(), userUuid);
        
        return convertToVO(kb);
    }

    @Override
    public List<KbKnowledgeBaseVO> listByCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }
        String userUuid = loginUser.getUserId().toString();
        
        List<KbKnowledgeBase> list = baseMapper.selectByUserUuid(userUuid);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public KbKnowledgeBaseVO getByKbId(String kbId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }
        
        KbKnowledgeBase kb = baseMapper.selectByKbId(kbId);
        if (kb == null) {
            return null;
        }
        
        if (!kb.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权访问该知识库");
        }
        
        return convertToVO(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateKnowledgeBase(String kbId, KbUpdateDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }
        
        KbKnowledgeBase kb = baseMapper.selectByKbId(kbId);
        if (kb == null) {
            return false;
        }
        
        if (!kb.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权修改该知识库");
        }
        
        kb.setKbName(dto.getKbName());
        kb.setKbDesc(dto.getKbDesc());
        if (dto.getChunkSize() != null) {
            kb.setChunkSize(dto.getChunkSize());
        }
        if (dto.getChunkOverlap() != null) {
            kb.setChunkOverlap(dto.getChunkOverlap());
        }
        kb.setUpdateBy(loginUser.getUsername());
        kb.setUpdatedAt(LocalDateTime.now());
        
        boolean result = this.updateById(kb);
        log.info("更新知识库: kbId={}, result={}", kbId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteKnowledgeBase(String kbId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }
        
        KbKnowledgeBase kb = baseMapper.selectByKbId(kbId);
        if (kb == null) {
            return false;
        }
        
        if (!kb.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权删除该知识库");
        }
        
        kb.setStatus(0);
        kb.setUpdateBy(loginUser.getUsername());
        kb.setUpdatedAt(LocalDateTime.now());
        this.updateById(kb);
        
        chunkMapper.deleteByKbId(kbId);
        embeddingMapper.deleteByKbId(kbId);
        
        log.info("删除知识库: kbId={}, userUuid={}", kbId, loginUser.getUserId());
        return true;
    }

    @Override
    public void updateDocCount(String kbId, int docCount, int chunkCount) {
        baseMapper.updateDocCount(kbId, docCount, chunkCount);
    }
    
    private KbKnowledgeBaseVO convertToVO(KbKnowledgeBase kb) {
        KbKnowledgeBaseVO vo = new KbKnowledgeBaseVO();
        BeanUtils.copyProperties(kb, vo);
        
        switch (kb.getStatus()) {
            case 0: vo.setStatusName("禁用"); break;
            case 1: vo.setStatusName("启用"); break;
            case 2: vo.setStatusName("构建中"); break;
            default: vo.setStatusName("未知"); break;
        }
        
        return vo;
    }
}
