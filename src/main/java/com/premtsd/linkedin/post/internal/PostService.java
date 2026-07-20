package com.premtsd.linkedin.post.internal;

import com.premtsd.linkedin.post.PostCreatedEvent;
import com.premtsd.linkedin.post.PostLikedEvent;

import com.premtsd.linkedin.post.internal.PostDtos.CreatePostRequest;
import com.premtsd.linkedin.post.internal.PostDtos.PostView;
import com.premtsd.linkedin.uploader.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final ApplicationEventPublisher events;

    // Direct in-process call into the uploader module — this replaces the
    // microservices post-service -> uploader-service Feign client.
    private final FileStorage fileStorage;

    @Transactional
    @CacheEvict(value = "userFeed", key = "#authorId")
    PostView create(Long authorId, CreatePostRequest req) {
        return save(authorId, req.content(), req.imageUrl());
    }

    /** Create a post with an attached image: uploads via the uploader module, then saves. */
    @Transactional
    @CacheEvict(value = "userFeed", key = "#authorId")
    PostView createWithImage(Long authorId, String content, MultipartFile file) {
        String imageUrl = (file != null && !file.isEmpty()) ? fileStorage.store(file) : null;
        return save(authorId, content, imageUrl);
    }

    private PostView save(Long authorId, String content, String imageUrl) {
        Post post = postRepository.save(Post.builder()
                .userId(authorId).content(content).imageUrl(imageUrl).build());
        log.info("Post created id={} author={}", post.getId(), authorId);
        events.publishEvent(new PostCreatedEvent(post.getId(), authorId, post.getContent()));
        return toView(post);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userFeed", key = "#userId")
    List<PostView> feedOf(Long userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    PostView getById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        return toView(post);
    }

    @Transactional
    void like(Long postId, Long likerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        if (likeRepository.existsByPostIdAndUserId(postId, likerId)) {
            return; // idempotent
        }
        likeRepository.save(PostLike.builder().postId(postId).userId(likerId).build());
        events.publishEvent(new PostLikedEvent(postId, likerId, post.getUserId()));
    }

    private PostView toView(Post p) {
        return new PostView(p.getId(), p.getUserId(), p.getContent(), p.getImageUrl(),
                p.getCreatedAt(), likeRepository.countByPostId(p.getId()));
    }
}
