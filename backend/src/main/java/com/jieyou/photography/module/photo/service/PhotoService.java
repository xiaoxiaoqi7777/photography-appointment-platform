package com.jieyou.photography.module.photo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.order.mapper.OrderMapper;
import com.jieyou.photography.module.order.mapper.OrderRow;
import com.jieyou.photography.module.order.service.OrderService;
import com.jieyou.photography.module.photo.entity.Photo;
import com.jieyou.photography.module.photo.mapper.PhotoMapper;
import com.jieyou.photography.module.photo.vo.FailedFile;
import com.jieyou.photography.module.photo.vo.PhotoVO;
import com.jieyou.photography.module.photo.vo.UploadResult;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 照片模块，依据 API 约定 5.5 与 9.5：
 * - 批量上传（≤20 张、JPG/JPEG/PNG、单文件 ≤10MB），部分失败仍返回 200；
 * - 上传落盘 uploads/{订单ID}/，同步生成 _wm 水印图，失败仅记日志；
 * - 用户交付前只能读取水印预览，原图下载仅限交付后的本人；
 * - uploads/ 不作为静态资源暴露，所有读取均经过本服务的鉴权接口。
 */
@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private static final int MAX_FILES = 20;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final String WATERMARK_TEXT = "解忧摄影";

    private final PhotoMapper photoMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final Path uploadRoot;

    public PhotoService(PhotoMapper photoMapper,
                        OrderMapper orderMapper,
                        OrderService orderService,
                        @Value("${app.upload-dir:uploads/}") String uploadDir) {
        this.photoMapper = photoMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 批量上传精修照片（管理员）：仅 SHOOTING_DONE / MODIFYING。
     * 至少一张成功时订单流转为 UPLOADED，并写入 UPLOAD 日志；
     * 首轮 upload_round=1，之后每轮 +1。
     */
    @Transactional
    public UploadResult upload(Long orderId, List<MultipartFile> files) {
        LoginUser operator = SecurityUtils.currentUser();
        OrderRow order = requireOrder(orderId);
        boolean firstRound;
        if ("SHOOTING_DONE".equals(order.getStatus())) {
            firstRound = true;
        } else if ("MODIFYING".equals(order.getStatus())) {
            firstRound = false;
        } else {
            throw new BizException(ResultCode.STATE_CONFLICT, "当前订单状态不允许上传照片");
        }
        if (files == null || files.isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "请选择要上传的照片");
        }
        if (files.size() > MAX_FILES) {
            throw new BizException(ResultCode.PARAM_INVALID, "单次最多上传 " + MAX_FILES + " 张照片");
        }

        int round = firstRound ? 1 : maxUploadRound(orderId) + 1;
        List<String> success = new ArrayList<>();
        List<FailedFile> failed = new ArrayList<>();
        List<Path> writtenFiles = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String rawName = file.getOriginalFilename();
                String name = displayName(rawName);
                try {
                    if (rawName != null
                            && (rawName.contains("/") || rawName.contains("\\") || rawName.contains(".."))) {
                        throw new FileRejectedException("文件名不合法");
                    }
                    validateFile(file, name);
                    Path directory = uploadRoot.resolve(String.valueOf(orderId)).normalize();
                    if (!directory.startsWith(uploadRoot)) {
                        throw new FileRejectedException("文件存储路径不合法");
                    }
                    Files.createDirectories(directory);

                    String extension = extensionOf(name);
                    String baseName = UUID.randomUUID().toString().replace("-", "");
                    Path original = directory.resolve(baseName + "." + extension);
                    writtenFiles.add(original);
                    try (InputStream input = file.getInputStream()) {
                        Files.copy(input, original);
                    }

                    Path watermark = directory.resolve(baseName + "_wm." + extension);
                    String watermarkPath = null;
                    try {
                        generateWatermark(original, watermark, extension);
                        writtenFiles.add(watermark);
                        watermarkPath = relativePath(watermark);
                    } catch (Exception e) {
                        // 水印失败不阻塞业务，watermark_path 暂空，后续可重试
                        log.warn("水印生成失败 orderId={} file={}: {}", orderId, name, e.getMessage());
                    }

                    Photo photo = new Photo();
                    photo.setOrderId(orderId);
                    photo.setOriginalName(name);
                    photo.setFilePath(relativePath(original));
                    photo.setWatermarkPath(watermarkPath);
                    photo.setFileSize(file.getSize());
                    photo.setUploadRound(round);
                    photo.setCreateTime(LocalDateTime.now());
                    photoMapper.insert(photo);
                    success.add(name);
                } catch (FileRejectedException e) {
                    failed.add(new FailedFile(name, e.getMessage()));
                } catch (IOException e) {
                    log.error("照片保存失败 orderId={} file={}", orderId, name, e);
                    failed.add(new FailedFile(name, "文件保存失败"));
                }
            }

            if (!success.isEmpty()) {
                orderService.markUploaded(orderId, operator);
            }
            return new UploadResult(success, failed);
        } catch (RuntimeException e) {
            // 数据库写入或状态流转失败时回滚，并清理本次已落盘文件
            writtenFiles.forEach(PhotoService::deleteQuietly);
            throw e;
        }
    }

    /**
     * 订单照片列表（USER 仅本人，ADMIN 全部）。
     * 列表只暴露鉴权预览入口，不返回磁盘路径。
     */
    public List<PhotoVO> list(Long orderId) {
        LoginUser user = SecurityUtils.currentUser();
        OrderRow order = requireOrder(orderId);
        requireAccess(order, user);

        return photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getOrderId, orderId)
                        .orderByAsc(Photo::getUploadRound)
                        .orderByAsc(Photo::getId))
                .stream()
                .map(photo -> new PhotoVO(photo.getId(), photo.getOriginalName(), photo.getFileSize(),
                        photo.getUploadRound(), previewUrl(orderId, photo.getId())))
                .toList();
    }

    /**
     * 水印预览（5.5.2）：交付前 USER 仅能读取水印图，交付后默认展示原图；ADMIN 始终返回原图。
     */
    public ResponseEntity<Resource> preview(Long orderId, Long photoId) {
        LoginUser user = SecurityUtils.currentUser();
        OrderRow order = requireOrder(orderId);
        Photo photo = requirePhoto(orderId, photoId);

        Path file;
        if (isAdmin(user)) {
            file = resolveStoredPath(photo.getFilePath());
        } else {
            requireOwnership(order, user);
            if ("DELIVERED".equals(order.getStatus())) {
                file = resolveStoredPath(photo.getFilePath());
            } else {
                if (photo.getWatermarkPath() == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "水印预览图暂不可用，请稍后重试");
                }
                file = resolveStoredPath(photo.getWatermarkPath());
            }
        }
        return streamResponse(file, null);
    }

    /**
     * 原图下载（5.5.3）：USER 仅本人且订单已交付；ADMIN 可随时下载。
     * Content-Disposition 使用 RFC 5987 的 filename*=UTF-8'' 编码，保证中文文件名不乱码。
     */
    public ResponseEntity<Resource> download(Long orderId, Long photoId) {
        LoginUser user = SecurityUtils.currentUser();
        OrderRow order = requireOrder(orderId);
        Photo photo = requirePhoto(orderId, photoId);

        if (!isAdmin(user)) {
            requireOwnership(order, user);
            if (!"DELIVERED".equals(order.getStatus())) {
                throw new BizException(ResultCode.FORBIDDEN, "订单交付后才能下载原图");
            }
        }
        Path file = resolveStoredPath(photo.getFilePath());
        String encodedName = URLEncoder.encode(photo.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return streamResponse(file, "attachment; filename*=UTF-8''" + encodedName);
    }

    private ResponseEntity<Resource> streamResponse(Path file, String contentDisposition) {
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store");
        if (contentDisposition != null) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        }
        return builder.body(new FileSystemResource(file));
    }

    private void validateFile(MultipartFile file, String name) throws IOException {
        if (file.isEmpty()) {
            throw new FileRejectedException("文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileRejectedException("单个文件不能超过 10MB");
        }
        String extension = extensionOf(name);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileRejectedException("仅支持 JPG/JPEG/PNG 格式");
        }
        byte[] header = new byte[8];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.readNBytes(header, 0, header.length);
        }
        boolean matches = "png".equals(extension) ? isPng(header, length) : isJpeg(header, length);
        if (!matches) {
            throw new FileRejectedException("文件内容与扩展名不符");
        }
    }

    private void generateWatermark(Path original, Path target, String extension) throws IOException {
        BufferedImage source = ImageIO.read(original.toFile());
        if (source == null) {
            throw new IOException("无法解析图片内容");
        }
        int width = source.getWidth();
        int height = source.getHeight();
        boolean png = "png".equals(extension);
        BufferedImage canvas = new BufferedImage(width, height,
                png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            if (!png) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.drawImage(source, 0, 0, null);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int fontSize = Math.max(14, Math.min(width, height) / 12);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            FontMetrics metrics = graphics.getFontMetrics();
            int padding = Math.max(10, fontSize / 2);
            int x = Math.max(padding, width - metrics.stringWidth(WATERMARK_TEXT) - padding);
            int y = Math.max(metrics.getAscent(), height - metrics.getDescent() - padding);

            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            graphics.setColor(new Color(0, 0, 0, 140));
            graphics.drawString(WATERMARK_TEXT, x + 1, y + 1);
            graphics.setColor(new Color(255, 255, 255, 210));
            graphics.drawString(WATERMARK_TEXT, x, y);
        } finally {
            graphics.dispose();
        }
        if (!ImageIO.write(canvas, png ? "png" : "jpg", target.toFile())) {
            throw new IOException("水印图写出失败");
        }
    }

    private OrderRow requireOrder(Long orderId) {
        OrderRow order = orderMapper.selectOrderRow(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Photo requirePhoto(Long orderId, Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null || !orderId.equals(photo.getOrderId())) {
            throw new BizException(ResultCode.NOT_FOUND, "照片不存在");
        }
        return photo;
    }

    private Path resolveStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new BizException(ResultCode.NOT_FOUND, "照片文件不存在");
        }
        Path path = uploadRoot.resolve(storedPath).normalize();
        if (!path.startsWith(uploadRoot) || !Files.isRegularFile(path)) {
            throw new BizException(ResultCode.NOT_FOUND, "照片文件不存在");
        }
        return path;
    }

    private int maxUploadRound(Long orderId) {
        List<Photo> latest = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getOrderId, orderId)
                .orderByDesc(Photo::getUploadRound)
                .last("LIMIT 1"));
        return latest.isEmpty() ? 0 : latest.get(0).getUploadRound();
    }

    private String relativePath(Path file) {
        return uploadRoot.relativize(file).toString().replace('\\', '/');
    }

    private static String previewUrl(Long orderId, Long photoId) {
        return "/api/orders/" + orderId + "/photos/" + photoId + "/preview";
    }

    private static boolean isAdmin(LoginUser user) {
        return "ADMIN".equals(user.role());
    }

    private static void requireAccess(OrderRow order, LoginUser user) {
        if (!isAdmin(user)) {
            requireOwnership(order, user);
        }
    }

    private static void requireOwnership(OrderRow order, LoginUser user) {
        if (!order.getUserId().equals(user.id())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该订单");
        }
    }

    /**
     * 仅取文件名部分并拒绝路径分隔符/上跳，防路径穿越与响应头注入。
     */
    private static String displayName(String raw) {
        if (raw == null) {
            return "photo";
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) {
            return "photo";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
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
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("清理文件失败: {}", path);
        }
    }

    private static final class FileRejectedException extends RuntimeException {
        private FileRejectedException(String message) {
            super(message);
        }
    }
}
