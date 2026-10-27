package com.jantursky.proctor.android

import com.google.mlkit.genai.common.DownloadStatus

/** Progress of a model download started with [MlKitBackend.download]. */
public sealed interface ModelDownload {
    /** The download began; [bytesToDownload] is the size AICore reports. */
    public data class Started(
        val bytesToDownload: Long,
    ) : ModelDownload

    public data class Progress(
        val bytesDownloaded: Long,
    ) : ModelDownload

    public data object Completed : ModelDownload

    public data class Failed(
        val reason: String,
    ) : ModelDownload
}

internal fun DownloadStatus.toModelDownload(): ModelDownload =
    when (this) {
        is DownloadStatus.DownloadStarted -> ModelDownload.Started(bytesToDownload)
        is DownloadStatus.DownloadProgress -> ModelDownload.Progress(totalBytesDownloaded)
        DownloadStatus.DownloadCompleted -> ModelDownload.Completed
        is DownloadStatus.DownloadFailed -> ModelDownload.Failed(e.describe())
        else -> ModelDownload.Failed("Unknown download status $this")
    }
