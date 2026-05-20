package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.entity.FileRecord;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.FileRecordMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.file.UploadedFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
/*
这是这次文件上传最核心的类。

它负责做的事情是：

接收上传文件
检查文件是否合法
检查大小是否超限
检查扩展名和图片类型是否允许
生成唯一文件名
把文件保存到本地 uploads/
往 file_record 表里插入一条记录
返回 UploadedFileVO
你可以把它理解成：

真正处理上传业务的人

它不是简单“把文件存一下”，而是把整个上传流程都串起来了。

它里面最重要的几个点
upload(...)
主方法，上传流程从这里开始
validateFile(...)
做文件大小、扩展名、类型校验
extractExtension(...)
从文件名里取出扩展名
allowedExtensionSet()
把配置里的允许扩展名转成集合，方便校验
它的意义
这个类让后端第一次真正具备了：

把前端上传图片，变成“磁盘文件 + 数据库记录 + 可访问 URL”的完整能力。
*/
@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final FileRecordMapper fileRecordMapper;

    @Value("${campus-hub.file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${campus-hub.file.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensions;

    @Value("${campus-hub.file.max-file-size:10485760}")
    private long maxFileSize;

    @Transactional
    public UploadedFileVO upload(MultipartFile file, UploadBusinessType businessType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename();
        String safeFileName = originalName == null || originalName.isBlank() ? "upload.bin" : originalName;
        String extension = extractExtension(safeFileName);
        validateFile(file, extension);

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path targetFile = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件保存失败");
        }

        String fileUrl = "/uploads/" + storedFileName;
        FileRecord record = new FileRecord();
        record.setUserId(currentUserId);
        record.setFileName(safeFileName);
        record.setFileUrl(fileUrl);
        record.setFileSize(file.getSize());
        record.setPurpose(businessType.name());
        fileRecordMapper.insert(record);

        return new UploadedFileVO(
                record.getId(),
                businessType,
                safeFileName,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize(),
                fileUrl,
                record.getCreatedAt() != null ? record.getCreatedAt() : LocalDateTime.now()
        );
    }

    private void validateFile(MultipartFile file, String extension) {
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        if (!allowedExtensionSet().contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 jpg、jpeg、png、gif、webp");
        }

        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持图片文件上传");
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件必须包含扩展名");
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private Set<String> allowedExtensionSet() {
        return Set.of(allowedExtensions.split(","));
    }
}
