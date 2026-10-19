package com.jieyou.photography.module.photo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 照片表 t_photo，结构依据《数据库设计》2.3.5 节。
 * file_path / watermark_path 存储相对于 app.upload-dir 的路径。
 */
@TableName("t_photo")
public class Photo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String originalName;
    private String filePath;
    private String watermarkPath;
    private Long fileSize;
    private Integer uploadRound;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getWatermarkPath() {
        return watermarkPath;
    }

    public void setWatermarkPath(String watermarkPath) {
        this.watermarkPath = watermarkPath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getUploadRound() {
        return uploadRound;
    }

    public void setUploadRound(Integer uploadRound) {
        this.uploadRound = uploadRound;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
