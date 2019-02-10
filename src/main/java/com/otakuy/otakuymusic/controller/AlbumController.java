package com.otakuy.otakuymusic.controller;

import com.otakuy.otakuymusic.model.Album;
import com.otakuy.otakuymusic.model.Result;
import com.otakuy.otakuymusic.model.douban.AlbumSuggestion;
import com.otakuy.otakuymusic.service.AlbumService;
import com.otakuy.otakuymusic.util.AlbumUtil;
import com.otakuy.otakuymusic.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
public class AlbumController {
    private final AlbumService albumService;
    private final AlbumUtil albumUtil;
    private final JWTUtil jwtUtil;

    @Autowired
    public AlbumController(AlbumService albumService, AlbumUtil albumUtil, JWTUtil jwtUtil) {
        this.albumService = albumService;
        this.albumUtil = albumUtil;
        this.jwtUtil = jwtUtil;
    }

    //增加新的专辑
    @PostMapping("/albums")
    public Mono<ResponseEntity<Result<Album>>> create(@RequestHeader("Authorization") String token, @RequestBody Album album) {
        return albumService.create(albumUtil.initNew(token, album)).map(newAlbum -> ResponseEntity.status(HttpStatus.CREATED).body(new Result<>("创建成功", newAlbum)));
    }

    //删除专辑(审核不通过专辑也可以删除)
    @DeleteMapping("/albums/{album_id}")
    public Mono<ResponseEntity<Result<String>>> delete(@RequestHeader("Authorization") String token, @PathVariable("album_id") String album_id) {
        return albumService.findById(album_id).map(album -> {
            albumUtil.checkAuthority(token, album);
            albumService.delete(album).subscribe();
            return ResponseEntity.status(HttpStatus.OK).body(new Result<String>("删除成功"));
        }).defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>("专辑不存在")));
    }

    //修改专辑(审核不通过专辑也可以修改)
    @PutMapping("/albums")
    public Mono<ResponseEntity<Result<Album>>> update(@RequestHeader("Authorization") String token, @RequestBody Album album) {
        return albumService.findById(album.getId()).map(oldAlbum -> {
            albumUtil.checkAuthority(token, oldAlbum);
            Album newAlbum = albumUtil.update(oldAlbum, album);
            albumService.save(newAlbum).subscribe();
            return ResponseEntity.status(HttpStatus.OK).body(new Result<>("更新成功", newAlbum));
        }).defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>("专辑不存在")));
    }

    //上传指定专辑的封面
    @PostMapping(value = "/albums/{album_id}/covers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Result<String>>> uploadCover(@RequestHeader("Authorization") String token, @PathVariable("album_id") String album_id, @RequestPart("file") FilePart filePart) throws IOException {
        return albumService.findById(album_id).map(album -> {
            albumUtil.checkAuthority(token, album);
            String url = null;
            try {
                url = albumService.uploadCover(album_id, filePart);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(new Result<>("上传专辑封面成功", url));
        });
    }

    //查看专辑详细
    @GetMapping("/albums/{album_id}")
    public Mono<ResponseEntity<Result<Album>>> findById(@RequestHeader("Authorization") String token, @PathVariable("album_id") String album_id) {
        return albumService.findById(album_id).map(album -> {
                    if (!(albumUtil.checkPermission(token, album) || albumUtil.checkAuthorityWithoutThrowException(token, album)))
                        album.setDownloadRes(null);
                    return ResponseEntity.status(HttpStatus.OK).body(new Result<>("success", album));
                }
        ).defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>("专辑不存在")));
    }

    //查找指定用户的所有维护的专辑(包含通过与没通过)
    @GetMapping("/uers/{owner}/albums")
    public Mono<ResponseEntity<Result<List<Album>>>> findAllByOwner(@PathVariable("owner") String owner/*, @RequestParam Integer offset, @RequestParam Integer limit*/) {
        return albumService.findAllByOwner(owner).collectList().map(albums -> {
            albums.forEach(album -> album.setDownloadRes(null));
            return ResponseEntity.ok(new Result<>("共有" + albums.size() + "张维护专辑", albums));
        }).defaultIfEmpty(ResponseEntity.ok(new Result<>("该用户不存在或者没有专辑", null)));
    }

    //获取首页轮播展示专辑 只返回专辑cover title intro
    @GetMapping("/albums/recommendAlbum")
    public Mono<ResponseEntity<Result<List<Album>>>> findAllByIsRecommend() {
        return albumService.findAllByIsRecommend().collectList().map(albums -> {
            albums.forEach(album -> album.setDownloadRes(null));
            return ResponseEntity.ok(new Result<>("共有" + albums.size() + "张置顶专辑", albums));
        });
    }

    //依赖豆瓣api根据指定专辑名匹配专辑
    @GetMapping("/douban")
    public Mono<ResponseEntity<Result<List<AlbumSuggestion>>>> getAlbumSuggestionByDouban(@RequestParam String title) throws UnsupportedEncodingException {
        return Mono.just(ResponseEntity.ok(new Result<>("ok", albumService.getAlbumSuggestionByDouban(title))));
    }

    //依赖豆瓣api获取专辑详细信息
    @GetMapping("/douban/{douban_id}")
    public Mono<ResponseEntity<Result<Album>>> getAlbumDetailByDouban(@PathVariable("douban_id") String douban_id) throws IOException {
        return Mono.just(ResponseEntity.ok(new Result<>("ok", albumService.getAlbumDetailByDouban(douban_id))));
    }

    /*    //确认修改
        @PostMapping("/test")
        public Mono<ResponseEntity<Result<Album>>> test(@RequestHeader("Authorization") String token, @RequestBody Revision revision) {
            return albumService.modify(revision).map(album -> ResponseEntity.ok(new Result<>(null, album)));
        }*/

}
