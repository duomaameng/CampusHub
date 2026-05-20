package com.campushub.vo.file;

import com.campushub.enums.UploadBusinessType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
这个类属于 vo，也就是：

后端上传成功后，返回给前端看的结果对象。

它里面有这些字段：

id
businessType
fileName
contentType
size
url
createdAt
这和前端之前在 api.ts 里期望拿到的上传结果结构是对齐的。

它的意义是：

前端上传成功后，马上就能拿到一份“这个文件已经可用了”的结果。

比如前端要预览图片、记录图片 ID、后续创建任务时引用这个图片，都会用到它。
*/
@Data
@AllArgsConstructor
public class UploadedFileVO {

    private Long id;
    private UploadBusinessType businessType;
    private String fileName;
    private String contentType;
    private Long size;
    private String url;
    private LocalDateTime createdAt;
}
