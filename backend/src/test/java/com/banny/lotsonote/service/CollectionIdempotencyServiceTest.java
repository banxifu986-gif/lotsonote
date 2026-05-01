package com.banny.lotsonote.service;

import com.banny.lotsonote.mapper.CollectionMapper;
import com.banny.lotsonote.mapper.CollectionNoteMapper;
import com.banny.lotsonote.mapper.NoteMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.EmptyVO;
import com.banny.lotsonote.model.dto.collection.UpdateCollectionBody;
import com.banny.lotsonote.model.entity.Collection;
import com.banny.lotsonote.service.impl.CollectionServiceImpl;
import com.banny.lotsonote.scope.RequestScopeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CollectionIdempotencyServiceTest {

    @Mock
    private RequestScopeData requestScopeData;

    @Mock
    private CollectionMapper collectionMapper;

    @Mock
    private CollectionNoteMapper collectionNoteMapper;

    @Mock
    private NoteMapper noteMapper;

    private CollectionServiceImpl collectionService;

    @BeforeEach
    void setUp() {
        collectionService = new CollectionServiceImpl();
        ReflectionTestUtils.setField(collectionService, "requestScopeData", requestScopeData);
        ReflectionTestUtils.setField(collectionService, "collectionMapper", collectionMapper);
        ReflectionTestUtils.setField(collectionService, "collectionNoteMapper", collectionNoteMapper);
        ReflectionTestUtils.setField(collectionService, "noteMapper", noteMapper);
    }

    @Test
    void createCollectionRelationShouldNotIncreaseCountWhenAlreadyCollected() {
        UpdateCollectionBody body = buildBody(1, "create");
        Collection collection = new Collection();
        collection.setCollectionId(1);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(collectionMapper.findByIdAndCreatorId(1, 10L)).thenReturn(collection);
        when(collectionMapper.countByCreatorIdAndNoteId(10L, 100)).thenReturn(1);
        when(collectionNoteMapper.insert(any())).thenReturn(0);

        ApiResponse<EmptyVO> response = collectionService.batchModifyCollection(body);

        assertEquals(200, response.getCode());
        verify(noteMapper, never()).collectNote(100);
    }

    @Test
    void deleteCollectionRelationShouldNotDecreaseCountWhenAlreadyUncollected() {
        UpdateCollectionBody body = buildBody(1, "delete");
        Collection collection = new Collection();
        collection.setCollectionId(1);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(collectionMapper.findByIdAndCreatorId(1, 10L)).thenReturn(collection);
        when(collectionMapper.countByCreatorIdAndNoteId(10L, 100)).thenReturn(0);
        when(collectionNoteMapper.deleteByCollectionIdAndNoteId(1, 100)).thenReturn(0);

        ApiResponse<EmptyVO> response = collectionService.batchModifyCollection(body);

        assertEquals(200, response.getCode());
        verify(noteMapper, never()).unCollectNote(100);
    }

    @Test
    void deleteCollectionRelationShouldNotDecreaseCountWhenOtherCollectionsStillExist() {
        UpdateCollectionBody body = buildBody(1, "delete");
        Collection collection = new Collection();
        collection.setCollectionId(1);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(collectionMapper.findByIdAndCreatorId(1, 10L)).thenReturn(collection);
        when(collectionMapper.countByCreatorIdAndNoteId(10L, 100)).thenReturn(2);
        when(collectionNoteMapper.deleteByCollectionIdAndNoteId(1, 100)).thenReturn(1);

        ApiResponse<EmptyVO> response = collectionService.batchModifyCollection(body);

        assertEquals(200, response.getCode());
        verify(noteMapper, never()).unCollectNote(100);
    }

    @Test
    void deleteCollectionRelationShouldDecreaseCountWhenDeletingLastCollection() {
        UpdateCollectionBody body = buildBody(1, "delete");
        Collection collection = new Collection();
        collection.setCollectionId(1);

        when(requestScopeData.getUserId()).thenReturn(10L);
        when(collectionMapper.findByIdAndCreatorId(1, 10L)).thenReturn(collection);
        when(collectionMapper.countByCreatorIdAndNoteId(10L, 100)).thenReturn(1);
        when(collectionNoteMapper.deleteByCollectionIdAndNoteId(1, 100)).thenReturn(1);

        ApiResponse<EmptyVO> response = collectionService.batchModifyCollection(body);

        assertEquals(200, response.getCode());
        verify(noteMapper).unCollectNote(100);
    }

    private UpdateCollectionBody buildBody(Integer collectionId, String action) {
        UpdateCollectionBody body = new UpdateCollectionBody();
        body.setNoteId(100);

        UpdateCollectionBody.UpdateItem item = new UpdateCollectionBody.UpdateItem();
        item.setCollectionId(collectionId);
        item.setAction(action);
        body.setCollections(new UpdateCollectionBody.UpdateItem[]{item});
        return body;
    }
}
