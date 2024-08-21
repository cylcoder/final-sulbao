package com.finalproject.sulbao.board.service;

import com.finalproject.sulbao.board.domain.BoardCategory;
import com.finalproject.sulbao.board.domain.Post;
import com.finalproject.sulbao.board.domain.PostImage;
import com.finalproject.sulbao.board.dto.PostDto;
import com.finalproject.sulbao.board.dto.ZzanfeedRequestDto;
import com.finalproject.sulbao.board.repository.BoardCategoryRepository;
import com.finalproject.sulbao.board.repository.PostRepository;
import com.finalproject.sulbao.common.file.FileService;
import com.finalproject.sulbao.login.model.entity.Login;
import com.finalproject.sulbao.login.model.repository.LoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.finalproject.sulbao.board.common.BoardCategoryConstants.*;
import static com.finalproject.sulbao.board.domain.PostImage.createPostImage;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    @Value("${file.upload-dir}")
    private String uploadDir;
    private final PostRepository postRepository;
    private final LoginRepository loginRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final FileService fileService;

    public void updateHit(Long id) {
        postRepository.findById(id).orElseThrow().updateHit();
    }

    public void delete(Long id) {
        postRepository.deleteById(id);
    }

    public PostDto save(Long userId, String title, String content, Long boardCategoryId, String thumbnail) {
        Login login = loginRepository.findById(userId).orElseThrow();
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        Post post = Post.createPost(login, boardCategory, title, content, thumbnail);
        Post savedPost = postRepository.save(post);
        return PostDto.toPostDto(savedPost);
    }

    public PostDto save(Long userId, Long boardCategoryId, ZzanfeedRequestDto requestDto) {
        Login login = loginRepository.findById(userId).orElseThrow();
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        String thumbnail = fileService.uploadFiles(requestDto.getThumbnail(), uploadDir).getUploadFileName();
        List<String> tags = requestDto.getTags();
        String title = requestDto.getTitle();
        String content = String.join("|", requestDto.getContents());

        List<PostImage> postImages = requestDto.getContentImages().stream()
                .map(contentImage -> {
                    String fileName = fileService.uploadFiles(contentImage, uploadDir).getUploadFileName();
                    return createPostImage(fileName);
                })
                .toList();

        Post post = Post.createPost(login, boardCategory, title, content, thumbnail, postImages, tags);
        postRepository.save(post);
        return PostDto.toPostDto(post);
    }

    public List<PostDto> loadMorePosts(Long boardCategoryId, int page) {
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        int pageSize = (boardCategoryId.equals(ZZANPOST_ID) ? ZZANPOST_PAGE_SIZE : ZZANFEED_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Post> posts = postRepository.findByBoardCategory(boardCategory, pageable);
        return posts.getContent().stream().map(PostDto::toPostDto).toList();
    }

    public List<PostDto> loadMorePosts(Long boardCategoryId, int page, String tag) {
        if (tag.isEmpty()) {
            return loadMorePosts(boardCategoryId, page);
        }
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        int pageSize = (boardCategoryId.equals(ZZANPOST_ID) ? ZZANPOST_PAGE_SIZE : ZZANFEED_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Post> posts = postRepository.findByBoardCategoryAndTag(boardCategory, "#" + tag, pageable);
        return posts.getContent().stream().map(PostDto::toPostDto).toList();
    }

    public PostDto findById(Long id) {
        Post post = postRepository.findById(id).orElseThrow();
        return PostDto.toPostDto(post);
    }

    public void update(Long postId, String title, String content, String thumbnailFileName) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.update(title, content, thumbnailFileName);
    }

    public void update(Long postId, ZzanfeedRequestDto requestDto) {
        Post post = postRepository.findById(postId).orElseThrow();

        MultipartFile thumbnailFile = requestDto.getThumbnail();
        String thumbnail = null;
        if (thumbnailFile != null) {
            thumbnail = fileService.uploadFiles(thumbnailFile, uploadDir).getUploadFileName();
        }

        String title = requestDto.getTitle();
        List<String> tags = requestDto.getTags();
        String content = String.join("|", requestDto.getContents());

        List<MultipartFile> contentImages = requestDto.getContentImages();

        List<PostImage> postImages = new ArrayList<>();
        for (MultipartFile contentImage : contentImages) {
            if (contentImage.isEmpty()) {
                postImages.add(null);
            } else {
                String fileName = fileService.uploadFiles(contentImage, uploadDir).getUploadFileName();
                postImages.add(PostImage.createPostImage(fileName));
            }
        }

        post.update(thumbnail, title, content, tags, postImages);
    }

    public Page<PostDto> getPostPage(Long boardCategoryId, int page) {
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        int pageSize = (boardCategoryId.equals(ZZANFEED_ID) ? ZZANFEED_PAGE_SIZE : ZZANPOST_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Post> postPage = postRepository.findByBoardCategory(boardCategory, pageable);
        return postPage.map(PostDto::toPostDto);
    }

    public Long count(Long boardCategoryId) {
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        return postRepository.countByBoardCategory(boardCategory);
    }

    public Page<PostDto> getPostPage(Long boardCategoryId, int page, String tag) {
        if (tag == null) {
            return getPostPage(boardCategoryId, page);
        }

        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        int pageSize = (boardCategoryId.equals(ZZANFEED_ID) ? ZZANFEED_PAGE_SIZE : ZZANPOST_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Post> postPage = postRepository.findByBoardCategoryAndTag(boardCategory, "#" + tag, pageable);
        return postPage.map(PostDto::toPostDto);
    }

    public Long count(Long boardCategoryId, String tag) {
        if (tag == null) {
            return count(boardCategoryId);
        }

        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        return postRepository.countByBoardCategoryAndTag(boardCategory, "#" + tag);
    }

    public List<String> findTopTags() {
        return postRepository.findTopTags().stream().limit(15).toList();
    }

    public List<PostDto> findByCategoryAndKeyword(String keyword, Long boardCategoryId) {
        BoardCategory boardCategory = boardCategoryRepository.findById(boardCategoryId).orElseThrow();
        if (boardCategoryId.equals(ZZANFEED_ID)) {
            List<Post> posts = postRepository.findByBoardCategoryAndKeyword(boardCategory, keyword);
            return posts.stream().limit(ZZANFEED_SEARCH_PAGE_SIZE).map(PostDto::toPostDto).toList();
        } else {
            List<Post> posts = postRepository.findByBoardCategoryAndKeyword(boardCategory, keyword);
            return posts.stream().limit(ZZANPOST_SEARCH_PAGE_SIZE).map(PostDto::toPostDto).toList();
        }
    }

    public List<PostDto> findByUser(Long id) {
        Login login = loginRepository.findById(id).orElseThrow();
        List<Post> posts = postRepository.findAllByLogin(login);
        return posts.stream().map(PostDto::toPostDto).toList();
    }
}
