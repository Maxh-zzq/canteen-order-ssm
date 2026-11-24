package com.xatu.canteen.service;

import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.CanteenProperties;
import com.xatu.canteen.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 菜品图片上传。校验后缀、大小，重命名后按月份落盘，禁止路径穿越。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final CanteenProperties properties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        long maxBytes = properties.getFile().getMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "图片不能超过 " + properties.getFile().getMaxSizeMb() + "MB");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        List<String> allowed = Arrays.asList(properties.getFile().getAllowedExtensions().split(","));
        if (!allowed.contains(ext)) {
            throw new BizException(ResultCode.BAD_REQUEST, "只支持 " + properties.getFile().getAllowedExtensions() + " 格式");
        }
        Path root = Paths.get(properties.getFile().getUploadDir()).toAbsolutePath().normalize();
        String month = LocalDate.now().format(MONTH);
        Path dir = root.resolve(month).normalize();
        if (!dir.startsWith(root)) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法路径");
        }
        try {
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new BizException(ResultCode.BAD_REQUEST, "非法路径");
            }
            file.transferTo(target.toFile());
            String prefix = properties.getFile().getAccessPrefix();
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            return prefix + month + "/" + name;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存上传文件失败", e);
            throw new BizException(ResultCode.SERVER_ERROR, "图片保存失败");
        }
    }
}
