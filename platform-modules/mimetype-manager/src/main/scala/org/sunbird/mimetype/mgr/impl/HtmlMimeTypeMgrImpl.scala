package org.sunbird.mimetype.mgr.impl

import java.io.File

import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}
import org.sunbird.telemetry.logger.TelemetryManager
import org.sunbird.common.Platform

import scala.concurrent.{ExecutionContext, Future}

class HtmlMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

    override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
        validateUploadRequest(objectId, node, uploadFile)
        val indexHtmlValidation: Boolean = if (Platform.config.hasPath("indexHtmlValidation.env")) Platform.config.getBoolean("indexHtmlValidation.env") else false
        val flag: Boolean = if (indexHtmlValidation) isValidPackageStructure(uploadFile, List[String]("index.html")) else true
        if (flag) {
            val urls = uploadArtifactToCloud(uploadFile, objectId, filePath)
            TelemetryManager.info("uploadArtifactToCloud Success to cloud with the URL's" + urls)
            node.getMetadata.put("s3Key", urls(IDX_S3_KEY))
            node.getMetadata.put("artifactUrl", urls(IDX_S3_URL))
            extractPackageInCloud(objectId, uploadFile, node, "snapshot", false)
            TelemetryManager.info("Package extracted in cloud artifacturl" + urls(IDX_S3_URL))
            Future(Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> urls(IDX_S3_URL), "s3Key" -> urls(IDX_S3_KEY), "size" -> getFileSize(uploadFile).asInstanceOf[AnyRef]))
            TelemetryManager.info("Upload Complete")
        } else {
            TelemetryManager.error("ERR_INVALID_FILE" + "Please Provide Valid File! with file name: " + uploadFile.getName)
            throw new ClientException("ERR_INVALID_FILE", "Please Provide Valid File!")
        }

    }

    override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
        validateUploadRequest(objectId, node, fileUrl)
        val file = copyURLToFile(objectId, fileUrl)
        upload(objectId, node, file, filePath, params)
    }

}
