package com.buurman.service.export.google;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * Manages the user-owned "Buurman exports" folder in Google Drive. With the {@code drive.file}
 * scope, the app can only see files it created — so the folder is per-app and isolated from the
 * user's other Drive content.
 */
@Service
public class BuurmanExportsFolderService {

  static final String FOLDER_NAME = "Buurman exports";
  private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

  /**
   * Returns the id of the user's "Buurman exports" folder, creating it on first use. Scoped to
   * folders the current user owns (via {@code 'me' in owners}) to avoid latching onto a
   * shared-with-me folder of the same name.
   */
  public String findOrCreate(Drive drive) {
    try {
      FileList result =
          drive
              .files()
              .list()
              .setQ(
                  "name = '"
                      + FOLDER_NAME
                      + "' and mimeType = '"
                      + FOLDER_MIME_TYPE
                      + "' and 'me' in owners"
                      + " and trashed = false")
              .setSpaces("drive")
              .setFields("files(id, name)")
              .setPageSize(1)
              .execute();

      List<File> files = result.getFiles();
      if (files != null && !files.isEmpty()) {
        return files.get(0).getId();
      }

      File folder = new File();
      folder.setName(FOLDER_NAME);
      folder.setMimeType(FOLDER_MIME_TYPE);
      File created = drive.files().create(folder).setFields("id").execute();
      return created.getId();
    } catch (IOException e) {
      throw GoogleSheetsClientFactory.translate("resolving Buurman exports folder", e);
    }
  }

  /** Moves an existing file into the given folder, detaching it from the Drive root. */
  public void moveIntoFolder(Drive drive, String fileId, String folderId) {
    try {
      drive
          .files()
          .update(fileId, null)
          .setAddParents(folderId)
          .setRemoveParents("root")
          .setFields("id, parents")
          .execute();
    } catch (IOException e) {
      throw GoogleSheetsClientFactory.translate("moving sheet into folder", e);
    }
  }

  /**
   * Returns a title that does not collide with any existing Sheet in the folder owned by this user.
   * If the base title is free it is returned as-is; otherwise a numeric suffix is appended: "{base}
   * (2)", "{base} (3)", … up to a safety cap. The lookup is scoped via Drive's {@code name contains
   * '…'} so we only retrieve candidate matches, not every sheet in the folder — important for users
   * with hundreds of past exports.
   *
   * <p><b>Concurrency caveat:</b> the lookup and the subsequent {@code spreadsheets.create} are not
   * atomic. Two same-second exports from the same user can both pick the same suffix and end up
   * with duplicate titles. Acceptable for v1 — Drive permits duplicate names, and the per-sheet ids
   * returned in the response remain distinct.
   */
  public String uniqueTitle(Drive drive, String folderId, String baseTitle) {
    Set<String> existing = listSheetTitlesStartingWith(drive, folderId, baseTitle);
    if (!existing.contains(baseTitle)) {
      return baseTitle;
    }
    for (int i = 2; i < 1000; i++) {
      String candidate = baseTitle + " (" + i + ")";
      if (!existing.contains(candidate)) {
        return candidate;
      }
    }
    return baseTitle + " (" + System.currentTimeMillis() + ")";
  }

  private static Set<String> listSheetTitlesStartingWith(
      Drive drive, String folderId, String baseTitle) {
    Set<String> titles = new HashSet<>();
    // Drive supports `name contains` (not startsWith), so we substring-match and filter client-side
    // for exact matches and "(n)" suffixes.
    String escapedBase = baseTitle.replace("\\", "\\\\").replace("'", "\\'");
    try {
      String pageToken = null;
      do {
        FileList result =
            drive
                .files()
                .list()
                .setQ(
                    "'"
                        + folderId
                        + "' in parents and mimeType = 'application/vnd.google-apps.spreadsheet'"
                        + " and name contains '"
                        + escapedBase
                        + "'"
                        + " and trashed = false")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .setPageSize(200)
                .setPageToken(pageToken)
                .execute();
        List<File> files = result.getFiles();
        if (files != null) {
          for (File f : files) {
            titles.add(Optional.ofNullable(f.getName()).orElse(""));
          }
        }
        pageToken = result.getNextPageToken();
      } while (pageToken != null);
    } catch (IOException e) {
      throw GoogleSheetsClientFactory.translate("listing existing sheets", e);
    }
    return titles;
  }
}
