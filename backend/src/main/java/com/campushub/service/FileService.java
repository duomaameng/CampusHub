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
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

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

    @Value("${campus-hub.file.allowed-document-extensions:pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip}")
    private String allowedDocumentExtensions;

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
        validateFile(file, extension, businessType);

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = storageDirectory(businessType.name());
        Path targetFile = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件保存失败");
        }

        String fileUrl = UploadBusinessType.CHAT_FILE.equals(businessType)
                ? "/private-uploads/" + storedFileName
                : "/uploads/" + storedFileName;
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

    public FileRecord requireOwnedFile(Long fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件记录不存在");
        }

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!currentUserId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能使用自己上传的文件");
        }
        return record;
    }

    public FileRecord requireFile(Long fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件记录不存在");
        }
        return record;
    }

    public Path resolveStoredFile(FileRecord record) {
        String fileUrl = record.getFileUrl();
        String storedFileName = fileUrl == null ? "" : fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        if (storedFileName.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        Path targetDirectory = storageDirectory(record.getPurpose());
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();
        if (!targetFile.startsWith(targetDirectory) || !Files.isRegularFile(targetFile)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return targetFile;
    }

    public FileRecord requireOwnedFile(Long fileId, UploadBusinessType expectedPurpose) {
        FileRecord record = requireOwnedFile(fileId);
        if (!expectedPurpose.name().equals(record.getPurpose())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件用途与业务场景不匹配");
        }
        return record;
    }

    public FileRecord requireOwnedFile(String fileUrl, UploadBusinessType expectedPurpose) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件地址不能为空");
        }

        FileRecord record = fileRecordMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getFileUrl, fileUrl)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件记录不存在");
        }

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!currentUserId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己上传的文件");
        }
        if (!expectedPurpose.name().equals(record.getPurpose())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件用途与业务场景不匹配");
        }
        return record;
    }

    @Transactional
    public void deleteOwnedFile(Long fileId) {
        FileRecord record = requireOwnedFile(fileId);
        String fileUrl = record.getFileUrl();
        String storedFileName = fileUrl == null ? "" : fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        if (storedFileName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件地址无效");
        }

        Path targetDirectory = storageDirectory(record.getPurpose());
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();
        if (!targetFile.startsWith(targetDirectory)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件路径无效");
        }

        fileRecordMapper.deleteById(fileId);
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件删除失败");
        }
    }

    private void validateFile(MultipartFile file, String extension, UploadBusinessType businessType) {
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        if (UploadBusinessType.CHAT_FILE.equals(businessType) || UploadBusinessType.TASK_FILE.equals(businessType)) {
            if (!extensionSet(allowedDocumentExtensions).contains(extension.toLowerCase())) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 pdf、doc、docx、xls、xlsx、ppt、pptx、txt、zip");
            }
        } else {
            if (!extensionSet(allowedExtensions).contains(extension.toLowerCase())) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 jpg、jpeg、png、gif、webp");
            }

            String contentType = file.getContentType();
            if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持图片文件上传");
            }
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件必须包含扩展名");
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private Set<String> extensionSet(String extensions) {
        return Arrays.stream(extensions.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Path storageDirectory(String purpose) {
        Path publicDirectory = Paths.get(uploadPath).toAbsolutePath().normalize();
        if (UploadBusinessType.CHAT_FILE.name().equals(purpose) || UploadBusinessType.TASK_FILE.name().equals(purpose)) {
            Path directoryName = publicDirectory.getFileName();
            return publicDirectory.resolveSibling((directoryName == null ? "uploads" : directoryName.toString()) + "-private").normalize();
        }
        return publicDirectory;
    }
}
