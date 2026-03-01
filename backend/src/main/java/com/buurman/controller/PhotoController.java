package com.buurman.controller;

import java.net.URL;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.BulkDownloadRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePhotoRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.generated.api.PhotosApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PhotoService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PhotoController implements PhotosApi {

  private final PhotoService photoService;
  private final HttpServletResponse httpServletResponse;

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getAllPhotos(
      String search, String entityType, Integer page, Integer size, String sort, String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    SortDirection sortDirection = SortDirection.valueOf(direction);
    PageRequest pageRequest = PageRequest.of(page, size, sort, sortDirection);
    return photoService.searchPhotosPaginated(search, entityType, principal, pageRequest);
  }

  @Override
  public PhotoResponse getPhoto(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return photoService.getPhoto(identifier, principal);
  }

  @Override
  public String getPhotoDownloadUrl(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL downloadUrl = photoService.getDownloadUrl(identifier, principal);
    return downloadUrl.toString();
  }

  @Override
  public String getPreviewUrl(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // For now, preview URL is same as download URL
    URL previewUrl = photoService.getDownloadUrl(identifier, principal);
    return previewUrl.toString();
  }

  @Override
  public PhotoResponse updatePhoto(String identifier, UpdatePhotoRequest updatePhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return photoService.updatePhoto(identifier, updatePhotoRequest, principal);
  }

  @Override
  public void deletePhoto(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    photoService.deletePhoto(identifier, principal);
  }

  @Override
  public Resource bulkDownload(BulkDownloadRequest bulkDownloadRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] zipData =
        photoService.bulkDownload(bulkDownloadRequest.documentIdentifiers(), principal);
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=photos.zip");
    httpServletResponse.setContentType("application/octet-stream");
    return new ByteArrayResource(zipData);
  }
}
