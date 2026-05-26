package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.enums.UploadBusinessType;
import com.campushub.service.FileService;
import com.campushub.vo.file.UploadedFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
