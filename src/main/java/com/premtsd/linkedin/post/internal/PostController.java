package com.premtsd.linkedin.post.internal;

import com.premtsd.linkedin.shared.SecurityUtils;
import com.premtsd.linkedin.post.internal.PostDtos.CreatePostRequest;
import com.premtsd.linkedin.post.internal.PostDtos.PostView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostView create(@Valid @RequestBody CreatePostRequest req) {
        return postService.create(SecurityUtils.currentUserId(), req);
    }

    /** Create a post with an image file — post module calls the uploader module directly. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PostView createWithImage(@RequestParam("content") String content,
                                    @RequestParam(value = "file", required = false) MultipartFile file) {
        return postService.createWithImage(SecurityUtils.currentUserId(), content, file);
    }

    @GetMapping("/feed")
    public List<PostView> myFeed() {
        return postService.feedOf(SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}")
    public PostView getOne(@PathVariable Long id) {
        return postService.getById(id);
    }

    @PostMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable Long id) {
        postService.like(id, SecurityUtils.currentUserId());
    }
}
