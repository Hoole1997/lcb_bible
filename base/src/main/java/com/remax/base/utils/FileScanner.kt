package com.remax.base.utils

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/**
 * 文件扫描工具
 * 提供文件过滤和扫描功能
 */
object FileScanner {

    private const val TAG = "FileScanner"
    const val COPY_DIR = "Remax File Recovery"
    const val COPY_DIR_PHOTO = "re_photo"
    const val COPY_DIR_VIDEO = "re_video"
    const val COPY_DIR_DOCUMENT = "re_document"
    const val COPY_DIR_AUDIO = "re_audio"
    const val COPY_DIR_OTHER = "re_other"

    /**
     * 日志控制开关
     */
    private var isLogEnabled = true

    /**
     * 设置是否启用日志
     * @param enabled true 启用日志，false 禁用日志
     */
    fun setLogEnabled(enabled: Boolean) {
        isLogEnabled = enabled
    }

    /**
     * 检查是否启用日志
     * @return true 如果启用日志，false 如果禁用日志
     */
    fun isLogEnabled(): Boolean = isLogEnabled

    /**
     * 日志输出方法
     */
    fun log(level: String, message: String, throwable: Throwable? = null) {
        if (!isLogEnabled) return

        when (level) {
            "D" -> Log.d(TAG, message, throwable)
            "W" -> Log.w(TAG, message, throwable)
            "E" -> Log.e(TAG, message, throwable)
            "V" -> Log.v(TAG, message, throwable)
        }
    }

    /**
     * 文件过滤器接口
     */
    interface FileFilter {
        /**
         * 判断文件是否符合过滤条件
         * @param file 待检查的文件
         * @return true 如果文件符合条件，false 如果不符合
         */
        fun accept(file: File): Boolean

        /**
         * 获取过滤器名称
         * @return 过滤器名称
         */
        fun getFilterName(): String

        /**
         * 获取要扫描的目录列表
         * @return 要扫描的目录列表，如果返回空列表则扫描整个外部存储
         */
        fun getScanDirectories(): List<File> = emptyList()

        /**
         * 获取要排除的目录列表
         * @return 要排除的目录列表，默认为空列表
         */
        fun getExcludeDirectories(): List<File> = emptyList()
    }

    /**
     * 基础文件过滤器
     * 提供通用的文件类型检查方法
     */
    abstract class BaseFileFilter : FileFilter {

        // 排除掉已恢复
        override fun getExcludeDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                File(externalStorage, COPY_DIR)
            )
        }

    }


    /**
     * 截图文件过滤器
     * 过滤出截图文件（Screenshot、截图等）
     */
    class ScreenshotFilter() : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false

            // 首先检查是否为图片文件
            if (!file.isImage()) {
                return false
            }

            val fileName = file.name.lowercase()
            val filePath = file.absolutePath.lowercase()

            // 检查文件名是否包含截图相关关键词
            val screenshotKeywords = listOf(
                "screenshot", "截图", "截屏", "screen", "capture", "shot"
            )

            // 检查路径是否包含截图相关目录
            val screenshotPaths = listOf(
                "screenshot", "screenshots", "截图", "截屏", "captures"
            )

            // 检查文件名
            val hasScreenshotKeyword = screenshotKeywords.any { keyword ->
                fileName.contains(keyword)
            }

            // 检查路径
            val hasScreenshotPath = screenshotPaths.any { path ->
                filePath.contains(path)
            }

            return hasScreenshotKeyword || hasScreenshotPath
        }

        override fun getFilterName(): String = "Screenshot Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                File(externalStorage, "DCIM"),
                File(externalStorage, "Pictures"),
                File(externalStorage, "Screenshots"),
                File(externalStorage, "Download")
            )
        }

        override fun getExcludeDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                File(externalStorage, COPY_DIR),
                File(externalStorage, "DCIM/.mediaTrash")
            )
        }
    }

    /**
     * 录屏文件过滤器
     * 过滤出录屏文件（Screen Record、录屏等）
     */
    class ScreenRecordFilter() : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false

            // 首先检查是否为视频文件
            if (!file.isVideo()) {
                return false
            }

            val fileName = file.name.lowercase()
            val filePath = file.absolutePath.lowercase()

            // 检查文件名是否包含录屏相关关键词
            val screenRecordKeywords = listOf(
                "screenrecord", "screen_record", "screen-record", "录屏", "录制",
                "record", "recording", "capture", "screencapture", "screen_capture",
                "screen-capture", "屏幕录制", "屏幕录像", "录像"
            )

            // 检查路径是否包含录屏相关目录
            val screenRecordPaths = listOf(
                "screenrecord", "screen_record", "screen-record", "录屏", "录制",
                "recordings", "captures", "screencaptures", "screen_captures",
                "screen-captures", "屏幕录制", "屏幕录像"
            )

            // 检查文件名
            val hasScreenRecordKeyword = screenRecordKeywords.any { keyword ->
                fileName.contains(keyword)
            }

            // 检查路径
            val hasScreenRecordPath = screenRecordPaths.any { path ->
                filePath.contains(path)
            }

            return hasScreenRecordKeyword || hasScreenRecordPath
        }

        override fun getFilterName(): String = "Screen Record Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                File(externalStorage, "DCIM"),
                File(externalStorage, "Movies"),
                File(externalStorage, "Pictures"),
                File(externalStorage, "Download"),
                File(externalStorage, "Recordings"),
                File(externalStorage, "ScreenRecords")
            )
        }

        override fun getExcludeDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                File(externalStorage, COPY_DIR),
                File(externalStorage, "DCIM/.mediaTrash")
            )
        }
    }

    /**
     * 图片文件过滤器
     * 过滤出所有图片文件
     */
    open class ImageFilter : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false
            return file.isImage()
        }

        override fun getFilterName(): String = "Image Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                externalStorage
            )
        }
    }

    /**
     * 视频文件过滤器
     * 过滤出所有视频文件
     */
    open class VideoFilter : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false
            return file.isVideo()
        }

        override fun getFilterName(): String = "Video Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                externalStorage
            )
        }
    }

    /**
     * 文档文件过滤器
     * 过滤出所有文档文件（PDF、Office文档等）
     */
    open class DocumentFilter : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false
            return file.isDocument()
        }

        override fun getFilterName(): String = "Document Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                externalStorage
            )
        }
    }

    /**
     * 音频文件过滤器
     * 过滤出所有音频文件（MP3、WAV、FLAC等）
     */
    open class AudioFilter : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false
            return file.isAudio()
        }

        override fun getFilterName(): String = "Audio Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                externalStorage
            )
        }
    }

    /**
     * 大文件过滤器
     * 过滤出文件大小大于等于指定最小值的文件
     */
    class LargeFileFilter(
        private val minSizeBytes: Long = 10 * 1024 * 1024L // 默认10MB
    ) : BaseFileFilter() {
        
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false
            
            // 检查文件大小是否大于等于最小值
            return try {
                file.length() >= minSizeBytes
            } catch (e: Exception) {
                log("W", "检查文件大小时发生错误: ${file.absolutePath}", e)
                false
            }
        }

        override fun getFilterName(): String = "Large File Filter (≥${formatFileSize(minSizeBytes)})"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(
                externalStorage
            )
        }
        
        /**
         * 格式化文件大小为可读格式
         * @param bytes 字节数
         * @return 格式化后的文件大小字符串
         */
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)}GB"
                bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
                bytes >= 1024 -> "${bytes / 1024}KB"
                else -> "${bytes}B"
            }
        }
    }

    /**
     * 垃圾清理过滤器
     * 过滤出所有类型的垃圾文件：
     * 1. 垃圾文件：.thumbnails目录下的图片、.crash、.anr、.tombstone等
     * 2. 安装包：APK文件
     * 3. 临时文件：.cache、.tmp、.temp、.dex、.odex
     * 4. 日志文件：.log、.log.txt、.out、.err
     */
    class JunkCleanFilter : BaseFileFilter() {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false
            return file.isJunkFile() || file.isApkFile() || file.isTempFile() || file.isLogFile()
        }

        override fun getFilterName(): String = "Junk Clean Filter"

        override fun getScanDirectories(): List<File> {
            val externalStorage = Environment.getExternalStorageDirectory()
            return listOf(externalStorage)
        }
    }

    /**
     * 自定义文件过滤器
     * 根据文件扩展名过滤
     */
    class CustomFilter(
        private val extensions: List<String>,
        private val scanDirectories: List<File> = emptyList(),
        private val excludeDirectories: List<File> = emptyList()
    ) : FileFilter {
        override fun accept(file: File): Boolean {
            if (!file.isFile) return false

            val extension = file.extension.lowercase()
            return extension in extensions
        }

        override fun getFilterName(): String = "Custom Filter (${extensions.joinToString(", ")})"

        override fun getScanDirectories(): List<File> = scanDirectories

        override fun getExcludeDirectories(): List<File> = excludeDirectories
    }

    /**
     * 扫描结果数据类
     */
    data class ScanResult(
        val files: List<File>, val totalCount: Int, val scanTime: Long, val filterName: String
    )

    /**
     * 扫描外部存储中的文件
     * @param context 上下文
     * @param filter 文件过滤器
     * @param onFileScanned 文件扫描回调，参数为当前扫描的文件和是否匹配过滤器
     * @param onProgress 扫描进度回调，参数为进度百分比(0.0-1.0)
     * @return 扫描结果
     */
    suspend fun scanExternalStorage(
        filter: FileFilter,
        onFileScanned: ((file: File, matchFilter: Boolean) -> Unit)? = null,
        onProgress: ((progress: Float) -> Unit)? = null
    ): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val files = mutableListOf<File>()

        try {
            // 获取要扫描的目录列表
            val scanDirectories = filter.getScanDirectories()
            val excludeDirectories = filter.getExcludeDirectories()

            val directoriesToScan = if (scanDirectories.isEmpty()) {
                // 如果没有指定目录，扫描整个外部存储
                listOf(Environment.getExternalStorageDirectory())
            } else {
                scanDirectories
            }

            // 检查目录是否可访问，并排除指定的目录
            val validDirectories = directoriesToScan.filter { dir ->
                dir.exists() && dir.canRead() && !isExcludedDirectory(dir, excludeDirectories)
            }

            if (validDirectories.isEmpty()) {
                log("W", "没有可访问的目录")
                return@withContext ScanResult(emptyList(), 0, 0, filter.getFilterName())
            }

            log(
                "D",
                "开始扫描，目录数: ${validDirectories.size}, 排除目录数: ${excludeDirectories.size}"
            )

            // 检查是否需要进度跟踪
            val needProgressTracking = onProgress != null || onFileScanned != null

            if (needProgressTracking) {
                // 需要进度跟踪的完整扫描流程
                // 第一步：计算所有文件的总数
                log("D", "开始计算文件总数...")
                var totalFiles = 0
                validDirectories.forEach { directory ->
                    totalFiles += countFiles(directory, excludeDirectories)
                }
                log("D", "文件总数计算完成: $totalFiles")

                if (totalFiles == 0) {
                    log("W", "没有找到任何文件")
                    return@withContext ScanResult(emptyList(), 0, 0, filter.getFilterName())
                }

                // 第二步：扫描文件并更新进度
                var scannedFiles = 0
                validDirectories.forEach { directory ->
                    scannedFiles = scanDirectoryWithProgress(
                        directory,
                        filter,
                        files,
                        onFileScanned,
                        excludeDirectories,
                        totalFiles,
                        scannedFiles
                    ) { newScannedFiles ->
                        val progress = newScannedFiles.toFloat() / totalFiles.toFloat()
                        onProgress?.invoke(progress)
                    }
                }
            } else {
                // 快速扫描模式 - 不需要进度跟踪
                log("D", "使用快速扫描模式")
                validDirectories.forEach { directory ->
                    scanDirectoryFast(directory, filter, files, excludeDirectories)
                }
            }

            val scanTime = System.currentTimeMillis() - startTime
            log("D", "扫描完成，找到 ${files.size} 个文件，耗时 ${scanTime}ms")

            ScanResult(files, files.size, scanTime, filter.getFilterName())

        } catch (e: Exception) {
            log("E", "扫描文件时发生错误", e)
            val scanTime = System.currentTimeMillis() - startTime
            ScanResult(files, 0, scanTime, filter.getFilterName())
        }
    }

    /**
     * 检查目录是否在排除列表中
     * @param directory 要检查的目录
     * @param excludeDirectories 排除目录列表
     * @return true 如果目录应该被排除，false 如果不应该被排除
     */
    private fun isExcludedDirectory(directory: File, excludeDirectories: List<File>): Boolean {
        return excludeDirectories.any { excludeDir ->
            directory.absolutePath == excludeDir.absolutePath ||
                    directory.absolutePath.startsWith(excludeDir.absolutePath + File.separator)
        }
    }

    /**
     * 扫描指定目录
     * @param directory 要扫描的目录
     * @param filter 文件过滤器
     * @param resultFiles 结果文件列表
     * @param onFileScanned 文件扫描回调
     * @param excludeDirectories 排除目录列表
     */
    private fun scanDirectory(
        directory: File,
        filter: FileFilter,
        resultFiles: MutableList<File>,
        onFileScanned: ((file: File, matchFilter: Boolean) -> Unit)?,
        excludeDirectories: List<File>
    ): Int {
        return scanDirectoryWithProgress(
            directory,
            filter,
            resultFiles,
            onFileScanned,
            excludeDirectories,
            0,
            0
        ) { }
    }

    /**
     * 快速扫描指定目录（无回调）
     * @param directory 要扫描的目录
     * @param filter 文件过滤器
     * @param resultFiles 结果文件列表
     * @param excludeDirectories 排除目录列表
     */
    private fun scanDirectoryFast(
        directory: File,
        filter: FileFilter,
        resultFiles: MutableList<File>,
        excludeDirectories: List<File>
    ) {
        try {
            if (!directory.exists() || !directory.canRead()) {
                log("W", "目录不存在或无法读取: ${directory.absolutePath}")
                return
            }

            val files = directory.listFiles()
            if (files == null) {
                log("W", "无法列出目录内容: ${directory.absolutePath}")
                return
            }

            for (file in files) {
                try {
                    if (file.isFile) {
                        // 直接检查文件是否符合过滤条件，不调用回调
                        try {
                            if (filter.accept(file)) {
                                resultFiles.add(file)
                                log("V", "找到匹配文件: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            log("W", "过滤器检查文件时发生错误: ${file.absolutePath}", e)
                        }
                    } else if (file.isDirectory) {
                        // 检查是否为排除目录
                        if (!isExcludedDirectory(file, excludeDirectories)) {
                            // 递归扫描子目录
                            scanDirectoryFast(file, filter, resultFiles, excludeDirectories)
                        } else {
                            log("D", "跳过排除目录: ${file.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    log("W", "处理文件时发生错误: ${file.absolutePath}", e)
                }
            }
        } catch (e: Exception) {
            log("E", "扫描目录时发生错误: ${directory.absolutePath}", e)
        }
    }

    /**
     * 扫描指定目录（带进度跟踪）
     * @param directory 要扫描的目录
     * @param filter 文件过滤器
     * @param resultFiles 结果文件列表
     * @param onFileScanned 文件扫描回调
     * @param excludeDirectories 排除目录列表
     * @param totalFiles 总文件数
     * @param currentScannedFiles 当前已扫描的文件数
     * @param onProgressUpdate 进度更新回调
     */
    private fun scanDirectoryWithProgress(
        directory: File,
        filter: FileFilter,
        resultFiles: MutableList<File>,
        onFileScanned: ((file: File, matchFilter: Boolean) -> Unit)?,
        excludeDirectories: List<File>,
        totalFiles: Int,
        currentScannedFiles: Int,
        onProgressUpdate: (Int) -> Unit
    ): Int {
        var scannedFiles = currentScannedFiles

        try {
            if (!directory.exists() || !directory.canRead()) {
                log("W", "目录不存在或无法读取: ${directory.absolutePath}")
                return scannedFiles
            }

            val files = directory.listFiles()
            if (files == null) {
                log("W", "无法列出目录内容: ${directory.absolutePath}")
                return scannedFiles
            }

            for (file in files) {
                try {
                    if (file.isFile) {
                        // 检查文件是否符合过滤条件（只调用一次）
                        val isMatchFilter = try {
                            filter.accept(file)
                        } catch (e: Exception) {
                            log("W", "过滤器检查文件时发生错误: ${file.absolutePath}", e)
                            false
                        }

                        // 调用文件扫描回调
                        onFileScanned?.invoke(file, isMatchFilter)

                        // 更新已扫描文件数
                        scannedFiles++
                        onProgressUpdate(scannedFiles)

                        // 如果文件匹配过滤器，添加到结果列表
                        if (isMatchFilter) {
                            resultFiles.add(file)
                            log("V", "找到匹配文件: ${file.absolutePath}")
                        }
                    } else if (file.isDirectory) {
                        // 检查是否为排除目录
                        if (!isExcludedDirectory(file, excludeDirectories)) {
                            // 递归扫描子目录
                            scannedFiles = scanDirectoryWithProgress(
                                file,
                                filter,
                                resultFiles,
                                onFileScanned,
                                excludeDirectories,
                                totalFiles,
                                scannedFiles,
                                onProgressUpdate
                            )
                        } else {
                            log("D", "跳过排除目录: ${file.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    log("W", "处理文件时发生错误: ${file.absolutePath}", e)
                }
            }
        } catch (e: Exception) {
            log("E", "扫描目录时发生错误: ${directory.absolutePath}", e)
        }

        return scannedFiles
    }

    /**
     * 统计目录中的文件总数（排除指定目录）
     * @param directory 要统计的目录
     * @param excludeDirectories 排除目录列表
     * @return 文件总数
     */
    private fun countFiles(directory: File, excludeDirectories: List<File> = emptyList()): Int {
        var count = 0
        try {
            if (!directory.exists() || !directory.canRead()) {
                return 0
            }

            val files = directory.listFiles()
            if (files == null) {
                return 0
            }

            for (file in files) {
                if (file.isFile) {
                    count++
                } else if (file.isDirectory) {
                    // 检查是否为排除目录
                    if (!isExcludedDirectory(file, excludeDirectories)) {
                        count += countFiles(file, excludeDirectories)
                    }
                }
            }
        } catch (e: Exception) {
            log("W", "统计文件数时发生错误: ${directory.absolutePath}", e)
        }
        return count
    }


}

/**
 * File扩展函数 - 文档类型检测
 */
fun File.isPdfFile(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            // 检查PDF文件头 (0-4字节: %PDF)
            val pdfHeader = byteArrayOf(0x25.toByte(), 0x50.toByte(), 0x44.toByte(), 0x46.toByte())
            headerBytes.copyOfRange(0, 4).contentEquals(pdfHeader)
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

fun File.isOfficeDocument(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            // 检查Office文档文件头 (0-2字节: PK) - 新版Office文档
            val pkHeader = byteArrayOf(0x50.toByte(), 0x4B.toByte())
            if (headerBytes.copyOfRange(0, 2).contentEquals(pkHeader)) {
                return isNewOfficeDocument(randomAccessFile)
            }

            // 检查旧版Office文档 (DOC, XLS, PPT)
            isLegacyOfficeDocument(headerBytes)
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

fun File.isNewOfficeDocument(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            // 检查Office文档文件头 (0-2字节: PK)
            val pkHeader = byteArrayOf(0x50.toByte(), 0x4B.toByte())
            if (headerBytes.copyOfRange(0, 2).contentEquals(pkHeader)) {
                return isNewOfficeDocument(randomAccessFile)
            }
            false
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

fun File.isLegacyOfficeDocument(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            isLegacyOfficeDocument(headerBytes)
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * 私有辅助函数
 */
private fun isNewOfficeDocument(randomAccessFile: RandomAccessFile): Boolean {
    return try {
        // 重新定位到文件开头
        randomAccessFile.seek(0)

        // 读取ZIP文件头
        val zipHeader = ByteArray(4)
        randomAccessFile.read(zipHeader)

        // 检查ZIP文件头
        val pkHeader = byteArrayOf(0x50.toByte(), 0x4B.toByte())
        if (!zipHeader.copyOfRange(0, 2).contentEquals(pkHeader)) {
            return false
        }

        // 检查Office文档特有的文件
        // Word文档: [Content_Types].xml, _rels/, docProps/
        // Excel文档: [Content_Types].xml, _rels/, docProps/, xl/
        // PowerPoint文档: [Content_Types].xml, _rels/, docProps/, ppt/
        val officeFiles = listOf(
            "[Content_Types].xml",
            "_rels/",
            "docProps/",
            "xl/",      // Excel特有
            "ppt/",     // PowerPoint特有
            "word/"     // Word特有
        )

        // 读取文件内容进行搜索
        val buffer = ByteArray(8192) // 8KB缓冲区
        randomAccessFile.read(buffer)
        val content = String(buffer, Charsets.UTF_8)

        // 检查是否包含Office文档特有的文件
        val foundOfficeFiles = officeFiles.count { content.contains(it) }

        // 如果找到至少2个Office特有文件，认为是Office文档
        return foundOfficeFiles >= 2
    } catch (e: Exception) {
        false
    }
}

private fun isLegacyOfficeDocument(headerBytes: ByteArray): Boolean {
    // 检查旧版Office文档的文件头
    // 所有旧版Office文档都使用相同的OLE文件头
    // DOC, XLS, PPT 文件都以 D0 CF 11 E0 A1 B1 1A E1 开头
    val oleHeader = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte())
    return headerBytes.copyOfRange(0, 4).contentEquals(oleHeader)
}


/**
 * 计算文件集合的总大小（返回字节数）
 * @param files 文件集合
 * @return Long 总字节数
 */
fun calculateTotalSizeInBytes(files: Collection<File>): Long {
    var totalBytes = 0L

    files.forEach { file ->
        if (file.exists() && file.isFile) {
            try {
                totalBytes += file.length()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return totalBytes
}

/**
 * File扩展函数 - 音频文件检测
 */
fun File.isAudio(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            // 检查MP3文件头 (0-3字节: ID3 或 0-2字节: 0xFF 0xFB/0xFA/0xF2/0xF3)
            val id3Header = byteArrayOf(0x49.toByte(), 0x44.toByte(), 0x33.toByte())
            if (headerBytes.copyOfRange(0, 3).contentEquals(id3Header)) {
                return true
            }

            // 检查MP3同步字节
            val syncBytes = listOf(
                byteArrayOf(0xFF.toByte(), 0xFB.toByte()),
                byteArrayOf(0xFF.toByte(), 0xFA.toByte()),
                byteArrayOf(0xFF.toByte(), 0xF2.toByte()),
                byteArrayOf(0xFF.toByte(), 0xF3.toByte())
            )
            if (syncBytes.any { sync ->
                    headerBytes.copyOfRange(0, 2).contentEquals(sync)
                }) {
                return true
            }

            // 检查WAV文件头 (0-4字节: RIFF, 8-12字节: WAVE)
            val riffBytes = headerBytes.copyOfRange(0, 4)
            val riffString = String(riffBytes)
            if (riffString == "RIFF") {
                val waveBytes = headerBytes.copyOfRange(8, 12)
                val waveString = String(waveBytes)
                if (waveString == "WAVE") {
                    return true
                }
            }

            // 检查FLAC文件头 (0-4字节: fLaC)
            val flacHeader =
                byteArrayOf(0x66.toByte(), 0x4C.toByte(), 0x61.toByte(), 0x43.toByte())
            if (headerBytes.copyOfRange(0, 4).contentEquals(flacHeader)) {
                return true
            }

            return false
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * File扩展函数 - 视频文件检测
 */
fun File.isVideo(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            // 检查MP4文件头 (4-8字节: ftyp)
            val ftypBytes = headerBytes.copyOfRange(4, 8)
            val ftypString = String(ftypBytes)
            if (ftypString == "ftyp") {
                // 检查支持的格式 (8-12字节)
                val formatBytes = headerBytes.copyOfRange(8, 12)
                val formatString = String(formatBytes)
                val supportedFormats =
                    listOf("mp42", "mp41", "isom", "iso2", "avc1", "3gp4", "M4V ", "M4A ")
                if (supportedFormats.any { format -> formatString.startsWith(format) }) {
                    return true
                }
            }

            // 检查AVI文件头 (0-4字节: RIFF, 8-12字节: AVI )
            val riffBytes = headerBytes.copyOfRange(0, 4)
            val riffString = String(riffBytes)
            if (riffString == "RIFF") {
                val aviBytes = headerBytes.copyOfRange(8, 12)
                val aviString = String(aviBytes)
                if (aviString == "AVI ") {
                    return true
                }
            }

            // 检查MKV文件头 (0-4字节: EBML)
            val ebmlBytes = headerBytes.copyOfRange(0, 4)
            val ebmlString = String(ebmlBytes)
            if (ebmlString == "EBML") {
                return true
            }

            return false
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * File扩展函数 - 图片文件检测
 */
fun File.isImage(): Boolean {
    return try {
        if (!exists() || !isFile) return false

        // 检查文件大小
        if (length() < 12) return false

        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            // 读取前12字节
            val headerBytes = ByteArray(12)
            randomAccessFile.read(headerBytes)

            // 检查PNG文件头 (0-4字节: 89504E47)
            val pngHeader =
                byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
            if (headerBytes.copyOfRange(0, 4).contentEquals(pngHeader)) {
                return true
            }

            // 检查JPEG文件头 (0-2字节: FFD8)
            val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
            if (headerBytes.copyOfRange(0, 2).contentEquals(jpegHeader)) {
                return true
            }

            // 检查WebP文件头 (8-12字节: WEBP)
            val webpHeader =
                byteArrayOf(0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte())
            if (headerBytes.copyOfRange(8, 12).contentEquals(webpHeader)) {
                return true
            }

            return false
        } finally {
            randomAccessFile.close()
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * File扩展函数 - 文档文件检测
 */
fun File.isDocument(): Boolean {
    return isPdfFile() || isOfficeDocument()
}

/**
 * File扩展函数 - 垃圾文件检测
 */
fun File.isJunkFile(): Boolean {
    if (!exists() || !isFile) return false
    
    val fileName = name.lowercase()
    val extension = extension.lowercase()
    
    // 检查垃圾文件扩展名
    val junkExtensions = listOf(".crash", ".anr", ".tombstone")
    if (extension in junkExtensions) {
        return true
    }
    
    // 检查.thumbnails目录下的图片文件
    if (parentFile?.name?.lowercase() == ".thumbnails") {
        val imageExtensions = listOf("jpg", "jpeg", "png")
        if (extension in imageExtensions) {
            return true
        }
    }
    
    return false
}

/**
 * File扩展函数 - 安装包文件检测
 */
fun File.isApkFile(): Boolean {
    if (!exists() || !isFile) return false
    return extension.lowercase() == "apk"
}

/**
 * File扩展函数 - 临时文件检测
 */
fun File.isTempFile(): Boolean {
    if (!exists() || !isFile) return false
    
    val extension = extension.lowercase()
    val tempExtensions = listOf("cache", "tmp", "temp", "dex", "odex")
    
    return extension in tempExtensions
}

/**
 * File扩展函数 - 日志文件检测
 */
fun File.isLogFile(): Boolean {
    if (!exists() || !isFile) return false
    
    val fileName = name.lowercase()
    val extension = extension.lowercase()
    
    // 检查日志文件扩展名
    val logExtensions = listOf("log", "out", "err")
    if (extension in logExtensions) {
        return true
    }
    
    // 检查.log.txt格式
    if (fileName.endsWith(".log.txt")) {
        return true
    }
    
    return false
}

/**
 * Long扩展函数 - 友好的文件大小显示
 * @return 格式化后的文件大小字符串
 */
fun Long.getFriendlySize(): String {
    return when {
        this >= 1_073_741_824L -> {
            val gb = this.toDouble() / 1_073_741_824.0
            val formatted = String.format(Locale.ENGLISH, "%.1f", gb)
            val result = if (formatted.endsWith(".0")) {
                formatted.substring(0, formatted.length - 2)
            } else {
                formatted
            }
            "${result}GB"
        }
        this >= 1_048_576L -> {
            val mb = this.toDouble() / 1_048_576.0
            val formatted = String.format(Locale.ENGLISH, "%.1f", mb)
            val result = if (formatted.endsWith(".0")) {
                formatted.substring(0, formatted.length - 2)
            } else {
                formatted
            }
            "${result}MB"
        }
        this >= 1024L -> {
            val kb = this.toDouble() / 1024.0
            val formatted = String.format(Locale.ENGLISH, "%.1f", kb)
            val result = if (formatted.endsWith(".0")) {
                formatted.substring(0, formatted.length - 2)
            } else {
                formatted
            }
            "${result}KB"
        }
        else -> "0KB"
    }
}

/**
 * String扩展函数 - 解析大小字符串，分离数字和单位
 * @param sizeString 格式化的文件大小字符串，如 "1.2MB", "500KB", "1024B"
 * @return Pair<数字部分, 单位部分>
 */
fun String.parseSizeAndUnit(): Pair<String, String> {
    return when {
        this.endsWith("GB") -> {
            val size = this.substring(0, this.length - 2)
            Pair(size, "GB")
        }
        this.endsWith("MB") -> {
            val size = this.substring(0, this.length - 2)
            Pair(size, "MB")
        }
        this.endsWith("KB") -> {
            val size = this.substring(0, this.length - 2)
            Pair(size, "KB")
        }
        this.endsWith("B") -> {
            val size = this.substring(0, this.length - 1)
            Pair(size, "B")
        }
        else -> {
            // 如果格式不匹配，默认显示原字符串
            Pair(this, "")
        }
    }
}