package com.jieyou.photography.module.cover.service;

import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.cover.vo.CoverUploadVO;
import com.jieyou.photography.security.LoginUser;
import com.jieyou.photography.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 主题/套餐封面上传与公开读取：
 * - 仅管理员可上传，JPG/JPEG/PNG、单文件 ≤5MB，校验扩展名与文件头；
 * - 文件名由服务端生成 UUID，原始文件名不落盘、不返回；
 * - 封面属于公开营销素材，通过只读接口公开访问；仅允许读取服务端生成格式的文件名，
 *   其余 uploads/ 内容仍不经静态资源暴露。
 */
@Service
public class CoverService {

    private static final Logger log = LoggerFactory.getLogger(CoverService.class);

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Pattern STORED_NAME_PATTERN =
            Pattern.compile("^[0-9a-f]{32}\\.(jpg|jpeg|png)$");
    private static final String COVER_DIRECTORY = "covers";
    private static final String CACHE_CONTROL_VALUE = "public, max-age=31536000, immutable";

    private final Path uploadRoot;
    private final Path coverRoot;

    public CoverService(@Value("${app.upload-dir:uploads/}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.coverRoot = uploadRoot.resolve(COVER_DIRECTORY).normalize();
        if (!coverRoot.startsWith(uploadRoot)) {
            throw new IllegalStateException("封面存储目录不合法");
        }
    }

    /**
     * 上传封面（管理员）：校验通过后以 UUID 文件名写入 uploads/covers/。
     */
    public CoverUploadVO upload(MultipartFile file) {
        LoginUser operator = SecurityUtils.currentUser();
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "请选择要上传的封面图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.PARAM_INVALID, "封面图片不能超过 5MB");
        }

        String rawName = file.getOriginalFilename();
        if (rawName != null
                && (rawName.contains("/") || rawName.contains("\\") || rawName.contains(".."))) {
            throw new BizException(ResultCode.PARAM_INVALID, "文件名不合法");
        }
        String extension = extensionOf(displayName(rawName));
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(ResultCode.PARAM_INVALID, "仅支持 JPG/JPEG/PNG 格式");
        }

        Path target = null;
        try {
            validateImageSignature(file, extension);
            Files.createDirectories(coverRoot);

            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            target = coverRoot.resolve(storedName).normalize();
            if (!target.startsWith(coverRoot)) {
                throw new BizException(ResultCode.PARAM_INVALID, "文件存储路径不合法");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            log.info("封面上传成功 adminId={} storedName={}", operator.id(), storedName);
            return new CoverUploadVO("/api/covers/" + storedName);
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            deleteQuietly(target);
            log.error("封面保存失败 adminId={}", operator.id(), e);
            throw new BizException(ResultCode.SERVER_ERROR, "封面保存失败，请稍后重试");
        }
    }

    /**
     * 公开读取封面：只接受服务端生成的 UUID 文件名，防止读取 uploads/ 下的其他文件。
     */
    public ResponseEntity<Resource> read(String filename) {
        if (filename == null || !STORED_NAME_PATTERN.matcher(filename).matches()) {
            throw new BizException(ResultCode.NOT_FOUND, "封面图片不存在");
        }
        Path file = coverRoot.resolve(filename).normalize();
        if (!file.startsWith(coverRoot) || !Files.isRegularFile(file)) {
            throw new BizException(ResultCode.NOT_FOUND, "封面图片不存在");
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }

    private void validateImageSignature(MultipartFile file, String extension) throws IOException {
        byte[] header = new byte[8];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.readNBytes(header, 0, header.length);
        }
        boolean matches = "png".equals(extension) ? isPng(header, length) : isJpeg(header, length);
        if (!matches) {
            throw new BizException(ResultCode.PARAM_INVALID, "文件内容与扩展名不符");
        }
    }

    private static String displayName(String raw) {
        if (raw == null) {
            return "cover";
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        return name.isEmpty() ? "cover" : name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isJpeg(byte[] header, int length) {
        return length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] header, int length) {
        return length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("清理未完成的封面文件失败: {}", path.getFileName());
        }
    }
}
