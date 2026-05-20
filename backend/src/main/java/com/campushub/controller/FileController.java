package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.enums.UploadBusinessType;
import com.campushub.service.FileService;
import com.campushub.vo.file.UploadedFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
/*
这个类是上传接口的入口。

它暴露的接口是：

POST /api/files/upload
前端上传时会传两样东西：

file
businessType
FileController 的作用就是：

把这两个参数接住
调用 FileService.upload(...)
把结果返回给前端
你可以把它理解成：

上传模块的前台接待员

它自己不负责保存文件、不负责校验，而是把活交给 FileService。
*/
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<UploadedFileVO> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam("businessType") UploadBusinessType businessType) {
        return ApiResponse.success(fileService.upload(file, businessType));
    }
}
