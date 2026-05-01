package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.annotation.NeedLogin;
import com.banny.lotsonote.mapper.CollectionMapper;
import com.banny.lotsonote.mapper.CollectionNoteMapper;
import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.EmptyVO;
import com.banny.lotsonote.model.dto.collection.CollectionQueryParams;
import com.banny.lotsonote.model.dto.collection.CreateCollectionBody;
import com.banny.lotsonote.model.dto.collection.UpdateCollectionBody;
import com.banny.lotsonote.model.entity.Collection;
import com.banny.lotsonote.model.entity.CollectionNote;
import com.banny.lotsonote.model.vo.collection.CollectionVO;
import com.banny.lotsonote.model.vo.collection.CreateCollectionVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.CollectionService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class CollectionServiceImpl implements CollectionService {
    @Autowired
    private RequestScopeData requestScopeData;

    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private CollectionNoteMapper collectionNoteMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Override
    public ApiResponse<List<CollectionVO>> getCollections(CollectionQueryParams queryParams) {
        // 收藏夹列表
        List<Collection> collections = collectionMapper.findByCreatorId(queryParams.getCreatorId());
        List<Integer> collectionIds = collections.stream().map(Collection::getCollectionId).toList();
        final Set<Integer> collectedNoteIdCollectionIds;

        // 查看是否传入了 noteId
        if (queryParams.getNoteId() != null) {
            // 收藏了 noteId 的收藏夹列表
            collectedNoteIdCollectionIds = collectionNoteMapper.filterCollectionIdsByNoteId(queryParams.getNoteId(), collectionIds);
        } else {
            collectedNoteIdCollectionIds = Collections.emptySet();
        }

        // 将 collections 映射为 CollectionVOList
        List<CollectionVO> collectionVOList = collections.stream().map(collection -> {
            CollectionVO collectionVO = new CollectionVO();
            BeanUtils.copyProperties(collection, collectionVO);

            // 检查是否传入了 noteId 参数并且当前收藏夹收藏了该 note
            if (queryParams.getNoteId() == null) return collectionVO;

            // 设置收藏夹收藏笔记状态
            CollectionVO.NoteStatus noteStatus = new CollectionVO.NoteStatus();

            noteStatus.setIsCollected(collectedNoteIdCollectionIds.contains(collection.getCollectionId()));
            noteStatus.setNoteId(queryParams.getNoteId());
            collectionVO.setNoteStatus(noteStatus);

            return collectionVO;
        }).toList();

        return ApiResponseUtil.success("获取收藏夹列表成功", collectionVOList);
    }

    @Override
    @NeedLogin
    public ApiResponse<CreateCollectionVO> createCollection(CreateCollectionBody requestBody) {

        Long creatorId = requestScopeData.getUserId();

        Collection collection = new Collection();
        BeanUtils.copyProperties(requestBody, collection);
        collection.setCreatorId(creatorId);

        try {
            collectionMapper.insert(collection);
            CreateCollectionVO createCollectionVO = new CreateCollectionVO();

            createCollectionVO.setCollectionId(collection.getCollectionId());
            return ApiResponseUtil.success("创建成功", createCollectionVO);
        } catch (Exception e) {
            return ApiResponseUtil.error("创建失败");
        }
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> deleteCollection(Integer collectionId) {
        // 校验是否是收藏夹创建者
        Long creatorId = requestScopeData.getUserId();
        Collection collection = collectionMapper.findByIdAndCreatorId(collectionId, creatorId);

        if (collection == null) {
            return ApiResponseUtil.error("收藏夹不存在或者无权限删除");
        }

        try {
            // 删除收藏夹
            collectionMapper.deleteById(collectionId);
            // 删除收藏夹中的所有的笔记
            collectionNoteMapper.deleteByCollectionId(collectionId);
            return ApiResponseUtil.success("删除成功");
        } catch (Exception e) {
            return ApiResponseUtil.error("删除失败");
        }
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> batchModifyCollection(UpdateCollectionBody requestBody) {

        Long userId = requestScopeData.getUserId();
        Integer noteId = requestBody.getNoteId();

        UpdateCollectionBody.UpdateItem[] collections = requestBody.getCollections();

        for (UpdateCollectionBody.UpdateItem collection : collections) {
            Integer collectionId = collection.getCollectionId();
            String action = collection.getAction();

            // 校验是否是收藏夹创建者
            Collection collectionEntity = collectionMapper.findByIdAndCreatorId(collectionId, userId);

            if (collectionEntity == null) {
                return ApiResponseUtil.error("收藏夹不存在或者无权限操作");
            }

            if ("create".equals(action)) {
                int collectedCountBefore = collectionMapper.countByCreatorIdAndNoteId(userId, noteId);
                CollectionNote collectionNote = new CollectionNote();
                collectionNote.setCollectionId(collectionId);
                collectionNote.setNoteId(noteId);
                int inserted = collectionNoteMapper.insert(collectionNote);
                if (inserted > 0 && collectedCountBefore == 0) {
                    noteMapper.collectNote(noteId);
                }
            }

            if ("delete".equals(action)) {
                int collectedCountBefore = collectionMapper.countByCreatorIdAndNoteId(userId, noteId);
                int deleted = collectionNoteMapper.deleteByCollectionIdAndNoteId(collectionId, noteId);
                if (deleted > 0 && collectedCountBefore == 1) {
                    noteMapper.unCollectNote(noteId);
                }
            }
        }
        return ApiResponseUtil.success("操作成功");
    }
}
